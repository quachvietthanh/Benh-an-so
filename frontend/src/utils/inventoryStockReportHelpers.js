import dayjs from 'dayjs'

export const MAX_REPORT_RANGE_DAYS = 366

/**
 * Kiểm tra tính hợp lệ của khoảng thời gian báo cáo xuất nhập tồn
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
 * Đối chiếu công thức cân đối kho:
 * Tồn cuối lý thuyết = Tồn đầu + Nhập - Cấp phát + Trả lại + Điều chỉnh
 *
 * Lưu ý: Kết quả này CHỈ dùng để cảnh báo / kiểm toán nội bộ giao diện.
 * Giá trị hiển thị chính thức của tồn cuối kỳ LUÔN LÀ closingQuantity trả về từ Backend.
 */
export function validateClosingBalance(item) {
  if (!item) {
    return {
      hasDiscrepancy: false,
      expectedClosing: 0,
      actualClosing: 0,
      difference: 0,
    }
  }

  const opening = Number(item.openingQuantity) || 0
  const received = Number(item.receivedQuantity) || 0
  const dispensed = Number(item.dispensedQuantity) || 0
  const returned = Number(item.returnedQuantity) || 0
  const adjusted = Number(item.adjustedQuantity) || 0

  const expectedClosing = opening + received - dispensed + returned + adjusted
  const actualClosing = Number(item.closingQuantity) || 0
  const difference = actualClosing - expectedClosing
  const hasDiscrepancy = expectedClosing !== actualClosing

  return {
    hasDiscrepancy,
    expectedClosing,
    actualClosing,
    difference,
  }
}

/**
 * Định dạng số lượng hiển thị (dấu phân cách hàng nghìn, hỗ trợ hiển thị dấu +/- cho điều chỉnh)
 */
export function formatQuantity(value, options = {}) {
  if (value === null || value === undefined || isNaN(value)) {
    return '0'
  }

  const num = Number(value)
  const formatted = Math.abs(num).toLocaleString('vi-VN')

  if (options.showSign) {
    if (num > 0) return `+${formatted}`
    if (num < 0) return `-${formatted}`
    return '0'
  }

  return num.toLocaleString('vi-VN')
}

/**
 * Trích xuất tên tệp từ header Content-Disposition
 */
export function extractFilenameFromHeader(contentDisposition, fallback = 'stock-in-out-report.csv') {
  if (!contentDisposition) return fallback

  const match = contentDisposition.match(/filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?/i)
  if (match && match[1]) {
    try {
      return decodeURIComponent(match[1])
    } catch {
      return match[1]
    }
  }

  return fallback
}

/**
 * Kích hoạt tải tệp CSV dạng Blob từ trình duyệt
 */
export function downloadCsvBlob(blobData, filename = 'stock-in-out-report.csv') {
  if (typeof window === 'undefined' || !blobData) return

  const blob = blobData instanceof Blob
    ? blobData
    : new Blob([blobData], { type: 'text/csv; charset=UTF-8' })

  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()

  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * Trích xuất nội dung thông báo lỗi từ Blob response (khi export bị lỗi 400/403)
 */
export async function extractBlobErrorMessage(error) {
  if (error?.response?.data instanceof Blob) {
    try {
      const text = await error.response.data.text()
      const json = JSON.parse(text)
      return getInventoryStockReportErrorMessage({
        response: {
          status: error.response.status,
          data: json,
        },
      })
    } catch {
      // Fallback nếu không parse được JSON
    }
  }
  return getInventoryStockReportErrorMessage(error)
}

/**
 * Tính toán các chỉ số tổng hợp kho cho toàn kỳ
 */
export function calculateStockSummary(items = []) {
  const safeItems = Array.isArray(items) ? items : []

  let totalOpening = 0
  let totalReceived = 0
  let totalDispensed = 0
  let totalReturned = 0
  let totalAdjusted = 0
  let totalClosing = 0
  let discrepancyCount = 0

  safeItems.forEach((item) => {
    totalOpening += Number(item.openingQuantity) || 0
    totalReceived += Number(item.receivedQuantity) || 0
    totalDispensed += Number(item.dispensedQuantity) || 0
    totalReturned += Number(item.returnedQuantity) || 0
    totalAdjusted += Number(item.adjustedQuantity) || 0
    totalClosing += Number(item.closingQuantity) || 0

    const balanceCheck = validateClosingBalance(item)
    if (balanceCheck.hasDiscrepancy) {
      discrepancyCount += 1
    }
  })

  return {
    totalMedicines: safeItems.length,
    totalOpening,
    totalReceived,
    totalDispensed,
    totalReturned,
    totalAdjusted,
    totalClosing,
    discrepancyCount,
    hasAnyDiscrepancy: discrepancyCount > 0,
  }
}

/**
 * Ánh xạ mã lỗi HTTP sang thông báo chuẩn tiếng Việt
 */
export function getInventoryStockReportErrorMessage(error) {
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
      return 'Khoảng thời gian không hợp lệ. Khoảng cách giữa hai ngày tối đa là 366 ngày.'
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
    return 'Khoảng thời gian không hợp lệ. Vui lòng kiểm tra lại ngày bắt đầu/kết thúc (tối đa 366 ngày).'
  }

  if (status === 403) {
    return 'Bạn không có quyền xem báo cáo xuất nhập tồn kho dược.'
  }

  if (status === 401) {
    return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
  }

  return 'Không thể tải báo cáo xuất nhập tồn. Vui lòng thử lại sau.'
}
