import React, { useMemo, useState } from 'react'
import {
  Modal,
  Typography,
  Checkbox,
  Input,
  Button,
  Space,
  Tag,
  Alert,
  message,
  Divider,
} from 'antd'
import {
  SlidersOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SafetyCertificateOutlined,
  InfoCircleOutlined,
  FileDoneOutlined,
} from '@ant-design/icons'

import patientConsentApi from '../../api/patientConsentApi'
import {
  CONSENT_SCOPES,
  calculateConsentImpact,
  validateScopeAdjustment,
} from '../../utils/patientConsentHelpers'
import './patientConsent.css'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input

export default function NarrowConsentScopeModal({
  open,
  onClose,
  patient,
  currentScopes = ['TREATMENT', 'COMMUNICATION', 'RESEARCH'],
  onSuccess,
}) {
  // Danh sách phạm vi được chọn (Mặc định TREATMENT luôn có)
  const [selectedScopes, setSelectedScopes] = useState(() => {
    return Array.isArray(currentScopes) && currentScopes.length > 0
      ? currentScopes
      : ['TREATMENT', 'COMMUNICATION', 'RESEARCH']
  })

  // Lý do thu hẹp phạm vi
  const [reason, setReason] = useState('')

  // Checkbox xác nhận trước khi lưu
  const [confirmedPreview, setConfirmedPreview] = useState(false)

  // Loading
  const [saving, setSaving] = useState(false)

  // Reset form khi mở modal
  React.useEffect(() => {
    if (open) {
      setSelectedScopes(
        Array.isArray(currentScopes) && currentScopes.length > 0
          ? currentScopes
          : ['TREATMENT', 'COMMUNICATION', 'RESEARCH']
      )
      setReason('')
      setConfirmedPreview(false)
    }
  }, [open, currentScopes])

  // Xử lý thay đổi checkbox
  const handleScopeChange = (scopeKey, checked) => {
    if (scopeKey === 'TREATMENT') return // Khám chữa bệnh là bắt buộc
    if (checked) {
      setSelectedScopes((prev) => [...prev, scopeKey])
    } else {
      setSelectedScopes((prev) => prev.filter((k) => k !== scopeKey))
    }
  }

  // Tính toán tác động & xem trước
  const impact = useMemo(() => {
    return calculateConsentImpact(currentScopes, selectedScopes, false)
  }, [currentScopes, selectedScopes])

  // Kiểm tra tính hợp lệ
  const validation = useMemo(() => {
    return validateScopeAdjustment(selectedScopes, false)
  }, [selectedScopes])

  // Xử lý lưu phiên bản mới
  const handleSave = async () => {
    if (!validation.valid) {
      message.error(validation.message)
      return
    }

    if (!confirmedPreview) {
      message.warning('Vui lòng xác nhận đã đọc lại bản xem trước cùng người bệnh trước khi chốt lưu.')
      return
    }

    setSaving(true)
    try {
      await patientConsentApi.updateConsent(patient.id, {
        consentAgreed: true,
        consentWithdrawn: false,
        consentWithdrawnReason: reason.trim() || 'Người bệnh yêu cầu thu hẹp phạm vi đồng ý',
        scopes: selectedScopes,
      })

      message.success('Đã cập nhật phạm vi đồng ý và lưu thành một phiên bản mới thành công!')
      if (onSuccess) onSuccess()
      onClose()
    } catch (err) {
      const errMsg =
        err?.response?.data?.message ||
        err?.message ||
        'Không thể cập nhật phiếu đồng ý. Vui lòng thử lại.'
      message.error(errMsg)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={720}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 8,
              background: '#eff6ff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#2563eb',
              fontSize: 18,
            }}
          >
            <SlidersOutlined />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              Thu hẹp phạm vi phiếu đồng ý xử lý dữ liệu
            </div>
            <Text type="secondary" style={{ fontSize: 12.5 }}>
              Người bệnh: <strong>{patient?.fullName}</strong> ({patient?.patientCode})
            </Text>
          </div>
        </div>
      }
      footer={[
        <Button key="cancel" onClick={onClose} disabled={saving}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<FileDoneOutlined />}
          onClick={handleSave}
          loading={saving}
          disabled={!validation.valid || !confirmedPreview}
          style={{ background: '#2563eb', borderRadius: 8, fontWeight: 600 }}
        >
          Xác nhận & Lưu phiên bản mới
        </Button>,
      ]}
      style={{ top: 24 }}
    >
      <Alert
        type="info"
        showIcon
        icon={<InfoCircleOutlined />}
        style={{ marginBottom: 16, borderRadius: 8 }}
        message="Nguyên tắc thu hẹp phạm vi đồng ý"
        description="Lễ tân tích chọn các mục người bệnh tiếp tục đồng ý và bỏ chọn các mục người bệnh không còn đồng ý. Hệ thống sẽ lưu thành MỘT PHIÊN BẢN MỚI kèm thời điểm hiện tại và không sửa đè lên phiên bản cũ."
      />

      <div style={{ marginBottom: 16 }}>
        <Text strong style={{ fontSize: 14, color: '#1e293b' }}>
          1. Lựa chọn phạm vi đồng ý (Checklist):
        </Text>
        <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 10 }}>
          {/* Mục 1: TREATMENT (Bắt buộc) */}
          <div
            className={`consent-scope-card-item ${
              selectedScopes.includes('TREATMENT') ? 'active' : ''
            }`}
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
              <Checkbox checked={true} disabled={true} style={{ marginTop: 2 }} />
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <Text strong style={{ fontSize: 14, color: '#0f172a' }}>
                    {CONSENT_SCOPES.TREATMENT.name}
                  </Text>
                  <Tag color="green" style={{ borderRadius: 4, fontWeight: 600 }}>
                    Bắt buộc (Cốt lõi)
                  </Tag>
                </div>
                <Paragraph
                  type="secondary"
                  style={{ fontSize: 12.5, margin: '4px 0 0', lineHeight: 1.5 }}
                >
                  {CONSENT_SCOPES.TREATMENT.description}
                </Paragraph>
              </div>
            </div>
          </div>

          {/* Mục 2: COMMUNICATION */}
          <div
            className={`consent-scope-card-item ${
              selectedScopes.includes('COMMUNICATION') ? 'active' : ''
            }`}
            style={{ cursor: 'pointer' }}
            onClick={() =>
              handleScopeChange('COMMUNICATION', !selectedScopes.includes('COMMUNICATION'))
            }
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
              <Checkbox
                checked={selectedScopes.includes('COMMUNICATION')}
                onChange={(e) => handleScopeChange('COMMUNICATION', e.target.checked)}
                style={{ marginTop: 2 }}
                onClick={(e) => e.stopPropagation()}
              />
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <Text strong style={{ fontSize: 14, color: '#0f172a' }}>
                    {CONSENT_SCOPES.COMMUNICATION.name}
                  </Text>
                  <Tag color="blue" style={{ borderRadius: 4 }}>
                    Có thể thu hẹp
                  </Tag>
                </div>
                <Paragraph
                  type="secondary"
                  style={{ fontSize: 12.5, margin: '4px 0 0', lineHeight: 1.5 }}
                >
                  {CONSENT_SCOPES.COMMUNICATION.description}
                </Paragraph>
              </div>
            </div>
          </div>

          {/* Mục 3: RESEARCH */}
          <div
            className={`consent-scope-card-item ${
              selectedScopes.includes('RESEARCH') ? 'active' : ''
            }`}
            style={{ cursor: 'pointer' }}
            onClick={() => handleScopeChange('RESEARCH', !selectedScopes.includes('RESEARCH'))}
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
              <Checkbox
                checked={selectedScopes.includes('RESEARCH')}
                onChange={(e) => handleScopeChange('RESEARCH', e.target.checked)}
                style={{ marginTop: 2 }}
                onClick={(e) => e.stopPropagation()}
              />
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <Text strong style={{ fontSize: 14, color: '#0f172a' }}>
                    {CONSENT_SCOPES.RESEARCH.name}
                  </Text>
                  <Tag color="purple" style={{ borderRadius: 4 }}>
                    Có thể thu hẹp
                  </Tag>
                </div>
                <Paragraph
                  type="secondary"
                  style={{ fontSize: 12.5, margin: '4px 0 0', lineHeight: 1.5 }}
                >
                  {CONSENT_SCOPES.RESEARCH.description}
                </Paragraph>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Lý do thu hẹp */}
      <div style={{ marginBottom: 16 }}>
        <Text strong style={{ fontSize: 13.5, color: '#1e293b' }}>
          2. Lý do người bệnh yêu cầu thu hẹp phạm vi:
        </Text>
        <TextArea
          rows={2}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Ví dụ: Người bệnh yêu cầu không nhận tin nhắn nhắc lịch tái khám và khảo sát chất lượng..."
          style={{ marginTop: 6, borderRadius: 8 }}
        />
      </div>

      <Divider style={{ margin: '14px 0' }} />

      {/* BẢN XEM TRƯỚC ĐỐI SOÁT TRƯỚC KHI CHỐT LƯU (BẮT BUỘC THEO ĐẶC TẢ) */}
      <div className="consent-preview-box">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
          <SafetyCertificateOutlined style={{ color: '#2563eb', fontSize: 18 }} />
          <strong style={{ fontSize: 14, color: '#0f172a' }}>
            BẢN XEM TRƯỚC ĐỐI SOÁT (Trước khi chốt lưu phiên bản mới):
          </strong>
        </div>

        {/* Khung 1: Phạm vi mới */}
        <div className="consent-preview-section">
          <div className="consent-preview-title" style={{ color: '#15803d' }}>
            <CheckCircleOutlined /> Phạm vi mới sẽ là ({impact.newScopesList.length} mục):
          </div>
          <div className="consent-preview-new-scopes">
            {impact.newScopesList.map((scope, idx) => (
              <div key={scope.key} style={{ display: 'flex', alignItems: 'center', gap: 6, margin: '3px 0' }}>
                <span style={{ fontWeight: 600 }}>• {scope.name}</span>
                {scope.required && (
                  <Tag color="green" style={{ fontSize: 11, padding: '0 6px', margin: 0 }}>
                    Bắt buộc
                  </Tag>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Khung 2: Các hoạt động sẽ dừng lại */}
        <div className="consent-preview-section">
          <div className="consent-preview-title" style={{ color: '#b45309' }}>
            <CloseCircleOutlined /> Các hoạt động sau sẽ DỪNG LẠI vì nằm ngoài phạm vi mới:
          </div>
          <div className="consent-preview-stopped-activities">
            {impact.stoppedActivities.map((act, idx) => (
              <div key={idx} style={{ margin: '3px 0', fontSize: 13 }}>
                • {act}
              </div>
            ))}
          </div>
        </div>

        {/* Checkbox xác nhận tránh thu hẹp nhầm */}
        <div style={{ marginTop: 14 }}>
          <Checkbox
            checked={confirmedPreview}
            onChange={(e) => setConfirmedPreview(e.target.checked)}
          >
            <Text strong style={{ color: '#1e3a8a', fontSize: 13 }}>
              Tôi xác nhận đã đọc lại bản xem trước trên và người bệnh đã đồng ý chốt phạm vi này.
            </Text>
          </Checkbox>
        </div>
      </div>
    </Modal>
  )
}
