import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Progress,
  Row,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CrownOutlined,
  LineChartOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import reportApi from '../../api/reportApi'

const { RangePicker } = DatePicker
const { Title, Paragraph, Text } = Typography

export default function DoctorVisitsReportView({
  initialRange = [dayjs().subtract(29, 'day'), dayjs()],
}) {
  const [range, setRange] = useState(initialRange)
  const [reportData, setReportData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  const [dateValidationError, setDateValidationError] = useState('')
  const [hoveredDoctor, setHoveredDoctor] = useState(null)

  const fromStr = useMemo(
    () => (range?.[0] ? range[0].format('YYYY-MM-DD') : ''),
    [range],
  )
  const toStr = useMemo(
    () => (range?.[1] ? range[1].format('YYYY-MM-DD') : ''),
    [range],
  )

  const validateDates = useCallback((start, end) => {
    if (!start || !end) {
      return 'Từ ngày và đến ngày là bắt buộc.'
    }
    if (start.isAfter(end, 'day')) {
      return 'Ngày bắt đầu không được lớn hơn ngày kết thúc.'
    }
    return ''
  }, [])

  const fetchDoctorVisitsReport = useCallback(async (startRange, endRange) => {
    const rStart = startRange || range?.[0]
    const rEnd = endRange || range?.[1]

    const valErr = validateDates(rStart, rEnd)
    if (valErr) {
      setDateValidationError(valErr)
      return
    }
    setDateValidationError('')

    const fStr = rStart.format('YYYY-MM-DD')
    const tStr = rEnd.format('YYYY-MM-DD')

    setLoading(true)
    setErrorMsg('')

    try {
      const response = await reportApi.doctorVisits({
        from: fStr,
        to: tStr,
      })

      setReportData(response.data || { from: fStr, to: tStr, items: [] })
    } catch (err) {
      console.error('Lỗi khi tải báo cáo lượt khám theo bác sĩ:', err)
      const status = err?.response?.status
      if (status === 403) {
        setErrorMsg('Bạn không có quyền xem báo cáo này.')
      } else if (status === 400) {
        setErrorMsg(
          err?.response?.data?.message || 'Khoảng thời gian không hợp lệ.',
        )
      } else {
        setErrorMsg('Không thể tải báo cáo. Vui lòng thử lại.')
      }
      setReportData(null)
    } finally {
      setLoading(false)
    }
  }, [range, validateDates])

  useEffect(() => {
    fetchDoctorVisitsReport()
  }, [fetchDoctorVisitsReport])

  const handleFetchClick = () => {
    fetchDoctorVisitsReport()
  }

  const handleRefresh = () => {
    fetchDoctorVisitsReport()
    message.success('Đã cập nhật dữ liệu mới nhất từ hệ thống!')
  }

  const handlePresetSelect = (presetType) => {
    let newRange = []
    if (presetType === 'today') {
      newRange = [dayjs(), dayjs()]
    } else if (presetType === '7days') {
      newRange = [dayjs().subtract(6, 'day'), dayjs()]
    } else if (presetType === '30days') {
      newRange = [dayjs().subtract(29, 'day'), dayjs()]
    }
    setRange(newRange)
    fetchDoctorVisitsReport(newRange[0], newRange[1])
  }

  const items = useMemo(() => {
    const list = Array.isArray(reportData?.items) ? reportData.items : []
    return [...list].sort(
      (a, b) => Number(b.totalVisits || 0) - Number(a.totalVisits || 0),
    )
  }, [reportData])

  const stats = useMemo(() => {
    const totalVisitsAll = items.reduce(
      (sum, item) => sum + Number(item.totalVisits || 0),
      0,
    )
    const doctorCount = items.length
    const avgVisits =
      doctorCount > 0 ? (totalVisitsAll / doctorCount).toFixed(1) : '0'

    let topDoctor = null
    if (items.length > 0) {
      topDoctor = items.reduce(
        (max, curr) =>
          Number(curr.totalVisits || 0) > Number(max.totalVisits || 0)
            ? curr
            : max,
        items[0],
      )
    }

    return {
      totalVisitsAll,
      doctorCount,
      avgVisits,
      topDoctor,
    }
  }, [items])

  const maxVisitsInChart = useMemo(() => {
    const maxVal = Math.max(
      ...items.map((d) => Number(d.totalVisits || 0)),
      10,
    )
    return Math.ceil(maxVal / 5) * 5
  }, [items])

  const columns = [
    {
      title: 'STT',
      key: 'stt',
      width: 70,
      align: 'center',
      render: (_, record, index) => {
        const rank = record.rank || index + 1
        if (rank === 1) return <Tag color="gold"><CrownOutlined /> #1</Tag>
        if (rank === 2) return <Tag color="cyan">#2</Tag>
        if (rank === 3) return <Tag color="blue">#3</Tag>
        return <Text type="secondary">{rank}</Text>
      },
    },
    {
      title: 'Mã bác sĩ',
      dataIndex: 'doctorCode',
      key: 'doctorCode',
      width: 140,
      render: (val) => (
        <Tag color="geekblue" style={{ fontFamily: 'monospace', fontWeight: 600 }}>
          {val || 'N/A'}
        </Tag>
      ),
    },
    {
      title: 'Bác sĩ',
      dataIndex: 'doctorName',
      key: 'doctorName',
      width: 220,
      render: (val, _record) => (
        <Space>
          <UserOutlined style={{ color: '#1677ff' }} />
          <strong style={{ color: '#0f172a' }}>{val || 'Chưa xác định'}</strong>
        </Space>
      ),
    },
    {
      title: 'Số lượt khám',
      dataIndex: 'totalVisits',
      key: 'totalVisits',
      width: 220,
      sorter: (a, b) => Number(a.totalVisits || 0) - Number(b.totalVisits || 0),
      render: (val) => {
        const count = Number(val || 0)
        const percent =
          stats.totalVisitsAll > 0
            ? Number(((count / stats.totalVisitsAll) * 100).toFixed(1))
            : 0
        return (
          <div style={{ width: 180 }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                marginBottom: 2,
              }}
            >
              <strong
                style={{ fontSize: 14, color: count > 0 ? '#1677ff' : '#94a3b8' }}
              >
                {count} lượt
              </strong>
              <Text type="secondary" style={{ fontSize: 11 }}>
                {percent}%
              </Text>
            </div>
            <Progress
              percent={percent}
              showInfo={false}
              size="small"
              strokeColor={
                count >= 20 ? '#ef4444' : count >= 10 ? '#f59e0b' : '#3b82f6'
              }
            />
          </div>
        )
      },
    },
    {
      title: 'Tỷ lệ đóng góp',
      key: 'percentage',
      width: 150,
      align: 'right',
      render: (_, record) => {
        const count = Number(record.totalVisits || 0)
        const percent =
          stats.totalVisitsAll > 0
            ? ((count / stats.totalVisitsAll) * 100).toFixed(1)
            : '0.0'
        return <strong style={{ color: '#047857' }}>{percent}%</strong>
      },
    },
  ]

  return (
    <Space direction="vertical" size={20} style={{ width: '100%' }}>
      <Card
        style={{
          borderRadius: 14,
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 10px rgba(0,0,0,0.02)',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            flexWrap: 'wrap',
            gap: 16,
          }}
        >
          <div>
            <Title level={3} style={{ margin: 0, color: '#0f172a' }}>
              <UserOutlined style={{ marginRight: 10, color: '#1677ff' }} />
              Báo cáo lượt khám theo bác sĩ
            </Title>
            <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
              Theo dõi số lượt khám của từng bác sĩ trong khoảng thời gian đã chọn.
            </Paragraph>
          </div>

          <Space wrap size="middle">
            <Button
              size="small"
              onClick={() => handlePresetSelect('today')}
              type={
                fromStr === dayjs().format('YYYY-MM-DD') &&
                toStr === dayjs().format('YYYY-MM-DD')
                  ? 'primary'
                  : 'default'
              }
            >
              Hôm nay
            </Button>
            <Button
              size="small"
              onClick={() => handlePresetSelect('7days')}
              type={
                fromStr === dayjs().subtract(6, 'day').format('YYYY-MM-DD') &&
                toStr === dayjs().format('YYYY-MM-DD')
                  ? 'primary'
                  : 'default'
              }
            >
              7 ngày gần nhất
            </Button>
            <Button
              size="small"
              onClick={() => handlePresetSelect('30days')}
              type={
                fromStr === dayjs().subtract(29, 'day').format('YYYY-MM-DD') &&
                toStr === dayjs().format('YYYY-MM-DD')
                  ? 'primary'
                  : 'default'
              }
            >
              30 ngày gần nhất
            </Button>
          </Space>
        </div>

        <div
          style={{
            marginTop: 16,
            paddingTop: 16,
            borderTop: '1px solid #f1f5f9',
            display: 'flex',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 12,
          }}
        >
          <Space align="center" wrap>
            <Text strong style={{ color: '#334155' }}>
              Khoảng thời gian:
            </Text>
            <RangePicker
              value={range}
              format="DD/MM/YYYY"
              onChange={(val) => val && setRange(val)}
              allowClear={false}
              style={{ borderRadius: 8 }}
            />
            <Button
              type="primary"
              icon={<SearchOutlined />}
              loading={loading}
              onClick={handleFetchClick}
              disabled={loading}
              style={{ borderRadius: 8 }}
            >
              Xem báo cáo
            </Button>
            <Button
              icon={<ReloadOutlined />}
              loading={loading}
              onClick={handleRefresh}
              style={{ borderRadius: 8 }}
            >
              Làm mới
            </Button>
          </Space>
        </div>

        {dateValidationError && (
          <Alert
            type="warning"
            showIcon
            message={dateValidationError}
            style={{ marginTop: 12, borderRadius: 8 }}
          />
        )}

        {errorMsg && (
          <Alert
            type="error"
            showIcon
            message={errorMsg}
            action={
              <Button size="small" onClick={handleFetchClick}>
                Thử lại
              </Button>
            }
            style={{ marginTop: 12, borderRadius: 8 }}
          />
        )}
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card
            style={{
              borderRadius: 14,
              border: '1px solid #e2e8f0',
              background: 'linear-gradient(135deg, #eff6ff 0%, #ffffff 100%)',
            }}
          >
            <Statistic
              title={<Text type="secondary">TỔNG LƯỢT KHÁM</Text>}
              value={stats.totalVisitsAll}
              suffix="lượt"
              valueStyle={{ color: '#2563eb', fontWeight: 700 }}
              prefix={<LineChartOutlined style={{ marginRight: 6 }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card
            style={{
              borderRadius: 14,
              border: '1px solid #e2e8f0',
              background: 'linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%)',
            }}
          >
            <Statistic
              title={<Text type="secondary">SỐ BÁC SĨ CÓ LƯỢT KHÁM</Text>}
              value={stats.doctorCount}
              suffix="bác sĩ"
              valueStyle={{ color: '#16a34a', fontWeight: 700 }}
              prefix={<TeamOutlined style={{ marginRight: 6 }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card
            style={{
              borderRadius: 14,
              border: '1px solid #e2e8f0',
              background: 'linear-gradient(135deg, #fff7ed 0%, #ffffff 100%)',
            }}
          >
            <Statistic
              title={<Text type="secondary">TRUNG BÌNH LƯỢT/BÁC SĨ</Text>}
              value={stats.avgVisits}
              suffix="lượt"
              valueStyle={{ color: '#ea580c', fontWeight: 700 }}
              prefix={<BarChartOutlined style={{ marginRight: 6 }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card
            style={{
              borderRadius: 14,
              border: '1px solid #e2e8f0',
              background: 'linear-gradient(135deg, #fefce8 0%, #ffffff 100%)',
            }}
          >
            <div style={{ fontSize: 14, color: '#64748b', marginBottom: 4 }}>
              BÁC SĨ CÓ NHIỀU LƯỢT NHẤT
            </div>
            {stats.topDoctor ? (
              <div>
                <strong
                  style={{
                    fontSize: 16,
                    color: '#ca8a04',
                    display: 'block',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  <CrownOutlined style={{ marginRight: 6 }} />
                  {stats.topDoctor.doctorName || 'N/A'}
                </strong>
                <Text style={{ fontSize: 13, color: '#854d0e', fontWeight: 600 }}>
                  {stats.topDoctor.totalVisits || 0} lượt khám
                </Text>
              </div>
            ) : (
              <Text type="secondary">Chưa có dữ liệu</Text>
            )}
          </Card>
        </Col>
      </Row>

      <Card
        style={{
          borderRadius: 14,
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 10px rgba(0,0,0,0.02)',
        }}
        title={
          <Space>
            <BarChartOutlined style={{ color: '#1677ff' }} />
            <span>Biểu đồ so sánh Lượt khám giữa các Bác sĩ</span>
          </Space>
        }
      >
        <Spin spinning={loading}>
          {!items || items.length === 0 ? (
            <Empty description="Không có dữ liệu lượt khám trong khoảng thời gian đã chọn." />
          ) : (
            <div>
              <div style={{ position: 'relative', width: '100%', overflowX: 'auto' }}>
                <svg
                  viewBox={`0 0 ${Math.max(items.length * 90 + 80, 760)} 220`}
                  style={{ width: '100%', minWidth: 600, maxHeight: 260, display: 'block' }}
                >
                  <defs>
                    <linearGradient id="doctorBarGrad" x1="0" y1="0" x2="0" y2="1">
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
                          x2={Math.max(items.length * 90 + 70, 745)}
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

                  {items.map((doctor, idx) => {
                    const totalWidth = Math.max(items.length * 90, 680)
                    const step = totalWidth / items.length
                    const x = 45 + step * idx + step / 2
                    const count = Number(doctor.totalVisits || 0)
                    const barHeight = (count / maxVisitsInChart) * 155
                    const barY = 180 - barHeight
                    const barWidth = Math.min(step * 0.55, 45)
                    const isHovered =
                      hoveredDoctor?.doctorId === doctor.doctorId ||
                      (hoveredDoctor?.doctorCode &&
                        hoveredDoctor?.doctorCode === doctor.doctorCode)

                    return (
                      <g
                        key={doctor.doctorId || doctor.doctorCode || idx}
                        onMouseEnter={() => setHoveredDoctor(doctor)}
                        onMouseLeave={() => setHoveredDoctor(null)}
                        style={{ cursor: 'pointer' }}
                      >
                        <rect
                          x={x - barWidth / 2}
                          y={barY}
                          width={barWidth}
                          height={Math.max(barHeight, 2)}
                          rx="4"
                          fill={isHovered ? '#1d4ed8' : 'url(#doctorBarGrad)'}
                          opacity={hoveredDoctor && !isHovered ? 0.4 : 1}
                        />
                        {count > 0 && (
                          <text
                            x={x}
                            y={barY - 6}
                            fontSize="11"
                            fontWeight="700"
                            textAnchor="middle"
                            fill={isHovered ? '#1d4ed8' : '#1e40af'}
                          >
                            {count}
                          </text>
                        )}
                        <text
                          x={x}
                          y="202"
                          fontSize="10"
                          textAnchor="middle"
                          fill="#475569"
                          fontWeight={isHovered ? '700' : '500'}
                        >
                          {doctor.doctorName
                            ? doctor.doctorName.length > 12
                              ? doctor.doctorName.substring(0, 10) + '...'
                              : doctor.doctorName
                            : 'Bác sĩ'}
                        </text>
                      </g>
                    )
                  })}
                </svg>
              </div>

              {hoveredDoctor && (
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
                    <UserOutlined style={{ color: '#2563eb' }} />
                    <span>
                      <strong>{hoveredDoctor.doctorName}</strong> ({hoveredDoctor.doctorCode || 'N/A'})
                    </span>
                  </Space>
                  <span>
                    Số lượt khám: <strong style={{ color: '#1677ff', fontSize: 15 }}>{hoveredDoctor.totalVisits || 0} lượt</strong>
                  </span>
                </div>
              )}
            </div>
          )}
        </Spin>
      </Card>

      <Card
        style={{
          borderRadius: 14,
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 10px rgba(0,0,0,0.02)',
        }}
        title={
          <Space>
            <CalendarOutlined />
            <span>Bảng tổng hợp Lượt khám theo từng Bác sĩ ({items.length} bác sĩ)</span>
          </Space>
        }
      >
        <Table
          rowKey={(record) => record.doctorId || record.doctorCode || record.rank}
          columns={columns}
          dataSource={items}
          loading={loading}
          pagination={{
            pageSize: 15,
            showSizeChanger: true,
            pageSizeOptions: ['10', '15', '30', '50'],
            showTotal: (total) => `Tổng cộng ${total} bác sĩ có dữ liệu trong kỳ`,
          }}
          locale={{
            emptyText: (
              <Empty description="Không có dữ liệu lượt khám trong khoảng thời gian đã chọn." />
            ),
          }}
        />
      </Card>
    </Space>
  )
}
