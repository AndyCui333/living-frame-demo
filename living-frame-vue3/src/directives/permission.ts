import type { Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'

export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const auth = useAuthStore()
    const values = Array.isArray(binding.value) ? binding.value : [binding.value]
    if (!values.some((value) => auth.hasPermission(value))) {
      el.parentNode?.removeChild(el)
    }
  },
}
