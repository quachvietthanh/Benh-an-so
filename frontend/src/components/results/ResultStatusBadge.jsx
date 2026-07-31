import React from 'react'
import { Tag } from 'antd'
import {
  ClockCircleOutlined,
  SyncOutlined,
  FileDoneOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'

const STATUS_MAP = {
  PENDING: {
    color: 'processing',
    text: 'Chờ thực hiện',
    icon: <ClockCircleOutlined />,
    bg: '#e6f7ff',
    border: '#91d5ff',
    textColor: '#096dd9',
  },
  IN_PROGRESS: {
    color: 'warning',
    text: 'Đang thực hiện',
    icon: <SyncOutlined spin />,
    bg: '#fff7e6',
    border: '#ffd591',
    textColor: '#d46b08',
  },
  RESULTED: {
    color: 'purple',
    text: 'Đã có kết quả',
    icon: <FileDoneOutlined />,
    bg: '#f9f0ff',
    border: '#d3ade6',
    textColor: '#531dab',
  },
  CONFIRMED: {
    color: 'success',
    text: 'Đã xác nhận',
    icon: <CheckCircleOutlined />,
    bg: '#f6ffed',
    border: '#b7eb8f',
    textColor: '#389e0d',
  },
}

export const ResultStatusBadge = ({ status = 'PENDING' }) => {
  const config = STATUS_MAP[status] || {
    color: 'default',
    text: status || 'Chưa xác định',
    icon: null,
    bg: '#fafafa',
    border: '#d9d9d9',
    textColor: '#595959',
  }

  return (
    <Tag
      color={config.color}
      icon={config.icon}
      style={{
        borderRadius: '12px',
        fontWeight: 600,
        fontSize: '12px',
        padding: '3px 10px',
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        boxShadow: '0 1px 2px rgba(0,0,0,0.03)',
      }}
    >
      {config.text}
    </Tag>
  )
}

export default ResultStatusBadge
