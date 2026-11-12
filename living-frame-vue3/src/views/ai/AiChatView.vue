<script setup lang="ts">
import { onMounted, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Clock, CopyDocument, Delete, MagicStick, Plus, Promotion, Setting, User } from '@element-plus/icons-vue'
import { chat, deleteConversation, getAiStatus, getConversationDetail, getConversations } from '@/api/ai'
import type { AiConversation, AiStatus } from '@/types/api'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  time: string
  error?: boolean
  memoryCount?: number
}

const router = useRouter()
const status = ref<AiStatus | null>(null)
const messages = ref<ChatMessage[]>([])
const conversations = ref<AiConversation[]>([])
const input = ref('')
const loading = ref(false)
const historyLoading = ref(false)
const historyVisible = ref(false)
const conversationId = ref<string>()
const messagesEl = ref<HTMLElement>()
let localMessageId = 0

const suggestions = [
  '介绍一下当前系统的用户体系',
  '如何给一个用户分配角色？',
  '帮我设计一个业务用户管理流程',
  'AI Agent 可以调用哪些工具？',
]

function nowTime(): string {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(new Date())
}

async function scrollToBottom() {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

async function loadStatus() {
  try {
    status.value = await getAiStatus()
  } catch {
    status.value = null
  }
}

async function loadConversations() {
  try {
    conversations.value = await getConversations()
  } catch {
    conversations.value = []
  }
}

async function openConversation(id: string) {
  historyLoading.value = true
  try {
    const detail = await getConversationDetail(id)
    conversationId.value = detail.id
    messages.value = detail.messages.map((message) => ({
      id: message.id,
      role: message.role,
      content: message.content,
      time: formatServerTime(message.createdAt),
    }))
    historyVisible.value = false
    await scrollToBottom()
  } finally {
    historyLoading.value = false
  }
}

function newConversation() {
  messages.value = []
  conversationId.value = undefined
  input.value = ''
  historyVisible.value = false
}

async function removeConversation(conversation: AiConversation) {
  try {
    await ElMessageBox.confirm(`确定删除会话「${conversation.title}」吗？`, '删除会话', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteConversation(conversation.id)
    if (conversation.id === conversationId.value) {
      newConversation()
    }
    await loadConversations()
    ElMessage.success('会话已删除')
  } catch {
    // 用户取消
  }
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value || status.value?.enabled === false) {
    return
  }
  messages.value.push({ id: `local-${++localMessageId}`, role: 'user', content: text, time: nowTime() })
  input.value = ''
  loading.value = true
  await scrollToBottom()
  try {
    const result = await chat({ message: text, conversationId: conversationId.value })
    conversationId.value = result.conversationId
    messages.value.push({
      id: `local-${++localMessageId}`,
      role: 'assistant',
      content: result.reply,
      time: nowTime(),
      memoryCount: result.memoryCount,
    })
    await loadConversations()
  } catch {
    messages.value.push({
      id: `local-${++localMessageId}`,
      role: 'assistant',
      content: '本次请求失败，请检查网络或稍后重试。',
      time: nowTime(),
      error: true,
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function formatServerTime(value: string): string {
  if (!value) {
    return ''
  }
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function useSuggestion(text: string) {
  input.value = text
}

async function copyMessage(content: string) {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('当前浏览器不支持复制')
  }
}

onMounted(async () => {
  await loadStatus()
  await loadConversations()
  if (conversations.value.length > 0) {
    await openConversation(conversations.value[0].id)
  }
})
</script>

<template>
  <div class="page chat-page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">AI 对话</h1>
        <p class="page-subtitle">与 AI 助手对话，快速了解系统与获得管理建议。</p>
      </div>
      <div class="chat-actions">
        <el-tag v-if="status" :type="status.enabled ? 'success' : 'warning'" effect="plain" round>
          {{ status.model ? `${status.providerName} · ${status.model}` : '未选择模型' }}
        </el-tag>
        <el-button :icon="Clock" @click="historyVisible = true">
          历史会话
          <span v-if="conversations.length" class="history-count">{{ conversations.length }}</span>
        </el-button>
        <el-button :icon="Setting" @click="router.push('/ai/settings')">模型设置</el-button>
        <el-button :icon="Plus" @click="newConversation">新会话</el-button>
      </div>
    </div>

    <el-alert
      v-if="status && !status.model"
      type="warning"
      :closable="false"
      show-icon
      title="尚未选择模型"
      description="当前账号没有默认模型，请到「AI 设置」选择厂商和模型并保存。"
    />
    <el-alert
      v-else-if="status && !status.enabled"
      type="warning"
      :closable="false"
      show-icon
      title="当前模型未配置"
      description="请到「AI 设置」中填写对应厂商的 API Key，或切换其他已配置的模型。"
    />

    <div class="chat-shell">
      <div ref="messagesEl" class="chat-messages">
        <div v-if="!messages.length" class="chat-empty">
          <div class="empty-icon"><el-icon :size="34"><MagicStick /></el-icon></div>
          <h2>有什么可以帮你？</h2>
          <p>你可以询问系统使用方式，或让 AI 帮你梳理管理流程。</p>
          <div class="suggestion-list">
            <button v-for="item in suggestions" :key="item" type="button" class="suggestion" @click="useSuggestion(item)">
              {{ item }}
            </button>
          </div>
        </div>

        <div v-for="message in messages" :key="message.id" class="message" :class="message.role">
          <div class="message-avatar">
            <el-icon v-if="message.role === 'assistant'"><MagicStick /></el-icon>
            <el-icon v-else><User /></el-icon>
          </div>
          <div class="message-body">
            <div class="message-bubble" :class="{ error: message.error }">
              <span class="message-text">{{ message.content }}</span>
            </div>
            <div class="message-meta">
              <span>{{ message.time }}</span>
              <span v-if="message.memoryCount" class="memory-badge">
                参考 {{ message.memoryCount }} 条长期记忆
              </span>
              <button
                v-if="message.role === 'assistant' && !message.error"
                type="button"
                class="copy-btn"
                @click="copyMessage(message.content)"
              >
                <el-icon><CopyDocument /></el-icon>
                复制
              </button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="message assistant">
          <div class="message-avatar"><el-icon><MagicStick /></el-icon></div>
          <div class="message-body">
            <div class="message-bubble typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input-wrap">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="4000"
          show-word-limit
          placeholder="输入消息，Enter 发送，Shift + Enter 换行"
          :disabled="loading || status?.enabled === false"
          @keydown.enter.exact.prevent="send"
        />
        <div class="input-footer">
          <span class="input-hint">
            <el-icon><ChatDotRound /></el-icon>
            {{ conversationId ? `会话 ${conversationId.slice(0, 8)}` : '新会话' }}
          </span>
          <el-button
            type="primary"
            :icon="Promotion"
            :loading="loading"
            :disabled="!input.trim() || status?.enabled === false"
            @click="send"
          >
            发送
          </el-button>
        </div>
      </div>
    </div>

    <el-drawer v-model="historyVisible" title="历史会话" size="380px" append-to-body>
      <div v-loading="historyLoading" class="history-list">
        <div
          v-for="conversation in conversations"
          :key="conversation.id"
          class="history-item"
          :class="{ active: conversation.id === conversationId }"
          role="button"
          tabindex="0"
          @click="openConversation(conversation.id)"
          @keydown.enter="openConversation(conversation.id)"
        >
          <div class="history-copy">
            <strong>{{ conversation.title }}</strong>
            <span>{{ conversation.messageCount }} 条消息 · {{ formatServerTime(conversation.lastMessageAt) }}</span>
          </div>
          <el-button
            link
            type="danger"
            :icon="Delete"
            aria-label="删除会话"
            @click.stop="removeConversation(conversation)"
          />
        </div>
        <el-empty v-if="!conversations.length" description="还没有历史会话" :image-size="70" />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.chat-page {
  height: calc(100vh - 116px);
  min-height: 620px;
}

.chat-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  margin-left: 5px;
  padding: 0 5px;
  border-radius: 9px;
  color: #5b73ff;
  background: #eef2ff;
  font-size: 10px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 120px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px 12px 12px 14px;
  border: 1px solid #eef1f7;
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.history-item:hover,
.history-item.active {
  border-color: #c7d2fe;
  background: #f7f9ff;
}

.history-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.history-copy strong {
  overflow: hidden;
  color: #263042;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-copy span {
  color: #9aa3b5;
  font-size: 10px;
}

.chat-shell {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--lf-border);
  border-radius: 18px;
  background: #ffffff;
  box-shadow: var(--lf-shadow);
}

.chat-messages {
  flex: 1;
  padding: 28px 30px;
  overflow-y: auto;
  background:
    radial-gradient(circle at 12% 0%, rgba(79, 107, 251, 0.05), transparent 26%),
    #ffffff;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  text-align: center;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 76px;
  height: 76px;
  border: 1px solid #e4e8ff;
  border-radius: 22px;
  color: #5b73ff;
  background: linear-gradient(145deg, #f2f5ff, #ffffff);
  box-shadow: 0 12px 28px rgba(79, 107, 251, 0.1);
}

.chat-empty h2 {
  margin: 18px 0 0;
  color: #111827;
  font-size: 22px;
  letter-spacing: -0.4px;
}

.chat-empty p {
  max-width: 460px;
  margin: 8px 0 0;
  color: #8d96a8;
  font-size: 13px;
  line-height: 1.7;
}

.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  max-width: 620px;
  margin-top: 22px;
}

.suggestion {
  padding: 9px 14px;
  border: 1px solid #e8ebf3;
  border-radius: 999px;
  color: #5d6678;
  background: #ffffff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.suggestion:hover {
  border-color: #ced7fe;
  color: #3d55e8;
  background: #f5f7ff;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  margin-bottom: 22px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 10px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #7c5cff);
}

.message.user .message-avatar {
  background: #eef2ff;
  color: #4f6bfb;
}

.message-body {
  display: flex;
  flex-direction: column;
  max-width: min(72%, 760px);
}

.message.user .message-body {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 15px;
  border: 1px solid #e8ebf3;
  border-radius: 4px 14px 14px 14px;
  color: #313b4d;
  background: #f8f9fc;
  font-size: 13px;
  line-height: 1.75;
}

.message.user .message-bubble {
  border-color: transparent;
  border-radius: 14px 4px 14px 14px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #6d5cf6);
}

.message-bubble.error {
  border-color: #fde2e2;
  color: #b42318;
  background: #fff7f7;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
  color: #b0b7c4;
  font-size: 10px;
}

.memory-badge {
  padding: 2px 7px;
  border-radius: 8px;
  color: #6d5bd0;
  background: #f1efff;
}

.copy-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 0;
  border: none;
  color: #b0b7c4;
  background: transparent;
  font-size: 10px;
  cursor: pointer;
}

.copy-btn:hover {
  color: #4f6bfb;
}

.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 54px;
  height: 38px;
}

.typing span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #8ea0ff;
  animation: typing 1s infinite ease-in-out;
}

.typing span:nth-child(2) {
  animation-delay: 0.16s;
}

.typing span:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes typing {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.45;
  }
  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.chat-input-wrap {
  padding: 14px 18px 16px;
  border-top: 1px solid var(--lf-border);
  background: #ffffff;
}

.chat-input-wrap :deep(.el-textarea__inner) {
  padding: 12px 14px;
  border-radius: 12px;
  box-shadow: none;
  background: #f8f9fc;
}

.chat-input-wrap :deep(.el-textarea__inner:focus) {
  background: #ffffff;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.input-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #a1a9b8;
  font-size: 11px;
}

@media (max-width: 700px) {
  .chat-page {
    height: auto;
  }

  .chat-messages {
    padding: 18px;
    min-height: 420px;
  }

  .message-body {
    max-width: 84%;
  }
}
</style>
