const DEFAULT_MESSAGE = 'Thao tác không thành công. Vui lòng thử lại.'

export const DOMAIN_ERROR_MESSAGES = {
  MEDICAL_RECORD_IN_RETENTION_PERIOD:
    'Hồ sơ đang trong thời hạn lưu trữ bắt buộc, không thể xóa. Vui lòng dùng chức năng lưu trữ (Archive) nếu cần ẩn hồ sơ khỏi danh sách hoạt động.',
  MEDICAL_RECORD_MISSING_DIAGNOSIS:
    'Bệnh án chưa có chẩn đoán. Vui lòng ghi nhận chẩn đoán ICD-10 trước khi ký xác nhận.',
  MEDICAL_RECORD_UNAUTHORIZED_SIGNER:
    'Chỉ bác sĩ phụ trách lượt khám mới có quyền ký xác nhận bệnh án này.',
  MEDICAL_RECORD_LOCKED:
    'Hồ sơ bệnh án đã được ký hoặc khóa, không thể chỉnh sửa trực tiếp.',
  MEDICAL_RECORD_ALREADY_LOCKED:
    'Hồ sơ bệnh án đã được ký hoặc khóa, không thể chỉnh sửa trực tiếp.',
  MEDICAL_RECORD_NOT_SIGNED:
    'Bệnh án chưa được ký xác nhận. Vui lòng ký bệnh án trước khi hoàn tất hoặc khóa.',
  MEDICAL_RECORD_INVALID_VISIT:
    'Lượt khám không ở trạng thái hợp lệ để ký hoặc xử lý bệnh án.',
}

export const normalizeApiError = (error, fallbackMessage = DEFAULT_MESSAGE) => {
  const response = error?.response
  const body = response?.data
  const responseMessage = typeof body === 'string' ? body : body?.message
  const details = body && typeof body.details === 'object' && !Array.isArray(body.details)
    ? body.details
    : {}
  const fields = details.fields && typeof details.fields === 'object' ? details.fields : {}

  return {
    status: body?.status || response?.status || 0,
    code: body?.code || 'NETWORK_ERROR',
    message: responseMessage || error?.message || fallbackMessage,
    details,
    fields,
    firstFieldError: Object.values(fields)[0] || null,
  }
}

export const getApiErrorMessage = (error, fallbackMessage) => {
  const normalized = normalizeApiError(error, fallbackMessage)
  if (normalized.code && DOMAIN_ERROR_MESSAGES[normalized.code]) {
    return DOMAIN_ERROR_MESSAGES[normalized.code]
  }
  return normalized.firstFieldError || normalized.message
}

export const isAccessDeniedApiError = (error) => {
  if (!error) return false
  const status = error?.status || error?.response?.status || error?.apiError?.status
  const code = error?.code || error?.response?.data?.code || error?.apiError?.code
  return status === 403 || code === 'ACCESS_DENIED'
}
