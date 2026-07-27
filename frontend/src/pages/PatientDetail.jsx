import React, { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, DatePicker, Descriptions, Form, Input, message, Modal, Select, Space, Spin, Table, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import patientApi from '../api/patientApi'
import { useAuthContext } from '../context/AuthContext'
import { formatDate, formatDateTime, formatGender } from '../utils/helpers'

const { Title } = Typography
const phoneRule = { pattern: /^0\d{9}$/, message: 'Số điện thoại phải gồm 10 số và bắt đầu bằng 0' }
const bloodTypes = ['A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE', 'UNKNOWN']

function PatientDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const canManage = user?.roles?.some((role) => ['admin', 'receptionist'].includes(role))
  const canViewHistory = user?.roles?.some((role) => ['admin', 'doctor'].includes(role))
  const [patient, setPatient] = useState(null)
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [editOpen, setEditOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const patientResponse = await patientApi.getById(id)
      setPatient(patientResponse.data)
      if (canViewHistory) {
        const historyResponse = await patientApi.getHistory(id, { page: 0, size: 50, sort: 'visitAt,desc' })
        setHistory(historyResponse.data.content || [])
      }
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể tải hồ sơ bệnh nhân')
    } finally {
      setLoading(false)
    }
  }, [id, canViewHistory])

  useEffect(() => { loadData() }, [loadData])

  const openEdit = () => {
    form.setFieldsValue({ ...patient, dateOfBirth: patient.dateOfBirth ? dayjs(patient.dateOfBirth) : null })
    setEditOpen(true)
  }

  const updatePatient = async (values) => {
    setSaving(true)
    try {
      const payload = { ...values, dateOfBirth: values.dateOfBirth.format('YYYY-MM-DD'), active: patient.active }
      const response = await patientApi.update(id, payload)
      setPatient(response.data)
      setEditOpen(false)
      message.success('Thông tin hồ sơ đã được cập nhật và lưu thành công')
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể cập nhật hồ sơ bệnh nhân')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div style={{ textAlign: 'center', padding: 100 }}><Spin size="large" /></div>
  if (!patient) return <div>Không tìm thấy bệnh nhân</div>

  const historyColumns = [
    { title: 'Mã lượt khám', dataIndex: 'visitCode', render: (value) => <Tag color="green">{value}</Tag> },
    { title: 'Ngày khám', dataIndex: 'visitAt', render: formatDateTime },
    { title: 'Loại khám', dataIndex: 'visitType' },
    { title: 'Lý do khám', dataIndex: 'reason' },
    { title: 'Trạng thái', dataIndex: 'visitStatus', render: (value) => <Tag>{value}</Tag> },
    { title: 'Ghi chú', dataIndex: 'note', render: (value) => value || '---' },
  ]

  return (
    <div>
      <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/patients')} style={{ marginBottom: 16 }}>Quay lại</Button>
      <Card title={<Space><Title level={5} style={{ margin: 0 }}>Thông tin bệnh nhân</Title><Tag color="blue">{patient.patientCode}</Tag></Space>}
        extra={canManage && <Button type="primary" icon={<EditOutlined />} onClick={openEdit}>Cập nhật</Button>} style={{ marginBottom: 24 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Họ tên" span={2}>{patient.fullName}</Descriptions.Item>
          <Descriptions.Item label="Ngày sinh">{formatDate(patient.dateOfBirth)}</Descriptions.Item>
          <Descriptions.Item label="Giới tính">{formatGender(patient.gender)}</Descriptions.Item>
          <Descriptions.Item label="Số điện thoại">{patient.phone || '---'}</Descriptions.Item>
          <Descriptions.Item label="Email">{patient.email || '---'}</Descriptions.Item>
          <Descriptions.Item label="Địa chỉ" span={2}>{patient.address || '---'}</Descriptions.Item>
          <Descriptions.Item label="CCCD">{patient.identityNumber || '---'}</Descriptions.Item>
          <Descriptions.Item label="Mã BHYT">{patient.insuranceNumber || '---'}</Descriptions.Item>
          <Descriptions.Item label="Nhóm máu">{patient.bloodType || '---'}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái"><Tag color={patient.active ? 'green' : 'red'}>{patient.active ? 'Đang hoạt động' : 'Ngừng hoạt động'}</Tag></Descriptions.Item>
          <Descriptions.Item label="Liên hệ khẩn cấp">{patient.emergencyContact || '---'}</Descriptions.Item>
          <Descriptions.Item label="SĐT khẩn cấp">{patient.emergencyPhone || '---'}</Descriptions.Item>
        </Descriptions>
      </Card>

      {canViewHistory && <Card title="Lịch sử khám chữa bệnh"><Table columns={historyColumns} dataSource={history} rowKey="id" pagination={false} locale={{ emptyText: 'Bệnh nhân chưa có lượt khám' }} /></Card>}

      <Modal title="Cập nhật thông tin bệnh nhân" open={editOpen} confirmLoading={saving} width={680}
        onCancel={() => setEditOpen(false)} onOk={() => form.submit()} okText="Lưu thay đổi" cancelText="Hủy">
        <Form form={form} layout="vertical" onFinish={updatePatient}>
          <Form.Item name="fullName" label="Họ tên" rules={[{ required: true }]}><Input /></Form.Item>
          <Space.Compact block>
            <Form.Item name="dateOfBirth" label="Ngày sinh" style={{ width: '100%', marginRight: 12 }} rules={[{ required: true }]}><DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="gender" label="Giới tính" style={{ width: '100%' }} rules={[{ required: true }]}><Select options={[{ value: 'MALE', label: 'Nam' }, { value: 'FEMALE', label: 'Nữ' }, { value: 'OTHER', label: 'Khác' }]} /></Form.Item>
          </Space.Compact>
          <Space.Compact block>
            <Form.Item name="phone" label="Số điện thoại" style={{ width: '100%', marginRight: 12 }} rules={[phoneRule]}><Input /></Form.Item>
            <Form.Item name="email" label="Email" style={{ width: '100%' }} rules={[{ type: 'email' }]}><Input /></Form.Item>
          </Space.Compact>
          <Form.Item name="address" label="Địa chỉ"><Input /></Form.Item>
          <Space.Compact block>
            <Form.Item name="identityNumber" label="CCCD" style={{ width: '100%', marginRight: 12 }}><Input /></Form.Item>
            <Form.Item name="insuranceNumber" label="Mã BHYT" style={{ width: '100%' }}><Input /></Form.Item>
          </Space.Compact>
          <Form.Item name="bloodType" label="Nhóm máu"><Select allowClear options={bloodTypes.map((value) => ({ value, label: value }))} /></Form.Item>
          <Space.Compact block>
            <Form.Item name="emergencyContact" label="Người liên hệ khẩn cấp" style={{ width: '100%', marginRight: 12 }}><Input /></Form.Item>
            <Form.Item name="emergencyPhone" label="SĐT khẩn cấp" style={{ width: '100%' }} rules={[phoneRule]}><Input /></Form.Item>
          </Space.Compact>
        </Form>
      </Modal>
    </div>
  )
}

export default PatientDetail
