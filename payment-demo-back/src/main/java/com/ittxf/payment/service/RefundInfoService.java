package com.ittxf.payment.service;

import com.ittxf.payment.entity.RefundInfo;
import com.baomidou.mybatisplus.spring.service.IService;

public interface RefundInfoService extends IService<RefundInfo> {

    RefundInfo createRefundByOrderNo(String orderNo, String reason);

    void updateRefund(String bodyAsString);

    void updateRefundForAlipay(String refundNo, String body, String status);
}
