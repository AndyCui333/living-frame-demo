import { http } from '../request'
import type { BusinessUserRecord, ID, PageQuery, PageResult } from '@/types/api'
import { toId } from '@/utils/id'

interface RawBusinessUser extends Omit<BusinessUserRecord, 'id'> {
  id: string | number
}

export interface BusinessUserCreatePayload {
  username: string
  password: string
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
}

export interface BusinessUserUpdatePayload {
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
}

function normalizeBusinessUser(raw: RawBusinessUser): BusinessUserRecord {
  return { ...raw, id: toId(raw.id) }
}

export async function getBusinessUsers(params: PageQuery): Promise<PageResult<BusinessUserRecord>> {
  const page = await http.get<PageResult<RawBusinessUser>>('/business/users', { params })
  return { ...page, records: page.records.map(normalizeBusinessUser) }
}

export function createBusinessUser(payload: BusinessUserCreatePayload): Promise<ID> {
  return http.post<ID>('/business/users', payload).then(toId)
}

export function updateBusinessUser(id: ID, payload: BusinessUserUpdatePayload): Promise<void> {
  return http.put<void>(`/business/users/${id}`, payload)
}

export function deleteBusinessUser(id: ID): Promise<void> {
  return http.delete<void>(`/business/users/${id}`)
}

export function updateBusinessUserStatus(id: ID, status: number): Promise<void> {
  return http.put<void>(`/business/users/${id}/status`, { status })
}

export function resetBusinessUserPassword(id: ID, password: string): Promise<void> {
  return http.put<void>(`/business/users/${id}/password`, { password })
}
