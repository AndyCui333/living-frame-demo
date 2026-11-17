<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { portalApi } from '../api'
import { resolveAvatar } from '../avatar'
import { loadProfile, session } from '../session'

const router = useRouter()
const fileInput = ref<HTMLInputElement | null>(null)
const saving = ref(false)
const uploading = ref(false)
const form = reactive({ nickname: '', phone: '', email: '', avatar: '' })

const avatarPreview = computed(() => resolveAvatar(form.avatar))
const nicknameInitial = computed(() => (form.nickname || session.user?.username || '我').slice(0, 1).toUpperCase())

function fillFromSession() {
  form.nickname = session.user?.nickname || ''
  form.phone = session.user?.phone || ''
  form.email = session.user?.email || ''
  form.avatar = session.user?.avatar || ''
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/me')
  }
}

function pickAvatar() {
  fileInput.value?.click()
}

async function onAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    form.avatar = await portalApi.uploadAvatar(file)
  } catch (error: any) {
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    uploading.value = false
  }
}

async function saveProfile() {
  if (!form.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    session.user = await portalApi.updateProfile({
      nickname: form.nickname.trim(),
      phone: form.phone.trim(),
      email: form.email.trim(),
      avatar: form.avatar,
    })
    fillFromSession()
    ElMessage.success('个人资料已保存')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!session.user) {
    await loadProfile().catch(() => undefined)
  }
  fillFromSession()
})
</script>

<template>
  <div class="page-view profile-page">
    <header class="profile-header">
      <button type="button" class="back-button" aria-label="返回" @click="goBack">
        <svg viewBox="0 0 24 24"><path d="m15 5-7 7 7 7" /></svg>
      </button>
      <div>
        <span class="eyebrow">PROFILE</span>
        <h1>个人资料</h1>
      </div>
    </header>

    <section class="identity-card card">
      <button type="button" class="avatar-edit" :disabled="uploading" @click="pickAvatar">
        <div class="avatar-large">
          <img v-if="avatarPreview" :src="avatarPreview" alt="头像" />
          <span v-else>{{ nicknameInitial }}</span>
        </div>
        <span class="avatar-camera">
          <svg viewBox="0 0 24 24">
            <path d="M4 8h3l1.5-2h7L17 8h3v11H4z" />
            <circle cx="12" cy="13" r="3.2" />
          </svg>
        </span>
      </button>
      <input ref="fileInput" class="file-input" type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="onAvatarChange" />
      <div class="identity-copy">
        <strong>{{ form.nickname || '未设置昵称' }}</strong>
        <span>{{ uploading ? '头像上传中...' : '点击头像更换图片' }}</span>
      </div>
    </section>

    <div class="section-title">
      <strong>基本信息</strong>
      <span>仅自己可见</span>
    </div>
    <section class="form-card card">
      <label class="profile-field">
        <span>昵称</span>
        <el-input v-model="form.nickname" size="large" maxlength="64" placeholder="请输入昵称" />
      </label>
      <label class="profile-field">
        <span>手机号</span>
        <el-input v-model="form.phone" size="large" maxlength="32" inputmode="tel" placeholder="请输入手机号" />
      </label>
      <label class="profile-field">
        <span>邮箱</span>
        <el-input v-model="form.email" size="large" maxlength="128" inputmode="email" placeholder="请输入邮箱" />
      </label>
      <el-button type="primary" class="primary-button save-button" :loading="saving" :disabled="uploading" @click="saveProfile">
        {{ saving ? '保存中...' : '保存修改' }}
      </el-button>
    </section>

    <p class="profile-footer">Living Frame · 账户安全由你掌握</p>
  </div>
</template>

<style scoped>
.profile-page {
  padding-top: calc(env(safe-area-inset-top, 0px) + 20px);
}

.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.profile-header h1 {
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

.identity-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 22px;
  padding: 17px;
}

.avatar-edit {
  position: relative;
  width: 72px;
  height: 72px;
  flex: 0 0 72px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.avatar-large {
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  overflow: hidden;
  border: 3px solid #ffffff;
  border-radius: 23px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f6bfb, #7658ff);
  box-shadow: 0 8px 22px rgba(79, 107, 251, 0.22);
  font-size: 25px;
  font-weight: 700;
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-camera {
  position: absolute;
  right: -4px;
  bottom: -4px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 2px solid #ffffff;
  border-radius: 9px;
  color: #4f6bfb;
  background: #eef2ff;
}

.avatar-camera svg {
  width: 13px;
  height: 13px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}

.file-input {
  display: none;
}

.identity-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.identity-copy strong {
  color: #263042;
  font-size: 16px;
}

.identity-copy span {
  color: #9aa3b5;
  font-size: 10px;
}

.form-card {
  padding: 16px;
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

.profile-footer {
  margin: 26px 0 0;
  color: #b4bcc9;
  font-size: 9px;
  letter-spacing: 0.06em;
  text-align: center;
}
</style>
