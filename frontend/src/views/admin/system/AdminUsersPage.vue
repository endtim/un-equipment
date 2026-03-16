<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <div class="admin-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索用户名或姓名" clearable />
        <el-button type="primary" @click="searchUsers">查询</el-button>
        <el-button type="primary" plain @click="openCreateUser">新增用户</el-button>
      </div>
      <el-table :data="users" border>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="姓名" width="130" />
        <el-table-column prop="departmentName" label="所属部门" width="170" />
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            {{ userRoleLabel(row.primaryRoleCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="openEditUser(row)">编辑</el-button>
            <el-button link type="danger" @click="removeUser(row)">删除</el-button>
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
      v-model="userDialogVisible"
      :title="userForm.id ? '编辑用户' : '新增用户'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-position="top">
        <div class="grid-3">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userForm.username" :disabled="Boolean(userForm.id)" maxlength="50" />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="userForm.realName" maxlength="50" />
          </el-form-item>
          <el-form-item label="所属部门" prop="departmentId">
            <el-select v-model="userForm.departmentId" class="full-width">
              <el-option v-for="item in departments" :key="item.id" :label="item.deptName" :value="item.id" />
            </el-select>
          </el-form-item>
        </div>
        <div class="grid-3">
          <el-form-item label="角色" prop="primaryRoleCode">
            <el-select v-model="userForm.primaryRoleCode" class="full-width">
              <el-option v-for="item in roles" :key="item.id" :label="item.roleName" :value="item.roleCode" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userForm.phone" maxlength="20" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userForm.email" maxlength="100" />
          </el-form-item>
        </div>
        <div class="grid-3" v-if="!userForm.id">
          <el-form-item label="登录密码" prop="password">
            <el-input v-model="userForm.password" type="password" show-password maxlength="100" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="closeUserDialog">取消</el-button>
        <el-button type="primary" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createUser,
  deleteUser,
  getAdminDepartments,
  getAdminRoles,
  getAdminUsersPage,
  updateUser
} from '../../../api/admin'
import { userRoleLabel as userRoleLabelDict } from '../../../utils/dicts'

function defaultUserForm() {
  return {
    id: null,
    username: '',
    realName: '',
    departmentId: null,
    primaryRoleCode: 'INTERNAL_USER',
    phone: '',
    email: '',
    password: ''
  }
}

export default {
  data() {
    return {
      users: [],
      roles: [],
      departments: [],
      total: 0,
      pageSizeOptions: [10, 20, 50, 100],
      query: {
        keyword: '',
        pageNum: 1,
        pageSize: 10
      },
      userDialogVisible: false,
      userForm: defaultUserForm(),
      userRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 4, max: 50, message: '用户名长度为 4-50 个字符', trigger: 'blur' }
        ],
        realName: [
          { required: true, message: '请输入姓名', trigger: 'blur' },
          { min: 2, max: 50, message: '姓名长度为 2-50 个字符', trigger: 'blur' }
        ],
        departmentId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
        primaryRoleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
        email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
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
      this.query.pageNum = Number(query.pageNum || 1)
      this.query.pageSize = Number(query.pageSize || 10)
    },
    syncQuery() {
      this.$router.replace({
        path: this.$route.path,
        query: {
          keyword: this.query.keyword || undefined,
          pageNum: this.query.pageNum > 1 ? String(this.query.pageNum) : undefined,
          pageSize: this.query.pageSize !== 10 ? String(this.query.pageSize) : undefined
        }
      })
    },
    async load() {
      const [departments, roles, usersPage] = await Promise.all([
        getAdminDepartments(),
        getAdminRoles(),
        getAdminUsersPage(this.query)
      ])
      this.departments = departments || []
      this.roles = roles || []
      this.users = usersPage.list || []
      this.total = usersPage.total || 0
      this.syncQuery()
    },
    async searchUsers() {
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
    openCreateUser() {
      this.userForm = defaultUserForm()
      if (this.departments.length) {
        this.userForm.departmentId = this.departments[0].id
      }
      this.userDialogVisible = true
    },
    openEditUser(row) {
      this.userForm = {
        id: row.id,
        username: row.username,
        realName: row.realName,
        departmentId: row.departmentId,
        primaryRoleCode: row.primaryRoleCode || 'INTERNAL_USER',
        phone: row.phone || '',
        email: row.email || '',
        password: ''
      }
      this.userDialogVisible = true
    },
    closeUserDialog() {
      this.userDialogVisible = false
      this.$nextTick(() => {
        this.$refs.userFormRef && this.$refs.userFormRef.clearValidate()
      })
    },
    async submitUser() {
      await this.$refs.userFormRef.validate()
      if (!this.userForm.id && !this.userForm.password) {
        ElMessage.error('新增用户必须设置登录密码')
        return
      }
      if (this.userForm.password && this.userForm.password.length < 6) {
        ElMessage.error('密码长度至少 6 位')
        return
      }
      const payload = {
        ...this.userForm,
        status: 'ENABLED'
      }
      if (this.userForm.id) {
        if (!payload.password) {
          delete payload.password
        }
        await updateUser(this.userForm.id, payload)
        ElMessage.success('用户已更新')
      } else {
        await createUser(payload)
        ElMessage.success('用户已创建')
      }
      this.closeUserDialog()
      await this.load()
    },
    async removeUser(row) {
      await ElMessageBox.confirm(`确认删除用户“${row.username}”吗？`, '删除确认')
      await deleteUser(row.id)
      ElMessage.success('用户已删除')
      await this.load()
    },
    userRoleLabel(value) {
      return userRoleLabelDict(value)
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

.full-width {
  width: 100%;
}
</style>
