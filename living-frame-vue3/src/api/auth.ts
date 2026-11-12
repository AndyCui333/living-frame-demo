import { http } from './request'
import type { CaptchaResult, LoginPayload, LoginResult, MeInfo, MenuNode } from '@/types/api'
import { toId } from '@/utils/id'

export function getCaptcha(): Promise<CaptchaResult> {
  return http.get<CaptchaResult>('/auth/captcha')
}

export async function login(payload: LoginPayload): Promise<LoginResult> {
  const result = await http.post<LoginResult>('/auth/login', payload)
  return { ...result, userId: toId(result.userId) }
}

export function logout(): Promise<void> {
  return http.post<void>('/auth/logout')
}

export async function getMe(): Promise<MeInfo> {
  const result = await http.get<MeInfo>('/auth/me')
  return {
    ...result,
    id: toId(result.id),
    roles: result.roles ?? [],
    permissions: result.permissions ?? [],
  }
}

function normalizeMenu(menu: MenuNode): MenuNode {
  return {
    ...menu,
    id: toId(menu.id),
    parentId: toId(menu.parentId),
    children: (menu.children ?? []).map(normalizeMenu),
  }
}

export async function getMenus(): Promise<MenuNode[]> {
  const result = await http.get<MenuNode[]>('/auth/menus')
  return (result ?? []).map(normalizeMenu)
}

export function changePassword(payload: { oldPassword: string; newPassword: string }): Promise<void> {
  return http.put<void>('/auth/password', payload)
}
