import JSONbigFactory from 'json-bigint'
import { clearToken, getToken } from './storage'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const JSONbig = JSONbigFactory({ storeAsString: true })

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  const response = await fetch(`${baseURL}${path}`, {
    ...options,
    headers,
  })
  let body = null
  try {
    const text = await response.text()
    body = text ? JSONbig.parse(text) : null
  } catch {
    body = null
  }
  if (response.status === 401) {
    clearToken()
    throw new Error(body?.message || '登录已过期')
  }
  if (!response.ok || (body && typeof body.code === 'number' && body.code !== 200)) {
    throw new Error(body?.message || `请求失败（${response.status}）`)
  }
  return body?.data
}

function query(params) {
  const search = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value)
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
  changePassword(payload) {
    return request('/biz/auth/password', {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  tickets() {
    return request('/biz/cs/tickets')
  },
  ticketDetail(id) {
    return request(`/biz/cs/tickets/${id}`)
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
