<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <h3 class="admin-card-title">充值审核</h3>

      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="充值单号/申请人/备注"
          style="width: 240px"
          @keyup.enter="onQueryChange"
        />
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 150px" @change="onQueryChange">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="PASS" />
          <el-option label="已驳回" value="REJECT" />
        </el-select>
        <el-input-number v-model="query.minAmount" :min="0" :precision="2" :controls="false" placeholder="最小金额" style="width: 120px" />
        <el-input-number v-model="query.maxAmount" :min="0" :precision="2" :controls="false" placeholder="最大金额" style="width: 120px" />
        <el-date-picker
          v-model="query.timeRange"
          type="datetimerange"
          start-placeholder="申请开始时间"
          end-placeholder="申请结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 340px"
          @change="onQueryChange"
        />
        <el-button type="primary" @click="onQueryChange">查询</el-button>
        <el-button type="success" plain @click="exportCsv">导出CSV</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="recharges" border>
        <el-table-column prop="rechargeNo" label="充值单号" width="200" />
        <el-table-column prop="userName" label="申请人" width="140" />
        <el-table-column prop="auditUserName" label="审核人" width="120" />
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column prop="createTime" label="申请时间" width="180" />
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
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditRecharge, exportRechargeOrders, getRechargeOrdersPage } from '../../../api/account'
import { rechargeStatusLabel, rechargeStatusTagType } from '../../../utils/dicts'

export default {
  data() {
    return {
      recharges: [],
      total: 0,
      query: {
        keyword: '',
        status: '',
        minAmount: null,
        maxAmount: null,
        timeRange: [],
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async executeSafely(action) {
      try {
        await action()
      } catch (error) {
        // 请求层统一提示
      }
    },
    buildParams() {
      const [startTime, endTime] = this.query.timeRange || []
      return {
        keyword: this.query.keyword || undefined,
        status: this.query.status || undefined,
        minAmount: this.query.minAmount == null ? undefined : this.query.minAmount,
        maxAmount: this.query.maxAmount == null ? undefined : this.query.maxAmount,
        startTime: startTime || undefined,
        endTime: endTime || undefined,
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize
      }
    },
    async load() {
      await this.executeSafely(async () => {
        const page = await getRechargeOrdersPage(this.buildParams())
        this.recharges = page.list || []
        this.total = page.total || 0
      })
    },
    async onQueryChange() {
      this.query.pageNum = 1
      await this.load()
    },
    async onSizeChange() {
      this.query.pageNum = 1
      await this.load()
    },
    async resetQuery() {
      this.query.keyword = ''
      this.query.status = ''
      this.query.minAmount = null
      this.query.maxAmount = null
      this.query.timeRange = []
      this.query.pageNum = 1
      await this.load()
    },
    async exportCsv() {
      await this.executeSafely(async () => {
        const params = this.buildParams()
        delete params.pageNum
        delete params.pageSize
        const blob = await exportRechargeOrders(params)
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = 'recharges.csv'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        ElMessage.success('CSV导出成功')
      })
    },
    async audit(row, action) {
      await this.executeSafely(async () => {
        let comment = ''
        if (action === 'REJECT') {
          const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回充值申请', {
            confirmButtonText: '确认',
            cancelButtonText: '取消'
          })
          comment = value || ''
        }
        await auditRecharge(row.id, { action, comment })
        ElMessage.success('充值审核已完成')
        await this.load()
      })
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
.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.action-wrap {
  display: flex;
  gap: 8px;
}

.handled-text {
  color: var(--muted);
}

.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
