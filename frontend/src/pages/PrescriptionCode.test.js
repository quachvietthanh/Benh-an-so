import test from 'node:test'
import assert from 'node:assert/strict'

/**
 * Electronic Prescription Code (Mã đơn thuốc điện tử) helper validation rules
 */
export const isValidElectronicPrescriptionCode = (code) => {
  if (!code || typeof code !== 'string') return false
  const trimmed = code.trim()
  // Electronic prescription codes adhere to standard prefix RX + sequence (min 6 digits)
  // or official national electronic prescription format
  return /^RX\d{6,}$/.test(trimmed) || /^(DT|RX)[\w-]{6,}$/.test(trimmed)
}

export const preservePrescriptionCodeOnAmend = (originalPrescription, amendedPayload) => {
  if (!originalPrescription?.prescriptionCode) {
    throw new Error('Original prescription must have a fixed electronic prescription code.')
  }
  return {
    ...amendedPayload,
    id: originalPrescription.id,
    prescriptionCode: originalPrescription.prescriptionCode, // Immutable prescription code
    medicalRecordId: originalPrescription.medicalRecordId,
  }
}

export const filterPrescriptionsByCodeOrKeyword = (prescriptions = [], query = '') => {
  const q = (query || '').trim().toLowerCase()
  if (!q) return prescriptions
  return prescriptions.filter((p) => {
    const code = String(p.prescriptionCode || '').toLowerCase()
    const doctor = String(p.doctorName || '').toLowerCase()
    const patient = String(p.patientName || '').toLowerCase()
    const medNames = (p.items || []).map((i) => String(i.medicineName || '').toLowerCase()).join(' ')
    return code.includes(q) || doctor.includes(q) || patient.includes(q) || medNames.includes(q)
  })
}

export const formatPrescriptionPdfDownloadName = (prescription) => {
  const code = prescription?.prescriptionCode || prescription?.id || 'unknown'
  return `prescription-${code}.pdf`
}

export const validateElectronicPrescriptionRequirements = ({
  medicalRecordId,
  diagnoses = [],
  items = [],
  doctorName,
}) => {
  const errors = []
  if (!medicalRecordId) errors.push('Mã bệnh án là bắt buộc để cấp đơn thuốc điện tử.')
  if (!diagnoses || diagnoses.length === 0) errors.push('Cần ít nhất một chẩn đoán bệnh chính để cấp mã đơn điện tử.')
  if (!items || items.length === 0) errors.push('Đơn thuốc điện tử phải có ít nhất một mục thuốc.')
  
  items.forEach((item, index) => {
    if (!item.medicineId) errors.push(`Thuốc dòng ${index + 1}: chưa chọn thuốc.`)
    if (!item.quantity || Number(item.quantity) <= 0) errors.push(`Thuốc dòng ${index + 1}: số lượng phải lớn hơn 0.`)
    if (!item.dosage || !String(item.dosage).trim()) errors.push(`Thuốc dòng ${index + 1}: liều dùng là bắt buộc.`)
    if (item.frequency === undefined || item.frequency === null || item.frequency === '') {
      errors.push(`Thuốc dòng ${index + 1}: tần suất dùng là bắt buộc.`)
    }
    if (!item.route) errors.push(`Thuốc dòng ${index + 1}: đường dùng là bắt buộc.`)
    if (!item.durationDays || Number(item.durationDays) <= 0) {
      errors.push(`Thuốc dòng ${index + 1}: số ngày dùng phải lớn hơn 0.`)
    }
  })

  return {
    isValid: errors.length === 0,
    errors,
  }
}

// ==========================================
// TEST SUITE: Cấp mã đơn thuốc điện tử
// ==========================================

test('1. Cấp mã đơn thuốc điện tử: Kiểm tra định dạng mã chuẩn định danh duy nhất', () => {
  assert.equal(isValidElectronicPrescriptionCode('RX000001'), true)
  assert.equal(isValidElectronicPrescriptionCode('RX000010'), true)
  assert.equal(isValidElectronicPrescriptionCode('RX999999'), true)
  assert.equal(isValidElectronicPrescriptionCode('DT-123456'), true)

  assert.equal(isValidElectronicPrescriptionCode(''), false)
  assert.equal(isValidElectronicPrescriptionCode(null), false)
  assert.equal(isValidElectronicPrescriptionCode('123'), false)
  assert.equal(isValidElectronicPrescriptionCode('RX-12'), false)
})

test('2. Cấp mã đơn thuốc điện tử: Kiểm tra tính bất biến (immutability) của mã đơn khi điều chỉnh', () => {
  const initialPrescription = {
    id: 'f47ac10b-58cc-4372-a567-0e02b2c3d479',
    prescriptionCode: 'RX000008',
    medicalRecordId: 'm-1001',
    status: 'PENDING_DISPENSE',
    items: [{ medicineId: 'm-1', quantity: 10, dosage: '1 viên' }],
  }

  const amendRequest = {
    changeReason: 'Tăng liều điều trị',
    note: 'Uống sau bữa ăn sáng',
    items: [{ medicineId: 'm-1', quantity: 20, dosage: '2 viên' }],
  }

  const result = preservePrescriptionCodeOnAmend(initialPrescription, amendRequest)

  assert.equal(result.id, initialPrescription.id)
  assert.equal(result.prescriptionCode, 'RX000008')
  assert.equal(result.medicalRecordId, 'm-1001')
  assert.equal(result.items[0].quantity, 20)
})

test('3. Cấp mã đơn thuốc điện tử: Tra cứu chính xác đơn thuốc theo mã điện tử', () => {
  const sampleList = [
    { id: '1', prescriptionCode: 'RX000001', patientName: 'Nguyễn Văn An', doctorName: 'BS. Lê Minh', items: [{ medicineName: 'Paracetamol 500mg' }] },
    { id: '2', prescriptionCode: 'RX000002', patientName: 'Trần Thị Bình', doctorName: 'BS. Phạm Hùng', items: [{ medicineName: 'Amoxicillin 500mg' }] },
    { id: '3', prescriptionCode: 'RX000003', patientName: 'Lê Hoàng Cúc', doctorName: 'BS. Lê Minh', items: [{ medicineName: 'Metformin 500mg' }] },
  ]

  // Search by exact code
  const res1 = filterPrescriptionsByCodeOrKeyword(sampleList, 'RX000002')
  assert.equal(res1.length, 1)
  assert.equal(res1[0].prescriptionCode, 'RX000002')
  assert.equal(res1[0].patientName, 'Trần Thị Bình')

  // Search by partial code
  const res2 = filterPrescriptionsByCodeOrKeyword(sampleList, '000003')
  assert.equal(res2.length, 1)
  assert.equal(res2[0].prescriptionCode, 'RX000003')

  // Search by medicine name
  const res3 = filterPrescriptionsByCodeOrKeyword(sampleList, 'Paracetamol')
  assert.equal(res3.length, 1)
  assert.equal(res3[0].prescriptionCode, 'RX000001')

  // Search by doctor
  const res4 = filterPrescriptionsByCodeOrKeyword(sampleList, 'Lê Minh')
  assert.equal(res4.length, 2)
})

test('4. Cấp mã đơn thuốc điện tử: Định dạng file xuất/in PDF gắn liền với mã đơn điện tử', () => {
  const prescription = {
    id: 'c1000000-0000-0000-0000-000000000001',
    prescriptionCode: 'RX000007',
  }
  const fileName = formatPrescriptionPdfDownloadName(prescription)
  assert.equal(fileName, 'prescription-RX000007.pdf')
})

test('5. Cấp mã đơn thuốc điện tử: Kiểm tra đầy đủ các trường bắt buộc để cấp mã đơn', () => {
  const validPayload = {
    medicalRecordId: 'rec-001',
    diagnoses: [{ code: 'J00', name: 'Viêm mũi họng cấp' }],
    doctorName: 'BS. Hoàng Nam',
    items: [
      {
        medicineId: 'med-01',
        quantity: 20,
        dosage: '1 viên',
        frequency: 2,
        route: 'ORAL',
        durationDays: 10,
      },
    ],
  }

  const validCheck = validateElectronicPrescriptionRequirements(validPayload)
  assert.equal(validCheck.isValid, true)
  assert.equal(validCheck.errors.length, 0)

  // Missing diagnoses and missing dosage
  const invalidPayload = {
    medicalRecordId: 'rec-001',
    diagnoses: [],
    items: [
      {
        medicineId: 'med-01',
        quantity: 0,
        dosage: '',
        frequency: null,
        route: '',
        durationDays: 0,
      },
    ],
  }
  const invalidCheck = validateElectronicPrescriptionRequirements(invalidPayload)
  assert.equal(invalidCheck.isValid, false)
  assert.ok(invalidCheck.errors.some((e) => e.includes('chẩn đoán')))
  assert.ok(invalidCheck.errors.some((e) => e.includes('số lượng')))
  assert.ok(invalidCheck.errors.some((e) => e.includes('liều dùng')))
})
