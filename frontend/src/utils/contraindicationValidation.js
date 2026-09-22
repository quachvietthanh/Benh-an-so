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
    label: 'Nghiêm trọng',
    color: '#dc2626',
    tagColor: 'red',
    bgColor: '#fff1f2',
    borderColor: '#fecdd3',
    rank: 3,
  },
  MODERATE: {
    label: 'Trung bình',
    color: '#d97706',
    tagColor: 'orange',
    bgColor: '#fffbeb',
    borderColor: '#fde68a',
    rank: 2,
  },
  LOW: {
    label: 'Nhẹ / Thận trọng',
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

/**
 * Danh mục mã bệnh ICD-10 chuẩn hệ thống đối chiếu UUID sang Mã và Tên bệnh tiếng Việt
 * Đảm bảo giao diện hiển thị đúng mã bệnh chuẩn (ví dụ: J45.9 - Hen phế quản, K25.9 - Loét dạ dày)
 */
export const DIAGNOSIS_CATALOG_FALLBACK = {
  'a1000000-0000-0000-0000-000000000001': { code: 'A09.0', name: 'Nhiễm trùng đường ruột' },
  'a1000000-0000-0000-0000-000000000002': { code: 'B34.9', name: 'Nhiễm virus không xác định' },
  'a1000000-0000-0000-0000-000000000003': { code: 'C04.9', name: 'U ác tính miệng' },
  'a1000000-0000-0000-0000-000000000004': { code: 'D18.0', name: 'U máu' },
  'a1000000-0000-0000-0000-000000000005': { code: 'E10.9', name: 'Đái tháo đường type 1' },
  'a1000000-0000-0000-0000-000000000006': { code: 'E11.9', name: 'Đái tháo đường type 2' },
  'a1000000-0000-0000-0000-000000000007': { code: 'E78.5', name: 'Mỡ máu cao' },
  'a1000000-0000-0000-0000-000000000008': { code: 'E66.9', name: 'Béo phì' },
  'a1000000-0000-0000-0000-000000000009': { code: 'F32.9', name: 'Trầm cảm' },
  'a1000000-0000-0000-0000-00000000000a': { code: 'F41.9', name: 'Lo âu' },
  'a1000000-0000-0000-0000-00000000000b': { code: 'F51.0', name: 'Mất ngủ' },
  'a1000000-0000-0000-0000-00000000000c': { code: 'G43.9', name: 'Đau nửa đầu' },
  'a1000000-0000-0000-0000-00000000000d': { code: 'G44.2', name: 'Đau đầu căng thẳng' },
  'a1000000-0000-0000-0000-00000000000e': { code: 'G47.9', name: 'Rối loạn giấc ngủ' },
  'a1000000-0000-0000-0000-00000000000f': { code: 'G56.0', name: 'Hội chứng ống cổ tay' },
  'a1000000-0000-0000-0000-000000000010': { code: 'H10.9', name: 'Viêm kết mạc' },
  'a1000000-0000-0000-0000-000000000011': { code: 'H52.1', name: 'Cận thị' },
  'a1000000-0000-0000-0000-000000000012': { code: 'H66.9', name: 'Viêm tai giữa' },
  'a1000000-0000-0000-0000-000000000013': { code: 'H91.9', name: 'Giảm thính lực' },
  'a1000000-0000-0000-0000-000000000014': { code: 'I10', name: 'Tăng huyết áp' },
  'a1000000-0000-0000-0000-000000000015': { code: 'I20.9', name: 'Đau thắt ngực' },
  'a1000000-0000-0000-0000-000000000016': { code: 'I25.9', name: 'Bệnh mạch vành' },
  'a1000000-0000-0000-0000-000000000017': { code: 'I48', name: 'Rung nhĩ' },
  'a1000000-0000-0000-0000-000000000018': { code: 'I84.9', name: 'Trĩ' },
  'a1000000-0000-0000-0000-000000000019': { code: 'J00', name: 'Cảm lạnh thông thường' },
  'a1000000-0000-0000-0000-00000000001a': { code: 'J01.9', name: 'Viêm xoang' },
  'a1000000-0000-0000-0000-00000000001b': { code: 'J02.9', name: 'Viêm họng cấp' },
  'a1000000-0000-0000-0000-00000000001c': { code: 'J03.9', name: 'Viêm amidan cấp' },
  'a1000000-0000-0000-0000-00000000001d': { code: 'J04.0', name: 'Viêm thanh quản cấp' },
  'a1000000-0000-0000-0000-00000000001e': { code: 'J06.9', name: 'Nhiễm trùng hô hấp trên' },
  'a1000000-0000-0000-0000-00000000001f': { code: 'J15.9', name: 'Viêm phổi' },
  'a1000000-0000-0000-0000-000000000020': { code: 'J20.9', name: 'Viêm phế quản cấp' },
  'a1000000-0000-0000-0000-000000000021': { code: 'J30.4', name: 'Viêm mũi dị ứng' },
  'a1000000-0000-0000-0000-000000000022': { code: 'J32.9', name: 'Viêm xoang mạn' },
  'a1000000-0000-0000-0000-000000000023': { code: 'J45.9', name: 'Hen phế quản' },
  'a1000000-0000-0000-0000-000000000024': { code: 'J47', name: 'Giãn phế quản' },
  'a1000000-0000-0000-0000-000000000025': { code: 'K21.9', name: 'Trào ngược dạ dày' },
  'a1000000-0000-0000-0000-000000000026': { code: 'K25.9', name: 'Loét dạ dày' },
  'a1000000-0000-0000-0000-000000000027': { code: 'K29.7', name: 'Viêm dạ dày' },
  'a1000000-0000-0000-0000-000000000028': { code: 'K30', name: 'Khó tiêu' },
  'a1000000-0000-0000-0000-000000000029': { code: 'K52.9', name: 'Viêm đại tràng' },
  'a1000000-0000-0000-0000-00000000002a': { code: 'K59.0', name: 'Táo bón' },
  'a1000000-0000-0000-0000-00000000002b': { code: 'K59.1', name: 'Tiêu chảy' },
  'a1000000-0000-0000-0000-00000000002c': { code: 'K80.2', name: 'Sỏi mật' },
  'a1000000-0000-0000-0000-00000000002d': { code: 'K92.1', name: 'Xuất huyết tiêu hóa' },
  'a1000000-0000-0000-0000-00000000002e': { code: 'L20.8', name: 'Chàm' },
  'a1000000-0000-0000-0000-00000000002f': { code: 'L23.9', name: 'Dị ứng da' },
  'a1000000-0000-0000-0000-000000000030': { code: 'L30.9', name: 'Viêm da' },
  'a1000000-0000-0000-0000-000000000031': { code: 'M06.9', name: 'Viêm khớp dạng thấp' },
  'a1000000-0000-0000-0000-000000000032': { code: 'M10.9', name: 'Gout' },
  'a1000000-0000-0000-0000-000000000033': { code: 'M17.9', name: 'Thoái hóa khớp gối' },
  'a1000000-0000-0000-0000-000000000034': { code: 'M19.9', name: 'Thoái hóa khớp' },
  'a1000000-0000-0000-0000-000000000035': { code: 'M47.9', name: 'Thoái hóa cột sống' },
  'a1000000-0000-0000-0000-000000000036': { code: 'M51.1', name: 'Thoát vị đĩa đệm' },
  'a1000000-0000-0000-0000-000000000037': { code: 'M54.5', name: 'Đau thắt lưng' },
  'a1000000-0000-0000-0000-000000000038': { code: 'M54.1', name: 'Đau cổ vai gáy' },
  'a1000000-0000-0000-0000-000000000039': { code: 'M79.2', name: 'Đau cơ' },
  'a1000000-0000-0000-0000-00000000003a': { code: 'M79.7', name: 'Đau nhức chân' },
  'a1000000-0000-0000-0000-00000000003b': { code: 'N30.9', name: 'Viêm bàng quang' },
  'a1000000-0000-0000-0000-00000000003c': { code: 'N39.0', name: 'Nhiễm trùng tiểu' },
  'a1000000-0000-0000-0000-00000000003d': { code: 'N40', name: 'Phì đại tiền liệt tuyến' },
  'a1000000-0000-0000-0000-00000000003e': { code: 'N95.1', name: 'Mãn kinh' },
  'a1000000-0000-0000-0000-00000000003f': { code: 'R05', name: 'Ho' },
  'a1000000-0000-0000-0000-000000000040': { code: 'R06.0', name: 'Khó thở' },
  'a1000000-0000-0000-0000-000000000041': { code: 'R10.4', name: 'Đau bụng' },
  'a1000000-0000-0000-0000-000000000042': { code: 'R11', name: 'Buồn nôn' },
  'a1000000-0000-0000-0000-000000000043': { code: 'R42', name: 'Chóng mặt' },
  'a1000000-0000-0000-0000-000000000044': { code: 'R50.9', name: 'Sốt' },
  'a1000000-0000-0000-0000-000000000045': { code: 'R51', name: 'Đau đầu' },
  'a1000000-0000-0000-0000-000000000046': { code: 'R52.9', name: 'Đau' },
  'a1000000-0000-0000-0000-000000000047': { code: 'R53', name: 'Mệt mỏi' },
  'a1000000-0000-0000-0000-000000000048': { code: 'R55', name: 'Ngất' },
  'a1000000-0000-0000-0000-00000000004a': { code: 'S06.0', name: 'Chấn động não' },
  'a1000000-0000-0000-0000-00000000004b': { code: 'S93.4', name: 'Bong gân cổ chân' },
  'a1000000-0000-0000-0000-00000000004c': { code: 'T14.2', name: 'Gãy xương' },
  'a1000000-0000-0000-0000-00000000004d': { code: 'Z00.0', name: 'Khám sức khỏe tổng quát' },
  'a1000000-0000-0000-0000-00000000004e': { code: 'Z01.0', name: 'Khám mắt' },
  'a1000000-0000-0000-0000-00000000004f': { code: 'Z23', name: 'Tiêm chủng' },
  'a1000000-0000-0000-0000-000000000050': { code: 'Z30.0', name: 'Kế hoạch hóa gia đình' },
}

/**
 * Lấy đối tượng thông tin chuẩn hóa mã bệnh và tên bệnh (ICD-10)
 * @param {object} record Đối tượng quy tắc hoặc bản ghi lâm sàng
 * @param {object} diagnosisMap Bộ map động id -> { code, name }
 * @returns {{ code: string, name: string, displayText: string }}
 */
export function getDiagnosisInfo(record, diagnosisMap = {}) {
  if (!record) return { code: '', name: '', displayText: '—' }
  const catalogId = record.diagnosisCatalogId || record.id
  const catalogEntry =
    (catalogId && diagnosisMap[catalogId]) ||
    (catalogId && DIAGNOSIS_CATALOG_FALLBACK[catalogId]) ||
    null

  const code = record.diagnosisCode || catalogEntry?.code || record.code || ''
  const name = record.diagnosisName || catalogEntry?.name || catalogEntry?.diseaseName || record.name || ''

  let displayText = ''
  if (code && name) {
    displayText = `${code} - ${name}`
  } else if (name) {
    displayText = name
  } else if (code) {
    displayText = code
  } else {
    displayText = 'Bệnh nền mạn tính'
  }

  return { code, name, displayText }
}
