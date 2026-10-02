<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2 class="fl tac">
          <span class="c-333">微信账单申请</span>
        </h2>
      </header>
      
      <el-form :inline="true" >
        <el-form-item>
            <el-date-picker v-model="billDate" value-format="YYYY-MM-DD" placeholder="选择账单日期" />
        </el-form-item>
        <el-form-item>
            <el-button type="primary" @click="downloadBill('tradebill')">下载交易账单</el-button>
        </el-form-item>
         <el-form-item>
            <el-button type="primary" @click="downloadBill('fundflowbill')">下载资金账单</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section id="index" class="container">
      <header class="comm-title">
        <h2 class="fl tac">
          <span class="c-333">支付宝账单申请</span>
        </h2>
      </header>

      <el-form :inline="true" >
        <el-form-item>
            <el-date-picker v-model="billDateAliPay" value-format="YYYY-MM-DD" placeholder="选择账单日期" />
        </el-form-item>
        <el-form-item>
            <el-button type="primary" @click="downloadBillAliPay('trade')">下载交易账单</el-button>
        </el-form-item>
         <el-form-item>
            <el-button type="primary" @click="downloadBillAliPay('signcustomer')">下载资金账单</el-button>
        </el-form-item>
      </el-form>
    </section>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import billApi from '../api/bill'

const billDate = ref('') //微信支付账单日期
const billDateAliPay = ref('') //支付宝账单日期

//下载账单：微信支付
function downloadBill (type) {
  if (!billDate.value) {
    ElMessage.warning('请先选择账单日期')
    return
  }
  //获取账单内容（后端 R.data 即 CSV 正文）
  billApi.downloadBill(billDate.value, type).then(response => {
    const csv = response.data
    //加 UTF-8 BOM 防止 Excel 打开中文乱码；类型用 text/csv 与扩展名一致
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const element = document.createElement('a')
    element.href = url
    //扩展名必须是 .csv，不能写 .xls，否则 Excel 报“格式与扩展名不匹配”
    element.download = `${billDate.value}-${type}.csv`
    element.style.display = 'none'
    document.body.appendChild(element)
    element.click()
    //下载完清理，避免内存泄漏
    document.body.removeChild(element)
    window.URL.revokeObjectURL(url)
  })
}

//下载账单：支付宝
function downloadBillAliPay (type) {
  if (!billDateAliPay.value) {
    ElMessage.warning('请先选择账单日期')
    return
  }
  //支付宝的账单文件不在本地，后端 R.data 即账单下载地址，交给浏览器直接下载
  billApi.downloadBillAliPay(billDateAliPay.value, type).then(response => {
    const element = document.createElement('a')
    element.href = response.data
    //跨域链接上的 download 属性浏览器不认，只能新开标签页由支付宝决定文件名
    element.target = '_blank'
    element.rel = 'noopener'
    element.style.display = 'none'
    document.body.appendChild(element)
    element.click()
    document.body.removeChild(element)
  })
}
</script>
