import test from 'node:test'
import assert from 'node:assert/strict'

/**
 * USER STORY:
 * "Là bác sĩ, tôi muốn được cảnh báo khi các thuốc trong đơn có tương tác,
 * để tránh kê đơn gây hại cho bệnh nhân."
 */

// Catalog danh mục thuốc seeded thực tế trong DB (V17)
export const MEDICINE_CATALOG = new Map([
  ['16000000-0000-0000-0000-000000000001', { medicineName: 'Paracetamol 500 mg', activeIngredient: 'Paracetamol' }],
  ['16000000-0000-0000-0000-000000000002', { medicineName: 'Ibuprofen 400 mg', activeIngredient: 'Ibuprofen' }],
  ['16000000-0000-0000-0000-000000000003', { medicineName: 'Amoxicillin 500 mg', activeIngredient: 'Amoxicillin' }],
  ['16000000-0000-0000-0000-000000000004', { medicineName: 'Amoxicillin Clavulanate 625 mg', activeIngredient: 'Amoxicillin + Clavulanic acid' }],
  ['16000000-0000-0000-0000-000000000005', { medicineName: 'Azithromycin 500 mg', activeIngredient: 'Azithromycin' }],
  ['16000000-0000-0000-0000-000000000006', { medicineName: 'Cefuroxime 500 mg', activeIngredient: 'Cefuroxime' }],
  ['16000000-0000-0000-0000-000000000007', { medicineName: 'Omeprazole 20 mg', activeIngredient: 'Omeprazole' }],
  ['16000000-0000-0000-0000-000000000008', { medicineName: 'Esomeprazole 40 mg', activeIngredient: 'Esomeprazole' }],
  ['16000000-0000-0000-0000-000000000009', { medicineName: 'Cetirizine 10 mg', activeIngredient: 'Cetirizine' }],
  ['16000000-0000-0000-0000-000000000010', { medicineName: 'Loratadine 10 mg', activeIngredient: 'Loratadine' }],
  ['16000000-0000-0000-0000-000000000016', { medicineName: 'Metformin 500 mg', activeIngredient: 'Metformin' }],
  ['16000000-0000-0000-0000-000000000017', { medicineName: 'Gliclazide MR 30 mg', activeIngredient: 'Gliclazide' }],
  ['16000000-0000-0000-0000-000000000018', { medicineName: 'Atorvastatin 20 mg', activeIngredient: 'Atorvastatin' }],
  ['16000000-0000-0000-0000-000000000021', { medicineName: 'Diclofenac 50 mg', activeIngredient: 'Diclofenac' }],
  ['16000000-0000-0000-0000-000000000023', { medicineName: 'Prednisolone 5 mg', activeIngredient: 'Prednisolone' }],
  ['16000000-0000-0000-0000-000000000028', { medicineName: 'Levofloxacin 500 mg', activeIngredient: 'Levofloxacin' }],
  ['16000000-0000-0000-0000-000000000029', { medicineName: 'Warfarin 2 mg', activeIngredient: 'Warfarin' }],
])

// Rule tương tác mẫu theo database V17
export const MOCK_INTERACTION_RULES = [
  {
    ruleId: '17100000-0000-0000-0000-000000000001',
    ingredientA: 'Amoxicillin',
    ingredientB: 'Amoxicillin + Clavulanic acid',
    severity: 'MODERATE',
    description: 'This duplicates penicillin therapy.',
    clinicalRecommendation: 'Do not prescribe both together unless there is a clear justification.',
  },
  {
    ruleId: '17100000-0000-0000-0000-000000000002',
    ingredientA: 'Azithromycin',
    ingredientB: 'Levofloxacin',
    severity: 'SEVERE',
    description: 'The combination may increase QT prolongation risk.',
    clinicalRecommendation: 'Avoid the combination and consider an alternative antibiotic.',
  },
  {
    ruleId: '17100000-0000-0000-0000-000000000003',
    ingredientA: 'Omeprazole',
    ingredientB: 'Esomeprazole',
    severity: 'MODERATE',
    description: 'This duplicates proton pump inhibitor therapy.',
    clinicalRecommendation: 'Use only one proton pump inhibitor at a time.',
  },
  {
    ruleId: '17100000-0000-0000-0000-000000000004',
    ingredientA: 'Cetirizine',
    ingredientB: 'Loratadine',
    severity: 'MODERATE',
    description: 'This duplicates H1-antihistamine therapy.',
    clinicalRecommendation: 'Use only one antihistamine to reduce excess sedation.',
  },
  {
    ruleId: '17100000-0000-0000-0000-000000000005',
    ingredientA: 'Metformin',
    ingredientB: 'Gliclazide',
    severity: 'MILD',
    description: 'The combination can increase hypoglycemia risk.',
    clinicalRecommendation: 'Monitor blood glucose and counsel the patient on hypoglycemia symptoms.',
  },
  {
    ruleId: '17100000-0000-0000-0000-000000000006',
    ingredientA: 'Diclofenac',
    ingredientB: 'Warfarin',
    severity: 'CONTRAINDICATED',
    description: 'Diclofenac can substantially increase bleeding risk when used with Warfarin.',
    clinicalRecommendation: 'Avoid the combination; consider Paracetamol if analgesia is needed.',
  },
  {
    ruleId: '17100000-0000-0000-0000-000000000007',
    ingredientA: 'Atorvastatin',
    ingredientB: 'Azithromycin',
    severity: 'MODERATE',
    description: 'Azithromycin may increase Atorvastatin exposure in susceptible patients.',
    clinicalRecommendation: 'Monitor for muscle pain and consider holding the statin during short antibiotic courses.',
  },
  {
    ruleId: '17100000-0000-0000-0000-000000000008',
    ingredientA: 'Metformin',
    ingredientB: 'Prednisolone',
    severity: 'MODERATE',
    description: 'Prednisolone can worsen glycemic control in patients taking Metformin.',
    clinicalRecommendation: 'Monitor blood glucose during corticosteroid treatment.',
  },
]

// Hàm giả lập API Engine Backend check tương tác thực sự dựa trên active ingredients
export const simulateBackendCheckInteractions = (drugIds = []) => {
  const uniqueIds = [...new Set(drugIds.filter(Boolean))]
  if (uniqueIds.length < 2) return []

  const medicines = uniqueIds.map((id) => ({ id, ...MEDICINE_CATALOG.get(id) })).filter((m) => m.activeIngredient)
  const warnings = []

  for (let i = 0; i < medicines.length; i++) {
    for (let j = i + 1; j < medicines.length; j++) {
      const medA = medicines[i]
      const medB = medicines[j]

      const matchedRule = MOCK_INTERACTION_RULES.find(
        (r) =>
          (r.ingredientA.toLowerCase() === medA.activeIngredient.toLowerCase() &&
            r.ingredientB.toLowerCase() === medB.activeIngredient.toLowerCase()) ||
          (r.ingredientB.toLowerCase() === medA.activeIngredient.toLowerCase() &&
            r.ingredientA.toLowerCase() === medB.activeIngredient.toLowerCase()),
      )

      if (matchedRule) {
        warnings.push({
          ruleId: matchedRule.ruleId,
          drugIdA: medA.id,
          drugIdB: medB.id,
          severity: matchedRule.severity,
          description: matchedRule.description,
          clinicalRecommendation: matchedRule.clinicalRecommendation,
        })
      }
    }
  }

  // Sắp xếp mức độ ưu tiên giảm dần: CONTRAINDICATED > SEVERE > MODERATE > MILD
  const severityRank = { CONTRAINDICATED: 4, SEVERE: 3, MODERATE: 2, MILD: 1 }
  warnings.sort((a, b) => (severityRank[b.severity] || 0) - (severityRank[a.severity] || 0))
  return warnings
}

// Helper chuẩn hóa hiển thị UI
export const processInteractionCheckResult = ({ responseBody = [], medicinesMap = MEDICINE_CATALOG }) => {
  return responseBody.map((warning) => {
    const medA = medicinesMap.get(String(warning.drugIdA))
    const medB = medicinesMap.get(String(warning.drugIdB))

    return {
      ruleId: warning.ruleId,
      drugIdA: warning.drugIdA,
      drugIdB: warning.drugIdB,
      drugNameA: medA?.medicineName || warning.drugIdA,
      drugNameB: medB?.medicineName || warning.drugIdB,
      severity: warning.severity || 'MILD',
      severityLabel: {
        CONTRAINDICATED: 'Chống chỉ định',
        SEVERE: 'Nghiêm trọng',
        MODERATE: 'Trung bình',
        MILD: 'Nhẹ',
      }[warning.severity] || 'Cảnh báo',
      description: warning.description || '',
      clinicalRecommendation: warning.clinicalRecommendation || null,
    }
  })
}

// Helper kiểm tra validation lý do bỏ qua cảnh báo
export const validateInteractionOverrideReason = (overrideReason = '') => {
  const trimmed = String(overrideReason || '').trim()
  if (!trimmed) {
    return {
      isValid: false,
      error: 'Bắt buộc nhập lý do chuyên môn để xác nhận bỏ qua cảnh báo tương tác thuốc.',
    }
  }
  return {
    isValid: true,
    error: null,
    overrideReason: trimmed,
  }
}

// Helper chuẩn hóa payload override gửi lên Backend
export const buildInteractionOverridesPayload = (warnings = [], overrideReason = '') => {
  const validation = validateInteractionOverrideReason(overrideReason)
  if (!validation.isValid) {
    throw new Error(validation.error)
  }

  return warnings.map((warning) => ({
    ruleId: warning.ruleId,
    overrideReason: validation.overrideReason,
  }))
}

// =========================================================================
// SUITE KIỂM THỬ CÁC TRƯỜNG HỢP THUỐC CỤ THỂ (SPECIFIC DRUG CASES)
// =========================================================================

test('TRƯỜNG HỢP 1: Cặp thuốc AN TOÀN (Paracetamol + Cefuroxime) -> Không hiển thị cảnh báo', () => {
  const drugIds = [
    '16000000-0000-0000-0000-000000000001', // Paracetamol 500 mg
    '16000000-0000-0000-0000-000000000006', // Cefuroxime 500 mg
  ]
  const rawResponse = simulateBackendCheckInteractions(drugIds)
  const warnings = processInteractionCheckResult({ responseBody: rawResponse })

  assert.equal(warnings.length, 0, 'Cặp thuốc an toàn không được có cảnh báo giả')
})

test('TRƯỜNG HỢP 2: Cặp CHỐNG CHỈ ĐỊNH (Diclofenac + Warfarin) -> Cảnh báo nguy cơ xuất huyết', () => {
  const drugIds = [
    '16000000-0000-0000-0000-000000000021', // Diclofenac 50 mg
    '16000000-0000-0000-0000-000000000029', // Warfarin 2 mg
  ]
  const rawResponse = simulateBackendCheckInteractions(drugIds)
  const warnings = processInteractionCheckResult({ responseBody: rawResponse })

  assert.equal(warnings.length, 1)
  assert.equal(warnings[0].severity, 'CONTRAINDICATED')
  assert.equal(warnings[0].severityLabel, 'Chống chỉ định')
  assert.equal(warnings[0].drugNameA, 'Diclofenac 50 mg')
  assert.equal(warnings[0].drugNameB, 'Warfarin 2 mg')
  assert.match(warnings[0].description, /bleeding risk/)
  assert.match(warnings[0].clinicalRecommendation, /Paracetamol/)
})

test('TRƯỜNG HỢP 3: Cặp NGHIÊM TRỌNG (Azithromycin + Levofloxacin) -> Cảnh báo kéo dài khoảng QT tim', () => {
  const drugIds = [
    '16000000-0000-0000-0000-000000000005', // Azithromycin 500 mg
    '16000000-0000-0000-0000-000000000028', // Levofloxacin 500 mg
  ]
  const rawResponse = simulateBackendCheckInteractions(drugIds)
  const warnings = processInteractionCheckResult({ responseBody: rawResponse })

  assert.equal(warnings.length, 1)
  assert.equal(warnings[0].severity, 'SEVERE')
  assert.equal(warnings[0].severityLabel, 'Nghiêm trọng')
  assert.equal(warnings[0].drugNameA, 'Azithromycin 500 mg')
  assert.equal(warnings[0].drugNameB, 'Levofloxacin 500 mg')
  assert.match(warnings[0].description, /QT prolongation risk/)
})

test('TRƯỜNG HỢP 4: Cặp TRÙNG LẶP THUỐC (Amoxicillin + Amoxicillin Clavulanate) -> Cảnh báo trùng lặp Penicillin', () => {
  const drugIds = [
    '16000000-0000-0000-0000-000000000003', // Amoxicillin 500 mg
    '16000000-0000-0000-0000-000000000004', // Amoxicillin Clavulanate 625 mg
  ]
  const rawResponse = simulateBackendCheckInteractions(drugIds)
  const warnings = processInteractionCheckResult({ responseBody: rawResponse })

  assert.equal(warnings.length, 1)
  assert.equal(warnings[0].severity, 'MODERATE')
  assert.equal(warnings[0].severityLabel, 'Trung bình')
  assert.match(warnings[0].description, /duplicates penicillin therapy/)
})

test('TRƯỜNG HỢP 5: Cặp TRÙNG LẶP THUỐC DẠ DÀY (Omeprazole + Esomeprazole) -> Cảnh báo trùng lặp PPI', () => {
  const drugIds = [
    '16000000-0000-0000-0000-000000000007', // Omeprazole 20 mg
    '16000000-0000-0000-0000-000000000008', // Esomeprazole 40 mg
  ]
  const rawResponse = simulateBackendCheckInteractions(drugIds)
  const warnings = processInteractionCheckResult({ responseBody: rawResponse })

  assert.equal(warnings.length, 1)
  assert.equal(warnings[0].severity, 'MODERATE')
  assert.match(warnings[0].description, /proton pump inhibitor/)
})

test('TRƯỜNG HỢP 6: Cặp TRÙNG LẶP DỊ ỨNG (Cetirizine + Loratadine) -> Cảnh báo trùng lặp H1-Antihistamine', () => {
  const drugIds = [
    '16000000-0000-0000-0000-000000000009', // Cetirizine 10 mg
    '16000000-0000-0000-0000-000000000010', // Loratadine 10 mg
  ]
  const rawResponse = simulateBackendCheckInteractions(drugIds)
  const warnings = processInteractionCheckResult({ responseBody: rawResponse })

  assert.equal(warnings.length, 1)
  assert.equal(warnings[0].severity, 'MODERATE')
  assert.match(warnings[0].description, /H1-antihistamine/)
})

test('TRƯỜNG HỢP 7: Cặp HẠ ĐƯỜNG HUYẾT (Metformin + Gliclazide) -> Cảnh báo nguy cơ hạ đường huyết nhẹ', () => {
  const drugIds = [
    '16000000-0000-0000-0000-000000000016', // Metformin 500 mg
    '16000000-0000-0000-0000-000000000017', // Gliclazide MR 30 mg
  ]
  const rawResponse = simulateBackendCheckInteractions(drugIds)
  const warnings = processInteractionCheckResult({ responseBody: rawResponse })

  assert.equal(warnings.length, 1)
  assert.equal(warnings[0].severity, 'MILD')
  assert.equal(warnings[0].severityLabel, 'Nhẹ')
  assert.match(warnings[0].description, /hypoglycemia risk/)
})

test('TRƯỜNG HỢP 8: ĐƠN PHỐI HỢP NHIỀU THUỐC (3+ thuốc) -> Phát hiện đồng thời nhiều cặp tương tác & sắp xếp ưu tiên', () => {
  const multiDrugIds = [
    '16000000-0000-0000-0000-000000000016', // Metformin
    '16000000-0000-0000-0000-000000000017', // Gliclazide (Tương tác với Metformin -> MILD)
    '16000000-0000-0000-0000-000000000021', // Diclofenac
    '16000000-0000-0000-0000-000000000029', // Warfarin (Tương tác với Diclofenac -> CONTRAINDICATED)
  ]
  const rawResponse = simulateBackendCheckInteractions(multiDrugIds)
  const warnings = processInteractionCheckResult({ responseBody: rawResponse })

  assert.equal(warnings.length, 2, 'Phát hiện 2 cặp tương tác độc lập trong đơn 4 thuốc')
  assert.equal(warnings[0].severity, 'CONTRAINDICATED', 'Ưu tiên hiển thị tương tác CHỐNG CHỈ ĐỊNH lên trên cùng')
  assert.equal(warnings[1].severity, 'MILD', 'Tương tác nhẹ nằm ở phía dưới')
})

test('TRƯỜNG HỢP 9: THAY ĐỔI ĐƠN KHI ĐIỀU CHỈNH -> Thay đổi thuốc hết tương tác thì cảnh báo biến mất', () => {
  // Đơn ban đầu có 2 thuốc tương tác (Diclofenac + Warfarin)
  let initialDrugIds = [
    '16000000-0000-0000-0000-000000000021', // Diclofenac 50 mg
    '16000000-0000-0000-0000-000000000029', // Warfarin 2 mg
  ]
  let response = simulateBackendCheckInteractions(initialDrugIds)
  assert.equal(response.length, 1, 'Đơn gốc có cảnh báo xuất huyết')

  // Bác sĩ đổi Diclofenac sang Paracetamol (theo đúng khuyến nghị lâm sàng)
  let adjustedDrugIds = [
    '16000000-0000-0000-0000-000000000001', // Paracetamol 500 mg
    '16000000-0000-0000-0000-000000000029', // Warfarin 2 mg
  ]
  response = simulateBackendCheckInteractions(adjustedDrugIds)
  assert.equal(response.length, 0, 'Đổi sang Paracetamol không còn tương tác, cảnh báo biến mất')
})

test('TRƯỜNG HỢP 10: XÁC NHẬN BỎ QUA CẢNH BÁO -> Phải truyền đúng ruleId và lý do chuyên môn không rỗng', () => {
  const drugIds = [
    '16000000-0000-0000-0000-000000000005', // Azithromycin
    '16000000-0000-0000-0000-000000000028', // Levofloxacin
  ]
  const rawResponse = simulateBackendCheckInteractions(drugIds)
  const warnings = processInteractionCheckResult({ responseBody: rawResponse })

  const reason = 'Bệnh nhân nhiễm trùng hô hấp nặng dai dẳng, đã được làm ECG theo dõi khoảng QTc.'
  const payload = buildInteractionOverridesPayload(warnings, reason)

  assert.equal(payload.length, 1)
  assert.equal(payload[0].ruleId, '17100000-0000-0000-0000-000000000002')
  assert.equal(payload[0].overrideReason, reason)
})
