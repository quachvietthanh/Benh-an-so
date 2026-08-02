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
  Typography,
  Space,
  Tag,
} from 'antd'
import {
  UserOutlined,
  UserAddOutlined,
  MedicineBoxOutlined,
  ThunderboltOutlined,
  ExperimentOutlined,
} from '@ant-design/icons'
import ClinicalServiceSelector from './ClinicalServiceSelector'
import { getPatients } from '../../services/mockDataService'
import { mergePatients } from '../../utils/storageHelpers'

const { Option } = Select
const { TextArea } = Input
const { Text } = Typography

export const CreateClinicalOrderModal = ({ visible, onClose, onCreateSuccess }) => {
  const [form] = Form.useForm()
  const [patients, setPatients] = useState([])
  const [selectedServices, setSelectedServices] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (visible) {
      const allPatients = mergePatients(getPatients())
      setPatients(allPatients)
      setSelectedServices([])
      form.resetFields()
      form.setFieldsValue({
        priority: 'NORMAL',
        department: 'Khoa Nội tổng quát',
        doctorName: 'BS. Phạm Hồng Anh',
      })
    }
  }, [visible, form])

  const handlePatientSelect = (patientId) => {
    const found = patients.find((p) => String(p.id) === String(patientId))
    if (found) {
      const visitCode = `LK-${new Date().toISOString().replace(/\D/g, '').slice(0, 8)}-${found.patientCode || '01'}`
      form.setFieldsValue({
        patientCode: found.patientCode,
        patientName: found.fullName,
        gender: found.gender || 'Nam',
        age: found.age || 30,
        phone: found.phone || found.phoneNumber || '',
        visitId: visitCode,
      })
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()

      if (selectedServices.length === 0) {
        message.warning('Vui lòng chọn ít nhất 1 dịch vụ cận lâm sàng!')
        return
      }

      setLoading(true)

      const totalAmount = selectedServices.reduce((sum, item) => sum + (Number(item.price || 0) * Number(item.quantity || 1)), 0)
      const orderCode = `CD-${new Date().toISOString().replace(/\D/g, '').slice(0, 8)}-${Math.floor(100 + Math.random() * 900)}`

      const newOrder = {
        id: `ord-${Date.now()}`,
        orderCode,
        visitId: values.visitId,
        patientId: values.patientId,
        patientCode: values.patientCode,
        patientName: values.patientName,
        gender: values.gender,
        age: values.age,
        phone: values.phone,
        doctorId: 'u3',
        doctorName: values.doctorName || 'BS. Trực',
        department: values.department,
        diagnosis: values.diagnosis?.trim(),
        notes: values.notes?.trim() || '',
        priority: values.priority,
        status: 'PENDING',
        items: selectedServices,
        totalAmount,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        resultSummary: null,
      }

      onCreateSuccess(newOrder)
      message.success(`Đã tạo phiếu chỉ định ${orderCode} thành công!`)
      onClose()
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      title={
        <Space>
          <ExperimentOutlined style={{ color: '#1890ff', fontSize: 20 }} />
          <span style={{ fontSize: 18, fontWeight: 600 }}>Tạo phiếu chỉ định cận lâm sàng mới</span>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      onOk={handleSubmit}
      okText="Tạo phiếu chỉ định"
      cancelText="Hủy bỏ"
      confirmLoading={loading}
      width={1000}
      style={{ top: 20 }}
      destroyOnClose
    >
      <Form form={form} layout="vertical" size="middle">
        <Row gutter={16}>
          {/* Patient Selection */}
          <Col xs={24} sm={12} md={8}>
            <Form.Item
              name="patientId"
              label="Chọn Bệnh nhân"
              rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}
            >
              <Select
                showSearch
                placeholder="Tìm tên hoặc Mã BN..."
                onChange={handlePatientSelect}
                filterOption={(input, option) =>
                  (option?.children || '').toLowerCase().includes(input.toLowerCase())
                }
              >
                {patients.map((p) => (
                  <Option key={p.id} value={p.id}>
                    [{p.patientCode}] {p.fullName} - {p.phone || p.phoneNumber || 'SĐT chưa có'}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>

          {/* Encounter / Visit Selection */}
          <Col xs={24} sm={12} md={8}>
            <Form.Item
              name="visitId"
              label="Chọn Lượt khám"
              rules={[{ required: true, whitespace: true, message: 'Vui lòng chọn lượt khám của bệnh nhân' }]}
            >
              <Select placeholder="Chọn lượt khám...">
                <Option value="LK-HOM-NAY">Lượt khám hôm nay (Nội tổng quát)</Option>
                <Option value="LK-TAI-KHAM">Lượt tái khám theo lịch</Option>
                <Option value="LK-CAP-CUU">Lượt khám Cấp cứu</Option>
              </Select>
            </Form.Item>
          </Col>

          <Form.Item name="patientCode" hidden><Input /></Form.Item>
          <Form.Item name="patientName" hidden><Input /></Form.Item>
          <Form.Item name="gender" hidden><Input /></Form.Item>
          <Form.Item name="age" hidden><Input /></Form.Item>
          <Form.Item name="phone" hidden><Input /></Form.Item>

          {/* Department */}
          <Col xs={24} sm={12} md={8}>
            <Form.Item
              name="department"
              label="Khoa / Phòng chỉ định"
              rules={[{ required: true, message: 'Vui lòng chọn khoa chỉ định' }]}
            >
              <Select>
                <Option value="Khoa Nội tổng quát">Khoa Nội tổng quát</Option>
                <Option value="Khoa Ngoại khoa">Khoa Ngoại khoa</Option>
                <Option value="Khoa Tim mạch">Khoa Tim mạch</Option>
                <Option value="Khoa Nhi khoa">Khoa Nhi khoa</Option>
                <Option value="Khoa Sản phụ khoa">Khoa Sản phụ khoa</Option>
                <Option value="Khoa Tai Mũi Họng">Khoa Tai Mũi Họng</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          {/* Doctor */}
          <Col xs={24} sm={12} md={8}>
            <Form.Item
              name="doctorName"
              label="Bác sĩ chỉ định"
              rules={[{ required: true, message: 'Vui lòng chọn bác sĩ chỉ định' }]}
            >
              <Select>
                <Option value="BS. Phạm Hồng Anh">BS. Phạm Hồng Anh</Option>
                <Option value="BS. Nguyễn Văn Minh">BS. Nguyễn Văn Minh</Option>
                <Option value="BS. Trần Quang Huy">BS. Trần Quang Huy</Option>
                <Option value="BS. Lê Thị Hoa">BS. Lê Thị Hoa</Option>
              </Select>
            </Form.Item>
          </Col>

          {/* Clinical Diagnosis */}
          <Col xs={24} sm={12} md={10}>
            <Form.Item
              name="diagnosis"
              label="Chẩn đoán lâm sàng / Lý do chỉ định"
              rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập chẩn đoán lâm sàng' }]}
            >
              <Input placeholder="Ví dụ: Tăng huyết áp vô căn, đau ngực trái chu kỳ..." />
            </Form.Item>
          </Col>

          {/* Priority */}
          <Col xs={24} sm={12} md={6}>
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

        <Row gutter={16}>
          <Col span={24}>
            <Form.Item name="notes" label="Ghi chú thêm cho phiếu chỉ định">
              <Input.TextArea rows={2} placeholder="Ghi chú dặn dò hoặc yêu cầu đặc biệt khi lấy mẫu / chụp chiếu..." />
            </Form.Item>
          </Col>
        </Row>

        <Divider style={{ margin: '12px 0 16px' }}>Lựa chọn Dịch vụ Cận lâm sàng</Divider>

        {/* Embedded Service Selector */}
        <ClinicalServiceSelector
          selectedServices={selectedServices}
          onChange={setSelectedServices}
        />
      </Form>
    </Modal>
  )
}

export default CreateClinicalOrderModal
