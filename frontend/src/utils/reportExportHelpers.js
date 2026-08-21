import dayjs from 'dayjs'

export const REPORT_TYPES = [
  { value: 'VISIT_REPORT', label: 'Báo cáo lượt khám' },
  { value: 'REVENUE_REPORT', label: 'Báo cáo doanh thu' },
  { value: 'OPERATIONAL_REPORT', label: 'Báo cáo tổng hợp vận hành' },
]

export const validateExportParams = (from, to) => {
  if (!from || !to) {
    return {
      isValid: false,
      message: 'Vui lòng chọn khoảng thời gian.',
    }
  }

  const fromDay = dayjs(from)
  const toDay = dayjs(to)

  if (!fromDay.isValid() || !toDay.isValid()) {
    return {
      isValid: false,
      message: 'Khoảng thời gian không hợp lệ.',
    }
  }

  if (fromDay.isAfter(toDay, 'day')) {
    return {
      isValid: false,
      message: 'Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.',
    }
  }

  const daysDiff = toDay.diff(fromDay, 'day') + 1
  if (daysDiff > 366) {
    return {
      isValid: false,
      message: 'Khoảng thời gian xuất báo cáo không được vượt quá 366 ngày.',
    }
  }

  return {
    isValid: true,
    from: fromDay.format('YYYY-MM-DD'),
    to: toDay.format('YYYY-MM-DD'),
    message: null,
  }
}

export const getExportFilename = (contentDisposition, reportType, from, to) => {
  if (contentDisposition) {
    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
    if (utf8Match && utf8Match[1]) {
      try {
        return decodeURIComponent(utf8Match[1].trim().replace(/^["']|["']$/g, ''))
      } catch {
        // ignore decode failure and try standard match
      }
    }

    const standardMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
    if (standardMatch && standardMatch[1]) {
      return standardMatch[1].trim().replace(/^["']|["']$/g, '')
    }
  }

  const fromStr = String(from || '')
  const toStr = String(to || '')

  switch (reportType) {
    case 'VISIT_REPORT':
      return `visit-report-${fromStr}-to-${toStr}.csv`
    case 'REVENUE_REPORT':
      return `revenue-report-${fromStr}-to-${toStr}.csv`
    case 'OPERATIONAL_REPORT':
      return `operational-report-${fromStr}-to-${toStr}.csv`
    default:
      return `report-${fromStr}-to-${toStr}.csv`
  }
}

export const downloadCsvBlob = (data, fileName) => {
  const blob = new Blob([data], {
    type: 'text/csv;charset=utf-8;',
  })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

export const getExportErrorMessage = async (error) => {
  let errorData = null

  if (error?.response?.data instanceof Blob) {
    try {
      const text = await error.response.data.text()
      errorData = JSON.parse(text)
    } catch {
      // ignore
    }
  } else if (error?.response?.data) {
    errorData = error.response.data
  }

  const status = error?.response?.status
  const errorCode = String(errorData?.code || errorData?.errorCode || '')
  const errorMsg = String(errorData?.message || '')

  if (status === 403) {
    return 'Bạn không có quyền xuất báo cáo.'
  }

  if (
    status === 404 ||
    errorCode === 'REPORT_DATA_EMPTY' ||
    errorMsg.includes('No report data') ||
    errorMsg.toLowerCase().includes('không có dữ liệu')
  ) {
    return 'Không có dữ liệu trong khoảng thời gian đã chọn.'
  }

  if (status === 400) {
    return 'Khoảng thời gian không hợp lệ.'
  }

  return 'Không thể xuất báo cáo. Vui lòng thử lại.'
}
