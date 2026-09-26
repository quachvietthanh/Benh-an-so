import React, { useState } from 'react'
import { Modal, Radio, Button, Typography, Space, message, Alert } from 'antd'
import { HeartOutlined, SaveOutlined } from '@ant-design/icons'
import contraindicationApi from '../../api/contraindicationApi'

const { Text, Paragraph } = Typography

export default function QuickUpdatePregnancyModal({
  open,
  onClose,
  patient,
  onSuccess,
}) {
  const [pregnancyStatus, setPregnancyStatus] = useState(patient?.pregnancyStatus || 'NOT_PREGNANT')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    if (!patient?.id) {
      message.error('Không tìm thấy thông tin bệnh nhân.')
      return
    }

    setSubmitting(true)
    try {
      await contraindicationApi.updatePregnancyStatus(patient.id, pregnancyStatus)
      message.success('Đã cập nhật tình trạng thai kỳ của bệnh nhân thành công.')
      if (typeof onSuccess === 'function') {
        onSuccess(pregnancyStatus)
      }
      onClose()
    } catch (err) {
      message.error(err.response?.data?.message || 'Không thể cập nhật tình trạng thai kỳ.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open={open}
      title={
        <Space style={{ color: '#be185d' }}>
          <HeartOutlined />
          <span>Bổ sung thông tin thai kỳ của bệnh nhân</span>
        </Space>
      }
      onCancel={onClose}
      destroyOnClose
      footer={[
        <Button key="cancel" onClick={onClose} disabled={submitting}>
          Hủy
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<SaveOutlined />}
          loading={submitting}
          onClick={handleSubmit}
          style={{ background: '#be185d', borderColor: '#be185d' }}
        >
          Lưu & Kiểm tra lại
        </Button>,
      ]}
    >
      <div style={{ marginBottom: 16 }}>
        <Paragraph style={{ marginBottom: 8 }}>
          Bệnh nhân: <strong>{patient?.fullName || patient?.patientName || '—'}</strong>{' '}
          {patient?.patientCode ? `(${patient.patientCode})` : ''}
        </Paragraph>
        <Alert
          type="info"
          showIcon
          message="Yêu cầu an toàn kê đơn"
          description="Để đối chiếu chính xác các chống chỉ định thuốc cho phụ nữ trong độ tuổi sinh sản, vui lòng xác nhận tình trạng thai kỳ hiện tại của bệnh nhân."
          style={{ marginBottom: 16 }}
        />
      </div>

      <div style={{ background: '#fdf2f8', padding: 16, borderRadius: 8, border: '1px solid #fbcfe8' }}>
        <Text strong style={{ display: 'block', marginBottom: 12, color: '#9d174d' }}>
          Tình trạng thai kỳ hiện tại:
        </Text>
        <Radio.Group
          value={pregnancyStatus}
          onChange={(e) => setPregnancyStatus(e.target.value)}
          size="middle"
        >
          <Space direction="vertical">
            <Radio value="NOT_PREGNANT">
              <span style={{ fontWeight: 600 }}>Không mang thai</span>{' '}
              <Text type="secondary">(Không áp dụng quy tắc chống chỉ định thai kỳ)</Text>
            </Radio>
            <Radio value="PREGNANT">
              <span style={{ fontWeight: 600, color: '#be185d' }}>Đang mang thai (Có thai kỳ)</span>{' '}
              <Text type="secondary">(Hệ thống sẽ đối chiếu và cảnh báo các thuốc chống chỉ định cho thai phụ)</Text>
            </Radio>
          </Space>
        </Radio.Group>
      </div>
    </Modal>
  )
}
