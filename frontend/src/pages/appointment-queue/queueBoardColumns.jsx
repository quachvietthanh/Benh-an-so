import React from 'react'
import { Avatar, Badge, Button, Dropdown, Space, Tag, Typography } from 'antd'
import {
  CloseCircleOutlined,
  EyeOutlined,
  MoreOutlined,
  StepForwardOutlined,
  StopOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { getAvatarStyle, getInitials } from '../../utils/appointmentQueueUiHelpers'
import { QUEUE_STATUS_META } from '../../utils/queueHelpers'
import { canUserCloseVisit } from '../../utils/closeVisitHelpers'

const { Text } = Typography

export const getQueueBoardColumns = ({
  getPatientInfo,
  getDoctorInfo,
  permissions = {},
  user,
  onOpenDetail,
  onCallNext,
  onUpdateStatus,
  onSkip,
  onCloseVisit,
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
    width: 240,
    render: (_, record) => {
      const pInfo = getPatientInfo(record.patientId, record.patientName, record.patientCode, record.phone)
      return (
        <Space align="center" size="small">
          <Avatar style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 150 }}>
            <Text strong style={{ fontSize: 14, color: '#0f172a', lineHeight: '1.4' }}>
              {pInfo.name}
            </Text>
            <Text type="secondary" style={{ fontSize: 12, lineHeight: '1.2' }}>
              Mã: {pInfo.code}
            </Text>
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
    width: 150,
    render: (st) => {
      const meta = QUEUE_STATUS_META[st] || { label: 'Không xác định', tone: 'gray' }
      return <Tag color={meta.tone} style={{ whiteSpace: 'nowrap' }}>{meta.label}</Tag>
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
    width: 150,
    render: (_, record) => {
      const pInfo = getPatientInfo(record.patientId, record.patientName)
      const dInfo = getDoctorInfo(record.doctorId, record.doctorName)

      const menuItems = [
        {
          key: 'detail',
          icon: <EyeOutlined />,
          label: 'Xem chi tiết lượt khám',
          onClick: () => onOpenDetail && onOpenDetail(record, pInfo, dInfo),
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
        permissions.canSkip &&
          record.status === 'IN_PROGRESS' && {
            type: 'divider',
          },
        permissions.canSkip &&
          record.status === 'IN_PROGRESS' && {
            key: 'skip',
            icon: <CloseCircleOutlined />,
            danger: true,
            label: 'Bỏ qua lượt (Vắng mặt)',
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
      ].filter(Boolean)

      const hasCallAction = permissions.canCallNext && record.status === 'WAITING'

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
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <Button size="small" icon={<MoreOutlined style={{ fontSize: 16 }} />} title="Thao tác" />
          </Dropdown>
        </Space>
      )
    },
  },
]
