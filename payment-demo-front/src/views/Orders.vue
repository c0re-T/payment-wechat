<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2 class="fl tac">
          <span class="c-333">订单列表</span>
        </h2>
      </header>
      <el-table :data="list" border style="width: 100%">
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column prop="orderNo" label="订单编号" width="230" ></el-table-column>
        <el-table-column prop="title" label="订单标题"></el-table-column>
        <el-table-column prop="totalFee" label="订单金额">
          <template #default="scope">
              {{scope.row.totalFee / 100}} 元
          </template>  
        </el-table-column>
        <el-table-column label="订单状态">
          <template #default="scope">
            <el-tag v-if="scope.row.orderStatus === '未支付'">
              {{scope.row.orderStatus}}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '支付成功'" type="success">
              {{ scope.row.orderStatus }}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '超时已关闭'" type="warning">
              {{scope.row.orderStatus}}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '用户已取消'" type="info">
              {{scope.row.orderStatus}}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '退款中'" type="danger">
              {{scope.row.orderStatus}}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '已退款'" type="info">
              {{scope.row.orderStatus}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间"></el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button v-if="scope.row.orderStatus === '未支付'" type="text" @click="cancel(scope.row.orderNo)">取消</el-button>
            <el-button v-if="scope.row.orderStatus === '支付成功'" type="text" @click="refund(scope.row.orderNo)">退款</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- 退款对话框 -->
   <el-dialog
      v-model="refundDialogVisible"
      @close="closeDialog"
      width="350px"
      center>
      <el-form>
        <el-form-item label="退款原因">
          <el-select v-model="reason" placeholder="请选择退款原因">
            <el-option label="不喜欢" value="不喜欢"></el-option>
            <el-option label="买错了" value="买错了"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="toRefunds()" :disabled="refundSubmitBtnDisabled">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import orderInfoApi from '../api/orderInfo'
import wxPayApi from '../api/wxPay'

const list = ref([]) //订单列表
const refundDialogVisible = ref(false) //退款弹窗
const orderNo = ref('') //退款订单号
const reason = ref('') //退款原因
const refundSubmitBtnDisabled = ref(false) //防止重复提交

//显示订单列表
function showOrderList () {
  orderInfoApi.list().then(response => {
    list.value = response.data
  })
}

//用户取消订单，参数不能叫 orderNo，否则会遮住同名 ref
function cancel (no) {
  wxPayApi.cancel(no).then(response => {
    ElMessage.success(response.message)
    //刷新订单列表
    showOrderList()
  })
}

//退款对话框
function refund (no) {
  refundDialogVisible.value = true
  orderNo.value = no
}

//关闭退款对话框
function closeDialog () {
  refundDialogVisible.value = false
  //还原组件状态
  orderNo.value = ''
  reason.value = ''
  refundSubmitBtnDisabled.value = false
}

//确认退款
function toRefunds () {
  refundSubmitBtnDisabled.value = true //禁用按钮，防止重复提交
  wxPayApi.refunds(orderNo.value, reason.value).then(() => {
    ElMessage.success("退款申请提交成功")
    closeDialog()
    showOrderList()
  })
}

showOrderList()
</script>
