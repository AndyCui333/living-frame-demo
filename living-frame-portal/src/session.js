import { reactive } from 'vue'
import { portalApi } from './api'
import { clearToken, getToken, setToken } from './storage'

export const session = reactive({
  token: getToken(),
  user: null,
  loading: false,
})

export async function loadProfile() {
  session.user = await portalApi.me()
  return session.user
}

export async function login(payload) {
  session.loading = true
  try {
    const result = await portalApi.login(payload)
    session.token = result.token
    setToken(result.token)
    await loadProfile()
    return result
  } catch (error) {
    clearToken()
    session.token = ''
    session.user = null
    throw error
  } finally {
    session.loading = false
  }
}

export async function logout() {
  try {
    await portalApi.logout()
  } catch {
    // 忽略退出接口异常，仍然清理本地会话
  } finally {
    session.token = ''
    session.user = null
    clearToken()
  }
}
