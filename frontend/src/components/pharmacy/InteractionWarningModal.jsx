import React, { useState, useEffect } from 'react'
import { Modal, Alert, List, Input, Form, Tag, Button, Typography, Space } from 'antd'
import { WarningOutlined, ExclamationCircleOutlined, CheckOutlined, ArrowLeftOutlined } from '@ant-design/icons'
import { validateOverrideReason } from '../../utils/drugInteractionValidation'

const { Text, Paragraph } = Typography

function renderSeverityTag(severity) {
  if (!severity) return null
  const sev = String(severity).toUpperCase()
  switch (sev) {
    case 'CONTRAINDICATED':
      return <Tag color="magenta">Chống chỉ định (CONTRAINDICATED)</Tag>
    case 'SEVERE':
      return <Tag color="red">Nghiêm trọng (SEVERE)</Tag>
    case 'MODERATE':
      return <Tag color="orange">Trung bình (MODERATE)</Tag>
    case 'MILD':
      return <Tag color="blue">Nhẹ (MILD)</Tag>
    default:
      return <Tag color="orange">{severity}</Tag>
  }
}

function InteractionWarningModal({
  open,
  warnings = [],
  onCancel,
  onConfirmOverride,
}) {
  const [overrideReason, setOverrideReason] = useState('')
  const [errorMsg, setErrorMsg] = useState('')
  const [showReasonInput, setShowReasonInput] = useState(false)

  useEffect(() => {
    if (open) {
      setOverrideReason('')
      setErrorMsg('')
      setShowReasonInput(false)
    }
  }, [open])

  const handleContinueClick = () => {
    if (!showReasonInput) {
      setShowReasonInput(true)
      return
    }

    const { valid, error, trimmedReason } = validateOverrideReason(overrideReason)
    if (!valid) {
      setErrorMsg(error)
      return
    }

    const overrides = warnings.map((w) => ({
      ruleId: w.ruleId,
      overrideReason: trimmedReason,
    }))

    onConfirmOverride(overrides)
  }

  return (
    <Modal
      open={open}
      title={
        <Space style={{ color: '#DC2626' }}>
          <ExclamationCircleOutlined />
          <span style={{ fontWeight: 700, fontSize: 16 }}>CẢNH BÁO TƯƠNG TÁC THUỐC TRONG ĐƠN</span>
        </Space>
      }
      onCancel={onCancel}
      width={700}
      destroyOnClose
      footer={[
        <Button key="back" icon={<ArrowLeftOutlined />} onClick={onCancel}>
          Quay lại điều chỉnh đơn
        </Button>,
        <Button
          key="submit"
          type="primary"
          danger
          icon={<CheckOutlined />}
          onClick={handleContinueClick}
        >
          {showReasonInput ? 'Xác nhận tiếp tục kê đơn' : 'Tiếp tục kê đơn'}
        </Button>,
      ]}
    >
      <Alert
        type="error"
        showIcon
        icon={<WarningOutlined />}
        style={{ marginBottom: 16 }}
        message="Phát hiện tương tác thuốc bất lợi"
        description="Đơn thuốc chứa sự kết hợp giữa các thuốc có nguy cơ gây tương tác. Vui lòng xem xét điều chỉnh đơn hoặc chọn Tiếp tục kê đơn và nhập lý do bỏ qua."
      />

      <List
        bordered
        dataSource={warnings}
        style={{ marginBottom: 16, backgroundColor: '#FFF5F5' }}
        renderItem={(w, idx) => (
          <List.Item key={w.ruleId || idx}>
            <div style={{ width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                <Text strong style={{ fontSize: 14, color: '#991B1B' }}>
                  Cặp tương tác #{idx + 1}: {w.drugNameA || w.drugIdA} — {w.drugNameB || w.drugIdB}
                </Text>
                {renderSeverityTag(w.severity)}
              </div>
              <Paragraph style={{ margin: 0, color: '#374151' }}>
                <strong>Nội dung cảnh báo:</strong> {w.description}
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

      {showReasonInput && (
        <Form layout="vertical" style={{ marginTop: 12 }}>
          <Form.Item
            label={<strong style={{ color: '#991B1B' }}>Lý do bỏ qua cảnh báo *</strong>}
            validateStatus={errorMsg ? 'error' : ''}
            help={errorMsg}
            required
          >
            <Input.TextArea
              rows={3}
              placeholder="Bắt buộc nhập lý do bỏ qua cảnh báo chuyên môn..."
              value={overrideReason}
              onChange={(e) => {
                setOverrideReason(e.target.value)
                if (e.target.value.trim()) setErrorMsg('')
              }}
            />
          </Form.Item>
        </Form>
      )}
    </Modal>
  )
}

export default InteractionWarningModal

