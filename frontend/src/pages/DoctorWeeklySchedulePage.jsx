import React from 'react'
import { Card, Typography } from 'antd'
import { CalendarOutlined } from '@ant-design/icons'
import DoctorWeeklyScheduleTable from '../components/appointment/DoctorWeeklyScheduleTable.jsx'

const { Title } = Typography

export default function DoctorWeeklySchedulePage() {
  return (
    <div style={{ padding: '0 0 24px 0' }}>
      {/* TIÊU ĐỀ TRANG RIÊNG BIỆT */}
      <Card
        style={{
          borderRadius: 12,
          marginBottom: 16,
          background: 'linear-gradient(135deg, #ffffff 0%, #f8fafc 100%)',
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
        }}
        bodyStyle={{ padding: '14px 20px' }}
      >
        <Title level={2} style={{ margin: 0, fontSize: 22, fontWeight: 700, color: '#0f172a' }}>
          <CalendarOutlined style={{ color: '#2563eb', marginRight: 8 }} /> Lịch tuần theo bác sĩ
        </Title>
      </Card>

      {/* 3. BẢNG MA TRẬN LỊCH TUẦN */}
      <DoctorWeeklyScheduleTable />
    </div>
  )
}
