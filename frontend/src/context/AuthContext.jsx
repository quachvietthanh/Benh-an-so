import React, { createContext, useState, useContext, useEffect } from 'react'
import authApi from '../api/authApi'

const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    const storedToken = localStorage.getItem('token')
    if (storedUser && storedToken) {
      setUser(JSON.parse(storedUser))
    }
    setLoading(false)
  }, [])

  const login = async (credentials) => {
    try {
      const response = await authApi.login(credentials)
      const data = response.data

      const normalizedUser = {
        id: data.userId,
        username: data.username,
        fullName: data.username,
        roles: data.role ? [data.role.toLowerCase()] : [],
        expiredAt: data.expiredAt,
      }

      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('user', JSON.stringify(normalizedUser))
      setUser(normalizedUser)

      return { success: true }
    } catch (error) {
      const message = error.response?.data?.message || 'Không thể kết nối đến máy chủ'
      return { success: false, message }
    }
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }
// TC-03: không thao tác gì quá 15 phút -> tự động hết phiên
  useEffect(() => {
    if (!user) return undefined
    const TIMEOUT = 15 * 60 * 1000
    let timer = setTimeout(logout, TIMEOUT)
    const resetTimer = () => {
      clearTimeout(timer)
      timer = setTimeout(logout, TIMEOUT)
    }
    const events = ['mousedown', 'keydown', 'scroll']
    events.forEach((e) => window.addEventListener(e, resetTimer))
    return () => {
      clearTimeout(timer)
      events.forEach((e) => window.removeEventListener(e, resetTimer))
    }
  }, [user])
  const isAuthenticated = !!user

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuthContext = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuthContext must be used within an AuthProvider')
  }
  return context
}

export default AuthContext
