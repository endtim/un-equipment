<template>
  <div class="content-card" style="padding: 24px;">
    <div class="section-title">{{ pageTitle }}</div>
    <el-table :data="orders" border>
      <el-table-column prop="orderNo" label="订单编号" width="220" />
      <el-table-column label="订单类型" width="120">
        <template #default="{ row }">{{ orderTypeLabel(row.orderType) }}</template>
      </el-table-column>
      <el-table-column prop="instrumentName" label="仪器名称" />
      <el-table-column label="状态" width="160">
        <template #default="{ row }">{{ orderStatusLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="100" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="canCancel(row)" link type="danger" @click="cancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailVisible" title="订单详情" size="40%">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单编号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">{{ orderTypeLabel(detail.orderType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ orderStatusLabel(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="仪器名称">{{ detail.instrumentName }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px;">
          <div class="section-title" style="font-size: 18px;">审核记录</div>
          <el-timeline>
            <el-timeline-item
              v-for="item in detail.auditRecords || []"
              :key="item.id"
              :timestamp="item.auditTime"
            >
              {{ auditResultLabel(item.auditResult) }} - {{ item.auditOpinion }}
            </el-timeline-item>
          </el-timeline>
        </div>

        <div v-if="detail.usageRecord" style="margin-top: 20px;">
          <div class="section-title" style="font-size: 18px;">使用记录</div>
          <div>签到时间：{{ detail.usageRecord.checkinTime || '-' }}</div>
          <div>开始时间：{{ detail.usageRecord.startTime || '-' }}</div>
          <div>结束时间：{{ detail.usageRecord.endTime || '-' }}</div>
        </div>

        <div v-if="detail.sampleDetail" style="margin-top: 20px;">
          <div class="section-title" style="font-size: 18px;">送样信息</div>
          <div>样品名称：{{ detail.sampleDetail.sampleName }}</div>
          <div>样品数量：{{ detail.sampleDetail.sampleCount }}</div>
          <div>测试状态：{{ sampleTestingStatusLabel(detail.sampleDetail.testingStatus) }}</div>
          <div>结果摘要：{{ detail.sampleDetail.resultSummary || '-' }}</div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script>
import { cancelOrder, getMyOrders, getOrderDetail } from '../../api/order'

export default {
  props: {
    orderType: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      orders: [],
      detailVisible: false,
      detail: null
    }
  },
  computed: {
    pageTitle() {
      if (this.orderType === 'MACHINE') {
        return '我的上机预约'
      }
      if (this.orderType === 'SAMPLE') {
        return '我的送样预约'
      }
      return '我的订单'
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      const orders = await getMyOrders()
      this.orders = this.orderType ? orders.filter(item => item.orderType === this.orderType) : orders
    },
    canCancel(row) {
      return !['COMPLETED', 'CANCELED'].includes(row.status)
    },
    async openDetail(row) {
      this.detail = await getOrderDetail(row.id)
      this.detailVisible = true
    },
    async cancel(row) {
      await cancelOrder(row.id)
      this.$message.success('订单已取消')
      await this.load()
    },
    orderTypeLabel(value) {
      const mapping = {
        MACHINE: '上机预约',
        SAMPLE: '送样预约'
      }
      return mapping[value] || value || '-'
    },
    orderStatusLabel(value) {
      const mapping = {
        PENDING_AUDIT: '待审核',
        WAITING_USE: '待使用',
        IN_USE: '使用中',
        WAITING_RECEIVE: '待接样',
        TESTING: '测试中',
        WAITING_RESULT: '待结果确认',
        WAITING_SETTLEMENT: '待结算',
        COMPLETED: '已完成',
        REJECTED: '已驳回',
        CANCELED: '已取消'
      }
      return mapping[value] || value || '-'
    },
    auditResultLabel(value) {
      const mapping = {
        PASS: '通过',
        REJECT: '驳回',
        PENDING: '待审核'
      }
      return mapping[value] || value || '-'
    },
    sampleTestingStatusLabel(value) {
      const mapping = {
        WAITING_RECEIVE: '待接样',
        TESTING: '测试中',
        WAITING_RESULT: '待结果确认',
        COMPLETED: '已完成'
      }
      return mapping[value] || value || '-'
    }
  }
}
</script>
