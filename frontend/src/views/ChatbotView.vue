<template>
  <div class="chat-view">
    <header class="chat-header">
      <div>
        <h2>AI Chatbot</h2>
        <p class="subtitle">Ask about training, nutrition, recovery, or how to use the platform</p>
      </div>
      <button
        class="btn-ghost"
        type="button"
        :disabled="loadingHistory || messages.length === 0"
        @click="confirmClear"
      >
        Clear chat
      </button>
    </header>

    <div class="chat-thread" ref="threadEl">
      <div v-if="loadingHistory" class="state-msg">Loading conversation…</div>

      <div v-else-if="messages.length === 0" class="empty-state">
        <p class="empty-title">👋 Hey{{ firstName ? ` ${firstName}` : '' }}, ask me anything!</p>
        <p class="empty-sub">
          I can help with workout and nutrition questions, give general guidance, and explain
          how to use the meal planner, workout planner, and appointment booking.
        </p>
        <div class="suggestions">
          <button
            v-for="s in suggestions"
            :key="s"
            type="button"
            class="suggestion-chip"
            @click="send(s)"
          >
            {{ s }}
          </button>
        </div>
      </div>

      <template v-else>
        <div
          v-for="m in messages"
          :key="m.id"
          class="bubble-row"
          :class="m.role === 'USER' ? 'from-user' : 'from-bot'"
        >
          <div class="avatar" :class="m.role === 'USER' ? 'avatar-user' : 'avatar-bot'">
            {{ m.role === 'USER' ? (initials || 'U') : '🏉' }}
          </div>
          <div class="bubble" :class="{ pending: m.pending }">
            <p class="bubble-text">{{ m.content }}</p>
            <span class="bubble-time">{{ formatTime(m.createdAt) }}</span>
          </div>
        </div>

        <div v-if="sending" class="bubble-row from-bot">
          <div class="avatar avatar-bot">🏉</div>
          <div class="bubble typing">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
      </template>
    </div>

    <p v-if="error" class="error-banner">{{ error }}</p>

    <form class="composer" @submit.prevent="send()">
      <textarea
        v-model="draft"
        rows="1"
        placeholder="Type your question… (Enter to send, Shift+Enter for a new line)"
        :disabled="sending"
        @keydown.enter.exact.prevent="send()"
      />
      <button type="submit" class="btn-send" :disabled="sending || !draft.trim()">
        {{ sending ? 'Sending…' : 'Send' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useChatbotStore } from '@/stores/chatbot'
import { useAuthStore } from '@/stores/auth'

const chatbotStore = useChatbotStore()
const authStore    = useAuthStore()

const messages       = computed(() => chatbotStore.messages)
const loadingHistory = computed(() => chatbotStore.loadingHistory)
const sending        = computed(() => chatbotStore.sending)
const error          = computed(() => chatbotStore.error)

const draft    = ref('')
const threadEl = ref(null)

const suggestions = [
  'What is a good breakfast for muscle gain?',
  'How do I generate a workout plan?',
  'Suggest a recovery routine after a heavy training day.'
]

const firstName = computed(() => (authStore.fullName || '').split(' ')[0] || '')
const initials  = computed(() => {
  const name = authStore.fullName || authStore.username || ''
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(p => p[0]?.toUpperCase())
    .join('')
})

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

async function send(text) {
  const value = text ?? draft.value
  if (!value.trim() || sending.value) return
  draft.value = ''
  await chatbotStore.sendMessage(value)
  await scrollToBottom()
}

function confirmClear() {
  if (window.confirm('Clear this conversation? This cannot be undone.')) {
    chatbotStore.clearHistory()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (threadEl.value) {
    threadEl.value.scrollTop = threadEl.value.scrollHeight
  }
}

watch(messages, scrollToBottom, { deep: false })

onMounted(async () => {
  await chatbotStore.fetchHistory()
  await scrollToBottom()
})
</script>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 140px);
  min-height: 480px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border);
}
.chat-header h2 {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--color-green-light);
  margin: 0 0 4px;
}
.subtitle { color: var(--color-muted); margin: 0; font-size: 14px; }

.btn-ghost {
  flex-shrink: 0;
  background: transparent;
  border: 1px solid var(--color-border-2);
  color: var(--color-text-dim);
  border-radius: var(--radius-md);
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: border-color .15s, color .15s;
}
.btn-ghost:hover:not(:disabled) {
  border-color: var(--color-error-border);
  color: var(--color-error);
}
.btn-ghost:disabled { opacity: .4; cursor: not-allowed; }

.chat-thread {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.state-msg { color: var(--color-muted); text-align: center; padding: 40px 0; }

.empty-state {
  margin: auto;
  max-width: 480px;
  text-align: center;
}
.empty-title { font-size: 18px; font-weight: 600; color: var(--color-text); margin: 0 0 8px; }
.empty-sub { color: var(--color-text-dim); font-size: 14px; margin: 0 0 20px; line-height: 1.5; }
.suggestions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.suggestion-chip {
  background: var(--color-surface);
  border: 1px solid var(--color-border-2);
  color: var(--color-text-dim);
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: background .15s, color .15s, border-color .15s;
}
.suggestion-chip:hover {
  background: var(--color-green-faint);
  border-color: var(--color-green);
  color: var(--color-text);
}

.bubble-row { display: flex; align-items: flex-end; gap: 10px; max-width: 78%; }
.bubble-row.from-user { align-self: flex-end; flex-direction: row-reverse; }
.bubble-row.from-bot  { align-self: flex-start; }

.avatar {
  flex-shrink: 0;
  width: 34px; height: 34px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700;
}
.avatar-bot  { background: var(--color-green-faint); color: var(--color-green-light); }
.avatar-user { background: var(--color-surface-2); color: var(--color-text-dim); }

.bubble {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 12px 16px;
  position: relative;
}
.from-user .bubble {
  background: var(--color-green-faint);
  border-color: var(--color-green);
}
.bubble-text {
  margin: 0;
  color: var(--color-text);
  font-size: 14px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble-time {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: var(--color-muted);
  text-align: right;
}
.bubble.pending { opacity: .65; }

.bubble.typing { display: flex; align-items: center; gap: 5px; padding: 14px 18px; }
.dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--color-text-dim);
  animation: bounce 1.2s infinite ease-in-out;
}
.dot:nth-child(2) { animation-delay: .15s; }
.dot:nth-child(3) { animation-delay: .3s; }
@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: .4; }
  40% { transform: translateY(-4px); opacity: 1; }
}

.error-banner {
  margin: 0 24px 12px;
  padding: 10px 14px;
  background: var(--color-error-bg);
  border: 1px solid var(--color-error-border);
  color: var(--color-error);
  border-radius: var(--radius-md);
  font-size: 13px;
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
}
.composer textarea {
  flex: 1;
  resize: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border-2);
  border-radius: var(--radius-md);
  color: var(--color-text);
  font-family: inherit;
  font-size: 14px;
  padding: 12px 14px;
  line-height: 1.5;
  max-height: 140px;
}
.composer textarea:focus {
  outline: none;
  border-color: var(--color-green);
}
.composer textarea:disabled { opacity: .6; }

.btn-send {
  flex-shrink: 0;
  background: var(--color-green);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 12px 22px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s, opacity .15s;
}
.btn-send:hover:not(:disabled) { background: var(--color-green-light); }
.btn-send:disabled { opacity: .4; cursor: not-allowed; }
</style>
