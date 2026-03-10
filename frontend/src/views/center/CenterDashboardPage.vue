<template>
  <div class="admin-page">
    <div class="grid-3">
      <div v-for="card in cards" :key="card.label" class="content-card admin-card">
        <div style="color: var(--muted);">{{ card.label }}</div>
        <div style="font-size: 30px; font-weight: 700; margin-top: 8px;">{{ card.value }}</div>
      </div>
    </div>
    <div class="content-card admin-card">
      <h3 class="admin-card-title">快捷入口</h3>
      <div style="display: flex; gap: 12px; flex-wrap: wrap;">
        <el-button type="primary" @click="$router.push('/center/orders')">查看全部订单</el-button>
        <el-button @click="$router.push('/center/machine-orders')">我的上机预约</el-button>
        <el-button @click="$router.push('/center/sample-orders')">我的送样预约</el-button>
        <el-button @click="$router.push('/center/account')">账户中心</el-button>
        <el-button @click="$router.push('/center/messages')">我的消息</el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  computed: {
    user() {
      return this.$store.state.user || {}
    },
    roleLabel() {
      const mapping = {
        ADMIN: '平台管理员',
        INSTRUMENT_OWNER: '仪器负责人',
        DEPT_MANAGER: '部门管理员',
        INTERNAL_USER: '校内用户',
        EXTERNAL_USER: '校外用户'
      }
      return mapping[this.user.roleCode] || this.user.roleCode || '-'
    },
    cards() {
      return [
        { label: '当前用户', value: this.user.realName || '-' },
        { label: '角色类型', value: this.roleLabel },
        { label: '所属单位', value: this.user.departmentName || '-' }
      ]
    }
  }
}
</script>
