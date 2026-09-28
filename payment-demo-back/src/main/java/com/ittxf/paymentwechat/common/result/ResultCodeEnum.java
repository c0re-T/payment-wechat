package com.ittxf.paymentwechat.common.result;

import lombok.Getter;

/**
 * 统一响应状态码枚举，集中维护 code 与默认提示信息。
 *
 * @author txf
 * @since 2026-09-28
 */
@Getter
public enum ResultCodeEnum {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 请求参数校验失败
     */
    PARAM_ERROR(400, "请求参数不合法"),

    /**
     * 业务处理失败
     */
    FAIL(500, "操作失败，请稍后重试");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 默认提示信息
     */
    private final String message;

    /**
     * 枚举构造函数，实例创建后 code 与 message 不可变
     *
     * @param code    状态码
     * @param message 默认提示信息
     */
    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
