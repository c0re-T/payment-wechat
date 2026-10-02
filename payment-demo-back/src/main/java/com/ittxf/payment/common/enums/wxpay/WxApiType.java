package com.ittxf.payment.common.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 微信支付 API 接口路径枚举。
 *
 * <p>集中维护商户后台调用微信支付时拼接的 URI，与 {@code wxpay.domain}
 * （默认 https://api.mch.weixin.qq.com）拼接后即为完整请求地址。
 * 路径中含 {@code %s} 的为路径参数，使用时需用订单号等真实值填充。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@AllArgsConstructor
@Getter
public enum WxApiType {

	/**
	 * Native 下单（APIv3），PC 网站扫码支付，返回 code_url
	 */
	NATIVE_PAY("/v3/pay/transactions/native"),

	/**
	 * 统一下单（APIv2），XML 报文，返回 prepay_id
	 */
	NATIVE_PAY_V2("/pay/unifiedorder"),

	/**
	 * 根据商户订单号查询订单，%s 为 out_trade_no
	 */
	ORDER_QUERY_BY_NO("/v3/pay/transactions/out-trade-no/%s"),

	/**
	 * 关闭订单，%s 为 out_trade_no
	 */
	CLOSE_ORDER_BY_NO("/v3/pay/transactions/out-trade-no/%s/close"),

	/**
	 * 申请退款（APIv3）
	 */
	DOMESTIC_REFUNDS("/v3/refund/domestic/refunds"),

	/**
	 * 查询单笔退款，%s 为 out_refund_no
	 */
	DOMESTIC_REFUNDS_QUERY("/v3/refund/domestic/refunds/%s"),

	/**
	 * 申请交易账单，%s 为账单日期
	 */
	TRADE_BILLS("/v3/bill/tradebill"),

	/**
	 * 申请资金账单
	 */
	FUND_FLOW_BILLS("/v3/bill/fundflowbill");


	/**
	 * 接口相对路径，需与域名拼接使用
	 */
	private final String type;
}
