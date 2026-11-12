<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { openChat } from '../chatState'

const SIZE = 58
const MARGIN = 14
const BOTTOM_SPACE = 86
const STORAGE_KEY = 'living-frame-portal-service-position'

const buttonRef = ref(null)
const position = reactive({ x: 0, y: 0 })
const dragging = ref(false)
const moved = ref(false)
let startX = 0
let startY = 0
let originX = 0
let originY = 0

function bounds() {
  const shell = document.querySelector('.portal-shell')
  const rect = shell ? shell.getBoundingClientRect() : { left: 0, right: window.innerWidth, top: 0 }
  return {
    minX: rect.left + MARGIN,
    maxX: rect.right - SIZE - MARGIN,
    minY: Math.max(rect.top + MARGIN, 12),
    maxY: window.innerHeight - BOTTOM_SPACE - SIZE,
  }
}

function clampPosition() {
  const limit = bounds()
  position.x = Math.min(Math.max(position.x, limit.minX), Math.max(limit.minX, limit.maxX))
  position.y = Math.min(Math.max(position.y, limit.minY), Math.max(limit.minY, limit.maxY))
}

function resetToDefault() {
  const limit = bounds()
  position.x = limit.maxX
  position.y = limit.maxY
}

function restorePosition() {
  resetToDefault()
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
    if (saved && Number.isFinite(saved.x) && Number.isFinite(saved.y)) {
      position.x = saved.x
      position.y = saved.y
    }
  } catch {
    // 使用默认位置
  }
  clampPosition()
}

function onPointerDown(event) {
  if (event.button !== undefined && event.button !== 0) return
  dragging.value = true
  moved.value = false
  startX = event.clientX
  startY = event.clientY
  originX = position.x
  originY = position.y
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp, { once: true })
}

function onPointerMove(event) {
  if (!dragging.value) return
  const dx = event.clientX - startX
  const dy = event.clientY - startY
  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
    moved.value = true
  }
  position.x = originX + dx
  position.y = originY + dy
  clampPosition()
}

function onPointerUp() {
  dragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  if (moved.value) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ x: position.x, y: position.y }))
    return
  }
  openChat('CHAT')
}

function onResize() {
  clampPosition()
}

onMounted(() => {
  restorePosition()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('pointermove', onPointerMove)
})
</script>

<template>
  <button
    ref="buttonRef"
    type="button"
    class="service-fab"
    :class="{ dragging }"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
    aria-label="联系在线客服"
    @pointerdown.prevent="onPointerDown"
  >
    <span class="service-halo"></span>
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4.8 13.3v-1.5a7.2 7.2 0 0 1 14.4 0v1.5" />
      <rect x="3.5" y="13" width="4.2" height="6.2" rx="2.1" />
      <rect x="16.3" y="13" width="4.2" height="6.2" rx="2.1" />
      <path d="M18.4 19.2c-.6 1.6-2 2.8-4.4 2.8" />
    </svg>
    <span class="service-dot"></span>
  </button>
</template>

<style scoped>
.service-fab {
  position: fixed;
  z-index: 40;
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  color: #ffffff;
  background: linear-gradient(145deg, #5b75ff, #7658ff);
  box-shadow: 0 12px 28px rgba(79, 107, 251, 0.38);
  cursor: grab;
  touch-action: none;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.service-fab.dragging {
  cursor: grabbing;
  transition: none;
  transform: scale(1.04);
}

.service-fab svg {
  position: relative;
  z-index: 2;
  width: 28px;
  height: 28px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}

.service-halo {
  position: absolute;
  inset: -7px;
  border: 1px solid rgba(79, 107, 251, 0.18);
  border-radius: 50%;
  background: rgba(79, 107, 251, 0.08);
  animation: halo 2.8s ease-out infinite;
}

.service-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 3;
  width: 9px;
  height: 9px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  background: #34d399;
}

@keyframes halo {
  0% {
    transform: scale(0.82);
    opacity: 0.7;
  }
  70% {
    transform: scale(1.18);
    opacity: 0;
  }
  100% {
    opacity: 0;
  }
}
</style>
