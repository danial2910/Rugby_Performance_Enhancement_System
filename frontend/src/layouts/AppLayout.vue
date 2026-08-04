<template>
  <!-- App Layout: sidebar + header + main content -->
  <!-- Used by all authenticated routes -->
  <div class="app-layout" :class="{ 'is-admin': authStore.isAdmin }">

    <!-- ── Sidebar ──────────────────────────────────── -->
    <aside class="sidebar">
      <!-- Logo -->
      <div class="sidebar-logo">
        <div class="logo-mark">
          <img src="/logo.png" alt="UTM Pirates Rugby" class="logo-img" />
        </div>
        <div class="logo-text">
          <span class="logo-title">RPES</span>
          <span class="logo-sub">UTM Pirates</span>
        </div>
      </div>

      <!-- Navigation -->
      <nav class="sidebar-nav">
        <!-- Athlete nav -->
        <template v-if="authStore.isAthlete">
          <p class="nav-section-label">Main</p>
          <RouterLink to="/dashboard"    class="nav-item"><span class="nav-icon">📊</span>Dashboard</RouterLink>
          <RouterLink to="/profile"      class="nav-item"><span class="nav-icon">👤</span>My Profile</RouterLink>

          <p class="nav-section-label">AI Features</p>
          <RouterLink to="/chatbot"      class="nav-item"><span class="nav-icon">💬</span>AI Chatbot</RouterLink>
          <RouterLink to="/meal-planner" class="nav-item"><span class="nav-icon">🥗</span>Meal Planner</RouterLink>
          <RouterLink to="/workout"      class="nav-item"><span class="nav-icon">🏋️</span>Workout Planner</RouterLink>

          <p class="nav-section-label">Manage</p>
          <RouterLink to="/appointments" class="nav-item"><span class="nav-icon">📅</span>Appointments</RouterLink>
        </template>

        <!-- Trainer nav -->
        <template v-if="authStore.isTrainer">
          <p class="nav-section-label">Main</p>
          <RouterLink to="/trainer/dashboard" class="nav-item"><span class="nav-icon">📊</span>Dashboard</RouterLink>
          <RouterLink to="/profile"           class="nav-item"><span class="nav-icon">👤</span>My Profile</RouterLink>
          <RouterLink to="/appointments"      class="nav-item"><span class="nav-icon">📅</span>Appointments</RouterLink>
          <p class="nav-section-label">Plan Management</p>
          <RouterLink to="/trainer/workouts"  class="nav-item"><span class="nav-icon">💪</span>Workout Plans</RouterLink>
          <RouterLink to="/trainer/meals"     class="nav-item"><span class="nav-icon">🥗</span>Meal Plans</RouterLink>
        </template>
      </nav>

      <!-- User chip at bottom -->
      <div class="sidebar-footer">
        <div class="user-chip">
          <div class="avatar">
            <img v-if="authStore.profilePicture" :src="authStore.profilePicture" alt="Profile" class="avatar-img" />
            <span v-else>{{ initials }}</span>
          </div>
          <div class="user-info">
            <p class="user-name">{{ authStore.fullName }}</p>
            <p class="user-role">{{ authStore.userRole }}</p>
          </div>
        </div>
      </div>
    </aside>

    <!-- ── Main Area ─────────────────────────────────── -->
    <div class="main-area">
      <!-- Header -->
      <header class="app-header">
        <h1 class="page-title">{{ pageTitle }}</h1>
        <div class="header-actions">
          <RouterLink v-if="authStore.isAthlete" to="/chatbot" class="btn btn-ghost">💬 Ask AI</RouterLink>
          <button class="btn btn-danger-ghost" @click="handleLogout">
            Sign Out
          </button>
        </div>
      </header>

      <!-- Page content -->
      <main class="page-content">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>

    <!-- ── Mobile Bottom Tab Bar (mobile only; hidden when no tabs, e.g. admin) ── -->
    <nav v-if="bottomTabs.length" class="bottom-nav">
      <RouterLink
        v-for="tab in bottomTabs"
        :key="tab.to"
        :to="tab.to"
        class="tab-item"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </RouterLink>
      <button
        class="tab-item"
        :class="{ 'tab-item-active': moreActive }"
        @click="showMore = true"
      >
        <span class="tab-icon">⋯</span>
        <span class="tab-label">More</span>
      </button>
    </nav>

    <!-- ── "More" Bottom Sheet (mobile only) ─────────── -->
    <Transition name="sheet">
      <div v-if="showMore" class="sheet-backdrop" @click="showMore = false">
        <div class="more-sheet" @click.stop>
          <div class="sheet-handle"></div>

          <div class="sheet-user">
            <div class="avatar">
              <img v-if="authStore.profilePicture" :src="authStore.profilePicture" alt="Profile" class="avatar-img" />
              <span v-else>{{ initials }}</span>
            </div>
            <div class="user-info">
              <p class="user-name">{{ authStore.fullName }}</p>
              <p class="user-role">{{ authStore.userRole }}</p>
            </div>
          </div>

          <div class="sheet-links">
            <RouterLink
              v-for="item in moreItems"
              :key="item.to"
              :to="item.to"
              class="sheet-item"
              @click="showMore = false"
            >
              <span class="tab-icon">{{ item.icon }}</span>{{ item.label }}
            </RouterLink>
          </div>

          <button class="sheet-item sheet-signout" @click="handleLogout">
            <span class="tab-icon">🚪</span>Sign Out
          </button>
        </div>
      </div>
    </Transition>

  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route     = useRoute()

const showMore = ref(false)

// Close the "More" sheet whenever the route changes
watch(() => route.fullPath, () => { showMore.value = false })

const initials = computed(() => {
  const name = authStore.fullName || authStore.username || '?'
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
})

// ── Mobile navigation (bottom tab bar + "More" sheet) ──────────────
// First 4 items become bottom tabs; the rest overflow into "More".
const navItems = computed(() => {
  // Admin only has the User Management page — no bottom tabs needed.
  if (authStore.isAdmin) return []
  if (authStore.isTrainer) {
    return [
      { to: '/trainer/dashboard', icon: '📊', label: 'Dashboard' },
      { to: '/trainer/workouts',  icon: '💪', label: 'Workouts' },
      { to: '/trainer/meals',     icon: '🥗', label: 'Meals' },
      { to: '/appointments',      icon: '📅', label: 'Appts' },
      { to: '/profile',           icon: '👤', label: 'Profile' }
    ]
  }
  // Athlete (default)
  return [
    { to: '/dashboard',    icon: '📊', label: 'Dashboard' },
    { to: '/meal-planner', icon: '🥗', label: 'Meals' },
    { to: '/workout',      icon: '🏋️', label: 'Workout' },
    { to: '/chatbot',      icon: '💬', label: 'AI Chat' },
    { to: '/appointments', icon: '📅', label: 'Appts' },
    { to: '/profile',      icon: '👤', label: 'Profile' }
  ]
})

const bottomTabs = computed(() => navItems.value.slice(0, 4))
const moreItems  = computed(() => navItems.value.slice(4))
const moreActive = computed(() =>
  moreItems.value.some(item => route.path.startsWith(item.to))
)

const pageTitles = {
  Dashboard:        'Dashboard',
  TrainerDashboard: 'Trainer Dashboard',
  TrainerWorkouts: 'Workout Plan Management',
  TrainerMeals:     'Meal Plan Management',
  MealPlanner:      'Meal Planner',
  Workout:          'Workout Planner',
  Chatbot:          'AI Chatbot',
  Appointments:     'Appointments',
  Profile:          'My Profile'
}

const pageTitle = computed(() => pageTitles[route.name] || 'Rugby Performance Enhancement System')

async function handleLogout() {
  await authStore.logout()
}
</script>

<style scoped>
.app-layout {
  display: grid;
  grid-template-columns: var(--sidebar-width) 1fr;
  min-height: 100vh;
}

/* ── Sidebar ─────────────────────────────────────────── */
.sidebar {
  background: var(--color-bg-2);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-bottom: 1px solid var(--color-border);
}

.logo-mark {
  width: 38px; height: 38px;
  background: var(--color-green);
  border-radius: 9px;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}
.logo-img {
  width: 100%; height: 100%;
  object-fit: contain;
  mix-blend-mode: screen;
  padding: 3px;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.4px;
  color: var(--color-text);
  line-height: 1.1;
}

.logo-sub {
  font-size: 11px;
  color: var(--color-muted);
}

.sidebar-nav {
  flex: 1;
  padding: 14px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-section-label {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 1.4px;
  text-transform: uppercase;
  color: var(--color-muted);
  padding: 14px 8px 5px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 12px;
  border-radius: var(--radius-sm);
  font-size: 13.5px;
  color: var(--color-text-dim);
  text-decoration: none;
  transition: background var(--transition), color var(--transition);
}

.nav-item:hover {
  background: var(--color-surface);
  color: var(--color-text);
}

.nav-item.router-link-active {
  background: var(--color-surface-2);
  color: var(--color-green-light);
  font-weight: 500;
}

.nav-icon {
  font-size: 15px;
  width: 18px;
  text-align: center;
}

.sidebar-footer {
  padding: 14px;
  border-top: 1px solid var(--color-border);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: var(--color-surface);
  border-radius: var(--radius-sm);
}

.avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: var(--color-green-dim);
  color: var(--color-green-light);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-img {
  width: 100%; height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.user-info { min-width: 0; }

.user-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 11px;
  color: var(--color-muted);
}

/* ── Main Area ───────────────────────────────────────── */
.main-area {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  overflow: hidden;
}

.app-header {
  height: var(--header-height);
  background: var(--color-bg-2);
  border-bottom: 1px solid var(--color-border);
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}

.page-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-content {
  flex: 1;
  padding: 28px;
  overflow-y: auto;
}

/* ── Buttons (global in app) ─────────────────────────── */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  border: none;
  text-decoration: none;
  transition: background var(--transition);
}

.btn-ghost {
  background: var(--color-surface);
  color: var(--color-text);
}

.btn-ghost:hover { background: var(--color-surface-2); }

.btn-danger-ghost {
  background: transparent;
  color: var(--color-muted);
  border: 1px solid var(--color-border);
}

.btn-danger-ghost:hover {
  background: var(--color-error-bg);
  color: var(--color-error);
  border-color: var(--color-error-border);
}

/* ── Mobile bottom nav + sheet (hidden on desktop) ──── */
.bottom-nav { display: none; }
.sheet-backdrop { display: none; }

/* ═══════════════════════════════════════════════════════
   MOBILE  (≤ 768px)
   ═══════════════════════════════════════════════════════ */
@media (max-width: 768px) {
  /* Single-column: drop the fixed sidebar */
  .app-layout {
    grid-template-columns: 1fr;
  }
  .sidebar {
    display: none;
  }

  /* Compact header */
  .app-header {
    height: 52px;
    padding: 0 16px;
  }
  .page-title {
    font-size: 17px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  /* Logout + Ask-AI live in the tab bar / More sheet on mobile.
     Admin has no bottom bar, so keep the header Sign Out reachable. */
  .app-layout:not(.is-admin) .header-actions {
    display: none;
  }

  /* Leave room for the fixed bottom bar */
  .page-content {
    padding: 16px 16px calc(72px + env(safe-area-inset-bottom));
  }
  /* Admin has no bottom bar → no reserved space needed */
  .is-admin .page-content {
    padding: 16px;
  }

  /* ── Bottom tab bar ── */
  .bottom-nav {
    display: flex;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 40;
    background: var(--color-bg-2);
    border-top: 1px solid var(--color-border);
    padding-bottom: env(safe-area-inset-bottom);
  }
  .tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    padding: 8px 2px;
    min-height: 56px;
    background: none;
    border: none;
    cursor: pointer;
    text-decoration: none;
    color: var(--color-muted);
    font-family: inherit;
    transition: color var(--transition);
  }
  .tab-item .tab-icon {
    font-size: 20px;
    line-height: 1;
  }
  .tab-label {
    font-size: 10.5px;
    font-weight: 500;
    letter-spacing: 0.2px;
  }
  .tab-item.router-link-active,
  .tab-item-active {
    color: var(--color-green-light);
  }

  /* ── "More" bottom sheet ── */
  .sheet-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 50;
    background: rgba(0, 0, 0, 0.55);
  }
  .more-sheet {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: var(--color-bg-2);
    border-top: 1px solid var(--color-border);
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    padding: 10px 14px calc(18px + env(safe-area-inset-bottom));
    box-shadow: var(--shadow-lg);
  }
  .sheet-handle {
    width: 40px;
    height: 4px;
    border-radius: 99px;
    background: var(--color-surface-3);
    margin: 4px auto 14px;
  }
  .sheet-user {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 4px 6px 14px;
    border-bottom: 1px solid var(--color-border);
    margin-bottom: 10px;
  }
  .sheet-links {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .sheet-item {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 13px 10px;
    border-radius: var(--radius-sm);
    font-size: 15px;
    font-family: inherit;
    color: var(--color-text);
    text-decoration: none;
    background: none;
    border: none;
    cursor: pointer;
    text-align: left;
  }
  .sheet-item:active {
    background: var(--color-surface);
  }
  .sheet-item.router-link-active {
    color: var(--color-green-light);
  }
  .sheet-signout {
    margin-top: 6px;
    color: var(--color-error);
  }

  /* Sheet slide-up transition */
  .sheet-enter-active,
  .sheet-leave-active { transition: opacity 0.22s ease; }
  .sheet-enter-active .more-sheet,
  .sheet-leave-active .more-sheet { transition: transform 0.26s ease; }
  .sheet-enter-from,
  .sheet-leave-to { opacity: 0; }
  .sheet-enter-from .more-sheet,
  .sheet-leave-to .more-sheet { transform: translateY(100%); }
}
</style>