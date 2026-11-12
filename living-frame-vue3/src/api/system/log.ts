import { http } from '../request'
import type { LoginLogRecord, OperationLogRecord, PageQuery, PageResult } from '@/types/api'
import { toId } from '@/utils/id'

interface RawLoginLog extends Omit<LoginLogRecord, 'id' | 'userId'> {
  id: string | number
  userId?: string | number | null
}

interface RawOperationLog extends Omit<OperationLogRecord, 'id' | 'userId'> {
  id: string | number
  userId?: string | number | null
}

export interface LoginLogQuery extends Omit<PageQuery, 'status'> {
  username?: string
  ip?: string
  status?: string
  loginType?: string
}

export interface OperationLogQuery extends Omit<PageQuery, 'status'> {
  username?: string
  operation?: string
  status?: string
}

export async function getLoginLogs(params: LoginLogQuery): Promise<PageResult<LoginLogRecord>> {
  const page = await http.get<PageResult<RawLoginLog>>('/system/login-logs', { params })
  return {
    ...page,
    records: page.records.map((record) => ({
      ...record,
      id: toId(record.id),
      userId: record.userId == null ? null : toId(record.userId),
    })),
  }
}

export async function getOperationLogs(params: OperationLogQuery): Promise<PageResult<OperationLogRecord>> {
  const page = await http.get<PageResult<RawOperationLog>>('/system/operation-logs', { params })
  return {
    ...page,
    records: page.records.map((record) => ({
      ...record,
      id: toId(record.id),
      userId: record.userId == null ? null : toId(record.userId),
    })),
  }
}
