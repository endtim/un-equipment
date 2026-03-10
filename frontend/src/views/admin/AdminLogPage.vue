<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <h3 class="admin-card-title">操作日志</h3>
      <div class="admin-toolbar" style="margin-bottom: 16px;">
        <el-select v-model="query.moduleName" clearable placeholder="模块">
          <el-option label="系统管理" value="SYSTEM" />
          <el-option label="订单管理" value="ORDER" />
          <el-option label="财务管理" value="FINANCE" />
          <el-option label="仪器管理" value="INSTRUMENT" />
          <el-option label="内容管理" value="CONTENT" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索操作名称或详情" clearable />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table :data="logs" border>
        <el-table-column prop="moduleName" label="模块" width="120">
          <template #default="{ row }">{{ moduleLabel(row.moduleName) }}</template>
        </el-table-column>
        <el-table-column prop="actionName" label="操作名称" width="180" />
        <el-table-column prop="requestUri" label="操作详情" min-width="220" />
        <el-table-column prop="resultCode" label="结果码" width="90" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          @current-change="changePage"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { getOperationLogs } from '../../api/log'

export default {
  data() {
    return {
      logs: [],
      total: 0,
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
          pageNum: this.query.pageNum > 1 ? String(this.query.pageNum) : undefined
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
    moduleLabel(value) {
      const mapping = {
        SYSTEM: '系统管理',
        ORDER: '订单管理',
        FINANCE: '财务管理',
        INSTRUMENT: '仪器管理',
        CONTENT: '内容管理'
      }
      return mapping[value] || value || '-'
    }
  }
}
</script>
