import React, { useEffect, useState } from 'react'
import { Alert, Button, Form, Input, Modal, Typography } from 'antd'
import { CloseCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { validateRejectionReason } from '../../utils/medicationProcurementHelpers.js'

const { Text } = Typography
const { TextArea } = Input

export default function RejectProcurementModal({
  open,
  onClose,
  onConfirmReject,
  loading = false,
  planCode = '',
}) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!open) {
      setReason('')
      setError('')
    }
  }, [open])

  const handleSubmit = () => {
    const val = validateRejectionReason(reason)
    if (!val.valid) {
      setError(val.message)
      return
    }
    setError('')
    onConfirmReject(reason.trim())
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={520}
      centered
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 8,
              background: '#fef2f2',
              color: '#dc2626',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 18,
            }}
          >
            <CloseCircleOutlined />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              Từ chối phiếu dự trù {planCode ? `[${planCode}]` : ''}
            </div>
            <div style={{ fontSize: 12.5, color: '#64748b' }}>
              Phiếu sẽ chuyển sang trạng thái 'Bị từ chối' và phản hồi đến Dược sĩ
            </div>
          </div>
        </div>
      }
      footer={[
        <Button key="cancel" onClick={onClose} disabled={loading} style={{ borderRadius: 8 }}>
          Hủy bỏ
        </Button>,
        <Button
          key="reject"
          danger
          type="primary"
          icon={<CloseCircleOutlined />}
          loading={loading}
          onClick={handleSubmit}
          style={{ borderRadius: 8, fontWeight: 600 }}
        >
          Xác nhận từ chối
        </Button>,
      ]}
    >
      <div style={{ margin: '14px 0' }}>
        <Alert
          type="warning"
          showIcon
          icon={<ExclamationCircleOutlined />}
          message="Lưu ý quan trọng"
          description="Dược sĩ sẽ nhìn thấy trực tiếp lý do từ chối này trên màn hình để hiểu nguyên nhân và lập lại phiếu dự trù phù hợp hơn."
          style={{ borderRadius: 10, marginBottom: 16 }}
        />

        <div>
          <label style={{ display: 'block', fontSize: 13, fontWeight: 600, color: '#334155', marginBottom: 6 }}>
            Lý do từ chối phiếu <span style={{ color: '#dc2626' }}>*</span>:
          </label>
          <TextArea
            rows={4}
            placeholder="Nhập lý do từ chối (ít nhất 5 ký tự, ví dụ: Vượt hạn mức ngân sách tháng này, đề nghị giảm số lượng thuốc...)"
            value={reason}
            onChange={(e) => {
              setReason(e.target.value)
              if (error) setError('')
            }}
            maxLength={500}
            showCount
            style={{ borderRadius: 8 }}
          />
          {error && (
            <div style={{ color: '#dc2626', fontSize: 12.5, marginTop: 4 }}>
              {error}
            </div>
          )}
        </div>
      </div>
    </Modal>
  )
}
