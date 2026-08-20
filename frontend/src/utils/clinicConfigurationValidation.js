import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'

dayjs.extend(customParseFormat)

export function validateClinicName(name) {
  const trimmed = (name || '').trim()
  if (!trimmed) {
    return { valid: false, error: 'Tên phòng khám là bắt buộc.' }
  }
  if (trimmed.length > 150) {
    return { valid: false, error: 'Tên phòng khám không được vượt quá 150 ký tự.' }
  }
  return { valid: true, value: trimmed }
}

export function validateAddress(address) {
  if (!address) {
    return { valid: true, value: null }
  }
  const trimmed = address.trim()
  if (trimmed.length > 500) {
    return { valid: false, error: 'Địa chỉ không được vượt quá 500 ký tự.' }
  }
  return { valid: true, value: trimmed || null }
}

export function validatePhone(phone) {
  if (!phone) {
    return { valid: true, value: null }
  }
  const trimmed = phone.trim()
  if (trimmed.length > 30) {
    return { valid: false, error: 'Số điện thoại không được vượt quá 30 ký tự.' }
  }
  return { valid: true, value: trimmed || null }
}

export function parseTimeString(timeVal) {
  if (!timeVal) return null
  if (dayjs.isDayjs(timeVal)) return timeVal
  return dayjs(timeVal, ['HH:mm:ss', 'HH:mm'])
}

export function getTimeInSeconds(timeVal) {
  const parsed = parseTimeString(timeVal)
  if (!parsed || !parsed.isValid()) return null
  return parsed.hour() * 3600 + parsed.minute() * 60 + parsed.second()
}

export function validateWorkingHours(openingTime, closingTime) {
  if (!openingTime) {
    return { valid: false, error: 'Giờ mở cửa là bắt buộc.' }
  }
  if (!closingTime) {
    return { valid: false, error: 'Giờ đóng cửa là bắt buộc.' }
  }

  const openSec = getTimeInSeconds(openingTime)
  const closeSec = getTimeInSeconds(closingTime)

  if (openSec === null) {
    return { valid: false, error: 'Giờ mở cửa không hợp lệ.' }
  }
  if (closeSec === null) {
    return { valid: false, error: 'Giờ đóng cửa không hợp lệ.' }
  }

  if (closeSec <= openSec) {
    return { valid: false, error: 'Giờ đóng cửa phải sau giờ mở cửa.' }
  }

  return { valid: true }
}

export function formatClinicConfigPayload(values) {
  const nameRes = validateClinicName(values?.clinicName)
  if (!nameRes.valid) return nameRes

  const addressRes = validateAddress(values?.address)
  if (!addressRes.valid) return addressRes

  const phoneRes = validatePhone(values?.phone)
  if (!phoneRes.valid) return phoneRes

  const hoursRes = validateWorkingHours(values?.openingTime, values?.closingTime)
  if (!hoursRes.valid) return hoursRes

  const openParsed = parseTimeString(values.openingTime)
  const closeParsed = parseTimeString(values.closingTime)

  return {
    valid: true,
    payload: {
      clinicName: nameRes.value,
      address: addressRes.value,
      phone: phoneRes.value,
      openingTime: openParsed.format('HH:mm:ss'),
      closingTime: closeParsed.format('HH:mm:ss'),
    },
  }
}
