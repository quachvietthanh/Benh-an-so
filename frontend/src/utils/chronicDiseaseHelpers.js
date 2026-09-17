/**
 * Helper utilities for Patient Chronic Disease feature (NCL-02-CN-009)
 */

export const MIN_YEAR_DETECTED = 1900

export const getCurrentYear = () => {
  return new Date().getFullYear()
}

/**
 * Validate yearDetected field:
 * Optional, but if provided must be an integer between 1900 and current year.
 *
 * @param {number|string|null|undefined} year
 * @param {number} [maxYear]
 * @returns {{ valid: boolean, error?: string, value?: number|null }}
 */
export const validateYearDetected = (year, maxYear = getCurrentYear()) => {
  if (year === null || year === undefined || year === '') {
    return { valid: true, value: null }
  }

  const num = Number(year)
  if (!Number.isInteger(num)) {
    return { valid: false, error: 'Năm phát hiện phải là số nguyên hợp lệ.' }
  }

  if (num < MIN_YEAR_DETECTED) {
    return {
      valid: false,
      error: `Năm phát hiện không được nhỏ hơn ${MIN_YEAR_DETECTED}.`,
    }
  }

  if (num > maxYear) {
    return {
      valid: false,
      error: `Năm phát hiện không được lớn hơn năm hiện tại (${maxYear}).`,
    }
  }

  return { valid: true, value: num }
}

/**
 * Map error codes from Backend REST API to user-friendly Vietnamese messages.
 * Exact match according to API Contract and requirements.
 *
 * @param {any} error
 * @param {string} [defaultMessage]
 * @returns {string}
 */
export const mapChronicDiseaseErrorMessage = (
  error,
  defaultMessage = 'Không thể lưu tiền sử bệnh mạn tính. Vui lòng thử lại.',
) => {
  const code = error?.response?.data?.code || error?.code || ''
  const status = error?.response?.status || error?.status

  if (code === 'DIAGNOSIS_CATALOG_NOT_FOUND') {
    return 'Mã bệnh không tồn tại trong danh mục chẩn đoán.'
  }

  if (code === 'PATIENT_CHRONIC_DISEASE_ALREADY_EXISTS') {
    return 'Bệnh nhân đã được ghi nhận bệnh này từ trước và vẫn đang trong danh sách tiền sử hiện tại.'
  }

  if (code === 'VALIDATION_FAILED') {
    return 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại năm phát hiện hoặc mã bệnh đã chọn.'
  }

  if (code === 'PATIENT_NOT_FOUND') {
    return 'Không tìm thấy hồ sơ bệnh nhân trong hệ thống.'
  }

  if (code === 'PATIENT_CHRONIC_DISEASE_NOT_FOUND') {
    return 'Không tìm thấy bản ghi tiền sử bệnh mạn tính hoặc đã bị xóa trước đó.'
  }

  if (code === 'RESOURCE_NOT_FOUND' || status === 404) {
    const backendMsg = error?.response?.data?.message
    if (backendMsg === 'Resource not found.') {
      return 'Không tìm thấy endpoint trên hệ thống (404). Vui lòng kiểm tra và khởi động lại tiến trình Backend Spring Boot để nạp Controller mới.'
    }
    return backendMsg || 'Không tìm thấy tài nguyên yêu cầu trên hệ thống (404).'
  }

  if (status === 403 || code === 'FORBIDDEN') {
    return 'Bạn không có quyền ghi nhận tiền sử bệnh mạn tính.'
  }

  if (error?.response?.data?.message && typeof error.response.data.message === 'string') {
    return error.response.data.message
  }

  return defaultMessage
}

/**
 * Check if the user has permission to write/delete chronic disease records.
 * Required role: DOCTOR or ADMIN, or permission: PATIENT_CHRONIC_DISEASE_WRITE.
 *
 * @param {any} user
 * @returns {boolean}
 */
export const hasChronicDiseaseWritePermission = (user) => {
  if (!user) return false

  const permissions = (user.permissions || []).map((p) =>
    String(p || '').toUpperCase().replace(/^PERMISSION_/, ''),
  )
  if (permissions.includes('PATIENT_CHRONIC_DISEASE_WRITE')) {
    return true
  }

  const roles = (user.roles || [user.role || ''])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)

  return roles.includes('doctor') || roles.includes('admin')
}

/**
 * Check if the user has permission to read chronic disease records.
 * Required role: DOCTOR, ADMIN, PHARMACIST, MANAGER, or permission: PATIENT_CHRONIC_DISEASE_READ.
 *
 * @param {any} user
 * @returns {boolean}
 */
export const hasChronicDiseaseReadPermission = (user) => {
  if (!user) return false

  const permissions = (user.permissions || []).map((p) =>
    String(p || '').toUpperCase().replace(/^PERMISSION_/, ''),
  )
  if (
    permissions.includes('PATIENT_CHRONIC_DISEASE_READ') ||
    permissions.includes('PATIENT_CHRONIC_DISEASE_WRITE')
  ) {
    return true
  }

  const roles = (user.roles || [user.role || ''])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)

  return (
    roles.includes('doctor') ||
    roles.includes('admin') ||
    roles.includes('pharmacist') ||
    roles.includes('manager') ||
    roles.includes('clinic_manager')
  )
}

/**
 * Format chronic disease label for UI display.
 *
 * @param {{ diagnosisCode?: string, diagnosisName?: string }} item
 * @returns {string}
 */
export const formatChronicDiseaseLabel = (item) => {
  if (!item) return ''
  const code = item.diagnosisCode || item.code || ''
  const name = item.diagnosisName || item.name || ''
  if (code && name) return `${code} - ${name}`
  return code || name || 'Chưa xác định'
}

/**
 * Format detected year with fallback.
 *
 * @param {number|null|undefined} year
 * @returns {string}
 */
export const formatYearDetected = (year) => {
  if (year === null || year === undefined || year === '') {
    return 'Không rõ'
  }
  return String(year)
}
