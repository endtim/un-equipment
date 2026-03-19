<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <div class="admin-toolbar">
        <div class="admin-card-title admin-card-title--tight">部门列表</div>
        <el-button type="primary" @click="openCreate">新增部门</el-button>
      </div>

      <el-table :data="departments" border>
        <el-table-column prop="deptName" label="部门名称" min-width="180" />
        <el-table-column prop="deptCode" label="部门编码" width="150" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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
      <el-empty v-if="departments.length === 0" description="暂无部门数据" class="admin-empty" />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑部门' : '新增部门'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-3">
          <el-form-item label="部门名称" prop="deptName">
            <el-input v-model="form.deptName" maxlength="100" />
          </el-form-item>
          <el-form-item label="部门编码" prop="deptCode">
            <el-input v-model="form.deptCode" maxlength="50" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" maxlength="20" />
          </el-form-item>
        </div>
        <div class="grid-3">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" maxlength="100" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" class="full-width">
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sortNo">
            <el-input-number v-model="form.sortNo" :min="0" class="full-width" />
          </el-form-item>
        </div>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
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
  createDepartment,
  deleteDepartment,
  getAdminDepartmentsPage,
  updateDepartment
} from '../../../api/admin'

function defaultForm() {
  return {
    id: null,
    deptName: '',
    deptCode: '',
    phone: '',
    email: '',
    sortNo: 0,
    status: 'ENABLED',
    remark: ''
  }
}

export default {
  data() {
    return {
      departments: [],
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 10
      },
      dialogVisible: false,
      form: defaultForm(),
      rules: {
        deptName: [
          { required: true, message: '请输入部门名称', trigger: 'blur' },
          { min: 2, max: 100, message: '部门名称长度为 2-100 个字符', trigger: 'blur' }
        ],
        deptCode: [
          { required: true, message: '请输入部门编码', trigger: 'blur' },
          { min: 2, max: 50, message: '部门编码长度为 2-50 个字符', trigger: 'blur' }
        ],
        email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
      }
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      const page = await getAdminDepartmentsPage(this.query)
      this.departments = page.list || []
      this.total = page.total || 0
    },
    async onSizeChange() {
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
        if (this.$refs.formRef) {
          this.$refs.formRef.clearValidate()
        }
      })
    },
    async submit() {
      await this.$refs.formRef.validate()
      if (this.form.id) {
        await updateDepartment(this.form.id, this.form)
        ElMessage.success('部门已更新')
      } else {
        await createDepartment(this.form)
        ElMessage.success('部门已创建')
      }
      this.closeDialog()
      await this.load()
    },
    async remove(row) {
      await ElMessageBox.confirm(`确认删除部门“${row.deptName}”吗？`, '删除确认')
      await deleteDepartment(row.id)
      ElMessage.success('部门已删除')
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

.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
