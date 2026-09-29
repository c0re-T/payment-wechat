package com.ittxf.paymentwechat.service.impl;

import com.google.gson.Gson;
import com.ittxf.paymentwechat.common.config.WxPayConfig;
import com.ittxf.paymentwechat.common.enums.wxpay.WxApiType;
import com.ittxf.paymentwechat.entity.OrderInfo;
import com.ittxf.paymentwechat.service.OrderInfoService;
import com.ittxf.paymentwechat.service.WxPayService;
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
        Gson gson = new Gson();
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
        String jsonParams = gson.toJson(paramsMap);
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
                // 注意：204 时 responseBody 为空串，后面的 fromJson 会返回 null 导致 NPE
                log.info("成功, 无返回结果");
            } else {
                // 非 2xx 多为参数非法、签名错误或证书问题，把微信原文打出来便于定位
                log.info("Native下单失败, 响应码: {}, 返回结果: {}", statusCode, responseBody);
                throw new IOException("request failed");
            }

            // 解析返回结果，HashMap.class 属于原始类型，存在 unchecked 转换
            Map<String, String> resultMap = gson.fromJson(responseBody, HashMap.class);
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
            // 包装成 RuntimeException 交给全局异常处理器统一返回失败响应
            throw new RuntimeException(e);
        }


    }
}
