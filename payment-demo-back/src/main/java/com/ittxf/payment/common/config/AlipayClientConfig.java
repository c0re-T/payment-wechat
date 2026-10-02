package com.ittxf.payment.common.config;

import com.alipay.api.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
// 加载支付宝沙箱配置文件
@PropertySource("classpath:alipay-sandbox.properties")
@RequiredArgsConstructor
public class AlipayClientConfig {

    // 自动获取配置文件中的属性
    private final Environment environment;

    // 配置支付宝客户端
    @Bean
    public AlipayClient alipayClient() throws AlipayApiException {
        // 创建配置对象
        AlipayConfig alipayConfig = new AlipayConfig();
        //设置网关地址
        alipayConfig.setServerUrl(environment.getProperty("alipay.gateway-url"));
        //设置应用ID
        alipayConfig.setAppId(environment.getProperty("alipay.app-id"));
        //设置应用私钥
        alipayConfig.setPrivateKey(environment.getProperty("alipay.merchant-private-key"));
        //设置请求格式，固定值json
        alipayConfig.setFormat(AlipayConstants.FORMAT_JSON);
        //设置字符集
        alipayConfig.setCharset(AlipayConstants.CHARSET_UTF8);
        //设置签名类型
        alipayConfig.setSignType(AlipayConstants.SIGN_TYPE_RSA2);
        //设置支付宝公钥
        alipayConfig.setAlipayPublicKey(environment.getProperty("alipay.alipay-public-key"));
        //实例化客户端
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);

        return alipayClient;
    }

}
