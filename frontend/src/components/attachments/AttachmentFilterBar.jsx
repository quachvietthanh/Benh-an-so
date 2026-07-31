import React from 'react'
import { Button, Col, Input, Row, Select } from 'antd'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import { CATEGORY_OPTIONS } from './attachmentConstants.jsx'

function AttachmentFilterBar({
  searchText,
  setSearchText,
  selectedCategory,
  setSelectedCategory,
  selectedStatus,
  setSelectedStatus,
  categoryOptions = CATEGORY_OPTIONS,
  onReload,
}) {
  return (
    <Row gutter={[12, 12]} style={{ marginBottom: 16 }} align="middle">
      <Col xs={24} sm={12} md={8}>
        <Input
          prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
          placeholder="Tìm theo tên BN, mã tệp, loại xét nghiệm..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          allowClear
        />
      </Col>
      <Col xs={12} sm={6} md={5}>
        <Select
          style={{ width: '100%' }}
          value={selectedCategory}
          onChange={setSelectedCategory}
          options={[
            { value: 'ALL', label: 'Tất cả loại kết quả' },
            ...categoryOptions.map((c) => ({ value: c.value, label: c.label || c.value })),
          ]}
        />
      </Col>
      <Col xs={12} sm={6} md={5}>
        <Select
          style={{ width: '100%' }}
          value={selectedStatus}
          onChange={setSelectedStatus}
          options={[
            { value: 'ALL', label: 'Tất cả trạng thái' },
            { value: 'NORMAL', label: '✅ Bình thường' },
            { value: 'ABNORMAL', label: '⚠️ Cần chú ý / Bất thường' },
          ]}
        />
      </Col>
      <Col xs={24} sm={24} md={6} style={{ textAlign: 'right' }}>
        <Button icon={<ReloadOutlined />} onClick={onReload}>
          Làm mới
        </Button>
      </Col>
    </Row>
  )
}

export default AttachmentFilterBar
