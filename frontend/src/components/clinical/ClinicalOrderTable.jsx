import React from 'react'
import { Table, Space, Button, Tag, Dropdown, Menu, Tooltip, Popconfirm, Typography } from 'antd'
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
      width: 140,
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
          <Text strong>{record.patientName}</Text>
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>
            <Tag color="default" style={{ margin: 0 }}>{record.patientCode}</Tag> • {record.gender || 'Nam'} ({record.age || 30}T)
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
          {Number(amount || 0).toLocaleString('vi-VN')} đ
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
      width: 170,
      render: (docName, record) => (
        <div>
          <Text style={{ fontSize: 13, fontWeight: 500 }}>{docName || 'BS. Trực'}</Text>
          <div style={{ fontSize: 11, color: '#8c8c8c' }}>
            {record.createdAt ? new Date(record.createdAt).toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-'}
          </div>
        </div>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 120,
      align: 'center',
      fixed: 'right',
      render: (_, record) => {
        const canEdit = canManage && record.status !== 'COMPLETED' && record.status !== 'CANCELLED'
        const menuItems = [
          {
            key: 'detail',
            label: 'Xem chi tiết phiếu',
            icon: <EyeOutlined style={{ color: '#1890ff' }} />,
            onClick: () => onViewDetail(record),
          },
          {
            key: 'print',
            label: 'In phiếu chỉ định',
            icon: <PrinterOutlined style={{ color: '#52c41a' }} />,
            onClick: () => onPrintOrder(record),
          },
          ...(canEdit
            ? [
                {
                  key: 'edit',
                  label: 'Chỉnh sửa chỉ định',
                  icon: <EditOutlined style={{ color: '#fa8c16' }} />,
                  onClick: () => onEditOrder(record),
                },
              ]
            : []),
          { type: 'divider' },
          {
            key: 'status_pending',
            label: 'Đánh dấu: Chờ tiếp nhận',
            icon: <SyncOutlined />,
            onClick: () => onUpdateStatus(record.id, 'PENDING'),
          },
          {
            key: 'status_progress',
            label: 'Đánh dấu: Đang thực hiện',
            icon: <SyncOutlined spin />,
            onClick: () => onUpdateStatus(record.id, 'IN_PROGRESS'),
          },
          {
            key: 'status_result',
            label: 'Đánh dấu: Đã có kết quả',
            icon: <FileDoneOutlined />,
            onClick: () => onUpdateStatus(record.id, 'RESULTED'),
          },
          {
            key: 'status_completed',
            label: 'Đánh dấu: Hoàn tất',
            icon: <CheckOutlined />,
            onClick: () => onUpdateStatus(record.id, 'COMPLETED'),
          },
          { type: 'divider' },
          {
            key: 'cancel',
            label: 'Hủy phiếu chỉ định',
            danger: true,
            icon: <DeleteOutlined />,
            onClick: () => onDeleteOrder(record.id),
          },
        ]

        return (
          <Dropdown menu={{ items: menuItems }} trigger={['click']}>
            <Button size="small" icon={<MoreOutlined />}>
              Thao tác
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
