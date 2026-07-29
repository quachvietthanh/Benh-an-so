import React from 'react'
import { Card, Col, Row, Statistic } from 'antd'
import { AlertOutlined, CheckCircleOutlined, CloudUploadOutlined, FileTextOutlined } from '@ant-design/icons'

function AttachmentStatsCards({ stats }) {
  if (!stats) return null

  return (
    <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
      <Col xs={12} sm={6}>
        <Card bodyStyle={{ padding: 16 }} style={{ borderRadius: 8, background: '#f8fafc', border: '1px solid #e2e8f0' }}>
          <Statistic
            title={<span style={{ color: '#475569', fontSize: 13 }}>Tổng tệp kết quả</span>}
            value={stats.total}
            prefix={<FileTextOutlined style={{ color: '#2563eb' }} />}
          />
        </Card>
      </Col>
      <Col xs={12} sm={6}>
        <Card bodyStyle={{ padding: 16 }} style={{ borderRadius: 8, background: '#fef2f2', border: '1px solid #fecdd3' }}>
          <Statistic
            title={<span style={{ color: '#9f1239', fontSize: 13 }}>Cần chú ý / Bất thường</span>}
            value={stats.abnormal}
            valueStyle={{ color: '#dc2626' }}
            prefix={<AlertOutlined style={{ color: '#dc2626' }} />}
          />
        </Card>
      </Col>
      <Col xs={12} sm={6}>
        <Card bodyStyle={{ padding: 16 }} style={{ borderRadius: 8, background: '#f0fdf4', border: '1px solid #bbf7d0' }}>
          <Statistic
            title={<span style={{ color: '#166534', fontSize: 13 }}>Kết quả bình thường</span>}
            value={stats.normal}
            valueStyle={{ color: '#16a34a' }}
            prefix={<CheckCircleOutlined style={{ color: '#16a34a' }} />}
          />
        </Card>
      </Col>
      <Col xs={12} sm={6}>
        <Card bodyStyle={{ padding: 16 }} style={{ borderRadius: 8, background: '#eff6ff', border: '1px solid #bfdbfe' }}>
          <Statistic
            title={<span style={{ color: '#1e40af', fontSize: 13 }}>Tải lên hôm nay</span>}
            value={stats.today}
            valueStyle={{ color: '#2563eb' }}
            prefix={<CloudUploadOutlined style={{ color: '#2563eb' }} />}
          />
        </Card>
      </Col>
    </Row>
  )
}

export default AttachmentStatsCards
