import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  LockOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import roleApi from '../api/roleApi'
import { useAuthContext } from '../context/AuthContext'
import {
  PERMISSION_DETAILS,
  getPermissionDetails,
  getRoleDisplayName,
} from '../components/rolePermissions/rolePermissionsConstants'
import RoleOverviewCards from '../components/rolePermissions/RoleOverviewCards'
import RolePermissionsFilterBar from '../components/rolePermissions/RolePermissionsFilterBar'
import RolePermissionsMatrixTable from '../components/rolePermissions/RolePermissionsMatrixTable'
import RolePermissionsMobileView from '../components/rolePermissions/RolePermissionsMobileView'
import RolePermissionsConfirmModal from '../components/rolePermissions/RolePermissionsConfirmModal'

const { Title } = Typography

function RolePermissionsPage() {
  const { user, updateCurrentUserPermissions } = useAuthContext()

  // Strict authorization check based on technical permissions
  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [user])

  const canRead = userPermissions.includes('ROLE_READ') || userPermissions.includes('PERMISSION_READ')
  const canUpdate = userPermissions.includes('ROLE_UPDATE')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const [roles, setRoles] = useState([])
  const [permissions, setPermissions] = useState([])

  // State maps: roleId -> Set of permission codes
  const [originalPermissionsByRole, setOriginalPermissionsByRole] = useState({})
  const [draftPermissionsByRole, setDraftPermissionsByRole] = useState({})

  // Saving state tracking per roleId
  const [savingRoleId, setSavingRoleId] = useState(null)

  // Filters & Search
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedModule, setSelectedModule] = useState('ALL')
  const [onlyShowDirty, setOnlyShowDirty] = useState(false)

  // Mobile selected role view
  const [mobileSelectedRoleId, setMobileSelectedRoleId] = useState(null)

  // Confirmation modal states
  const [confirmModalOpen, setConfirmModalOpen] = useState(false)
  const [targetRoleForSave, setTargetRoleForSave] = useState(null)

  // Load data from Backend
  const loadData = useCallback(async () => {
    if (!canRead) {
      setError('Bạn không có quyền xem danh sách vai trò hoặc danh mục quyền.')
      return
    }

    setLoading(true)
    setError(null)

    try {
      const [rolesRes, permissionsRes] = await Promise.all([
        roleApi.getRoles(),
        roleApi.getPermissions(),
      ])

      const fetchedRoles = Array.isArray(rolesRes.data) ? rolesRes.data : []
      const fetchedPermissions = Array.isArray(permissionsRes.data) ? permissionsRes.data : []

      setRoles(fetchedRoles)
      setPermissions(fetchedPermissions)

      if (fetchedRoles.length > 0 && !mobileSelectedRoleId) {
        setMobileSelectedRoleId(fetchedRoles[0].id)
      }

      // Build Set maps for original and draft
      const origMap = {}
      const draftMap = {}

      fetchedRoles.forEach((role) => {
        const codes = (role.permissions || []).map((p) => p.code).filter(Boolean)
        origMap[role.id] = new Set(codes)
        draftMap[role.id] = new Set(codes)
      })

      setOriginalPermissionsByRole(origMap)
      setDraftPermissionsByRole(draftMap)
    } catch (err) {
      const status = err.response?.status
      if (status === 403) {
        setError('Bạn không có quyền xem danh sách vai trò hoặc danh mục quyền (403 Forbidden).')
      } else {
        setError(err.response?.data?.message || 'Không thể tải dữ liệu phân quyền từ máy chủ.')
      }
    } finally {
      setLoading(false)
    }
  }, [canRead, mobileSelectedRoleId])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Check if a specific role has unsaved changes
  const isRoleDirty = useCallback(
    (roleId) => {
      const orig = originalPermissionsByRole[roleId] || new Set()
      const draft = draftPermissionsByRole[roleId] || new Set()
      if (orig.size !== draft.size) return true
      for (const code of draft) {
        if (!orig.has(code)) return true
      }
      return false
    },
    [originalPermissionsByRole, draftPermissionsByRole],
  )

  // Get all dirty roles
  const dirtyRoles = useMemo(() => {
    return roles.filter((role) => isRoleDirty(role.id))
  }, [roles, isRoleDirty])

  const hasAnyDirtyRole = dirtyRoles.length > 0

  // Check if a specific permission is dirty in any role
  const isPermissionDirtyInAnyRole = useCallback(
    (permissionCode) => {
      return roles.some((role) => {
        const origHas = (originalPermissionsByRole[role.id] || new Set()).has(permissionCode)
        const draftHas = (draftPermissionsByRole[role.id] || new Set()).has(permissionCode)
        return origHas !== draftHas
      })
    },
    [roles, originalPermissionsByRole, draftPermissionsByRole],
  )

  const isPermissionDirtyInRole = useCallback(
    (roleId, permissionCode) => {
      const origHas = (originalPermissionsByRole[roleId] || new Set()).has(permissionCode)
      const draftHas = (draftPermissionsByRole[roleId] || new Set()).has(permissionCode)
      return origHas !== draftHas
    },
    [originalPermissionsByRole, draftPermissionsByRole],
  )

  // Calculate diff for confirmation modal
  const getRoleDiff = useCallback(
    (roleId) => {
      const orig = originalPermissionsByRole[roleId] || new Set()
      const draft = draftPermissionsByRole[roleId] || new Set()

      const addedCodes = []
      const removedCodes = []

      draft.forEach((code) => {
        if (!orig.has(code)) addedCodes.push(code)
      })

      orig.forEach((code) => {
        if (!draft.has(code)) removedCodes.push(code)
      })

      return { addedCodes, removedCodes }
    },
    [originalPermissionsByRole, draftPermissionsByRole],
  )

  // Toggle single permission for a role (In-Memory Draft only, NO PUT)
  const handleTogglePermission = useCallback(
    (roleId, permissionCode, permissionActive) => {
      if (!canUpdate || permissionActive === false) return

      setDraftPermissionsByRole((prev) => {
        const currentSet = new Set(prev[roleId] || [])
        if (currentSet.has(permissionCode)) {
          currentSet.delete(permissionCode)
        } else {
          currentSet.add(permissionCode)
        }
        return {
          ...prev,
          [roleId]: currentSet,
        }
      })
    },
    [canUpdate],
  )

  // Toggle all permissions in a module for a role (In-Memory Draft only)
  const handleToggleModuleForRole = useCallback(
    (roleId, permsInModule) => {
      if (!canUpdate) return

      const activePermsInMod = permsInModule.filter((p) => p.active !== false)
      if (activePermsInMod.length === 0) return

      setDraftPermissionsByRole((prev) => {
        const currentSet = new Set(prev[roleId] || [])
        const allAssigned = activePermsInMod.every((p) => currentSet.has(p.code))

        if (allAssigned) {
          activePermsInMod.forEach((p) => currentSet.delete(p.code))
        } else {
          activePermsInMod.forEach((p) => currentSet.add(p.code))
        }

        return {
          ...prev,
          [roleId]: currentSet,
        }
      })
    },
    [canUpdate],
  )

  // Helper to get 3-state checkbox status for a module in a role
  const getModuleCheckState = useCallback(
    (roleId, permsInModule) => {
      const currentSet = draftPermissionsByRole[roleId] || new Set()
      const activePerms = permsInModule.filter((p) => p.active !== false)
      if (activePerms.length === 0) return { all: false, indeterminate: false, checkedCount: 0, total: 0 }

      let checkedCount = 0
      activePerms.forEach((p) => {
        if (currentSet.has(p.code)) checkedCount++
      })

      const all = checkedCount === activePerms.length
      const indeterminate = checkedCount > 0 && checkedCount < activePerms.length

      return {
        all,
        indeterminate,
        checkedCount,
        total: activePerms.length,
      }
    },
    [draftPermissionsByRole],
  )

  // Reset a role back to original backend state
  const handleResetRole = useCallback(
    (roleId) => {
      const orig = originalPermissionsByRole[roleId] || new Set()
      setDraftPermissionsByRole((prev) => ({
        ...prev,
        [roleId]: new Set(orig),
      }))
      message.info('Đã hoàn tác các thay đổi chưa lưu cho vai trò này.')
    },
    [originalPermissionsByRole],
  )

  // Reset all dirty roles
  const handleResetAllDirty = useCallback(() => {
    const freshDraft = {}
    Object.keys(originalPermissionsByRole).forEach((roleId) => {
      freshDraft[roleId] = new Set(originalPermissionsByRole[roleId])
    })
    setDraftPermissionsByRole(freshDraft)
    message.info('Đã hoàn tác tất cả thay đổi chưa lưu.')
  }, [originalPermissionsByRole])

  // Open Save confirmation modal for a specific role
  const handleRequestSaveRole = useCallback((role) => {
    setTargetRoleForSave(role)
    setConfirmModalOpen(true)
  }, [])

  // Execute Save Role permissions to Backend (PUT /api/v1/roles/{roleId}/permissions)
  const executeSaveRolePermissions = useCallback(
    async (role) => {
      const roleId = role.id
      const fullDraftCodes = Array.from(draftPermissionsByRole[roleId] || [])

      setSavingRoleId(roleId)
      try {
        await roleApi.updateRolePermissions(roleId, fullDraftCodes)

        setOriginalPermissionsByRole((prev) => ({
          ...prev,
          [roleId]: new Set(fullDraftCodes),
        }))

        const rolesRes = await roleApi.getRoles()
        if (Array.isArray(rolesRes.data)) {
          setRoles(rolesRes.data)
        }

        const currentRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
        const targetRoleName = String(role?.name || '').toLowerCase().replace(/^role_/, '')
        if (currentRoles.includes(targetRoleName) && typeof updateCurrentUserPermissions === 'function') {
          updateCurrentUserPermissions(fullDraftCodes)
        }

        message.success(
          `Cập nhật quyền cho vai trò "${getRoleDisplayName(role)}" thành công.${currentRoles.includes(targetRoleName) ? ' Quyền của phiên làm việc này đã được áp dụng ngay lập tức.' : ' Người dùng thuộc vai trò này cần tải lại trang để áp dụng quyền mới.'}`,
          5,
        )
      } catch (err) {
        const errorMsg = err.response?.data?.message || 'Không thể lưu phân quyền lên hệ thống.'
        message.error(`Lỗi cập nhật vai trò ${role.name}: ${errorMsg}`)
      } finally {
        setSavingRoleId(null)
      }
    },
    [draftPermissionsByRole],
  )

  // Confirm save from modal
  const handleConfirmSave = useCallback(async () => {
    if (!targetRoleForSave) return
    setConfirmModalOpen(false)
    await executeSaveRolePermissions(targetRoleForSave)
    setTargetRoleForSave(null)
  }, [targetRoleForSave, executeSaveRolePermissions])

  // Save all dirty roles sequentially
  const handleSaveAllDirty = useCallback(async () => {
    if (dirtyRoles.length === 0) return

    for (const role of dirtyRoles) {
      await executeSaveRolePermissions(role)
    }
  }, [dirtyRoles, executeSaveRolePermissions])

  // Refresh data from server (with dirty check)
  const handleRefresh = useCallback(() => {
    if (hasAnyDirtyRole) {
      if (window.confirm('Bạn có các thay đổi chưa lưu. Làm mới sẽ xóa các thay đổi này. Bạn có muốn tiếp tục?')) {
        loadData()
      }
    } else {
      loadData()
    }
  }, [hasAnyDirtyRole, loadData])

  // Extract all distinct modules
  const modules = useMemo(() => {
    const set = new Set(permissions.map((p) => p.module).filter(Boolean))
    return Array.from(set).sort()
  }, [permissions])

  // Filter permissions based on search keyword and module
  const filteredPermissions = useMemo(() => {
    return permissions.filter((perm) => {
      if (selectedModule !== 'ALL' && perm.module !== selectedModule) {
        return false
      }

      if (onlyShowDirty && !isPermissionDirtyInAnyRole(perm.code)) {
        return false
      }

      if (!searchTerm.trim()) return true

      const query = searchTerm.toLowerCase().trim()
      const details = getPermissionDetails(perm)
      const codeMatch = perm.code?.toLowerCase().includes(query)
      const nameMatch = perm.name?.toLowerCase().includes(query)
      const titleMatch = details.title.toLowerCase().includes(query)
      const descMatch = details.desc.toLowerCase().includes(query)
      const moduleMatch = perm.module?.toLowerCase().includes(query)

      return codeMatch || nameMatch || titleMatch || descMatch || moduleMatch
    })
  }, [permissions, selectedModule, searchTerm, onlyShowDirty, isPermissionDirtyInAnyRole])

  // Group filtered permissions by module for structured table rendering
  const groupedPermissions = useMemo(() => {
    const map = {}
    filteredPermissions.forEach((p) => {
      const mod = p.module || 'OTHER'
      if (!map[mod]) map[mod] = []
      map[mod].push(p)
    })
    return map
  }, [filteredPermissions])

  // Mobile selected role object
  const mobileRole = useMemo(() => {
    return roles.find((r) => r.id === mobileSelectedRoleId) || roles[0]
  }, [roles, mobileSelectedRoleId])

  // If user does not have permission to view
  if (!canRead) {
    return (
      <Card style={{ margin: '16px 0', borderRadius: 12 }}>
        <Alert
          type="error"
          showIcon
          icon={<LockOutlined />}
          message="Từ chối truy cập"
          description="Bạn không có quyền xem hoặc quản lý phân quyền vai trò hệ thống (Yêu cầu quyền ROLE_READ hoặc PERMISSION_READ)."
        />
      </Card>
    )
  }

  return (
    <div className="role-permissions-management-page" style={{ padding: '4px 0 28px' }}>
      <style>{`
        .visible-mobile {
          display: none;
        }
        .hidden-mobile {
          display: block;
        }
        .role-matrix-table th, .role-matrix-table td {
          transition: background-color 0.15s ease;
        }
        .perm-row-hover:hover {
          background-color: #f1f5f9 !important;
        }
        @media (max-width: 860px) {
          .visible-mobile {
            display: block;
          }
          .hidden-mobile, .role-matrix-desktop-container {
            display: none;
          }
        }
      `}</style>

      {/* HEADER BANNER */}
      <Card
        style={{
          marginBottom: 16,
          borderRadius: 12,
          background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 60%, #3b82f6 100%)',
          boxShadow: '0 4px 14px rgba(37, 99, 235, 0.18)',
          border: 'none',
        }}
        bodyStyle={{ padding: '18px 24px' }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 12,
                backgroundColor: 'rgba(255, 255, 255, 0.2)',
                backdropFilter: 'blur(8px)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 24,
                color: '#ffffff',
                border: '1px solid rgba(255, 255, 255, 0.3)',
              }}
            >
              <SafetyCertificateOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0, color: '#ffffff', fontWeight: 600 }}>
                Quản lý & Phân quyền Vai trò Hệ thống
              </Title>
              <div style={{ color: 'rgba(255, 255, 255, 0.85)', fontSize: 13, marginTop: 2 }}>
                Ma trận kiểm soát và điều chỉnh quyền hạn từng phân hệ cho các vai trò chuẩn phòng khám.
              </div>
            </div>
          </div>

          <Space wrap size="middle">
            <div style={{ display: 'flex', gap: 8 }}>
              <Tag color="cyan" style={{ fontSize: 12, padding: '4px 10px', borderRadius: 20 }}>
                <strong>{roles.length}</strong> vai trò
              </Tag>
              <Tag color="blue" style={{ fontSize: 12, padding: '4px 10px', borderRadius: 20 }}>
                <strong>{permissions.length}</strong> quyền
              </Tag>
              <Tag color="geekblue" style={{ fontSize: 12, padding: '4px 10px', borderRadius: 20 }}>
                <strong>{modules.length}</strong> phân hệ
              </Tag>
            </div>
            <Button
              icon={<ReloadOutlined />}
              onClick={handleRefresh}
              loading={loading}
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                color: '#ffffff',
                borderColor: 'rgba(255, 255, 255, 0.35)',
                borderRadius: 8,
              }}
            >
              Làm mới
            </Button>
          </Space>
        </div>
      </Card>

      {/* ERROR ALERT */}
      {error && (
        <Alert
          type="error"
          showIcon
          message="Không thể nạp dữ liệu phân quyền"
          description={error}
          style={{ marginBottom: 16, borderRadius: 8 }}
          action={
            <Button size="small" type="primary" onClick={loadData}>
              Thử lại
            </Button>
          }
        />
      )}

      {/* ROLE OVERVIEW CARDS (DESKTOP) */}
      <RoleOverviewCards
        roles={roles}
        permissions={permissions}
        draftPermissionsByRole={draftPermissionsByRole}
        isRoleDirty={isRoleDirty}
        savingRoleId={savingRoleId}
        canUpdate={canUpdate}
        onResetRole={handleResetRole}
        onSaveRole={handleRequestSaveRole}
      />

      {/* FILTER & SEARCH TOOLBAR */}
      <RolePermissionsFilterBar
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        selectedModule={selectedModule}
        setSelectedModule={setSelectedModule}
        onlyShowDirty={onlyShowDirty}
        setOnlyShowDirty={setOnlyShowDirty}
        modules={modules}
        dirtyRoles={dirtyRoles}
        hasAnyDirtyRole={hasAnyDirtyRole}
        savingRoleId={savingRoleId}
        canUpdate={canUpdate}
        filteredCount={filteredPermissions.length}
        totalCount={permissions.length}
        onSaveAllDirty={handleSaveAllDirty}
        onResetAllDirty={handleResetAllDirty}
      />

      {/* DESKTOP MATRIX TABLE */}
      <RolePermissionsMatrixTable
        roles={roles}
        groupedPermissions={groupedPermissions}
        originalPermissionsByRole={originalPermissionsByRole}
        draftPermissionsByRole={draftPermissionsByRole}
        isRoleDirty={isRoleDirty}
        isPermissionDirtyInRole={isPermissionDirtyInRole}
        savingRoleId={savingRoleId}
        canUpdate={canUpdate}
        getModuleCheckState={getModuleCheckState}
        onToggleModuleForRole={handleToggleModuleForRole}
        onTogglePermission={handleTogglePermission}
        onSaveRole={handleRequestSaveRole}
      />

      {/* MOBILE ACCORDION VIEW */}
      <RolePermissionsMobileView
        roles={roles}
        permissions={permissions}
        mobileSelectedRoleId={mobileSelectedRoleId}
        setMobileSelectedRoleId={setMobileSelectedRoleId}
        mobileRole={mobileRole}
        groupedPermissions={groupedPermissions}
        originalPermissionsByRole={originalPermissionsByRole}
        draftPermissionsByRole={draftPermissionsByRole}
        isRoleDirty={isRoleDirty}
        isPermissionDirtyInRole={isPermissionDirtyInRole}
        savingRoleId={savingRoleId}
        canUpdate={canUpdate}
        getModuleCheckState={getModuleCheckState}
        onToggleModuleForRole={handleToggleModuleForRole}
        onTogglePermission={handleTogglePermission}
        onResetRole={handleResetRole}
        onSaveRole={handleRequestSaveRole}
      />

      {/* CONFIRMATION SAVE MODAL */}
      <RolePermissionsConfirmModal
        confirmModalOpen={confirmModalOpen}
        targetRoleForSave={targetRoleForSave}
        roleDiff={targetRoleForSave ? getRoleDiff(targetRoleForSave.id) : null}
        savingRoleId={savingRoleId}
        onClose={() => {
          setConfirmModalOpen(false)
          setTargetRoleForSave(null)
        }}
        onConfirm={handleConfirmSave}
      />
    </div>
  )
}

export default RolePermissionsPage
