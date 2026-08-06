import axiosClient from './axiosClient.js'
import { pageParams, pickFields } from './apiContract.js'

const APPOINTMENT_FIELDS = ['patientId', 'doctorId', 'startTime', 'endTime', 'reason']
const SEARCH_FIELDS = ['patientId', 'doctorId', 'status', 'startDate', 'endDate']

const appointmentApi = {
  search: (params = {}) => axiosClient.get('/appointments', {
    params: pageParams(params, SEARCH_FIELDS),
  }),
  getAll: (params = {}) => axiosClient.get('/appointments', {
    params: pageParams(params, SEARCH_FIELDS),
  }),
  getById: (id) => axiosClient.get(`/appointments/${id}`),
  getDoctors: () => axiosClient.get('/users/doctors'),
  create: (data) => axiosClient.post('/appointments', pickFields(data, APPOINTMENT_FIELDS)),
  sendReminder: (id) => axiosClient.post(`/appointments/${id}/reminder`),
  cancel: (id, cancelReason) => axiosClient.patch(`/appointments/${id}/cancel`, { cancelReason }),
  noShow: (id) => axiosClient.patch(`/appointments/${id}/no-show`),
  checkIn: (id) => axiosClient.post(`/appointments/${id}/check-in`),
  getOverdue: (params = {}) => axiosClient.get('/appointments/overdue', {
    params: pageParams(params),
  }),
}

export default appointmentApi
