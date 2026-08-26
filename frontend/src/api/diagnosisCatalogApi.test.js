import assert from 'node:assert/strict'
import test from 'node:test'
import diagnosisCatalogApi from './diagnosisCatalogApi.js'
import axiosClient from './axiosClient.js'

test('diagnosisCatalogApi search passes params to /system/diagnosis-catalog', async () => {
  let capturedConfig = null
  const dummyResponse = { data: [{ id: '1', code: 'J00', name: 'Cảm lạnh', active: true }] }

  const originalGet = axiosClient.get
  axiosClient.get = async (url, config) => {
    capturedConfig = { url, ...config }
    return dummyResponse
  }

  try {
    const res = await diagnosisCatalogApi.search({ keyword: 'J00', active: true })
    assert.equal(capturedConfig.url, '/system/diagnosis-catalog')
    assert.deepEqual(capturedConfig.params, { keyword: 'J00', active: true })
    assert.deepEqual(res.data, dummyResponse.data)
  } finally {
    axiosClient.get = originalGet
  }
})

test('diagnosisCatalogApi search falls back to /diagnosis-catalog when /system returns 403', async () => {
  let callCount = 0
  let secondCallUrl = null
  const dummyResponse = { data: [{ id: '1', code: 'J00', name: 'Cảm lạnh' }] }

  const originalGet = axiosClient.get
  axiosClient.get = async (url, config) => {
    callCount++
    if (url === '/system/diagnosis-catalog') {
      const error = new Error('Access denied')
      error.response = { status: 403 }
      throw error
    }
    secondCallUrl = url
    return dummyResponse
  }

  try {
    const res = await diagnosisCatalogApi.search({ keyword: 'J00' })
    assert.equal(callCount, 2)
    assert.equal(secondCallUrl, '/diagnosis-catalog')
    assert.equal(res.isReadOnly, true)
    assert.deepEqual(res.data, dummyResponse.data)
  } finally {
    axiosClient.get = originalGet
  }
})


test('diagnosisCatalogApi create sends POST payload', async () => {
  let capturedUrl = null
  let capturedData = null
  const payload = { code: 'J00', name: 'Cảm lạnh thông thường', diseaseGroup: 'Hệ hô hấp', description: 'Ghi chú' }

  const originalPost = axiosClient.post
  axiosClient.post = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return { data: { id: 'uuid-1', ...data, active: true } }
  }

  try {
    const res = await diagnosisCatalogApi.create(payload)
    assert.equal(capturedUrl, '/system/diagnosis-catalog')
    assert.deepEqual(capturedData, payload)
    assert.equal(res.data.id, 'uuid-1')
  } finally {
    axiosClient.post = originalPost
  }
})

test('diagnosisCatalogApi update sends PUT payload', async () => {
  let capturedUrl = null
  let capturedData = null
  const payload = { name: 'Viêm họng cấp tính', diseaseGroup: 'Hệ hô hấp', description: 'Đã cập nhật' }

  const originalPut = axiosClient.put
  axiosClient.put = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return { data: { id: 'uuid-1', code: 'J02.9', ...data, active: true } }
  }

  try {
    const res = await diagnosisCatalogApi.update('uuid-1', payload)
    assert.equal(capturedUrl, '/system/diagnosis-catalog/uuid-1')
    assert.deepEqual(capturedData, payload)
    assert.equal(res.data.name, 'Viêm họng cấp tính')
  } finally {
    axiosClient.put = originalPut
  }
})

test('diagnosisCatalogApi updateStatus sends PATCH active payload', async () => {
  let capturedUrl = null
  let capturedData = null

  const originalPatch = axiosClient.patch
  axiosClient.patch = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return { data: { id: 'uuid-1', active: data.active } }
  }

  try {
    const res = await diagnosisCatalogApi.updateStatus('uuid-1', false)
    assert.equal(capturedUrl, '/system/diagnosis-catalog/uuid-1/status')
    assert.deepEqual(capturedData, { active: false })
    assert.equal(res.data.active, false)
  } finally {
    axiosClient.patch = originalPatch
  }
})

test('diagnosisCatalogApi delete sends DELETE request', async () => {
  let capturedUrl = null

  const originalDelete = axiosClient.delete
  axiosClient.delete = async (url) => {
    capturedUrl = url
    return { data: null }
  }

  try {
    await diagnosisCatalogApi.delete('uuid-1')
    assert.equal(capturedUrl, '/system/diagnosis-catalog/uuid-1')
  } finally {
    axiosClient.delete = originalDelete
  }
})
