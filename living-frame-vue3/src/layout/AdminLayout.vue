<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import SidebarMenu from './components/SidebarMenu.vue'
import HeaderBar from './components/HeaderBar.vue'

const auth = useAuthStore()
const collapsed = ref(localStorage.getItem('lf-sidebar-collapsed') === '1')

watch(collapsed, (value) => {
  localStorage.setItem('lf-sidebar-collapsed', value ? '1' : '0')
})

onMounted(() => {
  if (window.innerWidth < 900) {
    collapsed.value = true
  }
})
</script>

<template>
  <div class="admin-shell" :class="{ 'is-collapsed': collapsed }">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">LF</div>
        <div v-show="!collapsed" class="brand-text">
          <strong>Living Frame</strong>
          <span>ADMIN CONSOLE</span>
        </div>
      </div>

      <div class="sidebar-scroll">
        <el-menu
          :default-active="$route.path"
          :collapse="collapsed"
          :unique-opened="true"
          router
          class="sidebar-menu"
        >
          <SidebarMenu :items="auth.menus" :collapsed="collapsed" />
        </el-menu>
        <div v-if="!auth.menus.length && !collapsed" class="menu-empty">暂无可用菜单</div>
      </div>

      <div v-show="!collapsed" class="sidebar-footer">
        <span>v1.0.0</span>
        <span>JDK 25 · Spring Boot 3</span>
      </div>
    </aside>

    <div class="main-area">
      <HeaderBar :collapsed="collapsed" @toggle="collapsed = !collapsed" />
      <main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: var(--lf-bg);
}

.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  width: 244px;
  height: 100vh;
  flex: 0 0 244px;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 0%, rgba(79, 107, 251, 0.25), transparent 34%),
    var(--lf-sidebar);
  transition: width 0.22s ease, flex-basis 0.22s ease;
  z-index: 10;
}

.is-collapsed .sidebar {
  width: 64px;
  flex-basis: 64px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 72px;
  padding: 0 16px;
  flex: 0 0 72px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 10px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #5b73ff, #8b5cf6);
  box-shadow: 0 6px 16px rgba(91, 115, 255, 0.35);
}

.brand-text {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  white-space: nowrap;
}

.brand-text strong {
  color: #f9fafb;
  font-size: 15px;
  letter-spacing: -0.2px;
}

.brand-text span {
  margin-top: 2px;
  color: #77839a;
  font-size: 9px;
  letter-spacing: 1.4px;
}

.sidebar-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
}

.sidebar-scroll::-webkit-scrollbar {
  width: 4px;
}

.sidebar-scroll::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.16);
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

.menu-empty {
  padding: 20px 24px;
  color: #77839a;
  font-size: 12px;
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  color: #5f6b80;
  font-size: 10px;
}

.main-area {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.content-area {
  flex: 1;
  padding: 22px 24px 30px;
  overflow: auto;
}

@media (max-width: 700px) {
  .content-area {
    padding: 16px 14px 24px;
  }
}
</style>
