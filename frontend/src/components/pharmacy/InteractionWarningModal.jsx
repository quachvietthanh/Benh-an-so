import React, { useState, useEffect } from 'react'
import { Modal, Alert, List, Input, Form, Tag, Button, Typography, Space } from 'antd'
import { WarningOutlined, ExclamationCircleOutlined, CheckOutlined } from '@ant-design/icons'

const { Text, Paragraph } = Typography

const SEVERITY_MAP = {
  CONTRAINDICATED: { label: 'Chống chỉ định', color: 'red' },
  SEVERE: { label: 'Nghiêm trọng', color: 'volcano' },
  MODERATE: { label: 'Trung bình', color: 'orange' },
  MILD: { label: 'Nhẹ', color: 'gold' },
}

const renderSeverityTag = (severity) => {
  const item = SEVERITY_MAP[severity]
  if (item) return <Tag color={item.color}>{item.label}</Tag>
  return <Tag color="red">{severity || 'Cảnh báo'}</Tag>
}

function InteractionWarningModal({
  open,
  warnings = [],
  onCancel,
  onConfirmOverride,
  currentUser,
}) {
  const [overrideReason, setOverrideReason] = useState('')
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    if (open) {
      setOverrideReason('')
      setErrorMsg('')
    }
  }, [open])

  const handleConfirm = () => {
    const trimmed = overrideReason.trim()
    if (!trimmed) {
      setErrorMsg('Bắt buộc nhập lý do chuyên môn để xác nhận bỏ qua cảnh báo tương tác thuốc.')
      return
    }

    const now = new Date().toISOString()
    const overrides = warnings.map((w) => ({
      ruleId: w.ruleId,
      medicineAId: w.drugIdA,
      medicineBId: w.drugIdB,
      severity: w.severity,
      description: w.description,
      overrideReason: trimmed,
      ignoredBy: currentUser?.fullName || currentUser?.username || 'Bác sĩ phụ trách',
      ignoredAt: now,
    }))

    onConfirmOverride(overrides, trimmed)
  }

  return (
    <Modal
      open={open}
      title={
        <Space style={{ color: '#DC2626' }}>
          <ExclamationCircleOutlined />
          <span style={{ fontWeight: 700, fontSize: 16 }}>PHÁT HIỆN TƯƠNG TÁC THUỐC TRONG ĐƠN</span>
        </Space>
      }
      onCancel={onCancel}
      width={680}
      footer={[
        <Button key="back" onClick={onCancel}>
          Quay lại điều chỉnh đơn
        </Button>,
        <Button
          key="submit"
          type="primary"
          danger
          icon={<CheckOutlined />}
          onClick={handleConfirm}
        >
          Xác nhận tiếp tục kê đơn
        </Button>,
      ]}
    >
      <Alert
        type="error"
        showIcon
        icon={<WarningOutlined />}
        style={{ marginBottom: 16 }}
        message="CẢNH BÁO Y KHOA QUAN TRỌNG"
        description="Đơn thuốc có sự kết hợp của các thuốc có nguy cơ gây ra tương tác bất lợi. Vui lòng kiểm tra kỹ trước khi chỉ định."
      />

      <List
        bordered
        dataSource={warnings}
        style={{ marginBottom: 16, backgroundColor: '#FFF5F5' }}
        renderItem={(w, idx) => (
          <List.Item key={w.ruleId || idx}>
            <div style={{ width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                <Text strong style={{ fontSize: 14, color: '#991B1B' }}>
                  Cặp tương tác #{idx + 1}: {w.drugNameA || w.drugIdA} + {w.drugNameB || w.drugIdB}
                </Text>
                {renderSeverityTag(w.severity)}
              </div>
              <Paragraph style={{ margin: 0, color: '#374151' }}>
                <strong>Mô tả:</strong> {w.description}
              </Paragraph>
              {w.clinicalRecommendation && (
                <Paragraph style={{ marginTop: 4, marginBottom: 0, color: '#1E40AF', fontSize: 13 }}>
                  <strong>Khuyến nghị:</strong> {w.clinicalRecommendation}
                </Paragraph>
              )}
            </div>
          </List.Item>
        )}
      />

      <Form layout="vertical">
        <Form.Item
          label={<strong style={{ color: '#991B1B' }}>Lý do chuyên môn bỏ qua cảnh báo (Bắt buộc) *</strong>}
          validateStatus={errorMsg ? 'error' : ''}
          help={errorMsg}
          required
        >
          <Input.TextArea
            rows={3}
            placeholder="Ví dụ: Bệnh nhân đã được theo dõi sát chỉ số chức năng gan/thận; Giảm liều dùng Paracetamol xuống 1g/ngày..."
            value={overrideReason}
            onChange={(e) => {
              setOverrideReason(e.target.value)
              if (e.target.value.trim()) setErrorMsg('')
            }}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default InteractionWarningModal
