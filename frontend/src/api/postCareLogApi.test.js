import assert from 'node:assert/strict'
import test from 'node:test'
import axiosClient from './axiosClient.js'
import postCareLogApi from './postCareLogApi.js'

test('post-care log API uses the exact backend paths and bodies', async () => {
  const calls = []
  const originals = { get: axiosClient.get, post: axiosClient.post }
  axiosClient.get = async (...args) => { calls.push(['get', ...args]); return { data: {} } }
  axiosClient.post = async (...args) => { calls.push(['post', ...args]); return { data: {} } }

  try {
    await postCareLogApi.search({ channel: 'PHONE', page: 0, size: 20 })
    await postCareLogApi.getForPatient('patient-uuid')
    await postCareLogApi.create({ patientId: 'patient-uuid', careNotes: 'Ổn định' })
  } finally {
    Object.assign(axiosClient, originals)
  }

  assert.deepEqual(calls, [
    ['get', '/care-logs', { params: { channel: 'PHONE', page: 0, size: 20 } }],
    ['get', '/care-logs/patient/patient-uuid'],
    ['post', '/care-logs', { patientId: 'patient-uuid', careNotes: 'Ổn định' }],
  ])
})
