<template>
  <div class="bg-fa of">
    <!-- 顶部横幅 -->
    <section class="hero">
      <div class="container">
        <p class="hero-tag">微信支付 APIv3 · 实战演示</p>
        <h2 class="hero-title">选择课程，扫码即刻开通</h2>
        <p class="hero-desc">从下单、扫码支付、异步通知到退款与账单下载，完整走通一条真实的支付链路。</p>
        <ul class="hero-feature">
          <li>官方 SDK 自动签名验签</li>
          <li>支付结果实时轮询回显</li>
          <li>支持订单取消与原路退款</li>
        </ul>
      </div>
    </section>

    <section id="index" class="container">
      <div class="checkout">
        <!-- 左栏：课程与支付方式 -->
        <div class="checkout-main">
          <header class="comm-title">
            <h2><span class="c-333">选择课程</span></h2>
          </header>
          <ul class="course-grid">
            <li v-for="(product, index) in productList" :key="product.id">
              <a :class="['course-card', { current: payOrder.productId === product.id }]"
                 @click="selectItem(product.id)"
                 href="javascript:void(0);">
                <span class="course-no">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="course-body">
                  <span class="course-name">{{ product.title }}</span>
                  <span class="course-price">¥{{ product.price / 100 }}</span>
                </span>
                <span class="course-mark"></span>
              </a>
            </li>
          </ul>

          <header class="comm-title">
            <h2><span class="c-333">支付方式</span></h2>
          </header>
          <div class="channel-panel">
            <div v-for="channel in payChannels" :key="channel.type"
                 :class="['channel-item', { current: payOrder.payType === channel.type }]"
                 @click="selectPayType(channel.type)">
              <img :src="channel.icon" class="channel-icon" :alt="channel.name">
              <span class="channel-info">
                <span class="channel-name">{{ channel.name }}</span>
                <span class="channel-desc">{{ channel.desc }}</span>
              </span>
              <span class="channel-radio"></span>
            </div>
          </div>
        </div>

        <!-- 右栏：订单摘要 -->
        <aside class="checkout-aside">
          <div class="summary">
            <h3 class="summary-title">订单摘要</h3>
            <dl class="summary-row">
              <dt>已选课程</dt>
              <dd>{{ selectedProduct ? selectedProduct.title : '暂未选择' }}</dd>
            </dl>
            <dl class="summary-row">
              <dt>支付方式</dt>
              <dd>{{ currentChannelName }}</dd>
            </dl>
            <dl class="summary-row">
              <dt>课程金额</dt>
              <dd>{{ selectedProduct ? '¥' + selectedProduct.price / 100 : '-' }}</dd>
            </dl>
            <div class="summary-total">
              <span>合计</span>
              <strong>¥{{ selectedProduct ? selectedProduct.price / 100 : '0.00' }}</strong>
            </div>
            <el-button
              class="summary-pay"
              type="warning"
              size="large"
              :disabled="payBtnDisabled"
              @click="toPay()">
              确认支付
            </el-button>
            <p class="summary-tip">本页面为学习演示，支付流程使用真实接口，请谨慎操作。</p>
          </div>
        </aside>
      </div>
    </section>

    <!-- 微信支付二维码 -->
    <el-dialog
      v-model="codeDialogVisible"
      :show-close="false"
      @close="closeDialog"
      width="360px"
      center>
      <div class="pay-qrcode">
        <qrcode-vue :value="codeUrl" :size="260" level="M"/>
        <p class="pay-qrcode-tip">使用微信扫码支付</p>
        <p class="pay-qrcode-sub">支付完成后将自动跳转至订单列表</p>
        <p class="pay-qrcode-sub">谨慎操作，涉及金额</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import productApi from '../api/product'
import wxPayApi from '../api/wxPay'
import orderInfoApi from '../api/orderInfo'
import wxpayIcon from '../assets/img/wxpay.png'
import alipayIcon from '../assets/img/alipay.png'

const router = useRouter()

const payBtnDisabled = ref(false) //确认支付按钮是否禁用
const codeDialogVisible = ref(false) //微信支付二维码弹窗
const productList = ref([]) //商品列表
const payOrder = reactive({ //订单信息
  productId: '', //商品id
  payType: 'wxpay' //支付方式
})
const codeUrl = ref('') //二维码
const orderNo = ref('') //订单号
let timer = null //定时器句柄，不参与渲染所以不用 ref

//可选支付渠道，新增渠道只需在此追加
const payChannels = [
  { type: 'wxpay', name: '微信支付', desc: '使用微信扫描二维码完成付款', icon: wxpayIcon },
  { type: 'alipay', name: '支付宝', desc: '支持扫码或登录账户付款', icon: alipayIcon }
]

//当前选中的商品
const selectedProduct = computed(() =>
  productList.value.find(item => item.id === payOrder.productId))

//当前选中的支付方式名称
const currentChannelName = computed(() => {
  const channel = payChannels.find(item => item.type === payOrder.payType)
  return channel ? channel.name : '-'
})

//获取商品列表
productApi.list().then(response => {
  productList.value = response.data
  payOrder.productId = productList.value[0].id
})

//选择商品
function selectItem (productId) {
  payOrder.productId = productId
}

//选择支付方式
function selectPayType (type) {
  payOrder.payType = type
}

//确认支付
function toPay () {
  //微信支付
  if (payOrder.payType === 'wxpay') {
    payBtnDisabled.value = true

    //调用统一下单接口
    wxPayApi.nativePay(payOrder.productId).then(response => {
      codeUrl.value = response.data.codeUrl
      orderNo.value = response.data.orderNo
      codeDialogVisible.value = true

      // 启动定时器
      timer = setInterval(() => {
        queryOrderStatus()
      }, 1000)
      // timer = setInterval(queryOrderStatus, 3000)
    })
    return
  }

  //支付宝支付尚未接入，避免按钮被禁用后无法恢复
  ElMessage.info('支付宝支付通道暂未开通，请先使用微信支付')
}

//关闭微信支付二维码对话框时启用“确认支付”按钮
function closeDialog () {
  payBtnDisabled.value = false
  clearInterval(timer)
}

// 查询订单状态
function queryOrderStatus () {
  orderInfoApi.queryOrderStatus(orderNo.value).then(response => {
    console.log(response.message)
    // 支付成功后的页面跳转，R 的成功码是 200
    if (response.data) {
      console.log(response.message)
      clearInterval(timer)
      // 三秒后跳转到订单列表
      setTimeout(() => router.push('/orders'), 1000)
    }
  })
}

//离开页面时兜底清除轮询，避免定时器泄漏后继续跳转
onUnmounted(() => clearInterval(timer))
</script>
