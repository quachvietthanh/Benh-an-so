import dayjs from 'dayjs'

/**
 * Trạng thái chuẩn của Phiếu dự trù mua thuốc
 */
export const PROCUREMENT_STATUS = {
  DRAFT: 'DRAFT',
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  CANCELLED: 'CANCELLED',
}

/**
 * Tính số lượng gợi ý mua theo công thức chuẩn:
 * Gợi ý = max(0, (Lượng cấp phát kỳ trước + Tồn tối thiểu) - Tồn khả dụng)
 * Nếu thuốc mới chưa có lịch sử cấp phát -> trả về null để dược sĩ tự nhập
 */
export const calculateSuggestedQuantity = (currentStock, minStockThreshold, previousPeriodConsumption) => {
  const stock = Number(currentStock) || 0
  const minStock = Number(minStockThreshold) || 0

  if (
    previousPeriodConsumption === null ||
    previousPeriodConsumption === undefined ||
    isNaN(Number(previousPeriodConsumption)) ||
    Number(previousPeriodConsumption) < 0
  ) {
    return null
  }

  const consumption = Number(previousPeriodConsumption)
  const suggested = Math.max(0, consumption + minStock - stock)
  return suggested
}

/**
 * Kiểm tra xem thuốc đã có đủ dữ liệu tiêu thụ kỳ trước hay chưa
 */
export const hasEnoughConsumptionHistory = (item) => {
  if (!item) return false
  const consumption = item.previousPeriodConsumption
  return (
    consumption !== null &&
    consumption !== undefined &&
    !isNaN(Number(consumption)) &&
    Number(consumption) >= 0 &&
    item.hasConsumptionData !== false
  )
}

/**
 * Lấy nhãn, màu sắc và icon hiển thị cho trạng thái phiếu dự trù
 */
export const getProcurementStatusMeta = (status) => {
  const norm = String(status || '').toUpperCase()

  switch (norm) {
    case PROCUREMENT_STATUS.PENDING_APPROVAL:
      return {
        status: PROCUREMENT_STATUS.PENDING_APPROVAL,
        label: 'Chờ duyệt',
        color: 'orange',
        badgeColor: '#ea580c',
        textColor: '#c2410c',
        bgLight: '#fff7ed',
        borderLight: '#fed7aa',
        canEdit: true,
        canApprove: true,
      }
    case PROCUREMENT_STATUS.APPROVED:
      return {
        status: PROCUREMENT_STATUS.APPROVED,
        label: 'Đã duyệt',
        color: 'green',
        badgeColor: '#16a34a',
        textColor: '#15803d',
        bgLight: '#f0fdf4',
        borderLight: '#bbf7d0',
        canEdit: false,
        canApprove: false,
      }
    case PROCUREMENT_STATUS.REJECTED:
      return {
        status: PROCUREMENT_STATUS.REJECTED,
        label: 'Bị từ chối',
        color: 'error',
        badgeColor: '#dc2626',
        textColor: '#b91c1c',
        bgLight: '#fef2f2',
        borderLight: '#fecaca',
        canEdit: false,
        canApprove: false,
      }
    case PROCUREMENT_STATUS.DRAFT:
      return {
        status: PROCUREMENT_STATUS.DRAFT,
        label: 'Bản nháp',
        color: 'default',
        badgeColor: '#64748b',
        textColor: '#475569',
        bgLight: '#f8fafc',
        borderLight: '#e2e8f0',
        canEdit: true,
        canApprove: false,
      }
    case PROCUREMENT_STATUS.CANCELLED:
      return {
        status: PROCUREMENT_STATUS.CANCELLED,
        label: 'Đã hủy',
        color: 'default',
        badgeColor: '#94a3b8',
        textColor: '#64748b',
        bgLight: '#f8fafc',
        borderLight: '#e2e8f0',
        canEdit: false,
        canApprove: false,
      }
    default:
      return {
        status: 'UNKNOWN',
        label: status || 'Không rõ',
        color: 'default',
        badgeColor: '#94a3b8',
        textColor: '#64748b',
        bgLight: '#f8fafc',
        borderLight: '#e2e8f0',
        canEdit: false,
        canApprove: false,
      }
  }
}

/**
 * Kiểm tra xem người dùng hiện tại có được phép duyệt phiếu này hay không.
 * QUY TẮC SOD (Separation of Duties): Người tạo phiếu KHÔNG được tự duyệt hoặc từ chối phiếu của mình!
 */
export const canApproveOrRejectPlan = (plan, currentUserId) => {
  if (!plan) return { allowed: false, reason: 'Không có dữ liệu phiếu.' }

  if (plan.status !== PROCUREMENT_STATUS.PENDING_APPROVAL) {
    return {
      allowed: false,
      reason: `Chỉ có thể phê duyệt hoặc từ chối phiếu đang ở trạng thái 'Chờ duyệt' (Hiện tại: ${plan.status}).`,
    }
  }

  if (currentUserId && plan.createdBy && String(plan.createdBy) === String(currentUserId)) {
    return {
      allowed: false,
      isSoDViolation: true,
      reason: 'Quy tắc SoD: Bạn không thể tự phê duyệt hoặc từ chối phiếu dự trù do chính mình lập.',
    }
  }

  return { allowed: true, reason: '' }
}

/**
 * Kiểm tra tính hợp lệ của dữ liệu trước khi gửi phiếu dự trù
 */
export const validateProcurementPlanForm = (items = [], note = '') => {
  const errors = []

  if (!Array.isArray(items) || items.length === 0) {
    errors.push('Phiếu dự trù phải có ít nhất một loại thuốc.')
    return { valid: false, errors }
  }

  // Kiểm tra trùng mã thuốc
  const seenIds = new Set()
  let hasDuplicate = false
  for (const item of items) {
    if (seenIds.has(item.medicineId)) {
      hasDuplicate = true
      break
    }
    seenIds.add(item.medicineId)
  }
  if (hasDuplicate) {
    errors.push('Không được có loại thuốc trùng lặp trong cùng một phiếu dự trù.')
  }

  // Kiểm tra số lượng đề xuất cho từng dòng
  let invalidQtyCount = 0
  items.forEach((item, idx) => {
    const qty = Number(item.proposedQuantity)
    if (isNaN(qty) || qty <= 0 || !Number.isInteger(qty)) {
      invalidQtyCount++
    }
  })
  if (invalidQtyCount > 0) {
    errors.push(`Có ${invalidQtyCount} loại thuốc chưa nhập số lượng đặt hợp lệ (phải là số nguyên > 0).`)
  }

  return {
    valid: errors.length === 0,
    errors,
  }
}

/**
 * Kiểm tra tính hợp lệ của lý do từ chối phiếu
 */
export const validateRejectionReason = (reason) => {
  if (!reason || typeof reason !== 'string' || !reason.trim()) {
    return { valid: false, message: 'Lý do từ chối không được để trống.' }
  }
  if (reason.trim().length < 5) {
    return { valid: false, message: 'Lý do từ chối phải có ít nhất 5 ký tự.' }
  }
  return { valid: true, message: '' }
}

/**
 * Định dạng ngày (DD/MM/YYYY)
 */
export const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = dayjs(dateStr)
  return d.isValid() ? d.format('DD/MM/YYYY') : '—'
}

/**
 * Định dạng ngày giờ (DD/MM/YYYY HH:mm)
 */
export const formatDateTime = (dateStr) => {
  if (!dateStr) return '—'
  const d = dayjs(dateStr)
  return d.isValid() ? d.format('DD/MM/YYYY HH:mm') : '—'
}

/**
 * Tạo giải thích công thức gợi ý dạng văn bản ngắn gọn
 */
export const getSuggestionFormulaText = (currentStock, minStock, consumption, suggested) => {
  if (consumption === null || consumption === undefined) {
    return 'Chưa đủ dữ liệu tiêu thụ kỳ trước để tính gợi ý tự động. Vui lòng tự nhập số lượng cần mua.'
  }
  return `Gợi ý ≈ Tồn tối thiểu (${minStock}) + Tiêu thụ kỳ trước (${consumption}) - Tồn hiện tại (${currentStock}) = ${suggested}`
}
