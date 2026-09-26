import assert from 'node:assert/strict'
import test from 'node:test'
import axiosClient from './axiosClient.js'
import invoiceApi from './invoiceApi.js'

test('invoiceApi calls the exact backend paths, methods and parameters', async () => {
  const calls = []
  const originals = {
    get: axiosClient.get,
    post: axiosClient.post,
  }

  axiosClient.get = async (...args) => {
    calls.push(['get', ...args])
    return { data: {} }
  }
  axiosClient.post = async (...args) => {
    calls.push(['post', ...args])
    return { data: {} }
  }

  try {
    // 1. Search with all filters
    await invoiceApi.search({
      patientName: 'Nguyễn Văn A',
      invoiceCode: 'HD-2026-001',
      invoiceType: 'ORIGINAL',
      createdFrom: '2026-09-01T00:00:00Z',
      createdTo: '2026-09-21T23:59:59Z',
      page: 0,
      size: 20,
    })

    // 2. Get by ID
    await invoiceApi.getById('inv-uuid-001')

    // 3. Get adjustments
    await invoiceApi.getAdjustments('inv-uuid-001')

    // 4. Record reprint
    await invoiceApi.reprint('inv-uuid-001')
  } finally {
    Object.assign(axiosClient, originals)
  }

  assert.deepEqual(calls, [
    [
      'get',
      '/invoices',
      {
        params: {
          patientName: 'Nguyễn Văn A',
          invoiceCode: 'HD-2026-001',
          invoiceType: 'ORIGINAL',
          createdFrom: '2026-09-01T00:00:00Z',
          createdTo: '2026-09-21T23:59:59Z',
          page: 0,
          size: 20,
        },
      },
    ],
    ['get', '/invoices/inv-uuid-001'],
    ['get', '/invoices/inv-uuid-001/adjustments'],
    ['post', '/invoices/inv-uuid-001/reprint'],
  ])
})
