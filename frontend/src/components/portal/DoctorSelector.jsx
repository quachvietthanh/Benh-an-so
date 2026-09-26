import React, { useMemo, useState } from 'react'
import {
  Avatar,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Row,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd'
import {
  ArrowLeftOutlined,
  CheckCircleFilled,
  IdcardOutlined,
  SearchOutlined,
  UserOutlined,
} from '@ant-design/icons'

const { Text, Title } = Typography

function DoctorSelector({
  specialty,
  doctors = [],
  selectedDoctor = null,
  onSelectDoctor,
  onBack,
  loading = false,
}) {
  const [searchQuery, setSearchQuery] = useState('')

  const filteredDoctors = useMemo(() => {
    if (!searchQuery.trim()) return doctors
    const q = searchQuery.trim().toLowerCase()
    return doctors.filter(
      (d) =>
        (d.fullName && d.fullName.toLowerCase().includes(q)) ||
        (d.username && d.username.toLowerCase().includes(q)) ||
        (d.email && d.email.toLowerCase().includes(q)) ||
        (d.phone && d.phone.toLowerCase().includes(q))
    )
  }, [doctors, searchQuery])

  return (
    <div className="doctor-selector-container">
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <Space align="center" size={8}>
            {onBack && (
              <Button
                type="text"
                icon={<ArrowLeftOutlined />}
                onClick={onBack}
                style={{ padding: '0 8px' }}
              />
            )}
            <div>
              <Title level={5} style={{ margin: 0, color: '#1e3a8a' }}>
                <UserOutlined style={{ marginRight: 8 }} />
                Bước 2: Chọn bác sĩ phụ trách
              </Title>
              <Text type="secondary" style={{ fontSize: 13 }}>
                Chuyên khoa đang chọn:{' '}
                <strong style={{ color: '#2563eb' }}>{specialty?.name}</strong>
              </Text>
            </div>
          </Space>
        </div>

        <Input
          placeholder="Tìm tên bác sĩ..."
          prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{ width: 240, borderRadius: 8 }}
          allowClear
        />
      </div>

      {loading ? (
        <Row gutter={[16, 16]}>
          {[1, 2, 3, 4].map((k) => (
            <Col xs={24} sm={12} key={k}>
              <Card style={{ borderRadius: 10, border: '1px solid #e2e8f0' }}>
                <Skeleton active avatar paragraph={{ rows: 2 }} />
              </Card>
            </Col>
          ))}
        </Row>
      ) : filteredDoctors.length === 0 ? (
        <Card style={{ textAlign: 'center', padding: '32px 0', borderRadius: 10 }}>
          <Empty
            description={`Không tìm thấy bác sĩ nào thuộc chuyên khoa ${specialty?.name || ''}.`}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            {onBack && (
              <Button type="primary" onClick={onBack} icon={<ArrowLeftOutlined />}>
                Chọn chuyên khoa khác
              </Button>
            )}
          </Empty>
        </Card>
      ) : (
        <Row gutter={[16, 16]} style={{ display: 'flex', flexWrap: 'wrap' }}>
          {filteredDoctors.map((doctor) => {
            const isSelected = selectedDoctor?.id === doctor.id
            const doctorName = doctor.fullName || doctor.username || 'Bác sĩ'

            return (
              <Col xs={24} sm={12} key={doctor.id} style={{ display: 'flex' }}>
                <Card
                  hoverable
                  onClick={() => onSelectDoctor(doctor)}
                  style={{
                    width: '100%',
                    borderRadius: 14,
                    border: isSelected ? '2px solid #2563eb' : '1px solid #e2e8f0',
                    background: isSelected ? '#eff6ff' : '#ffffff',
                    transition: 'all 0.2s ease',
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    boxShadow: isSelected ? '0 8px 20px rgba(37, 99, 235, 0.12)' : '0 2px 6px rgba(0, 0, 0, 0.02)',
                  }}
                  bodyStyle={{ padding: '18px 20px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                    <Avatar
                      size={52}
                      style={{
                        backgroundColor: isSelected ? '#2563eb' : '#dbeafe',
                        color: isSelected ? '#ffffff' : '#1d4ed8',
                        fontSize: 20,
                        fontWeight: 600,
                        flexShrink: 0,
                      }}
                      icon={<UserOutlined />}
                    >
                      {doctorName.charAt(0).toUpperCase()}
                    </Avatar>

                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                        <Text strong style={{ fontSize: 16, color: isSelected ? '#1e40af' : '#1e293b' }}>
                          BS. {doctorName}
                        </Text>
                        {isSelected && (
                          <CheckCircleFilled style={{ color: '#2563eb', fontSize: 18 }} />
                        )}
                      </div>

                      <div style={{ marginTop: 4, display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center' }}>
                        <Tag color="blue" style={{ fontSize: 11, margin: 0, borderRadius: 4 }}>
                          <IdcardOutlined style={{ marginRight: 4 }} />
                          {specialty?.name || 'Bác sĩ chuyên khoa'}
                        </Tag>
                        {doctor.phone && (
                          <span style={{ fontSize: 12, color: '#64748b' }}>
                            SĐT: {doctor.phone}
                          </span>
                        )}
                      </div>

                      <div style={{ marginTop: 6, fontSize: 12, color: '#059669', fontWeight: 500 }}>
                        ● Có lịch khám trực tuyến
                      </div>
                    </div>
                  </div>

                  <div
                    style={{
                      marginTop: 14,
                      paddingTop: 10,
                      borderTop: isSelected ? '1px solid #bfdbfe' : '1px solid #f1f5f9',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      fontSize: 12,
                      fontWeight: 500,
                      color: isSelected ? '#2563eb' : '#94a3b8',
                    }}
                  >
                    <span>{isSelected ? 'Đã chọn bác sĩ này' : 'Bấm để chọn bác sĩ'}</span>
                    <span style={{ fontSize: 13 }}>➔</span>
                  </div>
                </Card>
              </Col>
            )
          })}
        </Row>
      )}
    </div>
  )
}

export default DoctorSelector
