package com.ittxf.paymentwechat.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域（CORS）配置。
 *
 * <p>前端（Vite 开发服务器）与后端（8090）不同源，浏览器会拦截跨域请求。
 * 这里通过 {@link WebMvcConfigurer} 集成的方式集全局生效，
 * 相比在单个 Controller 上标 {@code @CrossOrigin}，能避免逐接口重复配置。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 注册跨域规则，对全部接口生效
     *
     * @param registry 跨域配置登记器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 拦截所有的请求，允许跨域
                .allowedOriginPatterns("*") // 允许所有的来源（用 Patterns 是因为 allowCredentials=true 时不允许用 "*" 写法的 allowedOrigins）
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS") // 允许所有的方法
                .allowedHeaders("*") // 允许所有的请求头
                .allowCredentials(true) // 允许发送Cookie
                .maxAge(3600); // 预检请求缓存时间，单位为秒，避免每个请求都先发一次 OPTIONS
    }
}
