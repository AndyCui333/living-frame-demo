<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Refresh, Search } from '@element-plus/icons-vue'
import { getLoginLogs } from '@/api/system/log'
import type { LoginLogRecord } from '@/types/api'

const rows = ref<LoginLogRecord[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  ip: '',
  status: '',
  loginType: '',
})

async function loadLogs() {
  loading.value = true
  try {
    const page = await getLoginLogs({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      username: query.username.trim() || undefined,
      ip: query.ip.trim() || undefined,
      status: query.status || undefined,
      loginType: query.loginType || undefined,
    })
    rows.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadLogs()
}

function handleReset() {
  query.username = ''
  query.ip = ''
  query.status = ''
  query.loginType = ''
  query.pageNum = 1
  loadLogs()
}

async function copyIp(ip?: string) {
  if (!ip) {
    return
  }
  try {
    await navigator.clipboard.writeText(ip)
    ElMessage.success('IP 已复制')
  } catch {
    ElMessage.warning('当前浏览器不支持复制')
  }
}

function statusTag(status: string): 'success' | 'danger' {
  return status === 'SUCCESS' ? 'success' : 'danger'
}

function statusText(status: string) {
  return status === 'SUCCESS' ? '成功' : '失败'
}

onMounted(loadLogs)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">登录日志</h1>
        <p class="page-subtitle">记录后台与业务端登录成功、失败、IP 和浏览器信息。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadLogs">刷新</el-button>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="query.username"
            placeholder="用户名"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="query.ip"
            placeholder="登录 IP"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="query.loginType" placeholder="全部端" clearable style="width: 120px" @change="handleSearch">
            <el-option label="后台" value="ADMIN" />
            <el-option label="业务端" value="BUSINESS" />
          </el-select>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
          </el-select>
          <el-button type="primary" plain :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </div>
        <div class="toolbar-right text-muted">共 {{ total }} 条记录</div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="rows" row-key="id">
          <el-table-column prop="loginAt" label="登录时间" width="170" />
          <el-table-column label="用户" min-width="150">
            <template #default="{ row }">
              <div class="user-line">
                <strong>{{ row.username }}</strong>
                <span v-if="row.userId" class="text-muted">ID {{ row.userId }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="端" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.loginType === 'ADMIN' ? 'primary' : 'success'" effect="plain">
                {{ row.loginType === 'ADMIN' ? '后台' : '业务端' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="登录 IP" width="150">
            <template #default="{ row }">
              <button v-if="row.ip" type="button" class="ip-copy" title="复制 IP" @click="copyIp(row.ip)">
                <code>{{ row.ip }}</code>
                <el-icon><CopyDocument /></el-icon>
              </button>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="86" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="statusTag(row.status)" effect="plain">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="说明" min-width="190">
            <template #default="{ row }">{{ row.message || '—' }}</template>
          </el-table-column>
          <el-table-column label="浏览器 / UA" min-width="220">
            <template #default="{ row }">
              <el-tooltip v-if="row.userAgent" :content="row.userAgent" placement="top">
                <span class="ua-text">{{ row.userAgent }}</span>
              </el-tooltip>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无登录日志" :image-size="80" />
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
          @current-change="loadLogs"
          @size-change="handleSearch"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-line {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-line strong {
  color: #263042;
  font-size: 13px;
}

.user-line span {
  font-size: 10px;
}

.ip-copy {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  color: #4f6bfb;
  background: transparent;
  cursor: pointer;
}

.ip-copy code {
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
}

.ip-copy .el-icon {
  opacity: 0;
  font-size: 11px;
  transition: opacity 0.15s ease;
}

.ip-copy:hover .el-icon {
  opacity: 1;
}

.ua-text {
  display: block;
  max-width: 240px;
  overflow: hidden;
  color: #6b7280;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
