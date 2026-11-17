<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { portalApi } from '../api'
import { openChat } from '../chatState'

interface NotificationItem {
  id: string
  type: string
  title: string
  content: string
  targetType?: string
  targetId?: string | null
  read: boolean
  createdAt: string
}

const router = useRouter()
const loading = ref(false)
const filter = ref<'all' | 'unread' | 'read'>('all')
const notifications = ref<NotificationItem[]>([])
const selected = ref<string[]>([])
const unreadTotal = ref(0)

const filters = [
  { value: 'all', label: '全部' },
  { value: 'unread', label: '未读' },
  { value: 'read', label: '已读' },
] as const

const unreadCount = computed(() => unreadTotal.value)
const allSelected = computed(
  () => notifications.value.length > 0 && selected.value.length === notifications.value.length,
)

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/me')
  }
}

async function loadNotifications() {
  loading.value = true
  try {
    const [data, count] = await Promise.all([
      portalApi.notifications(filter.value),
      portalApi.unreadNotificationCount(),
    ])
    notifications.value = Array.isArray(data) ? (data as NotificationItem[]) : []
    unreadTotal.value = Number(count) || 0
    selected.value = []
  } finally {
    loading.value = false
  }
}

function switchFilter(value: string) {
  filter.value = value as 'all' | 'unread' | 'read'
  void loadNotifications()
}

function onTabChange(name: string | number) {
  switchFilter(String(name))
}

function toggleSelect(id: string) {
  if (selected.value.includes(id)) {
    selected.value = selected.value.filter((item) => item !== id)
  } else {
    selected.value = [...selected.value, id]
  }
}

function toggleAll() {
  selected.value = allSelected.value ? [] : notifications.value.map((item) => item.id)
}

function notifyChanged() {
  window.dispatchEvent(new Event('notifications-updated'))
}

async function markAllRead() {
  if (!unreadCount.value) return
  await portalApi.markAllNotificationsRead()
  await loadNotifications()
  notifyChanged()
  ElMessage.success('已全部标记为已读')
}

async function deleteSelected() {
  if (!selected.value.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条通知吗？`, '批量删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await portalApi.deleteNotifications(selected.value)
  await loadNotifications()
  notifyChanged()
  ElMessage.success('通知已删除')
}

async function openNotification(item: NotificationItem) {
  if (!item.read) {
    await portalApi.markNotificationRead(item.id).catch(() => undefined)
    item.read = true
    unreadTotal.value = Math.max(0, unreadTotal.value - 1)
    notifyChanged()
  }
  if (item.targetType === 'CS_TICKET' && item.targetId) {
    openChat('CHAT', false, item.targetId)
    await router.push('/')
  }
}

function formatTime(value: string) {
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

onMounted(loadNotifications)
</script>

<template>
  <div class="page-view notifications-page">
    <header class="notifications-header">
      <button type="button" class="back-button" aria-label="返回" @click="goBack">
        <svg viewBox="0 0 24 24"><path d="m15 5-7 7 7 7" /></svg>
      </button>
      <div class="notifications-title">
        <span class="eyebrow">NOTIFICATIONS</span>
        <h1>通知</h1>
        <p>{{ unreadCount }} 条未读消息</p>
      </div>
      <div class="header-actions">
        <el-button link type="primary" :disabled="!unreadCount" @click="markAllRead">全部已读</el-button>
        <el-button link type="danger" :disabled="!selected.length" @click="deleteSelected">批量删除</el-button>
      </div>
    </header>

    <div class="filter-tabs">
      <el-tabs v-model="filter" class="notification-tabs" @tab-change="onTabChange">
        <el-tab-pane v-for="item in filters" :key="item.value" :label="item.label" :name="item.value" />
      </el-tabs>
      <el-button link class="select-all" @click="toggleAll">
        {{ allSelected ? '取消全选' : '全选' }}
      </el-button>
    </div>

    <section class="notification-list card">
      <el-skeleton v-if="loading" :rows="3" animated class="notification-skeleton" />
      <template v-else-if="notifications.length">
        <article v-for="item in notifications" :key="item.id" class="notification-item" :class="{ unread: !item.read }">
          <el-checkbox
            class="notification-check"
            :model-value="selected.includes(item.id)"
            :aria-label="selected.includes(item.id) ? '取消选择' : '选择通知'"
            @update:model-value="toggleSelect(item.id)"
          />
          <button type="button" class="notification-main" @click="openNotification(item)">
            <span class="notification-icon">
              <svg viewBox="0 0 24 24">
                <path d="M5 6.5h14v9H9l-4 3v-12Z" />
                <path d="M8.5 10h7M8.5 13h4" />
              </svg>
            </span>
            <span class="notification-copy">
              <span class="notification-title-line">
                <strong>{{ item.title }}</strong>
                <i v-if="!item.read"></i>
              </span>
              <span class="notification-content">{{ item.content }}</span>
              <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
            </span>
            <svg class="notification-arrow" viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
          </button>
        </article>
      </template>
      <el-empty v-else description="暂无通知" :image-size="72">
        <p class="notification-empty-tip">客服回复你时，消息会出现在这里</p>
      </el-empty>
    </section>
  </div>
</template>

<style scoped>
.notifications-page {
  padding-top: calc(env(safe-area-inset-top, 0px) + 20px);
  padding-bottom: 28px;
}

.notifications-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.back-button {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  margin-top: 2px;
  padding: 0;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  color: #5b687e;
  background: #ffffff;
  cursor: pointer;
}

.back-button svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.notifications-title {
  flex: 1;
  min-width: 0;
}

.notifications-title h1 {
  margin: 4px 0 0;
  color: #172033;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.notifications-title p {
  margin: 4px 0 0;
  color: #9aa3b5;
  font-size: 10px;
}

.header-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  padding-top: 4px;
}

.header-actions :deep(.el-button) {
  margin-left: 0;
  font-size: 10px;
}

.filter-tabs {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 16px;
  padding: 4px 10px 4px 4px;
  border-radius: 12px;
  background: #eef2f8;
}

.notification-tabs {
  flex: 1;
  min-width: 0;
}

.notification-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.notification-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.notification-tabs :deep(.el-tabs__item) {
  height: 31px;
  padding: 0 14px;
  color: #8792a5;
  font-size: 10px;
  line-height: 31px;
}

.notification-tabs :deep(.el-tabs__item.is-active) {
  color: #4f6bfb;
  font-weight: 650;
}

.notification-tabs :deep(.el-tabs__active-bar) {
  height: 2px;
  border-radius: 999px;
  background: #4f6bfb;
}

.filter-tabs .select-all {
  margin-left: auto;
  color: #8a94a6;
  font-size: 10px;
}

.notification-list {
  margin-top: 12px;
  overflow: hidden;
  padding: 0;
}

.notification-skeleton {
  padding: 18px;
}

.notification-empty-tip {
  margin: 4px 0 0;
  color: #9aa3b5;
  font-size: 10px;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 12px 12px 9px;
  border-bottom: 1px solid #f1f3f7;
  background: #ffffff;
}

.notification-item:last-child {
  border-bottom: 0;
}

.notification-item.unread {
  background: linear-gradient(90deg, #f7f9ff, #ffffff);
}

.notification-check {
  flex: 0 0 18px;
  margin-right: 0;
  height: 18px;
}

.notification-check :deep(.el-checkbox__inner) {
  width: 18px;
  height: 18px;
  border-color: #d7deea;
  border-radius: 6px;
}

.notification-check :deep(.el-checkbox__inner::after) {
  left: 6px;
  top: 2px;
}

.notification-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.notification-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 11px;
  color: #4f6bfb;
  background: #eef2ff;
}

.notification-icon svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}

.notification-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.notification-title-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.notification-title-line strong {
  color: #303b50;
  font-size: 11px;
}

.notification-title-line i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #4f6bfb;
}

.notification-content {
  overflow: hidden;
  color: #8d96a8;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  color: #b0b8c4;
  font-size: 9px;
}

.notification-arrow {
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
  fill: none;
  stroke: #c0c8d4;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}

.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 56px 20px;
  color: #9aa3b5;
  font-size: 10px;
}

.notification-empty strong {
  color: #5f6b80;
  font-size: 12px;
}
</style>
