<template>
  <div class="page-shell">
    <div class="container center-shell">
      <aside class="center-sidebar content-card">
        <div class="center-brand">
          <div class="center-brand__kicker">个人服务入口</div>
          <div class="center-brand__title">用户中心</div>
        </div>
        <div class="center-nav">
          <div
            v-for="item in items"
            :key="item.path"
            class="center-nav-item"
            :class="{ active: isActive(item) }"
            @click="$router.push(item.path)"
          >
            {{ item.label }}
          </div>
        </div>
        <div class="center-actions">
          <el-button v-if="user" class="logout-btn" @click="logout">退出登录</el-button>
        </div>
      </aside>

      <main class="center-content">
        <div class="content-card center-page-header">
          <el-breadcrumb v-if="showBreadcrumb" separator="/">
            <el-breadcrumb-item
              v-for="(item, index) in breadcrumbItems"
              :key="`${item.label}-${index}`"
              :to="item.to"
            >
              {{ item.label }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <div class="center-page-title">{{ pageTitle }}</div>
          <div v-if="pageDescription" class="center-page-desc">{{ pageDescription }}</div>
        </div>
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
export default {
  computed: {
    user() {
      return this.$store.state.user
    },
    items() {
      return [
        { label: '中心首页', path: '/center' },
        { label: '账户中心', path: '/center/account' },
        { label: '充值申请', path: '/center/recharge' },
        { label: '我的订单', path: '/center/orders' },
        { label: '上机预约', path: '/center/machine-orders' },
        { label: '送样预约', path: '/center/sample-orders' },
        { label: '我的消息', path: '/center/messages' }
      ]
    },
    pageMeta() {
      return this.$route.meta || {}
    },
    pageTitle() {
      return this.pageMeta.title || '用户中心'
    },
    pageDescription() {
      return this.pageMeta.description || ''
    },
    showBreadcrumb() {
      return this.pageMeta.showBreadcrumb !== false && Array.isArray(this.pageMeta.breadcrumb)
    },
    breadcrumbItems() {
      const raw = this.pageMeta.breadcrumb || []
      return raw.map((label, index) => ({
        label,
        to: this.resolveBreadcrumbTo(index, raw.length)
      }))
    }
  },
  methods: {
    logout() {
      this.$store.commit('clearAuth')
      this.$router.push('/login')
    },
    isActive(item) {
      const current = this.$route.path
      if (item.path === '/center') {
        return current === '/center'
      }
      return current === item.path || current.startsWith(`${item.path}/`)
    },
    resolveBreadcrumbTo(index, total) {
      if (index >= total - 1) {
        return null
      }
      if (index === 0) {
        return '/center'
      }
      if (index === 1) {
        if (this.$route.path.startsWith('/center/machine-orders')) {
          return '/center/machine-orders'
        }
        if (this.$route.path.startsWith('/center/sample-orders')) {
          return '/center/sample-orders'
        }
        if (this.$route.path.startsWith('/center/orders')) {
          return '/center/orders'
        }
      }
      return null
    }
  }
}
</script>

<style scoped>
.center-shell {
  padding: 14px 0 24px;
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 14px;
}

.center-sidebar {
  padding: 14px 12px;
  align-self: start;
  position: sticky;
  top: 10px;
  max-height: calc(100vh - 20px);
  overflow: auto;
}

.center-brand {
  padding-bottom: 10px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 10px;
}

.center-brand__kicker {
  color: var(--muted);
  font-size: 12px;
}

.center-brand__title {
  margin-top: 4px;
  font-size: 16px;
  font-weight: 700;
  color: var(--primary);
}

.center-content {
  min-width: 0;
}

.center-page-header {
  margin-bottom: 14px;
  padding: 14px 18px;
  min-height: 96px;
}

.center-page-title {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #17427e;
}

.center-page-desc {
  margin-top: 8px;
  color: #70849e;
  line-height: 1.8;
}

.center-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.center-actions {
  margin-top: 10px;
}

.logout-btn {
  width: 100%;
}

.center-nav-item {
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  cursor: pointer;
  color: #36506f;
  background: #f8fbff;
  font-size: 13px;
  line-height: 1.4;
}

.center-nav-item.active {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
}

@media (max-width: 960px) {
  .center-shell {
    grid-template-columns: 1fr;
  }

  .center-sidebar {
    position: static;
    max-height: none;
    overflow: visible;
  }
}
</style>
