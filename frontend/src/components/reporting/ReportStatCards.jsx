import React from 'react'
import { Col, Row } from 'antd'
import {
  CalendarOutlined,
  DollarCircleOutlined,
  FileSearchOutlined,
  MedicineBoxOutlined,
} from '@ant-design/icons'

const formatMoney = (v) => `${Number(v || 0).toLocaleString('vi-VN')} đ`

export default function ReportStatCards({ summary = {}, activeTab, onSelectTab }) {
  return (
    <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
      <Col xs={12} sm={12} md={6}>
        <div
          className={`reports-stat-card ${activeTab === 'visits' ? 'active' : ''}`}
          onClick={() => onSelectTab?.('visits')}
          style={{
            cursor: 'pointer',
            border: activeTab === 'visits' ? '2px solid #2563eb' : '1px solid #f1f5f9',
            backgroundColor: activeTab === 'visits' ? '#eff6ff' : '#ffffff',
            borderRadius: 12,
            padding: '14px 16px',
            display: 'flex',
            alignItems: 'center',
            boxShadow: activeTab === 'visits' ? '0 4px 12px rgba(37,99,235,0.12)' : '0 2px 8px rgba(0,0,0,0.03)',
            transition: 'all 0.2s ease',
          }}
        >
          <div className="reports-stat-icon blue">
            <CalendarOutlined />
          </div>
          <div>
            <div className="reports-stat-label">Tổng lượt khám</div>
            <div className="reports-stat-value blue">
              {summary.visitCount || 0} lượt
            </div>
            <div className="reports-stat-sub" style={{ fontSize: 11.5, color: '#64748b' }}>
              Bấm để xem chi tiết →
            </div>
          </div>
        </div>
      </Col>

      <Col xs={12} sm={12} md={6}>
        <div
          className={`reports-stat-card ${activeTab === 'revenue' ? 'active' : ''}`}
          onClick={() => onSelectTab?.('revenue')}
          style={{
            cursor: 'pointer',
            border: activeTab === 'revenue' ? '2px solid #16a34a' : '1px solid #f1f5f9',
            backgroundColor: activeTab === 'revenue' ? '#f0fdf4' : '#ffffff',
            borderRadius: 12,
            padding: '14px 16px',
            display: 'flex',
            alignItems: 'center',
            boxShadow: activeTab === 'revenue' ? '0 4px 12px rgba(22,163,74,0.12)' : '0 2px 8px rgba(0,0,0,0.03)',
            transition: 'all 0.2s ease',
          }}
        >
          <div className="reports-stat-icon green">
            <DollarCircleOutlined />
          </div>
          <div>
            <div className="reports-stat-label">Doanh thu phòng khám</div>
            <div className="reports-stat-value green">
              {formatMoney(summary.revenue)}
            </div>
            <div className="reports-stat-sub" style={{ fontSize: 11.5, color: '#64748b' }}>
              Bấm để xem chi tiết →
            </div>
          </div>
        </div>
      </Col>

      <Col xs={12} sm={12} md={6}>
        <div
          className={`reports-stat-card ${activeTab === 'medicines' ? 'active' : ''}`}
          onClick={() => onSelectTab?.('medicines')}
          style={{
            cursor: 'pointer',
            border: activeTab === 'medicines' ? '2px solid #d97706' : '1px solid #f1f5f9',
            backgroundColor: activeTab === 'medicines' ? '#fffbeb' : '#ffffff',
            borderRadius: 12,
            padding: '14px 16px',
            display: 'flex',
            alignItems: 'center',
            boxShadow: activeTab === 'medicines' ? '0 4px 12px rgba(217,119,6,0.12)' : '0 2px 8px rgba(0,0,0,0.03)',
            transition: 'all 0.2s ease',
          }}
        >
          <div className="reports-stat-icon orange">
            <MedicineBoxOutlined />
          </div>
          <div>
            <div className="reports-stat-label">Đơn thuốc đã cấp</div>
            <div className="reports-stat-value orange">
              {summary.dispensedCount || 0} đơn
            </div>
            <div className="reports-stat-sub" style={{ fontSize: 11.5, color: '#64748b' }}>
              Bấm để xem chi tiết →
            </div>
          </div>
        </div>
      </Col>

      <Col xs={12} sm={12} md={6}>
        <div
          className={`reports-stat-card ${activeTab === 'audit' ? 'active' : ''}`}
          onClick={() => onSelectTab?.('audit')}
          style={{
            cursor: 'pointer',
            border: activeTab === 'audit' ? '2px solid #7c3aed' : '1px solid #f1f5f9',
            backgroundColor: activeTab === 'audit' ? '#faf5ff' : '#ffffff',
            borderRadius: 12,
            padding: '14px 16px',
            display: 'flex',
            alignItems: 'center',
            boxShadow: activeTab === 'audit' ? '0 4px 12px rgba(124,58,237,0.12)' : '0 2px 8px rgba(0,0,0,0.03)',
            transition: 'all 0.2s ease',
          }}
        >
          <div className="reports-stat-icon purple">
            <FileSearchOutlined />
          </div>
          <div>
            <div className="reports-stat-label">Nhật ký truy cập</div>
            <div className="reports-stat-value purple">
              {summary.auditCount || 0} lượt
            </div>
            <div className="reports-stat-sub" style={{ fontSize: 11.5, color: '#64748b' }}>
              Bấm để xem chi tiết →
            </div>
          </div>
        </div>
      </Col>
    </Row>
  )
}
