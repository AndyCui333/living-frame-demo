<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Avatar,
  Briefcase,
  ChatDotRound,
  DataBoard,
  Key,
  MagicStick,
  Refresh,
  User,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getUsers } from '@/api/system/user'
import { getRoles } from '@/api/system/role'
import { getPermissionTree } from '@/api/system/permission'
import { getBusinessUsers } from '@/api/business/user'
import { getAiStatus } from '@/api/ai'
import type { AiStatus } from '@/types/api'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(true)
const ai = ref<AiStatus | null>(null)

const stats = reactive({
  users: 0,
  roles: 0,
  permissions: 0,
  businessUsers: 0,
})

const roleNames: Record<string, string> = {
  SUPER_ADMIN: '超级管理员',
  SYSTEM_ADMIN: '系统管理员',
  BUSINESS_ADMIN: '业务管理员',
  VIEWER: '只读用户',
  BIZ_USER: '业务用户',
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const today = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  }).format(new Date()),
)

const roleTags = computed(() =>
  (auth.user?.roles ?? []).map((role) => roleNames[role] ?? role),
)

const quickLinks = computed(() => {
  const links = []
  if (auth.hasPermission('system:user:list')) {
    links.push({ title: '用户管理', desc: '管理后台账号与角色绑定', path: '/system/user', icon: User, color: '#4f6bfb' })
  }
  if (auth.hasPermission('system:role:list')) {
    links.push({ title: '角色管理', desc: '配置角色与权限关系', path: '/system/role', icon: Avatar, color: '#8b5cf6' })
  }
  if (auth.hasPermission('business:user:list')) {
    links.push({ title: '业务用户', desc: '维护业务端账号', path: '/business/user', icon: Briefcase, color: '#0ea5e9' })
  }
  if (auth.hasPermission('ai:chat')) {
    links.push({ title: 'AI 对话', desc: '和 DeepSeek 助手对话', path: '/ai/chat', icon: ChatDotRound, color: '#10b981' })
  }
  if (auth.hasPermission('ai:agent')) {
    links.push({ title: 'AI Agent', desc: '让模型调用系统工具', path: '/ai/agent', icon: MagicStick, color: '#f59e0b' })
  }
  if (auth.hasPermission('system:permission:list')) {
    links.push({ title: '权限管理', desc: '维护菜单与按钮权限', path: '/system/permission', icon: Key, color: '#ef4444' })
  }
  return links
})

function countTree(nodes: Array<{ children?: unknown[] }>): number {
  return nodes.reduce((total, node) => total + 1 + countTree((node.children ?? []) as Array<{ children?: unknown[] }>), 0)
}

async function loadData() {
  loading.value = true
  const tasks: Array<Promise<void>> = []

  if (auth.hasPermission('system:user:list')) {
    tasks.push(
      getUsers({ pageNum: 1, pageSize: 1 }).then((page) => {
        stats.users = page.total
      }),
    )
  }
  if (auth.hasPermission('system:role:list')) {
    tasks.push(
      getRoles({ pageNum: 1, pageSize: 1 }).then((page) => {
        stats.roles = page.total
      }),
    )
  }
  if (auth.hasPermission('system:permission:list')) {
    tasks.push(
      getPermissionTree().then((tree) => {
        stats.permissions = countTree(tree)
      }),
    )
  }
  if (auth.hasPermission('business:user:list')) {
    tasks.push(
      getBusinessUsers({ pageNum: 1, pageSize: 1 }).then((page) => {
        stats.businessUsers = page.total
      }),
    )
  }

  await Promise.allSettled(tasks)
  try {
    ai.value = await getAiStatus()
  } catch {
    ai.value = null
  }
  loading.value = false
}

onMounted(loadData)
</script>

<template>
  <div class="page dashboard">
    <section class="welcome-card">
      <div class="welcome-copy">
        <span class="welcome-eyebrow">DASHBOARD</span>
        <h1>{{ greeting }}，{{ auth.displayName }}</h1>
        <p>{{ today }} · 欢迎回到 Living Frame 管理后台。</p>
        <div class="welcome-tags">
          <el-tag v-for="role in roleTags" :key="role" effect="dark" round>{{ role }}</el-tag>
          <el-tag v-if="ai" :type="ai.enabled ? 'success' : 'warning'" effect="plain" round>
            {{ ai.enabled ? `AI · ${ai.model}` : 'AI 未配置' }}
          </el-tag>
        </div>
      </div>
      <div class="welcome-visual">
        <div class="visual-ring ring-one"></div>
        <div class="visual-ring ring-two"></div>
        <div class="visual-icon">
          <el-icon :size="46"><DataBoard /></el-icon>
        </div>
      </div>
      <div class="welcome-actions">
        <el-button v-if="auth.hasPermission('ai:chat')" type="primary" @click="router.push('/ai/chat')">
          打开 AI 助手
          <el-icon class="ml-1"><ArrowRight /></el-icon>
        </el-button>
        <el-button text class="refresh-btn" :loading="loading" @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </section>

    <section class="stat-grid" v-loading="loading">
      <div class="stat-card">
        <div class="stat-icon icon-blue"><el-icon><User /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">后台用户</span>
          <strong class="stat-value">{{ stats.users }}</strong>
          <span class="stat-hint">已启用与禁用的账号总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-purple"><el-icon><Avatar /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">角色数量</span>
          <strong class="stat-value">{{ stats.roles }}</strong>
          <span class="stat-hint">用于组合不同权限</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-amber"><el-icon><Key /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">菜单 / 权限</span>
          <strong class="stat-value">{{ stats.permissions }}</strong>
          <span class="stat-hint">菜单节点与按钮权限</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-cyan"><el-icon><Briefcase /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">业务用户</span>
          <strong class="stat-value">{{ stats.businessUsers }}</strong>
          <span class="stat-hint">业务端独立账号体系</span>
        </div>
      </div>
    </section>

    <section class="bottom-grid">
      <div class="panel">
        <div class="panel-heading">
          <div>
            <h2>快捷入口</h2>
            <p>根据当前账号权限展示可用功能</p>
          </div>
        </div>
        <div v-if="quickLinks.length" class="quick-grid">
          <button
            v-for="link in quickLinks"
            :key="link.path"
            type="button"
            class="quick-item"
            @click="router.push(link.path)"
          >
            <span class="quick-icon" :style="{ color: link.color, background: `${link.color}14` }">
              <el-icon :size="20"><component :is="link.icon" /></el-icon>
            </span>
            <span class="quick-text">
              <strong>{{ link.title }}</strong>
              <span>{{ link.desc }}</span>
            </span>
            <el-icon class="quick-arrow"><ArrowRight /></el-icon>
          </button>
        </div>
        <el-empty v-else description="当前账号没有可用的管理菜单" :image-size="80" />
      </div>

      <div class="panel">
        <div class="panel-heading">
          <div>
            <h2>系统信息</h2>
            <p>当前登录与模型配置</p>
          </div>
        </div>
        <div class="info-list">
          <div class="info-row">
            <span>登录账号</span>
            <strong>{{ auth.user?.username }}</strong>
          </div>
          <div class="info-row">
            <span>用户类型</span>
            <strong>{{ auth.user?.userType === 'ADMIN' ? '后台用户' : '业务用户' }}</strong>
          </div>
          <div class="info-row">
            <span>权限模式</span>
            <strong>{{ auth.user?.superAdmin ? '超级管理员（全部权限）' : '角色权限' }}</strong>
          </div>
          <div class="info-row">
            <span>权限数量</span>
            <strong>{{ auth.user?.superAdmin ? '全部' : `${auth.user?.permissions.length ?? 0} 项` }}</strong>
          </div>
          <div class="info-row">
            <span>AI 模型</span>
            <strong>{{ ai?.model || '—' }}</strong>
          </div>
          <div class="info-row">
            <span>AI 状态</span>
            <el-tag :type="ai?.enabled ? 'success' : 'info'" size="small" effect="plain">
              {{ ai?.enabled ? '已连接' : '未启用' }}
            </el-tag>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  gap: 18px;
}

.welcome-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 210px;
  padding: 34px 38px;
  overflow: hidden;
  border-radius: 20px;
  color: #ffffff;
  background:
    radial-gradient(circle at 84% 20%, rgba(139, 92, 246, 0.55), transparent 30%),
    linear-gradient(120deg, #111827 0%, #18213a 58%, #1a2350 100%);
  box-shadow: 0 18px 40px rgba(17, 24, 39, 0.16);
}

.welcome-copy {
  position: relative;
  z-index: 2;
}

.welcome-eyebrow {
  color: #8ea0ff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2.2px;
}

.welcome-copy h1 {
  margin: 12px 0 0;
  font-size: 30px;
  letter-spacing: -0.8px;
}

.welcome-copy p {
  margin: 9px 0 0;
  color: #a6b1c6;
  font-size: 13px;
}

.welcome-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 20px;
}

.welcome-tags :deep(.el-tag) {
  border-color: rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.08);
  color: #dbe2f4;
}

.welcome-actions {
  position: absolute;
  right: 34px;
  bottom: 28px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
}

.refresh-btn {
  color: #aab5ca;
}

.ml-1 {
  margin-left: 4px;
}

.welcome-visual {
  position: absolute;
  top: -40px;
  right: 30px;
  width: 260px;
  height: 260px;
  opacity: 0.8;
}

.visual-ring {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 50%;
}

.ring-one {
  inset: 16px;
}

.ring-two {
  inset: 52px;
}

.visual-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  display: grid;
  place-items: center;
  width: 104px;
  height: 104px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 28px;
  color: #cfd8ff;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
  transform: translate(-50%, -50%) rotate(-8deg);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  border: 1px solid var(--lf-border);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: var(--lf-shadow);
}

.stat-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border-radius: 12px;
  font-size: 19px;
}

.icon-blue {
  color: #4f6bfb;
  background: #eef2ff;
}

.icon-purple {
  color: #8b5cf6;
  background: #f5f0ff;
}

.icon-amber {
  color: #d97706;
  background: #fff7e8;
}

.icon-cyan {
  color: #0891b2;
  background: #eafaff;
}

.stat-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-label {
  color: #7b8497;
  font-size: 12px;
}

.stat-value {
  margin-top: 2px;
  color: #111827;
  font-size: 28px;
  line-height: 1.2;
  letter-spacing: -1px;
}

.stat-hint {
  margin-top: 3px;
  color: #a1a9b8;
  font-size: 11px;
}

.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.75fr);
  gap: 16px;
}

.panel {
  padding: 22px;
  border: 1px solid var(--lf-border);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: var(--lf-shadow);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-heading h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
  letter-spacing: -0.2px;
}

.panel-heading p {
  margin: 5px 0 0;
  color: #9aa3b5;
  font-size: 12px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px;
  border: 1px solid #eef1f7;
  border-radius: 12px;
  background: #fbfcff;
  text-align: left;
  cursor: pointer;
  transition: all 0.16s ease;
}

.quick-item:hover {
  border-color: #dfe5fe;
  background: #f7f9ff;
  transform: translateY(-1px);
}

.quick-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 10px;
}

.quick-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.quick-text strong {
  color: #263042;
  font-size: 12px;
}

.quick-text span {
  margin-top: 2px;
  overflow: hidden;
  color: #9aa3b5;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-arrow {
  margin-left: auto;
  color: #c2c9d6;
}

.info-list {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px dashed #eef1f7;
  font-size: 12px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row span {
  color: #8d96a8;
}

.info-row strong {
  color: #263042;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .welcome-card {
    padding: 24px;
  }

  .welcome-visual {
    display: none;
  }

  .welcome-copy h1 {
    font-size: 24px;
  }

  .stat-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
