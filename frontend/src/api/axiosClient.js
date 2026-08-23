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
  (response) => response,
  (error) => {
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
