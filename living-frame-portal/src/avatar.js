const base = (import.meta.env.VITE_AVATAR_BASE_URL || '/api/public/avatars/').replace(/\/?$/, '/')

export function resolveAvatar(path) {
  if (!path) return ''
  if (/^(https?:|data:|blob:)/i.test(path)) return path
  return `${base}${path.replace(/^\/+/, '')}`
}
