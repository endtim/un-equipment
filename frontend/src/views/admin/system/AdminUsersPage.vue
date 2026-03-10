<template>
  <div class="admin-page">
    <div class="grid-3">
      <div class="content-card admin-card" style="grid-column: span 2;">
        <h3 class="admin-card-title">{{ userForm.id ? '编辑用户' : '新增用户' }}</h3>
        <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-position="top">
          <div class="grid-3">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="userForm.username" :disabled="Boolean(userForm.id)" maxlength="50" />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="userForm.realName" maxlength="50" />
            </el-form-item>
            <el-form-item label="所属部门" prop="departmentId">
              <el-select v-model="userForm.departmentId" style="width: 100%;">
                <el-option v-for="item in departments" :key="item.id" :label="item.deptName" :value="item.id" />
              </el-select>
            </el-form-item>
          </div>
          <div class="grid-3">
            <el-form-item label="角色" prop="primaryRoleCode">
              <el-select v-model="userForm.primaryRoleCode" style="width: 100%;">
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
          <div class="admin-form-actions">
            <el-button type="primary" @click="submitUser">保存</el-button>
            <el-button v-if="userForm.id" @click="resetUser">取消</el-button>
          </div>
        </el-form>
      </div>

      <div class="content-card admin-card">
        <h3 class="admin-card-title">{{ roleForm.id ? '编辑角色' : '新增角色' }}</h3>
        <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-position="top">
          <el-form-item label="角色名称" prop="roleName">
            <el-input v-model="roleForm.roleName" maxlength="50" />
          </el-form-item>
          <el-form-item label="角色编码" prop="roleCode">
            <el-input v-model="roleForm.roleCode" maxlength="50" />
          </el-form-item>
          <div class="admin-form-actions">
            <el-button type="primary" @click="submitRole">保存</el-button>
            <el-button v-if="roleForm.id" @click="resetRole">取消</el-button>
          </div>
        </el-form>
        <el-divider />
        <el-table :data="roles" border size="small">
          <el-table-column prop="roleName" label="角色名称" />
          <el-table-column prop="roleCode" label="角色编码" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link @click="editRole(row)">编辑</el-button>
              <el-button link type="danger" @click="removeRole(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="content-card admin-table-card">
      <div class="admin-toolbar" style="margin-bottom: 14px;">
        <el-input v-model="query.keyword" placeholder="搜索用户名或姓名" clearable />
        <el-button type="primary" @click="searchUsers">查询</el-button>
      </div>
      <el-table :data="users" border>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="姓名" width="130" />
        <el-table-column prop="departmentName" label="所属部门" width="170" />
        <el-table-column prop="primaryRoleCode" label="角色" width="150" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="editUser(row)">编辑</el-button>
            <el-button link type="danger" @click="removeUser(row)">删除</el-button>
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
  createRole,
  createUser,
  deleteRole,
  deleteUser,
  getAdminDepartments,
  getAdminRoles,
  getAdminUsersPage,
  updateRole,
  updateUser
} from '../../../api/admin'

function defaultUserForm() {
  return {
    id: null,
    username: '',
    realName: '',
    departmentId: null,
    primaryRoleCode: 'INTERNAL_USER',
    phone: '',
    email: ''
  }
}

function defaultRoleForm() {
  return { id: null, roleName: '', roleCode: '' }
}

export default {
  data() {
    return {
      users: [],
      roles: [],
      departments: [],
      total: 0,
      query: {
        keyword: '',
        pageNum: 1,
        pageSize: 10
      },
      userForm: defaultUserForm(),
      roleForm: defaultRoleForm(),
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
      },
      roleRules: {
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
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
      this.departments = departments
      this.roles = roles
      this.users = usersPage.list || []
      this.total = usersPage.total || 0
      if (!this.userForm.departmentId && departments.length) {
        this.userForm.departmentId = departments[0].id
      }
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
    editUser(row) {
      this.userForm = {
        id: row.id,
        username: row.username,
        realName: row.realName,
        departmentId: row.departmentId,
        primaryRoleCode: row.primaryRoleCode || 'INTERNAL_USER',
        phone: row.phone || '',
        email: row.email || ''
      }
    },
    resetUser() {
      this.userForm = defaultUserForm()
      if (this.departments.length) {
        this.userForm.departmentId = this.departments[0].id
      }
      this.$refs.userFormRef && this.$refs.userFormRef.clearValidate()
    },
    async submitUser() {
      await this.$refs.userFormRef.validate()
      const payload = { ...this.userForm, password: this.userForm.id ? undefined : '123456', status: 'ENABLED' }
      if (this.userForm.id) {
        await updateUser(this.userForm.id, payload)
        ElMessage.success('用户已更新')
      } else {
        await createUser(payload)
        ElMessage.success('用户已创建，初始密码为 123456')
      }
      this.resetUser()
      await this.load()
    },
    async removeUser(row) {
      await ElMessageBox.confirm(`确认删除用户“${row.username}”吗？`, '删除确认')
      await deleteUser(row.id)
      ElMessage.success('用户已删除')
      await this.load()
    },
    editRole(row) {
      this.roleForm = { ...row }
    },
    resetRole() {
      this.roleForm = defaultRoleForm()
      this.$refs.roleFormRef && this.$refs.roleFormRef.clearValidate()
    },
    async submitRole() {
      await this.$refs.roleFormRef.validate()
      const payload = { ...this.roleForm, status: 'ENABLED' }
      if (payload.id) {
        await updateRole(payload.id, payload)
        ElMessage.success('角色已更新')
      } else {
        await createRole(payload)
        ElMessage.success('角色已创建')
      }
      this.resetRole()
      await this.load()
    },
    async removeRole(row) {
      await ElMessageBox.confirm(`确认删除角色“${row.roleName}”吗？`, '删除确认')
      await deleteRole(row.id)
      ElMessage.success('角色已删除')
      await this.load()
    }
  }
}
</script>
