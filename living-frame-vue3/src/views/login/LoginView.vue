<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  ArrowRight,
  ChatDotRound,
  CircleCheck,
  Key,
  Lock,
  Refresh,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import type { CaptchaResult } from '@/types/api'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaLoading = ref(false)
const captcha = ref<CaptchaResult>({ captchaId: '', image: '' })
const captchaRemaining = ref(0)
const captchaDisabled = computed(() => captcha.value.captchaId === 'disabled' && !captcha.value.image)

let countdownTimer: number | undefined
let captchaExpiresAt = 0

const form = reactive({
  username: 'admin',
  password: 'Admin@123',
  captchaCode: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const demoAccounts = [
  { label: '超级管理员', username: 'admin', password: 'Admin@123' },
  { label: '系统管理员', username: 'sysadmin', password: 'Sys@123' },
  { label: '只读用户', username: 'viewer', password: 'View@123' },
]

function stopCountdown() {
  if (countdownTimer !== undefined) {
    window.clearInterval(countdownTimer)
    countdownTimer = undefined
  }
}

function startCountdown(seconds: number) {
  stopCountdown()
  captchaExpiresAt = Date.now() + Math.max(0, seconds) * 1000
  captchaRemaining.value = Math.max(0, Math.ceil(seconds))
  countdownTimer = window.setInterval(() => {
    const remaining = Math.max(0, Math.ceil((captchaExpiresAt - Date.now()) / 1000))
    captchaRemaining.value = remaining
    if (remaining <= 0) {
      stopCountdown()
      form.captchaCode = ''
      void loadCaptcha(true)
    }
  }, 500)
}

async function loadCaptcha(auto = false) {
  if (captchaLoading.value) {
    return
  }
  stopCountdown()
  captchaLoading.value = true
  form.captchaCode = ''
  if (auto) {
    captchaRemaining.value = 0
  }
  try {
    captcha.value = await getCaptcha()
    if (captcha.value.captchaId === 'disabled' && !captcha.value.image) {
      form.captchaCode = 'x'
      captchaRemaining.value = 0
    } else {
      startCountdown(captcha.value.expiresIn ?? 60)
    }
  } catch {
    captcha.value = { captchaId: '', image: '' }
    captchaRemaining.value = 0
  } finally {
    captchaLoading.value = false
  }
}

function fillDemo(account: (typeof demoAccounts)[number]) {
  form.username = account.username
  form.password = account.password
  form.captchaCode = ''
  loadCaptcha()
  ElMessage.info(`已填入${account.label}账号`)
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!captcha.value.captchaId) {
    ElMessage.warning('请先获取验证码')
    return
  }
  if (!captchaDisabled.value && captchaRemaining.value <= 0) {
    await loadCaptcha()
    ElMessage.warning('验证码已失效，已自动刷新，请重新输入')
    return
  }
  loading.value = true
  try {
    await auth.login({
      username: form.username.trim(),
      password: form.password,
      captchaId: captcha.value.captchaId,
      captchaCode: form.captchaCode.trim(),
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } catch {
    form.captchaCode = ''
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadCaptcha()
})

onUnmounted(stopCountdown)
</script>

<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="brand-top">
        <div class="brand-mark">LF</div>
        <span class="brand-name">Living Frame</span>
      </div>

      <div class="brand-copy">
        <span class="eyebrow">MODERN ADMIN PLATFORM</span>
        <h1>让后台管理<br />更简单、更清晰。</h1>
        <p>
          Living Frame 集成用户权限、业务用户与 DeepSeek AI 能力，
          为你的管理系统提供统一的工作台。
        </p>

        <div class="feature-list">
          <div class="feature-item">
            <el-icon><Lock /></el-icon>
            <div>
              <strong>细粒度 RBAC</strong>
              <span>用户、角色、菜单与按钮权限统一管理</span>
            </div>
          </div>
          <div class="feature-item">
            <el-icon><ChatDotRound /></el-icon>
            <div>
              <strong>AI 助手与 Agent</strong>
              <span>对话、工具调用和业务数据洞察</span>
            </div>
          </div>
          <div class="feature-item">
            <el-icon><CircleCheck /></el-icon>
            <div>
              <strong>开箱即用</strong>
              <span>Vue 3 + TypeScript + Element Plus</span>
            </div>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        <span>© 2026 Living Frame</span>
        <span>JDK 25 · Spring Boot 3.5</span>
      </div>
    </section>

    <section class="form-panel">
      <div class="login-card">
        <div class="card-heading">
          <span class="eyebrow">WELCOME BACK</span>
          <h2>登录管理后台</h2>
          <p>输入账号信息，继续你的工作。</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="用户名" prop="username" label-for="login-username">
            <el-input
              id="login-username"
              v-model="form.username"
              placeholder="请输入用户名"
              autocomplete="username"
              clearable
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password" label-for="login-password">
            <el-input
              id="login-password"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="!captchaDisabled" label="验证码" prop="captchaCode" label-for="login-captcha">
            <div class="captcha-row">
              <el-input
                id="login-captcha"
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                maxlength="8"
                autocomplete="off"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
              <button
                class="captcha-box"
                type="button"
                title="点击验证码刷新"
                aria-label="点击验证码刷新"
                :disabled="captchaLoading"
                @click="loadCaptcha()"
              >
                <img v-if="captcha.image" :src="captcha.image" alt="验证码，点击刷新" />
                <span v-else class="captcha-empty">
                  <el-icon><Refresh /></el-icon>
                  获取
                </span>
              </button>
            </div>
            <div class="captcha-hint">
              <span class="captcha-countdown" :class="{ 'is-expiring': !captchaLoading && captchaRemaining <= 10 }">
                <template v-if="captchaLoading">正在刷新验证码…</template>
                <template v-else-if="!captcha.image">验证码获取失败，请点击刷新</template>
                <template v-else-if="captchaRemaining > 0">
                  验证码将在 <strong>{{ captchaRemaining }}</strong> 秒后失效
                </template>
                <template v-else>验证码已失效，正在自动刷新…</template>
              </span>
              <button type="button" class="captcha-refresh-link" :disabled="captchaLoading" @click="loadCaptcha()">
                立即刷新
              </button>
            </div>
          </el-form-item>
          <el-form-item v-else label="验证码">
            <el-alert type="info" :closable="false" show-icon title="当前环境已关闭验证码校验" />
          </el-form-item>

          <el-button class="login-button" type="primary" size="large" :loading="loading" @click="handleLogin">
            登录
            <el-icon class="login-arrow"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="demo-block">
          <span class="demo-title">演示账号</span>
          <div class="demo-list">
            <button
              v-for="account in demoAccounts"
              :key="account.username"
              type="button"
              class="demo-item"
              @click="fillDemo(account)"
            >
              <el-icon><UserFilled /></el-icon>
              <span>{{ account.label }}</span>
              <code>{{ account.username }}</code>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(420px, 0.9fr);
  min-height: 100vh;
  background: #f6f7fc;
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 100vh;
  padding: 42px 56px;
  overflow: hidden;
  color: #ffffff;
  background:
    radial-gradient(circle at 82% 8%, rgba(139, 92, 246, 0.55), transparent 32%),
    radial-gradient(circle at 12% 86%, rgba(79, 107, 251, 0.42), transparent 38%),
    linear-gradient(145deg, #0c1222 0%, #111a2e 55%, #121a31 100%);
}

.brand-panel::after {
  position: absolute;
  right: -120px;
  bottom: -160px;
  width: 460px;
  height: 460px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  box-shadow: 0 0 0 70px rgba(255, 255, 255, 0.025), 0 0 0 140px rgba(255, 255, 255, 0.018);
  content: "";
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  z-index: 1;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #5b73ff, #8b5cf6);
  box-shadow: 0 8px 20px rgba(91, 115, 255, 0.35);
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.brand-copy {
  position: relative;
  z-index: 1;
  max-width: 520px;
  margin: 8vh 0;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 16px;
  color: #8ea0ff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2.4px;
}

.brand-copy h1 {
  margin: 0;
  font-size: clamp(38px, 4.2vw, 58px);
  line-height: 1.12;
  letter-spacing: -1.8px;
}

.brand-copy > p {
  max-width: 440px;
  margin: 22px 0 0;
  color: #9ba8c0;
  font-size: 14px;
  line-height: 1.9;
}

.feature-list {
  display: grid;
  gap: 14px;
  margin-top: 36px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.035);
  backdrop-filter: blur(8px);
}

.feature-item .el-icon {
  margin-top: 2px;
  color: #8ea0ff;
  font-size: 18px;
}

.feature-item div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-item strong {
  font-size: 13px;
  font-weight: 600;
}

.feature-item span {
  color: #8f9bb2;
  font-size: 12px;
}

.brand-footer {
  display: flex;
  justify-content: space-between;
  position: relative;
  z-index: 1;
  color: #66738b;
  font-size: 11px;
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
}

.login-card {
  width: 100%;
  max-width: 420px;
}

.card-heading {
  margin-bottom: 28px;
}

.card-heading .eyebrow {
  margin-bottom: 10px;
  color: #7c8cff;
}

.card-heading h2 {
  margin: 0;
  color: #111827;
  font-size: 28px;
  letter-spacing: -0.8px;
}

.card-heading p {
  margin: 8px 0 0;
  color: #8a93a6;
  font-size: 13px;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 124px;
  gap: 10px;
  width: 100%;
}

.captcha-box {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: #f8f9fc;
  cursor: pointer;
}

.captcha-box:hover {
  border-color: var(--el-color-primary-light-5);
}

.captcha-box:disabled {
  cursor: wait;
  opacity: 0.65;
}

.captcha-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-empty {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #8a93a6;
  font-size: 12px;
}

.captcha-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 6px;
  color: #8a93a6;
  font-size: 11px;
}

.captcha-countdown strong {
  color: #4f6bfb;
  font-variant-numeric: tabular-nums;
}

.captcha-countdown.is-expiring,
.captcha-countdown.is-expiring strong {
  color: #d97706;
}

.captcha-refresh-link {
  padding: 0;
  border: none;
  color: #5b73ff;
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}

.captcha-refresh-link:disabled {
  color: #b8c0d4;
  cursor: wait;
}

.captcha-refresh-link:hover:not(:disabled) {
  color: #3d55e8;
  text-decoration: underline;
}

.login-button {
  width: 100%;
  margin-top: 6px;
  height: 46px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #4f6bfb, #7c5cff);
  box-shadow: 0 12px 24px rgba(79, 107, 251, 0.24);
}

.login-arrow {
  margin-left: 6px;
}

.demo-block {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px dashed #e4e7f0;
}

.demo-title {
  display: block;
  margin-bottom: 10px;
  color: #9aa3b5;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.6px;
}

.demo-list {
  display: grid;
  gap: 6px;
}

.demo-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #5d6678;
  background: transparent;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
}

.demo-item:hover {
  color: #3d55e8;
  border-color: #dfe5fe;
  background: #f0f3ff;
}

.demo-item .el-icon {
  color: #8ea0ff;
}

.demo-item code {
  margin-left: auto;
  color: #9aa3b5;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    min-height: 100vh;
    padding: 24px;
  }
}
</style>
