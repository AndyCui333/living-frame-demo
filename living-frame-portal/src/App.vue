<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from './components/BottomNav.vue'
import FloatingService from './components/FloatingService.vue'
import ChatSheet from './components/ChatSheet.vue'
import { loadProfile, logout, session } from './session'

const route = useRoute()
const router = useRouter()

onMounted(async () => {
  if (!session.token) return
  try {
    await loadProfile()
  } catch {
    await logout()
    router.replace('/login')
  }
})
</script>

<template>
  <div class="portal-shell">
    <RouterView />
    <BottomNav v-if="session.token && route.meta.tab" />
    <FloatingService v-if="session.token && route.meta.tab" />
    <ChatSheet v-if="session.token" />
  </div>
</template>
