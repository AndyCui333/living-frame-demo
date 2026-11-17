<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { portalApi } from '../api'
import { logout } from '../session'

const router = useRouter()
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/me')
  }
}

async function submit() {
  if (!form.oldPassword || !form.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,32}$/.test(form.newPassword)) {
    ElMessage.warning('新密码需为 8-32 位，且同时包含字母和数字')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await portalApi.changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    await logout()
    ElMessage.success('密码修改成功，请重新登录')
    router.replace('/login')
  } catch (error: any) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-view password-page">
    <header class="password-header">
      <button type="button" class="back-button" aria-label="返回" @click="goBack">
        <svg viewBox="0 0 24 24"><path d="m15 5-7 7 7 7" /></svg>
      </button>
      <div>
        <span class="eyebrow">SECURITY</span>
        <h1>修改密码</h1>
      </div>
    </header>

    <section class="password-card card">
      <p class="password-tip">为了账户安全，修改密码后需要重新登录。</p>
      <label class="profile-field">
        <span>原密码</span>
        <el-input v-model="form.oldPassword" type="password" size="large" show-password autocomplete="current-password" placeholder="请输入原密码" />
      </label>
      <label class="profile-field">
        <span>新密码</span>
        <el-input v-model="form.newPassword" type="password" size="large" show-password autocomplete="new-password" placeholder="8-32 位字母和数字" />
      </label>
      <label class="profile-field">
        <span>确认新密码</span>
        <el-input v-model="form.confirmPassword" type="password" size="large" show-password autocomplete="new-password" placeholder="请再次输入新密码" />
      </label>
      <el-button type="primary" class="primary-button save-button" :loading="saving" @click="submit">
        {{ saving ? '提交中...' : '确认修改' }}
      </el-button>
    </section>

    <p class="password-footer">Living Frame · 账户安全由你掌握</p>
  </div>
</template>

<style scoped>
.password-page {
  padding-top: calc(env(safe-area-inset-top, 0px) + 20px);
}

.password-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.password-header h1 {
  margin: 5px 0 0;
  color: #172033;
  font-size: 25px;
  letter-spacing: -0.04em;
}

.back-button {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  margin-top: 2px;
  padding: 0;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  color: #5b687e;
  background: #ffffff;
  cursor: pointer;
}

.back-button svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.password-card {
  margin-top: 22px;
  padding: 16px;
}

.password-tip {
  margin: 0 0 16px;
  padding: 10px 12px;
  border-radius: 11px;
  color: #7b6a45;
  background: #fffbf0;
  font-size: 10px;
  line-height: 1.6;
}

.profile-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 13px;
}

.profile-field span {
  color: #6d788c;
  font-size: 10px;
}

.profile-field :deep(.el-input__wrapper) {
  min-height: 44px;
  padding: 0 13px;
  border: 1px solid #e7ecf4;
  border-radius: 12px;
  background: #f8fafe;
  box-shadow: none;
}

.profile-field :deep(.el-input__wrapper.is-focus) {
  border-color: #b9c6ff;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(79, 107, 251, 0.08);
}

.profile-field :deep(.el-input__inner) {
  height: 42px;
  color: #263042;
  font-size: 12px;
}

.save-button {
  width: 100%;
  height: 43px;
  margin: 4px 0 0;
  border: 0;
  border-radius: 12px;
  font-size: 11px;
}

.password-footer {
  margin: 26px 0 0;
  color: #b4bcc9;
  font-size: 9px;
  letter-spacing: 0.06em;
  text-align: center;
}
</style>
