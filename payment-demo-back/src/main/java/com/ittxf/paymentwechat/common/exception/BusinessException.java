package com.ittxf.paymentwechat.common.exception;

import com.ittxf.paymentwechat.common.result.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常，用于在 Service/Controller 中主动抛出可预期的业务错误，由全局异常处理器统一拦截。
 *
 * @author txf
 * @since 2026-09-29
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误状态码
     */
    private final Integer code;

    /**
     * 使用枚举默认状态码与提示信息构造业务异常
     *
     * @param resultCode 响应状态码枚举
     */
    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用枚举默认状态码、自定义提示信息构造业务异常
     *
     * @param resultCode 响应状态码枚举
     * @param message    自定义错误提示信息
     */
    public BusinessException(ResultCodeEnum resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 使用自定义状态码与提示信息构造业务异常
     *
     * @param code    错误状态码
     * @param message 错误提示信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用默认失败状态码、自定义提示信息构造业务异常
     *
     * @param message 错误提示信息
     */
    public BusinessException(String message) {
        this(ResultCodeEnum.FAIL, message);
    }
}
