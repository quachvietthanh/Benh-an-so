import { useState } from 'react'
import authApi from '../api/authApi'

export const useAuth = () => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const login = async (credentials) => {
    setLoading(true)
    setError(null)
    try {
      const response = await authApi.login(credentials)
      const data = response.data

      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('user', JSON.stringify({
        id: data.userId,
        username: data.username,
        fullName: data.username,
        roles: data.role ? [String(data.role).toLowerCase()] : [],
        expiredAt: data.expiredAt,
      }))

      return { success: true, data }
    } catch (err) {
      const message = err.response?.data?.message || 'Đăng nhập thất bại'
      setError(message)
      return { success: false, message }
    } finally {
      setLoading(false)
    }
  }

  const logout = async () => {
    const accessToken = localStorage.getItem('token')
    if (accessToken) await authApi.logout(accessToken).catch(() => {})
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    window.location.href = '/login'
  }

  return { login, logout, loading, error }
}
