import React, { useState, useEffect, useCallback } from 'react'
import {
  Modal,
  Form,
  Select,
  InputNumber,
  Input,
  Button,
  Space,
  Alert,
  Tag,
  Typography,
  Descriptions,
  message,
  Tooltip,
  Divider,
} from 'antd'
import {
  EditOutlined,
  PlusOutlined,
  DeleteOutlined,
  WarningOutlined,
  LockOutlined,
  CheckCircleOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import pharmacyApi from '../../api/pharmacyApi'
import {
  checkMockDrugInteractions,
  updateMockPrescription,
} from '../../services/prescriptionMockRepository'
import InteractionWarningModal from './InteractionWarningModal'

const { Text, Title } = Typography

const createEmptyItem = () => ({
  id: `item-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
  medicineId: undefined,
  quantity: 1,
  dosage: '',
})

function EditPrescriptionModal({
  open,
  onCancel,
  onSuccess,
  prescription,
  medicines = [],
  currentUser,
  records = [],
}) {
  const [items, setItems] = useState([])
  const [changeReason, setChangeReason] = useState('')
  const [predefinedReason, setPredefinedReason] = useState('')
  const [note, setNote] = useState('')
  const [saving, setSaving] = useState(false)

  // Interaction check modal state
  const [detectedInteractions, setDetectedInteractions] = useState([])
  const [interactionModalOpen, setInteractionModalOpen] = useState(false)
  const [confirmedOverrides, setConfirmedOverrides] = useState([])

  // Parse items from prescription when modal opens
  useEffect(() => {
    if (prescription && open) {
      setNote(prescription.note || '')
      setChangeReason('')
      setPredefinedReason('')
      setConfirmedOverrides(prescription.interactionOverrides || [])

      let parsed = []
      try {
        parsed =
          typeof prescription.items === 'string'
            ? JSON.parse(prescription.items)
            : prescription.items || []
      } catch {
        parsed = []
      }

      const formatted = parsed.map((item) => ({
        id: item.id || `item-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
        medicineId: item.medicineId,
        dosage: item.dosage || '',
        quantity: item.quantity || 1,
      }))

      setItems(formatted.length ? formatted : [createEmptyItem()])
      performInteractionCheck(formatted)
    }
  }, [prescription, open])

  // Check interactions dynamically
  const performInteractionCheck = useCallback(
    async (currentItems) => {
      const medicineIds = [
        ...new Set(currentItems.map((item) => item.medicineId).filter(Boolean)),
      ]
      if (medicineIds.length < 2) {
        setDetectedInteractions([])
        return []
      }

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
    },
    [medicines]
  )

  const handleItemChange = (itemId, field, value) => {
    const updated = items.map((item) =>
      item.id === itemId ? { ...item, [field]: value } : item
    )
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
    if (items.length <= 1) {
      message.warning('Đơn thuốc phải có ít nhất 1 loại thuốc.')
      return
    }
    const updated = items.filter((item) => item.id !== itemId)
    setItems(updated)
    performInteractionCheck(updated)
    setConfirmedOverrides([])
  }

  const handlePredefinedReasonChange = (val) => {
    setPredefinedReason(val)
    if (val !== 'KHAC') {
      setChangeReason(val)
    } else {
      setChangeReason('')
    }
  }

  const isDispensed = prescription?.status === 'DISPENSED'

  const validateForm = () => {
    if (isDispensed) {
      return 'Đơn thuốc đã cấp phát, không được phép điều chỉnh.'
    }
    if (!items || items.length === 0) {
      return 'Danh sách thuốc không được để trống.'
    }
    for (let i = 0; i < items.length; i++) {
      const item = items[i]
      if (!item.medicineId) {
        return `Dòng ${i + 1}: Vui lòng chọn thuốc.`
      }
      if (!item.dosage || !String(item.dosage).trim()) {
        return `Dòng ${i + 1}: Vui lòng nhập liều dùng & hướng dẫn.`
      }
      if (
        !item.quantity ||
        Number(item.quantity) <= 0 ||
        !Number.isInteger(Number(item.quantity))
      ) {
        return `Dòng ${i + 1}: Số lượng phải là số nguyên lớn hơn 0.`
      }
    }

    const selectedIds = items.map((i) => i.medicineId).filter(Boolean)
    if (new Set(selectedIds).size !== selectedIds.length) {
      return 'Có thuốc trùng lặp trong đơn, vui lòng kiểm tra lại.'
    }

    const finalReason = changeReason || predefinedReason
    if (!finalReason || !finalReason.trim()) {
      return 'Vui lòng nhập lý do điều chỉnh đơn thuốc (bắt buộc để lưu vết thay đổi).'
    }

    return null
  }

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

    await executeUpdatePrescription(confirmedOverrides)
  }

  const handleConfirmOverrides = async (overrides) => {
    setConfirmedOverrides(overrides)
    setInteractionModalOpen(false)
    await executeUpdatePrescription(overrides)
  }

  const executeUpdatePrescription = async (overridesToSave = []) => {
    setSaving(true)
    const finalReason = (changeReason || predefinedReason || '').trim()

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

    try {
      // Try Backend Patch API first
      await pharmacyApi.updatePrescription(prescription.id, {
        note,
        changeReason: finalReason,
        items: formattedItems,
        interactionOverrides: formattedOverrides,
      })
      message.success('Đã điều chỉnh đơn thuốc và lưu vết thay đổi thành công trên Backend!')
    } catch {
      // Fallback to local storage mock update
      try {
        updateMockPrescription(
          prescription.id,
          {
            note,
            items: formattedItems,
            interactionOverrides: overridesToSave,
            changeReason: finalReason,
          },
          currentUser
        )
        message.success('Đã cập nhật đơn thuốc và lưu vết thay đổi lịch sử!')
      } catch (err) {
        message.error(`Không thể cập nhật đơn thuốc: ${err.message}`)
        setSaving(false)
        return
      }
    }

    setSaving(false)
    if (onSuccess) onSuccess()
    onCancel()
  }

  const matchedRecord = records.find(
    (r) =>
      String(r.id) === String(prescription?.medicalRecordId) ||
      String(r.recordCode) === String(prescription?.medicalRecordId)
  )

  return (
    <>
      <Modal
        open={open}
        title={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingRight: 24 }}>
            <Space>
              <EditOutlined style={{ color: '#2563EB', fontSize: 20 }} />
              <span style={{ fontSize: 18, fontWeight: 600, color: '#0F172A' }}>
                Điều Chỉnh Đơn Thuốc: {prescription?.prescriptionCode}
              </span>
            </Space>
            <Tag color={isDispensed ? 'red' : 'orange'} style={{ fontSize: 13, padding: '4px 10px' }}>
              {isDispensed ? 'Đã cấp phát (Khóa sửa)' : 'Trạng thái: Chờ cấp phát'}
            </Tag>
          </div>
        }
        onCancel={onCancel}
        width={900}
        footer={[
          <Button key="cancel" onClick={onCancel}>
            Hủy bỏ
          </Button>,
          <Button
            key="submit"
            type="primary"
            loading={saving}
            disabled={isDispensed}
            icon={<CheckCircleOutlined />}
            onClick={handleSaveClick}
            style={{ backgroundColor: '#2563EB' }}
          >
            Lưu Điều Chỉnh & Lưu Vết
          </Button>,
        ]}
      >
        {/* Requirement Banner Context */}
        <Alert
          type={isDispensed ? 'warning' : 'info'}
          showIcon
          icon={isDispensed ? <LockOutlined /> : <InfoCircleOutlined />}
          style={{ marginBottom: 16 }}
          message={
            isDispensed
              ? 'Đơn thuốc đã ở trạng thái ĐÃ CẤP PHÁT. Hệ thống không cho phép bác sĩ điều chỉnh.'
              : 'Bác sĩ được phép điều chỉnh đơn thuốc (sửa liều, đổi thuốc, hoặc BỎ THUỐC khỏi đơn) khi đơn còn ở trạng thái CHỜ CẤP PHÁT. Mọi thao tác sẽ được lưu vết lịch sử.'
          }
        />

        {/* Patient & Prescription Context Summary */}
        <Descriptions
          bordered
          size="small"
          column={{ xs: 1, sm: 2, md: 3 }}
          style={{ marginBottom: 16, backgroundColor: '#F8FAFC' }}
        >
          <Descriptions.Item label="Bệnh nhân">
            <Text strong>{prescription?.patientName || matchedRecord?.patientName || '—'}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Mã Bệnh Án">
            {matchedRecord?.recordCode || prescription?.medicalRecordId || '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Bác sĩ chỉ định">
            {prescription?.doctorName || currentUser?.fullName || 'BS. Phạm Hồng Anh'}
          </Descriptions.Item>
          <Descriptions.Item label="Chẩn đoán" span={3}>
            <Text strong style={{ color: '#1E40AF' }}>
              {matchedRecord?.diagnosis || 'Chẩn đoán bệnh theo lượt khám'}
            </Text>
          </Descriptions.Item>
        </Descriptions>

        {/* Medicines Edit Table / Section */}
        <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text strong style={{ fontSize: 14, color: '#1E293B' }}>
            Danh sách thuốc chỉ định (Bác sĩ có thể sửa hoặc bấm nút xóa để BỎ THUỐC khỏi đơn):
          </Text>
          {!isDispensed && (
            <Button
              type="dashed"
              icon={<PlusOutlined />}
              onClick={handleAddItem}
              style={{ color: '#2563EB', borderColor: '#93C5FD' }}
            >
              Thêm thuốc vào đơn
            </Button>
          )}
        </div>

        {items.map((item, index) => (
          <Space
            key={item.id}
            style={{
              display: 'flex',
              marginBottom: 10,
              width: '100%',
              backgroundColor: '#FAFAFA',
              padding: 8,
              borderRadius: 6,
              border: '1px solid #E2E8F0',
            }}
            align="start"
          >
            <Form.Item style={{ margin: 0, width: 340 }}>
              <Select
                showSearch
                disabled={isDispensed}
                optionFilterProp="label"
                placeholder="Chọn thuốc từ danh mục..."
                value={item.medicineId}
                onChange={(val) => handleItemChange(item.id, 'medicineId', val)}
                options={medicines.map((m) => ({
                  value: m.id,
                  label: `${m.name || m.medicineName} (${m.activeIngredient || 'Dược chất'} - Tồn: ${
                    m.stock !== undefined ? m.stock : 100
                  } ${m.unit || 'đơn vị'})`,
                }))}
              />
            </Form.Item>

            <Form.Item style={{ margin: 0, width: 140 }}>
              <InputNumber
                min={1}
                precision={0}
                disabled={isDispensed}
                value={item.quantity}
                onChange={(val) => handleItemChange(item.id, 'quantity', val)}
                addonBefore="SL"
                style={{ width: '100%' }}
              />
            </Form.Item>

            <Form.Item style={{ margin: 0, flex: 1 }}>
              <Input
                disabled={isDispensed}
                placeholder="Liều dùng & hướng dẫn (VD: Uống 2 viên/ngày chia 2 lần)"
                value={item.dosage}
                onChange={(e) => handleItemChange(item.id, 'dosage', e.target.value)}
              />
            </Form.Item>

            <Tooltip title="Bỏ thuốc này khỏi đơn">
              <Button
                danger
                icon={<DeleteOutlined />}
                disabled={isDispensed || items.length === 1}
                onClick={() => handleRemoveItem(item.id)}
              />
            </Tooltip>
          </Space>
        ))}

        {/* Dynamic Interaction Warnings Alert */}
        {detectedInteractions.length > 0 && (
          <Alert
            style={{ marginTop: 14 }}
            type="error"
            showIcon
            icon={<WarningOutlined />}
            message={`Cảnh báo: Phát hiện ${detectedInteractions.length} cặp tương tác thuốc nguy hiểm trong đơn!`}
            description={
              <div>
                {detectedInteractions.map((w, i) => (
                  <div key={i} style={{ marginTop: 4 }}>
                    <Tag color="red">{w.severity}</Tag>
                    <strong>
                      {w.drugNameA} + {w.drugNameB}:
                    </strong>{' '}
                    <span>{w.description}</span>
                  </div>
                ))}
              </div>
            }
          />
        )}

        <Divider style={{ margin: '16px 0' }} />

        {/* Change Audit Reason (Required as per postcondition "lưu vết") */}
        {!isDispensed && (
          <div style={{ backgroundColor: '#EFF6FF', padding: 14, borderRadius: 8, border: '1px solid #BFDBFE' }}>
            <Form.Item
              label={
                <strong style={{ color: '#1E40AF', fontSize: 14 }}>
                  Lý do điều chỉnh đơn thuốc (Bắt buộc để lưu vết lịch sử) *
                </strong>
              }
              required
              style={{ marginBottom: 8 }}
            >
              <Select
                placeholder="Chọn lý do mẫu hoặc nhập chi tiết bên dưới..."
                value={predefinedReason}
                onChange={handlePredefinedReasonChange}
                options={[
                  { value: 'Điều chỉnh liều dùng và số lượng thuốc', label: 'Điều chỉnh liều dùng và số lượng thuốc' },
                  { value: 'Bỏ thuốc không phù hợp với diễn biến lâm sàng', label: 'Bỏ thuốc không phù hợp với diễn biến lâm sàng' },
                  { value: 'Thay đổi thuốc do phát hiện nguy cơ tương tác thuốc', label: 'Thay đổi thuốc do phát hiện nguy cơ tương tác thuốc' },
                  { value: 'Bệnh nhân có phản ứng phụ / dị ứng thuốc', label: 'Bệnh nhân có phản ứng phụ / dị ứng thuốc' },
                  { value: 'KHAC', label: '-- Nhập lý do chi tiết khác --' },
                ]}
                style={{ marginBottom: 8 }}
              />

              <Input.TextArea
                rows={2}
                placeholder="Nhập lý do bác sĩ điều chỉnh đơn thuốc (ví dụ: Bỏ thuốc A do bệnh nhân ho hắng, giảm liều B...)..."
                value={changeReason}
                onChange={(e) => setChangeReason(e.target.value)}
              />
            </Form.Item>
          </div>
        )}
      </Modal>

      {/* Interaction Warning Modal */}
      <InteractionWarningModal
        open={interactionModalOpen}
        warnings={detectedInteractions}
        currentUser={currentUser}
        onCancel={() => setInteractionModalOpen(false)}
        onConfirmOverride={handleConfirmOverrides}
      />
    </>
  )
}

export default EditPrescriptionModal
