import React, { useState, useEffect } from 'react'
import { Modal, Alert, List, Input, Form, Tag, Button, Typography, Space } from 'antd'
import {
  WarningOutlined,
  ExclamationCircleOutlined,
  CheckOutlined,
  ArrowLeftOutlined,
  FireOutlined,
} from '@ant-design/icons'
import {
  PRESET_ALLERGY_OVERRIDE_REASONS,
  validateAllergyOverrideReason,
} from '../../utils/prescriptionAllergyValidation.js'

const { Text, Paragraph } = Typography
const { TextArea } = Input

function renderSeverityTag(severity) {
  if (!severity) return null
  const sev = String(severity).toUpperCase()
  switch (sev) {
    case 'ANAPHYLAXIS':
      return (
        <Tag color="#b91c1c" style={{ fontWeight: 700 }}>
          SỐC PHẢN VỆ (NGUY HIỂM TÍNH MẠNG)
        </Tag>
      )
    case 'SEVERE':
      return (
        <Tag color="red" style={{ fontWeight: 700 }}>
          Nghiêm trọng (SEVERE)
        </Tag>
      )
    case 'MODERATE':
      return (
        <Tag color="orange" style={{ fontWeight: 600 }}>
          Trung bình (MODERATE)
        </Tag>
      )
    case 'MILD':
      return (
        <Tag color="gold" style={{ fontWeight: 600 }}>
          Nhẹ (MILD)
        </Tag>
      )
    default:
      return <Tag color="volcano">{severity}</Tag>
  }
}

/**
 * Blocking Warning Modal for Prescription Medication Allergies (NCL-05-CN-004 / QTN-26)
 */
function PrescriptionAllergyWarningModal({
  open,
  warnings = [],
  patientName = '',
  onCancel,
  onConfirmOverride,
}) {
  const [overrideReason, setOverrideReason] = useState('')
  const [errorMsg, setErrorMsg] = useState('')
  const [showReasonInput, setShowReasonInput] = useState(true)

  useEffect(() => {
    if (open) {
      setOverrideReason('')
      setErrorMsg('')
      setShowReasonInput(true)
    }
  }, [open])

  const handlePresetSelect = (preset) => {
    setOverrideReason(preset)
    setErrorMsg('')
  }

  const handleContinueClick = () => {
    const { valid, error, trimmedReason } = validateAllergyOverrideReason(overrideReason)
    if (!valid) {
      setErrorMsg(error)
      return
    }

    const overrides = warnings.map((w) => ({
      allergyId: w.allergyId,
      medicineId: w.medicineId,
      overrideReason: trimmedReason,
    }))

    onConfirmOverride(overrides)
  }

  const hasAnaphylaxisRisk = warnings.some(
    (w) => String(w.severity).toUpperCase() === 'ANAPHYLAXIS' || String(w.severity).toUpperCase() === 'SEVERE'
  )

  return (
    <Modal
      open={open}
      title={
        <Space style={{ color: '#DC2626' }}>
          <FireOutlined style={{ fontSize: 20 }} />
          <span style={{ fontWeight: 700, fontSize: 16 }}>
            {hasAnaphylaxisRisk ? 'CẢNH BÁO SỐC PHẢN VỆ: TRÙNG TIỀN SỬ DỊ ỨNG THUỐC' : 'CẢNH BÁO DỊ ỨNG THUỐC TRONG ĐƠN'}
          </span>
        </Space>
      }
      onCancel={onCancel}
      width={720}
      destroyOnClose
      footer={[
        <Button key="back" icon={<ArrowLeftOutlined />} onClick={onCancel}>
          Quay lại đổi thuốc khác
        </Button>,
        <Button
          key="submit"
          type="primary"
          danger
          icon={<CheckOutlined />}
          onClick={handleContinueClick}
          disabled={!overrideReason.trim()}
        >
          Xác nhận tiếp tục kê đơn
        </Button>,
      ]}
    >
      <Alert
        type="error"
        showIcon
        icon={<WarningOutlined style={{ color: '#dc2626' }} />}
        style={{ marginBottom: 16, backgroundColor: '#fef2f2', border: '1px solid #fca5a5' }}
        message={
          <span style={{ fontWeight: 700, color: '#991b1b' }}>
            Phát hiện {warnings.length} thuốc trùng tiền sử dị ứng đã ghi nhận{patientName ? ` của bệnh nhân “${patientName}”` : ''}!
          </span>
        }
        description={
          <div style={{ color: '#7f1d1d', marginTop: 4 }}>
            Kê thuốc có hoạt chất trùng tiền sử dị ứng có thể gây phản ứng quá mẫn, sốc phản vệ và nguy hiểm tính mạng người bệnh.
            Bác sĩ phải kiểm tra kỹ lưỡng và bắt buộc nhập lý do lâm sàng để tiếp tục kê đơn.
          </div>
        }
      />

      <List
        bordered
        dataSource={warnings}
        style={{ marginBottom: 16, backgroundColor: '#FFF5F5', borderColor: '#FECACA', borderRadius: 8 }}
        renderItem={(w, idx) => (
          <List.Item key={`${w.allergyId}_${w.medicineId}_${idx}`} style={{ padding: '12px 16px' }}>
            <div style={{ width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6, flexWrap: 'wrap', gap: 8 }}>
                <Text strong style={{ fontSize: 14, color: '#991B1B' }}>
                  Thuốc #{idx + 1}: {w.medicineName || 'Thuốc'}
                </Text>
                {renderSeverityTag(w.severity)}
              </div>

              <div style={{ fontSize: 13, color: '#374151', lineHeight: '1.6' }}>
                <div>
                  <strong>Hoạt chất thuốc:</strong> <Text code>{w.activeIngredient || '—'}</Text>
                </div>
                <div>
                  <strong>Dị nguyên ghi nhận:</strong> <Text strong style={{ color: '#dc2626' }}>{w.allergenName || '—'}</Text>
                </div>
                {w.reaction && (
                  <div>
                    <strong>Biểu hiện phản ứng trước đây:</strong>{' '}
                    <Text type="secondary" style={{ fontStyle: 'italic' }}>
                      {w.reaction}
                    </Text>
                  </div>
                )}
              </div>
            </div>
          </List.Item>
        )}
      />

      <div style={{ marginTop: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
          <Text strong style={{ color: '#1E293B' }}>
            Lý do lâm sàng bỏ qua cảnh báo dị ứng <span style={{ color: '#DC2626' }}>*</span>
          </Text>
        </div>

        <div style={{ marginBottom: 8 }}>
          <Space wrap size={[6, 6]}>
            <Text type="secondary" style={{ fontSize: 12 }}>Chọn nhanh lý do:</Text>
            {PRESET_ALLERGY_OVERRIDE_REASONS.map((preset, pIdx) => (
              <Button
                key={pIdx}
                size="small"
                type={overrideReason === preset ? 'primary' : 'default'}
                onClick={() => handlePresetSelect(preset)}
                style={{ fontSize: 12, height: 'auto', padding: '2px 8px', borderRadius: 4 }}
              >
                {preset}
              </Button>
            ))}
          </Space>
        </div>

        <TextArea
          rows={3}
          maxLength={500}
          showCount
          value={overrideReason}
          placeholder="Nhập chi tiết lý do lâm sàng bắt buộc (ví dụ: Đã hội chẩn, cân nhắc lợi ích vượt trội, đã chuẩn bị sẵn phác đồ chống sốc phản vệ...)"
          onChange={(e) => {
            setOverrideReason(e.target.value)
            if (errorMsg) setErrorMsg('')
          }}
          status={errorMsg ? 'error' : ''}
        />

        {errorMsg && (
          <div style={{ color: '#DC2626', fontSize: 13, marginTop: 4 }}>
            {errorMsg}
          </div>
        )}
      </div>
    </Modal>
  )
}

export default PrescriptionAllergyWarningModal
