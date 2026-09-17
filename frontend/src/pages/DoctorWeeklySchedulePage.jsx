import React from 'react'
import { Breadcrumb, Card, Typography } from 'antd'
import {
  CalendarOutlined,
  HomeOutlined,
  TableOutlined,
} from '@ant-design/icons'
import DoctorWeeklyScheduleTable from '../components/appointment/DoctorWeeklyScheduleTable.jsx'

const { Title, Paragraph, Text } = Typography

export default function DoctorWeeklySchedulePage() {
  return (
    <div style={{ padding: '0 0 24px 0' }}>
      {/* 1. BREADCRUMB */}
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={[
          {
            href: '/',
            title: (
              <span>
                <HomeOutlined style={{ marginRight: 4 }} />
                Trang chủ
              </span>
            ),
          },
          {
            title: 'Tiếp nhận & Chăm sóc',
          },
          {
            title: (
              <span>
                <TableOutlined style={{ marginRight: 4 }} />
                Lịch tuần theo bác sĩ
              </span>
            ),
          },
        ]}
      />

      {/* 2. TIÊU ĐỀ TRANG RIÊNG BIỆT */}
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
        <div style={{ color: '#2563eb', fontWeight: 600, fontSize: 12, textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: 2 }}>
          ĐIỀU PHỐI LỊCH KHÁM
        </div>
        <Title level={2} style={{ margin: 0, fontSize: 22, fontWeight: 700, color: '#0f172a' }}>
          Lịch tuần bác sĩ
        </Title>
      </Card>

      {/* 3. BẢNG MA TRẬN LỊCH TUẦN */}
      <DoctorWeeklyScheduleTable />
    </div>
  )
}
