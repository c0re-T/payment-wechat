import request from '@/utils/request'

export default {
  list() {
    return request.get('/api/product/list')
  }
}
