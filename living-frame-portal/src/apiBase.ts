const STORAGE_KEY = 'living-frame-api-base'

export function getApiBase() {
  const saved = localStorage.getItem(STORAGE_KEY)
  const configured = saved || import.meta.env.VITE_API_BASE_URL || '/api'
  return String(configured).replace(/\/+$/, '') || '/api'
}

export function setApiBase(value) {
  const normalized = String(value || '').trim().replace(/\/+$/, '')
  if (!normalized) {
    localStorage.removeItem(STORAGE_KEY)
  } else {
    localStorage.setItem(STORAGE_KEY, normalized)
  }
}

export function getApiOrigin() {
  const base = getApiBase()
  return base.endsWith('/api') ? base.slice(0, -4) : base
}
