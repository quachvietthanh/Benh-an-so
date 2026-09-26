import React from 'react'
import { Select, Space, Button, Tooltip } from 'antd'
import { FilterOutlined, ReloadOutlined } from '@ant-design/icons'

const { Option } = Select

export const ResultFilter = ({
  status,
  onStatusChange,
  category,
  onCategoryChange,
  onReset,
}) => {
  return (
    <Space wrap size={12}>
      <Select
        value={status}
        onChange={onStatusChange}
        style={{ width: 170 }}
        dropdownStyle={{ borderRadius: 10 }}
      >
        <Option value="ALL">Tất cả trạng thái</Option>
        <Option value="PENDING">Chờ thực hiện</Option>
        <Option value="IN_PROGRESS">Đang thực hiện</Option>
        <Option value="RESULTED">Đã có kết quả</Option>
        <Option value="CONFIRMED">Đã xác nhận</Option>
      </Select>

      <Select
        value={category}
        onChange={onCategoryChange}
        style={{ width: 190 }}
        dropdownStyle={{ borderRadius: 10 }}
      >
        <Option value="ALL">Tất cả loại CĐLS</Option>
        <Option value="LABORATORY">Xét nghiệm (Máu, Nước tiểu)</Option>
        <Option value="IMAGING">Chẩn đoán hình ảnh (X-Quang, MRI...)</Option>
        <Option value="FUNCTIONAL">Thăm dò chức năng (Điện tim...)</Option>
      </Select>

      <Tooltip title="Đặt lại bộ lọc">
        <Button
          icon={<ReloadOutlined />}
          onClick={onReset}
          style={{ borderRadius: '8px', color: '#64748b' }}
        >
          Xóa lọc
        </Button>
      </Tooltip>
    </Space>
  )
}

export default ResultFilter
