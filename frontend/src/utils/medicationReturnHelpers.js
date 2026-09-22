/**
 * Helper utilities for Medication Return and Dispense Slip Cancellation (NCL-06-CN-009 / QTN-32, QTN-06, QTN-14)
 */

export const CLINICAL_TIMEZONE = 'Asia/Ho_Chi_Minh'
export const MAX_REASON_LENGTH = 500

/**
 * Format a Date or timestamp to YYYY-MM-DD in Asia/Ho_Chi_Minh timezone.
 * Uses native Intl.DateTimeFormat for zero-dependency accuracy.
 */
export const formatDateInTimezone = (dateInput, timezone = CLINICAL_TIMEZONE) => {
  if (!dateInput) return null
  try {
    const d = dateInput instanceof Date ? dateInput : new Date(dateInput)
    if (isNaN(d.getTime())) return null

    const formatter = new Intl.DateTimeFormat('en-CA', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })
    return formatter.format(d)
  } catch {
    return null
  }
}

/**
 * Checks whether a given dispensing timestamp was created today in Asia/Ho_Chi_Minh timezone.
 * Used to actively disable or hide non-today dispense slips on the UI.
 *
 * @param {string|Date} dispensedAt - Timestamp of the dispensing action
 * @param {string|Date} [referenceDate] - Optional reference date (defaults to current system time)
 * @returns {boolean} true if dispensedAt is today in VN timezone
 */
export const isDispensedToday = (dispensedAt, referenceDate = new Date()) => {
  if (!dispensedAt) return false
  const dispensedDateStr = formatDateInTimezone(dispensedAt)
  const todayDateStr = formatDateInTimezone(referenceDate)
  if (!dispensedDateStr || !todayDateStr) return false
  return dispensedDateStr === todayDateStr
}

/**
 * Validates the return medication form before sending to Backend.
 *
 * @param {string} reason - Return reason (required, max 500 chars)
 * @param {Array<Object>} items - Array of items with { dispenseItemId, quantity, maxReturnable, medicineName }
 * @returns {{ isValid: boolean, error: string | null, validItems: Array<{ dispenseItemId: string, quantity: number }> }}
 */
export const validateReturnForm = (reason, items = []) => {
  const trimmedReason = typeof reason === 'string' ? reason.trim() : ''

  if (!trimmedReason) {
    return {
      isValid: false,
      error: 'Vui lòng nhập lý do trả lại thuốc.',
      validItems: [],
    }
  }

  if (trimmedReason.length > MAX_REASON_LENGTH) {
    return {
      isValid: false,
      error: `Lý do trả thuốc không được vượt quá ${MAX_REASON_LENGTH} ký tự (hiện có ${trimmedReason.length} ký tự).`,
      validItems: [],
    }
  }

  const validItems = []

  for (const item of items) {
    const qty = Number(item?.quantity)
    if (isNaN(qty) || qty < 0) {
      return {
        isValid: false,
        error: `Số lượng trả lại của thuốc "${item?.medicineName || 'đã chọn'}" không hợp lệ (phải là số không âm).`,
        validItems: [],
      }
    }

    if (qty > 0) {
      if (!Number.isInteger(qty)) {
        return {
          isValid: false,
          error: `Số lượng trả lại của thuốc "${item?.medicineName || 'đã chọn'}" phải là số nguyên dương.`,
          validItems: [],
        }
      }

      const maxLimit = item?.maxReturnable != null ? Number(item.maxReturnable) : null
      if (maxLimit != null && !isNaN(maxLimit) && qty > maxLimit) {
        return {
          isValid: false,
          error: `Số lượng trả lại của thuốc "${item?.medicineName || 'đã chọn'}" (${qty}) vượt quá số lượng tối đa có thể trả (${maxLimit}).`,
          validItems: [],
        }
      }

      if (!item.dispenseItemId) {
        return {
          isValid: false,
          error: 'Thiếu mã dòng cấp phát thuốc (dispenseItemId).',
          validItems: [],
        }
      }

      validItems.push({
        dispenseItemId: String(item.dispenseItemId),
        quantity: qty,
      })
    }
  }

  if (validItems.length === 0) {
    return {
      isValid: false,
      error: 'Vui lòng nhập số lượng nhận lại lớn hơn 0 cho ít nhất một loại thuốc.',
      validItems: [],
    }
  }

  return {
    isValid: true,
    error: null,
    validItems,
  }
}

/**
 * Maps Backend HTTP errors to friendly, specific Vietnamese messages.
 *
 * @param {any} error - Axios error object or error response
 * @returns {string} User-facing error message in Vietnamese
 */
export const mapReturnErrorMessage = (error) => {
  if (!error) return 'Đã xảy ra lỗi không xác định.'
  if (typeof error === 'string') return error

  const status = error?.response?.status || error?.status
  const data = error?.response?.data
  const serverMsg =
    (typeof data === 'object' ? data?.message || data?.error || data?.detail : '') ||
    (typeof data === 'string' ? data : '') ||
    error?.message ||
    ''

  const lowerMsg = String(serverMsg).toLowerCase()

  // 403 Forbidden
  if (status === 403) {
    return 'Bạn không có quyền thực hiện thao tác trả thuốc (chỉ dành cho Dược sĩ hoặc Quản trị viên).'
  }

  // 409 Conflict
  if (status === 409) {
    if (
      lowerMsg.includes('payment') ||
      lowerMsg.includes('refunded') ||
      lowerMsg.includes('thu phí') ||
      lowerMsg.includes('hoàn tiền') ||
      lowerMsg.includes('prescriptionreturnpaymentnotrefundedexception')
    ) {
      return 'Lượt khám này đã được thu phí và chưa hoàn tiền. Vui lòng hoàn tiền trước khi trả thuốc.'
    }
    if (
      lowerMsg.includes('was not created today') ||
      lowerMsg.includes('not created today') ||
      lowerMsg.includes('today') ||
      lowerMsg.includes('trong ngày')
    ) {
      return 'Chỉ có thể trả thuốc cho phiếu cấp phát được lập TRONG NGÀY HÔM NAY.'
    }
    return serverMsg || 'Xung đột dữ liệu: Lượt khám đã thu phí hoặc phiếu không đủ điều kiện hoàn trả.'
  }

  // 400 Bad Request / Validation
  if (status === 400) {
    if (
      lowerMsg.includes('was not created today') ||
      lowerMsg.includes('not created today') ||
      lowerMsg.includes('today')
    ) {
      return 'Chỉ có thể trả thuốc cho phiếu cấp phát được lập TRONG NGÀY HÔM NAY.'
    }
    if (
      lowerMsg.includes('payment') ||
      lowerMsg.includes('refunded') ||
      lowerMsg.includes('prescriptionreturnpaymentnotrefundedexception')
    ) {
      return 'Lượt khám này đã được thu phí và chưa hoàn tiền. Vui lòng hoàn tiền trước khi trả thuốc.'
    }
    if (lowerMsg.includes('reason is required') || lowerMsg.includes('return reason is required')) {
      return 'Vui lòng nhập lý do trả thuốc.'
    }
    if (
      lowerMsg.includes('only dispensed or partially dispensed') ||
      lowerMsg.includes('prescriptioninvalidstatusexception')
    ) {
      return 'Chỉ đơn thuốc ở trạng thái [Đã cấp phát] hoặc [Cấp phát một phần] mới có thể hoàn trả thuốc. Đơn thuốc này có thể đã được hủy thành công trước đó, vui lòng làm mới danh sách.'
    }
    if (lowerMsg.includes('cannot exceed the dispensed quantity')) {
      return 'Tổng số lượng trả lại vượt quá tổng số lượng thuốc đã cấp thực tế của đơn thuốc. Vui lòng kiểm tra lại số lượng nhận lại.'
    }
    if (lowerMsg.includes('exceeds the remaining returnable quantity')) {
      return 'Số lượng trả lại không hợp lệ hoặc vượt quá số lượng còn lại có thể trả.'
    }
    if (lowerMsg.includes('greater than zero') || lowerMsg.includes('must be greater than zero')) {
      return 'Số lượng nhận lại của thuốc phải lớn hơn 0.'
    }
    if (lowerMsg.includes('exceed') || lowerMsg.includes('quantity')) {
      return serverMsg || 'Số lượng trả lại không hợp lệ hoặc vượt quá số lượng còn lại có thể trả.'
    }
    if (lowerMsg.includes('at least one') || lowerMsg.includes('items is required')) {
      return 'Vui lòng chọn ít nhất một loại thuốc có số lượng trả lớn hơn 0.'
    }
    return serverMsg || 'Dữ liệu trả thuốc không hợp lệ. Vui lòng kiểm tra lại.'
  }

  // 404 Not Found
  if (status === 404) {
    return 'Không tìm thấy đơn thuốc hoặc chi tiết dòng cấp phát trong hệ thống.'
  }

  // 500 Internal Server Error
  if (status >= 500) {
    return 'Lỗi hệ thống máy chủ khi xử lý hoàn trả thuốc. Vui lòng liên hệ quản trị viên hoặc thử lại sau.'
  }

  return serverMsg || 'Không thể kết nối đến máy chủ hoặc lỗi không xác định. Vui lòng thử lại.'
}

/**
 * Calculates projected prescription status after medication return.
 *
 * @param {Array<Object>} historyItems - List of dispense items for this prescription
 * @param {Record<string, number>} returnQuantities - Map of dispenseItemId -> returning quantity
 * @returns {{ projectedStatus: 'CANCELLED' | 'PARTIALLY_DISPENSED' | 'UNCHANGED', label: string, color: string, isFullCancellation: boolean, description: string }}
 */
export const calculateProjectedStatus = (
  historyItems = [],
  returnQuantities = {},
  prescriptionItems = []
) => {
  if (!Array.isArray(historyItems) || historyItems.length === 0) {
    return {
      projectedStatus: 'UNCHANGED',
      label: 'Không xác định',
      color: 'default',
      isFullCancellation: false,
      description: 'Chưa có dữ liệu cấp phát để dự tính.',
    }
  }

  let totalReturningNow = 0
  Object.values(returnQuantities).forEach((qty) => {
    const num = Number(qty) || 0
    if (num > 0) totalReturningNow += num
  })

  if (totalReturningNow <= 0) {
    return {
      projectedStatus: 'UNCHANGED',
      label: 'Chưa chọn số lượng trả',
      color: 'default',
      isFullCancellation: false,
      description: 'Chưa có loại thuốc nào được nhập số lượng nhận lại.',
    }
  }

  // Nếu có danh sách các thuốc thực tế trong đơn thuốc (PrescriptionItem):
  // Tính theo đúng contract Backend: Đơn thuốc thành CANCELLED khi tất cả các thuốc đều không còn số lượng đã cấp (> 0)
  if (Array.isArray(prescriptionItems) && prescriptionItems.length > 0) {
    const returningByRxItem = {}
    historyItems.forEach((hist) => {
      const qty = Number(returnQuantities[hist.id]) || 0
      if (qty > 0 && hist.prescriptionItemId) {
        returningByRxItem[hist.prescriptionItemId] =
          (returningByRxItem[hist.prescriptionItemId] || 0) + qty
      }
    })

    const allPrescriptionItemsCleared = prescriptionItems.every((rxItem) => {
      const currentlyDispensed = Number(rxItem.dispensedQuantity || 0)
      const returningForThisItem = Number(returningByRxItem[rxItem.id] || 0)
      return currentlyDispensed - returningForThisItem <= 0
    })

    if (allPrescriptionItemsCleared) {
      return {
        projectedStatus: 'CANCELLED',
        label: 'Đã hủy',
        color: 'red',
        isFullCancellation: true,
        description:
          'Toàn bộ thuốc đã cấp phát sẽ được nhận lại hoàn toàn. Đơn thuốc sẽ tự động chuyển sang trạng thái ĐÃ HỦY.',
      }
    }

    return {
      projectedStatus: 'PARTIALLY_DISPENSED',
      label: 'Cấp phát một phần',
      color: 'gold',
      isFullCancellation: false,
      description:
        'Chỉ một phần thuốc được nhận lại. Đơn thuốc sẽ giữ trạng thái CẤP PHÁT MỘT PHẦN với số lượng đã cấp được điều chỉnh giảm.',
    }
  }

  let totalDispensed = 0
  let totalPreviouslyReturned = 0

  historyItems.forEach((item) => {
    const dispensed = Number(item.dispensedQuantity || 0)
    const returned = Number(item.returnedQuantity || 0)

    totalDispensed += dispensed
    totalPreviouslyReturned += returned
  })

  const remainingAfter = totalDispensed - (totalPreviouslyReturned + totalReturningNow)

  if (remainingAfter <= 0) {
    return {
      projectedStatus: 'CANCELLED',
      label: 'Đã hủy',
      color: 'red',
      isFullCancellation: true,
      description:
        'Toàn bộ thuốc đã cấp phát sẽ được nhận lại hoàn toàn. Đơn thuốc sẽ tự động chuyển sang trạng thái ĐÃ HỦY.',
    }
  }

  return {
    projectedStatus: 'PARTIALLY_DISPENSED',
    label: 'Cấp phát một phần',
    color: 'gold',
    isFullCancellation: false,
    description:
      'Chỉ một phần thuốc được nhận lại. Đơn thuốc sẽ giữ trạng thái CẤP PHÁT MỘT PHẦN với số lượng đã cấp được điều chỉnh giảm.',
  }
}

/**
 * Calculates estimated refund amount if unit prices are available.
 *
 * @param {Array<{ quantity: number, unitPrice?: number, medicineId?: string }>} items
 * @param {Record<string, number>} [priceMap] - Optional map of medicineId -> price
 * @returns {number} Estimated total refund amount (VND)
 */
export const calculateReturnRefundAmount = (items = [], priceMap = {}) => {
  if (!Array.isArray(items)) return 0
  return items.reduce((sum, it) => {
    const qty = Number(it?.quantity || 0)
    if (qty <= 0) return sum
    const price = Number(it?.unitPrice ?? priceMap[it?.medicineId] ?? 0)
    return sum + (price > 0 ? qty * price : 0)
  }, 0)
}
