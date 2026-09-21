import dayjs from 'dayjs'

export const MAX_REPORT_RANGE_DAYS = 366

/**
 * Validate date range for disease pattern report (max 366 days, from <= to)
 * @param {string|Date|dayjs.Dayjs} from
 * @param {string|Date|dayjs.Dayjs} to
 * @returns {{ valid: boolean, error: string, from?: string, to?: string }}
 */
export const validateDateRange = (from, to) => {
  if (!from || !to) {
    return {
      valid: false,
      error: 'Vui lòng chọn khoảng thời gian.',
    }
  }

  const fromDay = dayjs(from).startOf('day')
  const toDay = dayjs(to).startOf('day')

  if (!fromDay.isValid() || !toDay.isValid()) {
    return {
      valid: false,
      error: 'Khoảng thời gian không hợp lệ.',
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
 * Format percentage with 2 decimal places
 * @param {number|string|null|undefined} value
 * @returns {string} e.g. "30.00%"
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
 * Extract filename from Content-Disposition header with fallback
 * @param {string} contentDisposition
 * @param {string} fallback
 * @returns {string}
 */
export const extractFilenameFromHeader = (contentDisposition, fallback = 'disease-pattern-report.csv') => {
  if (!contentDisposition || typeof contentDisposition !== 'string') {
    return fallback
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match && utf8Match[1]) {
    try {
      return decodeURIComponent(utf8Match[1].trim().replace(/^["']|["']$/g, ''))
    } catch {
      // ignore decode error and try standard match
    }
  }

  const standardMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  if (standardMatch && standardMatch[1]) {
    return standardMatch[1].trim().replace(/^["']|["']$/g, '')
  }

  return fallback
}

/**
 * Download CSV blob data in browser
 * @param {Blob|string|ArrayBuffer} blobData
 * @param {string} filename
 */
export const downloadCsvBlob = (blobData, filename = 'disease-pattern-report.csv') => {
  const blob = blobData instanceof Blob
    ? blobData
    : new Blob([blobData], { type: 'text/csv;charset=utf-8;' })

  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

/**
 * Parse error data when axios responseType is 'blob'
 * @param {any} error
 * @returns {Promise<any>}
 */
export const parseBlobError = async (error) => {
  if (error?.response?.data instanceof Blob) {
    try {
      const text = await error.response.data.text()
      return JSON.parse(text)
    } catch {
      return null
    }
  }
  return error?.response?.data || null
}

/**
 * Map error from export request to human-friendly Vietnamese message
 * @param {any} error
 * @returns {Promise<string>}
 */
export const getExportErrorMessage = async (error) => {
  const errorData = await parseBlobError(error)
  const status = error?.response?.status || errorData?.status
  const errorCode = String(errorData?.code || errorData?.errorCode || '')
  const errorMsg = String(errorData?.message || '')

  if (
    status === 422 ||
    status === 404 ||
    errorCode === 'REPORT_DATA_EMPTY' ||
    errorMsg.includes('No report data') ||
    errorMsg.includes('OperationalReportDataEmptyException') ||
    errorMsg.toLowerCase().includes('không có dữ liệu')
  ) {
    return 'Không thể xuất tệp vì kỳ báo cáo này chưa có dữ liệu chẩn đoán nào.'
  }

  if (status === 403) {
    return 'Bạn không có quyền xuất báo cáo này.'
  }

  if (status === 400) {
    if (errorCode === 'DATE_RANGE_TOO_LONG' || errorMsg.includes('366')) {
      return 'Khoảng thời gian báo cáo không được vượt quá 366 ngày.'
    }
    return 'Khoảng thời gian không hợp lệ. Vui lòng kiểm tra lại.'
  }

  return 'Không thể xuất báo cáo. Vui lòng thử lại.'
}
