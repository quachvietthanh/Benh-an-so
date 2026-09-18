import { fixMojibake } from './serviceCatalogValidation.js'
import { getDiseaseGroupName } from './icd10Data.js'

/**
 * Danh sách gợi ý các bệnh mắc kèm thường gặp trong thực hành lâm sàng
 */
export const COMMON_COMORBIDITIES_SUGGESTIONS = [
  { code: 'I10', name: 'Tăng huyết áp vô căn (nguyên phát)', diseaseGroup: 'Bệnh hệ tuần hoàn', category: 'CIRCULATORY' },
  { code: 'E11', name: 'Đái tháo đường không phụ thuộc insulin (typ 2)', diseaseGroup: 'Bệnh nội tiết, chuyển hóa', category: 'ENDOCRINE' },
  { code: 'E78', name: 'Rối loạn chuyển hóa lipoprotein và tăng lipid máu', diseaseGroup: 'Bệnh nội tiết, chuyển hóa', category: 'ENDOCRINE' },
  { code: 'K21', name: 'Bệnh trào ngược dạ dày - thực quản', diseaseGroup: 'Bệnh hệ tiêu hóa', category: 'DIGESTIVE' },
  { code: 'K25', name: 'Loét dạ dày', diseaseGroup: 'Bệnh hệ tiêu hóa', category: 'DIGESTIVE' },
  { code: 'J45', name: 'Hen phế quản (Suyễn)', diseaseGroup: 'Bệnh hệ hô hấp', category: 'RESPIRATORY' },
  { code: 'M19', name: 'Thoái hóa khớp khác', diseaseGroup: 'Bệnh hệ cơ xương khớp', category: 'MUSCULOSKELETAL' },
  { code: 'I25', name: 'Bệnh tim thiếu máu cục bộ mạn', diseaseGroup: 'Bệnh hệ tuần hoàn', category: 'CIRCULATORY' },
  { code: 'N18', name: 'Bệnh thận mạn tính', diseaseGroup: 'Bệnh hệ sinh dục - tiết niệu', category: 'GENITOURINARY' },
  { code: 'K76.0', name: 'Thoái hóa mỡ gan (Gan nhiễm mỡ)', diseaseGroup: 'Bệnh hệ tiêu hóa', category: 'DIGESTIVE' },
]

/**
 * Chuẩn hóa một mục bệnh mắc kèm
 */
export const normalizeComorbidityItem = (item) => {
  if (!item) return null
  const code = item.code ? String(item.code).trim().toUpperCase() : ''
  const rawName = item.rawName || item.name || ''
  const name = fixMojibake(rawName)
  const id = item.id || item.diagnosisCatalogId || null
  const note = item.note || ''
  const diseaseGroup = item.diseaseGroup || getDiseaseGroupName(code, item.diseaseGroup)
  const category = item.category || 'GENERAL'

  return {
    id,
    code,
    rawName,
    name,
    note,
    diseaseGroup,
    category,
  }
}

/**
 * NCL-13-CN-006-TC-02: Kiểm tra xem có thể thêm bệnh mắc kèm ứng viên không
 * - Chặn trùng lặp với chẩn đoán chính
 * - Chặn trùng lặp với danh sách bệnh kèm đã có
 */
export const validateCanAddComorbidity = ({ primaryIcd, secondaryIcds = [], candidateIcd }) => {
  if (!candidateIcd) {
    return {
      valid: false,
      error: 'Vui lòng chọn hoặc nhập thông tin bệnh mắc kèm hợp lệ.',
      type: 'INVALID_INPUT',
    }
  }

  const candidateCode = candidateIcd.code ? String(candidateIcd.code).trim().toUpperCase() : ''
  const candidateName = candidateIcd.name ? String(candidateIcd.name).trim().toLowerCase() : ''
  const candidateId = candidateIcd.id ? String(candidateIcd.id).trim() : ''

  if (!candidateCode && !candidateName) {
    return {
      valid: false,
      error: 'Mã ICD hoặc tên bệnh mắc kèm không được để trống.',
      type: 'EMPTY_DIAGNOSIS',
    }
  }

  // 1. Kiểm tra trùng lặp với Chẩn đoán chính (Primary Diagnosis)
  if (primaryIcd) {
    const primaryCode = primaryIcd.code ? String(primaryIcd.code).trim().toUpperCase() : ''
    const primaryId = primaryIcd.id ? String(primaryIcd.id).trim() : ''
    const primaryName = primaryIcd.name ? String(primaryIcd.name).trim().toLowerCase() : ''

    const isMatchByCode = candidateCode && primaryCode && candidateCode === primaryCode
    const isMatchById = candidateId && primaryId && candidateId === primaryId
    const isMatchByName = !candidateCode && !primaryCode && candidateName && primaryName && candidateName === primaryName

    if (isMatchByCode || isMatchById || isMatchByName) {
      return {
        valid: false,
        error: `Mã bệnh "${candidateCode || candidateIcd.name}" đã được chọn làm chẩn đoán chính. Không thể thêm làm bệnh mắc kèm.`,
        type: 'DUPLICATE_PRIMARY',
      }
    }
  }

  // 2. Kiểm tra trùng lặp với danh sách bệnh kèm đã có (Secondary Diagnoses)
  const isDuplicateSecondary = (Array.isArray(secondaryIcds) ? secondaryIcds : []).some((sec) => {
    if (!sec) return false
    const secCode = sec.code ? String(sec.code).trim().toUpperCase() : ''
    const secId = sec.id ? String(sec.id).trim() : ''
    const secName = sec.name ? String(sec.name).trim().toLowerCase() : ''

    if (candidateCode && secCode && candidateCode === secCode) return true
    if (candidateId && secId && candidateId === secId) return true
    if (!candidateCode && !secCode && candidateName && secName && candidateName === secName) return true
    return false
  })

  if (isDuplicateSecondary) {
    return {
      valid: false,
      error: `Mã bệnh "${candidateCode || candidateIcd.name}" đã có trong danh sách bệnh mắc kèm.`,
      type: 'DUPLICATE_SECONDARY',
    }
  }

  return {
    valid: true,
    error: null,
    type: null,
  }
}

/**
 * NCL-13-CN-006-TC-03: Quy tắc QTN-22
 * Mỗi chẩn đoán chính phải chọn được một mã bệnh trong danh mục.
 * Hệ thống yêu cầu phải có đúng một chẩn đoán chính trước khi lưu.
 */
export const validateDiagnosesSubmission = ({ primaryIcd, secondaryIcds = [] }) => {
  const errors = []

  // QTN-22: Bắt buộc phải có đúng một chẩn đoán chính
  const hasPrimaryCode = primaryIcd && Boolean(primaryIcd.code || primaryIcd.name)
  const hasPrimaryValid = Boolean(primaryIcd && (primaryIcd.id || primaryIcd.code))

  if (!hasPrimaryCode || !hasPrimaryValid) {
    errors.push('Yêu cầu phải có đúng một chẩn đoán chính. Vui lòng chọn mã bệnh chẩn đoán chính trước khi lưu.')
    return {
      valid: false,
      errors,
      errorCode: 'QTN_22_PRIMARY_REQUIRED',
    }
  }

  // Kiểm tra trùng lặp giữa chẩn đoán chính và bệnh kèm
  const primaryCode = String(primaryIcd.code || '').trim().toUpperCase()
  const secList = Array.isArray(secondaryIcds) ? secondaryIcds : []

  const duplicateWithPrimary = secList.find((sec) => {
    const secCode = sec?.code ? String(sec.code).trim().toUpperCase() : ''
    return secCode && primaryCode && secCode === primaryCode
  })

  if (duplicateWithPrimary) {
    errors.push(`Mã bệnh "${duplicateWithPrimary.code}" vừa là chẩn đoán chính vừa nằm trong danh sách bệnh mắc kèm.`)
  }

  // Kiểm tra trùng lặp trong các bệnh kèm
  const seenCodes = new Set()
  for (const sec of secList) {
    const secCode = sec?.code ? String(sec.code).trim().toUpperCase() : ''
    if (secCode) {
      if (seenCodes.has(secCode)) {
        errors.push(`Mã bệnh mắc kèm "${secCode}" bị trùng lặp nhiều lần trong danh sách.`)
        break
      }
      seenCodes.add(secCode)
    }
  }

  return {
    valid: errors.length === 0,
    errors,
    errorCode: errors.length > 0 ? 'VALIDATION_FAILED' : null,
  }
}

/**
 * Định dạng chuỗi tóm tắt chẩn đoán đầy đủ cho bệnh án và phiếu in
 */
export const formatComorbiditiesSummary = (primaryIcd, secondaryIcds = []) => {
  const parts = []

  if (primaryIcd) {
    const codePart = primaryIcd.code ? `[${primaryIcd.code}] ` : ''
    const notePart = primaryIcd.note ? ` (${primaryIcd.note})` : ''
    parts.push(`Chẩn đoán chính: ${codePart}${fixMojibake(primaryIcd.name || '')}${notePart}`)
  } else {
    parts.push('Chẩn đoán chính: Chưa xác định')
  }

  const validSecondaries = (Array.isArray(secondaryIcds) ? secondaryIcds : []).filter(Boolean)
  if (validSecondaries.length > 0) {
    const secondaryStr = validSecondaries
      .map((sec, idx) => {
        const codePart = sec.code ? `[${sec.code}] ` : ''
        const notePart = sec.note ? ` (${sec.note})` : ''
        return `${idx + 1}. ${codePart}${fixMojibake(sec.name || '')}${notePart}`
      })
      .join(', ')
    parts.push(`Bệnh mắc kèm: ${secondaryStr}`)
  }

  return parts.join('; ')
}
