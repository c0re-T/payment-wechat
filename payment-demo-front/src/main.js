// Vue 应用实例
import { createApp } from 'vue'
// ElementPlus，替代 element-ui
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 二维码生成器，替代 vue-qriously
import QrcodeVue from 'qrcode.vue'
// 引入App
import App from './App.vue'
// 引入路由器
import router from './router'

createApp(App)
  .use(router)
  .use(ElementPlus)
  .component('qrcode-vue', QrcodeVue)
  .mount('#app')
