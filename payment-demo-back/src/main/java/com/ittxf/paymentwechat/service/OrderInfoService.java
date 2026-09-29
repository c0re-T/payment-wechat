package com.ittxf.paymentwechat.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.ittxf.paymentwechat.entity.OrderInfo;

public interface OrderInfoService extends IService<OrderInfo> {

    OrderInfo createOrderByProductId(Long productId);

    void saveCodeUrl(String orderNo, String codeUrl);
}
