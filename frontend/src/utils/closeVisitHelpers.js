export const CLOSE_VISIT_OUTCOMES = Object.freeze({
  EARLY_ENDED: 'EARLY_ENDED',
  CANCELLED: 'CANCELLED',
})

export const PRESET_CLOSE_REASONS = Object.freeze({
  EARLY_ENDED: [
    'Bệnh nhân xin về sớm',
    'Bệnh nhân bỏ về',
    'Chuyển viện khẩn cấp',
    'Bệnh nhân từ chối tiếp tục khám',
  ],
  CANCELLED: [
    'Đăng ký nhầm ca khám',
    'Trùng lịch hẹn',
    'Tiếp nhận sai phòng khám',
  ],
})

export const canUserCloseVisit = (user, doctorIdOrQueue) => {
  if (!user) return false

  const userRoles = (Array.isArray(user.roles) ? user.roles : [user.role || user.roles])
    .filter(Boolean)
    .map((r) => String(r).toLowerCase().replace(/^role_/, ''))

  const userPerms = (Array.isArray(user.permissions) ? user.permissions : [user.permission || user.permissions])
    .filter(Boolean)
    .map((p) => String(p).toUpperCase().replace(/^PERMISSION_/, ''))

  const isAdmin = userRoles.includes('admin')
  const hasQueueUpdateStatus = userPerms.includes('QUEUE_UPDATE_STATUS') || isAdmin

  if (!hasQueueUpdateStatus) return false
  if (isAdmin) return true

  // Kiểm tra vai trò Bác sĩ phụ trách hàng đợi
  const isDoctor = userRoles.includes('doctor')
  if (!isDoctor) return false

  let assignedDoctorId = null
  if (typeof doctorIdOrQueue === 'object' && doctorIdOrQueue !== null) {
    assignedDoctorId = doctorIdOrQueue.doctorId || doctorIdOrQueue.doctor?.id || doctorIdOrQueue.id
  } else if (typeof doctorIdOrQueue === 'string') {
    assignedDoctorId = doctorIdOrQueue
  }

  const currentUserId = user.id || user.doctorId || user.userId
  if (!assignedDoctorId || !currentUserId) return false

  return String(assignedDoctorId) === String(currentUserId)
}

export const validateCloseVisitForm = ({
  outcome,
  reason,
  currentStatus = 'IN_PROGRESS',
  isRecordSigned = false,
  hasDispensedPrescriptions = false,
} = {}) => {
  if (isRecordSigned) {
    return {
      valid: false,
      field: 'isRecordSigned',
      message: 'Bệnh án đã được ký số / khóa nội dung theo quy định QTN-18, không thể kết thúc sớm hoặc hủy lượt khám. Vui lòng lập bản đính chính.',
    }
  }

  if (currentStatus && currentStatus !== 'IN_PROGRESS') {
    return {
      valid: false,
      field: 'currentStatus',
      message: 'Lượt khám này không còn ở trạng thái đang khám (IN_PROGRESS), không thể đóng ca.',
    }
  }

  if (!outcome || (outcome !== CLOSE_VISIT_OUTCOMES.EARLY_ENDED && outcome !== CLOSE_VISIT_OUTCOMES.CANCELLED)) {
    return {
      valid: false,
      field: 'outcome',
      message: 'Vui lòng chọn loại kết quả đóng ca khám (Kết thúc sớm hoặc Hủy lượt khám).',
    }
  }

  const trimmedReason = typeof reason === 'string' ? reason.trim() : ''
  if (!trimmedReason) {
    return {
      valid: false,
      field: 'reason',
      message: 'Lý do đóng lượt khám là bắt buộc, không được để trống.',
    }
  }

  if (trimmedReason.length > 500) {
    return {
      valid: false,
      field: 'reason',
      message: 'Lý do đóng lượt khám không được vượt quá 500 ký tự.',
    }
  }

  if (outcome === CLOSE_VISIT_OUTCOMES.CANCELLED && hasDispensedPrescriptions) {
    return {
      valid: false,
      field: 'outcome',
      message: 'Đơn thuốc của lượt khám này đã được phát tại quầy dược, không thể hủy. Vui lòng chọn "Kết thúc sớm" thay vì "Hủy lượt khám".',
      suggestion: CLOSE_VISIT_OUTCOMES.EARLY_ENDED,
    }
  }

  return {
    valid: true,
    trimmedReason,
  }
}

export const mapCloseVisitErrorMessage = (error, outcome) => {
  const code = error?.response?.data?.code || error?.apiError?.code
  const status = error?.response?.status || error?.apiError?.status
  const backendMsg = error?.response?.data?.message || error?.apiError?.message

  if (code === 'VISIT_INVALID_STATUS') {
    return {
      code: 'VISIT_INVALID_STATUS',
      message: 'Lượt khám này không còn ở trạng thái đang khám, không thể đóng ca. Vui lòng tải lại danh sách.',
      shouldRefreshQueue: true,
    }
  }

  if (code === 'MEDICAL_RECORD_LOCKED' || code === 'MEDICAL_RECORD_ALREADY_LOCKED') {
    return {
      code: 'MEDICAL_RECORD_LOCKED',
      message: 'Bệnh án đã được ký, không thể hủy lượt khám. Vui lòng lập bản đính chính theo quy định (QTN-18).',
    }
  }

  if (code === 'PRESCRIPTION_ALREADY_DISPENSED') {
    return {
      code: 'PRESCRIPTION_ALREADY_DISPENSED',
      message: 'Đơn thuốc của lượt khám này đã được phát, không thể hủy. Vui lòng chọn "Kết thúc sớm" thay vì "Hủy lượt khám".',
      suggestEarlyEnded: true,
    }
  }

  if (code === 'UNAUTHORIZED_QUEUE_OPERATION' || status === 403) {
    return {
      code: 'UNAUTHORIZED_QUEUE_OPERATION',
      message: 'Bạn không có quyền đóng lượt khám này (không thuộc hàng đợi bạn phụ trách).',
    }
  }

  if (code === 'VALIDATION_FAILED' || status === 400) {
    return {
      code: 'VALIDATION_FAILED',
      message: backendMsg || 'Dữ liệu không hợp lệ. Lý do đóng không được vượt quá 500 ký tự.',
    }
  }

  if (code === 'RESOURCE_NOT_FOUND' || status === 404) {
    return {
      code: 'RESOURCE_NOT_FOUND',
      message:
        backendMsg === 'Resource not found.'
          ? 'Không tìm thấy tài nguyên trên hệ thống (404). Nếu vừa cập nhật Backend, vui lòng khởi động lại tiến trình Spring Boot.'
          : (backendMsg || 'Không tìm thấy lượt khám tương ứng trên hệ thống (404).'),
    }
  }

  return {
    code: 'UNKNOWN_ERROR',
    message: backendMsg || (outcome === CLOSE_VISIT_OUTCOMES.EARLY_ENDED
      ? 'Không thể kết thúc sớm lượt khám. Vui lòng thử lại.'
      : 'Không thể hủy lượt khám. Vui lòng thử lại.'),
  }
}
