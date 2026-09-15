import React, { useState, useEffect, useMemo, useCallback } from 'react'
import {
  Modal,
  Alert,
  DatePicker,
  Select,
  Input,
  Space,
  Tag,
  Typography,
  Button,
  Row,
  Col,
  Divider,
  Spin,
  Empty,
} from 'antd'
import {
  SwapOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  UserOutlined,
  MedicineBoxOutlined,
  CheckOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import appointmentApi from '../../api/appointmentApi.js'
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
 * Tuân thủ quy tắc QTN-30 (Trong ca trực bác sĩ 30p/slot)
 * Thỏa mãn TC-01, TC-02, TC-03, TC-04
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
  const [slots, setSlots] = useState([])
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [slotError, setSlotError] = useState(null)
  const [selectedSlot, setSelectedSlot] = useState(null)
  const [reason, setReason] = useState('')
  const [selectedPreset, setSelectedPreset] = useState(null)
  const [touched, setTouched] = useState(false)

  // Fetch available slots from backend (QTN-30 / QTN-04)
  const fetchSlots = useCallback(async (docId, dateDayjs) => {
    if (!docId || !dateDayjs || !dateDayjs.isValid()) {
      setSlots([])
      setSelectedSlot(null)
      return
    }
    const dateStr = dateDayjs.format('YYYY-MM-DD')
    setLoadingSlots(true)
    setSlotError(null)
    setSelectedSlot(null)
    try {
      const res = await appointmentApi.getAvailableSlots(docId, dateStr)
      const data = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : []
      setSlots(data)
    } catch (err) {
      console.warn('Lỗi khi tải khung giờ khám khả dụng:', err)
      setSlotError('Không thể tải danh sách khung giờ từ máy chủ. Vui lòng kiểm tra lại kết nối.')
      setSlots([])
    } finally {
      setLoadingSlots(false)
    }
  }, [])

  // Khởi tạo form khi mở modal
  useEffect(() => {
    if (open && appointment) {
      const currentDocId = appointment.doctorId || appointment.doctor?.id
      setSelectedDoctorId(currentDocId)

      const appTime = appointment.appointmentAt || appointment.startTime || appointment.date
      const currentDay = appTime ? dayjs(appTime) : dayjs()
      const initialDate = currentDay.isBefore(dayjs(), 'day') ? dayjs() : currentDay
      setNewDate(initialDate)

      setSelectedSlot(null)
      setReason('')
      setSelectedPreset(null)
      setTouched(false)

      fetchSlots(currentDocId, initialDate)
    }
  }, [open, appointment, fetchSlots])

  // Khi thay đổi ngày hẹn
  const handleDateChange = (d) => {
    if (!d) return
    setNewDate(d)
    fetchSlots(selectedDoctorId, d)
  }

  // Khi thay đổi bác sĩ
  const handleDoctorChange = (docId) => {
    setSelectedDoctorId(docId)
    fetchSlots(docId, newDate)
  }

  // Phân tách khung giờ thành Ca sáng & Ca chiều
  const { morningSlots, afternoonSlots, totalAvailableSlots } = useMemo(() => {
    const morning = []
    const afternoon = []
    let availableCount = 0

    slots.forEach((slot, index) => {
      const startDayjs = dayjs(slot.startTime)
      const endDayjs = dayjs(slot.endTime)
      const startStr = startDayjs.isValid() ? startDayjs.format('HH:mm') : '08:00'
      const endStr = endDayjs.isValid() ? endDayjs.format('HH:mm') : '08:30'
      const hour = startDayjs.isValid() ? startDayjs.hour() : 8
      const isPast = startDayjs.isValid() ? startDayjs.isBefore(dayjs()) : false
      const canSelect = Boolean(slot.isAvailable) && !isPast

      if (canSelect) {
        availableCount += 1
      }

      const slotItem = {
        ...slot,
        key: slot.id || `${slot.startTime}_${index}`,
        startDayjs,
        endDayjs,
        startStr,
        endStr,
        label: `${startStr} – ${endStr}`,
        isPast,
        canSelect,
      }

      if (hour < 12) {
        morning.push(slotItem)
      } else {
        afternoon.push(slotItem)
      }
    })

    return { morningSlots: morning, afternoonSlots: afternoon, totalAvailableSlots: availableCount }
  }, [slots])

  // Tính thời gian bắt đầu và kết thúc mới từ slot đã chọn
  const computedTimes = useMemo(() => {
    if (!selectedSlot) return { start: null, end: null }
    return {
      start: dayjs(selectedSlot.startTime),
      end: dayjs(selectedSlot.endTime),
    }
  }, [selectedSlot])

  const timeValidation = useMemo(() => {
    if (!selectedSlot || !computedTimes.start || !computedTimes.end) {
      return { valid: false, error: 'Vui lòng chọn một khung giờ khám còn trống.' }
    }
    return validateRescheduleTime(computedTimes.start, computedTimes.end, dayjs())
  }, [selectedSlot, computedTimes])

  const reasonValidation = useMemo(() => {
    return validateRescheduleReason(reason)
  }, [reason])

  // Submit button enabled khi đã chọn slot hợp lệ và lý do hợp lệ
  const isFormValid = Boolean(selectedSlot) && timeValidation.valid && reasonValidation.valid

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

  const renderSlotButton = (slot) => {
    const isSelected = selectedSlot && (
      selectedSlot.key === slot.key ||
      selectedSlot.startTime === slot.startTime
    )

    let borderStyle = '1px solid #cbd5e1'
    let bgStyle = '#ffffff'
    let textColor = '#1e293b'

    if (isSelected) {
      borderStyle = '2px solid #2563eb'
      bgStyle = '#eff6ff'
      textColor = '#1d4ed8'
    } else if (!slot.canSelect) {
      borderStyle = '1px dashed #e2e8f0'
      bgStyle = '#f8fafc'
      textColor = '#94a3b8'
    }

    return (
      <Col xs={12} sm={8} md={6} key={slot.key}>
        <div
          role="button"
          tabIndex={slot.canSelect ? 0 : -1}
          onClick={() => {
            if (slot.canSelect) {
              setSelectedSlot(slot)
              setTouched(true)
            }
          }}
          onKeyDown={(e) => {
            if (slot.canSelect && (e.key === 'Enter' || e.key === ' ')) {
              e.preventDefault()
              setSelectedSlot(slot)
              setTouched(true)
            }
          }}
          style={{
            border: borderStyle,
            backgroundColor: bgStyle,
            borderRadius: 6,
            padding: '8px 6px',
            textAlign: 'center',
            cursor: slot.canSelect ? 'pointer' : 'not-allowed',
            opacity: slot.canSelect ? 1 : 0.65,
            transition: 'all 0.15s ease-in-out',
            boxShadow: isSelected ? '0 0 0 1px #2563eb' : undefined,
          }}
        >
          <div style={{ fontSize: 12.5, fontWeight: isSelected ? 700 : 600, color: textColor, marginBottom: 4 }}>
            {slot.label}
          </div>
          <div>
            {isSelected ? (
              <Tag color="blue" style={{ margin: 0, fontSize: 10.5, lineHeight: '18px', padding: '0 4px' }}>
                Đang chọn
              </Tag>
            ) : slot.canSelect ? (
              <Tag color="success" style={{ margin: 0, fontSize: 10.5, lineHeight: '18px', padding: '0 4px' }}>
                Còn trống
              </Tag>
            ) : slot.isPast ? (
              <Tag color="default" style={{ margin: 0, fontSize: 10.5, lineHeight: '18px', padding: '0 4px' }}>
                Quá giờ
              </Tag>
            ) : (
              <Tag color="error" style={{ margin: 0, fontSize: 10.5, lineHeight: '18px', padding: '0 4px' }}>
                Đã kín
              </Tag>
            )}
          </div>
        </div>
      </Col>
    )
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
      width={680}
      footer={[
        <Button key="back" onClick={onClose} disabled={loading}>
          Bỏ qua
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<SwapOutlined />}
          loading={loading}
          disabled={loading || !isFormValid}
          onClick={handleSubmit}
          id="btn-confirm-reschedule-appointment"
          style={{ backgroundColor: '#2563eb' }}
        >
          Xác nhận đổi lịch
        </Button>,
      ]}
      style={{ top: 20, maxWidth: '95vw' }}
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

        {/* Form chọn thông tin dời lịch mới */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Row gutter={12}>
            {/* Chọn Bác sĩ mới */}
            <Col xs={24} sm={14}>
              <label style={{ fontSize: 13, fontWeight: 600, color: '#0f172a', display: 'block', marginBottom: 4 }}>
                Bác sĩ khám mới:
              </label>
              <Select
                style={{ width: '100%' }}
                placeholder="Chọn bác sĩ phụ trách mới..."
                value={selectedDoctorId}
                onChange={handleDoctorChange}
                options={doctorList.map((d) => ({
                  value: d.id,
                  label: `${d.fullName || d.username} — ${d.department || 'Chuyên khoa'}`,
                }))}
              />
              <div style={{ fontSize: 11.5, color: '#64748b', marginTop: 3 }}>
                Lễ tân có thể chuyển sang bác sĩ khác nếu bệnh nhân yêu cầu.
              </div>
            </Col>

            {/* Chọn Ngày hẹn mới */}
            <Col xs={24} sm={10}>
              <label style={{ fontSize: 13, fontWeight: 600, color: '#0f172a', display: 'block', marginBottom: 4 }}>
                <span style={{ color: '#ef4444' }}>* </span>Ngày hẹn mới:
              </label>
              <DatePicker
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                value={newDate}
                onChange={handleDateChange}
                disabledDate={(current) => current && current.isBefore(dayjs(), 'day')}
                allowClear={false}
              />
            </Col>
          </Row>

          {/* Danh sách Khung giờ trống 30 phút theo ca trực bác sĩ (Blocker & Major 2) */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
              <label style={{ fontSize: 13, fontWeight: 600, color: '#0f172a' }}>
                <span style={{ color: '#ef4444' }}>* </span>Khung giờ khám 30 phút (Chọn slot trống):
              </label>
              <Button
                type="link"
                size="small"
                icon={<ReloadOutlined />}
                loading={loadingSlots}
                onClick={() => fetchSlots(selectedDoctorId, newDate)}
                style={{ padding: 0, fontSize: 12 }}
              >
                Làm mới slot
              </Button>
            </div>

            {loadingSlots ? (
              <div style={{ textAlign: 'center', padding: '24px 0', background: '#f8fafc', borderRadius: 6 }}>
                <Spin tip="Đang tải danh sách khung giờ trống của Bác sĩ..." />
              </div>
            ) : slotError ? (
              <Alert
                type="error"
                showIcon
                message="Lỗi tải khung giờ"
                description={
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 4 }}>
                    <span>{slotError}</span>
                    <Button size="small" icon={<ReloadOutlined />} onClick={() => fetchSlots(selectedDoctorId, newDate)}>
                      Thử lại
                    </Button>
                  </div>
                }
              />
            ) : slots.length === 0 ? (
              <Alert
                type="warning"
                showIcon
                message="Bác sĩ không có lịch làm việc vào ngày này"
                description="Bác sĩ không có ca trực hoặc không có khung giờ khám trong ngày được chọn. Vui lòng chọn ngày khác hoặc chuyển sang Bác sĩ khác."
                style={{ borderRadius: 6 }}
              />
            ) : totalAvailableSlots === 0 ? (
              <Alert
                type="warning"
                showIcon
                message="Tất cả khung giờ khám đã kín lịch hoặc quá giờ"
                description="Bác sĩ không còn khung giờ trống nào trong ngày này. Vui lòng chọn một ngày hẹn khác hoặc chuyển sang Bác sĩ khác."
                style={{ borderRadius: 6 }}
              />
            ) : (
              <div
                style={{
                  border: '1px solid #e2e8f0',
                  borderRadius: 8,
                  padding: '12px 14px',
                  backgroundColor: '#fbfcfd',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 12,
                }}
              >
                {/* Ca sáng */}
                {morningSlots.length > 0 && (
                  <div>
                    <div
                      style={{
                        fontSize: 12,
                        fontWeight: 700,
                        color: '#334155',
                        textTransform: 'uppercase',
                        marginBottom: 8,
                        display: 'flex',
                        alignItems: 'center',
                        gap: 6,
                      }}
                    >
                      <span>🌅 Ca sáng (07:30 – 12:00)</span>
                      <Tag color="default" style={{ fontSize: 11, margin: 0 }}>
                        {morningSlots.filter((s) => s.canSelect).length} slot trống
                      </Tag>
                    </div>
                    <Row gutter={[8, 8]}>
                      {morningSlots.map(renderSlotButton)}
                    </Row>
                  </div>
                )}

                {morningSlots.length > 0 && afternoonSlots.length > 0 && (
                  <Divider style={{ margin: '4px 0' }} />
                )}

                {/* Ca chiều */}
                {afternoonSlots.length > 0 && (
                  <div>
                    <div
                      style={{
                        fontSize: 12,
                        fontWeight: 700,
                        color: '#334155',
                        textTransform: 'uppercase',
                        marginBottom: 8,
                        display: 'flex',
                        alignItems: 'center',
                        gap: 6,
                      }}
                    >
                      <span>🌇 Ca chiều (13:00 – 17:30)</span>
                      <Tag color="default" style={{ fontSize: 11, margin: 0 }}>
                        {afternoonSlots.filter((s) => s.canSelect).length} slot trống
                      </Tag>
                    </div>
                    <Row gutter={[8, 8]}>
                      {afternoonSlots.map(renderSlotButton)}
                    </Row>
                  </div>
                )}
              </div>
            )}

            {touched && !timeValidation.valid && (
              <div style={{ color: '#ef4444', fontSize: 12, marginTop: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
                <ExclamationCircleOutlined /> {timeValidation.error}
              </div>
            )}
          </div>

          {/* Preview khung giờ mới đã chọn */}
          {selectedSlot && computedTimes.start && computedTimes.end && (
            <div
              style={{
                backgroundColor: '#eff6ff',
                border: '1px solid #bfdbfe',
                borderRadius: 6,
                padding: '10px 14px',
                fontSize: 13,
                color: '#1e40af',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <span>
                <ClockCircleOutlined style={{ marginRight: 6 }} />
                Khung giờ mới đã chọn: <strong>{computedTimes.start.format('HH:mm')} – {computedTimes.end.format('HH:mm')} ({newDate.format('DD/MM/YYYY')})</strong>
              </span>
              <Tag color="blue" style={{ margin: 0 }}>30 phút</Tag>
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
