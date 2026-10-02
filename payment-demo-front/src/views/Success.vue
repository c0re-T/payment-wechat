<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <el-result icon="success" title="支付成功！" sub-title="订单状态以支付平台的异步通知为准">
        <template #extra>
          <el-button type="primary" @click="router.push('/orders')">查看我的订单</el-button>
        </template>
      </el-result>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

//支付宝页面支付付款完成后，浏览器会跳回后端配置的 return_url，这里就是落地页
const router = useRouter()
</script>
