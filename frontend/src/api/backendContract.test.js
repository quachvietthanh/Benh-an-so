import test from 'node:test'
import assert from 'node:assert/strict'

import axiosClient from './axiosClient.js'
import appointmentApi from './appointmentApi.js'
import queueApi from './queueApi.js'
import patientApi from './patientApi.js'
import medicalRecordApi from './medicalRecordApi.js'
import clinicalOrderApi from './clinicalOrderApi.js'
import clinicalResultApi from './clinicalResultApi.js'
import pharmacyApi from './pharmacyApi.js'
import authApi from './authApi.js'

axiosClient.defaults.adapter = async (config) => ({
  data: {},
  status: 200,
  statusText: 'OK',
  headers: {},
  config,
})

const requestBody = (response) => (
  typeof response.config.data === 'string'
    ? JSON.parse(response.config.data)
    : response.config.data
)

test('appointment requests follow backend paths and DTO fields', async () => {
  const created = await appointmentApi.create({
    patientId: 'patient-id',
    doctorId: 'doctor-id',
    startTime: '2026-08-07T08:00:00',
    endTime: '2026-08-07T08:30:00',
    reason: 'Khám',
    status: 'SCHEDULED',
  })
  assert.equal(created.config.url, '/appointments')
  assert.equal(created.config.method, 'post')
  assert.deepEqual(requestBody(created), {
    patientId: 'patient-id',
    doctorId: 'doctor-id',
    startTime: '2026-08-07T08:00:00',
    endTime: '2026-08-07T08:30:00',
    reason: 'Khám',
  })

  const cancelled = await appointmentApi.cancel('appointment-id', 'Bệnh nhân yêu cầu')
  assert.equal(cancelled.config.url, '/appointments/appointment-id/cancel')
  assert.deepEqual(requestBody(cancelled), { cancelReason: 'Bệnh nhân yêu cầu' })
})

test('queue requests discard unsupported query and body properties', async () => {
  const queues = await queueApi.getQueues({
    date: '2026-08-07',
    doctorId: 'doctor-id',
    roomId: 'room-id',
    status: 'WAITING',
    sourceType: 'WALK_IN',
  })
  assert.deepEqual(queues.config.params, {
    date: '2026-08-07',
    doctorId: 'doctor-id',
    roomId: 'room-id',
  })

  const updated = await queueApi.updateStatus('item-id', { status: 'CANCELLED', cancelReason: 'Nhầm lượt' })
  assert.equal(updated.config.url, '/queue-items/item-id/status')
  assert.deepEqual(requestBody(updated), { targetStatus: 'CANCELLED', cancelReason: 'Nhầm lượt' })
})

test('patient paging and medical record routes match backend contracts', async () => {
  const patients = await patientApi.getAll({ fullName: 'An', page: -2, size: 500, keyword: 'ignored' })
  assert.deepEqual(patients.config.params, { fullName: 'An', page: 0, size: 100 })

  const record = await medicalRecordApi.create({
    visitId: 'visit-id',
    chiefComplaint: 'Đau đầu',
    patientId: 'ignored',
  })
  assert.deepEqual(requestBody(record), { visitId: 'visit-id', chiefComplaint: 'Đau đầu' })

  const diagnoses = await medicalRecordApi.replaceDiagnoses('record-id', { primaryDiagnosis: { code: 'R51' } })
  assert.equal(diagnoses.config.url, '/medical-records/record-id/diagnoses')
  assert.equal(diagnoses.config.method, 'put')
})

test('clinical order and result clients use visit and order-item endpoints', async () => {
  const order = await clinicalOrderApi.create('visit-id', {
    clinicalReason: 'Kiểm tra',
    doctorId: 'ignored',
    items: [{ serviceId: 'service-id', instruction: 'Nhịn ăn', price: 100000 }],
  })
  assert.equal(order.config.url, '/clinical-orders/visits/visit-id')
  assert.deepEqual(requestBody(order), {
    clinicalReason: 'Kiểm tra',
    items: [{ serviceId: 'service-id', instruction: 'Nhịn ăn' }],
  })

  const result = await clinicalResultApi.enter('order-item-id', {
    numericValue: 5.2,
    abnormalFlag: 'NORMAL',
    unit: 'mmol/L',
  })
  assert.equal(result.config.url, '/clinical-order-items/order-item-id/results')
  assert.deepEqual(requestBody(result), { numericValue: 5.2, abnormalFlag: 'NORMAL' })
})

test('prescription and authentication payloads match backend DTOs', async () => {
  const prescription = await pharmacyApi.createPrescription({
    medicalRecordId: 'record-id',
    note: 'Sau ăn',
    items: [{
      medicineId: 'medicine-id',
      dosage: '1 viên',
      frequency: '2 lần/ngày',
      route: 'ORAL',
      durationDays: 5,
      quantity: 10,
      instructions: 'Uống sau ăn',
      medicineName: 'ignored',
    }],
    interactionOverrides: [
      { ruleId: 'not-a-uuid', overrideReason: 'ignored' },
      { ruleId: '11111111-1111-4111-8111-111111111111', overrideReason: 'Đã cân nhắc' },
    ],
  })
  assert.equal(prescription.config.url, '/prescriptions')
  assert.deepEqual(requestBody(prescription), {
    medicalRecordId: 'record-id',
    note: 'Sau ăn',
    items: [{
      medicineId: 'medicine-id',
      dosage: '1 viên',
      frequency: '2 lần/ngày',
      route: 'ORAL',
      durationDays: 5,
      quantity: 10,
      instructions: 'Uống sau ăn',
    }],
    interactionOverrides: [
      { ruleId: '11111111-1111-4111-8111-111111111111', overrideReason: 'Đã cân nhắc' },
    ],
  })

  const refreshed = await authApi.refresh('refresh-token')
  assert.equal(refreshed.config.url, '/auth/refresh')
  assert.deepEqual(requestBody(refreshed), { refreshToken: 'refresh-token' })
})
