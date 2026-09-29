package com.ittxf.paymentwechat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 支付实验室后端服务启动类。
 *
 * <p>整个 Spring Boot 应用的入口，负责装配自动配置、扫描组件与 Mapper 接口，
 * 并启动内嵌的 Tomcat 容器对外提供 REST 接口。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@SpringBootApplication // 标记这是一个Spring Boot应用，包含@SpringBootConfiguration、@EnableAutoConfiguration和@ComponentScan
@MapperScan("com.ittxf.paymentwechat.mapper") // 扫描Mapper接口
// @EnableTransactionManagement // 启用事务管理，默认开启
public class PaymentWechatApplication {

    /**
     * 应用主入口，启动 Spring Boot 容器
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PaymentWechatApplication.class, args);
    }

}
