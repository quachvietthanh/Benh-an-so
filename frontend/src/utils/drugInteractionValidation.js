export function validateOverrideReason(reason) {
  if (typeof reason !== 'string') {
    return {
      valid: false,
      error: 'Vui lòng nhập lý do bỏ qua cảnh báo (không được để trống hoặc chỉ có khoảng trắng).',
      trimmedReason: '',
    }
  }
  const trimmed = reason.trim()
  if (!trimmed) {
    return {
      valid: false,
      error: 'Vui lòng nhập lý do bỏ qua cảnh báo (không được để trống hoặc chỉ có khoảng trắng).',
      trimmedReason: '',
    }
  }
  return {
    valid: true,
    error: '',
    trimmedReason: trimmed,
  }
}

export function isInteractionHandled(warning, confirmedOverrides = []) {
  if (!warning || !warning.ruleId) return false
  return (confirmedOverrides || []).some(
    (override) =>
      String(override.ruleId) === String(warning.ruleId) &&
      typeof override.overrideReason === 'string' &&
      override.overrideReason.trim().length > 0,
  )
}

export function areAllInteractionsHandled(detectedInteractions = [], confirmedOverrides = []) {
  if (!Array.isArray(detectedInteractions) || detectedInteractions.length === 0) {
    return true
  }
  return detectedInteractions.every((warning) => isInteractionHandled(warning, confirmedOverrides))
}

export function getUnhandledInteractions(detectedInteractions = [], confirmedOverrides = []) {
  if (!Array.isArray(detectedInteractions)) return []
  return detectedInteractions.filter((warning) => !isInteractionHandled(warning, confirmedOverrides))
}

export function canSubmitPrescription({
  canPrescribe = true,
  saving = false,
  checkingInteractions = false,
  interactionApiError = null,
  detectedInteractions = [],
  confirmedOverrides = [],
} = {}) {
  if (!canPrescribe) {
    return { allowed: false, reason: 'Chưa đủ điều kiện kê đơn.' }
  }
  if (saving) {
    return { allowed: false, reason: 'Đang lưu đơn thuốc...' }
  }
  if (checkingInteractions) {
    return { allowed: false, reason: 'Đang kiểm tra tương tác thuốc...' }
  }
  if (interactionApiError) {
    return {
      allowed: false,
      reason: 'Không thể kiểm tra tương tác thuốc. Vui lòng thử lại.',
    }
  }
  if (detectedInteractions.length > 0 && !areAllInteractionsHandled(detectedInteractions, confirmedOverrides)) {
    const unhandled = getUnhandledInteractions(detectedInteractions, confirmedOverrides)
    return {
      allowed: false,
      reason: `Đơn thuốc chứa ${unhandled.length}/${detectedInteractions.length} cảnh báo tương tác thuốc chưa được xử lý. Vui lòng điều chỉnh đơn hoặc nhập lý do bỏ qua hợp lệ.`,
    }
  }
  return { allowed: true, reason: '' }
}
