import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  AppstoreOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  EditOutlined,
  EnvironmentOutlined,
  ExclamationCircleOutlined,
  HistoryOutlined,
  LockOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
  TagOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Divider,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  TimePicker,
  Timeline,
  Tooltip,
  Typography,
} from 'antd'
import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import systemApi from '../api/systemApi'
import { useAuthContext } from '../context/AuthContext'
import {
  calculateServiceStats,
  checkServiceManagePermission,
  fixVietnameseEncoding,
  formatDate,
  formatMoney,
  getTodayDateString,
  normalizeServiceItem,
  processPriceHistory,
  translateServiceErrorMessage,
  validateServicePayload,
} from '../utils/serviceCatalogHelpers'

dayjs.extend(customParseFormat)

const { Paragraph, Text, Title } = Typography

const PRESET_PRICES = [50000, 100000, 150000, 200000, 300000, 500000]

function ServicesPage() {
  const { user } = useAuthContext()
  const canManage = useMemo(() => checkServiceManagePermission(user?.roles || []), [user?.roles])
  const isAdmin = useMemo(() => {
    const roles = (Array.isArray(user?.roles) ? user.roles : [user?.roles])
      .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    return roles.includes('admin')
  }, [user?.roles])

  // Services State
  const [services, setServices] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL') // ALL, ACTIVE, INACTIVE

  // Modal Create/Edit State
  const [modalOpen, setModalOpen] = useState(false)
  const [editingService, setEditingService] = useState(null)
  const [savingService, setSavingService] = useState(false)
  const [serviceForm] = Form.useForm()

  // Price History Drawer State
  const [historyDrawerOpen, setHistoryDrawerOpen] = useState(false)
  const [selectedServiceForHistory, setSelectedServiceForHistory] = useState(null)
  const [priceHistoryList, setPriceHistoryList] = useState([])
  const [loadingHistory, setLoadingHistory] = useState(false)

  // Status Switch Loading Map
  const [togglingStatusId, setTogglingStatusId] = useState(null)

  // Clinic Config State
  const [savingClinic, setSavingClinic] = useState(false)
  const [clinicForm] = Form.useForm()

  // Load Services from Backend
  const loadServices = useCallback(async () => {
    setLoading(true)
    try {
      const servicesRes = await systemApi.services({ size: 100 })
      const rawServices = (Array.isArray(servicesRes?.data)
        ? servicesRes.data
        : servicesRes?.data?.content || []).map(normalizeServiceItem)
      setServices(rawServices)
    } catch (err) {
      message.error(translateServiceErrorMessage(err, 'Không thể tải danh sách dịch vụ.'))
      setServices([])
    } finally {
      setLoading(false)
    }
  }, [])

  // Load Clinic Configuration (only for Admin)
  const loadClinic = useCallback(async () => {
    if (!isAdmin) return
    try {
      const clinicRes = await systemApi.clinic()
      const clinic = clinicRes?.data || {}
      clinicForm.setFieldsValue({
        ...clinic,
        openingTime: clinic.openingTime ? dayjs(clinic.openingTime, 'HH:mm:ss') : null,
        closingTime: clinic.closingTime ? dayjs(clinic.closingTime, 'HH:mm:ss') : null,
        examinationRooms: Array.isArray(clinic.examinationRooms)
          ? clinic.examinationRooms.join('\n')
          : clinic.examinationRooms || '',
      })
    } catch {
      // Clinic config error should not disturb service catalog
    }
  }, [clinicForm, isAdmin])

  useEffect(() => {
    loadServices()
    if (isAdmin) {
      loadClinic()
    }
  }, [loadServices, loadClinic, isAdmin])

  // KPI Statistics
  const stats = useMemo(() => calculateServiceStats(services), [services])

  // Filtered & Searched Services
  const filteredServices = useMemo(() => {
    let result = [...services]

    // Status Filter
    if (statusFilter === 'ACTIVE') {
      result = result.filter((s) => s.active !== false)
    } else if (statusFilter === 'INACTIVE') {
      result = result.filter((s) => s.active === false)
    }

    // Search keyword
    const keyword = searchTerm.trim().toLowerCase()
    if (keyword) {
      result = result.filter((s) =>
        [s.serviceCode, s.name]
          .filter(Boolean)
          .some((val) => String(val).toLowerCase().includes(keyword)),
      )
    }

    return result
  }, [searchTerm, statusFilter, services])

  // Modal Handlers
  const handleOpenCreateModal = () => {
    setEditingService(null)
    serviceForm.resetFields()
    serviceForm.setFieldsValue({
      effectiveFrom: dayjs(),
      active: true,
      price: 100000,
    })
    setModalOpen(true)
  }

  const handleOpenEditModal = (service) => {
    setEditingService(service)
    serviceForm.setFieldsValue({
      serviceCode: service.serviceCode,
      name: service.name,
      price: service.price,
      effectiveFrom: service.effectiveFrom ? dayjs(service.effectiveFrom) : dayjs(),
      active: service.active !== false,
    })
    setModalOpen(true)
  }

  const handleCloseModal = () => {
    if (savingService) return
    setModalOpen(false)
    setEditingService(null)
    serviceForm.resetFields()
  }

  // Save Service (Create or Update)
  const handleSaveService = async (values) => {
    const isEditing = Boolean(editingService)
    const validation = validateServicePayload(
      {
        ...values,
        serviceCode: isEditing ? editingService.serviceCode : values.serviceCode,
      },
      isEditing,
    )

    if (!validation.valid) {
      message.error(validation.error)
      return
    }

    setSavingService(true)
    const payload = validation.sanitizedData

    try {
      if (isEditing) {
        const res = await systemApi.updateService(editingService.id, payload)
        const updated = normalizeServiceItem(res?.data || { ...editingService, ...payload })
        setServices((prev) =>
          prev.map((item) => (item.id === editingService.id ? { ...item, ...updated } : item)),
        )
        message.success(`Đã cập nhật dịch vụ "${payload.name}"`)
      } else {
        const res = await systemApi.createService(payload)
        const created = normalizeServiceItem(res?.data || { id: 'svc_' + Date.now(), ...payload })
        setServices((prev) => [created, ...prev])
        message.success(`Đã thêm mới dịch vụ "${payload.name}"`)
      }
      handleCloseModal()
      loadServices()
    } catch (err) {
      message.error(translateServiceErrorMessage(err, 'Lưu dịch vụ thất bại.'))
    } finally {
      setSavingService(false)
    }
  }

  // Quick Status Toggle Handler
  const handleToggleStatus = (service, newActiveState) => {
    const actionText = newActiveState ? 'kích hoạt lại' : 'tạm ngưng áp dụng'

    Modal.confirm({
      title: `${newActiveState ? 'Kích hoạt' : 'Tạm ngưng'} dịch vụ?`,
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>
            Bạn có chắc chắn muốn {actionText} dịch vụ <strong>{service.name}</strong> (Mã:{' '}
            <code>{service.serviceCode}</code>)?
          </p>
          {!newActiveState && (
            <Text type="secondary" style={{ fontSize: 13 }}>
              Khi tạm ngưng, dịch vụ này sẽ không thể được chọn trong các lượt khám hoặc lập hóa đơn
              mới.
            </Text>
          )}
        </div>
      ),
      okText: newActiveState ? 'Kích hoạt' : 'Tạm ngưng',
      okButtonProps: newActiveState ? { type: 'primary' } : { danger: true },
      cancelText: 'Hủy',
      centered: true,
      onOk: async () => {
        setTogglingStatusId(service.id)
        try {
          if (systemApi.updateServiceStatus) {
            await systemApi.updateServiceStatus(service.id, newActiveState)
          } else {
            await systemApi.updateService(service.id, {
              name: service.name,
              active: newActiveState,
              price: service.price,
              effectiveFrom: service.effectiveFrom,
            })
          }
          setServices((prev) =>
            prev.map((s) => (s.id === service.id ? { ...s, active: newActiveState } : s)),
          )
          message.success(`Đã ${actionText} dịch vụ thành công`)
        } catch (err) {
          message.error(translateServiceErrorMessage(err, `Không thể ${actionText} dịch vụ.`))
        } finally {
          setTogglingStatusId(null)
        }
      },
    })
  }

  // Open Price History Drawer
  const handleOpenPriceHistory = async (service) => {
    setSelectedServiceForHistory(service)
    setHistoryDrawerOpen(true)
    setLoadingHistory(true)
    try {
      const res = await systemApi.getServicePriceHistory(service.id)
      const rawPrices = Array.isArray(res?.data) ? res.data : []
      const processed = processPriceHistory(rawPrices, getTodayDateString())
      setPriceHistoryList(processed)
    } catch {
      const fallback = processPriceHistory(
        [
          {
            id: service.id + '_p0',
            price: service.price,
            effectiveFrom: service.effectiveFrom || getTodayDateString(),
            createdAt: new Date().toISOString(),
          },
        ],
        getTodayDateString(),
      )
      setPriceHistoryList(fallback)
    } finally {
      setLoadingHistory(false)
    }
  }

  // Save Clinic Configuration (Admin only)
  const handleSaveClinic = async (values) => {
    setSavingClinic(true)
    try {
      const payload = {
        clinicName: values.clinicName?.trim() || '',
        address: values.address?.trim() || '',
        phone: values.phone?.trim() || '',
        openingTime: values.openingTime ? values.openingTime.format('HH:mm:ss') : '07:30:00',
        closingTime: values.closingTime ? values.closingTime.format('HH:mm:ss') : '17:00:00',
        examinationRooms: (values.examinationRooms || '')
          .split('\n')
          .map((room) => room.trim())
          .filter(Boolean),
      }
      await systemApi.updateClinic(payload)
      message.success('Đã lưu thông tin cấu hình phòng khám')
    } catch (err) {
      message.error(translateServiceErrorMessage(err, 'Không thể lưu cấu hình phòng khám.'))
    } finally {
      setSavingClinic(false)
    }
  }

  // Table Columns
  const columns = [
    {
      title: 'Mã dịch vụ',
      dataIndex: 'serviceCode',
      key: 'serviceCode',
      width: 140,
      render: (code) => (
        <span className="service-code-badge">
          <TagOutlined style={{ marginRight: 5, color: '#2563eb' }} />
          <code>{code}</code>
        </span>
      ),
    },
    {
      title: 'Tên dịch vụ khám / cận lâm sàng',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => (
        <div className="service-name-cell">
          <div className="service-icon-wrap">
            <AppstoreOutlined />
          </div>
          <div>
            <Text strong className="service-title-text">
              {name}
            </Text>
            {record.active === false && (
              <Tag color="error" style={{ marginLeft: 8, fontSize: 11, padding: '0 6px' }}>
                Tạm ngưng
              </Tag>
            )}
          </div>
        </div>
      ),
    },
    {
      title: 'Đơn giá hiệu lực',
      dataIndex: 'price',
      key: 'price',
      width: 170,
      align: 'right',
      sorter: (a, b) => Number(a.price || 0) - Number(b.price || 0),
      render: (price) => (
        <div className="service-price-wrapper">
          <span className="service-price-val">{formatMoney(price)}</span>
        </div>
      ),
    },
    {
      title: 'Ngày bắt đầu áp dụng',
      dataIndex: 'effectiveFrom',
      key: 'effectiveFrom',
      width: 180,
      sorter: (a, b) => String(a.effectiveFrom || '').localeCompare(String(b.effectiveFrom || '')),
      render: (date) => (
        <span className="service-effective-date">
          <CalendarOutlined style={{ marginRight: 6, color: '#64748b' }} />
          {formatDate(date)}
        </span>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 160,
      align: 'center',
      render: (active, record) => {
        const isActive = active !== false
        return (
          <Space orientation="horizontal" size={8} align="center">
            <Tag color={isActive ? 'success' : 'default'} className="service-status-pill">
              {isActive ? <CheckCircleOutlined /> : <StopOutlined />}
              <span style={{ marginLeft: 4 }}>{isActive ? 'Đang hiệu lực' : 'Tạm ngưng'}</span>
            </Tag>
            {canManage && (
              <Switch
                size="small"
                checked={isActive}
                loading={togglingStatusId === record.id}
                onChange={(checked) => handleToggleStatus(record, checked)}
                title={isActive ? 'Nhấn để tạm ngưng dịch vụ' : 'Nhấn để kích hoạt lại dịch vụ'}
              />
            )}
          </Space>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 180,
      align: 'center',
      render: (_, service) => (
        <Space orientation="horizontal" size={6}>
          <Tooltip title="Chỉnh sửa tên, trạng thái hoặc điều chỉnh bảng giá">
            <Button
              type="default"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleOpenEditModal(service)}
              disabled={!canManage}
            >
              Sửa
            </Button>
          </Tooltip>
          <Tooltip title="Xem lịch sử điều chỉnh bảng giá theo thời gian">
            <Button
              type="text"
              size="small"
              className="btn-price-history"
              icon={<HistoryOutlined />}
              onClick={() => handleOpenPriceHistory(service)}
            >
              Lịch sử giá
            </Button>
          </Tooltip>
        </Space>
      ),
    },
  ]

  // Main Service Catalog View
  const serviceCatalogContent = (
    <div className="service-catalog-container">
      {/* KPI Overview Cards */}
      <Row gutter={[16, 16]} className="service-kpi-row">
        <Col xs={24} sm={12} md={6}>
          <Card className="service-kpi-card card-kpi-total" bordered={false}>
            <div className="kpi-card-inner">
              <div className="kpi-icon-badge badge-blue">
                <AppstoreOutlined />
              </div>
              <div className="kpi-content">
                <Text type="secondary" className="kpi-title">
                  Tổng số dịch vụ
                </Text>
                <Title level={3} className="kpi-number">
                  {stats.total}
                </Title>
                <span className="kpi-subtext">Danh mục phòng khám</span>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card className="service-kpi-card card-kpi-active" bordered={false}>
            <div className="kpi-card-inner">
              <div className="kpi-icon-badge badge-emerald">
                <CheckCircleOutlined />
              </div>
              <div className="kpi-content">
                <Text type="secondary" className="kpi-title">
                  Đang hiệu lực
                </Text>
                <Title level={3} className="kpi-number text-emerald">
                  {stats.activeCount}
                </Title>
                <span className="kpi-subtext">Sẵn sàng lập hóa đơn</span>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card className="service-kpi-card card-kpi-inactive" bordered={false}>
            <div className="kpi-card-inner">
              <div className="kpi-icon-badge badge-amber">
                <StopOutlined />
              </div>
              <div className="kpi-content">
                <Text type="secondary" className="kpi-title">
                  Tạm ngưng áp dụng
                </Text>
                <Title level={3} className="kpi-number text-amber">
                  {stats.inactiveCount}
                </Title>
                <span className="kpi-subtext">Đã vô hiệu hóa</span>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card className="service-kpi-card card-kpi-avg" bordered={false}>
            <div className="kpi-card-inner">
              <div className="kpi-icon-badge badge-indigo">
                <DollarOutlined />
              </div>
              <div className="kpi-content">
                <Text type="secondary" className="kpi-title">
                  Đơn giá trung bình
                </Text>
                <Title level={3} className="kpi-number text-indigo">
                  {formatMoney(stats.avgPrice)}
                </Title>
                <span className="kpi-subtext">
                  Cao nhất: {formatMoney(stats.maxPrice)}
                </span>
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Main Table Card */}
      <Card className="service-table-card" bordered={false}>
        <div className="service-table-toolbar">
          <div className="toolbar-search-filter">
            <Input
              className="service-search-input"
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              placeholder="Tìm kiếm theo mã hoặc tên dịch vụ..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              allowClear
            />
            <Select
              className="service-status-select"
              value={statusFilter}
              onChange={setStatusFilter}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'ACTIVE', label: '✓ Đang hiệu lực' },
                { value: 'INACTIVE', label: '✕ Tạm ngưng áp dụng' },
              ]}
            />
          </div>

          <Space orientation="horizontal" size={10} className="toolbar-actions">
            <Button
              icon={<ReloadOutlined />}
              onClick={loadServices}
              loading={loading}
              title="Tải lại dữ liệu"
            >
              Làm mới
            </Button>
            {canManage && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleOpenCreateModal}
                className="btn-create-service"
              >
                Thêm dịch vụ
              </Button>
            )}
          </Space>
        </div>

        <Table
          className="services-data-table"
          rowKey="id"
          loading={loading}
          dataSource={filteredServices}
          columns={columns}
          scroll={{ x: 950 }}
          pagination={{
            pageSize: 8,
            showSizeChanger: true,
            pageSizeOptions: ['8', '15', '30', '50'],
            showTotal: (total, range) =>
              `Hiển thị ${range[0]}-${range[1]} trên tổng số ${total} dịch vụ`,
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  searchTerm || statusFilter !== 'ALL'
                    ? 'Không tìm thấy dịch vụ nào phù hợp với bộ lọc.'
                    : 'Chưa có dịch vụ nào trong danh mục.'
                }
              />
            ),
          }}
        />
      </Card>
    </div>
  )

  // Clinic Operations Configuration Tab View (Only accessible for Admin)
  const clinicConfigContent = isAdmin ? (
    <div className="clinic-config-wrapper">
      <Card className="clinic-config-card" bordered={false}>
        <div className="clinic-config-header">
          <div className="config-icon-box">
            <EnvironmentOutlined />
          </div>
          <div>
            <Title level={4} style={{ margin: 0 }}>
              Thông tin vận hành phòng khám
            </Title>
            <Paragraph type="secondary" style={{ margin: '4px 0 0' }}>
              Cập nhật thông tin liên hệ, thời gian hoạt động và danh sách phòng khám chuyên khoa.
            </Paragraph>
          </div>
        </div>

        <Divider style={{ margin: '16px 0 24px' }} />

        <Form
          form={clinicForm}
          layout="vertical"
          onFinish={handleSaveClinic}
          className="clinic-config-form"
        >
          <Row gutter={[20, 16]}>
            <Col xs={24} md={12}>
              <Form.Item
                name="clinicName"
                label="Tên phòng khám / Cơ sở y tế"
                rules={[{ required: true, message: 'Vui lòng nhập tên phòng khám' }]}
              >
                <Input placeholder="Ví dụ: Phòng khám Đa khoa Sài Gòn" size="large" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="phone" label="Số điện thoại hotline / CSKH">
                <Input placeholder="Ví dụ: 1900 6868 hoặc 028 1234 5678" size="large" />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                name="openingTime"
                label={
                  <span>
                    <ClockCircleOutlined style={{ marginRight: 6 }} />
                    Giờ mở cửa
                  </span>
                }
                rules={[{ required: true, message: 'Vui lòng chọn giờ mở cửa' }]}
              >
                <TimePicker format="HH:mm" minuteStep={5} size="large" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                name="closingTime"
                label={
                  <span>
                    <ClockCircleOutlined style={{ marginRight: 6 }} />
                    Giờ đóng cửa
                  </span>
                }
                rules={[{ required: true, message: 'Vui lòng chọn giờ đóng cửa' }]}
              >
                <TimePicker format="HH:mm" minuteStep={5} size="large" style={{ width: '100%' }} />
              </Form.Item>
            </Col>

            <Col xs={24}>
              <Form.Item name="address" label="Địa chỉ cơ sở">
                <Input placeholder="Nhập địa chỉ chi tiết của phòng khám" size="large" />
              </Form.Item>
            </Col>

            <Col xs={24}>
              <Form.Item
                name="examinationRooms"
                label="Danh sách buồng / phòng khám"
                extra="Nhập mỗi phòng khám trên một dòng (Ví dụ: Phòng khám Nội 1, Phòng khám Ngoại, Phòng Siêu âm...)"
                rules={[{ required: true, message: 'Vui lòng nhập ít nhất một phòng khám' }]}
              >
                <Input.TextArea rows={5} placeholder="Phòng khám Nội 1&#10;Phòng khám Nhi&#10;Phòng Siêu âm & CĐHA" />
              </Form.Item>
            </Col>
          </Row>

          <div className="clinic-form-actions">
            <Button onClick={loadClinic} disabled={savingClinic} style={{ marginRight: 12 }}>
              Hủy bỏ
            </Button>
            <Button type="primary" htmlType="submit" loading={savingClinic} size="large">
              Lưu cấu hình
            </Button>
          </div>
        </Form>
      </Card>
    </div>
  ) : null

  // Build Tab Items
  const tabItems = useMemo(() => {
    const items = [
      {
        key: 'catalog',
        label: (
          <span>
            <AppstoreOutlined />
            Danh mục dịch vụ & Bảng giá
          </span>
        ),
        children: serviceCatalogContent,
      },
    ]

    if (isAdmin) {
      items.push({
        key: 'clinic',
        label: (
          <span>
            <EnvironmentOutlined />
            Cấu hình vận hành phòng khám
          </span>
        ),
        children: clinicConfigContent,
      })
    }

    return items
  }, [serviceCatalogContent, clinicConfigContent, isAdmin])

  return (
    <div className="services-admin-page">
      {/* Page Header */}
      <div className="services-page-header">
        <div className="header-text-block">
          <span className="header-eyebrow">QUẢN TRỊ HỆ THỐNG</span>
          <Title level={3} className="header-main-title">
            Danh mục Dịch vụ & Bảng giá Hiệu lực
          </Title>
          <Paragraph type="secondary" className="header-subtitle">
            Thiết lập danh mục dịch vụ khám, chỉ định cận lâm sàng và quản lý các phiên bản bảng giá
            đang hiệu lực để thu phí chính xác, minh bạch.
          </Paragraph>
        </div>
      </div>

      {/* Main Tabs */}
      {isAdmin ? (
        <Tabs defaultActiveKey="catalog" className="service-main-tabs" items={tabItems} />
      ) : (
        serviceCatalogContent
      )}

      {/* Modal Create / Edit Service */}
      <Modal
        open={modalOpen}
        onCancel={handleCloseModal}
        footer={null}
        width={680}
        centered
        maskClosable={!savingService}
        className="service-modal-dialog"
        title={
          <div className="service-modal-header-custom">
            <div className="modal-header-icon">
              {editingService ? <EditOutlined /> : <PlusOutlined />}
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>
                {editingService ? 'Cập nhật dịch vụ & Bảng giá' : 'Thêm mới dịch vụ khám / cận lâm sàng'}
              </Title>
              <Text type="secondary" style={{ fontSize: 13 }}>
                {editingService
                  ? 'Điều chỉnh thông tin dịch vụ, trạng thái hoặc thiết lập mức giá hiệu lực mới.'
                  : 'Nhập thông tin dịch vụ và đơn giá ban đầu để đưa vào bảng giá.'}
              </Text>
            </div>
          </div>
        }
      >
        <Form
          form={serviceForm}
          layout="vertical"
          onFinish={handleSaveService}
          className="service-modal-form"
          requiredMark="optional"
        >
          <Row gutter={[16, 12]}>
            <Col xs={24} md={10}>
              <Form.Item
                name="serviceCode"
                label="Mã dịch vụ"
                rules={[
                  { required: true, message: 'Vui lòng nhập mã dịch vụ' },
                  { max: 50, message: 'Mã không quá 50 ký tự' },
                ]}
              >
                <Input
                  size="large"
                  placeholder="VD: KHAM-NOI, XN-01"
                  disabled={Boolean(editingService)}
                  prefix={
                    editingService ? (
                      <LockOutlined style={{ color: '#94a3b8' }} />
                    ) : (
                      <TagOutlined style={{ color: '#94a3b8' }} />
                    )
                  }
                  style={{ textTransform: 'uppercase' }}
                  onChange={(e) => {
                    if (!editingService) {
                      serviceForm.setFieldValue('serviceCode', e.target.value.toUpperCase())
                    }
                  }}
                />
              </Form.Item>
            </Col>

            <Col xs={24} md={14}>
              <Form.Item
                name="name"
                label="Tên dịch vụ khám / cận lâm sàng"
                rules={[
                  { required: true, message: 'Vui lòng nhập tên dịch vụ' },
                  { max: 255, message: 'Tên không quá 255 ký tự' },
                ]}
              >
                <Input
                  size="large"
                  placeholder="VD: Khám chuyên khoa Nội, Tổng phân tích máu..."
                />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                name="price"
                label="Đơn giá áp dụng"
                rules={[
                  { required: true, message: 'Vui lòng nhập đơn giá' },
                  {
                    validator: (_, val) => {
                      if (val !== undefined && val !== null && Number(val) < 0) {
                        return Promise.reject(new Error('Đơn giá phải lớn hơn hoặc bằng 0 ₫'))
                      }
                      return Promise.resolve()
                    },
                  },
                ]}
              >
                <InputNumber
                  size="large"
                  min={0}
                  step={10000}
                  controls={false}
                  addonAfter="₫"
                  style={{ width: '100%' }}
                  formatter={(val) => `${val || ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                  parser={(val) => val?.replace(/\./g, '') || ''}
                  placeholder="0"
                />
              </Form.Item>
              {/* Quick price presets */}
              <div className="quick-price-tags">
                <Text type="secondary" style={{ fontSize: 12, marginRight: 6 }}>
                  Gợi ý:
                </Text>
                {PRESET_PRICES.map((p) => (
                  <Tag
                    key={p}
                    className="price-preset-tag"
                    onClick={() => serviceForm.setFieldValue('price', p)}
                  >
                    {formatMoney(p)}
                  </Tag>
                ))}
              </div>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                name="effectiveFrom"
                label="Ngày bắt đầu hiệu lực giá"
                rules={[{ required: true, message: 'Vui lòng chọn ngày hiệu lực' }]}
              >
                <DatePicker
                  size="large"
                  format="DD/MM/YYYY"
                  style={{ width: '100%' }}
                  placeholder="Chọn ngày áp dụng"
                />
              </Form.Item>
              <div className="quick-date-tags">
                <Text type="secondary" style={{ fontSize: 12, marginRight: 6 }}>
                  Chọn nhanh:
                </Text>
                <Tag
                  className="price-preset-tag"
                  onClick={() => serviceForm.setFieldValue('effectiveFrom', dayjs())}
                >
                  Hôm nay
                </Tag>
                <Tag
                  className="price-preset-tag"
                  onClick={() =>
                    serviceForm.setFieldValue('effectiveFrom', dayjs().add(1, 'month').startOf('month'))
                  }
                >
                  Đầu tháng sau
                </Tag>
              </div>
            </Col>

            {editingService && (
              <Col xs={24}>
                <Form.Item name="active" label="Trạng thái dịch vụ" valuePropName="checked">
                  <div className="active-switch-container">
                    <Switch
                      checked={serviceForm.getFieldValue('active')}
                      onChange={(checked) => serviceForm.setFieldValue('active', checked)}
                    />
                    <span style={{ marginLeft: 10, fontWeight: 500 }}>
                      {serviceForm.getFieldValue('active')
                        ? 'Đang hiệu lực (Sẵn sàng phục vụ & lập hóa đơn)'
                        : 'Tạm ngưng (Ẩn khỏi danh mục chỉ định khám)'}
                    </span>
                  </div>
                </Form.Item>
              </Col>
            )}
          </Row>

          <Alert
            type="info"
            showIcon
            style={{ marginTop: 8, marginBottom: 20 }}
            message={
              editingService
                ? 'Lưu ý về phiên bản giá: Khi bạn thay đổi đơn giá kèm ngày hiệu lực, hệ thống sẽ ghi nhận một phiên bản giá mới. Hóa đơn cũ đã tạo trước đây vẫn giữ nguyên đơn giá tại thời điểm lập hóa đơn.'
                : 'Đơn giá và ngày hiệu lực sẽ được kích hoạt ngay cho các chỉ định cận lâm sàng và thu phí kể từ ngày đã chọn.'
            }
          />

          <div className="service-modal-actions">
            <Button onClick={handleCloseModal} disabled={savingService} size="large">
              Hủy
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={savingService}
              size="large"
              className="btn-modal-submit"
            >
              {editingService ? 'Lưu thay đổi' : 'Tạo dịch vụ'}
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Price History Drawer */}
      <Drawer
        open={historyDrawerOpen}
        onClose={() => setHistoryDrawerOpen(false)}
        width={560}
        title={
          <div className="history-drawer-header">
            <HistoryOutlined style={{ color: '#2563eb', fontSize: 18 }} />
            <div>
              <Title level={4} style={{ margin: 0 }}>
                Lịch sử Biến động Bảng giá
              </Title>
              {selectedServiceForHistory && (
                <Text type="secondary" style={{ fontSize: 13 }}>
                  {selectedServiceForHistory.name} (<code>{selectedServiceForHistory.serviceCode}</code>)
                </Text>
              )}
            </div>
          </div>
        }
      >
        <div className="price-history-drawer-content">
          <Alert
            type="info"
            showIcon
            message="Toàn vẹn dữ liệu thu phí"
            description="Mọi biến động giá đều được lưu vết theo phiên bản thời gian. Bác sĩ và thu ngân luôn áp dụng chính xác đơn giá có hiệu lực tại ngày lập hóa đơn."
            style={{ marginBottom: 20 }}
          />

          {loadingHistory ? (
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
              <ReloadOutlined spin style={{ fontSize: 24, color: '#2563eb' }} />
              <Paragraph style={{ marginTop: 10 }}>Đang tải lịch sử giá...</Paragraph>
            </div>
          ) : priceHistoryList.length === 0 ? (
            <Empty description="Chưa có dữ liệu lịch sử giá." />
          ) : (
            <div className="price-timeline-container">
              <Timeline
                items={priceHistoryList.map((item) => {
                  const isCurrent = item.status === 'CURRENT_ACTIVE'
                  const isFuture = item.status === 'FUTURE_SCHEDULED'

                  return {
                    color: isCurrent ? '#16a34a' : isFuture ? '#2563eb' : '#94a3b8',
                    children: (
                      <div className="price-history-card">
                        <div className="price-history-card-header">
                          <div className="price-amount-wrap">
                            <span className="price-tag-big">{formatMoney(item.price)}</span>
                            {item.diffAmount !== null && item.diffAmount !== 0 && (
                              <Tag
                                color={item.diffAmount > 0 ? 'volcano' : 'green'}
                                className="price-diff-badge"
                              >
                                {item.diffAmount > 0 ? (
                                  <ArrowUpOutlined />
                                ) : (
                                  <ArrowDownOutlined />
                                )}
                                {item.diffAmount > 0 ? '+' : ''}
                                {formatMoney(item.diffAmount)} ({item.diffPercent}%)
                              </Tag>
                            )}
                          </div>
                          <Tag
                            color={
                              isCurrent ? 'success' : isFuture ? 'processing' : 'default'
                            }
                          >
                            {item.statusLabel}
                          </Tag>
                        </div>

                        <div className="price-history-meta">
                          <div>
                            <CalendarOutlined style={{ marginRight: 6, color: '#64748b' }} />
                            <span>Hiệu lực từ: </span>
                            <strong>{formatDate(item.effectiveFrom)}</strong>
                          </div>
                          {item.createdAt && (
                            <div className="price-created-time">
                              <ClockCircleOutlined style={{ marginRight: 4 }} />
                              <span>Được tạo lúc: {dayjs(item.createdAt).format('HH:mm DD/MM/YYYY')}</span>
                            </div>
                          )}
                        </div>
                      </div>
                    ),
                  }
                })}
              />
            </div>
          )}
        </div>
      </Drawer>
    </div>
  )
}

export default ServicesPage
