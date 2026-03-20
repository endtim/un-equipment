import { createRouter, createWebHashHistory } from 'vue-router'
import store from '../store'
import { getUserInfo } from '../api/auth'
import HomeLayout from '../views/layout/HomeLayout.vue'
import CenterLayout from '../views/layout/CenterLayout.vue'
import AdminLayout from '../views/layout/AdminLayout.vue'
import LoginPage from '../views/auth/LoginPage.vue'
import RegisterPage from '../views/auth/RegisterPage.vue'
import HomePage from '../views/home/HomePage.vue'
import InstrumentListPage from '../views/instrument/InstrumentListPage.vue'
import InstrumentDetailPage from '../views/instrument/InstrumentDetailPage.vue'
import MyOrdersPage from '../views/order/MyOrdersPage.vue'
import AccountPage from '../views/account/AccountPage.vue'
import MessageCenterPage from '../views/message/MessageCenterPage.vue'
import AdminDashboardPage from '../views/admin/AdminDashboardPage.vue'
import AdminLogPage from '../views/admin/AdminLogPage.vue'
import AdminStatPage from '../views/admin/AdminStatPage.vue'
import PortalIntroPage from '../views/portal/PortalIntroPage.vue'
import PlatformMembersPage from '../views/portal/PlatformMembersPage.vue'
import NoticesPage from '../views/portal/NoticesPage.vue'
import HelpCenterPage from '../views/portal/HelpCenterPage.vue'
import ContentDetailPage from '../views/portal/ContentDetailPage.vue'
import CenterDashboardPage from '../views/center/CenterDashboardPage.vue'
import AdminUsersPage from '../views/admin/system/AdminUsersPage.vue'
import AdminRolesPage from '../views/admin/system/AdminRolesPage.vue'
import AdminDepartmentsPage from '../views/admin/system/AdminDepartmentsPage.vue'
import AdminCategoriesPage from '../views/admin/instrument/AdminCategoriesPage.vue'
import AdminInstrumentsPage from '../views/admin/instrument/AdminInstrumentsPage.vue'
import AdminOpenRulesPage from '../views/admin/instrument/AdminOpenRulesPage.vue'
import AdminMachineOrdersPage from '../views/admin/orders/AdminMachineOrdersPage.vue'
import AdminSampleOrdersPage from '../views/admin/orders/AdminSampleOrdersPage.vue'
import AdminRechargeAuditPage from '../views/admin/finance/AdminRechargeAuditPage.vue'
import AdminFinanceDetailPage from '../views/admin/finance/AdminFinanceDetailPage.vue'
import AdminSettlementPage from '../views/admin/finance/AdminSettlementPage.vue'
import AdminNoticesPage from '../views/admin/content/AdminNoticesPage.vue'
import AdminHelpDocsPage from '../views/admin/content/AdminHelpDocsPage.vue'

const ADMIN_CORE_ROLES = ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
let loadingUserPromise = null

const router = createRouter({
  history: createWebHashHistory(),
  scrollBehavior() {
    return { left: 0, top: 0 }
  },
  routes: [
    {
      path: '/login',
      component: LoginPage,
      meta: { title: '用户登录', pageType: 'entry', showBreadcrumb: false }
    },
    {
      path: '/register',
      component: RegisterPage,
      meta: { title: '校外用户注册', pageType: 'entry', showBreadcrumb: false }
    },
    {
      path: '/',
      component: HomeLayout,
      children: [
        { path: '', redirect: '/home' },
        {
          path: 'home',
          component: HomePage,
          meta: { title: '首页', pageType: 'landing', showBreadcrumb: false }
        },
        { path: 'service', redirect: '/instruments' },
        {
          path: 'platform-members',
          component: PlatformMembersPage,
          meta: { title: '平台成员', pageType: 'section', breadcrumb: ['首页', '平台成员'] }
        },
        {
          path: 'platform-intro',
          component: PortalIntroPage,
          meta: { title: '平台简介', pageType: 'reading', breadcrumb: ['首页', '平台简介'] }
        },
        {
          path: 'notices',
          component: NoticesPage,
          meta: { title: '通知公告', pageType: 'reading', breadcrumb: ['首页', '通知公告'] }
        },
        {
          path: 'notices/:id',
          component: ContentDetailPage,
          props: { contentType: 'notice' },
          meta: { title: '正文', pageType: 'reading', breadcrumb: ['首页', '通知公告', '正文'] }
        },
        {
          path: 'help-center',
          component: HelpCenterPage,
          meta: { title: '帮助中心', pageType: 'reading', breadcrumb: ['首页', '帮助中心'] }
        },
        {
          path: 'help-center/:id',
          component: ContentDetailPage,
          props: { contentType: 'help' },
          meta: { title: '正文', pageType: 'reading', breadcrumb: ['首页', '帮助中心', '正文'] }
        },
        {
          path: 'instruments',
          component: InstrumentListPage,
          meta: { title: '预约服务', pageType: 'operation', breadcrumb: ['首页', '预约服务'] }
        },
        {
          path: 'instruments/:id',
          component: InstrumentDetailPage,
          props: true,
          meta: {
            title: '仪器详情',
            pageType: 'detail',
            breadcrumb: ['首页', '预约服务', '仪器详情']
          }
        }
      ]
    },
    {
      path: '/center',
      component: CenterLayout,
      meta: { auth: true },
      children: [
        {
          path: '',
          component: CenterDashboardPage,
          meta: { title: '用户中心', pageType: 'entry', showBreadcrumb: false }
        },
        {
          path: 'account',
          component: AccountPage,
          props: { mode: 'account' },
          meta: { title: '账户中心', pageType: 'operation', breadcrumb: ['用户中心', '账户中心'] }
        },
        {
          path: 'recharge',
          component: AccountPage,
          props: { mode: 'recharge' },
          meta: { title: '充值申请', pageType: 'operation', breadcrumb: ['用户中心', '充值申请'] }
        },
        {
          path: 'messages',
          component: MessageCenterPage,
          meta: { title: '消息中心', pageType: 'reading', breadcrumb: ['用户中心', '消息中心'] }
        },
        {
          path: 'orders',
          component: MyOrdersPage,
          meta: { title: '我的订单', pageType: 'operation', breadcrumb: ['用户中心', '我的订单'] }
        },
        {
          path: 'machine-orders',
          component: MyOrdersPage,
          props: { orderType: 'MACHINE' },
          meta: { title: '上机预约', pageType: 'operation', breadcrumb: ['用户中心', '上机预约'] }
        },
        {
          path: 'sample-orders',
          component: MyOrdersPage,
          props: { orderType: 'SAMPLE' },
          meta: { title: '送样预约', pageType: 'operation', breadcrumb: ['用户中心', '送样预约'] }
        }
      ]
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { auth: true, admin: true, roles: ADMIN_CORE_ROLES },
      children: [
        {
          path: '',
          component: AdminDashboardPage,
          meta: {
            title: '管理平台',
            pageType: 'entry',
            showBreadcrumb: false,
            roles: ADMIN_CORE_ROLES
          }
        },
        {
          path: 'users',
          component: AdminUsersPage,
          meta: {
            title: '用户管理',
            description:
              '管理用户账号、角色归属、审核状态与启用禁用。校外注册用户在此完成审核闭环。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '系统管理', '用户管理'],
            roles: ['ADMIN', 'DEPT_MANAGER']
          }
        },
        {
          path: 'roles',
          component: AdminRolesPage,
          meta: {
            title: '角色管理',
            description: '维护平台角色及其权限边界，保证系统最小权限原则。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '系统管理', '角色管理'],
            roles: ['ADMIN']
          }
        },
        {
          path: 'departments',
          component: AdminDepartmentsPage,
          meta: {
            title: '部门管理',
            description: '维护组织架构与部门基础信息，为权限范围和统计口径提供基础。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '系统管理', '部门管理'],
            roles: ['ADMIN']
          }
        },
        {
          path: 'categories',
          component: AdminCategoriesPage,
          meta: {
            title: '仪器分类',
            description: '维护仪器分类体系，统一仪器检索与统计维度。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '仪器管理', '仪器分类'],
            roles: ['ADMIN']
          }
        },
        {
          path: 'instruments',
          component: AdminInstrumentsPage,
          meta: {
            title: '仪器管理',
            description: '维护仪器档案、价格、开放模式与运行状态，保障预约与结算口径一致。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '仪器管理', '仪器管理'],
            roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
          }
        },
        {
          path: 'open-rules',
          component: AdminOpenRulesPage,
          meta: {
            title: '开放规则',
            description: '配置仪器可预约星期与时段，支持多星期规则与生效区间控制。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '仪器管理', '开放规则'],
            roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
          }
        },
        {
          path: 'orders/machine',
          component: AdminMachineOrdersPage,
          meta: {
            title: '上机订单',
            description: '管理上机预约全流程：审核、签到、结束、结算与关闭，确保状态流转可追踪。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '业务处理', '上机订单'],
            roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
          }
        },
        {
          path: 'orders/sample',
          component: AdminSampleOrdersPage,
          meta: {
            title: '送样订单',
            description: '管理送样预约全流程：审核、接样、结果上传、结算与关闭。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '业务处理', '送样订单'],
            roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
          }
        },
        {
          path: 'recharges',
          component: AdminRechargeAuditPage,
          meta: {
            title: '充值审核',
            description: '处理充值单审核与双人复核，确保资金入账流程可审计、可追溯。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '业务处理', '充值审核'],
            roles: ['ADMIN', 'DEPT_MANAGER']
          }
        },
        { path: 'finance', redirect: '/admin/finance/report' },
        {
          path: 'finance/report',
          component: AdminFinanceDetailPage,
          props: { mode: 'report' },
          meta: {
            title: '经费明细报表',
            description: '汇总充值、仪器收费、退款与维护支出，支持筛选查询与CSV导出。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '经费管理', '经费明细报表'],
            roles: ['ADMIN', 'DEPT_MANAGER']
          }
        },
        {
          path: 'finance/expenses',
          component: AdminFinanceDetailPage,
          props: { mode: 'expense' },
          meta: {
            title: '维护支出登记',
            description: '登记仪器维护/维修/校准支出，并查看支出流水。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '经费管理', '维护支出登记'],
            roles: ['ADMIN', 'DEPT_MANAGER']
          }
        },
        {
          path: 'finance/budget-warnings',
          component: AdminFinanceDetailPage,
          props: { mode: 'budget-warning' },
          meta: {
            title: '预算预警',
            description: '查看预算执行预警信息，支持按年度筛选。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '经费管理', '预算预警'],
            roles: ['ADMIN', 'DEPT_MANAGER']
          }
        },
        {
          path: 'finance/budget-ledger',
          component: AdminFinanceDetailPage,
          props: { mode: 'budget-ledger' },
          meta: {
            title: '预算台账',
            description: '维护预算台账并支持新增预算。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '经费管理', '预算台账'],
            roles: ['ADMIN', 'DEPT_MANAGER']
          }
        },
        { path: 'finance/budgets', redirect: '/admin/finance/budget-ledger' },
        { path: 'finance-report', redirect: '/admin/finance/report' },
        { path: 'finance-expenses', redirect: '/admin/finance/expenses' },
        { path: 'finance-budgets', redirect: '/admin/finance/budget-ledger' },
        { path: 'finance-details', redirect: '/admin/finance/report' },
        { path: 'settlements', redirect: '/admin/settlements/overview' },
        {
          path: 'settlements/overview',
          component: AdminSettlementPage,
          props: { mode: 'settlement' },
          meta: {
            title: '结算总览',
            description: '管理结算、退款等业务，统一资金与订单状态的一致性。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '业务处理', '结算管理', '结算总览'],
            roles: ['ADMIN', 'DEPT_MANAGER']
          }
        },
        {
          path: 'settlements/anomalies',
          component: AdminSettlementPage,
          props: { mode: 'anomaly' },
          meta: {
            title: '异常账处理',
            description: '查看异常账清单并维护处理状态，保障对账闭环。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '业务处理', '结算管理', '异常账处理'],
            roles: ['ADMIN', 'DEPT_MANAGER']
          }
        },
        { path: 'settlement-anomalies', redirect: '/admin/settlements/anomalies' },
        {
          path: 'notices',
          component: AdminNoticesPage,
          meta: {
            title: '公告管理',
            description: '发布平台通知与运营公告，统一对外信息展示。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '内容管理', '公告管理'],
            roles: ['ADMIN']
          }
        },
        {
          path: 'help-docs',
          component: AdminHelpDocsPage,
          meta: {
            title: '帮助文档',
            description: '维护操作指南与流程文档，提升用户自助能力与流程规范性。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '内容管理', '帮助文档'],
            roles: ['ADMIN']
          }
        },
        {
          path: 'stats',
          component: AdminStatPage,
          meta: {
            title: '统计报表',
            description: '按时间区间查看运营指标、趋势、排行和分布信息。',
            pageType: 'reading',
            breadcrumb: ['管理平台', '工作台', '统计报表'],
            roles: ['ADMIN', 'INSTRUMENT_OWNER', 'DEPT_MANAGER']
          }
        },
        {
          path: 'logs',
          component: AdminLogPage,
          meta: {
            title: '日志审计',
            description: '追踪后台关键操作轨迹，支持按模块和关键字快速检索。',
            pageType: 'reading',
            breadcrumb: ['管理平台', '工作台', '日志审计'],
            roles: ['ADMIN']
          }
        }
      ]
    },
    { path: '/account', redirect: '/center/account' },
    { path: '/orders', redirect: '/center/orders' },
    { path: '/messages', redirect: '/center/messages' },
    { path: '/college-platforms', redirect: '/home' },
    { path: '/demands', redirect: '/home' },
    { path: '/admin/orders', redirect: '/admin/orders/machine' },
    { path: '/admin/finance', redirect: '/admin/finance/report' },
    { path: '/admin/system', redirect: '/admin/users' },
    { path: '/admin/content', redirect: '/admin/notices' }
  ]
})

function resolveRequiredRoles(to) {
  const roles = to.matched
    .map((record) => record.meta?.roles)
    .filter((item) => Array.isArray(item) && item.length > 0)
  if (roles.length === 0) {
    return null
  }
  return roles[roles.length - 1]
}

async function ensureUserLoaded() {
  const token = store.state.token
  if (!token || store.state.user) {
    return true
  }
  if (!loadingUserPromise) {
    loadingUserPromise = getUserInfo()
      .then((user) => {
        store.commit('setAuth', {
          token,
          user
        })
        return true
      })
      .catch(() => {
        store.commit('clearAuth')
        return false
      })
      .finally(() => {
        loadingUserPromise = null
      })
  }
  return loadingUserPromise
}

router.beforeEach(async (to) => {
  const loaded = await ensureUserLoaded()
  if (!loaded) {
    if (to.path === '/login') {
      return true
    }
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const user = store.state.user
  const token = store.state.token
  if (to.matched.some((record) => record.meta.auth) && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if ((to.path === '/login' || to.path === '/register') && token) {
    return '/home'
  }
  if (
    to.matched.some((record) => record.meta.admin) &&
    (!user || ['INTERNAL_USER', 'EXTERNAL_USER'].includes(user.roleCode))
  ) {
    return '/home'
  }

  const requiredRoles = resolveRequiredRoles(to)
  if (requiredRoles && (!user || !requiredRoles.includes(user.roleCode))) {
    return '/home'
  }

  return true
})

export default router
