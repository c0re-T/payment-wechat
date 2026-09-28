import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
// 封装axios实例给其他文件使用 比如api文件夹下的文件
const service = axios.create({
  baseURL: 'http://localhost:8090', // api 的 base_url
  timeout: 10000 // 请求超时时间
})

// request拦截器
service.interceptors.request.use(
  config => {
    return config
  },
  error => {
    // Do something with request error
    Promise.reject(error)
  }
)

// response 拦截器
service.interceptors.response.use(
  response => {
 
    const res = response.data
    if (res.code !== 200) {
      ElMessage({
        message: res.message,
        type: 'error',
        duration: 5 * 1000
      })

      return Promise.reject('error')
    } else {
      return response.data
    }
  },
  error => {
    ElMessage({
      message: error.message,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
