<template>
  <div class="page-shell">
    <div class="container admin-shell">
      <aside class="admin-sidebar content-card">
        <div class="admin-brand">
          <div class="admin-brand__kicker">后台工作区</div>
          <div class="admin-brand__title">管理平台</div>
        </div>
        <div v-for="group in groups" :key="group.key" class="admin-nav-group">
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
const ADMIN_GROUPS = [
  {
    key: 'workbench',
    label: '工作台',
    items: [
      { label: '后台首页', path: '/admin', roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER'] },
      { label: '统计报表', path: '/admin/stats', roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER'] },
      { label: '日志审计', path: '/admin/logs', roles: ['ADMIN'] }
    ]
  },
  {
    key: 'system',
    label: '系统管理',
    items: [
      { label: '用户管理', path: '/admin/users', roles: ['ADMIN', 'DEPT_MANAGER'] },
      { label: '角色管理', path: '/admin/roles', roles: ['ADMIN'] },
      { label: '部门管理', path: '/admin/departments', roles: ['ADMIN'] }
    ]
  },
  {
    key: 'instrument',
    label: '仪器管理',
    items: [
      { label: '仪器分类', path: '/admin/categories', roles: ['ADMIN'] },
      { label: '仪器管理', path: '/admin/instruments', roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER'] },
      { label: '开放规则', path: '/admin/open-rules', roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER'] }
    ]
  },
  {
    key: 'business',
    label: '业务处理',
    items: [
      { label: '上机订单', path: '/admin/orders/machine', roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER'] },
      { label: '送样订单', path: '/admin/orders/sample', roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER'] },
      { label: '充值审核', path: '/admin/recharges', roles: ['ADMIN', 'DEPT_MANAGER'] },
      { label: '结算管理', path: '/admin/settlements', roles: ['ADMIN', 'DEPT_MANAGER'] }
    ]
  },
  {
    key: 'content',
    label: '内容管理',
    items: [
      { label: '公告管理', path: '/admin/notices', roles: ['ADMIN'] },
      { label: '帮助文档', path: '/admin/help-docs', roles: ['ADMIN'] }
    ]
  }
]

export default {
  computed: {
    user() {
      return this.$store.state.user
    },
    roleCode() {
      return this.user?.roleCode || ''
    },
    groups() {
      return ADMIN_GROUPS
        .map(group => ({
          ...group,
          items: group.items.filter(item => item.roles.includes(this.roleCode))
        }))
        .filter(group => group.items.length > 0)
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
      return this.pageMeta.showBreadcrumb !== false
    },
    showPageHeader() {
      return this.pageMeta.pageType && this.pageMeta.pageType !== 'entry'
    },
    currentNav() {
      const currentPath = this.$route.path
      let bestMatch = null
      this.groups.forEach(group => {
        group.items.forEach(item => {
          if (currentPath === item.path || currentPath.startsWith(`${item.path}/`)) {
            if (!bestMatch || item.path.length > bestMatch.item.path.length) {
              bestMatch = { group, item }
            }
          }
        })
      })
      return bestMatch
    },
    breadcrumbItems() {
      if (this.currentNav) {
        const { group, item } = this.currentNav
        const groupEntry = group.items[0]
        return [
          { label: '管理平台', to: this.$route.path === '/admin' ? null : '/admin' },
          { label: group.label, to: groupEntry.path === item.path ? null : groupEntry.path },
          { label: item.label, to: null }
        ]
      }

      const raw = this.pageMeta.breadcrumb || []
      return raw.map((label, index) => ({
        label,
        to: index === raw.length - 1 ? null : index === 0 ? '/admin' : null
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
      if (item.path === '/admin') {
        return current === '/admin'
      }
      return current === item.path || current.startsWith(`${item.path}/`)
    }
  }
}
</script>

<style scoped>
.admin-shell {
  padding: 14px 0 24px;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 14px;
}

.admin-sidebar {
  padding: 12px;
  align-self: start;
  position: sticky;
  top: 10px;
}

.admin-brand {
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 8px;
}

.admin-brand__kicker {
  color: var(--muted);
  font-size: 12px;
}

.admin-brand__title {
  margin-top: 4px;
  font-size: 15px;
  font-weight: 700;
  color: var(--primary);
}

.admin-content {
  min-width: 0;
}

.admin-page-header {
  margin-bottom: 14px;
  padding: 14px 18px;
}

.admin-page-title {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #17427e;
}

.admin-page-desc {
  margin-top: 8px;
  color: #70849e;
  line-height: 1.8;
}

.admin-nav-group + .admin-nav-group {
  margin-top: 8px;
}

.admin-group-title {
  font-size: 11px;
  color: var(--muted);
  margin-bottom: 6px;
  line-height: 1.3;
}

.admin-nav-item {
  padding: 7px 11px;
  border: 1px solid var(--line);
  border-radius: 6px;
  cursor: pointer;
  color: #36506f;
  background: #f8fbff;
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 1.3;
}

.admin-nav-item.active {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
}

.admin-actions {
  margin-top: 8px;
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
