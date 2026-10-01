package com.ittxf.paymentwechat.controller;

import com.ittxf.paymentwechat.common.enums.OrderStatus;
import com.ittxf.paymentwechat.common.result.R;
import com.ittxf.paymentwechat.entity.OrderInfo;
import com.ittxf.paymentwechat.service.OrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-info")
@Tag(name = "订单信息管理")
public class OrderInfoController {

    private final OrderInfoService orderInfoService;

    @GetMapping("/list")
    @Operation(summary = "获取订单信息列表")
    public R<List<OrderInfo>> list() {
        return R.success(orderInfoService.listOrderByCreateTimeDesc());
    }

    @GetMapping("/query-order-status/{orderNo}")
    public R queryOrderStatus(@PathVariable("orderNo") String orderNo) {
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        boolean paid = OrderStatus.SUCCESS.getType().equals(orderStatus);
        return R.builder()
                .code(200)
                .message(paid ? "订单支付成功" : "订单支付中")
                .data(paid) // ← 关键：用 data 表达状态，code 恒 200 不再触发错误弹窗
                .build();
    }
}
