<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh, Trophy } from '@element-plus/icons-vue'
import { getCsRanking } from '@/api/customerService'
import type { CsRanking } from '@/types/api'

const loading = ref(false)
const rows = ref<CsRanking[]>([])

const totalProcessed = computed(() => rows.value.reduce((total, item) => total + (item.processedCount || 0), 0))
const ratedAgents = computed(() => rows.value.filter((item) => item.ratingCount > 0))
const averageRating = computed(() => {
  if (!ratedAgents.value.length) return '0.00'
  const total = ratedAgents.value.reduce((sum, item) => sum + Number(item.averageRating || 0), 0)
  return (total / ratedAgents.value.length).toFixed(2)
})

const summary = computed(() => [
  { label: '参与排名', value: rows.value.length, unit: '位客服' },
  { label: '累计处理', value: totalProcessed.value, unit: '个工单' },
  { label: '平均评分', value: averageRating.value, unit: '分' },
])

async function loadRanking() {
  loading.value = true
  try {
    rows.value = await getCsRanking()
  } finally {
    loading.value = false
  }
}

function typeLabel(type: string) {
  return type === 'TECH' ? '技术客服' : '在线聊天'
}

function ratingSortMethod(a: CsRanking, b: CsRanking) {
  return Number(a.averageRating || 0) - Number(b.averageRating || 0)
}

onMounted(loadRanking)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">客服排行</h1>
        <p class="page-subtitle">按已归档工单处理数量和平均评分综合展示客服服务表现。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadRanking">刷新排行</el-button>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="排名规则：按已归档处理数量排序，处理数量相同则按平均评分排序"
      class="ranking-tip"
    />

    <div class="ranking-summary">
      <div v-for="item in summary" :key="item.label" class="summary-card card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
      </div>
    </div>

    <section class="card ranking-card">
      <div class="ranking-card-head">
        <div>
          <strong>排行榜</strong>
          <span>共 {{ rows.length }} 位客服参与排名</span>
        </div>
        <el-icon class="ranking-head-icon"><Trophy /></el-icon>
      </div>
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="agentUserId"
        class="ranking-table"
        :header-cell-style="{ textAlign: 'center', whiteSpace: 'nowrap' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column label="排名" width="86" align="center" header-align="center">
          <template #default="{ row }">
            <span class="rank-badge" :class="`rank-${row.rank}`">{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="客服" min-width="180" align="center" header-align="center" />
        <el-table-column label="类型" width="130" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.serviceType === 'TECH' ? 'success' : 'primary'">
              {{ typeLabel(row.serviceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="processedCount"
          label="处理数量"
          width="132"
          align="center"
          header-align="center"
          sortable
        />
        <el-table-column
          label="平均评分"
          width="132"
          align="center"
          header-align="center"
          sortable
          :sort-method="ratingSortMethod"
        >
          <template #default="{ row }">
            <span v-if="row.ratingCount > 0" class="rating-value">{{ row.averageRating }} 分</span>
            <span v-else class="rating-empty">暂无评分</span>
          </template>
        </el-table-column>
        <el-table-column prop="ratingCount" label="评价数" width="110" align="center" header-align="center" />
        <template #empty>
          <el-empty description="暂无排名数据" :image-size="80" />
        </template>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.ranking-tip {
  border-radius: 12px;
}

.ranking-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 20px;
}

.summary-card span {
  color: var(--lf-text-secondary);
  font-size: 13px;
}

.summary-card strong {
  color: #172033;
  font-size: 28px;
  font-weight: 750;
  letter-spacing: -0.04em;
  line-height: 1;
}

.summary-card small {
  margin-left: 5px;
  color: #8b95a7;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0;
}

.ranking-card {
  overflow: hidden;
}

.ranking-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--lf-border);
}

.ranking-card-head > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ranking-card-head strong {
  color: #263042;
  font-size: 15px;
}

.ranking-card-head span {
  color: #9aa3b5;
  font-size: 12px;
}

.ranking-head-icon {
  color: #c7a84a;
  font-size: 24px;
}

.ranking-table {
  width: 100%;
}

.ranking-table :deep(.el-table__header .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  white-space: nowrap;
}

.ranking-table :deep(.el-table__header .caret-wrapper) {
  display: inline-flex;
  align-items: center;
  height: 20px;
}

.ranking-table :deep(.el-table__cell) {
  padding: 11px 0;
}

.rank-badge {
  display: inline-grid;
  place-items: center;
  width: 27px;
  height: 27px;
  border-radius: 8px;
  color: #6b7280;
  background: #f2f4f8;
  font-size: 12px;
  font-weight: 700;
}

.rank-badge.rank-1 {
  color: #8a5a00;
  background: #fff1c7;
}

.rank-badge.rank-2 {
  color: #5e6878;
  background: #edf0f5;
}

.rank-badge.rank-3 {
  color: #8a5b35;
  background: #f8e5d7;
}

.rating-value {
  color: #d49a16;
  font-weight: 650;
}

.rating-empty {
  color: #a9b1bf;
  font-size: 12px;
}

@media (max-width: 760px) {
  .ranking-summary {
    grid-template-columns: 1fr;
  }

  .page-heading {
    flex-direction: column;
  }
}
</style>
