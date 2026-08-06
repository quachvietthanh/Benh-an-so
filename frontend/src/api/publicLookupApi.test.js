import test from 'node:test'
import assert from 'node:assert/strict'

const store = new Map()
globalThis.localStorage = {
  getItem: (key) => store.get(key) ?? null,
  setItem: (key, value) => store.set(key, String(value)),
  removeItem: (key) => store.delete(key),
}

const { default: publicLookupApi } = await import('./publicLookupApi.js')

test('looks up a stored appointment without calling an unsupported backend endpoint', async () => {
  store.set('app_patients', JSON.stringify([
    { id: 'patient-1', dateOfBirth: '1990-05-12' },
  ]))
  store.set('app_appointments', JSON.stringify([
    {
      id: 'appointment-1',
      patientId: 'patient-1',
      appointmentCode: 'LH-1234567890',
      startTime: '2026-08-07T08:00:00Z',
      status: 'SCHEDULED',
    },
  ]))

  const response = await publicLookupApi.lookupAppointment({
    appointmentCode: 'lh-1234567890',
    dateOfBirth: '1990-05-12',
  })

  assert.equal(response.data.matched, true)
  assert.equal(response.data.careState, 'SCHEDULED')
  assert.equal(response.data.scheduledAt, '2026-08-07T08:00:00Z')
})
