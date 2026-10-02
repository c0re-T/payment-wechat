package com.ittxf.payment.common.util;


import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;

import static com.wechat.pay.contrib.apache.httpclient.constant.WechatPayHttpHeaders.*;

/**
 * 微信支付回调请求的验签器。
 *
 * <p>微信发送支付/退款结果通知时，会在请求头携带四个字段：
 * {@code Wechatpay-Serial}（平台证书序列号）、{@code Wechatpay-Signature}（签名）、
 * {@code Wechatpay-Nonce}（随机串）、{@code Wechatpay-Timestamp}（时间戳）。</p>
 *
 * <p>本类负责两件事：</p>
 * <ol>
 *     <li><b>防伪造</b>：用平台证书公钥校验签名，确认请求确实来自微信支付而非第三方伪装；</li>
 *     <li><b>防重放</b>：校验时间戳是否在 {@link #RESPONSE_EXPIRED_MINUTES} 分钟内，
 *         过期的旧报文直接拒接。</li>
 * </ol>
 *
 * <p>验签串由“时间戳\n随机串\n报文体\n”拼成，顺序与换行不可改动。</p>
 *
 * @author xy-peng
 */
public class WechatPay2ValidatorForRequest {

    protected static final Logger log = LoggerFactory.getLogger(WechatPay2ValidatorForRequest.class);
    /**
     * 应答超时时间，单位为分钟
     *
     * <p>超出该窗口的通知被视为过期报文，用于抵御重放攻击。</p>
     */
    protected static final long RESPONSE_EXPIRED_MINUTES = 5;
    /**
     * 验签器，封装了平台证书，提供 verify 与证书查询能力
     */
    protected final Verifier verifier;
    /**
     * 微信请求唯一标识，仅用于拼接到异常信息中方便排查日志
     */
    protected final String requestId;
    /**
     * 回调的原始报文体，验签必须用原文，不能用反序列化后重新生成的 JSON
     */
    protected final String body;


    /**
     * 构造验签器
     *
     * @param verifier  平台证书验签器，通常由 WxPayConfig 注入
     * @param requestId 微信请求 ID，取自请求头，仅用于日志
     * @param body      待验签的原始报文体
     */
    public WechatPay2ValidatorForRequest(Verifier verifier, String requestId, String body) {
        this.verifier = verifier;
        this.requestId = requestId;
        this.body = body;
    }

    /**
     * 生成参数缺失或非法的异常
     *
     * @param message 异常描述模板，支持占位符
     * @param args    模板参数
     * @return 包装后的 IllegalArgumentException
     */
    protected static IllegalArgumentException parameterError(String message, Object... args) {
        message = String.format(message, args);
        return new IllegalArgumentException("parameter error: " + message);
    }

    /**
     * 生成验签失败的异常
     *
     * @param message 异常描述模板，支持占位符
     * @param args    模板参数
     * @return 包装后的 IllegalArgumentException
     */
    protected static IllegalArgumentException verifyFail(String message, Object... args) {
        message = String.format(message, args);
        return new IllegalArgumentException("signature verify fail: " + message);
    }

    /**
     * 校验回调请求的签名是否合法
     *
     * <p>执行顺序：先校验请求头参数与时间戳，再拼验签串，最后用平台证书验签。
     * 参数错误或验签失败均仅记录警告并返回 false，不向上抛异常，
     * 便于 Controller 据此返回失败应答让微信重试。</p>
     *
     * @param request 微信回调的原始请求
     * @return true 表示验签通过，可信任该通知；false 表示应拒接
     * @throws IOException 读取请求异常时抛出
     */
    public final boolean validate(HttpServletRequest request) throws IOException {
        try {
            //处理请求参数
            validateParameters(request);

            //构造验签名串
            String message = buildMessage(request);

            String serial = request.getHeader(WECHAT_PAY_SERIAL);
            String signature = request.getHeader(WECHAT_PAY_SIGNATURE);

            //验签
            if (!verifier.verify(serial, message.getBytes(StandardCharsets.UTF_8), signature)) {
                throw verifyFail("serial=[%s] message=[%s] sign=[%s], request-id=[%s]",
                        serial, message, signature, requestId);
            }
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * 校验四个签名请求头是否齐全，并判断报文是否过期
     *
     * @param request 微信回调请求
     * @throws IllegalArgumentException 请求头缺失、时间戳非法或已过期时抛出
     */
    protected final void validateParameters(HttpServletRequest request) {

        // NOTE: ensure HEADER_WECHAT_PAY_TIMESTAMP at last
        // 数组末尾必须是时间戳，下方靠最后一次赋值拿到它做超时判断
        String[] headers = {WECHAT_PAY_SERIAL, WECHAT_PAY_SIGNATURE, WECHAT_PAY_NONCE, WECHAT_PAY_TIMESTAMP};

        String header = null;
        for (String headerName : headers) {
            header = request.getHeader(headerName);
            if (header == null) {
                throw parameterError("empty [%s], request-id=[%s]", headerName, requestId);
            }
        }

        //判断请求是否过期
        String timestampStr = header;
        try {
            Instant responseTime = Instant.ofEpochSecond(Long.parseLong(timestampStr));
            // 拒绝过期请求
            if (Duration.between(responseTime, Instant.now()).abs().toMinutes() >= RESPONSE_EXPIRED_MINUTES) {
                throw parameterError("timestamp=[%s] expires, request-id=[%s]", timestampStr, requestId);
            }
        } catch (DateTimeException | NumberFormatException e) {
            throw parameterError("invalid timestamp=[%s], request-id=[%s]", timestampStr, requestId);
        }
    }

    /**
     * 拼接待验签的原始字符串
     *
     * <p>格式固定为 {@code 时间戳\n随机串\n报文体\n}，末尾换行符不可省略，
     * 与微信服务端的签名生成规则必须严格一致。</p>
     *
     * @param request 微信回调请求
     * @return 用于验签的拼接字符串
     * @throws IOException 读取请求异常时抛出
     */
    protected final String buildMessage(HttpServletRequest request) throws IOException {
        String timestamp = request.getHeader(WECHAT_PAY_TIMESTAMP);
        String nonce = request.getHeader(WECHAT_PAY_NONCE);
        return timestamp + "\n"
                + nonce + "\n"
                + body + "\n";
    }

    /**
     * 读取 HTTP 应答体文本
     *
     * @param response 待读取的应答
     * @return 应答体字符串；实体不存在或不可重复读取时返回空串
     * @throws IOException 读取失败时抛出
     */
    protected final String getResponseBody(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        return (entity != null && entity.isRepeatable()) ? EntityUtils.toString(entity) : "";
    }

}
