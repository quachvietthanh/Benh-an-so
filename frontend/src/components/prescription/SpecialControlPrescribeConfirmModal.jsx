import React, { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Form,
  Input,
  Modal,
  Space,
  Tag,
  Typography,
} from 'antd'
import {
  AlertOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  MedicineBoxOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import SpecialControlBadge from '../pharmacy/SpecialControlBadge.jsx'
import {
  PRESET_SPECIAL_CONTROL_PRESCRIBE_REASONS,
  getSpecialControlMeta,
  validatePrescribeConfirmReason,
} from '../../utils/specialControlHelpers.js'

const { Text, Paragraph } = Typography
const { TextArea } = Input

export default function SpecialControlPrescribeConfirmModal({
  open,
  medicine,
  patient,
  initialReason = '',
  onConfirm,
  onCancel,
  submitting = false,
}) {
  const [form] = Form.useForm()
  const [reasonValue, setReasonValue] = useState(initialReason || '')

  useEffect(() => {
    if (open && medicine) {
      form.setFieldsValue({ reason: initialReason || '' })
      setReasonValue(initialReason || '')
    }
  }, [open, medicine, initialReason, form])

  if (!medicine) return null

  const meta = getSpecialControlMeta(medicine.specialControlGroup)
  const isCritical = meta?.severity === 'critical'
  const patientName = patient?.name || patient?.fullName || 'Bệnh nhân'
  const patientCode = patient?.code || patient?.patientCode || '—'

  const handleApplyPreset = (preset) => {
    form.setFieldsValue({ reason: preset })
    setReasonValue(preset)
  }

  const handleFinish = (values) => {
    const { valid, error, trimmedReason } = validatePrescribeConfirmReason(values.reason)
    if (!valid) {
      return
    }
    if (onConfirm) {
      onConfirm(trimmedReason)
    }
  }

  return (
    <Modal
      open={open}
      title={
        <Space align="center">
          <AlertOutlined style={{ color: isCritical ? '#dc2626' : '#d97706', fontSize: 20 }} />
          <span style={{ fontWeight: 700, fontSize: 16 }}>
            Xác nhận kê thuốc kiểm soát đặc biệt
          </span>
        </Space>
      }
      onCancel={submitting ? undefined : onCancel}
      maskClosable={false}
      keyboard={!submitting}
      destroyOnClose
      width={680}
      footer={[
        <Button
          key="cancel"
          onClick={onCancel}
          disabled={submitting}
          style={{ height: 42, minWidth: 100, borderRadius: 6 }}
        >
          Hủy bỏ (Không kê)
        </Button>,
        <Button
          key="submit"
          type="primary"
          danger={isCritical}
          style={
            !isCritical
              ? {
                  height: 42,
                  minWidth: 140,
                  borderRadius: 6,
                  fontWeight: 600,
                  fontSize: 15,
                  backgroundColor: '#d97706',
                  borderColor: '#d97706',
                }
              : { height: 42, minWidth: 140, borderRadius: 6, fontWeight: 600, fontSize: 15 }
          }
          loading={submitting}
          onClick={() => form.submit()}
        >
          Xác nhận kê đơn
        </Button>,
      ]}
    >
      <div style={{ marginTop: 8 }}>
        <Alert
          type={isCritical ? 'error' : 'warning'}
          showIcon
          message={`Cảnh báo: Thuốc thuộc nhóm ${meta?.label || 'Kiểm soát đặc biệt'}`}
          description={
            <div>
              Theo quy định chuyên môn y tế, mọi lượt kê đơn thuốc thuộc nhóm kiểm soát đặc biệt bắt buộc phải có chỉ định lâm sàng rõ ràng và sẽ được tự động ghi nhận vào Sổ theo dõi thuốc của viện.
              {medicine.specialControlNote && (
                <div style={{ marginTop: 6, fontWeight: 600, color: '#991b1b' }}>
                  Lưu ý thuốc: {medicine.specialControlNote}
                </div>
              )}
            </div>
          }
          style={{ marginBottom: 16 }}
        />

        <Card size="small" style={{ marginBottom: 16, backgroundColor: '#f8fafc', borderRadius: 8 }}>
          <Descriptions column={2} size="small">
            <Descriptions.Item label="Tên thuốc">
              <strong>{medicine.medicineName || medicine.name}</strong>
            </Descriptions.Item>
            <Descriptions.Item label="Nhóm kiểm soát">
              <SpecialControlBadge group={medicine.specialControlGroup} note={medicine.specialControlNote} />
            </Descriptions.Item>
            <Descriptions.Item label="Hoạt chất & Hàm lượng">
              {[medicine.activeIngredient, medicine.strength].filter(Boolean).join(' · ') || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Bệnh nhân">
              {patientName} ({patientCode})
            </Descriptions.Item>
          </Descriptions>
        </Card>

        <Form form={form} layout="vertical" onFinish={handleFinish}>
          <Form.Item
            name="reason"
            label={<strong style={{ fontSize: 14 }}>Chỉ định / Lý do lâm sàng bắt buộc *</strong>}
            extra="Nhập chi tiết lý do lâm sàng hoặc phác đồ điều trị (tối đa 500 ký tự)"
            rules={[
              { required: true, message: 'Vui lòng nhập lý do / chỉ định lâm sàng.' },
              { max: 500, message: 'Lý do chỉ định không được vượt quá 500 ký tự.' },
            ]}
          >
            <TextArea
              rows={3}
              placeholder="Nhập chẩn đoán xác định, mức độ đau, phác đồ điều trị hoặc hội chẩn..."
              maxLength={500}
              showCount
              value={reasonValue}
              onChange={(e) => setReasonValue(e.target.value)}
            />
          </Form.Item>

          <div style={{ marginBottom: 8 }}>
            <Text type="secondary" style={{ fontSize: 13, marginRight: 8 }}>
              Gợi ý lý do lâm sàng mẫu:
            </Text>
            <Space wrap size={[6, 6]} style={{ marginTop: 6 }}>
              {PRESET_SPECIAL_CONTROL_PRESCRIBE_REASONS.map((preset, idx) => (
                <Tag
                  key={idx}
                  color="blue"
                  style={{ cursor: 'pointer', padding: '2px 8px', borderRadius: 4 }}
                  onClick={() => handleApplyPreset(preset)}
                >
                  + {preset}
                </Tag>
              ))}
            </Space>
          </div>
        </Form>
      </div>
    </Modal>
  )
}
