<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh, TrendCharts, User, UserFilled, Warning } from '@element-plus/icons-vue'
import { getBusinessUserStatistics } from '@/api/business/user'
import type { BusinessUserStatistics } from '@/types/api'

const loading = ref(false)
const stats = ref<BusinessUserStatistics | null>(null)

const maxTrend = computed(() => Math.max(1, ...(stats.value?.trend.map((item) => item.count) ?? [0])))
const totalForPie = computed(() => (stats.value?.statusDistribution ?? []).reduce((sum, item) => sum + item.value, 0))
const pieColors = ['#4f6bfb', '#f59e0b']
const pieBackground = computed(() => {
  const slices = stats.value?.statusDistribution ?? []
  const total = totalForPie.value
  if (!total) return '#eef2f8'
  let cursor = 0
  const parts: string[] = []
  slices.forEach((slice, index) => {
    const start = (cursor / total) * 360
    cursor += slice.value
    const end = (cursor / total) * 360
    parts.push(`${pieColors[index % pieColors.length]} ${start}deg ${end}deg`)
  })
  return `conic-gradient(${parts.join(', ')})`
})

async function loadData() {
  loading.value = true
  try {
    stats.value = await getBusinessUserStatistics()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page statistics-page" v-loading="loading">
    <div class="page-heading">
      <div>
        <h1 class="page-title">用户统计</h1>
        <p class="page-subtitle">门户用户规模、活跃情况、状态分布与近 7 日新增趋势。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <section class="summary-grid">
      <div class="summary-card">
        <span class="summary-icon blue"><el-icon><UserFilled /></el-icon></span>
        <div>
          <span>门户用户总数</span>
          <strong>{{ stats?.totalUsers ?? 0 }}</strong>
          <small>启用 {{ stats?.enabledUsers ?? 0 }} 人</small>
        </div>
      </div>
      <div class="summary-card">
        <span class="summary-icon green"><el-icon><User /></el-icon></span>
        <div>
          <span>当前在线</span>
          <strong>{{ stats?.onlineUsers ?? 0 }}</strong>
          <small>近 5 分钟有活跃心跳</small>
        </div>
      </div>
      <div class="summary-card">
        <span class="summary-icon purple"><el-icon><TrendCharts /></el-icon></span>
        <div>
          <span>今日新增</span>
          <strong>{{ stats?.newUsersToday ?? 0 }}</strong>
          <small>近 7 日新增 {{ stats?.newUsersSevenDays ?? 0 }}</small>
        </div>
      </div>
      <div class="summary-card">
        <span class="summary-icon amber"><el-icon><Warning /></el-icon></span>
        <div>
          <span>禁用 / 黑名单</span>
          <strong>{{ stats?.disabledUsers ?? 0 }} / {{ stats?.blacklistedUsers ?? 0 }}</strong>
          <small>门户账号风险情况</small>
        </div>
      </div>
    </section>

    <section class="chart-grid">
      <div class="chart-card bar-card">
        <div class="chart-heading">
          <div>
            <h2>近 7 日新增用户</h2>
            <p>按自然日统计门户用户注册数量</p>
          </div>
          <span class="chart-badge">BAR</span>
        </div>
        <div class="bar-chart">
          <div v-for="item in stats?.trend ?? []" :key="item.date" class="bar-column">
            <span class="bar-value">{{ item.count }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ height: `${Math.max(4, (item.count / maxTrend) * 100)}%` }"></div>
            </div>
            <span class="bar-label">{{ item.date }}</span>
          </div>
          <el-empty v-if="!stats?.trend?.length" description="暂无趋势数据" :image-size="60" />
        </div>
      </div>

      <div class="chart-card pie-card">
        <div class="chart-heading">
          <div>
            <h2>用户状态分布</h2>
            <p>启用与禁用账号占比</p>
          </div>
          <span class="chart-badge">PIE</span>
        </div>
        <div class="pie-wrap">
          <div class="pie" :style="{ background: pieBackground }">
            <div class="pie-hole">
              <strong>{{ totalForPie }}</strong>
              <span>总用户</span>
            </div>
          </div>
          <div class="pie-legend">
            <div v-for="(item, index) in stats?.statusDistribution ?? []" :key="item.name" class="legend-row">
              <span class="legend-dot" :style="{ background: pieColors[index % pieColors.length] }"></span>
              <span>{{ item.name }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ totalForPie ? Math.round((item.value / totalForPie) * 1000) / 10 : 0 }}%</small>
            </div>
            <el-empty v-if="!stats?.statusDistribution?.length" description="暂无分布数据" :image-size="60" />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.statistics-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 17px;
  border: 1px solid #e8edf5;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 8px 22px rgba(48, 66, 103, 0.05);
}

.summary-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 12px;
  font-size: 18px;
}

.summary-icon.blue { color: #4f6bfb; background: #eef2ff; }
.summary-icon.green { color: #169b72; background: #eafaf4; }
.summary-icon.purple { color: #8b5cf6; background: #f5efff; }
.summary-icon.amber { color: #c88518; background: #fff7e8; }

.summary-card > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.summary-card span {
  color: #8a94a6;
  font-size: 11px;
}

.summary-card strong {
  color: #263042;
  font-size: 24px;
  line-height: 1.2;
}

.summary-card small {
  color: #a0a9b8;
  font-size: 10px;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 0.85fr);
  gap: 14px;
}

.chart-card {
  min-height: 340px;
  padding: 18px 20px;
  border: 1px solid #e8edf5;
  border-radius: 17px;
  background: #ffffff;
  box-shadow: 0 8px 22px rgba(48, 66, 103, 0.05);
}

.chart-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.chart-heading h2 {
  margin: 0;
  color: #263042;
  font-size: 15px;
}

.chart-heading p {
  margin: 5px 0 0;
  color: #98a2b3;
  font-size: 10px;
}

.chart-badge {
  padding: 3px 7px;
  border-radius: 6px;
  color: #7c8cff;
  background: #f0f3ff;
  font-size: 9px;
  letter-spacing: 0.08em;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  height: 255px;
  margin-top: 18px;
  padding: 0 4px;
}

.bar-column {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  height: 100%;
  min-width: 0;
}

.bar-value {
  color: #6b7280;
  font-size: 10px;
}

.bar-track {
  display: flex;
  align-items: flex-end;
  width: min(34px, 100%);
  flex: 1;
  overflow: hidden;
  border-radius: 7px 7px 3px 3px;
  background: #f3f6fb;
}

.bar-fill {
  width: 100%;
  border-radius: 7px 7px 3px 3px;
  background: linear-gradient(180deg, #7388ff, #4f6bfb);
  transition: height 0.3s ease;
}

.bar-label {
  color: #9aa3b5;
  font-size: 9px;
  white-space: nowrap;
}

.pie-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 30px;
  min-height: 250px;
  margin-top: 12px;
}

.pie {
  position: relative;
  display: grid;
  place-items: center;
  width: 170px;
  height: 170px;
  flex: 0 0 170px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.55);
}

.pie-hole {
  display: grid;
  place-items: center;
  width: 102px;
  height: 102px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(31, 45, 70, 0.08);
}

.pie-hole strong {
  color: #263042;
  font-size: 25px;
  line-height: 1;
}

.pie-hole span {
  color: #9aa3b5;
  font-size: 9px;
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 110px;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #5f6b80;
  font-size: 11px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-row strong {
  margin-left: auto;
  color: #263042;
}

.legend-row small {
  color: #9aa3b5;
  font-size: 9px;
}

@media (max-width: 1000px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .chart-grid {
    grid-template-columns: 1fr;
  }
  .pie-wrap {
    justify-content: flex-start;
  }
}

@media (max-width: 620px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
