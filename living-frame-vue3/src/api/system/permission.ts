import { http } from '../request'
import type { ID, PermissionRecord } from '@/types/api'
import { toId } from '@/utils/id'

interface RawPermission extends Omit<PermissionRecord, 'id' | 'parentId' | 'children'> {
  id: string | number
  parentId: string | number
  children?: RawPermission[]
}

export interface PermissionPayload {
  code?: string
  name: string
  type: 'MENU' | 'BUTTON'
  parentId: ID
  path?: string
  component?: string
  icon?: string
  sort?: number
  status: number
}

function normalizePermission(raw: RawPermission): PermissionRecord {
  return {
    ...raw,
    id: toId(raw.id),
    parentId: toId(raw.parentId),
    children: (raw.children ?? []).map(normalizePermission),
  }
}

export async function getPermissionTree(): Promise<PermissionRecord[]> {
  const tree = await http.get<RawPermission[]>('/system/permissions/tree')
  return (tree ?? []).map(normalizePermission)
}

export function createPermission(payload: PermissionPayload): Promise<ID> {
  return http.post<ID>('/system/permissions', payload).then(toId)
}

export function updatePermission(id: ID, payload: PermissionPayload): Promise<void> {
  return http.put<void>(`/system/permissions/${id}`, payload)
}

export function deletePermission(id: ID): Promise<void> {
  return http.delete<void>(`/system/permissions/${id}`)
}
