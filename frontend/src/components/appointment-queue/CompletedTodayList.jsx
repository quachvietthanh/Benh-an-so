import React from 'react'
import {
  Avatar,
  Button,
  Card,
  Empty,
  List,
  Tag,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  FileTextOutlined,
  HistoryOutlined,
  StopOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { getInitials } from '../../utils/appointmentQueueUiHelpers'
import { formatVisitCode } from '../../utils/helpers'

const { Text } = Typography

export default function CompletedTodayList({
  items = [],
  getPatientInfo,
  permissions = {},
  selectedDate,
  onOpenEncounter,
  onOpenHistory,
}) {
  const dateDisplay = selectedDate ? dayjs(selectedDate).format('DD/MM/YYYY') : dayjs().format('DD/MM/YYYY')

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
          <Text strong style={{ color: '#16a34a' }}>
            🟢 BỆNH NHÂN ĐÃ KHÁM XONG TRONG NGÀY ({items.length})
          </Text>
          <Tag color="success" style={{ margin: 0, fontWeight: 600 }}>
            Ngày {dateDisplay}
          </Tag>
        </div>
      }
      style={{ borderRadius: 12, borderColor: '#bbf7d0', backgroundColor: '#fafffa' }}
    >
      {items.length === 0 ? (
        <div style={{ padding: '24px 0', textAlign: 'center' }}>
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={
              <span style={{ color: '#64748b' }}>
                Chưa có bệnh nhân nào hoàn tất khám trong ngày {dateDisplay}.
              </span>
            }
          />
        </div>
      ) : (
        <List
          dataSource={items}
          pagination={{ pageSize: 5 }}
          renderItem={(item) => {
            const pInfo = getPatientInfo(item.patientId, item.patientName)
            return (
              <List.Item
                actions={[
                  <Button
                    key="view-record"
                    icon={<FileTextOutlined />}
                    onClick={() => onOpenEncounter && onOpenEncounter(item)}
                  >
                    Xem lại bệnh án
                  </Button>,
                  <Button
                    key="view-history"
                    type="primary"
                    ghost
                    icon={<HistoryOutlined />}
                    onClick={() => onOpenHistory && onOpenHistory(item.patientId, pInfo.name, pInfo.code)}
                  >
                    Lịch sử khám
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <Avatar
                      size={44}
                      style={{
                        backgroundColor: '#16a34a',
                        fontWeight: 700,
                        fontSize: 15,
                        boxShadow: '0 2px 5px rgba(22, 163, 74, 0.2)',
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
                      {item.queueNumber && (
                        <Tag
                          color="green"
                          style={{
                            fontWeight: 700,
                            fontSize: 12,
                            padding: '1px 8px',
                            borderRadius: 4,
                            margin: 0,
                          }}
                        >
                          STT #{String(item.queueNumber).padStart(2, '0')}
                        </Tag>
                      )}
                      <Tag color="success" style={{ margin: 0, fontSize: 11, borderRadius: 4, fontWeight: 600 }}>
                        <CheckCircleOutlined style={{ marginRight: 4 }} />
                        Đã hoàn thành
                      </Tag>
                    </div>
                  }
                  description={
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 4, flexWrap: 'wrap', fontSize: 12 }}>
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
                      {(() => {
                        const finishTime = item.completedAt || item.cancelledAt || item.skippedAt || item.updatedAt
                        if (!finishTime) return null
                        const isCompleted = item.status === 'COMPLETED'
                        const isEarly = item.status === 'EARLY_ENDED'
                        const label = isCompleted ? 'Hoàn tất lúc:' : isEarly ? 'Kết thúc lúc:' : 'Hủy lúc:'
                        const color = isCompleted ? '#15803d' : isEarly ? '#ea580c' : '#dc2626'
                        const IconComponent = isCompleted ? CheckCircleOutlined : isEarly ? StopOutlined : CloseCircleOutlined
                        return (
                          <span style={{ color, display: 'inline-flex', alignItems: 'center', gap: 4, fontWeight: 500 }}>
                            <IconComponent style={{ fontSize: 12, color }} />
                            <span>{label} {dayjs(finishTime).format('HH:mm')}</span>
                          </span>
                        )
                      })()}
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
