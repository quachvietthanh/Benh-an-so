import React from 'react'
import { Badge, Button, Card, Checkbox, Empty, Space, Spin, Switch, Tag, Tooltip, Typography } from 'antd'
import { CheckCircleFilled, SaveOutlined } from '@ant-design/icons'
import {
  MODULE_ICONS,
  getModuleDisplayName,
  getPermissionDetails,
  getRoleDisplayName,
  getRoleTheme,
} from './rolePermissionsConstants'

const { Text, Paragraph } = Typography

function RolePermissionsMatrixTable({
  roles,
  groupedPermissions,
  originalPermissionsByRole,
  draftPermissionsByRole,
  isRoleDirty,
  isPermissionDirtyInRole,
  savingRoleId,
  canUpdate,
  getModuleCheckState,
  onToggleModuleForRole,
  onTogglePermission,
  onSaveRole,
}) {
  const moduleKeys = Object.keys(groupedPermissions)

  if (moduleKeys.length === 0) {
    return (
      <div className="hidden-mobile">
        <Card style={{ borderRadius: 10, textAlign: 'center', padding: '32px 0' }}>
          <Empty description="Không tìm thấy quyền chức năng nào phù hợp với bộ lọc tìm kiếm." />
        </Card>
      </div>
    )
  }

  return (
    <div className="hidden-mobile role-matrix-desktop-container" style={{ marginBottom: 24 }}>
      <div
        style={{
          overflowX: 'auto',
          backgroundColor: '#ffffff',
          borderRadius: 12,
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
        }}
      >
        <table
          className="role-matrix-table"
          style={{
            width: '100%',
            borderCollapse: 'separate',
            borderSpacing: 0,
            textAlign: 'left',
          }}
        >
          <thead>
            <tr style={{ backgroundColor: '#f8fafc', borderBottom: '2px solid #e2e8f0' }}>
              <th
                style={{
                  width: '38%',
                  minWidth: 340,
                  padding: '14px 18px',
                  fontSize: 13,
                  fontWeight: 700,
                  color: '#334155',
                  borderBottom: '2px solid #e2e8f0',
                  position: 'sticky',
                  left: 0,
                  backgroundColor: '#f8fafc',
                  zIndex: 2,
                }}
              >
                DANH MỤC PHÂN HỆ & QUYỀN HẠN
              </th>

              {roles.map((role) => {
                const isDirty = isRoleDirty(role.id)
                const draftCount = (draftPermissionsByRole[role.id] || new Set()).size
                const isSaving = savingRoleId === role.id
                const theme = getRoleTheme(role.name)

                return (
                  <th
                    key={role.id}
                    style={{
                      width: `${62 / Math.max(1, roles.length)}%`,
                      minWidth: 130,
                      padding: '12px 10px',
                      textAlign: 'center',
                      borderBottom: '2px solid #e2e8f0',
                      borderLeft: '1px solid #f1f5f9',
                      backgroundColor: isDirty ? '#fffbeb' : '#f8fafc',
                    }}
                  >
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <span>{theme.icon}</span>
                        <Text strong style={{ fontSize: 13, color: theme.text }}>
                          {getRoleDisplayName(role)}
                        </Text>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Tag
                          bordered={false}
                          color={theme.color}
                          style={{ margin: 0, fontSize: 11, borderRadius: 12, padding: '0 8px' }}
                        >
                          {draftCount} quyền
                        </Tag>
                        {isDirty && (
                          <Button
                            type="primary"
                            size="small"
                            icon={<SaveOutlined />}
                            loading={isSaving}
                            disabled={!canUpdate}
                            onClick={() => onSaveRole(role)}
                            style={{
                              fontSize: 10,
                              height: 20,
                              padding: '0 6px',
                              borderRadius: 4,
                              backgroundColor: '#f59e0b',
                              borderColor: '#f59e0b',
                            }}
                          >
                            Lưu
                          </Button>
                        )}
                      </div>
                    </div>
                  </th>
                )
              })}
            </tr>
          </thead>

          <tbody>
            {moduleKeys.map((modKey) => {
              const permsInMod = groupedPermissions[modKey] || []
              const modTitle = getModuleDisplayName(modKey)
              const modIcon = MODULE_ICONS[modKey] || <SettingOutlined />

              return (
                <React.Fragment key={modKey}>
                  {/* MODULE GROUP HEADER ROW */}
                  <tr style={{ backgroundColor: '#f1f5f9', borderTop: '1px solid #e2e8f0' }}>
                    <td
                      style={{
                        padding: '10px 18px',
                        fontSize: 13,
                        fontWeight: 700,
                        color: '#1e293b',
                        position: 'sticky',
                        left: 0,
                        backgroundColor: '#f1f5f9',
                        zIndex: 1,
                        borderBottom: '1px solid #e2e8f0',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ fontSize: 16 }}>{modIcon}</span>
                        <span>{modTitle}</span>
                        <Tag style={{ marginLeft: 4, fontSize: 11, borderRadius: 10 }}>
                          {permsInMod.length} quyền
                        </Tag>
                      </div>
                    </td>

                    {roles.map((role) => {
                      const checkState = getModuleCheckState(role.id, permsInMod)
                      const isDirty = isRoleDirty(role.id)

                      return (
                        <td
                          key={role.id}
                          style={{
                            padding: '10px 8px',
                            textAlign: 'center',
                            borderLeft: '1px solid #e2e8f0',
                            borderBottom: '1px solid #e2e8f0',
                            backgroundColor: isDirty ? '#fef9c3' : '#f1f5f9',
                          }}
                        >
                          <Tooltip
                            title={
                              checkState.all
                                ? `Bỏ chọn toàn bộ quyền phân hệ ${modTitle}`
                                : `Chọn toàn bộ quyền phân hệ ${modTitle}`
                            }
                          >
                            <Checkbox
                              checked={checkState.all}
                              indeterminate={checkState.indeterminate}
                              disabled={!canUpdate || savingRoleId === role.id}
                              onChange={() => onToggleModuleForRole(role.id, permsInMod)}
                            >
                              <span style={{ fontSize: 11, fontWeight: 600, color: '#475569' }}>
                                {checkState.checkedCount}/{checkState.total}
                              </span>
                            </Checkbox>
                          </Tooltip>
                        </td>
                      )
                    })}
                  </tr>

                  {/* PERMISSION ROWS */}
                  {permsInMod.map((perm, idx) => {
                    const details = getPermissionDetails(perm)
                    const isLast = idx === permsInMod.length - 1

                    return (
                      <tr
                        key={perm.id || perm.code}
                        className="perm-row-hover"
                        style={{
                          backgroundColor: idx % 2 === 0 ? '#ffffff' : '#fcfdfd',
                          borderBottom: isLast ? '2px solid #e2e8f0' : '1px solid #f1f5f9',
                        }}
                      >
                        {/* Permission Info Column */}
                        <td
                          style={{
                            padding: '10px 18px',
                            position: 'sticky',
                            left: 0,
                            backgroundColor: idx % 2 === 0 ? '#ffffff' : '#fcfdfd',
                            zIndex: 1,
                            borderBottom: isLast ? '2px solid #e2e8f0' : '1px solid #f1f5f9',
                          }}
                        >
                          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                              <Text strong style={{ fontSize: 13, color: '#0f172a' }}>
                                {details.title}
                              </Text>
                              <Tag
                                style={{
                                  fontSize: 10,
                                  fontFamily: 'monospace',
                                  color: '#475569',
                                  backgroundColor: '#f1f5f9',
                                  border: '1px solid #e2e8f0',
                                  padding: '0 4px',
                                  borderRadius: 4,
                                }}
                              >
                                {perm.code}
                              </Tag>
                            </div>
                            <Text type="secondary" style={{ fontSize: 12, lineHeight: '16px' }}>
                              {details.desc}
                            </Text>
                          </div>
                        </td>

                        {/* Role Permission Toggles */}
                        {roles.map((role) => {
                          const isAssigned = (draftPermissionsByRole[role.id] || new Set()).has(perm.code)
                          const isDirty = isPermissionDirtyInRole(role.id, perm.code)

                          return (
                            <td
                              key={role.id}
                              style={{
                                padding: '10px 8px',
                                textAlign: 'center',
                                borderLeft: '1px solid #f1f5f9',
                                borderBottom: isLast ? '2px solid #e2e8f0' : '1px solid #f1f5f9',
                                backgroundColor: isDirty ? '#fffdf0' : undefined,
                              }}
                            >
                              <Tooltip
                                title={
                                  <div>
                                    <div style={{ fontWeight: 600 }}>{details.title}</div>
                                    <div style={{ fontSize: 11, color: '#cbd5e1' }}>{perm.code}</div>
                                    <div style={{ marginTop: 4 }}>
                                      Vai trò: <strong>{getRoleDisplayName(role)}</strong>
                                    </div>
                                    <div>
                                      Trạng thái: <strong>{isAssigned ? 'Đang BẬT' : 'Đang TẮT'}</strong>
                                      {isDirty && <span style={{ color: '#f59e0b' }}> (Có thay đổi chưa lưu)</span>}
                                    </div>
                                  </div>
                                }
                              >
                                <div style={{ display: 'inline-flex', alignItems: 'center', position: 'relative' }}>
                                  <Switch
                                    size="small"
                                    checked={isAssigned}
                                    disabled={!canUpdate || perm.active === false || savingRoleId === role.id}
                                    onChange={() => onTogglePermission(role.id, perm.code, perm.active)}
                                    style={{
                                      backgroundColor: isAssigned ? '#2563eb' : '#cbd5e1',
                                      boxShadow: isDirty ? '0 0 0 2px #f59e0b' : undefined,
                                    }}
                                  />
                                </div>
                              </Tooltip>
                            </td>
                          )
                        })}
                      </tr>
                    )
                  })}
                </React.Fragment>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default RolePermissionsMatrixTable
