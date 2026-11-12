import { reactive } from 'vue'

export const chatState = reactive({
  open: false,
  initialType: 'CHAT',
  showHistory: false,
  nonce: 0,
})

export function openChat(type = 'CHAT', showHistory = false) {
  chatState.initialType = type
  chatState.showHistory = showHistory
  chatState.open = true
  chatState.nonce += 1
}
