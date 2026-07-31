import React from 'react'
import { Button, Popconfirm, Space, Table, Tag, Tooltip, Typography } from 'antd'
import { DeleteOutlined, DownloadOutlined, EyeOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { CATEGORY_OPTIONS, getFileIcon, STATUS_MAP } from './attachmentConstants.jsx'

const { Paragraph } = Typography

function AttachmentTable({
  attachments,
  loading,
  compact = false,
  patientIdFilter = null,
  isDoctorOrAdmin = false,
  onOpenPreview,
  onDownload,
  onDelete,
}) {
  const columns = [
    {
      title: 'Mã & Tệp',
      key: 'file',
      width: 220,
      render: (_, record) => (
        <Space align="start" size={10}>
          {getFileIcon(record.fileType, record.fileName)}
          <div style={{ maxWidth: 170 }}>
            <Tag color="blue" style={{ marginBottom: 4 }}>
              {record.attachmentCode || record.id}
            </Tag>
            <div
              style={{
                fontWeight: 600,
                fontSize: 13,
                color: '#1e293b',
                wordBreak: 'break-word',
                lineHeight: 1.3,
                marginBottom: 2,
              }}
            >
              {record.fileName}
            </div>
            <small style={{ color: '#64748b', fontSize: 11 }}>{record.fileSize || 'N/A'}</small>
          </div>
        </Space>
      ),
    },
    {
      title: 'Loại kết quả',
      dataIndex: 'category',
      key: 'category',
      width: 150,
      render: (cat) => {
        const found = CATEGORY_OPTIONS.find((c) => c.value === cat)
        return (
          <Tag color={found?.color || 'blue'} style={{ whiteSpace: 'normal', margin: 0 }}>
            {cat || 'Khác'}
          </Tag>
        )
      },
    },

    ...(!patientIdFilter
      ? [
          {
            title: 'Bệnh nhân',
            key: 'patient',
            width: 170,
            render: (_, record) => (
              <div>
                <strong style={{ color: '#0f172a', display: 'block', fontSize: 13 }}>{record.patientName}</strong>
                {record.patientCode && (
                  <small style={{ color: '#64748b', fontSize: 11 }}>Mã: {record.patientCode}</small>
                )}
              </div>
            ),
          },
        ]
      : []),

    {
      title: 'Tóm tắt chỉ số / Kết quả',
      dataIndex: 'resultSummary',
      key: 'resultSummary',
      minWidth: 240,
      render: (val, record) => (
        <div style={{ maxWidth: 320 }}>
          <Paragraph ellipsis={{ rows: 2, expandable: true, symbol: 'Xem thêm' }} style={{ margin: 0, fontSize: 13 }}>
            {val}
          </Paragraph>
          {record.note && (
            <small style={{ color: '#e11d48', fontStyle: 'italic', display: 'block', marginTop: 4, fontSize: 11 }}>
              Ghi chú: {record.note}
            </small>
          )}
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 180,
      render: (st) => {
        const config = STATUS_MAP[st] || STATUS_MAP.NORMAL
        return (
          <Tag
            color={config.color}
            icon={config.icon}
            style={{
              margin: 0,
              whiteSpace: 'nowrap',
              padding: '2px 8px',
              fontSize: 12,
              borderRadius: 4,
            }}
          >
            {config.label}
          </Tag>
        )
      },
    },
    {
      title: 'Ngày làm',
      dataIndex: 'testDate',
      key: 'testDate',
      width: 120,
      render: (val) => {
        if (!val) return '---'
        const d = dayjs(val)
        return (
          <div style={{ fontSize: 12, lineHeight: 1.3 }}>
            <div style={{ fontWeight: 600, color: '#334155' }}>{d.format('HH:mm')}</div>
            <div style={{ color: '#64748b' }}>{d.format('DD/MM/YYYY')}</div>
          </div>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 110,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          <Tooltip title="Xem chi tiết & tệp">
            <Button type="primary" ghost size="small" icon={<EyeOutlined />} onClick={() => onOpenPreview(record)} />
          </Tooltip>
          <Tooltip title="Tải tệp về máy">
            <Button size="small" icon={<DownloadOutlined />} onClick={() => onDownload(record)} />
          </Tooltip>
          {isDoctorOrAdmin && (
            <Popconfirm
              title="Xóa tệp đính kèm?"
              description="Bạn có chắc chắn muốn xóa tệp kết quả này không?"
              onConfirm={() => onDelete(record.id)}
              okText="Xóa"
              cancelText="Hủy"
              okButtonProps={{ danger: true }}
            >
              <Button size="small" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={attachments}
      loading={loading}
      pagination={{ pageSize: compact ? 5 : 10, showSizeChanger: true }}
      scroll={{ x: 1080 }}
      locale={{
        emptyText: 'Chưa có tệp đính kèm hoặc kết quả cận lâm sàng nào trong danh sách',
      }}
    />
  )
}

export default AttachmentTable
