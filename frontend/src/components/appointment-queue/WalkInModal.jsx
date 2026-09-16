import React from 'react'
import {
  Button,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
} from 'antd'
import { UserAddOutlined } from '@ant-design/icons'

export default function WalkInModal({
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
      title="Tiếp nhận Bệnh nhân Tự đến"
      open={open}
      onCancel={onCancel}
      footer={null}
      destroyOnClose
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item label="Chọn Bệnh nhân" required style={{ marginBottom: 8 }}>
          <Row gutter={8}>
            <Col span={18}>
              <Form.Item
                name="patientId"
                noStyle
                rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân!' }]}
              >
                <Select
                  showSearch
                  placeholder="Tìm theo tên hoặc SĐT..."
                  optionFilterProp="children"
                  options={patients.map((p) => ({
                    value: p.id,
                    label: `${p.fullName || p.name} (${p.phone || p.patientCode || 'BN'})`,
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
          label="Chọn Bác sĩ khám"
          rules={[{ required: true, message: 'Vui lòng chọn bác sĩ!' }]}
        >
          <Select
            placeholder="Chọn bác sĩ phụ trách..."
            options={doctorList.map((d) => ({
              value: d.id,
              label: `${d.fullName || d.username} (${d.department || 'Phòng khám'})`,
            }))}
          />
        </Form.Item>

        <Form.Item
          name="reason"
          label="Lý do khám"
          rules={[{ required: true, message: 'Vui lòng nhập lý do khám!' }]}
        >
          <Input placeholder="VD: Đau đầu, sốt nhẹ..." />
        </Form.Item>

        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={onCancel}>Hủy</Button>
            <Button type="primary" htmlType="submit" loading={actionLoading}>
              Tiếp nhận tự đến
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  )
}
