import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Input,
  Progress,
  Row,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  CheckCircleFilled,
  DollarCircleOutlined,
  ExperimentOutlined,
  FilterOutlined,
  HistoryOutlined,
  PieChartOutlined,
  ReloadOutlined,
  SearchOutlined,
  ShopOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import revenueBreakdownReportApi from '../api/revenueBreakdownReportApi.js'
import { useAuthContext } from '../context/AuthContext'
import {
  formatCurrency,
  formatPercentage,
  getRevenueBreakdownErrorMessage,
  getServiceGroupColor,
  validateDateRange,
} from '../utils/revenueBreakdownReportHelpers.js'

const { RangePicker } = DatePicker
const { Title, Text } = Typography

const CANONICAL_SERVICE_GROUPS = [
  { groupCode: 'EXAMINATION', groupName: 'Khám bệnh', revenue: 0, percentage: 0 },
  { groupCode: 'LAB_TEST', groupName: 'Xét nghiệm', revenue: 0, percentage: 0 },
  { groupCode: 'IMAGING', groupName: 'Chẩn đoán hình ảnh', revenue: 0, percentage: 0 },
  { groupCode: 'OTHER', groupName: 'Dịch vụ khác', revenue: 0, percentage: 0 },
  { groupCode: 'MEDICATION', groupName: 'Thuốc / Dược phẩm', revenue: 0, percentage: 0 },
]

/**
 * Component SVG Donut Chart tương tác cao cho Cơ cấu Doanh thu theo Nhóm dịch vụ
 */
function ServiceGroupDonutChart({ serviceGroups = [], totalNetRevenue = 0 }) {
  const [hoveredGroup, setHoveredGroup] = useState(null)

  // Luôn đảm bảo hiển thị đầy đủ 5 nhóm dịch vụ chuẩn trong legend ngay cả khi doanh thu = 0
  const normalizedGroups = useMemo(() => {
    const map = new Map()
    CANONICAL_SERVICE_GROUPS.forEach((cg) => {
      map.set(cg.groupCode, { ...cg })
    })

    ;(serviceGroups || []).forEach((sg) => {
      if (sg?.groupCode) {
        const existing = map.get(sg.groupCode) || {}
        map.set(sg.groupCode, {
          ...existing,
          ...sg,
          groupName: sg.groupName || existing.groupName || sg.groupCode,
          revenue: Number(sg.revenue || 0),
          percentage: Number(sg.percentage || 0),
        })
      }
    })

    return Array.from(map.values())
  }, [serviceGroups])

  // Danh sách các nhóm có doanh thu dương để vẽ lát cắt Donut
  const positiveGroups = useMemo(() => {
    return normalizedGroups.filter((g) => Number(g.revenue || 0) > 0)
  }, [normalizedGroups])

  const totalPositive = useMemo(() => {
    return positiveGroups.reduce((sum, g) => sum + Number(g.revenue || 0), 0)
  }, [positiveGroups])

  // Tính toán tọa độ các cung tròn SVG
  const slices = useMemo(() => {
    if (!positiveGroups.length || totalPositive <= 0) return []

    const size = 260
    const center = size / 2
    const radius = 100
    const innerRadius = 66

    let cumulativeAngle = -Math.PI / 2 // Bắt đầu từ 12h

    return positiveGroups.map((group) => {
      const fraction = Number(group.revenue || 0) / totalPositive
      const angle = fraction * 2 * Math.PI

      const startAngle = cumulativeAngle
      const endAngle = cumulativeAngle + angle
      cumulativeAngle = endAngle

      const isLargeArc = angle > Math.PI ? 1 : 0

      const x1 = center + radius * Math.cos(startAngle)
      const y1 = center + radius * Math.sin(startAngle)
      const x2 = center + radius * Math.cos(endAngle)
      const y2 = center + radius * Math.sin(endAngle)

      const ix1 = center + innerRadius * Math.cos(endAngle)
      const iy1 = center + innerRadius * Math.sin(endAngle)
      const ix2 = center + innerRadius * Math.cos(startAngle)
      const iy2 = center + innerRadius * Math.sin(startAngle)

      const pathData = [
        `M ${x1} ${y1}`,
        `A ${radius} ${radius} 0 ${isLargeArc} 1 ${x2} ${y2}`,
        `L ${ix1} ${iy1}`,
        `A ${innerRadius} ${innerRadius} 0 ${isLargeArc} 0 ${ix2} ${iy2}`,
        'Z',
      ].join(' ')

      return {
        ...group,
        color: getServiceGroupColor(group.groupCode),
        pathData,
      }
    })
  }, [positiveGroups, totalPositive])

  return (
    <div style={{ maxWidth: 860, margin: '0 auto', width: '100%' }}>
      <Row gutter={[40, 24]} align="middle" justify="center">
        {/* Vùng vẽ Donut Chart SVG (căn giữa) */}
        <Col xs={24} md={10} style={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
          <div style={{ position: 'relative', width: 260, height: 260 }}>
            <svg
              viewBox="0 0 260 260"
              style={{ width: '100%', height: '100%', overflow: 'visible', display: 'block' }}
            >
              {/* Vòng nền khi không có dữ liệu phát sinh */}
              {slices.length === 0 ? (
                <circle
                  cx="130"
                  cy="130"
                  r="83"
                  fill="none"
                  stroke="#e2e8f0"
                  strokeWidth="34"
                />
              ) : (
                slices.map((slice) => {
                  const isHovered = hoveredGroup?.groupCode === slice.groupCode
                  return (
                    <path
                      key={slice.groupCode}
                      d={slice.pathData}
                      fill={slice.color}
                      opacity={hoveredGroup && !isHovered ? 0.45 : 1}
                      style={{
                        cursor: 'pointer',
                        transition: 'transform 0.2s ease, opacity 0.2s ease',
                        transformOrigin: '130px 130px',
                        transform: isHovered ? 'scale(1.04)' : 'scale(1)',
                      }}
                      onMouseEnter={() => setHoveredGroup(slice)}
                      onMouseLeave={() => setHoveredGroup(null)}
                    />
                  )
                })
              )}
            </svg>

            {/* Vùng thông tin trung tâm Donut */}
            <div
              style={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                textAlign: 'center',
                pointerEvents: 'none',
                width: 124,
              }}
            >
              <Text
                type="secondary"
                style={{
                  fontSize: 11.5,
                  fontWeight: 600,
                  display: 'block',
                  lineHeight: 1.2,
                  textTransform: 'uppercase',
                  letterSpacing: 0.5,
                }}
              >
                {hoveredGroup ? hoveredGroup.groupName : 'Doanh thu thuần'}
              </Text>
              <div
                style={{
                  fontSize: hoveredGroup ? 15 : 16,
                  fontWeight: 700,
                  color: hoveredGroup ? hoveredGroup.color : '#0f172a',
                  marginTop: 3,
                  lineHeight: 1.2,
                }}
              >
                {hoveredGroup
                  ? formatCurrency(hoveredGroup.revenue)
                  : formatCurrency(totalNetRevenue)}
              </div>
              {hoveredGroup && (
                <Tag
                  color={hoveredGroup.color}
                  style={{
                    marginTop: 4,
                    fontSize: 11,
                    borderRadius: 10,
                    padding: '0 6px',
                    fontWeight: 600,
                  }}
                >
                  {formatPercentage(hoveredGroup.percentage)}
                </Tag>
              )}
            </div>
          </div>
        </Col>

        {/* Chú giải Legend đầy đủ 5 nhóm (kể cả nhóm có doanh thu = 0) */}
        <Col xs={24} md={14}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {normalizedGroups.map((group) => {
              const isHovered = hoveredGroup?.groupCode === group.groupCode
              const color = getServiceGroupColor(group.groupCode)
              const rev = Number(group.revenue || 0)

              return (
                <div
                  key={group.groupCode}
                  onMouseEnter={() => setHoveredGroup(group)}
                  onMouseLeave={() => setHoveredGroup(null)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    borderRadius: 8,
                    background: isHovered ? '#f1f5f9' : '#f8fafc',
                    border: `1px solid ${isHovered ? color : '#e2e8f0'}`,
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span
                      style={{
                        width: 12,
                        height: 12,
                        borderRadius: '50%',
                        background: color,
                        display: 'inline-block',
                        flexShrink: 0,
                      }}
                    />
                    <span style={{ fontSize: 13, fontWeight: 500, color: '#334155' }}>
                      {group.groupName}
                    </span>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <span
                      style={{
                        fontSize: 13.5,
                        fontWeight: 600,
                        color: rev > 0 ? '#0f172a' : '#94a3b8',
                        marginRight: 10,
                      }}
                    >
                      {formatCurrency(rev)}
                    </span>
                    <Tag
                      style={{
                        margin: 0,
                        fontWeight: 600,
                        fontSize: 11.5,
                        borderRadius: 6,
                        background: rev > 0 ? `${color}15` : '#f1f5f9',
                        color: rev > 0 ? color : '#94a3b8',
                        border: 'none',
                        padding: '1px 8px',
                      }}
                    >
                      {formatPercentage(group.percentage)}
                    </Tag>
                  </div>
                </div>
              )
            })}
          </div>
        </Col>
      </Row>
    </div>
  )
}

/**
 * Trang chính: Báo cáo doanh thu theo dịch vụ và theo bác sĩ (NCL-08-CN-009)
 */
export default function RevenueBreakdownReportPage() {
  const { user } = useAuthContext()

  // Phân quyền: CHỈ role MANAGER (hoặc clinic_manager), nghiêm cấm ADMIN
  const userRoles = useMemo(() => {
    return (user?.roles || [user?.role || '']).map((r) =>
      String(r || '').toLowerCase().replace(/^role_/, '')
    )
  }, [user])

  const isAdmin = userRoles.includes('admin')
  const isManager = userRoles.includes('manager') || userRoles.includes('clinic_manager')
  const isAuthorized = isManager && !isAdmin

  // State bộ lọc (Mặc định 30 ngày gần nhất)
  const [dateRange, setDateRange] = useState(() => [
    dayjs().subtract(29, 'day'),
    dayjs(),
  ])

  // State dữ liệu báo cáo từ Backend
  const [reportData, setReportData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [searchDoctorText, setSearchDoctorText] = useState('')

  // Gọi API lấy báo cáo doanh thu bóc tách
  const fetchReport = useCallback(async (currentRange) => {
    if (!currentRange || !currentRange[0] || !currentRange[1]) {
      message.warning('Vui lòng chọn đầy đủ khoảng thời gian.')
      return
    }

    const validation = validateDateRange(currentRange[0], currentRange[1])
    if (!validation.valid) {
      message.error(validation.error)
      return
    }

    setLoading(true)
    try {
      const response = await revenueBreakdownReportApi.getRevenueBreakdown({
        from: validation.from,
        to: validation.to,
      })
      setReportData(response.data)
    } catch (err) {
      console.error('Lỗi tải báo cáo doanh thu bóc tách:', err)
      const errorMsg = getRevenueBreakdownErrorMessage(err)
      message.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }, [])

  // Nạp dữ liệu lần đầu khi component mount
  useEffect(() => {
    if (isAuthorized) {
      fetchReport(dateRange)
    }
  }, [isAuthorized, fetchReport])

  // Xử lý chọn nhanh khoảng thời gian
  const handleQuickPreset = (presetType) => {
    let newRange = [dayjs().subtract(29, 'day'), dayjs()]
    if (presetType === 'TODAY') {
      newRange = [dayjs().startOf('day'), dayjs().endOf('day')]
    } else if (presetType === '7DAYS') {
      newRange = [dayjs().subtract(6, 'day').startOf('day'), dayjs().endOf('day')]
    } else if (presetType === '30DAYS') {
      newRange = [dayjs().subtract(29, 'day').startOf('day'), dayjs().endOf('day')]
    } else if (presetType === 'THIS_MONTH') {
      newRange = [dayjs().startOf('month'), dayjs().endOf('month')]
    }
    setDateRange(newRange)
    fetchReport(newRange)
  }

  // Lọc danh sách bác sĩ client-side theo từ khóa tìm kiếm
  const filteredDoctors = useMemo(() => {
    if (!reportData?.doctors) return []
    const keyword = searchDoctorText.trim().toLowerCase()
    if (!keyword) return reportData.doctors

    return reportData.doctors.filter((doc) => {
      const name = String(doc.doctorName || '').toLowerCase()
      const code = String(doc.doctorCode || '').toLowerCase()
      return name.includes(keyword) || code.includes(keyword)
    })
  }, [reportData?.doctors, searchDoctorText])

  // Cấu hình các cột của Bảng doanh thu theo Bác sĩ
  const doctorColumns = useMemo(
    () => [
      {
        title: 'Bác sĩ',
        key: 'doctor',
        width: 250,
        fixed: 'left',
        sorter: (a, b) => (a.doctorName || '').localeCompare(b.doctorName || '', 'vi'),
        render: (_, record) => {
          const isUnassigned =
            record.doctorId === null ||
            record.doctorCode === 'UNASSIGNED' ||
            record.doctorName === 'Chưa phân bổ bác sĩ'

          if (isUnassigned) {
            return (
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <Avatar
                  style={{
                    backgroundColor: '#e2e8f0',
                    color: '#64748b',
                    fontSize: 14,
                  }}
                  icon={<TeamOutlined />}
                />
                <div>
                  <Text
                    italic
                    style={{
                      color: '#64748b',
                      fontSize: 13.5,
                      fontWeight: 500,
                      display: 'block',
                    }}
                  >
                    Chưa phân bổ bác sĩ
                  </Text>
                  <Tag
                    style={{
                      borderRadius: 4,
                      fontSize: 11,
                      background: '#f1f5f9',
                      color: '#475569',
                      border: '1px solid #cbd5e1',
                    }}
                  >
                    UNASSIGNED
                  </Tag>
                </div>
              </div>
            )
          }

          return (
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <Avatar
                style={{
                  backgroundColor: '#2563eb',
                  color: '#ffffff',
                  fontSize: 13,
                  fontWeight: 600,
                }}
              >
                {record.doctorName ? record.doctorName.charAt(0).toUpperCase() : 'BS'}
              </Avatar>
              <div style={{ minWidth: 0 }}>
                <Text
                  strong
                  style={{
                    fontSize: 13.5,
                    color: '#0f172a',
                    display: 'block',
                  }}
                >
                  {record.doctorName || 'Bác sĩ'}
                </Text>
                {record.doctorCode && (
                  <Tag
                    color="blue"
                    style={{
                      borderRadius: 4,
                      fontSize: 11,
                      padding: '0 6px',
                      fontWeight: 500,
                    }}
                  >
                    {record.doctorCode}
                  </Tag>
                )}
              </div>
            </div>
          )
        },
      },
      {
        title: 'Doanh thu khám',
        dataIndex: 'examRevenue',
        key: 'examRevenue',
        align: 'right',
        width: 150,
        sorter: (a, b) => Number(a.examRevenue || 0) - Number(b.examRevenue || 0),
        render: (val) => (
          <span style={{ fontSize: 13, color: '#334155', fontWeight: 500 }}>
            {formatCurrency(val)}
          </span>
        ),
      },
      {
        title: 'Doanh thu CLS',
        dataIndex: 'clinicalServiceRevenue',
        key: 'clinicalServiceRevenue',
        align: 'right',
        width: 150,
        sorter: (a, b) =>
          Number(a.clinicalServiceRevenue || 0) - Number(b.clinicalServiceRevenue || 0),
        render: (val) => (
          <span style={{ fontSize: 13, color: '#334155', fontWeight: 500 }}>
            {formatCurrency(val)}
          </span>
        ),
      },
      {
        title: 'Doanh thu thuốc',
        dataIndex: 'medicationRevenue',
        key: 'medicationRevenue',
        align: 'right',
        width: 150,
        sorter: (a, b) =>
          Number(a.medicationRevenue || 0) - Number(b.medicationRevenue || 0),
        render: (val) => (
          <span style={{ fontSize: 13, color: '#334155', fontWeight: 500 }}>
            {formatCurrency(val)}
          </span>
        ),
      },
      {
        title: 'Điều chỉnh / Hoàn tiền',
        dataIndex: 'adjustmentRevenue',
        key: 'adjustmentRevenue',
        align: 'right',
        width: 160,
        sorter: (a, b) =>
          Number(a.adjustmentRevenue || 0) - Number(b.adjustmentRevenue || 0),
        render: (val) => {
          const num = Number(val || 0)
          const isNegative = num < 0
          return (
            <span
              style={{
                fontSize: 13,
                fontWeight: 600,
                color: isNegative ? '#dc2626' : '#64748b',
              }}
            >
              {formatCurrency(val)}
            </span>
          )
        },
      },
      {
        title: 'Tổng doanh thu',
        dataIndex: 'totalRevenue',
        key: 'totalRevenue',
        align: 'right',
        width: 170,
        sorter: (a, b) => Number(a.totalRevenue || 0) - Number(b.totalRevenue || 0),
        render: (val) => (
          <span
            style={{
              fontSize: 14,
              fontWeight: 700,
              color: '#2563eb',
            }}
          >
            {formatCurrency(val)}
          </span>
        ),
      },
      {
        title: 'Tỷ trọng',
        dataIndex: 'percentage',
        key: 'percentage',
        width: 170,
        sorter: (a, b) => Number(a.percentage || 0) - Number(b.percentage || 0),
        render: (pct) => {
          const num = Number(pct || 0)
          return (
            <div style={{ width: '100%' }}>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  marginBottom: 2,
                }}
              >
                <Text strong style={{ fontSize: 12, color: '#334155' }}>
                  {formatPercentage(pct)}
                </Text>
              </div>
              <Progress
                percent={Math.max(0, num)}
                showInfo={false}
                size="small"
                strokeColor={num < 0 ? '#dc2626' : '#2563eb'}
                style={{ margin: 0 }}
              />
            </div>
          )
        },
      },
    ],
    []
  )

  // Kiểm tra quyền truy cập
  if (!isAuthorized) {
    return (
      <div style={{ paddingBottom: 32, fontFamily: 'Inter, "Segoe UI", Roboto, Arial, sans-serif' }}>
        <div style={{ marginBottom: 24 }}>
          <Title level={1} style={{ margin: 0, fontSize: 24, fontWeight: 700, color: '#0f172a' }}>
            <DollarCircleOutlined style={{ marginRight: 10, color: '#2563eb' }} />
            Báo cáo Doanh thu theo Dịch vụ & Bác sĩ
          </Title>
        </div>
        <Card
          style={{
            borderRadius: 12,
            textAlign: 'center',
            padding: 24,
          }}
        >
          <Empty description="Tính năng này chỉ dành cho Quản lý phòng khám (MANAGER). Tài khoản của bạn không được phân quyền xem báo cáo này." />
        </Card>
      </div>
    )
  }

  const hasData =
    (reportData?.serviceGroups && reportData.serviceGroups.length > 0) ||
    (reportData?.doctors && reportData.doctors.length > 0)

  return (
    <div style={{ paddingBottom: 32, fontFamily: 'Inter, "Segoe UI", Roboto, Arial, sans-serif' }}>
      {/* Responsive Grid CSS cho 5 thẻ KPI */}
      <style>{`
        .revenue-kpi-grid {
          display: grid;
          grid-template-columns: repeat(5, minmax(0, 1fr));
          gap: 16px;
          margin-bottom: 24px;
        }
        @media (max-width: 1200px) {
          .revenue-kpi-grid {
            grid-template-columns: repeat(3, minmax(0, 1fr));
          }
        }
        @media (max-width: 768px) {
          .revenue-kpi-grid {
            grid-template-columns: repeat(2, minmax(0, 1fr));
          }
        }
        @media (max-width: 480px) {
          .revenue-kpi-grid {
            grid-template-columns: 1fr;
          }
        }
      `}</style>

      {/* Header trang (H1: 24px bold, Subtitle: 14px regular) */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
          marginBottom: 24,
        }}
      >
        <div>
          <Title
            level={1}
            style={{
              margin: 0,
              fontSize: 24,
              fontWeight: 700,
              color: '#0f172a',
              display: 'flex',
              alignItems: 'center',
              gap: 10,
            }}
          >
            <DollarCircleOutlined style={{ color: '#2563eb', fontSize: 26 }} />
            Báo cáo Doanh thu theo Dịch vụ & Bác sĩ
          </Title>
        </div>
      </div>

      {/* Thanh bộ lọc (Filter Bar) - Padding 24px, Margin-bottom 24px */}
      <Card
        bordered={false}
        style={{
          marginBottom: 24,
          borderRadius: 12,
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
          border: '1px solid #e2e8f0',
          background: '#ffffff',
        }}
        styles={{ body: { padding: 24 } }}
      >
        <Row gutter={[16, 16]} align="bottom">
          <Col xs={24} md={12} lg={13}>
            <Text
              strong
              style={{
                display: 'block',
                fontSize: 13,
                marginBottom: 6,
                color: '#475569',
              }}
            >
              <CalendarOutlined style={{ marginRight: 6, color: '#2563eb' }} />
              Chọn kỳ báo cáo (Tối đa 366 ngày):
            </Text>
            <RangePicker
              value={dateRange}
              onChange={(val) => setDateRange(val)}
              format="DD/MM/YYYY"
              allowClear={false}
              disabledDate={(current) => current && current > dayjs().endOf('day')}
              style={{ width: '100%', height: 38, borderRadius: 8 }}
            />
          </Col>

          <Col xs={24} md={12} lg={11}>
            <Space size={10} wrap>
              <Button
                type="primary"
                icon={<SearchOutlined />}
                loading={loading}
                disabled={!dateRange || !dateRange[0] || !dateRange[1]}
                onClick={() => fetchReport(dateRange)}
                style={{
                  height: 38,
                  borderRadius: 8,
                  paddingLeft: 20,
                  paddingRight: 20,
                  fontWeight: 600,
                  backgroundColor: '#2563eb',
                }}
              >
                Xem báo cáo
              </Button>
              <Button
                icon={<ReloadOutlined />}
                loading={loading}
                onClick={() => fetchReport(dateRange)}
                style={{
                  height: 38,
                  borderRadius: 8,
                  paddingLeft: 16,
                  paddingRight: 16,
                  fontWeight: 500,
                  borderColor: '#cbd5e1',
                  color: '#334155',
                }}
              >
                Làm mới
              </Button>
            </Space>
          </Col>
        </Row>

        {/* Nút chọn nhanh kỳ báo cáo (Quick Presets) - Cùng chiều cao 28px, khoảng cách đều 8px, căn cùng baseline */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 12,
            marginTop: 16,
            paddingTop: 14,
            borderTop: '1px dashed #e2e8f0',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            <span
              style={{
                fontSize: 13,
                fontWeight: 500,
                color: '#64748b',
                display: 'flex',
                alignItems: 'center',
                gap: 4,
              }}
            >
              <FilterOutlined style={{ color: '#2563eb' }} /> Chọn nhanh:
            </span>
            <Button
              size="small"
              style={{ height: 28, borderRadius: 6, fontSize: 12, padding: '0 10px' }}
              onClick={() => handleQuickPreset('TODAY')}
            >
              Hôm nay
            </Button>
            <Button
              size="small"
              style={{ height: 28, borderRadius: 6, fontSize: 12, padding: '0 10px' }}
              onClick={() => handleQuickPreset('7DAYS')}
            >
              7 ngày qua
            </Button>
            <Button
              size="small"
              style={{ height: 28, borderRadius: 6, fontSize: 12, padding: '0 10px' }}
              onClick={() => handleQuickPreset('30DAYS')}
            >
              30 ngày qua
            </Button>
            <Button
              size="small"
              style={{ height: 28, borderRadius: 6, fontSize: 12, padding: '0 10px' }}
              onClick={() => handleQuickPreset('THIS_MONTH')}
            >
              Tháng này
            </Button>
          </div>

          {reportData && (
            <div style={{ fontSize: 13, color: '#64748b', display: 'flex', alignItems: 'center', gap: 4 }}>
              <span>Kỳ đang xem:</span>
              <Text strong style={{ color: '#2563eb' }}>{reportData.from}</Text>
              <span>đến</span>
              <Text strong style={{ color: '#2563eb' }}>{reportData.to}</Text>
            </div>
          )}
        </div>
      </Card>

      {/* Nội dung kết quả */}
      <Spin spinning={loading} tip="Đang kết xuất báo cáo doanh thu bóc tách...">
        {/* Khối 5 Thẻ KPI - Chia đều 5 cột, cùng chiều cao, cùng padding 20px, số liệu 22px bold đồng nhất */}
        <div className="revenue-kpi-grid">
          {/* 1. Tổng doanh thu thuần */}
          <div
            style={{
              borderRadius: 12,
              boxShadow: '0 2px 6px rgba(37, 99, 235, 0.06)',
              border: '1.5px solid #bfdbfe',
              background: '#f8faff',
              padding: '20px',
              minHeight: 125,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              height: '100%',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text style={{ fontSize: 13, fontWeight: 600, color: '#1e40af' }}>
                Tổng doanh thu thuần
              </Text>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 8,
                  background: '#eff6ff',
                  color: '#2563eb',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 16,
                }}
              >
                <DollarCircleOutlined />
              </div>
            </div>

            <div style={{ fontSize: 22, fontWeight: 700, color: '#1d4ed8', margin: '10px 0 12px 0', lineHeight: 1.2 }}>
              {formatCurrency(reportData?.totalNetRevenue ?? 0)}
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Tag color="blue" style={{ borderRadius: 4, fontSize: 11, fontWeight: 600, margin: 0, padding: '0 6px' }}>
                DOANH THU THUẦN
              </Tag>
              <Tooltip title="Số liệu khớp với Báo cáo doanh thu tổng hợp trong cùng kỳ">
                <CheckCircleFilled style={{ color: '#059669', fontSize: 14 }} />
              </Tooltip>
            </div>
          </div>

          {/* 2. Khám bệnh */}
          <div
            style={{
              borderRadius: 12,
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
              border: '1px solid #e2e8f0',
              background: '#ffffff',
              padding: '20px',
              minHeight: 125,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              height: '100%',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text style={{ fontSize: 13, fontWeight: 600, color: '#64748b' }}>
                Khám bệnh
              </Text>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 8,
                  background: '#eff6ff',
                  color: '#2563eb',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 16,
                }}
              >
                <UserOutlined />
              </div>
            </div>

            <div style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', margin: '10px 0 12px 0', lineHeight: 1.2 }}>
              {formatCurrency(reportData?.totalExamRevenue ?? 0)}
            </div>

            <div>
              <Tag color="blue" style={{ borderRadius: 4, fontSize: 11, fontWeight: 600, margin: 0, padding: '0 6px' }}>
                EXAMINATION
              </Tag>
            </div>
          </div>

          {/* 3. Dịch vụ CLS */}
          <div
            style={{
              borderRadius: 12,
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
              border: '1px solid #e2e8f0',
              background: '#ffffff',
              padding: '20px',
              minHeight: 125,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              height: '100%',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text style={{ fontSize: 13, fontWeight: 600, color: '#64748b' }}>
                Dịch vụ CLS
              </Text>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 8,
                  background: '#ecfdf5',
                  color: '#059669',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 16,
                }}
              >
                <ExperimentOutlined />
              </div>
            </div>

            <div style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', margin: '10px 0 12px 0', lineHeight: 1.2 }}>
              {formatCurrency(reportData?.totalClinicalServiceRevenue ?? 0)}
            </div>

            <div>
              <Tag color="green" style={{ borderRadius: 4, fontSize: 11, fontWeight: 600, margin: 0, padding: '0 6px' }}>
                LAB / IMAGING
              </Tag>
            </div>
          </div>

          {/* 4. Thuốc / Dược phẩm */}
          <div
            style={{
              borderRadius: 12,
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
              border: '1px solid #e2e8f0',
              background: '#ffffff',
              padding: '20px',
              minHeight: 125,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              height: '100%',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text style={{ fontSize: 13, fontWeight: 600, color: '#64748b' }}>
                Thuốc / Dược phẩm
              </Text>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 8,
                  background: '#f5f3ff',
                  color: '#7c3aed',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 16,
                }}
              >
                <ShopOutlined />
              </div>
            </div>

            <div style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', margin: '10px 0 12px 0', lineHeight: 1.2 }}>
              {formatCurrency(reportData?.totalMedicationRevenue ?? 0)}
            </div>

            <div>
              <Tag color="purple" style={{ borderRadius: 4, fontSize: 11, fontWeight: 600, margin: 0, padding: '0 6px' }}>
                MEDICATION
              </Tag>
            </div>
          </div>

          {/* 5. Điều chỉnh / Hoàn tiền */}
          <div
            style={{
              borderRadius: 12,
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
              border: '1px solid #e2e8f0',
              background: '#ffffff',
              padding: '20px',
              minHeight: 125,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              height: '100%',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text style={{ fontSize: 13, fontWeight: 600, color: '#64748b' }}>
                Điều chỉnh / Hoàn tiền
              </Text>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 8,
                  background: '#fef2f2',
                  color: '#dc2626',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 16,
                }}
              >
                <HistoryOutlined />
              </div>
            </div>

            <div
              style={{
                fontSize: 22,
                fontWeight: 700,
                color: Number(reportData?.totalAdjustmentRevenue || 0) < 0 ? '#dc2626' : '#0f172a',
                margin: '10px 0 12px 0',
                lineHeight: 1.2,
              }}
            >
              {formatCurrency(reportData?.totalAdjustmentRevenue ?? 0)}
            </div>

            <div>
              <Tag
                color={Number(reportData?.totalAdjustmentRevenue || 0) < 0 ? 'error' : 'default'}
                style={{ borderRadius: 4, fontSize: 11, fontWeight: 600, margin: 0, padding: '0 6px' }}
              >
                ADJUSTMENT
              </Tag>
            </div>
          </div>
        </div>

        {/* Khối Biểu đồ cơ cấu dịch vụ (Donut Chart) - H2: 16px semi-bold, Icon 18px, Padding 24px, Margin-bottom 24px */}
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <PieChartOutlined style={{ color: '#2563eb', fontSize: 18 }} />
              <span style={{ fontWeight: 600, fontSize: 16, color: '#0f172a' }}>
                Cơ cấu Doanh thu theo Nhóm dịch vụ
              </span>
            </div>
          }
          bordered={false}
          style={{
            borderRadius: 12,
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            border: '1px solid #e2e8f0',
            marginBottom: 24,
          }}
          styles={{ body: { padding: 24 } }}
        >
          <ServiceGroupDonutChart
            serviceGroups={reportData?.serviceGroups || []}
            totalNetRevenue={reportData?.totalNetRevenue || 0}
          />
        </Card>

        {/* Khối Bảng doanh thu theo Bác sĩ - H2: 16px semi-bold, Icon 18px, Padding 24px, Margin-bottom 24px */}
        <Card
          title={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <TeamOutlined style={{ color: '#2563eb', fontSize: 18 }} />
                <span style={{ fontWeight: 600, fontSize: 16, color: '#0f172a' }}>
                  Bảng phân bổ Doanh thu theo Bác sĩ
                </span>
                <Badge
                  count={filteredDoctors.length}
                  overflowCount={999}
                  style={{ backgroundColor: '#eff6ff', color: '#2563eb', fontWeight: 600, marginLeft: 4 }}
                />
              </div>

              <Input
                placeholder="Tìm theo tên hoặc mã bác sĩ..."
                prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
                value={searchDoctorText}
                onChange={(e) => setSearchDoctorText(e.target.value)}
                style={{ width: 320, borderRadius: 8, height: 36 }}
                allowClear
              />
            </div>
          }
          bordered={false}
          style={{
            borderRadius: 12,
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            border: '1px solid #e2e8f0',
            marginBottom: 24,
          }}
          styles={{ body: { padding: 24 } }}
        >
          {!hasData && !loading ? (
            <div style={{ padding: '40px 0', textAlign: 'center' }}>
              <Empty description="Không có dữ liệu doanh thu trong khoảng thời gian đã chọn." />
            </div>
          ) : (
            <Table
              columns={doctorColumns}
              dataSource={filteredDoctors}
              rowKey={(r) => r.doctorId || r.doctorCode || 'UNASSIGNED'}
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                pageSizeOptions: ['10', '20', '50'],
                showTotal: (total) => `Tổng cộng ${total} bác sĩ / dòng doanh thu`,
              }}
              scroll={{ x: 1200 }}
              size="middle"
            />
          )}
        </Card>
      </Spin>
    </div>
  )
}
