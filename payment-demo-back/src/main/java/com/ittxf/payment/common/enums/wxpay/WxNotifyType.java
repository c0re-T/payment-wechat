package com.ittxf.payment.common.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 微信支付回调通知地址枚举。
 *
 * <p>下单时通过 {@code notify_url} 告知微信“支付结果发到哪”，本枚举维护的就是
 * 本系统自己提供的回调 Controller 路径，需与 {@code wxpay.notify-domain}
 * （如 ngrok 内网穿透域名）拼接后构成公网可访问的完整地址。</p>
 *
 * <p>注意：此处路径必须与 Controller 的 {@code @RequestMapping} 包前缀保持一致，
 * 否则微信通知会打到不存在的地址上。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@AllArgsConstructor
@Getter
public enum WxNotifyType {

	/**
	 * Native 下单支付结果通知（APIv3）
	 */
	NATIVE_NOTIFY("/api/wx-pay/native/notify"),

	/**
	 * Native 下单支付结果通知（APIv2）
	 */
	NATIVE_NOTIFY_V2("/api/wx-pay-v2/native/notify"),


	/**
	 * 退款结果通知
	 */
	REFUND_NOTIFY("/api/wx-pay/refunds/notify");

	/**
	 * 回调接口相对路径，需与通知域名拼接使用
	 */
	private final String type;
}
