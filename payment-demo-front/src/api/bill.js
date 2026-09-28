import request from '@/utils/request'

export default {
  downloadBill(billDate, type) {
    return request.get(`/api/wx-pay/downloadbill/${billDate}/${type}`)
  }
}
