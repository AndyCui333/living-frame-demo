import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMe, getMenus, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginPayload, LoginResult, MeInfo, MenuNode } from '@/types/api'
import { getToken, removeToken, setToken } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref<MeInfo | null>(null)
  const menus = ref<MenuNode[]>([])
  const loading = ref(false)

  function withHomeMenu(menuTree: MenuNode[]): MenuNode[] {
    const hasHome = menuTree.some((item) => item.code === 'dashboard' || item.path === '/dashboard')
    const hasRanking = menuTree.some((item) => item.code === 'cs:ranking:list'
      || item.path === '/customer-service/ranking'
      || item.children?.some((child) => child.code === 'cs:ranking:list' || child.path === '/customer-service/ranking'))
    const next = [...menuTree]
    if (!hasHome) {
      next.unshift({
        id: 'dashboard-home',
        code: 'dashboard',
        name: '首页',
        type: 'MENU',
        parentId: '0',
        path: '/dashboard',
        sort: 0,
        status: 1,
        children: [],
      })
    }
    if (!hasRanking) {
      next.push({
        id: 'cs-ranking',
        code: 'cs:ranking:list',
        name: '客服排行',
        type: 'MENU',
        parentId: '0',
        path: '/customer-service/ranking',
        sort: 6,
        status: 1,
        children: [],
      })
    }
    return next
  }

  const displayName = computed(() => user.value?.nickname || user.value?.username || '未登录')
  const isSuperAdmin = computed(() => user.value?.superAdmin === true)
  const permissionSet = computed(() => new Set(user.value?.permissions ?? []))

  function hasPermission(code?: string): boolean {
    if (!code) {
      return true
    }
    return isSuperAdmin.value || permissionSet.value.has(code)
  }

  async function loadProfile(): Promise<void> {
    const [profile, menuTree] = await Promise.all([getMe(), getMenus()])
    user.value = profile
    menus.value = withHomeMenu(menuTree)
  }

  async function login(payload: LoginPayload): Promise<LoginResult> {
    loading.value = true
    try {
      const result = await loginApi(payload)
      token.value = result.token
      setToken(result.token)
      await loadProfile()
      return result
    } catch (error) {
      clearAuth()
      throw error
    } finally {
      loading.value = false
    }
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // token 已失效时也继续清理本地状态
    } finally {
      clearAuth()
    }
  }

  function clearAuth(): void {
    token.value = ''
    user.value = null
    menus.value = []
    removeToken()
  }

  return {
    token,
    user,
    menus,
    loading,
    displayName,
    isSuperAdmin,
    hasPermission,
    loadProfile,
    login,
    logout,
    clearAuth,
  }
})
