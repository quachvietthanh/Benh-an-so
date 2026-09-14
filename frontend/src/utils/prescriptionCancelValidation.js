/**
 * Quy tắc nghiệp vụ và kiểm tra hợp lệ cho chức năng Hủy đơn thuốc (NCL-05-CN-005)
 * Tuân thủ QTN-27 (Hủy đơn có lý do, không thể khôi phục)
 * Tuân thủ QTN-12 (Chỉ hủy khi chờ cấp phát; đã cấp phát phải dùng chức năng trả thuốc)
 * Tuân thủ TC-01, TC-02, TC-03, TC-04
 */

export const PRESET_CANCEL_REASONS = [
  'Bệnh nhân thay đổi phác đồ / dừng điều trị',
  'Kê nhầm loại thuốc / liều lượng / số lượng',
  'Bệnh nhân từ chối lấy thuốc tại viện',
  'Kê trùng đơn thuốc với lượt khám trước',
  'Bệnh nhân có chống chỉ định phát hiện bổ sung',
]

/**
 * Kiểm tra hợp lệ lý do hủy đơn thuốc (TC-01, TC-02)
 * @param {string} reason 
 * @returns {{ valid: boolean, error: string | null, reason: string }}
 */
export const validateCancelPrescriptionReason = (reason) => {
  if (reason == null) {
    return {
      valid: false,
      error: 'Vui lòng nhập lý do hủy đơn thuốc (1 - 500 ký tự).',
      reason: '',
    }
  }

  const trimmed = String(reason).trim()

  if (trimmed.length === 0) {
    return {
      valid: false,
      error: 'Lý do hủy đơn thuốc không được để trống hoặc chỉ chứa khoảng trắng.',
      reason: '',
    }
  }

  if (trimmed.length > 500) {
    return {
      valid: false,
      error: `Lý do hủy không được vượt quá 500 ký tự (hiện tại: ${trimmed.length} ký tự).`,
      reason: trimmed,
    }
  }

  return {
    valid: true,
    error: null,
    reason: trimmed,
  }
}

/**
 * Kiểm tra quyền và điều kiện hủy đơn thuốc (TC-03, TC-04)
 * @param {Object} params
 * @param {string[]} [params.userRoles]
 * @param {string[]} [params.userPermissions]
 * @param {Object} [params.prescription]
 * @param {string} [params.currentUserId]
 * @returns {{ allowed: boolean, reason: string | null }}
 */
export const canCancelPrescription = ({
  userRoles = [],
  userPermissions = [],
  prescription = null,
  currentUserId = null,
} = {}) => {
  const normalizedRoles = (userRoles || [])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)

  const normalizedPerms = (userPermissions || [])
    .map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))

  const isPharmacist = normalizedRoles.includes('pharmacist')
  const isDoctor = normalizedRoles.includes('doctor')
  const isAdmin = normalizedRoles.includes('admin')
  const hasUpdatePerm = normalizedPerms.includes('PRESCRIPTION_UPDATE')

  // TC-04: Dược sĩ không được phép hủy đơn thuốc (trừ khi có quyền Admin)
  if (isPharmacist && !isAdmin && !isDoctor) {
    return {
      allowed: false,
      reason: 'Dược sĩ không có quyền hủy đơn thuốc. Chỉ bác sĩ kê đơn hoặc quản trị viên mới được thực hiện.',
    }
  }

  // Phải là Bác sĩ, Admin, hoặc có quyền cập nhật đơn
  if (!isDoctor && !isAdmin && !hasUpdatePerm) {
    return {
      allowed: false,
      reason: 'Bạn không có quyền hủy đơn thuốc này. Chức năng chỉ dành cho Bác sĩ hoặc Quản trị viên.',
    }
  }

  if (!prescription) {
    return {
      allowed: false,
      reason: 'Không tìm thấy thông tin đơn thuốc để hủy.',
    }
  }

  // TC-03: Kiểm tra trạng thái đơn thuốc
  if (prescription.status === 'DISPENSED') {
    return {
      allowed: false,
      reason: 'Đơn thuốc đã được cấp phát. Vui lòng sử dụng chức năng trả lại thuốc nếu muốn thu hồi thuốc.',
    }
  }

  if (prescription.status === 'CANCELLED') {
    return {
      allowed: false,
      reason: 'Đơn thuốc này đã được hủy trước đó và không thể hủy lại.',
    }
  }

  if (prescription.status && prescription.status !== 'PENDING_DISPENSE') {
    return {
      allowed: false,
      reason: 'Chỉ có thể hủy đơn thuốc khi đang ở trạng thái chờ cấp phát (PENDING_DISPENSE).',
    }
  }

  // Kiểm tra bác sĩ kê đơn (nếu có thông tin prescribedBy và currentUserId)
  if (
    prescription.prescribedBy &&
    currentUserId &&
    String(prescription.prescribedBy) !== String(currentUserId) &&
    !isAdmin
  ) {
    return {
      allowed: false,
      reason: 'Chỉ bác sĩ đã kê đơn thuốc này (hoặc quản trị viên) mới có quyền hủy đơn.',
    }
  }

  return {
    allowed: true,
    reason: null,
  }
}

/**
 * Trả về thông báo hướng dẫn hoặc chặn theo trạng thái đơn thuốc (TC-03, QTN-12, QTN-27)
 * @param {Object} prescription 
 * @returns {string | null}
 */
export const getCancelRestrictionMessage = (prescription) => {
  if (!prescription) return null
  if (prescription.status === 'DISPENSED') {
    return 'Đơn thuốc đã được cấp phát. Vui lòng sử dụng chức năng trả lại thuốc nếu muốn thu hồi thuốc.'
  }
  if (prescription.status === 'CANCELLED') {
    return 'Đơn thuốc này đã được hủy trước đó và không thể thao tác.'
  }
  if (prescription.status !== 'PENDING_DISPENSE') {
    return 'Chỉ có thể hủy đơn thuốc khi đang ở trạng thái chờ cấp phát.'
  }
  return null
}
