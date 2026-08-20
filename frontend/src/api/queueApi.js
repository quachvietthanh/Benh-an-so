import axiosClient from './axiosClient'
import dayjs from 'dayjs'

const queueApi = {
  getQueues: (params = {}) => {
    const formattedParams = {
      date: dayjs().format('YYYY-MM-DD'),
      doctorId: params.doctorId,
      roomId: params.roomId,
      ...('date' in params ? { date: params.date } : {}),
    }
    Object.keys(formattedParams).forEach((key) => {
      if (formattedParams[key] === null || formattedParams[key] === undefined || formattedParams[key] === '') {
        delete formattedParams[key]
      }
    })
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

  updateStatus: (itemId, targetStatus, cancelReason) =>
    axiosClient.patch(`/queue-items/${itemId}/status`, {
      targetStatus,
      ...(cancelReason ? { cancelReason } : {}),
    }),

  skip: (itemId, reason = 'Vắng mặt khi gọi') => axiosClient.post(`/queue-items/${itemId}/skip`, { reason }),

  complete: (itemId) => axiosClient.post(`/queue-items/${itemId}/complete`),
}

export default queueApi
