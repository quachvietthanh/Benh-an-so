import { notification } from 'antd'

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

export const translateBackendMessage = (msg) => {
  if (!msg) return ''
  const str = String(msg).toLowerCase()
  if (str.includes('already has an active visit') || str.includes('active visit or queue item')) {
    return 'Bệnh nhân này hiện đã có một lượt khám hoặc đang ở trong hàng đợi khám, hệ thống không thêm trùng.'
  }
  if (str.includes('no waiting queue item') || str.includes('no waiting item')) {
    return 'Hiện tại không có bệnh nhân nào đang ở trạng thái chờ khám trong hàng đợi.'
  }
  if (str.includes('already in queue') || str.includes('already checked in')) {
    return 'Bệnh nhân đã xuất hiện trong hàng đợi khám, hệ thống không thêm trùng.'
  }
  if (str.includes('inactive patients')) {
    return 'Bệnh nhân đang ở trạng thái ngừng hoạt động nên không thể tiếp nhận.'
  }
  if (str.includes('assigned doctor does not exist')) {
    return 'Không tìm thấy bác sĩ được phân công. Vui lòng chọn lại bác sĩ.'
  }
  if (str.includes('inactive doctors')) {
    return 'Bác sĩ đang ở trạng thái ngừng hoạt động nên không thể nhận bệnh nhân.'
  }
  if (str.includes('active room assignment')) {
    return 'Bác sĩ chưa được phân công phòng khám đang hoạt động.'
  }
  if (str.includes('scheduled date')) {
    return 'Chỉ có thể tiếp nhận lịch hẹn đúng ngày đã đặt.'
  }
  if (str.includes('doctor queue is closed')) {
    return 'Hàng đợi của bác sĩ đã đóng trong ngày được chọn.'
  }
  if (str.includes('overlapping') || str.includes('busy slot')) {
    return 'Bác sĩ đã có lịch hẹn trùng trong cùng khung giờ.'
  }
  if (str.includes('past')) {
    return 'Khung giờ hẹn đã ở quá khứ, không thể thao tác.'
  }
  if (str.includes('completed') || str.includes('cancelled')) {
    return 'Lịch hẹn hoặc lượt khám đã hoàn tất hoặc đã bị hủy.'
  }
  if (str.includes('doctor is not assigned')) {
    return 'Bác sĩ chưa được gán phòng khám phù hợp.'
  }
  if (str.includes('medical record is not locked')) {
    return 'Bệnh án chưa được ký hoặc khóa trước khi hoàn tất.'
  }
  return /[À-ỹĐđ]/.test(String(msg)) ? msg : ''
}

export const handleQueueApiError = (error, defaultMessage = 'Thao tác không thành công') => {
  const status = error?.response?.status
  const rawMsg = error?.response?.data?.message || error?.response?.data?.error
  const backendMsg = translateBackendMessage(rawMsg)

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
      title = '409 - Hàng đợi / Chu trình vận hành'
      detail = backendMsg || 'Hiện tại bệnh nhân đã ở trong hàng đợi hoặc chưa đúng chu trình khám.'
      break
    default:
      if (status >= 500) {
        title = '500 - Lỗi máy chủ'
        detail = backendMsg || 'Hệ thống máy chủ gặp sự cố. Vui lòng thử lại sau.'
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
