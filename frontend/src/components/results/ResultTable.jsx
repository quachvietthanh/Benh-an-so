import React from 'react'
import { Table, Button, Tag, Space, Tooltip, Typography } from 'antd'
import {
  EditOutlined,
  EyeOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import ResultStatusBadge from './ResultStatusBadge'

const { Text } = Typography

export const ResultTable = ({
  dataSource = [],
  loading = false,
  onOpenModal,
}) => {
  const columns = [
    {
      title: 'Mã chỉ định',
      dataIndex: 'orderCode',
      key: 'orderCode',
      width: 140,
      fixed: 'left',
      render: (code, record) => (
        <div>
          <Text strong style={{ color: '#2563eb', fontSize: 13.5 }}>
            {code}
          </Text>
          <div style={{ fontSize: 11, color: '#94a3b8' }}>
            {record.department || 'Khoa CLS'}
          </div>
        </div>
      ),
    },
    {
      title: 'Mã BN',
      dataIndex: 'patientCode',
      key: 'patientCode',
      width: 110,
      render: (code) => <Tag color="blue" style={{ borderRadius: 6, fontWeight: 600 }}>{code || 'BN0001'}</Tag>,
    },
    {
      title: 'Họ tên bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      width: 180,
      render: (name, record) => (
        <div>
          <Text strong style={{ color: '#0f172a', fontSize: 14 }}>
            {name}
          </Text>
          <div style={{ fontSize: 12, color: '#64748b' }}>
            {record.gender || 'Nam'} ({record.age || 30}T)
          </div>
        </div>
      ),
    },
    {
      title: 'Ngày chỉ định',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 140,
      render: (date) => (
        <span style={{ fontSize: 13, color: '#334155' }}>
          {date ? new Date(date).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '-'}
        </span>
      ),
    },
    {
      title: 'Loại cận lâm sàng',
      dataIndex: 'items',
      key: 'items',
      width: 240,
      render: (items = []) => (
        <Space direction="vertical" size={3} style={{ width: '100%' }}>
          {items.slice(0, 2).map((item, idx) => (
            <Tooltip key={idx} title={`[${item.serviceCode || 'CLS'}] ${item.serviceName}`}>
              <Tag
                color="purple"
                style={{
                  borderRadius: 4,
                  margin: 0,
                  fontSize: 11,
                  maxWidth: 225,
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
            <Tag color="cyan" style={{ borderRadius: 4, fontSize: 11, margin: 0 }}>
              +{items.length - 2} dịch vụ khác...
            </Tag>
          )}
        </Space>
      ),
    },
    {
      title: 'Bác sĩ chỉ định',
      dataIndex: 'doctorName',
      key: 'doctorName',
      width: 150,
      render: (doctor) => (
        <span style={{ fontSize: 13, color: '#475569', fontWeight: 500 }}>
          {doctor || 'BS. Trực'}
        </span>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      align: 'center',
      render: (status) => <ResultStatusBadge status={status} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 150,
      align: 'center',
      fixed: 'right',
      render: (_, record) => {
        const isConfirmed = record.status === 'CONFIRMED'
        const isResulted = record.status === 'RESULTED'

        if (isConfirmed) {
          return (
            <Button
              type="default"
              size="small"
              icon={<LockOutlined style={{ color: '#16a34a' }} />}
              onClick={() => onOpenModal(record)}
              style={{
                borderRadius: '8px',
                fontSize: '12px',
                fontWeight: 600,
                borderColor: '#b7eb8f',
                background: '#f6ffed',
                color: '#276749',
              }}
            >
              Xem kết quả (Đã khóa)
            </Button>
          )
        }

        if (isResulted) {
          return (
            <Button
              type="primary"
              size="small"
              icon={<SafetyCertificateOutlined />}
              onClick={() => onOpenModal(record)}
              style={{
                borderRadius: '8px',
                fontSize: '12px',
                fontWeight: 600,
                background: '#d97706',
                borderColor: '#d97706',
              }}
            >
              BS Xác nhận
            </Button>
          )
        }

        return (
          <Button
            type="primary"
            size="small"
            icon={<EditOutlined />}
            onClick={() => onOpenModal(record)}
            style={{
              borderRadius: '8px',
              fontSize: '12px',
              fontWeight: 600,
              background: '#2563eb',
              borderColor: '#2563eb',
            }}
          >
            Nhập kết quả
          </Button>
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
        pageSizeOptions: ['10', '20', '50'],
        showTotal: (total) => `Tổng cộng ${total} phiếu chỉ định`,
      }}
      scroll={{ x: 1100 }}
      size="middle"
      style={{
        borderRadius: '12px',
        overflow: 'hidden',
        boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
      }}
    />
  )
}

export default ResultTable
