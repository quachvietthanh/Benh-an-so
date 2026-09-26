import test from 'node:test'
import assert from 'node:assert/strict'

import dashboardApi from './dashboardApi.js'

test('requests the backend operational dashboard endpoint', async () => {
  const previousLocalStorage = globalThis.localStorage
  globalThis.localStorage = { getItem: () => null }
  let capturedConfig

  try {
    const response = await dashboardApi.getOperational({
      adapter: async (config) => {
        capturedConfig = config
        return {
          data: {
            visitSummary: { total: 0, waiting: 0, inProgress: 0, completed: 0, cancelled: 0 },
            revenueSummary: { totalRevenueToday: 0 },
            inventoryAlertSummary: { lowStockCount: 0, expiryAlertCount: 0 },
            asOf: '2026-08-14T08:00:00Z',
          },
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
          request: {},
        }
      },
    })

    assert.equal(capturedConfig.method, 'get')
    assert.equal(capturedConfig.url, '/dashboard/operational')
    assert.equal(response.data.visitSummary.total, 0)
  } finally {
    if (previousLocalStorage === undefined) delete globalThis.localStorage
    else globalThis.localStorage = previousLocalStorage
  }
})
