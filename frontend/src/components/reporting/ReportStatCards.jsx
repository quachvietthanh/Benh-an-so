import React from 'react'
import { Col, Row } from 'antd'
import {
  CalendarOutlined,
  DollarCircleOutlined,
  FileSearchOutlined,
  MedicineBoxOutlined,
} from '@ant-design/icons'

const formatMoney = (v) => `${Number(v || 0).toLocaleString('vi-VN')} đ`

export default function ReportStatCards({ summary = {} }) {
  return (
    <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
      {/* Card 1: Tổng lượt khám */}
      <Col xs={12} sm={12} md={6}>
        <div className="reports-stat-card">
          <div className="reports-stat-icon blue">
            <CalendarOutlined />
          </div>
          <div>
            <div className="reports-stat-label">Tổng lượt khám</div>
            <div className="reports-stat-value blue">
              {summary.visitCount || 0} lượt
            </div>
            <div className="reports-stat-sub">Ghi nhận từ bệnh án</div>
          </div>
        </div>
      </Col>

      {/* Card 2: Doanh thu phòng khám */}
      <Col xs={12} sm={12} md={6}>
        <div className="reports-stat-card">
          <div className="reports-stat-icon green">
            <DollarCircleOutlined />
          </div>
          <div>
            <div className="reports-stat-label">Doanh thu phòng khám</div>
            <div className="reports-stat-value green">
              {formatMoney(summary.revenue)}
            </div>
            <div className="reports-stat-sub">Ghi nhận từ hóa đơn</div>
          </div>
        </div>
      </Col>

      {/* Card 3: Đơn thuốc đã cấp */}
      <Col xs={12} sm={12} md={6}>
        <div className="reports-stat-card">
          <div className="reports-stat-icon orange">
            <MedicineBoxOutlined />
          </div>
          <div>
            <div className="reports-stat-label">Đơn thuốc đã cấp</div>
            <div className="reports-stat-value orange">
              {summary.dispensedCount || 0} đơn
            </div>
            <div className="reports-stat-sub">Ghi nhận từ kho thuốc</div>
          </div>
        </div>
      </Col>

      {/* Card 4: Nhật ký truy cập */}
      <Col xs={12} sm={12} md={6}>
        <div className="reports-stat-card">
          <div className="reports-stat-icon purple">
            <FileSearchOutlined />
          </div>
          <div>
            <div className="reports-stat-label">Nhật ký truy cập</div>
            <div className="reports-stat-value purple">
              {summary.auditCount || 0} lượt
            </div>
            <div className="reports-stat-sub">Ghi nhận truy cập y tế</div>
          </div>
        </div>
      </Col>
    </Row>
  )
}
