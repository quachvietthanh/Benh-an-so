import axiosClient from './axiosClient'
import dayjs from 'dayjs'

const queueApi = {
  getQueues: (params = {}) => {
    const formattedParams = {
      date: dayjs().format('YYYY-MM-DD'),
      ...params,
    }
    return axiosClient.get('/queues', { params: formattedParams })
  },
  getMyQueue: (params = {}) => {
    const formattedParams = {
      date: dayjs().format('YYYY-MM-DD'),
      ...params,
    }
    return axiosClient.get('/queues/me', { params: formattedParams })
  },
  getById: (itemId) => axiosClient.get(`/queue-items/${itemId}`),
  checkInAppointment: (appointmentId) => axiosClient.post(`/appointments/${appointmentId}/check-in`),
  checkInWalkIn: (data) => axiosClient.post('/queue-items/walk-in', data),
  callNext: (queueId) => axiosClient.post(`/queues/${queueId}/call-next`),
  updateStatus: (itemId, data) => axiosClient.patch(`/queue-items/${itemId}/status`, data),
  skip: (itemId, reason = 'Bỏ qua lượt khám') => axiosClient.post(`/queue-items/${itemId}/skip`, { reason }),
  complete: (itemId) => axiosClient.post(`/queue-items/${itemId}/complete`),
}

export default queueApi
