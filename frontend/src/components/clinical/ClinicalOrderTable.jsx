import React from 'react'
import { Table, Space, Button, Tag, Dropdown, Tooltip, Typography } from 'antd'
import {
  EyeOutlined,
  EditOutlined,
  PrinterOutlined,
  DeleteOutlined,
  MoreOutlined,
  ThunderboltOutlined,
  CheckOutlined,
  SyncOutlined,
  FileDoneOutlined,
} from '@ant-design/icons'
import ClinicalOrderStatusBadge, { ClinicalOrderPriorityBadge } from './ClinicalOrderStatusBadge'

const { Text } = Typography

const GENDER_LABELS = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác',
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export const ClinicalOrderTable = ({
  dataSource = [],
  loading = false,
  onViewDetail,
  onEditOrder,
  onUpdateStatus,
  onDeleteOrder,
  onPrintOrder,
  canManage = true,
}) => {
  const columns = [
    {
      title: 'Mã chỉ định',
      dataIndex: 'orderCode',
      key: 'orderCode',
      width: 130,
      render: (code, record) => (
        <div>
          <Text strong style={{ color: '#1890ff' }}>{code}</Text>
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>{record.department || 'Khoa khám bệnh'}</div>
        </div>
      ),
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      width: 190,
      render: (_, record) => (
        <div>
          <Text strong>{record.patientName || 'Bệnh nhân'}</Text>
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>
            <Tag color="default" style={{ margin: 0 }}>{record.patientCode || 'Chưa có mã'}</Tag>
            {' • '}{GENDER_LABELS[record.gender] || record.gender || 'Chưa cập nhật giới tính'}
            {record.age !== null && record.age !== undefined ? ` (${record.age}T)` : ''}
          </div>
        </div>
      ),
    },
    {
      title: 'Chẩn đoán lâm sàng',
      dataIndex: 'diagnosis',
      key: 'diagnosis',
      ellipsis: true,
      render: (diag) => (
        <Tooltip title={diag}>
          <span>{diag || 'Chưa ghi nhận chẩn đoán'}</span>
        </Tooltip>
      ),
    },
    {
      title: 'Dịch vụ chỉ định',
      dataIndex: 'items',
      key: 'items',
      width: 250,
      render: (items = []) => (
        <Space direction="vertical" size={3} style={{ width: '100%' }}>
          {items.slice(0, 2).map((item, idx) => (
            <Tooltip key={idx} title={`[${item.serviceCode || 'CLS'}] ${item.serviceName}`}>
              <Tag
                color="blue"
                style={{
                  borderRadius: 4,
                  margin: 0,
                  fontSize: 11,
                  maxWidth: 235,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  display: 'block',
                }}
              >
                [{item.serviceCode || 'CLS'}] {item.serviceName}
              </Tag>
            </Tooltip>
          ))}
          {items.length > 2 && (
            <Tag color="purple" style={{ borderRadius: 4, fontSize: 11, margin: 0 }}>
              +{items.length - 2} dịch vụ khác...
            </Tag>
          )}
        </Space>
      ),
    },
    {
      title: 'Độ ưu tiên',
      dataIndex: 'priority',
      key: 'priority',
      width: 110,
      align: 'center',
      render: (priority) => <ClinicalOrderPriorityBadge priority={priority} />,
    },
    {
      title: 'Tổng chi phí',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      align: 'right',
      render: (amount) => (
        <Text strong style={{ color: '#cf1322' }}>
          {amount == null ? 'Chưa cập nhật' : `${Number(amount).toLocaleString('vi-VN')} đ`}
        </Text>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      align: 'center',
      render: (status) => <ClinicalOrderStatusBadge status={status} />,
    },
    {
      title: 'Bác sĩ & Ngày tạo',
      dataIndex: 'doctorName',
      key: 'doctorName',
      width: 160,
      render: (docName, record) => (
        <div>
          <Text style={{ fontSize: 13 }}>{docName || 'BS. Trực'}</Text>
          <div style={{ fontSize: 11, color: '#8c8c8c' }}>
            {record.createdAt ? new Date(record.createdAt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '-'}
          </div>
        </div>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 140,
      align: 'center',
      fixed: 'right',
      render: (_, record) => {
        const statusMenu = [
          { key: 'PENDING', label: 'Chuyển: Chờ tiếp nhận', icon: <SyncOutlined /> },
          { key: 'IN_PROGRESS', label: 'Chuyển: Đang thực hiện', icon: <SyncOutlined spin /> },
          { key: 'RESULTED', label: 'Chuyển: Đã có kết quả', icon: <FileDoneOutlined /> },
          { key: 'COMPLETED', label: 'Chuyển: Hoàn tất', icon: <CheckOutlined /> },
        ]

        const isServerOrder = UUID_PATTERN.test(String(record.id || ''))
        const canChange = canManage && !isServerOrder && record.status !== 'COMPLETED' && record.status !== 'CANCELLED'
        const menuItems = [
          {
            key: 'detail',
            label: 'Xem chi tiết',
            icon: <EyeOutlined />,
            onClick: () => onViewDetail(record),
          },
          {
            key: 'print',
            label: 'In phiếu chỉ định',
            icon: <PrinterOutlined />,
            onClick: () => onPrintOrder(record),
          },
          ...(canChange
            ? [{
                key: 'edit',
                label: 'Chỉnh sửa chỉ định',
                icon: <EditOutlined />,
                onClick: () => onEditOrder(record),
              }, { type: 'divider' }, ...statusMenu
                .filter((item) => item.key !== record.status)
                .map((item) => ({
                  key: `status-${item.key}`,
                  label: item.label,
                  icon: item.icon,
                  onClick: () => onUpdateStatus(record.id, item.key),
                })), {
                key: 'cancel',
                label: 'Hủy phiếu chỉ định',
                danger: true,
                icon: <DeleteOutlined />,
                onClick: () => onDeleteOrder(record.id),
              }]
            : []),
        ]

        return (
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <Button size="small" icon={<MoreOutlined />} aria-label={`Thao tác với phiếu ${record.orderCode || ''}`}>
              Chi tiết
            </Button>
          </Dropdown>
        )
      },
    },
  ]

  return (
    <Table
      columns={columns}
      dataSource={dataSource}
      rowKey="id"
      loading={loading}
      pagination={{
        pageSize: 10,
        showSizeChanger: true,
        showTotal: (total) => `Tổng số ${total} phiếu chỉ định`,
      }}
      scroll={{ x: 1200 }}
      size="middle"
      bordered
    />
  )
}

export default ClinicalOrderTable
