import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  message,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import {
  AlertOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  DashboardOutlined,
  DollarCircleOutlined,
  DownloadOutlined,
  EyeOutlined,
  FileSearchOutlined,
  LineChartOutlined,
  MedicineBoxOutlined,
  NodeIndexOutlined,
  PlusOutlined,
  PrinterOutlined,
  QrcodeOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  ShoppingOutlined,
  UserOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import reportApi from '../api/reportApi'
import {
  getStoredAuditLogs,
  getStoredInvoices,
  getStoredMedicalRecords,
  getStoredMedicines,
  getStoredPrescriptions,
} from '../utils/storageHelpers'

const { RangePicker } = DatePicker
const { Text, Title } = Typography

const money = (v) => `${Number(v || 0).toLocaleString('vi-VN')} đ`

function ReportsPage() {
  const [range, setRange] = useState([dayjs().subtract(6, 'day'), dayjs()])
  const [loading, setLoading] = useState(false)
  const [activeTab, setActiveTab] = useState('visits')

  // Dynamic Data States (STRICTLY FROM RECORDED DATA)
  const [summary, setSummary] = useState({ visitCount: 0, revenue: 0, dispensedCount: 0, auditCount: 0 })
  const [timeline, setTimeline] = useState([])
  const [topMedicines, setTopMedicines] = useState([])
  const [auditLogs, setAuditLogs] = useState([])
  const [invoicesList, setInvoicesList] = useState([])
  const [logSearch, setLogSearch] = useState('')

  const getParams = useCallback(() => ({
    from: range[0] ? range[0].format('YYYY-MM-DD') : dayjs().startOf('month').format('YYYY-MM-DD'),
    to: range[1] ? range[1].format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD'),
  }), [range])

  const loadData = useCallback(async () => {
    setLoading(true)
    const params = getParams()
    try {
      const [summaryRes, timelineRes, medicinesRes, auditRes] = await Promise.allSettled([
        reportApi.summary(params),
        reportApi.timeline(params),
        reportApi.topMedicines(params),
        reportApi.audit(params),
      ])

      const apiSummary = summaryRes.status === 'fulfilled' ? summaryRes.value.data : null
      const apiAudit = auditRes.status === 'fulfilled' ? auditRes.value.data : null

      // Get STRICT REAL RECORDED DATA from local persistence + API (No pre-set static offsets!)
      const storedRecords = getStoredMedicalRecords()
      const storedInvoices = getStoredInvoices()
      const storedPrescriptions = getStoredPrescriptions()
      const storedMedicines = getStoredMedicines()
      const storedLogs = getStoredAuditLogs()

      const apiRecords = (apiSummary && Array.isArray(apiSummary.records)) ? apiSummary.records : []
      const apiInvs = (apiSummary && Array.isArray(apiSummary.invoices)) ? apiSummary.invoices : []
      const apiPrescs = (apiSummary && Array.isArray(apiSummary.prescriptions)) ? apiSummary.prescriptions : []

      // Merge ONLY real created items from user actions
      const realRecords = [...storedRecords, ...apiRecords]
      const realInvoices = [...storedInvoices, ...apiInvs]
      const realPrescriptions = [...storedPrescriptions, ...apiPrescs]
      const realLogs = (apiAudit && Array.isArray(apiAudit)) ? [...storedLogs, ...apiAudit] : storedLogs

      setInvoicesList(realInvoices)

      // STRICT REAL STATS CALCULATION
      const totalVisits = realRecords.length
      const totalRevenue = realInvoices.reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0)
      const dispensedCount = realPrescriptions.filter((p) => p.status === 'DISPENSED' || p.status === 'COMPLETED').length
      const auditCount = realLogs.length

      // STRICT REAL TIMELINE CALCULATION FOR SELECTED RANGE
      const startDate = range[0] || dayjs().subtract(6, 'day')
      const endDate = range[1] || dayjs()
      const diffDays = Math.max(0, endDate.diff(startDate, 'day'))
      const datesCount = Math.min(diffDays + 1, 31)

      const timelineList = []
      for (let i = 0; i < datesCount; i++) {
        const currentDate = startDate.add(i, 'day')
        const formattedDate = currentDate.format('DD/MM/YYYY')
        const dateISO = currentDate.format('YYYY-MM-DD')

        // Count real records on this date
        const dayVisits = realRecords.filter((r) => (
          r.createdAt && dayjs(r.createdAt).format('YYYY-MM-DD') === dateISO
        )).length

        // Sum real invoice revenue on this date
        const dayRevenue = realInvoices
          .filter((inv) => inv.createdAt && dayjs(inv.createdAt).format('YYYY-MM-DD') === dateISO)
          .reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0)

        timelineList.push({
          reportDate: formattedDate,
          visitCount: dayVisits,
          revenue: dayRevenue,
        })
      }

      // STRICT REAL TOP MEDICINES RANKING
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

      const rankedMeds = Array.from(medMap.entries())
        .map(([name, dispensedQuantity]) => ({ name, category: 'Dược phẩm', dispensedQuantity }))
        .sort((a, b) => b.dispensedQuantity - a.dispensedQuantity)

      setSummary({
        visitCount: totalVisits,
        revenue: totalRevenue,
        dispensedCount,
        auditCount,
      })

      setTimeline(timelineList)
      setTopMedicines(rankedMeds)
      setAuditLogs(realLogs)
    } catch {
      // ignore load error
    } finally {
      setLoading(false)
    }
  }, [getParams, range])

  useEffect(() => { loadData() }, [loadData])

  const handleExportCSV = () => {
    try {
      const params = getParams()
      const csvRows = [
        ['BAO CAO VAN HANH VA NHAT KY TRUY CAP (DU LIEU THUC TE GHI NHAN)'],
        [`Khoang thoi gian: ${params.from} - ${params.to}`],
        [''],
        ['1. TONG QUAN CHI SO THUC TE'],
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
        [''],
        ['4. NHAT KY TRUY CAP BENH AN'],
        ['Nguoi dung', 'Benh nhan', 'Ma benh an', 'Hanh dong', 'Thoi gian'],
        ...auditLogs.map((l) => [l.userName, l.patientName, l.recordCode, l.action, l.accessedAt]),
      ]

      const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + csvRows.map((e) => e.join(',')).join('\n')
      const encodedUri = encodeURI(csvContent)
      const link = document.createElement('a')
      link.setAttribute('href', encodedUri)
      link.setAttribute('download', `bao-cao-van-hanh-thuc-te-${params.from}-${params.to}.csv`)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      message.success('Đã xuất file báo cáo CSV thực tế thành công!')
    } catch {
      message.error('Không thể xuất báo cáo')
    }
  }

  // Calculated right side metrics strictly from recorded timeline
  const totalVisitsTimeline = timeline.reduce((s, t) => s + Number(t.visitCount || 0), 0)
  const averageVisitsPerDay = (totalVisitsTimeline / Math.max(timeline.length, 1)).toFixed(1).replace('.', ',')
  const peakDayItem = timeline.reduce((max, item) => (Number(item.visitCount || 0) > Number(max.visitCount || 0) ? item : max), timeline[0] || {})
  const periodRevenueTotal = timeline.reduce((s, t) => s + Number(t.revenue || 0), 0)

  const filteredLogs = auditLogs.filter((log) => {
    if (!logSearch.trim()) return true
    const term = logSearch.toLowerCase()
    return (
      (log.userName && log.userName.toLowerCase().includes(term)) ||
      (log.patientName && log.patientName.toLowerCase().includes(term)) ||
      (log.recordCode && log.recordCode.toLowerCase().includes(term)) ||
      (log.action && log.action.toLowerCase().includes(term))
    )
  })

  const maxVisitsChart = Math.max(...timeline.map((t) => Number(t.visitCount || 0)), 10)

  return (
    <div style={{ background: '#f8fafc', minHeight: '100vh', padding: '16px 24px' }}>
      {/* Header Section */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ margin: 0, fontWeight: 700, color: '#0f172a' }}>
            Báo cáo vận hành và nhật ký truy cập
          </Title>
          <Text type="secondary" style={{ fontSize: 13, color: '#64748b' }}>
            Theo dõi số liệu khám bệnh, doanh thu, thuốc và lịch sử truy cập hệ thống (Dữ liệu ghi nhận thực tế)
          </Text>
        </div>
        <Space size="middle">
          <RangePicker
            value={range}
            format="DD/MM/YYYY"
            onChange={(val) => val && setRange(val)}
            allowClear={false}
            style={{ borderRadius: 8, padding: '6px 12px' }}
          />
          <Button
            icon={<ReloadOutlined />}
            onClick={loadData}
            style={{ borderRadius: 8, height: 38 }}
          >
            Làm mới
          </Button>
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            onClick={handleExportCSV}
            style={{ borderRadius: 8, height: 38, background: '#1677ff', border: 'none', fontWeight: 600 }}
          >
            Xuất báo cáo (CSV)
          </Button>
        </Space>
      </div>

      {/* Top 4 Summary Cards (COMPUTED STRICTLY FROM REAL RECORDED DATA) */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {/* Card 1: Tổng lượt khám */}
        <Col xs={24} sm={12} md={6}>
          <div
            style={{
              background: '#ffffff',
              borderRadius: 14,
              padding: '18px 20px',
              display: 'flex',
              alignItems: 'center',
              boxShadow: '0 2px 10px rgba(0,0,0,0.03)',
              border: '1px solid #f1f5f9',
            }}
          >
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: 14,
                background: '#1677ff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginRight: 16,
                color: '#fff',
                fontSize: 24,
              }}
            >
              <CalendarOutlined />
            </div>
            <div>
              <div style={{ fontSize: 13, color: '#64748b', fontWeight: 500 }}>Tổng lượt khám</div>
              <div style={{ fontSize: 22, fontWeight: 800, color: '#1677ff', lineHeight: 1.2 }}>
                {summary.visitCount} lượt
              </div>
              <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>Ghi nhận thực tế từ bệnh án</div>
            </div>
          </div>
        </Col>

        {/* Card 2: Doanh thu phòng khám */}
        <Col xs={24} sm={12} md={6}>
          <div
            style={{
              background: '#ffffff',
              borderRadius: 14,
              padding: '18px 20px',
              display: 'flex',
              alignItems: 'center',
              boxShadow: '0 2px 10px rgba(0,0,0,0.03)',
              border: '1px solid #f1f5f9',
            }}
          >
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: 14,
                background: '#22c55e',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginRight: 16,
                color: '#fff',
                fontSize: 24,
              }}
            >
              <DollarCircleOutlined />
            </div>
            <div>
              <div style={{ fontSize: 13, color: '#64748b', fontWeight: 500 }}>Doanh thu phòng khám</div>
              <div style={{ fontSize: 20, fontWeight: 800, color: '#16a34a', lineHeight: 1.2 }}>
                {money(summary.revenue)}
              </div>
              <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>Ghi nhận thực tế từ hóa đơn</div>
            </div>
          </div>
        </Col>

        {/* Card 3: Đơn thuốc đã cấp */}
        <Col xs={24} sm={12} md={6}>
          <div
            style={{
              background: '#ffffff',
              borderRadius: 14,
              padding: '18px 20px',
              display: 'flex',
              alignItems: 'center',
              boxShadow: '0 2px 10px rgba(0,0,0,0.03)',
              border: '1px solid #f1f5f9',
            }}
          >
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: 14,
                background: '#ff7a00',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginRight: 16,
                color: '#fff',
                fontSize: 24,
              }}
            >
              <MedicineBoxOutlined />
            </div>
            <div>
              <div style={{ fontSize: 13, color: '#64748b', fontWeight: 500 }}>Đơn thuốc đã cấp</div>
              <div style={{ fontSize: 22, fontWeight: 800, color: '#ea580c', lineHeight: 1.2 }}>
                {summary.dispensedCount} đơn
              </div>
              <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>Ghi nhận thực tế từ kho thuốc</div>
            </div>
          </div>
        </Col>

        {/* Card 4: Nhật ký truy cập */}
        <Col xs={24} sm={12} md={6}>
          <div
            style={{
              background: '#ffffff',
              borderRadius: 14,
              padding: '18px 20px',
              display: 'flex',
              alignItems: 'center',
              boxShadow: '0 2px 10px rgba(0,0,0,0.03)',
              border: '1px solid #f1f5f9',
            }}
          >
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: 14,
                background: '#7c3aed',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginRight: 16,
                color: '#fff',
                fontSize: 24,
              }}
            >
              <FileSearchOutlined />
            </div>
            <div>
              <div style={{ fontSize: 13, color: '#64748b', fontWeight: 500 }}>Nhật ký truy cập</div>
              <div style={{ fontSize: 22, fontWeight: 800, color: '#7c3aed', lineHeight: 1.2 }}>
                {summary.auditCount} lượt xem
              </div>
              <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>Ghi nhận thực tế truy cập y tế</div>
            </div>
          </div>
        </Col>
      </Row>

      {/* Tabs Header Navigation */}
      <Card
        style={{ borderRadius: 14, boxShadow: '0 2px 10px rgba(0,0,0,0.02)', border: '1px solid #f1f5f9' }}
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

      <div style={{ marginTop: 20 }}>
        {/* TAB 2 / VISITS: Strict Real Data Chart and Breakdown Table */}
        {activeTab === 'visits' && (
          <Row gutter={20}>
            {/* Left 75% Panel */}
            <Col xs={24} lg={17}>
              <Card
                style={{ borderRadius: 14, border: '1px solid #f1f5f9', boxShadow: '0 2px 10px rgba(0,0,0,0.02)' }}
              >
                <Title level={4} style={{ margin: '0 0 16px 0', fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
                  Biểu đồ phân tích lượt khám và xu hướng vận hành theo ngày
                </Title>

                <Alert
                  type="info"
                  showIcon
                  message={<strong>Xu hướng vận hành thực tế</strong>}
                  description="Dữ liệu biểu đồ và bảng chi tiết được tổng hợp trực tiếp từ các lượt khám bệnh và hóa đơn thực tế được ghi nhận trên hệ thống."
                  style={{ marginBottom: 20, borderRadius: 10, background: '#eff6ff', border: '1px solid #bfdbfe' }}
                />

                {/* SVG Line Chart strictly mapping timeline entries */}
                <div style={{ padding: '10px 0 20px 0' }}>
                  <div style={{ fontSize: 12, color: '#64748b', fontWeight: 600, marginBottom: 8 }}>Lượt khám (lượt)</div>
                  <svg viewBox="0 0 700 210" style={{ width: '100%', maxHeight: 220 }}>
                    {/* Horizontal Dotted Lines */}
                    {[0, Math.ceil(maxVisitsChart * 0.25), Math.ceil(maxVisitsChart * 0.5), Math.ceil(maxVisitsChart * 0.75), maxVisitsChart].reverse().map((val, idx) => {
                      const y = 30 + idx * 35
                      return (
                        <g key={idx}>
                          <line x1="40" y1={y} x2="680" y2={y} stroke="#e2e8f0" strokeDasharray="4 4" />
                          <text x="10" y={y + 4} fontSize="11" fill="#94a3b8">{val}</text>
                        </g>
                      )
                    })}

                    {/* Blue Trend Line mapping real timeline points */}
                    {timeline.length > 0 && (
                      <>
                        <polyline
                          fill="none"
                          stroke="#2563eb"
                          strokeWidth="2.5"
                          points={timeline.map((item, idx) => {
                            const x = 70 + idx * Math.min(95, 590 / Math.max(timeline.length - 1, 1))
                            const y = 170 - (Number(item.visitCount || 0) / Math.max(maxVisitsChart, 1)) * 130
                            return `${x},${y}`
                          }).join(' ')}
                        />
                        {timeline.map((item, idx) => {
                          const x = 70 + idx * Math.min(95, 590 / Math.max(timeline.length - 1, 1))
                          const y = 170 - (Number(item.visitCount || 0) / Math.max(maxVisitsChart, 1)) * 130
                          return (
                            <g key={idx}>
                              <circle cx={x} cy={y} r="5" fill="#2563eb" stroke="#ffffff" strokeWidth="2" />
                              <text x={x} y={y - 10} fontSize="12" fontWeight="bold" textAnchor="middle" fill="#1e40af">
                                {item.visitCount}
                              </text>
                              <text x={x} y="198" fontSize="10" textAnchor="middle" fill="#64748b">
                                {item.reportDate}
                              </text>
                            </g>
                          )
                        })}
                      </>
                    )}
                  </svg>
                </div>

                {/* Daily Breakdown Table showing strict recorded data */}
                <Table
                  rowKey="reportDate"
                  dataSource={timeline}
                  pagination={false}
                  loading={loading}
                  size="middle"
                  columns={[
                    { title: 'Ngày báo cáo', dataIndex: 'reportDate', key: 'reportDate', render: (v) => <strong>{v}</strong> },
                    {
                      title: 'Số lượt khám',
                      dataIndex: 'visitCount',
                      key: 'visitCount',
                      render: (v) => (
                        <Tag style={{ borderRadius: 12, padding: '2px 12px', background: '#eff6ff', color: '#2563eb', border: '1px solid #bfdbfe', fontWeight: 600 }}>
                          {v} lượt
                        </Tag>
                      ),
                    },
                    { title: 'Doanh thu phát sinh', dataIndex: 'revenue', key: 'revenue', render: (v) => money(v) },
                  ]}
                />
              </Card>
            </Col>

            {/* Right 25% Side Panel mapped strictly to recorded timeline */}
            <Col xs={24} lg={7}>
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                {/* Side Card 1: Trung bình / ngày */}
                <div
                  style={{
                    background: '#ffffff',
                    borderRadius: 14,
                    padding: '24px 20px',
                    border: '1px solid #f1f5f9',
                    boxShadow: '0 2px 10px rgba(0,0,0,0.02)',
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <div
                    style={{
                      width: 52,
                      height: 52,
                      borderRadius: '50%',
                      background: '#eff6ff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      marginRight: 16,
                      color: '#2563eb',
                      fontSize: 22,
                    }}
                  >
                    <LineChartOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 13, color: '#64748b', fontWeight: 500 }}>Trung bình/ngày</div>
                    <div style={{ fontSize: 22, fontWeight: 800, color: '#2563eb', marginTop: 2 }}>
                      {averageVisitsPerDay} lượt
                    </div>
                  </div>
                </div>

                {/* Side Card 2: Ngày cao nhất */}
                <div
                  style={{
                    background: '#ffffff',
                    borderRadius: 14,
                    padding: '24px 20px',
                    border: '1px solid #f1f5f9',
                    boxShadow: '0 2px 10px rgba(0,0,0,0.02)',
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <div
                    style={{
                      width: 52,
                      height: 52,
                      borderRadius: '50%',
                      background: '#f0fdf4',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      marginRight: 16,
                      color: '#16a34a',
                      fontSize: 22,
                    }}
                  >
                    <CalendarOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 13, color: '#64748b', fontWeight: 500 }}>Ngày cao nhất</div>
                    <div style={{ fontSize: 20, fontWeight: 800, color: '#16a34a', marginTop: 2 }}>
                      {peakDayItem?.reportDate || dayjs().format('DD/MM/YYYY')}
                    </div>
                  </div>
                </div>

                {/* Side Card 3: Doanh thu kỳ này */}
                <div
                  style={{
                    background: '#ffffff',
                    borderRadius: 14,
                    padding: '24px 20px',
                    border: '1px solid #f1f5f9',
                    boxShadow: '0 2px 10px rgba(0,0,0,0.02)',
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <div
                    style={{
                      width: 52,
                      height: 52,
                      borderRadius: '50%',
                      background: '#fff7ed',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      marginRight: 16,
                      color: '#ea580c',
                      fontSize: 22,
                    }}
                  >
                    <WalletOutlined />
                  </div>
                  <div>
                    <div style={{ fontSize: 13, color: '#64748b', fontWeight: 500 }}>Doanh thu kỳ này</div>
                    <div style={{ fontSize: 20, fontWeight: 800, color: '#ea580c', marginTop: 2 }}>
                      {money(periodRevenueTotal)}
                    </div>
                  </div>
                </div>
              </Space>
            </Col>
          </Row>
        )}

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

        {/* TAB 3: REVENUE */}
        {activeTab === 'revenue' && (
          <Card style={{ borderRadius: 14, border: '1px solid #f1f5f9' }} title="Danh sách hóa đơn thanh toán thực tế">
            <Table
              rowKey="id"
              dataSource={invoicesList}
              loading={loading}
              columns={[
                { title: 'Mã hóa đơn', dataIndex: 'invoiceCode', key: 'invoiceCode', render: (v) => <strong>{v}</strong> },
                { title: 'Tên bệnh nhân', dataIndex: 'patientName', key: 'patientName' },
                {
                  title: 'Loại hóa đơn',
                  dataIndex: 'invoiceType',
                  key: 'invoiceType',
                  render: (v) => (
                    <Tag color={v === 'ORIGINAL' ? 'green' : 'orange'}>
                      {v === 'ORIGINAL' ? 'Hóa đơn gốc' : 'Hóa đơn điều chỉnh'}
                    </Tag>
                  ),
                },
                { title: 'Phương thức', dataIndex: 'paymentMethodLabel', key: 'paymentMethodLabel' },
                { title: 'Số tiền', dataIndex: 'totalAmount', key: 'totalAmount', render: (v) => money(v) },
                { title: 'Ngày lập', dataIndex: 'createdAt', key: 'createdAt', render: (v) => new Date(v).toLocaleString('vi-VN') },
              ]}
            />
          </Card>
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
