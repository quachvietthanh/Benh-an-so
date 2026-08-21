import React from 'react'
import { Alert, Button, Card, Col, Input, Row, Select, Space, Switch, Typography } from 'antd'
import {
  ExclamationCircleOutlined,
  FilterOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  UndoOutlined,
} from '@ant-design/icons'
import { MODULE_DISPLAY_NAMES } from './rolePermissionsConstants'

const { Text } = Typography
const { Option } = Select

function RolePermissionsFilterBar({
  searchTerm,
  setSearchTerm,
  selectedModule,
  setSelectedModule,
  onlyShowDirty,
  setOnlyShowDirty,
  modules,
  dirtyRoles,
  hasAnyDirtyRole,
  savingRoleId,
  canUpdate,
  filteredCount,
  totalCount,
  onSaveAllDirty,
  onResetAllDirty,
}) {
  return (
    <>
      {/* UNSAVED CHANGES GLOBAL BANNER */}
      {hasAnyDirtyRole && (
        <Alert
          type="warning"
          showIcon
          icon={<ExclamationCircleOutlined style={{ color: '#d97706', fontSize: 18 }} />}
          style={{
            marginBottom: 16,
            borderRadius: 10,
            border: '1.5px solid #f59e0b',
            backgroundColor: '#fffbeb',
            boxShadow: '0 4px 14px rgba(245, 158, 11, 0.15)',
          }}
          message={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <Text strong style={{ color: '#92400e', fontSize: 14 }}>
                  Có {dirtyRoles.length} vai trò đang có thay đổi quyền chưa được lưu vào Backend!
                </Text>
                <div style={{ color: '#b45309', fontSize: 12, marginTop: 2 }}>
                  Các thay đổi chỉ có hiệu lực sau khi bạn nhấn nút Lưu bên dưới hoặc tại từng vai trò.
                </div>
              </div>
              <Space>
                <Button
                  size="small"
                  icon={<UndoOutlined />}
                  onClick={onResetAllDirty}
                  disabled={savingRoleId !== null}
                  style={{ borderRadius: 6 }}
                >
                  Hủy tất cả
                </Button>
                <Button
                  type="primary"
                  size="small"
                  icon={<SaveOutlined />}
                  loading={savingRoleId !== null}
                  disabled={!canUpdate}
                  onClick={onSaveAllDirty}
                  style={{ borderRadius: 6, backgroundColor: '#d97706', borderColor: '#d97706', fontWeight: 600 }}
                >
                  Lưu tất cả vai trò ({dirtyRoles.length})
                </Button>
              </Space>
            </div>
          }
        />
      )}

      {/* FILTER & SEARCH TOOLBAR */}
      <Card
        size="small"
        style={{
          marginBottom: 16,
          borderRadius: 10,
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
        }}
        bodyStyle={{ padding: '12px 16px' }}
      >
        <Row gutter={[12, 12]} align="middle" justify="space-between">
          <Col xs={24} md={14} lg={12}>
            <Space wrap size="middle" style={{ width: '100%' }}>
              <Input
                placeholder="Tìm kiếm mã quyền, tên chức năng, phân hệ..."
                prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                allowClear
                style={{ width: 280, borderRadius: 8 }}
              />

              <Select
                value={selectedModule}
                onChange={(val) => setSelectedModule(val)}
                style={{ width: 230, borderRadius: 8 }}
                placeholder="Lọc theo phân hệ"
              >
                <Option value="ALL">Tất cả phân hệ ({modules.length})</Option>
                {modules.map((mod) => (
                  <Option key={mod} value={mod}>
                    {MODULE_DISPLAY_NAMES[mod] || mod}
                  </Option>
                ))}
              </Select>
            </Space>
          </Col>

          <Col xs={24} md={10} lg={12} style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
            <Space align="center">
              <Switch
                size="small"
                checked={onlyShowDirty}
                onChange={(val) => setOnlyShowDirty(val)}
                disabled={!hasAnyDirtyRole}
              />
              <span style={{ fontSize: 13, color: hasAnyDirtyRole ? '#334155' : '#94a3b8' }}>
                Chỉ hiện quyền có thay đổi
              </span>
            </Space>

            <div style={{ fontSize: 12, color: '#64748b' }}>
              Hiển thị: <strong>{filteredCount}</strong> / {totalCount} quyền
            </div>

            {(searchTerm || selectedModule !== 'ALL' || onlyShowDirty) && (
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setSearchTerm('')
                  setSelectedModule('ALL')
                  setOnlyShowDirty(false)
                }}
                style={{ padding: 0, fontSize: 12 }}
              >
                Đặt lại bộ lọc
              </Button>
            )}
          </Col>
        </Row>
      </Card>
    </>
  )
}

export default RolePermissionsFilterBar
