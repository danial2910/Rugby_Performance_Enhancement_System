<template>
  <div
    class="modal-overlay"
    @click.self="emit('close')">
    <div
      ref="dialogEl"
      class="modal"
      :class="{ 'modal-wide': wide }"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="titleId"
      @keydown="onKeydown">
      <h2 :id="titleId" class="modal-title">
        <slot name="title">{{ title }}</slot>
      </h2>

      <div class="modal-body">
        <slot />
      </div>

      <div class="modal-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

// Plain counter rather than Vue's useId(): package.json declares vue ^3.4.0
// and useId() only exists from 3.5.
let modalSeq = 0

const props = defineProps({
  title: { type: String, default: '' },
  wide: { type: Boolean, default: false },
  /** Selector for the control to focus on open. Defaults to the first one. */
  initialFocus: { type: String, default: '' }
})

const emit = defineEmits(['close'])

const dialogEl = ref(null)
const titleId = `modal-title-${++modalSeq}`

let previouslyFocused = null

const FOCUSABLE = [
  'a[href]', 'button:not([disabled])', 'input:not([disabled])',
  'select:not([disabled])', 'textarea:not([disabled])', '[tabindex]:not([tabindex="-1"])'
].join(',')

function focusable() {
  if (!dialogEl.value) return []
  return Array.from(dialogEl.value.querySelectorAll(FOCUSABLE))
    .filter(el => el.offsetParent !== null)
}

function onKeydown(event) {
  if (event.key === 'Escape') {
    event.stopPropagation()
    emit('close')
    return
  }

  if (event.key !== 'Tab') return

  // Trap Tab inside the dialog. Without this, focus walks into the form
  // behind the overlay, where a keyboard user can operate controls they
  // cannot see — including while a delete confirmation is open.
  const items = focusable()
  if (!items.length) {
    event.preventDefault()
    return
  }

  const first = items[0]
  const last = items[items.length - 1]
  const active = document.activeElement

  if (event.shiftKey && (active === first || !dialogEl.value.contains(active))) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && active === last) {
    event.preventDefault()
    first.focus()
  }
}

onMounted(() => {
  previouslyFocused = document.activeElement
  document.body.style.overflow = 'hidden'

  const target = props.initialFocus
    ? dialogEl.value?.querySelector(props.initialFocus)
    : focusable()[0]
  target?.focus()
})

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  previouslyFocused?.focus?.()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  /* Short viewports: let the overlay scroll rather than clipping the dialog's
     title and actions off both edges with no way to reach them. */
  overflow-y: auto;
  padding: 16px;
}

.modal {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 28px;
  width: 100%;
  max-width: 400px;
  max-height: calc(100dvh - 32px);
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.modal-wide { max-width: 480px; }

.modal-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 20px;
  font-weight: 700;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  overflow-y: auto;
}
.modal-body :deep(p) {
  font-size: 14px;
  color: var(--color-text-dim);
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 4px;
  flex-wrap: wrap;
}

@media (max-width: 480px) {
  .modal { padding: 22px; }
  .modal-actions :deep(.btn) { flex: 1; justify-content: center; }
}
</style>
