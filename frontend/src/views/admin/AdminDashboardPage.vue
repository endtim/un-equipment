<template>
  <div class="admin-page">
    <div class="grid-3">
      <div v-for="card in cards" :key="card.label" class="content-card admin-card">
        <div style="color: var(--muted);">{{ card.label }}</div>
        <div style="font-size: 32px; font-weight: 800; margin-top: 8px;">{{ card.value }}</div>
      </div>
    </div>

    <div class="content-card admin-card">
      <h3 class="admin-card-title">快捷入口</h3>
      <div style="display: flex; gap: 10px; flex-wrap: wrap;">
        <el-button type="primary" @click="$router.push('/admin/orders/machine')">上机订单</el-button>
        <el-button @click="$router.push('/admin/orders/sample')">送样订单</el-button>
        <el-button @click="$router.push('/admin/recharges')">充值审核</el-button>
        <el-button @click="$router.push('/admin/users')">用户管理</el-button>
        <el-button @click="$router.push('/admin/instruments')">仪器管理</el-button>
        <el-button @click="$router.push('/admin/notices')">公告管理</el-button>
        <el-button @click="$router.push('/admin/stats')">统计报表</el-button>
        <el-button @click="$router.push('/admin/logs')">日志审计</el-button>
      </div>
    </div>

    <div class="grid-3">
      <div class="content-card admin-card">
        <h3 class="admin-card-title">部门分布</h3>
        <div v-for="(value, key) in overview.departmentDistribution || {}" :key="key" style="padding: 8px 0;">
          {{ key }}：{{ value }}
        </div>
      </div>
      <div class="content-card admin-card">
        <h3 class="admin-card-title">热门仪器</h3>
        <div v-for="item in overview.topInstruments || []" :key="item.key" style="padding: 8px 0;">
          {{ item.key }}：{{ item.value }}
        </div>
      </div>
      <div class="content-card admin-card">
        <h3 class="admin-card-title">收入概况</h3>
        <div style="padding: 8px 0;">累计收入：{{ formatAmount(overview.incomeAmount) }}</div>
        <div style="padding: 8px 0;">完成服务：{{ overview.completionCount || 0 }}</div>
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
      { label: '收入金额', value: this.formatAmount(this.overview.incomeAmount) }
    ]
  },
  methods: {
    formatAmount(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>
