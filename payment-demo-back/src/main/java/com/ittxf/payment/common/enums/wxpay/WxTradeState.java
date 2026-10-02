package com.ittxf.payment.common.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 微信支付交易状态枚举。
 *
 * <p>对应支付结果回调与查单接口报文中的 {@code trade_state} 字段原值，
 * 后台根据它判断是否要把本地订单改为已支付。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@AllArgsConstructor
@Getter
public enum WxTradeState {

    /**
     * 支付成功
     */
    SUCCESS("SUCCESS"),

    /**
     * 未支付
     */
    NOTPAY("NOTPAY"),

    /**
     * 已关闭
     */
    CLOSED("CLOSED"),

    /**
     * 转入退款
     */
    REFUND("REFUND");

    /**
     * 微信侧交易状态原值，与报文字符串完全一致，不可随意改动
     */
    private final String type;
}
