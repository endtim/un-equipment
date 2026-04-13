<template>
  <div class="portal-shell">
    <div class="portal-topbar">
      <div class="portal-container topbar-inner">
        <div>欢迎访问高校大型仪器共享平台</div>
        <div class="topbar-links">
          <span>服务时间：周一至周五 08:30-17:30</span>
          <span class="topbar-action" @click="aboutVisible = true">关于系统</span>
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
            <div class="brand-meta-line">{{ siteInfo.college }}</div>
            <div class="brand-meta-line">指导老师：{{ siteInfo.supervisorName }}</div>
            <div class="brand-meta-line">设计者：{{ siteInfo.designerName }}</div>
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
        <div class="footer-title">{{ siteInfo.systemName }}</div>
        <div class="footer-meta">{{ siteInfo.school }} · {{ siteInfo.college }}</div>
        <div class="footer-meta">指导老师：{{ siteInfo.supervisorName }} | 设计者：{{ siteInfo.designerName }}</div>
        <div class="footer-meta">版本：{{ siteInfo.version }} | 设计时间：{{ siteInfo.designTime }}</div>
        <div class="footer-meta">地址：{{ siteInfo.address }} | 电话：{{ siteInfo.phone }}</div>
        <div class="footer-meta">{{ siteInfo.copyright }}</div>
      </div>
    </footer>

    <el-dialog v-model="aboutVisible" title="关于系统" width="720px">
      <div class="about-dialog">
        <div class="about-dialog__header">
          <div class="about-dialog__logo-wrap">
            <img
              v-if="!logoLoadFailed"
              :src="schoolLogoSrc"
              alt="学校校徽"
              class="about-dialog__logo"
              @error="logoLoadFailed = true"
            />
            <div v-else class="about-dialog__logo-fallback">校</div>
          </div>
          <div class="about-dialog__title-group">
            <div class="about-dialog__title">{{ siteInfo.systemName }}</div>
            <div class="about-dialog__subtitle">{{ siteInfo.school }} · {{ siteInfo.college }}</div>
            <div class="about-dialog__desc">{{ siteInfo.description }}</div>
          </div>
        </div>
        <div class="about-grid">
          <div class="about-item"><span>系统名称</span><strong>{{ siteInfo.systemName }}</strong></div>
          <div class="about-item"><span>版本号</span><strong>{{ siteInfo.version }}</strong></div>
          <div class="about-item"><span>指导老师</span><strong>{{ siteInfo.supervisorName }}</strong></div>
          <div class="about-item"><span>设计者</span><strong>{{ siteInfo.designerName }}</strong></div>
          <div class="about-item"><span>年级班级</span><strong>{{ siteInfo.gradeClass }}</strong></div>
          <div class="about-item"><span>专业</span><strong>{{ siteInfo.major }}</strong></div>
          <div class="about-item"><span>学校</span><strong>{{ siteInfo.school }}</strong></div>
          <div class="about-item"><span>设计时间</span><strong>{{ siteInfo.designTime }}</strong></div>
          <div class="about-item about-item--wide"><span>版权信息</span><strong>{{ siteInfo.copyright }}</strong></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import siteInfo from '../../config/siteInfo'

export default {
  data() {
    return {
      logoLoadFailed: false,
      aboutVisible: false
    }
  },
  computed: {
    siteInfo() {
      return siteInfo
    },
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
      // 首页和入口页不展示二级页头，其余业务页统一走页面头信息，保持导航体验一致。
      return (
        this.pageMeta.pageType &&
        this.pageMeta.pageType !== 'landing' &&
        this.pageMeta.pageType !== 'entry'
      )
    },
    breadcrumbItems() {
      const raw = this.pageMeta.breadcrumb || []
      // 面包屑只在可回退节点生成跳转，最后一级保持当前页文本。
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
      // 预约服务下有详情页，需用 startsWith 覆盖“列表 + 详情”高亮场景。
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

.brand-meta-line {
  font-size: 13px;
  line-height: 1.6;
  opacity: 0.92;
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

.about-dialog {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.about-dialog__header {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 4px 0 12px;
  border-bottom: 1px solid #e5ecf4;
}

.about-dialog__logo-wrap {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  background: #f6f9fd;
  border: 1px solid #d7e3f2;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.about-dialog__logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}

.about-dialog__logo-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 700;
  color: #0b4ea2;
}

.about-dialog__title-group {
  min-width: 0;
}

.about-dialog__title {
  font-size: 24px;
  font-weight: 700;
  color: #123f78;
}

.about-dialog__subtitle {
  margin-top: 6px;
  font-size: 15px;
  color: #4f6888;
}

.about-dialog__desc {
  margin-top: 10px;
  line-height: 1.8;
  color: #5a6f8d;
}

.about-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.about-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  background: #f8fbff;
  border: 1px solid #dbe7f5;
  border-radius: 10px;
}

.about-item span {
  font-size: 13px;
  color: #70849e;
}

.about-item strong {
  font-size: 15px;
  color: #153e76;
  line-height: 1.6;
}

.about-item--wide {
  grid-column: 1 / -1;
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

@media (max-width: 768px) {
  .about-dialog__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .about-grid {
    grid-template-columns: 1fr;
  }
}
</style>
