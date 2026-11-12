<script setup>
import { computed, onMounted, ref } from 'vue'
import { portalApi } from '../api'
import { openChat } from '../chatState'
import { resolveAvatar } from '../avatar'
import { session } from '../session'

const tickets = ref([])
const displayName = computed(() => session.user?.nickname || session.user?.username || '朋友')
const avatar = computed(() => resolveAvatar(session.user?.avatar))
const activeCount = computed(() => tickets.value.filter((ticket) => ['ASSIGNED', 'PROCESSING', 'PENDING'].includes(ticket.status)).length)

async function loadTickets() {
  try {
    tickets.value = await portalApi.tickets()
  } catch {
    tickets.value = []
  }
}

function contact(type = 'CHAT', history = false) {
  openChat(type, history)
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' }).format(date)
}

onMounted(loadTickets)
</script>

<template>
  <div class="page-view home-page">
    <header class="app-bar">
      <div>
        <span class="eyebrow">WELCOME BACK</span>
        <h1>你好，{{ displayName }}</h1>
        <p>今天也为你准备好了一切。</p>
      </div>
      <div class="avatar">
        <img v-if="avatar" :src="avatar" alt="头像" />
        <span v-else>{{ displayName.slice(0, 1).toUpperCase() }}</span>
      </div>
    </header>

    <section class="home-hero">
      <div class="hero-glow"></div>
      <div class="hero-copy">
        <span class="hero-kicker">LIVING FRAME SERVICE</span>
        <h2>有问题，<br />随时找我们。</h2>
        <p>在线客服为你创建专属工单，技术问题也会被认真对待。</p>
        <button type="button" class="hero-button" @click="contact('CHAT')">
          开始咨询
          <svg viewBox="0 0 24 24"><path d="M5 12h13M13 6l6 6-6 6" /></svg>
        </button>
      </div>
      <div class="hero-orbit">
        <div class="orbit-ring ring-one"></div>
        <div class="orbit-ring ring-two"></div>
        <div class="orbit-center">
          <svg viewBox="0 0 24 24">
            <path d="M4.8 13.3v-1.5a7.2 7.2 0 0 1 14.4 0v1.5" />
            <rect x="3.5" y="13" width="4.2" height="6.2" rx="2.1" />
            <rect x="16.3" y="13" width="4.2" height="6.2" rx="2.1" />
          </svg>
        </div>
      </div>
    </section>

    <div class="section-title">
      <strong>快捷服务</strong>
      <span>为你精选</span>
    </div>
    <section class="quick-grid">
      <button type="button" class="quick-card" @click="contact('CHAT')">
        <span class="quick-icon blue">
          <svg viewBox="0 0 24 24"><path d="M5 6.5h14v9H9l-4 3v-12Z" /><path d="M8.5 10h7M8.5 13h4" /></svg>
        </span>
        <strong>在线客服</strong>
        <span>即时沟通，创建工单</span>
      </button>
      <button type="button" class="quick-card" @click="contact('TECH')">
        <span class="quick-icon purple">
          <svg viewBox="0 0 24 24"><path d="m8 8-4 4 4 4M16 8l4 4-4 4M14 5l-4 14" /></svg>
        </span>
        <strong>技术支持</strong>
        <span>系统问题，技术协助</span>
      </button>
      <button type="button" class="quick-card" @click="contact('CHAT', true)">
        <span class="quick-icon mint">
          <svg viewBox="0 0 24 24"><path d="M6 4h12v16H6z" /><path d="M9 8h6M9 12h6M9 16h4" /></svg>
        </span>
        <strong>我的工单</strong>
        <span>查看历史沟通记录</span>
      </button>
      <button type="button" class="quick-card" @click="contact('CHAT')">
        <span class="quick-icon amber">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8" /><path d="M9.6 9.2a2.5 2.5 0 0 1 4.9.1c0 1.8-2.5 2.1-2.5 4M12 17.2v.1" /></svg>
        </span>
        <strong>使用帮助</strong>
        <span>常见问题快速解答</span>
      </button>
    </section>

    <div class="section-title">
      <strong>最近进展</strong>
      <button type="button" class="link-button" @click="contact('CHAT', true)">全部工单</button>
    </div>
    <section class="recent-card card">
      <div v-if="activeCount" class="recent-summary">
        <span class="pulse-dot"></span>
        <div>
          <strong>{{ activeCount }} 个工单正在处理中</strong>
          <span>客服回复后会在这里提醒你</span>
        </div>
        <button type="button" class="ghost-button" @click="contact('CHAT', true)">查看</button>
      </div>
      <div v-else class="recent-empty">
        <span>还没有进行中的工单</span>
        <button type="button" class="text-link" @click="contact('CHAT')">发起咨询</button>
      </div>
      <div v-for="ticket in tickets.slice(0, 2)" :key="ticket.id" class="recent-item" @click="contact('CHAT', true)">
        <div>
          <strong>{{ ticket.title }}</strong>
          <span>{{ ticket.ticketNo }} · {{ formatDate(ticket.updatedAt) }}</span>
        </div>
        <span class="recent-status" :class="ticket.status.toLowerCase()">
          {{ ticket.status === 'ARCHIVED' ? '已归档' : ticket.status === 'PROCESSING' ? '沟通中' : ticket.status === 'ASSIGNED' ? '已接单' : '等待中' }}
        </span>
      </div>
    </section>

    <p class="home-footer">Living Frame · 你的服务台</p>
  </div>
</template>

<style scoped>
.home-page {
  padding-top: calc(env(safe-area-inset-top, 0px) + 22px);
}

.home-hero {
  position: relative;
  display: flex;
  min-height: 218px;
  margin-top: 24px;
  overflow: hidden;
  border-radius: 24px;
  color: #ffffff;
  background: linear-gradient(125deg, #425df5 0%, #6b59f6 55%, #8e65ff 100%);
  box-shadow: 0 18px 38px rgba(79, 94, 220, 0.28);
}

.hero-glow {
  position: absolute;
  top: -80px;
  right: -40px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  filter: blur(6px);
}

.hero-copy {
  position: relative;
  z-index: 2;
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: flex-start;
  padding: 24px 20px;
}

.hero-kicker {
  color: rgba(255, 255, 255, 0.7);
  font-size: 8px;
  letter-spacing: 0.16em;
}

.hero-copy h2 {
  margin: 9px 0 8px;
  font-size: 26px;
  font-weight: 750;
  letter-spacing: -0.03em;
  line-height: 1.22;
}

.hero-copy p {
  max-width: 230px;
  margin: 0;
  color: rgba(255, 255, 255, 0.76);
  font-size: 10px;
  line-height: 1.7;
}

.hero-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 34px;
  margin-top: 17px;
  padding: 0 13px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 10px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.16);
  font-size: 10px;
  cursor: pointer;
}

.hero-button svg {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.hero-orbit {
  position: absolute;
  top: 50%;
  right: 12px;
  width: 148px;
  height: 148px;
  transform: translateY(-50%);
}

.orbit-ring {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 50%;
}

.ring-one {
  inset: 8px;
}

.ring-two {
  inset: 28px;
}

.orbit-center {
  position: absolute;
  top: 50%;
  left: 50%;
  display: grid;
  place-items: center;
  width: 62px;
  height: 62px;
  transform: translate(-50%, -50%);
  border: 1px solid rgba(255, 255, 255, 0.34);
  border-radius: 20px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.18);
  box-shadow: 0 12px 28px rgba(39, 39, 131, 0.24);
  backdrop-filter: blur(8px);
}

.orbit-center svg {
  width: 27px;
  height: 27px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.5;
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-height: 116px;
  padding: 14px;
  border: 1px solid rgba(230, 235, 244, 0.9);
  border-radius: 17px;
  background: #ffffff;
  box-shadow: 0 8px 22px rgba(48, 66, 103, 0.06);
  text-align: left;
  cursor: pointer;
}

.quick-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  margin-bottom: 9px;
  border-radius: 11px;
}

.quick-icon.blue {
  color: #4f6bfb;
  background: #eef2ff;
}

.quick-icon.purple {
  color: #8b5cf6;
  background: #f5efff;
}

.quick-icon.mint {
  color: #17a77c;
  background: #eafaf4;
}

.quick-icon.amber {
  color: #d4952c;
  background: #fff7e8;
}

.quick-icon svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}

.quick-card strong {
  color: #263042;
  font-size: 12px;
}

.quick-card > span:last-child {
  margin-top: 4px;
  color: #9aa3b5;
  font-size: 9px;
  line-height: 1.5;
}

.link-button {
  border: 0;
  color: #4f6bfb;
  background: transparent;
  font-size: 10px;
  cursor: pointer;
}

.recent-card {
  overflow: hidden;
}

.recent-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-bottom: 1px solid #f1f3f7;
  background: linear-gradient(90deg, #f7f9ff, #ffffff);
}

.pulse-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  border-radius: 50%;
  background: #4f6bfb;
  box-shadow: 0 0 0 5px rgba(79, 107, 251, 0.09);
}

.recent-summary > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.recent-summary strong {
  color: #303b50;
  font-size: 11px;
}

.recent-summary span {
  color: #9aa3b5;
  font-size: 9px;
}

.recent-summary .ghost-button {
  height: 30px;
  padding: 0 10px;
  font-size: 10px;
}

.recent-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 14px;
  border-bottom: 1px solid #f1f3f7;
  color: #9aa3b5;
  font-size: 11px;
}

.text-link {
  border: 0;
  color: #4f6bfb;
  background: transparent;
  font-size: 10px;
  cursor: pointer;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 13px 14px;
  border-bottom: 1px solid #f4f6fa;
  cursor: pointer;
}

.recent-item:last-child {
  border-bottom: 0;
}

.recent-item > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.recent-item strong {
  overflow: hidden;
  color: #394357;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-item span {
  color: #a4adbb;
  font-size: 9px;
}

.recent-status {
  padding: 3px 7px;
  border-radius: 7px;
  color: #8d96a8;
  background: #f3f5f8;
  font-size: 9px;
  white-space: nowrap;
}

.recent-status.assigned,
.recent-status.processing {
  color: #4f6bfb;
  background: #eef2ff;
}

.recent-status.archived {
  color: #5f8a70;
  background: #effaf2;
}

.home-footer {
  margin: 30px 0 0;
  color: #b4bcc9;
  font-size: 9px;
  letter-spacing: 0.08em;
  text-align: center;
}
</style>
