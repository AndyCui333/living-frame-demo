<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowDown, Expand, Fold, Lock, SwitchButton, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { changePassword } from '@/api/auth'
import { resolveAvatarUrl } from '@/utils/avatar'

const props = defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ toggle: [] }>()

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const profileVisible = ref(false)
const passwordVisible = ref(false)
const passwordSubmitting = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/,
      message: '8-32 位，且同时包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const roleNames: Record<string, string> = {
  SUPER_ADMIN: '超级管理员',
  SYSTEM_ADMIN: '系统管理员',
  BUSINESS_ADMIN: '业务管理员',
  VIEWER: '只读用户',
  BIZ_USER: '业务用户',
}

const roleTags = computed(() =>
  (auth.user?.roles ?? []).map((role) => ({ code: role, label: roleNames[role] ?? role })),
)

const currentTitle = computed(() => (route.meta.title as string) || '工作台')
const avatarText = computed(() => (auth.displayName || 'U').slice(0, 1).toUpperCase())

async function handleCommand(command: string) {
  if (command === 'profile') {
    profileVisible.value = true
  } else if (command === 'password') {
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordVisible.value = true
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await auth.logout()
      router.replace('/login')
    } catch {
      // 用户取消
    }
  }
}

async function submitPassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  passwordSubmitting.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordVisible.value = false
    ElMessage.success('密码已修改，请重新登录')
    await auth.logout()
    router.replace('/login')
  } finally {
    passwordSubmitting.value = false
  }
}
</script>

<template>
  <header class="header-bar">
    <div class="header-left">
      <div class="breadcrumb">
        <span class="breadcrumb-root">Living Frame</span>
        <span class="breadcrumb-sep">/</span>
        <span class="breadcrumb-current">{{ currentTitle }}</span>
      </div>
      <el-button text class="collapse-btn" @click="emit('toggle')">
        <el-icon :size="20">
          <Expand v-if="props.collapsed" />
          <Fold v-else />
        </el-icon>
      </el-button>
    </div>

    <div class="header-right">
      <div class="role-tags header-roles">
        <el-tag v-for="role in roleTags" :key="role.code" size="small" effect="plain" round>
          {{ role.label }}
        </el-tag>
      </div>

      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-trigger">
          <el-avatar :size="34" :src="resolveAvatarUrl(auth.user?.avatar)">
            {{ avatarText }}
          </el-avatar>
          <div class="user-meta">
            <span class="user-name">{{ auth.displayName }}</span>
            <span class="user-account">@{{ auth.user?.username }}</span>
          </div>
          <el-icon class="user-arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" :icon="User">个人信息</el-dropdown-item>
            <el-dropdown-item command="password" :icon="Lock">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>

  <el-dialog v-model="profileVisible" title="个人信息" width="460px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="用户名">{{ auth.user?.username }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ auth.user?.nickname || '—' }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ auth.user?.email || '—' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ auth.user?.phone || '—' }}</el-descriptions-item>
      <el-descriptions-item label="角色">
        <div class="role-tags">
          <el-tag v-for="role in roleTags" :key="role.code" size="small">{{ role.label }}</el-tag>
        </div>
      </el-descriptions-item>
      <el-descriptions-item label="权限数量">
        {{ auth.user?.superAdmin ? '全部权限' : `${auth.user?.permissions.length ?? 0} 项` }}
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>

  <el-dialog v-model="passwordVisible" title="修改密码" width="440px" destroy-on-close>
    <el-form
      ref="passwordFormRef"
      :model="passwordForm"
      :rules="passwordRules"
      label-width="82px"
      @submit.prevent
    >
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="off" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordVisible = false">取消</el-button>
      <el-button type="primary" :loading="passwordSubmitting" @click="submitPassword">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 22px;
  border-bottom: 1px solid var(--lf-border);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  color: #6b7280;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.breadcrumb-root {
  color: #9ca3af;
}

.breadcrumb-sep {
  color: #d1d5db;
}

.breadcrumb-current {
  color: #111827;
  font-weight: 600;
}

.header-roles {
  max-width: 260px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 8px 5px 5px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.user-trigger:hover {
  background: #f3f5fb;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.user-account {
  font-size: 11px;
  color: #9ca3af;
}

.user-arrow {
  color: #9ca3af;
}

@media (max-width: 900px) {
  .header-roles {
    display: none;
  }

  .user-meta {
    display: none;
  }
}
</style>
