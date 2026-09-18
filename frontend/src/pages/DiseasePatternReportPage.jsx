import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
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
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  BarChartOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  DownloadOutlined,
  FileTextOutlined,
  FilterOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  SearchOutlined,
  TrophyOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import diseasePatternReportApi from '../api/diseasePatternReportApi.js'
import reportApi from '../api/reportApi.js'
import userApi from '../api/userApi.js'
import { useAuthContext } from '../context/AuthContext'
import { SYSTEM_DOCTORS } from '../utils/overdueMedicalRecordHelpers.js'
import {
  downloadCsvBlob,
  extractFilenameFromHeader,
  formatPercentage,
  getExportErrorMessage,
  validateDateRange,
} from '../utils/diseasePatternReportHelpers.js'

const { RangePicker } = DatePicker
const { Title, Text } = Typography

// Rank color palettes for Top 3 and bar visualization
const RANK_COLORS = {
  1: { badge: '#d97706', bg: '#fef3c7', text: '#b45309', label: 'Top 1' },
  2: { badge: '#2563eb', bg: '#dbeafe', text: '#1d4ed8', label: 'Top 2' },
  3: { badge: '#059669', bg: '#d1fae5', text: '#047857', label: 'Top 3' },
}

const BAR_COLORS = [
  '#2563eb',
  '#059669',
  '#d97706',
  '#db2777',
  '#7c3aed',
  '#0891b2',
  '#ea580c',
  '#4f46e5',
]

function DiseasePatternReportPage() {
  const { user } = useAuthContext()

  // Phân quyền nghiêm ngặt: CHỈ role MANAGER (hoặc clinic_manager), loại trừ hoàn toàn ADMIN và các role khác
  const userRoles = useMemo(() => {
    return (user?.roles || [user?.role || '']).map((r) =>
      String(r || '').toLowerCase().replace(/^role_/, '')
    )
  }, [user])

  const isAdmin = userRoles.includes('admin')
  const isManager = userRoles.includes('manager') || userRoles.includes('clinic_manager')
  const isAuthorized = isManager && !isAdmin

  // State bộ lọc (Mặc định nạp khoảng thời gian bao gồm tháng trước và tháng này để sẵn dữ liệu lâm sàng)
  const [dateRange, setDateRange] = useState(() => [
    dayjs().subtract(1, 'month').startOf('month'),
    dayjs(),
  ])
  const [selectedDoctorId, setSelectedDoctorId] = useState(undefined)
  const [doctors, setDoctors] = useState([])
  const [doctorsLoading, setDoctorsLoading] = useState(false)

  // State dữ liệu báo cáo
  const [reportData, setReportData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [hasSearched, setHasSearched] = useState(false)
  const isExportingRef = useRef(false)

  // Tải danh sách bác sĩ để lọc (kết hợp SYSTEM_DOCTORS, userApi và reportApi để luôn có dữ liệu cho Manager)
  useEffect(() => {
    if (!isAuthorized) return

    let isMounted = true
    setDoctorsLoading(true)

    const loadDoctors = async () => {
      const doctorMap = new Map()

      // 1. Nạp danh sách bác sĩ hệ thống mặc định (đảm bảo luôn hiển thị ngay cả khi userApi bị 403 Forbidden)
      if (Array.isArray(SYSTEM_DOCTORS)) {
        SYSTEM_DOCTORS.forEach((d) => {
          doctorMap.set(String(d.id).toLowerCase(), {
            id: d.id,
            fullName: d.fullName || d.name || d.username,
            username: d.username,
          })
        })
      }

      // 2. Thử nạp từ userApi nếu tài khoản có quyền USER_READ
      try {
        const res = await userApi.getDoctors()
        const list = Array.isArray(res?.data)
          ? res.data
          : Array.isArray(res?.data?.data)
          ? res.data.data
          : []
        list.forEach((doc) => {
          if (doc?.id) {
            const key = String(doc.id).toLowerCase()
            const existing = doctorMap.get(key)
            doctorMap.set(key, {
              id: doc.id,
              fullName: doc.fullName || doc.name || existing?.fullName || doc.username,
              username: doc.username || existing?.username,
            })
          }
        })
      } catch (err) {
        // Tài khoản Quản lý (MANAGER) không có quyền USER_READ, sử dụng danh sách fallback
      }

      // 3. Thử tải thêm danh sách bác sĩ có lượt khám từ reportApi
      try {
        const resVisits = await reportApi.doctorVisits({
          from: dayjs().subtract(365, 'day').format('YYYY-MM-DD'),
          to: dayjs().format('YYYY-MM-DD'),
        })
        const items = resVisits?.data?.items || []
        items.forEach((item) => {
          if (item?.doctorId) {
            const key = String(item.doctorId).toLowerCase()
            const existing = doctorMap.get(key)
            doctorMap.set(key, {
              id: item.doctorId,
              fullName: existing?.fullName || item.doctorName || item.doctorCode,
              username: existing?.username || item.doctorCode,
            })
          }
        })
      } catch (err) {
        // Bỏ qua nếu lỗi
      }

      if (isMounted) {
        setDoctors(Array.from(doctorMap.values()))
        setDoctorsLoading(false)
      }
    }

    loadDoctors()

    return () => {
      isMounted = false
    }
  }, [isAuthorized])

  // Lấy dữ liệu báo cáo
  const handleFetchReport = useCallback(
    async (overrideRange, overrideDoctorId) => {
      const activeRange = overrideRange || dateRange
      const activeDoctorId =
        overrideDoctorId !== undefined
          ? overrideDoctorId === null
            ? undefined
            : overrideDoctorId
          : selectedDoctorId

      if (!activeRange || !activeRange[0] || !activeRange[1]) {
        message.warning('Vui lòng chọn khoảng thời gian báo cáo.')
        return
      }

      const validation = validateDateRange(activeRange[0], activeRange[1])
      if (!validation.valid) {
        message.error(validation.error)
        return
      }

      setLoading(true)
      setHasSearched(true)

      try {
        const params = {
          from: validation.from,
          to: validation.to,
          ...(activeDoctorId ? { doctorId: activeDoctorId } : {}),
        }

        const res = await diseasePatternReportApi.getReport(params)
        setReportData(res.data)
      } catch (err) {
        const status = err?.response?.status
        const errorCode = err?.response?.data?.code || ''

        if (status === 400 && errorCode === 'DATE_RANGE_TOO_LONG') {
          message.error('Khoảng thời gian báo cáo không được vượt quá 366 ngày.')
        } else if (status === 400) {
          message.error('Khoảng thời gian không hợp lệ. Vui lòng kiểm tra lại.')
        } else if (status === 403) {
          message.error('Bạn không có quyền xem báo cáo này.')
        } else {
          message.error('Không thể tải báo cáo. Vui lòng thử lại.')
        }
        setReportData(null)
      } finally {
        setLoading(false)
      }
    },
    [dateRange, selectedDoctorId]
  )

  // Tự động tải báo cáo lần đầu khi vào trang
  useEffect(() => {
    if (isAuthorized) {
      handleFetchReport()
    }
  }, [isAuthorized, handleFetchReport])

  // Thay đổi bác sĩ lọc
  const handleDoctorChange = (val) => {
    setSelectedDoctorId(val)
    handleFetchReport(undefined, val)
  }

  // Xóa bộ lọc bác sĩ
  const handleClearDoctorFilter = () => {
    setSelectedDoctorId(undefined)
    handleFetchReport(undefined, null)
  }

  // Xử lý chọn nhanh thời gian (Quick Presets)
  const handleQuickPreset = (preset) => {
    let newRange = null
    const today = dayjs()

    switch (preset) {
      case '7DAYS':
        newRange = [today.subtract(6, 'day'), today]
        break
      case '30DAYS':
        newRange = [today.subtract(29, 'day'), today]
        break
      case 'THIS_MONTH':
        newRange = [today.startOf('month'), today]
        break
      case 'LAST_MONTH':
        newRange = [
          today.subtract(1, 'month').startOf('month'),
          today.subtract(1, 'month').endOf('month'),
        ]
        break
      case 'THIS_YEAR':
        newRange = [today.startOf('year'), today]
        break
      default:
        newRange = [today.subtract(1, 'month').startOf('month'), today]
    }

    setDateRange(newRange)
    handleFetchReport(newRange, selectedDoctorId)
  }

  // Xử lý xuất file CSV
  const handleExport = async () => {
    if (isExportingRef.current || exporting) return
    if (!reportData || reportData.totalDiagnoses === 0) {
      message.warning('Không có dữ liệu để xuất file.')
      return
    }

    const validation = validateDateRange(reportData.from, reportData.to)
    if (!validation.valid) {
      message.error(validation.error)
      return
    }

    isExportingRef.current = true
    setExporting(true)

    try {
      const params = {
        from: validation.from,
        to: validation.to,
        ...(reportData.doctorId ? { doctorId: reportData.doctorId } : {}),
      }

      const res = await diseasePatternReportApi.exportReport(params)
      const disposition = res.headers?.['content-disposition'] || ''
      const defaultFilename = reportData.doctorId
        ? `disease-pattern-report-${validation.from}-to-${validation.to}-doctor-${reportData.doctorId}.csv`
        : `disease-pattern-report-${validation.from}-to-${validation.to}.csv`

      const filename = extractFilenameFromHeader(disposition, defaultFilename)
      downloadCsvBlob(res.data, filename)
      message.success('Xuất file báo cáo thành công!')
    } catch (err) {
      const errorMsg = await getExportErrorMessage(err)
      if (
        errorMsg.includes('chưa có dữ liệu') ||
        errorMsg.includes('Không thể xuất tệp')
      ) {
        message.warning(errorMsg)
      } else {
        message.error(errorMsg)
      }
    } finally {
      isExportingRef.current = false
      setExporting(false)
    }
  }

  // Danh sách bác sĩ cho Select
  const doctorOptions = useMemo(() => {
    const opts = [{ value: undefined, label: 'Tất cả bác sĩ' }]
    doctors.forEach((doc) => {
      let displayName =
        doc.fullName || doc.name || doc.username || `Bác sĩ (${String(doc.id).slice(0, 8)})`
      if (!displayName.startsWith('BS')) {
        displayName = `BS. ${displayName}`
      }
      opts.push({
        value: doc.id,
        label: displayName,
      })
    })
    return opts
  }, [doctors])

  // Top 5 bệnh nhân phổ biến nhất để vẽ biểu đồ trực quan
  const topDiseases = useMemo(() => {
    if (!reportData?.items) return []
    return reportData.items.slice(0, 5)
  }, [reportData])

  // Cấu hình các cột của Bảng xếp hạng
  const columns = useMemo(
    () => [
      {
        title: 'Hạng',
        dataIndex: 'rank',
        key: 'rank',
        width: 100,
        align: 'center',
        render: (rank) => {
          if (RANK_COLORS[rank]) {
            const conf = RANK_COLORS[rank]
            return (
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 4,
                  padding: '3px 10px',
                  borderRadius: 20,
                  fontSize: 12,
                  fontWeight: 700,
                  background: conf.bg,
                  color: conf.text,
                  border: `1px solid ${conf.badge}`,
                }}
              >
                <TrophyOutlined />
                #{rank}
              </span>
            )
          }
          return (
            <span
              style={{
                display: 'inline-block',
                width: 24,
                height: 24,
                lineHeight: '24px',
                borderRadius: '50%',
                background: '#f1f5f9',
                color: '#475569',
                fontSize: 12,
                fontWeight: 600,
              }}
            >
              {rank}
            </span>
          )
        },
      },
      {
        title: 'Mã bệnh (ICD-10)',
        dataIndex: 'diseaseCode',
        key: 'diseaseCode',
        width: 150,
        render: (code) => (
          <Tag
            color="blue"
            style={{
              fontWeight: 700,
              fontSize: 13,
              borderRadius: 6,
              padding: '2px 8px',
            }}
          >
            {code}
          </Tag>
        ),
      },
      {
        title: 'Tên bệnh',
        dataIndex: 'diseaseName',
        key: 'diseaseName',
        ellipsis: true,
        render: (text) => (
          <Text strong style={{ color: '#0f172a', fontSize: 13.5 }}>
            {text}
          </Text>
        ),
      },
      {
        title: 'Nhóm bệnh',
        dataIndex: 'diseaseGroup',
        key: 'diseaseGroup',
        width: 220,
        ellipsis: true,
        render: (group) => (
          <Tag
            style={{
              borderRadius: 6,
              background: '#f8fafc',
              border: '1px solid #e2e8f0',
              color: '#334155',
            }}
          >
            {group || 'Chưa phân nhóm'}
          </Tag>
        ),
      },
      {
        title: 'Số lượt chẩn đoán',
        dataIndex: 'diagnosisCount',
        key: 'diagnosisCount',
        width: 170,
        align: 'right',
        render: (count) => (
          <Text
            style={{
              fontWeight: 700,
              color: '#2563eb',
              fontSize: 14.5,
            }}
          >
            {Number(count).toLocaleString('vi-VN')}
          </Text>
        ),
      },
      {
        title: 'Tỷ lệ cơ cấu',
        dataIndex: 'percentage',
        key: 'percentage',
        width: 220,
        render: (pct) => (
          <div style={{ width: '100%' }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                marginBottom: 2,
              }}
            >
              <Text strong style={{ fontSize: 12, color: '#475569' }}>
                {formatPercentage(pct)}
              </Text>
            </div>
            <Progress
              percent={pct}
              showInfo={false}
              size="small"
              strokeColor={{
                from: '#2563eb',
                to: '#059669',
              }}
              style={{ margin: 0 }}
            />
          </div>
        ),
      },
    ],
    []
  )

  // Chặn nếu người dùng không đủ quyền
  if (!isAuthorized) {
    return (
      <div style={{ padding: 24, maxWidth: 800, margin: '40px auto' }}>
        <Alert
          message="Từ chối truy cập"
          description="Tính năng Báo cáo mô hình bệnh tật theo mã bệnh chỉ dành riêng cho vai trò Quản lý phòng khám. Bạn không có quyền xem trang này."
          type="error"
          showIcon
          style={{ borderRadius: 10 }}
        />
      </div>
    )
  }

  const isDataAvailable = reportData && reportData.totalDiagnoses > 0

  return (
    <div style={{ padding: '20px 24px 40px', maxWidth: 1400, margin: '0 auto' }}>
      {/* Header trang với Typography & Badge cao cấp */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
          marginBottom: 18,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div
            style={{
              width: 44,
              height: 44,
              borderRadius: 10,
              background: 'linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '1px solid #bfdbfe',
              boxShadow: '0 2px 4px rgba(37, 99, 235, 0.08)',
            }}
          >
            <BarChartOutlined style={{ fontSize: 24, color: '#2563eb' }} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <Title level={4} style={{ margin: 0, fontWeight: 700, color: '#0f172a' }}>
                Báo cáo mô hình bệnh tật theo mã bệnh
              </Title>
              <Tag
                color="blue"
                style={{
                  borderRadius: 12,
                  fontSize: 11,
                  padding: '1px 8px',
                  fontWeight: 600,
                  margin: 0,
                }}
              >
                Quản lý phòng khám
              </Tag>
            </div>
          </div>
        </div>

        {/* Nút thao tác nhanh trên header */}
        <Space size={10}>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => handleFetchReport()}
            loading={loading}
            style={{ borderRadius: 8, height: 36 }}
          >
            Làm mới
          </Button>
          <Tooltip
            title={
              !isDataAvailable
                ? 'Chỉ có thể xuất tệp khi báo cáo có dữ liệu chẩn đoán'
                : 'Tải xuống tệp CSV (chuẩn UTF-8 mở bằng Excel)'
            }
          >
            <Button
              icon={<DownloadOutlined />}
              onClick={handleExport}
              loading={exporting}
              disabled={!isDataAvailable || loading || exporting}
              type="primary"
              style={{
                height: 36,
                borderRadius: 8,
                fontWeight: 600,
                background: isDataAvailable
                  ? 'linear-gradient(135deg, #10b981 0%, #059669 100%)'
                  : undefined,
                borderColor: isDataAvailable ? '#059669' : undefined,
                boxShadow: isDataAvailable
                  ? '0 2px 6px rgba(16, 185, 129, 0.25)'
                  : undefined,
              }}
            >
              Xuất file Excel / CSV
            </Button>
          </Tooltip>
        </Space>
      </div>

      {/* Thanh bộ lọc (Filter Bar) tinh gọn, đẹp mắt */}
      <Card
        bordered={false}
        style={{
          marginBottom: 20,
          borderRadius: 12,
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
          border: '1px solid #f1f5f9',
          background: '#ffffff',
        }}
        styles={{ body: { padding: '16px 20px' } }}
      >
        <Row gutter={[16, 12]} align="middle">
          {/* Khoảng thời gian */}
          <Col xs={24} sm={12} md={9} lg={8}>
            <Text strong style={{ display: 'block', fontSize: 12.5, marginBottom: 4, color: '#475569' }}>
              <CalendarOutlined style={{ marginRight: 6, color: '#2563eb' }} />
              Khoảng thời gian (Tối đa 366 ngày):
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

          {/* Bác sĩ chẩn đoán */}
          <Col xs={24} sm={12} md={9} lg={8}>
            <Text strong style={{ display: 'block', fontSize: 12.5, marginBottom: 4, color: '#475569' }}>
              <UserOutlined style={{ marginRight: 6, color: '#2563eb' }} />
              Bác sĩ chẩn đoán:
            </Text>
            <Select
              value={selectedDoctorId}
              onChange={handleDoctorChange}
              options={doctorOptions}
              loading={doctorsLoading}
              placeholder="Tất cả bác sĩ"
              style={{ width: '100%', height: 38 }}
              showSearch
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              allowClear
            />
          </Col>

          {/* Nút Xem báo cáo */}
          <Col xs={24} sm={24} md={6} lg={8} style={{ display: 'flex', alignItems: 'flex-end', paddingTop: { md: 20 } }}>
            <div style={{ width: '100%', display: 'flex', gap: 10, alignItems: 'center' }}>
              <Button
                type="primary"
                icon={<SearchOutlined />}
                onClick={() => handleFetchReport()}
                loading={loading}
                disabled={!dateRange || !dateRange[0] || !dateRange[1]}
                style={{
                  height: 38,
                  borderRadius: 8,
                  padding: '0 24px',
                  fontWeight: 600,
                  background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
                  boxShadow: '0 2px 4px rgba(37, 99, 235, 0.2)',
                  minWidth: 130,
                }}
              >
                Xem báo cáo
              </Button>

              {selectedDoctorId && (
                <Button
                  size="small"
                  type="text"
                  onClick={handleClearDoctorFilter}
                  style={{ color: '#64748b', fontSize: 12 }}
                >
                  Xóa lọc bác sĩ
                </Button>
              )}
            </div>
          </Col>
        </Row>

        {/* Dòng chọn nhanh khoảng thời gian (Quick Presets) */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            marginTop: 14,
            paddingTop: 12,
            borderTop: '1px dashed #e2e8f0',
            flexWrap: 'wrap',
          }}
        >
          <span style={{ fontSize: 12, fontWeight: 600, color: '#64748b', display: 'flex', alignItems: 'center', gap: 4 }}>
            <FilterOutlined style={{ color: '#2563eb' }} /> Chọn nhanh:
          </span>
          <Button size="small" style={{ borderRadius: 6, fontSize: 12 }} onClick={() => handleQuickPreset('30DAYS')}>
            30 ngày qua
          </Button>
          <Button size="small" style={{ borderRadius: 6, fontSize: 12 }} onClick={() => handleQuickPreset('THIS_MONTH')}>
            Tháng này
          </Button>
          <Button size="small" style={{ borderRadius: 6, fontSize: 12 }} onClick={() => handleQuickPreset('LAST_MONTH')}>
            Tháng trước (Tháng 8/2026)
          </Button>
          <Button size="small" style={{ borderRadius: 6, fontSize: 12 }} onClick={() => handleQuickPreset('THIS_YEAR')}>
            Năm 2026
          </Button>
          <Button size="small" style={{ borderRadius: 6, fontSize: 12 }} onClick={() => handleQuickPreset('7DAYS')}>
            7 ngày qua
          </Button>
        </div>
      </Card>

      {/* Nội dung kết quả */}
      {loading ? (
        <Card
          bordered={false}
          style={{
            textAlign: 'center',
            padding: '60px 20px',
            borderRadius: 12,
            boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
          }}
        >
          <Spin size="large" tip="Đang tổng hợp dữ liệu báo cáo mô hình bệnh tật..." />
        </Card>
      ) : isDataAvailable ? (
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          {/* Thẻ tóm tắt thông tin tổng quan (Summary KPI Cards) */}
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={6}>
              <Card
                bordered={false}
                style={{
                  borderRadius: 10,
                  boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                  border: '1px solid #e0e7ff',
                  background: 'linear-gradient(135deg, #ffffff 0%, #f8faff 100%)',
                }}
                styles={{ body: { padding: '16px 20px' } }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary" style={{ fontSize: 12.5, fontWeight: 500 }}>
                      Tổng số lượt chẩn đoán
                    </Text>
                    <div style={{ fontSize: 24, fontWeight: 800, color: '#2563eb', marginTop: 4 }}>
                      {Number(reportData.totalDiagnoses).toLocaleString('vi-VN')}
                    </div>
                  </div>
                  <div
                    style={{
                      width: 42,
                      height: 42,
                      borderRadius: 10,
                      background: '#eff6ff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#2563eb',
                      fontSize: 20,
                    }}
                  >
                    <MedicineBoxOutlined />
                  </div>
                </div>
              </Card>
            </Col>

            <Col xs={24} sm={12} md={6}>
              <Card
                bordered={false}
                style={{
                  borderRadius: 10,
                  boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                  border: '1px solid #f1f5f9',
                }}
                styles={{ body: { padding: '16px 20px' } }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary" style={{ fontSize: 12.5, fontWeight: 500 }}>
                      Kỳ báo cáo
                    </Text>
                    <div style={{ fontSize: 15, fontWeight: 700, color: '#0f172a', marginTop: 6 }}>
                      {dayjs(reportData.from).format('DD/MM/YYYY')} - {dayjs(reportData.to).format('DD/MM/YYYY')}
                    </div>
                  </div>
                  <div
                    style={{
                      width: 42,
                      height: 42,
                      borderRadius: 10,
                      background: '#f1f5f9',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#64748b',
                      fontSize: 20,
                    }}
                  >
                    <CalendarOutlined />
                  </div>
                </div>
              </Card>
            </Col>

            <Col xs={24} sm={12} md={6}>
              <Card
                bordered={false}
                style={{
                  borderRadius: 10,
                  boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                  border: '1px solid #f1f5f9',
                }}
                styles={{ body: { padding: '16px 20px' } }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary" style={{ fontSize: 12.5, fontWeight: 500 }}>
                      Bác sĩ chẩn đoán
                    </Text>
                    <div
                      style={{
                        fontSize: 15,
                        fontWeight: 700,
                        color: '#0f172a',
                        marginTop: 6,
                        maxWidth: 160,
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                      title={reportData.doctorName || 'Tất cả bác sĩ'}
                    >
                      {reportData.doctorName || 'Tất cả bác sĩ'}
                    </div>
                  </div>
                  <div
                    style={{
                      width: 42,
                      height: 42,
                      borderRadius: 10,
                      background: '#ecfdf5',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#059669',
                      fontSize: 20,
                    }}
                  >
                    <UserOutlined />
                  </div>
                </div>
              </Card>
            </Col>

            <Col xs={24} sm={12} md={6}>
              <Card
                bordered={false}
                style={{
                  borderRadius: 10,
                  boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                  border: '1px solid #f1f5f9',
                }}
                styles={{ body: { padding: '16px 20px' } }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary" style={{ fontSize: 12.5, fontWeight: 500 }}>
                      Thời điểm kết xuất
                    </Text>
                    <div style={{ fontSize: 14, fontWeight: 600, color: '#334155', marginTop: 6 }}>
                      {reportData.generatedAt
                        ? dayjs(reportData.generatedAt).format('DD/MM/YYYY HH:mm:ss')
                        : '-'}
                    </div>
                  </div>
                  <div
                    style={{
                      width: 42,
                      height: 42,
                      borderRadius: 10,
                      background: '#f8fafc',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#64748b',
                      fontSize: 20,
                    }}
                  >
                    <ClockCircleOutlined />
                  </div>
                </div>
              </Card>
            </Col>
          </Row>

          {/* Biểu đồ trực quan Top 5 bệnh tật phổ biến */}
          {topDiseases.length > 0 && (
            <Card
              title={
                <Space size={8}>
                  <TrophyOutlined style={{ color: '#d97706', fontSize: 18 }} />
                  <span style={{ fontSize: 15, fontWeight: 700, color: '#0f172a' }}>
                    Cơ cấu Top 5 bệnh tật phổ biến nhất trong kỳ
                  </span>
                </Space>
              }
              bordered={false}
              style={{
                borderRadius: 12,
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                border: '1px solid #f1f5f9',
              }}
              styles={{ body: { padding: '18px 20px' } }}
            >
              <Row gutter={[16, 14]}>
                {topDiseases.map((item, idx) => (
                  <Col xs={24} sm={24} md={12} lg={12} key={item.catalogId || item.diseaseCode}>
                    <div
                      style={{
                        padding: '14px 16px',
                        background: '#f8fafc',
                        borderRadius: 8,
                        border: '1px solid #f1f5f9',
                        borderLeft: `4px solid ${BAR_COLORS[idx % BAR_COLORS.length]}`,
                      }}
                    >
                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          marginBottom: 8,
                        }}
                      >
                        <Space size={8} wrap>
                          <Tag
                            style={{
                              borderRadius: 12,
                              fontWeight: 700,
                              background: BAR_COLORS[idx % BAR_COLORS.length],
                              color: '#fff',
                              border: 'none',
                            }}
                          >
                            #{item.rank}
                          </Tag>
                          <Tag color="blue" style={{ borderRadius: 6, fontWeight: 600 }}>
                            {item.diseaseCode}
                          </Tag>
                          <Text strong style={{ fontSize: 13.5, color: '#0f172a' }}>
                            {item.diseaseName}
                          </Text>
                        </Space>
                        <Text strong style={{ color: BAR_COLORS[idx % BAR_COLORS.length], fontSize: 14 }}>
                          {formatPercentage(item.percentage)}
                        </Text>
                      </div>

                      <Progress
                        percent={item.percentage}
                        showInfo={false}
                        strokeColor={BAR_COLORS[idx % BAR_COLORS.length]}
                        size="small"
                        style={{ margin: '4px 0' }}
                      />

                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          marginTop: 6,
                          fontSize: 12,
                          color: '#64748b',
                        }}
                      >
                        <span>Nhóm: <b>{item.diseaseGroup || 'Chưa phân nhóm'}</b></span>
                        <span><b>{item.diagnosisCount}</b> lượt chẩn đoán</span>
                      </div>
                    </div>
                  </Col>
                ))}
              </Row>
            </Card>
          )}

          {/* Bảng xếp hạng chi tiết */}
          <Card
            title={
              <Space size={8}>
                <FileTextOutlined style={{ color: '#2563eb', fontSize: 18 }} />
                <span style={{ fontSize: 15, fontWeight: 700, color: '#0f172a' }}>
                  Danh sách chi tiết xếp hạng theo mã bệnh
                </span>
              </Space>
            }
            bordered={false}
            style={{
              borderRadius: 12,
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
              border: '1px solid #f1f5f9',
            }}
            styles={{ body: { padding: '16px 20px' } }}
          >
            <Table
              dataSource={reportData.items}
              columns={columns}
              rowKey={(r) => r.catalogId || r.diseaseCode || r.rank}
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showTotal: (total) => `Tổng cộng ${total} mã bệnh chẩn đoán`,
              }}
              bordered={false}
            />
          </Card>
        </Space>
      ) : hasSearched ? (
        /* Empty State thân thiện, có gợi ý hành động */
        <Card
          bordered={false}
          style={{
            padding: '48px 24px',
            textAlign: 'center',
            borderRadius: 12,
            border: '1px solid #f1f5f9',
            background: '#ffffff',
            boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
          }}
        >
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={
              <div style={{ maxWidth: 520, margin: '0 auto' }}>
                <Text strong style={{ fontSize: 16, color: '#1e293b', display: 'block', marginBottom: 6 }}>
                  Không có dữ liệu chẩn đoán trong khoảng thời gian đã chọn
                </Text>
                <Text type="secondary" style={{ fontSize: 13, lineHeight: 1.6 }}>
                  Khoảng thời gian từ <b>{dateRange[0]?.format('DD/MM/YYYY')}</b> đến <b>{dateRange[1]?.format('DD/MM/YYYY')}</b>{' '}
                  chưa ghi nhận ca chẩn đoán nào có gắn mã bệnh trong hệ thống.
                </Text>
              </div>
            }
          >
            <div style={{ marginTop: 20 }}>
              <Space size={10} wrap>
                <Button
                  type="primary"
                  icon={<CalendarOutlined />}
                  onClick={() => handleQuickPreset('LAST_MONTH')}
                  style={{ borderRadius: 8, height: 36 }}
                >
                  Xem Tháng trước (Tháng 8/2026)
                </Button>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={() => handleQuickPreset('THIS_YEAR')}
                  style={{ borderRadius: 8, height: 36 }}
                >
                  Xem Toàn bộ Năm 2026
                </Button>
                <Button
                  onClick={() => handleQuickPreset('30DAYS')}
                  style={{ borderRadius: 8, height: 36 }}
                >
                  Xem 30 ngày qua
                </Button>
              </Space>
            </div>
          </Empty>
        </Card>
      ) : null}
    </div>
  )
}

export default DiseasePatternReportPage
