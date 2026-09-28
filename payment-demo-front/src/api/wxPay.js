import request from '@/utils/request'

export default {
  nativePay(productId) {
    return request.post(`/api/wx-pay/native/${productId}`)
  },

  cancel(orderNo) {
    return request.post(`/api/wx-pay/cancel/${orderNo}`)
  },

  refunds(orderNo, reason) {
    // reason 是中文，拼进路径前必须编码
    return request.post(`/api/wx-pay/refunds/${orderNo}/${encodeURIComponent(reason)}`)
  }
}
