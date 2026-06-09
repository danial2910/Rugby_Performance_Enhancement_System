/**
 * appointment.js — Pinia store for UC008/UC009/UC012
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import appointmentService from '@/services/appointmentService'

export const useAppointmentStore = defineStore('appointment', () => {

  // ── Shared state ──────────────────────────────────────────────────────────
  const trainers        = ref([])   // available trainers for booking form
  const appointments    = ref([])   // athlete's own appointments / trainer's appointments
  const loading         = ref(false)
  const loadingTrainers = ref(false)
  const saving          = ref(false)
  const error           = ref(null)
  const successMsg      = ref(null)

  // ── Trainer list (booking form) ───────────────────────────────────────────

  async function fetchTrainers() {
    loadingTrainers.value = true
    error.value = null
    try {
      const response = await appointmentService.getAvailableTrainers()
      if (response.success) trainers.value = response.data
    } catch {
      error.value = 'Failed to load trainers.'
    } finally {
      loadingTrainers.value = false
    }
  }

  // ── UC008: Create appointment ─────────────────────────────────────────────

  async function createAppointment(payload) {
    saving.value      = true
    error.value       = null
    successMsg.value  = null
    try {
      const response = await appointmentService.createAppointment(payload)
      if (response.success) {
        appointments.value = [response.data, ...appointments.value]
        successMsg.value   = 'Appointment booked successfully!'
        return response.data
      }
      error.value = response.message || 'Failed to book appointment.'
      return null
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to book appointment. Please try again.'
      return null
    } finally {
      saving.value = false
    }
  }

  // ── UC009: Athlete fetches own appointments ───────────────────────────────

  async function fetchAthleteAppointments() {
    loading.value = true
    error.value   = null
    try {
      const response = await appointmentService.getAthleteAppointments()
      if (response.success) appointments.value = response.data
    } catch {
      error.value = 'Failed to load appointments.'
    } finally {
      loading.value = false
    }
  }

  // ── UC009: Edit ───────────────────────────────────────────────────────────

  async function editAppointment(id, payload) {
    saving.value     = true
    error.value      = null
    successMsg.value = null
    try {
      const response = await appointmentService.editAppointment(id, payload)
      if (response.success) {
        const idx = appointments.value.findIndex(a => a.id === id)
        if (idx !== -1) appointments.value[idx] = response.data
        successMsg.value = 'Appointment updated successfully!'
        return response.data
      }
      error.value = response.message || 'Failed to update appointment.'
      return null
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to update appointment. Please try again.'
      return null
    } finally {
      saving.value = false
    }
  }

  // ── UC009: Cancel ─────────────────────────────────────────────────────────

  async function cancelAppointment(id) {
    error.value = null
    try {
      const response = await appointmentService.cancelAppointment(id)
      if (response.success) {
        const idx = appointments.value.findIndex(a => a.id === id)
        if (idx !== -1) appointments.value[idx] = response.data
        successMsg.value = 'Appointment cancelled.'
        return response.data
      }
      error.value = response.message || 'Failed to cancel.'
      return null
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to cancel. Please try again.'
      return null
    }
  }

  // ── UC012: Trainer fetches appointments ───────────────────────────────────

  async function fetchTrainerAppointments() {
    loading.value = true
    error.value   = null
    try {
      const response = await appointmentService.getTrainerAppointments()
      if (response.success) appointments.value = response.data
    } catch {
      error.value = 'Failed to load appointments.'
    } finally {
      loading.value = false
    }
  }

  // ── UC012: Approve ────────────────────────────────────────────────────────

  async function approveAppointment(id, remarks) {
    saving.value = true
    error.value  = null
    try {
      const response = await appointmentService.approveAppointment(id, remarks)
      if (response.success) {
        const idx = appointments.value.findIndex(a => a.id === id)
        if (idx !== -1) appointments.value[idx] = response.data
        successMsg.value = 'Appointment approved.'
        return response.data
      }
      error.value = response.message || 'Failed to approve.'
      return null
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to approve. Please try again.'
      return null
    } finally {
      saving.value = false
    }
  }

  // ── UC012: Reject ─────────────────────────────────────────────────────────

  async function rejectAppointment(id, remarks) {
    saving.value = true
    error.value  = null
    try {
      const response = await appointmentService.rejectAppointment(id, remarks)
      if (response.success) {
        const idx = appointments.value.findIndex(a => a.id === id)
        if (idx !== -1) appointments.value[idx] = response.data
        successMsg.value = 'Appointment rejected.'
        return response.data
      }
      error.value = response.message || 'Failed to reject.'
      return null
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to reject. Please try again.'
      return null
    } finally {
      saving.value = false
    }
  }

  // ── UC012: Add post-session feedback ─────────────────────────────────────

  async function addFeedback(id, feedbackText) {
    saving.value = true
    error.value  = null
    try {
      const response = await appointmentService.addFeedback(id, feedbackText)
      if (response.success) {
        const idx = appointments.value.findIndex(a => a.id === id)
        if (idx !== -1) appointments.value[idx] = response.data
        successMsg.value = 'Feedback saved.'
        return response.data
      }
      error.value = response.message || 'Failed to save feedback.'
      return null
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to save feedback. Please try again.'
      return null
    } finally {
      saving.value = false
    }
  }

  // ── Delete (athlete) ──────────────────────────────────────────────────────

  async function deleteAsAthlete(id) {
    error.value = null
    try {
      const response = await appointmentService.deleteAsAthlete(id)
      if (response.success) {
        appointments.value = appointments.value.filter(a => a.id !== id)
        successMsg.value = 'Appointment deleted.'
        return true
      }
      error.value = response.message || 'Failed to delete.'
      return false
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to delete. Please try again.'
      return false
    }
  }

  // ── Delete (trainer) ──────────────────────────────────────────────────────

  async function deleteAsTrainer(id) {
    error.value = null
    try {
      const response = await appointmentService.deleteAsTrainer(id)
      if (response.success) {
        appointments.value = appointments.value.filter(a => a.id !== id)
        successMsg.value = 'Appointment deleted.'
        return true
      }
      error.value = response.message || 'Failed to delete.'
      return false
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to delete. Please try again.'
      return false
    }
  }

  function clearError()      { error.value = null }
  function clearSuccess()    { successMsg.value = null }

  return {
    trainers, appointments,
    loading, loadingTrainers, saving, error, successMsg,
    fetchTrainers, createAppointment,
    fetchAthleteAppointments, editAppointment, cancelAppointment,
    deleteAsAthlete, deleteAsTrainer,
    fetchTrainerAppointments, approveAppointment, rejectAppointment,
    addFeedback, clearError, clearSuccess
  }
})
