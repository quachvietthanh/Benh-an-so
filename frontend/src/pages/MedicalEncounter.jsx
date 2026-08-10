import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  EyeOutlined,
  MedicineBoxOutlined,
  PrinterOutlined,
  SearchOutlined,
  SolutionOutlined,
} from '@ant-design/icons'

import clinicalServiceApi from '../api/clinicalServiceApi'
import medicalRecordApi from '../api/medicalRecordApi'
import queueApi from '../api/queueApi'
import visitApi from '../api/visitApi'
import ClinicalOrderPrintModal from '../components/clinical/ClinicalOrderPrintModal'
import MedicalEncounterForm from '../components/clinical/MedicalEncounterForm'
import { useAuthContext } from '../context/AuthContext'
import { commonIcd10List, icd10Categories, searchIcd10 } from '../utils/icd10Data'
import {
  buildClinicalOrderPayload,
  buildDiagnosisPayload,
  buildMedicalRecordPayload,
  getQueueInProgressBlockReason,
  normalizeMedicalRecordDetail,
  unwrapCollection,
} from '../utils/workflowContract'

const { Text, Paragraph, Title } = Typography

const getApiMessage = (error, fallback) =>
  error?.response?.data?.message ||
  Object.values(error?.response?.data?.errors || {})[0] ||
  error?.message ||
  fallback

const mapClinicalService = (item) => ({
  id: item.id,
  code: item.serviceCode,
  name: item.serviceName,
  category:
    item.serviceType === 'LAB_TEST'
      ? 'XET_NGHIEM'
      : item.serviceType === 'IMAGING'
        ? 'CDHA'
        : 'THU_THUAT',
  department: item.description || 'Dịch vụ cận lâm sàng',
  price: 0,
})

function MedicalEncounter() {
  const { visitId: visitIdFromPath } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const [form] = Form.useForm()

  const visitId = visitIdFromPath || location.state?.visitId
  const roles = useMemo(
    () => (user?.roles || []).map((role) => String(role).toLowerCase().replace(/^role_/, '')),
    [user?.roles],
  )
  const canEditEncounter = roles.includes('doctor') || roles.includes('admin')

  const [encounter, setEncounter] = useState(null)
  const [currentRecordId, setCurrentRecordId] = useState(null)
  const [records, setRecords] = useState([])
  const [clinicalServices, setClinicalServices] = useState([])
  const [serviceCatalogError, setServiceCatalogError] = useState('')
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('current')
  const [viewing, setViewing] = useState(null)

  const [vitalSigns, setVitalSigns] = useState({
    bp: '',
    pulse: '',
    temp: '37.0',
    respRate: '16',
    weight: '',
    height: '',
    spO2: '98',
  })
  const [diagnosisType, setDiagnosisType] = useState('DEFINITIVE')
  const [primaryIcd, setPrimaryIcd] = useState(null)
  const [secondaryIcds, setSecondaryIcds] = useState([])
  const [diagnosisModalOpen, setDiagnosisModalOpen] = useState(false)
  const [icdSearchQuery, setIcdSearchQuery] = useState('')
  const [icdCategory, setIcdCategory] = useState('ALL')
  const [backendIcdCatalog, setBackendIcdCatalog] = useState([])
  const [icdSearching, setIcdSearching] = useState(false)
  const [recentIcds, setRecentIcds] = useState(loadRecentDiagnoses)

  const [selectedOrders, setSelectedOrders] = useState([])
  const [orderCategory, setOrderCategory] = useState('ALL')
  const [orderSearchQuery, setOrderSearchQuery] = useState('')
  const [printModalOpen, setPrintModalOpen] = useState(false)

  const selectedPatientObj = useMemo(() => {
    if (!encounter?.patient) return null
    return {
      ...encounter.patient,
      phoneNumber: encounter.patient.phone,
      healthInsuranceCode: encounter.patient.insuranceNumber,
      medicalHistory: form.getFieldValue('medicalHistory'),
    }
  }, [encounter, form])

  const hydrateRecord = useCallback((detail) => {
    if (!detail) {
      setCurrentRecordId(null)
      setPrimaryIcd(null)
      setSecondaryIcds([])
      return
    }

    setCurrentRecordId(detail.medicalRecordId)
    form.setFieldsValue({
      patientId: detail.patient?.id,
      symptoms: detail.symptoms || detail.chiefComplaint || '',
      medicalHistory: detail.medicalHistory || '',
      examinationNote: detail.physicalExamination || '',
      treatmentPlan: detail.treatmentPlan || detail.doctorInstructions || '',
      diagnosisText:
        detail.primaryIcdCode && detail.primaryIcdName
          ? `[${detail.primaryIcdCode}] ${detail.primaryIcdName}`
          : detail.conclusion || '',
    })

    const diagnoses = Array.isArray(detail.diagnoses) ? detail.diagnoses : []
    const primary = diagnoses.find((item) => item.diagnosisType === 'PRIMARY')
    setPrimaryIcd(
      primary
        ? {
          code: primary.diagnosisCode,
          name: primary.diagnosisName,
          note: primary.note,
        }
        : detail.primaryIcdCode
          ? { code: detail.primaryIcdCode, name: detail.primaryIcdName }
          : null,
    )
    setSecondaryIcds(
      diagnoses
        .filter((item) => item.diagnosisType === 'SECONDARY')
        .map((item) => ({
          code: item.diagnosisCode,
          name: item.diagnosisName,
          note: item.note,
        })),
    )
  }, [form])

  const loadWorkflow = useCallback(async () => {
    if (!visitId) return
    setLoading(true)
    setLoadError('')
    setServiceCatalogError('')

    try {
      const encounterResponse = await visitApi.getEncounter(visitId)
      const encounterData = encounterResponse.data
      setEncounter(encounterData)
      form.setFieldsValue({
        patientId: encounterData.patient?.id,
        symptoms: encounterData.visit?.reason || '',
      })

      const [recordResult, historyResult, serviceResult] = await Promise.allSettled([
        medicalRecordApi.getByVisit(visitId),
        encounterData.patient?.id
          ? medicalRecordApi.getByPatient(encounterData.patient.id)
          : Promise.resolve({ data: [] }),
        clinicalServiceApi.getCatalog({ page: 0, size: 100 }),
      ])

      if (recordResult.status === 'fulfilled') {
        hydrateRecord(recordResult.value.data)
      } else if (recordResult.reason?.response?.status === 404) {
        hydrateRecord(null)
      } else {
        throw recordResult.reason
      }

      const history =
        historyResult.status === 'fulfilled' && Array.isArray(historyResult.value.data)
          ? historyResult.value.data.map(normalizeMedicalRecordDetail).filter(Boolean)
          : []
      setRecords(history)

      if (serviceResult.status === 'fulfilled') {
        setClinicalServices(unwrapCollection(serviceResult.value.data).map(mapClinicalService))
      } else {
        setClinicalServices([])
        setServiceCatalogError('Không tải được danh mục dịch vụ thật từ backend. Không thể tạo chỉ định lúc này.')
      }
    } catch (error) {
      setEncounter(null)
      setLoadError(getApiMessage(error, 'Không thể tải ngữ cảnh lượt khám.'))
    } finally {
      setLoading(false)
    }
  }, [form, hydrateRecord, visitId])

  useEffect(() => {
    loadWorkflow()
  }, [loadWorkflow])

  useEffect(() => {
    const query = icdSearchQuery.trim()
    if (!query) {
      setBackendIcdCatalog([])
      return
    }

    const timer = setTimeout(async () => {
      try {
        const response = await medicalRecordApi.getDiagnosisCatalog(query)
        setBackendIcdCatalog(Array.isArray(response.data) ? response.data : [])
      } catch {
        setBackendIcdCatalog([])
      }
    }, 250)

    return () => clearTimeout(timer)
  }, [icdSearchQuery])

  const bmiValue = useMemo(() => {
    const weight = Number(vitalSigns.weight)
    const height = Number(vitalSigns.height) / 100
    return weight > 0 && height > 0 ? (weight / (height * height)).toFixed(1) : null
  }, [vitalSigns.height, vitalSigns.weight])

  const filteredIcdList = useMemo(() => {
    const combined = new Map()
    searchIcd10(icdSearchQuery, icdCategory).forEach((item) => combined.set(item.code, item))
    backendIcdCatalog.forEach((item) =>
      combined.set(item.code, {
        id: item.id,
        code: item.code,
        name: item.name,
        category: 'ALL',
      }),
    )
    return Array.from(combined.values())
  }, [backendIcdCatalog, icdCategory, icdSearchQuery])

  const filteredCatalog = useMemo(() => {
    const query = orderSearchQuery.trim().toLowerCase()
    return clinicalServices.filter(
      (item) =>
        (orderCategory === 'ALL' || item.category === orderCategory) &&
        (!query || item.name.toLowerCase().includes(query) || item.code.toLowerCase().includes(query)),
    )
  }, [clinicalServices, orderCategory, orderSearchQuery])

  const totalOrderFee = useMemo(
    () => selectedOrders.reduce((sum, item) => sum + (Number(item.price) || 0), 0),
    [selectedOrders],
  )

  const prescriptionBlockReason = getQueueInProgressBlockReason(
    encounter?.queueItem,
    'chuyển sang kê đơn',
  )

  const handleAddOrder = (catalogItem) => {
    if (selectedOrders.some((item) => item.id === catalogItem.id)) {
      message.info('Dịch vụ này đã có trong phiếu chỉ định.')
      return
    }
    setSelectedOrders((items) => [...items, { ...catalogItem, isUrgent: false, note: '' }])
  }

  const handleRemoveOrder = (code) =>
    setSelectedOrders((items) => items.filter((item) => item.code !== code))

  const handleToggleUrgent = (code) =>
    setSelectedOrders((items) =>
      items.map((item) => (item.code === code ? { ...item, isUrgent: !item.isUrgent } : item)),
    )

  const handleUpdateOrderNote = (code, note) =>
    setSelectedOrders((items) =>
      items.map((item) => (item.code === code ? { ...item, note } : item)),
    )

  const resolveDiagnosis = async (diagnosis) => {
    if (diagnosis?.id) return diagnosis
    if (!diagnosis?.code) throw new Error('Vui lòng chọn chẩn đoán ICD-10 từ danh mục backend.')

    const response = await medicalRecordApi.getDiagnosisCatalog(diagnosis.code)
    const exact = (response.data || []).find(
      (item) => String(item.code).toUpperCase() === String(diagnosis.code).toUpperCase(),
    )
    if (!exact) {
      throw new Error(`Mã ${diagnosis.code} chưa tồn tại trong danh mục chẩn đoán backend.`)
    }
    return { id: exact.id, code: exact.code, name: exact.name, note: diagnosis.note }
  }

  const openPrescription = async (medicalRecordId) => {
    if (!medicalRecordId) {
      message.error('Không có medicalRecordId thật để chuyển sang kê đơn.')
      return false
    }

    const queueItemId = encounter?.queueItem?.id
    if (!queueItemId) {
      message.error('Không có queueItemId thật để chuyển sang kê đơn.')
      return false
    }

    try {
      const response = await queueApi.getById(queueItemId)
      const liveQueueItem = response?.data
      if (!liveQueueItem?.id || String(liveQueueItem.id) !== String(queueItemId)) {
        throw new Error('Backend không trả đúng queue item của lượt khám.')
      }

      const blockReason = getQueueInProgressBlockReason(liveQueueItem, 'chuyển sang kê đơn')
      if (blockReason) throw new Error(blockReason)

      setEncounter((current) =>
        current
          ? { ...current, queueItem: { ...current.queueItem, ...liveQueueItem } }
          : current,
      )
      navigate(`/prescriptions/${medicalRecordId}`, {
        state: {
          visitId,
          queueItemId: liveQueueItem.id,
        },
      })
      return true
    } catch (error) {
      message.error(getApiMessage(error, 'Không thể chuyển sang kê đơn.'))
      return false
    }
  }

  const showSuccessModal = (medicalRecordId) => {
    Modal.confirm({
      title: 'Đã lưu bệnh án theo đúng lượt khám',
      icon: <CheckCircleOutlined style={{ color: '#16a34a' }} />,
      content: (
        <div>
          <Paragraph>
            Medical record ID: <Text code>{medicalRecordId}</Text>
          </Paragraph>
          <Paragraph>Tiếp tục kê đơn khi bệnh án còn ở trạng thái có thể chỉnh sửa.</Paragraph>
        </div>
      ),
      okText: 'Chuyển sang kê đơn',
      cancelText: 'Ở lại bệnh án',
      onOk: () => openPrescription(medicalRecordId),
    })
  }

  const saveRecord = async () => {
    if (!visitId || !encounter) {
      message.error('Không có visitId hợp lệ để lưu bệnh án.')
      return
    }
    if (!encounter.queueItem?.id) {
      message.error('Không có queueItemId thật trong encounter để lưu bệnh án.')
      return
    }

    let values
    try {
      values = await form.validateFields()
    } catch {
      message.error('Vui lòng nhập triệu chứng và thông tin khám bắt buộc.')
      return
    }

    if (!primaryIcd) {
      message.error('Vui lòng chọn chẩn đoán chính từ danh mục ICD-10.')
      return
    }

    if (selectedOrders.some((item) => !item.id)) {
      message.error('Phiếu chỉ định chứa dịch vụ không có UUID backend.')
      return
    }

    if (selectedOrders.length > 0) {
      const orderBlockReason = getQueueInProgressBlockReason(
        encounter.queueItem,
        'tạo chỉ định cận lâm sàng mới',
      )
      if (orderBlockReason) {
        message.error(orderBlockReason)
        return
      }
    }

    setSaving(true)
    let persistedRecordId = currentRecordId
    try {
      const [resolvedPrimary, resolvedSecondary] = await Promise.all([
        resolveDiagnosis(primaryIcd),
        Promise.all(secondaryIcds.map(resolveDiagnosis)),
      ])
      const diagnosisText = [
        `[${resolvedPrimary.code}] ${resolvedPrimary.name}`,
        ...resolvedSecondary.map((item) => `[${item.code}] ${item.name}`),
      ].join('; ')

      const recordPayload = buildMedicalRecordPayload({
        visitId,
        values: { ...values, conclusion: diagnosisText },
        vitalSigns,
      })

      let recordResponse
      if (persistedRecordId) {
        const updatePayload = Object.fromEntries(
          Object.entries(recordPayload).filter(([key]) => key !== 'visitId'),
        )
        recordResponse = await medicalRecordApi.update(persistedRecordId, updatePayload)
      } else {
        recordResponse = await medicalRecordApi.create(recordPayload)
      }

      persistedRecordId = recordResponse.data?.id || persistedRecordId
      if (!persistedRecordId) throw new Error('Backend không trả medicalRecordId sau khi lưu.')
      setCurrentRecordId(persistedRecordId)

      await medicalRecordApi.recordDiagnosis(
        persistedRecordId,
        buildDiagnosisPayload({
          primaryDiagnosis: resolvedPrimary,
          secondaryDiagnoses: resolvedSecondary,
          note: values.examinationNote || values.symptoms,
        }),
      )

      let liveQueueItem
      if (selectedOrders.length > 0) {
        const liveQueueResponse = await queueApi.getById(encounter.queueItem.id)
        const queueBeforeOrder = liveQueueResponse?.data
        if (
          !queueBeforeOrder?.id ||
          String(queueBeforeOrder.id) !== String(encounter.queueItem.id)
        ) {
          throw new Error('Backend không trả đúng queue item trước khi tạo chỉ định.')
        }
        const orderBlockReason = getQueueInProgressBlockReason(
          queueBeforeOrder,
          'tạo chỉ định cận lâm sàng mới',
        )
        if (orderBlockReason) throw new Error(orderBlockReason)

        await medicalRecordApi.createClinicalOrder(
          visitId,
          buildClinicalOrderPayload({ clinicalReason: diagnosisText, orders: selectedOrders }),
        )
        const queueResponse = await queueApi.updateStatus(
          encounter.queueItem.id,
          'WAITING_FOR_RESULT',
        )
        liveQueueItem = queueResponse?.data
        if (
          !liveQueueItem?.id ||
          String(liveQueueItem.id) !== String(encounter.queueItem.id) ||
          liveQueueItem.status !== 'WAITING_FOR_RESULT'
        ) {
          throw new Error('Backend không xác nhận queue item đã chuyển sang WAITING_FOR_RESULT.')
        }
        setSelectedOrders([])
      } else {
        const queueResponse = await queueApi.getById(encounter.queueItem.id)
        liveQueueItem = queueResponse?.data
        if (!liveQueueItem?.id || String(liveQueueItem.id) !== String(encounter.queueItem.id)) {
          throw new Error('Backend không trả đúng queue item của lượt khám.')
        }
      }

      const continuationBlockReason = getQueueInProgressBlockReason(
        liveQueueItem,
        'chuyển sang kê đơn',
      )
      if (liveQueueItem.status !== 'WAITING_FOR_RESULT' && continuationBlockReason) {
        throw new Error(continuationBlockReason)
      }

      message.success('Đã lưu bệnh án, chẩn đoán và chỉ định vào backend.')
      await loadWorkflow()
      if (liveQueueItem.status === 'WAITING_FOR_RESULT') {
        Modal.confirm({
          title: 'Lượt khám đang chờ kết quả cận lâm sàng',
          content: continuationBlockReason,
          okText: 'Về hàng đợi',
          cancelText: 'Ở lại bệnh án',
          onOk: () => navigate('/appointments'),
        })
      } else {
        showSuccessModal(persistedRecordId)
      }
    } catch (error) {
      const prefix = persistedRecordId
        ? `Bệnh án ${persistedRecordId} đã có trên backend nhưng một bước đồng bộ chưa hoàn tất. `
        : ''
      message.error(prefix + getApiMessage(error, 'Không thể lưu bệnh án.'))
    } finally {
      setSaving(false)
    }
  }

  const historyColumns = [
    {
      title: 'Mã bệnh án',
      dataIndex: 'medicalRecordId',
      render: (value) => <Text code>{value}</Text>,
    },
    { title: 'Mã lượt khám', dataIndex: 'visitCode', render: (value) => value || '—' },
    { title: 'Bệnh nhân', dataIndex: 'patientName' },
    { title: 'Chẩn đoán', dataIndex: 'diagnosis', render: (value) => value || 'Chưa ghi nhận' },
    { title: 'Bác sĩ', dataIndex: 'doctorName', render: (value) => value || '—' },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      render: (value) => <Tag color={value === 'LOCKED' ? 'green' : 'processing'}>{value === 'LOCKED' ? 'Đã khóa' : 'Đang mở (Bản nháp)'}</Tag>,
    },
    {
      title: '',
      render: (_, record) => (
        <Button size="small" icon={<EyeOutlined />} onClick={() => setViewing(record)}>
          Xem
        </Button>
      ),
    },
  ]

  if (!visitId) {
    return (
      <Card>
        <Alert
          type="warning"
          showIcon
          message="Chưa chọn lượt khám"
          description="Màn khám bệnh phải được mở từ một lượt khám trong hàng đợi. Không thể chọn bệnh nhân tự do tại đây."
          action={<Button onClick={() => navigate('/appointments')}>Mở danh sách lượt khám</Button>}
        />
      </Card>
    )
  }

  if (loading && !encounter) {
    return <Spin fullscreen tip="Đang tải ngữ cảnh lượt khám..." />
  }

  if (loadError) {
    return (
      <Alert
        type="error"
        showIcon
        message="Không thể mở lượt khám"
        description={loadError}
        action={<Button onClick={loadWorkflow}>Thử lại</Button>}
      />
    )
  }

  return (
    <div style={{ paddingBottom: 40 }}>
      <div className="page-header" style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', gap: 16 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <MedicineBoxOutlined style={{ color: '#2563eb' }} /> Khám bệnh & Chẩn đoán bệnh
          </Title>
          <Text type="secondary">Bệnh án được gắn cố định với lượt khám và số thứ tự trong hàng đợi khám.</Text>
        </div>
        {canEditEncounter && (
          <Space>
            {selectedOrders.length > 0 && (
              <Button icon={<PrinterOutlined />} onClick={() => setPrintModalOpen(true)}>
                In phiếu chỉ định
              </Button>
            )}
            {currentRecordId && !prescriptionBlockReason && (
              <Button
                icon={<MedicineBoxOutlined />}
                onClick={() => openPrescription(currentRecordId)}
              >
                Chuyển sang kê đơn
              </Button>
            )}
            <Button type="primary" size="large" loading={saving} icon={<CheckCircleOutlined />} onClick={saveRecord}>
              {currentRecordId ? 'Cập nhật bệnh án' : 'Lưu bệnh án'}
            </Button>
          </Space>
        )}
      </div>

      <Card style={{ marginBottom: 16 }}>
        <Descriptions column={{ xs: 1, sm: 2, lg: 4 }} size="small" bordered>
          <Descriptions.Item label="Bệnh nhân">
            <Text strong>{encounter?.patient?.fullName}</Text> ({encounter?.patient?.patientCode})
          </Descriptions.Item>
          <Descriptions.Item label="Lượt khám">
            <Text code>{encounter?.visit?.visitCode || encounter?.visit?.id}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Hàng đợi / STT">
            {encounter?.queueItem?.id} / {encounter?.queueItem?.queueNumber}
          </Descriptions.Item>
          <Descriptions.Item label="Phòng">{encounter?.room?.roomNumber || '—'}</Descriptions.Item>
          <Descriptions.Item label="Bác sĩ">{encounter?.doctor?.fullName || '—'}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái hàng đợi">
            <Tag color="processing">{
              {
                WAITING: 'Chờ khám',
                IN_PROGRESS: 'Đang khám',
                WAITING_FOR_RESULT: 'Chờ kết quả CĐLS',
                COMPLETED: 'Đã hoàn tất',
                SKIPPED: 'Đã bỏ qua'
              }[encounter?.queueItem?.status] || encounter?.queueItem?.status || '—'
            }</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Mã bệnh án">
            {currentRecordId ? <Text code>{currentRecordId}</Text> : <Tag>Chưa tạo</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái bệnh án">
            <Tag color={encounter?.medicalRecord?.status === 'LOCKED' ? 'green' : 'blue'}>
              {encounter?.medicalRecord?.status === 'LOCKED' ? 'Đã khóa' : 'Đang mở (Bản nháp)'}
            </Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {prescriptionBlockReason && (
        <Alert
          showIcon
          type={encounter?.queueItem?.status === 'WAITING_FOR_RESULT' ? 'warning' : 'error'}
          message="Chưa thể chuyển sang kê đơn"
          description={prescriptionBlockReason}
          action={<Button onClick={loadWorkflow}>Tải lại trạng thái</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      <Alert
        showIcon
        type="info"
        message="Quy trình theo lượt khám"
        description="1. Xác nhận thông tin lượt khám → 2. Khám và chọn chẩn đoán ICD-10 → 3. Chỉ định cận lâm sàng (nếu cần) → 4. Chờ kết quả CĐLS → 5. Chuyển sang kê đơn thuốc."
        style={{ marginBottom: 16 }}
      />

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        items={[
          {
            key: 'current',
            label: <span><SolutionOutlined /> Khám & chẩn đoán</span>,
            children: (
              <MedicalEncounterForm
                form={form}
                isDoctor={canEditEncounter}
                encounterContext={encounter}
                selectedPatientObj={selectedPatientObj}
                vitalSigns={vitalSigns}
                setVitalSigns={setVitalSigns}
                bmiValue={bmiValue}
                diagnosisType={diagnosisType}
                setDiagnosisType={setDiagnosisType}
                primaryIcd={primaryIcd}
                clearPrimaryDiagnosis={clearPrimaryDiagnosis}
                selectPrimaryDiagnosis={selectPrimaryDiagnosis}
                secondaryIcds={secondaryIcds}
                setSecondaryIcds={setSecondaryIcds}
                addSecondaryDiagnosis={addSecondaryDiagnosis}
                diagnosisOptions={diagnosisSelectOptions}
                diagnosisSearching={icdSearching}
                onDiagnosisSearch={setIcdSearchQuery}
                setDiagnosisModalOpen={setDiagnosisModalVisibility}
                selectedOrders={selectedOrders}
                orderCategory={orderCategory}
                setOrderCategory={setOrderCategory}
                orderSearchQuery={orderSearchQuery}
                setOrderSearchQuery={setOrderSearchQuery}
                filteredCatalog={filteredCatalog}
                handleAddOrder={handleAddOrder}
                handleRemoveOrder={handleRemoveOrder}
                handleToggleUrgent={handleToggleUrgent}
                handleUpdateOrderNote={handleUpdateOrderNote}
                totalOrderFee={totalOrderFee}
                setPrintModalOpen={setPrintModalOpen}
                serviceCatalogError={serviceCatalogError}
              />
            ),
          },
          {
            key: 'history',
            label: `Lịch sử bệnh án (${records.length})`,
            children: records.length ? (
              <Table
                rowKey="medicalRecordId"
                columns={historyColumns}
                dataSource={records}
                pagination={{ pageSize: 10 }}
              />
            ) : <Empty description="Bệnh nhân chưa có lịch sử bệnh án" />,
          },
        ]}
      />

      <Modal
        title="Tra cứu danh mục ICD-10 backend"
        open={diagnosisModalOpen}
        onCancel={() => setDiagnosisModalOpen(false)}
        footer={<Button onClick={() => setDiagnosisModalOpen(false)}>Đóng</Button>}
        width={780}
      >
        <Space style={{ width: '100%', marginBottom: 12 }} align="start">
          <Input
            prefix={<SearchOutlined />}
            placeholder="Nhập mã hoặc tên bệnh để lấy UUID danh mục..."
            value={icdSearchQuery}
            onChange={(event) => setIcdSearchQuery(event.target.value)}
            style={{ width: 480 }}
          />
          <Select
            value={icdCategory}
            onChange={setIcdCategory}
            style={{ width: 240 }}
            options={icd10Categories.map((item) => ({ value: item.key, label: item.label }))}
          />
        </Space>
        <Table
          size="small"
          rowKey={(item) => item.id || item.code}
          dataSource={filteredIcdList}
          loading={icdSearching}
          pagination={false}
          columns={[
            { title: 'Mã', dataIndex: 'code', width: 100, render: (value) => <Tag color="blue">{value}</Tag> },
            { title: 'Tên chẩn đoán', dataIndex: 'name' },
            {
              title: 'Nguồn',
              width: 100,
              render: (_, item) => <Tag color={item.id ? 'green' : 'default'}>{item.id ? 'Backend' : 'Gợi ý'}</Tag>,
            },
            {
              title: '',
              width: 180,
              render: (_, item) => (
                <Space>
                  <Button
                    size="small"
                    type="primary"
                    onClick={async () => {
                      await selectPrimaryDiagnosis(item)
                      setDiagnosisModalOpen(false)
                    }}
                  >
                    Chọn chính
                  </Button>
                  <Button
                    size="small"
                    onClick={() => {
                      if (!secondaryIcds.some((diagnosis) => diagnosis.code === item.code)) {
                        setSecondaryIcds((items) => [...items, item])
                      }
                    }}
                  >
                    Thêm phụ
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Modal>

      <Modal
        title="Chi tiết bệnh án"
        open={Boolean(viewing)}
        onCancel={() => setViewing(null)}
        footer={<Button onClick={() => setViewing(null)}>Đóng</Button>}
        width={760}
      >
        {viewing && (
          <>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="Medical record ID">{viewing.medicalRecordId}</Descriptions.Item>
              <Descriptions.Item label="Visit">{viewing.visitCode || viewing.visitId}</Descriptions.Item>
              <Descriptions.Item label="Bệnh nhân">{viewing.patientName}</Descriptions.Item>
              <Descriptions.Item label="Bác sĩ">{viewing.doctorName || '—'}</Descriptions.Item>
              <Descriptions.Item label="Triệu chứng">{viewing.symptoms || '—'}</Descriptions.Item>
              <Descriptions.Item label="Khám lâm sàng">{viewing.physicalExamination || '—'}</Descriptions.Item>
              <Descriptions.Item label="Chẩn đoán">{viewing.diagnosis || '—'}</Descriptions.Item>
              <Descriptions.Item label="Hướng điều trị">{viewing.treatmentPlan || '—'}</Descriptions.Item>
            </Descriptions>
            <Divider />
            <Text type="secondary">Tệp kết quả cận lâm sàng được quản lý tại màn Kết quả CLS, không gắn trực tiếp vào medical record.</Text>
          </>
        )}
      </Modal>

      <ClinicalOrderPrintModal
        open={printModalOpen}
        onClose={() => setPrintModalOpen(false)}
        patient={selectedPatientObj}
        recordCode={currentRecordId || `VISIT-${visitId}`}
        diagnosis={form.getFieldValue('diagnosisText')}
        primaryIcd={primaryIcd}
        secondaryIcds={secondaryIcds}
        orders={selectedOrders}
        doctorName={encounter?.doctor?.fullName || user?.fullName || user?.username}
        vitalSigns={vitalSigns}
      />
    </div>
  )
}

export default MedicalEncounter
