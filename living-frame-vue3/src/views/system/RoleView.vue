<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  assignRolePermissions,
  createRole,
  deleteRole,
  getAllRoles,
  getRolePermissions,
  getRoles,
  updateRole,
} from '@/api/system/role'
import { getPermissionTree } from '@/api/system/permission'
import { useAuthStore } from '@/stores/auth'
import type { PermissionRecord, RoleRecord } from '@/types/api'
import { ElTree } from 'element-plus'

const auth = useAuthStore()

const rows = ref<RoleRecord[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
})

interface RoleFormModel {
  id: string
  code: string
  name: string
  description: string
  sort: number
  status: number
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<RoleFormModel>({
  id: '',
  code: '',
  name: '',
  description: '',
  sort: 0,
  status: 1,
})

const rules: FormRules = {
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

const permissionVisible = ref(false)
const permissionLoading = ref(false)
const permissionSubmitting = ref(false)
const permissionTree = ref<PermissionRecord[]>([])
const permissionRole = ref<RoleRecord | null>(null)
const treeRef = ref<InstanceType<typeof ElTree>>()

async function loadRoles() {
  loading.value = true
  try {
    const page = await getRoles({
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

function handleSearch() {
  query.pageNum = 1
  loadRoles()
}

function handleReset() {
  query.keyword = ''
  query.status = undefined
  query.pageNum = 1
  loadRoles()
}

function resetForm() {
  form.id = ''
  form.code = ''
  form.name = ''
  form.description = ''
  form.sort = 0
  form.status = 1
  formRef.value?.clearValidate()
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.sort = (rows.value.length + 1) * 10
  dialogVisible.value = true
}

function openEdit(row: RoleRecord) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.code = row.code
  form.name = row.name
  form.description = row.description ?? ''
  form.sort = row.sort
  form.status = row.status
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createRole({
        code: form.code.trim(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        sort: form.sort,
      })
      ElMessage.success('角色创建成功')
    } else {
      await updateRole(form.id, {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        sort: form.sort,
        status: form.status,
      })
      ElMessage.success('角色信息已更新')
    }
    dialogVisible.value = false
    await loadRoles()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: RoleRecord, value: string | number | boolean) {
  const nextStatus = value ? 1 : 0
  try {
    await updateRole(row.id, {
      name: row.name,
      description: row.description,
      sort: row.sort,
      status: nextStatus,
    })
    row.status = nextStatus
    ElMessage.success(nextStatus === 1 ? '角色已启用' : '角色已禁用')
  } catch {
    // 保持原状态
  }
}

async function handleDelete(row: RoleRecord) {
  try {
    await ElMessageBox.confirm(
      `确定删除角色「${row.name}」吗？角色仍绑定用户时无法删除。`,
      '删除角色',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteRole(row.id)
    ElMessage.success('角色已删除')
    if (rows.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    await loadRoles()
  } catch {
    // 用户取消
  }
}

async function openPermissionDialog(row: RoleRecord) {
  permissionRole.value = row
  permissionVisible.value = true
  permissionLoading.value = true
  try {
    const [tree, checkedIds] = await Promise.all([getPermissionTree(), getRolePermissions(row.id)])
    permissionTree.value = tree
    await nextTick()
    treeRef.value?.setCheckedKeys(checkedIds, false)
  } catch {
    permissionTree.value = []
  } finally {
    permissionLoading.value = false
  }
}

async function savePermissions() {
  if (!permissionRole.value || !treeRef.value) {
    return
  }
  permissionSubmitting.value = true
  try {
    const checked = treeRef.value.getCheckedKeys(false) as string[]
    const halfChecked = treeRef.value.getHalfCheckedKeys() as string[]
    const permissionIds = Array.from(new Set([...checked, ...halfChecked]))
    await assignRolePermissions(permissionRole.value.id, permissionIds)
    ElMessage.success('权限分配成功，相关用户需要重新登录')
    permissionVisible.value = false
  } finally {
    permissionSubmitting.value = false
  }
}

onMounted(() => {
  loadRoles()
  // 预加载角色下拉数据，用于其他页面共享（此处保持接口连通性）
  getAllRoles().catch(() => undefined)
})
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">角色管理</h1>
        <p class="page-subtitle">通过角色组合菜单与按钮权限，再将角色分配给用户。</p>
      </div>
      <el-button v-permission="'system:role:create'" type="primary" :icon="Plus" @click="openCreate">
        新增角色
      </el-button>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="query.keyword"
            placeholder="搜索角色编码 / 名称"
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
        <div class="toolbar-right text-muted">共 {{ total }} 个角色</div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="rows" row-key="id">
          <el-table-column label="角色" min-width="210">
            <template #default="{ row }">
              <div class="role-cell">
                <span class="role-badge">{{ (row.name || 'R').slice(0, 1) }}</span>
                <div>
                  <strong>{{ row.name }}</strong>
                  <code>{{ row.code }}</code>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="220">
            <template #default="{ row }">{{ row.description || '—' }}</template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="80" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                :disabled="row.code === 'SUPER_ADMIN' || !auth.hasPermission('system:role:update')"
                @change="(value) => toggleStatus(row as RoleRecord, value)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="165">
            <template #default="{ row }">
              <span class="text-muted">{{ row.createdAt || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="142" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="编辑" placement="top">
                <el-button
                  v-permission="'system:role:update'"
                  link
                  type="primary"
                  :icon="Edit"
                  aria-label="编辑"
                  @click="openEdit(row as RoleRecord)"
                />
              </el-tooltip>
              <el-tooltip content="分配权限" placement="top">
                <el-button
                  v-permission="'system:role:assign-permission'"
                  link
                  type="primary"
                  :icon="Key"
                  aria-label="分配权限"
                  @click="openPermissionDialog(row as RoleRecord)"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button
                  v-permission="'system:role:delete'"
                  link
                  type="danger"
                  :icon="Delete"
                  aria-label="删除"
                  :disabled="row.code === 'SUPER_ADMIN'"
                  @click="handleDelete(row as RoleRecord)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无角色数据" :image-size="80" />
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
          @current-change="loadRoles"
          @size-change="handleSearch"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增角色' : '编辑角色'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="82px" @submit.prevent>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" :disabled="dialogMode === 'edit'" placeholder="例如 BUSINESS_ADMIN" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="例如 业务管理员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="角色职责说明" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionVisible" title="分配权限" width="560px" destroy-on-close>
      <div class="permission-head">
        <span class="role-badge">{{ (permissionRole?.name || 'R').slice(0, 1) }}</span>
        <div>
          <strong>{{ permissionRole?.name }}</strong>
          <code>{{ permissionRole?.code }}</code>
        </div>
        <span class="text-muted permission-tip">勾选菜单与按钮权限</span>
      </div>
      <div v-loading="permissionLoading" class="permission-tree">
        <el-tree
          ref="treeRef"
          :data="permissionTree"
          node-key="id"
          show-checkbox
          default-expand-all
          :props="{ label: 'name', children: 'children' }"
          :check-strictly="false"
        >
          <template #default="{ data }">
            <div class="permission-node">
              <span>{{ data.name }}</span>
              <el-tag size="small" :type="data.type === 'MENU' ? 'primary' : 'info'" effect="plain">
                {{ data.type === 'MENU' ? '菜单' : '按钮' }}
              </el-tag>
              <code>{{ data.code }}</code>
            </div>
          </template>
        </el-tree>
        <el-empty v-if="!permissionLoading && !permissionTree.length" description="暂无权限数据" :image-size="70" />
      </div>
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionSubmitting" @click="savePermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.role-badge {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 9px;
  color: #4f6bfb;
  font-size: 13px;
  font-weight: 700;
  background: #eef2ff;
}

.role-cell strong {
  display: block;
  color: #263042;
  font-size: 13px;
}

.role-cell code,
.permission-head code {
  color: #9aa3b5;
  font-size: 10px;
}

.permission-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 0 14px;
  border-bottom: 1px dashed #eef1f7;
}

.permission-head strong {
  display: block;
  color: #263042;
  font-size: 13px;
}

.permission-tip {
  margin-left: auto;
  font-size: 11px;
}

.permission-tree {
  min-height: 260px;
  max-height: 420px;
  padding-top: 12px;
  overflow: auto;
}

.permission-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.permission-node code {
  margin-left: auto;
  color: #b0b7c4;
  font-size: 10px;
}
</style>
