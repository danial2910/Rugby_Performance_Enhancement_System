/**
 * chatbot.js — Pinia store for UC011: Chat with AI Chatbot
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import chatbotService from '@/services/chatbotService'

export const useChatbotStore = defineStore('chatbot', () => {
  const messages       = ref([])
  const loadingHistory = ref(false)
  const sending        = ref(false)
  const error          = ref(null)

  /** UC011 Step 2: Load saved conversation when the chat view mounts */
  async function fetchHistory() {
    loadingHistory.value = true
    error.value = null
    try {
      const response = await chatbotService.getHistory()
      if (response.success) {
        messages.value = response.data
      }
    } catch {
      error.value = 'Failed to load chat history.'
    } finally {
      loadingHistory.value = false
    }
  }

  /** UC011 Steps 3-7: Send a message and append both turns to the thread */
  async function sendMessage(text) {
    const trimmed = text?.trim()
    if (!trimmed || sending.value) return null

    error.value = null
    sending.value = true

    // Optimistic placeholder so the UI feels responsive while the AI thinks
    const pendingId = `pending-${Date.now()}`
    messages.value.push({
      id: pendingId,
      role: 'USER',
      content: trimmed,
      createdAt: new Date().toISOString(),
      pending: true
    })

    try {
      const response = await chatbotService.sendMessage(trimmed)
      // Drop the optimistic placeholder — the real persisted turns replace it
      messages.value = messages.value.filter(m => m.id !== pendingId)

      if (response.success) {
        messages.value.push(response.data.userMessage, response.data.assistantMessage)
        return response.data
      }
      error.value = response.message || 'Failed to send message.'
      return null
    } catch (err) {
      messages.value = messages.value.filter(m => m.id !== pendingId)
      if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
        error.value = 'The chatbot is taking too long to respond. Please try again.'
      } else if (err.response?.status === 400) {
        error.value = err.response.data?.message || 'Please check your message and try again.'
      } else if (!err.response) {
        error.value = 'Connection error. Please check that the backend server is running.'
      } else {
        error.value = 'Chatbot unavailable. Please try again later or contact support.'
      }
      return null
    } finally {
      sending.value = false
    }
  }

  /** Clear the conversation and start fresh */
  async function clearHistory() {
    error.value = null
    try {
      const response = await chatbotService.clearHistory()
      if (response.success) {
        messages.value = []
        return true
      }
      return false
    } catch {
      error.value = 'Failed to clear chat history.'
      return false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    messages, loadingHistory, sending, error,
    fetchHistory, sendMessage, clearHistory, clearError
  }
})
