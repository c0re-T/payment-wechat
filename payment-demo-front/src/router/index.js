// 创建应用程序的路由器
import { createRouter, createWebHistory } from 'vue-router'

// 引入组件
import Index from '../views/index.vue'
import Orders from '../views/Orders.vue'
import Download from '../views/Download.vue'
import Success from '../views/Success.vue'

// 创建并暴露一个路由器
export default createRouter({
    history: createWebHistory(),
    routes:[
        {
            path: '/',
            component: Index
        },
        {
            path: '/orders',
            component: Orders
        },
        {
            path: '/download',
            component: Download
        },
        //支付宝收银台付款后的回跳落地页，不进导航菜单
        {
            path: '/success',
            component: Success
        }
    ]
})
