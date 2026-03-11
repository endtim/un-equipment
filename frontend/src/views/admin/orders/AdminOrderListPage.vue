<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <div class="page-head">
        <h3 class="admin-card-title page-title">{{ title }}</h3>
        <el-tag>{{ orderType === 'MACHINE' ? '上机流程' : '送样流程' }}</el-tag>
      </div>
      <el-table :data="orders" border>
        <el-table-column prop="orderNo" label="订单编号" width="220" />
        <el-table-column prop="userName" label="申请人" width="130" />
        <el-table-column prop="instrumentName" label="仪器名称" min-width="180" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column label="操作" min-width="360">
          <template #default="{ row }">
            <div class="action-wrap">
              <el-button v-if="row.status === 'PENDING_AUDIT'" link type="primary" @click="audit(row, 'APPROVE')">审核通过</el-button>
              <el-button v-if="row.status === 'PENDING_AUDIT'" link type="danger" @click="audit(row, 'REJECT')">驳回</el-button>
              <el-button v-if="row.orderType === 'MACHINE' && row.status === 'WAITING_USE'" link @click="checkIn(row)">签到</el-button>
              <el-button v-if="row.orderType === 'MACHINE' && row.status === 'IN_USE'" link @click="finish(row)">结束使用</el-button>
              <el-button v-if="row.orderType === 'SAMPLE' && row.status === 'WAITING_RECEIVE'" link @click="receive(row)">接样</el-button>
              <el-button v-if="row.orderType === 'SAMPLE' && row.status === 'TESTING'" link @click="uploadResult(row)">上传结果</el-button>
              <el-button v-if="row.status === 'WAITING_SETTLEMENT'" link type="success" @click="settle(row)">结算完成</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="orders.length === 0" description="暂无数据" class="admin-empty" />
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  orderStatusLabel as orderStatusLabelDict,
  orderStatusTagType as orderStatusTagTypeDict
} from '../../../utils/dicts'
import {
  auditOrder,
  checkInOrder,
  finishOrder,
  getAdminOrders,
  receiveSampleOrder,
  settleOrder,
  uploadAdminOrderResult
} from '../../../api/order'

export default {
  props: {
    orderType: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      orders: []
    }
  },
  computed: {
    title() {
      return this.orderType === 'MACHINE' ? '上机订单管理' : '送样订单管理'
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      const all = await getAdminOrders()
      this.orders = all.filter(item => item.orderType === this.orderType)
    },
    async audit(row, action) {
      const comment = await this.askComment(action === 'APPROVE' ? '审核通过' : '驳回订单')
      await auditOrder(row.id, { action, comment })
      ElMessage.success('审核状态已更新')
      await this.load()
    },
    async checkIn(row) {
      await checkInOrder(row.id)
      ElMessage.success('已签到')
      await this.load()
    },
    async receive(row) {
      await receiveSampleOrder(row.id)
      ElMessage.success('样品已接收')
      await this.load()
    },
    async finish(row) {
      const comment = await this.askComment('结束上机')
      await finishOrder(row.id, { comment })
      ElMessage.success('订单已结束')
      await this.load()
    },
    async uploadResult(row) {
      const comment = await this.askComment('上传送样结果')
      await uploadAdminOrderResult(row.id, { comment })
      ElMessage.success('结果已上传')
      await this.load()
    },
    async settle(row) {
      await settleOrder(row.id)
      ElMessage.success('订单已结算')
      await this.load()
    },
    async askComment(title) {
      try {
        const { value } = await ElMessageBox.prompt('可选填写备注', title, {
          confirmButtonText: '确认',
          cancelButtonText: '跳过',
          inputValue: ''
        })
        return value || ''
      } catch (error) {
        return ''
      }
    },
    statusLabel(value) {
      return orderStatusLabelDict(value)
    },
    statusTagType(value) {
      return orderStatusTagTypeDict(value)
    }
  }
}
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.page-title {
  margin: 0;
}

.action-wrap {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
