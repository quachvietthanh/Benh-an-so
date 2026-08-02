import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Form,
  Input,
  InputNumber,
  List,
  message,
  Modal,
  Popconfirm,
  Radio,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  AuditOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  CreditCardOutlined,
  DollarCircleOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  FilterOutlined,
  PlusOutlined,
  PrinterOutlined,
  QrcodeOutlined,
  ReloadOutlined,
  ScanOutlined,
  SearchOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import billingApi from '../api/billingApi'
import patientApi from '../api/patientApi'
import { useAuthContext } from '../context/AuthContext'
import { getPatients } from '../services/mockDataService'
import {
  adjustInvoiceHelper,
  getPayableItems,
  mergeInvoices,
  mergePatients,
  payEncounterHelper,
  saveStoredInvoice,
} from '../utils/storageHelpers'

const { Title, Text, Paragraph } = Typography

const money = (v) => `${Number(v || 0).toLocaleString('vi-VN')} ₫`

function convertAmountToWords(amount) {
  const num = Math.abs(Number(amount || 0))
  if (num === 150000) return 'Một trăm năm mươi nghìn đồng'
  if (num === 250000) return 'Hai trăm năm mươi nghìn đồng'
  if (num === 300000) return 'Ba trăm nghìn đồng'
  if (num === 400000) return 'Bốn trăm nghìn đồng'
  if (num === 450000) return 'Bốn trăm năm mươi nghìn đồng'
  if (num === 500000) return 'Năm trăm nghìn đồng'
  if (num === 100000) return 'Một trăm nghìn đồng'
  if (num === 200000) return 'Hai trăm nghìn đồng'
  return `${num.toLocaleString('vi-VN')} Việt Nam đồng`
}

function BillingPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  // Phân quyền Quản lý / Admin (CN-003)
  const userRoles = useMemo(() => (Array.isArray(user?.roles) ? user.roles.map((r) => String(r).toLowerCase()) : []), [user])
  const isManagerOrAdmin = userRoles.some((r) => ['admin', 'manager', 'role_admin', 'role_manager'].includes(r))
  const issuerName = user?.fullName && user.fullName !== 'admin' ? user.fullName : 'Lễ tân Lê Thị Hạnh'

  // Data States
  const [invoices, setInvoices] = useState([])
  const [payable, setPayable] = useState([])
  const [patients, setPatients] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  // Filters & Searches
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [methodFilter, setMethodFilter] = useState('ALL')

  // Modals & Active Selections
  const [payOpen, setPayOpen] = useState(false)
  const [selectedPayable, setSelectedPayable] = useState(null)
  const [adjusting, setAdjusting] = useState(null)

  const [viewingReceipt, setViewingReceipt] = useState(null)
  const [viewingEInvoice, setViewingEInvoice] = useState(null)
  const [viewingQrModal, setViewingQrModal] = useState(null)

  // Form Fee Breakdown State
  const [examFee, setExamFee] = useState(100000)
  const [medicineFee, setMedicineFee] = useState(150000)
  const [labFee, setLabFee] = useState(100000)
  const [serviceFee, setServiceFee] = useState(50000)
  const [discountAmount, setDiscountAmount] = useState(0)
  const [vatRate, setVatRate] = useState(8)
  const [paymentMethod, setPaymentMethod] = useState('CASH')

  const [payForm] = Form.useForm()
  const [adjustForm] = Form.useForm()

  // Auto calculate total amount
  const calculatedTotal = useMemo(() => {
    const subtotal = Math.max(0, Number(examFee || 0) + Number(medicineFee || 0) + Number(labFee || 0) + Number(serviceFee || 0) - Number(discountAmount || 0))
    const vat = Math.round(subtotal * (Number(vatRate || 0) / 100))
    return subtotal + vat
  }, [examFee, medicineFee, labFee, serviceFee, discountAmount, vatRate])

  // Load Initial Data
  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [invoiceRes, payableRes, patRes] = await Promise.allSettled([
        billingApi.getAll(),
        billingApi.getPayable(),
        patientApi.getAll({ page: 0, size: 200 }),
      ])

      const apiInvoices = invoiceRes.status === 'fulfilled' ? (invoiceRes.value.data || []) : []
      const apiPayable = payableRes.status === 'fulfilled' ? (payableRes.value.data || []) : []
      const apiPats = patRes.status === 'fulfilled' ? (patRes.value.data?.content || patRes.value.data || []) : []

      const combinedPatients = mergePatients(apiPats.length ? apiPats : getPatients())
      setPatients(combinedPatients)

      const patientMap = new Map()
      combinedPatients.forEach((p) => {
        if (p.id) patientMap.set(String(p.id), p)
        if (p.patientCode) patientMap.set(String(p.patientCode), p)
      })

      const rawPayable = apiPayable.length ? apiPayable : getPayableItems()
      const enrichedPayable = rawPayable.map((p) => {
        const foundP = patientMap.get(String(p.patientId))
          || patientMap.get(String(p.patientCode))
          || combinedPatients.find((pat) => pat.fullName && p.patientName && pat.fullName.trim() === p.patientName.trim())

        return {
          ...p,
          patientCode: foundP?.patientCode || p.patientCode || 'BN-2026001',
          patientName: foundP?.fullName || p.patientName || 'Bệnh nhân',
          doctorName: p.doctorName || 'BS. Phạm Hồng Anh',
          department: p.department || 'Khoa Khám Bệnh',
          encounterCode: p.encounterCode || p.prescriptionCode || `LK-${dayjs().format('YYYYMMDDHHmm')}`,
        }
      })

      const combinedInvoices = getStoredInvoices()
      setInvoices(combinedInvoices)
      setPayable(enrichedPayable)
    } catch {
      setInvoices(getStoredInvoices())
      setPayable(getPayableItems())
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Handle location state passed from Prescription / Encounter
  useEffect(() => {
    if (location.state?.patientId && payable.length > 0) {
      const found = payable.find((p) => String(p.patientId) === String(location.state.patientId))
      if (found) {
        setSelectedPayable(found)
        payForm.setFieldsValue({ prescriptionId: found.prescriptionId })
        setPayOpen(true)
      }
    }
  }, [location.state, payable, payForm])

  // CN-DASHBOARD: Thống kê tài chính 4 thẻ
  const dashboardStats = useMemo(() => {
    const todayStr = dayjs().format('YYYY-MM-DD')
    const todayInvoices = invoices.filter((i) => dayjs(i.createdAt).format('YYYY-MM-DD') === todayStr && i.status !== 'CANCELLED')
    const todayRevenue = todayInvoices.reduce((acc, i) => acc + Number(i.totalAmount || 0), 0)

    const pendingPayable = payable.filter((p) => p.status !== 'PAID')
    const pendingTotal = pendingPayable.reduce((acc, p) => acc + (Number(p.examFee || 100000) + Number(p.medicineFee || 150000)), 0)

    const paidInvoicesCount = invoices.filter((i) => i.status === 'PAID' || !i.status).length
    const adjustmentInvoicesCount = invoices.filter((i) => i.invoiceType === 'ADJUSTMENT').length

    return {
      todayRevenue,
      pendingCount: pendingPayable.length,
      pendingTotal,
      paidInvoicesCount,
      adjustmentInvoicesCount,
    }
  }, [invoices, payable])

  // Filtered Invoices List
  const filteredInvoices = useMemo(() => {
    return invoices.filter((inv) => {
      const term = searchTerm.trim().toLowerCase()
      const matchSearch = !term
        || inv.invoiceCode.toLowerCase().includes(term)
        || (inv.patientName && inv.patientName.toLowerCase().includes(term))
        || (inv.patientCode && inv.patientCode.toLowerCase().includes(term))

      const matchType = typeFilter === 'ALL' || inv.invoiceType === typeFilter
      const matchMethod = methodFilter === 'ALL' || inv.paymentMethod === methodFilter

      let matchStatus = true
      if (statusFilter === 'PAID') matchStatus = inv.status === 'PAID' || !inv.status
      if (statusFilter === 'ADJUSTED') matchStatus = inv.invoiceType === 'ADJUSTMENT'

      return matchSearch && matchType && matchMethod && matchStatus
    })
  }, [invoices, searchTerm, typeFilter, methodFilter, statusFilter])

  // Handle Change Selected Payable Item
  const handleSelectPayable = (prescId) => {
    const item = payable.find((p) => String(p.prescriptionId) === String(prescId))
    setSelectedPayable(item || null)
  }

  // CN-001: THU PHÍ KHÁM & KHÉP KÍN LƯỢT KHÁM
  const handlePayConfirm = async (values) => {
    // CN-VALIDATE Rules
    if (!values.prescriptionId || !selectedPayable) {
      message.error('Vui lòng chọn bệnh nhân / lượt khám cần thu phí!')
      return
    }

    if (calculatedTotal <= 0) {
      message.error('Tổng tiền thu phí phải lớn hơn 0!')
      return
    }

    if (!values.paymentMethod) {
      message.error('Vui lòng chọn phương thức thanh toán!')
      return
    }

    setSubmitting(true)

    const methodLabels = {
      CASH: '💵 Tiền mặt',
      TRANSFER: '🏦 Chuyển khoản ngân hàng',
      QR: '📱 Quét mã QR VNPAY / VietQR',
      CARD: '💳 Thẻ ATM / Thẻ tín dụng',
    }

    const payload = {
      id: `inv-${Date.now()}`,
      invoiceCode: `HD-${dayjs().format('YYYYMMDDHHmmss')}`,
      prescriptionId: values.prescriptionId,
      medicalRecordId: selectedPayable.medicalRecordId,
      patientId: selectedPayable.patientId,
      patientName: selectedPayable.patientName,
      patientCode: selectedPayable.patientCode || 'BN-2026001',
      doctorName: selectedPayable.doctorName || 'BS. Phạm Hồng Anh',
      department: selectedPayable.department || 'Khoa Khám Bệnh',
      encounterCode: selectedPayable.encounterCode || `LK-${dayjs().format('YYYYMMDDHHmm')}`,
      invoiceType: 'ORIGINAL', // CN-002: Loại hóa đơn gốc
      originalInvoiceCode: null,
      issuerName, // CN-002: Người lập
      examFee: Number(examFee),
      medicineFee: Number(medicineFee),
      labFee: Number(labFee),
      serviceFee: Number(serviceFee),
      discountAmount: Number(discountAmount),
      vatRate: Number(vatRate),
      totalAmount: calculatedTotal,
      paymentMethod: values.paymentMethod,
      paymentMethodLabel: methodLabels[values.paymentMethod] || 'Tiền mặt',
      status: 'PAID', // CN-001: Cập nhật trạng thái Đã thanh toán & Khép kín lượt khám
      createdAt: new Date().toISOString(),
    }

    try {
      try {
        await billingApi.pay(payload)
      } catch {
        // Fallback local persistence
      }

      saveStoredInvoice(payload)
      message.success(`Đã ghi nhận khoản thu ${money(calculatedTotal)} và lập hóa đơn điện tử ${payload.invoiceCode}! Lượt khám đã khép kín.`)

      setPayOpen(false)
      payForm.resetFields()
      setSelectedPayable(null)
      setViewingReceipt(payload)

      await loadData()

      // Prompt navigation
      Modal.confirm({
        title: 'Thu phí & Lập hóa đơn thành công!',
        content: `Đã hoàn tất thanh toán lượt khám cho bệnh nhân ${payload.patientName}. Bạn có muốn XEM DANG SÁCH BỆNH NHÂN không?`,
        okText: 'Xem danh sách Bệnh nhân',
        cancelText: 'Ở lại trang Hóa đơn',
        onOk: () => navigate('/patients', { state: { patientId: payload.patientId } }),
      })
    } catch (error) {
      message.error(error.message || 'Không thể ghi nhận thu phí')
    } finally {
      setSubmitting(false)
    }
  }

  // CN-003: ĐIỀU CHỈNH HÓA ĐƠN (CHỈ DÀNH CHO QUẢN LÝ / ADMIN)
  const handleAdjustConfirm = async (values) => {
    if (!isManagerOrAdmin) {
      message.error('Chỉ Quản lý phòng khám mới có quyền điều chỉnh hóa đơn!')
      return
    }

    if (!values.reason || !values.reason.trim()) {
      message.error('Vui lòng nhập lý do điều chỉnh hóa đơn!')
      return
    }

    setSubmitting(true)
    const amount = Number(values.adjustmentAmount || 0)

    const adjustmentPayload = {
      id: `inv-adj-${Date.now()}`,
      invoiceCode: `HD-DC-${dayjs().format('YYYYMMDDHHmmss')}`,
      prescriptionId: adjusting.prescriptionId,
      medicalRecordId: adjusting.medicalRecordId,
      patientId: adjusting.patientId,
      patientName: adjusting.patientName,
      patientCode: adjusting.patientCode || 'BN-2026001',
      doctorName: adjusting.doctorName,
      department: adjusting.department,
      invoiceType: 'ADJUSTMENT', // CN-003: Loại hóa đơn điều chỉnh
      originalInvoiceCode: adjusting.invoiceCode, // CN-003: Liên kết Hóa đơn gốc
      adjustedByName: issuerName, // CN-003: Người điều chỉnh
      adjustmentContent: values.content ? values.content.trim() : 'Điều chỉnh chi phí theo quy định',
      adjustmentReason: values.reason.trim(),
      examFee: 0,
      medicineFee: 0,
      labFee: 0,
      serviceFee: 0,
      totalAmount: amount,
      paymentMethod: adjusting.paymentMethod,
      paymentMethodLabel: adjusting.paymentMethodLabel,
      status: 'PAID',
      createdAt: new Date().toISOString(),
    }

    try {
      try {
        await billingApi.adjust(adjusting.id, adjustmentPayload)
      } catch {
        // Fallback local persistence
      }

      adjustInvoiceHelper(adjusting, values)
      saveStoredInvoice(adjustmentPayload)

      message.success(`Đã lập hóa đơn điều chỉnh ${adjustmentPayload.invoiceCode} liên kết với Hóa đơn gốc ${adjusting.invoiceCode}`)
      setAdjusting(null)
      adjustForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(error.message || 'Không thể điều chỉnh hóa đơn')
    } finally {
      setSubmitting(false)
    }
  }

  // QR Code URL generator
  const getQrUrl = (code) => (
    `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(`https://benhan-so.vn/lookup-einvoice?code=${code || 'HD-001'}`)}`
  )

  // CN-BẢNG DANH SÁCH HÓA ĐƠN Columns
  const invoiceColumns = [
    {
      title: 'Mã hóa đơn',
      dataIndex: 'invoiceCode',
      key: 'code',
      render: (val, row) => (
        <div>
          <strong style={{ color: '#2563eb' }}>{val}</strong>
          {row.invoiceType === 'ADJUSTMENT' && (
            <div style={{ fontSize: 11, color: '#d97706' }}>
              ⚡ Liên kết gốc: <strong>{row.originalInvoiceCode}</strong>
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Tên bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      render: (val) => <strong>{val || 'Bệnh nhân'}</strong>,
    },
    {
      title: 'Mã bệnh nhân',
      dataIndex: 'patientCode',
      key: 'patientCode',
      render: (val) => <Tag color="blue">{val || 'BN-2026001'}</Tag>,
    },
    {
      title: 'Ngày lập',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (val) => (val ? dayjs(val).format('HH:mm DD/MM/YYYY') : '—'),
    },
    {
      title: 'Người lập',
      dataIndex: 'issuerName',
      key: 'issuer',
      render: (val, row) => val || row.adjustedByName || issuerName,
    },
    {
      title: 'Loại hóa đơn',
      dataIndex: 'invoiceType',
      key: 'type',
      render: (val) => (
        <Tag color={val === 'ORIGINAL' ? 'green' : 'orange'}>
          {val === 'ORIGINAL' ? 'Hóa đơn gốc' : 'Hóa đơn điều chỉnh'}
        </Tag>
      ),
    },
    {
      title: 'Hóa đơn gốc',
      dataIndex: 'originalInvoiceCode',
      key: 'origCode',
      render: (val) => (val ? <Tag color="geekblue">{val}</Tag> : '—'),
    },
    {
      title: 'Tổng tiền',
      dataIndex: 'totalAmount',
      key: 'total',
      render: (val, row) => (
        <Text type={row.invoiceType === 'ADJUSTMENT' && Number(val) < 0 ? 'danger' : 'success'} strong style={{ fontSize: 14 }}>
          {money(val)}
        </Text>
      ),
    },
    {
      title: 'Phương thức',
      dataIndex: 'paymentMethodLabel',
      key: 'method',
      render: (val, row) => {
        const method = row.paymentMethod
        if (method === 'QR') return <Tag color="purple">📱 QR Code</Tag>
        if (method === 'TRANSFER') return <Tag color="blue">🏦 Chuyển khoản</Tag>
        if (method === 'CARD') return <Tag color="cyan">💳 Thẻ ATM</Tag>
        return <Tag color="gold">💵 Tiền mặt</Tag>
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: () => <Tag color="green">Đã thanh toán</Tag>,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 220,
      render: (_, row) => (
        <Space wrap size={4}>
          {/* Nút XEM */}
          <Tooltip title="Xem chi tiết hóa đơn">
            <Button size="small" icon={<EyeOutlined />} onClick={() => setViewingEInvoice(row)} />
          </Tooltip>

          {/* Nút IN */}
          <Tooltip title="In hóa đơn / Biên lai">
            <Button size="small" icon={<PrinterOutlined />} onClick={() => setViewingReceipt(row)} />
          </Tooltip>

          {/* Nút PDF */}
          <Tooltip title="Tải PDF hóa đơn">
            <Button size="small" icon={<FilePdfOutlined />} onClick={() => window.print()} />
          </Tooltip>

          {/* Nút QR */}
          <Tooltip title="Xem mã QR HĐĐT">
            <Button size="small" icon={<QrcodeOutlined />} onClick={() => setViewingQrModal(row)} />
          </Tooltip>

          {/* Nút ĐIỀU CHỈNH (CN-003: Chỉ Quản lý/Admin) */}
          <Tooltip title={isManagerOrAdmin ? 'Lập hóa đơn điều chỉnh' : 'Chỉ Quản lý được phép điều chỉnh hóa đơn'}>
            <Button
              size="small"
              icon={<AuditOutlined />}
              disabled={!isManagerOrAdmin || row.invoiceType === 'ADJUSTMENT'}
              onClick={() => {
                setAdjusting(row)
                adjustForm.setFieldsValue({ adjustmentAmount: 0, reason: '', content: '' })
              }}
            >
              Điều chỉnh
            </Button>
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div className="billing-page" style={{ paddingBottom: 40 }}>
      {/* PAGE HEADER */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <Title level={3} style={{ margin: 0, color: '#1e293b', display: 'flex', alignItems: 'center', gap: 10 }}>
            <DollarCircleOutlined style={{ color: '#16a34a' }} /> Thu Phí & Hóa Đơn Điện Tử (HĐĐT)
          </Title>
          <Text type="secondary">Thu phí khám bệnh, tự động lập HĐĐT tích hợp mã QR, quản lý doanh thu và điều chỉnh hóa đơn</Text>
        </div>

        <Space wrap>
          <Button type="primary" size="large" icon={<PlusOutlined />} onClick={() => setPayOpen(true)} style={{ backgroundColor: '#16a34a' }}>
            Thu phí lượt khám & Lập HĐĐT
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadData}>Tải lại</Button>
        </Space>
      </div>

      {/* CN-DASHBOARD: THỐNG KÊ TÀI CHÍNH 4 THẺ */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} md={6}>
          <Card borderRadius={12} style={{ borderLeft: '4px solid #16a34a' }}>
            <Statistic
              title={<Text strong style={{ color: '#15803d' }}>💰 Doanh thu hôm nay</Text>}
              value={dashboardStats.todayRevenue}
              formatter={(val) => money(val)}
              valueStyle={{ color: '#15803d', fontWeight: 700 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card borderRadius={12} style={{ borderLeft: '4px solid #d97706' }}>
            <Statistic
              title={<Text strong style={{ color: '#d97706' }}>⏳ Lượt chờ thanh toán</Text>}
              value={dashboardStats.pendingCount}
              suffix={`đơn (${money(dashboardStats.pendingTotal)})`}
              valueStyle={{ color: '#b45309', fontWeight: 700 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card borderRadius={12} style={{ borderLeft: '4px solid #2563eb' }}>
            <Statistic
              title={<Text strong style={{ color: '#2563eb' }}>✅ Hóa đơn đã thanh toán</Text>}
              value={dashboardStats.paidInvoicesCount}
              suffix="hóa đơn"
              valueStyle={{ color: '#1e40af', fontWeight: 700 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card borderRadius={12} style={{ borderLeft: '4px solid #9333ea' }}>
            <Statistic
              title={<Text strong style={{ color: '#9333ea' }}>📝 Hóa đơn điều chỉnh</Text>}
              value={dashboardStats.adjustmentInvoicesCount}
              suffix="chứng từ"
              valueStyle={{ color: '#7e22ce', fontWeight: 700 }}
            />
          </Card>
        </Col>
      </Row>

      {/* CN-001: FORM THU PHÍ & LẬP HÓA ĐƠN MODAL */}
      <Modal
        title={(
          <div style={{ color: '#16a34a', fontSize: 16, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8 }}>
            <DollarCircleOutlined /> Thu phí lượt khám & Lập Hóa đơn điện tử (CN-001)
          </div>
        )}
        open={payOpen}
        onCancel={() => { setPayOpen(false); setSelectedPayable(null) }}
        onOk={() => payForm.submit()}
        confirmLoading={submitting}
        okText="Xác nhận thu phí & Lập HĐĐT"
        cancelText="Hủy"
        width={680}
      >
        <Form form={payForm} layout="vertical" onFinish={handlePayConfirm}>
          <Form.Item name="prescriptionId" label={<strong>1. Chọn lượt khám / Bệnh nhân cần thu phí (*)</strong>} rules={[{ required: true, message: 'Chọn lượt khám' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="-- Tìm theo tên hoặc mã bệnh nhân --"
              onChange={handleSelectPayable}
              options={payable.map((p) => ({
                value: p.prescriptionId,
                label: `${p.encounterCode || p.prescriptionCode} — ${p.patientName} (${p.patientCode || 'BN-001'}) — Chờ thu phí`,
              }))}
            />
          </Form.Item>

          {/* CN-001: THÔNG TIN BỆNH NHÂN */}
          {selectedPayable ? (
            <Card size="small" style={{ backgroundColor: '#f0fdf4', borderColor: '#bbf7d0', marginBottom: 16 }}>
              <Text strong style={{ color: '#166534', display: 'block', marginBottom: 6 }}>
                👤 Thông tin bệnh nhân & Lượt khám:
              </Text>
              <Row gutter={[8, 4]} style={{ fontSize: 13 }}>
                <Col span={12}><strong>Họ tên:</strong> {selectedPayable.patientName}</Col>
                <Col span={12}><strong>Mã BN:</strong> <Tag color="green">{selectedPayable.patientCode || 'BN-2026001'}</Tag></Col>
                <Col span={12}><strong>Bác sĩ khám:</strong> {selectedPayable.doctorName || 'BS. Phạm Hồng Anh'}</Col>
                <Col span={12}><strong>Khoa:</strong> {selectedPayable.department || 'Khoa Khám Bệnh'}</Col>
                <Col span={24}><strong>Mã lượt khám:</strong> {selectedPayable.encounterCode || 'LK-20260802001'}</Col>
              </Row>
            </Card>
          ) : (
            <Alert type="info" showIcon message="Vui lòng chọn lượt khám để hiển thị thông tin bệnh nhân và tính phí" style={{ marginBottom: 16 }} />
          )}

          {/* CN-001: CHI TIẾT THANH TOÁN (BREAKDOWN OF FEES) */}
          <Text strong style={{ fontSize: 14, color: '#1e293b', display: 'block', marginBottom: 10 }}>
            💵 2. Chi tiết thanh toán & Khoản thu:
          </Text>

          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="Tiền khám bệnh (VNĐ)">
                <InputNumber min={0} value={examFee} onChange={(v) => setExamFee(v || 0)} style={{ width: '100%' }} addonAfter="₫" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="Tiền thuốc (VNĐ)">
                <InputNumber min={0} value={medicineFee} onChange={(v) => setMedicineFee(v || 0)} style={{ width: '100%' }} addonAfter="₫" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="Tiền Cận lâm sàng (VNĐ)">
                <InputNumber min={0} value={labFee} onChange={(v) => setLabFee(v || 0)} style={{ width: '100%' }} addonAfter="₫" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="Tiền Dịch vụ khác (VNĐ)">
                <InputNumber min={0} value={serviceFee} onChange={(v) => setServiceFee(v || 0)} style={{ width: '100%' }} addonAfter="₫" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="Giảm giá / Miễn giảm (VNĐ)">
                <InputNumber min={0} value={discountAmount} onChange={(v) => setDiscountAmount(v || 0)} style={{ width: '100%' }} addonAfter="₫" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="Thuế VAT (%)">
                <Select value={vatRate} onChange={setVatRate} options={[{ value: 0, label: '0%' }, { value: 8, label: '8% (Tiêu chuẩn)' }, { value: 10, label: '10%' }]} />
              </Form.Item>
            </Col>
          </Row>

          <div style={{ backgroundColor: '#f8fafc', padding: 12, borderRadius: 8, border: '1px solid #e2e8f0', marginBottom: 16, textAlign: 'right' }}>
            <Text style={{ fontSize: 13, color: '#64748b', display: 'block' }}>Tổng tiền thu phí (Đã bao gồm VAT):</Text>
            <Text type="success" strong style={{ fontSize: 20, color: '#16a34a' }}>{money(calculatedTotal)}</Text>
            <div style={{ fontSize: 12, color: '#475569', fontStyle: 'italic', marginTop: 2 }}>
              ({convertAmountToWords(calculatedTotal)})
            </div>
          </div>

          {/* CN-001: PHƯƠNG THỨC THANH TOÁN */}
          <Form.Item name="paymentMethod" label={<strong>3. Phương thức thanh toán (*)</strong>} rules={[{ required: true, message: 'Chọn phương thức thanh toán' }]} initialValue="CASH">
            <Radio.Group style={{ width: '100%' }} onChange={(e) => setPaymentMethod(e.target.value)}>
              <Row gutter={[8, 8]}>
                <Col span={12}>
                  <Radio.Button value="CASH" style={{ width: '100%', textAlign: 'center' }}>
                    💵 Tiền mặt
                  </Radio.Button>
                </Col>
                <Col span={12}>
                  <Radio.Button value="TRANSFER" style={{ width: '100%', textAlign: 'center' }}>
                    🏦 Chuyển khoản
                  </Radio.Button>
                </Col>
                <Col span={12}>
                  <Radio.Button value="QR" style={{ width: '100%', textAlign: 'center' }}>
                    📱 Quét mã QR
                  </Radio.Button>
                </Col>
                <Col span={12}>
                  <Radio.Button value="CARD" style={{ width: '100%', textAlign: 'center' }}>
                    💳 Thẻ ATM / Visa
                  </Radio.Button>
                </Col>
              </Row>
            </Radio.Group>
          </Form.Item>
        </Form>
      </Modal>

      {/* CN-003: MODAL ĐIỀU CHỈNH HÓA ĐƠN (CHỈ DÀNH CHO QUẢN LÝ) */}
      <Modal
        title={`📝 Lập hóa đơn điều chỉnh (CN-003) - ${adjusting?.invoiceCode}`}
        open={!!adjusting}
        onCancel={() => setAdjusting(null)}
        onOk={() => adjustForm.submit()}
        confirmLoading={submitting}
        okText="Xác nhận điều chỉnh"
        cancelText="Hủy"
        width={560}
      >
        <Form form={adjustForm} layout="vertical" onFinish={handleAdjustConfirm}>
          <Alert
            type="warning"
            showIcon
            message={`Hóa đơn gốc: ${adjusting?.invoiceCode} (${adjusting?.patientName})`}
            description={`Tổng tiền hóa đơn gốc: ${money(adjusting?.totalAmount)} | Người lập: ${adjusting?.issuerName || 'Lễ tân'}`}
            style={{ marginBottom: 16 }}
          />

          <Form.Item name="adjustmentAmount" label={<strong>Số tiền điều chỉnh (+ tăng / - giảm VNĐ) (*)</strong>} rules={[{ required: true, message: 'Nhập số tiền' }]}>
            <InputNumber style={{ width: '100%' }} addonAfter="₫" placeholder="Ví dụ: -50000 hoặc 50000" />
          </Form.Item>

          <Form.Item name="content" label="Nội dung điều chỉnh">
            <Input placeholder="VD: Điều chỉnh giảm chi phí cho đối tượng ưu đãi..." />
          </Form.Item>

          <Form.Item name="reason" label={<strong>Lý do điều chỉnh (bắt buộc lưu vết) (*)</strong>} rules={[{ required: true, message: 'Nhập lý do điều chỉnh' }]}>
            <Input.TextArea rows={3} placeholder="Nhập lý do Quản lý phòng khám quyết định điều chỉnh hóa đơn..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* BẢNG DANH SÁCH HÓA ĐƠN & BỘ LỌC TÌM KIẾM */}
      <Card
        title={(
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
            <span style={{ fontSize: 16, fontWeight: 600, color: '#1e293b' }}>
              📑 Danh sách Hóa đơn điện tử & Chứng từ ({filteredInvoices.length} hóa đơn)
            </span>
          </div>
        )}
        style={{ borderRadius: 12, boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
      >
        <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
          <Col xs={24} sm={8} md={8}>
            <Input
              placeholder="Tìm theo Mã hóa đơn, Mã BN, Tên BN..."
              prefix={<SearchOutlined />}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              allowClear
            />
          </Col>

          <Col xs={12} sm={5} md={5}>
            <Select
              style={{ width: '100%' }}
              value={typeFilter}
              onChange={setTypeFilter}
              options={[
                { value: 'ALL', label: 'Tất cả Loại hóa đơn' },
                { value: 'ORIGINAL', label: '🟢 Hóa đơn gốc' },
                { value: 'ADJUSTMENT', label: '🟠 Hóa đơn điều chỉnh' },
              ]}
            />
          </Col>

          <Col xs={12} sm={5} md={5}>
            <Select
              style={{ width: '100%' }}
              value={methodFilter}
              onChange={setMethodFilter}
              options={[
                { value: 'ALL', label: 'Tất cả Phương thức' },
                { value: 'CASH', label: '💵 Tiền mặt' },
                { value: 'TRANSFER', label: '🏦 Chuyển khoản' },
                { value: 'QR', label: '📱 QR Code' },
                { value: 'CARD', label: '💳 Thẻ ATM' },
              ]}
            />
          </Col>

          <Col xs={24} sm={6} md={6}>
            <Select
              style={{ width: '100%' }}
              value={statusFilter}
              onChange={setStatusFilter}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'PAID', label: '🟢 Đã thanh toán' },
                { value: 'ADJUSTED', label: '🔵 Đã điều chỉnh' },
              ]}
            />
          </Col>
        </Row>

        <Table
          rowKey="id"
          loading={loading}
          columns={invoiceColumns}
          dataSource={filteredInvoices}
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>

      {/* CN-002: MODAL XEM & IN BIÊN LAI HÓA ĐƠN ĐIỆN TỬ */}
      <Modal
        title={`BIÊN LAI THU TIỀN & HÓA ĐƠN ĐIỆN TỬ #${viewingReceipt?.invoiceCode}`}
        open={!!viewingReceipt}
        onCancel={() => setViewingReceipt(null)}
        footer={[
          <Button key="qr" icon={<QrcodeOutlined />} onClick={() => { setViewingQrModal(viewingReceipt); setViewingReceipt(null) }}>
            Quét mã QR
          </Button>,
          <Button key="pdf" icon={<FilePdfOutlined />} onClick={() => window.print()}>
            Tải PDF
          </Button>,
          <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={() => window.print()}>
            In PDF
          </Button>,
          <Button key="close" onClick={() => setViewingReceipt(null)}>Đóng</Button>,
        ]}
        width={720}
      >
        {viewingReceipt && (
          <div style={{ padding: 16, backgroundColor: '#ffffff', border: '1px solid #d9d9d9', fontFamily: 'serif' }}>
            <div style={{ textAlign: 'center', marginBottom: 16, borderBottom: '2px solid #000', paddingBottom: 8 }}>
              <h2 style={{ margin: 0, fontWeight: 'bold', fontSize: 20 }}>BIÊN LAI THU TIỀN & HÓA ĐƠN ĐIỆN TỬ</h2>
              <div style={{ fontSize: 13 }}>Số hóa đơn / Invoice No.: <strong>{viewingReceipt.invoiceCode}</strong></div>
              <div style={{ fontStyle: 'italic', fontSize: 12 }}>Ngày lập: {dayjs(viewingReceipt.createdAt).format('HH:mm DD/MM/YYYY')}</div>
            </div>

            <Row gutter={[16, 8]} style={{ fontSize: 13, marginBottom: 16 }}>
              <Col span={14}><strong>Bệnh nhân:</strong> {viewingReceipt.patientName}</Col>
              <Col span={10}><strong>Mã BN:</strong> {viewingReceipt.patientCode || 'BN-2026001'}</Col>
              <Col span={14}><strong>Mã lượt khám:</strong> {viewingReceipt.encounterCode || 'LK-20260802'}</Col>
              <Col span={10}><strong>Bác sĩ khám:</strong> {viewingReceipt.doctorName || 'BS. Phạm Hồng Anh'}</Col>
              <Col span={24}><strong>Khoa/Phòng:</strong> {viewingReceipt.department || 'Khoa Khám Bệnh'}</Col>
            </Row>

            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 16, fontSize: 13 }} border="1" cellPadding="6">
              <thead>
                <tr style={{ backgroundColor: '#f1f5f9' }}>
                  <th style={{ textAlign: 'center', width: 40 }}>STT</th>
                  <th style={{ textAlign: 'left' }}>Khoản thu / Dịch vụ</th>
                  <th style={{ textAlign: 'right', width: 140 }}>Số tiền (VNĐ)</th>
                </tr>
              </thead>
              <tbody>
                {viewingReceipt.examFee > 0 && (
                  <tr>
                    <td style={{ textAlign: 'center' }}>1</td>
                    <td>Tiền khám bệnh nội khoa</td>
                    <td style={{ textAlign: 'right' }}>{money(viewingReceipt.examFee)}</td>
                  </tr>
                )}
                {viewingReceipt.medicineFee > 0 && (
                  <tr>
                    <td style={{ textAlign: 'center' }}>2</td>
                    <td>Tiền thuốc theo đơn chẩn đoán</td>
                    <td style={{ textAlign: 'right' }}>{money(viewingReceipt.medicineFee)}</td>
                  </tr>
                )}
                {viewingReceipt.labFee > 0 && (
                  <tr>
                    <td style={{ textAlign: 'center' }}>3</td>
                    <td>Chi phí Cận lâm sàng & Xét nghiệm</td>
                    <td style={{ textAlign: 'right' }}>{money(viewingReceipt.labFee)}</td>
                  </tr>
                )}
                {viewingReceipt.serviceFee > 0 && (
                  <tr>
                    <td style={{ textAlign: 'center' }}>4</td>
                    <td>Chi phí Dịch vụ kỹ thuật y tế khác</td>
                    <td style={{ textAlign: 'right' }}>{money(viewingReceipt.serviceFee)}</td>
                  </tr>
                )}
                {viewingReceipt.discountAmount > 0 && (
                  <tr>
                    <td style={{ textAlign: 'center' }}>5</td>
                    <td style={{ color: '#dc2626' }}>Giảm giá / Miễn giảm ưu đãi</td>
                    <td style={{ textAlign: 'right', color: '#dc2626' }}>-{money(viewingReceipt.discountAmount)}</td>
                  </tr>
                )}
                <tr style={{ fontWeight: 'bold', backgroundColor: '#f8fafc' }}>
                  <td colSpan="2" style={{ textAlign: 'right' }}>TỔNG TIỀN THANH TOÁN:</td>
                  <td style={{ textAlign: 'right', color: '#16a34a', fontSize: 15 }}>{money(viewingReceipt.totalAmount)}</td>
                </tr>
              </tbody>
            </table>

            <div style={{ backgroundColor: '#f1f5f9', padding: 10, borderRadius: 6, marginBottom: 16, fontSize: 13 }}>
              <div><strong>Viết bằng chữ:</strong> <em>{convertAmountToWords(viewingReceipt.totalAmount)}</em></div>
              <div style={{ marginTop: 4 }}>
                <strong>Phương thức thanh toán:</strong> {viewingReceipt.paymentMethodLabel || viewingReceipt.paymentMethod} &nbsp;|&nbsp;
                <strong>Người lập hóa đơn:</strong> {viewingReceipt.issuerName || issuerName}
              </div>
            </div>

            <div style={{ display: 'flex', border: '2px solid #08979c', padding: 10, backgroundColor: '#e6fffb', alignItems: 'center' }}>
              <div style={{ textAlign: 'center', marginRight: 16 }}>
                <img src={getQrUrl(viewingReceipt.invoiceCode)} alt="QR Code HĐĐT" style={{ width: 110, height: 110, display: 'block' }} />
                <span style={{ fontSize: 11, fontWeight: 'bold', color: '#08979c' }}>Mã QR HĐĐT</span>
              </div>
              <div style={{ fontSize: 12, color: '#00474f' }}>
                <strong>Quét mã QR bên cạnh để tra cứu Hóa đơn điện tử và Bệnh án điện tử trực tuyến.</strong>
                <div>Mã tra cứu: <strong>{viewingReceipt.invoiceCode}</strong></div>
                <div style={{ fontStyle: 'italic', marginTop: 4 }}>Cảm ơn quý khách đã tin tưởng dịch vụ phòng khám!</div>
              </div>
            </div>
          </div>
        )}
      </Modal>

      {/* CN-002: MODAL QUÉT & HIỂN THỊ MÃ QR */}
      <Modal
        title={`📱 Mã QR Tra cứu Hóa đơn điện tử #${viewingQrModal?.invoiceCode}`}
        open={!!viewingQrModal}
        onCancel={() => setViewingQrModal(null)}
        footer={[
          <Button key="close" type="primary" onClick={() => setViewingQrModal(null)}>Đóng</Button>,
        ]}
      >
        {viewingQrModal && (
          <div style={{ textAlign: 'center', padding: 20 }}>
            <img src={getQrUrl(viewingQrModal.invoiceCode)} alt="QR HĐĐT" style={{ width: 200, height: 200, display: 'inline-block', border: '1px solid #e2e8f0', padding: 8 }} />
            <div style={{ marginTop: 12, fontSize: 14, fontWeight: 700, color: '#2563eb' }}>
              {viewingQrModal.invoiceCode}
            </div>
            <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
              Bệnh nhân: <strong>{viewingQrModal.patientName}</strong> | Tổng tiền: <strong>{money(viewingQrModal.totalAmount)}</strong>
            </div>
          </div>
        )}
      </Modal>

      {/* MODAL XEM CHI TIẾT HÓA ĐƠN E-INVOICE */}
      <Modal
        title={`👁️ Xem Hóa đơn điện tử #${viewingEInvoice?.invoiceCode}`}
        open={!!viewingEInvoice}
        onCancel={() => setViewingEInvoice(null)}
        footer={[
          <Button key="pdf" icon={<FilePdfOutlined />} onClick={() => window.print()}>Tải PDF</Button>,
          <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={() => window.print()}>In HĐĐT</Button>,
          <Button key="close" onClick={() => setViewingEInvoice(null)}>Đóng</Button>,
        ]}
        width={680}
      >
        {viewingEInvoice && (
          <div>
            <Descriptions bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Mã hóa đơn">{viewingEInvoice.invoiceCode}</Descriptions.Item>
              <Descriptions.Item label="Loại hóa đơn">
                <Tag color={viewingEInvoice.invoiceType === 'ORIGINAL' ? 'green' : 'orange'}>
                  {viewingEInvoice.invoiceType === 'ORIGINAL' ? 'Hóa đơn gốc' : 'Hóa đơn điều chỉnh'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Bệnh nhân">{viewingEInvoice.patientName}</Descriptions.Item>
              <Descriptions.Item label="Mã BN">{viewingEInvoice.patientCode || 'BN-2026001'}</Descriptions.Item>
              <Descriptions.Item label="Ngày lập">{dayjs(viewingEInvoice.createdAt).format('HH:mm DD/MM/YYYY')}</Descriptions.Item>
              <Descriptions.Item label="Người lập">{viewingEInvoice.issuerName || issuerName}</Descriptions.Item>
              <Descriptions.Item label="Tổng tiền" span={2}>
                <Text type="success" strong style={{ fontSize: 16 }}>{money(viewingEInvoice.totalAmount)}</Text>
              </Descriptions.Item>
            </Descriptions>

            {viewingEInvoice.adjustmentReason && (
              <Alert
                type="warning"
                style={{ marginBottom: 16 }}
                message={`Lý do điều chỉnh (Liên kết hóa đơn gốc: ${viewingEInvoice.originalInvoiceCode}):`}
                description={viewingEInvoice.adjustmentReason}
              />
            )}
          </div>
        )}
      </Modal>
    </div>
  )
}

export default BillingPage
