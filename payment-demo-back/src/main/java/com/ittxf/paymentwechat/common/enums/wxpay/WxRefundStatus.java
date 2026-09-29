package com.ittxf.paymentwechat.common.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 微信支付退款结果状态枚举。
 *
 * <p>对应微信退款回调报文中 {@code refund_status} 字段的原始英文值，
 * 用于将微信返回的状态映射为本地 {@link com.ittxf.paymentwechat.common.enums.OrderStatus}。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@AllArgsConstructor
@Getter
public enum WxRefundStatus {

    /**
     * 退款成功
     */
    SUCCESS("SUCCESS"),

    /**
     * 退款关闭
     */
    CLOSED("CLOSED"),

    /**
     * 退款处理中
     */
    PROCESSING("PROCESSING"),

    /**
     * 退款异常
     */
    ABNORMAL("ABNORMAL");

    /**
     * 微信侧退款状态原值，与报文字符串完全一致，不可随意改动
     */
    private final String type;
}
