package com.ittxf.payment.common.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 微信支付回调应答结果枚举。
 *
 * <p>微信依据 HTTP 状态码判断商户是否成功接收通知，应答体中的 {@code code}
 * 仅作失败说明之用。其取值必须是字符串 {@code SUCCESS} / {@code FAIL}，
 * 与本项目统一响应体 {@link com.ittxf.payment.common.result.R} 的
 * Integer 状态码是两套互不相干的协议，不要混用。</p>
 *
 * @author txf
 * @since 2026-09-30
 */
@AllArgsConstructor
@Getter
public enum WxNotifyResult {

    /**
     * 处理成功，应答 HTTP 200，微信不再重推通知
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 处理失败，需配合非 2xx 状态码应答，微信才会按策略重试
     */
    FAIL("FAIL", "处理失败");

    /**
     * 应答体 code 字段，微信规定的字符串取值，不可改动
     */
    private final String code;

    /**
     * 应答体 message 字段，面向微信的可读说明
     */
    private final String message;

    /**
     * 转为微信要求的应答体结构
     *
     * <p>返回不可变 Map，由 Spring 的 Jackson 消息转换器序列化为
     * {@code {"code":"...","message":"..."}}，同时保证响应
     * {@code Content-Type} 为 application/json。</p>
     *
     * @return 应答体
     */
    public Map<String, String> toResponseBody() {
        return Map.of("code", this.code, "message", this.message);
    }
}
