import request from '@/utils/request'

export default {
  queryOrderStatus(orderNo) {
    return request.get(`/api/order-info/query-order-status/${orderNo}`)
  },

  list() {
    return request.get('/api/order-info/list')
  }
}
