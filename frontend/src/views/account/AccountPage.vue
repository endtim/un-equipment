<template>
  <div class="account-page">
    <div class="grid-3" style="margin-bottom: 18px;">
      <div v-for="card in cards" :key="card.label" class="content-card" style="padding: 20px;">
        <div style="color: var(--muted);">{{ card.label }}</div>
        <div style="font-size: 32px; font-weight: 700; margin-top: 8px; color: var(--primary);">{{ card.value }}</div>
      </div>
    </div>

    <div v-if="isRechargePage" class="content-card" style="padding: 22px; margin-bottom: 18px;">
      <div class="section-title" style="font-size: 22px;">充值申请</div>
      <el-form :model="recharge" label-width="90px" style="max-width: 720px;">
        <el-form-item label="充值金额">
          <el-input-number v-model="recharge.amount" :min="1" :step="100" style="width: 240px;" />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input v-model="recharge.remark" type="textarea" :rows="3" placeholder="可填写转账说明、用途备注等信息" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">提交充值申请</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="isAccountPage" class="content-card" style="padding: 18px 22px; margin-bottom: 18px; display: flex; justify-content: space-between; align-items: center;">
      <div style="color: #5f7897;">当前页展示资金明细与充值记录，充值操作请进入“充值申请”。</div>
      <el-button type="primary" plain @click="$router.push('/center/recharge')">去充值申请</el-button>
    </div>

    <div class="grid-3">
      <div class="content-card" style="padding: 24px; grid-column: span 2;">
        <div class="section-title" style="font-size: 22px;">{{ isRechargePage ? '最近交易流水' : '交易流水' }}</div>
        <el-table :data="account.transactions || []" border>
          <el-table-column prop="txnType" label="交易类型" width="120">
            <template #default="{ row }">{{ txnTypeLabel(row.txnType) }}</template>
          </el-table-column>
          <el-table-column prop="inoutType" label="收支方向" width="120">
            <template #default="{ row }">{{ inoutTypeLabel(row.inoutType) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120" />
          <el-table-column prop="remark" label="备注" />
          <el-table-column prop="createTime" label="发生时间" width="180" :formatter="formatDateTimeCell" />
        </el-table>
      </div>

      <div class="content-card" style="padding: 24px;">
        <div class="section-title" style="font-size: 22px;">{{ isRechargePage ? '充值进度' : '充值记录' }}</div>
        <el-table :data="account.recharges || []" border size="small">
          <el-table-column prop="rechargeNo" label="充值单号" />
          <el-table-column prop="amount" label="金额" width="90" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="rechargeTagType(row.status)" size="small">{{ rechargeStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { getMyAccount, submitRecharge } from '../../api/account'
import {
  inoutTypeLabel as inoutTypeLabelDict,
  rechargeStatusLabel as rechargeStatusLabelDict,
  rechargeStatusTagType as rechargeStatusTagTypeDict,
  txnTypeLabel as txnTypeLabelDict
} from '../../utils/dicts'
import { formatDateTime } from '../../utils/datetime'

export default {
  props: {
    mode: {
      type: String,
      default: 'account'
    }
  },
  data() {
    return {
      account: {},
      recharge: {
        amount: 100,
        remark: ''
      }
    }
  },
  computed: {
    isRechargePage() {
      return this.mode === 'recharge'
    },
    isAccountPage() {
      return !this.isRechargePage
    },
    cards() {
      if (this.isRechargePage) {
        return [
          { label: '当前可用余额', value: this.formatAmount(this.account.balance) },
          { label: '待处理充值单', value: String((this.account.recharges || []).filter(item => item.status === 'PENDING').length) },
          { label: '累计充值', value: this.formatAmount(this.account.totalRecharge) }
        ]
      }
      return [
        { label: '当前余额', value: this.formatAmount(this.account.balance) },
        { label: '累计充值', value: this.formatAmount(this.account.totalRecharge) },
        { label: '累计消费', value: this.formatAmount(this.account.totalConsume) }
      ]
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    formatDateTimeCell(row, column, value) {
      return formatDateTime(value)
    },
    async load() {
      this.account = await getMyAccount()
    },
    async submit() {
      await submitRecharge(this.recharge)
      this.$message.success('充值申请已提交')
      this.recharge.remark = ''
      await this.load()
    },
    formatAmount(value) {
      return value == null ? '0.00' : Number(value).toFixed(2)
    },
    txnTypeLabel(value) {
      return txnTypeLabelDict(value)
    },
    inoutTypeLabel(value) {
      return inoutTypeLabelDict(value)
    },
    rechargeStatusLabel(value) {
      return rechargeStatusLabelDict(value)
    },
    rechargeTagType(value) {
      return rechargeStatusTagTypeDict(value)
    }
  }
}
</script>
