<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2 class="fl tac">
          <span class="c-333">课程列表</span>
        </h2>
      </header>
      <ul>
        <li v-for="product in productList" :key="product.id">
          <a :class="['orderBtn', {current:payOrder.productId === product.id}]"
            @click="selectItem(product.id)"
            href="javascript:void(0);" >
            <span class="orderBtn-title">{{product.title}}</span>
            <span class="orderBtn-price">¥{{product.price / 100}}</span>
          </a>
        </li>
      </ul>

      
      <div class="PaymentChannel_payment-channel-panel">
        <h3 class="PaymentChannel_title"> 
          选择支付方式 
        </h3>
        <div class="PaymentChannel_channel-options" >

          <!-- 选择微信 -->
          <div :class="['ChannelOption_payment-channel-option', {current:payOrder.payType === 'wxpay'}]"
          @click="selectPayType('wxpay')">
            <div class="ChannelOption_channel-icon">
              <img src="../assets/img/wxpay.png" class="ChannelOption_icon">
            </div>
            <div class="ChannelOption_channel-info">
              <div class="ChannelOption_channel-label">
                <div class="ChannelOption_label">微信支付</div>
                <div class="ChannelOption_sub-label"></div>
                <div class="ChannelOption_check-option"></div>
              </div>
            </div>
          </div>

          <!-- 选择支付宝 -->
          <div :class="['ChannelOption_payment-channel-option', {current:payOrder.payType === 'alipay'}]"
          @click="selectPayType('alipay')">
            <div class="ChannelOption_channel-icon">
              <img src="../assets/img/alipay.png" class="ChannelOption_icon">
            </div>
            <div class="ChannelOption_channel-info">
              <div class="ChannelOption_channel-label">
                <div class="ChannelOption_label">支付宝</div>
                <div class="ChannelOption_sub-label"></div>
                <div class="ChannelOption_check-option"></div>
              </div>
            </div>
          </div>

        </div>
      </div>

      <div class="payButtom">
        <el-button 
        :disabled="payBtnDisabled"
        type="warning" 
        round 
        style="width: 180px;height: 44px;font-size: 18px;"
        @click="toPay()">
          确认支付
        </el-button>
      </div>
    </section>

    <!-- 微信支付二维码 -->
    <el-dialog
      v-model="codeDialogVisible"
      :show-close="false"
      @close="closeDialog"
      width="350px"
      center>
     <qrcode-vue :value="codeUrl" :size="300"/>
        <!-- <img src="../assets/img/code.png" alt="" style="width:100%"><br> -->
        使用微信扫码支付
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import productApi from '../api/product'
import wxPayApi from '../api/wxPay'
import orderInfoApi from '../api/orderInfo'

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
  payBtnDisabled.value = true

  //微信支付
  if (payOrder.payType === 'wxpay') {
    //调用统一下单接口
    wxPayApi.nativePay(payOrder.productId).then(response => {
      codeUrl.value = response.data.codeUrl
      orderNo.value = response.data.orderNo
      codeDialogVisible.value = true

      // 启动定时器
      timer = setInterval(queryOrderStatus, 3000)
    })
  }
}

//关闭微信支付二维码对话框时启用“确认支付”按钮
function closeDialog () {
  payBtnDisabled.value = false
  clearInterval(timer)
}

// 查询订单状态
function queryOrderStatus () {
  orderInfoApi.queryOrderStatus(orderNo.value).then(response => {
    // 支付成功后的页面跳转，R 的成功码是 200
    if (response.code === 200) {
      clearInterval(timer)
      // 三秒后跳转到订单列表
      setTimeout(() => router.push('/orders'), 3000)
    }
  })
}

//离开页面时兜底清除轮询，避免定时器泄漏后继续跳转
onUnmounted(() => clearInterval(timer))
</script>
