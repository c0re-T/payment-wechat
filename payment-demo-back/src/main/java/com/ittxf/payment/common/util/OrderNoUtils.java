package com.ittxf.payment.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 订单号工具类
 *
 * <p>生成商户侧唯一的 {@code out_trade_no}（订单号）与 {@code out_refund_no}（退款单号）。
 * 微信支付并不要求订单号格式，但要求同一商户号下全局唯一，
 * 因此采用“14 位时间戳 + 3 位随机数”拼接，兼顾可读性与唯一性。</p>
 *
 * @author qy
 * @since 1.0
 */
public class OrderNoUtils {

    /**
     * 获取订单编号，用作微信下单的 out_trade_no
     *
     * @return 形如 ORDER_20260929153045123 的订单号（前缀 + 14 位时间 + 3 位随机）
     */
    public static String getOrderNo() {
        return "ORDER_" + getNo();
    }

    /**
     * 获取退款单编号，用作微信退款的 out_refund_no
     *
     * @return 形如 REFUND_20260929153045123 的退款单号
     */
    public static String getRefundNo() {
        return "REFUND_" + getNo();
    }

    /**
     * 获取编号主体（不含业务前缀）
     *
     * <p>格式为 yyyyMMddHHmmss + 3 位随机数字，精确到秒，
     * 意味着同一秒内最多支持 1000 个不重复编号。</p>
     *
     * <p>注意：SimpleDateFormat 非线程安全，所以每次调用都在方法内部新建实例，
     * 不能改成静态字段复用；同时本实现并非严格唯一，
     * 高并发场景应改用数据库唯一索引或雪花算法兼底。</p>
     *
     * @return 17 位纯数字编号
     */
    public static String getNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String newDate = sdf.format(new Date());
        String result = "";
        Random random = new Random();
        // 拼接 3 位随机数，降低同一秒内生单冲突概率
        for (int i = 0; i < 3; i++) {
            result += random.nextInt(10);
        }
        return newDate + result;
    }

}
