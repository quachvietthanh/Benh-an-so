import React, { useEffect, useState, useCallback } from 'react'
import {
  Card,
  Table,
  Row,
  Col,
  DatePicker,
  Select,
  Button,
  Tag,
  Space,
  Typography,
  Tooltip,
} from 'antd'
import {
  HistoryOutlined,
  ReloadOutlined,
  EyeOutlined,
  CheckOutlined,
  PlusOutlined,
  SearchOutlined,
  FilterOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useAuthContext } from '../context/AuthContext.jsx'
import cashierShiftApi from '../api/cashierShiftApi.js'
import {
  formatCurrency,
  formatDateTime,
  getShiftStatusMeta,
  canConfirmShift,
  mapCashierShiftError,
} from '../utils/cashierShiftHelpers.js'
import CashierShiftDetailModal from '../components/cashier-shift/CashierShiftDetailModal.jsx'
import ConfirmCashierShiftModal from '../components/cashier-shift/ConfirmCashierShiftModal.jsx'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

export default function CashierShiftHistoryPage() {
  const navigate = useNavigate()
  const { user } = useAuthContext()

  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)

  // Filters & Pagination
  const [statusFilter, setStatusFilter] = useState(null)
  const [dateRange, setDateRange] = useState(null)
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 })

  // Modal states
  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [selectedShiftId, setSelectedShiftId] = useState(null)

  const [confirmModalOpen, setConfirmModalOpen] = useState(false)
  const [shiftToConfirm, setShiftToConfirm] = useState(null)

  const userRoles = (Array.isArray(user?.roles) ? user.roles : [user?.roles || user?.role])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)
  const userPerms = (Array.isArray(user?.permissions) ? user.permissions : [user?.permissions])
    .map((p) => String(p || '').toUpperCase().replace(/^permission_/, ''))
    .filter(Boolean)

  const canCreate = userPerms.includes('CASHIER_SHIFT_CREATE') || userRoles.includes('receptionist') || userRoles.includes('admin')

  const fetchShifts = useCallback(
    async (page = 1, pageSize = 10, status = statusFilter, dates = dateRange) => {
      setLoading(true)
      try {
        const params = {
          page: page - 1, // Spring Data 0-indexed
          size: pageSize,
        }

        if (status) {
          params.status = status
        }

        if (dates && dates.length === 2 && dates[0] && dates[1]) {
          params.from = dates[0].startOf('day').toISOString()
          params.to = dates[1].endOf('day').toISOString()
        }

        const res = await cashierShiftApi.search(params)
        const pageData = res.data

        setData(pageData.content || [])
        setTotal(pageData.totalElements || 0)
        setPagination({ current: page, pageSize })
      } catch (err) {
        console.error('Lỗi tải danh sách phiếu chốt ca:', mapCashierShiftError(err))
        setData([])
        setTotal(0)
      } finally {
        setLoading(false)
      }
    },
    [statusFilter, dateRange]
  )

  useEffect(() => {
    fetchShifts(1, pagination.pageSize)
  }, [fetchShifts, pagination.pageSize])

  const handleFilterChange = (newStatus, newDates) => {
    setStatusFilter(newStatus)
    setDateRange(newDates)
    // Reset về trang 1 khi đổi bộ lọc
    fetchShifts(1, pagination.pageSize, newStatus, newDates)
  }

  const handleResetFilters = () => {
    setStatusFilter(null)
    setDateRange(null)
    fetchShifts(1, pagination.pageSize, null, null)
  }

  const handleTableChange = (newPagination) => {
    fetchShifts(newPagination.current, newPagination.pageSize)
  }

  const openDetail = (id) => {
    setSelectedShiftId(id)
    setDetailModalOpen(true)
  }

  const openConfirm = (shift) => {
    setShiftToConfirm(shift)
    setConfirmModalOpen(true)
  }

  const handleConfirmSuccess = () => {
    fetchShifts(pagination.current, pagination.pageSize)
  }

  const columns = [
    {
      title: 'Mã phiếu',
      dataIndex: 'shiftCode',
      key: 'shiftCode',
      width: 130,
      render: (code, record) => (
        <Button
          type="link"
          style={{ padding: 0, fontWeight: 'bold' }}
          onClick={() => openDetail(record.id)}
        >
          {code}
        </Button>
      ),
    },
    {
      title: 'Thu ngân / Lễ tân',
      dataIndex: 'cashierName',
      key: 'cashierName',
      ellipsis: true,
      render: (name) => <Text strong>{name || '—'}</Text>,
    },
    {
      title: 'Thời gian ca làm việc',
      key: 'shiftTime',
      width: 260,
      render: (_, record) => (
        <span style={{ fontSize: 13 }}>
          {formatDateTime(record.startTime)} <br />
          <Text type="secondary">đến {formatDateTime(record.endTime)}</Text>
        </span>
      ),
    },
    {
      title: 'Tổng thu hệ thống',
      dataIndex: 'totalSystemAmount',
      key: 'totalSystemAmount',
      align: 'right',
      render: (val) => <Text strong>{formatCurrency(val)}</Text>,
    },
    {
      title: 'Tiền mặt thực đếm',
      dataIndex: 'actualCashAmount',
      key: 'actualCashAmount',
      align: 'right',
      render: (val) => <Text style={{ color: '#389e0d' }}>{formatCurrency(val)}</Text>,
    },
    {
      title: 'Chênh lệch',
      dataIndex: 'differenceAmount',
      key: 'differenceAmount',
      align: 'right',
      render: (val) => {
        const num = Number(val) || 0
        if (num === 0) {
          return <Tag color="green">0 ₫</Tag>
        }
        if (num > 0) {
          return <Tag color="orange">+{formatCurrency(num)}</Tag>
        }
        return <Tag color="red">{formatCurrency(num)}</Tag>
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      align: 'center',
      width: 150,
      render: (status) => {
        const meta = getShiftStatusMeta(status)
        return <Tag color={meta.tagColor}>{meta.label}</Tag>
      },
    },
    {
      title: 'Người duyệt',
      dataIndex: 'confirmedByName',
      key: 'confirmedByName',
      ellipsis: true,
      render: (name, record) => {
        if (!name) return <Text type="secondary">—</Text>
        return (
          <Tooltip title={`Duyệt lúc: ${formatDateTime(record.confirmedAt)}`}>
            <span>{name}</span>
          </Tooltip>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      align: 'center',
      width: 140,
      render: (_, record) => {
        const canConfirm = canConfirmShift(user, record)
        return (
          <Space orientation="horizontal" size="small">
            <Tooltip title="Xem chi tiết phiếu chốt ca">
              <Button
                type="text"
                icon={<EyeOutlined />}
                onClick={() => openDetail(record.id)}
              />
            </Tooltip>

            {/* Nút duyệt chỉ hiển thị khi có quyền duyệt VÀ không phải tự duyệt */}
            {canConfirm && (
              <Tooltip title="Duyệt phiếu chốt ca">
                <Button
                  type="primary"
                  size="small"
                  icon={<CheckOutlined />}
                  onClick={() => openConfirm(record)}
                >
                  Duyệt
                </Button>
              </Tooltip>
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <div style={{ padding: '4px 0 24px 0' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <HistoryOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Lịch sử chốt ca thu ngân
          </Title>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => fetchShifts(pagination.current, pagination.pageSize)}>
            Làm mới
          </Button>
          {canCreate && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/cashier-shifts/close')}>
              Chốt ca mới
            </Button>
          )}
        </Space>
      </div>

      {/* Filter Card */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 12]} align="middle">
          <Col xs={24} sm={12} md={8} lg={6}>
            <div style={{ marginBottom: 4 }}>
              <Text strong style={{ fontSize: 13 }}><FilterOutlined /> Trạng thái phiếu:</Text>
            </div>
            <Select
              allowClear
              placeholder="Tất cả trạng thái"
              style={{ width: '100%' }}
              value={statusFilter}
              onChange={(val) => handleFilterChange(val, dateRange)}
              options={[
                { label: 'Chờ quản lý duyệt (PENDING_CONFIRMATION)', value: 'PENDING_CONFIRMATION' },
                { label: 'Đã xác nhận (CONFIRMED)', value: 'CONFIRMED' },
                { label: 'Đã từ chối (REJECTED)', value: 'REJECTED' },
              ]}
            />
          </Col>

          <Col xs={24} sm={12} md={10} lg={8}>
            <div style={{ marginBottom: 4 }}>
              <Text strong style={{ fontSize: 13 }}><SearchOutlined /> Khoảng thời gian ca:</Text>
            </div>
            <RangePicker
              style={{ width: '100%' }}
              value={dateRange}
              onChange={(dates) => handleFilterChange(statusFilter, dates)}
              format="DD/MM/YYYY"
              placeholder={['Từ ngày', 'Đến ngày']}
            />
          </Col>

          <Col xs={24} sm={24} md={6} lg={4} style={{ display: 'flex', alignItems: 'flex-end', paddingTop: 20 }}>
            <Button onClick={handleResetFilters} block>
              Xóa bộ lọc
            </Button>
          </Col>
        </Row>
      </Card>

      {/* Main Table */}
      <Card bordered bodyStyle={{ padding: 0 }}>
        <Table
          dataSource={data}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `Tổng cộng ${t} phiếu chốt ca`,
            pageSizeOptions: ['10', '20', '50'],
          }}
          onChange={handleTableChange}
          locale={{ emptyText: 'Chưa có phiếu chốt ca nào phù hợp với điều kiện tìm kiếm.' }}
        />
      </Card>

      {/* Detail Modal */}
      <CashierShiftDetailModal
        open={detailModalOpen}
        shiftId={selectedShiftId}
        onClose={() => {
          setDetailModalOpen(false)
          setSelectedShiftId(null)
        }}
      />

      {/* Confirm Modal */}
      <ConfirmCashierShiftModal
        open={confirmModalOpen}
        shift={shiftToConfirm}
        onClose={() => {
          setConfirmModalOpen(false)
          setShiftToConfirm(null)
        }}
        onSuccess={handleConfirmSuccess}
      />
    </div>
  )
}
