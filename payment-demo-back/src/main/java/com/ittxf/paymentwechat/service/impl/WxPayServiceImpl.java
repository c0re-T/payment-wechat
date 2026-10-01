package com.ittxf.paymentwechat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ittxf.paymentwechat.common.config.WxPayConfig;
import com.ittxf.paymentwechat.common.enums.OrderStatus;
import com.ittxf.paymentwechat.common.enums.wxpay.WxApiType;
import com.ittxf.paymentwechat.common.enums.wxpay.WxNotifyType;
import com.ittxf.paymentwechat.common.enums.wxpay.WxRefundStatus;
import com.ittxf.paymentwechat.common.enums.wxpay.WxTradeState;
import com.ittxf.paymentwechat.common.exception.BusinessException;
import com.ittxf.paymentwechat.entity.OrderInfo;
import com.ittxf.paymentwechat.entity.RefundInfo;
import com.ittxf.paymentwechat.service.OrderInfoService;
import com.ittxf.paymentwechat.service.PaymentInfoService;
import com.ittxf.paymentwechat.service.RefundInfoService;
import com.ittxf.paymentwechat.service.WxPayService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 微信支付业务实现。
 *
 * <p>基于 wechatpay-apache-httpclient 完成 Native 下单：拼装 APIv3 要求的 JSON 报文，
 * 通过已装配签名能力的 {@code wxPayClient} 发出请求，从应答中取出 code_url 返回前端生成二维码。</p>
 *
 * <p>签名、验签、平台证书更新全部由 {@code wxPayClient} 内部处理，
 * 本类只需关注业务报文的组装与解析。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    /**
     * 微信支付参数配置，提供 appid、商户号、域名、回调域名等
     */
    private final WxPayConfig wxPayConfig;

    /**
     * 已自动处理签名与验签的 HTTP 客户端，由 {@link WxPayConfig#getWxPayClient} 装配
     */
    private final CloseableHttpClient wxPayClient;
    /**
     * 不校验应答签名的客户端，仅用于微信不返回签名头的接口（如下载账单文件）
     */
    private final CloseableHttpClient wxPayNoSignClient;
    private final OrderInfoService orderInfoService;
    private final ObjectMapper objectMapper;
    private final PaymentInfoService paymentInfoService;
    private final ReentrantLock lock = new ReentrantLock();
    private final RefundInfoService refundInfoService;



    /**
     * Native 下单（扫码支付）
     *
     * <p>执行步骤：生成本地订单 → 拼下单报文 → 发送签名请求 → 解析 code_url 返回。</p>
     *
     * <p>支付结果不在本方法里体现，而是由微信异步推送到 notify_url，
     * 或由前端轮询查单接口确认。</p>
     *
     * @param productId 待购买的商品 ID
     * @return 含 codeUrl（二维码内容）与 orderNo（商户订单号）的结果集
     * @throws RuntimeException 微信侧返回非 200 应答或 IO 异常时抛出
     */
    @Override
    public Map<String, Object> nativePay(Long productId) {

        log.info("生成订单");

        // 生成订单
        OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId);
        log.info("订单生成成功，订单号：{}", orderInfo.getOrderNo());

        // 这笔订单之前已经成功下过单，直接复用已落库的 code_url，避免重复请求微信
        // 注意：判断的是 codeUrl 而不是 orderNo —— orderNo 在创单时必然已赋值，用它判断会导致永远提前返回
        if (StringUtils.hasText(orderInfo.getCodeUrl())) {
            log.info("订单已存在二维码，直接复用：orderNo={}", orderInfo.getOrderNo());
            Map<String, Object> result = new HashMap<>();
            result.put("codeUrl", orderInfo.getCodeUrl());
            result.put("orderNo", orderInfo.getOrderNo());
            return result;
        }

        log.info("调用统一下单API");

        // 调用统一下单API：域名 + 接口路径拼成完整地址
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));

        // 添加post请求参数，字段名必须与微信 APIv3 文档完全一致（下划线风格）
        HashMap paramsMap = new HashMap();
        paramsMap.put("appid", wxPayConfig.getAppid()); // 公众号/小程序/开放平台 ID
        paramsMap.put("mchid", wxPayConfig.getMchId()); // 商户号
        paramsMap.put("description", orderInfo.getTitle()); // 商品描述，展示在微信账单里
        paramsMap.put("out_trade_no", orderInfo.getOrderNo()); // 商户订单号，同一商户号下必须唯一
        // 支付结果通知地址，必须是公网可访问的 HTTPS 地址
        // 回调路径统一取自 WxNotifyType，与 WxPayController 的映射一一对应：
        // 硬编码路径一旦与 Controller 不一致，微信的 POST 会落到静态资源处理器上抛 NoResourceFoundException
        paramsMap.put("notify_url",
                wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));

        // 金额信息为嵌套对象，单位为分，传元会被微信拒绝
        HashMap amountMap = new HashMap();
        amountMap.put("total", orderInfo.getTotalFee()); // 金额
        amountMap.put("currency", "CNY"); // 货币，国内固定人民币

        paramsMap.put("amount", amountMap); // 设置金额信息

        // 将参数转换为JSON字符串
        // writeValueAsString 声明抛出受检异常 JsonProcessingException（Gson 的 toJson 不抛）。
        // 受检异常若逐层 throws，会污染接口与所有调用方签名，且 @Transactional 默认不回滚受检异常，
        // 因此在发生处转为非受检的 BusinessException 继续向上抛，最终由全局异常处理器统一兜底
        String jsonParams;
        try {
            jsonParams = objectMapper.writeValueAsString(paramsMap);
        } catch (JsonProcessingException e) {
            throw new BusinessException("下单报文序列化失败", e);
        }
        log.info("请求参数：{}", jsonParams);

        // 报文体必须声明 application/json，否则微信返回 400；编码固定 UTF-8
        StringEntity entity = new StringEntity(jsonParams, "UTF-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        // 完成签名并执行请求，签名头 Authorization 由 wxPayClient 自动注入
        // 响应结果会自动完成验签，如果验签失败会抛出异常，连接会自动关闭
        try (CloseableHttpResponse response = wxPayClient.execute(httpPost)){

            String responseBody = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) { // 处理成功
                log.info("成功, 返回结果: {}", responseBody);
            } else if (statusCode == 204) { // 处理成功，无返回Body
                // 注意：204 时 responseBody 为空串，后面的 readValue 会抛 JsonProcessingException
                log.info("成功, 无返回结果");
            } else {
                // 非 2xx 多为参数非法、签名错误或证书问题，把微信原文打出来便于定位
                log.info("Native下单失败, 响应码: {}, 返回结果: {}", statusCode, responseBody);
                throw new IOException("request failed");
            }

            // 解析返回结果，HashMap.class 属于原始类型，存在 unchecked 转换
            // readValue 抛出的 JsonProcessingException 是 IOException 的子类，已由下方 catch 处理
            Map<String, String> resultMap = objectMapper.readValue(responseBody, HashMap.class);
            // 获取二维码链接，前端将其渲染成二维码供用户扫码
            String codeUrl = resultMap.get("code_url");

            // 保存二维码链接到数据库
            String orderNo = orderInfo.getOrderNo();
            orderInfoService.saveCodeUrl(orderNo, codeUrl);

            // 返回结果，key 需与前端 index.vue 读取的字段名对应
            Map<String, Object> result = new HashMap<>();
            result.put("codeUrl", codeUrl);
            result.put("orderNo", orderInfo.getOrderNo());

            return result;

        } catch (IOException e) {
            // 转为业务异常继续上抛，由全局异常处理器统一转为规范响应；cause 保留原始堆栈
            throw new BusinessException("调用微信支付统一下单接口失败", e);
        }


    }

    /**
     *
     * @param bodyMap
     */
    @Override
    public void processOrder(Map<String, Object> bodyMap) {
        log.info("处理订单");

        // 解密报文
        String plainText = decryptFromResource(bodyMap);


        try {
            // 将解密后的报文转换为 Map
            Map<String, Object> plainTextMap = objectMapper.readValue(plainText, HashMap.class);
            String outTradeNo = (String) plainTextMap.get("out_trade_no");


             /* 在对业务数据进行状态检查和处理之前，
             * 要采用数据锁进行并发控制，
             * 以避免函数重入造成的数据混乱*/
            // 尝试获取锁，成功则进行业务处理，获取锁失败则直接返回，不必一直等待锁的释放
            if (lock.tryLock()) {
                try {
                    // 防止重复处理
                    // 接口调用的幂等性：无论重复调用多少次，结果都是一样的
                    // 只有仍处于“未支付”的订单才需要处理；已是“支付成功”说明是微信重推的重复通知，直接跳过
                    String orderStatus = orderInfoService.getOrderStatus(outTradeNo);
                    if (!OrderStatus.NOTPAY.getType().equals(orderStatus)) {
                        log.info("订单已处理，跳过重复通知：outTradeNo={}, orderStatus={}", outTradeNo, orderStatus);
                        return;
                    }

                    // 模拟通知并发
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    // 更新订单状态
                    orderInfoService.updateStatusByOrderNo(outTradeNo, OrderStatus.SUCCESS);

                    // 记录支付日志
                    paymentInfoService.createPaymentInfo(plainText);
                } finally {
                    lock.unlock();
                }
            }


        } catch (JsonProcessingException e) {
            throw new BusinessException("支付通知明文解析失败，明文：" + plainText, e);
        }


    }

    /**
     * 根据订单号取消订单
     * @param orderNo
     */
    @Override
    public void cancelOrder(String orderNo) {
        // 调用微信支付的关单接口
        this.closeOrder(orderNo);

        // 更新商户端的订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);

    }

    @Override
    public String queryOrder(String orderNo) {
        log.info("查询订单：orderNo={}", orderNo);

        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        // 完成签名并执行请求，签名头 Authorization 由 wxPayClient 自动注入
        try (CloseableHttpResponse response = wxPayClient.execute(httpGet)) {

            String responseBody = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) { // 处理成功
                log.info("成功, 返回结果: {}", responseBody);
            } else if (statusCode == 204) { // 处理成功，无返回Body
                log.info("查询订单失败, 响应码: {}, 返回结果: {}", statusCode, responseBody);
                throw new IOException("request failed");
            } else {
                log.info("查询订单失败, 响应码: {}, 返回结果: {}", statusCode, responseBody);
                throw new IOException("request failed");
            }

            return responseBody;

        } catch (IOException e) {
            throw new BusinessException("查询订单失败", e);
        }

    }

    /**
     * 根据订单号查询退款
     * @param refundNo
     * @return
     */
    @Override
    public String queryRefund(String refundNo) {
        log.info("查询退款接口调用：refundNo={}", refundNo);

        String url = String.format(WxApiType.DOMESTIC_REFUNDS_QUERY.getType(), refundNo);
        url = wxPayConfig.getDomain().concat(url);

        // 创建远程Get 请求对象
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        // 完成签名并执行请求，签名头 Authorization 由 wxPayClient 自动注入
        try (CloseableHttpResponse response = wxPayClient.execute(httpGet)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) { // 处理成功
                log.info("成功, 返回结果: {}", responseBody);
            } else if (statusCode == 204) { // 处理成功，无返回Body
                log.info("成功");
            } else {
                log.info("查询退款失败, 响应码: {}, 返回结果: {}", statusCode, responseBody);
                throw new IOException("request failed");
            }

            return responseBody;

        } catch (IOException e) {
            throw new BusinessException("查询退款失败", e);
        }
    }

    /**
     * 申请退款
     * @param orderNo
     */
    @Transactional(rollbackFor = Exception.class) // 默认回滚所有异常
    @Override
    public void refund(String orderNo, String reason) {
        log.info("创建退款单记录");
        // 根据订单编号创建退款单
        RefundInfo refundInfo = refundInfoService.createRefundByOrderNo(orderNo, reason);

        log.info("调用退款API");
        log.info("创建退款单记录成功，退款单号：{}", refundInfo.getRefundNo());

        // 调用微信支付的退款接口
        String url = wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());
        HttpPost httpPost = new HttpPost(url);

        // 组装请求body参数
        Map paramsMap = new HashMap();
        paramsMap.put("out_trade_no", orderNo);//订单编号
        paramsMap.put("out_refund_no", refundInfo.getRefundNo());//退款单编号
        paramsMap.put("reason", refundInfo.getReason());//退款原因
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));//退款通知地址

        Map amountMap = new HashMap();
        amountMap.put("refund", refundInfo.getRefund());//退款金额
        amountMap.put("total", refundInfo.getTotalFee());//原订单金额
        amountMap.put("currency", "CNY");//退款币种
        paramsMap.put("amount", amountMap);

        //将参数转换成json字符串
        try {
            String jsonParams = objectMapper.writeValueAsString(paramsMap);
            log.info("请求参数 ===> {}" + jsonParams);

            StringEntity entity = new StringEntity(jsonParams,"utf-8");
            entity.setContentType("application/json");//设置请求报文格式
            httpPost.setEntity(entity);//将请求报文放入请求对象
            httpPost.setHeader("Accept", "application/json");//设置响应报文格式

            //完成签名并执行请求，并完成验签
            try (CloseableHttpResponse response = wxPayClient.execute(httpPost)) {
                //解析响应结果
                String bodyAsString = EntityUtils.toString(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    log.info("成功, 退款返回结果 = " + bodyAsString);
                } else if (statusCode == 204) {
                    log.info("成功");
                } else {
                    throw new RuntimeException("退款异常, 响应码 = " + statusCode+ ", 退款返回结果 = " + bodyAsString);
                }

                //更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_PROCESSING);

                //更新退款单
                refundInfoService.updateRefund(bodyAsString);
            }
        } catch (IOException e) {
            throw new BusinessException("微信支付退款接口调用失败", e);
        }

    }

    /**
     * 下载对账单
     * @param billDate
     * @param type
     * @return
     */
    @Override
    public String downloadBill(String billDate, String type) {
        log.warn("下载对账单接口调用 {}", billDate);

        // 获取账单url地址
        String downloadUrl = this.queryBill(billDate, type);

        // 创建远程Get 请求对象
        HttpGet httpGet = new HttpGet(downloadUrl);
        httpGet.addHeader("Accept", "application/json");

        // 下载账单文件返回的是原始 CSV 流、不带可校验的签名头，必须用跳过验签的 wxPayNoSignClient
        try (CloseableHttpResponse response = wxPayNoSignClient.execute(httpGet)) {
            String bodyAsString = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                log.info("成功, 账单返回结果 = " + bodyAsString);
            }else if (statusCode == 204) {
                log.info("成功");
            } else {
                log.info("下载对账单异常, 响应码: {}, 返回结果: {}", statusCode, bodyAsString);
                throw new IOException("request failed");
            }

            return bodyAsString;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询对账单
     * @param billDate
     * @param type
     * @return
     */
    @Override
    public String queryBill(String billDate, String type) {
        log.warn("申请账单接口调用 {}", billDate);

        String url = "";
        if ("tradebill".equals(type)) {
            url = WxApiType.TRADE_BILLS.getType();
        }else if ("fundflowbill".equals(type)) {
            url = WxApiType.FUND_FLOW_BILLS.getType();;
        }else {
            throw new BusinessException("不支持的账单类型");
        }

        url = wxPayConfig.getDomain().concat(url).concat("?bill_date=").concat(billDate);

        // 创建远程Get 请求对象
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Accept", "application/json");

        // 使用wxPayClient发送请求得到响应
        try (CloseableHttpResponse response = wxPayClient.execute(httpGet)) {
            String bodyAsString = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                log.info("成功, 账单返回结果 = " + bodyAsString);
            }else if (statusCode == 204) {
                log.info("成功");
            } else {
                log.info("申请账单异常, 响应码: {}, 返回结果: {}", statusCode, bodyAsString);
                throw new IOException("request failed");
            }

            // 获取账单下载地址
            HashMap<String, String> resultMap = objectMapper.readValue(bodyAsString, HashMap.class);
            return resultMap.get("download_url");


        } catch (IOException e) {
            throw new BusinessException("申请账单异常", e);
        }
    }

    /**
     * 处理微信退款异步通知
     *
     * <p>退款回调的 resource 同样用 APIv3 密钥加密，需先解密；
     * 再根据 refund_status 将本地订单映射为已退款/退款异常，并回写退款单。</p>
     *
     * @param bodyMap 已解析的退款通知外层报文
     */
    @Override
    public void processRefund(Map<String, Object> bodyMap) {
        log.info("处理退款通知");

        // 解密报文
        String plainText = decryptFromResource(bodyMap);

        try {
            // 将解密后的报文转换为 Map
            Map<String, Object> plainTextMap = objectMapper.readValue(plainText, HashMap.class);
            String outTradeNo = (String) plainTextMap.get("out_trade_no");
            String refundStatus = (String) plainTextMap.get("refund_status");

            // 将微信退款状态映射为本地订单状态；PROCESSING/CLOSED 等中间态不改订单状态
            if (WxRefundStatus.SUCCESS.getType().equals(refundStatus)) {
                orderInfoService.updateStatusByOrderNo(outTradeNo, OrderStatus.REFUND_SUCCESS);
            } else if (WxRefundStatus.ABNORMAL.getType().equals(refundStatus)) {
                orderInfoService.updateStatusByOrderNo(outTradeNo, OrderStatus.REFUND_ABNORMAL);
            }

            // 回写退款单：微信退款单号、退款状态、回调原文
            refundInfoService.updateRefund(plainText);

        } catch (JsonProcessingException e) {
            throw new BusinessException("退款回调报文解析失败：" + plainText, e);
        }
    }

    /**
     * 根据订单号查询微信支付查单接口，核实订单状态
     * 如果订单已支付，则更新商户端订单状态
     * 如果订单未支付，则调用关单接口关闭订单，并更新商户端订单状态
     * @param orderNo
     * @return
     */
    @Override
    public String checkOrderStatus(String orderNo) {
        log.warn("核实订单状态：orderNo={}", orderNo);

        // 调用微信支付的查单接口
        String responseBody = this.queryOrder(orderNo);

        try {
            Map resultMap = objectMapper.readValue(responseBody, HashMap.class);

            // 获取微信支付端的订单状态
            Object tradeState = resultMap.get("trade_state");

            // 判断订单状态
            if (WxTradeState.SUCCESS.getType().equals(tradeState)) {
                // 订单已支付，更新商户端订单状态
                log.info("订单已支付，更新商户端订单状态：orderNo={}", orderNo);
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            } else if (WxTradeState.NOTPAY.getType().equals(tradeState)) {
                // 订单未支付，调用关单接口关闭订单，并更新商户端订单状态
                log.info("订单未支付，调用关单接口关闭订单，并更新商户端订单状态：orderNo={}", orderNo);
                this.closeOrder(orderNo);
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
            }

            return responseBody;
        } catch (JsonProcessingException e) {
            throw new BusinessException("微信支付定时任务查单接口返回结果解析失败", e);
        }

    }

    /**
     * 关单接口
     * @param orderNo
     */
    private void closeOrder(String orderNo) {
        log.info("关单接口：orderNo={}", orderNo);

        // 创建远程请求对象
        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url);
        HttpPost httpPost = new HttpPost(url);

        // 组装json请求参数
        // 注意：out_trade_no 是路径参数，已拼在 URL 上；关单接口的 body 只允许包含 mchid，
        // 多传 out_trade_no 会被微信以“未在 API 文档中定义的参数”拒绝（INVALID_REQUEST）
        HashMap<String, String> paramsMap = new HashMap<>();

        paramsMap.put("mchid", wxPayConfig.getMchId());

        try {
            String jsonParams = objectMapper.writeValueAsString(paramsMap);
            log.info("请求参数={}", jsonParams);

            // 将请求参数设置到请求对象中
            // 报文体必须声明 application/json，否则微信返回 400；编码固定 UTF-8
            StringEntity entity = new StringEntity(jsonParams, "UTF-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");

            // 用 try-with-resources 持有响应：无论是否读 body，退出时都会自动 close() 释放连接
            try (CloseableHttpResponse response = wxPayClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) { // 处理成功
                    log.info("成功200");
                } else if (statusCode == 204) { // 处理成功，无返回Body
                    log.info("成功204");
                } else {
                    log.info("关单失败, 响应码: {}，返回结果: {}", statusCode, EntityUtils.toString(response.getEntity()));
                    throw new IOException("request failed");
                }
            }
        } catch (IOException e) {
            throw new BusinessException("关单接口调用失败", e);
        }
    }

    /**
     * 对称解密通知中的 resource 节点
     *
     * <p>微信回调报文的 resource 用 APIv3 密钥做 AES-256-GCM 加密，
     * 需用 associated_data、nonce、ciphertext 三项解密后才是真实订单报文。</p>
     *
     * <p>解密失败时转为非受检的 BusinessException 上抛：本方法处于
     * {@code nativeNotify} 调用链上，会被 Controller 的 catch (Exception) 兜住，
     * 进而应答 FAIL + 500 让微信重推通知，而不是静默丢失这笔支付结果。</p>
     *
     * @param bodyMap 已解析的通知外层报文
     * @return 解密后的订单明文 JSON
     * @throws BusinessException APIv3 密钥不匹配或报文密文损坏时抛出
     */
    private String decryptFromResource(Map<String, Object> bodyMap) {
        log.info("密文解密");

        // 通知内容
        Map<String, Object> resourceMap = (Map<String, Object>) bodyMap.get("resource");
        // 数据密文
        String ciphertext = (String) resourceMap.get("ciphertext");
        // 随机串
        String nonce = (String) resourceMap.get("nonce");
        // 附加数据
        String associatedData = (String) resourceMap.get("associated_data");

        log.info("数据密文：{}", ciphertext);

        try {
            AesUtil aesUtil = new AesUtil(wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));
            String plainText = aesUtil.decryptToString(
                    associatedData.getBytes(StandardCharsets.UTF_8),
                    nonce.getBytes(StandardCharsets.UTF_8),
                    ciphertext);

            log.info("解密后内容：{}", plainText);

            return plainText;
        } catch (GeneralSecurityException e) {
            // decryptToString 声明抛出受检异常，不能直接 throws 上抛（会传染到 Controller），
            // 在此转为非受检业务异常，cause 保留原始堆栈便于定位是密钥错还是密文损坏
            throw new BusinessException("支付通知解密失败，请确认 APIv3 密钥配置与商户号匹配", e);
        }
    }
}
