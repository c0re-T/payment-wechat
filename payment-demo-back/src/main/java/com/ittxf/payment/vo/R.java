package com.ittxf.payment.vo;

import com.ittxf.payment.common.result.ResultCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果，包装业务数据返回给前端。
 *
 * @author txf
 * @since 2026-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码，取值见 ResultCodeEnum
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 业务数据
     */
    private T data;

    /**
     * 成功返回，无数据，提示信息取枚举默认值
     *
     * @return 统一响应结果
     */
    public static <T> R<T> success() {
        return R.<T>success(ResultCodeEnum.SUCCESS.getMessage(), null);
    }

    /**
     * 成功返回，携带数据，提示信息取枚举默认值
     *
     * @param data 业务数据
     * @return 统一响应结果
     */
    public static <T> R<T> success(T data) {
        return R.<T>success(ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    /**
     * 成功返回，自定义提示信息并携带数据
     *
     * @param message 提示信息
     * @param data    业务数据
     * @return 统一响应结果
     */
    public static <T> R<T> success(String message, T data) {
        return R.<T>of(ResultCodeEnum.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回，状态码与提示信息均取自枚举
     *
     * @param resultCode 响应状态码枚举
     * @return 统一响应结果
     */
    public static <T> R<T> fail(ResultCodeEnum resultCode) {
        return R.<T>of(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败返回，使用枚举默认失败状态码，自定义提示信息
     *
     * @param message 错误提示信息
     * @return 统一响应结果
     */
    public static <T> R<T> fail(String message) {
        return R.<T>fail(ResultCodeEnum.FAIL, message);
    }

    /**
     * 失败返回，自定义提示信息，用于在枚举默认文案后追加异常详情
     *
     * @param resultCode 响应状态码枚举
     * @param message    错误提示信息
     * @return 统一响应结果
     */
    public static <T> R<T> fail(ResultCodeEnum resultCode, String message) {
        return R.<T>of(resultCode.getCode(), message, null);
    }

    /**
     * 失败返回，状态码不在枚举范围内时使用，如透传第三方错误码
     *
     * @param code    错误状态码
     * @param message 错误提示信息
     * @return 统一响应结果
     */
    public static <T> R<T> fail(Integer code, String message) {
        return R.<T>of(code, message, null);
    }

    /**
     * 唯一的实例构建入口，其余工厂方法均委托至此，避免构造逻辑重复
     *
     * @param code    状态码
     * @param message 提示信息
     * @param data    业务数据
     * @return 统一响应结果
     */
    private static <T> R<T> of(Integer code, String message, T data) {
        return R.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
