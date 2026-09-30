package com.ittxf.paymentwechat.controller;

import com.ittxf.paymentwechat.common.result.R;
import com.ittxf.paymentwechat.entity.OrderInfo;
import com.ittxf.paymentwechat.service.OrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
