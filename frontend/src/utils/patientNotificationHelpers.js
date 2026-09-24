/**
 * Helper nghiệp vụ cho tính năng Thông báo và nhắc lịch trên cổng bệnh nhân (NCL-14-CN-008, QTN-23).
 */

export const NOTIFICATION_TYPES = {
  APPOINTMENT_REMINDER: 'APPOINTMENT_REMINDER',
  APPOINTMENT_CHANGED: 'APPOINTMENT_CHANGED',
  LAB_RESULT_AVAILABLE: 'LAB_RESULT_AVAILABLE',
}

export const NOTIFICATION_TYPE_CONFIG = {
  APPOINTMENT_REMINDER: {
    type: 'APPOINTMENT_REMINDER',
    label: 'Nhắc lịch hẹn',
    badgeColor: '#2563eb',
    tagColor: 'blue',
    targetRoute: '/portal/my-appointments',
    actionText: 'Xem lịch hẹn của tôi',
    defaultIcon: 'ClockCircleOutlined',
  },
  APPOINTMENT_CHANGED: {
    type: 'APPOINTMENT_CHANGED',
    label: 'Lịch hẹn thay đổi',
    badgeColor: '#ea580c',
    tagColor: 'orange',
    targetRoute: '/portal/my-appointments',
    actionText: 'Xem chi tiết lịch hẹn',
    defaultIcon: 'SwapOutlined',
  },
  LAB_RESULT_AVAILABLE: {
    type: 'LAB_RESULT_AVAILABLE',
    label: 'Kết quả cận lâm sàng mới',
    badgeColor: '#16a34a',
    tagColor: 'green',
    targetRoute: '/portal/medical-history',
    actionText: 'Xem lịch sử khám & kết quả',
    defaultIcon: 'ExperimentOutlined',
  },
}

/**
 * GIẢI PHÁP TẠM THỜI (NCL-14-CN-008 / QTN-23):
 * Backend hiện chưa có endpoint /unread-count riêng; số lượng thông báo chưa đọc
 * được tính trực tiếp từ danh sách tải về (tối đa 50-100 bản ghi mới nhất).
 *
 * @param {Array} notifications Danh sách thông báo
 * @returns {number} Số lượng thông báo chưa đọc
 */
export const calculateUnreadCount = (notifications) => {
  if (!Array.isArray(notifications) || notifications.length === 0) {
    return 0
  }
  return notifications.filter((item) => item && item.read === false).length
}

/**
 * Lấy metadata cấu hình giao diện theo loại thông báo
 * @param {string} type
 */
export const getNotificationTypeMeta = (type) => {
  return (
    NOTIFICATION_TYPE_CONFIG[type] || {
      type: type || 'UNKNOWN',
      label: 'Thông báo',
      badgeColor: '#64748b',
      tagColor: 'default',
      targetRoute: '/portal/dashboard',
      actionText: 'Xem thông tin',
      defaultIcon: 'BellOutlined',
    }
  )
}

/**
 * Điều hướng theo loại thông báo (RÀNG BUỘC KIẾN TRÚC #2):
 * Backend Response hiện chỉ trả về [id, type, title, message, read, readAt, createdAt],
 * KHÔNG chứa appointmentId/clinicalResultId/rescheduleLogId.
 * Do đó điều hướng an toàn theo 'type' tới trang danh mục tương ứng.
 *
 * @param {string} type
 * @returns {string} URL path cổng bệnh nhân
 */
export const resolveNotificationRoute = (type) => {
  switch (type) {
    case NOTIFICATION_TYPES.APPOINTMENT_REMINDER:
    case NOTIFICATION_TYPES.APPOINTMENT_CHANGED:
      return '/portal/my-appointments'
    case NOTIFICATION_TYPES.LAB_RESULT_AVAILABLE:
      return '/portal/medical-history'
    default:
      return '/portal/dashboard'
  }
}

/**
 * Định dạng thời gian hiển thị thân thiện (VD: 08:30 24/09/2026)
 * @param {string|Date} dateVal
 * @returns {string}
 */
export const formatNotificationTime = (dateVal) => {
  if (!dateVal) return '—'
  const date = new Date(dateVal)
  if (isNaN(date.getTime())) return '—'

  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()

  return `${hours}:${minutes} ${day}/${month}/${year}`
}
