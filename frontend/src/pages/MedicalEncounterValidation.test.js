import test from 'node:test'
import assert from 'node:assert/strict'

// Helper kiểm tra quyền bác sĩ (Authorization Check)
export const isDoctorRole = (roles = []) => {
  const allowed = ['admin', 'doctor', 'role_admin', 'role_doctor']
  return roles.some((role) => allowed.includes(String(role).toLowerCase()))
}

// Helper kiểm tra tính hợp lệ của Chẩn đoán y khoa (Diagnosis Validation)
export const validateMedicalEncounter = (visitId, formValues, primaryIcd) => {
  const errors = []

  if (!visitId) {
    errors.push('Thiếu visitId của lượt khám')
  }
  if (!formValues?.symptoms || !formValues.symptoms.trim()) {
    errors.push('Vui lòng nhập triệu chứng lâm sàng')
  }
  if (!primaryIcd && (!formValues?.diagnosisText || !formValues.diagnosisText.trim())) {
    errors.push('Vui lòng chọn Mã bệnh ICD-10 hoặc nhập nội dung Chẩn đoán chính!')
  }

  return {
    isValid: errors.length === 0,
    errors,
  }
}

// Helper tính tổng phí dịch vụ chỉ định cận lâm sàng
export const calculateTotalOrderFee = (orders = []) => {
  return orders.reduce((sum, item) => sum + (Number(item.price) || 0), 0)
}

// --- SUITE KIỂM THỬ TỰ ĐỘNG (Node.js Test Runner) ---

test('1. Kiểm thử THIẾU CHẨN ĐOÁN (Missing Diagnosis Validation)', () => {
  // TH 1.1: Thiếu cả mã ICD-10 lẫn nội dung chẩn đoán
  const invalidInput = {
    symptoms: 'Ho, sốt nhẹ 38 độ',
    diagnosisText: '',
  }
  const result1 = validateMedicalEncounter('visit-1', invalidInput, null)
  assert.equal(result1.isValid, false)
  assert.ok(result1.errors.includes('Vui lòng chọn Mã bệnh ICD-10 hoặc nhập nội dung Chẩn đoán chính!'))

  // TH 1.2: Có mã ICD-10 chính (J00 - Viêm mũi họng cấp)
  const validInputWithIcd = {
    symptoms: 'Ho, sốt nhẹ 38 độ',
  }
  const primaryIcd = { code: 'J00', name: 'Viêm mũi họng cấp' }
  const result2 = validateMedicalEncounter('visit-1', validInputWithIcd, primaryIcd)
  assert.equal(result2.isValid, true)
  assert.equal(result2.errors.length, 0)

  // TH 1.3: Có văn bản chẩn đoán trực tiếp
  const validInputWithText = {
    symptoms: 'Đau thắt lưng',
    diagnosisText: 'Thoái hóa cột sống thắt lưng',
  }
  const result3 = validateMedicalEncounter('visit-1', validInputWithText, null)
  assert.equal(result3.isValid, true)
  assert.equal(result3.errors.length, 0)
})

test('4. Không cho mở encounter chỉ bằng patientId', () => {
  const result = validateMedicalEncounter(null, { patientId: 'patient-1', symptoms: 'Đau đầu' }, {
    id: 'catalog-1',
    code: 'R51',
    name: 'Đau đầu',
  })
  assert.equal(result.isValid, false)
  assert.ok(result.errors.includes('Thiếu visitId của lượt khám'))
})

test('2. Kiểm thử QUYỀN TRUY CẬP VÀI TRÒ (User Role Authorization Check)', () => {
  // TH 2.1: Vai trò Bác sĩ (Doctor) -> Có quyền chẩn đoán & nhập chỉ định
  assert.equal(isDoctorRole(['doctor']), true)
  assert.equal(isDoctorRole(['ROLE_DOCTOR']), true)
  assert.equal(isDoctorRole(['admin']), true)

  // TH 2.2: Vai trò không phải Bác sĩ (Lễ tân, Dược sĩ, Bệnh nhân) -> Không có quyền
  assert.equal(isDoctorRole(['receptionist']), false)
  assert.equal(isDoctorRole(['pharmacist']), false)
  assert.equal(isDoctorRole(['patient']), false)
  assert.equal(isDoctorRole([]), false)
})

test('3. Kiểm thử TÍNH TỔNG CHI PHÍ CHỈ ĐỊNH CẬN LÂM SÀNG', () => {
  const sampleOrders = [
    { code: 'ORD-001', name: 'Công thức máu', price: 95000 },
    { code: 'ORD-010', name: 'X-quang ngực', price: 120000 },
    { code: 'ORD-012', name: 'Siêu âm bụng', price: 150000 },
  ]
  const total = calculateTotalOrderFee(sampleOrders)
  assert.equal(total, 365000)
})
