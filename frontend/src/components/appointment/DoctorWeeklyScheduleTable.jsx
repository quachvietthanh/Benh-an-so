import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Divider,
  Empty,
  Form,
  Input,
  Modal,
  Radio,
  Row,
  Select,
  Skeleton,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  InfoCircleOutlined,
  LeftOutlined,
  PlusOutlined,
  RightOutlined,
  SearchOutlined,
  StopOutlined,
  SyncOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import appointmentApi from '../../api/appointmentApi.js'
import patientApi from '../../api/patientApi.js'
import { useAuthContext } from '../../context/AuthContext.jsx'
import {
  DAY_OF_WEEK_LABELS,
  SLOT_CONFIG,
  SLOT_STATUS,
  canAccessDoctorWeeklyTable,
  cleanWeeklyTableErrorMessage,
  evaluateSlotAction,
  formatAppointmentStatusVi,
  formatSlotTimeRange,
  formatWeekRange,
} from '../../utils/doctorWeeklyTableHelpers.js'

const { Text, Paragraph, Title } = Typography
const { TextArea } = Input

export default function DoctorWeeklyScheduleTable({ onAppointmentBooked }) {
  const { user } = useAuthContext()

  const [selectedDate, setSelectedDate] = useState(() => dayjs())
  const [selectedDoctorId, setSelectedDoctorId] = useState('ALL')
  const [viewMode, setViewMode] = useState('BY_DAY') // 'BY_DAY' | 'BY_WEEK'
  const [selectedDayIndex, setSelectedDayIndex] = useState(0) // 0: Thứ 2, ..., 6: Chủ nhật

  const [loading, setLoading] = useState(false)
  const [tableData, setTableData] = useState(null)
  const [loadError, setLoadError] = useState(null)

  // Booking Modal State
  const [bookingModalOpen, setBookingModalOpen] = useState(false)
  const [activeSlotTarget, setActiveSlotTarget] = useState(null)
  const [bookingLoading, setBookingLoading] = useState(false)
  const [patients, setPatients] = useState([])
  const [patientLoading, setPatientLoading] = useState(false)
  const [patientSearch, setPatientSearch] = useState('')
  const [bookForm] = Form.useForm()

  // Detail Modal State
  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [selectedAppointment, setSelectedAppointment] = useState(null)

  const isAccessAllowed = useMemo(() => {
    return canAccessDoctorWeeklyTable(user)
  }, [user])

  // Fetch Weekly Table from API
  const fetchWeeklyTable = useCallback(async (dateObj, doctorId) => {
    if (!isAccessAllowed) return

    setLoading(true)
    setLoadError(null)
    try {
      const dateStr = dateObj ? dateObj.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD')
      const params = { date: dateStr }
      if (doctorId && doctorId !== 'ALL') {
        params.doctorId = doctorId
      }

      const res = await appointmentApi.getDoctorWeeklyTable(params)
      setTableData(res.data)

      // Đồng bộ ngày được chọn vào ngày hiện tại trong tuần nếu rơi vào tuần đó
      if (res.data?.days && res.data.days.length > 0) {
        const todayStr = dayjs().format('YYYY-MM-DD')
        const todayIdx = res.data.days.findIndex((d) => d.date === todayStr)
        if (todayIdx >= 0) {
          setSelectedDayIndex(todayIdx)
        }
      }
    } catch (err) {
      console.error('[DoctorWeeklyTable API Error]:', err)
      const errorMsg = cleanWeeklyTableErrorMessage(err)
      setLoadError(errorMsg)
      message.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }, [isAccessAllowed])

  // Fetch Patients for Booking Modal
  const loadPatients = useCallback(async () => {
    if (patients.length > 0) return
    setPatientLoading(true)
    try {
      const res = await patientApi.getAll({ page: 0, size: 300 })
      const list = res.data?.content || res.data || []
      setPatients(list)
    } catch (err) {
      console.error('[Load Patients Error]:', err)
    } finally {
      setPatientLoading(false)
    }
  }, [patients.length])

  useEffect(() => {
    if (isAccessAllowed) {
      fetchWeeklyTable(selectedDate, selectedDoctorId)
    }
  }, [fetchWeeklyTable, selectedDate, selectedDoctorId, isAccessAllowed])

  // Điều hướng tuần
  const handlePrevWeek = () => {
    setSelectedDate((prev) => prev.subtract(1, 'week'))
  }

  const handleNextWeek = () => {
    setSelectedDate((prev) => prev.add(1, 'week'))
  }

  const handleToday = () => {
    setSelectedDate(dayjs())
  }

  // Danh sách bác sĩ từ dữ liệu API
  const doctorOptions = useMemo(() => {
    const list = tableData?.doctors || []
    return [
      { value: 'ALL', label: 'Tất cả Bác sĩ' },
      ...list.map((d) => ({
        value: d.id,
        label: `${d.fullName || d.username} (${d.specialtyName || 'Đa khoa'})`,
      })),
    ]
  }, [tableData?.doctors])

  // Danh sách 7 ngày
  const daysList = useMemo(() => {
    return tableData?.days || []
  }, [tableData?.days])

  const currentDayData = useMemo(() => {
    if (!daysList || daysList.length === 0) return null
    return daysList[selectedDayIndex] || daysList[0]
  }, [daysList, selectedDayIndex])

  // Thao tác khi nhấp vào một ô slot
  const handleSlotClick = (slot, doctor, dayInfo) => {
    const evaluation = evaluateSlotAction(slot, doctor?.fullName || doctor?.doctorName, dayInfo?.date)

    if (evaluation.reason === 'ON_LEAVE' || evaluation.reason === 'QTN_30_VIOLATION') {
      Modal.warning({
        title: 'Bác sĩ đang trong khoảng nghỉ phép',
        icon: <WarningOutlined style={{ color: '#ea580c' }} />,
        content: (
          <div>
            <Paragraph>{evaluation.message}</Paragraph>
            <Paragraph type="secondary" style={{ fontSize: 13 }}>
              Lưu ý: Chỉ được đặt lịch hẹn vào khung giờ nằm trong lịch làm việc của bác sĩ và không trùng khoảng nghỉ đã đăng ký.
            </Paragraph>
          </div>
        ),
        okText: 'Đã hiểu',
      })
      return
    }

    if (evaluation.reason === 'OFF_DUTY') {
      message.info(`Bác sĩ ${doctor?.fullName || doctor?.doctorName || ''} không có lịch làm việc vào khung giờ này.`)
      return
    }

    if (evaluation.reason === 'PAST') {
      message.info('Khung giờ này đã qua, không thể đặt lịch mới.')
      return
    }

    if (evaluation.reason === 'BOOKED') {
      setSelectedAppointment({
        ...slot.appointment,
        doctorName: doctor?.fullName || doctor?.doctorName,
        date: dayInfo?.date,
        slotStartTime: slot.slotStartTime,
        slotEndTime: slot.slotEndTime,
      })
      setDetailModalOpen(true)
      return
    }

    if (evaluation.canBook) {
      loadPatients()
      setActiveSlotTarget({
        slot,
        doctor,
        dayInfo,
      })
      bookForm.setFieldsValue({
        patientId: undefined,
        reason: 'Khám bệnh định kỳ',
      })
      setBookingModalOpen(true)
    }
  }

  // Submit Đặt lịch nhanh
  const handleBookSubmit = async (values) => {
    if (!activeSlotTarget) return

    setBookingLoading(true)
    try {
      const { slot, doctor } = activeSlotTarget

      const payload = {
        patientId: values.patientId,
        doctorId: doctor.id || doctor.doctorId,
        startTime: slot.startTime,
        endTime: slot.endTime,
        reason: values.reason?.trim() || 'Khám bệnh',
      }

      const res = await appointmentApi.create(payload)
      const created = res.data
      message.success(`Đặt lịch hẹn thành công: ${created?.appointmentCode || 'Thành công'}`)

      setBookingModalOpen(false)
      bookForm.resetFields()
      setActiveSlotTarget(null)

      // Refresh lại dữ liệu bảng
      await fetchWeeklyTable(selectedDate, selectedDoctorId)
      onAppointmentBooked?.(created)
    } catch (err) {
      console.error('[Book Slot Error Caught]:', err)
      const errorMsg = cleanWeeklyTableErrorMessage(err)
      message.error(errorMsg)
    } finally {
      setBookingLoading(false)
    }
  }

  // Nếu người dùng không có quyền (TC-04: Dược sĩ)
  if (!isAccessAllowed) {
    return (
      <Card style={{ borderRadius: 12, margin: '16px 0' }}>
        <Alert
          type="error"
          showIcon
          icon={<StopOutlined />}
          message="Từ chối truy cập (403 Forbidden)"
          description="Tài khoản của bạn không có quyền xem hoặc thao tác trên bảng lịch tuần theo bác sĩ. Theo quy định kiểm thử NCL-03-CN-010-TC-04, quyền này chỉ dành cho vai trò Lễ tân và Quản lý phòng khám."
        />
      </Card>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* 1. THANH ĐIỀU KHIỂN & BỘ LỌC */}
      <Card style={{ borderRadius: 12, border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Row gutter={[16, 16]} align="middle" justify="space-between">
          {/* Cột trái: Điều hướng tuần */}
          <Col xs={24} md={14} lg={12}>
            <Space wrap size={10} align="center">
              <Button icon={<LeftOutlined />} onClick={handlePrevWeek}>
                Tuần trước
              </Button>
              <Button onClick={handleToday} type="default">
                Hôm nay
              </Button>
              <Button icon={<RightOutlined />} onClick={handleNextWeek}>
                Tuần sau
              </Button>
              <DatePicker
                picker="week"
                value={selectedDate}
                onChange={(val) => val && setSelectedDate(val)}
                format="Tuần ww, YYYY"
                allowClear={false}
                style={{ width: 155 }}
              />
              <span style={{ fontSize: 13.5, color: '#334155', fontWeight: 600 }}>
                {tableData ? formatWeekRange(tableData.weekStartDate, tableData.weekEndDate) : ''}
              </span>
            </Space>
          </Col>

          {/* Cột phải: Lọc bác sĩ & chế độ xem */}
          <Col xs={24} md={10} lg={12} style={{ display: 'flex', justifyContent: 'flex-end', flexWrap: 'wrap', gap: 12 }}>
            <Select
              style={{ minWidth: 230 }}
              value={selectedDoctorId}
              onChange={setSelectedDoctorId}
              options={doctorOptions}
              placeholder="Chọn Bác sĩ"
            />
            <Radio.Group
              value={viewMode}
              onChange={(e) => setViewMode(e.target.value)}
              buttonStyle="solid"
            >
              <Radio.Button value="BY_DAY">Theo ngày (Cột Bác sĩ)</Radio.Button>
              <Radio.Button value="BY_WEEK">Toàn tuần (Cột Ngày)</Radio.Button>
            </Radio.Group>
            <Button icon={<SyncOutlined spin={loading} />} onClick={() => fetchWeeklyTable(selectedDate, selectedDoctorId)}>
              Làm mới
            </Button>
          </Col>
        </Row>

        {/* 2. THANH CHÚ THÍCH TRẠNG THÁI (LEGEND) */}
        <Divider style={{ margin: '14px 0 10px 0' }} />
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 16, fontSize: 13, color: '#475569' }}>
          <span style={{ fontWeight: 600, color: '#0f172a' }}>Chú thích ô:</span>
          {Object.entries(SLOT_CONFIG).map(([key, config]) => (
            <div key={key} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span
                style={{
                  display: 'inline-block',
                  width: 14,
                  height: 14,
                  borderRadius: 3,
                  backgroundColor: config.bgColor,
                  border: `1.5px solid ${config.borderColor}`,
                }}
              />
              <span>{config.label}</span>
            </div>
          ))}
        </div>
      </Card>

      {/* Thông báo lỗi nếu có */}
      {loadError && (
        <Alert
          type="error"
          showIcon
          message="Lỗi nạp dữ liệu lịch tuần"
          description={loadError}
          closable
        />
      )}

      {/* 3. NỘI DUNG MA TRẬN LỊCH */}
      <Card style={{ borderRadius: 12, border: '1px solid #e2e8f0', minHeight: 450, padding: 0 }}>
        {loading && !tableData ? (
          <div style={{ padding: 24 }}>
            <Skeleton active paragraph={{ rows: 10 }} />
          </div>
        ) : !tableData || !tableData.timeSlots || tableData.timeSlots.length === 0 ? (
          <Empty description="Không có dữ liệu lịch làm việc của bác sĩ trong tuần này" style={{ padding: 48 }} />
        ) : viewMode === 'BY_DAY' ? (
          /* CHẾ ĐỘ 1: THEO NGÀY (CÁC CỘT LÀ BÁC SĨ, HÀNG LÀ KHUNG GIỜ) - Chuẩn TC-01 */
          <div>
            {/* Thanh Tab chọn Ngày từ Thứ Hai đến Chủ Nhật */}
            <div
              style={{
                display: 'flex',
                borderBottom: '1px solid #e2e8f0',
                backgroundColor: '#f8fafc',
                overflowX: 'auto',
              }}
            >
              {daysList.map((day, idx) => {
                const isSelected = selectedDayIndex === idx
                const dayLabel = DAY_OF_WEEK_LABELS[day.dayOfWeek] || day.dayOfWeek
                const isToday = day.date === dayjs().format('YYYY-MM-DD')

                // Đếm số ca đã đặt trong ngày này
                let bookedCount = 0
                day.doctorSchedules?.forEach((ds) => {
                  ds.slots?.forEach((s) => {
                    if (s.status === SLOT_STATUS.BOOKED) bookedCount++
                  })
                })

                return (
                  <button
                    key={day.date}
                    type="button"
                    onClick={() => setSelectedDayIndex(idx)}
                    style={{
                      flex: 1,
                      minWidth: 120,
                      padding: '12px 10px',
                      border: 'none',
                      borderBottom: isSelected ? '3px solid #2563eb' : '3px solid transparent',
                      backgroundColor: isSelected ? '#ffffff' : 'transparent',
                      cursor: 'pointer',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 4,
                      transition: 'all 0.15s ease',
                      outline: 'none',
                    }}
                  >
                    <div style={{ fontSize: 13, fontWeight: isSelected ? 700 : 500, color: isSelected ? '#2563eb' : '#475569' }}>
                      {dayLabel} {isToday && <Tag color="blue" style={{ fontSize: 10, padding: '0 4px', margin: 0 }}>Hôm nay</Tag>}
                    </div>
                    <div style={{ fontSize: 12, color: isSelected ? '#1e293b' : '#64748b' }}>
                      {dayjs(day.date).format('DD/MM')}
                    </div>
                    {bookedCount > 0 ? (
                      <Badge count={`${bookedCount} ca`} style={{ backgroundColor: '#2563eb', fontSize: 11 }} />
                    ) : (
                      <span style={{ fontSize: 11, color: '#94a3b8' }}>Chưa có ca</span>
                    )}
                  </button>
                )
              })}
            </div>

            {/* Bảng Lưới Khung Giờ x Bác Sĩ */}
            <div style={{ overflowX: 'auto', padding: 12 }}>
              <table
                style={{
                  width: '100%',
                  borderCollapse: 'collapse',
                  textAlign: 'left',
                  tableLayout: 'fixed',
                }}
              >
                <thead>
                  <tr style={{ backgroundColor: '#f1f5f9' }}>
                    <th
                      style={{
                        width: 110,
                        padding: '10px 12px',
                        border: '1px solid #cbd5e1',
                        fontSize: 13,
                        fontWeight: 700,
                        color: '#334155',
                        textAlign: 'center',
                      }}
                    >
                      <ClockCircleOutlined style={{ marginRight: 6 }} />
                      Khung giờ
                    </th>
                    {currentDayData?.doctorSchedules
                      ?.filter((ds) => selectedDoctorId === 'ALL' || String(ds.doctorId) === String(selectedDoctorId))
                      .map((ds) => (
                        <th
                          key={ds.doctorId}
                          style={{
                            padding: '10px 12px',
                            border: '1px solid #cbd5e1',
                            fontSize: 13.5,
                            fontWeight: 700,
                            color: '#0f172a',
                            backgroundColor: ds.workingDay ? '#f8fafc' : '#f1f5f9',
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <UserOutlined style={{ color: '#2563eb' }} />
                            <span>{ds.doctorName}</span>
                          </div>
                          <div style={{ fontSize: 11.5, color: '#64748b', fontWeight: 400, marginTop: 2 }}>
                            {ds.workingDay
                              ? `Làm việc: ${ds.workingStartTime ? ds.workingStartTime.slice(0, 5) : '08:00'} - ${ds.workingEndTime ? ds.workingEndTime.slice(0, 5) : '17:00'}`
                              : 'Không có ca trong ngày'}
                          </div>
                        </th>
                      ))}
                  </tr>
                </thead>
                <tbody>
                  {tableData.timeSlots.map((timeSlot) => {
                    const slotStart = String(timeSlot).slice(0, 5)
                    const slotEnd = dayjs(`2000-01-01T${timeSlot}`).add(30, 'minute').format('HH:mm')
                    const timeRangeLabel = `${slotStart} - ${slotEnd}`

                    const targetSchedules = currentDayData?.doctorSchedules?.filter(
                      (ds) => selectedDoctorId === 'ALL' || String(ds.doctorId) === String(selectedDoctorId)
                    ) || []

                    return (
                      <tr key={timeSlot}>
                        {/* Cột 1: Khung giờ */}
                        <td
                          style={{
                            padding: '8px 10px',
                            border: '1px solid #e2e8f0',
                            backgroundColor: '#f8fafc',
                            fontSize: 12.5,
                            fontWeight: 600,
                            color: '#475569',
                            textAlign: 'center',
                            whiteSpace: 'nowrap',
                          }}
                        >
                          {timeRangeLabel}
                        </td>

                        {/* Các cột: Từng Bác sĩ */}
                        {targetSchedules.map((ds) => {
                          const slot = ds.slots?.find((s) => s.slotStartTime?.startsWith(slotStart))
                          const status = slot?.status || (ds.workingDay ? SLOT_STATUS.AVAILABLE : SLOT_STATUS.OFF_DUTY)
                          const config = SLOT_CONFIG[status] || SLOT_CONFIG.OFF_DUTY

                          return (
                            <td
                              key={`${ds.doctorId}-${timeSlot}`}
                              style={{
                                padding: 6,
                                border: '1px solid #e2e8f0',
                                backgroundColor: config.bgColor,
                                verticalAlign: 'top',
                                transition: 'background-color 0.15s ease',
                              }}
                            >
                              <div
                                onClick={() => handleSlotClick(slot, ds, currentDayData)}
                                style={{
                                  borderRadius: 6,
                                  border: `1px solid ${config.borderColor}`,
                                  padding: '6px 8px',
                                  minHeight: 52,
                                  cursor: status === SLOT_STATUS.AVAILABLE || status === SLOT_STATUS.BOOKED || status === SLOT_STATUS.ON_LEAVE ? 'pointer' : 'default',
                                  display: 'flex',
                                  flexDirection: 'column',
                                  justifyContent: 'center',
                                  backgroundColor: '#ffffff',
                                  boxShadow: '0 1px 2px rgba(0,0,0,0.03)',
                                }}
                              >
                                {status === SLOT_STATUS.AVAILABLE && (
                                  <div style={{ textAlign: 'center' }}>
                                    <Tag color="success" style={{ margin: 0, fontSize: 11, fontWeight: 600 }}>
                                      <PlusOutlined style={{ marginRight: 4 }} />
                                      Còn trống
                                    </Tag>
                                    <div style={{ fontSize: 11, color: '#16a34a', marginTop: 3 }}>
                                      Bấm để đặt lịch
                                    </div>
                                  </div>
                                )}

                                {status === SLOT_STATUS.BOOKED && (
                                  <div>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                      <Tag color="blue" style={{ margin: 0, fontSize: 11, fontWeight: 600, padding: '0 4px' }}>
                                        {slot?.appointment?.appointmentCode || 'ĐÃ ĐẶT'}
                                      </Tag>
                                      <span style={{ fontSize: 11, color: '#2563eb', fontWeight: 600 }}>Chi tiết</span>
                                    </div>
                                    <div style={{ fontSize: 12.5, fontWeight: 600, color: '#0f172a', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                      {slot?.appointment?.patientName || 'Bệnh nhân'}
                                    </div>
                                    {slot?.appointment?.patientPhone && (
                                      <div style={{ fontSize: 11, color: '#64748b' }}>
                                        SĐT: {slot.appointment.patientPhone}
                                      </div>
                                    )}
                                  </div>
                                )}

                                {status === SLOT_STATUS.ON_LEAVE && (
                                  <div style={{ textAlign: 'center' }}>
                                    <Tag color="warning" style={{ margin: 0, fontSize: 11, fontWeight: 600 }}>
                                      <StopOutlined style={{ marginRight: 4 }} />
                                      Nghỉ phép
                                    </Tag>
                                    <div style={{ fontSize: 11, color: '#b45309', marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                      {slot?.timeOffReason || 'Nghỉ phép'}
                                    </div>
                                  </div>
                                )}

                                {status === SLOT_STATUS.OFF_DUTY && (
                                  <div style={{ textAlign: 'center', color: '#94a3b8', fontSize: 11.5 }}>
                                    Không có ca
                                  </div>
                                )}

                                {status === SLOT_STATUS.PAST && (
                                  <div style={{ textAlign: 'center', color: '#94a3b8', fontSize: 11.5 }}>
                                    Đã qua
                                  </div>
                                )}
                              </div>
                            </td>
                          )
                        })}
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          /* CHẾ ĐỘ 2: TOÀN TUẦN (CÁC CỘT LÀ 7 NGÀY, HÀNG LÀ KHUNG GIỜ) */
          <div style={{ overflowX: 'auto', padding: 12 }}>
            <table
              style={{
                width: '100%',
                borderCollapse: 'collapse',
                textAlign: 'left',
                tableLayout: 'fixed',
              }}
            >
              <thead>
                <tr style={{ backgroundColor: '#f1f5f9' }}>
                  <th
                    style={{
                      width: 110,
                      padding: '10px 12px',
                      border: '1px solid #cbd5e1',
                      fontSize: 13,
                      fontWeight: 700,
                      color: '#334155',
                      textAlign: 'center',
                    }}
                  >
                    Khung giờ
                  </th>
                  {daysList.map((day) => {
                    const dayLabel = DAY_OF_WEEK_LABELS[day.dayOfWeek] || day.dayOfWeek
                    const isToday = day.date === dayjs().format('YYYY-MM-DD')
                    return (
                      <th
                        key={day.date}
                        style={{
                          padding: '10px 12px',
                          border: '1px solid #cbd5e1',
                          fontSize: 13,
                          fontWeight: 700,
                          color: '#0f172a',
                          backgroundColor: isToday ? '#eff6ff' : '#f8fafc',
                          textAlign: 'center',
                        }}
                      >
                        <div>{dayLabel}</div>
                        <div style={{ fontSize: 12, color: '#64748b', fontWeight: 400 }}>
                          {dayjs(day.date).format('DD/MM/YYYY')}
                        </div>
                      </th>
                    )
                  })}
                </tr>
              </thead>
              <tbody>
                {tableData.timeSlots.map((timeSlot) => {
                  const slotStart = String(timeSlot).slice(0, 5)
                  const slotEnd = dayjs(`2000-01-01T${timeSlot}`).add(30, 'minute').format('HH:mm')
                  const timeRangeLabel = `${slotStart} - ${slotEnd}`

                  return (
                    <tr key={timeSlot}>
                      <td
                        style={{
                          padding: '8px 10px',
                          border: '1px solid #e2e8f0',
                          backgroundColor: '#f8fafc',
                          fontSize: 12.5,
                          fontWeight: 600,
                          color: '#475569',
                          textAlign: 'center',
                        }}
                      >
                        {timeRangeLabel}
                      </td>
                      {daysList.map((day) => {
                        // Lấy lịch của bác sĩ được chọn hoặc bác sĩ đầu tiên
                        const ds =
                          selectedDoctorId === 'ALL'
                            ? day.doctorSchedules?.[0]
                            : day.doctorSchedules?.find((d) => String(d.doctorId) === String(selectedDoctorId))

                        const slot = ds?.slots?.find((s) => s.slotStartTime?.startsWith(slotStart))
                        const status = slot?.status || (ds?.workingDay ? SLOT_STATUS.AVAILABLE : SLOT_STATUS.OFF_DUTY)
                        const config = SLOT_CONFIG[status] || SLOT_CONFIG.OFF_DUTY

                        return (
                          <td
                            key={`${day.date}-${timeSlot}`}
                            style={{
                              padding: 6,
                              border: '1px solid #e2e8f0',
                              backgroundColor: config.bgColor,
                              verticalAlign: 'top',
                            }}
                          >
                            <div
                              onClick={() => ds && handleSlotClick(slot, ds, day)}
                              style={{
                                borderRadius: 6,
                                border: `1px solid ${config.borderColor}`,
                                padding: '6px 8px',
                                minHeight: 48,
                                cursor: status === SLOT_STATUS.AVAILABLE || status === SLOT_STATUS.BOOKED || status === SLOT_STATUS.ON_LEAVE ? 'pointer' : 'default',
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center',
                                backgroundColor: '#ffffff',
                                textAlign: 'center',
                              }}
                            >
                              {status === SLOT_STATUS.AVAILABLE && (
                                <span style={{ color: '#16a34a', fontSize: 11.5, fontWeight: 600 }}>
                                  + Trống
                                </span>
                              )}
                              {status === SLOT_STATUS.BOOKED && (
                                <div>
                                  <Tag color="blue" style={{ margin: 0, fontSize: 10.5, padding: '0 3px' }}>
                                    {slot?.appointment?.patientName || 'Đã đặt'}
                                  </Tag>
                                </div>
                              )}
                              {status === SLOT_STATUS.ON_LEAVE && (
                                <Tag color="warning" style={{ margin: 0, fontSize: 10.5 }}>
                                  Nghỉ phép
                                </Tag>
                              )}
                              {status === SLOT_STATUS.OFF_DUTY && (
                                <span style={{ color: '#94a3b8', fontSize: 11 }}>—</span>
                              )}
                              {status === SLOT_STATUS.PAST && (
                                <span style={{ color: '#cbd5e1', fontSize: 11 }}>Đã qua</span>
                              )}
                            </div>
                          </td>
                        )
                      })}
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* 4. MODAL ĐẶT LỊCH NHANH TỪ Ô TRỐNG (ĐÁP ỨNG TC-02) */}
      <Modal
        title={
          <Space align="center" size={8}>
            <CalendarOutlined style={{ color: '#2563eb', fontSize: 18 }} />
            <span style={{ fontWeight: 700, fontSize: 16 }}>Đặt lịch hẹn cho Bác sĩ (Ô trống)</span>
          </Space>
        }
        open={bookingModalOpen}
        onCancel={() => {
          if (!bookingLoading) {
            setBookingModalOpen(false)
            setActiveSlotTarget(null)
          }
        }}
        footer={null}
        destroyOnClose
        width={560}
      >
        {activeSlotTarget && (
          <div style={{ paddingTop: 8 }}>
            {/* Tóm tắt thông tin ô đã chọn */}
            <Card
              size="small"
              style={{
                backgroundColor: '#f8fafc',
                borderColor: '#cbd5e1',
                marginBottom: 16,
              }}
            >
              <Row gutter={[12, 8]}>
                <Col span={12}>
                  <Text type="secondary" style={{ fontSize: 12.5 }}>Bác sĩ:</Text>
                  <div style={{ fontWeight: 600, color: '#0f172a' }}>
                    {activeSlotTarget.doctor?.doctorName || activeSlotTarget.doctor?.fullName}
                  </div>
                </Col>
                <Col span={12}>
                  <Text type="secondary" style={{ fontSize: 12.5 }}>Ngày khám:</Text>
                  <div style={{ fontWeight: 600, color: '#0f172a' }}>
                    {dayjs(activeSlotTarget.dayInfo?.date).format('DD/MM/YYYY')} ({DAY_OF_WEEK_LABELS[activeSlotTarget.dayInfo?.dayOfWeek]})
                  </div>
                </Col>
                <Col span={24}>
                  <Text type="secondary" style={{ fontSize: 12.5 }}>Khung giờ đặt:</Text>
                  <div>
                    <Tag color="green" style={{ fontSize: 13, fontWeight: 600, padding: '2px 8px' }}>
                      <ClockCircleOutlined style={{ marginRight: 6 }} />
                      {formatSlotTimeRange(activeSlotTarget.slot?.slotStartTime, activeSlotTarget.slot?.slotEndTime)}
                    </Tag>
                  </div>
                </Col>
              </Row>
            </Card>

            {/* Form chọn Bệnh nhân và Lý do */}
            <Form form={bookForm} layout="vertical" onFinish={handleBookSubmit}>
              <Form.Item
                name="patientId"
                label={<span style={{ fontWeight: 600 }}>Chọn Bệnh nhân <span style={{ color: '#dc2626' }}>*</span></span>}
                rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân đặt lịch' }]}
              >
                <Select
                  showSearch
                  placeholder="Tìm tên, mã hồ sơ hoặc số điện thoại..."
                  loading={patientLoading}
                  filterOption={(input, option) =>
                    (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                  }
                  options={patients.map((p) => ({
                    value: p.id,
                    label: `${p.fullName || p.name} - Mã BN: ${p.patientCode || 'Chưa có'} - SĐT: ${p.phone || p.phoneNumber || '—'}`,
                  }))}
                  style={{ width: '100%' }}
                />
              </Form.Item>

              <Form.Item
                name="reason"
                label={<span style={{ fontWeight: 600 }}>Lý do khám bệnh</span>}
                initialValue="Khám bệnh định kỳ"
              >
                <TextArea rows={3} maxLength={300} placeholder="Nhập triệu chứng sơ bộ hoặc lý do khám..." />
              </Form.Item>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 16 }}>
                <Button
                  onClick={() => setBookingModalOpen(false)}
                  disabled={bookingLoading}
                >
                  Hủy bỏ
                </Button>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={bookingLoading}
                  style={{ minWidth: 120, fontWeight: 600 }}
                >
                  Xác nhận đặt lịch
                </Button>
              </div>
            </Form>
          </div>
        )}
      </Modal>

      {/* 5. MODAL XEM CHI TIẾT LỊCH HẸN ĐÃ ĐẶT */}
      <Modal
        title={
          <Space align="center" size={8}>
            <InfoCircleOutlined style={{ color: '#2563eb', fontSize: 18 }} />
            <span style={{ fontWeight: 700, fontSize: 16 }}>Chi tiết Lịch hẹn đã đặt</span>
          </Space>
        }
        open={detailModalOpen}
        onCancel={() => {
          setDetailModalOpen(false)
          setSelectedAppointment(null)
        }}
        footer={[
          <Button key="close" type="primary" onClick={() => setDetailModalOpen(false)}>
            Đóng
          </Button>,
        ]}
        width={500}
      >
        {selectedAppointment && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, paddingTop: 6 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: 13, color: '#64748b' }}>Mã lịch hẹn:</span>
              <Tag color="blue" style={{ fontSize: 13, fontWeight: 700, padding: '2px 8px' }}>
                {selectedAppointment.appointmentCode || 'APT'}
              </Tag>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: 13, color: '#64748b' }}>Bệnh nhân:</span>
              <span style={{ fontSize: 14, fontWeight: 600, color: '#0f172a' }}>
                {selectedAppointment.patientName}
              </span>
            </div>

            {selectedAppointment.patientCode && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 13, color: '#64748b' }}>Mã bệnh nhân:</span>
                <span style={{ fontSize: 13, color: '#334155' }}>{selectedAppointment.patientCode}</span>
              </div>
            )}

            {selectedAppointment.patientPhone && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 13, color: '#64748b' }}>Số điện thoại:</span>
                <span style={{ fontSize: 13, color: '#334155' }}>{selectedAppointment.patientPhone}</span>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: 13, color: '#64748b' }}>Bác sĩ phụ trách:</span>
              <span style={{ fontSize: 13.5, fontWeight: 600, color: '#0f172a' }}>
                {selectedAppointment.doctorName || 'Bác sĩ'}
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: 13, color: '#64748b' }}>Thời gian hẹn:</span>
              <Tag color="green" style={{ fontSize: 12.5, fontWeight: 600 }}>
                {formatSlotTimeRange(selectedAppointment.slotStartTime, selectedAppointment.slotEndTime)} - {dayjs(selectedAppointment.date).format('DD/MM/YYYY')}
              </Tag>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <span style={{ fontSize: 13, color: '#64748b' }}>Lý do khám:</span>
              <span style={{ fontSize: 13, color: '#1e293b', maxWidth: 300, textAlign: 'right' }}>
                {selectedAppointment.reason || 'Khám bệnh'}
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: 13, color: '#64748b' }}>Trạng thái:</span>
              {(() => {
                const statusMeta = formatAppointmentStatusVi(selectedAppointment.status)
                return (
                  <Tag color={statusMeta.color} style={{ fontWeight: 600 }}>
                    {statusMeta.label}
                  </Tag>
                )
              })()}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
