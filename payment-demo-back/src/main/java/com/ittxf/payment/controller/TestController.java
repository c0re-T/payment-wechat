package com.ittxf.payment.controller;

import com.ittxf.payment.common.config.WxPayConfig;
import com.ittxf.payment.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
@Tag(name = "测试接口", description = "测试接口")
public class TestController {

    private final WxPayConfig wxPayConfig;

    @GetMapping("/getWxPayConfig")
    @Operation(summary = "获取微信支付配置", description = "用于获取微信支付配置")
    public R getWxPayConfig() {
        String mchId = wxPayConfig.getMchId();
        return R.success(mchId);
    }
}
