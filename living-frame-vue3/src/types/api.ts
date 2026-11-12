export type ID = string

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PageQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: number
}

export interface LoginPayload {
  username: string
  password: string
  captchaId: string
  captchaCode: string
}

export interface LoginResult {
  token: string
  tokenName: string
  expiresIn: number
  userId: ID
  username: string
  nickname: string
  userType: 'ADMIN' | 'BUSINESS'
}

export interface CaptchaResult {
  captchaId: string
  image: string
  expiresIn?: number
}

export interface MeInfo {
  id: ID
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  userType: string
  roles: string[]
  permissions: string[]
  superAdmin: boolean
}

export interface MenuNode {
  id: ID
  code: string
  name: string
  type: 'MENU' | 'BUTTON'
  parentId: ID
  path?: string
  component?: string
  icon?: string
  sort: number
  status: number
  children: MenuNode[]
}

export interface RoleBrief {
  id: ID
  code: string
  name: string
}

export interface UserRecord {
  id: ID
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  status: number
  lastLoginAt?: string
  createdAt?: string
  roles: RoleBrief[]
}

export interface RoleRecord {
  id: ID
  code: string
  name: string
  description?: string
  status: number
  sort: number
  createdAt?: string
}

export interface PermissionRecord {
  id: ID
  code: string
  name: string
  type: 'MENU' | 'BUTTON'
  parentId: ID
  path?: string
  component?: string
  icon?: string
  sort: number
  status: number
  children: PermissionRecord[]
}

export interface BusinessUserRecord {
  id: ID
  username: string
  nickname: string
  phone?: string
  email?: string
  avatar?: string
  status: number
  lastLoginAt?: string
  createdAt?: string
}

export interface AiStatus {
  enabled: boolean
  provider: string
  providerName: string
  model: string
  modelName: string
  reasoningLevel: string
}

export interface AiReasoningOption {
  value: string
  label: string
}

export interface AiModelOption {
  id: string
  name: string
  supportsReasoning: boolean
  reasoningLevels: AiReasoningOption[]
  defaultReasoningLevel: string
}

export interface AiProviderOption {
  provider: string
  name: string
  vendor: string
  configured: boolean
  enabled: boolean
  models: AiModelOption[]
}

export interface AiConfig {
  provider: string
  providerName: string
  model: string
  modelName: string
  reasoningLevel: string
  reasoningLevelName: string
  temperature: number
  maxTokens: number
  providerConfigured: boolean
  enabled: boolean
}

export interface AiOptions {
  config: AiConfig
  providers: AiProviderOption[]
  configs: AiUserConfig[]
}

export interface AiUserConfig {
  id: string
  provider: string
  providerName: string
  baseUrl: string
  model: string
  hasApiKey: boolean
  isDefault: boolean
  reasoningLevel: string
  temperature: number
  maxTokens: number
}

export interface AiUserConfigPayload {
  provider: string
  providerName: string
  baseUrl: string
  model: string
  apiKey?: string
  clearApiKey?: boolean
  defaultConfig?: boolean
  reasoningLevel: string
  temperature: number
  maxTokens: number
}

export interface AiProviderConfig {
  provider: string
  name: string
  baseUrl: string
  apiKeyMasked: string
  configured: boolean
  enabled: boolean
}

export interface AiConfigPayload {
  provider: string
  providerName: string
  baseUrl: string
  model: string
  apiKey?: string
  clearApiKey?: boolean
  defaultConfig?: boolean
  reasoningLevel: string
  temperature: number
  maxTokens: number
}

export interface AiProviderUpdatePayload {
  baseUrl?: string
  apiKey?: string
  enabled?: boolean
  clearApiKey?: boolean
}

export interface MinioFileConfig {
  defaultBucket: string
}

export interface MinioObject {
  objectName: string
  name: string
  size: number
  lastModified?: string | null
  dir: boolean
}

export interface AiProviderTestResult {
  success: boolean
  message: string
  modelCount: number
}

export interface ChatResult {
  conversationId: string
  reply: string
  memoryCount: number
}

export interface AiConversation {
  id: string
  title: string
  messageCount: number
  lastMessageAt: string
}

export interface AiConversationMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export interface AiConversationDetail {
  id: string
  title: string
  messages: AiConversationMessage[]
}

export interface CsAgent {
  id: string
  userId: string
  name: string
  serviceType: 'CHAT' | 'TECH'
  onlineStatus: 'IDLE' | 'BUSY' | 'OFFLINE'
  currentTicketId?: string | null
}

export interface CsTicket {
  id: string
  ticketNo: string
  title: string
  content: string
  ticketType: 'CHAT' | 'TECH'
  status: 'DRAFT' | 'PENDING' | 'ASSIGNED' | 'PROCESSING' | 'ARCHIVED'
  portalUserName: string
  assignedAgentId?: string | null
  assignedAgentName?: string
  rating?: number | null
  feedback?: string | null
  assignedAt?: string | null
  lastMessageAt?: string | null
  closedAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface CsMessage {
  id: string
  senderType: 'PORTAL' | 'AGENT' | 'SYSTEM'
  senderName: string
  content: string
  createdAt: string
}

export interface CsTicketDetail {
  ticket: CsTicket
  messages: CsMessage[]
}

export interface AgentResult {
  reply: string
}

export interface LoginLogRecord {
  id: string
  userId?: string | null
  username: string
  loginType: string
  ip?: string
  userAgent?: string
  status: 'SUCCESS' | 'FAIL' | string
  message?: string
  loginAt?: string
}

export interface OperationLogRecord {
  id: string
  userId?: string | null
  username?: string
  operation: string
  method?: string
  requestUri?: string
  ip?: string
  status: 'SUCCESS' | 'FAIL' | string
  errorMessage?: string
  durationMs?: number
  createdAt?: string
}

export interface BlacklistRecord {
  id: string
  targetType: 'IP' | 'USERNAME' | string
  targetValue: string
  reason?: string
  status: number
  createdBy?: string
}

export interface BlacklistCreatePayload {
  targetType: 'IP' | 'USERNAME'
  targetValue: string
  reason: string
  status?: number
}

