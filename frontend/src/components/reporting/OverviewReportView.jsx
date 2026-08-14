import React from 'react'
import { Alert, Card, Col, Row, Typography } from 'antd'

const { Title } = Typography
const formatMoney = (v) => `${Number(v || 0).toLocaleString('vi-VN')} đ`

export default function OverviewReportView({ summary = {} }) {
  return (
    <Card style={{ borderRadius: 14, border: '1px solid #f1f5f9' }}>
      <Title level={4}>Tổng quan chỉ số vận hành ghi nhận thực tế</Title>
      <Alert
        type="success"
        showIcon
        message="Dữ liệu vận hành thực tế"
        description="Toàn bộ chỉ số được tổng hợp trực tiếp từ các lượt khám, đơn thuốc đã cấp, hóa đơn đã thu và nhật ký truy cập."
        style={{ marginBottom: 20 }}
      />
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Card title="Doanh thu thực tế đã thu" size="small">
            <Title level={3} style={{ color: '#16a34a', margin: 0 }}>
              {formatMoney(summary.revenue)}
            </Title>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="Số lượt khám đã ghi nhận" size="small">
            <Title level={3} style={{ color: '#2563eb', margin: 0 }}>
              {summary.visitCount || 0} lượt
            </Title>
          </Card>
        </Col>
      </Row>
    </Card>
  )
}
