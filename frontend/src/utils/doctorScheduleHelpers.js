import dayjs from 'dayjs'

export const DAYS_OF_WEEK = [
  { key: 'MONDAY', label: 'Thứ Hai', shortLabel: 'T2', index: 1 },
  { key: 'TUESDAY', label: 'Thứ Ba', shortLabel: 'T3', index: 2 },
  { key: 'WEDNESDAY', label: 'Thứ Tư', shortLabel: 'T4', index: 3 },
  { key: 'THURSDAY', label: 'Thứ Năm', shortLabel: 'T5', index: 4 },
  { key: 'FRIDAY', label: 'Thứ Sáu', shortLabel: 'T6', index: 5 },
  { key: 'SATURDAY', label: 'Thứ Bảy', shortLabel: 'T7', index: 6 },
  { key: 'SUNDAY', label: 'Chủ Nhật', shortLabel: 'CN', index: 0 },
]

export const getDayLabel = (dayOfWeek) => {
  const found = DAYS_OF_WEEK.find((d) => d.key === dayOfWeek)
  return found ? found.label : dayOfWeek
}

export const getDayShortLabel = (dayOfWeek) => {
  const found = DAYS_OF_WEEK.find((d) => d.key === dayOfWeek)
  return found ? found.shortLabel : dayOfWeek
}

/**
 * Normalizes time string from "08:00:00" to "08:00" or returns fallback
 */
export const normalizeTimeDisplay = (timeStr) => {
  if (!timeStr) return ''
  const parts = String(timeStr).split(':')
  if (parts.length >= 2) {
    return `${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}`
  }
  return timeStr
}

/**
 * Normalizes time string to "HH:mm:ss" required by backend LocalTime
 */
export const toBackendTime = (timeStr) => {
  if (!timeStr) return ''
  const parts = String(timeStr).split(':')
  if (parts.length === 2) {
    return `${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}:00`
  }
  if (parts.length >= 3) {
    return `${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}:${parts[2].slice(0, 2).padStart(2, '0')}`
  }
  return timeStr
}

/**
 * Validates whether a given working interval [startTime, endTime] is within clinic hours [opening, closing]
 * All times are in "HH:mm:ss" or "HH:mm" format.
 */
export const isTimeWithinClinicHours = (startTime, endTime, clinicOpening, clinicClosing) => {
  if (!startTime || !endTime) return { valid: false, message: 'Vui lòng chọn đầy đủ giờ bắt đầu và kết thúc.' }

  const start = toBackendTime(startTime)
  const end = toBackendTime(endTime)

  if (end <= start) {
    return { valid: false, message: 'Giờ kết thúc phải sau giờ bắt đầu.' }
  }

  if (clinicOpening) {
    const opening = toBackendTime(clinicOpening)
    if (start < opening) {
      return {
        valid: false,
        message: `Giờ bắt đầu (${normalizeTimeDisplay(start)}) sớm hơn giờ mở cửa của phòng khám (${normalizeTimeDisplay(opening)}).`,
      }
    }
  }

  if (clinicClosing) {
    const closing = toBackendTime(clinicClosing)
    if (end > closing) {
      return {
        valid: false,
        message: `Giờ kết thúc (${normalizeTimeDisplay(end)}) muộn hơn giờ đóng cửa của phòng khám (${normalizeTimeDisplay(closing)}).`,
      }
    }
  }

  return { valid: true, message: '' }
}

/**
 * Formats a duration in minutes/hours between two timestamps or strings
 */
export const formatDuration = (startTime, endTime) => {
  if (!startTime || !endTime) return ''
  const start = dayjs(startTime)
  const end = dayjs(endTime)
  if (!start.isValid() || !end.isValid()) return ''

  const diffMinutes = end.diff(start, 'minute')
  if (diffMinutes < 0) return ''

  const hours = Math.floor(diffMinutes / 60)
  const minutes = diffMinutes % 60

  if (hours === 0) return `${minutes} phút`
  if (minutes === 0) return `${hours} giờ`
  return `${hours} giờ ${minutes} phút`
}

export const formatAppointmentStatus = (status) => {
  switch (status) {
    case 'SCHEDULED':
      return { label: 'Đã đặt hẹn', color: 'blue' }
    case 'CONFIRMED':
      return { label: 'Đã xác nhận', color: 'cyan' }
    case 'CHECKED_IN':
      return { label: 'Đã tiếp nhận', color: 'geekblue' }
    case 'IN_PROGRESS':
      return { label: 'Đang khám', color: 'orange' }
    case 'COMPLETED':
      return { label: 'Đã hoàn tất', color: 'green' }
    case 'CANCELLED':
      return { label: 'Đã hủy', color: 'default' }
    case 'NO_SHOW':
      return { label: 'Vắng mặt', color: 'volcano' }
    default:
      return { label: status || 'Không rõ', color: 'default' }
  }
}

export const formatTimeOffStatus = (status) => {
  switch (status) {
    case 'ACTIVE':
      return { label: 'Đang hiệu lực', color: 'success' }
    case 'CANCELLED':
      return { label: 'Đã hủy', color: 'default' }
    default:
      return { label: status || 'Chưa xác định', color: 'default' }
  }
}

/**
 * Removes technical error codes, HTTP status prefixes, and parenthesized error tags
 * (e.g. "409 - ", "(403 Forbidden)", "[403]", "(400)") so doctor schedule notifications
 * display clean, user-friendly Vietnamese text without technical codes.
 */
export const cleanDoctorScheduleErrorMessage = (msg) => {
  if (!msg || typeof msg !== 'string') return ''
  return msg
    .replace(/\s*\(\d{3}(?:\s+[A-Za-z]+)?\)/gi, '') // removes '(403 Forbidden)', '(403)'
    .replace(/^\[?\d{3}\]?\s*[-:]\s*/, '') // removes '409 - ', '[403]: ', '400 - '
    .replace(/\s*\(?(?:ERR_[A-Z_]+|[A-Z_]{3,}_[A-Z_]+)\)?/g, '') // removes raw error enums
    .trim()
}

/**
 * Detects which upcoming appointments are affected by a doctor's weekly schedule configuration.
 * An appointment is affected if:
 * 1. It is active (SCHEDULED or CONFIRMED)
 * 2. It falls on a day of week where doctor is NOT active, OR
 * 3. Its appointment time range falls outside the doctor's working hours on that day.
 */
export const detectAffectedAppointments = (appointments = [], weeklySchedules = []) => {
  if (!Array.isArray(appointments) || !Array.isArray(weeklySchedules)) return []

  const activeDaysMap = new Map()
  weeklySchedules.forEach((s) => {
    if (s.active) {
      activeDaysMap.set(s.dayOfWeek, {
        start: toBackendTime(s.startTime),
        end: toBackendTime(s.endTime),
      })
    }
  })

  const daysEnum = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

  return appointments.filter((apt) => {
    if (!['SCHEDULED', 'CONFIRMED'].includes(apt.status)) return false

    const startDayjs = dayjs(apt.startTime)
    if (!startDayjs.isValid()) return false

    const dayKey = daysEnum[startDayjs.day()]
    const schedule = activeDaysMap.get(dayKey)

    // Doctor is not working on this day of week in the new weekly schedule
    if (!schedule) return true

    const apptStart = startDayjs.format('HH:mm:ss')
    const apptEnd = dayjs(apt.endTime).format('HH:mm:ss')

    // Appointment starts before working hours or ends after working hours
    return apptStart < schedule.start || apptEnd > schedule.end
  })
}


