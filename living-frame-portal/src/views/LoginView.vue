<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { portalApi } from '../api'
import { login, session } from '../session'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '', captchaCode: '' })
const captcha = reactive({ id: '', image: '' })
const captchaLoading = ref(false)
const error = ref('')

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const data = await portalApi.captcha()
    captcha.id = data.captchaId
    captcha.image = data.image
  } catch {
    error.value = '验证码加载失败，请点击重试'
  } finally {
    captchaLoading.value = false
  }
}

function fillDemo() {
  form.username = 'biz01'
  form.password = 'Biz@123'
  form.captchaCode = ''
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
          <input id="portal-username" v-model="form.username" autocomplete="username" placeholder="请输入门户账号" />
        </div>
        <div class="field">
          <label for="portal-password">密码</label>
          <input
            id="portal-password"
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入登录密码"
          />
        </div>
        <div class="field">
          <label for="portal-captcha">验证码</label>
          <div class="captcha-row">
            <input
              id="portal-captcha"
              v-model="form.captchaCode"
              maxlength="8"
              autocomplete="off"
              placeholder="请输入验证码"
            />
            <button type="button" class="captcha-image" :disabled="captchaLoading" @click="loadCaptcha">
              <img v-if="captcha.image" :src="captcha.image" alt="验证码" />
              <span v-else>{{ captchaLoading ? '加载中' : '点击加载' }}</span>
            </button>
          </div>
        </div>

        <p v-if="error" class="login-error">{{ error }}</p>

        <button type="submit" class="login-button" :disabled="session.loading">
          {{ session.loading ? '登录中...' : '进入门户' }}
          <svg viewBox="0 0 24 24"><path d="M5 12h13M13 6l6 6-6 6" /></svg>
        </button>
      </form>

      <button type="button" class="demo-link" @click="fillDemo">使用演示账号 biz01 / Biz@123</button>
    </section>

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
  gap: 8px;
}

.captcha-row input {
  flex: 1;
}

.captcha-image {
  display: grid;
  place-items: center;
  width: 104px;
  height: 48px;
  flex: 0 0 104px;
  overflow: hidden;
  padding: 0;
  border: 1px solid #e5eaf2;
  border-radius: 13px;
  background: #f9fbfe;
  cursor: pointer;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-image span {
  color: #8d96a8;
  font-size: 9px;
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
