<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

const progress = ref(0)
const dragging = ref(false)
const moved = ref(false)
let startY = 0
let suppressClick = false

function onPointerDown(event) {
  if (event.button !== undefined && event.button !== 0) return
  if (!event.target.closest('.bottom-nav')) return
  startY = event.clientY
  moved.value = false
  dragging.value = true
}

function onPointerMove(event) {
  if (!dragging.value) return
  const distance = startY - event.clientY
  if (distance <= 2) {
    progress.value = 0
    return
  }
  moved.value = true
  progress.value = Math.min(1, distance / 46)
  event.preventDefault()
}

function onPointerUp() {
  if (!dragging.value) return
  dragging.value = false
  if (moved.value) suppressClick = true
  progress.value = 0
}

function onClickCapture(event) {
  if (!suppressClick) return
  suppressClick = false
  if (event.target.closest('.bottom-nav')) {
    event.preventDefault()
    event.stopPropagation()
  }
}

onMounted(() => {
  document.addEventListener('pointerdown', onPointerDown)
  document.addEventListener('pointermove', onPointerMove, { passive: false })
  document.addEventListener('pointerup', onPointerUp)
  document.addEventListener('click', onClickCapture, true)
})

onUnmounted(() => {
  document.removeEventListener('pointerdown', onPointerDown)
  document.removeEventListener('pointermove', onPointerMove)
  document.removeEventListener('pointerup', onPointerUp)
  document.removeEventListener('click', onClickCapture, true)
})
</script>

<template>
  <div
    class="version-peek"
    :class="{ dragging }"
    :style="{ transform: `translateX(-50%) translateY(${(1 - progress) * 100}%)` }"
    aria-hidden="true"
  >
    <span class="version-grip"></span>
    <span>Living Frame Portal · v1.0.0</span>
  </div>
</template>

<style scoped>
.version-peek {
  position: fixed;
  bottom: calc(64px + env(safe-area-inset-bottom, 0px));
  left: 50%;
  z-index: 25;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: min(100%, 480px);
  height: 30px;
  color: #a0a9b8;
  background: linear-gradient(180deg, rgba(244, 247, 251, 0.98), #eef3f8);
  border-top: 1px solid rgba(222, 230, 242, 0.9);
  border-radius: 14px 14px 0 0;
  box-shadow: 0 -6px 18px rgba(39, 58, 99, 0.06);
  font-size: 9px;
  letter-spacing: 0.06em;
  pointer-events: none;
  transition: transform 0.38s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.version-peek.dragging {
  transition: none;
}

.version-grip {
  width: 24px;
  height: 3px;
  border-radius: 999px;
  background: #d3dbe8;
}

@media (min-width: 560px) {
  .version-peek {
    border-radius: 14px 14px 0 0;
  }
}
</style>
