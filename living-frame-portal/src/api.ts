import JSONbigFactory from 'json-bigint'
import { getApiBase } from './apiBase'
import { clearAllDrafts } from './drafts'
import { clearToken, getToken } from './storage'

const JSONbig = JSONbigFactory({ storeAsString: true })

async function request(path: string, options: any = {}): Promise<any> {
  const headers: Record<string, string> = { ...(options.headers || {}) }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  const response = await fetch(`${getApiBase()}${path}`, {
    ...options,
    headers,
  })
  let body: any = null
  try {
    const text = await response.text()
    body = text ? JSONbig.parse(text) : null
  } catch {
    body = null
  }
  if (response.status === 401) {
    clearAllDrafts()
    clearToken()
    throw new Error(body?.message || '登录已过期')
  }
  if (!response.ok || (body && typeof body.code === 'number' && body.code !== 200)) {
    throw new Error(body?.message || `请求失败（${response.status}）`)
  }
  return body?.data
}

function query(params: Record<string, unknown> = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  })
  const value = search.toString()
  return value ? `?${value}` : ''
}

export const portalApi = {
  captcha() {
    return request('/auth/captcha')
  },
  login(payload) {
    return request('/biz/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  me() {
    return request('/biz/auth/me')
  },
  logout() {
    return request('/biz/auth/logout', { method: 'POST' })
  },
  heartbeat() {
    return request('/biz/auth/heartbeat', { method: 'POST' })
  },
  changePassword(payload) {
    return request('/biz/auth/password', {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  updateProfile(payload) {
    return request('/biz/auth/profile', {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  uploadAvatar(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request('/system/files/avatar', {
      method: 'POST',
      body: formData,
    })
  },
  notifications(filter = 'all') {
    return request(`/biz/notifications?filter=${encodeURIComponent(filter)}`)
  },
  unreadNotificationCount() {
    return request('/biz/notifications/unread-count')
  },
  markNotificationRead(id) {
    return request(`/biz/notifications/${id}/read`, { method: 'POST' })
  },
  markAllNotificationsRead() {
    return request('/biz/notifications/read-all', { method: 'POST' })
  },
  deleteNotifications(ids) {
    return request('/biz/notifications/batch', {
      method: 'DELETE',
      body: JSON.stringify(ids),
    })
  },
  tickets() {
    return request('/biz/cs/tickets').then((list) => {
      if (!Array.isArray(list)) return []
      const active = list.find((ticket) => !['ARCHIVED', 'DRAFT'].includes(ticket.status))
      const archived = list.filter((ticket) => ticket.status === 'ARCHIVED')
      return active ? [active, ...archived] : archived
    })
  },
  ticketDetail(id) {
    return request(`/biz/cs/tickets/${id}`)
  },
  markTicketRead(id) {
    return request(`/biz/cs/tickets/${id}/read`, { method: 'POST' })
  },
  leaveTicket(id) {
    return request(`/biz/cs/tickets/${id}/leave`, { method: 'POST' })
  },
  chat(payload) {
    return request('/biz/cs/tickets/chat', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  sendMessage(id, content) {
    return request(`/biz/cs/tickets/${id}/messages`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    })
  },
  feedback(id, payload) {
    return request(`/biz/cs/tickets/${id}/feedback`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  confirmComplete(id) {
    return request(`/biz/cs/tickets/${id}/confirm-complete`, { method: 'POST' })
  },
  rejectComplete(id) {
    return request(`/biz/cs/tickets/${id}/reject-complete`, { method: 'POST' })
  },
  deleteDraft(id) {
    return request(`/biz/cs/tickets/${id}`, { method: 'DELETE' })
  },
  submitDraft(id) {
    return request(`/biz/cs/tickets/${id}/submit`, { method: 'POST' })
  },
  draft(payload) {
    return request('/biz/cs/tickets/drafts', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  updateDraft(id, payload) {
    return request(`/biz/cs/tickets/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  query,
}
