import React from 'react'
import {
  Avatar,
  Badge,
  Button,
  Card,
  Dropdown,
  List,
  Modal,
  Tag,
  Typography,
} from 'antd'
import {
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  HistoryOutlined,
  MedicineBoxOutlined,
  MoreOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { getAvatarStyle, getInitials } from '../../utils/appointmentQueueUiHelpers'
import { formatVisitCode } from '../../utils/helpers'

const { Text } = Typography

export default function InProgressPatientList({
  items = [],
  getPatientInfo,
  permissions = {},
  onOpenEncounter,
  onOpenHistory,
  onUpdateStatus,
  onComplete,
  onSkip,
}) {
  return (
    <Card
      title={
        <Text strong style={{ color: '#16a34a' }}>
          🔴 BỆNH NHÂN ĐANG KHÁM ({items.length})
        </Text>
      }
      style={{ borderRadius: 12, borderColor: '#bbf7d0' }}
    >
      {items.length === 0 ? (
        <Text type="secondary">Chưa có bệnh nhân nào đang trong phòng khám.</Text>
      ) : (
        <List
          dataSource={items}
          renderItem={(item) => {
            const pInfo = getPatientInfo(item.patientId, item.patientName)
            const secondaryActions = [
              {
                key: 'history',
                label: 'Lịch sử khám của bệnh nhân',
                icon: <HistoryOutlined style={{ color: '#2563eb' }} />,
                onClick: () => onOpenHistory && onOpenHistory(item.patientId, pInfo.name, pInfo.code),
              },
              ...(permissions.canUpdateStatus
                ? [
                    {
                      key: 'wait-result',
                      label: 'Chờ kết quả cận lâm sàng',
                      onClick: () => onUpdateStatus && onUpdateStatus(item.id, 'WAITING_FOR_RESULT'),
                    },
                  ]
                : []),
              ...(permissions.canComplete
                ? [
                    {
                      key: 'complete',
                      label: 'Hoàn tất lượt khám',
                      icon: <CheckCircleOutlined />,
                      onClick: () =>
                        Modal.confirm({
                          title: 'Xác nhận hoàn tất lượt khám?',
                          content: 'Đảm bảo bệnh án đã được bác sĩ ký hoặc khóa trước khi hoàn tất.',
                          okText: 'Hoàn tất',
                          cancelText: 'Hủy',
                          onOk: () => onComplete && onComplete(item.id),
                        }),
                    },
                  ]
                : []),
              ...(permissions.canSkip
                ? [
                    {
                      key: 'skip',
                      label: 'Bỏ qua lượt khám',
                      icon: <CloseCircleOutlined />,
                      danger: true,
                      onClick: () => onSkip && onSkip(item),
                    },
                  ]
                : []),
            ]
            return (
              <List.Item
                actions={[
                  (permissions.isAdmin || permissions.isDoctor) && (
                    <Button
                      key="exam"
                      type="primary"
                      icon={<MedicineBoxOutlined />}
                      onClick={() => onOpenEncounter && onOpenEncounter(item)}
                    >
                      Ghi bệnh án / Khám
                    </Button>
                  ),
                  <Button
                    key="history"
                    icon={<HistoryOutlined />}
                    onClick={() => onOpenHistory && onOpenHistory(item.patientId, pInfo.name, pInfo.code)}
                  >
                    Lịch sử khám
                  </Button>,
                  secondaryActions.length > 0 && (
                    <Dropdown
                      key="more"
                      menu={{ items: secondaryActions }}
                      trigger={['click']}
                      placement="bottomRight"
                    >
                      <Button
                        icon={<MoreOutlined />}
                        aria-label={`Thao tác khác với bệnh nhân ${pInfo.name}`}
                      />
                    </Dropdown>
                  ),
                ].filter(Boolean)}
              >
                <List.Item.Meta
                  avatar={
                    <Badge status="processing" color="#16a34a" offset={[-2, 38]}>
                      <Avatar
                        size={46}
                        style={{
                          ...getAvatarStyle(pInfo.name),
                          fontWeight: 700,
                          fontSize: 16,
                          boxShadow: '0 2px 6px rgba(0, 0, 0, 0.08)',
                          border: '1px solid rgba(0, 0, 0, 0.06)',
                        }}
                      >
                        {getInitials(pInfo.name)}
                      </Avatar>
                    </Badge>
                  }
                  title={
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                      <Text strong style={{ fontSize: 15, color: '#0f172a' }}>
                        {pInfo.name}
                      </Text>
                      <Tag
                        color="blue"
                        style={{
                          fontWeight: 700,
                          fontSize: 12,
                          padding: '1px 8px',
                          borderRadius: 4,
                          margin: 0,
                        }}
                      >
                        STT #{String(item.queueNumber || 1).padStart(2, '0')}
                      </Tag>
                      {item.sourceType === 'WALK_IN' ? (
                        <Tag color="orange" style={{ margin: 0, fontSize: 11, borderRadius: 4 }}>
                          Tự đến
                        </Tag>
                      ) : (
                        <Tag color="cyan" style={{ margin: 0, fontSize: 11, borderRadius: 4 }}>
                          <CalendarOutlined style={{ marginRight: 4 }} />
                          Hẹn trước
                        </Tag>
                      )}
                      <Tag color="success" style={{ margin: 0, fontSize: 11, borderRadius: 4, fontWeight: 600 }}>
                        Đang khám
                      </Tag>
                    </div>
                  }
                  description={
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        marginTop: 4,
                        flexWrap: 'wrap',
                        fontSize: 12,
                      }}
                    >
                      <span style={{ color: '#64748b' }}>
                        Mã lượt:{' '}
                        <Tag
                          style={{
                            fontFamily: 'monospace',
                            fontWeight: 600,
                            fontSize: 11.5,
                            color: '#1d4ed8',
                            backgroundColor: '#eff6ff',
                            borderColor: '#bfdbfe',
                            borderRadius: 4,
                            margin: 0,
                            padding: '0 6px',
                          }}
                        >
                          {formatVisitCode(item.visitCode, item.visitId)}
                        </Tag>
                      </span>
                      {pInfo.code && pInfo.code !== '—' && (
                        <span style={{ color: '#64748b' }}>
                          Mã BN: <Text strong style={{ color: '#334155' }}>{pInfo.code}</Text>
                        </span>
                      )}
                      {pInfo.phone && (
                        <span style={{ color: '#64748b' }}>
                          SĐT: <Text style={{ color: '#334155' }}>{pInfo.phone}</Text>
                        </span>
                      )}
                      {item.checkedInAt && (
                        <span style={{ color: '#64748b', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                          <ClockCircleOutlined style={{ fontSize: 11, color: '#94a3b8' }} />
                          <span>Vào lúc: {dayjs(item.checkedInAt).format('HH:mm')}</span>
                        </span>
                      )}
                    </div>
                  }
                />
              </List.Item>
            )
          }}
        />
      )}
    </Card>
  )
}
