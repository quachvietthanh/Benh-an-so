import axiosClient from './axiosClient'

const appointmentApi = {
  getAll: (params) => axiosClient.get('/appointments', { params: { size: 100, ...params } }),
  getById: (id) => axiosClient.get(`/appointments/${id}`),
  create: (data) => axiosClient.post('/appointments', data),
  cancel: (id, reason) => axiosClient.patch(`/appointments/${id}/cancel`, { reason: reason || 'Khai báo hủy lịch' }),
  noShow: (id) => axiosClient.patch(`/appointments/${id}/no-show`),
  sendReminder: (id) => axiosClient.post(`/appointments/${id}/reminder`),
  getOverdue: (params) => axiosClient.get('/appointments/overdue', { params }),
  checkIn: (id) => axiosClient.post(`/appointments/${id}/check-in`),
}

export default appointmentApi
