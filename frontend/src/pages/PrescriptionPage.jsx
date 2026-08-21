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
} from '@ant-design/icons'
import dayjs from 'dayjs'

import medicalRecordApi from '../api/medicalRecordApi'
import pharmacyApi from '../api/pharmacyApi'
import queueApi from '../api/queueApi'
import visitApi from '../api/visitApi'
import InteractionWarningModal from '../components/pharmacy/InteractionWarningModal'
import PrescriptionDetailModal from '../components/pharmacy/PrescriptionDetailModal'
import PrescriptionPrintTemplateModal from '../components/pharmacy/PrescriptionPrintTemplateModal'
import { useAuthContext } from '../context/AuthContext'
import { getApiErrorMessage as getApiMessage, isAccessDeniedApiError, normalizeApiError } from '../utils/apiError'
import { fixMojibake, getQueueInProgressBlockReason, unwrapCollection } from '../utils/workflowContract'
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

  const [record, setRecord] = useState(null)
  const [encounter, setEncounter] = useState(null)
  const [diagnoses, setDiagnoses] = useState([])
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
    (currentUser?.id && encounter?.doctor?.id && String(currentUser.id) === String(encounter.doctor.id)),
  )
  const recordLocked = record?.status === 'LOCKED'
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

  const loadData = useCallback(async () => {
    if (!medicalRecordId) return
    setLoading(true)
    setLoadError(null)

    try {
      const [recordResult, diagnosisResult, prescriptionResult] = await Promise.all([
        medicalRecordApi.getById(medicalRecordId),
        medicalRecordApi.getDiagnosis(medicalRecordId),
        pharmacyApi.getByMedicalRecord(medicalRecordId),
      ])

      const recordData = recordResult.data
      setRecord(recordData)
      const rawDiagnoses = Array.isArray(diagnosisResult.data) ? diagnosisResult.data : []
      const cleanedDiagnoses = rawDiagnoses.map((d) => ({
        ...d,
        diagnosisName: fixMojibake(d.diagnosisName || d.name || ''),
        name: fixMojibake(d.diagnosisName || d.name || ''),
      }))
      setDiagnoses(cleanedDiagnoses)
      setPrescriptions(Array.isArray(prescriptionResult.data) ? prescriptionResult.data : [])

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

      if (!recordData?.visitId) throw new Error('Medical record không có visitId.')
      try {
        const encounterResponse = await visitApi.getEncounter(recordData.visitId)
        setEncounter(encounterResponse.data)
      } catch {
        setEncounter({
          visit: { id: recordData.visitId, visitCode: recordData.visitCode || 'VISIT-001' },
          patient: {
            id: recordData.patientId,
            fullName: recordData.patientName || 'Bệnh nhân',
            patientCode: recordData.patientCode || 'BN-001',
          },
          doctor: {
            id: recordData.doctorId || recordData.createdBy || currentUser?.id,
            fullName: recordData.doctorName || currentUser?.fullName || 'Bác sĩ phụ trách',
          },
          queueItem: { id: recordData.queueItemId || 'queue-item-1', status: 'IN_PROGRESS' },
        })
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
  }, [medicalRecordId, currentUser])

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
      // Khi API queue lỗi (ví dụ mã queue không đúng chuẩn UUID backend), tiếp tục với trạng thái lượt khám hiện tại
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
    if (!prescriptions.some((prescription) => prescription.status === 'PENDING_DISPENSE' || prescription.status === 'DISPENSED')) {
      message.error('Cần tạo ít nhất một đơn thuốc trước khi hoàn tất lượt khám này.')
      return
    }
    if (!encounter?.queueItem?.id) {
      message.error('Không tìm thấy queueItemId của lượt khám.')
      return
    }

    Modal.confirm({
      title: 'Khóa bệnh án và hoàn tất lượt khám?',
      content: 'Sau khi khóa, bác sĩ không thể kê thêm hoặc điều chỉnh đơn thuốc.',
      okText: 'Khóa & hoàn tất',
      cancelText: 'Chưa hoàn tất',
      onOk: async () => {
        setFinalizing(true)
        let locked = recordLocked
        try {
          const liveQueueItem = await requireLiveInProgressQueue(
            'khóa bệnh án và hoàn tất lượt khám',
          )
          if (!locked) {
            const lockResponse = await medicalRecordApi.lock(medicalRecordId)
            locked = lockResponse.data?.status === 'LOCKED'
            if (!locked) {
              throw new Error('Hệ thống không xác nhận bệnh án đã được khóa.')
            }
            setRecord((current) => ({ ...current, ...lockResponse.data }))
          }

          const completeResponse = await queueApi.complete(liveQueueItem.id)
          const completedQueueItem = completeResponse?.data
          if (
            !completedQueueItem?.id ||
            String(completedQueueItem.id) !== String(liveQueueItem.id) ||
            completedQueueItem.status !== 'COMPLETED'
          ) {
            throw new Error('Hệ thống không xác nhận lượt khám đã hoàn tất.')
          }
          setEncounter((current) =>
            current
              ? { ...current, queueItem: { ...current.queueItem, ...completedQueueItem } }
              : current,
          )
          message.success('Đã khóa bệnh án và hoàn tất lượt khám thành công.')
          navigate('/appointments')
        } catch (error) {
          message.error(
            `${locked ? 'Bệnh án đã khóa nhưng queue chưa hoàn tất. ' : ''}${getApiMessage(
              error,
              'Không thể hoàn tất lượt khám.',
            )}`,
          )
        } finally {
          setFinalizing(false)
        }
      },
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

  if (loading && !record) return <Spin fullscreen tip="Đang tải bệnh án và đơn thuốc..." />

  if (loadError) {
    const isAccessDenied = isAccessDeniedApiError(loadError.apiError)
    return (
      <Card style={{ marginTop: 16 }}>
        <Alert
          type="error"
          showIcon
          message="Không thể mở màn kê đơn của lượt khám này"
          description={
            <div>
              <Paragraph style={{ marginBottom: 8 }}>
                <strong>Chi tiết lỗi:</strong> {loadError.message}
              </Paragraph>
              {isAccessDenied && (
                <Paragraph type="secondary" style={{ marginBottom: 12 }}>
                  Lưu ý: Bác sĩ chỉ có quyền xem và kê/điều chỉnh đơn thuốc cho các lượt khám do chính mình phụ trách. Vui lòng mở danh sách hàng đợi của phòng khám để chọn đúng lượt khám của bạn.
                </Paragraph>
              )}
            </div>
          }
          action={
            <Space direction="vertical">
              <Button type="primary" onClick={() => navigate('/appointments')}>
                Mở Hàng đợi & Lượt khám của tôi
              </Button>
              <Button onClick={loadData}>Thử lại</Button>
            </Space>
          }
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

  const recordStatusLabel = recordLocked ? 'Đã khóa' : (record?.status === 'DRAFT' ? 'Đang mở (Bản nháp)' : (record?.status || 'Đang mở'))

  return (
    <div style={{ paddingBottom: 40 }}>
      <div className="page-header" style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
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
          <Text type="secondary">
            {editingPrescription
              ? 'Sửa đổi liều dùng, số lượng hoặc thêm/bớt thuốc khi đơn đang ở trạng thái chờ cấp phát.'
              : 'Hồ sơ gắn liền với bệnh án hiện tại, đảm bảo an toàn thông tin điều trị.'}
          </Text>
        </div>
        <Space wrap>
          {editingPrescription && (
            <Button icon={<RollbackOutlined />} onClick={cancelEditMode}>
              Hủy điều chỉnh
            </Button>
          )}
          {canPrescribe && (
            <Tooltip title={!canSubmit ? submitStatus.reason : ''}>
              <Button
                type="primary"
                size="large"
                loading={saving || checkingInteractions}
                disabled={!canSubmit}
                icon={<CheckCircleOutlined />}
                onClick={handleSaveClick}
              >
                {editingPrescription ? 'Lưu điều chỉnh đơn thuốc' : 'Tạo đơn thuốc'}
              </Button>
            </Tooltip>
          )}
          {isAssignedDoctor && prescriptions.length > 0 && !editingPrescription && (
            <Button
              danger
              icon={<LockOutlined />}
              loading={finalizing}
              disabled={Boolean(prescriptionBlockReason)}
              onClick={finalizeEncounter}
            >
              Khóa bệnh án & hoàn tất khám
            </Button>
          )}
        </Space>
      </div>

      <Card style={{ marginBottom: 16, borderRadius: 8 }}>
        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, lg: 4 }}>
          <Descriptions.Item label="Mã bệnh án"><Text code>{medicalRecordId}</Text></Descriptions.Item>
          <Descriptions.Item label="Mã lượt khám">{encounter?.visit?.visitCode || record?.visitId || '—'}</Descriptions.Item>
          <Descriptions.Item label="Bệnh nhân">
            <Text strong>{encounter?.patient?.fullName || record?.patientName || '—'}</Text> ({encounter?.patient?.patientCode || record?.patientCode || 'BN'})
          </Descriptions.Item>
          <Descriptions.Item label="Bác sĩ phụ trách">{encounter?.doctor?.fullName || record?.doctorName || '—'}</Descriptions.Item>
          <Descriptions.Item label="Chẩn đoán chính" span={2}>
            <Text strong style={{ color: '#1e40af' }}>{diagnosisSummary}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Mã hàng đợi">
            <Text code>{encounter?.queueItem?.id || '—'}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái lượt khám">
            <Tag color={encounter?.queueItem?.status === 'IN_PROGRESS' ? 'processing' : (encounter?.queueItem?.status === 'WAITING_FOR_RESULT' ? 'warning' : 'default')}>
              {queueStatusLabel}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái bệnh án">
            <Tag color={recordLocked ? 'green' : 'blue'}>{recordStatusLabel}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Số đơn thuốc hiện có">
            <Badge count={prescriptions.length} showZero color="#2563eb" />
          </Descriptions.Item>
        </Descriptions>
      </Card>

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
                          borderRadius: 10,
                          borderWidth: 2,
                          borderColor: isComplete ? '#BFDBFE' : '#FDE68A',
                          backgroundColor: item.isOriginal ? '#ffffff' : '#f8fafc',
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
                          <Space size={8} wrap>
                            <Text strong style={{ fontSize: 15, color: '#1E40AF' }}>
                              Thuốc #{index + 1}
                            </Text>

                            {isComplete ? (
                              <Tag color="success" icon={<CheckCircleOutlined />}>
                                Đủ thông tin bắt buộc
                              </Tag>
                            ) : (
                              <Tag color="warning" icon={<ExclamationCircleOutlined />}>
                                Chưa đủ trường bắt buộc
                              </Tag>
                            )}

                            {editingPrescription && (
                              <Tag color={item.isOriginal ? 'default' : 'cyan'}>
                                {item.isOriginal ? 'Thuốc trong đơn gốc' : 'Thuốc thêm mới'}
                              </Tag>
                            )}

                            {selectedMed && (() => {
                              const avail = getAvailableStock(selectedMed)
                              if (avail <= 0) {
                                return (
                                  <Tag color="red" icon={<CloseCircleOutlined />}>
                                    HẾT HÀNG (Tồn khả dụng: 0 {unit})
                                  </Tag>
                                )
                              }
                              if (item.quantity > avail) {
                                return (
                                  <Tag color="volcano" icon={<WarningOutlined />}>
                                    Vượt quá tồn kho (Còn {avail} {unit})
                                  </Tag>
                                )
                              }
                              return (
                                <Text type="secondary" style={{ fontSize: 13 }}>
                                  (Tồn khả dụng: <strong style={{ color: '#16a34a' }}>{avail}</strong> {unit})
                                </Text>
                              )
                            })()}
                          </Space>

                          <Tooltip title={items.length <= 1 ? 'Đơn thuốc phải có ít nhất 1 thuốc' : 'Bỏ thuốc này khỏi đơn'}>
                            <Button
                              danger
                              size="small"
                              icon={<DeleteOutlined />}
                              disabled={!canPrescribe || items.length <= 1 || checkingInteractions || saving}
                              onClick={() => handleRemoveItem(item.clientId)}
                            >
                              Bỏ thuốc
                            </Button>
                          </Tooltip>
                        </div>

                        {/* Hàng 1: Thuốc & Cách dùng */}
                        <Row gutter={[12, 12]} style={{ marginBottom: 10 }}>
                          <Col xs={24} md={15}>
                            <Form.Item
                              label={<span>1. Chọn thuốc <span style={{ color: '#ef4444' }}>*</span></span>}
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
                                  return {
                                    value: medicine.id,
                                    disabled: isOut,
                                    label: isOut
                                      ? `${medicine.medicineName} — ${medicine.strength ? `${medicine.strength} ` : ''}— Hết hàng`
                                      : `${medicine.medicineName} — ${medicine.strength ? `${medicine.strength} ` : ''}— Còn ${availStock} ${medicine.unit || 'viên'}`,
                                  }
                                })}
                                placeholder="Tìm kiếm thuốc theo tên hoặc hoạt chất..."
                              />
                            </Form.Item>
                          </Col>

                          <Col xs={24} md={9}>
                            <Form.Item
                              label={<span>2. Cách dùng <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <Select
                                style={{ width: '100%' }}
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.route}
                                onChange={(value) => handleItemChange(item.clientId, 'route', value)}
                                options={ROUTE_OPTIONS}
                                placeholder="Chọn cách dùng (bắt buộc)"
                              />
                            </Form.Item>
                          </Col>
                        </Row>

                        {/* Thông tin quy cách / hàm lượng thuốc đang chọn */}
                        {selectedMed && (
                          <div style={{ background: '#F1F5F9', padding: '8px 12px', borderRadius: 8, marginBottom: 12, display: 'flex flexWrap', gap: 8, alignItems: 'center' }}>
                            <Tag color="blue" style={{ margin: 0 }}>
                              Hàm lượng: <strong>{selectedMed.strength || 'Theo quy cách'}</strong>
                            </Tag>
                            <Tag color="cyan" style={{ margin: 0 }}>
                              Hoạt chất: <strong>{selectedMed.activeIngredient || selectedMed.medicineName}</strong>
                            </Tag>
                            <Tag color="purple" style={{ margin: 0 }}>
                              Đơn vị: <strong>{selectedMed.unit || 'viên'}</strong>
                            </Tag>
                          </div>
                        )}

                        {/* Hàng 2: Liều dùng, Tần suất, Số ngày & Tổng số lượng */}
                        <Row gutter={[12, 12]} style={{ marginBottom: 10 }}>
                          <Col xs={24} sm={12} md={6}>
                            <Form.Item
                              label={<span>3. Liều một lần <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <Input
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.dosage}
                                onChange={(event) => handleItemChange(item.clientId, 'dosage', event.target.value)}
                                placeholder={`VD: 1 ${unit}/lần`}
                              />
                            </Form.Item>
                            <div style={{ marginTop: 4 }}>
                              <Space size={4} wrap>
                                {[`1 ${unit}`, `2 ${unit}`, `1/2 ${unit}`].map((sug) => (
                                  <Tag
                                    key={sug}
                                    style={{ cursor: 'pointer', fontSize: 11, margin: 0 }}
                                    onClick={() => handleItemChange(item.clientId, 'dosage', sug)}
                                  >
                                    {sug}
                                  </Tag>
                                ))}
                              </Space>
                            </div>
                          </Col>

                          <Col xs={24} sm={12} md={6}>
                            <Form.Item
                              label={<span>4. Số lần / ngày <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <InputNumber
                                min={1}
                                max={24}
                                step={1}
                                precision={0}
                                style={{ width: '100%' }}
                                placeholder="VD: 2"
                                addonAfter="lần/ngày"
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.frequency}
                                onChange={(value) => handleItemChange(item.clientId, 'frequency', value)}
                              />
                            </Form.Item>
                          </Col>

                          <Col xs={24} sm={12} md={6}>
                            <Form.Item
                              label={<span>5. Số ngày dùng <span style={{ color: '#ef4444' }}>*</span></span>}
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
                                addonAfter="ngày"
                              />
                            </Form.Item>
                          </Col>

                          <Col xs={24} sm={12} md={6}>
                            <Form.Item
                              label={<span>6. Tổng số lượng <span style={{ color: '#ef4444' }}>*</span></span>}
                              style={{ marginBottom: 0 }}
                            >
                              <InputNumber
                                min={1}
                                precision={0}
                                disabled={!canPrescribe || checkingInteractions || saving}
                                value={item.quantity}
                                onChange={(value) => handleItemChange(item.clientId, 'quantity', value)}
                                style={{ width: '100%' }}
                                addonAfter={unit}
                              />
                            </Form.Item>
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
                            {Number(item.frequency) > 0 && Number(item.durationDays) > 0 && (
                              <div style={{ marginTop: 4 }}>
                                <Text
                                  type="secondary"
                                  style={{ fontSize: 11, cursor: 'pointer', color: '#2563EB' }}
                                  onClick={() => {
                                    const qty = Number(item.frequency) * Number(item.durationDays)
                                    handleItemChange(item.clientId, 'quantity', qty)
                                  }}
                                >
                                  ⚡ Gợi ý: {Number(item.frequency) * Number(item.durationDays)} {unit}
                                </Text>
                              </div>
                            )}
                          </Col>
                        </Row>

                        {/* Hàng 3: Hướng dẫn dùng chi tiết */}
                        <Form.Item
                          label="7. Hướng dẫn dùng & Lời dặn chi tiết"
                          style={{ marginBottom: 4 }}
                        >
                          <Input
                            disabled={!canPrescribe || checkingInteractions || saving}
                            value={item.instructions}
                            onChange={(event) => handleItemChange(item.clientId, 'instructions', event.target.value)}
                            placeholder="Ví dụ: Uống sau khi ăn no 30 phút, uống với nhiều nước..."
                          />
                        </Form.Item>
                        <Space size={4} wrap style={{ marginBottom: 4 }}>
                          {['Uống sau ăn no', 'Uống trước ăn 30 phút', 'Uống trước khi đi ngủ', 'Uống nhiều nước'].map((preset) => (
                            <Tag
                              key={preset}
                              style={{ cursor: 'pointer', fontSize: 11 }}
                              onClick={() => handleItemChange(item.clientId, 'instructions', preset)}
                            >
                              + {preset}
                            </Tag>
                          ))}
                        </Space>

                        {/* Cảnh báo tồn kho */}
                        {selectedMed && (() => {
                          const avail = getAvailableStock(selectedMed)
                          if (avail <= 0) {
                            return (
                              <Alert
                                type="error"
                                showIcon
                                icon={<StopOutlined />}
                                message={`Thuốc "${selectedMed.medicineName}" hiện đã HẾT HÀNG (tồn khả dụng = 0). Vui lòng đổi sang thuốc khác.`}
                                style={{ marginTop: 10 }}
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
                                style={{ marginTop: 10 }}
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
                      style={{ width: '100%', marginTop: 8 }}
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
                          size="large"
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

      {/* Modal thông báo Cấp mã đơn thuốc điện tử thành công */}
      <Modal
        open={issuedPrescriptionModalOpen}
        onCancel={() => setIssuedPrescriptionModalOpen(false)}
        footer={[
          justIssuedPrescription?.interconnectionStatus === 'SUCCESS' ? (
            <Tag key="interconnected" color="success" icon={<CheckCircleOutlined />} style={{ padding: '6px 12px', fontSize: 13 }}>
              Đã liên thông ({justIssuedPrescription.interconnectionReceiptCode})
            </Tag>
          ) : (
            <Button
              key="sendInterconnection"
              type="primary"
              icon={<CloudUploadOutlined />}
              loading={sendingInterconnectionId === justIssuedPrescription?.id}
              onClick={async () => {
                if (justIssuedPrescription) {
                  await handleSendPrescriptionToInterconnection(justIssuedPrescription)
                }
              }}
              style={{ backgroundColor: '#0284c7', borderColor: '#0284c7' }}
            >
              Gửi liên thông ngay
            </Button>
          ),
          <Button
            key="print"
            type="default"
            icon={<PrinterOutlined />}
            onClick={() => {
              setIssuedPrescriptionModalOpen(false)
              if (justIssuedPrescription) {
                setSelectedPrescriptionForPrint(justIssuedPrescription)
                setPrintModalOpen(true)
              }
            }}
          >
            In đơn thuốc
          </Button>,
          <Button
            key="detail"
            icon={<EyeOutlined />}
            onClick={() => {
              setIssuedPrescriptionModalOpen(false)
              if (justIssuedPrescription) {
                openDetailModal(justIssuedPrescription)
              }
            }}
          >
            Xem chi tiết đơn
          </Button>,
          <Button
            key="close"
            onClick={() => setIssuedPrescriptionModalOpen(false)}
          >
            Đóng & Tiếp tục
          </Button>,
        ]}
        width={560}
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
                  size="large"
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
    </div>
  )
}

export default PrescriptionPage
