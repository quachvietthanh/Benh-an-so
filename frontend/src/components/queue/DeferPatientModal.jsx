import React, { useEffect } from 'react'
import { Alert, Form, Input, Modal, Radio, Space, Tag, Typography } from 'antd'
import { ClockCircleOutlined, ExclamationCircleOutlined, UserOutlined } from '@ant-design/icons'
import { PRESET_DEFER_REASONS } from '../../utils/queueDeferRecallHelpers'

const { Text } = Typography

const QUICK_REASONS = PRESET_DEFER_REASONS

function DeferPatientModal({ open, item, onClose, onSubmit, loading }) {
  const [form] = Form.useForm()

  useEffect(() => {
    if (open && item) {
      form.setFieldsValue({
        quickReason: QUICK_REASONS[0],
        customReason: QUICK_REASONS[0],
      })
    }
  }, [open, item, form])

  const handleQuickReasonChange = (e) => {
    const val = e.target.value
    if (val !== 'Khác') {
      form.setFieldsValue({ customReason: val })
    } else {
      form.setFieldsValue({ customReason: '' })
    }
  }

  const handleFinish = (values) => {
    const finalReason = (values.customReason || values.quickReason || 'Vắng mặt khi gọi').trim()
    onSubmit({ reason: finalReason })
  }

  if (!item) return null

  const callCount = Number(item.callCount) || 1

  return (
    <Modal
      open={open}
      title={
        <Space align="center">
          <ExclamationCircleOutlined style={{ color: '#d97706', fontSize: 20 }} />
          <span>Tạm hoãn bệnh nhân vắng mặt trong hàng đợi</span>
        </Space>
      }
      okText="Xác nhận tạm hoãn"
      cancelText="Hủy bỏ"
      okButtonProps={{
        style: { backgroundColor: '#d97706', borderColor: '#d97706' },
        loading,
      }}
      cancelButtonProps={{ disabled: loading }}
      onOk={() => form.submit()}
      onCancel={onClose}
      destroyOnClose
      width={540}
    >
      <div style={{ marginTop: 16 }}>
        <Alert
          type="warning"
          showIcon
          message="Chuyển bệnh nhân sang danh sách tạm hoãn"
          description="Lượt khám sẽ được đánh dấu là Vắng mặt (Tạm hoãn). Hàng đợi sẽ tự động sẵn sàng phục vụ bệnh nhân tiếp theo. Khi bệnh nhân quay lại quầy, Lễ tân có thể đưa họ trở lại hàng đợi khám."
          style={{ marginBottom: 16 }}
        />

        <div
          style={{
            background: '#f8fafc',
            padding: '12px 16px',
            borderRadius: 8,
            marginBottom: 16,
            border: '1px solid #e2e8f0',
          }}
        >
          <Space orientation="vertical" size={6} style={{ width: '100%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Space>
                <UserOutlined style={{ color: '#64748b' }} />
                <Text strong style={{ fontSize: 15 }}>
                  {item.patientName || 'Bệnh nhân'}
                </Text>
                {item.patientCode && <Text type="secondary">({item.patientCode})</Text>}
              </Space>
              <Tag color="gold" style={{ fontWeight: 600 }}>
                STT #{String(item.queueNumber || 1).padStart(2, '0')}
              </Tag>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13 }}>
              <Text type="secondary">
                Bác sĩ: <Text strong>{item.doctorName || 'BS. Phụ trách'}</Text>
              </Text>
              <Tag icon={<ClockCircleOutlined />} color="orange" style={{ margin: 0 }}>
                Đã gọi: {callCount} lần
              </Tag>
            </div>
          </Space>
        </div>

        <Form form={form} layout="vertical" onFinish={handleFinish}>
          <Form.Item label="Lý do tạm hoãn thường gặp" name="quickReason">
            <Radio.Group onChange={handleQuickReasonChange} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {QUICK_REASONS.map((r) => (
                <Radio key={r} value={r}>
                  {r}
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>

          <Form.Item
            label="Chi tiết lý do ghi nhận"
            name="customReason"
            rules={[{ required: true, message: 'Vui lòng nhập lý do tạm hoãn bệnh nhân.' }]}
          >
            <Input.TextArea
              rows={3}
              placeholder="Nhập ghi chú hoặc lý do tạm hoãn bệnh nhân..."
              maxLength={255}
              showCount
            />
          </Form.Item>
        </Form>
      </div>
    </Modal>
  )
}

export default DeferPatientModal
