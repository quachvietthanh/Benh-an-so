import React, { useState, useEffect, useMemo } from 'react'
import {
  Modal,
  Alert,
  DatePicker,
  TimePicker,
  Select,
  Input,
  Space,
  Tag,
  Typography,
  Button,
  Row,
  Col,
  Divider,
} from 'antd'
import {
  SwapOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  UserOutlined,
  MedicineBoxOutlined,
  CheckOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  PRESET_RESCHEDULE_REASONS,
  validateRescheduleReason,
  validateRescheduleTime,
} from '../../utils/appointmentRescheduleValidation.js'
import { fixMojibake } from '../../utils/serviceCatalogValidation.js'

const { Text } = Typography
const { TextArea } = Input

/**
 * Modal Đổi lịch hẹn khám tại quầy tiếp đón (NCL-03-CN-007)
 * Tuân thủ quy tắc QTN-04 (Tránh trùng lịch bác sĩ)
 * Tuân thủ quy tắc QTN-30 (Trong ca trực bác sĩ)
 * Thỏa mãn TC-01, TC-03
 */
export default function ReceptionRescheduleAppointmentModal({
  open,
  onClose,
  appointment,
  doctorList = [],
  onConfirm,
  loading = false,
}) {
  const [selectedDoctorId, setSelectedDoctorId] = useState(undefined)
  const [newDate, setNewDate] = useState(dayjs())
  const [newTime, setNewTime] = useState(dayjs().add(1, 'hour').minute(0))
  const [reason, setReason] = useState('')
  const [selectedPreset, setSelectedPreset] = useState(null)
  const [touched, setTouched] = useState(false)

  // Khởi tạo form khi mở modal
  useEffect(() => {
    if (open && appointment) {
      const currentDocId = appointment.doctorId || appointment.doctor?.id
      setSelectedDoctorId(currentDocId)

      const appTime = appointment.appointmentAt || appointment.startTime || appointment.date
      const currentDay = appTime ? dayjs(appTime) : dayjs()
      const initialDate = currentDay.isBefore(dayjs(), 'day') ? dayjs() : currentDay
      setNewDate(initialDate)

      const initialTime = currentDay.isValid() && currentDay.isAfter(dayjs())
        ? currentDay
        : dayjs().add(1, 'hour').minute(0)
      setNewTime(initialTime)

      setReason('')
      setSelectedPreset(null)
      setTouched(false)
    }
  }, [open, appointment])

  // Tính thời gian bắt đầu và kết thúc mới (+30 phút chuẩn ca khám)
  const computedTimes = useMemo(() => {
    if (!newDate || !newTime) return { start: null, end: null }
    const start = newDate
      .hour(newTime.hour())
      .minute(newTime.minute())
      .second(0)
      .millisecond(0)
    const end = start.add(30, 'minute')
    return { start, end }
  }, [newDate, newTime])

  const timeValidation = useMemo(() => {
    if (!computedTimes.start || !computedTimes.end) {
      return { valid: false, error: 'Chưa chọn thời gian hợp lệ.' }
    }
    return validateRescheduleTime(computedTimes.start, computedTimes.end, dayjs())
  }, [computedTimes])

  const reasonValidation = useMemo(() => {
    return validateRescheduleReason(reason)
  }, [reason])

  const isFormValid = timeValidation.valid && reasonValidation.valid

  if (!appointment) return null

  const patientName = fixMojibake(
    appointment.patientName || appointment.patient?.fullName || 'Bệnh nhân',
  )
  const patientCode = appointment.patientCode || appointment.patient?.patientCode || '—'
  const patientPhone = appointment.phone || appointment.patient?.phone || appointment.patient?.phoneNumber || '—'
  const currentDoctorName = fixMojibake(
    appointment.doctorName || appointment.doctor?.fullName || 'Bác sĩ phụ trách',
  )
  const currentDepartment = appointment.department || appointment.doctor?.department || 'Chuyên khoa'
  const currentAppTime = appointment.appointmentAt || appointment.startTime || appointment.date
    ? dayjs(appointment.appointmentAt || appointment.startTime || appointment.date).format('HH:mm - DD/MM/YYYY')
    : '—'

  const handleSelectPreset = (presetText) => {
    setSelectedPreset(presetText)
    setReason(presetText)
    setTouched(true)
  }

  const handleReasonChange = (e) => {
    const val = e.target.value
    setReason(val)
    setTouched(true)
    if (selectedPreset && val !== selectedPreset) {
      setSelectedPreset(null)
    }
  }

  const handleSubmit = async () => {
    setTouched(true)
    if (!isFormValid) return

    const payload = {
      newDoctorId: selectedDoctorId || appointment.doctorId,
      startTime: computedTimes.start.toISOString(),
      endTime: computedTimes.end.toISOString(),
      reason: reasonValidation.reason,
    }

    if (onConfirm) {
      await onConfirm(appointment, payload)
    }
  }

  return (
    <Modal
      open={open}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              backgroundColor: '#eff6ff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#2563eb',
              fontSize: 18,
            }}
          >
            <SwapOutlined />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#1e3a8a' }}>
              Đổi Lịch Hẹn Khám Tại Quầy
            </div>
            <div style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
              Mã lịch hẹn: <Text strong code>{appointment.appointmentCode || appointment.id}</Text>
            </div>
          </div>
        </div>
      }
      onCancel={onClose}
      destroyOnClose
      width={660}
      footer={[
        <Button key="back" onClick={onClose} disabled={loading}>
          Bỏ qua
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<SwapOutlined />}
          loading={loading}
          disabled={loading || (touched && !isFormValid)}
          onClick={handleSubmit}
          id="btn-confirm-reschedule-appointment"
          style={{ backgroundColor: '#2563eb' }}
        >
          Xác nhận đổi lịch
        </Button>,
      ]}
      style={{ top: 25 }}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginTop: 8 }}>
        {/* Thông tin lịch hẹn hiện tại: Bố cục 2 thẻ cân đối đối xứng */}
        <div
          style={{
            backgroundColor: '#f8fafc',
            borderRadius: 8,
            border: '1px solid #e2e8f0',
            padding: '12px 14px',
          }}
        >
          <div
            style={{
              fontSize: 13,
              fontWeight: 600,
              color: '#334155',
              marginBottom: 10,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            <CalendarOutlined style={{ color: '#2563eb' }} /> Thông tin lịch hẹn hiện tại:
          </div>

          <Row gutter={[12, 12]}>
            {/* Cột trái: Bệnh nhân */}
            <Col xs={24} sm={12}>
              <div
                style={{
                  backgroundColor: '#ffffff',
                  borderRadius: 6,
                  border: '1px solid #e2e8f0',
                  padding: '10px 12px',
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  gap: 6,
                }}
              >
                <div>
                  <div
                    style={{
                      fontSize: 11,
                      fontWeight: 700,
                      textTransform: 'uppercase',
                      color: '#64748b',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 6,
                      marginBottom: 4,
                    }}
                  >
                    <UserOutlined style={{ color: '#2563eb' }} /> Bệnh nhân
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                    <Text strong style={{ fontSize: 13.5, color: '#0f172a' }}>
                      {patientName}
                    </Text>
                    {patientCode && patientCode !== '—' && (
                      <Tag color="blue" style={{ margin: 0, fontSize: 11, lineHeight: '18px', padding: '0 5px' }}>
                        {patientCode}
                      </Tag>
                    )}
                  </div>
                </div>
                <div style={{ fontSize: 12, color: '#64748b' }}>
                  Số điện thoại: <Text strong style={{ color: '#334155' }}>{patientPhone || '—'}</Text>
                </div>
              </div>
            </Col>

            {/* Cột phải: Bác sĩ hiện tại */}
            <Col xs={24} sm={12}>
              <div
                style={{
                  backgroundColor: '#ffffff',
                  borderRadius: 6,
                  border: '1px solid #e2e8f0',
                  padding: '10px 12px',
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  gap: 6,
                }}
              >
                <div>
                  <div
                    style={{
                      fontSize: 11,
                      fontWeight: 700,
                      textTransform: 'uppercase',
                      color: '#64748b',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 6,
                      marginBottom: 4,
                    }}
                  >
                    <MedicineBoxOutlined style={{ color: '#059669' }} /> Bác sĩ hiện tại
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                    <Text strong style={{ fontSize: 13.5, color: '#0f172a' }}>
                      {currentDoctorName}
                    </Text>
                    {currentDepartment && currentDepartment !== '—' && (
                      <Tag color="cyan" style={{ margin: 0, fontSize: 11, lineHeight: '18px', padding: '0 5px' }}>
                        {currentDepartment}
                      </Tag>
                    )}
                  </div>
                </div>
                <div style={{ fontSize: 12, color: '#64748b' }}>
                  Khung giờ cũ: <Text type="danger" strong>{currentAppTime}</Text>
                </div>
              </div>
            </Col>
          </Row>
        </div>

        {/* Form nhập thông tin dời lịch mới */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div>
            <label style={{ fontSize: 13, fontWeight: 600, color: '#0f172a', display: 'block', marginBottom: 4 }}>
              Bác sĩ khám mới:
            </label>
            <Select
              style={{ width: '100%' }}
              placeholder="Chọn bác sĩ phụ trách mới..."
              value={selectedDoctorId}
              onChange={setSelectedDoctorId}
              options={doctorList.map((d) => ({
                value: d.id,
                label: `${d.fullName || d.username} — ${d.department || 'Chuyên khoa'}`,
              }))}
            />
            <div style={{ fontSize: 11.5, color: '#64748b', marginTop: 3 }}>
              Mặc định giữ nguyên bác sĩ hiện tại. Lễ tân có thể chuyển sang bác sĩ khác nếu bệnh nhân yêu cầu.
            </div>
          </div>

          <Row gutter={12}>
            <Col xs={24} sm={12}>
              <label style={{ fontSize: 13, fontWeight: 600, color: '#0f172a', display: 'block', marginBottom: 4 }}>
                <span style={{ color: '#ef4444' }}>* </span>Ngày hẹn mới:
              </label>
              <DatePicker
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                value={newDate}
                onChange={(d) => d && setNewDate(d)}
                disabledDate={(current) => current && current.isBefore(dayjs(), 'day')}
                allowClear={false}
              />
            </Col>
            <Col xs={24} sm={12}>
              <label style={{ fontSize: 13, fontWeight: 600, color: '#0f172a', display: 'block', marginBottom: 4 }}>
                <span style={{ color: '#ef4444' }}>* </span>Giờ bắt đầu:
              </label>
              <TimePicker
                format="HH:mm"
                minuteStep={15}
                style={{ width: '100%' }}
                value={newTime}
                onChange={(t) => t && setNewTime(t)}
                allowClear={false}
              />
            </Col>
          </Row>

          {/* Preview khung giờ mới */}
          {computedTimes.start && computedTimes.end && (
            <div
              style={{
                backgroundColor: '#eff6ff',
                border: '1px solid #bfdbfe',
                borderRadius: 6,
                padding: '8px 12px',
                fontSize: 12.5,
                color: '#1e40af',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <span>
                <ClockCircleOutlined /> Khung giờ dự kiến mới: <strong>{computedTimes.start.format('HH:mm')} – {computedTimes.end.format('HH:mm')} ({computedTimes.start.format('DD/MM/YYYY')})</strong>
              </span>
              <Tag color="blue">30 phút</Tag>
            </div>
          )}

          {touched && !timeValidation.valid && (
            <div style={{ color: '#ef4444', fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}>
              <ExclamationCircleOutlined /> {timeValidation.error}
            </div>
          )}

          {/* Gợi ý lý do nhanh */}
          <div>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#1e293b', marginBottom: 6 }}>
              Gợi ý lý do dời lịch (chọn để điền):
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {PRESET_RESCHEDULE_REASONS.map((preset) => {
                const isSelected = selectedPreset === preset
                return (
                  <Button
                    key={preset}
                    size="small"
                    type={isSelected ? 'primary' : 'default'}
                    icon={isSelected ? <CheckOutlined /> : null}
                    onClick={() => handleSelectPreset(preset)}
                    style={{
                      fontSize: 12,
                      borderRadius: 16,
                      height: 'auto',
                      padding: '3px 10px',
                      borderColor: isSelected ? '#2563eb' : '#cbd5e1',
                      backgroundColor: isSelected ? '#dbeafe' : undefined,
                      color: isSelected ? '#1e40af' : '#334155',
                    }}
                  >
                    {preset}
                  </Button>
                )
              })}
            </div>
          </div>

          {/* Ô nhập lý do chi tiết */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: '#0f172a' }}>
                <span style={{ color: '#ef4444' }}>* </span>
                Lý do dời lịch hẹn:
              </span>
              <span
                style={{
                  fontSize: 12,
                  color: reason.length > 500 ? '#ef4444' : reason.length > 400 ? '#f59e0b' : '#64748b',
                  fontWeight: reason.length > 500 ? 700 : 400,
                }}
              >
                {reason.length} / 500 ký tự
              </span>
            </div>
            <TextArea
              rows={3}
              value={reason}
              onChange={handleReasonChange}
              onBlur={() => setTouched(true)}
              placeholder="Nhập lý do dời lịch (bắt buộc, từ 1 đến 500 ký tự)..."
              status={touched && !reasonValidation.valid ? 'error' : ''}
              maxLength={520}
              id="input-reschedule-appointment-reason"
            />
            {touched && !reasonValidation.valid && (
              <div style={{ color: '#ef4444', fontSize: 12, marginTop: 3, display: 'flex', alignItems: 'center', gap: 4 }}>
                <ExclamationCircleOutlined /> {reasonValidation.error}
              </div>
            )}
          </div>

          {/* Cảnh báo quy tắc QTN-04 và QTN-30 */}
          <Alert
            type="info"
            showIcon
            message="Quy tắc giải phóng khung giờ & tránh trùng lịch (QTN-04, QTN-30)"
            description="Sau khi dời lịch thành công, khung giờ cũ sẽ được tự động giải phóng để tiếp nhận bệnh nhân khác. Hệ thống sẽ kiểm tra trùng lịch bác sĩ và lịch trực theo quy định."
            style={{ fontSize: 12, backgroundColor: '#f0fdf4', borderColor: '#bbf7d0' }}
          />
        </div>
      </div>
    </Modal>
  )
}
