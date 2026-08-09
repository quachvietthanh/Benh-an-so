import React, { useEffect, useState } from 'react'
import {
  Modal,
  Button,
  Form,
  Select,
  Input,
  Row,
  Col,
  message,
  Divider,
  Space,
} from 'antd'
import { ExperimentOutlined } from '@ant-design/icons'
import ClinicalServiceSelector from './ClinicalServiceSelector'
import { mergePatients, mergeQueues, saveStoredQueueItem } from '../../utils/storageHelpers'
import patientApi from '../../api/patientApi'
import queueApi from '../../api/queueApi'
import medicalRecordApi from '../../api/medicalRecordApi'
import { useAuthContext } from '../../context/AuthContext'

const { Option } = Select

export const CreateClinicalOrderModal = ({ visible, onClose, onCreateSuccess }) => {
  const { user } = useAuthContext()
  const [form] = Form.useForm()
  const [patients, setPatients] = useState([])
  const [activeQueues, setActiveQueues] = useState([])
  const [selectedServices, setSelectedServices] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!visible) return undefined

    let active = true
    const loadFormData = async () => {
      setSelectedServices([])
      form.resetFields()

      const normalizedRoles = (Array.isArray(user?.roles) ? user.roles : [user?.role])
        .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
      const useDoctorQueue = normalizedRoles.includes('doctor') && !normalizedRoles.includes('admin')
      const [patientResult, queueResult] = await Promise.allSettled([
        patientApi.getAll({ page: 0, size: 500 }),
        useDoctorQueue ? queueApi.getMyQueue() : queueApi.getQueues(),
      ])
      if (!active) return

      const apiPatients = patientResult.status === 'fulfilled'
        ? (patientResult.value.data?.content || (Array.isArray(patientResult.value.data) ? patientResult.value.data : []))
        : []
      const apiQueues = queueResult.status === 'fulfilled'
        ? (Array.isArray(queueResult.value.data) ? queueResult.value.data : (queueResult.value.data?.content || []))
        : []
      const isDemo = localStorage.getItem('token') === 'demo-token'
      const doctorQueueIds = new Set(apiQueues.map((item) => String(item.id || item.queueItemId)))
      const queues = useDoctorQueue && !isDemo
        ? mergeQueues(apiQueues).filter((item) => doctorQueueIds.has(String(item.id || item.queueItemId)))
        : mergeQueues(apiQueues)
      const examiningQueueCandidates = queues.filter((item) =>
        ['IN_PROGRESS', 'WAITING_FOR_RESULT'].includes(item.status) && item.patientId
      )
      const recordChecks = isDemo
        ? []
        : await Promise.allSettled(examiningQueueCandidates.map((item) =>
            item.visitId ? medicalRecordApi.getByVisit(item.visitId) : Promise.reject(new Error('missing-visit')),
          ))
      if (!active) return
      const examiningQueues = isDemo
        ? examiningQueueCandidates
        : examiningQueueCandidates.filter((_, index) => {
            const result = recordChecks[index]
            const record = result?.status === 'fulfilled' ? result.value?.data : null
            return Boolean(record?.medicalRecordId || record?.id) && record?.status !== 'LOCKED'
          })
      const activePatientIds = new Set(examiningQueues.map((item) => String(item.patientId)))
      const mergedPatients = mergePatients(apiPatients)
      const availablePatients = activePatientIds.size
        ? mergedPatients.filter((patient) => activePatientIds.has(String(patient.id)))
        : (isDemo ? mergedPatients : [])

      setActiveQueues(examiningQueues)
      setPatients(availablePatients)

      const defaultQueue = examiningQueues[0]
      const defaultPatient = defaultQueue
        ? availablePatients.find((patient) => String(patient.id) === String(defaultQueue.patientId))
        : availablePatients[0]

      form.setFieldsValue({
        department: user?.department || 'Khoa Nội tổng quát',
        doctorName: user?.fullName || user?.username || 'Bác sĩ trực',
        ...(defaultPatient ? {
          patientId: defaultPatient.id,
          patientCode: defaultPatient.patientCode,
          patientName: defaultPatient.fullName,
          gender: defaultPatient.gender || 'Nam',
          age: defaultPatient.age || 30,
          phone: defaultPatient.phone || defaultPatient.phoneNumber || '',
          visitId: defaultQueue?.visitId || defaultPatient.visitId,
        } : {}),
      })
    }

    loadFormData()
    return () => { active = false }
  }, [visible, form, user?.department, user?.fullName, user?.role, user?.roles, user?.username])

  const handlePatientSelect = (patientId) => {
    const found = patients.find((p) => String(p.id) === String(patientId))
    const queueItem = activeQueues.find((item) => String(item.patientId) === String(patientId))
    if (found) {
      form.setFieldsValue({
        patientCode: found.patientCode,
        patientName: found.fullName,
        gender: found.gender || 'Nam',
        age: found.age || 30,
        phone: found.phone || found.phoneNumber || '',
        visitId: queueItem?.visitId || found.visitId,
      })
    }
  }

  const handleSubmit = async (values) => {
    try {
      if (selectedServices.length === 0) {
        message.warning('Vui lòng chọn ít nhất một dịch vụ cận lâm sàng.')
        return
      }

      setLoading(true)

      const hasCompletePricing = selectedServices.every((item) =>
        item.price !== null && item.price !== undefined && Number.isFinite(Number(item.price)),
      )
      const totalAmount = hasCompletePricing
        ? selectedServices.reduce((sum, item) => sum + (Number(item.price) * Number(item.quantity || 1)), 0)
        : null
      const fallbackOrderCode = `CD-${new Date().toISOString().replace(/\D/g, '').slice(0, 8)}-${Math.floor(100 + Math.random() * 900)}`
      const isUuid = (value) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(String(value || ''))
      const isDemo = localStorage.getItem('token') === 'demo-token'
      const activeQueue = activeQueues.find((item) => String(item.patientId) === String(values.patientId))
      const activeVisit = values.visitId || activeQueue?.visitId
      const canCreateOnServer = isUuid(activeVisit) && selectedServices.every((item) => isUuid(item.serviceId || item.id))

      if (!canCreateOnServer && !isDemo) {
        message.error('Không thể tạo phiếu: lượt khám hoặc dịch vụ chưa được đồng bộ với hệ thống.')
        return
      }

      let serverOrder = null
      if (canCreateOnServer) {
        try {
          const response = await medicalRecordApi.createClinicalOrder(activeVisit, {
            clinicalReason: values.diagnosis,
            items: selectedServices.map((item) => ({
              serviceId: item.serviceId || item.id,
              instruction: item.note || '',
            })),
          })
          serverOrder = response?.data || null
          if (!serverOrder?.id) throw new Error('Máy chủ không trả về mã phiếu chỉ định.')
        } catch (error) {
          if (!isDemo) {
            console.error(error)
            message.error('Không thể tạo chỉ định cận lâm sàng. Vui lòng kiểm tra lượt khám và thử lại.')
            return
          }
        }
      }

      let queueSynced = true
      if (activeQueue?.status !== 'WAITING_FOR_RESULT') {
        const queueItemId = activeQueue?.id || activeQueue?.queueItemId
        if (!isDemo) {
          if (!queueItemId) {
            queueSynced = false
          } else {
            try {
              await queueApi.updateStatus(queueItemId, { status: 'WAITING_FOR_RESULT' })
            } catch (error) {
              console.warn('Không thể đồng bộ trạng thái hàng đợi:', error)
              queueSynced = false
            }
          }
        }
        if (queueSynced && activeQueue) {
          saveStoredQueueItem({ ...activeQueue, status: 'WAITING_FOR_RESULT' })
        }
      }

      const orderCode = serverOrder?.orderCode || fallbackOrderCode
      const normalizedStatus = serverOrder?.status === 'ORDERED'
        ? 'PENDING'
        : serverOrder?.status === 'PARTIALLY_COMPLETED'
          ? 'RESULTED'
          : serverOrder?.status || 'PENDING'

      const newOrder = {
        id: serverOrder?.id || `ord-${Date.now()}`,
        orderCode,
        visitId: serverOrder?.visitId || activeVisit,
        patientId: serverOrder?.patientId || values.patientId,
        patientCode: values.patientCode,
        patientName: values.patientName,
        gender: values.gender,
        age: values.age,
        phone: values.phone,
        doctorId: serverOrder?.orderedBy || user?.id,
        doctorName: values.doctorName || user?.fullName || 'Bác sĩ trực',
        department: values.department,
        diagnosis: values.diagnosis,
        priority: 'NORMAL',
        status: normalizedStatus,
        items: selectedServices.map((item, index) => ({
          ...item,
          id: serverOrder?.items?.[index]?.id || item.id,
          serviceCode: serverOrder?.items?.[index]?.serviceCode || item.serviceCode,
          serviceName: serverOrder?.items?.[index]?.serviceName || item.serviceName,
          status: serverOrder?.items?.[index]?.status || item.status,
        })),
        totalAmount,
        createdAt: serverOrder?.orderedAt || new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        resultSummary: null,
      }

      onCreateSuccess(newOrder)
      message.success(`Đã tạo phiếu chỉ định ${orderCode} thành công!`)
      if (!queueSynced) {
        message.warning('Phiếu chỉ định đã được tạo, nhưng trạng thái hàng đợi chưa cập nhật. Vui lòng thử lại tại màn hình hàng đợi.')
      }
      onClose()
    } catch (err) {
      console.error(err)
      message.error('Vui lòng kiểm tra lại các thông tin bắt buộc.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      title={
        <Space>
          <ExperimentOutlined style={{ color: '#1890ff', fontSize: 20 }} />
          <span style={{ fontSize: 18, fontWeight: 600 }}>Tạo chỉ định cận lâm sàng</span>
        </Space>
      }
      open={visible}
      onCancel={() => !loading && onClose()}
      footer={[
        <Button key="cancel" onClick={onClose} disabled={loading}>
          Hủy
        </Button>,
        <Button
          key="submit"
          type="primary"
          htmlType="submit"
          form="create-clinical-order-form"
          loading={loading}
        >
          Tạo chỉ định cận lâm sàng
        </Button>,
      ]}
      width={1000}
      style={{ top: 20 }}
      destroyOnClose
    >
      <Form
        id="create-clinical-order-form"
        form={form}
        layout="vertical"
        size="middle"
        onFinish={handleSubmit}
        scrollToFirstError
      >
        <Row gutter={16}>
          {/* Patient Selection */}
          <Col xs={24} sm={12} md={8}>
            <Form.Item
              name="patientId"
              label="Chọn bệnh nhân đang khám"
              rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}
            >
              <Select
                showSearch
                placeholder="Tìm tên hoặc mã bệnh nhân..."
                onChange={handlePatientSelect}
                filterOption={(input, option) =>
                  String(option?.children || '').toLowerCase().includes(input.toLowerCase())
                }
                notFoundContent="Chưa có bệnh nhân đang khám có bệnh án đang mở"
              >
                {patients.map((p) => {
                  const isExamining = activeQueues.some((q) => String(q.patientId) === String(p.id))
                  return (
                    <Option key={p.id} value={p.id}>
                      {isExamining ? '[Đang khám] ' : ''}[{p.patientCode}] {p.fullName} - {p.phone || p.phoneNumber || 'Chưa có SĐT'}
                    </Option>
                  )
                })}
              </Select>
            </Form.Item>
          </Col>

          <Form.Item name="visitId" hidden><Input /></Form.Item>
          <Form.Item name="patientCode" hidden><Input /></Form.Item>
          <Form.Item name="patientName" hidden><Input /></Form.Item>
          <Form.Item name="gender" hidden><Input /></Form.Item>
          <Form.Item name="age" hidden><Input /></Form.Item>
          <Form.Item name="phone" hidden><Input /></Form.Item>

          {/* Thông tin người tạo được lấy từ phiên đăng nhập để khớp dữ liệu máy chủ. */}
          <Col xs={12} sm={6} md={8}>
            <Form.Item
              name="department"
              label="Khoa / Phòng chỉ định"
              rules={[{ required: true, message: 'Vui lòng nhập khoa chỉ định' }]}
            >
              <Input disabled />
            </Form.Item>
          </Col>

          {/* Doctor */}
          <Col xs={12} sm={6} md={8}>
            <Form.Item
              name="doctorName"
              label="Bác sĩ chỉ định"
              rules={[{ required: true, message: 'Vui lòng chọn bác sĩ chỉ định' }]}
            >
              <Input disabled />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          {/* Clinical Diagnosis */}
          <Col span={24}>
            <Form.Item
              name="diagnosis"
              label="Chẩn đoán lâm sàng / Lý do chỉ định"
              rules={[{ required: true, message: 'Vui lòng nhập chẩn đoán lâm sàng' }]}
            >
              <Input placeholder="Ví dụ: Tăng huyết áp vô căn, đau ngực trái chu kỳ, sốt chưa rõ nguyên nhân..." />
            </Form.Item>
          </Col>

        </Row>

        <Divider style={{ margin: '12px 0 16px' }}>Lựa chọn dịch vụ cận lâm sàng</Divider>

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
