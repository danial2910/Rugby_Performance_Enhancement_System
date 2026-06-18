<template>
  <div class="appt-page">

    <!-- ══════════════════════════════════════════════════════════
         ATHLETE VIEW  (UC008 + UC009)
         ══════════════════════════════════════════════════════════ -->
    <template v-if="authStore.isAthlete">

      <div class="page-header">
        <div>
          <h1 class="page-title">Appointments</h1>
          <p class="page-subtitle">Book a session with a trainer and track your appointments</p>
        </div>
        <button class="btn btn-primary" @click="showBookingForm = !showBookingForm">
          {{ showBookingForm ? '✕ Close' : '+ Book Appointment' }}
        </button>
      </div>

      <!-- ── Booking Form ──────────────────────────────────────── -->
      <transition name="slide-down">
        <div v-if="showBookingForm" class="card booking-card">
          <div class="card-header">
            <span class="card-icon">📅</span>
            <h2 class="card-title">New Appointment</h2>
          </div>

          <transition name="fade">
            <div v-if="apptStore.successMsg" class="alert alert-success">
              ✅ {{ apptStore.successMsg }}
              <button class="alert-close" @click="apptStore.clearSuccess()">×</button>
            </div>
          </transition>
          <transition name="fade">
            <div v-if="apptStore.error" class="alert alert-error">
              ✕ {{ apptStore.error }}
              <button class="alert-close" @click="apptStore.clearError()">×</button>
            </div>
          </transition>

          <form @submit.prevent="handleBook" class="booking-form">

            <!-- Service + Location -->
            <div class="form-row">
              <div class="form-group" :class="{ 'has-error': errors.serviceType }">
                <label class="form-label">Service Type <span class="req">*</span></label>
                <select v-model="form.serviceType" class="form-select" @change="clearErr('serviceType')">
                  <option value="" disabled>Select service</option>
                  <option value="FITNESS_TRAINING">💪 Fitness Training</option>
                  <option value="NUTRITION_COUNSELLING">🥗 Nutrition Counselling</option>
                  <option value="WELLNESS_COACHING">🧘 Wellness Coaching</option>
                </select>
                <p v-if="errors.serviceType" class="field-error">{{ errors.serviceType }}</p>
              </div>

              <div class="form-group" :class="{ 'has-error': errors.location }">
                <label class="form-label">Location <span class="req">*</span></label>
                <select v-model="form.location" class="form-select" @change="clearErr('location')">
                  <option value="" disabled>Select location</option>
                  <option value="GYM">🏋️ Gym</option>
                  <option value="ONLINE">💻 Online</option>
                </select>
                <p v-if="errors.location" class="field-error">{{ errors.location }}</p>
              </div>
            </div>

            <!-- Trainer selection -->
            <div class="form-group" :class="{ 'has-error': errors.trainerId }">
              <label class="form-label">Select Trainer <span class="req">*</span></label>

              <div v-if="apptStore.loadingTrainers" class="load-row">
                <div class="spinner"></div><span>Loading trainers…</span>
              </div>
              <div v-else-if="!apptStore.trainers.length" class="load-row muted">
                No trainers available at the moment.
              </div>

              <div v-else class="trainer-grid">
                <button
                  v-for="trainer in apptStore.trainers"
                  :key="trainer.userId"
                  type="button"
                  class="trainer-card"
                  :class="{ selected: form.trainerId === trainer.userId }"
                  @click="selectTrainer(trainer)"
                >
                  <div class="trainer-avatar">
                    <img v-if="trainer.profilePicture" :src="trainer.profilePicture" alt="" />
                    <span v-else>{{ initials(trainer.fullName) }}</span>
                  </div>
                  <div class="trainer-info">
                    <div class="trainer-name">{{ trainer.fullName }}</div>
                    <div class="trainer-exp" v-if="trainer.expertise">{{ trainer.expertise }}</div>
                    <div class="trainer-avail" v-if="trainer.availabilitySlots?.length">
                      <span class="avail-label">Available:</span>
                      <span
                        v-for="slot in trainer.availabilitySlots.slice(0, 3)"
                        :key="slot.day"
                        class="avail-chip"
                      >{{ slot.day.slice(0,3) }} {{ slot.startTime }}–{{ slot.endTime }}</span>
                      <span v-if="trainer.availabilitySlots.length > 3" class="muted" style="font-size:10px">
                        +{{ trainer.availabilitySlots.length - 3 }} more
                      </span>
                    </div>
                  </div>
                  <span v-if="form.trainerId === trainer.userId" class="trainer-tick">✓</span>
                </button>
              </div>
              <p v-if="errors.trainerId" class="field-error">{{ errors.trainerId }}</p>
            </div>

            <!-- Trainer availability hint -->
            <div v-if="selectedTrainerSlots.length" class="avail-hint">
              <span class="avail-hint-icon">📅</span>
              <div class="avail-hint-slots">
                <span class="avail-hint-label">Available slots:</span>
                <span v-for="slot in selectedTrainerSlots" :key="slot.day" class="avail-hint-chip">
                  {{ slot.day.slice(0,3) }} {{ slot.startTime }}–{{ slot.endTime }}
                </span>
              </div>
            </div>

            <!-- Date + Time -->
            <div class="form-row">
              <div class="form-group" :class="{ 'has-error': errors.date }">
                <label class="form-label">Date <span class="req">*</span></label>
                <input
                  v-model="form.date"
                  type="date"
                  class="form-input"
                  :min="today"
                  @change="onDateChange"
                />
                <p v-if="errors.date" class="field-error">{{ errors.date }}</p>
              </div>
              <div class="form-group" :class="{ 'has-error': errors.time }">
                <label class="form-label">Time <span class="req">*</span></label>
                <!-- Slot selector when trainer+date selected and slots available -->
                <select
                  v-if="availableTimeSlots.length"
                  v-model="form.time"
                  class="form-select"
                  @change="clearErr('time')"
                >
                  <option value="" disabled>Select a time slot</option>
                  <option v-for="t in availableTimeSlots" :key="t" :value="t">{{ t }}</option>
                </select>
                <!-- Fallback free input when no slots computed -->
                <input
                  v-else
                  v-model="form.time"
                  type="time"
                  class="form-input"
                  @change="clearErr('time')"
                />
                <p v-if="errors.time" class="field-error">{{ errors.time }}</p>
              </div>
            </div>

            <!-- Duration -->
            <div class="form-group" :class="{ 'has-error': errors.duration }">
              <label class="form-label">Duration <span class="req">*</span></label>
              <div class="duration-row">
                <button
                  v-for="mins in [30, 45, 60, 90]"
                  :key="mins"
                  type="button"
                  class="dur-btn"
                  :class="{ active: form.duration === mins }"
                  @click="form.duration = mins; clearErr('duration')"
                >{{ mins }} min</button>
              </div>
              <p v-if="errors.duration" class="field-error">{{ errors.duration }}</p>
            </div>

            <!-- Purpose -->
            <div class="form-group" :class="{ 'has-error': errors.purpose }">
              <label class="form-label">Purpose <span class="req">*</span></label>
              <textarea
                v-model="form.purpose"
                class="form-textarea"
                rows="3"
                placeholder="e.g. Improve sprint speed and tackle technique for the upcoming match"
                @input="clearErr('purpose')"
              ></textarea>
              <p v-if="errors.purpose" class="field-error">{{ errors.purpose }}</p>
            </div>

            <!-- Special requirements -->
            <div class="form-group">
              <label class="form-label">
                Special Requirements <span class="optional-tag">optional</span>
              </label>
              <textarea
                v-model="form.specialRequirements"
                class="form-textarea"
                rows="2"
                placeholder="e.g. Knee injury — avoid high-impact exercises"
              ></textarea>
            </div>

            <button type="submit" class="btn btn-primary btn-full" :disabled="apptStore.saving">
              <span v-if="apptStore.saving" class="btn-spinner"></span>
              {{ apptStore.saving ? 'Booking…' : '📅 Confirm Booking' }}
            </button>
          </form>
        </div>
      </transition>

      <!-- ── My Appointments List ──────────────────────────────── -->
      <div class="card">
        <div class="card-header">
          <span class="card-icon">📋</span>
          <h2 class="card-title">My Appointments</h2>
          <div class="filter-row">
            <button
              v-for="f in statusFilters"
              :key="f.value"
              class="filter-btn"
              :class="{ active: statusFilter === f.value }"
              @click="statusFilter = f.value"
            >{{ f.label }}</button>
          </div>
        </div>

        <div v-if="apptStore.loading" class="load-row center">
          <div class="spinner"></div><span>Loading…</span>
        </div>
        <div v-else-if="filteredAppointments.length === 0" class="empty-state">
          <span>📭</span>
          <p>No {{ statusFilter !== 'ALL' ? statusFilter.toLowerCase() : '' }} appointments found.</p>
        </div>

        <div v-else class="appt-list">
          <div
            v-for="appt in filteredAppointments"
            :key="appt.id"
            class="appt-item"
          >
            <div class="appt-dot" :class="`dot-${appt.status.toLowerCase()}`"></div>
            <div class="appt-body">
              <div class="appt-top">
                <span class="appt-service">{{ serviceLabel(appt.serviceType) }}</span>
                <span class="appt-badge" :class="`badge-${appt.status.toLowerCase()}`">{{ appt.status }}</span>
              </div>
              <div class="appt-sub">with <strong>{{ appt.trainerName }}</strong></div>
              <div class="appt-meta">
                <span>📅 {{ formatDate(appt.date) }}</span>
                <span>🕐 {{ appt.time }}</span>
                <span>⏱ {{ appt.duration }} min</span>
                <span>📍 {{ locationLabel(appt.location) }}</span>
              </div>
              <div class="appt-purpose">{{ appt.purpose }}</div>
              <div v-if="appt.trainerRemarks" class="appt-remarks">
                <strong>Trainer remarks:</strong> {{ appt.trainerRemarks }}
              </div>
              <div v-if="appt.trainerFeedback" class="appt-feedback">
                <strong>📋 Session feedback:</strong> {{ appt.trainerFeedback }}
              </div>
            </div>
            <div class="appt-actions">
              <template v-if="appt.status === 'PENDING'">
                <button class="btn btn-sm btn-edit" @click="openEdit(appt)">✏️ Edit</button>
                <button class="btn btn-sm btn-ghost" @click="handleCancel(appt.id)">Cancel</button>
              </template>
              <button class="btn btn-sm btn-delete" @click="confirmDeleteAthlete(appt)" title="Remove from history">🗑️ Delete</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Edit Appointment Modal ─────────────────────────────────────── -->
      <transition name="fade">
        <div v-if="editingAppt" class="modal-overlay" @click.self="closeEdit">
          <div class="modal-card">
            <div class="modal-header">
              <h2 class="modal-title">✏️ Edit Appointment</h2>
              <button class="modal-close" @click="closeEdit">×</button>
            </div>

            <transition name="fade">
              <div v-if="apptStore.error" class="alert alert-error">
                ✕ {{ apptStore.error }}
                <button class="alert-close" @click="apptStore.clearError()">×</button>
              </div>
            </transition>

            <form @submit.prevent="handleEdit" class="booking-form">

              <!-- Service + Location -->
              <div class="form-row">
                <div class="form-group" :class="{ 'has-error': editErrors.serviceType }">
                  <label class="form-label">Service Type <span class="req">*</span></label>
                  <select v-model="editForm.serviceType" class="form-select" @change="clearEditErr('serviceType')">
                    <option value="" disabled>Select service</option>
                    <option value="FITNESS_TRAINING">💪 Fitness Training</option>
                    <option value="NUTRITION_COUNSELLING">🥗 Nutrition Counselling</option>
                    <option value="WELLNESS_COACHING">🧘 Wellness Coaching</option>
                  </select>
                  <p v-if="editErrors.serviceType" class="field-error">{{ editErrors.serviceType }}</p>
                </div>
                <div class="form-group" :class="{ 'has-error': editErrors.location }">
                  <label class="form-label">Location <span class="req">*</span></label>
                  <select v-model="editForm.location" class="form-select" @change="clearEditErr('location')">
                    <option value="" disabled>Select location</option>
                    <option value="GYM">🏋️ Gym</option>
                    <option value="ONLINE">💻 Online</option>
                  </select>
                  <p v-if="editErrors.location" class="field-error">{{ editErrors.location }}</p>
                </div>
              </div>

              <!-- Trainer -->
              <div class="form-group" :class="{ 'has-error': editErrors.trainerId }">
                <label class="form-label">Trainer <span class="req">*</span></label>
                <div class="trainer-grid">
                  <button
                    v-for="trainer in apptStore.trainers"
                    :key="trainer.userId"
                    type="button"
                    class="trainer-card"
                    :class="{ selected: editForm.trainerId === trainer.userId }"
                    @click="editForm.trainerId = trainer.userId; clearEditErr('trainerId')"
                  >
                    <div class="trainer-avatar">
                      <img v-if="trainer.profilePicture" :src="trainer.profilePicture" alt="" />
                      <span v-else>{{ initials(trainer.fullName) }}</span>
                    </div>
                    <div class="trainer-info">
                      <div class="trainer-name">{{ trainer.fullName }}</div>
                      <div class="trainer-exp" v-if="trainer.expertise">{{ trainer.expertise }}</div>
                    </div>
                    <span v-if="editForm.trainerId === trainer.userId" class="trainer-tick">✓</span>
                  </button>
                </div>
                <p v-if="editErrors.trainerId" class="field-error">{{ editErrors.trainerId }}</p>
              </div>

              <!-- Date + Time -->
              <div class="form-row">
                <div class="form-group" :class="{ 'has-error': editErrors.date }">
                  <label class="form-label">Date <span class="req">*</span></label>
                  <input v-model="editForm.date" type="date" class="form-input" :min="today" @change="clearEditErr('date')" />
                  <p v-if="editErrors.date" class="field-error">{{ editErrors.date }}</p>
                </div>
                <div class="form-group" :class="{ 'has-error': editErrors.time }">
                  <label class="form-label">Time <span class="req">*</span></label>
                  <input v-model="editForm.time" type="time" class="form-input" @change="clearEditErr('time')" />
                  <p v-if="editErrors.time" class="field-error">{{ editErrors.time }}</p>
                </div>
              </div>

              <!-- Duration -->
              <div class="form-group" :class="{ 'has-error': editErrors.duration }">
                <label class="form-label">Duration <span class="req">*</span></label>
                <div class="duration-row">
                  <button
                    v-for="mins in [30, 45, 60, 90]"
                    :key="mins"
                    type="button"
                    class="dur-btn"
                    :class="{ active: editForm.duration === mins }"
                    @click="editForm.duration = mins; clearEditErr('duration')"
                  >{{ mins }} min</button>
                </div>
                <p v-if="editErrors.duration" class="field-error">{{ editErrors.duration }}</p>
              </div>

              <!-- Purpose -->
              <div class="form-group" :class="{ 'has-error': editErrors.purpose }">
                <label class="form-label">Purpose <span class="req">*</span></label>
                <textarea v-model="editForm.purpose" class="form-textarea" rows="3" @input="clearEditErr('purpose')"></textarea>
                <p v-if="editErrors.purpose" class="field-error">{{ editErrors.purpose }}</p>
              </div>

              <!-- Special Requirements -->
              <div class="form-group">
                <label class="form-label">Special Requirements <span class="optional-tag">optional</span></label>
                <textarea v-model="editForm.specialRequirements" class="form-textarea" rows="2"></textarea>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn btn-ghost" @click="closeEdit">Discard</button>
                <button type="submit" class="btn btn-primary" :disabled="apptStore.saving">
                  <span v-if="apptStore.saving" class="btn-spinner"></span>
                  {{ apptStore.saving ? 'Saving…' : 'Save Changes' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </transition>

    </template>

    <!-- ══════════════════════════════════════════════════════════
         TRAINER VIEW  (UC012 — Approve / Reject)
         ══════════════════════════════════════════════════════════ -->
    <template v-else-if="authStore.isTrainer">

      <div class="page-header">
        <div>
          <h1 class="page-title">Appointment Requests</h1>
          <p class="page-subtitle">Review and respond to athlete booking requests</p>
        </div>
        <span v-if="pendingCount > 0" class="pending-badge">{{ pendingCount }} pending</span>
      </div>

      <div class="tab-row">
        <button
          v-for="f in statusFilters"
          :key="f.value"
          class="tab-btn"
          :class="{ active: statusFilter === f.value }"
          @click="statusFilter = f.value"
        >
          {{ f.label }}
          <span v-if="f.value !== 'ALL'" class="tab-cnt">
            {{ apptStore.appointments.filter(a => a.status === f.value).length }}
          </span>
        </button>
      </div>

      <transition name="fade">
        <div v-if="apptStore.successMsg" class="alert alert-success">
          ✅ {{ apptStore.successMsg }}
          <button class="alert-close" @click="apptStore.clearSuccess()">×</button>
        </div>
      </transition>
      <transition name="fade">
        <div v-if="apptStore.error" class="alert alert-error">
          ✕ {{ apptStore.error }}
          <button class="alert-close" @click="apptStore.clearError()">×</button>
        </div>
      </transition>

      <div v-if="apptStore.loading" class="load-row center">
        <div class="spinner"></div><span>Loading…</span>
      </div>
      <div v-else-if="filteredAppointments.length === 0" class="empty-state card">
        <span>📭</span>
        <p>No {{ statusFilter !== 'ALL' ? statusFilter.toLowerCase() : '' }} appointments.</p>
      </div>

      <div v-else class="trainer-appt-list">
        <div
          v-for="appt in filteredAppointments"
          :key="appt.id"
          class="tl-item"
          :class="`tl-${appt.status.toLowerCase()}`"
        >
          <!-- ── Main row ────────────────────────────────────────── -->
          <div class="tl-main">
            <!-- Athlete -->
            <div class="tl-athlete">
              <div class="t-avatar">{{ initials(appt.athleteName) }}</div>
              <div class="tl-athlete-info">
                <div class="tl-name">{{ appt.athleteName }}</div>
                <div class="tl-service">{{ serviceLabel(appt.serviceType) }}</div>
              </div>
            </div>

            <!-- Date / Time -->
            <div class="tl-col tl-datetime">
              <div class="tl-col-main">{{ formatDate(appt.date) }}</div>
              <div class="tl-col-sub">🕐 {{ appt.time }}</div>
            </div>

            <!-- Duration / Location -->
            <div class="tl-col tl-dur">
              <div class="tl-col-main">⏱ {{ appt.duration }} min</div>
              <div class="tl-col-sub">{{ locationLabel(appt.location) }}</div>
            </div>

            <!-- Purpose -->
            <div class="tl-col tl-purpose">
              <div class="tl-purpose-text">{{ appt.purpose }}</div>
              <div v-if="appt.specialRequirements" class="tl-special">⚠️ {{ appt.specialRequirements }}</div>
            </div>

            <!-- Status + delete -->
            <div class="tl-right">
              <span class="appt-badge" :class="`badge-${appt.status.toLowerCase()}`">{{ appt.status }}</span>
              <button class="btn-delete-sm" @click="confirmDeleteTrainer(appt)" title="Remove from history">
                🗑️ Delete
              </button>
            </div>
          </div>

          <!-- ── Pending: remarks + approve/reject ──────────────── -->
          <div v-if="appt.status === 'PENDING'" class="tl-expand tl-pending-actions">
            <textarea
              v-model="remarksMap[appt.id]"
              class="form-textarea tl-remarks-input"
              rows="2"
              placeholder="Optional remarks (e.g. 'See you at Field 2' or reason for rejection)"
            ></textarea>
            <div class="tl-action-btns">
              <button class="btn btn-approve" :disabled="apptStore.saving" @click="handleApprove(appt.id)">
                ✅ Approve
              </button>
              <button class="btn btn-reject" :disabled="apptStore.saving" @click="handleReject(appt.id)">
                ✕ Reject
              </button>
            </div>
          </div>

          <!-- ── Non-pending: existing remarks ─────────────────── -->
          <div v-else-if="appt.trainerRemarks" class="tl-expand tl-remarks-display">
            <strong>Your remarks:</strong> {{ appt.trainerRemarks }}
          </div>

          <!-- ── Approved: feedback section ─────────────────────── -->
          <div v-if="appt.status === 'APPROVED'" class="tl-expand tl-feedback-section">
            <div v-if="appt.trainerFeedback && feedbackEditId !== appt.id" class="t-feedback-display">
              <div class="t-feedback-header">
                <span>📋 Session Feedback</span>
                <button class="btn-link" @click="openFeedbackEdit(appt)">Edit</button>
              </div>
              <p class="t-feedback-text">{{ appt.trainerFeedback }}</p>
            </div>
            <div v-else class="t-feedback-form">
              <label class="t-feedback-label">
                {{ appt.trainerFeedback ? '✏️ Edit Feedback' : '📋 Add Session Feedback' }}
              </label>
              <textarea
                v-model="feedbackMap[appt.id]"
                class="form-textarea"
                rows="3"
                placeholder="e.g. Great effort today. Focus on form for deadlifts next session. Recommend 2 rest days before next training."
              ></textarea>
              <div class="t-feedback-btns">
                <button
                  class="btn btn-feedback-save"
                  :disabled="apptStore.saving || !feedbackMap[appt.id]?.trim()"
                  @click="handleSaveFeedback(appt.id)"
                >
                  <span v-if="apptStore.saving" class="btn-spinner"></span>
                  💾 Save Feedback
                </button>
                <button
                  v-if="appt.trainerFeedback"
                  class="btn btn-ghost"
                  @click="feedbackEditId = null"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

    </template>

    <!-- ── Delete confirmation modal (shared) ───────────────────────────── -->
    <transition name="fade">
      <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
        <div class="modal-card modal-small">
          <div class="modal-header">
            <h2 class="modal-title">🗑️ Delete Appointment</h2>
            <button class="modal-close" @click="deleteTarget = null">×</button>
          </div>
          <div class="modal-body">
            <p>Permanently remove this appointment from your records?</p>
            <div class="delete-preview">
              <span class="appt-badge" :class="`badge-${deleteTarget.status.toLowerCase()}`">{{ deleteTarget.status }}</span>
              <span>{{ formatDate(deleteTarget.date) }} · {{ deleteTarget.time }}</span>
              <span>{{ deleteTarget.status === 'PENDING' || authStore.isAthlete ? deleteTarget.trainerName : deleteTarget.athleteName }}</span>
            </div>
            <p class="delete-warning">This cannot be undone.</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="deleteTarget = null">Cancel</button>
            <button class="btn btn-danger" :disabled="apptStore.saving" @click="handleDelete">
              <span v-if="apptStore.saving" class="btn-spinner"></span>
              Delete
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useAuthStore }        from '@/stores/auth'
import { useAppointmentStore } from '@/stores/appointment'

const authStore = useAuthStore()
const apptStore = useAppointmentStore()

// ── State ─────────────────────────────────────────────────────────────────────

const showBookingForm = ref(false)
const statusFilter    = ref('ALL')
const deleteTarget    = ref(null)   // appointment pending delete confirmation
const deleteRole      = ref('')     // 'athlete' | 'trainer'
const remarksMap      = reactive({})
const feedbackMap     = reactive({})
const feedbackEditId  = ref(null)

const form = ref({
  serviceType: '', trainerId: '', date: '', time: '',
  duration: null, location: '', purpose: '', specialRequirements: ''
})
const errors = ref({})

// ── Edit state ────────────────────────────────────────────────────────────────
const editingAppt = ref(null)   // the appointment being edited
const editForm    = ref({
  serviceType: '', trainerId: '', date: '', time: '',
  duration: null, location: '', purpose: '', specialRequirements: ''
})
const editErrors = ref({})

const today = computed(() => new Date().toISOString().split('T')[0])

// ── Trainer availability helpers ──────────────────────────────────────────────

/** Availability slots for the currently selected trainer */
const selectedTrainerSlots = computed(() => {
  if (!form.value.trainerId) return []
  const trainer = apptStore.trainers.find(t => t.userId === form.value.trainerId)
  return trainer?.availabilitySlots || []
})

/** Day-of-week string (e.g. "MONDAY") for the currently chosen date */
const selectedDateDay = computed(() => {
  if (!form.value.date) return ''
  const days = ['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY']
  return days[new Date(form.value.date + 'T00:00:00').getDay()]
})

/** Slot matching the selected date's weekday */
const slotForSelectedDay = computed(() => {
  if (!selectedDateDay.value || !selectedTrainerSlots.value.length) return null
  return selectedTrainerSlots.value.find(
    s => s.day?.toUpperCase().startsWith(selectedDateDay.value.slice(0, 3))
  ) || null
})

/**
 * 30-minute time increments within the trainer's slot window for the selected day.
 * e.g. slot 09:00–17:00 → ["09:00","09:30","10:00",…,"16:30"]
 */
const availableTimeSlots = computed(() => {
  const slot = slotForSelectedDay.value
  if (!slot) return []
  const slots = []
  const [sh, sm] = slot.startTime.split(':').map(Number)
  const [eh, em] = slot.endTime.split(':').map(Number)
  let cur = sh * 60 + sm
  const end = eh * 60 + em
  while (cur < end) {
    const h = String(Math.floor(cur / 60)).padStart(2, '0')
    const m = String(cur % 60).padStart(2, '0')
    slots.push(`${h}:${m}`)
    cur += 30
  }
  return slots
})

/** Reset time when trainer changes, validate day when date changes */
function onDateChange() {
  clearErr('date')
  form.value.time = ''
  if (form.value.trainerId && selectedDateDay.value && !slotForSelectedDay.value) {
    const available = selectedTrainerSlots.value.map(s =>
      s.day.charAt(0) + s.day.slice(1).toLowerCase()
    ).join(', ')
    errors.value.date = `Trainer not available on ${selectedDateDay.value.charAt(0) + selectedDateDay.value.slice(1).toLowerCase()}. Available: ${available}.`
  }
}

// Reset time slot when trainer selection changes
watch(() => form.value.trainerId, () => {
  form.value.date = ''
  form.value.time = ''
  delete errors.value.date
  delete errors.value.time
})

const statusFilters = [
  { value: 'ALL',       label: 'All' },
  { value: 'PENDING',   label: '⏳ Pending' },
  { value: 'APPROVED',  label: '✅ Approved' },
  { value: 'REJECTED',  label: '✕ Rejected' },
  { value: 'CANCELLED', label: '🚫 Cancelled' }
]

// ── Lifecycle ─────────────────────────────────────────────────────────────────

onMounted(async () => {
  if (authStore.isAthlete) {
    await Promise.all([
      apptStore.fetchTrainers(),
      apptStore.fetchAthleteAppointments()
    ])
  } else if (authStore.isTrainer) {
    await apptStore.fetchTrainerAppointments()
  }
})

// ── Computed ──────────────────────────────────────────────────────────────────

const filteredAppointments = computed(() => {
  const list = statusFilter.value === 'ALL'
    ? apptStore.appointments
    : apptStore.appointments.filter(a => a.status === statusFilter.value)

  return [...list].sort((a, b) => {
    const now = new Date()
    const dateA = new Date(a.date + 'T' + (a.time || '00:00'))
    const dateB = new Date(b.date + 'T' + (b.time || '00:00'))
    const aUpcoming = dateA >= now
    const bUpcoming = dateB >= now

    if (aUpcoming !== bUpcoming) return aUpcoming ? -1 : 1
    return aUpcoming ? dateA - dateB : dateB - dateA
  })
})

const pendingCount = computed(() =>
  apptStore.appointments.filter(a => a.status === 'PENDING').length
)

// ── Validate + Submit ─────────────────────────────────────────────────────────

function validate() {
  errors.value = {}
  if (!form.value.serviceType)      errors.value.serviceType = 'Please select a service type.'
  if (!form.value.trainerId)        errors.value.trainerId   = 'Please select a trainer.'
  if (!form.value.date)             errors.value.date        = 'Please select a date.'
  if (!form.value.time)             errors.value.time        = 'Please select a time.'
  if (!form.value.duration)         errors.value.duration    = 'Please select a duration.'
  if (!form.value.location)         errors.value.location    = 'Please select a location.'
  if (!form.value.purpose?.trim())  errors.value.purpose     = 'Please describe the purpose.'
  return Object.keys(errors.value).length === 0
}

function clearErr(field) {
  delete errors.value[field]
  apptStore.clearError()
}

async function handleBook() {
  if (!validate()) return
  apptStore.clearError()
  const result = await apptStore.createAppointment({ ...form.value })
  if (result) {
    form.value = { serviceType: '', trainerId: '', date: '', time: '', duration: null, location: '', purpose: '', specialRequirements: '' }
    errors.value = {}
    showBookingForm.value = false
  }
}

async function handleCancel(id) {
  if (!confirm('Cancel this appointment?')) return
  await apptStore.cancelAppointment(id)
}

// ── Delete ────────────────────────────────────────────────────────────────────
function confirmDeleteAthlete(appt) {
  deleteTarget.value = appt
  deleteRole.value   = 'athlete'
}

function confirmDeleteTrainer(appt) {
  deleteTarget.value = appt
  deleteRole.value   = 'trainer'
}

async function handleDelete() {
  if (!deleteTarget.value) return
  const id = deleteTarget.value.id
  let ok = false
  if (deleteRole.value === 'athlete') {
    ok = await apptStore.deleteAsAthlete(id)
  } else {
    ok = await apptStore.deleteAsTrainer(id)
  }
  if (ok) deleteTarget.value = null
}

function openEdit(appt) {
  editingAppt.value = appt
  editForm.value = {
    serviceType:        appt.serviceType,
    trainerId:          appt.trainerId,
    date:               appt.date,
    time:               appt.time,
    duration:           appt.duration,
    location:           appt.location,
    purpose:            appt.purpose,
    specialRequirements: appt.specialRequirements || ''
  }
  editErrors.value = {}
  apptStore.clearError()
}

function closeEdit() {
  editingAppt.value = null
  editErrors.value  = {}
  apptStore.clearError()
}

function validateEdit() {
  editErrors.value = {}
  if (!editForm.value.serviceType)     editErrors.value.serviceType = 'Please select a service type.'
  if (!editForm.value.trainerId)       editErrors.value.trainerId   = 'Please select a trainer.'
  if (!editForm.value.date)            editErrors.value.date        = 'Please select a date.'
  if (!editForm.value.time)            editErrors.value.time        = 'Please select a time.'
  if (!editForm.value.duration)        editErrors.value.duration    = 'Please select a duration.'
  if (!editForm.value.location)        editErrors.value.location    = 'Please select a location.'
  if (!editForm.value.purpose?.trim()) editErrors.value.purpose     = 'Please describe the purpose.'
  return Object.keys(editErrors.value).length === 0
}

function clearEditErr(field) {
  delete editErrors.value[field]
  apptStore.clearError()
}

async function handleEdit() {
  if (!validateEdit()) return
  apptStore.clearError()
  const result = await apptStore.editAppointment(editingAppt.value.id, { ...editForm.value })
  if (result) closeEdit()
}

async function handleApprove(id) {
  await apptStore.approveAppointment(id, remarksMap[id] || '')
  delete remarksMap[id]
}

async function handleReject(id) {
  if (!remarksMap[id]?.trim() && !confirm('Reject without providing a reason?')) return
  await apptStore.rejectAppointment(id, remarksMap[id] || '')
  delete remarksMap[id]
}

function openFeedbackEdit(appt) {
  feedbackEditId.value    = appt.id
  feedbackMap[appt.id]    = appt.trainerFeedback || ''
  apptStore.clearError()
}

async function handleSaveFeedback(id) {
  const text = feedbackMap[id]?.trim()
  if (!text) return
  const result = await apptStore.addFeedback(id, text)
  if (result) {
    feedbackEditId.value = null
    delete feedbackMap[id]
  }
}

function selectTrainer(trainer) {
  form.value.trainerId = trainer.userId
  form.value.date      = ''
  form.value.time      = ''
  clearErr('trainerId')
  delete errors.value.date
  delete errors.value.time
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function initials(name) {
  if (!name) return '?'
  return name.split(' ').filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join('')
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d + 'T00:00:00').toLocaleDateString('en-MY', {
    weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
  })
}

function serviceLabel(s) {
  const m = { FITNESS_TRAINING: '💪 Fitness Training', NUTRITION_COUNSELLING: '🥗 Nutrition Counselling', WELLNESS_COACHING: '🧘 Wellness Coaching' }
  return m[s] || s
}

function locationLabel(l) {
  return l === 'GYM' ? '🏋️ Gym' : l === 'ONLINE' ? '💻 Online' : l || ''
}
</script>

<style scoped>
/* ── Page ──────────────────────────────────────────────────────────────────── */
.appt-page   { display: flex; flex-direction: column; gap: 22px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.page-title  { font-family: 'Barlow Condensed', sans-serif; font-size: 32px; font-weight: 700; color: var(--color-text); margin: 0 0 4px; }
.page-subtitle { color: var(--color-muted); font-size: 14px; margin: 0; }

/* ── Card ──────────────────────────────────────────────────────────────────── */
.card          { background: var(--color-bg-2); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 22px; }
.booking-card  { padding-bottom: 28px; }
.card-header   { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
.card-icon     { font-size: 18px; }
.card-title    { font-family: 'Barlow Condensed', sans-serif; font-size: 18px; font-weight: 700; flex: 1; margin: 0; color: var(--color-text); }

/* ── Form ──────────────────────────────────────────────────────────────────── */
.booking-form  { display: flex; flex-direction: column; gap: 18px; }
.form-row      { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 600px) { .form-row { grid-template-columns: 1fr; } }

.form-group    { display: flex; flex-direction: column; gap: 6px; }
.form-label    { font-size: 13px; font-weight: 600; color: var(--color-text); }
.req           { color: #ef4444; }
.optional-tag  { font-size: 11px; font-weight: 400; color: var(--color-muted); margin-left: 4px; }
.field-error   { font-size: 11px; color: #ef4444; margin: 0; }

.form-input, .form-select, .form-textarea {
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 9px 12px;
  color: var(--color-text);
  font-size: 13px; font-family: inherit;
  outline: none;
  transition: border-color 0.15s;
}
.form-input:focus, .form-select:focus, .form-textarea:focus { border-color: var(--color-green-light); }
.form-textarea { resize: vertical; min-height: 72px; }
.has-error .form-input, .has-error .form-select, .has-error .form-textarea { border-color: #ef4444; }

/* ── Duration ──────────────────────────────────────────────────────────────── */
.duration-row { display: flex; gap: 10px; flex-wrap: wrap; }
.dur-btn {
  padding: 8px 18px; border-radius: var(--radius-md);
  border: 1px solid var(--color-border); background: var(--color-bg-3);
  color: var(--color-text); font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.15s; font-family: inherit;
}
.dur-btn:hover  { border-color: var(--color-green-light); color: var(--color-green-light); }
.dur-btn.active { background: rgba(180,255,0,0.12); border-color: var(--color-green-light); color: var(--color-green-light); }

/* ── Availability hint ─────────────────────────────────────────────────────── */
.avail-hint {
  display: flex; align-items: flex-start; gap: 8px;
  background: rgba(180,255,0,0.06);
  border: 1px solid rgba(180,255,0,0.2);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 12px;
}
.avail-hint-icon { font-size: 14px; flex-shrink: 0; margin-top: 1px; }
.avail-hint-slots { display: flex; flex-wrap: wrap; align-items: center; gap: 5px; }
.avail-hint-label { color: var(--color-muted); }
.avail-hint-chip {
  background: var(--color-bg-3);
  border: 1px solid var(--color-border);
  padding: 2px 8px; border-radius: 10px;
  color: var(--color-green-light); font-size: 11px; font-weight: 600;
}

/* ── Trainer cards ─────────────────────────────────────────────────────────── */
.trainer-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 10px; }
.trainer-card {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 12px 14px;
  background: var(--color-bg-3);
  border: 2px solid var(--color-border); border-radius: var(--radius-md);
  cursor: pointer; text-align: left; position: relative;
  font-family: inherit; color: var(--color-text);
  transition: border-color 0.15s, background 0.15s;
}
.trainer-card:hover   { border-color: var(--color-green-light); }
.trainer-card.selected { border-color: var(--color-green-light); background: rgba(180,255,0,0.07); }
.trainer-avatar {
  width: 42px; height: 42px; border-radius: 50%;
  background: var(--color-bg-2); border: 1px solid var(--color-border);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; color: var(--color-green-light);
  overflow: hidden; flex-shrink: 0;
}
.trainer-avatar img { width: 100%; height: 100%; object-fit: cover; }
.trainer-info { flex: 1; min-width: 0; }
.trainer-name { font-size: 13px; font-weight: 700; margin-bottom: 2px; }
.trainer-exp  { font-size: 11px; color: var(--color-muted); margin-bottom: 4px; }
.trainer-avail { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; margin-top: 3px; }
.avail-label  { font-size: 10px; color: var(--color-muted); }
.avail-chip   { font-size: 10px; background: var(--color-bg-2); border: 1px solid var(--color-border); padding: 1px 6px; border-radius: 10px; color: var(--color-muted); }
.trainer-tick { position: absolute; top: 8px; right: 10px; font-size: 14px; font-weight: 700; color: var(--color-green-light); }

/* ── Buttons ───────────────────────────────────────────────────────────────── */
.btn {
  padding: 9px 18px; border-radius: var(--radius-md);
  font-size: 13px; font-weight: 600;
  border: none; cursor: pointer;
  display: inline-flex; align-items: center; gap: 6px;
  transition: opacity 0.15s, transform 0.1s; font-family: inherit;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn:not(:disabled):hover  { opacity: 0.88; }
.btn:not(:disabled):active { transform: scale(0.97); }
.btn-primary { background: var(--color-green-light); color: #000; }
.btn-full    { width: 100%; justify-content: center; padding: 12px; }
.btn-sm      { padding: 6px 12px; font-size: 12px; }
.btn-ghost   { background: var(--color-bg-3); border: 1px solid var(--color-border); color: var(--color-muted); }
.btn-approve { background: rgba(180,255,0,0.12); border: 1px solid var(--color-green-light); color: var(--color-green-light); flex: 1; justify-content: center; }
.btn-reject  { background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.35); color: #ef4444; flex: 1; justify-content: center; }

.btn-spinner { width: 13px; height: 13px; border: 2px solid rgba(0,0,0,0.2); border-top-color: #000; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Alerts ────────────────────────────────────────────────────────────────── */
.alert { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: var(--radius-md); font-size: 13px; }
.alert-success { background: rgba(180,255,0,0.08); border: 1px solid rgba(180,255,0,0.3); color: var(--color-green-light); }
.alert-error   { background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.3); color: #ef4444; }
.alert-close   { margin-left: auto; background: none; border: none; cursor: pointer; font-size: 16px; color: inherit; }

/* ── Filter ────────────────────────────────────────────────────────────────── */
.filter-row { display: flex; gap: 6px; flex-wrap: wrap; margin-left: auto; }
.filter-btn {
  padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600;
  border: 1px solid var(--color-border); background: transparent; color: var(--color-muted);
  cursor: pointer; transition: all 0.15s; font-family: inherit;
}
.filter-btn.active, .filter-btn:hover { border-color: var(--color-green-light); color: var(--color-green-light); background: rgba(180,255,0,0.08); }

/* ── Appointment list (athlete) ────────────────────────────────────────────── */
.appt-list   { display: flex; flex-direction: column; }
.appt-item   { display: flex; align-items: flex-start; gap: 14px; padding: 16px 0; border-bottom: 1px solid var(--color-border); }
.appt-item:last-child { border-bottom: none; }
.appt-dot    { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 5px; }
.dot-pending   { background: #f59e0b; }
.dot-approved  { background: var(--color-green-light); }
.dot-rejected  { background: #ef4444; }
.dot-cancelled { background: var(--color-muted); }
.appt-body   { flex: 1; min-width: 0; }
.appt-top    { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; flex-wrap: wrap; }
.appt-service { font-size: 14px; font-weight: 700; color: var(--color-text); }
.appt-badge  { font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 20px; }
.badge-pending   { background: rgba(245,158,11,0.15); color: #f59e0b; }
.badge-approved  { background: rgba(180,255,0,0.12); color: var(--color-green-light); }
.badge-rejected  { background: rgba(239,68,68,0.1); color: #ef4444; }
.badge-cancelled { background: var(--color-bg-3); color: var(--color-muted); }
.appt-sub    { font-size: 13px; color: var(--color-muted); margin-bottom: 5px; }
.appt-meta   { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12px; color: var(--color-muted); margin-bottom: 5px; }
.appt-purpose { font-size: 13px; color: var(--color-text); }
.appt-remarks  { margin-top: 7px; font-size: 12px; color: var(--color-muted); background: var(--color-bg-3); padding: 7px 10px; border-radius: var(--radius-md); }
.appt-feedback { margin-top: 7px; font-size: 12px; color: var(--color-text); background: rgba(180,255,0,0.05); border: 1px solid rgba(180,255,0,0.15); padding: 8px 10px; border-radius: var(--radius-md); }

/* ── Trainer feedback section ──────────────────────────────────────────────── */
.t-feedback-section { border-top: 1px solid var(--color-border); padding-top: 10px; }
.t-feedback-display { display: flex; flex-direction: column; gap: 5px; }
.t-feedback-header  { display: flex; align-items: center; justify-content: space-between; font-size: 12px; font-weight: 700; color: var(--color-green-light); }
.btn-link { background: none; border: none; cursor: pointer; font-size: 11px; color: var(--color-muted); text-decoration: underline; padding: 0; font-family: inherit; }
.btn-link:hover { color: var(--color-text); }
.t-feedback-text { font-size: 13px; color: var(--color-text); margin: 0; line-height: 1.5; }
.t-feedback-form { display: flex; flex-direction: column; gap: 8px; }
.t-feedback-label { font-size: 12px; font-weight: 600; color: var(--color-text); }
.t-feedback-btns  { display: flex; gap: 8px; }
.btn-feedback-save { background: rgba(180,255,0,0.12); border: 1px solid var(--color-green-light); color: var(--color-green-light); flex: 1; justify-content: center; }

/* ── Loading / Empty ───────────────────────────────────────────────────────── */
.load-row    { display: flex; align-items: center; gap: 10px; padding: 16px; color: var(--color-muted); font-size: 13px; }
.load-row.center { justify-content: center; padding: 32px; }
.muted       { color: var(--color-muted); }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 40px 16px; color: var(--color-muted); font-size: 13px; text-align: center; }
.empty-state span { font-size: 32px; opacity: 0.5; }
.spinner     { width: 22px; height: 22px; border: 3px solid var(--color-border); border-top-color: var(--color-green-light); border-radius: 50%; animation: spin 0.8s linear infinite; }

/* ── Trainer tabs ──────────────────────────────────────────────────────────── */
.tab-row   { display: flex; gap: 6px; flex-wrap: wrap; }
.tab-btn   { padding: 7px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; border: 1px solid var(--color-border); background: transparent; color: var(--color-muted); cursor: pointer; transition: all 0.15s; font-family: inherit; display: flex; align-items: center; gap: 5px; }
.tab-btn.active, .tab-btn:hover { border-color: var(--color-green-light); color: var(--color-green-light); background: rgba(180,255,0,0.08); }
.tab-cnt   { font-size: 10px; background: var(--color-bg-3); padding: 1px 5px; border-radius: 8px; }
.pending-badge { background: #f59e0b; color: #000; font-size: 12px; font-weight: 700; padding: 5px 14px; border-radius: 20px; }

/* ── Trainer appointment list ──────────────────────────────────────────────── */
.trainer-appt-list { display: flex; flex-direction: column; gap: 0; }

.tl-item {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-border);
  border-radius: var(--radius-lg);
  margin-bottom: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.tl-pending   { border-left-color: #f59e0b; }
.tl-approved  { border-left-color: var(--color-green-light); }
.tl-rejected  { border-left-color: #ef4444; opacity: 0.85; }
.tl-cancelled { border-left-color: var(--color-muted); opacity: 0.7; }

/* Main row — horizontal columns */
.tl-main {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  flex-wrap: wrap;
}

/* Athlete column */
.tl-athlete {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 180px;
  flex: 0 0 auto;
}
.tl-athlete-info { min-width: 0; }
.tl-name    { font-size: 14px; font-weight: 700; color: var(--color-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tl-service { font-size: 11px; color: var(--color-muted); white-space: nowrap; }

/* Generic data columns */
.tl-col     { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.tl-col-main { font-size: 13px; font-weight: 600; color: var(--color-text); white-space: nowrap; }
.tl-col-sub  { font-size: 11px; color: var(--color-muted); white-space: nowrap; }

.tl-datetime { flex: 0 0 160px; }
.tl-dur      { flex: 0 0 110px; }
.tl-purpose  { flex: 1; min-width: 120px; }
.tl-purpose-text { font-size: 13px; color: var(--color-text); }
.tl-special  { font-size: 11px; color: #f59e0b; margin-top: 2px; }

/* Right-side: status + delete */
.tl-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
  margin-left: auto;
}

/* Expanded rows below main */
.tl-expand {
  border-top: 1px solid var(--color-border);
  padding: 12px 16px;
}
.tl-pending-actions { display: flex; align-items: flex-start; gap: 10px; flex-wrap: wrap; }
.tl-remarks-input   { flex: 1; min-width: 200px; resize: none; }
.tl-action-btns     { display: flex; gap: 8px; flex-shrink: 0; }
.tl-remarks-display { font-size: 12px; color: var(--color-muted); }

/* Avatar (shared) */
.t-avatar    { width: 38px; height: 38px; border-radius: 50%; background: var(--color-bg-3); border: 1px solid var(--color-border); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; color: var(--color-green-light); flex-shrink: 0; }

/* Feedback within list item */
.tl-feedback-section { border-top: 1px solid var(--color-border); padding: 12px 16px; }

/* ── Appointment action buttons ────────────────────────────────────────────── */
.appt-actions { display: flex; gap: 8px; flex-wrap: wrap; flex-shrink: 0; align-items: center; margin-top: 4px; }
.btn-edit   { background: rgba(59,130,246,0.1); border: 1px solid rgba(59,130,246,0.35); color: #3b82f6; }
.btn-edit:hover { background: rgba(59,130,246,0.18); }
.btn-delete { background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.25); color: #ef4444; }
.btn-delete:hover { background: rgba(239,68,68,0.15); }

/* Trainer card delete row */
.t-delete-row { padding: 8px 0 2px; border-top: 1px solid var(--color-border); margin-top: 6px; display: flex; justify-content: flex-end; }
.btn-delete-sm { background: none; border: none; cursor: pointer; font-size: 12px; color: var(--color-muted); padding: 4px 8px; border-radius: var(--radius-sm); transition: all var(--transition); }
.btn-delete-sm:hover { color: #ef4444; background: rgba(239,68,68,0.08); }

/* Delete modal */
.modal-small .modal-body  { display: flex; flex-direction: column; gap: 10px; }
.modal-small .modal-body p { font-size: 14px; color: var(--color-text-dim); margin: 0; }
.delete-preview { display: flex; align-items: center; gap: 10px; padding: 10px 12px; background: var(--color-bg-3); border-radius: var(--radius-sm); font-size: 13px; color: var(--color-text); flex-wrap: wrap; }
.delete-warning { font-size: 12px; color: var(--color-error) !important; }
.btn-danger { background: var(--color-error, #ef4444); color: #fff; border: none; }
.btn-danger:hover:not(:disabled) { opacity: 0.85; }
.btn-danger:disabled { opacity: 0.55; cursor: not-allowed; }

/* ── Edit Modal ─────────────────────────────────────────────────────────────── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(0,0,0,0.65);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.modal-card {
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  width: 100%; max-width: 620px;
  max-height: 90vh; overflow-y: auto;
  padding: 24px 26px;
  display: flex; flex-direction: column; gap: 18px;
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
}
.modal-title {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 20px; font-weight: 700; color: var(--color-text); margin: 0;
}
.modal-close {
  background: none; border: none; font-size: 22px;
  color: var(--color-muted); cursor: pointer; line-height: 1; padding: 2px 6px;
}
.modal-close:hover { color: var(--color-text); }
.modal-footer {
  display: flex; justify-content: flex-end; gap: 10px; padding-top: 4px;
}

/* ── Transitions ───────────────────────────────────────────────────────────── */
.slide-down-enter-active { transition: all 0.3s ease; }
.slide-down-enter-from   { opacity: 0; transform: translateY(-10px); }
.slide-down-leave-active { transition: all 0.2s ease; }
.slide-down-leave-to     { opacity: 0; transform: translateY(-10px); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to       { opacity: 0; }
</style>
