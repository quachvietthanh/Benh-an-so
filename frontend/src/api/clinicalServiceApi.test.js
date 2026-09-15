import assert from 'node:assert/strict'
import test from 'node:test'
import clinicalServiceApi from './clinicalServiceApi.js'
import axiosClient from './axiosClient.js'
import {
  checkOverlapClientSide,
  formatAgeRangeDisplay,
  formatGenderDisplay,
  formatReferenceBoundsDisplay,
  translateClinicalErrorMessage,
  validateReferenceRangePayload,
} from '../utils/clinicalServiceValidation.js'

test('clinicalServiceApi.getCatalog sends GET to /clinical-services with default page and size', async () => {
  let capturedConfig = null
  const dummyResponse = { data: { content: [{ id: '1', serviceCode: 'LAB-GLU', active: true }] } }

  const originalGet = axiosClient.get
  axiosClient.get = async (url, config) => {
    capturedConfig = { url, ...config }
    return dummyResponse
  }

  try {
    const res = await clinicalServiceApi.getCatalog({ keyword: 'glucose' })
    assert.equal(capturedConfig.url, '/clinical-services')
    assert.deepEqual(capturedConfig.params, { page: 0, size: 100, keyword: 'glucose' })
    assert.deepEqual(res.data, dummyResponse.data)
  } finally {
    axiosClient.get = originalGet
  }
})

test('clinicalServiceApi.search passes params to /system/clinical-services', async () => {
  let capturedConfig = null
  const dummyResponse = {
    data: {
      content: [
        { id: '1', serviceCode: 'LAB-GLU', serviceName: 'Blood glucose', active: true, referenceRanges: [] },
      ],
    },
  }

  const originalGet = axiosClient.get
  axiosClient.get = async (url, config) => {
    capturedConfig = { url, ...config }
    return dummyResponse
  }

  try {
    const res = await clinicalServiceApi.search({ keyword: 'GLU', active: true, page: 0, size: 20 })
    assert.equal(capturedConfig.url, '/system/clinical-services')
    assert.deepEqual(capturedConfig.params, { keyword: 'GLU', active: true, page: 0, size: 20 })
    assert.deepEqual(res.data, dummyResponse.data)
  } finally {
    axiosClient.get = originalGet
  }
})

test('clinicalServiceApi.getById fetches detail by UUID', async () => {
  let capturedUrl = null
  const dummyResponse = { data: { id: 'svc-123', serviceCode: 'LAB-GLU' } }

  const originalGet = axiosClient.get
  axiosClient.get = async (url) => {
    capturedUrl = url
    return dummyResponse
  }

  try {
    const res = await clinicalServiceApi.getById('svc-123')
    assert.equal(capturedUrl, '/system/clinical-services/svc-123')
    assert.deepEqual(res.data, dummyResponse.data)
  } finally {
    axiosClient.get = originalGet
  }
})

test('clinicalServiceApi.create sends POST to /system/clinical-services', async () => {
  let capturedUrl = null
  let capturedData = null
  const payload = {
    serviceCatalogId: 'cat-1',
    serviceCode: 'LAB-CBC',
    serviceName: 'Công thức máu',
    serviceType: 'LAB_TEST',
    resultDataType: 'NUMBER',
    unit: '10^9/L',
  }
  const dummyResponse = { data: { id: 'new-id', ...payload } }

  const originalPost = axiosClient.post
  axiosClient.post = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return dummyResponse
  }

  try {
    const res = await clinicalServiceApi.create(payload)
    assert.equal(capturedUrl, '/system/clinical-services')
    assert.deepEqual(capturedData, payload)
    assert.equal(res.data.id, 'new-id')
  } finally {
    axiosClient.post = originalPost
  }
})

test('clinicalServiceApi.update sends PUT to /system/clinical-services/{id}', async () => {
  let capturedUrl = null
  let capturedData = null
  const payload = { serviceName: 'Tên mới', serviceType: 'LAB_TEST', resultDataType: 'NUMBER' }
  const dummyResponse = { data: { id: 'svc-1', ...payload } }

  const originalPut = axiosClient.put
  axiosClient.put = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return dummyResponse
  }

  try {
    const res = await clinicalServiceApi.update('svc-1', payload)
    assert.equal(capturedUrl, '/system/clinical-services/svc-1')
    assert.deepEqual(capturedData, payload)
    assert.equal(res.data.serviceName, 'Tên mới')
  } finally {
    axiosClient.put = originalPut
  }
})

test('clinicalServiceApi.updateStatus sends PATCH to /system/clinical-services/{id}/status', async () => {
  let capturedUrl = null
  let capturedData = null
  const dummyResponse = { data: { id: 'svc-1', active: false } }

  const originalPatch = axiosClient.patch
  axiosClient.patch = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return dummyResponse
  }

  try {
    const res = await clinicalServiceApi.updateStatus('svc-1', false)
    assert.equal(capturedUrl, '/system/clinical-services/svc-1/status')
    assert.deepEqual(capturedData, { active: false })
    assert.equal(res.data.active, false)
  } finally {
    axiosClient.patch = originalPatch
  }
})

test('clinicalServiceApi reference ranges CRUD endpoints operate correctly', async () => {
  let capturedGetUrl = null
  let capturedPostUrl = null
  let capturedPutUrl = null
  let capturedPatchUrl = null
  let capturedPatchData = null

  const originalGet = axiosClient.get
  const originalPost = axiosClient.post
  const originalPut = axiosClient.put
  const originalPatch = axiosClient.patch

  axiosClient.get = async (url) => {
    capturedGetUrl = url
    return { data: [{ id: 'rng-1', lowerBound: 3.9, upperBound: 5.5 }] }
  }
  axiosClient.post = async (url, data) => {
    capturedPostUrl = url
    return { data: { id: 'rng-2', ...data } }
  }
  axiosClient.put = async (url, data) => {
    capturedPutUrl = url
    return { data: { id: 'rng-1', ...data } }
  }
  axiosClient.patch = async (url, data) => {
    capturedPatchUrl = url
    capturedPatchData = data
    return { data: { id: 'rng-1', active: true } }
  }

  try {
    const listRes = await clinicalServiceApi.getReferenceRanges('svc-1')
    assert.equal(capturedGetUrl, '/system/clinical-services/svc-1/reference-ranges')
    assert.equal(listRes.data.length, 1)

    const createRes = await clinicalServiceApi.createReferenceRange('svc-1', {
      gender: 'MALE',
      minAge: 18,
      maxAge: 60,
      lowerBound: 3.9,
      upperBound: 5.5,
    })
    assert.equal(capturedPostUrl, '/system/clinical-services/svc-1/reference-ranges')
    assert.equal(createRes.data.gender, 'MALE')

    const updateRes = await clinicalServiceApi.updateReferenceRange('svc-1', 'rng-1', {
      gender: 'FEMALE',
      minAge: 18,
      maxAge: 60,
      lowerBound: 3.8,
      upperBound: 5.4,
    })
    assert.equal(capturedPutUrl, '/system/clinical-services/svc-1/reference-ranges/rng-1')
    assert.equal(updateRes.data.gender, 'FEMALE')

    const patchRes = await clinicalServiceApi.updateReferenceRangeStatus('svc-1', 'rng-1', true)
    assert.equal(capturedPatchUrl, '/system/clinical-services/svc-1/reference-ranges/rng-1/status')
    assert.deepEqual(capturedPatchData, { active: true })
    assert.equal(patchRes.data.active, true)
  } finally {
    axiosClient.get = originalGet
    axiosClient.post = originalPost
    axiosClient.put = originalPut
    axiosClient.patch = originalPatch
  }
})

test('clinicalServiceValidation formatters & validator work accurately', () => {
  assert.equal(formatGenderDisplay('MALE'), 'Nam')
  assert.equal(formatGenderDisplay('FEMALE'), 'Nữ')
  assert.equal(formatGenderDisplay(null), 'Tất cả')

  assert.equal(formatAgeRangeDisplay(18, 60), '18 - 60 tuổi')
  assert.equal(formatAgeRangeDisplay(18, null), '≥ 18 tuổi')
  assert.equal(formatAgeRangeDisplay(null, 65), '≤ 65 tuổi')
  assert.equal(formatAgeRangeDisplay(null, null), 'Mọi lứa tuổi')

  assert.equal(formatReferenceBoundsDisplay(3.9, 5.5, 'mmol/L'), '3.9 - 5.5 mmol/L')
  assert.equal(formatReferenceBoundsDisplay(3.9, null, 'mmol/L'), '≥ 3.9 mmol/L')
  assert.equal(formatReferenceBoundsDisplay(null, 5.5, 'mmol/L'), '≤ 5.5 mmol/L')

  // Validation
  const valid = validateReferenceRangePayload({ minAge: 18, maxAge: 60, lowerBound: 3.9, upperBound: 5.5 })
  assert.equal(valid.isValid, true)

  const invalidAge = validateReferenceRangePayload({ minAge: 70, maxAge: 60, lowerBound: 3.9 })
  assert.equal(invalidAge.isValid, false)
  assert.ok(invalidAge.errors.some((e) => e.includes('tối thiểu không được lớn hơn')))

  const invalidBounds = validateReferenceRangePayload({ minAge: 18, lowerBound: 10, upperBound: 5 })
  assert.equal(invalidBounds.isValid, false)
  assert.ok(invalidBounds.errors.some((e) => e.includes('Cận dưới không được lớn hơn')))

  const missingBounds = validateReferenceRangePayload({ minAge: 18, maxAge: 60, lowerBound: null, upperBound: null })
  assert.equal(missingBounds.isValid, false)
  assert.ok(missingBounds.errors.some((e) => e.includes('ít nhất cận dưới hoặc cận trên')))

  // Error translations
  const overlapErr = { response: { data: { code: 'CLINICAL_REFERENCE_RANGE_OVERLAP' } } }
  assert.ok(translateClinicalErrorMessage(overlapErr).includes('trùng lặp khoảng tuổi và giới tính'))
  const codeExistsErr = { response: { data: { code: 'CLINICAL_SERVICE_CODE_ALREADY_EXISTS' } } }
  assert.ok(translateClinicalErrorMessage(codeExistsErr).includes('đã tồn tại trong hệ thống'))
})

test('checkOverlapClientSide detects age interval collisions properly', () => {
  const existing = [
    { id: 'rng-1', gender: 'MALE', minAge: 18, maxAge: 60, active: true },
    { id: 'rng-2', gender: 'FEMALE', minAge: 18, maxAge: 60, active: true },
    { id: 'rng-3', gender: null, minAge: 0, maxAge: 17, active: true },
    { id: 'rng-4', gender: 'MALE', minAge: 65, maxAge: 90, active: false },
  ]

  // Same gender (MALE) overlapping [20 - 40] vs [18 - 60] -> overlap
  const res1 = checkOverlapClientSide({ gender: 'MALE', minAge: 20, maxAge: 40 }, existing)
  assert.equal(res1.isOverlap, true)
  assert.equal(res1.conflictingRange.id, 'rng-1')

  // Different gender (FEMALE) vs [18 - 60 MALE] -> no overlap with rng-1, but overlaps rng-2
  const res2 = checkOverlapClientSide({ gender: 'FEMALE', minAge: 20, maxAge: 40 }, existing)
  assert.equal(res2.isOverlap, true)
  assert.equal(res2.conflictingRange.id, 'rng-2')

  // Different gender (OTHER) vs existing -> no overlap
  const res3 = checkOverlapClientSide({ gender: 'OTHER', minAge: 20, maxAge: 40 }, existing)
  assert.equal(res3.isOverlap, false)

  // Non-overlapping adjacent age [61 - 70] vs [18 - 60] -> no overlap
  const res4 = checkOverlapClientSide({ gender: 'MALE', minAge: 61, maxAge: 70 }, existing)
  assert.equal(res4.isOverlap, false)

  // Editing rng-1 with excludeId = 'rng-1' -> no overlap with itself
  const res5 = checkOverlapClientSide({ gender: 'MALE', minAge: 18, maxAge: 60 }, existing, 'rng-1')
  assert.equal(res5.isOverlap, false)

  // Inactive range rng-4 is ignored -> [65 - 80 MALE] doesn't collide
  const res6 = checkOverlapClientSide({ gender: 'MALE', minAge: 65, maxAge: 80 }, existing)
  assert.equal(res6.isOverlap, false)
})

