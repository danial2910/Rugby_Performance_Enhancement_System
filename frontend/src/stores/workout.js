/**
 * workout.js — Pinia store for UC004/UC005: Workout Planner
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import workoutService from '@/services/workoutService'

export const useWorkoutStore = defineStore('workout', () => {
  const plans           = ref([])
  const activePlan      = ref(null)
  const generating      = ref(false)
  const loadingPlans    = ref(false)
  const error           = ref(null)
  const generateSuccess = ref(false)

  /** UC004: Generate a new workout plan via Gemini */
  async function generatePlan(payload) {
    generating.value      = true
    error.value           = null
    generateSuccess.value = false
    try {
      const response = await workoutService.generatePlan(payload)
      if (response.success) {
        activePlan.value      = response.data
        generateSuccess.value = true
        plans.value = [response.data, ...plans.value]
        return response.data
      }
      error.value = response.message || 'Failed to generate plan.'
      return null
    } catch (err) {
      if (err.response?.status === 503) {
        error.value = err.response.data?.message ||
          'AI engine is not available. Please try again shortly.'
      } else if (err.response?.status === 400) {
        error.value = err.response.data?.message || 'Please check your inputs.'
      } else if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
        error.value = 'Request timed out. AI generation is taking too long — try again or use a shorter prompt.'
      } else if (!err.response) {
        error.value = 'Connection error. Please check that the backend server is running.'
      } else {
        error.value = 'Failed to generate plan. Please try again.'
      }
      return null
    } finally {
      generating.value = false
    }
  }

  /** UC005: Load all saved plans */
  async function fetchPlans() {
    loadingPlans.value = true
    error.value        = null
    try {
      const response = await workoutService.getPlans()
      if (response.success) {
        plans.value = response.data
        // Sync activePlan reference to the active plan in the list (if any)
        const active = response.data.find(p => p.isActive)
        if (active) activePlan.value = active
      }
    } catch {
      error.value = 'Failed to load saved plans.'
    } finally {
      loadingPlans.value = false
    }
  }

  /** UC005: Edit plan name and/or content */
  async function editPlan(id, payload) {
    error.value = null
    try {
      const response = await workoutService.editPlan(id, payload)
      if (response.success) {
        const idx = plans.value.findIndex(p => p.id === id)
        if (idx !== -1) plans.value[idx] = response.data
        if (activePlan.value?.id === id) activePlan.value = response.data
        return response.data
      }
      error.value = response.message || 'Failed to update plan.'
      return null
    } catch {
      error.value = 'Failed to update plan. Please try again.'
      return null
    }
  }

  /** UC005: Set a plan as currently active (deactivates all others) */
  async function activatePlan(id) {
    error.value = null
    try {
      const response = await workoutService.activatePlan(id)
      if (response.success) {
        // Mark all plans inactive, then mark the activated one active
        plans.value = plans.value.map(p => ({ ...p, isActive: p.id === id }))
        activePlan.value = response.data
        return response.data
      }
      error.value = response.message || 'Failed to activate plan.'
      return null
    } catch {
      error.value = 'Failed to activate plan. Please try again.'
      return null
    }
  }

  /** UC005: Update completed checklist items */
  async function updateProgress(id, completedItems) {
    error.value = null
    try {
      const response = await workoutService.updateProgress(id, completedItems)
      if (response.success) {
        const idx = plans.value.findIndex(p => p.id === id)
        if (idx !== -1) plans.value[idx] = response.data
        if (activePlan.value?.id === id) activePlan.value = response.data
        return response.data
      }
      error.value = response.message || 'Failed to update progress.'
      return null
    } catch {
      error.value = 'Failed to save progress. Please try again.'
      return null
    }
  }

  /** UC004 AF2: Duplicate an existing plan with a new name */
  async function copyPlan(id, newName) {
    error.value = null
    try {
      const response = await workoutService.copyPlan(id, newName)
      if (response.success) {
        plans.value = [response.data, ...plans.value]
        return response.data
      }
      error.value = response.message || 'Failed to copy plan.'
      return null
    } catch {
      error.value = 'Failed to copy plan. Please try again.'
      return null
    }
  }

  /** UC005: Delete a plan */
  async function deletePlan(id) {
    error.value = null
    try {
      const response = await workoutService.deletePlan(id)
      if (response.success) {
        plans.value = plans.value.filter(p => p.id !== id)
        if (activePlan.value?.id === id) activePlan.value = null
      }
    } catch {
      error.value = 'Failed to delete plan. Please try again.'
    }
  }

  function setActivePlan(plan) { activePlan.value = plan }
  function clearError()        { error.value = null }

  return {
    plans, activePlan, generating, loadingPlans, error, generateSuccess,
    generatePlan, fetchPlans, editPlan, activatePlan, updateProgress,
    deletePlan, copyPlan, setActivePlan, clearError
  }
})
