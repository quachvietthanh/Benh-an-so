import React, { useMemo, useState } from 'react'
import {
  Alert,
  Card,
  Empty,
  Progress,
  Radio,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd'
import {
  AreaChartOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  FireOutlined,
  LineChartOutlined,
  RiseOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

const { Text } = Typography

const formatCurrency = (val, currency = 'VND') => {
  const num = Number(val || 0)
  return `${num.toLocaleString('vi-VN')} ${currency === 'VND' ? '₫' : currency}`
}

const WEEKDAY_NAMES = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']

export default function VisitReportView({
  timeline = [],
  summary = {},
  loading = false,
}) {
  const [chartType, setChartType] = useState('line')
  const [hoveredDay, setHoveredDay] = useState(null)

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
      maxVisitItem: maxVisitItem || { reportDate: '', visitCount: 0 },
      minVisitItem: minVisitItem || { reportDate: '', visitCount: 0 },
      workloadStatus,
      workloadTitle,
      workloadColor,
      staffingRecommendation,
    }
  }, [summary, timeline])

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
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
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
  )
}
