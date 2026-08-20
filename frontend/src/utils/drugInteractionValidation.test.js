import test from 'node:test'
import assert from 'node:assert/strict'
import {
  validateOverrideReason,
  isInteractionHandled,
  areAllInteractionsHandled,
  getUnhandledInteractions,
  canSubmitPrescription,
} from './drugInteractionValidation.js'

test('1. Không có interaction -> Cho phép tạo đơn thuốc', () => {
  const result = canSubmitPrescription({
    canPrescribe: true,
    saving: false,
    checkingInteractions: false,
    interactionApiError: null,
    detectedInteractions: [],
    confirmedOverrides: [],
  })
  assert.equal(result.allowed, true)
  assert.equal(result.reason, '')
})

test('2. Có interaction chưa xử lý -> Nút tạo đơn bị disable & báo lý do', () => {
  const warnings = [
    { ruleId: 'rule-01', drugIdA: 'med-1', drugIdB: 'med-2', severity: 'SEVERE' },
  ]
  const result = canSubmitPrescription({
    canPrescribe: true,
    saving: false,
    checkingInteractions: false,
    interactionApiError: null,
    detectedInteractions: warnings,
    confirmedOverrides: [],
  })
  assert.equal(result.allowed, false)
  assert.match(result.reason, /1\/1 cảnh báo tương tác thuốc chưa được xử lý/)
})

test('3. Sửa thuốc làm hết interaction -> Cho phép tạo đơn thuốc', () => {
  const result = canSubmitPrescription({
    canPrescribe: true,
    saving: false,
    checkingInteractions: false,
    interactionApiError: null,
    detectedInteractions: [],
    confirmedOverrides: [],
  })
  assert.equal(result.allowed, true)
})

test('4. Bắt buộc nhập lý do bỏ qua hợp lệ (trống hoặc whitespace -> Không hợp lệ)', () => {
  assert.equal(validateOverrideReason('').valid, false)
  assert.equal(validateOverrideReason('   ').valid, false)
  assert.equal(validateOverrideReason(null).valid, false)

  const validRes = validateOverrideReason(' Theo dõi sát đáp ứng lâm sàng ')
  assert.equal(validRes.valid, true)
  assert.equal(validRes.trimmedReason, 'Theo dõi sát đáp ứng lâm sàng')
})

test('5. Interaction + đã nhập lý do override hợp lệ -> Cho phép tạo đơn thuốc', () => {
  const warnings = [
    { ruleId: 'rule-01', drugIdA: 'med-1', drugIdB: 'med-2', severity: 'SEVERE' },
  ]
  const overrides = [
    { ruleId: 'rule-01', overrideReason: 'Theo dõi sát đáp ứng lâm sàng' },
  ]
  assert.equal(areAllInteractionsHandled(warnings, overrides), true)
  assert.equal(getUnhandledInteractions(warnings, overrides).length, 0)

  const result = canSubmitPrescription({
    canPrescribe: true,
    saving: false,
    checkingInteractions: false,
    interactionApiError: null,
    detectedInteractions: warnings,
    confirmedOverrides: overrides,
  })
  assert.equal(result.allowed, true)
})

test('6. Thay đổi thuốc sau khi override -> Phải kiểm tra lại & reset override cũ', () => {
  const warnings = [
    { ruleId: 'rule-01', drugIdA: 'med-1', drugIdB: 'med-2', severity: 'SEVERE' },
    { ruleId: 'rule-02', drugIdA: 'med-1', drugIdB: 'med-3', severity: 'MODERATE' },
  ]
  const oldOverrides = [
    { ruleId: 'rule-01', overrideReason: 'Lý do cũ' },
  ]
  assert.equal(areAllInteractionsHandled(warnings, oldOverrides), false)
  assert.equal(getUnhandledInteractions(warnings, oldOverrides).length, 1)

  const result = canSubmitPrescription({
    canPrescribe: true,
    saving: false,
    checkingInteractions: false,
    interactionApiError: null,
    detectedInteractions: warnings,
    confirmedOverrides: [],
  })
  assert.equal(result.allowed, false)
})

test('7. API interaction bị lỗi -> Không cho tạo đơn & báo lỗi thử lại', () => {
  const result = canSubmitPrescription({
    canPrescribe: true,
    saving: false,
    checkingInteractions: false,
    interactionApiError: 'Không thể kiểm tra tương tác thuốc từ Backend.',
    detectedInteractions: [],
    confirmedOverrides: [],
  })
  assert.equal(result.allowed, false)
  assert.equal(result.reason, 'Không thể kiểm tra tương tác thuốc. Vui lòng thử lại.')
})
