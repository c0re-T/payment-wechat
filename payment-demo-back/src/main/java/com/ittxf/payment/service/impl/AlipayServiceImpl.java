package com.ittxf.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayDataDataserviceBillDownloadurlQueryRequest;
import com.alipay.api.response.AlipayDataDataserviceBillDownloadurlQueryResponse;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.Status;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.ittxf.payment.common.enums.OrderStatus;
import com.ittxf.payment.common.enums.PayType;
import com.ittxf.payment.common.exception.BusinessException;
import com.ittxf.payment.entity.OrderInfo;
import com.ittxf.payment.entity.RefundInfo;
import com.ittxf.payment.service.AlipayService;
import com.ittxf.payment.service.OrderInfoService;
import com.ittxf.payment.service.PaymentInfoService;
import com.ittxf.payment.service.RefundInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlipayServiceImpl implements AlipayService {

    private final OrderInfoService orderInfoService;
    private final AlipayClient alipayClient;
    private final Environment environment;
    private final PaymentInfoService paymentInfoService;
    private final ReentrantLock lock = new ReentrantLock();
    private final RefundInfoService refundInfoService;

    @Override
    public String tradeCreate(Long productId) {

        // 生成订单
        log.info("生成订单");
        OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId, PayType.ALIPAY.getType());

        // 调用支付宝的接口
        log.info("调用支付宝接口");
        // 构造请求参数以调用接口
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 构造业务请求参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(orderInfo.getOrderNo());
        BigDecimal totalFee = new BigDecimal(orderInfo.getTotalFee()).divide(new BigDecimal(100));
        model.setTotalAmount(totalFee.toString());
        model.setSubject(orderInfo.getTitle());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        request.setBizModel(model);

        // 配置需要的公共请求参数
        request.setNotifyUrl(environment.getProperty("alipay.notify-url"));
        request.setReturnUrl(environment.getProperty("alipay.return-url"));

        // 构造业务请求参数
        /*JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOrderNo());
        bizContent.put("total_amount", totalFee);
        bizContent.put("subject", orderInfo.getTitle());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toJSONString());*/

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果: {}", response.getBody());
                return response.getBody();
            } else {
                log.info("调用失败,返回码: {}", response.getCode());
                throw new BusinessException("创建订单失败，请稍后再试");
            }
        } catch (AlipayApiException e) {
            throw new BusinessException("调用支付宝接口失败", e);
        }
    }

    /**
     * 处理订单
     * @param params
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void processOrder(Map<String, String> params) {
        log.info("处理订单");

        if (lock.tryLock()) {
            try {
                // 处理订单逻辑
                String outTradeNo = params.get("out_trade_no");

                // 处理重复通知
                // 接口调用的幂等性：只有“未支付”的订单才需要处理；已是其他状态（如 SUCCESS）说明是重复通知，直接跳过
                OrderInfo order = orderInfoService.getOrderByOrderNo(outTradeNo);
                if (!OrderStatus.NOTPAY.getType().equals(order.getOrderStatus())) {
                    log.info("订单已处理，跳过重复通知：outTradeNo={}, orderStatus={}", outTradeNo, order.getOrderStatus());
                    return;
                }

                // 更新订单状态
                orderInfoService.updateStatusByOrderNo(outTradeNo, OrderStatus.SUCCESS);

                // 记录支付日志
                paymentInfoService.createPaymentInfoAliPay(params);

            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 取消订单
     * @param orderNo
     */
    @Override
    public void cancelOrder(String orderNo) {

        // 调用支付宝提供的统一关闭订单接口
        this.closeOrder(orderNo);
        
        // 更新订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

    /**
     * 查询订单
     *
     * @param orderNo
     * @return
     */
    @Override
    public String queryOrder(String orderNo) {
        log.info("查询订单：orderNo={}", orderNo);

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        request.setBizContent(bizContent.toString());
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果: {}", response.getBody());
                return response.getBody();
            } else {
                log.info("调用失败,返回码: {}", response.getCode());
                throw new BusinessException("调用支付宝查询订单接口失败");
            }
        } catch (AlipayApiException e) {
            throw new BusinessException("调用支付宝查询订单接口失败");
        }
    }

    /**
     * 核实订单状态（支付宝对账兜底）
     *
     * <p>主动调用支付宝查单接口，以支付宝侧结果为准纠正本地订单状态，
     * 用于兜底“异步通知丢失导致本地一直显示未支付”的场景：</p>
     * <ul>
     *     <li>支付宝已支付（TRADE_SUCCESS / TRADE_FINISHED）→ 本地更新为 SUCCESS</li>
     *     <li>支付宝仍未支付（WAIT_BUYER_PAY）→ 关单并本地更新为 CLOSED</li>
     *     <li>支付宝交易已关闭（TRADE_CLOSED）或查无此单 → 本地更新为 CLOSED</li>
     * </ul>
     *
     * @param orderNo 商户订单号
     */
    @Override
    public void checkOrderStatus(String orderNo) {
        log.info("核实订单状态：orderNo={}", orderNo);

        // 幂等：本地已不是“未支付”，说明已处理过（通知已到），无需再对账
        String localStatus = orderInfoService.getOrderStatus(orderNo);
        if (!OrderStatus.NOTPAY.getType().equals(localStatus)) {
            log.info("本地订单非未支付状态，跳过对账：orderNo={}, status={}", orderNo, localStatus);
            return;
        }

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        request.setBizContent(bizContent.toString());

        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    // 支付宝侧已支付：纠正本地状态（异步通知丢失的兜底）
                    log.info("支付宝侧已支付，纠正本地状态为 SUCCESS：orderNo={}, tradeStatus={}", orderNo, tradeStatus);
                    orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
                } else if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
                    // 超时仍未支付：关单并置为 CLOSED
                    log.info("支付宝侧仍未支付，执行关单：orderNo={}", orderNo);
                    this.closeOrder(orderNo);
                    orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
                } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                    orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
                }
            } else if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                // 支付宝侧无此交易：本地关单，避免僵尸单反复被扫
                log.info("支付宝侧无此交易，本地置为 CLOSED：orderNo={}", orderNo);
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
            } else {
                log.warn("支付宝查单返回非成功：orderNo={}, code={}, subCode={}, subMsg={}",
                        orderNo, response.getCode(), response.getSubCode(), response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            // 查单本身异常仅记录，不中断整批；关单等后续异常由定时任务循环的 try/catch 兜底
            log.error("支付宝查单异常：orderNo={}", orderNo, e);
        }
    }

    /**
     * 退款
     * @param orderNo
     * @param reason
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void refund(String orderNo, String reason) {
        log.info("调用退款API");

        // 创建退款单
        RefundInfo refundInfo = refundInfoService.createRefundByOrderNo(orderNo, reason);

        // 调用统一收单交易退款接口 alipay.trade.refund
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        // 组装请求参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo); // 商户订单号
        // 退款金额：库中存的是分，转成元（用 divide 避免 double 精度误差）
        BigDecimal refundAmount = new BigDecimal(refundInfo.getRefund()).divide(new BigDecimal(100));
        bizContent.put("refund_amount", refundAmount.toString()); // 退款金额，单位：元
        bizContent.put("refund_reason", reason); // 退款原因
        request.setBizContent(bizContent.toString());

        String status = "已退款";

        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果: {}", response.getBody());
                // 更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
                // 更新退款单
                refundInfoService.updateRefundForAlipay(
                        refundInfo.getRefundNo(),
                        response.getBody(),
                        status
                ); // 退款成功
            } else {
                log.info("调用失败,返回码: {}, subCode: {}, subMsg: {}",
                        response.getCode(), response.getSubCode(), response.getSubMsg());
                status = "退款异常";
                // 更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);
                // 更新退款单
                refundInfoService.updateRefundForAlipay(
                        refundInfo.getRefundNo(),
                        response.getBody(),
                        status
                ); // 退款失败
            }
        } catch (AlipayApiException e) {
            // 保留原始堆栈，便于定位是签名、参数还是网络问题
            throw new BusinessException("调用支付宝退款接口失败", e);
        }


    }

    /**
     * 查询支付宝对账单的下载地址
     *
     * <p>调用 alipay.data.dataservice.bill.downloadurl.query，拿到账单文件（ZIP）的
     * 临时下载地址，交给前端/浏览器直接下载；与微信不同，支付宝账单文件不在本地，
     * 后端只负责换取 URL。</p>
     *
     * @param billDate 账单日期，yyyy-MM-dd（日账单）或 yyyy-MM（月账单）
     * @param type 账单类型：trade / signcustomer（前端传值，映射为支付宝的 trade / signCustomer）
     * @return 账单下载地址
     */
    @Override
    public String queryBill(String billDate, String type) {
        log.info("查询支付宝对账单下载地址：billDate={}, type={}", billDate, type);

        // 前端传 trade / signcustomer，映射为支付宝要求的 bill_type 取值
        String billType;
        if ("trade".equalsIgnoreCase(type)) {
            billType = "trade";
        } else if ("signcustomer".equalsIgnoreCase(type)) {
            billType = "signcustomer";
        } else {
            throw new BusinessException("   不支持的账单类型：" + type);
        }

        AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("bill_type", billType);
        bizContent.put("bill_date", billDate);
        request.setBizContent(bizContent.toString());

        try {
            AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String downloadUrl = response.getBillDownloadUrl();
                log.info("支付宝账单下载地址：{}", downloadUrl);
                return downloadUrl;
            } else {
                log.warn("查询支付宝账单失败：code={}, subCode={}, subMsg={}",
                        response.getCode(), response.getSubCode(), response.getSubMsg());
                throw new BusinessException("查询支付宝账单失败：" + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            throw new BusinessException("查询支付宝账单异常", e);
        }
    }

    /**
     * 调用支付宝的统一关闭订单接口
     * @param orderNo
     */
    private void closeOrder(String orderNo) {

        log.info("调用支付宝的统一关闭订单接口");
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        request.setBizContent(bizContent.toString());

        try {
            AlipayTradeCloseResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果: {}", response.getBody());
            }
            else {
                log.info("调用失败,返回码: {}", response.getCode());
            }

        } catch (AlipayApiException e) {
            throw new BusinessException("调用支付宝关闭订单接口失败");
        }
    }
}
