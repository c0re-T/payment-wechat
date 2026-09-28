<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2 class="fl tac">
          <span class="c-333">账单申请</span>
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

  </div>
</template>

<script setup>
import { ref } from 'vue'
import billApi from '../api/bill'

const billDate = ref('') //账单日期

//下载账单
function downloadBill (type) {
  //获取账单内容
  billApi.downloadBill(billDate.value, type).then(response => {
    const element = document.createElement('a')
    element.setAttribute('href', 'data:application/vnd.ms-excel;charset=utf-8,' + encodeURIComponent(response.data.result))
    element.setAttribute('download', billDate.value + '-' + type)
    element.style.display = 'none'
    element.click()
  })
}
</script>
