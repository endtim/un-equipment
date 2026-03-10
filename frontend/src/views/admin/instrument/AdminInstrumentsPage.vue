<template>
  <div class="admin-page">
    <div class="content-card admin-card">
      <h3 class="admin-card-title">{{ form.id ? '编辑仪器' : '新增仪器' }}</h3>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-3">
          <el-form-item label="仪器名称" prop="name">
            <el-input v-model="form.name" maxlength="200" />
          </el-form-item>
          <el-form-item label="仪器编号" prop="code">
            <el-input v-model="form.code" maxlength="50" />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" style="width: 100%;">
              <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" />
            </el-select>
          </el-form-item>
        </div>
        <div class="grid-3">
          <el-form-item label="位置" prop="location">
            <el-input v-model="form.location" maxlength="200" />
          </el-form-item>
          <el-form-item label="校内价格(元)" prop="priceInternal">
            <el-input-number v-model="form.priceInternal" :min="0" :precision="2" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="校外价格(元)" prop="priceExternal">
            <el-input-number v-model="form.priceExternal" :min="0" :precision="2" style="width: 100%;" />
          </el-form-item>
        </div>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <div class="admin-form-actions">
          <el-button type="primary" @click="submit">保存</el-button>
          <el-button v-if="form.id" @click="reset">取消</el-button>
        </div>
      </el-form>
    </div>

    <div class="content-card admin-table-card">
      <div class="admin-toolbar" style="margin-bottom: 14px;">
        <el-input v-model="query.keyword" placeholder="搜索仪器名称或编号" clearable />
        <el-select v-model="query.categoryId" clearable placeholder="分类">
          <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态">
          <el-option label="正常" value="NORMAL" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table :data="instruments" border>
        <el-table-column prop="instrumentName" label="仪器名称" min-width="180" />
        <el-table-column prop="instrumentNo" label="仪器编号" width="150" />
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column prop="location" label="位置" min-width="160" />
        <el-table-column label="校内价" width="110">
          <template #default="{ row }">{{ formatPrice(row.priceInternal || row.machinePricePerHour) }}</template>
        </el-table-column>
        <el-table-column label="校外价" width="110">
          <template #default="{ row }">{{ formatPrice(row.priceExternal || row.samplePricePerItem) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'">{{ row.status === 'NORMAL' ? '正常' : row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="edit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createInstrument,
  deleteInstrument,
  getAdminInstrumentCategories,
  getAdminInstruments,
  updateInstrument
} from '../../../api/admin'

function defaultForm() {
  return {
    id: null,
    name: '',
    code: '',
    categoryId: null,
    location: '',
    description: '',
    priceInternal: 0,
    priceExternal: 0,
    status: 'NORMAL'
  }
}

export default {
  data() {
    return {
      categories: [],
      instruments: [],
      total: 0,
      query: {
        keyword: '',
        categoryId: null,
        status: '',
        pageNum: 1,
        pageSize: 10
      },
      form: defaultForm(),
      rules: {
        name: [
          { required: true, message: '请输入仪器名称', trigger: 'blur' },
          { min: 2, max: 200, message: '仪器名称长度为 2-200 个字符', trigger: 'blur' }
        ],
        categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
        priceInternal: [{ required: true, message: '请输入校内价格', trigger: 'change' }],
        priceExternal: [{ required: true, message: '请输入校外价格', trigger: 'change' }]
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
      this.query.categoryId = query.categoryId ? Number(query.categoryId) : null
      this.query.status = query.status || ''
      this.query.pageNum = Number(query.pageNum || 1)
      this.query.pageSize = Number(query.pageSize || 10)
    },
    syncQuery() {
      this.$router.replace({
        path: this.$route.path,
        query: {
          keyword: this.query.keyword || undefined,
          categoryId: this.query.categoryId || undefined,
          status: this.query.status || undefined,
          pageNum: this.query.pageNum > 1 ? String(this.query.pageNum) : undefined,
          pageSize: this.query.pageSize !== 10 ? String(this.query.pageSize) : undefined
        }
      })
    },
    async load() {
      const [categories, page] = await Promise.all([
        getAdminInstrumentCategories(),
        getAdminInstruments(this.query)
      ])
      this.categories = categories
      this.instruments = page.list || []
      this.total = page.total || 0
      if (!this.form.categoryId && categories.length) {
        this.form.categoryId = categories[0].id
      }
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
    edit(row) {
      this.form = {
        id: row.id,
        name: row.instrumentName,
        code: row.instrumentNo,
        categoryId: row.categoryId,
        location: row.location || '',
        description: row.intro || row.description || '',
        priceInternal: Number(row.priceInternal || row.machinePricePerHour || 0),
        priceExternal: Number(row.priceExternal || row.samplePricePerItem || 0),
        status: row.status || 'NORMAL'
      }
    },
    reset() {
      this.form = defaultForm()
      if (this.categories.length) {
        this.form.categoryId = this.categories[0].id
      }
      this.$refs.formRef && this.$refs.formRef.clearValidate()
    },
    async submit() {
      await this.$refs.formRef.validate()
      const payload = {
        ...this.form,
        priceInternal: Number(this.form.priceInternal || 0),
        priceExternal: Number(this.form.priceExternal || 0),
        machinePricePerHour: Number(this.form.priceInternal || 0),
        samplePricePerItem: Number(this.form.priceExternal || 0)
      }
      if (this.form.id) {
        await updateInstrument(this.form.id, payload)
        ElMessage.success('仪器已更新')
      } else {
        await createInstrument(payload)
        ElMessage.success('仪器已创建')
      }
      this.reset()
      await this.load()
    },
    async remove(row) {
      await ElMessageBox.confirm(`确认删除仪器“${row.instrumentName}”吗？`, '删除确认')
      await deleteInstrument(row.id)
      ElMessage.success('仪器已删除')
      await this.load()
    },
    formatPrice(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>
