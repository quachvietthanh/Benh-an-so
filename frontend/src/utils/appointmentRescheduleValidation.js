import dayjs from 'dayjs'

/**
 * Quy tắc nghiệp vụ và kiểm tra hợp lệ cho chức năng Đổi lịch hẹn tại quầy (NCL-03-CN-007)
 * Tuân thủ QTN-04 (Tránh trùng lịch bác sĩ)
 * Tuân thủ QTN-30 (Trong lịch làm việc bác sĩ)
 * Tuân thủ TC-01, TC-02, TC-03, TC-04
 */

export const PRESET_RESCHEDULE_REASONS = [
  'Bệnh nhân báo bận đột xuất / xin dời giờ',
  'Bác sĩ có lịch bận đột xuất / điều chỉnh ca trực',
  'Bệnh nhân đến muộn xin lùi giờ khám',
  'Phòng khám điều phối lại lịch làm việc',
  'Chuyển sang bác sĩ chuyên khoa phù hợp hơn',
]

/**
 * Kiểm tra hợp lệ lý do dời lịch hẹn (TC-01, TC-03)
 * @param {string} reason 
 * @returns {{ valid: boolean, error: string | null, reason: string }}
 */
export const validateRescheduleReason = (reason) => {
  if (reason == null) {
    return {
      valid: false,
      error: 'Vui lòng nhập lý do dời lịch hẹn (1 - 500 ký tự).',
      reason: '',
    }
  }

  const trimmed = String(reason).trim()

  if (trimmed.length === 0) {
    return {
      valid: false,
      error: 'Lý do dời lịch hẹn không được để trống hoặc chỉ chứa khoảng trắng.',
      reason: '',
    }
  }

  if (trimmed.length > 500) {
    return {
      valid: false,
      error: `Lý do dời lịch không được vượt quá 500 ký tự (hiện tại: ${trimmed.length} ký tự).`,
      reason: trimmed,
    }
  }

  return {
    valid: true,
    error: null,
    reason: trimmed,
  }
}

/**
 * Kiểm tra tính hợp lệ của thời gian bắt đầu và kết thúc mới (TC-03)
 * @param {string | Date | dayjs.Dayjs} startTime 
 * @param {string | Date | dayjs.Dayjs} endTime 
 * @param {dayjs.Dayjs} [now]
 * @returns {{ valid: boolean, error: string | null }}
 */
export const validateRescheduleTime = (startTime, endTime, now = dayjs()) => {
  if (!startTime || !endTime) {
    return {
      valid: false,
      error: 'Thời gian bắt đầu và thời gian kết thúc là bắt buộc.',
    }
  }

  const start = dayjs(startTime)
  const end = dayjs(endTime)

  if (!start.isValid() || !end.isValid()) {
    return {
      valid: false,
      error: 'Thời gian bắt đầu hoặc thời gian kết thúc không đúng định dạng.',
    }
  }

  if (start.isBefore(now)) {
    return {
      valid: false,
      error: 'Thời gian bắt đầu lịch hẹn phải ở trong tương lai.',
    }
  }

  if (!end.isAfter(start)) {
    return {
      valid: false,
      error: 'Thời gian kết thúc phải sau thời gian bắt đầu.',
    }
  }

  return {
    valid: true,
    error: null,
  }
}

/**
 * Kiểm tra điều kiện lịch hẹn có được phép dời lịch tại quầy hay không (TC-01, TC-02)
 * @param {Object} appointment
 * @param {dayjs.Dayjs} [now]
 * @returns {{ allowed: boolean, reason: string | null }}
 */
export const canRescheduleAppointment = (appointment, now = dayjs()) => {
  if (!appointment) {
    return {
      allowed: false,
      reason: 'Không tìm thấy thông tin lịch hẹn.',
    }
  }

  const status = appointment.status

  // TC-02: Kiểm tra các trạng thái từ chối
  if (status === 'CHECKED_IN') {
    return {
      allowed: false,
      reason: 'Lịch hẹn đã được tiếp nhận khám (CHECKED_IN), không thể dời lịch.',
    }
  }

  if (status === 'COMPLETED') {
    return {
      allowed: false,
      reason: 'Lịch hẹn đã hoàn tất ca khám, không thể dời lịch.',
    }
  }

  if (status === 'NO_SHOW') {
    return {
      allowed: false,
      reason: 'Lịch hẹn đã bị đánh dấu không đến khám, không thể dời lịch. Vui lòng tạo lịch hẹn mới.',
    }
  }

  if (status === 'CANCELLED') {
    return {
      allowed: false,
      reason: 'Lịch hẹn đã bị hủy trước đó, không thể dời lịch. Vui lòng tạo lịch hẹn mới.',
    }
  }

  if (!['SCHEDULED', 'CONFIRMED'].includes(status)) {
    return {
      allowed: false,
      reason: 'Chỉ lịch hẹn ở trạng thái đã đặt (SCHEDULED) hoặc đã xác nhận (CONFIRMED) mới được dời lịch.',
    }
  }

  // TC-02: Kiểm tra nếu lịch hẹn đã quá giờ khám
  const timeVal = appointment.appointmentAt || appointment.startTime || appointment.date
  if (timeVal) {
    const appTime = dayjs(timeVal)
    if (appTime.isValid() && appTime.isBefore(now)) {
      return {
        allowed: false,
        reason: 'Lịch hẹn đã quá giờ khám. Không thể dời lịch hẹn trong quá khứ, vui lòng tạo lịch hẹn mới.',
      }
    }
  }

  return {
    allowed: true,
    reason: null,
  }
}

/**
 * Lấy thông báo hạn chế khi không thể dời lịch hẹn (TC-02)
 * @param {Object} appointment 
 * @param {dayjs.Dayjs} [now]
 * @returns {string | null}
 */
export const getRescheduleRestrictionMessage = (appointment, now = dayjs()) => {
  return canRescheduleAppointment(appointment, now).reason
}
