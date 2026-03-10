<template>
  <div class="content-card" style="padding: 24px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <div class="section-title" style="margin: 0;">{{ pageTitle }}</div>
      <el-tag>{{ mode === 'settlement' ? '当前为结算占位页' : '充值审核流程' }}</el-tag>
    </div>

    <template v-if="mode === 'settlement'">
      <el-empty description="当前版本暂未接入独立结算列表，后续可基于结算记录接口扩展。" />
    </template>

    <template v-else>
      <el-table :data="recharges" border>
        <el-table-column prop="rechargeNo" label="充值单号" width="180" />
        <el-table-column prop="userName" label="申请人" width="120" />
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <div v-if="row.status === 'PENDING'" style="display: flex; gap: 8px;">
              <el-button link type="primary" @click="audit(row, 'APPROVE')">审核通过</el-button>
              <el-button link type="danger" @click="audit(row, 'REJECT')">驳回</el-button>
            </div>
            <span v-else style="color: var(--muted);">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditRecharge, getRechargeOrders } from '../../api/account'

export default {
  props: {
    mode: {
      type: String,
      default: 'recharge'
    }
  },
  data() {
    return {
      recharges: []
    }
  },
  computed: {
    pageTitle() {
      return this.mode === 'settlement' ? '结算管理' : '充值审核'
    }
  },
  async created() {
    if (this.mode === 'recharge') {
      await this.load()
    }
  },
  methods: {
    async load() {
      this.recharges = await getRechargeOrders()
    },
    async audit(row, action) {
      let comment = ''
      if (action === 'REJECT') {
        try {
          const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回充值申请', {
            confirmButtonText: '确认',
            cancelButtonText: '取消'
          })
          comment = value || ''
        } catch (error) {
          return
        }
      }
      await auditRecharge(row.id, { action, comment })
      ElMessage.success('充值审核已完成')
      await this.load()
    },
    statusLabel(value) {
      const mapping = {
        PENDING: '待审核',
        APPROVED: '已通过',
        REJECTED: '已驳回'
      }
      return mapping[value] || value || '-'
    },
    statusTagType(value) {
      const mapping = {
        PENDING: 'warning',
        APPROVED: 'success',
        REJECTED: 'danger'
      }
      return mapping[value] || 'info'
    }
  }
}
</script>
