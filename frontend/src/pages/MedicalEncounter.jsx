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
  CheckCircleFilled,
  CheckOutlined,
  EyeOutlined,
  MedicineBoxOutlined,
  PrinterOutlined,
  SearchOutlined,
  SolutionOutlined,
  SafetyCertificateOutlined,
  SafetyCertificateFilled,
  LockOutlined,
  EditOutlined,
  HistoryOutlined,
  FileProtectOutlined,
} from '@ant-design/icons'

import clinicalServiceApi from '../api/clinicalServiceApi'
import medicalRecordApi from '../api/medicalRecordApi'
import queueApi from '../api/queueApi'
import systemApi from '../api/systemApi'
import visitApi from '../api/visitApi'
import MedicalEncounterForm from '../components/clinical/MedicalEncounterForm'
import SignMedicalRecordModal from '../components/clinical/SignMedicalRecordModal'
import AmendMedicalRecordModal from '../components/clinical/AmendMedicalRecordModal'
import MedicalRecordVersionHistoryModal from '../components/clinical/MedicalRecordVersionHistoryModal'
import MedicalRecordSignatureStamp from '../components/clinical/MedicalRecordSignatureStamp'
import { isMedicalRecordSigned } from '../utils/medicalRecordSignHelpers'
import { useAuthContext } from '../context/AuthContext'
import { clinicalServiceCatalog } from '../utils/clinicalCatalogData'
import { getCategoryFromIcdCode, icd10Categories } from '../utils/icd10Data'
import { fixMojibake } from '../utils/serviceCatalogValidation'
import { getApiErrorMessage as getApiMessage, normalizeApiError } from '../utils/apiError'
import { formatRecordStatus } from '../utils/helpers'
import {
  buildClinicalOrderPayload,
  buildDiagnosisPayload,
  buildMedicalRecordPayload,
  getQueueInProgressBlockReason,
  normalizeMedicalRecordDetail,
  unwrapCollection,
} from '../utils/workflowContract'

const ClinicalOrderPrintModal = React.lazy(() => import('../components/clinical/ClinicalOrderPrintModal'))

const { Text, Paragraph, Title } = Typography

const loadRecentDiagnoses = () => {
  try {
    const saved = localStorage.getItem('recent_diagnoses')
    if (saved) {
      const parsed = JSON.parse(saved)
      if (Array.isArray(parsed)) {
        const valid = parsed.filter((item) => item?.id && item?.code)
        if (valid.length !== parsed.length) {
          localStorage.setItem('recent_diagnoses', JSON.stringify(valid))
        }
        return valid
      }
    }
  } catch {
  }
  return []
}

const saveRecentDiagnosis = (icd) => {
  if (!icd?.code) return
  try {
    const current = loadRecentDiagnoses()
    const updated = [icd, ...current.filter((item) => item.code !== icd.code)].slice(0, 10)
    localStorage.setItem('recent_diagnoses', JSON.stringify(updated))
  } catch {
  }
}

const mapClinicalService = (item) => ({
  id: item.id || item.serviceCode || item.code,
  code: item.serviceCode || item.code || '',
  name: fixMojibake(item.serviceName || item.name || ''),
  category:
    item.category ||
    (item.serviceType === 'LAB_TEST'
      ? 'XET_NGHIEM'
      : item.serviceType === 'IMAGING'
        ? 'CDHA'
        : 'THU_THUAT'),
  department: item.department || item.description || 'Dịch vụ cận lâm sàng',
  price: Number(item.price ?? item.currentPrice ?? 0),
  preparation: item.preparation || '',
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
  const [medicalRecord, setMedicalRecord] = useState(null)
  const [signModalOpen, setSignModalOpen] = useState(false)
  const [amendModalOpen, setAmendModalOpen] = useState(false)
  const [versionHistoryModalOpen, setVersionHistoryModalOpen] = useState(false)
  const [versionHistory, setVersionHistory] = useState(null)
  const [records, setRecords] = useState([])
  const [clinicalServices, setClinicalServices] = useState([])
  const [serviceCatalogError, setServiceCatalogError] = useState('')
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('current')
  const [viewing, setViewing] = useState(null)

  const isRecordSigned = useMemo(() => {
    const status = medicalRecord?.status || encounter?.medicalRecord?.status
    return isMedicalRecordSigned(status)
  }, [medicalRecord?.status, encounter?.medicalRecord?.status])

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
  const [allBackendDiagnoses, setAllBackendDiagnoses] = useState([])
  const [icdSearching, setIcdSearching] = useState(false)
  const [, setRecentIcds] = useState(loadRecentDiagnoses)

  const loadAllBackendDiagnoses = useCallback(async () => {
    setIcdSearching(true)
    try {
      const keyPrefixes = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'R', 'S', 'T', 'Z']
      const results = await Promise.allSettled(
        keyPrefixes.map((prefix) => medicalRecordApi.getDiagnosisCatalog(prefix)),
      )
      const map = new Map()
      results.forEach((res) => {
        if (res.status === 'fulfilled' && Array.isArray(res.value?.data)) {
          res.value.data.forEach((item) => {
            if (item?.code && !map.has(item.code)) {
              map.set(item.code, {
                id: item.id,
                code: item.code,
                rawName: item.name,
                name: fixMojibake(item.name),
                description: fixMojibake(item.description || ''),
                category: getCategoryFromIcdCode(item.code),
              })
            }
          })
        }
      })
      const list = Array.from(map.values()).sort((a, b) => a.code.localeCompare(b.code))
      setAllBackendDiagnoses(list)
    } catch {
      setAllBackendDiagnoses([])
    } finally {
      setIcdSearching(false)
    }
  }, [])

  useEffect(() => {
    loadAllBackendDiagnoses()
  }, [loadAllBackendDiagnoses])

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
      setMedicalRecord(null)
      setPrimaryIcd(null)
      setSecondaryIcds([])
      return
    }

    setMedicalRecord(detail)
    const cleanPrimaryName = fixMojibake(detail.primaryIcdName)
    const cleanConclusion = fixMojibake(detail.conclusion)

    setCurrentRecordId(detail.medicalRecordId)
    form.setFieldsValue({
      patientId: detail.patient?.id,
      symptoms: fixMojibake(detail.symptoms || detail.chiefComplaint || ''),
      medicalHistory: fixMojibake(detail.medicalHistory || ''),
      examinationNote: fixMojibake(detail.physicalExamination || ''),
      treatmentPlan: fixMojibake(detail.treatmentPlan || detail.doctorInstructions || ''),
      diagnosisText:
        detail.primaryIcdCode && cleanPrimaryName
          ? `[${detail.primaryIcdCode}] ${cleanPrimaryName}`
          : cleanConclusion || '',
    })

    const diagnoses = Array.isArray(detail.diagnoses) ? detail.diagnoses : []
    const primary = diagnoses.find((item) => item.diagnosisType === 'PRIMARY')
    setPrimaryIcd(
      primary
        ? {
          code: primary.diagnosisCode,
          name: fixMojibake(primary.diagnosisName),
          note: fixMojibake(primary.note),
        }
        : detail.primaryIcdCode
          ? { code: detail.primaryIcdCode, name: cleanPrimaryName }
          : null,
    )
    setSecondaryIcds(
      diagnoses
        .filter((item) => item.diagnosisType === 'SECONDARY')
        .map((item) => ({
          code: item.diagnosisCode,
          name: fixMojibake(item.diagnosisName),
          note: fixMojibake(item.note),
        })),
    )
  }, [form])

  const handleOpenSignFlow = useCallback(async () => {
    if (!canEditEncounter) {
      message.error('Bạn không có quyền ký bệnh án.')
      return
    }
    if (isRecordSigned) {
      setSignModalOpen(true)
      return
    }
    if (!currentRecordId) {
      Modal.confirm({
        title: 'Lưu bệnh án trước khi ký',
        content: 'Bệnh án cần được lưu vào hệ thống trước khi tiến hành ký xác nhận. Bạn có muốn lưu bệnh án ngay bây giờ không?',
        okText: 'Lưu & Tiếp tục ký',
        cancelText: 'Hủy',
        onOk: async () => {
          const savedId = await saveRecord({ showModal: false })
          if (savedId) {
            setSignModalOpen(true)
          }
        },
      })
      return
    }
    setSignModalOpen(true)
  }, [canEditEncounter, isRecordSigned, currentRecordId])

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
        symptoms: fixMojibake(encounterData.visit?.reason || ''),
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
        const recId = recordResult.value.data?.medicalRecordId || recordResult.value.data?.id
        if (recId) {
          medicalRecordApi
            .getVersionHistory(recId)
            .then((vRes) => setVersionHistory(vRes.data))
            .catch(() => setVersionHistory(null))
        }
      } else if ((recordResult.reason?.apiError || normalizeApiError(recordResult.reason)).status === 404) {
        hydrateRecord(null)
        setVersionHistory(null)
      } else {
        throw recordResult.reason
      }

      const history =
        historyResult.status === 'fulfilled' && Array.isArray(historyResult.value.data)
          ? historyResult.value.data.map(normalizeMedicalRecordDetail).filter(Boolean)
          : []
      setRecords(history)

      if (
        serviceResult.status === 'fulfilled' &&
        (Array.isArray(serviceResult.value?.data?.content) || Array.isArray(serviceResult.value?.data)) &&
        (serviceResult.value?.data?.content?.length > 0 || serviceResult.value?.data?.length > 0)
      ) {
        setClinicalServices(unwrapCollection(serviceResult.value.data).map(mapClinicalService))
        setServiceCatalogError('')
      } else {
        let fallbackServices = []
        try {
          const sysRes = await systemApi.services({ active: true, size: 100 })
          const items = unwrapCollection(sysRes.data)
          if (items.length > 0) {
            fallbackServices = items.map(mapClinicalService)
          }
        } catch {
        }
        if (fallbackServices.length === 0) {
          fallbackServices = clinicalServiceCatalog.map(mapClinicalService)
        }
        setClinicalServices(fallbackServices)
        setServiceCatalogError('')
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
        const raw = Array.isArray(response.data) ? response.data : []
        setBackendIcdCatalog(
          raw.map((item) => ({
            id: item.id,
            code: item.code,
            rawName: item.name,
            name: fixMojibake(item.name),
            description: fixMojibake(item.description || ''),
            category: getCategoryFromIcdCode(item.code),
          })),
        )
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
    const query = icdSearchQuery.trim().toLowerCase()
    let list = []
    if (query) {
      if (backendIcdCatalog.length > 0) {
        list = backendIcdCatalog
      } else {
        list = allBackendDiagnoses.filter(
          (item) =>
            item.code.toLowerCase().includes(query) ||
            (item.name && item.name.toLowerCase().includes(query)),
        )
      }
    } else {
      list = allBackendDiagnoses
    }

    if (icdCategory !== 'ALL') {
      list = list.filter((item) => item.category === icdCategory)
    }
    return list
  }, [allBackendDiagnoses, backendIcdCatalog, icdCategory, icdSearchQuery])

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

  const selectPrimaryDiagnosis = useCallback(
    (icd) => {
      if (!icd?.code) return
      const backendItem = allBackendDiagnoses.find(
        (item) => String(item.code).toUpperCase() === String(icd.code).toUpperCase() || (icd.id && String(item.id) === String(icd.id)),
      )
      const cleanIcd = {
        id: icd.id || backendItem?.id,
        code: backendItem?.code || icd.code,
        name: fixMojibake(backendItem?.name || icd.name),
        note: icd.note,
      }
      setPrimaryIcd(cleanIcd)
      form.setFieldsValue({
        diagnosisText: `[${cleanIcd.code}] ${cleanIcd.name}`,
      })
      if (cleanIcd.id) {
        saveRecentDiagnosis(cleanIcd)
        setRecentIcds(loadRecentDiagnoses())
      }
    },
    [allBackendDiagnoses, form],
  )

  const clearPrimaryDiagnosis = useCallback(() => {
    setPrimaryIcd(null)
    form.setFieldsValue({
      diagnosisText: '',
    })
  }, [form])

  const addSecondaryDiagnosis = useCallback(
    (icd) => {
      if (!icd?.code) return
      if (primaryIcd?.code === icd.code) {
        message.warning('Mã này đã được chọn làm chẩn đoán chính.')
        return
      }
      const backendItem = allBackendDiagnoses.find(
        (item) => String(item.code).toUpperCase() === String(icd.code).toUpperCase() || (icd.id && String(item.id) === String(icd.id)),
      )
      const cleanIcd = {
        id: icd.id || backendItem?.id,
        code: backendItem?.code || icd.code,
        name: fixMojibake(backendItem?.name || icd.name),
        note: icd.note,
      }
      setSecondaryIcds((prev) => {
        if (prev.some((item) => item.code === cleanIcd.code)) {
          message.info('Mã chẩn đoán phụ này đã có trong danh sách.')
          return prev
        }
        return [...prev, cleanIcd]
      })
      if (cleanIcd.id) {
        saveRecentDiagnosis(cleanIcd)
        setRecentIcds(loadRecentDiagnoses())
      }
    },
    [allBackendDiagnoses, primaryIcd?.code],
  )

  const diagnosisSelectOptions = useMemo(() => {
    if (icdSearchQuery.trim()) {
      return filteredIcdList
    }
    return allBackendDiagnoses
  }, [allBackendDiagnoses, filteredIcdList, icdSearchQuery])

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
    if (diagnosis?.id && diagnosis?.rawName) return diagnosis
    if (!diagnosis?.code) throw new Error('Vui lòng chọn chẩn đoán ICD-10 từ danh mục chuẩn.')

    const foundInState = allBackendDiagnoses.find(
      (item) => String(item.code).toUpperCase() === String(diagnosis.code).toUpperCase() || (diagnosis.id && String(item.id) === String(diagnosis.id)),
    )
    if (foundInState?.id) {
      return {
        id: foundInState.id,
        code: foundInState.code,
        rawName: foundInState.rawName,
        name: foundInState.name,
        note: diagnosis.note,
      }
    }

    const response = await medicalRecordApi.getDiagnosisCatalog(diagnosis.code)
    const list = Array.isArray(response.data) ? response.data : []
    const exact =
      list.find((item) => String(item.code).toUpperCase() === String(diagnosis.code).toUpperCase()) ||
      list.find((item) => String(item.code).toUpperCase().startsWith(String(diagnosis.code).toUpperCase())) ||
      list[0]

    if (!exact?.id) {
      throw new Error(`Mã ${diagnosis.code} chưa tồn tại trong danh mục chẩn đoán chuẩn.`)
    }
    return {
      id: exact.id,
      code: exact.code,
      rawName: exact.name,
      name: fixMojibake(exact.name),
      note: diagnosis.note,
    }
  }

  const openPrescription = async (medicalRecordId) => {
    if (!medicalRecordId) {
      message.error('Chưa có mã bệnh án hợp lệ để chuyển sang kê đơn. Vui lòng bấm "Cập nhật bệnh án" trước.')
      return false
    }
    if (!primaryIcd?.code) {
      message.warning('Bệnh án cần có chẩn đoán chính trước khi chuyển sang kê đơn thuốc. Vui lòng chọn chẩn đoán và bấm "Cập nhật bệnh án".')
      return false
    }

    const queueItemId = encounter?.queueItem?.id
    if (!queueItemId) {
      message.error('Chưa có thông tin hàng đợi hợp lệ để chuyển sang kê đơn.')
      return false
    }

    try {
      const response = await queueApi.getById(queueItemId)
      const liveQueueItem = response?.data
      if (!liveQueueItem?.id || String(liveQueueItem.id) !== String(queueItemId)) {
        throw new Error('Hệ thống không tìm thấy thông tin lượt khám trong hàng đợi.')
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

  const saveRecord = async (options = { showModal: true }) => {
    if (!visitId || !encounter) {
      message.error('Không có visitId hợp lệ để lưu bệnh án.')
      return null
    }
    if (!encounter.queueItem?.id) {
      message.error('Không có thông tin lượt khám trong hàng đợi để lưu bệnh án.')
      return null
    }

    let values
    try {
      values = await form.validateFields()
    } catch {
      message.error('Vui lòng nhập triệu chứng và thông tin khám bắt buộc.')
      return null
    }

    if (!primaryIcd) {
      message.error('Vui lòng chọn chẩn đoán chính từ danh mục ICD-10.')
      return null
    }

    if (selectedOrders.some((item) => !item.id)) {
      message.error('Phiếu chỉ định chứa dịch vụ chưa hợp lệ trong hệ thống.')
      return null
    }

    if (selectedOrders.length > 0) {
      const orderBlockReason = getQueueInProgressBlockReason(
        encounter.queueItem,
        'tạo chỉ định cận lâm sàng mới',
      )
      if (orderBlockReason) {
        message.error(orderBlockReason)
        return null
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
      if (!persistedRecordId) throw new Error('Hệ thống chưa tạo được mã bệnh án sau khi lưu.')
      setCurrentRecordId(persistedRecordId)
      setMedicalRecord((prev) => ({ ...prev, ...recordResponse.data, medicalRecordId: persistedRecordId }))

      await medicalRecordApi.recordDiagnosis(
        persistedRecordId,
        buildDiagnosisPayload({
          primaryDiagnosis: resolvedPrimary,
          secondaryDiagnoses: resolvedSecondary,
          note: values.examinationNote || values.symptoms,
        }),
      )

      let liveQueueItem = encounter.queueItem
      if (selectedOrders.length > 0) {
        const liveQueueResponse = await queueApi.getById(encounter.queueItem.id)
        const queueBeforeOrder = liveQueueResponse?.data
        if (
          !queueBeforeOrder?.id ||
          String(queueBeforeOrder.id) !== String(encounter.queueItem.id)
        ) {
          throw new Error('Hệ thống không tìm thấy thông tin lượt khám trước khi tạo chỉ định.')
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
        liveQueueItem = queueResponse?.data || liveQueueItem
        setSelectedOrders([])
      } else {
        try {
          const queueResponse = await queueApi.getById(encounter.queueItem.id)
          if (queueResponse?.data) {
            liveQueueItem = queueResponse.data
          }
        } catch {
          // Sử dụng thông tin hàng đợi hiện tại trong encounter
        }
      }

      const continuationBlockReason = getQueueInProgressBlockReason(
        liveQueueItem,
        'chuyển sang kê đơn',
      )

      message.success('Đã lưu bệnh án, chẩn đoán và chỉ định thành công.')
      try {
        await loadWorkflow()
      } catch (loadErr) {
        console.warn('Lỗi làm mới dữ liệu sau khi lưu:', loadErr)
      }

      if (liveQueueItem?.status === 'WAITING_FOR_RESULT') {
        if (options?.showModal !== false) {
          Modal.confirm({
            title: 'Lượt khám đang chờ kết quả cận lâm sàng',
            content: continuationBlockReason,
            okText: 'Về hàng đợi',
            cancelText: 'Ở lại bệnh án',
            onOk: () => navigate('/appointments'),
          })
        }
      } else if (options?.showModal !== false) {
        showSuccessModal(persistedRecordId)
      }
      return persistedRecordId
    } catch (error) {
      console.error('Lỗi khi lưu bệnh án:', error)
      const prefix = persistedRecordId
        ? `Bệnh án ${persistedRecordId} đã được lưu nhưng có cảnh báo: `
        : ''
      message.error(prefix + getApiMessage(error, 'Không thể lưu bệnh án.'))
      return null
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
      render: (value) => {
        const formatted = formatRecordStatus(value)
        return <Tag color={formatted.color}>{formatted.label}</Tag>
      },
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
          <Space wrap>
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
            {!isRecordSigned ? (
              <>
                <Button
                  type="primary"
                  size="large"
                  loading={saving}
                  icon={<CheckCircleOutlined />}
                  onClick={() => saveRecord()}
                >
                  {currentRecordId ? 'Cập nhật bệnh án' : 'Lưu bệnh án'}
                </Button>
                <Button
                  type="primary"
                  size="large"
                  icon={<SafetyCertificateOutlined />}
                  onClick={handleOpenSignFlow}
                  style={{
                    background: 'linear-gradient(135deg, #16a34a 0%, #15803d 100%)',
                    borderColor: '#16a34a',
                    fontWeight: 600,
                  }}
                >
                  Ký bệnh án
                </Button>
              </>
            ) : (
              <Space wrap>
                <Tag
                  color="success"
                  style={{
                    fontSize: 13,
                    padding: '6px 12px',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    fontWeight: 700,
                  }}
                >
                  <SafetyCertificateFilled /> BỆNH ÁN ĐÃ KÝ & KHÓA
                </Tag>
                <Button
                  icon={<SafetyCertificateOutlined />}
                  onClick={() => setSignModalOpen(true)}
                >
                  Xem chứng thư ký số
                </Button>
                <Button
                  type="primary"
                  icon={<EditOutlined />}
                  onClick={() => setAmendModalOpen(true)}
                  style={{
                    background: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)',
                    borderColor: '#d97706',
                    fontWeight: 600,
                  }}
                >
                  Lập bản đính chính
                </Button>
                <Button
                  icon={<HistoryOutlined />}
                  onClick={() => setVersionHistoryModalOpen(true)}
                >
                  Lịch sử phiên bản {versionHistory?.amendmentVersions?.length ? `(${versionHistory.amendmentVersions.length + 1})` : ''}
                </Button>
              </Space>
            )}
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
            {(() => {
              const currentStatus = medicalRecord?.status || encounter?.medicalRecord?.status
              const formatted = formatRecordStatus(currentStatus)
              return (
                <Space size={4}>
                  <Tag color={formatted.color}>{formatted.label}</Tag>
                  {isRecordSigned && <SafetyCertificateFilled style={{ color: '#16a34a' }} />}
                  {versionHistory?.amendmentVersions?.length > 0 && (
                    <Tag color="orange" style={{ fontWeight: 600 }}>
                      +{versionHistory.amendmentVersions.length} đính chính
                    </Tag>
                  )}
                </Space>
              )
            })()}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {isRecordSigned && (
        <Alert
          type="warning"
          showIcon
          icon={<FileProtectOutlined style={{ fontSize: 20, color: '#d97706' }} />}
          message={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
              <span style={{ fontWeight: 700, fontSize: 14 }}>
                Hồ sơ bệnh án đã ký số & khóa nội dung gốc
              </span>
              {versionHistory?.amendmentVersions?.length > 0 && (
                <Tag color="orange" style={{ fontWeight: 600, fontSize: 12 }}>
                  ĐÃ CÓ {versionHistory.amendmentVersions.length} BẢN ĐÍNH CHÍNH
                </Tag>
              )}
            </div>
          }
          description={
            <div style={{ marginTop: 6, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ color: '#78350f', fontSize: 13, maxWidth: 650 }}>
                Nội dung khám ban đầu được lưu giữ nguyên vẹn để đảm bảo tính toàn vẹn và pháp lý.
                Nếu có phát hiện sai sót chuyên môn hoặc bổ sung phác đồ, bác sĩ hãy sử dụng chức năng <b>Lập bản đính chính</b> để tạo phiên bản gắn kèm.
              </div>
              <Space>
                <Button
                  type="primary"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={() => setAmendModalOpen(true)}
                  style={{ background: '#d97706', borderColor: '#d97706', fontWeight: 600 }}
                >
                  + Lập bản đính chính
                </Button>
                <Button
                  size="small"
                  icon={<HistoryOutlined />}
                  onClick={() => setVersionHistoryModalOpen(true)}
                >
                  Xem lịch sử phiên bản
                </Button>
              </Space>
            </div>
          }
          style={{
            marginBottom: 16,
            borderRadius: 8,
            background: '#fffbeb',
            borderColor: '#fde68a',
          }}
        />
      )}

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
        description="1. Xác nhận thông tin lượt khám → 2. Khám và chọn chẩn đoán ICD-10 → 3. Chỉ định cận lâm sàng (nếu cần) → 4. Ký xác nhận & khóa bệnh án → 5. Chuyển sang kê đơn thuốc."
        style={{ marginBottom: 16 }}
      />

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        items={[
          {
            key: 'current',
            label: (
              <span>
                <SolutionOutlined /> Khám & chẩn đoán {isRecordSigned && <Tag color="success" style={{ marginLeft: 4 }}>Đã ký</Tag>}
              </span>
            ),
            children: (
              <MedicalEncounterForm
                form={form}
                isDoctor={canEditEncounter}
                isSigned={isRecordSigned}
                medicalRecord={medicalRecord || encounter?.medicalRecord}
                onOpenSignModal={handleOpenSignFlow}
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
                setDiagnosisModalOpen={setDiagnosisModalOpen}
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
        title="Tra cứu danh mục ICD-10"
        open={diagnosisModalOpen}
        onCancel={() => setDiagnosisModalOpen(false)}
        footer={<Button onClick={() => setDiagnosisModalOpen(false)}>Đóng</Button>}
        width={780}
      >
        <Space style={{ width: '100%', marginBottom: 12 }} align="start">
          <Input
            prefix={<SearchOutlined />}
            placeholder="Nhập mã hoặc tên bệnh cần tra cứu..."
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
              width: 130,
              render: (_, item) => (
                <Tag color={item.id ? 'green' : 'default'}>
                  {item.id ? 'Danh mục chuẩn' : 'Gợi ý'}
                </Tag>
              ),
            },
            {
              title: 'Thao tác chọn',
              width: 220,
              align: 'center',
              render: (_, item) => {
                const isPrimary = primaryIcd?.code === item.code
                const isSecondary = secondaryIcds.some((diagnosis) => diagnosis.code === item.code)

                return (
                  <Space size={6} wrap>
                    {isPrimary ? (
                      <Tag color="success" style={{ fontWeight: 600 }}>
                        <CheckCircleOutlined /> Đang là CĐ chính
                      </Tag>
                    ) : (
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
                    )}
                    {isSecondary ? (
                      <Tag color="purple" style={{ fontWeight: 600 }}>
                        <CheckOutlined /> Đã thêm phụ
                      </Tag>
                    ) : isPrimary ? null : (
                      <Button
                        size="small"
                        disabled={!primaryIcd}
                        onClick={() => {
                          addSecondaryDiagnosis(item)
                        }}
                      >
                        + Thêm phụ
                      </Button>
                    )}
                  </Space>
                )
              },
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
            {isMedicalRecordSigned(viewing.status) && (
              <div style={{ marginBottom: 16 }}>
                <MedicalRecordSignatureStamp
                  signatureData={viewing.signatureData}
                  signedAt={viewing.signedAt}
                  signedBy={viewing.signedBy}
                  doctorName={viewing.doctorName || 'Bác sĩ phụ trách'}
                  status={viewing.status}
                />
              </div>
            )}
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="Medical record ID">{viewing.medicalRecordId}</Descriptions.Item>
              <Descriptions.Item label="Visit">{viewing.visitCode || viewing.visitId}</Descriptions.Item>
              <Descriptions.Item label="Bệnh nhân">{viewing.patientName}</Descriptions.Item>
              <Descriptions.Item label="Bác sĩ">{viewing.doctorName || '—'}</Descriptions.Item>
              <Descriptions.Item label="Triệu chứng">{viewing.symptoms || '—'}</Descriptions.Item>
              <Descriptions.Item label="Khám lâm sàng">{viewing.physicalExamination || '—'}</Descriptions.Item>
              <Descriptions.Item label="Chẩn đoán">{viewing.diagnosis || '—'}</Descriptions.Item>
              <Descriptions.Item label="Hướng điều trị">{viewing.treatmentPlan || '—'}</Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                {(() => {
                  const formatted = formatRecordStatus(viewing.status)
                  return <Tag color={formatted.color}>{formatted.label}</Tag>
                })()}
              </Descriptions.Item>
            </Descriptions>
            <Divider />
            <Text type="secondary">Tệp kết quả cận lâm sàng được quản lý tại màn Kết quả CLS, không gắn trực tiếp vào medical record.</Text>
          </>
        )}
      </Modal>

      {signModalOpen && (
        <SignMedicalRecordModal
          open={signModalOpen}
          onClose={() => setSignModalOpen(false)}
          onSuccess={async (signedData) => {
            setMedicalRecord((prev) => ({ ...prev, ...signedData, status: 'SIGNED' }))
            await loadWorkflow()
          }}
          recordId={currentRecordId}
          encounterContext={encounter}
          patient={selectedPatientObj}
          formValues={form.getFieldsValue()}
          vitalSigns={vitalSigns}
          bmiValue={bmiValue}
          primaryIcd={primaryIcd}
          secondaryIcds={secondaryIcds}
          selectedOrders={selectedOrders}
          currentUser={user}
        />
      )}

      {printModalOpen && (
        <React.Suspense fallback={<Spin size="small" />}>
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
        </React.Suspense>
      )}

      {amendModalOpen && (
        <AmendMedicalRecordModal
          open={amendModalOpen}
          onClose={() => setAmendModalOpen(false)}
          onSuccess={async () => {
            await loadWorkflow()
            setVersionHistoryModalOpen(true)
          }}
          recordId={currentRecordId}
          encounterContext={encounter}
          medicalRecord={medicalRecord || encounter?.medicalRecord}
          patient={selectedPatientObj}
          currentUser={user}
        />
      )}

      {versionHistoryModalOpen && (
        <MedicalRecordVersionHistoryModal
          open={versionHistoryModalOpen}
          onClose={() => setVersionHistoryModalOpen(false)}
          recordId={currentRecordId}
          canAmend={canEditEncounter && isRecordSigned}
          onOpenAmendModal={() => setAmendModalOpen(true)}
        />
      )}
    </div>
  )
}

export default MedicalEncounter
