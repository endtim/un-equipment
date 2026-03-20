<template>
  <div class="page-shell">
    <div class="container admin-shell">
      <aside class="admin-sidebar content-card">
        <div class="admin-brand">
          <div class="admin-brand__kicker">后台工作区</div>
          <div class="admin-brand__title">管理平台</div>
        </div>
        <el-collapse v-model="activeGroupKey" accordion class="admin-accordion">
          <el-collapse-item
            v-for="group in groups"
            :key="group.key"
            :name="group.key"
            class="admin-nav-group"
          >
            <template #title>
              <span class="admin-group-title">{{ group.label }}</span>
            </template>
            <div v-for="item in group.items" :key="item.path">
              <div
                class="admin-nav-item"
                :class="{ active: isActive(item) }"
                @click="onNavClick(item)"
              >
                {{ item.label }}
              </div>
              <div v-if="item.children && item.children.length" class="admin-sub-nav">
                <div
                  v-for="child in item.children"
                  :key="child.path"
                  class="admin-sub-nav-item"
                  :class="{ active: isActive(child) }"
                  @click="$router.push(child.path)"
                >
                  {{ child.label }}
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
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
      {
        label: '统计报表',
        path: '/admin/stats',
        roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
      },
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
      {
        label: '仪器管理',
        path: '/admin/instruments',
        roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
      },
      {
        label: '开放规则',
        path: '/admin/open-rules',
        roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
      }
    ]
  },
  {
    key: 'orders',
    label: '订单与审核',
    items: [
      {
        label: '上机订单',
        path: '/admin/orders/machine',
        roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
      },
      {
        label: '送样订单',
        path: '/admin/orders/sample',
        roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
      },
      { label: '充值审核', path: '/admin/recharges', roles: ['ADMIN', 'DEPT_MANAGER'] },
      {
        label: '结算管理',
        path: '/admin/settlements',
        roles: ['ADMIN', 'DEPT_MANAGER'],
        children: [
          {
            label: '结算总览',
            path: '/admin/settlements/overview',
            roles: ['ADMIN', 'DEPT_MANAGER']
          },
          {
            label: '异常账处理',
            path: '/admin/settlements/anomalies',
            roles: ['ADMIN', 'DEPT_MANAGER']
          }
        ]
      }
    ]
  },
  {
    key: 'finance',
    label: '经费管理',
    items: [
      { label: '经费明细报表', path: '/admin/finance/report', roles: ['ADMIN', 'DEPT_MANAGER'] },
      { label: '维护支出登记', path: '/admin/finance/expenses', roles: ['ADMIN', 'DEPT_MANAGER'] },
      {
        label: '预算预警',
        path: '/admin/finance/budget-warnings',
        roles: ['ADMIN', 'DEPT_MANAGER']
      },
      { label: '预算台账', path: '/admin/finance/budget-ledger', roles: ['ADMIN', 'DEPT_MANAGER'] }
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
  data() {
    return {
      activeGroupKey: ''
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    },
    roleCode() {
      return this.user?.roleCode || ''
    },
    groups() {
      return ADMIN_GROUPS.map((group) => ({
        ...group,
        items: group.items
          .map((item) => {
            const visibleChildren = (item.children || []).filter((child) =>
              child.roles.includes(this.roleCode)
            )
            return {
              ...item,
              children: visibleChildren
            }
          })
          .filter(
            (item) =>
              item.roles.includes(this.roleCode) ||
              (Array.isArray(item.children) && item.children.length > 0)
          )
      })).filter((group) => group.items.length > 0)
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
      this.groups.forEach((group) => {
        group.items.forEach((item) => {
          if (currentPath === item.path || currentPath.startsWith(`${item.path}/`)) {
            if (!bestMatch || item.path.length > bestMatch.item.path.length) {
              bestMatch = { group, parent: item, item }
            }
          }
          ;(item.children || []).forEach((child) => {
            if (currentPath === child.path || currentPath.startsWith(`${child.path}/`)) {
              if (!bestMatch || child.path.length > bestMatch.item.path.length) {
                bestMatch = { group, parent: item, item: child }
              }
            }
          })
        })
      })
      return bestMatch
    },
    breadcrumbItems() {
      if (this.currentNav) {
        const { group, parent, item } = this.currentNav
        const groupEntry = group.items[0]
        const list = [
          { label: '管理平台', to: this.$route.path === '/admin' ? null : '/admin' },
          { label: group.label, to: groupEntry.path === item.path ? null : groupEntry.path }
        ]
        if (parent && parent.path !== item.path) {
          list.push({ label: parent.label, to: parent.path })
        }
        list.push({ label: item.label, to: null })
        return list
      }

      const raw = this.pageMeta.breadcrumb || []
      return raw.map((label, index) => ({
        label,
        to: index === raw.length - 1 ? null : index === 0 ? '/admin' : null
      }))
    }
  },
  watch: {
    groups: {
      immediate: true,
      handler() {
        this.syncActiveGroup()
      }
    },
    '$route.path'() {
      this.syncActiveGroup()
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
    },
    onNavClick(item) {
      if (item.children && item.children.length > 0) {
        const target = item.children[0]?.path || item.path
        this.$router.push(target)
        return
      }
      this.$router.push(item.path)
    },
    syncActiveGroup() {
      if (!this.groups.length) {
        this.activeGroupKey = ''
        return
      }
      if (this.currentNav?.group?.key) {
        this.activeGroupKey = this.currentNav.group.key
        return
      }
      const exists = this.groups.some((group) => group.key === this.activeGroupKey)
      if (!exists) {
        this.activeGroupKey = this.groups[0].key
      }
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
  min-height: 104px;
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

.admin-group-title {
  font-size: 13px;
  font-weight: 700;
  color: #355577;
  letter-spacing: 0.2px;
  line-height: 1.3;
}

.admin-nav-item {
  padding: 8px 12px;
  border: 1px solid #d7e2ef;
  border-radius: 6px;
  cursor: pointer;
  color: #3b5674;
  background: #f9fbff;
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 1.3;
  transition: all 0.18s ease;
}

.admin-nav-item:hover {
  background: #f1f6ff;
  border-color: #bfd3ea;
  transform: translateX(1px);
}

.admin-sub-nav {
  margin: -2px 0 10px 0;
  padding: 2px 0 0 10px;
  border-left: 2px solid #d8e5f5;
}

.admin-sub-nav-item {
  padding: 6px 10px;
  border: 1px solid #dfe8f4;
  border-radius: 6px;
  cursor: pointer;
  color: #4d6480;
  background: #fcfdff;
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 1.3;
  transition: all 0.18s ease;
}

.admin-sub-nav-item:hover {
  background: #f2f7ff;
  border-color: #c4d8f0;
  transform: translateX(1px);
}

.admin-sub-nav-item.active {
  color: #0f3e74;
  border-color: #9ebde0;
  background: linear-gradient(180deg, #f4f8ff 0%, #eaf2ff 100%);
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(15, 62, 116, 0.05);
}

.admin-accordion {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.admin-accordion :deep(.el-collapse-item__header) {
  border: 1px solid #d9e4f1;
  border-radius: 8px;
  padding: 0 10px;
  height: 40px;
  line-height: 40px;
  font-size: 13px;
  background: #f8fbff;
  transition: all 0.18s ease;
}

.admin-accordion :deep(.el-collapse-item__header:hover) {
  background: #f2f7ff;
  border-color: #c2d6ec;
}

.admin-accordion :deep(.el-collapse-item__header.is-active) {
  background: linear-gradient(180deg, #f4f8ff 0%, #edf4ff 100%);
  border-color: #a9c3e2;
  box-shadow: 0 2px 8px rgba(20, 72, 138, 0.08);
}

.admin-accordion :deep(.el-collapse-item__arrow) {
  color: #6d88a7;
  font-weight: 700;
}

.admin-accordion :deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

.admin-accordion :deep(.el-collapse-item__content) {
  padding: 8px 2px 4px;
}

.admin-nav-item.active {
  color: #fff;
  background: linear-gradient(180deg, #1f5fb0 0%, #154f98 100%);
  border-color: #154f98;
  box-shadow: 0 3px 8px rgba(16, 73, 142, 0.2);
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
