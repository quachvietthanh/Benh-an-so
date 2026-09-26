import test from 'node:test'
import assert from 'node:assert/strict'

import publicLookupApi from './publicLookupApi.js'

test('sends the verified lookup payload through the public client without a JWT', async () => {
  const payload = {
    appointmentCode: 'LH-1234567890',
    dateOfBirth: '1990-05-12',
  }
  let capturedConfig

  const response = await publicLookupApi.lookupAppointment(payload, {
    adapter: async (config) => {
      capturedConfig = config
      return {
        data: {
          matched: false,
          careState: null,
          scheduledAt: null,
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
        request: {},
      }
    },
  })

  assert.equal(capturedConfig.method, 'post')
  assert.equal(capturedConfig.url, '/public/appointments/lookup')
  assert.equal(capturedConfig.headers.get('Authorization'), undefined)
  assert.deepEqual(JSON.parse(capturedConfig.data), payload)
  assert.equal(response.data.matched, false)
})
