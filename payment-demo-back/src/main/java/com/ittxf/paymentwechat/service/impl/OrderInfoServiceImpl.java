package com.ittxf.paymentwechat.service.impl;

import com.ittxf.paymentwechat.entity.OrderInfo;
import com.ittxf.paymentwechat.mapper.OrderInfoMapper;
import com.ittxf.paymentwechat.service.OrderInfoService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

}
