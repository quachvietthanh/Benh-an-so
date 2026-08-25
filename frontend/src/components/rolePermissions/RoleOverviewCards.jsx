import React from 'react'
import { Badge, Button, Card, Col, Progress, Row, Space, Tag, Typography } from 'antd'
import { ReloadOutlined, SaveOutlined, UndoOutlined } from '@ant-design/icons'
import { getRoleDisplayName, getRoleTheme } from './rolePermissionsConstants'

const { Text } = Typography

function RoleOverviewCards({
  roles,
  permissions,
  draftPermissionsByRole,
  isRoleDirty,
  savingRoleId,
  canUpdate,
  onResetRole,
  onSaveRole,
}) {
  return (
    <div className="hidden-mobile" style={{ marginBottom: 18 }}>
      <Row gutter={[12, 12]} style={{ display: 'flex', flexWrap: 'wrap' }}>
        {roles.map((role) => {
          const isDirty = isRoleDirty(role.id)
          const draftCount = (draftPermissionsByRole[role.id] || new Set()).size
          const isSaving = savingRoleId === role.id
          const theme = getRoleTheme(role.name)
          const percent = permissions.length > 0 ? Math.round((draftCount / permissions.length) * 100) : 0

          return (
            <Col
              key={role.id}
              xs={24}
              sm={12}
              md={8}
              lg={Math.max(4, Math.floor(24 / Math.max(1, roles.length)))}
              style={{ display: 'flex' }}
            >
              <Card
                size="small"
                style={{
                  width: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  borderRadius: 10,
                  borderWidth: isDirty ? 1.5 : 1,
                  borderColor: isDirty ? '#f59e0b' : theme.border,
                  backgroundColor: isDirty ? '#fffdf7' : '#ffffff',
                  boxShadow: isDirty ? '0 4px 12px rgba(245, 158, 11, 0.15)' : '0 1px 3px rgba(0,0,0,0.04)',
                  transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
                }}
                bodyStyle={{
                  padding: '12px 14px',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  height: '100%',
                  flex: 1,
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 6, marginBottom: 8, height: 26 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0, overflow: 'hidden' }}>
                      <span style={{ fontSize: 15, flexShrink: 0 }}>{theme.icon}</span>
                      <Text
                        strong
                        ellipsis
                        style={{ fontSize: 13, color: theme.text, whiteSpace: 'nowrap' }}
                        title={getRoleDisplayName(role)}
                      >
                        {getRoleDisplayName(role)}
                      </Text>
                    </div>
                    {isDirty ? (
                      <Badge
                        count="Chưa lưu"
                        style={{
                          backgroundColor: '#f59e0b',
                          color: '#ffffff',
                          fontSize: 10,
                          fontWeight: 600,
                          borderRadius: 10,
                          boxShadow: 'none',
                          padding: '0 6px',
                          height: 18,
                          lineHeight: '18px',
                          flexShrink: 0,
                        }}
                      />
                    ) : (
                      <Tag
                        bordered={false}
                        color={theme.color}
                        style={{ margin: 0, fontSize: 10, borderRadius: 10, padding: '0 6px', height: 18, lineHeight: '18px', flexShrink: 0 }}
                      >
                        {role.name}
                      </Tag>
                    )}
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
                    <span style={{ fontSize: 12, color: '#64748b' }}>Quyền khả dụng:</span>
                    <span style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
                      {draftCount}
                      <span style={{ fontSize: 11, fontWeight: 400, color: '#94a3b8' }}> / {permissions.length}</span>
                    </span>
                  </div>

                  <Progress
                    percent={percent}
                    size="small"
                    strokeColor={isDirty ? '#f59e0b' : theme.text}
                    trailColor="#f1f5f9"
                    showInfo={false}
                    style={{ margin: '4px 0 10px' }}
                  />
                </div>

                <div style={{ display: 'flex', gap: 6, marginTop: 4 }}>
                  {isDirty && (
                    <Button
                      size="small"
                      icon={<UndoOutlined />}
                      onClick={() => onResetRole(role.id)}
                      disabled={isSaving}
                      style={{ fontSize: 11, borderRadius: 6 }}
                      title="Khôi phục trạng thái gốc"
                    />
                  )}
                  <Button
                    type={isDirty ? 'primary' : 'default'}
                    size="small"
                    icon={<SaveOutlined />}
                    loading={isSaving}
                    disabled={!isDirty || !canUpdate}
                    onClick={() => onSaveRole(role)}
                    style={{
                      flex: 1,
                      fontSize: 11,
                      borderRadius: 6,
                      background: isDirty ? '#f59e0b' : undefined,
                      borderColor: isDirty ? '#f59e0b' : undefined,
                      fontWeight: isDirty ? 600 : 400,
                    }}
                  >
                    {isSaving ? 'Đang lưu...' : isDirty ? 'Lưu quyền' : 'Đã đồng bộ'}
                  </Button>
                </div>
              </Card>
            </Col>
          )
        })}
      </Row>
    </div>
  )
}

export default RoleOverviewCards
