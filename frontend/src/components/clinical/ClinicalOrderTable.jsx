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
      render: (items) => {
        const safeItems = Array.isArray(items) ? items : []
        return (
          <Space direction="vertical" size={3} style={{ width: '100%' }}>
            {safeItems.slice(0, 2).map((item, idx) => (
              <Tooltip key={idx} title={`[${item?.serviceCode || 'CLS'}] ${item?.serviceName || ''}`}>
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
                  [{item?.serviceCode || 'CLS'}] {item?.serviceName}
                </Tag>
              </Tooltip>
            ))}
            {safeItems.length > 2 && (
              <Tag color="purple" style={{ borderRadius: 4, fontSize: 11, margin: 0 }}>
                +{safeItems.length - 2} dịch vụ khác...
              </Tag>
            )}
          </Space>
        )
      },
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
        const ALLOWED_NEXT_STATUS = {
          PENDING: [{ key: 'IN_PROGRESS', label: 'Chuyển: Đang thực hiện', icon: <SyncOutlined spin /> }],
          IN_PROGRESS: [{ key: 'RESULTED', label: 'Chuyển: Đã có kết quả', icon: <FileDoneOutlined /> }],
          RESULTED: [{ key: 'COMPLETED', label: 'Chuyển: Hoàn tất', icon: <CheckOutlined /> }],
          COMPLETED: [],
          CANCELLED: [],
        }

        const nextOptions = ALLOWED_NEXT_STATUS[record.status] || []
        const canCancel = record.status === 'PENDING' || record.status === 'IN_PROGRESS'

        const menuItems = [
          ...nextOptions.map((m) => ({
            key: m.key,
            label: m.label,
            icon: m.icon,
            onClick: () => onUpdateStatus(record.id, m.key),
          })),
        ]

        if (canCancel) {
          if (menuItems.length > 0) {
            menuItems.push({ type: 'divider' })
          }
          menuItems.push({
            key: 'CANCELLED',
            label: 'Hủy phiếu chỉ định',
            danger: true,
            icon: <DeleteOutlined />,
            onClick: () => onDeleteOrder(record.id),
          })
        }

        return (
          <Space size="small">
            <Tooltip title="Xem chi tiết phiếu chỉ định">
              <Button
                type="text"
                size="small"
                icon={<EyeOutlined style={{ color: '#1890ff' }} />}
                onClick={() => onViewDetail(record)}
              />
            </Tooltip>

            <Tooltip title="In phiếu chỉ định">
              <Button
                type="text"
                size="small"
                icon={<PrinterOutlined style={{ color: '#52c41a' }} />}
                onClick={() => onPrintOrder(record)}
              />
            </Tooltip>

            {canManage && record.status === 'PENDING' && (
              <Tooltip title="Chỉnh sửa chỉ định (chỉ dành cho phiếu Chờ tiếp nhận)">
                <Button
                  type="text"
                  size="small"
                  icon={<EditOutlined style={{ color: '#fa8c16' }} />}
                  onClick={() => onEditOrder(record)}
                />
              </Tooltip>
            )}

            {menuItems.length > 0 && (
              <Dropdown
                menu={{ items: menuItems }}
                trigger={['click']}
              >
                <Button type="text" size="small" icon={<MoreOutlined />} />
              </Dropdown>
            )}
          </Space>
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
