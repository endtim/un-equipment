<template>
  <div>
    <div class="grid-3" style="margin-bottom: 14px;">
      <div v-for="card in summaryCards" :key="card.label" class="content-card" style="padding: 16px 18px;">
        <div style="color: var(--muted); font-size: 13px;">{{ card.label }}</div>
        <div style="margin-top: 6px; font-size: 26px; font-weight: 700; color: var(--primary);">{{ card.value }}</div>
      </div>
    </div>

    <div class="content-card" style="padding: 24px;">
      <div class="section-title">{{ pageTitle }}</div>
      <div class="mode-tip">{{ pageTip }}</div>

      <el-table :data="orders" border>
        <el-table-column prop="orderNo" label="订单编号" width="220" />
        <el-table-column v-if="showOrderTypeColumn" label="订单类型" width="120">
          <template #default="{ row }">{{ orderTypeLabel(row.orderType) }}</template>
        </el-table-column>
        <el-table-column prop="instrumentName" label="仪器名称" min-width="160" />
        <el-table-column v-if="isMachinePage" label="预约时段" min-width="320">
          <template #default="{ row }">{{ formatRange(row.reservedStart, row.reservedEnd) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180" :formatter="formatDateTimeCell" />
        <el-table-column label="状态" width="170">
          <template #default="{ row }">{{ orderStatusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="110" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canCancel(row)" link type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="emptyText" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        class="pagination"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>

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
              :timestamp="formatDateTime(item.auditTime)"
            >
              {{ auditResultLabel(item.auditResult) }} - {{ item.auditOpinion }}
            </el-timeline-item>
          </el-timeline>
        </div>

        <div v-if="detail.usageRecord" style="margin-top: 20px;">
          <div class="section-title" style="font-size: 18px;">使用记录</div>
          <div>签到时间：{{ formatDateTime(detail.usageRecord.checkinTime) }}</div>
          <div>开始时间：{{ formatDateTime(detail.usageRecord.startTime) }}</div>
          <div>结束时间：{{ formatDateTime(detail.usageRecord.endTime) }}</div>
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
import {
  auditResultLabel as auditResultLabelDict,
  orderStatusLabel as orderStatusLabelDict,
  orderTypeLabel as orderTypeLabelDict,
  sampleTestingStatusLabel as sampleTestingStatusLabelDict
} from '../../utils/dicts'
import { formatDateTime as formatDateTimeUtil } from '../../utils/datetime'

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
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 10
      },
      detailVisible: false,
      detail: null
    }
  },
  computed: {
    isMachinePage() {
      return this.orderType === 'MACHINE'
    },
    isSamplePage() {
      return this.orderType === 'SAMPLE'
    },
    showOrderTypeColumn() {
      return !this.isMachinePage && !this.isSamplePage
    },
    pageTitle() {
      if (this.isMachinePage) {
        return '我的上机预约'
      }
      if (this.isSamplePage) {
        return '我的送样预约'
      }
      return '我的订单'
    },
    pageTip() {
      if (this.isMachinePage) {
        return '展示上机类预约单，重点关注预约时段和上机进度。'
      }
      if (this.isSamplePage) {
        return '展示送样类预约单，重点关注接样、测试与结果状态。'
      }
      return '展示全部订单，可统一查看状态与金额。'
    },
    summaryCards() {
      const total = this.total
      const waiting = this.orders.filter(item => ['PENDING_AUDIT', 'WAITING_USE', 'WAITING_RECEIVE', 'TESTING'].includes(item.status)).length
      const completed = this.orders.filter(item => item.status === 'COMPLETED').length
      const totalAmount = this.orders.reduce((sum, item) => sum + Number(item.amount || 0), 0)
      return [
        { label: '订单总数', value: String(total) },
        { label: '进行中', value: String(waiting) },
        { label: '已完成', value: String(completed) },
        { label: '累计金额', value: totalAmount.toFixed(2) }
      ]
    },
    emptyText() {
      if (this.isMachinePage) {
        return '暂无上机预约数据'
      }
      if (this.isSamplePage) {
        return '暂无送样预约数据'
      }
      return '暂无订单数据'
    }
  },
  watch: {
    orderType: {
      immediate: true,
      async handler() {
        this.query.pageNum = 1
        await this.load()
      }
    }
  },
  methods: {
    formatDateTimeCell(row, column, value) {
      return this.formatDateTime(value)
    },
    formatRange(start, end) {
      const startText = this.formatDateTime(start)
      const endText = this.formatDateTime(end)
      if (startText === '-' && endText === '-') {
        return '-'
      }
      return `${startText} ~ ${endText}`
    },
    async load() {
      const params = {
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize
      }
      if (this.orderType) {
        params.orderType = this.orderType
      }
      const page = await getMyOrders(params)
      this.orders = page.list || []
      this.total = page.total || 0
    },
    async onSizeChange() {
      this.query.pageNum = 1
      await this.load()
    },
    canCancel(row) {
      return ['PENDING_AUDIT', 'WAITING_USE', 'WAITING_RECEIVE'].includes(row.status)
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
      return orderTypeLabelDict(value)
    },
    orderStatusLabel(value) {
      return orderStatusLabelDict(value)
    },
    auditResultLabel(value) {
      return auditResultLabelDict(value)
    },
    sampleTestingStatusLabel(value) {
      return sampleTestingStatusLabelDict(value)
    },
    formatDateTime(value) {
      return formatDateTimeUtil(value)
    }
  }
}
</script>

<style scoped>
.mode-tip {
  margin: 4px 0 14px;
  color: #6f86a2;
  font-size: 13px;
}

.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
