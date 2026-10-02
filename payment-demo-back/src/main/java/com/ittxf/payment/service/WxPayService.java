package com.ittxf.payment.service;

import java.util.Map;

/**
 * 微信支付业务接口。
 *
 * <p>抽象商户后台调用微信支付各能力的方法，目前仅包含 Native 下单，
 * 后续查单、关单、退款、账单下载等可在此追加。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
public interface WxPayService {

    /**
     * Native 下单（扫码支付）
     *
     * <p>完整流程：生成本地订单 → 调用微信统一下单接口 → 取出 code_url 返回前端生成二维码。</p>
     *
     * @param productId 商品 ID
     * @return 至少包含 codeUrl（二维码内容）与 orderNo（商户订单号）两个键
     */
    Map<String, Object> nativePay(Long productId);

    void processOrder(Map<String, Object> bodyMap);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    String checkOrderStatus(String orderNo);

    void refund(String orderNo, String reason);

    String queryRefund(String orderNo);

    /**
     * 处理微信退款异步通知：解密报文、按退款状态更新本地订单与退款单
     *
     * @param bodyMap 已解析的退款通知外层报文
     */
    void processRefund(Map<String, Object> bodyMap);

    String queryBill(String billDate, String type);

    String downloadBill(String billDate, String type);
}
