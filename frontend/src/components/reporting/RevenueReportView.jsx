import React, { useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Input,
  message,
  Progress,
  Radio,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  DollarCircleOutlined,
  DownloadOutlined,
  ExclamationCircleOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  InfoCircleOutlined,
  LineChartOutlined,
  PercentageOutlined,
  ReloadOutlined,
  SearchOutlined,
  SwapOutlined,
  TrophyOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  formatMoney,
  calculateFinancialMetrics,
  aggregateDailyTimeline,
  aggregatePaymentMethods,
  getPresetDateRange,
} from '../../utils/revenueHelpers'
import '../../styles/reports.css'

const { RangePicker } = DatePicker
const { Text, Title } = Typography

export {
  formatMoney,
  calculateFinancialMetrics,
  aggregateDailyTimeline,
  aggregatePaymentMethods,
  getPresetDateRange,
}

export default function RevenueReportView({
  range = [dayjs().subtract(6, 'day'), dayjs()],
  onRangeChange,
  invoices = [],
  loading = false,
  onRefresh,
}) {
  const [activePreset, setActivePreset] = useState('7days')
  const [chartMode, setChartMode] = useState('trend') // 'trend' (Area/Line) | 'comparison' (Bar)
  const [invoiceTypeFilter, setInvoiceTypeFilter] = useState('ALL')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [detailTab, setDetailTab] = useState('timeline')
  const [hoveredPoint, setHoveredPoint] = useState(null)

  const startDate = range[0] || dayjs().subtract(6, 'day')
  const endDate = range[1] || dayjs()
  const daysDiff = Math.max(1, endDate.diff(startDate, 'day') + 1)

  // Filter invoices strictly within selected date range
  const periodInvoices = useMemo(() => {
    const startStr = startDate.format('YYYY-MM-DD')
    const endStr = endDate.format('YYYY-MM-DD')
    return (invoices || []).filter((inv) => {
      if (!inv.createdAt) return false
      const invDate = dayjs(inv.createdAt).format('YYYY-MM-DD')
      return invDate >= startStr && invDate <= endStr
    })
  }, [invoices, startDate, endDate])

  // Financial Metrics
  const metrics = useMemo(() => {
    return calculateFinancialMetrics(periodInvoices, [], daysDiff)
  }, [periodInvoices, daysDiff])

  // Daily Timeline
  const dailyTimeline = useMemo(() => {
    return aggregateDailyTimeline(periodInvoices, startDate, endDate)
  }, [periodInvoices, startDate, endDate])

  // Payment Breakdown
  const paymentMethods = useMemo(() => {
    return aggregatePaymentMethods(periodInvoices)
  }, [periodInvoices])

  // Peak Day
  const peakDay = useMemo(() => {
    if (!dailyTimeline.length) return null
    return dailyTimeline.reduce((max, cur) => (cur.netRevenue > max.netRevenue ? cur : max), dailyTimeline[0])
  }, [dailyTimeline])

  // Handle Preset Change
  const handleSelectPreset = (key) => {
    setActivePreset(key)
    const newRange = getPresetDateRange(key)
    if (onRangeChange) {
      onRangeChange(newRange)
    }
  }

  // Filtered invoices for detailed table
  const filteredInvoices = useMemo(() => {
    return periodInvoices.filter((inv) => {
      // Type filter
      if (invoiceTypeFilter === 'ORIGINAL' && inv.invoiceType === 'ADJUSTMENT') return false
      if (invoiceTypeFilter === 'ADJUSTMENT' && inv.invoiceType !== 'ADJUSTMENT') return false

      // Search keyword
      if (!searchKeyword.trim()) return true
      const kw = searchKeyword.toLowerCase()
      return (
        (inv.invoiceCode && inv.invoiceCode.toLowerCase().includes(kw)) ||
        (inv.patientName && inv.patientName.toLowerCase().includes(kw)) ||
        (inv.originalInvoiceCode && inv.originalInvoiceCode.toLowerCase().includes(kw)) ||
        (inv.adjustmentReason && inv.adjustmentReason.toLowerCase().includes(kw))
      )
    })
  }, [periodInvoices, invoiceTypeFilter, searchKeyword])

  // Export Financial CSV
  const handleExportFinancialCSV = () => {
    try {
      const fromStr = startDate.format('YYYY-MM-DD')
      const toStr = endDate.format('YYYY-MM-DD')

      const csvRows = [
        ['BAO CAO DOANH THU PHONG KHAM (DA LOAI TRU KHOAN DIEU CHINH)'],
        [`Ky bao cao: Tu ${fromStr} Den ${toStr} (${daysDiff} ngay)`],
        [''],
        ['1. TONG HOP CHI SO TAI CHINH'],
        ['Chi so', 'Gia tri', 'Giai trinh'],
        ['Doanh thu thuan (Net Revenue)', `${metrics.netRevenue} VND`, 'Doanh thu thuc te sau khi tru khoan dieu chinh'],
        ['Doanh thu hoa don goc (Gross)', `${metrics.grossRevenue} VND`, `Tong tu ${metrics.originalCount} hoa don goc`],
        ['Tong khoan dieu chinh / hoan tien', `${metrics.adjustmentTotal} VND`, `Tong tu ${metrics.adjustmentCount} hoa don dieu chinh`],
        ['Doanh thu trung binh / ngay', `${Math.round(metrics.averageDaily)} VND/ngay`, 'Doanh thu thuan chia tong so ngay trong ky'],
        ['Ty le dieu chinh / hoan tien', `${metrics.adjustmentRate.toFixed(2)}%`, 'Ty le giam tru so voi doanh thu goc'],
        [''],
        ['2. DIEN BIEN DOANH THU THEO NGAY'],
        ['Ngay', 'So HD goc', 'So HD dieu chinh', 'Doanh thu goc (VND)', 'Khoan dieu chinh (VND)', 'Doanh thu thuan (VND)'],
        ...dailyTimeline.map((d) => [
          d.displayDate,
          d.originalCount,
          d.adjustmentCount,
          d.grossRevenue,
          d.adjustments,
          d.netRevenue,
        ]),
        [''],
        ['3. CO CAU PHUONG THUC THANH TOAN'],
        ['Phuong thuc', 'So giao dich', 'Tong tien (VND)', 'Ty trong (%)'],
        ...paymentMethods.map((m) => [m.label, m.count, m.total, `${m.percentage.toFixed(1)}%`]),
        [''],
        ['4. DANH SACH HOA DON CHI TIET TRONG KY'],
        ['Ma hoa don', 'Loai hoa don', 'Benh nhan', 'So tien (VND)', 'Ma HD goc lien quan', 'Phuong thuc', 'Ly do dieu chinh', 'Ngay lap'],
        ...periodInvoices.map((inv) => [
          inv.invoiceCode || '',
          inv.invoiceType === 'ADJUSTMENT' ? 'Hoa don dieu chinh' : 'Hoa don goc',
          inv.patientName || '',
          inv.totalAmount || 0,
          inv.originalInvoiceCode || '',
          inv.paymentMethodLabel || inv.paymentMethod || 'Tien mat',
          inv.adjustmentReason || '',
          inv.createdAt ? dayjs(inv.createdAt).format('DD/MM/YYYY HH:mm') : '',
        ]),
      ]

      const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + csvRows.map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\n')
      const encodedUri = encodeURI(csvContent)
      const link = document.createElement('a')
      link.setAttribute('href', encodedUri)
      link.setAttribute('download', `bao-cao-doanh-thu-${fromStr}-${toStr}.csv`)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      message.success('Đã xuất báo cáo doanh thu tài chính CSV thành công!')
    } catch {
      message.error('Không thể xuất báo cáo doanh thu')
    }
  }

  // Calculate Chart Coordinates
  const chartWidth = 740
  const chartHeight = 250
  const padLeft = 75
  const padRight = 30
  const padTop = 30
  const padBottom = 40

  const maxRevenueVal = useMemo(() => {
    const maxVal = Math.max(
      ...dailyTimeline.map((d) => Math.max(d.grossRevenue, d.netRevenue)),
      1000000,
    )
    return Math.ceil(maxVal / 1000000) * 1000000
  }, [dailyTimeline])

  const chartPoints = useMemo(() => {
    if (!dailyTimeline.length) return []
    const availableWidth = chartWidth - padLeft - padRight
    const stepX = dailyTimeline.length > 1 ? availableWidth / (dailyTimeline.length - 1) : availableWidth / 2

    return dailyTimeline.map((d, idx) => {
      const x = padLeft + (dailyTimeline.length === 1 ? availableWidth / 2 : idx * stepX)
      const ratio = Math.max(0, d.netRevenue / maxRevenueVal)
      const y = chartHeight - padBottom - ratio * (chartHeight - padTop - padBottom)

      const grossRatio = Math.max(0, d.grossRevenue / maxRevenueVal)
      const yGross = chartHeight - padBottom - grossRatio * (chartHeight - padTop - padBottom)

      const adjHeight = (Math.abs(d.adjustments) / maxRevenueVal) * (chartHeight - padTop - padBottom)

      return {
        x,
        y,
        yGross,
        adjHeight,
        ...d,
      }
    })
  }, [dailyTimeline, maxRevenueVal])

  const polylineStr = useMemo(() => {
    return chartPoints.map((p) => `${p.x},${p.y}`).join(' ')
  }, [chartPoints])

  const areaPathStr = useMemo(() => {
    if (!chartPoints.length) return ''
    const firstX = chartPoints[0].x
    const lastX = chartPoints[chartPoints.length - 1].x
    const baseY = chartHeight - padBottom
    return `M${firstX} ${baseY} ${chartPoints.map((p) => `L${p.x} ${p.y}`).join(' ')} L${lastX} ${baseY} Z`
  }, [chartPoints])

  return (
    <div className="revenue-report-container">
      {/* 1. Quick Period Selector Bar */}
      <div className="revenue-preset-bar">
        <div className="revenue-preset-group">
          <span style={{ fontSize: 13, fontWeight: 600, color: '#475569', marginRight: 4 }}>
            <CalendarOutlined /> Kỳ báo cáo:
          </span>
          {[
            { key: 'today', label: 'Hôm nay' },
            { key: '7days', label: '7 ngày qua' },
            { key: '30days', label: '30 ngày qua' },
            { key: 'thisMonth', label: 'Tháng này' },
            { key: 'thisQuarter', label: 'Quý này' },
            { key: 'thisYear', label: 'Năm nay' },
          ].map((preset) => (
            <button
              key={preset.key}
              type="button"
              className={`revenue-preset-btn ${activePreset === preset.key ? 'active' : ''}`}
              onClick={() => handleSelectPreset(preset.key)}
            >
              {preset.label}
            </button>
          ))}
        </div>

        <Space size="middle" style={{ flexWrap: 'wrap' }}>
          <RangePicker
            value={range}
            format="DD/MM/YYYY"
            onChange={(val) => {
              if (val) {
                setActivePreset('custom')
                if (onRangeChange) onRangeChange(val)
              }
            }}
            allowClear={false}
            style={{ borderRadius: 8, padding: '5px 12px' }}
          />
          <Button
            icon={<ReloadOutlined />}
            onClick={onRefresh}
            loading={loading}
            style={{ borderRadius: 8 }}
          >
            Làm mới
          </Button>
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            onClick={handleExportFinancialCSV}
            style={{ borderRadius: 8, background: '#0f766e', borderColor: '#0f766e', fontWeight: 600 }}
          >
            Xuất báo cáo (CSV)
          </Button>
        </Space>
      </div>

      {/* 2. Top Notice on Adjustment Exclusion */}
      <div className="revenue-adjustment-alert">
        <CheckCircleOutlined className="revenue-adjustment-alert-icon" />
        <div className="revenue-adjustment-alert-content">
          <h4>Báo cáo tài chính chuẩn xác — Tự động loại trừ khoản điều chỉnh</h4>
          <p>
            Doanh thu thuần được tính tự động từ các hóa đơn gốc và đã trừ đi toàn bộ khoản điều chỉnh/hoàn tiền phát sinh trong kỳ theo đúng nghiệp vụ kế toán phòng khám.
          </p>
        </div>
      </div>

      {/* 3. 5 KPI Cards Grid */}
      <div className="revenue-kpi-grid">
        {/* Card 1: Doanh thu thuần */}
        <div className="revenue-kpi-card net">
          <div>
            <div className="revenue-kpi-header">
              <span className="revenue-kpi-label">Doanh thu thuần (Net)</span>
              <div className="revenue-kpi-icon net"><DollarCircleOutlined /></div>
            </div>
            <div className="revenue-kpi-value net">{formatMoney(metrics.netRevenue)}</div>
          </div>
          <div className="revenue-kpi-sub">
            <span className="revenue-kpi-badge success">Thực thu</span>
            <span>= Doanh thu gốc - Điều chỉnh</span>
          </div>
        </div>

        {/* Card 2: Doanh thu gốc */}
        <div className="revenue-kpi-card gross">
          <div>
            <div className="revenue-kpi-header">
              <span className="revenue-kpi-label">Doanh thu gốc (Gross)</span>
              <div className="revenue-kpi-icon gross"><WalletOutlined /></div>
            </div>
            <div className="revenue-kpi-value gross">{formatMoney(metrics.grossRevenue)}</div>
          </div>
          <div className="revenue-kpi-sub">
            <span className="revenue-kpi-badge info">{metrics.originalCount} hóa đơn</span>
            <span>Hóa đơn phát sinh gốc</span>
          </div>
        </div>

        {/* Card 3: Khoản điều chỉnh */}
        <div className="revenue-kpi-card adjustment">
          <div>
            <div className="revenue-kpi-header">
              <span className="revenue-kpi-label">Khoản điều chỉnh</span>
              <div className="revenue-kpi-icon adjustment"><SwapOutlined /></div>
            </div>
            <div className="revenue-kpi-value adjustment">
              {metrics.adjustmentTotal !== 0 ? formatMoney(metrics.adjustmentTotal) : '0 đ'}
            </div>
          </div>
          <div className="revenue-kpi-sub">
            <span className="revenue-kpi-badge warning">{metrics.adjustmentCount} điều chỉnh</span>
            <span>Giảm trừ / hoàn tiền</span>
          </div>
        </div>

        {/* Card 4: Trung bình / ngày */}
        <div className="revenue-kpi-card average">
          <div>
            <div className="revenue-kpi-header">
              <span className="revenue-kpi-label">Trung bình / ngày</span>
              <div className="revenue-kpi-icon average"><LineChartOutlined /></div>
            </div>
            <div className="revenue-kpi-value average">{formatMoney(metrics.averageDaily)}</div>
          </div>
          <div className="revenue-kpi-sub">
            <span>Tính trên <strong>{daysDiff}</strong> ngày kỳ này</span>
          </div>
        </div>

        {/* Card 5: Tỷ lệ điều chỉnh */}
        <div className="revenue-kpi-card rate">
          <div>
            <div className="revenue-kpi-header">
              <span className="revenue-kpi-label">Tỷ lệ điều chỉnh</span>
              <div className="revenue-kpi-icon rate"><PercentageOutlined /></div>
            </div>
            <div className="revenue-kpi-value" style={{ color: metrics.adjustmentRate > 5 ? '#c2410c' : '#0891b2' }}>
              {metrics.adjustmentRate.toFixed(2)}%
            </div>
          </div>
          <div className="revenue-kpi-sub">
            <span className={`revenue-kpi-badge ${metrics.adjustmentRate > 5 ? 'warning' : 'success'}`}>
              {metrics.adjustmentRate > 5 ? 'Cần lưu ý' : 'An toàn (<5%)'}
            </span>
            <span>Tỷ lệ giảm trừ</span>
          </div>
        </div>
      </div>

      {/* 4. Interactive Chart & Payment Breakdown Section */}
      <Row gutter={[16, 16]}>
        {/* Left 70% Revenue Chart Panel */}
        <Col xs={24} lg={17}>
          <div className="revenue-chart-card">
            <div className="revenue-chart-toolbar">
              <div>
                <h3 className="revenue-chart-title">
                  <BarChartOutlined style={{ color: '#0f766e' }} />
                  Biểu đồ phân tích doanh thu theo thời gian
                </h3>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  Diễn biến doanh thu từ {startDate.format('DD/MM/YYYY')} đến {endDate.format('DD/MM/YYYY')}
                </Text>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div className="revenue-chart-legend">
                  {chartMode === 'trend' ? (
                    <>
                      <span><span className="legend-dot net" /> Doanh thu thuần</span>
                    </>
                  ) : (
                    <>
                      <span><span className="legend-dot gross" /> Hóa đơn gốc</span>
                      <span><span className="legend-dot adjustment" /> Điều chỉnh</span>
                    </>
                  )}
                </div>

                <div className="revenue-chart-mode-switch">
                  <button
                    type="button"
                    className={`revenue-mode-btn ${chartMode === 'trend' ? 'active' : ''}`}
                    onClick={() => setChartMode('trend')}
                  >
                    Xu hướng
                  </button>
                  <button
                    type="button"
                    className={`revenue-mode-btn ${chartMode === 'comparison' ? 'active' : ''}`}
                    onClick={() => setChartMode('comparison')}
                  >
                    So sánh cột
                  </button>
                </div>
              </div>
            </div>

            {/* SVG Interactive Chart */}
            <div className="revenue-svg-wrapper" onMouseLeave={() => setHoveredPoint(null)}>
              <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} preserveAspectRatio="xMidYMid meet">
                <defs>
                  <linearGradient id="revenueAreaGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#059669" stopOpacity="0.35" />
                    <stop offset="100%" stopColor="#059669" stopOpacity="0.02" />
                  </linearGradient>
                  <linearGradient id="grossBarGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#3b82f6" />
                    <stop offset="100%" stopColor="#2563eb" />
                  </linearGradient>
                  <linearGradient id="adjBarGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#f97316" />
                    <stop offset="100%" stopColor="#ea580c" />
                  </linearGradient>
                </defs>

                {/* Horizontal Grid lines and Y Axis values */}
                {[0, 0.25, 0.5, 0.75, 1].map((pct, idx) => {
                  const val = maxRevenueVal * (1 - pct)
                  const y = padTop + pct * (chartHeight - padTop - padBottom)
                  return (
                    <g key={idx}>
                      <line
                        x1={padLeft}
                        y1={y}
                        x2={chartWidth - padRight}
                        y2={y}
                        className="revenue-grid-line dashed"
                      />
                      <text x={padLeft - 8} y={y + 4} textAnchor="end" className="revenue-axis-text">
                        {val >= 1000000 ? `${(val / 1000000).toFixed(val % 1000000 === 0 ? 0 : 1)} tr` : `${val.toLocaleString('vi-VN')} đ`}
                      </text>
                    </g>
                  )
                })}

                {/* Base X Axis line */}
                <line
                  x1={padLeft}
                  y1={chartHeight - padBottom}
                  x2={chartWidth - padRight}
                  y2={chartHeight - padBottom}
                  stroke="#cbd5e1"
                  strokeWidth="1.5"
                />

                {/* MODE 1: AREA & LINE CHART */}
                {chartMode === 'trend' && chartPoints.length > 0 && (
                  <>
                    <path d={areaPathStr} fill="url(#revenueAreaGrad)" />
                    <polyline
                      fill="none"
                      stroke="#059669"
                      strokeWidth="3"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      points={polylineStr}
                    />

                    {/* Chart Points */}
                    {chartPoints.map((pt, idx) => {
                      const isPeak = peakDay && pt.date === peakDay.date && pt.netRevenue > 0
                      const isHovered = hoveredPoint?.date === pt.date

                      return (
                        <g
                          key={idx}
                          onMouseEnter={() => setHoveredPoint(pt)}
                          style={{ cursor: 'pointer' }}
                        >
                          {/* Vertical hover guide */}
                          {isHovered && (
                            <line
                              x1={pt.x}
                              y1={padTop}
                              x2={pt.x}
                              y2={chartHeight - padBottom}
                              stroke="#059669"
                              strokeWidth="1.5"
                              strokeDasharray="3 3"
                            />
                          )}

                          <circle
                            cx={pt.x}
                            cy={pt.y}
                            r={isHovered ? 7 : (isPeak ? 6 : 4.5)}
                            fill={isPeak ? '#fbbf24' : '#059669'}
                            stroke="#ffffff"
                            strokeWidth="2"
                          />

                          {/* Date Label on X Axis */}
                          {(dailyTimeline.length <= 15 || idx % Math.ceil(dailyTimeline.length / 10) === 0) && (
                            <text
                              x={pt.x}
                              y={chartHeight - padBottom + 18}
                              textAnchor="middle"
                              className="revenue-axis-text"
                              fontWeight={isPeak ? '700' : '500'}
                              fill={isPeak ? '#0f766e' : '#64748b'}
                            >
                              {pt.shortDate}
                            </text>
                          )}
                        </g>
                      )
                    })}
                  </>
                )}

                {/* MODE 2: COMPARISON BAR CHART (GROSS vs ADJUSTMENTS) */}
                {chartMode === 'comparison' && chartPoints.length > 0 && (
                  <>
                    {chartPoints.map((pt, idx) => {
                      const barWidth = Math.max(6, Math.min(24, (chartWidth - padLeft - padRight) / (chartPoints.length * 2.5)))
                      const barHeight = chartHeight - padBottom - pt.yGross
                      const isHovered = hoveredPoint?.date === pt.date

                      return (
                        <g
                          key={idx}
                          onMouseEnter={() => setHoveredPoint(pt)}
                          style={{ cursor: 'pointer' }}
                        >
                          {/* Gross Bar */}
                          <rect
                            x={pt.x - barWidth - 1}
                            y={pt.yGross}
                            width={barWidth}
                            height={Math.max(2, barHeight)}
                            rx="3"
                            fill="url(#grossBarGrad)"
                            opacity={isHovered ? 1 : 0.85}
                          />

                          {/* Adjustment Bar */}
                          {pt.adjHeight > 0 && (
                            <rect
                              x={pt.x + 1}
                              y={chartHeight - padBottom - pt.adjHeight}
                              width={barWidth}
                              height={Math.max(2, pt.adjHeight)}
                              rx="3"
                              fill="url(#adjBarGrad)"
                              opacity={isHovered ? 1 : 0.85}
                            />
                          )}

                          {/* Date Label */}
                          {(dailyTimeline.length <= 15 || idx % Math.ceil(dailyTimeline.length / 10) === 0) && (
                            <text
                              x={pt.x}
                              y={chartHeight - padBottom + 18}
                              textAnchor="middle"
                              className="revenue-axis-text"
                            >
                              {pt.shortDate}
                            </text>
                          )}
                        </g>
                      )
                    })}
                  </>
                )}
              </svg>

              {/* Tooltip on Hover */}
              {hoveredPoint && (
                <div
                  className="revenue-chart-tooltip"
                  style={{
                    left: `${Math.min(chartWidth - 180, Math.max(10, hoveredPoint.x - 70))}px`,
                    top: `${Math.max(10, hoveredPoint.y - 80)}px`,
                  }}
                >
                  <div className="revenue-chart-tooltip-title">
                    Ngày {hoveredPoint.displayDate}
                  </div>
                  <div className="revenue-chart-tooltip-row">
                    <span>Doanh thu thuần:</span>
                    <strong style={{ color: '#34d399' }}>{formatMoney(hoveredPoint.netRevenue)}</strong>
                  </div>
                  <div className="revenue-chart-tooltip-row">
                    <span>Hóa đơn gốc:</span>
                    <span>{formatMoney(hoveredPoint.grossRevenue)}</span>
                  </div>
                  {hoveredPoint.adjustments !== 0 && (
                    <div className="revenue-chart-tooltip-row">
                      <span>Điều chỉnh:</span>
                      <span style={{ color: '#fb923c' }}>{formatMoney(hoveredPoint.adjustments)}</span>
                    </div>
                  )}
                  <div className="revenue-chart-tooltip-row">
                    <span>Số giao dịch:</span>
                    <span>{hoveredPoint.totalCount} hóa đơn</span>
                  </div>
                </div>
              )}
            </div>
          </div>
        </Col>

        {/* Right 30% Payment Methods & Insights */}
        <Col xs={24} lg={7}>
          <div className="revenue-payment-card">
            <h3 style={{ fontSize: 15, fontWeight: 700, color: '#0f172a', margin: '0 0 14px 0' }}>
              <WalletOutlined style={{ color: '#2563eb' }} /> Cơ cấu phương thức thanh toán
            </h3>

            <div style={{ flex: 1 }}>
              {paymentMethods.map((m) => (
                <div key={m.key} className="payment-method-row">
                  <div className="payment-method-header">
                    <span>{m.label}</span>
                    <span style={{ fontWeight: 700, color: '#0f172a' }}>{formatMoney(m.total)}</span>
                  </div>
                  <div className="payment-progress-bar">
                    <div
                      className="payment-progress-fill"
                      style={{
                        width: `${m.percentage}%`,
                        background: m.color,
                      }}
                    />
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#64748b' }}>
                    <span>{m.count} giao dịch</span>
                    <strong>{m.percentage.toFixed(1)}%</strong>
                  </div>
                </div>
              ))}
            </div>

            {/* Peak Day Highlight Callout */}
            {peakDay && peakDay.netRevenue > 0 && (
              <div
                style={{
                  background: '#f0fdf4',
                  borderRadius: 10,
                  padding: '12px 14px',
                  border: '1px solid #bbf7d0',
                  marginTop: 12,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#15803d', fontWeight: 700, fontSize: 13 }}>
                  <TrophyOutlined style={{ color: '#eab308', fontSize: 16 }} /> Ngày doanh thu cao nhất
                </div>
                <div style={{ marginTop: 4, fontSize: 12, color: '#166534' }}>
                  <strong>{peakDay.displayDate}</strong> đạt <strong>{formatMoney(peakDay.netRevenue)}</strong> ({peakDay.totalCount} giao dịch)
                </div>
              </div>
            )}
          </div>
        </Col>
      </Row>

      {/* 5. Detailed Breakdown Tabs Section */}
      <div className="revenue-table-card">
        <Tabs
          activeKey={detailTab}
          onChange={setDetailTab}
          style={{ padding: '0 20px' }}
          items={[
            {
              key: 'timeline',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <CalendarOutlined /> Diễn biến doanh thu theo ngày ({dailyTimeline.length} ngày)
                </span>
              ),
            },
            {
              key: 'invoices',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <FileTextOutlined /> Danh sách hóa đơn chi tiết trong kỳ ({periodInvoices.length})
                </span>
              ),
            },
          ]}
        />

        {/* TAB 1: DAILY TIMELINE TABLE */}
        {detailTab === 'timeline' && (
          <Table
            rowKey="date"
            dataSource={dailyTimeline}
            loading={loading}
            pagination={{ pageSize: 10, showSizeChanger: true }}
            size="middle"
            columns={[
              {
                title: 'Ngày báo cáo',
                dataIndex: 'displayDate',
                key: 'displayDate',
                render: (v, row) => (
                  <Space>
                    <strong>{v}</strong>
                    {peakDay && row.date === peakDay.date && row.netRevenue > 0 && (
                      <Tag color="gold" style={{ fontSize: 11 }}>Đỉnh kỳ</Tag>
                    )}
                  </Space>
                ),
              },
              {
                title: 'Hóa đơn gốc',
                dataIndex: 'grossRevenue',
                key: 'grossRevenue',
                align: 'right',
                render: (v, row) => (
                  <div>
                    <span style={{ fontWeight: 600, color: '#2563eb' }}>{formatMoney(v)}</span>
                    <div style={{ fontSize: 11, color: '#94a3b8' }}>{row.originalCount} HD gốc</div>
                  </div>
                ),
              },
              {
                title: 'Khoản điều chỉnh',
                dataIndex: 'adjustments',
                key: 'adjustments',
                align: 'right',
                render: (v, row) => (
                  <div>
                    <span style={{ fontWeight: 600, color: v !== 0 ? '#ea580c' : '#94a3b8' }}>
                      {v !== 0 ? formatMoney(v) : '—'}
                    </span>
                    {row.adjustmentCount > 0 && (
                      <div style={{ fontSize: 11, color: '#ea580c' }}>{row.adjustmentCount} điều chỉnh</div>
                    )}
                  </div>
                ),
              },
              {
                title: 'Doanh thu thuần',
                dataIndex: 'netRevenue',
                key: 'netRevenue',
                align: 'right',
                render: (v) => (
                  <Tag
                    style={{
                      borderRadius: 10,
                      padding: '3px 12px',
                      background: '#ecfdf5',
                      color: '#059669',
                      border: '1px solid #a7f3d0',
                      fontWeight: 700,
                      fontSize: 13,
                    }}
                  >
                    {formatMoney(v)}
                  </Tag>
                ),
              },
              {
                title: 'Tổng giao dịch',
                dataIndex: 'totalCount',
                key: 'totalCount',
                align: 'center',
                render: (v) => <Tag color="blue">{v} hóa đơn</Tag>,
              },
            ]}
          />
        )}

        {/* TAB 2: INVOICES LIST TABLE */}
        {detailTab === 'invoices' && (
          <div>
            <div className="revenue-table-header">
              <Space size="middle" style={{ flexWrap: 'wrap' }}>
                <Input
                  placeholder="Tìm theo mã HD, tên bệnh nhân, lý do..."
                  prefix={<SearchOutlined />}
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  style={{ width: 280, borderRadius: 8 }}
                  allowClear
                />
                <Radio.Group
                  value={invoiceTypeFilter}
                  onChange={(e) => setInvoiceTypeFilter(e.target.value)}
                  buttonStyle="solid"
                >
                  <Radio.Button value="ALL">Tất cả ({periodInvoices.length})</Radio.Button>
                  <Radio.Button value="ORIGINAL">Hóa đơn gốc ({metrics.originalCount})</Radio.Button>
                  <Radio.Button value="ADJUSTMENT">Hóa đơn điều chỉnh ({metrics.adjustmentCount})</Radio.Button>
                </Radio.Group>
              </Space>

              <Text type="secondary" style={{ fontSize: 12 }}>
                Hiển thị {filteredInvoices.length} / {periodInvoices.length} hóa đơn trong kỳ
              </Text>
            </div>

            <Table
              rowKey="id"
              dataSource={filteredInvoices}
              loading={loading}
              pagination={{ pageSize: 10, showSizeChanger: true }}
              size="middle"
              columns={[
                {
                  title: 'Mã hóa đơn',
                  dataIndex: 'invoiceCode',
                  key: 'invoiceCode',
                  render: (v, row) => (
                    <div>
                      <strong>{v || '—'}</strong>
                      {row.originalInvoiceCode && (
                        <div style={{ fontSize: 11, color: '#64748b' }}>
                          Gốc: <code>{row.originalInvoiceCode}</code>
                        </div>
                      )}
                    </div>
                  ),
                },
                {
                  title: 'Bệnh nhân',
                  dataIndex: 'patientName',
                  key: 'patientName',
                  render: (v) => <strong>{v || 'Bệnh nhân'}</strong>,
                },
                {
                  title: 'Loại hóa đơn',
                  dataIndex: 'invoiceType',
                  key: 'invoiceType',
                  render: (v) => (
                    <Tag color={v === 'ADJUSTMENT' ? 'orange' : 'green'} style={{ borderRadius: 6, fontWeight: 600 }}>
                      {v === 'ADJUSTMENT' ? 'Điều chỉnh' : 'Hóa đơn gốc'}
                    </Tag>
                  ),
                },
                {
                  title: 'Phương thức',
                  dataIndex: 'paymentMethodLabel',
                  key: 'paymentMethodLabel',
                  render: (v, row) => v || row.paymentMethod || 'Tiền mặt',
                },
                {
                  title: 'Số tiền',
                  dataIndex: 'totalAmount',
                  key: 'totalAmount',
                  align: 'right',
                  render: (v, row) => {
                    const isAdjustment = row.invoiceType === 'ADJUSTMENT' || Number(v) < 0
                    return (
                      <span
                        style={{
                          fontWeight: 700,
                          fontSize: 14,
                          color: isAdjustment ? '#c2410c' : '#15803d',
                        }}
                      >
                        {isAdjustment && Number(v) > 0 ? `-${formatMoney(v)}` : formatMoney(v)}
                      </span>
                    )
                  },
                },
                {
                  title: 'Lý do điều chỉnh / Ghi chú',
                  dataIndex: 'adjustmentReason',
                  key: 'adjustmentReason',
                  render: (v) => v ? <Text type="secondary" italic>{v}</Text> : '—',
                },
                {
                  title: 'Ngày lập',
                  dataIndex: 'createdAt',
                  key: 'createdAt',
                  render: (v) => v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '—',
                },
              ]}
            />
          </div>
        )}
      </div>
    </div>
  )
}
