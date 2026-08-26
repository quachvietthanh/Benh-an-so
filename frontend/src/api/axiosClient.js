import axios from 'axios'
import { normalizeApiError } from '../utils/apiError.js'
import { API_TIMEOUT } from '../utils/constants.js'
import loadingManager from '../utils/loadingManager.js'

const configuredBaseUrl = import.meta.env?.VITE_API_BASE_URL

const axiosClient = axios.create({
  baseURL: configuredBaseUrl || 'http://localhost:8080/api/v1',
  timeout: API_TIMEOUT || 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

axiosClient.interceptors.request.use(
  (config) => {
    if (!config?.skipGlobalLoading) {
      loadingManager.start()
    }
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
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

axiosClient.interceptors.response.use(
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
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default axiosClient

