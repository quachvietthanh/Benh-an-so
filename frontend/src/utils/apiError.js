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
  DIAGNOSIS_CATALOG_CODE_ALREADY_EXISTS:
    'Mã bệnh đã tồn tại trong danh mục hệ thống.',
  DIAGNOSIS_CATALOG_CODE_EXISTS:
    'Mã bệnh đã tồn tại trong danh mục hệ thống.',
  DIAGNOSIS_CATALOG_NAME_ALREADY_EXISTS:
    'Tên bệnh đã tồn tại trong danh mục hệ thống.',
  DIAGNOSIS_CATALOG_NOT_FOUND:
    'Không tìm thấy thông tin mã bệnh trong danh mục.',
  DIAGNOSIS_CATALOG_IN_USE:
    'Mã bệnh đang được sử dụng trong hồ sơ bệnh án, không thể xóa.',
  SERVICE_CATALOG_NOT_FOUND:
    'Không tìm thấy thông tin dịch vụ trong hệ thống.',
  MEDICAL_RECORD_TEMPLATE_CHANGE_WITH_CONTENT:
    'Bệnh án đã có nội dung khám, không thể đổi sang mẫu khác để bảo toàn dữ liệu. Vui lòng tiếp tục với mẫu hiện tại.',
  MEDICAL_RECORD_TEMPLATE_SPECIALTY_MISMATCH:
    'Mẫu bệnh án được chọn không khớp với chuyên khoa của lượt khám.',
  MEDICAL_RECORD_TEMPLATE_INACTIVE:
    'Mẫu bệnh án này đang tạm ngưng hoạt động.',
  MEDICAL_RECORD_TEMPLATE_NOT_FOUND:
    'Không tìm thấy mẫu bệnh án trong hệ thống.',
  MEDICAL_RECORD_TEMPLATE_DEFAULT_NOT_CONFIGURED:
    'Chuyên khoa chưa được cấu hình mẫu mặc định hợp lệ.',
  REQUEST_TIMEOUT:
    'Hệ thống đang bận hoặc phản hồi chậm. Dữ liệu đang được đồng bộ.',
}

export const normalizeApiError = (error, fallbackMessage = DEFAULT_MESSAGE) => {
  const response = error?.response
  const body = response?.data
  const responseMessage = typeof body === 'string' ? body : body?.message
  const details = body && typeof body.details === 'object' && !Array.isArray(body.details)
    ? body.details
    : {}
  const fields = details.fields && typeof details.fields === 'object' ? details.fields : {}

  const isTimeout =
    error?.code === 'ECONNABORTED' ||
    (typeof error?.message === 'string' && error.message.toLowerCase().includes('timeout'))

  return {
    status: body?.status || response?.status || 0,
    code: body?.code || (isTimeout ? 'REQUEST_TIMEOUT' : 'NETWORK_ERROR'),
    message: isTimeout
      ? 'Hệ thống đang bận hoặc phản hồi chậm. Dữ liệu đang được đồng bộ.'
      : (responseMessage || error?.message || fallbackMessage),
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
