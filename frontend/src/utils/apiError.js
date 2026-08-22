const DEFAULT_MESSAGE = 'Thao tác không thành công. Vui lòng thử lại.'

export const DOMAIN_ERROR_MESSAGES = {
  MEDICAL_RECORD_IN_RETENTION_PERIOD:
    'Hồ sơ đang trong thời hạn lưu trữ bắt buộc, không thể xóa. Vui lòng dùng chức năng lưu trữ (Archive) nếu cần ẩn hồ sơ khỏi danh sách hoạt động.',
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

export const isAccessDeniedApiError = (apiError) =>
  apiError?.code === 'ACCESS_DENIED' || apiError?.status === 403
