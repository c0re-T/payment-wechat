import request from '@/utils/request'

/**
 * 微信支付相关接口
 *
 * 统一以 /api/wx-pay 为前缀，需与后端 WxPayController 的 @RequestMapping 保持一致。
 */
export default {
  /**
   * Native 下单，获取二维码链接
   *
   * @param {number|string} productId 商品 ID，作为路径参数传递
   * @returns {Promise} data 中含 codeUrl（二维码内容）与 orderNo（商户订单号）
   */
  nativePay(productId) {
    return request.post(`/api/wx-pay/native/${productId}`)
  },

  /**
   * 取消（关闭）未支付的订单
   *
   * @param {string} orderNo 商户订单号
   * @returns {Promise} 关单结果
   */
  cancel(orderNo) {
    return request.post(`/api/wx-pay/cancel/${orderNo}`)
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
    return request.post(`/api/wx-pay/refunds/${orderNo}/${encodeURIComponent(reason)}`)
  }
}
