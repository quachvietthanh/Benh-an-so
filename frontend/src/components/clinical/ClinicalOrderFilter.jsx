import React from 'react'
import { Card, Row, Col, Input, Select, DatePicker, Button, Space, Tooltip } from 'antd'
import { SearchOutlined, FilterOutlined, ReloadOutlined } from '@ant-design/icons'

const { RangePicker } = DatePicker
const { Option } = Select

export const ClinicalOrderFilter = ({
  searchText,
  onSearchChange,
  statusFilter,
  onStatusChange,
  priorityFilter,
  onPriorityChange,
  categoryFilter,
  onCategoryChange,
  dateRange,
  onDateRangeChange,
  onReset,
}) => {
  return (
    <Card size="small" style={{ marginBottom: 16, borderRadius: 8, boxShadow: '0 1px 2px rgba(0,0,0,0.03)' }}>
      <Row gutter={[12, 12]} align="middle">
        <Col xs={24} sm={12} md={6} lg={6}>
          <Input
            prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
            placeholder="Tìm theo Mã chỉ định, Tên BN, Mã BN..."
            value={searchText}
            onChange={(e) => onSearchChange(e.target.value)}
            allowClear
          />
        </Col>

        <Col xs={12} sm={6} md={4} lg={4}>
          <Select
            placeholder="Trạng thái"
            value={statusFilter}
            onChange={onStatusChange}
            style={{ width: '100%' }}
          >
            <Option value="ALL">Tất cả trạng thái</Option>
            <Option value="PENDING">Chờ tiếp nhận</Option>
            <Option value="IN_PROGRESS">Đang thực hiện</Option>
            <Option value="RESULTED">Đã có kết quả</Option>
            <Option value="COMPLETED">Hoàn tất</Option>
            <Option value="CANCELLED">Đã hủy</Option>
          </Select>
        </Col>

        <Col xs={12} sm={6} md={4} lg={4}>
          <Select
            placeholder="Độ ưu tiên"
            value={priorityFilter}
            onChange={onPriorityChange}
            style={{ width: '100%' }}
          >
            <Option value="ALL">Tất cả độ ưu tiên</Option>
            <Option value="URGENT">Khẩn cấp</Option>
            <Option value="NORMAL">Thường</Option>
          </Select>
        </Col>

        <Col xs={12} sm={8} md={4} lg={4}>
          <Select
            placeholder="Nhóm dịch vụ"
            value={categoryFilter}
            onChange={onCategoryChange}
            style={{ width: '100%' }}
          >
            <Option value="ALL">Tất cả nhóm</Option>
            <Option value="LABORATORY">Xét nghiệm</Option>
            <Option value="IMAGING">Chẩn đoán hình ảnh</Option>
            <Option value="FUNCTIONAL">Thăm dò chức năng</Option>
          </Select>
        </Col>

        <Col xs={24} sm={10} md={4} lg={4}>
          <RangePicker
            value={dateRange}
            onChange={onDateRangeChange}
            format="DD/MM/YYYY"
            placeholder={['Từ ngày', 'Đến ngày']}
            style={{ width: '100%' }}
          />
        </Col>

        <Col xs={24} sm={6} md={2} lg={2} style={{ textAlign: 'right' }}>
          <Tooltip title="Đặt lại bộ lọc">
            <Button icon={<ReloadOutlined />} onClick={onReset}>
              Xóa lọc
            </Button>
          </Tooltip>
        </Col>
      </Row>
    </Card>
  )
}

export default ClinicalOrderFilter
