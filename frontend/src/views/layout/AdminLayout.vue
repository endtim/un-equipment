<template>
  <div class="page-shell">
    <div class="container admin-shell">
      <aside class="admin-sidebar content-card">
        <div class="admin-brand">
          <div class="admin-brand__kicker">后台工作区</div>
          <div class="admin-brand__title">管理平台</div>
        </div>
        <div class="admin-nav-group" v-for="group in groups" :key="group.label">
          <div class="admin-group-title">{{ group.label }}</div>
          <div
            v-for="item in group.items"
            :key="item.path"
            class="admin-nav-item"
            :class="{ active: isActive(item) }"
            @click="$router.push(item.path)"
          >
            {{ item.label }}
          </div>
        </div>
        <div class="admin-actions">
          <el-button v-if="user" class="logout-btn" @click="logout">退出登录</el-button>
        </div>
      </aside>

      <main class="admin-content">
        <div v-if="showPageHeader" class="content-card admin-page-header">
          <el-breadcrumb v-if="showBreadcrumb" separator="/">
            <el-breadcrumb-item
              v-for="(item, index) in breadcrumbItems"
              :key="`${item.label}-${index}`"
              :to="item.to"
            >
              {{ item.label }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <div class="admin-page-title">{{ pageTitle }}</div>
          <div v-if="pageDescription" class="admin-page-desc">{{ pageDescription }}</div>
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
    groups() {
      return [
        {
          label: '工作台',
          items: [
            { label: '后台首页', path: '/admin' },
            { label: '统计报表', path: '/admin/stats' },
            { label: '日志审计', path: '/admin/logs' }
          ]
        },
        {
          label: '系统管理',
          items: [
            { label: '用户管理', path: '/admin/users' },
            { label: '部门管理', path: '/admin/departments' }
          ]
        },
        {
          label: '仪器管理',
          items: [
            { label: '仪器分类', path: '/admin/categories' },
            { label: '仪器管理', path: '/admin/instruments' }
          ]
        },
        {
          label: '业务处理',
          items: [
            { label: '上机订单', path: '/admin/orders/machine' },
            { label: '送样订单', path: '/admin/orders/sample' },
            { label: '充值审核', path: '/admin/recharges' },
            { label: '结算管理', path: '/admin/settlements' }
          ]
        },
        {
          label: '内容管理',
          items: [
            { label: '公告管理', path: '/admin/notices' },
            { label: '帮助文档', path: '/admin/help-docs' }
          ]
        }
      ]
    },
    pageMeta() {
      return this.$route.meta || {}
    },
    pageTitle() {
      return this.pageMeta.title || ''
    },
    pageDescription() {
      return this.pageMeta.description || ''
    },
    showBreadcrumb() {
      return this.pageMeta.showBreadcrumb !== false && Array.isArray(this.pageMeta.breadcrumb)
    },
    showPageHeader() {
      return this.pageMeta.pageType && this.pageMeta.pageType !== 'entry'
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
      return this.$route.path === item.path || this.$route.path.startsWith(`${item.path}/`)
    },
    resolveBreadcrumbTo(index, total) {
      if (index >= total - 1) {
        return null
      }
      if (index === 0) {
        return '/admin'
      }
      if (index === 1) {
        if (this.$route.path.startsWith('/admin/users') || this.$route.path.startsWith('/admin/departments')) {
          return '/admin/users'
        }
        if (this.$route.path.startsWith('/admin/categories') || this.$route.path.startsWith('/admin/instruments')) {
          return '/admin/instruments'
        }
        if (this.$route.path.startsWith('/admin/orders') || this.$route.path.startsWith('/admin/recharges') || this.$route.path.startsWith('/admin/settlements')) {
          return '/admin/orders/machine'
        }
        if (this.$route.path.startsWith('/admin/notices') || this.$route.path.startsWith('/admin/help-docs')) {
          return '/admin/notices'
        }
        if (this.$route.path.startsWith('/admin/stats') || this.$route.path.startsWith('/admin/logs')) {
          return '/admin/stats'
        }
      }
      return null
    }
  }
}
</script>

<style scoped>
.admin-shell {
  padding: 20px 0 32px;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 18px;
}

.admin-sidebar {
  padding: 20px;
  align-self: start;
  position: static;
}

.admin-brand {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 16px;
}

.admin-brand__kicker {
  color: var(--muted);
  font-size: 13px;
}

.admin-brand__title {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: var(--primary);
}

.admin-content {
  min-width: 0;
}

.admin-page-header {
  margin-bottom: 18px;
  padding: 18px 22px;
}

.admin-page-title {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: #17427e;
}

.admin-page-desc {
  margin-top: 8px;
  color: #70849e;
  line-height: 1.8;
}

.admin-nav-group + .admin-nav-group {
  margin-top: 16px;
}

.admin-group-title {
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 8px;
}

.admin-nav-item {
  padding: 11px 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  cursor: pointer;
  color: #36506f;
  background: #f8fbff;
  margin-bottom: 8px;
}

.admin-nav-item.active {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
}

.admin-actions {
  margin-top: 14px;
}

.logout-btn {
  width: 100%;
}

@media (max-width: 960px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: static;
  }
}
</style>
