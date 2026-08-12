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
  message,
  Pagination,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  InboxOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  RightOutlined,
  SearchOutlined,
  ShopOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'
import { buildFefoPreview } from '../utils/workflowContract'

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
      const nextPrescriptions = toCollection(payload)
        .filter((item) => item?.status === 'PENDING_DISPENSE')
        .sort((first, second) =>
          String(first.prescribedAt || '').localeCompare(String(second.prescribedAt || '')),
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
      const [batchResponse, stockResponse, lowStockResponse] = await Promise.all([
        pharmacyApi.batches(),
        pharmacyApi.stocks({ active: true }),
        pharmacyApi.lowStock(),
      ])
      if (requestId !== inventoryRequestIdRef.current) return

      const nextBatches = toCollection(batchResponse.data).map(normalizeBatch)

      setBatches(nextBatches)
      setStocks(toCollection(stockResponse.data))
      setLowStockItems(toCollection(lowStockResponse.data))
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

    setDispensingId(selectedPrescription.id)
    setShortageDetails([])
    try {
      const response = await pharmacyApi.dispense(selectedPrescription.id)
      const allocationCount = Number(response.data?.allocationCount) || 0
      message.success(
        `Đã cấp phát đơn ${selectedPrescription.prescriptionCode || selectedPrescription.id}`
        + (allocationCount ? ` qua ${allocationCount} lượt phân bổ FEFO.` : '.'),
      )
      await loadData()
    } catch (error) {
      const responseData = error?.response?.data
      if (error?.response?.status === 409 && Array.isArray(responseData?.details)) {
        setShortageDetails(responseData.details)
      }
      message.error(getErrorMessage(error, 'Không thể cấp phát đơn thuốc.'))
    } finally {
      setDispensingId(null)
    }
  }

  const previewColumns = [
    {
      title: 'Thuốc',
      key: 'medicine',
      width: 230,
      render: (_, item) => {
        const isShortage = Number(item.shortageQuantity) > 0
        return (
          <Space direction="vertical" size={2}>
            <Space wrap>
              <Text strong style={{ color: isShortage ? '#cf1322' : 'inherit', fontSize: 14 }}>
                {item.medicineName || item.medicineId}
              </Text>
              {isShortage && <Tag color="error" style={{ fontWeight: 600 }}>Không đủ thuốc</Tag>}
            </Space>
            <Text type={isShortage ? 'danger' : 'secondary'} style={{ fontSize: 12 }}>
              {[item.activeIngredient, item.strength].filter(Boolean).join(' · ') || item.dosage || '—'}
            </Text>
          </Space>
        )
      },
    },
    {
      title: 'Cần cấp',
      dataIndex: 'requiredQuantity',
      width: 95,
      align: 'center',
      render: (quantity, item) => (
        <Text strong style={{ color: Number(item.shortageQuantity) > 0 ? '#cf1322' : 'inherit', fontSize: 14 }}>
          {quantity} {item.unit || ''}
        </Text>
      ),
    },
    {
      title: 'Tồn khả dụng',
      key: 'available',
      width: 120,
      align: 'center',
      render: (_, item) => {
        const stock = stockByMedicineId.get(String(item.medicineId))
        const available = stock?.eligibleStockQuantity ?? item.availableQuantity
        const isShortage = Number(available) < Number(item.requiredQuantity)
        return (
          <Tag color={isShortage ? 'error' : 'success'} style={{ fontWeight: 600, fontSize: 13, padding: '2px 8px' }}>
            {available} {item.unit || ''}
          </Tag>
        )
      },
    },
    {
      title: 'Phân bổ FEFO dự kiến',
      key: 'allocations',
      render: (_, item) => item.allocations.length ? (
        <Space direction="vertical" size={4}>
          {item.allocations.map((allocation) => (
            <Tag key={`${allocation.batchId}-${allocation.quantity}`} color="blue" style={{ fontSize: 12 }}>
              <strong>Lô {allocation.batchNumber}</strong> · HSD {dayjs(allocation.expiryDate).format('DD/MM/YYYY')} · <strong>x{allocation.quantity}</strong>
            </Tag>
          ))}
        </Space>
      ) : (
        <Tag color="error" style={{ fontWeight: 600 }}>❌ Không có lô đủ điều kiện cấp phát</Tag>
      ),
    },
    {
      title: 'Kết quả kiểm tra',
      key: 'result',
      width: 150,
      align: 'center',
      render: (_, item) => Number(item.shortageQuantity) > 0 ? (
        <Tag color="error" style={{ fontWeight: 700, padding: '4px 10px', fontSize: 13 }}>
          ⛔ THIẾU {item.shortageQuantity} {item.unit || ''}
        </Tag>
      ) : (
        <Tag color="success" icon={<CheckCircleOutlined />} style={{ fontWeight: 600, padding: '4px 10px' }}>
          Đủ tồn kho
        </Tag>
      ),
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

  const eligibleBatchCount = batches.filter((batch) => batch.eligibleForDispense !== false).length

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

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} md={8}>
          <Card><Statistic title="Đơn chờ cấp phát" value={prescriptionTotal} prefix={<MedicineBoxOutlined />} /></Card>
        </Col>
        <Col xs={24} md={8}>
          <Card><Statistic title="Lô đủ điều kiện FEFO" value={eligibleBatchCount} prefix={<CheckCircleOutlined />} /></Card>
        </Col>
        <Col xs={24} md={8}>
          <Card
            style={{ cursor: 'pointer' }}
            onClick={() => navigate('/medicines', { state: { tab: 'alerts' } })}
          >
            <Statistic
              title="Thuốc dưới ngưỡng tồn (Cảnh báo)"
              value={lowStockItems.length}
              valueStyle={lowStockItems.length ? { color: '#cf1322', fontWeight: 700 } : undefined}
              prefix={<WarningOutlined />}
              suffix={<Text type="secondary" style={{ fontSize: 13, marginLeft: 6 }}>Xem →</Text>}
            />
          </Card>
        </Col>
      </Row>

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
                          <Text>{item.patientName || 'Chưa có tên bệnh nhân'} ({item.patientCode || '—'})</Text>
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
                    message={
                      <span style={{ fontSize: 15, fontWeight: 700, color: '#cf1322' }}>
                        ⛔ KHÔNG ĐỦ THUỐC ĐỂ CẤP PHÁT (ĐÃ KHÓA NÚT CẤP PHÁT)
                      </span>
                    }
                    description={
                      <div style={{ marginTop: 4 }}>
                        <span>
                          Đơn thuốc có một hoặc nhiều loại thuốc <strong>không đủ tồn kho khả dụng</strong> (được bôi đỏ ở bảng bên dưới). Hệ thống đã vô hiệu hóa thao tác cấp phát để đảm bảo an toàn.
                        </span>
                        <div style={{ marginTop: 8 }}>
                          <Button
                            type="primary"
                            danger
                            size="small"
                            icon={<InboxOutlined />}
                            onClick={() => navigate('/pharmacy/receipts')}
                          >
                            Đi đến Nhập kho theo lô để bổ sung thuốc
                          </Button>
                        </div>
                      </div>
                    }
                  />
                )}

                <Table
                  rowKey={(item) => item.id || item.medicineId}
                  columns={previewColumns}
                  dataSource={fefoPreview}
                  pagination={false}
                  size="middle"
                  scroll={{ x: 850 }}
                  onRow={(record) => {
                    const isShortage = Number(record.shortageQuantity) > 0
                    return {
                      style: isShortage
                        ? {
                            backgroundColor: '#fff1f0',
                            borderLeft: '4px solid #ff4d4f',
                          }
                        : {},
                    }
                  }}
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

                <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 12, marginTop: 8 }}>
                  {hasPreviewShortage && (
                    <Text type="danger" strong style={{ fontSize: 13 }}>
                      ⚠️ Vui lòng bổ sung kho trước khi cấp phát
                    </Text>
                  )}
                  <Tooltip
                    title={
                      hasPreviewShortage
                        ? 'Không thể cấp phát: Đơn thuốc có thuốc bị thiếu tồn kho (dòng bôi đỏ).'
                        : (!canDispense ? 'Tài khoản không có quyền cấp phát thuốc.' : '')
                    }
                  >
                    <span>
                      <Popconfirm
                        title="Xác nhận cấp phát thuốc"
                        description={`Cấp đơn ${selectedPrescription.prescriptionCode || selectedPrescription.id} theo phân bổ FEFO đang hiển thị?`}
                        okText="Xác nhận cấp phát"
                        cancelText="Kiểm tra lại"
                        onConfirm={handleDispense}
                        disabled={!canDispense || hasPreviewShortage || fefoPreview.length === 0}
                      >
                        <Button
                          type="primary"
                          size="large"
                          icon={<CheckCircleOutlined style={{ fontSize: 18 }} />}
                          loading={dispensingId === selectedPrescription.id}
                          disabled={!canDispense || hasPreviewShortage || fefoPreview.length === 0}
                          style={{
                            height: 48,
                            minWidth: 260,
                            fontSize: 16,
                            fontWeight: 700,
                            borderRadius: 8,
                            boxShadow: (hasPreviewShortage || !canDispense) ? 'none' : '0 4px 14px rgba(22, 119, 255, 0.4)',
                          }}
                        >
                          Xác nhận cấp phát theo FEFO
                        </Button>
                      </Popconfirm>
                    </span>
                  </Tooltip>
                </div>
                {!canDispense && <Text type="danger">Tài khoản hiện tại không có quyền cấp phát thuốc.</Text>}
              </Space>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default PharmacyPage
