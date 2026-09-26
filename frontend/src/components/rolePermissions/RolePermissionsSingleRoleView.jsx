import React from 'react'
import {
  Badge,
  Button,
  Card,
  Checkbox,
  Empty,
  Progress,
  Radio,
  Space,
  Switch,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  CaretDownOutlined,
  CaretRightOutlined,
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

const { Text, Title, Paragraph } = Typography

function RolePermissionsSingleRoleView({
  roles,
  permissions,
  selectedRoleId,
  onSelectRoleId,
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
  const currentRole = roles.find((r) => r.id === selectedRoleId) || roles[0]
  const moduleKeys = Object.keys(groupedPermissions)

  if (!currentRole) {
    return (
      <Card style={{ borderRadius: 12, textAlign: 'center', padding: '32px 0' }}>
        <Empty description="Chưa có dữ liệu vai trò để hiển thị." />
      </Card>
    )
  }

  const roleId = currentRole.id
  const isDirty = isRoleDirty(roleId)
  const isSaving = savingRoleId === roleId
  const theme = getRoleTheme(currentRole.name)
  const draftCodes = draftPermissionsByRole[roleId] || new Set()
  const draftCount = draftCodes.size
  const totalCount = permissions.length
  const percent = totalCount > 0 ? Math.round((draftCount / totalCount) * 100) : 0

  return (
    <div className="role-permissions-single-view" style={{ marginBottom: 24 }}>
      <style>{`
        .role-tab-btn {
          transition: all 0.2s ease;
        }
        .role-tab-btn:hover {
          transform: translateY(-1px);
        }
        .perm-item-row:hover {
          background-color: #f8fafc;
        }
      `}</style>

      <div
        style={{
          display: 'flex',
          gap: 10,
          overflowX: 'auto',
          paddingBottom: 12,
          marginBottom: 16,
          scrollbarWidth: 'thin',
        }}
      >
        {roles.map((role) => {
          const rTheme = getRoleTheme(role.name)
          const isSelected = role.id === selectedRoleId
          const rDirty = isRoleDirty(role.id)
          const rCount = (draftPermissionsByRole[role.id] || new Set()).size

          return (
            <Button
              key={role.id}
              className="role-tab-btn"
              onClick={() => onSelectRoleId(role.id)}
              style={{
                borderRadius: 10,
                padding: '8px 18px',
                height: 44,
                backgroundColor: isSelected ? rTheme.text : '#ffffff',
                color: isSelected ? '#ffffff' : '#334155',
                borderColor: isSelected ? rTheme.text : rDirty ? '#f59e0b' : '#e2e8f0',
                borderWidth: isSelected || rDirty ? 1.5 : 1,
                fontWeight: isSelected ? 600 : 500,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                flexShrink: 0,
                boxShadow: isSelected
                  ? '0 4px 12px rgba(0,0,0,0.12)'
                  : rDirty
                  ? '0 2px 8px rgba(245, 158, 11, 0.15)'
                  : '0 1px 3px rgba(0,0,0,0.03)',
              }}
            >
              <span style={{ fontSize: 16 }}>{rTheme.icon}</span>
              <span style={{ fontSize: 13 }}>{getRoleDisplayName(role)}</span>
              {rDirty ? (
                <Badge
                  count="Sửa"
                  style={{
                    backgroundColor: isSelected ? '#f59e0b' : '#d97706',
                    color: '#ffffff',
                    fontSize: 10,
                    fontWeight: 700,
                    height: 18,
                    lineHeight: '18px',
                    padding: '0 6px',
                    borderRadius: 10,
                    boxShadow: 'none',
                  }}
                />
              ) : (
                <Tag
                  bordered={false}
                  style={{
                    margin: 0,
                    fontSize: 11,
                    borderRadius: 12,
                    padding: '0 6px',
                    backgroundColor: isSelected ? 'rgba(255,255,255,0.2)' : '#f1f5f9',
                    color: isSelected ? '#ffffff' : '#64748b',
                  }}
                >
                  {rCount}
                </Tag>
              )}
            </Button>
          )
        })}
      </div>

      <Card
        size="small"
        style={{
          marginBottom: 16,
          borderRadius: 12,
          border: '1.5px solid',
          borderColor: isDirty ? '#f59e0b' : theme.border,
          backgroundColor: isDirty ? '#fffdf7' : theme.bg,
          boxShadow: isDirty
            ? '0 4px 14px rgba(245, 158, 11, 0.15)'
            : '0 2px 8px rgba(0,0,0,0.03)',
        }}
        bodyStyle={{ padding: '16px 20px' }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 16,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: 10,
                backgroundColor: isDirty ? '#fef3c7' : '#ffffff',
                border: `1px solid ${isDirty ? '#fde68a' : theme.border}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 22,
              }}
            >
              {theme.icon}
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Title level={5} style={{ margin: 0, color: theme.text, fontWeight: 700 }}>
                  {getRoleDisplayName(currentRole)}
                </Title>
                <Tag
                  bordered={false}
                  color={theme.color}
                  style={{ fontSize: 11, borderRadius: 10, padding: '0 8px' }}
                >
                  {currentRole.name}
                </Tag>
                {isDirty && (
                  <Badge
                    count="Có thay đổi chưa lưu"
                    style={{
                      backgroundColor: '#f59e0b',
                      color: '#ffffff',
                      fontSize: 10,
                      fontWeight: 600,
                      borderRadius: 10,
                      boxShadow: 'none',
                    }}
                  />
                )}
              </div>
              <div style={{ color: '#64748b', fontSize: 12, marginTop: 3 }}>
                Được cấp <strong>{draftCount}</strong> trên tổng số <strong>{totalCount}</strong> quyền ({percent}%)
              </div>
            </div>
          </div>

          <Space>
            {isDirty && (
              <Button
                icon={<UndoOutlined />}
                onClick={() => onResetRole(roleId)}
                disabled={isSaving}
                style={{ borderRadius: 8 }}
              >
                Hoàn tác
              </Button>
            )}
            <Button
              type={isDirty ? 'primary' : 'default'}
              icon={<SaveOutlined />}
              loading={isSaving}
              disabled={!isDirty || !canUpdate}
              onClick={() => onSaveRole(currentRole)}
              style={{
                borderRadius: 8,
                backgroundColor: isDirty ? '#f59e0b' : undefined,
                borderColor: isDirty ? '#f59e0b' : undefined,
                fontWeight: isDirty ? 600 : 400,
              }}
            >
              {isSaving ? 'Đang lưu...' : isDirty ? 'Lưu thay đổi quyền' : 'Đã đồng bộ'}
            </Button>
          </Space>
        </div>

        <div style={{ marginTop: 12 }}>
          <Progress
            percent={percent}
            size="small"
            strokeColor={isDirty ? '#f59e0b' : theme.text}
            trailColor="#e2e8f0"
            showInfo={false}
          />
        </div>
      </Card>

      {moduleKeys.length === 0 ? (
        <Card style={{ borderRadius: 12, textAlign: 'center', padding: '36px 0' }}>
          <Empty description="Không tìm thấy quyền chức năng nào phù hợp với bộ lọc." />
        </Card>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {moduleKeys.map((modKey) => {
            const permsInMod = groupedPermissions[modKey] || []
            const modTitle = getModuleDisplayName(modKey)
            const modIcon = MODULE_ICONS[modKey] || <SettingOutlined />
            const isExpanded = expandedModules.has(modKey)
            const checkState = getModuleCheckState(roleId, permsInMod)
            const isModDirty = permsInMod.some((p) => isPermissionDirtyInRole(roleId, p.code))

            return (
              <Card
                key={modKey}
                size="small"
                style={{
                  borderRadius: 10,
                  border: '1px solid',
                  borderColor: isModDirty ? '#fcd34d' : '#e2e8f0',
                  backgroundColor: '#ffffff',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
                  overflow: 'hidden',
                }}
                bodyStyle={{ padding: 0 }}
              >
                <div
                  style={{
                    padding: '12px 18px',
                    backgroundColor: isModDirty ? '#fffdf0' : '#f8fafc',
                    borderBottom: isExpanded ? '1px solid #e2e8f0' : 'none',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    cursor: 'pointer',
                    userSelect: 'none',
                    transition: 'background-color 0.15s ease',
                  }}
                  onClick={() => onToggleExpandModule(modKey)}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ fontSize: 13, color: '#64748b', display: 'flex', alignItems: 'center' }}>
                      {isExpanded ? <DownOutlined /> : <RightOutlined />}
                    </span>
                    <span style={{ fontSize: 17, display: 'flex', alignItems: 'center' }}>{modIcon}</span>
                    <span style={{ fontSize: 14, fontWeight: 700, color: '#1e293b' }}>
                      {modTitle}
                    </span>
                    <Tag
                      style={{
                        margin: 0,
                        fontSize: 11,
                        borderRadius: 12,
                        backgroundColor: '#e2e8f0',
                        border: 'none',
                        color: '#475569',
                      }}
                    >
                      {permsInMod.length} quyền
                    </Tag>
                    {isModDirty && (
                      <Tag color="warning" style={{ fontSize: 10, borderRadius: 10, margin: 0 }}>
                        Đã sửa
                      </Tag>
                    )}
                  </div>

                  <div
                    onClick={(e) => e.stopPropagation()}
                    style={{ display: 'flex', alignItems: 'center', gap: 12 }}
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
                        disabled={!canUpdate || isSaving}
                        onChange={() => onToggleModuleForRole(roleId, permsInMod)}
                      >
                        <span style={{ fontSize: 12, fontWeight: 600, color: '#334155' }}>
                          Cấp {checkState.checkedCount}/{checkState.total} quyền
                        </span>
                      </Checkbox>
                    </Tooltip>
                  </div>
                </div>

                {isExpanded && (
                  <div style={{ padding: '4px 18px 8px' }}>
                    {permsInMod.map((perm, idx) => {
                      const details = getPermissionDetails(perm)
                      const isAssigned = (draftPermissionsByRole[roleId] || new Set()).has(perm.code)
                      const isPermDirty = isPermissionDirtyInRole(roleId, perm.code)
                      const isLast = idx === permsInMod.length - 1

                      return (
                        <div
                          key={perm.id || perm.code}
                          className="perm-item-row"
                          style={{
                            padding: '10px 8px 10px 14px',
                            borderBottom: isLast ? 'none' : '1px solid #f1f5f9',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            gap: 16,
                            borderRadius: 6,
                            backgroundColor: isPermDirty ? '#fffdf0' : undefined,
                            transition: 'background-color 0.15s ease',
                          }}
                        >
                          <div style={{ flex: 1 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                              <Text strong style={{ fontSize: 13, color: '#0f172a' }}>
                                {details.title}
                              </Text>
                              <Tag
                                style={{
                                  fontSize: 11,
                                  fontFamily: 'monospace',
                                  color: '#475569',
                                  backgroundColor: '#f1f5f9',
                                  border: '1px solid #e2e8f0',
                                  padding: '0 6px',
                                  borderRadius: 4,
                                  margin: 0,
                                }}
                              >
                                {perm.code}
                              </Tag>
                              {isPermDirty && (
                                <span style={{ fontSize: 11, color: '#d97706', fontWeight: 600 }}>
                                  (Đã thay đổi)
                                </span>
                              )}
                            </div>
                            <div style={{ fontSize: 12, color: '#64748b', marginTop: 3, lineHeight: '17px' }}>
                              {details.desc}
                            </div>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
                            <Tooltip
                              title={
                                isAssigned
                                  ? `Đang BẬT quyền cho ${getRoleDisplayName(currentRole)} (Click để Tắt)`
                                  : `Đang TẮT quyền cho ${getRoleDisplayName(currentRole)} (Click để Bật)`
                              }
                            >
                              <Switch
                                size="default"
                                checked={isAssigned}
                                disabled={!canUpdate || perm.active === false || isSaving}
                                onChange={() => onTogglePermission(roleId, perm.code, perm.active)}
                                style={{
                                  backgroundColor: isAssigned ? '#2563eb' : '#cbd5e1',
                                  boxShadow: isPermDirty ? '0 0 0 2px #f59e0b' : undefined,
                                }}
                              />
                            </Tooltip>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </Card>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default RolePermissionsSingleRoleView
