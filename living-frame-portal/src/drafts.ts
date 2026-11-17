const PREFIX = 'living-frame-portal-drafts:'
const MAX_DRAFTS = 5

function key(userId) {
  return `${PREFIX}${userId || 'anonymous'}`
}

export function loadDrafts(userId) {
  try {
    const value = JSON.parse(localStorage.getItem(key(userId)) || '[]')
    return Array.isArray(value) ? value.slice(0, MAX_DRAFTS) : []
  } catch {
    return []
  }
}

export function saveDraft(userId, draft) {
  const drafts = loadDrafts(userId)
  const existingIndex = drafts.findIndex((item) => item.id === draft.id)
  const next = {
    ...draft,
    id: draft.id || `draft-${Date.now()}`,
    updatedAt: new Date().toISOString(),
  }
  if (existingIndex >= 0) {
    drafts[existingIndex] = next
  } else {
    if (drafts.length >= MAX_DRAFTS) {
      throw new Error(`草稿箱最多保存 ${MAX_DRAFTS} 条`)
    }
    drafts.unshift(next)
  }
  localStorage.setItem(key(userId), JSON.stringify(drafts.slice(0, MAX_DRAFTS)))
  return next
}

export function removeDraft(userId, draftId) {
  const drafts = loadDrafts(userId).filter((item) => item.id !== draftId)
  localStorage.setItem(key(userId), JSON.stringify(drafts))
  return drafts
}

export function clearAllDrafts() {
  const keys = []
  for (let i = 0; i < localStorage.length; i += 1) {
    const itemKey = localStorage.key(i)
    if (itemKey?.startsWith(PREFIX)) keys.push(itemKey)
  }
  keys.forEach((itemKey) => localStorage.removeItem(itemKey))
}
