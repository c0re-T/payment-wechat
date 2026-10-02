package com.ittxf.payment.controller;

import com.ittxf.payment.entity.Product;
import com.ittxf.payment.service.ProductService;
import com.ittxf.payment.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// @CrossOrigin // 允许跨域，只能局部生效，生产环境不建议使用
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品管理相关接口")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/test")
    @Operation(summary = "测试接口", description = "用于测试商品管理相关接口")
    public R test() {
        Map<String, Object> map = new HashMap<>();
        map.put("msg", "This is a product controller");
        map.put("timestamp", new Date());
        return R.builder()
                .code(200)
                .message("操作成功")
                .data(map)
                .build();
    }

    @GetMapping("/list")
    @Operation(summary = "获取商品列表", description = "用于获取商品列表")
    public R<List<Product>> list() {
        if (productService.list() != null && productService.list().size() > 0) {
            return R.success(productService.list());
        }
        return R.fail("没有数据");
    }
}
