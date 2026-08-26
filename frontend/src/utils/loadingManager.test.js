import assert from 'node:assert/strict'
import test from 'node:test'
import { loadingManager } from './loadingManager.js'

test('loadingManager starts, stops, and notifies subscribers accurately', () => {
  loadingManager.reset()

  assert.strictEqual(loadingManager.isLoading, false)
  assert.strictEqual(loadingManager.count, 0)

  const history = []
  const unsubscribe = loadingManager.subscribe((state) => {
    history.push({ ...state })
  })

  // Initial state was pushed on subscribe
  assert.deepStrictEqual(history[0], { isLoading: false, activeRequests: 0 })

  loadingManager.start()
  assert.strictEqual(loadingManager.isLoading, true)
  assert.strictEqual(loadingManager.count, 1)

  loadingManager.start()
  assert.strictEqual(loadingManager.isLoading, true)
  assert.strictEqual(loadingManager.count, 2)

  loadingManager.stop()
  assert.strictEqual(loadingManager.isLoading, true)
  assert.strictEqual(loadingManager.count, 1)

  loadingManager.stop()
  assert.strictEqual(loadingManager.isLoading, false)
  assert.strictEqual(loadingManager.count, 0)

  // Stop below 0 doesn't go negative
  loadingManager.stop()
  assert.strictEqual(loadingManager.isLoading, false)
  assert.strictEqual(loadingManager.count, 0)

  unsubscribe()
  loadingManager.start()
  // No new items pushed to history after unsubscribe
  assert.strictEqual(history[history.length - 1].activeRequests, 0)

  loadingManager.reset()
  assert.strictEqual(loadingManager.isLoading, false)
})
