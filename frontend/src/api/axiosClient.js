import axios from 'axios'

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: thêm JWT token vào header nếu không phải demo-token
axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token && token !== 'demo-token') {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor: xử lý lỗi chung
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const token = localStorage.getItem('token')
    // Nếu token là demo-token hoặc đang ở trang /login thì không ép hard-reload chuyển hướng
    if (error.response?.status === 401 && token !== 'demo-token') {
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

