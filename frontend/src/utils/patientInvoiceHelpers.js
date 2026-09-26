import dayjs from 'dayjs'

/**
 * Định dạng tiền tệ VND chuẩn Việt Nam (vd: 150.000 ₫)
 */
export const formatCurrency = (amount) => {
  if (amount === null || amount === undefined || isNaN(Number(amount))) {
    return '0 ₫'
  }
  const numeric = Number(amount)
  return `${numeric.toLocaleString('vi-VN')} ₫`
}

/**
 * Định dạng ngày khám (DD/MM/YYYY)
 */
export const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = dayjs(dateStr)
  return d.isValid() ? d.format('DD/MM/YYYY') : '—'
}

/**
 * Định dạng giờ (HH:mm)
 */
export const formatTime = (dateStr) => {
  if (!dateStr) return '—'
  const d = dayjs(dateStr)
  return d.isValid() ? d.format('HH:mm') : '—'
}

/**
 * Định dạng ngày giờ đầy đủ
 */
export const formatDateTime = (dateStr) => {
  if (!dateStr) return '—'
  const d = dayjs(dateStr)
  return d.isValid() ? d.format('DD/MM/YYYY HH:mm') : '—'
}

/**
 * Sắp xếp danh sách hóa đơn theo ngày khám mới nhất lên đầu (TC-01, Mục 2)
 */
export const sortInvoicesByDateDesc = (invoices = []) => {
  if (!Array.isArray(invoices)) return []
  return [...invoices].sort((a, b) => {
    const timeA = a.visitDate ? dayjs(a.visitDate).valueOf() : (a.createdAt ? dayjs(a.createdAt).valueOf() : 0)
    const timeB = b.visitDate ? dayjs(b.visitDate).valueOf() : (b.createdAt ? dayjs(b.createdAt).valueOf() : 0)
    return timeB - timeA
  })
}

/**
 * Ánh xạ danh sách hóa đơn và liên kết hóa đơn điều chỉnh với hóa đơn gốc
 * Quy tắc nghiệp vụ (Mục 1, 4):
 * - Hóa đơn gốc chưa sửa: Bản có hiệu lực
 * - Hóa đơn gốc ĐÃ BỊ ĐIỀU CHỈNH: Gắn nhãn "Đã điều chỉnh" kèm link đến bản điều chỉnh
 * - Hóa đơn điều chỉnh: Bản có hiệu lực mới nhất, gắn nhãn "Hóa đơn điều chỉnh"
 */
export const mapInvoicesWithAdjustments = (invoices = []) => {
  if (!Array.isArray(invoices)) return []

  // Tìm các hóa đơn điều chỉnh
  const adjustmentInvoices = invoices.filter(
    (inv) => String(inv.invoiceType || '').toUpperCase() === 'ADJUSTMENT'
  )

  // Tạo map theo visitId hoặc originalInvoiceId
  const adjustmentByVisit = new Map()
  adjustmentInvoices.forEach((adj) => {
    if (adj.visitId) {
      adjustmentByVisit.set(adj.visitId, adj)
    }
  })

  return invoices.map((inv) => {
    const isAdjustment = String(inv.invoiceType || '').toUpperCase() === 'ADJUSTMENT'
    const relatedAdjustment = !isAdjustment && inv.visitId ? adjustmentByVisit.get(inv.visitId) : null
    const hasBeenAdjusted = Boolean(relatedAdjustment)

    return {
      ...inv,
      isAdjustment,
      isOriginal: !isAdjustment,
      hasBeenAdjusted,
      // Bản điều chỉnh mới nhất có hiệu lực
      effectiveAdjustment: relatedAdjustment
        ? {
            invoiceId: relatedAdjustment.invoiceId,
            invoiceCode: relatedAdjustment.invoiceCode,
            totalAmount: relatedAdjustment.totalAmount,
            createdAt: relatedAdjustment.createdAt,
          }
        : null,
      // Cờ báo bản này có hiệu lực pháp lý cao nhất hiện tại hay không
      isLatestEffective: isAdjustment || !hasBeenAdjusted,
      statusBadge: isAdjustment
        ? {
            label: 'Hóa đơn điều chỉnh',
            color: 'blue',
            isEffective: true,
            hint: 'Bản điều chỉnh mới nhất có hiệu lực',
          }
        : hasBeenAdjusted
        ? {
            label: 'Đã có điều chỉnh',
            color: 'warning',
            isEffective: false,
            hint: 'Hóa đơn gốc đã được điều chỉnh. Vui lòng dùng bản mới nhất.',
          }
        : {
            label: 'Hóa đơn gốc',
            color: 'green',
            isEffective: true,
            hint: 'Hóa đơn gốc hợp lệ có hiệu lực',
          },
    }
  })
}

/**
 * Lọc và tìm kiếm hóa đơn theo từ khóa và phân loại
 */
export const filterInvoices = (invoices = [], { searchKeyword = '', filterType = 'ALL' } = {}) => {
  if (!Array.isArray(invoices)) return []

  let filtered = [...invoices]

  // Lọc theo loại hóa đơn
  if (filterType === 'ORIGINAL') {
    filtered = filtered.filter((inv) => inv.isOriginal)
  } else if (filterType === 'ADJUSTMENT') {
    filtered = filtered.filter((inv) => inv.isAdjustment)
  } else if (filterType === 'EFFECTIVE_ONLY') {
    filtered = filtered.filter((inv) => inv.isLatestEffective)
  }

  // Tìm kiếm theo từ khóa
  if (searchKeyword && searchKeyword.trim()) {
    const kw = searchKeyword.trim().toLowerCase()
    filtered = filtered.filter((inv) => {
      const code = String(inv.invoiceCode || '').toLowerCase()
      const visitCode = String(inv.visitCode || '').toLowerCase()
      const doctor = String(inv.doctorName || '').toLowerCase()
      const specialty = String(inv.specialtyName || '').toLowerCase()
      const date = formatDate(inv.visitDate || inv.createdAt).toLowerCase()
      return (
        code.includes(kw) ||
        visitCode.includes(kw) ||
        doctor.includes(kw) ||
        specialty.includes(kw) ||
        date.includes(kw)
      )
    })
  }

  return filtered
}

/**
 * Tính toán số liệu tổng hợp (KPI) cho bệnh nhân
 */
export const calculateInvoiceStats = (invoices = []) => {
  if (!Array.isArray(invoices) || invoices.length === 0) {
    return {
      totalCount: 0,
      totalAmount: 0,
      adjustedCount: 0,
      latestVisitDate: null,
    }
  }

  // Tổng tiền chỉ tính các hóa đơn có hiệu lực (hoặc tất cả hóa đơn không bị ghi đè)
  const totalAmount = invoices.reduce((sum, inv) => {
    // Nếu hóa đơn gốc đã bị thay bằng bản điều chỉnh, không cộng trùng tổng tiền
    if (inv.hasBeenAdjusted) return sum
    return sum + Number(inv.totalAmount || 0)
  }, 0)

  const adjustedCount = invoices.filter((inv) => inv.isAdjustment || inv.hasBeenAdjusted).length

  const sorted = sortInvoicesByDateDesc(invoices)
  const latestVisitDate = sorted[0]?.visitDate || sorted[0]?.createdAt || null

  return {
    totalCount: invoices.length,
    totalAmount,
    adjustedCount,
    latestVisitDate,
  }
}

/**
 * Tải file blob PDF về máy người dùng qua DOM API
 */
export const downloadPdfBlob = (blobData, filename = 'hoa-don.pdf') => {
  const blob = blobData instanceof Blob ? blobData : new Blob([blobData], { type: 'application/pdf' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.setAttribute('data-testid', 'invoice-download-anchor')
  document.body.appendChild(link)
  link.click()

  setTimeout(() => {
    if (link.parentNode) {
      link.parentNode.removeChild(link)
    }
    window.URL.revokeObjectURL(url)
  }, 1000)
}

/**
 * Xử lý lỗi truy cập bảo mật thân thiện (Ràng buộc bảo mật qua UI/UX):
 * Tuyệt đối không tiết lộ hóa đơn có tồn tại hay không đối với các lỗi 403/404
 */
export const getSecuritySafeErrorMessage = (error) => {
  const status = error?.response?.status || error?.status
  if (status === 403 || status === 404) {
    return 'Không tìm thấy hóa đơn hoặc bạn không có quyền truy cập.'
  }
  return error?.response?.data?.message || error?.message || 'Không thể tải dữ liệu hóa đơn. Vui lòng thử lại sau.'
}

/**
 * Chuyển đổi số tiền thành chữ tiếng Việt (cho hóa đơn in)
 */
export const numberToVietnameseWords = (amount) => {
  if (amount === 0 || amount === '0') return 'Không đồng'
  if (!amount || isNaN(Number(amount))) return '—'

  const num = Math.abs(Math.round(Number(amount)))
  const units = ['', 'nghìn', 'triệu', 'tỷ', 'nghìn tỷ', 'triệu tỷ']
  const digits = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín']

  const readThreeDigits = (n, hasHigherGroup) => {
    const h = Math.floor(n / 100)
    const t = Math.floor((n % 100) / 10)
    const u = n % 10
    let res = ''

    if (h > 0 || hasHigherGroup) {
      res += `${digits[h]} trăm `
    }

    if (t > 1) {
      res += `${digits[t]} mươi `
      if (u === 1) res += 'mốt '
      else if (u === 5) res += 'lăm '
      else if (u > 0) res += `${digits[u]} `
    } else if (t === 1) {
      res += 'mười '
      if (u === 5) res += 'lăm '
      else if (u > 0) res += `${digits[u]} `
    } else {
      if ((h > 0 || hasHigherGroup) && u > 0) {
        res += `lẻ ${digits[u]} `
      } else if (u > 0) {
        res += `${digits[u]} `
      }
    }

    return res.trim()
  }

  let str = num.toString()
  const groups = []
  while (str.length > 0) {
    groups.push(parseInt(str.slice(-3), 10))
    str = str.slice(0, -3)
  }

  let result = ''
  for (let i = groups.length - 1; i >= 0; i--) {
    const g = groups[i]
    if (g > 0) {
      const read = readThreeDigits(g, i < groups.length - 1)
      result += `${read} ${units[i]} `
    }
  }

  result = result.trim() + ' đồng'
  return result.charAt(0).toUpperCase() + result.slice(1)
}
