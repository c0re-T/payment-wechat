import request from '@/utils/request'

/**
 * 支付宝支付相关接口
 *
 * 统一以 /api/ali-pay 为前缀，需与后端 AliPayController 的 @RequestMapping 保持一致。
 */
export default {
  /**
   * 统一收单交易页面支付（alipay.trade.page.pay）
   *
   * @param {number|string} productId 商品 ID，作为路径参数传递
   * @returns {Promise} data 中含 formStr（支付宝收银台表单字符串）
   */
  tradePagePay(productId) {
    return request.post(`/api/ali-pay/trade/page/pay/${productId}`)
  },

  /**
   * 撤销（关闭）未支付的支付宝交易
   *
   * @param {string} orderNo 商户订单号
   * @returns {Promise} 撤单结果
   */
  cancel(orderNo) {
    return request.post(`/api/ali-pay/trade/close/${orderNo}`)
  },

  /**
   * 发起退款
   *
   * @param {string} orderNo 待退款的商户订单号
   * @param {string} reason  退款原因
   * @returns {Promise} 退款受理结果
   */
  refunds(orderNo, reason) {
    // reason 是中文，拼进路径前必须编码
    return request.post(`/api/ali-pay/trade/refund/${orderNo}/${encodeURIComponent(reason)}`)
  }
}
