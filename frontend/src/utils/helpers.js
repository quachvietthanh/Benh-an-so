/**
 * Format date to Vietnamese locale
 */
export const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('vi-VN')
}

/**
 * Format date time to Vietnamese locale
 */
export const formatDateTime = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('vi-VN')
}

/**
 * Format gender
 */
export const formatGender = (gender) => {
  const map = { MALE: 'Nam', FEMALE: 'Nữ', OTHER: 'Khác' }
  if (map[gender]) return map[gender]
  if (['Nam', 'Nữ', 'Khác'].includes(gender)) return gender
  return 'Không xác định'
}

/**
 * Format record status with badge color
 */
export const formatRecordStatus = (status) => {
  const map = {
    NEW: { label: 'Mới', color: 'blue' },
    IN_PROGRESS: { label: 'Đang xử lý', color: 'orange' },
    COMPLETED: { label: 'Hoàn thành', color: 'green' },
    CANCELLED: { label: 'Đã hủy', color: 'red' },
  }
  return map[status] || { label: 'Không xác định', color: 'default' }
}

/**
 * Generate a random color from a string
 */
export const stringToColor = (str) => {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  const color = `hsl(${hash % 360}, 70%, 50%)`
  return color
}
