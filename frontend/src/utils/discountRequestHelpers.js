export const DISCOUNT_TYPES = {
  PERCENTAGE: 'PERCENTAGE',
  FIXED_AMOUNT: 'FIXED_AMOUNT',
  FULL_FREE: 'FULL_FREE',
}

export const DISCOUNT_TYPE_OPTIONS = {
  PERCENTAGE: 'Giảm theo tỷ lệ %',
  FIXED_AMOUNT: 'Giảm số tiền cố định',
  FULL_FREE: 'Miễn phí 100%',
}

export const DISCOUNT_TYPE_CARDS = [
  {
    key: DISCOUNT_TYPES.PERCENTAGE,
    title: 'Giảm theo tỷ lệ %',
    description: 'Giảm theo phần trăm viện phí (ví dụ 10%, 20%, 50%)',
    badge: '%',
    color: '#0284c7',
  },
  {
    key: DISCOUNT_TYPES.FIXED_AMOUNT,
    title: 'Giảm số tiền cố định',
    description: 'Giảm trừ trực tiếp một số tiền cụ thể vào viện phí',
    badge: 'VNĐ',
    color: '#16a34a',
  },
  {
    key: DISCOUNT_TYPES.FULL_FREE,
    title: 'Miễn phí 100%',
    description: 'Miễn phí toàn bộ viện phí (chế độ bảo trợ / chính sách)',
    badge: 'FREE',
    color: '#9333ea',
  },
]

export const getStatusTag = (status) => {
  switch (status) {
    case 'PENDING':
      return { label: 'Chờ duyệt', color: 'orange', status: 'warning' }
    case 'APPROVED':
      return { label: 'Đã duyệt', color: 'green', status: 'success' }
    case 'REJECTED':
      return { label: 'Đã từ chối', color: 'red', status: 'error' }
    default:
      return { label: status || 'Không xác định', color: 'default', status: 'default' }
  }
}

export const validateDiscountForm = (discountType, discountValue, reason, originalAmount) => {
  const safeReason = typeof reason === 'string' ? reason.trim() : ''
  if (!safeReason) {
    return {
      isValid: false,
      message: 'Lý do đề nghị giảm giá không được để trống.',
    }
  }

  const orig = Number(originalAmount) || 0
  const numVal = Number(discountValue)

  switch (discountType) {
    case DISCOUNT_TYPES.PERCENTAGE: {
      if (discountValue === undefined || discountValue === null || discountValue === '' || Number.isNaN(numVal)) {
        return {
          isValid: false,
          message: 'Vui lòng nhập tỷ lệ phần trăm giảm giá.',
        }
      }
      if (numVal <= 0 || numVal > 100) {
        return {
          isValid: false,
          message: 'Tỷ lệ giảm giá phải lớn hơn 0% và không vượt quá 100%.',
        }
      }
      return { isValid: true, message: null }
    }

    case DISCOUNT_TYPES.FIXED_AMOUNT: {
      if (discountValue === undefined || discountValue === null || discountValue === '' || Number.isNaN(numVal)) {
        return {
          isValid: false,
          message: 'Vui lòng nhập số tiền giảm giá.',
        }
      }
      if (numVal <= 0) {
        return {
          isValid: false,
          message: 'Số tiền giảm giá phải lớn hơn 0.',
        }
      }
      if (orig > 0 && numVal > orig) {
        return {
          isValid: false,
          message: 'Số tiền giảm giá không được vượt quá tổng viện phí.',
        }
      }
      return { isValid: true, message: null }
    }

    case DISCOUNT_TYPES.FULL_FREE: {
      return { isValid: true, message: null }
    }

    default:
      return {
        isValid: false,
        message: 'Loại giảm giá không hợp lệ.',
      }
  }
}

/**
 * Tính toán số tiền giảm và số tiền còn lại dự kiến ĐỂ PREVIEW TRÊN UI.
 * CHỈ mang tính THAM KHẢO khi người dùng đang nhập liệu.
 * Giá trị chính thức luôn lấy từ Backend trả về sau khi tạo/duyệt.
 */
export const calculatePreview = (discountType, discountValue, originalAmount) => {
  const orig = Math.max(0, Number(originalAmount) || 0)
  const numVal = Number(discountValue) || 0

  if (orig === 0) {
    return { originalAmount: 0, discountAmount: 0, finalAmount: 0 }
  }

  switch (discountType) {
    case DISCOUNT_TYPES.PERCENTAGE: {
      if (numVal <= 0) {
        return { originalAmount: orig, discountAmount: 0, finalAmount: orig }
      }
      const safePercent = Math.min(100, numVal)
      const discountAmount = Math.min(orig, Math.round((orig * safePercent) / 100))
      const finalAmount = Math.max(0, orig - discountAmount)
      return { originalAmount: orig, discountAmount, finalAmount }
    }

    case DISCOUNT_TYPES.FIXED_AMOUNT: {
      if (numVal <= 0) {
        return { originalAmount: orig, discountAmount: 0, finalAmount: orig }
      }
      const discountAmount = Math.min(orig, Math.round(numVal))
      const finalAmount = Math.max(0, orig - discountAmount)
      return { originalAmount: orig, discountAmount, finalAmount }
    }

    case DISCOUNT_TYPES.FULL_FREE: {
      return { originalAmount: orig, discountAmount: orig, finalAmount: 0 }
    }

    default:
      return { originalAmount: orig, discountAmount: 0, finalAmount: orig }
  }
}

/**
 * Kiểm tra người dùng hiện tại có quyền duyệt/từ chối đề xuất hay không.
 * Backend cấm người đề xuất tự duyệt yêu cầu của mình (SelfApprovalNotAllowedException).
 * Trả về false nếu currentUserId trùng với requestedBy.
 */
export const canApproveOrReject = (request, currentUserId) => {
  if (!request || !currentUserId) return true
  const requester = request.requestedBy
  if (!requester) return true
  return String(requester).toLowerCase() !== String(currentUserId).toLowerCase()
}

/**
 * Map mã lỗi trả về từ API backend sang thông báo thân thiện chuẩn theo contract
 */
export const mapDiscountErrorMessage = (error) => {
  if (!error) return 'Đã có lỗi xảy ra. Vui lòng thử lại.'

  const errorData = error.response?.data
  const status = error.response?.status
  const errorCode = errorData?.code || error.apiError?.code

  if (errorCode === 'SELF_APPROVAL_NOT_ALLOWED' || status === 403 && String(errorData?.message || '').includes('tự phê duyệt')) {
    return 'Bạn không thể tự duyệt/từ chối đề xuất do chính mình tạo.'
  }

  if (errorCode === 'PENDING_DISCOUNT_APPROVAL') {
    return 'Lượt khám này đang có đề xuất giảm giá chờ duyệt. Vui lòng chờ kết quả trước khi thu phí/lập hóa đơn.'
  }

  if (errorCode === 'DISCOUNT_ALREADY_EXISTS') {
    return 'Lượt khám này đã có đề xuất giảm giá đang chờ duyệt hoặc đã được phê duyệt.'
  }

  if (errorCode === 'DISCOUNT_EXCEEDS_TOTAL') {
    return 'Số tiền giảm giá không được vượt quá tổng viện phí chưa giảm.'
  }

  if (errorCode === 'INVALID_DISCOUNT_STATE') {
    return 'Chỉ có thể xử lý đề xuất đang ở trạng thái chờ duyệt (PENDING).'
  }

  if (errorCode === 'DISCOUNT_REQUEST_NOT_FOUND') {
    return 'Không tìm thấy thông tin đề xuất giảm giá.'
  }

  if (errorCode === 'VISIT_NOT_FOUND') {
    return 'Lượt khám không tồn tại trong hệ thống.'
  }

  if (errorCode === 'ACCESS_DENIED' || status === 403) {
    return 'Bạn không có quyền thực hiện thao tác này.'
  }

  if (errorData?.message) {
    return errorData.message
  }

  if (error.message) {
    return error.message
  }

  return 'Có lỗi xảy ra khi xử lý đề xuất giảm giá. Vui lòng thử lại.'
}
