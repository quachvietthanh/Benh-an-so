import React, { createContext, useState, useContext, useEffect } from 'react'
import authApi from '../api/authApi'

const AuthContext = createContext(null)

const normalizeRoles = (rawRoles) => {
  if (!rawRoles) return []
  const arr = Array.isArray(rawRoles) ? rawRoles : [rawRoles]
  const normalized = new Set()
  arr.forEach((r) => {
    if (!r) return
    const str = String(r).toLowerCase()
    const clean = str.replace(/^role_/, '')
    normalized.add(clean)
    normalized.add(`role_${clean}`)
    if (clean === 'clinic_manager') {
      normalized.add('manager')
      normalized.add('role_manager')
    }
  })
  return Array.from(normalized)
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    const storedToken = localStorage.getItem('token')
    if (storedUser && storedToken) {
      try {
        const parsed = JSON.parse(storedUser)
        if (parsed && typeof parsed === 'object') {
          const raw = parsed.roles || parsed.role || []
          parsed.roles = normalizeRoles(raw)
          setUser(parsed)
        } else {
          localStorage.removeItem('user')
          localStorage.removeItem('token')
        }
      } catch (e) {
        console.warn('Invalid user data in localStorage, resetting...', e)
        localStorage.removeItem('user')
        localStorage.removeItem('token')
        setUser(null)
      }
    }
    setLoading(false)
  }, [])

  const login = async (credentials) => {
    try {
      const response = await authApi.login(credentials)
      const data = response.data

      const rawRoles = data.roles || (data.role ? [data.role] : [])
      const normalizedUser = {
        id: data.userId || data.id,
        username: data.username,
        fullName: data.username,
        roles: normalizeRoles(rawRoles),
        expiredAt: data.expiredAt,
      }

      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('user', JSON.stringify(normalizedUser))
      setUser(normalizedUser)

      return { success: true }
    } catch (error) {
      const message =
        error.response?.data?.message ||
        error.message ||
        'Tên đăng nhập hoặc mật khẩu không đúng'
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
