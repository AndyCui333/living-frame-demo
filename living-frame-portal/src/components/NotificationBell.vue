<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { portalApi } from '../api'

const router = useRouter()
const unread = ref(0)
let timer

async function refresh() {
  try {
    unread.value = Number(await portalApi.unreadNotificationCount()) || 0
  } catch {
    // 通知数量获取失败不阻塞页面
  }
}

function openNotifications() {
  void router.push('/notifications')
}

function onChanged() {
  void refresh()
}

onMounted(() => {
  void refresh()
  timer = window.setInterval(refresh, 20_000)
  window.addEventListener('notifications-updated', onChanged)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
  window.removeEventListener('notifications-updated', onChanged)
})
</script>

<template>
  <button type="button" class="notification-bell" aria-label="通知" @click="openNotifications">
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M6.8 10.2a5.2 5.2 0 0 1 10.4 0v3.2l1.5 2.2H5.3l1.5-2.2z" />
      <path d="M9.6 18.1a2.6 2.6 0 0 0 4.8 0" />
    </svg>
    <span v-if="unread > 0" class="notification-badge">{{ unread > 99 ? '99+' : unread }}</span>
  </button>
</template>

<style scoped>
.notification-bell {
  position: fixed;
  top: calc(env(safe-area-inset-top, 0px) + 18px);
  right: max(16px, calc(50vw - 224px));
  z-index: 35;
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 13px;
  color: #5f6b80;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 8px 20px rgba(34, 53, 94, 0.12);
  backdrop-filter: blur(12px);
  cursor: pointer;
}

.notification-bell svg {
  width: 19px;
  height: 19px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
}

.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border: 2px solid #ffffff;
  border-radius: 999px;
  color: #ffffff;
  background: #f04438;
  font-size: 8px;
  line-height: 13px;
  text-align: center;
}

@media (min-width: 560px) {
  .notification-bell {
    top: calc(24px + env(safe-area-inset-top, 0px));
  }
}
</style>
