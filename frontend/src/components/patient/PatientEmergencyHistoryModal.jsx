import React from 'react'
import { Modal, Table, Tag, Typography, Empty, Space, Button } from 'antd'
import {
  HistoryOutlined,
  UserOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import { formatDateTime } from '../../utils/helpers'
import { getEmergencyContactHistory } from '../../utils/emergencyContactValidation'

const { Text } = Typography

/**
 * Modal hiển thị lịch sử thay đổi thông tin liên hệ khẩn cấp (NCL-02-CN-007-TC-03)
 * @param {Object} props
 * @param {boolean} props.open
 * @param {Function} props.onClose
 * @param {Object} props.patient
 */
export default function PatientEmergencyHistoryModal({
  open,
  onClose,
  patient,
}) {
  if (!patient) return null

  const historyList = getEmergencyContactHistory(patient.id)

  const columns = [
    {
      title: 'Thời điểm thay đổi',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 170,
      render: (val) => (
        <Space size={4}>
          <ClockCircleOutlined style={{ color: '#64748b' }} />
          <Text style={{ fontSize: 13 }}>{formatDateTime(val)}</Text>
        </Space>
      ),
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'actor',
      key: 'actor',
      width: 150,
      render: (val) => (
        <Tag color="blue" icon={<UserOutlined />}>
          {val || 'Lễ tân'}
        </Tag>
      ),
    },
    {
      title: 'Thông tin trước khi sửa',
      dataIndex: 'oldValue',
      key: 'oldValue',
      render: (val) => (
        <span style={{ color: '#64748b', textDecoration: val === 'Chưa thiết lập' ? 'none' : 'line-through' }}>
          {val || 'Chưa thiết lập'}
        </span>
      ),
    },
    {
      title: 'Thông tin mới sau khi sửa',
      dataIndex: 'newValue',
      key: 'newValue',
      render: (val) => (
        <span style={{ fontWeight: 600, color: val === 'Đã xóa người liên hệ' ? '#ef4444' : '#15803d' }}>
          {val}
        </span>
      ),
    },
  ]

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <HistoryOutlined style={{ color: '#0284c7', fontSize: 18 }} />
          <span>Lịch sử thay đổi người liên hệ khẩn cấp - {patient.fullName}</span>
        </div>
      }
      footer={[
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
      width={780}
    >
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary" style={{ fontSize: 13 }}>
          Ghi nhận toàn bộ vết thay đổi theo quy chuẩn <strong>NCL-02-CN-007-TC-03</strong>: người thực hiện, thời điểm và giá trị trước/sau khi sửa.
        </Text>
      </div>

      <Table
        dataSource={historyList}
        columns={columns}
        rowKey="id"
        size="small"
        pagination={{ pageSize: 5 }}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="Chưa có lịch sử thay đổi thông tin người liên hệ khẩn cấp trên hệ thống này"
            />
          ),
        }}
      />
    </Modal>
  )
}
