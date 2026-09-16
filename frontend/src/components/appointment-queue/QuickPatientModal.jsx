import React from 'react'
import {
  Button,
  Col,
  DatePicker,
  Form,
  Input,
  Modal,
  Radio,
  Row,
  Space,
} from 'antd'
import dayjs from 'dayjs'
import PersonalDataConsentField from '../patient/PersonalDataConsentField'

export default function QuickPatientModal({
  open,
  onCancel,
  form,
  onFinish,
  loading = false,
}) {
  const patientNameWatch = Form.useWatch('fullName', form)

  return (
    <Modal
      title="Đăng Ký Nhanh Bệnh Nhân Mới (Đồng bộ Đăng ký bệnh nhân)"
      open={open}
      onCancel={onCancel}
      footer={null}
      destroyOnClose
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item
          name="fullName"
          label="Họ và tên bệnh nhân"
          rules={[{ required: true, message: 'Vui lòng nhập họ và tên!' }]}
        >
          <Input placeholder="VD: Nguyễn Văn Nam" />
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="phone"
              label="Số điện thoại"
              rules={[{ required: true, message: 'Vui lòng nhập SĐT!' }]}
            >
              <Input placeholder="0901234567" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="gender"
              label="Giới tính"
              initialValue="MALE"
            >
              <Radio.Group>
                <Radio value="MALE">Nam</Radio>
                <Radio value="FEMALE">Nữ</Radio>
              </Radio.Group>
            </Form.Item>
          </Col>
        </Row>
        <Form.Item
          name="dateOfBirth"
          label="Ngày sinh"
          rules={[{ required: true, message: 'Vui lòng chọn ngày sinh!' }]}
        >
          <DatePicker
            format="DD/MM/YYYY"
            style={{ width: '100%' }}
            disabledDate={(date) => date && date.isAfter(dayjs(), 'day')}
          />
        </Form.Item>
        <Form.Item name="address" label="Địa chỉ">
          <Input placeholder="Số nhà, tên đường, quận/huyện..." />
        </Form.Item>
        <PersonalDataConsentField patientName={patientNameWatch} />
        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={onCancel}>Hủy</Button>
            <Button type="primary" htmlType="submit" loading={loading}>
              Lưu Bệnh Nhân Mới
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  )
}
