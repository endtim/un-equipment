<template>
  <div class="admin-page">
    <admin-table-card title="操作日志">
      <template #toolbar>
      <div class="admin-toolbar">
        <el-select v-model="query.moduleName" clearable placeholder="模块" class="admin-filter--sm">
          <el-option label="系统管理" value="SYSTEM" />
          <el-option label="订单管理" value="ORDER" />
          <el-option label="财务管理" value="FINANCE" />
          <el-option label="仪器管理" value="INSTRUMENT" />
          <el-option label="内容管理" value="CONTENT" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="搜索操作名称、接口路径或资源ID"
          clearable
          class="admin-filter--xl"
        />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      </template>

      <el-table :data="logs" border>
        <el-table-column prop="moduleName" label="模块" width="120">
          <template #default="{ row }">{{
            row.moduleLabel || moduleLabel(row.moduleName)
          }}</template>
        </el-table-column>
        <el-table-column label="动作" width="180">
          <template #default="{ row }">{{
            row.actionLabel || actionLabel(row.actionName)
          }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="requestMethod" label="方法" width="90" />
        <el-table-column prop="requestUri" label="请求信息" min-width="260" show-overflow-tooltip />
        <el-table-column prop="requestIp" label="IP" width="140" />
        <el-table-column prop="bizId" label="资源ID" width="100" />
        <el-table-column prop="resultCode" label="结果码" width="90" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <status-tag
              :label="row.resultLabel || (row.resultCode === 200 ? '成功' : '失败')"
              :type="row.resultCode === 200 ? 'success' : 'danger'"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="操作时间"
          width="180"
          :formatter="formatDateTimeCell"
        />
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="pageSizeOptions"
          :total="total"
          @size-change="changePageSize"
          @current-change="changePage"
        />
      </div>
    </admin-table-card>
  </div>
</template>

<script>
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import { getOperationLogs } from '../../api/log'
import { formatDateTime } from '../../utils/datetime'

export default {
  components: {
    AdminTableCard,
    StatusTag
  },
  data() {
    return {
      logs: [],
      total: 0,
      pageSizeOptions: [10, 20, 50, 100],
      query: {
        moduleName: '',
        keyword: '',
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  async created() {
    this.restoreQuery()
    await this.load()
  },
  methods: {
    formatDateTimeCell(row, column, value) {
      return formatDateTime(value)
    },
    restoreQuery() {
      const query = this.$route.query || {}
      this.query.moduleName = query.moduleName || ''
      this.query.keyword = query.keyword || ''
      this.query.pageNum = Number(query.pageNum || 1)
      this.query.pageSize = Number(query.pageSize || 10)
    },
    syncQuery() {
      this.$router.replace({
        path: this.$route.path,
        query: {
          moduleName: this.query.moduleName || undefined,
          keyword: this.query.keyword || undefined,
          pageNum: this.query.pageNum > 1 ? String(this.query.pageNum) : undefined,
          pageSize: this.query.pageSize !== 10 ? String(this.query.pageSize) : undefined
        }
      })
    },
    async load() {
      const data = await getOperationLogs(this.query)
      this.logs = data.list || []
      this.total = data.total || 0
      this.syncQuery()
    },
    async search() {
      this.query.pageNum = 1
      await this.load()
    },
    async changePage(pageNum) {
      this.query.pageNum = pageNum
      await this.load()
    },
    async changePageSize(pageSize) {
      this.query.pageSize = pageSize
      this.query.pageNum = 1
      await this.load()
    },
    moduleLabel(value) {
      const mapping = {
        SYSTEM: '系统管理',
        ORDER: '订单管理',
        FINANCE: '财务管理',
        INSTRUMENT: '仪器管理',
        CONTENT: '内容管理'
      }
      return mapping[value] || '未知模块'
    },
    actionLabel(value) {
      const mapping = {
        AUDIT_EXTERNAL_USER: '审核校外用户',
        UPDATE_USER_STATUS: '更新用户状态',
        AUDIT_RECHARGE_FIRST_PASS: '充值初审通过',
        REQUEST_REFUND: '发起退款申请',
        HANDLE_RECONCILIATION_ANOMALY: '处理对账异常',
        REFUND_COMPENSATE_REPAIR: '退款补偿修复',
        REFUND_COMPENSATE_ROLLBACK: '退款补偿回滚'
      }
      return mapping[value] || '未知动作'
    }
  }
}
</script>
