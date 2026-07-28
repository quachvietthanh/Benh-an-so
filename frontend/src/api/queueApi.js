import axiosClient from './axiosClient'

const queueApi = {
  // Add patient to queue (POST /api/v1/queue)
  addToQueue: (data) => axiosClient.post('/queue', data),

  // Call next patient (POST /api/v1/queue/call-next) with fallback to legacy endpoint
  callNext: async (payload = {}) => {
    try {
      return await axiosClient.post('/queue/call-next', payload)
    } catch (err) {
      // Fallback to legacy endpoint if dedicated endpoint fails or doctor info isn't in payload
      return await axiosClient.post('/appointments/queue/call-next')
    }
  },

  // Update status (PUT /api/v1/queue/{id}/status)
  updateStatus: (id, data) => {
    // data can be string "COMPLETED" or object { newStatus, doctorId, cancelReason }
    const body = typeof data === 'string' ? { newStatus: data } : data
    return axiosClient.put(`/queue/${id}/status`, body)
  },

  // Get queue by room (GET /api/v1/queue/room/{roomNumber})
  getByRoom: (roomNumber, params = {}) =>
    axiosClient.get(`/queue/room/${encodeURIComponent(roomNumber)}`, { params }),

  // Get queue by doctor (GET /api/v1/queue/doctor/{doctorId})
  getByDoctor: (doctorId, params = {}) =>
    axiosClient.get(`/queue/doctor/${doctorId}`, { params }),

  // Get all queue items (GET /api/v1/appointments/queue or /api/v1/queue)
  getQueue: async (params = {}) => {
    try {
      return await axiosClient.get('/appointments/queue', { params })
    } catch (err) {
      return await axiosClient.get('/queue/room/ALL', { params })
    }
  },

  // Count queue items (GET /api/v1/queue/count)
  count: (params = {}) => axiosClient.get('/queue/count', { params }),
}

export default queueApi
