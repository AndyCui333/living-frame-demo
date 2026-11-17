import { getApiBase } from './apiBase'

export function resolveAvatar(path) {
  if (!path) return ''
  if (/^(https?:|data:|blob:)/i.test(path)) return path
  const base = `${getApiBase()}/public/avatars/`
  return `${base}${path.replace(/^\/+/, '')}`
}
