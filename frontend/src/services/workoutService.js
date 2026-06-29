/**
 * workoutService.js — API calls for UC004/UC005
 *
 * POST   /api/workout/generate              → generate a new AI workout plan
 * GET    /api/workout/plans                 → list saved plans
 * GET    /api/workout/plans/:id             → get a single plan
 * PUT    /api/workout/plans/:id             → edit plan name / content
 * PUT    /api/workout/plans/:id/activate    → set plan as currently active
 * PATCH  /api/workout/plans/:id/progress    → update completed checklist items
 * DELETE /api/workout/plans/:id             → delete a plan
 */
import http from './http'

const workoutService = {
  async generatePlan(payload) {
    // AI generation can take 30–90 seconds — override the default 10s timeout
    const { data } = await http.post('/workout/generate', payload, { timeout: 120000 })
    return data
  },

  async getPlans() {
    const { data } = await http.get('/workout/plans')
    return data
  },

  async getPlan(id) {
    const { data } = await http.get(`/workout/plans/${id}`)
    return data
  },

  async editPlan(id, payload) {
    const { data } = await http.put(`/workout/plans/${id}`, payload)
    return data
  },

  async activatePlan(id) {
    const { data } = await http.put(`/workout/plans/${id}/activate`)
    return data
  },

  async updateProgress(id, completedItems) {
    const { data } = await http.patch(`/workout/plans/${id}/progress`, { completedItems })
    return data
  },

  async deletePlan(id) {
    const { data } = await http.delete(`/workout/plans/${id}`)
    return data
  },

  async copyPlan(id, newName) {
    const { data } = await http.post(`/workout/plans/${id}/copy`, { newName })
    return data
  }
}

export default workoutService
