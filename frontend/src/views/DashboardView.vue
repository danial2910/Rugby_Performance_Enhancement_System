<template>
  <div class="dashboard">

    <!-- ── Welcome Header ──────────────────────────────────────────────────── -->
    <div class="welcome-header">
      <div class="welcome-left">
        <div class="welcome-avatar">
          <img v-if="authStore.profilePicture" :src="authStore.profilePicture" alt="avatar" />
          <span v-else>{{ initials(authStore.fullName || authStore.username) }}</span>
        </div>
        <div>
          <p class="welcome-greeting">Good {{ timeGreeting() }},</p>
          <h1 class="welcome-name">{{ authStore.fullName || authStore.username }}</h1>
          <p class="welcome-sub" v-if="profileStore.profile?.rugbyPosition">
            🏉 {{ profileStore.profile.rugbyPosition }}
            <span v-if="profileStore.profile?.teamName"> · {{ profileStore.profile.teamName }}</span>
          </p>
        </div>
      </div>
      <div class="welcome-stats">
        <div class="stat-pill">
          <span class="stat-num">{{ workoutStore.plans.length }}</span>
          <span class="stat-lbl">Workout Plans</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num">{{ mealStore.plans.length }}</span>
          <span class="stat-lbl">Meal Plans</span>
        </div>
        <div class="stat-pill" :class="{ 'stat-pill-alert': trainerUpdateCount > 0 }">
          <span class="stat-num">{{ trainerUpdateCount }}</span>
          <span class="stat-lbl">Trainer Updates</span>
        </div>
        <div class="stat-pill" :class="{ 'stat-pill-appt': upcomingAppointments.length > 0 }">
          <span class="stat-num">{{ upcomingAppointments.length }}</span>
          <span class="stat-lbl">Upcoming Sessions</span>
        </div>
      </div>
    </div>

    <!-- ── Trainer Updates Banner ──────────────────────────────────────────── -->
    <transition name="slide-down">
      <div v-if="trainerUpdateCount > 0" class="trainer-banner">
        <div class="trainer-banner-icon">✏️</div>
        <div class="trainer-banner-text">
          <strong>Your trainer has updated {{ trainerUpdateCount }} of your plan{{ trainerUpdateCount > 1 ? 's' : '' }}.</strong>
          <span> Scroll down to review the changes.</span>
        </div>
        <a href="#trainer-updates" class="trainer-banner-link">View Changes ↓</a>
      </div>
    </transition>

    <!-- ── Trainer Feedback Notification Banner ──────────────────────────────── -->
    <transition name="slide-down">
      <div v-if="feedbackNotifications.length > 0" class="feedback-banner">
        <div class="feedback-banner-icon">💬</div>
        <div class="feedback-banner-text">
          <strong>You have {{ feedbackNotifications.length }} new trainer feedback{{ feedbackNotifications.length > 1 ? 's' : '' }} on your appointment{{ feedbackNotifications.length > 1 ? 's' : '' }}.</strong>
          <span> Scroll down to see what your trainer said.</span>
        </div>
        <a href="#feedback-section" class="feedback-banner-link">View Feedback ↓</a>
      </div>
    </transition>

    <!-- ── Loading state ──────────────────────────────────────────────────── -->
    <div v-if="loading" class="loading-grid">
      <div class="skeleton-card" v-for="n in 2" :key="n"></div>
    </div>

    <template v-else>
      <!-- ── Active Plans Row ─────────────────────────────────────────────── -->
      <div class="plans-row">

        <!-- Active Workout Plan Card -->
        <div class="plan-card" :class="{ 'no-plan': !activeWorkout }">
          <div class="plan-card-header">
            <span class="plan-card-icon">💪</span>
            <h2 class="plan-card-title">Active Workout Plan</h2>
            <router-link to="/workout" class="plan-card-link">View All →</router-link>
          </div>

          <div v-if="!activeWorkout" class="no-plan-body">
            <p class="no-plan-msg">No active workout plan yet.</p>
            <router-link to="/workout" class="btn-generate">Generate a Plan</router-link>
          </div>

          <template v-else>
            <!-- Plan name + meta -->
            <div class="plan-name">{{ activeWorkout.planName }}</div>
            <div class="plan-chips">
              <span v-if="activeWorkout.rugbyPosition" class="chip">🏉 {{ activeWorkout.rugbyPosition }}</span>
              <span v-if="activeWorkout.goal" class="chip">🎯 {{ goalLabel(activeWorkout.goal) }}</span>
              <span v-if="activeWorkout.trainingPhase" class="chip">📅 {{ phaseLabel(activeWorkout.trainingPhase) }}</span>
            </div>

            <!-- Circular progress -->
            <div class="progress-section">
              <div class="circular-progress">
                <svg viewBox="0 0 80 80" class="ring-svg">
                  <circle class="ring-bg" cx="40" cy="40" r="34" />
                  <circle
                    class="ring-fill ring-workout"
                    cx="40" cy="40" r="34"
                    :stroke-dasharray="`${workoutProgress * 2.136} ${213.6 - workoutProgress * 2.136}`"
                  />
                </svg>
                <div class="ring-label">
                  <span class="ring-pct">{{ workoutProgress }}%</span>
                  <span class="ring-sub">done</span>
                </div>
              </div>
              <div class="progress-detail">
                <div class="progress-stat">
                  <span class="progress-num">{{ workoutCompletedCount }}</span>
                  <span class="progress-lbl">Exercises Done</span>
                </div>
                <div class="progress-divider"></div>
                <div class="progress-stat">
                  <span class="progress-num">{{ workoutTotalCount }}</span>
                  <span class="progress-lbl">Total Exercises</span>
                </div>
                <div class="progress-divider"></div>
                <div class="progress-stat">
                  <span class="progress-num">{{ workoutTotalCount - workoutCompletedCount }}</span>
                  <span class="progress-lbl">Remaining</span>
                </div>
              </div>
            </div>

            <!-- Progress bar -->
            <div class="progress-bar-wrap">
              <div class="progress-bar">
                <div class="progress-bar-fill bar-workout" :style="{ width: workoutProgress + '%' }"></div>
              </div>
              <span class="progress-bar-label">{{ workoutCompletedCount }}/{{ workoutTotalCount }} exercises</span>
            </div>

            <!-- Trainer note for this workout plan -->
            <div v-if="activeWorkout.trainerNote" class="trainer-note-card">
              <div class="tnc-header">
                <span class="tnc-icon">✏️</span>
                <span class="tnc-title">Trainer Note</span>
                <span v-if="activeWorkout.lastEditedBy" class="tnc-by">by {{ activeWorkout.lastEditedBy }}</span>
              </div>
              <p class="tnc-text">{{ activeWorkout.trainerNote }}</p>
            </div>
          </template>
        </div>

        <!-- Active Meal Plan Card -->
        <div class="plan-card" :class="{ 'no-plan': !activeMeal }">
          <div class="plan-card-header">
            <span class="plan-card-icon">🥗</span>
            <h2 class="plan-card-title">Active Meal Plan</h2>
            <router-link to="/meal-planner" class="plan-card-link">View All →</router-link>
          </div>

          <div v-if="!activeMeal" class="no-plan-body">
            <p class="no-plan-msg">No active meal plan yet.</p>
            <router-link to="/meal-planner" class="btn-generate">Generate a Plan</router-link>
          </div>

          <template v-else>
            <!-- Plan name + meta -->
            <div class="plan-name">{{ activeMeal.planName }}</div>
            <div class="plan-chips">
              <span v-if="activeMeal.rugbyPosition" class="chip">🏉 {{ activeMeal.rugbyPosition }}</span>
              <span v-if="activeMeal.goal" class="chip">🎯 {{ mealGoalLabel(activeMeal.goal) }}</span>
              <span v-if="activeMeal.trainingPhase" class="chip">📅 {{ phaseLabel(activeMeal.trainingPhase) }}</span>
            </div>

            <!-- Calorie target row -->
            <div class="calorie-row">
              <div class="calorie-card calorie-target">
                <span class="cal-num">{{ estimatedCalories(activeMeal) }}</span>
                <span class="cal-lbl">kcal / day target</span>
              </div>
              <div class="calorie-card calorie-meals">
                <span class="cal-num">{{ activeMeal.mealsPerDay || '–' }}</span>
                <span class="cal-lbl">meals per day</span>
              </div>
              <div class="calorie-card calorie-weight">
                <span class="cal-num">{{ activeMeal.weight || '–' }}<small>kg</small></span>
                <span class="cal-lbl">current weight</span>
              </div>
            </div>

            <!-- Circular progress -->
            <div class="progress-section">
              <div class="circular-progress">
                <svg viewBox="0 0 80 80" class="ring-svg">
                  <circle class="ring-bg" cx="40" cy="40" r="34" />
                  <circle
                    class="ring-fill ring-meal"
                    cx="40" cy="40" r="34"
                    :stroke-dasharray="`${mealProgress * 2.136} ${213.6 - mealProgress * 2.136}`"
                  />
                </svg>
                <div class="ring-label">
                  <span class="ring-pct">{{ mealProgress }}%</span>
                  <span class="ring-sub">done</span>
                </div>
              </div>
              <div class="progress-detail">
                <div class="progress-stat">
                  <span class="progress-num">{{ mealCompletedCount }}</span>
                  <span class="progress-lbl">Meals Taken</span>
                </div>
                <div class="progress-divider"></div>
                <div class="progress-stat">
                  <span class="progress-num">{{ mealTotalCount }}</span>
                  <span class="progress-lbl">Total Meals</span>
                </div>
                <div class="progress-divider"></div>
                <div class="progress-stat">
                  <span class="progress-num">{{ mealTotalCount - mealCompletedCount }}</span>
                  <span class="progress-lbl">Remaining</span>
                </div>
              </div>
            </div>

            <!-- Progress bar -->
            <div class="progress-bar-wrap">
              <div class="progress-bar">
                <div class="progress-bar-fill bar-meal" :style="{ width: mealProgress + '%' }"></div>
              </div>
              <span class="progress-bar-label">{{ mealCompletedCount }}/{{ mealTotalCount }} meals</span>
            </div>

            <!-- Trainer note for this meal plan -->
            <div v-if="activeMeal.trainerNote" class="trainer-note-card">
              <div class="tnc-header">
                <span class="tnc-icon">✏️</span>
                <span class="tnc-title">Trainer Note</span>
                <span v-if="activeMeal.lastEditedBy" class="tnc-by">by {{ activeMeal.lastEditedBy }}</span>
              </div>
              <p class="tnc-text">{{ activeMeal.trainerNote }}</p>
            </div>
          </template>
        </div>

      </div>

      <!-- ── Trainer Changes + Upcoming Appointments (side by side) ──────── -->
      <div
        v-if="trainerUpdateCount > 0 || upcomingAppointments.length > 0"
        class="updates-appt-row"
      >

        <!-- Trainer Updates column -->
        <div id="trainer-updates" v-if="trainerUpdateCount > 0" class="trainer-updates-section">
          <div class="section-header">
            <h2 class="section-title">
              <span class="section-icon">✏️</span> Trainer Changes
            </h2>
            <p class="section-sub">Your trainer has made changes to the following plans.</p>
          </div>

          <div class="updates-grid">
            <div
              v-for="item in allTrainerUpdates"
              :key="item.plan.id + item.type"
              class="update-card"
            >
              <div class="update-card-top">
                <div class="update-type-badge" :class="item.type === 'workout' ? 'badge-workout' : 'badge-meal'">
                  {{ item.type === 'workout' ? '💪 Workout' : '🥗 Meal' }}
                </div>
                <span v-if="item.plan.isActive" class="active-chip">✅ Active</span>
                <span class="update-date">{{ formatDate(item.plan.updatedAt || item.plan.createdAt) }}</span>
              </div>
              <div class="update-plan-name">{{ item.plan.planName }}</div>
              <div class="update-meta">
                <span v-if="item.plan.rugbyPosition">🏉 {{ item.plan.rugbyPosition }}</span>
                <span v-if="item.plan.goal">· {{ item.type === 'workout' ? goalLabel(item.plan.goal) : mealGoalLabel(item.plan.goal) }}</span>
              </div>

              <div class="update-note-box">
                <div class="unb-header">
                  <span class="unb-icon">📝</span>
                  <span class="unb-label">Trainer Note</span>
                  <span v-if="item.plan.lastEditedBy" class="unb-by">— {{ item.plan.lastEditedBy }}</span>
                </div>
                <p class="unb-text">{{ item.plan.trainerNote }}</p>
              </div>

              <router-link
                :to="item.type === 'workout' ? '/workout' : '/meal-planner'"
                class="update-view-btn"
              >
                View Full Plan →
              </router-link>
            </div>
          </div>
        </div>

        <!-- Upcoming Appointments column -->
        <div v-if="upcomingAppointments.length > 0" class="appt-reminder-section">
        <div class="section-header">
          <h2 class="section-title">
            <span class="section-icon">📅</span> Upcoming Appointments
          </h2>
          <p class="section-sub">Your confirmed sessions — don't miss them!</p>
        </div>

        <div class="appt-reminder-grid">
          <div
            v-for="appt in upcomingAppointments"
            :key="appt.id"
            class="appt-reminder-card"
            :class="{
              'appt-today':   isToday(appt.date),
              'appt-soon':    isSoon(appt.date) && !isToday(appt.date),
              'appt-future':  !isSoon(appt.date) && !isToday(appt.date)
            }"
          >
            <!-- Header row -->
            <div class="arc-header">
              <span class="arc-badge" :class="{
                'arc-badge-today':  isToday(appt.date),
                'arc-badge-soon':   isSoon(appt.date) && !isToday(appt.date),
                'arc-badge-future': !isSoon(appt.date) && !isToday(appt.date)
              }">
                {{ isToday(appt.date) ? '🔔 Today!' : isSoon(appt.date) ? '⚡ Soon' : '✅ Confirmed' }}
              </span>
              <span class="arc-service">{{ serviceLabel(appt.serviceType) }}</span>
            </div>

            <!-- Trainer + meta -->
            <div class="arc-trainer-row">
              <div class="arc-avatar">{{ initials(appt.trainerName) }}</div>
              <div class="arc-trainer-info">
                <div class="arc-trainer-name">{{ appt.trainerName }}</div>
                <div class="arc-appt-meta">
                  <span>📅 {{ formatApptDate(appt.date) }}</span>
                  <span>🕐 {{ appt.time }}</span>
                  <span>⏱ {{ appt.duration }} min</span>
                  <span>{{ appt.location === 'FACE_TO_FACE' ? '🤝 Face to Face' : '💻 Online' }}</span>
                </div>
              </div>
            </div>

            <!-- Countdown -->
            <div class="arc-countdown" :class="{ 'countdown-today': isToday(appt.date), 'countdown-soon': isSoon(appt.date) && !isToday(appt.date) }">
              <template v-if="isPast(appt.date, appt.time)">
                <span class="countdown-label">Appointment time passed</span>
              </template>
              <template v-else-if="isToday(appt.date)">
                <div class="countdown-units">
                  <div class="cd-unit">
                    <span class="cd-num">{{ countdown(appt.date, appt.time).hours }}</span>
                    <span class="cd-lbl">hrs</span>
                  </div>
                  <span class="cd-sep">:</span>
                  <div class="cd-unit">
                    <span class="cd-num">{{ countdown(appt.date, appt.time).minutes }}</span>
                    <span class="cd-lbl">min</span>
                  </div>
                  <span class="cd-sep">:</span>
                  <div class="cd-unit">
                    <span class="cd-num">{{ countdown(appt.date, appt.time).seconds }}</span>
                    <span class="cd-lbl">sec</span>
                  </div>
                </div>
                <span class="countdown-sublabel">until your session</span>
              </template>
              <template v-else>
                <div class="countdown-units">
                  <div class="cd-unit">
                    <span class="cd-num">{{ countdown(appt.date, appt.time).days }}</span>
                    <span class="cd-lbl">days</span>
                  </div>
                  <span class="cd-sep">:</span>
                  <div class="cd-unit">
                    <span class="cd-num">{{ countdown(appt.date, appt.time).hours }}</span>
                    <span class="cd-lbl">hrs</span>
                  </div>
                  <span class="cd-sep">:</span>
                  <div class="cd-unit">
                    <span class="cd-num">{{ countdown(appt.date, appt.time).minutes }}</span>
                    <span class="cd-lbl">min</span>
                  </div>
                  <span class="cd-sep">:</span>
                  <div class="cd-unit">
                    <span class="cd-num">{{ countdown(appt.date, appt.time).seconds }}</span>
                    <span class="cd-lbl">sec</span>
                  </div>
                </div>
                <span class="countdown-sublabel">until your session</span>
              </template>
            </div>

            <!-- Trainer remarks if any -->
            <div v-if="appt.trainerRemarks" class="arc-remarks">
              <span class="arc-remarks-icon">💬</span>
              <span>{{ appt.trainerRemarks }}</span>
            </div>
          </div>
        </div>
      </div><!-- end appt-reminder-section -->

      </div><!-- end updates-appt-row -->

      <!-- ── Trainer Feedback Cards ─────────────────────────────────────────── -->
      <div id="feedback-section" v-if="feedbackNotifications.length > 0" class="feedback-section">
        <div class="section-header">
          <h2 class="section-title">
            <span class="section-icon">💬</span> Trainer Feedback
          </h2>
          <p class="section-sub">Your trainer has left feedback on your completed sessions.</p>
        </div>
        <div class="feedback-grid">
          <div
            v-for="appt in feedbackNotifications"
            :key="appt.id"
            class="feedback-card">
            <div class="feedback-card-top">
              <div class="feedback-service-badge">{{ serviceLabel(appt.serviceType) }}</div>
              <span class="feedback-date">{{ formatApptDate(appt.date) }}</span>
            </div>
            <div class="feedback-trainer-row">
              <div class="feedback-avatar">{{ initials(appt.trainerName) }}</div>
              <div class="feedback-trainer-info">
                <div class="feedback-trainer-name">{{ appt.trainerName }}</div>
                <div class="feedback-meta">🕐 {{ appt.time }} · ⏱ {{ appt.duration }} min · {{ appt.location === 'FACE_TO_FACE' ? '🤝 Face to Face' : '💻 Online' }}</div>
              </div>
            </div>
            <div class="feedback-message-box">
              <div class="feedback-message-header">
                <span class="feedback-message-icon">📝</span>
                <span class="feedback-message-label">Trainer Feedback</span>
              </div>
              <p class="feedback-message-text">{{ appt.trainerFeedback }}</p>
            </div>
            <router-link to="/appointments" class="feedback-view-btn">
              View in Appointments →
            </router-link>
          </div>
        </div>
      </div>

      <!-- ── Quick Links (when no active plans or no trainer updates) ──────── -->
      <div v-if="!activeWorkout || !activeMeal" class="quick-links">
        <h2 class="section-title">Get Started</h2>
        <div class="quick-grid">
          <router-link v-if="!activeWorkout" to="/workout" class="quick-card">
            <span class="quick-icon">💪</span>
            <div>
              <div class="quick-title">Generate Workout Plan</div>
              <div class="quick-sub">Get a personalised AI training plan for your position</div>
            </div>
            <span class="quick-arrow">→</span>
          </router-link>
          <router-link v-if="!activeMeal" to="/meal-planner" class="quick-card">
            <span class="quick-icon">🥗</span>
            <div>
              <div class="quick-title">Generate Meal Plan</div>
              <div class="quick-sub">7-day nutrition plan tailored to your goal and phase</div>
            </div>
            <span class="quick-arrow">→</span>
          </router-link>
          <router-link to="/chatbot" class="quick-card">
            <span class="quick-icon">🤖</span>
            <div>
              <div class="quick-title">Ask the AI Chatbot</div>
              <div class="quick-sub">Get answers to your rugby training and nutrition questions</div>
            </div>
            <span class="quick-arrow">→</span>
          </router-link>
        </div>
      </div>

    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore }        from '@/stores/auth'
import { useProfileStore }     from '@/stores/profile'
import { useWorkoutStore }     from '@/stores/workout'
import { useMealStore }        from '@/stores/meal'
import { useAppointmentStore } from '@/stores/appointment'

const authStore    = useAuthStore()
const profileStore = useProfileStore()
const workoutStore = useWorkoutStore()
const mealStore    = useMealStore()
const apptStore    = useAppointmentStore()

const loading = ref(true)

// ── Live clock for countdown ──────────────────────────────────────────────────
const now = ref(new Date())
let clockTimer = null

// ── Lifecycle ─────────────────────────────────────────────────────────────────

onMounted(async () => {
  loading.value = true
  await Promise.all([
    profileStore.fetchProfile(),
    workoutStore.fetchPlans(),
    mealStore.fetchPlans(),
    apptStore.fetchAthleteAppointments()
  ])
  loading.value = false

  // Tick every second for the countdown timer
  clockTimer = setInterval(() => { now.value = new Date() }, 1000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})

// ── Active plans ──────────────────────────────────────────────────────────────

const activeWorkout = computed(() =>
  workoutStore.plans.find(p => p.isActive) || null
)

const activeMeal = computed(() =>
  mealStore.plans.find(p => p.isActive) || null
)

// ── Trainer updates (all plans with a trainerNote) ────────────────────────────

const allTrainerUpdates = computed(() => {
  const updates = []
  workoutStore.plans.forEach(p => { if (p.trainerNote) updates.push({ plan: p, type: 'workout' }) })
  mealStore.plans.forEach(p => { if (p.trainerNote) updates.push({ plan: p, type: 'meal' }) })
  // Sort: active plans first, then by updatedAt desc
  return updates.sort((a, b) => {
    if (a.plan.isActive !== b.plan.isActive) return a.plan.isActive ? -1 : 1
    return new Date(b.plan.updatedAt || 0) - new Date(a.plan.updatedAt || 0)
  })
})

const trainerUpdateCount = computed(() => allTrainerUpdates.value.length)

// ── Upcoming approved appointments ────────────────────────────────────────────

const upcomingAppointments = computed(() => {
  const todayStr = new Date().toISOString().split('T')[0]
  return apptStore.appointments
    .filter(a => a.status === 'APPROVED' && a.date >= todayStr)
    .sort((a, b) => {
      const da = new Date(`${a.date}T${a.time}:00`)
      const db = new Date(`${b.date}T${b.time}:00`)
      return da - db
    })
})

// ── Trainer feedback notifications ───────────────────────────────────────────
const feedbackNotifications = computed(() =>
  apptStore.appointments.filter(a => a.status === 'APPROVED' && a.trainerFeedback)
)

// ── Countdown helpers ──────────────────────────────────────────────────────────

function apptDateTime(date, time) {
  return new Date(`${date}T${time}:00`)
}

function isToday(date) {
  const todayStr = now.value.toISOString().split('T')[0]
  return date === todayStr
}

function isSoon(date) {
  // Within the next 3 days
  const dt     = new Date(date + 'T00:00:00')
  const todayMs = new Date(now.value.toISOString().split('T')[0] + 'T00:00:00').getTime()
  const diffDays = (dt.getTime() - todayMs) / (1000 * 60 * 60 * 24)
  return diffDays <= 3
}

function isPast(date, time) {
  return apptDateTime(date, time) <= now.value
}

function countdown(date, time) {
  const target  = apptDateTime(date, time)
  const diffMs  = target - now.value
  if (diffMs <= 0) return { days: 0, hours: '00', minutes: '00', seconds: '00' }

  const totalSecs = Math.floor(diffMs / 1000)
  const days      = Math.floor(totalSecs / (60 * 60 * 24))
  const hours     = String(Math.floor((totalSecs % (60 * 60 * 24)) / 3600)).padStart(2, '0')
  const minutes   = String(Math.floor((totalSecs % 3600) / 60)).padStart(2, '0')
  const seconds   = String(totalSecs % 60).padStart(2, '0')
  return { days, hours, minutes, seconds }
}

function serviceLabel(s) {
  const m = {
    FITNESS_TRAINING:     '💪 Fitness Training',
    NUTRITION_COUNSELLING:'🥗 Nutrition Counselling',
    WELLNESS_COACHING:    '🧘 Wellness Coaching'
  }
  return m[s] || s
}

function formatApptDate(d) {
  if (!d) return ''
  return new Date(d + 'T00:00:00').toLocaleDateString('en-MY', {
    weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
  })
}

// ── Workout progress ──────────────────────────────────────────────────────────

const workoutTotalCount = computed(() =>
  activeWorkout.value ? extractExerciseKeys(activeWorkout.value.generatedPlan).length : 0
)
const workoutCompletedCount = computed(() =>
  activeWorkout.value ? (activeWorkout.value.completedItems?.length || 0) : 0
)
const workoutProgress = computed(() => {
  if (!workoutTotalCount.value) return 0
  return Math.round((workoutCompletedCount.value / workoutTotalCount.value) * 100)
})

// ── Meal progress ─────────────────────────────────────────────────────────────

const mealTotalCount = computed(() =>
  activeMeal.value ? extractMealKeys(activeMeal.value.generatedPlan).length : 0
)
const mealCompletedCount = computed(() =>
  activeMeal.value ? (activeMeal.value.completedItems?.length || 0) : 0
)
const mealProgress = computed(() => {
  if (!mealTotalCount.value) return 0
  return Math.round((mealCompletedCount.value / mealTotalCount.value) * 100)
})

// ── Calorie estimation (Mifflin-St Jeor, mirrors backend logic) ───────────────

function estimatedCalories(plan) {
  if (!plan || !plan.weight || !plan.height || !plan.age) return '–'
  const bmr = (10 * plan.weight) + (6.25 * plan.height) - (5 * plan.age) + 5
  const mult = plan.activityLevel === 'MODERATE' ? 1.55
             : plan.activityLevel === 'EXTREME'  ? 1.9
             : 1.725
  let tdee = bmr * mult
  if (plan.goal === 'MUSCLE_GAIN')  tdee += 400
  else if (plan.goal === 'WEIGHT_LOSS') tdee -= 400
  else if (plan.goal === 'PERFORMANCE') tdee += 200
  return Math.round(tdee).toLocaleString()
}

// ── Exercise key parser — exact copy from WorkoutView.vue ────────────────────

function extractExerciseKeys(text) {
  if (!text) return []

  const lines = text.split('\n')
  const keys  = []
  let inSummary  = false
  let headerDone = false
  let dayCol     = -1
  let exCol      = -1

  for (const line of lines) {
    const trimmed = line.trim()

    // Detect the Weekly Structure Summary heading (## or ###)
    if (/weekly.*(structure.*summary|summary)/i.test(trimmed)) {
      inSummary  = true
      headerDone = false
      dayCol     = -1
      exCol      = -1
      continue
    }

    if (!inSummary) continue

    // A new ## heading after we've started means the summary block ended
    if (trimmed.startsWith('##') && headerDone) break

    // Only process pipe-table rows
    if (!trimmed.startsWith('|')) continue

    const cells = trimmed.split('|').map(c => c.trim()).slice(1, -1)
    if (!cells.length) continue

    // Skip separator rows (--- or :---:)
    if (cells.every(c => /^[-: ]+$/.test(c))) continue

    if (!headerDone) {
      dayCol     = cells.findIndex(c => /^day$/i.test(c))
      exCol      = cells.findIndex(c => /key\s*exercise|exercise/i.test(c))
      headerDone = true
      continue
    }

    if (dayCol < 0 || exCol < 0) continue
    if (!cells[dayCol] || !cells[exCol]) continue

    // The AI sometimes writes the Day column as just "1" and sometimes as
    // "Day 1" — strip any leading "Day" word so we don't double it up below.
    const dayNum      = cells[dayCol].trim().replace(/^day\s*/i, '')
    const exerciseStr = cells[exCol].trim()
    if (!dayNum || !exerciseStr) continue

    exerciseStr.split(',').map(e => e.trim()).filter(Boolean).forEach(ex => {
      const clean = ex.replace(/\*\*/g, '').trim()
      // Skip rest-day placeholders ("-", "–", "—", etc.)
      if (clean && !/^[-–—\s]+$/.test(clean)) keys.push(`Day ${dayNum} – ${clean}`)
    })
  }

  return keys
}

// ── Meal key parser — exact copy from MealPlannerView.vue ─────────────────────

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
      if (!mealName.toLowerCase().includes('summary') && !mealName.toLowerCase().includes('table')) {
        const key = `${currentDay} – ${mealName}`
        if (!keys.includes(key)) keys.push(key)
      }
    }
  }
  return keys
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function timeGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'morning'
  if (h < 17) return 'afternoon'
  return 'evening'
}

function initials(name) {
  if (!name) return '?'
  return name.split(' ').filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join('')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('en-MY', {
    day: 'numeric', month: 'short', year: 'numeric'
  })
}

function goalLabel(goal) {
  const m = { STRENGTH: '💪 Strength', POWER: '⚡ Power', ENDURANCE: '🏃 Endurance', LEAN: '🔥 Lean' }
  return m[goal] || goal
}

function mealGoalLabel(goal) {
  const m = { MUSCLE_GAIN: '💪 Muscle Gain', WEIGHT_LOSS: '⚖️ Weight Loss', PERFORMANCE: '🏆 Performance', MAINTENANCE: '🔄 Maintenance' }
  return m[goal] || goal
}

function phaseLabel(phase) {
  const m = { PRE_SEASON: '🔥 Pre-Season', IN_SEASON: '🏉 In-Season', OFF_SEASON: '💪 Off-Season', POST_SEASON: '🛌 Post-Season' }
  return m[phase] || phase || ''
}
</script>

<style scoped>
/* ── Layout ────────────────────────────────────────────────────────────────── */
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ── Welcome Header ────────────────────────────────────────────────────────── */
.welcome-header {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.welcome-left  { display: flex; align-items: center; gap: 16px; }

.welcome-avatar {
  width: 60px; height: 60px;
  border-radius: 50%;
  background: var(--color-bg-3);
  border: 2px solid var(--color-green-light);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700;
  color: var(--color-green-light);
  overflow: hidden; flex-shrink: 0;
}
.welcome-avatar img { width: 100%; height: 100%; object-fit: cover; }

.welcome-greeting { margin: 0; font-size: 13px; color: var(--color-muted); }
.welcome-name {
  margin: 2px 0 4px;
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 28px; font-weight: 700;
  color: var(--color-text);
}
.welcome-sub { margin: 0; font-size: 13px; color: var(--color-muted); }

.welcome-stats { display: flex; gap: 12px; flex-wrap: wrap; }
.stat-pill {
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 18px;
  display: flex; flex-direction: column; align-items: center;
  min-width: 80px;
}
.stat-pill-alert {
  background: rgba(245,158,11,0.08);
  border-color: rgba(245,158,11,0.4);
}
.stat-num { font-size: 22px; font-weight: 700; color: var(--color-green-light); line-height: 1; }
.stat-pill-alert .stat-num { color: #f59e0b; }
.stat-lbl { font-size: 11px; color: var(--color-muted); margin-top: 3px; white-space: nowrap; }

/* ── Trainer Banner ────────────────────────────────────────────────────────── */
.trainer-banner {
  background: rgba(245,158,11,0.1);
  border: 1px solid rgba(245,158,11,0.35);
  border-radius: var(--radius-md);
  padding: 14px 20px;
  display: flex; align-items: center; gap: 12px;
  flex-wrap: wrap;
}
.trainer-banner-icon { font-size: 20px; flex-shrink: 0; }
.trainer-banner-text { flex: 1; font-size: 14px; color: var(--color-text); }
.trainer-banner-text strong { color: #f59e0b; }
.trainer-banner-link {
  font-size: 13px; font-weight: 600;
  color: #f59e0b;
  text-decoration: none; white-space: nowrap;
}
.trainer-banner-link:hover { text-decoration: underline; }

/* ── Loading skeletons ─────────────────────────────────────────────────────── */
.loading-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.skeleton-card {
  height: 320px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

/* ── Plans Row ─────────────────────────────────────────────────────────────── */
.plans-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
@media (max-width: 900px) { .plans-row { grid-template-columns: 1fr; } }

/* ── Plan Cards ────────────────────────────────────────────────────────────── */
.plan-card {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  display: flex; flex-direction: column; gap: 14px;
}
.plan-card.no-plan {
  border-style: dashed;
  opacity: 0.7;
}

.plan-card-header {
  display: flex; align-items: center; gap: 8px;
}
.plan-card-icon { font-size: 18px; }
.plan-card-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 17px; font-weight: 700;
  color: var(--color-text); margin: 0; flex: 1;
}
.plan-card-link {
  font-size: 12px; font-weight: 600;
  color: var(--color-green-light);
  text-decoration: none; white-space: nowrap;
}
.plan-card-link:hover { text-decoration: underline; }

.no-plan-body { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 20px 0; }
.no-plan-msg  { color: var(--color-muted); font-size: 14px; margin: 0; }
.btn-generate {
  padding: 8px 18px;
  background: var(--color-green-light);
  color: #000; font-weight: 700; font-size: 13px;
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: opacity 0.15s;
}
.btn-generate:hover { opacity: 0.85; }

.plan-name {
  font-size: 15px; font-weight: 700;
  color: var(--color-text);
}

.plan-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip {
  font-size: 11px;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  padding: 3px 9px; border-radius: 20px;
  color: var(--color-muted);
}

/* ── Circular Progress ─────────────────────────────────────────────────────── */
.progress-section {
  display: flex; align-items: center; gap: 20px;
}

.circular-progress {
  position: relative; width: 80px; height: 80px; flex-shrink: 0;
}
.ring-svg { width: 80px; height: 80px; transform: rotate(-90deg); }
.ring-bg {
  fill: none; stroke: var(--color-border); stroke-width: 7;
}
.ring-fill {
  fill: none; stroke-width: 7;
  stroke-linecap: round;
  transition: stroke-dasharray 0.6s ease;
  stroke-dasharray: 0 213.6;
}
.ring-workout { stroke: var(--color-green-light); }
.ring-meal    { stroke: #3b82f6; }

.ring-label {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.ring-pct  { font-size: 16px; font-weight: 700; color: var(--color-text); line-height: 1; }
.ring-sub  { font-size: 9px; color: var(--color-muted); }

.progress-detail { display: flex; align-items: center; gap: 12px; flex: 1; flex-wrap: wrap; }
.progress-stat { display: flex; flex-direction: column; align-items: center; }
.progress-num { font-size: 20px; font-weight: 700; color: var(--color-text); line-height: 1; }
.progress-lbl { font-size: 10px; color: var(--color-muted); margin-top: 2px; white-space: nowrap; }
.progress-divider { width: 1px; height: 30px; background: var(--color-border); }

/* ── Progress Bar ──────────────────────────────────────────────────────────── */
.progress-bar-wrap {
  display: flex; align-items: center; gap: 10px;
}
.progress-bar {
  flex: 1; height: 7px;
  background: var(--color-bg-3);
  border-radius: 4px; overflow: hidden;
}
.progress-bar-fill {
  height: 100%; border-radius: 4px;
  transition: width 0.6s ease;
}
.bar-workout { background: var(--color-green-light); }
.bar-meal    { background: #3b82f6; }
.progress-bar-label { font-size: 11px; color: var(--color-muted); white-space: nowrap; }

/* ── Calorie Row ───────────────────────────────────────────────────────────── */
.calorie-row { display: flex; gap: 10px; }
.calorie-card {
  flex: 1; background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  display: flex; flex-direction: column; align-items: center; gap: 3px;
}
.cal-num { font-size: 18px; font-weight: 700; color: var(--color-text); line-height: 1; }
.cal-num small { font-size: 11px; font-weight: 400; }
.calorie-target .cal-num { color: #f59e0b; }
.cal-lbl { font-size: 10px; color: var(--color-muted); text-align: center; }

/* ── Trainer Note Card (inside plan card) ──────────────────────────────────── */
.trainer-note-card {
  background: rgba(245,158,11,0.08);
  border: 1px solid rgba(245,158,11,0.3);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}
.tnc-header {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 700; color: #f59e0b;
  margin-bottom: 6px;
}
.tnc-title { flex: 1; }
.tnc-by { font-weight: 400; color: var(--color-muted); font-size: 11px; }
.tnc-text { margin: 0; font-size: 13px; color: var(--color-text); line-height: 1.55; }

/* ── Trainer Updates Section ───────────────────────────────────────────────── */
.trainer-updates-section { display: flex; flex-direction: column; gap: 16px; }

.section-header { margin-bottom: 4px; }
.section-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 22px; font-weight: 700;
  color: var(--color-text); margin: 0 0 4px;
  display: flex; align-items: center; gap: 8px;
}
.section-icon { font-size: 18px; }
.section-sub { margin: 0; font-size: 13px; color: var(--color-muted); }

.updates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.update-card {
  background: var(--color-bg-2);
  border: 1px solid rgba(245,158,11,0.3);
  border-left: 4px solid #f59e0b;
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex; flex-direction: column; gap: 10px;
}

.update-card-top {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}
.update-type-badge {
  font-size: 11px; font-weight: 700;
  padding: 3px 8px; border-radius: 20px;
}
.badge-workout { background: rgba(180,255,0,0.12); color: var(--color-green-light); }
.badge-meal    { background: rgba(59,130,246,0.12); color: #3b82f6; }
.active-chip {
  font-size: 10px; font-weight: 700;
  background: rgba(180,255,0,0.12); color: var(--color-green-light);
  padding: 2px 7px; border-radius: 4px;
}
.update-date { font-size: 11px; color: var(--color-muted); margin-left: auto; }

.update-plan-name { font-size: 15px; font-weight: 700; color: var(--color-text); }
.update-meta { font-size: 12px; color: var(--color-muted); }

.update-note-box {
  background: rgba(245,158,11,0.07);
  border: 1px solid rgba(245,158,11,0.2);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}
.unb-header {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 700; color: #f59e0b;
  margin-bottom: 6px;
}
.unb-label { flex: 1; }
.unb-by { font-weight: 400; color: var(--color-muted); }
.unb-text { margin: 0; font-size: 13px; color: var(--color-text); line-height: 1.55; }

.update-view-btn {
  display: inline-block; align-self: flex-start;
  font-size: 12px; font-weight: 600;
  color: #f59e0b; text-decoration: none;
  transition: opacity 0.15s;
}
.update-view-btn:hover { opacity: 0.75; text-decoration: underline; }

/* ── Quick Links ───────────────────────────────────────────────────────────── */
.quick-links { display: flex; flex-direction: column; gap: 14px; }
.quick-grid  { display: flex; flex-direction: column; gap: 10px; }

.quick-card {
  display: flex; align-items: center; gap: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  text-decoration: none;
  transition: border-color 0.15s, background 0.15s;
}
.quick-card:hover {
  border-color: var(--color-green-light);
  background: rgba(180,255,0,0.04);
}
.quick-icon { font-size: 24px; flex-shrink: 0; }
.quick-title { font-size: 14px; font-weight: 700; color: var(--color-text); margin-bottom: 2px; }
.quick-sub   { font-size: 12px; color: var(--color-muted); }
.quick-arrow { font-size: 18px; color: var(--color-muted); margin-left: auto; }

/* ── Side-by-side row: Trainer Changes + Upcoming Appointments ─────────────── */
.updates-appt-row {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.updates-appt-row > * {
  flex: 1;
  min-width: 0;
}
@media (max-width: 900px) {
  .updates-appt-row { flex-direction: column; }
}

/* ── Appointment Reminder Section ──────────────────────────────────────────── */
.appt-reminder-section { display: flex; flex-direction: column; gap: 16px; }

.appt-reminder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.appt-reminder-card {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex; flex-direction: column; gap: 12px;
  transition: border-color 0.2s;
}

.appt-today  { border-left: 4px solid #f59e0b; background: rgba(245,158,11,0.04); }
.appt-soon   { border-left: 4px solid #3b82f6; background: rgba(59,130,246,0.04); }
.appt-future { border-left: 4px solid var(--color-green-light); background: rgba(180,255,0,0.03); }

/* Badge row */
.arc-header { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.arc-badge {
  font-size: 11px; font-weight: 700;
  padding: 3px 9px; border-radius: 20px;
}
.arc-badge-today  { background: rgba(245,158,11,0.15); color: #f59e0b; }
.arc-badge-soon   { background: rgba(59,130,246,0.15); color: #3b82f6; }
.arc-badge-future { background: rgba(180,255,0,0.12);  color: var(--color-green-light); }

.arc-service { font-size: 13px; font-weight: 600; color: var(--color-text); }

/* Trainer row */
.arc-trainer-row {
  display: flex; align-items: center; gap: 12px;
}
.arc-avatar {
  width: 38px; height: 38px; border-radius: 50%;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700;
  color: var(--color-green-light);
  flex-shrink: 0;
}
.arc-trainer-info { flex: 1; min-width: 0; }
.arc-trainer-name { font-size: 14px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.arc-appt-meta {
  display: flex; flex-wrap: wrap; gap: 8px;
  font-size: 11px; color: var(--color-muted);
}

/* Countdown */
.arc-countdown {
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
}
.countdown-today  { background: rgba(245,158,11,0.08); border-color: rgba(245,158,11,0.3); }
.countdown-soon   { background: rgba(59,130,246,0.06); border-color: rgba(59,130,246,0.3); }

.countdown-units {
  display: flex; align-items: center; gap: 4px;
}
.cd-unit {
  display: flex; flex-direction: column; align-items: center; min-width: 44px;
}
.cd-num {
  font-family: 'Barlow Condensed', monospace;
  font-size: 28px; font-weight: 700; line-height: 1;
  color: var(--color-text);
}
.countdown-today .cd-num  { color: #f59e0b; }
.countdown-soon .cd-num   { color: #3b82f6; }
.cd-lbl  {
  font-size: 9px; color: var(--color-muted);
  text-transform: uppercase; letter-spacing: 0.5px;
  margin-top: 2px;
}
.cd-sep  {
  font-size: 22px; font-weight: 700;
  color: var(--color-muted);
  align-self: flex-start; margin-top: 2px; padding: 0 2px;
}

.countdown-label    { font-size: 13px; color: var(--color-muted); }
.countdown-sublabel { font-size: 11px; color: var(--color-muted); }

/* Trainer remarks */
.arc-remarks {
  display: flex; align-items: flex-start; gap: 7px;
  font-size: 12px; color: var(--color-muted);
  background: var(--color-bg-3);
  border-radius: var(--radius-md);
  padding: 8px 10px;
}
.arc-remarks-icon { flex-shrink: 0; font-size: 13px; }

/* Stat pill — upcoming sessions */
.stat-pill-appt {
  background: rgba(180,255,0,0.08);
  border-color: rgba(180,255,0,0.35);
}
.stat-pill-appt .stat-num { color: var(--color-green-light); }

/* ── Transitions ───────────────────────────────────────────────────────────── */
.slide-down-enter-active { transition: all 0.3s ease; }
.slide-down-enter-from   { opacity: 0; transform: translateY(-8px); }

/* ── Trainer Feedback Banner ─────────────────────────────────────────────────── */
.feedback-banner {
  background: rgba(59,130,246,0.1);
  border: 1px solid rgba(59,130,246,0.35);
  border-radius: var(--radius-md);
  padding: 14px 20px;
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
}
.feedback-banner-icon { font-size: 20px; flex-shrink: 0; }
.feedback-banner-text { flex: 1; font-size: 14px; color: var(--color-text); }
.feedback-banner-text strong { color: #3b82f6; }
.feedback-banner-link { font-size: 13px; font-weight: 600; color: #3b82f6; text-decoration: none; white-space: nowrap; }
.feedback-banner-link:hover { text-decoration: underline; }

/* ── Trainer Feedback Section ────────────────────────────────────────────────── */
.feedback-section { display: flex; flex-direction: column; gap: 16px; }

.feedback-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.feedback-card {
  background: var(--color-bg-2);
  border: 1px solid rgba(59,130,246,0.3);
  border-left: 4px solid #3b82f6;
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex; flex-direction: column; gap: 12px;
}

.feedback-card-top { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.feedback-service-badge {
  font-size: 11px; font-weight: 700;
  background: rgba(59,130,246,0.12); color: #3b82f6;
  padding: 3px 8px; border-radius: 20px;
}
.feedback-date { font-size: 11px; color: var(--color-muted); margin-left: auto; }

.feedback-trainer-row { display: flex; align-items: center; gap: 12px; }
.feedback-avatar { width: 36px; height: 36px; border-radius: 50%; background: var(--color-bg-3); border: 1px solid var(--color-border); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; color: #3b82f6; flex-shrink: 0; }
.feedback-trainer-info { flex: 1; min-width: 0; }
.feedback-trainer-name { font-size: 14px; font-weight: 700; color: var(--color-text); }
.feedback-meta { font-size: 11px; color: var(--color-muted); margin-top: 2px; }

.feedback-message-box {
  background: rgba(59,130,246,0.06);
  border: 1px solid rgba(59,130,246,0.2);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}
.feedback-message-header {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 700; color: #3b82f6;
  margin-bottom: 8px;
}
.feedback-message-icon { font-size: 14px; }
.feedback-message-label {}
.feedback-message-text { margin: 0; font-size: 13px; color: var(--color-text); line-height: 1.6; }

.feedback-view-btn {
  align-self: flex-start;
  font-size: 12px; font-weight: 600;
  color: #3b82f6; text-decoration: none;
  padding: 6px 12px;
  border: 1px solid rgba(59,130,246,0.3);
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}
.feedback-view-btn:hover { background: rgba(59,130,246,0.08); }

/* ── Slide-down transition ──────────────────────────────────────────────────── */
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.3s ease; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-10px); }

/* ── Mobile ─────────────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  /* Welcome header: stack, tighten padding, size down the big name */
  .welcome-header { padding: 18px 16px; }
  .welcome-name   { font-size: 24px; }
  .welcome-avatar { width: 48px; height: 48px; font-size: 17px; }
  /* Stats span the full width and share the row evenly */
  .welcome-stats  { width: 100%; }
  .welcome-stats .stat-pill { flex: 1; min-width: 0; padding: 10px 8px; }

  /* Card grids never demand 300px on a narrow phone → single column */
  .updates-grid,
  .appt-reminder-grid,
  .feedback-grid,
  .loading-grid { grid-template-columns: 1fr; }
}
</style>
