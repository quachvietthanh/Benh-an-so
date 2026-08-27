import React, { useMemo } from 'react'
import {
  Button,
  Card,
  Col,
  DatePicker,
  Divider,
  Empty,
  Row,
  Skeleton,
  Space,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  ArrowLeftOutlined,
  CalendarOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  ReloadOutlined,
  SunOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

const { Text, Title } = Typography

function TimeSlotPicker({
  doctor,
  specialty,
  selectedDate,
  onDateChange,
  slots = [],
  selectedSlot = null,
  onSelectSlot,
  loading = false,
  onReload,
  onBack,
}) {
  const dateOptions = useMemo(() => {
    const dates = []
    for (let i = 0; i < 10; i++) {
      const d = dayjs().add(i, 'day')
      dates.push({
        fullDate: d.format('YYYY-MM-DD'),
        dayName: i === 0 ? 'Hôm nay' : i === 1 ? 'Ngày mai' : `Thứ ${d.day() === 0 ? 'CN' : d.day() + 1}`,
        dateDisplay: d.format('DD/MM'),
      })
    }
    return dates
  }, [])

  const { morningSlots, afternoonSlots } = useMemo(() => {
    const morning = []
    const afternoon = []

    slots.forEach((slot) => {
      const startDayjs = dayjs(slot.startTime)
      const endDayjs = dayjs(slot.endTime)
      const startStr = startDayjs.format('HH:mm')
      const endStr = endDayjs.format('HH:mm')
      const hour = startDayjs.hour()

      const slotItem = {
        ...slot,
        startStr,
        endStr,
        label: `${startStr} - ${endStr}`,
      }

      if (hour < 12) {
        morning.push(slotItem)
      } else {
        afternoon.push(slotItem)
      }
    })

    return { morningSlots: morning, afternoonSlots: afternoon }
  }, [slots])

  const availableCount = useMemo(() => {
    return slots.filter((s) => s.isAvailable).length
  }, [slots])

  const disabledDate = (current) => {
    return current && current < dayjs().startOf('day')
  }

  return (
    <div className="timeslot-picker-container">
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
                <CalendarOutlined style={{ marginRight: 8 }} />
                Bước 3: Chọn ngày và khung giờ khám
              </Title>
              <Text type="secondary" style={{ fontSize: 13 }}>
                Bác sĩ:{' '}
                <strong style={{ color: '#2563eb' }}>
                  BS. {doctor?.fullName || doctor?.username}
                </strong>{' '}
                ({specialty?.name})
              </Text>
            </div>
          </Space>
        </div>

        <Space size={8}>
          <Button
            icon={<ReloadOutlined />}
            size="small"
            onClick={onReload}
            loading={loading}
          >
            Làm mới giờ
          </Button>
          <DatePicker
            value={selectedDate ? dayjs(selectedDate) : null}
            onChange={(d) => onDateChange(d ? d.format('YYYY-MM-DD') : '')}
            disabledDate={disabledDate}
            format="DD/MM/YYYY"
            placeholder="Chọn ngày khác..."
            allowClear={false}
            style={{ borderRadius: 6 }}
          />
        </Space>
      </div>

      {/* Quick Date Bar */}
      <div style={{ marginBottom: 20, overflowX: 'auto', paddingBottom: 4 }}>
        <div style={{ display: 'flex', gap: 8, minWidth: 600 }}>
          {dateOptions.map((item) => {
            const isSelected = selectedDate === item.fullDate
            return (
              <button
                key={item.fullDate}
                type="button"
                onClick={() => onDateChange(item.fullDate)}
                style={{
                  flex: '1 0 80px',
                  padding: '10px 8px',
                  borderRadius: 10,
                  border: isSelected ? '2px solid #2563eb' : '1px solid #e2e8f0',
                  background: isSelected ? '#eff6ff' : '#ffffff',
                  cursor: 'pointer',
                  textAlign: 'center',
                  transition: 'all 0.15s ease',
                  boxShadow: isSelected ? '0 2px 8px rgba(37, 99, 235, 0.15)' : 'none',
                }}
              >
                <div style={{ fontSize: 12, color: isSelected ? '#1d4ed8' : '#64748b', fontWeight: isSelected ? 700 : 500 }}>
                  {item.dayName}
                </div>
                <div style={{ fontSize: 15, fontWeight: 700, color: isSelected ? '#1e40af' : '#1e293b', marginTop: 2 }}>
                  {item.dateDisplay}
                </div>
              </button>
            )
          })}
        </div>
      </div>

      {/* Slots Section */}
      <Card
        style={{
          borderRadius: 12,
          border: '1px solid #e2e8f0',
          background: '#ffffff',
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
        }}
        bodyStyle={{ padding: '20px 24px' }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Space size={6}>
            <ClockCircleOutlined style={{ color: '#2563eb', fontSize: 16 }} />
            <Text strong style={{ fontSize: 15, color: '#1e293b' }}>
              Khung giờ khám ngày {dayjs(selectedDate).format('DD/MM/YYYY')}
            </Text>
          </Space>

          {!loading && slots.length > 0 && (
            <Tag color={availableCount > 0 ? 'success' : 'error'}>
              Còn trống: {availableCount}/{slots.length} khung giờ
            </Tag>
          )}
        </div>

        {loading ? (
          <div style={{ padding: '24px 0' }}>
            <Skeleton active paragraph={{ rows: 4 }} />
          </div>
        ) : slots.length === 0 ? (
          <div style={{ padding: '32px 0', textAlign: 'center' }}>
            <Empty
              description={
                <div style={{ marginTop: 8 }}>
                  <Text strong style={{ fontSize: 15, color: '#475569', display: 'block' }}>
                    Bác sĩ không có lịch làm việc vào ngày {dayjs(selectedDate).format('DD/MM/YYYY')}
                  </Text>
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    Vui lòng chọn một ngày khác trên thanh ngày ở trên để xem khung giờ trống.
                  </Text>
                </div>
              }
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          </div>
        ) : (
          <div>
            {/* Buổi Sáng */}
            {morningSlots.length > 0 && (
              <div style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 10 }}>
                  <SunOutlined style={{ color: '#f59e0b', fontSize: 15 }} />
                  <Text strong style={{ fontSize: 13, color: '#475569', textTransform: 'uppercase' }}>
                    Buổi sáng
                  </Text>
                </div>

                <Row gutter={[10, 10]}>
                  {morningSlots.map((slot) => {
                    const isSelected = selectedSlot?.startTime === slot.startTime
                    const isAvailable = slot.isAvailable

                    return (
                      <Col xs={12} sm={8} md={6} lg={4} key={slot.startTime}>
                        <Tooltip
                          title={
                            isAvailable
                              ? `Bấm để chọn khung giờ ${slot.label}`
                              : 'Khung giờ này đã có người đặt trước'
                          }
                        >
                          <button
                            type="button"
                            disabled={!isAvailable}
                            onClick={() => onSelectSlot(slot)}
                            style={{
                              width: '100%',
                              padding: '10px 4px',
                              borderRadius: 8,
                              border: isSelected
                                ? '2px solid #2563eb'
                                : isAvailable
                                  ? '1px solid #93c5fd'
                                  : '1px dashed #cbd5e1',
                              background: isSelected
                                ? '#2563eb'
                                : isAvailable
                                  ? '#eff6ff'
                                  : '#f1f5f9',
                              color: isSelected
                                ? '#ffffff'
                                : isAvailable
                                  ? '#1d4ed8'
                                  : '#94a3b8',
                              fontWeight: isSelected ? 700 : 600,
                              fontSize: 13,
                              cursor: isAvailable ? 'pointer' : 'not-allowed',
                              transition: 'all 0.15s ease',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              gap: 4,
                              textDecoration: isAvailable ? 'none' : 'line-through',
                              boxShadow: isSelected ? '0 2px 6px rgba(37, 99, 235, 0.3)' : 'none',
                            }}
                          >
                            {isSelected && <CheckCircleFilled style={{ fontSize: 12 }} />}
                            <span>{slot.label}</span>
                          </button>
                        </Tooltip>
                      </Col>
                    )
                  })}
                </Row>
              </div>
            )}

            {/* Buổi Chiều */}
            {afternoonSlots.length > 0 && (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 10 }}>
                  <ClockCircleOutlined style={{ color: '#0284c7', fontSize: 15 }} />
                  <Text strong style={{ fontSize: 13, color: '#475569', textTransform: 'uppercase' }}>
                    Buổi chiều
                  </Text>
                </div>

                <Row gutter={[10, 10]}>
                  {afternoonSlots.map((slot) => {
                    const isSelected = selectedSlot?.startTime === slot.startTime
                    const isAvailable = slot.isAvailable

                    return (
                      <Col xs={12} sm={8} md={6} lg={4} key={slot.startTime}>
                        <Tooltip
                          title={
                            isAvailable
                              ? `Bấm để chọn khung giờ ${slot.label}`
                              : 'Khung giờ này đã có người đặt trước'
                          }
                        >
                          <button
                            type="button"
                            disabled={!isAvailable}
                            onClick={() => onSelectSlot(slot)}
                            style={{
                              width: '100%',
                              padding: '10px 4px',
                              borderRadius: 8,
                              border: isSelected
                                ? '2px solid #2563eb'
                                : isAvailable
                                  ? '1px solid #93c5fd'
                                  : '1px dashed #cbd5e1',
                              background: isSelected
                                ? '#2563eb'
                                : isAvailable
                                  ? '#eff6ff'
                                  : '#f1f5f9',
                              color: isSelected
                                ? '#ffffff'
                                : isAvailable
                                  ? '#1d4ed8'
                                  : '#94a3b8',
                              fontWeight: isSelected ? 700 : 600,
                              fontSize: 13,
                              cursor: isAvailable ? 'pointer' : 'not-allowed',
                              transition: 'all 0.15s ease',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              gap: 4,
                              textDecoration: isAvailable ? 'none' : 'line-through',
                              boxShadow: isSelected ? '0 2px 6px rgba(37, 99, 235, 0.3)' : 'none',
                            }}
                          >
                            {isSelected && <CheckCircleFilled style={{ fontSize: 12 }} />}
                            <span>{slot.label}</span>
                          </button>
                        </Tooltip>
                      </Col>
                    )
                  })}
                </Row>
              </div>
            )}

            <Divider style={{ margin: '18px 0 12px' }} />

            <div style={{ display: 'flex', alignItems: 'center', gap: 16, fontSize: 12, color: '#64748b', flexWrap: 'wrap' }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ width: 12, height: 12, borderRadius: 3, background: '#eff6ff', border: '1px solid #93c5fd' }} />
                Còn trống
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ width: 12, height: 12, borderRadius: 3, background: '#2563eb' }} />
                Đang chọn
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ width: 12, height: 12, borderRadius: 3, background: '#f1f5f9', border: '1px dashed #cbd5e1' }} />
                Đã kín / Đã có người đặt
              </span>
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}

export default TimeSlotPicker
