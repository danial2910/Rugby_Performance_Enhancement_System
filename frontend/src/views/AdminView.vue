<template>
  <div class="admin-page">

    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">User Management</h1>
        <p class="page-subtitle">View all registered accounts in the Rugby AI Planner system</p>
      </div>
      <button class="btn-refresh" @click="loadUsers" :disabled="loading">
        <span v-if="loading" class="spinner-sm"></span>
        <span v-else>↻</span>
        Refresh
      </button>
    </div>

    <!-- Stats row -->
    <div class="stats-row" v-if="!loading && users.length">
      <div class="stat-card">
        <p class="stat-num">{{ users.length }}</p>
        <p class="stat-label">Total Users</p>
      </div>
      <div class="stat-card">
        <p class="stat-num">{{ athleteCount }}</p>
        <p class="stat-label">Athletes</p>
      </div>
      <div class="stat-card">
        <p class="stat-num">{{ trainerCount }}</p>
        <p class="stat-label">Trainers</p>
      </div>
      <div class="stat-card">
        <p class="stat-num">{{ adminCount }}</p>
        <p class="stat-label">Admins</p>
      </div>
    </div>

    <!-- Error state -->
    <div v-if="error" class="alert alert-error">
      <span>✕</span> {{ error }}
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="loading-state">
      <div class="spinner-muted"></div>
      <p>Loading users...</p>
    </div>

    <!-- Users table -->
    <div v-else-if="users.length" class="card">
      <!-- Search bar -->
      <div class="table-toolbar">
        <input
          v-model="search"
          type="text"
          class="search-input"
          placeholder="Search by name, email, or username..."
        />
        <span class="result-count">{{ filteredUsers.length }} result{{ filteredUsers.length !== 1 ? 's' : '' }}</span>
      </div>

      <div class="table-wrap">
        <table class="users-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Full Name</th>
              <th>Username</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Role</th>
              <th>Status</th>
              <th>Joined</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredUsers.length === 0">
              <td colspan="8" class="no-results">No users match your search.</td>
            </tr>
            <tr v-for="(user, index) in filteredUsers" :key="user.userId">
              <td class="col-num">{{ index + 1 }}</td>
              <td class="col-name">
                <span class="avatar">{{ initials(user.fullName) }}</span>
                {{ user.fullName || '—' }}
              </td>
              <td class="col-muted">{{ user.username }}</td>
              <td class="col-muted">{{ user.email }}</td>
              <td class="col-muted">{{ user.phoneNumber || '—' }}</td>
              <td>
                <span class="role-badge" :class="roleCls(user.userRole)">
                  {{ roleLabel(user.userRole) }}
                </span>
              </td>
              <td>
                <span class="status-badge" :class="user.enabled ? 'status-active' : 'status-disabled'">
                  {{ user.enabled ? 'Active' : 'Disabled' }}
                </span>
              </td>
              <td class="col-muted col-date">{{ formatDate(user.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else class="empty-state">
      <div class="empty-icon">👥</div>
      <h3>No users found</h3>
      <p>No accounts have been registered yet.</p>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import adminService from '@/services/adminService'

const users   = ref([])
const loading = ref(false)
const error   = ref(null)
const search  = ref('')

// ── Stats ─────────────────────────────────────────────────────────────────────
const athleteCount = computed(() => users.value.filter(u => u.userRole === 'ATHLETE').length)
const trainerCount = computed(() => users.value.filter(u => u.userRole === 'TRAINER').length)
const adminCount   = computed(() => users.value.filter(u => u.userRole === 'ADMIN').length)

// ── Search filter ─────────────────────────────────────────────────────────────
const filteredUsers = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return users.value
  return users.value.filter(u =>
    (u.fullName  || '').toLowerCase().includes(q) ||
    (u.email     || '').toLowerCase().includes(q) ||
    (u.username  || '').toLowerCase().includes(q)
  )
})

// ── Data loading ──────────────────────────────────────────────────────────────
async function loadUsers() {
  loading.value = true
  error.value   = null
  try {
    const response = await adminService.getAllUsers()
    if (response.success) {
      users.value = response.data
    } else {
      error.value = response.message || 'Failed to load users.'
    }
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to load users. Please try again.'
  } finally {
    loading.value = false
  }
}

onMounted(loadUsers)

// ── Helpers ───────────────────────────────────────────────────────────────────
function initials(name) {
  if (!name) return '?'
  return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
}

function roleLabel(role) {
  const map = { ATHLETE: '🏉 Athlete', TRAINER: '🏋️ Trainer', ADMIN: '🛡️ Admin' }
  return map[role] || role
}

function roleCls(role) {
  return { 'role-athlete': role === 'ATHLETE', 'role-trainer': role === 'TRAINER', 'role-admin': role === 'ADMIN' }
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('en-MY', { day: 'numeric', month: 'short', year: 'numeric' })
}
</script>

<style scoped>
/* ── Page Layout ─────────────────────────────────────────────── */
.admin-page    { display: flex; flex-direction: column; gap: 24px; }
.page-header   { display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.page-title    { font-family: 'Barlow Condensed', sans-serif; font-size: 26px; font-weight: 700; }
.page-subtitle { color: var(--color-muted); font-size: 13.5px; margin-top: 4px; }

/* ── Refresh button ──────────────────────────────────────────── */
.btn-refresh {
  display: flex; align-items: center; gap: 7px;
  padding: 9px 18px;
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); color: var(--color-text);
  font-size: 13.5px; font-weight: 500; cursor: pointer;
  transition: border-color var(--transition); font-family: inherit;
}
.btn-refresh:hover:not(:disabled) { border-color: var(--color-green); color: var(--color-green-light); }
.btn-refresh:disabled { opacity: 0.5; cursor: not-allowed; }

/* ── Stats row ───────────────────────────────────────────────── */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat-card {
  background: var(--color-bg-2); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); padding: 18px 20px; text-align: center;
}
.stat-num   { font-family: 'Barlow Condensed', sans-serif; font-size: 32px; font-weight: 700; color: var(--color-green-light); }
.stat-label { font-size: 12.5px; color: var(--color-muted); margin-top: 2px; }

/* ── Card ────────────────────────────────────────────────────── */
.card {
  background: var(--color-bg-2); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); overflow: hidden;
}

/* ── Table toolbar ───────────────────────────────────────────── */
.table-toolbar {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 20px; border-bottom: 1px solid var(--color-border);
}
.search-input {
  flex: 1; background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); color: var(--color-text);
  font-size: 13.5px; padding: 8px 12px; font-family: inherit;
  transition: border-color var(--transition);
}
.search-input:focus { outline: none; border-color: var(--color-green); }
.result-count { font-size: 12.5px; color: var(--color-muted); white-space: nowrap; }

/* ── Table ───────────────────────────────────────────────────── */
.table-wrap { overflow-x: auto; }
.users-table { width: 100%; border-collapse: collapse; }
.users-table th {
  text-align: left; font-size: 11px; font-weight: 600;
  text-transform: uppercase; letter-spacing: 0.6px;
  color: var(--color-muted); padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
}
.users-table td {
  padding: 13px 16px; border-bottom: 1px solid var(--color-border);
  font-size: 13.5px; color: var(--color-text);
}
.users-table tr:last-child td { border-bottom: none; }
.users-table tr:hover td { background: var(--color-surface); }

.col-num  { color: var(--color-muted); font-size: 12px; width: 36px; }
.col-name { display: flex; align-items: center; gap: 10px; font-weight: 500; }
.col-muted { color: var(--color-text-dim); }
.col-date  { white-space: nowrap; }
.no-results { text-align: center; color: var(--color-muted); padding: 32px !important; }

/* ── Avatar ──────────────────────────────────────────────────── */
.avatar {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--color-green-dim); color: var(--color-green-light);
  font-size: 11px; font-weight: 700; flex-shrink: 0;
}

/* ── Role badges ─────────────────────────────────────────────── */
.role-badge {
  display: inline-block; font-size: 11.5px; font-weight: 500;
  padding: 3px 10px; border-radius: 99px;
}
.role-athlete { background: var(--color-green-dim); color: var(--color-green-light); }
.role-trainer { background: #1e3a5f; color: #60a5fa; }
.role-admin   { background: #2d1e3a; color: #c084fc; }

/* ── Status badges ───────────────────────────────────────────── */
.status-badge {
  display: inline-block; font-size: 11.5px; font-weight: 500;
  padding: 3px 10px; border-radius: 99px;
}
.status-active   { background: var(--color-green-dim); color: var(--color-green-light); }
.status-disabled { background: var(--color-error-bg, #2d1a1a); color: var(--color-error, #e07070); }

/* ── Alert ───────────────────────────────────────────────────── */
.alert       { display: flex; align-items: center; gap: 8px; padding: 12px 16px; border-radius: var(--radius-sm); font-size: 13.5px; }
.alert-error { background: var(--color-error-bg); color: var(--color-error); border: 1px solid var(--color-error-border); }

/* ── Loading / empty ─────────────────────────────────────────── */
.loading-state {
  display: flex; flex-direction: column; align-items: center;
  gap: 12px; padding: 60px; color: var(--color-muted); font-size: 14px;
}
.spinner-muted {
  width: 24px; height: 24px;
  border: 2px solid var(--color-border); border-top-color: var(--color-green);
  border-radius: 50%; animation: spin 0.7s linear infinite;
}
.spinner-sm {
  width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.3); border-top-color: currentColor;
  border-radius: 50%; animation: spin 0.7s linear infinite; flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }

.empty-state {
  display: flex; flex-direction: column; align-items: center;
  gap: 10px; padding: 60px;
  background: var(--color-bg-2); border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg); text-align: center; color: var(--color-muted);
}
.empty-icon { font-size: 48px; }
.empty-state h3 { font-family: 'Barlow Condensed', sans-serif; font-size: 20px; color: var(--color-text-dim); }
.empty-state p  { font-size: 13.5px; }

/* ── Responsive ──────────────────────────────────────────────── */
@media (max-width: 768px) {
  .page-title { font-size: 22px; }
  /* Search fills the row; result count drops beneath it */
  .table-toolbar { flex-wrap: wrap; }
  .search-input  { flex-basis: 100%; }
  /* 8 columns stay readable and scroll horizontally instead of crushing */
  .users-table { min-width: 720px; }
  .table-wrap  { -webkit-overflow-scrolling: touch; }
}
@media (max-width: 700px) { .stats-row { grid-template-columns: 1fr 1fr; } }
</style>
