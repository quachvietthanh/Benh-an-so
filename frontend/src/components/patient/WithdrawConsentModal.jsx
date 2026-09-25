import React, { useState } from 'react'
import {
  Modal,
  Typography,
  Input,
  Button,
  Alert,
  Checkbox,
  message,
  Divider,
  Tag,
} from 'antd'
import {
  WarningOutlined,
  CloseCircleOutlined,
  StopOutlined,
  SafetyCertificateOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'

import patientConsentApi from '../../api/patientConsentApi'
import { calculateConsentImpact } from '../../utils/patientConsentHelpers'
import './patientConsent.css'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input

export default function WithdrawConsentModal({
  open,
  onClose,
  patient,
  currentScopes = ['TREATMENT', 'COMMUNICATION', 'RESEARCH'],
  onSuccess,
}) {
  const [reason, setReason] = useState('')
  const [confirmedImpact, setConfirmedImpact] = useState(false)
  const [confirmedPatientConsent, setConfirmedPatientConsent] = useState(false)
  const [saving, setSaving] = useState(false)

  React.useEffect(() => {
    if (open) {
      setReason('')
      setConfirmedImpact(false)
      setConfirmedPatientConsent(false)
    }
  }, [open])

  // Tính toán tác động rút lại toàn bộ
  const impact = calculateConsentImpact(currentScopes, [], true)

  const canSubmit = reason.trim().length >= 5 && confirmedImpact && confirmedPatientConsent

  const handleWithdraw = async () => {
    if (reason.trim().length < 5) {
      message.warning('Vui lòng nhập lý do người bệnh rút lại sự đồng ý (tối thiểu 5 ký tự).')
      return
    }

    if (!confirmedImpact || !confirmedPatientConsent) {
      message.warning('Vui lòng tích xác nhận đầy đủ các bước kiểm tra trước khi thực hiện.')
      return
    }

    setSaving(true)
    try {
      await patientConsentApi.updateConsent(patient.id, {
        consentAgreed: true,
        consentWithdrawn: true,
        consentWithdrawnReason: reason.trim(),
        scopes: [],
      })

      message.success('Đã ghi nhận rút lại toàn bộ sự đồng ý và lưu thành phiên bản mới!')
      if (onSuccess) onSuccess()
      onClose()
    } catch (err) {
      const errMsg =
        err?.response?.data?.message ||
        err?.message ||
        'Không thể rút lại sự đồng ý. Vui lòng thử lại.'
      message.error(errMsg)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={700}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 8,
              background: '#fef2f2',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#dc2626',
              fontSize: 20,
            }}
          >
            <WarningOutlined />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#991b1b' }}>
              Rút lại toàn bộ phiếu đồng ý xử lý dữ liệu
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
          danger
          icon={<StopOutlined />}
          onClick={handleWithdraw}
          loading={saving}
          disabled={!canSubmit}
          style={{ borderRadius: 8, fontWeight: 600 }}
        >
          Xác nhận Rút lại toàn bộ & Lưu phiên bản mới
        </Button>,
      ]}
      style={{ top: 24 }}
    >
      <Alert
        type="error"
        showIcon
        icon={<WarningOutlined style={{ color: '#dc2626' }} />}
        style={{ marginBottom: 16, borderRadius: 8 }}
        message="Cảnh báo: Đây là thao tác có phạm vi tác động rộng nhất"
        description="Khi người bệnh rút lại toàn bộ sự đồng ý, phòng khám sẽ lập tức chấm dứt mọi hoạt động sử dụng dữ liệu cá nhân cho các mục đích liên lạc, chăm sóc khách hàng, khảo sát và nghiên cứu khoa học. Hồ sơ bệnh án đã có trước thời điểm này vẫn được bảo mật và lưu trữ bắt buộc tối thiểu 10 năm theo Luật Khám bệnh, chữa bệnh."
      />

      {/* Nhập lý do bắt buộc */}
      <div style={{ marginBottom: 16 }}>
        <Text strong style={{ fontSize: 13.5, color: '#1e293b' }}>
          1. Lý do người bệnh yêu cầu rút lại toàn bộ sự đồng ý <span style={{ color: '#dc2626' }}>*</span>:
        </Text>
        <TextArea
          rows={3}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Bắt buộc nhập lý do cụ thể (tối thiểu 5 ký tự). Ví dụ: Người bệnh chuyển nơi cư trú, yêu cầu ngừng toàn bộ việc lưu giữ thông tin liên lạc và thông báo y tế..."
          style={{ marginTop: 6, borderRadius: 8 }}
        />
        <div style={{ textAlign: 'right', marginTop: 4 }}>
          <Text type={reason.trim().length >= 5 ? 'secondary' : 'danger'} style={{ fontSize: 12 }}>
            {reason.trim().length}/5 ký tự tối thiểu
          </Text>
        </div>
      </div>

      <Divider style={{ margin: '14px 0' }} />

      {/* BẢN XEM TRƯỚC ĐỐI SOÁT TRƯỚC KHI CHỐT LƯU (BẮT BUỘC THEO ĐẶC TẢ) */}
      <div className="consent-preview-box">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
          <SafetyCertificateOutlined style={{ color: '#dc2626', fontSize: 18 }} />
          <strong style={{ fontSize: 14, color: '#991b1b' }}>
            BẢN XEM TRƯỚC HẬU QUẢ VÀ TÁC ĐỘNG:
          </strong>
        </div>

        {/* Khung 1: Trạng thái mới */}
        <div className="consent-preview-section">
          <div className="consent-preview-title" style={{ color: '#991b1b' }}>
            <CloseCircleOutlined /> Trạng thái mới sau khi lưu:
          </div>
          <div className="consent-preview-withdrawn-all">
            <strong>ĐÃ RÚT LẠI TOÀN BỘ SỰ ĐỒNG Ý</strong>
            <div style={{ fontSize: 12.5, marginTop: 2 }}>
              Phạm vi xử lý dữ liệu mới: <em>Trống (0 mục)</em>
            </div>
          </div>
        </div>

        {/* Khung 2: Các hoạt động sẽ dừng lại */}
        <div className="consent-preview-section">
          <div className="consent-preview-title" style={{ color: '#b45309' }}>
            <StopOutlined /> Các hoạt động sau sẽ DỪNG LẠI NGAY LẬP TỨC:
          </div>
          <div className="consent-preview-stopped-activities">
            {impact.stoppedActivities.map((act, idx) => (
              <div key={idx} style={{ margin: '3px 0', fontSize: 13 }}>
                • {act}
              </div>
            ))}
          </div>
        </div>

        <div style={{ fontSize: 12.5, color: '#475569', fontStyle: 'italic', marginTop: 8 }}>
          {impact.legalRetentionNotice}
        </div>
      </div>

      {/* Các bước xác nhận kiểm soát 2 tầng */}
      <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
        <Checkbox
          checked={confirmedImpact}
          onChange={(e) => setConfirmedImpact(e.target.checked)}
        >
          <Text strong style={{ color: '#0f172a', fontSize: 13 }}>
            Lễ tân đã giải thích rõ cho người bệnh về các hoạt động sẽ dừng lại khi rút toàn bộ đồng ý.
          </Text>
        </Checkbox>

        <Checkbox
          checked={confirmedPatientConsent}
          onChange={(e) => setConfirmedPatientConsent(e.target.checked)}
        >
          <Text strong style={{ color: '#991b1b', fontSize: 13 }}>
            Người bệnh xác nhận dứt khoát yêu cầu rút lại toàn bộ và không có thắc mắc nào khác.
          </Text>
        </Checkbox>
      </div>
    </Modal>
  )
}
