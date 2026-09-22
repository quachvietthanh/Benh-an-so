import dayjs from 'dayjs'

export const INVOICE_TYPE_OPTIONS = [
  { value: '', label: 'Tất cả loại hóa đơn' },
  { value: 'ORIGINAL', label: 'Hóa đơn gốc' },
  { value: 'ADJUSTMENT', label: 'Hóa đơn điều chỉnh' },
]

export const INVOICE_TYPE_META = {
  ORIGINAL: {
    label: 'Hóa đơn gốc',
    color: 'blue',
  },
  ADJUSTMENT: {
    label: 'Hóa đơn điều chỉnh',
    color: 'purple',
  },
}

export const INVOICE_LINE_TYPE_META = {
  EXAM_FEE: {
    label: 'Tiền khám',
    color: 'cyan',
  },
  MEDICINE_FEE: {
    label: 'Tiền thuốc',
    color: 'green',
  },
  SERVICE_FEE: {
    label: 'Tiền dịch vụ / Cận lâm sàng',
    color: 'geekblue',
  },
  ADJUSTMENT: {
    label: 'Khoản điều chỉnh',
    color: 'volcano',
  },
}

export const SEEDED_VISIT_PATIENT_MAP = {
  'd0000000-0000-0000-0000-000000000001': 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001',
  'd0000000-0000-0000-0000-000000000002': 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001',
  'd0000000-0000-0000-0000-000000000006': 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb005',
}

export const SEEDED_VISIT_CODE_MAP = {
  'd0000000-0000-0000-0000-000000000001': 'VIS000001',
  'd0000000-0000-0000-0000-000000000002': 'VIS000002',
  'd0000000-0000-0000-0000-000000000006': 'VIS000006',
}

export const SEEDED_INVOICE_CODE_MAP = {
  '23100000-0000-0000-0000-000000000001': 'HD000001',
  '23100000-0000-0000-0000-000000000002': 'HDDC000001',
}

export const formatVisitCode = (val) => {
  if (!val) return '—'
  const lower = String(val).toLowerCase()
  if (SEEDED_VISIT_CODE_MAP[lower]) return SEEDED_VISIT_CODE_MAP[lower]
  const str = String(val).toUpperCase()
  if (str.startsWith('VIS')) return str
  return `VIS-${str.slice(0, 8)}`
}

export const formatInvoiceCode = (val, invoiceList = []) => {
  if (!val) return '—'
  const lower = String(val).toLowerCase()
  if (SEEDED_INVOICE_CODE_MAP[lower]) return SEEDED_INVOICE_CODE_MAP[lower]
  const matched = (invoiceList || []).find((inv) => String(inv.id || '').toLowerCase() === lower)
  if (matched?.invoiceCode) return matched.invoiceCode
  const str = String(val).toUpperCase()
  if (str.startsWith('HD')) return str
  return str.length > 12 ? `${str.slice(0, 8)}...` : str
}

export const resolvePatientInfo = ({
  visitId,
  encounter,
  patientList = [],
  payableList = [],
  queueList = [],
}) => {
  if (encounter?.patient?.fullName) {
    return {
      fullName: encounter.patient.fullName,
      patientCode: encounter.patient.patientCode,
      phone: encounter.patient.phone || '',
      dateOfBirth: encounter.patient.dateOfBirth,
      gender: encounter.patient.gender,
      visitCode: encounter.visit?.visitCode || formatVisitCode(visitId),
      doctorName: encounter.doctor?.fullName || '',
      reason: encounter.visit?.reason || '',
    }
  }

  const visitIdStr = String(visitId || '').toLowerCase()
  const matchedPayable = payableList.find((p) => String(p.visitId || '').toLowerCase() === visitIdStr)
  if (matchedPayable) {
    return {
      fullName: matchedPayable.patientName,
      patientCode: matchedPayable.patientCode,
      phone: '',
      dateOfBirth: null,
      gender: '',
      visitCode: matchedPayable.visitCode || formatVisitCode(visitId),
      doctorName: '',
      reason: matchedPayable.reason || '',
    }
  }

  const matchedQueue = queueList.find((q) => String(q.visitId || '').toLowerCase() === visitIdStr)
  if (matchedQueue) {
    return {
      fullName: matchedQueue.patientName,
      patientCode: matchedQueue.patientCode || '',
      phone: '',
      dateOfBirth: null,
      gender: '',
      visitCode: matchedQueue.visitCode || formatVisitCode(visitId),
      doctorName: matchedQueue.doctorName || '',
      reason: '',
    }
  }

  const seededPatientId = SEEDED_VISIT_PATIENT_MAP[visitIdStr]
  if (seededPatientId) {
    const matchedPt = patientList.find(
      (p) => String(p.id || '').toLowerCase() === seededPatientId.toLowerCase(),
    )
    if (matchedPt) {
      return {
        fullName: matchedPt.fullName || matchedPt.name || '',
        patientCode: matchedPt.patientCode || matchedPt.code || '',
        phone: matchedPt.phone || matchedPt.phoneNumber || '',
        dateOfBirth: matchedPt.dateOfBirth,
        gender: matchedPt.gender,
        visitCode: formatVisitCode(visitId),
        doctorName: '',
        reason: '',
      }
    }
  }

  return null
}

/**
 * Định dạng số tiền sang định dạng VNĐ chuẩn
 * @param {number|string} amount
 * @returns {string}
 */
export const formatCurrency = (amount) => {
  const num = Number(amount || 0)
  return `${num.toLocaleString('vi-VN')} ₫`
}

/**
 * Định dạng ngày giờ chuẩn hiển thị
 * @param {string|Date} timestamp
 * @returns {string}
 */
export const formatDateTime = (timestamp) => {
  if (!timestamp) return '—'
  const date = dayjs(timestamp)
  if (!date.isValid()) return '—'
  return date.format('HH:mm:ss DD/MM/YYYY')
}

/**
 * Định dạng ngày chuẩn
 * @param {string|Date} timestamp
 * @returns {string}
 */
export const formatDate = (timestamp) => {
  if (!timestamp) return '—'
  const date = dayjs(timestamp)
  if (!date.isValid()) return '—'
  return date.format('DD/MM/YYYY')
}

/**
 * Kiểm tra xem hóa đơn có phải là bản in lại hay không
 * @param {number} reprintCount
 * @returns {boolean}
 */
export const isReprint = (reprintCount) => {
  return typeof reprintCount === 'number' && reprintCount > 0
}

/**
 * Tạo thông tin hiển thị huy hiệu và watermark cho bản in lại
 * @param {number} reprintCount
 * @param {string|Date} lastReprintedAt
 * @returns {{isReprint: boolean, badgeText: string, watermarkText: string, timeText: string}}
 */
export const getReprintMetadata = (reprintCount, lastReprintedAt) => {
  const count = Number(reprintCount || 0)
  if (count <= 0) {
    return {
      isReprint: false,
      badgeText: '',
      watermarkText: '',
      timeText: '',
    }
  }

  return {
    isReprint: true,
    badgeText: `BẢN IN LẠI (Lần ${count})`,
    watermarkText: `BẢN IN LẠI - LẦN ${count}`,
    timeText: lastReprintedAt ? `In lại lúc: ${formatDateTime(lastReprintedAt)}` : '',
  }
}

/**
 * Chuyển số tiền thành chữ Tiếng Việt chuẩn y tế / tài chính
 * @param {number|string} number
 * @returns {string}
 */
export const numberToVietnameseWords = (number) => {
  const num = Math.round(Math.abs(Number(number || 0)))
  if (num === 0) return 'Không đồng'

  const units = ['', 'nghìn', 'triệu', 'tỷ', 'nghìn tỷ', 'triệu tỷ']
  const digits = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín']

  const readThreeDigits = (n, showZeroHundred = false) => {
    const hundred = Math.floor(n / 100)
    const ten = Math.floor((n % 100) / 10)
    const one = n % 10
    const parts = []

    if (hundred > 0 || showZeroHundred) {
      parts.push(`${digits[hundred]} trăm`)
    }

    if (ten > 1) {
      parts.push(`${digits[ten]} mươi`)
      if (one === 1) parts.push('mốt')
      else if (one === 5) parts.push('lăm')
      else if (one > 0) parts.push(digits[one])
    } else if (ten === 1) {
      parts.push('mười')
      if (one === 5) parts.push('lăm')
      else if (one > 0) parts.push(digits[one])
    } else if (ten === 0 && (hundred > 0 || showZeroHundred)) {
      if (one > 0) {
        parts.push(`lẻ ${digits[one]}`)
      }
    } else if (one > 0) {
      parts.push(digits[one])
    }

    return parts.join(' ')
  }

  const chunks = []
  let temp = num
  while (temp > 0) {
    chunks.push(temp % 1000)
    temp = Math.floor(temp / 1000)
  }

  const resultWords = []
  for (let i = chunks.length - 1; i >= 0; i--) {
    const chunk = chunks[i]
    if (chunk > 0) {
      const isFirst = i === chunks.length - 1
      const chunkWord = readThreeDigits(chunk, !isFirst)
      const unit = units[i]
      resultWords.push(unit ? `${chunkWord} ${unit}` : chunkWord)
    }
  }

  const sentence = resultWords.join(' ').trim()
  return (sentence.charAt(0).toUpperCase() + sentence.slice(1) + ' đồng').replace(/\s+/g, ' ')
}

/**
 * Xây dựng tham số tìm kiếm an toàn cho API /invoices
 * @param {Object} rawParams
 * @returns {Object}
 */
export const buildInvoiceSearchParams = (rawParams = {}) => {
  const {
    patientName,
    invoiceCode,
    invoiceType,
    createdFrom,
    createdTo,
    visitId,
    page = 0,
    size = 20,
  } = rawParams

  const cleanParams = {
    page: Math.max(0, Number(page) || 0),
    size: Math.min(100, Math.max(1, Number(size) || 20)),
  }

  if (patientName && String(patientName).trim()) {
    cleanParams.patientName = String(patientName).trim()
  }

  if (invoiceCode && String(invoiceCode).trim()) {
    cleanParams.invoiceCode = String(invoiceCode).trim()
  }

  if (invoiceType && (invoiceType === 'ORIGINAL' || invoiceType === 'ADJUSTMENT')) {
    cleanParams.invoiceType = invoiceType
  }

  if (visitId && String(visitId).trim()) {
    cleanParams.visitId = String(visitId).trim()
  }

  if (createdFrom) {
    cleanParams.createdFrom = dayjs(createdFrom).toISOString()
  }

  if (createdTo) {
    cleanParams.createdTo = dayjs(createdTo).toISOString()
  }

  return cleanParams
}
