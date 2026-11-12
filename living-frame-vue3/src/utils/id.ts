import type { ID } from '@/types/api'

export function toId(value: string | number | null | undefined): ID {
  return value === null || value === undefined ? '' : String(value)
}

export function normalizeIdList(values: Array<string | number> | null | undefined): ID[] {
  return (values ?? []).map(toId).filter(Boolean)
}
