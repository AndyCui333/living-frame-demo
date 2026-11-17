import { http } from './request'
import type { CsAgent, CsMessage, CsOnlineInbox, CsRanking, CsTicket, CsTicketDetail } from '@/types/api'
import { toId } from '@/utils/id'

interface RawCsAgent extends Omit<CsAgent, 'id' | 'userId' | 'currentTicketId'> {
  id: string | number
  userId: string | number
  currentTicketId?: string | number | null
}

interface RawCsTicket extends Omit<CsTicket, 'id' | 'assignedAgentId'> {
  id: string | number
  assignedAgentId?: string | number | null
}

interface RawCsMessage extends Omit<CsMessage, 'id'> {
  id: string | number
}

interface RawCsTicketDetail {
  ticket: RawCsTicket
  messages: RawCsMessage[]
}

interface RawCsOnlineInbox extends Omit<CsOnlineInbox, 'earliestUnreadTicketId' | 'unreadTickets'> {
  earliestUnreadTicketId?: string | number | null
  unreadTickets: RawCsTicket[]
}

interface RawCsRanking extends Omit<CsRanking, 'agentUserId'> {
  agentUserId: string | number
}

function mapTicket(ticket: RawCsTicket): CsTicket {
  return {
    ...ticket,
    id: toId(ticket.id),
    assignedAgentId: ticket.assignedAgentId === null || ticket.assignedAgentId === undefined
      ? null
      : toId(ticket.assignedAgentId),
  }
}

export function getCsTickets(type?: string): Promise<CsTicket[]> {
  return http.get<RawCsTicket[]>('/cs/tickets', { params: type ? { type } : undefined })
    .then((items) => items.map(mapTicket))
}

export function getCsTicketDetail(id: string): Promise<CsTicketDetail> {
  return http.get<RawCsTicketDetail>(`/cs/tickets/${id}`).then((detail) => ({
    ticket: mapTicket(detail.ticket),
    messages: detail.messages.map((message) => ({ ...message, id: toId(message.id) })),
  }))
}

export function getCsAgents(serviceType?: string): Promise<CsAgent[]> {
  return http.get<RawCsAgent[]>('/cs/agents', { params: serviceType ? { serviceType } : undefined })
    .then((items) => items.map((item) => ({
      ...item,
      id: toId(item.id),
      userId: toId(item.userId),
      currentTicketId: item.currentTicketId === null || item.currentTicketId === undefined
        ? null
        : toId(item.currentTicketId),
    })))
}

export function replyCsTicket(id: string, content: string): Promise<CsMessage> {
  return http.post<RawCsMessage>(`/cs/tickets/${id}/reply`, { content })
    .then((message) => ({ ...message, id: toId(message.id) }))
}

export function transferCsTicket(id: string, targetAgentId: string): Promise<void> {
  return http.post<void>(`/cs/tickets/${id}/transfer`, { targetAgentId })
}

export function archiveCsTicket(id: string): Promise<void> {
  return http.post<void>(`/cs/tickets/${id}/archive`)
}

export function dispatchCsTicket(id: string): Promise<void> {
  return http.post<void>(`/cs/tickets/${id}/dispatch`)
}

export function markCsTicketRead(id: string): Promise<void> {
  return http.post<void>(`/cs/tickets/${id}/read`)
}

export function leaveCsTicket(id: string): Promise<void> {
  return http.post<void>(`/cs/tickets/${id}/leave`)
}

export function heartbeatCsAgent(): Promise<CsOnlineInbox> {
  return http.post<RawCsOnlineInbox>('/cs/agents/heartbeat').then((inbox) => ({
    ...inbox,
    earliestUnreadTicketId: inbox.earliestUnreadTicketId === null || inbox.earliestUnreadTicketId === undefined
      ? null
      : toId(inbox.earliestUnreadTicketId),
    unreadTickets: inbox.unreadTickets.map(mapTicket),
  }))
}

export function offlineCsAgent(): Promise<void> {
  return http.post<void>('/cs/agents/offline')
}

export function getCsRanking(): Promise<CsRanking[]> {
  return http.get<RawCsRanking[]>('/cs/agents/ranking').then((items) =>
    items.map((item) => ({ ...item, agentUserId: toId(item.agentUserId) })),
  )
}
