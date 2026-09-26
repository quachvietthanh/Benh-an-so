import dayjs from 'dayjs'

/**
 * Kiểm tra xem ID có phải là định dạng UUID hợp lệ hay không.
 * Ngăn chặn việc lộ hoặc đổi mã lượt khám dạng số tuần tự (1, 2, 3...) trên URL.
 */
export const isValidUuid = (id) => {
  if (!id || typeof id !== 'string') return false
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
  return uuidRegex.test(id.trim())
}

/**
 * Lọc CHỈ các kết quả đã được bác sĩ xác nhận (status === 'FINAL').
 * Tuyệt đối không giữ lại bất kỳ kết quả nháp (DRAFT) hay trạng thái nào khác
 * để tránh để lộ thông tin bất thường chưa được kết luận chính thức.
 */
export const filterConfirmedResults = (results = []) => {
  if (!Array.isArray(results)) return []
  return results.filter((item) => item && item.status === 'FINAL')
}

/**
 * Phân loại và lấy thông tin hiển thị cho cờ bất thường (abnormalFlag)
 */
export const getAbnormalFlagInfo = (flag) => {
  const normalized = String(flag || '').toUpperCase().trim()
  switch (normalized) {
    case 'HIGH':
      return {
        flag: 'HIGH',
        color: 'error',
        badgeColor: '#dc2626',
        textColor: '#b91c1c',
        bgLight: '#fef2f2',
        borderLight: '#fecaca',
        text: 'Cao hơn bình thường',
        shortText: 'Cao',
        isAbnormal: true,
        direction: 'HIGH',
      }
    case 'LOW':
      return {
        flag: 'LOW',
        color: 'warning',
        badgeColor: '#d97706',
        textColor: '#b45309',
        bgLight: '#fffbeb',
        borderLight: '#fde68a',
        text: 'Thấp hơn bình thường',
        shortText: 'Thấp',
        isAbnormal: true,
        direction: 'LOW',
      }
    case 'ABNORMAL':
      return {
        flag: 'ABNORMAL',
        color: 'error',
        badgeColor: '#e11d48',
        textColor: '#be123c',
        bgLight: '#fff1f2',
        borderLight: '#fecdd3',
        text: 'Chỉ số bất thường',
        shortText: 'Bất thường',
        isAbnormal: true,
        direction: 'ABNORMAL',
      }
    case 'NORMAL':
    default:
      return {
        flag: 'NORMAL',
        color: 'success',
        badgeColor: '#16a34a',
        textColor: '#15803d',
        bgLight: '#f0fdf4',
        borderLight: '#bbf7d0',
        text: 'Chỉ số bình thường',
        shortText: 'Bình thường',
        isAbnormal: false,
        direction: 'NORMAL',
      }
  }
}

/**
 * Định dạng ngày khám (DD/MM/YYYY)
 */
export const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = dayjs(dateStr)
  return d.isValid() ? d.format('DD/MM/YYYY') : '—'
}

/**
 * Định dạng giờ (HH:mm)
 */
export const formatTime = (dateStr) => {
  if (!dateStr) return '—'
  const d = dayjs(dateStr)
  return d.isValid() ? d.format('HH:mm') : '—'
}

/**
 * Định dạng ngày giờ đầy đủ (DD/MM/YYYY HH:mm)
 */
export const formatDateTime = (dateStr) => {
  if (!dateStr) return '—'
  const d = dayjs(dateStr)
  return d.isValid() ? d.format('DD/MM/YYYY HH:mm') : '—'
}

/**
 * Kiểm tra xem kết quả có phải là kết quả trả sau (về sau khi lượt khám đã diễn ra) hay không.
 * Nếu kết quả được nhập sau giờ khám từ 2 giờ trở lên hoặc sang ngày khác.
 */
export const isPostVisitResult = (enteredAt, visitAt) => {
  if (!enteredAt || !visitAt) return false
  const entered = dayjs(enteredAt)
  const visit = dayjs(visitAt)
  if (!entered.isValid() || !visit.isValid()) return false
  const diffHours = entered.diff(visit, 'hour', true)
  return diffHours >= 2
}

/**
 * Kiểm tra xem kết quả có phải mới có trong vòng số ngày quy định (mặc định 7 ngày) hay không
 */
export const isRecentResult = (enteredAt, maxDays = 7) => {
  if (!enteredAt) return false
  const entered = dayjs(enteredAt)
  if (!entered.isValid()) return false
  const diffDays = dayjs().diff(entered, 'day', true)
  return diffDays >= 0 && diffDays <= maxDays
}

/**
 * Tính phần trăm vị trí giá trị trên thanh khoảng đo tham chiếu (để vẽ Visual Gauge)
 */
export const calculateValuePercentage = (value, lower, upper) => {
  const numVal = Number(value)
  const numLow = Number(lower)
  const numUp = Number(upper)

  if (isNaN(numVal) || isNaN(numLow) || isNaN(numUp) || numUp <= numLow) {
    return 50
  }

  // Dải đo mở rộng thêm 25% mỗi phía để hiển thị giá trị nằm ngoài khoảng
  const range = numUp - numLow
  const expandedMin = numLow - range * 0.25
  const expandedMax = numUp + range * 0.25
  const totalExpanded = expandedMax - expandedMin

  let pct = ((numVal - expandedMin) / totalExpanded) * 100
  if (pct < 4) pct = 4
  if (pct > 96) pct = 96
  return Math.round(pct)
}

/**
 * Tải file PDF từ blob nhị phân
 */
export const downloadPdfBlob = (blob, filename = 'ket-qua-can-lam-sang.pdf') => {
  if (!blob) return
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.setAttribute('data-testid', 'clinical-result-download-anchor')
  document.body.appendChild(link)
  link.click()

  setTimeout(() => {
    if (link.parentNode) {
      link.parentNode.removeChild(link)
    }
    window.URL.revokeObjectURL(url)
  }, 1000)
}

/**
 * Xử lý lỗi bảo mật an toàn cho bệnh nhân:
 * Không tiết lộ liệu lượt khám hay kết quả có tồn tại hay không khi nhận 403 hoặc 404
 */
export const getSecuritySafeErrorMessage = (error) => {
  const status = error?.response?.status || error?.status
  if (status === 403 || status === 404) {
    return 'Không tìm thấy kết quả cận lâm sàng của lượt khám này hoặc bạn không có quyền truy cập.'
  }
  if (status === 401) {
    return 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.'
  }
  return (
    error?.response?.data?.message ||
    error?.message ||
    'Không thể tải dữ liệu kết quả cận lâm sàng. Vui lòng thử lại sau.'
  )
}

/**
 * Phân loại danh mục dịch vụ cận lâm sàng (Xét nghiệm, CĐHA, Chức năng)
 */
export const getServiceCategory = (serviceCode = '', serviceName = '') => {
  const code = String(serviceCode).toUpperCase()
  const name = String(serviceName).toLowerCase()

  if (code.startsWith('XN') || name.includes('xét nghiệm') || name.includes('máu') || name.includes('nước tiểu') || name.includes('sinh hóa')) {
    return {
      category: 'LAB',
      label: 'Xét nghiệm',
      color: 'blue',
    }
  }
  if (code.startsWith('CDHA') || code.startsWith('XQ') || name.includes('x-quang') || name.includes('siêu âm') || name.includes('ct') || name.includes('cộng hưởng từ') || name.includes('mri')) {
    return {
      category: 'IMAGING',
      label: 'Chẩn đoán hình ảnh',
      color: 'purple',
    }
  }
  if (
    code.startsWith('TDCN') ||
    code.includes('ECG') ||
    name.includes('điện tim') ||
    name.includes('điện tâm đồ') ||
    name.includes('nội soi') ||
    name.includes('đo chức năng')
  ) {
    return {
      category: 'FUNCTIONAL',
      label: 'Thăm dò chức năng',
      color: 'cyan',
    }
  }
  return {
    category: 'GENERAL',
    label: 'Cận lâm sàng',
    color: 'geekblue',
  }
}
