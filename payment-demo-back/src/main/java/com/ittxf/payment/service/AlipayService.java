package com.ittxf.payment.service;


import java.util.Map;

public interface AlipayService {

    String tradeCreate(Long productId);

    void processOrder(Map<String, String> params);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    /**
     * 核实订单状态（定时对账兜底）：主动查支付宝，以支付宝侧结果纠正本地订单状态
     * @param orderNo 商户订单号
     */
    void checkOrderStatus(String orderNo);

    /**
     * 查询支付宝对账单的下载地址（alipay.data.dataservice.bill.downloadurl.query）
     * @param billDate 账单日期，格式 yyyy-MM-dd（日账单）或 yyyy-MM（月账单）
     * @param type 账单类型：trade（交易账单）/ signcustomer（资金账单）
     * @return 账单文件的下载地址
     */
    String queryBill(String billDate, String type);

    /**
     * 申请退款（alipay.trade.refund）
     * @param orderNo 商户订单号
     * @param reason 退款原因
     */
    void refund(String orderNo, String reason);
}
