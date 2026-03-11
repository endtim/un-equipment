<template>
  <div class="account-page">
    <div class="grid-3" style="margin-bottom: 18px;">
      <div v-for="card in cards" :key="card.label" class="content-card" style="padding: 20px;">
        <div style="color: var(--muted);">{{ card.label }}</div>
        <div style="font-size: 32px; font-weight: 700; margin-top: 8px; color: var(--primary);">{{ card.value }}</div>
      </div>
    </div>

    <div class="content-card" style="padding: 22px; margin-bottom: 18px;">
      <div class="section-title" style="font-size: 22px;">充值申请</div>
      <el-form :model="recharge" label-width="90px" style="max-width: 720px;">
        <el-form-item label="充值金额">
          <el-input-number v-model="recharge.amount" :min="1" style="width: 240px;" />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input v-model="recharge.remark" type="textarea" :rows="3" placeholder="可填写转账说明、用途备注等信息" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">提交充值申请</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="grid-3">
      <div class="content-card" style="padding: 24px; grid-column: span 2;">
        <div class="section-title" style="font-size: 22px;">交易流水</div>
        <el-table :data="account.transactions || []" border>
          <el-table-column prop="txnType" label="交易类型" width="120">
            <template #default="{ row }">{{ txnTypeLabel(row.txnType) }}</template>
          </el-table-column>
          <el-table-column prop="inoutType" label="收支方向" width="120">
            <template #default="{ row }">{{ inoutTypeLabel(row.inoutType) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120" />
          <el-table-column prop="remark" label="备注" />
          <el-table-column prop="createTime" label="发生时间" width="180" />
        </el-table>
      </div>

      <div class="content-card" style="padding: 24px;">
        <div class="section-title" style="font-size: 22px;">充值记录</div>
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

export default {
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
    cards() {
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
