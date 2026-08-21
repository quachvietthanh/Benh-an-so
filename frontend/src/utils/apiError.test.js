import assert from 'node:assert/strict'
import test from 'node:test'

import { getApiErrorMessage, isAccessDeniedApiError, normalizeApiError } from './apiError.js'

test('normalizes the common backend error envelope', () => {
  const error = {
    response: {
      status: 409,
      data: {
        status: 409,
        code: 'INSUFFICIENT_STOCK',
        message: 'Insufficient stock.',
        details: { shortages: [{ medicineId: 'medicine-1' }] },
      },
    },
  }

  assert.deepEqual(normalizeApiError(error), {
    status: 409,
    code: 'INSUFFICIENT_STOCK',
    message: 'Insufficient stock.',
    details: { shortages: [{ medicineId: 'medicine-1' }] },
    fields: {},
    firstFieldError: null,
  })
})

test('prefers a field validation error for display', () => {
  const error = {
    response: {
      status: 400,
      data: {
        code: 'VALIDATION_FAILED',
        message: 'Validation failed.',
        details: { fields: { name: 'Name is required.' } },
      },
    },
  }

  assert.equal(getApiErrorMessage(error, 'Fallback'), 'Name is required.')
})

test('normalizes a legacy plain-text HTTP response', () => {
  const error = { response: { status: 502, data: 'Gateway unavailable.' } }

  assert.deepEqual(normalizeApiError(error), {
    status: 502,
    code: 'NETWORK_ERROR',
    message: 'Gateway unavailable.',
    details: {},
    fields: {},
    firstFieldError: null,
  })
})

test('identifies access denial from the stable code or HTTP status, never the message', () => {
  assert.equal(isAccessDeniedApiError({ code: 'ACCESS_DENIED', status: 400, message: 'Other message.' }), true)
  assert.equal(isAccessDeniedApiError({ code: 'NETWORK_ERROR', status: 403, message: 'Other message.' }), true)
  assert.equal(isAccessDeniedApiError({ code: 'NETWORK_ERROR', status: 400, message: 'Access denied.' }), false)
})
