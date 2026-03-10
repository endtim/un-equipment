<template>
  <div class="portal-home">
    <section class="hero-section">
      <div class="hero-card banner-card">
        <div class="banner-mask">
          <div class="banner-title">大型仪器共享平台</div>
          <div class="banner-subtitle">面向教学科研的一站式预约与送样服务</div>
          <div class="banner-actions">
            <el-button type="primary" @click="$router.push('/instruments')">进入预约服务</el-button>
            <el-button @click="$router.push('/help-center')">查看帮助中心</el-button>
          </div>
        </div>
      </div>

      <div class="content-card login-card">
        <div class="panel-title">平台用户入口</div>
        <template v-if="user">
          <div class="user-name">{{ user.realName }}</div>
          <div class="user-line"><span>账号</span><span>{{ user.username }}</span></div>
          <div class="user-line"><span>角色</span><span>{{ roleLabel }}</span></div>
          <div class="user-line"><span>部门</span><span>{{ user.departmentName || '-' }}</span></div>
          <el-button type="primary" class="full" @click="$router.push('/center')">进入用户中心</el-button>
          <el-button v-if="isAdmin" class="full" @click="$router.push('/admin')">进入管理平台</el-button>
        </template>
        <template v-else>
          <div class="login-text">请登录后进行预约、送样、充值和订单跟踪。</div>
          <el-button type="primary" class="full" @click="$router.push('/login')">登录平台</el-button>
        </template>
      </div>
    </section>

    <section class="grid-3">
      <div class="content-card panel">
        <div class="panel-head">
          <h3>通知公告</h3>
          <span @click="$router.push('/notices')">更多</span>
        </div>
        <div v-for="item in noticeList" :key="item.id || item.title" class="list-row">
          <span class="list-title">{{ item.title }}</span>
          <span class="list-time">{{ formatDate(item.publishTime || item.createTime) }}</span>
        </div>
      </div>
      <div class="content-card panel">
        <div class="panel-head">
          <h3>服务指南</h3>
          <span @click="$router.push('/help-center')">更多</span>
        </div>
        <div v-for="item in helpDocList" :key="item.id || item.title" class="list-row">
          <span class="list-title">{{ item.title }}</span>
          <span class="list-time">指南</span>
        </div>
      </div>
      <div class="content-card panel">
        <div class="panel-head">
          <h3>仪器实时情况</h3>
        </div>
        <div class="stats-row"><span>在册仪器</span><strong>{{ overview.instrumentCount || 0 }}</strong></div>
        <div class="stats-row"><span>累计预约</span><strong>{{ overview.reservationCount || 0 }}</strong></div>
        <div class="stats-row"><span>完成服务</span><strong>{{ overview.completionCount || 0 }}</strong></div>
        <div class="stats-row"><span>服务收入</span><strong>{{ formatAmount(overview.incomeAmount) }}</strong></div>
      </div>
    </section>

    <section class="content-card dashboard">
      <div class="dashboard-title">平台统计看板</div>
      <div class="dashboard-grid">
        <div class="dashboard-item">
          <div class="num">{{ overview.instrumentCount || 0 }}</div>
          <div class="label">在册仪器</div>
        </div>
        <div class="dashboard-item">
          <div class="num">{{ overview.reservationCount || 0 }}</div>
          <div class="label">累计预约</div>
        </div>
        <div class="dashboard-item">
          <div class="num">{{ overview.completionCount || 0 }}</div>
          <div class="label">已完成服务</div>
        </div>
        <div class="dashboard-item">
          <div class="num">{{ overview.departmentCount || 0 }}</div>
          <div class="label">覆盖部门</div>
        </div>
      </div>
    </section>

    <section class="grid-3">
      <div class="content-card panel">
        <div class="panel-head"><h3>仪器预约排行</h3></div>
        <div v-for="(item, index) in instrumentRanking" :key="item.name" class="rank-row">
          <span class="rank-index">{{ index + 1 }}</span>
          <span class="rank-name">{{ item.name }}</span>
          <span>{{ item.value }} 次</span>
        </div>
      </div>
      <div class="content-card panel">
        <div class="panel-head"><h3>用户预约排行</h3></div>
        <div v-for="(item, index) in userRanking" :key="item.name" class="rank-row">
          <span class="rank-index">{{ index + 1 }}</span>
          <span class="rank-name">{{ item.name }}</span>
          <span>{{ item.value }} 单</span>
        </div>
      </div>
      <div class="content-card panel">
        <div class="panel-head"><h3>最新预约信息</h3></div>
        <div v-for="item in latestReservations" :key="item.id" class="latest-row">
          <div>{{ item.instrument }}</div>
          <div class="list-time">{{ item.user }} · {{ item.time }}</div>
        </div>
      </div>
    </section>

    <section>
      <div class="panel-head" style="margin-bottom: 12px;"><h3>热门仪器</h3><span @click="$router.push('/instruments')">更多</span></div>
      <div class="hot-grid">
        <div v-for="item in hotInstruments" :key="item.id || item.instrumentName" class="content-card hot-card" @click="openInstrument(item)">
          <div class="hot-name">{{ item.instrumentName }}</div>
          <div class="hot-meta">{{ item.categoryName || '-' }} · {{ item.location || '-' }}</div>
          <div class="hot-price">
            <span>校内 {{ formatAmount(item.priceInternal || item.machinePricePerHour) }}</span>
            <span>校外 {{ formatAmount(item.priceExternal || item.samplePricePerItem) }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { getNotices, getHelpDocs } from '../../api/content'
import { getInstruments } from '../../api/instrument'
import { getOverview } from '../../api/stat'

export default {
  data() {
    return {
      overview: {},
      notices: [],
      helpDocs: [],
      instruments: [],
      fallbackUserRanking: [
        { name: '王老师', value: 18 },
        { name: '化学学院测试中心', value: 15 },
        { name: '张老师', value: 12 },
        { name: '陈同学', value: 10 },
        { name: '材料学院开放平台', value: 9 }
      ],
      fallbackReservations: [
        { id: 1, instrument: '超高效液相色谱仪', user: '药学院 李老师', time: '今天 14:20' },
        { id: 2, instrument: '扫描电子显微镜', user: '材料学院 陈同学', time: '今天 13:40' },
        { id: 3, instrument: '激光共聚焦显微镜', user: '生命学院 王老师', time: '今天 11:05' },
        { id: 4, instrument: '高分辨质谱仪', user: '食品学院 赵老师', time: '今天 09:30' },
        { id: 5, instrument: 'X 射线衍射仪', user: '理学院 孙同学', time: '昨天 16:45' }
      ]
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    },
    isAdmin() {
      return this.user && !['INTERNAL_USER', 'EXTERNAL_USER'].includes(this.user.roleCode)
    },
    roleLabel() {
      const mapping = {
        ADMIN: '平台管理员',
        INSTRUMENT_OWNER: '仪器负责人',
        DEPT_MANAGER: '部门管理员',
        INTERNAL_USER: '校内用户',
        EXTERNAL_USER: '校外用户'
      }
      return mapping[this.user?.roleCode] || '平台用户'
    },
    noticeList() {
      return (this.notices || []).slice(0, 6)
    },
    helpDocList() {
      return (this.helpDocs || []).slice(0, 6)
    },
    instrumentRanking() {
      if (Array.isArray(this.overview.topInstruments) && this.overview.topInstruments.length) {
        return this.overview.topInstruments.slice(0, 5).map(item => ({ name: item.key, value: item.value }))
      }
      return (this.instruments || []).slice(0, 5).map((item, index) => ({ name: item.instrumentName, value: 10 - index }))
    },
    userRanking() {
      return this.fallbackUserRanking
    },
    latestReservations() {
      return this.fallbackReservations
    },
    hotInstruments() {
      return (this.instruments || []).slice(0, 8)
    }
  },
  async created() {
    const [overview, notices, helpDocs, instruments] = await Promise.all([
      getOverview().catch(() => ({})),
      getNotices().catch(() => []),
      getHelpDocs().catch(() => []),
      getInstruments({ pageNum: 1, pageSize: 8 }).catch(() => [])
    ])
    this.overview = overview || {}
    this.notices = notices || []
    this.helpDocs = helpDocs || []
    this.instruments = Array.isArray(instruments?.list) ? instruments.list : (Array.isArray(instruments) ? instruments : [])
  },
  methods: {
    openInstrument(item) {
      if (item && item.id) {
        this.$router.push(`/instruments/${item.id}`)
      } else {
        this.$router.push('/instruments')
      }
    },
    formatDate(value) {
      return value ? String(value).slice(0, 10) : ''
    },
    formatAmount(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>

<style scoped>
.portal-home {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) 320px;
  gap: 18px;
}

.banner-card {
  min-height: 320px;
  background: linear-gradient(135deg, #0c4f9f, #2a7bd2);
  color: #fff;
  position: relative;
}

.banner-mask {
  padding: 46px 40px;
  max-width: 580px;
}

.banner-title {
  font-size: 42px;
  font-weight: 700;
}

.banner-subtitle {
  margin-top: 12px;
  line-height: 1.8;
}

.banner-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.login-card {
  padding: 18px;
}

.panel-title {
  height: 44px;
  line-height: 44px;
  margin: -18px -18px 16px;
  padding: 0 16px;
  background: #0b4ea2;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.user-name {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 10px;
}

.user-line {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #d9e2ef;
}

.login-text {
  color: #556a86;
  line-height: 1.8;
  margin-bottom: 14px;
}

.full {
  width: 100%;
  margin-top: 10px;
}

.panel {
  padding: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-head h3 {
  margin: 0;
  color: #17427e;
}

.panel-head span {
  color: #5679a8;
  cursor: pointer;
}

.list-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px dashed #dde5ef;
}

.list-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.list-time {
  color: #7c8ea6;
  font-size: 13px;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed #dde5ef;
}

.dashboard {
  padding: 20px;
}

.dashboard-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f3f7e;
  margin-bottom: 14px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-item {
  background: #f5f9ff;
  border: 1px solid #d9e6f7;
  padding: 16px;
}

.dashboard-item .num {
  font-size: 34px;
  font-weight: 700;
  color: #0c4e9f;
}

.dashboard-item .label {
  margin-top: 8px;
  color: #6f8198;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed #dde5ef;
}

.rank-index {
  width: 24px;
  text-align: center;
  color: #0b4ea2;
  font-weight: 700;
}

.rank-name {
  flex: 1;
}

.latest-row {
  padding: 10px 0;
  border-bottom: 1px dashed #dde5ef;
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.hot-card {
  padding: 14px;
  cursor: pointer;
}

.hot-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a4278;
}

.hot-meta {
  margin-top: 10px;
  color: #667c97;
  font-size: 13px;
}

.hot-price {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
  color: #0b4ea2;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .hero-section,
  .dashboard-grid,
  .hot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
