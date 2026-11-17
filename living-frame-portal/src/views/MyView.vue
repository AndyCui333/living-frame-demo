<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { openChat } from '../chatState'
import { resolveAvatar } from '../avatar'
import { logout, session } from '../session'

const router = useRouter()
const displayName = computed(() => session.user?.nickname || session.user?.username || '门户用户')
const avatar = computed(() => resolveAvatar(session.user?.avatar))

function contact() {
  openChat('CHAT')
}

function history() {
  openChat('CHAT', true)
}

function editProfile() {
  void router.push('/me/profile')
}

function changePassword() {
  void router.push('/me/password')
}

async function signOut() {
  await logout()
  router.replace('/login')
}
</script>

<template>
  <div class="page-view me-page">
    <section class="profile-card" role="button" tabindex="0" @click="editProfile">
      <div class="profile-avatar">
        <img v-if="avatar" :src="avatar" alt="头像" />
        <span v-else>{{ displayName.slice(0, 1).toUpperCase() }}</span>
      </div>
      <div class="profile-copy">
        <strong>{{ displayName }}</strong>
        <span>点击查看和编辑</span>
      </div>
      <span class="profile-chevron">
        <svg viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
      </span>
    </section>

    <div class="section-title">
      <strong>我的服务</strong>
      <span>随时为你</span>
    </div>
    <section class="menu-card card">
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
      <button type="button" class="menu-item" @click="history">
        <span class="menu-icon blue">
          <svg viewBox="0 0 24 24"><path d="M6 4h12v16H6z" /><path d="M9 8h6M9 12h6M9 16h4" /></svg>
        </span>
        <span class="menu-copy">
          <strong>我的工单</strong>
          <span>查看所有客服沟通记录</span>
        </span>
        <svg class="chevron" viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
      </button>
    </section>

    <section class="assurance-card">
      <div class="assurance-head">
        <strong>服务保障</strong>
        <span>安心使用</span>
      </div>
      <div class="assurance-items">
        <span><i></i>隐私加密</span>
        <span><i></i>工单可追溯</span>
        <span><i></i>专属客服</span>
      </div>
    </section>

    <div class="me-bottom">
      <div class="account-actions">
        <button type="button" class="account-button" @click="changePassword">修改密码</button>
        <button type="button" class="account-button danger" @click="signOut">退出登录</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.me-page {
  display: flex;
  flex-direction: column;
  padding-top: calc(env(safe-area-inset-top, 0px) + 22px);
  padding-bottom: calc(76px + env(safe-area-inset-bottom, 0px));
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 8px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background:
    radial-gradient(circle at 90% 0%, rgba(124, 92, 255, 0.5), transparent 34%),
    linear-gradient(135deg, #18234a, #2b3c84 70%, #3e3b9e);
  box-shadow: 0 18px 38px rgba(31, 47, 119, 0.24);
  cursor: pointer;
}

.profile-card:focus-visible {
  outline: 2px solid #b9c6ff;
  outline-offset: 2px;
}

.profile-avatar {
  display: grid;
  place-items: center;
  width: 66px;
  height: 66px;
  flex: 0 0 66px;
  overflow: hidden;
  border: 3px solid rgba(255, 255, 255, 0.9);
  border-radius: 21px;
  color: #4f6bfb;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(13, 22, 52, 0.25);
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
  color: #ffffff;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-copy > span {
  color: rgba(255, 255, 255, 0.62);
  font-size: 10px;
}

.profile-chevron {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  color: rgba(255, 255, 255, 0.65);
}

.profile-chevron svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
}

.menu-card {
  overflow: hidden;
}

.assurance-card {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #e8edf5;
  border-radius: 17px;
  background: linear-gradient(120deg, #f7f9ff, #ffffff);
  box-shadow: 0 8px 20px rgba(48, 66, 103, 0.04);
}

.assurance-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.assurance-head strong {
  color: #3d4960;
  font-size: 11px;
}

.assurance-head span {
  color: #a0a9b8;
  font-size: 9px;
}

.assurance-items {
  display: flex;
  align-items: center;
  gap: 8px;
}

.assurance-items span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: 1;
  padding: 8px 6px;
  border-radius: 9px;
  color: #6b778c;
  background: rgba(255, 255, 255, 0.8);
  font-size: 9px;
  white-space: nowrap;
}

.assurance-items i {
  width: 5px;
  height: 5px;
  flex: 0 0 5px;
  border-radius: 50%;
  background: #4f8dfb;
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

.me-bottom {
  margin-top: auto;
  padding-top: 24px;
}

.account-actions {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.account-button {
  width: 100%;
  height: 44px;
  border: 1px solid #e4e9ff;
  border-radius: 14px;
  color: #4f6bfb;
  background: #f7f9ff;
  font-size: 11px;
  cursor: pointer;
}

.account-button.danger {
  border-color: #f2e6e6;
  color: #c45b5b;
  background: #fffafa;
}

</style>
