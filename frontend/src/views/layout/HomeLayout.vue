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
          <div class="brand-logo-frame">
            <img
              v-if="!logoLoadFailed"
              :src="schoolLogoSrc"
              alt="学校校徽"
              class="brand-logo"
              @error="logoLoadFailed = true"
            />
            <div v-else class="brand-logo-fallback">校</div>
          </div>
          <div class="brand-university">
            <div class="uni-cn">南昌大学</div>
            <div class="uni-en">NANCHANG UNIVERSITY</div>
          </div>
        </div>
        <div class="brand-title">大型仪器预约共享管理平台</div>
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
  data() {
    return {
      logoLoadFailed: false
    }
  },
  computed: {
    schoolLogoSrc() {
      return `${process.env.BASE_URL || '/'}school-logo.png`
    },
    user() {
      return this.$store.state.user
    },
    navItems() {
      return [
        { label: '首页', path: '/home', match: ['/home'] },
        { label: '平台成员', path: '/platform-members', match: ['/platform-members'] },
        { label: '预约服务', path: '/instruments', match: ['/instruments'] },
        { label: '平台简介', path: '/platform-intro', match: ['/platform-intro'] },
        { label: '通知公告', path: '/notices', match: ['/notices'] },
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
      return (
        this.pageMeta.pageType &&
        this.pageMeta.pageType !== 'landing' &&
        this.pageMeta.pageType !== 'entry'
      )
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
  background:
    linear-gradient(
      180deg,
      rgba(11, 78, 162, 0.08),
      rgba(11, 78, 162, 0.02) 220px,
      transparent 220px
    ),
    #f4f7fb;
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
  position: relative;
  overflow: hidden;
  background: linear-gradient(90deg, #06479f 0%, #0b57b7 55%, #0f6ac8 100%);
  color: #fff;
}

.portal-header::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 15% 20%, rgba(255, 255, 255, 0.14), transparent 38%),
    radial-gradient(circle at 88% 40%, rgba(75, 177, 255, 0.2), transparent 36%);
  pointer-events: none;
}

.header-inner {
  position: relative;
  z-index: 1;
  min-height: 112px;
  padding: 14px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-logo-frame {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.45);
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.brand-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: rgba(255, 255, 255, 0.96);
}

.brand-logo-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.92);
  background: radial-gradient(circle at 35% 35%, #f4d88a, #d5aa40 72%, #bb8e28);
  filter: drop-shadow(0 3px 8px rgba(2, 26, 70, 0.35));
}

.brand-university {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.uni-cn {
  font-size: 38px;
  line-height: 1;
  font-weight: 700;
  letter-spacing: 4px;
}

.uni-en {
  font-size: 18px;
  line-height: 1;
  letter-spacing: 1px;
  opacity: 0.95;
  margin-top: 6px;
}

.brand-title {
  font-size: 42px;
  font-weight: 700;
  letter-spacing: 1px;
  line-height: 1.1;
  white-space: nowrap;
  text-shadow: 0 2px 6px rgba(2, 26, 70, 0.3);
}

.portal-nav {
  position: sticky;
  top: 0;
  z-index: 30;
  background: #0a4b99;
  border-top: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 2px 10px rgba(11, 78, 162, 0.08);
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
  content: '';
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
    padding: 16px 0;
    gap: 12px;
    flex-direction: column;
    align-items: flex-start;
  }

  .brand-logo-frame {
    width: 60px;
    height: 60px;
  }

  .uni-cn {
    font-size: 30px;
    letter-spacing: 2px;
  }

  .uni-en {
    font-size: 14px;
    letter-spacing: 1px;
  }

  .brand-title {
    font-size: 24px;
  }

  .nav-inner {
    overflow-x: auto;
  }
}
</style>
