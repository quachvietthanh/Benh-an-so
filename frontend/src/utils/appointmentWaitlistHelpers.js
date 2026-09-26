/**
 * Helper utilities for Appointment Waitlist (NCL-03-CN-012 / QTN-04)
 */

export const TIME_PREFERENCES = {
  ANYTIME: 'ANYTIME',
  MORNING: 'MORNING',
  AFTERNOON: 'AFTERNOON',
}

export const TIME_PREFERENCE_LABELS = {
  ANYTIME: 'Bất kỳ lúc nào',
  MORNING: 'Buổi sáng',
  AFTERNOON: 'Buổi chiều',
}

export const TIME_PREFERENCE_OPTIONS = [
  { value: 'ANYTIME', label: 'Bất kỳ lúc nào' },
  { value: 'MORNING', label: 'Buổi sáng' },
  { value: 'AFTERNOON', label: 'Buổi chiều' },
]

export const WAITLIST_STATUSES = {
  WAITING: 'WAITING',
  SCHEDULED: 'SCHEDULED',
  CANCELLED: 'CANCELLED',
  EXPIRED: 'EXPIRED',
}

/**
 * Returns tag metadata (label and color) for a waitlist entry status.
 * @param {string} status
 * @returns {{ label: string, color: string }}
 */
export const getWaitlistStatusTag = (status) => {
  switch (status) {
    case 'WAITING':
      return { label: 'Đang chờ', color: 'blue' }
    case 'SCHEDULED':
      return { label: 'Đã đặt lịch', color: 'green' }
    case 'CANCELLED':
      return { label: 'Đã hủy', color: 'default' }
    case 'EXPIRED':
      return { label: 'Hết hạn', color: 'gray' }
    default:
      return { label: status || 'Không xác định', color: 'default' }
  }
}

/**
 * Maps API / Backend errors to human-readable Vietnamese messages.
 * Đặc biệt xử lý DoctorHasAvailableSlotsException hướng dẫn đặt lịch trực tiếp.
 * @param {any} error
 * @returns {string}
 */
export const mapWaitlistErrorMessage = (error) => {
  if (!error) return 'Có lỗi xảy ra khi xử lý danh sách chờ.'

  if (typeof error === 'string') {
    if (error.includes('DoctorHasAvailableSlotsException') || error.includes('khung giờ trống')) {
      return 'Bác sĩ vẫn còn khung giờ trống trong ngày này. Vui lòng đặt lịch trực tiếp thay vì thêm vào danh sách chờ.'
    }
    if (error.includes('PatientAlreadyInWaitlistException') || error.includes('danh sách chờ của bác sĩ')) {
      return 'Bệnh nhân đã có tên trong danh sách chờ của bác sĩ vào ngày này.'
    }
    if (error.includes('DoctorNotWorkingException') || error.includes('không có lịch làm việc')) {
      return 'Bác sĩ không có lịch làm việc vào ngày này.'
    }
    if (error.includes('quá khứ')) {
      return 'Ngày mong muốn khám không được ở trong quá khứ.'
    }
    return error
  }

  const status = error.response?.status
  const data = error.response?.data
  const rawMessage = data?.message || error.message || ''
  const errorCode = data?.code || ''

  // 1. DoctorHasAvailableSlotsException (400)
  if (
    errorCode === 'DOCTOR_HAS_AVAILABLE_SLOTS' ||
    rawMessage.includes('DoctorHasAvailableSlotsException') ||
    rawMessage.includes('khung giờ trống')
  ) {
    return 'Bác sĩ vẫn còn khung giờ trống trong ngày này. Vui lòng đặt lịch trực tiếp thay vì thêm vào danh sách chờ.'
  }

  // 2. PatientAlreadyInWaitlistException (409)
  if (
    status === 409 ||
    errorCode === 'PATIENT_ALREADY_IN_WAITLIST' ||
    rawMessage.includes('PatientAlreadyInWaitlistException') ||
    rawMessage.includes('đã có tên trong danh sách chờ')
  ) {
    return 'Bệnh nhân đã có tên trong danh sách chờ của bác sĩ vào ngày này.'
  }

  // 3. DoctorNotWorkingException (400)
  if (
    errorCode === 'DOCTOR_NOT_WORKING' ||
    rawMessage.includes('DoctorNotWorkingException') ||
    rawMessage.includes('không có lịch làm việc')
  ) {
    return 'Bác sĩ không có lịch làm việc vào ngày này.'
  }

  // 4. Past date
  if (rawMessage.includes('quá khứ')) {
    return 'Ngày mong muốn khám không được ở trong quá khứ.'
  }

  // 5. 403 Forbidden
  if (status === 403) {
    return 'Bạn không có quyền thực hiện thao tác trên danh sách chờ.'
  }

  // 6. 404 Not Found
  if (status === 404) {
    return rawMessage || 'Không tìm thấy dữ liệu yêu cầu.'
  }

  return rawMessage || 'Có lỗi xảy ra khi xử lý danh sách chờ. Vui lòng thử lại.'
}

/**
 * Validates the Add To Waitlist form.
 * @param {string} patientId
 * @param {string} doctorId
 * @param {string|Date|any} desiredDate (format 'YYYY-MM-DD' or string/Date)
 * @param {string} [note]
 * @param {string} [todayOverride] optional 'YYYY-MM-DD' for deterministic testing
 * @returns {{ isValid: boolean, errors: Record<string, string> }}
 */
export const validateAddToWaitlistForm = (patientId, doctorId, desiredDate, note, todayOverride) => {
  const errors = {}

  if (!patientId || String(patientId).trim() === '') {
    errors.patientId = 'Vui lòng chọn bệnh nhân!'
  }

  if (!doctorId || String(doctorId).trim() === '') {
    errors.doctorId = 'Vui lòng chọn bác sĩ!'
  }

  if (!desiredDate) {
    errors.desiredDate = 'Vui lòng chọn ngày mong muốn khám!'
  } else {
    // Normalize date to YYYY-MM-DD
    let dateStr = ''
    if (typeof desiredDate === 'string') {
      dateStr = desiredDate.slice(0, 10)
    } else if (desiredDate && typeof desiredDate.format === 'function') {
      dateStr = desiredDate.format('YYYY-MM-DD')
    } else if (desiredDate instanceof Date) {
      dateStr = desiredDate.toISOString().slice(0, 10)
    }

    const todayStr = todayOverride || new Date().toISOString().slice(0, 10)
    if (dateStr && dateStr < todayStr) {
      errors.desiredDate = 'Ngày mong muốn khám không được ở trong quá khứ!'
    }
  }

  if (note && typeof note === 'string' && note.length > 500) {
    errors.note = 'Ghi chú không được vượt quá 500 ký tự!'
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  }
}

/**
 * Parses the response from getSuggestion.
 * Handles 204 No Content gracefully as null (not an error).
 * @param {any} response
 * @returns {any|null}
 */
export const extractWaitlistSuggestion = (response) => {
  if (!response) return null
  if (response.status === 204) return null
  if (!response.data || response.data === '') return null
  return response.data
}
