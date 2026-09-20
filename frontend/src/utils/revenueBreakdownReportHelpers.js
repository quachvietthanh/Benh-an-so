import dayjs from 'dayjs'

export const MAX_REPORT_RANGE_DAYS = 366

export const SERVICE_GROUP_COLORS = {
  EXAMINATION: '#2563eb', // Xanh dương
  LAB_TEST: '#059669',    // Xanh ngọc / Xanh lá
  IMAGING: '#d97706',     // Cam hổ phách
  MEDICATION: '#7c3aed',  // Tím
  OTHER: '#64748b',       // Xám đá
}

export const DEFAULT_SERVICE_GROUP_COLOR = '#94a3b8'

/**
 * Lấy mã màu sắc cố định theo mã nhóm dịch vụ
 * @param {string} groupCode
 * @returns {string} Hex color
 */
export const getServiceGroupColor = (groupCode) => {
  if (!groupCode || typeof groupCode !== 'string') {
    return DEFAULT_SERVICE_GROUP_COLOR
  }
  return SERVICE_GROUP_COLORS[groupCode.toUpperCase()] || DEFAULT_SERVICE_GROUP_COLOR
}

/**
 * Kiểm tra tính hợp lệ của khoảng thời gian báo cáo (from <= to, tối đa 366 ngày)
 * @param {string|Date|dayjs.Dayjs} from
 * @param {string|Date|dayjs.Dayjs} to
 * @returns {{ valid: boolean, error: string, from?: string, to?: string }}
 */
export const validateDateRange = (from, to) => {
  if (!from || !to) {
    return {
      valid: false,
      error: 'Vui lòng chọn khoảng thời gian báo cáo.',
    }
  }

  const fromDay = dayjs(from).startOf('day')
  const toDay = dayjs(to).startOf('day')

  if (!fromDay.isValid() || !toDay.isValid()) {
    return {
      valid: false,
      error: 'Khoảng thời gian không hợp lệ. Vui lòng kiểm tra lại.',
    }
  }

  if (fromDay.isAfter(toDay)) {
    return {
      valid: false,
      error: 'Ngày bắt đầu không được lớn hơn ngày kết thúc.',
    }
  }

  const daysInclusive = toDay.diff(fromDay, 'day') + 1
  if (daysInclusive > MAX_REPORT_RANGE_DAYS) {
    return {
      valid: false,
      error: 'Khoảng thời gian báo cáo không được vượt quá 366 ngày.',
    }
  }

  return {
    valid: true,
    error: '',
    from: fromDay.format('YYYY-MM-DD'),
    to: toDay.format('YYYY-MM-DD'),
  }
}

/**
 * Định dạng tiền tệ theo quy chuẩn VNĐ trong hệ thống (ví dụ: "45.000.000 đ")
 * @param {number|string|null|undefined} value
 * @param {string} currency
 * @returns {string}
 */
export const formatCurrency = (value, currency = 'VND') => {
  if (value === null || value === undefined || value === '') {
    return '0 đ'
  }
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '0 đ'
  }

  const formatted = Math.abs(num).toLocaleString('vi-VN')
  const sign = num < 0 ? '-' : ''
  return `${sign}${formatted} đ`
}

/**
 * Định dạng phần trăm với 2 chữ số thập phân (ví dụ: "22.22%")
 * @param {number|string|null|undefined} value
 * @returns {string}
 */
export const formatPercentage = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00%'
  }
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '0.00%'
  }
  return `${num.toFixed(2)}%`
}

/**
 * Ánh xạ phản hồi lỗi HTTP thành thông báo tiếng Việt thân thiện
 * @param {any} error
 * @returns {string}
 */
export const getRevenueBreakdownErrorMessage = (error) => {
  const status = error?.response?.status
  const data = error?.response?.data
  const errorCode = String(data?.code || data?.errorCode || '')
  const errorMsg = String(data?.message || '')

  if (status === 400) {
    if (errorCode === 'DATE_RANGE_TOO_LONG' || errorMsg.includes('366')) {
      return 'Khoảng thời gian báo cáo không được vượt quá 366 ngày.'
    }
    return 'Khoảng thời gian không hợp lệ. Vui lòng kiểm tra lại.'
  }

  if (status === 403) {
    return 'Bạn không có quyền xem báo cáo doanh thu này.'
  }

  return 'Không thể tải báo cáo. Vui lòng thử lại.'
}
