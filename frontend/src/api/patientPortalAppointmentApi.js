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
    return axiosClient.get('/patient-portal/appointments')
      .catch((err) => {
        if (patientId) {
          return axiosClient.get('/appointments', {
            params: { patientId, size: 100 },
          })
        }
        throw err
      })
  },
  cancelAppointment: (id, cancellationReason) => {
    const payload = {}
    if (cancellationReason && cancellationReason.trim()) {
      payload.cancellationReason = cancellationReason.trim()
    }
    return axiosClient.patch(`/patient-portal/appointments/${id}/cancel`, payload)
  },
  rescheduleAppointment: (id, data) => {
    return axiosClient.put(`/patient-portal/appointments/${id}/reschedule`, data)
  },
  getAppointmentDetail: (id) => {
    return axiosClient.get(`/patient-portal/appointments/${id}`)
  },
}

export default patientPortalAppointmentApi
