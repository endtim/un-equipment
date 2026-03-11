<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <div class="admin-toolbar">
        <div class="admin-card-title admin-card-title--tight">分类列表</div>
        <el-button type="primary" @click="openCreate">新增分类</el-button>
      </div>
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
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="categories.length === 0" description="暂无分类数据" class="admin-empty" />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑分类' : '新增分类'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-3">
          <el-form-item label="分类名称" prop="categoryName">
            <el-input v-model="form.categoryName" maxlength="100" />
          </el-form-item>
          <el-form-item label="分类编码" prop="categoryCode">
            <el-input v-model="form.categoryCode" maxlength="50" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" class="full-width">
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
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
      dialogVisible: false,
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
    async submit() {
      await this.$refs.formRef.validate()
      if (this.form.id) {
        await updateInstrumentCategory(this.form.id, this.form)
        ElMessage.success('分类已更新')
      } else {
        await createInstrumentCategory(this.form)
        ElMessage.success('分类已创建')
      }
      this.closeDialog()
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

<style scoped>
.admin-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
  justify-content: space-between;
}

.admin-card-title--tight {
  margin: 0;
}

.full-width {
  width: 100%;
}
</style>
