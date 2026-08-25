import React from 'react'
import { Badge, Button, Card, Checkbox, Empty, Space, Spin, Switch, Tag, Tooltip, Typography } from 'antd'
import {
  CaretDownOutlined,
  CaretRightOutlined,
  CheckCircleFilled,
  DownOutlined,
  RightOutlined,
  SaveOutlined,
  SettingOutlined,
} from '@ant-design/icons'
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
      <style>{`
        .role-matrix-scroll-wrapper {
          overflow-x: auto;
          overflow-y: auto;
          max-height: calc(100vh - 240px);
          background-color: #ffffff;
          border-radius: 12px;
          border: 1px solid #e2e8f0;
          box-shadow: 0 2px 10px rgba(0,0,0,0.04);
          scrollbar-width: thin;
        }
        .role-matrix-table {
          width: 100%;
          border-collapse: separate;
          border-spacing: 0;
          text-align: left;
        }
        .role-matrix-table thead th {
          position: sticky;
          top: 0;
          z-index: 10;
          background-color: #f8fafc;
          border-bottom: 2px solid #cbd5e1;
        }
        .role-matrix-table thead th.sticky-corner-cell {
          position: sticky;
          top: 0;
          left: 0;
          z-index: 15;
          background-color: #f8fafc;
        }
        .module-header-row {
          cursor: pointer;
          user-select: none;
          transition: background-color 0.15s ease;
        }
        .module-header-row:hover {
          background-color: #e2e8f0 !important;
        }
        .module-header-row:hover td.sticky-module-cell {
          background-color: #e2e8f0 !important;
        }
        .perm-row-hover {
          transition: background-color 0.15s ease;
        }
        .perm-row-hover:hover td {
          background-color: #f1f5f9 !important;
        }
      `}</style>

      <div className="role-matrix-scroll-wrapper">
        <table className="role-matrix-table">
          <thead>
            <tr>
              <th
                className="sticky-corner-cell"
                style={{
                  width: '38%',
                  minWidth: 350,
                  padding: '14px 18px',
                  fontSize: 13,
                  fontWeight: 700,
                  color: '#1e293b',
                  boxShadow: '2px 0 5px rgba(0,0,0,0.03)',
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
                      minWidth: 135,
                      padding: '12px 10px',
                      textAlign: 'center',
                      borderLeft: '1px solid #e2e8f0',
                      backgroundColor: isDirty ? '#fffbeb' : '#f8fafc',
                    }}
                  >
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span style={{ fontSize: 15 }}>{theme.icon}</span>
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
              const isExpanded = expandedModules ? expandedModules.has(modKey) : true

              return (
                <React.Fragment key={modKey}>
                  {/* MODULE GROUP HEADER ROW */}
                  <tr
                    className="module-header-row"
                    onClick={() => onToggleExpandModule && onToggleExpandModule(modKey)}
                    style={{ backgroundColor: '#f1f5f9' }}
                  >
                    <td
                      className="sticky-module-cell"
                      style={{
                        padding: '11px 18px',
                        fontSize: 13,
                        fontWeight: 700,
                        color: '#1e293b',
                        position: 'sticky',
                        left: 0,
                        backgroundColor: '#f1f5f9',
                        zIndex: 4,
                        borderBottom: isExpanded ? '1px solid #e2e8f0' : '2px solid #cbd5e1',
                        borderTop: '1px solid #e2e8f0',
                        boxShadow: '2px 0 5px rgba(0,0,0,0.03)',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ fontSize: 12, color: '#64748b', display: 'flex', alignItems: 'center' }}>
                          {isExpanded ? <DownOutlined /> : <RightOutlined />}
                        </span>
                        <span style={{ fontSize: 16, display: 'flex', alignItems: 'center' }}>{modIcon}</span>
                        <span style={{ fontSize: 13, fontWeight: 700 }}>{modTitle}</span>
                        <Tag
                          style={{
                            marginLeft: 4,
                            fontSize: 11,
                            borderRadius: 10,
                            backgroundColor: '#e2e8f0',
                            border: 'none',
                            color: '#475569',
                          }}
                        >
                          {permsInMod.length} quyền
                        </Tag>
                        {!isExpanded && (
                          <span style={{ fontSize: 11, color: '#64748b', fontWeight: 400, marginLeft: 4 }}>
                            (Click để mở)
                          </span>
                        )}
                      </div>
                    </td>

                    {roles.map((role) => {
                      const checkState = getModuleCheckState(role.id, permsInMod)
                      const isDirty = isRoleDirty(role.id)

                      return (
                        <td
                          key={role.id}
                          onClick={(e) => e.stopPropagation()}
                          style={{
                            padding: '10px 8px',
                            textAlign: 'center',
                            borderLeft: '1px solid #e2e8f0',
                            borderBottom: isExpanded ? '1px solid #e2e8f0' : '2px solid #cbd5e1',
                            borderTop: '1px solid #e2e8f0',
                            backgroundColor: isDirty ? '#fef9c3' : '#f1f5f9',
                          }}
                        >
                          <Tooltip
                            title={
                              checkState.all
                                ? `Bỏ chọn toàn bộ quyền phân hệ ${modTitle}`
                                : `Cấp toàn bộ quyền phân hệ ${modTitle}`
                            }
                          >
                            <Checkbox
                              checked={checkState.all}
                              indeterminate={checkState.indeterminate}
                              disabled={!canUpdate || savingRoleId === role.id}
                              onChange={() => onToggleModuleForRole(role.id, permsInMod)}
                            >
                              <span style={{ fontSize: 11, fontWeight: 600, color: '#334155' }}>
                                {checkState.checkedCount}/{checkState.total}
                              </span>
                            </Checkbox>
                          </Tooltip>
                        </td>
                      )
                    })}
                  </tr>

                  {/* PERMISSION CHILD ROWS (ONLY RENDERED WHEN EXPANDED) */}
                  {isExpanded &&
                    permsInMod.map((perm, idx) => {
                      const details = getPermissionDetails(perm)
                      const isLast = idx === permsInMod.length - 1

                      return (
                        <tr
                          key={perm.id || perm.code}
                          className="perm-row-hover"
                          style={{
                            backgroundColor: idx % 2 === 0 ? '#ffffff' : '#fcfdfd',
                          }}
                        >
                          {/* Permission Info Column (Indented) */}
                          <td
                            style={{
                              padding: '9px 18px 9px 36px',
                              position: 'sticky',
                              left: 0,
                              backgroundColor: idx % 2 === 0 ? '#ffffff' : '#fcfdfd',
                              zIndex: 3,
                              borderBottom: isLast ? '2px solid #cbd5e1' : '1px solid #f1f5f9',
                              boxShadow: '2px 0 5px rgba(0,0,0,0.03)',
                            }}
                          >
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
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
                                    margin: 0,
                                  }}
                                >
                                  {perm.code}
                                </Tag>
                              </div>
                              <Text type="secondary" style={{ fontSize: 12, lineHeight: '16px', color: '#64748b' }}>
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
                                  padding: '9px 8px',
                                  textAlign: 'center',
                                  borderLeft: '1px solid #f1f5f9',
                                  borderBottom: isLast ? '2px solid #cbd5e1' : '1px solid #f1f5f9',
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
