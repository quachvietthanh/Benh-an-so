import React, { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Typography,
  message,
} from 'antd'
import { ClockCircleOutlined, UserAddOutlined, CalendarOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import appointmentWaitlistApi from '../../api/appointmentWaitlistApi.js'
import {
  TIME_PREFERENCE_OPTIONS,
  mapWaitlistErrorMessage,
  validateAddToWaitlistForm,
} from '../../utils/appointmentWaitlistHelpers.js'

const { Text, Paragraph } = Typography

export default function AddToWaitlistModal({
  open,
  onCancel,
  onSuccess,
  patients = [],
  doctorList = [],
  initialPatientId,
  initialDoctorId,
  initialDesiredDate,
  initialTimePreference = 'ANYTIME',
  onOpenDirectBooking,
}) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [availableSlotsWarning, setAvailableSlotsWarning] = useState(null)

  useEffect(() => {
    if (open) {
      setAvailableSlotsWarning(null)
      form.setFieldsValue({
        patientId: initialPatientId || undefined,
        doctorId: initialDoctorId || undefined,
        desiredDate: initialDesiredDate ? dayjs(initialDesiredDate) : dayjs(),
        timePreference: initialTimePreference || 'ANYTIME',
        note: '',
      })
    }
  }, [open, initialPatientId, initialDoctorId, initialDesiredDate, initialTimePreference, form])

  const handleSubmit = async (values) => {
    setAvailableSlotsWarning(null)

    const dateStr = values.desiredDate ? values.desiredDate.format('YYYY-MM-DD') : ''
    const validation = validateAddToWaitlistForm(
      values.patientId,
      values.doctorId,
      dateStr,
      values.note
    )

    if (!validation.isValid) {
      const firstError = Object.values(validation.errors)[0]
      message.error(firstError)
      return
    }

    const payload = {
      patientId: values.patientId,
      doctorId: values.doctorId,
      desiredDate: dateStr,
      timePreference: values.timePreference || 'ANYTIME',
      note: values.note ? values.note.trim() : null,
    }

    setSubmitting(true)
    try {
      const res = await appointmentWaitlistApi.add(payload)
      message.success('Đã thêm bệnh nhân vào danh sách chờ thành công!')
      form.resetFields()
      onSuccess?.(res.data)
      onCancel?.()
    } catch (err) {
      const mappedMsg = mapWaitlistErrorMessage(err)
      const rawMsg = err.response?.data?.message || err.message || ''
      const errorCode = err.response?.data?.code || ''

      // Xử lý riêng biệt lỗi DoctorHasAvailableSlotsException:
      // Backend từ chối cho vào danh sách chờ vì bác sĩ còn chỗ trống
      if (
        errorCode === 'DOCTOR_HAS_AVAILABLE_SLOTS' ||
        rawMsg.includes('DoctorHasAvailableSlotsException') ||
        rawMsg.includes('khung giờ trống')
      ) {
        setAvailableSlotsWarning({
          message: mappedMsg,
          patientId: values.patientId,
          doctorId: values.doctorId,
          date: values.desiredDate,
        })
      } else {
        message.error(mappedMsg)
      }
    } finally {
      setSubmitting(false)
    }
  }

  const handleSwitchToDirectBooking = () => {
    if (availableSlotsWarning && onOpenDirectBooking) {
      const target = { ...availableSlotsWarning }
      setAvailableSlotsWarning(null)
      onCancel?.()
      onOpenDirectBooking(target)
    }
  }

  return (
    <Modal
      title={
        <Space>
          <ClockCircleOutlined style={{ color: '#1677ff' }} />
          <span>Thêm Bệnh Nhân Vào Danh Sách Chờ Lịch Hẹn</span>
        </Space>
      }
      open={open}
      onCancel={onCancel}
      footer={null}
      destroyOnClose
      width={600}
    >
      <Alert
        type="info"
        showIcon
        message="Nguyên tắc danh sách chờ (FIFO)"
        description="Khi bác sĩ đã kín lịch vào ngày mong muốn, hệ thống sẽ lưu bệnh nhân vào danh sách chờ. Khi có bất kỳ lịch hẹn nào bị hủy, hệ thống sẽ tự động gợi ý bệnh nhân nộp đơn sớm nhất để lấp chỗ trống."
        style={{ marginBottom: 16 }}
      />

      {availableSlotsWarning && (
        <Alert
          type="warning"
          showIcon
          message="Bác sĩ vẫn còn khung giờ trống!"
          description={
            <div>
              <Paragraph style={{ marginBottom: 8 }}>
                {availableSlotsWarning.message}
              </Paragraph>
              {onOpenDirectBooking && (
                <Button
                  type="primary"
                  size="small"
                  icon={<CalendarOutlined />}
                  onClick={handleSwitchToDirectBooking}
                >
                  Chuyển sang Đặt lịch trực tiếp ngay
                </Button>
              )}
            </div>
          }
          style={{ marginBottom: 16 }}
        />
      )}

      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item
          name="patientId"
          label="Bệnh nhân"
          rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân!' }]}
        >
          <Select
            showSearch
            placeholder="Tìm kiếm và chọn bệnh nhân..."
            optionFilterProp="children"
            options={patients.map((p) => ({
              value: p.id,
              label: `${p.fullName || p.name} (${p.patientCode || 'BN'} - ${p.phone || p.phoneNumber || 'Chưa có SĐT'})`,
            }))}
          />
        </Form.Item>

        <Form.Item
          name="doctorId"
          label="Bác sĩ khám"
          rules={[{ required: true, message: 'Vui lòng chọn bác sĩ!' }]}
        >
          <Select
            placeholder="Chọn bác sĩ phụ trách..."
            options={doctorList.map((d) => ({
              value: d.id,
              label: `${d.fullName || d.username} - ${d.department || 'Chuyên khoa'}`,
            }))}
          />
        </Form.Item>

        <Form.Item
          name="desiredDate"
          label="Ngày mong muốn khám"
          rules={[{ required: true, message: 'Vui lòng chọn ngày mong muốn khám!' }]}
        >
          <DatePicker
            format="DD/MM/YYYY"
            style={{ width: '100%' }}
            disabledDate={(current) => current && current.isBefore(dayjs().startOf('day'))}
          />
        </Form.Item>

        <Form.Item
          name="timePreference"
          label="Khung thời gian mong muốn"
          initialValue="ANYTIME"
          rules={[{ required: true, message: 'Vui lòng chọn khung thời gian mong muốn!' }]}
        >
          <Select options={TIME_PREFERENCE_OPTIONS} />
        </Form.Item>

        <Form.Item
          name="note"
          label="Ghi chú nhu cầu khám (tùy chọn, tối đa 500 ký tự)"
          rules={[{ max: 500, message: 'Ghi chú không được vượt quá 500 ký tự!' }]}
        >
          <Input.TextArea
            rows={3}
            maxLength={500}
            showCount
            placeholder="VD: Bệnh nhân đau bụng quặn từng cơn, mong muốn được khám sớm nhất có thể nếu có người hủy..."
          />
        </Form.Item>

        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={onCancel}>Hủy bỏ</Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={submitting}
              icon={<ClockCircleOutlined />}
            >
              Thêm vào danh sách chờ
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  )
}
