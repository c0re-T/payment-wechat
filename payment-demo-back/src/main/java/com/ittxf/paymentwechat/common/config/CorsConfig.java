package com.ittxf.paymentwechat.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 拦截所有的请求，允许跨域
                .allowedOriginPatterns("*") // 允许所有的来源
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS") // 允许所有的方法
                .allowedHeaders("*") // 允许所有的请求头
                .allowCredentials(true) // 允许发送Cookie
                .maxAge(3600); // 预检请求缓存时间，单位为秒
    }
}
