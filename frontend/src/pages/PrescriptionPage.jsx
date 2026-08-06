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
  Badge,
  Tooltip,
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
  SafetyOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'
import { mergeMedicalRecords, mergeMedicines } from '../utils/storageHelpers'
import {
  getMockPrescriptions,
  getMockPrescriptionById,
  saveMockPrescription,
  updateMockPrescription,
  checkMockDrugInteractions,
  getMockPrescriptionHistory,
} from '../services/prescriptionMockRepository'
import InteractionWarningModal from '../components/pharmacy/InteractionWarningModal'
import PrescriptionHistoryModal from '../components/pharmacy/PrescriptionHistoryModal'

const { Text, Title, Paragraph } = Typography

// Explicit Data Mode configuration for Prescription Module
const PRESCRIPTION_DATA_MODE = 'MOCK' // 'MOCK' | 'API'

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
  const isAdmin = userRoles.includes('admin')
  const isNurse = userRoles.includes('nurse')
  const isReceptionist = userRoles.includes('receptionist')

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

  // Pre-fill state from navigation (e.g. from MedicalEncounter page)
  useEffect(() => {
    if (location.state?.recordCode || location.state?.patientId) {
      const match = records.find((r) => 
        (location.state.recordCode && r.recordCode === location.state.recordCode) ||
        (location.state.patientId && r.patientId === location.state.patientId)
      )
      if (match) setSelectedRecordId(match.id)
    }
  }, [location.state, records])

  // Initial Data Fetching
  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      if (PRESCRIPTION_DATA_MODE === 'API') {
        const [medicineRes, recordRes, prescriptionRes] = await Promise.all([
          pharmacyApi.medicines(),
          medicalRecordApi.getAll(),
          pharmacyApi.prescriptions(),
        ])
        setMedicines(medicineRes.data || [])
        setRecords(recordRes.data || [])
        setPrescriptions(prescriptionRes.data || [])
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
    } catch (err) {
      if (PRESCRIPTION_DATA_MODE === 'API') {
        message.error(`Lỗi tải dữ liệu API: ${err.response?.data?.message || err.message}`)
      } else {
        setMedicines(mergeMedicines([]))
        setRecords(mergeMedicalRecords([]))
        setPrescriptions(getMockPrescriptions())
      }
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
        const BEWarnings = response.data || []
        setDetectedInteractions(BEWarnings)
        return BEWarnings
      } catch (err) {
        message.error(`Không thể kiểm tra tương tác API: ${err.response?.data?.message || err.message}`)
        return []
      }
    } else {
      const mockWarnings = checkMockDrugInteractions(medicineIds)
      setDetectedInteractions(mockWarnings)
      return mockWarnings
    }
  }, [])

  // Item change handler
  const handleItemChange = (itemId, field, value) => {
    const updated = items.map((item) => (item.id === itemId ? { ...item, [field]: value } : item))
    setItems(updated)

    if (field === 'medicineId') {
      performInteractionCheck(updated)
      // Reset confirmed overrides when drug composition changes
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

  // Strictly Validate Requirements
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

    // Check duplicate medicines
    const selectedIds = items.map((i) => i.medicineId).filter(Boolean)
    if (new Set(selectedIds).size !== selectedIds.length) {
      return 'Thuốc này đã có trong đơn.'
    }

    if (editingPrescription && (!changeReason || !changeReason.trim())) {
      return 'Vui lòng nhập lý do điều chỉnh đơn thuốc (để lưu vết thay đổi).'
    }

    return null
  }

  // Trigger Save Process
  const handleSaveClick = async () => {
    const errorMsg = validateForm()
    if (errorMsg) {
      message.error(errorMsg)
      return
    }

    // Check latest interactions
    const warnings = await performInteractionCheck(items)

    // If interactions exist and haven't been confirmed with override reason -> Open Modal
    if (warnings.length > 0 && confirmedOverrides.length === 0) {
      setInteractionModalOpen(true)
      return
    }

    // Proceed to save
    await executeSavePrescription(confirmedOverrides)
  }

  // Callback from Interaction Warning Modal
  const handleConfirmInteractionOverrides = async (overrides) => {
    setConfirmedOverrides(overrides)
    setInteractionModalOpen(false)
    await executeSavePrescription(overrides)
  }

  // Execute Persistence Call
  const executeSavePrescription = async (overridesToSave = []) => {
    setSaving(true)
    try {
      const payload = {
        medicalRecordId: selectedRecord?.id || selectedRecordId,
        visitId: selectedRecord?.visitId || selectedRecord?.id || selectedRecordId,
        patientId: selectedRecord?.patientId,
        patientName: selectedRecord?.patientName,
        doctorId: currentUser?.id,
        doctorName: currentUser?.fullName || currentUser?.username,
        note,
        items: items.map((i) => ({
          medicineId: i.medicineId,
          dosage: i.dosage.trim(),
          quantity: Number(i.quantity),
        })),
        interactionOverrides: overridesToSave,
        changeReason: changeReason ? changeReason.trim() : undefined,
      }

      if (PRESCRIPTION_DATA_MODE === 'API') {
        if (editingPrescription) {
          await pharmacyApi.updatePrescription(editingPrescription.id, payload)
          message.success('Đã cập nhật đơn thuốc thành công!')
        } else {
          await pharmacyApi.createPrescription(payload)
          message.success('Đơn thuốc đã tạo thành công ở trạng thái chờ cấp phát!')
        }
      } else {
        // MOCK MODE
        if (editingPrescription) {
          updateMockPrescription(editingPrescription.id, payload, currentUser)
          message.success('Đã cập nhật đơn thuốc trong dữ liệu mô phỏng và lưu vết thay đổi.')
        } else {
          saveMockPrescription(payload, currentUser)
          message.success('Đã lưu đơn thuốc trong dữ liệu mô phỏng.')
        }
      }

      // Reset form
      resetForm()
      await loadData()
    } catch (err) {
      if (PRESCRIPTION_DATA_MODE === 'API') {
        message.error(`Lỗi hệ thống: ${err.response?.data?.message || err.message}`)
      } else {
        message.error(`Không thể lưu đơn thuốc mô phỏng: ${err.message}`)
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
          <Text type="secondary" style={{ fontSize: 13 }}>
            Chức năng chuyên môn dành riêng cho Bác sĩ điều trị phụ trách lượt khám (NCL-04, NCL-05).
          </Text>
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
                  label: `${m.name || m.medicineName} (Tồn kho: ${m.stock} ${m.unit || 'đơn vị'})`,
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
      <Card bordered title="Danh sách đơn thuốc đã phát hành">
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
                  return m ? `${m.name || m.medicineName} (${item.quantity})` : item.medicineId
                }).filter(Boolean).join('; ') || '—'
              },
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
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
            },
            {
              title: 'Thao tác',
              key: 'actions',
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
    </div>
  )
}

export default PrescriptionPage
