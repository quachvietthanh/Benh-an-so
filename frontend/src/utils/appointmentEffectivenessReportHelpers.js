import dayjs from 'dayjs'

export const MAX_REPORT_RANGE_DAYS = 366

export const ALL_APPOINTMENT_STATUSES = [
  'SCHEDULED',
  'CONFIRMED',
  'CHECKED_IN',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
  'NO_SHOW',
]

export const ALL_BOOKING_CHANNELS = ['ONLINE_PORTAL', 'RECEPTION_COUNTER']

/**
 * Kiểm tra tính hợp lệ của khoảng thời gian báo cáo
 */
export function validateDateRange(from, to) {
  if (!from || !to) {
    return {
      valid: false,
      error: 'Vui lòng chọn khoảng thời gian báo cáo.',
      code: 'MISSING_DATE_RANGE',
    }
  }

  const start = dayjs(from)
  const end = dayjs(to)

  if (!start.isValid() || !end.isValid()) {
    return {
      valid: false,
      error: 'Khoảng thời gian không hợp lệ. Vui lòng kiểm tra lại.',
      code: 'INVALID_DATE',
    }
  }

  if (start.isAfter(end, 'day')) {
    return {
      valid: false,
      error: 'Ngày bắt đầu không được lớn hơn ngày kết thúc.',
      code: 'START_AFTER_END',
    }
  }

  const inclusiveDays = end.diff(start, 'day') + 1
  if (inclusiveDays > MAX_REPORT_RANGE_DAYS) {
    return {
      valid: false,
      error: 'Khoảng thời gian báo cáo không được vượt quá 366 ngày.',
      code: 'DATE_RANGE_TOO_LONG',
    }
  }

  return {
    valid: true,
    error: '',
    from: start.format('YYYY-MM-DD'),
    to: end.format('YYYY-MM-DD'),
    inclusiveDays,
  }
}

/**
 * Ánh xạ mã trạng thái lịch hẹn sang tiếng Việt chuẩn y tế
 */
export function formatStatus(status) {
  switch (status) {
    case 'COMPLETED':
      return 'Đã khám xong'
    case 'CANCELLED':
      return 'Đã hủy'
    case 'NO_SHOW':
      return 'Không đến khám'
    case 'CHECKED_IN':
      return 'Đã có mặt'
    case 'IN_PROGRESS':
      return 'Đang khám'
    case 'SCHEDULED':
      return 'Đã đặt lịch'
    case 'CONFIRMED':
      return 'Đã xác nhận'
    default:
      return status || 'Chưa xác định'
  }
}

/**
 * Ánh xạ kênh đặt lịch sang tiếng Việt chuẩn
 */
export function formatBookingChannel(channel) {
  switch (channel) {
    case 'ONLINE_PORTAL':
      return 'Cổng bệnh nhân trực tuyến'
    case 'RECEPTION_COUNTER':
      return 'Đăng ký tại quầy'
    default:
      return channel || 'Chưa xác định'
  }
}

/**
 * Bảng màu có ý nghĩa nghiệp vụ rõ ràng cho từng trạng thái lịch hẹn:
 * - COMPLETED: Xanh lá (tích cực, khám thành công)
 * - CANCELLED: Đỏ (tiêu cực, hủy hẹn)
 * - NO_SHOW: Cam đậm / Đỏ cam (tiêu cực, chỉ số trọng tâm đo hiệu quả nhắc lịch)
 * - CHECKED_IN: Xanh dương (đang xử lý / đã có mặt tiếp đón)
 * - IN_PROGRESS: Xanh cyan (đang trong phòng khám)
 * - SCHEDULED: Xám nhạt (chờ hẹn)
 * - CONFIRMED: Tím / Vàng nhạt (đã xác nhận)
 */
export function getStatusColor(status) {
  switch (status) {
    case 'COMPLETED':
      return '#059669' // Xanh lá
    case 'CANCELLED':
      return '#dc2626' // Đỏ
    case 'NO_SHOW':
      return '#ea580c' // Cam đậm (chỉ số cảnh báo quan trọng)
    case 'CHECKED_IN':
      return '#2563eb' // Xanh dương
    case 'IN_PROGRESS':
      return '#0891b2' // Xanh cyan
    case 'SCHEDULED':
      return '#64748b' // Xám
    case 'CONFIRMED':
      return '#7c3aed' // Tím
    default:
      return '#94a3b8'
  }
}

/**
 * Màu đại diện cho kênh đặt lịch để vẽ biểu đồ so sánh đối chiếu
 */
export function getChannelColor(channel) {
  switch (channel) {
    case 'ONLINE_PORTAL':
      return '#2563eb' // Xanh dương (công nghệ, trực tuyến)
    case 'RECEPTION_COUNTER':
      return '#059669' // Xanh ngọc / xanh lá (truyền thống, tại quầy)
    default:
      return '#64748b'
  }
}

/**
 * Định dạng phần trăm hiển thị (2 chữ số thập phân)
 */
export function formatPercentage(val) {
  if (val === null || val === undefined || isNaN(val)) return '0.00%'
  return `${Number(val).toFixed(2)}%`
}

/**
 * Gộp items theo Trạng thái (status)
 * Tính tổng số lượng (count) và tính lại percentage của nhóm gộp = tổng count nhóm / total * 100
 * Giữ nguyên percentage gốc từ Backend cho từng kênh con bên trong
 */
export function groupItemsByStatus(items = [], total = 0) {
  const safeItems = Array.isArray(items) ? items : []
  const resolvedTotal = Number(total) > 0
    ? Number(total)
    : safeItems.reduce((sum, item) => sum + (Number(item.count) || 0), 0)

  const groupMap = new Map()

  safeItems.forEach((item) => {
    if (!item?.status) return
    const st = item.status
    if (!groupMap.has(st)) {
      groupMap.set(st, {
        status: st,
        statusLabel: formatStatus(st),
        color: getStatusColor(st),
        count: 0,
        channels: [],
      })
    }

    const group = groupMap.get(st)
    const itemCount = Number(item.count) || 0
    group.count += itemCount

    group.channels.push({
      bookingChannel: item.bookingChannel,
      channelLabel: formatBookingChannel(item.bookingChannel),
      count: itemCount,
      percentage: Number(item.percentage) || 0, // Giữ nguyên percentage gốc từ Backend
    })
  })

  // Sắp xếp các kênh con bên trong (ONLINE_PORTAL trước, RECEPTION_COUNTER sau)
  groupMap.forEach((group) => {
    group.channels.sort((a, b) =>
      String(a.bookingChannel).localeCompare(String(b.bookingChannel))
    )
    group.percentage = resolvedTotal > 0
      ? Number(((group.count * 100) / resolvedTotal).toFixed(2))
      : 0
  })

  // Sắp xếp theo thứ tự ưu tiên chuẩn hóa hoặc thứ tự enum
  const statusOrder = [
    'COMPLETED',
    'CHECKED_IN',
    'IN_PROGRESS',
    'CONFIRMED',
    'SCHEDULED',
    'NO_SHOW',
    'CANCELLED',
  ]

  return Array.from(groupMap.values()).sort((a, b) => {
    const idxA = statusOrder.indexOf(a.status)
    const idxB = statusOrder.indexOf(b.status)
    if (idxA !== -1 && idxB !== -1) return idxA - idxB
    if (idxA !== -1) return -1
    if (idxB !== -1) return 1
    return b.count - a.count
  })
}

/**
 * Gộp items theo Kênh đặt lịch (bookingChannel)
 * Phục vụ mục đích đo hiệu quả các chức năng đặt lịch trực tuyến đã đầu tư vs tại quầy
 */
export function groupItemsByChannel(items = [], total = 0) {
  const safeItems = Array.isArray(items) ? items : []
  const resolvedTotal = Number(total) > 0
    ? Number(total)
    : safeItems.reduce((sum, item) => sum + (Number(item.count) || 0), 0)

  const groupMap = new Map()

  safeItems.forEach((item) => {
    if (!item?.bookingChannel) return
    const ch = item.bookingChannel
    if (!groupMap.has(ch)) {
      groupMap.set(ch, {
        bookingChannel: ch,
        channelLabel: formatBookingChannel(ch),
        color: getChannelColor(ch),
        count: 0,
        statuses: [],
      })
    }

    const group = groupMap.get(ch)
    const itemCount = Number(item.count) || 0
    group.count += itemCount

    group.statuses.push({
      status: item.status,
      statusLabel: formatStatus(item.status),
      color: getStatusColor(item.status),
      count: itemCount,
      percentage: Number(item.percentage) || 0, // Giữ nguyên percentage gốc từ Backend
    })
  })

  groupMap.forEach((group) => {
    group.statuses.sort((a, b) => b.count - a.count)
    group.percentage = resolvedTotal > 0
      ? Number(((group.count * 100) / resolvedTotal).toFixed(2))
      : 0
  })

  // Đảm bảo thứ tự ONLINE_PORTAL trước, RECEPTION_COUNTER sau
  return Array.from(groupMap.values()).sort((a, b) =>
    String(a.bookingChannel).localeCompare(String(b.bookingChannel))
  )
}

/**
 * Trích xuất các chỉ số KPI then chốt từ danh sách items và total
 */
export function extractAppointmentKpis(items = [], total = 0) {
  const safeItems = Array.isArray(items) ? items : []
  const resolvedTotal = Number(total) || 0

  let noShowCount = 0
  let completedCount = 0
  let cancelledCount = 0

  safeItems.forEach((item) => {
    const count = Number(item.count) || 0
    if (item.status === 'NO_SHOW') {
      noShowCount += count
    } else if (item.status === 'COMPLETED') {
      completedCount += count
    } else if (item.status === 'CANCELLED') {
      cancelledCount += count
    }
  })

  const noShowPercentage = resolvedTotal > 0
    ? Number(((noShowCount * 100) / resolvedTotal).toFixed(2))
    : 0

  const completedPercentage = resolvedTotal > 0
    ? Number(((completedCount * 100) / resolvedTotal).toFixed(2))
    : 0

  const cancelledPercentage = resolvedTotal > 0
    ? Number(((cancelledCount * 100) / resolvedTotal).toFixed(2))
    : 0

  return {
    total: resolvedTotal,
    noShowCount,
    noShowPercentage,
    completedCount,
    completedPercentage,
    cancelledCount,
    cancelledPercentage,
    // Gợi ý UX: Ngưỡng cảnh báo > 15% là quy ước hiển thị trực quan ở Frontend, KHÔNG PHẢI business rule chính thức từ Backend
    isNoShowWarning: noShowPercentage > 15,
  }
}

/**
 * Xử lý ánh xạ lỗi HTTP chuẩn mực tiếng Việt
 */
export function getAppointmentEffectivenessErrorMessage(error) {
  const status = error?.response?.status || error?.status
  const data = error?.response?.data || {}
  const code = data.code || ''
  const msg = String(data.message || '')

  if (status === 400) {
    if (
      code === 'DATE_RANGE_TOO_LONG' ||
      msg.includes('366 days') ||
      msg.includes('vượt quá 366')
    ) {
      return 'Khoảng thời gian báo cáo không được vượt quá 366 ngày.'
    }
    if (
      msg.includes('before or equal to to') ||
      msg.includes('lớn hơn ngày kết thúc')
    ) {
      return 'Ngày bắt đầu không được lớn hơn ngày kết thúc.'
    }
    if (
      code === 'MISSING_PARAMETER' ||
      msg.includes('required') ||
      msg.includes('bắt buộc')
    ) {
      return 'Vui lòng chọn đầy đủ khoảng thời gian báo cáo.'
    }
    if (
      msg.includes('bookingChannel must be one of') ||
      msg.includes('bookingChannel')
    ) {
      return 'Kênh đặt lịch không hợp lệ. Vui lòng chọn lại.'
    }
    return 'Tham số lọc không hợp lệ. Vui lòng kiểm tra lại.'
  }

  if (status === 403) {
    return 'Bạn không có quyền xem báo cáo này.'
  }

  if (status === 401) {
    return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
  }

  return 'Không thể tải báo cáo. Vui lòng thử lại.'
}
