<template>
  <div ref="root" class="plan-edit" @keydown="onKeydown">

    <div class="edit-toolbar">
      <div class="edit-pane-toggle" role="group" aria-label="Editor layout">
        <button
          v-for="mode in availableModes"
          :key="mode.value"
          type="button"
          class="pane-btn"
          :aria-pressed="paneMode === mode.value"
          @click="setPane(mode.value)">
          {{ mode.label }}
        </button>
      </div>

      <div class="edit-toolbar-end">
        <p class="shortcut-hint">
          <kbd>{{ saveKeyLabel }}</kbd> save · <kbd>Esc</kbd> cancel
        </p>
        <button
          v-if="canRestore"
          type="button"
          class="btn btn-ghost btn-sm"
          @click="emit('restore')">
          Restore original
        </button>
      </div>
    </div>

    <div class="edit-split" :class="'pane-' + effectivePane">
      <div class="edit-col edit-col-write">
        <label class="form-label" :for="`${idPrefix}-edit-content`">
          Plan content (Markdown)
        </label>
        <textarea
          :id="`${idPrefix}-edit-content`"
          ref="textarea"
          :value="modelValue"
          class="plan-editor"
          spellcheck="false"
          :aria-invalid="!!error"
          :aria-describedby="`${idPrefix}-edit-content-hint`"
          :placeholder="placeholder"
          @input="emit('update:modelValue', $event.target.value)"
          @scroll="onEditorScroll"></textarea>
        <p v-if="error" class="field-error">{{ error }}</p>
        <p class="field-hint" :id="`${idPrefix}-edit-content-hint`">{{ hint }}</p>
      </div>

      <div class="edit-col edit-col-preview">
        <h3 class="form-label preview-label" :id="`${idPrefix}-preview-label`">Preview</h3>
        <div
          ref="preview"
          class="preview-pane"
          role="region"
          :aria-labelledby="`${idPrefix}-preview-label`"
          tabindex="0"
          @scroll="onPreviewScroll">
          <div class="plan-markdown" v-html="renderedPreview"></div>
        </div>
        <p
          class="field-hint preview-status"
          :class="{ 'status-warn': trackedCount === 0 }"
          role="status">
          <template v-if="trackedCount === 0">
            No {{ noun.many }} detected — progress tracking will be empty. {{ zeroHint }}
          </template>
          <template v-else>
            {{ trackedCount }} {{ trackedCount === 1 ? noun.one : noun.many }}
            detected for progress tracking.
          </template>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  modelValue: { type: String, default: '' },
  /** Namespaces the element ids so two editors never collide. */
  idPrefix: { type: String, required: true },
  /** Counts the trackable items the current text exposes. */
  parse: { type: Function, required: true },
  noun: { type: Object, default: () => ({ one: 'item', many: 'items' }) },
  hint: { type: String, default: '' },
  zeroHint: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  canRestore: { type: Boolean, default: false },
  error: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'save', 'cancel', 'restore'])

const root = ref(null)
const textarea = ref(null)
const preview = ref(null)

const PANE_KEY = 'rpes:plan-editor-pane'
const SPLIT_MIN = 900

const allModes = [
  { value: 'write', label: 'Write' },
  { value: 'split', label: 'Split' },
  { value: 'preview', label: 'Preview' }
]

/** Measured width of the editor itself, not the viewport. */
const containerWidth = ref(0)
const splitViable = computed(() => containerWidth.value >= SPLIT_MIN)
const availableModes = computed(() =>
  splitViable.value ? allModes : allModes.filter(m => m.value !== 'split')
)

const paneMode = ref('write')
/** Never render a two-column split the container cannot actually hold. */
const effectivePane = computed(() =>
  paneMode.value === 'split' && !splitViable.value ? 'write' : paneMode.value
)

function setPane(mode) {
  paneMode.value = mode
  try { localStorage.setItem(PANE_KEY, mode) } catch { /* private mode */ }
}

const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform)
const saveKeyLabel = isMac ? '⌘S' : 'Ctrl+S'

// ── Debounced derivations ────────────────────────────────────────────────────
// Rendering and parsing the whole plan on every keystroke is wasted work; the
// textarea itself stays instant because it is never debounced.
const previewText = ref(props.modelValue)
const countText = ref(props.modelValue)
let previewTimer = null
let countTimer = null

watch(() => props.modelValue, value => {
  clearTimeout(previewTimer)
  clearTimeout(countTimer)
  previewTimer = setTimeout(() => { previewText.value = value }, 150)
  countTimer = setTimeout(() => { countText.value = value }, 400)
})

const renderedPreview = computed(() => renderMarkdown(previewText.value))
const trackedCount = computed(() => props.parse(countText.value))

// ── Proportional scroll sync ─────────────────────────────────────────────────
// Writing to one pane's scrollTop fires its own scroll event, so a short lock
// stops the two panes echoing each other. The lock is released on a timer, not
// requestAnimationFrame: rAF is throttled when the frame is occluded or
// backgrounded, which would leave the lock stuck on and kill sync entirely.
let syncing = false
let syncTimer = null

function sync(source, target) {
  if (syncing || !source || !target) return
  const sourceMax = source.scrollHeight - source.clientHeight
  const targetMax = target.scrollHeight - target.clientHeight
  if (sourceMax <= 0 || targetMax <= 0) return
  syncing = true
  target.scrollTop = (source.scrollTop / sourceMax) * targetMax
  clearTimeout(syncTimer)
  syncTimer = setTimeout(() => { syncing = false }, 80)
}

function onEditorScroll() {
  if (effectivePane.value === 'split') sync(textarea.value, preview.value)
}
function onPreviewScroll() {
  if (effectivePane.value === 'split') sync(preview.value, textarea.value)
}

// ── Keyboard ─────────────────────────────────────────────────────────────────
function onKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    // Otherwise this opens the browser's "Save page as" dialog over the plan.
    event.preventDefault()
    emit('save')
    return
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    emit('cancel')
  }
}

// ── Lifecycle ────────────────────────────────────────────────────────────────
let observer = null

onMounted(async () => {
  if (root.value && 'ResizeObserver' in window) {
    observer = new ResizeObserver(entries => {
      containerWidth.value = entries[0].contentRect.width
    })
    observer.observe(root.value)
  }
  containerWidth.value = root.value?.getBoundingClientRect().width ?? 0

  let stored = null
  try { stored = localStorage.getItem(PANE_KEY) } catch { /* private mode */ }
  paneMode.value = allModes.some(m => m.value === stored)
    ? stored
    : (containerWidth.value >= SPLIT_MIN ? 'split' : 'write')

  // Opening the editor without this leaves focus on <body>, 30+ tab stops away.
  await nextTick()
  textarea.value?.focus()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  clearTimeout(previewTimer)
  clearTimeout(countTimer)
  clearTimeout(syncTimer)
})
</script>

<style scoped>
.plan-edit { display: flex; flex-direction: column; gap: 14px; }

/* ── Toolbar ─────────────────────────────────────────────────── */
.edit-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; flex-wrap: wrap;
}
.edit-toolbar-end { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.edit-pane-toggle {
  display: inline-flex; gap: 2px; padding: 3px;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
.pane-btn {
  padding: 6px 14px; min-height: 36px;
  border: none; background: none; cursor: pointer;
  font-family: inherit; font-size: 12.5px; font-weight: 500;
  color: var(--color-muted);
  border-radius: 4px;
  transition: background var(--transition), color var(--transition);
}
.pane-btn:hover { color: var(--color-text); }
.pane-btn[aria-pressed="true"] {
  background: var(--color-surface-2);
  color: var(--color-green-on-tint);
}

.shortcut-hint { font-size: 11.5px; color: var(--color-muted); }
.shortcut-hint kbd {
  font-family: inherit; font-size: 10.5px; font-weight: 600;
  padding: 2px 5px; border-radius: 4px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-2);
  color: var(--color-text-dim);
}

@media (pointer: coarse) {
  .pane-btn { min-height: var(--touch-target); }
  /* No physical keyboard to speak of. */
  .shortcut-hint { display: none; }
}
/* Also drop it whenever the toolbar is too tight to carry it without wrapping. */
@container editor (max-width: 560px) {
  .shortcut-hint { display: none; }
}

/* ── Split ───────────────────────────────────────────────────── */
/* Keyed to this element's own width, not the viewport: the app's 240px
   sidebar and the form column leave the editor far narrower than the window,
   so a viewport breakpoint put a 253px editor on a 1280px laptop. */
.plan-edit { container-type: inline-size; container-name: editor; }

.edit-split { display: grid; gap: 14px; grid-template-columns: minmax(0, 1fr); }
.edit-col { display: flex; flex-direction: column; gap: 5px; min-width: 0; }

.pane-write .edit-col-preview { display: none; }
.pane-preview .edit-col-write { display: none; }

@container editor (min-width: 900px) {
  .edit-split.pane-split { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
  /* One shared height so the two columns end level and scroll in step. */
  .edit-split.pane-split .plan-editor,
  .edit-split.pane-split .preview-pane { height: 560px; }
  .edit-split.pane-split .plan-editor { resize: none; }
}

.preview-label { color: var(--color-muted); font-size: 13px; font-weight: 500; }

/* ── Editor ──────────────────────────────────────────────────── */
.plan-editor {
  width: 100%; min-height: 480px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  font-size: 13px;
  /* Monospace for pipe-table alignment, not as a technical costume. */
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  padding: 14px;
  resize: vertical; box-sizing: border-box; line-height: 1.6;
  tab-size: 2;
}
.plan-editor::placeholder { color: var(--color-muted); }
.plan-editor:focus { outline: none; border-color: var(--color-green-light); }

/* ── Preview ─────────────────────────────────────────────────── */
.preview-pane {
  min-height: 480px; overflow-y: auto;
  padding: 16px 18px;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
.preview-pane:focus-visible {
  outline: 2px solid var(--color-green-light); outline-offset: 2px;
}
.preview-pane .plan-markdown :deep(.md-h2:first-child) { margin-top: 0; }

.preview-status { font-variant-numeric: tabular-nums; }
.preview-status.status-warn {
  color: var(--color-warn);
  background: var(--color-warn-bg);
  border: 1px solid var(--color-warn-border);
  border-radius: var(--radius-sm);
  padding: 7px 10px;
  line-height: 1.5;
}

/* ── Shared markdown presentation (mirrors the plan view) ────── */
.plan-markdown :deep(.md-h2) {
  font-family: 'Barlow Condensed', sans-serif; font-size: 20px; font-weight: 700;
  color: var(--color-green-on-tint); margin: 28px 0 12px;
  padding-bottom: 6px; border-bottom: 1px solid var(--color-border);
}
.plan-markdown :deep(.md-h3) { font-size: 15px; font-weight: 600; color: var(--color-text); margin: 18px 0 8px; }
.plan-markdown :deep(.md-h4) { font-size: 13.5px; font-weight: 600; color: var(--color-text-dim); margin: 12px 0 6px; }
.plan-markdown :deep(.md-p)  { font-size: 14px; line-height: 1.7; color: var(--color-text-dim); margin: 8px 0; }
.plan-markdown :deep(.md-ul) { padding-left: 18px; margin: 8px 0; display: flex; flex-direction: column; gap: 5px; }
.plan-markdown :deep(li)     { font-size: 14px; line-height: 1.6; color: var(--color-text-dim); }
.plan-markdown :deep(.md-table-wrap) { display: block; width: 100%; overflow-x: auto; }
.plan-markdown :deep(.md-table) {
  width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 13px;
  border: 1px solid var(--color-border); border-radius: var(--radius-sm); overflow: hidden;
}
.plan-markdown :deep(.md-table th) {
  padding: 8px 10px; text-align: left; font-weight: 600; color: var(--color-text);
  background: var(--color-surface); border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}
.plan-markdown :deep(.md-table td) {
  padding: 7px 10px; border-top: 1px solid var(--color-border);
  color: var(--color-text-dim); font-variant-numeric: tabular-nums;
}
.plan-markdown :deep(.md-hr) { border: none; border-top: 1px solid var(--color-border); margin: 20px 0; }
.plan-markdown :deep(strong) { color: var(--color-text); }

/* ── Buttons used inside the toolbar ─────────────────────────── */
.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  border-radius: var(--radius-sm); font-size: 13px; font-weight: 500;
  cursor: pointer; border: none; font-family: inherit;
  transition: background var(--transition), color var(--transition);
  white-space: nowrap;
}
.btn-ghost {
  background: var(--color-surface); color: var(--color-text);
  border: 1px solid var(--color-border);
}
.btn-ghost:hover { background: var(--color-surface-2); }
.btn-sm { padding: 6px 10px; min-height: 36px; font-size: 12px; }
@media (pointer: coarse) {
  .btn-sm { min-height: var(--touch-target); }
}

@media (prefers-reduced-motion: reduce) {
  .pane-btn, .btn { transition: none; }
}
</style>
