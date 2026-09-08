import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthContext } from '../../context/AuthContext'

const PatientRoute = ({ children }) => {
  const { isAuthenticated, loading, user } = useAuthContext()

  if (loading) {
    return null
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
