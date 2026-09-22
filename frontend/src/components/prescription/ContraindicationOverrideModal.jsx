import React, { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Divider,
  Form,
  Input,
  Modal,
  Space,
  Tag,
  Typography,
} from 'antd'
import {
  ArrowLeftOutlined,
  CheckOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  StopOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import {
  CONTRAINDICATION_SEVERITY_META,
  CONTRAINDICATION_TYPE_META,
  PRESET_CONTRAINDICATION_OVERRIDE_REASONS,
  validateContraindicationOverrideReason,
} from '../../utils/contraindicationValidation'

const { Text, Paragraph } = Typography
const { TextArea } = Input

function getSeverityBadge(severity) {
  const meta = CONTRAINDICATION_SEVERITY_META[severity] || {
    label: severity,
    tagColor: 'red',
  }
  return (
    <Tag color={meta.tagColor} style={{ fontWeight: 700, margin: 0 }}>
      {meta.label}
    </Tag>
  )
}

function getTypeBadge(type) {
  const meta = CONTRAINDICATION_TYPE_META[type] || {
    label: type,
    tagColor: 'default',
  }
  return (
    <Tag color={meta.tagColor} style={{ fontWeight: 600, margin: 0 }}>
      {meta.label}
    </Tag>
  )
}

export default function ContraindicationOverrideModal({
  open,
  warnings = [],
  initialOverrides = [],
  patientName = '',
  onCancel,
  onConfirmOverride,
}) {
  // Lưu trữ lý do nhập cho từng cảnh báo theo key: `${ruleId}_${medicineId}`
  const [reasonsMap, setReasonsMap] = useState({})
  const [errorsMap, setErrorsMap] = useState({})
  const [globalError, setGlobalError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (open) {
      const initialMap = {}
      ;(initialOverrides || []).forEach((ov) => {
        if (ov?.ruleId && ov?.medicineId) {
          initialMap[`${ov.ruleId}_${ov.medicineId}`] = ov.overrideReason || ''
        }
      })
      setReasonsMap(initialMap)
      setErrorsMap({})
      setGlobalError('')
      setSubmitting(false)
    }
  }, [open, initialOverrides])

  const handleReasonChange = (ruleId, medicineId, value) => {
    const key = `${ruleId}_${medicineId}`
    setReasonsMap((prev) => ({ ...prev, [key]: value }))
    if (errorsMap[key]) {
      setErrorsMap((prev) => {
        const next = { ...prev }
        delete next[key]
        return next
      })
    }
    setGlobalError('')
  }

  const handleApplyPreset = (ruleId, medicineId, preset) => {
    handleReasonChange(ruleId, medicineId, preset)
  }

  const handleApplyPresetToAll = (preset) => {
    const nextMap = {}
    warnings.forEach((w) => {
      nextMap[`${w.ruleId}_${w.medicineId}`] = preset
    })
    setReasonsMap(nextMap)
    setErrorsMap({})
    setGlobalError('')
  }

  const handleConfirm = async () => {
    if (submitting) return
    const nextErrors = {}
    let hasError = false
    const overrides = []

    warnings.forEach((w) => {
      const key = `${w.ruleId}_${w.medicineId}`
      const val = reasonsMap[key] || ''
      const res = validateContraindicationOverrideReason(val)

      if (!res.valid) {
        nextErrors[key] = res.error
        hasError = true
      } else {
        overrides.push({
          ruleId: w.ruleId,
          medicineId: w.medicineId,
          overrideReason: res.trimmedReason,
        })
      }
    })

    if (hasError) {
      setErrorsMap(nextErrors)
      setGlobalError('Vui lòng nhập lý do chuyên môn hợp lệ cho tất cả các cảnh báo chống chỉ định.')
      return
    }

    setSubmitting(true)
    try {
      await onConfirmOverride(overrides)
    } finally {
      setSubmitting(false)
    }
  }

  const hasAbsoluteContraindication = warnings.some(
    (w) => w.severity === 'CONTRAINDICATED',
  )

  return (
    <Modal
      open={open}
      title={
        <Space style={{ color: '#b91c1c' }}>
          <StopOutlined style={{ fontSize: 20 }} />
          <span style={{ fontWeight: 700, fontSize: 16 }}>
            XÁC NHẬN BỎ QUA CẢNH BÁO CHỐNG CHỈ ĐỊNH LÂM SÀNG
          </span>
        </Space>
      }
      onCancel={onCancel}
      width={780}
      destroyOnClose
      footer={[
        <Button key="back" icon={<ArrowLeftOutlined />} onClick={onCancel} disabled={submitting}>
          Quay lại điều chỉnh đơn
        </Button>,
        <Button
          key="submit"
          type="primary"
          danger
          icon={<CheckOutlined />}
          onClick={handleConfirm}
          loading={submitting}
          disabled={submitting}
          style={{ fontWeight: 600 }}
        >
          Xác nhận ghi đè & Tiếp tục
        </Button>,
      ]}
    >
      <div style={{ marginBottom: 12 }}>
        <Paragraph style={{ marginBottom: 6 }}>
          Bệnh nhân: <strong>{patientName || 'Bệnh nhân'}</strong>
        </Paragraph>

        {hasAbsoluteContraindication ? (
          <Alert
            type="error"
            showIcon
            icon={<StopOutlined />}
            message="PHÁT HIỆN THUỐC CÓ MỨC CHỐNG CHỈ ĐỊNH TUYỆT ĐỐI"
            description="Đơn thuốc chứa thuốc có mức chống chỉ định cao nhất (CONTRAINDICATED). Nếu vẫn quyết định chỉ định, bác sĩ bắt buộc phải ghi rõ căn cứ hội chẩn hoặc lý do lâm sàng đặc biệt để lưu nhật ký kiểm toán y khoa."
            style={{ marginBottom: 12 }}
          />
        ) : (
          <Alert
            type="warning"
            showIcon
            icon={<WarningOutlined />}
            message="CẢNH BÁO AN TOÀN KÊ ĐƠN THUỐC"
            description="Hệ thống phát hiện thuốc trong đơn có nguy cơ đối với độ tuổi, tình trạng thai kỳ hoặc bệnh nền của bệnh nhân. Vui lòng nhập lý do chuyên môn cho từng cảnh báo để tiếp tục."
            style={{ marginBottom: 12 }}
          />
        )}

        {/* Nút áp dụng lý do mẫu cho tất cả nếu có nhiều cảnh báo */}
        {warnings.length > 1 && (
          <div style={{ background: '#f8fafc', padding: '8px 12px', borderRadius: 6, marginBottom: 12, border: '1px solid #e2e8f0' }}>
            <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
              Áp dụng nhanh lý do mẫu cho tất cả cảnh báo:
            </Text>
            <Space wrap size="small">
              {PRESET_CONTRAINDICATION_OVERRIDE_REASONS.map((preset, pIdx) => (
                <Button
                  key={pIdx}
                  size="small"
                  onClick={() => handleApplyPresetToAll(preset)}
                >
                  {preset.slice(0, 32)}...
                </Button>
              ))}
            </Space>
          </div>
        )}

        {globalError && (
          <Alert
            type="error"
            showIcon
            message={globalError}
            style={{ marginBottom: 12 }}
          />
        )}
      </div>

      <div style={{ maxHeight: 440, overflowY: 'auto', paddingRight: 4 }}>
        {warnings.map((w, idx) => {
          const key = `${w.ruleId}_${w.medicineId}`
          const reasonVal = reasonsMap[key] || ''
          const itemError = errorsMap[key]

          return (
            <Card
              key={key}
              size="small"
              style={{
                marginBottom: 12,
                borderRadius: 8,
                border: itemError ? '1.5px solid #ef4444' : '1px solid #e2e8f0',
                borderLeft: `5px solid ${
                  w.severity === 'CONTRAINDICATED'
                    ? '#991b1b'
                    : w.severity === 'SEVERE'
                    ? '#dc2626'
                    : w.severity === 'MODERATE'
                    ? '#d97706'
                    : '#0284c7'
                }`,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                <Space size="small" wrap>
                  <Text strong style={{ fontSize: 14, color: '#0f172a' }}>
                    {idx + 1}. {w.medicineName || 'Thuốc'}
                  </Text>
                  {getSeverityBadge(w.severity)}
                  {getTypeBadge(w.type)}
                </Space>
              </div>

              {/* Tách biệt 2 nội dung message và recommendation */}
              <div style={{ background: '#f8fafc', padding: '8px 10px', borderRadius: 6, marginBottom: 8 }}>
                <div style={{ fontSize: 13, color: '#1e293b', marginBottom: 4 }}>
                  <strong>Thông điệp:</strong> {w.message}
                </div>
                {w.recommendation && (
                  <div style={{ fontSize: 12.5, color: '#0369a1', fontStyle: 'italic' }}>
                    💡 <strong>Khuyến cáo:</strong> {w.recommendation}
                  </div>
                )}
              </div>

              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                  <Text strong style={{ fontSize: 12.5, color: '#334155' }}>
                    Lý do chuyên môn bỏ qua cảnh báo <span style={{ color: '#ef4444' }}>*</span>:
                  </Text>
                </div>

                <TextArea
                  rows={2}
                  placeholder="Nhập lý do chuyên môn cụ thể để ghi nhận vào biên bản kiểm toán y khoa..."
                  value={reasonVal}
                  onChange={(e) => handleReasonChange(w.ruleId, w.medicineId, e.target.value)}
                  status={itemError ? 'error' : ''}
                />

                {itemError && (
                  <div style={{ color: '#dc2626', fontSize: 12, marginTop: 4 }}>
                    {itemError}
                  </div>
                )}

                {/* Gợi ý nhanh */}
                <div style={{ marginTop: 6 }}>
                  <Space wrap size={[4, 4]}>
                    <Text type="secondary" style={{ fontSize: 11.5 }}>Gợi ý nhanh:</Text>
                    {PRESET_CONTRAINDICATION_OVERRIDE_REASONS.map((preset, pIdx) => (
                      <Tag
                        key={pIdx}
                        style={{ cursor: 'pointer', fontSize: 11.5 }}
                        onClick={() => handleApplyPreset(w.ruleId, w.medicineId, preset)}
                      >
                        {preset.slice(0, 28)}...
                      </Tag>
                    ))}
                  </Space>
                </div>
              </div>
            </Card>
          )
        })}
      </div>
    </Modal>
  )
}
