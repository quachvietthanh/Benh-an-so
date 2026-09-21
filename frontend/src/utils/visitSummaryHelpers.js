import dayjs from 'dayjs'

/**
 * Kiểm tra xem bệnh án của lượt khám đã được ký hợp lệ để in phiếu tóm tắt chưa (NCL-04-CN-011-TC-02)
 * Bắt buộc trạng thái phải là SIGNED, LOCKED, hoặc ARCHIVED
 */
export const isMedicalRecordSignedForSummary = (medicalRecord) => {
  if (!medicalRecord) return false
  const status = typeof medicalRecord === 'string' ? medicalRecord : medicalRecord.status
  const normalizedStatus = String(status || '').toUpperCase()
  return (
    normalizedStatus === 'SIGNED' ||
    normalizedStatus === 'LOCKED' ||
    normalizedStatus === 'ARCHIVED'
  )
}

/**
 * Định dạng nhãn giới tính thuần tiếng Việt
 */
export const formatGenderVi = (gender) => {
  if (!gender) return 'Không xác định'
  const g = String(gender).toUpperCase()
  if (g === 'MALE' || g === 'NAM') return 'Nam'
  if (g === 'FEMALE' || g === 'NU' || g === 'NỮ') return 'Nữ'
  return 'Khác'
}

/**
 * Tính số tuổi từ ngày sinh
 */
export const calculateAgeFromDob = (dob) => {
  if (!dob) return ''
  const parsed = dayjs(dob)
  if (!parsed.isValid()) return ''
  const diff = dayjs().diff(parsed, 'year')
  return diff >= 0 ? `${diff} tuổi` : ''
}

/**
 * Định dạng ngày giờ chuẩn Việt Nam (DD/MM/YYYY HH:mm)
 */
export const formatDateTimeVi = (dateVal, fallback = '---') => {
  if (!dateVal) return fallback
  const parsed = dayjs(dateVal)
  return parsed.isValid() ? parsed.format('DD/MM/YYYY HH:mm') : fallback
}

/**
 * Định dạng ngày chuẩn Việt Nam (DD/MM/YYYY)
 */
export const formatDateVi = (dateVal, fallback = '---') => {
  if (!dateVal) return fallback
  const parsed = dayjs(dateVal)
  return parsed.isValid() ? parsed.format('DD/MM/YYYY') : fallback
}

/**
 * Định dạng trạng thái bệnh án sang Tag tiếng Việt và màu sắc tương ứng
 */
export const getRecordStatusBadge = (status) => {
  const s = String(status || '').toUpperCase()
  switch (s) {
    case 'SIGNED':
      return { text: 'Đã ký số', color: 'success', isSigned: true }
    case 'LOCKED':
      return { text: 'Đã khóa', color: 'purple', isSigned: true }
    case 'ARCHIVED':
      return { text: 'Đã lưu trữ', color: 'blue', isSigned: true }
    case 'IN_PROGRESS':
    case 'DRAFT':
    case 'DRAFTING':
      return { text: 'Chưa ký (Đang ghi)', color: 'warning', isSigned: false }
    default:
      return { text: 'Chưa có bệnh án', color: 'default', isSigned: false }
  }
}
