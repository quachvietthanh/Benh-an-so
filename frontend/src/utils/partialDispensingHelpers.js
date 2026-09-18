export const getRemainingQuantity = (item) => {
  if (!item) return 0
  if (typeof item.remainingQuantity === 'number') {
    return Math.max(0, item.remainingQuantity)
  }
  const prescribed = Number(item.quantity || 0)
  const dispensed = Number(item.dispensedQuantity || 0)
  return Math.max(0, prescribed - dispensed)
}

export const validateDispenseQuantity = (inputQty, remainingQty) => {
  if (inputQty === null || inputQty === undefined || inputQty === '') {
    return { isValid: false, error: 'Số lượng không được để trống.' }
  }
  const num = Number(inputQty)
  if (isNaN(num)) {
    return { isValid: false, error: 'Số lượng phải là một số hợp lệ.' }
  }
  if (num < 0) {
    return { isValid: false, error: 'Số lượng không được âm (tối thiểu là 0).' }
  }
  if (num > remainingQty) {
    return {
      isValid: false,
      error: `Số lượng thực cấp (${num}) không được vượt quá số lượng còn lại (${remainingQty}).`,
    }
  }
  return { isValid: true, error: null }
}

export const calculateItemShortage = (inputQty, remainingQty) => {
  const validRemaining = Math.max(0, Number(remainingQty || 0))
  const validDispensed = Math.max(0, Number(inputQty || 0))
  return Math.max(0, validRemaining - validDispensed)
}

export const buildPartialDispensePayload = (quantities = {}, items = []) => {
  if (!Array.isArray(items) || items.length === 0) {
    return { payloadItems: [], hasAnyItemToDispense: false }
  }

  const payloadItems = []

  items.forEach((item) => {
    const itemId = item.id || item.prescriptionItemId
    if (!itemId) return

    const remaining = getRemainingQuantity(item)
    const rawValue = quantities[itemId] !== undefined ? quantities[itemId] : remaining
    const qty = Math.floor(Number(rawValue || 0))

    if (qty > 0) {
      payloadItems.push({
        prescriptionItemId: itemId,
        quantity: Math.min(qty, remaining),
      })
    }
  })

  return {
    payloadItems,
    hasAnyItemToDispense: payloadItems.length > 0,
  }
}

export const hasShortageAfterDispense = (quantities = {}, items = []) => {
  if (!Array.isArray(items) || items.length === 0) return false

  return items.some((item) => {
    const itemId = item.id || item.prescriptionItemId
    const remaining = getRemainingQuantity(item)
    if (remaining <= 0) return false

    const rawValue = quantities[itemId] !== undefined ? quantities[itemId] : remaining
    const qty = Math.floor(Number(rawValue || 0))
    return qty < remaining
  })
}

export const mapDispenseError = (error, prescriptionId, payloadItems = []) => {
  const status = error?.response?.status || error?.status || 0
  const responseData = error?.response?.data || error?.data || {}
  const code = responseData?.code || ''
  const details = responseData?.details || {}

  if (status === 409 && (code === 'INSUFFICIENT_STOCK' || Array.isArray(details.shortages))) {
    const shortages = Array.isArray(details.shortages) ? details.shortages : []
    return {
      status: 409,
      code: 'INSUFFICIENT_STOCK',
      message: 'Không đủ số lượng thuốc tồn kho khả dụng để thực hiện cấp phát.',
      shortages,
    }
  }

  if (code === 'DATA_INTEGRITY_VIOLATION' || (status === 409 && code === 'DATA_INTEGRITY_VIOLATION')) {
    return {
      status: 409,
      code: 'DATA_INTEGRITY_VIOLATION',
      message:
        'Dữ liệu cấp phát không hợp lệ hoặc bị xung đột ràng buộc hệ thống. Vui lòng tải lại trang và thử lại. Nếu lỗi vẫn tiếp diễn, liên hệ quản trị viên hệ thống.',
      shortages: [],
    }
  }

  if (status === 409) {
    return {
      status: 409,
      code: code || 'PRESCRIPTION_INVALID_STATUS',
      message: responseData?.message || 'Đơn thuốc đã được cấp phát đầy đủ hoặc đã bị hủy, không thể tiếp tục cấp phát.',
      shortages: [],
    }
  }

  if (status === 400) {
    return {
      status: 400,
      code: code || 'VALIDATION_FAILED',
      message: responseData?.message || 'Số lượng cấp phát không hợp lệ.',
      shortages: [],
    }
  }

  if (status === 403) {
    return {
      status: 403,
      code: 'FORBIDDEN',
      message: 'Bạn không có quyền thực hiện cấp phát thuốc.',
      shortages: [],
    }
  }

  if (status === 500) {
    console.error('[PartialDispense 500 Error]', {
      prescriptionId,
      payload: payloadItems,
      response: responseData,
    })
    return {
      status: 500,
      code: 'INTERNAL_SERVER_ERROR',
      message:
        'Hệ thống đang gặp sự cố khi xử lý cấp phát một phần. Vui lòng liên hệ quản trị viên hệ thống (bộ phận kỹ thuật) để được hỗ trợ trước khi thử lại.',
      shortages: [],
    }
  }

  return {
    status,
    code: code || 'UNKNOWN_ERROR',
    message: responseData?.message || 'Thao tác cấp phát một phần không thành công. Vui lòng thử lại.',
    shortages: [],
  }
}

export const canUserDispense = (roles = [], permissions = []) => {
  const normalizedRoles = (roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const normalizedPermissions = (permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))

  const isPharmacist = normalizedRoles.includes('pharmacist')
  const isAdmin = normalizedRoles.includes('admin')
  const hasPerm = normalizedPermissions.includes('PRESCRIPTION_UPDATE_STATUS') || normalizedPermissions.includes('PHARMACY_READ')

  const isPureDoctor = normalizedRoles.includes('doctor') && !isPharmacist && !isAdmin
  return (isPharmacist || isAdmin || hasPerm) && !isPureDoctor
}

export const canUserViewDispenseHistory = (roles = [], permissions = []) => {
  const normalizedRoles = (roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const normalizedPermissions = (permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))

  return (
    normalizedRoles.includes('doctor') ||
    normalizedRoles.includes('pharmacist') ||
    normalizedRoles.includes('admin') ||
    normalizedRoles.includes('manager') ||
    normalizedPermissions.includes('PRESCRIPTION_DISPENSE_HISTORY_READ') ||
    normalizedPermissions.includes('PRESCRIPTION_READ')
  )
}

export const calculateBillingItemAmount = (item, prescriptionStatus) => {
  if (!item) return { effectiveQty: 0, unitPrice: 0, amount: 0 }

  const unitPrice = Number(item.unitPrice || item.price || 0)
  let effectiveQty = 0

  if (prescriptionStatus === 'PARTIALLY_DISPENSED') {
    effectiveQty = Math.max(0, Number(item.dispensedQuantity || 0))
  } else if (prescriptionStatus === 'DISPENSED') {
    effectiveQty = Math.max(0, Number(item.dispensedQuantity != null ? item.dispensedQuantity : item.quantity || 0))
  } else {
    effectiveQty = Math.max(0, Number(item.quantity || 0))
  }

  return {
    effectiveQty,
    unitPrice,
    amount: effectiveQty * unitPrice,
  }
}
