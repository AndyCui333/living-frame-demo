<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import { useRouter } from 'vue-router'
import { heartbeatCsAgent, offlineCsAgent } from '@/api/customerService'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
let timer: number | undefined
let alerted = false
let lastUnreadCount = 0

function isCustomerService(): boolean {
  return (auth.user?.roles ?? []).some((role) => role === 'CS_CHAT' || role === 'CS_TECH')
}

function goToTicket(ticketId: string) {
  void router.push({
    path: '/customer-service/chat',
    query: { ticket: ticketId },
  })
}

async function beat(showPopup = false) {
  if (!isCustomerService()) return
  try {
    const inbox = await heartbeatCsAgent()
    if (showPopup && !alerted && inbox.unreadCount > 0 && inbox.earliestUnreadTicketId) {
      alerted = true
      lastUnreadCount = inbox.unreadCount
      try {
        await ElMessageBox.confirm(
          `您有 ${inbox.unreadCount} 条未读客服会话，最早一条来自 ${inbox.unreadTickets[0]?.portalUserName ?? '门户用户'}。`,
          '客服上线提醒',
          { confirmButtonText: '立即处理', cancelButtonText: '稍后', type: 'info' },
        )
        goToTicket(inbox.earliestUnreadTicketId)
      } catch {
        // 用户选择稍后处理
      }
    } else if (!showPopup && inbox.unreadCount > lastUnreadCount && inbox.earliestUnreadTicketId) {
      ElNotification({
        title: '收到新的客服消息',
        message: `${inbox.unreadTickets[0]?.portalUserName ?? '门户用户'} 发来新消息，点击立即处理`,
        type: 'info',
        duration: 6000,
        onClick: () => goToTicket(inbox.earliestUnreadTicketId as string),
      })
    }
    lastUnreadCount = inbox.unreadCount
  } catch {
    // 在线状态失败不阻塞页面
  }
}

function leave() {
  void offlineCsAgent()
}

onMounted(() => {
  void beat(true)
  timer = window.setInterval(() => void beat(false), 5_000)
  window.addEventListener('beforeunload', leave)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  window.removeEventListener('beforeunload', leave)
  void offlineCsAgent()
})
</script>

<template>
  <span class="cs-online-watcher" aria-hidden="true"></span>
</template>

<style scoped>
.cs-online-watcher {
  display: none;
}
</style>
