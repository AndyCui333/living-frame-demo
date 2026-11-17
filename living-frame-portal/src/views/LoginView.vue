<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { portalApi } from '../api'
import { getApiBase, setApiBase } from '../apiBase'
import { login, session } from '../session'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '', captchaCode: '' })
const settingsVisible = ref(false)
const apiBaseInput = ref('')
const settingsError = ref('')
const captcha = reactive({
  id: '',
  image: '',
  remaining: 0,
  expiresAt: 0,
  expiresAtText: '',
  expired: false,
})
const captchaLoading = ref(false)
const error = ref('')
let captchaTimer = null

function pad(value) {
  return String(value).padStart(2, '0')
}

function stopCaptchaTimer() {
  if (captchaTimer) {
    window.clearInterval(captchaTimer)
    captchaTimer = null
  }
}

function tickCaptcha() {
  if (!captcha.expiresAt) return
  const remaining = Math.max(0, Math.ceil((captcha.expiresAt - Date.now()) / 1000))
  captcha.remaining = remaining
  if (remaining <= 0) {
    captcha.expired = true
    stopCaptchaTimer()
    void loadCaptcha(true)
  }
}

function startCaptchaCountdown(seconds) {
  stopCaptchaTimer()
  const total = Number(seconds) || 0
  if (total <= 0) {
    captcha.remaining = 0
    captcha.expiresAt = 0
    captcha.expiresAtText = ''
    captcha.expired = false
    return
  }
  captcha.expiresAt = Date.now() + total * 1000
  const expires = new Date(captcha.expiresAt)
  captcha.expiresAtText = `${pad(expires.getHours())}:${pad(expires.getMinutes())}:${pad(expires.getSeconds())}`
  captcha.expired = false
  tickCaptcha()
  captchaTimer = window.setInterval(tickCaptcha, 1000)
}

async function loadCaptcha(auto = false) {
  captchaLoading.value = true
  if (auto) {
    captcha.remaining = 0
  }
  try {
    const data = await portalApi.captcha()
    captcha.id = data.captchaId
    captcha.image = data.image
    startCaptchaCountdown(data.expiresIn)
  } catch {
    error.value = '验证码获取失败，请点击刷新'
  } finally {
    captchaLoading.value = false
  }
}

function fillDemo() {
  form.username = 'biz01'
  form.password = 'Biz@123'
  form.captchaCode = ''
}

function openSettings() {
  apiBaseInput.value = getApiBase()
  settingsError.value = ''
  settingsVisible.value = true
}

function saveSettings() {
  const value = apiBaseInput.value.trim()
  if (!value || (!value.startsWith('/') && !/^https?:\/\//i.test(value))) {
    settingsError.value = '请输入以 / 或 http(s):// 开头的 API 地址'
    return
  }
  setApiBase(value)
  settingsVisible.value = false
  window.location.reload()
}

async function submit() {
  error.value = ''
  if (!form.username.trim() || !form.password) {
    error.value = '请输入账号和密码'
    return
  }
  if (!captcha.id) {
    error.value = '请先获取验证码'
    return
  }
  if (captcha.expired || (captcha.expiresAt && Date.now() >= captcha.expiresAt)) {
    error.value = '验证码已失效，请点击刷新'
    form.captchaCode = ''
    await loadCaptcha()
    return
  }
  try {
    await login({
      username: form.username.trim(),
      password: form.password,
      captchaId: captcha.id,
      captchaCode: form.captchaCode.trim(),
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (err) {
    error.value = err.message || '登录失败'
    form.captchaCode = ''
    await loadCaptcha()
  }
}

onMounted(loadCaptcha)
onUnmounted(stopCaptchaTimer)
</script>

<template>
  <div class="login-page">
    <div class="login-glow glow-one"></div>
    <div class="login-glow glow-two"></div>

    <header class="login-brand">
      <div class="brand-mark">
        <span>LF</span>
      </div>
      <div>
        <strong>Living Frame</strong>
        <span>PORTAL</span>
      </div>
    </header>

    <section class="login-copy">
      <span class="eyebrow">WELCOME BACK</span>
      <h1>让每一次沟通，<br />都被认真对待。</h1>
      <p>登录你的门户账号，随时联系专属客服、查看工单进度。</p>
    </section>

    <section class="login-card">
      <div class="login-card-head">
        <strong>登录门户</strong>
        <span>使用业务账号继续</span>
      </div>

      <form @submit.prevent="submit">
        <div class="field">
          <label for="portal-username">账号</label>
          <el-input id="portal-username" v-model="form.username" size="large" autocomplete="username" placeholder="请输入门户账号" />
        </div>
        <div class="field">
          <label for="portal-password">密码</label>
          <el-input
            id="portal-password"
            v-model="form.password"
            type="password"
            size="large"
            show-password
            autocomplete="current-password"
            placeholder="请输入登录密码"
          />
        </div>
        <div class="field">
          <label for="portal-captcha">验证码</label>
          <div class="captcha-row">
            <el-input
              id="portal-captcha"
              v-model="form.captchaCode"
              class="captcha-input"
              size="large"
              maxlength="8"
              autocomplete="off"
              placeholder="请输入验证码"
            />
            <button
              type="button"
              class="captcha-image"
              :class="{ expired: captcha.expired }"
              title="点击验证码刷新"
              aria-label="点击验证码刷新"
              :disabled="captchaLoading"
              @click="loadCaptcha()"
            >
              <img v-if="captcha.image" :src="captcha.image" alt="验证码，点击刷新" />
              <span v-else>{{ captchaLoading ? '正在刷新' : '获取' }}</span>
            </button>
          </div>
          <div v-if="captchaLoading || captcha.image || captcha.expired" class="captcha-meta">
            <span class="captcha-countdown" :class="{ 'is-expiring': !captchaLoading && captcha.remaining <= 10 }">
              <template v-if="captchaLoading">正在刷新验证码…</template>
              <template v-else-if="!captcha.image">验证码获取失败，请点击刷新</template>
              <template v-else-if="captcha.remaining > 0">
                验证码将在 <strong>{{ captcha.remaining }}</strong> 秒后失效
              </template>
              <template v-else>验证码已失效，正在自动刷新…</template>
            </span>
          </div>
        </div>

        <p v-if="error" class="login-error">{{ error }}</p>

        <el-button type="primary" native-type="submit" class="login-button" :loading="session.loading">
          {{ session.loading ? '登录中...' : '进入门户' }}
          <svg v-if="!session.loading" viewBox="0 0 24 24"><path d="M5 12h13M13 6l6 6-6 6" /></svg>
        </el-button>
      </form>

      <button type="button" class="demo-link" @click="fillDemo">使用演示账号 biz01 / Biz@123</button>
      <button type="button" class="server-settings-link" @click="openSettings">服务器设置</button>
    </section>

    <el-dialog
      v-model="settingsVisible"
      title="服务器设置"
      width="330px"
      align-center
      class="server-settings-dialog"
      :close-on-click-modal="false"
    >
      <p class="settings-tip">输入门户后端 API 地址。例如：http://120.26.36.225:7369/api</p>
      <el-input v-model="apiBaseInput" size="large" placeholder="/api 或 http://192.168.1.10:8080/api" />
      <span v-if="settingsError" class="settings-error">{{ settingsError }}</span>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存并刷新</el-button>
      </template>
    </el-dialog>

    <p class="login-footer">Living Frame · 安全登录</p>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: calc(env(safe-area-inset-top, 0px) + 28px) 24px 30px;
  overflow: hidden;
  background:
    radial-gradient(circle at 80% 0%, rgba(124, 92, 255, 0.22), transparent 30%),
    linear-gradient(160deg, #f7f9ff 0%, #eef3fb 58%, #e8eef8 100%);
}

@media (min-width: 560px) {
  .login-page {
    min-height: calc(100vh - 48px);
  }
}

.login-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  pointer-events: none;
}

.glow-one {
  top: -100px;
  right: -80px;
  width: 260px;
  height: 260px;
  background: rgba(99, 122, 255, 0.16);
}

.glow-two {
  bottom: -120px;
  left: -100px;
  width: 280px;
  height: 280px;
  background: rgba(124, 92, 255, 0.12);
}

.login-brand {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #7c5cff);
  box-shadow: 0 8px 20px rgba(79, 107, 251, 0.28);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.login-brand > div:last-child {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.login-brand strong {
  color: #1c2638;
  font-size: 13px;
}

.login-brand span {
  color: #9aa3b5;
  font-size: 8px;
  letter-spacing: 0.16em;
}

.login-copy {
  position: relative;
  z-index: 2;
  margin-top: 38px;
}

.login-copy h1 {
  margin: 10px 0 10px;
  color: #172033;
  font-size: 28px;
  font-weight: 760;
  letter-spacing: -0.04em;
  line-height: 1.28;
}

.login-copy p {
  max-width: 290px;
  margin: 0;
  color: #8e98aa;
  font-size: 11px;
  line-height: 1.75;
}

.login-card {
  position: relative;
  z-index: 2;
  margin-top: 28px;
  padding: 22px 20px 18px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 46px rgba(46, 66, 108, 0.13);
  backdrop-filter: blur(18px);
}

.login-card-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 20px;
}

.login-card-head strong {
  color: #1d2738;
  font-size: 18px;
}

.login-card-head span {
  color: #9aa3b5;
  font-size: 10px;
}

.captcha-row {
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.captcha-row .captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-row :deep(.el-input__wrapper),
.field :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #e5eaf2;
  border-radius: 14px;
  background: #f9fbfe;
  box-shadow: none;
}

.captcha-row :deep(.el-input__wrapper.is-focus),
.field :deep(.el-input__wrapper.is-focus) {
  border-color: #b9c6ff;
  box-shadow: 0 0 0 4px rgba(79, 107, 251, 0.08);
}

.captcha-row :deep(.el-input__inner),
.field :deep(.el-input__inner) {
  height: 46px;
  padding: 0;
  border: 0;
  border-radius: 0;
  color: #1f2937;
  background: transparent;
  box-shadow: none;
  font-size: 13px;
}

.captcha-image {
  display: grid;
  place-items: center;
  width: 120px;
  height: 48px;
  flex: 0 0 120px;
  overflow: hidden;
  padding: 2px;
  border: 1px solid #e5eaf2;
  border-radius: 13px;
  background: #ffffff;
  cursor: pointer;
}

.captcha-image.expired {
  opacity: 0.55;
}

.captcha-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.captcha-image span {
  color: #8d96a8;
  font-size: 9px;
}

.captcha-meta {
  margin-top: 5px;
  padding: 0 2px;
  color: #a0a9b8;
  font-size: 9px;
}

.captcha-countdown strong {
  color: #4f6bfb;
  font-variant-numeric: tabular-nums;
}

.captcha-countdown.is-expiring,
.captcha-countdown.is-expiring strong {
  color: #d97706;
}

.login-error {
  margin: 0 0 12px;
  padding: 9px 11px;
  border-radius: 10px;
  color: #b24b4b;
  background: #fff3f3;
  font-size: 10px;
  line-height: 1.5;
}

.login-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  margin-top: 4px;
  border: 0;
  border-radius: 14px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #7658ff);
  box-shadow: 0 12px 26px rgba(79, 107, 251, 0.26);
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}

.login-button svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.login-button:disabled {
  opacity: 0.6;
}

.demo-link {
  display: block;
  width: 100%;
  margin-top: 13px;
  padding: 2px;
  border: 0;
  color: #8792a5;
  background: transparent;
  font-size: 9px;
  text-align: center;
  cursor: pointer;
}

.server-settings-link {
  display: block;
  width: 100%;
  margin-top: 8px;
  padding: 2px;
  border: 0;
  color: #b0b8c4;
  background: transparent;
  font-size: 9px;
  text-align: center;
  cursor: pointer;
}

:global(.server-settings-dialog) {
  border-radius: 20px;
}

:global(.server-settings-dialog .el-dialog__header) {
  padding: 20px 20px 0;
}

:global(.server-settings-dialog .el-dialog__title) {
  color: #172033;
  font-size: 17px;
}

:global(.server-settings-dialog .el-dialog__body) {
  padding: 12px 20px 4px;
}

:global(.server-settings-dialog .el-dialog__footer) {
  padding: 8px 20px 18px;
}

.settings-tip {
  margin: 0 0 12px;
  color: #8d96a8;
  font-size: 10px;
  line-height: 1.6;
}

.settings-error {
  display: block;
  margin-top: 6px;
  color: #c45b5b;
  font-size: 9px;
}

.login-footer {
  position: relative;
  z-index: 2;
  margin: auto 0 0;
  padding-top: 22px;
  color: #b3bbc8;
  font-size: 9px;
  letter-spacing: 0.08em;
  text-align: center;
}
</style>
