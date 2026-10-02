package com.ittxf.payment.common.config;

import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.AutoUpdateCertificatesVerifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;


/**
 * 微信支付参数与客户端配置。
 *
 * <p>职责分为两层：</p>
 * <ol>
 *     <li>通过 {@link PropertySource} 读取 {@code classpath:wxpay.properties}，
 *         再由 {@link ConfigurationProperties} 把 {@code wxpay} 前缀的配置项映射到本类字段；</li>
 *     <li>基于这些参数装配两个 Bean：自动更新平台证书的验签器，
 *         以及会自动处理签名/验签的 {@code wxPayClient}。</li>
 * </ol>
 *
 * <p>业务代码只需注入 {@link CloseableHttpClient} 直接发请求，
 * 无需关心签名细节，这正是 wechatpay-apache-httpclient 提供的能力。</p>
 *
 * <p>安全提醒：商户号、APIv3 密钥等属于敏感凭证，不应提交到公开仓库，
 * 生产环境建议改用环境变量或密钥管理服务注入。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@Configuration
@PropertySource("classpath:wxpay.properties") //读取配置文件
@ConfigurationProperties(prefix="wxpay") //读取wxpay前缀的配置项
@Data //使用set方法将wxpay节点中的值填充到当前类的属性中
@Slf4j
public class WxPayConfig {

    // 商户号，微信支付分配给商户的唯一身份标识，对应报文中的 mchid
    private String mchId;

    // 商户API证书序列号，用于告知微信本次签名出自哪张证书，与私钥必须成对
    private String mchSerialNo;

    // 商户私钥文件名，位于 classpath（src/main/resources）下，即申请证书时本地生成的 apiclient_key.pem
    private String privateKeyPath;

    // APIv3密钥，商户自行设置的 32 位对称密钥，用于解密回调报文与下载平台证书
    private String apiV3Key;

    // APPID，公众号/小程序/开放平台的主体 ID，需与商户号绑定后才能发起支付
    private String appid;

    // 微信服务器地址，即所有 API 请求的统一域名前缀，沙箱与生产环境不同
    private String domain;

    // 接收结果通知地址，与 WxNotifyType 中的回调路径拼接成完整 notify_url
    private String notifyDomain;

    // APIv2密钥，旧版 XML 接口的对称密钥，仅调用 v2 接口时需要
    private String partnerKey;

    /**
     * 获取商户的私钥文件，从 classpath（src/main/resources）下读取
     * @param filename 私钥文件名，如 apiclient_key.pem
     * @return 商户私钥
     */
    private PrivateKey getPrivateKey(String filename){

        // 使用 ClassPathResource 从类路径加载，打包成 jar 后同样可用，避免 FileInputStream 按工作目录找不到的问题
        try (InputStream in = new ClassPathResource(filename).getInputStream()) {
            return PemUtil.loadPrivateKey(in);
        } catch (IOException e) {
            throw new RuntimeException("私钥文件不存在：classpath(src/main/resources) 下未找到 " + filename, e);
        }
    }

    /**
     * 获取签名验证器
     *
     * <p>验证器会拿商户私钥去调用微信的下载平台证书接口，并每隔一段时间自动刷新，
     * 用于校验微信应答与回调报文的签名。无需手工传入证书文件。</p>
     *
     * <p>注意：构建该 Bean 时会发生一次联网请求，因此商户参数错误或网络不通时，
     * 应用会在启动阶段直接失败。</p>
     *
     * @return 支持定时更新平台证书的验签器
     */
    @Bean
    public AutoUpdateCertificatesVerifier getVerifier(){

        log.info("获取签名验证器");

        //获取商户私钥
        PrivateKey privateKey = getPrivateKey(privateKeyPath);

        //私钥签名对象
        PrivateKeySigner privateKeySigner = new PrivateKeySigner(mchSerialNo, privateKey);

        //身份认证对象
        WechatPay2Credentials wechatPay2Credentials = new WechatPay2Credentials(mchId, privateKeySigner);

        // 使用定时更新的签名验证器，不需要传入证书
        AutoUpdateCertificatesVerifier verifier = new AutoUpdateCertificatesVerifier(
                wechatPay2Credentials,
                apiV3Key.getBytes(StandardCharsets.UTF_8));

        return verifier;
    }


    /**
     * 获取http请求对象
     *
     * <p>该客户端会对每个出站请求自动签名，并对微信应答自动验签，
     * 同时处理证书自动更新，是调用下单/查单/关单/退款接口的主力客户端。</p>
     *
     * @param verifier 应答验签器，由 {@link #getVerifier()} 提供
     * @return 已具备签名与验签能力的 HTTP 客户端
     */
    @Bean(name = "wxPayClient")
    public CloseableHttpClient getWxPayClient(AutoUpdateCertificatesVerifier verifier){

        log.info("获取httpClient");

        //获取商户私钥
        PrivateKey privateKey = getPrivateKey(privateKeyPath);

        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(mchId, mchSerialNo, privateKey)
                .withValidator(new WechatPay2Validator(verifier));
        // ... 接下来，你仍然可以通过builder设置各种参数，来配置你的HttpClient

        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        CloseableHttpClient httpClient = builder.build();

        return httpClient;
    }

    /**
     * 获取HttpClient，无需进行应答签名验证，跳过验签的流程
     *
     * <p>适用于微信不会返回签名头的接口（如下载账单文件），
     * 通过 {@code withValidator(response -> true)} 放行所有应答。</p>
     *
     * <p>安全提醒：跳过验签意味着无法确认应答确实来自微信，
     * 不得用于处理支付结果等资金相关场景。</p>
     *
     * @return 仅签名、不验签的 HTTP 客户端
     */
    @Bean(name = "wxPayNoSignClient")
    public CloseableHttpClient getWxPayNoSignClient(){

        //获取商户私钥
        PrivateKey privateKey = getPrivateKey(privateKeyPath);

        //用于构造HttpClient
        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                //设置商户信息
                .withMerchant(mchId, mchSerialNo, privateKey)
                //无需进行签名验证、通过withValidator((response) -> true)实现
                .withValidator((response) -> true);

        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        CloseableHttpClient httpClient = builder.build();

        log.info("== getWxPayNoSignClient END ==");

        return httpClient;
    }

}
