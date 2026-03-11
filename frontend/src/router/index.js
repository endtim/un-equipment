import { createRouter, createWebHashHistory } from 'vue-router'
import store from '../store'
import HomeLayout from '../views/layout/HomeLayout.vue'
import CenterLayout from '../views/layout/CenterLayout.vue'
import AdminLayout from '../views/layout/AdminLayout.vue'
import LoginPage from '../views/auth/LoginPage.vue'
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
import CenterDashboardPage from '../views/center/CenterDashboardPage.vue'
import AdminUsersPage from '../views/admin/system/AdminUsersPage.vue'
import AdminRolesPage from '../views/admin/system/AdminRolesPage.vue'
import AdminDepartmentsPage from '../views/admin/system/AdminDepartmentsPage.vue'
import AdminCategoriesPage from '../views/admin/instrument/AdminCategoriesPage.vue'
import AdminInstrumentsPage from '../views/admin/instrument/AdminInstrumentsPage.vue'
import AdminMachineOrdersPage from '../views/admin/orders/AdminMachineOrdersPage.vue'
import AdminSampleOrdersPage from '../views/admin/orders/AdminSampleOrdersPage.vue'
import AdminRechargeAuditPage from '../views/admin/finance/AdminRechargeAuditPage.vue'
import AdminSettlementPage from '../views/admin/finance/AdminSettlementPage.vue'
import AdminNoticesPage from '../views/admin/content/AdminNoticesPage.vue'
import AdminHelpDocsPage from '../views/admin/content/AdminHelpDocsPage.vue'

const router = createRouter({
  history: createWebHashHistory(),
  scrollBehavior() {
    return { left: 0, top: 0 }
  },
  routes: [
    { path: '/login', component: LoginPage, meta: { title: '用户登录', pageType: 'entry', showBreadcrumb: false } },
    {
      path: '/',
      component: HomeLayout,
      children: [
        { path: '', redirect: '/home' },
        { path: 'home', component: HomePage, meta: { title: '首页', pageType: 'landing', showBreadcrumb: false } },
        { path: 'service', redirect: '/instruments' },
        {
          path: 'platform-members',
          component: PlatformMembersPage,
          meta: {
            title: '平台成员',
            description: '展示平台主管、仪器负责人和开放共享服务团队。',
            pageType: 'section',
            breadcrumb: ['首页', '平台成员']
          }
        },
        {
          path: 'platform-intro',
          component: PortalIntroPage,
          meta: {
            title: '平台简介',
            description: '介绍平台建设目标、服务对象与开放共享运行机制。',
            pageType: 'reading',
            breadcrumb: ['首页', '平台简介']
          }
        },
        {
          path: 'college-platforms',
          component: PortalIntroPage,
          meta: {
            title: '院级平台',
            description: '查看院级平台开放信息和服务入口。',
            pageType: 'reading',
            breadcrumb: ['首页', '院级平台']
          }
        },
        {
          path: 'demands',
          component: PortalIntroPage,
          meta: {
            title: '需求发布',
            description: '发布仪器测试需求和服务咨询信息。',
            pageType: 'reading',
            breadcrumb: ['首页', '需求发布']
          }
        },
        {
          path: 'notices',
          component: NoticesPage,
          meta: {
            title: '通知公告',
            description: '查看平台制度通知、业务公告与仪器服务动态。',
            pageType: 'reading',
            breadcrumb: ['首页', '通知公告']
          }
        },
        {
          path: 'help-center',
          component: HelpCenterPage,
          meta: {
            title: '帮助中心',
            description: '查阅预约流程、充值说明、送样规范和常见问题。',
            pageType: 'reading',
            breadcrumb: ['首页', '帮助中心']
          }
        },
        {
          path: 'instruments',
          component: InstrumentListPage,
          meta: {
            title: '预约服务',
            description: '按分类、状态和关键词检索大型仪器，进入预约或送样流程。',
            pageType: 'operation',
            breadcrumb: ['首页', '预约服务']
          }
        },
        {
          path: 'instruments/:id',
          component: InstrumentDetailPage,
          props: true,
          meta: {
            title: '仪器详情',
            description: '查看仪器开放规则、收费标准、附件资料和预约入口。',
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
        { path: '', component: CenterDashboardPage, meta: { title: '用户中心', pageType: 'entry', showBreadcrumb: false } },
        {
          path: 'account',
          component: AccountPage,
          meta: {
            title: '账户中心',
            description: '查看余额、流水和充值记录，办理充值申请。',
            pageType: 'operation',
            breadcrumb: ['用户中心', '账户中心']
          }
        },
        {
          path: 'recharge',
          component: AccountPage,
          meta: {
            title: '充值申请',
            description: '提交充值申请并查看充值审核进度。',
            pageType: 'operation',
            breadcrumb: ['用户中心', '充值申请']
          }
        },
        {
          path: 'messages',
          component: MessageCenterPage,
          meta: {
            title: '消息中心',
            description: '集中查看订单状态、充值结果和系统通知消息。',
            pageType: 'reading',
            breadcrumb: ['用户中心', '消息中心']
          }
        },
        {
          path: 'orders',
          component: MyOrdersPage,
          meta: {
            title: '我的订单',
            description: '查看全部预约记录，跟踪审核、使用与结算状态。',
            pageType: 'operation',
            breadcrumb: ['用户中心', '我的订单']
          }
        },
        {
          path: 'machine-orders',
          component: MyOrdersPage,
          props: { orderType: 'MACHINE' },
          meta: {
            title: '上机预约',
            description: '查看个人上机预约订单及流转进度。',
            pageType: 'operation',
            breadcrumb: ['用户中心', '上机预约']
          }
        },
        {
          path: 'sample-orders',
          component: MyOrdersPage,
          props: { orderType: 'SAMPLE' },
          meta: {
            title: '送样预约',
            description: '查看个人送样订单、接样状态和结果信息。',
            pageType: 'operation',
            breadcrumb: ['用户中心', '送样预约']
          }
        }
      ]
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { auth: true, admin: true },
      children: [
        { path: '', component: AdminDashboardPage, meta: { title: '管理平台', pageType: 'entry', showBreadcrumb: false } },
        {
          path: 'users',
          component: AdminUsersPage,
          meta: {
            title: '用户管理',
            description: '维护平台用户、角色绑定和账户启停状态。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '系统管理', '用户管理']
          }
        },
        {
          path: 'roles',
          component: AdminRolesPage,
          meta: {
            title: '角色管理',
            description: '维护角色基础信息和编码规则。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '系统管理', '角色管理']
          }
        },
        {
          path: 'departments',
          component: AdminDepartmentsPage,
          meta: {
            title: '部门管理',
            description: '维护部门层级、基础信息和组织归属。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '系统管理', '部门管理']
          }
        },
        {
          path: 'categories',
          component: AdminCategoriesPage,
          meta: {
            title: '仪器分类',
            description: '维护大型仪器分类体系和前台展示分组。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '仪器管理', '仪器分类']
          }
        },
        {
          path: 'instruments',
          component: AdminInstrumentsPage,
          meta: {
            title: '仪器管理',
            description: '维护仪器档案、开放状态和收费信息。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '仪器管理', '仪器管理']
          }
        },
        {
          path: 'orders/machine',
          component: AdminMachineOrdersPage,
          meta: {
            title: '上机订单',
            description: '处理上机预约审核、签到、完成与结算。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '业务处理', '上机订单']
          }
        },
        {
          path: 'orders/sample',
          component: AdminSampleOrdersPage,
          meta: {
            title: '送样订单',
            description: '处理送样接收、结果上传和订单结算流程。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '业务处理', '送样订单']
          }
        },
        {
          path: 'recharges',
          component: AdminRechargeAuditPage,
          meta: {
            title: '充值审核',
            description: '审核用户充值申请并同步账户余额变更。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '业务处理', '充值审核']
          }
        },
        {
          path: 'settlements',
          component: AdminSettlementPage,
          meta: {
            title: '结算管理',
            description: '查看服务结算结果和后续财务处理信息。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '业务处理', '结算管理']
          }
        },
        {
          path: 'notices',
          component: AdminNoticesPage,
          meta: {
            title: '公告管理',
            description: '维护平台通知公告的发布、编辑和展示状态。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '内容管理', '公告管理']
          }
        },
        {
          path: 'help-docs',
          component: AdminHelpDocsPage,
          meta: {
            title: '帮助文档',
            description: '维护预约说明、模板下载和帮助资料。',
            pageType: 'operation',
            breadcrumb: ['管理平台', '内容管理', '帮助文档']
          }
        },
        {
          path: 'stats',
          component: AdminStatPage,
          meta: {
            title: '统计报表',
            description: '查看平台总览、排行和趋势等运行数据。',
            pageType: 'reading',
            breadcrumb: ['管理平台', '工作台', '统计报表']
          }
        },
        {
          path: 'logs',
          component: AdminLogPage,
          meta: {
            title: '日志审计',
            description: '查询关键业务操作日志和审计记录。',
            pageType: 'reading',
            breadcrumb: ['管理平台', '工作台', '日志审计']
          }
        }
      ]
    },
    { path: '/account', redirect: '/center/account' },
    { path: '/orders', redirect: '/center/orders' },
    { path: '/messages', redirect: '/center/messages' },
    { path: '/admin/orders', redirect: '/admin/orders/machine' },
    { path: '/admin/finance', redirect: '/admin/recharges' },
    { path: '/admin/system', redirect: '/admin/users' },
    { path: '/admin/content', redirect: '/admin/notices' }
  ]
})

router.beforeEach((to, from, next) => {
  const user = store.state.user
  const token = store.state.token
  if (to.matched.some(record => record.meta.auth) && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.path === '/login' && token) {
    next('/home')
    return
  }
  if (to.matched.some(record => record.meta.admin) &&
    (!user || ['INTERNAL_USER', 'EXTERNAL_USER'].includes(user.roleCode))) {
    next('/home')
    return
  }
  next()
})

export default router
