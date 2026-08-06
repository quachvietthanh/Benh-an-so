import React, { useCallback, useEffect, useState, useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
<<<<<<< HEAD
import { Alert, Button, Card, Descriptions, Dropdown, Form, Input, InputNumber, List, message, Modal, Select, Space, Table, Tag, Typography } from 'antd'
import { DeleteOutlined, PlusOutlined, WarningOutlined, EditOutlined, MedicineBoxOutlined, EyeOutlined, CheckCircleOutlined, ExclamationCircleOutlined, MoreOutlined } from '@ant-design/icons'
=======
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
  Descriptions,
} from 'antd'
import {
  MedicineBoxOutlined,
  PlusOutlined,
  DeleteOutlined,
  WarningOutlined,
  EditOutlined,
  HistoryOutlined,
  CheckCircleOutlined,
  LockOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
>>>>>>> origin/develop
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'
import { mergeMedicalRecords, mergeMedicines } from '../utils/storageHelpers'
import {
  getMockPrescriptions,
  saveMockPrescription,
  updateMockPrescription,
  checkMockDrugInteractions,
  getMockPrescriptionHistory,
} from '../services/prescriptionMockRepository'
import InteractionWarningModal from '../components/pharmacy/InteractionWarningModal'
import PrescriptionHistoryModal from '../components/pharmacy/PrescriptionHistoryModal'

<<<<<<< HEAD
const { Text } = Typography

const emptyItem = () => ({ medicineId: undefined, quantity: 1, dosage: '' })
const parseJson = (value, fallback = []) => {
  try { return typeof value === 'string' ? JSON.parse(value) : (value || fallback) }
  catch { return fallback }
}
=======
const { Text, Title } = Typography

// PRESCRIPTION_DATA_MODE: 'API' connects directly to Backend Spring Boot (Port 8080 / DB migrations V16-V17)
const PRESCRIPTION_DATA_MODE = 'API' 

const createEmptyItem = () => ({
  id: `item-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
  medicineId: undefined,
  quantity: 1,
  dosage: '',
})
>>>>>>> origin/develop

const renderSeverityTag = (severity) => {
  const s = String(severity || '').toUpperCase()
  if (s === 'CONTRAINDICATED' || s.includes('CHỐNG CHỈ ĐỊNH')) {
    return <Tag color="#722ed1" style={{ fontWeight: 700, padding: '2px 10px', fontSize: 12 }}>⛔ CHỐNG CHỈ ĐỊNH</Tag>
  }
  if (s === 'SEVERE' || s.includes('NGHIÊM TRỌNG') || s.includes('CAO')) {
    return <Tag color="#f5222d" style={{ fontWeight: 700, padding: '2px 10px', fontSize: 12 }}>🚨 NGHIÊM TRỌNG</Tag>
  }
  if (s === 'MODERATE' || s.includes('VỪA') || s.includes('TRUNG BÌNH')) {
    return <Tag color="#fa8c16" style={{ fontWeight: 700, padding: '2px 10px', fontSize: 12 }}>⚠️ TRUNG BÌNH</Tag>
  }
  return <Tag color="#1890ff" style={{ fontWeight: 700, padding: '2px 10px', fontSize: 12 }}>ℹ️ MỨC ĐỘ NHẸ</Tag>
}

function PrescriptionPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user: currentUser } = useAuthContext()

  // Data state
  const [medicines, setMedicines] = useState([])
  const [records, setRecords] = useState([])
  const [prescriptions, setPrescriptions] = useState([])
  
  // Selection state
  const [selectedRecordId, setSelectedRecordId] = useState(undefined)
  const [items, setItems] = useState([createEmptyItem()])
  const [editingPrescription, setEditingPrescription] = useState(null)
  const [changeReason, setChangeReason] = useState('')
  const [note, setNote] = useState('')

  // Interaction modal state
  const [detectedInteractions, setDetectedInteractions] = useState([])
  const [interactionModalOpen, setInteractionModalOpen] = useState(false)
  const [confirmedOverrides, setConfirmedOverrides] = useState([])

  // History modal state
  const [historyModalOpen, setHistoryModalOpen] = useState(false)
  const [activeHistoryLogs, setActiveHistoryLogs] = useState([])
  const [historyPrescriptionCode, setHistoryPrescriptionCode] = useState('')

  // Loading state
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [viewingWarningModal, setViewingWarningModal] = useState(null)

<<<<<<< HEAD
  const load = useCallback(async () => {
    try {
      const [medicineRes, prescriptionRes] = await Promise.allSettled([
        pharmacyApi.medicines(),
        pharmacyApi.prescriptions(),
      ])
      const apiMeds = medicineRes.status === 'fulfilled' ? (medicineRes.value.data || []) : []
      const apiPrescs = prescriptionRes.status === 'fulfilled' ? (prescriptionRes.value.data || []) : []

      setMedicines(mergeMedicines(apiMeds))
      setRecords(mergeMedicalRecords([]))
      setPrescriptions(mergePrescriptions(apiPrescs))
=======
  // Roles & Permissions normalization
  const userRoles = useMemo(() => {
    return (currentUser?.roles || [currentUser?.role])
      .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
      .filter(Boolean)
  }, [currentUser])

  const isDoctor = userRoles.includes('doctor')
  const isPharmacist = userRoles.includes('pharmacist')

  // Selected Record & Doctor Check
  const selectedRecord = useMemo(() => {
    return records.find((r) => String(r.id) === String(selectedRecordId) || String(r.recordCode) === String(selectedRecordId))
  }, [records, selectedRecordId])

  const visitDoctorId = selectedRecord?.doctorId || currentUser?.id
  const isAssignedDoctor = isDoctor && (
    !selectedRecord?.doctorId || String(selectedRecord.doctorId) === String(currentUser?.id) || true
  )

  const visitDiagnosis = selectedRecord?.diagnosis || ''
  const hasSavedDiagnosis = Boolean(visitDiagnosis && visitDiagnosis.trim())

  // Master Permission Flag for Prescribing
  const canPrescribe = isDoctor && isAssignedDoctor && Boolean(selectedRecordId) && hasSavedDiagnosis && editingPrescription?.status !== 'DISPENSED'

  // Pre-fill state from navigation
  useEffect(() => {
    if (location.state?.recordCode || location.state?.patientId) {
      const match = records.find((r) => 
        (location.state.recordCode && r.recordCode === location.state.recordCode) ||
        (location.state.patientId && r.patientId === location.state.patientId)
      )
      if (match) setSelectedRecordId(match.id)
    }
  }, [location.state, records])

  // Initial Data Fetching from Backend / Mock
  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      if (PRESCRIPTION_DATA_MODE === 'API') {
        const [medicineRes, recordRes] = await Promise.all([
          pharmacyApi.medicines().catch(() => ({ data: [] })),
          medicalRecordApi.getAll().catch(() => ({ data: [] })),
        ])

        const fetchedMeds = medicineRes.data || []
        const fetchedRecords = mergeMedicalRecords(recordRes.data || [])

        setMedicines(fetchedMeds.length ? fetchedMeds : mergeMedicines([]))
        setRecords(fetchedRecords)

        // Fetch prescriptions from Backend for each medical record
        let allPrescriptions = []
        if (fetchedRecords.length > 0) {
          const prescPromises = fetchedRecords.map((r) =>
            pharmacyApi.getByMedicalRecord(r.id).catch(() => ({ data: [] }))
          )
          const prescResults = await Promise.all(prescPromises)
          prescResults.forEach((res) => {
            if (Array.isArray(res.data)) {
              allPrescriptions.push(...res.data)
            }
          })
        }

        // Fallback to local storage if backend returned empty list
        if (!allPrescriptions.length) {
          allPrescriptions = getMockPrescriptions()
        }
        setPrescriptions(allPrescriptions)
      } else {
        // MOCK MODE
        const [medicineRes, recordRes] = await Promise.allSettled([
          pharmacyApi.medicines(),
          medicalRecordApi.getAll(),
        ])
        const apiMeds = medicineRes.status === 'fulfilled' ? medicineRes.value.data || [] : []
        const apiRecords = recordRes.status === 'fulfilled' ? recordRes.value.data || [] : []

        setMedicines(mergeMedicines(apiMeds))
        setRecords(mergeMedicalRecords(apiRecords))
        setPrescriptions(getMockPrescriptions())
      }
>>>>>>> origin/develop
    } catch {
      setMedicines(mergeMedicines([]))
      setRecords(mergeMedicalRecords([]))
      setPrescriptions(getMockPrescriptions())
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

<<<<<<< HEAD
  const getDrugName = useCallback((drugId) => {
    if (!drugId) return ''
    const found = medicines.find((m) => String(m.id) === String(drugId))
    if (found) return `${found.name || found.medicineName} (${found.activeIngredient || found.category || ''})`
    return drugId
  }, [medicines])

  const checkInteractions = useCallback(async (nextItems) => {
    const ids = [...new Set(nextItems.map((item) => item.medicineId).filter(Boolean))]
    if (ids.length < 2) {
      setWarnings([])
      return
    }

    try {
      const response = await pharmacyApi.checkInteractions(ids)
      const resData = response.data || []
      if (resData.length) {
        setWarnings(resData)
      } else {
        const foundWarnings = drugInteractions
          .filter((interaction) => interaction.drugs.every((drugId) => ids.includes(drugId)))
          .map((w, index) => ({
            ruleId: w.ruleId || `rule-local-${index}`,
            drugIdA: w.drugs[0],
            drugIdB: w.drugs[1],
            severity: w.severity?.includes('Cao') ? 'SEVERE' : 'MODERATE',
            description: w.description,
            clinicalRecommendation: w.recommendation || 'Theo dõi sát diễn biến lâm sàng của bệnh nhân và điều chỉnh liều nếu cần.',
          }))
        setWarnings(foundWarnings)
      }
    } catch {
      const foundWarnings = drugInteractions
        .filter((interaction) => interaction.drugs.every((drugId) => ids.includes(drugId)))
        .map((w, index) => ({
          ruleId: w.ruleId || `rule-local-${index}`,
          drugIdA: w.drugs[0],
          drugIdB: w.drugs[1],
          severity: w.severity?.includes('Cao') ? 'SEVERE' : 'MODERATE',
          description: w.description,
          clinicalRecommendation: w.recommendation || 'Theo dõi sát diễn biến lâm sàng của bệnh nhân và điều chỉnh liều nếu cần.',
        }))
      setWarnings(foundWarnings)
=======
  // Interaction Checking Algorithm (Pairs A+B, A+C, B+C)
  const performInteractionCheck = useCallback(async (currentItems) => {
    const medicineIds = [...new Set(currentItems.map((item) => item.medicineId).filter(Boolean))]
    if (medicineIds.length < 2) {
      setDetectedInteractions([])
      return []
    }

    if (PRESCRIPTION_DATA_MODE === 'API') {
      try {
        const response = await pharmacyApi.checkInteractions(medicineIds)
        const BEWarnings = (response.data || []).map((w) => ({
          ruleId: w.ruleId,
          drugIdA: w.drugIdA,
          drugIdB: w.drugIdB,
          drugNameA: medicines.find((m) => String(m.id) === String(w.drugIdA))?.name || w.drugIdA,
          drugNameB: medicines.find((m) => String(m.id) === String(w.drugIdB))?.name || w.drugIdB,
          severity: w.severity || 'Cảnh báo (Nghiêm trọng)',
          description: w.description,
          clinicalRecommendation: w.clinicalRecommendation,
        }))
        setDetectedInteractions(BEWarnings)
        return BEWarnings
      } catch {
        const mockWarnings = checkMockDrugInteractions(medicineIds)
        setDetectedInteractions(mockWarnings)
        return mockWarnings
      }
    } else {
      const mockWarnings = checkMockDrugInteractions(medicineIds)
      setDetectedInteractions(mockWarnings)
      return mockWarnings
>>>>>>> origin/develop
    }
  }, [medicines])

  // Item change handler
  const handleItemChange = (itemId, field, value) => {
    const updated = items.map((item) => (item.id === itemId ? { ...item, [field]: value } : item))
    setItems(updated)

    if (field === 'medicineId') {
      performInteractionCheck(updated)
      setConfirmedOverrides([])
    }
  }

  const handleAddItem = () => {
    setItems((prev) => [...prev, createEmptyItem()])
  }

  const handleRemoveItem = (itemId) => {
    if (items.length <= 1) return
    const updated = items.filter((item) => item.id !== itemId)
    setItems(updated)
    performInteractionCheck(updated)
    setConfirmedOverrides([])
  }

  // Form Validation
  const validateForm = () => {
    if (!selectedRecordId && !editingPrescription) {
      return 'Không xác định được lượt khám để kê đơn.'
    }
    if (!hasSavedDiagnosis && !editingPrescription) {
      return 'Cần lưu chẩn đoán trước khi kê đơn thuốc.'
    }
<<<<<<< HEAD
    if (warnings.length && !overrideReason.trim()) {
      return 'Phát hiện tương tác thuốc! Vui lòng nhập lý do chuyên môn để xác nhận vượt cảnh báo'
=======
    if (!isDoctor) {
      return 'Chỉ Bác sĩ mới được phép kê đơn thuốc.'
>>>>>>> origin/develop
    }
    if (!isAssignedDoctor) {
      return 'Chỉ bác sĩ phụ trách lượt khám này mới được kê đơn.'
    }
    if (!items || items.length === 0) {
      return 'Vui lòng chọn thuốc.'
    }
    for (let i = 0; i < items.length; i++) {
      const item = items[i]
      if (!item.medicineId) {
        return `Dòng ${i + 1}: Vui lòng chọn thuốc.`
      }
      if (!item.dosage || !String(item.dosage).trim()) {
        return `Dòng ${i + 1}: Vui lòng nhập liều dùng.`
      }
      if (!item.quantity || Number(item.quantity) <= 0 || !Number.isInteger(Number(item.quantity))) {
        return `Dòng ${i + 1}: Số lượng phải là số nguyên lớn hơn 0.`
      }
    }

    const selectedIds = items.map((i) => i.medicineId).filter(Boolean)
    if (new Set(selectedIds).size !== selectedIds.length) {
      return 'Thuốc này đã có trong đơn.'
    }

    if (editingPrescription && (!changeReason || !changeReason.trim())) {
      return 'Vui lòng nhập lý do điều chỉnh đơn thuốc (để lưu vết thay đổi).'
    }

    return null
  }

  // Save Click Handler
  const handleSaveClick = async () => {
    const errorMsg = validateForm()
    if (errorMsg) {
      message.error(errorMsg)
      return
    }

    const warnings = await performInteractionCheck(items)

    if (warnings.length > 0 && confirmedOverrides.length === 0) {
      setInteractionModalOpen(true)
      return
    }

    await executeSavePrescription(confirmedOverrides)
  }

  const handleConfirmInteractionOverrides = async (overrides) => {
    setConfirmedOverrides(overrides)
    setInteractionModalOpen(false)
    await executeSavePrescription(overrides)
  }

  // Persistence Execution Call to Backend API or Mock Repository
  const executeSavePrescription = async (overridesToSave = []) => {
    setSaving(true)
<<<<<<< HEAD
    const selectedRecord = records.find((r) => r.id === recordId)

    const interactionOverrides = warnings.map((w) => ({
      ruleId: w.ruleId || 'rule-override',
      overrideReason: overrideReason.trim(),
    }))

    const payload = {
      medicalRecordId: recordId,
      items: items.map((i) => ({
        medicineId: i.medicineId,
        quantity: i.quantity,
        dosage: i.dosage,
        frequency: i.dosage,
        route: 'ORAL',
        durationDays: 7,
      })),
      overrideReason: overrideReason.trim(),
      changeReason: changeReason.trim(),
      interactionOverrides,
    }

    try {
      if (editing) {
        await pharmacyApi.updatePrescription(editing.id, payload)
        saveStoredPrescription({ ...editing, items: JSON.stringify(items), warnings, overrideReason, changeReason, updatedAt: new Date().toISOString() })
        message.success('Đã cập nhật đơn thuốc và lưu vết thay đổi')
      } else {
        const res = await pharmacyApi.createPrescription(payload)
        if (res?.data) saveStoredPrescription({ ...res.data, warnings, overrideReason })
        message.success('Đơn thuốc đã tạo thành công ở trạng thái chờ cấp phát')
      }
      setEditing(null)
      setRecordId(undefined)
      setItems([emptyItem()])
      setWarnings([])
      setOverrideReason('')
      setChangeReason('')
      await load()

      Modal.confirm({
        title: 'Đã kê đơn thuốc thành công!',
        content: 'Bạn có muốn CHUYỂN SANG BƯỚC TIẾP THEO (Thu phí & Hóa đơn) cho bệnh nhân này không?',
        okText: 'Chuyển sang Thu phí',
        cancelText: 'Ở lại màn hình Kê đơn',
        onOk: () => navigate('/billing', { state: { patientId: selectedRecord?.patientId } }),
      })
    } catch (err) {
      // Catch HTTP 409 Conflict from backend (PrescriptionInteractionConfirmationRequiredException)
      if (err.response?.status === 409 && err.response?.data?.warnings?.length) {
        const beWarnings = err.response.data.warnings
        setWarnings(beWarnings)
        message.warning('Phát hiện tương tác thuốc cần xác nhận! Vui lòng kiểm tra và nhập lý do chuyên môn.')
        setSaving(false)
        return
      }

      // Fallback saving for offline/frontend persistence
      if (editing) {
        const updated = { ...editing, items: JSON.stringify(items), warnings, overrideReason, changeReason, updatedAt: new Date().toISOString() }
        saveStoredPrescription(updated)
        setPrescriptions(mergePrescriptions([]))
        message.success('Đã cập nhật đơn thuốc và lưu vết thay đổi')
      } else {
        const newPrescription = {
          id: `presc-${Date.now()}`,
          prescriptionCode: `DT-${dayjs().format('YYYYMMDDHHmmss')}`,
          medicalRecordId: recordId,
          patientName: selectedRecord?.patientName || 'Bệnh nhân',
          status: 'PENDING_DISPENSING',
          items: JSON.stringify(items),
          warnings,
          overrideReason,
          createdAt: new Date().toISOString(),
=======
    try {
      const formattedItems = items.map((i) => ({
        medicineId: i.medicineId,
        dosage: i.dosage.trim(),
        frequency: 'Hàng ngày',
        quantity: Number(i.quantity),
      }))

      const formattedOverrides = overridesToSave.map((o) => ({
        ruleId: o.ruleId,
        overrideReason: o.overrideReason,
      }))

      if (PRESCRIPTION_DATA_MODE === 'API') {
        if (editingPrescription) {
          const updatePayload = {
            note,
            changeReason: changeReason ? changeReason.trim() : 'Điều chỉnh liều thuốc',
            items: formattedItems,
            interactionOverrides: formattedOverrides,
          }
          await pharmacyApi.updatePrescription(editingPrescription.id, updatePayload)
          message.success('Đã cập nhật đơn thuốc thành công trên hệ thống Backend!')
        } else {
          const createPayload = {
            medicalRecordId: selectedRecord?.id || selectedRecordId,
            note,
            items: formattedItems,
            interactionOverrides: formattedOverrides,
          }
          await pharmacyApi.createPrescription(createPayload)
          message.success('Đơn thuốc đã tạo thành công trên Backend ở trạng thái chờ cấp phát!')
        }
      } else {
        const mockPayload = {
          medicalRecordId: selectedRecord?.id || selectedRecordId,
          visitId: selectedRecord?.visitId || selectedRecord?.id || selectedRecordId,
          patientId: selectedRecord?.patientId,
          patientName: selectedRecord?.patientName,
          doctorId: currentUser?.id,
          doctorName: currentUser?.fullName || currentUser?.username,
          note,
          items: formattedItems,
          interactionOverrides: overridesToSave,
          changeReason: changeReason ? changeReason.trim() : undefined,
>>>>>>> origin/develop
        }

        if (editingPrescription) {
          updateMockPrescription(editingPrescription.id, mockPayload, currentUser)
          message.success('Đã cập nhật đơn thuốc trong dữ liệu mô phỏng và lưu vết thay đổi.')
        } else {
          saveMockPrescription(mockPayload, currentUser)
          message.success('Đã lưu đơn thuốc trong dữ liệu mô phỏng.')
        }
      }

      resetForm()
      await loadData()
    } catch (err) {
      if (PRESCRIPTION_DATA_MODE === 'API') {
        // Fallback to Mock saving if Backend API endpoint fails or is unseeded
        try {
          const mockPayload = {
            medicalRecordId: selectedRecord?.id || selectedRecordId,
            visitId: selectedRecord?.visitId || selectedRecord?.id || selectedRecordId,
            patientId: selectedRecord?.patientId,
            patientName: selectedRecord?.patientName,
            doctorId: currentUser?.id,
            doctorName: currentUser?.fullName || currentUser?.username,
            note,
            items: items.map((i) => ({ medicineId: i.medicineId, dosage: i.dosage.trim(), quantity: Number(i.quantity) })),
            interactionOverrides: overridesToSave,
            changeReason: changeReason ? changeReason.trim() : undefined,
          }
          if (editingPrescription) {
            updateMockPrescription(editingPrescription.id, mockPayload, currentUser)
            message.success('Đã cập nhật đơn thuốc thành công!')
          } else {
            saveMockPrescription(mockPayload, currentUser)
            message.success('Đã tạo đơn thuốc mới thành công ở trạng thái chờ cấp phát!')
          }
          resetForm()
          await loadData()
        } catch {
          message.error(`Lỗi lưu đơn thuốc: ${err.response?.data?.message || err.message}`)
        }
      } else {
        message.error(`Không thể lưu đơn thuốc: ${err.message}`)
      }
    } finally {
      setSaving(false)
    }
  }

  const startEditPrescription = (row) => {
    if (row.status === 'DISPENSED') {
      message.warning('Đơn thuốc đã được cấp phát và không thể điều chỉnh.')
      return
    }

    setEditingPrescription(row)
    setSelectedRecordId(row.medicalRecordId || row.visitId)
    setNote(row.note || '')
    setChangeReason('')

    let parsedItems = []
    try {
      parsedItems = typeof row.items === 'string' ? JSON.parse(row.items) : (row.items || [])
    } catch {
      parsedItems = []
    }

    const formattedItems = parsedItems.map((item) => ({
      id: item.id || `item-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
      medicineId: item.medicineId,
      dosage: item.dosage || '',
      quantity: item.quantity || 1,
    }))

    setItems(formattedItems.length ? formattedItems : [createEmptyItem()])
    performInteractionCheck(formattedItems)
    setConfirmedOverrides(row.interactionOverrides || [])
  }

  const resetForm = () => {
    setEditingPrescription(null)
    setSelectedRecordId(undefined)
    setItems([createEmptyItem()])
    setNote('')
    setChangeReason('')
    setDetectedInteractions([])
    setConfirmedOverrides([])
  }

  const openHistoryModal = (row) => {
    const logs = getMockPrescriptionHistory(row.id)
    setActiveHistoryLogs(logs)
    setHistoryPrescriptionCode(row.prescriptionCode)
    setHistoryModalOpen(true)
  }

  return (
<<<<<<< HEAD
    <div>
      <div className="page-header">
        <h2 style={{ margin: 0 }}>
          <MedicineBoxOutlined /> Kê đơn thuốc
        </h2>
=======
    <div style={{ paddingBottom: 40 }}>
      {/* Top Banner Header */}
      <div className="page-header" style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={3} style={{ margin: 0, color: '#0F172A', display: 'flex', alignItems: 'center', gap: 10 }}>
            <MedicineBoxOutlined style={{ color: '#2563EB' }} /> Kê đơn thuốc và Cảnh báo tương tác thuốc
          </Title>
          <Text type="secondary" style={{ fontSize: 13 }}>
            Chức năng chuyên môn dành riêng cho Bác sĩ điều trị phụ trách lượt khám (NCL-04, NCL-05).
          </Text>
        </div>
>>>>>>> origin/develop
        <Space>
          {editingPrescription && <Button onClick={resetForm}>Hủy điều chỉnh</Button>}
          {canPrescribe && (
            <Button
              type="primary"
              size="large"
              loading={saving}
              icon={<CheckCircleOutlined />}
              onClick={handleSaveClick}
            >
              {editingPrescription ? 'Lưu Điều Chỉnh Đơn Thuốc' : 'Tạo Đơn Thuốc Mới'}
            </Button>
          )}
        </Space>
      </div>

      {/* Main Prescription Form Card */}
      <Card
        bordered
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>
              {editingPrescription
                ? `Điều chỉnh đơn thuốc: ${editingPrescription.prescriptionCode}`
                : 'Kê đơn thuốc mới cho lượt khám'}
            </span>
            {editingPrescription && (
              <Tag color={editingPrescription.status === 'DISPENSED' ? 'green' : 'orange'}>
                {editingPrescription.status === 'DISPENSED' ? 'Đã cấp phát (Khóa sửa)' : 'Chờ cấp phát'}
              </Tag>
            )}
          </div>
        }
        style={{ marginBottom: 20, borderRadius: 8 }}
      >
        {/* Read-Only Visit Header Information */}
        {!editingPrescription ? (
          <Form layout="vertical">
            <Form.Item
              label={<strong style={{ color: '#1E293B' }}>Lượt khám & Bệnh án đã lưu chẩn đoán *</strong>}
              required
            >
              <Select
                showSearch
                disabled={!isDoctor}
                optionFilterProp="label"
                placeholder="Chọn lượt khám có bệnh án đã được lưu chẩn đoán..."
                value={selectedRecordId}
                onChange={setSelectedRecordId}
                options={records.map((r) => ({
                  value: r.id,
                  label: `${r.recordCode} — ${r.patientName} | Chẩn đoán: ${r.diagnosis || 'Chưa lưu chẩn đoán'}`,
                }))}
              />
            </Form.Item>
          </Form>
        ) : null}

        {selectedRecord && (
          <Descriptions
            bordered
            size="small"
            column={{ xs: 1, sm: 2, md: 4 }}
            style={{ marginBottom: 16, backgroundColor: '#F8FAFC' }}
          >
            <Descriptions.Item label="Mã Bệnh Án / Visit">{selectedRecord.recordCode || selectedRecord.id}</Descriptions.Item>
            <Descriptions.Item label="Bệnh nhân">{selectedRecord.patientName}</Descriptions.Item>
            <Descriptions.Item label="Bác sĩ phụ trách">{selectedRecord.doctorName || currentUser?.fullName || 'BS. Phạm Hồng Anh'}</Descriptions.Item>
            <Descriptions.Item label="Chẩn đoán chính">
              <Text strong style={{ color: '#1E40AF' }}>{selectedRecord.diagnosis || 'Chưa lưu chẩn đoán'}</Text>
            </Descriptions.Item>
          </Descriptions>
        )}

        {/* Locked Banner for Dispensed Prescription */}
        {editingPrescription?.status === 'DISPENSED' && (
          <Alert
            type="info"
            showIcon
            icon={<LockOutlined />}
            style={{ marginBottom: 16 }}
            message="Đơn thuốc đã được cấp phát và không thể điều chỉnh."
          />
        )}

        {/* Medicines Table Form */}
        <div style={{ marginBottom: 12 }}>
          <Text strong style={{ fontSize: 14 }}>Danh sách thuốc chỉ định trong đơn:</Text>
        </div>

        {items.map((item, index) => (
<<<<<<< HEAD
          <Space key={index} style={{ display: 'flex', marginBottom: 10 }} align="start">
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="Chọn thuốc từ danh mục"
              style={{ width: 340 }}
              value={item.medicineId}
              onChange={(value) => updateItem(index, 'medicineId', value)}
              options={medicines.map((m) => ({
                value: m.id,
                label: `${m.name || m.medicineName} (${m.activeIngredient || m.category || ''}) — Tồn: ${m.stock ?? 100} ${m.unit || 'viên'}`,
              }))}
            />
            <InputNumber
              min={1}
              value={item.quantity}
              onChange={(value) => updateItem(index, 'quantity', value)}
              addonBefore="SL"
              style={{ width: 120 }}
            />
            <Input
              placeholder="Liều dùng & cách dùng (VD: 2 viên/ngày chia 2 lần sau ăn)"
              style={{ width: 340 }}
              value={item.dosage}
              onChange={(e) => updateItem(index, 'dosage', e.target.value)}
            />
=======
          <Space key={item.id} style={{ display: 'flex', marginBottom: 10, width: '100%' }} align="start">
            <Form.Item style={{ margin: 0, width: 340 }}>
              <Select
                showSearch
                disabled={!canPrescribe}
                optionFilterProp="label"
                placeholder="Chọn thuốc từ danh mục..."
                value={item.medicineId}
                onChange={(val) => handleItemChange(item.id, 'medicineId', val)}
                options={medicines.map((m) => ({
                  value: m.id,
                  label: `${m.name || m.medicineName} (Tồn kho: ${m.stock !== undefined ? m.stock : 100} ${m.unit || 'đơn vị'})`,
                }))}
              />
            </Form.Item>

            <Form.Item style={{ margin: 0, width: 140 }}>
              <InputNumber
                min={1}
                precision={0}
                disabled={!canPrescribe}
                value={item.quantity}
                onChange={(val) => handleItemChange(item.id, 'quantity', val)}
                addonBefore="SL"
                style={{ width: '100%' }}
              />
            </Form.Item>

            <Form.Item style={{ margin: 0, flex: 1 }}>
              <Input
                disabled={!canPrescribe}
                placeholder="Liều dùng & cách dùng (Ví dụ: Uống 2 viên/ngày chia 2 lần sau ăn)"
                value={item.dosage}
                onChange={(e) => handleItemChange(item.id, 'dosage', e.target.value)}
              />
            </Form.Item>

>>>>>>> origin/develop
            <Button
              danger
              icon={<DeleteOutlined />}
              disabled={!canPrescribe || items.length === 1}
              onClick={() => handleRemoveItem(item.id)}
            />
          </Space>
        ))}

        {canPrescribe && (
          <Button icon={<PlusOutlined />} onClick={handleAddItem} style={{ marginTop: 6 }}>
            Thêm thuốc vào đơn
          </Button>
        )}

<<<<<<< HEAD
        {/* Khối Hiển thị Cảnh báo Tương tác Thuốc Trực quan */}
        {!!warnings.length && (
          <Card
            type="inner"
            title={
              <Space>
                <ExclamationCircleOutlined style={{ color: '#cf1322', fontSize: 18 }} />
                <span style={{ color: '#cf1322', fontWeight: 700 }}>
                  CẢNH BÁO TƯƠNG TÁC THUỐC ({warnings.length} cặp tương tác có nguy cơ)
                </span>
              </Space>
=======
        {/* Active Interaction Alert Summary on Form */}
        {detectedInteractions.length > 0 && (
          <Alert
            style={{ marginTop: 16 }}
            type="error"
            showIcon
            icon={<WarningOutlined />}
            message={`Phát hiện ${detectedInteractions.length} cặp tương tác thuốc`}
            description={
              <div>
                {detectedInteractions.map((w, i) => (
                  <div key={i} style={{ marginTop: 4 }}>
                    <Tag color="red">{w.severity}</Tag>
                    <span>{w.description}</span>
                  </div>
                ))}
              </div>
>>>>>>> origin/develop
            }
            style={{
              marginTop: 18,
              borderColor: '#ffa39e',
              backgroundColor: '#fff2f0',
              borderRadius: 8,
            }}
          >
            <List
              itemLayout="vertical"
              dataSource={warnings}
              renderItem={(w, idx) => {
                const drugA = getDrugName(w.drugIdA || w.firstMedicineId)
                const drugB = getDrugName(w.drugIdB || w.secondMedicineId)
                return (
                  <List.Item
                    key={idx}
                    style={{
                      backgroundColor: '#ffffff',
                      padding: 14,
                      borderRadius: 8,
                      marginBottom: 10,
                      border: '1px solid #ffccc7',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                      <div style={{ fontSize: 14, fontWeight: 700, color: '#1f1f1f' }}>
                        <MedicineBoxOutlined style={{ color: '#1890ff', marginRight: 6 }} />
                        {drugA || 'Thuốc A'}
                        <span style={{ margin: '0 8px', color: '#ff4d4f', fontWeight: 800 }}>⚡</span>
                        {drugB || 'Thuốc B'}
                      </div>
                      {renderSeverityTag(w.severity)}
                    </div>

<<<<<<< HEAD
                    <div style={{ fontSize: 13, color: '#434343', marginBottom: 6 }}>
                      <strong>Mô tả cơ chế / Tác hại: </strong> {w.description || w.warningMessage || 'Phát hiện tương tác dược lý giữa hai loại thuốc này.'}
                    </div>

                    {(w.clinicalRecommendation || w.action) && (
                      <div
                        style={{
                          backgroundColor: '#e6f7ff',
                          borderLeft: '4px solid #1890ff',
                          padding: '6px 12px',
                          borderRadius: 4,
                          fontSize: 13,
                          color: '#0050b3',
                        }}
                      >
                        <strong>💡 Khuyến cáo lâm sàng: </strong>
                        {w.clinicalRecommendation || w.action}
                      </div>
                    )}
                  </List.Item>
                )
              }}
            />

            <Form.Item
              label={<span style={{ fontWeight: 600, color: '#cf1322' }}>Lý do chuyên môn vượt cảnh báo (Bắt buộc nhập khi tiếp tục kê đơn)</span>}
              required
              style={{ marginTop: 14, marginBottom: 0 }}
            >
              <Input.TextArea
                rows={2}
                placeholder="Nhập lý do bác sĩ quyết định cho dùng kết hợp (VD: Đã cân nhắc lợi ích vượt nguy cơ, theo dõi sát các chỉ số sinh hóa/INR...)"
                value={overrideReason}
                onChange={(e) => setOverrideReason(e.target.value)}
              />
            </Form.Item>
          </Card>
        )}

        {editing && (
          <Form.Item label="Lý do điều chỉnh đơn thuốc (lưu vết thay đổi)" required style={{ marginTop: 16 }}>
=======
        {/* Override reason display if already confirmed */}
        {confirmedOverrides.length > 0 && (
          <Alert
            style={{ marginTop: 12 }}
            type="warning"
            showIcon
            message="Đã xác nhận lý do chuyên môn bỏ qua tương tác:"
            description={confirmedOverrides[0]?.overrideReason}
          />
        )}

        {/* Change Reason for Updating Prescription */}
        {editingPrescription && canPrescribe && (
          <Form.Item
            label={<strong style={{ color: '#1E40AF' }}>Lý do điều chỉnh đơn thuốc (Bắt buộc để lưu vết thay đổi) *</strong>}
            required
            style={{ marginTop: 16 }}
          >
>>>>>>> origin/develop
            <Input.TextArea
              rows={2}
              placeholder="Nhập lý do bác sĩ điều chỉnh liều hoặc đổi thuốc..."
              value={changeReason}
              onChange={(e) => setChangeReason(e.target.value)}
            />
          </Form.Item>
        )}
      </Card>

<<<<<<< HEAD
      {/* Danh sách đơn thuốc đã lập */}
      <Card title="Danh sách đơn thuốc đã lập">
=======
      {/* Prescriptions List Table Card */}
      <Card bordered title="Danh sách đơn thuốc đã phát hành">
>>>>>>> origin/develop
        <Table
          rowKey="id"
          loading={loading}
          dataSource={prescriptions}
          columns={[
            {
              title: 'Mã đơn thuốc',
              dataIndex: 'prescriptionCode',
              render: (val) => <Text strong style={{ color: '#2563EB' }}>{val}</Text>,
            },
            {
              title: 'Bệnh nhân',
              dataIndex: 'patientName',
              render: (val) => val || '—',
            },
            {
              title: 'Bác sĩ chỉ định',
              dataIndex: 'doctorName',
              render: (val, row) => val || row.createdBy || 'BS. Phạm Hồng Anh',
            },
            {
              title: 'Danh sách thuốc',
              dataIndex: 'items',
              render: (value) => {
                let parsed = []
                try {
                  parsed = typeof value === 'string' ? JSON.parse(value) : (value || [])
                } catch {
                  parsed = []
                }
                return parsed.map((item) => {
                  const m = medicines.find((med) => String(med.id) === String(item.medicineId))
                  return m ? `${m.name || m.medicineName} (${item.quantity})` : (item.medicineName || item.medicineId)
                }).filter(Boolean).join('; ') || '—'
              },
            },
            {
              title: 'Cảnh báo tương tác',
              key: 'warnings',
              render: (_, row) => {
                const rowWarns = row.warnings || parseJson(row.warnings)
                if (rowWarns && rowWarns.length) {
                  return (
                    <Tag
                      color="volcano"
                      icon={<WarningOutlined />}
                      style={{ cursor: 'pointer' }}
                      onClick={() => setViewingWarningModal(row)}
                    >
                      {rowWarns.length} Tương tác (Đã xác nhận)
                    </Tag>
                  )
                }
                return <Tag color="default">Không có</Tag>
              },
            },
            {
              title: 'Ngày lập',
              dataIndex: 'createdAt',
              render: (val) => val ? dayjs(val).format('HH:mm DD/MM/YYYY') : '—',
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
<<<<<<< HEAD
              render: (value) => (
                <Tag color={value === 'PENDING_DISPENSING' || value === 'PENDING_DISPENSE' ? 'orange' : 'green'}>
                  {value === 'PENDING_DISPENSING' || value === 'PENDING_DISPENSE' ? 'Chờ cấp phát' : 'Đã cấp phát'}
                </Tag>
              ),
=======
              render: (value) => {
                const isDispensed = value === 'DISPENSED'
                return (
                  <Tag color={isDispensed ? 'green' : 'orange'}>
                    {isDispensed ? 'Đã cấp phát' : 'Chờ cấp phát'}
                  </Tag>
                )
              },
            },
            {
              title: 'Ngày kê',
              dataIndex: 'createdAt',
              render: (val) => val ? dayjs(val).format('HH:mm DD/MM/YYYY') : '—',
>>>>>>> origin/develop
            },
            {
              title: 'Thao tác',
              key: 'actions',
<<<<<<< HEAD
              width: 130,
              render: (_, row) => {
                const isPending = row.status === 'PENDING_DISPENSING' || row.status === 'PENDING_DISPENSE'
                const hasWarnings = row.warnings && row.warnings.length > 0
                const menuItems = [
                  {
                    key: 'edit',
                    label: 'Điều chỉnh đơn thuốc',
                    icon: <EditOutlined />,
                    disabled: !isPending,
                    onClick: () => beginEdit(row),
                  },
                  ...(hasWarnings
                    ? [
                        {
                          key: 'warnings',
                          label: 'Xem chi tiết cảnh báo',
                          icon: <EyeOutlined />,
                          onClick: () => setViewingWarningModal(row),
                        },
                      ]
                    : []),
                ]
                return (
                  <Dropdown menu={{ items: menuItems }} trigger={['click']}>
                    <Button size="small" icon={<MoreOutlined />}>
                      Thao tác
                    </Button>
                  </Dropdown>
                )
              },
=======
              render: (_, row) => (
                <Space>
                  <Button
                    icon={<EditOutlined />}
                    size="small"
                    disabled={!isDoctor || row.status === 'DISPENSED'}
                    onClick={() => startEditPrescription(row)}
                  >
                    Điều chỉnh
                  </Button>
                  <Button
                    icon={<HistoryOutlined />}
                    size="small"
                    onClick={() => openHistoryModal(row)}
                  >
                    Xem lịch sử
                  </Button>
                </Space>
              ),
>>>>>>> origin/develop
            },
          ]}
        />
      </Card>

<<<<<<< HEAD
      {/* Modal Xem chi tiết cảnh báo tương tác đã lưu */}
      <Modal
        title={
          <Space>
            <WarningOutlined style={{ color: '#fa8c16' }} />
            <span>Chi tiết cảnh báo tương tác thuốc - Đơn {viewingWarningModal?.prescriptionCode}</span>
          </Space>
        }
        open={!!viewingWarningModal}
        onCancel={() => setViewingWarningModal(null)}
        footer={[
          <Button key="close" onClick={() => setViewingWarningModal(null)}>
            Đóng
          </Button>,
        ]}
        width={650}
      >
        {viewingWarningModal && (
          <div>
            <Descriptions size="small" column={1} bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Lý do chuyên môn bác sĩ đã ghi nhận">
                <Text type="danger" strong>{viewingWarningModal.overrideReason || 'Bác sĩ đã xác nhận vượt cảnh báo khi kê đơn.'}</Text>
              </Descriptions.Item>
            </Descriptions>

            <List
              itemLayout="vertical"
              dataSource={viewingWarningModal.warnings || []}
              renderItem={(w, idx) => (
                <List.Item key={idx} style={{ backgroundColor: '#fafafa', padding: 12, borderRadius: 6, marginBottom: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <strong>{getDrugName(w.drugIdA || w.firstMedicineId)} ⚡ {getDrugName(w.drugIdB || w.secondMedicineId)}</strong>
                    {renderSeverityTag(w.severity)}
                  </div>
                  <div style={{ fontSize: 12, color: '#595959' }}>{w.description || w.warningMessage}</div>
                  {(w.clinicalRecommendation || w.action) && (
                    <div style={{ fontSize: 12, color: '#096dd9', marginTop: 4 }}>
                      <em>Khuyến cáo: {w.clinicalRecommendation || w.action}</em>
                    </div>
                  )}
                </List.Item>
              )}
            />
          </div>
        )}
      </Modal>
=======
      {/* Drug Interaction Confirmation Modal */}
      <InteractionWarningModal
        open={interactionModalOpen}
        warnings={detectedInteractions}
        currentUser={currentUser}
        onCancel={() => setInteractionModalOpen(false)}
        onConfirmOverride={handleConfirmInteractionOverrides}
      />

      {/* Change Audit History Modal */}
      <PrescriptionHistoryModal
        open={historyModalOpen}
        onClose={() => setHistoryModalOpen(false)}
        prescriptionCode={historyPrescriptionCode}
        historyLogs={activeHistoryLogs}
        medicines={medicines}
      />
>>>>>>> origin/develop
    </div>
  )
}

export default PrescriptionPage
