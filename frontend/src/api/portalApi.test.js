import assert from 'node:assert/strict'
import test from 'node:test'
import portalApi from './portalApi.js'
import publicApiClient from './publicApiClient.js'

test('portalApi.lookup uses publicApiClient GET /portal/lookup with correct query params', async () => {
  const calls = []
  const originalGet = publicApiClient.get

  publicApiClient.get = async (url, config) => {
    calls.push({ url, config })
    return { data: { appointmentCode: 'APT000001' } }
  }

  try {
    const res1 = await portalApi.lookup({ code: ' APT000001 ' })
    assert.equal(calls[0].url, '/portal/lookup')
    assert.deepEqual(calls[0].config.params, { code: 'APT000001' })
    assert.equal(res1.data.appointmentCode, 'APT000001')

    const res2 = await portalApi.lookup({ code: 'APT000002', phone: ' 0912345678 ' })
    assert.equal(calls[1].url, '/portal/lookup')
    assert.deepEqual(calls[1].config.params, { code: 'APT000002', phone: '0912345678' })
    assert.equal(res2.data.appointmentCode, 'APT000001')
  } finally {
    publicApiClient.get = originalGet
  }
})
