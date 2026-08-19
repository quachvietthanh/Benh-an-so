import test from 'node:test'
import assert from 'node:assert/strict'

export const isDoctorRole = (roles = []) => {
  const allowed = ['admin', 'doctor', 'role_admin', 'role_doctor']
  return roles.some((role) => allowed.includes(String(role).toLowerCase()))
}

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

export const calculateTotalOrderFee = (orders = []) => {
  return orders.reduce((sum, item) => sum + (Number(item.price) || 0), 0)
}

test('1. Kiểm thử THIẾU CHẨN ĐOÁN (Missing Diagnosis Validation)', () => {
  const invalidInput = {
    symptoms: 'Ho, sốt nhẹ 38 độ',
    diagnosisText: '',
  }
  const result1 = validateMedicalEncounter('visit-1', invalidInput, null)
  assert.equal(result1.isValid, false)
  assert.ok(result1.errors.includes('Vui lòng chọn Mã bệnh ICD-10 hoặc nhập nội dung Chẩn đoán chính!'))

  const validInputWithIcd = {
    symptoms: 'Ho, sốt nhẹ 38 độ',
  }
  const primaryIcd = { code: 'J00', name: 'Viêm mũi họng cấp' }
  const result2 = validateMedicalEncounter('visit-1', validInputWithIcd, primaryIcd)
  assert.equal(result2.isValid, true)
  assert.equal(result2.errors.length, 0)

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
  assert.equal(isDoctorRole(['doctor']), true)
  assert.equal(isDoctorRole(['ROLE_DOCTOR']), true)
  assert.equal(isDoctorRole(['admin']), true)

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
