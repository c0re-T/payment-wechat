package com.ittxf.paymentwechat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    @GetMapping("/test")
    public String test() {
        return "This is a product controller";
    }
}
