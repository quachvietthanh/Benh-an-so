import { message, notification } from 'antd'

/**
 * Metadata cho Trạng thái QueueItem / Visit / Appointment
 */
export const QUEUE_STATUS_META = {
  WAITING: { label: 'Đang chờ', tone: 'blue', color: '#2563eb', bg: '#eff6ff' },
  IN_PROGRESS: { label: 'Đang khám / Đã gọi', tone: 'green', color: '#16a34a', bg: '#f0fdf4' },
  WAITING_FOR_RESULT: { label: 'Chờ kết quả CĐLS', tone: 'purple', color: '#9333ea', bg: '#faf5ff' },
  COMPLETED: { label: 'Hoàn thành', tone: 'gray', color: '#475569', bg: '#f8fafc' },
  CANCELLED: { label: 'Đã hủy', tone: 'red', color: '#dc2626', bg: '#fef2f2' },
  SKIPPED: { label: 'Vắng mặt khi gọi', tone: 'orange', color: '#d97706', bg: '#fffbeb' },
}

export const APPOINTMENT_STATUS_META = {
  SCHEDULED: { label: 'Đã đặt hẹn', tone: 'blue', color: '#2563eb' },
  CHECKED_IN: { label: 'Đã check-in (Chờ khám)', tone: 'cyan', color: '#0891b2' },
  COMPLETED: { label: 'Đã khám xong', tone: 'gray', color: '#475569' },
  CANCELLED: { label: 'Đã hủy', tone: 'red', color: '#dc2626' },
  NO_SHOW: { label: 'Không đến', tone: 'orange', color: '#d97706' },
}

/**
 * Xử lý thông báo lỗi HTTP Status tiêu chuẩn cho FE (400, 401, 403, 404, 409)
 */
export const handleQueueApiError = (error, defaultMessage = 'Thao tác không thành công') => {
  const status = error?.response?.status
  const backendMsg = error?.response?.data?.message || error?.response?.data?.error

  let title = 'Thông báo hệ thống'
  let detail = backendMsg || defaultMessage

  switch (status) {
    case 400:
      title = '400 - Dữ liệu không hợp lệ'
      detail = backendMsg || 'Thông tin gửi lên bị thiếu hoặc không đúng định dạng.'
      break
    case 401:
      title = '401 - Hết phiên đăng nhập'
      detail = 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.'
      break
    case 403:
      title = '403 - Không có quyền thao tác'
      detail = backendMsg || 'Tài khoản của bạn không được cấp quyền thực hiện chức năng này.'
      break
    case 404:
      title = '404 - Không tìm thấy dữ liệu'
      detail = backendMsg || 'Dữ liệu Lịch hẹn, Hàng đợi hoặc Lượt khám không tồn tại.'
      break
    case 409:
      title = '409 - Xung đột luồng vận hành'
      detail = backendMsg || 'Sai chu trình khám, trùng lượt khám, bác sĩ chưa được gán phòng hoặc bệnh án chưa được khóa.'
      break
    default:
      if (status >= 500) {
        title = '500 - Lỗi máy chủ Backend'
        detail = backendMsg || 'Hệ thống máy chủ gặp sự cố. Vui lòng thử lại sau.'
      }
      break
  }

  notification.error({
    message: title,
    description: detail,
    placement: 'topRight',
    duration: 4.5,
  })

  return detail
}

/**
 * Phân quyền vai trò cho chức năng Hàng đợi khám
 */
export const checkQueuePermissions = (roles = []) => {
  const normalized = (Array.isArray(roles) ? roles : [roles])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))

  const isAdmin = normalized.includes('admin')
  const isReceptionist = normalized.includes('receptionist')
  const isDoctor = normalized.includes('doctor')
  const isNurse = normalized.includes('nurse')

  return {
    canCheckIn: isAdmin || isReceptionist,
    canCallNext: isAdmin || isReceptionist || isDoctor,
    canSkip: isAdmin || isReceptionist || isDoctor,
    canComplete: isAdmin || isDoctor,
    canChangeResultStatus: isAdmin || isDoctor || isNurse,
    canManageWalkIn: isAdmin || isReceptionist,
    isNurseOnly: isNurse && !isAdmin && !isDoctor && !isReceptionist,
  }
}
