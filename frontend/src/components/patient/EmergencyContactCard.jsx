import React from 'react'
import { Card, Tag, Button, Space, Typography, Tooltip, message } from 'antd'
import {
  PhoneOutlined,
  UserOutlined,
  CopyOutlined,
  HistoryOutlined,
  HeartFilled,
  ExclamationCircleOutlined,
  EditOutlined,
} from '@ant-design/icons'

const { Text } = Typography

/**
 * Thẻ hiển thị thông tin Người liên hệ khẩn cấp (NCL-02-CN-007)
 * Dùng trên trang Chi tiết bệnh nhân và Đầu bệnh án cho Bác sĩ.
 * 
 * @param {Object} props
 * @param {Object} props.patient - Bệnh nhân hiện tại
 * @param {Function} [props.onOpenEdit] - Callback mở modal chỉnh sửa
 * @param {Function} [props.onOpenHistory] - Callback mở modal xem lịch sử
 * @param {boolean} [props.compact=false] - Chế độ thu gọn cho đầu bệnh án
 */
export default function EmergencyContactCard({
  patient,
  onOpenEdit,
  onOpenHistory,
  compact = false,
}) {
  if (!patient) return null

  const contact = patient.emergencyContact ? String(patient.emergencyContact).trim() : ''
  const relationship = patient.emergencyRelationship ? String(patient.emergencyRelationship).trim() : ''
  const phone = patient.emergencyPhone ? String(patient.emergencyPhone).trim() : ''

  const hasInfo = Boolean(contact || phone)

  const handleCopy = (e) => {
    e.stopPropagation()
    if (phone) {
      navigator.clipboard.writeText(phone)
      message.success(`Đã sao chép số điện thoại: ${phone}`)
    }
  }

  // Chế độ thu gọn hiển thị ở đầu bệnh án của Bác sĩ
  if (compact) {
    if (!hasInfo) {
      return (
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '4px 10px', background: '#fef2f2', borderRadius: 6, border: '1px dashed #fca5a5' }}>
          <ExclamationCircleOutlined style={{ color: '#ef4444', fontSize: 13 }} />
          <Text type="secondary" style={{ fontSize: 12, color: '#991b1b' }}>
            Chưa có người liên hệ khẩn cấp
          </Text>
        </div>
      )
    }

    return (
      <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8, padding: '4px 12px', background: '#fff1f2', borderRadius: 6, border: '1px solid #fecdd3' }}>
        <HeartFilled style={{ color: '#e11d48', fontSize: 14 }} />
        <span style={{ fontSize: 12, fontWeight: 600, color: '#9f1239' }}>
          Khẩn cấp:
        </span>
        <span style={{ fontSize: 13, fontWeight: 700, color: '#0f172a' }}>
          {contact || 'Chưa rõ tên'}
        </span>
        {relationship && (
          <Tag color="magenta" style={{ margin: 0, fontWeight: 600, fontSize: 11 }}>
            {relationship}
          </Tag>
        )}
        {phone && (
          <Space size={4} style={{ marginLeft: 4 }}>
            <a
              href={`tel:${phone}`}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
                color: '#2563eb',
                fontWeight: 700,
                textDecoration: 'none',
                background: '#eff6ff',
                padding: '2px 8px',
                borderRadius: 4,
                border: '1px solid #bfdbfe',
              }}
            >
              <PhoneOutlined /> {phone}
            </a>
            <Tooltip title="Sao chép số điện thoại">
              <Button
                type="text"
                size="small"
                icon={<CopyOutlined />}
                onClick={handleCopy}
                style={{ padding: '0 4px', color: '#64748b' }}
              />
            </Tooltip>
          </Space>
        )}
      </div>
    )
  }

  // Chế độ đầy đủ hiển thị tại trang chi tiết bệnh nhân
  return (
    <Card
      size="small"
      style={{
        marginBottom: 16,
        borderColor: hasInfo ? '#fda4af' : '#e2e8f0',
        background: hasInfo ? 'linear-gradient(to right, #fff1f2, #ffffff)' : '#f8fafc',
      }}
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Space size={8}>
            <HeartFilled style={{ color: hasInfo ? '#e11d48' : '#94a3b8', fontSize: 16 }} />
            <span style={{ fontWeight: 700, fontSize: 14, color: hasInfo ? '#881337' : '#475569' }}>
              Thông tin người liên hệ khẩn cấp
            </span>
            {relationship && (
              <Tag color="volcano" style={{ fontWeight: 600 }}>
                {relationship}
              </Tag>
            )}
          </Space>
          <Space size={8}>
            {onOpenHistory && (
              <Button
                size="small"
                icon={<HistoryOutlined />}
                onClick={onOpenHistory}
                style={{ fontSize: 12 }}
              >
                Lịch sử thay đổi
              </Button>
            )}
            {onOpenEdit && (
              <Button
                size="small"
                type="primary"
                ghost
                icon={<EditOutlined />}
                onClick={onOpenEdit}
                style={{ fontSize: 12 }}
              >
                Cập nhật
              </Button>
            )}
          </Space>
        </div>
      }
    >
      {hasInfo ? (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <Space size={16} align="center">
            <div>
              <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                Họ và tên người liên hệ
              </Text>
              <Text strong style={{ fontSize: 15, color: '#0f172a' }}>
                <UserOutlined style={{ marginRight: 6, color: '#64748b' }} />
                {contact || '---'}
              </Text>
            </div>

            <div style={{ borderLeft: '1px solid #e2e8f0', paddingLeft: 16 }}>
              <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                Quan hệ nhân thân
              </Text>
              <Tag color="blue" style={{ fontSize: 13, padding: '2px 8px', marginTop: 2 }}>
                {relationship || 'Người thân'}
              </Tag>
            </div>

            <div style={{ borderLeft: '1px solid #e2e8f0', paddingLeft: 16 }}>
              <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                Số điện thoại liên hệ khẩn cấp
              </Text>
              <Space size={6} style={{ marginTop: 2 }}>
                <a
                  href={`tel:${phone}`}
                  style={{
                    fontSize: 15,
                    fontWeight: 700,
                    color: '#0284c7',
                    textDecoration: 'none',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                  }}
                >
                  <PhoneOutlined /> {phone || '---'}
                </a>
                {phone && (
                  <Tooltip title="Sao chép số">
                    <Button
                      size="small"
                      type="text"
                      icon={<CopyOutlined />}
                      onClick={handleCopy}
                    />
                  </Tooltip>
                )}
              </Space>
            </div>
          </Space>

          <div>
            <Text type="secondary" style={{ fontSize: 11, fontStyle: 'italic' }}>
              Quy tắc Cohesive Triplet: Đầy đủ 3 trường Họ tên, Quan hệ và SĐT
            </Text>
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Space size={8}>
            <ExclamationCircleOutlined style={{ color: '#f59e0b', fontSize: 16 }} />
            <Text type="secondary">
              Bệnh nhân chưa có thông tin người liên hệ khẩn cấp. Lễ tân có thể bổ sung khi tiếp đón.
            </Text>
          </Space>
          {onOpenEdit && (
            <Button size="small" type="dashed" onClick={onOpenEdit}>
              + Thêm người liên hệ
            </Button>
          )}
        </div>
      )}
    </Card>
  )
}
