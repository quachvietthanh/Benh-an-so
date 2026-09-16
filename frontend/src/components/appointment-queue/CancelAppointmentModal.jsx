import React from 'react'
import {
  Alert,
  Button,
  Form,
  Modal,
  Select,
  Space,
  Typography,
} from 'antd'

const { Text, Paragraph } = Typography

export default function CancelAppointmentModal({
  open,
  onCancel,
  form,
  onFinish,
  cancelModalItem,
  getPatientInfo,
  actionLoading = false,
}) {
  return (
    <Modal
      title="Hủy Lịch Hẹn Khám"
      open={open}
      onCancel={onCancel}
      footer={null}
      destroyOnClose
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Paragraph>
          Lịch hẹn: <Text strong>{cancelModalItem?.appointmentCode}</Text> | Bệnh nhân:{' '}
          <Text strong>
            {getPatientInfo
              ? getPatientInfo(cancelModalItem?.patientId, cancelModalItem?.patientName).name
              : cancelModalItem?.patientName}
          </Text>
        </Paragraph>
        <Form.Item
          name="reason"
          label="Lý do hủy lịch"
          rules={[{ required: true, message: 'Vui lòng chọn hoặc nhập lý do hủy!' }]}
        >
          <Select
            options={[
              { value: 'Bệnh nhân báo bận đột xuất', label: 'Bệnh nhân báo bận đột xuất' },
              { value: 'Bệnh nhân muốn đổi ngày/giờ khám', label: 'Bệnh nhân muốn đổi ngày/giờ khám' },
              { value: 'Bác sĩ bận lịch công tác đột xuất', label: 'Bác sĩ bận lịch công tác đột xuất' },
              { value: 'Nhập trùng thông tin lịch hẹn', label: 'Nhập trùng thông tin lịch hẹn' },
            ]}
          />
        </Form.Item>
        <Alert
          type="info"
          showIcon
          message="Xác nhận giải phóng khung giờ"
          description="Khi chuyển lịch hẹn sang trạng thái ĐÃ HỦY, hệ thống sẽ giải phóng khung giờ của Bác sĩ để người khác có thể đăng ký."
          style={{ marginBottom: 16 }}
        />
        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={onCancel}>Quay lại</Button>
            <Button type="primary" danger htmlType="submit" loading={actionLoading}>
              Xác nhận Hủy Lịch Hẹn
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  )
}
