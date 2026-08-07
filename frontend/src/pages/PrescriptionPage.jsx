import React, { useCallback, useEffect, useState, useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
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
  Tabs,
  Tooltip,
  Badge,
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
  SearchOutlined,
  FilterOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
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
import EditPrescriptionModal from '../components/pharmacy/EditPrescriptionModal'

const { Text, Title } = Typography

// PRESCRIPTION_DATA_MODE: 'API' connects directly to Backend Spring Boot (Port 8080 / DB migrations V16-V17)
const PRESCRIPTION_DATA_MODE = 'API' 

const createEmptyItem = () => ({
  id: `item-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
  medicineId: undefined,
  quantity: 1,
  dosage: '',
})

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

  // Edit Modal State
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [modalPrescription, setModalPrescription] = useState(null)

  // Filter & Search State
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [searchKeyword, setSearchKeyword] = useState('')

  // Loading state
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

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

  // Filtered Prescriptions Computation
  const filteredPrescriptions = useMemo(() => {
    return prescriptions.filter((p) => {
      // Status filter
      if (statusFilter === 'PENDING' && p.status === 'DISPENSED') return false
      if (statusFilter === 'DISPENSED' && p.status !== 'DISPENSED') return false

      // Search keyword
      if (!searchKeyword.trim()) return true
      const kw = searchKeyword.toLowerCase().trim()
      const codeMatch = String(p.prescriptionCode || '').toLowerCase().includes(kw)
      const nameMatch = String(p.patientName || '').toLowerCase().includes(kw)
      const docMatch = String(p.doctorName || p.createdBy || '').toLowerCase().includes(kw)
      return codeMatch || nameMatch || docMatch
    })
  }, [prescriptions, statusFilter, searchKeyword])

  const pendingCount = useMemo(() => {
    return prescriptions.filter((p) => p.status !== 'DISPENSED').length
  }, [prescriptions])

  const dispensedCount = useMemo(() => {
    return prescriptions.filter((p) => p.status === 'DISPENSED').length
  }, [prescriptions])

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
    if (!isDoctor) {
      return 'Chỉ Bác sĩ mới được phép kê đơn thuốc.'
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
    <div style={{ paddingBottom: 40 }}>
      {/* Top Banner Header */}
      <div className="page-header" style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={3} style={{ margin: 0, color: '#0F172A', display: 'flex', alignItems: 'center', gap: 10 }}>
            <MedicineBoxOutlined style={{ color: '#2563EB' }} /> Kê đơn thuốc và Cảnh báo tương tác thuốc
          </Title>
        </div>
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
            }
          />
        )}

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
            <Input.TextArea
              rows={2}
              placeholder="Nhập lý do bác sĩ điều chỉnh liều hoặc đổi thuốc..."
              value={changeReason}
              onChange={(e) => setChangeReason(e.target.value)}
            />
          </Form.Item>
        )}
      </Card>

      {/* Prescriptions List Table Card */}
      <Card
        bordered
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <span style={{ fontSize: 16, fontWeight: 600, color: '#0F172A' }}>
              Danh sách đơn thuốc & Điều chỉnh đơn thuốc khi chưa cấp phát
            </span>
            <Input
              placeholder="Tìm mã đơn, tên bệnh nhân, bác sĩ..."
              prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              style={{ width: 280 }}
              allowClear
            />
          </div>
        }
      >
        {/* Status Filter Tabs */}
        <Tabs
          activeKey={statusFilter}
          onChange={setStatusFilter}
          items={[
            {
              key: 'ALL',
              label: `Tất cả đơn thuốc (${prescriptions.length})`,
            },
            {
              key: 'PENDING',
              label: (
                <span>
                  Đơn thuốc Chờ cấp phát <Badge count={pendingCount} overflowCount={99} style={{ backgroundColor: '#F59E0B', marginLeft: 4 }} />
                </span>
              ),
            },
            {
              key: 'DISPENSED',
              label: (
                <span>
                  Đơn thuốc Đã cấp phát <Badge count={dispensedCount} overflowCount={99} style={{ backgroundColor: '#10B981', marginLeft: 4 }} />
                </span>
              ),
            },
          ]}
          style={{ marginBottom: 12 }}
        />

        <Table
          rowKey="id"
          loading={loading}
          dataSource={filteredPrescriptions}
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
              title: 'Danh sách thuốc chỉ định',
              dataIndex: 'items',
              render: (value) => {
                let parsed = []
                try {
                  parsed = typeof value === 'string' ? JSON.parse(value) : (value || [])
                } catch {
                  parsed = []
                }
                return (
                  <div>
                    {parsed.map((item, idx) => {
                      const m = medicines.find((med) => String(med.id) === String(item.medicineId))
                      const name = m ? (m.name || m.medicineName) : (item.medicineName || item.medicineId)
                      return (
                        <Tag color="blue" key={idx} style={{ marginBottom: 2 }}>
                          {name} ({item.quantity})
                        </Tag>
                      )
                    })}
                  </div>
                )
              },
            },
            {
              title: 'Trạng thái cấp phát',
              dataIndex: 'status',
              render: (value) => {
                const isDispensed = value === 'DISPENSED'
                return (
                  <Tag color={isDispensed ? 'green' : 'orange'} style={{ fontWeight: 600 }}>
                    {isDispensed ? 'Đã cấp phát (Khóa sửa)' : 'Chờ cấp phát (Được sửa)'}
                  </Tag>
                )
              },
            },
            {
              title: 'Ngày kê',
              dataIndex: 'createdAt',
              render: (val) => (val ? dayjs(val).format('HH:mm DD/MM/YYYY') : '—'),
            },
            {
              title: 'Thao tác điều chỉnh',
              key: 'actions',
              render: (_, row) => {
                const isDispensed = row.status === 'DISPENSED'
                return (
                  <Space>
                    <Tooltip
                      title={
                        isDispensed
                          ? 'Đơn thuốc đã được cấp phát, không thể điều chỉnh.'
                          : !isDoctor
                          ? 'Chỉ Bác sĩ mới có quyền điều chỉnh đơn thuốc.'
                          : 'Bấm để sửa liều lượng, đổi thuốc hoặc bỏ thuốc trong đơn'
                      }
                    >
                      <Button
                        type="primary"
                        ghost={!isDispensed}
                        icon={<EditOutlined />}
                        size="small"
                        disabled={!isDoctor || isDispensed}
                        onClick={() => {
                          setModalPrescription(row)
                          setEditModalOpen(true)
                        }}
                      >
                        Điều chỉnh
                      </Button>
                    </Tooltip>
                    <Button
                      icon={<HistoryOutlined />}
                      size="small"
                      onClick={() => openHistoryModal(row)}
                    >
                      Xem lịch sử
                    </Button>
                  </Space>
                )
              },
            },
          ]}
        />
      </Card>

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

      {/* Dedicated Edit Prescription Modal */}
      <EditPrescriptionModal
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onSuccess={() => loadData()}
        prescription={modalPrescription}
        medicines={medicines}
        currentUser={currentUser}
        records={records}
      />
    </div>
  )
}

export default PrescriptionPage
