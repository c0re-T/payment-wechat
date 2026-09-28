package com.ittxf.paymentwechat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication // 标记这是一个Spring Boot应用，包含@SpringBootConfiguration、@EnableAutoConfiguration和@ComponentScan
@MapperScan("com.ittxf.paymentwechat.mapper") // 扫描Mapper接口
// @EnableTransactionManagement // 启用事务管理，默认开启
public class PaymentWechatApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentWechatApplication.class, args);
    }

}
