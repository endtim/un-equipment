<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <div class="admin-toolbar">
        <div class="admin-card-title admin-card-title--tight">角色列表</div>
        <el-button type="primary" @click="openCreateRole">新增角色</el-button>
      </div>

      <el-table :data="roles" border>
        <el-table-column prop="roleName" label="角色名称" min-width="200" />
        <el-table-column prop="roleCode" label="角色编码" min-width="200" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="openEditRole(row)">编辑</el-button>
            <el-button link type="danger" @click="removeRole(row)">删除</el-button>
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
      <el-empty v-if="roles.length === 0" description="暂无角色数据" class="admin-empty" />
    </div>

    <el-dialog
      v-model="roleDialogVisible"
      :title="roleForm.id ? '编辑角色' : '新增角色'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-position="top">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="roleForm.roleCode" maxlength="50" :disabled="Boolean(roleForm.id)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeRoleDialog">取消</el-button>
        <el-button type="primary" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRole, deleteRole, getAdminRolesPage, updateRole } from '../../../api/admin'

function defaultRoleForm() {
  return { id: null, roleName: '', roleCode: '' }
}

export default {
  data() {
    return {
      roles: [],
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 10
      },
      roleDialogVisible: false,
      roleForm: defaultRoleForm(),
      roleRules: {
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
      }
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      const page = await getAdminRolesPage(this.query)
      this.roles = page.list || []
      this.total = page.total || 0
    },
    async onSizeChange() {
      this.query.pageNum = 1
      await this.load()
    },
    openCreateRole() {
      this.roleForm = defaultRoleForm()
      this.roleDialogVisible = true
    },
    openEditRole(row) {
      this.roleForm = { ...row }
      this.roleDialogVisible = true
    },
    closeRoleDialog() {
      this.roleDialogVisible = false
      this.$nextTick(() => {
        if (this.$refs.roleFormRef) {
          this.$refs.roleFormRef.clearValidate()
        }
      })
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
      this.closeRoleDialog()
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

.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
