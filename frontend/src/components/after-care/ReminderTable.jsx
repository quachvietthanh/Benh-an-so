import React from 'react'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  EllipsisOutlined,
  HistoryOutlined,
  PhoneOutlined,
  SendOutlined,
} from '@ant-design/icons'
import { Button, Dropdown, Empty, Modal, Space, Table, Tag, Tooltip, Typography } from 'antd'
import {
  formatVietnamDate,
  formatVietnamDateTime,
  getAllowedStatusActions,
  getReminderStatusMeta,
  getReminderTypeMeta,
  shortId,
} from '../../utils/aftercareHelpers'

const { Text } = Typography

const STATUS_ACTION_META = {
  SENT: { label: 'Đánh dấu đã nhắc', icon: <SendOutlined />, confirm: 'Xác nhận đã gửi lời nhắc?' },
  COMPLETED: { label: 'Hoàn thành', icon: <CheckCircleOutlined />, confirm: 'Xác nhận hoàn thành lịch nhắc?' },
  CANCELLED: { label: 'Hủy', icon: <CloseCircleOutlined />, confirm: 'Bạn có chắc muốn hủy lịch nhắc này?', danger: true },
}

function ReminderTable({
  mode = 'all',
  records,
  loading,
  pagination,
  onPageChange,
  patientsById,
  visitsById,
  canCreateCareLog,
  canReadCareLog,
  canUpdate,
  updatingId,
  onOpenCareLog,
  onOpenHistory,
  onStatusChange,
}) {
  const isDue = mode === 'due'

  const patientColumn = {
    title: 'Bệnh nhân',
    key: 'patient',
    width: 190,
    fixed: 'left',
    render: (_, record) => {
      const patient = patientsById[record.patientId]
      return (
        <div className="aftercare-person-cell">
          <Text strong>{patient?.fullName || `ID ${shortId(record.patientId)}`}</Text>
          {!isDue && <Text type="secondary">{patient?.patientCode || 'Chưa tải được mã BN'}</Text>}
        </div>
      )
    },
  }

  const columns = [
    ...(!isDue ? [{
      title: 'Mã/ID',
      dataIndex: 'id',
      key: 'id',
      width: 118,
      fixed: 'left',
      render: (value) => (
        <Tooltip title={value}><Text code>{shortId(value)}</Text></Tooltip>
      ),
    }] : []),
    patientColumn,
    ...(isDue ? [{
      title: 'Mã BN',
      key: 'patientCode',
      width: 120,
      render: (_, record) => patientsById[record.patientId]?.patientCode || '—',
    }] : []),
    {
      title: 'Lượt khám',
      dataIndex: 'visitId',
      key: 'visitId',
      width: 140,
      render: (value) => {
        const visit = visitsById[value]
        return (
          <Tooltip title={value}>
            <Text>{visit?.visitCode || `ID ${shortId(value)}`}</Text>
          </Tooltip>
        )
      },
    },
    {
      title: 'Ngày tái khám',
      dataIndex: 'followUpDate',
      key: 'followUpDate',
      width: 135,
      render: formatVietnamDate,
    },
    {
      title: 'Thời điểm nhắc',
      dataIndex: 'remindAt',
      key: 'remindAt',
      width: 170,
      render: formatVietnamDateTime,
    },
    {
      title: isDue ? 'Loại nhắc' : 'Loại',
      dataIndex: 'reminderType',
      key: 'reminderType',
      width: 150,
      render: (value) => <Tag color="blue">{getReminderTypeMeta(value).label}</Tag>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 135,
      render: (value) => {
        const meta = getReminderStatusMeta(value)
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Ghi chú',
      dataIndex: 'notes',
      key: 'notes',
      width: 230,
      ellipsis: true,
      render: (value) => value || '—',
    },
    ...(!isDue ? [{
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: formatVietnamDateTime,
    }] : []),
    {
      title: 'Thao tác',
      key: 'actions',
      width: 90,
      fixed: 'right',
      render: (_, record) => {
        const statusActions = canUpdate ? getAllowedStatusActions(record.status) : []
        const canRecordCare = canCreateCareLog && ['PENDING', 'SENT'].includes(record.status)

        const menuItems = []

        if (canRecordCare) {
          menuItems.push({
            key: 'care',
            icon: <PhoneOutlined />,
            label: 'Ghi nhận chăm sóc',
            onClick: () => onOpenCareLog(record),
          })
        }

        if (canReadCareLog) {
          menuItems.push({
            key: 'history',
            icon: <HistoryOutlined />,
            label: 'Lịch sử BN',
            onClick: () => onOpenHistory(record.patientId),
          })
        }

        statusActions.forEach((status) => {
          const meta = STATUS_ACTION_META[status]
          if (!meta) return
          menuItems.push({
            key: status,
            icon: meta.icon,
            label: meta.label,
            danger: meta.danger,
            onClick: () => {
              Modal.confirm({
                title: meta.confirm,
                okText: 'Xác nhận',
                cancelText: 'Không',
                okButtonProps: meta.danger ? { danger: true } : undefined,
                onOk: () => onStatusChange(record, status),
              })
            },
          })
        })

        if (menuItems.length === 0) return '—'

        return (
          <Dropdown
            menu={{ items: menuItems }}
            trigger={['click']}
            placement="bottomRight"
          >
            <Button
              size="small"
              icon={<EllipsisOutlined />}
              loading={updatingId === record.id}
              disabled={Boolean(updatingId) && updatingId !== record.id}
              style={{
                borderRadius: 6,
                minWidth: 32,
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            />
          </Dropdown>
        )
      },
    },
  ]

  return (
    <Table
      className="aftercare-table"
      columns={columns}
      dataSource={records}
      rowKey="id"
      loading={loading}
      scroll={{ x: isDue ? 1550 : 1740 }}
      pagination={{
        current: pagination.page + 1,
        pageSize: pagination.size,
        total: pagination.total,
        showSizeChanger: true,
        pageSizeOptions: [10, 20, 50],
        showTotal: (total) => `${total} lịch nhắc`,
        onChange: (page, size) => onPageChange(page - 1, size),
      }}
      locale={{
        emptyText: (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={isDue ? 'Không có lịch nhắc nào đến hạn.' : 'Chưa có lịch nhắc tái khám.'}
          />
        ),
      }}
    />
  )
}

export default ReminderTable
