import { ref, onMounted, onBeforeUnmount } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'

/**
 * Hold back any action that would discard unsaved editor text until the user
 * confirms it.
 *
 * Covers the four ways work used to vanish silently: Cancel, switching to
 * another plan in the side list, navigating within the app, and closing or
 * reloading the tab. In-app paths resolve through the caller's own dialog so
 * every exit asks the same question the same way; only tab close falls back to
 * the browser's native prompt, which is the one case a page cannot style.
 *
 * @param {object}   opts
 * @param {import('vue').Ref<boolean>} opts.isDirty  whether there is work to lose
 * @param {Function} [opts.onDiscard]  runs just before a confirmed discard
 */
export function useUnsavedGuard({ isDirty, onDiscard }) {
  /** null, or { run, cancel } awaiting the user's answer. */
  const pending = ref(null)

  /** Run `action` now, or park it behind a confirmation if work would be lost. */
  function guard(action) {
    if (!isDirty.value) {
      action()
      return
    }
    pending.value = { run: action }
  }

  /** User chose to lose the changes. */
  function discard() {
    const held = pending.value
    pending.value = null
    onDiscard?.()
    held?.run?.()
  }

  /** User chose to stay. Also resolves a held-back route navigation as blocked. */
  function keep() {
    const held = pending.value
    pending.value = null
    held?.cancel?.()
  }

  function onBeforeUnload(event) {
    if (!isDirty.value) return
    event.preventDefault()
    event.returnValue = ''
  }

  onMounted(() => window.addEventListener('beforeunload', onBeforeUnload))
  onBeforeUnmount(() => window.removeEventListener('beforeunload', onBeforeUnload))

  // Sidebar, bottom nav, back button. Returning a promise lets the router wait
  // on the same in-app dialog the other paths use instead of window.confirm.
  onBeforeRouteLeave(() => {
    if (!isDirty.value) return true
    return new Promise(resolve => {
      pending.value = {
        run: () => resolve(true),
        cancel: () => resolve(false)
      }
    })
  })

  return { pending, guard, discard, keep }
}
