/**
 * mealService.js — API calls for UC006/UC007
 *
 * POST   /api/meal/generate             → generate a new AI 7-day meal plan
 * GET    /api/meal/plans                → list saved plans
 * GET    /api/meal/plans/:id            → get a single plan
 * PUT    /api/meal/plans/:id            → edit plan name / content
 * PUT    /api/meal/plans/:id/activate   → set plan as currently active
 * PATCH  /api/meal/plans/:id/progress   → update completed checklist items
 * DELETE /api/meal/plans/:id            → delete a plan
 */
import http from './http'

const mealService = {
  async generatePlan(payload) {
    // 7-day meal generation can take a while — use extended timeout
    const { data } = await http.post('/meal/generate', payload, { timeout: 250000 })
    return data
  },

  async getPlans() {
    const { data } = await http.get('/meal/plans')
    return data
  },

  async getPlan(id) {
    const { data } = await http.get(`/meal/plans/${id}`)
    return data
  },

  async editPlan(id, payload) {
    const { data } = await http.put(`/meal/plans/${id}`, payload)
    return data
  },

  async activatePlan(id) {
    const { data } = await http.put(`/meal/plans/${id}/activate`)
    return data
  },

  async updateProgress(id, completedItems) {
    const { data } = await http.patch(`/meal/plans/${id}/progress`, { completedItems })
    return data
  },

  async deletePlan(id) {
    const { data } = await http.delete(`/meal/plans/${id}`)
    return data
  },

  async copyPlan(id, newName) {
    const { data } = await http.post(`/meal/plans/${id}/copy`, { newName })
    return data
  }
}

export default mealService
