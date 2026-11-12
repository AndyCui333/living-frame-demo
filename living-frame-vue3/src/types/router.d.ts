import type { RouteMeta } from 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    permission?: string
    public?: boolean
  }
}

export {}
