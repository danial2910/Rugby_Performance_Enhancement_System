<template>
  <div class="trainer-page">

    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">Athlete Plan Management</h1>
        <p class="page-subtitle">Monitor progress and edit your athletes' workout and meal plans</p>
      </div>
    </div>

    <div class="trainer-layout">

      <!-- ══════════════════════════════════════════
           PANEL 1: Athlete List
           ══════════════════════════════════════════ -->
      <div class="panel panel-athletes">
        <div class="panel-header">
          <h2 class="panel-title">Athletes</h2>
          <span class="count-badge">{{ trainerStore.athletes.length }}</span>
        </div>

        <!-- Search -->
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            v-model="athleteSearch"
            type="text"
            class="search-input"
            placeholder="Search athletes..."
          />
        </div>

        <!-- Loading -->
        <div v-if="trainerStore.loadingAthletes" class="panel-loading">
          <div class="spinner"></div>
          <span>Loading athletes…</span>
        </div>

        <!-- Empty -->
        <div v-else-if="filteredAthletes.length === 0" class="panel-empty">
          <span class="empty-icon">👥</span>
          <p>No athletes found</p>
        </div>

        <!-- List -->
        <div v-else class="athlete-list">
          <button
            v-for="athlete in filteredAthletes"
            :key="athlete.id"
            class="athlete-item"
            :class="{ active: trainerStore.selectedAthlete?.id === athlete.id }"
            @click="handleSelectAthlete(athlete)"
          >
            <div class="athlete-avatar">
              {{ initials(athlete.fullName || athlete.username) }}
            </div>
            <div class="athlete-info">
              <div class="athlete-name">{{ athlete.fullName || athlete.username }}</div>
              <div class="athlete-meta">
                <span v-if="athlete.rugbyPosition" class="pos-tag">{{ athlete.rugbyPosition }}</span>
                <span class="plan-counts">
                  💪 {{ athlete.workoutPlanCount }} &nbsp; 🥗 {{ athlete.mealPlanCount }}
                </span>
              </div>
            </div>
            <span class="chevron">›</span>
          </button>
        </div>
      </div>

      <!-- ══════════════════════════════════════════
           PANEL 2: Plan List
           ══════════════════════════════════════════ -->
      <div class="panel panel-plans">
        <!-- No athlete selected -->
        <div v-if="!trainerStore.selectedAthlete" class="panel-placeholder">
          <span class="placeholder-icon">👈</span>
          <p>Select an athlete to view their plans</p>
        </div>

        <template v-else>
          <div class="panel-header">
            <div class="athlete-selected-name">
              {{ trainerStore.selectedAthlete.fullName || trainerStore.selectedAthlete.username }}
            </div>
          </div>

          <!-- Type tabs -->
          <div class="type-tabs">
            <button
              class="type-tab"
              :class="{ active: trainerStore.planType === 'workout' }"
              @click="trainerStore.setPlanType('workout')"
            >
              💪 Workout
              <span class="tab-count">{{ trainerStore.workoutPlans.length }}</span>
            </button>
            <button
              class="type-tab"
              :class="{ active: trainerStore.planType === 'meal' }"
              @click="trainerStore.setPlanType('meal')"
            >
              🥗 Meal
              <span class="tab-count">{{ trainerStore.mealPlans.length }}</span>
            </button>
          </div>

          <!-- Loading plans -->
          <div v-if="trainerStore.loadingPlans" class="panel-loading">
            <div class="spinner"></div>
            <span>Loading plans…</span>
          </div>

          <!-- Empty plans -->
          <div v-else-if="activePlanList.length === 0" class="panel-empty">
            <span class="empty-icon">📋</span>
            <p>No {{ trainerStore.planType }} plans yet</p>
          </div>

          <!-- Plan list -->
          <div v-else class="plan-list">
            <button
              v-for="plan in activePlanList"
              :key="plan.id"
              class="plan-item"
              :class="{
                active: trainerStore.selectedPlan?.id === plan.id,
                'has-note': plan.trainerNote
              }"
              @click="trainerStore.selectPlan(plan, trainerStore.planType)"
            >
              <div class="plan-item-name">{{ plan.planName }}</div>
              <div class="plan-item-meta">
                <span>{{ formatDate(plan.createdAt) }}</span>
                <span v-if="plan.isActive" class="active-pill">ACTIVE</span>
                <span v-if="plan.trainerNote" class="note-pill">✏️ Edited</span>
              </div>
            </button>
          </div>
        </template>
      </div>

      <!-- ══════════════════════════════════════════
           PANEL 3: Plan Viewer / Editor
           ══════════════════════════════════════════ -->
      <div class="panel panel-viewer">
        <!-- No plan selected -->
        <div v-if="!trainerStore.selectedPlan" class="panel-placeholder">
          <span class="placeholder-icon">📄</span>
          <p>Select a plan to view or edit it</p>
        </div>

        <template v-else>
          <!-- Viewer header -->
          <div class="viewer-header">
            <div class="viewer-title-row">
              <h3 class="viewer-plan-name">{{ trainerStore.selectedPlan.planName }}</h3>
              <div class="viewer-actions">
                <button v-if="!editMode" class="btn btn-edit" @click="startEdit">
                  ✏️ Edit Plan
                </button>
                <template v-else>
                  <button class="btn btn-save" :disabled="trainerStore.saving" @click="handleSave">
                    <span v-if="trainerStore.saving" class="btn-spinner"></span>
                    {{ trainerStore.saving ? 'Saving…' : '✅ Save Changes' }}
                  </button>
                  <button class="btn btn-cancel" :disabled="trainerStore.saving" @click="cancelEdit">
                    Cancel
                  </button>
                </template>
              </div>
            </div>

            <!-- Plan meta chips -->
            <div class="plan-chips">
              <span v-if="trainerStore.selectedPlan.rugbyPosition" class="chip">
                🏉 {{ trainerStore.selectedPlan.rugbyPosition }}
              </span>
              <span v-if="trainerStore.selectedPlan.goal" class="chip">
                🎯 {{ goalLabel(trainerStore.selectedPlan.goal) }}
              </span>
              <span v-if="trainerStore.selectedPlan.goals && trainerStore.selectedPlan.goals.length" class="chip">
                🎯 {{ trainerStore.selectedPlan.goals.map(g => goalLabel(g)).join(' + ') }}
              </span>
              <span v-if="trainerStore.selectedPlan.trainingPhase" class="chip">
                📅 {{ phaseLabel(trainerStore.selectedPlan.trainingPhase) }}
              </span>
              <span v-if="trainerStore.selectedPlan.isActive" class="chip chip-active">
                ✅ ACTIVE
              </span>
            </div>

            <!-- ── Plan Progress (visible in read mode only) ──────────── -->
            <div v-if="!editMode && trainerStore.selectedPlan.completedItems" class="progress-section">
              <div class="progress-header">
                <span class="progress-title">📊 Athlete Progress</span>
                <span class="progress-fraction">
                  {{ trainerStore.selectedPlan.completedItems.length }}
                  / {{ estimatedTotalItems(trainerStore.selectedPlan) }} items completed
                </span>
              </div>
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :style="{ width: planProgressPct(trainerStore.selectedPlan) + '%' }"
                  :class="{
                    'fill-low':  planProgressPct(trainerStore.selectedPlan) < 34,
                    'fill-mid':  planProgressPct(trainerStore.selectedPlan) >= 34 && planProgressPct(trainerStore.selectedPlan) < 75,
                    'fill-high': planProgressPct(trainerStore.selectedPlan) >= 75
                  }"
                ></div>
              </div>
              <div class="progress-pct-label">{{ planProgressPct(trainerStore.selectedPlan) }}% complete</div>
            </div>

            <!-- Existing trainer note display -->
            <div v-if="trainerStore.selectedPlan.trainerNote && !editMode" class="trainer-note-banner">
              <div class="note-banner-header">
                <span>✏️ Trainer Note</span>
                <span v-if="trainerStore.selectedPlan.lastEditedBy" class="note-by">
                  by {{ trainerStore.selectedPlan.lastEditedBy }}
                </span>
              </div>
              <p class="note-banner-text">{{ trainerStore.selectedPlan.trainerNote }}</p>
            </div>
          </div>

          <!-- Save Success banner -->
          <transition name="fade">
            <div v-if="trainerStore.saveSuccess" class="alert alert-success">
              ✅ Plan updated successfully.
              <button class="alert-close" @click="trainerStore.clearSaveSuccess()">×</button>
            </div>
          </transition>

          <!-- Error banner -->
          <transition name="fade">
            <div v-if="trainerStore.error" class="alert alert-error">
              ✕ {{ trainerStore.error }}
              <button class="alert-close" @click="trainerStore.clearError()">×</button>
            </div>
          </transition>

          <!-- ── READ MODE ────────────────────────────────────────────── -->
          <div v-if="!editMode" class="plan-content">
            <div
              class="markdown-body"
              v-html="renderMarkdown(trainerStore.selectedPlan.generatedPlan)"
            ></div>
          </div>

          <!-- ── EDIT MODE ───────────────────────────────────────────── -->
          <div v-else class="edit-form">

            <!-- Plan name -->
            <div class="form-group">
              <label class="form-label">Plan Name</label>
              <input
                v-model="editForm.planName"
                type="text"
                class="form-input"
                placeholder="Plan name"
              />
              <p v-if="editErrors.planName" class="field-error">{{ editErrors.planName }}</p>
            </div>

            <!-- Plan content -->
            <div class="form-group">
              <label class="form-label">Plan Content</label>
              <textarea
                v-model="editForm.generatedPlan"
                class="form-textarea plan-textarea"
                placeholder="Edit the plan content (supports Markdown)"
              ></textarea>
              <p v-if="editErrors.generatedPlan" class="field-error">{{ editErrors.generatedPlan }}</p>
            </div>

            <!-- Trainer note — MANDATORY -->
            <div class="form-group" :class="{ 'has-error': editErrors.trainerNote }">
              <label class="form-label">
                Trainer Note <span class="req">*</span>
                <span class="label-hint">Required — explain why you are modifying this plan</span>
              </label>
              <textarea
                v-model="editForm.trainerNote"
                class="form-textarea note-textarea"
                placeholder="e.g. Adjusted squat volume to account for athlete's knee injury. Replaced high-impact plyometrics with swimming until fully recovered."
                maxlength="1000"
              ></textarea>
              <div class="note-footer">
                <p v-if="editErrors.trainerNote" class="field-error">{{ editErrors.trainerNote }}</p>
                <span class="char-count">{{ editForm.trainerNote.length }}/1000</span>
              </div>
            </div>
          </div>
        </template>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useTrainerStore } from '@/stores/trainer'

const trainerStore = useTrainerStore()

// ── Athlete search ────────────────────────────────────────────────────────────

const athleteSearch = ref('')

const filteredAthletes = computed(() => {
  const q = athleteSearch.value.toLowerCase().trim()
  if (!q) return trainerStore.athletes
  return trainerStore.athletes.filter(a =>
    (a.fullName   || '').toLowerCase().includes(q) ||
    (a.username   || '').toLowerCase().includes(q) ||
    (a.matrixNumber || '').toLowerCase().includes(q) ||
    (a.rugbyPosition || '').toLowerCase().includes(q)
  )
})

// ── Computed plan list based on active tab ────────────────────────────────────

const activePlanList = computed(() =>
  trainerStore.planType === 'workout'
    ? trainerStore.workoutPlans
    : trainerStore.mealPlans
)

// ── Edit mode state ───────────────────────────────────────────────────────────

const editMode  = ref(false)
const editForm  = ref({ planName: '', generatedPlan: '', trainerNote: '' })
const editErrors = ref({})

function startEdit() {
  const plan = trainerStore.selectedPlan
  editForm.value = {
    planName:      plan.planName      || '',
    generatedPlan: plan.generatedPlan || '',
    trainerNote:   ''                        // always blank — trainer must re-enter
  }
  editErrors.value = {}
  editMode.value   = true
  trainerStore.clearError()
  trainerStore.clearSaveSuccess()
}

function cancelEdit() {
  editMode.value   = false
  editErrors.value = {}
  trainerStore.clearError()
}

function validateEdit() {
  editErrors.value = {}
  if (!editForm.value.planName?.trim())
    editErrors.value.planName = 'Plan name is required.'
  if (!editForm.value.generatedPlan?.trim())
    editErrors.value.generatedPlan = 'Plan content is required.'
  if (!editForm.value.trainerNote?.trim())
    editErrors.value.trainerNote = 'Please provide a note explaining why you are modifying this plan.'
  return Object.keys(editErrors.value).length === 0
}

async function handleSave() {
  if (!validateEdit()) return
  trainerStore.clearError()

  const plan    = trainerStore.selectedPlan
  const payload = {
    planName:      editForm.value.planName.trim(),
    generatedPlan: editForm.value.generatedPlan.trim(),
    trainerNote:   editForm.value.trainerNote.trim()
  }

  let result
  if (trainerStore.planType === 'workout') {
    result = await trainerStore.editWorkoutPlan(plan.id, payload)
  } else {
    result = await trainerStore.editMealPlan(plan.id, payload)
  }

  if (result) {
    editMode.value = false
  }
}

// ── Athlete selection ─────────────────────────────────────────────────────────

async function handleSelectAthlete(athlete) {
  editMode.value = false
  await trainerStore.selectAthlete(athlete)
}

// ── Lifecycle ─────────────────────────────────────────────────────────────────

onMounted(() => {
  trainerStore.fetchAthletes()
})

// ── Helpers ───────────────────────────────────────────────────────────────────

function initials(name) {
  if (!name) return '?'
  return name.split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(w => w[0].toUpperCase())
    .join('')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('en-MY', {
    day: 'numeric', month: 'short', year: 'numeric'
  })
}

function goalLabel(goal) {
  const map = {
    STRENGTH: '💪 Strength', POWER: '⚡ Power',
    ENDURANCE: '🏃 Endurance', LEAN: '🔥 Lean',
    MUSCLE_GAIN: '💪 Muscle Gain', WEIGHT_LOSS: '⚖️ Weight Loss',
    PERFORMANCE: '🏆 Performance', MAINTENANCE: '🔄 Maintenance'
  }
  return map[goal] || goal
}

// ── Plan progress helpers ─────────────────────────────────────────────────────

/**
 * Estimate total checkable items for a plan.
 * Meal plan: mealsPerDay × 7 days.
 * Workout plan: sessionsPerWeek × 4 weeks (approx 1 month).
 */
function estimatedTotalItems(plan) {
  if (plan.mealsPerDay)      return (plan.mealsPerDay || 3) * 7
  if (plan.sessionsPerWeek)  return (plan.sessionsPerWeek || 3) * 4
  return Math.max(1, plan.completedItems?.length || 1)
}

function planProgressPct(plan) {
  const completed = plan.completedItems?.length || 0
  const total     = estimatedTotalItems(plan)
  if (total === 0) return 0
  return Math.min(100, Math.round((completed / total) * 100))
}

function phaseLabel(phase) {
  const map = {
    PRE_SEASON: '🔥 Pre-Season', IN_SEASON: '🏉 In-Season',
    OFF_SEASON: '💪 Off-Season', POST_SEASON: '🛌 Post-Season'
  }
  return map[phase] || phase || ''
}

function mdInlineFormat(s) {
  return s
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
}

function renderMarkdown(text) {
  if (!text) return ''
  let escaped = text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  // tables — extract into placeholder tokens first so the later
  // newline-sensitive paragraph pass below can't inject stray <br/>
  // tags inside the <table>, which breaks rendering into fragmented blocks.
  const tables = []
  escaped = escaped.replace(/(^\|.+\|[ \t]*$\n?)+/gm, (block) => {
    const lines = block.trim().split('\n')
    const rows = lines
      .map(line => line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(c => c.trim()))
      .filter(cells => !cells.every(c => /^[-:\s]+$/.test(c)))
    if (rows.length === 0) return block
    const [headerRow, ...bodyRows] = rows
    const thead = '<thead><tr>' + headerRow.map(c => `<th>${mdInlineFormat(c)}</th>`).join('') + '</tr></thead>'
    const tbody = '<tbody>' + bodyRows.map(r => '<tr>' + r.map(c => `<td>${mdInlineFormat(c)}</td>`).join('') + '</tr>').join('') + '</tbody>'
    const token = `@@TABLE_${tables.length}@@`
    tables.push(`<div class="md-table-wrap"><table class="md-table">${thead}${tbody}</table></div>`)
    return token + '\n'
  })

  let html = escaped
    .replace(/^#{3}\s+(.+)$/gm, '<h3>$1</h3>')
    .replace(/^#{2}\s+(.+)$/gm, '<h2>$1</h2>')
    .replace(/^#{1}\s+(.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    // lists
    .replace(/^[-*]\s+(.+)$/gm, '<li>$1</li>')
    .replace(/(<li>[\s\S]+?<\/li>)(?=\n(?!<li>)|\n*$)/g, '<ul>$1</ul>')
    // paragraphs
    .replace(/\n{2,}/g, '</p><p>')
    .replace(/\n/g, '<br/>')

  html = html.replace(/@@TABLE_(\d+)@@/g, (_, i) => tables[Number(i)])
  return `<p>${html}</p>`
}
</script>

<style scoped>
/* ── Layout ────────────────────────────────────────────────────────────────── */
.trainer-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  height: 100%;
}

.page-header {}
.page-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 32px; font-weight: 700;
  color: var(--color-text);
  margin: 0 0 4px;
}
.page-subtitle { color: var(--color-muted); margin: 0; font-size: 14px; }

.trainer-layout {
  display: grid;
  grid-template-columns: 260px 240px 1fr;
  gap: 16px;
  min-height: 0;
  flex: 1;
}

/* ── Panels ────────────────────────────────────────────────────────────────── */
.panel {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 500px;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.panel-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 16px; font-weight: 700;
  color: var(--color-text);
  margin: 0;
  flex: 1;
}

.count-badge {
  background: var(--color-green-light);
  color: #000;
  font-size: 11px; font-weight: 700;
  padding: 2px 7px;
  border-radius: 10px;
}

.athlete-selected-name {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 15px; font-weight: 700;
  color: var(--color-green-light);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── Search ────────────────────────────────────────────────────────────────── */
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.search-icon { font-size: 13px; opacity: 0.6; }
.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--color-text);
  font-size: 13px;
}
.search-input::placeholder { color: var(--color-muted); }

/* ── Loading / Empty / Placeholder ─────────────────────────────────────────── */
.panel-loading, .panel-empty, .panel-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex: 1;
  padding: 32px 16px;
  color: var(--color-muted);
  font-size: 13px;
  text-align: center;
}
.empty-icon, .placeholder-icon { font-size: 32px; opacity: 0.5; }
.panel-placeholder p, .panel-empty p { margin: 0; }

.spinner {
  width: 24px; height: 24px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-green-light);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Athlete list ──────────────────────────────────────────────────────────── */
.athlete-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.athlete-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  padding: 10px 10px;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s, border-color 0.15s;
  color: var(--color-text);
}
.athlete-item:hover { background: var(--color-bg-3); }
.athlete-item.active {
  background: rgba(180, 255, 0, 0.08);
  border-color: var(--color-green-light);
}

.athlete-avatar {
  width: 36px; height: 36px;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700;
  color: var(--color-green-light);
  flex-shrink: 0;
}

.athlete-info { flex: 1; min-width: 0; }
.athlete-name {
  font-size: 13px; font-weight: 600;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.athlete-meta {
  display: flex; align-items: center; gap: 6px;
  margin-top: 2px;
}
.pos-tag {
  font-size: 10px;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  padding: 1px 5px; border-radius: 4px;
  color: var(--color-muted);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  max-width: 90px;
}
.plan-counts { font-size: 10px; color: var(--color-muted); white-space: nowrap; }
.chevron { font-size: 18px; color: var(--color-muted); }

/* ── Type tabs ─────────────────────────────────────────────────────────────── */
.type-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.type-tab {
  flex: 1;
  background: transparent;
  border: none; border-bottom: 2px solid transparent;
  padding: 10px 8px;
  font-size: 12px; font-weight: 600;
  color: var(--color-muted);
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
  display: flex; align-items: center; justify-content: center; gap: 5px;
}
.type-tab.active { color: var(--color-green-light); border-bottom-color: var(--color-green-light); }
.tab-count {
  font-size: 10px;
  background: var(--color-bg-3);
  padding: 1px 5px; border-radius: 8px;
}

/* ── Plan list ─────────────────────────────────────────────────────────────── */
.plan-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.plan-item {
  width: 100%;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  color: var(--color-text);
}
.plan-item:hover { background: var(--color-bg-3); }
.plan-item.active {
  background: rgba(180, 255, 0, 0.08);
  border-color: var(--color-green-light);
}
.plan-item.has-note { border-left: 3px solid #f59e0b; }

.plan-item-name {
  font-size: 13px; font-weight: 600;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin-bottom: 4px;
}
.plan-item-meta {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; color: var(--color-muted);
}
.active-pill {
  background: rgba(180, 255, 0, 0.15);
  color: var(--color-green-light);
  font-size: 10px; font-weight: 700;
  padding: 1px 5px; border-radius: 4px;
}
.note-pill {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  font-size: 10px; font-weight: 700;
  padding: 1px 5px; border-radius: 4px;
}

/* ── Viewer ────────────────────────────────────────────────────────────────── */
.panel-viewer { overflow-y: auto; }

.viewer-header {
  padding: 16px 20px 12px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.viewer-title-row {
  display: flex; align-items: flex-start; gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.viewer-plan-name {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 20px; font-weight: 700;
  color: var(--color-text);
  margin: 0;
  flex: 1;
}
.viewer-actions { display: flex; gap: 8px; flex-shrink: 0; }

.plan-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.chip {
  font-size: 11px;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  padding: 3px 8px; border-radius: 20px;
  color: var(--color-muted);
}
.chip-active {
  background: rgba(180, 255, 0, 0.1);
  border-color: var(--color-green-light);
  color: var(--color-green-light);
}

/* ── Plan progress ─────────────────────────────────────────────────────────── */
.progress-section {
  margin-top: 12px;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}
.progress-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px;
}
.progress-title  { font-size: 12px; font-weight: 700; color: var(--color-text); }
.progress-fraction { font-size: 11px; color: var(--color-muted); }
.progress-track  {
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}
.progress-fill   {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s ease;
}
.fill-low  { background: #ef4444; }
.fill-mid  { background: #f59e0b; }
.fill-high { background: var(--color-green-light); }
.progress-pct-label { font-size: 11px; color: var(--color-muted); margin-top: 5px; text-align: right; }

/* ── Trainer note banner ───────────────────────────────────────────────────── */
.trainer-note-banner {
  margin-top: 12px;
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}
.note-banner-header {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; font-weight: 700;
  color: #f59e0b;
  margin-bottom: 6px;
}
.note-by { font-weight: 400; color: var(--color-muted); }
.note-banner-text { margin: 0; font-size: 13px; color: var(--color-text); line-height: 1.5; }

/* ── Alerts ────────────────────────────────────────────────────────────────── */
.alert {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  margin: 12px 20px 0;
}
.alert-error   { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); color: #ef4444; }
.alert-success { background: rgba(180,255,0,0.1); border: 1px solid rgba(180,255,0,0.3); color: var(--color-green-light); }
.alert-close { margin-left: auto; background: none; border: none; cursor: pointer; font-size: 16px; color: inherit; }

/* ── Plan content (read mode) ──────────────────────────────────────────────── */
.plan-content {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
}

.markdown-body { color: var(--color-text); line-height: 1.7; font-size: 14px; }
.markdown-body :deep(h1) { font-family: 'Barlow Condensed', sans-serif; font-size: 22px; font-weight: 700; color: var(--color-green-light); margin: 20px 0 8px; }
.markdown-body :deep(h2) { font-family: 'Barlow Condensed', sans-serif; font-size: 18px; font-weight: 700; color: var(--color-text); margin: 16px 0 6px; border-bottom: 1px solid var(--color-border); padding-bottom: 4px; }
.markdown-body :deep(h3) { font-size: 15px; font-weight: 600; color: var(--color-green-light); margin: 12px 0 4px; }
.markdown-body :deep(ul) { padding-left: 20px; margin: 6px 0; }
.markdown-body :deep(li) { margin-bottom: 3px; }
.markdown-body :deep(strong) { color: var(--color-text); }
.markdown-body :deep(code) { background: var(--color-bg-3); padding: 1px 5px; border-radius: 4px; font-size: 12px; }
.markdown-body :deep(.md-table) {
  width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 13px;
  border: 1px solid var(--color-border); border-radius: var(--radius-sm); overflow: hidden;
}
.markdown-body :deep(.md-table th) {
  padding: 8px 10px; text-align: left; font-weight: 600; color: var(--color-text);
  background: var(--color-surface); border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}
.markdown-body :deep(.md-table td) {
  padding: 7px 10px; border-top: 1px solid var(--color-border); color: var(--color-text-dim);
}
.markdown-body :deep(.md-table tbody tr:nth-child(even)) { background: var(--color-bg-2); }
.markdown-body :deep(.md-table tbody tr:hover td) { background: var(--color-bg-3); }

/* ── Edit form ─────────────────────────────────────────────────────────────── */
.edit-form {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label {
  font-size: 13px; font-weight: 600;
  color: var(--color-text);
  display: flex; align-items: center; gap: 8px;
}
.req { color: #ef4444; }
.label-hint { font-size: 11px; font-weight: 400; color: var(--color-muted); }

.form-input, .form-textarea {
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 9px 12px;
  color: var(--color-text);
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s;
  font-family: inherit;
}
.form-input:focus, .form-textarea:focus { border-color: var(--color-green-light); }
.has-error .form-input, .has-error .form-textarea { border-color: #ef4444; }
.field-error { font-size: 11px; color: #ef4444; margin: 0; }

.plan-textarea { min-height: 320px; resize: vertical; font-family: 'Courier New', monospace; font-size: 12px; line-height: 1.6; }
.note-textarea { min-height: 120px; resize: vertical; }

.note-footer {
  display: flex; justify-content: space-between; align-items: flex-start;
}
.char-count { font-size: 11px; color: var(--color-muted); flex-shrink: 0; }

/* ── Buttons ───────────────────────────────────────────────────────────────── */
.btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 13px; font-weight: 600;
  border: none; cursor: pointer;
  display: flex; align-items: center; gap: 6px;
  transition: opacity 0.15s, transform 0.1s;
  white-space: nowrap;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn:not(:disabled):hover { opacity: 0.88; }
.btn:not(:disabled):active { transform: scale(0.97); }

.btn-edit   { background: var(--color-bg-3); border: 1px solid var(--color-border); color: var(--color-text); }
.btn-save   { background: var(--color-green-light); color: #000; }
.btn-cancel { background: var(--color-bg-3); border: 1px solid var(--color-border); color: var(--color-muted); }

.btn-spinner {
  width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3);
  border-top-color: #000;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* ── Transitions ───────────────────────────────────────────────────────────── */
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ── Scrollbar ─────────────────────────────────────────────────────────────── */
.athlete-list::-webkit-scrollbar,
.plan-list::-webkit-scrollbar,
.panel-viewer::-webkit-scrollbar,
.plan-content::-webkit-scrollbar,
.edit-form::-webkit-scrollbar { width: 5px; }
.athlete-list::-webkit-scrollbar-track,
.plan-list::-webkit-scrollbar-track,
.panel-viewer::-webkit-scrollbar-track,
.plan-content::-webkit-scrollbar-track,
.edit-form::-webkit-scrollbar-track { background: transparent; }
.athlete-list::-webkit-scrollbar-thumb,
.plan-list::-webkit-scrollbar-thumb,
.panel-viewer::-webkit-scrollbar-thumb,
.plan-content::-webkit-scrollbar-thumb,
.edit-form::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 3px; }
</style>
