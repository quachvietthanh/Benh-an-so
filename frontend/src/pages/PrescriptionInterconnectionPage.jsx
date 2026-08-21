import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Divider,
  Empty,
  Input,
  Modal,
  Popconfirm,
  Progress,
  Radio,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  BarcodeOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  CloudServerOutlined,
  CloudUploadOutlined,
  CopyOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  FilterOutlined,
  MedicineBoxOutlined,
  RedoOutlined,
  ReloadOutlined,
  SearchOutlined,
  SyncOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import pharmacyApi from '../api/pharmacyApi'
import PrescriptionDetailModal from '../components/pharmacy/PrescriptionDetailModal'
import { useAuthContext } from '../context/AuthContext'
import { getApiErrorMessage as getApiMessage } from '../utils/apiError'
import {
  formatPrescriptionCode,
  getInterconnectionStatusInfo,
  INTERCONNECTION_STATUS_CONFIG,
  isStandardRxCode,
} from '../utils/electronicPrescriptionValidation'
import { fixMojibake } from '../utils/workflowContract'

const { Title, Text, Paragraph } = Typography
const { RangePicker } = DatePicker

const PRESCRIPTION_STATUS_TAGS = {
  PENDING_DISPENSE: { label: 'Chờ cấp phát', color: 'orange' },
  DISPENSED: { label: 'Đã cấp phát', color: 'green' },
  CANCELLED: { label: 'Đã hủy', color: 'default' },
}

function PrescriptionInterconnectionPage() {
  const { user } = useAuthContext()
  const userRoles = useMemo(() => {
    return (user?.roles || [user?.role || '']).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  }, [user])
  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [user])

  const isAdmin = userRoles.includes('admin')
  const isManager = userRoles.includes('manager') || userRoles.includes('clinic_manager')
  const canRead = userPermissions.includes('PRESCRIPTION_INTERCONNECTION_READ') || isAdmin || isManager
  const canRetry = userPermissions.includes('PRESCRIPTION_INTERCONNECTION_RETRY') || isAdmin || isManager

  // Filter states
  const [currentTabStatus, setCurrentTabStatus] = useState('FAILED') // 'FAILED' | 'NOT_SENT' | 'SUCCESS'
  const [dateRange, setDateRange] = useState(null)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)

  // Data states
  const [loading, setLoading] = useState(false)
  const [dataList, setDataList] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [loadError, setLoadError] = useState(null)

  // Summary counts across 3 statuses
  const [statusCounts, setStatusCounts] = useState({
    FAILED: 0,
    NOT_SENT: 0,
    SUCCESS: 0,
  })

  // Action states
  const [retryingId, setRetryingId] = useState(null)
  const [batchRetrying, setBatchRetrying] = useState(false)
  const [batchProgress, setBatchProgress] = useState({ current: 0, total: 0 })

  // Detail modal states
  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [selectedPrescriptionDetail, setSelectedPrescriptionDetail] = useState(null)
  const [loadingDetail, setLoadingDetail] = useState(false)

  // Fetch summary counts for all 3 statuses
  const fetchStatusCounts = useCallback(async () => {
    if (!canRead) return
    try {
      const [failedRes, notSentRes, successRes] = await Promise.allSettled([
        pharmacyApi.searchInterconnections({ status: 'FAILED', page: 0, size: 1 }),
        pharmacyApi.searchInterconnections({ status: 'NOT_SENT', page: 0, size: 1 }),
        pharmacyApi.searchInterconnections({ status: 'SUCCESS', page: 0, size: 1 }),
      ])

      setStatusCounts({
        FAILED: failedRes.status === 'fulfilled' ? (failedRes.value?.data?.totalElements ?? 0) : 0,
        NOT_SENT: notSentRes.status === 'fulfilled' ? (notSentRes.value?.data?.totalElements ?? 0) : 0,
        SUCCESS: successRes.status === 'fulfilled' ? (successRes.value?.data?.totalElements ?? 0) : 0,
      })
    } catch {
      // ignore
    }
  }, [canRead])

  // Fetch main list for active tab
  const fetchData = useCallback(async () => {
    if (!canRead) return
    setLoading(true)
    setLoadError(null)
    try {
      const params = {
        status: currentTabStatus,
        page,
        size: pageSize,
      }

      if (dateRange && dateRange[0] && dateRange[1]) {
        params.from = dateRange[0].startOf('day').toISOString()
        params.to = dateRange[1].endOf('day').toISOString()
      }

      const response = await pharmacyApi.searchInterconnections(params)
      const resData = response.data || {}

      let items = []
      if (Array.isArray(resData.content)) {
        items = resData.content
        setTotalElements(resData.totalElements ?? items.length)
      } else if (Array.isArray(resData)) {
        items = resData
        setTotalElements(items.length)
      } else {
        items = []
        setTotalElements(0)
      }

      setDataList(items)
      fetchStatusCounts()
    } catch (err) {
      setLoadError(getApiMessage(err, 'Không thể tải danh sách liên thông đơn thuốc từ máy chủ.'))
      setDataList([])
    } finally {
      setLoading(false)
    }
  }, [canRead, currentTabStatus, page, pageSize, dateRange, fetchStatusCounts])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  // Filter data by search keyword
  const filteredData = useMemo(() => {
    const kw = searchKeyword.trim().toLowerCase()
    if (!kw) return dataList
    return dataList.filter((item) => {
      const matchCode = String(item.prescriptionCode || '').toLowerCase().includes(kw)
      const matchPatient = String(item.patientName || item.patientCode || '').toLowerCase().includes(kw)
      const matchDoctor = String(item.doctorName || '').toLowerCase().includes(kw)
      const matchReceipt = String(item.receiptCode || '').toLowerCase().includes(kw)
      const matchError = String(item.lastInterconnectionError || '').toLowerCase().includes(kw)
      return matchCode || matchPatient || matchDoctor || matchReceipt || matchError
    })
  }, [dataList, searchKeyword])

  // Handle single retry
  const handleRetrySubmission = async (prescriptionId, prescriptionCode) => {
    if (!canRetry || retryingId) return
    setRetryingId(prescriptionId)
    try {
      const response = await pharmacyApi.retryInterconnection(prescriptionId)
      const result = response.data || {}
      if (result.status === 'SUCCESS') {
        message.success(
          `Gửi lại đơn ${prescriptionCode || ''} thành công! Mã biên nhận: ${result.receiptCode}`,
        )
      } else {
        message.error(
          `Gửi lại đơn ${prescriptionCode || ''} thất bại: ${result.failureReason || 'Cổng liên thông từ chối'}`,
        )
      }
      await fetchData()
    } catch (err) {
      message.error(getApiMessage(err, `Không thể gửi lại đơn ${prescriptionCode || ''}`))
    } finally {
      setRetryingId(null)
    }
  }

  // Handle batch retry for all FAILED items currently on the page
  const handleBatchRetry = async () => {
    const failedItems = filteredData.filter((i) => i.interconnectionStatus === 'FAILED' || currentTabStatus === 'FAILED')
    if (failedItems.length === 0 || batchRetrying) return

    setBatchRetrying(true)
    setBatchProgress({ current: 0, total: failedItems.length })
    let successCount = 0
    let failureCount = 0

    for (let i = 0; i < failedItems.length; i++) {
      const item = failedItems[i]
      setBatchProgress({ current: i + 1, total: failedItems.length })
      try {
        const res = await pharmacyApi.retryInterconnection(item.prescriptionId)
        if (res.data?.status === 'SUCCESS') {
          successCount++
        } else {
          failureCount++
        }
      } catch {
        failureCount++
      }
    }

    setBatchRetrying(false)
    message.info(
      `Hoàn tất gửi lại: ${successCount} đơn thành công, ${failureCount} đơn thất bại.`,
    )
    await fetchData()
  }

  // Open prescription detail modal
  const handleOpenDetail = async (prescriptionId) => {
    if (!prescriptionId) return
    setLoadingDetail(true)
    try {
      const res = await pharmacyApi.getById(prescriptionId)
      setSelectedPrescriptionDetail(res.data)
      setDetailModalOpen(true)
    } catch (err) {
      message.error(getApiMessage(err, 'Không thể tải chi tiết đơn thuốc.'))
    } finally {
      setLoadingDetail(false)
    }
  }

  // Metrics calculation
  const totalAll = statusCounts.FAILED + statusCounts.NOT_SENT + statusCounts.SUCCESS
  const successRate = totalAll > 0 ? Math.round((statusCounts.SUCCESS / totalAll) * 100) : 100

  const columns = [
    {
      title: 'Mã đơn điện tử',
      dataIndex: 'prescriptionCode',
      key: 'prescriptionCode',
      width: 170,
      render: (code, row) => {
        const displayCode = formatPrescriptionCode(code || row.prescriptionId)

        return (
          <Space direction="vertical" size={2}>
            <Space size={4} align="center">
              <Tag
                color="blue"
                style={{
                  fontSize: 13,
                  fontWeight: 700,
                  cursor: 'pointer',
                  padding: '2px 8px',
                  borderRadius: 4,
                  backgroundColor: '#eff6ff',
                  borderColor: '#93c5fd',
                  color: '#1d4ed8',
                  whiteSpace: 'nowrap',
                }}
                onClick={() => handleOpenDetail(row.prescriptionId)}
              >
                <BarcodeOutlined style={{ marginRight: 4 }} />
                {displayCode}
              </Tag>
              <Tooltip title="Sao chép mã đơn">
                <Button
                  type="text"
                  size="small"
                  icon={<CopyOutlined style={{ color: '#2563eb', fontSize: 12 }} />}
                  onClick={(e) => {
                    e.stopPropagation()
                    if (displayCode && displayCode !== '—') {
                      navigator.clipboard.writeText(displayCode)
                      message.success(`Đã sao chép: ${displayCode}`)
                    }
                  }}
                  style={{ padding: '0 4px', height: 20, width: 20 }}
                />
              </Tooltip>
            </Space>
            <Tag color="cyan" style={{ fontSize: 10, margin: 0, whiteSpace: 'nowrap' }}>
              Định danh duy nhất
            </Tag>
          </Space>
        )
      },
    },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      width: 180,
      render: (_, row) => (
        <Space align="start" size={8}>
          <Avatar size="small" icon={<UserOutlined />} style={{ backgroundColor: '#0284c7', marginTop: 2 }} />
          <div>
            <div style={{ fontWeight: 600, color: '#0f172a', whiteSpace: 'nowrap' }}>
              {fixMojibake(row.patientName) || 'Bệnh nhân'}
            </div>
            <Text type="secondary" style={{ fontSize: 12, whiteSpace: 'nowrap' }}>
              Mã BN: <Text code style={{ fontSize: 11 }}>{row.patientCode || '—'}</Text>
            </Text>
          </div>
        </Space>
      ),
    },
    {
      title: 'Bác sĩ kê đơn',
      dataIndex: 'doctorName',
      key: 'doctorName',
      width: 160,
      render: (doctor) => (
        <Text strong style={{ color: '#334155', whiteSpace: 'nowrap' }}>
          {fixMojibake(doctor) || '—'}
        </Text>
      ),
    },
    {
      title: 'Trạng thái đơn',
      dataIndex: 'prescriptionStatus',
      key: 'prescriptionStatus',
      width: 130,
      render: (status) => {
        const config = PRESCRIPTION_STATUS_TAGS[status] || { label: status || '—', color: 'default' }
        return <Tag color={config.color} style={{ whiteSpace: 'nowrap' }}>{config.label}</Tag>
      },
    },
    {
      title: 'Trạng thái liên thông',
      key: 'interconnectionStatus',
      width: 200,
      render: (_, row) => {
        const info = getInterconnectionStatusInfo(
          row.interconnectionStatus,
          row.receiptCode,
          row.lastInterconnectionError,
        )

        return (
          <Space direction="vertical" size={2}>
            <Tag color={info.color} style={{ fontWeight: 600, fontSize: 12, whiteSpace: 'nowrap' }}>
              {info.isSuccess && <CheckCircleOutlined style={{ marginRight: 4 }} />}
              {info.isFailed && <CloseCircleOutlined style={{ marginRight: 4 }} />}
              {info.isNotSent && <CloudServerOutlined style={{ marginRight: 4 }} />}
              {info.label}
            </Tag>
            {row.lastInterconnectionAt && (
              <Text type="secondary" style={{ fontSize: 11, whiteSpace: 'nowrap' }}>
                <ClockCircleOutlined style={{ marginRight: 3 }} />
                {dayjs(row.lastInterconnectionAt).format('HH:mm DD/MM/YYYY')}
              </Text>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Mã biên nhận / Chi tiết lỗi',
      key: 'resultDetail',
      width: 270,
      render: (_, row) => {
        if (row.interconnectionStatus === 'SUCCESS' && row.receiptCode) {
          return (
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, whiteSpace: 'nowrap' }}>
              <Tag
                color="green"
                style={{
                  fontSize: 13,
                  fontWeight: 700,
                  letterSpacing: 0.5,
                  padding: '3px 10px',
                  borderRadius: 6,
                  borderColor: '#86efac',
                  backgroundColor: '#f0fdf4',
                  color: '#15803d',
                  margin: 0,
                  whiteSpace: 'nowrap',
                  fontFamily: 'monospace',
                }}
              >
                {row.receiptCode}
              </Tag>
              <Tooltip title="Sao chép mã biên nhận">
                <Button
                  type="text"
                  size="small"
                  icon={<CopyOutlined style={{ color: '#16a34a', fontSize: 13 }} />}
                  onClick={() => {
                    navigator.clipboard.writeText(row.receiptCode)
                    message.success(`Đã sao chép mã biên nhận: ${row.receiptCode}`)
                  }}
                  style={{ padding: '0 4px', height: 22, width: 22 }}
                />
              </Tooltip>
            </div>
          )
        }

        if (row.interconnectionStatus === 'FAILED' && row.lastInterconnectionError) {
          return (
            <Alert
              type="error"
              showIcon
              message={
                <span style={{ fontSize: 12, color: '#991b1b' }}>
                  <strong>Lỗi:</strong> {row.lastInterconnectionError}
                </span>
              }
              style={{ padding: '4px 8px', borderRadius: 6 }}
            />
          )
        }

        return <Text type="secondary" style={{ fontStyle: 'italic', fontSize: 12, whiteSpace: 'nowrap' }}>Chưa có biên nhận</Text>
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 120,
      align: 'center',
      fixed: 'right',
      render: (_, row) => {
        const isRetryingThis = retryingId === row.prescriptionId
        const isFailed = row.interconnectionStatus === 'FAILED'

        return (
          <Space size={6} style={{ whiteSpace: 'nowrap' }}>
            {isFailed && canRetry && (
              <Popconfirm
                title="Gửi lại đơn thuốc này lên Cổng liên thông?"
                description={`Mã đơn: ${row.prescriptionCode || row.prescriptionId}`}
                okText="Gửi lại ngay"
                cancelText="Hủy"
                onConfirm={() => handleRetrySubmission(row.prescriptionId, row.prescriptionCode)}
              >
                <Button
                  type="primary"
                  size="small"
                  danger
                  icon={<RedoOutlined spin={isRetryingThis} />}
                  loading={isRetryingThis}
                >
                  Gửi lại
                </Button>
              </Popconfirm>
            )}

            <Tooltip title="Xem chi tiết đơn thuốc">
              <Button
                size="small"
                icon={<EyeOutlined />}
                onClick={() => handleOpenDetail(row.prescriptionId)}
              />
            </Tooltip>
          </Space>
        )
      },
    },
  ]

  return (
    <div style={{ paddingBottom: 32 }}>
      {/* Header Block */}
      <div
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
            <CloudServerOutlined style={{ marginRight: 10, color: '#0284c7' }} />
            Tra cứu & Giám sát Liên thông Đơn thuốc
          </Title>
          <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
            Theo dõi tập trung tình trạng gửi đơn thuốc lên Cổng liên thông Quốc gia (mô phỏng), xử lý dứt điểm các đơn lỗi và không để tồn đọng đơn chưa gửi.
          </Paragraph>
        </div>

        <Space wrap>
          {currentTabStatus === 'FAILED' && filteredData.length > 0 && canRetry && (
            <Popconfirm
              title="Gửi lại toàn bộ các đơn thuốc lỗi trên trang này?"
              description={`Tổng số ${filteredData.length} đơn lỗi sẽ được gửi lại tuần tự.`}
              okText="Bắt đầu gửi lại"
              cancelText="Hủy"
              onConfirm={handleBatchRetry}
            >
              <Button
                type="primary"
                danger
                icon={<SyncOutlined spin={batchRetrying} />}
                loading={batchRetrying}
              >
                {batchRetrying
                  ? `Đang gửi lại (${batchProgress.current}/${batchProgress.total})...`
                  : `Gửi lại tất cả đơn lỗi (${filteredData.length})`}
              </Button>
            </Popconfirm>
          )}

          <Button icon={<ReloadOutlined />} loading={loading} onClick={fetchData}>
            Làm mới
          </Button>
        </Space>
      </div>

      {loadError && (
        <Alert
          type="error"
          showIcon
          message="Không tải được dữ liệu liên thông"
          description={loadError}
          action={<Button size="small" onClick={fetchData}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      {/* KPI Statistic Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              borderRadius: 10,
              cursor: 'pointer',
              borderColor: currentTabStatus === 'FAILED' ? '#f87171' : undefined,
              backgroundColor: currentTabStatus === 'FAILED' ? '#fef2f2' : '#ffffff',
            }}
            onClick={() => {
              setCurrentTabStatus('FAILED')
              setPage(0)
            }}
          >
            <Statistic
              title={<span style={{ fontWeight: 600, color: '#991b1b' }}>Đơn liên thông lỗi (Cần xử lý)</span>}
              value={statusCounts.FAILED}
              valueStyle={{ color: '#dc2626', fontWeight: 800 }}
              prefix={<CloseCircleOutlined />}
              suffix={<Text type="secondary" style={{ fontSize: 12, marginLeft: 6 }}>→ Xem</Text>}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              borderRadius: 10,
              cursor: 'pointer',
              borderColor: currentTabStatus === 'NOT_SENT' ? '#93c5fd' : undefined,
              backgroundColor: currentTabStatus === 'NOT_SENT' ? '#f0f9ff' : '#ffffff',
            }}
            onClick={() => {
              setCurrentTabStatus('NOT_SENT')
              setPage(0)
            }}
          >
            <Statistic
              title={<span style={{ fontWeight: 600, color: '#0369a1' }}>Chưa gửi liên thông</span>}
              value={statusCounts.NOT_SENT}
              valueStyle={{ color: '#0284c7', fontWeight: 800 }}
              prefix={<ClockCircleOutlined />}
              suffix={<Text type="secondary" style={{ fontSize: 12, marginLeft: 6 }}>→ Xem</Text>}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              borderRadius: 10,
              cursor: 'pointer',
              borderColor: currentTabStatus === 'SUCCESS' ? '#86efac' : undefined,
              backgroundColor: currentTabStatus === 'SUCCESS' ? '#f0fdf4' : '#ffffff',
            }}
            onClick={() => {
              setCurrentTabStatus('SUCCESS')
              setPage(0)
            }}
          >
            <Statistic
              title={<span style={{ fontWeight: 600, color: '#166534' }}>Đã liên thông thành công</span>}
              value={statusCounts.SUCCESS}
              valueStyle={{ color: '#16a34a', fontWeight: 800 }}
              prefix={<CheckCircleOutlined />}
              suffix={<Text type="secondary" style={{ fontSize: 12, marginLeft: 6 }}>→ Xem</Text>}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ fontSize: 12.5, color: '#64748b' }}>Tỷ lệ liên thông thành công</div>
                <div style={{ fontSize: 24, fontWeight: 800, color: successRate >= 90 ? '#16a34a' : '#d97706' }}>
                  {successRate}%
                </div>
              </div>
              <Progress
                type="circle"
                percent={successRate}
                size={44}
                strokeColor={successRate >= 90 ? '#16a34a' : '#d97706'}
              />
            </div>
          </Card>
        </Col>
      </Row>

      {/* Tabs for Interconnection Status Filter */}
      <Card styles={{ body: { padding: '12px 16px' } }} style={{ marginBottom: 16, borderRadius: 10 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <Tabs
            activeKey={currentTabStatus}
            onChange={(key) => {
              setCurrentTabStatus(key)
              setPage(0)
            }}
            style={{ marginBottom: -16 }}
            items={[
              {
                key: 'FAILED',
                label: (
                  <Space size={6} align="center">
                    <CloseCircleOutlined style={{ color: '#dc2626' }} />
                    <span>Đơn liên thông lỗi (FAILED)</span>
                    <Tag
                      color="error"
                      style={{
                        margin: 0,
                        borderRadius: 10,
                        fontSize: 11,
                        padding: '0 7px',
                        fontWeight: 700,
                        lineHeight: '18px',
                      }}
                    >
                      {statusCounts.FAILED}
                    </Tag>
                  </Space>
                ),
              },
              {
                key: 'NOT_SENT',
                label: (
                  <Space size={6} align="center">
                    <ClockCircleOutlined style={{ color: '#0284c7' }} />
                    <span>Chưa gửi liên thông (NOT_SENT)</span>
                    <Tag
                      color="blue"
                      style={{
                        margin: 0,
                        borderRadius: 10,
                        fontSize: 11,
                        padding: '0 7px',
                        fontWeight: 700,
                        lineHeight: '18px',
                      }}
                    >
                      {statusCounts.NOT_SENT}
                    </Tag>
                  </Space>
                ),
              },
              {
                key: 'SUCCESS',
                label: (
                  <Space size={6} align="center">
                    <CheckCircleOutlined style={{ color: '#16a34a' }} />
                    <span>Đã liên thông thành công (SUCCESS)</span>
                    <Tag
                      color="success"
                      style={{
                        margin: 0,
                        borderRadius: 10,
                        fontSize: 11,
                        padding: '0 7px',
                        fontWeight: 700,
                        lineHeight: '18px',
                      }}
                    >
                      {statusCounts.SUCCESS}
                    </Tag>
                  </Space>
                ),
              },
            ]}
          />

          <Space wrap size={10}>
            <RangePicker
              format="DD/MM/YYYY"
              value={dateRange}
              onChange={(dates) => {
                setDateRange(dates)
                setPage(0)
              }}
              placeholder={['Từ ngày', 'Đến ngày']}
              presets={[
                { label: 'Hôm nay', value: [dayjs().startOf('day'), dayjs().endOf('day')] },
                { label: '7 ngày qua', value: [dayjs().subtract(7, 'day'), dayjs()] },
                { label: '30 ngày qua', value: [dayjs().subtract(30, 'day'), dayjs()] },
              ]}
              style={{ width: 240 }}
            />

            <Input
              placeholder="Tìm theo mã đơn, BN, bác sĩ, mã biên nhận..."
              prefix={<SearchOutlined style={{ color: '#9ca3af' }} />}
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              allowClear
              style={{ width: 280 }}
            />
          </Space>
        </div>
      </Card>

      {/* Main Table */}
      <Card style={{ borderRadius: 10 }}>
        <Table
          rowKey="prescriptionId"
          columns={columns}
          dataSource={filteredData}
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize,
            total: totalElements,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            showTotal: (total) => `Tổng số ${total} đơn thuốc theo bộ lọc`,
            onChange: (p, s) => {
              setPage(p - 1)
              setPageSize(s)
            },
          }}
          scroll={{ x: 1230 }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  currentTabStatus === 'FAILED'
                    ? 'Tuyệt vời! Không có đơn thuốc nào bị lỗi liên thông cần xử lý.'
                    : currentTabStatus === 'NOT_SENT'
                    ? 'Không có đơn thuốc nào đang ở trạng thái chưa liên thông.'
                    : 'Không tìm thấy bản ghi liên thông nào phù hợp tiêu chí lọc.'
                }
              />
            ),
          }}
        />
      </Card>

      {/* Detail Modal */}
      <PrescriptionDetailModal
        open={detailModalOpen}
        onClose={() => {
          setDetailModalOpen(false)
          setSelectedPrescriptionDetail(null)
        }}
        prescription={selectedPrescriptionDetail}
        canEdit={false}
        canSendInterconnection={true}
        onInterconnectionUpdated={() => {
          fetchData()
        }}
      />
    </div>
  )
}

export default PrescriptionInterconnectionPage
