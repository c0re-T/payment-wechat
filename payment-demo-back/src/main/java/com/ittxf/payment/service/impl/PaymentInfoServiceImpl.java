package com.ittxf.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ittxf.payment.common.enums.PayType;
import com.ittxf.payment.common.exception.BusinessException;
import com.ittxf.payment.entity.PaymentInfo;
import com.ittxf.payment.mapper.PaymentInfoMapper;
import com.ittxf.payment.service.PaymentInfoService;
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
            // 获取用户实际支付金额：payer_total 不是顶层字段，嵌套在 amount 子对象里
            Map<String, Object> amount = (Map) plainTextMap.get("amount");
            // amount 子对象内的金额为整数，Jackson 反序列化为 Integer，用 Number 统一取 int 值
            Integer payerTotal = ((Number) amount.get("payer_total")).intValue();


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
            throw new BusinessException("创建微信支付信息失败");
        }


    }

    /**
     * 创建支付信息（支付宝）
     * @param params
     */
    @Override
    public void createPaymentInfoAliPay(Map<String, String> params) {
        log.info("创建支付信息（支付宝）");

        try {
            PaymentInfo paymentInfo = new PaymentInfo();
            // 获取订单号
            paymentInfo.setOrderNo(params.get("out_trade_no"));
            // 获取支付类型
            paymentInfo.setPaymentType(PayType.ALIPAY.getType());
            // 获取交易号
            paymentInfo.setTransactionId(params.get("trade_no"));
            // 获取交易类型（支付宝异步通知无 trade_type 字段，取不到则为空）
            paymentInfo.setTradeType(params.get("trade_type"));
            // 获取交易状态：支付宝异步通知的字段名是 trade_status（不是微信的 trade_state）
            paymentInfo.setTradeState(params.get("trade_status"));
            // 获取用户实际支付金额
            BigDecimal payerTotal = new BigDecimal(params.get("total_amount")).multiply(new BigDecimal(100));
            paymentInfo.setPayerTotal(payerTotal.intValue());
            // 获取支付信息
            paymentInfo.setContent(objectMapper.writeValueAsString(params));

            this.save(paymentInfo);

        } catch (JsonProcessingException e) {
            throw new BusinessException("创建阿里支付信息失败");
        }
    }
}
