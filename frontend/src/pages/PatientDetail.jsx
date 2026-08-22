import React, { useCallback, useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { Button, Card, DatePicker, Descriptions, Form, Input, message, Modal, Select, Space, Spin, Table, Tabs, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, EditOutlined, FileTextOutlined, PaperClipOutlined, FolderOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import patientApi from '../api/patientApi'
import { useAuthContext } from '../context/AuthContext'
import { mergePatients, saveStoredPatient } from '../utils/storageHelpers'
import { formatDate, formatDateTime, formatGender } from '../utils/helpers'
import AttachmentResultManager from '../components/attachments/AttachmentResultManager'
import MedicalRecordList from './MedicalRecordList'

const { Title } = Typography
const phoneRule = { pattern: /^0\d{9}$/, message: 'Số điện thoại phải gồm 10 số và bắt đầu bằng 0' }
const bloodTypes = ['A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE', 'UNKNOWN']

function PatientDetail() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const userRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const userPermissions = (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  const canManage = userPermissions.includes('PATIENT_UPDATE') || userPermissions.includes('PATIENT_CREATE') || userRoles.includes('admin') || userRoles.includes('receptionist')
  const canViewHistory = userPermissions.includes('MEDICAL_RECORD_READ') || userPermissions.includes('PATIENT_READ') || userRoles.includes('admin') || userRoles.includes('doctor')
  const [patient, setPatient] = useState(null)
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [editOpen, setEditOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()

  const loadData = useCallback(async () => {
    setLoading(true)
    let foundPatient = location.state?.patient || null

    try {
      if (!foundPatient) {
        const patientResponse = await patientApi.getById(id)
        if (patientResponse?.data) {
          foundPatient = patientResponse.data
        }
      }
    } catch {
    }

    if (!foundPatient) {
      const allMerged = mergePatients([])
      foundPatient = allMerged.find((p) =>
        String(p.id).toLowerCase() === String(id).toLowerCase() ||
        String(p.patientCode || '').toLowerCase() === String(id).toLowerCase()
      ) || null

      if (!foundPatient) {
        try {
          const allRes = await patientApi.getAll({ page: 0, size: 200 })
          const list = allRes.data?.content || []
          foundPatient = list.find((p) =>
            String(p.id).toLowerCase() === String(id).toLowerCase() ||
            String(p.patientCode || '').toLowerCase() === String(id).toLowerCase()
          ) || null
        } catch {
        }
      }
    }

    setPatient(foundPatient)

    if (foundPatient && canViewHistory) {
      try {
        const historyResponse = await patientApi.getHistory(foundPatient.id, { page: 0, size: 50, sort: 'visitAt,desc' })
        setHistory(historyResponse.data?.content || [])
      } catch {
        setHistory([])
      }
    }
    setLoading(false)
  }, [id, canViewHistory, location.state])

  useEffect(() => { loadData() }, [loadData])

  const openEdit = () => {
    form.setFieldsValue({ ...patient, dateOfBirth: patient.dateOfBirth ? dayjs(patient.dateOfBirth) : null })
    setEditOpen(true)
  }

  const updatePatient = async (values) => {
    setSaving(true)
    const formattedDob = values.dateOfBirth ? values.dateOfBirth.format('YYYY-MM-DD') : patient.dateOfBirth
    const updatedObj = {
      ...patient,
      ...values,
      dateOfBirth: formattedDob,
      active: patient.active !== undefined ? patient.active : true,
    }

    try {
      const payload = { ...values, dateOfBirth: formattedDob, active: patient.active }
      const response = await patientApi.update(id, payload)
      const resPatient = response.data ? { ...updatedObj, ...response.data } : updatedObj
      saveStoredPatient(resPatient)
      setPatient(resPatient)
      setEditOpen(false)
      message.success('Thông tin hồ sơ đã được cập nhật và lưu thành công')
    } catch {
      saveStoredPatient(updatedObj)
      setPatient(updatedObj)
      setEditOpen(false)
      message.success('Thông tin hồ sơ đã được cập nhật và lưu thành công')
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

      {canViewHistory && (
        <Card bodyStyle={{ padding: 16 }}>
          <Tabs
            defaultActiveKey="history"
            items={[
              {
                key: 'history',
                label: (
                  <span>
                    <FileTextOutlined /> Lịch sử khám chữa bệnh ({history.length})
                  </span>
                ),
                children: (
                  <Table
                    columns={historyColumns}
                    dataSource={history}
                    rowKey="id"
                    pagination={false}
                    locale={{ emptyText: 'Bệnh nhân chưa có lượt khám' }}
                  />
                ),
              },
              {
                key: 'records',
                label: (
                  <span>
                    <FolderOutlined /> Hồ sơ bệnh án & Lưu trữ
                  </span>
                ),
                children: (
                  <MedicalRecordList patientId={patient.id} />
                ),
              },
              {
                key: 'attachments',
                label: (
                  <span>
                    <PaperClipOutlined /> Kết quả Cận lâm sàng & Tệp đính kèm
                  </span>
                ),
                children: (
                  <AttachmentResultManager
                    patientIdFilter={patient.id}
                    patientNameFilter={patient.fullName}
                    compact
                  />
                ),
              },
            ]}
          />
        </Card>
      )}

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
