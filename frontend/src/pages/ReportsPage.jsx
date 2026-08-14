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
  AlertOutlined,
  AreaChartOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  DollarCircleOutlined,
  DownloadOutlined,
  FallOutlined,
  FireOutlined,
  InfoCircleOutlined,
  LineChartOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
  PieChartOutlined,
  ReloadOutlined,
  RiseOutlined,
  TeamOutlined,
  UserOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import reportApi from '../api/reportApi'
import { useAuthContext } from '../context/AuthContext'

const { RangePicker } = DatePicker
const { Title, Text, Paragraph } = Typography

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
  const isManager = roles.includes('manager')

  // Mặc định khoảng thời gian: 30 ngày gần nhất
  const [range, setRange] = useState([dayjs().subtract(29, 'day'), dayjs()])
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [chartType, setChartType] = useState('line') // 'line' | 'bar'
  const [hoveredDay, setHoveredDay] = useState(null)

  // Dữ liệu từ Backend API
  const [summaryData, setSummaryData] = useState({
    visitCount: 0,
    revenue: 0,
    currency: 'VND',
  })
  const [timelineData, setTimelineData] = useState([])
  const [loadError, setLoadError] = useState('')

  const fromStr = useMemo(
    () => (range?.[0] ? range[0].format('YYYY-MM-DD') : dayjs().subtract(29, 'day').format('YYYY-MM-DD')),
    [range],
  )
  const toStr = useMemo(
    () => (range?.[1] ? range[1].format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD')),
    [range],
  )

  // Gọi API tổng hợp báo cáo từ Backend
  const loadReports = useCallback(async () => {
    if (!fromStr || !toStr) return

    // Nếu không phải quyền Manager, không gọi API để tránh lỗi 403 Access Denied
    if (!isManager) {
      setLoadError('PERMISSION_DENIED_NOT_MANAGER')
      return
    }

    setLoading(true)
    setLoadError('')
    try {
      const params = { from: fromStr, to: toStr }
      const [summaryRes, timelineRes] = await Promise.all([
        reportApi.summary(params),
        reportApi.timeline(params),
      ])

      if (summaryRes.data) {
        setSummaryData({
          visitCount: Number(summaryRes.data.visitCount || 0),
          revenue: Number(summaryRes.data.revenue || 0),
          currency: summaryRes.data.currency || 'VND',
        })
      }

      if (timelineRes.data && Array.isArray(timelineRes.data.items)) {
        setTimelineData(timelineRes.data.items)
      } else {
        setTimelineData([])
      }
    } catch (err) {
      console.error('Lỗi tải báo cáo vận hành:', err)
      const status = err?.response?.status
      if (status === 403) {
        setLoadError('PERMISSION_DENIED_NOT_MANAGER')
      } else {
        setLoadError(
          err?.response?.data?.message || err?.message || 'Không thể tải báo cáo từ máy chủ.',
        )
      }
    } finally {
      setLoading(false)
    }
  }, [fromStr, toStr, isManager])

  useEffect(() => {
    loadReports()
  }, [loadReports])

  // Phân tích các chỉ số vận hành và tải khám
  const analytics = useMemo(() => {
    const totalVisits = summaryData.visitCount || 0
    const totalDays = Math.max(timelineData.length, 1)
    const avgPerDay = totalVisits / totalDays
    const formattedAvg = avgPerDay.toFixed(1).replace('.', ',')

    let maxVisitItem = null
    let minVisitItem = null

    timelineData.forEach((item) => {
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
      maxVisitItem: maxVisitItem || { date: toStr, visitCount: 0 },
      minVisitItem: minVisitItem || { date: fromStr, visitCount: 0 },
      workloadStatus,
      workloadTitle,
      workloadColor,
      staffingRecommendation,
    }
  }, [summaryData, timelineData, fromStr, toStr])

  // Xuất file CSV báo cáo từ Backend
  const handleExport = async () => {
    setExporting(true)
    try {
      const response = await reportApi.export({ from: fromStr, to: toStr })
      const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `Bao-cao-luot-kham-${fromStr}-${toStr}.csv`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      message.success('Xuất báo cáo lượt khám thành công!')
    } catch (err) {
      message.error(err?.response?.data?.message || 'Không thể xuất file báo cáo.')
    } finally {
      setExporting(false)
    }
  }

  // Chuẩn bị dữ liệu vẽ biểu đồ SVG
  const maxVisitsInChart = useMemo(() => {
    const maxVal = Math.max(...timelineData.map((d) => Number(d.visitCount || 0)), 10)
    return Math.ceil(maxVal / 5) * 5 // Làm tròn bội số của 5
  }, [timelineData])

  const chartPoints = useMemo(() => {
    if (!timelineData.length) return []
    const count = timelineData.length
    const width = 760
    const height = 220
    const paddingLeft = 45
    const paddingRight = 35
    const paddingTop = 25
    const paddingBottom = 40
    const chartW = width - paddingLeft - paddingRight
    const chartH = height - paddingTop - paddingBottom

    return timelineData.map((item, index) => {
      const x = count === 1 ? paddingLeft + chartW / 2 : paddingLeft + (index / (count - 1)) * chartW
      const val = Number(item.visitCount || 0)
      const y = paddingTop + chartH - (val / maxVisitsInChart) * chartH
      return {
        ...item,
        x,
        y,
        val,
        formattedDate: dayjs(item.date).format('DD/MM'),
        fullDate: dayjs(item.date).format('DD/MM/YYYY'),
        weekday: WEEKDAY_NAMES[dayjs(item.date).day()],
      }
    })
  }, [timelineData, maxVisitsInChart])

  // Cột bảng chi tiết từng ngày
  const columns = [
    {
      title: 'Ngày khám',
      dataIndex: 'date',
      key: 'date',
      width: 180,
      render: (val) => {
        const d = dayjs(val)
        const weekday = WEEKDAY_NAMES[d.day()]
        const isWeekend = d.day() === 0 || d.day() === 6
        return (
          <Space>
            <strong style={{ color: isWeekend ? '#e11d48' : '#1e293b' }}>
              {d.format('DD/MM/YYYY')}
            </strong>
            <Tag color={isWeekend ? 'volcano' : 'default'} style={{ fontSize: 11 }}>
              {weekday}
            </Tag>
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
                {summaryData.visitCount > 0
                  ? `${((count / summaryData.visitCount) * 100).toFixed(1)}%`
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
          {formatCurrency(val, summaryData.currency)}
        </span>
      ),
    },
  ]

  return (
    <div style={{ paddingBottom: 40, background: '#f8fafc', minHeight: '100vh', padding: '16px 20px' }}>
      {/* Header trang */}
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
            Báo cáo Lượt khám theo thời gian
          </Title>
          <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
            Tổng hợp tải khám thực tế từ cơ sở dữ liệu để đánh giá khối lượng công việc và điều phối nguồn lực nhân sự.
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

          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadReports}>
            Làm mới
          </Button>

          <Button
            type="primary"
            icon={<DownloadOutlined />}
            loading={exporting}
            onClick={handleExport}
            style={{ borderRadius: 8, background: '#1677ff' }}
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
          action={<Button size="small" onClick={loadReports}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      ) : null}

      {/* KPI Cards: Khối lượng công việc & Tải khám */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small" style={{ borderRadius: 10, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Statistic
              title="Tổng số lượt khám trong kỳ"
              value={summaryData.visitCount}
              suffix="lượt"
              valueStyle={{ color: '#1677ff', fontWeight: 700 }}
              prefix={<CalendarOutlined />}
            />
            <Text type="secondary" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
              Từ {dayjs(fromStr).format('DD/MM/YYYY')} đến {dayjs(toStr).format('DD/MM/YYYY')} ({analytics.totalDays} ngày)
            </Text>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card size="small" style={{ borderRadius: 10, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Statistic
              title="Trung bình lượt khám / ngày"
              value={analytics.formattedAvg}
              suffix="lượt/ngày"
              valueStyle={{ color: '#0284c7', fontWeight: 700 }}
              prefix={<AreaChartOutlined />}
            />
            <Text type="secondary" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
              Khối lượng công việc bình quân mỗi ngày
            </Text>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card size="small" style={{ borderRadius: 10, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Statistic
              title="Ngày cao điểm nhất (Peak Day)"
              value={analytics.maxVisitItem?.visitCount || 0}
              suffix="lượt"
              valueStyle={{ color: '#dc2626', fontWeight: 700 }}
              prefix={<FireOutlined />}
            />
            <Text type="secondary" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
              Ngày {dayjs(analytics.maxVisitItem?.date).format('DD/MM/YYYY')} ({WEEKDAY_NAMES[dayjs(analytics.maxVisitItem?.date).day()]})
            </Text>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card size="small" style={{ borderRadius: 10, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Statistic
              title="Doanh thu phát sinh trong kỳ"
              value={formatCurrency(summaryData.revenue, summaryData.currency)}
              valueStyle={{ color: '#16a34a', fontWeight: 700, fontSize: 20 }}
              prefix={<WalletOutlined />}
            />
            <Text type="secondary" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
              Tổng thu phí dịch vụ & thuốc trong kỳ
            </Text>
          </Card>
        </Col>
      </Row>

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
        style={{ marginBottom: 20, borderRadius: 10 }}
      />

      {/* Biểu đồ Trực quan Lượt khám theo thời gian */}
      <Card
        style={{ borderRadius: 12, marginBottom: 20, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}
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
          {timelineData.length === 0 ? (
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
                    {/* Gradient vùng tô biểu đồ đường */}
                    <linearGradient id="visitAreaGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.35" />
                      <stop offset="100%" stopColor="#3b82f6" stopOpacity="0.0" />
                    </linearGradient>
                    {/* Gradient cột */}
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

                  {/* VẼ DẠNG BIỂU ĐỒ CỘT (BAR CHART) */}
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
                              transition="all 0.2s"
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
                            {/* Nhãn ngày X-axis */}
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

                  {/* VẼ DẠNG BIỂU ĐỒ ĐƯỜNG (LINE & AREA CHART) */}
                  {chartType === 'line' && (
                    <g>
                      {/* Vùng tô Gradient Area */}
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

                      {/* Đường line xanh */}
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

                      {/* Các điểm nút tròn */}
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
                            {/* Nhãn ngày X-axis */}
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

              {/* Thông tin chi tiết khi hover vào 1 ngày trên biểu đồ */}
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
                      Doanh thu ngày: <strong style={{ color: '#16a34a' }}>{formatCurrency(hoveredDay.revenue, summaryData.currency)}</strong>
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
        style={{ borderRadius: 12, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}
        title={
          <Space>
            <CalendarOutlined />
            <span>Bảng thống kê Chi tiết Lượt khám & Doanh thu từng ngày ({timelineData.length} ngày)</span>
          </Space>
        }
      >
        <Table
          rowKey="date"
          columns={columns}
          dataSource={timelineData}
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
    </div>
  )
}

export default ReportsPage
