package com.ittxf.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.internal.util.AlipaySignature;
import com.ittxf.payment.common.enums.OrderStatus;
import com.ittxf.payment.common.result.R;
import com.ittxf.payment.entity.OrderInfo;
import com.ittxf.payment.service.AlipayService;
import com.ittxf.payment.service.OrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/ali-pay")
@Tag(name = "支付宝支付", description = "支付宝支付相关接口")
@Slf4j
@RequiredArgsConstructor
public class AlipayController {

    private final AlipayService alipayService;
    private final Environment environment;
    private final OrderInfoService orderInfoService;

    @PostMapping("/trade/page/pay/{productId}")
    @Operation(summary = "PC端支付", description = "PC端支付接口")
    public R tradePagePay(@PathVariable("productId") Long productId) {
        log.info("统一收单下单并支付页面接口的调用");

        // 支付宝开放平台接受 request 请求对象后，会返回一个 form 表单
        // 会为开发者生成一个 html 形式的form表单，包含自动提交，只需要在页面中嵌入这个form表单即可
        String formStr = alipayService.tradeCreate(productId);

        // 返回 form 表单给前端，表单会自动提交到action属性所指向的支付宝开放平台中，从而为用户展示一个支付页面
        // 前端接收到 form 表单后，需要将 form 表单嵌入到页面中
        return R.success(formStr);
    }

    @PostMapping("/trade/notify")
    @Operation(summary = "支付宝服务器异步通知", description = "服务器异步通知接口")
    public String tradeNotify(@RequestParam Map<String, String> params) {
        log.info("支付宝服务器异步通知，参数：{}", params);

        String result = "failure";

        // 异步通知的处理逻辑
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(params,
                    environment.getProperty("alipay.alipay-public-key"),
                    AlipayConstants.CHARSET_UTF8,
                    AlipayConstants.SIGN_TYPE_RSA2);

            // 验签不通过：直接返回 failure，绝不继续处理后续业务（防止伪造通知）
            if (!signVerified) {
                log.error("支付宝异步通知签名验证失败");
                return result;
            }
            log.info("支付宝异步通知签名验证成功");

            // 按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验。
            // 1 商户需要验证该通知数据中的 out_trade_no 是否为商户系统中创建的订单号
            String outTradeNo = params.get("out_trade_no");
            OrderInfo order = orderInfoService.getOrderByOrderNo(outTradeNo);
            if (order == null) {
                log.error("订单不存在");
                return result;
            }

            // 2 判断total_amount 是否确实为该订单的实际金额（即商户订单创建时的金额）
            String totalAmount = params.get("total_amount");
            int totalFee = new BigDecimal(totalAmount).multiply(BigDecimal.valueOf(100)).intValue();
            if (!order.getTotalFee().equals(totalFee)) {
                log.error("订单金额不一致");
                return result;
            }

            // 3 校验通知中的 seller_id(或者 seller_email) 是否为 out_trade_no 这笔单据的对应的操作方
            String sellerId = params.get("seller_id");
            if (!sellerId.equals(environment.getProperty("alipay.seller-id"))) {
                log.error("商家ID不一致");
                return result;
            }

            // 4 验证 app_id 是否为该商户本身
            String appId = params.get("app_id");
            if (!appId.equals(environment.getProperty("alipay.app-id"))) {
                log.error("应用ID不一致");
                return result;
            }

            // 在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS 时，才表示该交易已成功
            // 非成功终态（如 WAIT_BUYER_PAY、TRADE_CLOSED）无需处理，直接 ack success 阻止支付宝对中间态反复重推
            if (!"TRADE_SUCCESS".equals(params.get("trade_status"))) {
                log.info("交易状态非 TRADE_SUCCESS，无需处理：trade_status={}", params.get("trade_status"));
                return "success";
            }

            // 校验成功后执行商户自身业务处理
            alipayService.processOrder(params);

            // 业务处理成功后，才把 result 置为 success，告知支付宝停止重推
            result = "success";
            // 返回给支付宝服务器的响应结果
            return result;

        } catch (AlipayApiException e) {
            // 通知接口不抛异常：返回 failure 让支付宝按策略重推，异常堆栈只记日志
            log.error("支付宝异步通知处理异常", e);
            return result;
        }
    }

    /**
     * 用户取消订单
     * @param orderNo
     * @return
     */
    @PostMapping("/trade/close/{orderNo}")
    @Operation(summary = "用户取消订单", description = "用户取消订单接口")
    public R cancel(@PathVariable("orderNo") String orderNo) {
        log.info("用户取消订单，orderNo：{}", orderNo);

        alipayService.cancelOrder(orderNo);

        return R.success("订单已取消", null);
    }

    /**
     * 查询订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    @GetMapping("/trade/query/{orderNo}")
    @Operation(summary = "查询订单", description = "根据商户订单号查询订单状态")
    public R queryOrder(@PathVariable("orderNo") String orderNo) {
        log.info("查询订单，orderNo：{}", orderNo);
        return R.success("查询订单成功", alipayService.queryOrder(orderNo));
    }

    @PostMapping("/trade/refund/{orderNo}/{reason}")
    @Operation(summary = "申请退款", description = "根据商户订单号申请退款")
    public R refunds(@PathVariable("orderNo") String orderNo,
                     @PathVariable("reason") String reason) {
        log.info("申请退款，orderNo：{}，reason：{}", orderNo, reason);
        alipayService.refund(orderNo, reason);
        return R.success("退款申请成功", null);
    }

    @GetMapping("/bill/downloadurl/query/{billDate}/{type}")
    @Operation(summary = "查询支付宝对账单", description = "返回账单文件的下载地址")
    public R queryBill(@PathVariable("billDate") String billDate,
                       @PathVariable("type") String type) {
        log.info("查询支付宝对账单，billDate：{}，type：{}", billDate, type);
        String downloadUrl = alipayService.queryBill(billDate, type);
        return R.success("查询账单成功", downloadUrl);
    }
}
