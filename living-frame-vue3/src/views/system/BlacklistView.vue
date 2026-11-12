<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createBlacklist,
  deleteBlacklist,
  getBlacklist,
  updateBlacklistStatus,
} from '@/api/system/blacklist'
import type { BlacklistRecord } from '@/types/api'

const rows = ref<BlacklistRecord[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  targetType: '',
  targetValue: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  targetType: 'IP' as 'IP' | 'USERNAME',
  targetValue: '',
  reason: '',
  status: 1,
})

const rules: FormRules = {
  targetType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  targetValue: [{ required: true, message: '请输入 IP 或用户名', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写加入黑名单的完整原因', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const page = await getBlacklist({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      targetType: query.targetType || undefined,
      targetValue: query.targetValue.trim() || undefined,
      status: query.status,
    })
    rows.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.targetType = ''
  query.targetValue = ''
  query.status = undefined
  query.pageNum = 1
  loadList()
}

function openCreate() {
  form.targetType = 'IP'
  form.targetValue = ''
  form.reason = ''
  form.status = 1
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    await createBlacklist({
      targetType: form.targetType,
      targetValue: form.targetValue.trim(),
      reason: form.reason.trim(),
      status: form.status,
    })
    ElMessage.success('已加入系统黑名单（永久生效）')
    dialogVisible.value = false
    await loadList()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: BlacklistRecord, value: string | number | boolean) {
  const status = value ? 1 : 0
  try {
    await updateBlacklistStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 1 ? '系统黑名单已启用' : '系统黑名单已禁用')
  } catch {
    // 保持原状态
  }
}

async function handleDelete(row: BlacklistRecord) {
  try {
    await ElMessageBox.confirm(
      `确定移除系统黑名单「${row.targetValue}」吗？移除后该目标可重新尝试登录。`,
      '移出系统黑名单',
      { type: 'warning', confirmButtonText: '移出', cancelButtonText: '取消' },
    )
    await deleteBlacklist(row.id)
    ElMessage.success('已移出系统黑名单')
    await loadList()
  } catch {
    // 用户取消
  }
}

onMounted(loadList)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">系统黑名单</h1>
        <p class="page-subtitle">
          所有黑名单均为永久生效并存储在数据库中；登录失败 10 次会自动把来源 IP 加入系统黑名单。
        </p>
      </div>
      <el-button v-permission="'system:blacklist:create'" type="primary" :icon="Plus" @click="openCreate">
        新增黑名单
      </el-button>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="query.targetType" placeholder="全部类型" clearable style="width: 130px" @change="handleSearch">
            <el-option label="IP" value="IP" />
            <el-option label="用户名" value="USERNAME" />
          </el-select>
          <el-input
            v-model="query.targetValue"
            placeholder="IP / 用户名"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="生效" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" plain :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </div>
        <div class="toolbar-right text-muted">共 {{ total }} 条记录</div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="rows" row-key="id">
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.targetType === 'IP' ? 'warning' : 'primary'" effect="plain">
                {{ row.targetType === 'IP' ? 'IP' : '用户名' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="目标" min-width="180">
            <template #default="{ row }">
              <code class="target-text">{{ row.targetValue }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="加入原因" min-width="360">
            <template #default="{ row }">{{ row.reason || '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                @change="(value) => toggleStatus(row as BlacklistRecord, value)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createdBy" label="创建人" width="120">
            <template #default="{ row }">{{ row.createdBy || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="移出系统黑名单" placement="top">
                <el-button
                  v-permission="'system:blacklist:delete'"
                  link
                  type="danger"
                  :icon="Delete"
                  aria-label="移出系统黑名单"
                  @click="handleDelete(row as BlacklistRecord)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="系统黑名单为空" :image-size="80" />
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
          @current-change="loadList"
          @size-change="handleSearch"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新增系统黑名单" width="540px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="82px" @submit.prevent>
        <el-form-item label="类型" prop="targetType">
          <el-radio-group v-model="form.targetType">
            <el-radio-button value="IP">IP</el-radio-button>
            <el-radio-button value="USERNAME">用户名</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="目标" prop="targetValue">
          <el-input
            v-model="form.targetValue"
            :placeholder="form.targetType === 'IP' ? '例如 192.168.1.10' : '例如 test_user'"
          />
        </el-form-item>
        <el-form-item label="加入原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请写清楚为什么将该 IP 或账号加入系统黑名单，例如：多次暴力破解、异常登录、安全审计要求等"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="系统黑名单为永久生效，需手动禁用或移出。"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确认加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.target-text {
  color: #263042;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
}
</style>
