import dayjs from 'dayjs'

export const TREATMENT_PLAN_PRESETS = [
  'Điều trị ngoại trú theo đơn thuốc, theo dõi diễn tiến lâm sàng.',
  'Theo dõi sát triệu chứng lâm sàng, tái khám đánh giá lại sau 7 ngày.',
  'Chuyển tuyến khám và điều trị chuyên khoa sâu theo đúng phân tuyến chuyên môn.',
  'Đề nghị nhập viện theo dõi và điều trị nội trú tại chuyên khoa.',
]

export const DOCTOR_INSTRUCTION_PRESETS = [
  'Uống nhiều nước (1.5 - 2 lít/ngày), nghỉ ngơi hợp lý, tránh thức khuya và lao động quá sức.',
  'Chế độ ăn thanh đạm, giảm muối, hạn chế dầu mỡ, kiêng rượu bia và các chất kích thích.',
  'Vận động nhẹ nhàng 30 phút mỗi ngày, theo dõi huyết áp và đường huyết định kỳ tại nhà.',
  'Uống thuốc đúng giờ, đúng liều lượng theo đơn đã kê; không tự ý ngưng hoặc đổi thuốc.',
]

export const DANGER_SIGNS_WARNING =
  '⚠️ CẢNH BÁO NGUY HIỂM - CẦN QUAY LẠI TÁI KHÁM NGAY NẾU:\n' +
  '• Sốt cao liên tục > 38.5°C không hạ sau khi dùng thuốc hạ sốt.\n' +
  '• Khó thở, thở gấp, đau tức ngực dữ dội hoặc vã mồ hôi lạnh.\n' +
  '• Nôn liên tục không giữ được thức ăn/thuốc, tiêu chảy mất nước nặng.\n' +
  '• Chóng mặt dữ dội, ngất xỉu, co giật hoặc có bất kỳ biểu hiện bất thường nào.'

export const QUICK_REVISIT_OFFSETS = [
  { days: 3, label: '+3 ngày' },
  { days: 7, label: '+7 ngày (1 tuần)' },
  { days: 14, label: '+14 ngày (2 tuần)' },
  { days: 30, label: '+30 ngày (1 tháng)' },
]

/**
 * Tính toán mốc ngày tái khám theo số ngày offset từ ngày khám (hoặc hôm nay)
 */
export const calculateRevisitDate = (baseDate, days) => {
  const base = baseDate && dayjs(baseDate).isValid() ? dayjs(baseDate) : dayjs()
  return base.add(days, 'day')
}

/**
 * Validate mốc tái khám không được trước ngày khám (tuân thủ Backend logic)
 */
export const validateRevisitDate = (revisitDate, visitDate) => {
  if (!revisitDate) {
    return { valid: true }
  }

  const revisit = dayjs(revisitDate).startOf('day')
  if (!revisit.isValid()) {
    return { valid: false, error: 'Mốc tái khám không đúng định dạng ngày hợp lệ.' }
  }

  const base = visitDate && dayjs(visitDate).isValid()
    ? dayjs(visitDate).startOf('day')
    : dayjs().startOf('day')

  if (revisit.isBefore(base)) {
    return {
      valid: false,
      error: 'Mốc tái khám không được trước ngày khám bệnh.',
    }
  }

  return { valid: true }
}

/**
 * Định dạng mốc tái khám hiển thị kèm thứ trong tuần và số ngày cách ngày khám
 */
export const formatRevisitDateDisplay = (revisitDate, visitDate) => {
  if (!revisitDate) return ''
  const revisit = dayjs(revisitDate)
  if (!revisit.isValid()) return ''

  const base = visitDate && dayjs(visitDate).isValid()
    ? dayjs(visitDate).startOf('day')
    : dayjs().startOf('day')

  const diffDays = revisit.startOf('day').diff(base, 'day')

  const dayOfWeekNames = [
    'Chủ Nhật',
    'Thứ Hai',
    'Thứ Ba',
    'Thứ Tư',
    'Thứ Năm',
    'Thứ Sáu',
    'Thứ Bảy',
  ]
  const dow = dayOfWeekNames[revisit.day()]
  const dateFormatted = revisit.format('DD/MM/YYYY')

  if (diffDays === 0) {
    return `${dow}, ngày ${dateFormatted} (Hôm nay)`
  }
  if (diffDays > 0) {
    return `${dow}, ngày ${dateFormatted} (sau ${diffDays} ngày)`
  }
  return `${dow}, ngày ${dateFormatted} (trước ${Math.abs(diffDays)} ngày)`
}

/**
 * Làm sạch và chuyển đổi thông điệp lỗi Backend sang tiếng Việt thân thiện
 */
export const cleanInstructionsErrorMessage = (error, fallback = 'Có lỗi khi cập nhật lời dặn và kế hoạch điều trị.') => {
  if (!error) return fallback

  const status = error?.response?.status || error?.status
  const backendCode =
    error?.response?.data?.code ||
    error?.response?.data?.error ||
    error?.code

  const rawMessage =
    error?.response?.data?.message ||
    error?.response?.data?.detail ||
    error?.message ||
    ''

  if (
    status === 409 ||
    backendCode === 'MEDICAL_RECORD_ALREADY_LOCKED' ||
    backendCode === 'MEDICAL_RECORD_SIGNED' ||
    rawMessage.includes('already locked') ||
    rawMessage.includes('đã được ký')
  ) {
    return 'Bệnh án đã được ký duyệt và khóa chỉnh sửa. Theo quy định QTN-18, vui lòng sử dụng chức năng "Lập bản đính chính" để sửa đổi nội dung chuyên môn.'
  }

  if (
    status === 403 ||
    backendCode === 'ACCESS_DENIED' ||
    rawMessage.includes('access denied') ||
    rawMessage.includes('không có quyền')
  ) {
    return 'Bạn không có quyền cập nhật lời dặn và kế hoạch điều trị cho lượt khám này theo quy định QTN-07.'
  }

  if (
    backendCode === 'VALIDATION_FAILED' ||
    rawMessage.includes('không được trước ngày khám') ||
    rawMessage.includes('before visit date')
  ) {
    return 'Mốc tái khám không được trước ngày khám bệnh.'
  }

  if (
    status === 404 ||
    backendCode === 'MEDICAL_RECORD_NOT_FOUND' ||
    rawMessage.includes('not found')
  ) {
    return 'Không tìm thấy hồ sơ bệnh án tương ứng để cập nhật.'
  }

  return rawMessage || fallback
}
