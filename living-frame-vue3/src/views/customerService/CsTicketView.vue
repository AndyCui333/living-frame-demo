<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Refresh, Switch, Tickets, View } from '@element-plus/icons-vue'
import {
  archiveCsTicket,
  dispatchCsTicket,
  getCsAgents,
  getCsTicketDetail,
  getCsTickets,
  transferCsTicket,
} from '@/api/customerService'
import { useAuthStore } from '@/stores/auth'
import type { CsAgent, CsMessage, CsTicket } from '@/types/api'

const auth = useAuthStore()
const rows = ref<CsTicket[]>([])
const loading = ref(false)
const statusFilter = ref('')
const typeFilter = ref('')
const transferVisible = ref(false)
const transferLoading = ref(false)
const activeTicket = ref<CsTicket | null>(null)
const agents = ref<CsAgent[]>([])
const selectedAgentId = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailMessages = ref<CsMessage[]>([])

const isSuperAdmin = computed(() => auth.isSuperAdmin)
const filteredRows = computed(() => rows.value.filter((row) => {
  if (statusFilter.value && row.status !== statusFilter.value) return false
  if (typeFilter.value && row.ticketType !== typeFilter.value) return false
  return true
}))

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
  if (!value) return '—'
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  }).format(date)
}

async function loadTickets() {
  loading.value = true
  try {
    rows.value = await getCsTickets()
  } finally {
    loading.value = false
  }
}

async function openTransfer(ticket: CsTicket) {
  activeTicket.value = ticket
  transferVisible.value = true
  transferLoading.value = true
  selectedAgentId.value = ''
  try {
    agents.value = await getCsAgents(ticket.ticketType)
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

async function archive(ticket: CsTicket) {
  try {
    await ElMessageBox.confirm(
      `确定对工单「${ticket.title}」执行截单归档吗？归档后门户用户可评价。`,
      '截单归档',
      { type: 'warning', confirmButtonText: '截单归档', cancelButtonText: '取消' },
    )
    await archiveCsTicket(ticket.id)
    ElMessage.success('工单已归档')
    await loadTickets()
  } catch {
    // 用户取消
  }
}

async function dispatch(ticket: CsTicket) {
  await dispatchCsTicket(ticket.id)
  ElMessage.success('工单已派遣')
  await loadTickets()
}

async function openDetail(ticket: CsTicket) {
  detailVisible.value = true
  detailLoading.value = true
  activeTicket.value = ticket
  try {
    const detail = await getCsTicketDetail(ticket.id)
    activeTicket.value = detail.ticket
    detailMessages.value = detail.messages
  } finally {
    detailLoading.value = false
  }
}

function canOperate(ticket: CsTicket): boolean {
  return ['ASSIGNED', 'PROCESSING'].includes(ticket.status)
    && (isSuperAdmin.value || ticket.assignedAgentId === auth.user?.id)
}

onMounted(loadTickets)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">工单流程</h1>
        <p class="page-subtitle">查看工单流转、转接客服、截单归档，门户用户可在归档后评价。</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadTickets">刷新</el-button>
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="statusFilter" placeholder="工单状态" clearable style="width: 140px">
            <el-option label="待派遣" value="PENDING" />
            <el-option label="已接单" value="ASSIGNED" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
          <el-select v-if="isSuperAdmin" v-model="typeFilter" placeholder="客服类型" clearable style="width: 140px">
            <el-option label="在线聊天" value="CHAT" />
            <el-option label="技术客服" value="TECH" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <span class="text-muted">共 {{ filteredRows.length }} 个工单</span>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="filteredRows" row-key="id">
          <el-table-column prop="ticketNo" label="工单编号" width="160" />
          <el-table-column label="标题" min-width="220">
            <template #default="{ row }">
              <div class="ticket-title-cell">
                <strong>{{ row.title }}</strong>
                <span>{{ row.portalUserName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="105">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ typeLabel(row.ticketType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="105">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small" effect="plain">
                {{ statusLabels[row.status] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="接单客服" width="130">
            <template #default="{ row }">{{ row.assignedAgentName || '待派遣' }}</template>
          </el-table-column>
          <el-table-column label="更新时间" width="130">
            <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="详情" placement="top">
                <el-button link type="primary" :icon="View" @click="openDetail(row as CsTicket)" />
              </el-tooltip>
              <el-button
                v-if="row.status === 'PENDING'"
                v-permission="'cs:ticket:dispatch'"
                link
                type="primary"
                :icon="Tickets"
                @click="dispatch(row as CsTicket)"
              >
                派遣
              </el-button>
              <el-button
                v-if="canOperate(row as CsTicket)"
                v-permission="'cs:ticket:transfer'"
                link
                type="primary"
                :icon="Switch"
                @click="openTransfer(row as CsTicket)"
              >
                转接
              </el-button>
              <el-button
                v-if="canOperate(row as CsTicket)"
                v-permission="'cs:ticket:close'"
                link
                type="warning"
                :icon="CircleCheck"
                @click="archive(row as CsTicket)"
              >
                归档
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂时没有工单" :image-size="70" />
          </template>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="transferVisible" title="转接工单" width="460px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon title="仅可转接给同客服类型且空闲的坐席" class="transfer-tip" />
      <el-select v-model="selectedAgentId" v-loading="transferLoading" placeholder="选择客服人员" style="width: 100%">
        <el-option
          v-for="agent in agents"
          :key="agent.userId"
          :label="`${agent.name} · ${agent.onlineStatus === 'IDLE' ? '空闲' : agent.onlineStatus === 'BUSY' ? '忙碌' : '离线'}`"
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

    <el-drawer v-model="detailVisible" title="工单详情" size="500px">
      <div v-loading="detailLoading" class="ticket-detail">
        <template v-if="activeTicket">
          <h2>{{ activeTicket.title }}</h2>
          <div class="detail-tags">
            <el-tag :type="statusType(activeTicket.status)" effect="plain">
              {{ statusLabels[activeTicket.status] ?? activeTicket.status }}
            </el-tag>
            <el-tag effect="plain">{{ typeLabel(activeTicket.ticketType) }}</el-tag>
            <el-tag effect="plain" type="info">{{ activeTicket.ticketNo }}</el-tag>
          </div>
          <p class="ticket-content">{{ activeTicket.content }}</p>
          <div class="ticket-info">
            <span>门户用户：{{ activeTicket.portalUserName }}</span>
            <span>接单客服：{{ activeTicket.assignedAgentName || '待派遣' }}</span>
            <span>更新时间：{{ formatTime(activeTicket.updatedAt) }}</span>
            <span v-if="activeTicket.rating">评价：{{ activeTicket.rating }} 分</span>
          </div>
          <el-divider content-position="left">流转记录</el-divider>
          <div v-for="message in detailMessages" :key="message.id" class="timeline-message">
            <strong>{{ message.senderName }}</strong>
            <span>{{ message.content }}</span>
            <small>{{ formatTime(message.createdAt) }}</small>
          </div>
          <el-empty v-if="!detailMessages.length" description="暂无消息" :image-size="60" />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.heading-actions {
  display: flex;
  gap: 8px;
}

.ticket-title-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.ticket-title-cell strong {
  color: #263042;
  font-size: 12px;
}

.ticket-title-cell span {
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

.ticket-detail h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.detail-tags {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.ticket-content {
  margin: 18px 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.ticket-info {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 14px;
  border-radius: 10px;
  background: #f8f9fc;
  color: #6b7280;
  font-size: 12px;
}

.timeline-message {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f3f8;
}

.timeline-message strong {
  color: #263042;
  font-size: 12px;
}

.timeline-message span {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.timeline-message small {
  color: #9aa3b5;
  font-size: 10px;
}
</style>
