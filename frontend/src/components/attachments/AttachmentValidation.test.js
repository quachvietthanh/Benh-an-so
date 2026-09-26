import test from 'node:test'
import assert from 'node:assert/strict'

const ALLOWED_CONTENT_TYPES = ['application/pdf', 'image/jpeg', 'image/png']
const MAX_SIZE_BYTES = 10 * 1024 * 1024

export const validateAttachmentFile = (file) => {
  if (!file?.name) return { isValid: false, error: 'Tệp đính kèm không hợp lệ hoặc bị trống' }
  if (!ALLOWED_CONTENT_TYPES.includes(file.type)) {
    return { isValid: false, error: 'Chỉ hỗ trợ tệp PDF, JPG/JPEG và PNG' }
  }
  if (!file.size || file.size > MAX_SIZE_BYTES) {
    return { isValid: false, error: 'Dung lượng tệp phải từ 1 byte đến 10 MB' }
  }
  return { isValid: true, error: null }
}

export const formatAttachmentSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.floor(Math.log(bytes) / Math.log(1024))
  return `${parseFloat((bytes / (1024 ** index)).toFixed(1))} ${units[index]}`
}

export const filterAttachmentsByCategory = (attachments = [], category = 'ALL') => {
  if (!category || category === 'ALL') return attachments
  return attachments.filter((item) =>
    String(item.category).toLowerCase() === String(category).toLowerCase(),
  )
}

export const validateAttachmentForm = (formValues) => {
  const errors = []
  if (!formValues?.patientId) errors.push('Vui lòng chọn bệnh nhân')
  if (!formValues?.resultId) errors.push('Vui lòng chọn kết quả cận lâm sàng')
  return { isValid: errors.length === 0, errors }
}

test('chỉ nhận PDF/JPEG/PNG tối đa 10 MB theo backend', () => {
  assert.equal(validateAttachmentFile({ name: 'ket-qua.pdf', type: 'application/pdf', size: 2 * 1024 * 1024 }).isValid, true)
  assert.equal(validateAttachmentFile({ name: 'x-quang.jpg', type: 'image/jpeg', size: 512 * 1024 }).isValid, true)
  assert.equal(validateAttachmentFile({ name: 'sieu-am.png', type: 'image/png', size: 512 * 1024 }).isValid, true)
  assert.equal(validateAttachmentFile({ name: 'anh.webp', type: 'image/webp', size: 512 * 1024 }).isValid, false)
  assert.equal(validateAttachmentFile({ name: 'qua-lon.pdf', type: 'application/pdf', size: 11 * 1024 * 1024 }).isValid, false)
})

test('định dạng dung lượng tệp', () => {
  assert.equal(formatAttachmentSize(0), '0 B')
  assert.equal(formatAttachmentSize(512), '512 B')
  assert.equal(formatAttachmentSize(1024), '1 KB')
  assert.equal(formatAttachmentSize(1572864), '1.5 MB')
})

test('lọc attachment theo loại kết quả', () => {
  const attachments = [
    { id: 1, category: 'LAB_RESULT' },
    { id: 2, category: 'IMAGING_RESULT' },
    { id: 3, category: 'LAB_RESULT' },
  ]
  assert.equal(filterAttachmentsByCategory(attachments, 'LAB_RESULT').length, 2)
  assert.equal(filterAttachmentsByCategory(attachments, 'ALL').length, 3)
})

test('form upload bắt buộc patientId và clinical resultId', () => {
  const missingResult = validateAttachmentForm({ patientId: 'patient-1' })
  assert.equal(missingResult.isValid, false)
  assert.ok(missingResult.errors.includes('Vui lòng chọn kết quả cận lâm sàng'))

  const valid = validateAttachmentForm({ patientId: 'patient-1', resultId: 'result-1' })
  assert.equal(valid.isValid, true)
})
