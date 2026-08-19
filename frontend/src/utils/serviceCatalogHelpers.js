import dayjs from 'dayjs'

/**
 * Format số tiền VND sang chuỗi hiển thị
 */
export const formatVND = (value) => {
  const num = Number(value || 0)
  return `${num.toLocaleString('vi-VN')} ₫`
}

/**
 * Kiểm tra quyền quản lý danh mục dịch vụ và bảng giá (Admin và Manager)
 */
export const checkServiceManagementPermission = (roles) => {
  if (!roles) return false
  const roleList = (Array.isArray(roles) ? roles : [roles])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)
  return roleList.includes('admin') || roleList.includes('manager')
}

/**
 * Chuẩn hóa dữ liệu trả về từ API (hỗ trợ cả Spring Data Page { content: [...] } lẫn mảng thuần)
 */
export const normalizeServiceList = (data) => {
  if (!data) return []
  if (Array.isArray(data)) return data
  if (Array.isArray(data.content)) return data.content
  if (Array.isArray(data.items)) return data.items
  return []
}

/**
 * Validate dữ liệu tạo dịch vụ mới
 */
export const validateCreateServicePayload = (values) => {
  const errors = {}
  const serviceCode = String(values?.serviceCode || '').trim()
  const name = String(values?.name || '').trim()
  const price = values?.price
  const effectiveFrom = values?.effectiveFrom

  if (!serviceCode) {
    errors.serviceCode = 'Vui lòng nhập mã dịch vụ'
  } else if (serviceCode.length > 50) {
    errors.serviceCode = 'Mã dịch vụ không được vượt quá 50 ký tự'
  }

  if (!name) {
    errors.name = 'Vui lòng nhập tên dịch vụ'
  } else if (name.length > 255) {
    errors.name = 'Tên dịch vụ không được vượt quá 255 ký tự'
  }

  if (price === undefined || price === null || price === '') {
    errors.price = 'Vui lòng nhập đơn giá'
  } else if (Number(price) < 0 || isNaN(Number(price))) {
    errors.price = 'Đơn giá phải lớn hơn hoặc bằng 0'
  }

  if (!effectiveFrom) {
    errors.effectiveFrom = 'Vui lòng chọn ngày bắt đầu hiệu lực'
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
    payload: {
      serviceCode,
      name,
      price: Number(price || 0),
      effectiveFrom: effectiveFrom ? (dayjs.isDayjs(effectiveFrom) ? effectiveFrom.format('YYYY-MM-DD') : String(effectiveFrom)) : '',
    },
  }
}

/**
 * Validate dữ liệu cập nhật dịch vụ
 */
export const validateUpdateServicePayload = (values) => {
  const errors = {}
  const name = String(values?.name || '').trim()
  const price = values?.price
  const effectiveFrom = values?.effectiveFrom
  const active = values?.active !== undefined ? Boolean(values?.active) : true

  if (!name) {
    errors.name = 'Vui lòng nhập tên dịch vụ'
  } else if (name.length > 255) {
    errors.name = 'Tên dịch vụ không được vượt quá 255 ký tự'
  }

  if (price === undefined || price === null || price === '') {
    errors.price = 'Vui lòng nhập đơn giá'
  } else if (Number(price) < 0 || isNaN(Number(price))) {
    errors.price = 'Đơn giá phải lớn hơn hoặc bằng 0'
  }

  if (!effectiveFrom) {
    errors.effectiveFrom = 'Vui lòng chọn ngày bắt đầu hiệu lực'
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
    payload: {
      name,
      active,
      price: Number(price || 0),
      effectiveFrom: effectiveFrom ? (dayjs.isDayjs(effectiveFrom) ? effectiveFrom.format('YYYY-MM-DD') : String(effectiveFrom)) : '',
    },
  }
}

/**
 * Phân loại các mốc giá trong lịch sử theo thời điểm hiện tại:
 * - CURRENT: Giá đang áp dụng hiện hành (mốc có effectiveFrom gần nhất <= today)
 * - UPCOMING: Giá có hiệu lực trong tương lai (effectiveFrom > today)
 * - PAST: Giá đã qua trong lịch sử
 */
export const categorizePriceHistory = (prices = [], referenceDate = dayjs()) => {
  if (!Array.isArray(prices) || prices.length === 0) return []

  const ref = dayjs(referenceDate).startOf('day')

  // Sắp xếp giảm dần theo effectiveFrom (mới nhất lên đầu)
  const sorted = [...prices].sort((a, b) => {
    return dayjs(b.effectiveFrom).diff(dayjs(a.effectiveFrom))
  })

  // Tìm index của mốc giá hiện hành (mốc đầu tiên có effectiveFrom <= ref)
  const currentIdx = sorted.findIndex((item) => {
    const itemDate = dayjs(item.effectiveFrom).startOf('day')
    return itemDate.isSame(ref) || itemDate.isBefore(ref)
  })

  return sorted.map((item, index) => {
    const itemDate = dayjs(item.effectiveFrom).startOf('day')
    let status = 'PAST'
    let statusLabel = 'Lịch sử'
    let badgeColor = 'default'

    if (itemDate.isAfter(ref)) {
      status = 'UPCOMING'
      statusLabel = 'Sắp áp dụng'
      badgeColor = 'warning'
    } else if (index === currentIdx) {
      status = 'CURRENT'
      statusLabel = 'Đang áp dụng'
      badgeColor = 'success'
    }

    return {
      ...item,
      status,
      statusLabel,
      badgeColor,
    }
  })
}

/**
 * Tính toán thống kê nhanh danh mục
 */
export const calculateServiceStats = (services = []) => {
  const total = services.length
  const activeCount = services.filter((s) => Boolean(s.active)).length
  const inactiveCount = total - activeCount
  const activeServices = services.filter((s) => Boolean(s.active))
  const avgPrice =
    activeServices.length > 0
      ? Math.round(
          activeServices.reduce((sum, s) => sum + Number(s.price || 0), 0) /
            activeServices.length
        )
      : 0

  return {
    total,
    activeCount,
    inactiveCount,
    avgPrice,
  }
}
