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
import { ElMessage } from 'element-plus'
import billApi from '../api/bill'

const billDate = ref('') //账单日期

//下载账单
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
</script>
