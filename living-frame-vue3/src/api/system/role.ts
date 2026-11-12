import { http } from '../request'
import type { ID, PageQuery, PageResult, RoleRecord } from '@/types/api'
import { normalizeIdList, toId } from '@/utils/id'

interface RawRole extends Omit<RoleRecord, 'id'> {
  id: string | number
}

export interface RoleCreatePayload {
  code: string
  name: string
  description?: string
  sort?: number
}

export interface RoleUpdatePayload {
  name: string
  description?: string
  sort?: number
  status: number
}

function normalizeRole(raw: RawRole): RoleRecord {
  return { ...raw, id: toId(raw.id) }
}

export async function getRoles(params: PageQuery): Promise<PageResult<RoleRecord>> {
  const page = await http.get<PageResult<RawRole>>('/system/roles', { params })
  return { ...page, records: page.records.map(normalizeRole) }
}

export async function getAllRoles(): Promise<RoleRecord[]> {
  const roles = await http.get<RawRole[]>('/system/roles/options')
  return (roles ?? []).map(normalizeRole)
}

export function createRole(payload: RoleCreatePayload): Promise<ID> {
  return http.post<ID>('/system/roles', payload).then(toId)
}

export function updateRole(id: ID, payload: RoleUpdatePayload): Promise<void> {
  return http.put<void>(`/system/roles/${id}`, payload)
}

export function deleteRole(id: ID): Promise<void> {
  return http.delete<void>(`/system/roles/${id}`)
}

export async function getRolePermissions(id: ID): Promise<ID[]> {
  const ids = await http.get<Array<string | number>>(`/system/roles/${id}/permissions`)
  return normalizeIdList(ids)
}

export function assignRolePermissions(id: ID, permissionIds: ID[]): Promise<void> {
  return http.put<void>(`/system/roles/${id}/permissions`, { permissionIds })
}
