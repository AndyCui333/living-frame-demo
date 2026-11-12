<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Avatar,
  Briefcase,
  ChatDotRound,
  Cpu,
  DataBoard,
  Document,
  FolderOpened,
  Key,
  MagicStick,
  Menu as MenuIcon,
  Setting,
  Tickets,
  User,
  Warning,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'
import type { MenuNode } from '@/types/api'

defineOptions({ name: 'SidebarMenu' })

const props = defineProps<{
  items: MenuNode[]
  collapsed: boolean
}>()

const route = useRoute()
const activePath = computed(() => route.path)

const iconMap: Record<string, Component> = {
  'system': Setting,
  'system:user:list': User,
  'system:role:list': Avatar,
  'system:permission:list': Key,
  'system:login-log:list': Document,
  'system:operation-log:list': Tickets,
  'system:blacklist:list': Warning,
  'system:file:list': FolderOpened,
  'business:user:list': User,
  'ai': MagicStick,
  'ai:chat': ChatDotRound,
  'ai:agent': Cpu,
  'ai:config': Setting,
  dashboard: DataBoard,
}

function iconFor(item: MenuNode): Component {
  if (item.icon && iconMap[item.icon]) {
    return iconMap[item.icon]
  }
  if (item.code && iconMap[item.code]) {
    return iconMap[item.code]
  }
  if (item.path?.includes('/user')) return User
  if (item.path?.includes('/role')) return Avatar
  if (item.path?.includes('/permission')) return Key
  if (item.path?.includes('/chat')) return ChatDotRound
  if (item.path?.includes('/agent')) return Cpu
  if (item.path?.includes('/ai')) return MagicStick
  if (item.path?.includes('/business')) return Briefcase
  if (item.path?.includes('/system')) return Setting
  return MenuIcon
}
</script>

<template>
  <template v-for="item in props.items" :key="item.id">
    <el-sub-menu v-if="item.children && item.children.length" :index="item.code || item.path || item.id">
      <template #title>
        <el-icon><component :is="iconFor(item)" /></el-icon>
        <span>{{ item.name }}</span>
      </template>
      <SidebarMenu :items="item.children" :collapsed="props.collapsed" />
    </el-sub-menu>
    <el-menu-item v-else :index="item.path || '/dashboard'">
      <el-icon><component :is="iconFor(item)" /></el-icon>
      <template #title>
        <span>{{ item.name }}</span>
      </template>
    </el-menu-item>
  </template>
</template>

<style scoped>
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 46px;
  margin: 4px 10px;
  border-radius: 9px;
  color: #aeb8cc;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.06);
}

/* 二级菜单悬停时保持浅蓝色文字，避免白色文字与浅色背景混在一起 */
:deep(.el-sub-menu .el-menu-item:not(.is-active):hover) {
  color: #c7d2fe;
  background: rgba(255, 255, 255, 0.05);
}

/* Element Plus 内联子菜单默认是白色背景，需要覆盖成侧边栏深色 */
:deep(.el-menu--inline) {
  background: transparent !important;
}

:deep(.el-menu--inline .el-menu-item) {
  color: #aeb8cc;
}

:deep(.el-sub-menu__title .el-sub-menu__icon-arrow) {
  position: static;
  margin-left: auto;
  margin-right: 0;
}

:deep(.el-menu--inline .el-menu-item:hover) {
  color: #c7d2fe !important;
  background: rgba(255, 255, 255, 0.05) !important;
}

:deep(.el-menu--inline .el-menu-item.is-active) {
  color: #ffffff !important;
  background: linear-gradient(90deg, rgba(79, 107, 251, 0.95), rgba(124, 92, 255, 0.9)) !important;
}

:deep(.el-menu--inline .el-menu-item.is-active:hover) {
  color: #ffffff !important;
}

:deep(.el-menu-item.is-active) {
  color: #ffffff;
  background: linear-gradient(90deg, rgba(79, 107, 251, 0.95), rgba(124, 92, 255, 0.9));
  box-shadow: 0 8px 18px rgba(79, 107, 251, 0.28);
}

:deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
}
</style>
