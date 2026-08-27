import axiosClient from './axiosClient'

const patientPortalAppointmentApi = {
  getSpecialties: (params = { active: true }) => {
    return axiosClient.get('/system/specialties', { params })
  },
  getDoctors: (params = {}) => {
    return axiosClient.get('/users/doctors', { params })
  },
  getAvailableSlots: (doctorId, date) => {
    return axiosClient.get('/patient-portal/appointments/available-slots', {
      params: { doctorId, date },
    })
  },
  bookAppointment: (data) => {
    return axiosClient.post('/patient-portal/appointments', data)
  },
  getMyAppointments: (patientId) => {
    return axiosClient.get('/appointments', {
      params: { patientId, size: 100 },
    })
  },
  cancelAppointment: (id, reason) => {
    return axiosClient.patch(`/appointments/${id}/cancel`, {
      reason: reason || 'Bệnh nhân yêu cầu hủy lịch trực tuyến',
    })
  },
}

export default patientPortalAppointmentApi
