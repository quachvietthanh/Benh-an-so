import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Divider,
  Empty,
  Form,
  Input,
  InputNumber,
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
  notification,
} from 'antd'
import {
  AlertOutlined,
  ArrowLeftOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CopyOutlined,
  DeleteOutlined,
  FieldTimeOutlined,
  HistoryOutlined,
  InboxOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  ShopOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useLocation, useNavigate } from 'react-router-dom'
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'

const { Text, Title, Paragraph } = Typography

const EMPTY_RECEIPT_ITEM = {
  medicineId: undefined,
  batchNumber: '',
  expiryDate: null,
  quantity: 1,
  importPrice: 0,
}

const toCollection = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

const normalizeBatch = (batch) => ({
  ...batch,
  batchId: batch?.batchId || batch?.id,
  id: batch?.batchId || batch?.id,
})

const getErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback

function InventoryReceiptPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuthContext()

  // Phân quyền
  const roles = useMemo(() => {
    const values = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
    return values
      .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
      .filter(Boolean)
  }, [user])
  const canManageReceipts = roles.includes('pharmacist') || roles.includes('admin')

  const [form] = Form.useForm()

  // State Tabs
  const [activeTab, setActiveTab] = useState(location.state?.tab || 'create')

  // State Dữ liệu
  const [medicines, setMedicines] = useState([])
  const [batches, setBatches] = useState([])
  const [expiryAlerts, setExpiryAlerts] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [loadError, setLoadError] = useState('')

  // State Bộ lọc Lô thuốc
  const [batchKeyword, setBatchKeyword] = useState('')
  const [batchStatusFilter, setBatchStatusFilter] = useState('ALL')
  const [batchEligibleFilter, setBatchEligibleFilter] = useState('ALL')
  const [batchExpiryFilter, setBatchExpiryFilter] = useState('ALL')

  // Watch form items để tính tổng tiền theo thời gian thực
  const formItems = Form.useWatch('items', form) || []
  const totalReceiptAmount = useMemo(() => {
    return formItems.reduce((sum, item) => {
      const q = Number(item?.quantity || 0)
      const p = Number(item?.importPrice || 0)
      return sum + q * p
    }, 0)
  }, [formItems])

  const loadData = useCallback(async () => {
    if (!canManageReceipts) return
    setLoading(true)
    setLoadError('')
    try {
      const [medicineResponse, batchResponse, expiryResponse] = await Promise.allSettled([
        pharmacyApi.medicines({ active: true }),
        pharmacyApi.batches(),
        pharmacyApi.expiryAlerts(),
      ])

      if (medicineResponse.status === 'fulfilled') {
        setMedicines(toCollection(medicineResponse.value?.data).filter((item) => item?.active !== false))
      }
      if (batchResponse.status === 'fulfilled') {
        setBatches(toCollection(batchResponse.value?.data).map(normalizeBatch))
      }
      if (expiryResponse.status === 'fulfilled') {
        setExpiryAlerts(toCollection(expiryResponse.value?.data))
      }
    } catch (error) {
      setLoadError(getErrorMessage(error, 'Không thể tải dữ liệu kho từ máy chủ.'))
    } finally {
      setLoading(false)
    }
  }, [canManageReceipts])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Tự động điền dòng thuốc nếu được chuyển đến từ danh sách cảnh báo thiếu tồn kho
  useEffect(() => {
    if (location.state?.prefillItem && medicines.length > 0) {
      const { medicineId, quantity, medicineName } = location.state.prefillItem
      form.setFieldsValue({
        items: [
          {
            medicineId,
            batchNumber: `LOT-${dayjs().format('YYYYMM')}-${Math.floor(1000 + Math.random() * 9000)}`,
            expiryDate: dayjs().add(1, 'year'),
            quantity: Math.max(Number(quantity) || 1, 1),
            importPrice: 0,
          },
        ],
        note: `Phiếu nhập bổ sung cho ${medicineName || 'thuốc dưới ngưỡng tồn'}`,
      })
      setActiveTab('create')
    }
  }, [location.state, medicines, form])

  const medicineOptions = useMemo(
    () =>
      medicines.map((medicine) => ({
        value: medicine.id,
        label: `${medicine.medicineCode || '—'} · ${medicine.medicineName} (${[medicine.activeIngredient, medicine.strength].filter(Boolean).join(' - ') || medicine.unit})`,
        unit: medicine.unit,
        medicineCode: medicine.medicineCode,
        medicineName: medicine.medicineName,
      })),
    [medicines],
  )

  const medicineMap = useMemo(
    () => new Map(medicines.map((m) => [String(m.id), m])),
    [medicines],
  )

  // Thống kê nhanh
  const stats = useMemo(() => {
    const totalBatches = batches.length
    const eligibleBatches = batches.filter((b) => b.eligibleForDispense !== false && b.status !== 'EXPIRED' && b.quantity > 0).length
    const nearExpiryBatches = batches.filter((b) => {
      if (!b.expiryDate || b.status === 'EXPIRED') return false
      const days = dayjs(b.expiryDate).diff(dayjs(), 'day')
      return days >= 0 && days <= 90 && b.quantity > 0
    }).length
    const expiredOrDepleted = batches.filter((b) => b.status === 'EXPIRED' || b.quantity === 0).length

    return {
      totalBatches,
      eligibleBatches,
      nearExpiryBatches,
      expiredOrDepleted,
      expiryAlertCount: expiryAlerts.length,
    }
  }, [batches, expiryAlerts])

  // Xử lý nộp phiếu nhập kho
  const handleSubmit = async (values) => {
    if (!canManageReceipts) {
      message.error('Bạn không có quyền tạo phiếu nhập kho.')
      return
    }

    const rawItems = Array.isArray(values.items) ? values.items : []
    if (rawItems.length === 0) {
      message.error('Phiếu nhập phải có ít nhất một dòng thuốc.')
      return
    }

    // Ràng buộc không trùng số lô cho cùng một thuốc trong một phiếu
    const duplicateKeys = new Set()
    for (let i = 0; i < rawItems.length; i++) {
      const item = rawItems[i]
      if (!item.medicineId) {
        message.error(`Dòng thuốc số ${i + 1}: Vui lòng chọn loại thuốc cần nhập.`)
        return
      }
      if (!String(item.batchNumber || '').trim()) {
        message.error(`Dòng thuốc số ${i + 1}: Vui lòng nhập số lô.`)
        return
      }
      if (!item.expiryDate || !item.expiryDate.isAfter(dayjs(), 'day')) {
        message.error(`Dòng thuốc số ${i + 1}: Hạn sử dụng phải là ngày trong tương lai.`)
        return
      }
      if (Number(item.quantity || 0) <= 0) {
        message.error(`Dòng thuốc số ${i + 1}: Số lượng nhập phải lớn hơn 0.`)
        return
      }

      const key = `${item.medicineId}-${String(item.batchNumber || '').trim().toUpperCase()}`
      if (duplicateKeys.has(key)) {
        message.error(`Dòng thuốc số ${i + 1}: Không thể nhập trùng cùng một số lô (${item.batchNumber}) cho cùng một loại thuốc trong một phiếu.`)
        return
      }
      duplicateKeys.add(key)
    }

    const payload = {
      note: String(values.note || '').trim() || null,
      items: rawItems.map((item) => ({
        medicineId: item.medicineId,
        batchNumber: String(item.batchNumber).trim(),
        expiryDate: item.expiryDate.format('YYYY-MM-DD'),
        quantity: Number(item.quantity),
        importPrice: Number(item.importPrice || 0),
      })),
    }

    setSubmitting(true)
    try {
      const response = await pharmacyApi.receiveBatch(payload)
      const receiptData = response.data
      const receiptId = receiptData?.id

      // Xử lý thông báo cảnh báo gộp lô (nếu có)
      const warnings = Array.isArray(receiptData?.warnings) ? receiptData.warnings : []
      if (warnings.length > 0) {
        notification.warning({
          message: 'Lưu ý gộp lô thuốc đã tồn tại',
          description: warnings.map((w, idx) => (
            <div key={idx} style={{ marginBottom: 4 }}>
              Số lô <strong>{w.batchNumber}</strong> đã tồn tại trong kho và đã được gộp số lượng tồn thành công.
            </div>
          )),
          duration: 6,
        })
      } else {
        message.success(`Đã tạo phiếu nhập kho thành công${receiptId ? ` (Mã: ${String(receiptId).slice(-8)})` : ''}.`)
      }

      form.resetFields()
      form.setFieldsValue({ items: [{ ...EMPTY_RECEIPT_ITEM }] })
      await loadData()
      setActiveTab('batches')
    } catch (error) {
      const msg = getErrorMessage(error, 'Không thể tạo phiếu nhập kho.')
      message.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  // Lọc danh sách lô thuốc
  const filteredBatches = useMemo(() => {
    let list = Array.isArray(batches) ? batches : []

    // Lọc theo từ khóa
    const kw = batchKeyword.trim().toLowerCase()
    if (kw) {
      list = list.filter((b) =>
        [b.batchNumber, b.medicineName, b.medicineCode].some((f) =>
          String(f || '').toLowerCase().includes(kw)
        )
      )
    }

    // Lọc theo trạng thái
    if (batchStatusFilter !== 'ALL') {
      list = list.filter((b) => String(b.status).toUpperCase() === batchStatusFilter)
    }

    // Lọc theo điều kiện cấp phát
    if (batchEligibleFilter === 'ELIGIBLE') {
      list = list.filter((b) => b.eligibleForDispense !== false && b.status !== 'EXPIRED' && b.quantity > 0)
    } else if (batchEligibleFilter === 'INELIGIBLE') {
      list = list.filter((b) => b.eligibleForDispense === false || b.status === 'EXPIRED' || b.quantity === 0)
    }

    // Lọc theo hạn dùng
    if (batchExpiryFilter === 'EXPIRED') {
      list = list.filter((b) => b.expiryDate && dayjs(b.expiryDate).isBefore(dayjs(), 'day'))
    } else if (batchExpiryFilter === 'NEAR_30') {
      list = list.filter((b) => {
        if (!b.expiryDate) return false
        const d = dayjs(b.expiryDate).diff(dayjs(), 'day')
        return d >= 0 && d <= 30
      })
    } else if (batchExpiryFilter === 'NEAR_90') {
      list = list.filter((b) => {
        if (!b.expiryDate) return false
        const d = dayjs(b.expiryDate).diff(dayjs(), 'day')
        return d >= 0 && d <= 90
      })
    } else if (batchExpiryFilter === 'SAFE') {
      list = list.filter((b) => {
        if (!b.expiryDate) return false
        return dayjs(b.expiryDate).diff(dayjs(), 'day') > 90
      })
    }

    return list
  }, [batches, batchKeyword, batchStatusFilter, batchEligibleFilter, batchExpiryFilter])

  // Cột bảng Lô thuốc
  const batchColumns = [
    {
      title: 'Mã thuốc',
      dataIndex: 'medicineCode',
      key: 'medicineCode',
      width: 120,
      render: (value) => <Text code>{value || '—'}</Text>,
    },
    {
      title: 'Tên thuốc & Thông tin',
      key: 'medicineName',
      render: (_, batch) => {
        const med = medicineMap.get(String(batch.medicineId))
        return (
          <Space direction="vertical" size={0}>
            <strong>{batch.medicineName || med?.medicineName || '—'}</strong>
            {med && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                {[med.activeIngredient, med.strength].filter(Boolean).join(' · ')}
              </Text>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Số lô',
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      width: 140,
      render: (value) => (
        <Space>
          <Tag color="geekblue" style={{ fontWeight: 600 }}>{value}</Tag>
          <Tooltip title="Sao chép số lô">
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => {
                navigator.clipboard?.writeText(value)
                message.success(`Đã sao chép số lô: ${value}`)
              }}
            />
          </Tooltip>
        </Space>
      ),
    },
    {
      title: 'Hạn sử dụng',
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      width: 180,
      render: (value) => {
        if (!value) return '—'
        const exp = dayjs(value)
        const days = exp.diff(dayjs(), 'day')
        const isExpired = days < 0
        const isNear30 = days >= 0 && days <= 30
        const isNear90 = days > 30 && days <= 90

        return (
          <Space direction="vertical" size={1}>
            <span style={{ fontWeight: 600 }}>{exp.format('DD/MM/YYYY')}</span>
            {isExpired ? (
              <Tag color="error">Đã hết hạn ({Math.abs(days)} ngày trước)</Tag>
            ) : isNear30 ? (
              <Tag color="volcano">Sắp hết hạn (Còn {days} ngày)</Tag>
            ) : isNear90 ? (
              <Tag color="warning">Cận hạn (Còn {days} ngày)</Tag>
            ) : (
              <Tag color="success">Còn {days} ngày</Tag>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Tồn kho lô',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 120,
      align: 'right',
      render: (value, batch) => {
        const med = medicineMap.get(String(batch.medicineId))
        const q = Number(value || 0)
        return (
          <span style={{ fontWeight: 700, color: q === 0 ? '#94a3b8' : '#1e293b', fontSize: 14 }}>
            {q.toLocaleString('vi-VN')} {med?.unit || ''}
          </span>
        )
      },
    },
    {
      title: 'Trạng thái cấp phát (FEFO)',
      key: 'eligible',
      width: 180,
      render: (_, batch) => {
        const isExpired = batch.expiryDate && dayjs(batch.expiryDate).isBefore(dayjs(), 'day')
        const isDepleted = Number(batch.quantity || 0) === 0
        const isEligible = batch.eligibleForDispense !== false && !isExpired && !isDepleted

        if (isExpired) {
          return <Tag color="red">Hết hạn - Không cấp</Tag>
        }
        if (isDepleted) {
          return <Tag color="default">Đã hết tồn</Tag>
        }
        if (isEligible) {
          return <Tag color="green" icon={<CheckCircleOutlined />}>Được cấp phát (FEFO)</Tag>
        }
        return <Tag color="orange">Không đủ điều kiện</Tag>
      },
    },
  ]

  // Cột bảng Cảnh báo Hạn dùng
  const alertColumns = [
    {
      title: 'Mã & Tên thuốc',
      key: 'medicine',
      render: (_, item) => (
        <Space direction="vertical" size={1}>
          <strong>{item.medicineName || '—'}</strong>
          <Text code style={{ fontSize: 12 }}>{item.medicineId}</Text>
        </Space>
      ),
    },
    {
      title: 'Số lô',
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      width: 140,
      render: (val) => <Tag color="blue">{val}</Tag>,
    },
    {
      title: 'Hạn sử dụng',
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      width: 150,
      render: (val) => val ? dayjs(val).format('DD/MM/YYYY') : '—',
    },
    {
      title: 'Thời hạn còn lại',
      dataIndex: 'daysRemaining',
      key: 'daysRemaining',
      width: 160,
      render: (days, item) => {
        const d = Number(days ?? 0)
        if (d < 0 || item.status === 'EXPIRED') {
          return <Tag color="red">Đã quá hạn {Math.abs(d)} ngày</Tag>
        }
        if (d <= 30) {
          return <Tag color="volcano" icon={<AlertOutlined />}>Còn {d} ngày</Tag>
        }
        return <Tag color="warning" icon={<WarningOutlined />}>Còn {d} ngày</Tag>
      },
    },
    {
      title: 'Tồn hiện tại',
      dataIndex: 'stockQuantity',
      key: 'stockQuantity',
      width: 120,
      align: 'right',
      render: (val) => (
        <span style={{ fontWeight: 700, color: Number(val || 0) > 0 ? '#dc2626' : '#94a3b8' }}>
          {Number(val || 0).toLocaleString('vi-VN')}
        </span>
      ),
    },
    {
      title: 'Mức độ cảnh báo',
      dataIndex: 'status',
      key: 'status',
      width: 170,
      render: (status) => {
        if (status === 'EXPIRED') {
          return <Tag color="magenta">Đã hết hạn</Tag>
        }
        if (status === 'NEAR_EXPIRY_30_DAYS') {
          return <Tag color="red">Khẩn cấp (≤ 30 ngày)</Tag>
        }
        if (status === 'NEAR_EXPIRY_90_DAYS') {
          return <Tag color="orange">Cận hạn (≤ 90 ngày)</Tag>
        }
        return <Tag>{status}</Tag>
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 150,
      align: 'center',
      render: (_, record) => (
        <Button
          type="primary"
          size="small"
          icon={<PlusOutlined />}
          onClick={() => {
            form.setFieldsValue({
              items: [
                {
                  medicineId: record.medicineId,
                  batchNumber: `LOT-${dayjs().format('YYYYMM')}-${Math.floor(1000 + Math.random() * 9000)}`,
                  expiryDate: dayjs().add(1, 'year'),
                  quantity: Math.max(Number(record.stockQuantity) || 10, 10),
                  importPrice: 0,
                },
              ],
              note: `Phiếu nhập thay thế lô ${record.batchNumber} sắp hết hạn`,
            })
            setActiveTab('create')
          }}
        >
          Nhập lô mới
        </Button>
      ),
    },
  ]

  return (
    <div style={{ paddingBottom: 40 }}>
      {/* Header trang */}
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
            <InboxOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Nhập kho theo lô & Quản lý Hạn dùng
          </Title>
          <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
            Tạo phiếu nhập thuốc theo số lô và hạn dùng chính xác, kiểm soát tồn kho và theo dõi hạn sử dụng FEFO.
          </Paragraph>
        </div>
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/pharmacy')}>
            Về Cấp phát thuốc
          </Button>
          <Button icon={<ShopOutlined />} onClick={() => navigate('/medicines')}>
            Danh mục & Ngưỡng tồn
          </Button>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadData}>
            Làm mới
          </Button>
        </Space>
      </div>

      {loadError && (
        <Alert
          type="error"
          showIcon
          message="Không tải được dữ liệu kho"
          description={loadError}
          action={<Button size="small" onClick={loadData}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Dashboard KPI Metric Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Tổng số lô trong kho"
              value={stats.totalBatches}
              prefix={<InboxOutlined style={{ color: '#1677ff' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Lô đủ điều kiện cấp phát (FEFO)"
              value={stats.eligibleBatches}
              valueStyle={{ color: '#52c41a', fontWeight: 700 }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              borderRadius: 8,
              borderLeft: stats.nearExpiryBatches > 0 ? '4px solid #faad14' : undefined,
              cursor: 'pointer',
            }}
            onClick={() => setActiveTab('alerts')}
          >
            <Statistic
              title="Lô sắp hết hạn (≤ 90 ngày)"
              value={stats.nearExpiryBatches}
              valueStyle={stats.nearExpiryBatches > 0 ? { color: '#faad14', fontWeight: 700 } : undefined}
              prefix={<FieldTimeOutlined />}
              suffix={<Text type="secondary" style={{ fontSize: 12, marginLeft: 6 }}>Xem →</Text>}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Lô hết hạn / Hết tồn"
              value={stats.expiredOrDepleted}
              valueStyle={stats.expiredOrDepleted > 0 ? { color: '#ff4d4f' } : undefined}
              prefix={<WarningOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Tabs Chức năng chính */}
      <Card styles={{ body: { padding: '16px 20px' } }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'create',
              label: (
                <Space>
                  <PlusOutlined />
                  <span>Tạo phiếu nhập kho theo lô</span>
                </Space>
              ),
              children: (
                <div>
                  <Form
                    form={form}
                    layout="vertical"
                    initialValues={{
                      note: '',
                      items: [
                        {
                          ...EMPTY_RECEIPT_ITEM,
                          batchNumber: `LOT-${dayjs().format('YYYYMM')}-${Math.floor(1000 + Math.random() * 9000)}`,
                          expiryDate: dayjs().add(1, 'year'),
                        },
                      ],
                    }}
                    onFinish={handleSubmit}
                  >
                    <Row gutter={16}>
                      <Col xs={24} md={16}>
                        <Form.Item
                          name="note"
                          label="Ghi chú phiếu nhập kho"
                          extra="Nhập thông tin nguồn cung cấp, hóa đơn chứng từ hoặc biên bản kiểm nhận..."
                        >
                          <Input.TextArea
                            rows={2}
                            maxLength={1000}
                            showCount
                            placeholder="Ví dụ: Nhập theo hợp đồng dược phẩm quý III từ Công ty Dược TW"
                          />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Card
                          size="small"
                          style={{
                            background: '#f8fafc',
                            borderColor: '#e2e8f0',
                            textAlign: 'right',
                          }}
                        >
                          <Text type="secondary">Tổng giá trị phiếu nhập:</Text>
                          <div style={{ fontSize: 22, fontWeight: 700, color: '#1677ff', marginTop: 4 }}>
                            {totalReceiptAmount.toLocaleString('vi-VN')} ₫
                          </div>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {formItems.length} dòng thuốc
                          </Text>
                        </Card>
                      </Col>
                    </Row>

                    <Divider style={{ margin: '16px 0' }} />

                    {/* Danh sách dòng thuốc */}
                    <Title level={5} style={{ marginBottom: 12 }}>
                      Chi tiết thuốc, số lô và hạn dùng:
                    </Title>

                    <Form.List
                      name="items"
                      rules={[
                        {
                          validator: async (_, items) => {
                            if (!items || items.length === 0) {
                              throw new Error('Phiếu nhập phải có ít nhất một dòng thuốc.')
                            }
                          },
                        },
                      ]}
                    >
                      {(fields, { add, remove }, { errors }) => (
                        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                          {fields.map((field, index) => {
                            const currentItem = formItems[index]
                            const selectedMed = medicineMap.get(String(currentItem?.medicineId))

                            return (
                              <Card
                                key={field.key}
                                size="small"
                                style={{
                                  background: '#ffffff',
                                  borderColor: '#cbd5e1',
                                  borderRadius: 8,
                                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                                }}
                                title={
                                  <Space>
                                    <Tag color="blue">Dòng {index + 1}</Tag>
                                    {selectedMed && (
                                      <Text strong>{selectedMed.medicineName} ({selectedMed.medicineCode})</Text>
                                    )}
                                  </Space>
                                }
                                extra={
                                  fields.length > 1 && (
                                    <Button
                                      danger
                                      type="text"
                                      icon={<DeleteOutlined />}
                                      onClick={() => remove(field.name)}
                                    >
                                      Xóa dòng này
                                    </Button>
                                  )
                                }
                              >
                                <Row gutter={[16, 8]}>
                                  <Col xs={24} md={10}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'medicineId']}
                                      label="Chọn loại thuốc cần nhập"
                                      rules={[{ required: true, message: 'Vui lòng chọn thuốc' }]}
                                    >
                                      <Select
                                        showSearch
                                        optionFilterProp="label"
                                        options={medicineOptions}
                                        placeholder="Tìm theo tên thuốc, mã thuốc, hoạt chất..."
                                      />
                                    </Form.Item>
                                  </Col>

                                  <Col xs={24} md={7}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'batchNumber']}
                                      label="Số lô sản xuất"
                                      rules={[
                                        { required: true, whitespace: true, message: 'Vui lòng nhập số lô' },
                                        { max: 100, message: 'Số lô không vượt quá 100 ký tự' },
                                      ]}
                                    >
                                      <Input
                                        placeholder="Ví dụ: LOT-2026-001"
                                        addonAfter={
                                          <Tooltip title="Tạo số lô ngẫu nhiên">
                                            <Button
                                              type="text"
                                              size="small"
                                              icon={<ReloadOutlined />}
                                              onClick={() => {
                                                const currentItems = form.getFieldValue('items') || []
                                                currentItems[index] = {
                                                  ...currentItems[index],
                                                  batchNumber: `LOT-${dayjs().format('YYYYMM')}-${Math.floor(1000 + Math.random() * 9000)}`,
                                                }
                                                form.setFieldsValue({ items: currentItems })
                                              }}
                                            />
                                          </Tooltip>
                                        }
                                      />
                                    </Form.Item>
                                  </Col>

                                  <Col xs={24} md={7}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'expiryDate']}
                                      label="Hạn sử dụng (Bắt buộc tương lai)"
                                      rules={[{ required: true, message: 'Vui lòng chọn hạn dùng' }]}
                                    >
                                      <DatePicker
                                        style={{ width: '100%' }}
                                        format="DD/MM/YYYY"
                                        placeholder="Chọn ngày hết hạn"
                                        disabledDate={(current) => current && !current.isAfter(dayjs(), 'day')}
                                      />
                                    </Form.Item>
                                  </Col>

                                  <Col xs={24} md={12}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'quantity']}
                                      label={
                                        <Space>
                                          <span>Số lượng nhập</span>
                                          {selectedMed?.unit && (
                                            <Text type="secondary">(Đơn vị: {selectedMed.unit})</Text>
                                          )}
                                        </Space>
                                      }
                                      rules={[{ required: true, message: 'Nhập số lượng' }]}
                                    >
                                      <InputNumber
                                        min={1}
                                        max={1000000}
                                        precision={0}
                                        style={{ width: '100%' }}
                                        placeholder="Nhập số lượng"
                                        addonAfter={selectedMed?.unit || 'Đơn vị'}
                                      />
                                    </Form.Item>
                                  </Col>

                                  <Col xs={24} md={12}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'importPrice']}
                                      label="Đơn giá nhập (VNĐ)"
                                      rules={[{ required: true, message: 'Nhập đơn giá' }]}
                                    >
                                      <InputNumber
                                        min={0}
                                        precision={2}
                                        style={{ width: '100%' }}
                                        placeholder="Nhập giá nhập"
                                        addonAfter="₫"
                                        formatter={(val) => `${val}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                                        parser={(val) => val.replace(/\$\s?|(,*)/g, '')}
                                      />
                                    </Form.Item>
                                  </Col>
                                </Row>
                              </Card>
                            )
                          })}

                          <Form.ErrorList errors={errors} />

                          <Button
                            type="dashed"
                            block
                            icon={<PlusOutlined />}
                            onClick={() =>
                              add({
                                ...EMPTY_RECEIPT_ITEM,
                                batchNumber: `LOT-${dayjs().format('YYYYMM')}-${Math.floor(1000 + Math.random() * 9000)}`,
                                expiryDate: dayjs().add(1, 'year'),
                              })
                            }
                            style={{ height: 44, borderRadius: 8 }}
                          >
                            + Thêm dòng thuốc khác vào phiếu nhập
                          </Button>
                        </Space>
                      )}
                    </Form.List>

                    <div style={{ marginTop: 24, display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
                      <Button
                        size="large"
                        onClick={() => {
                          form.resetFields()
                          form.setFieldsValue({ items: [{ ...EMPTY_RECEIPT_ITEM }] })
                        }}
                      >
                        Làm lại
                      </Button>
                      <Button
                        type="primary"
                        htmlType="submit"
                        size="large"
                        icon={<SaveOutlined />}
                        loading={submitting}
                        disabled={loading || medicines.length === 0}
                      >
                        Xác nhận nhập kho theo lô
                      </Button>
                    </div>
                  </Form>
                </div>
              ),
            },
            {
              key: 'batches',
              label: (
                <Space>
                  <InboxOutlined />
                  <span>Danh sách & Theo dõi Lô thuốc ({batches.length})</span>
                </Space>
              ),
              children: (
                <div>
                  {/* Bộ lọc Lô thuốc */}
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
                      <Input
                        placeholder="Tìm theo số lô, mã, tên thuốc..."
                        allowClear
                        prefix={<SearchOutlined />}
                        style={{ width: 280 }}
                        value={batchKeyword}
                        onChange={(e) => setBatchKeyword(e.target.value)}
                      />

                      <Select
                        defaultValue="ALL"
                        value={batchStatusFilter}
                        style={{ width: 170 }}
                        onChange={setBatchStatusFilter}
                        options={[
                          { value: 'ALL', label: 'Tất cả trạng thái lô' },
                          { value: 'ACTIVE', label: '🟢 Đang hoạt động' },
                          { value: 'DEPLETED', label: '⚪ Đã hết tồn' },
                          { value: 'EXPIRED', label: '🔴 Đã hết hạn' },
                        ]}
                      />

                      <Select
                        defaultValue="ALL"
                        value={batchEligibleFilter}
                        style={{ width: 200 }}
                        onChange={setBatchEligibleFilter}
                        options={[
                          { value: 'ALL', label: 'Tất cả điều kiện FEFO' },
                          { value: 'ELIGIBLE', label: '🟢 Được cấp phát FEFO' },
                          { value: 'INELIGIBLE', label: '🔴 Không đủ điều kiện' },
                        ]}
                      />

                      <Select
                        defaultValue="ALL"
                        value={batchExpiryFilter}
                        style={{ width: 200 }}
                        onChange={setBatchExpiryFilter}
                        options={[
                          { value: 'ALL', label: 'Tất cả hạn dùng' },
                          { value: 'NEAR_30', label: '⚠️ Sắp hết hạn (≤ 30 ngày)' },
                          { value: 'NEAR_90', label: '🟡 Cận hạn (≤ 90 ngày)' },
                          { value: 'SAFE', label: '🟢 Còn hạn dài (> 90 ngày)' },
                          { value: 'EXPIRED', label: '🔴 Đã hết hạn' },
                        ]}
                      />
                    </Space>

                    <Button icon={<ReloadOutlined />} loading={loading} onClick={loadData}>
                      Làm mới
                    </Button>
                  </div>

                  <Table
                    rowKey={(batch) => batch.batchId || `${batch.medicineId}-${batch.batchNumber}`}
                    columns={batchColumns}
                    dataSource={filteredBatches}
                    loading={loading}
                    pagination={{
                      pageSize: 10,
                      showSizeChanger: true,
                      pageSizeOptions: ['10', '20', '50', '100'],
                      showTotal: (total) => `Tổng số ${total} lô thuốc trong kho`,
                    }}
                    scroll={{ x: 950 }}
                    locale={{ emptyText: <Empty description="Không tìm thấy lô thuốc nào phù hợp" /> }}
                  />
                </div>
              ),
            },
            {
              key: 'alerts',
              label: (
                <Space>
                  <WarningOutlined style={{ color: stats.expiryAlertCount > 0 ? '#faad14' : undefined }} />
                  <span>Cảnh báo Hạn sử dụng</span>
                  {stats.expiryAlertCount > 0 && (
                    <Badge count={stats.expiryAlertCount} overflowCount={99} style={{ backgroundColor: '#faad14' }} />
                  )}
                </Space>
              ),
              children: (
                <div>
                  <Alert
                    type="info"
                    showIcon
                    icon={<CalendarOutlined />}
                    message="Quy tắc kiểm soát hạn sử dụng thuốc"
                    description="Thuốc hết hạn hoặc cận hạn sẽ tự động bị loại khỏi danh sách đủ điều kiện cấp phát theo nguyên tắc FEFO (First Expired, First Out) để đảm bảo an toàn tuyệt đối cho bệnh nhân."
                    style={{ marginBottom: 16 }}
                  />

                  <Table
                    rowKey={(item) => item.batchId || `${item.medicineId}-${item.batchNumber}`}
                    columns={alertColumns}
                    dataSource={expiryAlerts}
                    loading={loading}
                    pagination={{ pageSize: 10, showSizeChanger: false }}
                    scroll={{ x: 900 }}
                    locale={{
                      emptyText: (
                        <Empty
                          description="Không có lô thuốc nào sắp hết hạn trong kho"
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                        />
                      ),
                    }}
                  />
                </div>
              ),
            },
          ]}
        />
      </Card>
    </div>
  )
}

export default InventoryReceiptPage
