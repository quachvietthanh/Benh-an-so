import React from 'react'
import { Badge, Button, Card, Checkbox, Empty, Progress, Space, Switch, Tag, Typography } from 'antd'
import {
  CheckCircleFilled,
  DownOutlined,
  RightOutlined,
  SaveOutlined,
  SettingOutlined,
  UndoOutlined,
} from '@ant-design/icons'
import {
  MODULE_ICONS,
  getModuleDisplayName,
  getPermissionDetails,
  getRoleDisplayName,
  getRoleTheme,
} from './rolePermissionsConstants'

const { Text, Title } = Typography

function RolePermissionsMobileView({
  roles,
  permissions,
  mobileSelectedRoleId,
  setMobileSelectedRoleId,
  mobileRole,
  groupedPermissions,
  expandedModules,
  onToggleExpandModule,
  originalPermissionsByRole,
  draftPermissionsByRole,
  isRoleDirty,
  isPermissionDirtyInRole,
  savingRoleId,
  canUpdate,
  getModuleCheckState,
  onToggleModuleForRole,
  onTogglePermission,
  onResetRole,
  onSaveRole,
}) {
  const isDirty = mobileRole ? isRoleDirty(mobileRole.id) : false
  const draftCount = mobileRole ? (draftPermissionsByRole[mobileRole.id] || new Set()).size : 0
  const theme = mobileRole ? getRoleTheme(mobileRole.name) : {}
  const isSaving = mobileRole ? savingRoleId === mobileRole.id : false
  const percent = permissions.length > 0 ? Math.round((draftCount / permissions.length) * 100) : 0
  const moduleKeys = Object.keys(groupedPermissions)

  return (
    <div className="visible-mobile">
      <div
        style={{
          display: 'flex',
          gap: 8,
          overflowX: 'auto',
          paddingBottom: 10,
          marginBottom: 14,
          scrollbarWidth: 'none',
        }}
      >
        {roles.map((r) => {
          const rTheme = getRoleTheme(r.name)
          const isSelected = r.id === mobileSelectedRoleId
          const rDirty = isRoleDirty(r.id)
          const rCount = (draftPermissionsByRole[r.id] || new Set()).size

          return (
            <Button
              key={r.id}
              onClick={() => setMobileSelectedRoleId(r.id)}
              style={{
                borderRadius: 20,
                padding: '4px 14px',
                height: 36,
                backgroundColor: isSelected ? rTheme.text : '#ffffff',
                color: isSelected ? '#ffffff' : '#334155',
                borderColor: isSelected ? rTheme.text : rDirty ? '#f59e0b' : '#e2e8f0',
                fontWeight: isSelected ? 600 : 400,
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                flexShrink: 0,
                boxShadow: isSelected ? '0 2px 6px rgba(0,0,0,0.1)' : 'none',
              }}
            >
              <span>{rTheme.icon}</span>
              <span>{getRoleDisplayName(r)}</span>
              {rDirty ? (
                <span
                  style={{
                    width: 7,
                    height: 7,
                    borderRadius: '50%',
                    backgroundColor: '#f59e0b',
                    display: 'inline-block',
                  }}
                />
              ) : (
                <span
                  style={{
                    fontSize: 11,
                    opacity: isSelected ? 0.85 : 0.6,
                  }}
                >
                  ({rCount})
                </span>
              )}
            </Button>
          )
        })}
      </div>

      {mobileRole && (
        <Card
          size="small"
          style={{
            marginBottom: 14,
            borderRadius: 10,
            borderColor: isDirty ? '#f59e0b' : theme.border,
            backgroundColor: isDirty ? '#fffdf7' : theme.bg,
          }}
          bodyStyle={{ padding: '12px 14px' }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ fontSize: 18 }}>{theme.icon}</span>
              <Text strong style={{ fontSize: 14, color: theme.text }}>
                {getRoleDisplayName(mobileRole)}
              </Text>
            </div>
            <Space>
              {isDirty && (
                <Button
                  size="small"
                  icon={<UndoOutlined />}
                  onClick={() => onResetRole(mobileRole.id)}
                  disabled={isSaving}
                >
                  Hủy
                </Button>
              )}
              <Button
                type="primary"
                size="small"
                icon={<SaveOutlined />}
                loading={isSaving}
                disabled={!isDirty || !canUpdate}
                onClick={() => onSaveRole(mobileRole)}
                style={{
                  backgroundColor: isDirty ? '#f59e0b' : undefined,
                  borderColor: isDirty ? '#f59e0b' : undefined,
                }}
              >
                {isDirty ? 'Lưu thay đổi' : 'Đã đồng bộ'}
              </Button>
            </Space>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#64748b' }}>
            <span>Số quyền được cấp:</span>
            <strong>
              {draftCount} / {permissions.length} ({percent}%)
            </strong>
          </div>
          <Progress
            percent={percent}
            size="small"
            strokeColor={isDirty ? '#f59e0b' : theme.text}
            showInfo={false}
            style={{ margin: '4px 0 0' }}
          />
        </Card>
      )}

      {moduleKeys.length === 0 ? (
        <Card style={{ borderRadius: 10, textAlign: 'center', padding: '24px 0' }}>
          <Empty description="Không tìm thấy quyền chức nào." />
        </Card>
      ) : (
        moduleKeys.map((modKey) => {
          const permsInMod = groupedPermissions[modKey] || []
          const modTitle = getModuleDisplayName(modKey)
          const modIcon = MODULE_ICONS[modKey] || <SettingOutlined />
          const isExpanded = expandedModules ? expandedModules.has(modKey) : true
          const checkState = mobileRole ? getModuleCheckState(mobileRole.id, permsInMod) : {}

          return (
            <Card
              key={modKey}
              size="small"
              style={{
                marginBottom: 12,
                borderRadius: 10,
                border: '1px solid #e2e8f0',
              }}
              bodyStyle={{ padding: 0 }}
            >
              <div
                style={{
                  padding: '10px 14px',
                  backgroundColor: '#f8fafc',
                  borderBottom: isExpanded ? '1px solid #e2e8f0' : 'none',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  cursor: 'pointer',
                  userSelect: 'none',
                }}
                onClick={() => onToggleExpandModule && onToggleExpandModule(modKey)}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontSize: 12, color: '#64748b', display: 'flex', alignItems: 'center' }}>
                    {isExpanded ? <DownOutlined /> : <RightOutlined />}
                  </span>
                  <span>{modIcon}</span>
                  <Text strong style={{ fontSize: 13, color: '#1e293b' }}>
                    {modTitle}
                  </Text>
                  <Tag style={{ margin: 0, fontSize: 10, borderRadius: 8 }}>
                    {permsInMod.length}
                  </Tag>
                </div>

                {mobileRole && (
                  <div onClick={(e) => e.stopPropagation()}>
                    <Checkbox
                      checked={checkState.all}
                      indeterminate={checkState.indeterminate}
                      disabled={!canUpdate || isSaving}
                      onChange={() => onToggleModuleForRole(mobileRole.id, permsInMod)}
                    >
                      <span style={{ fontSize: 11, fontWeight: 600, color: '#475569' }}>
                        {checkState.checkedCount}/{checkState.total}
                      </span>
                    </Checkbox>
                  </div>
                )}
              </div>

              {isExpanded && (
                <div style={{ padding: '4px 14px' }}>
                  {permsInMod.map((perm, idx) => {
                    const details = getPermissionDetails(perm)
                    const isAssigned = mobileRole
                      ? (draftPermissionsByRole[mobileRole.id] || new Set()).has(perm.code)
                      : false
                    const isPermDirty = mobileRole ? isPermissionDirtyInRole(mobileRole.id, perm.code) : false
                    const isLast = idx === permsInMod.length - 1

                    return (
                      <div
                        key={perm.id || perm.code}
                        style={{
                          padding: '10px 0',
                          borderBottom: isLast ? 'none' : '1px solid #f1f5f9',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          gap: 12,
                        }}
                      >
                        <div style={{ flex: 1 }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                            <Text strong style={{ fontSize: 13, color: '#0f172a' }}>
                              {details.title}
                            </Text>
                            <Tag style={{ fontSize: 10, fontFamily: 'monospace', margin: 0 }}>
                              {perm.code}
                            </Tag>
                          </div>
                          <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                            {details.desc}
                          </div>
                        </div>

                        {mobileRole && (
                          <Switch
                            size="small"
                            checked={isAssigned}
                            disabled={!canUpdate || perm.active === false || isSaving}
                            onChange={() => onTogglePermission(mobileRole.id, perm.code, perm.active)}
                            style={{
                              backgroundColor: isAssigned ? '#2563eb' : '#cbd5e1',
                              boxShadow: isPermDirty ? '0 0 0 2px #f59e0b' : undefined,
                              flexShrink: 0,
                            }}
                          />
                        )}
                      </div>
                    )
                  })}
                </div>
              )}
            </Card>
          )
        })
      )}
    </div>
  )
}

export default RolePermissionsMobileView
