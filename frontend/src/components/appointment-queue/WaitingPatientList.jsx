import React from 'react'
import {
  Avatar,
  Button,
  Card,
  List,
  Tag,
  Typography,
} from 'antd'
import {
  AlertOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  HistoryOutlined,
  StarOutlined,
  StepForwardOutlined,
  SwapOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { getAvatarStyle, getInitials } from '../../utils/appointmentQueueUiHelpers'
import { formatVisitCode } from '../../utils/helpers'
import { getPriorityTag } from '../../utils/queuePriorityHelpers.js'

const { Text } = Typography

export default function WaitingPatientList({
  items = [],
  getPatientInfo,
  permissions = {},
  onCallNext,
  onOpenHistory,
}) {
  return (
    <Card
      title={
        <Text strong style={{ color: '#2563eb' }}>
          🟡 BỆNH NHÂN ĐANG CHỜ ({items.length})
        </Text>
      }
      style={{ borderRadius: 12 }}
    >
      <List
        dataSource={items}
        pagination={{ pageSize: 5 }}
        renderItem={(item) => {
          const pInfo = getPatientInfo(item.patientId, item.patientName)
          const priorityTag = getPriorityTag(item.priority)
          const isEmergency = item.priority === 'EMERGENCY'

          return (
            <List.Item
              style={{
                backgroundColor: isEmergency ? '#fff1f2' : undefined,
                borderLeft: isEmergency ? '4px solid #dc2626' : undefined,
                paddingLeft: isEmergency ? 12 : undefined,
                borderRadius: isEmergency ? 8 : undefined,
                marginBottom: isEmergency ? 6 : undefined,
                transition: 'all 0.2s',
              }}
              actions={[
                permissions.canCallNext && (
                  <Button
                    key="call-next"
                    type="primary"
                    danger={isEmergency}
                    icon={<StepForwardOutlined />}
                    onClick={() => onCallNext && onCallNext(item.medicalQueueId || item.queueId)}
                  >
                    Gọi vào khám
                  </Button>
                ),
                <Button
                  key="history"
                  icon={<HistoryOutlined />}
                  onClick={() => onOpenHistory && onOpenHistory(item.patientId, pInfo.name, pInfo.code)}
                >
                  Lịch sử
                </Button>,
              ].filter(Boolean)}
            >
              <List.Item.Meta
                avatar={
                  <Avatar
                    size={44}
                    style={{
                      ...getAvatarStyle(pInfo.name),
                      fontWeight: 700,
                      fontSize: 15,
                      boxShadow: '0 2px 5px rgba(0, 0, 0, 0.06)',
                      border: isEmergency ? '2px solid #dc2626' : '1px solid rgba(0, 0, 0, 0.05)',
                    }}
                  >
                    {getInitials(pInfo.name)}
                  </Avatar>
                }
                title={
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                    <Text strong style={{ fontSize: 15, color: '#0f172a' }}>
                      {pInfo.name}
                    </Text>
                    {priorityTag && (
                      <Tag
                        color={priorityTag.color}
                        style={{
                          margin: 0,
                          fontWeight: 700,
                          fontSize: 11.5,
                          padding: '1px 8px',
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
                    )}
                    <Tag
                      color="gold"
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
                    {item.isHandoverToMe && (
                      <Tag color="cyan" style={{ margin: 0, fontSize: 11, borderRadius: 4, fontWeight: 600, background: '#e0f2fe', borderColor: '#7dd3fc', color: '#0369a1' }}>
                        <SwapOutlined style={{ marginRight: 4 }} />
                        Tiếp nhận từ BS: {item.fromDoctorName || 'Đồng nghiệp'}
                      </Tag>
                    )}
                    <Tag color="warning" style={{ margin: 0, fontSize: 11, borderRadius: 4 }}>
                      Chờ gọi
                    </Tag>
                  </div>
                }
                description={
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 4 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap', fontSize: 12 }}>
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
                      {item.checkedInAt && (
                        <span style={{ color: '#64748b', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                          <ClockCircleOutlined style={{ fontSize: 11, color: '#94a3b8' }} />
                          <span>Đến lúc: {dayjs(item.checkedInAt).format('HH:mm DD/MM')}</span>
                        </span>
                      )}
                    </div>
                    {item.priorityReason && (
                      <div style={{ fontSize: 12, color: isEmergency ? '#dc2626' : '#d97706', fontWeight: 500 }}>
                        <span>Lý do ưu tiên: {item.priorityReason}</span>
                      </div>
                    )}
                  </div>
                }
              />
            </List.Item>
          )
        }}
      />
    </Card>
  )
}
