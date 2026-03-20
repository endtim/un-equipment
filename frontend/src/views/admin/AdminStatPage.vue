<template>
  <div class="admin-page">
    <admin-table-card title="统计筛选">
      <template #toolbar>
      <div class="admin-toolbar">
        <el-date-picker
          v-model="query.timeRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD[T]HH:mm:ss"
          style="width: 360px"
          @change="onQueryChange"
        />
        <el-button type="primary" @click="onQueryChange">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
      </template>
    </admin-table-card>

    <admin-summary-cards :items="cards" />

    <div class="grid-3">
      <div class="content-card admin-card">
        <h3 class="admin-card-title">仪器预约排行（Top5）</h3>
        <el-empty v-if="topInstruments.length === 0" description="暂无排行数据" />
        <div v-for="(item, index) in topInstruments" :key="`${item.key}-${index}`" class="list-row">
          <span>{{ index + 1 }}. {{ item.key }}</span>
          <span class="row-value">{{ item.value }}</span>
        </div>
      </div>

      <div class="content-card admin-card">
        <h3 class="admin-card-title">部门预约分布</h3>
        <el-empty v-if="departmentRows.length === 0" description="暂无分布数据" />
        <div v-for="row in departmentRows" :key="row.key" class="list-row">
          <span>{{ row.key }}</span>
          <span class="row-value">{{ row.value }}</span>
        </div>
      </div>

      <div class="content-card admin-card">
        <h3 class="admin-card-title">汇总信息</h3>
        <div class="list-row">
          <span>部门总数</span>
          <span class="row-value">{{ overview.departmentCount || 0 }}</span>
        </div>
        <div class="list-row">
          <span>完成订单</span>
          <span class="row-value">{{ overview.completionCount || 0 }}</span>
        </div>
        <div class="list-row">
          <span>待结算订单</span>
          <span class="row-value">{{ overview.waitingSettlementCount || 0 }}</span>
        </div>
        <div class="list-row">
          <span>已结算记录</span>
          <span class="row-value">{{ overview.settledCount || 0 }}</span>
        </div>
      </div>
    </div>

    <div class="content-card admin-card">
      <h3 class="admin-card-title">订单趋势</h3>
      <el-empty v-if="trendRows.length === 0" description="暂无趋势数据" />
      <div v-for="row in trendRows" :key="row.key" class="list-row">
        <span>{{ row.key }}</span>
        <span class="row-value">{{ row.value }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import AdminSummaryCards from '../../components/admin/AdminSummaryCards.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { getAdminOverview } from '../../api/stat'

export default {
  components: {
    AdminSummaryCards,
    AdminTableCard
  },
  data() {
    return {
      overview: {},
      cards: [],
      query: {
        timeRange: []
      }
    }
  },
  computed: {
    topInstruments() {
      return this.overview.topInstruments || []
    },
    departmentRows() {
      const source = this.overview.departmentDistribution || {}
      return Object.keys(source).map((key) => ({ key, value: source[key] }))
    },
    trendRows() {
      const source = this.overview.orderTrend || {}
      return Object.keys(source).map((key) => ({ key, value: source[key] }))
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    buildParams() {
      const [startTime, endTime] = this.query.timeRange || []
      return {
        startTime: startTime || undefined,
        endTime: endTime || undefined
      }
    },
    async load() {
      this.overview = await getAdminOverview(this.buildParams())
      this.cards = [
        { label: '仪器总数', value: this.overview.instrumentCount || 0 },
        { label: '预约总数', value: this.overview.reservationCount || 0 },
        { label: '服务收入(元)', value: this.formatAmount(this.overview.incomeAmount) },
        { label: '平均订单金额(元)', value: this.formatAmount(this.overview.averageOrderAmount) },
        { label: '待结算订单', value: this.overview.waitingSettlementCount || 0 },
        { label: '完成服务数', value: this.overview.completionCount || 0 }
      ]
    },
    async onQueryChange() {
      await this.load()
    },
    async resetQuery() {
      this.query.timeRange = []
      await this.load()
    },
    formatAmount(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>

<style scoped>
.list-row {
  padding: 8px 0;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px dashed #eef2f7;
}

.list-row:last-child {
  border-bottom: none;
}

.row-value {
  color: #1b4f9a;
  font-weight: 600;
}
</style>
