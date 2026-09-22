/**
 * Tiện ích hỗ trợ nghiệp vụ Gọi lại và Tạm hoãn bệnh nhân vắng trong hàng đợi (NCL-03-CN-009).
 * Quy tắc: QTN-08
 * Tiêu chí chấp thuận: TC-01, TC-02, TC-03, TC-04
 */

export const PRESET_DEFER_REASONS = [
  'Bệnh nhân chưa có mặt khi gọi số thứ tự',
  'Bệnh nhân đi làm xét nghiệm cận lâm sàng',
  'Bệnh nhân có việc bận tạm thời ngoài quầy',
  'Chưa kịp chuẩn bị hồ sơ/giấy tờ',
  'Khác',
]

export function validateDeferReason(reason) {
  if (typeof reason !== 'string') {
    return { valid: false, error: 'Lý do tạm hoãn không hợp lệ.', reason: '' }
  }
  const trimmed = reason.trim()
  if (!trimmed) {
    return { valid: false, error: 'Vui lòng nhập lý do tạm hoãn bệnh nhân.', reason: '' }
  }
  if (trimmed.length > 255) {
    return { valid: false, error: 'Lý do tạm hoãn không được vượt quá 255 ký tự.', reason: trimmed.slice(0, 255) }
  }
  return { valid: true, error: null, reason: trimmed }
}

export const QUEUE_ACTION_META = {
  CHECK_IN: { label: 'Tiếp nhận vào hàng đợi', color: 'blue' },
  CALL: { label: 'Gọi vào khám', color: 'blue' },
  CALL_NEXT: { label: 'Gọi vào khám', color: 'blue' },
  SKIP: { label: 'Tạm hoãn lượt khám', color: 'orange' },
  RE_QUEUE: { label: 'Đưa lại vào hàng đợi', color: 'cyan' },
  COMPLETE: { label: 'Hoàn thành khám', color: 'green' },
  CANCEL: { label: 'Hủy lượt khám', color: 'red' },
  WAIT_FOR_RESULT: { label: 'Chờ kết quả CĐLS', color: 'purple' },
  RESUME: { label: 'Tiếp tục khám', color: 'geekblue' },
  PRIORITIZED: { label: 'Đánh dấu ưu tiên', color: 'volcano' },
}

export const QUEUE_ITEM_STATUS_VN = {
  WAITING: { label: 'Đang chờ khám', color: 'blue' },
  IN_PROGRESS: { label: 'Đang khám / Đã gọi', color: 'green' },
  SKIPPED: { label: 'Tạm hoãn (Vắng mặt)', color: 'orange' },
  WAITING_FOR_RESULT: { label: 'Chờ kết quả CĐLS', color: 'purple' },
  COMPLETED: { label: 'Hoàn thành', color: 'default' },
  CANCELLED: { label: 'Đã hủy', color: 'error' },
}

/**
 * Định dạng tên hành động trong lịch sử hàng đợi sang tiếng Việt thuần (TC-04)
 */
export function formatQueueActionVi(action) {
  if (!action) return 'Thao tác không xác định'
  const key = String(action).toUpperCase()
  return QUEUE_ACTION_META[key]?.label || key
}

/**
 * Định dạng trạng thái lượt khám sang tiếng Việt thuần
 */
export function formatQueueStatusVi(status) {
  if (!status) return 'Không xác định'
  const key = String(status).toUpperCase()
  return QUEUE_ITEM_STATUS_VN[key]?.label || key
}

/**
 * Kiểm tra điều kiện tạm hoãn bệnh nhân (TC-01, TC-03)
 * Chỉ cho phép tạm hoãn khi lượt khám đang ở trạng thái IN_PROGRESS (đang ở lượt được gọi).
 * Từ chối khi bệnh nhân đã COMPLETED hoặc không còn trong hàng đợi (TC-03).
 */
export function evaluateDeferAction(item, permissions = {}) {
  if (!item) {
    return { allowed: false, message: 'Không tìm thấy thông tin lượt khám.' }
  }

  const status = String(item.status || '').toUpperCase()

  if (status === 'COMPLETED') {
    return {
      allowed: false,
      message: 'Hệ thống từ chối: Bệnh nhân đã hoàn tất khám, không còn trong hàng đợi.',
    }
  }

  if (status === 'CANCELLED') {
    return {
      allowed: false,
      message: 'Hệ thống từ chối: Lượt khám đã bị hủy trước đó.',
    }
  }

  if (status !== 'IN_PROGRESS') {
    return {
      allowed: false,
      message: 'Chỉ có thể tạm hoãn bệnh nhân đang ở lượt được gọi khám.',
    }
  }

  const hasPermission = Boolean(
    permissions.canSkip ||
    permissions.isAdmin ||
    permissions.isReceptionist ||
    permissions.isDoctor
  )

  if (!hasPermission) {
    return {
      allowed: false,
      message: 'Bạn không có quyền thực hiện thao tác tạm hoãn bệnh nhân.',
    }
  }

  return { allowed: true, message: null }
}

/**
 * Kiểm tra điều kiện đưa lại vào hàng đợi (TC-02)
 * Chỉ áp dụng cho bệnh nhân đang ở trạng thái SKIPPED (Tạm hoãn).
 */
export function evaluateReQueueAction(item, permissions = {}) {
  if (!item) {
    return { allowed: false, message: 'Không tìm thấy thông tin lượt khám.' }
  }

  const status = String(item.status || '').toUpperCase()

  if (status !== 'SKIPPED') {
    return {
      allowed: false,
      message: 'Chỉ áp dụng cho bệnh nhân đang ở trạng thái tạm hoãn.',
    }
  }

  const hasPermission = Boolean(
    permissions.canUpdateQueueStatus ||
    permissions.canReQueue ||
    permissions.isAdmin ||
    permissions.isReceptionist
  )

  if (!hasPermission) {
    return {
      allowed: false,
      message: 'Chỉ Lễ tân hoặc Quản trị viên mới có quyền đưa bệnh nhân trở lại hàng đợi.',
    }
  }

  return { allowed: true, message: null }
}

/**
 * Định dạng nhãn số lần gọi hiển thị trên danh sách (TC-01, TC-04)
 */
export function formatCallCountBadge(callCount) {
  const count = Number(callCount) || 0
  if (count <= 0) return ''
  return `Đã gọi: ${count} lần`
}

export function getCallCountBadgeProps(callCount) {
  const count = Number(callCount) || 0
  if (count <= 0) return null
  return {
    count,
    text: `Đã gọi: ${count} lần`,
    color: count >= 3 ? 'volcano' : 'orange',
  }
}

/**
 * Xử lý thông báo lỗi thân thiện khi thao tác hàng đợi
 */
export function cleanQueueActionErrorMessage(error, defaultMsg = 'Thao tác không thành công.') {
  if (!error) return defaultMsg
  const responseMsg = error?.response?.data?.message || error?.message || ''
  const errorCode = error?.response?.data?.code || error?.response?.data?.error || ''

  if (
    errorCode === 'QUEUE_ITEM_INVALID_STATUS' ||
    responseMsg.includes('does not allow this action') ||
    responseMsg.includes('Invalid status') ||
    responseMsg.includes('Only skipped items can be re-queued')
  ) {
    return 'Lượt khám không ở trạng thái phù hợp để thực hiện thao tác này.'
  }
  if (responseMsg.includes('Medical queue is closed')) {
    return 'Hàng đợi phòng khám hiện đã đóng.'
  }
  if (responseMsg.includes('Cannot re-queue into a queue from a different date')) {
    return 'Không thể đưa lại vào hàng đợi của ngày khác.'
  }
  if (
    errorCode === 'UNAUTHORIZED_QUEUE_OPERATION' ||
    error?.response?.status === 403
  ) {
    return 'Bạn không có quyền thực hiện thao tác này.'
  }
  if (
    errorCode === 'QUEUE_ITEM_NOT_FOUND' ||
    errorCode === 'QUEUE_NOT_FOUND' ||
    error?.response?.status === 404
  ) {
    return 'Không tìm thấy thông tin lượt khám trong hàng đợi.'
  }

  return responseMsg || defaultMsg
}
