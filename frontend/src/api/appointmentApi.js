import axiosClient from './axiosClient'
import userApi from './userApi'
import queueApi from './queueApi'

/**
 * RESTful API Client for Appointments
 * Backend Controller: AppointmentController.java (@RequestMapping("/appointments"))
 */
const appointmentApi = {
  // GET /api/v1/appointments
  getAll: (params = {}) => {
    const searchParams = {
      patientId: params.patientId || null,
      doctorId: params.doctorId || null,
      status: params.status || null,
      startDate: params.startDate || null,
      endDate: params.endDate || null,
      page: params.page !== undefined ? params.page : 0,
      size: params.size || 20,
    }
    return axiosClient.get('/appointments', { params: searchParams })
  },

  // GET /api/v1/appointments/{id}
  getById: (id) => {
    return axiosClient.get(`/appointments/${id}`)
  },

  // POST /api/v1/appointments
  create: (data) => {
    return axiosClient.post('/appointments', data)
  },

  // POST /api/v1/appointments/{id}/reminder
  sendReminder: (id) => {
    return axiosClient.post(`/appointments/${id}/reminder`)
  },

  // PATCH /api/v1/appointments/{id}/cancel
  cancel: (id, reason) => {
    return axiosClient.patch(`/appointments/${id}/cancel`, { reason })
  },

  // GET /api/v1/appointments/overdue
  getOverdue: (params = {}) => {
    return axiosClient.get('/appointments/overdue', { params })
  },

  // PATCH /api/v1/appointments/{id}/no-show
  noShow: (id) => {
    return axiosClient.patch(`/appointments/${id}/no-show`)
  },

  // GET active doctors list from GET /api/v1/users/doctors
  getDoctors: () => {
    return userApi.getDoctors()
  },

  // Queue integration aliases for backwards compatibility
  getQueue: (params) => queueApi.getQueue(params),
  checkIn: (id) => queueApi.updateStatus(id, 'IN_PROGRESS'),
  callNext: (payload) => queueApi.callNext(payload),
  complete: (id) => queueApi.updateStatus(id, 'COMPLETED'),
  updateQueueStatus: (id, data) => queueApi.updateStatus(id, data),
  addToQueue: (data) => queueApi.addToQueue(data),
}

export default appointmentApi
