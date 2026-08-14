import React from 'react'
import { Card, Col, Progress, Row, Space, Tag } from 'antd'
import { WalletOutlined } from '@ant-design/icons'
import { formatMoney } from '../../../utils/revenueHelpers'

export default function RevenuePaymentBreakdown({ paymentBreakdown = [], netRevenue = 0 }) {
  return (
    <Card
      style={{ borderRadius: 14, border: '1px solid #f1f5f9', marginBottom: 20 }}
      title={
        <Space>
          <WalletOutlined style={{ color: '#0284c7' }} />
          <span style={{ fontWeight: 700 }}>Cơ cấu Doanh thu theo Phương thức thanh toán</span>
        </Space>
      }
    >
      <Row gutter={[16, 16]}>
        {paymentBreakdown.map((item, idx) => {
          const percent = netRevenue > 0 ? (item.amount / netRevenue) * 100 : 0
          return (
            <Col xs={24} sm={12} md={6} key={idx}>
              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: 10,
                  padding: '12px 16px',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <Tag color={item.color} style={{ fontWeight: 600 }}>
                    {item.label}
                  </Tag>
                  <span style={{ fontSize: 12, color: '#64748b' }}>
                    {item.count} giao dịch
                  </span>
                </div>
                <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a', marginBottom: 4 }}>
                  {formatMoney(item.amount)}
                </div>
                <Progress
                  percent={Number(percent.toFixed(1))}
                  size="small"
                  strokeColor={item.color}
                />
              </div>
            </Col>
          )
        })}
      </Row>
    </Card>
  )
}
