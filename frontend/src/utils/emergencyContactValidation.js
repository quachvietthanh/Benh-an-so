/**
 * Module tiện ích & xác thực Người liên hệ khẩn cấp (NCL-02-CN-007)
 * Tuân thủ quy tắc Cohesive Triplet (bộ ba toàn vẹn) và định dạng số điện thoại Việt Nam.
 */

// Regex số điện thoại di động Việt Nam (hỗ trợ cả dạng che mặt nạ ẩn danh 09******78 và tiền tố +84)
export const VIETNAMESE_PHONE_REGEX = /^(?:(0|\+84)(3|5|7|8|9)[0-9]{8}|[0-9]{2}\*{6}[0-9]{2})?$/

// Danh mục mối quan hệ gợi ý chuẩn y tế
export const EMERGENCY_RELATIONSHIPS = [
  'Bố',
  'Mẹ',
  'Vợ',
  'Chồng',
  'Con',
  'Anh/Chị/Em',
  'Ông/Bà',
  'Người giám hộ',
  'Khác',
]

/**
 * Kiểm tra tính hợp lệ của số điện thoại
 * @param {string} phone 
 * @returns {boolean}
 */
export function isValidEmergencyPhone(phone) {
  if (!phone) return true
  const trimmed = String(phone).trim()
  if (trimmed === '') return true
  return VIETNAMESE_PHONE_REGEX.test(trimmed)
}

/**
 * Kiểm tra quy tắc bộ ba toàn vẹn (Cohesive Triplet Rule)
 * Quy định:
 * - Có thể bỏ trống cả 3 trường (hợp lệ khi bệnh nhân không đăng ký hoặc muốn xóa trắng)
 * - Nếu nhập bất kỳ 1 trường nào trong 3 trường thì bắt buộc phải nhập đủ cả 3 trường và hợp lệ
 * 
 * @param {Object} triplet - { emergencyContact, emergencyRelationship, emergencyPhone }
 * @returns {{ valid: boolean, isCleared: boolean, errors: Object }}
 */
export function validateEmergencyContactTriplet(triplet = {}) {
  const contact = triplet.emergencyContact ? String(triplet.emergencyContact).trim() : ''
  const relationship = triplet.emergencyRelationship ? String(triplet.emergencyRelationship).trim() : ''
  const phone = triplet.emergencyPhone ? String(triplet.emergencyPhone).trim() : ''

  const hasAny = Boolean(contact || relationship || phone)
  const errors = {}

  // Trường hợp xóa sạch cả 3 trường
  if (!hasAny) {
    return {
      valid: true,
      isCleared: true,
      errors: {},
    }
  }

  // Nếu có ít nhất một trường -> bắt buộc cả 3 trường
  if (!contact) {
    errors.emergencyContact = 'Vui lòng nhập họ tên người liên hệ khẩn cấp'
  } else if (contact.length > 100) {
    errors.emergencyContact = 'Họ tên người liên hệ không được vượt quá 100 ký tự'
  }

  if (!relationship) {
    errors.emergencyRelationship = 'Vui lòng chọn hoặc nhập mối quan hệ nhân thân'
  } else if (relationship.length > 50) {
    errors.emergencyRelationship = 'Mối quan hệ không được vượt quá 50 ký tự'
  }

  if (!phone) {
    errors.emergencyPhone = 'Vui lòng nhập số điện thoại người liên hệ khẩn cấp'
  } else if (!isValidEmergencyPhone(phone)) {
    errors.emergencyPhone = 'Số điện thoại không đúng định dạng di động Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08, 09)'
  }

  const valid = Object.keys(errors).length === 0
  return {
    valid,
    isCleared: false,
    errors,
  }
}

/**
 * Chuẩn hóa chuỗi hiển thị tóm tắt người liên hệ khẩn cấp
 * Ví dụ: "Nguyễn Văn A (Bố) • 0912345678"
 * @param {Object} patient 
 * @returns {string|null}
 */
export function formatEmergencyContactDisplay(patient) {
  if (!patient) return null
  const contact = patient.emergencyContact ? String(patient.emergencyContact).trim() : ''
  const relationship = patient.emergencyRelationship ? String(patient.emergencyRelationship).trim() : ''
  const phone = patient.emergencyPhone ? String(patient.emergencyPhone).trim() : ''

  if (!contact && !phone) return null

  const parts = []
  if (contact) {
    parts.push(relationship ? `${contact} (${relationship})` : contact)
  }
  if (phone) {
    parts.push(phone)
  }

  return parts.join(' • ')
}

/**
 * Key prefix lưu lịch sử thay đổi người liên hệ khẩn cấp trên LocalStorage
 */
export const EMERGENCY_HISTORY_STORAGE_KEY = 'patient_emergency_contact_history'

/**
 * Lưu vết lịch sử cập nhật người liên hệ khẩn cấp (TC-03)
 * @param {string|number} patientId 
 * @param {Object} entry { actor, oldValue, newValue, timestamp }
 */
export function saveEmergencyContactHistory(patientId, entry) {
  if (!patientId || !entry) return
  try {
    const raw = localStorage.getItem(EMERGENCY_HISTORY_STORAGE_KEY)
    const allHistory = raw ? JSON.parse(raw) : {}
    const patientKey = String(patientId)
    const list = Array.isArray(allHistory[patientKey]) ? allHistory[patientKey] : []

    const newRecord = {
      id: 'emg_hist_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7),
      timestamp: entry.timestamp || new Date().toISOString(),
      actor: entry.actor || 'Lễ tân',
      oldValue: entry.oldValue || 'Chưa thiết lập',
      newValue: entry.newValue || 'Đã xóa người liên hệ',
      note: entry.note || '',
    }

    list.unshift(newRecord)
    allHistory[patientKey] = list
    localStorage.setItem(EMERGENCY_HISTORY_STORAGE_KEY, JSON.stringify(allHistory))
    return newRecord
  } catch (err) {
    console.warn('Lỗi khi lưu lịch sử liên hệ khẩn cấp:', err)
  }
}

/**
 * Lấy danh sách lịch sử thay đổi của bệnh nhân
 * @param {string|number} patientId 
 * @returns {Array}
 */
export function getEmergencyContactHistory(patientId) {
  if (!patientId) return []
  try {
    const raw = localStorage.getItem(EMERGENCY_HISTORY_STORAGE_KEY)
    if (!raw) return []
    const allHistory = JSON.parse(raw)
    const list = allHistory[String(patientId)]
    return Array.isArray(list) ? list : []
  } catch (err) {
    console.warn('Lỗi khi đọc lịch sử liên hệ khẩn cấp:', err)
    return []
  }
}
