import test from 'node:test'
import assert from 'node:assert/strict'
import {
  MIN_YEAR_DETECTED,
  getCurrentYear,
  validateYearDetected,
  mapChronicDiseaseErrorMessage,
  hasChronicDiseaseWritePermission,
  hasChronicDiseaseReadPermission,
  formatChronicDiseaseLabel,
  formatYearDetected,
} from './chronicDiseaseHelpers.js'
import { patientChronicDiseaseApi } from '../api/patientChronicDiseaseApi.js'
import axiosClient from '../api/axiosClient.js'

test('validateYearDetected: allows null, undefined, or empty string (optional)', () => {
  assert.equal(validateYearDetected(null).valid, true)
  assert.equal(validateYearDetected(undefined).valid, true)
  assert.equal(validateYearDetected('').valid, true)
  assert.equal(validateYearDetected(null).value, null)
})

test('validateYearDetected: validates year correctly within [1900, currentYear]', () => {
  const currentYear = 2026

  // Valid years
  assert.equal(validateYearDetected(1900, currentYear).valid, true)
  assert.equal(validateYearDetected(2018, currentYear).valid, true)
  assert.equal(validateYearDetected(2026, currentYear).valid, true)
  assert.equal(validateYearDetected('2020', currentYear).valid, true)
  assert.equal(validateYearDetected('2020', currentYear).value, 2020)

  // Block year < 1900
  const underMin = validateYearDetected(1899, currentYear)
  assert.equal(underMin.valid, false)
  assert.ok(underMin.error.includes('1900'))

  // Block year > currentYear
  const overMax = validateYearDetected(2027, currentYear)
  assert.equal(overMax.valid, false)
  assert.ok(overMax.error.includes('2026'))

  // Block non-integer
  const nonInt = validateYearDetected(2020.5, currentYear)
  assert.equal(nonInt.valid, false)
  assert.ok(nonInt.error.includes('số nguyên'))
})

test('mapChronicDiseaseErrorMessage: maps DIAGNOSIS_CATALOG_NOT_FOUND', () => {
  const err = {
    response: {
      status: 404,
      data: { code: 'DIAGNOSIS_CATALOG_NOT_FOUND', message: 'Not found' },
    },
  }
  const msg = mapChronicDiseaseErrorMessage(err)
  assert.equal(msg, 'Mã bệnh không tồn tại trong danh mục chẩn đoán.')
})

test('mapChronicDiseaseErrorMessage: maps PATIENT_CHRONIC_DISEASE_ALREADY_EXISTS', () => {
  const err = {
    response: {
      status: 409,
      data: { code: 'PATIENT_CHRONIC_DISEASE_ALREADY_EXISTS', message: 'Already exists' },
    },
  }
  const msg = mapChronicDiseaseErrorMessage(err)
  assert.equal(
    msg,
    'Bệnh nhân đã được ghi nhận bệnh này từ trước và vẫn đang trong danh sách tiền sử hiện tại.',
  )
})

test('mapChronicDiseaseErrorMessage: maps VALIDATION_FAILED', () => {
  const err = {
    response: {
      status: 400,
      data: { code: 'VALIDATION_FAILED', message: 'Invalid data' },
    },
  }
  const msg = mapChronicDiseaseErrorMessage(err)
  assert.equal(
    msg,
    'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại năm phát hiện hoặc mã bệnh đã chọn.',
  )
})

test('mapChronicDiseaseErrorMessage: maps 403 Forbidden', () => {
  const err = {
    response: {
      status: 403,
      data: { code: 'FORBIDDEN', message: 'Access denied' },
    },
  }
  const msg = mapChronicDiseaseErrorMessage(err)
  assert.equal(msg, 'Bạn không có quyền ghi nhận tiền sử bệnh mạn tính.')
})

test('mapChronicDiseaseErrorMessage: handles PATIENT_NOT_FOUND and PATIENT_CHRONIC_DISEASE_NOT_FOUND', () => {
  const errPatient = {
    response: {
      status: 404,
      data: { code: 'PATIENT_NOT_FOUND' },
    },
  }
  assert.equal(mapChronicDiseaseErrorMessage(errPatient), 'Không tìm thấy hồ sơ bệnh nhân trong hệ thống.')

  const errDisease = {
    response: {
      status: 404,
      data: { code: 'PATIENT_CHRONIC_DISEASE_NOT_FOUND' },
    },
  }
  assert.equal(
    mapChronicDiseaseErrorMessage(errDisease),
    'Không tìm thấy bản ghi tiền sử bệnh mạn tính hoặc đã bị xóa trước đó.',
  )

  const errResourceNotFound = {
    response: {
      status: 404,
      data: { code: 'RESOURCE_NOT_FOUND', message: 'Resource not found.' },
    },
  }
  assert.ok(
    mapChronicDiseaseErrorMessage(errResourceNotFound).includes('khởi động lại tiến trình Backend Spring Boot'),
  )
})

test('mapChronicDiseaseErrorMessage: falls back to custom default message', () => {
  const err = new Error('Network error')
  const msg = mapChronicDiseaseErrorMessage(err, 'Lỗi kết nối tùy biến.')
  assert.equal(msg, 'Lỗi kết nối tùy biến.')
})

test('hasChronicDiseaseWritePermission: checks WRITE permission correctly', () => {
  // Doctor role
  assert.equal(hasChronicDiseaseWritePermission({ roles: ['doctor'] }), true)
  assert.equal(hasChronicDiseaseWritePermission({ roles: ['ROLE_DOCTOR'] }), true)

  // Admin role
  assert.equal(hasChronicDiseaseWritePermission({ roles: ['admin'] }), true)
  assert.equal(hasChronicDiseaseWritePermission({ roles: ['ROLE_ADMIN'] }), true)

  // Explicit permission
  assert.equal(
    hasChronicDiseaseWritePermission({ permissions: ['PATIENT_CHRONIC_DISEASE_WRITE'] }),
    true,
  )

  // Pharmacist or Manager should NOT have write permission
  assert.equal(hasChronicDiseaseWritePermission({ roles: ['pharmacist'] }), false)
  assert.equal(hasChronicDiseaseWritePermission({ roles: ['manager'] }), false)
  assert.equal(hasChronicDiseaseWritePermission({ roles: ['receptionist'] }), false)
  assert.equal(hasChronicDiseaseWritePermission(null), false)
})

test('hasChronicDiseaseReadPermission: checks READ permission correctly', () => {
  // Admin, Doctor, Pharmacist, Manager have read permission
  assert.equal(hasChronicDiseaseReadPermission({ roles: ['admin'] }), true)
  assert.equal(hasChronicDiseaseReadPermission({ roles: ['doctor'] }), true)
  assert.equal(hasChronicDiseaseReadPermission({ roles: ['pharmacist'] }), true)
  assert.equal(hasChronicDiseaseReadPermission({ roles: ['manager'] }), true)
  assert.equal(hasChronicDiseaseReadPermission({ roles: ['clinic_manager'] }), true)

  // Explicit permission
  assert.equal(
    hasChronicDiseaseReadPermission({ permissions: ['PATIENT_CHRONIC_DISEASE_READ'] }),
    true,
  )

  // Receptionist without read permission
  assert.equal(hasChronicDiseaseReadPermission({ roles: ['receptionist'] }), false)
  assert.equal(hasChronicDiseaseReadPermission(null), false)
})

test('formatChronicDiseaseLabel and formatYearDetected: formats correctly', () => {
  assert.equal(
    formatChronicDiseaseLabel({ diagnosisCode: 'E11.9', diagnosisName: 'Đái tháo đường type 2' }),
    'E11.9 - Đái tháo đường type 2',
  )
  assert.equal(formatChronicDiseaseLabel(null), '')
  assert.equal(formatYearDetected(2018), '2018')
  assert.equal(formatYearDetected(null), 'Không rõ')
  assert.equal(formatYearDetected(undefined), 'Không rõ')
  assert.equal(formatYearDetected(''), 'Không rõ')
})

test('patientChronicDiseaseApi: calls correct endpoints and HTTP methods', async () => {
  const patientId = 'p-uuid-1'
  const diseaseId = 'd-uuid-1'
  const originalGet = axiosClient.get
  const originalPost = axiosClient.post
  const originalDelete = axiosClient.delete

  let calledUrl = null
  let calledData = null
  let calledConfig = null

  try {
    axiosClient.get = async (url) => {
      calledUrl = url
      return { data: [{ id: diseaseId }] }
    }
    const listRes = await patientChronicDiseaseApi.list(patientId)
    assert.equal(calledUrl, `/patients/${patientId}/chronic-diseases`)
    assert.equal(listRes.data[0].id, diseaseId)

    axiosClient.post = async (url, data) => {
      calledUrl = url
      calledData = data
      return { data: { id: diseaseId, ...data } }
    }
    const payload = { diagnosisCatalogId: 'cat-1', yearDetected: 2020 }
    const addRes = await patientChronicDiseaseApi.add(patientId, payload)
    assert.equal(calledUrl, `/patients/${patientId}/chronic-diseases`)
    assert.deepEqual(calledData, payload)
    assert.equal(addRes.data.diagnosisCatalogId, 'cat-1')

    axiosClient.delete = async (url, config) => {
      calledUrl = url
      calledConfig = config
      return { status: 204 }
    }
    const deleteRes = await patientChronicDiseaseApi.remove(patientId, diseaseId, 'Chẩn đoán nhầm')
    assert.equal(calledUrl, `/patients/${patientId}/chronic-diseases/${diseaseId}`)
    assert.deepEqual(calledConfig.params, { reason: 'Chẩn đoán nhầm' })
    assert.equal(deleteRes.status, 204)
  } finally {
    axiosClient.get = originalGet
    axiosClient.post = originalPost
    axiosClient.delete = originalDelete
  }
})
