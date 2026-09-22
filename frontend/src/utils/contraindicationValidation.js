/**
 * Cấu hình và hàm hỗ trợ kiểm tra, xác thực cảnh báo chống chỉ định (NCL-05-CN-006)
 */

export const CONTRAINDICATION_SEVERITY_META = {
  CONTRAINDICATED: {
    label: 'Chống chỉ định tuyệt đối',
    color: '#991b1b',
    tagColor: '#7f1d1d',
    bgColor: '#fef2f2',
    borderColor: '#fca5a5',
    rank: 4,
  },
  SEVERE: {
    label: 'Nghiêm trọng (Severe)',
    color: '#dc2626',
    tagColor: 'red',
    bgColor: '#fff1f2',
    borderColor: '#fecdd3',
    rank: 3,
  },
  MODERATE: {
    label: 'Trung bình (Moderate)',
    color: '#d97706',
    tagColor: 'orange',
    bgColor: '#fffbeb',
    borderColor: '#fde68a',
    rank: 2,
  },
  LOW: {
    label: 'Nhẹ / Thận trọng (Low)',
    color: '#0284c7',
    tagColor: 'blue',
    bgColor: '#f0f9ff',
    borderColor: '#bae6fd',
    rank: 1,
  },
}

export const CONTRAINDICATION_TYPE_META = {
  AGE: {
    label: 'Độ tuổi',
    tagColor: 'cyan',
    iconName: 'UserOutlined',
  },
  PREGNANCY: {
    label: 'Thai kỳ',
    tagColor: 'magenta',
    iconName: 'HeartOutlined',
  },
  DISEASE: {
    label: 'Bệnh nền / Bệnh mạn tính',
    tagColor: 'purple',
    iconName: 'MedicineBoxOutlined',
  },
}

export const PRESET_CONTRAINDICATION_OVERRIDE_REASONS = [
  'Đã hội chẩn chuyên khoa, xác định lợi ích điều trị vượt trội nguy cơ lâm sàng.',
  'Trường hợp cấp cứu / bệnh tiến triển nặng, không có thuốc thay thế phù hợp trong danh mục.',
  'Liều dùng đã được bác sĩ hiệu chỉnh an toàn theo thể trạng và chức năng cơ quan của bệnh nhân.',
  'Đã theo dõi sát dấu hiệu sinh tồn, xét nghiệm chức năng tạng và tư vấn kỹ cho người bệnh/thân nhân.',
]

/**
 * Kiểm tra tính hợp lệ của lý do bỏ qua cảnh báo chống chỉ định
 * @param {string} reason
 * @returns {{ valid: boolean, error?: string, trimmedReason: string }}
 */
export function validateContraindicationOverrideReason(reason) {
  if (!reason || typeof reason !== 'string') {
    return {
      valid: false,
      error: 'Bác sĩ bắt buộc phải nhập lý do chuyên môn bỏ qua cảnh báo.',
      trimmedReason: '',
    }
  }

  const trimmed = reason.trim()
  if (trimmed.length === 0) {
    return {
      valid: false,
      error: 'Lý do bỏ qua cảnh báo không được để trống hoặc chỉ chứa khoảng trắng.',
      trimmedReason: '',
    }
  }

  if (trimmed.length < 5) {
    return {
      valid: false,
      error: 'Lý do bỏ qua cảnh báo quá ngắn (tối thiểu 5 ký tự) để phục vụ kiểm toán lâm sàng.',
      trimmedReason: trimmed,
    }
  }

  return {
    valid: true,
    trimmedReason: trimmed,
  }
}

/**
 * Kiểm tra xem 1 cảnh báo cụ thể đã có lý do ghi đè hợp lệ hay chưa
 * @param {object} warning
 * @param {Array} overrides Mảng [{ ruleId, medicineId, overrideReason }]
 */
export function isContraindicationHandled(warning, overrides = []) {
  if (!warning || !warning.ruleId || !warning.medicineId) return false
  const targetRuleId = String(warning.ruleId)
  const targetMedicineId = String(warning.medicineId)

  return (overrides || []).some(
    (o) =>
      String(o.ruleId) === targetRuleId &&
      String(o.medicineId) === targetMedicineId &&
      typeof o.overrideReason === 'string' &&
      o.overrideReason.trim().length > 0,
  )
}

/**
 * Kiểm tra xem tất cả các cảnh báo chống chỉ định đã được nhập lý do hay chưa
 * @param {Array} warnings
 * @param {Array} overrides
 */
export function areAllContraindicationsHandled(warnings = [], overrides = []) {
  if (!warnings || warnings.length === 0) return true
  return warnings.every((w) => isContraindicationHandled(w, overrides))
}

/**
 * Lấy danh sách các cảnh báo chưa được xử lý
 * @param {Array} warnings
 * @param {Array} overrides
 */
export function getUnhandledContraindications(warnings = [], overrides = []) {
  if (!warnings || warnings.length === 0) return []
  return warnings.filter((w) => !isContraindicationHandled(w, overrides))
}

/**
 * Chuẩn hóa mảng contraindicationOverrides gửi backend
 * @param {Array} rawOverrides
 */
export function sanitizeContraindicationOverrides(rawOverrides = []) {
  return (rawOverrides || [])
    .filter(
      (o) =>
        o &&
        o.ruleId &&
        o.medicineId &&
        typeof o.overrideReason === 'string' &&
        o.overrideReason.trim().length > 0,
    )
    .map((o) => ({
      ruleId: o.ruleId,
      medicineId: o.medicineId,
      overrideReason: o.overrideReason.trim(),
    }))
}
