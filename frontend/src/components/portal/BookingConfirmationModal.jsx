import React, { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Result,
  Tag,
  Typography,
} from 'antd'
import {
  CalendarOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  IdcardOutlined,
  MedicineBoxOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

const { Text, Paragraph } = Typography

function BookingConfirmationModal({
  open,
  onClose,
  specialty,
  doctor,
  selectedDate,
  selectedSlot,
  patient,
  onSubmit,
  loading = false,
  onSuccessNavigate,
}) {
  const [form] = Form.useForm()
  const [bookingSuccess, setBookingSuccess] = useState(null)
  const [conflictError, setConflictError] = useState('')

  const handleConfirm = async () => {
    setConflictError('')
    try {
      const values = await form.validateFields()
      const startDayjs = dayjs(selectedSlot.startTime)
      const startTimeStr = startDayjs.format('HH:mm')

      const payload = {
        doctorId: doctor.id,
        appointmentDate: selectedDate,
        startTime: startTimeStr,
        reason: values.reason?.trim() || 'Đặt lịch hẹn khám trực tuyến',
      }

      const result = await onSubmit(payload)
      if (result) {
        try {
          const cached = JSON.parse(localStorage.getItem('portal_booked_appointments') || '[]')
          const item = {
            id: result.id || String(Date.now()),
            appointmentCode: result.appointmentCode || result.id || `AP-${dayjs().format('YYYYMMDD')}-${Math.floor(100 + Math.random() * 900)}`,
            status: result.status || 'SCHEDULED',
            bookingChannel: 'ONLINE_PORTAL',
            doctor: doctor || { fullName: 'Bác sĩ phụ trách' },
            doctorName: doctor?.fullName || doctor?.username,
            specialtyName: specialty?.name,
            startTime: selectedSlot?.startTime,
            endTime: selectedSlot?.endTime,
            reason: payload.reason,
            createdAt: new Date().toISOString(),
          }
          localStorage.setItem('portal_booked_appointments', JSON.stringify([item, ...cached]))
        } catch {
          // ignore storage error
        }
        setBookingSuccess(result)
      }
    } catch (err) {
      if (err.errorFields) return
      const status = err?.response?.status
      const msg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        'Có lỗi xảy ra khi đặt lịch hẹn.'

      if (status === 409 || msg.toLowerCase().includes('already') || msg.toLowerCase().includes('đã có người đặt') || msg.toLowerCase().includes('trùng')) {
        setConflictError(
          'Khung giờ này vừa có bệnh nhân khác đặt trước. Hệ thống đã cập nhật lại danh sách giờ, vui lòng đóng hộp thoại và chọn khung giờ khác.'
        )
      } else {
        setConflictError(msg)
      }
    }
  }

  const handleModalClose = () => {
    setBookingSuccess(null)
    setConflictError('')
    form.resetFields()
    onClose()
  }

  if (bookingSuccess) {
    return (
      <Modal
        open={open}
        footer={null}
        onCancel={handleModalClose}
        width={560}
        destroyOnClose
        centered
      >
        <Result
          status="success"
          title="Đặt lịch khám thành công!"
          subTitle={
            <div style={{ marginTop: 8 }}>
              <div style={{ fontSize: 15, color: '#1e293b' }}>
                Mã lịch hẹn của bạn là:{' '}
                <strong style={{ color: '#2563eb', fontSize: 17 }}>
                  {bookingSuccess.appointmentCode || bookingSuccess.id}
                </strong>
              </div>
              <Text type="secondary" style={{ fontSize: 13, display: 'block', marginTop: 6 }}>
                Trạng thái: <Tag color="blue">Đã đặt lịch (SCHEDULED)</Tag>
              </Text>
            </div>
          }
          extra={[
            <Button
              type="primary"
              key="view"
              onClick={() => {
                handleModalClose()
                onSuccessNavigate('/portal/my-appointments')
              }}
              style={{ background: '#2563eb', borderColor: '#2563eb' }}
            >
              Xem danh sách lịch hẹn của tôi
            </Button>,
            <Button
              key="home"
              onClick={() => {
                handleModalClose()
                onSuccessNavigate('/portal/dashboard')
              }}
            >
              Về trang chủ Portal
            </Button>,
          ]}
        >
          <div
            style={{
              background: '#f8fafc',
              padding: 16,
              borderRadius: 10,
              border: '1px solid #e2e8f0',
              fontSize: 13,
            }}
          >
            <Paragraph style={{ margin: 0 }}>
              ● <strong>Bác sĩ khám:</strong> BS. {doctor?.fullName || doctor?.username} ({specialty?.name})
            </Paragraph>
            <Paragraph style={{ margin: '4px 0 0' }}>
              ● <strong>Thời gian:</strong> {selectedSlot?.label} ngày {dayjs(selectedDate).format('DD/MM/YYYY')}
            </Paragraph>
            <Paragraph style={{ margin: '6px 0 0', color: '#64748b', fontSize: 12 }}>
              * Vui lòng có mặt tại phòng khám trước giờ hẹn 10-15 phút để làm thủ tục tiếp đón.
            </Paragraph>
          </div>
        </Result>
      </Modal>
    )
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 16, color: '#1e3a8a' }}>
          <CheckCircleFilled style={{ color: '#2563eb' }} />
          <span>Xác nhận thông tin đặt lịch khám</span>
        </div>
      }
      open={open}
      onCancel={handleModalClose}
      onOk={handleConfirm}
      confirmLoading={loading}
      okText="Xác nhận đặt lịch"
      cancelText="Quay lại"
      width={620}
      destroyOnClose
      centered
    >
      {conflictError && (
        <Alert
          type="error"
          showIcon
          icon={<ExclamationCircleOutlined />}
          message="Không thể đặt khung giờ này"
          description={conflictError}
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Summary Card */}
      <Card
        size="small"
        style={{
          background: '#f0fdf4',
          borderColor: '#bbf7d0',
          borderRadius: 10,
          marginBottom: 16,
        }}
        bodyStyle={{ padding: '14px 18px' }}
      >
        <Descriptions column={1} size="small">
          <Descriptions.Item label={<span style={{ color: '#166534', fontWeight: 600 }}><MedicineBoxOutlined /> Chuyên khoa</span>}>
            <strong style={{ color: '#14532d' }}>{specialty?.name}</strong>
          </Descriptions.Item>
          <Descriptions.Item label={<span style={{ color: '#166534', fontWeight: 600 }}><UserOutlined /> Bác sĩ khám</span>}>
            <strong style={{ color: '#14532d' }}>BS. {doctor?.fullName || doctor?.username}</strong>
          </Descriptions.Item>
          <Descriptions.Item label={<span style={{ color: '#166534', fontWeight: 600 }}><CalendarOutlined /> Ngày khám</span>}>
            <span style={{ fontWeight: 600, color: '#14532d' }}>
              {dayjs(selectedDate).format('dddd, DD/MM/YYYY')}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label={<span style={{ color: '#166534', fontWeight: 600 }}><ClockCircleOutlined /> Khung giờ</span>}>
            <Tag color="blue" style={{ fontSize: 14, fontWeight: 700, padding: '2px 10px' }}>
              {selectedSlot?.label}
            </Tag>
          </Descriptions.Item>
          {patient && (
            <Descriptions.Item label={<span style={{ color: '#166534', fontWeight: 600 }}><IdcardOutlined /> Người đặt lịch</span>}>
              <span>{patient.fullName || patient.username} {patient.phoneNumber ? `(${patient.phoneNumber})` : ''}</span>
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>

      {/* Form Input Reason */}
      <Form form={form} layout="vertical">
        <Form.Item
          name="reason"
          label={<span style={{ fontWeight: 600, fontSize: 13 }}>Lý do khám / Triệu chứng hiện tại</span>}
          rules={[
            { max: 500, message: 'Lý do khám không vượt quá 500 ký tự' },
          ]}
          initialValue="Đặt lịch hẹn khám trực tuyến"
        >
          <Input.TextArea
            rows={3}
            placeholder="Mô tả ngắn gọn triệu chứng hoặc lý do bạn muốn đi khám (ví dụ: Đau họng 2 ngày, kiểm tra định kỳ...)"
            style={{ borderRadius: 8 }}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default BookingConfirmationModal
