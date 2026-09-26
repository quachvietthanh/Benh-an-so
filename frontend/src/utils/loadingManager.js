/**
 * Global Loading Manager
 * Manages active async request counts and notifies subscribers of loading state changes.
 */

class LoadingManager {
  constructor() {
    this.activeRequests = 0
    this.listeners = new Set()
  }

  /**
   * Subscribe to loading state changes.
   * @param {Function} listener - Callback receiving { isLoading: boolean, activeRequests: number }
   * @returns {Function} Unsubscribe function
   */
  subscribe(listener) {
    if (typeof listener !== 'function') return () => {}
    this.listeners.add(listener)
    // Initial emit
    try {
      listener({ isLoading: this.activeRequests > 0, activeRequests: this.activeRequests })
    } catch {
      // Ignore listener error
    }
    return () => {
      this.listeners.delete(listener)
    }
  }

  /**
   * Notify all subscribers
   */
  notify() {
    const state = {
      isLoading: this.activeRequests > 0,
      activeRequests: this.activeRequests,
    }
    this.listeners.forEach((listener) => {
      try {
        listener(state)
      } catch {
        // Ignore subscriber error
      }
    })
  }

  /**
   * Increments active request counter and triggers loading state
   */
  start() {
    this.activeRequests += 1
    this.notify()
  }

  /**
   * Decrements active request counter
   */
  stop() {
    this.activeRequests = Math.max(0, this.activeRequests - 1)
    this.notify()
  }

  /**
   * Resets active requests to 0
   */
  reset() {
    this.activeRequests = 0
    this.notify()
  }

  /**
   * Returns current loading status
   */
  get isLoading() {
    return this.activeRequests > 0
  }

  /**
   * Returns active request count
   */
  get count() {
    return this.activeRequests
  }
}

export const loadingManager = new LoadingManager()
export default loadingManager
