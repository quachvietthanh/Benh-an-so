/**
 * Tiện ích và hàm hỗ trợ nghiệp vụ Quản lý Danh mục Dịch vụ và Bảng giá
 */

/**
 * Kiểm tra quyền quản lý dịch vụ và bảng giá
 * Cho phép ADMIN và MANAGER
 */
export function checkServiceManagePermission(roles) {
  const rList = (Array.isArray(roles) ? roles : [roles])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)
  return rList.includes('admin') || rList.includes('manager')
}

/**
 * Khắc phục lỗi encoding tiếng Việt (Mojibake UTF-8 bị đọc nhầm theo ISO-8859-1/Windows-1252)
 */
export function fixVietnameseEncoding(str) {
  if (!str || typeof str !== 'string') return str
  // Kiểm tra các ký tự dấu hiệu của mojibake như Ä, Å, Æ, Ã...
  if (!/[ÃÄÅÆÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞß]/.test(str)) {
    return str
  }

  try {
    const bytes = new Uint8Array([...str].map((c) => c.charCodeAt(0) & 0xff))
    const decoded = new TextDecoder('utf-8', { fatal: false }).decode(bytes)
    if (decoded && !decoded.includes('\ufffd') && decoded !== str) {
      return decoded
    }
  } catch {
    // fallback nếu có lỗi giải mã
  }

  return str
}

/**
 * Chuẩn hóa một bản ghi dịch vụ, giải mã tiếng Việt chuẩn xác
 */
export function normalizeServiceItem(service) {
  if (!service || typeof service !== 'object') return service
  return {
    ...service,
    name: fixVietnameseEncoding(service.name || ''),
    serviceCode: service.serviceCode || '',
  }
}

/**
 * Định dạng tiền tệ theo chuẩn Việt Nam (VND)
 */
export function formatMoney(value) {
  const num = Number(value || 0)
  if (Number.isNaN(num)) return '0 ₫'
  return `${num.toLocaleString('vi-VN')} ₫`
}

/**
 * Định dạng ngày YYYY-MM-DD sang DD/MM/YYYY
 */
export function formatDate(dateStr) {
  if (!dateStr) return '—'
  const parts = String(dateStr).split('T')[0].split('-')
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`
  }
  return dateStr
}

/**
 * Lấy chuỗi ngày hôm nay YYYY-MM-DD theo giờ địa phương
 */
export function getTodayDateString(refDate = new Date()) {
  const d = new Date(refDate)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * Validate dữ liệu dịch vụ trước khi gửi API
 */
export function validateServicePayload(payload, isEditing = false) {
  if (!payload) {
    return { valid: false, error: 'Dữ liệu dịch vụ không hợp lệ.' }
  }

  const { serviceCode, name, price, effectiveFrom } = payload

  // 1. Kiểm tra mã dịch vụ (chỉ bắt buộc khi tạo mới)
  if (!isEditing) {
    if (!serviceCode || typeof serviceCode !== 'string' || !serviceCode.trim()) {
      return { valid: false, error: 'Mã dịch vụ không được để trống.' }
    }
    if (serviceCode.trim().length > 50) {
      return { valid: false, error: 'Mã dịch vụ không được vượt quá 50 ký tự.' }
    }
  }

  // 2. Kiểm tra tên dịch vụ
  if (!name || typeof name !== 'string' || !name.trim()) {
    return { valid: false, error: 'Tên dịch vụ không được để trống.' }
  }
  if (name.trim().length > 255) {
    return { valid: false, error: 'Tên dịch vụ không được vượt quá 255 ký tự.' }
  }

  // 3. Kiểm tra đơn giá
  if (price === undefined || price === null || price === '') {
    return { valid: false, error: 'Đơn giá dịch vụ không được để trống.' }
  }
  const numericPrice = Number(price)
  if (Number.isNaN(numericPrice)) {
    return { valid: false, error: 'Đơn giá dịch vụ phải là chữ số.' }
  }
  if (numericPrice < 0) {
    return { valid: false, error: 'Đơn giá dịch vụ phải lớn hơn hoặc bằng 0.' }
  }

  // 4. Kiểm tra ngày hiệu lực
  if (!effectiveFrom) {
    return { valid: false, error: 'Ngày hiệu lực của giá không được để trống.' }
  }

  let formattedDate = effectiveFrom
  if (typeof effectiveFrom === 'object' && typeof effectiveFrom.format === 'function') {
    formattedDate = effectiveFrom.format('YYYY-MM-DD')
  } else if (typeof effectiveFrom === 'string') {
    formattedDate = effectiveFrom.split('T')[0]
  }

  if (!/^\d{4}-\d{2}-\d{2}$/.test(formattedDate)) {
    return { valid: false, error: 'Định dạng ngày hiệu lực không hợp lệ (YYYY-MM-DD).' }
  }

  return {
    valid: true,
    sanitizedData: {
      ...(isEditing ? {} : { serviceCode: serviceCode.trim().toUpperCase() }),
      name: name.trim().replace(/\s+/g, ' '),
      price: numericPrice,
      effectiveFrom: formattedDate,
      ...(isEditing ? { active: payload.active !== undefined ? Boolean(payload.active) : true } : {}),
    },
  }
}

/**
 * Xử lý danh sách lịch sử bảng giá (Price History)
 * - Sắp xếp theo effectiveFrom giảm dần
 * - Xác định phiên bản đang áp dụng (CURRENT_ACTIVE), sắp áp dụng (FUTURE_SCHEDULED), hoặc lịch sử (SUPERSEDED)
 * - Tính chênh lệch giá với phiên bản trước đó
 */
export function processPriceHistory(rawPrices = [], referenceDateStr = null) {
  if (!Array.isArray(rawPrices) || rawPrices.length === 0) {
    return []
  }

  const todayStr = referenceDateStr || getTodayDateString()

  // Sắp xếp theo effectiveFrom giảm dần; nếu cùng ngày thì theo createdAt giảm dần
  const sorted = [...rawPrices].sort((a, b) => {
    const dateA = String(a.effectiveFrom || '')
    const dateB = String(b.effectiveFrom || '')
    if (dateA !== dateB) {
      return dateB.localeCompare(dateA)
    }
    const createA = String(a.createdAt || '')
    const createB = String(b.createdAt || '')
    return createB.localeCompare(createA)
  })

  // Tìm phiên bản đang áp dụng: phiên bản mới nhất có effectiveFrom <= today
  let currentActiveIndex = -1
  for (let i = 0; i < sorted.length; i++) {
    const effDate = String(sorted[i].effectiveFrom || '')
    if (effDate <= todayStr) {
      currentActiveIndex = i
      break
    }
  }

  return sorted.map((item, index) => {
    const effDate = String(item.effectiveFrom || '')
    let status = 'SUPERSEDED'
    let statusLabel = 'Lịch sử'
    let statusColor = 'default'

    if (effDate > todayStr) {
      status = 'FUTURE_SCHEDULED'
      statusLabel = 'Sắp áp dụng'
      statusColor = 'processing'
    } else if (index === currentActiveIndex) {
      status = 'CURRENT_ACTIVE'
      statusLabel = 'Đang áp dụng'
      statusColor = 'success'
    }

    // Tính chênh lệch với phiên bản trước đó theo thời gian (là phần tử index + 1)
    const prevItem = sorted[index + 1]
    let diffAmount = null
    let diffPercent = null

    if (prevItem && prevItem.price !== undefined && prevItem.price !== null) {
      const currentPrice = Number(item.price || 0)
      const prevPrice = Number(prevItem.price || 0)
      diffAmount = currentPrice - prevPrice
      if (prevPrice > 0) {
        diffPercent = Number(((diffAmount / prevPrice) * 100).toFixed(1))
      }
    }

    return {
      ...item,
      status,
      statusLabel,
      statusColor,
      isCurrentActive: status === 'CURRENT_ACTIVE',
      diffAmount,
      diffPercent,
    }
  })
}

/**
 * Tính toán số liệu thống kê KPI từ danh sách dịch vụ
 */
export function calculateServiceStats(services = []) {
  const list = Array.isArray(services) ? services : []
  const total = list.length
  const activeCount = list.filter((s) => s.active !== false).length
  const inactiveCount = total - activeCount

  const prices = list
    .map((s) => Number(s.price))
    .filter((p) => !Number.isNaN(p) && p >= 0)

  const avgPrice = prices.length > 0
    ? Math.round(prices.reduce((sum, p) => sum + p, 0) / prices.length)
    : 0

  const minPrice = prices.length > 0 ? Math.min(...prices) : 0
  const maxPrice = prices.length > 0 ? Math.max(...prices) : 0

  return {
    total,
    activeCount,
    inactiveCount,
    avgPrice,
    minPrice,
    maxPrice,
  }
}

/**
 * Chuyển đổi mã lỗi Backend thành thông báo tiếng Việt dễ hiểu
 */
export function translateServiceErrorMessage(error, defaultMessage = 'Có lỗi xảy ra khi xử lý dịch vụ.') {
  const backendMsg = error?.response?.data?.message || error?.message || ''

  if (/service code already exists/i.test(backendMsg)) {
    return 'Mã dịch vụ đã tồn tại trong hệ thống. Vui lòng nhập mã khác.'
  }
  if (/service name already exists/i.test(backendMsg)) {
    return 'Tên dịch vụ đã tồn tại trong hệ thống. Vui lòng chọn tên khác.'
  }
  if (/different service price already exists for this effective date/i.test(backendMsg)) {
    return 'Đã tồn tại một mức giá khác cho ngày hiệu lực này. Vui lòng chọn ngày khác hoặc giữ nguyên giá.'
  }
  if (/service catalog not found/i.test(backendMsg)) {
    return 'Không tìm thấy dịch vụ tương ứng trên hệ thống.'
  }
  if (/access is denied|forbidden/i.test(backendMsg) || error?.response?.status === 403) {
    return 'Bạn không có quyền thực hiện thao tác quản lý dịch vụ/bảng giá.'
  }

  return backendMsg || defaultMessage
}
