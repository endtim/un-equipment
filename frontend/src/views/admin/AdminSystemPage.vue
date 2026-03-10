<template>
  <div>
    <div class="grid-3" style="margin-bottom: 20px;">
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">{{ departmentForm.id ? '编辑部门' : '新增部门' }}</div>
        <el-form :model="departmentForm" label-position="top">
          <el-form-item label="部门名称">
            <el-input v-model="departmentForm.deptName" />
          </el-form-item>
          <el-form-item label="部门编码">
            <el-input v-model="departmentForm.deptCode" />
          </el-form-item>
          <div style="display: flex; gap: 8px;">
            <el-button type="primary" @click="submitDepartment">保存</el-button>
            <el-button v-if="departmentForm.id" @click="resetDepartment">取消</el-button>
          </div>
        </el-form>
      </div>
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">{{ roleForm.id ? '编辑角色' : '新增角色' }}</div>
        <el-form :model="roleForm" label-position="top">
          <el-form-item label="角色名称">
            <el-input v-model="roleForm.roleName" />
          </el-form-item>
          <el-form-item label="角色编码">
            <el-input v-model="roleForm.roleCode" />
          </el-form-item>
          <div style="display: flex; gap: 8px;">
            <el-button type="primary" @click="submitRole">保存</el-button>
            <el-button v-if="roleForm.id" @click="resetRole">取消</el-button>
          </div>
        </el-form>
      </div>
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">{{ userForm.id ? '编辑用户' : '新增用户' }}</div>
        <el-form :model="userForm" label-position="top">
          <el-form-item label="用户名">
            <el-input v-model="userForm.username" :disabled="Boolean(userForm.id)" />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="userForm.realName" />
          </el-form-item>
          <el-form-item label="所属部门">
            <el-select v-model="userForm.departmentId" style="width: 100%;">
              <el-option v-for="item in departments" :key="item.id" :label="item.deptName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="主角色">
            <el-select v-model="userForm.primaryRoleCode" style="width: 100%;">
              <el-option v-for="item in roles" :key="item.id" :label="item.roleName" :value="item.roleCode" />
            </el-select>
          </el-form-item>
          <div style="display: flex; gap: 8px;">
            <el-button type="primary" @click="submitUser">保存</el-button>
            <el-button v-if="userForm.id" @click="resetUser">取消</el-button>
          </div>
        </el-form>
      </div>
    </div>

    <div class="grid-3" style="margin-bottom: 20px;">
      <div class="content-card" style="padding: 20px;" v-show="showDepartments">
        <div class="section-title" style="font-size: 18px;">部门列表</div>
        <el-table :data="departments" border size="small">
          <el-table-column prop="deptName" label="部门名称" />
          <el-table-column prop="deptCode" label="部门编码" />
          <el-table-column prop="status" label="状态" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link @click="editDepartment(row)">编辑</el-button>
              <el-button link type="danger" @click="removeDepartment(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="content-card" style="padding: 20px;" v-show="showRoles">
        <div class="section-title" style="font-size: 18px;">角色列表</div>
        <el-table :data="roles" border size="small">
          <el-table-column prop="roleName" label="角色名称" />
          <el-table-column prop="roleCode" label="角色编码" />
          <el-table-column prop="status" label="状态" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link @click="editRole(row)">编辑</el-button>
              <el-button link type="danger" @click="removeRole(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="content-card" style="padding: 20px;" :style="showUsersOnly ? 'grid-column: span 3;' : ''">
        <div style="display: flex; gap: 12px; margin-bottom: 16px;">
          <el-input v-model="userQuery.keyword" placeholder="搜索用户名或姓名" />
          <el-button type="primary" @click="searchUsers">查询</el-button>
        </div>
        <div class="section-title" style="font-size: 18px;">用户列表</div>
        <el-table :data="users" border size="small">
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="realName" label="真实姓名" />
          <el-table-column prop="departmentName" label="所属部门" />
          <el-table-column prop="primaryRoleCode" label="主角色" />
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
            :current-page="userQuery.pageNum"
            :page-size="userQuery.pageSize"
            :total="userTotal"
            @current-change="changeUserPage"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createDepartment,
  createRole,
  createUser,
  deleteDepartment,
  deleteRole,
  deleteUser,
  getAdminDepartments,
  getAdminRoles,
  getAdminUsersPage,
  updateDepartment,
  updateRole,
  updateUser
} from '../../api/admin'

function defaultDepartmentForm() {
  return { id: null, deptName: '', deptCode: '' }
}

function defaultRoleForm() {
  return { id: null, roleName: '', roleCode: '' }
}

function defaultUserForm() {
  return { id: null, username: '', realName: '', departmentId: null, primaryRoleCode: 'INTERNAL_USER' }
}

export default {
  props: {
    focus: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      departments: [],
      roles: [],
      users: [],
      userTotal: 0,
      userQuery: {
        keyword: '',
        pageNum: 1,
        pageSize: 8
      },
      departmentForm: defaultDepartmentForm(),
      roleForm: defaultRoleForm(),
      userForm: defaultUserForm()
    }
  },
  computed: {
    showUsersOnly() {
      return this.focus === 'users'
    },
    showDepartments() {
      return !this.showUsersOnly
    },
    showRoles() {
      return !this.showUsersOnly
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      const [departments, roles, users] = await Promise.all([
        getAdminDepartments(),
        getAdminRoles(),
        getAdminUsersPage(this.userQuery)
      ])
      this.departments = departments
      this.roles = roles
      this.users = users.list
      this.userTotal = users.total
      if (!this.userForm.departmentId && departments.length) {
        this.userForm.departmentId = departments[0].id
      }
    },
    async searchUsers() {
      this.userQuery.pageNum = 1
      await this.load()
    },
    async changeUserPage(pageNum) {
      this.userQuery.pageNum = pageNum
      await this.load()
    },
    editDepartment(row) {
      this.departmentForm = { ...row }
    },
    resetDepartment() {
      this.departmentForm = defaultDepartmentForm()
    },
    async submitDepartment() {
      if (this.departmentForm.id) {
        await updateDepartment(this.departmentForm.id, this.departmentForm)
        ElMessage.success('部门已更新')
      } else {
        await createDepartment(this.departmentForm)
        ElMessage.success('部门已创建')
      }
      this.resetDepartment()
      await this.load()
    },
    async removeDepartment(row) {
      await ElMessageBox.confirm(`确认删除部门“${row.deptName}”吗？`, '删除确认')
      await deleteDepartment(row.id)
      ElMessage.success('部门已删除')
      await this.load()
    },
    editRole(row) {
      this.roleForm = { ...row }
    },
    resetRole() {
      this.roleForm = defaultRoleForm()
    },
    async submitRole() {
      if (this.roleForm.id) {
        await updateRole(this.roleForm.id, this.roleForm)
        ElMessage.success('角色已更新')
      } else {
        await createRole(this.roleForm)
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
    },
    editUser(row) {
      this.userForm = {
        id: row.id,
        username: row.username,
        realName: row.realName,
        departmentId: row.departmentId,
        primaryRoleCode: row.primaryRoleCode || 'INTERNAL_USER'
      }
    },
    resetUser() {
      this.userForm = defaultUserForm()
      if (this.departments.length) {
        this.userForm.departmentId = this.departments[0].id
      }
    },
    async submitUser() {
      const payload = { ...this.userForm, password: '123456', status: 'ENABLED' }
      if (this.userForm.id) {
        await updateUser(this.userForm.id, payload)
        ElMessage.success('用户已更新')
      } else {
        await createUser(payload)
        ElMessage.success('用户已创建')
      }
      this.resetUser()
      await this.load()
    },
    async removeUser(row) {
      await ElMessageBox.confirm(`确认删除用户“${row.username}”吗？`, '删除确认')
      await deleteUser(row.id)
      ElMessage.success('用户已删除')
      await this.load()
    }
  }
}
</script>
