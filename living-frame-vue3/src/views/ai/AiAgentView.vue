<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Cpu, MagicStick, Refresh, Setting, User, VideoPlay } from '@element-plus/icons-vue'
import { getAiStatus, runAgent } from '@/api/ai'
import type { AiStatus } from '@/types/api'

const router = useRouter()
const status = ref<AiStatus | null>(null)
const task = ref('')
const result = ref('')
const loading = ref(false)

const examples = [
  '统计后台用户数量，并给出简要结论',
  '列出系统所有角色编码，说明它们可能的职责',
  '同时统计后台用户和业务用户数量',
]

const tools = [
  { name: 'countAdminUsers', label: '统计后台用户', desc: '读取 sys_user 表数量', icon: User, color: '#4f6bfb' },
  { name: 'listRoleCodes', label: '列出角色编码', desc: '读取角色 code 列表', icon: MagicStick, color: '#8b5cf6' },
  { name: 'countBusinessUsers', label: '统计业务用户', desc: '读取 biz_user 表数量', icon: Cpu, color: '#0ea5e9' },
]

async function loadStatus() {
  try {
    status.value = await getAiStatus()
  } catch {
    status.value = null
  }
}

function useExample(text: string) {
  task.value = text
}

async function execute() {
  const text = task.value.trim()
  if (!text || loading.value || status.value?.enabled === false) {
    return
  }
  loading.value = true
  result.value = ''
  try {
    const response = await runAgent({ task: text })
    result.value = response.reply
    ElMessage.success('Agent 执行完成')
  } catch {
    result.value = '执行失败，请检查 AI 配置或稍后重试。'
  } finally {
    loading.value = false
  }
}

onMounted(loadStatus)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">AI Agent</h1>
        <p class="page-subtitle">让 AI 调用系统工具，完成统计与信息查询任务。</p>
      </div>
      <div class="agent-head-actions">
        <el-tag v-if="status" :type="status.enabled ? 'success' : 'warning'" effect="plain" round>
          {{ status.model ? `${status.providerName} · ${status.model}` : '未选择模型' }}
        </el-tag>
        <el-button :icon="Setting" @click="router.push('/ai/settings')">模型设置</el-button>
      </div>
    </div>

    <el-alert
      v-if="status && !status.model"
      type="warning"
      :closable="false"
      show-icon
      title="尚未选择模型"
      description="当前账号没有默认模型，请到「AI 设置」选择厂商和模型并保存。"
    />
    <el-alert
      v-else-if="status && !status.enabled"
      type="warning"
      :closable="false"
      show-icon
      title="当前模型未配置"
      description="请到「AI 设置」中填写对应厂商的 API Key，或切换其他已配置的模型。"
    />

    <div class="agent-grid">
      <section class="agent-main">
        <div class="agent-card">
          <div class="agent-card-head">
            <span class="agent-icon"><el-icon><Cpu /></el-icon></span>
            <div>
              <h2>任务描述</h2>
              <p>描述你想让 Agent 完成的事情，它会自动选择工具。</p>
            </div>
          </div>
          <el-input
            v-model="task"
            type="textarea"
            :rows="5"
            resize="none"
            maxlength="4000"
            show-word-limit
            placeholder="例如：统计后台用户数量，并给出简要结论"
            :disabled="loading || status?.enabled === false"
          />
          <div class="agent-actions">
            <el-button
              type="primary"
              :icon="VideoPlay"
              :loading="loading"
              :disabled="!task.trim() || status?.enabled === false"
              @click="execute"
            >
              执行 Agent
            </el-button>
            <el-button :icon="Refresh" @click="task = ''; result = ''">重置</el-button>
          </div>
          <div class="example-list">
            <span class="example-label">示例任务</span>
            <button v-for="item in examples" :key="item" type="button" class="example-item" @click="useExample(item)">
              {{ item }}
            </button>
          </div>
        </div>

        <div class="agent-card result-card">
          <div class="agent-card-head">
            <span class="agent-icon result"><el-icon><MagicStick /></el-icon></span>
            <div>
              <h2>执行结果</h2>
              <p>Agent 会先调用工具，再汇总结果。</p>
            </div>
          </div>
          <div v-if="loading" class="result-loading">
            <span class="pulse"></span>
            <span class="pulse"></span>
            <span class="pulse"></span>
            <span>正在调用模型与工具...</span>
          </div>
          <div v-else-if="result" class="result-content">{{ result }}</div>
          <el-empty v-else description="还没有执行结果" :image-size="80" />
        </div>
      </section>

      <aside class="agent-side">
        <div class="side-card">
          <h3>可用工具</h3>
          <p>Agent 只能调用以下只读工具</p>
          <div class="tool-list">
            <div v-for="tool in tools" :key="tool.name" class="tool-item">
              <span class="tool-icon" :style="{ color: tool.color, background: `${tool.color}14` }">
                <el-icon><component :is="tool.icon" /></el-icon>
              </span>
              <div>
                <strong>{{ tool.label }}</strong>
                <span>{{ tool.desc }}</span>
                <code>{{ tool.name }}</code>
              </div>
            </div>
          </div>
        </div>

        <div class="side-card hint-card">
          <h3>使用提示</h3>
          <ul>
            <li>任务描述越具体，Agent 结果越稳定。</li>
            <li>工具调用由模型自动决定，可能多次调用。</li>
            <li>Agent 只能读取统计信息，不会修改数据。</li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.agent-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.7fr);
  gap: 16px;
}

.agent-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.agent-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.agent-card {
  padding: 22px;
  border: 1px solid var(--lf-border);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: var(--lf-shadow);
}

.agent-card-head {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  margin-bottom: 16px;
}

.agent-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  border-radius: 11px;
  color: #4f6bfb;
  background: #eef2ff;
  font-size: 18px;
}

.agent-icon.result {
  color: #8b5cf6;
  background: #f5f0ff;
}

.agent-card-head h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.agent-card-head p {
  margin: 4px 0 0;
  color: #9aa3b5;
  font-size: 12px;
}

.agent-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.example-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed #eef1f7;
}

.example-label {
  color: #a1a9b8;
  font-size: 11px;
}

.example-item {
  padding: 6px 10px;
  border: 1px solid #e8ebf3;
  border-radius: 999px;
  color: #5d6678;
  background: #fbfcff;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.example-item:hover {
  border-color: #ced7fe;
  color: #3d55e8;
  background: #f5f7ff;
}

.result-card {
  min-height: 260px;
}

.result-content {
  padding: 16px;
  border: 1px solid #eef1f7;
  border-radius: 12px;
  color: #313b4d;
  background: #fbfcff;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-loading {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 20px 4px;
  color: #8d96a8;
  font-size: 12px;
}

.pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8ea0ff;
  animation: pulse 1s infinite ease-in-out;
}

.pulse:nth-child(2) {
  animation-delay: 0.15s;
}

.pulse:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.agent-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-card {
  padding: 20px;
  border: 1px solid var(--lf-border);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: var(--lf-shadow);
}

.side-card h3 {
  margin: 0;
  color: #111827;
  font-size: 14px;
}

.side-card > p {
  margin: 5px 0 14px;
  color: #9aa3b5;
  font-size: 11px;
}

.tool-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tool-item {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.tool-icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  border-radius: 9px;
  font-size: 15px;
}

.tool-item strong {
  display: block;
  color: #313b4d;
  font-size: 12px;
}

.tool-item span {
  display: block;
  margin-top: 1px;
  color: #9aa3b5;
  font-size: 10px;
}

.tool-item code {
  display: block;
  margin-top: 2px;
  color: #b0b7c4;
  font-size: 9px;
}

.hint-card ul {
  margin: 0;
  padding-left: 16px;
  color: #7b8497;
  font-size: 11px;
  line-height: 1.9;
}

@media (max-width: 960px) {
  .agent-grid {
    grid-template-columns: 1fr;
  }
}
</style>
