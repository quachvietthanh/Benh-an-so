import React, { useMemo } from 'react'
import { Card, Empty, Radio, Space, Spin, Tag } from 'antd'
import {
  AreaChartOutlined,
  BarChartOutlined,
  CalendarOutlined,
  LineChartOutlined,
  TrophyOutlined,
} from '@ant-design/icons'
import { formatMoney } from '../../../utils/revenueHelpers'

export default function RevenueInteractiveChart({
  dailyTimeline = [],
  metrics = {},
  loading = false,
  chartMode = 'trend',
  setChartMode,
  hoveredPoint,
  setHoveredPoint,
}) {
  const maxRevenueVal = useMemo(() => {
    const maxDayNet = Math.max(...dailyTimeline.map((d) => d.grossRevenue || d.netRevenue || 0), 100000)
    return Math.ceil(maxDayNet / 500000) * 500000
  }, [dailyTimeline])

  const chartPoints = useMemo(() => {
    if (!dailyTimeline.length) return []
    const count = dailyTimeline.length
    const width = 760
    const height = 220
    const paddingLeft = 60
    const paddingRight = 30
    const paddingTop = 20
    const paddingBottom = 35
    const chartW = width - paddingLeft - paddingRight
    const chartH = height - paddingTop - paddingBottom

    return dailyTimeline.map((item, index) => {
      const x = count === 1 ? paddingLeft + chartW / 2 : paddingLeft + (index / (count - 1)) * chartW
      const netY = paddingTop + chartH - (Math.max(0, item.netRevenue) / (maxRevenueVal || 1)) * chartH
      const grossY = paddingTop + chartH - (Math.max(0, item.grossRevenue) / (maxRevenueVal || 1)) * chartH
      return {
        ...item,
        x,
        netY,
        grossY,
      }
    })
  }, [dailyTimeline, maxRevenueVal])

  return (
    <Card
      style={{ borderRadius: 14, border: '1px solid #f1f5f9', marginBottom: 20 }}
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <Space>
            <LineChartOutlined style={{ color: '#059669', fontSize: 18 }} />
            <span style={{ fontWeight: 700, fontSize: 15 }}>Biểu đồ Diễn biến Doanh thu theo thời gian</span>
          </Space>
          <Space wrap size="small">
            {metrics.peakDay?.date && (
              <Tag icon={<TrophyOutlined />} color="gold" style={{ borderRadius: 6, fontWeight: 600 }}>
                Đỉnh doanh thu: {metrics.peakDay.reportDate} ({formatMoney(metrics.peakDay.revenue)})
              </Tag>
            )}
            <Radio.Group
              value={chartMode}
              onChange={(e) => setChartMode(e.target.value)}
              optionType="button"
              buttonStyle="solid"
              size="small"
            >
              <Radio.Button value="trend">
                <AreaChartOutlined /> Xu hướng thuần
              </Radio.Button>
              <Radio.Button value="comparison">
                <BarChartOutlined /> Đối chiếu Gốc/Thuần
              </Radio.Button>
            </Radio.Group>
          </Space>
        </div>
      }
    >
      <Spin spinning={loading}>
        {dailyTimeline.length === 0 ? (
          <Empty description="Không có dữ liệu hóa đơn trong khoảng thời gian đã chọn" />
        ) : (
          <div>
            <div style={{ position: 'relative', width: '100%', overflowX: 'auto' }}>
              <svg viewBox="0 0 760 220" style={{ width: '100%', minWidth: 600, maxHeight: 260, display: 'block' }}>
                <defs>
                  <linearGradient id="revenueAreaGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#10b981" stopOpacity="0.38" />
                    <stop offset="100%" stopColor="#10b981" stopOpacity="0.0" />
                  </linearGradient>
                  <linearGradient id="netBarGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#059669" />
                    <stop offset="100%" stopColor="#34d399" />
                  </linearGradient>
                  <linearGradient id="grossBarGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#2563eb" />
                    <stop offset="100%" stopColor="#93c5fd" />
                  </linearGradient>
                </defs>

                {[0, 0.25, 0.5, 0.75, 1].map((ratio, idx) => {
                  const val = Math.round(maxRevenueVal * ratio)
                  const y = 185 - ratio * 165
                  return (
                    <g key={idx}>
                      <line x1="60" y1={y} x2="730" y2={y} stroke="#f1f5f9" strokeDasharray="3 3" />
                      <text x="10" y={y + 4} fontSize="10" fill="#94a3b8" fontWeight="500">
                        {val >= 1000000 ? `${(val / 1000000).toFixed(1)}Tr` : val >= 1000 ? `${(val / 1000).toFixed(0)}k` : val}
                      </text>
                    </g>
                  )
                })}

                {metrics.averageDaily > 0 && (
                  <g>
                    <line
                      x1="60"
                      y1={185 - (metrics.averageDaily / (maxRevenueVal || 1)) * 165}
                      x2="730"
                      y2={185 - (metrics.averageDaily / (maxRevenueVal || 1)) * 165}
                      stroke="#8b5cf6"
                      strokeWidth="1.5"
                      strokeDasharray="5 3"
                    />
                    <text
                      x="680"
                      y={185 - (metrics.averageDaily / (maxRevenueVal || 1)) * 165 - 4}
                      fontSize="9"
                      fill="#8b5cf6"
                      fontWeight="700"
                    >
                      TB ({formatMoney(metrics.averageDaily)})
                    </text>
                  </g>
                )}

                {chartMode === 'trend' && (
                  <g>
                    {chartPoints.length > 1 && (
                      <polygon
                        fill="url(#revenueAreaGrad)"
                        points={`
                          ${chartPoints[0].x},185
                          ${chartPoints.map((p) => `${p.x},${p.netY}`).join(' ')}
                          ${chartPoints[chartPoints.length - 1].x},185
                        `}
                      />
                    )}

                    {chartPoints.length > 1 && (
                      <polyline
                        fill="none"
                        stroke="#10b981"
                        strokeWidth="3"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        points={chartPoints.map((p) => `${p.x},${p.netY}`).join(' ')}
                      />
                    )}

                    {chartPoints.map((item, idx) => {
                      const isHovered = hoveredPoint?.date === item.date
                      return (
                        <g
                          key={idx}
                          onMouseEnter={() => setHoveredPoint(item)}
                          onMouseLeave={() => setHoveredPoint(null)}
                          style={{ cursor: 'pointer' }}
                        >
                          <circle
                            cx={item.x}
                            cy={item.netY}
                            r={isHovered ? 7 : 4}
                            fill={isHovered ? '#047857' : '#10b981'}
                            stroke="#ffffff"
                            strokeWidth="2.5"
                          />
                          {(chartPoints.length <= 14 || idx % Math.ceil(chartPoints.length / 10) === 0) && (
                            <text x={item.x} y="205" fontSize="10" textAnchor="middle" fill="#64748b">
                              {item.formattedDate}
                            </text>
                          )}
                        </g>
                      )
                    })}
                  </g>
                )}

                {chartMode === 'comparison' && (
                  <g>
                    {chartPoints.map((item, idx) => {
                      const groupWidth = Math.max(Math.min(480 / chartPoints.length, 36), 8)
                      const barW = groupWidth / 2 - 1
                      const grossH = Math.max((item.grossRevenue / (maxRevenueVal || 1)) * 165, 2)
                      const netH = Math.max((Math.max(0, item.netRevenue) / (maxRevenueVal || 1)) * 165, 2)
                      const isHovered = hoveredPoint?.date === item.date

                      return (
                        <g
                          key={idx}
                          onMouseEnter={() => setHoveredPoint(item)}
                          onMouseLeave={() => setHoveredPoint(null)}
                          style={{ cursor: 'pointer' }}
                        >
                          <rect
                            x={item.x - groupWidth / 2}
                            y={185 - grossH}
                            width={barW}
                            height={grossH}
                            rx="3"
                            fill="url(#grossBarGrad)"
                            opacity={hoveredPoint && !isHovered ? 0.35 : 0.85}
                          />
                          <rect
                            x={item.x - groupWidth / 2 + barW + 2}
                            y={185 - netH}
                            width={barW}
                            height={netH}
                            rx="3"
                            fill="url(#netBarGrad)"
                            opacity={hoveredPoint && !isHovered ? 0.35 : 1}
                          />
                          {(chartPoints.length <= 14 || idx % Math.ceil(chartPoints.length / 10) === 0) && (
                            <text x={item.x} y="205" fontSize="10" textAnchor="middle" fill="#64748b">
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

            {hoveredPoint && (
              <div
                style={{
                  background: '#f0fdf4',
                  border: '1px solid #bbf7d0',
                  borderRadius: 10,
                  padding: '10px 18px',
                  marginTop: 14,
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: 12,
                }}
              >
                <Space>
                  <CalendarOutlined style={{ color: '#059669', fontSize: 16 }} />
                  <span>
                    <strong>{hoveredPoint.fullDate}</strong> ({hoveredPoint.weekday})
                  </span>
                </Space>
                <Space size="large" wrap>
                  <span>
                    Doanh thu gốc:{' '}
                    <strong style={{ color: '#2563eb' }}>{formatMoney(hoveredPoint.grossRevenue)}</strong>
                  </span>
                  <span>
                    Điều chỉnh:{' '}
                    <strong style={{ color: '#ea580c' }}>
                      {hoveredPoint.adjustmentTotal !== 0 ? formatMoney(hoveredPoint.adjustmentTotal) : '0 đ'}
                    </strong>
                  </span>
                  <span>
                    Doanh thu thuần:{' '}
                    <strong style={{ color: '#059669', fontSize: 15 }}>
                      {formatMoney(hoveredPoint.netRevenue)}
                    </strong>
                  </span>
                  <Tag color="blue">{hoveredPoint.invoiceCount} hóa đơn</Tag>
                </Space>
              </div>
            )}
          </div>
        )}
      </Spin>
    </Card>
  )
}
