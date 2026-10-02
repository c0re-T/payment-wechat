package com.ittxf.payment.common.task;

import com.ittxf.payment.common.enums.PayType;
import com.ittxf.payment.entity.OrderInfo;
import com.ittxf.payment.service.AlipayService;
import com.ittxf.payment.service.OrderInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliPayTask {

    private final OrderInfoService orderInfoService;
    private final AlipayService alipayService;

    /**
     * 从第0秒开始每隔30秒执行1次，查询创建超过5分钟，并且未支付的订单
     */
    @Scheduled(cron = "0/30 * * ? * ?")
    public void orderConfirm() {
        log.info("orderConfirm执行......");

        // 只捞支付宝渠道的超时未支付单，避免误查微信订单
        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(1, PayType.ALIPAY.getType());

        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();
            log.info("超时订单：{}", orderNo);

            // 核实订单状态：调用支付宝查单接口；单笔失败不影响整批
            try {
                alipayService.checkOrderStatus(orderNo);
            } catch (Exception e) {
                log.error("支付宝对账查单失败，orderNo={}", orderNo, e);
            }
        }
    }

}
