/**
 * Tiện ích nghiệp vụ và kiểm tra hợp lệ cho chức năng Gộp hồ sơ bệnh nhân trùng (NCL-02-CN-006).
 * Quy tắc: QTN-33, QTN-10, QTN-02
 * Tiêu chí chấp thuận: TC-01, TC-02, TC-03, TC-04, TC-05
 */

export const MERGE_REASON_PRESETS = [
  'Hồ sơ trùng tiếp đón do bệnh nhân không mang CCCD/BHYT lần đầu',
  'Trùng hồ sơ do tiếp đón tạo mới thay vì tra cứu hồ sơ cũ',
  'Bệnh nhân đổi số điện thoại nên tiếp đón tạo hồ sơ mới',
  'Gộp hồ sơ nghi trùng từ hệ thống gợi ý tự động',
  'Khác',
]

/**
 * Kiểm tra xem người dùng hiện tại có quyền thực hiện gộp hồ sơ hay không (TC-04, QTN-01)
 * Chỉ Lễ tân (RECEPTIONIST), Quản lý (MANAGER), và Quản trị viên (ADMIN) mới được gộp.
 * Bác sĩ (DOCTOR) và Dược sĩ (PHARMACIST) bị từ chối truy cập.
 * @param {string[]} [roles]
 * @param {string[]} [permissions]
 * @returns {boolean}
 */
export function canUserMergePatients(roles = [], permissions = []) {
  const normRoles = (Array.isArray(roles) ? roles : [roles])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))

  const normPerms = (Array.isArray(permissions) ? permissions : [permissions])
    .map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))

  if (normPerms.includes('PATIENT_MERGE')) return true

  const isReceptionist = normRoles.includes('receptionist')
  const isManager = normRoles.includes('manager') || normRoles.includes('clinic_manager')
  const isAdmin = normRoles.includes('admin') || normRoles.includes('administrator')

  return isReceptionist || isManager || isAdmin
}

/**
 * Kiểm tra tính hợp lệ trước khi thực hiện gộp 2 hồ sơ bệnh nhân (TC-01, TC-03)
 * @param {Object} sourcePatient - Hồ sơ bị gộp (phụ, sẽ chuyển sang trạng thái MERGED)
 * @param {Object} targetPatient - Hồ sơ giữ lại (chính, nhận toàn bộ dữ liệu)
 * @param {string} [reason] - Lý do gộp hồ sơ
 * @returns {{ allowed: boolean, message: string | null }}
 */
export function validatePatientMerge(sourcePatient, targetPatient, reason = '') {
  if (!sourcePatient || !targetPatient) {
    return {
      allowed: false,
      message: 'Vui lòng chọn đầy đủ hồ sơ giữ lại và hồ sơ bị gộp.',
    }
  }

  const sourceId = String(sourcePatient.id || sourcePatient.patientId || '')
  const targetId = String(targetPatient.id || targetPatient.patientId || '')

  if (!sourceId || !targetId) {
    return {
      allowed: false,
      message: 'Không xác định được mã định danh của hồ sơ bệnh nhân.',
    }
  }

  // 1. Không thể gộp hồ sơ vào chính nó (CANNOT_MERGE_SAME_PATIENT)
  if (sourceId === targetId) {
    return {
      allowed: false,
      message: 'Không thể gộp một hồ sơ vào chính nó. Vui lòng chọn 2 hồ sơ khác nhau.',
    }
  }

  // 2. Kiểm tra trạng thái đã gộp (PATIENT_ALREADY_MERGED - TC-03)
  const isSourceMerged = Boolean(sourcePatient.isMerged || sourcePatient.status === 'MERGED' || sourcePatient.mergedIntoPatientId)
  if (isSourceMerged) {
    return {
      allowed: false,
      message: `Hồ sơ nguồn [${sourcePatient.patientCode || 'Nguồn'}] đã ở trạng thái đã gộp trước đó. Không thể gộp tiếp.`,
    }
  }

  const isTargetMerged = Boolean(targetPatient.isMerged || targetPatient.status === 'MERGED' || targetPatient.mergedIntoPatientId)
  if (isTargetMerged) {
    return {
      allowed: false,
      message: `Hồ sơ đích [${targetPatient.patientCode || 'Đích'}] đã ở trạng thái đã gộp. Vui lòng chọn hồ sơ chính đang hoạt động.`,
    }
  }

  // 3. Kiểm tra độ dài lý do
  const trimmedReason = String(reason || '').trim()
  if (trimmedReason.length > 500) {
    return {
      allowed: false,
      message: 'Lý do gộp hồ sơ không được vượt quá 500 ký tự.',
    }
  }

  return {
    allowed: true,
    message: null,
  }
}

/**
 * Xử lý thông báo lỗi phản hồi từ API gộp hồ sơ sang tiếng Việt thân thiện
 * @param {any} error 
 * @param {string} defaultMessage 
 * @returns {string}
 */
export function cleanMergeErrorMessage(error, defaultMessage = 'Không thể thực hiện gộp hồ sơ bệnh nhân.') {
  if (!error) return defaultMessage
  const responseData = error?.response?.data || {}
  const errorCode = responseData.code || responseData.error || ''
  const responseMsg = responseData.message || error?.message || ''

  if (errorCode === 'CANNOT_MERGE_SAME_PATIENT') {
    return 'Không thể gộp hồ sơ vào chính nó.'
  }
  if (errorCode === 'PATIENT_ALREADY_MERGED') {
    return 'Một trong hai hồ sơ đã ở trạng thái đã gộp trước đó.'
  }
  if (errorCode === 'PATIENT_IDENTITY_CONFLICT') {
    return 'Xung đột định danh: Hai hồ sơ có thông tin định danh (CCCD hoặc thông tin nhân khẩu) mâu thuẫn đã được ký khóa bệnh án.'
  }
  if (error?.response?.status === 403 || errorCode === 'ACCESS_DENIED') {
    return 'Bạn không có quyền thực hiện thao tác gộp hồ sơ bệnh nhân (Yêu cầu quyền Lễ tân hoặc Quản lý).'
  }
  if (error?.response?.status === 404 || errorCode === 'PATIENT_NOT_FOUND') {
    return 'Không tìm thấy một trong hai hồ sơ bệnh nhân trong hệ thống.'
  }

  return responseMsg || defaultMessage
}
