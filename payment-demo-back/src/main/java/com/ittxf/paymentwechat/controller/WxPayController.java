package com.ittxf.paymentwechat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ittxf.paymentwechat.common.enums.wxpay.WxNotifyResult;
import com.ittxf.paymentwechat.common.result.R;
import com.ittxf.paymentwechat.common.util.HttpUtils;
import com.ittxf.paymentwechat.common.util.WechatPay2ValidatorForRequest;
import com.ittxf.paymentwechat.service.OrderInfoService;
import com.ittxf.paymentwechat.service.WxPayService;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            // 测试时可用，模拟接受微信端的重复通知
            TimeUnit.SECONDS.sleep(5); // 单位：秒

            // 处理成功：返回 SUCCESS，微信不再重推
            return ResponseEntity.ok(WxNotifyResult.SUCCESS.toResponseBody());
        } catch (Exception e) {
            // 任何异常都必须在此就地兜住，返回 FAIL + 500：
            // 一是应答体要是微信认识的格式，二是非 2xx 才能让微信重推，两者缺一不可
            log.error("处理微信支付通知失败，微信将重试", e);
            return ResponseEntity.internalServerError().body(WxNotifyResult.FAIL.toResponseBody());
        }
    }

    /**
     * 取消（关闭）未支付的订单
     *
     * <p>路径需与前端 wxPay.js 的 cancel 保持一致；先调微信关单，
     * 失败时服务层抛异常由全局异常处理器兜底，不会把未关成功的订单误标为已取消。</p>
     *
     * @param orderNo 商户订单号
     * @return 统一响应，成功 code 为 200
     */
    @PostMapping("/cancel/{orderNo}")
    @Operation(summary = "取消订单", description = "调用微信关单接口并将本地订单置为已取消")
    public R cancelOrder(@PathVariable("orderNo") String orderNo) {
        log.info("取消订单，orderNo：{}", orderNo);
        wxPayService.cancelOrder(orderNo);
        return R.success("订单已取消", null);
    }

    /**
     * 查询订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    @GetMapping("/query/{orderNo}")
    @Operation(summary = "查询订单", description = "根据商户订单号查询订单状态")
    public R queryOrder(@PathVariable("orderNo") String orderNo) {
        log.info("查询订单，orderNo：{}", orderNo);
        return R.success("查询订单成功", wxPayService.queryOrder(orderNo));
    }

    /**
     * 申请退款
     * @param orderNo 订单号
     * @return 统一响应，成功 code 为 200，数据为退款申请结果
     */
    @PostMapping("/refunds/{orderNo}/{reason}")
    @Operation(summary = "申请退款", description = "根据商户订单号申请退款")
    public R refunds(@PathVariable("orderNo") String orderNo,
                     @PathVariable("reason") String reason) {
        log.info("申请退款，orderNo：{}", orderNo);
        wxPayService.refund(orderNo, reason);
        return R.success("退款申请成功", null);
    }

    /**
     * 查询退款
     * @param refundNo 退款号
     * @return 退款信息
     */
    @GetMapping("/query-refund/{refundNo}")
    @Operation(summary = "查询退款", description = "根据商户订单号查询退款状态")
    public R queryRefund(@PathVariable("refundNo") String refundNo) {
        log.info("查询退款，orderNo：{}", refundNo);
        return R.success("查询退款成功", wxPayService.queryRefund(refundNo));
    }

    /**
     * 微信退款异步通知
     * @param request 微信回调的原始请求
     * @return 微信约定格式的应答，处理失败时附带 500 状态码以触发重试
     */
    @PostMapping("/refunds/notify")
    @Operation(summary = "微信退款异步通知", description = "接收微信退款结果通知")
    public ResponseEntity<Map<String, String>> refundsNotify(HttpServletRequest request) {
        log.info("退款通知执行");

        // 验签与解密都必须使用微信发来的原始报文体，故手动读流，不能改用 @RequestBody
        String body = HttpUtils.readData(request);

        try {
            HashMap<String, Object> bodyMap = objectMapper.readValue(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("退款通知，requestId：{}", requestId);

            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest =
                    new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("微信退款异步通知验签失败");
                // 验签失败：返回 FAIL，微信会重试
                return ResponseEntity.badRequest().body(WxNotifyResult.FAIL.toResponseBody());
            }
            log.info("微信退款异步通知验签成功");

            // 处理退款
            wxPayService.processRefund(bodyMap);

            // 处理成功：返回 SUCCESS，微信不再重推
            return ResponseEntity.ok(WxNotifyResult.SUCCESS.toResponseBody());
        } catch (Exception e) {
            // 任何异常都必须在此就地兜住，返回 FAIL + 500：
            // 一是应答体要是微信认识的格式，二是非 2xx 才能让微信重推，两者缺一不可
            log.error("处理微信退款通知失败，微信将重试", e);
            return ResponseEntity.internalServerError().body(WxNotifyResult.FAIL.toResponseBody());
        }
    }

    /**
     * 查询交易账单
     * @param billDate 账单日期
     * @param type 账单类型
     * @return 账单信息
     */
    @GetMapping("/querybill/{billDate}/{type}")
    @Operation(summary = "查询交易账单", description = "根据账单日期查询交易账单")
    public R queryTradeBill(
            @PathVariable("billDate") String billDate,
            @PathVariable("type") String type) {
        log.info("查询交易账单，billDate：{}", billDate);
        return R.success("查询交易账单成功", wxPayService.queryBill(billDate, type));
    }

    @GetMapping("/downloadbill/{billDate}/{type}")
    @Operation(summary = "下载交易账单", description = "根据账单日期下载交易账单")
    public R downloadBill(
            @PathVariable("billDate") String billDate,
            @PathVariable("type") String type) {
        log.info("下载交易账单，billDate：{}", billDate);
        String csv = wxPayService.downloadBill(billDate, type);
        // 账单正文为 CSV 文本，放入 data 返回前端，由前端拼成带 BOM 的 .csv 下载
        return R.success("下载交易账单成功", csv);
    }

}
