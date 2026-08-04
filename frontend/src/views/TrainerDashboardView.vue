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
          <p class="welcome-sub">Trainer Dashboard</p>
        </div>
      </div>
      <div class="welcome-stats">
        <div class="stat-pill">
          <span class="stat-num">{{ trainerStore.athletes.length }}</span>
          <span class="stat-lbl">Athletes</span>
        </div>
        <div class="stat-pill" :class="{ 'stat-pill-alert': pendingAppointments.length > 0 }">
          <span class="stat-num">{{ pendingAppointments.length }}</span>
          <span class="stat-lbl">Pending Requests</span>
        </div>
        <div class="stat-pill" :class="{ 'stat-pill-appt': upcomingAppointments.length > 0 }">
          <span class="stat-num">{{ upcomingAppointments.length }}</span>
          <span class="stat-lbl">Upcoming Sessions</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num">{{ totalPlans }}</span>
          <span class="stat-lbl">Total Plans</span>
        </div>
      </div>
    </div>

    <!-- ── Pending Appointments Banner ──────────────────────────────────────── -->
    <transition name="slide-down">
      <div v-if="pendingAppointments.length > 0" class="pending-banner">
        <div class="pending-banner-icon">🔔</div>
        <div class="pending-banner-text">
          <strong>You have {{ pendingAppointments.length }} pending appointment request{{ pendingAppointments.length > 1 ? 's' : '' }}.</strong>
          <span> Review and respond below.</span>
        </div>
        <a href="#pending-section" class="pending-banner-link">View Requests ↓</a>
      </div>
    </transition>

    <!-- ── Loading state ───────────────────────────────────────────────────── -->
    <div v-if="loading" class="loading-grid">
      <div class="skeleton-card" v-for="n in 3" :key="n"></div>
    </div>

    <template v-else>

      <!-- ── Pending Appointments Section ────────────────────────────────────── -->
      <div id="pending-section" v-if="pendingAppointments.length > 0" class="section">
        <div class="section-header">
          <h2 class="section-title">
            <span class="section-icon">🔔</span> Pending Requests
          </h2>
          <p class="section-sub">Athletes are waiting for your response.</p>
        </div>
        <div class="cards-grid">
          <div v-for="appt in pendingAppointments" :key="appt.id" class="pending-card">
            <div class="pending-card-top">
              <span class="pending-badge">Pending</span>
              <span class="service-badge">{{ serviceLabel(appt.serviceType) }}</span>
              <span class="card-date">{{ formatDate(appt.createdAt) }}</span>
            </div>
            <div class="person-row">
              <div class="person-avatar">{{ initials(appt.athleteName) }}</div>
              <div class="person-info">
                <div class="person-name">{{ appt.athleteName }}</div>
                <div class="appt-meta">
                  <span>📅 {{ formatApptDate(appt.date) }}</span>
                  <span>🕐 {{ appt.time }}</span>
                  <span>⏱ {{ appt.duration }} min</span>
                  <span>{{ appt.location === 'FACE_TO_FACE' ? '🤝 Face to Face' : '💻 Online' }}</span>
                </div>
              </div>
            </div>
            <div v-if="appt.purpose" class="purpose-box">
              <span class="purpose-label">Purpose:</span> {{ appt.purpose }}
            </div>
            <div v-if="appt.specialRequirements" class="purpose-box">
              <span class="purpose-label">Special Requirements:</span> {{ appt.specialRequirements }}
            </div>
            <router-link to="/appointments" class="card-action-link">
              Manage in Appointments →
            </router-link>
          </div>
        </div>
      </div>

      <!-- ── Upcoming Appointments + Athletes Overview (side by side) ────────── -->
      <div class="two-col-row">

        <!-- Upcoming Appointments -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">
              <span class="section-icon">📅</span> Upcoming Sessions
            </h2>
            <p class="section-sub">Your confirmed upcoming appointments.</p>
          </div>
          <div v-if="upcomingAppointments.length === 0" class="empty-state">
            <span class="empty-icon">📅</span>
            <p>No upcoming sessions scheduled.</p>
          </div>
          <div v-else class="upcoming-list">
            <div
              v-for="appt in upcomingAppointments"
              :key="appt.id"
              class="upcoming-card"
              :class="{
                'card-today': isToday(appt.date),
                'card-soon':  isSoon(appt.date) && !isToday(appt.date),
                'card-future': !isSoon(appt.date) && !isToday(appt.date)
              }"
            >
              <div class="upcoming-card-top">
                <span class="time-badge" :class="{
                  'badge-today':  isToday(appt.date),
                  'badge-soon':   isSoon(appt.date) && !isToday(appt.date),
                  'badge-future': !isSoon(appt.date) && !isToday(appt.date)
                }">
                  {{ isToday(appt.date) ? '🔔 Today!' : isSoon(appt.date) ? '⚡ Soon' : '✅ Confirmed' }}
                </span>
                <span class="service-badge">{{ serviceLabel(appt.serviceType) }}</span>
              </div>
              <div class="person-row">
                <div class="person-avatar person-avatar-blue">{{ initials(appt.athleteName) }}</div>
                <div class="person-info">
                  <div class="person-name">{{ appt.athleteName }}</div>
                  <div class="appt-meta">
                    <span>📅 {{ formatApptDate(appt.date) }}</span>
                    <span>🕐 {{ appt.time }}</span>
                    <span>⏱ {{ appt.duration }} min</span>
                    <span>{{ appt.location === 'FACE_TO_FACE' ? '🤝 Face to Face' : '💻 Online' }}</span>
                  </div>
                </div>
              </div>

              <!-- Countdown -->
              <div class="countdown-box" :class="{ 'countdown-today': isToday(appt.date), 'countdown-soon': isSoon(appt.date) && !isToday(appt.date) }">
                <template v-if="isPast(appt.date, appt.time)">
                  <span class="countdown-label">Session time passed</span>
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
                  <span class="countdown-sublabel">until session</span>
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
                  </div>
                  <span class="countdown-sublabel">until session</span>
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- Athletes Overview -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">
              <span class="section-icon">🏉</span> Athletes Overview
            </h2>
            <p class="section-sub">Your athletes' plan progress at a glance.</p>
          </div>
          <div v-if="trainerStore.athletes.length === 0" class="empty-state">
            <span class="empty-icon">👥</span>
            <p>No athletes registered yet.</p>
          </div>
          <div v-else class="athletes-list">
            <div v-for="athlete in trainerStore.athletes" :key="athlete.id" class="athlete-card">
              <div class="athlete-card-top">
                <div class="person-avatar person-avatar-green">{{ initials(athlete.fullName) }}</div>
                <div class="athlete-info">
                  <div class="person-name">{{ athlete.fullName }}</div>
                  <div class="athlete-meta">
                    <span v-if="athlete.rugbyPosition" class="chip">🏉 {{ athlete.rugbyPosition }}</span>
                  </div>
                </div>
              </div>
              <div class="progress-bars">
                <div class="mini-progress">
                  <div class="mini-progress-header">
                    <span class="mini-progress-label">💪 Workout</span>
                    <span class="mini-progress-pct">{{ athlete.workoutProgress || 0 }}%</span>
                  </div>
                  <div class="mini-bar">
                    <div class="mini-bar-fill bar-workout" :style="{ width: (athlete.workoutProgress || 0) + '%' }"></div>
                  </div>
                  <span class="mini-plan-count">{{ athlete.workoutPlanCount || 0 }} plan{{ (athlete.workoutPlanCount || 0) !== 1 ? 's' : '' }}</span>
                </div>
                <div class="mini-progress">
                  <div class="mini-progress-header">
                    <span class="mini-progress-label">🥗 Meal</span>
                    <span class="mini-progress-pct">{{ athlete.mealProgress || 0 }}%</span>
                  </div>
                  <div class="mini-bar">
                    <div class="mini-bar-fill bar-meal" :style="{ width: (athlete.mealProgress || 0) + '%' }"></div>
                  </div>
                  <span class="mini-plan-count">{{ athlete.mealPlanCount || 0 }} plan{{ (athlete.mealPlanCount || 0) !== 1 ? 's' : '' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>

      <!-- ── Quick Links ───────────────────────────────────────────────────── -->
      <div class="quick-links">
        <h2 class="section-title">Quick Actions</h2>
        <div class="quick-grid">
          <router-link to="/trainer/workouts" class="quick-card">
            <span class="quick-icon">💪</span>
            <div>
              <div class="quick-title">Manage Workout Plans</div>
              <div class="quick-sub">View and edit athlete workout plans</div>
            </div>
            <span class="quick-arrow">→</span>
          </router-link>
          <router-link to="/trainer/meals" class="quick-card">
            <span class="quick-icon">🥗</span>
            <div>
              <div class="quick-title">Manage Meal Plans</div>
              <div class="quick-sub">View and edit athlete meal plans</div>
            </div>
            <span class="quick-arrow">→</span>
          </router-link>
          <router-link to="/appointments" class="quick-card">
            <span class="quick-icon">📅</span>
            <div>
              <div class="quick-title">Appointments</div>
              <div class="quick-sub">Review requests and manage your schedule</div>
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
import { useTrainerStore }     from '@/stores/trainer'
import { useAppointmentStore } from '@/stores/appointment'

const authStore    = useAuthStore()
const trainerStore = useTrainerStore()
const apptStore    = useAppointmentStore()

const loading = ref(true)

const now = ref(new Date())
let clockTimer = null

onMounted(async () => {
  loading.value = true
  await Promise.all([
    trainerStore.fetchAthletes(),
    apptStore.fetchTrainerAppointments()
  ])
  loading.value = false
  clockTimer = setInterval(() => { now.value = new Date() }, 1000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})

// ── Computed ──────────────────────────────────────────────────────────────────

const totalPlans = computed(() =>
  trainerStore.athletes.reduce((sum, a) => sum + (a.workoutPlanCount || 0) + (a.mealPlanCount || 0), 0)
)

const pendingAppointments = computed(() =>
  apptStore.appointments
    .filter(a => a.status === 'PENDING')
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
)

const upcomingAppointments = computed(() => {
  const todayStr = now.value.toISOString().split('T')[0]
  return apptStore.appointments
    .filter(a => a.status === 'APPROVED' && a.date >= todayStr)
    .sort((a, b) => {
      const da = new Date(`${a.date}T${a.time}:00`)
      const db = new Date(`${b.date}T${b.time}:00`)
      return da - db
    })
})

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

function formatApptDate(d) {
  if (!d) return ''
  return new Date(d + 'T00:00:00').toLocaleDateString('en-MY', {
    weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
  })
}

function serviceLabel(s) {
  const m = {
    FITNESS_TRAINING:      '💪 Fitness Training',
    NUTRITION_COUNSELLING: '🥗 Nutrition Counselling',
    WELLNESS_COACHING:     '🧘 Wellness Coaching'
  }
  return m[s] || s
}

function isToday(date) {
  return date === now.value.toISOString().split('T')[0]
}

function isSoon(date) {
  const dt = new Date(date + 'T00:00:00')
  const todayMs = new Date(now.value.toISOString().split('T')[0] + 'T00:00:00').getTime()
  return (dt.getTime() - todayMs) / (1000 * 60 * 60 * 24) <= 3
}

function isPast(date, time) {
  return new Date(`${date}T${time}:00`) <= now.value
}

function countdown(date, time) {
  const target = new Date(`${date}T${time}:00`)
  const diffMs = target - now.value
  if (diffMs <= 0) return { days: 0, hours: '00', minutes: '00', seconds: '00' }
  const totalSecs = Math.floor(diffMs / 1000)
  const days    = Math.floor(totalSecs / (60 * 60 * 24))
  const hours   = String(Math.floor((totalSecs % (60 * 60 * 24)) / 3600)).padStart(2, '0')
  const minutes = String(Math.floor((totalSecs % 3600) / 60)).padStart(2, '0')
  const seconds = String(totalSecs % 60).padStart(2, '0')
  return { days, hours, minutes, seconds }
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
.stat-pill-appt {
  background: rgba(180,255,0,0.08);
  border-color: rgba(180,255,0,0.35);
}
.stat-num { font-size: 22px; font-weight: 700; color: var(--color-green-light); line-height: 1; }
.stat-pill-alert .stat-num { color: #f59e0b; }
.stat-pill-appt .stat-num { color: var(--color-green-light); }
.stat-lbl { font-size: 11px; color: var(--color-muted); margin-top: 3px; white-space: nowrap; }

/* ── Pending Banner ────────────────────────────────────────────────────────── */
.pending-banner {
  background: rgba(245,158,11,0.1);
  border: 1px solid rgba(245,158,11,0.35);
  border-radius: var(--radius-md);
  padding: 14px 20px;
  display: flex; align-items: center; gap: 12px;
  flex-wrap: wrap;
}
.pending-banner-icon { font-size: 20px; flex-shrink: 0; }
.pending-banner-text { flex: 1; font-size: 14px; color: var(--color-text); }
.pending-banner-text strong { color: #f59e0b; }
.pending-banner-link {
  font-size: 13px; font-weight: 600;
  color: #f59e0b; text-decoration: none; white-space: nowrap;
}
.pending-banner-link:hover { text-decoration: underline; }

/* ── Loading skeletons ─────────────────────────────────────────────────────── */
.loading-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 20px; }
.skeleton-card {
  height: 200px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

/* ── Section ───────────────────────────────────────────────────────────────── */
.section { display: flex; flex-direction: column; gap: 16px; }

.section-header { margin-bottom: 4px; }
.section-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 22px; font-weight: 700;
  color: var(--color-text); margin: 0 0 4px;
  display: flex; align-items: center; gap: 8px;
}
.section-icon { font-size: 18px; }
.section-sub { margin: 0; font-size: 13px; color: var(--color-muted); }

/* ── Two Column Row ────────────────────────────────────────────────────────── */
.two-col-row {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.two-col-row > * {
  flex: 1;
  min-width: 0;
}
@media (max-width: 900px) {
  .two-col-row { flex-direction: column; }
}

/* ── Cards Grid ────────────────────────────────────────────────────────────── */
.cards-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

/* ── Empty State ───────────────────────────────────────────────────────────── */
.empty-state {
  background: var(--color-bg-2);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  padding: 32px 20px;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  color: var(--color-muted);
}
.empty-icon { font-size: 28px; }
.empty-state p { margin: 0; font-size: 14px; }

/* ── Pending Card ──────────────────────────────────────────────────────────── */
.pending-card {
  background: var(--color-bg-2);
  border: 1px solid rgba(245,158,11,0.3);
  border-left: 4px solid #f59e0b;
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex; flex-direction: column; gap: 10px;
}

.pending-card-top {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}

.pending-badge {
  font-size: 11px; font-weight: 700;
  background: rgba(245,158,11,0.15); color: #f59e0b;
  padding: 3px 9px; border-radius: 20px;
}

.service-badge {
  font-size: 12px; font-weight: 600; color: var(--color-text);
}

.card-date { font-size: 11px; color: var(--color-muted); margin-left: auto; }

/* ── Person Row (shared) ───────────────────────────────────────────────────── */
.person-row {
  display: flex; align-items: center; gap: 12px;
}
.person-avatar {
  width: 38px; height: 38px; border-radius: 50%;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700;
  color: var(--color-green-light);
  flex-shrink: 0;
}
.person-avatar-blue { color: #3b82f6; }
.person-avatar-green { color: var(--color-green-light); }
.person-info { flex: 1; min-width: 0; }
.person-name { font-size: 14px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.appt-meta {
  display: flex; flex-wrap: wrap; gap: 8px;
  font-size: 11px; color: var(--color-muted);
}

/* ── Purpose Box ───────────────────────────────────────────────────────────── */
.purpose-box {
  font-size: 12px; color: var(--color-muted);
  background: var(--color-bg-3);
  border-radius: var(--radius-md);
  padding: 8px 10px;
}
.purpose-label { font-weight: 600; color: var(--color-text-dim); }

/* ── Card Action Link ──────────────────────────────────────────────────────── */
.card-action-link {
  align-self: flex-start;
  font-size: 12px; font-weight: 600;
  color: #f59e0b; text-decoration: none;
}
.card-action-link:hover { text-decoration: underline; }

/* ── Upcoming Card ─────────────────────────────────────────────────────────── */
.upcoming-card {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex; flex-direction: column; gap: 12px;
}
.card-today  { border-left: 4px solid #f59e0b; background: rgba(245,158,11,0.04); }
.card-soon   { border-left: 4px solid #3b82f6; background: rgba(59,130,246,0.04); }
.card-future { border-left: 4px solid var(--color-green-light); background: rgba(180,255,0,0.03); }

.upcoming-card-top { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.time-badge {
  font-size: 11px; font-weight: 700;
  padding: 3px 9px; border-radius: 20px;
}
.badge-today  { background: rgba(245,158,11,0.15); color: #f59e0b; }
.badge-soon   { background: rgba(59,130,246,0.15); color: #3b82f6; }
.badge-future { background: rgba(180,255,0,0.12);  color: var(--color-green-light); }

/* ── Countdown ─────────────────────────────────────────────────────────────── */
.countdown-box {
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
}
.countdown-today { background: rgba(245,158,11,0.08); border-color: rgba(245,158,11,0.3); }
.countdown-soon  { background: rgba(59,130,246,0.06); border-color: rgba(59,130,246,0.3); }

.countdown-units { display: flex; align-items: center; gap: 4px; }
.cd-unit { display: flex; flex-direction: column; align-items: center; min-width: 44px; }
.cd-num {
  font-family: 'Barlow Condensed', monospace;
  font-size: 28px; font-weight: 700; line-height: 1;
  color: var(--color-text);
}
.countdown-today .cd-num { color: #f59e0b; }
.countdown-soon .cd-num  { color: #3b82f6; }
.cd-lbl {
  font-size: 9px; color: var(--color-muted);
  text-transform: uppercase; letter-spacing: 0.5px;
  margin-top: 2px;
}
.cd-sep {
  font-size: 22px; font-weight: 700;
  color: var(--color-muted);
  align-self: flex-start; margin-top: 2px; padding: 0 2px;
}
.countdown-label    { font-size: 13px; color: var(--color-muted); }
.countdown-sublabel { font-size: 11px; color: var(--color-muted); }

/* ── Upcoming List ─────────────────────────────────────────────────────────── */
.upcoming-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 500px;
  overflow-y: auto;
}

/* ── Athletes List ─────────────────────────────────────────────────────────── */
.athletes-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 500px;
  overflow-y: auto;
}

.athlete-card {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  display: flex; flex-direction: column; gap: 12px;
}

.athlete-card-top {
  display: flex; align-items: center; gap: 12px;
}

.athlete-info { flex: 1; min-width: 0; }
.athlete-meta { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; }

.chip {
  font-size: 11px;
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  padding: 3px 9px; border-radius: 20px;
  color: var(--color-muted);
}

/* ── Mini Progress Bars ────────────────────────────────────────────────────── */
.progress-bars {
  display: flex; gap: 16px;
}

.mini-progress {
  flex: 1;
  display: flex; flex-direction: column; gap: 4px;
}

.mini-progress-header {
  display: flex; justify-content: space-between; align-items: center;
}
.mini-progress-label { font-size: 11px; color: var(--color-muted); }
.mini-progress-pct { font-size: 12px; font-weight: 700; color: var(--color-text); }

.mini-bar {
  height: 6px;
  background: var(--color-bg-3);
  border-radius: 3px;
  overflow: hidden;
}
.mini-bar-fill {
  height: 100%; border-radius: 3px;
  transition: width 0.6s ease;
}
.bar-workout { background: var(--color-green-light); }
.bar-meal    { background: #3b82f6; }

.mini-plan-count {
  font-size: 10px; color: var(--color-muted);
}

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

/* ── Transitions ───────────────────────────────────────────────────────────── */
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.3s ease; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-10px); }

/* ── Mobile ────────────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  /* Welcome header: tighter padding, smaller name/avatar, full-width stats */
  .welcome-header { padding: 18px 16px; }
  .welcome-name   { font-size: 24px; }
  .welcome-avatar { width: 48px; height: 48px; }
  .welcome-stats  { width: 100%; }
  .welcome-stats .stat-pill { flex: 1; min-width: 0; }
  /* Loading skeletons stack instead of 3 cramped columns */
  .loading-grid { grid-template-columns: 1fr; }
}
</style>
