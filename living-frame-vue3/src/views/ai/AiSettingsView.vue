<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Key, MagicStick, Plus, Refresh, Setting } from '@element-plus/icons-vue'
import {
  createAiConfig,
  deleteAiConfig,
  getAiConfigs,
  getAiStatus,
  setDefaultAiConfig,
  updateAiConfig,
} from '@/api/ai'
import type { AiStatus, AiUserConfig, AiUserConfigPayload } from '@/types/api'

const loading = ref(false)
const saving = ref(false)
const configs = ref<AiUserConfig[]>([])
const status = ref<AiStatus | null>(null)

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)

const providerOptions = [
  { value: 'DEEPSEEK', label: 'DeepSeek', baseUrl: 'https://api.deepseek.com', model: 'deepseek-flash' },
  { value: 'QWEN', label: '通义千问', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode', model: 'qwen3.8-max' },
  { value: 'OPENAI', label: 'OpenAI', baseUrl: 'https://api.openai.com', model: 'gpt-4o' },
  { value: 'CUSTOM', label: '自定义（OpenAI 兼容）', baseUrl: '', model: '' },
]

const reasoningOptions = [
  { value: 'NONE', label: '关闭' },
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '中' },
  { value: 'HIGH', label: '高' },
  { value: 'MAX', label: '最高' },
]

const aiForm = ref({
  provider: 'DEEPSEEK',
  providerName: 'DeepSeek',
  baseUrl: 'https://api.deepseek.com',
  model: 'deepseek-flash',
  apiKey: '',
  clearApiKey: false,
  defaultConfig: true,
  reasoningLevel: 'HIGH',
  temperature: 0.7,
  maxTokens: 4096,
})

const activeConfig = computed(() => configs.value.find((config) => config.isDefault))
const editingConfig = computed(() => configs.value.find((config) => config.id === editingId.value))

async function loadData() {
  loading.value = true
  try {
    const [list, current] = await Promise.all([getAiConfigs(), getAiStatus()])
    configs.value = list
    status.value = current
  } finally {
    loading.value = false
  }
}

function onProviderChange(provider: string) {
  const option = providerOptions.find((item) => item.value === provider)
  if (!option) {
    return
  }
  aiForm.value.providerName = option.label.replace('（OpenAI 兼容）', '')
  aiForm.value.baseUrl = option.baseUrl
  aiForm.value.model = option.model
}

function openCreate() {
  editingId.value = null
  aiForm.value.provider = 'DEEPSEEK'
  aiForm.value.providerName = 'DeepSeek'
  aiForm.value.baseUrl = 'https://api.deepseek.com'
  aiForm.value.model = 'deepseek-flash'
  aiForm.value.apiKey = ''
  aiForm.value.clearApiKey = false
  aiForm.value.defaultConfig = configs.value.length === 0
  aiForm.value.reasoningLevel = 'HIGH'
  aiForm.value.temperature = 0.7
  aiForm.value.maxTokens = 4096
  dialogVisible.value = true
}

function openEdit(config: AiUserConfig) {
  editingId.value = config.id
  aiForm.value.provider = config.provider
  aiForm.value.providerName = config.providerName
  aiForm.value.baseUrl = config.baseUrl
  aiForm.value.model = config.model
  aiForm.value.apiKey = ''
  aiForm.value.clearApiKey = false
  aiForm.value.defaultConfig = config.isDefault
  aiForm.value.reasoningLevel = config.reasoningLevel || 'HIGH'
  aiForm.value.temperature = Number(config.temperature ?? 0.7)
  aiForm.value.maxTokens = config.maxTokens ?? 4096
  dialogVisible.value = true
}

async function submit() {
  if (!aiForm.value.providerName.trim() || !aiForm.value.baseUrl.trim() || !aiForm.value.model.trim()) {
    ElMessage.warning('请填写厂商名称、Base URL 和模型')
    return
  }
  if (!/^https?:\/\/.+/.test(aiForm.value.baseUrl.trim())) {
    ElMessage.warning('Base URL 必须以 http:// 或 https:// 开头')
    return
  }
  if (!editingId.value && !aiForm.value.apiKey.trim()) {
    ElMessage.warning('新增模型时请填写 API Key')
    return
  }
  const payload: AiUserConfigPayload = {
    provider: aiForm.value.provider,
    providerName: aiForm.value.providerName.trim(),
    baseUrl: aiForm.value.baseUrl.trim(),
    model: aiForm.value.model.trim(),
    apiKey: aiForm.value.apiKey.trim() || undefined,
    clearApiKey: aiForm.value.clearApiKey || undefined,
    defaultConfig: aiForm.value.defaultConfig,
    reasoningLevel: aiForm.value.reasoningLevel,
    temperature: aiForm.value.temperature,
    maxTokens: aiForm.value.maxTokens,
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateAiConfig(editingId.value, payload)
      ElMessage.success('模型配置已更新')
    } else {
      await createAiConfig(payload)
      ElMessage.success('模型配置已添加')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function makeDefault(config: AiUserConfig) {
  await setDefaultAiConfig(config.id)
  ElMessage.success('默认模型已切换')
  await loadData()
}

async function removeConfig(config: AiUserConfig) {
  try {
    await ElMessageBox.confirm(`确定删除模型「${config.providerName} · ${config.model}」吗？`, '删除模型配置', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteAiConfig(config.id)
    ElMessage.success('模型配置已删除')
    await loadData()
  } catch {
    // 用户取消
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">AI 设置</h1>
        <p class="page-subtitle">每个账号独立维护模型和 API Key；Key 在后端加密保存，前端只显示配置状态。</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">添加模型</el-button>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="API Key 加密存储"
      description="密钥使用 AES-GCM 加密后保存到数据库，接口不会返回 Key 明文或密文；编辑时留空表示不修改。"
      class="security-alert"
    />

    <section v-if="status?.enabled" class="current-card">
      <div class="current-icon"><el-icon :size="22"><MagicStick /></el-icon></div>
      <div class="current-copy">
        <span class="current-label">当前默认模型</span>
        <strong>{{ status.providerName }} · {{ status.model }}</strong>
        <span class="current-meta">Key 已配置，可直接使用 AI 对话和 Agent</span>
      </div>
      <el-tag type="success" effect="plain" round>已启用</el-tag>
      <el-button type="primary" plain @click="$router.push('/ai/chat')">去对话</el-button>
    </section>

    <section v-else class="current-card current-empty">
      <div class="current-icon"><el-icon :size="22"><MagicStick /></el-icon></div>
      <div class="current-copy">
        <span class="current-label">当前默认模型</span>
        <strong>{{ activeConfig ? `${activeConfig.providerName} · ${activeConfig.model}` : '尚未配置模型' }}</strong>
        <span class="current-meta">{{ activeConfig ? '请填写该模型的 API Key 后才能使用' : '请添加模型并填写 API Key' }}</span>
      </div>
      <el-tag type="warning" effect="plain" round>待配置 Key</el-tag>
    </section>

    <section class="settings-card">
      <div class="card-head">
        <span class="card-icon"><el-icon><Key /></el-icon></span>
        <div>
          <h2>我的模型列表</h2>
          <p>新账号默认生成 DeepSeek、通义千问、OpenAI 三条配置，可删除或新增。</p>
        </div>
      </div>

      <el-table v-loading="loading" :data="configs" row-key="id">
        <el-table-column label="厂商" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.providerName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模型" min-width="180">
          <template #default="{ row }">
            <div class="model-cell">
              <strong>{{ row.model }}</strong>
              <span>{{ row.baseUrl }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="API Key" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.hasApiKey ? 'success' : 'warning'" size="small" effect="plain">
              {{ row.hasApiKey ? '已加密配置' : '未配置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" size="small" type="success" effect="plain">默认</el-tag>
            <el-button v-else link type="primary" @click="makeDefault(row as AiUserConfig)">设为默认</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" aria-label="编辑" @click="openEdit(row as AiUserConfig)" />
            <el-button link type="danger" :icon="Delete" aria-label="删除" @click="removeConfig(row as AiUserConfig)" />
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有模型配置，请添加模型" :image-size="70" />
        </template>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模型配置' : '添加模型配置'" width="580px" destroy-on-close>
      <el-form label-position="top" @submit.prevent>
        <div class="form-row">
          <el-form-item label="厂商类型">
            <el-select v-model="aiForm.provider" style="width: 220px" @change="onProviderChange">
              <el-option v-for="option in providerOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="厂商名称">
            <el-input v-model="aiForm.providerName" placeholder="如 DeepSeek / 公司模型网关" />
          </el-form-item>
        </div>
        <el-form-item label="Base URL">
          <el-input v-model="aiForm.baseUrl" placeholder="https://api.deepseek.com" />
        </el-form-item>
        <el-form-item label="模型名称 / ID">
          <el-input v-model="aiForm.model" placeholder="例如 deepseek-flash、qwen3.8-max、gpt-4o" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input
            v-model="aiForm.apiKey"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="editingConfig?.hasApiKey ? '已配置加密 Key，留空不修改' : '请输入该模型对应的 API Key'"
          />
          <span class="field-hint">Key 会加密保存，前端不会展示明文；编辑时留空表示保留原 Key。</span>
        </el-form-item>
        <el-form-item v-if="editingId">
          <el-checkbox v-model="aiForm.clearApiKey">清除已保存的 API Key</el-checkbox>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="推理等级">
            <el-select v-model="aiForm.reasoningLevel" style="width: 150px">
              <el-option v-for="level in reasoningOptions" :key="level.value" :label="level.label" :value="level.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="最大 Token">
            <el-input-number v-model="aiForm.maxTokens" :min="256" :max="32768" :step="256" />
          </el-form-item>
        </div>
        <el-form-item label="温度">
          <el-slider v-model="aiForm.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="默认模型">
          <el-switch v-model="aiForm.defaultConfig" active-text="保存后设为当前默认" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.heading-actions {
  display: flex;
  gap: 8px;
}

.security-alert {
  margin-bottom: 16px;
}

.current-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 22px;
  margin-bottom: 16px;
  border: 1px solid #e3e8ff;
  border-radius: 16px;
  background: linear-gradient(120deg, #f7f9ff, #ffffff 58%, #f5f1ff);
  box-shadow: var(--lf-shadow);
}

.current-empty {
  border-style: dashed;
  background: #fbfcff;
}

.current-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  border-radius: 13px;
  color: #5b73ff;
  background: #eef2ff;
}

.current-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.current-label {
  color: #8d96a8;
  font-size: 11px;
}

.current-copy strong {
  margin-top: 2px;
  color: #1f2937;
  font-size: 16px;
}

.current-meta {
  margin-top: 3px;
  color: #9aa3b5;
  font-size: 11px;
}

.settings-card {
  padding: 22px;
  border: 1px solid var(--lf-border);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: var(--lf-shadow);
}

.card-head {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  margin-bottom: 18px;
}

.card-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 10px;
  color: #4f6bfb;
  background: #eef2ff;
  font-size: 17px;
}

.card-head h2 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.card-head p {
  margin: 4px 0 0;
  color: #9aa3b5;
  font-size: 11px;
}

.model-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.model-cell strong {
  color: #263042;
  font-size: 12px;
}

.model-cell span {
  overflow: hidden;
  color: #9aa3b5;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-hint {
  display: block;
  margin-top: 4px;
  color: #9aa3b5;
  font-size: 10px;
}

.form-row {
  display: flex;
  gap: 16px;
}

@media (max-width: 760px) {
  .current-card {
    flex-wrap: wrap;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
