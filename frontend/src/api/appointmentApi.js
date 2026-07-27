import axiosClient from './axiosClient'

const appointmentApi = {
  getAll: () => axiosClient.get('/appointments'),
  getDoctors: () => axiosClient.get('/appointments/doctors'),
  getQueue: () => axiosClient.get('/appointments/queue'),
  create: (data) => axiosClient.post('/appointments', data),
  cancel: (id, reason) => axiosClient.patch(`/appointments/${id}/cancel`, { reason }),
  noShow: (id) => axiosClient.patch(`/appointments/${id}/no-show`),
  checkIn: (id) => axiosClient.patch(`/appointments/${id}/check-in`),
  callNext: () => axiosClient.post('/appointments/queue/call-next'),
  complete: (id) => axiosClient.patch(`/appointments/${id}/complete`),
}

export default appointmentApi
