package com.ittxf.payment.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付渠道枚举，维护本地库 t_payment_info.pay_type 字段的取值。
 *
 * <p>用于区分一笔支付到底走的是微信还是支付宝，对账与退款时需要据此选择对应的接口。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@AllArgsConstructor
@Getter
public enum PayType {
    /**
     * 微信
     */
    WXPAY("微信"),


    /**
     * 支付宝
     */
    ALIPAY("支付宝");

    /**
     * 渠道文本，直接存入数据库
     */
    private final String type;
}
