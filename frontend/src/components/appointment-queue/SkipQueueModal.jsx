import React from 'react'
import { Button, Form, Modal, Select, Space, Typography } from 'antd'

const { Text, Paragraph } = Typography

export default function SkipQueueModal({
  open,
  onCancel,
  form,
  onFinish,
  skipModalItem,
  getPatientInfo,
  actionLoading = false,
}) {
  return (
    <Modal
      title="Xác nhận bỏ qua lượt khám"
      open={open}
      onCancel={onCancel}
      footer={null}
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Paragraph>
          Bệnh nhân:{' '}
          <Text strong>
            {getPatientInfo
              ? getPatientInfo(skipModalItem?.patientId, skipModalItem?.patientName).name
              : skipModalItem?.patientName}
          </Text>{' '}
          (STT: {skipModalItem?.queueNumber})
        </Paragraph>
        <Form.Item
          name="reason"
          label="Lý do bỏ qua lượt khám"
          rules={[{ required: true, message: 'Vui lòng chọn lý do!' }]}
        >
          <Select
            options={[
              { value: 'Vắng mặt khi gọi', label: 'Đã gọi 3 lần nhưng không có mặt' },
              { value: 'Khách hàng xin lùi lượt', label: 'Bệnh nhân có việc bận đột xuất' },
              { value: 'Bệnh nhân ra về', label: 'Bệnh nhân ra về không khám nữa' },
            ]}
          />
        </Form.Item>
        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={onCancel}>Hủy</Button>
            <Button type="primary" danger htmlType="submit" loading={actionLoading}>
              Xác nhận bỏ qua
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  )
}
