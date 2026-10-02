package com.ittxf.payment.common.util;


import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;


/**
 * HTTP 请求处理工具类。
 *
 * <p>主要服务于微信支付回调场景：验签必须使用微信发过来的
 * <b>原始报文体</b>，哪怕多一个空格、字段顺序变了都会导致验签失败，
 * 因此不能直接用 {@code @RequestBody} 接成实体类再序列化回去，
 * 必须从输入流里把原文逐字读出来。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
public class HttpUtils {

    /**
     * 将通知参数转化为字符串
     *
     * <p>逐行读取请求体并用换行符拼回。注意：输入流只能读一次，
     * 调用本方法后无法再通过 {@code @RequestBody} 获取同一份参数。</p>
     *
     * @param request 微信回调的原始请求对象
     * @return 完整的请求体字符串（读取失败时抛 RuntimeException）
     */
    public static String readData(HttpServletRequest request) {
        BufferedReader br = null;
        try {
            StringBuilder result = new StringBuilder();
            br = request.getReader();
            // 逐行读取，除首行外每行前补一个换行，保持与原文一致
            for (String line; (line = br.readLine()) != null; ) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                result.append(line);
            }
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // 手动关闭流，不用 try-with-resources 是为了兼容 br 可能为 null 的早期分支
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}