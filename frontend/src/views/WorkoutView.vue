<template>
  <div class="workout-page">

    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">Workout Planner 💪</h1>
    <p class="page-subtitle">Generate a personalised rugby workout plan powered by AI</p>
      </div>
    </div>

    <div class="workout-layout">

      <!-- ══════════════════════════════════════════
           LEFT: Generate Form + Saved Plans
           ══════════════════════════════════════════ -->
      <div class="form-column">
        <section class="card" aria-labelledby="generate-heading">
          <div class="card-header">
            <h2 class="card-title" id="generate-heading">Generate New Plan</h2>
          </div>

          <!-- Generate + plan-loading errors: both originate in this column -->
          <transition name="fade">
            <div v-if="workoutStore.error" class="alert alert-error" role="alert">
              <span>{{ workoutStore.error }}</span>
              <button
                type="button"
                class="alert-close"
                aria-label="Dismiss error"
                @click="workoutStore.clearError()">×</button>
            </div>
          </transition>

          <form @submit.prevent="handleGenerate" class="generate-form" novalidate>

            <!-- Plan Name -->
            <div class="form-group">
              <label class="form-label" for="wk-plan-name">
                Plan Name <span class="optional-tag">optional</span>
              </label>
              <input
                id="wk-plan-name"
                v-model="form.planName"
                type="text"
                class="form-input"
                placeholder="e.g. Pre-Season Power Block" />
            </div>

            <!-- Rugby Position -->
            <div class="form-group" :class="{ 'has-error': errors.rugbyPosition }">
              <label class="form-label" for="wk-position">
                Rugby Position <span class="req" aria-hidden="true">*</span>
              </label>
              <select
                id="wk-position"
                v-model="form.rugbyPosition"
                class="form-select"
                required
                :aria-invalid="!!errors.rugbyPosition"
                :aria-describedby="errors.rugbyPosition ? 'wk-position-error' : undefined"
                @change="clearError('rugbyPosition')">
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
              <p v-if="errors.rugbyPosition" class="field-error" id="wk-position-error">
                {{ errors.rugbyPosition }}
              </p>
            </div>

            <!-- Goal + Training Level -->
            <div class="form-row">
              <div class="form-group" :class="{ 'has-error': errors.goal }">
                <label class="form-label" for="wk-goal">
                  Primary Goal <span class="req" aria-hidden="true">*</span>
                </label>
                <select
                  id="wk-goal"
                  v-model="form.goal"
                  class="form-select"
                  required
                  :aria-invalid="!!errors.goal"
                  :aria-describedby="errors.goal ? 'wk-goal-error' : undefined"
                  @change="clearError('goal')">
                  <option value="" disabled>Select goal</option>
                  <option value="STRENGTH">Strength</option>
                  <option value="POWER">Power</option>
                  <option value="ENDURANCE">Endurance</option>
                  <option value="LEAN">Lean / Cut</option>
                </select>
                <p v-if="errors.goal" class="field-error" id="wk-goal-error">{{ errors.goal }}</p>
              </div>

              <div class="form-group" :class="{ 'has-error': errors.trainingLevel }">
                <label class="form-label" for="wk-level">
                  Training Level <span class="req" aria-hidden="true">*</span>
                </label>
                <select
                  id="wk-level"
                  v-model="form.trainingLevel"
                  class="form-select"
                  required
                  :aria-invalid="!!errors.trainingLevel"
                  :aria-describedby="errors.trainingLevel ? 'wk-level-error' : undefined"
                  @change="clearError('trainingLevel')">
                  <option value="" disabled>Select level</option>
                  <option value="BEGINNER">Beginner</option>
                  <option value="INTERMEDIATE">Intermediate</option>
                  <option value="ADVANCED">Advanced</option>
                </select>
                <p v-if="errors.trainingLevel" class="field-error" id="wk-level-error">
                  {{ errors.trainingLevel }}
                </p>
              </div>
            </div>

            <!-- Physical Stats -->
            <div class="form-row three-col">
              <div class="form-group" :class="{ 'has-error': errors.weight }">
                <label class="form-label" for="wk-weight">
                  Weight (kg) <span class="req" aria-hidden="true">*</span>
                </label>
                <input
                  id="wk-weight"
                  v-model.number="form.weight"
                  type="number"
                  class="form-input"
                  inputmode="numeric"
                  placeholder="90"
                  min="40"
                  max="200"
                  required
                  :aria-invalid="!!errors.weight"
                  :aria-describedby="errors.weight ? 'wk-weight-error' : undefined"
                  @input="clearError('weight')" />
                <p v-if="errors.weight" class="field-error" id="wk-weight-error">{{ errors.weight }}</p>
              </div>

              <div class="form-group" :class="{ 'has-error': errors.height }">
                <label class="form-label" for="wk-height">
                  Height (cm) <span class="req" aria-hidden="true">*</span>
                </label>
                <input
                  id="wk-height"
                  v-model.number="form.height"
                  type="number"
                  class="form-input"
                  inputmode="numeric"
                  placeholder="180"
                  min="140"
                  max="220"
                  required
                  :aria-invalid="!!errors.height"
                  :aria-describedby="errors.height ? 'wk-height-error' : undefined"
                  @input="clearError('height')" />
                <p v-if="errors.height" class="field-error" id="wk-height-error">{{ errors.height }}</p>
              </div>

              <div class="form-group" :class="{ 'has-error': errors.age }">
                <label class="form-label" for="wk-age">
                  Age <span class="req" aria-hidden="true">*</span>
                </label>
                <input
                  id="wk-age"
                  v-model.number="form.age"
                  type="number"
                  class="form-input"
                  inputmode="numeric"
                  placeholder="22"
                  min="15"
                  max="60"
                  required
                  :aria-invalid="!!errors.age"
                  :aria-describedby="errors.age ? 'wk-age-error' : undefined"
                  @input="clearError('age')" />
                <p v-if="errors.age" class="field-error" id="wk-age-error">{{ errors.age }}</p>
              </div>
            </div>

            <!-- Sessions Per Week -->
            <fieldset class="form-group fieldset-reset" :class="{ 'has-error': errors.sessionsPerWeek }">
              <legend class="form-label">
                Sessions Per Week <span class="req" aria-hidden="true">*</span>
              </legend>
              <div
                class="sessions-selector"
                role="radiogroup"
                aria-label="Sessions per week"
                :aria-invalid="!!errors.sessionsPerWeek"
                :aria-describedby="errors.sessionsPerWeek ? 'wk-sessions-error' : undefined">
                <button
                  v-for="n in [2, 3, 4, 5, 6, 7]"
                  :key="n"
                  type="button"
                  class="session-btn"
                  role="radio"
                  :aria-checked="form.sessionsPerWeek === n"
                  :class="{ active: form.sessionsPerWeek === n }"
                  @click="form.sessionsPerWeek = n; clearError('sessionsPerWeek')">
                  {{ n }}
                </button>
              </div>
              <p v-if="errors.sessionsPerWeek" class="field-error" id="wk-sessions-error">
                {{ errors.sessionsPerWeek }}
              </p>
            </fieldset>

            <!-- Training Phase + Available Equipment -->
            <div class="form-row">
              <div class="form-group" :class="{ 'has-error': errors.trainingPhase }">
                <label class="form-label" for="wk-phase">
                  Training Phase <span class="req" aria-hidden="true">*</span>
                </label>
                <select
                  id="wk-phase"
                  v-model="form.trainingPhase"
                  class="form-select"
                  required
                  :aria-invalid="!!errors.trainingPhase"
                  :aria-describedby="errors.trainingPhase ? 'wk-phase-error' : undefined"
                  @change="clearError('trainingPhase')">
                  <option value="" disabled>Select phase</option>
                  <option value="PRE_SEASON">Pre-Season</option>
                  <option value="IN_SEASON">In-Season</option>
                  <option value="OFF_SEASON">Off-Season</option>
                  <option value="POST_SEASON">Post-Season</option>
                </select>
                <p v-if="errors.trainingPhase" class="field-error" id="wk-phase-error">
                  {{ errors.trainingPhase }}
                </p>
              </div>

              <div class="form-group" :class="{ 'has-error': errors.availableEquipment }">
                <label class="form-label" for="wk-equipment">
                  Available Equipment <span class="req" aria-hidden="true">*</span>
                </label>
                <select
                  id="wk-equipment"
                  v-model="form.availableEquipment"
                  class="form-select"
                  required
                  :aria-invalid="!!errors.availableEquipment"
                  :aria-describedby="errors.availableEquipment ? 'wk-equipment-error' : undefined"
                  @change="clearError('availableEquipment')">
                  <option value="" disabled>Select equipment</option>
                  <option value="GYM">Full Gym</option>
                  <option value="HOME">Home Setup</option>
                  <option value="FIELD">Field Only</option>
                  <option value="MINIMAL">Minimal / Bodyweight</option>
                </select>
                <p v-if="errors.availableEquipment" class="field-error" id="wk-equipment-error">
                  {{ errors.availableEquipment }}
                </p>
              </div>
            </div>

            <!-- Focus Area -->
            <div class="form-group">
              <label class="form-label" for="wk-focus">
                Specific Focus / Weakness <span class="optional-tag">optional</span>
              </label>
              <input
                id="wk-focus"
                v-model="form.focusArea"
                type="text"
                class="form-input"
                placeholder="e.g. Improve scrummaging power and sprint acceleration"
                aria-describedby="wk-focus-hint" />
              <p class="field-hint" id="wk-focus-hint">AI will prioritise these areas in the plan.</p>
            </div>

            <!-- Injury Notes -->
            <div class="form-group">
              <label class="form-label" for="wk-injury">
                Injury / Health Notes <span class="optional-tag">optional</span>
              </label>
              <textarea
                id="wk-injury"
                v-model="form.injuryNotes"
                class="form-textarea"
                rows="2"
                placeholder="e.g. Mild left knee soreness, avoid deep squats"
                aria-describedby="wk-injury-hint"></textarea>
              <p class="field-hint" id="wk-injury-hint">
                AI will avoid exercises that aggravate your condition.
              </p>
            </div>

            <!-- Submit -->
            <button type="submit" class="btn-generate" :disabled="workoutStore.generating">
              <span v-if="workoutStore.generating" class="spinner" aria-hidden="true"></span>
              <span>{{ workoutStore.generating ? 'Generating your plan…' : 'Generate Workout Plan' }}</span>
            </button>

            <p v-if="workoutStore.generating" class="generating-hint">
              AI is building your personalised plan — this may take 20–60 seconds.
            </p>

          </form>
        </section>

        <!-- Saved Plans List -->
        <section class="card saved-plans-card" aria-labelledby="saved-heading">
          <div class="card-header">
            <h2 class="card-title" id="saved-heading">Saved Plans</h2>
            <span class="plan-count" v-if="workoutStore.plans.length">
              {{ workoutStore.plans.length }}
            </span>
          </div>

          <div v-if="workoutStore.loadingPlans" class="loading-state">
            <div class="spinner-muted" aria-hidden="true"></div>
            <p>Loading plans…</p>
          </div>

          <div v-else-if="workoutStore.plans.length === 0" class="empty-state">
            <p>No saved plans yet. Generate your first plan above.</p>
          </div>

          <div v-else>
            <ul class="plans-list">
              <li
                v-for="plan in paginatedPlans"
                :key="plan.id"
                class="plan-item"
                :class="{
                  'is-selected': workoutStore.activePlan?.id === plan.id,
                  'is-current': plan.isActive
                }">
                <button
                  type="button"
                  class="plan-item-select"
                  :aria-pressed="workoutStore.activePlan?.id === plan.id"
                  @click="selectPlan(plan)">
                  <span class="plan-item-name-row">
                    <span class="plan-item-name">{{ plan.planName }}</span>
                    <span v-if="plan.isActive" class="badge-active">Active</span>
                  </span>
                  <span class="plan-item-meta">
                    {{ plan.rugbyPosition }} · {{ goalLabel(plan.goal) }} ·
                    {{ plan.sessionsPerWeek }}×/week
                  </span>
                  <span class="plan-item-progress" v-if="listProgress[plan.id]?.done">
                    <span class="progress-bar-track">
                      <span
                        class="progress-bar-fill"
                        :style="{ transform: `scaleX(${listProgress[plan.id].percent / 100})` }"></span>
                    </span>
                    <span class="progress-label">
                      {{ listProgress[plan.id].done }} of
                      {{ listProgress[plan.id].total }} done
                    </span>
                  </span>
                  <span class="plan-item-date">{{ formatDate(plan.createdAt) }}</span>
                </button>

                <div class="plan-item-actions">
                  <button
                    type="button"
                    class="icon-btn"
                    :aria-label="`Copy plan ${plan.planName}`"
                    @click="openCopyModal(plan)">
                    <IconCopy class="icon" />
                  </button>
                  <button
                    type="button"
                    class="icon-btn icon-btn-danger"
                    :aria-label="`Delete plan ${plan.planName}`"
                    @click="confirmDelete(plan)">
                    <IconTrash class="icon" />
                  </button>
                </div>
              </li>
            </ul>

            <!-- Pagination controls -->
            <nav v-if="totalPages > 1" class="pagination" aria-label="Saved plans pages">
              <button
                type="button"
                class="page-btn"
                :disabled="currentPage === 1"
                aria-label="Previous page"
                @click="prevPage">‹</button>
              <span class="page-info" aria-live="polite">
                Page {{ currentPage }} of {{ totalPages }}
              </span>
              <button
                type="button"
                class="page-btn"
                :disabled="currentPage === totalPages"
                aria-label="Next page"
                @click="nextPage">›</button>
            </nav>
          </div>
        </section>
      </div>

      <!-- ══════════════════════════════════════════
           RIGHT: Plan Display + Manage
           ══════════════════════════════════════════ -->
      <div class="plan-column">

        <!-- Empty state -->
        <div v-if="!workoutStore.activePlan && !workoutStore.generating" class="plan-empty">
          <h2 class="plan-empty-title">No plan selected</h2>
          <p>Generate a new plan, or choose a saved plan from the left.</p>
        </div>

        <!-- Generating state -->
        <div v-if="workoutStore.generating" class="plan-generating" aria-live="polite">
          <div class="ai-pulse" aria-hidden="true"></div>
          <h2 class="plan-generating-title">Building your plan…</h2>
          <p>
            Writing a personalised
            <strong>{{ form.sessionsPerWeek }}-session</strong> week
            for a <strong>{{ form.rugbyPosition || 'rugby player' }}</strong>.
          </p>
          <div class="progress-dots" aria-hidden="true">
            <span></span><span></span><span></span>
          </div>
        </div>

        <!-- Plan content -->
        <section
          v-if="workoutStore.activePlan && !workoutStore.generating"
          class="plan-display card"
          aria-labelledby="plan-heading">

          <!-- Errors from actions in THIS column surface here, next to the
               control that caused them, rather than in the form far above. -->
          <transition name="fade">
            <div v-if="planError" class="alert alert-error" role="alert">
              <span>{{ planError }}</span>
              <button
                type="button"
                class="alert-close"
                aria-label="Dismiss error"
                @click="planError = ''">×</button>
            </div>
          </transition>

          <!-- Plan header -->
          <div class="plan-display-header">
            <div class="plan-header-left">
              <div v-if="editMode" class="form-group plan-name-group">
                <label class="form-label" for="wk-edit-name">Plan name</label>
                <div class="plan-name-row">
                  <input
                    id="wk-edit-name"
                    v-model="editForm.planName"
                    class="form-input plan-name-input"
                    :aria-invalid="!!editErrors.planName"
                    :aria-describedby="editErrors.planName ? 'wk-edit-name-error' : undefined"
                    placeholder="Plan name" />
                  <div class="plan-name-actions">
                    <button
                      type="button"
                      class="btn btn-save"
                      :disabled="saving"
                      @click="handleSaveEdit">
                      <span v-if="saving" class="spinner spinner-sm" aria-hidden="true"></span>
                      <span>{{ saving ? 'Saving…' : 'Save' }}</span>
                    </button>
                    <button type="button" class="btn btn-ghost" @click="cancelEdit">
                      Cancel
                    </button>
                  </div>
                </div>
                <p v-if="editErrors.planName" class="field-error" id="wk-edit-name-error">
                  {{ editErrors.planName }}
                </p>
              </div>
              <h2 v-else class="plan-display-title" id="plan-heading">
                {{ workoutStore.activePlan.planName }}
                <span v-if="workoutStore.activePlan.isActive" class="title-active-badge">Active</span>
              </h2>

              <ul class="plan-tags" v-if="!editMode">
                <li class="tag tag-position">{{ workoutStore.activePlan.rugbyPosition }}</li>
                <li class="tag tag-goal">{{ goalLabel(workoutStore.activePlan.goal) }}</li>
                <li class="tag tag-level">{{ levelLabel(workoutStore.activePlan.trainingLevel) }}</li>
                <li class="tag tag-sessions">{{ workoutStore.activePlan.sessionsPerWeek }}×/week</li>
                <li class="tag tag-phase">{{ phaseLabel(workoutStore.activePlan.trainingPhase) }}</li>
              </ul>
              <p class="plan-stats-line" v-if="!editMode">
                {{ workoutStore.activePlan.weight }}kg ·
                {{ workoutStore.activePlan.height }}cm ·
                {{ workoutStore.activePlan.age }}yo
              </p>
              <p
                class="plan-injury-line"
                v-if="!editMode && workoutStore.activePlan.injuryNotes">
                Injury notes: {{ workoutStore.activePlan.injuryNotes }}
              </p>
            </div>

            <!-- Action buttons (view mode; editing keeps Save/Cancel beside the name) -->
            <div class="plan-header-actions" v-if="!editMode">
              <button
                v-if="!editMode && !workoutStore.activePlan.isActive"
                type="button"
                class="btn btn-activate"
                :disabled="activating"
                @click="handleActivate">
                <span v-if="activating" class="spinner spinner-sm" aria-hidden="true"></span>
                <span>{{ activating ? 'Setting active…' : 'Set Active' }}</span>
              </button>
              <span v-if="!editMode && workoutStore.activePlan.isActive" class="active-indicator">
                Currently active
              </span>

              <button
                v-if="!editMode"
                id="wk-edit-btn"
                type="button"
                class="btn btn-ghost"
                @click="startEdit">
                Edit
              </button>
            </div>
          </div>

          <!-- ── Tabs: Plan / Progress ───────────────────────── -->
          <div class="plan-tabs" v-if="!editMode" role="tablist" aria-label="Plan views">
            <button
              type="button"
              class="plan-tab"
              role="tab"
              id="tab-plan"
              :aria-controls="activeTab === 'plan' ? 'panel-plan' : undefined"
              :aria-selected="activeTab === 'plan'"
              :tabindex="activeTab === 'plan' ? 0 : -1"
              :class="{ active: activeTab === 'plan' }"
              @click="activeTab = 'plan'"
              @keydown="onTabKeydown">
              Plan
            </button>
            <button
              type="button"
              class="plan-tab"
              role="tab"
              id="tab-progress"
              :aria-controls="activeTab === 'progress' ? 'panel-progress' : undefined"
              :aria-selected="activeTab === 'progress'"
              :tabindex="activeTab === 'progress' ? 0 : -1"
              :class="{ active: activeTab === 'progress' }"
              @click="activeTab = 'progress'"
              @keydown="onTabKeydown">
              Progress
              <span class="tab-badge" v-if="stats.done">{{ stats.done }}</span>
            </button>
          </div>

          <!-- ── Plan Tab ───────────────────────────────────────── -->
          <div
            v-if="!editMode && activeTab === 'plan'"
            class="plan-body"
            role="tabpanel"
            id="panel-plan"
            aria-labelledby="tab-plan"
            tabindex="0">
            <div class="plan-markdown" v-html="renderedPlan"></div>
          </div>

          <!-- ── Edit mode ──────────────────────────────────────── -->
          <div v-if="editMode" class="plan-body edit-body">
            <PlanEditor
              v-model="editForm.generatedPlan"
              id-prefix="wk"
              :parse="countExercises"
              :noun="{ one: 'exercise', many: 'exercises' }"
              hint='Edit any part of the plan. Keep the "Weekly Structure Summary" table so progress tracking can still read your exercises.'
              zero-hint='Check your "Weekly Structure Summary" table.'
              placeholder="Edit your workout plan here…"
              :can-restore="!!workoutStore.activePlan.originalPlan"
              :error="editErrors.generatedPlan || ''"
              @save="handleSaveEdit"
              @cancel="cancelEdit"
              @restore="restoreOpen = true" />
          </div>

          <!-- ── Progress Tab ───────────────────────────────────── -->
          <div
            v-if="!editMode && activeTab === 'progress'"
            class="plan-body progress-body"
            role="tabpanel"
            id="panel-progress"
            aria-labelledby="tab-progress"
            tabindex="0">

            <!-- Overall progress summary -->
            <div class="progress-summary">
              <div
                class="progress-ring-wrap"
                role="img"
                :aria-label="`${stats.percent}% complete: ${stats.done} of ${stats.total} exercises`">
                <svg class="progress-ring" viewBox="0 0 60 60" aria-hidden="true">
                  <circle cx="30" cy="30" r="24" fill="none"
                    stroke="var(--color-border-2)" stroke-width="6"/>
                  <circle cx="30" cy="30" r="24" fill="none"
                    stroke="var(--color-green)" stroke-width="6"
                    stroke-linecap="round"
                    :stroke-dasharray="RING_CIRCUMFERENCE"
                    :stroke-dashoffset="ringOffset"
                    transform="rotate(-90 30 30)"/>
                </svg>
                <span class="progress-ring-label" aria-hidden="true">{{ stats.percent }}%</span>
              </div>
              <div class="progress-summary-text">
                <h3 class="progress-summary-title">Exercise progress</h3>
                <p class="progress-summary-sub" aria-live="polite">
                  {{ stats.done }} of {{ stats.total }} exercises completed
                </p>
                <button
                  type="button"
                  class="btn btn-ghost btn-sm"
                  v-if="stats.done"
                  @click="handleResetProgress">
                  Reset progress
                </button>
              </div>
            </div>

            <!-- Logged items that no longer match the edited plan -->
            <p v-if="stats.orphaned > 0" class="orphan-note" role="status">
              <template v-if="stats.orphaned === 1">
                1 logged exercise no longer appears in this plan, so it is not counted.
                Reset progress to clear it.
              </template>
              <template v-else>
                {{ stats.orphaned }} logged exercises no longer appear in this plan, so they
                are not counted. Reset progress to clear them.
              </template>
            </p>

            <!-- Exercise checklist grouped by day -->
            <div v-if="stats.total > 0" class="checklist-grouped">
              <section
                v-for="day in groupedDays"
                :key="day.label"
                class="checklist-day"
                :aria-label="day.label">

                <div class="checklist-day-header">
                  <h3 class="checklist-day-title">{{ day.label }}</h3>
                  <div class="checklist-day-right">
                    <span class="checklist-day-focus" v-if="day.focus">{{ day.focus }}</span>
                    <span class="checklist-day-count">
                      {{ day.exercises.filter(isCompleted).length }}/{{ day.exercises.length }}
                    </span>
                  </div>
                </div>

                <button
                  v-for="key in day.exercises"
                  :key="key"
                  type="button"
                  class="checklist-item"
                  role="checkbox"
                  :aria-checked="isCompleted(key)"
                  :class="{ completed: isCompleted(key), pending: pendingKeys.has(key) }"
                  @click="toggleItem(key)">
                  <span class="checklist-check" aria-hidden="true">
                    <svg v-if="isCompleted(key)" viewBox="0 0 16 16" class="check-mark">
                      <path d="M3.5 8.5l3 3 6-7" fill="none" stroke="currentColor"
                        stroke-width="2.25" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </span>
                  <span class="checklist-label">{{ itemNameOnly(key) }}</span>
                </button>
              </section>
            </div>

            <p v-else class="checklist-empty">
              No exercises found to track. This plan needs a "Weekly Structure Summary"
              table with Day and Key Exercises columns — regenerate the plan, or add one
              in Edit.
            </p>

          </div>
        </section>
      </div>
    </div>

    <!-- Unsaved-changes guard -->
    <BaseModal
      v-if="pendingNav"
      title="Discard your changes?"
      initial-focus=".btn-ghost"
      @close="keepEditing">
      <p>
        You have unsaved edits to <strong>{{ workoutStore.activePlan?.planName }}</strong>.
        Leaving the editor now will discard them.
      </p>
      <template #actions>
        <button type="button" class="btn btn-ghost" @click="keepEditing">
          Keep editing
        </button>
        <button type="button" class="btn btn-danger" @click="confirmDiscard">
          Discard changes
        </button>
      </template>
    </BaseModal>

    <!-- Restore original confirmation -->
    <BaseModal
      v-if="restoreOpen"
      title="Restore original plan?"
      initial-focus=".btn-ghost"
      @close="restoreOpen = false">
      <p>
        This replaces the editor contents with the plan as it was before your
        first edit. Your current text will be lost unless you cancel.
      </p>
      <template #actions>
        <button type="button" class="btn btn-ghost" @click="restoreOpen = false">Cancel</button>
        <button type="button" class="btn btn-primary" @click="restoreOriginal">
          Restore original
        </button>
      </template>
    </BaseModal>

    <!-- Delete confirmation modal -->
    <BaseModal
      v-if="deleteTarget"
      title="Delete plan"
      initial-focus=".btn-ghost"
      @close="deleteTarget = null">
      <p>
        Delete <strong>{{ deleteTarget.planName }}</strong>?
        This cannot be undone.
      </p>
      <template #actions>
        <button type="button" class="btn btn-ghost" @click="deleteTarget = null">Cancel</button>
        <button type="button" class="btn btn-danger" :disabled="deleting" @click="handleDelete">
          <span v-if="deleting" class="spinner spinner-sm" aria-hidden="true"></span>
          <span>{{ deleting ? 'Deleting…' : 'Delete plan' }}</span>
        </button>
      </template>
    </BaseModal>

    <!-- Copy plan modal -->
    <BaseModal
      v-if="copySource"
      title="Copy plan"
      wide
      initial-focus="#wk-copy-name"
      @close="closeCopyModal">
      <p>Duplicating <strong>{{ copySource.planName }}</strong>.</p>

      <div class="copy-plan-section">
        <h3 class="copy-section-label" id="wk-copy-source-label">Or copy a different plan</h3>
        <ul class="copy-plan-list" aria-labelledby="wk-copy-source-label">
          <li v-for="p in workoutStore.plans" :key="p.id">
            <button
              type="button"
              class="copy-plan-item"
              :class="{ selected: copySource.id === p.id }"
              :aria-pressed="copySource.id === p.id"
              @click="selectCopySource(p)">
              <span class="copy-plan-name">{{ p.planName }}</span>
              <span class="copy-plan-meta">{{ p.rugbyPosition }} · {{ p.sessionsPerWeek }}×/week</span>
            </button>
          </li>
        </ul>
      </div>

      <div class="form-group">
        <label class="form-label" for="wk-copy-name">New plan name</label>
        <input
          id="wk-copy-name"
          v-model="copyName"
          class="form-input"
          placeholder="e.g. Pre-Season Copy"
          maxlength="80"
          :aria-invalid="!!copyError"
          :aria-describedby="copyError ? 'wk-copy-error' : undefined"
          @keydown.enter="handleCopy" />
        <p v-if="copyError" class="field-error" id="wk-copy-error">{{ copyError }}</p>
      </div>

      <template #actions>
        <button type="button" class="btn btn-ghost" @click="closeCopyModal">Cancel</button>
        <button type="button" class="btn btn-primary" :disabled="copying" @click="handleCopy">
          <span v-if="copying" class="spinner spinner-sm" aria-hidden="true"></span>
          <span>{{ copying ? 'Copying…' : 'Copy plan' }}</span>
        </button>
      </template>
    </BaseModal>

  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useWorkoutStore } from '@/stores/workout'
import { useProfileStore } from '@/stores/profile'
import BaseModal from '@/components/BaseModal.vue'
import PlanEditor from '@/components/PlanEditor.vue'
import IconCopy from '@/components/icons/IconCopy.vue'
import IconTrash from '@/components/icons/IconTrash.vue'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'
import {
  renderMarkdown,
  parseWorkoutStructure,
  groupExerciseKeys,
  completionStats,
  itemNameOnly
} from '@/utils/markdown'

const workoutStore = useWorkoutStore()
const profileStore = useProfileStore()

/** 2 * PI * r, for r = 24 — the progress ring's path length. */
const RING_CIRCUMFERENCE = (2 * Math.PI * 24).toFixed(1)

// ── Form state ───────────────────────────────────────────────────────────────
const form = ref({
  planName: '',
  rugbyPosition: '',
  goal: '',
  trainingLevel: '',
  weight: null,
  height: null,
  age: null,
  injuryNotes: '',
  sessionsPerWeek: 4,
  trainingPhase: '',
  availableEquipment: '',
  focusArea: ''
})

const errors = ref({})
const deleteTarget = ref(null)
const deleting = ref(false)

/** Errors raised by actions in the plan column, shown beside those actions. */
const planError = ref('')

// ── Copy modal state ─────────────────────────────────────────────────────────
const copySource = ref(null)
const copyName = ref('')
const copying = ref(false)
const copyError = ref('')

// ── Tabs ─────────────────────────────────────────────────────────────────────
const activeTab = ref('plan')

function onTabKeydown(event) {
  if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
  event.preventDefault()
  activeTab.value = activeTab.value === 'plan' ? 'progress' : 'plan'
  const next = activeTab.value === 'plan' ? 'tab-plan' : 'tab-progress'
  document.getElementById(next)?.focus()
}

// ── Edit mode ────────────────────────────────────────────────────────────────
const editMode = ref(false)
const saving = ref(false)
const activating = ref(false)
const editForm = ref({ planName: '', generatedPlan: '' })
const editErrors = ref({})

/** Snapshot of the text as the editor was opened, for the dirty check. */
const editBaseline = ref({ planName: '', generatedPlan: '' })
const restoreOpen = ref(false)

const isDirty = computed(() =>
  editMode.value && (
    editForm.value.planName !== editBaseline.value.planName ||
    editForm.value.generatedPlan !== editBaseline.value.generatedPlan
  )
)

function closeEditor() {
  editMode.value = false
  editErrors.value = {}
  restoreOpen.value = false
  // Focus would otherwise fall to <body>, sending the user back to the top of
  // the page. The Edit button only exists again after the re-render.
  nextTick(() => document.getElementById('wk-edit-btn')?.focus())
}

const { pending: pendingNav, guard: guardUnsaved, discard: confirmDiscard, keep: keepEditing } =
  useUnsavedGuard({ isDirty, onDiscard: closeEditor })

function startEdit() {
  editForm.value.planName = workoutStore.activePlan.planName
  editForm.value.generatedPlan = workoutStore.activePlan.generatedPlan
  editBaseline.value = { ...editForm.value }
  editErrors.value = {}
  planError.value = ''
  editMode.value = true
}

function cancelEdit() {
  guardUnsaved(closeEditor)
}

function restoreOriginal() {
  const original = workoutStore.activePlan?.originalPlan
  if (original) editForm.value.generatedPlan = original
  restoreOpen.value = false
}


async function handleSaveEdit() {
  editErrors.value = {}
  if (!editForm.value.planName.trim()) {
    editErrors.value.planName = 'Give the plan a name before saving.'
  }
  if (!editForm.value.generatedPlan.trim()) {
    editErrors.value.generatedPlan = 'Plan content cannot be empty.'
  }
  if (Object.keys(editErrors.value).length) return

  saving.value = true
  planError.value = ''
  workoutStore.clearError()

  const result = await workoutStore.editPlan(workoutStore.activePlan.id, {
    planName: editForm.value.planName.trim(),
    generatedPlan: editForm.value.generatedPlan.trim()
  })
  saving.value = false

  // Only leave edit mode when the save actually landed; otherwise the user's
  // unsaved text would be discarded with no indication of what went wrong.
  if (result) {
    editBaseline.value = { ...editForm.value }
    closeEditor()
  } else {
    planError.value = workoutStore.error || 'Could not save your changes. Please try again.'
    workoutStore.clearError()
  }
}

// ── Activate plan ────────────────────────────────────────────────────────────
async function handleActivate() {
  activating.value = true
  planError.value = ''
  workoutStore.clearError()

  const result = await workoutStore.activatePlan(workoutStore.activePlan.id)
  activating.value = false

  if (!result) {
    planError.value = workoutStore.error || 'Could not set this plan active. Please try again.'
    workoutStore.clearError()
  }
}

// ── Plan parsing (memoized) ──────────────────────────────────────────────────
// These used to be plain functions called straight from the template, so every
// render re-parsed the whole plan — about five full passes per tick, and again
// on every checklist toggle.

/** Trackable exercises in a candidate plan text — passed to PlanEditor. */
function countExercises(text) {
  return parseWorkoutStructure(text).keys.length
}

const planText = computed(() => workoutStore.activePlan?.generatedPlan || '')

const renderedPlan = computed(() => renderMarkdown(planText.value))

const structure = computed(() => parseWorkoutStructure(planText.value))

const groupedDays = computed(() =>
  groupExerciseKeys(structure.value.keys, structure.value.focusByDay)
)

// ── Progress ─────────────────────────────────────────────────────────────────
/**
 * Locally-held completion list. Toggles apply here first so the checkbox
 * responds immediately, then reconcile with the server.
 */
const localCompleted = ref([])
const pendingKeys = ref(new Set())

watch(
  () => workoutStore.activePlan,
  plan => { localCompleted.value = plan?.completedItems ? [...plan.completedItems] : [] },
  { immediate: true, deep: false }
)

const stats = computed(() => completionStats(localCompleted.value, structure.value.keys))

const ringOffset = computed(() => {
  const c = Number(RING_CIRCUMFERENCE)
  return c - (c * stats.value.percent) / 100
})

function isCompleted(key) {
  return localCompleted.value.includes(key)
}

// Writes are chained so two quick taps cannot land out of order.
let writeChain = Promise.resolve()

async function toggleItem(key) {
  const plan = workoutStore.activePlan
  if (!plan) return

  const before = [...localCompleted.value]
  localCompleted.value = before.includes(key)
    ? before.filter(k => k !== key)
    : [...before, key]

  const snapshot = [...localCompleted.value]
  pendingKeys.value = new Set(pendingKeys.value).add(key)

  writeChain = writeChain.then(async () => {
    const result = await workoutStore.updateProgress(plan.id, snapshot)

    const next = new Set(pendingKeys.value)
    next.delete(key)
    pendingKeys.value = next

    if (!result) {
      localCompleted.value = before
      planError.value = workoutStore.error || 'Could not save that change. Please try again.'
      workoutStore.clearError()
    }
  })

  await writeChain
}

async function handleResetProgress() {
  const plan = workoutStore.activePlan
  if (!plan) return

  const before = [...localCompleted.value]
  localCompleted.value = []

  const result = await workoutStore.updateProgress(plan.id, [])
  if (!result) {
    localCompleted.value = before
    planError.value = workoutStore.error || 'Could not reset progress. Please try again.'
    workoutStore.clearError()
  }
}

// ── Select plan from list ────────────────────────────────────────────────────
function selectPlan(plan) {
  // The saved-plans list sits beside the editor and is always clickable, so
  // this path has to respect the unsaved guard too.
  guardUnsaved(() => {
    workoutStore.setActivePlan(plan)
    activeTab.value = 'plan'
    editMode.value = false
    planError.value = ''
  })
}

// ── Pagination ───────────────────────────────────────────────────────────────
const PAGE_SIZE = 5
const currentPage = ref(1)

const totalPages = computed(() =>
  Math.max(1, Math.ceil(workoutStore.plans.length / PAGE_SIZE))
)

const paginatedPlans = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return workoutStore.plans.slice(start, start + PAGE_SIZE)
})

function prevPage() { if (currentPage.value > 1) currentPage.value-- }
function nextPage() { if (currentPage.value < totalPages.value) currentPage.value++ }

/** Per-plan completion for the saved-plans list — parsed once per page, not per render. */
const listProgress = computed(() => {
  const map = {}
  for (const plan of paginatedPlans.value) {
    const { keys } = parseWorkoutStructure(plan.generatedPlan)
    map[plan.id] = completionStats(plan.completedItems, keys)
  }
  return map
})

// ── Lifecycle ────────────────────────────────────────────────────────────────
onMounted(async () => {
  workoutStore.fetchPlans()

  if (!profileStore.profile) {
    await profileStore.fetchProfile()
  }
  const p = profileStore.profile
  if (!p) return

  if (p.rugbyPosition) form.value.rugbyPosition = p.rugbyPosition
  if (p.goal) form.value.goal = p.goal
  if (p.trainingLevel) form.value.trainingLevel = p.trainingLevel
  if (p.weight) form.value.weight = p.weight
  if (p.height) form.value.height = p.height
  if (p.age) form.value.age = p.age
  if (p.injuryNotes) form.value.injuryNotes = p.injuryNotes
})

// ── Validation ───────────────────────────────────────────────────────────────
function validate() {
  errors.value = {}
  if (!form.value.rugbyPosition) errors.value.rugbyPosition = 'Please select your rugby position.'
  if (!form.value.goal) errors.value.goal = 'Please select a goal.'
  if (!form.value.trainingLevel) errors.value.trainingLevel = 'Please select your training level.'
  if (!form.value.weight) errors.value.weight = 'Weight is required.'
  if (!form.value.height) errors.value.height = 'Height is required.'
  if (!form.value.age) errors.value.age = 'Age is required.'
  if (!form.value.sessionsPerWeek) errors.value.sessionsPerWeek = 'Please select sessions per week.'
  if (!form.value.trainingPhase) errors.value.trainingPhase = 'Please select a training phase.'
  if (!form.value.availableEquipment) {
    errors.value.availableEquipment = 'Please select available equipment.'
  }
  return Object.keys(errors.value).length === 0
}

function clearError(field) {
  if (errors.value[field]) delete errors.value[field]
  workoutStore.clearError()
}

// ── Generate ─────────────────────────────────────────────────────────────────
async function handleGenerate() {
  if (!validate()) {
    // Move focus to the first problem so keyboard and screen-reader users are
    // not left guessing why nothing happened.
    const firstField = Object.keys(errors.value)[0]
    const map = {
      rugbyPosition: 'wk-position', goal: 'wk-goal', trainingLevel: 'wk-level',
      weight: 'wk-weight', height: 'wk-height', age: 'wk-age',
      trainingPhase: 'wk-phase', availableEquipment: 'wk-equipment'
    }
    document.getElementById(map[firstField])?.focus()
    return
  }

  workoutStore.clearError()
  planError.value = ''
  activeTab.value = 'plan'
  editMode.value = false
  await workoutStore.generatePlan({ ...form.value })
  currentPage.value = 1
}

// ── Delete ───────────────────────────────────────────────────────────────────
function confirmDelete(plan) { deleteTarget.value = plan }

async function handleDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  workoutStore.clearError()

  await workoutStore.deletePlan(deleteTarget.value.id)
  deleting.value = false

  if (workoutStore.error) {
    planError.value = workoutStore.error
    workoutStore.clearError()
  }
  deleteTarget.value = null
}

// ── Copy ─────────────────────────────────────────────────────────────────────
function openCopyModal(plan) {
  copySource.value = plan
  copyName.value = `${plan.planName} (Copy)`
  copyError.value = ''
}

function selectCopySource(plan) {
  copySource.value = plan
  copyName.value = `${plan.planName} (Copy)`
  copyError.value = ''
}

function closeCopyModal() {
  copySource.value = null
  copyName.value = ''
  copyError.value = ''
}

async function handleCopy() {
  const name = copyName.value.trim()
  if (!name) {
    copyError.value = 'Enter a name for the copied plan.'
    return
  }

  copying.value = true
  copyError.value = ''
  const result = await workoutStore.copyPlan(copySource.value.id, name)
  copying.value = false

  if (result) {
    closeCopyModal()
    workoutStore.setActivePlan(result)
    currentPage.value = 1
  } else {
    copyError.value = workoutStore.error || 'Could not copy this plan. Please try again.'
    workoutStore.clearError()
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────
function goalLabel(goal) {
  const map = {
    STRENGTH: 'Strength', POWER: 'Power', ENDURANCE: 'Endurance', LEAN: 'Lean'
  }
  return map[goal] || goal
}

function levelLabel(level) {
  const map = { BEGINNER: 'Beginner', INTERMEDIATE: 'Intermediate', ADVANCED: 'Advanced' }
  return map[level] || level
}

function phaseLabel(phase) {
  const map = {
    PRE_SEASON: 'Pre-Season', IN_SEASON: 'In-Season',
    OFF_SEASON: 'Off-Season', POST_SEASON: 'Post-Season'
  }
  return map[phase] || phase || ''
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('en-MY', {
    day: 'numeric', month: 'short', year: 'numeric'
  })
}
</script>

<style scoped>
/* ── Page Layout ─────────────────────────────────────────────── */
.workout-page   { display: flex; flex-direction: column; gap: 24px; }
.page-header    { display: flex; align-items: flex-start; }
.page-title     { font-family: 'Barlow Condensed', sans-serif; font-size: 28px; font-weight: 700; color: var(--color-green-light); }
.page-subtitle  { color: var(--color-muted); font-size: 13.5px; margin-top: 4px; }

.workout-layout {
  display: grid;
  grid-template-columns: 370px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

/* ── Card ────────────────────────────────────────────────────── */
.card {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 22px;
}
.card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; }
.card-title  { font-family: 'Barlow Condensed', sans-serif; font-size: 18px; font-weight: 700; flex: 1; }
.plan-count  { background: var(--color-green-dim); color: var(--color-green-on-tint); font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 99px; }

/* ── Form ────────────────────────────────────────────────────── */
.generate-form  { display: flex; flex-direction: column; gap: 16px; }
.form-row       { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-row.three-col { grid-template-columns: 1fr 1fr 1fr; }
.form-group     { display: flex; flex-direction: column; gap: 5px; }
.fieldset-reset { border: none; padding: 0; margin: 0; min-width: 0; }
.form-label     { font-size: 13px; font-weight: 500; padding: 0; }
.req            { color: var(--color-error); }
.optional-tag   { font-size: 11px; color: var(--color-muted); font-weight: 400; }

.form-input, .form-select, .form-textarea {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  font-size: 13.5px;
  padding: 9px 12px;
  width: 100%;
  min-height: 38px;
  transition: border-color var(--transition);
  font-family: inherit;
  box-sizing: border-box;
}
.form-input::placeholder,
.form-textarea::placeholder { color: var(--color-muted); }
.form-input:focus, .form-select:focus, .form-textarea:focus {
  outline: none; border-color: var(--color-green-light);
}
.form-textarea  { resize: vertical; min-height: 60px; }
.form-select    { cursor: pointer; }

.form-group.has-error .form-input,
.form-group.has-error .form-select { border-color: var(--color-error); }
.field-error    { font-size: 12px; color: var(--color-error); }
.field-hint     { font-size: 11.5px; color: var(--color-muted); }

/* Sessions selector */
.sessions-selector { display: flex; gap: 8px; flex-wrap: wrap; }
.session-btn {
  width: var(--touch-target); height: var(--touch-target);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-dim);
  font-size: 14px; font-weight: 600;
  cursor: pointer;
  transition: border-color var(--transition), color var(--transition), background var(--transition);
  font-family: inherit;
}
.session-btn:hover  { border-color: var(--color-green); color: var(--color-green-on-tint); }
.session-btn.active {
  background: var(--color-green-dim);
  border-color: var(--color-green);
  color: var(--color-green-on-tint);
}

/* Generate button */
.btn-generate {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  width: 100%; padding: 13px;
  min-height: var(--touch-target);
  background: var(--color-green); color: #fff;
  font-size: 14px; font-weight: 600;
  border: none; border-radius: var(--radius-sm);
  cursor: pointer; transition: opacity var(--transition);
  font-family: inherit;
}
.btn-generate:disabled { opacity: 0.65; cursor: not-allowed; }
.btn-generate:not(:disabled):hover { opacity: 0.9; }

.generating-hint { font-size: 12px; color: var(--color-muted); text-align: center; }

/* ── Saved Plans ─────────────────────────────────────────────── */
.saved-plans-card { margin-top: 16px; }
.plans-list { display: flex; flex-direction: column; gap: 8px; list-style: none; }

.plan-item {
  display: flex; align-items: stretch; gap: 6px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  transition: border-color var(--transition), background var(--transition);
}
.plan-item:hover { border-color: var(--color-green); }
.plan-item.is-selected { border-color: var(--color-green); background: var(--color-green-dim); }
/* Active plan reads from the "Active" badge rather than an accent stripe. */
.plan-item.is-current { box-shadow: inset 0 0 0 1px var(--color-green); }

.plan-item-select {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column; gap: 2px;
  padding: 12px; text-align: left;
  background: none; border: none; cursor: pointer;
  font-family: inherit; color: inherit;
  border-radius: var(--radius-sm);
}
.plan-item-name-row { display: flex; align-items: center; gap: 6px; }
.plan-item-name {
  font-size: 13.5px; font-weight: 500; color: var(--color-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.badge-active {
  font-size: 9px; font-weight: 700; letter-spacing: 0.5px; text-transform: uppercase;
  padding: 2px 6px; border-radius: 99px;
  background: var(--color-green); color: #fff; flex-shrink: 0;
}
.plan-item-meta { font-size: 11.5px; color: var(--color-muted); }
.plan-item-date { font-size: 11px; color: var(--color-muted); margin-top: 1px; }
/* On the green-tinted selected row, muted grey drops to 2.5:1. */
.plan-item.is-selected .plan-item-meta,
.plan-item.is-selected .plan-item-date,
.plan-item.is-selected .progress-label { color: var(--color-text-dim); }

/* Progress bar in list */
.plan-item-progress { display: flex; align-items: center; gap: 6px; margin-top: 5px; }
.progress-bar-track {
  flex: 1; height: 4px; border-radius: 99px;
  background: var(--color-border-2); overflow: hidden;
}
.progress-bar-fill {
  display: block; height: 100%; width: 100%;
  background: var(--color-green); border-radius: 99px;
  transform-origin: left center;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.progress-label { font-size: 10px; color: var(--color-muted); white-space: nowrap; }

.plan-item-actions {
  display: flex; flex-direction: column; justify-content: center;
  gap: 8px; flex-shrink: 0; padding: 6px 6px 6px 0;
}
.icon-btn {
  width: var(--touch-target); height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: none; border: 1px solid transparent; cursor: pointer;
  border-radius: var(--radius-sm);
  color: var(--color-text-dim);
  transition: color var(--transition), background var(--transition), border-color var(--transition);
}
.icon-btn .icon { width: 17px; height: 17px; }
.icon-btn:hover { background: var(--color-surface-2); color: var(--color-text); }
.icon-btn-danger:hover {
  color: var(--color-error);
  background: var(--color-error-bg);
  border-color: var(--color-error-border);
}

/* ── Pagination ──────────────────────────────────────────────── */
.pagination {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; padding-top: 12px; margin-top: 10px;
  border-top: 1px solid var(--color-border);
}
.page-btn {
  width: var(--touch-target); height: 36px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border); background: var(--color-surface);
  color: var(--color-text); font-size: 16px; font-weight: 600;
  cursor: pointer; transition: border-color var(--transition), color var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.page-btn:hover:not(:disabled) { border-color: var(--color-green); color: var(--color-green-on-tint); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 12.5px; color: var(--color-muted); text-align: center; }

/* ── States ──────────────────────────────────────────────────── */
.loading-state, .empty-state {
  display: flex; flex-direction: column; align-items: center;
  padding: 24px; gap: 10px;
  color: var(--color-muted); font-size: 13.5px; text-align: center;
}
.spinner-muted {
  width: 20px; height: 20px;
  border: 2px solid var(--color-border-2);
  border-top-color: var(--color-green-light);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* ── Plan Column ─────────────────────────────────────────────── */
.plan-column { min-width: 0; }

.plan-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 360px;
  background: var(--color-bg-2);
  border: 1px dashed var(--color-border-2);
  border-radius: var(--radius-lg);
  color: var(--color-muted); text-align: center; gap: 8px; padding: 40px;
}
.plan-empty-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 20px; font-weight: 700; color: var(--color-text-dim);
}
.plan-empty p { font-size: 13.5px; max-width: 34ch; }

.plan-generating {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 380px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  text-align: center; gap: 16px; padding: 40px;
}
.plan-generating-title {
  font-family: 'Barlow Condensed', sans-serif; font-size: 22px; font-weight: 700;
}
.plan-generating p { font-size: 14px; color: var(--color-muted); max-width: 38ch; line-height: 1.6; }

.ai-pulse {
  width: 56px; height: 56px; border-radius: 50%;
  background: var(--color-green-dim);
  border: 1px solid var(--color-green);
  animation: pulse 1.8s cubic-bezier(0.22, 1, 0.36, 1) infinite;
}
@keyframes spin  { to { transform: rotate(360deg); } }
@keyframes pulse {
  0%, 100% { transform: scale(1);    opacity: 1;   }
  50%      { transform: scale(1.08); opacity: 0.7; }
}

.progress-dots { display: flex; gap: 6px; }
.progress-dots span {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--color-green-light);
  animation: dot-fade 1.4s ease-in-out infinite;
}
.progress-dots span:nth-child(2) { animation-delay: 0.18s; }
.progress-dots span:nth-child(3) { animation-delay: 0.36s; }
@keyframes dot-fade { 0%, 100% { opacity: 0.25; } 50% { opacity: 1; } }

/* Looping decorative motion is stilled here rather than globally: a scoped
   `.ai-pulse[data-v-x]` out-specifies a bare global `.ai-pulse`. The spinner
   keeps turning — it is the only in-progress signal left. */
@media (prefers-reduced-motion: reduce) {
  .ai-pulse { animation: none; }
  .progress-dots span { animation: none; opacity: 0.55; }
  .progress-bar-fill { transition: none; }
  .progress-ring circle { transition: none; }
  .checklist-item, .session-btn, .plan-item { transition: none; }
}

/* ── Plan Display ────────────────────────────────────────────── */
.plan-display { padding: 28px; }

.plan-display-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  gap: 16px; padding-bottom: 18px;
  border-bottom: 1px solid var(--color-border);
}
.plan-header-left { flex: 1; min-width: 0; }
.plan-header-actions {
  display: flex; align-items: center; gap: 8px;
  flex-shrink: 0; flex-wrap: wrap; justify-content: flex-end;
}

.plan-display-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 22px; font-weight: 700; margin-bottom: 10px;
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}
.title-active-badge {
  font-size: 10px; font-weight: 700; letter-spacing: 0.5px; text-transform: uppercase;
  padding: 3px 8px; border-radius: 99px;
  background: var(--color-green); color: #fff;
  font-family: 'DM Sans', sans-serif;
}
.plan-name-input { font-size: 17px; font-weight: 600; }

/* Save and Cancel ride on the same line as the name field, matching its
   height exactly via stretch rather than a guessed pixel value. */
.plan-name-group { width: 100%; }
.plan-name-row {
  display: flex; align-items: stretch; gap: 8px; flex-wrap: wrap;
}
.plan-name-row .form-input { flex: 1 1 220px; min-width: 0; }
.plan-name-actions { display: flex; align-items: stretch; gap: 8px; flex-shrink: 0; }
.plan-name-actions .btn { min-height: 0; }

.plan-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; list-style: none; }
.tag { font-size: 11.5px; font-weight: 500; padding: 3px 10px; border-radius: 99px; }
.tag-position { background: var(--color-green-dim); color: var(--color-green-on-tint); }
.tag-goal     { background: var(--tag-goal-bg);  color: var(--tag-goal-fg);  }
.tag-level    { background: var(--tag-level-bg); color: var(--tag-level-fg); }
.tag-phase    { background: var(--tag-phase-bg); color: var(--tag-phase-fg); }
.tag-sessions {
  background: var(--color-surface); color: var(--color-text-dim);
  border: 1px solid var(--color-border);
}
.plan-stats-line  { font-size: 12px; color: var(--color-muted); }
.plan-injury-line {
  font-size: 12px; color: var(--tag-level-fg); margin-top: 3px;
}

/* Action buttons */
.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  padding: 9px 14px; min-height: 38px;
  border-radius: var(--radius-sm);
  font-size: 13px; font-weight: 500;
  cursor: pointer; border: none; font-family: inherit;
  transition: background var(--transition), opacity var(--transition), color var(--transition);
  white-space: nowrap;
}
.btn-ghost { background: var(--color-surface); color: var(--color-text); border: 1px solid var(--color-border); }
.btn-ghost:hover { background: var(--color-surface-2); }
.btn-activate {
  background: var(--color-green-dim); color: var(--color-green-on-tint);
  border: 1px solid var(--color-green);
}
.btn-activate:hover:not(:disabled) { background: var(--color-green); color: #fff; }
.btn-activate:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-save { background: var(--color-green); color: #fff; }
.btn-save:hover:not(:disabled) { opacity: 0.85; }
.btn-save:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-danger { background: var(--color-error); color: #2a0f0f; font-weight: 600; }
.btn-danger:hover:not(:disabled) { opacity: 0.88; }
.btn-danger:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-primary { background: var(--color-green); color: #fff; }
.btn-primary:hover:not(:disabled) { opacity: 0.88; }
.btn-primary:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-sm { padding: 6px 10px; min-height: 36px; font-size: 12px; }
/* Touch input gets the full target; a mouse does not need it. */
@media (pointer: coarse) {
  .btn-sm { min-height: var(--touch-target); }
}
.active-indicator {
  font-size: 12.5px; color: var(--color-green-on-tint); font-weight: 500;
}

/* ── Tabs ────────────────────────────────────────────────────── */
.plan-tabs {
  display: flex; gap: 2px; margin-top: 18px;
  border-bottom: 1px solid var(--color-border);
}
.plan-tab {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 11px 16px; min-height: var(--touch-target);
  font-size: 13.5px; font-weight: 500;
  background: none; border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-muted);
  cursor: pointer; font-family: inherit;
  margin-bottom: -1px;
  transition: color var(--transition), border-color var(--transition);
}
.plan-tab:hover { color: var(--color-text); }
.plan-tab[aria-selected="true"] {
  color: var(--color-green-on-tint);
  border-bottom-color: var(--color-green);
}
.tab-badge {
  background: var(--color-green); color: #fff;
  font-size: 10px; font-weight: 700;
  padding: 1px 6px; border-radius: 99px;
}

/* ── Plan Body ───────────────────────────────────────────────── */
.plan-body { padding-top: 20px; min-width: 0; }
.plan-body:focus-visible { outline: 2px solid var(--color-green-light); outline-offset: 4px; }


/* ── Edit mode ───────────────────────────────────────────────── */
/* The editor itself is PlanEditor.vue; this only reserves its slot. */
.edit-body { padding-top: 20px; }

/* ── Progress Body ───────────────────────────────────────────── */
.progress-summary {
  display: flex; align-items: center; gap: 20px;
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
}
.progress-ring-wrap { position: relative; width: 60px; height: 60px; flex-shrink: 0; }
.progress-ring { width: 60px; height: 60px; }
.progress-ring circle:last-child { transition: stroke-dashoffset 0.4s cubic-bezier(0.22, 1, 0.36, 1); }
.progress-ring-label {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; color: var(--color-green-on-tint);
  font-variant-numeric: tabular-nums;
}
.progress-summary-text { flex: 1; min-width: 0; }
.progress-summary-title { font-size: 14px; font-weight: 600; margin-bottom: 3px; }
.progress-summary-sub { font-size: 12.5px; color: var(--color-muted); margin-bottom: 10px; }

.orphan-note {
  font-size: 12.5px; line-height: 1.55;
  color: var(--color-warn);
  background: var(--color-warn-bg);
  border: 1px solid var(--color-warn-border);
  border-radius: var(--radius-sm);
  padding: 10px 14px; margin-bottom: 16px;
}

/* ── Checklist (grouped by day) ──────────────────────────────── */
.checklist-grouped { display: flex; flex-direction: column; gap: 16px; }

.checklist-day {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.checklist-day-header {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  padding: 10px 14px;
  background: var(--color-bg-2);
  border-bottom: 1px solid var(--color-border);
}
.checklist-day-title { font-size: 13px; font-weight: 600; color: var(--color-green-on-tint); }
.checklist-day-right { display: flex; align-items: center; gap: 10px; min-width: 0; }
.checklist-day-focus {
  font-size: 11.5px; color: var(--color-muted);
  max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.checklist-day-count {
  font-size: 12px; color: var(--color-muted);
  flex-shrink: 0; font-variant-numeric: tabular-nums;
}

.checklist-item {
  display: flex; align-items: center; gap: 12px;
  width: 100%; padding: 12px 14px; min-height: var(--touch-target);
  text-align: left; font-family: inherit;
  background: none; border: none;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer; user-select: none;
  transition: background var(--transition);
}
.checklist-item:last-child { border-bottom: none; }
.checklist-item:hover { background: var(--color-bg-2); }
.checklist-item.completed { background: var(--color-green-dim); }
.checklist-item.pending { opacity: 0.6; }

.checklist-check {
  width: 20px; height: 20px;
  border: 2px solid var(--color-border-2);
  border-radius: 5px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0;
  transition: background var(--transition), border-color var(--transition);
}
.check-mark { width: 14px; height: 14px; }
.checklist-item.completed .checklist-check {
  background: var(--color-green);
  border-color: var(--color-green);
}
.checklist-label { font-size: 14px; font-weight: 500; flex: 1; color: var(--color-text); }
.checklist-item.completed .checklist-label {
  /* --color-muted drops to 2.5:1 on the green-tinted completed row. */
  color: var(--color-text-dim);
  text-decoration: line-through;
  text-decoration-thickness: 1px;
  text-underline-offset: 2px;
}
.checklist-empty {
  font-size: 13.5px; color: var(--color-muted);
  text-align: center; padding: 24px 16px; line-height: 1.6;
}

/* ── Markdown ────────────────────────────────────────────────── */
.plan-markdown :deep(.md-h2) {
  font-family: 'Barlow Condensed', sans-serif; font-size: 20px; font-weight: 700;
  color: var(--color-green-on-tint); margin: 28px 0 12px;
  padding-bottom: 6px; border-bottom: 1px solid var(--color-border);
}
.plan-markdown :deep(.md-h2:first-child) { margin-top: 0; }
.plan-markdown :deep(.md-h3) { font-size: 15px; font-weight: 600; color: var(--color-text); margin: 18px 0 8px; }
.plan-markdown :deep(.md-h4) { font-size: 13.5px; font-weight: 600; color: var(--color-text-dim); margin: 12px 0 6px; }
.plan-markdown :deep(.md-p)  { font-size: 14px; line-height: 1.7; color: var(--color-text-dim); margin: 8px 0; max-width: 72ch; }
.plan-markdown :deep(.md-ul) { padding-left: 18px; margin: 8px 0; display: flex; flex-direction: column; gap: 5px; }
.plan-markdown :deep(li)     { font-size: 14px; line-height: 1.6; color: var(--color-text-dim); max-width: 72ch; }
.plan-markdown :deep(.md-table-wrap) {
  display: block; width: 100%; overflow-x: auto; -webkit-overflow-scrolling: touch;
}
.plan-markdown :deep(.md-table) {
  width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 13.5px;
  border: 1px solid var(--color-border); border-radius: var(--radius-sm); overflow: hidden;
}
.plan-markdown :deep(.md-table th) {
  padding: 9px 12px; text-align: left; font-weight: 600; color: var(--color-text);
  background: var(--color-surface); border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}
.plan-markdown :deep(.md-table td) {
  padding: 8px 12px; border-top: 1px solid var(--color-border);
  color: var(--color-text-dim); font-variant-numeric: tabular-nums;
}
.plan-markdown :deep(.md-table tbody tr:nth-child(even)) { background: var(--color-bg-2); }
.plan-markdown :deep(.md-table tbody tr:hover td) { background: var(--color-bg-3); }
.plan-markdown :deep(.md-hr) { border: none; border-top: 1px solid var(--color-border); margin: 20px 0; }
.plan-markdown :deep(strong) { color: var(--color-text); }

/* ── Alerts ──────────────────────────────────────────────────── */
.alert {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-radius: var(--radius-sm);
  font-size: 13.5px; margin-bottom: 16px;
}
.alert-error {
  background: var(--color-error-bg); color: var(--color-error);
  border: 1px solid var(--color-error-border);
}
.alert-close {
  margin-left: auto; flex-shrink: 0;
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  background: none; border: none; cursor: pointer;
  font-size: 18px; color: inherit; border-radius: var(--radius-sm);
  opacity: 0.7; transition: opacity var(--transition);
}
.alert-close:hover { opacity: 1; background: rgba(255, 255, 255, 0.06); }

/* ── Spinner ─────────────────────────────────────────────────── */
.spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite; flex-shrink: 0;
}
.spinner-sm { width: 13px; height: 13px; }

/* ── Copy modal content ──────────────────────────────────────── */
.copy-plan-section { display: flex; flex-direction: column; gap: 6px; min-height: 0; }
.copy-section-label {
  font-size: 11px; font-weight: 600; text-transform: uppercase;
  letter-spacing: 0.8px; color: var(--color-muted);
}
.copy-plan-list {
  display: flex; flex-direction: column; gap: 4px;
  max-height: 180px; overflow-y: auto; list-style: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 6px; background: var(--color-bg-3);
}
.copy-plan-item {
  display: flex; flex-direction: column; gap: 2px;
  width: 100%; padding: 9px 10px; text-align: left;
  border-radius: var(--radius-sm);
  background: none; border: 1px solid transparent;
  cursor: pointer; font-family: inherit;
  transition: background var(--transition);
}
.copy-plan-item:hover { background: var(--color-surface); }
.copy-plan-item.selected {
  background: var(--color-green-dim);
  border-color: var(--color-green);
}
.copy-plan-name { font-size: 13px; font-weight: 500; color: var(--color-text); }
.copy-plan-meta { font-size: 11px; color: var(--color-muted); }
.copy-plan-item.selected .copy-plan-meta { color: var(--color-text-dim); }

/* ── Transitions ─────────────────────────────────────────────── */
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ── Responsive ──────────────────────────────────────────────── */
@media (max-width: 1100px) {
  .workout-layout { grid-template-columns: 320px minmax(0, 1fr); }
}
@media (max-width: 900px) {
  .workout-layout { grid-template-columns: minmax(0, 1fr); }
}
@media (max-width: 768px) {
  .plan-display { padding: 20px; }
  .plan-display-header { flex-direction: column; gap: 12px; }
  .plan-header-actions { width: 100%; justify-content: flex-start; }
  .plan-display-title { font-size: 20px; }
  .checklist-day-focus { max-width: 110px; }
}
@media (max-width: 480px) {
  .form-row, .form-row.three-col { grid-template-columns: 1fr; }
  .card { padding: 18px; }
  .plan-header-actions .btn { flex: 1; }
  .plan-name-actions { width: 100%; }
  .plan-name-actions .btn { flex: 1; min-height: 40px; }
}
</style>
