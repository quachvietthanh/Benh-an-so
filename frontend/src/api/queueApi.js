import axiosClient from './axiosClient'

/**
 * RESTful API Client for Medical Queue
 * Backend Controller: MedicalQueueController.java (@RequestMapping("/queue"))
 */
const queueApi = {
  // POST /api/v1/queue (Add patient to queue)
  addToQueue: (data) => {
    return axiosClient.post('/queue', data)
  },

  // POST /api/v1/queue/call-next (Call next patient)
  callNext: (data = {}) => {
    return axiosClient.post('/queue/call-next', data)
  },

  // PUT /api/v1/queue/{id}/status (Update queue item status)
  updateStatus: (id, data) => {
    const payload = typeof data === 'string' ? { newStatus: data } : data
    return axiosClient.put(`/queue/${id}/status`, payload)
  },

  // GET /api/v1/queue/room/{roomNumber}
  getByRoom: (roomNumber, params = {}) => {
    return axiosClient.get(`/queue/room/${encodeURIComponent(roomNumber)}`, { params })
  },

  // GET /api/v1/queue/doctor/{doctorId}
  getByDoctor: (doctorId, params = {}) => {
    return axiosClient.get(`/queue/doctor/${doctorId}`, { params })
  },

  // GET /api/v1/queue/count
  count: (params = {}) => {
    return axiosClient.get('/queue/count', { params })
  },

  // Alias for backward compatibility
  getQueue: (params = {}) => {
    return axiosClient.get('/queue/room/ALL', { params })
  },
}

export default queueApi
