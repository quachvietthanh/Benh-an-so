import axiosClient from './axiosClient'

const queueApi = {
  getQueues: (params = {}) => axiosClient.get('/queues', { params }),
  getMyQueue: (params = {}) => axiosClient.get('/queues/me', { params }),
  getById: (itemId) => axiosClient.get(`/queue-items/${itemId}`),
  checkInAppointment: (appointmentId) => axiosClient.post(`/appointments/${appointmentId}/check-in`),
  checkInWalkIn: (data) => axiosClient.post('/queue-items/walk-in', data),
  callNext: (queueId) => axiosClient.post(`/queues/${queueId}/call-next`),
  updateStatus: (itemId, data) => axiosClient.patch(`/queue-items/${itemId}/status`, data),
  complete: (itemId) => axiosClient.post(`/queue-items/${itemId}/complete`),
}

export default queueApi
