<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getOperationLogs } from '@/api/system/log'
import type { OperationLogRecord } from '@/types/api'

const rows = ref<OperationLogRecord[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  operation: '',
  status: '',
})

async function loadLogs() {
  loading.value = true
  try {
    const page = await getOperationLogs({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      username: query.username.trim() || undefined,
      operation: query.operation.trim() || undefined,
      status: query.status || undefined,
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
  query.operation = ''
  query.status = ''
  query.pageNum = 1
  loadLogs()
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
        <h1 class="page-title">操作日志</h1>
        <p class="page-subtitle">记录管理端写操作、操作人、请求地址、耗时与执行结果。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadLogs">刷新</el-button>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="query.username"
            placeholder="操作人"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="query.operation"
            placeholder="操作描述"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
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
          <el-table-column prop="createdAt" label="操作时间" width="170" />
          <el-table-column prop="operation" label="操作" min-width="180" />
          <el-table-column label="操作人" width="120">
            <template #default="{ row }">
              <span>{{ row.username || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="方法" width="82" align="center">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :type="row.method === 'DELETE' ? 'danger' : 'primary'">
                {{ row.method || '—' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="请求地址" min-width="230">
            <template #default="{ row }">
              <code class="uri-text">{{ row.requestUri || '—' }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="ip" label="操作 IP" width="135" />
          <el-table-column label="状态" width="86" align="center">
            <template #default="{ row }">
              <el-tooltip v-if="row.status === 'FAIL' && row.errorMessage" :content="row.errorMessage" placement="top">
                <el-tag size="small" type="danger" effect="plain">失败</el-tag>
              </el-tooltip>
              <el-tag v-else size="small" :type="statusTag(row.status)" effect="plain">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="86" align="right">
            <template #default="{ row }">{{ row.durationMs ?? 0 }} ms</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无操作日志" :image-size="80" />
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
.uri-text {
  display: block;
  max-width: 250px;
  overflow: hidden;
  color: #4f6bfb;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-text {
  display: block;
  max-width: 220px;
  overflow: hidden;
  color: #b42318;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
