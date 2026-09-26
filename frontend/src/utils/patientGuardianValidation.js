/**
 * Quy tắc nghiệp vụ và kiểm tra hợp lệ cho chức năng Hồ sơ bệnh nhân trẻ em gắn người giám hộ (NCL-02-CN-008).
 * Quy tắc: QTN-44, QTN-24
 * Tiêu chí chấp thuận: TC-01, TC-02, TC-03, TC-04
 */
import dayjs from 'dayjs'

export const GUARDIAN_RELATIONSHIP_PRESETS = [
  'Bố',
  'Mẹ',
  'Ông',
  'Bà',
  'Anh ruột',
  'Chị ruột',
  'Người giám hộ hợp pháp',
  'Khác',
]

/**
 * Kiểm tra xem bệnh nhân có phải là người chưa thành niên (< 18 tuổi) hay không (QTN-44)
 * @param {string|Date|dayjs.Dayjs} dateOfBirth 
 * @param {string|Date|dayjs.Dayjs} [asOfDate]
 * @returns {boolean}
 */
export function isMinorPatient(dateOfBirth, asOfDate = dayjs()) {
  if (!dateOfBirth) return false
  const dob = dayjs(dateOfBirth)
  if (!dob.isValid()) return false
  const current = dayjs(asOfDate)
  // Tuổi < 18 (tính chính xác theo ngày tháng năm)
  return current.diff(dob, 'year', true) < 18
}

/**
 * Kiểm tra số điện thoại người giám hộ theo chuẩn di động Việt Nam (10 chữ số)
 * @param {string} phone 
 * @returns {boolean}
 */
export function isValidGuardianPhone(phone) {
  if (!phone) return false
  const clean = String(phone).trim().replace(/[\s.-]/g, '')
  return /^0[35789]\d{8}$/.test(clean)
}

/**
 * Kiểm tra hợp lệ các trường thông tin người giám hộ (TC-01, TC-02)
 * @param {Object} params
 * @param {string|dayjs.Dayjs} [params.dateOfBirth]
 * @param {string} [params.guardianName]
 * @param {string} [params.guardianRelationship]
 * @param {string} [params.guardianPhone]
 * @param {string} [params.guardianIdentityNumber]
 * @param {dayjs.Dayjs} [asOfDate]
 * @returns {{ valid: boolean, isMinor: boolean, errors: Object }}
 */
export function validateGuardianFields(params = {}, asOfDate = dayjs()) {
  const isMinor = isMinorPatient(params.dateOfBirth, asOfDate)
  const errors = {}

  if (!isMinor) {
    return {
      valid: true,
      isMinor: false,
      errors: {},
    }
  }

  // Bệnh nhân dưới 18 tuổi -> bắt buộc phải có người giám hộ (QTN-44 & TC-02)
  const name = params.guardianName ? String(params.guardianName).trim() : ''
  const relationship = params.guardianRelationship ? String(params.guardianRelationship).trim() : ''
  const phone = params.guardianPhone ? String(params.guardianPhone).trim() : ''
  const idNumber = params.guardianIdentityNumber ? String(params.guardianIdentityNumber).trim() : ''

  if (!name) {
    errors.guardianName = 'Bệnh nhân dưới 18 tuổi bắt buộc phải khai báo họ tên người giám hộ.'
  } else if (name.length > 100) {
    errors.guardianName = 'Họ tên người giám hộ không được vượt quá 100 ký tự.'
  }

  if (!relationship) {
    errors.guardianRelationship = 'Vui lòng chọn hoặc nhập mối quan hệ của người giám hộ với bệnh nhân.'
  } else if (relationship.length > 50) {
    errors.guardianRelationship = 'Mối quan hệ không được vượt quá 50 ký tự.'
  }

  if (!phone) {
    errors.guardianPhone = 'Vui lòng nhập số điện thoại người giám hộ.'
  } else if (!isValidGuardianPhone(phone)) {
    errors.guardianPhone = 'Số điện thoại người giám hộ không đúng định dạng di động Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08, 09).'
  }

  if (idNumber && idNumber.length > 20) {
    errors.guardianIdentityNumber = 'Số CCCD/CMND người giám hộ không được vượt quá 20 ký tự.'
  }

  const valid = Object.keys(errors).length === 0
  return {
    valid,
    isMinor: true,
    errors,
  }
}

/**
 * Kiểm tra xem bệnh nhân có cần chuyển tiếp thành niên hay không (TC-04)
 * Điều kiện: Bệnh nhân đã đủ 18 tuổi nhưng hồ sơ vẫn còn gắn thông tin người giám hộ
 * @param {Object} patient
 * @param {dayjs.Dayjs} [asOfDate]
 * @returns {boolean}
 */
export function requiresAdultTransition(patient, asOfDate = dayjs()) {
  if (!patient) return false
  if (patient.requiresAdultTransitionPrompt != null) {
    return Boolean(patient.requiresAdultTransitionPrompt)
  }
  if (patient.isMerged) return false

  const isMinor = isMinorPatient(patient.dateOfBirth, asOfDate)
  const hasGuardian = Boolean(patient.guardianName && String(patient.guardianName).trim())

  // Đã đủ 18 tuổi nhưng vẫn còn thông tin người giám hộ
  return !isMinor && hasGuardian
}

/**
 * Định dạng chuỗi hiển thị thông tin người giám hộ
 * @param {Object} patient 
 * @returns {string|null}
 */
export function formatGuardianDisplay(patient) {
  if (!patient) return null
  const name = patient.guardianName ? String(patient.guardianName).trim() : ''
  const relationship = patient.guardianRelationship ? String(patient.guardianRelationship).trim() : ''
  const phone = patient.guardianPhone ? String(patient.guardianPhone).trim() : ''

  if (!name && !phone) return null

  const parts = []
  if (name) {
    parts.push(relationship ? `${name} (${relationship})` : name)
  }
  if (phone) {
    parts.push(phone)
  }

  return parts.join(' • ')
}
