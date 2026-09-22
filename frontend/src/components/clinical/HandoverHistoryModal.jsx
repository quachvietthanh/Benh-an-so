import React, { useEffect, useState } from 'react'
import { Modal, Button, Timeline, Spin, Empty, Tag, Card, Typography } from 'antd'
import { HistoryOutlined, SwapOutlined, UserOutlined, ClockCircleOutlined } from '@ant-design/icons'
import visitApi from '../../api/visitApi.js'
import { formatHandoverDateTime } from '../../utils/handoverValidation.js'
import { getApiErrorMessage } from '../../utils/apiError.js'

const { Text } = Typography

export default function HandoverHistoryModal({
  open,
  onClose,
  visitId,
  patientName,
  patientCode,
  visitCode,
}) {
  const [handovers, setHandovers] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (open && visitId) {
      fetchHandovers()
    } else {
      setHandovers([])
      setError('')
    }
  }, [open, visitId])

  const fetchHandovers = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await visitApi.getVisitHandovers(visitId)
      setHandovers(Array.isArray(res.data) ? res.data : [])
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể tải lịch sử bàn giao ca khám.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={680}
      style={{ top: 24 }}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <HistoryOutlined style={{ color: '#0284c7', fontSize: 20 }} />
          <span style={{ fontSize: 16, fontWeight: 700 }}>
            Lịch sử bàn giao ca khám
          </span>
        </div>
      }
      footer={
        <div style={{ display: 'flex', justifyContent: 'center', width: '100%', padding: '6px 0' }}>
          <Button
            onClick={onClose}
            style={{
              height: 46,
              minWidth: 130,
              borderRadius: 8,
              fontSize: 15,
              fontWeight: 600,
            }}
          >
            Đóng
          </Button>
        </div>
      }
    >
      {(patientName || visitCode) && (
        <Card
          size="small"
          style={{
            marginBottom: 16,
            background: '#f8fafc',
            borderColor: '#e2e8f0',
            borderRadius: 8,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
            <div>
              <Text type="secondary">Bệnh nhân: </Text>
              <Text strong>{patientName || '---'}</Text>
              {patientCode && <Text type="secondary"> ({patientCode})</Text>}
            </div>
            {visitCode && (
              <div>
                <Text type="secondary">Mã lượt khám: </Text>
                <Tag color="blue" style={{ fontFamily: 'monospace', fontWeight: 700, margin: 0 }}>
                  {visitCode}
                </Tag>
              </div>
            )}
          </div>
        </Card>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin tip="Đang tải lịch sử bàn giao..." />
        </div>
      ) : error ? (
        <div style={{ textAlign: 'center', padding: '30px 0', color: '#ef4444' }}>
          {error}
        </div>
      ) : handovers.length === 0 ? (
        <Empty
          description="Lượt khám này chưa từng được bàn giao bác sĩ"
          style={{ margin: '40px 0' }}
        />
      ) : (
        <div style={{ maxHeight: '60vh', overflowY: 'auto', padding: '12px 8px 4px 8px' }}>
          <Timeline
            items={handovers.map((item, idx) => ({
              color: '#0284c7',
              dot: <SwapOutlined style={{ fontSize: 16, color: '#0284c7' }} />,
              children: (
                <div
                  key={item.id || idx}
                  style={{
                    background: '#ffffff',
                    border: '1px solid #e2e8f0',
                    borderRadius: 8,
                    padding: 12,
                    marginBottom: 8,
                    boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      marginBottom: 6,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 700 }}>
                      <Tag color="geekblue" style={{ margin: 0 }}>
                        Lần {handovers.length - idx}
                      </Tag>
                      <span>{item.fromDoctorName || 'Bác sĩ phụ trách trước'}</span>
                      <SwapOutlined style={{ color: '#0284c7' }} />
                      <span style={{ color: '#0284c7' }}>{item.toDoctorName || 'Bác sĩ tiếp nhận'}</span>
                    </div>
                    <div style={{ fontSize: 12, color: '#64748b', display: 'flex', alignItems: 'center', gap: 4 }}>
                      <ClockCircleOutlined />
                      <span>{formatHandoverDateTime(item.handedOverAt)}</span>
                    </div>
                  </div>

                  <div style={{ background: '#f8fafc', padding: '8px 12px', borderRadius: 6, fontSize: 13 }}>
                    <Text type="secondary">Lý do bàn giao: </Text>
                    <Text italic>"{item.reason || 'Không có lý do chi tiết'}"</Text>
                  </div>
                </div>
              ),
            }))}
          />
        </div>
      )}
    </Modal>
  )
}
