package com.ittxf.paymentwechat.common.result;

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
@Builder // 核心注解：生成链式建造者 API
@NoArgsConstructor // 配合 Jackson 反序列化和无参构造
@AllArgsConstructor // 配合 @Builder 生成全参构造
public class R<T> implements Serializable { // 当程序是微服务时，需要全局实体类实现 Serializable 接口，单体项目可不实现

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
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
     * 成功返回，无数据
     *
     * @return 统一响应结果
     */
    public static <T> R<T> success() {
        return R.<T>success(ResultCodeEnum.SUCCESS.getMessage(), null);
    }

    /**
     * 成功返回，携带数据
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
        return R.<T>builder()
                .code(ResultCodeEnum.SUCCESS.getCode())
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 失败返回，状态码取枚举 FAIL
     *
     * @param message 错误提示信息
     * @return 统一响应结果
     */
    public static <T> R<T> fail(String message) {
        return R.<T>fail(ResultCodeEnum.FAIL.getCode(), message);
    }

    /**
     * 失败返回，自定义错误码和提示信息
     *
     * @param code    错误状态码
     * @param message 错误提示信息
     * @return 统一响应结果
     */
    public static <T> R<T> fail(Integer code, String message) {
        return R.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
