package com.ittxf.paymentwechat.common.exception;

import com.ittxf.paymentwechat.common.result.R;
import com.ittxf.paymentwechat.common.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常兜底处理器，统一拦截 Controller 抛出的未处理异常并封装为标准响应。
 *
 * @author txf
 * @since 2026-09-29
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理，向前端透传异常信息
     *
     * @param e 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理，返回参数不合法提示
     *
     * @param e 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数校验异常: {}", e.getMessage());
        return R.fail(ResultCodeEnum.PARAM_ERROR.getCode(), ResultCodeEnum.PARAM_ERROR.getMessage());
    }

    /**
     * 全局异常兜底，捕获所有未被上述处理器拦截的异常，避免堆栈信息泄露给前端
     *
     * @param e 未知异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常兜底: {}", e.getMessage(), e);
        return R.fail(ResultCodeEnum.FAIL.getCode(), ResultCodeEnum.FAIL.getMessage());
    }
}
