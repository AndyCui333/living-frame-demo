import { reactive } from 'vue'

export const chatState = reactive({
  open: false,
  initialType: 'CHAT',
  showHistory: false,
  targetTicketId: null,
  nonce: 0,
})

export function openChat(type = 'CHAT', showHistory = false, targetTicketId = null) {
  chatState.initialType = type
  chatState.showHistory = showHistory
  chatState.targetTicketId = targetTicketId
  chatState.open = true
  chatState.nonce += 1
}
