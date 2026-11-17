<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from './components/BottomNav.vue'
import FloatingService from './components/FloatingService.vue'
import ChatSheet from './components/ChatSheet.vue'
import VersionPeek from './components/VersionPeek.vue'
import NotificationBell from './components/NotificationBell.vue'
import { portalApi } from './api'
import { loadProfile, logout, session } from './session'

const route = useRoute()
const router = useRouter()
let activityTimer = null

function stopActivityHeartbeat() {
  if (activityTimer) {
    window.clearInterval(activityTimer)
    activityTimer = null
  }
}

function startActivityHeartbeat() {
  stopActivityHeartbeat()
  if (!session.token) return
  const beat = () => {
    if (session.token) void portalApi.heartbeat().catch(() => undefined)
  }
  beat()
  activityTimer = window.setInterval(beat, 30_000)
}

onMounted(async () => {
  if (!session.token) return
  try {
    await loadProfile()
    startActivityHeartbeat()
  } catch {
    await logout()
    router.replace('/login')
  }
})

watch(
  () => session.token,
  (token) => {
    if (token) {
      startActivityHeartbeat()
    } else {
      stopActivityHeartbeat()
    }
  },
)

onUnmounted(stopActivityHeartbeat)
</script>

<template>
  <div class="portal-shell" :class="{ 'has-tab': session.token && route.meta.tab }">
    <RouterView />
    <NotificationBell v-if="session.token && route.name !== 'notifications'" />
    <BottomNav v-if="session.token && route.meta.tab" />
    <VersionPeek v-if="session.token && route.meta.tab" />
    <FloatingService v-if="session.token && route.meta.tab" />
    <ChatSheet v-if="session.token" />
  </div>
</template>
