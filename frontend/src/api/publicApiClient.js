import axios from 'axios'
import { normalizeApiError } from '../utils/apiError.js'
import loadingManager from '../utils/loadingManager.js'

const configuredBaseUrl = import.meta.env?.VITE_API_BASE_URL

const publicApiClient = axios.create({
  baseURL: configuredBaseUrl || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
})

publicApiClient.interceptors.request.use(
  (config) => {
    if (!config?.skipGlobalLoading) {
      loadingManager.start()
    }
    return config
  },
  (error) => {
    if (!error?.config?.skipGlobalLoading) {
      loadingManager.stop()
    }
    return Promise.reject(error)
  }
)

publicApiClient.interceptors.response.use(
  (response) => {
    if (!response?.config?.skipGlobalLoading) {
      loadingManager.stop()
    }
    return response
  },
  (error) => {
    if (!error?.config?.skipGlobalLoading) {
      loadingManager.stop()
    }
    if (error && typeof error === 'object') {
      error.apiError = normalizeApiError(error)
    }
    return Promise.reject(error)
  }
)

export default publicApiClient

