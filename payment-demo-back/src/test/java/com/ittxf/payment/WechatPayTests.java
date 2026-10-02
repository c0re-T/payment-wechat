package com.ittxf.payment;

import com.ittxf.payment.common.config.WxPayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WechatPayTests {

    @Autowired
    private WxPayConfig wxPayConfig;

    @Test
    void contextLoads() {
        /*// 获取私钥路径
        String privateKeyPath = wxPayConfig.getPrivateKeyPath();

        // 获取私钥（方法改成public）
        PrivateKey privateKey =
                wxPayConfig.getPrivateKey(privateKeyPath);
        System.out.println(privateKey);*/

    }

}
