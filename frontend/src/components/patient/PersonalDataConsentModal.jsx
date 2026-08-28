import React from 'react'
import { Modal, Typography, Divider, Button, Space, Tag } from 'antd'
import {
  SafetyCertificateOutlined,
  PrinterOutlined,
  CheckCircleOutlined,
  FileProtectOutlined,
} from '@ant-design/icons'
import { FULL_CONSENT_DOCUMENT } from '../../constants/patientConsentConstants'

const { Title, Paragraph, Text } = Typography

export default function PersonalDataConsentModal({
  open,
  onClose,
  patientName = '',
  agreedAt = null,
  version = 'v1.0',
}) {
  const handlePrint = () => {
    window.print()
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={780}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <SafetyCertificateOutlined style={{ fontSize: 24, color: '#16a34a' }} />
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              Quy chế & Phiếu đồng ý xử lý dữ liệu cá nhân
            </div>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Nghị định 13/2023/NĐ-CP • Phiên bản hiệu lực: <Tag color="blue">{version}</Tag>
            </Text>
          </div>
        </div>
      }
      footer={[
        <Button key="print" icon={<PrinterOutlined />} onClick={handlePrint}>
          In phiếu
        </Button>,
        <Button key="close" type="primary" onClick={onClose}>
          Đã hiểu & Đóng
        </Button>,
      ]}
      style={{ top: 30 }}
      styles={{ body: { maxHeight: '70vh', overflowY: 'auto', paddingRight: 8 } }}
    >
      <div style={{ textAlign: 'center', marginBottom: 20 }}>
        <Title level={4} style={{ textTransform: 'uppercase', color: '#1e3a8a', marginBottom: 4 }}>
          {FULL_CONSENT_DOCUMENT.title}
        </Title>
        <Text italic style={{ color: '#475569', fontSize: 13 }}>
          {FULL_CONSENT_DOCUMENT.subtitle}
        </Text>
      </div>

      {patientName && (
        <div
          style={{
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: 8,
            padding: '12px 16px',
            marginBottom: 20,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 10,
          }}
        >
          <div>
            <Text type="secondary">Chủ thể dữ liệu (Người bệnh): </Text>
            <Text strong style={{ fontSize: 15, color: '#0f172a' }}>
              {patientName}
            </Text>
          </div>
          {agreedAt && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <CheckCircleOutlined style={{ color: '#16a34a' }} />
              <Text type="secondary">Thời điểm ghi nhận đồng ý: </Text>
              <Text strong>{new Date(agreedAt).toLocaleString('vi-VN')}</Text>
            </div>
          )}
        </div>
      )}

      {FULL_CONSENT_DOCUMENT.sections.map((section, idx) => (
        <div key={idx} style={{ marginBottom: 16 }}>
          <Text strong style={{ fontSize: 14, color: '#1e293b', display: 'block', marginBottom: 4 }}>
            {section.title}
          </Text>
          <Paragraph
            style={{
              color: '#334155',
              fontSize: 13,
              lineHeight: 1.6,
              whiteSpace: 'pre-line',
              margin: 0,
            }}
          >
            {section.content}
          </Paragraph>
          {idx < FULL_CONSENT_DOCUMENT.sections.length - 1 && (
            <Divider style={{ margin: '14px 0' }} />
          )}
        </div>
      ))}

      <div
        style={{
          marginTop: 24,
          padding: 14,
          background: '#f0fdf4',
          border: '1px solid #bbf7d0',
          borderRadius: 8,
          display: 'flex',
          gap: 12,
          alignItems: 'flex-start',
        }}
      >
        <FileProtectOutlined style={{ fontSize: 20, color: '#16a34a', marginTop: 2 }} />
        <div style={{ fontSize: 12, color: '#166534', lineHeight: 1.5 }}>
          <strong>Cam kết phòng khám:</strong> Thông tin của người bệnh được bảo vệ tuyệt mật, chỉ sử dụng cho mục đích chăm sóc y tế và tuân thủ các quy định pháp luật hiện hành.
        </div>
      </div>
    </Modal>
  )
}
