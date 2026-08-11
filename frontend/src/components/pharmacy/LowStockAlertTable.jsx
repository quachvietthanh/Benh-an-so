import React, { useState } from 'react'
import {
  Alert,
  Button,
  Empty,
  Input,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  AlertOutlined,
  EditOutlined,
  InboxOutlined,
  ReloadOutlined,
  SearchOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'

const { Text } = Typography

function LowStockAlertTable({
  items = [],
  loading = false,
  onRefresh,
  onEditThreshold,
}) {
  const navigate = useNavigate()
  const [searchKeyword, setSearchKeyword] = useState('')

  const filteredItems = items.filter((item) => {
    const keyword = searchKeyword.trim().toLowerCase()
    if (!keyword) return true
    return [
      item.medicineCode,
      item.medicineName,
      item.activeIngredient,
      item.unit,
    ].some((field) => String(field || '').toLowerCase().includes(keyword))
  })

  const handleCreateReceipt = (record) => {
    // Điều hướng sang trang Nhập kho kèm dữ liệu thuốc và số lượng đề xuất
    navigate('/pharmacy/receipts', {
      state: {
        prefillItem: {
          medicineId: record.medicineId,
          medicineName: record.medicineName,
          medicineCode: record.medicineCode,
          quantity: Math.max(record.shortageQuantity || 1, 1),
          unit: record.unit,
        },
      },
    })
  }

  const columns = [
    {
      title: 'Mã thuốc',
      dataIndex: 'medicineCode',
      key: 'medicineCode',
      width: 120,
      render: (val) => <Text code>{val || '—'}</Text>,
    },
    {
      title: 'Tên thuốc & Hoạt chất',
      key: 'medicineInfo',
      render: (_, record) => (
        <Space direction="vertical" size={1}>
          <strong>{record.medicineName}</strong>
          {record.activeIngredient && (
            <Text orientation="left" type="secondary" style={{ fontSize: 12 }}>
              Hoạt chất: {record.activeIngredient}
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Đơn vị',
      dataIndex: 'unit',
      key: 'unit',
      width: 90,
      align: 'center',
      render: (val) => <Tag color="blue">{val || '—'}</Tag>,
    },
    {
      title: 'Tồn thực tế',
      dataIndex: 'stockQuantity',
      key: 'stockQuantity',
      width: 110,
      align: 'right',
      render: (val, record) => (
        <span style={{ fontWeight: 600 }}>
          {Number(val || 0).toLocaleString('vi-VN')} {record.unit || ''}
        </span>
      ),
    },
    {
      title: 'Tồn khả dụng (FEFO)',
      dataIndex: 'eligibleStockQuantity',
      key: 'eligibleStockQuantity',
      width: 150,
      align: 'right',
      render: (val, record) => {
        const num = Number(val || 0)
        return (
          <Tag color={num === 0 ? 'red' : 'orange'} style={{ fontWeight: 600 }}>
            {num.toLocaleString('vi-VN')} {record.unit || ''}
          </Tag>
        )
      },
    },
    {
      title: 'Ngưỡng tồn tối thiểu',
      dataIndex: 'minStockThreshold',
      key: 'minStockThreshold',
      width: 160,
      align: 'right',
      render: (val, record) => (
        <Space>
          <span style={{ fontWeight: 600 }}>
            {Number(val || 0).toLocaleString('vi-VN')} {record.unit || ''}
          </span>
          <Tooltip title="Chỉnh sửa ngưỡng tồn">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => onEditThreshold?.(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
    {
      title: 'Cần bổ sung (Thiếu)',
      dataIndex: 'shortageQuantity',
      key: 'shortageQuantity',
      width: 150,
      align: 'right',
      render: (val, record) => (
        <Tag
          color="error"
          style={{
            fontSize: 13,
            fontWeight: 700,
            padding: '2px 8px',
          }}
        >
          + {Number(val || 0).toLocaleString('vi-VN')} {record.unit || ''}
        </Tag>
      ),
    },
    {
      title: 'Mức độ cảnh báo',
      key: 'urgency',
      width: 150,
      align: 'center',
      render: (_, record) => {
        const isOutOfStock = Number(record.eligibleStockQuantity || 0) === 0
        return isOutOfStock ? (
          <Tag color="volcano" icon={<AlertOutlined />}>
            Hết hàng (Khẩn cấp)
          </Tag>
        ) : (
          <Tag color="warning" icon={<WarningOutlined />}>
            Dưới ngưỡng tồn
          </Tag>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 160,
      align: 'center',
      render: (_, record) => (
        <Button
          type="primary"
          size="small"
          icon={<InboxOutlined />}
          onClick={() => handleCreateReceipt(record)}
        >
          Nhập kho ngay
        </Button>
      ),
    },
  ]

  return (
    <div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 12,
          marginBottom: 16,
        }}
      >
        <Space wrap>
          <Input
            placeholder="Tìm theo mã hoặc tên thuốc thiếu..."
            allowClear
            prefix={<SearchOutlined />}
            style={{ width: 300 }}
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
        </Space>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            loading={loading}
            onClick={onRefresh}
          >
            Làm mới danh sách
          </Button>
        </Space>
      </div>

      {items.length === 0 && !loading ? (
        <Alert
          type="success"
          showIcon
          message="Kho thuốc an toàn"
          description="Hiện tại không có loại thuốc nào dưới ngưỡng tồn tối thiểu. Tất cả thuốc đều đáp ứng đủ mức tồn dự trữ quy định."
          style={{ marginBottom: 16 }}
        />
      ) : null}

      <Table
        rowKey={(record) => record.medicineId || record.id}
        columns={columns}
        dataSource={filteredItems}
        loading={loading}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50'],
          showTotal: (total) => `Tổng số ${total} thuốc cần bổ sung`,
        }}
        locale={{
          emptyText: (
            <Empty
              description="Không có thuốc nào dưới ngưỡng tồn tối thiểu"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          ),
        }}
        scroll={{ x: 1000 }}
      />
    </div>
  )
}

export default LowStockAlertTable
