import React from 'react'
import { Card, Row, Col, Input, Select, Table } from 'antd'
import { SearchOutlined } from '@ant-design/icons'

export const AppointmentListTab = ({
  filteredAppointments = [],
  appointmentColumns = [],
  loading = false,
  appKeyword,
  setAppKeyword,
  appDoctorFilter,
  setAppDoctorFilter,
  appStatusFilter,
  setAppStatusFilter,
  permissions = {},
  user,
  doctors = [],
}) => {
  return (
    <Card style={{ borderRadius: 12 }}>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={8} md={6}>
          <Input
            placeholder="Tìm mã LH, tên bệnh nhân..."
            prefix={<SearchOutlined />}
            value={appKeyword}
            onChange={(e) => setAppKeyword(e.target.value)}
            allowClear
          />
        </Col>
        <Col xs={12} sm={8} md={6}>
          <Select
            style={{ width: '100%' }}
            value={appDoctorFilter}
            onChange={setAppDoctorFilter}
            options={
              permissions.isDoctorOnly
                ? [{ value: 'ALL', label: `Bác sĩ: ${user?.fullName || user?.username || 'Bạn'}` }]
                : [
                    { value: 'ALL', label: 'Tất cả Bác sĩ' },
                    ...doctors.map((d) => ({ value: d.id, label: d.fullName || d.username })),
                  ]
            }
            disabled={permissions.isDoctorOnly}
          />
        </Col>
        <Col xs={12} sm={8} md={6}>
          <Select
            style={{ width: '100%' }}
            value={appStatusFilter}
            onChange={setAppStatusFilter}
            options={[
              { value: 'ALL', label: 'Tất cả trạng thái' },
              { value: 'SCHEDULED', label: 'Đã đặt (SCHEDULED)' },
              { value: 'CHECKED_IN', label: 'Đã check-in' },
              { value: 'NO_SHOW', label: 'Không đến (NO_SHOW)' },
              { value: 'COMPLETED', label: 'Đã hoàn thành' },
              { value: 'CANCELLED', label: 'Đã hủy' },
            ]}
          />
        </Col>
      </Row>

      <Table
        dataSource={filteredAppointments}
        columns={appointmentColumns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10, showSizeChanger: true }}
      />
    </Card>
  )
}

export default AppointmentListTab
