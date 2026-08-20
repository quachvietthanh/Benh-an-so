import React from 'react'
import { EllipsisOutlined, HistoryOutlined } from '@ant-design/icons'
import { Button, Dropdown, Empty, Table, Tag, Tooltip, Typography } from 'antd'
import {
  formatVietnamDateTime,
  getContactChannelMeta,
  getContactOutcomeMeta,
  getPatientConditionMeta,
  shortId,
} from '../../utils/aftercareHelpers'

const { Text } = Typography

function CareLogTable({
  records,
  loading,
  pagination,
  onPageChange,
  patientsById,
  visitsById,
  performerNames,
  onOpenHistory,
}) {
  const columns = [
    {
      title: 'Bệnh nhân',
      key: 'patient',
      width: 190,
      fixed: 'left',
      render: (_, record) => {
        const patient = patientsById[record.patientId]
        return (
          <div className="aftercare-person-cell">
            <Text strong>{patient?.fullName || `ID ${shortId(record.patientId)}`}</Text>
            <Text type="secondary">{patient?.patientCode || 'Chưa tải được mã BN'}</Text>
          </div>
        )
      },
    },
    {
      title: 'Lượt khám',
      dataIndex: 'visitId',
      key: 'visitId',
      width: 135,
      render: (value) => value ? (
        <Tooltip title={value}>{visitsById[value]?.visitCode || `ID ${shortId(value)}`}</Tooltip>
      ) : '—',
    },
    {
      title: 'Lịch nhắc liên quan',
      dataIndex: 'reminderId',
      key: 'reminderId',
      width: 155,
      render: (value) => value ? <Tooltip title={value}>ID {shortId(value)}</Tooltip> : '—',
    },
    {
      title: 'Thời gian liên hệ',
      dataIndex: 'contactedAt',
      key: 'contactedAt',
      width: 170,
      render: formatVietnamDateTime,
    },
    {
      title: 'Kênh liên hệ',
      dataIndex: 'contactChannel',
      key: 'contactChannel',
      width: 130,
      render: (value) => <Tag color="blue">{getContactChannelMeta(value).label}</Tag>,
    },
    {
      title: 'Tình trạng bệnh nhân',
      dataIndex: 'patientCondition',
      key: 'patientCondition',
      width: 165,
      render: (value) => {
        const meta = getPatientConditionMeta(value)
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Kết quả liên hệ',
      dataIndex: 'contactOutcome',
      key: 'contactOutcome',
      width: 180,
      render: (value) => {
        const meta = getContactOutcomeMeta(value)
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Nội dung chăm sóc',
      dataIndex: 'careNotes',
      key: 'careNotes',
      width: 280,
      render: (value) => <Text>{value || '—'}</Text>,
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'performedBy',
      key: 'performedBy',
      width: 155,
      render: (value) => performerNames[value] || (value ? `ID ${shortId(value)}` : '—'),
    },
    {
      title: 'Thời gian ghi nhận',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: formatVietnamDateTime,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 90,
      fixed: 'right',
      render: (_, record) => (
        <Dropdown
          menu={{
            items: [
              {
                key: 'history',
                icon: <HistoryOutlined />,
                label: 'Lịch sử BN',
                onClick: () => onOpenHistory(record.patientId),
              },
            ],
          }}
          trigger={['click']}
          placement="bottomRight"
        >
          <Button
            size="small"
            icon={<EllipsisOutlined />}
            style={{
              borderRadius: 6,
              minWidth: 32,
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          />
        </Dropdown>
      ),
    },
  ]

  return (
    <Table
      className="aftercare-table"
      columns={columns}
      dataSource={records}
      rowKey="id"
      loading={loading}
      scroll={{ x: 1900 }}
      pagination={{
        current: pagination.page + 1,
        pageSize: pagination.size,
        total: pagination.total,
        showSizeChanger: !pagination.clientSide,
        pageSizeOptions: [10, 20, 50],
        showTotal: (total) => `${total} ghi nhận`,
        onChange: (page, size) => onPageChange(page - 1, size),
      }}
      locale={{
        emptyText: (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Chưa có ghi nhận chăm sóc sau khám."
          />
        ),
      }}
    />
  )
}

export default CareLogTable
