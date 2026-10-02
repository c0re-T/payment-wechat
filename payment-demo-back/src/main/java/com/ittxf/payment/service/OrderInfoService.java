package com.ittxf.payment.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.ittxf.payment.common.enums.OrderStatus;
import com.ittxf.payment.entity.OrderInfo;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {

    OrderInfo createOrderByProductId(Long productId, String type);

    void saveCodeUrl(String orderNo, String codeUrl);

    List<OrderInfo> listOrderByCreateTimeDesc();

    void updateStatusByOrderNo(String outTradeNo, OrderStatus orderStatus);

    String getOrderStatus(String outTradeNo);

    /**
     * 获取创建超过指定分钟数、仍未支付、且属于指定支付渠道的订单
     * @param i 超时分钟数
     * @param paymentType 支付渠道（PayType.getType()），用于隔离微信/支付宝对账
     */
    List<OrderInfo> getNoPayOrderByDuration(int i, String paymentType);

    OrderInfo getOrderByOrderNo(String orderNo);
}
