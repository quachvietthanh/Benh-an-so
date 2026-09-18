/**
 * Tiện ích hỗ trợ quản lý và theo dõi chỉ định cận lâm sàng chờ kết quả (NCL-04-CN-008).
 * Quy tắc: QTN-13, QTN-17
 * Tiêu chí chấp thuận: TC-01, TC-02, TC-03, TC-04
 */

export const PRESET_CANCEL_REASONS = [
  'Bệnh nhân từ chối thực hiện dịch vụ',
  'Bệnh nhân không đồng ý thực hiện',
  'Bác sĩ chỉ định nhầm / trùng lặp',
  'Bệnh nhân không đủ điều kiện sức khỏe để thực hiện',
  'Bác sĩ thay đổi phác đồ chẩn đoán',
  'Bệnh nhân chuyển viện / kết thúc sớm ca khám',
  'Khác',
]

/**
 * Kiểm tra hợp lệ lý do hủy chỉ định (TC-02, TC-04)
 * Yêu cầu: Không được để trống, không vượt quá 500 ký tự (theo đặc tả Backend V65)
 */
export function validateCancelReason(reason) {
  if (typeof reason !== 'string') {
    return { valid: false, error: 'Vui lòng nhập lý do hủy chỉ định cận lâm sàng.', reason: '', trimmedReason: '' }
  }
  const trimmed = reason.trim()
  if (!trimmed) {
    return { valid: false, error: 'Vui lòng nhập lý do hủy chỉ định cận lâm sàng.', reason: '', trimmedReason: '' }
  }
  if (trimmed.length > 500) {
    return { valid: false, error: 'Lý do hủy chỉ định không được vượt quá 500 ký tự.', reason: trimmed.slice(0, 500), trimmedReason: trimmed.slice(0, 500) }
  }
  return { valid: true, error: null, reason: trimmed, trimmedReason: trimmed }
}

export const SERVICE_TYPE_META = {
  LAB_TEST: { label: 'Xét nghiệm', color: 'cyan', badge: 'LAB' },
  IMAGING: { label: 'CĐ hình ảnh', color: 'geekblue', badge: 'IMG' },
  OTHER: { label: 'Thăm dò chức năng', color: 'purple', badge: 'FUNC' },
  XRAY: { label: 'X-Quang', color: 'geekblue', badge: 'XRAY' },
  CT: { label: 'Chụp CT', color: 'blue', badge: 'CT' },
  ULTRASOUND: { label: 'Siêu âm', color: 'cyan', badge: 'US' },
  MRI: { label: 'Chụp MRI', color: 'indigo', badge: 'MRI' },
  BLOOD: { label: 'Xét nghiệm Máu', color: 'magenta', badge: 'BLD' },
  BIOCHEMISTRY: { label: 'Sinh hóa máu', color: 'volcano', badge: 'BIO' },
  URINE: { label: 'Xét nghiệm Nước tiểu', color: 'gold', badge: 'URI' },
  ENDOSCOPY: { label: 'Nội soi', color: 'purple', badge: 'ENDO' },
  ECG: { label: 'Điện tim', color: 'green', badge: 'ECG' },
}

export function formatServiceTypeVi(type) {
  if (!type) return 'Chung'
  const key = String(type).toUpperCase()
  return SERVICE_TYPE_META[key]?.label || key
}

export function getServiceTypeColor(type) {
  if (!type) return 'default'
  const key = String(type).toUpperCase()
  return SERVICE_TYPE_META[key]?.color || 'default'
}

/**
 * Đánh giá mức độ cảnh báo theo thời gian chờ kết quả (phút)
 * < 30 phút: Bình thường (xanh / normal)
 * 30 - 60 phút: Đang chờ lâu (vàng cam / warning)
 * > 60 phút: Chờ rất lâu / Cần can thiệp (đỏ / danger)
 */
export function getWaitingDurationMeta(minutes, createdAt) {
  let min = 0
  if (minutes !== null && minutes !== undefined && !Number.isNaN(Number(minutes))) {
    min = Math.max(0, Number(minutes))
  } else if (createdAt) {
    const createdDate = new Date(createdAt)
    if (!Number.isNaN(createdDate.getTime())) {
      min = Math.max(0, Math.floor((Date.now() - createdDate.getTime()) / (60 * 1000)))
    }
  }

  const formatFriendlyDuration = (m) => {
    if (m >= 1440) {
      const days = Math.floor(m / 1440)
      const remHours = Math.floor((m % 1440) / 60)
      return remHours > 0 ? `${days} ngày ${remHours} giờ` : `${days} ngày`
    }
    if (m >= 60) {
      const hours = Math.floor(m / 60)
      const remainingMin = m % 60
      return remainingMin > 0 ? `${hours}h ${remainingMin}m` : `${hours} giờ`
    }
    return `${m} phút`
  }

  if (min >= 60) {
    return {
      minutes: min,
      text: formatFriendlyDuration(min),
      tagText: min === 60 ? 'Chờ 60 phút' : `Chờ ${formatFriendlyDuration(min)}`,
      level: 'danger',
      color: 'error',
      tagColor: 'volcano',
      isOverdue: true,
      alertMessage: 'Thời gian chờ vượt quá 60 phút',
    }
  }
  if (min >= 30) {
    return {
      minutes: min,
      text: `${min} phút`,
      tagText: `Chờ ${min} phút`,
      level: 'warning',
      color: 'warning',
      tagColor: 'orange',
      isOverdue: false,
      alertMessage: 'Thời gian chờ từ 30-60 phút',
    }
  }
  return {
    minutes: min,
    text: `${min} phút`,
    tagText: `Chờ ${min} phút`,
    level: 'normal',
    color: 'success',
    tagColor: 'blue',
    isOverdue: false,
    alertMessage: 'Trong ngưỡng thời gian thông thường',
  }
}

/**
 * Xử lý thông báo lỗi thân thiện chuẩn nghiệp vụ y tế
 */
export function cleanClinicalOrderErrorMessage(error, defaultMsg = 'Thao tác không thành công.') {
  if (!error) return defaultMsg
  const responseMsg = error?.response?.data?.message || error?.message || ''
  const errorCode = error?.response?.data?.code || error?.response?.data?.error || ''

  // QTN-13: Từ chối hủy khi đã có kết quả
  if (
    errorCode === 'CLINICAL_ORDER_HAS_RESULT' ||
    responseMsg.includes('đã có kết quả') ||
    responseMsg.includes('QTN-13') ||
    responseMsg.includes('has result') ||
    responseMsg.includes('result already exists')
  ) {
    return 'Không thể hủy chỉ định vì dịch vụ cận lâm sàng đã có kết quả thực hiện trong hệ thống (QTN-13).'
  }

  // QTN-17: Cảnh báo còn chỉ định treo khi ký bệnh án
  if (
    errorCode === 'PENDING_CLINICAL_ORDERS_WARNING' ||
    responseMsg.includes('chưa có kết quả') ||
    responseMsg.includes('QTN-17')
  ) {
    const base = responseMsg || 'Lượt khám còn chỉ định cận lâm sàng đang chờ kết quả. Vui lòng xác nhận trước khi tiếp tục'
    return base.includes('QTN-17') ? base : `${base} (QTN-17)`
  }

  if (
    errorCode === 'CLINICAL_ORDER_LOCKED_MEDICAL_RECORD' ||
    responseMsg.includes('locked')
  ) {
    return 'Không thể hủy chỉ định vì hồ sơ bệnh án đã được ký số và khóa nội dung.'
  }

  if (
    errorCode === 'CLINICAL_ORDER_INVALID_VISIT' ||
    responseMsg.includes('visit')
  ) {
    return 'Lượt khám không còn ở trạng thái hoạt động để thực hiện thao tác này.'
  }

  if (
    errorCode === 'UNAUTHORIZED_ACCESS' ||
    errorCode === 'ACCESS_DENIED' ||
    error?.response?.status === 403
  ) {
    return 'Bạn không có quyền thực hiện thao tác này.'
  }

  if (
    errorCode === 'CLINICAL_ORDER_NOT_FOUND' ||
    errorCode === 'CLINICAL_ORDER_ITEM_NOT_FOUND' ||
    error?.response?.status === 404
  ) {
    return 'Không tìm thấy thông tin phiếu hoặc dịch vụ chỉ định cận lâm sàng.'
  }

  if (responseMsg.includes('does not permit cancellation') || responseMsg.includes('invalid status')) {
    return 'Không thể hủy chỉ định cận lâm sàng ở trạng thái hiện tại.'
  }

  return responseMsg || defaultMsg
}
