import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Dropdown,
  Form,
  Input,
  Modal,
  Radio,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  ArrowRightOutlined,
  CheckCircleOutlined,
  ClearOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EllipsisOutlined,
  ExperimentOutlined,
  FileDoneOutlined,
  ReloadOutlined,
  SearchOutlined,
  SolutionOutlined,
  UserOutlined,
} from '@ant-design/icons'

import clinicalOrderApi from '../api/clinicalOrderApi.js'
import userApi from '../api/userApi.js'
import { useAuthContext } from '../context/AuthContext'
import {
  PRESET_CANCEL_REASONS,
  cleanClinicalOrderErrorMessage,
  formatServiceTypeVi,
  getServiceTypeColor,
  getWaitingDurationMeta,
  validateCancelReason,
} from '../utils/clinicalOrderHelpers.js'

const { Title, Text, Paragraph } = Typography
const { RangePicker } = DatePicker

function PendingClinicalOrdersPage() {
  const navigate = useNavigate()
  const { user } = useAuthContext()

  const [loading, setLoading] = useState(false)
  const [orders, setOrders] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)

  // Filters
  const [searchText, setSearchText] = useState('')
  const [serviceTypeFilter, setServiceTypeFilter] = useState('ALL')
  const [selectedDoctorId, setSelectedDoctorId] = useState('ALL')
  const [dateRange, setDateRange] = useState(null)
  const [doctorsList, setDoctorsList] = useState([])

  // Cancel Modal state
  const [cancelModalOpen, setCancelModalOpen] = useState(false)
  const [selectedItemToCancel, setSelectedItemToCancel] = useState(null)
  const [cancelLoading, setCancelLoading] = useState(false)
  const [cancelForm] = Form.useForm()

  const userRoles = useMemo(() => {
    return (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  }, [user?.roles])

  const isAdmin = userRoles.includes('admin') || userRoles.includes('manager') || userRoles.includes('clinic_manager')
  const isDoctor = userRoles.includes('doctor')

  // Load doctors list for admin filter
  useEffect(() => {
    if (isAdmin) {
      userApi.getAllDoctors?.()
        .then((res) => {
          const list = Array.isArray(res?.data) ? res.data : (res?.data?.content || [])
          setDoctorsList(list)
        })
        .catch(() => {
          // Fallback if endpoint not available
          setDoctorsList([])
        })
    }
  }, [isAdmin])

  // Fetch pending orders from API
  const fetchPendingOrders = useCallback(async () => {
    setLoading(true)
    try {
      const params = {
        page,
        size: pageSize,
      }

      if (selectedDoctorId && selectedDoctorId !== 'ALL') {
        params.doctorId = selectedDoctorId
      }

      if (dateRange && dateRange[0] && dateRange[1]) {
        params.fromDate = dateRange[0].startOf('day').toISOString()
        params.toDate = dateRange[1].endOf('day').toISOString()
      }

      const res = await clinicalOrderApi.getPendingOrders(params)
      const data = res?.data
      const content = Array.isArray(data) ? data : (data?.content || [])
      setOrders(content)
      setTotalElements(Number(data?.totalElements) || content.length)
    } catch (err) {
      const msg = cleanClinicalOrderErrorMessage(err, 'Không thể tải danh sách chỉ định chờ kết quả.')
      message.error(msg)
      setOrders([])
      setTotalElements(0)
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, selectedDoctorId, dateRange])

  useEffect(() => {
    fetchPendingOrders()
  }, [fetchPendingOrders])

  // Filter local search and service type
  const filteredOrders = useMemo(() => {
    const kw = searchText.trim().toLowerCase()
    return orders.filter((item) => {
      // Service type filter
      if (serviceTypeFilter !== 'ALL') {
        const itemType = String(item.serviceType || '').toUpperCase()
        if (serviceTypeFilter === 'LAB_TEST' && itemType !== 'LAB_TEST') return false
        if (serviceTypeFilter === 'IMAGING' && itemType !== 'IMAGING') return false
        if (serviceTypeFilter === 'OTHER' && (itemType === 'LAB_TEST' || itemType === 'IMAGING')) return false
      }

      // Keyword search
      if (!kw) return true
      return [
        item.orderCode,
        item.visitCode,
        item.patientCode,
        item.patientFullName,
        item.doctorFullName,
        item.serviceCode,
        item.serviceName,
        item.clinicalReason,
      ].some((val) => String(val || '').toLowerCase().includes(kw))
    })
  }, [orders, searchText, serviceTypeFilter])

  // KPI Statistics
  const stats = useMemo(() => {
    const total = orders.length
    let over30 = 0
    let over60 = 0
    let todayCount = 0
    const now = dayjs()

    orders.forEach((item) => {
      const waitMin = Number(item.waitingMinutes) || 0
      if (waitMin >= 60) over60++
      else if (waitMin >= 30) over30++

      if (item.orderedAt && dayjs(item.orderedAt).isSame(now, 'day')) {
        todayCount++
      }
    })

    return { total, over30, over60, todayCount }
  }, [orders])

  const handleResetFilters = () => {
    setSearchText('')
    setServiceTypeFilter('ALL')
    setSelectedDoctorId('ALL')
    setDateRange(null)
    setPage(0)
  }

  // Open Cancel Modal
  const handleOpenCancelModal = (record) => {
    setSelectedItemToCancel(record)
    cancelForm.setFieldsValue({
      quickReason: PRESET_CANCEL_REASONS[0],
      customReason: PRESET_CANCEL_REASONS[0],
    })
    setCancelModalOpen(true)
  }

  const handleQuickReasonChange = (e) => {
    const val = e.target.value
    if (val !== 'Khác') {
      cancelForm.setFieldsValue({ customReason: val })
    } else {
      cancelForm.setFieldsValue({ customReason: '' })
    }
  }

  const handleConfirmCancel = async (values) => {
    if (!selectedItemToCancel?.orderItemId) return

    const validation = validateCancelReason(values.customReason || values.quickReason || '')
    if (!validation.valid) {
      message.error(validation.error)
      return
    }

    setCancelLoading(true)
    try {
      await clinicalOrderApi.cancelOrderItem(selectedItemToCancel.orderItemId, validation.reason)
      message.success(`Đã hủy chỉ định dịch vụ "${selectedItemToCancel.serviceName}" thành công!`)
      setCancelModalOpen(false)
      cancelForm.resetFields()
      setSelectedItemToCancel(null)
      fetchPendingOrders()
    } catch (err) {
      const errorMsg = cleanClinicalOrderErrorMessage(err, 'Không thể hủy chỉ định cận lâm sàng.')
      message.error(errorMsg)
    } finally {
      setCancelLoading(false)
    }
  }

  const columns = [
    {
      title: 'Thời gian chỉ định',
      dataIndex: 'orderedAt',
      key: 'orderedAt',
      width: 140,
      render: (val, record) => {
        const meta = getWaitingDurationMeta(record.waitingMinutes, record.orderedAt)
        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Text style={{ fontSize: 13, fontWeight: 600, color: '#0f172a' }}>
              {val ? dayjs(val).format('HH:mm:ss') : '—'}
            </Text>
            <Text type="secondary" style={{ fontSize: 11 }}>
              {val ? dayjs(val).format('DD/MM/YYYY') : ''}
            </Text>
            <Tag
              color={meta.tagColor || meta.color}
              icon={<ClockCircleOutlined />}
              style={{ fontWeight: 600, fontSize: 11, marginTop: 2, alignSelf: 'flex-start', borderRadius: 4, margin: 0, padding: '0 4px' }}
            >
              {meta.tagText}
            </Tag>
          </div>
        )
      },
    },
    {
      title: 'Mã chỉ định & Lượt khám',
      key: 'codes',
      width: 155,
      render: (_, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <div>
            <span style={{ fontSize: 11, color: '#64748b', display: 'block' }}>Mã CĐ:</span>
            <Tooltip title={`Mã phiếu chỉ định: ${record.orderCode}`}>
              <Tag
                color="blue"
                style={{
                  fontFamily: 'monospace',
                  fontWeight: 600,
                  fontSize: 11,
                  maxWidth: 130,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  display: 'inline-block',
                  verticalAlign: 'bottom',
                  borderRadius: 4,
                  margin: 0,
                }}
              >
                {record.orderCode || 'ORD-—'}
              </Tag>
            </Tooltip>
          </div>
          <div>
            <span style={{ fontSize: 11, color: '#64748b', display: 'block' }}>Mã lượt:</span>
            <Tag color="geekblue" style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: 11, borderRadius: 4, margin: 0, padding: '0 4px' }}>
              {record.visitCode || 'VIS-—'}
            </Tag>
          </div>
        </div>
      ),
    },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      width: 150,
      render: (_, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <UserOutlined style={{ color: '#2563eb', fontSize: 13, flexShrink: 0 }} />
            <Text strong style={{ fontSize: 13, color: '#0f172a' }} ellipsis title={record.patientFullName}>
              {record.patientFullName || 'Bệnh nhân'}
            </Text>
          </div>
          <div style={{ fontSize: 11, color: '#64748b' }}>
            Mã BN: <Tag color="default" style={{ fontFamily: 'monospace', fontSize: 11, borderRadius: 3, margin: 0, padding: '0 3px' }}>{record.patientCode || '—'}</Tag>
          </div>
        </div>
      ),
    },
    {
      title: 'Bác sĩ chỉ định',
      dataIndex: 'doctorFullName',
      key: 'doctorFullName',
      width: 140,
      render: (val) => (
        <Text style={{ fontWeight: 500, color: '#1e293b', fontSize: 13 }} ellipsis title={val || 'BS. Phụ trách'}>
          {val || 'BS. Phụ trách'}
        </Text>
      ),
    },
    {
      title: 'Dịch vụ CĐLS',
      key: 'service',
      width: 180,
      render: (_, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <Text strong style={{ fontSize: 13, color: '#0f172a' }} ellipsis title={record.serviceName}>
            {record.serviceName}
          </Text>
          <Space size={4} wrap>
            <Tag color="default" style={{ fontFamily: 'monospace', fontSize: 10, borderRadius: 3, margin: 0, padding: '0 3px' }}>
              {record.serviceCode}
            </Tag>
            <Tag color={getServiceTypeColor(record.serviceType)} style={{ fontWeight: 600, fontSize: 10, borderRadius: 3, margin: 0, padding: '0 3px' }}>
              {formatServiceTypeVi(record.serviceType)}
            </Tag>
          </Space>
        </div>
      ),
    },
    {
      title: 'Lý do & Hướng dẫn',
      key: 'reason',
      width: 140,
      ellipsis: true,
      render: (_, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {record.clinicalReason && (
            <Text style={{ fontSize: 11 }} ellipsis title={record.clinicalReason}>
              {record.clinicalReason}
            </Text>
          )}
          {record.instruction && (
            <Text type="secondary" style={{ fontSize: 11 }} ellipsis title={record.instruction}>
              {record.instruction}
            </Text>
          )}
          {!record.clinicalReason && !record.instruction && (
            <Text type="secondary" style={{ fontSize: 11 }}>—</Text>
          )}
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 110,
      align: 'center',
      render: () => (
        <Tag color="warning" icon={<ClockCircleOutlined />} style={{ fontWeight: 600, fontSize: 11, padding: '2px 6px', borderRadius: 4 }}>
          Chờ kết quả
        </Tag>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 75,
      align: 'center',
      render: (_, record) => {
        const actionMenuItems = [
          {
            key: 'medical-record',
            icon: <SolutionOutlined style={{ color: '#2563eb', fontSize: 16 }} />,
            label: <span style={{ fontSize: 14, fontWeight: 500, color: '#1e293b' }}>Xem bệnh án lượt khám</span>,
            style: { padding: '10px 16px', borderRadius: 6, minHeight: 40, display: 'flex', alignItems: 'center' },
            onClick: () => navigate(`/medical-records/visits/${record.visitId}`),
          },
          {
            key: 'results',
            icon: <FileDoneOutlined style={{ color: '#16a34a', fontSize: 16 }} />,
            label: <span style={{ fontSize: 14, fontWeight: 500, color: '#1e293b' }}>Nhập / Xem kết quả CLS</span>,
            style: { padding: '10px 16px', borderRadius: 6, minHeight: 40, display: 'flex', alignItems: 'center' },
            onClick: () => navigate(`/clinical-results?orderItemId=${record.orderItemId}&visitId=${record.visitId}`),
          },
          {
            type: 'divider',
            style: { margin: '4px 0' },
          },
          {
            key: 'cancel',
            icon: <DeleteOutlined style={{ fontSize: 16 }} />,
            label: <span style={{ fontSize: 14, fontWeight: 500 }}>Hủy chỉ định cận lâm sàng</span>,
            style: { padding: '10px 16px', borderRadius: 6, minHeight: 40, display: 'flex', alignItems: 'center' },
            danger: true,
            onClick: () => handleOpenCancelModal(record),
          },
        ]

        return (
          <Dropdown
            menu={{
              items: actionMenuItems,
              style: {
                minWidth: 235,
                padding: '6px',
                borderRadius: 10,
                boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.12), 0 8px 10px -6px rgba(0, 0, 0, 0.08)',
              },
            }}
            trigger={['click']}
            placement="bottomRight"
          >
            <Tooltip title="Thao tác">
              <Button
                type="text"
                icon={<EllipsisOutlined style={{ fontSize: 22, color: '#334155' }} />}
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: 8,
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '1px solid #cbd5e1',
                  background: '#f8fafc',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
              />
            </Tooltip>
          </Dropdown>
        )
      },
    },
  ]

  return (
    <div style={{ maxWidth: 1600, margin: '0 auto', paddingBottom: 32 }}>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
          flexWrap: 'wrap',
          gap: 12,
        }}
      >
        <div>
          <Title level={3} style={{ margin: 0, color: '#0f172a', fontWeight: 700 }}>
            <ExperimentOutlined style={{ color: '#2563eb', marginRight: 8 }} />
            Theo dõi chỉ định cận lâm sàng chờ kết quả
          </Title>
        </div>

        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={fetchPendingOrders}
            loading={loading}
            style={{ borderRadius: 8, borderColor: '#cbd5e1' }}
          >
            Làm mới
          </Button>
        </Space>
      </div>

      {/* KPI Cards */}
      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={6}>
          <Card
            size="small"
            style={{
              borderRadius: 10,
              borderLeft: '4px solid #2563eb',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Tổng chỉ định chờ</span>}
              value={stats.total}
              prefix={<ExperimentOutlined style={{ color: '#2563eb' }} />}
              valueStyle={{ fontWeight: 700, color: '#1e293b' }}
            />
          </Card>
        </Col>

        <Col xs={12} sm={6}>
          <Card
            size="small"
            style={{
              borderRadius: 10,
              borderLeft: '4px solid #f59e0b',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Chờ 30 - 60 phút</span>}
              value={stats.over30}
              valueStyle={{ color: '#d97706', fontWeight: 700 }}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={6}>
          <Card
            size="small"
            style={{
              borderRadius: 10,
              borderLeft: '4px solid #ef4444',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Chờ quá 60 phút (Gấp)</span>}
              value={stats.over60}
              valueStyle={{ color: '#dc2626', fontWeight: 700 }}
              prefix={<AlertOutlined />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={6}>
          <Card
            size="small"
            style={{
              borderRadius: 10,
              borderLeft: '4px solid #16a34a',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Chỉ định trong ngày</span>}
              value={stats.todayCount}
              valueStyle={{ color: '#16a34a', fontWeight: 700 }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Filter Bar */}
      <Card
        size="small"
        style={{
          borderRadius: 10,
          marginBottom: 16,
          border: '1px solid #e2e8f0',
          background: '#f8fafc',
        }}
      >
        <Row gutter={[12, 12]} align="middle">
          <Col xs={24} sm={12} md={7}>
            <Input
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              placeholder="Tìm theo tên BN, mã BN, mã chỉ định, dịch vụ..."
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              allowClear
            />
          </Col>

          <Col xs={12} sm={6} md={5}>
            <Select
              style={{ width: '100%' }}
              value={serviceTypeFilter}
              onChange={setServiceTypeFilter}
              options={[
                { value: 'ALL', label: 'Tất cả loại dịch vụ' },
                { value: 'LAB_TEST', label: 'Xét nghiệm (LAB)' },
                { value: 'IMAGING', label: 'Chẩn đoán hình ảnh (IMG)' },
                { value: 'OTHER', label: 'Thăm dò chức năng (FUNC)' },
              ]}
            />
          </Col>

          {isAdmin && (
            <Col xs={12} sm={6} md={5}>
              <Select
                style={{ width: '100%' }}
                value={selectedDoctorId}
                onChange={setSelectedDoctorId}
                placeholder="Lọc theo bác sĩ"
                options={[
                  { value: 'ALL', label: 'Tất cả bác sĩ' },
                  ...doctorsList.map((d) => ({
                    value: d.id,
                    label: d.fullName || d.username,
                  })),
                ]}
              />
            </Col>
          )}

          <Col xs={24} sm={12} md={isAdmin ? 5 : 9}>
            <RangePicker
              style={{ width: '100%' }}
              placeholder={['Từ ngày', 'Đến ngày']}
              value={dateRange}
              onChange={setDateRange}
              format="DD/MM/YYYY"
            />
          </Col>

          <Col xs={24} sm={12} md={isAdmin ? 2 : 3} style={{ textAlign: 'right' }}>
            <Button icon={<ClearOutlined />} onClick={handleResetFilters}>
              Đặt lại
            </Button>
          </Col>
        </Row>
      </Card>

      {/* Table */}
      <Table
        rowKey={(record) => record.orderItemId || record.id}
        columns={columns}
        dataSource={filteredOrders}
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total: totalElements,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50'],
          onChange: (newPage, newPageSize) => {
            setPage(newPage - 1)
            setPageSize(newPageSize)
          },
          showTotal: (total) => `Tổng cộng: ${total} chỉ định chờ`,
        }}
        bordered
        size="middle"
      />

      {/* Modal Hủy Chỉ Định (TC-02, TC-04, QTN-13) */}
      <Modal
        open={cancelModalOpen}
        title={
          <Space align="center">
            <CloseCircleOutlined style={{ color: '#dc2626', fontSize: 20 }} />
            <span>Xác nhận hủy chỉ định cận lâm sàng (TC-02)</span>
          </Space>
        }
        okText="Xác nhận hủy chỉ định"
        cancelText="Bỏ qua"
        okButtonProps={{ danger: true, loading: cancelLoading }}
        cancelButtonProps={{ disabled: cancelLoading }}
        onOk={() => cancelForm.submit()}
        onCancel={() => {
          setCancelModalOpen(false)
          cancelForm.resetFields()
        }}
        destroyOnClose
        width={560}
      >
        <div style={{ marginTop: 12 }}>
          <Alert
            type="warning"
            showIcon
            message="Lưu ý khi hủy dịch vụ cận lâm sàng"
            description="Chỉ định đã hủy sẽ rời khỏi danh sách chờ và được lưu vết nhật ký cùng lý do. Nếu dịch vụ đã được phòng xét nghiệm nhập kết quả, hệ thống sẽ từ chối hủy theo quy tắc QTN-13."
            style={{ marginBottom: 16 }}
          />

          {selectedItemToCancel && (
            <div
              style={{
                background: '#f8fafc',
                padding: '12px 16px',
                borderRadius: 8,
                marginBottom: 16,
                border: '1px solid #e2e8f0',
              }}
            >
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Text strong>{selectedItemToCancel.serviceName}</Text>
                  <Tag color="blue">{selectedItemToCancel.serviceCode}</Tag>
                </div>
                <Text type="secondary" style={{ fontSize: 13 }}>
                  Bệnh nhân: <Text strong>{selectedItemToCancel.patientFullName}</Text> ({selectedItemToCancel.patientCode})
                </Text>
                <Text type="secondary" style={{ fontSize: 13 }}>
                  Mã chỉ định: <Text code>{selectedItemToCancel.orderCode}</Text> • Mã lượt khám: <Text code>{selectedItemToCancel.visitCode}</Text>
                </Text>
              </Space>
            </div>
          )}

          <Form form={cancelForm} layout="vertical" onFinish={handleConfirmCancel}>
            <Form.Item label="Lý do hủy thường gặp" name="quickReason">
              <Radio.Group onChange={handleQuickReasonChange} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {PRESET_CANCEL_REASONS.map((r) => (
                  <Radio key={r} value={r}>
                    {r}
                  </Radio>
                ))}
              </Radio.Group>
            </Form.Item>

            <Form.Item
              label="Chi tiết lý do hủy chỉ định"
              name="customReason"
              rules={[{ required: true, message: 'Vui lòng nhập lý do hủy chỉ định cận lâm sàng.' }]}
            >
              <Input.TextArea
                rows={3}
                placeholder="Nhập lý do cụ thể vì sao hủy dịch vụ này..."
                maxLength={500}
                showCount
              />
            </Form.Item>
          </Form>
        </div>
      </Modal>
    </div>
  )
}

export default PendingClinicalOrdersPage
