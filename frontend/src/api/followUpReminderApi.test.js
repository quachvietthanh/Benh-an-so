import assert from 'node:assert/strict'
import test from 'node:test'
import axiosClient from './axiosClient.js'
import followUpReminderApi from './followUpReminderApi.js'

test('follow-up reminder API uses the exact backend paths and bodies', async () => {
  const calls = []
  const originals = {
    get: axiosClient.get,
    post: axiosClient.post,
    patch: axiosClient.patch,
  }
  axiosClient.get = async (...args) => { calls.push(['get', ...args]); return { data: {} } }
  axiosClient.post = async (...args) => { calls.push(['post', ...args]); return { data: {} } }
  axiosClient.patch = async (...args) => { calls.push(['patch', ...args]); return { data: {} } }

  try {
    await followUpReminderApi.getDue({ page: 0, size: 20 })
    await followUpReminderApi.search({ status: 'PENDING', page: 0, size: 20 })
    await followUpReminderApi.create({ patientId: 'patient-uuid' })
    await followUpReminderApi.updateStatus('reminder-uuid', 'SENT')
  } finally {
    Object.assign(axiosClient, originals)
  }

  assert.deepEqual(calls, [
    ['get', '/follow-up-reminders/due', { params: { page: 0, size: 20 } }],
    ['get', '/follow-up-reminders', { params: { status: 'PENDING', page: 0, size: 20 } }],
    ['post', '/follow-up-reminders', { patientId: 'patient-uuid' }],
    ['patch', '/follow-up-reminders/reminder-uuid/status', { status: 'SENT' }],
  ])
})
