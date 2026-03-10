<template>
  <div class="admin-page">
    <div class="content-card admin-card">
      <h3 class="admin-card-title">{{ form.id ? '编辑部门' : '新增部门' }}</h3>
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
            <el-select v-model="form.status" style="width: 100%;">
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sortNo">
            <el-input-number v-model="form.sortNo" :min="0" style="width: 100%;" />
          </el-form-item>
        </div>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
        <div class="admin-form-actions">
          <el-button type="primary" @click="submit">保存</el-button>
          <el-button v-if="form.id" @click="reset">取消</el-button>
        </div>
      </el-form>
    </div>

    <div class="content-card admin-table-card">
      <h3 class="admin-card-title">部门列表</h3>
      <el-table :data="departments" border>
        <el-table-column prop="deptName" label="部门名称" min-width="180" />
        <el-table-column prop="deptCode" label="部门编码" width="150" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="状态" width="100">
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
      <el-empty v-if="departments.length === 0" description="暂无部门数据" class="admin-empty" />
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDepartment, deleteDepartment, getAdminDepartments, updateDepartment } from '../../../api/admin'

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
      this.departments = await getAdminDepartments()
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
        await updateDepartment(this.form.id, this.form)
        ElMessage.success('部门已更新')
      } else {
        await createDepartment(this.form)
        ElMessage.success('部门已创建')
      }
      this.reset()
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
