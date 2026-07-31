import test from 'node:test'
import assert from 'node:assert/strict'

/**
 * 1. Helper kiểm tra tính hợp lệ của tệp đính kèm kết quả (Attachment File Validation)
 */
export const validateAttachmentFile = (file) => {
  const allowedExtensions = ['pdf', 'jpg', 'jpeg', 'png', 'webp', 'dcm']
  const maxSizeBytes = 15 * 1024 * 1024 // 15 MB limit

  if (!file || !file.name) {
    return { isValid: false, error: 'Tệp đính kèm không hợp lệ hoặc bị trống' }
  }

  const ext = file.name.split('.').pop().toLowerCase()
  if (!allowedExtensions.includes(ext)) {
    return { isValid: false, error: `Định dạng .${ext} không được hỗ trợ. Vui lòng chọn tệp PDF, JPG, PNG, WEBP.` }
  }

  if (file.size > maxSizeBytes) {
    return { isValid: false, error: 'Dung lượng tệp vượt quá giới hạn 15 MB' }
  }

  return { isValid: true, error: null }
}

/**
 * 2. Helper định dạng kích thước tệp hiển thị trên giao diện (Format File Size)
 */
export const formatAttachmentSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`
}

/**
 * 3. Helper lọc danh sách đính kèm theo Loại kết quả (Filter Attachments)
 */
export const filterAttachmentsByCategory = (attachments = [], category = 'ALL') => {
  if (!category || category === 'ALL') return attachments
  return attachments.filter((item) => String(item.category).toLowerCase() === String(category).toLowerCase())
}

/**
 * 4. Helper kiểm tra thông tin Form Tải lên đính kèm (Attachment Form Validation)
 */
export const validateAttachmentForm = (formValues) => {
  const errors = []
  if (!formValues?.patientId) errors.push('Vui lòng chọn bệnh nhân')
  if (!formValues?.category) errors.push('Vui lòng chọn loại kết quả cận lâm sàng')
  if (!formValues?.resultSummary || !formValues.resultSummary.trim()) {
    errors.push('Vui lòng nhập tóm tắt kết quả cận lâm sàng')
  }

  return {
    isValid: errors.length === 0,
    errors,
  }
}

// =========================================================================
// --- BỘ KIỂM THỬ TỰ ĐỘNG GIAO DIỆN VÀ TỆP ĐÍNH KÈM (AUTOMATED SUITE) ---
// =========================================================================

test('1. KIỂM THỬ XÁC THỰC TỆP ĐÍNH KÈM (File Format & Size Limit Test)', () => {
  // TH 1.1: Tệp PDF hợp lệ < 15MB
  const validPdf = { name: 'ket_qua_x_quang.pdf', size: 2 * 1024 * 1024 }
  const result1 = validateAttachmentFile(validPdf)
  assert.equal(result1.isValid, true)
  assert.equal(result1.error, null)

  // TH 1.2: Tệp hình ảnh PNG hợp lệ
  const validPng = { name: 'sieu_am_bung.png', size: 500 * 1024 }
  const result2 = validateAttachmentFile(validPng)
  assert.equal(result2.isValid, true)

  // TH 1.3: Tệp quá dung lượng 15MB -> Bị từ chối
  const oversizedFile = { name: 'file_too_large.pdf', size: 20 * 1024 * 1024 }
  const result3 = validateAttachmentFile(oversizedFile)
  assert.equal(result3.isValid, false)
  assert.equal(result3.error, 'Dung lượng tệp vượt quá giới hạn 15 MB')

  // TH 1.4: Định dạng tệp không được hỗ trợ (.exe) -> Bị từ chối
  const invalidExt = { name: 'malware.exe', size: 1 * 1024 * 1024 }
  const result4 = validateAttachmentFile(invalidExt)
  assert.equal(result4.isValid, false)
  assert.ok(result4.error.includes('không được hỗ trợ'))
})

test('2. KIỂM THỬ ĐỊNH DẠNG DUNG LƯỢNG TỆP HIỂN THỊ (File Size Formatter)', () => {
  assert.equal(formatAttachmentSize(0), '0 B')
  assert.equal(formatAttachmentSize(512), '512 B')
  assert.equal(formatAttachmentSize(1024), '1 KB')
  assert.equal(formatAttachmentSize(1572864), '1.5 MB')
})

test('3. KIỂM THỬ LỌC DANH SÁCH ĐÍNH KÈM THEO DANH MỤC (Category Filter Test)', () => {
  const attachments = [
    { id: 1, name: 'File A', category: 'Công thức máu' },
    { id: 2, name: 'File B', category: 'X-quang ngực' },
    { id: 3, name: 'File C', category: 'Công thức máu' },
  ]

  const bloodResults = filterAttachmentsByCategory(attachments, 'Công thức máu')
  assert.equal(bloodResults.length, 2)

  const xrayResults = filterAttachmentsByCategory(attachments, 'X-quang ngực')
  assert.equal(xrayResults.length, 1)

  const allResults = filterAttachmentsByCategory(attachments, 'ALL')
  assert.equal(allResults.length, 3)
})

test('4. KIỂM THỬ THÔNG TIN FORM TẢI LÊN TỆP ĐÍNH KÈM (Upload Form Validation)', () => {
  // TH 4.1: Thiếu thông tin bắt buộc
  const invalidForm = { patientId: 'p1', category: 'Công thức máu', resultSummary: '' }
  const valResult1 = validateAttachmentForm(invalidForm)
  assert.equal(valResult1.isValid, false)
  assert.ok(valResult1.errors.includes('Vui lòng nhập tóm tắt kết quả cận lâm sàng'))

  // TH 4.2: Điền đầy đủ thông tin hợp lệ
  const validForm = {
    patientId: 'p1',
    category: 'X-quang ngực',
    resultSummary: 'Hình ảnh tim phổi bình thường, không phát hiện tổn thương nhu mô phổi.',
  }
  const valResult2 = validateAttachmentForm(validForm)
  assert.equal(valResult2.isValid, true)
  assert.equal(valResult2.errors.length, 0)
})
