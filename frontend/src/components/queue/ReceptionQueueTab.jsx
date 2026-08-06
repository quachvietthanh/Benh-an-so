import React from 'react'
import { Card, Row, Col, Input, Select, Button, Table, Empty } from 'antd'
import { SearchOutlined, StepForwardOutlined } from '@ant-design/icons'

export const ReceptionQueueTab = ({
  filteredQueues = [],
  queueBoardColumns = [],
  loading = false,
  queueKeyword,
  setQueueKeyword,
  queueDoctorFilter,
  setQueueDoctorFilter,
  queueRoomFilter,
  setQueueRoomFilter,
  queueStatusFilter,
  setQueueStatusFilter,
  queueSourceFilter,
  setQueueSourceFilter,
  permissions = {},
  user,
  doctors = [],
  extractedRooms = [],
  actionLoading = false,
  handleCallNext,
}) => {
  return (
    <Card style={{ borderRadius: 12 }}>
      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={6} md={5}>
          <Input
            placeholder="Tìm tên, mã BN, mã lượt..."
            prefix={<SearchOutlined />}
            value={queueKeyword}
            onChange={(e) => setQueueKeyword(e.target.value)}
            allowClear
          />
        </Col>
        <Col xs={12} sm={6} md={4}>
          <Select
            style={{ width: '100%' }}
            value={queueDoctorFilter}
            onChange={setQueueDoctorFilter}
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
        <Col xs={12} sm={6} md={4}>
          <Select
            style={{ width: '100%' }}
            value={queueRoomFilter}
            onChange={setQueueRoomFilter}
            options={[
              { value: 'ALL', label: 'Tất cả Phòng khám' },
              ...extractedRooms.map((r) => ({ value: r.id, label: `${r.code} - ${r.name}` })),
            ]}
          />
        </Col>
        <Col xs={12} sm={6} md={4}>
          <Select
            style={{ width: '100%' }}
            value={queueStatusFilter}
            onChange={setQueueStatusFilter}
            options={[
              { value: 'ALL', label: 'Tất cả Trạng thái' },
              { value: 'WAITING', label: 'Đang chờ (WAITING)' },
              { value: 'IN_PROGRESS', label: 'Đang khám (IN_PROGRESS)' },
              { value: 'WAITING_FOR_RESULT', label: 'Chờ CĐLS (WAITING_FOR_RESULT)' },
              { value: 'COMPLETED', label: 'Hoàn thành (COMPLETED)' },
              { value: 'SKIPPED', label: 'Bỏ qua / Vắng mặt (SKIPPED)' },
              { value: 'CANCELLED', label: 'Đã hủy (CANCELLED)' },
            ]}
          />
        </Col>
        <Col xs={12} sm={6} md={4}>
          <Select
            style={{ width: '100%' }}
            value={queueSourceFilter}
            onChange={setQueueSourceFilter}
            options={[
              { value: 'ALL', label: 'Tất cả Nguồn' },
              { value: 'APPOINTMENT', label: 'Hẹn trước (Appointment)' },
              { value: 'WALK_IN', label: 'Tự đến (Walk-in)' },
            ]}
          />
        </Col>
        <Col xs={24} sm={24} md={3} style={{ textAlign: 'right' }}>
          {permissions.canCallNext && (
            <Button
              type="primary"
              icon={<StepForwardOutlined />}
              loading={actionLoading}
              onClick={() => handleCallNext()}
            >
              Gọi tiếp
            </Button>
          )}
        </Col>
      </Row>

      {filteredQueues.length === 0 ? (
        <Empty
          description="Hàng đợi hiện tại đang trống. Bệnh nhân sau khi check-in sẽ xuất hiện ở đây theo thứ tự."
          style={{ margin: '40px 0' }}
        />
      ) : (
        <Table
          dataSource={filteredQueues}
          columns={queueBoardColumns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      )}
    </Card>
  )
}

export default ReceptionQueueTab
