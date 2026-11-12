import { http } from '../request'
import type {
  BlacklistCreatePayload,
  BlacklistRecord,
  ID,
  PageQuery,
  PageResult,
} from '@/types/api'
import { toId } from '@/utils/id'

interface RawBlacklist extends Omit<BlacklistRecord, 'id'> {
  id: string | number
}

export interface BlacklistQuery extends PageQuery {
  targetType?: string
  targetValue?: string
  status?: number
}

export async function getBlacklist(params: BlacklistQuery): Promise<PageResult<BlacklistRecord>> {
  const page = await http.get<PageResult<RawBlacklist>>('/system/blacklist', { params })
  return { ...page, records: page.records.map((record) => ({ ...record, id: toId(record.id) })) }
}

export function createBlacklist(payload: BlacklistCreatePayload): Promise<ID> {
  return http.post<ID>('/system/blacklist', payload).then(toId)
}

export function updateBlacklistStatus(id: string, status: number): Promise<void> {
  return http.put<void>(`/system/blacklist/${id}/status`, { status })
}

export function deleteBlacklist(id: string): Promise<void> {
  return http.delete<void>(`/system/blacklist/${id}`)
}
