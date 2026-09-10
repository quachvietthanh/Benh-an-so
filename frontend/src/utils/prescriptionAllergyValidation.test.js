import test from 'node:test'
import assert from 'node:assert/strict'
import {
  PRESET_ALLERGY_OVERRIDE_REASONS,
  validateAllergyOverrideReason,
  isAllergyHandled,
  areAllAllergiesHandled,
  getUnhandledAllergies,
  buildAllergyOverridesPayload,
  canSubmitPrescriptionWithAllergies,
} from './prescriptionAllergyValidation.js'

test('1. PRESET_ALLERGY_OVERRIDE_REASONS contains required clinical presets', () => {
  assert.ok(Array.isArray(PRESET_ALLERGY_OVERRIDE_REASONS))
  assert.ok(PRESET_ALLERGY_OVERRIDE_REASONS.length >= 4)
  assert.ok(PRESET_ALLERGY_OVERRIDE_REASONS.some((r) => r.includes('hội chẩn')))
})

test('2. validateAllergyOverrideReason rejects empty, null, and whitespace-only reasons', () => {
  assert.equal(validateAllergyOverrideReason('').valid, false)
  assert.equal(validateAllergyOverrideReason('   ').valid, false)
  assert.equal(validateAllergyOverrideReason(null).valid, false)
  assert.equal(validateAllergyOverrideReason(undefined).valid, false)
  assert.match(validateAllergyOverrideReason('').error, /không được để trống/)
})

test('3. validateAllergyOverrideReason rejects reason exceeding 500 characters', () => {
  const longReason = 'a'.repeat(501)
  const res = validateAllergyOverrideReason(longReason)
  assert.equal(res.valid, false)
  assert.match(res.error, /không được vượt quá 500 ký tự/)
})

test('4. validateAllergyOverrideReason trims whitespace and accepts valid reason', () => {
  const res = validateAllergyOverrideReason('  Bệnh nhân dung nạp tốt với liều điều trị  ')
  assert.equal(res.valid, true)
  assert.equal(res.error, '')
  assert.equal(res.trimmedReason, 'Bệnh nhân dung nạp tốt với liều điều trị')
})

test('5. isAllergyHandled correctly matches allergyId and medicineId with non-empty reason', () => {
  const warning = { allergyId: 'alg-1', medicineId: 'med-10' }
  const overrides = [
    { allergyId: 'alg-1', medicineId: 'med-10', overrideReason: 'Lợi ích vượt trội nguy cơ' },
  ]
  assert.equal(isAllergyHandled(warning, overrides), true)

  const emptyReasonOverrides = [
    { allergyId: 'alg-1', medicineId: 'med-10', overrideReason: '   ' },
  ]
  assert.equal(isAllergyHandled(warning, emptyReasonOverrides), false)

  const mismatchedOverrides = [
    { allergyId: 'alg-2', medicineId: 'med-10', overrideReason: 'Hợp lệ' },
  ]
  assert.equal(isAllergyHandled(warning, mismatchedOverrides), false)
})

test('6. areAllAllergiesHandled returns true when empty or all handled', () => {
  assert.equal(areAllAllergiesHandled([], []), true)

  const warnings = [
    { allergyId: 'alg-1', medicineId: 'med-1' },
    { allergyId: 'alg-2', medicineId: 'med-2' },
  ]
  const partialOverrides = [
    { allergyId: 'alg-1', medicineId: 'med-1', overrideReason: 'Lý do 1' },
  ]
  assert.equal(areAllAllergiesHandled(warnings, partialOverrides), false)

  const fullOverrides = [
    { allergyId: 'alg-1', medicineId: 'med-1', overrideReason: 'Lý do 1' },
    { allergyId: 'alg-2', medicineId: 'med-2', overrideReason: 'Lý do 2' },
  ]
  assert.equal(areAllAllergiesHandled(warnings, fullOverrides), true)
})

test('7. getUnhandledAllergies filters out handled warnings', () => {
  const warnings = [
    { allergyId: 'alg-1', medicineId: 'med-1' },
    { allergyId: 'alg-2', medicineId: 'med-2' },
  ]
  const overrides = [
    { allergyId: 'alg-1', medicineId: 'med-1', overrideReason: 'Đã xử lý' },
  ]
  const unhandled = getUnhandledAllergies(warnings, overrides)
  assert.equal(unhandled.length, 1)
  assert.equal(unhandled[0].allergyId, 'alg-2')
})

test('8. buildAllergyOverridesPayload formats payload for backend contract', () => {
  const warnings = [
    { allergyId: 'alg-1', medicineId: 'med-1' },
    { allergyId: 'alg-2', medicineId: 'med-2' },
  ]
  const payload = buildAllergyOverridesPayload(warnings, 'Phác đồ điều trị đã hội chẩn')
  assert.equal(payload.length, 2)
  assert.deepEqual(payload[0], {
    allergyId: 'alg-1',
    medicineId: 'med-1',
    overrideReason: 'Phác đồ điều trị đã hội chẩn',
  })
})

test('9. canSubmitPrescriptionWithAllergies disables submission when unhandled allergy exists', () => {
  const warnings = [{ allergyId: 'alg-1', medicineId: 'med-1' }]
  const res = canSubmitPrescriptionWithAllergies({
    canPrescribe: true,
    saving: false,
    checkingAllergies: false,
    allergyApiError: null,
    detectedAllergies: warnings,
    confirmedAllergyOverrides: [],
  })
  assert.equal(res.allowed, false)
  assert.match(res.reason, /1\/1 cảnh báo dị ứng thuốc chưa được xử lý/)
})

test('10. canSubmitPrescriptionWithAllergies allows submission when overrides confirmed', () => {
  const warnings = [{ allergyId: 'alg-1', medicineId: 'med-1' }]
  const confirmed = [{ allergyId: 'alg-1', medicineId: 'med-1', overrideReason: 'Đã giải thích cho bệnh nhân' }]
  const res = canSubmitPrescriptionWithAllergies({
    canPrescribe: true,
    saving: false,
    checkingAllergies: false,
    allergyApiError: null,
    detectedAllergies: warnings,
    confirmedAllergyOverrides: confirmed,
  })
  assert.equal(res.allowed, true)
  assert.equal(res.reason, '')
})
