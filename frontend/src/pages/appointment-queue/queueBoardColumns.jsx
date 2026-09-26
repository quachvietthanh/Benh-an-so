import React from 'react'
import { Avatar, Badge, Button, Dropdown, Space, Tag, Tooltip, Typography } from 'antd'
import {
  AlertOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  HistoryOutlined,
  MoreOutlined,
  ReloadOutlined,
  StarOutlined,
  StepForwardOutlined,
  StopOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { getAvatarStyle, getInitials } from '../../utils/appointmentQueueUiHelpers'
import { QUEUE_STATUS_META } from '../../utils/queueHelpers'
import { canUserCloseVisit } from '../../utils/closeVisitHelpers'
import { canPrioritize, getPriorityTag } from '../../utils/queuePriorityHelpers.js'

const { Text } = Typography

export const getQueueBoardColumns = ({
  getPatientInfo,
  getDoctorInfo,
  permissions = {},
  user,
  reQueuingId,
  onOpenDetail,
  onCallNext,
  onUpdateStatus,
  onSkip,
  onReQueue,
  onOpenHistory,
  onCloseVisit,
  onPrioritize,
}) => [
  {
    title: 'STT',
    dataIndex: 'queueNumber',
    key: 'queueNumber',
    width: 70,
    render: (num, _, idx) => <Badge count={num || idx + 1} style={{ backgroundColor: '#2563eb' }} />,
  },
  {
    title: 'Bệnh nhân',
    dataIndex: 'patientName',
    key: 'patientName',
    width: 250,
    render: (_, record) => {
      const pInfo = getPatientInfo(record.patientId, record.patientName, record.patientCode, record.phone)
      const callCount = Number(record.callCount) || 0
      const priorityTag = getPriorityTag(record.priority)

      return (
        <Space align="center" size="small">
          <Avatar style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 150 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
              <Text strong style={{ fontSize: 14, color: '#0f172a', lineHeight: '1.4' }}>
                {pInfo.name}
              </Text>
              {priorityTag && (
                <Tooltip title={record.priorityReason ? `Lý do: ${record.priorityReason}` : priorityTag.label}>
                  <Tag
                    color={priorityTag.color}
                    style={{
                      margin: 0,
                      fontWeight: 700,
                      fontSize: 11,
                      padding: '1px 6px',
                      borderRadius: 4,
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 4,
                      backgroundColor: priorityTag.bgColor,
                      borderColor: priorityTag.borderColor,
                      color: priorityTag.textColor,
                    }}
                  >
                    {priorityTag.isEmergency ? (
                      <AlertOutlined style={{ fontSize: 12 }} />
                    ) : (
                      <StarOutlined style={{ fontSize: 12 }} />
                    )}
                    {priorityTag.label}
                  </Tag>
                </Tooltip>
              )}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
              <Text type="secondary" style={{ fontSize: 12, lineHeight: '1.2' }}>
                Mã: {pInfo.code}
              </Text>
              {callCount > 0 && (
                <Tag color="orange" style={{ margin: 0, fontSize: 10, padding: '0 4px', lineHeight: '16px', borderRadius: 4 }}>
                  Đã gọi: {callCount} lần
                </Tag>
              )}
            </div>
          </div>
        </Space>
      )
    },
  },
  {
    title: 'Mã lượt khám',
    dataIndex: 'visitCode',
    key: 'visitCode',
    width: 150,
    render: (val, record) => {
      const rawCode = val || record.visitId || record.id || 'Chưa có'
      let displayCode = rawCode
      if (displayCode.length > 20) {
        displayCode = `VIS-${String(rawCode).slice(-6).toUpperCase()}`
      }
      return (
        <Text code style={{ whiteSpace: 'nowrap', display: 'inline-block', fontSize: 13 }}>
          {displayCode}
        </Text>
      )
    },
  },
  {
    title: 'Nguồn',
    dataIndex: 'sourceType',
    key: 'sourceType',
    width: 150,
    render: (src) =>
      src === 'WALK_IN' ? (
        <Tag color="orange" style={{ whiteSpace: 'nowrap' }}>
          Bệnh nhân tự đến
        </Tag>
      ) : (
        <Tag color="blue" style={{ whiteSpace: 'nowrap' }}>
          Hẹn trước
        </Tag>
      ),
  },
  {
    title: 'Bác sĩ',
    dataIndex: 'doctorName',
    key: 'doctorName',
    width: 180,
    render: (_, record) => {
      const dInfo = getDoctorInfo(record.doctorId, record.doctorName, record.department)
      return <span style={{ whiteSpace: 'nowrap' }}>{dInfo.name}</span>
    },
  },
  {
    title: 'Phòng',
    dataIndex: 'roomName',
    key: 'roomName',
    width: 140,
    render: (room, record) => (
      <span style={{ whiteSpace: 'nowrap' }}>
        {room || record.roomNumber || record.roomCode || 'Chưa phân phòng'}
      </span>
    ),
  },
  {
    title: 'Trạng thái',
    dataIndex: 'status',
    key: 'status',
    width: 170,
    render: (st, record) => {
      const meta = QUEUE_STATUS_META[st] || { label: 'Không xác định', tone: 'gray' }
      return (
        <Space direction="vertical" size={2}>
          <Tag color={meta.tone} style={{ whiteSpace: 'nowrap' }}>{meta.label}</Tag>
          {st === 'SKIPPED' && record?.skipReason && (
            <Text type="secondary" style={{ fontSize: 11, display: 'block', maxWidth: 150 }} ellipsis={{ tooltip: record.skipReason }}>
              {record.skipReason}
            </Text>
          )}
        </Space>
      )
    },
  },
  {
    title: 'Thời gian đến',
    dataIndex: 'checkedInAt',
    key: 'checkedInAt',
    width: 160,
    render: (time) => (
      <span style={{ whiteSpace: 'nowrap' }}>
        {time ? dayjs(time).format('HH:mm DD/MM/YYYY') : '—'}
      </span>
    ),
  },
  {
    title: 'Thao tác',
    key: 'action',
    width: 190,
    render: (_, record) => {
      const pInfo = getPatientInfo(record.patientId, record.patientName)
      const dInfo = getDoctorInfo(record.doctorId, record.doctorName)

      const canManageReQueue =
        permissions.canCheckIn ||
        permissions.canUpdateQueueStatus ||
        permissions.isAdmin ||
        permissions.isReceptionist

      const canManagePrioritize = Boolean(permissions.isAdmin || permissions.isReceptionist)

      const menuItems = [
        {
          key: 'detail',
          icon: <EyeOutlined />,
          label: 'Xem chi tiết lượt khám',
          onClick: () => onOpenDetail && onOpenDetail(record, pInfo, dInfo),
        },
        canManagePrioritize &&
          canPrioritize(record.status) && {
            key: 'prioritize_menu',
            icon: <AlertOutlined style={{ color: '#dc2626' }} />,
            label: 'Đánh dấu ưu tiên khám',
            onClick: () => onPrioritize && onPrioritize(record),
          },
        permissions.canCallNext &&
          record.status === 'WAITING' && {
            key: 'call',
            icon: <StepForwardOutlined />,
            label: 'Gọi vào khám ngay',
            onClick: () => onCallNext && onCallNext(record.medicalQueueId || record.queueId || record.id),
          },
        permissions.canUpdateStatus &&
          record.status === 'IN_PROGRESS' && {
            key: 'wait_cdls',
            label: 'Chuyển sang Chờ kết quả CĐLS',
            onClick: () => onUpdateStatus && onUpdateStatus(record.id, 'WAITING_FOR_RESULT'),
          },
        permissions.canUpdateStatus &&
          record.status === 'WAITING_FOR_RESULT' && {
            key: 'resume',
            label: 'Tiếp tục khám bệnh',
            onClick: () => onUpdateStatus && onUpdateStatus(record.id, 'IN_PROGRESS'),
          },
        canManageReQueue &&
          record.status === 'SKIPPED' && {
            key: 're_queue_menu',
            icon: <ReloadOutlined />,
            label: 'Đưa lại vào hàng đợi',
            onClick: () => onReQueue && onReQueue(record),
          },
        permissions.canSkip &&
          record.status === 'IN_PROGRESS' && {
            type: 'divider',
          },
        permissions.canSkip &&
          record.status === 'IN_PROGRESS' && {
            key: 'skip',
            icon: <CloseCircleOutlined />,
            danger: true,
            label: 'Tạm hoãn lượt khám (Vắng mặt)',
            onClick: () => onSkip && onSkip(record),
          },
        canUserCloseVisit(user, record.doctorId) &&
          record.status === 'IN_PROGRESS' && {
            key: 'close_visit',
            icon: <StopOutlined />,
            danger: true,
            label: 'Kết thúc sớm / Hủy ca',
            onClick: () => onCloseVisit && onCloseVisit(record),
          },
        {
          type: 'divider',
        },
        onOpenHistory && {
          key: 'queue_history',
          icon: <HistoryOutlined />,
          label: 'Lịch sử luân chuyển hàng đợi',
          onClick: () => onOpenHistory(record),
        },
      ].filter(Boolean)

      const hasCallAction = permissions.canCallNext && record.status === 'WAITING'
      const hasReQueueAction = onReQueue && canManageReQueue && record.status === 'SKIPPED'
      const hasDeferAction = permissions.canSkip && record.status === 'IN_PROGRESS'

      return (
        <Space size="small">
          {hasCallAction && (
            <Button
              type="primary"
              size="small"
              icon={<StepForwardOutlined />}
              onClick={() => onCallNext && onCallNext(record.medicalQueueId || record.queueId || record.id)}
            >
              Gọi khám
            </Button>
          )}
          {hasReQueueAction && (
            <Button
              type="primary"
              size="small"
              style={{ backgroundColor: '#0284c7', borderColor: '#0284c7' }}
              icon={<ReloadOutlined />}
              loading={reQueuingId === record.id}
              onClick={() => onReQueue && onReQueue(record)}
            >
              Đưa lại hàng đợi
            </Button>
          )}
          {hasDeferAction && (
            <Button
              size="small"
              danger
              icon={<CloseCircleOutlined />}
              onClick={() => onSkip && onSkip(record)}
            >
              Tạm hoãn
            </Button>
          )}
          {canManagePrioritize && canPrioritize(record.status) && (
            <Button
              style={{
                height: 36,
                borderRadius: 8,
                borderColor: record.priority === 'EMERGENCY' ? '#fca5a5' : '#fed7aa',
                color: record.priority === 'EMERGENCY' ? '#dc2626' : '#ea580c',
                backgroundColor: record.priority === 'EMERGENCY' ? '#fef2f2' : '#fff7ed',
                fontWeight: 600,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
              }}
              icon={
                record.priority === 'EMERGENCY' ? (
                  <AlertOutlined style={{ fontSize: 16 }} />
                ) : (
                  <StarOutlined style={{ fontSize: 16 }} />
                )
              }
              onClick={() => onPrioritize && onPrioritize(record)}
            >
              {record.priority === 'EMERGENCY' ? 'Cấp cứu' : record.priority === 'PRIORITY' ? 'Ưu tiên' : 'Ưu tiên'}
            </Button>
          )}
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <Button
              style={{
                height: 36,
                width: 36,
                borderRadius: 8,
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
              icon={<MoreOutlined style={{ fontSize: 18 }} />}
              title="Thao tác"
            />
          </Dropdown>
        </Space>
      )
    },
  },
]
