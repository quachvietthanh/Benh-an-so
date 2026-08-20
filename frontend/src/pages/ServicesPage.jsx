import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  AppstoreOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  EditOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  DatePicker,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Radio,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat.js'
import systemApi from '../api/systemApi'
import { useAuthContext } from '../context/AuthContext'
import {
  categorizePriceHistory,
  fixMojibake,
  formatDateDisplay,
  formatServiceCurrency,
  prepareCreateServicePayload,
  prepareUpdateServicePayload,
  translateServiceErrorMessage,
} from '../utils/serviceCatalogValidation'
import '../styles/services.css'

dayjs.extend(customParseFormat)

const { Paragraph, Text, Title } = Typography
const { Option } = Select

function ServicesPage() {
  const { user } = useAuthContext()
  const [services, setServices] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [sortBy, setSortBy] = useState('DEFAULT')

  // Modal States
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [editingService, setEditingService] = useState(null)
  const [savingService, setSavingService] = useState(false)

  // Price History Drawer States
  const [historyDrawerOpen, setHistoryDrawerOpen] = useState(false)
  const [historyService, setHistoryService] = useState(null)
  const [priceHistory, setPriceHistory] = useState([])
  const [loadingHistory, setLoadingHistory] = useState(false)

  // Status Toggling State
  const [togglingId, setTogglingId] = useState(null)

  const [createForm] = Form.useForm()
  const [editForm] = Form.useForm()

  // User permission check
  const userRoles = (user?.roles || []).map((r) =>
    String(r || '')
      .toLowerCase()
      .replace(/^role_/, ''),
  )
  const canManage =
    userRoles.includes('admin') ||
    userRoles.includes('manager') ||
    userRoles.includes('clinic_manager')

  // Fetch Services from backend
  const loadServices = useCallback(async () => {
    setLoading(true)
    try {
      const res = await systemApi.services({ size: 200 })
      const data = res?.data
      const rawList = Array.isArray(data)
        ? data
        : Array.isArray(data?.content)
          ? data.content
          : []
      const list = rawList.map((s) => ({
        ...s,
        name: fixMojibake(s.name),
      }))
      setServices(list)
    } catch (err) {
      message.error(translateServiceErrorMessage(err))
      setServices([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadServices()
  }, [loadServices])

  // Statistics
  const stats = useMemo(() => {
    const total = services.length
    const active = services.filter((s) => s.active).length
    const inactive = total - active
    const activePrices = services
      .filter((s) => s.active && s.price !== null && s.price !== undefined)
      .map((s) => Number(s.price))
    const avgPrice =
      activePrices.length > 0
        ? Math.round(activePrices.reduce((acc, val) => acc + val, 0) / activePrices.length)
        : 0

    return { total, active, inactive, avgPrice }
  }, [services])

  // Filtered and Sorted Services
  const filteredServices = useMemo(() => {
    let result = [...services]

    // Filter by search keyword
    const keyword = searchTerm.trim().toLowerCase()
    if (keyword) {
      result = result.filter(
        (s) =>
          (s.serviceCode && s.serviceCode.toLowerCase().includes(keyword)) ||
          (s.name && s.name.toLowerCase().includes(keyword)),
      )
    }

    // Filter by status
    if (statusFilter === 'ACTIVE') {
      result = result.filter((s) => Boolean(s.active))
    } else if (statusFilter === 'INACTIVE') {
      result = result.filter((s) => !s.active)
    }

    // Sort
    if (sortBy === 'NAME_ASC') {
      result.sort((a, b) => (a.name || '').localeCompare(b.name || '', 'vi'))
    } else if (sortBy === 'NAME_DESC') {
      result.sort((a, b) => (b.name || '').localeCompare(a.name || '', 'vi'))
    } else if (sortBy === 'PRICE_ASC') {
      result.sort((a, b) => Number(a.price || 0) - Number(b.price || 0))
    } else if (sortBy === 'PRICE_DESC') {
      result.sort((a, b) => Number(b.price || 0) - Number(a.price || 0))
    } else if (sortBy === 'DATE_DESC') {
      result.sort((a, b) => (b.effectiveFrom || '').localeCompare(a.effectiveFrom || ''))
    }

    return result
  }, [services, searchTerm, statusFilter, sortBy])

  // Open Create Modal
  const handleOpenCreateModal = () => {
    createForm.resetFields()
    createForm.setFieldsValue({
      effectiveFrom: dayjs(),
      price: 0,
    })
    setCreateModalOpen(true)
  }

  // Submit Create Service
  const handleCreateService = async (values) => {
    setSavingService(true)
    try {
      const payload = prepareCreateServicePayload({
        ...values,
        effectiveFrom: values.effectiveFrom?.format?.('YYYY-MM-DD') || values.effectiveFrom,
      })
      await systemApi.createService(payload)
      message.success('Đã thêm dịch vụ và thiết lập bảng giá thành công!')
      setCreateModalOpen(false)
      createForm.resetFields()
      await loadServices()
    } catch (err) {
      message.error(translateServiceErrorMessage(err))
    } finally {
      setSavingService(false)
    }
  }

  // Open Edit Modal
  const handleOpenEditModal = (service) => {
    setEditingService(service)
    editForm.setFieldsValue({
      serviceCode: service.serviceCode,
      name: service.name,
      price: Number(service.price || 0),
      effectiveFrom: service.effectiveFrom ? dayjs(service.effectiveFrom) : dayjs(),
      active: Boolean(service.active),
    })
    setEditModalOpen(true)
  }

  // Submit Update Service
  const handleUpdateService = async (values) => {
    if (!editingService) return
    setSavingService(true)
    try {
      const payload = prepareUpdateServicePayload({
        ...values,
        effectiveFrom: values.effectiveFrom?.format?.('YYYY-MM-DD') || values.effectiveFrom,
      })
      await systemApi.updateService(editingService.id, payload)
      message.success('Cập nhật dịch vụ và bảng giá thành công!')
      setEditModalOpen(false)
      setEditingService(null)
      editForm.resetFields()
      await loadServices()
    } catch (err) {
      message.error(translateServiceErrorMessage(err))
    } finally {
      setSavingService(false)
    }
  }

  // Toggle Status via PATCH
  const handleToggleStatus = async (service, nextActive) => {
    setTogglingId(service.id)
    try {
      await systemApi.updateServiceStatus(service.id, nextActive)
      message.success(
        nextActive
          ? `Đã kích hoạt dịch vụ "${service.name}"`
          : `Đã ngừng áp dụng dịch vụ "${service.name}"`,
      )
      setServices((prev) =>
        prev.map((s) => (s.id === service.id ? { ...s, active: nextActive } : s)),
      )
    } catch (err) {
      message.error(translateServiceErrorMessage(err))
    } finally {
      setTogglingId(null)
    }
  }

  // Open Price History Drawer
  const handleOpenHistoryDrawer = async (service) => {
    setHistoryService(service)
    setHistoryDrawerOpen(true)
    setLoadingHistory(true)
    try {
      const res = await systemApi.getServicePriceHistory(service.id)
      const data = Array.isArray(res?.data) ? res.data : []
      setPriceHistory(categorizePriceHistory(data))
    } catch (err) {
      message.error(translateServiceErrorMessage(err))
      setPriceHistory([])
    } finally {
      setLoadingHistory(false)
    }
  }

  // Table Columns
  const columns = [
    {
      title: 'Mã dịch vụ',
      dataIndex: 'serviceCode',
      key: 'serviceCode',
      width: 140,
      render: (code) => <span className="service-code-badge">{code}</span>,
    },
    {
      title: 'Tên dịch vụ khám & cận lâm sàng',
      dataIndex: 'name',
      key: 'name',
      render: (name) => (
        <div className="service-name-cell">
          <span className="service-name-icon">
            <AppstoreOutlined />
          </span>
          <div>
            <Text strong style={{ fontSize: 13.5, color: '#1e293b' }}>
              {name}
            </Text>
          </div>
        </div>
      ),
    },
    {
      title: 'Đơn giá niêm yết',
      dataIndex: 'price',
      key: 'price',
      width: 170,
      align: 'right',
      render: (price) => (
        <span className="service-price-value">{formatServiceCurrency(price)}</span>
      ),
    },
    {
      title: 'Ngày hiệu lực',
      dataIndex: 'effectiveFrom',
      key: 'effectiveFrom',
      width: 150,
      align: 'center',
      render: (date) => {
        const isFuture = date && dayjs(date).isAfter(dayjs(), 'day')
        return (
          <div className="service-effective-cell">
            <span className="service-effective-date">{formatDateDisplay(date)}</span>
            {isFuture && (
              <Tag color="processing" style={{ fontSize: 10, margin: 0 }}>
                Sắp áp dụng
              </Tag>
            )}
          </div>
        )
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 160,
      align: 'center',
      render: (active, record) => (
        <Space size={8}>
          <Tag color={active ? 'success' : 'default'} style={{ margin: 0 }}>
            {active ? 'Đang hiệu lực' : 'Ngừng áp dụng'}
          </Tag>
          {canManage && (
            <Popconfirm
              title={active ? 'Ngừng áp dụng dịch vụ này?' : 'Kích hoạt lại dịch vụ này?'}
              description={
                active
                  ? 'Dịch vụ sẽ không thể được chọn khi bác sĩ chỉ định khám mới.'
                  : 'Dịch vụ sẽ hiển thị trở lại trong bảng giá và danh mục chỉ định.'
              }
              okText="Đồng ý"
              cancelText="Hủy"
              onConfirm={() => handleToggleStatus(record, !active)}
              disabled={togglingId === record.id}
            >
              <Switch
                size="small"
                checked={Boolean(active)}
                loading={togglingId === record.id}
              />
            </Popconfirm>
          )}
        </Space>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 160,
      align: 'center',
      render: (_, record) => (
        <div className="service-actions-group">
          {canManage && (
            <Tooltip title="Chỉnh sửa thông tin & cập nhật giá">
              <Button
                type="text"
                size="small"
                className="btn-action-edit"
                icon={<EditOutlined style={{ color: '#2563eb' }} />}
                onClick={() => handleOpenEditModal(record)}
              >
                Sửa
              </Button>
            </Tooltip>
          )}
          <Tooltip title="Xem lịch sử các phiên bản giá">
            <Button
              type="text"
              size="small"
              className="btn-action-history"
              icon={<HistoryOutlined style={{ color: '#475569' }} />}
              onClick={() => handleOpenHistoryDrawer(record)}
            >
              Lịch sử giá
            </Button>
          </Tooltip>
        </div>
      ),
    },
  ]

  return (
    <div className="service-management-container">
      {/* Header Info */}
      <div className="service-page-header">
        <div className="service-page-header-left">
          <span className="service-page-eyebrow">Hệ thống danh mục</span>
          <h2 className="service-page-title">
            Danh mục dịch vụ & Bảng giá hiệu lực
          </h2>
          <p className="service-page-desc">
            Quản lý tên dịch vụ, đơn giá niêm yết và thời điểm áp dụng giá phục vụ chỉ định khám và lập hóa đơn thu phí nhất quán.
          </p>
        </div>
        {canManage && (
          <div className="service-page-header-right">
            <Button
              type="primary"
              size="large"
              icon={<PlusOutlined />}
              onClick={handleOpenCreateModal}
              className="btn-create-service"
            >
              Thêm dịch vụ mới
            </Button>
          </div>
        )}
      </div>

      {/* Statistics Grid */}
      <div className="service-stats-grid">
        <div className="service-stat-card">
          <div className="service-stat-icon-wrapper icon-blue">
            <AppstoreOutlined />
          </div>
          <div className="service-stat-info">
            <span className="service-stat-label">Tổng số dịch vụ</span>
            <span className="service-stat-value">{stats.total}</span>
            <span className="service-stat-subtext">Danh mục hiện có</span>
          </div>
        </div>

        <div className="service-stat-card">
          <div className="service-stat-icon-wrapper icon-green">
            <CheckCircleOutlined />
          </div>
          <div className="service-stat-info">
            <span className="service-stat-label">Đang hiệu lực</span>
            <span className="service-stat-value text-green">
              {stats.active}
            </span>
            <span className="service-stat-subtext">Sẵn sàng chỉ định & thu phí</span>
          </div>
        </div>

        <div className="service-stat-card">
          <div className="service-stat-icon-wrapper icon-amber">
            <StopOutlined />
          </div>
          <div className="service-stat-info">
            <span className="service-stat-label">Tạm ngừng áp dụng</span>
            <span className="service-stat-value text-amber">
              {stats.inactive}
            </span>
            <span className="service-stat-subtext">Đã khóa khỏi bảng giá</span>
          </div>
        </div>

        <div className="service-stat-card">
          <div className="service-stat-icon-wrapper icon-purple">
            <DollarOutlined />
          </div>
          <div className="service-stat-info">
            <span className="service-stat-label">Đơn giá trung bình</span>
            <span className="service-stat-value text-purple" style={{ fontSize: 18 }}>
              {formatServiceCurrency(stats.avgPrice)}
            </span>
            <span className="service-stat-subtext">Dịch vụ đang hoạt động</span>
          </div>
        </div>
      </div>

      {/* Main Table Container */}
      <div className="service-table-container">
        {/* Toolbar */}
        <div className="service-table-toolbar">
          <div className="toolbar-left">
            <Input
              className="toolbar-search-input"
              allowClear
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              placeholder="Tìm theo mã hoặc tên dịch vụ..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <Select
              className="toolbar-filter-select"
              style={{ minWidth: 160 }}
              value={statusFilter}
              onChange={setStatusFilter}
              placeholder="Lọc trạng thái"
            >
              <Option value="ALL">Tất cả trạng thái</Option>
              <Option value="ACTIVE">Đang hiệu lực</Option>
              <Option value="INACTIVE">Ngừng áp dụng</Option>
            </Select>
            <Select
              className="toolbar-sort-select"
              style={{ minWidth: 190 }}
              value={sortBy}
              onChange={setSortBy}
              placeholder="Sắp xếp"
            >
              <Option value="DEFAULT">Mặc định hệ thống</Option>
              <Option value="NAME_ASC">Tên dịch vụ (A - Z)</Option>
              <Option value="NAME_DESC">Tên dịch vụ (Z - A)</Option>
              <Option value="PRICE_ASC">Đơn giá (Thấp → Cao)</Option>
              <Option value="PRICE_DESC">Đơn giá (Cao → Thấp)</Option>
              <Option value="DATE_DESC">Ngày hiệu lực (Mới nhất)</Option>
            </Select>
          </div>
          <div className="toolbar-right">
            <Button icon={<ReloadOutlined />} loading={loading} onClick={loadServices} className="btn-refresh">
              Làm mới
            </Button>
          </div>
        </div>

        {/* Table */}
        <Table
          className="services-table"
          rowKey="id"
          loading={loading}
          dataSource={filteredServices}
          columns={columns}
          scroll={{ x: 900 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            showTotal: (total, range) =>
              `Hiển thị ${range[0]} - ${range[1]} trên tổng ${total} dịch vụ`,
          }}
          locale={{
            emptyText: (
              <Empty
                className="service-empty-copy"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  searchTerm || statusFilter !== 'ALL'
                    ? 'Không tìm thấy dịch vụ phù hợp với bộ lọc'
                    : 'Chưa có dịch vụ nào trong danh mục'
                }
              />
            ),
          }}
        />
      </div>

      {/* Modal: Thêm dịch vụ mới */}
      <Modal
        className="service-form-modal"
        width={620}
        title={
          <div className="service-modal-title">
            <span className="service-name-icon">
              <PlusOutlined />
            </span>
            <div>
              <Title level={4} style={{ margin: 0 }}>Thêm dịch vụ khám & Bảng giá</Title>
              <Text type="secondary" style={{ fontSize: 12 }}>
                Nhập mã định danh, tên dịch vụ và mức giá áp dụng ban đầu.
              </Text>
            </div>
          </div>
        }
        open={createModalOpen}
        footer={null}
        centered
        destroyOnClose
        maskClosable={!savingService}
        closable={!savingService}
        onCancel={() => setCreateModalOpen(false)}
      >
        <Form
          className="service-form"
          form={createForm}
          layout="vertical"
          onFinish={handleCreateService}
        >
          <div className="service-form-grid">
            <Form.Item
              name="serviceCode"
              label="Mã dịch vụ"
              rules={[
                { required: true, message: 'Vui lòng nhập mã dịch vụ' },
                { max: 50, message: 'Mã không quá 50 ký tự' },
              ]}
              extra="Ví dụ: DV_KHAM_NOI, DV_SIEU_AM, DV_XQUANG..."
            >
              <Input
                size="large"
                placeholder="Nhập mã dịch vụ..."
                onChange={(e) => {
                  createForm.setFieldValue(
                    'serviceCode',
                    e.target.value.toUpperCase().replace(/\s+/g, '_'),
                  )
                }}
              />
            </Form.Item>

            <Form.Item
              name="name"
              label="Tên dịch vụ"
              rules={[
                { required: true, message: 'Vui lòng nhập tên dịch vụ' },
                { max: 255, message: 'Tên không quá 255 ký tự' },
              ]}
            >
              <Input size="large" placeholder="Ví dụ: Khám nội tổng quát" />
            </Form.Item>

            <Form.Item
              name="price"
              label="Đơn giá niêm yết"
              rules={[{ required: true, message: 'Vui lòng nhập đơn giá dịch vụ' }]}
            >
              <InputNumber
                size="large"
                min={0}
                step={10000}
                controls={false}
                formatter={(val) => `${val || ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                parser={(val) => val?.replace(/\./g, '') || ''}
                addonAfter="₫"
                placeholder="0"
                style={{ width: '100%' }}
              />
            </Form.Item>

            <Form.Item
              name="effectiveFrom"
              label="Ngày bắt đầu hiệu lực"
              rules={[{ required: true, message: 'Vui lòng chọn ngày hiệu lực' }]}
            >
              <DatePicker
                size="large"
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                placeholder="Chọn ngày áp dụng"
              />
            </Form.Item>
          </div>

          <div className="service-modal-hint" style={{ marginTop: 8 }}>
            <InfoCircleOutlined />
            <span>
              Đơn giá này sẽ tự động có hiệu lực kể từ ngày được chỉ định và áp dụng trực tiếp khi lập hóa đơn thu phí cho bệnh nhân.
            </span>
          </div>

          <div className="service-modal-actions">
            <Button size="large" onClick={() => setCreateModalOpen(false)} disabled={savingService}>
              Hủy bỏ
            </Button>
            <Button
              size="large"
              type="primary"
              htmlType="submit"
              loading={savingService}
              style={{ background: '#2563eb' }}
            >
              Lưu dịch vụ
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Modal: Sửa thông tin & Điều chỉnh bảng giá */}
      <Modal
        className="service-form-modal"
        width={620}
        title={
          <div className="service-modal-title">
            <span className="service-name-icon">
              <EditOutlined />
            </span>
            <div>
              <Title level={4} style={{ margin: 0 }}>Cập nhật dịch vụ & Điều chỉnh giá</Title>
              <Text type="secondary" style={{ fontSize: 12 }}>
                Điều chỉnh tên dịch vụ, đơn giá áp dụng hoặc ngày hiệu lực mới.
              </Text>
            </div>
          </div>
        }
        open={editModalOpen}
        footer={null}
        centered
        destroyOnClose
        maskClosable={!savingService}
        closable={!savingService}
        onCancel={() => {
          setEditModalOpen(false)
          setEditingService(null)
        }}
      >
        <Form
          className="service-form"
          form={editForm}
          layout="vertical"
          onFinish={handleUpdateService}
        >
          <div className="service-form-grid">
            <Form.Item name="serviceCode" label="Mã dịch vụ">
              <Input size="large" disabled style={{ background: '#f8fafc', color: '#0f172a', fontWeight: 600 }} />
            </Form.Item>

            <Form.Item
              name="name"
              label="Tên dịch vụ"
              rules={[
                { required: true, message: 'Vui lòng nhập tên dịch vụ' },
                { max: 255, message: 'Tên không quá 255 ký tự' },
              ]}
            >
              <Input size="large" placeholder="Nhập tên dịch vụ" />
            </Form.Item>

            <Form.Item
              name="price"
              label="Đơn giá niêm yết"
              rules={[{ required: true, message: 'Vui lòng nhập đơn giá' }]}
            >
              <InputNumber
                size="large"
                min={0}
                step={10000}
                controls={false}
                formatter={(val) => `${val || ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                parser={(val) => val?.replace(/\./g, '') || ''}
                addonAfter="₫"
                placeholder="0"
                style={{ width: '100%' }}
              />
            </Form.Item>

            <Form.Item
              name="effectiveFrom"
              label="Ngày hiệu lực giá"
              rules={[{ required: true, message: 'Vui lòng chọn ngày hiệu lực' }]}
            >
              <DatePicker
                size="large"
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                placeholder="Chọn ngày áp dụng"
              />
            </Form.Item>

            <Form.Item className="service-form-full" name="active" label="Trạng thái áp dụng">
              <Radio.Group size="large" buttonStyle="solid">
                <Radio.Button value={true}>
                  <CheckCircleOutlined style={{ color: '#10b981', marginRight: 6 }} />
                  Đang hiệu lực
                </Radio.Button>
                <Radio.Button value={false}>
                  <StopOutlined style={{ color: '#ef4444', marginRight: 6 }} />
                  Ngừng áp dụng
                </Radio.Button>
              </Radio.Group>
            </Form.Item>
          </div>

          <div className="service-modal-hint" style={{ marginTop: 8 }}>
            <InfoCircleOutlined />
            <span>
              Khi cập nhật đơn giá kèm ngày hiệu lực mới, hệ thống sẽ tự động lưu phiên bản giá mới vào lịch sử giá mà không ảnh hưởng đến các hóa đơn đã lập trước đó.
            </span>
          </div>

          <div className="service-modal-actions">
            <Button
              size="large"
              onClick={() => {
                setEditModalOpen(false)
                setEditingService(null)
              }}
              disabled={savingService}
            >
              Hủy bỏ
            </Button>
            <Button
              size="large"
              type="primary"
              htmlType="submit"
              loading={savingService}
              style={{ background: '#2563eb' }}
            >
              Lưu thay đổi
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Drawer: Lịch sử giá dịch vụ */}
      <Drawer
        className="price-history-drawer"
        title={
          <div>
            <Title level={5} style={{ margin: 0, color: '#0f172a' }}>
              <HistoryOutlined style={{ marginRight: 8, color: '#2563eb' }} />
              Lịch sử các phiên bản giá
            </Title>
            {historyService && (
              <div className="price-history-header-meta">
                <div>
                  <span className="service-code-badge" style={{ marginRight: 8 }}>
                    {historyService.serviceCode}
                  </span>
                  <Text strong>{historyService.name}</Text>
                </div>
                <Tag color={historyService.active ? 'success' : 'default'}>
                  {historyService.active ? 'Đang hiệu lực' : 'Ngừng áp dụng'}
                </Tag>
              </div>
            )}
          </div>
        }
        placement="right"
        width={480}
        open={historyDrawerOpen}
        onClose={() => {
          setHistoryDrawerOpen(false)
          setHistoryService(null)
          setPriceHistory([])
        }}
      >
        {loadingHistory ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <ReloadOutlined spin style={{ fontSize: 24, color: '#2563eb' }} />
            <div style={{ marginTop: 12, color: '#64748b' }}>Đang tải lịch sử giá...</div>
          </div>
        ) : priceHistory.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Chưa có dữ liệu lịch sử giá cho dịch vụ này"
          />
        ) : (
          <div>
            <Alert
              className="service-notice-banner"
              type="info"
              showIcon
              message="Nguyên tắc áp dụng giá"
              description="Hóa đơn thu viện phí sẽ tự động tra cứu và áp dụng mức giá có ngày hiệu lực gần nhất tính đến thời điểm lập chỉ định khám."
            />

            <div style={{ marginTop: 16 }}>
              {priceHistory.map((item, index) => {
                const isCurrent = item.priceStatus === 'CURRENT'
                const isUpcoming = item.priceStatus === 'UPCOMING'
                const cardClass = isCurrent
                  ? 'price-history-card-item is-current'
                  : isUpcoming
                    ? 'price-history-card-item is-upcoming'
                    : 'price-history-card-item'

                return (
                  <div key={item.id || index} className={cardClass}>
                    <div className="price-history-card-top">
                      <span
                        className={`price-history-amount ${
                          isCurrent ? 'is-current' : isUpcoming ? 'is-upcoming' : ''
                        }`}
                      >
                        {item.formattedPrice}
                      </span>
                      <Tag color={item.statusColor}>{item.statusLabel}</Tag>
                    </div>
                    <div className="price-history-card-details">
                      <div className="price-history-detail-row">
                        <CalendarOutlined style={{ color: '#94a3b8' }} />
                        <span>
                          <strong>Ngày bắt đầu hiệu lực:</strong> {item.formattedEffectiveFrom}
                        </span>
                      </div>
                      {item.formattedCreatedAt && item.formattedCreatedAt !== '—' && (
                        <div className="price-history-detail-row">
                          <ClockCircleOutlined style={{ color: '#94a3b8' }} />
                          <span>
                            <strong>Thời điểm thiết lập:</strong> {item.formattedCreatedAt}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </Drawer>
    </div>
  )
}

export default ServicesPage
