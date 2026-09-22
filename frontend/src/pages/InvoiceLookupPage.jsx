import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Button,
  Card,
  Col,
  DatePicker,
  Dropdown,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  FileSearchOutlined,
  HistoryOutlined,
  MoreOutlined,
  PrinterOutlined,
  ReloadOutlined,
  SearchOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import billingApi from '../api/billingApi'
import invoiceApi from '../api/invoiceApi'
import patientApi from '../api/patientApi'
import queueApi from '../api/queueApi'
import systemApi from '../api/systemApi'
import visitApi from '../api/visitApi'
import InvoiceDetailModal from '../components/invoice/InvoiceDetailModal'
import { executePrintInvoice } from '../components/invoice/InvoicePrintTemplate'
import {
  buildInvoiceSearchParams,
  formatCurrency,
  formatDateTime,
  formatVisitCode,
  INVOICE_TYPE_META,
  INVOICE_TYPE_OPTIONS,
  resolvePatientInfo,
} from '../utils/invoiceLookupHelpers'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

export default function InvoiceLookupPage() {
  // Bộ lọc tìm kiếm
  const [filterPatientName, setFilterPatientName] = useState('')
  const [filterInvoiceCode, setFilterInvoiceCode] = useState('')
  const [filterInvoiceType, setFilterInvoiceType] = useState('')
  const [filterDateRange, setFilterDateRange] = useState(null)

  // Phân trang
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)

  // Danh sách dữ liệu & trạng thái tải
  const [invoices, setInvoices] = useState([])
  const [loading, setLoading] = useState(false)

  // Bộ đệm lưu thông tin bệnh nhân/lượt khám theo visitId
  const [encounterCache, setEncounterCache] = useState({})
  const [patientList, setPatientList] = useState([])
  const [payableList, setPayableList] = useState([])
  const [queueList, setQueueList] = useState([])
  const [patientsLoading, setPatientsLoading] = useState(false)

  // Cấu hình phòng khám phục vụ in ấn
  const [clinic, setClinic] = useState(null)

  // Modal chi tiết hóa đơn
  const [selectedInvoiceId, setSelectedInvoiceId] = useState(null)
  const [detailModalOpen, setDetailModalOpen] = useState(false)

  // Trạng thái đang in lại của từng dòng (theo invoiceId)
  const [reprintingIds, setReprintingIds] = useState({})

  // Tải cấu hình phòng khám, danh sách bệnh nhân và lượt khám thanh toán
  useEffect(() => {
    systemApi
      .clinic()
      .then((res) => {
        if (res?.data) setClinic(res.data)
      })
      .catch(() => {})

    setPatientsLoading(true)
    Promise.allSettled([
      patientApi.getAll({ page: 0, size: 200 }),
      billingApi.getPayable({ page: 0, size: 100 }),
      queueApi.getQueues({ date: dayjs().format('YYYY-MM-DD') }),
    ])
      .then(([patRes, payRes, queRes]) => {
        if (patRes.status === 'fulfilled') {
          const pData = patRes.value?.data
          setPatientList(Array.isArray(pData?.content) ? pData.content : Array.isArray(pData) ? pData : [])
        }
        if (payRes.status === 'fulfilled') {
          const payData = payRes.value?.data
          setPayableList(Array.isArray(payData?.content) ? payData.content : Array.isArray(payData) ? payData : [])
        }
        if (queRes.status === 'fulfilled') {
          const qData = queRes.value?.data
          setQueueList(Array.isArray(qData?.content) ? qData.content : Array.isArray(qData) ? qData : [])
        }
      })
      .finally(() => {
        setPatientsLoading(false)
      })
  }, [])

  // Tải danh sách hóa đơn từ Backend
  const fetchInvoices = useCallback(async () => {
    setLoading(true)
    try {
      const createdFrom = filterDateRange?.[0]
        ? filterDateRange[0].startOf('day').toISOString()
        : undefined
      const createdTo = filterDateRange?.[1]
        ? filterDateRange[1].endOf('day').toISOString()
        : undefined

      const params = buildInvoiceSearchParams({
        patientName: filterPatientName,
        invoiceCode: filterInvoiceCode,
        invoiceType: filterInvoiceType,
        createdFrom,
        createdTo,
        page,
        size: pageSize,
      })

      const res = await invoiceApi.search(params)
      const data = res.data

      const content = Array.isArray(data?.content)
        ? data.content
        : Array.isArray(data)
        ? data
        : []

      setInvoices(content)
      setTotalElements(data?.totalElements || content.length)

      // Thu thập các visitId duy nhất chưa có trong cache để tải thông tin bệnh nhân
      const uncachedVisitIds = Array.from(
        new Set(content.map((inv) => inv.visitId).filter(Boolean)),
      ).filter((id) => !encounterCache[id])

      if (uncachedVisitIds.length > 0) {
        Promise.allSettled(
          uncachedVisitIds.map((vId) =>
            visitApi.getEncounter(vId).then((r) => ({ visitId: vId, encounter: r.data })),
          ),
        ).then((results) => {
          const newEntries = {}
          results.forEach((r) => {
            if (r.status === 'fulfilled' && r.value?.encounter) {
              newEntries[r.value.visitId] = r.value.encounter
            }
          })
          if (Object.keys(newEntries).length > 0) {
            setEncounterCache((prev) => ({ ...prev, ...newEntries }))
          }
        })
      }
    } catch (err) {
      const errorMsg =
        err.response?.data?.message || err.message || 'Không thể tải danh sách hóa đơn'
      message.error(errorMsg)
      setInvoices([])
      setTotalElements(0)
    } finally {
      setLoading(false)
    }
  }, [
    filterPatientName,
    filterInvoiceCode,
    filterInvoiceType,
    filterDateRange,
    page,
    pageSize,
    encounterCache,
  ])

  // Tự động gọi API tra cứu khi thay đổi bộ lọc hoặc phân trang
  useEffect(() => {
    fetchInvoices()
  }, [fetchInvoices])

  // Debounce tìm kiếm theo tên bệnh nhân & mã hóa đơn (300ms)
  const debounceTimerRef = useRef(null)
  const handlePatientNameChange = (e) => {
    const val = e.target.value
    if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current)
    debounceTimerRef.current = setTimeout(() => {
      setFilterPatientName(val)
      setPage(0) // Luôn reset về trang 1
    }, 300)
  }

  const handleInvoiceCodeChange = (e) => {
    const val = e.target.value
    if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current)
    debounceTimerRef.current = setTimeout(() => {
      setFilterInvoiceCode(val)
      setPage(0) // Luôn reset về trang 1
    }, 300)
  }

  const handleTypeChange = (val) => {
    setFilterInvoiceType(val)
    setPage(0) // Luôn reset về trang 1
  }

  const handleDateRangeChange = (dates) => {
    setFilterDateRange(dates)
    setPage(0) // Luôn reset về trang 1
  }

  const handleResetFilters = () => {
    setFilterPatientName('')
    setFilterInvoiceCode('')
    setFilterInvoiceType('')
    setFilterDateRange(null)
    setPage(0)
  }

  // Cập nhật số lần in lại của một hóa đơn trên bảng sau khi in thành công
  const handleUpdateReprintCount = useCallback((updatedInvoice) => {
    if (!updatedInvoice?.id) return
    setInvoices((prev) =>
      prev.map((item) => (item.id === updatedInvoice.id ? { ...item, ...updatedInvoice } : item)),
    )
  }, [])

  // Xử lý in lại trực tiếp từ bảng
  const handleReprintFromRow = async (record) => {
    setReprintingIds((prev) => ({ ...prev, [record.id]: true }))
    try {
      const res = await invoiceApi.reprint(record.id)
      const updatedInvoice = res.data

      handleUpdateReprintCount(updatedInvoice)
      message.success(`Đã ghi nhận in lại thành công (Lần ${updatedInvoice.reprintCount})`)

      // Lấy thêm thông tin điều chỉnh nếu là hóa đơn điều chỉnh
      let adjustmentData = null
      if (updatedInvoice.type === 'ADJUSTMENT' || updatedInvoice.originalInvoiceId) {
        try {
          const adjRes = await invoiceApi.getAdjustments(
            updatedInvoice.originalInvoiceId || updatedInvoice.id,
          )
          adjustmentData = adjRes.data
        } catch {}
      }

      // Xác định thông tin bệnh nhân cho phiếu in
      const resolved = resolvePatientInfo({
        visitId: record.visitId,
        encounter: encounterCache[record.visitId],
        patientList,
        payableList,
        queueList,
      })

      const rowEncounter = {
        patient: {
          fullName: resolved?.fullName || '—',
          patientCode: resolved?.patientCode || '—',
          phone: resolved?.phone || '',
          dateOfBirth: resolved?.dateOfBirth,
          gender: resolved?.gender,
        },
        doctor: {
          fullName: resolved?.doctorName || 'Bác sĩ phụ trách',
        },
        visit: {
          visitCode: resolved?.visitCode || formatVisitCode(record.visitId),
          reason: resolved?.reason || 'Khám bệnh',
        },
      }

      // Kích hoạt in chứng từ
      executePrintInvoice({
        invoice: updatedInvoice,
        encounter: rowEncounter,
        clinic,
        adjustmentData,
      })
    } catch (err) {
      const errorMsg =
        err.response?.data?.message ||
        err.message ||
        'Không thể ghi nhận in lại vào hệ thống. Hủy thao tác in.'
      message.error(errorMsg)
      // TUYỆT ĐỐI KHÔNG mở cửa sổ in nếu backend thất bại
    } finally {
      setReprintingIds((prev) => ({ ...prev, [record.id]: false }))
    }
  }

  // Cấu hình các cột của Bảng danh sách
  const columns = useMemo(
    () => [
      {
        title: 'Mã hóa đơn',
        dataIndex: 'invoiceCode',
        key: 'invoiceCode',
        width: 160,
        render: (code, record) => (
          <Button
            type="link"
            style={{ padding: 0, fontWeight: 700, fontFamily: 'monospace' }}
            onClick={() => {
              setSelectedInvoiceId(record.id)
              setDetailModalOpen(true)
            }}
          >
            {code || record.id}
          </Button>
        ),
      },
      {
        title: 'Bệnh nhân',
        key: 'patient',
        render: (_, record) => {
          const patientInfo = resolvePatientInfo({
            visitId: record.visitId,
            encounter: encounterCache[record.visitId],
            patientList,
            payableList,
            queueList,
          })

          if (patientInfo?.fullName) {
            return (
              <div>
                <div style={{ fontWeight: 600 }}>{patientInfo.fullName}</div>
                <div style={{ fontSize: 11.5, color: '#64748b' }}>
                  Mã BN: {patientInfo.patientCode || '—'} {patientInfo.phone ? `• ${patientInfo.phone}` : ''}
                </div>
              </div>
            )
          }

          if (patientsLoading) {
            return <span style={{ color: '#94a3b8' }}>Đang tải...</span>
          }

          return (
            <div>
              <div style={{ fontWeight: 600, color: '#475569' }}>Lượt khám bệnh</div>
              <div style={{ fontSize: 11.5, color: '#94a3b8' }}>
                {formatVisitCode(record.visitId)}
              </div>
            </div>
          )
        },
      },
      {
        title: 'Loại hóa đơn',
        dataIndex: 'type',
        key: 'type',
        width: 160,
        render: (type) => {
          const meta = INVOICE_TYPE_META[type] || { label: type, color: 'default' }
          return <Tag color={meta.color}>{meta.label}</Tag>
        },
      },
      {
        title: 'Tổng tiền',
        dataIndex: 'totalAmount',
        key: 'totalAmount',
        width: 150,
        align: 'right',
        render: (amount) => (
          <span style={{ fontWeight: 700, color: '#0369a1', fontSize: 14 }}>
            {formatCurrency(amount)}
          </span>
        ),
      },
      {
        title: 'Ngày lập',
        dataIndex: 'createdAt',
        key: 'createdAt',
        width: 170,
        render: (date) => formatDateTime(date),
      },
      {
        title: 'Trạng thái in',
        key: 'reprintStatus',
        width: 160,
        align: 'center',
        render: (_, record) => {
          const count = Number(record.reprintCount || 0)
          if (count > 0) {
            return (
              <Tooltip title={`In lại gần nhất: ${formatDateTime(record.lastReprintedAt)}`}>
                <Tag color="volcano" icon={<HistoryOutlined />}>
                  Đã in lại x{count}
                </Tag>
              </Tooltip>
            )
          }
          return (
            <Tag color="green" icon={<CheckCircleOutlined />}>
              Bản gốc
            </Tag>
          )
        },
      },
      {
        title: 'Thao tác',
        key: 'actions',
        width: 80,
        align: 'center',
        render: (_, record) => {
          const actionItems = [
            {
              key: 'detail',
              icon: <EyeOutlined style={{ color: '#0284c7' }} />,
              label: 'Xem chi tiết',
              onClick: () => {
                setSelectedInvoiceId(record.id)
                setDetailModalOpen(true)
              },
            },
            {
              type: 'divider',
            },
            {
              key: 'reprint',
              icon: <PrinterOutlined style={{ color: '#2563eb' }} />,
              label: 'In lại hóa đơn',
              onClick: () => {
                Modal.confirm({
                  title: 'Xác nhận in lại hóa đơn này?',
                  icon: <ExclamationCircleOutlined style={{ color: '#0284c7' }} />,
                  content: (
                    <div>
                      Thao tác này sẽ ghi nhận lượt in lại vào nhật ký kiểm toán hệ thống và đánh dấu{' '}
                      <strong>"BẢN IN LẠI"</strong> trên chứng từ.
                    </div>
                  ),
                  okText: 'Xác nhận & In',
                  cancelText: 'Hủy',
                  okButtonProps: { type: 'primary' },
                  onOk: () => handleReprintFromRow(record),
                })
              },
            },
          ]

          return (
            <Dropdown menu={{ items: actionItems }} trigger={['click']} placement="bottomRight">
              <Button
                type="text"
                size="small"
                icon={<MoreOutlined style={{ fontSize: 18, color: '#475569' }} />}
                style={{ width: 32, height: 32, borderRadius: 6 }}
                loading={reprintingIds[record.id]}
                title="Thao tác"
              />
            </Dropdown>
          )
        },
      },
    ],
    [encounterCache, reprintingIds, patientList, payableList, queueList, patientsLoading],
  )

  const selectedEncounter = useMemo(() => {
    const selected = invoices.find((inv) => inv.id === selectedInvoiceId)
    if (!selected) return null
    const enc = encounterCache[selected.visitId]
    const resolved = resolvePatientInfo({
      visitId: selected.visitId,
      encounter: enc,
      patientList,
      payableList,
      queueList,
    })
    return {
      patient: {
        fullName: resolved?.fullName || enc?.patient?.fullName || '—',
        patientCode: resolved?.patientCode || enc?.patient?.patientCode || '—',
        phone: resolved?.phone || enc?.patient?.phone || '',
        dateOfBirth: resolved?.dateOfBirth || enc?.patient?.dateOfBirth,
        gender: resolved?.gender || enc?.patient?.gender,
      },
      doctor: {
        fullName: resolved?.doctorName || enc?.doctor?.fullName || 'Bác sĩ phụ trách',
      },
      visit: {
        visitCode: resolved?.visitCode || enc?.visit?.visitCode || formatVisitCode(selected.visitId),
        reason: resolved?.reason || enc?.visit?.reason || 'Khám bệnh',
      },
    }
  }, [invoices, selectedInvoiceId, encounterCache, patientList, payableList, queueList])

  return (
    <div style={{ padding: 24 }}>
      <div style={{ marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0, color: '#0f172a' }}>
          <FileSearchOutlined style={{ marginRight: 8, color: '#0284c7' }} />
          Tra cứu và in lại hóa đơn
        </Title>
      </div>

      {/* Thanh bộ lọc tìm kiếm */}
      <Card size="small" style={{ marginBottom: 16, borderRadius: 8 }}>
        <Row gutter={[12, 12]} align="middle">
          <Col xs={24} sm={12} md={6}>
            <Input
              size="middle"
              prefix={<UserOutlined style={{ color: '#94a3b8' }} />}
              placeholder="Tên bệnh nhân..."
              allowClear
              defaultValue={filterPatientName}
              onChange={handlePatientNameChange}
              style={{ height: 38, borderRadius: 6 }}
            />
          </Col>
          <Col xs={24} sm={12} md={5}>
            <Input
              size="middle"
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              placeholder="Mã hóa đơn (HD-...)"
              allowClear
              defaultValue={filterInvoiceCode}
              onChange={handleInvoiceCodeChange}
              style={{ height: 38, borderRadius: 6 }}
            />
          </Col>
          <Col xs={24} sm={12} md={5}>
            <Select
              size="middle"
              style={{ width: '100%', height: 38 }}
              placeholder="Loại hóa đơn"
              value={filterInvoiceType}
              onChange={handleTypeChange}
              options={INVOICE_TYPE_OPTIONS}
            />
          </Col>
          <Col xs={24} sm={12} md={6}>
            <RangePicker
              size="middle"
              style={{ width: '100%', height: 38, borderRadius: 6 }}
              value={filterDateRange}
              onChange={handleDateRangeChange}
              format="DD/MM/YYYY"
              placeholder={['Từ ngày', 'Đến ngày']}
            />
          </Col>
          <Col xs={24} sm={12} md={2}>
            <Button
              size="middle"
              icon={<ReloadOutlined />}
              onClick={handleResetFilters}
              style={{ width: '100%', height: 38, borderRadius: 6 }}
            >
              Đặt lại
            </Button>
          </Col>
        </Row>
      </Card>

      {/* Bảng danh sách hóa đơn */}
      <Card bodyStyle={{ padding: 0 }} style={{ borderRadius: 8, overflow: 'hidden' }}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={invoices}
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize,
            total: totalElements,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            showTotal: (total) => (
              <span style={{ color: '#475569', fontSize: 13, marginRight: 8 }}>
                Tổng cộng <strong style={{ color: '#0f172a' }}>{total}</strong> hóa đơn
              </span>
            ),
            onChange: (p, s) => {
              setPage(p - 1)
              setPageSize(s)
            },
          }}
          locale={{ emptyText: 'Không tìm thấy hóa đơn nào phù hợp với bộ lọc' }}
        />
      </Card>

      {/* Modal xem chi tiết và in lại */}
      <InvoiceDetailModal
        open={detailModalOpen}
        onClose={() => setDetailModalOpen(false)}
        invoiceId={selectedInvoiceId}
        encounter={selectedEncounter}
        onReprintSuccess={handleUpdateReprintCount}
      />
    </div>
  )
}
