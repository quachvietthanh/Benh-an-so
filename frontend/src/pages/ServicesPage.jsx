import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  AppstoreOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  EditOutlined,
  EnvironmentOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Badge,
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Radio,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tabs,
  Tag,
  Timeline,
  TimePicker,
  Tooltip,
  Typography,
} from 'antd'
import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import systemApi from '../api/systemApi'
import { useAuthContext } from '../context/AuthContext'
import {
  calculateServiceStats,
  categorizePriceHistory,
  checkServiceManagementPermission,
  formatVND,
  normalizeServiceList,
  validateCreateServicePayload,
  validateUpdateServicePayload,
} from '../utils/serviceCatalogHelpers'

dayjs.extend(customParseFormat)

const { Paragraph, Text, Title } = Typography

function ServicesPage() {
  const { user } = useAuthContext()
  const canManage = checkServiceManagementPermission(user?.roles)
  const isAdmin = (user?.roles || []).some(
    (r) => String(r || '').toLowerCase().replace(/^role_/, '') === 'admin'
  )

  const [services, setServices] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')

  // Modals state
  const [modalType, setModalType] = useState(null) // 'create' | 'edit' | null
  const [editingService, setEditingService] = useState(null)
  const [savingService, setSavingService] = useState(false)
  const [togglingId, setTogglingId] = useState(null)

  // Price history drawer state
  const [historyDrawerOpen, setHistoryDrawerOpen] = useState(false)
  const [selectedServiceForHistory, setSelectedServiceForHistory] = useState(null)
  const [priceHistory, setPriceHistory] = useState([])
  const [loadingHistory, setLoadingHistory] = useState(false)

  // Clinic config state
  const [savingClinic, setSavingClinic] = useState(false)

  const [serviceForm] = Form.useForm()
  const [clinicForm] = Form.useForm()

  // Load services & clinic config from backend
  const loadData = useCallback(async () => {
    setLoading(true)
    
    // 1. Tải danh mục dịch vụ (Dành cho cả ADMIN và MANAGER)
    try {
      const serviceResponse = await systemApi.services({ size: 200 })
      const rawServices = serviceResponse?.data
      const normalizedList = normalizeServiceList(rawServices)
      setServices(normalizedList)
    } catch (err) {
      console.error('Failed to load system service data:', err)
      message.error('Không thể tải danh mục dịch vụ. Vui lòng thử lại.')
    } finally {
      setLoading(false)
    }

    // 2. Tải cấu hình phòng khám (Chỉ khi tài khoản là ADMIN)
    if (isAdmin) {
      try {
        const clinicResponse = await systemApi.clinic()
        const clinic = clinicResponse?.data || {}
        clinicForm.setFieldsValue({
          ...clinic,
          openingTime: clinic.openingTime ? dayjs(clinic.openingTime, 'HH:mm:ss') : null,
          closingTime: clinic.closingTime ? dayjs(clinic.closingTime, 'HH:mm:ss') : null,
          examinationRooms: Array.isArray(clinic.examinationRooms)
            ? clinic.examinationRooms.join('\n')
            : clinic.examinationRooms || '',
        })
      } catch (err) {
        console.warn('Clinic config not available or forbidden:', err)
      }
    }
  }, [clinicForm, isAdmin])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Filtered services
  const filteredServices = useMemo(() => {
    let result = services

    // Filter by status
    if (statusFilter === 'active') {
      result = result.filter((s) => Boolean(s.active))
    } else if (statusFilter === 'inactive') {
      result = result.filter((s) => !Boolean(s.active))
    }

    // Filter by search keyword
    const keyword = searchTerm.trim().toLowerCase()
    if (keyword) {
      result = result.filter((s) => {
        const codeMatch = String(s.serviceCode || '').toLowerCase().includes(keyword)
        const nameMatch = String(s.name || '').toLowerCase().includes(keyword)
        return codeMatch || nameMatch
      })
    }

    return result
  }, [services, statusFilter, searchTerm])

  // Summary statistics
  const stats = useMemo(() => calculateServiceStats(services), [services])

  // Open Create Modal
  const openCreateModal = () => {
    setEditingService(null)
    serviceForm.resetFields()
    serviceForm.setFieldsValue({
      serviceCode: '',
      name: '',
      price: 100000,
      effectiveFrom: dayjs(),
    })
    setModalType('create')
  }

  // Open Edit Modal
  const openEditModal = (service) => {
    setEditingService(service)
    serviceForm.resetFields()
    serviceForm.setFieldsValue({
      serviceCode: service.serviceCode,
      name: service.name,
      price: service.price,
      effectiveFrom: service.effectiveFrom ? dayjs(service.effectiveFrom) : dayjs(),
      active: service.active !== undefined ? service.active : true,
    })
    setModalType('edit')
  }

  const closeModal = () => {
    setModalType(null)
    setEditingService(null)
    serviceForm.resetFields()
  }

  // Open Price History Drawer
  const openPriceHistory = async (service) => {
    setSelectedServiceForHistory(service)
    setHistoryDrawerOpen(true)
    setLoadingHistory(true)
    try {
      const res = await systemApi.getServicePriceHistory(service.id)
      const data = Array.isArray(res?.data) ? res.data : []
      setPriceHistory(categorizePriceHistory(data))
    } catch (err) {
      console.error('Failed to load price history:', err)
      // Fallback with current service price if history API errors
      setPriceHistory(
        categorizePriceHistory([
          {
            id: 'current',
            price: service.price,
            effectiveFrom: service.effectiveFrom,
            createdAt: new Date().toISOString(),
          },
        ])
      )
    } finally {
      setLoadingHistory(false)
    }
  }

  // Handle Save (Create or Update Service)
  const handleSaveService = async (values) => {
    setSavingService(true)
    try {
      if (modalType === 'create') {
        const validation = validateCreateServicePayload(values)
        if (!validation.isValid) {
          const firstErr = Object.values(validation.errors)[0]
          message.error(firstErr)
          setSavingService(false)
          return
        }

        const res = await systemApi.createService(validation.payload)
        message.success(`Đã thêm dịch vụ "${validation.payload.name}" thành công`)
        closeModal()
        await loadData()
      } else if (modalType === 'edit' && editingService) {
        const validation = validateUpdateServicePayload(values)
        if (!validation.isValid) {
          const firstErr = Object.values(validation.errors)[0]
          message.error(firstErr)
          setSavingService(false)
          return
        }

        await systemApi.updateService(editingService.id, validation.payload)
        message.success(`Đã cập nhật dịch vụ "${validation.payload.name}" thành công`)
        closeModal()
        await loadData()
      }
    } catch (err) {
      console.error('Error saving service:', err)
      const errorMsg =
        err?.response?.data?.message ||
        err?.response?.data?.detail ||
        (modalType === 'create'
          ? 'Không thể tạo dịch vụ. Vui lòng kiểm tra trùng mã hoặc tên dịch vụ.'
          : 'Không thể cập nhật dịch vụ. Vui lòng thử lại.')
      message.error(errorMsg)
    } finally {
      setSavingService(false)
    }
  }

  // Handle Status Toggle Switch
  const handleToggleStatus = async (service, nextActive) => {
    setTogglingId(service.id)
    try {
      await systemApi.updateServiceStatus(service.id, { active: nextActive })
      message.success(
        nextActive
          ? `Đã kích hoạt dịch vụ "${service.name}"`
          : `Đã tạm ngừng dịch vụ "${service.name}"`
      )
      setServices((prev) =>
        prev.map((s) => (s.id === service.id ? { ...s, active: nextActive } : s))
      )
    } catch (err) {
      console.error('Error toggling service status:', err)
      message.error('Không thể thay đổi trạng thái dịch vụ.')
    } finally {
      setTogglingId(null)
    }
  }

  // Handle Save Clinic Configuration
  const handleSaveClinic = async (values) => {
    setSavingClinic(true)
    try {
      const payload = {
        ...values,
        clinicName: values.clinicName?.trim() || '',
        address: values.address?.trim() || '',
        phone: values.phone?.trim() || '',
        openingTime: values.openingTime ? values.openingTime.format('HH:mm:ss') : '08:00:00',
        closingTime: values.closingTime ? values.closingTime.format('HH:mm:ss') : '17:00:00',
        examinationRooms: typeof values.examinationRooms === 'string'
          ? values.examinationRooms
              .split('\n')
              .map((r) => r.trim())
              .filter(Boolean)
          : values.examinationRooms || [],
      }

      await systemApi.updateClinic(payload)
      message.success('Đã lưu cấu hình phòng khám thành công')
    } catch (err) {
      console.error('Failed to save clinic config:', err)
      message.error(err.response?.data?.message || 'Không thể lưu cấu hình phòng khám')
    } finally {
      setSavingClinic(false)
    }
  }

  // Table Columns Definition
  const columns = [
    {
      title: 'Mã dịch vụ',
      dataIndex: 'serviceCode',
      key: 'serviceCode',
      width: 140,
      render: (code) => (
        <Tooltip title="Mã định danh duy nhất của dịch vụ dùng cho chỉ định & lập hóa đơn">
          <span className="service-code-badge">{code || '—'}</span>
        </Tooltip>
      ),
    },
    {
      title: 'Tên dịch vụ',
      dataIndex: 'name',
      key: 'name',
      sorter: (a, b) => (a.name || '').localeCompare(b.name || '', 'vi'),
      render: (name) => (
        <div className="service-name-cell">
          <span className="service-name-icon">
            <MedicineBoxOutlined />
          </span>
          <div>
            <Text strong className="service-name-text">
              {name}
            </Text>
          </div>
        </div>
      ),
    },
    {
      title: 'Đơn giá hiện hành',
      dataIndex: 'price',
      key: 'price',
      width: 170,
      align: 'right',
      sorter: (a, b) => Number(a.price || 0) - Number(b.price || 0),
      render: (price) => (
        <span className="service-price-tag">{formatVND(price)}</span>
      ),
    },
    {
      title: 'Hiệu lực từ',
      dataIndex: 'effectiveFrom',
      key: 'effectiveFrom',
      width: 170,
      sorter: (a, b) => dayjs(a.effectiveFrom).unix() - dayjs(b.effectiveFrom).unix(),
      render: (dateStr) => {
        if (!dateStr) return <span className="service-date-cell">—</span>
        const isUpcoming = dayjs(dateStr).isAfter(dayjs(), 'day')
        return (
          <Space direction="vertical" size={2}>
            <span className="service-date-cell">
              {dayjs(dateStr).format('DD/MM/YYYY')}
            </span>
            {isUpcoming ? (
              <Tag color="warning" className="service-subtag">
                Sắp áp dụng
              </Tag>
            ) : (
              <Tag color="cyan" className="service-subtag">
                Đang áp dụng
              </Tag>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 170,
      align: 'center',
      render: (active, record) => (
        <Space orientation="horizontal" size="small" align="center">
          <Tag
            className={`service-status-pill ${active ? 'is-active' : 'is-inactive'}`}
            color={active ? 'success' : 'default'}
          >
            {active ? 'Hiệu lực' : 'Ngừng dùng'}
          </Tag>
          {canManage && (
            <Popconfirm
              title={active ? 'Tạm ngừng dịch vụ này?' : 'Kích hoạt lại dịch vụ này?'}
              description={
                active
                  ? 'Dịch vụ tạm ngừng sẽ không thể tạo mới trong chỉ định khám.'
                  : 'Dịch vụ sẽ sẵn sàng cho việc chỉ định và tính phí.'
              }
              onConfirm={() => handleToggleStatus(record, !active)}
              okText="Đồng ý"
              cancelText="Hủy"
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
      width: 190,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="Xem lịch sử điều chỉnh biểu giá">
            <Button
              className="service-action-btn history-btn"
              icon={<HistoryOutlined />}
              onClick={() => openPriceHistory(record)}
            >
              Lịch sử giá
            </Button>
          </Tooltip>
          {canManage && (
            <Tooltip title="Chỉnh sửa thông tin & cập nhật giá mới">
              <Button
                className="service-action-btn edit-btn"
                type="primary"
                ghost
                icon={<EditOutlined />}
                onClick={() => openEditModal(record)}
              >
                Sửa
              </Button>
            </Tooltip>
          )}
        </Space>
      ),
    },
  ]

  // Tab: Service Catalog & Price Management
  const serviceCatalogContent = (
    <div className="service-catalog-container">
      {/* KPI Header Stats */}
      <div className="service-kpi-grid">
        <Card className="service-kpi-card kpi-total" bordered={false}>
          <div className="service-kpi-inner">
            <div className="service-kpi-icon-wrap total">
              <AppstoreOutlined />
            </div>
            <div className="service-kpi-content">
              <span className="service-kpi-label">Tổng số dịch vụ</span>
              <h3 className="service-kpi-value">{stats.total}</h3>
              <span className="service-kpi-hint">Đã đăng ký trong hệ thống</span>
            </div>
          </div>
        </Card>

        <Card className="service-kpi-card kpi-active" bordered={false}>
          <div className="service-kpi-inner">
            <div className="service-kpi-icon-wrap active">
              <CheckCircleOutlined />
            </div>
            <div className="service-kpi-content">
              <span className="service-kpi-label">Đang hiệu lực</span>
              <h3 className="service-kpi-value text-success">{stats.activeCount}</h3>
              <span className="service-kpi-hint">Sẵn sàng lập hóa đơn</span>
            </div>
          </div>
        </Card>

        <Card className="service-kpi-card kpi-inactive" bordered={false}>
          <div className="service-kpi-inner">
            <div className="service-kpi-icon-wrap inactive">
              <StopOutlined />
            </div>
            <div className="service-kpi-content">
              <span className="service-kpi-label">Tạm ngừng sử dụng</span>
              <h3 className="service-kpi-value text-secondary">{stats.inactiveCount}</h3>
              <span className="service-kpi-hint">Chưa đưa vào bảng giá</span>
            </div>
          </div>
        </Card>

        <Card className="service-kpi-card kpi-price" bordered={false}>
          <div className="service-kpi-inner">
            <div className="service-kpi-icon-wrap price">
              <DollarOutlined />
            </div>
            <div className="service-kpi-content">
              <span className="service-kpi-label">Giá trung bình dịch vụ</span>
              <h3 className="service-kpi-value text-primary">{formatVND(stats.avgPrice)}</h3>
              <span className="service-kpi-hint">Theo biểu giá đang áp dụng</span>
            </div>
          </div>
        </Card>
      </div>

      {/* Main Data Card */}
      <Card className="services-data-card" bordered={false}>
        <div className="service-toolbar-row">
          <div className="service-toolbar-left">
            <Input
              className="service-search-input"
              allowClear
              prefix={<SearchOutlined />}
              placeholder="Tìm theo mã hoặc tên dịch vụ..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <Radio.Group
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              buttonStyle="solid"
              className="service-filter-radios"
            >
              <Radio.Button value="all">Tất cả ({stats.total})</Radio.Button>
              <Radio.Button value="active">Đang hiệu lực ({stats.activeCount})</Radio.Button>
              <Radio.Button value="inactive">Ngừng dùng ({stats.inactiveCount})</Radio.Button>
            </Radio.Group>
          </div>

          <div className="service-toolbar-right">
            <Button
              icon={<ReloadOutlined />}
              loading={loading}
              onClick={loadData}
              className="service-reload-btn"
            >
              Làm mới
            </Button>
            {canManage && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={openCreateModal}
                className="service-add-btn"
              >
                Thêm dịch vụ mới
              </Button>
            )}
          </div>
        </div>

        <Table
          className="services-table"
          rowKey="id"
          loading={loading}
          dataSource={filteredServices}
          columns={columns}
          scroll={{ x: 920 }}
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
                className="service-empty-container"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  searchTerm || statusFilter !== 'all'
                    ? 'Không tìm thấy dịch vụ phù hợp với bộ lọc hiện tại'
                    : 'Chưa có dịch vụ nào trong danh mục'
                }
              />
            ),
          }}
        />
      </Card>
    </div>
  )

  // Tab: Clinic Configuration
  const clinicConfigurationContent = (
    <section className="clinic-config-shell">
      <Card className="clinic-config-card" bordered={false}>
        <div className="clinic-config-header">
          <span className="clinic-config-icon">
            <EnvironmentOutlined />
          </span>
          <div>
            <Title level={4}>Thông tin vận hành phòng khám</Title>
            <Paragraph type="secondary">
              Cập nhật thông tin liên hệ, khung giờ làm việc và danh sách phòng khám trong cơ sở y tế.
            </Paragraph>
          </div>
        </div>

        <Form
          className="clinic-form"
          form={clinicForm}
          layout="vertical"
          onFinish={handleSaveClinic}
        >
          <div className="clinic-form-grid">
            <Form.Item
              name="clinicName"
              label="Tên phòng khám / Cơ sở y tế"
              rules={[{ required: true, message: 'Vui lòng nhập tên phòng khám' }]}
            >
              <Input size="large" placeholder="Ví dụ: Phòng khám Đa khoa Quốc tế" />
            </Form.Item>

            <Form.Item name="phone" label="Số điện thoại liên hệ">
              <Input size="large" placeholder="Ví dụ: 1900 1234 hoặc 028 3822 1234" />
            </Form.Item>

            <div className="clinic-hours-row">
              <Form.Item
                className="clinic-hours-field"
                name="openingTime"
                label={
                  <span className="clinic-time-label">
                    <ClockCircleOutlined /> Giờ mở cửa
                  </span>
                }
                rules={[{ required: true, message: 'Chọn giờ mở cửa' }]}
              >
                <TimePicker size="large" format="HH:mm" minuteStep={5} orientation="horizontal" />
              </Form.Item>
              <Form.Item
                className="clinic-hours-field"
                name="closingTime"
                label={
                  <span className="clinic-time-label">
                    <ClockCircleOutlined /> Giờ đóng cửa
                  </span>
                }
                rules={[{ required: true, message: 'Chọn giờ đóng cửa' }]}
              >
                <TimePicker size="large" format="HH:mm" minuteStep={5} orientation="horizontal" />
              </Form.Item>
            </div>

            <Form.Item className="clinic-form-full" name="address" label="Địa chỉ cơ sở y tế">
              <Input size="large" placeholder="Ví dụ: 123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM" />
            </Form.Item>

            <Form.Item
              className="clinic-form-full"
              name="examinationRooms"
              label="Danh sách phòng khám & phòng chuyên khoa"
              extra="Mỗi dòng tương ứng với một phòng khám chức năng."
              rules={[{ required: true, message: 'Vui lòng nhập ít nhất một phòng khám' }]}
            >
              <Input.TextArea
                rows={5}
                placeholder={'Phòng 101 - Khám Tổng Quát\nPhòng 102 - Khám Nội Tim Mạch\nPhòng 201 - Siêu Âm Chẩn Đoán'}
              />
            </Form.Item>
          </div>

          <div className="clinic-form-actions">
            <Button icon={<ReloadOutlined />} onClick={loadData} disabled={savingClinic}>
              Khôi phục dữ liệu
            </Button>
            <Button type="primary" htmlType="submit" loading={savingClinic}>
              Lưu cấu hình phòng khám
            </Button>
          </div>
        </Form>
      </Card>
    </section>
  )

  return (
    <div className="services-admin-page">
      {/* Page Header */}
      <div className="service-page-heading">
        <div className="service-heading-copy">
          <span className="service-section-eyebrow">
            <ThunderboltOutlined /> Quản trị bảng giá & dịch vụ
          </span>
          <Title level={3}>Danh mục dịch vụ và Bảng giá viện phí</Title>
          <Paragraph type="secondary">
            Thiết lập mã dịch vụ, biểu giá khám, xét nghiệm, chẩn đoán hình ảnh cùng thời điểm hiệu lực để thu phí chính xác, minh bạch.
          </Paragraph>
        </div>
      </div>

      {/* Main Tabs */}
      {isAdmin ? (
        <Tabs
          className="service-main-tabs"
          defaultActiveKey="catalog"
          items={[
            {
              key: 'catalog',
              label: (
                <span>
                  <AppstoreOutlined /> Danh mục & Bảng giá
                </span>
              ),
              children: serviceCatalogContent,
            },
            {
              key: 'clinic',
              label: (
                <span>
                  <EnvironmentOutlined /> Cấu hình phòng khám
                </span>
              ),
              children: clinicConfigurationContent,
            },
          ]}
        />
      ) : (
        serviceCatalogContent
      )}

      {/* Modal: Create & Edit Service */}
      <Modal
        className="service-form-modal"
        width={640}
        title={
          <div className="service-modal-header">
            <span className="service-modal-icon">
              {modalType === 'create' ? <PlusOutlined /> : <EditOutlined />}
            </span>
            <div>
              <Title level={4}>
                {modalType === 'create'
                  ? 'Thêm dịch vụ khám mới'
                  : `Cập nhật dịch vụ: ${editingService?.serviceCode || ''}`}
              </Title>
              <Text type="secondary">
                {modalType === 'create'
                  ? 'Khai báo thông tin dịch vụ, đơn giá khởi điểm và thời điểm bắt đầu áp dụng.'
                  : 'Cập nhật tên, trạng thái hoặc thiết lập mức giá mới có hiệu lực theo thời gian.'}
              </Text>
            </div>
          </div>
        }
        open={modalType !== null}
        footer={null}
        centered
        destroyOnClose
        maskClosable={!savingService}
        closable={!savingService}
        onCancel={closeModal}
      >
        <Form
          className="service-form"
          form={serviceForm}
          layout="vertical"
          onFinish={handleSaveService}
        >
          <div className="service-form-grid">
            <Form.Item
              name="serviceCode"
              label="Mã dịch vụ"
              rules={[
                { required: true, message: 'Vui lòng nhập mã dịch vụ' },
                { max: 50, message: 'Tối đa 50 ký tự' },
              ]}
              extra={modalType === 'edit' ? 'Mã dịch vụ là cố định và không thể thay đổi sau khi tạo.' : undefined}
            >
              <Input
                size="large"
                placeholder="Ví dụ: KHAM-NOI, XN-MAU-01"
                disabled={modalType === 'edit'}
              />
            </Form.Item>

            <Form.Item
              name="name"
              label="Tên dịch vụ y tế"
              rules={[
                { required: true, message: 'Vui lòng nhập tên dịch vụ' },
                { max: 255, message: 'Tối đa 255 ký tự' },
              ]}
            >
              <Input size="large" placeholder="Ví dụ: Khám tổng quát, Siêu âm ổ bụng" />
            </Form.Item>

            <Form.Item
              name="price"
              label={modalType === 'create' ? 'Đơn giá niêm yết (VND)' : 'Đơn giá mới (VND)'}
              rules={[
                { required: true, message: 'Vui lòng nhập đơn giá' },
                {
                  type: 'number',
                  min: 0,
                  message: 'Đơn giá phải lớn hơn hoặc bằng 0',
                },
              ]}
            >
              <InputNumber
                size="large"
                min={0}
                step={5000}
                controls={false}
                formatter={(value) => `${value || ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                parser={(value) => value?.replace(/\./g, '') || ''}
                addonAfter="₫"
                placeholder="0"
                style={{ width: '100%' }}
              />
            </Form.Item>

            <Form.Item
              name="effectiveFrom"
              label="Ngày hiệu lực áp dụng"
              rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu hiệu lực' }]}
            >
              <DatePicker
                size="large"
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                placeholder="Chọn ngày áp dụng"
              />
            </Form.Item>

            {modalType === 'edit' && (
              <Form.Item className="service-form-full" name="active" label="Trạng thái dịch vụ">
                <Select
                  size="large"
                  options={[
                    { value: true, label: '🟢 Đang hiệu lực (Sẵn sàng chỉ định & thu phí)' },
                    { value: false, label: '⏸️ Ngừng sử dụng (Tạm ẩn khỏi danh mục chỉ định)' },
                  ]}
                />
              </Form.Item>
            )}
          </div>

          <Alert
            className="service-modal-alert"
            type="info"
            showIcon
            icon={<InfoCircleOutlined />}
            message={
              modalType === 'create'
                ? 'Biểu giá mới sẽ tự động áp dụng cho mọi chỉ định và hóa đơn phát sinh từ ngày hiệu lực.'
                : 'Nếu thay đổi đơn giá hoặc ngày hiệu lực, hệ thống sẽ lưu vết lịch sử giá mới. Hóa đơn cũ giữ nguyên đơn giá lịch sử, hóa đơn từ ngày hiệu lực mới sẽ áp dụng giá mới.'
            }
          />

          <div className="service-modal-actions">
            <Button size="large" onClick={closeModal} disabled={savingService}>
              Hủy
            </Button>
            <Button size="large" type="primary" htmlType="submit" loading={savingService}>
              {modalType === 'create' ? 'Tạo dịch vụ' : 'Lưu thay đổi'}
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Drawer: Price History */}
      <Drawer
        title={
          <div className="price-history-drawer-header">
            <HistoryOutlined className="history-drawer-icon" />
            <div>
              <Title level={4} style={{ margin: 0 }}>
                Lịch sử biểu giá dịch vụ
              </Title>
              <Text type="secondary" style={{ fontSize: 13 }}>
                {selectedServiceForHistory?.serviceCode} — {selectedServiceForHistory?.name}
              </Text>
            </div>
          </div>
        }
        placement="right"
        width={560}
        open={historyDrawerOpen}
        onClose={() => setHistoryDrawerOpen(false)}
        destroyOnClose
      >
        {loadingHistory ? (
          <div className="price-history-loading">
            <Spin size="large" tip="Đang tải lịch sử giá..." />
          </div>
        ) : (
          <div className="price-history-content">
            {/* Service quick info */}
            <Card className="price-history-summary-card" size="small">
              <Descriptions column={2} size="small">
                <Descriptions.Item label="Mã dịch vụ">
                  <span className="service-code-badge">
                    {selectedServiceForHistory?.serviceCode}
                  </span>
                </Descriptions.Item>
                <Descriptions.Item label="Trạng thái">
                  <Tag color={selectedServiceForHistory?.active ? 'success' : 'default'}>
                    {selectedServiceForHistory?.active ? 'Đang hiệu lực' : 'Ngừng dùng'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Giá hiện tại" span={2}>
                  <span className="price-history-current-val">
                    {formatVND(selectedServiceForHistory?.price)}
                  </span>
                </Descriptions.Item>
              </Descriptions>
            </Card>

            <div className="price-history-timeline-section">
              <Title level={5} style={{ marginBottom: 16 }}>
                Các mốc thời gian áp dụng giá
              </Title>

              {priceHistory.length === 0 ? (
                <Empty description="Chưa có dữ liệu lịch sử giá cho dịch vụ này" />
              ) : (
                <Timeline
                  mode="left"
                  items={priceHistory.map((item, idx) => ({
                    color:
                      item.status === 'CURRENT'
                        ? 'green'
                        : item.status === 'UPCOMING'
                        ? 'orange'
                        : 'gray',
                    children: (
                      <div className="price-history-item-card">
                        <div className="price-history-item-header">
                          <span className="price-history-item-price">
                            {formatVND(item.price)}
                          </span>
                          <Tag color={item.badgeColor}>{item.statusLabel}</Tag>
                        </div>
                        <div className="price-history-item-dates">
                          <div>
                            <Text type="secondary">Hiệu lực từ: </Text>
                            <Text strong>
                              {item.effectiveFrom
                                ? dayjs(item.effectiveFrom).format('DD/MM/YYYY')
                                : '—'}
                            </Text>
                          </div>
                          {item.createdAt && (
                            <div>
                              <Text type="secondary">Thời điểm tạo: </Text>
                              <Text>
                                {dayjs(item.createdAt).format('DD/MM/YYYY HH:mm')}
                              </Text>
                            </div>
                          )}
                        </div>
                      </div>
                    ),
                  }))}
                />
              )}
            </div>
          </div>
        )}
      </Drawer>
    </div>
  )
}

export default ServicesPage
