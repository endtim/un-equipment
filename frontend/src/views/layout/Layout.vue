<template>
  <div class="page-shell">
    <router-view v-if="isHome" />
    <div v-else class="container" style="padding: 20px 0 32px;">
      <div class="hero-card" style="padding: 18px 24px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
        <div>
          <div style="font-size: 13px; color: var(--muted);">高校大型仪器共享平台</div>
          <div style="font-size: 26px; font-weight: 700; margin-top: 6px;">大型仪器预约共享管理平台</div>
        </div>
        <div style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap; justify-content: flex-end;">
          <el-button text @click="$router.push('/')">首页</el-button>
          <el-button text @click="$router.push('/instruments')">仪器资源</el-button>
          <el-button text @click="$router.push('/platform-intro')">平台简介</el-button>
          <el-button text @click="$router.push('/notices')">通知公告</el-button>
          <el-button text @click="$router.push('/help-center')">帮助中心</el-button>
          <el-button v-if="user" text @click="$router.push('/orders')">我的预约</el-button>
          <el-button v-if="user" text @click="$router.push('/account')">账户中心</el-button>
          <el-button v-if="user" text @click="$router.push('/messages')">消息中心</el-button>
          <el-button v-if="isAdmin" text @click="$router.push('/admin')">管理首页</el-button>
          <el-button v-if="isAdmin" text @click="$router.push('/admin/system')">系统管理</el-button>
          <el-button v-if="isAdmin" text @click="$router.push('/admin/instruments')">仪器管理</el-button>
          <el-button v-if="isAdmin" text @click="$router.push('/admin/content')">内容管理</el-button>
          <el-button v-if="isAdmin" text @click="$router.push('/admin/stats')">统计分析</el-button>
          <el-button v-if="isAdmin" text @click="$router.push('/admin/logs')">操作日志</el-button>
          <el-button v-if="!user" type="primary" @click="$router.push('/login')">登录</el-button>
          <el-dropdown v-else>
            <span class="el-dropdown-link">{{ user.realName }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/account')">账户中心</el-dropdown-item>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <router-view />
    </div>
  </div>
</template>

<script>
export default {
  computed: {
    isHome() {
      return this.$route.path === '/'
    },
    user() {
      return this.$store.state.user
    },
    isAdmin() {
      return this.user && !['INTERNAL_USER', 'EXTERNAL_USER'].includes(this.user.roleCode)
    }
  },
  methods: {
    logout() {
      this.$store.commit('clearAuth')
      this.$router.push('/')
    }
  }
}
</script>
