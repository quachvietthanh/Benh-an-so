import { notification } from 'antd'
import { normalizeApiError } from './apiError.js'

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
  CHECKED_IN: { label: 'Đã tiếp nhận (Chờ khám)', tone: 'cyan', color: '#0891b2' },
  COMPLETED: { label: 'Đã khám xong', tone: 'gray', color: '#475569' },
  CANCELLED: { label: 'Đã hủy', tone: 'red', color: '#dc2626' },
  NO_SHOW: { label: 'Không đến', tone: 'orange', color: '#d97706' },
}

const QUEUE_ERROR_MESSAGES = {
  APPOINTMENT_TIME_CONFLICT: 'Bác sĩ đã có lịch hẹn trùng trong cùng khung giờ.',
  APPOINTMENT_TIME_IN_PAST: 'Khung giờ hẹn đã ở quá khứ, không thể thao tác.',
  CHECK_IN_CONFLICT: 'Không thể tiếp nhận vì dữ liệu lịch hẹn hoặc hàng đợi không hợp lệ.',
  DOCTOR_INACTIVE: 'Bác sĩ đang ở trạng thái ngừng hoạt động nên không thể nhận bệnh nhân.',
  DOCTOR_ROOM_ASSIGNMENT_NOT_FOUND: 'Bác sĩ chưa được phân công phòng khám đang hoạt động.',
  MEDICAL_RECORD_NOT_LOCKED: 'Bệnh án chưa được ký hoặc khóa trước khi hoàn tất.',
  PATIENT_INACTIVE: 'Bệnh nhân đang ở trạng thái ngừng hoạt động nên không thể tiếp nhận.',
  QUEUE_ITEM_INVALID_STATUS: 'Lượt khám không ở trạng thái phù hợp để thực hiện thao tác.',
  VISIT_ALREADY_CANCELLED: 'Lịch hẹn hoặc lượt khám đã bị hủy.',
  VISIT_ALREADY_COMPLETED: 'Lịch hẹn hoặc lượt khám đã hoàn tất.',
}

export const handleQueueApiError = (error, defaultMessage = 'Thao tác không thành công') => {
  const apiError = normalizeApiError(error, defaultMessage)
  const { code, status } = apiError
  const translatedMessage = QUEUE_ERROR_MESSAGES[code]

  let title = 'Thông báo hệ thống'
  let detail = translatedMessage || apiError.firstFieldError || defaultMessage

  switch (status) {
    case 400:
      title = '400 - Dữ liệu không hợp lệ'
      detail = translatedMessage || apiError.firstFieldError || 'Thông tin gửi lên bị thiếu hoặc không đúng định dạng.'
      break
    case 401:
      title = '401 - Hết phiên đăng nhập'
      detail = 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.'
      break
    case 403:
      title = '403 - Không có quyền thao tác'
      detail = translatedMessage || 'Tài khoản của bạn không được cấp quyền thực hiện chức năng này.'
      break
    case 404:
      title = '404 - Không tìm thấy dữ liệu'
      detail = translatedMessage || 'Dữ liệu Lịch hẹn, Hàng đợi hoặc Lượt khám không tồn tại.'
      break
    case 409:
      title = '409 - Hàng đợi / Chu trình vận hành'
      detail = translatedMessage || 'Hiện tại bệnh nhân đã ở trong hàng đợi hoặc chưa đúng chu trình khám.'
      break
    default:
      if (status >= 500) {
        title = '500 - Lỗi máy chủ'
        detail = translatedMessage || 'Hệ thống máy chủ gặp sự cố. Vui lòng thử lại sau.'
      }
      break
  }

  notification.warning({
    message: title,
    description: detail,
    placement: 'topRight',
    duration: 4.5,
  })

  return detail
}

export const checkQueuePermissions = (roles = [], permissions = []) => {
  const normalized = (Array.isArray(roles) ? roles : [roles])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))

  const userPerms = (Array.isArray(permissions) ? permissions : [permissions])
    .map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))

  const hasPerm = (code) => userPerms.includes(code)

  const isAdmin = normalized.includes('admin')
  const isReceptionist = normalized.includes('receptionist')
  const isDoctor = normalized.includes('doctor')
  const isNurse = normalized.includes('nurse')

  const canCreateApp = hasPerm('APPOINTMENT_CREATE')
  const canReadApp = hasPerm('APPOINTMENT_READ')
  const canUpdateApp = hasPerm('APPOINTMENT_UPDATE')
  const canDeleteApp = hasPerm('APPOINTMENT_DELETE')

  const canCreateQueue = hasPerm('QUEUE_CREATE')
  const canCallNext = hasPerm('QUEUE_CALL_NEXT')
  const canUpdateQueueStatus = hasPerm('QUEUE_UPDATE_STATUS')
  const canViewQueue = hasPerm('QUEUE_VIEW')
  const canCountQueue = hasPerm('QUEUE_COUNT')

  return {
    canViewBoard: canViewQueue || canReadApp || canCreateApp || isAdmin || isDoctor || isNurse || isReceptionist,
    canViewMyQueue: (canViewQueue || isDoctor) && isDoctor,
    canCheckIn: canCreateQueue || canUpdateQueueStatus || isAdmin || isReceptionist,
    canCallNext: canCallNext || canUpdateQueueStatus || isAdmin || isReceptionist || isDoctor,
    canSkip: canUpdateQueueStatus || isAdmin || isReceptionist || isDoctor,
    canComplete: canUpdateQueueStatus || isAdmin || isDoctor,
    canUpdateStatus: canUpdateQueueStatus || isAdmin || isDoctor || isNurse,
    canChangeResultStatus: canUpdateQueueStatus || isAdmin || isDoctor || isNurse,
    canManageWalkIn: canCreateQueue || canCreateApp || isAdmin || isReceptionist,
    canCreateAppointment: canCreateApp || isAdmin || isReceptionist,
    canReadAppointment: canReadApp || isAdmin || isDoctor || isReceptionist,
    canUpdateAppointment: canUpdateApp || isAdmin || isReceptionist,
    canDeleteAppointment: canDeleteApp || isAdmin || isReceptionist,
    isNurseOnly: isNurse && !isAdmin && !isDoctor && !isReceptionist,
    isDoctorOnly: isDoctor && !isAdmin && !isReceptionist,
    isAdmin,
    isReceptionist,
    isDoctor,
    isNurse,
  }
}
