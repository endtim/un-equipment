<template>
  <div class="portal-shell">
    <div class="portal-topbar">
      <div class="portal-container topbar-inner">
        <div>欢迎访问高校大型仪器共享平台</div>
        <div class="topbar-links">
          <span>服务时间：周一至周五 08:30-17:30</span>
          <span v-if="!user" class="topbar-action" @click="$router.push('/login')">用户登录</span>
          <template v-else>
            <span class="topbar-action" @click="$router.push('/center')">{{ user.realName }}</span>
            <span class="topbar-action" @click="logout">退出登录</span>
          </template>
        </div>
      </div>
    </div>

    <header class="portal-header">
      <div class="portal-container header-inner">
        <div class="brand-block" @click="$router.push('/home')">
          <div class="seal-mark">校</div>
          <div>
            <div class="brand-school">学校大型仪器设备开放共享</div>
            <div class="brand-title">高校大型仪器共享平台</div>
          </div>
        </div>
        <div class="header-slogan">服务教学科研 | 促进开放共享 | 支持规范预约</div>
      </div>
    </header>

    <nav class="portal-nav">
      <div class="portal-container nav-inner">
        <div
          v-for="item in navItems"
          :key="item.label"
          class="nav-item"
          :class="{ active: isNavActive(item) }"
          @click="goNav(item)"
        >
          {{ item.label }}
        </div>
      </div>
    </nav>

    <main class="portal-main" :class="pageClass">
      <div class="portal-container">
        <section v-if="showPageHeader" class="portal-page-header content-card">
          <div class="portal-page-header__main">
            <el-breadcrumb v-if="showBreadcrumb" separator="/">
              <el-breadcrumb-item
                v-for="(item, index) in breadcrumbItems"
                :key="`${item.label}-${index}`"
                :to="item.to"
              >
                {{ item.label }}
              </el-breadcrumb-item>
            </el-breadcrumb>
            <div class="portal-page-title">{{ pageTitle }}</div>
            <div v-if="pageDescription" class="portal-page-desc">{{ pageDescription }}</div>
          </div>
        </section>

        <router-view />
      </div>
    </main>

    <footer class="portal-footer">
      <div class="portal-container">
        <div class="footer-title">高校大型仪器共享平台</div>
        <div class="footer-meta">版权所有：高校大型仪器共享平台建设项目组</div>
        <div class="footer-meta">地址：江西省南昌市高新区学府大道 999 号 | 电话：0791-88886666</div>
      </div>
    </footer>
  </div>
</template>

<script>
export default {
  computed: {
    user() {
      return this.$store.state.user
    },
    navItems() {
      return [
        { label: '首页', path: '/home', match: ['/home'] },
        { label: '平台成员', path: '/platform-members', match: ['/platform-members'] },
        { label: '预约服务', path: '/instruments', match: ['/instruments'] },
        { label: '院级平台', path: '/college-platforms', match: ['/college-platforms'] },
        { label: '平台简介', path: '/platform-intro', match: ['/platform-intro'] },
        { label: '通知公告', path: '/notices', match: ['/notices'] },
        { label: '需求发布', path: '/demands', match: ['/demands'] },
        { label: '帮助中心', path: '/help-center', match: ['/help-center'] },
        { label: '管理平台', path: this.user ? '/admin' : '/login', match: ['/admin'] }
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
      return this.pageMeta.pageType && this.pageMeta.pageType !== 'landing' && this.pageMeta.pageType !== 'entry'
    },
    breadcrumbItems() {
      const raw = this.pageMeta.breadcrumb || []
      return raw.map((label, index) => ({
        label,
        to: this.resolveBreadcrumbTo(index, raw.length)
      }))
    },
    pageClass() {
      return `portal-main--${this.pageMeta.pageType || 'section'}`
    }
  },
  methods: {
    logout() {
      this.$store.commit('clearAuth')
      this.$router.push('/home')
    },
    goNav(item) {
      if (item.path && item.path !== this.$route.path) {
        this.$router.push(item.path)
      }
    },
    isNavActive(item) {
      if (item.path === '/instruments') {
        return this.$route.path.startsWith('/instruments')
      }
      return item.match.includes(this.$route.path)
    },
    resolveBreadcrumbTo(index, total) {
      if (index >= total - 1) {
        return null
      }
      if (index === 0) {
        return '/home'
      }
      if (index === 1 && this.$route.path.startsWith('/instruments')) {
        return '/instruments'
      }
      return null
    }
  }
}
</script>

<style scoped>
.portal-shell {
  min-height: 100vh;
  background: linear-gradient(180deg, rgba(11, 78, 162, 0.08), rgba(11, 78, 162, 0.02) 220px, transparent 220px), #f4f7fb;
}

.portal-container {
  width: min(1320px, calc(100% - 32px));
  margin: 0 auto;
}

.portal-topbar {
  height: 40px;
  background: #0a3d84;
  color: rgba(255, 255, 255, 0.84);
  font-size: 13px;
}

.topbar-inner {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topbar-links {
  display: flex;
  align-items: center;
  gap: 20px;
}

.topbar-action {
  cursor: pointer;
}

.portal-header {
  background: linear-gradient(90deg, #0b4ea2 0%, #0c56b2 60%, #1263c8 100%);
  color: #fff;
}

.header-inner {
  height: 116px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 18px;
  cursor: pointer;
}

.seal-mark {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #f4d88a, #d5aa40 72%, #bb8e28);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  box-shadow: inset 0 0 0 4px rgba(255, 255, 255, 0.2);
}

.brand-school {
  font-size: 18px;
  letter-spacing: 1px;
  opacity: 0.92;
}

.brand-title {
  font-size: 34px;
  font-weight: 700;
  margin-top: 6px;
  letter-spacing: 2px;
}

.header-slogan {
  font-size: 16px;
  opacity: 0.9;
}

.portal-nav {
  position: sticky;
  top: 0;
  z-index: 30;
  background: #0a4b99;
  border-top: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08), 0 2px 10px rgba(11, 78, 162, 0.08);
}

.nav-inner {
  height: 58px;
  display: flex;
  align-items: center;
}

.nav-item {
  color: #fff;
  padding: 0 28px;
  font-size: 16px;
  line-height: 58px;
  cursor: pointer;
  position: relative;
  white-space: nowrap;
}

.nav-item.active::after {
  content: "";
  position: absolute;
  left: 22px;
  right: 22px;
  bottom: 0;
  height: 3px;
  background: #e8c56f;
}

.portal-main {
  padding: 22px 0 40px;
}

.portal-main--landing {
  padding-top: 22px;
}

.portal-main--reading .portal-container,
.portal-main--detail .portal-container {
  width: min(1240px, calc(100% - 32px));
}

.portal-page-header {
  margin-bottom: 18px;
  padding: 18px 24px;
  border-radius: 6px;
}

.portal-page-title {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 700;
  color: #143c77;
}

.portal-page-desc {
  margin-top: 8px;
  color: #70849e;
  line-height: 1.8;
}

.portal-footer {
  margin-top: 24px;
  background: #0c4a95;
  color: rgba(255, 255, 255, 0.92);
  text-align: center;
  padding: 24px 16px 28px;
}

.footer-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
}

.footer-meta {
  font-size: 13px;
  line-height: 1.8;
}

@media (max-width: 1200px) {
  .header-inner {
    height: auto;
    padding: 20px 0;
    gap: 12px;
    flex-direction: column;
    align-items: flex-start;
  }

  .nav-inner {
    overflow-x: auto;
  }
}
</style>
