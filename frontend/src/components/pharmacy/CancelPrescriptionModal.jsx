import React, { useState, useEffect, useMemo } from 'react'
import {
  Modal,
  Alert,
  Input,
  Space,
  Tag,
  Typography,
  Button,
  Divider,
} from 'antd'
import {
  ExclamationCircleOutlined,
  StopOutlined,
  MedicineBoxOutlined,
  UserOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  CheckOutlined,
  IdcardOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  PRESET_CANCEL_REASONS,
  validateCancelPrescriptionReason,
} from '../../utils/prescriptionCancelValidation.js'
import { formatPrescriptionCode } from '../../utils/electronicPrescriptionValidation.js'
import { fixMojibake } from '../../utils/serviceCatalogValidation.js'

const { Text, Paragraph } = Typography
const { TextArea } = Input

/**
 * Modal Hủy đơn thuốc chưa cấp phát (NCL-05-CN-005)
 * Hủy có lý do, không thể hoàn tác
 * Tuân thủ TC-01, TC-02
 */
export default function CancelPrescriptionModal({
  open,
  onClose,
  prescription,
  onConfirm,
  loading = false,
}) {
  const [reason, setReason] = useState('')
  const [touched, setTouched] = useState(false)
  const [selectedPreset, setSelectedPreset] = useState(null)

  // Reset form khi mở modal mới
  useEffect(() => {
    if (open) {
      setReason('')
      setTouched(false)
      setSelectedPreset(null)
    }
  }, [open, prescription])

  const validation = useMemo(() => {
    return validateCancelPrescriptionReason(reason)
  }, [reason])

  if (!prescription) return null

  const prescriptionCode = formatPrescriptionCode(
    prescription.prescriptionCode || prescription.id,
  )
  const itemCount = (prescription.items || []).length
  const patientName = fixMojibake(prescription.patientName || prescription.patient?.fullName || '—')
  const doctorName = fixMojibake(prescription.doctorName || prescription.doctor?.fullName || '—')
  const rawDate = prescription.prescribedAt || prescription.createdAt
  const prescribedAt = rawDate
    ? dayjs(rawDate).format('HH:mm - DD/MM/YYYY')
    : '—'

  const handleSelectPreset = (presetText) => {
    setSelectedPreset(presetText)
    setReason(presetText)
    setTouched(true)
  }

  const handleReasonChange = (e) => {
    const val = e.target.value
    setReason(val)
    setTouched(true)
    if (selectedPreset && val !== selectedPreset) {
      setSelectedPreset(null)
    }
  }

  const handleSubmit = async () => {
    setTouched(true)
    if (!validation.valid) {
      return
    }
    if (onConfirm) {
      await onConfirm(prescription, validation.reason)
    }
  }

  return (
    <Modal
      open={open}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              backgroundColor: '#fee2e2',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#dc2626',
              fontSize: 18,
            }}
          >
            <StopOutlined />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#991b1b' }}>
              Hủy đơn thuốc chưa cấp phát
            </div>
            <div style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
              Mã đơn điện tử: <Text strong code>{prescriptionCode}</Text>
            </div>
          </div>
        </div>
      }
      onCancel={onClose}
      destroyOnClose
      width={640}
      footer={[
        <Button key="back" onClick={onClose} disabled={loading}>
          Bỏ qua
        </Button>,
        <Button
          key="submit"
          type="primary"
          danger
          icon={<StopOutlined />}
          loading={loading}
          disabled={loading || (touched && !validation.valid) || !reason.trim()}
          onClick={handleSubmit}
          id="btn-confirm-cancel-prescription"
        >
          Xác nhận hủy đơn
        </Button>,
      ]}
      style={{ top: 30 }}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16, marginTop: 12 }}>
        {/* Banner cảnh báo nghiệp vụ */}
        <Alert
          type="error"
          showIcon
          icon={<ExclamationCircleOutlined style={{ fontSize: 18, color: '#dc2626' }} />}
          message={
            <span style={{ fontWeight: 600, color: '#991b1b' }}>
              Cảnh báo: Hành động này không thể hoàn tác!
            </span>
          }
          description={
            <div style={{ fontSize: 12.5, lineHeight: 1.5, color: '#7f1d1d', marginTop: 4 }}>
              Theo quy tắc nghiệp vụ, đơn thuốc sau khi hủy sẽ chuyển sang trạng thái <strong>ĐÃ HỦY (CANCELLED)</strong>, không thể phục hồi và sẽ tự động biến mất khỏi danh sách chờ cấp phát của Dược sĩ.
            </div>
          }
          style={{ backgroundColor: '#fef2f2', borderColor: '#fecaca', borderRadius: 8 }}
        />

        {/* Thông tin vắn tắt đơn thuốc */}
        <div
          style={{
            backgroundColor: '#f8fafc',
            borderRadius: 8,
            border: '1px solid #e2e8f0',
            padding: '12px 16px',
          }}
        >
          <div
            style={{
              fontSize: 13,
              fontWeight: 600,
              color: '#334155',
              marginBottom: 10,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            <MedicineBoxOutlined style={{ color: '#2563eb' }} /> Thông tin đơn thuốc cần hủy:
          </div>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              rowGap: 10,
              columnGap: 20,
              fontSize: 13,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
              <UserOutlined style={{ color: '#64748b', flexShrink: 0 }} />
              <span style={{ color: '#64748b', whiteSpace: 'nowrap', flexShrink: 0 }}>Bệnh nhân:</span>
              <Text strong ellipsis={{ tooltip: patientName }} style={{ color: '#0f172a', minWidth: 0 }}>
                {patientName}
              </Text>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
              <ClockCircleOutlined style={{ color: '#64748b', flexShrink: 0 }} />
              <span style={{ color: '#64748b', whiteSpace: 'nowrap', flexShrink: 0 }}>Ngày kê đơn:</span>
              <Text strong style={{ color: '#0f172a', whiteSpace: 'nowrap' }}>
                {prescribedAt}
              </Text>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
              <IdcardOutlined style={{ color: '#64748b', flexShrink: 0 }} />
              <span style={{ color: '#64748b', whiteSpace: 'nowrap', flexShrink: 0 }}>Bác sĩ kê đơn:</span>
              <Text strong ellipsis={{ tooltip: doctorName }} style={{ color: '#0f172a', minWidth: 0 }}>
                {doctorName}
              </Text>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
              <FileTextOutlined style={{ color: '#64748b', flexShrink: 0 }} />
              <span style={{ color: '#64748b', whiteSpace: 'nowrap', flexShrink: 0 }}>Quy mô đơn:</span>
              <Tag color="blue" style={{ margin: 0, fontWeight: 500 }}>
                {itemCount} loại thuốc
              </Tag>
            </div>
          </div>
        </div>

        {/* Gợi ý lý do chọn nhanh */}
        <div>
          <div style={{ fontSize: 13, fontWeight: 600, color: '#1e293b', marginBottom: 8 }}>
            Gợi ý lý do hủy nhanh (chọn để điền):
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {PRESET_CANCEL_REASONS.map((preset) => {
              const isSelected = selectedPreset === preset
              return (
                <Button
                  key={preset}
                  size="small"
                  type={isSelected ? 'primary' : 'default'}
                  icon={isSelected ? <CheckOutlined /> : null}
                  onClick={() => handleSelectPreset(preset)}
                  style={{
                    fontSize: 12,
                    borderRadius: 16,
                    height: 'auto',
                    padding: '4px 10px',
                    borderColor: isSelected ? '#dc2626' : '#cbd5e1',
                    backgroundColor: isSelected ? '#fee2e2' : undefined,
                    color: isSelected ? '#991b1b' : '#334155',
                  }}
                >
                  {preset}
                </Button>
              )
            })}
          </div>
        </div>

        {/* Ô nhập lý do hủy chi tiết */}
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: '#0f172a' }}>
              <span style={{ color: '#ef4444' }}>* </span>
              Lý do hủy đơn thuốc:
            </span>
            <span
              style={{
                fontSize: 12,
                color: reason.length > 500 ? '#ef4444' : reason.length > 400 ? '#f59e0b' : '#64748b',
                fontWeight: reason.length > 500 ? 700 : 400,
              }}
            >
              {reason.length} / 500 ký tự
            </span>
          </div>
          <TextArea
            rows={4}
            value={reason}
            onChange={handleReasonChange}
            onBlur={() => setTouched(true)}
            placeholder="Nhập chi tiết lý do hủy đơn thuốc (bắt buộc, từ 1 đến 500 ký tự)..."
            status={touched && !validation.valid ? 'error' : ''}
            maxLength={520}
            style={{ borderRadius: 6 }}
            id="input-cancel-prescription-reason"
          />
          {touched && !validation.valid && (
            <div style={{ color: '#ef4444', fontSize: 12, marginTop: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
              <ExclamationCircleOutlined /> {validation.error}
            </div>
          )}
          <div style={{ fontSize: 11.5, color: '#64748b', marginTop: 4 }}>
            Lý do hủy sẽ được ghi nhận vào nhật ký kiểm toán hệ thống (Audit Log) để phục vụ tra soát.
          </div>
        </div>
      </div>
    </Modal>
  )
}
