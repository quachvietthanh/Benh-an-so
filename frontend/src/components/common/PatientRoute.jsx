import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthContext } from '../../context/AuthContext'
import Loading from './Loading'

const PatientRoute = ({ children }) => {
  const { isAuthenticated, loading, user } = useAuthContext()

  if (loading) {
    return <Loading fullPage tip="Đang kiểm tra quyền truy cập..." subtip="Đang tải thông tin cổng bệnh nhân..." />
  }

  if (!isAuthenticated) {
    return <Navigate to="/portal/login" replace />
  }

  const userRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const isPatient = userRoles.includes('patient')

  if (!isPatient) {
    return <Navigate to="/portal/login" replace />
  }

  return children
}

export default PatientRoute
