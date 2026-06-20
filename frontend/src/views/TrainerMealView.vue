<template>
  <div class="trainer-page">

    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">🥗 Meal Plan Management</h1>
        <p class="page-subtitle">View, edit and track your athletes' nutrition plans</p>
      </div>
    </div>

    <!-- ══════════════════════════════════════════
         PROGRESS CHART SECTION
         ══════════════════════════════════════════ -->
    <div class="chart-section" v-if="athletesWithMealData.length > 0">
      <div class="chart-header">
        <div>
          <h2 class="chart-title">📊 All Athletes — Meal Plan Progress</h2>
          <p class="chart-sub">Nutrition tracking completion across your squad</p>
        </div>
        <div class="chart-toggle">
          <button
            v-for="t in chartTypes" :key="t.value"
            class="chart-type-btn" :class="{ active: chartType === t.value }"
            @click="chartType = t.value">{{ t.label }}</button>
        </div>
      </div>

      <!-- Bar Chart (vertical SVG) -->
      <div v-if="chartType === 'bar'" class="bar-chart-wrap">
        <svg :width="barChartWidth" :height="barChartH" class="bar-svg">
          <!-- Gridlines + Y-axis labels -->
          <line v-for="g in [0,25,50,75,100]" :key="'bg'+g"
            :x1="barLeft" :y1="barY(g)" :x2="barChartWidth - barRight" :y2="barY(g)"
            stroke="var(--color-border)" stroke-width="1" stroke-dasharray="3,3"/>
          <text v-for="g in [0,25,50,75,100]" :key="'bl'+g"
            :x="barLeft - 6" :y="barY(g) + 4"
            font-size="10" fill="var(--color-muted)" text-anchor="end">{{ g }}%</text>
          <!-- Axis lines -->
          <line :x1="barLeft" :y1="barY(100) - 6" :x2="barLeft" :y2="barY(0)"
            stroke="var(--color-border)" stroke-width="1.5"/>
          <line :x1="barLeft" :y1="barY(0)" :x2="barChartWidth - barRight" :y2="barY(0)"
            stroke="var(--color-border)" stroke-width="1.5"/>
          <!-- Y-axis title -->
          <text :transform="`rotate(-90, 11, ${Math.round(barChartH / 2 - barBottom / 2)})`"
            x="11" :y="Math.round(barChartH / 2 - barBottom / 2)"
            font-size="10" fill="var(--color-muted)" text-anchor="middle">Progress (%)</text>
          <!-- Bars + value labels + athlete names -->
          <g v-for="(item, i) in athletesWithMealData" :key="item.athleteId">
            <rect
              :x="barX(i)" :y="barY(item.pct)"
              :width="barW" :height="Math.max(item.pct > 0 ? 3 : 0, barY(0) - barY(item.pct))"
              :fill="barColors[i % barColors.length]" rx="4" ry="4"/>
            <text v-if="item.pct > 0"
              :x="barX(i) + barW / 2" :y="barY(item.pct) - 5"
              font-size="10" font-weight="700" fill="var(--color-text)" text-anchor="middle">
              {{ item.pct }}%
            </text>
            <text :x="barX(i) + barW / 2" :y="barY(0) + 16"
              font-size="11" fill="var(--color-text)" text-anchor="middle">
              {{ item.shortName }}
            </text>
          </g>
        </svg>
      </div>

      <!-- Line Chart -->
      <div v-if="chartType === 'line'" class="line-chart-wrap">
        <svg :width="lineChartW" :height="lineChartH" class="line-svg">
          <line v-for="g in [0,25,50,75,100]" :key="g"
            :x1="lineLeft" :y1="lineY(g)" :x2="lineChartW - lineRight" :y2="lineY(g)"
            stroke="var(--color-border)" stroke-width="1"/>
          <text v-for="g in [0,25,50,75,100]" :key="'l'+g"
            :x="lineLeft - 6" :y="lineY(g) + 4"
            font-size="10" fill="var(--color-muted)" text-anchor="end">{{ g }}%</text>
          <polyline :points="linePoints" fill="none" stroke="#3b82f6" stroke-width="2.5" stroke-linejoin="round"/>
          <circle v-for="(item, i) in athletesWithMealData" :key="item.athleteId"
            :cx="lineX(i)" :cy="lineY(item.pct)" r="5"
            fill="#3b82f6" stroke="var(--color-bg-2)" stroke-width="2">
            <title>{{ item.name }}: {{ item.pct }}%</title>
          </circle>
          <text v-for="(item, i) in athletesWithMealData" :key="'xl'+item.athleteId"
            :x="lineX(i)" :y="lineChartH - 4"
            font-size="10" fill="var(--color-muted)" text-anchor="middle">{{ item.shortName }}</text>
        </svg>
      </div>

      <!-- Pie Chart -->
      <div v-if="chartType === 'pie'" class="pie-chart-wrap">
        <svg :width="pieSize" :height="pieSize" :viewBox="`0 0 ${pieSize} ${pieSize}`" class="pie-svg">
          <g :transform="`translate(${pieSize/2},${pieSize/2})`">
            <path v-for="(slice, i) in pieSlices" :key="i"
              :d="slice.d" :fill="pieColors[i % pieColors.length]"
              stroke="var(--color-bg-2)" stroke-width="2">
              <title>{{ slice.name }}: {{ slice.pct }}%</title>
            </path>
            <circle cx="0" cy="0" r="50" fill="var(--color-bg-2)"/>
            <text x="0" y="-6" font-size="13" font-weight="700" fill="var(--color-text)" text-anchor="middle">Squad</text>
            <text x="0" y="10" font-size="11" fill="var(--color-muted)" text-anchor="middle">Avg {{ avgPct }}%</text>
          </g>
        </svg>
        <div class="pie-legend">
          <div v-for="(item, i) in athletesWithMealData" :key="item.athleteId" class="legend-item">
            <span class="legend-dot" :style="{ background: pieColors[i % pieColors.length] }"></span>
            <span class="legend-name">{{ item.name }}</span>
            <span class="legend-pct">{{ item.pct }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 3-Panel Layout -->
    <div class="trainer-layout">

      <!-- PANEL 1: Athlete List -->
      <div class="panel panel-athletes">
        <div class="panel-header">
          <h2 class="panel-title">Athletes</h2>
          <span class="count-badge">{{ trainerStore.athletes.length }}</span>
        </div>
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input v-model="athleteSearch" type="text" class="search-input" placeholder="Search athletes..." />
        </div>
        <div v-if="trainerStore.loadingAthletes" class="panel-loading">
          <div class="spinner"></div><span>Loading athletes…</span>
        </div>
        <div v-else-if="filteredAthletes.length === 0" class="panel-empty">
          <span class="empty-icon">👥</span><p>No athletes found</p>
        </div>
        <div v-else class="athlete-list">
          <button
            v-for="athlete in filteredAthletes" :key="athlete.id"
            class="athlete-item" :class="{ active: trainerStore.selectedAthlete?.id === athlete.id }"
            @click="handleSelectAthlete(athlete)">
            <div class="athlete-avatar">{{ initials(athlete.fullName || athlete.username) }}</div>
            <div class="athlete-info">
              <div class="athlete-name">{{ athlete.fullName || athlete.username }}</div>
              <div class="athlete-meta">
                <span v-if="athlete.rugbyPosition" class="pos-tag">{{ athlete.rugbyPosition }}</span>
                <span class="plan-counts">🥗 {{ athlete.mealPlanCount }}</span>
              </div>
            </div>
            <span class="chevron">›</span>
          </button>
        </div>
      </div>

      <!-- PANEL 2: Meal Plan List -->
      <div class="panel panel-plans">
        <div v-if="!trainerStore.selectedAthlete" class="panel-placeholder">
          <span class="placeholder-icon">👈</span><p>Select an athlete</p>
        </div>
        <template v-else>
          <div class="panel-header">
            <div class="athlete-selected-name">
              {{ trainerStore.selectedAthlete.fullName || trainerStore.selectedAthlete.username }}
            </div>
          </div>
          <div v-if="trainerStore.loadingPlans" class="panel-loading">
            <div class="spinner"></div><span>Loading plans…</span>
          </div>
          <div v-else-if="trainerStore.mealPlans.length === 0" class="panel-empty">
            <span class="empty-icon">📋</span><p>No meal plans yet</p>
          </div>
          <div v-else class="plan-list">
            <button
              v-for="plan in trainerStore.mealPlans" :key="plan.id"
              class="plan-item"
              :class="{ active: trainerStore.selectedPlan?.id === plan.id, 'has-note': plan.trainerNote }"
              @click="trainerStore.selectPlan(plan, 'meal')">
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

      <!-- PANEL 3: Plan Viewer / Editor -->
      <div class="panel panel-viewer">
        <div v-if="!trainerStore.selectedPlan" class="panel-placeholder">
          <span class="placeholder-icon">📄</span><p>Select a plan to view or edit it</p>
        </div>
        <template v-else>
          <div class="viewer-header">
            <div class="viewer-title-row">
              <h3 class="viewer-plan-name">{{ trainerStore.selectedPlan.planName }}</h3>
              <div class="viewer-actions">
                <button v-if="!editMode" class="btn btn-edit" @click="startEdit">✏️ Edit Plan</button>
                <template v-else>
                  <button class="btn btn-save" :disabled="trainerStore.saving" @click="handleSave">
                    <span v-if="trainerStore.saving" class="btn-spinner"></span>
                    {{ trainerStore.saving ? 'Saving…' : '✅ Save Changes' }}
                  </button>
                  <button class="btn btn-cancel" :disabled="trainerStore.saving" @click="cancelEdit">Cancel</button>
                </template>
              </div>
            </div>
            <div class="plan-chips">
              <span v-if="trainerStore.selectedPlan.rugbyPosition" class="chip">🏉 {{ trainerStore.selectedPlan.rugbyPosition }}</span>
              <span v-if="trainerStore.selectedPlan.goals && trainerStore.selectedPlan.goals.length" class="chip">
                🎯 {{ trainerStore.selectedPlan.goals.map(g => goalLabel(g)).join(' + ') }}
              </span>
              <span v-else-if="trainerStore.selectedPlan.goal" class="chip">🎯 {{ goalLabel(trainerStore.selectedPlan.goal) }}</span>
              <span v-if="trainerStore.selectedPlan.isActive" class="chip chip-active">✅ ACTIVE</span>
            </div>
            <!-- Progress bar -->
            <div v-if="!editMode && trainerStore.selectedPlan.completedItems" class="progress-section">
              <div class="progress-header">
                <span class="progress-title">📊 Athlete Progress</span>
                <span class="progress-fraction">
                  {{ trainerStore.selectedPlan.completedItems.length }}
                  / {{ estimatedTotalItems(trainerStore.selectedPlan) }} items completed
                </span>
              </div>
              <div class="progress-track">
                <div class="progress-fill"
                  :style="{ width: planProgressPct(trainerStore.selectedPlan) + '%' }"
                  :class="fillClass(planProgressPct(trainerStore.selectedPlan))"></div>
              </div>
              <div class="progress-pct-label">{{ planProgressPct(trainerStore.selectedPlan) }}% complete</div>
            </div>
            <div v-if="trainerStore.selectedPlan.trainerNote && !editMode" class="trainer-note-banner">
              <div class="note-banner-header">
                <span>✏️ Trainer Note</span>
                <span v-if="trainerStore.selectedPlan.lastEditedBy" class="note-by">by {{ trainerStore.selectedPlan.lastEditedBy }}</span>
              </div>
              <p class="note-banner-text">{{ trainerStore.selectedPlan.trainerNote }}</p>
            </div>
          </div>

          <transition name="fade">
            <div v-if="trainerStore.saveSuccess" class="alert alert-success">
              ✅ Plan updated successfully.
              <button class="alert-close" @click="trainerStore.clearSaveSuccess()">×</button>
            </div>
          </transition>
          <transition name="fade">
            <div v-if="trainerStore.error" class="alert alert-error">
              ✕ {{ trainerStore.error }}
              <button class="alert-close" @click="trainerStore.clearError()">×</button>
            </div>
          </transition>

          <div v-if="!editMode" class="plan-content">
            <div class="markdown-body" v-html="renderMarkdown(trainerStore.selectedPlan.generatedPlan)"></div>
          </div>

          <div v-else class="edit-form">
            <div class="form-group">
              <label class="form-label">Plan Name</label>
              <input v-model="editForm.planName" type="text" class="form-input" placeholder="Plan name" />
              <p v-if="editErrors.planName" class="field-error">{{ editErrors.planName }}</p>
            </div>
            <div class="form-group">
              <label class="form-label">Plan Content</label>
              <textarea v-model="editForm.generatedPlan" class="form-textarea plan-textarea" placeholder="Edit the plan content (supports Markdown)"></textarea>
              <p v-if="editErrors.generatedPlan" class="field-error">{{ editErrors.generatedPlan }}</p>
            </div>
            <div class="form-group" :class="{ 'has-error': editErrors.trainerNote }">
              <label class="form-label">
                Trainer Note <span class="req">*</span>
                <span class="label-hint">Required — explain why you are modifying this plan</span>
              </label>
              <textarea v-model="editForm.trainerNote" class="form-textarea note-textarea"
                placeholder="e.g. Adjusted calorie targets to support the athlete's weight cut."
                maxlength="1000"></textarea>
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
    (a.fullName || '').toLowerCase().includes(q) ||
    (a.username || '').toLowerCase().includes(q) ||
    (a.rugbyPosition || '').toLowerCase().includes(q)
  )
})

// ── Edit mode state ───────────────────────────────────────────────────────────
const editMode   = ref(false)
const editForm   = ref({ planName: '', generatedPlan: '', trainerNote: '' })
const editErrors = ref({})

function startEdit() {
  const plan = trainerStore.selectedPlan
  editForm.value = { planName: plan.planName || '', generatedPlan: plan.generatedPlan || '', trainerNote: '' }
  editErrors.value = {}
  editMode.value = true
  trainerStore.clearError()
  trainerStore.clearSaveSuccess()
}
function cancelEdit() { editMode.value = false; editErrors.value = {}; trainerStore.clearError() }
function validateEdit() {
  editErrors.value = {}
  if (!editForm.value.planName?.trim())      editErrors.value.planName = 'Plan name is required.'
  if (!editForm.value.generatedPlan?.trim()) editErrors.value.generatedPlan = 'Plan content is required.'
  if (!editForm.value.trainerNote?.trim())   editErrors.value.trainerNote = 'Please provide a note explaining your changes.'
  return Object.keys(editErrors.value).length === 0
}
async function handleSave() {
  if (!validateEdit()) return
  trainerStore.clearError()
  const payload = { planName: editForm.value.planName.trim(), generatedPlan: editForm.value.generatedPlan.trim(), trainerNote: editForm.value.trainerNote.trim() }
  const result = await trainerStore.editMealPlan(trainerStore.selectedPlan.id, payload)
  if (result) editMode.value = false
}
async function handleSelectAthlete(athlete) {
  editMode.value = false
  await trainerStore.selectAthlete(athlete)
}

onMounted(() => trainerStore.fetchAthletes())

// ── Helpers ───────────────────────────────────────────────────────────────────
function initials(name) {
  if (!name) return '?'
  return name.split(' ').filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join('')
}
function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('en-MY', { day: 'numeric', month: 'short', year: 'numeric' })
}
function goalLabel(goal) {
  const map = { MUSCLE_GAIN: '💪 Muscle Gain', WEIGHT_LOSS: '⚖️ Weight Loss', PERFORMANCE: '🏆 Performance', MAINTENANCE: '🔄 Maintenance', MAINTAIN: '🔄 Maintain' }
  return map[goal] || goal
}
function estimatedTotalItems(plan) {
  if (plan.mealsPerDay) return (plan.mealsPerDay || 3) * 7
  return Math.max(1, plan.completedItems?.length || 1)
}
function planProgressPct(plan) {
  const completed = plan.completedItems?.length || 0
  const total = estimatedTotalItems(plan)
  if (total === 0) return 0
  return Math.min(100, Math.round((completed / total) * 100))
}
function fillClass(pct) {
  if (pct < 34) return 'fill-low'
  if (pct < 75) return 'fill-mid'
  return 'fill-high'
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

  // Pull out markdown table blocks first so later, newline-sensitive
  // replacements (the \n -> <br/> pass below) can't inject stray <br/>
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
    tables.push(`<table class="md-table">${thead}${tbody}</table>`)
    return token + '\n'
  })

  let html = escaped
    .replace(/^#{3}\s+(.+)$/gm, '<h3>$1</h3>')
    .replace(/^#{2}\s+(.+)$/gm, '<h2>$1</h2>')
    .replace(/^#{1}\s+(.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/^[-*]\s+(.+)$/gm, '<li>$1</li>')
    .replace(/(<li>[\s\S]+?<\/li>)(?=\n(?!<li>)|\n*$)/g, '<ul>$1</ul>')
    .replace(/\n{2,}/g, '</p><p>').replace(/\n/g, '<br/>')

  html = html.replace(/@@TABLE_(\d+)@@/g, (_, i) => tables[Number(i)])
  return `<p>${html}</p>`
}

// ── Progress Chart ────────────────────────────────────────────────────────────
const chartType = ref('pie')
const chartTypes = [
  { value: 'pie',  label: '🥧 Pie'  },
  { value: 'bar',  label: '📊 Bar'  },
  { value: 'line', label: '📈 Line' }
]

const athletesWithMealData = computed(() => {
  return trainerStore.athletes.map(a => ({
    athleteId: a.id,
    name: a.fullName || a.username,
    shortName: (a.fullName || a.username).split(' ')[0].slice(0, 8),
    pct: a.mealProgress != null ? a.mealProgress : 0
  })).filter(a => a.pct > 0 || trainerStore.athletes.length <= 10)
})

const avgPct = computed(() => {
  if (!athletesWithMealData.value.length) return 0
  return Math.round(athletesWithMealData.value.reduce((s, a) => s + a.pct, 0) / athletesWithMealData.value.length)
})

// Line chart geometry
const lineChartW = 600; const lineChartH = 200
const lineLeft   = 40;  const lineRight  = 20; const linePad = 24
function lineX(i) {
  const n = athletesWithMealData.value.length
  if (n <= 1) return lineLeft + (lineChartW - lineLeft - lineRight) / 2
  return lineLeft + i * (lineChartW - lineLeft - lineRight - linePad * 2) / (n - 1) + linePad
}
function lineY(pct) {
  const topPad = 16; const botPad = 28
  return topPad + (100 - pct) * (lineChartH - topPad - botPad) / 100
}
const linePoints = computed(() =>
  athletesWithMealData.value.map((a, i) => `${lineX(i)},${lineY(a.pct)}`).join(' ')
)

// Bar chart geometry (vertical SVG)
const barChartH = 240
const barTop    = 20
const barBottom = 40
const barLeft   = 48
const barRight  = 20
const barW      = 44
const barGap    = 16
const barColors = ['#3b82f6','#b4ff00','#f59e0b','#ec4899','#8b5cf6','#10b981','#f97316','#06b6d4']
const barChartWidth = computed(() => {
  const n = athletesWithMealData.value.length
  return Math.max(320, barLeft + barRight + n * (barW + barGap) + barGap)
})
function barX(i)   { return barLeft + barGap / 2 + i * (barW + barGap) }
function barY(pct) {
  const h = barChartH - barTop - barBottom
  return barTop + (100 - pct) * h / 100
}

// Pie chart geometry
const pieSize = 200; const pieRadius = 90
const pieColors = ['#3b82f6','#b4ff00','#f59e0b','#ec4899','#8b5cf6','#10b981','#f97316','#06b6d4']
const pieSlices = computed(() => {
  const data = athletesWithMealData.value
  if (!data.length) return []
  const total = data.reduce((s, a) => s + (a.pct || 1), 0)
  let angle = -Math.PI / 2
  return data.map(a => {
    const share = ((a.pct || 1) / total) * 2 * Math.PI
    const x1 = Math.cos(angle) * pieRadius; const y1 = Math.sin(angle) * pieRadius
    angle += share
    const x2 = Math.cos(angle) * pieRadius; const y2 = Math.sin(angle) * pieRadius
    const large = share > Math.PI ? 1 : 0
    return { d: `M0,0 L${x1},${y1} A${pieRadius},${pieRadius} 0 ${large} 1 ${x2},${y2} Z`, name: a.name, pct: a.pct }
  })
})
</script>

<style scoped>
.trainer-page { display: flex; flex-direction: column; gap: 24px; }
.page-title { font-family: 'Barlow Condensed', sans-serif; font-size: 32px; font-weight: 700; color: var(--color-text); margin: 0 0 4px; }
.page-subtitle { color: var(--color-muted); margin: 0; font-size: 14px; }
.chart-section { background: var(--color-bg-2); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px 24px; display: flex; flex-direction: column; gap: 16px; }
.chart-header { display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.chart-title { font-family: 'Barlow Condensed', sans-serif; font-size: 20px; font-weight: 700; color: var(--color-text); margin: 0 0 4px; }
.chart-sub { color: var(--color-muted); font-size: 13px; margin: 0; }
.chart-toggle { display: flex; gap: 6px; }
.chart-type-btn { padding: 6px 14px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; background: var(--color-bg-3); border: 1px solid var(--color-border); color: var(--color-muted); cursor: pointer; transition: background 0.15s; }
.chart-type-btn.active { background: rgba(59,130,246,0.15); border-color: #3b82f6; color: #3b82f6; }
.bar-chart-wrap { overflow-x: auto; }
.bar-svg { display: block; min-width: 300px; }
.line-chart-wrap { overflow-x: auto; }
.line-svg { display: block; min-width: 300px; }
.pie-chart-wrap { display: flex; align-items: center; gap: 32px; flex-wrap: wrap; }
.pie-legend { display: flex; flex-direction: column; gap: 8px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.legend-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.legend-name { flex: 1; color: var(--color-text); }
.legend-pct { font-weight: 700; color: var(--color-text); }
.trainer-layout { display: grid; grid-template-columns: 260px 240px 1fr; gap: 16px; min-height: 0; flex: 1; }
.panel { background: var(--color-bg-2); border: 1px solid var(--color-border); border-radius: var(--radius-lg); display: flex; flex-direction: column; overflow: hidden; min-height: 500px; }
.panel-header { display: flex; align-items: center; gap: 8px; padding: 16px 16px 12px; border-bottom: 1px solid var(--color-border); flex-shrink: 0; }
.panel-title { font-family: 'Barlow Condensed', sans-serif; font-size: 16px; font-weight: 700; color: var(--color-text); margin: 0; flex: 1; }
.count-badge { background: #3b82f6; color: #fff; font-size: 11px; font-weight: 700; padding: 2px 7px; border-radius: 10px; }
.athlete-selected-name { font-family: 'Barlow Condensed', sans-serif; font-size: 15px; font-weight: 700; color: #3b82f6; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border-bottom: 1px solid var(--color-border); flex-shrink: 0; }
.search-icon { font-size: 13px; opacity: 0.6; }
.search-input { flex: 1; background: transparent; border: none; outline: none; color: var(--color-text); font-size: 13px; }
.search-input::placeholder { color: var(--color-muted); }
.panel-loading, .panel-empty, .panel-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; flex: 1; padding: 32px 16px; color: var(--color-muted); font-size: 13px; text-align: center; }
.empty-icon, .placeholder-icon { font-size: 32px; opacity: 0.5; }
.panel-placeholder p, .panel-empty p { margin: 0; }
.spinner { width: 24px; height: 24px; border: 3px solid var(--color-border); border-top-color: #3b82f6; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.athlete-list { flex: 1; overflow-y: auto; padding: 8px; display: flex; flex-direction: column; gap: 4px; }
.athlete-item { display: flex; align-items: center; gap: 10px; width: 100%; background: transparent; border: 1px solid transparent; border-radius: var(--radius-md); padding: 10px; cursor: pointer; text-align: left; transition: background 0.15s; color: var(--color-text); }
.athlete-item:hover { background: var(--color-bg-3); }
.athlete-item.active { background: rgba(59,130,246,0.08); border-color: #3b82f6; }
.athlete-avatar { width: 36px; height: 36px; background: var(--color-bg-3); border: 1px solid var(--color-border); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; color: #3b82f6; flex-shrink: 0; }
.athlete-info { flex: 1; min-width: 0; }
.athlete-name { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.athlete-meta { display: flex; align-items: center; gap: 6px; margin-top: 2px; }
.pos-tag { font-size: 10px; background: var(--color-bg-3); border: 1px solid var(--color-border); padding: 1px 5px; border-radius: 4px; color: var(--color-muted); }
.plan-counts { font-size: 10px; color: var(--color-muted); }
.chevron { font-size: 18px; color: var(--color-muted); }
.plan-list { flex: 1; overflow-y: auto; padding: 8px; display: flex; flex-direction: column; gap: 4px; }
.plan-item { width: 100%; background: transparent; border: 1px solid transparent; border-radius: var(--radius-md); padding: 10px 12px; text-align: left; cursor: pointer; transition: background 0.15s; color: var(--color-text); }
.plan-item:hover { background: var(--color-bg-3); }
.plan-item.active { background: rgba(59,130,246,0.08); border-color: #3b82f6; }
.plan-item.has-note { border-left: 3px solid #f59e0b; }
.plan-item-name { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 4px; }
.plan-item-meta { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--color-muted); }
.active-pill { background: rgba(59,130,246,0.15); color: #3b82f6; font-size: 10px; font-weight: 700; padding: 1px 5px; border-radius: 4px; }
.note-pill { background: rgba(245,158,11,0.15); color: #f59e0b; font-size: 10px; font-weight: 700; padding: 1px 5px; border-radius: 4px; }
.panel-viewer { overflow-y: auto; }
.viewer-header { padding: 16px 20px 12px; border-bottom: 1px solid var(--color-border); flex-shrink: 0; }
.viewer-title-row { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 10px; flex-wrap: wrap; }
.viewer-plan-name { font-family: 'Barlow Condensed', sans-serif; font-size: 20px; font-weight: 700; color: var(--color-text); margin: 0; flex: 1; }
.viewer-actions { display: flex; gap: 8px; flex-shrink: 0; }
.plan-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.chip { font-size: 11px; background: var(--color-bg-3); border: 1px solid var(--color-border); padding: 3px 8px; border-radius: 20px; color: var(--color-muted); }
.chip-active { background: rgba(59,130,246,0.1); border-color: #3b82f6; color: #3b82f6; }
.progress-section { margin-top: 12px; background: var(--color-bg-3); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 12px 14px; }
.progress-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.progress-title { font-size: 12px; font-weight: 700; color: var(--color-text); }
.progress-fraction { font-size: 11px; color: var(--color-muted); }
.progress-track { height: 8px; background: var(--color-border); border-radius: 4px; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 4px; transition: width 0.4s ease; }
.progress-pct-label { font-size: 11px; color: var(--color-muted); margin-top: 5px; text-align: right; }
.trainer-note-banner { margin-top: 12px; background: rgba(245,158,11,0.08); border: 1px solid rgba(245,158,11,0.3); border-radius: var(--radius-md); padding: 12px 14px; }
.note-banner-header { display: flex; align-items: center; justify-content: space-between; font-size: 12px; font-weight: 700; color: #f59e0b; margin-bottom: 6px; }
.note-by { font-weight: 400; color: var(--color-muted); }
.note-banner-text { margin: 0; font-size: 13px; color: var(--color-text); line-height: 1.5; }
.alert { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: var(--radius-md); font-size: 13px; margin: 12px 20px 0; }
.alert-error   { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); color: #ef4444; }
.alert-success { background: rgba(59,130,246,0.1); border: 1px solid rgba(59,130,246,0.3); color: #3b82f6; }
.alert-close { margin-left: auto; background: none; border: none; cursor: pointer; font-size: 16px; color: inherit; }
.plan-content { padding: 20px; flex: 1; overflow-y: auto; }
.markdown-body { color: var(--color-text); line-height: 1.7; font-size: 14px; }
.markdown-body :deep(h1) { font-family: 'Barlow Condensed', sans-serif; font-size: 22px; font-weight: 700; color: #3b82f6; margin: 20px 0 8px; }
.markdown-body :deep(h2) { font-family: 'Barlow Condensed', sans-serif; font-size: 18px; font-weight: 700; color: var(--color-text); margin: 16px 0 6px; border-bottom: 1px solid var(--color-border); padding-bottom: 4px; }
.markdown-body :deep(h3) { font-size: 15px; font-weight: 600; color: #3b82f6; margin: 12px 0 4px; }
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
.edit-form { padding: 20px; flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--color-text); display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.req { color: #ef4444; }
.label-hint { font-size: 11px; font-weight: 400; color: var(--color-muted); }
.form-input, .form-textarea { width: 100%; background: var(--color-bg-3); border: 1px solid var(--color-border); border-radius: var(--radius-sm); padding: 10px 12px; color: var(--color-text); font-size: 13px; font-family: inherit; transition: border-color 0.15s; box-sizing: border-box; }
.form-input:focus, .form-textarea:focus { outline: none; border-color: #3b82f6; }
.form-textarea { resize: vertical; }
.plan-textarea { min-height: 200px; font-family: monospace; font-size: 12px; }
.note-textarea { min-height: 80px; }
.note-footer { display: flex; align-items: center; justify-content: space-between; }
.char-count { font-size: 11px; color: var(--color-muted); }
.field-error { font-size: 12px; color: #ef4444; margin: 0; }
.btn { display: inline-flex; align-items: center; gap: 6px; padding: 7px 14px; border-radius: var(--radius-sm); font-size: 13px; font-weight: 500; font-family: inherit; cursor: pointer; border: none; transition: background 0.15s; }
.btn-edit { background: var(--color-bg-3); border: 1px solid var(--color-border); color: var(--color-text); }
.btn-edit:hover { background: var(--color-surface-2); }
.btn-save { background: #3b82f6; color: #fff; font-weight: 700; }
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-cancel { background: transparent; border: 1px solid var(--color-border); color: var(--color-muted); }
.btn-spinner { width: 12px; height: 12px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
