/**
 * Tiện ích hỗ trợ tính năng Ưu tiên khám cho trường hợp cấp cứu (NCL-03-CN-013 / QTN-40).
 * Quản lý mức ưu tiên, xác thực biểu mẫu, định dạng nhãn hiển thị và ánh xạ thông báo lỗi.
 */

export const PRESET_PRIORITY_REASONS = {
  EMERGENCY: [
    'Sốt cao co giật',
    'Đau ngực dữ dội/khó thở',
    'Chảy máu không cầm',
    'Mất ý thức/ngất xỉu',
    'Tai nạn chấn thương nặng',
  ],
  PRIORITY: [
    'Người cao tuổi (>75 tuổi)',
    'Phụ nữ có thai',
    'Trẻ em dưới 6 tuổi',
    'Người khuyết tật',
  ],
}

/**
 * Kiểm tra tính hợp lệ của dữ liệu đánh dấu ưu tiên
 * @param {string} priority 'PRIORITY' hoặc 'EMERGENCY'
 * @param {string} reason Lý do ưu tiên bắt buộc (tối đa 500 ký tự)
 * @returns {{ valid: boolean, error: string | null, trimmedReason: string }}
 */
export function validatePrioritizeForm(priority, reason) {
  if (!priority || (priority !== 'PRIORITY' && priority !== 'EMERGENCY')) {
    return {
      valid: false,
      error: 'Mức độ ưu tiên bắt buộc phải là Đối tượng ưu tiên hoặc Cấp cứu.',
      trimmedReason: typeof reason === 'string' ? reason.trim() : '',
    }
  }

  if (typeof reason !== 'string') {
    return {
      valid: false,
      error: 'Lý do ưu tiên không hợp lệ.',
      trimmedReason: '',
    }
  }

  const trimmed = reason.trim()
  if (!trimmed) {
    return {
      valid: false,
      error: 'Vui lòng nhập hoặc chọn lý do ưu tiên khám cho bệnh nhân.',
      trimmedReason: '',
    }
  }

  if (trimmed.length > 500) {
    return {
      valid: false,
      error: 'Lý do ưu tiên không được vượt quá 500 ký tự.',
      trimmedReason: trimmed.slice(0, 500),
    }
  }

  return {
    valid: true,
    error: null,
    trimmedReason: trimmed,
  }
}

/**
 * Kiểm tra trạng thái lượt khám có đủ điều kiện đánh dấu ưu tiên hay không
 * Backend quy định: CHỈ cho phép khi status === 'WAITING'
 * @param {string} status Trạng thái lượt khám
 * @returns {boolean}
 */
export function canPrioritize(status) {
  if (!status) return false
  return String(status).toUpperCase() === 'WAITING'
}

/**
 * Lấy cấu hình huy hiệu / nhãn hiển thị mức độ ưu tiên
 * @param {string} priority 'EMERGENCY' | 'PRIORITY' | 'NORMAL'
 * @returns {{ label: string, color: string, tone: string, textColor: string, bgColor: string, borderColor: string, isEmergency: boolean, rank: number } | null}
 */
export function getPriorityTag(priority) {
  if (!priority) return null
  const key = String(priority).toUpperCase()

  if (key === 'EMERGENCY') {
    return {
      label: 'CẤP CỨU',
      color: 'red',
      tone: 'error',
      textColor: '#dc2626',
      bgColor: '#fef2f2',
      borderColor: '#fca5a5',
      isEmergency: true,
      rank: 1,
    }
  }

  if (key === 'PRIORITY') {
    return {
      label: 'ƯU TIÊN',
      color: 'orange',
      tone: 'warning',
      textColor: '#d97706',
      bgColor: '#fffbeb',
      borderColor: '#fde68a',
      isEmergency: false,
      rank: 2,
    }
  }

  return null
}

/**
 * Ánh xạ lỗi API từ Backend sang thông báo tiếng Việt thân thiện
 * @param {object} error Lỗi Axios
 * @returns {string}
 */
export function mapPrioritizeErrorMessage(error) {
  const status = error?.response?.status
  const serverMsg = error?.response?.data?.message || ''

  if (status === 409) {
    return 'Chỉ có thể đánh dấu ưu tiên khi bệnh nhân đang ở trạng thái chờ khám.'
  }

  if (status === 403) {
    return 'Bạn không có quyền thực hiện đánh dấu ưu tiên (yêu cầu quyền Lễ tân hoặc Quản trị viên).'
  }

  if (status === 400) {
    if (serverMsg) return serverMsg
    return 'Thông tin đánh dấu ưu tiên không hợp lệ (lý do bắt buộc và tối đa 500 ký tự).'
  }

  if (status === 404 || serverMsg === 'Resource not found.') {
    return 'Không thể kết nối đến tính năng này. Vui lòng thử lại sau ít phút hoặc liên hệ bộ phận kỹ thuật.'
  }

  return serverMsg || 'Không thể đánh dấu ưu tiên cho bệnh nhân. Vui lòng thử lại.'
}

/**
 * Hàm hỗ trợ sắp xếp theo thứ tự ưu tiên (bảo toàn logic Backend: EMERGENCY -> PRIORITY -> NORMAL)
 * @param {Array} items
 * @returns {Array}
 */
export function sortQueueItemsByPriority(items = []) {
  if (!Array.isArray(items)) return []
  return [...items].sort((a, b) => {
    const rankA = a.priority === 'EMERGENCY' ? 1 : a.priority === 'PRIORITY' ? 2 : 3
    const rankB = b.priority === 'EMERGENCY' ? 1 : b.priority === 'PRIORITY' ? 2 : 3
    if (rankA !== rankB) return rankA - rankB
    if (rankA < 3 && a.prioritizedAt && b.prioritizedAt) {
      return new Date(a.prioritizedAt).getTime() - new Date(b.prioritizedAt).getTime()
    }
    return Number(a.queueNumber || 999999) - Number(b.queueNumber || 999999)
  })
}
