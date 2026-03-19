<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <div class="admin-summary-grid" style="margin-bottom: 14px">
        <div v-for="card in summaryCards" :key="card.label" class="admin-summary-card">
          <div class="admin-summary-label">{{ card.label }}</div>
          <div class="admin-summary-value">{{ card.value }}</div>
        </div>
      </div>

      <div class="admin-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索用户名或姓名" clearable />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已启用" value="ENABLED" />
          <el-option label="已禁用" value="DISABLED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
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
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="userStatusTagType(row.status)">{{ userStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <template v-if="row.primaryRoleCode === 'EXTERNAL_USER' && row.status === 'PENDING'">
              <el-button link type="primary" @click="auditExternal(row, 'APPROVE')"
                >审核通过</el-button
              >
              <el-button link type="danger" @click="auditExternal(row, 'REJECT')">驳回</el-button>
            </template>
            <el-button
              v-if="row.status === 'ENABLED'"
              link
              type="warning"
              @click="changeStatus(row, 'DISABLED')"
              >禁用</el-button
            >
            <el-button
              v-if="row.status === 'DISABLED' || row.status === 'REJECTED'"
              link
              type="success"
              @click="changeStatus(row, 'ENABLED')"
              >启用</el-button
            >
            <el-button link type="primary" @click="openAuditLogs(row)">审核记录</el-button>
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
              <el-option
                v-for="item in departments"
                :key="item.id"
                :label="item.deptName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="grid-3">
          <el-form-item label="角色" prop="primaryRoleCode">
            <el-select v-model="userForm.primaryRoleCode" class="full-width">
              <el-option
                v-for="item in roles"
                :key="item.id"
                :label="item.roleName"
                :value="item.roleCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userForm.phone" maxlength="20" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userForm.email" maxlength="100" />
          </el-form-item>
        </div>
        <div class="grid-3">
          <el-form-item label="账号状态" prop="status">
            <el-select v-model="userForm.status" class="full-width">
              <el-option label="待审核" value="PENDING" />
              <el-option label="已启用" value="ENABLED" />
              <el-option label="已禁用" value="DISABLED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
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

    <el-dialog
      v-model="auditLogDialogVisible"
      :title="`审核记录 - ${auditLogTargetName || ''}`"
      width="820px"
      destroy-on-close
    >
      <el-table :data="auditLogs" border>
        <el-table-column label="动作类型" width="140">
          <template #default="{ row }">{{ auditActionLabel(row.actionType) }}</template>
        </el-table-column>
        <el-table-column label="结果" width="120">
          <template #default="{ row }">{{ auditResultLabel(row.actionResult) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="operatorRole" label="操作角色" width="130" />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>
      <div class="admin-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="auditLogQuery.pageNum"
          :page-size="auditLogQuery.pageSize"
          :page-sizes="pageSizeOptions"
          :total="auditLogTotal"
          @size-change="changeAuditLogPageSize"
          @current-change="changeAuditLogPage"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  auditUser,
  createUser,
  deleteUser,
  getAdminDepartments,
  getAdminRoles,
  getAdminUsersPage,
  getUserAuditLogs,
  updateUser,
  updateUserStatus
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
    status: 'ENABLED',
    password: ''
  }
}

export default {
  computed: {
    summaryCards() {
      const pending = this.users.filter((item) => item.status === 'PENDING').length
      const enabled = this.users.filter((item) => item.status === 'ENABLED').length
      const disabled = this.users.filter((item) => item.status === 'DISABLED').length
      const rejected = this.users.filter((item) => item.status === 'REJECTED').length
      return [
        { label: '用户总数', value: this.total || 0 },
        { label: '当前页待审核', value: pending },
        { label: '当前页已启用', value: enabled },
        { label: '当前页已禁用/驳回', value: disabled + rejected }
      ]
    }
  },
  data() {
    return {
      users: [],
      roles: [],
      departments: [],
      total: 0,
      pageSizeOptions: [10, 20, 50, 100],
      query: {
        keyword: '',
        status: '',
        pageNum: 1,
        pageSize: 10
      },
      userDialogVisible: false,
      userForm: defaultUserForm(),
      auditLogDialogVisible: false,
      auditLogTargetUserId: null,
      auditLogTargetName: '',
      auditLogs: [],
      auditLogTotal: 0,
      auditLogQuery: {
        pageNum: 1,
        pageSize: 10
      },
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
        status: [{ required: true, message: '请选择账号状态', trigger: 'change' }],
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
      this.query.status = query.status || ''
      this.query.pageNum = Number(query.pageNum || 1)
      this.query.pageSize = Number(query.pageSize || 10)
    },
    syncQuery() {
      this.$router.replace({
        path: this.$route.path,
        query: {
          keyword: this.query.keyword || undefined,
          status: this.query.status || undefined,
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
        status: row.status || 'ENABLED',
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
        ...this.userForm
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
    async auditExternal(row, action) {
      const actionText = action === 'APPROVE' ? '通过' : '驳回'
      let remark = ''
      if (action === 'REJECT') {
        const prompt = await ElMessageBox.prompt(`请填写驳回“${row.username}”的原因`, '驳回原因', {
          confirmButtonText: '确认驳回',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '请输入驳回原因',
          inputValidator: (value) => (value && value.trim() ? true : '驳回原因不能为空')
        })
        remark = (prompt.value || '').trim()
      } else {
        await ElMessageBox.confirm(
          `确认${actionText}该校外注册用户“${row.username}”吗？`,
          '审核确认'
        )
      }
      await auditUser(row.id, { action, remark })
      ElMessage.success(`已${actionText}`)
      await this.load()
    },
    async changeStatus(row, status) {
      const label = status === 'ENABLED' ? '启用' : '禁用'
      await ElMessageBox.confirm(`确认${label}账号“${row.username}”吗？`, '状态确认')
      await updateUserStatus(row.id, { status })
      ElMessage.success(`账号已${label}`)
      await this.load()
    },
    async openAuditLogs(row) {
      this.auditLogTargetUserId = row.id
      this.auditLogTargetName = row.username
      this.auditLogQuery.pageNum = 1
      this.auditLogQuery.pageSize = 10
      this.auditLogDialogVisible = true
      await this.loadAuditLogs()
    },
    async loadAuditLogs() {
      if (!this.auditLogTargetUserId) {
        return
      }
      const page = await getUserAuditLogs(this.auditLogTargetUserId, this.auditLogQuery)
      this.auditLogs = page.list || []
      this.auditLogTotal = page.total || 0
    },
    async changeAuditLogPage(pageNum) {
      this.auditLogQuery.pageNum = pageNum
      await this.loadAuditLogs()
    },
    async changeAuditLogPageSize(pageSize) {
      this.auditLogQuery.pageSize = pageSize
      this.auditLogQuery.pageNum = 1
      await this.loadAuditLogs()
    },
    userRoleLabel(value) {
      return userRoleLabelDict(value)
    },
    userStatusLabel(status) {
      if (status === 'PENDING') return '待审核'
      if (status === 'ENABLED') return '已启用'
      if (status === 'DISABLED') return '已禁用'
      if (status === 'REJECTED') return '已驳回'
      return '未知状态'
    },
    userStatusTagType(status) {
      if (status === 'PENDING') return 'warning'
      if (status === 'ENABLED') return 'success'
      if (status === 'DISABLED') return 'info'
      if (status === 'REJECTED') return 'danger'
      return 'info'
    },
    auditActionLabel(actionType) {
      if (actionType === 'REGISTER_AUDIT') return '注册审核'
      if (actionType === 'STATUS_CHANGE') return '状态变更'
      if (actionType === 'CREATE') return '创建用户'
      if (actionType === 'UPDATE') return '更新用户'
      return '未知动作'
    },
    auditResultLabel(result) {
      if (result === 'PASS') return '通过'
      if (result === 'ENABLED') return '已启用'
      if (result === 'DISABLED') return '已禁用'
      if (result === 'REJECTED') return '已驳回'
      return '未知结果'
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
