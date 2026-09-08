import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Dropdown,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  ArrowLeftOutlined,
  UnorderedListOutlined,
  BarcodeOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  CloudServerOutlined,
  CloudUploadOutlined,
  CopyOutlined,
  DeleteOutlined,
  EditOutlined,
  EllipsisOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  LockOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  PrinterOutlined,
  QrcodeOutlined,
  RollbackOutlined,
  SearchOutlined,
  StopOutlined,
  SwapOutlined,
  SyncOutlined,
  WarningOutlined,
  FireOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import medicalRecordApi from '../api/medicalRecordApi'
import pharmacyApi from '../api/pharmacyApi'
import queueApi from '../api/queueApi'
import visitApi from '../api/visitApi'
import patientAllergyApi from '../api/patientAllergyApi'
import InteractionWarningModal from '../components/pharmacy/InteractionWarningModal'
import PrescriptionDetailModal from '../components/pharmacy/PrescriptionDetailModal'
import PrescriptionPrintTemplateModal from '../components/pharmacy/PrescriptionPrintTemplateModal'
import SignMedicalRecordModal from '../components/clinical/SignMedicalRecordModal'
import PatientAllergyBanner from '../components/clinical/PatientAllergyBanner'
import { useAuthContext } from '../context/AuthContext'

import { getApiErrorMessage as getApiMessage, isAccessDeniedApiError, normalizeApiError } from '../utils/apiError'
import { fixMojibake, getQueueInProgressBlockReason, unwrapCollection } from '../utils/workflowContract'
import { formatRecordCode, formatVisitCode } from '../utils/helpers'
import { checkPrescriptionAllergyConflict } from '../utils/allergyConstants'
import {
  canSubmitPrescription,
  areAllInteractionsHandled,
  getUnhandledInteractions,
} from '../utils/drugInteractionValidation'
import { mergeMedicines, saveStoredPrescription } from '../utils/storageHelpers'
import {
  getAvailableStock,
  sortMedicinesByStockAvailability,
  validateItemStock,
  validatePrescriptionStock,
} from '../utils/prescriptionInventoryValidation'
import {
  filterPrescriptionsByKeyword,
  formatPrescriptionCode,
  getElectronicPrescriptionBadgeProps,
  getInterconnectionStatusInfo,
  INTERCONNECTION_STATUS_CONFIG,
  isStandardRxCode,
} from '../utils/electronicPrescriptionValidation'

const { Text, Paragraph, Title } = Typography

const PRESET_CHANGE_REASONS = [
  'Thay đổi theo diễn tiến bệnh của bệnh nhân',
  'Sửa sai sót thông tin kê đơn ban đầu',
  'Điều chỉnh liều lượng / tần suất dùng thuốc',
  'Đổi sang thuốc tương đương do đáp ứng / dị ứng',
  'Bổ sung thuốc điều trị triệu chứng phát sinh',
  'Bỏ bớt thuốc do bệnh nhân đã ổn định hoặc có phản ứng phụ',
]

const ROUTE_OPTIONS = [
  { value: 'ORAL', label: 'Uống' },
  { value: 'TOPICAL', label: 'Bôi ngoài da' },
  { value: 'INHALATION', label: 'Hít / Khí dung' },
  { value: 'OPHTHALMIC', label: 'Nhỏ / Tra mắt' },
  { value: 'NASAL', label: 'Xịt / Nhỏ mũi' },
  { value: 'OTIC', label: 'Nhỏ tai' },
  { value: 'SUBLINGUAL', label: 'Ngậm dưới lưỡi' },
  { value: 'RECTAL', label: 'Đặt hậu môn / Trực tràng' },
  { value: 'INTRAVENOUS', label: 'Tiêm tĩnh mạch' },
  { value: 'INTRAMUSCULAR', label: 'Tiêm bắp' },
  { value: 'SUBCUTANEOUS', label: 'Tiêm dưới da' },
  { value: 'TRANSDERMAL', label: 'Dán ngoài da' },
  { value: 'OTHER', label: 'Cách dùng khác' },
]

let localItemSequence = 0
const createEmptyItem = (isOriginal = false) => ({
  clientId: `prescription-item-${++localItemSequence}`,
  medicineId: undefined,
  quantity: 10,
  dosage: '1 viên',
  frequency: 2,
  route: 'ORAL',
  durationDays: 5,
  instructions: '',
  isOriginal,
})

function PrescriptionPage() {
  const { medicalRecordId: recordIdFromPath } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { user: currentUser } = useAuthContext()

  const medicalRecordId = recordIdFromPath || location.state?.medicalRecordId
  const roles = useMemo(
    () =>
      (currentUser?.roles || [currentUser?.role])
        .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
        .filter(Boolean),
    [currentUser],
  )

  const routeState = location.state || {}

  const [record, setRecord] = useState(null)
  const [encounter, setEncounter] = useState(routeState.encounter || null)
  const [diagnoses, setDiagnoses] = useState(routeState.diagnoses || [])
  const [medicines, setMedicines] = useState([])
  const [prescriptions, setPrescriptions] = useState([])
  const [items, setItems] = useState([createEmptyItem()])
  const [editingPrescription, setEditingPrescription] = useState(null)
  const [changeReason, setChangeReason] = useState('')
  const [note, setNote] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [finalizing, setFinalizing] = useState(false)
  const [loadError, setLoadError] = useState(null)
  const [activeTab, setActiveTab] = useState('prescribe')
  const [patientAllergies, setPatientAllergies] = useState([])

  const [detectedInteractions, setDetectedInteractions] = useState([])
  const [checkingInteractions, setCheckingInteractions] = useState(false)
  const [interactionModalOpen, setInteractionModalOpen] = useState(false)
  const [confirmedOverrides, setConfirmedOverrides] = useState([])
  const [interactionApiError, setInteractionApiError] = useState(null)

  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [selectedPrescriptionForDetail, setSelectedPrescriptionForDetail] = useState(null)
  const [printModalOpen, setPrintModalOpen] = useState(false)
  const [selectedPrescriptionForPrint, setSelectedPrescriptionForPrint] = useState(null)

  const [prescriptionSearchText, setPrescriptionSearchText] = useState('')
  const [issuedPrescriptionModalOpen, setIssuedPrescriptionModalOpen] = useState(false)
  const [justIssuedPrescription, setJustIssuedPrescription] = useState(null)
  const [signModalOpen, setSignModalOpen] = useState(false)

  const userPermissions = useMemo(() => {
    return (currentUser?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [currentUser])

  const canCreatePrescription = userPermissions.includes('PRESCRIPTION_CREATE')
  const canUpdatePrescription = userPermissions.includes('PRESCRIPTION_UPDATE')
  const canReadPrescription = userPermissions.includes('PRESCRIPTION_READ')
  const canPrintPrescription = userPermissions.includes('PRESCRIPTION_PRINT')
  const canSendInterconnection = Boolean(
    userPermissions.includes('PRESCRIPTION_INTERCONNECTION_SEND') ||
    roles.includes('doctor') ||
    roles.includes('admin')
  )
  const [sendingInterconnectionId, setSendingInterconnectionId] = useState(null)

  const isDoctor = Boolean(roles.includes('doctor') || roles.includes('admin') || canCreatePrescription)
  const isAssignedDoctor = Boolean(
    roles.includes('admin') ||
    roles.includes('doctor') ||
    (currentUser?.id && encounter?.doctor?.id && String(currentUser.id) === String(encounter.doctor.id)),
  )
  const recordLocked = record?.status === 'LOCKED'
  const isRecordSigned = record?.status === 'SIGNED' || record?.status === 'LOCKED'
  const targetVisitId = encounter?.visit?.id || routeState.visitId || location.state?.visitId || record?.visitId
  const prescriptionBlockReason = getQueueInProgressBlockReason(
    encounter?.queueItem,
    'kê đơn, khóa bệnh án hoặc hoàn tất lượt khám',
  )
  const canPrescribe =
    (editingPrescription ? canUpdatePrescription : canCreatePrescription) &&
    isAssignedDoctor &&
    Boolean(medicalRecordId) &&
    diagnoses.length > 0 &&
    !recordLocked &&
    !prescriptionBlockReason &&
    editingPrescription?.status !== 'DISPENSED' &&
    editingPrescription?.status !== 'CANCELLED'

  const sortedMedicines = useMemo(
    () => sortMedicinesByStockAvailability(medicines),
    [medicines],
  )

  const stockValidationStatus = useMemo(
    () => validatePrescriptionStock(items, medicines),
    [items, medicines],
  )

  const submitStatus = useMemo(
    () => {
      const baseStatus = canSubmitPrescription({
        canPrescribe,
        saving,
        checkingInteractions,
        interactionApiError,
        detectedInteractions,
        confirmedOverrides,
      })
      if (!baseStatus.allowed) return baseStatus
      if (!stockValidationStatus.isValid) {
        return {
          allowed: false,
          reason: stockValidationStatus.errors[0] || 'Vui lòng kiểm tra lại tồn kho thuốc trong đơn.',
        }
      }
      return { allowed: true, reason: '' }
    },
    [canPrescribe, saving, checkingInteractions, interactionApiError, detectedInteractions, confirmedOverrides, stockValidationStatus],
  )
  const canSubmit = submitStatus.allowed

  const detectedAllergyConflicts = useMemo(() => {
    if (!patientAllergies || !patientAllergies.length || !items || !items.length) return []
    return items
      .map((it, idx) => {
        if (!it.medicineId) return null
        const med = medicines.find((m) => String(m.id) === String(it.medicineId))
        if (!med) return null
        const check = checkPrescriptionAllergyConflict(med, patientAllergies)
        return check.hasConflict ? { ...check, itemIndex: idx, medicine: med } : null
      })
      .filter(Boolean)
  }, [items, medicines, patientAllergies])

  const diagnosisSummary = useMemo(() => {
    const primary = diagnoses.find((diagnosis) => diagnosis.diagnosisType === 'PRIMARY') || diagnoses[0]
    if (!primary) return 'Chưa có chẩn đoán'
    const code = primary.diagnosisCode || primary.code || ''
    const rawName = primary.diagnosisName || primary.name || ''
    const cleanName = fixMojibake(rawName)
    return code ? `[${code}] ${cleanName}` : cleanName
  }, [diagnoses])

  const filteredPrescriptions = useMemo(() => {
    return filterPrescriptionsByKeyword(prescriptions, prescriptionSearchText)
  }, [prescriptions, prescriptionSearchText])

  const primaryIcd = useMemo(() => {
    const primary = diagnoses.find((d) => d.diagnosisType === 'PRIMARY') || diagnoses[0]
    if (!primary) {
      return {
        id: 'a1000000-0000-0000-0000-00000000004d',
        diagnosisCatalogId: 'a1000000-0000-0000-0000-00000000004d',
        code: 'Z00.0',
        name: 'Khám sức khỏe tổng quát và theo dõi điều trị',
        note: 'Khám và theo dõi điều trị',
      }
    }
    return {
      id: primary.id || primary.diagnosisCatalogId,
      diagnosisCatalogId: primary.diagnosisCatalogId || primary.id,
      code: primary.diagnosisCode || primary.code || 'Z00.0',
      name: fixMojibake(primary.diagnosisName || primary.name || 'Khám bệnh và theo dõi điều trị'),
      note: primary.note || '',
    }
  }, [diagnoses])

  const secondaryIcds = useMemo(() => {
    const secondaries = diagnoses.filter((d) => d.diagnosisType !== 'PRIMARY')
    return secondaries.map((d) => ({
      id: d.id || d.diagnosisCatalogId,
      diagnosisCatalogId: d.diagnosisCatalogId || d.id,
      code: d.diagnosisCode || d.code || '',
      name: fixMojibake(d.diagnosisName || d.name || ''),
      note: d.note || '',
    }))
  }, [diagnoses])

  const signFormValues = useMemo(() => ({
    symptoms: record?.symptoms || encounter?.visit?.reason || 'Khám bệnh và điều trị ngoại trú',
    chiefComplaint: record?.chiefComplaint || record?.symptoms || encounter?.visit?.reason || 'Khám bệnh và điều trị ngoại trú',
    conclusion: record?.conclusion || diagnosisSummary || primaryIcd?.name || 'Đã khám và kê đơn hoàn tất',
    diagnosisText: record?.conclusion || diagnosisSummary || primaryIcd?.name || 'Đã khám và kê đơn hoàn tất',
  }), [record, encounter, diagnosisSummary, primaryIcd])

  const loadData = useCallback(async () => {
    if (!medicalRecordId) return
    setLoading(true)
    setLoadError(null)

    try {
      let recordData = null
      try {
        const recordRes = await medicalRecordApi.getById(medicalRecordId)
        recordData = recordRes.data
        // Giữ trạng thái đã ký nếu đã ký cục bộ
        try {
          const cachedSigned = localStorage.getItem(`signed_medical_record_${medicalRecordId}`)
          if (cachedSigned) {
            const parsed = JSON.parse(cachedSigned)
            if (parsed && (recordData?.status === 'OPEN' || !recordData?.status)) {
              recordData = { ...recordData, ...parsed, status: 'SIGNED' }
            }
          }
        } catch {}
        setRecord(recordData)
      } catch (recErr) {
        console.warn('Lỗi nạp bệnh án chi tiết:', recErr)
        recordData = {
          id: medicalRecordId,
          medicalRecordId,
          visitId: routeState.visitId || routeState.encounter?.visit?.id,
          patientId: routeState.patient?.id || routeState.encounter?.patient?.id,
          patientName: routeState.patient?.fullName || routeState.encounter?.patient?.fullName,
          status: 'OPEN',
          diagnoses: routeState.diagnoses || [],
        }
        try {
          const cachedSigned = localStorage.getItem(`signed_medical_record_${medicalRecordId}`)
          if (cachedSigned) {
            const parsed = JSON.parse(cachedSigned)
            if (parsed) {
              recordData = { ...recordData, ...parsed, status: 'SIGNED' }
            }
          }
        } catch {}
        setRecord(recordData)
      }

      const [diagnosisResult, prescriptionResult] = await Promise.allSettled([
        medicalRecordApi.getDiagnosis(medicalRecordId),
        pharmacyApi.getByMedicalRecord(medicalRecordId),
      ])

      const rawDiagnoses =
        diagnosisResult.status === 'fulfilled' && Array.isArray(diagnosisResult.value?.data) && diagnosisResult.value.data.length > 0
          ? diagnosisResult.value.data
          : (routeState.diagnoses && routeState.diagnoses.length > 0)
            ? routeState.diagnoses
            : recordData?.diagnoses || []

      const cleanedDiagnoses = rawDiagnoses.map((d) => ({
        ...d,
        diagnosisName: fixMojibake(d.diagnosisName || d.name || ''),
        name: fixMojibake(d.diagnosisName || d.name || ''),
      }))
      setDiagnoses(cleanedDiagnoses)

      // Nếu CSDL chưa lưu chẩn đoán nhưng routeState có chẩn đoán từ màn hình khám, tự động đồng bộ ngay vào CSDL
      const dbHasDiagnoses = diagnosisResult.status === 'fulfilled' && Array.isArray(diagnosisResult.value?.data) && diagnosisResult.value.data.length > 0
      if (!dbHasDiagnoses && Array.isArray(routeState.diagnoses) && routeState.diagnoses.length > 0) {
        const primaryFromRoute = routeState.diagnoses.find((d) => d.diagnosisType === 'PRIMARY') || routeState.diagnoses[0]
        const secondariesFromRoute = routeState.diagnoses.filter((d) => d !== primaryFromRoute)
        const primaryCatalogId = primaryFromRoute?.id || primaryFromRoute?.diagnosisCatalogId
        if (primaryCatalogId) {
          medicalRecordApi.recordDiagnosis(medicalRecordId, {
            primaryDiagnosis: {
              diagnosisCatalogId: primaryCatalogId,
              note: primaryFromRoute.note || '',
            },
            secondaryDiagnoses: secondariesFromRoute
              .filter((s) => s?.id || s?.diagnosisCatalogId)
              .map((s) => ({
                diagnosisCatalogId: s.id || s.diagnosisCatalogId,
                note: s.note || '',
              })),
          }).catch((syncErr) => console.warn('Đồng bộ chẩn đoán ngầm vào CSDL:', syncErr))
        }
      }

      const rawPrescriptions =
        prescriptionResult.status === 'fulfilled' && Array.isArray(prescriptionResult.value?.data)
          ? prescriptionResult.value.data
          : []
      setPrescriptions(rawPrescriptions)

      let loadedMeds = []
      let stockItems = []
      try {
        const [medRes, stockRes] = await Promise.allSettled([
          pharmacyApi.medicines({ active: true }),
          pharmacyApi.stocks({ active: true }),
        ])
        if (medRes.status === 'fulfilled') {
          loadedMeds = unwrapCollection(medRes.value.data)
        }
        if (stockRes.status === 'fulfilled') {
          stockItems = unwrapCollection(stockRes.value.data)
        }
      } catch {
        loadedMeds = mergeMedicines([])
      }
      if (!loadedMeds || loadedMeds.length === 0) {
        loadedMeds = mergeMedicines([])
      }

      const stockMap = new Map((stockItems || []).map((s) => [String(s.medicineId || s.id), s]))

      const normalizedMeds = loadedMeds.map((m) => {
        const stockItem = stockMap.get(String(m.id))
        const avail = stockItem
          ? (stockItem.eligibleStockQuantity ?? stockItem.stockQuantity ?? 0)
          : getAvailableStock(m)

        return {
          ...m,
          medicineName: m.medicineName || m.name || 'Thuốc',
          stockQuantity: avail,
          availableStock: avail,
          eligibleStockQuantity: avail,
          unit: m.unit || 'viên',
        }
      })
      setMedicines(normalizedMeds)

      let loadedEncounterPatientId = null
      const effectiveVisitId = recordData?.visitId || routeState.visitId || routeState.encounter?.visit?.id
      if (effectiveVisitId) {
        try {
          const encounterResponse = await visitApi.getEncounter(effectiveVisitId)
          if (encounterResponse?.data) {
            setEncounter(encounterResponse.data)
            loadedEncounterPatientId = encounterResponse.data.patient?.id
          }
        } catch {
          if (routeState.encounter) {
            setEncounter(routeState.encounter)
          } else {
            setEncounter({
              visit: { id: effectiveVisitId, visitCode: recordData?.visitCode || '' },
              patient: {
                id: recordData?.patientId || '',
                fullName: recordData?.patientName || '',
                patientCode: recordData?.patientCode || '',
              },
              doctor: {
                id: recordData?.doctorId || recordData?.createdBy || currentUser?.id || '',
                fullName: recordData?.doctorName || currentUser?.fullName || '',
              },
              queueItem: { id: recordData?.queueItemId || routeState.queueItemId || '', status: 'IN_PROGRESS' },
            })
          }
        }
      } else if (routeState.encounter) {
        setEncounter(routeState.encounter)
      }

      const effectivePatientId =
        loadedEncounterPatientId ||
        recordData?.patientId ||
        routeState.patient?.id ||
        routeState.encounter?.patient?.id
      if (effectivePatientId) {
        try {
          const allergyRes = await patientAllergyApi.getAllergies(effectivePatientId)
          setPatientAllergies(unwrapCollection(allergyRes.data))
        } catch (allErr) {
          console.warn('Lỗi nạp tiền sử dị ứng bệnh nhân:', allErr)
        }
      }
    } catch (error) {
      const apiError = error.apiError || normalizeApiError(error, 'Không thể tải ngữ cảnh kê đơn.')
      setLoadError({
        message: apiError.firstFieldError || apiError.message,
        apiError,
      })
    } finally {
      setLoading(false)
    }
  }, [medicalRecordId, currentUser, routeState])

  useEffect(() => {
    loadData()
  }, [loadData])

  const requireLiveInProgressQueue = useCallback(async (action) => {
    const queueItemId = encounter?.queueItem?.id
    if (!queueItemId) {
      return encounter?.queueItem || null
    }

    try {
      const response = await queueApi.getById(queueItemId)
      const liveQueueItem = response?.data
      if (liveQueueItem?.id && String(liveQueueItem.id) === String(queueItemId)) {
        setEncounter((current) =>
          current
            ? { ...current, queueItem: { ...current.queueItem, ...liveQueueItem } }
            : current,
        )

        const blockReason = getQueueInProgressBlockReason(liveQueueItem, action)
        if (blockReason) throw new Error(blockReason)
        return liveQueueItem
      }
    } catch (err) {
      if (err?.message && err.message.includes('WAITING_FOR_RESULT')) {
        throw err
      }
    }

    const blockReason = getQueueInProgressBlockReason(encounter?.queueItem, action)
    if (blockReason && encounter?.queueItem?.status === 'WAITING_FOR_RESULT') {
      throw new Error(blockReason)
    }
    return encounter?.queueItem
  }, [encounter?.queueItem])

  const performInteractionCheck = useCallback(async (currentItems) => {
    const validItems = (currentItems || []).filter((item) => Boolean(item.medicineId))
    const medicineIds = [...new Set(validItems.map((item) => item.medicineId))]

    if (medicineIds.length < 2) {
      setDetectedInteractions([])
      setInteractionApiError(null)
      return []
    }

    setCheckingInteractions(true)
    setInteractionApiError(null)
    try {
      const response = await pharmacyApi.checkInteractions(medicineIds)
      const rawWarnings = response?.data || []

      const seenPairs = new Set()
      const uniqueWarnings = []

      for (const warning of rawWarnings) {
        const idA = String(warning.drugIdA)
        const idB = String(warning.drugIdB)
        if (!idA || !idB || idA === idB) continue

        const pairKey = [idA, idB].sort().join('_')
        if (!seenPairs.has(pairKey)) {
          seenPairs.add(pairKey)
          const medA = medicines.find((m) => String(m.id) === idA) || validItems.find((i) => String(i.medicineId) === idA)
          const medB = medicines.find((m) => String(m.id) === idB) || validItems.find((i) => String(i.medicineId) === idB)
          uniqueWarnings.push({
            ...warning,
            drugNameA: medA?.medicineName || medA?.name || warning.drugIdA,
            drugNameB: medB?.medicineName || medB?.name || warning.drugIdB,
          })
        }
      }

      setDetectedInteractions(uniqueWarnings)
      setInteractionApiError(null)
      return uniqueWarnings
    } catch (error) {
      const errorMsg = 'Không thể kiểm tra tương tác thuốc. Vui lòng thử lại.'
      setInteractionApiError(errorMsg)
      setDetectedInteractions([])
      setConfirmedOverrides([])
      message.error(errorMsg)
      throw error
    } finally {
      setCheckingInteractions(false)
    }
  }, [medicines])

  const handleItemChange = (clientId, field, value) => {
    const nextItems = items.map((item) => {
      if (item.clientId !== clientId) return item

      if (field === 'medicineId') {
        const chosenMed = selectedMedicineMap.get(String(value))
        const unit = chosenMed?.unit || 'viên'
        const initialDosage = item.dosage || `1 ${unit}`
        const initialRoute = item.route || 'ORAL'
        const freq = Number(item.frequency) || 2
        const days = Number(item.durationDays) || 5
        const initialQty = item.quantity > 1 ? item.quantity : freq * days

        return {
          ...item,
          medicineId: value,
          dosage: initialDosage,
          route: initialRoute,
          quantity: initialQty,
        }
      }

      return { ...item, [field]: value }
    })

    setItems(nextItems)
    if (field === 'medicineId') {
      setConfirmedOverrides([])
      performInteractionCheck(nextItems).catch(() => {})
    }
  }

  const handleRemoveItem = (clientId) => {
    if (items.length <= 1) {
      message.warning('Đơn thuốc phải có ít nhất 1 loại thuốc. Không thể xóa toàn bộ thuốc.')
      return
    }
    const nextItems = items.filter((entry) => entry.clientId !== clientId)
    setItems(nextItems)
    setConfirmedOverrides([])
    performInteractionCheck(nextItems).catch(() => {})
  }

  const validateForm = () => {
    if (!medicalRecordId) return 'Thiếu medicalRecordId.'
    if (!diagnoses.length) return 'Bệnh án phải có chẩn đoán trước khi kê đơn.'
    if (!isDoctor) return 'Chỉ bác sĩ mới được kê đơn.'
    if (!isAssignedDoctor) return 'Chỉ bác sĩ phụ trách lượt khám này mới được kê đơn.'
    if (prescriptionBlockReason) return prescriptionBlockReason
    if (recordLocked) return 'Bệnh án đã khóa nên không thể kê hoặc điều chỉnh đơn.'
    if (!items.length) return 'Đơn thuốc phải có ít nhất một thuốc.'

    const seen = new Set()
    for (let index = 0; index < items.length; index += 1) {
      const item = items[index]
      if (!item.medicineId) return `Dòng ${index + 1}: chưa chọn thuốc.`
      if (seen.has(item.medicineId)) return `Dòng ${index + 1}: thuốc bị trùng trong đơn.`
      seen.add(item.medicineId)

      if (!item.dosage || !item.dosage.trim()) {
        return `Dòng ${index + 1}: chưa nhập liều dùng một lần (ví dụ: 1 viên/lần, 5ml/lần).`
      }
      if (item.dosage.trim().length > 100) {
        return `Dòng ${index + 1}: liều dùng không được vượt quá 100 ký tự.`
      }

      const freqNum = Number(item.frequency)
      if (
        item.frequency === '' ||
        item.frequency == null ||
        isNaN(freqNum) ||
        !Number.isInteger(freqNum) ||
        freqNum <= 0
      ) {
        return `Dòng ${index + 1}: số lần dùng mỗi ngày (tần suất) phải là số nguyên dương lớn hơn 0.`
      }

      if (!item.route) {
        return `Dòng ${index + 1}: chưa chọn cách dùng thuốc (uống, bôi ngoài da, tiêm...).`
      }

      const durationNum = Number(item.durationDays)
      if (
        item.durationDays === '' ||
        item.durationDays == null ||
        isNaN(durationNum) ||
        !Number.isInteger(durationNum) ||
        durationNum <= 0
      ) {
        return `Dòng ${index + 1}: số ngày dùng thuốc phải là số nguyên dương lớn hơn 0.`
      }

      const qtyNum = Number(item.quantity)
      if (
        item.quantity === '' ||
        item.quantity == null ||
        isNaN(qtyNum) ||
        !Number.isInteger(qtyNum) ||
        qtyNum <= 0
      ) {
        return `Dòng ${index + 1}: tổng số lượng thuốc phải là số nguyên dương lớn hơn 0.`
      }

      const itemStockRes = validateItemStock(item, selectedMedicineMap)
      if (!itemStockRes.isValid) {
        return `Dòng ${index + 1}: ${itemStockRes.error}`
      }
    }

    if (editingPrescription && !changeReason.trim()) {
      return 'Bác sĩ bắt buộc phải nhập lý do điều chỉnh đơn thuốc (theo quy chế lưu vết bệnh án).'
    }
    return null
  }

  const formatItems = () =>
    items.map((item) => ({
      medicineId: item.medicineId,
      dosage: item.dosage.trim(),
      frequency: Number(item.frequency),
      route: item.route,
      durationDays: Number(item.durationDays),
      quantity: Number(item.quantity),
      instructions: (item.instructions || '').trim(),
    }))

  const executeSavePrescription = async (overrides = []) => {
    setSaving(true)
    try {
      const activeQueueItem = await requireLiveInProgressQueue(
        editingPrescription ? 'điều chỉnh đơn thuốc' : 'tạo đơn thuốc',
      )

      let freshMeds = []
      let freshStocks = []
      try {
        const [medRes, stockRes] = await Promise.allSettled([
          pharmacyApi.medicines({ active: true }),
          pharmacyApi.stocks({ active: true }),
        ])
        if (medRes.status === 'fulfilled') {
          freshMeds = unwrapCollection(medRes.value.data)
        }
        if (stockRes.status === 'fulfilled') {
          freshStocks = unwrapCollection(stockRes.value.data)
        }
      } catch {
        freshMeds = medicines
      }
      if (!freshMeds || freshMeds.length === 0) {
        freshMeds = medicines
      }

      const freshStockMap = new Map((freshStocks || []).map((s) => [String(s.medicineId || s.id), s]))

      const normalizedFreshMeds = freshMeds.map((m) => {
        const stockItem = freshStockMap.get(String(m.id))
        const avail = stockItem
          ? (stockItem.eligibleStockQuantity ?? stockItem.stockQuantity ?? 0)
          : getAvailableStock(m)

        return {
          ...m,
          medicineName: m.medicineName || m.name || 'Thuốc',
          stockQuantity: avail,
          availableStock: avail,
          eligibleStockQuantity: avail,
          unit: m.unit || 'viên',
        }
      })
      setMedicines(normalizedFreshMeds)

      const liveStockValidation = validatePrescriptionStock(items, normalizedFreshMeds)
      if (!liveStockValidation.isValid) {
        Modal.error({
          title: 'Không thể tạo/lưu đơn thuốc do tồn kho thay đổi',
          content: (
            <div>
              <Paragraph style={{ color: '#dc2626', marginBottom: 8 }}>
                Dữ liệu tồn kho khả dụng mới nhất của hệ thống không đủ cho đơn thuốc này:
              </Paragraph>
              <ul style={{ paddingLeft: 20, color: '#b91c1c', marginBottom: 8 }}>
                {liveStockValidation.errors.map((err, idx) => (
                  <li key={idx}><strong>{err}</strong></li>
                ))}
              </ul>
              <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                Vui lòng chọn lại thuốc khác hoặc giảm số lượng kê phù hợp với tồn khả dụng hiện tại.
              </Paragraph>
            </div>
          ),
        })
        return
      }

      const payload = {
        note: note.trim(),
        items: formatItems(),
        ...(overrides && overrides.length > 0
          ? {
              interactionOverrides: overrides.map((override) => ({
                ruleId: override.ruleId,
                overrideReason: override.overrideReason,
              })),
            }
          : {}),
      }

      let response
      if (editingPrescription) {
        response = await pharmacyApi.updatePrescription(editingPrescription.id, {
          ...payload,
          changeReason: changeReason.trim(),
        })
      } else {
        response = await pharmacyApi.createPrescription({
          ...payload,
          medicalRecordId,
        })
      }

      const prescriptionCode = response.data?.prescriptionCode || editingPrescription?.prescriptionCode || ''
      
      const pData = response.data || {}
      const savedObj = {
        id: pData.id || `presc-${Date.now()}`,
        prescriptionCode: prescriptionCode || `DT-${Date.now().toString().slice(-6)}`,
        visitId: encounter?.visitId || encounter?.visit?.id || activeQueueItem?.visitId || medicalRecordId,
        visitCode: encounter?.visitCode || encounter?.visit?.visitCode || activeQueueItem?.visitCode || encounter?.queueItem?.visitCode,
        patientId: encounter?.patientId || encounter?.patient?.id || activeQueueItem?.patientId,
        patientCode: encounter?.patientCode || encounter?.patient?.patientCode || activeQueueItem?.patientCode,
        patientName: encounter?.patientName || encounter?.patient?.fullName || activeQueueItem?.patientName,
        doctorName: encounter?.doctor?.fullName || record?.doctorName || currentUser?.fullName,
        medicalRecordId: medicalRecordId,
        status: pData.status || 'PENDING_DISPENSE',
        items: items.map((i) => ({
          medicineId: i.medicineId,
          medicineName: i.medicineName || i.name,
          quantity: Number(i.quantity),
          dosage: i.dosage,
          frequency: Number(i.frequency),
          route: i.route,
          durationDays: Number(i.durationDays),
          unitPrice: i.unitPrice || i.price,
        })),
        createdAt: new Date().toISOString(),
      }
      saveStoredPrescription(savedObj)

      message.success(
        editingPrescription
          ? `Đã cập nhật và lưu vết điều chỉnh đơn thuốc ${prescriptionCode} thành công (Mã đơn cố định).`
          : `Đã cấp mã đơn thuốc điện tử ${prescriptionCode} thành công.`,
      )

      if (!editingPrescription) {
        setJustIssuedPrescription({
          ...savedObj,
          ...pData,
          prescriptionCode: prescriptionCode || savedObj.prescriptionCode,
          items: pData.items || savedObj.items,
        })
        setIssuedPrescriptionModalOpen(true)
      }

      setEditingPrescription(null)
      setItems([createEmptyItem()])
      setNote('')
      setChangeReason('')
      setDetectedInteractions([])
      setConfirmedOverrides([])
      await loadData()
      setActiveTab('history')
    } catch (error) {
      message.error(getApiMessage(error, 'Không thể lưu đơn thuốc.'))
    } finally {
      setSaving(false)
    }
  }

  const handleSaveClick = async () => {
    const validationError = validateForm()
    if (validationError) {
      message.error(validationError)
      return
    }

    try {
      const warnings = await performInteractionCheck(items)
      if (warnings.length > 0 && !areAllInteractionsHandled(warnings, confirmedOverrides)) {
        setInteractionModalOpen(true)
        return
      }

      const checkStatus = canSubmitPrescription({
        canPrescribe,
        saving,
        checkingInteractions,
        interactionApiError,
        detectedInteractions: warnings,
        confirmedOverrides,
      })

      if (!checkStatus.allowed) {
        message.error(checkStatus.reason)
        return
      }

      if (detectedAllergyConflicts.length > 0) {
        Modal.confirm({
          title: 'CẢNH BÁO NGUY CƠ DỊ ỨNG THUỐC / SỐC PHẢN VỆ',
          icon: <FireOutlined style={{ color: '#ef4444', fontSize: 22 }} />,
          width: 580,
          content: (
            <div style={{ marginTop: 8 }}>
              <Paragraph type="danger" strong style={{ fontSize: 14 }}>
                Phát hiện {detectedAllergyConflicts.length} loại thuốc trong đơn trùng với tiền sử dị ứng đã ghi nhận của bệnh nhân:
              </Paragraph>
              <ul style={{ paddingLeft: 20, marginBottom: 12 }}>
                {detectedAllergyConflicts.map((c, i) => (
                  <li key={i} style={{ marginBottom: 6 }}>
                    <Text strong>{c.medicine?.medicineName || c.medicine?.name}:</Text> Dị ứng với{' '}
                    <Tag color={c.severityMeta?.color}>{c.matchedAllergy?.allergenName}</Tag> - Mức độ:{' '}
                    <Text type="danger" strong>{c.severityMeta?.label}</Text>
                    {c.matchedAllergy?.reaction ? ` (${c.matchedAllergy.reaction})` : ''}
                  </li>
                ))}
              </ul>
              <Paragraph style={{ color: '#475569', fontSize: 13 }}>
                Kê thuốc bệnh nhân đã có tiền sử dị ứng có thể dẫn đến phản vệ nguy hiểm tính mạng. Bác sĩ có chắc chắn đã kiểm tra kỹ và muốn tiếp tục lưu đơn thuốc này?
              </Paragraph>
            </div>
          ),
          okText: 'Xác nhận tiếp tục kê',
          okType: 'danger',
          cancelText: 'Hủy để đổi thuốc khác',
          onOk: async () => {
            await executeSavePrescription(confirmedOverrides)
          },
        })
        return
      }

      await executeSavePrescription(confirmedOverrides)
    } catch (error) {
      message.error(getApiMessage(error, 'Không thể tạo đơn thuốc.'))
    }
  }

  const handleConfirmInteractionOverrides = async (overrides) => {
    setConfirmedOverrides(overrides)
    setInteractionModalOpen(false)
    await executeSavePrescription(overrides)
  }

  const startEditPrescription = (prescription) => {
    if (prescriptionBlockReason) {
      message.error(prescriptionBlockReason)
      return
    }
    if (prescription.status !== 'PENDING_DISPENSE') {
      message.warning('Chỉ đơn thuốc đang ở trạng thái chờ cấp phát (PENDING_DISPENSE) mới được điều chỉnh.')
      return
    }
    if (!isAssignedDoctor) {
      message.error('Chỉ bác sĩ phụ trách lượt khám này mới có quyền điều chỉnh đơn thuốc.')
      return
    }
    if (recordLocked) {
      message.error('Bệnh án đã khóa, không thể điều chỉnh đơn thuốc.')
      return
    }

    setEditingPrescription(prescription)
    setNote(prescription.note || '')
    setChangeReason('')
    setItems(
      (prescription.items || []).map((item) => ({
        clientId: `prescription-item-${++localItemSequence}`,
        medicineId: item.medicineId,
        quantity: Number(item.quantity),
        dosage: item.dosage || '',
        frequency: item.frequency != null ? Number(item.frequency) : 2,
        route: item.route || 'ORAL',
        durationDays: Number(item.durationDays) || 5,
        instructions: item.instructions || '',
        isOriginal: true,
      })),
    )
    setActiveTab('prescribe')
    message.info(`Đang mở chế độ điều chỉnh đơn thuốc ${prescription.prescriptionCode}.`)
  }

  const cancelEditMode = () => {
    setEditingPrescription(null)
    setItems([createEmptyItem()])
    setChangeReason('')
    setNote('')
    setDetectedInteractions([])
    setConfirmedOverrides([])
  }

  const handleCancelPrescription = (prescription) => {
    if (prescription.status !== 'PENDING_DISPENSE') {
      message.warning('Chỉ có thể hủy đơn thuốc khi đang chờ cấp phát.')
      return
    }

    Modal.confirm({
      title: `Hủy đơn thuốc ${prescription.prescriptionCode}?`,
      icon: <ExclamationCircleOutlined style={{ color: '#ef4444' }} />,
      content: 'Đơn thuốc sẽ được chuyển sang trạng thái CANCELLED. Hành động này không thể hoàn tác.',
      okText: 'Xác nhận hủy',
      okButtonProps: { danger: true },
      cancelText: 'Bỏ qua',
      onOk: async () => {
        setCancelling(true)
        try {
          await requireLiveInProgressQueue('hủy đơn thuốc')
          await pharmacyApi.cancelPrescription(prescription.id)
          message.success(`Đã hủy đơn thuốc ${prescription.prescriptionCode}.`)
          await loadData()
        } catch (error) {
          message.error(getApiMessage(error, 'Không thể hủy đơn thuốc.'))
        } finally {
          setCancelling(false)
        }
      },
    })
  }

  const openDetailModal = (prescription) => {
    setSelectedPrescriptionForDetail(prescription)
    setDetailModalOpen(true)
  }

  const finalizeEncounter = () => {
    const finalizeBlockReason = getQueueInProgressBlockReason(
      encounter?.queueItem,
      'khóa bệnh án và hoàn tất lượt khám',
    )
    if (finalizeBlockReason) {
      message.error(finalizeBlockReason)
      return
    }

    // Bắt buộc kiểm tra điều kiện ký số theo quy định y tế (Thông tư 46/2018/TT-BYT) và logic backend
    if (!isRecordSigned) {
      Modal.warning({
        title: 'Bệnh án chưa được ký số — Không thể khóa hoàn tất',
        icon: <ExclamationCircleOutlined style={{ color: '#ea580c' }} />,
        width: 520,
        content: (
          <div style={{ marginTop: 12 }}>
            <Paragraph style={{ marginBottom: 8, fontSize: 13, lineHeight: 1.6 }}>
              Theo quy chế hồ sơ bệnh án điện tử (<strong>Thông tư 46/2018/TT-BYT</strong>), hồ sơ bệnh án bắt buộc phải được Bác sĩ kiểm tra lâm sàng và <strong>Ký số xác nhận</strong> trước khi Khóa hồ sơ và Hoàn tất ca khám.
            </Paragraph>
            <Paragraph type="secondary" style={{ marginBottom: 0, fontSize: 13 }}>
              Trạng thái hiện tại: <strong style={{ color: '#ea580c' }}>Bản nháp (Chưa ký số)</strong>. Vui lòng quay lại màn hình Khám bệnh để thực hiện Ký số bệnh án.
            </Paragraph>
          </div>
        ),
        okText: 'Quay lại Khám bệnh để ký số',
        okButtonProps: {
          type: 'primary',
          style: { background: '#1d4ed8', borderColor: '#1d4ed8' },
        },
        closable: true,
        maskClosable: true,
        onOk: () => {
          if (targetVisitId) {
            navigate(`/medical-records/visits/${targetVisitId}`)
          } else {
            navigate('/appointments')
          }
        },
      })
      return
    }

    const hasPrescription = prescriptions.some((p) => p.status === 'PENDING_DISPENSE' || p.status === 'DISPENSED')

    Modal.confirm({
      title: 'Khóa bệnh án và hoàn tất lượt khám?',
      content: hasPrescription
        ? 'Bệnh án đã được ký số hợp lệ. Sau khi khóa, bác sĩ không thể kê thêm hoặc điều chỉnh đơn thuốc. Lượt khám sẽ chuyển sang trạng thái Hoàn tất.'
        : 'Lượt khám này chưa có đơn thuốc. Bệnh án đã được ký số, bạn có muốn khóa bệnh án và hoàn tất ca khám luôn không?',
      okText: 'Khóa & hoàn tất',
      okType: 'danger',
      cancelText: 'Chưa hoàn tất',
      onOk: async () => {
        setFinalizing(true)
        try {
          const liveQueueItem = await requireLiveInProgressQueue(
            'khóa bệnh án và hoàn tất lượt khám',
          )
          if (!recordLocked) {
            const lockResponse = await medicalRecordApi.lock(medicalRecordId)
            setRecord((current) => ({ ...current, ...lockResponse.data, status: 'LOCKED' }))
          }

          if (liveQueueItem?.id) {
            try {
              const completeResponse = await queueApi.complete(liveQueueItem.id)
              const completedQueueItem = completeResponse?.data
              if (completedQueueItem?.id) {
                setEncounter((current) =>
                  current
                    ? { ...current, queueItem: { ...current.queueItem, ...completedQueueItem } }
                    : current,
                )
              }
            } catch (queueErr) {
              console.warn('Lỗi hoàn tất queue:', queueErr)
            }
          }

          message.success('Đã khóa bệnh án và hoàn tất lượt khám thành công.')
          navigate('/appointments')
        } catch (error) {
          console.error('Lỗi khi khóa bệnh án và hoàn tất ca khám:', error)
          const errorMsg = getApiMessage(error, 'Không thể khóa bệnh án. Vui lòng kiểm tra lại trạng thái ký số và quyền hạn.')
          message.error(errorMsg)
        } finally {
          setFinalizing(false)
        }
      },
    })
  }

  const handleSignSuccess = async (signedData) => {
    const nextStatus = signedData?.status || 'SIGNED'
    setRecord((current) => ({
      ...current,
      ...signedData,
      status: nextStatus,
    }))

    Modal.success({
      title: 'Ký số bệnh án thành công!',
      icon: <CheckCircleOutlined style={{ color: '#16a34a' }} />,
      content: (
        <div>
          <Paragraph style={{ marginBottom: 8 }}>
            Hồ sơ bệnh án đã được Bác sĩ ký số xác nhận hợp lệ (trạng thái: <strong style={{ color: '#0284c7' }}>ĐÃ KÝ SỐ</strong>).
          </Paragraph>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Bác sĩ có thể tiếp tục rà soát đơn thuốc và bấm nút <strong>"Khóa bệnh án & hoàn tất khám"</strong> để khóa hồ sơ và kết thúc ca khám.
          </Paragraph>
        </div>
      ),
      okText: 'Tiếp tục rà soát đơn thuốc',
    })
  }

  const handlePrintPrescription = (prescription) => {
    if (!canPrintPrescription) {
      message.error('Bạn không có quyền in đơn thuốc (Yêu cầu quyền PRESCRIPTION_PRINT).')
      return
    }
    if (!prescription) return
    setSelectedPrescriptionForPrint(prescription)
    setPrintModalOpen(true)
  }

  const handleSendPrescriptionToInterconnection = async (prescription) => {
    if (!prescription?.id || sendingInterconnectionId) return
    setSendingInterconnectionId(prescription.id)
    try {
      const response = await pharmacyApi.sendToInterconnection(prescription.id)
      const data = response?.data || {}
      setJustIssuedPrescription((prev) => {
        if (prev && String(prev.id) === String(prescription.id)) {
          return {
            ...prev,
            interconnectionStatus: data.status,
            interconnectionReceiptCode: data.receiptCode,
            lastInterconnectionError: data.failureReason,
            lastInterconnectionAt: data.completedAt || new Date().toISOString(),
          }
        }
        return prev
      })
      if (data.status === 'SUCCESS') {
        message.success(
          `Đã gửi đơn thuốc ${prescription.prescriptionCode || ''} lên Cổng liên thông Quốc gia thành công! Mã biên nhận: ${data.receiptCode}`,
        )
      } else {
        message.error(
          `Gửi liên thông đơn ${prescription.prescriptionCode || ''} thất bại: ${data.failureReason || 'Cổng liên thông từ chối tiếp nhận'}`,
        )
      }
      await loadData()
    } catch (error) {
      message.error(getApiMessage(error, 'Không thể gửi đơn thuốc lên Cổng liên thông Quốc gia.'))
    } finally {
      setSendingInterconnectionId(null)
    }
  }

  const historyColumns = [
    {
      title: 'Mã đơn điện tử',
      dataIndex: 'prescriptionCode',
      key: 'prescriptionCode',
      width: 180,
      render: (value, row) => {
        const displayCode = formatPrescriptionCode(value || row.id)
        const isRx = isStandardRxCode(displayCode)
        const isModified = row.updatedAt && row.updatedAt !== row.prescribedAt

        const handleCopy = (e) => {
          e?.stopPropagation()
          if (displayCode && displayCode !== '—') {
            navigator.clipboard.writeText(displayCode)
            message.success(`Đã sao chép mã đơn điện tử: ${displayCode}`)
          }
        }

        return (
          <Space direction="vertical" size={3}>
            <Space size={4} align="center">
              <Tooltip title={isRx ? 'Mã đơn thuốc điện tử chuẩn liên thông quốc gia (Định danh duy nhất không đổi)' : 'Mã đơn thuốc tra cứu'}>
                <Tag
                  color="blue"
                  style={{
                    fontSize: 13,
                    fontWeight: 700,
                    cursor: 'pointer',
                    borderRadius: 4,
                    padding: '2px 8px',
                    letterSpacing: 0.5,
                    border: '1px solid #93c5fd',
                    backgroundColor: '#eff6ff',
                    color: '#1d4ed8',
                  }}
                  onClick={() => openDetailModal(row)}
                >
                  <BarcodeOutlined style={{ marginRight: 4 }} />
                  {displayCode}
                </Tag>
              </Tooltip>
              {displayCode && displayCode !== '—' && (
                <Tooltip title="Sao chép mã đơn">
                  <Button
                    type="text"
                    size="small"
                    icon={<CopyOutlined style={{ color: '#2563eb', fontSize: 13 }} />}
                    onClick={handleCopy}
                    style={{ padding: '0 4px', height: 22, width: 22 }}
                  />
                </Tooltip>
              )}
            </Space>

            <Space size={4} wrap>
              <Tag color="cyan" style={{ fontSize: 10, padding: '0 4px', margin: 0 }}>
                Định danh cố định
              </Tag>
              {isModified && (
                <Tag color="purple" style={{ fontSize: 10, padding: '0 4px', margin: 0 }}>
                  <SyncOutlined spin={false} /> Đã sửa (giữ mã)
                </Tag>
              )}
            </Space>
          </Space>
        )
      },
    },
    {
      title: 'Danh sách thuốc trong đơn',
      dataIndex: 'items',
      key: 'items',
      render: (value = []) => (
        <div>
          {value.slice(0, 3).map((item, idx) => (
            <div key={idx} style={{ marginBottom: 2 }}>
              <Text strong>{item.medicineName}</Text> <Text type="secondary">× {item.quantity} ({item.dosage || 'Theo chỉ định'})</Text>
            </div>
          ))}
          {value.length > 3 && (
            <Text type="secondary" style={{ fontSize: 12, fontStyle: 'italic' }}>
              +{value.length - 3} thuốc khác...
            </Text>
          )}
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 150,
      render: (value) => {
        if (value === 'PENDING_DISPENSE') {
          return (
            <Tag color="orange" icon={<ClockCircleOutlined />}>
              Chờ cấp phát
            </Tag>
          )
        }
        if (value === 'DISPENSED') {
          return (
            <Tag color="green" icon={<CheckCircleOutlined />}>
              Đã cấp phát
            </Tag>
          )
        }
        if (value === 'CANCELLED') {
          return (
            <Tag color="default" icon={<CloseCircleOutlined />}>
              Đã hủy
            </Tag>
          )
        }
        return <Tag>{value}</Tag>
      },
    },
    {
      title: 'Bác sĩ kê / Thời gian',
      key: 'prescribedInfo',
      width: 180,
      render: (_, row) => (
        <div>
          <div><Text strong>{row.doctorName || '—'}</Text></div>
          <div style={{ fontSize: 12, color: '#6b7280' }}>
            {row.prescribedAt ? dayjs(row.prescribedAt).format('HH:mm DD/MM/YYYY') : '—'}
          </div>
        </div>
      ),
    },
    {
      title: 'Liên thông Quốc gia',
      key: 'interconnection',
      width: 175,
      render: (_, row) => {
        const status = row.interconnectionStatus || (row.receiptCode ? 'SUCCESS' : 'NOT_SENT')
        const receiptCode = row.interconnectionReceiptCode || row.receiptCode || ''
        const error = row.lastInterconnectionError || row.failureReason || ''
        const isSending = sendingInterconnectionId === row.id
        const isSuccess = status === 'SUCCESS'
        const isFailed = status === 'FAILED'

        if (isSuccess) {
          return (
            <Space direction="vertical" size={2}>
              <Tag color="success" icon={<CheckCircleOutlined />}>
                Đã liên thông
              </Tag>
              {receiptCode && (
                <Tooltip title="Mã biên nhận từ Cổng liên thông Quốc gia">
                  <Text code style={{ fontSize: 11, color: '#15803d', fontWeight: 600 }}>
                    {receiptCode}
                  </Text>
                </Tooltip>
              )}
            </Space>
          )
        }

        if (isFailed) {
          return (
            <Space direction="vertical" size={2}>
              <Tooltip title={error ? `Lý do lỗi: ${error}` : 'Gửi liên thông không thành công'}>
                <Tag color="error" icon={<CloseCircleOutlined />} style={{ cursor: 'pointer' }}>
                  Liên thông lỗi
                </Tag>
              </Tooltip>
              {canPrescribe && (
                <Button
                  size="small"
                  type="link"
                  icon={<SyncOutlined spin={isSending} />}
                  loading={isSending}
                  onClick={() => handleSendPrescriptionToInterconnection(row)}
                  style={{ padding: 0, height: 'auto', fontSize: 11, color: '#dc2626' }}
                >
                  Gửi lại
                </Button>
              )}
            </Space>
          )
        }

        return (
          <Space direction="vertical" size={2}>
            <Tag color="default" icon={<CloudServerOutlined />}>
              Chưa liên thông
            </Tag>
            {canPrescribe && row.status !== 'CANCELLED' && (
              <Button
                size="small"
                type="link"
                icon={<CloudUploadOutlined />}
                loading={isSending}
                onClick={() => handleSendPrescriptionToInterconnection(row)}
                style={{ padding: 0, height: 'auto', fontSize: 11, color: '#0284c7' }}
              >
                Gửi liên thông
              </Button>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 90,
      align: 'center',
      render: (_, prescription) => {
        const isPending = prescription.status === 'PENDING_DISPENSE'
        const isPrintable = Boolean(
          canPrintPrescription &&
          prescription.id &&
          prescription.prescriptionCode &&
          (prescription.status === 'PENDING_DISPENSE' || prescription.status === 'DISPENSED')
        )
        const canEditThis = canPrescribe && isPending
        const isInterconnected = prescription.interconnectionStatus === 'SUCCESS'

        const menuItems = [
          {
            key: 'detail',
            icon: <EyeOutlined />,
            label: 'Xem chi tiết đơn thuốc',
            onClick: () => openDetailModal(prescription),
          },
          canPrescribe && prescription.status !== 'CANCELLED' && {
            key: 'interconnection',
            icon: <CloudUploadOutlined style={{ color: '#0284c7' }} />,
            label: isInterconnected ? 'Xem trạng thái liên thông' : 'Gửi lên Cổng liên thông',
            onClick: () => {
              if (!isInterconnected) {
                handleSendPrescriptionToInterconnection(prescription)
              } else {
                openDetailModal(prescription)
              }
            },
          },
          isPrintable && {
            key: 'print',
            icon: <PrinterOutlined />,
            label: 'In đơn thuốc',
            onClick: () => handlePrintPrescription(prescription),
          },
          canEditThis && {
            key: 'edit',
            icon: <EditOutlined />,
            label: 'Điều chỉnh đơn thuốc',
            onClick: () => startEditPrescription(prescription),
          },
          isPending && canPrescribe && {
            type: 'divider',
          },
          isPending && canPrescribe && {
            key: 'cancel',
            icon: <StopOutlined />,
            danger: true,
            label: 'Hủy đơn thuốc này',
            onClick: () => handleCancelPrescription(prescription),
          },
        ].filter(Boolean)

        return (
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <Button size="small" icon={<EllipsisOutlined />} title="Thao tác" />
          </Dropdown>
        )
      },
    },
  ]

  if (!medicalRecordId) {
    return (
      <Card>
        <Alert
          type="warning"
          showIcon
          message="Chưa có bệnh án để kê đơn"
          description="Màn kê đơn chỉ mở từ một lượt khám đã lưu và phải có mã bệnh án trên đường dẫn."
          action={<Button onClick={() => navigate('/appointments')}>Về danh sách lượt khám</Button>}
        />
      </Card>
    )
  }

  const selectedMedicineMap = new Map(medicines.map((m) => [String(m.id), m]))

  const queueStatusLabel = {
    WAITING: 'Chờ khám',
    IN_PROGRESS: 'Đang khám',
    WAITING_FOR_RESULT: 'Chờ kết quả CĐLS',
    COMPLETED: 'Đã hoàn tất',
    SKIPPED: 'Đã bỏ qua',
  }[encounter?.queueItem?.status] || encounter?.queueItem?.status || 'Chưa xác định'

  const recordStatusLabel = recordLocked
    ? 'Đã khóa hồ sơ'
    : (record?.status === 'SIGNED'
      ? 'Đã ký số (Chờ khóa)'
      : (record?.status === 'DRAFT' ? 'Chưa ký số (Bản nháp)' : (record?.status ? `Chưa ký số (${record.status})` : 'Chưa ký số')))

  return (
    <div style={{ paddingBottom: 40 }}>
      {loadError && (
        <Alert
          type="warning"
          showIcon
          closable
          onClose={() => setLoadError(null)}
          message="Thông báo đồng bộ"
          description={loadError.message}
          style={{ marginBottom: 16 }}
        />
      )}
      {/* Thanh điều hướng quay lại gọn gàng */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
        <Button
          type="text"
          size="small"
          icon={<ArrowLeftOutlined />}
          onClick={() => {
            if (targetVisitId) {
              navigate(`/medical-records/visits/${targetVisitId}`)
            } else {
              navigate('/appointments')
            }
          }}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 4,
            borderRadius: 6,
            background: '#F1F5F9',
            color: '#1E293B',
            fontWeight: 500,
            fontSize: 13,
            padding: '2px 10px',
          }}
        >
          Quay lại Khám bệnh
        </Button>
        <span style={{ color: '#CBD5E1' }}>•</span>
        <Button
          type="text"
          size="small"
          icon={<UnorderedListOutlined />}
          onClick={() => navigate('/appointments')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 4,
            borderRadius: 6,
            color: '#64748B',
            fontSize: 13,
            padding: '2px 8px',
          }}
        >
          Hàng đợi khám
        </Button>
      </div>

      <div
        className="page-header"
        style={{
          marginBottom: 16,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 16,
          flexWrap: 'wrap',
        }}
      >
        <div>
          <Title level={3} style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <MedicineBoxOutlined style={{ color: '#2563eb' }} />
            {editingPrescription ? (
              <span>
                Điều chỉnh Đơn thuốc <Text code style={{ color: '#2563eb', fontSize: 20 }}>{editingPrescription.prescriptionCode}</Text>
              </span>
            ) : (
              'Kê đơn thuốc theo bệnh án'
            )}
          </Title>
          <Text type="secondary" style={{ fontSize: 13 }}>
            {editingPrescription
              ? 'Sửa đổi liều dùng, số lượng hoặc thêm/bớt thuốc khi đơn đang ở trạng thái chờ cấp phát.'
              : 'Hồ sơ gắn liền với bệnh án hiện tại, đảm bảo an toàn thông tin điều trị.'}
          </Text>
        </div>
        <Space wrap size="middle" className="prescription-header-actions">
          {editingPrescription && (
            <Button icon={<RollbackOutlined />} onClick={cancelEditMode}>
              Hủy điều chỉnh
            </Button>
          )}
          {canPrescribe && (
            <Tooltip title={!canSubmit ? submitStatus.reason : ''}>
              <Button
                type="primary"
                loading={saving || checkingInteractions}
                disabled={!canSubmit}
                icon={<CheckCircleOutlined />}
                onClick={handleSaveClick}
                style={{ fontWeight: 600 }}
              >
                {editingPrescription ? 'Lưu điều chỉnh đơn thuốc' : 'Tạo đơn thuốc'}
              </Button>
            </Tooltip>
          )}
          {isDoctor && !recordLocked && !editingPrescription && (
            <>
              {/* Nút 1: Ký số bệnh án */}
              <Tooltip title={isRecordSigned ? 'Bệnh án đã được ký số hợp lệ (Bấm để xem chứng thư)' : 'Mở hộp thoại ký số xác nhận bệnh án điện tử'}>
                <Button
                  type={!isRecordSigned ? 'primary' : 'default'}
                  icon={isRecordSigned ? <CheckCircleOutlined style={{ color: '#16a34a' }} /> : <EditOutlined />}
                  loading={finalizing}
                  disabled={Boolean(prescriptionBlockReason)}
                  onClick={() => setSignModalOpen(true)}
                  style={{
                    fontWeight: 700,
                    ...(isRecordSigned
                      ? { color: '#15803d', borderColor: '#86efac', background: '#f0fdf4' }
                      : { background: '#0284c7', borderColor: '#0284c7', boxShadow: '0 2px 6px rgba(2, 132, 199, 0.3)' }
                    ),
                  }}
                >
                  {isRecordSigned ? 'Đã ký số' : 'Ký số bệnh án'}
                </Button>
              </Tooltip>

              {/* Nút 2: Khóa bệnh án */}
              <Tooltip
                title={
                  !isRecordSigned
                    ? 'Bệnh án cần được Ký số xác nhận trước khi Khóa'
                    : 'Khóa hồ sơ bệnh án và hoàn tất lượt khám'
                }
              >
                <span>
                  <Button
                    danger={isRecordSigned}
                    type="primary"
                    icon={<LockOutlined />}
                    loading={finalizing}
                    disabled={Boolean(prescriptionBlockReason) || !isRecordSigned}
                    onClick={finalizeEncounter}
                    style={{
                      fontWeight: 700,
                      ...(isRecordSigned
                        ? { background: '#dc2626', borderColor: '#dc2626', boxShadow: '0 2px 6px rgba(220, 38, 38, 0.3)' }
                        : {}
                      ),
                    }}
                  >
                    Khóa bệnh án
                  </Button>
                </span>
              </Tooltip>
            </>
          )}
        </Space>
      </div>

      <Card style={{ marginBottom: 16, borderRadius: 8 }}>
        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, lg: 4 }}>
          <Descriptions.Item label="Mã bệnh án">
            {medicalRecordId ? (
              <Space size={4} align="center">
                <Tag
                  color="geekblue"
                  style={{
                    fontFamily: 'monospace',
                    fontWeight: 700,
                    fontSize: 12,
                    padding: '1px 8px',
                    borderRadius: 4,
                    margin: 0,
                  }}
                >
                  {formatRecordCode(medicalRecordId)}
                </Tag>
                <Tooltip title={`Mã UUID đầy đủ: ${medicalRecordId}`}>
                  <Typography.Text
                    copyable={{
                      text: String(medicalRecordId),
                      tooltips: ['Sao chép mã UUID', 'Đã sao chép!'],
                    }}
                    type="secondary"
                    style={{ fontSize: 11 }}
                  />
                </Tooltip>
              </Space>
            ) : (
              '—'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Mã lượt khám">
            <Space size={4} align="center">
              <Tag
                color="blue"
                style={{
                  fontFamily: 'monospace',
                  fontWeight: 700,
                  fontSize: 12,
                  padding: '1px 8px',
                  borderRadius: 4,
                  margin: 0,
                }}
              >
                {formatVisitCode(encounter?.visit?.visitCode, record?.visitId)}
              </Tag>
              {(encounter?.visit?.visitCode || record?.visitId) && (
                <Tooltip title={`Mã lượt khám đầy đủ: ${encounter?.visit?.visitCode || record?.visitId}`}>
                  <Typography.Text
                    copyable={{
                      text: String(encounter?.visit?.visitCode || record?.visitId),
                      tooltips: ['Sao chép mã', 'Đã sao chép!'],
                    }}
                    type="secondary"
                    style={{ fontSize: 11 }}
                  />
                </Tooltip>
              )}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="Bệnh nhân">
            <Text strong>{encounter?.patient?.fullName || record?.patientName || '—'}</Text> ({encounter?.patient?.patientCode || record?.patientCode || 'BN'})
          </Descriptions.Item>
          <Descriptions.Item label="Bác sĩ phụ trách">{encounter?.doctor?.fullName || record?.doctorName || '—'}</Descriptions.Item>
          <Descriptions.Item label="Chẩn đoán chính" span={2}>
            <Text strong style={{ color: '#1e40af' }}>{diagnosisSummary}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Hàng đợi / STT">
            {encounter?.queueItem ? (
              <Space size={4} align="center">
                <Tag color="blue" style={{ fontWeight: 700, borderRadius: 10 }}>
                  STT #{encounter.queueItem.queueNumber || 1}
                </Tag>
                {encounter.queueItem.id && (
                  <Tooltip title={`Mã hàng đợi đầy đủ: ${encounter.queueItem.id}`}>
                    <Typography.Text
                      type="secondary"
                      copyable={{
                        text: String(encounter.queueItem.id),
                        tooltips: ['Sao chép mã', 'Đã sao chép!'],
                      }}
                      style={{ fontSize: 11, fontFamily: 'monospace' }}
                    >
                      #{String(encounter.queueItem.id).slice(0, 8)}
                    </Typography.Text>
                  </Tooltip>
                )}
              </Space>
            ) : (
              '—'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái lượt khám">
            <Tag color={encounter?.queueItem?.status === 'IN_PROGRESS' ? 'processing' : (encounter?.queueItem?.status === 'WAITING_FOR_RESULT' ? 'warning' : 'default')}>
              {queueStatusLabel}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái bệnh án">
            <Tag
              color={recordLocked ? 'green' : (record?.status === 'SIGNED' ? 'cyan' : 'orange')}
              icon={recordLocked ? <LockOutlined /> : (record?.status === 'SIGNED' ? <CheckCircleOutlined /> : <WarningOutlined />)}
              style={{ fontWeight: 600 }}
            >
              {recordStatusLabel}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Số đơn thuốc hiện có">
            <Badge count={prescriptions.length} showZero color="#2563eb" />
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Banner tiền sử dị ứng thuốc - Hiển thị nổi bật ở đầu bệnh án của mọi lượt khám */}
      <PatientAllergyBanner
        patientId={encounter?.patient?.id || record?.patientId || routeState.patient?.id || routeState.encounter?.patient?.id}
        patientName={encounter?.patient?.fullName || record?.patientName || routeState.patient?.fullName || routeState.encounter?.patient?.fullName}
        visitId={targetVisitId}
        allergies={patientAllergies}
        onAllergiesChange={setPatientAllergies}
        currentUser={currentUser}
        compact={false}
      />

      {prescriptionBlockReason && (
        <Alert
          type={encounter?.queueItem?.status === 'WAITING_FOR_RESULT' ? 'warning' : 'error'}
          showIcon
          message="Tạm khóa thao tác kê đơn và hoàn tất lượt khám"
          description={prescriptionBlockReason}
          action={<Button onClick={loadData}>Tải lại trạng thái</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      {!isAssignedDoctor && (
        <Alert
          type="warning"
          showIcon
          message="Bạn không phải bác sĩ phụ trách lượt khám này"
          description="Hệ thống chỉ cho phép bác sĩ phụ trách lượt khám thực hiện kê đơn và điều chỉnh đơn thuốc."
          style={{ marginBottom: 16 }}
        />
      )}
      {!diagnoses.length && (
        <Alert type="error" showIcon message="Bệnh án chưa có chẩn đoán" style={{ marginBottom: 16 }} />
      )}
      {!recordLocked && !isRecordSigned && (
        <Alert
          type="info"
          showIcon
          icon={<InfoCircleOutlined />}
          message="Bệnh án đang chờ Ký số xác nhận"
          description={
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 4 }}>
              <span>
                Hồ sơ bệnh án hiện tại đang ở trạng thái <strong>Bản nháp</strong>. Bác sĩ cần thực hiện <strong>Ký số bệnh án</strong> trước, sau đó mới có thể thực hiện Khóa bệnh án để hoàn tất ca khám.
              </span>
              <Space wrap size="small">
                <Button
                  size="small"
                  type="primary"
                  icon={<EditOutlined />}
                  onClick={() => setSignModalOpen(true)}
                  style={{ fontWeight: 600, background: '#0284c7', borderColor: '#0284c7' }}
                >
                  Ký số bệnh án ngay
                </Button>
                {targetVisitId && (
                  <Button
                    size="small"
                    type="default"
                    icon={<ArrowLeftOutlined />}
                    onClick={() => navigate(`/medical-records/visits/${targetVisitId}`)}
                    style={{ fontWeight: 500 }}
                  >
                    Xem lại chi tiết tại màn Khám bệnh
                  </Button>
                )}
              </Space>
            </div>
          }
          style={{ marginBottom: 16 }}
        />
      )}
      {isRecordSigned && !recordLocked && (
        <Alert
          type="success"
          showIcon
          icon={<CheckCircleOutlined style={{ color: '#16a34a' }} />}
          message="Bệnh án đã được Ký số thành công — Sẵn sàng để Khóa hồ sơ"
          description={
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8, marginTop: 4 }}>
              <span>
                Hồ sơ đã được lưu chữ ký điện tử hợp lệ (<strong>ĐÃ KÝ SỐ</strong>). Sau khi hoàn tất kê đơn và kiểm tra thuốc, bác sĩ bấm nút <strong>"Khóa bệnh án & hoàn tất khám"</strong> để khóa hồ sơ và kết thúc ca khám.
              </span>
              <Button
                type="primary"
                danger
                size="small"
                icon={<LockOutlined />}
                loading={finalizing}
                onClick={finalizeEncounter}
                style={{ fontWeight: 600 }}
              >
                Khóa bệnh án & hoàn tất khám
              </Button>
            </div>
          }
          style={{ marginBottom: 16, background: '#f0fdf4', borderColor: '#bbf7d0' }}
        />
      )}
      {recordLocked && (
        <Alert
          type="info"
          showIcon
          icon={<LockOutlined />}
          message="Bệnh án đã được khóa; không thể sửa đổi đơn thuốc."
          style={{ marginBottom: 16 }}
        />
      )}

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        items={[
          {
            key: 'prescribe',
            label: (
              <span>
                {editingPrescription ? <EditOutlined /> : <PlusOutlined />}
                {editingPrescription ? ` Điều chỉnh đơn: ${editingPrescription.prescriptionCode}` : ' Kê đơn thuốc mới'}
              </span>
            ),
            children: (
              <div>
                {editingPrescription && (
                  <Alert
                    type="info"
                    showIcon
                    icon={<EditOutlined style={{ fontSize: 18 }} />}
                    message={
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                        <span>
                          <strong>ĐANG ĐIỀU CHỈNH ĐƠN THUỐC: {editingPrescription.prescriptionCode}</strong> — Trạng thái: <Tag color="orange">Chờ cấp phát (PENDING_DISPENSE)</Tag>
                        </span>
                        <Button size="small" onClick={cancelEditMode}>Hủy điều chỉnh</Button>
                      </div>
                    }
                    description="Bác sĩ có thể sửa liều lượng, tần suất, cách dùng, số lượng, hướng dẫn; bấm '+ Thêm thuốc mới' để bổ sung hoặc bấm biểu tượng thùng rác để bỏ thuốc không còn phù hợp khỏi đơn. Mọi thay đổi đều được hệ thống tự động lưu vết lịch sử (audit snapshot)."
                    style={{ marginBottom: 16, backgroundColor: '#eff6ff', borderColor: '#bfdbfe' }}
                  />
                )}

                <Card
                  title={
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span>
                        <MedicineBoxOutlined style={{ color: '#2563eb', marginRight: 8 }} />
                        {editingPrescription
                          ? `Danh sách thuốc điều chỉnh (${items.length} loại thuốc)`
                          : `Thuốc trong đơn (${items.length} loại thuốc)`}
                      </span>
                      {editingPrescription && (
                        <Tag color="blue">Đơn gốc kê lúc: {dayjs(editingPrescription.prescribedAt).format('HH:mm DD/MM/YYYY')}</Tag>
                      )}
                    </div>
                  }
                >
                  {detectedAllergyConflicts.length > 0 && (
                    <Alert
                      type="error"
                      showIcon
                      icon={<FireOutlined style={{ fontSize: 20, color: '#dc2626' }} />}
                      message={
                        <Text strong style={{ fontSize: 15, color: '#991b1b' }}>
                          CẢNH BÁO NGUY CƠ PHẢN VỆ: PHÁT HIỆN {detectedAllergyConflicts.length} THUỐC TRÙNG TIỀN SỬ DỊ ỨNG!
                        </Text>
                      }
                      description={
                        <div style={{ marginTop: 6 }}>
                          <div>Đơn thuốc đang kê có chứa hoạt chất/nhóm thuốc mà bệnh nhân có tiền sử dị ứng đã ghi nhận trong hồ sơ:</div>
                          <ul style={{ margin: '8px 0 0 18px', padding: 0 }}>
                            {detectedAllergyConflicts.map((c, idx) => (
                              <li key={idx} style={{ marginBottom: 4 }}>
                                <Text strong>{c.medicine?.medicineName || c.medicine?.name}:</Text> Dị ứng với <Tag color={c.severityMeta?.color}>{c.matchedAllergy?.allergenName}</Tag> - Mức độ: <Text type="danger" strong>{c.severityMeta?.label}</Text> {c.matchedAllergy?.reaction ? `(${c.matchedAllergy.reaction})` : ''}
                              </li>
                            ))}
                          </ul>
                          <div style={{ marginTop: 6, fontWeight: 600, color: '#b91c1c' }}>
                            Khuyến cáo: Thay thế bằng nhóm thuốc an toàn khác để ngăn ngừa sốc phản vệ đe dọa tính mạng người bệnh.
                          </div>
                        </div>
                      }
                      style={{ marginBottom: 16, border: '2px solid #ef4444', backgroundColor: '#fef2f2', borderRadius: 8 }}
                    />
                  )}

                  {items.map((item, index) => {
                    const selectedMed = selectedMedicineMap.get(String(item.medicineId))
                    const unit = selectedMed?.unit || 'viên'
                    const isComplete = Boolean(
                      item.medicineId &&
                      item.dosage?.trim() &&
                      item.frequency &&
                      Number(item.frequency) > 0 &&
                      item.route &&
                      item.durationDays &&
                      Number(item.durationDays) > 0 &&
                      item.quantity &&
                      Number(item.quantity) > 0,
                    )

                    return (
                      <Card
                        key={item.clientId}
                        size="small"
                        style={{
                          marginBottom: 16,
                          borderRadius: 8,
                          border: isComplete ? '1px solid #BFDBFE' : '1px solid #E2E8F0',
                          borderLeft: isComplete ? '4px solid #2563EB' : '4px solid #F59E0B',
                          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
                          backgroundColor: item.isOriginal ? '#ffffff' : '#fafafa',
                        }}
                      >
                        {/* Header của thẻ thuốc */}
                        <div
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            paddingBottom: 10,
                            marginBottom: 14,
                            borderBottom: '1px solid #F1F5F9',
                            flexWrap: 'wrap',
                            gap: 8,
                          }}
                        >
                          <Space size={8} wrap align="center">
                            <Tag
                              color={isComplete ? 'blue' : 'gold'}
                              style={{
                                fontWeight: 700,
                                fontSize: 13,
                                padding: '2px 10px',
                                borderRadius: 12,
                                margin: 0,
                              }}
                            >
                              Thuốc #{index + 1}
                            </Tag>

                            {isComplete ? (
                              <Tag color="success" icon={<CheckCircleOutlined />} style={{ borderRadius: 12, margin: 0 }}>
                                Đầy đủ thông tin
                              </Tag>
                            ) : (
                              <Tag color="warning" icon={<ExclamationCircleOutlined />} style={{ borderRadius: 12, margin: 0 }}>
                                Chưa đủ trường bắt buộc
                              </Tag>
                            )}

                            {editingPrescription && (
                              <Tag color={item.isOriginal ? 'default' : 'cyan'} style={{ borderRadius: 12, margin: 0 }}>
                                {item.isOriginal ? 'Thuốc trong đơn gốc' : 'Thuốc thêm mới'}
                              </Tag>
                            )}

                            {(() => {
                              const itemAllergyConflict = detectedAllergyConflicts.find((c) => c.itemIndex === index)
                              if (!itemAllergyConflict) return null
                              return (
                                <Tag
                                  color={itemAllergyConflict.isLifeThreatening ? '#b91c1c' : 'red'}
                                  icon={<FireOutlined />}
                                  style={{
                                    fontWeight: 700,
                                    fontSize: 12,
                                    padding: '2px 8px',
                                    borderRadius: 12,
                                    margin: 0,
                                    backgroundColor: itemAllergyConflict.isLifeThreatening ? '#fef2f2' : undefined,
                                    borderColor: itemAllergyConflict.isLifeThreatening ? '#ef4444' : undefined,
                                  }}
                                >
                                  {itemAllergyConflict.isLifeThreatening ? '🚨 NGUY CƠ SỐC PHẢN VỆ' : 'CẢNH BÁO DỊ ỨNG'}: {itemAllergyConflict.matchedAllergy?.allergenName} ({itemAllergyConflict.severityMeta?.label})
                                </Tag>
                              )
                            })()}

                            {selectedMed && (() => {
                              const avail = getAvailableStock(selectedMed)
                              if (avail <= 0) {
                                return (
                                  <Tag color="red" icon={<CloseCircleOutlined />} style={{ borderRadius: 12, margin: 0 }}>
                                    HẾT HÀNG (Tồn khả dụng: 0 {unit})
                                  </Tag>
                                )
                              }
                              if (item.quantity > avail) {
                                return (
                                  <Tag color="volcano" icon={<WarningOutlined />} style={{ borderRadius: 12, margin: 0 }}>
                                    Vượt quá tồn kho (Còn {avail} {unit})
                                  </Tag>
                                )
                              }
                              return (
                                <Text type="secondary" style={{ fontSize: 12 }}>
                                  (Tồn khả dụng: <strong style={{ color: '#16a34a' }}>{avail}</strong> {unit})
                                </Text>
                              )
                            })()}
                          </Space>

                          <Tooltip title={items.length <= 1 ? 'Đơn thuốc phải có ít nhất 1 thuốc' : 'Bỏ thuốc này khỏi đơn'}>
                            <Button
                              danger
                              type="text"
                              size="small"
                              icon={<DeleteOutlined />}
                              disabled={!canPrescribe || items.length <= 1 || checkingInteractions || saving}
                              onClick={() => handleRemoveItem(item.clientId)}
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: 4,
                                backgroundColor: '#FEF2F2',
                                border: '1px solid #FEE2E2',
                                borderRadius: 6,
                                padding: '2px 10px',
                                fontWeight: 500,
                              }}
                            >
                              Bỏ thuốc
                            </Button>
                          </Tooltip>
                        </div>

                        {/* Hàng 1: Thuốc & Đường dùng */}
                        <Row gutter={[12, 12]} style={{ marginBottom: 12 }}>
                          <Col xs={24} md={16}>
                            <Form.Item
                              label={<span style={{ fontWeight: 600, color: '#334155' }}>Thuốc & Hoạt chất <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <Select
                                showSearch
                                style={{ width: '100%' }}
                                optionFilterProp="label"
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.medicineId}
                                onChange={(value) => handleItemChange(item.clientId, 'medicineId', value)}
                                options={sortedMedicines.map((medicine) => {
                                  const availStock = getAvailableStock(medicine)
                                  const isOut = availStock <= 0
                                  const medConflict = checkPrescriptionAllergyConflict(medicine, patientAllergies)
                                  const allergyPrefix = medConflict.hasConflict
                                    ? `[⚠️ DỊ ỨNG${medConflict.isLifeThreatening ? ' - SỐC PHẢN VỆ' : ''}] `
                                    : ''
                                  return {
                                    value: medicine.id,
                                    disabled: isOut,
                                    label: isOut
                                      ? `${allergyPrefix}${medicine.medicineName} — ${medicine.strength ? `${medicine.strength} ` : ''}— Hết hàng`
                                      : `${allergyPrefix}${medicine.medicineName} — ${medicine.strength ? `${medicine.strength} ` : ''}— Còn ${availStock} ${medicine.unit || 'viên'}`,
                                  }
                                })}
                                placeholder="Tìm kiếm thuốc theo tên hoặc hoạt chất..."
                              />
                            </Form.Item>
                          </Col>

                          <Col xs={24} md={8}>
                            <Form.Item
                              label={<span style={{ fontWeight: 600, color: '#334155' }}>Đường dùng <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <Select
                                style={{ width: '100%' }}
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.route}
                                onChange={(value) => handleItemChange(item.clientId, 'route', value)}
                                options={ROUTE_OPTIONS}
                                placeholder="Chọn đường dùng..."
                              />
                            </Form.Item>
                          </Col>
                        </Row>

                        {selectedMed && (
                          <div
                            style={{
                              background: '#F8FAFC',
                              border: '1px solid #E2E8F0',
                              padding: '6px 12px',
                              borderRadius: 6,
                              marginBottom: 12,
                              display: 'flex',
                              flexWrap: 'wrap',
                              gap: 8,
                              alignItems: 'center',
                              fontSize: 12,
                            }}
                          >
                            <span style={{ color: '#64748B', fontWeight: 500 }}>Quy cách:</span>
                            <Tag color="blue" style={{ margin: 0, borderRadius: 4 }}>
                              Hàm lượng: <strong>{selectedMed.strength || 'Theo quy cách'}</strong>
                            </Tag>
                            <Tag color="cyan" style={{ margin: 0, borderRadius: 4 }}>
                              Hoạt chất: <strong>{selectedMed.activeIngredient || selectedMed.medicineName}</strong>
                            </Tag>
                            <Tag color="purple" style={{ margin: 0, borderRadius: 4 }}>
                              Đơn vị tính: <strong>{selectedMed.unit || 'viên'}</strong>
                            </Tag>
                            {(() => {
                              const avail = getAvailableStock(selectedMed)
                              return (
                                <Tag color={avail > 0 ? 'green' : 'red'} style={{ margin: 0, borderRadius: 4 }}>
                                  Tồn khả dụng: <strong>{avail} {selectedMed.unit || 'viên'}</strong>
                                </Tag>
                              )
                            })()}
                          </div>
                        )}

                        {(() => {
                          const itemAllergyConflict = detectedAllergyConflicts.find((c) => c.itemIndex === index)
                          if (!itemAllergyConflict) return null
                          return (
                            <Alert
                              type="error"
                              showIcon
                              icon={<FireOutlined style={{ fontSize: 18, color: '#dc2626' }} />}
                              message={
                                <Text strong style={{ color: itemAllergyConflict.isLifeThreatening ? '#7f1d1d' : '#991b1b', fontSize: 13 }}>
                                  {itemAllergyConflict.isLifeThreatening ? '🚨 CẢNH BÁO SỐC PHẢN VỆ / ĐE DỌA TÍNH MẠNG' : 'CẢNH BÁO DỊ ỨNG THUỐC ĐÃ BIẾT'}
                                </Text>
                              }
                              description={
                                <div style={{ fontSize: 12 }}>
                                  <div>{itemAllergyConflict.warningMessage}</div>
                                  {itemAllergyConflict.matchedAllergy?.notes && (
                                    <div style={{ marginTop: 2, color: '#4b5563' }}>
                                      <strong>Ghi chú tiền sử:</strong> {itemAllergyConflict.matchedAllergy.notes}
                                    </div>
                                  )}
                                </div>
                              }
                              style={{
                                marginBottom: 12,
                                borderRadius: 6,
                                border: '1.5px solid #ef4444',
                                backgroundColor: '#fef2f2',
                              }}
                            />
                          )
                        })()}

                        {/* Hàng 2: Liều dùng, Tần suất, Số ngày & Tổng số lượng (cân đối 4 cột) */}
                        <Row gutter={[12, 12]} style={{ marginBottom: 12 }}>
                          <Col xs={24} sm={12} md={6}>
                            <Form.Item
                              label={<span style={{ fontWeight: 600, color: '#334155' }}>Liều dùng / lần <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <Input
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.dosage}
                                onChange={(event) => handleItemChange(item.clientId, 'dosage', event.target.value)}
                                placeholder={`VD: 1 ${unit}`}
                              />
                            </Form.Item>
                            <div style={{ marginTop: 6, minHeight: 24, display: 'flex', alignItems: 'center', gap: 4, flexWrap: 'wrap' }}>
                              {[`1 ${unit}`, `2 ${unit}`, `1/2 ${unit}`].map((sug) => (
                                <Tag
                                  key={sug}
                                  style={{
                                    cursor: 'pointer',
                                    fontSize: 11,
                                    margin: 0,
                                    padding: '0 6px',
                                    borderRadius: 4,
                                    background: '#F8FAFC',
                                    border: '1px solid #E2E8F0',
                                  }}
                                  onClick={() => handleItemChange(item.clientId, 'dosage', sug)}
                                >
                                  {sug}
                                </Tag>
                              ))}
                            </div>
                          </Col>

                          <Col xs={24} sm={12} md={6}>
                            <Form.Item
                              label={<span style={{ fontWeight: 600, color: '#334155' }}>Số lần / ngày <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <InputNumber
                                min={1}
                                max={24}
                                step={1}
                                precision={0}
                                style={{ width: '100%' }}
                                placeholder="2"
                                addonAfter="lần/ngày"
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.frequency}
                                onChange={(value) => handleItemChange(item.clientId, 'frequency', value)}
                              />
                            </Form.Item>
                            <div style={{ marginTop: 6, minHeight: 24, display: 'flex', alignItems: 'center', gap: 4, flexWrap: 'wrap' }}>
                              {[1, 2, 3].map((f) => (
                                <Tag
                                  key={f}
                                  style={{
                                    cursor: 'pointer',
                                    fontSize: 11,
                                    margin: 0,
                                    padding: '0 6px',
                                    borderRadius: 4,
                                    background: '#F8FAFC',
                                    border: '1px solid #E2E8F0',
                                  }}
                                  onClick={() => handleItemChange(item.clientId, 'frequency', f)}
                                >
                                  {f} lần
                                </Tag>
                              ))}
                            </div>
                          </Col>

                          <Col xs={24} sm={12} md={6}>
                            <Form.Item
                              label={<span style={{ fontWeight: 600, color: '#334155' }}>Số ngày dùng <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <InputNumber
                                min={1}
                                max={365}
                                precision={0}
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.durationDays}
                                onChange={(value) => handleItemChange(item.clientId, 'durationDays', value)}
                                style={{ width: '100%' }}
                                placeholder="5"
                                addonAfter="ngày"
                              />
                            </Form.Item>
                            <div style={{ marginTop: 6, minHeight: 24, display: 'flex', alignItems: 'center', gap: 4, flexWrap: 'wrap' }}>
                              {[3, 5, 7, 10].map((d) => (
                                <Tag
                                  key={d}
                                  style={{
                                    cursor: 'pointer',
                                    fontSize: 11,
                                    margin: 0,
                                    padding: '0 6px',
                                    borderRadius: 4,
                                    background: '#F8FAFC',
                                    border: '1px solid #E2E8F0',
                                  }}
                                  onClick={() => handleItemChange(item.clientId, 'durationDays', d)}
                                >
                                  {d} ngày
                                </Tag>
                              ))}
                            </div>
                          </Col>

                          <Col xs={24} sm={12} md={6}>
                            <Form.Item
                              label={<span style={{ fontWeight: 600, color: '#334155' }}>Tổng số lượng <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <InputNumber
                                min={1}
                                precision={0}
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.quantity}
                                onChange={(value) => handleItemChange(item.clientId, 'quantity', value)}
                                style={{ width: '100%' }}
                                placeholder="10"
                                addonAfter={unit}
                              />
                            </Form.Item>
                            <div style={{ marginTop: 6, minHeight: 24, display: 'flex', alignItems: 'center' }}>
                              {Number(item.frequency) > 0 && Number(item.durationDays) > 0 ? (
                                <Tag
                                  color="blue"
                                  style={{
                                    cursor: 'pointer',
                                    fontSize: 11,
                                    margin: 0,
                                    padding: '0 6px',
                                    borderRadius: 4,
                                  }}
                                  onClick={() => {
                                    const qty = Number(item.frequency) * Number(item.durationDays)
                                    handleItemChange(item.clientId, 'quantity', qty)
                                  }}
                                >
                                  ⚡ Tự tính: {Number(item.frequency) * Number(item.durationDays)} {unit}
                                </Tag>
                              ) : (
                                <span style={{ fontSize: 11, color: '#94A3B8' }}>(= Lần × Ngày)</span>
                              )}
                            </div>
                            {selectedMed && (() => {
                              const avail = getAvailableStock(selectedMed)
                              if (avail > 0 && item.quantity > avail) {
                                return (
                                  <div style={{ color: '#dc2626', fontSize: 12, marginTop: 4, fontWeight: 500 }}>
                                    Không đủ tồn kho. Tối đa có thể kê: {avail} {unit}.
                                  </div>
                                )
                              }
                              return null
                            })()}
                          </Col>
                        </Row>

                        {/* Hàng 3: Hướng dẫn dùng & Lời dặn chi tiết */}
                        <Form.Item
                          label={<span style={{ fontWeight: 600, color: '#334155' }}>Hướng dẫn sử dụng & Lời dặn của bác sĩ</span>}
                          style={{ marginBottom: 4 }}
                        >
                          <Input
                            disabled={!canPrescribe || checkingInteractions || saving}
                            value={item.instructions}
                            onChange={(event) => handleItemChange(item.clientId, 'instructions', event.target.value)}
                            placeholder="Ví dụ: Uống sau khi ăn no 30 phút, uống với nhiều nước..."
                          />
                        </Form.Item>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap', marginTop: 4 }}>
                          <span style={{ fontSize: 11, color: '#64748B' }}>Gợi ý nhanh:</span>
                          {['Uống sau ăn no', 'Uống trước ăn 30 phút', 'Uống trước khi đi ngủ', 'Uống nhiều nước'].map((preset) => (
                            <Tag
                              key={preset}
                              style={{
                                cursor: 'pointer',
                                fontSize: 11,
                                margin: 0,
                                padding: '1px 8px',
                                borderRadius: 4,
                                background: '#F8FAFC',
                                border: '1px solid #E2E8F0',
                                color: '#334155',
                              }}
                              onClick={() => {
                                const current = item.instructions?.trim()
                                const newVal = current
                                  ? current.includes(preset)
                                    ? current
                                    : `${current}; ${preset}`
                                  : preset
                                handleItemChange(item.clientId, 'instructions', newVal)
                              }}
                            >
                              + {preset}
                            </Tag>
                          ))}
                        </div>

                        {selectedMed && (() => {
                          const avail = getAvailableStock(selectedMed)
                          if (avail <= 0) {
                            return (
                              <Alert
                                type="error"
                                showIcon
                                icon={<StopOutlined />}
                                message={`Thuốc "${selectedMed.medicineName}" hiện đã HẾT HÀNG (tồn khả dụng = 0). Vui lòng đổi sang thuốc khác.`}
                                style={{ marginTop: 12, borderRadius: 6 }}
                              />
                            )
                          }
                          if (item.quantity > avail) {
                            return (
                              <Alert
                                type="error"
                                showIcon
                                icon={<WarningOutlined />}
                                message={`Số lượng kê (${item.quantity} ${unit}) vượt quá tồn kho khả dụng (hiện còn ${avail} ${unit}).`}
                                style={{ marginTop: 12, borderRadius: 6 }}
                              />
                            )
                          }
                          return null
                        })()}
                      </Card>
                    )
                  })}

                  {canPrescribe && (
                    <Button
                      type="dashed"
                      icon={<PlusOutlined />}
                      disabled={checkingInteractions || saving || items.some((i) => {
                        if (!i.medicineId) return false
                        const med = selectedMedicineMap.get(String(i.medicineId))
                        if (!med) return false
                        const avail = getAvailableStock(med)
                        const totalQty = items
                          .filter((x) => String(x.medicineId) === String(i.medicineId))
                          .reduce((sum, x) => sum + Number(x.quantity || 0), 0)
                        return avail <= 0 || Number(i.quantity || 0) > avail || totalQty > avail
                      })}
                      onClick={() => {
                        setConfirmedOverrides([])
                        setItems((current) => [...current, createEmptyItem(false)])
                      }}
                      style={{
                        width: '100%',
                        marginTop: 4,
                        height: 42,
                        borderRadius: 8,
                        borderColor: '#93C5FD',
                        color: '#2563EB',
                        fontWeight: 600,
                        backgroundColor: '#F8FAFC',
                      }}
                    >
                      + Thêm thuốc mới vào đơn
                    </Button>
                  )}

                  <Divider style={{ margin: '16px 0' }} />

                  <Form.Item label="Ghi chú đơn thuốc (cho bệnh nhân & dược sĩ)">
                    <Input.TextArea
                      rows={2}
                      disabled={!canPrescribe || checkingInteractions || saving}
                      value={note}
                      onChange={(event) => setNote(event.target.value)}
                      placeholder="Nhập dặn dò thêm cho bệnh nhân..."
                    />
                  </Form.Item>

                  {editingPrescription && (
                    <div style={{ backgroundColor: '#fffbeb', padding: 14, borderRadius: 8, border: '1px solid #fef3c7', marginTop: 12 }}>
                      <Form.Item
                        label={
                          <span>
                            <strong style={{ color: '#b45309' }}>Lý do điều chỉnh đơn thuốc *</strong> (Bắt buộc theo quy chế bệnh án)
                          </span>
                        }
                        style={{ marginBottom: 8 }}
                      >
                        <Input.TextArea
                          rows={2}
                          disabled={!canPrescribe || checkingInteractions || saving}
                          value={changeReason}
                          onChange={(event) => setChangeReason(event.target.value)}
                          placeholder="Nhập lý do điều chỉnh đơn thuốc hoặc chọn nhanh từ danh sách bên dưới..."
                        />
                      </Form.Item>

                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>Mẫu lý do gợi ý:</Text>
                        {PRESET_CHANGE_REASONS.map((preset, idx) => (
                          <Tag
                            key={idx}
                            color="orange"
                            style={{ cursor: (canPrescribe && !checkingInteractions && !saving) ? 'pointer' : 'not-allowed', margin: '2px 0' }}
                            onClick={() => {
                              if (!canPrescribe || checkingInteractions || saving) return
                              setChangeReason(preset)
                            }}
                          >
                            + {preset}
                          </Tag>
                        ))}
                      </div>
                    </div>
                  )}

                  {checkingInteractions && (
                    <div style={{ marginTop: 16 }}>
                      <Alert
                        type="info"
                        showIcon
                        icon={<Spin size="small" />}
                        message="Đang kiểm tra tương tác thuốc..."
                      />
                    </div>
                  )}

                  {!checkingInteractions && interactionApiError && (
                    <div style={{ marginTop: 16 }}>
                      <Alert
                        type="error"
                        showIcon
                        icon={<WarningOutlined />}
                        message="Lỗi kiểm tra tương tác thuốc"
                        description={
                          <div>
                            <Paragraph style={{ marginBottom: 8, color: '#991b1b' }}>
                              Không thể kiểm tra tương tác thuốc. Vui lòng thử lại.
                            </Paragraph>
                            <Button
                              size="small"
                              type="primary"
                              danger
                              onClick={() => performInteractionCheck(items).catch(() => {})}
                            >
                              Thử lại kiểm tra tương tác
                            </Button>
                          </div>
                        }
                      />
                    </div>
                  )}

                  {!checkingInteractions && !interactionApiError && detectedInteractions.length > 0 && (
                    <div style={{ marginTop: 16 }}>
                      {areAllInteractionsHandled(detectedInteractions, confirmedOverrides) ? (
                        <Alert
                          type="warning"
                          showIcon
                          icon={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
                          message={`Đã xác nhận lý do bỏ qua cho toàn bộ ${detectedInteractions.length} cảnh báo tương tác thuốc`}
                          description={
                            <div>
                              <ul style={{ margin: '4px 0 8px 0', paddingLeft: 20 }}>
                                {detectedInteractions.map((w, idx) => {
                                  const ov = confirmedOverrides.find((o) => String(o.ruleId) === String(w.ruleId))
                                  return (
                                    <li key={idx}>
                                      <strong>{w.drugNameA}</strong> — <strong>{w.drugNameB}</strong> ({w.severity}):{' '}
                                      <Text type="secondary">Lý do: "{ov?.overrideReason}"</Text>
                                    </li>
                                  )
                                })}
                              </ul>
                              <Button size="small" onClick={() => setInteractionModalOpen(true)}>
                                Xem / Thay đổi lý do bỏ qua
                              </Button>
                            </div>
                          }
                        />
                      ) : (
                        <Alert
                          type="error"
                          showIcon
                          icon={<WarningOutlined />}
                          message={`Phát hiện ${detectedInteractions.length} tương tác thuốc bất lợi (${getUnhandledInteractions(detectedInteractions, confirmedOverrides).length} chưa xử lý)`}
                          description={
                            <div>
                              <Paragraph style={{ marginBottom: 8, color: '#991b1b' }}>
                                Nút "Tạo đơn thuốc" tạm thời bị khóa. Bác sĩ phải điều chỉnh bỏ/đổi thuốc hoặc bấm "Xem cảnh báo & Bỏ qua" để nhập lý do chuyên môn bỏ qua trước khi kê đơn.
                              </Paragraph>
                              <ul style={{ margin: '4px 0 8px 0', paddingLeft: 20 }}>
                                {detectedInteractions.map((w, idx) => (
                                  <li key={idx}>
                                    <strong>{w.drugNameA}</strong> — <strong>{w.drugNameB}</strong> ({w.severity}): {w.description}
                                  </li>
                                ))}
                              </ul>
                              <Button size="small" danger onClick={() => setInteractionModalOpen(true)}>
                                Xem cảnh báo & Bỏ qua
                              </Button>
                            </div>
                          }
                        />
                      )}
                    </div>
                  )}

                  <div style={{ marginTop: 20, display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
                    {editingPrescription && (
                      <Button disabled={checkingInteractions || saving} onClick={cancelEditMode}>Hủy điều chỉnh</Button>
                    )}
                    {canPrescribe && (
                      <Tooltip title={!canSubmit ? submitStatus.reason : ''}>
                        <Button
                          type="primary"
                          loading={saving || checkingInteractions}
                          disabled={!canSubmit}
                          icon={<CheckCircleOutlined />}
                          onClick={handleSaveClick}
                        >
                          {editingPrescription ? 'Lưu điều chỉnh đơn thuốc' : 'Tạo đơn thuốc'}
                        </Button>
                      </Tooltip>
                    )}
                  </div>
                </Card>
              </div>
            ),
          },
          {
            key: 'history',
            label: (
              <span>
                <HistoryOutlined /> Danh sách & Lịch sử đơn thuốc ({prescriptions.length})
              </span>
            ),
            children: (
              <div>
                <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
                  <Input
                    placeholder="Tìm kiếm theo mã đơn điện tử (RX...), tên/mã bệnh nhân, mã khám..."
                    prefix={<SearchOutlined style={{ color: '#9ca3af' }} />}
                    value={prescriptionSearchText}
                    onChange={(e) => setPrescriptionSearchText(e.target.value)}
                    allowClear
                    style={{ maxWidth: 420 }}
                  />
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    Hiển thị <strong>{filteredPrescriptions.length}</strong> / {prescriptions.length} đơn thuốc
                  </Text>
                </div>

                <Table
                  rowKey="id"
                  dataSource={filteredPrescriptions}
                  columns={historyColumns}
                  pagination={{ pageSize: 10 }}
                  bordered
                />
              </div>
            ),
          },
        ]}
      />

      <Modal
        open={issuedPrescriptionModalOpen}
        onCancel={() => setIssuedPrescriptionModalOpen(false)}
        footer={
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 12,
              flexWrap: 'wrap',
              paddingTop: 14,
              borderTop: '1px solid #f0f0f0',
              width: '100%',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center' }}>
              {justIssuedPrescription?.interconnectionStatus === 'SUCCESS' ? (
                <span
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '6px 14px',
                    backgroundColor: '#f0fdf4',
                    color: '#166534',
                    border: '1px solid #86efac',
                    borderRadius: 9999,
                    fontSize: 13,
                    fontWeight: 600,
                    whiteSpace: 'nowrap',
                    lineHeight: 1.4,
                  }}
                >
                  <CheckCircleOutlined style={{ color: '#16a34a', fontSize: 14 }} />
                  Đã liên thông ({justIssuedPrescription.interconnectionReceiptCode})
                </span>
              ) : (
                <Button
                  type="primary"
                  icon={<CloudUploadOutlined />}
                  loading={sendingInterconnectionId === justIssuedPrescription?.id}
                  onClick={async () => {
                    if (justIssuedPrescription) {
                      await handleSendPrescriptionToInterconnection(justIssuedPrescription)
                    }
                  }}
                  style={{
                    backgroundColor: '#0284c7',
                    borderColor: '#0284c7',
                    whiteSpace: 'nowrap',
                    padding: '6px 14px',
                    display: 'inline-flex',
                    alignItems: 'center',
                  }}
                >
                  Gửi liên thông ngay
                </Button>
              )}
            </div>

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                flexWrap: 'wrap',
              }}
            >
              <Button
                key="print"
                type="default"
                icon={<PrinterOutlined />}
                style={{
                  whiteSpace: 'nowrap',
                  padding: '6px 14px',
                  display: 'inline-flex',
                  alignItems: 'center',
                }}
                onClick={() => {
                  setIssuedPrescriptionModalOpen(false)
                  if (justIssuedPrescription) {
                    setSelectedPrescriptionForPrint(justIssuedPrescription)
                    setPrintModalOpen(true)
                  }
                }}
              >
                In đơn
              </Button>
              <Button
                key="detail"
                type="default"
                icon={<EyeOutlined />}
                style={{
                  whiteSpace: 'nowrap',
                  padding: '6px 14px',
                  display: 'inline-flex',
                  alignItems: 'center',
                }}
                onClick={() => {
                  setIssuedPrescriptionModalOpen(false)
                  if (justIssuedPrescription) {
                    openDetailModal(justIssuedPrescription)
                  }
                }}
              >
                Xem chi tiết
              </Button>
              <Button
                key="close"
                type="primary"
                style={{
                  whiteSpace: 'nowrap',
                  padding: '6px 14px',
                  display: 'inline-flex',
                  alignItems: 'center',
                }}
                onClick={() => setIssuedPrescriptionModalOpen(false)}
              >
                Đóng & Tiếp tục
              </Button>
            </div>
          </div>
        }
        width={640}
      >
        <div style={{ textAlign: 'center', padding: '16px 8px 8px' }}>
          <div style={{ fontSize: 44, marginBottom: 8 }}>🩺</div>
          <Title level={4} style={{ color: '#166534', margin: 0 }}>
            Cấp Mã Đơn Thuốc Điện Tử Thành Công
          </Title>
          <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 16 }}>
            Hệ thống đã tự động cấp mã định danh duy nhất cho đơn thuốc của lượt khám này.
          </Paragraph>

          <Card
            style={{
              backgroundColor: '#f0fdf4',
              borderColor: '#86efac',
              borderRadius: 12,
              marginBottom: 16,
            }}
          >
            <div style={{ fontSize: 13, color: '#166534', marginBottom: 6, textTransform: 'uppercase', letterSpacing: 1 }}>
              Mã Đơn Thuốc Điện Tử
            </div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, marginBottom: 8 }}>
              <Tag
                color="blue"
                style={{
                  fontSize: 22,
                  padding: '6px 18px',
                  fontWeight: 800,
                  letterSpacing: 1.5,
                  borderRadius: 8,
                  borderColor: '#93c5fd',
                  backgroundColor: '#eff6ff',
                  color: '#1d4ed8',
                }}
              >
                <BarcodeOutlined style={{ marginRight: 8 }} />
                {justIssuedPrescription?.prescriptionCode || '—'}
              </Tag>
              <Tooltip title="Sao chép mã đơn">
                <Button
                  icon={<CopyOutlined />}
                  onClick={() => {
                    if (justIssuedPrescription?.prescriptionCode) {
                      navigator.clipboard.writeText(justIssuedPrescription.prescriptionCode)
                      message.success(`Đã sao chép mã đơn: ${justIssuedPrescription.prescriptionCode}`)
                    }
                  }}
                />
              </Tooltip>
            </div>
            <div style={{ fontSize: 12.5, color: '#15803d', fontStyle: 'italic' }}>
              ✓ Mã định danh duy nhất được gắn cố định với đơn, không đổi trong suốt vòng đời phục vụ in ấn, tra cứu và liên thông.
            </div>
          </Card>

          {justIssuedPrescription?.interconnectionStatus === 'SUCCESS' && (
            <div
              style={{
                backgroundColor: '#f0fdf4',
                borderColor: '#86efac',
                borderWidth: 1,
                borderStyle: 'solid',
                borderRadius: 8,
                padding: '10px 14px',
                marginBottom: 16,
                textAlign: 'left',
              }}
            >
              <Space size={10} align="center">
                <CheckCircleOutlined style={{ fontSize: 22, color: '#16a34a' }} />
                <div>
                  <div style={{ fontWeight: 700, color: '#166534', fontSize: 13 }}>
                    ĐÃ LIÊN THÔNG QUỐC GIA THÀNH CÔNG
                  </div>
                  <div style={{ fontSize: 12.5, color: '#15803d', marginTop: 2 }}>
                    Mã biên nhận: <Text code strong style={{ color: '#15803d', fontSize: 13 }}>{justIssuedPrescription.interconnectionReceiptCode}</Text>
                  </div>
                </div>
              </Space>
            </div>
          )}

          <Descriptions size="small" column={2} bordered>
            <Descriptions.Item label="Bệnh nhân">
              <strong>{encounter?.patient?.fullName || record?.patientName || '—'}</strong>
            </Descriptions.Item>
            <Descriptions.Item label="Mã BN">
              {encounter?.patient?.patientCode || record?.patientCode || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Mã lượt khám">
              {encounter?.visit?.visitCode || record?.visitId || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color="orange">Chờ cấp phát</Tag>
            </Descriptions.Item>
          </Descriptions>
        </div>
      </Modal>

      {interactionModalOpen && (
        <React.Suspense fallback={<Spin size="small" />}>
          <InteractionWarningModal
            open={interactionModalOpen}
            warnings={detectedInteractions}
            onCancel={() => setInteractionModalOpen(false)}
            onConfirmOverride={handleConfirmInteractionOverrides}
          />
        </React.Suspense>
      )}

      <PrescriptionDetailModal
        open={detailModalOpen}
        onClose={() => setDetailModalOpen(false)}
        prescription={selectedPrescriptionForDetail}
        medicines={medicines}
        canEdit={canPrescribe}
        canSendInterconnection={canSendInterconnection}
        onInterconnectionUpdated={loadData}
        onEditClick={startEditPrescription}
        onPrintClick={(p) => {
          setSelectedPrescriptionForPrint(p)
          setPrintModalOpen(true)
        }}
      />

      <PrescriptionPrintTemplateModal
        open={printModalOpen}
        onClose={() => {
          setPrintModalOpen(false)
          setSelectedPrescriptionForPrint(null)
        }}
        prescription={selectedPrescriptionForPrint}
        record={record}
        diagnoses={diagnoses}
        patient={encounter?.patient}
        encounter={encounter}
      />

      {signModalOpen && (
        <SignMedicalRecordModal
          open={signModalOpen}
          onClose={() => setSignModalOpen(false)}
          onSuccess={handleSignSuccess}
          recordId={medicalRecordId}
          encounterContext={encounter}
          medicalRecord={record}
          patient={
            encounter?.patient || {
              id: record?.patientId,
              fullName: record?.patientName,
              patientCode: record?.patientCode,
            }
          }
          formValues={signFormValues}
          vitalSigns={record?.vitalSigns || {}}
          bmiValue={record?.bmiValue || null}
          primaryIcd={primaryIcd}
          secondaryIcds={secondaryIcds}
          selectedOrders={[]}
          currentUser={currentUser}
          onOpenAmend={() => {
            setSignModalOpen(false)
            if (targetVisitId) {
              navigate(`/medical-records/visits/${targetVisitId}`)
            }
          }}
        />
      )}
    </div>
  )
}

export default PrescriptionPage
