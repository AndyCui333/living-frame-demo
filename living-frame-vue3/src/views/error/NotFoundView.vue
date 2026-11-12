<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <div class="error-code">404</div>
    <h1>页面不存在</h1>
    <p>你访问的页面可能已被移动或删除，请检查地址后重试。</p>
    <div class="error-actions">
      <el-button type="primary" @click="router.replace('/dashboard')">返回工作台</el-button>
      <el-button @click="router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  text-align: center;
  background: #f6f7fc;
}

.error-code {
  color: #dfe5fe;
  font-size: 120px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -6px;
}

h1 {
  margin: 10px 0 0;
  color: #111827;
  font-size: 24px;
}

p {
  max-width: 420px;
  margin: 10px 0 24px;
  color: #8d96a8;
  font-size: 13px;
  line-height: 1.8;
}

.error-actions {
  display: flex;
  gap: 10px;
}
</style>
