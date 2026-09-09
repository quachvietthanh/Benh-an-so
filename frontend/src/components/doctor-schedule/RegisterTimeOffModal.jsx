import React, { useState } from 'react'
import { Button, DatePicker, Form, Input, Modal, Space, Typography, message } from 'antd'
import { ClockCircleOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import doctorScheduleApi from '../../api/doctorScheduleApi.js'
import { formatDuration } from '../../utils/doctorScheduleHelpers.js'

const { TextArea } = Input
const { RangePicker } = DatePicker

/**
 * Modal to register an unexpected time-off interval for a doctor.
 */
function RegisterTimeOffModal({ open, onClose, doctor, onSuccess }) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [selectedRange, setSelectedRange] = useState(null)

  const handleRangeChange = (dates) => {
    setSelectedRange(dates)
  }

  const handleApplyPreset = (presetType) => {
    const now = dayjs()
    let start, end

    if (presetType === 'today_pm') {
      start = now.hour(13).minute(0).second(0)
      end = now.hour(17).minute(30).second(0)
      if (start.isBefore(now)) {
        start = now.add(10, 'minute')
      }
    } else if (presetType === 'tomorrow_full') {
      start = now.add(1, 'day').hour(8).minute(0).second(0)
      end = now.add(1, 'day').hour(17).minute(30).second(0)
    } else if (presetType === 'next_2days') {
      start = now.add(1, 'day').hour(8).minute(0).second(0)
      end = now.add(2, 'day').hour(17).minute(30).second(0)
    }

    if (start && end) {
      const range = [start, end]
      form.setFieldsValue({ timeRange: range })
      setSelectedRange(range)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const [startDayjs, endDayjs] = values.timeRange

      if (!endDayjs.isAfter(startDayjs)) {
        message.error('Thời gian kết thúc nghỉ phải sau thời gian bắt đầu.')
        return
      }

      if (startDayjs.isBefore(dayjs())) {
        message.error('Thời gian nghỉ không được bắt đầu trong quá khứ.')
        return
      }

      setSubmitting(true)

      const payload = {
        startTime: startDayjs.toISOString(),
        endTime: endDayjs.toISOString(),
        reason: values.reason.trim(),
      }

      const res = await doctorScheduleApi.registerTimeOff(doctor.id, payload)
      message.success('Đăng ký khoảng nghỉ đột xuất thành công!')
      form.resetFields()
      setSelectedRange(null)
      onClose()

      if (onSuccess) {
        onSuccess(res.data)
      }
    } catch (err) {
      if (err.errorFields) return
      const apiMsg = err.response?.data?.message || err.apiError?.message || 'Không thể đăng ký khoảng nghỉ. Vui lòng thử lại.'
      message.error(apiMsg)
    } finally {
      setSubmitting(false)
    }
  }

  const durationText = selectedRange && selectedRange[0] && selectedRange[1]
    ? formatDuration(selectedRange[0], selectedRange[1])
    : null

  return (
    <Modal
      title={(
        <Space align="center">
          <ClockCircleOutlined style={{ color: '#ea580c' }} />
          <span>Đăng ký khoảng nghỉ đột xuất / Nghỉ phép</span>
        </Space>
      )}
      open={open}
      onCancel={() => {
        form.resetFields()
        setSelectedRange(null)
        onClose()
      }}
      onOk={handleSubmit}
      okText="Xác nhận đăng ký nghỉ"
      cancelText="Hủy bỏ"
      confirmLoading={submitting}
      width={600}
      destroyOnClose
    >
      <div style={{ marginBottom: 16, padding: '10px 14px', background: '#f8fafc', borderRadius: 8, border: '1px solid #e2e8f0' }}>
        <div style={{ color: '#64748b', fontSize: 13 }}>Bác sĩ tiếp nhận:</div>
        <div style={{ fontWeight: 600, fontSize: 15, color: '#1e293b' }}>
          {doctor?.fullName || doctor?.username || 'Chưa chọn bác sĩ'}
        </div>
      </div>

      <Form form={form} layout="vertical">
        <Form.Item label="Gợi ý chọn nhanh khung thời gian">
          <Space wrap>
            <Button size="small" onClick={() => handleApplyPreset('today_pm')}>
              Chiều nay (13:00 - 17:30)
            </Button>
            <Button size="small" onClick={() => handleApplyPreset('tomorrow_full')}>
              Ngày mai cả ngày (08:00 - 17:30)
            </Button>
            <Button size="small" onClick={() => handleApplyPreset('next_2days')}>
              2 ngày tới
            </Button>
          </Space>
        </Form.Item>

        <Form.Item
          name="timeRange"
          label="Thời gian bắt đầu & kết thúc nghỉ"
          rules={[{ required: true, message: 'Vui lòng chọn khoảng thời gian nghỉ.' }]}
        >
          <RangePicker
            showTime={{ format: 'HH:mm' }}
            format="DD/MM/YYYY HH:mm"
            placeholder={['Bắt đầu nghỉ', 'Kết thúc nghỉ']}
            style={{ width: '100%' }}
            onChange={handleRangeChange}
            disabledDate={(current) => current && current < dayjs().startOf('day')}
          />
        </Form.Item>

        {durationText && (
          <div style={{ marginBottom: 16, marginTop: -8 }}>
            <Typography.Text type="secondary">
              Thời lượng nghỉ dự kiến: <strong style={{ color: '#ea580c' }}>{durationText}</strong>
            </Typography.Text>
          </div>
        )}

        <Form.Item
          name="reason"
          label="Lý do nghỉ"
          rules={[
            { required: true, message: 'Vui lòng nhập lý do nghỉ.' },
            { max: 500, message: 'Lý do nghỉ không được vượt quá 500 ký tự.' },
          ]}
        >
          <TextArea
            rows={3}
            placeholder="Nhập lý do chi tiết (ví dụ: Nghỉ ốm đột xuất, Đi công tác hội nghị chuyên ngành, Giải quyết việc riêng...)"
            maxLength={500}
            showCount
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default RegisterTimeOffModal
