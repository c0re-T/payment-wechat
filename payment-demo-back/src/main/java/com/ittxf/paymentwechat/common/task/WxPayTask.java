package com.ittxf.paymentwechat.common.task;

import com.ittxf.paymentwechat.entity.OrderInfo;
import com.ittxf.paymentwechat.service.OrderInfoService;
import com.ittxf.paymentwechat.service.WxPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WxPayTask {

    private final OrderInfoService orderInfoService;
    private final WxPayService wxPayService;

    /**
     * 定时任务示例：按 cron 表达式周期性触发。
     * Spring 的 cron 为 6 位（比 Linux 多了开头的“秒”），从左到右依次是：
     *   秒        0-59
     *   分        0-59
     *   时        0-23
     *   日(月中的天) 1-31
     *   月        1-12 或 JAN-DEC
     *   星期(周中的天) 0-7 或 SUN-SAT（0 和 7 都表示周日）
     * 常用占位符：
     *  * 每一格都匹配；
     *  ? 不指定（只能用于“日/星期”，二者一般一个给值、另一个给 ?，避免冲突）；- 区间；, 枚举；
     *  / 从起始值按步长递增，如 0/5 表示 0,5,10...。
     * 常见示例（秒 分 时 日 月 星期）：
     *   0 0/1 * * * ?    每分钟
     *   0 0/5 * * * ?    每 5 分钟
     *   0 0 * * * ?      每小时整点
     *   0 30 3 * * ?     每天 3:30
     *   0 0 9 ? * MON    每周一 9:00
     *   0 0 0 1 * ?      每月 1 号 0:00
     * 支付场景下这类任务常用于：定时关闭超时未支付订单、主动查单对账（兜底回调丢失）。
     */
    /*@Scheduled(cron = "0/1 0/1 * ? * ?")
    public void task1() {
        log.info("task1执行......");
    }*/

    /**
     * 从第0秒开始每隔30秒执行1次，查询创建超过5分钟，并且未支付的订单
     */
    @Scheduled(cron = "0/30 * * ? * ?")
    public void orderConfirm() {
        log.info("orderConfirm执行......");

        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(1);

        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();
            log.info("超时订单：{}", orderNo);

            // 核实订单状态：调用微信支付查单接口
            String orderStatus = wxPayService.checkOrderStatus(orderNo);
        }
    }

}
