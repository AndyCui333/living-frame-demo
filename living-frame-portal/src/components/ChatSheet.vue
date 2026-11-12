<script setup>
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { portalApi } from '../api'
import { chatState } from '../chatState'

const type = ref('CHAT')
const tickets = ref([])
const current = ref(null)
const messages = ref([])
const input = ref('')
const loading = ref(false)
const sending = ref(false)
const historyVisible = ref(false)
const messagesEl = ref(null)
const feedbackRating = ref(0)
const feedbackText = ref('')
const feedbackSending = ref(false)
let polling = null

const isOpen = computed(() => chatState.open)
const title = computed(() => (type.value === 'TECH' ? '技术客服' : '在线客服'))
const statusText = computed(() => {
  if (!current.value) return '新建对话'
  return statusLabels[current.value.status] || current.value.status
})
const canRate = computed(() => current.value?.status === 'ARCHIVED' && !current.value?.rating)

const statusLabels = {
  DRAFT: '草稿',
  PENDING: '等待接单',
  ASSIGNED: '已接单',
  PROCESSING: '沟通中',
  ARCHIVED: '已归档',
}

const typeLabels = {
  CHAT: '在线客服',
  TECH: '技术客服',
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function isRecent(ticket) {
  if (!ticket.lastMessageAt) return false
  const time = new Date(ticket.lastMessageAt.replace(' ', 'T')).getTime()
  return Number.isFinite(time) && Date.now() - time < 2 * 60 * 1000
}

function isInProgress(ticket) {
  return ticket.status === 'ASSIGNED' || ticket.status === 'PROCESSING'
}

async function scrollToBottom() {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

async function loadTickets() {
  tickets.value = await portalApi.tickets()
  return tickets.value
}

async function loadDetail(ticketId) {
  const detail = await portalApi.ticketDetail(ticketId)
  current.value = detail.ticket
  type.value = detail.ticket.ticketType
  messages.value = detail.messages
  feedbackRating.value = detail.ticket.rating || 0
  feedbackText.value = detail.ticket.feedback || ''
  await scrollToBottom()
}

async function boot() {
  loading.value = true
  historyVisible.value = chatState.showHistory
  type.value = chatState.initialType || 'CHAT'
  current.value = null
  messages.value = []
  try {
    const list = await loadTickets()
    if (!historyVisible.value) {
      const active = list
        .filter((ticket) => ticket.ticketType === type.value && isInProgress(ticket) && isRecent(ticket))
        .sort((a, b) => String(b.lastMessageAt).localeCompare(String(a.lastMessageAt)))[0]
      if (active) {
        await loadDetail(active.id)
      }
    }
  } finally {
    loading.value = false
    startPolling()
  }
}

function startPolling() {
  stopPolling()
  polling = setInterval(async () => {
    if (!chatState.open || historyVisible.value || !current.value || sending.value) return
    if (!['PENDING', 'ASSIGNED', 'PROCESSING'].includes(current.value.status)) return
    try {
      await loadDetail(current.value.id)
    } catch {
      // 轮询失败不打扰用户，下一轮重试
    }
  }, 4500)
}

function stopPolling() {
  if (polling) {
    clearInterval(polling)
    polling = null
  }
}

function close() {
  chatState.open = false
  stopPolling()
}

function chooseType(nextType) {
  if (type.value === nextType && !current.value) return
  type.value = nextType
  current.value = null
  messages.value = []
  historyVisible.value = false
  feedbackRating.value = 0
  feedbackText.value = ''
}

async function openHistory() {
  historyVisible.value = true
  loading.value = true
  try {
    await loadTickets()
  } finally {
    loading.value = false
  }
}

async function chooseTicket(ticket) {
  historyVisible.value = false
  loading.value = true
  try {
    await loadDetail(ticket.id)
  } finally {
    loading.value = false
  }
}

async function send() {
  const content = input.value.trim()
  if (!content || sending.value) return
  sending.value = true
  try {
    const detail = await portalApi.chat({
      ticketId: current.value?.id || null,
      content,
      ticketType: type.value,
    })
    current.value = detail.ticket
    type.value = detail.ticket.ticketType
    messages.value = detail.messages
    input.value = ''
    await loadTickets()
    await scrollToBottom()
  } catch (error) {
    window.alert(error.message || '发送失败')
  } finally {
    sending.value = false
  }
}

function onKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    send()
  }
}

async function submitFeedback() {
  if (!current.value || !feedbackRating.value || feedbackSending.value) return
  feedbackSending.value = true
  try {
    await portalApi.feedback(current.value.id, {
      rating: feedbackRating.value,
      feedback: feedbackText.value.trim(),
    })
    current.value = { ...current.value, rating: feedbackRating.value, feedback: feedbackText.value.trim() }
  } catch (error) {
    window.alert(error.message || '评价失败')
  } finally {
    feedbackSending.value = false
  }
}

watch(
  () => chatState.nonce,
  async () => {
    if (chatState.open) {
      await boot()
    }
  },
)

onUnmounted(stopPolling)
</script>

<template>
  <Teleport to="body">
    <Transition name="chat-fade">
      <div v-if="isOpen" class="chat-overlay" @click.self="close">
        <section class="chat-sheet">
          <header class="chat-header">
            <button type="button" class="chat-icon" aria-label="关闭" @click="close">
              <svg viewBox="0 0 24 24"><path d="m6 9 6 6 6-6" /></svg>
            </button>
            <div class="chat-heading">
              <strong>{{ title }}</strong>
              <span>{{ statusText }}</span>
            </div>
            <button type="button" class="chat-icon" aria-label="历史工单" @click="openHistory">
              <svg viewBox="0 0 24 24">
                <path d="M4 12a8 8 0 1 0 2.4-5.7" />
                <path d="M4 5v4.5h4.5" />
                <path d="M12 8v4l3 2" />
              </svg>
            </button>
          </header>

          <div v-if="historyVisible" class="history-panel">
            <div class="history-heading">
              <div>
                <strong>历史工单</strong>
                <span>下拉或点击查看历史对话</span>
              </div>
              <button type="button" class="text-button" @click="historyVisible = false">返回对话</button>
            </div>
            <div v-if="loading" class="loading-line">加载中...</div>
            <button
              v-for="ticket in tickets"
              :key="ticket.id"
              type="button"
              class="history-card"
              @click="chooseTicket(ticket)"
            >
              <div class="history-card-top">
                <strong>{{ ticket.title }}</strong>
                <span class="status-pill" :class="ticket.status.toLowerCase()">
                  {{ statusLabels[ticket.status] || ticket.status }}
                </span>
              </div>
              <span>{{ ticket.ticketNo }} · {{ typeLabels[ticket.ticketType] }} · {{ formatTime(ticket.lastMessageAt || ticket.updatedAt) }}</span>
            </button>
            <div v-if="!loading && !tickets.length" class="empty-state">
              <strong>还没有工单记录</strong>
              <span>发送第一条消息，客服会为你创建工单</span>
            </div>
          </div>

          <div v-else class="conversation">
            <div ref="messagesEl" class="message-list">
              <div v-if="!current && !messages.length" class="chat-welcome">
                <div class="welcome-orbit">
                  <svg viewBox="0 0 24 24">
                    <path d="M4.8 13.3v-1.5a7.2 7.2 0 0 1 14.4 0v1.5" />
                    <rect x="3.5" y="13" width="4.2" height="6.2" rx="2.1" />
                    <rect x="16.3" y="13" width="4.2" height="6.2" rx="2.1" />
                  </svg>
                </div>
                <h2>你好，我是 Living Frame 客服</h2>
                <p>发送你的问题，我们会为你创建工单并安排客服。</p>
                <div class="type-switch">
                  <button type="button" :class="{ active: type === 'CHAT' }" @click="chooseType('CHAT')">在线客服</button>
                  <button type="button" :class="{ active: type === 'TECH' }" @click="chooseType('TECH')">技术客服</button>
                </div>
                <div class="suggestions">
                  <button type="button" @click="input = '如何修改登录密码？'">如何修改登录密码？</button>
                  <button type="button" @click="input = '我想了解工单处理进度'">我想了解工单处理进度</button>
                </div>
              </div>

              <div
                v-for="message in messages"
                :key="message.id"
                class="bubble-row"
                :class="message.senderType.toLowerCase()"
              >
                <div class="bubble-meta">
                  <span>{{ message.senderType === 'SYSTEM' ? '系统' : message.senderName }}</span>
                  <span>{{ formatTime(message.createdAt) }}</span>
                </div>
                <div class="bubble">{{ message.content }}</div>
              </div>

              <div v-if="canRate" class="rating-card">
                <strong>本次服务怎么样？</strong>
                <span>你的评价会帮助我们做得更好</span>
                <div class="stars">
                  <button
                    v-for="star in 5"
                    :key="star"
                    type="button"
                    :class="{ active: star <= feedbackRating }"
                    @click="feedbackRating = star"
                  >★</button>
                </div>
                <textarea v-model="feedbackText" rows="2" maxlength="1000" placeholder="补充评价（可选）"></textarea>
                <button type="button" class="primary-button" :disabled="!feedbackRating || feedbackSending" @click="submitFeedback">
                  提交评价
                </button>
              </div>
              <div v-else-if="current?.status === 'ARCHIVED' && current?.rating" class="rated-tip">
                已评价 {{ current.rating }} 分，感谢你的反馈 🌿
              </div>
            </div>

            <footer class="composer">
              <div class="composer-top">
                <span class="service-type-label">{{ typeLabels[type] }}</span>
                <span v-if="current" class="ticket-label">{{ current.ticketNo }}</span>
                <span v-else class="ticket-label">发送消息后创建工单</span>
              </div>
              <div class="composer-row">
                <textarea
                  v-model="input"
                  rows="1"
                  maxlength="5000"
                  placeholder="输入你想咨询的问题..."
                  @keydown="onKeydown"
                ></textarea>
                <button type="button" class="send-button" :disabled="!input.trim() || sending" @click="send">
                  <svg viewBox="0 0 24 24"><path d="m4 4 17 8-17 8 3.2-8L4 4Z" /><path d="M7.2 12H21" /></svg>
                </button>
              </div>
              <span class="composer-hint">超过 2 分钟未继续且工单未在进行中，将自动开启新工单</span>
            </footer>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.chat-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(23, 32, 51, 0.28);
  backdrop-filter: blur(3px);
}

.chat-sheet {
  display: flex;
  flex-direction: column;
  width: min(100%, 480px);
  height: min(86vh, 760px);
  overflow: hidden;
  border-radius: 24px 24px 0 0;
  background: #f7f9fd;
  box-shadow: 0 -20px 60px rgba(24, 40, 75, 0.22);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 14px 12px;
  border-bottom: 1px solid var(--portal-line);
  background: #ffffff;
}

.chat-heading {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.chat-heading strong {
  color: #182235;
  font-size: 15px;
}

.chat-heading span {
  color: var(--portal-muted);
  font-size: 10px;
}

.chat-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid #edf0f5;
  border-radius: 11px;
  color: #5b687e;
  background: #fafbfe;
  cursor: pointer;
}

.chat-icon svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.conversation {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.message-list {
  flex: 1;
  padding: 18px 16px 8px;
  overflow-y: auto;
}

.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 26px 12px 14px;
  text-align: center;
}

.welcome-orbit {
  display: grid;
  place-items: center;
  width: 62px;
  height: 62px;
  border: 1px solid #e4e9ff;
  border-radius: 21px;
  color: #4f6bfb;
  background: linear-gradient(145deg, #eef2ff, #ffffff);
  box-shadow: 0 10px 30px rgba(79, 107, 251, 0.12);
}

.welcome-orbit svg {
  width: 29px;
  height: 29px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.5;
}

.chat-welcome h2 {
  margin: 16px 0 6px;
  color: #172033;
  font-size: 17px;
}

.chat-welcome p {
  max-width: 280px;
  margin: 0;
  color: var(--portal-muted);
  font-size: 11px;
  line-height: 1.7;
}

.type-switch {
  display: flex;
  gap: 5px;
  margin-top: 18px;
  padding: 4px;
  border-radius: 12px;
  background: #eef2f8;
}

.type-switch button {
  height: 31px;
  padding: 0 14px;
  border: 0;
  border-radius: 9px;
  color: #8792a5;
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}

.type-switch button.active {
  color: #4f6bfb;
  background: #ffffff;
  box-shadow: 0 3px 10px rgba(58, 80, 130, 0.1);
  font-weight: 650;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 7px;
  margin-top: 18px;
}

.suggestions button {
  padding: 8px 11px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  color: #667389;
  background: #ffffff;
  font-size: 10px;
  cursor: pointer;
}

.bubble-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 14px;
}

.bubble-row.portal {
  align-items: flex-end;
}

.bubble-row.system {
  align-items: center;
}

.bubble-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  color: #9ba4b5;
  font-size: 9px;
}

.bubble {
  max-width: 78%;
  padding: 10px 13px;
  border: 1px solid #e9edf4;
  border-radius: 4px 14px 14px 14px;
  color: #354054;
  background: #ffffff;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-row.portal .bubble {
  border-color: transparent;
  border-radius: 14px 4px 14px 14px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #7658ff);
}

.bubble-row.system .bubble {
  color: #8d96a8;
  background: #eef1f6;
  font-size: 9px;
}

.rating-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 10px 0 16px;
  padding: 14px;
  border: 1px solid #ebe6ff;
  border-radius: 15px;
  background: #fbfaff;
}

.rating-card strong {
  color: #3f356d;
  font-size: 12px;
}

.rating-card span {
  color: #988fae;
  font-size: 10px;
}

.stars {
  display: flex;
  gap: 3px;
}

.stars button {
  padding: 0;
  border: 0;
  color: #ddd6ed;
  background: transparent;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
}

.stars button.active {
  color: #f4b740;
}

.rating-card textarea {
  width: 100%;
  padding: 9px 11px;
  border: 1px solid #e9e4f5;
  border-radius: 10px;
  color: #4b435f;
  background: #ffffff;
  outline: none;
  resize: none;
  font-size: 11px;
}

.rating-card .primary-button {
  align-self: flex-end;
  height: 36px;
  padding: 0 15px;
  font-size: 11px;
}

.rated-tip {
  padding: 10px 12px;
  border-radius: 11px;
  color: #5f8a70;
  background: #effaf2;
  font-size: 10px;
  text-align: center;
}

.composer {
  padding: 8px 14px calc(12px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--portal-line);
  background: #ffffff;
}

.composer-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1px 2px 7px;
}

.service-type-label {
  padding: 3px 8px;
  border-radius: 6px;
  color: #4f6bfb;
  background: #eef2ff;
  font-size: 9px;
}

.ticket-label {
  overflow: hidden;
  max-width: 65%;
  color: #a0a9b8;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.composer-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.composer-row textarea {
  flex: 1;
  min-height: 42px;
  max-height: 96px;
  padding: 11px 13px;
  border: 1px solid #e7ecf4;
  border-radius: 14px;
  color: #263042;
  background: #f8fafe;
  outline: none;
  resize: none;
  font-size: 12px;
  line-height: 1.5;
}

.send-button {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border: 0;
  border-radius: 13px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #7658ff);
  box-shadow: 0 7px 16px rgba(79, 107, 251, 0.25);
  cursor: pointer;
}

.send-button:disabled {
  opacity: 0.42;
  box-shadow: none;
}

.send-button svg {
  width: 19px;
  height: 19px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.composer-hint {
  display: block;
  margin-top: 6px;
  color: #b1b8c4;
  font-size: 9px;
  text-align: center;
}

.history-panel {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.history-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.history-heading strong {
  display: block;
  color: #182235;
  font-size: 14px;
}

.history-heading span {
  display: block;
  margin-top: 4px;
  color: var(--portal-muted);
  font-size: 10px;
}

.text-button {
  padding: 4px;
  border: 0;
  color: #4f6bfb;
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}

.loading-line {
  padding: 30px 0;
  color: var(--portal-muted);
  font-size: 11px;
  text-align: center;
}

.history-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  margin-bottom: 8px;
  padding: 12px 13px;
  border: 1px solid #e9edf4;
  border-radius: 13px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
}

.history-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.history-card strong {
  overflow: hidden;
  color: #263042;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-card > span {
  color: #9aa4b4;
  font-size: 9px;
}

.status-pill {
  padding: 2px 6px;
  border-radius: 6px;
  color: #8d96a8;
  background: #f1f3f7;
  font-size: 9px;
  white-space: nowrap;
}

.status-pill.assigned,
.status-pill.processing {
  color: #4f6bfb;
  background: #eef2ff;
}

.status-pill.archived {
  color: #5f8a70;
  background: #effaf2;
}

.chat-fade-enter-active,
.chat-fade-leave-active {
  transition: opacity 0.18s ease;
}

.chat-fade-enter-active .chat-sheet,
.chat-fade-leave-active .chat-sheet {
  transition: transform 0.2s ease;
}

.chat-fade-enter-from,
.chat-fade-leave-to {
  opacity: 0;
}

.chat-fade-enter-from .chat-sheet,
.chat-fade-leave-to .chat-sheet {
  transform: translateY(24px);
}
</style>
