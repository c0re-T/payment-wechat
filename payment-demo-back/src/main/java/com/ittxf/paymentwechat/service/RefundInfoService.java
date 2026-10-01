package com.ittxf.paymentwechat.service;

import com.ittxf.paymentwechat.entity.RefundInfo;
import com.baomidou.mybatisplus.spring.service.IService;

public interface RefundInfoService extends IService<RefundInfo> {

    RefundInfo createRefundByOrderNo(String orderNo, String reason);

    void updateRefund(String bodyAsString);
}
