import dayjs from 'dayjs'

export const SLOT_STATUS = Object.freeze({
  AVAILABLE: 'AVAILABLE',
  BOOKED: 'BOOKED',
  ON_LEAVE: 'ON_LEAVE',
  OFF_DUTY: 'OFF_DUTY',
  PAST: 'PAST',
})

export const SLOT_CONFIG = Object.freeze({
  AVAILABLE: {
    label: 'Còn trống',
    tagColor: 'success',
    bgColor: '#f0fdf4',
    borderColor: '#86efac',
    textColor: '#15803d',
    actionText: '+ Đặt lịch',
  },
  BOOKED: {
    label: 'Đã đặt',
    tagColor: 'blue',
    bgColor: '#eff6ff',
    borderColor: '#93c5fd',
    textColor: '#1d4ed8',
    actionText: 'Xem chi tiết',
  },
  ON_LEAVE: {
    label: 'Nghỉ phép',
    tagColor: 'warning',
    bgColor: '#fffbeb',
    borderColor: '#fcd34d',
    textColor: '#b45309',
    actionText: 'Khoảng nghỉ',
  },
  OFF_DUTY: {
    label: 'Không có ca',
    tagColor: 'default',
    bgColor: '#f8fafc',
    borderColor: '#e2e8f0',
    textColor: '#94a3b8',
    actionText: 'Không làm việc',
  },
  PAST: {
    label: 'Đã qua',
    tagColor: 'default',
    bgColor: '#f1f5f9',
    borderColor: '#cbd5e1',
    textColor: '#94a3b8',
    actionText: 'Hết hạn đặt',
  },
})

export const DAY_OF_WEEK_LABELS = Object.freeze({
  MONDAY: 'Thứ Hai',
  TUESDAY: 'Thứ Ba',
  WEDNESDAY: 'Thứ Tư',
  THURSDAY: 'Thứ Năm',
  FRIDAY: 'Thứ Sáu',
  SATURDAY: 'Thứ Bảy',
  SUNDAY: 'Chủ Nhật',
})

export const formatAppointmentStatusVi = (status) => {
  switch (String(status || '').toUpperCase()) {
    case 'SCHEDULED':
      return { label: 'Đã đặt hẹn', color: 'blue' }
    case 'CONFIRMED':
      return { label: 'Đã xác nhận', color: 'cyan' }
    case 'CHECKED_IN':
      return { label: 'Đã tiếp nhận', color: 'purple' }
    case 'IN_PROGRESS':
      return { label: 'Đang khám', color: 'processing' }
    case 'COMPLETED':
      return { label: 'Đã hoàn thành', color: 'success' }
    case 'CANCELLED':
      return { label: 'Đã hủy', color: 'error' }
    case 'NO_SHOW':
      return { label: 'Không đến khám', color: 'default' }
    case 'WAITING':
      return { label: 'Chờ khám', color: 'gold' }
    default:
      return { label: status || 'Đã đặt hẹn', color: 'blue' }
  }
}

export const canAccessDoctorWeeklyTable = (user) => {
  if (!user) return false

  const userRoles = (Array.isArray(user.roles) ? user.roles : [user.role || user.roles])
    .filter(Boolean)
    .map((r) => String(r).toLowerCase().replace(/^role_/, ''))

  const userPerms = (Array.isArray(user.permissions) ? user.permissions : [user.permission || user.permissions])
    .filter(Boolean)
    .map((p) => String(p).toUpperCase().replace(/^PERMISSION_/, ''))

  const isAdmin = userRoles.includes('admin')
  const isManager = userRoles.includes('manager') || userRoles.includes('clinic_manager')
  const isReceptionist = userRoles.includes('receptionist')
  const isDoctor = userRoles.includes('doctor')
  const isPharmacist = userRoles.includes('pharmacist')

  if (isPharmacist && !isAdmin && !isReceptionist) {
    return false
  }

  const hasAppointmentRead = userPerms.includes('APPOINTMENT_READ') || isAdmin || isReceptionist || isManager || isDoctor
  return hasAppointmentRead
}

export const evaluateSlotAction = (slot, doctorName, dateStr) => {
  if (!slot) {
    return {
      canBook: false,
      reason: 'INVALID',
      message: 'Không tìm thấy thông tin khung giờ.',
    }
  }

  if (slot.status === SLOT_STATUS.ON_LEAVE) {
    const leaveDetail = slot.timeOffReason ? ` (${slot.timeOffReason})` : ''
    return {
      canBook: false,
      reason: 'ON_LEAVE',
      message: `Bác sĩ ${doctorName || ''} đang trong thời gian nghỉ phép${leaveDetail}, không thể đặt lịch hẹn trong khoảng thời gian này.`,
    }
  }

  if (slot.status === SLOT_STATUS.OFF_DUTY) {
    return {
      canBook: false,
      reason: 'OFF_DUTY',
      message: `Bác sĩ ${doctorName || ''} không có lịch làm việc trong khung giờ này.`,
    }
  }

  if (slot.status === SLOT_STATUS.PAST) {
    return {
      canBook: false,
      reason: 'PAST',
      message: 'Khung giờ này đã trôi qua trong quá khứ, không thể đặt lịch mới.',
    }
  }

  if (slot.status === SLOT_STATUS.BOOKED) {
    return {
      canBook: false,
      isViewable: true,
      reason: 'BOOKED',
      message: 'Khung giờ này đã có bệnh nhân đặt lịch hẹn.',
      appointment: slot.appointment,
    }
  }

  if (slot.status === SLOT_STATUS.AVAILABLE) {
    return {
      canBook: true,
      reason: 'AVAILABLE',
      message: 'Khung giờ còn trống, có thể đặt lịch.',
    }
  }

  return {
    canBook: false,
    reason: slot.status,
    message: 'Khung giờ hiện không khả dụng.',
  }
}

export const formatSlotTimeRange = (slotStartTime, slotEndTime) => {
  const start = slotStartTime ? String(slotStartTime).slice(0, 5) : '--:--'
  const end = slotEndTime ? String(slotEndTime).slice(0, 5) : '--:--'
  return `${start} - ${end}`
}

export const formatWeekRange = (startDate, endDate) => {
  if (!startDate || !endDate) return ''
  const s = dayjs(startDate)
  const e = dayjs(endDate)
  return `Tuần từ ${s.format('DD/MM/YYYY')} đến ${e.format('DD/MM/YYYY')}`
}

export const cleanWeeklyTableErrorMessage = (error) => {
  const code = error?.response?.data?.code || error?.apiError?.code
  const status = error?.response?.status || error?.apiError?.status
  const backendMsg = error?.response?.data?.message || error?.apiError?.message

  if (code === 'DOCTOR_NOT_WORKING' || backendMsg?.includes('not working') || backendMsg?.includes('QTN-30')) {
    return 'Bác sĩ không làm việc hoặc đang trong khoảng nghỉ tại thời điểm này.'
  }

  if (code === 'APPOINTMENT_CONFLICT' || backendMsg?.includes('conflict') || backendMsg?.includes('QTN-04')) {
    return 'Khung giờ này đã bị trùng với một lịch hẹn khác của bác sĩ.'
  }

  if (status === 403 || code === 'ACCESS_DENIED') {
    return 'Bạn không có quyền xem hoặc thao tác trên bảng lịch tuần bác sĩ (403 Forbidden).'
  }

  if (status === 400 || code === 'VALIDATION_FAILED') {
    return backendMsg || 'Thông tin đặt lịch không hợp lệ. Vui lòng kiểm tra lại.'
  }

  return backendMsg || 'Đã có lỗi xảy ra khi tải hoặc cập nhật bảng lịch tuần. Vui lòng thử lại.'
}
