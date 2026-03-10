<template>
  <div class="content-card" style="padding: 24px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <div class="section-title" style="margin: 0;">{{ pageTitle }}</div>
      <el-tag>{{ orderType === 'MACHINE' ? '上机订单流转' : '送样订单流转' }}</el-tag>
    </div>
    <el-table :data="orders" border>
      <el-table-column prop="orderNo" label="订单编号" width="220" />
      <el-table-column prop="userName" label="申请人" width="120" />
      <el-table-column prop="instrumentName" label="仪器名称" />
      <el-table-column label="订单状态" width="160">
        <template #default="{ row }">
          <el-tag :type="orderTagType(row.status)">{{ orderStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="100" />
      <el-table-column label="操作" min-width="360">
        <template #default="{ row }">
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <el-button v-if="row.status === 'PENDING_AUDIT'" link type="primary" @click="audit(row, 'APPROVE')">审核通过</el-button>
            <el-button v-if="row.status === 'PENDING_AUDIT'" link type="danger" @click="audit(row, 'REJECT')">驳回</el-button>
            <el-button v-if="row.orderType === 'MACHINE' && row.status === 'WAITING_USE'" link @click="checkIn(row)">登记签到</el-button>
            <el-button v-if="row.orderType === 'MACHINE' && row.status === 'IN_USE'" link @click="finish(row)">结束使用</el-button>
            <el-button v-if="row.orderType === 'SAMPLE' && row.status === 'WAITING_RECEIVE'" link @click="receive(row)">接收样品</el-button>
            <el-button v-if="row.orderType === 'SAMPLE' && row.status === 'TESTING'" link @click="uploadResult(row)">上传结果</el-button>
            <el-button v-if="row.status === 'WAITING_SETTLEMENT'" link type="success" @click="settle(row)">结算完成</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  auditOrder,
  checkInOrder,
  finishOrder,
  getAdminOrders,
  receiveSampleOrder,
  settleOrder,
  uploadAdminOrderResult
} from '../../api/order'

export default {
  props: {
    orderType: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      orders: []
    }
  },
  computed: {
    pageTitle() {
      return this.orderType === 'MACHINE' ? '上机订单管理' : '送样订单管理'
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      const orders = await getAdminOrders()
      this.orders = this.orderType ? orders.filter(item => item.orderType === this.orderType) : orders
    },
    async audit(row, action) {
      const comment = await this.askComment(action === 'APPROVE' ? '订单审核通过' : '订单驳回')
      await auditOrder(row.id, { action, comment })
      ElMessage.success('订单审核状态已更新')
      await this.load()
    },
    async checkIn(row) {
      await checkInOrder(row.id)
      ElMessage.success('已完成签到登记')
      await this.load()
    },
    async receive(row) {
      await receiveSampleOrder(row.id)
      ElMessage.success('样品接收成功')
      await this.load()
    },
    async finish(row) {
      const comment = await this.askComment('结束上机使用')
      await finishOrder(row.id, { comment })
      ElMessage.success('上机订单已结束')
      await this.load()
    },
    async uploadResult(row) {
      const comment = await this.askComment('上传送样结果')
      await uploadAdminOrderResult(row.id, { comment })
      ElMessage.success('送样结果已上传')
      await this.load()
    },
    async settle(row) {
      await settleOrder(row.id)
      ElMessage.success('订单已结算')
      await this.load()
    },
    async askComment(title) {
      try {
        const { value } = await ElMessageBox.prompt('可选填写说明', title, {
          confirmButtonText: '确认',
          cancelButtonText: '跳过',
          inputValue: ''
        })
        return value || ''
      } catch (error) {
        return ''
      }
    },
    orderStatusLabel(value) {
      const mapping = {
        PENDING_AUDIT: '待审核',
        WAITING_USE: '待使用',
        IN_USE: '使用中',
        WAITING_RECEIVE: '待接样',
        TESTING: '测试中',
        WAITING_SETTLEMENT: '待结算',
        COMPLETED: '已完成',
        REJECTED: '已驳回',
        CANCELED: '已取消'
      }
      return mapping[value] || value || '-'
    },
    orderTagType(value) {
      const mapping = {
        PENDING_AUDIT: 'warning',
        WAITING_USE: 'info',
        IN_USE: 'primary',
        WAITING_RECEIVE: 'info',
        TESTING: 'primary',
        WAITING_SETTLEMENT: 'warning',
        COMPLETED: 'success',
        REJECTED: 'danger',
        CANCELED: 'info'
      }
      return mapping[value] || 'info'
    }
  }
}
</script>
