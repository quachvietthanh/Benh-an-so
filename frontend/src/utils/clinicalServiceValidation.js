export const CLINICAL_SERVICE_TYPES = [
  { value: 'LAB_TEST', label: 'Xét nghiệm', shortLabel: 'Xét nghiệm', color: 'blue' },
  { value: 'IMAGING', label: 'Chẩn đoán hình ảnh', shortLabel: 'Chẩn đoán hình ảnh', color: 'cyan' },
  { value: 'OTHER', label: 'Thăm dò chức năng & Khác', shortLabel: 'Thăm dò chức năng & Khác', color: 'purple' },
]

export const CLINICAL_RESULT_DATA_TYPES = [
  { value: 'NUMBER', label: 'Định lượng (Số trị)', shortLabel: 'Định lượng (Số trị)', color: 'green' },
  { value: 'TEXT', label: 'Định tính / Mô tả (Văn bản)', shortLabel: 'Định tính / Mô tả', color: 'orange' },
  { value: 'FILE', label: 'Tệp tin đính kèm (Ảnh / PDF)', shortLabel: 'Tệp tin đính kèm', color: 'geekblue' },
  { value: 'MIXED', label: 'Hỗn hợp (Số, Mô tả, File)', shortLabel: 'Hỗn hợp', color: 'magenta' },
]

export const GENDER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả giới tính (Chung)' },
  { value: 'MALE', label: 'Nam' },
  { value: 'FEMALE', label: 'Nữ' },
  { value: 'OTHER', label: 'Khác' },
]

export const COMMON_UNITS = [
  'mmol/L',
  'mg/dL',
  'g/L',
  'U/L',
  'UI/L',
  '%',
  '10^9/L',
  '10^12/L',
  'fL',
  'pg',
  'mmHg',
  'bpm',
  'ml/min',
]

export const formatGenderDisplay = (gender) => {
  if (!gender) return 'Tất cả'
  switch (String(gender).toUpperCase()) {
    case 'MALE':
      return 'Nam'
    case 'FEMALE':
      return 'Nữ'
    case 'OTHER':
      return 'Khác'
    default:
      return 'Tất cả'
  }
}

export const formatAgeRangeDisplay = (minAge, maxAge) => {
  if (minAge != null && maxAge != null) {
    return `${minAge} - ${maxAge} tuổi`
  }
  if (minAge != null) {
    return `≥ ${minAge} tuổi`
  }
  if (maxAge != null) {
    return `≤ ${maxAge} tuổi`
  }
  return 'Mọi lứa tuổi'
}

export const formatReferenceBoundsDisplay = (lowerBound, upperBound, unit = '') => {
  const unitSuffix = unit ? ` ${unit}` : ''
  if (lowerBound != null && upperBound != null) {
    return `${lowerBound} - ${upperBound}${unitSuffix}`
  }
  if (lowerBound != null) {
    return `≥ ${lowerBound}${unitSuffix}`
  }
  if (upperBound != null) {
    return `≤ ${upperBound}${unitSuffix}`
  }
  return 'Chưa đặt'
}

export const translateClinicalErrorMessage = (error, defaultFallback = 'Thao tác không thành công.') => {
  if (!error) return defaultFallback

  const apiError = error.apiError || error.response?.data
  const errorCode = apiError?.code || error.code || ''
  const message = apiError?.message || error.message || ''

  switch (errorCode) {
    case 'CLINICAL_SERVICE_CODE_ALREADY_EXISTS':
      return 'Mã kỹ thuật cận lâm sàng này đã tồn tại trong hệ thống. Vui lòng chọn mã khác.'
    case 'CLINICAL_SERVICE_CATALOG_NOT_FOUND':
      return 'Không tìm thấy kỹ thuật cận lâm sàng tương ứng.'
    case 'CLINICAL_REFERENCE_RANGE_NOT_FOUND':
      return 'Không tìm thấy thông tin ngưỡng tham chiếu tương ứng.'
    case 'CLINICAL_REFERENCE_RANGE_OVERLAP':
      return 'Ngưỡng tham chiếu bị trùng lặp khoảng tuổi và giới tính với một ngưỡng đang hoạt động khác của dịch vụ này.'
    case 'CLINICAL_SERVICE_UNAVAILABLE':
      return 'Dịch vụ cận lâm sàng hiện đang tạm ngưng hoạt động.'
    case 'VALIDATION_FAILED':
      return message || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại các trường nhập liệu.'
    default:
      if (message && !message.includes('Request failed with status code')) {
        return message
      }
      return defaultFallback
  }
}

export const validateReferenceRangePayload = ({ minAge, maxAge, lowerBound, upperBound }) => {
  const errors = []

  if (minAge != null && minAge < 0) {
    errors.push('Độ tuổi tối thiểu không được là số âm.')
  }
  if (maxAge != null && maxAge < 0) {
    errors.push('Độ tuổi tối đa không được là số âm.')
  }
  if (minAge != null && maxAge != null && Number(minAge) > Number(maxAge)) {
    errors.push('Độ tuổi tối thiểu không được lớn hơn độ tuổi tối đa.')
  }
  if (lowerBound == null && upperBound == null) {
    errors.push('Ngưỡng tham chiếu bắt buộc phải có ít nhất cận dưới hoặc cận trên.')
  }
  if (lowerBound != null && upperBound != null && Number(lowerBound) > Number(upperBound)) {
    errors.push('Cận dưới không được lớn hơn cận trên.')
  }

  return {
    isValid: errors.length === 0,
    errors,
  }
}

export const checkOverlapClientSide = (newRange, existingRanges = [], excludeId = null) => {
  const newGender = newRange.gender === 'ALL' || !newRange.gender ? null : String(newRange.gender).toUpperCase()
  const minA = newRange.minAge != null ? Number(newRange.minAge) : -Infinity
  const maxA = newRange.maxAge != null ? Number(newRange.maxAge) : Infinity

  for (const r of existingRanges) {
    if (excludeId && String(r.id) === String(excludeId)) continue
    if (r.active === false) continue

    const existGender = r.gender === 'ALL' || !r.gender ? null : String(r.gender).toUpperCase()
    const sameGender = (newGender === null && existGender === null) || (newGender !== null && newGender === existGender)

    if (sameGender) {
      const minB = r.minAge != null ? Number(r.minAge) : -Infinity
      const maxB = r.maxAge != null ? Number(r.maxAge) : Infinity

      if (minA <= maxB && minB <= maxA) {
        const genderLabel = formatGenderDisplay(r.gender)
        const ageLabel = formatAgeRangeDisplay(r.minAge, r.maxAge)
        return {
          isOverlap: true,
          conflictingRange: r,
          message: `Khoảng tuổi bị trùng lặp với ngưỡng đang áp dụng: [${genderLabel}, ${ageLabel}].`,
        }
      }
    }
  }

  return { isOverlap: false }
}
