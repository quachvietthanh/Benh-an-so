import React from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Input,
  Radio,
  Row,
  Segmented,
  Select,
  Space,
  Switch,
  Tooltip,
  Typography,
} from 'antd'
import {
  AppstoreOutlined,
  BarsOutlined,
  DownSquareOutlined,
  ExclamationCircleOutlined,
  FilterOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  TableOutlined,
  UndoOutlined,
  UpSquareOutlined,
  UserOutlined,
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
  viewMode,
  setViewMode,
  onExpandAll,
  onCollapseAll,
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
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: 12,
              }}
            >
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
                  style={{
                    borderRadius: 6,
                    backgroundColor: '#d97706',
                    borderColor: '#d97706',
                    fontWeight: 600,
                  }}
                >
                  Lưu tất cả vai trò ({dirtyRoles.length})
                </Button>
              </Space>
            </div>
          }
        />
      )}

      {/* FILTER & TOOLBAR CARD */}
      <Card
        size="small"
        style={{
          marginBottom: 16,
          borderRadius: 10,
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
          backgroundColor: '#ffffff',
        }}
        bodyStyle={{ padding: '14px 18px' }}
      >
        <Row gutter={[14, 14]} align="middle" justify="space-between">
          {/* Left Controls: Search & Module Filter */}
          <Col xs={24} lg={12}>
            <Space wrap size="middle" style={{ width: '100%' }}>
              <Input
                placeholder="Tìm tên quyền, mã code (VD: APPOINTMENT_CREATE)..."
                prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                allowClear
                style={{ width: 300, borderRadius: 8 }}
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

          {/* Right Controls: View Mode, Expand/Collapse, Dirty filter */}
          <Col
            xs={24}
            lg={12}
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              alignItems: 'center',
              gap: 12,
              flexWrap: 'wrap',
            }}
          >
            {/* VIEW MODE SWITCHER */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Segmented
                value={viewMode}
                onChange={(val) => setViewMode(val)}
                options={[
                  {
                    label: 'Ma trận tổng thể',
                    value: 'matrix',
                    icon: <TableOutlined />,
                  },
                  {
                    label: 'Từng vai trò',
                    value: 'single',
                    icon: <UserOutlined />,
                  },
                ]}
                style={{
                  backgroundColor: '#f1f5f9',
                  padding: 3,
                  borderRadius: 8,
                  fontWeight: 500,
                }}
              />
            </div>

            {/* EXPAND / COLLAPSE BUTTONS */}
            <Space size="small">
              <Tooltip title="Mở rộng tất cả các nhóm phân hệ">
                <Button
                  size="small"
                  icon={<FolderOpenOutlined />}
                  onClick={onExpandAll}
                  style={{ fontSize: 12, borderRadius: 6 }}
                >
                  Mở tất cả
                </Button>
              </Tooltip>

              <Tooltip title="Thu gọn tất cả các nhóm phân hệ">
                <Button
                  size="small"
                  icon={<FolderOutlined />}
                  onClick={onCollapseAll}
                  style={{ fontSize: 12, borderRadius: 6 }}
                >
                  Thu gọn
                </Button>
              </Tooltip>
            </Space>

            {/* ONLY DIRTY SWITCH */}
            <Space align="center" size="small">
              <Switch
                size="small"
                checked={onlyShowDirty}
                onChange={(val) => setOnlyShowDirty(val)}
                disabled={!hasAnyDirtyRole}
              />
              <span
                style={{
                  fontSize: 12,
                  color: hasAnyDirtyRole ? '#334155' : '#94a3b8',
                  whiteSpace: 'nowrap',
                }}
              >
                Chỉ quyền đã sửa
              </span>
            </Space>

            {/* RESULTS COUNTER */}
            <div style={{ fontSize: 12, color: '#64748b', whiteSpace: 'nowrap' }}>
              <strong>{filteredCount}</strong>/{totalCount} quyền
            </div>

            {/* RESET FILTER LINK */}
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
                Đặt lại
              </Button>
            )}
          </Col>
        </Row>
      </Card>
    </>
  )
}

export default RolePermissionsFilterBar
