import dayjs from 'dayjs'

/**
 * Validation & Helper utilities for Appointment Confirmation (NCL-03-CN-008)
 * Conforming to Business Rules:
 * - QTN-01 (RBAC: Receptionist & Admin for staff, Patient for portal)
 * - QTN-08 (Precondition: SCHEDULED and startTime > now)
 */

/**
 * Check if a receptionist or staff can confirm an appointment.
 *
 * @param {Object} appointment
 * @param {dayjs.Dayjs|Date|string} [nowRef]
 * @returns {{ allowed: boolean, reason: string }}
 */
export function canConfirmAppointment(appointment, nowRef = dayjs()) {
  if (!appointment) {
    return { allowed: false, reason: 'Không tìm thấy thông tin lịch hẹn.' }
  }

  const status = appointment.status
  if (status === 'CONFIRMED') {
    return { allowed: false, reason: 'Lịch hẹn đã được xác nhận trước đó.' }
  }

  if (status === 'CANCELLED') {
    return { allowed: false, reason: 'Lịch hẹn đã bị hủy, không thể xác nhận.' }
  }

  if (status === 'CHECKED_IN') {
    return { allowed: false, reason: 'Bệnh nhân đã được tiếp nhận khám.' }
  }

  if (status === 'COMPLETED') {
    return { allowed: false, reason: 'Lịch hẹn đã hoàn tất khám bệnh.' }
  }

  if (status !== 'SCHEDULED') {
    return {
      allowed: false,
      reason: 'Chỉ có thể xác nhận lịch hẹn ở trạng thái đã đặt.',
    }
  }

  const timeVal = appointment.startTime || appointment.appointmentAt || appointment.date
  if (timeVal) {
    const startDayjs = dayjs(timeVal)
    const nowDayjs = dayjs(nowRef)
    if (startDayjs.isValid() && nowDayjs.isValid()) {
      if (!startDayjs.isAfter(nowDayjs)) {
        return {
          allowed: false,
          reason: 'Lịch hẹn đã quá giờ khám, không thể xác nhận.',
        }
      }
    }
  }

  return { allowed: true, reason: '' }
}

/**
 * Check if a patient can self-confirm their appointment on the portal.
 *
 * @param {Object} appointment
 * @param {dayjs.Dayjs|Date|string} [nowRef]
 * @returns {{ allowed: boolean, reason: string }}
 */
export function canPatientConfirmAppointment(appointment, nowRef = dayjs()) {
  return canConfirmAppointment(appointment, nowRef)
}

/**
 * Format confirmation display info (operator and timestamp).
 *
 * @param {string} confirmedByName
 * @param {string|Date|dayjs.Dayjs} confirmedAt
 * @returns {string}
 */
export function formatConfirmationInfo(confirmedByName, confirmedAt) {
  if (!confirmedAt) return ''
  const timeFormatted = dayjs(confirmedAt).isValid()
    ? dayjs(confirmedAt).format('HH:mm - DD/MM/YYYY')
    : String(confirmedAt)
  const operator = confirmedByName ? confirmedByName.trim() : 'Lễ tân'
  return `Xác nhận bởi ${operator} lúc ${timeFormatted}`
}

/**
 * Filter unconfirmed future appointments for a given date.
 *
 * @param {Array} appointments
 * @param {string|dayjs.Dayjs} [date] Target date (YYYY-MM-DD) or dayjs object
 * @param {dayjs.Dayjs|Date|string} [nowRef]
 * @returns {Array}
 */
export function filterUnconfirmedAppointments(appointments = [], date = null, nowRef = dayjs()) {
  if (!Array.isArray(appointments)) return []
  const now = dayjs(nowRef)
  const targetDateStr = date ? dayjs(date).format('YYYY-MM-DD') : null

  return appointments.filter((apt) => {
    if (!apt || apt.status !== 'SCHEDULED') return false
    const timeVal = apt.startTime || apt.appointmentAt || apt.date
    if (!timeVal) return false
    const aptTime = dayjs(timeVal)
    if (!aptTime.isValid()) return false

    // Must not be past cutoff (startTime > now)
    if (!aptTime.isAfter(now)) return false

    // If specific date requested, match calendar date
    if (targetDateStr) {
      if (aptTime.format('YYYY-MM-DD') !== targetDateStr) return false
    }

    return true
  }).sort((a, b) => {
    const timeA = dayjs(a.startTime || a.appointmentAt || a.date).valueOf()
    const timeB = dayjs(b.startTime || b.appointmentAt || b.date).valueOf()
    return timeA - timeB
  })
}
