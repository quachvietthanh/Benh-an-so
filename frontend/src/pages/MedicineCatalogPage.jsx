import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  CloseOutlined,
  ControlOutlined,
  DashboardOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  InboxOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'
import medicineApi from '../api/medicineApi'
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'
import StockThresholdModal from '../components/pharmacy/StockThresholdModal'
import LowStockAlertTable from '../components/pharmacy/LowStockAlertTable'

const { Title, Text, Paragraph } = Typography

// Helper chuẩn hóa văn bản để so sánh trùng lắp
const normalizeText = (value) =>
  String(value ?? '')
    .trim()
    .toLocaleLowerCase('vi-VN')
    .replace(/\s+/g, ' ')

// Nhãn hiển thị tiếng Việt cho DosageForm
const DOSAGE_FORM_LABELS = {
  TABLET: 'Viên nén',
  CAPSULE: 'Viên nang',
  SYRUP: 'Siro',
  SUSPENSION: 'Hỗn dịch',
  SOLUTION: 'Dung dịch',
  INJECTION: 'Dạng tiêm',
  INFUSION: 'Dạng truyền',
  CREAM: 'Kem bôi',
  OINTMENT: 'Thuốc mỡ',
  GEL: 'Gel',
  DROPS: 'Thuốc nhỏ',
  INHALER: 'Dạng hít/xịt',
  POWDER: 'Thuốc bột',
  SUPPOSITORY: 'Thuốc đặt',
  OTHER: 'Khác',
}

// Nhãn hiển thị tiếng Việt cho AdministrationRoute
const ROUTE_LABELS = {
  ORAL: 'Uống',
  SUBLINGUAL: 'Ngậm dưới lưỡi',
  BUCCAL: 'Ngậm má',
  INTRAVENOUS: 'Tiêm tĩnh mạch (IV)',
  INTRAMUSCULAR: 'Tiêm bắp (IM)',
  SUBCUTANEOUS: 'Tiêm dưới da (SC)',
  TOPICAL: 'Bôi ngoài da',
  OPHTHALMIC: 'Nhỏ mắt',
  OTIC: 'Nhỏ tai',
  NASAL: 'Nhỏ/Xịt mũi',
  INHALATION: 'Dạng hít qua hô hấp',
  RECTAL: 'Đặt trực tràng',
  VAGINAL: 'Đặt âm đạo',
  TRANSDERMAL: 'Dán qua da',
  OTHER: 'Khác',
}

function MedicineCatalogPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user: currentUser } = useAuthContext()

  // Phân quyền theo chuẩn Acceptance Criteria:
  // CHỈ PHARMACIST hoặc ADMIN mới có quyền mở và quản lý danh mục thuốc & ngưỡng tồn
  const userRoles = Array.isArray(currentUser?.roles)
    ? currentUser.roles
    : currentUser?.role
    ? [currentUser.role]
    : []

  const normalizedRoles = userRoles.map((role) =>
    String(role || '').toUpperCase().replace(/^ROLE_/, '')
  )
  const canManageMedicineCatalog =
    normalizedRoles.includes('PHARMACIST') || normalizedRoles.includes('ADMIN')

  // Tab điều hướng
  const [activeTab, setActiveTab] = useState(
    location.state?.tab || 'catalog'
  )

  // State danh sách thuốc
  const [medicines, setMedicines] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)

  // State cảnh báo thiếu tồn kho từ /inventory/low-stock
  const [lowStockList, setLowStockList] = useState([])
  const [lowStockLoading, setLowStockLoading] = useState(false)

  // State bộ lọc và phân trang
  const [searchKeyword, setSearchKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [stockStatusFilter, setStockStatusFilter] = useState('ALL')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)

  // State Modal Thêm / Sửa thuốc
  const [modalOpen, setModalOpen] = useState(false)
  const [editingMedicine, setEditingMedicine] = useState(null)
  const [deactivatingMedicine, setDeactivatingMedicine] = useState(null)

  // State Modal Thiết lập Ngưỡng tồn
  const [thresholdModalOpen, setThresholdModalOpen] = useState(false)
  const [thresholdMedicine, setThresholdMedicine] = useState(null)

  // State Inline Quick Edit ngưỡng tồn
  const [inlineEditingId, setInlineEditingId] = useState(null)
  const [inlineValue, setInlineValue] = useState(0)
  const [inlineSaving, setInlineSaving] = useState(false)

  const [form] = Form.useForm()

  // Hàm tải danh sách cảnh báo thiếu hàng từ Backend API
  const loadLowStockAlerts = useCallback(async () => {
    if (!canManageMedicineCatalog) return
    setLowStockLoading(true)
    try {
      const res = await pharmacyApi.lowStock()
      const list = Array.isArray(res?.data) ? res.data : []
      setLowStockList(list)
    } catch {
      setLowStockList([])
    } finally {
      setLowStockLoading(false)
    }
  }, [canManageMedicineCatalog])

  // Hàm tải danh sách thuốc từ Backend API
  const loadMedicines = useCallback(async () => {
    if (!canManageMedicineCatalog) return

    setLoading(true)
    setErrorMessage(null)
    try {
      const activeParam =
        statusFilter === 'ALL'
          ? undefined
          : statusFilter === 'ACTIVE'
          ? true
          : false

      const res = await medicineApi.search({
        keyword: searchKeyword.trim() || undefined,
        active: activeParam,
        page: page - 1,
        size: pageSize,
      })

      const responseData = res?.data
      const content = responseData?.content
        ? responseData.content
        : Array.isArray(responseData)
        ? responseData
        : []

      setMedicines(Array.isArray(content) ? content : [])
      setTotalElements(responseData?.totalElements ?? content.length)
    } catch (err) {
      const status = err.response?.status
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải danh mục thuốc từ máy chủ.'

      if (status === 403) {
        setErrorMessage('Bạn không có quyền quản lý danh mục thuốc.')
      } else if (status === 401) {
        setErrorMessage('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
      } else {
        setErrorMessage(`Lỗi máy chủ (${status || 500}): ${msg}`)
      }
      setMedicines([])
    } finally {
      setLoading(false)
    }
  }, [canManageMedicineCatalog, searchKeyword, statusFilter, page, pageSize])

  const refreshAll = useCallback(() => {
    loadMedicines()
    loadLowStockAlerts()
  }, [loadMedicines, loadLowStockAlerts])

  useEffect(() => {
    loadMedicines()
  }, [loadMedicines])

  useEffect(() => {
    loadLowStockAlerts()
  }, [loadLowStockAlerts])

  // Xử lý mở Modal Thêm thuốc
  const openAddModal = () => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }
    setEditingMedicine(null)
    form.resetFields()
    form.setFieldsValue({
      medicineCode: `MED-${Date.now().toString().slice(-6)}`,
      dosageForm: 'TABLET',
      defaultRoute: 'ORAL',
      unit: 'Viên',
      minStockThreshold: 10,
    })
    setModalOpen(true)
  }

  // Xử lý mở Modal Sửa thuốc
  const openEditModal = (record) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }
    setEditingMedicine(record)
    form.setFieldsValue({
      medicineCode: record.medicineCode || '',
      medicineName: record.medicineName || '',
      activeIngredient: record.activeIngredient || '',
      strength: record.strength || '',
      dosageForm: record.dosageForm || 'TABLET',
      unit: record.unit || '',
      defaultRoute: record.defaultRoute || 'ORAL',
      minStockThreshold: record.minStockThreshold ?? 0,
    })
    setModalOpen(true)
  }

  // Xử lý mở Modal Thiết lập Ngưỡng tồn
  const openThresholdModal = (record) => {
    setThresholdMedicine(record)
    setThresholdModalOpen(true)
  }

  // Xử lý Lưu form Thêm / Sửa thuốc
  const handleSaveMedicine = async (values) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }

    const trimmedCode = String(values.medicineCode ?? '').trim()
    const trimmedName = String(values.medicineName ?? '').trim()
    const trimmedActive = String(values.activeIngredient ?? '').trim()
    const trimmedStrength = String(values.strength ?? '').trim()
    const trimmedUnit = String(values.unit ?? '').trim()
    const dosageFormVal = values.dosageForm
    const defaultRouteVal = values.defaultRoute
    const thresholdVal = Number(values.minStockThreshold ?? 0)

    // Validations bắt buộc
    if (!trimmedName) {
      message.error('Vui lòng nhập tên thuốc.')
      return
    }
    if (!trimmedUnit) {
      message.error('Vui lòng nhập đơn vị tính.')
      return
    }
    if (!trimmedActive) {
      message.error('Vui lòng nhập hoạt chất.')
      return
    }
    if (!trimmedStrength) {
      message.error('Vui lòng nhập hàm lượng.')
      return
    }
    if (!editingMedicine && !trimmedCode) {
      message.error('Vui lòng nhập mã thuốc.')
      return
    }

    // Kiểm tra trùng lắp ở Frontend (Tên thuốc + Hoạt chất)
    const targetKey =
      normalizeText(trimmedName) + '_' + normalizeText(trimmedActive)

    const isDuplicate = medicines.some((m) => {
      if (editingMedicine && String(m.id) === String(editingMedicine.id)) {
        return false
      }
      const existingKey =
        normalizeText(m.medicineName) + '_' + normalizeText(m.activeIngredient)
      return existingKey === targetKey
    })

    if (isDuplicate) {
      message.warning('Thuốc đã tồn tại trong danh mục.')
      return
    }

    setSubmitting(true)
    try {
      if (editingMedicine) {
        const updatePayload = {
          medicineName: trimmedName,
          activeIngredient: trimmedActive,
          strength: trimmedStrength,
          dosageForm: dosageFormVal,
          unit: trimmedUnit,
          defaultRoute: defaultRouteVal,
          minStockThreshold: thresholdVal,
        }
        await medicineApi.update(editingMedicine.id, updatePayload)
        message.success(`Đã cập nhật thuốc ${trimmedName}`)
      } else {
        const createPayload = {
          medicineCode: trimmedCode,
          medicineName: trimmedName,
          activeIngredient: trimmedActive,
          strength: trimmedStrength,
          dosageForm: dosageFormVal,
          unit: trimmedUnit,
          defaultRoute: defaultRouteVal,
          minStockThreshold: thresholdVal,
        }
        await medicineApi.create(createPayload)
        message.success(`Đã thêm thuốc mới ${trimmedName} vào danh mục thành công`)
      }

      setModalOpen(false)
      setEditingMedicine(null)
      form.resetFields()
      refreshAll()
    } catch (err) {
      const status = err.response?.status
      const msg = err.response?.data?.message || err.message

      if (status === 409) {
        message.error(msg || 'Thuốc đã tồn tại trong danh mục.')
      } else if (status === 400) {
        message.error(msg || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.')
      } else {
        message.error(msg || 'Không thể lưu dữ liệu thuốc lên máy chủ.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  // Xử lý Inline Quick Edit Ngưỡng tồn
  const startInlineEdit = (record) => {
    setInlineEditingId(record.id)
    setInlineValue(Number(record.minStockThreshold ?? 0))
  }

  const cancelInlineEdit = () => {
    setInlineEditingId(null)
    setInlineValue(0)
  }

  const saveInlineEdit = async (record) => {
    const nextThreshold = Number(inlineValue ?? 0)
    if (nextThreshold < 0) {
      message.error('Ngưỡng tồn không được âm.')
      return
    }

    setInlineSaving(true)
    try {
      const payload = {
        medicineName: record.medicineName,
        activeIngredient: record.activeIngredient,
        strength: record.strength,
        dosageForm: record.dosageForm,
        unit: record.unit,
        defaultRoute: record.defaultRoute,
        minStockThreshold: nextThreshold,
      }
      await medicineApi.update(record.id, payload)
      message.success(`Đã đổi ngưỡng tồn của ${record.medicineName} thành ${nextThreshold} ${record.unit || ''}`)
      setInlineEditingId(null)
      refreshAll()
    } catch (err) {
      message.error(err?.response?.data?.message || err?.message || 'Không thể lưu ngưỡng tồn.')
    } finally {
      setInlineSaving(false)
    }
  }

  // Xử lý Ngừng sử dụng / Kích hoạt lại thuốc
  const handleToggleStatus = async (record, targetActiveState) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }

    try {
      await medicineApi.updateStatus(record.id, targetActiveState)
      message.success(
        `Đã ${targetActiveState ? 'kích hoạt lại' : 'ngừng sử dụng'} thuốc ${
          record.medicineName
        }`
      )
      setDeactivatingMedicine(null)
      refreshAll()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể thay đổi trạng thái thuốc.'
      message.error(msg)
    }
  }

  // Lọc thuốc theo trạng thái tồn kho ở Frontend (cho trang hiện tại)
  const filteredMedicines = useMemo(() => {
    let list = Array.isArray(medicines) ? medicines : []

    if (stockStatusFilter === 'LOW_STOCK') {
      list = list.filter(
        (m) =>
          Number(m.minStockThreshold || 0) > 0 &&
          Number(m.stockQuantity || 0) < Number(m.minStockThreshold || 0)
      )
    } else if (stockStatusFilter === 'OUT_OF_STOCK') {
      list = list.filter((m) => Number(m.stockQuantity || 0) === 0)
    } else if (stockStatusFilter === 'SAFE') {
      list = list.filter(
        (m) =>
          Number(m.minStockThreshold || 0) === 0 ||
          Number(m.stockQuantity || 0) >= Number(m.minStockThreshold || 0)
      )
    } else if (stockStatusFilter === 'UNSET') {
      list = list.filter((m) => Number(m.minStockThreshold || 0) === 0)
    }

    return list
  }, [medicines, stockStatusFilter])

  // Thống kê nhanh
  const stats = useMemo(() => {
    const total = totalElements || medicines.length
    const lowStockCount = lowStockList.length
    const outOfStockCount = lowStockList.filter(
      (item) => Number(item.eligibleStockQuantity || item.stockQuantity || 0) === 0
    ).length
    const safeCount = Math.max(total - lowStockCount, 0)

    return { total, lowStockCount, outOfStockCount, safeCount }
  }, [totalElements, medicines, lowStockList])

  // Chặn tài khoản không thuộc workspace dược
  if (!canManageMedicineCatalog) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          showIcon
          icon={<StopOutlined />}
          message="Từ chối truy cập"
          description="Bạn không có quyền quản lý danh mục và ngưỡng tồn thuốc. Chức năng này dành cho Dược sĩ hoặc Quản trị viên."
          style={{ maxWidth: 600, margin: '40px auto' }}
        />
      </div>
    )
  }

  const columns = [
    {
      title: 'Mã thuốc',
      dataIndex: 'medicineCode',
      key: 'medicineCode',
      width: 120,
      render: (v) => <Text code>{v || '—'}</Text>,
    },
    {
      title: 'Tên thuốc & Hoạt chất',
      key: 'medicineInfo',
      render: (_, record) => (
        <Space direction="vertical" size={1}>
          <strong>{record.medicineName || '—'}</strong>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {[record.activeIngredient, record.strength].filter(Boolean).join(' · ')}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Dạng bào chế',
      dataIndex: 'dosageForm',
      key: 'dosageForm',
      width: 120,
      render: (v) => (v ? DOSAGE_FORM_LABELS[v] || v : '—'),
    },
    {
      title: 'Đơn vị',
      dataIndex: 'unit',
      key: 'unit',
      width: 90,
      align: 'center',
      render: (v) => <Tag color="blue">{v || '—'}</Tag>,
    },
    {
      title: 'Tồn kho thực tế',
      dataIndex: 'stockQuantity',
      key: 'stockQuantity',
      width: 130,
      align: 'right',
      render: (val, record) => {
        const stock = Number(val || 0)
        const threshold = Number(record.minStockThreshold || 0)
        const isOutOfStock = stock === 0
        const isLow = threshold > 0 && stock < threshold

        return (
          <Space direction="vertical" size={2} align="end">
            <span
              style={{
                fontWeight: 700,
                color: isOutOfStock ? '#dc2626' : isLow ? '#d97706' : '#16a34a',
              }}
            >
              {stock.toLocaleString('vi-VN')} {record.unit || ''}
            </span>
            {isOutOfStock ? (
              <Tag color="red" style={{ margin: 0 }}>Hết hàng</Tag>
            ) : isLow ? (
              <Tag color="orange" style={{ margin: 0 }}>Dưới ngưỡng</Tag>
            ) : (
              <Tag color="green" style={{ margin: 0 }}>An toàn</Tag>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Ngưỡng tồn tối thiểu',
      dataIndex: 'minStockThreshold',
      key: 'minStockThreshold',
      width: 200,
      align: 'right',
      render: (val, record) => {
        const isInline = inlineEditingId === record.id
        const thresholdNum = Number(val || 0)

        if (isInline) {
          return (
            <Space size="small">
              <InputNumber
                size="small"
                min={0}
                max={1000000}
                precision={0}
                value={inlineValue}
                style={{ width: 90 }}
                autoFocus
                onChange={(num) => setInlineValue(Number(num ?? 0))}
                onPressEnter={() => saveInlineEdit(record)}
              />
              <Button
                type="primary"
                size="small"
                icon={<CheckOutlined />}
                loading={inlineSaving}
                onClick={() => saveInlineEdit(record)}
              />
              <Button
                size="small"
                icon={<CloseOutlined />}
                disabled={inlineSaving}
                onClick={cancelInlineEdit}
              />
            </Space>
          )
        }

        return (
          <Space size="small">
            <span
              style={{
                fontWeight: 600,
                color: thresholdNum === 0 ? '#94a3b8' : '#1e293b',
              }}
            >
              {thresholdNum === 0 ? (
                <Text type="secondary" italic>Chưa đặt (0)</Text>
              ) : (
                `${thresholdNum.toLocaleString('vi-VN')} ${record.unit || ''}`
              )}
            </span>
            <Tooltip title="Chỉnh sửa nhanh ngưỡng tồn">
              <Button
                type="text"
                size="small"
                icon={<EditOutlined style={{ color: '#1677ff' }} />}
                onClick={() => startInlineEdit(record)}
              />
            </Tooltip>
          </Space>
        )
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 120,
      render: (val) => (
        <Tag color={val !== false ? 'green' : 'default'}>
          {val !== false ? 'Đang dùng' : 'Ngừng dùng'}
        </Tag>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 220,
      align: 'center',
      render: (_, record) => (
        <Space size="small" wrap>
          <Tooltip title="Thiết lập chi tiết ngưỡng tồn kho & xem trước cảnh báo">
            <Button
              size="small"
              icon={<ControlOutlined />}
              onClick={() => openThresholdModal(record)}
            >
              Ngưỡng tồn
            </Button>
          </Tooltip>

          <Button
            icon={<EditOutlined />}
            size="small"
            onClick={() => openEditModal(record)}
          >
            Sửa
          </Button>

          {record.active !== false ? (
            <Button
              danger
              size="small"
              icon={<StopOutlined />}
              onClick={() => setDeactivatingMedicine(record)}
            >
              Ngừng
            </Button>
          ) : (
            <Button
              type="primary"
              ghost
              size="small"
              icon={<CheckCircleOutlined />}
              onClick={() => handleToggleStatus(record, true)}
            >
              Kích hoạt
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24, paddingBottom: 40 }}>
      {/* Tiêu đề & Mô tả */}
      <div
        className="page-header"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
          marginBottom: 16,
        }}
      >
        <div>
          <Title level={2} style={{ margin: 0 }}>
            <MedicineBoxOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Quản lý Danh mục & Ngưỡng tồn thuốc
          </Title>
          <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
            Thiết lập ngưỡng tồn tối thiểu cho từng loại thuốc để hệ thống tự động cảnh báo bổ sung trước khi hết hàng.
          </Paragraph>
        </div>

        <Space wrap>
          <Button
            icon={<InboxOutlined />}
            onClick={() => navigate('/pharmacy/receipts')}
          >
            Nhập kho theo lô
          </Button>
          <Button
            icon={<ReloadOutlined />}
            loading={loading || lowStockLoading}
            onClick={refreshAll}
          >
            Làm mới
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={openAddModal}
          >
            Thêm thuốc mới
          </Button>
        </Space>
      </div>

      {/* Hiển thị lỗi API nếu có */}
      {errorMessage && (
        <Alert
          type="error"
          showIcon
          message={errorMessage}
          action={
            <Button
              size="small"
              type="primary"
              danger
              icon={<ReloadOutlined />}
              onClick={refreshAll}
            >
              Thử lại
            </Button>
          }
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Dashboard KPI Metric Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Tổng số loại thuốc"
              value={stats.total}
              prefix={<MedicineBoxOutlined style={{ color: '#1677ff' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              borderRadius: 8,
              borderLeft: stats.lowStockCount > 0 ? '4px solid #faad14' : undefined,
            }}
          >
            <Statistic
              title="Thuốc dưới ngưỡng tồn"
              value={stats.lowStockCount}
              valueStyle={stats.lowStockCount > 0 ? { color: '#faad14', fontWeight: 700 } : undefined}
              prefix={<WarningOutlined />}
              suffix={
                stats.lowStockCount > 0 && (
                  <Button
                    type="link"
                    size="small"
                    style={{ padding: 0, marginLeft: 8 }}
                    onClick={() => setActiveTab('alerts')}
                  >
                    Xem chi tiết →
                  </Button>
                )
              }
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              borderRadius: 8,
              borderLeft: stats.outOfStockCount > 0 ? '4px solid #ff4d4f' : undefined,
            }}
          >
            <Statistic
              title="Thuốc đã hết hàng"
              value={stats.outOfStockCount}
              valueStyle={stats.outOfStockCount > 0 ? { color: '#ff4d4f', fontWeight: 700 } : undefined}
              prefix={<AlertOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Tồn kho đạt mức an toàn"
              value={stats.safeCount}
              valueStyle={{ color: '#52c41a' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Tabs điều hướng */}
      <Card styles={{ body: { padding: '16px 20px' } }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'catalog',
              label: (
                <Space>
                  <MedicineBoxOutlined />
                  <span>Danh mục & Thiết lập ngưỡng tồn</span>
                </Space>
              ),
              children: (
                <div>
                  {/* Bộ lọc & Tìm kiếm */}
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
                    <Space wrap size="middle">
                      <Input.Search
                        placeholder="Tìm theo tên thuốc, hoạt chất, mã..."
                        allowClear
                        style={{ width: 320 }}
                        onSearch={(val) => {
                          setSearchKeyword(val)
                          setPage(1)
                        }}
                        onChange={(e) => {
                          if (!e.target.value) {
                            setSearchKeyword('')
                            setPage(1)
                          }
                        }}
                      />

                      <Select
                        defaultValue="ALL"
                        value={statusFilter}
                        style={{ width: 160 }}
                        onChange={(val) => {
                          setStatusFilter(val)
                          setPage(1)
                        }}
                        options={[
                          { value: 'ALL', label: 'Tất cả trạng thái' },
                          { value: 'ACTIVE', label: 'Đang dùng' },
                          { value: 'INACTIVE', label: 'Ngừng dùng' },
                        ]}
                      />

                      <Select
                        defaultValue="ALL"
                        value={stockStatusFilter}
                        style={{ width: 220 }}
                        onChange={setStockStatusFilter}
                        options={[
                          { value: 'ALL', label: 'Tất cả tình trạng tồn' },
                          { value: 'LOW_STOCK', label: '⚠️ Dưới ngưỡng tồn (Cần nhập)' },
                          { value: 'OUT_OF_STOCK', label: '🔴 Đã hết hàng' },
                          { value: 'SAFE', label: '🟢 Đạt mức an toàn' },
                          { value: 'UNSET', label: '⚪ Chưa đặt ngưỡng (0)' },
                        ]}
                      />
                    </Space>
                  </div>

                  {/* Bảng Danh mục & Ngưỡng tồn */}
                  <Table
                    rowKey="id"
                    columns={columns}
                    dataSource={filteredMedicines}
                    loading={loading}
                    pagination={false}
                    scroll={{ x: 1050 }}
                    locale={{ emptyText: 'Không tìm thấy thuốc nào phù hợp với bộ lọc' }}
                  />

                  <div
                    style={{
                      marginTop: 16,
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      gap: 12,
                    }}
                  >
                    <Text type="secondary">
                      Hiển thị {filteredMedicines.length} thuốc · Trang {page} / {Math.ceil(totalElements / pageSize) || 1}
                    </Text>

                    <Pagination
                      current={page}
                      pageSize={pageSize}
                      total={totalElements}
                      showSizeChanger
                      pageSizeOptions={['10', '20', '50', '100']}
                      onChange={(p, ps) => {
                        setPage(p)
                        setPageSize(ps)
                      }}
                    />
                  </div>
                </div>
              ),
            },
            {
              key: 'alerts',
              label: (
                <Space>
                  <WarningOutlined style={{ color: stats.lowStockCount > 0 ? '#faad14' : undefined }} />
                  <span>Cảnh báo thiếu tồn kho</span>
                  {stats.lowStockCount > 0 && (
                    <Badge
                      count={stats.lowStockCount}
                      overflowCount={99}
                      style={{ backgroundColor: '#ff4d4f' }}
                    />
                  )}
                </Space>
              ),
              children: (
                <LowStockAlertTable
                  items={lowStockList}
                  loading={lowStockLoading}
                  onRefresh={loadLowStockAlerts}
                  onEditThreshold={(item) => {
                    openThresholdModal({
                      id: item.medicineId,
                      medicineCode: item.medicineCode,
                      medicineName: item.medicineName,
                      activeIngredient: item.activeIngredient,
                      unit: item.unit,
                      stockQuantity: item.stockQuantity,
                      minStockThreshold: item.minStockThreshold,
                    })
                  }}
                />
              ),
            },
          ]}
        />
      </Card>

      {/* Modal Thiết lập Ngưỡng tồn chuyên biệt */}
      <StockThresholdModal
        open={thresholdModalOpen}
        medicine={thresholdMedicine}
        onCancel={() => {
          setThresholdModalOpen(false)
          setThresholdMedicine(null)
        }}
        onSuccess={() => {
          refreshAll()
        }}
      />

      {/* Modal Thêm / Sửa thuốc đầy đủ */}
      <Modal
        title={
          editingMedicine
            ? `Sửa thông tin thuốc: ${editingMedicine.medicineName}`
            : 'Thêm thuốc mới vào danh mục'
        }
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false)
          setEditingMedicine(null)
        }}
        onOk={() => form.submit()}
        confirmLoading={submitting}
        okText={editingMedicine ? 'Cập nhật' : 'Thêm mới'}
        cancelText="Hủy"
        width={650}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSaveMedicine}
          initialValues={{
            dosageForm: 'TABLET',
            defaultRoute: 'ORAL',
            unit: 'Viên',
            minStockThreshold: 10,
          }}
        >
          <Form.Item
            name="medicineCode"
            label="Mã thuốc"
            rules={[{ required: true, message: 'Vui lòng nhập mã thuốc.' }]}
          >
            <Input
              placeholder="Nhập mã thuốc (VD: MED-001)"
              disabled={!!editingMedicine}
            />
          </Form.Item>

          <Form.Item
            name="medicineName"
            label="Tên thuốc"
            rules={[{ required: true, message: 'Vui lòng nhập tên thuốc.' }]}
          >
            <Input placeholder="Nhập tên thuốc (VD: Paracetamol 500mg)" />
          </Form.Item>

          <Form.Item
            name="activeIngredient"
            label="Hoạt chất"
            rules={[{ required: true, message: 'Vui lòng nhập hoạt chất.' }]}
          >
            <Input placeholder="Nhập hoạt chất (VD: Paracetamol)" />
          </Form.Item>

          <Form.Item
            name="strength"
            label="Hàm lượng"
            rules={[{ required: true, message: 'Vui lòng nhập hàm lượng.' }]}
          >
            <Input placeholder="Nhập hàm lượng (VD: 500 mg, 10mg/5ml...)" />
          </Form.Item>

          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              name="dosageForm"
              label="Dạng bào chế"
              rules={[{ required: true, message: 'Vui lòng chọn dạng bào chế.' }]}
              style={{ flex: 1 }}
            >
              <Select
                options={Object.entries(DOSAGE_FORM_LABELS).map(
                  ([key, val]) => ({
                    value: key,
                    label: val,
                  })
                )}
              />
            </Form.Item>

            <Form.Item
              name="defaultRoute"
              label="Đường dùng"
              rules={[{ required: true, message: 'Vui lòng chọn đường dùng.' }]}
              style={{ flex: 1 }}
            >
              <Select
                options={Object.entries(ROUTE_LABELS).map(([key, val]) => ({
                  value: key,
                  label: val,
                }))}
              />
            </Form.Item>
          </Space>

          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item
                name="unit"
                label="Đơn vị tính"
                rules={[{ required: true, message: 'Vui lòng nhập đơn vị tính.' }]}
              >
                <Input placeholder="Nhập đơn vị tính (Viên / Chai / Tuýp...)" />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                name="minStockThreshold"
                label="Ngưỡng tồn kho tối thiểu"
                rules={[{ required: true, message: 'Vui lòng nhập ngưỡng tồn kho.' }]}
                extra="Hệ thống sẽ bật cảnh báo khi lượng tồn < mức này"
              >
                <InputNumber
                  min={0}
                  max={1000000}
                  precision={0}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* Modal Xác nhận Ngừng sử dụng thuốc */}
      <Modal
        title="Xác nhận ngừng sử dụng thuốc"
        open={!!deactivatingMedicine}
        onCancel={() => setDeactivatingMedicine(null)}
        onOk={() =>
          deactivatingMedicine && handleToggleStatus(deactivatingMedicine, false)
        }
        okText="Xác nhận ngừng dùng"
        okButtonProps={{ danger: true }}
        cancelText="Hủy"
      >
        <p>
          Bạn có chắc chắn muốn <strong>ngừng sử dụng</strong> thuốc{' '}
          <strong>{deactivatingMedicine?.medicineName}</strong> (Hoạt chất:{' '}
          <em>{deactivatingMedicine?.activeIngredient}</em>) không?
        </p>
        <p style={{ color: '#8c8c8c', fontSize: 13 }}>
          * Thuốc sẽ chuyển sang trạng thái <em>Ngừng dùng</em> và không thể chọn
          khi kê đơn mới, nhưng dữ liệu vẫn lưu vết trong lịch sử hệ thống.
        </p>
      </Modal>
    </div>
  )
}

export default MedicineCatalogPage
