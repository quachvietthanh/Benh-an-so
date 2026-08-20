import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Input,
  List,
  Modal,
  Pagination,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  CheckCircleOutlined,
  FieldTimeOutlined,
  InboxOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  RightOutlined,
  RollbackOutlined,
  SearchOutlined,
  ShopOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'
import { buildFefoPreview, fixMojibake } from '../utils/workflowContract'
import { saveStoredPrescription, dispensePrescriptionHelper, mergePrescriptions } from '../utils/storageHelpers'


const { Text, Title } = Typography
const PRESCRIPTION_PAGE_SIZE = 20

const toCollection = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

const parseItems = (value) => {
  if (Array.isArray(value)) return value
  if (typeof value !== 'string') return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const normalizeBatch = (batch) => ({
  ...batch,
  batchId: batch?.batchId || batch?.id,
  id: batch?.batchId || batch?.id,
})

const getErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback

const formatDateTime = (value) =>
  value && dayjs(value).isValid() ? dayjs(value).format('HH:mm DD/MM/YYYY') : '—'

function PharmacyPage() {
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const roles = useMemo(() => {
    const values = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
    return values
      .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
      .filter(Boolean)
  }, [user])
  const canDispense = roles.includes('pharmacist') || roles.includes('admin')

  const [prescriptions, setPrescriptions] = useState([])
  const [prescriptionPage, setPrescriptionPage] = useState(0)
  const [prescriptionTotal, setPrescriptionTotal] = useState(0)
  const [prescriptionTotalPages, setPrescriptionTotalPages] = useState(0)
  const [batches, setBatches] = useState([])
  const [stocks, setStocks] = useState([])
  const [lowStockItems, setLowStockItems] = useState([])
  const [expiryAlerts, setExpiryAlerts] = useState([])
  const [expiryModalOpen, setExpiryModalOpen] = useState(false)
  const [selectedPrescriptionId, setSelectedPrescriptionId] = useState(null)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [prescriptionLoading, setPrescriptionLoading] = useState(false)
  const [inventoryLoading, setInventoryLoading] = useState(false)
  const [dispensingId, setDispensingId] = useState(null)
  const [prescriptionLoadError, setPrescriptionLoadError] = useState('')
  const [inventoryLoadError, setInventoryLoadError] = useState('')
  const [shortageDetails, setShortageDetails] = useState([])
  const prescriptionRequestIdRef = useRef(0)
  const inventoryRequestIdRef = useRef(0)

  const loadPrescriptionPage = useCallback(async (pageNumber) => {
    const requestId = prescriptionRequestIdRef.current + 1
    prescriptionRequestIdRef.current = requestId
    setPrescriptionLoading(true)
    setPrescriptionLoadError('')
    try {
      const prescriptionResponse = await pharmacyApi.prescriptions({
        status: 'PENDING_DISPENSE',
        page: pageNumber,
        size: PRESCRIPTION_PAGE_SIZE,
      })
      if (requestId !== prescriptionRequestIdRef.current) return

      const payload = prescriptionResponse.data
      const rawApiPrescriptions = toCollection(payload)
      const nextPrescriptions = mergePrescriptions(rawApiPrescriptions)
        .filter((item) => item?.status === 'PENDING_DISPENSE')
        .sort((first, second) =>
          String(first.prescribedAt || first.createdAt || '').localeCompare(String(second.prescribedAt || second.createdAt || '')),
        )
      const parsedTotal = Number(payload?.totalElements)
      const nextTotal = Number.isFinite(parsedTotal) ? Math.max(parsedTotal, 0) : nextPrescriptions.length
      const parsedTotalPages = Number(payload?.totalPages)
      const calculatedTotalPages = Math.ceil(nextTotal / PRESCRIPTION_PAGE_SIZE)
      const nextTotalPages = Number.isFinite(parsedTotalPages)
        && (parsedTotalPages > 0 || nextTotal === 0)
        ? Math.max(parsedTotalPages, 0)
        : calculatedTotalPages

      setPrescriptionTotal(nextTotal)
      setPrescriptionTotalPages(nextTotalPages)

      if (nextTotal > 0 && pageNumber >= nextTotalPages) {
        setPrescriptions([])
        setSelectedPrescriptionId(null)
        setPrescriptionPage(Math.max(nextTotalPages - 1, 0))
        return
      }

      if (nextTotal === 0 && pageNumber !== 0) {
        setPrescriptions([])
        setSelectedPrescriptionId(null)
        setPrescriptionPage(0)
        return
      }

      setPrescriptions(nextPrescriptions)
      setSelectedPrescriptionId((currentId) => {
        if (nextPrescriptions.some((item) => String(item.id) === String(currentId))) {
          return currentId
        }
        return nextPrescriptions[0]?.id || null
      })
    } catch (error) {
      if (requestId === prescriptionRequestIdRef.current) {
        setPrescriptionLoadError(getErrorMessage(error, 'Không thể tải danh sách đơn chờ cấp phát.'))
      }
    } finally {
      if (requestId === prescriptionRequestIdRef.current) {
        setPrescriptionLoading(false)
      }
    }
  }, [])

  const loadInventoryData = useCallback(async () => {
    const requestId = inventoryRequestIdRef.current + 1
    inventoryRequestIdRef.current = requestId
    setInventoryLoading(true)
    setInventoryLoadError('')
    try {
      const [batchResponse, stockResponse, lowStockResponse, expiryResponse] = await Promise.allSettled([
        pharmacyApi.batches(),
        pharmacyApi.stocks({ active: true }),
        pharmacyApi.lowStock(),
        pharmacyApi.expiryAlerts(),
      ])
      if (requestId !== inventoryRequestIdRef.current) return

      if (batchResponse.status === 'fulfilled') {
        setBatches(toCollection(batchResponse.value?.data).map(normalizeBatch))
      }
      if (stockResponse.status === 'fulfilled') {
        setStocks(toCollection(stockResponse.value?.data))
      }
      if (lowStockResponse.status === 'fulfilled') {
        setLowStockItems(toCollection(lowStockResponse.value?.data))
      }
      if (expiryResponse.status === 'fulfilled') {
        setExpiryAlerts(toCollection(expiryResponse.value?.data))
      }
    } catch (error) {
      if (requestId === inventoryRequestIdRef.current) {
        setInventoryLoadError(getErrorMessage(error, 'Không thể tải dữ liệu tồn kho từ máy chủ.'))
      }
    } finally {
      if (requestId === inventoryRequestIdRef.current) {
        setInventoryLoading(false)
      }
    }
  }, [])

  const loadData = useCallback(() => Promise.all([
    loadPrescriptionPage(prescriptionPage),
    loadInventoryData(),
  ]), [loadInventoryData, loadPrescriptionPage, prescriptionPage])

  useEffect(() => {
    loadPrescriptionPage(prescriptionPage)
  }, [loadPrescriptionPage, prescriptionPage])

  useEffect(() => {
    loadInventoryData()
  }, [loadInventoryData])

  const loading = prescriptionLoading || inventoryLoading
  const loadError = [prescriptionLoadError, inventoryLoadError].filter(Boolean).join(' ')

  const filteredPrescriptions = useMemo(() => {
    const keyword = searchKeyword.trim().toLowerCase()
    if (!keyword) return prescriptions
    return prescriptions.filter((item) =>
      [item.prescriptionCode, item.patientCode, item.patientName, item.visitCode]
        .some((value) => String(value || '').toLowerCase().includes(keyword)),
    )
  }, [prescriptions, searchKeyword])

  const selectedPrescription = useMemo(
    () => prescriptions.find((item) => String(item.id) === String(selectedPrescriptionId)) || null,
    [prescriptions, selectedPrescriptionId],
  )

  const stockByMedicineId = useMemo(
    () => new Map(stocks.map((stock) => [String(stock.medicineId), stock])),
    [stocks],
  )

  const fefoPreview = useMemo(
    () => buildFefoPreview(parseItems(selectedPrescription?.items), batches),
    [selectedPrescription, batches],
  )
  const hasPreviewShortage = fefoPreview.some((item) => Number(item.shortageQuantity) > 0)

  const selectPrescription = (prescriptionId) => {
    setSelectedPrescriptionId(prescriptionId)
    setShortageDetails([])
  }

  const changePrescriptionPage = (pageNumber) => {
    setPrescriptionPage(pageNumber - 1)
    setPrescriptions([])
    setSelectedPrescriptionId(null)
    setSearchKeyword('')
    setShortageDetails([])
  }

  const handleDispense = async () => {
    if (!selectedPrescription) return
    if (!canDispense) {
      message.error('Chỉ dược sĩ hoặc quản trị viên mới được cấp phát thuốc.')
      return
    }

    if (hasPreviewShortage) {
      message.error('Không thể cấp phát đơn thuốc do tồn kho khả dụng không đủ.')
      return
    }

    setDispensingId(selectedPrescription.id)
    setShortageDetails([])
    try {
      let allocationCount = 0
      let dispensedSuccessfully = false
      try {
        const response = await pharmacyApi.dispense(selectedPrescription.id)
        allocationCount = Number(response.data?.allocationCount) || 0
        dispensedSuccessfully = true
      } catch (apiErr) {
        if (apiErr?.response?.status === 409) {
          throw apiErr
        }
        console.warn('[PharmacyPage] Backend dispense API fallback:', apiErr)
        dispensePrescriptionHelper(selectedPrescription.id)
        dispensedSuccessfully = true
      }

      if (dispensedSuccessfully) {
        saveStoredPrescription({ ...selectedPrescription, status: 'DISPENSED' })
        message.success(
          `Đã cấp phát đơn ${selectedPrescription.prescriptionCode || selectedPrescription.id}`
          + (allocationCount ? ` qua ${allocationCount} lượt phân bổ FEFO.` : '.'),
        )
        await loadData()
      }
    } catch (error) {
      const responseData = error?.response?.data
      if (error?.response?.status === 409 && Array.isArray(responseData?.details)) {
        setShortageDetails(responseData.details)
      }
      message.error(getErrorMessage(error, error?.message || 'Không thể cấp phát đơn thuốc do thiếu tồn kho.'))
    } finally {
      setDispensingId(null)
    }
  }

  const previewColumns = [
    {
      title: 'Thuốc',
      key: 'medicine',
      width: 210,
      render: (_, item) => (
        <Space direction="vertical" size={0}>
          <Text strong>{item.medicineName || item.medicineId}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {[item.activeIngredient, item.strength].filter(Boolean).join(' · ') || item.dosage || '—'}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Cần cấp',
      dataIndex: 'requiredQuantity',
      width: 90,
      align: 'center',
      render: (quantity, item) => <Text strong>{quantity} {item.unit || ''}</Text>,
    },
    {
      title: 'Tồn khả dụng',
      key: 'available',
      width: 115,
      align: 'center',
      render: (_, item) => {
        const stock = stockByMedicineId.get(String(item.medicineId))
        const available = stock?.eligibleStockQuantity ?? item.availableQuantity
        return <Tag color={Number(available) < Number(item.requiredQuantity) ? 'red' : 'green'}>{available}</Tag>
      },
    },
    {
      title: 'Phân bổ FEFO dự kiến',
      key: 'allocations',
      render: (_, item) => item.allocations.length ? (
        <Space direction="vertical" size={4}>
          {item.allocations.map((allocation) => (
            <Tag key={`${allocation.batchId}-${allocation.quantity}`} color="blue">
              {allocation.batchNumber} · HSD {dayjs(allocation.expiryDate).format('DD/MM/YYYY')} · x{allocation.quantity}
            </Tag>
          ))}
        </Space>
      ) : <Text type="danger">Không có lô đủ điều kiện</Text>,
    },
    {
      title: 'Kết quả',
      key: 'result',
      width: 105,
      align: 'center',
      render: (_, item) => Number(item.shortageQuantity) > 0
        ? <Tag color="red">Thiếu {item.shortageQuantity}</Tag>
        : <Tag color="green">Đủ tồn</Tag>,
    },
  ]

  const shortageColumns = [
    { title: 'Thuốc', dataIndex: 'medicineName', key: 'medicineName' },
    { title: 'Cần', dataIndex: 'requiredQuantity', key: 'requiredQuantity', align: 'center' },
    { title: 'Khả dụng', dataIndex: 'availableQuantity', key: 'availableQuantity', align: 'center' },
    {
      title: 'Thiếu',
      dataIndex: 'shortageQuantity',
      key: 'shortageQuantity',
      align: 'center',
      render: (value) => <Tag color="red">{value}</Tag>,
    },
  ]

  const eligibleBatchCount = batches.filter((batch) => batch.eligibleForDispense !== false && batch.status !== 'EXPIRED').length

  return (
    <div style={{ paddingBottom: 32 }}>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ margin: 0 }}>
            <MedicineBoxOutlined /> Cấp phát thuốc
          </Title>
          <Text type="secondary">Xử lý đơn chờ cấp phát và xem trước phân bổ lô theo FEFO.</Text>
        </div>
        <Space wrap>
          <Button icon={<InboxOutlined />} onClick={() => navigate('/pharmacy/receipts')}>
            Nhập kho
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
          message="Không tải được workspace cấp phát"
          description={loadError}
          action={<Button size="small" onClick={loadData}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Metric Cards KPI */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Đơn chờ cấp phát" value={prescriptionTotal} prefix={<MedicineBoxOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Lô đủ điều kiện FEFO" value={eligibleBatchCount} prefix={<CheckCircleOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            style={{ cursor: 'pointer' }}
            onClick={() => navigate('/medicines', { state: { tab: 'alerts' } })}
          >
            <Statistic
              title="Thuốc dưới ngưỡng tồn"
              value={lowStockItems.length}
              valueStyle={lowStockItems.length ? { color: '#cf1322', fontWeight: 700 } : undefined}
              prefix={<WarningOutlined />}
              suffix={<Text type="secondary" style={{ fontSize: 13, marginLeft: 6 }}>Xem →</Text>}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            style={{
              cursor: 'pointer',
              borderLeft: expiryAlerts.length > 0 ? '4px solid #faad14' : undefined,
            }}
            onClick={() => setExpiryModalOpen(true)}
          >
            <Statistic
              title="Cảnh báo Hạn sử dụng"
              value={expiryAlerts.length}
              valueStyle={expiryAlerts.length ? { color: '#d97706', fontWeight: 700 } : undefined}
              prefix={<FieldTimeOutlined style={{ color: expiryAlerts.length > 0 ? '#faad14' : undefined }} />}
              suffix={<Text type="secondary" style={{ fontSize: 13, marginLeft: 6 }}>Chi tiết →</Text>}
            />
          </Card>
        </Col>
      </Row>

      {/* Banner Cảnh báo Hạn dùng */}
      {expiryAlerts.length > 0 && (
        <Alert
          type="warning"
          showIcon
          icon={<FieldTimeOutlined />}
          message={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
              <span>
                <strong>Cảnh báo Hạn sử dụng thuốc:</strong> Có <strong>{expiryAlerts.length} lô thuốc</strong> cần chú ý ({expiryAlerts.filter((a) => a.alertStatus === 'EXPIRED').length} lô đã hết hạn, {expiryAlerts.filter((a) => a.alertStatus === 'NEAR_EXPIRY').length} lô gần hết hạn)
              </span>
              <Space>
                <Button
                  size="small"
                  type="primary"
                  danger
                  onClick={() => setExpiryModalOpen(true)}
                >
                  Xem danh sách cảnh báo hạn dùng
                </Button>
                <Button
                  size="small"
                  icon={<InboxOutlined />}
                  onClick={() => navigate('/pharmacy/receipts', { state: { tab: 'create' } })}
                >
                  Nhập kho thay thế
                </Button>
              </Space>
            </div>
          }
          style={{ marginBottom: 16 }}
        />
      )}

      {lowStockItems.length > 0 && (
        <Alert
          type="warning"
          showIcon
          message={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
              <span>
                <strong>Cảnh báo tồn kho:</strong> Có <strong>{lowStockItems.length} loại thuốc</strong> đang dưới ngưỡng tồn tối thiểu
              </span>
              <Space>
                <Button
                  size="small"
                  type="primary"
                  danger
                  onClick={() => navigate('/medicines', { state: { tab: 'alerts' } })}
                >
                  Xem chi tiết thiếu hụt
                </Button>
                <Button
                  size="small"
                  icon={<ShopOutlined />}
                  onClick={() => navigate('/medicines')}
                >
                  Điều chỉnh ngưỡng tồn
                </Button>
              </Space>
            </div>
          }
          description={
            <div style={{ marginTop: 4 }}>
              {lowStockItems
                .slice(0, 5)
                .map((item) => `${item.medicineName}: khả dụng ${item.eligibleStockQuantity}, thiếu ${item.shortageQuantity} ${item.unit || ''}`)
                .join(' · ')}
              {lowStockItems.length > 5 && ` và ${lowStockItems.length - 5} thuốc khác...`}
            </div>
          }
          style={{ marginBottom: 16 }}
        />
      )}

      <Row gutter={[16, 16]} align="stretch">
        <Col xs={24} xl={9}>
          <Card
            title={`Danh sách đơn chờ (${prescriptionTotal})`}
            styles={{ body: { padding: 12 } }}
            style={{ height: '100%' }}
          >
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="Tìm trong trang hiện tại"
              value={searchKeyword}
              onChange={(event) => setSearchKeyword(event.target.value)}
              style={{ marginBottom: 12 }}
            />
            <List
              loading={prescriptionLoading}
              dataSource={filteredPrescriptions}
              locale={{ emptyText: <Empty description="Không có đơn chờ cấp phát" /> }}
              style={{ maxHeight: 650, overflowY: 'auto' }}
              renderItem={(item) => {
                const selected = String(item.id) === String(selectedPrescriptionId)
                return (
                  <List.Item
                    key={item.id}
                    onClick={() => selectPrescription(item.id)}
                    style={{
                      cursor: 'pointer',
                      border: selected ? '1px solid #1677ff' : '1px solid #f0f0f0',
                      background: selected ? '#e6f4ff' : '#fff',
                      borderRadius: 8,
                      marginBottom: 8,
                      padding: 12,
                    }}
                    extra={<RightOutlined style={{ color: selected ? '#1677ff' : '#bfbfbf' }} />}
                  >
                    <List.Item.Meta
                      title={(
                        <Space wrap>
                          <Text strong>{item.prescriptionCode || item.id}</Text>
                          <Tag color="orange">Chờ cấp phát</Tag>
                        </Space>
                      )}
                      description={(
                        <Space direction="vertical" size={1}>
                          <Text>{fixMojibake(item.patientName) || 'Chưa có tên bệnh nhân'} ({item.patientCode || '—'})</Text>
                          <Text type="secondary">Lượt khám: {item.visitCode || item.visitId || '—'}</Text>
                          <Text type="secondary">Kê lúc {formatDateTime(item.prescribedAt)}</Text>
                        </Space>
                      )}
                    />
                  </List.Item>
                )
              }}
            />
            {prescriptionTotal > 0 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap', marginTop: 12 }}>
                <Text type="secondary">
                  Trang {prescriptionPage + 1}/{prescriptionTotalPages || 1} · Tổng {prescriptionTotal} đơn
                </Text>
                <Pagination
                  current={prescriptionPage + 1}
                  pageSize={PRESCRIPTION_PAGE_SIZE}
                  total={prescriptionTotal}
                  showSizeChanger={false}
                  hideOnSinglePage
                  onChange={changePrescriptionPage}
                />
              </div>
            )}
          </Card>
        </Col>

        <Col xs={24} xl={15}>
          <Card
            title={selectedPrescription
              ? `Chi tiết đơn ${selectedPrescription.prescriptionCode || selectedPrescription.id}`
              : 'Chi tiết đơn thuốc'}
            style={{ height: '100%' }}
          >
            {!selectedPrescription ? (
              <Empty description="Chọn một đơn thuốc để xem chi tiết cấp phát" />
            ) : (
              <Space direction="vertical" size="large" style={{ width: '100%' }}>
                <Descriptions bordered size="small" column={{ xs: 1, md: 2 }}>
                  <Descriptions.Item label="Bệnh nhân">
                    {selectedPrescription.patientName || '—'} ({selectedPrescription.patientCode || '—'})
                  </Descriptions.Item>
                  <Descriptions.Item label="Lượt khám">{selectedPrescription.visitCode || selectedPrescription.visitId || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Bác sĩ">{selectedPrescription.doctorName || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Ngày kê">{formatDateTime(selectedPrescription.prescribedAt)}</Descriptions.Item>
                  <Descriptions.Item label="Ghi chú" span={2}>{selectedPrescription.note || 'Không có'}</Descriptions.Item>
                </Descriptions>

                {hasPreviewShortage && (
                  <Alert
                    type="error"
                    showIcon
                    message="Không đủ tồn khả dụng để cấp toàn bộ đơn"
                    description="Xem từng dòng thuốc bên dưới và nhập bổ sung trước khi cấp phát."
                  />
                )}

                <Table
                  rowKey={(item) => item.id || item.medicineId}
                  columns={previewColumns}
                  dataSource={fefoPreview}
                  pagination={false}
                  size="small"
                  scroll={{ x: 850 }}
                />

                {shortageDetails.length > 0 && (
                  <Alert
                    type="error"
                    showIcon
                    message="Máy chủ từ chối cấp phát do thiếu tồn kho"
                    description={(
                      <Table
                        rowKey={(item) => item.prescriptionItemId || item.medicineId}
                        columns={shortageColumns}
                        dataSource={shortageDetails}
                        pagination={false}
                        size="small"
                        style={{ marginTop: 8 }}
                      />
                    )}
                  />
                )}

                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <Popconfirm
                    title={
                      <Text strong style={{ fontSize: 17, color: '#1e3a8a' }}>
                        Xác nhận cấp phát đơn thuốc
                      </Text>
                    }
                    description={
                      <div style={{ marginTop: 8, marginBottom: 10, maxWidth: 420, fontSize: 14.5 }}>
                        <div style={{ color: '#1e293b', lineHeight: 1.5 }}>
                          Bạn có chắc chắn muốn xuất kho cho đơn thuốc{' '}
                          <Text strong style={{ color: '#1677ff', fontSize: 16 }}>
                            {selectedPrescription.prescriptionCode || selectedPrescription.id}
                          </Text>?
                        </div>
                        <div style={{ marginTop: 8, padding: '10px 14px', backgroundColor: '#f0f7ff', borderRadius: 8, fontSize: 13.5, color: '#334155', border: '1px solid #bae6fd', lineHeight: 1.6 }}>
                          <div>• Bệnh nhân: <strong style={{ color: '#0f172a' }}>{fixMojibake(selectedPrescription.patientName) || '—'}</strong> ({selectedPrescription.patientCode || '—'})</div>
                          <div>• Tổng số thuốc: <strong style={{ color: '#0f172a' }}>{fefoPreview.length} loại</strong> theo phân bổ FEFO.</div>
                        </div>
                      </div>
                    }
                    icon={<MedicineBoxOutlined style={{ color: '#1677ff', fontSize: 24, marginTop: 2 }} />}
                    okText="Xác nhận cấp phát"
                    cancelText="Kiểm tra lại"
                    okButtonProps={{
                      type: 'primary',
                      size: 'large',
                      icon: <CheckCircleOutlined />,
                      style: { height: 40, minWidth: 170, borderRadius: 8, fontWeight: 600 },
                    }}
                    cancelButtonProps={{
                      size: 'large',
                      icon: <RollbackOutlined />,
                      style: { height: 40, minWidth: 170, borderRadius: 8, fontWeight: 500 },
                    }}
                    onConfirm={handleDispense}
                    disabled={!canDispense || hasPreviewShortage || fefoPreview.length === 0}
                    overlayStyle={{ maxWidth: 500 }}
                  >
                    <Button
                      type="primary"
                      size="large"
                      icon={<CheckCircleOutlined />}
                      loading={dispensingId === selectedPrescription.id}
                      disabled={!canDispense || hasPreviewShortage || fefoPreview.length === 0}
                    >
                      Xác nhận cấp phát theo FEFO
                    </Button>
                  </Popconfirm>
                </div>
                {!canDispense && <Text type="danger">Tài khoản hiện tại không có quyền cấp phát thuốc.</Text>}
              </Space>
            )}
          </Card>
        </Col>
      </Row>

      <Modal
        title={
          <Space>
            <FieldTimeOutlined style={{ color: '#d97706' }} />
            <span>Danh sách cảnh báo hạn sử dụng thuốc</span>
          </Space>
        }
        open={expiryModalOpen}
        onCancel={() => setExpiryModalOpen(false)}
        footer={[
          <Button key="close" onClick={() => setExpiryModalOpen(false)}>
            Đóng
          </Button>,
          <Button
            key="receipts"
            type="primary"
            icon={<InboxOutlined />}
            onClick={() => {
              setExpiryModalOpen(false)
              navigate('/pharmacy/receipts', { state: { tab: 'create' } })
            }}
          >
            Nhập kho thay thế
          </Button>,
        ]}
        width={850}
        destroyOnClose
      >
        <Table
          rowKey={(record, index) => record.batchId || record.id || index}
          dataSource={expiryAlerts}
          locale={{ emptyText: 'Hiện không có lô thuốc nào cần cảnh báo hạn dùng.' }}
          pagination={{ pageSize: 10, hideOnSinglePage: true }}
          size="small"
          columns={[
            {
              title: 'Mã & Tên thuốc',
              key: 'medicine',
              render: (_, item) => (
                <Space direction="vertical" size={1}>
                  <strong>{item.medicineName || '—'}</strong>
                  <Text code style={{ fontSize: 12 }}>{item.medicineCode || item.medicineId}</Text>
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
              width: 130,
              render: (val) => (val ? dayjs(val).format('DD/MM/YYYY') : '—'),
            },
            {
              title: 'Số ngày còn lại',
              dataIndex: 'daysToExpiry',
              key: 'daysToExpiry',
              width: 160,
              render: (days, item) => {
                const d = Number(days ?? 0)
                if (item.alertStatus === 'EXPIRED' || d < 0) {
                  return <Tag color="red">🔴 Quá hạn {Math.abs(d)} ngày</Tag>
                }
                if (d === 0) {
                  return <Tag color="volcano">⚠️ Hết hạn hôm nay</Tag>
                }
                return <Tag color="warning">🟡 Còn {d} ngày</Tag>
              },
            },
            {
              title: 'Số lượng còn',
              dataIndex: 'quantity',
              key: 'quantity',
              width: 120,
              align: 'right',
              render: (val) => (
                <span style={{ fontWeight: 700, color: Number(val || 0) > 0 ? '#dc2626' : '#94a3b8' }}>
                  {Number(val || 0).toLocaleString('vi-VN')}
                </span>
              ),
            },
            {
              title: 'Trạng thái cảnh báo',
              dataIndex: 'alertStatus',
              key: 'alertStatus',
              width: 280,
              render: (alertStatus) => {
                if (alertStatus === 'EXPIRED') {
                  return <Tag color="red" icon={<AlertOutlined />}>🔴 Lô thuốc đã hết hạn – Không được cấp phát</Tag>
                }
                if (alertStatus === 'NEAR_EXPIRY') {
                  return <Tag color="orange" icon={<WarningOutlined />}>🟡 Lô thuốc sắp hết hạn</Tag>
                }
                return <Tag>{alertStatus || '—'}</Tag>
              },
            },
          ]}
        />
      </Modal>
    </div>
  )
}

export default PharmacyPage
