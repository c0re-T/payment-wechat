package com.ittxf.paymentwechat.controller;

import com.ittxf.paymentwechat.common.result.R;
import com.ittxf.paymentwechat.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 微信支付入口 Controller。
 *
 * <p>只负责参数接收、日志记录与统一响应包装，不写具体业务逻辑，
 * 真正的下单调用由 {@link WxPayService} 完成。</p>
 *
 * <p>路径前缀 {@code /api/wx-pay} 需与前端 wxPay.js 以及
 * {@link com.ittxf.paymentwechat.common.enums.wxpay.WxNotifyType} 中的回调路径保持一致，
 * 不一致时请求会落到静态资源处理器上抛 NoResourceFoundException。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@RestController
@RequestMapping("/api/wx-pay") // 得和前端的接口地址保持一致 wxPay.js
@RequiredArgsConstructor // 构造器注入，配合 final 字段保证依赖不可变
@Tag(name = "网站微信支付API", description = "微信支付相关接口")
@Slf4j
public class WxPayController {

    private final WxPayService wxPayService;

    /**
     * 调用统一下单 API，生成支付二维码
     *
     * <p>前端拿到返回的 codeUrl 后自行渲染二维码，并轮询订单状态确认支付结果。</p>
     *
     * @param productId 路径参数，待购买的商品 ID
     * @return 统一响应，data 中含 codeUrl（二维码内容）与 orderNo（商户订单号）
     */
    @Operation(summary = "调用统一下单API，生成支付二维码", description = "微信支付统一下单接口")
    @PostMapping("/native/{productId}")
    public R unifiedOrder(@PathVariable("productId") Long productId) {
        log.info("生成订单， productId：{}", productId);
        // 返回支付二维码和链接
        Map<String, Object> result = wxPayService.nativePay(productId);
        return R.success(result);
    }
}
