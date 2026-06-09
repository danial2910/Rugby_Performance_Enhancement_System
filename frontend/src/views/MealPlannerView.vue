<template>
  <div class="meal-page">

    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">Meal Planner</h1>
        <p class="page-subtitle">Generate a personalised 7-day rugby nutrition plan powered by AI</p>
      </div>
    </div>

    <div class="meal-layout">

      <!-- ══════════════════════════════════════════
           LEFT: Generate Form + Saved Plans
           ══════════════════════════════════════════ -->
      <div class="form-column">
        <div class="card">
          <div class="card-header">
            <span class="card-icon">🥗</span>
            <h2 class="card-title">Generate New Plan</h2>
          </div>

          <!-- Error banner -->
          <transition name="fade">
            <div v-if="mealStore.error" class="alert alert-error">
              <span>✕</span> {{ mealStore.error }}
              <button class="alert-close" @click="mealStore.clearError()">×</button>
            </div>
          </transition>

          <form @submit.prevent="handleGenerate" class="generate-form">

            <!-- Plan Name -->
            <div class="form-group">
              <label class="form-label">Plan Name <span class="optional-tag">optional</span></label>
              <input v-model="form.planName" type="text" class="form-input"
                placeholder='e.g. "Pre-Season Bulking Week"' />
            </div>

            <!-- Rugby Position -->
            <div class="form-group" :class="{ 'has-error': errors.rugbyPosition }">
              <label class="form-label">Rugby Position <span class="req">*</span></label>
              <select v-model="form.rugbyPosition" class="form-select" @change="clearError('rugbyPosition')">
                <option value="" disabled>Select your position</option>
                <optgroup label="Forwards">
                  <option>Prop (Loosehead)</option>
                  <option>Prop (Tighthead)</option>
                  <option>Hooker</option>
                  <option>Lock</option>
                  <option>Flanker (Openside)</option>
                  <option>Flanker (Blindside)</option>
                  <option>Number 8</option>
                </optgroup>
                <optgroup label="Backs">
                  <option>Scrum Half</option>
                  <option>Fly Half</option>
                  <option>Inside Centre</option>
                  <option>Outside Centre</option>
                  <option>Wing</option>
                  <option>Fullback</option>
                </optgroup>
              </select>
              <p v-if="errors.rugbyPosition" class="field-error">{{ errors.rugbyPosition }}</p>
            </div>

            <!-- Goals (multi-select) -->
            <div class="form-group" :class="{ 'has-error': errors.goals }">
              <label class="form-label">
                Nutrition Goals <span class="req">*</span>
                <span class="label-hint">Select one or more</span>
              </label>
              <div class="goal-selector">
                <button v-for="g in goals" :key="g.value" type="button"
                  class="goal-btn" :class="{ active: form.goals.includes(g.value) }"
                  @click="toggleGoal(g.value)">
                  <span class="goal-icon">{{ g.icon }}</span>
                  <span class="goal-label">{{ g.label }}</span>
                  <span v-if="form.goals.includes(g.value)" class="goal-tick">✓</span>
                </button>
              </div>
              <p v-if="errors.goals" class="field-error">{{ errors.goals }}</p>
            </div>

            <!-- Physical Stats -->
            <div class="form-row three-col">
              <div class="form-group" :class="{ 'has-error': errors.weight }">
                <label class="form-label">Weight (kg) <span class="req">*</span></label>
                <input v-model.number="form.weight" type="number" class="form-input"
                  placeholder="90" min="40" max="200" @input="clearError('weight')" />
                <p v-if="errors.weight" class="field-error">{{ errors.weight }}</p>
              </div>
              <div class="form-group" :class="{ 'has-error': errors.height }">
                <label class="form-label">Height (cm) <span class="req">*</span></label>
                <input v-model.number="form.height" type="number" class="form-input"
                  placeholder="180" min="140" max="220" @input="clearError('height')" />
                <p v-if="errors.height" class="field-error">{{ errors.height }}</p>
              </div>
              <div class="form-group" :class="{ 'has-error': errors.age }">
                <label class="form-label">Age <span class="req">*</span></label>
                <input v-model.number="form.age" type="number" class="form-input"
                  placeholder="22" min="15" max="60" @input="clearError('age')" />
                <p v-if="errors.age" class="field-error">{{ errors.age }}</p>
              </div>
            </div>

            <!-- Dietary Preference -->
            <div class="form-group" :class="{ 'has-error': errors.dietaryPreference }">
              <label class="form-label">Dietary Preference <span class="req">*</span></label>
              <div class="diet-selector">
                <button v-for="d in diets" :key="d.value" type="button"
                  class="diet-btn" :class="{ active: form.dietaryPreference === d.value }"
                  @click="form.dietaryPreference = d.value; clearError('dietaryPreference')">
                  {{ d.icon }} {{ d.label }}
                </button>
              </div>
              <p v-if="errors.dietaryPreference" class="field-error">{{ errors.dietaryPreference }}</p>
            </div>

            <!-- Meals Per Day -->
            <div class="form-group" :class="{ 'has-error': errors.mealsPerDay }">
              <label class="form-label">Meals Per Day <span class="req">*</span></label>
              <div class="sessions-selector">
                <button v-for="n in [3, 4, 5, 6]" :key="n" type="button"
                  class="session-btn" :class="{ active: form.mealsPerDay === n }"
                  @click="form.mealsPerDay = n; clearError('mealsPerDay')">
                  {{ n }}
                </button>
              </div>
              <p v-if="errors.mealsPerDay" class="field-error">{{ errors.mealsPerDay }}</p>
            </div>

            <!-- Activity Level -->
            <div class="form-group" :class="{ 'has-error': errors.activityLevel }">
              <label class="form-label">Activity Level <span class="req">*</span></label>
              <div class="goal-selector">
                <button v-for="a in activityLevels" :key="a.value" type="button"
                  class="goal-btn" :class="{ active: form.activityLevel === a.value }"
                  @click="form.activityLevel = a.value; clearError('activityLevel')">
                  <span class="goal-icon">{{ a.icon }}</span>
                  <span class="goal-label">{{ a.label }}</span>
                </button>
              </div>
              <p v-if="errors.activityLevel" class="field-error">{{ errors.activityLevel }}</p>
            </div>

            <!-- Training Phase + Meal Prep Time -->
            <div class="form-row">
              <div class="form-group" :class="{ 'has-error': errors.trainingPhase }">
                <label class="form-label">Training Phase <span class="req">*</span></label>
                <select v-model="form.trainingPhase" class="form-select" @change="clearError('trainingPhase')">
                  <option value="" disabled>Select phase</option>
                  <option value="PRE_SEASON">🔥 Pre-Season</option>
                  <option value="IN_SEASON">🏉 In-Season</option>
                  <option value="OFF_SEASON">💪 Off-Season</option>
                  <option value="POST_SEASON">🛌 Post-Season</option>
                </select>
                <p v-if="errors.trainingPhase" class="field-error">{{ errors.trainingPhase }}</p>
              </div>

              <div class="form-group" :class="{ 'has-error': errors.mealPrepTime }">
                <label class="form-label">Meal Prep Time <span class="req">*</span></label>
                <select v-model="form.mealPrepTime" class="form-select" @change="clearError('mealPrepTime')">
                  <option value="" disabled>Select prep time</option>
                  <option value="LOW">⚡ Quick (under 15 min)</option>
                  <option value="MEDIUM">🍳 Standard (15–30 min)</option>
                  <option value="HIGH">👨‍🍳 Full Cook (30+ min)</option>
                </select>
                <p v-if="errors.mealPrepTime" class="field-error">{{ errors.mealPrepTime }}</p>
              </div>
            </div>

            <!-- Target Weight -->
            <div class="form-group">
              <label class="form-label">Target Weight (kg) <span class="optional-tag">optional</span></label>
              <input v-model.number="form.targetWeight" type="number" class="form-input"
                placeholder="e.g. 95" min="40" max="200" />
              <p class="field-hint">Helps AI calibrate your calorie surplus or deficit.</p>
            </div>

            <!-- Allergies -->
            <div class="form-group">
              <label class="form-label">Food Allergies / Intolerances <span class="optional-tag">optional</span></label>
              <input v-model="form.allergies" type="text" class="form-input"
                placeholder='e.g. "peanuts, dairy, gluten"' />
              <p class="field-hint">AI will exclude these ingredients from all meals.</p>
            </div>

            <!-- Calorie estimate preview -->
            <div v-if="estimatedCalories" class="calorie-preview">
              <span class="calorie-icon">🔥</span>
              <div>
                <p class="calorie-label">Estimated Daily Target</p>
                <p class="calorie-value">~{{ estimatedCalories }} kcal/day</p>
              </div>
            </div>

            <!-- Submit -->
            <button type="submit" class="btn-generate" :disabled="mealStore.generating">
              <span v-if="mealStore.generating" class="spinner"></span>
              <span>{{ mealStore.generating ? 'Generating your plan...' : '🥗 Generate 7-Day Meal Plan' }}</span>
            </button>

            <p v-if="mealStore.generating" class="generating-hint">
              AI is building your 7-day personalised meal plan — this may take 30–90 seconds.
            </p>

          </form>
        </div>

        <!-- Saved Plans List -->
        <div class="card saved-plans-card">
          <div class="card-header">
            <span class="card-icon">📋</span>
            <h2 class="card-title">Saved Plans</h2>
            <span class="plan-count" v-if="mealStore.plans.length">{{ mealStore.plans.length }}</span>
          </div>

          <div v-if="mealStore.loadingPlans" class="loading-state">
            <div class="spinner-muted"></div>
            <p>Loading plans...</p>
          </div>

          <div v-else-if="mealStore.plans.length === 0" class="empty-state">
            <p>No saved meal plans yet. Generate your first plan above!</p>
          </div>

          <div v-else class="plans-list">
            <div v-for="plan in paginatedPlans" :key="plan.id"
              class="plan-item"
              :class="{ 'is-selected': mealStore.activePlan?.id === plan.id, 'is-current': plan.isActive }"
              @click="selectPlan(plan)">

              <!-- Active badge -->
              <div class="plan-item-badge" v-if="plan.isActive">✅</div>

              <div class="plan-item-info">
                <div class="plan-item-name-row">
                  <p class="plan-item-name">{{ plan.planName }}</p>
                  <span v-if="plan.isActive" class="badge-active">ACTIVE</span>
                </div>
                <p class="plan-item-meta">
                  {{ plan.rugbyPosition }} · {{ goalsLabel(plan.goals) }} · {{ dietLabel(plan.dietaryPreference) }}
                </p>
                <!-- Progress bar -->
                <div class="plan-item-progress" v-if="plan.completedItems && plan.completedItems.length > 0">
                  <div class="progress-bar-track">
                    <div class="progress-bar-fill" :style="{ width: progressPercent(plan) + '%' }"></div>
                  </div>
                  <span class="progress-label">{{ plan.completedItems.length }} meals logged</span>
                </div>
                <p class="plan-item-date">{{ formatDate(plan.createdAt) }}</p>
              </div>

              <div class="plan-item-actions">
                <button class="icon-btn" @click.stop="openCopyModal(plan)" title="Copy plan">📋</button>
                <button class="icon-btn icon-btn-danger" @click.stop="confirmDelete(plan)" title="Delete plan">🗑️</button>
              </div>
            </div>

            <!-- Pagination controls -->
            <div v-if="totalPages > 1" class="pagination">
              <button class="page-btn" :disabled="currentPage === 1" @click="prevPage">‹</button>
              <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
              <button class="page-btn" :disabled="currentPage === totalPages" @click="nextPage">›</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════════════════════════════════════
           RIGHT: Plan Display + Manage
           ══════════════════════════════════════════ -->
      <div class="plan-column">

        <!-- Empty state -->
        <div v-if="!mealStore.activePlan && !mealStore.generating" class="plan-empty">
          <div class="plan-empty-icon">🥗</div>
          <h3>No plan selected</h3>
          <p>Generate a new meal plan or select a saved plan from the left panel.</p>
        </div>

        <!-- Generating state -->
        <div v-if="mealStore.generating" class="plan-generating">
          <div class="ai-pulse"><span>🤖</span></div>
          <h3>AI is generating your meal plan...</h3>
          <p>
            Ollama <strong>llama3.2</strong> is building a personalised
            <strong>7-day</strong> nutrition plan for a
            <strong>{{ form.rugbyPosition || 'rugby player' }}</strong>
            following a <strong>{{ dietLabel(form.dietaryPreference) || '' }}</strong> diet.
          </p>
          <div class="progress-dots">
            <span></span><span></span><span></span>
          </div>
        </div>

        <!-- Plan content -->
        <div v-if="mealStore.activePlan && !mealStore.generating" class="plan-display card">

          <!-- Plan header -->
          <div class="plan-display-header">
            <div class="plan-header-left">
              <!-- Edit mode: editable title -->
              <input
                v-if="editMode"
                v-model="editForm.planName"
                class="form-input plan-name-input"
                placeholder="Plan name"
              />
              <h2 v-else class="plan-display-title">
                {{ mealStore.activePlan.planName }}
                <span v-if="mealStore.activePlan.isActive" class="title-active-badge">ACTIVE</span>
              </h2>

              <div class="plan-tags" v-if="!editMode">
                <span class="tag tag-position">🏉 {{ mealStore.activePlan.rugbyPosition }}</span>
                <span class="tag tag-goal">{{ goalsLabel(mealStore.activePlan.goals) }}</span>
                <span class="tag tag-diet">{{ dietLabel(mealStore.activePlan.dietaryPreference) }}</span>
                <span class="tag tag-meals">{{ mealStore.activePlan.mealsPerDay }} meals/day</span>
                <span class="tag tag-phase">{{ phaseLabel(mealStore.activePlan.trainingPhase) }}</span>
              </div>
              <p class="plan-stats-line" v-if="!editMode">
                {{ mealStore.activePlan.weight }}kg ·
                {{ mealStore.activePlan.height }}cm ·
                {{ mealStore.activePlan.age }}yo
                <span v-if="mealStore.activePlan.allergies"> · ⚠️ {{ mealStore.activePlan.allergies }}</span>
              </p>
            </div>

            <!-- Action buttons -->
            <div class="plan-header-actions">
              <button
                v-if="!editMode && !mealStore.activePlan.isActive"
                class="btn btn-activate"
                :disabled="activating"
                @click="handleActivate">
                <span v-if="activating" class="spinner spinner-sm"></span>
                <span v-else>📌 Set Active</span>
              </button>
              <span v-if="!editMode && mealStore.activePlan.isActive" class="active-indicator">
                📌 Currently Active
              </span>

              <button v-if="!editMode" class="btn btn-ghost" @click="startEdit">✏️ Edit</button>
              <button v-if="editMode" class="btn btn-save" :disabled="saving" @click="handleSaveEdit">
                <span v-if="saving" class="spinner spinner-sm"></span>
                <span v-else>💾 Save</span>
              </button>
              <button v-if="editMode" class="btn btn-ghost" @click="cancelEdit">Cancel</button>
            </div>
          </div>

          <!-- ── Tabs: Plan / Progress ───────────────────────── -->
          <div class="plan-tabs" v-if="!editMode">
            <button class="plan-tab" :class="{ active: activeTab === 'plan' }" @click="activeTab = 'plan'">
              📄 Plan
            </button>
            <button class="plan-tab" :class="{ active: activeTab === 'progress' }" @click="activeTab = 'progress'">
              ✅ Progress
              <span class="tab-badge" v-if="mealStore.activePlan.completedItems?.length">
                {{ mealStore.activePlan.completedItems.length }}
              </span>
            </button>
          </div>

          <!-- ── Plan Tab ───────────────────────────────────────── -->
          <div v-if="!editMode && activeTab === 'plan'" class="plan-body">
            <div class="plan-markdown" v-html="renderMarkdown(mealStore.activePlan.generatedPlan)"></div>
          </div>

          <!-- ── Edit Tab ───────────────────────────────────────── -->
          <div v-if="editMode" class="plan-body edit-body">
            <label class="form-label" style="margin-bottom:6px;display:block;">Plan Content (Markdown)</label>
            <textarea
              v-model="editForm.generatedPlan"
              class="plan-editor"
              spellcheck="false"
              placeholder="Edit your meal plan here..."></textarea>
            <p class="field-hint" style="margin-top:6px;">
              You can edit or customise any part of the AI-generated meal plan. Uses markdown format.
            </p>
          </div>

          <!-- ── Progress Tab ───────────────────────────────────── -->
          <div v-if="!editMode && activeTab === 'progress'" class="plan-body progress-body">

            <!-- Summary ring -->
            <div class="progress-summary">
              <div class="progress-ring-wrap">
                <svg class="progress-ring" viewBox="0 0 60 60">
                  <circle cx="30" cy="30" r="24" fill="none" stroke="var(--color-border)" stroke-width="6"/>
                  <circle cx="30" cy="30" r="24" fill="none"
                    stroke="var(--color-green)" stroke-width="6"
                    stroke-linecap="round"
                    :stroke-dasharray="150.8"
                    :stroke-dashoffset="150.8 - (150.8 * progressPercent(mealStore.activePlan) / 100)"
                    transform="rotate(-90 30 30)"
                    style="transition:stroke-dashoffset 0.4s ease"/>
                </svg>
                <span class="progress-ring-label">{{ progressPercent(mealStore.activePlan) }}%</span>
              </div>
              <div class="progress-summary-text">
                <p class="progress-summary-title">Weekly Nutrition Progress</p>
                <p class="progress-summary-sub">
                  {{ mealStore.activePlan.completedItems?.length || 0 }} of
                  {{ extractMealKeys(mealStore.activePlan.generatedPlan).length }} meals logged
                </p>
                <button
                  class="btn btn-ghost btn-sm reset-btn"
                  v-if="mealStore.activePlan.completedItems?.length"
                  @click="handleResetProgress">
                  🔄 Reset Progress
                </button>
              </div>
            </div>

            <!-- ── Weekly Nutrition Summary Table ──────────────────── -->
            <div class="nutrition-table-section">
              <h3 class="nutrition-table-title">📊 Weekly Nutrition Summary</h3>
              <div v-if="nutritionTableRows(mealStore.activePlan.generatedPlan).length > 0" class="nutrition-table-wrap">
                <table class="nutrition-table">
                  <thead>
                    <tr>
                      <th>Day</th>
                      <th>Calories</th>
                      <th>Protein</th>
                      <th>Carbs</th>
                      <th>Fat</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(row, i) in nutritionTableRows(mealStore.activePlan.generatedPlan)"
                      :key="i"
                      :class="{ 'row-total': row.isTotal }">
                      <td class="col-day">{{ row.day }}</td>
                      <td class="col-num">{{ row.calories }}</td>
                      <td class="col-num">{{ row.protein }}</td>
                      <td class="col-num">{{ row.carbs }}</td>
                      <td class="col-num">{{ row.fat }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="nutrition-table-empty">
                No nutrition summary table found. The AI plan may not have included macro data — try regenerating.
              </p>
            </div>

            <!-- Meal checklist grouped by day -->
            <div class="checklist-grouped">
              <div
                v-for="day in groupedMealKeys(mealStore.activePlan.generatedPlan)"
                :key="day.label"
                class="checklist-day">

                <div class="checklist-day-header">
                  <span class="checklist-day-title">{{ day.label }}</span>
                  <span class="checklist-day-count">
                    {{ day.meals.filter(m => isCompleted(m)).length }}/{{ day.meals.length }}
                  </span>
                </div>

                <div
                  v-for="meal in day.meals"
                  :key="meal"
                  class="checklist-item"
                  :class="{ completed: isCompleted(meal) }"
                  @click="toggleItem(meal)">
                  <div class="checklist-check">
                    <span v-if="isCompleted(meal)">✓</span>
                  </div>
                  <span class="checklist-label">{{ mealNameOnly(meal) }}</span>
                </div>
              </div>

              <p v-if="extractMealKeys(mealStore.activePlan.generatedPlan).length === 0"
                class="checklist-empty">
                No meal headings found in this plan. Try regenerating.
              </p>
            </div>

          </div>
        </div>
      </div>
    </div>

    <!-- Delete confirmation modal -->
    <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
      <div class="modal">
        <h3>Delete Meal Plan</h3>
        <p>Are you sure you want to delete <strong>"{{ deleteTarget.planName }}"</strong>? This cannot be undone.</p>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="deleteTarget = null">Cancel</button>
          <button class="btn btn-danger" @click="handleDelete">Delete</button>
        </div>
      </div>
    </div>

    <!-- Copy meal plan modal -->
    <div v-if="copySource" class="modal-overlay" @click.self="closeCopyModal">
      <div class="modal modal-copy">
        <h3>📋 Copy Meal Plan</h3>
        <p>Duplicating <strong>"{{ copySource.planName }}"</strong>. Enter a name for the new copy.</p>

        <!-- Plan picker -->
        <div class="copy-plan-list">
          <p class="copy-section-label">Or copy a different plan:</p>
          <div
            v-for="p in mealStore.plans"
            :key="p.id"
            class="copy-plan-item"
            :class="{ selected: copySource.id === p.id }"
            @click="selectCopySource(p)">
            <span class="copy-plan-name">{{ p.planName }}</span>
            <span class="copy-plan-meta">{{ p.rugbyPosition }} · {{ p.mealsPerDay }} meals/day</span>
          </div>
        </div>

        <div class="copy-name-row">
          <label class="form-label">New plan name</label>
          <input
            v-model="copyName"
            class="form-input"
            placeholder="e.g. In-Season Nutrition Copy"
            @keydown.enter="handleCopy"
            maxlength="80" />
          <p v-if="copyError" class="copy-error">{{ copyError }}</p>
        </div>

        <div class="modal-actions">
          <button class="btn btn-ghost" @click="closeCopyModal">Cancel</button>
          <button class="btn btn-primary" :disabled="copying" @click="handleCopy">
            <span v-if="copying" class="spinner-sm"></span>
            {{ copying ? 'Copying…' : 'Copy Plan' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMealStore }    from '@/stores/meal'
import { useProfileStore } from '@/stores/profile'

const mealStore    = useMealStore()
const profileStore = useProfileStore()

// ── Constants ────────────────────────────────────────────────────────────────
const goals = [
  { value: 'MUSCLE_GAIN', icon: '💪', label: 'Muscle Gain' },
  { value: 'WEIGHT_LOSS', icon: '🔥', label: 'Weight Loss' },
  { value: 'MAINTAIN',    icon: '⚖️', label: 'Maintain'   },
  { value: 'PERFORMANCE', icon: '⚡', label: 'Performance' }
]

const diets = [
  { value: 'HALAL',          icon: '🌙', label: 'Halal'          },
  { value: 'VEGETARIAN',     icon: '🥦', label: 'Vegetarian'     },
  { value: 'VEGAN',          icon: '🌱', label: 'Vegan'          },
  { value: 'NO_RESTRICTION', icon: '🍽️', label: 'No Restriction' }
]

const activityLevels = [
  { value: 'MODERATE', icon: '🚶', label: 'Moderate' },
  { value: 'ACTIVE',   icon: '🏃', label: 'Active'   },
  { value: 'EXTREME',  icon: '⚡', label: 'Extreme'  }
]

// ── Form state ───────────────────────────────────────────────────────────────
const form = ref({
  planName:          '',
  rugbyPosition:     '',
  goals:             [],
  weight:            null,
  height:            null,
  age:               null,
  dietaryPreference: '',
  allergies:         '',
  mealsPerDay:       4,
  activityLevel:     '',
  targetWeight:      null,
  trainingPhase:     '',
  mealPrepTime:      ''
})

const errors       = ref({})
const deleteTarget = ref(null)

// ── Copy modal state ──────────────────────────────────────────────────────────
const copySource = ref(null)
const copyName   = ref('')
const copying    = ref(false)
const copyError  = ref('')

// ── Tabs ─────────────────────────────────────────────────────────────────────
const activeTab = ref('plan')

// ── Edit mode ────────────────────────────────────────────────────────────────
const editMode   = ref(false)
const saving     = ref(false)
const activating = ref(false)
const editForm   = ref({ planName: '', generatedPlan: '' })

function startEdit() {
  editForm.value.planName      = mealStore.activePlan.planName
  editForm.value.generatedPlan = mealStore.activePlan.generatedPlan
  editMode.value = true
}

function cancelEdit() { editMode.value = false }

async function handleSaveEdit() {
  if (!editForm.value.planName.trim() || !editForm.value.generatedPlan.trim()) return
  saving.value = true
  await mealStore.editPlan(mealStore.activePlan.id, {
    planName:      editForm.value.planName.trim(),
    generatedPlan: editForm.value.generatedPlan.trim()
  })
  saving.value   = false
  editMode.value = false
}

// ── Activate plan ────────────────────────────────────────────────────────────
async function handleActivate() {
  activating.value = true
  await mealStore.activatePlan(mealStore.activePlan.id)
  activating.value = false
}

// ── Progress / checklist ─────────────────────────────────────────────────────

/**
 * Extract individual meal keys from the plan markdown.
 * Meal plan headings pattern:
 *   ## Day 1 (Monday)
 *   ### Breakfast
 * → key: "Day 1 – Breakfast"
 */
function extractMealKeys(text) {
  if (!text) return []
  const lines = text.split('\n')
  const keys  = []
  let currentDay = ''

  for (const line of lines) {
    const dayMatch  = line.match(/^##\s+(Day\s+\d+[^\n]*)/i)
    const mealMatch = line.match(/^###\s+(.+)/)
    if (dayMatch) {
      currentDay = dayMatch[1].replace(/\*\*/g, '').trim()
    } else if (mealMatch && currentDay) {
      const mealName = mealMatch[1].replace(/\*\*/g, '').trim()
      // Skip summary/table sections
      if (!mealName.toLowerCase().includes('summary') && !mealName.toLowerCase().includes('table')) {
        const key = `${currentDay} – ${mealName}`
        if (!keys.includes(key)) keys.push(key)
      }
    }
  }
  return keys
}

/**
 * Group meal keys by their day heading for grouped display.
 */
function groupedMealKeys(text) {
  const keys = extractMealKeys(text)
  const map  = new Map()
  for (const key of keys) {
    const [day, ...rest] = key.split(' – ')
    if (!map.has(day)) map.set(day, [])
    map.get(day).push(key)
  }
  return Array.from(map.entries()).map(([label, meals]) => ({ label, meals }))
}

/** Strip the day prefix — show only the meal name in checklist row */
function mealNameOnly(key) {
  return key.includes(' – ') ? key.split(' – ').slice(1).join(' – ') : key
}

function isCompleted(key) {
  return mealStore.activePlan?.completedItems?.includes(key) ?? false
}

async function toggleItem(key) {
  const plan = mealStore.activePlan
  if (!plan) return
  const current = plan.completedItems ? [...plan.completedItems] : []
  const updated  = current.includes(key)
    ? current.filter(k => k !== key)
    : [...current, key]
  await mealStore.updateProgress(plan.id, updated)
}

async function handleResetProgress() {
  if (!mealStore.activePlan) return
  await mealStore.updateProgress(mealStore.activePlan.id, [])
}

function progressPercent(plan) {
  const total = extractMealKeys(plan?.generatedPlan).length
  const done  = plan?.completedItems?.length || 0
  if (!total) return 0
  return Math.round((done / total) * 100)
}

/**
 * Parse the AI-generated markdown for a "Weekly Nutrition Summary" table.
 * Looks for a markdown table section with columns: Day | Calories | Protein | Carbs | Fat
 * Returns an array of row objects; the last row is flagged isTotal if it matches Total/Average.
 *
 * The AI typically outputs lines like:
 *   | Day 1 (Monday) | 2500 | 150g | 280g | 75g |
 *   | **Total**       | 17500 | 1050g | 1960g | 525g |
 */
function nutritionTableRows(text) {
  if (!text) return []

  const rows = []
  const lines = text.split('\n')

  // Find the nutrition summary section — look for a header that mentions it
  let inSummarySection = false
  let inTable = false

  for (const raw of lines) {
    const line = raw.trim()

    // Detect entry into a summary section heading
    if (/weekly\s+nutrition\s+summ/i.test(line) || /daily\s+nutrition\s+summ/i.test(line)) {
      inSummarySection = true
      continue
    }

    // If we hit another ## heading after entering the section, we've left it
    if (inSummarySection && /^##\s/.test(line)) {
      break
    }

    // We're now inside the section — look for table rows (lines starting with |)
    if (!line.startsWith('|')) continue

    // Skip separator lines (e.g. |---|---|)
    if (/^\|[\s\-:|]+\|/.test(line)) {
      inTable = true
      continue
    }

    // Parse the cell values
    const cells = line.split('|').map(c => c.replace(/\*\*/g, '').trim()).filter(Boolean)
    if (cells.length < 2) continue

    // Skip the header row (contains "Day" or "Calories")
    const firstCell = cells[0].toLowerCase()
    if (firstCell === 'day' || firstCell.includes('calorie')) continue

    // Determine if this is a total/average row
    const isTotal = /total|average|avg/i.test(firstCell)

    rows.push({
      day:      cells[0] || '–',
      calories: cells[1] || '–',
      protein:  cells[2] || '–',
      carbs:    cells[3] || '–',
      fat:      cells[4] || '–',
      isTotal
    })
  }

  // Fallback: if we found no summary section header, try parsing any table
  // that looks like it has at least 5 columns starting with a Day reference
  if (rows.length === 0) {
    for (const raw of lines) {
      const line = raw.trim()
      if (!line.startsWith('|')) continue
      if (/^\|[\s\-:|]+\|/.test(line)) continue
      const cells = line.split('|').map(c => c.replace(/\*\*/g, '').trim()).filter(Boolean)
      if (cells.length < 5) continue
      const first = cells[0].toLowerCase()
      if (first === 'day' || first.includes('calorie')) continue
      // Only include rows that seem to reference days or totals
      if (!(/day\s*\d/i.test(first) || /mon|tue|wed|thu|fri|sat|sun/i.test(first) || /total|avg/i.test(first))) continue
      rows.push({
        day:      cells[0],
        calories: cells[1],
        protein:  cells[2],
        carbs:    cells[3],
        fat:      cells[4],
        isTotal: /total|average|avg/i.test(cells[0])
      })
    }
  }

  return rows
}

// ── Select plan from list ────────────────────────────────────────────────────
function selectPlan(plan) {
  mealStore.setActivePlan(plan)
  activeTab.value = 'plan'
  editMode.value  = false
}

// ── Pagination ───────────────────────────────────────────────────────────────
const PAGE_SIZE   = 5
const currentPage = ref(1)

const totalPages = computed(() =>
  Math.max(1, Math.ceil(mealStore.plans.length / PAGE_SIZE))
)

const paginatedPlans = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return mealStore.plans.slice(start, start + PAGE_SIZE)
})

function prevPage() { if (currentPage.value > 1) currentPage.value-- }
function nextPage() { if (currentPage.value < totalPages.value) currentPage.value++ }

// ── Lifecycle ────────────────────────────────────────────────────────────────
onMounted(async () => {
  mealStore.fetchPlans()

  if (!profileStore.profile) {
    await profileStore.fetchProfile()
  }
  const p = profileStore.profile
  if (!p) return

  if (p.rugbyPosition)  form.value.rugbyPosition  = p.rugbyPosition
  if (p.weight)         form.value.weight         = p.weight
  if (p.height)         form.value.height         = p.height
  if (p.age)            form.value.age            = p.age
  if (p.targetWeight)   form.value.targetWeight   = p.targetWeight
  if (p.activityLevel)  form.value.activityLevel  = p.activityLevel

  if (p.dietaryRestrictions) {
    const d = p.dietaryRestrictions.toLowerCase()
    if (d.includes('vegan'))           form.value.dietaryPreference = 'VEGAN'
    else if (d.includes('vegetarian')) form.value.dietaryPreference = 'VEGETARIAN'
    else if (d.includes('halal'))      form.value.dietaryPreference = 'HALAL'
    else                               form.value.dietaryPreference = 'NO_RESTRICTION'
    if (!form.value.allergies) form.value.allergies = p.dietaryRestrictions
  }
})

// ── Calorie estimate preview ─────────────────────────────────────────────────
const estimatedCalories = computed(() => {
  const { weight, height, age, goals } = form.value
  if (!weight || !height || !age) return null
  const bmr  = (10 * weight) + (6.25 * height) - (5 * age) + 5
  const tdee = Math.round(bmr * 1.725)
  const adjustments = { MUSCLE_GAIN: 400, WEIGHT_LOSS: -400, PERFORMANCE: 200, MAINTAIN: 0 }
  const primaryGoal = goals[0] || ''
  return tdee + (adjustments[primaryGoal] || 0)
})

// ── Validation ───────────────────────────────────────────────────────────────
function validate() {
  errors.value = {}
  if (!form.value.rugbyPosition)          errors.value.rugbyPosition     = 'Please select your rugby position.'
  if (!form.value.goals.length)           errors.value.goals             = 'Please select at least one nutrition goal.'
  if (!form.value.weight)            errors.value.weight            = 'Weight is required.'
  if (!form.value.height)            errors.value.height            = 'Height is required.'
  if (!form.value.age)               errors.value.age               = 'Age is required.'
  if (!form.value.dietaryPreference) errors.value.dietaryPreference = 'Please select a dietary preference.'
  if (!form.value.mealsPerDay)       errors.value.mealsPerDay       = 'Please select meals per day.'
  if (!form.value.activityLevel)     errors.value.activityLevel     = 'Please select your activity level.'
  if (!form.value.trainingPhase)     errors.value.trainingPhase     = 'Please select a training phase.'
  if (!form.value.mealPrepTime)      errors.value.mealPrepTime      = 'Please select meal prep time.'
  return Object.keys(errors.value).length === 0
}

function clearError(field) {
  if (errors.value[field]) delete errors.value[field]
  mealStore.clearError()
}

// ── Generate ─────────────────────────────────────────────────────────────────
async function handleGenerate() {
  if (!validate()) return
  mealStore.clearError()
  activeTab.value  = 'plan'
  editMode.value   = false
  await mealStore.generatePlan({ ...form.value })
  currentPage.value = 1
}

// ── Delete ───────────────────────────────────────────────────────────────────
function confirmDelete(plan) { deleteTarget.value = plan }

async function handleDelete() {
  if (!deleteTarget.value) return
  await mealStore.deletePlan(deleteTarget.value.id)
  deleteTarget.value = null
}

// ── Copy ──────────────────────────────────────────────────────────────────────
function openCopyModal(plan) {
  copySource.value = plan
  copyName.value   = plan.planName + ' (Copy)'
  copyError.value  = ''
}

function selectCopySource(plan) {
  copySource.value = plan
  copyName.value   = plan.planName + ' (Copy)'
  copyError.value  = ''
}

function closeCopyModal() {
  copySource.value = null
  copyName.value   = ''
  copyError.value  = ''
}

async function handleCopy() {
  const name = copyName.value.trim()
  if (!name) { copyError.value = 'Please enter a name for the copied plan.'; return }
  copying.value   = true
  copyError.value = ''
  const result = await mealStore.copyPlan(copySource.value.id, name)
  copying.value = false
  if (result) {
    closeCopyModal()
    mealStore.setActivePlan(result)
    currentPage.value = 1
  } else {
    copyError.value = mealStore.error || 'Failed to copy plan.'
  }
}

// ── Goal toggle (multi-select) ────────────────────────────────────────────────
function toggleGoal(value) {
  const idx = form.value.goals.indexOf(value)
  if (idx === -1) form.value.goals.push(value)
  else            form.value.goals.splice(idx, 1)
  if (errors.value.goals) delete errors.value.goals
  mealStore.clearError()
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function goalLabel(goal) {
  const map = { MUSCLE_GAIN: '💪 Muscle Gain', WEIGHT_LOSS: '🔥 Weight Loss', MAINTAIN: '⚖️ Maintain', PERFORMANCE: '⚡ Performance' }
  return map[goal] || goal
}

function goalsLabel(goals) {
  if (!goals || goals.length === 0) return '—'
  const map = { MUSCLE_GAIN: '💪 Muscle Gain', WEIGHT_LOSS: '🔥 Weight Loss', MAINTAIN: '⚖️ Maintain', PERFORMANCE: '⚡ Performance' }
  return goals.map(g => map[g] || g).join(' + ')
}

function dietLabel(diet) {
  const map = { HALAL: '🌙 Halal', VEGETARIAN: '🥦 Vegetarian', VEGAN: '🌱 Vegan', NO_RESTRICTION: '🍽️ No Restriction' }
  return map[diet] || diet
}

function phaseLabel(phase) {
  const map = { PRE_SEASON: '🔥 Pre-Season', IN_SEASON: '🏉 In-Season', OFF_SEASON: '💪 Off-Season', POST_SEASON: '🛌 Post-Season' }
  return map[phase] || phase || ''
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('en-MY', { day: 'numeric', month: 'short', year: 'numeric' })
}

function renderMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/^## (.+)$/gm,   '<h2 class="md-h2">$1</h2>')
    .replace(/^### (.+)$/gm,  '<h3 class="md-h3">$1</h3>')
    .replace(/^#### (.+)$/gm, '<h4 class="md-h4">$1</h4>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g,    '<em>$1</em>')
    .replace(/^[-*] (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>[\s\S]*?<\/li>\n?)+/g, m => `<ul class="md-ul">${m}</ul>`)
    .replace(/^\|(.+)\|$/gm, (_, cells) =>
      `<tr>${cells.split('|').map(c => `<td>${c.trim()}</td>`).join('')}</tr>`)
    .replace(/(<tr>[\s\S]*?<\/tr>\n?)+/g, m => `<table class="md-table">${m}</table>`)
    .replace(/^---+$/gm, '<hr class="md-hr">')
    .replace(/\n\n/g, '</p><p class="md-p">')
    .replace(/\n/g,   '<br>')
    .replace(/^/, '<p class="md-p">')
    .replace(/$/, '</p>')
}
</script>

<style scoped>
/* ── Page Layout ─────────────────────────────────────────────── */
.meal-page      { display: flex; flex-direction: column; gap: 24px; }
.page-header    { display: flex; align-items: flex-start; }
.page-title     { font-family: 'Barlow Condensed', sans-serif; font-size: 26px; font-weight: 700; }
.page-subtitle  { color: var(--color-muted); font-size: 13.5px; margin-top: 4px; }

.meal-layout {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 20px;
  align-items: start;
}

/* ── Card ────────────────────────────────────────────────────── */
.card           { background: var(--color-bg-2); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 22px; }
.card-header    { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; }
.card-icon      { font-size: 18px; }
.card-title     { font-family: 'Barlow Condensed', sans-serif; font-size: 18px; font-weight: 700; flex: 1; }
.plan-count     { background: var(--color-green-dim); color: var(--color-green-light); font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 99px; }

/* ── Form ────────────────────────────────────────────────────── */
.generate-form      { display: flex; flex-direction: column; gap: 16px; }
.form-row           { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-row.three-col { grid-template-columns: 1fr 1fr 1fr; }
.form-group         { display: flex; flex-direction: column; gap: 5px; }
.form-label         { font-size: 13px; font-weight: 500; }
.req                { color: var(--color-error); }
.optional-tag       { font-size: 11px; color: var(--color-muted); font-weight: 400; }
.field-error        { font-size: 12px; color: var(--color-error); }
.field-hint         { font-size: 11.5px; color: var(--color-muted); }

.form-input, .form-select {
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); color: var(--color-text);
  font-size: 13.5px; padding: 9px 12px; width: 100%;
  transition: border-color var(--transition); font-family: inherit; box-sizing: border-box;
}
.form-input:focus, .form-select:focus { outline: none; border-color: var(--color-green); }
.form-group.has-error .form-input,
.form-group.has-error .form-select { border-color: var(--color-error); }

.label-hint { font-size: 11px; font-weight: 400; color: var(--color-muted); }

/* Goal selector */
.goal-selector  { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.goal-btn {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 10px 8px; border-radius: var(--radius-sm);
  border: 1px solid var(--color-border); background: var(--color-surface);
  color: var(--color-text-dim); cursor: pointer;
  transition: all var(--transition); font-family: inherit;
}
.goal-btn:hover  { border-color: var(--color-green); color: var(--color-green-light); }
.goal-btn.active { background: var(--color-green-dim); border-color: var(--color-green); color: var(--color-green-light); }
.goal-icon       { font-size: 18px; }
.goal-label      { font-size: 12px; font-weight: 500; flex: 1; }
.goal-tick       { font-size: 11px; font-weight: 700; color: var(--color-green-light); }

/* Diet selector */
.diet-selector  { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.diet-btn {
  padding: 9px 10px; border-radius: var(--radius-sm);
  border: 1px solid var(--color-border); background: var(--color-surface);
  color: var(--color-text-dim); cursor: pointer; font-size: 12.5px; font-weight: 500;
  transition: all var(--transition); font-family: inherit; text-align: center;
}
.diet-btn:hover  { border-color: var(--color-green); color: var(--color-green-light); }
.diet-btn.active { background: var(--color-green-dim); border-color: var(--color-green); color: var(--color-green-light); }

/* Sessions selector */
.sessions-selector { display: flex; gap: 8px; }
.session-btn {
  width: 48px; height: 42px; border-radius: var(--radius-sm);
  border: 1px solid var(--color-border); background: var(--color-surface);
  color: var(--color-text-dim); font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all var(--transition); font-family: inherit;
}
.session-btn:hover  { border-color: var(--color-green); color: var(--color-green-light); }
.session-btn.active { background: var(--color-green-dim); border-color: var(--color-green); color: var(--color-green-light); }

/* Calorie preview */
.calorie-preview {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: var(--radius-sm);
  background: var(--color-green-dim); border: 1px solid var(--color-green);
}
.calorie-icon   { font-size: 22px; }
.calorie-label  { font-size: 11.5px; color: var(--color-muted); }
.calorie-value  { font-size: 16px; font-weight: 700; color: var(--color-green-light); }

/* Generate button */
.btn-generate {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  width: 100%; padding: 12px;
  background: var(--color-green); color: #fff;
  font-size: 14px; font-weight: 600;
  border: none; border-radius: var(--radius-sm);
  cursor: pointer; transition: opacity var(--transition); font-family: inherit;
}
.btn-generate:disabled            { opacity: 0.65; cursor: not-allowed; }
.btn-generate:not(:disabled):hover { opacity: 0.9; }
.generating-hint { font-size: 12px; color: var(--color-muted); text-align: center; }

/* ── Saved Plans ─────────────────────────────────────────────── */
.saved-plans-card { margin-top: 16px; }
.plans-list       { display: flex; flex-direction: column; gap: 8px; }

.plan-item {
  display: flex; align-items: flex-start; gap: 10px; padding: 12px;
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); cursor: pointer;
  transition: border-color var(--transition), background var(--transition);
}
.plan-item:hover         { border-color: var(--color-green); }
.plan-item.is-selected   { border-color: var(--color-green); background: var(--color-green-dim); }
.plan-item.is-current    { border-left: 3px solid var(--color-green); }

.plan-item-badge { font-size: 14px; flex-shrink: 0; margin-top: 1px; }
.plan-item-info  { flex: 1; min-width: 0; }
.plan-item-name-row { display: flex; align-items: center; gap: 6px; }
.plan-item-name  { font-size: 13.5px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.badge-active    { font-size: 9px; font-weight: 700; letter-spacing: 0.5px; padding: 2px 6px; border-radius: 99px; background: var(--color-green); color: #fff; flex-shrink: 0; }
.plan-item-meta  { font-size: 11.5px; color: var(--color-muted); margin-top: 2px; }
.plan-item-date  { font-size: 11px; color: var(--color-muted); margin-top: 3px; }

/* Progress bar in list */
.plan-item-progress { display: flex; align-items: center; gap: 6px; margin-top: 5px; }
.progress-bar-track { flex: 1; height: 4px; background: var(--color-border); border-radius: 99px; overflow: hidden; }
.progress-bar-fill  { height: 100%; background: var(--color-green); border-radius: 99px; transition: width 0.3s ease; }
.progress-label { font-size: 10px; color: var(--color-muted); white-space: nowrap; }

.plan-item-actions { display: flex; flex-direction: column; gap: 4px; flex-shrink: 0; }
.icon-btn { background: none; border: none; cursor: pointer; font-size: 14px; padding: 4px 6px; border-radius: var(--radius-sm); opacity: 0.45; transition: opacity var(--transition); }
.icon-btn:hover { opacity: 1; }
.icon-btn-danger:hover { opacity: 1; color: var(--color-error); }

/* ── Pagination ──────────────────────────────────────────────── */
.pagination {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; padding-top: 10px; border-top: 1px solid var(--color-border); margin-top: 6px;
}
.page-btn {
  width: 30px; height: 30px; border-radius: var(--radius-sm);
  border: 1px solid var(--color-border); background: var(--color-surface);
  color: var(--color-text); font-size: 16px; font-weight: 600;
  cursor: pointer; transition: all var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.page-btn:hover:not(:disabled) { border-color: var(--color-green); color: var(--color-green-light); }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.page-info { font-size: 12.5px; color: var(--color-muted); min-width: 44px; text-align: center; }

/* ── States ──────────────────────────────────────────────────── */
.loading-state, .empty-state {
  display: flex; flex-direction: column; align-items: center;
  padding: 24px; gap: 10px;
  color: var(--color-muted); font-size: 13.5px; text-align: center;
}
.spinner-muted { width: 20px; height: 20px; border: 2px solid var(--color-border); border-top-color: var(--color-green); border-radius: 50%; animation: spin 0.7s linear infinite; }

/* ── Plan Column ─────────────────────────────────────────────── */
.plan-column { min-width: 0; }

.plan-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 360px; background: var(--color-bg-2);
  border: 1px dashed var(--color-border); border-radius: var(--radius-lg);
  color: var(--color-muted); text-align: center; gap: 10px; padding: 40px;
}
.plan-empty-icon { font-size: 48px; }
.plan-empty h3   { font-family: 'Barlow Condensed', sans-serif; font-size: 20px; color: var(--color-text-dim); }
.plan-empty p    { font-size: 13.5px; }

.plan-generating {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 380px; background: var(--color-bg-2); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); text-align: center; gap: 16px; padding: 40px;
}
.plan-generating h3 { font-family: 'Barlow Condensed', sans-serif; font-size: 22px; }
.plan-generating p  { font-size: 14px; color: var(--color-muted); max-width: 340px; line-height: 1.6; }

.ai-pulse {
  width: 64px; height: 64px; border-radius: 50%;
  background: var(--color-green-dim);
  display: flex; align-items: center; justify-content: center; font-size: 30px;
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes spin   { to { transform: rotate(360deg); } }
@keyframes pulse { 0%,100%{transform:scale(1);opacity:1} 50%{transform:scale(1.1);opacity:0.7} }

.progress-dots { display: flex; gap: 6px; }
.progress-dots span { width: 8px; height: 8px; border-radius: 50%; background: var(--color-green); animation: dot-bounce 1.2s ease-in-out infinite; }
.progress-dots span:nth-child(2) { animation-delay: 0.2s; }
.progress-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dot-bounce { 0%,100%{opacity:0.3;transform:translateY(0)} 50%{opacity:1;transform:translateY(-4px)} }

/* ── Plan Display ────────────────────────────────────────────── */
.plan-display { padding: 28px; }

.plan-display-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  gap: 16px; padding-bottom: 18px; border-bottom: 1px solid var(--color-border);
}
.plan-header-left    { flex: 1; min-width: 0; }
.plan-header-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; flex-wrap: wrap; justify-content: flex-end; }

.plan-display-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 22px; font-weight: 700; margin-bottom: 10px;
  display: flex; align-items: center; gap: 8px;
}
.title-active-badge {
  font-size: 10px; font-weight: 700; letter-spacing: 0.5px;
  padding: 3px 8px; border-radius: 99px;
  background: var(--color-green); color: #fff; font-family: inherit;
}
.plan-name-input { font-size: 17px; font-weight: 600; margin-bottom: 10px; }

.plan-tags     { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.tag           { font-size: 11.5px; font-weight: 500; padding: 3px 10px; border-radius: 99px; }
.tag-position  { background: var(--color-green-dim); color: var(--color-green-light); }
.tag-goal      { background: #1e3a5f; color: #60a5fa; }
.tag-diet      { background: #2d1e3a; color: #c084fc; }
.tag-meals     { background: var(--color-surface); color: var(--color-text-dim); border: 1px solid var(--color-border); }
.tag-phase     { background: #2a1e3a; color: #c084fc; }
.plan-stats-line { font-size: 12px; color: var(--color-muted); }

/* Action buttons */
.btn           { display: inline-flex; align-items: center; gap: 6px; padding: 7px 14px; border-radius: var(--radius-sm); font-size: 13px; font-weight: 500; cursor: pointer; border: none; font-family: inherit; transition: all var(--transition); white-space: nowrap; }
.btn-ghost     { background: var(--color-surface); color: var(--color-text); border: 1px solid var(--color-border); }
.btn-ghost:hover { background: var(--color-surface-2); }
.btn-activate  { background: var(--color-green-dim); color: var(--color-green-light); border: 1px solid var(--color-green); }
.btn-activate:hover:not(:disabled) { background: var(--color-green); color: #fff; }
.btn-activate:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-save      { background: var(--color-green); color: #fff; }
.btn-save:hover:not(:disabled) { opacity: 0.85; }
.btn-save:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-danger    { background: var(--color-error); color: #fff; }
.btn-danger:hover { opacity: 0.85; }
.btn-primary   { background: var(--color-green); color: #fff; }
.btn-primary:hover:not(:disabled) { opacity: 0.88; }
.btn-primary:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-sm        { padding: 5px 10px; font-size: 12px; }
.active-indicator { font-size: 12.5px; color: var(--color-green-light); font-weight: 500; display: flex; align-items: center; gap: 4px; }

/* ── Tabs ────────────────────────────────────────────────────── */
.plan-tabs {
  display: flex; gap: 2px;
  margin: 18px 0 0;
  border-bottom: 1px solid var(--color-border);
}
.plan-tab {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 9px 16px; font-size: 13.5px; font-weight: 500;
  background: none; border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-muted); cursor: pointer; font-family: inherit;
  margin-bottom: -1px; transition: all var(--transition);
}
.plan-tab:hover  { color: var(--color-text); }
.plan-tab.active { color: var(--color-green-light); border-bottom-color: var(--color-green); }
.tab-badge {
  background: var(--color-green); color: #fff;
  font-size: 10px; font-weight: 700;
  padding: 1px 6px; border-radius: 99px;
}

/* ── Plan Body ───────────────────────────────────────────────── */
.plan-body { padding-top: 20px; overflow: hidden; }

/* ── Edit Body ───────────────────────────────────────────────── */
.edit-body { padding-top: 20px; }
.plan-editor {
  width: 100%; min-height: 440px;
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); color: var(--color-text);
  font-size: 13px; font-family: 'Courier New', monospace;
  padding: 14px; resize: vertical; box-sizing: border-box; line-height: 1.6;
}
.plan-editor:focus { outline: none; border-color: var(--color-green); }

/* ── Progress Body ───────────────────────────────────────────── */
.progress-body { padding-top: 20px; }

.progress-summary {
  display: flex; align-items: center; gap: 20px;
  padding: 16px; background: var(--color-surface);
  border: 1px solid var(--color-border); border-radius: var(--radius-sm); margin-bottom: 20px;
}
.progress-ring-wrap { position: relative; width: 60px; height: 60px; flex-shrink: 0; }
.progress-ring      { width: 60px; height: 60px; }
.progress-ring-label {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; color: var(--color-green-light);
}
.progress-summary-text  { flex: 1; }
.progress-summary-title { font-size: 14px; font-weight: 600; margin-bottom: 3px; }
.progress-summary-sub   { font-size: 12.5px; color: var(--color-muted); margin-bottom: 8px; }
.reset-btn { color: var(--color-muted); }

/* ── Nutrition Summary Table ─────────────────────────────────── */
.nutrition-table-section {
  margin-bottom: 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.nutrition-table-title {
  font-size: 14px; font-weight: 700;
  color: var(--color-text);
  padding: 12px 16px 10px;
  margin: 0;
  border-bottom: 1px solid var(--color-border);
}
.nutrition-table-wrap { overflow-x: auto; }
.nutrition-table {
  width: 100%; border-collapse: collapse; font-size: 13px;
}
.nutrition-table th {
  background: var(--color-bg-3);
  color: var(--color-muted);
  font-weight: 600; font-size: 11px; text-transform: uppercase; letter-spacing: 0.6px;
  padding: 8px 12px; text-align: left;
  border-bottom: 1px solid var(--color-border);
}
.nutrition-table td {
  padding: 9px 12px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}
.nutrition-table tbody tr:last-child td { border-bottom: none; }
.nutrition-table tbody tr:hover td { background: var(--color-bg-3); }
.nutrition-table .col-day { font-weight: 500; }
.nutrition-table .col-num { color: var(--color-text); font-variant-numeric: tabular-nums; }
.nutrition-table .row-total td {
  background: rgba(180,255,0,0.06);
  font-weight: 700;
  color: var(--color-green-light);
  border-top: 1px solid var(--color-border);
}
.nutrition-table-empty {
  padding: 16px; font-size: 13px; color: var(--color-muted); margin: 0;
}

/* ── Grouped Checklist ───────────────────────────────────────── */
.checklist-grouped { display: flex; flex-direction: column; gap: 16px; }

.checklist-day { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-sm); overflow: hidden; }

.checklist-day-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px;
  background: var(--color-bg-2);
  border-bottom: 1px solid var(--color-border);
}
.checklist-day-title { font-size: 13px; font-weight: 600; color: var(--color-green-light); }
.checklist-day-count { font-size: 12px; color: var(--color-muted); }

.checklist-item {
  display: flex; align-items: center; gap: 12px;
  padding: 11px 14px;
  cursor: pointer; user-select: none;
  border-bottom: 1px solid var(--color-border);
  transition: background var(--transition);
}
.checklist-item:last-child { border-bottom: none; }
.checklist-item:hover { background: var(--color-bg-2); }
.checklist-item.completed { background: var(--color-green-dim); }

.checklist-check {
  width: 20px; height: 20px; border: 2px solid var(--color-border);
  border-radius: 5px; display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; color: var(--color-green-light);
  flex-shrink: 0; transition: all var(--transition);
}
.checklist-item.completed .checklist-check { background: var(--color-green); border-color: var(--color-green); color: #fff; }
.checklist-label { font-size: 13.5px; flex: 1; }
.checklist-item.completed .checklist-label { text-decoration: line-through; color: var(--color-muted); }
.checklist-empty { font-size: 13.5px; color: var(--color-muted); text-align: center; padding: 20px 0; }

/* ── Markdown ────────────────────────────────────────────────── */
.plan-markdown :deep(.md-h2) { font-family: 'Barlow Condensed', sans-serif; font-size: 20px; font-weight: 700; color: var(--color-green-light); margin: 28px 0 12px; padding-bottom: 6px; border-bottom: 1px solid var(--color-border); }
.plan-markdown :deep(.md-h3) { font-size: 15px; font-weight: 600; color: var(--color-text); margin: 18px 0 8px; }
.plan-markdown :deep(.md-h4) { font-size: 13.5px; font-weight: 600; color: var(--color-text-dim); margin: 12px 0 6px; }
.plan-markdown :deep(.md-p)  { font-size: 14px; line-height: 1.7; color: var(--color-text-dim); margin: 8px 0; }
.plan-markdown :deep(.md-ul) { padding-left: 18px; margin: 8px 0; display: flex; flex-direction: column; gap: 5px; }
.plan-markdown :deep(li)     { font-size: 14px; line-height: 1.6; color: var(--color-text-dim); }
.plan-markdown :deep(.md-table) { width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 13.5px; }
.plan-markdown :deep(td)     { padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-dim); }
.plan-markdown :deep(tr:first-child td) { background: var(--color-surface); font-weight: 600; color: var(--color-text); }
.plan-markdown :deep(.md-hr) { border: none; border-top: 1px solid var(--color-border); margin: 20px 0; }
.plan-markdown :deep(strong) { color: var(--color-text); }

/* ── Alerts ──────────────────────────────────────────────────── */
.alert         { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: var(--radius-sm); font-size: 13.5px; margin-bottom: 16px; }
.alert-error   { background: var(--color-error-bg); color: var(--color-error); border: 1px solid var(--color-error-border); }
.alert-close   { margin-left: auto; background: none; border: none; cursor: pointer; font-size: 16px; color: inherit; opacity: 0.6; }
.alert-close:hover { opacity: 1; }

/* ── Spinner ─────────────────────────────────────────────────── */
.spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; flex-shrink: 0; }
.spinner-sm { width: 13px; height: 13px; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Modal ───────────────────────────────────────────────────── */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal         { background: var(--color-bg-2); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 28px; max-width: 400px; width: 90%; display: flex; flex-direction: column; gap: 14px; }
.modal h3      { font-family: 'Barlow Condensed', sans-serif; font-size: 20px; font-weight: 700; }
.modal p       { font-size: 14px; color: var(--color-text-dim); line-height: 1.6; }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 4px; }

/* Copy modal extras */
.modal-copy    { max-width: 480px; }
.copy-section-label { font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.8px; color: var(--color-muted); margin-bottom: 6px; }
.copy-plan-list { display: flex; flex-direction: column; gap: 4px; max-height: 180px; overflow-y: auto; border: 1px solid var(--color-border); border-radius: var(--radius-sm); padding: 6px; background: var(--color-bg-3); }
.copy-plan-item { display: flex; flex-direction: column; gap: 2px; padding: 8px 10px; border-radius: var(--radius-sm); cursor: pointer; transition: background var(--transition); }
.copy-plan-item:hover { background: var(--color-surface); }
.copy-plan-item.selected { background: var(--color-green-dim); border: 1px solid var(--color-green); }
.copy-plan-name { font-size: 13px; font-weight: 500; color: var(--color-text); }
.copy-plan-meta { font-size: 11px; color: var(--color-muted); }
.copy-name-row  { display: flex; flex-direction: column; gap: 6px; }
.copy-error     { font-size: 12px; color: var(--color-error); }
.spinner-sm     { width: 12px; height: 12px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; display: inline-block; }

/* ── Transitions ─────────────────────────────────────────────── */
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to       { opacity: 0; }

/* ── Responsive ──────────────────────────────────────────────── */
@media (max-width: 900px) { .meal-layout { grid-template-columns: 1fr; } }
</style>
