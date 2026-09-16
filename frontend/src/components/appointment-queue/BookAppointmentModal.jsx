import React from 'react'
import {
  Alert,
  Button,
  Col,
  DatePicker,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  TimePicker,
} from 'antd'
import { UserAddOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'

export default function BookAppointmentModal({
  open,
  onCancel,
  form,
  onFinish,
  patients = [],
  doctorList = [],
  actionLoading = false,
  onOpenQuickPatient,
}) {
  return (
    <Modal
      title="Đặt Lịch Hẹn Khám Bệnh Mới"
      open={open}
      onCancel={onCancel}
      footer={null}
      destroyOnClose
      width={600}
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item label="Bệnh nhân" required style={{ marginBottom: 8 }}>
          <Row gutter={8}>
            <Col span={18}>
              <Form.Item
                name="patientId"
                noStyle
                rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân!' }]}
              >
                <Select
                  showSearch
                  placeholder="Chọn từ Đăng ký bệnh nhân..."
                  optionFilterProp="children"
                  options={patients.map((p) => ({
                    value: p.id,
                    label: `${p.fullName || p.name} (${p.patientCode || 'BN'} - ${p.phone || p.phoneNumber || 'Chưa có SĐT'})`,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Button
                type="dashed"
                icon={<UserAddOutlined />}
                onClick={onOpenQuickPatient}
                style={{ width: '100%' }}
              >
                Tạo mới
              </Button>
            </Col>
          </Row>
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

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="appointmentDate"
              label="Ngày hẹn"
              initialValue={dayjs()}
              rules={[{ required: true, message: 'Vui lòng chọn ngày hẹn!' }]}
            >
              <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="appointmentTime"
              label="Khung giờ hẹn"
              initialValue={dayjs().add(1, 'hour')}
              rules={[{ required: true, message: 'Vui lòng chọn khung giờ!' }]}
            >
              <TimePicker format="HH:mm" minuteStep={15} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          name="reason"
          label="Lý do khám"
          rules={[{ required: true, message: 'Vui lòng nhập lý do khám!' }]}
        >
          <Input placeholder="VD: Khám định kỳ, đau ngực, ho kéo dài..." />
        </Form.Item>

        <Alert
          type="warning"
          showIcon
          message="Ràng buộc kiểm tra tự động"
          description="Hệ thống sẽ kiểm tra: (1) Khung giờ không được ở quá khứ. (2) Bác sĩ không có lịch trùng trong cùng khung giờ. Lịch hẹn tạo thành công sẽ ở trạng thái ĐÃ ĐẶT."
          style={{ marginBottom: 16 }}
        />

        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={onCancel}>Hủy</Button>
            <Button type="primary" htmlType="submit" loading={actionLoading}>
              Xác nhận Đặt Lịch Hẹn
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  )
}
