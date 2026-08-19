import React, { useEffect, useState } from 'react'
import { Badge, Card, Col, Row, Tag, Typography } from 'antd'
import { BellOutlined, SoundOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import queueApi from '../api/queueApi'

const { Title, Text } = Typography

function PublicQueueDisplay() {
  const [queueItems, setQueueItems] = useState([])
  const [currentTime, setCurrentTime] = useState(dayjs())

  const fetchQueue = async () => {
    try {
      const res = await queueApi.getQueues({ date: dayjs().format('YYYY-MM-DD') })
      if (res?.data && Array.isArray(res.data)) {
        setQueueItems(res.data)
      }
    } catch {
    }
  }

  useEffect(() => {
    fetchQueue()
    const timer = setInterval(() => {
      setCurrentTime(dayjs())
      fetchQueue()
    }, 10000)
    return () => clearInterval(timer)
  }, [])

  const currentCalling = queueItems.filter((i) => i.status === 'CALLED' || i.status === 'IN_PROGRESS')
  const waitingList = queueItems.filter((i) => i.status === 'CHECKED_IN' || i.status === 'WAITING')

  return (
    <div style={{ background: '#0f172a', minHeight: '100vh', padding: 24, color: '#fff' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, paddingBottom: 16, borderBottom: '2px solid #334155' }}>
        <div>
          <Title level={2} style={{ color: '#38bdf8', margin: 0 }}>
            <SoundOutlined style={{ marginRight: 12 }} />
            HỆ THỐNG GỌI SỐ PHÒNG KHÁM BỆNH
          </Title>
          <Text style={{ color: '#94a3b8', fontSize: 16 }}>Bệnh án số - Bệnh viện / Phòng khám Đa khoa</Text>
        </div>
        <div style={{ textAlign: 'right' }}>
          <Title level={3} style={{ color: '#f8fafc', margin: 0 }}>
            {currentTime.format('HH:mm:ss')}
          </Title>
          <Text style={{ color: '#94a3b8', fontSize: 16 }}>
            {currentTime.format('DD/MM/YYYY')}
          </Text>
        </div>
      </div>

      <Row gutter={[24, 24]}>
        <Col span={16}>
          <Card title={<span style={{ color: '#38bdf8', fontSize: 20, fontWeight: 700 }}><BellOutlined /> ĐANG KHÁM VÀ GỌI SỐ</span>} style={{ background: '#1e293b', borderColor: '#334155' }}>
            {currentCalling.length === 0 ? (
              <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>
                <Title level={4} style={{ color: '#64748b' }}>Chưa có bệnh nhân nào đang gọi</Title>
              </div>
            ) : (
              <Row gutter={[16, 16]}>
                {currentCalling.map((item, idx) => (
                  <Col span={12} key={item.id || idx}>
                    <div style={{ background: '#0284c7', padding: 20, borderRadius: 12, textAlign: 'center', color: '#fff' }}>
                      <Text style={{ color: '#e0f2fe', fontSize: 16, display: 'block' }}>PHÒNG KHÁM: {item.roomNumber || item.department || 'Phòng 01'}</Text>
                      <div style={{ fontSize: 56, fontWeight: 800, color: '#ffffff', margin: '8px 0' }}>
                        STT: {item.queueNumber || idx + 1}
                      </div>
                      <Title level={3} style={{ color: '#ffffff', margin: 0 }}>
                        {item.patientName}
                      </Title>
                      <Tag color="gold" style={{ marginTop: 12, fontSize: 14, padding: '4px 12px' }}>
                        BS. {item.doctorName || 'Nguyễn Văn A'}
                      </Tag>
                    </div>
                  </Col>
                ))}
              </Row>
            )}
          </Card>
        </Col>

        <Col span={8}>
          <Card title={<span style={{ color: '#f59e0b', fontSize: 20, fontWeight: 700 }}>DANH SÁCH CHỜ TIẾP THEO</span>} style={{ background: '#1e293b', borderColor: '#334155' }}>
            {waitingList.length === 0 ? (
              <Text style={{ color: '#64748b' }}>Không có bệnh nhân chờ</Text>
            ) : (
              <div style={{ maxHeight: 500, overflowY: 'auto' }}>
                {waitingList.slice(0, 10).map((item, idx) => (
                  <div key={item.id || idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 16px', background: '#0f172a', borderRadius: 8, marginBottom: 8, borderLeft: '4px solid #f59e0b' }}>
                    <div>
                      <Text style={{ color: '#f8fafc', fontWeight: 600, fontSize: 16, display: 'block' }}>
                        STT {item.queueNumber || idx + 1}: {item.patientName}
                      </Text>
                      <Text style={{ color: '#94a3b8', fontSize: 13 }}>
                        {item.department || 'Khám tổng quát'}
                      </Text>
                    </div>
                    <Badge status="processing" text={<span style={{ color: '#f59e0b' }}>Đang chờ</span>} />
                  </div>
                ))}
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default PublicQueueDisplay
