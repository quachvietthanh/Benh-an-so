import React from 'react'
import { useAuthContext } from '../../context/AuthContext'

const RoleProtected = ({ 
  children, 
  roles = [], 
  fallback = null,
  permissions = [] 
}) => {
  const { user } = useAuthContext()

  if (!user) {
    return fallback
  }

  if (roles.length > 0) {
    const userRoles = user.roles || []
    const hasRole = roles.some(role => userRoles.includes(role))
    if (hasRole) return children
  }

  if (permissions.length > 0) {
    const userPermissions = user.permissions || []
    const hasPermission = permissions.some(p => userPermissions.includes(p))
    if (hasPermission) return children
  }

  if (roles.length === 0 && permissions.length === 0) {
    return children
  }

  return fallback
}

export default RoleProtected
