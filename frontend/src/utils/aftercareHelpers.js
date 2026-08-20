import dayjs from 'dayjs'

export const REMINDER_TYPES = [
  { value: 'REVISIT', label: 'Tái khám' },
  { value: 'MEDICATION_CHECK', label: 'Kiểm tra dùng thuốc' },
  { value: 'GENERAL', label: 'Nhắc nhở chung' },
]

export const REMINDER_STATUSES = [
  { value: 'PENDING', label: 'Chờ nhắc', color: 'processing' },
  { value: 'SENT', label: 'Đã gửi nhắc', color: 'warning' },
  { value: 'COMPLETED', label: 'Hoàn thành', color: 'success' },
  { value: 'CANCELLED', label: 'Đã hủy', color: 'default' },
]

export const CONTACT_CHANNELS = [
  { value: 'PHONE', label: 'Điện thoại' },
  { value: 'SMS', label: 'SMS' },
  { value: 'IN_PERSON', label: 'Trực tiếp' },
  { value: 'ZALO', label: 'Zalo' },
]

export const PATIENT_CONDITIONS = [
  { value: 'STABLE', label: 'Ổn định', color: 'success' },
  { value: 'RECOVERING', label: 'Đang hồi phục', color: 'processing' },
  { value: 'COMPLICATIONS', label: 'Có biến chứng', color: 'error' },
  { value: 'NEEDS_REVISIT', label: 'Cần tái khám', color: 'warning' },
]

export const CONTACT_OUTCOMES = [
  { value: 'REACHED', label: 'Liên lạc thành công', color: 'success' },
  { value: 'UNREACHABLE', label: 'Không liên lạc được', color: 'warning' },
  { value: 'DECLINED', label: 'Từ chối trao đổi', color: 'default' },
]

const byValue = (items, value) => items.find((item) => item.value === value)

export const getReminderTypeMeta = (value) => (
  byValue(REMINDER_TYPES, value) || { value, label: value || '—' }
)

export const getReminderStatusMeta = (value) => (
  byValue(REMINDER_STATUSES, value) || { value, label: value || '—', color: 'default' }
)

export const getContactChannelMeta = (value) => (
  byValue(CONTACT_CHANNELS, value) || { value, label: value || '—' }
)

export const getPatientConditionMeta = (value) => (
  byValue(PATIENT_CONDITIONS, value) || { value, label: value || '—', color: 'default' }
)

export const getContactOutcomeMeta = (value) => (
  byValue(CONTACT_OUTCOMES, value) || { value, label: value || '—', color: 'default' }
)

export const normalizePage = (data, fallbackPage = 0, fallbackSize = 20) => {
  if (Array.isArray(data)) {
    return {
      content: data,
      totalElements: data.length,
      number: fallbackPage,
      size: fallbackSize,
      totalPages: data.length ? 1 : 0,
    }
  }

  if (!data || !Array.isArray(data.content)) {
    throw new Error('Dữ liệu phân trang từ Backend không đúng định dạng.')
  }

  const content = data.content
  return {
    content,
    totalElements: Number.isFinite(data?.totalElements) ? data.totalElements : content.length,
    number: Number.isFinite(data?.number) ? data.number : fallbackPage,
    size: Number.isFinite(data?.size) ? data.size : fallbackSize,
    totalPages: Number.isFinite(data?.totalPages)
      ? data.totalPages
      : (content.length ? 1 : 0),
  }
}

export const isUuid = (value) => (
  typeof value === 'string'
  && /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(value)
)

export const shortId = (value) => {
  if (!value) return '—'
  const text = String(value)
  return text.length > 12 ? `${text.slice(0, 8)}…` : text
}

const getPickerParts = (value, endOfDay) => {
  const parsed = dayjs.isDayjs(value) ? value : dayjs(value)
  if (!parsed.isValid()) return null
  return {
    year: parsed.year(),
    month: parsed.month(),
    date: parsed.date(),
    hour: endOfDay ? 23 : parsed.hour(),
    minute: endOfDay ? 59 : parsed.minute(),
    second: endOfDay ? 59 : parsed.second(),
    millisecond: endOfDay ? 999 : parsed.millisecond(),
  }
}

// DatePicker represents the wall-clock value selected by the user. Vietnam has
// a fixed UTC+07:00 offset, so convert those components exactly once to Instant.
export const vietnamDateTimeToIso = (value, { endOfDay = false } = {}) => {
  const parts = getPickerParts(value, endOfDay)
  if (!parts) return null
  const utcMillis = Date.UTC(
    parts.year,
    parts.month,
    parts.date,
    parts.hour - 7,
    parts.minute,
    parts.second,
    parts.millisecond,
  )
  return new Date(utcMillis).toISOString()
}

export const vietnamNowForPicker = (now = new Date()) => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(now).reduce((result, part) => {
    result[part.type] = part.value
    return result
  }, {})
  return dayjs(`${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}:${parts.second}`)
}

export const formatVietnamDate = (value) => {
  if (!value) return '—'
  if (/^\d{4}-\d{2}-\d{2}$/.test(String(value))) {
    const [year, month, date] = String(value).split('-')
    return `${date}/${month}/${year}`
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(parsed)
}

export const getVietnamDateKey = (value) => {
  if (!value) return null
  if (/^\d{4}-\d{2}-\d{2}$/.test(String(value))) return String(value)
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return null
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(parsed).reduce((result, part) => {
    result[part.type] = part.value
    return result
  }, {})
  return `${parts.year}-${parts.month}-${parts.day}`
}

const visitSortTime = (visit) => {
  const value = visit?.completedAt || visit?.checkedInAt || visit?.queueDate
  const timestamp = value ? new Date(value).getTime() : Number.NaN
  return Number.isNaN(timestamp) ? 0 : timestamp
}

export const selectTodayCompletedVisits = (queueItems, todayKey) => {
  if (!Array.isArray(queueItems)) {
    throw new Error('Dữ liệu lượt khám hôm nay từ Backend không đúng định dạng.')
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(todayKey || ''))) {
    throw new Error('Ngày lấy lượt khám hôm nay không hợp lệ.')
  }

  return queueItems
    .filter((item) => (
      item?.status === 'COMPLETED'
      && item?.queueDate === todayKey
      && isUuid(item?.patientId)
      && isUuid(item?.visitId)
    ))
    .map((item) => ({
      ...item,
      id: item.visitId,
      visitAt: item.checkedInAt || item.queueDate,
      doctorInstructions: item.doctorInstructions || item.notes || 'Dặn dò tái khám và theo dõi sau khám',
    }))
    .sort((left, right) => visitSortTime(right) - visitSortTime(left))
}

export const buildTodayPatients = (todayVisits, patientDetails = []) => {
  if (!Array.isArray(todayVisits) || !Array.isArray(patientDetails)) {
    throw new Error('Dữ liệu bệnh nhân hôm nay không đúng định dạng.')
  }

  const detailsById = new Map(
    patientDetails
      .filter((patient) => isUuid(patient?.id))
      .map((patient) => [String(patient.id), patient]),
  )
  const seenPatientIds = new Set()

  return todayVisits.reduce((patients, visit) => {
    const patientId = visit?.patientId
    if (!isUuid(patientId) || seenPatientIds.has(patientId)) return patients

    seenPatientIds.add(patientId)
    const detail = detailsById.get(String(patientId)) || {}
    patients.push({
      id: patientId,
      patientCode: detail.patientCode || null,
      fullName: detail.fullName || visit.patientName || null,
      phone: detail.phone || null,
      latestVisitAt: visit.completedAt || visit.checkedInAt || visit.queueDate,
    })
    return patients
  }, [])
}

export const getTodayVisitsForPatient = (todayVisits, patientId) => (
  (Array.isArray(todayVisits) ? todayVisits : [])
    .filter((visit) => isUuid(visit?.visitId) && String(visit.patientId) === String(patientId))
)

export const isTodayVisitSelectionValid = (todayVisits, patientId, visitId) => (
  isUuid(patientId)
  && isUuid(visitId)
  && getTodayVisitsForPatient(todayVisits, patientId)
    .some((visit) => String(visit.visitId) === String(visitId))
)

export const formatVietnamDateTime = (value) => {
  if (!value) return '—'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(parsed)
}

export const buildReminderPayload = (values) => ({
  patientId: values.patientId,
  visitId: values.visitId,
  appointmentId: null,
  followUpDate: values.followUpDate.format('YYYY-MM-DD'),
  remindAt: vietnamDateTimeToIso(values.remindAt),
  reminderType: values.reminderType,
  notes: values.notes?.trim() || null,
})

export const buildCareLogPayload = (values) => ({
  patientId: values.patientId,
  reminderId: values.reminderId || null,
  visitId: values.visitId || null,
  contactChannel: values.contactChannel,
  contactedAt: vietnamDateTimeToIso(values.contactedAt),
  patientCondition: values.patientCondition,
  careNotes: values.careNotes.trim(),
  contactOutcome: values.contactOutcome,
})

export const getErrorStatus = (error) => error?.response?.status || null

export const isDoctorInstructionError = (error) => {
  const message = String(error?.response?.data?.message || error?.message || '')
  return /no follow-up indication from doctor/i.test(message)
}

export const getAftercareErrorMessage = (error, fallback = 'Không thể tải dữ liệu.') => {
  if (isDoctorInstructionError(error)) {
    return 'Không thể tạo lịch nhắc vì lượt khám chưa có dặn dò/chỉ định sau khám của bác sĩ.'
  }

  const status = getErrorStatus(error)
  const backendMessage = error?.response?.data?.message
  if (status === 400) return backendMessage || 'Dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại.'
  if (status === 401) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
  if (status === 403) return 'Bạn không có quyền thực hiện chức năng này.'
  if (status === 404) return backendMessage || 'Không tìm thấy dữ liệu hoặc API tương ứng.'
  if (status === 409) return backendMessage || 'Dữ liệu đã thay đổi trạng thái. Vui lòng làm mới và thử lại.'
  if (status >= 500) return 'Backend đang gặp lỗi. Vui lòng thử lại sau.'
  return backendMessage || error?.message || fallback
}

const normalizeAuthority = (value) => String(value || '')
  .toUpperCase()
  .replace(/^PERMISSION_/, '')

export const hasAftercarePermission = (user, permission) => {
  const userRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const isAdmin = userRoles.includes('admin')
  const isReceptionist = userRoles.includes('receptionist')

  if (isAdmin || isReceptionist) {
    if (permission === 'FOLLOW_UP_REMINDER_READ' || permission === 'CARE_LOG_READ' || permission === 'MEDICAL_RECORD_READ') {
      return true
    }
  }

  const permissions = Array.isArray(user?.permissions) ? user.permissions : []
  return permissions.some((item) => normalizeAuthority(item) === permission)
}

export const getAllowedStatusActions = (status) => {
  if (status === 'PENDING') return ['SENT', 'COMPLETED', 'CANCELLED']
  if (status === 'SENT') return ['COMPLETED', 'CANCELLED']
  return []
}
