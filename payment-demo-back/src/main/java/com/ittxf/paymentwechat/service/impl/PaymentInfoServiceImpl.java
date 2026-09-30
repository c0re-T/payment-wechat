package com.ittxf.paymentwechat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ittxf.paymentwechat.common.enums.PayType;
import com.ittxf.paymentwechat.common.exception.BusinessException;
import com.ittxf.paymentwechat.entity.PaymentInfo;
import com.ittxf.paymentwechat.mapper.PaymentInfoMapper;
import com.ittxf.paymentwechat.service.PaymentInfoService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    private final ObjectMapper objectMapper;

    @Override
    public void createPaymentInfo(String plainText) {

        log.info("创建支付信息");

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Map plainTextMap = objectMapper.readValue(plainText, HashMap.class);

            // 获取订单号
            String orderNo = (String) plainTextMap.get("out_trade_no");
            // 获取交易号
            String transactionId = (String) plainTextMap.get("transaction_id");
            // 获取交易类型
            String tradeType = (String) plainTextMap.get("trade_type");
            // 获取交易状态
            String tradeState = (String) plainTextMap.get("trade_state");
            // 获取用户实际支付金额
            Map<String, Object> amount = (Map) plainTextMap.get("payer_total");
            Integer payerTotal = ((BigDecimal) amount.get("payer_total")).intValue();


            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderNo(orderNo);
            paymentInfo.setPaymentType(PayType.WXPAY.getType());
            paymentInfo.setTransactionId(transactionId);
            paymentInfo.setTradeType(tradeType);
            paymentInfo.setTradeState(tradeState);
            paymentInfo.setPayerTotal(payerTotal);
            // 获取支付信息
            paymentInfo.setContent(plainText);
            this.save(paymentInfo);

        } catch (JsonProcessingException e) {
            throw new BusinessException("创建支付信息失败");
        }


    }
}
