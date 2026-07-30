import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Card,
  Row,
  Col,
  Button,
  Statistic,
  Typography,
  Space,
  message,
  Spin,
  Breadcrumb,
  Modal,
  Tabs,
  Badge,
} from 'antd'
import {
  PlusOutlined,
  ReloadOutlined,
  ExperimentOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  FileDoneOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import ClinicalOrderFilter from '../components/clinical/ClinicalOrderFilter'
import ClinicalOrderTable from '../components/clinical/ClinicalOrderTable'
import CreateClinicalOrderModal from '../components/clinical/CreateClinicalOrderModal'
import EditClinicalOrderModal from '../components/clinical/EditClinicalOrderModal'
import ClinicalOrderDetailModal from '../components/clinical/ClinicalOrderDetailModal'
import PrintClinicalOrderModal from '../components/clinical/PrintClinicalOrderModal'

import { useAuthContext } from '../context/AuthContext'
import { demoClinicalOrders } from '../mock-data/mockData'
import {
  mergeClinicalOrders,
  saveStoredClinicalOrder,
  deleteStoredClinicalOrder,
  logMedicalAccess,
} from '../utils/storageHelpers'

import { Form, Select, Input } from 'antd'

const { Title, Text } = Typography

export function ClinicalOrdersPage() {
  const { user } = useAuthContext()
  const canManage = user?.roles?.some((role) => ['admin', 'doctor', 'receptionist'].includes(role))

  const [loading, setLoading] = useState(false)
  const [orders, setOrders] = useState([])

  // Filters state
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [priorityFilter, setPriorityFilter] = useState('ALL')
  const [categoryFilter, setCategoryFilter] = useState('ALL')
  const [dateRange, setDateRange] = useState(null)

  // Modals state
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [editModalVisible, setEditModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [printModalVisible, setPrintModalVisible] = useState(false)
  const [cancelModalVisible, setCancelModalVisible] = useState(false)

  const [selectedOrder, setSelectedOrder] = useState(null)
  const [orderToCancel, setOrderToCancel] = useState(null)
  const [cancelReasonSelect, setCancelReasonSelect] = useState('Bệnh nhân đổi ý / Không thực hiện')
  const [cancelReasonCustom, setCancelReasonCustom] = useState('')

  const handleDeleteOrder = (orderId) => {
    const target = orders.find((o) => o.id === orderId || o.orderCode === orderId)
    if (target) {
      setOrderToCancel(target)
      setCancelReasonSelect('Bệnh nhân đổi ý / Không thực hiện')
      setCancelReasonCustom('')
      setCancelModalVisible(true)
    }
  }

  const handleConfirmCancelOrder = () => {
    if (!orderToCancel) return

    const finalReason = cancelReasonSelect === 'OTHER' ? (cancelReasonCustom.trim() || 'Lý do khác') : cancelReasonSelect

    const cancelledOrder = {
      ...orderToCancel,
      status: 'CANCELLED',
      cancelReason: finalReason,
      cancelledBy: user?.fullName || 'BS. Phạm Hồng Anh',
      updatedAt: new Date().toISOString(),
    }

    // Save to LocalStorage
    saveStoredClinicalOrder(cancelledOrder)

    // Log to Audit History
    logMedicalAccess({
      userName: user?.fullName || 'BS. Phạm Hồng Anh',
      patientName: orderToCancel.patientName,
      recordCode: orderToCancel.orderCode,
      action: `Hủy chỉ định CLS - Lý do: ${finalReason}`,
    })

    setOrders((prev) => prev.map((o) => (o.id === orderToCancel.id ? cancelledOrder : o)))
    message.success(`Đã hủy phiếu chỉ định ${orderToCancel.orderCode} và lưu vào lịch sử thành công!`)
    setCancelModalVisible(false)
    setOrderToCancel(null)
  }

  const loadData = useCallback(() => {
    setLoading(true)
    try {
      const merged = mergeClinicalOrders(demoClinicalOrders)
      setOrders(merged)
    } catch (err) {
      console.error(err)
      message.error('Không thể tải danh sách chỉ định cận lâm sàng')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  const [activeTab, setActiveTab] = useState('ACTIVE') // 'ACTIVE' | 'CANCELLED'

  // Filter logic
  const filteredOrders = useMemo(() => {
    const kw = searchText.trim().toLowerCase()
    return orders.filter((order) => {
      // Tab filter logic: ACTIVE tab excludes CANCELLED, CANCELLED tab shows ONLY CANCELLED
      if (activeTab === 'ACTIVE' && order.status === 'CANCELLED') return false
      if (activeTab === 'CANCELLED' && order.status !== 'CANCELLED') return false

      const matchesKw = !kw || [
        order.orderCode,
        order.patientName,
        order.patientCode,
        order.diagnosis,
        order.doctorName,
        order.cancelReason,
      ].some((val) => String(val || '').toLowerCase().includes(kw))

      const matchesStatus = statusFilter === 'ALL' || order.status === statusFilter
      const matchesPriority = priorityFilter === 'ALL' || order.priority === priorityFilter

      const matchesCategory = categoryFilter === 'ALL' || order.items?.some((item) => {
        const cat = item.category || item.categoryName || ''
        if (categoryFilter === 'LABORATORY') return cat.includes('LAB') || cat.includes('Xét nghiệm')
        if (categoryFilter === 'IMAGING') return cat.includes('IMG') || cat.includes('hình ảnh')
        if (categoryFilter === 'FUNCTIONAL') return cat.includes('FUNC') || cat.includes('chức năng')
        return true
      })

      const createdAt = order.createdAt ? dayjs(order.createdAt) : null
      const matchesDate = !dateRange?.length || (createdAt
        && !createdAt.isBefore(dateRange[0].startOf('day'))
        && !createdAt.isAfter(dateRange[1].endOf('day')))

      return matchesKw && matchesStatus && matchesPriority && matchesCategory && matchesDate
    })
  }, [orders, activeTab, searchText, statusFilter, priorityFilter, categoryFilter, dateRange])

  // Stats calculation
  const stats = useMemo(() => {
    const total = orders.length
    const pending = orders.filter((o) => o.status === 'PENDING').length
    const inProgress = orders.filter((o) => o.status === 'IN_PROGRESS').length
    const resulted = orders.filter((o) => o.status === 'RESULTED').length
    const completed = orders.filter((o) => o.status === 'COMPLETED').length
    const cancelled = orders.filter((o) => o.status === 'CANCELLED').length
    const urgent = orders.filter((o) => o.priority === 'URGENT').length
    return { total, pending, inProgress, resulted, completed, cancelled, urgent }
  }, [orders])

  // Actions
  const handleResetFilter = () => {
    setSearchText('')
    setStatusFilter('ALL')
    setPriorityFilter('ALL')
    setCategoryFilter('ALL')
    setDateRange(null)
  }

  const handleCreateSuccess = (newOrder) => {
    saveStoredClinicalOrder(newOrder)
    setOrders((prev) => [newOrder, ...prev.filter((o) => o.id !== newOrder.id)])
  }

  const handleUpdateSuccess = (updatedOrder) => {
    saveStoredClinicalOrder(updatedOrder)
    setOrders((prev) => prev.map((o) => (o.id === updatedOrder.id ? updatedOrder : o)))
  }

  const handleUpdateStatus = (orderId, newStatus) => {
    const target = orders.find((o) => o.id === orderId)
    if (!target) return

    const updated = {
      ...target,
      status: newStatus,
      updatedAt: new Date().toISOString(),
    }
    saveStoredClinicalOrder(updated)
    setOrders((prev) => prev.map((o) => (o.id === orderId ? updated : o)))
    message.success(`Đã cập nhật trạng thái phiếu ${target.orderCode}`)
  }

  const handleOpenDetail = (order) => {
    setSelectedOrder(order)
    setDetailModalVisible(true)
  }

  const handleOpenEdit = (order) => {
    setSelectedOrder(order)
    setEditModalVisible(true)
  }

  const handleOpenPrint = (order) => {
    setSelectedOrder(order)
    setPrintModalVisible(true)
  }

  return (
    <div style={{ padding: '4px 0 24px' }}>
      {/* Header Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <ExperimentOutlined style={{ color: '#1890ff', marginRight: 8 }} />
            Quản lý Chỉ định Cận lâm sàng
          </Title>
          <Text type="secondary">
            Lập biểu mẫu chỉ định dịch vụ cận lâm sàng (Xét nghiệm, CĐHA, TDCN) và quản lý tiến trình thực hiện
          </Text>
        </div>

        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={loadData}>
            Tải lại
          </Button>

          {canManage && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateModalVisible(true)}
              style={{ background: '#1890ff', borderColor: '#1890ff' }}
            >
              Tạo chỉ định mới
            </Button>
          )}
        </Space>
      </div>

      {/* Overview Statistics Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={8} md={4}>
          <Card size="small" style={{ borderRadius: 8, borderLeft: '4px solid #1890ff' }}>
            <Statistic
              title="Tổng chỉ định"
              value={stats.total}
              prefix={<ExperimentOutlined style={{ color: '#1890ff' }} />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={4}>
          <Card size="small" style={{ borderRadius: 8, borderLeft: '4px solid #096dd9' }}>
            <Statistic
              title="Chờ tiếp nhận"
              value={stats.pending}
              valueStyle={{ color: '#096dd9' }}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={4}>
          <Card size="small" style={{ borderRadius: 8, borderLeft: '4px solid #fa8c16' }}>
            <Statistic
              title="Đang thực hiện"
              value={stats.inProgress}
              valueStyle={{ color: '#fa8c16' }}
              prefix={<SyncOutlined spin />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={4}>
          <Card size="small" style={{ borderRadius: 8, borderLeft: '4px solid #722ed1' }}>
            <Statistic
              title="Đã có kết quả"
              value={stats.resulted}
              valueStyle={{ color: '#722ed1' }}
              prefix={<FileDoneOutlined />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={4}>
          <Card size="small" style={{ borderRadius: 8, borderLeft: '4px solid #52c41a' }}>
            <Statistic
              title="Hoàn tất"
              value={stats.completed}
              valueStyle={{ color: '#52c41a' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={4}>
          <Card size="small" style={{ borderRadius: 8, borderLeft: '4px solid #ff4d4f' }}>
            <Statistic
              title="Chỉ định Khẩn"
              value={stats.urgent}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Filter Component */}
      <ClinicalOrderFilter
        searchText={searchText}
        onSearchChange={setSearchText}
        statusFilter={statusFilter}
        onStatusChange={setStatusFilter}
        priorityFilter={priorityFilter}
        onPriorityChange={setPriorityFilter}
        categoryFilter={categoryFilter}
        onCategoryChange={setCategoryFilter}
        dateRange={dateRange}
        onDateRangeChange={setDateRange}
        onReset={handleResetFilter}
      />

      {/* Main View Tabs: Active Orders vs Cancelled History */}
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        style={{ marginBottom: 0 }}
        items={[
          {
            key: 'ACTIVE',
            label: (
              <Space>
                <span>Chỉ định đang xử lý</span>
                <Badge count={stats.total - stats.cancelled} overflowCount={99} style={{ backgroundColor: '#1890ff' }} />
              </Space>
            ),
          },
          {
            key: 'CANCELLED',
            label: (
              <Space>
                <span style={{ color: activeTab === 'CANCELLED' ? '#cf1322' : 'inherit' }}>Lịch sử phiếu đã hủy</span>
                <Badge count={stats.cancelled} overflowCount={99} style={{ backgroundColor: '#ff4d4f' }} />
              </Space>
            ),
          },
        ]}
      />

      {/* Table Component */}
      <Card size="small" style={{ borderRadius: '0 8px 8px 8px' }}>
        <ClinicalOrderTable
          dataSource={filteredOrders}
          loading={loading}
          onViewDetail={handleOpenDetail}
          onEditOrder={handleOpenEdit}
          onUpdateStatus={handleUpdateStatus}
          onDeleteOrder={handleDeleteOrder}
          onPrintOrder={handleOpenPrint}
          canManage={canManage}
        />
      </Card>

      {/* Modals */}
      <CreateClinicalOrderModal
        visible={createModalVisible}
        onClose={() => setCreateModalVisible(false)}
        onCreateSuccess={handleCreateSuccess}
      />

      <EditClinicalOrderModal
        visible={editModalVisible}
        order={selectedOrder}
        onClose={() => setEditModalVisible(false)}
        onUpdateSuccess={handleUpdateSuccess}
      />

      <ClinicalOrderDetailModal
        visible={detailModalVisible}
        order={selectedOrder}
        onClose={() => setDetailModalVisible(false)}
        onPrintOrder={handleOpenPrint}
      />

      <PrintClinicalOrderModal
        visible={printModalVisible}
        order={selectedOrder}
        onClose={() => setPrintModalVisible(false)}
      />

      {/* Cancel Order Modal with Reason Input */}
      <Modal
        title={
          <span style={{ color: '#cf1322', fontWeight: 600, fontSize: 16 }}>
            Xác nhận Hủy phiếu chỉ định #{orderToCancel?.orderCode}
          </span>
        }
        open={cancelModalVisible}
        onCancel={() => setCancelModalVisible(false)}
        onOk={handleConfirmCancelOrder}
        okText="Xác nhận hủy & Lưu lịch sử"
        okButtonProps={{ danger: true }}
        cancelText="Quay lại"
        destroyOnClose
      >
        <div style={{ padding: '8px 0' }}>
          <div style={{ marginBottom: 12, background: '#fff2f0', padding: 10, borderRadius: 6, border: '1px solid #ffccc7' }}>
            <div><b>Bệnh nhân:</b> {orderToCancel?.patientName} [{orderToCancel?.patientCode}]</div>
            <div><b>Chẩn đoán:</b> {orderToCancel?.diagnosis}</div>
            <div><b>Tổng số tiền:</b> <span style={{ color: '#cf1322', fontWeight: 600 }}>{Number(orderToCancel?.totalAmount || 0).toLocaleString('vi-VN')} đ</span></div>
          </div>

          <Form layout="vertical">
            <Form.Item label="Chọn lý do hủy phiếu chỉ định:" required>
              <Select
                value={cancelReasonSelect}
                onChange={setCancelReasonSelect}
                style={{ width: '100%' }}
              >
                <Select.Option value="Bệnh nhân đổi ý / Không thực hiện">Bệnh nhân đổi ý / Không thực hiện</Select.Option>
                <Select.Option value="Chỉ định trùng / Nhầm lẫn dịch vụ">Chỉ định trùng / Nhầm lẫn dịch vụ</Select.Option>
                <Select.Option value="Bác sĩ thay đổi phác đồ điều trị">Bác sĩ thay đổi phác đồ điều trị</Select.Option>
                <Select.Option value="Phòng máy bảo trì / Ngừng nhận dịch vụ">Phòng máy bảo trì / Ngừng nhận dịch vụ</Select.Option>
                <Select.Option value="OTHER">Lý do khác (Nhập chi tiết bên dưới)</Select.Option>
              </Select>
            </Form.Item>

            {cancelReasonSelect === 'OTHER' && (
              <Form.Item label="Nhập lý do chi tiết:" required>
                <Input.TextArea
                  rows={3}
                  placeholder="Ghi rõ lý do hủy chỉ định cận lâm sàng..."
                  value={cancelReasonCustom}
                  onChange={(e) => setCancelReasonCustom(e.target.value)}
                />
              </Form.Item>
            )}
          </Form>
        </div>
      </Modal>
    </div>
  )
}

export default ClinicalOrdersPage
