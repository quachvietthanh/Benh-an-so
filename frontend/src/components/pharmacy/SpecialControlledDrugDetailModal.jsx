import React from 'react'
import {
  Button,
  Card,
  Descriptions,
  Divider,
  Modal,
  Space,
  Tag,
  Typography,
} from 'antd'
import {
  AuditOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CloseOutlined,
  FileTextOutlined,
  MedicineBoxOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import SpecialControlBadge from './SpecialControlBadge.jsx'
import { SPECIAL_CONTROL_ACTION_TYPES } from '../../utils/specialControlHelpers.js'

const { Text, Paragraph } = Typography

export default function SpecialControlledDrugDetailModal({
  open,
  onClose,
  entry,
}) {
  if (!entry) return null

  const isPrescribed = entry.actionType === 'PRESCRIBED'
  const actionMeta = SPECIAL_CONTROL_ACTION_TYPES[entry.actionType] || {
    label: entry.actionType || 'Ghi nhận',
    color: 'default',
  }

  return (
    <Modal
      open={open}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#1e293b' }}>
          <AuditOutlined style={{ fontSize: 22, color: '#0284c7' }} />
          <span style={{ fontSize: 16, fontWeight: 700 }}>
            Chi tiết nhật ký thuốc kiểm soát đặc biệt
          </span>
        </div>
      }
      onCancel={onClose}
      footer={[
        <Button
          key="close"
          type="primary"
          onClick={onClose}
          style={{
            height: 42,
            minWidth: 120,
            fontSize: 14,
            fontWeight: 600,
          }}
        >
          Đóng
        </Button>,
      ]}
      width={720}
      destroyOnClose
    >
      <div style={{ padding: '8px 0' }}>
        {/* Thẻ tóm tắt nghiệp vụ */}
        <Card
          size="small"
          style={{
            marginBottom: 16,
            backgroundColor: isPrescribed ? '#eff6ff' : '#f0fdf4',
            borderColor: isPrescribed ? '#bfdbfe' : '#bbf7d0',
            borderRadius: 8,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <div>
              <Text type="secondary" style={{ fontSize: 12 }}>Mã định danh nhật ký</Text>
              <div>
                <Text strong style={{ fontSize: 16, color: '#0f172a' }}>
                  {entry.registerCode || entry.id}
                </Text>
              </div>
            </div>
            <Tag
              color={actionMeta.color}
              style={{ fontSize: 13, padding: '4px 12px', borderRadius: 6, fontWeight: 600 }}
            >
              {actionMeta.label}
            </Tag>
          </div>
        </Card>

        {/* Thông tin nghiệp vụ & đối tượng */}
        <Descriptions
          bordered
          size="small"
          column={{ xs: 1, sm: 2 }}
          style={{ marginBottom: 16 }}
        >
          <Descriptions.Item label="Thời gian ghi nhận">
            <Space size={4}>
              <CalendarOutlined style={{ color: '#64748b' }} />
              <span>
                {entry.confirmedAt || entry.createdAt
                  ? dayjs(entry.confirmedAt || entry.createdAt).format('DD/MM/YYYY HH:mm:ss')
                  : '—'}
              </span>
            </Space>
          </Descriptions.Item>

          <Descriptions.Item label="Người thực hiện">
            <Space size={4}>
              <UserOutlined style={{ color: '#64748b' }} />
              <strong>{entry.confirmedBy || 'Cán bộ y tế'}</strong>
            </Space>
          </Descriptions.Item>

          <Descriptions.Item label="Họ tên bệnh nhân">
            <Text strong style={{ color: '#1e40af' }}>
              {entry.patientName || '—'}
            </Text>
          </Descriptions.Item>

          <Descriptions.Item label="Mã bệnh nhân">
            <Text type="secondary">{entry.patientCode || '—'}</Text>
          </Descriptions.Item>

          {entry.patientIdCard && (
            <Descriptions.Item label="Số CMND / CCCD người nhận" span={2}>
              <Text code strong style={{ fontSize: 14 }}>
                {entry.patientIdCard}
              </Text>
            </Descriptions.Item>
          )}

          {entry.prescriptionCode && (
            <Descriptions.Item label="Mã đơn thuốc liên quan" span={2}>
              <Text strong style={{ color: '#0369a1' }}>
                {entry.prescriptionCode}
              </Text>
            </Descriptions.Item>
          )}
        </Descriptions>

        {/* Thông tin chi tiết thuốc */}
        <Descriptions
          title={<span style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>Thông tin thuốc & Lô xuất</span>}
          bordered
          size="small"
          column={{ xs: 1, sm: 2 }}
          style={{ marginBottom: 16 }}
        >
          <Descriptions.Item label="Tên thuốc" span={2}>
            <Space direction="vertical" size={2}>
              <Text strong style={{ fontSize: 15, color: '#0f172a' }}>
                {entry.medicineName}
              </Text>
              <SpecialControlBadge
                isSpecialControl={true}
                group={entry.specialControlGroup}
              />
            </Space>
          </Descriptions.Item>

          <Descriptions.Item label="Số lượng">
            <Text strong style={{ fontSize: 15, color: '#047857' }}>
              {entry.quantity} {entry.unit || 'viên'}
            </Text>
          </Descriptions.Item>

          <Descriptions.Item label="Số lô thuốc xuất">
            <span>{entry.batchNumber || entry.medicineBatchId || 'Lô FEFO theo hệ thống'}</span>
          </Descriptions.Item>
        </Descriptions>

        {/* Lý do xác nhận / Ghi chú kiểm soát */}
        <Card
          size="small"
          title={
            <span style={{ fontSize: 13.5, fontWeight: 600, color: '#475569' }}>
              {isPrescribed ? 'Lý do chỉ định của bác sĩ' : 'Lý do xuất kho & Ghi chú đối chiếu'}
            </span>
          }
          style={{
            backgroundColor: '#f8fafc',
            borderColor: '#e2e8f0',
            borderRadius: 8,
          }}
        >
          <Paragraph style={{ margin: 0, color: '#334155', whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>
            {entry.confirmationReason || entry.reason || 'Đã xác nhận đầy đủ theo quy định kiểm soát đặc biệt.'}
          </Paragraph>
        </Card>
      </div>
    </Modal>
  )
}
