import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
  message,
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
} from 'antd'
import {
  AlertOutlined,
  AreaChartOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  DollarCircleOutlined,
  DownloadOutlined,
  EyeOutlined,
  FallOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  FireOutlined,
  InfoCircleOutlined,
  LineChartOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
  NodeIndexOutlined,
  PieChartOutlined,
  PlusOutlined,
  PrinterOutlined,
  QrcodeOutlined,
  ReloadOutlined,
  RiseOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  ShoppingOutlined,
  TeamOutlined,
  UserOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import reportApi from '../api/reportApi'
import billingApi from '../api/billingApi'
import { useAuthContext } from '../context/AuthContext'
import RevenueReportView from '../components/reporting/RevenueReportView'
import {
  getStoredAuditLogs,
  getStoredInvoices,
  getStoredMedicalRecords,
  getStoredMedicines,
  getStoredPrescriptions,
  mergeInvoices,
} from '../utils/storageHelpers'

const { RangePicker } = DatePicker
const { Title, Text, Paragraph } = Typography

const money = (v) => `${Number(v || 0).toLocaleString('vi-VN')} đ`

const formatCurrency = (val, currency = 'VND') => {
  const num = Number(val || 0)
  return `${num.toLocaleString('vi-VN')} ${currency === 'VND' ? '₫' : currency}`
}

const WEEKDAY_NAMES = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']

function ReportsPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuthContext()

  // Phân quyền
  const roles = useMemo(() => {
    const values = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
    return values
      .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
      .filter(Boolean)
  }, [user])
  const isManager = roles.includes('manager') || roles.includes('admin')

  // Mặc định khoảng thời gian: 30 ngày gần nhất
  const [range, setRange] = useState([dayjs().subtract(29, 'day'), dayjs()])
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [activeTab, setActiveTab] = useState('visits')
  const [chartType, setChartType] = useState('line') // 'line' | 'bar'
  const [hoveredDay, setHoveredDay] = useState(null)
  const [loadError, setLoadError] = useState('')

  // Dynamic Data States
  const [summary, setSummary] = useState({
    visitCount: 0,
    revenue: 0,
    dispensedCount: 0,
    auditCount: 0,
    currency: 'VND',
  })
  const [timeline, setTimeline] = useState([])
  const [topMedicines, setTopMedicines] = useState([])
  const [auditLogs, setAuditLogs] = useState([])
  const [invoicesList, setInvoicesList] = useState([])
  const [logSearch, setLogSearch] = useState('')

  const fromStr = useMemo(
    () => (range?.[0] ? range[0].format('YYYY-MM-DD') : dayjs().subtract(29, 'day').format('YYYY-MM-DD')),
    [range],
  )
  const toStr = useMemo(
    () => (range?.[1] ? range[1].format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD')),
    [range],
  )

  const getParams = useCallback(() => ({
    from: fromStr,
    to: toStr,
  }), [fromStr, toStr])

  // Gọi API và hợp nhất dữ liệu thực tế
  const loadData = useCallback(async () => {
    if (!fromStr || !toStr) return

    setLoading(true)
    setLoadError('')
    const params = getParams()

    try {
      const [summaryRes, timelineRes, medicinesRes, auditRes, invoicesRes] = await Promise.allSettled([
        reportApi.summary(params),
        reportApi.timeline(params),
        reportApi.topMedicines(params),
        reportApi.audit(params),
        billingApi.getAll(),
      ])

      const apiSummary = summaryRes.status === 'fulfilled' ? summaryRes.value.data : null
      const apiTimeline = timelineRes.status === 'fulfilled' ? timelineRes.value.data : null
      const apiTopMedicines = medicinesRes.status === 'fulfilled' ? medicinesRes.value.data : null
      const apiAudit = auditRes.status === 'fulfilled' ? auditRes.value.data : null
      const apiInvoicesRaw = invoicesRes.status === 'fulfilled' ? invoicesRes.value.data : null
      const apiInvoicesList = (apiInvoicesRaw && Array.isArray(apiInvoicesRaw.content))
        ? apiInvoicesRaw.content
        : (Array.isArray(apiInvoicesRaw) ? apiInvoicesRaw : [])

      // Get stored items from local persistence
      const storedRecords = getStoredMedicalRecords()
      const storedInvoices = getStoredInvoices()
      const storedPrescriptions = getStoredPrescriptions()
      const storedMedicines = getStoredMedicines()
      const storedLogs = getStoredAuditLogs()

      const apiRecords = (apiSummary && Array.isArray(apiSummary.records)) ? apiSummary.records : []
      const apiInvs = (apiSummary && Array.isArray(apiSummary.invoices)) ? apiSummary.invoices : apiInvoicesList
      const apiPrescs = (apiSummary && Array.isArray(apiSummary.prescriptions)) ? apiSummary.prescriptions : []

      // Merge items
      const realRecords = [...storedRecords, ...apiRecords]
      const realInvoices = mergeInvoices([...storedInvoices, ...apiInvs])
      const realPrescriptions = [...storedPrescriptions, ...apiPrescs]
      const realLogs = (apiAudit && Array.isArray(apiAudit)) ? [...storedLogs, ...apiAudit] : storedLogs

      setInvoicesList(realInvoices)

      // Calculate total visits and revenue
      const totalVisits = apiSummary?.visitCount != null ? Number(apiSummary.visitCount) : realRecords.length
      const totalRevenue = apiSummary?.revenue != null ? Number(apiSummary.revenue) : realInvoices.reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0)
      const dispensedCount = realPrescriptions.filter((p) => p.status === 'DISPENSED' || p.status === 'COMPLETED').length
      const auditCount = realLogs.length

      // Timeline mapping
      let timelineList = []
      if (apiTimeline && Array.isArray(apiTimeline.items) && apiTimeline.items.length > 0) {
        timelineList = apiTimeline.items.map((item) => ({
          date: item.date,
          reportDate: dayjs(item.date).format('DD/MM/YYYY'),
          visitCount: Number(item.visitCount || 0),
          revenue: Number(item.revenue || 0),
        }))
      } else {
        const startDate = range[0] || dayjs().subtract(29, 'day')
        const endDate = range[1] || dayjs()
        const diffDays = Math.max(0, endDate.diff(startDate, 'day'))
        const datesCount = Math.min(diffDays + 1, 366)

        for (let i = 0; i < datesCount; i++) {
          const currentDate = startDate.add(i, 'day')
          const formattedDate = currentDate.format('DD/MM/YYYY')
          const dateISO = currentDate.format('YYYY-MM-DD')

          const dayVisits = realRecords.filter((r) => (
            r.createdAt && dayjs(r.createdAt).format('YYYY-MM-DD') === dateISO
          )).length

          const dayRevenue = realInvoices
            .filter((inv) => inv.createdAt && dayjs(inv.createdAt).format('YYYY-MM-DD') === dateISO)
            .reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0)

          timelineList.push({
            date: dateISO,
            reportDate: formattedDate,
            visitCount: dayVisits,
            revenue: dayRevenue,
          })
        }
      }

      // Top medicines ranking
      let rankedMeds = []
      if (apiTopMedicines && Array.isArray(apiTopMedicines.items)) {
        rankedMeds = apiTopMedicines.items.map((m) => ({
          name: m.medicineName || m.name,
          category: 'Dược phẩm',
          dispensedQuantity: m.totalDispensedQuantity || m.dispensedQuantity || 0,
        }))
      } else {
        const medMap = new Map()
        realPrescriptions.forEach((p) => {
          let items = []
          try {
            items = typeof p.items === 'string' ? JSON.parse(p.items) : (p.items || [])
          } catch {
            items = []
          }
          items.forEach((item) => {
            const medName = storedMedicines.find((m) => m.id === item.medicineId)?.name || item.medicineId || 'Thuốc'
            const currentQty = medMap.get(medName) || 0
            medMap.set(medName, currentQty + Number(item.quantity || 1))
          })
        })

        rankedMeds = Array.from(medMap.entries())
          .map(([name, dispensedQuantity]) => ({ name, category: 'Dược phẩm', dispensedQuantity }))
          .sort((a, b) => b.dispensedQuantity - a.dispensedQuantity)
      }

      setSummary({
        visitCount: totalVisits,
        revenue: totalRevenue,
        dispensedCount,
        auditCount,
        currency: apiSummary?.currency || 'VND',
      })

      setTimeline(timelineList)
      setTopMedicines(rankedMeds)
      setAuditLogs(realLogs)
    } catch (err) {
      console.error('Lỗi tải báo cáo vận hành:', err)
      const status = err?.response?.status
      if (status === 403) {
        setLoadError('PERMISSION_DENIED_NOT_MANAGER')
      } else {
        setLoadError(err?.response?.data?.message || err?.message || 'Không thể tải báo cáo.')
      }
    } finally {
      setLoading(false)
    }
  }, [fromStr, toStr, getParams, range])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Phân tích các chỉ số vận hành và tải khám (từ timeline)
  const analytics = useMemo(() => {
    const totalVisits = summary.visitCount || 0
    const totalDays = Math.max(timeline.length, 1)
    const avgPerDay = totalVisits / totalDays
    const formattedAvg = avgPerDay.toFixed(1).replace('.', ',')

    let maxVisitItem = null
    let minVisitItem = null

    timeline.forEach((item) => {
      const count = Number(item.visitCount || 0)
      if (!maxVisitItem || count > Number(maxVisitItem.visitCount || 0)) {
        maxVisitItem = item
      }
      if (!minVisitItem || count < Number(minVisitItem.visitCount || 0)) {
        minVisitItem = item
      }
    })

    // Đánh giá mức độ tải khám và gợi ý nhân sự
    let workloadStatus = 'STABLE'
    let workloadTitle = 'Tải khám ổn định'
    let workloadColor = '#16a34a'
    let staffingRecommendation = 'Bố trí 2 bác sĩ trực, 1 quầy tiếp nhận.'

    if (avgPerDay >= 25) {
      workloadStatus = 'OVERLOAD'
      workloadTitle = 'Tải khám rất cao (Cao điểm)'
      workloadColor = '#dc2626'
      staffingRecommendation = 'Cần tăng cường tối thiểu 4-5 bác sĩ trực, 2 quầy tiếp đón và 2 dược sĩ cấp phát.'
    } else if (avgPerDay >= 15) {
      workloadStatus = 'HIGH'
      workloadTitle = 'Tải khám cao'
      workloadColor = '#ea580c'
      staffingRecommendation = 'Cần bố trí 3 bác sĩ trực và tăng cường điều dưỡng hỗ trợ phân luồng.'
    } else if (avgPerDay < 5 && totalVisits > 0) {
      workloadStatus = 'LOW'
      workloadTitle = 'Tải khám thấp'
      workloadColor = '#0284c7'
      staffingRecommendation = 'Duy trì 1-2 bác sĩ trực, tối ưu chi phí ca trực.'
    }

    return {
      totalVisits,
      totalDays,
      avgPerDay,
      formattedAvg,
      maxVisitItem: maxVisitItem || { date: toStr, reportDate: dayjs(toStr).format('DD/MM/YYYY'), visitCount: 0 },
      minVisitItem: minVisitItem || { date: fromStr, reportDate: dayjs(fromStr).format('DD/MM/YYYY'), visitCount: 0 },
      workloadStatus,
      workloadTitle,
      workloadColor,
      staffingRecommendation,
    }
  }, [summary, timeline, fromStr, toStr])

  // Xuất file CSV báo cáo
  const handleExportCSV = async () => {
    setExporting(true)
    try {
      const response = await reportApi.export({ from: fromStr, to: toStr })
      const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `Bao-cao-van-hanh-${fromStr}-${toStr}.csv`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      message.success('Đã xuất báo cáo CSV thành công!')
    } catch {
      // Fallback export nếu API export chưa kết nối
      try {
        const csvRows = [
          ['BAO CAO VAN HANH VA NHAT KY TRUY CAP (DU LIEU GHI NHAN)'],
          [`Khoang thoi gian: ${fromStr} - ${toStr}`],
          [''],
          ['1. TONG QUAN CHI SO'],
          [`Tong luot kham: ${summary.visitCount}`],
          [`Doanh thu phong kham: ${summary.revenue} VND`],
          [`Don thuoc da cap: ${summary.dispensedCount}`],
          [`Nhat ky truy cap: ${summary.auditCount}`],
          [''],
          ['2. XU HUONG LUOT KHAM THEO NGAY'],
          ['Ngay bao cao', 'So luot kham', 'Doanh thu phat sinh (VND)'],
          ...timeline.map((t) => [t.reportDate, t.visitCount, t.revenue]),
          [''],
          ['3. TOP THUOC DUNG NHIEU'],
          ['Ten thuoc', 'So luong cap phat'],
          ...topMedicines.map((m) => [m.name, m.dispensedQuantity]),
        ]

        const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + csvRows.map((e) => e.join(',')).join('\n')
        const encodedUri = encodeURI(csvContent)
        const link = document.createElement('a')
        link.setAttribute('href', encodedUri)
        link.setAttribute('download', `bao-cao-van-hanh-${fromStr}-${toStr}.csv`)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        message.success('Đã xuất file báo cáo CSV thành công!')
      } catch {
        message.error('Không thể xuất báo cáo')
      }
    } finally {
      setExporting(false)
    }
  }

  // Chuẩn bị dữ liệu vẽ biểu đồ SVG lượt khám
  const maxVisitsInChart = useMemo(() => {
    const maxVal = Math.max(...timeline.map((d) => Number(d.visitCount || 0)), 10)
    return Math.ceil(maxVal / 5) * 5
  }, [timeline])

  const chartPoints = useMemo(() => {
    if (!timeline.length) return []
    const count = timeline.length
    const width = 760
    const height = 220
    const paddingLeft = 45
    const paddingRight = 35
    const paddingTop = 25
    const paddingBottom = 40
    const chartW = width - paddingLeft - paddingRight
    const chartH = height - paddingTop - paddingBottom

    return timeline.map((item, index) => {
      const x = count === 1 ? paddingLeft + chartW / 2 : paddingLeft + (index / (count - 1)) * chartW
      const val = Number(item.visitCount || 0)
      const y = paddingTop + chartH - (val / maxVisitsInChart) * chartH
      const itemDate = item.date || item.reportDate
      return {
        ...item,
        x,
        y,
        val,
        formattedDate: dayjs(itemDate).isValid() ? dayjs(itemDate).format('DD/MM') : item.reportDate,
        fullDate: dayjs(itemDate).isValid() ? dayjs(itemDate).format('DD/MM/YYYY') : item.reportDate,
        weekday: dayjs(itemDate).isValid() ? WEEKDAY_NAMES[dayjs(itemDate).day()] : '',
      }
    })
  }, [timeline, maxVisitsInChart])

  // Lọc nhật ký truy cập
  const filteredLogs = useMemo(() => {
    return auditLogs.filter((log) => {
      if (!logSearch.trim()) return true
      const term = logSearch.toLowerCase()
      return (
        (log.userName && log.userName.toLowerCase().includes(term)) ||
        (log.patientName && log.patientName.toLowerCase().includes(term)) ||
        (log.recordCode && log.recordCode.toLowerCase().includes(term)) ||
        (log.action && log.action.toLowerCase().includes(term))
      )
    })
  }, [auditLogs, logSearch])

  // Cột bảng chi tiết lượt khám từng ngày
  const visitTableColumns = [
    {
      title: 'Ngày khám',
      dataIndex: 'reportDate',
      key: 'reportDate',
      width: 180,
      render: (val, record) => {
        const d = dayjs(record.date || val)
        const weekday = d.isValid() ? WEEKDAY_NAMES[d.day()] : ''
        const isWeekend = d.isValid() && (d.day() === 0 || d.day() === 6)
        return (
          <Space>
            <strong style={{ color: isWeekend ? '#e11d48' : '#1e293b' }}>
              {val}
            </strong>
            {weekday && (
              <Tag color={isWeekend ? 'volcano' : 'default'} style={{ fontSize: 11 }}>
                {weekday}
              </Tag>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Số lượt khám',
      dataIndex: 'visitCount',
      key: 'visitCount',
      width: 220,
      sorter: (a, b) => Number(a.visitCount || 0) - Number(b.visitCount || 0),
      render: (val) => {
        const count = Number(val || 0)
        const percent = maxVisitsInChart > 0 ? (count / maxVisitsInChart) * 100 : 0
        return (
          <div style={{ width: 180 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
              <strong style={{ fontSize: 14, color: count > 0 ? '#1677ff' : '#94a3b8' }}>
                {count} lượt
              </strong>
              <Text type="secondary" style={{ fontSize: 11 }}>
                {summary.visitCount > 0
                  ? `${((count / summary.visitCount) * 100).toFixed(1)}%`
                  : '0%'}
              </Text>
            </div>
            <Progress
              percent={percent}
              showInfo={false}
              size="small"
              strokeColor={count >= 20 ? '#ef4444' : count >= 10 ? '#f59e0b' : '#3b82f6'}
            />
          </div>
        )
      },
    },
    {
      title: 'Mức độ tải khám',
      key: 'intensity',
      width: 180,
      render: (_, record) => {
        const count = Number(record.visitCount || 0)
        if (count >= 20) {
          return <Tag color="red" icon={<FireOutlined />}>Cao điểm ({count} lượt)</Tag>
        }
        if (count >= 10) {
          return <Tag color="orange" icon={<RiseOutlined />}>Khá đông ({count} lượt)</Tag>
        }
        if (count > 0) {
          return <Tag color="green" icon={<CheckCircleOutlined />}>Ổn định ({count} lượt)</Tag>
        }
        return <Tag color="default">Không có lượt khám</Tag>
      },
    },
    {
      title: 'Doanh thu phát sinh',
      dataIndex: 'revenue',
      key: 'revenue',
      width: 200,
      align: 'right',
      sorter: (a, b) => Number(a.revenue || 0) - Number(b.revenue || 0),
      render: (val) => (
        <span style={{ fontWeight: 600, color: Number(val || 0) > 0 ? '#16a34a' : '#94a3b8' }}>
          {formatCurrency(val, summary.currency)}
        </span>
      ),
    },
  ]

  return (
    <div style={{ background: '#f8fafc', minHeight: '100vh', padding: '16px 20px', paddingBottom: 40 }}>
      {/* Header Section */}
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
          <Title level={2} style={{ margin: 0, color: '#0f172a' }}>
            <LineChartOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Báo cáo Vận hành & Lượt khám
          </Title>
          <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
            Theo dõi số liệu khám bệnh, doanh thu tài chính, thuốc và nhật ký truy cập y tế theo thời gian thực.
          </Paragraph>
        </div>

        <Space wrap size="middle">
          <RangePicker
            value={range}
            format="DD/MM/YYYY"
            onChange={(val) => val && setRange(val)}
            allowClear={false}
            presets={[
              { label: '7 ngày qua', value: [dayjs().subtract(6, 'day'), dayjs()] },
              { label: '30 ngày qua', value: [dayjs().subtract(29, 'day'), dayjs()] },
              { label: 'Tháng này', value: [dayjs().startOf('month'), dayjs().endOf('month')] },
              {
                label: 'Tháng trước',
                value: [
                  dayjs().subtract(1, 'month').startOf('month'),
                  dayjs().subtract(1, 'month').endOf('month'),
                ],
              },
              { label: 'Quý này', value: [dayjs().startOf('quarter'), dayjs().endOf('quarter')] },
            ]}
            style={{ borderRadius: 8, padding: '6px 12px' }}
          />

          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadData}>
            Làm mới
          </Button>

          <Button
            type="primary"
            icon={<DownloadOutlined />}
            loading={exporting}
            onClick={handleExportCSV}
            style={{ borderRadius: 8, background: '#1677ff', fontWeight: 600 }}
          >
            Xuất báo cáo (CSV)
          </Button>
        </Space>
      </div>

      {loadError === 'PERMISSION_DENIED_NOT_MANAGER' ? (
        <Card
          style={{
            borderRadius: 12,
            marginBottom: 24,
            border: '1px solid #fed7aa',
            background: '#fffbeb',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16 }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: '50%',
                background: '#fef3c7',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#d97706',
                fontSize: 24,
                flexShrink: 0,
              }}
            >
              <TeamOutlined />
            </div>
            <div style={{ flex: 1 }}>
              <Title level={4} style={{ margin: '0 0 6px 0', color: '#92400e' }}>
                Phân quyền Báo cáo vận hành: Dành riêng cho Quản lý phòng khám (Manager)
              </Title>
              <Paragraph style={{ margin: '0 0 12px 0', color: '#78350f', fontSize: 14 }}>
                Theo chuẩn bảo mật và nghiệp vụ y tế, chức năng tổng hợp báo cáo lượt khám, phân tích tải khám và doanh thu được phân quyền riêng cho tài khoản <strong>Quản lý phòng khám (Clinic Manager)</strong>.
                <br />
                Tài khoản đang đăng nhập hiện tại là <strong>{user?.fullName || user?.username} ({roles.join(', ')})</strong>.
              </Paragraph>
              <Space wrap>
                <Button
                  type="primary"
                  icon={<LogoutOutlined />}
                  style={{ background: '#d97706', borderColor: '#d97706' }}
                  onClick={() => {
                    logout()
                    navigate('/login')
                  }}
                >
                  Đăng xuất để đăng nhập tài khoản Quản lý (`manager1`)
                </Button>
              </Space>
            </div>
          </div>
        </Card>
      ) : loadError ? (
        <Alert
          type="error"
          showIcon
          message="Không tải được dữ liệu báo cáo"
          description={loadError}
          action={<Button size="small" onClick={loadData}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      ) : null}

      {/* Top 4 Summary Cards */}
      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        {/* Card 1: Tổng lượt khám */}
        <Col xs={12} sm={12} md={6}>
          <div className="reports-stat-card">
            <div className="reports-stat-icon blue">
              <CalendarOutlined />
            </div>
            <div>
              <div className="reports-stat-label">Tổng lượt khám</div>
              <div className="reports-stat-value blue">
                {summary.visitCount} lượt
              </div>
              <div className="reports-stat-sub">Ghi nhận từ bệnh án</div>
            </div>
          </div>
        </Col>

        {/* Card 2: Doanh thu phòng khám */}
        <Col xs={12} sm={12} md={6}>
          <div className="reports-stat-card">
            <div className="reports-stat-icon green">
              <DollarCircleOutlined />
            </div>
            <div>
              <div className="reports-stat-label">Doanh thu phòng khám</div>
              <div className="reports-stat-value green">
                {money(summary.revenue)}
              </div>
              <div className="reports-stat-sub">Ghi nhận từ hóa đơn</div>
            </div>
          </div>
        </Col>

        {/* Card 3: Đơn thuốc đã cấp */}
        <Col xs={12} sm={12} md={6}>
          <div className="reports-stat-card">
            <div className="reports-stat-icon orange">
              <MedicineBoxOutlined />
            </div>
            <div>
              <div className="reports-stat-label">Đơn thuốc đã cấp</div>
              <div className="reports-stat-value orange">
                {summary.dispensedCount} đơn
              </div>
              <div className="reports-stat-sub">Ghi nhận từ kho thuốc</div>
            </div>
          </div>
        </Col>

        {/* Card 4: Nhật ký truy cập */}
        <Col xs={12} sm={12} md={6}>
          <div className="reports-stat-card">
            <div className="reports-stat-icon purple">
              <FileSearchOutlined />
            </div>
            <div>
              <div className="reports-stat-label">Nhật ký truy cập</div>
              <div className="reports-stat-value purple">
                {summary.auditCount} lượt
              </div>
              <div className="reports-stat-sub">Ghi nhận truy cập y tế</div>
            </div>
          </div>
        </Col>
      </Row>

      {/* Tabs Header Navigation */}
      <Card
        style={{ borderRadius: 14, boxShadow: '0 2px 10px rgba(0,0,0,0.02)', border: '1px solid #f1f5f9', marginBottom: 20 }}
        bodyStyle={{ padding: 0 }}
      >
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          style={{ paddingLeft: 16, paddingRight: 16 }}
          items={[
            {
              key: 'overview',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <DashboardOutlined /> Tổng quan vận hành
                </span>
              ),
            },
            {
              key: 'visits',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <LineChartOutlined /> Báo cáo lượt khám
                </span>
              ),
            },
            {
              key: 'revenue',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <DollarCircleOutlined /> Báo cáo doanh thu
                </span>
              ),
            },
            {
              key: 'medicines',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <MedicineBoxOutlined /> Báo cáo thuốc dùng nhiều
                </span>
              ),
            },
            {
              key: 'audit',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <FileSearchOutlined /> Nhật ký truy cập bệnh án
                </span>
              ),
            },
          ]}
        />
      </Card>

      <div>
        {/* TAB 1: OVERVIEW */}
        {activeTab === 'overview' && (
          <Card style={{ borderRadius: 14, border: '1px solid #f1f5f9' }}>
            <Title level={4}>Tổng quan chỉ số vận hành ghi nhận thực tế</Title>
            <Alert
              type="success"
              showIcon
              message="Dữ liệu vận hành thực tế"
              description="Toàn bộ chỉ số được tổng hợp trực tiếp từ các lượt khám, đơn thuốc đã cấp, hóa đơn đã thu và nhật ký truy cập."
              style={{ marginBottom: 20 }}
            />
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <Card title="Doanh thu thực tế đã thu" size="small">
                  <Title level={3} style={{ color: '#16a34a' }}>{money(summary.revenue)}</Title>
                </Card>
              </Col>
              <Col span={12}>
                <Card title="Số lượt khám đã ghi nhận" size="small">
                  <Title level={3} style={{ color: '#2563eb' }}>{summary.visitCount} lượt</Title>
                </Card>
              </Col>
            </Row>
          </Card>
        )}

        {/* TAB 2: VISITS REPORT (WITH WORKLOAD EVALUATION & INTERACTIVE CHARTS) */}
        {activeTab === 'visits' && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            {/* Khung Gợi ý Điều phối Nhân sự Dựa trên Tải khám */}
            <Alert
              type={analytics.workloadStatus === 'OVERLOAD' ? 'error' : analytics.workloadStatus === 'HIGH' ? 'warning' : 'info'}
              showIcon
              icon={<TeamOutlined style={{ fontSize: 20 }} />}
              message={
                <Space>
                  <strong>Đánh giá tải khám: {analytics.workloadTitle}</strong>
                  <Tag color={analytics.workloadStatus === 'OVERLOAD' ? 'red' : 'blue'}>
                    Trung bình {analytics.formattedAvg} lượt/ngày
                  </Tag>
                </Space>
              }
              description={
                <div>
                  <span>Khuyến nghị bố trí nhân lực: <strong>{analytics.staffingRecommendation}</strong></span>
                </div>
              }
              style={{ borderRadius: 10 }}
            />

            {/* Biểu đồ Trực quan Lượt khám theo thời gian */}
            <Card
              style={{ borderRadius: 14, border: '1px solid #f1f5f9', boxShadow: '0 2px 10px rgba(0,0,0,0.02)' }}
              title={
                <Space>
                  <LineChartOutlined style={{ color: '#1677ff' }} />
                  <span>Biểu đồ Xu hướng & Khối lượng Lượt khám theo ngày</span>
                </Space>
              }
              extra={
                <Radio.Group
                  value={chartType}
                  onChange={(e) => setChartType(e.target.value)}
                  optionType="button"
                  buttonStyle="solid"
                  size="small"
                >
                  <Radio.Button value="line">
                    <AreaChartOutlined /> Biểu đồ đường
                  </Radio.Button>
                  <Radio.Button value="bar">
                    <BarChartOutlined /> Biểu đồ cột
                  </Radio.Button>
                </Radio.Group>
              }
            >
              <Spin spinning={loading}>
                {timeline.length === 0 ? (
                  <Empty description="Không có dữ liệu lượt khám trong khoảng thời gian đã chọn" />
                ) : (
                  <div>
                    {/* Vùng vẽ biểu đồ SVG Interactive */}
                    <div style={{ position: 'relative', width: '100%', overflowX: 'auto' }}>
                      <svg
                        viewBox="0 0 760 220"
                        style={{ width: '100%', minWidth: 600, maxHeight: 260, display: 'block' }}
                      >
                        <defs>
                          <linearGradient id="visitAreaGrad" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.35" />
                            <stop offset="100%" stopColor="#3b82f6" stopOpacity="0.0" />
                          </linearGradient>
                          <linearGradient id="visitBarGrad" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stopColor="#2563eb" />
                            <stop offset="100%" stopColor="#60a5fa" />
                          </linearGradient>
                        </defs>

                        {/* Lưới ngang tham chiếu */}
                        {[0, 0.25, 0.5, 0.75, 1].map((ratio, idx) => {
                          const val = Math.round(maxVisitsInChart * ratio)
                          const y = 180 - ratio * 155
                          return (
                            <g key={idx}>
                              <line
                                x1="45"
                                y1={y}
                                x2="725"
                                y2={y}
                                stroke="#e2e8f0"
                                strokeDasharray="4 4"
                              />
                              <text
                                x="10"
                                y={y + 4}
                                fontSize="11"
                                fill="#94a3b8"
                                fontWeight="500"
                              >
                                {val}
                              </text>
                            </g>
                          )
                        })}

                        {/* Đường mức trung bình */}
                        {analytics.avgPerDay > 0 && (
                          <g>
                            <line
                              x1="45"
                              y1={180 - (analytics.avgPerDay / maxVisitsInChart) * 155}
                              x2="725"
                              y2={180 - (analytics.avgPerDay / maxVisitsInChart) * 155}
                              stroke="#f59e0b"
                              strokeWidth="1.5"
                              strokeDasharray="6 3"
                            />
                            <text
                              x="730"
                              y={180 - (analytics.avgPerDay / maxVisitsInChart) * 155 + 4}
                              fontSize="10"
                              fill="#f59e0b"
                              fontWeight="700"
                            >
                              TB ({analytics.formattedAvg})
                            </text>
                          </g>
                        )}

                        {/* DẠNG BIỂU ĐỒ CỘT (BAR CHART) */}
                        {chartType === 'bar' && (
                          <g>
                            {chartPoints.map((item, idx) => {
                              const barWidth = Math.max(
                                Math.min(500 / chartPoints.length, 32),
                                6,
                              )
                              const barHeight = (item.val / maxVisitsInChart) * 155
                              const barY = 180 - barHeight
                              const isHovered = hoveredDay?.date === item.date

                              return (
                                <g
                                  key={idx}
                                  onMouseEnter={() => setHoveredDay(item)}
                                  onMouseLeave={() => setHoveredDay(null)}
                                  style={{ cursor: 'pointer' }}
                                >
                                  <rect
                                    x={item.x - barWidth / 2}
                                    y={barY}
                                    width={barWidth}
                                    height={Math.max(barHeight, 2)}
                                    rx="4"
                                    fill={isHovered ? '#1d4ed8' : 'url(#visitBarGrad)'}
                                    opacity={hoveredDay && !isHovered ? 0.4 : 1}
                                  />
                                  {item.val > 0 && (
                                    <text
                                      x={item.x}
                                      y={barY - 6}
                                      fontSize="11"
                                      fontWeight="700"
                                      textAnchor="middle"
                                      fill={isHovered ? '#1d4ed8' : '#1e40af'}
                                    >
                                      {item.val}
                                    </text>
                                  )}
                                  {(chartPoints.length <= 15 || idx % Math.ceil(chartPoints.length / 10) === 0) && (
                                    <text
                                      x={item.x}
                                      y="202"
                                      fontSize="10"
                                      textAnchor="middle"
                                      fill="#64748b"
                                    >
                                      {item.formattedDate}
                                    </text>
                                  )}
                                </g>
                              )
                            })}
                          </g>
                        )}

                        {/* DẠNG BIỂU ĐỒ ĐƯỜNG (LINE & AREA CHART) */}
                        {chartType === 'line' && (
                          <g>
                            {chartPoints.length > 1 && (
                              <polygon
                                fill="url(#visitAreaGrad)"
                                points={`
                                  ${chartPoints[0].x},180
                                  ${chartPoints.map((p) => `${p.x},${p.y}`).join(' ')}
                                  ${chartPoints[chartPoints.length - 1].x},180
                                `}
                              />
                            )}

                            {chartPoints.length > 1 && (
                              <polyline
                                fill="none"
                                stroke="#2563eb"
                                strokeWidth="3"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                points={chartPoints.map((p) => `${p.x},${p.y}`).join(' ')}
                              />
                            )}

                            {chartPoints.map((item, idx) => {
                              const isHovered = hoveredDay?.date === item.date
                              return (
                                <g
                                  key={idx}
                                  onMouseEnter={() => setHoveredDay(item)}
                                  onMouseLeave={() => setHoveredDay(null)}
                                  style={{ cursor: 'pointer' }}
                                >
                                  <circle
                                    cx={item.x}
                                    cy={item.y}
                                    r={isHovered ? 7 : 4.5}
                                    fill={isHovered ? '#1d4ed8' : '#2563eb'}
                                    stroke="#ffffff"
                                    strokeWidth="2.5"
                                  />
                                  {item.val > 0 && (
                                    <text
                                      x={item.x}
                                      y={item.y - 9}
                                      fontSize="11"
                                      fontWeight="700"
                                      textAnchor="middle"
                                      fill={isHovered ? '#1d4ed8' : '#1e40af'}
                                    >
                                      {item.val}
                                    </text>
                                  )}
                                  {(chartPoints.length <= 15 || idx % Math.ceil(chartPoints.length / 10) === 0) && (
                                    <text
                                      x={item.x}
                                      y="202"
                                      fontSize="10"
                                      textAnchor="middle"
                                      fill="#64748b"
                                    >
                                      {item.formattedDate}
                                    </text>
                                  )}
                                </g>
                              )
                            })}
                          </g>
                        )}
                      </svg>
                    </div>

                    {/* Tooltip hover */}
                    {hoveredDay && (
                      <div
                        style={{
                          background: '#eff6ff',
                          border: '1px solid #bfdbfe',
                          borderRadius: 8,
                          padding: '8px 16px',
                          marginTop: 12,
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                        }}
                      >
                        <Space>
                          <CalendarOutlined style={{ color: '#2563eb' }} />
                          <span>
                            <strong>{hoveredDay.fullDate}</strong> ({hoveredDay.weekday})
                          </span>
                        </Space>
                        <Space size="large">
                          <span>
                            Số lượt khám: <strong style={{ color: '#1677ff', fontSize: 15 }}>{hoveredDay.val} lượt</strong>
                          </span>
                          <span>
                            Doanh thu ngày: <strong style={{ color: '#16a34a' }}>{formatCurrency(hoveredDay.revenue, summary.currency)}</strong>
                          </span>
                        </Space>
                      </div>
                    )}
                  </div>
                )}
              </Spin>
            </Card>

            {/* Bảng Dữ liệu Chi tiết Từng Ngày */}
            <Card
              style={{ borderRadius: 14, boxShadow: '0 2px 10px rgba(0,0,0,0.02)', border: '1px solid #f1f5f9' }}
              title={
                <Space>
                  <CalendarOutlined />
                  <span>Bảng thống kê Chi tiết Lượt khám & Doanh thu từng ngày ({timeline.length} ngày)</span>
                </Space>
              }
            >
              <Table
                rowKey="reportDate"
                columns={visitTableColumns}
                dataSource={timeline}
                loading={loading}
                pagination={{
                  pageSize: 15,
                  showSizeChanger: true,
                  pageSizeOptions: ['10', '15', '30', '50'],
                  showTotal: (total) => `Tổng cộng ${total} ngày trong kỳ`,
                }}
                locale={{ emptyText: <Empty description="Chưa có dữ liệu lượt khám" /> }}
              />
            </Card>
          </Space>
        )}

        {/* TAB 3: REVENUE REPORT (WITH DEDICATED REVENUE VIEW & ADJUSTMENTS EXCLUSION) */}
        {activeTab === 'revenue' && (
          <RevenueReportView
            range={range}
            onRangeChange={setRange}
            invoices={invoicesList}
            loading={loading}
            onRefresh={loadData}
          />
        )}

        {/* TAB 4: MEDICINES */}
        {activeTab === 'medicines' && (
          <Card style={{ borderRadius: 14, border: '1px solid #f1f5f9' }} title="Báo cáo số lượng thuốc đã cấp phát thực tế">
            <Table
              rowKey="name"
              dataSource={topMedicines}
              loading={loading}
              locale={{ emptyText: 'Chưa có dữ liệu cấp phát thuốc' }}
              columns={[
                { title: 'Thứ hạng', key: 'rank', render: (_, __, idx) => <Tag color={idx < 3 ? 'volcano' : 'blue'}>Top {idx + 1}</Tag> },
                { title: 'Tên thuốc', dataIndex: 'name', key: 'name', render: (v) => <strong>{v}</strong> },
                { title: 'Nhóm thuốc', dataIndex: 'category', key: 'category', render: (v) => v || 'Dược phẩm' },
                { title: 'Số lượng đã cấp phát', dataIndex: 'dispensedQuantity', key: 'dispensedQuantity', render: (v) => <Text type="danger" strong>{v || 0} đơn vị</Text> },
              ]}
            />
          </Card>
        )}

        {/* TAB 5: AUDIT LOGS */}
        {activeTab === 'audit' && (
          <Card
            style={{ borderRadius: 14, border: '1px solid #f1f5f9' }}
            title="Nhật ký truy cập và giám sát hồ sơ y tế thực tế"
            extra={
              <Input
                placeholder="Tìm kiếm người dùng, bệnh nhân..."
                prefix={<SearchOutlined />}
                value={logSearch}
                onChange={(e) => setLogSearch(e.target.value)}
                style={{ width: 300, borderRadius: 8 }}
              />
            }
          >
            <Table
              rowKey="id"
              dataSource={filteredLogs}
              loading={loading}
              locale={{ emptyText: 'Chưa có nhật ký truy cập' }}
              columns={[
                { title: 'Người dùng', dataIndex: 'userName', key: 'userName', render: (v) => <Tag color="purple">{v}</Tag> },
                { title: 'Bệnh nhân', dataIndex: 'patientName', key: 'patientName', render: (v) => <strong>{v}</strong> },
                { title: 'Mã bệnh án', dataIndex: 'recordCode', key: 'recordCode', render: (v) => <Tag color="blue">{v}</Tag> },
                { title: 'Hành động', dataIndex: 'action', key: 'action', render: (v) => <Tag color="cyan">{v}</Tag> },
                { title: 'Thời gian', dataIndex: 'accessedAt', key: 'accessedAt', render: (v) => new Date(v).toLocaleString('vi-VN') },
              ]}
            />
          </Card>
        )}
      </div>
    </div>
  )
}

export default ReportsPage
