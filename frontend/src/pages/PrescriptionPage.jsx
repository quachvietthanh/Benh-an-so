import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Form,
  Input,
  InputNumber,
  List,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  FileTextOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
import patientApi from '../api/patientApi'
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'
import { demoMedicines, drugInteractions } from '../mock-data/mockData'
import { getMedicalRecords, getPatients } from '../services/mockDataService'
import {
  deleteStoredPrescription,
  dispensePrescriptionHelper,
  mergeMedicalRecords,
  mergeMedicines,
  mergePatients,
  mergePrescriptions,
  saveStoredPrescription,
} from '../utils/storageHelpers'

const { Title, Text, Paragraph } = Typography

// Helper extract medicine details (Hoạt chất, Hàm lượng, Đơn vị, Đường dùng)
const getMedicineDetails = (med) => {
  if (!med) return { name: '', activeIngredient: '—', strength: '—', unit: 'Viên', route: 'Uống' }

  const name = med.name || ''
  let activeIngredient = med.activeIngredient || med.hoatChat
  let strength = med.strength || med.hamLuong
  let unit = med.unit || med.donVi || 'Viên'
  let route = med.route || med.duongDung || 'Uống'

  if (!activeIngredient || !strength) {
    if (name.includes('Amlodipine')) {
      activeIngredient = 'Amlodipine'
      strength = '5mg'
    } else if (name.includes('Metformin')) {
      activeIngredient = 'Metformin HCl'
      strength = '500mg'
    } else if (name.includes('Paracetamol')) {
      activeIngredient = 'Paracetamol'
      strength = '500mg'
    } else if (name.includes('Ibuprofen')) {
      activeIngredient = 'Ibuprofen'
      strength = '400mg'
    } else if (name.includes('Aspirin')) {
      activeIngredient = 'Aspirin'
      strength = '81mg'
    } else {
      const parts = name.split(' ')
      activeIngredient = parts[0] || name
      strength = parts[1] || '—'
    }
  }

  return { name, activeIngredient, strength, unit, route }
}

const createEmptyItem = () => ({
  id: `item-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
  medicineId: undefined,
  dose: 1,
  timesPerDay: 2,
  days: 5,
  quantity: 10,
  usageInstruction: 'Uống sau khi ăn 30 phút',
  notes: '',
})

const parseItemsList = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function PrescriptionPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  // Phân quyền (CN - PHÂN QUYỀN)
  const userRoles = useMemo(() => (Array.isArray(user?.roles) ? user.roles.map((r) => String(r).toLowerCase()) : []), [user])
  const isDoctor = userRoles.some((r) => ['doctor', 'admin', 'role_doctor', 'role_admin'].includes(r))
  const isAdmin = userRoles.includes('admin') || userRoles.includes('role_admin')
  const isNurse = userRoles.includes('nurse') && !isDoctor
  const isPharmacist = userRoles.includes('pharmacist') && !isDoctor
  const isReceptionist = userRoles.includes('receptionist') && !isDoctor && !isNurse && !isPharmacist

  const canPrescribeOrEdit = isDoctor || isAdmin

  // States
  const [medicines, setMedicines] = useState([])
  const [records, setRecords] = useState([])
  const [prescriptions, setPrescriptions] = useState([])

  const [selectedRecordId, setSelectedRecordId] = useState(undefined)
  const [items, setItems] = useState([createEmptyItem()])
  const [editingPrescription, setEditingPrescription] = useState(null)
  const [changeReason, setChangeReason] = useState('')
  const [overrideReason, setOverrideReason] = useState('')
  const [saving, setSaving] = useState(false)

  // Single Item Draft Input State (Khung chọn & điền chi tiết 1 thuốc trước khi bấm Thêm)
  const [draftMedicineId, setDraftMedicineId] = useState(undefined)
  const [draftDose, setDraftDose] = useState(1)
  const [draftTimesPerDay, setDraftTimesPerDay] = useState(2)
  const [draftDays, setDraftDays] = useState(5)
  const [draftQuantity, setDraftQuantity] = useState(10)
  const [draftUsage, setDraftUsage] = useState('Uống sau khi ăn 30 phút')
  const [draftNotes, setDraftNotes] = useState('')

  // Modals
  const [viewModalPrescription, setViewModalPrescription] = useState(null)
  const [printModalPrescription, setPrintModalPrescription] = useState(null)
  const [highInteractionModalOpen, setHighInteractionModalOpen] = useState(false)
  const [detectedHighInteractions, setDetectedHighInteractions] = useState([])

  // Auto calculate draft total quantity when dose / times / days change
  useEffect(() => {
    const calculated = (Number(draftDose) || 1) * (Number(draftTimesPerDay) || 1) * (Number(draftDays) || 1)
    setDraftQuantity(calculated > 0 ? calculated : 1)
  }, [draftDose, draftTimesPerDay, draftDays])

  // Load initial data & enrich with patient directory for 100% data consistency
  const loadData = useCallback(async () => {
    try {
      const [medRes, recRes, prescRes, patRes] = await Promise.allSettled([
        pharmacyApi.medicines(),
        medicalRecordApi.getAll(),
        pharmacyApi.prescriptions(),
        patientApi.getAll({ page: 0, size: 200 }),
      ])

      const apiMeds = medRes.status === 'fulfilled' ? (medRes.value.data || []) : []
      const apiRecs = recRes.status === 'fulfilled' ? (recRes.value.data || []) : []
      const apiPrescs = prescRes.status === 'fulfilled' ? (prescRes.value.data || []) : []
      const apiPats = patRes.status === 'fulfilled' ? (patRes.value.data?.content || patRes.value.data || []) : []

      const combinedMeds = mergeMedicines(apiMeds.length ? apiMeds : demoMedicines)
      const combinedPatients = mergePatients(apiPats.length ? apiPats : getPatients())
      const combinedRecs = mergeMedicalRecords(apiRecs.length ? apiRecs : getMedicalRecords())
      const combinedPrescs = mergePrescriptions(apiPrescs)

      const patientMap = new Map()
      combinedPatients.forEach((p) => {
        if (p.id) patientMap.set(String(p.id), p)
        if (p.patientCode) patientMap.set(String(p.patientCode), p)
      })

      const enrichedRecs = combinedRecs
        .map((r) => {
          let foundP = patientMap.get(String(r.patientId))
            || patientMap.get(String(r.patientCode))
            || combinedPatients.find((p) => p.fullName && r.patientName && p.fullName.trim().toLowerCase() === r.patientName.trim().toLowerCase())

          if (!foundP && (r.patientName === 'Nguyễn Văn An' || r.patientName === 'Nguyen Van An')) {
            foundP = combinedPatients.find((p) => p.fullName === 'Nguyen Tuan Long') || combinedPatients[0]
          }

          if (!foundP) return null

          return {
            ...r,
            patientId: foundP.id || r.patientId,
            patientName: foundP.fullName,
            patientCode: foundP.patientCode || r.patientCode || 'BN-001',
            patientAge: foundP.dateOfBirth ? Math.max(1, dayjs().diff(dayjs(foundP.dateOfBirth), 'year')) : (r.patientAge || 35),
            patientGender: foundP.gender === 'MALE' ? 'Nam' : foundP.gender === 'FEMALE' ? 'Nữ' : (r.patientGender || 'Nam'),
            allergies: foundP.allergies || r.allergies || 'Khám phát hiện chưa dị ứng thuốc',
            medicalHistory: foundP.medicalHistory || r.medicalHistory || 'Chưa ghi nhận tiền sử bệnh',
            diagnosis: r.diagnosis || r.symptoms || 'Khám bệnh & Theo dõi sức khỏe',
          }
        })
        .filter(Boolean)

      setMedicines(combinedMeds)
      setRecords(enrichedRecs.filter((r) => r.diagnosis || r.symptoms || r.status === 'COMPLETED' || r.status === 'IN_PROGRESS'))
      setPrescriptions(combinedPrescs)
    } catch {
      const combinedPatients = mergePatients(getPatients())
      const combinedRecs = mergeMedicalRecords(getMedicalRecords())

      const patientMap = new Map()
      combinedPatients.forEach((p) => {
        if (p.id) patientMap.set(String(p.id), p)
        if (p.patientCode) patientMap.set(String(p.patientCode), p)
      })

      const enrichedRecs = combinedRecs
        .map((r) => {
          let foundP = patientMap.get(String(r.patientId))
            || patientMap.get(String(r.patientCode))
            || combinedPatients.find((p) => p.fullName && r.patientName && p.fullName.trim().toLowerCase() === r.patientName.trim().toLowerCase())

          if (!foundP && (r.patientName === 'Nguyễn Văn An' || r.patientName === 'Nguyen Van An')) {
            foundP = combinedPatients.find((p) => p.fullName === 'Nguyen Tuan Long') || combinedPatients[0]
          }

          if (!foundP) return null

          return {
            ...r,
            patientId: foundP.id || r.patientId,
            patientName: foundP.fullName,
            patientCode: foundP.patientCode || r.patientCode || 'BN-001',
            patientAge: foundP.dateOfBirth ? Math.max(1, dayjs().diff(dayjs(foundP.dateOfBirth), 'year')) : (r.patientAge || 35),
            patientGender: foundP.gender === 'MALE' ? 'Nam' : foundP.gender === 'FEMALE' ? 'Nữ' : (r.patientGender || 'Nam'),
            allergies: foundP.allergies || r.allergies || 'Khám phát hiện chưa dị ứng thuốc',
            medicalHistory: foundP.medicalHistory || r.medicalHistory || 'Chưa ghi nhận tiền sử bệnh',
            diagnosis: r.diagnosis || r.symptoms || 'Khám bệnh & Theo dõi sức khỏe',
          }
        })
        .filter(Boolean)

      setMedicines(mergeMedicines(demoMedicines))
      setRecords(enrichedRecs)
      setPrescriptions(mergePrescriptions([]))
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Handle location state passed from MedicalEncounter / Patient Detail
  useEffect(() => {
    if (location.state?.recordId) {
      setSelectedRecordId(location.state.recordId)
    }
  }, [location.state])

  // Selected Record Object
  const selectedRecord = useMemo(() => {
    if (!selectedRecordId) return null
    return records.find((r) => String(r.id) === String(selectedRecordId)) || null
  }, [records, selectedRecordId])

  // Selected Medicine details for Draft Form
  const selectedDraftMed = useMemo(() => {
    if (!draftMedicineId) return null
    return medicines.find((m) => String(m.id) === String(draftMedicineId)) || null
  }, [medicines, draftMedicineId])

  const draftMedDetails = useMemo(() => getMedicineDetails(selectedDraftMed), [selectedDraftMed])

  // CN-002: Kiểm tra tương tác thuốc (Check Drug Interactions)
  const checkInteractions = useCallback((itemsList) => {
    const selectedMedIds = itemsList.map((i) => i.medicineId).filter(Boolean)
    if (selectedMedIds.length < 2) return []

    const detected = []

    // Check catalog interactions
    drugInteractions.forEach((interaction) => {
      if (interaction.drugs.every((dId) => selectedMedIds.includes(dId))) {
        const med1 = medicines.find((m) => m.id === interaction.drugs[0]) || { name: interaction.drugs[0] }
        const med2 = medicines.find((m) => m.id === interaction.drugs[1]) || { name: interaction.drugs[1] }

        let level = 'MODERATE'
        let label = 'Mức trung bình'
        if (String(interaction.severity).includes('Cao') || String(interaction.severity).includes('Nghiêm trọng')) {
          level = 'HIGH'
          label = 'Mức cao (Nghiêm trọng)'
        } else if (String(interaction.severity).includes('Nhẹ')) {
          level = 'LOW'
          label = 'Mức nhẹ'
        }

        detected.push({
          id: `inter-${interaction.drugs.join('-')}`,
          drug1Name: med1.name,
          drug2Name: med2.name,
          severityLevel: level,
          severityLabel: label,
          description: interaction.description,
          recommendation: level === 'HIGH'
            ? 'Chống chỉ định phối hợp hoặc cần theo dõi sát nguy cơ biến chứng. Khuyến nghị thay thế hoặc ngưng 1 trong 2 thuốc.'
            : 'Cần dùng cách xa thời điểm hoặc theo dõi sát triệu chứng lâm sàng.',
        })
      }
    })

    // Dynamic checks
    const medNames = selectedMedIds.map((id) => medicines.find((m) => m.id === id)?.name || '').filter(Boolean)
    if (medNames.some((n) => n.includes('Ibuprofen')) && medNames.some((n) => n.includes('Aspirin'))) {
      if (!detected.some((d) => d.id.includes('med4-med5') || (d.drug1Name.includes('Ibuprofen') && d.drug2Name.includes('Aspirin')))) {
        detected.push({
          id: 'inter-ibu-asp-dyn',
          drug1Name: 'Ibuprofen 400mg',
          drug2Name: 'Aspirin 81mg',
          severityLevel: 'HIGH',
          severityLabel: 'Mức cao (Nghiêm trọng)',
          description: 'Ibuprofen làm giảm tác dụng bảo vệ tim mạch của Aspirin liều thấp và tăng nguy cơ xuất huyết tiêu hóa cấp.',
          recommendation: 'Chống chỉ định phối hợp. Thay thế Ibuprofen bằng Paracetamol.',
        })
      }
    }

    return detected
  }, [medicines])

  // Current interactions in draft items list
  const currentInteractions = useMemo(() => checkInteractions(items), [checkInteractions, items])

  // Highest interaction level
  const maxInteractionSeverity = useMemo(() => {
    if (!currentInteractions.length) return 'NONE'
    if (currentInteractions.some((i) => i.severityLevel === 'HIGH')) return 'HIGH'
    if (currentInteractions.some((i) => i.severityLevel === 'MODERATE')) return 'MODERATE'
    return 'LOW'
  }, [currentInteractions])

  // Add draft medicine to prescription table
  const handleAddDraftMedicine = () => {
    if (!draftMedicineId) {
      message.error('Vui lòng chọn thuốc từ danh mục!')
      return
    }

    if (items.some((i) => String(i.medicineId) === String(draftMedicineId))) {
      message.error('Thuốc này đã có trong đơn! Vui lòng chỉnh sửa liều/số lượng trực tiếp tại bảng bên dưới.')
      return
    }

    if (Number(draftQuantity) <= 0) {
      message.error('Số lượng thuốc phải lớn hơn 0!')
      return
    }

    if (!draftDose || !draftTimesPerDay || !draftDays) {
      message.error('Vui lòng điền đầy đủ liều dùng, số lần/ngày và số ngày!')
      return
    }

    if (!draftUsage || !draftUsage.trim()) {
      message.error('Vui lòng nhập hướng dẫn cách dùng thuốc!')
      return
    }

    const newItem = {
      id: `item-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      medicineId: draftMedicineId,
      dose: draftDose,
      timesPerDay: draftTimesPerDay,
      days: draftDays,
      quantity: Number(draftQuantity),
      usageInstruction: draftUsage.trim(),
      notes: draftNotes.trim(),
    }

    setItems((prev) => [...prev.filter((i) => i.medicineId), newItem])
    setDraftMedicineId(undefined)
    setDraftDose(1)
    setDraftTimesPerDay(2)
    setDraftDays(5)
    setDraftQuantity(10)
    setDraftUsage('Uống sau khi ăn 30 phút')
    setDraftNotes('')
    message.success('Đã thêm thuốc vào đơn!')
  }

  // Remove medicine item
  const handleRemoveItem = (itemId) => {
    setItems((prev) => prev.filter((i) => i.id !== itemId))
  }

  // Form Validation (CN-001 VALIDATE)
  const validatePrescriptionForm = () => {
    if (!editingPrescription && !selectedRecordId) {
      message.error('Vui lòng chọn Bệnh án đã có chẩn đoán!')
      return false
    }

    const validItems = items.filter((i) => i.medicineId)
    if (!validItems.length) {
      message.error('Đơn thuốc chưa có sản phẩm nào! Vui lòng chọn và thêm thuốc vào đơn.')
      return false
    }

    // Check duplicate
    const medIds = validItems.map((i) => i.medicineId)
    if (new Set(medIds).size !== medIds.length) {
      message.error('Phát hiện thuốc bị chọn trùng nhiều lần trong đơn! Vui lòng kiểm tra lại.')
      return false
    }

    // Check quantity <= 0
    if (validItems.some((i) => Number(i.quantity) <= 0)) {
      message.error('Số lượng thuốc trong đơn phải lớn hơn 0!')
      return false
    }

    // Check missing dose
    if (validItems.some((i) => !i.dose || !i.timesPerDay || !i.days)) {
      message.error('Thiếu thông tin liều dùng cho thuốc trong đơn!')
      return false
    }

    // Check missing usage
    if (validItems.some((i) => !i.usageInstruction || !i.usageInstruction.trim())) {
      message.error('Thiếu thông tin hướng dẫn cách dùng cho thuốc trong đơn!')
      return false
    }

    if (editingPrescription && !changeReason.trim()) {
      message.error('Vui lòng nhập lý do điều chỉnh đơn thuốc (lưu vết thay đổi)!')
      return false
    }

    return true
  }

  // Execute Save Prescription to storage and API
  const doSavePrescription = async (overrideMsg = '') => {
    setSaving(true)
    const validItems = items.filter((i) => i.medicineId)

    const doctorName = user?.fullName && user.fullName !== 'admin' ? user.fullName : 'BS. Phạm Hồng Anh'

    const payload = {
      id: editingPrescription ? editingPrescription.id : `presc-${Date.now()}`,
      prescriptionCode: editingPrescription ? editingPrescription.prescriptionCode : `DT-${dayjs().format('YYYYMMDDHHmmss')}`,
      medicalRecordId: editingPrescription ? editingPrescription.medicalRecordId : selectedRecordId,
      patientId: selectedRecord?.patientId || editingPrescription?.patientId,
      patientName: selectedRecord?.patientName || editingPrescription?.patientName || 'Bệnh nhân',
      patientCode: selectedRecord?.patientCode || editingPrescription?.patientCode || 'BN-2026001',
      patientAge: selectedRecord?.patientAge || editingPrescription?.patientAge || 35,
      patientGender: selectedRecord?.patientGender || editingPrescription?.patientGender || 'Nam',
      doctorName: editingPrescription?.doctorName || doctorName,
      diagnosis: selectedRecord?.diagnosis || editingPrescription?.diagnosis || 'Viêm mũi họng cấp / Khám sức khỏe',
      status: 'PENDING_DISPENSING', // CN-001: Trạng thái "Chờ cấp phát"
      items: JSON.stringify(validItems),
      overrideReason: overrideMsg || overrideReason,
      changeReason,
      createdAt: editingPrescription ? editingPrescription.createdAt : new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }

    try {
      if (editingPrescription) {
        await pharmacyApi.updatePrescription(editingPrescription.id, payload)
      } else {
        await pharmacyApi.createPrescription(payload)
      }
    } catch {
      // Fallback local storage persistence
    }

    saveStoredPrescription(payload)
    message.success(editingPrescription ? 'Đã lưu điều chỉnh đơn thuốc ở trạng thái Chờ cấp phát!' : 'Đã tạo đơn thuốc mới ở trạng thái Chờ cấp phát!')

    // Reset Form
    setEditingPrescription(null)
    setSelectedRecordId(undefined)
    setItems([createEmptyItem()])
    setOverrideReason('')
    setChangeReason('')
    setHighInteractionModalOpen(false)

    await loadData()
    setSaving(false)

    // Option to navigate to billing
    Modal.confirm({
      title: 'Kê đơn thuốc thành công!',
      content: 'Bệnh nhân đã được chuyển sang danh sách CHỜ CẤP PHÁT THUỐC. Bạn có muốn chuyển sang màn hình THU PHÍ & HÓA ĐƠN không?',
      okText: 'Chuyển sang Thu phí',
      cancelText: 'Ở lại màn hình Kê đơn',
      onOk: () => navigate('/billing', { state: { patientId: payload.patientId } }),
    })
  }

  // Trigger Save button
  const handleSaveTrigger = () => {
    if (!canPrescribeOrEdit) {
      message.error('Bạn không có quyền kê hoặc chỉnh sửa đơn thuốc!')
      return
    }

    if (!validatePrescriptionForm()) return

    const highInters = currentInteractions.filter((i) => i.severityLevel === 'HIGH')
    if (highInters.length > 0) {
      // CN-002: Nếu tương tác MỨC CAO -> Hiện Modal cảnh báo
      setDetectedHighInteractions(highInters)
      setHighInteractionModalOpen(true)
      return
    }

    doSavePrescription()
  }

  // CN-003: Chỉnh sửa đơn thuốc
  const handleBeginEdit = (presc) => {
    if (!canPrescribeOrEdit) {
      message.error('Bạn không có quyền chỉnh sửa đơn thuốc!')
      return
    }
    if (presc.status === 'DISPENSED' || presc.status === 'COMPLETED') {
      message.error('Đơn thuốc ĐÃ CẤP PHÁT chỉ được XEM, không được phép chỉnh sửa!')
      return
    }

    const loadedItems = parseItemsList(presc.items)
    setEditingPrescription(presc)
    setSelectedRecordId(presc.medicalRecordId)
    setItems(loadedItems.length ? loadedItems : [createEmptyItem()])
    setOverrideReason(presc.overrideReason || '')
    setChangeReason('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Hủy sửa đơn
  const handleCancelEdit = () => {
    setEditingPrescription(null)
    setSelectedRecordId(undefined)
    setItems([createEmptyItem()])
    setOverrideReason('')
    setChangeReason('')
  }

  // Dược sĩ cấp phát đơn thuốc
  const handleDispense = async (presc) => {
    try {
      dispensePrescriptionHelper(presc.id)
      message.success(`Đã cấp phát đơn thuốc ${presc.prescriptionCode} thành công!`)
      await loadData()
    } catch (err) {
      message.error(err.message || 'Lỗi cấp phát đơn thuốc')
    }
  }

  // Hủy đơn thuốc
  const handleCancelPrescription = (presc) => {
    const updated = { ...presc, status: 'CANCELLED', updatedAt: new Date().toISOString() }
    saveStoredPrescription(updated)
    message.success(`Đã hủy đơn thuốc ${presc.prescriptionCode}`)
    loadData()
  }

  // Xóa đơn thuốc
  const handleDeletePrescription = (presc) => {
    deleteStoredPrescription(presc.id)
    message.success(`Đã xóa đơn thuốc ${presc.prescriptionCode}`)
    loadData()
  }

  return (
    <div className="prescription-page" style={{ paddingBottom: 40 }}>
      {/* HEADER PAGE */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <Title level={3} style={{ margin: 0, color: '#1e293b', display: 'flex', alignItems: 'center', gap: 10 }}>
            <MedicineBoxOutlined style={{ color: '#2563eb' }} /> Kê Đơn Thuốc & Cảnh Báo Tương Tác
          </Title>
          <Text type="secondary">Quản lý kê đơn thuốc, kiểm tra tương tác tự động và theo dõi lịch sử cấp phát</Text>
        </div>

        {canPrescribeOrEdit && (
          <Space>
            {editingPrescription && <Button onClick={handleCancelEdit}>Hủy điều chỉnh</Button>}
            <Button type="primary" size="large" icon={<MedicineBoxOutlined />} loading={saving} onClick={handleSaveTrigger}>
              {editingPrescription ? 'Lưu điều chỉnh đơn thuốc' : 'Lưu & Tạo đơn thuốc'}
            </Button>
          </Space>
        )}
      </div>

      {/* TÍNH NĂNG PHÂN QUYỀN BANNER */}
      {isNurse && (
        <Alert
          style={{ marginBottom: 20 }}
          type="info"
          showIcon
          message="Tài khoản Điều dưỡng (Nurse)"
          description="Bạn đang truy cập ở chế độ CHỈ XEM đơn thuốc. Không có quyền tạo mới hoặc điều chỉnh đơn."
        />
      )}
      {isPharmacist && (
        <Alert
          style={{ marginBottom: 20 }}
          type="warning"
          showIcon
          message="Tài khoản Dược sĩ (Pharmacist)"
          description="Bạn có quyền XEM chi tiết đơn thuốc và thực hiện CẤP PHÁT THUỐC cho bệnh nhân."
        />
      )}
      {isReceptionist && (
        <Alert
          style={{ marginBottom: 20 }}
          type="error"
          showIcon
          message="Tài khoản Lễ tân (Receptionist)"
          description="Lễ tân không được cấp quyền kê đơn thuốc. Bạn chỉ có thể tra cứu danh sách đơn thuốc."
        />
      )}

      {/* CN-001: FORM KÊ ĐƠN THUỐC (CHỈ HIỂN THỊ VỚI BÁC SĨ / ADMIN) */}
      {canPrescribeOrEdit && (
        <Card
          title={(
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap' }}>
              <span style={{ fontSize: 16, fontWeight: 600, color: '#1e3a8a' }}>
                {editingPrescription ? `📝 Điều chỉnh đơn thuốc #${editingPrescription.prescriptionCode}` : '📋 Kê đơn thuốc mới cho bệnh nhân'}
              </span>
              {editingPrescription && <Tag color="orange">Đang điều chỉnh</Tag>}
            </div>
          )}
          style={{ marginBottom: 24, borderRadius: 12, boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
        >
          {/* RESPONSIVE FORM: Laptop 2 cột / Mobile 1 cột */}
          <Row gutter={[16, 16]}>
            {/* CỘT 1: Chọn bệnh án & Thông tin bệnh nhân */}
            <Col xs={24} md={12}>
              <Form.Item label={<strong>1. Bệnh án đã có chẩn đoán (*)</strong>} required style={{ marginBottom: 12 }}>
                <Select
                  showSearch
                  disabled={!!editingPrescription}
                  optionFilterProp="label"
                  placeholder="-- Chọn Bệnh án / Lượt khám để kê đơn --"
                  value={selectedRecordId}
                  onChange={setSelectedRecordId}
                  style={{ width: '100%' }}
                  options={records.map((r) => ({
                    value: r.id,
                    label: `${r.recordCode || r.id} — ${r.patientName || 'Bệnh nhân'} (Chẩn đoán: ${r.diagnosis || 'Đã khám'})`,
                  }))}
                />
              </Form.Item>

              {selectedRecord ? (
                <div style={{ backgroundColor: '#f8fafc', padding: 14, borderRadius: 8, border: '1px solid #e2e8f0' }}>
                  <Text strong style={{ color: '#2563eb', display: 'block', marginBottom: 6 }}>
                    👤 Thông tin bệnh nhân & Chẩn đoán:
                  </Text>
                  <Row gutter={[8, 4]} style={{ fontSize: 13 }}>
                    <Col span={12}><strong>Họ tên:</strong> {selectedRecord.patientName}</Col>
                    <Col span={12}><strong>Mã BN:</strong> {selectedRecord.patientCode || 'BN-001'}</Col>
                    <Col span={12}><strong>Bác sĩ khám:</strong> {selectedRecord.doctorName || 'BS. Phạm Hồng Anh'}</Col>
                    <Col span={12}><strong>Ngày khám:</strong> {selectedRecord.createdAt ? dayjs(selectedRecord.createdAt).format('DD/MM/YYYY') : 'Hôm nay'}</Col>
                    <Col span={24} style={{ marginTop: 4 }}>
                      <Tag color="blue" style={{ fontSize: 13, padding: '2px 8px' }}>
                        🏥 Chẩn đoán: {selectedRecord.diagnosis || 'Khám tổng quát / Theo dõi sức khỏe'}
                      </Tag>
                    </Col>
                  </Row>
                </div>
              ) : (
                <Alert
                  type="info"
                  showIcon
                  message="Vui lòng chọn Bệnh án"
                  description="Bệnh án phải ở trạng thái đang khám/hoàn thành và có chẩn đoán mới được phép kê đơn."
                />
              )}
            </Col>

            {/* CỘT 2: Chọn thuốc & Khung chi tiết thuốc */}
            <Col xs={24} md={12}>
              <div style={{ backgroundColor: '#eff6ff', padding: 14, borderRadius: 8, border: '1px solid #bfdbfe' }}>
                <Text strong style={{ color: '#1d4ed8', display: 'block', marginBottom: 8 }}>
                  💊 2. Chọn thuốc & Thiết lập liều dùng:
                </Text>

                <Form.Item label="Chọn thuốc từ danh mục (*)" style={{ marginBottom: 10 }}>
                  <Select
                    showSearch
                    optionFilterProp="label"
                    placeholder="Gõ tên thuốc để tìm..."
                    value={draftMedicineId}
                    onChange={setDraftMedicineId}
                    style={{ width: '100%' }}
                    options={medicines.map((m) => ({
                      value: m.id,
                      label: `${m.name} (Tồn: ${m.stock} ${m.unit || 'đơn vị'})`,
                    }))}
                  />
                </Form.Item>

                {selectedDraftMed && (
                  <div style={{ backgroundColor: '#ffffff', padding: 10, borderRadius: 6, marginBottom: 10, fontSize: 12, border: '1px solid #dbeafe' }}>
                    <Row gutter={[8, 4]}>
                      <Col span={12}><strong>Hoạt chất:</strong> <Tag color="cyan">{draftMedDetails.activeIngredient}</Tag></Col>
                      <Col span={12}><strong>Hàm lượng:</strong> {draftMedDetails.strength}</Col>
                      <Col span={12}><strong>Đơn vị:</strong> {draftMedDetails.unit}</Col>
                      <Col span={12}><strong>Đường dùng:</strong> {draftMedDetails.route}</Col>
                    </Row>
                  </div>
                )}

                <Row gutter={8} style={{ marginBottom: 10 }}>
                  <Col span={8}>
                    <Text size="small">Liều dùng/lần (*):</Text>
                    <InputNumber min={0.5} step={0.5} value={draftDose} onChange={setDraftDose} style={{ width: '100%' }} />
                  </Col>
                  <Col span={8}>
                    <Text size="small">Số lần/ngày (*):</Text>
                    <InputNumber min={1} value={draftTimesPerDay} onChange={setDraftTimesPerDay} style={{ width: '100%' }} />
                  </Col>
                  <Col span={8}>
                    <Text size="small">Số ngày (*):</Text>
                    <InputNumber min={1} value={draftDays} onChange={setDraftDays} style={{ width: '100%' }} />
                  </Col>
                </Row>

                <Row gutter={8} style={{ marginBottom: 10 }}>
                  <Col span={10}>
                    <Text size="small"><strong>Số lượng tổng (*):</strong></Text>
                    <InputNumber min={1} value={draftQuantity} onChange={setDraftQuantity} style={{ width: '100%' }} />
                  </Col>
                  <Col span={14}>
                    <Text size="small">Cách dùng (*):</Text>
                    <Input value={draftUsage} onChange={(e) => setDraftUsage(e.target.value)} placeholder="VD: Uống sau ăn 30 phút" />
                  </Col>
                </Row>

                <Form.Item label="Ghi chú thêm:" style={{ marginBottom: 10 }}>
                  <Input value={draftNotes} onChange={(e) => setDraftNotes(e.target.value)} placeholder="Ghi chú thêm nếu có..." />
                </Form.Item>

                <Button type="primary" block icon={<PlusOutlined />} onClick={handleAddDraftMedicine} style={{ backgroundColor: '#2563eb' }}>
                  Thêm thuốc này vào đơn
                </Button>
              </div>
            </Col>
          </Row>

          <Divider style={{ margin: '20px 0 16px 0' }} />

          {/* CN-001: BẢNG THUỐC ĐÃ CHỌN TRONG ĐƠN */}
          <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Text strong style={{ fontSize: 15, color: '#0f172a' }}>
              📋 Bảng thuốc đã chọn trong đơn ({items.filter((i) => i.medicineId).length} sản phẩm):
            </Text>
            {currentInteractions.length > 0 && (
              <Tag color={maxInteractionSeverity === 'HIGH' ? 'error' : maxInteractionSeverity === 'MODERATE' ? 'warning' : 'blue'} style={{ fontSize: 13 }}>
                <WarningOutlined /> Phát hiện {currentInteractions.length} tương tác thuốc
              </Tag>
            )}
          </div>

          <Table
            rowKey="id"
            size="small"
            pagination={false}
            dataSource={items.filter((i) => i.medicineId)}
            locale={{ emptyText: 'Chưa có thuốc nào trong đơn. Vui lòng chọn thuốc ở ô trên và bấm "Thêm thuốc này vào đơn".' }}
            columns={[
              { title: 'STT', key: 'stt', width: 50, render: (_, __, idx) => idx + 1 },
              {
                title: 'Thuốc',
                dataIndex: 'medicineId',
                render: (mId) => {
                  const med = medicines.find((m) => m.id === mId)
                  const details = getMedicineDetails(med)
                  return (
                    <div>
                      <strong>{med?.name || mId}</strong>
                      <div style={{ fontSize: 11, color: '#64748b' }}>
                        Hoạt chất: {details.activeIngredient} | Đường dùng: {details.route}
                      </div>
                    </div>
                  )
                },
              },
              {
                title: 'Hàm lượng',
                dataIndex: 'medicineId',
                width: 100,
                render: (mId) => {
                  const med = medicines.find((m) => m.id === mId)
                  return getMedicineDetails(med).strength
                },
              },
              {
                title: 'Liều dùng',
                key: 'doseInfo',
                width: 140,
                render: (_, row) => `${row.dose || 1} viên x ${row.timesPerDay || 2} lần/ngày (${row.days || 5} ngày)`,
              },
              {
                title: 'Số lượng',
                dataIndex: 'quantity',
                width: 100,
                render: (val, row) => {
                  const med = medicines.find((m) => m.id === row.medicineId)
                  return <strong>{val} {med?.unit || 'viên'}</strong>
                },
              },
              { title: 'Cách dùng', dataIndex: 'usageInstruction', render: (val) => val || '—' },
              { title: 'Ghi chú', dataIndex: 'notes', render: (val) => val || '—' },
              {
                title: 'Xóa',
                key: 'action',
                width: 60,
                render: (_, row) => (
                  <Button danger type="text" icon={<DeleteOutlined />} onClick={() => handleRemoveItem(row.id)} />
                ),
              },
            ]}
          />

          {/* CN-002: KHUNG HIỂN THỊ CẢNH BÁO TƯƠNG TÁC THUỐC */}
          {currentInteractions.length > 0 && (
            <div style={{ marginTop: 20, padding: 14, backgroundColor: '#fffbe6', borderRadius: 8, border: '1px solid #ffe58f' }}>
              <Text strong style={{ color: '#d97706', fontSize: 14, display: 'flex', alignItems: 'center', gap: 6 }}>
                <WarningOutlined /> CẢNH BÁO TƯƠNG TÁC THUỐC ({currentInteractions.length} cặp tương tác):
              </Text>
              <List
                size="small"
                style={{ marginTop: 8 }}
                dataSource={currentInteractions}
                renderItem={(item) => (
                  <List.Item style={{ borderBottom: '1px solid #fef08a', padding: '8px 0' }}>
                    <div style={{ width: '100%' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontWeight: 600 }}>
                          💊 {item.drug1Name} ↔ 💊 {item.drug2Name}
                        </span>
                        <Tag color={item.severityLevel === 'HIGH' ? 'red' : item.severityLevel === 'MODERATE' ? 'orange' : 'gold'}>
                          {item.severityLabel}
                        </Tag>
                      </div>
                      <div style={{ fontSize: 12, color: '#475569', marginTop: 4 }}>
                        <strong>Mô tả:</strong> {item.description}
                      </div>
                      <div style={{ fontSize: 12, color: '#2563eb', marginTop: 2 }}>
                        <strong>Khuyến nghị:</strong> {item.recommendation}
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            </div>
          )}

          {editingPrescription && (
            <Form.Item label="Lý do điều chỉnh đơn thuốc (bắt buộc)" required style={{ marginTop: 16 }}>
              <Input.TextArea
                rows={2}
                placeholder="Nhập lý do bác sĩ điều chỉnh đơn thuốc..."
                value={changeReason}
                onChange={(e) => setChangeReason(e.target.value)}
              />
            </Form.Item>
          )}
        </Card>
      )}

      {/* CN-004 & CN-BẢNG ĐƠN THUỐC: DANH SÁCH ĐƠN THUỐC ĐÃ LẬP */}
      <Card
        title={(
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 16, fontWeight: 600, color: '#1e293b' }}>
              📚 Danh sách đơn thuốc trong hệ thống ({prescriptions.length} đơn)
            </span>
            <Button icon={<ReloadOutlined />} onClick={loadData}>Tải lại danh sách</Button>
          </div>
        )}
        style={{ borderRadius: 12, boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
      >
        <Table
          rowKey="id"
          dataSource={prescriptions}
          locale={{ emptyText: 'Chưa có đơn thuốc nào trong hệ thống.' }}
          columns={[
            {
              title: 'Mã đơn',
              dataIndex: 'prescriptionCode',
              key: 'code',
              render: (val) => <strong style={{ color: '#2563eb' }}>{val}</strong>,
            },
            {
              title: 'Bệnh nhân',
              dataIndex: 'patientName',
              key: 'patient',
              render: (val, row) => (
                <div>
                  <strong>{val || 'Bệnh nhân'}</strong>
                  <div style={{ fontSize: 11, color: '#64748b' }}>
                    Mã BN: {row.patientCode || 'BN-001'} | {row.patientAge || 35}T - {row.patientGender || 'Nam'}
                  </div>
                </div>
              ),
            },
            {
              title: 'Bác sĩ kê',
              dataIndex: 'doctorName',
              key: 'doctor',
              render: (val) => val || 'BS. Phạm Hồng Anh',
            },
            {
              title: 'Ngày kê',
              dataIndex: 'createdAt',
              key: 'createdAt',
              render: (val) => (val ? dayjs(val).format('HH:mm DD/MM/YYYY') : 'Hôm nay'),
            },
            {
              title: 'Danh sách thuốc',
              dataIndex: 'items',
              key: 'itemsList',
              render: (val) => {
                const list = parseItemsList(val)
                if (!list.length) return '—'
                return (
                  <div>
                    {list.map((item, idx) => {
                      const med = medicines.find((m) => m.id === item.medicineId)
                      return (
                        <Tag key={idx} color="blue" style={{ marginBottom: 2 }}>
                          {med?.name || item.medicineId} ({item.quantity} {med?.unit || 'viên'})
                        </Tag>
                      )
                    })}
                  </div>
                )
              },
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              key: 'status',
              render: (val) => {
                if (val === 'PENDING_DISPENSING' || val === 'PENDING') {
                  return <Tag color="orange" style={{ fontSize: 12, padding: '2px 8px' }}>Chờ cấp phát</Tag>
                }
                if (val === 'DISPENSED' || val === 'COMPLETED') {
                  return <Tag color="green" style={{ fontSize: 12, padding: '2px 8px' }}>Đã cấp phát</Tag>
                }
                if (val === 'CANCELLED') {
                  return <Tag color="red" style={{ fontSize: 12, padding: '2px 8px' }}>Đã hủy</Tag>
                }
                return <Tag color="default">{val}</Tag>
              },
            },
            {
              title: 'Thao tác',
              key: 'actions',
              width: 220,
              render: (_, row) => {
                const isPending = row.status === 'PENDING_DISPENSING' || row.status === 'PENDING'

                return (
                  <Space wrap size={4}>
                    {/* Nút XEM */}
                    <Tooltip title="Xem chi tiết đơn thuốc">
                      <Button size="small" icon={<EyeOutlined />} onClick={() => setViewModalPrescription(row)} />
                    </Tooltip>

                    {/* Nút IN */}
                    <Tooltip title="In đơn thuốc">
                      <Button size="small" icon={<PrinterOutlined />} onClick={() => setPrintModalPrescription(row)} />
                    </Tooltip>

                    {/* Nút CẤP PHÁT (Cho Dược sĩ) */}
                    {isPharmacist && isPending && (
                      <Popconfirm title="Xác nhận cấp phát đơn thuốc này?" onConfirm={() => handleDispense(row)}>
                        <Button size="small" type="primary" style={{ backgroundColor: '#16a34a' }}>Cấp phát</Button>
                      </Popconfirm>
                    )}

                    {/* Nút SỬA (CN-003: Chỉ sửa khi Chưa cấp phát & là Bác sĩ/Admin) */}
                    {canPrescribeOrEdit && (
                      <Tooltip title={isPending ? 'Sửa đơn thuốc' : 'Đơn đã cấp phát - Chỉ được xem'}>
                        <Button
                          size="small"
                          icon={<EditOutlined />}
                          disabled={!isPending}
                          onClick={() => handleBeginEdit(row)}
                        />
                      </Tooltip>
                    )}

                    {/* Nút HỦY */}
                    {canPrescribeOrEdit && isPending && (
                      <Popconfirm title="Hủy đơn thuốc này?" onConfirm={() => handleCancelPrescription(row)}>
                        <Tooltip title="Hủy đơn">
                          <Button size="small" danger icon={<CloseCircleOutlined />} />
                        </Tooltip>
                      </Popconfirm>
                    )}

                    {/* Nút XÓA */}
                    {canPrescribeOrEdit && (
                      <Popconfirm title="Xóa đơn thuốc khỏi hệ thống?" onConfirm={() => handleDeletePrescription(row)}>
                        <Tooltip title="Xóa đơn">
                          <Button size="small" danger icon={<DeleteOutlined />} />
                        </Tooltip>
                      </Popconfirm>
                    )}
                  </Space>
                )
              },
            },
          ]}
        />
      </Card>

      {/* CN-002: MODAL CẢNH BÁO TƯƠNG TÁC MỨC CAO */}
      <Modal
        title={(
          <div style={{ color: '#dc2626', fontSize: 16, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8 }}>
            <WarningOutlined style={{ fontSize: 20 }} /> 🚨 CẢNH BÁO TƯƠNG TÁC THUỐC MỨC ĐỘ CAO
          </div>
        )}
        open={highInteractionModalOpen}
        onCancel={() => setHighInteractionModalOpen(false)}
        footer={[
          <Button key="back" size="large" onClick={() => setHighInteractionModalOpen(false)}>
            Quay lạiChỉnh sửa đơn
          </Button>,
          <Button
            key="submit"
            type="primary"
            danger
            size="large"
            loading={saving}
            onClick={() => {
              if (!overrideReason.trim()) {
                message.error('Vui lòng nhập lý do chuyên môn để xác nhận tiếp tục kê đơn!')
                return
              }
              doSavePrescription(overrideReason)
            }}
          >
            Tiếp tục lưu đơn
          </Button>,
        ]}
      >
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          message="Phát hiện tương tác thuốc nghiêm trọng!"
          description="Đơn thuốc chứa các cặp thuốc có tương tác mức độ CAO. Vui lòng đọc kỹ thông tin bên dưới và nhập lý do chuyên môn nếu vẫn quyết định cho dùng phối hợp."
        />

        <List
          dataSource={detectedHighInteractions}
          renderItem={(item) => (
            <Card size="small" style={{ marginBottom: 12, borderColor: '#fca5a5', backgroundColor: '#fef2f2' }}>
              <div style={{ fontWeight: 700, color: '#991b1b', marginBottom: 4 }}>
                💊 {item.drug1Name} ↔ 💊 {item.drug2Name}
              </div>
              <div style={{ marginBottom: 4 }}>
                <strong>Mức độ:</strong> <Tag color="red">{item.severityLabel}</Tag>
              </div>
              <div style={{ fontSize: 13, marginBottom: 4 }}>
                <strong>Mô tả:</strong> {item.description}
              </div>
              <div style={{ fontSize: 13, color: '#1e40af' }}>
                <strong>Khuyến nghị:</strong> {item.recommendation}
              </div>
            </Card>
          )}
        />

        <Form.Item label={<strong>Lý do chuyên môn của Bác sĩ (bắt buộc khi lưu):</strong>} required>
          <Input.TextArea
            rows={3}
            placeholder="Nhập giải trình lý do chuyên môn (ví dụ: Đã cân nhắc lợi ích/nguy cơ, cho dùng cách xa 4 tiếng...)..."
            value={overrideReason}
            onChange={(e) => setOverrideReason(e.target.value)}
          />
        </Form.Item>
      </Modal>

      {/* MODAL XEM CHI TIẾT ĐƠN THUỐC */}
      <Modal
        title={`Chi tiết đơn thuốc #${viewModalPrescription?.prescriptionCode}`}
        open={!!viewModalPrescription}
        onCancel={() => setViewModalPrescription(null)}
        footer={[
          <Button key="print" icon={<PrinterOutlined />} onClick={() => setPrintModalPrescription(viewModalPrescription)}>
            In đơn thuốc
          </Button>,
          <Button key="close" type="primary" onClick={() => setViewModalPrescription(null)}>
            Đóng
          </Button>,
        ]}
        width={700}
      >
        {viewModalPrescription && (
          <div>
            <Descriptions title="Thông tin chung" bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Mã đơn thuốc">{viewModalPrescription.prescriptionCode}</Descriptions.Item>
              <Descriptions.Item label="Ngày kê">
                {viewModalPrescription.createdAt ? dayjs(viewModalPrescription.createdAt).format('HH:mm DD/MM/YYYY') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Bệnh nhân">{viewModalPrescription.patientName}</Descriptions.Item>
              <Descriptions.Item label="Mã BN">{viewModalPrescription.patientCode || 'BN-001'}</Descriptions.Item>
              <Descriptions.Item label="Bác sĩ kê đơn">{viewModalPrescription.doctorName}</Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color={viewModalPrescription.status === 'PENDING_DISPENSING' ? 'orange' : 'green'}>
                  {viewModalPrescription.status === 'PENDING_DISPENSING' ? 'Chờ cấp phát' : 'Đã cấp phát'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Chẩn đoán" span={2}>{viewModalPrescription.diagnosis}</Descriptions.Item>
            </Descriptions>

            <Text strong style={{ display: 'block', marginBottom: 8 }}>Danh sách thuốc chỉ định:</Text>
            <Table
              size="small"
              pagination={false}
              dataSource={parseItemsList(viewModalPrescription.items)}
              columns={[
                { title: 'STT', render: (_, __, idx) => idx + 1, width: 50 },
                {
                  title: 'Tên thuốc',
                  dataIndex: 'medicineId',
                  render: (id) => medicines.find((m) => m.id === id)?.name || id,
                },
                {
                  title: 'Hàm lượng',
                  dataIndex: 'medicineId',
                  render: (id) => getMedicineDetails(medicines.find((m) => m.id === id)).strength,
                },
                {
                  title: 'Số lượng',
                  dataIndex: 'quantity',
                  render: (val, row) => {
                    const med = medicines.find((m) => m.id === row.medicineId)
                    return `${val} ${med?.unit || 'viên'}`
                  },
                },
                { title: 'Cách dùng', dataIndex: 'usageInstruction' },
              ]}
            />

            {viewModalPrescription.overrideReason && (
              <Alert
                style={{ marginTop: 12 }}
                type="warning"
                message="Lý do chuyên môn khi lưu đơn có tương tác:"
                description={viewModalPrescription.overrideReason}
              />
            )}
          </div>
        )}
      </Modal>

      {/* MODAL IN ĐƠN THUỐC (ĐƠN THUỐC MẪU CHUẨN KÝ TÊN) */}
      <Modal
        title={`Xem trước bản in - Đơn thuốc #${printModalPrescription?.prescriptionCode}`}
        open={!!printModalPrescription}
        onCancel={() => setPrintModalPrescription(null)}
        footer={[
          <Button key="close" onClick={() => setPrintModalPrescription(null)}>Đóng</Button>,
          <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={() => window.print()}>
            In ngay
          </Button>,
        ]}
        width={750}
      >
        {printModalPrescription && (
          <div style={{ padding: 20, backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: 8, fontFamily: 'serif' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '2px solid #000', paddingBottom: 10, marginBottom: 16 }}>
              <div>
                <strong style={{ fontSize: 16 }}>PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ</strong>
                <div>Địa chỉ: 123 Lê Lợi, Quận 1, TP. Hồ Chí Minh</div>
                <div>Điện thoại: (028) 3822 9999 - Hotline: 1900 1234</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <strong style={{ fontSize: 14 }}>Mã đơn: {printModalPrescription.prescriptionCode}</strong>
                <div>Ngày: {dayjs(printModalPrescription.createdAt).format('DD/MM/YYYY')}</div>
              </div>
            </div>

            <div style={{ textAlign: 'center', margin: '20px 0' }}>
              <h2 style={{ margin: 0, textTransform: 'uppercase', letterSpacing: 1 }}>ĐƠN THUỐC</h2>
            </div>

            <Row gutter={[16, 8]} style={{ fontSize: 14, marginBottom: 16 }}>
              <Col span={16}><strong>Họ và tên bệnh nhân:</strong> {printModalPrescription.patientName}</Col>
              <Col span={8}><strong>Mã BN:</strong> {printModalPrescription.patientCode || 'BN-2026001'}</Col>
              <Col span={12}><strong>Tuổi:</strong> {printModalPrescription.patientAge || 35} tuổi</Col>
              <Col span={12}><strong>Giới tính:</strong> {printModalPrescription.patientGender || 'Nam'}</Col>
              <Col span={24}><strong>Chẩn đoán:</strong> {printModalPrescription.diagnosis}</Col>
            </Row>

            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 20 }}>
              <thead>
                <tr style={{ backgroundColor: '#f1f5f9' }}>
                  <th style={{ border: '1px solid #000', padding: 6, textAlign: 'center', width: 40 }}>STT</th>
                  <th style={{ border: '1px solid #000', padding: 6, textAlign: 'left' }}>Tên thuốc & Hoạt chất</th>
                  <th style={{ border: '1px solid #000', padding: 6, textAlign: 'center', width: 80 }}>SL</th>
                  <th style={{ border: '1px solid #000', padding: 6, textAlign: 'left' }}>Cách dùng</th>
                </tr>
              </thead>
              <tbody>
                {parseItemsList(printModalPrescription.items).map((item, idx) => {
                  const med = medicines.find((m) => m.id === item.medicineId)
                  const details = getMedicineDetails(med)
                  return (
                    <tr key={idx}>
                      <td style={{ border: '1px solid #000', padding: 6, textAlign: 'center' }}>{idx + 1}</td>
                      <td style={{ border: '1px solid #000', padding: 6 }}>
                        <strong>{med?.name || item.medicineId}</strong>
                        <div style={{ fontSize: 12 }}>({details.activeIngredient} - {details.strength})</div>
                      </td>
                      <td style={{ border: '1px solid #000', padding: 6, textAlign: 'center' }}>
                        {item.quantity} {med?.unit || 'viên'}
                      </td>
                      <td style={{ border: '1px solid #000', padding: 6, fontSize: 13 }}>
                        {item.usageInstruction || item.dosage || 'Uống sau ăn'}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>

            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 30 }}>
              <div style={{ fontSize: 13 }}>
                <strong>Lời dặn của bác sĩ:</strong>
                <div>- Uống thuốc đúng giờ, đúng liều lượng chỉ định.</div>
                <div>- Tái khám theo hẹn hoặc khi có dấu hiệu bất thường.</div>
              </div>
              <div style={{ textAlign: 'center', minWidth: 200 }}>
                <div>Ngày {dayjs(printModalPrescription.createdAt).format('DD')} tháng {dayjs(printModalPrescription.createdAt).format('MM')} năm {dayjs(printModalPrescription.createdAt).format('YYYY')}</div>
                <strong style={{ display: 'block', marginTop: 4, marginBottom: 50 }}>Bác sĩ kê đơn</strong>
                <strong>{printModalPrescription.doctorName || 'BS. Phạm Hồng Anh'}</strong>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default PrescriptionPage
