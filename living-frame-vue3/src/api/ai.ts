import { http } from './request'
import type {
  AgentResult,
  AiConversation,
  AiConversationDetail,
  AiConversationMessage,
  AiStatus,
  AiUserConfig,
  AiUserConfigPayload,
  ChatResult,
} from '@/types/api'
import { toId } from '@/utils/id'

interface RawAiUserConfig extends Omit<AiUserConfig, 'id'> {
  id: string | number
}

interface RawAiConversation extends Omit<AiConversation, 'id'> {
  id: string | number
}

interface RawAiConversationMessage extends Omit<AiConversationMessage, 'id'> {
  id: string | number
}

interface RawAiConversationDetail extends Omit<AiConversationDetail, 'id' | 'messages'> {
  id: string | number
  messages: RawAiConversationMessage[]
}

export function getAiStatus(): Promise<AiStatus> {
  return http.get<AiStatus>('/ai/status')
}

export function getAiConfigs(): Promise<AiUserConfig[]> {
  return http.get<RawAiUserConfig[]>('/ai/configs').then((configs) =>
    configs.map((config) => ({ ...config, id: toId(config.id) })),
  )
}

export function createAiConfig(payload: AiUserConfigPayload): Promise<string> {
  return http.post<string | number>('/ai/configs', payload).then(toId)
}

export function updateAiConfig(id: string, payload: AiUserConfigPayload): Promise<void> {
  return http.put<void>(`/ai/configs/${id}`, payload)
}

export function deleteAiConfig(id: string): Promise<void> {
  return http.delete<void>(`/ai/configs/${id}`)
}

export function setDefaultAiConfig(id: string): Promise<void> {
  return http.put<void>(`/ai/configs/${id}/default`)
}

export function chat(payload: { message: string; conversationId?: string }): Promise<ChatResult> {
  return http.post<ChatResult>('/ai/chat', payload)
}

export function getConversations(): Promise<AiConversation[]> {
  return http.get<RawAiConversation[]>('/ai/conversations').then((items) =>
    items.map((item) => ({ ...item, id: toId(item.id) })),
  )
}

export function getConversationDetail(id: string): Promise<AiConversationDetail> {
  return http.get<RawAiConversationDetail>(`/ai/conversations/${id}`).then((detail) => ({
    ...detail,
    id: toId(detail.id),
    messages: detail.messages.map((message) => ({ ...message, id: toId(message.id) })),
  }))
}

export function deleteConversation(id: string): Promise<void> {
  return http.delete<void>(`/ai/conversations/${id}`)
}

export function runAgent(payload: { task: string }): Promise<AgentResult> {
  return http.post<AgentResult>('/ai/agent', payload)
}
