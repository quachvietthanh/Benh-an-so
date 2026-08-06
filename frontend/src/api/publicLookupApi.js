import { mergeAppointments, mergePatients } from '../utils/storageHelpers.js'

const toCareState = (status) => {
  const value = String(status || '').toUpperCase()
  if (value === 'COMPLETED') return 'COMPLETED'
  if (['CHECKED_IN', 'CALLED', 'IN_PROGRESS'].includes(value)) return 'IN_PROGRESS'
  if (['CANCELLED', 'NO_SHOW'].includes(value)) return 'UNAVAILABLE'
  return 'SCHEDULED'
}

const scheduledAt = (appointment) => {
  if (appointment.startTime || appointment.appointmentAt) {
    return appointment.startTime || appointment.appointmentAt
  }
  if (appointment.date) return `${appointment.date}T${appointment.slot || '00:00'}:00`
  return new Date().toISOString()
}

// Backend không có public lookup controller. Tra cứu chỉ dùng dữ liệu đã lưu cục bộ và không gửi PII tới URL 404.
const publicLookupApi = {
  lookupAppointment: async ({ appointmentCode, dateOfBirth }) => {
    const normalizedCode = String(appointmentCode || '').trim().toUpperCase()
    const appointment = mergeAppointments([]).find((item) => (
      String(item.appointmentCode || item.code || '').trim().toUpperCase() === normalizedCode
    ))
    const patient = appointment
      ? mergePatients([]).find((item) => String(item.id) === String(appointment.patientId))
      : null
    const patientDateOfBirth = patient?.dateOfBirth || patient?.dob
    const matched = Boolean(appointment && patient && patientDateOfBirth === dateOfBirth)

    return {
      data: matched
        ? {
            matched: true,
            careState: toCareState(appointment.status),
            scheduledAt: scheduledAt(appointment),
          }
        : { matched: false, careState: null, scheduledAt: null },
    }
  },
}

export default publicLookupApi
