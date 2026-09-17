import React from 'react'
import {
  Avatar,
  Button,
  Card,
  List,
  Tag,
  Typography,
} from 'antd'
import { HistoryOutlined } from '@ant-design/icons'
import { getInitials } from '../../utils/appointmentQueueUiHelpers'
import { formatVisitCode } from '../../utils/helpers'

const { Text } = Typography

export default function WaitingForResultList({
  items = [],
  getPatientInfo,
  permissions = {},
  onUpdateStatus,
  onOpenEncounter,
  onOpenHistory,
}) {
  return (
    <Card
      title={
        <Text strong style={{ color: '#9333ea' }}>
          🟣 BỆNH NHÂN CHỜ KẾT QUẢ CĐLS ({items.length})
        </Text>
      }
      style={{ borderRadius: 12, borderColor: '#e9d5ff' }}
    >
      <List
        dataSource={items}
        pagination={{ pageSize: 5 }}
        renderItem={(item) => {
          const pInfo = getPatientInfo(item.patientId, item.patientName)
          return (
            <List.Item
              actions={[
                permissions.canUpdateStatus && (
                  <Button
                    key="resume"
                    type="primary"
                    style={{ backgroundColor: '#16a34a' }}
                    onClick={() => onUpdateStatus && onUpdateStatus(item.id, 'IN_PROGRESS')}
                  >
                    Tiếp tục khám
                  </Button>
                ),
                <Button key="view-record" onClick={() => onOpenEncounter && onOpenEncounter(item)}>
                  Xem bệnh án
                </Button>,
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
                      backgroundColor: '#9333ea',
                      fontWeight: 700,
                      fontSize: 15,
                      boxShadow: '0 2px 5px rgba(0, 0, 0, 0.06)',
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
                        color="purple"
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
                    <Tag color="purple" style={{ margin: 0, fontSize: 11, borderRadius: 4, fontWeight: 600 }}>
                      Chờ kết quả CĐLS
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
                    <span style={{ color: '#7e22ce' }}>
                      Đang chờ phòng xét nghiệm / CĐHA trả kết quả
                    </span>
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
