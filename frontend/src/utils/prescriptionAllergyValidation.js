/**
 * Validation & helper utilities for prescription medication allergy alerts (NCL-05-CN-004 / QTN-26)
 */

export const PRESET_ALLERGY_OVERRIDE_REASONS = [
  'Đã hội chẩn chuyên khoa, cân nhắc lợi ích điều trị vượt trội nguy cơ',
  'Đã chuẩn bị sẵn sàng phác đồ xử trí phản vệ và tiền mê chống dị ứng',
  'Bệnh nhân có đáp ứng tốt và dung nạp an toàn ở tiền sử điều trị gần nhất',
  'Không có thuốc thay thế phù hợp trong tình huống cấp cứu / điều trị bắt buộc',
]

/**
 * Validate that an allergy override reason is non-empty and within character limits.
 * @param {string} reason
 * @returns {{ valid: boolean, error: string, trimmedReason: string }}
 */
export function validateAllergyOverrideReason(reason) {
  if (typeof reason !== 'string') {
    return {
      valid: false,
      error: 'Vui lòng nhập lý do bỏ qua cảnh báo dị ứng thuốc (không được để trống hoặc chỉ có khoảng trắng).',
      trimmedReason: '',
    }
  }

  const trimmed = reason.trim()
  if (!trimmed) {
    return {
      valid: false,
      error: 'Vui lòng nhập lý do bỏ qua cảnh báo dị ứng thuốc (không được để trống hoặc chỉ có khoảng trắng).',
      trimmedReason: '',
    }
  }

  if (trimmed.length > 500) {
    return {
      valid: false,
      error: 'Lý do bỏ qua cảnh báo dị ứng không được vượt quá 500 ký tự.',
      trimmedReason: trimmed,
    }
  }

  return {
    valid: true,
    error: '',
    trimmedReason: trimmed,
  }
}

/**
 * Check whether a single allergy warning is satisfied by a confirmed override.
 * @param {Object} warning { allergyId, medicineId }
 * @param {Array} confirmedOverrides [{ allergyId, medicineId, overrideReason }]
 * @returns {boolean}
 */
export function isAllergyHandled(warning, confirmedOverrides = []) {
  if (!warning || !warning.allergyId || !warning.medicineId) return false
  return (confirmedOverrides || []).some(
    (override) =>
      String(override.allergyId) === String(warning.allergyId) &&
      String(override.medicineId) === String(warning.medicineId) &&
      typeof override.overrideReason === 'string' &&
      override.overrideReason.trim().length > 0
  )
}

/**
 * Check whether all detected allergy warnings have confirmed overrides.
 * @param {Array} detectedWarnings
 * @param {Array} confirmedOverrides
 * @returns {boolean}
 */
export function areAllAllergiesHandled(detectedWarnings = [], confirmedOverrides = []) {
  if (!Array.isArray(detectedWarnings) || detectedWarnings.length === 0) {
    return true
  }
  return detectedWarnings.every((warning) => isAllergyHandled(warning, confirmedOverrides))
}

/**
 * Get all allergy warnings that are still unhandled.
 * @param {Array} detectedWarnings
 * @param {Array} confirmedOverrides
 * @returns {Array}
 */
export function getUnhandledAllergies(detectedWarnings = [], confirmedOverrides = []) {
  if (!Array.isArray(detectedWarnings)) return []
  return detectedWarnings.filter((warning) => !isAllergyHandled(warning, confirmedOverrides))
}

/**
 * Build payload array of allergy overrides for Backend CreatePrescriptionRequest / AmendPrescriptionRequest.
 * @param {Array} warnings List of warning items containing allergyId and medicineId
 * @param {string} defaultReason A shared reason if applied to all
 * @param {Object} specificReasons Optional map of [key]: reason
 * @returns {Array<{ allergyId: string, medicineId: string, overrideReason: string }>}
 */
export function buildAllergyOverridesPayload(warnings = [], defaultReason = '', specificReasons = {}) {
  if (!Array.isArray(warnings)) return []

  return warnings
    .filter((w) => Boolean(w && w.allergyId && w.medicineId))
    .map((w) => {
      const key = `${w.allergyId}_${w.medicineId}`
      const reason = (specificReasons[key] || defaultReason || '').trim()
      return {
        allergyId: w.allergyId,
        medicineId: w.medicineId,
        overrideReason: reason,
      }
    })
    .filter((item) => item.overrideReason.length > 0)
}

/**
 * Evaluate whether prescription submission is blocked by unhandled allergy warnings.
 * @param {Object} options
 * @returns {{ allowed: boolean, reason: string }}
 */
export function canSubmitPrescriptionWithAllergies({
  canPrescribe = true,
  saving = false,
  checkingAllergies = false,
  allergyApiError = null,
  detectedAllergies = [],
  confirmedAllergyOverrides = [],
} = {}) {
  if (!canPrescribe) {
    return { allowed: false, reason: 'Chưa đủ điều kiện kê đơn.' }
  }
  if (saving) {
    return { allowed: false, reason: 'Đang lưu đơn thuốc...' }
  }
  if (checkingAllergies) {
    return { allowed: false, reason: 'Đang kiểm tra dị ứng thuốc...' }
  }
  if (allergyApiError) {
    return {
      allowed: false,
      reason: 'Không thể kiểm tra dị ứng thuốc. Vui lòng thử lại.',
    }
  }
  if (detectedAllergies.length > 0 && !areAllAllergiesHandled(detectedAllergies, confirmedAllergyOverrides)) {
    const unhandled = getUnhandledAllergies(detectedAllergies, confirmedAllergyOverrides)
    return {
      allowed: false,
      reason: `Đơn thuốc chứa ${unhandled.length}/${detectedAllergies.length} cảnh báo dị ứng thuốc chưa được xử lý. Bắt buộc bác sĩ phải điều chỉnh đơn hoặc nhập lý do bỏ qua hợp lệ.`,
    }
  }
  return { allowed: true, reason: '' }
}
