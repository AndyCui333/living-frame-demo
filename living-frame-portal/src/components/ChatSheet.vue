<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { portalApi } from '../api'
import { chatState } from '../chatState'
import { loadDrafts, removeDraft, saveDraft } from '../drafts'
import { session } from '../session'

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
const drafts = ref([])
const activeDraftId = ref('')
let polling = null

const isOpen = computed(() => chatState.open)
const title = computed(() => (type.value === 'TECH' ? '技术客服' : '在线客服'))
const displayTitle = computed(() => (historyVisible.value ? '我的工单' : title.value))
const statusText = computed(() => {
  if (historyVisible.value) return '查看全部工单记录'
  if (!current.value) return '新建对话'
  return statusLabels[current.value.status] || current.value.status
})
const canRate = computed(() => current.value?.status === 'ARCHIVED' && !current.value?.rating)

const statusLabels = {
  DRAFT: '草稿',
  PENDING: '等待接单',
  ASSIGNED: '已接单',
  PROCESSING: '沟通中',
  WAIT_CONFIRM: '待确认完结',
  ARCHIVED: '已结束',
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

function isUnfinished(ticket) {
  return ticket.status !== 'ARCHIVED' && ticket.status !== 'DRAFT'
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
  await portalApi.markTicketRead(ticketId).catch(() => undefined)
  window.dispatchEvent(new Event('notifications-updated'))
  await scrollToBottom()
}

async function boot() {
  loading.value = true
  historyVisible.value = chatState.showHistory
  type.value = chatState.initialType || 'CHAT'
  current.value = null
  messages.value = []
  drafts.value = loadDrafts(session.user?.id)
  activeDraftId.value = ''
  try {
    const list = await loadTickets()
    const targetTicketId = chatState.targetTicketId
    chatState.targetTicketId = null
    if (!historyVisible.value && targetTicketId) {
      await loadDetail(targetTicketId)
    } else if (!historyVisible.value) {
      const active = list
        .filter((ticket) => isUnfinished(ticket))
        .sort((a, b) => String(b.updatedAt).localeCompare(String(a.updatedAt)))[0]
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
    if (!['PENDING', 'ASSIGNED', 'PROCESSING', 'WAIT_CONFIRM'].includes(current.value.status)) return
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
  const ticketId = current.value?.id
  chatState.open = false
  stopPolling()
  if (ticketId) {
    void portalApi.leaveTicket(ticketId).catch(() => undefined)
  }
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

function newConversation() {
  type.value = 'CHAT'
  current.value = null
  messages.value = []
  input.value = ''
  activeDraftId.value = ''
  feedbackRating.value = 0
  feedbackText.value = ''
}

function saveCurrentDraft(showMessage = true) {
  const content = input.value.trim()
  if (!content) {
    if (showMessage) ElMessage.warning('请先输入内容再保存草稿')
    return
  }
  try {
    const draft = saveDraft(session.user?.id, {
      id: activeDraftId.value || undefined,
      title: content.slice(0, 36),
      content,
      ticketType: type.value,
    })
    activeDraftId.value = draft.id
    drafts.value = loadDrafts(session.user?.id)
    if (showMessage) ElMessage.success('草稿已保存到当前登录会话')
  } catch (error: any) {
    ElMessage.error(error.message || '草稿保存失败')
  }
}

function useDraft(draft) {
  input.value = draft.content
  type.value = draft.ticketType === 'TECH' ? 'TECH' : 'CHAT'
  current.value = null
  messages.value = []
  activeDraftId.value = draft.id
  historyVisible.value = false
  feedbackRating.value = 0
  feedbackText.value = ''
}

function deleteDraft(draft) {
  drafts.value = removeDraft(session.user?.id, draft.id)
  if (activeDraftId.value === draft.id) {
    activeDraftId.value = ''
  }
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
    if (activeDraftId.value) {
      drafts.value = removeDraft(session.user?.id, activeDraftId.value)
      activeDraftId.value = ''
    }
    await loadTickets()
    await scrollToBottom()
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
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
  } catch (error: any) {
    ElMessage.error(error.message || '评价失败')
  } finally {
    feedbackSending.value = false
  }
}

async function confirmCompletion() {
  if (!current.value) return
  try {
    await ElMessageBox.confirm('确认服务已经完成？确认后本次工单将结束，并进入评价。', '确认完结', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await portalApi.confirmComplete(current.value.id)
    await loadDetail(current.value.id)
    await loadTickets()
  } catch (error: any) {
    ElMessage.error(error.message || '确认完结失败')
  }
}

async function rejectCompletion() {
  if (!current.value) return
  try {
    await portalApi.rejectComplete(current.value.id)
    await loadDetail(current.value.id)
    await loadTickets()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
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

watch(
  () => chatState.open,
  (open, wasOpen) => {
    if (wasOpen && !open && input.value.trim()) {
      saveCurrentDraft(false)
    }
  },
)

onUnmounted(() => {
  stopPolling()
  const ticketId = current.value?.id
  if (ticketId) {
    void portalApi.leaveTicket(ticketId).catch(() => undefined)
  }
})
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
              <strong>{{ displayTitle }}</strong>
              <span>{{ statusText }}</span>
            </div>
            <button
              type="button"
              class="chat-icon"
              :aria-label="historyVisible ? '返回对话' : '我的工单'"
              :title="historyVisible ? '返回对话' : '我的工单'"
              @click="historyVisible ? (historyVisible = false) : openHistory()"
            >
              <svg v-if="historyVisible" viewBox="0 0 24 24">
                <path d="M20 11.5a7 7 0 0 1-10.2 6.2L4 19l1.3-5.8A7 7 0 1 1 20 11.5Z" />
                <path d="M8.5 8.7h7M8.5 11.7h5" />
              </svg>
              <svg v-else viewBox="0 0 24 24">
                <path d="M4 12a8 8 0 1 0 2.4-5.7" />
                <path d="M4 5v4.5h4.5" />
                <path d="M12 8v4l3 2" />
              </svg>
            </button>
          </header>

          <div v-if="historyVisible" class="history-panel">
            <div v-if="drafts.length" class="draft-section">
              <div class="draft-heading">
                <strong>草稿箱</strong>
                <span>仅保存在当前登录浏览器，退出登录后清除</span>
              </div>
              <div v-for="draft in drafts" :key="draft.id" class="draft-card">
                <button type="button" class="draft-main" @click="useDraft(draft)">
                  <strong>{{ draft.title }}</strong>
                  <span>{{ draft.content }}</span>
                </button>
                <button type="button" class="draft-delete" aria-label="删除草稿" @click="deleteDraft(draft)">×</button>
              </div>
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
                <p>发送你的问题，我们会为你创建工单并安排在线客服。</p>
                <div class="type-static">在线客服</div>
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

              <div v-if="current?.status === 'WAIT_CONFIRM'" class="complete-card">
                <strong>客服申请结束本次服务</strong>
                <span>确认后工单才会结束并进入评价；如果还需要继续沟通，请选择“继续处理”。</span>
                <div class="complete-actions">
                  <button type="button" class="primary-button" @click="confirmCompletion">确认完成</button>
                  <button type="button" class="ghost-button" @click="rejectCompletion">继续处理</button>
                </div>
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
                <el-input
                  v-model="feedbackText"
                  type="textarea"
                  :rows="2"
                  maxlength="1000"
                  placeholder="补充评价（可选）"
                  class="feedback-input"
                />
                <el-button type="primary" class="primary-button" :loading="feedbackSending" :disabled="!feedbackRating" @click="submitFeedback">
                  提交评价
                </el-button>
              </div>
              <div v-else-if="current?.status === 'ARCHIVED' && current?.rating" class="rated-tip">
                已评价 {{ current.rating }} 分，感谢你的反馈 🌿
              </div>
            </div>

            <footer v-if="current?.status !== 'ARCHIVED'" class="composer">
              <div class="composer-top">
                <span class="service-type-label">{{ typeLabels[type] }}</span>
                <span v-if="current" class="ticket-label">{{ current.ticketNo }}</span>
                <span v-else class="ticket-label">发送消息后创建工单</span>
                <el-button link type="primary" class="save-draft-button" @click="saveCurrentDraft()">存草稿</el-button>
              </div>
              <div class="composer-row">
                <el-input
                  v-model="input"
                  type="textarea"
                  :autosize="{ minRows: 1, maxRows: 3 }"
                  maxlength="5000"
                  placeholder="输入你想咨询的问题..."
                  class="chat-composer-input"
                  @keydown="onKeydown"
                />
                <el-button
                  type="primary"
                  class="send-button"
                  :loading="sending"
                  :disabled="!input.trim()"
                  aria-label="发送消息"
                  @click="send"
                >
                  <svg v-if="!sending" viewBox="0 0 24 24"><path d="m4 4 17 8-17 8 3.2-8L4 4Z" /><path d="M7.2 12H21" /></svg>
                </el-button>
              </div>
              <span class="composer-hint">工单未结束前不会创建新工单；结束后再次咨询会开启新工单</span>
            </footer>
            <footer v-else class="ended-footer">
              <div>
                <strong>该工单已结束</strong>
                <span>继续咨询需要发起一个新工单</span>
              </div>
              <button type="button" class="primary-button" @click="newConversation">发起新工单</button>
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

.type-static {
  margin-top: 14px;
  padding: 5px 12px;
  border-radius: 9px;
  color: #4f6bfb;
  background: #eef2ff;
  font-size: 10px;
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

.complete-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 10px 0 16px;
  padding: 14px;
  border: 1px solid #f0dfb8;
  border-radius: 15px;
  background: #fffbf0;
}

.complete-card strong {
  color: #72552a;
  font-size: 12px;
}

.complete-card span {
  color: #9b8868;
  font-size: 10px;
  line-height: 1.6;
}

.complete-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.complete-card .primary-button,
.complete-card .ghost-button {
  height: 34px;
  padding: 0 12px;
  font-size: 10px;
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

.feedback-input :deep(.el-textarea__inner) {
  padding: 9px 11px;
  border: 1px solid #e9e4f5;
  border-radius: 10px;
  color: #4b435f;
  background: #ffffff;
  box-shadow: none;
  resize: none;
  font-size: 11px;
}

.composer-row .chat-composer-input {
  flex: 1;
  min-width: 0;
}

.composer-row .chat-composer-input :deep(.el-textarea__inner) {
  min-height: 42px !important;
  max-height: 96px;
  padding: 11px 13px;
  border: 1px solid #e7ecf4;
  border-radius: 14px;
  color: #263042;
  background: #f8fafe;
  box-shadow: none;
  resize: none;
  font-size: 12px;
  line-height: 1.5;
}

.send-button {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 13px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #7658ff);
  box-shadow: 0 7px 16px rgba(79, 107, 251, 0.25);
}

.send-button:disabled {
  opacity: 0.42;
  box-shadow: none;
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
  max-width: 55%;
  color: #a0a9b8;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.save-draft-button {
  margin-left: auto;
  padding: 3px 0 3px 8px;
  border: 0;
  color: #4f6bfb;
  background: transparent;
  font-size: 9px;
  cursor: pointer;
}

.draft-section {
  margin-bottom: 16px;
}

.draft-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8px;
}

.draft-heading strong {
  color: #3d4960;
  font-size: 12px;
}

.draft-heading span {
  color: #a0a9b8;
  font-size: 9px;
}

.draft-card {
  display: flex;
  align-items: stretch;
  gap: 6px;
  margin-bottom: 7px;
  border: 1px dashed #dce2ee;
  border-radius: 12px;
  background: #fbfcff;
}

.draft-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  padding: 10px 11px;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.draft-main strong {
  overflow: hidden;
  color: #3d4960;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-main span {
  overflow: hidden;
  color: #9aa3b5;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-delete {
  width: 32px;
  border: 0;
  color: #b7bfcc;
  background: transparent;
  font-size: 18px;
  cursor: pointer;
}

.composer-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
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

.ended-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px calc(16px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--portal-line);
  background: #ffffff;
}

.ended-footer > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.ended-footer strong {
  color: #4b5563;
  font-size: 11px;
}

.ended-footer span {
  color: #9aa3b5;
  font-size: 9px;
}

.ended-footer .primary-button {
  height: 36px;
  padding: 0 13px;
  font-size: 10px;
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
