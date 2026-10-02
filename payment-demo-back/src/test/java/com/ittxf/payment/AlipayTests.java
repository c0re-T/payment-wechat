package com.ittxf.payment;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
@Slf4j
public class AlipayTests {

    @Autowired
    private Environment env;

    @Test
    void testAlipayConfig() {
        log.info("Alipay config: {}", env.getProperty("alipay.app-id"));
    }
}
