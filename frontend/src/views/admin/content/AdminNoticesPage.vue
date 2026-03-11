<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <div class="admin-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索公告标题" clearable />
        <el-select v-model="query.publishStatus" clearable placeholder="发布状态">
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="草稿" value="DRAFT" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button type="primary" plain @click="openCreate">发布公告</el-button>
      </div>
      <el-table :data="notices" border>
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="publishTagType(row.publishStatus)">{{ publishStatusLabel(row.publishStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeNotice(row)">删除</el-button>
          </template>
        </el-table-column>
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
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑公告' : '发布公告'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="正文内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="submitNotice">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { createNotice, deleteNotice, getAdminNoticePage, updateNotice } from '../../../api/admin'

function defaultForm() {
  return { id: null, title: '', summary: '', content: '', publishStatus: 'PUBLISHED' }
}

export default {
  data() {
    return {
      notices: [],
      total: 0,
      pageSizeOptions: [10, 20, 50, 100],
      dialogVisible: false,
      query: {
        keyword: '',
        publishStatus: '',
        pageNum: 1,
        pageSize: 10
      },
      form: defaultForm(),
      rules: {
        title: [
          { required: true, message: '请输入公告标题', trigger: 'blur' },
          { min: 2, max: 100, message: '标题长度为 2-100 个字符', trigger: 'blur' }
        ],
        summary: [{ max: 255, message: '摘要最多 255 个字符', trigger: 'blur' }],
        content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
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
      this.query.keyword = query.keyword || ''
      this.query.publishStatus = query.publishStatus || ''
      this.query.pageNum = Number(query.pageNum || 1)
      this.query.pageSize = Number(query.pageSize || 10)
    },
    syncQuery() {
      this.$router.replace({
        path: this.$route.path,
        query: {
          keyword: this.query.keyword || undefined,
          publishStatus: this.query.publishStatus || undefined,
          pageNum: this.query.pageNum > 1 ? String(this.query.pageNum) : undefined,
          pageSize: this.query.pageSize !== 10 ? String(this.query.pageSize) : undefined
        }
      })
    },
    async load() {
      const page = await getAdminNoticePage(this.query)
      this.notices = page.list || []
      this.total = page.total || 0
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
    openCreate() {
      this.form = defaultForm()
      this.dialogVisible = true
    },
    openEdit(row) {
      this.form = { ...row }
      this.dialogVisible = true
    },
    closeDialog() {
      this.dialogVisible = false
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate()
      })
    },
    async submitNotice() {
      await this.$refs.formRef.validate()
      if (this.form.id) {
        await updateNotice(this.form.id, this.form)
        ElMessage.success('公告已更新')
      } else {
        await createNotice(this.form)
        ElMessage.success('公告已发布')
      }
      this.closeDialog()
      await this.load()
    },
    async removeNotice(row) {
      await ElMessageBox.confirm(`确认删除公告“${row.title}”吗？`, '删除确认')
      await deleteNotice(row.id)
      ElMessage.success('公告已删除')
      await this.load()
    },
    publishStatusLabel(value) {
      const mapping = { PUBLISHED: '已发布', DRAFT: '草稿' }
      return mapping[value] || value || '-'
    },
    publishTagType(value) {
      const mapping = { PUBLISHED: 'success', DRAFT: 'info' }
      return mapping[value] || 'info'
    }
  }
}
</script>

<style scoped>
.admin-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
