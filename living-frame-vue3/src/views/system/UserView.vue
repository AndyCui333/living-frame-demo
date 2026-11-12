<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search, Upload, UserFilled } from '@element-plus/icons-vue'
import {
  assignUserRoles,
  createUser,
  deleteUser,
  getUserRoles,
  getUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus,
} from '@/api/system/user'
import { getAllRoles } from '@/api/system/role'
import { uploadAvatar } from '@/api/avatar'
import { resolveAvatarUrl } from '@/utils/avatar'
import { useAuthStore } from '@/stores/auth'
import type { RoleRecord, UserRecord } from '@/types/api'

const auth = useAuthStore()

const rows = ref<UserRecord[]>([])
const total = ref(0)
const loading = ref(false)
const roleOptions = ref<RoleRecord[]>([])

function isSuperAdminTarget(row: UserRecord): boolean {
  return row.roles?.some((role) => role.code === 'SUPER_ADMIN') === true
}

function canManageSuperAdminTarget(row: UserRecord): boolean {
  return auth.isSuperAdmin || !isSuperAdminTarget(row)
}

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
})

interface UserFormModel {
  id: string
  username: string
  password: string
  nickname: string
  email: string
  phone: string
  avatar: string
  roleIds: string[]
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<UserFormModel>({
  id: '',
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  roleIds: [],
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
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const roleDialogVisible = ref(false)
const roleDialogUser = ref<UserRecord | null>(null)
const selectedRoleIds = ref<string[]>([])
const roleSubmitting = ref(false)

async function loadUsers() {
  loading.value = true
  try {
    const page = await getUsers({
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

async function loadRoleOptions() {
  try {
    const roles = await getAllRoles()
    roleOptions.value = auth.isSuperAdmin
      ? roles
      : roles.filter((role) => role.code !== 'SUPER_ADMIN')
  } catch {
    roleOptions.value = []
  }
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
  form.email = ''
  form.phone = ''
  form.avatar = ''
  form.roleIds = []
  formRef.value?.clearValidate()
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: UserRecord) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.username = row.username
  form.nickname = row.nickname ?? ''
  form.email = row.email ?? ''
  form.phone = row.phone ?? ''
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
      await createUser({
        username: form.username.trim(),
        password: form.password,
        nickname: form.nickname.trim() || undefined,
        email: form.email.trim() || undefined,
        phone: form.phone.trim() || undefined,
        avatar: form.avatar.trim() || undefined,
        roleIds: form.roleIds,
      })
      ElMessage.success('用户创建成功')
    } else {
      await updateUser(form.id, {
        nickname: form.nickname.trim() || undefined,
        email: form.email.trim() || undefined,
        phone: form.phone.trim() || undefined,
        avatar: form.avatar.trim() || undefined,
      })
      ElMessage.success('用户信息已更新')
    }
    dialogVisible.value = false
    await loadUsers()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: UserRecord, value: string | number | boolean) {
  const nextStatus = value ? 1 : 0
  try {
    await updateUserStatus(row.id, nextStatus)
    row.status = nextStatus
    ElMessage.success(nextStatus === 1 ? '用户已启用' : '用户已禁用')
  } catch {
    // 保持原状态
  }
}

async function handleDelete(row: UserRecord) {
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${row.username}」吗？删除后该账号将无法登录。`,
      '删除用户',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    if (rows.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    await loadUsers()
  } catch {
    // 用户取消
  }
}

async function handleResetPassword(row: UserRecord) {
  try {
    const result = await ElMessageBox.prompt(`为「${row.username}」设置新密码`, '重置密码', {
      inputType: 'password',
      inputPattern: /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/,
      inputErrorMessage: '密码需为 8-32 位，且同时包含字母和数字',
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
    })
    await resetUserPassword(row.id, result.value)
    ElMessage.success('密码已重置，该用户需要重新登录')
  } catch {
    // 用户取消
  }
}

async function openRoleDialog(row: UserRecord) {
  roleDialogUser.value = row
  roleSubmitting.value = true
  roleDialogVisible.value = true
  try {
    const roles = await getUserRoles(row.id)
    selectedRoleIds.value = roles.map((role) => role.id)
  } catch {
    selectedRoleIds.value = []
  } finally {
    roleSubmitting.value = false
  }
}

async function saveRoles() {
  if (!roleDialogUser.value) {
    return
  }
  roleSubmitting.value = true
  try {
    await assignUserRoles(roleDialogUser.value.id, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    await loadUsers()
  } finally {
    roleSubmitting.value = false
  }
}

onMounted(() => {
  loadUsers()
  loadRoleOptions()
})
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">维护后台账号、启用状态与角色绑定关系。</p>
      </div>
      <el-button v-permission="'system:user:create'" type="primary" :icon="Plus" @click="openCreate">
        新增用户
      </el-button>
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
        <div class="toolbar-right text-muted">共 {{ total }} 个账号</div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="rows" row-key="id">
          <el-table-column label="用户" min-width="210">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="34" :src="resolveAvatarUrl(row.avatar)">
                  {{ (row.nickname || row.username || 'U').slice(0, 1).toUpperCase() }}
                </el-avatar>
                <div class="user-cell-text">
                  <strong>{{ row.username }}</strong>
                  <span>{{ row.nickname || '未设置昵称' }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" min-width="180">
            <template #default="{ row }">
              <div v-if="row.roles?.length" class="role-tags">
                <el-tag v-for="role in row.roles" :key="role.id" size="small" effect="plain">
                  {{ role.name }}
                </el-tag>
              </div>
              <span v-else class="text-muted">未分配</span>
            </template>
          </el-table-column>
          <el-table-column label="联系方式" min-width="190">
            <template #default="{ row }">
              <div class="contact-cell">
                <span>{{ row.email || '—' }}</span>
                <span v-if="row.phone">{{ row.phone }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                :disabled="!canManageSuperAdminTarget(row as UserRecord)"
                @change="(value) => toggleStatus(row as UserRecord, value)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginAt" label="最后登录" width="165">
            <template #default="{ row }">
              <span class="text-muted">{{ row.lastLoginAt || '从未登录' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="156" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="编辑" placement="top">
                <el-button
                  v-permission="'system:user:update'"
                  link
                  type="primary"
                  :icon="Edit"
                  aria-label="编辑"
                  @click="openEdit(row as UserRecord)"
                />
              </el-tooltip>
              <el-tooltip content="分配角色" placement="top">
                <el-button
                  v-permission="'system:user:assign-role'"
                  link
                  type="primary"
                  :icon="UserFilled"
                  aria-label="分配角色"
                  :disabled="!canManageSuperAdminTarget(row as UserRecord)"
                  @click="openRoleDialog(row as UserRecord)"
                />
              </el-tooltip>
              <el-tooltip content="重置密码" placement="top">
                <el-button
                  v-permission="'system:user:reset-password'"
                  link
                  type="warning"
                  :icon="Key"
                  aria-label="重置密码"
                  :disabled="!canManageSuperAdminTarget(row as UserRecord)"
                  @click="handleResetPassword(row as UserRecord)"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button
                  v-permission="'system:user:delete'"
                  link
                  type="danger"
                  :icon="Delete"
                  aria-label="删除"
                  :disabled="!canManageSuperAdminTarget(row as UserRecord)"
                  @click="handleDelete(row as UserRecord)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无用户数据" :image-size="80" />
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
      :title="dialogMode === 'create' ? '新增用户' : '编辑用户'"
      width="560px"
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
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="name@example.com" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="可选" />
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
        <el-form-item v-if="dialogMode === 'create'" label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色（可稍后分配）" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="460px" destroy-on-close>
      <div class="role-dialog-head">
        <el-avatar :size="38">{{ (roleDialogUser?.nickname || roleDialogUser?.username || 'U').slice(0, 1) }}</el-avatar>
        <div>
          <strong>{{ roleDialogUser?.username }}</strong>
          <p>{{ roleDialogUser?.nickname || '未设置昵称' }}</p>
        </div>
      </div>
      <el-checkbox-group v-model="selectedRoleIds" class="role-check-list">
        <label v-for="role in roleOptions" :key="role.id" class="role-check-item">
          <el-checkbox :value="role.id">
            <span class="role-check-name">{{ role.name }}</span>
            <code>{{ role.code }}</code>
          </el-checkbox>
        </label>
      </el-checkbox-group>
      <el-empty v-if="!roleOptions.length" description="暂无可分配角色" :image-size="70" />
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
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
  overflow: hidden;
  color: #263042;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.contact-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
}

.contact-cell span:first-child {
  color: #4b5563;
}

.contact-cell span:last-child {
  color: #9aa3b5;
  font-size: 11px;
}

.role-dialog-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 0 16px;
  border-bottom: 1px dashed #eef1f7;
}

.role-dialog-head strong {
  display: block;
  color: #263042;
  font-size: 13px;
}

.role-dialog-head p {
  margin: 2px 0 0;
  color: #9aa3b5;
  font-size: 11px;
}

.role-check-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 14px;
}

.role-check-item {
  display: block;
  cursor: pointer;
}

.role-check-item :deep(.el-checkbox) {
  display: flex;
  align-items: center;
  width: 100%;
  height: auto;
  padding: 9px 10px;
  border-radius: 8px;
}

.role-check-item :deep(.el-checkbox:hover) {
  background: #f7f9ff;
}

.role-check-name {
  color: #263042;
  font-size: 13px;
}

.role-check-item code {
  margin-left: 8px;
  color: #a1a9b8;
  font-size: 10px;
}
</style>
