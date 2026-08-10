import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Form,
  Input,
  List,
  message,
  Modal,
  Select,
  Space,
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
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
import clinicalServiceApi from '../api/clinicalServiceApi'
import patientApi from '../api/patientApi'
import queueApi from '../api/queueApi'
import { useAuthContext } from '../context/AuthContext'
import { logMedicalAccess, mergeMedicalRecords, saveStoredMedicalRecord, getStoredQueueItems, saveStoredQueueItem, saveStoredClinicalOrder, mergeQueues } from '../utils/storageHelpers'
import { saveStoredAttachment } from '../utils/attachmentHelpers'
import { getPopularIcd10, icd10Categories, searchIcd10 } from '../utils/icd10Data'
import { clinicalServiceCatalog } from '../utils/clinicalCatalogData'
import ClinicalOrderPrintModal from '../components/clinical/ClinicalOrderPrintModal'
import MedicalEncounterForm from '../components/clinical/MedicalEncounterForm'

const { Text, Paragraph } = Typography

const RECENT_DIAGNOSES_KEY = 'benhsoan_recent_diagnoses'
const MAX_DIAGNOSIS_OPTIONS = 10
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const isUuid = (value) => UUID_PATTERN.test(String(value || ''))
const MEDICAL_RECORD_STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  IN_PROGRESS: 'Đang khám',
  COMPLETED: 'Hoàn thành',
  LOCKED: 'Đã khóa',
  CANCELLED: 'Đã hủy',
}

const normalizeDiagnosis = (item) => {
  if (!item?.code) return null
  return {
    ...item,
    diagnosisCatalogId: item.diagnosisCatalogId || item.id || null,
  }
}

const mergeDiagnosisOptions = (...lists) => {
  const byCode = new Map()
  lists.flat().filter(Boolean).forEach((rawItem) => {
    const item = normalizeDiagnosis(rawItem)
    if (!item) return
    const existing = byCode.get(item.code)
    byCode.set(item.code, existing
      ? {
          ...item,
          ...existing,
          diagnosisCatalogId: existing.diagnosisCatalogId || item.diagnosisCatalogId,
        }
      : item)
  })
  return Array.from(byCode.values())
}

const loadRecentDiagnoses = () => {
  try {
    const stored = JSON.parse(localStorage.getItem(RECENT_DIAGNOSES_KEY) || '[]')
    return mergeDiagnosisOptions(stored, getPopularIcd10()).slice(0, MAX_DIAGNOSIS_OPTIONS)
  } catch {
    return getPopularIcd10().slice(0, MAX_DIAGNOSIS_OPTIONS)
  }
}

const normalizeClinicalService = (item) => {
  const code = item?.serviceCode || item?.code
  if (!code) return null
  const fallback = clinicalServiceCatalog.find((service) => service.code === code) || {}
  const type = String(item.serviceType || item.category || fallback.category || '').toUpperCase()
  const category = type.includes('LAB')
    ? 'XET_NGHIEM'
    : type.includes('IMAG') || type.includes('CDHA')
      ? 'CDHA'
      : type.includes('FUNC') || type.includes('THAM_DO')
        ? 'THAM_DO_CHUC_NANG'
        : fallback.category || 'THU_THUAT'
  const translatedName = code === 'LAB-GLU'
    ? 'Định lượng glucose máu'
    : code === 'IMG-CTH'
      ? 'Chụp CT sọ não không tiêm thuốc cản quang'
      : item.serviceName || item.name || fallback.name

  return {
    ...fallback,
    ...item,
    id: item.id || fallback.id,
    code,
    name: translatedName,
    category,
    price: item.price ?? fallback.price ?? null,
    department: item.department || fallback.department || (category === 'XET_NGHIEM' ? 'Phòng Xét nghiệm' : 'Phòng Cận lâm sàng'),
    preparation: item.description || fallback.preparation || '',
  }
}

function MedicalEncounter() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const normalizedUserRoles = (Array.isArray(user?.roles) ? user.roles : [user?.role])
    .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
  const isDoctor = normalizedUserRoles.some((role) => ['admin', 'doctor'].includes(role))
  const shouldUseMyQueue = normalizedUserRoles.includes('doctor') && !normalizedUserRoles.includes('admin')

  const [form] = Form.useForm()
  const [patients, setPatients] = useState([])
  const [records, setRecords] = useState([])
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('current')
  const [viewing, setViewing] = useState(null)

  // Selected Patient State
  const [selectedPatientId, setSelectedPatientId] = useState(null)
  const [currentVisitId, setCurrentVisitId] = useState(location.state?.visitId || null)

  // Vital Signs State
  const [vitalSigns, setVitalSigns] = useState({
    bp: '',
    pulse: '',
    temp: '37.0',
    respRate: '16',
    weight: '',
    height: '',
    spO2: '98',
  })

  // Diagnosis State
  const [diagnosisType, setDiagnosisType] = useState('DEFINITIVE') // PRELIMINARY, DEFINITIVE, DIFFERENTIAL
  const [primaryIcd, setPrimaryIcd] = useState(null)
  const [secondaryIcds, setSecondaryIcds] = useState([])
  const [diagnosisModalOpen, setDiagnosisModalOpen] = useState(false)
  const [icdSearchQuery, setIcdSearchQuery] = useState('')
  const [icdCategory, setIcdCategory] = useState('ALL')
  const [backendIcdCatalog, setBackendIcdCatalog] = useState([])
  const [icdSearching, setIcdSearching] = useState(false)
  const [recentIcds, setRecentIcds] = useState(loadRecentDiagnoses)

  // Clinical Orders State
  const [selectedOrders, setSelectedOrders] = useState([])
  const [orderCategory, setOrderCategory] = useState('ALL')
  const [orderSearchQuery, setOrderSearchQuery] = useState('')
  const [serviceCatalog, setServiceCatalog] = useState(clinicalServiceCatalog)

  // Results & Attachments State
  const [results, setResults] = useState({})
  const [files, setFiles] = useState([])

  // Print Order Modal State
  const [printModalOpen, setPrintModalOpen] = useState(false)

  // Pre-fill from navigation state
  useEffect(() => {
    if (location.state?.patientId) {
      setSelectedPatientId(location.state.patientId)
      form.setFieldsValue({ patientId: location.state.patientId })
    }
    if (location.state?.visitId) {
      setCurrentVisitId(location.state.visitId)
    }
  }, [location.state, form])

  const loadData = useCallback(async () => {
    try {
      const [patientResponse, recordResponse, queueResponse] = await Promise.allSettled([
        patientApi.getAll({ page: 0, size: 200 }),
        medicalRecordApi.getAll(),
        shouldUseMyQueue
          ? queueApi.getMyQueue({ date: dayjs().format('YYYY-MM-DD') })
          : queueApi.getQueues({ date: dayjs().format('YYYY-MM-DD') }),
      ])

      const allPatients = patientResponse.status === 'fulfilled'
        ? (patientResponse.value.data?.content || [])
        : []
      
      const apiQueues = queueResponse.status === 'fulfilled'
        ? (queueResponse.value.data || [])
        : []

      const doctorQueueIds = new Set(apiQueues.map((item) => String(item.id || item.queueItemId)))
      const todayQueues = shouldUseMyQueue && localStorage.getItem('token') !== 'demo-token'
        ? mergeQueues(apiQueues).filter((item) => doctorQueueIds.has(String(item.id || item.queueItemId)))
        : mergeQueues(apiQueues)
      const examiningQueues = todayQueues.filter((item) =>
        ['IN_PROGRESS', 'WAITING_FOR_RESULT'].includes(item.status),
      )

      // Lọc danh sách bệnh nhân đã check-in hàng đợi ngày hôm nay theo thời gian/STT
      const sortedTodayQueues = [...examiningQueues].sort((a, b) => {
        const numA = a.queueNumber !== undefined && a.queueNumber !== null ? a.queueNumber : 999999
        const numB = b.queueNumber !== undefined && b.queueNumber !== null ? b.queueNumber : 999999
        if (numA !== numB) return numA - numB
        return new Date(a.checkedInAt || 0) - new Date(b.checkedInAt || 0)
      })

      const todayPatientsList = []
      const addedIds = new Set()

      sortedTodayQueues.forEach((qItem) => {
        const pIdStr = String(qItem.patientId)
        if (!addedIds.has(pIdStr) && qItem.patientId) {
          addedIds.add(pIdStr)
          const pObj = allPatients.find((p) => String(p.id) === pIdStr) || {}
          const timeLabel = qItem.checkedInAt ? dayjs(qItem.checkedInAt).format('HH:mm DD/MM/YYYY') : 'Hôm nay'
          todayPatientsList.push({
            ...pObj,
            id: qItem.patientId,
            fullName: pObj.fullName || qItem.patientName || pObj.name || 'Bệnh nhân',
            patientCode: pObj.patientCode || qItem.patientCode || 'Chưa có mã bệnh nhân',
            phoneNumber: pObj.phoneNumber || qItem.phone || 'Không SĐT',
            gender: pObj.gender || qItem.gender || 'MALE',
            dateOfBirth: pObj.dateOfBirth || '1995-01-01',
            healthInsuranceCode: pObj.healthInsuranceCode || 'Không có',
            medicalHistory: pObj.medicalHistory || 'Chưa ghi nhận',
            checkInTimeStr: timeLabel,
            queueNumber: qItem.queueNumber,
            visitId: qItem.visitId,
            queueItemId: qItem.id || qItem.queueItemId,
            queueStatus: qItem.status,
          })
        }
      })

      // Đảm bảo giữ bệnh nhân chuyển từ màn hình khác sang nếu có
      if (location.state?.patientId && !addedIds.has(String(location.state.patientId))) {
        const targetP = allPatients.find((p) => String(p.id) === String(location.state.patientId))
        if (targetP) todayPatientsList.unshift(targetP)
      }

      setPatients(todayPatientsList)

      const apiRecords = recordResponse.status === 'fulfilled' ? recordResponse.value.data || [] : []
      setRecords(mergeMedicalRecords(apiRecords))
    } catch {
      setRecords(mergeMedicalRecords([]))
    }
  }, [location.state?.patientId, shouldUseMyQueue])

  useEffect(() => {
    loadData()
  }, [loadData])

  useEffect(() => {
    let active = true
    clinicalServiceApi.getCatalog({ page: 0, size: 200 })
      .then((response) => {
        if (!active) return
        const list = Array.isArray(response.data) ? response.data : (response.data?.content || [])
        const normalized = list.map(normalizeClinicalService).filter(Boolean)
        if (normalized.length) setServiceCatalog(normalized)
      })
      .catch(() => {
        if (active) setServiceCatalog(clinicalServiceCatalog)
      })
    return () => { active = false }
  }, [])

  // Fetch Backend Diagnosis ICD-10 Catalog API
  useEffect(() => {
    const query = icdSearchQuery.trim()
    if (query.length < 2) {
      setBackendIcdCatalog([])
      setIcdSearching(false)
      return undefined
    }

    let active = true
    const timeoutId = window.setTimeout(async () => {
      setIcdSearching(true)
      try {
        const response = await medicalRecordApi.getDiagnosisCatalog(query)
        if (active) {
          const list = Array.isArray(response.data) ? response.data : []
          setBackendIcdCatalog(list
            .filter((item) => item?.active !== false)
            .map(normalizeDiagnosis)
            .filter(Boolean))
        }
      } catch {
        if (active) setBackendIcdCatalog([])
      } finally {
        if (active) setIcdSearching(false)
      }
    }, 350)

    return () => {
      active = false
      window.clearTimeout(timeoutId)
    }
  }, [icdSearchQuery])

  const selectedPatientObj = useMemo(() => {
    return patients.find((p) => String(p.id) === String(selectedPatientId))
  }, [patients, selectedPatientId])

  const handlePatientSelection = useCallback((patientId) => {
    const patient = patients.find((item) => String(item.id) === String(patientId))
    setSelectedPatientId(patientId)
    setCurrentVisitId(patient?.visitId || null)
  }, [patients])

  // BMI calculation
  const bmiValue = useMemo(() => {
    const w = parseFloat(vitalSigns.weight)
    const h = parseFloat(vitalSigns.height) / 100
    if (w > 0 && h > 0) {
      return (w / (h * h)).toFixed(1)
    }
    return null
  }, [vitalSigns.weight, vitalSigns.height])

  // ICD-10 Search Results (combining Backend API & Local fallback)
  const filteredIcdList = useMemo(() => {
    const query = icdSearchQuery.trim()
    const localMatches = searchIcd10(icdSearchQuery, icdCategory)
    const baseList = query
      ? mergeDiagnosisOptions(backendIcdCatalog, localMatches)
      : recentIcds

    return baseList
      .filter((item) => icdCategory === 'ALL' || !item.category || item.category === icdCategory)
      .slice(0, MAX_DIAGNOSIS_OPTIONS)
  }, [icdSearchQuery, icdCategory, backendIcdCatalog, recentIcds])

  const diagnosisSelectOptions = useMemo(() => {
    const source = icdSearchQuery.trim() ? filteredIcdList : recentIcds
    return source
      .filter((item) => item.code !== primaryIcd?.code)
      .slice(0, MAX_DIAGNOSIS_OPTIONS)
  }, [filteredIcdList, icdSearchQuery, primaryIcd?.code, recentIcds])

  const rememberDiagnosis = useCallback((rawItem) => {
    const item = normalizeDiagnosis(rawItem)
    if (!item) return
    setRecentIcds((current) => {
      const next = mergeDiagnosisOptions([item], current).slice(0, MAX_DIAGNOSIS_OPTIONS)
      try {
        localStorage.setItem(RECENT_DIAGNOSES_KEY, JSON.stringify(next))
      } catch {
        // Không chặn thao tác khám nếu bộ nhớ trình duyệt không khả dụng.
      }
      return next
    })
  }, [])

  const resolveDiagnosisFromCatalog = useCallback(async (rawItem) => {
    const item = normalizeDiagnosis(rawItem)
    if (!item) return null
    try {
      const response = await medicalRecordApi.getDiagnosisCatalog(item.code)
      const exactMatch = (Array.isArray(response.data) ? response.data : [])
        .filter((candidate) => candidate?.active !== false)
        .map(normalizeDiagnosis)
        .find((candidate) => candidate?.code === item.code)
      return exactMatch ? { ...item, ...exactMatch } : item
    } catch {
      return item
    }
  }, [])

  const selectPrimaryDiagnosis = useCallback((rawItem) => {
    const item = normalizeDiagnosis(rawItem)
    if (!item) return
    setPrimaryIcd(item)
    setSecondaryIcds((current) => current.filter((diagnosis) => diagnosis.code !== item.code))
    form.setFieldsValue({ diagnosisText: `[${item.code}] ${item.name}` })
    rememberDiagnosis(item)

    resolveDiagnosisFromCatalog(item).then((resolved) => {
      if (!resolved?.diagnosisCatalogId) return
      setPrimaryIcd((current) => current?.code === resolved.code ? resolved : current)
      rememberDiagnosis(resolved)
    })
    return item
  }, [form, rememberDiagnosis, resolveDiagnosisFromCatalog])

  const addSecondaryDiagnosis = useCallback((rawItem) => {
    const item = normalizeDiagnosis(rawItem)
    if (!item) return false
    if (primaryIcd?.code === item.code) {
      message.warning('Chẩn đoán này đang được chọn làm chẩn đoán chính.')
      return false
    }
    if (secondaryIcds.some((diagnosis) => diagnosis.code === item.code)) {
      message.info('Chẩn đoán phụ này đã có trong danh sách.')
      return false
    }
    setSecondaryIcds((current) => {
      return [...current, item]
    })
    rememberDiagnosis(item)
    resolveDiagnosisFromCatalog(item).then((resolved) => {
      if (!resolved?.diagnosisCatalogId) return
      setSecondaryIcds((current) => current.map((diagnosis) =>
        diagnosis.code === resolved.code ? resolved : diagnosis
      ))
      rememberDiagnosis(resolved)
    })
    return true
  }, [primaryIcd?.code, rememberDiagnosis, resolveDiagnosisFromCatalog, secondaryIcds])

  const clearPrimaryDiagnosis = useCallback(() => {
    setPrimaryIcd(null)
    form.setFieldValue('diagnosisText', '')
  }, [form])

  const setDiagnosisModalVisibility = useCallback((open) => {
    setIcdSearchQuery('')
    setIcdCategory('ALL')
    setBackendIcdCatalog([])
    setDiagnosisModalOpen(open)
  }, [])

  // Clinical Orders Catalog Filtered
  const filteredCatalog = useMemo(() => {
    const q = orderSearchQuery.toLowerCase().trim()
    return serviceCatalog.filter((item) => {
      const matchesCat = orderCategory === 'ALL' || item.category === orderCategory
      const matchesQ = !q || item.name.toLowerCase().includes(q) || item.code.toLowerCase().includes(q)
      return matchesCat && matchesQ
    })
  }, [orderCategory, orderSearchQuery, serviceCatalog])

  // Order helper actions
  const handleAddOrder = (catalogItem) => {
    if (selectedOrders.some((item) => item.code === catalogItem.code)) {
      message.info(`Dịch vụ ${catalogItem.name} đã có trong danh sách chỉ định`)
      return
    }
    const newOrderItem = {
      ...catalogItem,
      isUrgent: false,
      note: '',
    }
    setSelectedOrders((prev) => [...prev, newOrderItem])
    message.success(`Đã thêm chỉ định: ${catalogItem.name}`)
  }

  const handleRemoveOrder = (code) => {
    setSelectedOrders((prev) => prev.filter((item) => item.code !== code))
    setResults((prev) => {
      const copy = { ...prev }
      delete copy[code]
      return copy
    })
  }

  const handleToggleUrgent = (code) => {
    setSelectedOrders((prev) =>
      prev.map((item) => (item.code === code ? { ...item, isUrgent: !item.isUrgent } : item))
    )
  }

  const handleUpdateOrderNote = (code, note) => {
    setSelectedOrders((prev) => prev.map((item) => (item.code === code ? { ...item, note } : item)))
  }

  const totalOrderFee = useMemo(() => {
    return selectedOrders.reduce((sum, item) => sum + (Number(item.price) || 0), 0)
  }, [selectedOrders])
  const persistedOrderFee = selectedOrders.length > 0 && selectedOrders.every((item) =>
    item.price !== null && item.price !== undefined && Number.isFinite(Number(item.price)),
  ) ? totalOrderFee : null

  // Đồng bộ trạng thái hàng đợi sau khi phiếu chỉ định đã được tạo thành công.
  const syncQueueCompletion = async (patientId, hasOrders) => {
    if (!hasOrders) return true

    try {
      const targetStatus = 'WAITING_FOR_RESULT'
      const targetQueueId = selectedPatientObj?.queueItemId || (
        String(location.state?.patientId) === String(patientId)
          ? location.state?.queueItemId
          : null
      )
      const isDemo = localStorage.getItem('token') === 'demo-token'

      if (!targetQueueId && !isDemo) return false
      if (targetQueueId && !isDemo && selectedPatientObj?.queueStatus !== targetStatus) {
        await queueApi.updateStatus(targetQueueId, { status: targetStatus })
      }

      // Chỉ cập nhật bộ nhớ cục bộ sau khi máy chủ xác nhận (hoặc trong chế độ minh họa).
      const allQueues = getStoredQueueItems()
      allQueues.forEach((q) => {
        if (
          (targetQueueId && String(q.id) === String(targetQueueId)) ||
          (isDemo && !targetQueueId && String(q.patientId) === String(patientId) && ['IN_PROGRESS', 'WAITING', 'WAITING_FOR_RESULT'].includes(q.status))
        ) {
          saveStoredQueueItem({ ...q, status: targetStatus })
        }
      })
      return true
    } catch (e) {
      console.warn('Không thể đồng bộ trạng thái hàng đợi:', e)
      return false
    }
  }

  // Form Submission & API Integration
  const saveRecord = async () => {
    let values
    try {
      values = await form.validateFields()
    } catch {
      message.error('Vui lòng nhập đầy đủ thông tin bắt buộc: Bệnh nhân, Triệu chứng và Chẩn đoán chính!')
      return
    }

    if (!primaryIcd && !values.diagnosisText) {
      message.error('Vui lòng chọn Mã bệnh ICD-10 hoặc nhập nội dung Chẩn đoán chính!')
      return
    }

    setSaving(true)
    const isDemo = localStorage.getItem('token') === 'demo-token'

    const canonicalPrimaryIcd = primaryIcd
      ? await resolveDiagnosisFromCatalog(primaryIcd)
      : null
    const canonicalSecondaryIcds = (await Promise.all(
      secondaryIcds.map((item) => resolveDiagnosisFromCatalog(item)),
    )).filter(Boolean)
    if (canonicalPrimaryIcd) {
      setPrimaryIcd(canonicalPrimaryIcd)
      rememberDiagnosis(canonicalPrimaryIcd)
    }
    setSecondaryIcds(canonicalSecondaryIcds)

    const unresolvedDiagnoses = [
      ...(canonicalPrimaryIcd && !isUuid(canonicalPrimaryIcd.diagnosisCatalogId) ? [canonicalPrimaryIcd] : []),
      ...canonicalSecondaryIcds.filter((item) => !isUuid(item.diagnosisCatalogId)),
    ]
    if (!isDemo && unresolvedDiagnoses.length > 0) {
      const codes = unresolvedDiagnoses.map((item) => item.code).filter(Boolean).join(', ')
      message.error(`Không thể lưu vì mã chẩn đoán ${codes || 'đã chọn'} không còn hoạt động trong danh mục. Vui lòng tìm và chọn lại.`)
      setSaving(false)
      return
    }
    if (!isDemo && canonicalSecondaryIcds.length > 0 && !isUuid(canonicalPrimaryIcd?.diagnosisCatalogId)) {
      message.error('Vui lòng chọn chẩn đoán chính từ danh mục trước khi thêm chẩn đoán phụ.')
      setSaving(false)
      return
    }

    // Format full diagnosis text
    const primaryDiagStr = canonicalPrimaryIcd ? `[${canonicalPrimaryIcd.code}] ${canonicalPrimaryIcd.name}` : values.diagnosisText
    const secondaryDiagStr = canonicalSecondaryIcds.map((item) => `[${item.code}] ${item.name}`).join('; ')
    const fullDiagnosisText = secondaryDiagStr ? `${primaryDiagStr} (Kèm theo: ${secondaryDiagStr})` : primaryDiagStr

    const orderNamesList = selectedOrders.map((o) => `${o.name} (${o.isUrgent ? 'CẤP CỨU' : 'Thường'})`)

    const recordCode = `BA-${dayjs().format('YYYYMMDDHHmmss')}`
    const completeRecord = {
      id: `mr-${Date.now()}`,
      recordCode,
      patientId: values.patientId,
      patientName: selectedPatientObj ? `${selectedPatientObj.fullName} (${selectedPatientObj.patientCode})` : 'Bệnh nhân',
      doctorName: user?.fullName || user?.username || 'BS. Phạm Hồng Anh',
      symptoms: values.symptoms,
      examinationNote: values.examinationNote || '',
      diagnosis: fullDiagnosisText,
      diagnosisType,
      primaryIcd: canonicalPrimaryIcd || { code: 'ICD-10', name: values.diagnosisText || 'Chẩn đoán xác định' },
      secondaryIcds: canonicalSecondaryIcds,
      vitalSigns,
      treatmentPlan: values.treatmentPlan || '',
      clinicalOrders: orderNamesList,
      clinicalOrderItems: selectedOrders,
      clinicalResults: Object.fromEntries(Object.entries(results).filter(([, value]) => value?.trim())),
      totalFee: persistedOrderFee,
      status: 'IN_PROGRESS',
      createdAt: dayjs().toISOString(),
      attachments: files.map((file) => ({ id: file.uid || String(Date.now()), fileName: file.name })),
    }

    try {
      // 1. Call Backend POST /medical-records API if available
      let beRecordId = null
      const navigationVisitId = String(location.state?.patientId) === String(values.patientId)
        ? location.state?.visitId
        : null
      const validVisitId = selectedPatientObj?.visitId || currentVisitId || navigationVisitId

      const beCreatePayload = {
        visitId: validVisitId,
        chiefComplaint: values.symptoms || 'Khám bệnh',
        symptoms: values.symptoms || '',
        medicalHistory: selectedPatientObj?.medicalHistory || 'Chưa ghi nhận',
        physicalExamination: values.examinationNote || (vitalSigns ? `Huyết áp: ${vitalSigns.bp || '120/80'}, Mạch: ${vitalSigns.pulse || '75'}, Thân nhiệt: ${vitalSigns.temp || '37.0'}°C` : ''),
        clinicalProgress: 'Đang điều trị',
        treatmentPlan: values.treatmentPlan || '',
        doctorInstructions: values.treatmentPlan || 'Theo dõi sức khỏe và uống thuốc theo đơn',
        conclusion: fullDiagnosisText,
      }

      if (!isUuid(validVisitId) && !isDemo) {
        throw new Error('Lượt khám chưa được đồng bộ với hệ thống. Vui lòng tải lại danh sách bệnh nhân.')
      }

      if (isUuid(validVisitId)) {
        try {
          const existingResponse = await medicalRecordApi.getByVisit(validVisitId)
          const existingRecord = existingResponse?.data || {}
          const existingRecordId = existingRecord.medicalRecordId || existingRecord.id || null
          if (!existingRecordId) {
            throw new Error('Không tìm thấy mã bệnh án của lượt khám hiện tại.')
          }
          if (existingRecord.status === 'LOCKED') {
            throw new Error('Bệnh án đã khóa nên không thể chỉnh sửa hoặc tạo thêm chỉ định.')
          }
          const updatePayload = {
            chiefComplaint: beCreatePayload.chiefComplaint,
            symptoms: beCreatePayload.symptoms,
            medicalHistory: beCreatePayload.medicalHistory,
            physicalExamination: beCreatePayload.physicalExamination,
            clinicalProgress: beCreatePayload.clinicalProgress,
            treatmentPlan: beCreatePayload.treatmentPlan,
            doctorInstructions: beCreatePayload.doctorInstructions,
            conclusion: beCreatePayload.conclusion,
          }
          await medicalRecordApi.update(existingRecordId, updatePayload)
          beRecordId = existingRecordId
        } catch (getError) {
          if (getError?.response?.status === 404) {
            try {
              const response = await medicalRecordApi.create(beCreatePayload)
              beRecordId = response?.data?.id || null
              if (!beRecordId) throw new Error('Máy chủ không trả về mã bệnh án vừa tạo.')
            } catch (createError) {
              if (!isDemo) throw new Error('Không thể tạo bệnh án cho lượt khám hiện tại.', { cause: createError })
              console.warn('Không thể tạo bệnh án trên máy chủ:', createError)
            }
          } else {
            if (!isDemo) {
              if (getError instanceof Error && !getError?.response) throw getError
              throw new Error('Không thể tải hoặc cập nhật bệnh án trên máy chủ.', { cause: getError })
            }
            console.warn('Không thể tải hoặc cập nhật bệnh án trên máy chủ:', getError)
          }
        }
      }

      if (!beRecordId && !isDemo) {
        throw new Error('Bệnh án chưa được lưu trên máy chủ. Vui lòng thử lại.')
      }

      if (beRecordId) {
        completeRecord.id = beRecordId
      }

      // 2. Đồng bộ chẩn đoán theo đúng mã danh mục từ máy chủ.
      if (beRecordId && isUuid(canonicalPrimaryIcd?.diagnosisCatalogId)) {
        try {
          await medicalRecordApi.recordDiagnosis(beRecordId, {
            primaryDiagnosis: {
              diagnosisCatalogId: canonicalPrimaryIcd.diagnosisCatalogId,
              code: canonicalPrimaryIcd.code,
              name: canonicalPrimaryIcd.name,
              note: values.examinationNote || values.symptoms || '',
            },
            secondaryDiagnoses: canonicalSecondaryIcds
              .filter((item) => isUuid(item.diagnosisCatalogId) && item.code !== canonicalPrimaryIcd.code)
              .map((item) => ({
                diagnosisCatalogId: item.diagnosisCatalogId,
                code: item.code,
                name: item.name,
                note: item.note || '',
              })),
          })
        } catch (diagErr) {
          console.warn('Không thể đồng bộ chẩn đoán với máy chủ:', diagErr)
          if (!isDemo) {
            throw new Error('Bệnh án đã lưu nhưng danh mục chẩn đoán chưa đồng bộ. Biểu mẫu được giữ lại để bạn thử lại.')
          }
        }
      } else if (beRecordId) {
        message.warning('Bệnh án đã lưu, nhưng chẩn đoán tự nhập không có mã danh mục để đồng bộ.')
      }

      let storedClinicalOrder = null
      let clinicalOrderSyncFailed = false
      if (selectedOrders.length > 0) {
        const canSyncOrder = Boolean(beRecordId) && isUuid(validVisitId) && selectedOrders.every((item) => isUuid(item.id))
        let serverOrder = null
        if (canSyncOrder) {
          try {
            const response = await medicalRecordApi.createClinicalOrder(validVisitId, {
              clinicalReason: fullDiagnosisText,
              items: selectedOrders.map((item) => ({
                serviceId: item.id,
                instruction: item.note || (item.isUrgent ? 'CẤP CỨU' : ''),
              })),
            })
            serverOrder = response?.data || null
            if (!serverOrder?.id) throw new Error('Máy chủ không trả về mã phiếu chỉ định.')
          } catch (orderErr) {
            console.warn('Không thể đồng bộ chỉ định cận lâm sàng:', orderErr)
            clinicalOrderSyncFailed = !isDemo
          }
        } else if (!isDemo) {
          clinicalOrderSyncFailed = true
        }

        if (!clinicalOrderSyncFailed) {
          const normalizeOrderStatus = (status) => {
            if (status === 'ORDERED') return 'PENDING'
            if (status === 'PARTIALLY_COMPLETED') return 'RESULTED'
            return status || 'PENDING'
          }
          const fallbackOrderCode = `CD-${dayjs().format('YYYYMMDD-HHmmss')}`
          storedClinicalOrder = {
            id: serverOrder?.id || `ord-${Date.now()}`,
            orderCode: serverOrder?.orderCode || fallbackOrderCode,
            visitId: serverOrder?.visitId || validVisitId,
            patientId: serverOrder?.patientId || values.patientId,
            patientCode: selectedPatientObj?.patientCode || '',
            patientName: selectedPatientObj?.fullName || selectedPatientObj?.patientName || 'Bệnh nhân',
            gender: selectedPatientObj?.gender,
            age: selectedPatientObj?.age,
            doctorId: serverOrder?.orderedBy || user?.id,
            doctorName: user?.fullName || user?.username || 'Bác sĩ trực',
            department: user?.department || 'Khoa khám bệnh',
            diagnosis: fullDiagnosisText,
            priority: selectedOrders.some((item) => item.isUrgent) ? 'URGENT' : 'NORMAL',
            status: normalizeOrderStatus(serverOrder?.status),
            items: selectedOrders.map((item, index) => {
              const serverItem = serverOrder?.items?.[index]
              return {
                ...item,
                id: serverItem?.id || item.id,
                serviceId: item.id,
                serviceCode: serverItem?.serviceCode || item.code,
                serviceName: serverItem?.serviceName || item.name,
                instruction: serverItem?.instruction || item.note || '',
                status: normalizeOrderStatus(serverItem?.status),
                price: item.price == null ? null : Number(item.price),
                quantity: 1,
              }
            }),
            totalAmount: persistedOrderFee,
            createdAt: serverOrder?.orderedAt || dayjs().toISOString(),
            updatedAt: dayjs().toISOString(),
          }
          saveStoredClinicalOrder(storedClinicalOrder)
        }
      }

      if (clinicalOrderSyncFailed) {
        message.error('Bệnh án đã được lưu nhưng chưa tạo được phiếu chỉ định cận lâm sàng. Biểu mẫu được giữ lại để bạn thử lại.')
        return
      }

      // 3. File Attachments
      files.forEach((file) => {
        if (beRecordId) {
          medicalRecordApi.attach(beRecordId, file).catch(() => {})
        }
        saveStoredAttachment({
          id: file.uid || `att-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
          attachmentCode: `KQ-${dayjs().format('YYYYMMDDHHmm')}`,
          patientId: values.patientId,
          patientName: selectedPatientObj ? selectedPatientObj.fullName : 'Bệnh nhân',
          patientCode: selectedPatientObj ? selectedPatientObj.patientCode : '',
          category: selectedOrders[0]?.name || 'Khác',
          testDate: dayjs().format('YYYY-MM-DD HH:mm'),
          doctorName: user?.fullName || user?.username || 'Bác sĩ',
          status: 'NORMAL',
          resultSummary: fullDiagnosisText || 'Kết quả cận lâm sàng đính kèm bệnh án',
          fileName: file.name,
          fileType: file.type || 'application/pdf',
          fileSize: file.size ? `${(file.size / (1024 * 1024)).toFixed(1)} MB` : '1.0 MB',
          fileUrl: URL.createObjectURL && file instanceof Blob ? URL.createObjectURL(file) : '',
          createdAt: dayjs().toISOString(),
        })
      })

      saveStoredMedicalRecord(completeRecord)
      logMedicalAccess({
        userName: user?.fullName || user?.username || 'Bác sĩ',
        patientName: completeRecord.patientName,
        recordCode: completeRecord.recordCode,
        action: 'Tạo bệnh án & chẩn đoán ICD-10 mới',
      })

      const hasCreatedOrders = selectedOrders.length > 0 && Boolean(storedClinicalOrder)
      const queueSynced = await syncQueueCompletion(values.patientId, hasCreatedOrders)
      if (!queueSynced) {
        message.warning('Bệnh án và phiếu chỉ định đã được lưu, nhưng trạng thái hàng đợi chưa cập nhật. Vui lòng thử lại tại màn hình hàng đợi.')
      }
      message.success(`Đã lưu thành công bệnh án ${recordCode}`)
      resetFormState()
      await loadData()
      setActiveTab('history')
      showSuccessModal(recordCode, values.patientId, hasCreatedOrders)
    } catch (err) {
      console.error('Lỗi khi lưu bệnh án:', err)
      message.error(err?.message || 'Có lỗi xảy ra, vui lòng thử lại!')
    } finally {
      setSaving(false)
    }
  }

  const resetFormState = () => {
    form.resetFields()
    setSelectedPatientId(null)
    setVitalSigns({ bp: '', pulse: '', temp: '37.0', respRate: '16', weight: '', height: '', spO2: '98' })
    setPrimaryIcd(null)
    setSecondaryIcds([])
    setSelectedOrders([])
    setResults({})
    setFiles([])
  }

  const showSuccessModal = (code, pId, hasOrders) => {
    Modal.confirm({
      title: hasOrders ? 'Đã lưu bệnh án và tạo chỉ định thành công!' : 'Đã lưu bệnh án thành công!',
      icon: <CheckCircleOutlined style={{ color: '#16A34A' }} />,
      content: (
        <div>
          <Paragraph>Mã bệnh án: <Text strong style={{ color: '#2563EB' }}>{code}</Text></Paragraph>
          <Paragraph>Bạn muốn thực hiện thao tác tiếp theo nào?</Paragraph>
        </div>
      ),
      okText: 'Chuyển sang Kê đơn thuốc',
      cancelText: 'Xem Lịch sử khám',
      onOk: () => navigate('/prescriptions', { state: { patientId: pId, recordCode: code } }),
      onCancel: () => setActiveTab('history'),
    })
  }

  const beforeUpload = (file) => {
    const allowed = ['application/pdf', 'image/jpeg', 'image/png'].includes(file.type)
    if (!allowed) {
      message.error('Chỉ chấp nhận tệp PDF, JPG hoặc PNG')
      return false
    }
    if (file.size > 10 * 1024 * 1024) {
      message.error('Tệp không được vượt quá 10 MB')
      return false
    }
    setFiles((current) => [...current, file])
    return false
  }

  const downloadAttachment = async (file) => {
    try {
      const response = await medicalRecordApi.downloadAttachment(file.id)
      const url = URL.createObjectURL(response.data)
      const link = document.createElement('a')
      link.href = url
      link.download = file.fileName
      link.click()
      URL.revokeObjectURL(url)
    } catch {
      message.info(`Đã lưu tệp đính kèm: ${file.fileName}`)
    }
  }

  const openRecord = async (record) => {
    logMedicalAccess({
      userName: user?.fullName || user?.username || 'Bác sĩ',
      patientName: record.patientName || 'Bệnh nhân',
      recordCode: record.recordCode || 'BA-001',
      action: 'Xem thông tin hồ sơ bệnh án điện tử',
    })
    try {
      const res = await medicalRecordApi.getById(record.id)
      setViewing({ ...record, ...(res.data || {}) })
    } catch {
      setViewing(record)
    }
  }

  const historyColumns = [
    {
      title: 'Mã bệnh án',
      dataIndex: 'recordCode',
      render: (value) => <Tag color="blue" style={{ fontWeight: 600 }}>{value}</Tag>,
    },
    { title: 'Bệnh nhân', dataIndex: 'patientName' },
    {
      title: 'Chẩn đoán',
      dataIndex: 'diagnosis',
      ellipsis: true,
      render: (val) => <Text style={{ color: '#1E40AF' }}>{val}</Text>,
    },
    { title: 'Bác sĩ khám', dataIndex: 'doctorName' },
    {
      title: 'Ngày lập',
      dataIndex: 'createdAt',
      render: (value) => dayjs(value).format('HH:mm DD/MM/YYYY'),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      render: (value) => <Tag color="green">{MEDICAL_RECORD_STATUS_LABELS[value] || 'Hoàn thành'}</Tag>,
    },
    {
      title: 'Thao tác',
      render: (_, record) => (
        <Button icon={<EyeOutlined />} size="small" onClick={() => openRecord(record)}>
          Xem chi tiết
        </Button>
      ),
    },
  ]

  return (
    <div style={{ paddingBottom: 40 }}>
      {/* Header Bar */}
      <div className="page-header" style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0, color: '#0F172A', display: 'flex', alignItems: 'center', gap: 10 }}>
            <MedicineBoxOutlined style={{ color: '#2563EB' }} /> Khám bệnh & phân loại chẩn đoán y khoa
          </h2>
          <Text type="secondary" style={{ fontSize: 13 }}>
            Ghi nhận chẩn đoán ICD-10, tạo chỉ định cận lâm sàng và quản lý bệnh án điện tử.
          </Text>
        </div>
        {isDoctor && (
          <Space>
            {selectedOrders.length > 0 && (
              <Button icon={<PrinterOutlined />} onClick={() => setPrintModalOpen(true)}>
                In phiếu chỉ định ({selectedOrders.length})
              </Button>
            )}
            <Button type="primary" size="large" loading={saving} icon={<CheckCircleOutlined />} onClick={saveRecord}>
              Lưu hồ sơ bệnh án
            </Button>
          </Space>
        )}
      </div>

      <Alert
        showIcon
        type="info"
        message="Quy trình khám bệnh"
        description="1. Chọn bệnh nhân và nhập sinh hiệu → 2. Chọn mã ICD-10 cho chẩn đoán chính và phụ → 3. Tạo chỉ định cận lâm sàng → 4. Đính kèm kết quả → 5. Lưu bệnh án và chuyển kê đơn."
        style={{ marginBottom: 16, borderRadius: 8 }}
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
                <SolutionOutlined /> Ghi bệnh án & chẩn đoán
              </span>
            ),
            children: (
              <MedicalEncounterForm
                form={form}
                isDoctor={isDoctor}
                patients={patients}
                setSelectedPatientId={handlePatientSelection}
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
                setSelectedOrders={setSelectedOrders}
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
                results={results}
                setResults={setResults}
                files={files}
                setFiles={setFiles}
                beforeUpload={beforeUpload}
              />
            ),
          },
          {
            key: 'history',
            label: `Lịch sử hồ sơ bệnh án (${records.length})`,
            children: (
              <Card bordered>
                <Table
                  rowKey="id"
                  columns={historyColumns}
                  dataSource={records}
                  pagination={{ pageSize: 10 }}
                />
              </Card>
            ),
          },
        ]}
      />

      {/* Modal ICD-10 Search Catalog */}
      <Modal
        title="Tra cứu danh mục mã bệnh ICD-10"
        open={diagnosisModalOpen}
        onCancel={() => setDiagnosisModalVisibility(false)}
        footer={[
          <Button key="close" onClick={() => setDiagnosisModalVisibility(false)}>
            Đóng
          </Button>,
        ]}
        width={750}
      >
        <Alert
          type={primaryIcd ? 'info' : 'warning'}
          showIcon
          message={primaryIcd
            ? `Chẩn đoán chính: [${primaryIcd.code}] ${primaryIcd.name}`
            : 'Chưa chọn chẩn đoán chính'}
          description={primaryIcd
            ? 'Chẩn đoán chính sẽ không xuất hiện trong lựa chọn chẩn đoán phụ.'
            : 'Hãy chọn chẩn đoán chính trước khi thêm chẩn đoán phụ.'}
          style={{ marginBottom: 12 }}
        />
        <div style={{ marginBottom: 12, display: 'flex', gap: 10 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Tìm theo mã bệnh (J00, K29...) hoặc tên bệnh..."
            value={icdSearchQuery}
            onChange={(e) => setIcdSearchQuery(e.target.value)}
            allowClear
          />
          <Select
            value={icdCategory}
            onChange={setIcdCategory}
            style={{ width: 260 }}
            options={icd10Categories.map((c) => ({ value: c.key, label: c.label }))}
          />
        </div>

        <Text type="secondary" style={{ display: 'block', marginBottom: 10 }}>
          {icdSearchQuery.trim()
            ? 'Tối đa 10 kết quả phù hợp từ danh mục ICD-10.'
            : '10 chẩn đoán gần đây hoặc thường dùng. Nhập ít nhất 2 ký tự để tìm thêm từ hệ thống.'}
        </Text>

        <Table
          size="small"
          rowKey="code"
          dataSource={filteredIcdList}
          loading={icdSearching}
          pagination={false}
          columns={[
            {
              title: 'Mã ICD',
              dataIndex: 'code',
              width: 100,
              render: (code) => <Tag color="blue" style={{ fontWeight: 700 }}>{code}</Tag>,
            },
            { title: 'Tên bệnh / Hội chứng y khoa', dataIndex: 'name' },
            {
              title: 'Thao tác',
              width: 160,
              render: (_, item) => (
                <Space>
                  <Button
                    size="small"
                    type="primary"
                    onClick={async () => {
                      await selectPrimaryDiagnosis(item)
                      setDiagnosisModalOpen(false)
                      message.success(`Đã chọn chẩn đoán chính: [${item.code}] ${item.name}`)
                    }}
                  >
                    Chọn chính
                  </Button>
                  <Button
                    size="small"
                    disabled={!primaryIcd || primaryIcd.code === item.code}
                    onClick={async () => {
                      const added = await addSecondaryDiagnosis(item)
                      if (added) message.success(`Đã thêm chẩn đoán phụ: [${item.code}] ${item.name}`)
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

      {/* Modal View Medical Record Details */}
      <Modal
        title={`Chi tiết hồ sơ bệnh án điện tử ${viewing?.recordCode || ''}`}
        open={!!viewing}
        onCancel={() => setViewing(null)}
        footer={<Button onClick={() => setViewing(null)}>Đóng</Button>}
        width={760}
      >
        {viewing && (
          <div>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="Bệnh nhân">{viewing.patientName}</Descriptions.Item>
              <Descriptions.Item label="Bác sĩ khám">{viewing.doctorName}</Descriptions.Item>
              <Descriptions.Item label="Lý do khám / Triệu chứng">{viewing.symptoms}</Descriptions.Item>
              <Descriptions.Item label="Khám lâm sàng">{viewing.examinationNote || '---'}</Descriptions.Item>
              <Descriptions.Item label="Chẩn đoán Y khoa">
                <Text strong style={{ color: '#1E40AF' }}>{viewing.diagnosis}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Hướng điều trị">{viewing.treatmentPlan || '---'}</Descriptions.Item>
              <Descriptions.Item label="Chỉ định cận lâm sàng">
                {viewing.clinicalOrders?.join(', ') || 'Không có chỉ định'}
              </Descriptions.Item>
              <Descriptions.Item label="Kết quả cận lâm sàng">
                {Object.entries(viewing.clinicalResults || {})
                  .map(([key, value]) => `${key}: ${value}`)
                  .join(' | ') || 'Chưa cập nhật'}
              </Descriptions.Item>
            </Descriptions>

            <Divider style={{ margin: '16px 0' }} />
            <Text strong>Tệp Đính Kèm Hồ Sơ</Text>
            <List
              header={null}
              dataSource={viewing.attachments || []}
              locale={{ emptyText: 'Không có tệp đính kèm' }}
              renderItem={(file) => (
                <List.Item>
                  <Button type="link" onClick={() => downloadAttachment(file)}>
                    {file.fileName}
                  </Button>
                </List.Item>
              )}
            />
          </div>
        )}
      </Modal>

      {/* Clinical Order Printable Sheet Preview Modal */}
      <ClinicalOrderPrintModal
        open={printModalOpen}
        onClose={() => setPrintModalOpen(false)}
        patient={selectedPatientObj}
        recordCode={`BA-${dayjs().format('YYYYMMDD')}`}
        diagnosis={form.getFieldValue('diagnosisText')}
        primaryIcd={primaryIcd}
        secondaryIcds={secondaryIcds}
        orders={selectedOrders}
        doctorName={user?.fullName || user?.username || 'BS. Phạm Hồng Anh'}
        vitalSigns={vitalSigns}
      />
    </div>
  )
}

export default MedicalEncounter
