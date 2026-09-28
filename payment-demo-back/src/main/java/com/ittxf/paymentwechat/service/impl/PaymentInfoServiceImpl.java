package com.ittxf.paymentwechat.service.impl;

import com.ittxf.paymentwechat.entity.PaymentInfo;
import com.ittxf.paymentwechat.mapper.PaymentInfoMapper;
import com.ittxf.paymentwechat.service.PaymentInfoService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

}
