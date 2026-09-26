import React from 'react'
import { Tag } from 'antd'
import {
  ClockCircleOutlined,
  SyncOutlined,
  FileDoneOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'

const STATUS_CONFIG = {
  ORDERED: {
    color: 'processing',
    text: 'Chờ tiếp nhận',
    icon: <ClockCircleOutlined />,
  },
  PENDING: {
    color: 'processing',
    text: 'Chờ tiếp nhận',
    icon: <ClockCircleOutlined />,
  },
  IN_PROGRESS: {
    color: 'warning',
    text: 'Đang thực hiện',
    icon: <SyncOutlined spin />,
  },
  RESULTED: {
    color: 'purple',
    text: 'Đã có kết quả',
    icon: <FileDoneOutlined />,
  },
  PARTIALLY_COMPLETED: {
    color: 'purple',
    text: 'Đã có một phần kết quả',
    icon: <FileDoneOutlined />,
  },
  COMPLETED: {
    color: 'success',
    text: 'Hoàn tất',
    icon: <CheckCircleOutlined />,
  },
  CANCELLED: {
    color: 'error',
    text: 'Đã hủy',
    icon: <CloseCircleOutlined />,
  },
}

export const ClinicalOrderStatusBadge = ({ status }) => {
  const config = STATUS_CONFIG[status] || {
    color: 'default',
    text: 'Không xác định',
    icon: null,
  }

  return (
    <Tag color={config.color} icon={config.icon} style={{ borderRadius: 6, fontWeight: 500, padding: '2px 8px' }}>
      {config.text}
    </Tag>
  )
}

export const ClinicalOrderPriorityBadge = ({ priority }) => {
  if (priority === 'URGENT') {
    return (
      <Tag color="red" icon={<ThunderboltOutlined />} style={{ borderRadius: 6, fontWeight: 600, padding: '2px 8px' }}>
        Khẩn cấp
      </Tag>
    )
  }
  return (
    <Tag color="blue" style={{ borderRadius: 6, fontWeight: 500, padding: '2px 8px' }}>
      Thường
    </Tag>
  )
}

export default ClinicalOrderStatusBadge
