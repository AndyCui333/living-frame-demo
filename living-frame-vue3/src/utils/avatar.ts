const avatarBaseUrl = (import.meta.env.VITE_AVATAR_BASE_URL || '/api/public/avatars/').replace(/\/?$/, '/')

/**
 * 数据库保存的是 MinIO 半路径（如 avatar/2026/11/xxx.png）。
 * 完整地址由前端环境变量拼接，兼容历史完整 URL。
 */
export function resolveAvatarUrl(path?: string | null): string {
  if (!path) {
    return ''
  }
  if (/^(https?:|data:|blob:)/i.test(path)) {
    return path
  }
  return `${avatarBaseUrl}${path.replace(/^\/+/, '')}`
}
