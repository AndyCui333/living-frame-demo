import { http } from '../request'
import type { ID, PageQuery, PageResult, RoleBrief, UserRecord } from '@/types/api'
import { toId } from '@/utils/id'

interface RawRoleBrief {
  id: string | number
  code: string
  name: string
}

interface RawUser extends Omit<UserRecord, 'id' | 'roles'> {
  id: string | number
  roles: RawRoleBrief[]
}

export interface UserCreatePayload {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  roleIds: ID[]
}

export interface UserUpdatePayload {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
}

function normalizeUser(raw: RawUser): UserRecord {
  return {
    ...raw,
    id: toId(raw.id),
    roles: (raw.roles ?? []).map((role) => ({
      id: toId(role.id),
      code: role.code,
      name: role.name,
    })),
  }
}

export async function getUsers(params: PageQuery): Promise<PageResult<UserRecord>> {
  const page = await http.get<PageResult<RawUser>>('/system/users', { params })
  return { ...page, records: page.records.map(normalizeUser) }
}

export function createUser(payload: UserCreatePayload): Promise<ID> {
  return http.post<ID>('/system/users', payload).then(toId)
}

export function updateUser(id: ID, payload: UserUpdatePayload): Promise<void> {
  return http.put<void>(`/system/users/${id}`, payload)
}

export function deleteUser(id: ID): Promise<void> {
  return http.delete<void>(`/system/users/${id}`)
}

export function updateUserStatus(id: ID, status: number): Promise<void> {
  return http.put<void>(`/system/users/${id}/status`, { status })
}

export function resetUserPassword(id: ID, password: string): Promise<void> {
  return http.put<void>(`/system/users/${id}/password`, { password })
}

export function getUserRoles(id: ID): Promise<RoleBrief[]> {
  return http.get<RawRoleBrief[]>(`/system/users/${id}/roles`).then((roles) =>
    (roles ?? []).map((role) => ({ id: toId(role.id), code: role.code, name: role.name })),
  )
}

export function assignUserRoles(id: ID, roleIds: ID[]): Promise<void> {
  return http.put<void>(`/system/users/${id}/roles`, { roleIds })
}
