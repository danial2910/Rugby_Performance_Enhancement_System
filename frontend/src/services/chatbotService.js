/**
 * chatbotService.js — API calls for UC011: Chat with AI Chatbot
 *
 * GET    /api/chatbot/history   → load saved conversation
 * POST   /api/chatbot/message   → send a message, get AI reply
 * DELETE /api/chatbot/history   → clear the conversation
 */
import http from './http'

const chatbotService = {
  async getHistory() {
    const { data } = await http.get('/chatbot/history')
    return data
  },

  async sendMessage(message) {
    // AI generation can take a while — override the default 10s timeout
    const { data } = await http.post('/chatbot/message', { message }, { timeout: 120000 })
    return data
  },

  async clearHistory() {
    const { data } = await http.delete('/chatbot/history')
    return data
  }
}

export default chatbotService
