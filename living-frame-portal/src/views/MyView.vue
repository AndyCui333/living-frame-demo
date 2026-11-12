<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { openChat } from '../chatState'
import { resolveAvatar } from '../avatar'
import { logout, session } from '../session'

const router = useRouter()
const displayName = computed(() => session.user?.nickname || session.user?.username || '门户用户')
const avatar = computed(() => resolveAvatar(session.user?.avatar))
const account = computed(() => session.user?.username || '')

function contact() {
  openChat('CHAT')
}

function history() {
  openChat('CHAT', true)
}

async function signOut() {
  await logout()
  router.replace('/login')
}
</script>

<template>
  <div class="page-view me-page">
    <header class="app-bar">
      <div>
        <span class="eyebrow">MY SPACE</span>
        <h1>我的</h1>
        <p>管理你的账户与沟通记录。</p>
      </div>
    </header>

    <section class="profile-card">
      <div class="profile-avatar">
        <img v-if="avatar" :src="avatar" alt="头像" />
        <span v-else>{{ displayName.slice(0, 1).toUpperCase() }}</span>
      </div>
      <div class="profile-copy">
        <strong>{{ displayName }}</strong>
        <span>@{{ account }}</span>
        <span class="profile-badge">门户用户</span>
      </div>
      <button type="button" class="profile-service" aria-label="联系客服" @click="contact">
        <svg viewBox="0 0 24 24">
          <path d="M4.8 13.3v-1.5a7.2 7.2 0 0 1 14.4 0v1.5" />
          <rect x="3.5" y="13" width="4.2" height="6.2" rx="2.1" />
          <rect x="16.3" y="13" width="4.2" height="6.2" rx="2.1" />
        </svg>
      </button>
    </section>

    <div class="section-title">
      <strong>我的服务</strong>
      <span>随时为你</span>
    </div>
    <section class="menu-card card">
      <button type="button" class="menu-item" @click="history">
        <span class="menu-icon blue">
          <svg viewBox="0 0 24 24"><path d="M6 4h12v16H6z" /><path d="M9 8h6M9 12h6M9 16h4" /></svg>
        </span>
        <span class="menu-copy">
          <strong>历史工单</strong>
          <span>查看所有客服沟通记录</span>
        </span>
        <svg class="chevron" viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
      </button>
      <button type="button" class="menu-item" @click="contact">
        <span class="menu-icon purple">
          <svg viewBox="0 0 24 24"><path d="M5 6.5h14v9H9l-4 3v-12Z" /><path d="M8.5 10h7M8.5 13h4" /></svg>
        </span>
        <span class="menu-copy">
          <strong>联系客服</strong>
          <span>在线咨询，创建新工单</span>
        </span>
        <svg class="chevron" viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
      </button>
    </section>

    <section class="account-card card">
      <div class="account-row">
        <span>账号</span>
        <strong>@{{ account }}</strong>
      </div>
      <div class="account-row">
        <span>手机号</span>
        <strong>{{ session.user?.phone || '未设置' }}</strong>
      </div>
      <div class="account-row">
        <span>邮箱</span>
        <strong>{{ session.user?.email || '未设置' }}</strong>
      </div>
    </section>

    <button type="button" class="logout-button" @click="signOut">退出登录</button>

    <p class="me-footer">Living Frame Portal · v1.0.0</p>
  </div>
</template>

<style scoped>
.me-page {
  padding-top: calc(env(safe-area-inset-top, 0px) + 22px);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 24px;
  padding: 18px;
  border: 1px solid rgba(230, 235, 244, 0.9);
  border-radius: 22px;
  background: linear-gradient(125deg, #ffffff, #f7f9ff);
  box-shadow: var(--portal-shadow);
}

.profile-avatar {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  flex: 0 0 64px;
  overflow: hidden;
  border: 3px solid #ffffff;
  border-radius: 21px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #7658ff);
  box-shadow: 0 8px 20px rgba(79, 107, 251, 0.24);
  font-size: 22px;
  font-weight: 700;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.profile-copy strong {
  overflow: hidden;
  color: #1d2738;
  font-size: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-copy > span {
  color: #9aa3b5;
  font-size: 10px;
}

.profile-copy .profile-badge {
  align-self: flex-start;
  margin-top: 3px;
  padding: 2px 7px;
  border-radius: 6px;
  color: #4f6bfb;
  background: #eef2ff;
  font-size: 9px;
}

.profile-service {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  border: 1px solid #e4e9ff;
  border-radius: 13px;
  color: #4f6bfb;
  background: #f4f6ff;
  cursor: pointer;
}

.profile-service svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}

.menu-card {
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px;
  border: 0;
  border-bottom: 1px solid #f2f4f8;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.menu-item:last-child {
  border-bottom: 0;
}

.menu-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 11px;
}

.menu-icon.blue {
  color: #4f6bfb;
  background: #eef2ff;
}

.menu-icon.purple {
  color: #8b5cf6;
  background: #f5efff;
}

.menu-icon svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}

.menu-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.menu-copy strong {
  color: #303b50;
  font-size: 12px;
}

.menu-copy span {
  color: #9aa3b5;
  font-size: 9px;
}

.chevron {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: #bdc5d2;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}

.account-card {
  margin-top: 14px;
  padding: 6px 14px;
}

.account-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 11px 0;
  border-bottom: 1px solid #f3f5f9;
}

.account-row:last-child {
  border-bottom: 0;
}

.account-row span {
  color: #9aa3b5;
  font-size: 10px;
}

.account-row strong {
  overflow: hidden;
  color: #49546a;
  font-size: 11px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-button {
  width: 100%;
  height: 44px;
  margin-top: 22px;
  border: 1px solid #f2e6e6;
  border-radius: 14px;
  color: #c45b5b;
  background: #fffafa;
  font-size: 11px;
  cursor: pointer;
}

.me-footer {
  margin: 30px 0 0;
  color: #b4bcc9;
  font-size: 9px;
  letter-spacing: 0.08em;
  text-align: center;
}
</style>
