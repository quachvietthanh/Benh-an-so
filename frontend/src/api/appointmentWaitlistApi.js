import axiosClient from './axiosClient.js'

const appointmentWaitlistApi = {
  add: (payload) => axiosClient.post('/appointments/waitlist', payload),
  list: (params) => axiosClient.get('/appointments/waitlist', { params }),
  getSuggestion: (doctorId, date) =>
    axiosClient.get('/appointments/waitlist/suggest', { params: { doctorId, date } }),
  cancel: (id, cancelReason) =>
    axiosClient.patch(`/appointments/waitlist/${id}/cancel`, { cancelReason }),
}

export default appointmentWaitlistApi
