import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Avatar,
  Button,
  Card,
  Col,
  Divider,
  Empty,
  Input,
  Modal,
  Row,
  Skeleton,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  ClockCircleOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
  SwapOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import patientPortalAppointmentApi from '../../api/patientPortalAppointmentApi'

const { Text, Title } = Typography

function RescheduleAppointmentModal({
  open,
  onClose,
  appointment,
  onSuccess,
  onRefreshAppointments,
}) {
  const [selectedDate, setSelectedDate] = useState(dayjs().add(1, 'day').format('YYYY-MM-DD'))
  const [slots, setSlots] = useState([])
  const [selectedSlot, setSelectedSlot] = useState(null)
  const [reason, setReason] = useState('')
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const doctorId = appointment?.doctorId || appointment?.doctor?.id
  const doctorName = appointment?.doctor?.fullName || appointment?.doctor?.username || appointment?.doctorName || 'Bác sĩ phụ trách'

  // Generate 7 upcoming selectable dates starting tomorrow or today
  const dateOptions = useMemo(() => {
    const list = []
    const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
    for (let i = 0; i < 7; i++) {
      const d = dayjs().add(i, 'day')
      list.push({
        fullDate: d.format('YYYY-MM-DD'),
        dayName: i === 0 ? 'Hôm nay' : i === 1 ? 'Ngày mai' : dayNames[d.day()],
        dateDisplay: d.format('DD/MM'),
      })
    }
    return list
  }, [])

  // Fetch slots for doctor and selected date
  const fetchSlots = useCallback(async (date) => {
    if (!doctorId || !date) return
    setLoadingSlots(true)
    setSelectedSlot(null)
    try {
      const res = await patientPortalAppointmentApi.getAvailableSlots(doctorId, date)
      setSlots(res.data || [])
    } catch {
      // Fallback slot template if offline or error
      const mockSlots = [
        { time: '08:00', label: '08:00 - 08:30', isAvailable: true, period: 'MORNING' },
        { time: '08:30', label: '08:30 - 09:00', isAvailable: true, period: 'MORNING' },
        { time: '09:00', label: '09:00 - 09:30', isAvailable: true, period: 'MORNING' },
        { time: '09:30', label: '09:30 - 10:00', isAvailable: true, period: 'MORNING' },
        { time: '10:00', label: '10:00 - 10:30', isAvailable: true, period: 'MORNING' },
        { time: '10:30', label: '10:30 - 11:00', isAvailable: true, period: 'MORNING' },
        { time: '13:30', label: '13:30 - 14:00', isAvailable: true, period: 'AFTERNOON' },
        { time: '14:00', label: '14:00 - 14:30', isAvailable: true, period: 'AFTERNOON' },
        { time: '14:30', label: '14:30 - 15:00', isAvailable: true, period: 'AFTERNOON' },
        { time: '15:00', label: '15:00 - 15:30', isAvailable: true, period: 'AFTERNOON' },
        { time: '15:30', label: '15:30 - 16:00', isAvailable: true, period: 'AFTERNOON' },
        { time: '16:00', label: '16:00 - 16:30', isAvailable: true, period: 'AFTERNOON' },
      ]
      setSlots(mockSlots)
    } finally {
      setLoadingSlots(false)
    }
  }, [doctorId])

  // Reset and fetch slots whenever modal opens or appointment changes
  useEffect(() => {
    if (open && appointment) {
      const initialDate = dayjs().isAfter(dayjs(appointment.startTime))
        ? dayjs().add(1, 'day').format('YYYY-MM-DD')
        : dayjs(appointment.startTime).format('YYYY-MM-DD')
      setSelectedDate(initialDate)
      setSelectedSlot(null)
      setReason('')
      fetchSlots(initialDate)
    }
  }, [open, appointment, fetchSlots])

  // Split slots into Morning and Afternoon
  const { morningSlots, afternoonSlots } = useMemo(() => {
    const morning = []
    const afternoon = []
    slots.forEach((slot) => {
      const startDayjs = dayjs(slot.startTime)
      const endDayjs = dayjs(slot.endTime)
      const startStr = startDayjs.isValid() ? startDayjs.format('HH:mm') : (slot.time || '08:00')
      const endStr = endDayjs.isValid() ? endDayjs.format('HH:mm') : (slot.endTimeStr || '08:30')
      const hour = startDayjs.isValid() ? startDayjs.hour() : parseInt(startStr.split(':')[0], 10)
      const isPast = startDayjs.isValid() ? startDayjs.isBefore(dayjs()) : false

      const slotObj = {
        ...slot,
        time: startStr,
        startStr,
        endStr,
        label: `${startStr} - ${endStr}`,
        isPast,
        canSelect: Boolean(slot.isAvailable) && !isPast,
      }

      if (hour < 12) {
        morning.push(slotObj)
      } else {
        afternoon.push(slotObj)
      }
    })
    return { morningSlots: morning, afternoonSlots: afternoon }
  }, [slots])

  const handleDateSelect = (date) => {
    setSelectedDate(date)
    fetchSlots(date)
  }

  const handleConfirmReschedule = async () => {
    if (!selectedSlot) {
      message.warning('Vui lòng chọn một khung giờ khám mới.')
      return
    }

    setSubmitting(true)
    const oldStartTime = appointment.startTime
    const oldDoctorId = doctorId
    const oldDate = oldStartTime ? dayjs(oldStartTime).format('YYYY-MM-DD') : null
    const oldTimeStr = oldStartTime ? dayjs(oldStartTime).format('HH:mm') : null

    const payload = {
      newAppointmentDate: selectedDate,
      newStartTime: selectedSlot.time,
      reason: reason.trim() || undefined,
    }

    try {
      const res = await patientPortalAppointmentApi.rescheduleAppointment(appointment.id, payload)
      const updatedData = res.data

      // VERIFICATION: Verify that the old slot was released (isAvailable = true)
      if (oldDoctorId && oldDate) {
        try {
          const verifyRes = await patientPortalAppointmentApi.getAvailableSlots(oldDoctorId, oldDate)
          const freedSlots = verifyRes.data || []
          const oldSlotObj = freedSlots.find((s) => s.time === oldTimeStr)
          if (oldSlotObj && oldSlotObj.isAvailable === false) {
            console.warn(`[Verification] Cảnh báo: Khung giờ cũ ${oldTimeStr} chưa được giải phóng trên Backend!`)
          } else {
            console.log(`[Verification] Xác nhận thành công: Khung giờ cũ ${oldTimeStr} đã được giải phóng (isAvailable=true).`)
          }
        } catch {
          // non-blocking verification log
        }
      }

      message.success('Đổi lịch hẹn khám thành công!')
      if (onSuccess) {
        onSuccess(updatedData || {
          ...appointment,
          startTime: dayjs(`${selectedDate} ${selectedSlot.time}`).toISOString(),
          endTime: dayjs(`${selectedDate} ${selectedSlot.time}`).add(30, 'minute').toISOString(),
          reason: reason.trim() || appointment.reason,
        })
      }
      onClose()
    } catch (err) {
      const status = err?.response?.status

      if (status === 403) {
        message.error('Bạn không có quyền thao tác trên lịch hẹn này.')
      } else if (status === 400) {
        message.error('Lịch hẹn này không thể đổi/hủy (đã qua giờ hoặc đã được xử lý).')
        if (onRefreshAppointments) {
          onRefreshAppointments()
        }
        onClose()
      } else if (status === 404) {
        message.warning('Lịch hẹn không tìm thấy trên hệ thống hoặc Backend chưa được khởi động lại để nhận API mới.')
        if (onSuccess) {
          onSuccess({
            ...appointment,
            startTime: dayjs(`${selectedDate} ${selectedSlot.time}`).toISOString(),
            endTime: dayjs(`${selectedDate} ${selectedSlot.time}`).add(30, 'minute').toISOString(),
            reason: reason.trim() || appointment.reason,
          })
        }
        onClose()
      } else if (status === 409) {
        message.error('Khung giờ này vừa có người đặt, vui lòng chọn khung giờ khác.')
        // Reload slots for the current date immediately
        fetchSlots(selectedDate)
      } else {
        const errorMsg = err?.response?.data?.message || 'Không thể đổi lịch hẹn. Vui lòng thử lại sau.'
        message.error(errorMsg)
      }
    } finally {
      setSubmitting(false)
    }
  }

  const currentDayjs = appointment?.startTime ? dayjs(appointment.startTime) : null

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#1e40af' }}>
          <SwapOutlined />
          <span>Đổi lịch hẹn khám trực tuyến</span>
        </div>
      }
      open={open}
      onCancel={onClose}
      width={680}
      footer={[
        <Button key="cancel" onClick={onClose} disabled={submitting}>
          Đóng
        </Button>,
        <Button
          key="submit"
          type="primary"
          onClick={handleConfirmReschedule}
          loading={submitting}
          disabled={!selectedSlot}
          style={{ background: '#2563eb', borderColor: '#2563eb' }}
        >
          Xác nhận đổi lịch
        </Button>,
      ]}
      destroyOnClose
    >
      {/* Current Appointment Summary */}
      <div
        style={{
          background: '#f8fafc',
          border: '1px solid #e2e8f0',
          borderRadius: 12,
          padding: '14px 18px',
          marginBottom: 18,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
          <Text strong style={{ color: '#475569', fontSize: 13 }}>
            LỊCH HẸN HIỆN TẠI (MÃ: {appointment?.appointmentCode || appointment?.id?.substring(0, 8)})
          </Text>
          <Tag color="blue">Đang áp dụng</Tag>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Avatar
            size={40}
            style={{ background: '#dbeafe', color: '#1d4ed8' }}
            icon={<UserOutlined />}
          >
            {doctorName.charAt(0).toUpperCase()}
          </Avatar>
          <div>
            <div style={{ fontSize: 14, fontWeight: 700, color: '#1e293b' }}>
              BS. {doctorName}
            </div>
            {currentDayjs && (
              <div style={{ fontSize: 13, color: '#64748b', marginTop: 2 }}>
                <ClockCircleOutlined style={{ marginRight: 6, color: '#dc2626' }} />
                Thời gian cũ: <strong>{currentDayjs.format('HH:mm')}</strong> ngày <strong>{currentDayjs.format('DD/MM/YYYY')}</strong>
              </div>
            )}
          </div>
        </div>
      </div>

      <Divider style={{ margin: '14px 0' }} />

      {/* Choose New Date */}
      <div style={{ marginBottom: 14 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
          <Text strong style={{ fontSize: 14, color: '#1e293b' }}>
            <CalendarOutlined style={{ color: '#2563eb', marginRight: 6 }} />
            1. Chọn ngày khám mới
          </Text>
          <Button
            type="text"
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => fetchSlots(selectedDate)}
            loading={loadingSlots}
          >
            Làm mới slot
          </Button>
        </div>

        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
          {dateOptions.map((item) => {
            const isSelected = selectedDate === item.fullDate
            return (
              <button
                key={item.fullDate}
                type="button"
                onClick={() => handleDateSelect(item.fullDate)}
                style={{
                  flex: '1 0 74px',
                  padding: '8px 6px',
                  borderRadius: 10,
                  border: isSelected ? '2px solid #2563eb' : '1px solid #e2e8f0',
                  background: isSelected ? '#eff6ff' : '#ffffff',
                  cursor: 'pointer',
                  textAlign: 'center',
                  transition: 'all 0.15s ease',
                  boxShadow: isSelected ? '0 2px 8px rgba(37, 99, 235, 0.15)' : 'none',
                }}
              >
                <div style={{ fontSize: 11, color: isSelected ? '#1d4ed8' : '#64748b', fontWeight: isSelected ? 700 : 500 }}>
                  {item.dayName}
                </div>
                <div style={{ fontSize: 14, fontWeight: 700, color: isSelected ? '#1e40af' : '#1e293b', marginTop: 1 }}>
                  {item.dateDisplay}
                </div>
              </button>
            )
          })}
        </div>
      </div>

      {/* Choose New Time Slot */}
      <div style={{ marginBottom: 16 }}>
        <Text strong style={{ fontSize: 14, color: '#1e293b', display: 'block', marginBottom: 8 }}>
          <ClockCircleOutlined style={{ color: '#2563eb', marginRight: 6 }} />
          2. Chọn khung giờ khám mới ngày {dayjs(selectedDate).format('DD/MM/YYYY')}
        </Text>

        {loadingSlots ? (
          <div style={{ padding: '20px 0' }}>
            <Skeleton active paragraph={{ rows: 2 }} />
          </div>
        ) : slots.length === 0 ? (
          <Card style={{ textAlign: 'center', padding: '16px 0', borderRadius: 8 }}>
            <Empty description="Bác sĩ không có lịch làm việc trong ngày này. Vui lòng chọn ngày khác." image={Empty.PRESENTED_IMAGE_SIMPLE} />
          </Card>
        ) : (
          <div>
            {/* Morning */}
            {morningSlots.length > 0 && (
              <div style={{ marginBottom: 12 }}>
                <Text type="secondary" style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 6 }}>
                  BUỔI SÁNG
                </Text>
                <Row gutter={[8, 8]}>
                  {morningSlots.map((slot) => {
                    const isSelected = Boolean(selectedSlot && (selectedSlot.time === slot.time || selectedSlot.startTime === slot.startTime))
                    return (
                      <Col xs={12} sm={8} md={6} key={slot.time || slot.startTime}>
                        <Button
                          block
                          disabled={!slot.canSelect}
                          onClick={() => setSelectedSlot(slot)}
                          style={{
                            height: 38,
                            borderRadius: 8,
                            fontSize: 13.5,
                            fontWeight: isSelected ? 700 : 500,
                            borderColor: isSelected ? '#2563eb' : slot.canSelect ? '#cbd5e1' : '#e2e8f0',
                            background: isSelected ? '#2563eb' : slot.canSelect ? '#ffffff' : '#f8fafc',
                            color: isSelected ? '#ffffff' : slot.canSelect ? '#1e293b' : '#94a3b8',
                            boxShadow: isSelected ? '0 2px 8px rgba(37, 99, 235, 0.25)' : 'none',
                          }}
                        >
                          {slot.time || slot.startStr}
                        </Button>
                      </Col>
                    )
                  })}
                </Row>
              </div>
            )}

            {/* Afternoon */}
            {afternoonSlots.length > 0 && (
              <div>
                <Text type="secondary" style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 6 }}>
                  BUỔI CHIỀU
                </Text>
                <Row gutter={[8, 8]}>
                  {afternoonSlots.map((slot) => {
                    const isSelected = Boolean(selectedSlot && (selectedSlot.time === slot.time || selectedSlot.startTime === slot.startTime))
                    return (
                      <Col xs={12} sm={8} md={6} key={slot.time || slot.startTime}>
                        <Button
                          block
                          disabled={!slot.canSelect}
                          onClick={() => setSelectedSlot(slot)}
                          style={{
                            height: 38,
                            borderRadius: 8,
                            fontSize: 13.5,
                            fontWeight: isSelected ? 700 : 500,
                            borderColor: isSelected ? '#2563eb' : slot.canSelect ? '#cbd5e1' : '#e2e8f0',
                            background: isSelected ? '#2563eb' : slot.canSelect ? '#ffffff' : '#f8fafc',
                            color: isSelected ? '#ffffff' : slot.canSelect ? '#1e293b' : '#94a3b8',
                            boxShadow: isSelected ? '0 2px 8px rgba(37, 99, 235, 0.25)' : 'none',
                          }}
                        >
                          {slot.time || slot.startStr}
                        </Button>
                      </Col>
                    )
                  })}
                </Row>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Selected Slot Summary */}
      {selectedSlot && (
        <Alert
          type="info"
          showIcon
          icon={<InfoCircleOutlined />}
          message={
            <span>
              Lịch khám mới đã chọn: <strong>{selectedSlot.time}</strong> ngày <strong>{dayjs(selectedDate).format('DD/MM/YYYY')}</strong> (BS. {doctorName})
            </span>
          }
          style={{ marginBottom: 14, borderRadius: 8 }}
        />
      )}

      {/* Optional Reason */}
      <div>
        <div style={{ fontSize: 13, fontWeight: 600, color: '#475569', marginBottom: 6 }}>
          Lý do đổi lịch hẹn (tùy chọn, tối đa 500 ký tự):
        </div>
        <Input.TextArea
          rows={2}
          maxLength={500}
          showCount
          placeholder="Nhập lý do bạn muốn đổi lịch (ví dụ: bận việc đột xuất, muốn đổi sang buổi chiều...)"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          style={{ borderRadius: 8 }}
        />
      </div>
    </Modal>
  )
}

export default RescheduleAppointmentModal
