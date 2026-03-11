<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <h3 class="admin-card-title">充值审核</h3>
      <el-table :data="recharges" border>
        <el-table-column prop="rechargeNo" label="充值单号" width="200" />
        <el-table-column prop="userName" label="申请人" width="140" />
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <div v-if="row.status === 'PENDING'" class="action-wrap">
              <el-button link type="primary" @click="audit(row, 'APPROVE')">通过</el-button>
              <el-button link type="danger" @click="audit(row, 'REJECT')">驳回</el-button>
            </div>
            <span v-else class="handled-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="recharges.length === 0" description="暂无充值数据" class="admin-empty" />
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditRecharge, getRechargeOrders } from '../../../api/account'
import { rechargeStatusLabel, rechargeStatusTagType } from '../../../utils/dicts'

export default {
  data() {
    return {
      recharges: []
    }
  },
  async created() {
    await this.load()
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
      return rechargeStatusLabel(value)
    },
    statusTagType(value) {
      return rechargeStatusTagType(value)
    }
  }
}
</script>

<style scoped>
.action-wrap {
  display: flex;
  gap: 8px;
}

.handled-text {
  color: var(--muted);
}
</style>
