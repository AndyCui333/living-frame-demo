<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, CircleCheck, Promotion, Refresh, Switch, Tickets } from '@element-plus/icons-vue'
import {
  archiveCsTicket,
  dispatchCsTicket,
  getCsAgents,
  getCsTicketDetail,
  getCsTickets,
  replyCsTicket,
  transferCsTicket,
} from '@/api/customerService'
import type { CsAgent, CsMessage, CsTicket } from '@/types/api'

const tickets = ref<CsTicket[]>([])
const activeTicket = ref<CsTicket | null>(null)
const messages = ref<CsMessage[]>([])
const loading = ref(false)
const detailLoading = ref(false)
const sending = ref(false)
const reply = ref('')
const transferVisible = ref(false)
const transferLoading = ref(false)
const agents = ref<CsAgent[]>([])
const selectedAgentId = ref('')
const messagesEl = ref<HTMLElement>()

const activeId = computed(() => activeTicket.value?.id)
const canReply = computed(() => activeTicket.value !== null
  && ['ASSIGNED', 'PROCESSING'].includes(activeTicket.value.status))
const canTransfer = computed(() => activeTicket.value !== null
  && ['ASSIGNED', 'PROCESSING'].includes(activeTicket.value.status))
const canArchive = computed(() => activeTicket.value !== null
  && ['ASSIGNED', 'PROCESSING'].includes(activeTicket.value.status))
const canDispatch = computed(() => activeTicket.value?.status === 'PENDING')

const statusLabels: Record<string, string> = {
  PENDING: '待派遣',
  ASSIGNED: '已接单',
  PROCESSING: '处理中',
  ARCHIVED: '已归档',
}

function statusType(status: string): 'warning' | 'primary' | 'success' | 'info' {
  if (status === 'PENDING') return 'warning'
  if (status === 'ASSIGNED') return 'primary'
  if (status === 'PROCESSING') return 'success'
  return 'info'
}

function typeLabel(type: string): string {
  return type === 'TECH' ? '技术客服' : '在线聊天'
}

function formatTime(value?: string | null): string {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  }).format(date)
}

async function loadTickets(keepActive = true) {
  loading.value = true
  try {
    tickets.value = await getCsTickets()
    const current = keepActive ? tickets.value.find((item) => item.id === activeId.value) : undefined
    const next = current ?? tickets.value[0]
    if (next) {
      await openTicket(next)
    } else {
      activeTicket.value = null
      messages.value = []
    }
  } finally {
    loading.value = false
  }
}

async function openTicket(ticket: CsTicket) {
  activeTicket.value = ticket
  detailLoading.value = true
  try {
    const detail = await getCsTicketDetail(ticket.id)
    activeTicket.value = detail.ticket
    messages.value = detail.messages
    await scrollToBottom()
  } finally {
    detailLoading.value = false
  }
}

async function sendReply() {
  const content = reply.value.trim()
  if (!content || !activeTicket.value || sending.value) return
  sending.value = true
  try {
    const message = await replyCsTicket(activeTicket.value.id, content)
    messages.value.push(message)
    reply.value = ''
    await scrollToBottom()
    await loadTickets()
  } finally {
    sending.value = false
  }
}

async function openTransfer() {
  if (!activeTicket.value) return
  transferLoading.value = true
  transferVisible.value = true
  selectedAgentId.value = ''
  try {
    agents.value = await getCsAgents(activeTicket.value.ticketType)
  } finally {
    transferLoading.value = false
  }
}

async function confirmTransfer() {
  if (!activeTicket.value || !selectedAgentId.value) {
    ElMessage.warning('请选择转接客服')
    return
  }
  await transferCsTicket(activeTicket.value.id, selectedAgentId.value)
  ElMessage.success('工单已转接')
  transferVisible.value = false
  await loadTickets()
}

async function confirmArchive() {
  if (!activeTicket.value) return
  try {
    await ElMessageBox.confirm('截单后工单将归档，门户用户可以进行评价。确定继续吗？', '截单归档', {
      type: 'warning',
      confirmButtonText: '截单归档',
      cancelButtonText: '取消',
    })
    await archiveCsTicket(activeTicket.value.id)
    ElMessage.success('工单已归档')
    await loadTickets()
  } catch {
    // 用户取消
  }
}

async function dispatchTicket() {
  if (!activeTicket.value) return
  await dispatchCsTicket(activeTicket.value.id)
  ElMessage.success('已重新派遣')
  await loadTickets()
}

async function scrollToBottom() {
  await new Promise((resolve) => setTimeout(resolve, 0))
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

onMounted(() => loadTickets(false))
</script>

<template>
  <div class="page cs-chat-page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">客服聊天</h1>
        <p class="page-subtitle">处理分配给你的客服会话，支持转接、截单归档和实时回复。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadTickets()">刷新</el-button>
    </div>

    <div class="cs-chat-shell">
      <aside class="ticket-list" v-loading="loading">
        <div class="ticket-list-head">
          <span>待处理会话</span>
          <el-tag size="small" effect="plain" round>{{ tickets.length }}</el-tag>
        </div>
        <button
          v-for="ticket in tickets"
          :key="ticket.id"
          type="button"
          class="ticket-item"
          :class="{ active: ticket.id === activeId }"
          @click="openTicket(ticket)"
        >
          <div class="ticket-item-top">
            <strong>{{ ticket.title }}</strong>
            <el-tag :type="statusType(ticket.status)" size="small" effect="plain">
              {{ statusLabels[ticket.status] ?? ticket.status }}
            </el-tag>
          </div>
          <span class="ticket-item-meta">
            {{ ticket.ticketNo }} · {{ ticket.portalUserName }} · {{ typeLabel(ticket.ticketType) }}
          </span>
          <span class="ticket-item-time">{{ formatTime(ticket.lastMessageAt || ticket.updatedAt) }}</span>
        </button>
        <el-empty v-if="!tickets.length" description="暂时没有待处理会话" :image-size="70" />
      </aside>

      <section class="chat-panel">
        <template v-if="activeTicket">
          <header class="chat-panel-head">
            <div>
              <div class="chat-title-line">
                <h2>{{ activeTicket.title }}</h2>
                <el-tag :type="statusType(activeTicket.status)" size="small" effect="plain">
                  {{ statusLabels[activeTicket.status] ?? activeTicket.status }}
                </el-tag>
              </div>
              <p>
                {{ activeTicket.ticketNo }} · {{ activeTicket.portalUserName }}
                · {{ typeLabel(activeTicket.ticketType) }}
                <template v-if="activeTicket.assignedAgentName"> · 客服 {{ activeTicket.assignedAgentName }}</template>
              </p>
            </div>
            <div class="chat-head-actions">
              <el-button
                v-if="canDispatch"
                v-permission="'cs:ticket:dispatch'"
                type="primary"
                plain
                :icon="Tickets"
                @click="dispatchTicket"
              >
                重新派遣
              </el-button>
              <el-button
                v-if="canTransfer"
                v-permission="'cs:ticket:transfer'"
                :icon="Switch"
                @click="openTransfer"
              >
                转接
              </el-button>
              <el-button
                v-if="canArchive"
                v-permission="'cs:ticket:close'"
                type="warning"
                plain
                :icon="CircleCheck"
                @click="confirmArchive"
              >
                截单归档
              </el-button>
            </div>
          </header>

          <div ref="messagesEl" v-loading="detailLoading" class="message-stream">
            <div
              v-for="message in messages"
              :key="message.id"
              class="cs-message"
              :class="message.senderType.toLowerCase()"
            >
              <div class="cs-message-meta">
                <span>{{ message.senderName }}</span>
                <span>{{ formatTime(message.createdAt) }}</span>
              </div>
              <div class="cs-message-bubble">{{ message.content }}</div>
            </div>
            <el-empty v-if="!detailLoading && !messages.length" description="还没有消息" :image-size="60" />
          </div>

          <footer class="reply-box">
            <el-input
              v-model="reply"
              type="textarea"
              :rows="3"
              maxlength="5000"
              show-word-limit
              resize="none"
              placeholder="回复门户用户..."
              :disabled="!canReply || sending"
              @keydown.enter.exact.prevent="sendReply"
            />
            <div class="reply-actions">
              <span class="reply-hint">
                <el-icon><ChatDotRound /></el-icon>
                Enter 发送，Shift + Enter 换行
              </span>
              <el-button
                v-permission="'cs:chat:reply'"
                type="primary"
                :icon="Promotion"
                :loading="sending"
                :disabled="!canReply || !reply.trim()"
                @click="sendReply"
              >
                发送
              </el-button>
            </div>
          </footer>
        </template>
        <el-empty v-else description="请选择左侧会话开始处理" :image-size="90" />
      </section>
    </div>

    <el-dialog v-model="transferVisible" title="转接工单" width="460px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="仅可转接给同客服类型且空闲的坐席"
        class="transfer-tip"
      />
      <el-select v-model="selectedAgentId" v-loading="transferLoading" placeholder="选择客服人员" style="width: 100%">
        <el-option
          v-for="agent in agents"
          :key="agent.userId"
          :label="`${agent.name} · ${typeLabel(agent.serviceType)} · ${agent.onlineStatus === 'IDLE' ? '空闲' : agent.onlineStatus === 'BUSY' ? '忙碌' : '离线'}`"
          :value="agent.userId"
          :disabled="agent.onlineStatus !== 'IDLE' || agent.userId === activeTicket?.assignedAgentId"
        >
          <div class="agent-option">
            <span>{{ agent.name }} · {{ typeLabel(agent.serviceType) }}</span>
            <el-tag size="small" :type="agent.onlineStatus === 'IDLE' ? 'success' : 'info'" effect="plain">
              {{ agent.onlineStatus === 'IDLE' ? '空闲' : agent.onlineStatus === 'BUSY' ? '忙碌' : '离线' }}
            </el-tag>
          </div>
        </el-option>
      </el-select>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedAgentId" @click="confirmTransfer">确认转接</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cs-chat-page {
  height: calc(100vh - 116px);
  min-height: 620px;
}

.cs-chat-shell {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--lf-border);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: var(--lf-shadow);
}

.ticket-list {
  width: 320px;
  flex: 0 0 320px;
  overflow-y: auto;
  border-right: 1px solid var(--lf-border);
  background: #fbfcff;
}

.ticket-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--lf-border);
  color: #263042;
  font-size: 13px;
  font-weight: 600;
}

.ticket-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-bottom: 1px solid #f1f3f8;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
}

.ticket-item:hover,
.ticket-item.active {
  background: #eef2ff;
}

.ticket-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ticket-item-top strong {
  overflow: hidden;
  color: #263042;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-item-meta,
.ticket-item-time {
  overflow: hidden;
  color: #9aa3b5;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.chat-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--lf-border);
}

.chat-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-title-line h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.chat-panel-head p {
  margin: 6px 0 0;
  color: #9aa3b5;
  font-size: 11px;
}

.chat-head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.message-stream {
  flex: 1;
  padding: 22px;
  overflow-y: auto;
  background:
    radial-gradient(circle at 10% 0%, rgba(79, 107, 251, 0.04), transparent 30%),
    #ffffff;
}

.cs-message {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 16px;
}

.cs-message.agent {
  align-items: flex-end;
}

.cs-message.system {
  align-items: center;
}

.cs-message-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  color: #9aa3b5;
  font-size: 10px;
}

.cs-message-bubble {
  max-width: 72%;
  padding: 10px 13px;
  border: 1px solid #edf0f6;
  border-radius: 4px 12px 12px 12px;
  color: #344054;
  background: #f7f8fc;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.cs-message.agent .cs-message-bubble {
  border-color: transparent;
  border-radius: 12px 4px 12px 12px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #6d5cf6);
}

.cs-message.system .cs-message-bubble {
  color: #8d96a8;
  background: #f4f5f8;
  font-size: 10px;
}

.reply-box {
  padding: 14px 22px 18px;
  border-top: 1px solid var(--lf-border);
}

.reply-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.reply-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #9aa3b5;
  font-size: 10px;
}

.transfer-tip {
  margin-bottom: 14px;
}

.agent-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

@media (max-width: 900px) {
  .cs-chat-shell {
    flex-direction: column;
  }

  .ticket-list {
    width: 100%;
    flex: 0 0 220px;
    border-right: 0;
    border-bottom: 1px solid var(--lf-border);
  }
}
</style>
