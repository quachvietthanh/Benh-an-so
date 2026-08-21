import assert from 'node:assert/strict'
import test from 'node:test'

import axiosClient from './axiosClient.js'
import publicApiClient from './publicApiClient.js'

const originalWindow = globalThis.window
const originalLocalStorage = globalThis.localStorage

const createHttpError = (status, data) => ({
  message: 'Request failed',
  response: { status, data },
})

const rejectingAdapter = (error) => async () => Promise.reject(error)

const withBrowserEnvironment = async (callback) => {
  const storage = new Map()
  globalThis.localStorage = {
    getItem: (key) => storage.get(key) || null,
    setItem: (key, value) => storage.set(key, value),
    removeItem: (key) => storage.delete(key),
  }
  globalThis.window = { location: { pathname: '/dashboard', href: '' } }
  try {
    await callback(storage)
  } finally {
    globalThis.window = originalWindow
    globalThis.localStorage = originalLocalStorage
  }
}

test('authenticated client attaches the normalized envelope without changing API payloads', async () => {
  await withBrowserEnvironment(async () => {
    const requestPayload = { medicineId: 'medicine-1' }
    const payload = {
      status: 409,
      code: 'INSUFFICIENT_STOCK',
      message: 'Insufficient stock.',
      details: { shortages: [{ medicineId: 'medicine-1' }] },
    }
    const error = createHttpError(409, payload)
    let capturedConfig

    await assert.rejects(
      axiosClient.post('/prescriptions', requestPayload, {
        adapter: async (config) => {
          capturedConfig = config
          return Promise.reject(error)
        },
      }),
      (received) => {
        assert.strictEqual(received, error)
        assert.strictEqual(received.response.data, payload)
        assert.deepEqual(JSON.parse(capturedConfig.data), requestPayload)
        assert.deepEqual(received.apiError, {
          status: 409,
          code: 'INSUFFICIENT_STOCK',
          message: 'Insufficient stock.',
          details: payload.details,
          fields: {},
          firstFieldError: null,
        })
        return true
      }
    )
  })
})

test('public client normalizes a legacy text error without redirecting', async () => {
  const payload = 'Gateway unavailable.'
  const error = createHttpError(502, payload)

  await assert.rejects(
    publicApiClient.get('/public/appointments', { adapter: rejectingAdapter(error) }),
    (received) => {
      assert.strictEqual(received, error)
      assert.strictEqual(received.response.data, payload)
      assert.deepEqual(received.apiError, {
        status: 502,
        code: 'NETWORK_ERROR',
        message: payload,
        details: {},
        fields: {},
        firstFieldError: null,
      })
      return true
    }
  )
})

test('authenticated client normalizes a 401 before preserving the existing login redirect', async () => {
  await withBrowserEnvironment(async (storage) => {
    storage.set('token', 'token')
    storage.set('user', 'user')
    const error = createHttpError(401, { code: 'TOKEN_INVALID', message: 'Token invalid.' })

    await assert.rejects(axiosClient.get('/me', { adapter: rejectingAdapter(error) }), (received) => {
      assert.equal(received.apiError.code, 'TOKEN_INVALID')
      assert.equal(received.apiError.status, 401)
      return true
    })

    assert.equal(storage.get('token'), undefined)
    assert.equal(storage.get('user'), undefined)
    assert.equal(globalThis.window.location.href, '/login')
  })
})
