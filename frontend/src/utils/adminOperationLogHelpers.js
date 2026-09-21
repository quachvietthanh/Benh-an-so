import dayjs from 'dayjs'

export const RESOURCE_TYPE_LABELS = {
  USER: 'Tài khoản nhân viên',
  ROLE: 'Vai trò & Phân quyền',
  MEDICINE: 'Danh mục thuốc',
  SERVICE_CATALOG: 'Danh mục dịch vụ',
  SERVICE_PRICE: 'Bảng giá dịch vụ',
  DIAGNOSIS_CATALOG: 'Danh mục mã bệnh (ICD-10)',
  SECURITY_ALERT: 'Cảnh báo an toàn bảo mật',
  CONFIGURATION: 'Cấu hình hệ thống & phòng khám',
  SYSTEM_BACKUP: 'Sao lưu & Phục hồi dữ liệu',
  MEDICAL_RECORD_TEMPLATE: 'Mẫu bệnh án chuyên khoa',
  CLINICAL_SERVICE: 'Danh mục cận lâm sàng & ngưỡng',
  ROOM: 'Quản lý phòng & phân phòng',
  DOCTOR_SCHEDULE: 'Lịch làm việc bác sĩ',
  DOCTOR_TIMEOFF: 'Lịch nghỉ của bác sĩ',
}

export const RESOURCE_TYPE_OPTIONS = [
  { value: 'USER', label: 'Tài khoản nhân viên' },
  { value: 'ROLE', label: 'Vai trò & Phân quyền' },
  { value: 'MEDICINE', label: 'Danh mục thuốc' },
  { value: 'SERVICE_CATALOG', label: 'Danh mục dịch vụ' },
  { value: 'SERVICE_PRICE', label: 'Bảng giá dịch vụ' },
  { value: 'DIAGNOSIS_CATALOG', label: 'Danh mục mã bệnh (ICD-10)' },
  { value: 'SECURITY_ALERT', label: 'Cảnh báo an toàn bảo mật' },
  { value: 'CONFIGURATION', label: 'Cấu hình hệ thống & phòng khám' },
  { value: 'SYSTEM_BACKUP', label: 'Sao lưu & Phục hồi dữ liệu' },
  { value: 'MEDICAL_RECORD_TEMPLATE', label: 'Mẫu bệnh án chuyên khoa' },
  { value: 'CLINICAL_SERVICE', label: 'Danh mục cận lâm sàng & ngưỡng' },
  { value: 'ROOM', label: 'Quản lý phòng & phân phòng' },
  { value: 'DOCTOR_SCHEDULE', label: 'Lịch làm việc bác sĩ' },
  { value: 'DOCTOR_TIMEOFF', label: 'Lịch nghỉ của bác sĩ' },
]

export const RESOURCE_TYPE_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả đối tượng' },
  ...RESOURCE_TYPE_OPTIONS,
]

export const ACTION_TYPE_CONFIG = {
  CREATE: { label: 'Tạo mới', color: 'green' },
  UPDATE: { label: 'Cập nhật', color: 'blue' },
  ACTIVATE: { label: 'Kích hoạt', color: 'cyan' },
  DEACTIVATE: { label: 'Vô hiệu hóa', color: 'orange' },
  UNLOCK: { label: 'Mở khóa', color: 'purple' },
  BACKUP: { label: 'Sao lưu', color: 'cyan' },
  RESTORE: { label: 'Phục hồi', color: 'magenta' },
  EXPORT: { label: 'Tải về / Xuất', color: 'geekblue' },
  CANCEL: { label: 'Hủy bỏ', color: 'red' },
  DELETE: { label: 'Xóa', color: 'red' },
  RESET_PASSWORD: { label: 'Đặt lại mật khẩu', color: 'volcano' },
}

export const FIELD_LABEL_DICTIONARY = {
  id: 'Mã định danh',
  username: 'Tên đăng nhập',
  fullName: 'Họ và tên',
  email: 'Email',
  phone: 'Số điện thoại',
  role: 'Vai trò',
  roles: 'Danh sách vai trò',
  active: 'Trạng thái hoạt động',
  status: 'Trạng thái',
  permissions: 'Danh sách quyền',
  permissionCodes: 'Mã các quyền',
  medicineCode: 'Mã thuốc',
  medicineName: 'Tên thuốc',
  activeIngredient: 'Hoạt chất',
  strength: 'Hàm lượng',
  unit: 'Đơn vị tính',
  dosage: 'Liều lượng',
  minStockThreshold: 'Ngưỡng tồn tối thiểu',
  serviceCode: 'Mã dịch vụ',
  serviceName: 'Tên dịch vụ',
  description: 'Mô tả',
  price: 'Đơn giá (VNĐ)',
  effectiveFrom: 'Ngày hiệu lực',
  effectiveTo: 'Ngày hết hiệu lực',
  code: 'Mã',
  name: 'Tên',
  abbreviation: 'Tên viết tắt',
  diseaseGroup: 'Nhóm bệnh',
  alertType: 'Loại cảnh báo',
  severity: 'Mức độ nghiêm trọng',
  backupCode: 'Mã bản sao lưu',
  fileName: 'Tên tệp tin',
  fileSize: 'Kích thước tệp (bytes)',
  backupType: 'Loại sao lưu',
  restoredAt: 'Thời điểm phục hồi',
  clinicName: 'Tên phòng khám',
  address: 'Địa chỉ',
  openingTime: 'Giờ mở cửa',
  closingTime: 'Giờ đóng cửa',
  retentionYears: 'Thời hạn lưu trữ hồ sơ (năm)',
  signingDeadlineHours: 'Hạn ký bệnh án (giờ)',
  enabled: 'Bật chế độ ẩn danh',
  templateName: 'Tên mẫu bệnh án',
  templateId: 'Mã mẫu bệnh án',
  version: 'Phiên bản',
  serviceType: 'Loại dịch vụ CĐLS',
  resultDataType: 'Kiểu dữ liệu kết quả',
  roomId: 'Mã phòng',
  doctorId: 'Mã bác sĩ',
  dayOfWeek: 'Ngày trong tuần',
  startTime: 'Giờ bắt đầu',
  endTime: 'Giờ kết thúc',
  reason: 'Lý do',
  summary: 'Tóm tắt thay đổi',
  event: 'Sự kiện ghi nhận',
}

/**
 * An toàn parse chuỗi JSON của field detail.
 * Trả về { before: {}, after: {}, isValid: boolean }
 * Tuyệt đối không throw exception làm crash ứng dụng.
 */
export const safeParseDetail = (detailString) => {
  if (!detailString) {
    return { before: {}, after: {}, isValid: false }
  }

  const normalizeObj = (parsed) => {
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      return { before: {}, after: {}, isValid: false }
    }
    if ('before' in parsed || 'after' in parsed) {
      return {
        before:
          parsed.before && typeof parsed.before === 'object' && !Array.isArray(parsed.before)
            ? parsed.before
            : {},
        after:
          parsed.after && typeof parsed.after === 'object' && !Array.isArray(parsed.after)
            ? parsed.after
            : {},
        isValid: true,
      }
    }
    return {
      before: {},
      after: parsed,
      isValid: true,
    }
  }

  if (typeof detailString === 'object' && detailString !== null && !Array.isArray(detailString)) {
    return normalizeObj(detailString)
  }

  try {
    const parsed = JSON.parse(detailString)
    return normalizeObj(parsed)
  } catch {
    return { before: {}, after: {}, isValid: false }
  }
}

/**
 * Map loại đối tượng sang nhãn tiếng Việt
 */
export const formatResourceType = (type) => {
  return RESOURCE_TYPE_LABELS[type] || type || '—'
}

/**
 * Map hành động sang nhãn và cấu hình màu Tag
 */
export const formatActionType = (type) => {
  return ACTION_TYPE_CONFIG[type] || { label: type || 'Khác', color: 'default' }
}

/**
 * Kiểm tra khoảng thời gian lọc trước khi gửi lên API
 */
export const validateDateRange = (from, to) => {
  if (!from || !to) return { isValid: true, error: null }
  const fromTime = new Date(from).getTime()
  const toTime = new Date(to).getTime()

  if (isNaN(fromTime) || isNaN(toTime)) {
    return { isValid: false, error: 'Mốc thời gian không hợp lệ.' }
  }

  if (fromTime > toTime) {
    return {
      isValid: false,
      error: 'Khoảng thời gian lọc không hợp lệ. "Từ ngày" phải trước hoặc bằng "Đến ngày".',
    }
  }

  return { isValid: true, error: null }
}

/**
 * So sánh 2 object before/after để tạo danh sách các dòng so sánh
 */
export const buildDiffRows = (before, after) => {
  const safeBefore = before && typeof before === 'object' && !Array.isArray(before) ? before : {}
  const safeAfter = after && typeof after === 'object' && !Array.isArray(after) ? after : {}

  const allKeys = Array.from(new Set([...Object.keys(safeBefore), ...Object.keys(safeAfter)]))

  return allKeys.map((key) => {
    const oldVal = safeBefore[key]
    const newVal = safeAfter[key]

    const isOldDefined = key in safeBefore
    const isNewDefined = key in safeAfter
    const changed = !isOldDefined || !isNewDefined || JSON.stringify(oldVal) !== JSON.stringify(newVal)

    return {
      field: key,
      label: FIELD_LABEL_DICTIONARY[key] || key,
      oldValue: oldVal,
      newValue: newVal,
      changed,
    }
  })
}

/**
 * Định dạng hiển thị giá trị trường dữ liệu
 */
export const formatFieldValue = (val) => {
  if (val === null || val === undefined) return '—'
  if (typeof val === 'boolean') return val ? 'Có / Đang hoạt động' : 'Không / Vô hiệu hóa'
  if (Array.isArray(val)) {
    return val.length === 0 ? '—' : val.join(', ')
  }
  if (typeof val === 'object') {
    try {
      return JSON.stringify(val)
    } catch {
      return String(val)
    }
  }
  if (typeof val === 'number') {
    return val.toLocaleString('vi-VN')
  }
  return String(val)
}

/**
 * Định dạng ngày giờ theo múi giờ Việt Nam Asia/Ho_Chi_Minh
 */
export const formatVietnamDateTime = (isoString) => {
  if (!isoString) return '—'
  const date = new Date(isoString)
  if (isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}

/**
 * Chuyển đổi RangePicker dates sang chuỗi ISO-8601 UTC bảo toàn múi giờ Việt Nam
 */
export const convertDateRangeToIso = (dates) => {
  if (!dates || !dates[0] || !dates[1]) {
    return { from: undefined, to: undefined }
  }

  const fromDay = dayjs(dates[0]).format('YYYY-MM-DD')
  const toDay = dayjs(dates[1]).format('YYYY-MM-DD')

  const fromIso = new Date(`${fromDay}T00:00:00+07:00`).toISOString()
  const toIso = new Date(`${toDay}T23:59:59.999+07:00`).toISOString()

  return { from: fromIso, to: toIso }
}

/**
 * Kiểm tra quyền xem nhật ký thao tác quản trị
 */
export const canViewAdminOperationLogs = (roles = [], permissions = []) => {
  const normalizedRoles = (roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const normalizedPermissions = (permissions || []).map((p) =>
    String(p || '').toUpperCase().replace(/^PERMISSION_/, '')
  )

  const isAdmin = normalizedRoles.includes('admin')
  const isManager = normalizedRoles.includes('manager') || normalizedRoles.includes('clinic_manager')
  const hasPerm = normalizedPermissions.includes('ADMIN_OPERATION_LOG_READ')

  const isDoctor = normalizedRoles.includes('doctor') && !isAdmin && !isManager
  const isReceptionist = normalizedRoles.includes('receptionist') && !isAdmin && !isManager
  const isPharmacist = normalizedRoles.includes('pharmacist') && !isAdmin && !isManager

  if (isDoctor || isReceptionist || isPharmacist) return false

  return isAdmin || isManager || hasPerm
}
