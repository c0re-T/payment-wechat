package com.ittxf.paymentwechat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ittxf.paymentwechat.common.enums.wxpay.WxNotifyResult;
import com.ittxf.paymentwechat.common.result.R;
import com.ittxf.paymentwechat.common.util.HttpUtils;
import com.ittxf.paymentwechat.common.util.WechatPay2ValidatorForRequest;
import com.ittxf.paymentwechat.service.WxPayService;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private final ObjectMapper objectMapper;
    private final Verifier verifier;

    /**
     * 调用统一下单 API，生成支付二维码
     *
     * <p>前端拿到返回的 codeUrl 后自行渲染二维码，并轮询订单状态确认支付结果。</p>
     *
     * @param productId 路径参数，待购买的商品 ID
     * @return 统一响应，data 中含 codeUrl（二维码内容）与 orderNo（商户订单号）
     */
    @PostMapping("/native/{productId}")
    @Operation(summary = "调用统一下单API，生成支付二维码", description = "微信支付统一下单接口")
    public R unifiedOrder(@PathVariable("productId") Long productId) {
        log.info("生成订单， productId：{}", productId);
        // 返回支付二维码和链接
        Map<String, Object> result = wxPayService.nativePay(productId);
        return R.success(result);
    }

    /**
     * 微信支付异步通知
     *
     * <p><b>本接口是「Controller 不处理异常」原则的例外，必须自行捕获。</b>
     * 微信规定应答体必须是 {@code {"code":"SUCCESS/FAIL"}} 结构的 JSON，
     * 并依据 HTTP 状态码决定是否重推。若让异常飞到全局异常处理器，
     * 会返回前端用的 {@code R} 结构且状态码仍为 200，
     * 微信会误判为接收成功而永不重试，导致订单静默停留在未支付状态。</p>
     *
     * @param request 微信回调的原始请求
     * @return 微信约定格式的应答，处理失败时附带 500 状态码以触发重试
     */
    @PostMapping("/native/notify")
    @Operation(summary = "微信支付异步通知", description = "接收微信支付结果通知")
    public ResponseEntity<Map<String, String>> nativeNotify(HttpServletRequest request) {

        // 验签与解密都必须使用微信发来的原始报文体，故手动读流，不能改用 @RequestBody
        String body = HttpUtils.readData(request);
        log.info("收到微信支付异步通知：body：{}", body);

        try {
            // 模拟异常
            // int a = 1 / 0;

            // 处理通知参数，将前端传来的 JSON 字符串转换为 Map
            Map<String, Object> bodyMap = objectMapper.readValue(body, HashMap.class);
            String id = (String) bodyMap.get("id");
            log.info("微信支付异步通知id，id：{}", id);

            // 签名的验证
            WechatPay2ValidatorForRequest validator =
                    new WechatPay2ValidatorForRequest(verifier, id, body);
            if (!validator.validate(request)) {
                log.error("微信支付异步通知验签失败");
                // 验签失败：返回 FAIL，微信会重试
                return ResponseEntity.badRequest().body(WxNotifyResult.FAIL.toResponseBody());
            }
            log.info("微信支付异步通知验签成功");

            // 处理订单
            wxPayService.processOrder(bodyMap);

            // 模拟超时
            // TimeUnit.SECONDS.sleep(5); // 单位：秒

            // 处理成功：返回 SUCCESS，微信不再重推
            return ResponseEntity.ok(WxNotifyResult.SUCCESS.toResponseBody());
        } catch (Exception e) {
            // 任何异常都必须在此就地兜住，返回 FAIL + 500：
            // 一是应答体要是微信认识的格式，二是非 2xx 才能让微信重推，两者缺一不可
            log.error("处理微信支付通知失败，微信将重试", e);
            return ResponseEntity.internalServerError().body(WxNotifyResult.FAIL.toResponseBody());
        }
    }
}
