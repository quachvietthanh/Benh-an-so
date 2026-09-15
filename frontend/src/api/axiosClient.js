import axios from 'axios'
import { normalizeApiError } from '../utils/apiError.js'
import { API_TIMEOUT } from '../utils/constants.js'

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
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

axiosClient.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error && typeof error === 'object') {
      error.apiError = normalizeApiError(error)
    }
    if (error?.response?.status === 404) {
      const isExpected404 =
        error.config?.url?.includes('/medical-records/visits/') &&
        error.config?.method?.toLowerCase() === 'get'

      if (!isExpected404) {
        console.error(
          '%c[API 404 DETECTED]',
          'background: #dc2626; color: white; padding: 2px 6px; border-radius: 4px; font-weight: bold;',
          {
            url: error.config?.url,
            fullUrl: `${error.config?.baseURL || ''}${error.config?.url || ''}`,
            method: error.config?.method?.toUpperCase(),
            params: error.config?.params,
            data: error.config?.data,
            responseStatus: error.response?.status,
            responseData: error.response?.data,
          }
        )
        console.trace('[API 404 STACK TRACE - Endpoint nào đang trả về 404?]')
      }
    }
    const errorCode = error.response?.data?.code || error.apiError?.code
    if (error.response?.status === 403 && errorCode === 'MUST_CHANGE_PASSWORD') {
      try {
        const storedUser = localStorage.getItem('user')
        if (storedUser) {
          const parsed = JSON.parse(storedUser)
          if (parsed && typeof parsed === 'object') {
            parsed.mustChangePassword = true
            localStorage.setItem('user', JSON.stringify(parsed))
          }
        }
      } catch {
        // ignore parse error
      }
      if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
        window.dispatchEvent(new CustomEvent('auth:must-change-password'))
      }
    } else if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (
        typeof window !== 'undefined' &&
        window.location?.pathname?.startsWith('/portal') &&
        window.location.pathname !== '/portal/login' &&
        window.location.pathname !== '/portal' &&
        window.location.pathname !== '/portal/register'
      ) {
        window.location.href = '/portal/login'
      } else if (
        typeof window !== 'undefined' &&
        !window.location?.pathname?.startsWith('/portal') &&
        window.location?.pathname !== '/login'
      ) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default axiosClient

