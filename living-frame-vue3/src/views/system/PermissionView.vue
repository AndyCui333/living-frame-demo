<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Key, Operation, Plus } from '@element-plus/icons-vue'
import {
  createPermission,
  deletePermission,
  getPermissionTree,
  updatePermission,
} from '@/api/system/permission'
import type { PermissionRecord } from '@/types/api'

const tree = ref<PermissionRecord[]>([])
const loading = ref(false)

interface PermissionFormModel {
  id: string
  code: string
  name: string
  type: 'MENU' | 'BUTTON'
  parentId: string
  path: string
  component: string
  icon: string
  sort: number
  status: number
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<PermissionFormModel>({
  id: '',
  code: '',
  name: '',
  type: 'MENU',
  parentId: '0',
  path: '',
  component: '',
  icon: '',
  sort: 0,
  status: 1,
})

const rules: FormRules = {
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

interface ParentOption {
  id: string
  name: string
  level: number
}

function flattenMenus(nodes: PermissionRecord[], level = 1): ParentOption[] {
  const result: ParentOption[] = []
  for (const node of nodes) {
    if (node.type === 'MENU') {
      result.push({ id: node.id, name: node.name, level })
      result.push(...flattenMenus(node.children ?? [], level + 1))
    }
  }
  return result
}

const parentOptions = computed<ParentOption[]>(() => [
  { id: '0', name: '顶级节点', level: 0 },
  ...flattenMenus(tree.value),
])

const parentNameMap = computed(() => {
  const map = new Map<string, string>()
  const walk = (nodes: PermissionRecord[]) => {
    for (const node of nodes) {
      map.set(node.id, node.name)
      walk(node.children ?? [])
    }
  }
  walk(tree.value)
  return map
})

async function loadTree() {
  loading.value = true
  try {
    tree.value = await getPermissionTree()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = ''
  form.code = ''
  form.name = ''
  form.type = 'MENU'
  form.parentId = '0'
  form.path = ''
  form.component = ''
  form.icon = ''
  form.sort = 0
  form.status = 1
  formRef.value?.clearValidate()
}

function openCreate(parent?: PermissionRecord) {
  dialogMode.value = 'create'
  resetForm()
  if (parent) {
    form.parentId = parent.id
    form.type = parent.type === 'MENU' ? 'BUTTON' : 'MENU'
  }
  form.sort = tree.value.length * 10
  dialogVisible.value = true
}

function openEdit(row: PermissionRecord) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.code = row.code
  form.name = row.name
  form.type = row.type
  form.parentId = row.parentId || '0'
  form.path = row.path ?? ''
  form.component = row.component ?? ''
  form.icon = row.icon ?? ''
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
      await createPermission({
        code: form.code.trim(),
        name: form.name.trim(),
        type: form.type,
        parentId: form.parentId,
        path: form.type === 'MENU' ? form.path.trim() || undefined : undefined,
        component: form.type === 'MENU' ? form.component.trim() || undefined : undefined,
        icon: form.type === 'MENU' ? form.icon.trim() || undefined : undefined,
        sort: form.sort,
        status: form.status,
      })
      ElMessage.success('权限创建成功')
    } else {
      await updatePermission(form.id, {
        name: form.name.trim(),
        type: form.type,
        parentId: form.parentId,
        path: form.type === 'MENU' ? form.path.trim() || undefined : undefined,
        component: form.type === 'MENU' ? form.component.trim() || undefined : undefined,
        icon: form.type === 'MENU' ? form.icon.trim() || undefined : undefined,
        sort: form.sort,
        status: form.status,
      })
      ElMessage.success('权限已更新，相关用户需要重新登录')
    }
    dialogVisible.value = false
    await loadTree()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: PermissionRecord) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」吗？存在子节点或角色绑定时无法删除。`,
      '删除权限',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deletePermission(row.id)
    ElMessage.success('权限已删除')
    await loadTree()
  } catch {
    // 用户取消
  }
}

onMounted(loadTree)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">权限管理</h1>
        <p class="page-subtitle">维护菜单结构与按钮权限编码，权限编码与后端 @PreAuthorize 一一对应。</p>
      </div>
      <el-button v-permission="'system:permission:create'" type="primary" :icon="Plus" @click="openCreate()">
        新增权限
      </el-button>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-tag effect="plain" type="primary">菜单</el-tag>
          <span class="text-muted legend-text">可配置前端路由，参与菜单树</span>
          <el-tag effect="plain" type="info">按钮</el-tag>
          <span class="text-muted legend-text">只作为操作权限，不需要 path</span>
        </div>
        <div class="toolbar-right text-muted">共 {{ parentNameMap.size }} 个节点</div>
      </div>

      <div class="table-wrap">
        <el-table
          v-loading="loading"
          :data="tree"
          row-key="id"
          default-expand-all
          :tree-props="{ children: 'children' }"
        >
          <el-table-column label="名称" min-width="190">
            <template #default="{ row }">
              <div class="perm-name">
                <el-icon v-if="row.type === 'MENU'" class="perm-icon"><Key /></el-icon>
                <el-icon v-else class="perm-icon button"><Operation /></el-icon>
                <strong>{{ row.name }}</strong>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="权限编码" min-width="220">
            <template #default="{ row }">
              <code class="text-mono perm-code">{{ row.code }}</code>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.type === 'MENU' ? 'primary' : 'info'" effect="plain">
                {{ row.type === 'MENU' ? '菜单' : '按钮' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="路由地址" min-width="150">
            <template #default="{ row }">
              <code v-if="row.path" class="text-mono path-text">{{ row.path }}</code>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="父节点" min-width="110">
            <template #default="{ row }">
              {{ row.parentId === '0' ? '顶级' : parentNameMap.get(row.parentId) || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-tooltip v-if="row.type === 'MENU'" content="新增子项" placement="top">
                <el-button
                  v-permission="'system:permission:create'"
                  link
                  type="primary"
                  :icon="Plus"
                  aria-label="新增子项"
                  @click="openCreate(row as PermissionRecord)"
                />
              </el-tooltip>
              <el-tooltip content="编辑" placement="top">
                <el-button
                  v-permission="'system:permission:update'"
                  link
                  type="primary"
                  :icon="Edit"
                  aria-label="编辑"
                  @click="openEdit(row as PermissionRecord)"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button
                  v-permission="'system:permission:delete'"
                  link
                  type="danger"
                  :icon="Delete"
                  aria-label="删除"
                  @click="handleDelete(row as PermissionRecord)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无权限数据" :image-size="80" />
          </template>
        </el-table>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增权限' : '编辑权限'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px" @submit.prevent>
        <el-form-item label="权限类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button value="MENU">菜单</el-radio-button>
            <el-radio-button value="BUTTON">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限编码" prop="code">
          <el-input v-model="form.code" :disabled="dialogMode === 'edit'" placeholder="例如 system:user:create" />
        </el-form-item>
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="form.name" placeholder="例如 新增用户" />
        </el-form-item>
        <el-form-item label="父节点">
          <el-select v-model="form.parentId" style="width: 100%" filterable>
            <el-option
              v-for="option in parentOptions"
              :key="option.id"
              :label="`${'　'.repeat(option.level)}${option.name}`"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
        <template v-if="form.type === 'MENU'">
          <el-form-item label="路由地址">
            <el-input v-model="form.path" placeholder="例如 /system/user" />
          </el-form-item>
          <el-form-item label="组件路径">
            <el-input v-model="form.component" placeholder="例如 system/user/index" />
          </el-form-item>
          <el-form-item label="图标">
            <el-input v-model="form.icon" placeholder="Element Plus 图标名称，可选" />
          </el-form-item>
        </template>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
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
  </div>
</template>

<style scoped>
.legend-text {
  margin-right: 12px;
  font-size: 12px;
}

.perm-name {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 8px;
}

/* 权限树的展开/收起图标统一放到文字右侧 */
:deep(.el-table__body .el-table__row td:first-child .cell) {
  display: flex;
  align-items: center;
}

:deep(.el-table__body .el-table__row td:first-child .el-table__expand-icon) {
  order: 99;
  margin-right: 0;
  margin-left: 8px;
}

.perm-icon {
  color: #4f6bfb;
}

.perm-icon.button {
  color: #8b5cf6;
}

.perm-name strong {
  color: #263042;
  font-size: 13px;
  font-weight: 600;
}

.path-text {
  color: #4f6bfb;
  font-size: 12px;
  background: #f3f6ff;
  padding: 2px 6px;
  border-radius: 5px;
}

.perm-code {
  color: #4b5563;
  font-size: 11px;
  white-space: nowrap;
}
</style>
