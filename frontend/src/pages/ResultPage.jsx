import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Card,
  Row,
  Col,
  Statistic,
  Button,
  Typography,
  Space,
  message,
  Breadcrumb,
} from 'antd'
import {
  FileDoneOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
  ExperimentOutlined,
} from '@ant-design/icons'

import ResultSearch from '../components/results/ResultSearch'
import ResultFilter from '../components/results/ResultFilter'
import ResultTable from '../components/results/ResultTable'
import ResultModal from '../components/results/ResultModal'

import clinicalResultApi from '../api/clinicalResultApi'
import queueApi from '../api/queueApi'
import { mergeClinicalOrders, saveStoredClinicalOrder, getStoredQueueItems, saveStoredQueueItem, getStoredMedicalRecords, saveStoredMedicalRecord } from '../utils/storageHelpers'

const { Title, Text } = Typography

export function ResultPage() {
  const [loading, setLoading] = useState(false)
  const [orders, setOrders] = useState([])

  // Search & Filters state
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [categoryFilter, setCategoryFilter] = useState('ALL')

  // Modal state
  const [modalVisible, setModalVisible] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      // 1. Attempt RESTful API fetch GET /api/results
      try {
        const response = await clinicalResultApi.getAll({
          search: searchText,
          status: statusFilter,
          category: categoryFilter,
        })
        if (response.data && Array.isArray(response.data)) {
          setOrders(response.data)
          setLoading(false)
          return
        }
      } catch (apiErr) {
        // Fallback to local storage & mock data if backend not connected yet
      }

      // 2. Local storage fallback (real database/user created clinical orders)
      const merged = mergeClinicalOrders([])
      setOrders(merged)
    } catch (err) {
      console.error('Error loading clinical results:', err)
      message.error('Không thể tải danh sách chỉ định cận lâm sàng')
    } finally {
      setLoading(false)
    }
  }, [searchText, statusFilter, categoryFilter])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Filtered orders calculation
  const filteredOrders = useMemo(() => {
    const kw = searchText.trim().toLowerCase()
    return orders.filter((order) => {
      // Ignore cancelled orders from result entry table
      if (order.status === 'CANCELLED') return false

      const matchesKw =
        !kw ||
        [
          order.orderCode,
          order.patientName,
          order.patientCode,
          order.diagnosis,
          order.doctorName,
        ].some((val) => String(val || '').toLowerCase().includes(kw))

      const matchesStatus = statusFilter === 'ALL' || order.status === statusFilter

      const matchesCategory =
        categoryFilter === 'ALL' ||
        order.items?.some((item) => {
          const cat = item.category || item.categoryName || ''
          if (categoryFilter === 'LABORATORY') return cat.includes('LAB') || cat.includes('Xét nghiệm')
          if (categoryFilter === 'IMAGING') return cat.includes('IMG') || cat.includes('hình ảnh')
          if (categoryFilter === 'FUNCTIONAL') return cat.includes('FUNC') || cat.includes('chức năng')
          return true
        })

      return matchesKw && matchesStatus && matchesCategory
    })
  }, [orders, searchText, statusFilter, categoryFilter])

  // Stats calculation
  const stats = useMemo(() => {
    const total = orders.filter((o) => o.status !== 'CANCELLED').length
    const pending = orders.filter((o) => o.status === 'PENDING').length
    const inProgress = orders.filter((o) => o.status === 'IN_PROGRESS').length
    const resulted = orders.filter((o) => o.status === 'RESULTED').length
    const confirmed = orders.filter((o) => o.status === 'CONFIRMED').length
    return { total, pending, inProgress, resulted, confirmed }
  }, [orders])

  const handleResetFilter = () => {
    setSearchText('')
    setStatusFilter('ALL')
    setCategoryFilter('ALL')
  }

  const handleOpenModal = (order) => {
    setSelectedOrder(order)
    setModalVisible(true)
  }

  const handleSaveResultSuccess = async (updatedOrder) => {
    // 1. Save locally to LocalStorage
    saveStoredClinicalOrder(updatedOrder)
    setOrders((prev) => prev.map((o) => (o.id === updatedOrder.id ? updatedOrder : o)))

    // 2. Đồng bộ chu trình: Khi Bác sĩ Xác nhận & Khóa (CONFIRMED/COMPLETED), chuyển lượt khám trong Hàng Đợi sang COMPLETED & bổ sung kết quả vào Hồ Sơ
    if (['CONFIRMED', 'COMPLETED'].includes(updatedOrder.status)) {
      try {
        const allQueues = getStoredQueueItems()
        allQueues.forEach((q) => {
          if (
            (updatedOrder.patientId && String(q.patientId) === String(updatedOrder.patientId)) ||
            (updatedOrder.patientName && q.patientName === updatedOrder.patientName)
          ) {
            if (['IN_PROGRESS', 'WAITING_FOR_RESULT', 'WAITING'].includes(q.status)) {
              const completedItem = { ...q, status: 'COMPLETED', completedAt: new Date().toISOString() }
              saveStoredQueueItem(completedItem)
              if (q.id && !String(q.id).startsWith('local') && !String(q.id).startsWith('qi-')) {
                queueApi.complete(q.id).catch(() => {})
              }
            }
          }
        })
        const allRecords = getStoredMedicalRecords()
        allRecords.forEach((r) => {
          if (
            (updatedOrder.patientId && String(r.patientId) === String(updatedOrder.patientId)) ||
            (updatedOrder.patientName && r.patientName === updatedOrder.patientName)
          ) {
            const updatedRec = {
              ...r,
              status: 'COMPLETED',
              clinicalResults: {
                ...(r.clinicalResults || {}),
                [updatedOrder.orderCode]: updatedOrder.conclusion || updatedOrder.resultSummary || 'Đã có kết quả CĐLS',
              },
            }
            saveStoredMedicalRecord(updatedRec)
          }
        })
      } catch (err) {
        console.warn('Sync order completion error:', err)
      }
    }

    // 3. Call RESTful API PUT /api/results/{id} if backend available
    try {
      await clinicalResultApi.update(updatedOrder.id, updatedOrder)
    } catch {
      // silent fallback
    }
  }

  return (
    <div style={{ maxWidth: 1600, margin: '0 auto', padding: '4px 0 24px' }}>
      {/* Header Section */}
      <div
        style={{
          display: 'flex',
          justify: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
          flexWrap: 'wrap',
          gap: 12,
        }}
      >
        <div>
          <Title level={3} style={{ margin: 0, color: '#0f172a', fontWeight: 700 }}>
            <FileDoneOutlined style={{ color: '#2563eb', marginRight: 8 }} />
            Nhập kết quả cận lâm sàng
          </Title>
          <Text style={{ color: '#64748b', fontSize: 14 }}>
            Quy trình nghiệp vụ Kỹ thuật viên nhập kết quả & Bác sĩ duyệt xác nhận khóa thông tin
          </Text>
        </div>

        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={loadData}
            style={{ borderRadius: 8, borderColor: '#cbd5e1' }}
          >
            Tải lại
          </Button>
        </Space>
      </div>

      {/* Summary Statistics Cards */}
      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={8} md={4} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #2563eb',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Tổng phiếu CĐLS</span>}
              value={stats.total}
              prefix={<ExperimentOutlined style={{ color: '#2563eb' }} />}
              valueStyle={{ fontWeight: 700, color: '#1e293b' }}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={4} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #096dd9',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Chờ thực hiện</span>}
              value={stats.pending}
              valueStyle={{ color: '#096dd9', fontWeight: 700 }}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={4} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #d46b08',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Đang thực hiện</span>}
              value={stats.inProgress}
              valueStyle={{ color: '#d46b08', fontWeight: 700 }}
              prefix={<SyncOutlined spin />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={6} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #722ed1',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Đã có kết quả</span>}
              value={stats.resulted}
              valueStyle={{ color: '#722ed1', fontWeight: 700 }}
              prefix={<FileDoneOutlined />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={6} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #16a34a',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Đã xác nhận & Khóa</span>}
              value={stats.confirmed}
              valueStyle={{ color: '#16a34a', fontWeight: 700 }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Toolbar: Search & Filter */}
      <Card
        size="small"
        style={{
          borderRadius: '12px',
          marginBottom: 16,
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
        }}
      >
        <Row gutter={[16, 12]} align="middle">
          <Col xs={24} md={12} lg={10}>
            <ResultSearch value={searchText} onChange={setSearchText} />
          </Col>

          <Col xs={24} md={12} lg={14} style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <ResultFilter
              status={statusFilter}
              onStatusChange={setStatusFilter}
              category={categoryFilter}
              onCategoryChange={setCategoryFilter}
              onReset={handleResetFilter}
            />
          </Col>
        </Row>
      </Card>

      {/* Table Component */}
      <ResultTable
        dataSource={filteredOrders}
        loading={loading}
        onOpenModal={handleOpenModal}
      />

      {/* Entry Modal */}
      <ResultModal
        visible={modalVisible}
        order={selectedOrder}
        onClose={() => setModalVisible(false)}
        onSaveSuccess={handleSaveResultSuccess}
      />
    </div>
  )
}

export default ResultPage
