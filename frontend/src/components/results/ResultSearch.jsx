import React from 'react'
import { Input } from 'antd'
import { SearchOutlined } from '@ant-design/icons'

export const ResultSearch = ({ value, onChange, placeholder = 'Tìm kiếm theo Mã CĐ, Mã BN, Tên bệnh nhân...' }) => {
  return (
    <Input
      prefix={<SearchOutlined style={{ color: '#94a3b8', fontSize: 16 }} />}
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      allowClear
      style={{
        borderRadius: '10px',
        padding: '8px 14px',
        border: '1px solid #cbd5e1',
        fontSize: '14px',
        boxShadow: '0 1px 2px rgba(0,0,0,0.02)',
      }}
    />
  )
}

export default ResultSearch
