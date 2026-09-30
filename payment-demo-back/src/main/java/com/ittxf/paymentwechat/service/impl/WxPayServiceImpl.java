package com.ittxf.paymentwechat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ittxf.paymentwechat.common.config.WxPayConfig;
import com.ittxf.paymentwechat.common.enums.OrderStatus;
import com.ittxf.paymentwechat.common.enums.wxpay.WxApiType;
import com.ittxf.paymentwechat.common.exception.BusinessException;
import com.ittxf.paymentwechat.entity.OrderInfo;
import com.ittxf.paymentwechat.service.OrderInfoService;
import com.ittxf.paymentwechat.service.PaymentInfoService;
import com.ittxf.paymentwechat.service.WxPayService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

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
    private final OrderInfoService orderInfoService;
    private final ObjectMapper objectMapper;
    private final PaymentInfoService paymentInfoService;



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
        // 注意：此处硬编码的 /wxpay/notify 与 WxNotifyType.NATIVE_NOTIFY 不一致，实现回调时需统一
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat("/wxpay/notify"));

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

        try {
            // 完成签名并执行请求，签名头 Authorization 由 wxPayClient 自动注入
            CloseableHttpResponse response = wxPayClient.execute(httpPost);

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

            // 更新订单状态
            orderInfoService.updateStatusByOrderNo(outTradeNo, OrderStatus.SUCCESS);

            // 记录支付日志
            paymentInfoService.createPaymentInfo(plainText);


        } catch (JsonProcessingException e) {
            throw new BusinessException("支付通知明文解析失败，明文：" + plainText, e);
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
