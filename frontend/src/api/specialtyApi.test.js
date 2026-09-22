import assert from 'node:assert/strict'
import test from 'node:test'

import axiosClient from './axiosClient.js'
import specialtyApi from './specialtyApi.js'

test('specialtyApi.search formats keyword and active parameters properly', async () => {
  let capturedConfig
  const originalGet = axiosClient.get
  axiosClient.get = async (url, config) => {
    capturedConfig = { url, config }
    return { data: [{ id: 's-1', code: 'GENERAL', name: 'General', active: true }] }
  }

  try {
    const res = await specialtyApi.search({ keyword: '  nhi  ', active: true })
    assert.strictEqual(capturedConfig.url, '/system/specialties')
    assert.deepEqual(capturedConfig.config.params, { keyword: 'nhi', active: true })
    assert.strictEqual(res.data.length, 1)

    // With active: 'ALL' -> active should be omitted
    await specialtyApi.search({ keyword: '', active: 'ALL' })
    assert.deepEqual(capturedConfig.config.params, {})
  } finally {
    axiosClient.get = originalGet
  }
})

test('specialtyApi.getById calls GET /system/specialties/:id', async () => {
  let capturedUrl
  const originalGet = axiosClient.get
  axiosClient.get = async (url) => {
    capturedUrl = url
    return { data: { id: 's-uuid-1', code: 'PEDIATRICS', name: 'Khoa Nhi' } }
  }

  try {
    const res = await specialtyApi.getById('s-uuid-1')
    assert.strictEqual(capturedUrl, '/system/specialties/s-uuid-1')
    assert.strictEqual(res.data.code, 'PEDIATRICS')
  } finally {
    axiosClient.get = originalGet
  }
})

test('specialtyApi.create calls POST /system/specialties with payload', async () => {
  let capturedUrl
  let capturedData
  const originalPost = axiosClient.post
  axiosClient.post = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return { data: { id: 's-uuid-2', ...data, active: true } }
  }

  try {
    const payload = {
      code: 'CARDIOLOGY',
      name: 'Khoa Tim Mạch',
      description: 'Chuyên khoa tim mạch',
      doctorIds: ['doc-1'],
      roomIds: ['room-101'],
    }
    const res = await specialtyApi.create(payload)
    assert.strictEqual(capturedUrl, '/system/specialties')
    assert.deepEqual(capturedData, payload)
    assert.strictEqual(res.data.code, 'CARDIOLOGY')
  } finally {
    axiosClient.post = originalPost
  }
})

test('specialtyApi.update calls PUT /system/specialties/:id with payload', async () => {
  let capturedUrl
  let capturedData
  const originalPut = axiosClient.put
  axiosClient.put = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return { data: { id: 's-uuid-1', ...data } }
  }

  try {
    const payload = {
      name: 'Khoa Nhi Cập Nhật',
      description: 'Mô tả mới',
      doctorIds: ['doc-1', 'doc-2'],
      roomIds: ['room-102'],
    }
    const res = await specialtyApi.update('s-uuid-1', payload)
    assert.strictEqual(capturedUrl, '/system/specialties/s-uuid-1')
    assert.deepEqual(capturedData, payload)
    assert.strictEqual(res.data.name, 'Khoa Nhi Cập Nhật')
  } finally {
    axiosClient.put = originalPut
  }
})

test('specialtyApi.deactivate calls PATCH /system/specialties/:id/deactivate with confirm param', async () => {
  let capturedUrl
  let capturedConfig
  const originalPatch = axiosClient.patch
  axiosClient.patch = async (url, data, config) => {
    capturedUrl = url
    capturedConfig = config
    return { data: { id: 's-uuid-1', active: false } }
  }

  try {
    await specialtyApi.deactivate('s-uuid-1', true)
    assert.strictEqual(capturedUrl, '/system/specialties/s-uuid-1/deactivate')
    assert.deepEqual(capturedConfig.params, { confirm: true })

    await specialtyApi.deactivate('s-uuid-1', false)
    assert.deepEqual(capturedConfig.params, { confirm: false })
  } finally {
    axiosClient.patch = originalPatch
  }
})

test('specialtyApi.activate calls PATCH /system/specialties/:id/activate', async () => {
  let capturedUrl
  const originalPatch = axiosClient.patch
  axiosClient.patch = async (url) => {
    capturedUrl = url
    return { data: { id: 's-uuid-1', active: true } }
  }

  try {
    const res = await specialtyApi.activate('s-uuid-1')
    assert.strictEqual(capturedUrl, '/system/specialties/s-uuid-1/activate')
    assert.strictEqual(res.data.active, true)
  } finally {
    axiosClient.patch = originalPatch
  }
})
