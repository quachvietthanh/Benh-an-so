import assert from 'node:assert/strict'
import test from 'node:test'

import systemConfigApi from './systemConfigApi.js'
import axiosClient from './axiosClient.js'
import { invalidatePatientDataCache } from '../utils/storageHelpers.js'

test('systemConfigApi.getAnonymizationStatus issues GET to /system/anonymization', async () => {
  let capturedConfig = null
  const originalGet = axiosClient.get

  axiosClient.get = async (url, config) => {
    capturedConfig = { url, config }
    return { data: { enabled: false, updatedAt: '2026-09-08T14:30:00Z' } }
  }

  try {
    const res = await systemConfigApi.getAnonymizationStatus()
    assert.strictEqual(capturedConfig.url, '/system/anonymization')
    assert.strictEqual(res.data.enabled, false)
  } finally {
    axiosClient.get = originalGet
  }
})

test('systemConfigApi.updateAnonymizationStatus issues PATCH to /system/anonymization with enabled payload', async () => {
  let captured = null
  const originalPatch = axiosClient.patch

  axiosClient.patch = async (url, data, config) => {
    captured = { url, data, config }
    return { data: { enabled: data.enabled, updatedAt: '2026-09-08T15:00:00Z' } }
  }

  try {
    const res = await systemConfigApi.updateAnonymizationStatus(true)
    assert.strictEqual(captured.url, '/system/anonymization')
    assert.deepEqual(captured.data, { enabled: true })
    assert.strictEqual(res.data.enabled, true)
  } finally {
    axiosClient.patch = originalPatch
  }
})

test('invalidatePatientDataCache removes all patient caches and dispatches window events', () => {
  const originalWindow = globalThis.window
  const originalLocalStorage = globalThis.localStorage

  const storage = new Map([
    ['app_patients', JSON.stringify([{ id: 'p1', fullName: 'Nguyen Van A' }])],
    ['app_queues', JSON.stringify([{ id: 'q1', patientName: 'Nguyen Van A' }])],
    ['app_medical_records', JSON.stringify([{ id: 'm1' }])],
    ['app_prescriptions', JSON.stringify([{ id: 'rx1' }])],
    ['app_invoices', JSON.stringify([{ id: 'inv1' }])],
    ['app_appointments', JSON.stringify([{ id: 'app1' }])],
    ['portal_booked_appointments', JSON.stringify([{ id: 'portal1' }])],
    ['signed_medical_record_m1', JSON.stringify({ signed: true })],
    ['unrelated_key', 'keep_me'],
  ])

  const dispatchedEvents = []

  globalThis.localStorage = {
    length: storage.size,
    key: (index) => Array.from(storage.keys())[index],
    getItem: (key) => storage.get(key) || null,
    setItem: (key, val) => storage.set(key, String(val)),
    removeItem: (key) => storage.delete(key),
  }

  globalThis.window = {
    dispatchEvent: (evt) => {
      dispatchedEvents.push(evt)
      return true
    },
  }

  try {
    const success = invalidatePatientDataCache(true)
    assert.strictEqual(success, true)

    // Patient-identifying keys must be gone
    assert.strictEqual(storage.has('app_patients'), false)
    assert.strictEqual(storage.has('app_queues'), false)
    assert.strictEqual(storage.has('app_medical_records'), false)
    assert.strictEqual(storage.has('app_prescriptions'), false)
    assert.strictEqual(storage.has('app_invoices'), false)
    assert.strictEqual(storage.has('app_appointments'), false)
    assert.strictEqual(storage.has('portal_booked_appointments'), false)
    assert.strictEqual(storage.has('signed_medical_record_m1'), false)

    // Unrelated keys must be preserved
    assert.strictEqual(storage.get('unrelated_key'), 'keep_me')

    // Events must be dispatched
    assert.strictEqual(dispatchedEvents.length, 2)
    assert.strictEqual(dispatchedEvents[0].type, 'patient-data:cache-invalidated')
    assert.strictEqual(dispatchedEvents[1].type, 'app:anonymization-mode-changed')
  } finally {
    globalThis.window = originalWindow
    globalThis.localStorage = originalLocalStorage
  }
})
