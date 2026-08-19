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

const getJwtPayload = (token) => {
  if (!token || typeof token !== 'string') return null
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payloadBase64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const decoded = atob(payloadBase64)
    return JSON.parse(decoded)
  } catch {
    return null
  }
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    const storedToken = localStorage.getItem('token')
    if (storedUser && storedToken) {
      try {
        const payload = getJwtPayload(storedToken)
        if (payload && payload.exp && payload.exp * 1000 < Date.now()) {
          // Token expired -> reset session
          localStorage.removeItem('user')
          localStorage.removeItem('token')
          setUser(null)
        } else {
          const parsed = JSON.parse(storedUser)
          if (parsed && typeof parsed === 'object') {
            const tokenRole = payload?.role || payload?.roles || parsed.roles || parsed.role
            const tokenUsername = payload?.username || parsed.username
            parsed.username = tokenUsername || parsed.username
            parsed.fullName = tokenUsername || parsed.fullName || parsed.username
            parsed.roles = normalizeRoles(tokenRole)
            localStorage.setItem('user', JSON.stringify(parsed))
            setUser(parsed)
          } else {
            localStorage.removeItem('user')
            localStorage.removeItem('token')
            setUser(null)
          }
        }
      } catch (e) {
        console.warn('Invalid user data in localStorage, resetting...', e)
        localStorage.removeItem('user')
        localStorage.removeItem('token')
        setUser(null)
      }
    } else if (storedToken && !storedUser) {
      // If token exists but user object is missing, reconstruct from JWT payload
      const payload = getJwtPayload(storedToken)
      if (payload && (payload.role || payload.sub)) {
        const reconstructedUser = {
          id: payload.userId || payload.sub,
          username: payload.username || 'User',
          fullName: payload.username || 'User',
          roles: normalizeRoles(payload.role),
        }
        localStorage.setItem('user', JSON.stringify(reconstructedUser))
        setUser(reconstructedUser)
      }
    }
    setLoading(false)
  }, [])

  const login = async (credentials) => {
    try {
      const response = await authApi.login(credentials)
      const data = response.data

      const payload = getJwtPayload(data.accessToken)
      const rawRoles = payload?.role || data.roles || (data.role ? [data.role] : [])
      const username = payload?.username || data.username || credentials.username

      const normalizedUser = {
        id: data.userId || data.id || payload?.userId || payload?.sub,
        username: username,
        fullName: username,
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
        (error.response?.status === 500
          ? 'Máy chủ Backend đang bị lỗi hoặc chưa sẵn sàng kết nối (Lỗi 500)'
          : error.message || 'Tên đăng nhập hoặc mật khẩu không đúng')
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
