import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AdminLayout from '@/layout/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '首页', permission: 'dashboard' },
        },
        {
          path: 'system',
          redirect: '/system/user',
        },
        {
          path: 'system/user',
          name: 'SystemUser',
          component: () => import('@/views/system/UserView.vue'),
          meta: { title: '用户管理', permission: 'system:user:list' },
        },
        {
          path: 'system/role',
          name: 'SystemRole',
          component: () => import('@/views/system/RoleView.vue'),
          meta: { title: '角色管理', permission: 'system:role:list' },
        },
        {
          path: 'system/permission',
          name: 'SystemPermission',
          component: () => import('@/views/system/PermissionView.vue'),
          meta: { title: '权限管理', permission: 'system:permission:list' },
        },
        {
          path: 'system/login-log',
          name: 'SystemLoginLog',
          component: () => import('@/views/system/AuditLoginLogView.vue'),
          props: { scope: 'SYSTEM' },
          meta: { title: '系统登录日志', permission: 'system:login-log:list' },
        },
        {
          path: 'system/operation-log',
          name: 'SystemOperationLog',
          component: () => import('@/views/system/AuditOperationLogView.vue'),
          props: { scope: 'SYSTEM' },
          meta: { title: '系统操作日志', permission: 'system:operation-log:list' },
        },
        {
          path: 'system/blacklist',
          name: 'SystemBlacklist',
          component: () => import('@/views/system/BlacklistView.vue'),
          meta: { title: '系统黑名单', permission: 'system:blacklist:list' },
        },
        {
          path: 'system/file',
          name: 'SystemFile',
          component: () => import('@/views/system/FileView.vue'),
          meta: { title: '文件管理', permission: 'system:file:list' },
        },
        {
          path: 'business',
          redirect: '/business/user',
        },
        {
          path: 'business/user',
          name: 'BusinessUser',
          component: () => import('@/views/business/BusinessUserView.vue'),
          meta: { title: '业务用户', permission: 'business:user:list' },
        },
        {
          path: 'business/login-log',
          name: 'BusinessLoginLog',
          component: () => import('@/views/system/AuditLoginLogView.vue'),
          props: { scope: 'PORTAL' },
          meta: { title: '门户登录日志', permission: 'business:login-log:portal' },
        },
        {
          path: 'business/operation-log',
          name: 'BusinessOperationLog',
          component: () => import('@/views/system/AuditOperationLogView.vue'),
          props: { scope: 'PORTAL' },
          meta: { title: '门户操作日志', permission: 'business:operation-log:portal' },
        },
        {
          path: 'business/statistics',
          name: 'BusinessStatistics',
          component: () => import('@/views/business/BusinessStatisticsView.vue'),
          meta: { title: '用户统计', permission: 'business:statistics:list' },
        },
        {
          path: 'ai',
          redirect: '/ai/chat',
        },
        {
          path: 'ai/chat',
          name: 'AiChat',
          component: () => import('@/views/ai/AiChatView.vue'),
          meta: { title: 'AI 对话', permission: 'ai:chat' },
        },
        {
          path: 'ai/agent',
          name: 'AiAgent',
          component: () => import('@/views/ai/AiAgentView.vue'),
          meta: { title: 'AI Agent', permission: 'ai:agent' },
        },
        {
          path: 'ai/settings',
          name: 'AiSettings',
          component: () => import('@/views/ai/AiSettingsView.vue'),
          meta: { title: 'AI 设置', permission: 'ai:chat' },
        },
        {
          path: 'customer-service',
          redirect: '/customer-service/chat',
        },
        {
          path: 'customer-service/chat',
          name: 'CsChat',
          component: () => import('@/views/customerService/CsChatView.vue'),
          meta: { title: '客服聊天', permission: 'cs:chat:list' },
        },
        {
          path: 'customer-service/ticket',
          name: 'CsTicket',
          component: () => import('@/views/customerService/CsTicketView.vue'),
          meta: { title: '工单流程', permission: 'cs:ticket:list' },
        },
        {
          path: 'customer-service/ranking',
          name: 'CsRanking',
          component: () => import('@/views/customerService/CsRankingView.vue'),
          meta: { title: '客服排行' },
        },
      ],
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('@/views/error/ForbiddenView.vue'),
      meta: { public: true, title: '无权限' },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/NotFoundView.vue'),
      meta: { public: true, title: '页面不存在' },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (to.name === 'Login' && auth.token) {
      return { path: '/dashboard' }
    }
    return true
  }

  if (!auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!auth.user) {
    try {
      await auth.loadProfile()
    } catch {
      auth.clearAuth()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.permission && !auth.hasPermission(to.meta.permission)) {
    if (to.name === 'Dashboard') {
      const fallback = auth.firstMenuPath
      if (fallback && fallback !== to.fullPath) {
        return { path: fallback }
      }
    }
    return { path: '/403' }
  }

  return true
})

export default router
