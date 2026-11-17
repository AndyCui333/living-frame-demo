import { createRouter, createWebHistory } from 'vue-router'
import { session } from './session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('./views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'home',
      component: () => import('./views/HomeView.vue'),
      meta: { tab: true },
    },
    {
      path: '/me',
      name: 'me',
      component: () => import('./views/MyView.vue'),
      meta: { tab: true },
    },
    {
      path: '/me/profile',
      name: 'profile',
      component: () => import('./views/ProfileView.vue'),
    },
    {
      path: '/me/password',
      name: 'password',
      component: () => import('./views/PasswordView.vue'),
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('./views/NotificationView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.public && !session.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && session.token) {
    return { name: 'home' }
  }
  return true
})

export default router
