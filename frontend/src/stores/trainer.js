/**
 * trainer.js — Pinia store for Trainer plan management
 *
 * State:
 *   athletes            — list of all athletes
 *   selectedAthlete     — currently selected athlete object
 *   workoutPlans        — workout plans for the selected athlete
 *   mealPlans           — meal plans for the selected athlete
 *   selectedPlan        — the plan currently open in the viewer
 *   planType            — 'workout' | 'meal' — which tab is active
 *   loading*            — individual loading flags
 *   error               — last error message
 *   saveSuccess         — true after a successful edit save
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import trainerService from '@/services/trainerService'

export const useTrainerStore = defineStore('trainer', () => {
  const athletes         = ref([])
  const selectedAthlete  = ref(null)
  const workoutPlans     = ref([])
  const mealPlans        = ref([])
  const selectedPlan     = ref(null)
  const planType         = ref('workout')   // 'workout' | 'meal'
  const loadingAthletes  = ref(false)
  const loadingPlans     = ref(false)
  const saving           = ref(false)
  const error            = ref(null)
  const saveSuccess      = ref(false)

  // ── Athletes ────────────────────────────────────────────────────────────────

  async function fetchAthletes() {
    loadingAthletes.value = true
    error.value = null
    try {
      const response = await trainerService.getAthletes()
      if (response.success) {
        athletes.value = response.data
      } else {
        error.value = response.message || 'Failed to load athletes.'
      }
    } catch {
      error.value = 'Failed to load athletes. Please try again.'
    } finally {
      loadingAthletes.value = false
    }
  }

  // ── Plans for a selected athlete ────────────────────────────────────────────

  async function selectAthlete(athlete) {
    selectedAthlete.value = athlete
    selectedPlan.value    = null
    workoutPlans.value    = []
    mealPlans.value       = []
    await fetchAthletePlans(athlete.id)
  }

  async function fetchAthletePlans(userId) {
    loadingPlans.value = true
    error.value = null
    try {
      const [workoutRes, mealRes] = await Promise.all([
        trainerService.getAthleteWorkoutPlans(userId),
        trainerService.getAthleteMealPlans(userId)
      ])
      if (workoutRes.success) workoutPlans.value = workoutRes.data
      if (mealRes.success)    mealPlans.value    = mealRes.data
    } catch {
      error.value = 'Failed to load athlete plans.'
    } finally {
      loadingPlans.value = false
    }
  }

  // ── Edit a plan ─────────────────────────────────────────────────────────────

  async function editWorkoutPlan(id, payload) {
    saving.value      = true
    saveSuccess.value = false
    error.value       = null
    try {
      const response = await trainerService.editWorkoutPlan(id, payload)
      if (response.success) {
        // Update the list entry
        const idx = workoutPlans.value.findIndex(p => p.id === id)
        if (idx !== -1) workoutPlans.value[idx] = response.data
        if (selectedPlan.value?.id === id) selectedPlan.value = response.data
        saveSuccess.value = true
        return response.data
      }
      error.value = response.message || 'Failed to save changes.'
      return null
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to save changes. Please try again.'
      return null
    } finally {
      saving.value = false
    }
  }

  async function editMealPlan(id, payload) {
    saving.value      = true
    saveSuccess.value = false
    error.value       = null
    try {
      const response = await trainerService.editMealPlan(id, payload)
      if (response.success) {
        const idx = mealPlans.value.findIndex(p => p.id === id)
        if (idx !== -1) mealPlans.value[idx] = response.data
        if (selectedPlan.value?.id === id) selectedPlan.value = response.data
        saveSuccess.value = true
        return response.data
      }
      error.value = response.message || 'Failed to save changes.'
      return null
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to save changes. Please try again.'
      return null
    } finally {
      saving.value = false
    }
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  function selectPlan(plan, type) {
    selectedPlan.value = plan
    planType.value     = type
  }

  function setPlanType(type) {
    planType.value    = type
    selectedPlan.value = null
  }

  // Back-navigation helpers (used by the mobile drill-down flow)
  function deselectPlan()    { selectedPlan.value = null }
  function deselectAthlete() { selectedAthlete.value = null; selectedPlan.value = null }

  function clearError()        { error.value = null }
  function clearSaveSuccess()  { saveSuccess.value = false }

  return {
    athletes, selectedAthlete, workoutPlans, mealPlans,
    selectedPlan, planType,
    loadingAthletes, loadingPlans, saving, error, saveSuccess,
    fetchAthletes, selectAthlete, fetchAthletePlans,
    editWorkoutPlan, editMealPlan,
    selectPlan, setPlanType, deselectPlan, deselectAthlete,
    clearError, clearSaveSuccess
  }
})
