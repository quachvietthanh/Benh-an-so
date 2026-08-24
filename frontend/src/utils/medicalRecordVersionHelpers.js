import dayjs from 'dayjs'

/**
 * Checks if the current user has permission to view the medical record version history.
 * Authorized roles: ADMIN, MANAGER, CLINIC_MANAGER
 * Authorized permissions: MEDICAL_RECORD_VERSION_HISTORY_READ, AUDIT_READ
 *
 * @param {Array<string>} roles
 * @param {Array<string>} permissions
 * @returns {boolean}
 */
export const canViewMedicalRecordVersionHistory = (roles = [], permissions = []) => {
  const normalizedRoles = (Array.isArray(roles) ? roles : [roles])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)

  const normalizedPerms = (Array.isArray(permissions) ? permissions : [permissions])
    .map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
    .filter(Boolean)

  if (
    normalizedRoles.includes('admin') ||
    normalizedRoles.includes('manager') ||
    normalizedRoles.includes('clinic_manager')
  ) {
    return true
  }

  if (
    normalizedPerms.includes('MEDICAL_RECORD_VERSION_HISTORY_READ') ||
    normalizedPerms.includes('AUDIT_READ')
  ) {
    return true
  }

  return false
}

/**
 * Normalizes and formats the raw backend version history response into a display-friendly structure.
 *
 * @param {Object} rawData - Response from GET /medical-records/{id}/versions
 * @returns {Object} Normalized version history data
 */
export const normalizeVersionHistoryData = (rawData) => {
  if (!rawData) {
    return {
      originalOnly: true,
      originalVersion: null,
      amendments: [],
      allVersions: [],
      totalVersions: 0,
      hasAmendments: false,
    }
  }

  const originalOnly = Boolean(rawData.originalOnly)
  const original = rawData.originalVersion
    ? {
        versionNumber: rawData.originalVersion.versionNumber || 1,
        isOriginal: true,
        modifiedBy: rawData.originalVersion.modifiedBy || 'Bác sĩ phụ trách',
        modifiedAt: rawData.originalVersion.modifiedAt,
        formattedModifiedAt: rawData.originalVersion.modifiedAt
          ? dayjs(rawData.originalVersion.modifiedAt).format('HH:mm:ss DD/MM/YYYY')
          : '---',
        reason: rawData.originalVersion.reason || 'Khởi tạo bệnh án ban đầu',
        content: rawData.originalVersion.content || null,
        snapshot: rawData.originalVersion.snapshot || null,
      }
    : null

  const amendments = (Array.isArray(rawData.amendments) ? rawData.amendments : []).map((item, index) => ({
    versionNumber: item.versionNumber || index + 2,
    isOriginal: false,
    modifiedBy: item.modifiedBy || 'Quản lý / Bác sĩ',
    modifiedAt: item.modifiedAt,
    formattedModifiedAt: item.modifiedAt
      ? dayjs(item.modifiedAt).format('HH:mm:ss DD/MM/YYYY')
      : '---',
    reason: item.reason || 'Đính chính / Bổ sung thông tin',
    content: item.content || 'Không có ghi chú nội dung',
    snapshot: item.snapshot || null,
  }))

  const allVersions = original ? [original, ...amendments] : [...amendments]

  return {
    originalOnly: originalOnly && amendments.length === 0,
    originalVersion: original,
    amendments,
    allVersions,
    totalVersions: allVersions.length,
    hasAmendments: amendments.length > 0,
  }
}

/**
 * Validates whether a record has sufficient information for version history query.
 *
 * @param {Object|string} recordOrId
 * @returns {{ valid: boolean, recordId: string|null, error: string|null }}
 */
export const validateVersionHistoryQuery = (recordOrId) => {
  if (!recordOrId) {
    return { valid: false, recordId: null, error: 'Mã hồ sơ bệnh án không hợp lệ hoặc để trống.' }
  }

  const recordId = typeof recordOrId === 'string'
    ? recordOrId
    : (recordOrId.medicalRecordId || recordOrId.id)

  if (!recordId) {
    return { valid: false, recordId: null, error: 'Không tìm thấy ID của hồ sơ bệnh án.' }
  }

  return { valid: true, recordId: String(recordId), error: null }
}
