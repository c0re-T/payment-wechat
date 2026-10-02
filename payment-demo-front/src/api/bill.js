import request from '@/utils/request'

export default {
  //微信账单：data 为账单 CSV 正文
  downloadBill(billDate, type) {
    return request.get(`/api/wx-pay/downloadbill/${billDate}/${type}`)
  },

  //支付宝账单：data 中含 downloadUrl，账单文件在支付宝侧，需拿链接再下载
  downloadBillAliPay(billDate, type) {
    return request.get(`/api/ali-pay/bill/downloadurl/query/${billDate}/${type}`)
  }
}
