<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const active = computed(() => (route.name === 'me' ? 'me' : 'home'))
</script>

<template>
  <nav class="bottom-nav">
    <RouterLink to="/" class="nav-item" :class="{ active: active === 'home' }">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 10.8 12 4l8 6.8v8.1a1.1 1.1 0 0 1-1.1 1.1H5.1A1.1 1.1 0 0 1 4 18.9z" />
        <path d="M9.2 20v-6.1h5.6V20" />
      </svg>
      <span>首页</span>
    </RouterLink>
    <RouterLink to="/me" class="nav-item" :class="{ active: active === 'me' }">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="8" r="3.4" />
        <path d="M4.8 20c.5-3.7 3.3-5.8 7.2-5.8s6.7 2.1 7.2 5.8" />
      </svg>
      <span>我的</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 50%;
  z-index: 30;
  display: flex;
  align-items: flex-start;
  justify-content: space-around;
  width: min(100%, 480px);
  padding: 9px 28px calc(9px + env(safe-area-inset-bottom, 0px));
  transform: translateX(-50%);
  border-top: 1px solid rgba(229, 235, 244, 0.9);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 -8px 26px rgba(39, 58, 99, 0.06);
  backdrop-filter: blur(16px);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 76px;
  padding: 3px 14px 2px;
  color: #a0a9b8;
  text-decoration: none;
  font-size: 10px;
  transition: color 0.15s ease;
}

.nav-item svg {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
}

.nav-item.active {
  color: #4f6bfb;
  font-weight: 650;
}

.nav-item.active svg {
  stroke-width: 2;
}

@media (min-width: 560px) {
  .bottom-nav {
    bottom: 24px;
    border-radius: 0 0 32px 32px;
  }
}
</style>
