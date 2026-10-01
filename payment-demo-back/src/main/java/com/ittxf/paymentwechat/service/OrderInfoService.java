package com.ittxf.paymentwechat.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.ittxf.paymentwechat.common.enums.OrderStatus;
import com.ittxf.paymentwechat.entity.OrderInfo;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {

    OrderInfo createOrderByProductId(Long productId);

    void saveCodeUrl(String orderNo, String codeUrl);

    List<OrderInfo> listOrderByCreateTimeDesc();

    void updateStatusByOrderNo(String outTradeNo, OrderStatus orderStatus);

    String getOrderStatus(String outTradeNo);

    List<OrderInfo> getNoPayOrderByDuration(int i);

    OrderInfo getOrderByOrderNo(String orderNo);
}
