import test from 'node:test'
import assert from 'node:assert/strict'

// Helper kiểm tra điều kiện cần gọi API checkInteractions
export const shouldCheckInteractions = (items = []) => {
  const validItems = (items || []).filter((item) => Boolean(item && item.medicineId))
  const medicineIds = [...new Set(validItems.map((item) => item.medicineId))]
  return {
    shouldCheck: medicineIds.length >= 2,
    medicineIds,
  }
}

// Helper lọc và khử trùng lặp cảnh báo (Bỏ A-A, khử trùng A-B và B-A)
export const filterAndDeduplicateWarnings = (rawWarnings = [], medicines = [], validItems = []) => {
  const seenPairs = new Set()
  const uniqueWarnings = []

  for (const warning of rawWarnings || []) {
    const idA = String(warning?.drugIdA || '')
    const idB = String(warning?.drugIdB || '')
    
    // Ràng buộc 1: Không kiểm tra A-A hoặc thiếu ID
    if (!idA || !idB || idA === idB) continue

    // Ràng buộc 2: Khử trùng A-B và B-A (dùng key sắp xếp)
    const pairKey = [idA, idB].sort().join('_')
    if (!seenPairs.has(pairKey)) {
      seenPairs.add(pairKey)
      const medA = medicines.find((m) => String(m.id) === idA) || validItems.find((i) => String(i.medicineId) === idA)
      const medB = medicines.find((m) => String(m.id) === idB) || validItems.find((i) => String(i.medicineId) === idB)

      uniqueWarnings.push({
        ...warning,
        drugNameA: medA?.medicineName || medA?.name || warning.drugIdA,
        drugNameB: medB?.medicineName || medB?.name || warning.drugIdB,
      })
    }
  }

  return uniqueWarnings
}

// Helper kiểm tra lý do bỏ qua cảnh báo (Validation)
export const validateOverrideReason = (overrideReason) => {
  if (!overrideReason || typeof overrideReason !== 'string' || !overrideReason.trim()) {
    return {
      isValid: false,
      errorMsg: 'Vui lòng nhập lý do bỏ qua cảnh báo (không được để trống hoặc chỉ có khoảng trắng).',
    }
  }
  return {
    isValid: true,
    trimmedReason: overrideReason.trim(),
  }
}

// Helper đóng gói payload interactionOverrides gửi lên Backend
export const formatInteractionOverrides = (warnings = [], overrideReason = '') => {
  const validation = validateOverrideReason(overrideReason)
  if (!validation.isValid) {
    throw new Error(validation.errorMsg)
  }

  return (warnings || []).map((w) => ({
    ruleId: w.ruleId,
    overrideReason: validation.trimmedReason,
  }))
}

// --- SUITE KIỂM THỬ TỰ ĐỘNG TƯƠNG TÁC THUỐC (NCL-05-CN-002-CV-03) ---

test('1. Kiểm thử RÀNG BUỘC SỐ LƯỢNG THUỐC: 1 thuốc không kích hoạt kiểm tra tương tác', () => {
  const items1 = [{ medicineId: 'med-01', dosage: '1 viên' }]
  const res1 = shouldCheckInteractions(items1)
  assert.equal(res1.shouldCheck, false)
  assert.equal(res1.medicineIds.length, 1)

  const itemsEmpty = []
  const resEmpty = shouldCheckInteractions(itemsEmpty)
  assert.equal(resEmpty.shouldCheck, false)
  assert.equal(resEmpty.medicineIds.length, 0)
})

test('2. Kiểm thử KÍCH HOẠT KIỂM TRA: 2 thuốc trở lên cần kiểm tra tương tác', () => {
  const items2 = [
    { medicineId: 'med-01', dosage: '1 viên' },
    { medicineId: 'med-02', dosage: '2 viên' },
  ]
  const res2 = shouldCheckInteractions(items2)
  assert.equal(res2.shouldCheck, true)
  assert.equal(res2.medicineIds.length, 2)
  assert.deepEqual(res2.medicineIds, ['med-01', 'med-02'])
})

test('3. Kiểm thử LOẠI BỎ TỰ TƯƠNG TÁC (A-A)', () => {
  const rawWarningsWithSelf = [
    { ruleId: 'r1', drugIdA: 'med-01', drugIdB: 'med-01', severity: 'HIGH', description: 'Tự tương tác' },
    { ruleId: 'r2', drugIdA: 'med-01', drugIdB: 'med-02', severity: 'MODERATE', description: 'Tương tác A-B' },
  ]
  const medicines = [
    { id: 'med-01', medicineName: 'Paracetamol 500mg' },
    { id: 'med-02', medicineName: 'Ibuprofen 400mg' },
  ]

  const filtered = filterAndDeduplicateWarnings(rawWarningsWithSelf, medicines)
  assert.equal(filtered.length, 1)
  assert.equal(filtered[0].ruleId, 'r2')
  assert.equal(filtered[0].drugNameA, 'Paracetamol 500mg')
  assert.equal(filtered[0].drugNameB, 'Ibuprofen 400mg')
})

test('4. Kiểm thử KHỬ TRÙNG LẶP CẶP TƯƠNG TÁC (A-B và B-A chỉ hiển thị 1 lần)', () => {
  const rawDuplicates = [
    { ruleId: 'r1', drugIdA: 'med-01', drugIdB: 'med-02', severity: 'SEVERE', description: 'Tương tác Paracetamol & Ibuprofen' },
    { ruleId: 'r2', drugIdA: 'med-02', drugIdB: 'med-01', severity: 'SEVERE', description: 'Tương tác Ibuprofen & Paracetamol' },
  ]
  const medicines = [
    { id: 'med-01', medicineName: 'Paracetamol 500mg' },
    { id: 'med-02', medicineName: 'Ibuprofen 400mg' },
  ]

  const deduplicated = filterAndDeduplicateWarnings(rawDuplicates, medicines)
  assert.equal(deduplicated.length, 1)
  assert.equal(deduplicated[0].ruleId, 'r1')
})

test('5. Kiểm thử VALIDATION LÝ DO BỎ QUA CẢNH BÁO: Bắt buộc nhập, từ chối chuỗi rỗng / khoảng trắng', () => {
  // TH 5.1: Bỏ trống
  const v1 = validateOverrideReason('')
  assert.equal(v1.isValid, false)
  assert.ok(v1.errorMsg.includes('không được để trống'))

  // TH 5.2: Chỉ có khoảng trắng
  const v2 = validateOverrideReason('   \n\t   ')
  assert.equal(v2.isValid, false)
  assert.ok(v2.errorMsg.includes('khoảng trắng'))

  // TH 5.3: Lý do hợp lệ
  const validReasonText = '  Bệnh nhân được theo dõi chức năng gan sát sao và giảm liều.  '
  const v3 = validateOverrideReason(validReasonText)
  assert.equal(v3.isValid, true)
  assert.equal(v3.trimmedReason, 'Bệnh nhân được theo dõi chức năng gan sát sao và giảm liều.')
})

test('6. Kiểm thử ĐÓNG GÓI PAYLOAD INTERACTION OVERRIDES GỬI LÊN BACKEND', () => {
  const warnings = [
    { ruleId: 'rule-uuid-01', drugIdA: 'med-01', drugIdB: 'med-02', severity: 'MODERATE' },
    { ruleId: 'rule-uuid-02', drugIdA: 'med-01', drugIdB: 'med-03', severity: 'SEVERE' },
  ]
  const reason = '  Lợi ích điều trị vượt trội so với nguy cơ, bác sĩ đã hội chẩn.  '

  const payload = formatInteractionOverrides(warnings, reason)
  assert.equal(payload.length, 2)
  assert.equal(payload[0].ruleId, 'rule-uuid-01')
  assert.equal(payload[0].overrideReason, 'Lợi ích điều trị vượt trội so với nguy cơ, bác sĩ đã hội chẩn.')
  assert.equal(payload[1].ruleId, 'rule-uuid-02')
  assert.equal(payload[1].overrideReason, 'Lợi ích điều trị vượt trội so với nguy cơ, bác sĩ đã hội chẩn.')
})
