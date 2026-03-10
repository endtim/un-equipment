<template>
  <div class="admin-page">
    <div class="grid-3">
      <div v-for="card in cards" :key="card.label" class="content-card admin-card">
        <div style="color: var(--muted);">{{ card.label }}</div>
        <div style="font-size: 30px; font-weight: 700; margin-top: 8px; color: var(--primary);">{{ card.value }}</div>
      </div>
    </div>
    <div class="grid-3">
      <div class="content-card admin-card">
        <h3 class="admin-card-title">部门预约分布</h3>
        <div v-for="(value, key) in overview.departmentDistribution || {}" :key="key" style="padding: 8px 0;">
          {{ key }}：{{ value }}
        </div>
      </div>
      <div class="content-card admin-card">
        <h3 class="admin-card-title">热门仪器排行</h3>
        <div v-for="item in overview.topInstruments || []" :key="item.key" style="padding: 8px 0;">
          {{ item.key }}：{{ item.value }}
        </div>
      </div>
      <div class="content-card admin-card">
        <h3 class="admin-card-title">汇总信息</h3>
        <div style="padding: 8px 0;">部门数量：{{ overview.departmentCount || 0 }}</div>
        <div style="padding: 8px 0;">完成订单：{{ overview.completionCount || 0 }}</div>
        <div style="padding: 8px 0;">收入金额：{{ formatAmount(overview.incomeAmount) }}</div>
      </div>
    </div>
    <div class="content-card admin-card">
      <h3 class="admin-card-title">订单趋势</h3>
      <div v-for="(value, key) in overview.orderTrend || {}" :key="key" style="padding: 8px 0;">
        {{ key }}：{{ value }}
      </div>
    </div>
  </div>
</template>

<script>
import { getOverview } from '../../api/stat'

export default {
  data() {
    return {
      overview: {},
      cards: []
    }
  },
  async created() {
    this.overview = await getOverview()
    this.cards = [
      { label: '仪器总数', value: this.overview.instrumentCount || 0 },
      { label: '预约总数', value: this.overview.reservationCount || 0 },
      { label: '完成服务数', value: this.overview.completionCount || 0 }
    ]
  },
  methods: {
    formatAmount(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>
