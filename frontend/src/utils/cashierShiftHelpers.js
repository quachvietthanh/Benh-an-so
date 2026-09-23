/**
 * Định dạng tiền tệ VND
 * @param {number|string|null|undefined} amount
 * @returns {string}
 */
export const formatCurrency = (amount) => {
  if (amount === null || amount === undefined || amount === '') return '0 ₫'
  const numeric = typeof amount === 'number' ? amount : Number(amount)
  if (Number.isNaN(numeric)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(numeric)
}

/**
 * Định dạng ngày giờ chuẩn hiển thị UI
 * @param {string|Date|null|undefined} dateInput
 * @returns {string}
 */
export const formatDateTime = (dateInput) => {
  if (!dateInput) return '—'
  const date = new Date(dateInput)
  if (Number.isNaN(date.getTime())) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const seconds = pad(date.getSeconds())
  const day = pad(date.getDate())
  const month = pad(date.getMonth() + 1)
  const year = date.getFullYear()
  return `${hours}:${minutes}:${seconds} ${day}/${month}/${year}`
}

/**
 * Tính chênh lệch tiền mặt dự kiến ở Frontend để preview trực quan
 * (Lưu ý: Không dùng số này gửi lên Backend, Backend tự tính chính thức)
 * @param {number|string} actualCash
 * @param {number|string} systemCash
 * @returns {number}
 */
export const calculatePreviewDifference = (actualCash, systemCash) => {
  const actual = Number(actualCash) || 0
  const system = Number(systemCash) || 0
  return actual - system
}

/**
 * Kiểm tra xem có bắt buộc nhập ghi chú giải trình không
 * Quy tắc: Bắt buộc khi tiền thực đếm lệch tiền hệ thống (difference !== 0)
 * @param {number} difference
 * @returns {boolean}
 */
export const isNoteRequired = (difference) => {
  return typeof difference === 'number' && !Number.isNaN(difference) && Math.abs(difference) > 0.001
}

/**
 * Lấy cấu hình hiển thị trạng thái phiếu chốt ca
 * @param {'PENDING_CONFIRMATION'|'CONFIRMED'|'REJECTED'|string} status
 * @returns {{ label: string, color: string, tagColor: string }}
 */
export const getShiftStatusMeta = (status) => {
  switch (status) {
    case 'PENDING_CONFIRMATION':
      return {
        label: 'Chờ quản lý duyệt',
        color: 'warning',
        tagColor: 'gold',
      }
    case 'CONFIRMED':
      return {
        label: 'Đã xác nhận',
        color: 'success',
        tagColor: 'green',
      }
    case 'REJECTED':
      return {
        label: 'Đã từ chối',
        color: 'error',
        tagColor: 'red',
      }
    default:
      return {
        label: status || 'Không xác định',
        color: 'default',
        tagColor: 'default',
      }
  }
}

/**
 * Kiểm tra người dùng hiện tại có đủ điều kiện duyệt phiếu hay không
 * Điều kiện:
 * 1. Phiếu đang ở trạng thái PENDING_CONFIRMATION
 * 2. Người dùng có quyền CASHIER_SHIFT_CONFIRM hoặc vai trò MANAGER / CLINIC_MANAGER / ADMIN
 * 3. Người dùng KHÔNG PHẢI là người tạo phiếu (cashierId !== currentUserId) - QTN-38 / QTN-09
 * @param {Object} currentUser
 * @param {Object} shift
 * @returns {boolean}
 */
export const canConfirmShift = (currentUser, shift) => {
  if (!currentUser || !shift) return false
  if (shift.status !== 'PENDING_CONFIRMATION') return false

  const currentUserId = currentUser.id || currentUser.userId
  const shiftCashierId = shift.cashierId

  // Chặn tự duyệt: nếu trùng ID người chốt ca thì ẩn nút duyệt
  if (
    currentUserId &&
    shiftCashierId &&
    String(currentUserId).trim().toLowerCase() === String(shiftCashierId).trim().toLowerCase()
  ) {
    return false
  }

  // Kiểm tra quyền
  const roles = (Array.isArray(currentUser.roles) ? currentUser.roles : [currentUser.roles || currentUser.role])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)
  const permissions = (Array.isArray(currentUser.permissions) ? currentUser.permissions : [currentUser.permissions])
    .map((p) => String(p || '').toUpperCase().replace(/^permission_/, ''))
    .filter(Boolean)

  const hasConfirmPerm = permissions.includes('CASHIER_SHIFT_CONFIRM')
  const isManagerOrAdmin = roles.includes('manager') || roles.includes('clinic_manager') || roles.includes('admin')

  return hasConfirmPerm || isManagerOrAdmin
}

/**
 * Map các mã lỗi nghiệp vụ Backend sang thông điệp tiếng Việt
 * @param {any} error
 * @returns {string}
 */
export const mapCashierShiftError = (error) => {
  if (!error) return 'Đã có lỗi xảy ra. Vui lòng thử lại.'

  const data = error.response?.data
  const code = (
    data?.code ||
    data?.errorCode ||
    error.code ||
    ''
  ).toUpperCase()

  const message = data?.message || error.message || ''

  if (code === 'NO_UNSETTLED_PAYMENTS' || message.includes('NoUnsettledPaymentsException')) {
    return 'Ca hiện tại chưa có khoản thu nào để thực hiện chốt ca.'
  }

  if (code === 'CASHIER_SHIFT_NOTE_REQUIRED' || message.includes('CashierShiftNoteRequiredException')) {
    return 'Số tiền thực tế có chênh lệch so với hệ thống, bắt buộc phải nhập ghi chú giải trình.'
  }

  if (
    code === 'CASHIER_SHIFT_SELF_CONFIRMATION_NOT_ALLOWED' ||
    message.includes('SelfConfirmationNotAllowedException') ||
    message.includes('không được tự duyệt')
  ) {
    return 'Bạn không thể tự duyệt phiếu chốt ca của chính mình.'
  }

  if (code === 'PAYMENT_ALREADY_SETTLED' || message.includes('PaymentAlreadySettledException')) {
    return 'Khoản thu này đã nằm trong ca đã chốt, không thể chỉnh sửa hoặc hoàn tiền.'
  }

  if (code === 'CASHIER_SHIFT_ALREADY_CONFIRMED' || message.includes('CashierShiftAlreadyConfirmedException')) {
    return 'Phiếu chốt ca này đã được xác nhận trước đó.'
  }

  if (code === 'CASHIER_SHIFT_NOT_FOUND' || message.includes('CashierShiftNotFoundException')) {
    return 'Không tìm thấy thông tin phiếu chốt ca.'
  }

  return message || 'Thao tác không thành công. Vui lòng kiểm tra lại.'
}
