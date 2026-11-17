<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search, Upload, UserFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import {
  blacklistBusinessUser,
  createBusinessUser,
  deleteBusinessUser,
  getBusinessUserActivity,
  getBusinessUsers,
  resetBusinessUserPassword,
  updateBusinessUser,
  updateBusinessUserStatus,
} from '@/api/business/user'
import { uploadAvatar } from '@/api/avatar'
import { resolveAvatarUrl } from '@/utils/avatar'
import type { BusinessUserActivity, BusinessUserRecord } from '@/types/api'

const rows = ref<BusinessUserRecord[]>([])
const total = ref(0)
const loading = ref(false)
const activity = ref<BusinessUserActivity | null>(null)
const activityLoading = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
})

interface BusinessFormModel {
  id: string
  username: string
  password: string
  nickname: string
  phone: string
  email: string
  avatar: string
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<BusinessFormModel>({
  id: '',
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '用户名长度需为 4-32 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        if (dialogMode.value === 'create' && !value) {
          callback(new Error('请输入初始密码'))
          return
        }
        if (value && !/^(?=.*[A-Za-z])(?=.*\d).{8,32}$/.test(value)) {
          callback(new Error('密码需为 8-32 位，且同时包含字母和数字'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  email: [{
    validator: (_rule, value: string, callback) => {
      if (!value || value.includes('*')) {
        callback()
        return
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        callback(new Error('邮箱格式不正确'))
        return
      }
      callback()
    },
    trigger: 'blur',
  }],
}

async function loadUsers() {
  loading.value = true
  try {
    const page = await getBusinessUsers({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      status: typeof query.status === 'number' ? query.status : undefined,
    })
    rows.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function loadActivity() {
  activityLoading.value = true
  try {
    activity.value = await getBusinessUserActivity()
  } finally {
    activityLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadUsers(), loadActivity()])
}

function handleSearch() {
  query.pageNum = 1
  loadUsers()
}

function handleReset() {
  query.keyword = ''
  query.status = undefined
  query.pageNum = 1
  loadUsers()
}

function resetForm() {
  form.id = ''
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.phone = ''
  form.email = ''
  form.avatar = ''
  formRef.value?.clearValidate()
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: BusinessUserRecord) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.username = row.username
  form.nickname = row.nickname ?? ''
  form.phone = row.phone ?? ''
  form.email = row.email ?? ''
  form.avatar = row.avatar ?? ''
  dialogVisible.value = true
}

async function handleAvatarUpload(options: UploadRequestOptions) {
  try {
    const path = await uploadAvatar(options.file as File)
    form.avatar = path
    options.onSuccess?.(path)
    ElMessage.success('头像已上传')
  } catch (error) {
    options.onError?.(error as Parameters<NonNullable<typeof options.onError>>[0])
  }
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createBusinessUser({
        username: form.username.trim(),
        password: form.password,
        nickname: form.nickname.trim() || undefined,
        phone: form.phone.trim() || undefined,
        email: form.email.trim() || undefined,
        avatar: form.avatar.trim() || undefined,
      })
      ElMessage.success('业务用户创建成功')
    } else {
      await updateBusinessUser(form.id, {
        nickname: form.nickname.trim() || undefined,
        phone: form.phone.trim() || undefined,
        email: form.email.trim() || undefined,
        avatar: form.avatar.trim() || undefined,
      })
      ElMessage.success('业务用户已更新')
    }
    dialogVisible.value = false
    await loadUsers()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: BusinessUserRecord, value: string | number | boolean) {
  const nextStatus = value ? 1 : 0
  try {
    await updateBusinessUserStatus(row.id, nextStatus)
    row.status = nextStatus
    ElMessage.success(nextStatus === 1 ? '业务用户已启用' : '业务用户已禁用')
    await loadActivity()
  } catch {
    // 保持原状态
  }
}

async function handleDelete(row: BusinessUserRecord) {
  try {
    await ElMessageBox.confirm(`确定删除业务用户「${row.username}」吗？`, '删除业务用户', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteBusinessUser(row.id)
    ElMessage.success('业务用户已删除')
    if (rows.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    await loadUsers()
  } catch {
    // 用户取消
  }
}

async function handleResetPassword(row: BusinessUserRecord) {
  try {
    const result = await ElMessageBox.prompt(`为「${row.username}」设置新密码`, '重置密码', {
      inputType: 'password',
      inputPattern: /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/,
      inputErrorMessage: '密码需为 8-32 位，且同时包含字母和数字',
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
    })
    await resetBusinessUserPassword(row.id, result.value)
    ElMessage.success('密码已重置')
  } catch {
    // 用户取消
  }
}

async function handleBlacklist(row: BusinessUserRecord) {
  if (row.blacklisted) return
  try {
    await ElMessageBox.confirm(
      `确定将门户用户「${row.username}」加入永久黑名单并禁用账号吗？加入后该账号将无法登录。`,
      '一键拉黑',
      { type: 'warning', confirmButtonText: '加入黑名单', cancelButtonText: '取消' },
    )
    await blacklistBusinessUser(row.id)
    ElMessage.success('已加入永久黑名单并禁用账号')
    await refreshAll()
  } catch {
    // 用户取消
  }
}

onMounted(async () => {
  await refreshAll()
})

let activityTimer: number | undefined

onMounted(() => {
  activityTimer = window.setInterval(() => void loadActivity(), 30_000)
})

onUnmounted(() => {
  if (activityTimer) window.clearInterval(activityTimer)
})
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">业务用户</h1>
        <p class="page-subtitle">业务端独立账号体系，与后台用户、角色权限隔离。</p>
      </div>
      <el-button v-permission="'business:user:create'" type="primary" :icon="Plus" @click="openCreate">
        新增业务用户
      </el-button>
    </div>

    <div v-loading="activityLoading" class="activity-grid">
      <div class="activity-card">
        <span>门户用户总数</span>
        <strong>{{ activity?.totalUsers ?? 0 }}</strong>
        <small>今日新增 {{ activity?.newUsersToday ?? 0 }}</small>
      </div>
      <div class="activity-card online">
        <span>当前在线</span>
        <strong>{{ activity?.onlineUsers ?? 0 }}</strong>
        <small>在线率 {{ activity?.onlineRate ?? 0 }}%</small>
      </div>
      <div class="activity-card today">
        <span>今日活跃</span>
        <strong>{{ activity?.todayActiveUsers ?? 0 }}</strong>
        <small>活跃率 {{ activity?.todayActiveRate ?? 0 }}%</small>
      </div>
      <div class="activity-card seven">
        <span>近 7 日活跃</span>
        <strong>{{ activity?.sevenDayActiveUsers ?? 0 }}</strong>
        <small>禁用 {{ activity?.disabledUsers ?? 0 }} · 黑名单 {{ activity?.blacklistedUsers ?? 0 }}</small>
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="query.keyword"
            placeholder="搜索用户名 / 昵称"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" plain :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </div>
        <div class="toolbar-right text-muted">共 {{ total }} 个业务账号</div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="rows" row-key="id">
          <el-table-column label="业务用户" min-width="190">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="34" :src="resolveAvatarUrl(row.avatar)">
                  {{ (row.nickname || row.username || 'B').slice(0, 1).toUpperCase() }}
                </el-avatar>
                <div class="user-cell-text">
                  <strong>{{ row.username }}</strong>
                  <span>{{ row.nickname || '未设置昵称' }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="130">
            <template #default="{ row }">{{ row.phone || '—' }}</template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.email || '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="70" align="center">
            <template #default="{ row }">
              <el-switch :model-value="row.status === 1" @change="(value) => toggleStatus(row as BusinessUserRecord, value)" />
            </template>
          </el-table-column>
          <el-table-column label="在线" width="70" align="center">
            <template #default="{ row }">
              <el-tag :type="row.online ? 'success' : 'info'" effect="plain" size="small">
                {{ row.online ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastActiveAt" label="最近活跃" width="160">
            <template #default="{ row }">
              <span class="text-muted">{{ row.lastActiveAt || '暂无活跃' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="150">
            <template #default="{ row }">
              <span class="text-muted">{{ row.createdAt || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="编辑" placement="top">
                <el-button
                  v-permission="'business:user:update'"
                  link
                  type="primary"
                  :icon="Edit"
                  aria-label="编辑"
                  @click="openEdit(row as BusinessUserRecord)"
                />
              </el-tooltip>
              <el-tooltip content="重置密码" placement="top">
                <el-button
                  v-permission="'business:user:update'"
                  link
                  type="warning"
                  :icon="Key"
                  aria-label="重置密码"
                  @click="handleResetPassword(row as BusinessUserRecord)"
                />
              </el-tooltip>
              <el-tooltip :content="row.blacklisted ? '已在黑名单中' : '加入黑名单'" placement="top">
                <el-button
                  v-permission="'business:user:update'"
                  link
                  type="danger"
                  :icon="CircleCloseFilled"
                  :disabled="row.blacklisted"
                  aria-label="加入黑名单"
                  @click="handleBlacklist(row as BusinessUserRecord)"
                >
                  拉黑
                </el-button>
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button
                  v-permission="'business:user:delete'"
                  link
                  type="danger"
                  :icon="Delete"
                  aria-label="删除"
                  @click="handleDelete(row as BusinessUserRecord)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无业务用户" :image-size="80" />
          </template>
        </el-table>
      </div>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadUsers"
          @size-change="handleSearch"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增业务用户' : '编辑业务用户'"
      width="540px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="82px" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="dialogMode === 'edit'" placeholder="4-32 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="8-32 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="展示名称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="可选" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="name@example.com" />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-editor">
            <el-avatar :size="64" :src="resolveAvatarUrl(form.avatar)">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <div class="avatar-actions">
              <el-upload
                :show-file-list="false"
                accept="image/jpeg,image/png,image/gif,image/webp"
                :http-request="handleAvatarUpload"
              >
                <el-button :icon="Upload">上传头像</el-button>
              </el-upload>
              <el-button v-if="form.avatar" link type="danger" @click="form.avatar = ''">移除</el-button>
            </div>
          </div>
          <div class="form-hint">支持 JPG、PNG、GIF、WebP，最大 5MB；保存的是 MinIO 半路径。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.activity-card {
  position: relative;
  overflow: hidden;
  padding: 16px 18px;
  border: 1px solid #e8edf5;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(31, 45, 70, 0.04);
}

.activity-card::after {
  position: absolute;
  top: -28px;
  right: -20px;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: rgba(79, 107, 251, 0.08);
  content: "";
}

.activity-card span {
  display: block;
  color: #8a94a6;
  font-size: 11px;
}

.activity-card strong {
  display: block;
  margin: 7px 0 4px;
  color: #263042;
  font-size: 26px;
  line-height: 1;
}

.activity-card small {
  color: #9aa3b5;
  font-size: 10px;
}

.activity-card.online::after {
  background: rgba(30, 167, 113, 0.12);
}

.activity-card.online strong {
  color: #16835c;
}

.activity-card.today::after {
  background: rgba(80, 107, 251, 0.13);
}

.activity-card.today strong {
  color: #4f6bfb;
}

.activity-card.seven::after {
  background: rgba(139, 92, 246, 0.12);
}

.activity-card.seven strong {
  color: #7c4fe0;
}

@media (max-width: 1000px) {
  .activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-cell-text strong {
  color: #263042;
  font-size: 13px;
}

.user-cell-text span {
  color: #9aa3b5;
  font-size: 11px;
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-hint {
  width: 100%;
  margin-top: 4px;
  color: #9aa3b5;
  font-size: 11px;
  line-height: 1.5;
}
</style>
