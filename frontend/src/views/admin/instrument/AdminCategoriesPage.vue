<template>
  <div class="admin-page">
    <div class="content-card admin-card">
      <h3 class="admin-card-title">{{ form.id ? '编辑分类' : '新增分类' }}</h3>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-3">
          <el-form-item label="分类名称" prop="categoryName">
            <el-input v-model="form.categoryName" maxlength="100" />
          </el-form-item>
          <el-form-item label="分类编码" prop="categoryCode">
            <el-input v-model="form.categoryCode" maxlength="50" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width: 100%;">
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>
        </div>
        <div class="admin-form-actions">
          <el-button type="primary" @click="submit">保存</el-button>
          <el-button v-if="form.id" @click="reset">取消</el-button>
        </div>
      </el-form>
    </div>

    <div class="content-card admin-table-card">
      <h3 class="admin-card-title">分类列表</h3>
      <el-table :data="categories" border>
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="categoryCode" label="分类编码" width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="edit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="categories.length === 0" description="暂无分类数据" class="admin-empty" />
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createInstrumentCategory,
  deleteInstrumentCategory,
  getAdminInstrumentCategories,
  updateInstrumentCategory
} from '../../../api/admin'

function defaultForm() {
  return { id: null, categoryName: '', categoryCode: '', status: 'ENABLED', sortNo: 0 }
}

export default {
  data() {
    return {
      categories: [],
      form: defaultForm(),
      rules: {
        categoryName: [
          { required: true, message: '请输入分类名称', trigger: 'blur' },
          { min: 2, max: 100, message: '分类名称长度为 2-100 个字符', trigger: 'blur' }
        ],
        categoryCode: [
          { required: true, message: '请输入分类编码', trigger: 'blur' },
          { min: 2, max: 50, message: '分类编码长度为 2-50 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      this.categories = await getAdminInstrumentCategories()
    },
    edit(row) {
      this.form = { ...row }
    },
    reset() {
      this.form = defaultForm()
      this.$refs.formRef && this.$refs.formRef.clearValidate()
    },
    async submit() {
      await this.$refs.formRef.validate()
      if (this.form.id) {
        await updateInstrumentCategory(this.form.id, this.form)
        ElMessage.success('分类已更新')
      } else {
        await createInstrumentCategory(this.form)
        ElMessage.success('分类已创建')
      }
      this.reset()
      await this.load()
    },
    async remove(row) {
      await ElMessageBox.confirm(`确认删除分类“${row.categoryName}”吗？`, '删除确认')
      await deleteInstrumentCategory(row.id)
      ElMessage.success('分类已删除')
      await this.load()
    }
  }
}
</script>
