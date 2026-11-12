<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <div class="error-code">403</div>
    <h1>没有访问权限</h1>
    <p>当前账号没有访问该页面的权限，请联系管理员分配对应角色。</p>
    <div class="error-actions">
      <el-button type="primary" @click="router.replace('/dashboard')">返回工作台</el-button>
      <el-button @click="router.back()">返回上一页</el-button>
    </div>
    <el-icon class="error-icon"><Lock /></el-icon>
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

.error-icon {
  margin-top: 36px;
  color: #c8d1ff;
  font-size: 42px;
}
</style>
