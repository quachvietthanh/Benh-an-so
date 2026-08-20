/* global TextDecoder */

import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat.js'
import { normalizeApiError } from './apiError.js'

dayjs.extend(customParseFormat)

const win1252Map = {
  0x80: 0x20AC, 0x82: 0x201A, 0x83: 0x0192, 0x84: 0x201E, 0x85: 0x2026,
  0x86: 0x2020, 0x87: 0x2021, 0x88: 0x02C6, 0x89: 0x2030, 0x8A: 0x0160,
  0x8B: 0x2039, 0x8C: 0x0152, 0x8E: 0x017D, 0x91: 0x2018, 0x92: 0x2019,
  0x93: 0x201C, 0x94: 0x201D, 0x95: 0x2022, 0x96: 0x2013, 0x97: 0x2014,
  0x98: 0x02DC, 0x99: 0x2122, 0x9A: 0x0161, 0x9B: 0x203A, 0x9C: 0x0153,
  0x9E: 0x017E, 0x9F: 0x0178
}
const revMap = {}
for (let b = 0; b < 256; b++) {
  const unicode = win1252Map[b] || b
  revMap[unicode] = b
}

/**
 * Repairs UTF-8 strings misdecoded as Windows-1252 / ISO-8859-1 (mojibake)
 */
export function fixMojibake(str) {
  if (!str || typeof str !== 'string') return str
  if (!/[ÃÄá»Æ¿]/.test(str)) return str

  try {
    const bytes = []
    for (let i = 0; i < str.length; i++) {
      const code = str.charCodeAt(i)
      if (revMap[code] !== undefined) {
        bytes.push(revMap[code])
      } else if (code < 256) {
        bytes.push(code)
      } else {
        return str
      }
    }
    return new TextDecoder('utf-8', { fatal: true }).decode(new Uint8Array(bytes))
  } catch {
    return str
  }
}

/**
 * Validate service code (Mã dịch vụ)
 * Rules: Required, max 50 chars, alphanumeric + underscores + dashes
 */
export function validateServiceCode(code) {
  const trimmed = (code || '').toString().trim()
  if (!trimmed) {
    return { valid: false, error: 'Mã dịch vụ là bắt buộc.' }
  }
  if (trimmed.length > 50) {
    return { valid: false, error: 'Mã dịch vụ không được vượt quá 50 ký tự.' }
  }
  if (!/^[A-Za-z0-9_.-]+$/.test(trimmed)) {
    return {
      valid: false,
      error: 'Mã dịch vụ chỉ được chứa chữ cái, chữ số, dấu gạch ngang (-) hoặc gạch dưới (_).',
    }
  }
  return { valid: true, value: trimmed.toUpperCase() }
}

/**
 * Validate service name (Tên dịch vụ)
 * Rules: Required, max 255 chars
 */
export function validateServiceName(name) {
  const trimmed = (name || '').toString().trim().replace(/\s+/g, ' ')
  if (!trimmed) {
    return { valid: false, error: 'Tên dịch vụ là bắt buộc.' }
  }
  if (trimmed.length > 255) {
    return { valid: false, error: 'Tên dịch vụ không được vượt quá 255 ký tự.' }
  }
  return { valid: true, value: trimmed }
}

/**
 * Validate service price (Đơn giá dịch vụ)
 * Rules: Required, numeric, >= 0
 */
export function validateServicePrice(price) {
  if (price === null || price === undefined || price === '') {
    return { valid: false, error: 'Đơn giá dịch vụ là bắt buộc.' }
  }
  const numeric = Number(price)
  if (Number.isNaN(numeric) || numeric < 0) {
    return { valid: false, error: 'Đơn giá dịch vụ phải là số lớn hơn hoặc bằng 0.' }
  }
  return { valid: true, value: numeric }
}

/**
 * Validate effective date (Ngày hiệu lực)
 * Rules: Required, valid date format YYYY-MM-DD
 */
export function validateEffectiveDate(effectiveFrom) {
  if (!effectiveFrom) {
    return { valid: false, error: 'Ngày bắt đầu hiệu lực là bắt buộc.' }
  }
  const parsed = dayjs(effectiveFrom)
  if (!parsed.isValid()) {
    return { valid: false, error: 'Ngày hiệu lực không đúng định dạng.' }
  }
  return { valid: true, value: parsed.format('YYYY-MM-DD') }
}

/**
 * Format currency to Vietnamese standard (xxx.xxx ₫)
 */
export function formatServiceCurrency(amount) {
  if (amount === null || amount === undefined || amount === '') return '0 ₫'
  const numeric = Number(amount)
  if (Number.isNaN(numeric)) return '0 ₫'
  return `${numeric.toLocaleString('vi-VN')} ₫`
}

/**
 * Format date for table/card display (DD/MM/YYYY)
 */
export function formatDateDisplay(date) {
  if (!date) return '—'
  const parsed = dayjs(date)
  return parsed.isValid() ? parsed.format('DD/MM/YYYY') : '—'
}

/**
 * Format date time for display (DD/MM/YYYY HH:mm)
 */
export function formatDateTimeDisplay(date) {
  if (!date) return '—'
  const parsed = dayjs(date)
  return parsed.isValid() ? parsed.format('DD/MM/YYYY HH:mm') : '—'
}

/**
 * Categorize price versions into CURRENT, UPCOMING, or EXPIRED
 * based on effectiveFrom and a reference date (default: today).
 */
export function categorizePriceHistory(prices = [], refDate = null) {
  if (!Array.isArray(prices) || prices.length === 0) {
    return []
  }

  const todayStr = refDate
    ? dayjs(refDate).format('YYYY-MM-DD')
    : dayjs().format('YYYY-MM-DD')

  // Sort descending by effectiveFrom
  const sorted = [...prices].sort((a, b) => {
    const dateA = a.effectiveFrom || ''
    const dateB = b.effectiveFrom || ''
    return dateB.localeCompare(dateA)
  })

  // Find the first price that has effectiveFrom <= todayStr (this is the current active price)
  const currentIdx = sorted.findIndex((p) => (p.effectiveFrom || '') <= todayStr)

  return sorted.map((p, index) => {
    const isFuture = (p.effectiveFrom || '') > todayStr
    let status = 'EXPIRED'
    let statusLabel = 'Hết hiệu lực'
    let statusColor = 'default'

    if (isFuture) {
      status = 'UPCOMING'
      statusLabel = 'Sắp áp dụng'
      statusColor = 'processing'
    } else if (index === currentIdx) {
      status = 'CURRENT'
      statusLabel = 'Đang áp dụng'
      statusColor = 'success'
    }

    return {
      ...p,
      priceStatus: status,
      statusLabel,
      statusColor,
      formattedPrice: formatServiceCurrency(p.price),
      formattedEffectiveFrom: formatDateDisplay(p.effectiveFrom),
      formattedCreatedAt: formatDateTimeDisplay(p.createdAt),
    }
  })
}

/**
 * Prepares and validates payload for Create Service (POST /system/services)
 */
export function prepareCreateServicePayload(values = {}) {
  const codeRes = validateServiceCode(values.serviceCode)
  if (!codeRes.valid) throw new Error(codeRes.error)

  const nameRes = validateServiceName(values.name)
  if (!nameRes.valid) throw new Error(nameRes.error)

  const priceRes = validateServicePrice(values.price)
  if (!priceRes.valid) throw new Error(priceRes.error)

  const dateRes = validateEffectiveDate(values.effectiveFrom)
  if (!dateRes.valid) throw new Error(dateRes.error)

  return {
    serviceCode: codeRes.value,
    name: nameRes.value,
    price: priceRes.value,
    effectiveFrom: dateRes.value,
  }
}

/**
 * Prepares and validates payload for Update Service (PUT /system/services/{id})
 */
export function prepareUpdateServicePayload(values = {}) {
  const nameRes = validateServiceName(values.name)
  if (!nameRes.valid) throw new Error(nameRes.error)

  const priceRes = validateServicePrice(values.price)
  if (!priceRes.valid) throw new Error(priceRes.error)

  const dateRes = validateEffectiveDate(values.effectiveFrom)
  if (!dateRes.valid) throw new Error(dateRes.error)

  return {
    name: nameRes.value,
    active: Boolean(values.active),
    price: priceRes.value,
    effectiveFrom: dateRes.value,
  }
}

/**
 * Translates backend errors using stable response codes.
 */
export function translateServiceErrorMessage(error) {
  if (!error) return 'Đã xảy ra lỗi không xác định.'
  
  const apiError = normalizeApiError(error)

  if (apiError.code === 'ACCESS_DENIED' || apiError.status === 403) {
    return 'Bạn không có quyền thực hiện thao tác này.'
  }
  if (apiError.code === 'AUTHENTICATION_FAILED' || apiError.status === 401) {
    return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
  }
  if (apiError.code === 'RESOURCE_NOT_FOUND' || apiError.status === 404) {
    return 'Không tìm thấy thông tin dịch vụ trong hệ thống.'
  }

  return apiError.firstFieldError || apiError.message
}
