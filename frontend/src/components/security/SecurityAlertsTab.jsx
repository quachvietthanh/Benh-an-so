import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Drawer,
  Dropdown,
  Empty,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  EllipsisOutlined,
  EyeOutlined,
  FileSearchOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
  StopOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import securityAlertApi from '../../api/securityAlertApi'

const { Title, Text, Paragraph } = Typography

// Cấu hình loại cảnh báo theo chuẩn nghiệp vụ Backend
const ALERT_TYPE_CONFIG = {
  THRESHOLD_EXCEEDED: {
    label: 'Vượt ngưỡng truy cập',
    color: 'error',
    icon: <WarningOutlined />,
    description: 'Tài khoản mở xem quá 20 hồ sơ bệnh án trong vòng 1 giờ',
  },
  OFF_HOURS_ACCESS: {
    label: 'Truy cập ngoài giờ',
    color: 'warning',
    icon: <ClockCircleOutlined />,
    description: 'Tài khoản truy cập bệnh án ngoài khung giờ hành chính (trước 7:00 hoặc sau 18:00)',
  },
}

function SecurityAlertsTab() {
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(false)
  const [updatingId, setUpdatingId] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [selectedAlert, setSelectedAlert] = useState(null)

  // Phân trang
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)

  // Bộ lọc
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [severityFilter, setSeverityFilter] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')

  const fetchAlerts = useCallback(async (targetPage = page, targetSize = pageSize) => {
    setLoading(true)
    setErrorMessage('')
    try {
      const response = await securityAlertApi.getAlerts({
        page: targetPage,
        size: targetSize,
        sort: 'createdAt,desc',
      })
      const data = response?.data
      if (data && Array.isArray(data.content)) {
        setAlerts(data.content)
        setTotalElements(data.totalElements ?? data.content.length)
      } else if (Array.isArray(data)) {
        setAlerts(data)
        setTotalElements(data.length)
      } else {
        setAlerts([])
        setTotalElements(0)
      }
    } catch (err) {
      const status = err?.response?.status
      if (status === 404) {
        setErrorMessage(
          'API /api/v1/security-alerts trả về 404 (Resource not found). Nguyên nhân: Tiến trình Backend Spring Boot đang chạy bản build cũ (chưa nạp Controller mới). Vui lòng khởi động lại terminal Backend (Ctrl+C rồi chạy lại "mvn spring-boot:run").'
        )
      } else {
        const msg =
          err?.response?.data?.message ||
          err?.apiError?.message ||
          err?.message ||
          'Không thể tải danh sách cảnh báo bất thường.'
        setErrorMessage(msg)
      }
    } finally {
      setLoading(false)
    }
  }, [page, pageSize])

  useEffect(() => {
    fetchAlerts(page, pageSize)
  }, [fetchAlerts, page, pageSize])

  // Cập nhật trạng thái
  const handleUpdateStatus = async (alertId, newStatus) => {
    setUpdatingId(alertId)
    try {
      await securityAlertApi.updateStatus(alertId, newStatus)
      message.success(
        newStatus === 'READ'
          ? 'Đã đánh dấu cảnh báo là đã đọc.'
          : 'Đã bỏ qua cảnh báo thành công.'
      )
      setAlerts((prev) =>
        prev.map((item) => (item.id === alertId ? { ...item, status: newStatus } : item))
      )
      if (selectedAlert?.id === alertId) {
        setSelectedAlert((prev) => (prev ? { ...prev, status: newStatus } : null))
      }
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.apiError?.message ||
        err?.message ||
        'Cập nhật trạng thái thất bại.'
      message.error(msg)
    } finally {
      setUpdatingId(null)
    }
  }

  // Danh sách lọc client
  const filteredAlerts = useMemo(() => {
    return alerts.filter((item) => {
      if (typeFilter !== 'ALL' && item.alertType !== typeFilter) return false
      if (severityFilter !== 'ALL' && item.severity !== severityFilter) return false
      if (statusFilter !== 'ALL' && item.status !== statusFilter) return false
      return true
    })
  }, [alerts, typeFilter, severityFilter, statusFilter])

  // Thống kê nhanh
  const stats = useMemo(() => {
    const total = alerts.length
    const unread = alerts.filter((a) => a.status === 'UNREAD').length
    const high = alerts.filter((a) => a.severity === 'HIGH').length
    const offHours = alerts.filter((a) => a.alertType === 'OFF_HOURS_ACCESS').length
    return { total, unread, high, offHours }
  }, [alerts])

  // Định dạng mã cảnh báo chuẩn hệ thống (CB-XXXXXXXX)
  const formatAlertCode = (id) => {
    if (!id) return 'CB-00000000'
    const cleanId = String(id).replace(/-/g, '')
    return `CB-${cleanId.slice(0, 8).toUpperCase()}`
  }

  // Chuyển đổi mô tả nguyên nhân sang tiếng Việt chuẩn nghiệp vụ y tế
  const formatDescriptionVietnamese = (item) => {
    if (!item) return ''
    if (typeof item === 'string') {
      const matchHour = item.match(/User opened (\d+) medical records within one hour \(threshold (\d+)\)/i)
      if (matchHour) {
        return `Tài khoản đã mở xem ${matchHour[1]} hồ sơ bệnh án trong vòng 1 giờ, vượt quá ngưỡng quy định của hệ thống (tối đa ${matchHour[2]} lượt/giờ).`
      }
      const matchOff = item.match(/User accessed medical records outside working hours \((\d+) access\(es\)\)/i)
      if (matchOff) {
        return `Tài khoản đã truy cập mở xem hồ sơ bệnh án ngoài khung giờ làm việc hành chính (${matchOff[1]} lượt xem trước 07:00 hoặc sau 18:00).`
      }
      return item
    }

    const { alertType, accessCount, description } = item
    if (alertType === 'THRESHOLD_EXCEEDED') {
      return `Tài khoản đã mở xem ${accessCount || 20} hồ sơ bệnh án trong vòng 1 giờ, vượt quá ngưỡng quy định của hệ thống (tối đa 20 lượt/giờ).`
    }
    if (alertType === 'OFF_HOURS_ACCESS') {
      return `Tài khoản đã truy cập mở xem hồ sơ bệnh án ngoài khung giờ làm việc hành chính (${accessCount || 1} lượt xem trước 07:00 hoặc sau 18:00).`
    }
    if (description) {
      if (description.includes('within one hour')) {
        return `Tài khoản đã mở xem ${accessCount || 20} hồ sơ bệnh án trong vòng 1 giờ, vượt quá ngưỡng quy định (tối đa 20 lượt/giờ).`
      }
      if (description.includes('outside working hours')) {
        return `Tài khoản đã truy cập mở xem hồ sơ bệnh án ngoài khung giờ làm việc hành chính (${accessCount || 1} lượt xem).`
      }
      return description
    }
    return 'Hệ thống phát hiện dấu hiệu truy cập bất thường trên hồ sơ bệnh án.'
  }

  const columns = [
    {
      title: 'Tài khoản',
      key: 'user',
      width: 190,
      render: (_, record) => {
        const isHigh = record.severity === 'HIGH'
        return (
          <Space size={8} align="center">
            <Avatar
              size={34}
              style={{
                backgroundColor: isHigh ? '#fee2e2' : '#fef3c7',
                color: isHigh ? '#dc2626' : '#d97706',
                fontWeight: 700,
                fontSize: 13,
                flexShrink: 0,
              }}
            >
              {(record.fullName || record.username || 'U').charAt(0).toUpperCase()}
            </Avatar>
            <div style={{ minWidth: 0, overflow: 'hidden' }}>
              <div
                style={{
                  fontWeight: 600,
                  fontSize: 13,
                  color: '#0f172a',
                  lineHeight: 1.3,
                  whiteSpace: 'nowrap',
                  textOverflow: 'ellipsis',
                  overflow: 'hidden',
                }}
                title={record.fullName}
              >
                {record.fullName || 'Chưa cập nhật tên'}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, color: '#64748b', lineHeight: 1.2 }}>
                <span>@{record.username || 'unknown'}</span>
                <span style={{ color: '#cbd5e1' }}>•</span>
                <span style={{ fontFamily: 'monospace', color: '#1d4ed8', fontWeight: 600 }}>
                  {formatAlertCode(record.id)}
                </span>
              </div>
            </div>
          </Space>
        )
      },
    },
    {
      title: 'Loại cảnh báo & Mức độ',
      key: 'alertTypeSeverity',
      width: 220,
      render: (_, record) => {
        const isHigh = record.severity === 'HIGH'
        const isThreshold = record.alertType === 'THRESHOLD_EXCEEDED'
        const cfg = ALERT_TYPE_CONFIG[record.alertType] || {
          label: record.alertType,
          color: 'default',
          icon: <AlertOutlined />,
        }

        return (
          <Space size={6} wrap align="center">
            <Tooltip title={cfg.description}>
              <Tag
                color={isThreshold ? 'error' : 'warning'}
                icon={cfg.icon}
                style={{
                  margin: 0,
                  fontWeight: 500,
                  fontSize: 12,
                  borderRadius: 4,
                  padding: '1px 7px',
                }}
              >
                {cfg.label}
              </Tag>
            </Tooltip>

            <span
              style={{
                display: 'inline-block',
                padding: '1px 6px',
                borderRadius: 10,
                fontSize: 10,
                fontWeight: 700,
                letterSpacing: '0.4px',
                backgroundColor: isHigh ? '#dc2626' : '#f59e0b',
                color: '#ffffff',
              }}
            >
              {isHigh ? 'MỨC CAO' : 'MỨC THẤP'}
            </span>
          </Space>
        )
      },
    },
    {
      title: 'Số lần mở',
      dataIndex: 'accessCount',
      key: 'accessCount',
      width: 105,
      align: 'center',
      render: (count, record) => {
        const isHigh = record.severity === 'HIGH'
        return (
          <Tooltip title={formatDescriptionVietnamese(record)}>
            <span
              style={{
                display: 'inline-block',
                padding: '2px 8px',
                borderRadius: 6,
                backgroundColor: isHigh ? '#fff1f2' : '#fffbeb',
                color: isHigh ? '#be123c' : '#b45309',
                border: `1px solid ${isHigh ? '#fecdd3' : '#fde68a'}`,
                fontWeight: 700,
                fontSize: 12,
                cursor: 'pointer',
              }}
            >
              {count} lần
            </span>
          </Tooltip>
        )
      },
    },
    {
      title: 'Khoảng thời gian',
      key: 'window',
      width: 135,
      render: (_, record) => {
        const s = dayjs(record.windowStart)
        const e = dayjs(record.windowEnd)
        if (!s.isValid() || !e.isValid()) return <Text type="secondary">-</Text>
        return (
          <div style={{ lineHeight: 1.3 }}>
            <div style={{ fontSize: 12, fontWeight: 500, color: '#334155' }}>
              <ClockCircleOutlined style={{ marginRight: 4, color: '#94a3b8' }} />
              {s.format('HH:mm')} - {e.format('HH:mm')}
            </div>
            <div style={{ fontSize: 11, color: '#64748b' }}>
              {s.format('DD/MM/YYYY')}
            </div>
          </div>
        )
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      align: 'center',
      render: (st) => {
        if (st === 'UNREAD') {
          return (
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 5,
                padding: '2px 8px',
                borderRadius: 12,
                backgroundColor: '#fef2f2',
                color: '#dc2626',
                border: '1px solid #fecaca',
                fontWeight: 600,
                fontSize: 11,
              }}
            >
              <Badge status="error" />
              Chưa đọc
            </span>
          )
        }
        if (st === 'READ') {
          return (
            <Tag color="blue" icon={<EyeOutlined />} style={{ margin: 0, fontSize: 11, borderRadius: 10 }}>
              Đã đọc
            </Tag>
          )
        }
        return (
          <Tag color="default" icon={<CheckCircleOutlined />} style={{ margin: 0, fontSize: 11, borderRadius: 10 }}>
            Đã bỏ qua
          </Tag>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 70,
      align: 'center',
      render: (_, record) => {
        const isActionLoading = updatingId === record.id
        const isUnread = record.status === 'UNREAD'
        const isRead = record.status === 'READ'

        const menuItems = [
          {
            key: 'detail',
            icon: <FileSearchOutlined />,
            label: 'Xem chi tiết cảnh báo',
            onClick: () => setSelectedAlert(record),
          },
          isUnread && {
            type: 'divider',
          },
          isUnread && {
            key: 'mark-read',
            icon: <EyeOutlined style={{ color: '#2563eb' }} />,
            label: <span style={{ color: '#2563eb', fontWeight: 500 }}>Đánh dấu đã đọc</span>,
            disabled: isActionLoading,
            onClick: () => handleUpdateStatus(record.id, 'READ'),
          },
          (isUnread || isRead) && {
            type: 'divider',
          },
          (isUnread || isRead) && {
            key: 'dismiss',
            icon: <StopOutlined style={{ color: '#dc2626' }} />,
            label: <span style={{ color: '#dc2626', fontWeight: 500 }}>Bỏ qua / Giải quyết</span>,
            disabled: isActionLoading,
            onClick: () => {
              Modal.confirm({
                title: 'Bỏ qua cảnh báo này?',
                content: 'Hành động này sẽ đánh dấu cảnh báo là đã giải quyết.',
                okText: 'Bỏ qua',
                cancelText: 'Hủy',
                okButtonProps: { danger: true },
                onOk: () => handleUpdateStatus(record.id, 'DISMISSED'),
              })
            },
          },
        ].filter(Boolean)

        return (
          <Dropdown
            menu={{ items: menuItems }}
            trigger={['click']}
            placement="bottomRight"
          >
            <Tooltip title="Thao tác">
              <Button
                type="text"
                size="small"
                icon={<EllipsisOutlined style={{ fontSize: 18, color: '#475569' }} />}
                loading={isActionLoading}
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: 6,
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              />
            </Tooltip>
          </Dropdown>
        )
      },
    },
  ]

  return (
    <div className="security-alerts-tab" style={{ padding: '2px 0' }}>
      {/* Tiêu đề & Giới thiệu tinh gọn */}
      <div style={{ marginBottom: 14 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <WarningOutlined style={{ fontSize: 18, color: '#ef4444' }} />
          <Title level={4} style={{ margin: 0, fontSize: 17, color: '#0f172a' }}>
            Giám Sát & Cảnh Báo Truy Cập Bất Thường
          </Title>
        </div>
        <Paragraph type="secondary" style={{ margin: '2px 0 0', fontSize: 12, color: '#64748b' }}>
          Phát hiện sớm hành vi xem dữ liệu y tế sai mục đích thông qua tự động theo dõi số lần mở bệnh án của mỗi tài khoản và cảnh báo khi vượt ngưỡng (&gt; 20 lượt/giờ) hoặc mở ngoài giờ làm việc (trước 7:00 hoặc sau 18:00).
        </Paragraph>
      </div>

      {/* Thông báo lỗi nếu có */}
      {errorMessage && (
        <Alert
          type="error"
          showIcon
          message="Không thể nạp dữ liệu cảnh báo"
          description={errorMessage}
          action={
            <Button size="small" danger onClick={() => fetchAlerts(page, pageSize)}>
              Thử lại
            </Button>
          }
          style={{ marginBottom: 12, borderRadius: 8 }}
        />
      )}

      {/* Thống kê nhanh: Card phẳng, viền nhẹ, hiện đại */}
      <Row gutter={[12, 12]} style={{ marginBottom: 12 }}>
        <Col xs={12} sm={6}>
          <div
            style={{
              padding: '10px 14px',
              borderRadius: 8,
              backgroundColor: '#ffffff',
              border: '1px solid #e2e8f0',
              display: 'flex',
              alignItems: 'center',
              gap: 12,
            }}
          >
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                backgroundColor: '#eff6ff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#2563eb',
                fontSize: 16,
              }}
            >
              <AlertOutlined />
            </div>
            <div>
              <div style={{ fontSize: 11, color: '#64748b', fontWeight: 500 }}>Tổng cảnh báo</div>
              <div style={{ fontSize: 18, fontWeight: 700, color: '#0f172a', lineHeight: 1.2 }}>
                {totalElements}
              </div>
            </div>
          </div>
        </Col>

        <Col xs={12} sm={6}>
          <div
            style={{
              padding: '10px 14px',
              borderRadius: 8,
              backgroundColor: '#ffffff',
              border: stats.unread > 0 ? '1px solid #fecaca' : '1px solid #e2e8f0',
              display: 'flex',
              alignItems: 'center',
              gap: 12,
            }}
          >
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                backgroundColor: stats.unread > 0 ? '#fef2f2' : '#f8fafc',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: stats.unread > 0 ? '#dc2626' : '#94a3b8',
                fontSize: 16,
              }}
            >
              <Badge status={stats.unread > 0 ? 'error' : 'default'} />
            </div>
            <div>
              <div style={{ fontSize: 11, color: '#64748b', fontWeight: 500 }}>Chưa xử lý</div>
              <div
                style={{
                  fontSize: 18,
                  fontWeight: 700,
                  color: stats.unread > 0 ? '#dc2626' : '#0f172a',
                  lineHeight: 1.2,
                }}
              >
                {stats.unread}
              </div>
            </div>
          </div>
        </Col>

        <Col xs={12} sm={6}>
          <div
            style={{
              padding: '10px 14px',
              borderRadius: 8,
              backgroundColor: '#ffffff',
              border: '1px solid #e2e8f0',
              display: 'flex',
              alignItems: 'center',
              gap: 12,
            }}
          >
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                backgroundColor: '#fef2f2',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#ef4444',
                fontSize: 16,
              }}
            >
              <WarningOutlined />
            </div>
            <div>
              <div style={{ fontSize: 11, color: '#64748b', fontWeight: 500 }}>Mức độ CAO</div>
              <div style={{ fontSize: 18, fontWeight: 700, color: '#dc2626', lineHeight: 1.2 }}>
                {stats.high}
              </div>
            </div>
          </div>
        </Col>

        <Col xs={12} sm={6}>
          <div
            style={{
              padding: '10px 14px',
              borderRadius: 8,
              backgroundColor: '#ffffff',
              border: '1px solid #e2e8f0',
              display: 'flex',
              alignItems: 'center',
              gap: 12,
            }}
          >
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                backgroundColor: '#faf5ff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#9333ea',
                fontSize: 16,
              }}
            >
              <ClockCircleOutlined />
            </div>
            <div>
              <div style={{ fontSize: 11, color: '#64748b', fontWeight: 500 }}>Ngoài giờ làm việc</div>
              <div style={{ fontSize: 18, fontWeight: 700, color: '#9333ea', lineHeight: 1.2 }}>
                {stats.offHours}
              </div>
            </div>
          </div>
        </Col>
      </Row>

      {/* Thanh công cụ lọc & thao tác gọn gàng */}
      <div
        style={{
          marginBottom: 12,
          padding: '8px 12px',
          borderRadius: 8,
          backgroundColor: '#f8fafc',
          border: '1px solid #e2e8f0',
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 8,
        }}
      >
        <Space wrap size={8}>
          <Select
            size="small"
            value={typeFilter}
            onChange={setTypeFilter}
            style={{ width: 175 }}
            options={[
              { value: 'ALL', label: 'Tất cả loại vi phạm' },
              { value: 'THRESHOLD_EXCEEDED', label: '⚠️ Vượt ngưỡng truy cập' },
              { value: 'OFF_HOURS_ACCESS', label: '🕒 Truy cập ngoài giờ' },
            ]}
          />

          <Select
            size="small"
            value={severityFilter}
            onChange={setSeverityFilter}
            style={{ width: 130 }}
            options={[
              { value: 'ALL', label: 'Tất cả mức độ' },
              { value: 'HIGH', label: '🔴 Mức độ CAO' },
              { value: 'LOW', label: '🟡 Mức độ THẤP' },
            ]}
          />

          <Select
            size="small"
            value={statusFilter}
            onChange={setStatusFilter}
            style={{ width: 140 }}
            options={[
              { value: 'ALL', label: 'Tất cả trạng thái' },
              { value: 'UNREAD', label: '⭕ Chưa đọc' },
              { value: 'READ', label: '👁️ Đã đọc' },
              { value: 'DISMISSED', label: '⏹️ Đã bỏ qua' },
            ]}
          />
        </Space>

        <Tooltip title="Bộ quét chạy ngầm mỗi 5 phút/lần. Bấm làm mới để kiểm tra vi phạm mới.">
          <Button
            size="small"
            icon={<ReloadOutlined />}
            loading={loading}
            onClick={() => fetchAlerts(page, pageSize)}
          >
            Làm mới
          </Button>
        </Tooltip>
      </div>

      {/* Bảng dữ liệu: Đã tối ưu chiều rộng, KHÔNG bị cuộn ngang */}
      <Card
        size="small"
        bodyStyle={{ padding: 0 }}
        style={{ borderRadius: 8, overflow: 'hidden', border: '1px solid #e2e8f0' }}
      >
        <Table
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={filteredAlerts}
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize,
            total: totalElements,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            showTotal: (total) => `Tổng số ${total} cảnh báo`,
            size: 'small',
            style: { margin: '12px 16px' },
            onChange: (p, s) => {
              setPage(p - 1)
              setPageSize(s)
            },
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  <div style={{ padding: '16px 0' }}>
                    <div style={{ fontWeight: 600, color: '#334155' }}>
                      Không có cảnh báo bất thường nào
                    </div>
                    <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 2 }}>
                      Chưa phát hiện hành vi truy cập bệnh án vượt ngưỡng hoặc ngoài giờ làm việc.
                    </div>
                  </div>
                }
              />
            ),
          }}
        />
      </Card>

      {/* Drawer xem chi tiết cảnh báo */}
      <Drawer
        title={
          <Space align="center" size={10}>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                backgroundColor: selectedAlert?.severity === 'HIGH' ? '#fee2e2' : '#fef3c7',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: selectedAlert?.severity === 'HIGH' ? '#dc2626' : '#d97706',
                fontSize: 16,
              }}
            >
              <WarningOutlined />
            </div>
            <div>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a', lineHeight: 1.2 }}>
                Chi tiết cảnh báo truy cập
              </div>
              <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                Thông tin ghi nhận từ hệ thống giám sát và quét an toàn dữ liệu
              </div>
            </div>
          </Space>
        }
        placement="right"
        width={560}
        onClose={() => setSelectedAlert(null)}
        open={Boolean(selectedAlert)}
        bodyStyle={{ padding: '20px 24px', backgroundColor: '#f8fafc' }}
        footer={
          selectedAlert && (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '4px 0' }}>
              <Space size={10}>
                {selectedAlert.status === 'UNREAD' && (
                  <Button
                    type="primary"
                    size="middle"
                    icon={<EyeOutlined />}
                    loading={updatingId === selectedAlert.id}
                    onClick={() => handleUpdateStatus(selectedAlert.id, 'READ')}
                    style={{ fontWeight: 500, borderRadius: 6 }}
                  >
                    Đánh dấu đã đọc
                  </Button>
                )}

                {selectedAlert.status !== 'DISMISSED' && (
                  <Button
                    danger
                    size="middle"
                    icon={<StopOutlined />}
                    loading={updatingId === selectedAlert.id}
                    onClick={() => {
                      Modal.confirm({
                        title: 'Bỏ qua cảnh báo này?',
                        content: 'Hành động này sẽ đánh dấu cảnh báo là đã giải quyết / bỏ qua.',
                        okText: 'Bỏ qua',
                        cancelText: 'Hủy',
                        okButtonProps: { danger: true },
                        onOk: () => handleUpdateStatus(selectedAlert.id, 'DISMISSED'),
                      })
                    }}
                    style={{ fontWeight: 500, borderRadius: 6 }}
                  >
                    Bỏ qua cảnh báo
                  </Button>
                )}
              </Space>

              <Button size="middle" onClick={() => setSelectedAlert(null)} style={{ borderRadius: 6 }}>
                Đóng
              </Button>
            </div>
          )
        }
      >
        {selectedAlert && (
          <Descriptions
            bordered
            column={1}
            size="middle"
            labelStyle={{
              width: 175,
              minWidth: 175,
              maxWidth: 175,
              backgroundColor: '#f8fafc',
              color: '#334155',
              fontWeight: 600,
              fontSize: 13,
              whiteSpace: 'nowrap',
              verticalAlign: 'middle',
              padding: '12px 16px',
            }}
            contentStyle={{
              backgroundColor: '#ffffff',
              color: '#0f172a',
              fontSize: 13,
              verticalAlign: 'middle',
              padding: '12px 16px',
            }}
            style={{
              backgroundColor: '#ffffff',
              borderRadius: 8,
              overflow: 'hidden',
              border: '1px solid #e2e8f0',
            }}
          >
            <Descriptions.Item label="Mã cảnh báo">
              <Space size={8} align="center" wrap>
                <span
                  style={{
                    fontFamily: 'SFMono-Regular, Consolas, "Liberation Mono", Menlo, monospace',
                    fontWeight: 700,
                    fontSize: 13,
                    color: '#1d4ed8',
                    backgroundColor: '#eff6ff',
                    padding: '3px 10px',
                    borderRadius: 6,
                    border: '1px solid #bfdbfe',
                    letterSpacing: '0.5px',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 4,
                  }}
                >
                  {formatAlertCode(selectedAlert.id)}
                </span>
                <Tooltip title="Sao chép mã cảnh báo">
                  <Typography.Text
                    copyable={{
                      text: formatAlertCode(selectedAlert.id),
                      tooltips: ['Sao chép mã cảnh báo', 'Đã sao chép!'],
                    }}
                  />
                </Tooltip>
                <span
                  style={{
                    fontSize: 11,
                    color: '#94a3b8',
                    fontFamily: 'monospace',
                  }}
                  title={`UUID kỹ thuật: ${selectedAlert.id}`}
                >
                  ({selectedAlert.id ? `${selectedAlert.id.slice(0, 8)}...${selectedAlert.id.slice(-4)}` : ''})
                </span>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Thời điểm phát hiện">
              <Space size={8} align="center">
                <ClockCircleOutlined style={{ color: '#64748b' }} />
                <span style={{ fontWeight: 600, color: '#0f172a', fontSize: 13 }}>
                  {dayjs(selectedAlert.createdAt).format('DD/MM/YYYY HH:mm:ss')}
                </span>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Tài khoản vi phạm">
              <Space size={10} align="center">
                <Avatar
                  size={36}
                  style={{
                    backgroundColor: selectedAlert.severity === 'HIGH' ? '#fee2e2' : '#fef3c7',
                    color: selectedAlert.severity === 'HIGH' ? '#dc2626' : '#d97706',
                    fontWeight: 700,
                    fontSize: 14,
                    border: `1px solid ${selectedAlert.severity === 'HIGH' ? '#fca5a5' : '#fcd34d'}`,
                  }}
                >
                  {(selectedAlert.fullName || selectedAlert.username || 'U').charAt(0).toUpperCase()}
                </Avatar>
                <div>
                  <div style={{ fontWeight: 700, fontSize: 13, color: '#0f172a', lineHeight: 1.3 }}>
                    {selectedAlert.fullName || 'Chưa cập nhật họ tên'}
                  </div>
                  <div style={{ color: '#64748b', fontSize: 12 }}>
                    @{selectedAlert.username}
                  </div>
                </div>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Loại cảnh báo">
              <Tag
                color={selectedAlert.alertType === 'THRESHOLD_EXCEEDED' ? 'error' : 'warning'}
                icon={
                  selectedAlert.alertType === 'THRESHOLD_EXCEEDED' ? (
                    <WarningOutlined />
                  ) : (
                    <ClockCircleOutlined />
                  )
                }
                style={{
                  fontWeight: 600,
                  fontSize: 12,
                  borderRadius: 4,
                  padding: '2px 10px',
                  margin: 0,
                }}
              >
                {ALERT_TYPE_CONFIG[selectedAlert.alertType]?.label || selectedAlert.alertType}
              </Tag>
            </Descriptions.Item>

            <Descriptions.Item label="Mức độ nghiêm trọng">
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '3px 10px',
                  borderRadius: 20,
                  fontSize: 11,
                  fontWeight: 700,
                  letterSpacing: '0.4px',
                  backgroundColor: selectedAlert.severity === 'HIGH' ? '#fee2e2' : '#fef3c7',
                  color: selectedAlert.severity === 'HIGH' ? '#dc2626' : '#d97706',
                  border: `1px solid ${selectedAlert.severity === 'HIGH' ? '#fca5a5' : '#fcd34d'}`,
                }}
              >
                <span
                  style={{
                    width: 6,
                    height: 6,
                    borderRadius: '50%',
                    backgroundColor: selectedAlert.severity === 'HIGH' ? '#dc2626' : '#d97706',
                  }}
                />
                {selectedAlert.severity === 'HIGH' ? 'MỨC ĐỘ CAO' : 'MỨC ĐỘ THẤP'}
              </span>
            </Descriptions.Item>

            <Descriptions.Item label="Số lượt mở bệnh án">
              <Space size={8} align="center">
                <span style={{ fontWeight: 700, color: '#dc2626', fontSize: 16 }}>
                  {selectedAlert.accessCount} lượt
                </span>
                <span style={{ color: '#64748b', fontSize: 12 }}>
                  {selectedAlert.alertType === 'THRESHOLD_EXCEEDED'
                    ? '(vượt ngưỡng cho phép tối đa 20 lượt/giờ)'
                    : '(ngoài khung giờ làm việc 07:00 - 18:00)'}
                </span>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Khoảng thời gian">
              <Space size={6} align="center">
                <ClockCircleOutlined style={{ color: '#64748b' }} />
                <span style={{ fontWeight: 600, color: '#0f172a', fontSize: 13 }}>
                  {dayjs(selectedAlert.windowStart).format('HH:mm DD/MM/YYYY')}
                  {' - '}
                  {dayjs(selectedAlert.windowEnd).format('HH:mm DD/MM/YYYY')}
                </span>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Mô tả nguyên nhân">
              <div
                style={{
                  padding: '12px 14px',
                  borderRadius: 8,
                  backgroundColor: selectedAlert.severity === 'HIGH' ? '#fff1f2' : '#fffbeb',
                  border: `1px solid ${selectedAlert.severity === 'HIGH' ? '#fecdd3' : '#fde68a'}`,
                  color: selectedAlert.severity === 'HIGH' ? '#9f1239' : '#92400e',
                  fontSize: 13,
                  lineHeight: 1.6,
                  fontWeight: 500,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                  <InfoCircleOutlined style={{ fontSize: 15, marginTop: 3, flexShrink: 0 }} />
                  <div>
                    {formatDescriptionVietnamese(selectedAlert)}
                  </div>
                </div>
              </div>
            </Descriptions.Item>

            <Descriptions.Item label="Trạng thái">
              {selectedAlert.status === 'UNREAD' && (
                <span
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '3px 10px',
                    borderRadius: 12,
                    backgroundColor: '#fef2f2',
                    color: '#dc2626',
                    border: '1px solid #fecaca',
                    fontWeight: 600,
                    fontSize: 12,
                  }}
                >
                  <Badge status="error" /> Chưa đọc (Mới phát hiện)
                </span>
              )}
              {selectedAlert.status === 'READ' && (
                <Tag
                  color="blue"
                  icon={<EyeOutlined />}
                  style={{
                    borderRadius: 10,
                    padding: '2px 10px',
                    fontSize: 12,
                    margin: 0,
                  }}
                >
                  Đã đọc
                </Tag>
              )}
              {selectedAlert.status === 'DISMISSED' && (
                <Tag
                  color="default"
                  icon={<CheckCircleOutlined />}
                  style={{
                    borderRadius: 10,
                    padding: '2px 10px',
                    fontSize: 12,
                    margin: 0,
                  }}
                >
                  Đã giải quyết / Bỏ qua
                </Tag>
              )}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  )
}

export default SecurityAlertsTab
