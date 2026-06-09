/**
 * router/index.js — Vue Router
 *
 * Navigation Guards implement UC001 security:
 * - Protected routes require valid JWT (meta.requiresAuth)
 * - Unauthenticated users are redirected to Login
 * - Already-logged-in users are redirected away from Login
 * - Role-based guards: TRAINER routes require isTrainer
 *
 * Route structure:
 * /login          → LoginView        (public)
 * /register       → RegisterView     (public - UC002)
 * /dashboard      → DashboardView    (protected - ATHLETE)
 * /trainer          → redirect → /trainer/workouts
 * /trainer/workouts → TrainerWorkoutView (protected - TRAINER)
 * /trainer/meals    → TrainerMealView    (protected - TRAINER)
 * /meal-planner   → MealPlannerView  (protected)
 * /workout        → WorkoutView      (protected)
 * /chatbot        → ChatbotView      (protected)
 * /appointments   → AppointmentView  (protected)
 * /profile        → ProfileView      (protected)
 */
import { createRouter, createWebHistory } from 'vue-router'

// Lazy-loaded views for better performance
const LoginView       = () => import('@/views/auth/LoginView.vue')
const RegisterView    = () => import('@/views/auth/RegisterView.vue')
const DashboardView        = () => import('@/views/DashboardView.vue')
const TrainerWorkoutView   = () => import('@/views/TrainerWorkoutView.vue')
const TrainerMealView      = () => import('@/views/TrainerMealView.vue')
const MealPlannerView = () => import('@/views/MealPlannerView.vue')
const WorkoutView     = () => import('@/views/WorkoutView.vue')
const ChatbotView     = () => import('@/views/ChatbotView.vue')
const AppointmentView = () => import('@/views/AppointmentView.vue')
const ProfileView     = () => import('@/views/ProfileView.vue')
const AdminView       = () => import('@/views/AdminView.vue')
const NotFoundView    = () => import('@/views/NotFoundView.vue')

// Layout wrappers
const AuthLayout = () => import('@/layouts/AuthLayout.vue')
const AppLayout  = () => import('@/layouts/AppLayout.vue')

const routes = [
  // ── Auth Layout (no sidebar) ──────────────────────────────────────────
  {
    path: '/',
    component: AuthLayout,
    children: [
      {
        path: '',
        redirect: '/login'
      },
      {
        path: 'login',
        name: 'Login',
        component: LoginView,
        meta: {
          requiresAuth: false,
          title: 'Login — Rugby Performance Enhancement System'
        }
      },
      {
        path: 'register',
        name: 'Register',
        component: RegisterView,
        meta: {
          requiresAuth: false,
          title: 'Register — Rugby Performance Enhancement System'
        }
      }
    ]
  },

  // ── App Layout (with sidebar + header) ────────────────────────────────
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: DashboardView,
        meta: {
          requiresAuth: true,
          role: 'ATHLETE',
          title: 'Dashboard — Rugby Performance Enhancement System'
        }
      },
      {
        path: 'trainer',
        redirect: '/trainer/workouts'
      },
      {
        path: 'trainer/workouts',
        name: 'TrainerWorkouts',
        component: TrainerWorkoutView,
        meta: {
          requiresAuth: true,
          role: 'TRAINER',
          title: 'Workout Plan Management — Rugby Performance Enhancement System'
        }
      },
      {
        path: 'trainer/meals',
        name: 'TrainerMeals',
        component: TrainerMealView,
        meta: {
          requiresAuth: true,
          role: 'TRAINER',
          title: 'Meal Plan Management — Rugby Performance Enhancement System'
        }
      },
      {
        path: 'meal-planner',
        name: 'MealPlanner',
        component: MealPlannerView,
        meta: {
          requiresAuth: true,
          title: 'Meal Planner — Rugby Performance Enhancement System'
        }
      },
      {
        path: 'workout',
        name: 'Workout',
        component: WorkoutView,
        meta: {
          requiresAuth: true,
          title: 'Workout Planner — Rugby Performance Enhancement System'
        }
      },
      {
        path: 'chatbot',
        name: 'Chatbot',
        component: ChatbotView,
        meta: {
          requiresAuth: true,
          title: 'AI Chatbot — Rugby Performance Enhancement System'
        }
      },
      {
        path: 'appointments',
        name: 'Appointments',
        component: AppointmentView,
        meta: {
          requiresAuth: true,
          title: 'Appointments — Rugby Performance Enhancement System'
        }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: ProfileView,
        meta: {
          requiresAuth: true,
          title: 'My Profile — Rugby Performance Enhancement System'
        }
      },
      {
        path: 'admin',
        name: 'Admin',
        component: AdminView,
        meta: {
          requiresAuth: true,
          role: 'ADMIN',
          title: 'User Management — Rugby Performance Enhancement System'
        }
      }
    ]
  },

  // ── 404 ───────────────────────────────────────────────────────────────
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// ─── Navigation Guard ──────────────────────────────────────────────────────
router.beforeEach((to, from, next) => {
  // Update page title
  document.title = to.meta.title || 'Rugby Performance Enhancement System — UTM Pirates'

  const token    = localStorage.getItem('access_token')
  const userRaw  = localStorage.getItem('user')
  const userRole = userRaw ? JSON.parse(userRaw)?.userRole : null

  const isLoggedIn = !!token

  // UC001 Guard: protected route without auth → redirect to login
  if (to.meta.requiresAuth && !isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // Already logged in → don't show login/register page
  if (!to.meta.requiresAuth && isLoggedIn && (to.name === 'Login' || to.name === 'Register')) {
    const destination = userRole === 'ADMIN' ? '/admin'
                      : userRole === 'TRAINER' ? '/trainer/workouts'
                      : '/dashboard'
    return next(destination)
  }

  // Role-based guard: ADMIN-only routes
  if (to.meta.role === 'ADMIN' && userRole !== 'ADMIN') {
    const fallback = userRole === 'TRAINER' ? '/trainer/workouts' : '/dashboard'
    return next(fallback)
  }

  // Role-based guard: TRAINER-only routes
  if (to.meta.role === 'TRAINER' && userRole !== 'TRAINER') {
    return next(userRole === 'ADMIN' ? '/admin' : '/dashboard')
  }

  // Role-based guard: ATHLETE-only routes
  if (to.meta.role === 'ATHLETE' && userRole !== 'ATHLETE') {
    return next(userRole === 'ADMIN' ? '/admin' : '/trainer/workouts')
  }

  next()
})

export default router
