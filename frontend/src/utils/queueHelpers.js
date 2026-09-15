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

const QUEUE_DETAIL_TRANSLATIONS = {
  'Patient already has an active visit or queue item.': 'Bệnh nhân đã có lượt khám hoặc số thứ tự đang chờ/đang khám trong ngày.',
  'Patient already has an active visit or queue item': 'Bệnh nhân đã có lượt khám hoặc số thứ tự đang chờ/đang khám trong ngày.',
  'Appointment can only be checked in on its scheduled date.': 'Lịch hẹn chỉ có thể tiếp nhận đúng vào ngày hẹn khám.',
  'Appointment can only be checked in on its scheduled date': 'Lịch hẹn chỉ có thể tiếp nhận đúng vào ngày hẹn khám.',
  'Doctor queue is closed for the selected date.': 'Hàng đợi phòng khám của bác sĩ đã đóng trong ngày được chọn.',
  'Doctor queue is closed for the selected date': 'Hàng đợi phòng khám của bác sĩ đã đóng trong ngày được chọn.',
  'Inactive patients cannot be checked in.': 'Hồ sơ bệnh nhân đang tạm ngưng hoạt động, không thể tiếp nhận.',
  'Inactive patients cannot be checked in': 'Hồ sơ bệnh nhân đang tạm ngưng hoạt động, không thể tiếp nhận.',
  'Inactive doctors cannot receive queue items.': 'Bác sĩ đang tạm ngưng hoạt động, không thể nhận bệnh nhân vào hàng đợi.',
  'Inactive doctors cannot receive queue items': 'Bác sĩ đang tạm ngưng hoạt động, không thể nhận bệnh nhân vào hàng đợi.',
  'Assigned doctor does not exist.': 'Bác sĩ được chỉ định không tồn tại trong hệ thống.',
  'Assigned doctor does not exist': 'Bác sĩ được chỉ định không tồn tại trong hệ thống.',
  'Doctor is not assigned to room': 'Bác sĩ chưa được phân công phòng khám đang hoạt động.',
  'Doctor is not assigned to room.': 'Bác sĩ chưa được phân công phòng khám đang hoạt động.',
  'No waiting queue item is available.': 'Hiện không còn bệnh nhân nào đang chờ trong hàng đợi.',
  'No waiting queue item is available': 'Hiện không còn bệnh nhân nào đang chờ trong hàng đợi.',
  'Medical queue is closed.': 'Hàng đợi phòng khám hiện đang đóng.',
  'Medical queue is closed': 'Hàng đợi phòng khám hiện đang đóng.',
  'Medical queue does not exist.': 'Không tìm thấy hàng đợi phòng khám của bác sĩ.',
  'Medical queue does not exist': 'Không tìm thấy hàng đợi phòng khám của bác sĩ.',
  'Checked-in appointments cannot be marked as no show.': 'Lịch hẹn đã tiếp nhận không thể đánh dấu là không đến.',
  'Checked-in appointments cannot be marked as no show': 'Lịch hẹn đã tiếp nhận không thể đánh dấu là không đến.',
  'Visit must have a locked medical record before completion.': 'Lượt khám phải có hồ sơ bệnh án đã khóa trước khi hoàn tất.',
  'Visit must have a locked medical record before completion': 'Lượt khám phải có hồ sơ bệnh án đã khóa trước khi hoàn tất.',
  'Medical record must be locked before visit completion.': 'Hồ sơ bệnh án phải được khóa trước khi hoàn tất lượt khám.',
  'Medical record must be locked before visit completion': 'Hồ sơ bệnh án phải được khóa trước khi hoàn tất lượt khám.',
}

const QUEUE_ERROR_MESSAGES = {
  // Appointment errors
  APPOINTMENT_ALREADY_CANCELLED: 'Lịch hẹn khám này đã bị hủy trước đó.',
  APPOINTMENT_ALREADY_COMPLETED: 'Lịch hẹn này đã hoàn tất quá trình khám bệnh.',
  APPOINTMENT_ALREADY_IN_PROGRESS: 'Lịch hẹn này hiện đang trong quá trình khám bệnh.',
  APPOINTMENT_INVALID_STATUS: 'Trạng thái lịch hẹn không hợp lệ để thực hiện thao tác.',
  APPOINTMENT_NOT_FOUND: 'Không tìm thấy thông tin lịch hẹn trong hệ thống.',
  APPOINTMENT_PAST_CUTOFF: 'Đã quá thời hạn cho phép điều chỉnh lịch hẹn này.',
  APPOINTMENT_TIME_CONFLICT: 'Bác sĩ đã có lịch hẹn trùng trong cùng khung giờ.',
  APPOINTMENT_TIME_IN_PAST: 'Khung giờ hẹn đã ở quá khứ, không thể thao tác.',
  UNAUTHORIZED_APPOINTMENT_OPERATION: 'Bạn không có quyền thao tác trên lịch hẹn này.',

  // Queue & Visit errors
  CHECK_IN_CONFLICT: 'Không thể tiếp nhận vì dữ liệu lịch hẹn hoặc hàng đợi không hợp lệ.',
  QUEUE_ITEM_INVALID_STATUS: 'Lượt khám không ở trạng thái phù hợp để thực hiện thao tác.',
  QUEUE_ITEM_NOT_FOUND: 'Không tìm thấy lượt khám trong hàng đợi phòng khám.',
  QUEUE_NOT_FOUND: 'Không tìm thấy hàng đợi của phòng khám.',
  UNAUTHORIZED_QUEUE_OPERATION: 'Bạn không có quyền thao tác trên hàng đợi của phòng khám này.',
  VISIT_ALREADY_CANCELLED: 'Lịch hẹn hoặc lượt khám đã bị hủy.',
  VISIT_ALREADY_COMPLETED: 'Lịch hẹn hoặc lượt khám đã hoàn tất.',
  VISIT_ENCOUNTER_ACCESS_DENIED: 'Chỉ Bác sĩ được phân công phụ trách phòng khám mới có quyền khám cho lượt khám này.',
  VISIT_INVALID_STATUS: 'Trạng thái lượt khám không hợp lệ để thực hiện thao tác.',
  VISIT_NOT_FOUND: 'Không tìm thấy thông tin lượt khám trong hệ thống.',

  // Doctor & Room assignment errors
  DOCTOR_INACTIVE: 'Bác sĩ đang ở trạng thái ngừng hoạt động nên không thể nhận bệnh nhân.',
  DOCTOR_NOT_FOUND: 'Không tìm thấy thông tin bác sĩ trong hệ thống.',
  DOCTOR_NOT_ASSIGNED_TO_ROOM: 'Bác sĩ chưa được phân công phòng khám đang hoạt động.',
  DOCTOR_ROOM_ASSIGNMENT_NOT_FOUND: 'Bác sĩ chưa được phân công phòng khám đang hoạt động.',
  DOCTOR_ROOM_ASSIGNMENT_CONFLICT: 'Bác sĩ đã được phân công vào một phòng khám khác trong cùng ca trực.',
  DOCTOR_SCHEDULE_UNAVAILABLE: 'Lịch trực của bác sĩ hiện không khả dụng.',
  DOCTOR_NOT_WORKING: 'Bác sĩ không có lịch làm việc trong ngày này.',

  // Clinical & Patient status
  PATIENT_INACTIVE: 'Bệnh nhân đang ở trạng thái ngừng hoạt động nên không thể tiếp nhận.',
  PATIENT_NOT_FOUND: 'Không tìm thấy thông tin bệnh nhân trong hệ thống.',
  MEDICAL_RECORD_NOT_LOCKED: 'Bệnh án chưa được ký hoặc khóa trước khi hoàn tất.',
}

export const handleQueueApiError = (error, defaultMessage = 'Thao tác không thành công') => {
  const apiError = normalizeApiError(error, defaultMessage)
  const { code, status, message: rawMessage } = apiError

  // 1. Check if backend returned a specific detailed message
  const trimmedMsg = typeof rawMessage === 'string' ? rawMessage.trim() : ''
  const specificDetail = QUEUE_DETAIL_TRANSLATIONS[trimmedMsg]

  // 2. Check domain code mapping
  const codeMessage = QUEUE_ERROR_MESSAGES[code]

  let title = 'Thông báo hệ thống'
  let detail = specificDetail || codeMessage || apiError.firstFieldError || defaultMessage

  switch (status) {
    case 400:
      title = '400 - Dữ liệu không hợp lệ'
      detail = specificDetail || codeMessage || apiError.firstFieldError || 'Thông tin gửi lên bị thiếu hoặc không đúng định dạng.'
      break
    case 401:
      title = '401 - Hết phiên đăng nhập'
      detail = 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.'
      break
    case 403:
      title = '403 - Không có quyền thao tác'
      detail = specificDetail || codeMessage || 'Tài khoản của bạn không được cấp quyền thực hiện chức năng này.'
      break
    case 404:
      title = '404 - Không tìm thấy dữ liệu'
      detail = specificDetail || codeMessage || 'Dữ liệu Lịch hẹn, Hàng đợi hoặc Lượt khám không tồn tại.'
      break
    case 409:
      title = '409 - Hàng đợi / Chu trình vận hành'
      detail = specificDetail || codeMessage || 'Hiện tại bệnh nhân đã ở trong hàng đợi hoặc chưa đúng chu trình khám.'
      break
    default:
      if (status >= 500) {
        title = '500 - Lỗi máy chủ'
        detail = specificDetail || codeMessage || 'Hệ thống máy chủ gặp sự cố. Vui lòng thử lại sau.'
      }
      break
  }

  notification.warning({
    message: title,
    description: detail,
    placement: 'topRight',
    duration: 5,
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
    canViewBoard: canViewQueue || canReadApp || canCreateApp || isAdmin || isDoctor || isReceptionist,
    canViewMyQueue: (canViewQueue || isDoctor) && isDoctor,
    canCheckIn: canCreateQueue || canUpdateQueueStatus || isAdmin || isReceptionist,
    canCallNext: canCallNext || canUpdateQueueStatus || isAdmin || isReceptionist || isDoctor,
    canSkip: canUpdateQueueStatus || isAdmin || isReceptionist || isDoctor,
    canComplete: canUpdateQueueStatus || isAdmin || isDoctor,
    canUpdateStatus: canUpdateQueueStatus || isAdmin || isDoctor,
    canChangeResultStatus: canUpdateQueueStatus || isAdmin || isDoctor,
    canManageWalkIn: canCreateQueue || canCreateApp || isAdmin || isReceptionist,
    canCreateAppointment: canCreateApp || isAdmin || isReceptionist,
    canReadAppointment: canReadApp || isAdmin || isDoctor || isReceptionist,
    canUpdateAppointment: canUpdateApp || isAdmin || isReceptionist,
    canDeleteAppointment: canDeleteApp || isAdmin || isReceptionist,
    isDoctorOnly: isDoctor && !isAdmin && !isReceptionist,
    isAdmin,
    isReceptionist,
    isDoctor,
  }
}
