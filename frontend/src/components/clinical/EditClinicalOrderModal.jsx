import React, { useEffect, useState } from 'react'
import {
  Modal,
  Form,
  Select,
  Input,
  Radio,
  Row,
  Col,
  message,
  Divider,
  Space,
  Tag,
} from 'antd'
import { EditOutlined, ThunderboltOutlined } from '@ant-design/icons'
import ClinicalServiceSelector from './ClinicalServiceSelector'
import { formatGender } from '../../utils/helpers'

const { Option } = Select
const { TextArea } = Input

export const EditClinicalOrderModal = ({ visible, order, onClose, onUpdateSuccess }) => {
  const [form] = Form.useForm()
  const [selectedServices, setSelectedServices] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (visible && order) {
      form.setFieldsValue({
        department: order.department || 'Khoa Nội tổng quát',
        doctorName: order.doctorName || 'BS. Phạm Hồng Anh',
        diagnosis: order.diagnosis || '',
        priority: order.priority || 'NORMAL',
      })
      setSelectedServices(order.items || [])
    }
  }, [visible, order, form])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()

      if (selectedServices.length === 0) {
        message.warning('Vui lòng giữ ít nhất 1 dịch vụ cận lâm sàng!')
        return
      }

      setLoading(true)

      const hasCompletePricing = selectedServices.every((item) =>
        item.price !== null && item.price !== undefined && Number.isFinite(Number(item.price)),
      )
      const totalAmount = hasCompletePricing
        ? selectedServices.reduce((sum, item) => sum + (Number(item.price) * Number(item.quantity || 1)), 0)
        : null

      const updatedOrder = {
        ...order,
        department: values.department,
        doctorName: values.doctorName,
        diagnosis: values.diagnosis,
        priority: values.priority,
        items: selectedServices,
        totalAmount,
        updatedAt: new Date().toISOString(),
      }

      onUpdateSuccess(updatedOrder)
      message.success(`Đã cập nhật phiếu chỉ định ${order.orderCode} thành công!`)
      onClose()
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  if (!order) return null

  return (
    <Modal
      title={
        <Space>
          <EditOutlined style={{ color: '#fa8c16', fontSize: 20 }} />
          <span style={{ fontSize: 18, fontWeight: 600 }}>Chỉnh sửa phiếu chỉ định #{order.orderCode}</span>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      onOk={handleSubmit}
      okText="Lưu thay đổi"
      cancelText="Hủy bỏ"
      confirmLoading={loading}
      width={1000}
      style={{ top: 20 }}
      destroyOnClose
    >
      <Form form={form} layout="vertical" size="middle">
        <Row gutter={16}>
          <Col xs={24} sm={8}>
            <div style={{ background: '#f5f5f5', padding: '8px 12px', borderRadius: 6, marginBottom: 12 }}>
              <div style={{ fontSize: 12, color: '#8c8c8c' }}>Bệnh nhân:</div>
              <div style={{ fontWeight: 600, fontSize: 15 }}>{order.patientName}</div>
              <div style={{ fontSize: 12, color: '#595959' }}>
                [{order.patientCode}] • {formatGender(order.gender)} ({order.age}T)
              </div>
            </div>
          </Col>

          <Col xs={12} sm={8}>
            <Form.Item name="department" label="Khoa / Phòng chỉ định" rules={[{ required: true }]}>
              <Select>
                <Option value="Khoa Nội tổng quát">Khoa Nội tổng quát</Option>
                <Option value="Khoa Ngoại khoa">Khoa Ngoại khoa</Option>
                <Option value="Khoa Tim mạch">Khoa Tim mạch</Option>
                <Option value="Khoa Nhi khoa">Khoa Nhi khoa</Option>
              </Select>
            </Form.Item>
          </Col>

          <Col xs={12} sm={8}>
            <Form.Item name="doctorName" label="Bác sĩ chỉ định" rules={[{ required: true }]}>
              <Select>
                <Option value="BS. Phạm Hồng Anh">BS. Phạm Hồng Anh</Option>
                <Option value="BS. Nguyễn Văn Minh">BS. Nguyễn Văn Minh</Option>
                <Option value="BS. Trần Quang Huy">BS. Trần Quang Huy</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col xs={24} sm={16}>
            <Form.Item name="diagnosis" label="Chẩn đoán lâm sàng / Lý do chỉ định" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
          </Col>

          <Col xs={24} sm={8}>
            <Form.Item name="priority" label="Độ ưu tiên thực hiện">
              <Radio.Group buttonStyle="solid" style={{ width: '100%' }}>
                <Radio.Button value="NORMAL" style={{ width: '50%', textAlign: 'center' }}>
                  Thường
                </Radio.Button>
                <Radio.Button value="URGENT" style={{ width: '50%', textAlign: 'center', color: '#ff4d4f' }}>
                  <ThunderboltOutlined /> Khẩn cấp
                </Radio.Button>
              </Radio.Group>
            </Form.Item>
          </Col>
        </Row>

        <Divider style={{ margin: '12px 0 16px' }}>Cập nhật Dịch vụ Cận lâm sàng</Divider>

        <ClinicalServiceSelector
          selectedServices={selectedServices}
          onChange={setSelectedServices}
        />
      </Form>
    </Modal>
  )
}

export default EditClinicalOrderModal
