import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildClinicalOrderPayload,
  buildDiagnosisPayload,
  buildFefoPreview,
  buildMedicalRecordPayload,
  getQueueInProgressBlockReason,
  normalizeMedicalRecordDetail,
} from './workflowContract.js'

test('medical-record payload uses visitId and backend narrative fields', () => {
  const payload = buildMedicalRecordPayload({
    visitId: 'visit-1',
    values: { symptoms: 'Đau đầu', examinationNote: 'Tim đều', treatmentPlan: 'Theo dõi' },
    vitalSigns: { bp: '120/80', pulse: '72' },
  })

  assert.equal(payload.visitId, 'visit-1')
  assert.equal('patientId' in payload, false)
  assert.match(payload.physicalExamination, /Huyết áp 120\/80/)
})

test('diagnosis and clinical-order payloads use backend UUID fields', () => {
  const diagnosis = buildDiagnosisPayload({
    primaryDiagnosis: { id: 'catalog-1', code: 'J00', name: 'Viêm mũi họng cấp' },
    secondaryDiagnoses: [{ id: 'catalog-2', code: 'K29', name: 'Viêm dạ dày' }],
  })
  assert.equal(diagnosis.primaryDiagnosis.diagnosisCatalogId, 'catalog-1')
  assert.equal(diagnosis.secondaryDiagnoses[0].diagnosisCatalogId, 'catalog-2')
  assert.equal('name' in diagnosis.secondaryDiagnoses[0], false)

  const freeTextDiagnosis = buildDiagnosisPayload({
    primaryDiagnosis: { id: 'catalog-1', code: 'J00', name: 'Viêm mũi họng cấp' },
    secondaryDiagnoses: [{ name: 'Bệnh lý phụ khác' }],
  })
  assert.equal('diagnosisCatalogId' in freeTextDiagnosis.secondaryDiagnoses[0], false)
  assert.equal(freeTextDiagnosis.secondaryDiagnoses[0].name, 'Bệnh lý phụ khác')

  const order = buildClinicalOrderPayload({
    clinicalReason: 'Theo dõi',
    orders: [{ id: 'service-uuid', code: 'XN-01', note: 'Nhịn ăn' }],
  })
  assert.deepEqual(order.items, [{ serviceId: 'service-uuid', instruction: 'Nhịn ăn' }])
})

test('medical-record detail is normalized without inventing a list-all shape', () => {
  const record = normalizeMedicalRecordDetail({
    medicalRecordId: 'record-1',
    patient: { id: 'patient-1', patientCode: 'BN001', fullName: 'Nguyễn Văn A' },
    visit: { id: 'visit-1', visitCode: 'LK001', doctorName: 'BS. B' },
    diagnoses: [{ diagnosisType: 'PRIMARY', diagnosisCode: 'J00', diagnosisName: 'Cảm lạnh' }],
  })

  assert.equal(record.id, 'record-1')
  assert.equal(record.visitId, 'visit-1')
  assert.equal(record.patientName, 'Nguyễn Văn A')
  assert.equal(record.diagnosis, '[J00] Cảm lạnh')
})

test('FEFO preview allocates earliest eligible batches and reports shortage', () => {
  const preview = buildFefoPreview(
    [{ medicineId: 'medicine-1', medicineName: 'Thuốc A', quantity: 8 }],
    [
      { batchId: 'late', medicineId: 'medicine-1', batchNumber: 'L2', expiryDate: '2027-02-01', quantity: 4, eligibleForDispense: true },
      { batchId: 'early', medicineId: 'medicine-1', batchNumber: 'L1', expiryDate: '2027-01-01', quantity: 3, eligibleForDispense: true },
    ],
  )[0]

  assert.deepEqual(preview.allocations.map((item) => item.batchId), ['early', 'late'])
  assert.equal(preview.availableQuantity, 7)
  assert.equal(preview.shortageQuantity, 1)
})

test('WAITING_FOR_RESULT blocks prescribing, locking, and completion until queue resumes', () => {
  const waitingReason = getQueueInProgressBlockReason(
    { id: 'queue-item-1', status: 'WAITING_FOR_RESULT' },
    'kê đơn, khóa bệnh án hoặc hoàn tất lượt khám',
  )

  assert.match(waitingReason, /WAITING_FOR_RESULT/)
  assert.match(waitingReason, /IN_PROGRESS/)
  assert.equal(
    getQueueInProgressBlockReason(
      { id: 'queue-item-1', status: 'IN_PROGRESS' },
      'kê đơn, khóa bệnh án hoặc hoàn tất lượt khám',
    ),
    null,
  )
  assert.match(
    getQueueInProgressBlockReason(null, 'kê đơn'),
    /Không có queueItemId thật/,
  )
  assert.match(
    getQueueInProgressBlockReason({ id: 'queue-item-1', status: 'COMPLETED' }, 'kê đơn'),
    /hiện tại: COMPLETED/,
  )
})
