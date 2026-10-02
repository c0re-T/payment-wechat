package com.ittxf.payment.service;

import java.util.Map;

public interface PaymentInfoService {

    void createPaymentInfo(String plainText);

    void createPaymentInfoAliPay(Map<String, String> params);
}
