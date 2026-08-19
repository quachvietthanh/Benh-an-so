export const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('vi-VN')
}

export const formatDateTime = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('vi-VN')
}

export const formatGender = (gender) => {
  const map = { MALE: 'Nam', FEMALE: 'Nữ', OTHER: 'Khác' }
  if (map[gender]) return map[gender]
  if (['Nam', 'Nữ', 'Khác'].includes(gender)) return gender
  return 'Không xác định'
}

export const formatRecordStatus = (status) => {
  const map = {
    NEW: { label: 'Mới', color: 'blue' },
    IN_PROGRESS: { label: 'Đang xử lý', color: 'orange' },
    COMPLETED: { label: 'Hoàn thành', color: 'green' },
    CANCELLED: { label: 'Đã hủy', color: 'red' },
  }
  return map[status] || { label: 'Không xác định', color: 'default' }
}

export const stringToColor = (str) => {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  const color = `hsl(${hash % 360}, 70%, 50%)`
  return color
}
