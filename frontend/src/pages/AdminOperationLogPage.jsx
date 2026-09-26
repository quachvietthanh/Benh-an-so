import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Col,
  DatePicker,
  Descriptions,
  Divider,
  Drawer,
  Empty,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AppstoreOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  DollarOutlined,
  ExperimentOutlined,
  EyeOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  HistoryOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import adminOperationLogApi from '../api/adminOperationLogApi.js'
import userApi from '../api/userApi.js'
import { useAuthContext } from '../context/AuthContext'
import {
  ACTION_TYPE_CONFIG,
  RESOURCE_TYPE_FILTER_OPTIONS,
  RESOURCE_TYPE_OPTIONS,
  buildDiffRows,
  canViewAdminOperationLogs,
  convertDateRangeToIso,
  formatActionType,
  formatFieldValue,
  formatResourceType,
  formatVietnamDateTime,
  safeParseDetail,
  validateDateRange,
} from '../utils/adminOperationLogHelpers.js'

const { Title, Text, Paragraph } = Typography

export default function AdminOperationLogPage() {
  const navigate = useNavigate()
  const { user } = useAuthContext()

  // User roles & permissions check
  const userRoles = useMemo(
    () => (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, '')),
    [user]
  )
  const userPerms = useMemo(
    () => (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, '')),
    [user]
  )

  const canView = canViewAdminOperationLogs(userRoles, userPerms)

  // Filter states
  const [selectedResourceType, setSelectedResourceType] = useState(undefined)
  const [selectedActorId, setSelectedActorId] = useState(undefined)
  const [selectedDateRange, setSelectedDateRange] = useState(null)

  // Data states
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)

  // Actors list for filter
  const [actors, setActors] = useState([])
  const [actorsLoading, setActorsLoading] = useState(false)
  const [usersMap, setUsersMap] = useState(new Map())

  // Detail Drawer state
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [selectedLog, setSelectedLog] = useState(null)
  const [onlyShowChanged, setOnlyShowChanged] = useState(false)

  // Load actors for actor filter (Admin & Manager only)
  const fetchActors = useCallback(async () => {
    if (!canView) return
    setActorsLoading(true)
    try {
      const res = await userApi.getAll({ size: 100 })
      const list = res?.data?.content || res?.data?.data || (Array.isArray(res?.data) ? res.data : [])

      const map = new Map()
      list.forEach((u) => {
        if (u?.id) map.set(String(u.id), u)
      })
      setUsersMap(map)

      // Filter actors to users who have admin, manager roles or who can perform administrative tasks
      const adminUsers = list.filter((u) => {
        const rawRole = u.roleName || u.role || ''
        const singleRole = String(rawRole).toLowerCase().replace(/^role_/, '')
        const multiRoles = Array.isArray(u.roles)
          ? u.roles.map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
          : []
        const userRoles = multiRoles.length > 0 ? multiRoles : (singleRole ? [singleRole] : [])
        return (
          userRoles.includes('admin') ||
          userRoles.includes('manager') ||
          userRoles.includes('clinic_manager')
        )
      })
      setActors(adminUsers)
    } catch {
      // Non-blocking fallback
      setActors([])
    } finally {
      setActorsLoading(false)
    }
  }, [canView])

  // Helper format tên người thực hiện thân thiện và chuẩn hóa tiếng Việt
  const formatActorDisplay = useCallback(
    (name, actorId) => {
      const u = actorId ? usersMap.get(String(actorId)) : null
      let displayName = name || u?.fullName || u?.username || 'Hệ thống'

      if (displayName === 'Clinic Manager') {
        displayName = 'Quản lý phòng khám'
      } else if (displayName === 'System Administrator') {
        displayName = 'Quản trị viên hệ thống'
      } else if (displayName === 'System') {
        displayName = 'Hệ thống tự động'
      }

      const username = u?.username
      return { displayName, username }
    },
    [usersMap]
  )

  // Load logs from backend
  const fetchLogs = useCallback(
    async (targetPage = page, targetPageSize = pageSize) => {
      if (!canView) return

      const { from, to } = convertDateRangeToIso(selectedDateRange)
      const rangeValidation = validateDateRange(from, to)
      if (!rangeValidation.isValid) {
        message.error(rangeValidation.error)
        return
      }

      setLoading(true)
      try {
        const params = {
          page: targetPage,
          size: targetPageSize,
          sort: 'createdAt,desc',
        }

        if (selectedResourceType) {
          params.resourceType = selectedResourceType
        }
        if (selectedActorId) {
          params.actorId = selectedActorId
        }
        if (from) {
          params.from = from
        }
        if (to) {
          params.to = to
        }

        const res = await adminOperationLogApi.getLogs(params)
        const data = res?.data
        const content = data?.content || (Array.isArray(data) ? data : [])
        const total = typeof data?.totalElements === 'number' ? data.totalElements : 0

        setLogs(content)
        setTotalElements(total)
      } catch (err) {
        const status = err?.response?.status || err?.status
        if (status === 400) {
          message.error(
            'Khoảng thời gian lọc không hợp lệ. "Từ ngày" phải trước hoặc bằng "Đến ngày".'
          )
        } else if (status === 403) {
          message.error('Bạn không có quyền xem nhật ký thao tác quản trị.')
        } else {
          message.error('Không thể tải nhật ký thao tác. Vui lòng thử lại.')
        }
      } finally {
        setLoading(false)
      }
    },
    [canView, page, pageSize, selectedResourceType, selectedActorId, selectedDateRange]
  )

  useEffect(() => {
    if (canView) {
      fetchActors()
    }
  }, [canView, fetchActors])

  useEffect(() => {
    if (canView) {
      fetchLogs(page, pageSize)
    }
  }, [canView, page, pageSize, fetchLogs])

  // Handle Search button click (reset to page 0)
  const handleSearch = () => {
    setPage(0)
    fetchLogs(0, pageSize)
  }

  // Handle Reset filters button click
  const handleResetFilters = () => {
    setSelectedResourceType(undefined)
    setSelectedActorId(undefined)
    setSelectedDateRange(null)
    setPage(0)
  }

  // Open Drawer with selected log entry
  const handleOpenDetail = (record) => {
    setSelectedLog(record)
    setOnlyShowChanged(false)
    setDrawerVisible(true)
  }

  // Resource Type Icon helper
  const getResourceIcon = (type) => {
    switch (type) {
      case 'USER':
        return <UserOutlined style={{ color: '#1677ff', marginRight: 6 }} />
      case 'ROLE':
        return <SafetyCertificateOutlined style={{ color: '#722ed1', marginRight: 6 }} />
      case 'MEDICINE':
        return <MedicineBoxOutlined style={{ color: '#13c2c2', marginRight: 6 }} />
      case 'SERVICE_CATALOG':
        return <AppstoreOutlined style={{ color: '#eb2f96', marginRight: 6 }} />
      case 'SERVICE_PRICE':
        return <DollarOutlined style={{ color: '#faad14', marginRight: 6 }} />
      case 'DIAGNOSIS_CATALOG':
        return <MedicineBoxOutlined style={{ color: '#52c41a', marginRight: 6 }} />
      case 'SECURITY_ALERT':
        return <SafetyCertificateOutlined style={{ color: '#f5222d', marginRight: 6 }} />
      case 'CONFIGURATION':
        return <SettingOutlined style={{ color: '#722ed1', marginRight: 6 }} />
      case 'SYSTEM_BACKUP':
        return <DatabaseOutlined style={{ color: '#2f54eb', marginRight: 6 }} />
      case 'MEDICAL_RECORD_TEMPLATE':
        return <FileTextOutlined style={{ color: '#1890ff', marginRight: 6 }} />
      case 'CLINICAL_SERVICE':
        return <ExperimentOutlined style={{ color: '#fa8c16', marginRight: 6 }} />
      case 'ROOM':
        return <AppstoreOutlined style={{ color: '#13c2c2', marginRight: 6 }} />
      case 'DOCTOR_SCHEDULE':
      case 'DOCTOR_TIMEOFF':
        return <CalendarOutlined style={{ color: '#fa541c', marginRight: 6 }} />
      default:
        return <FileSearchOutlined style={{ color: '#8c8c8c', marginRight: 6 }} />
    }
  }

  // Access denied guard
  if (!canView) {
    return (
      <div style={{ padding: 24, maxWidth: 800, margin: '40px auto' }}>
        <Alert
          type="error"
          showIcon
          message="Từ chối truy cập (403 Forbidden)"
          description="Bạn không có quyền truy cập vào chức năng Nhật ký thao tác quản trị hệ thống (NCL-09-CN-006). Vui lòng liên hệ Quản trị viên để được cấp quyền ADMIN_OPERATION_LOG_READ."
          action={
            <Button
              type="primary"
              onClick={() => navigate('/')}
              style={{ height: 38, minWidth: 96, borderRadius: 6 }}
            >
              Về trang chủ
            </Button>
          }
        />
      </div>
    )
  }

  // Table columns definition
  const columns = [
    {
      title: 'STT',
      key: 'stt',
      width: 60,
      align: 'center',
      render: (_, __, index) => page * pageSize + index + 1,
    },
    {
      title: 'Thời điểm',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 175,
      render: (val) => (
        <Space size={4}>
          <ClockCircleOutlined style={{ color: '#8c8c8c' }} />
          <Text style={{ fontSize: 13, fontWeight: 500 }}>{formatVietnamDateTime(val)}</Text>
        </Space>
      ),
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'actorName',
      key: 'actorName',
      width: 190,
      render: (name, record) => {
        const { displayName, username } = formatActorDisplay(name, record.actorId)
        return (
          <Space size={8} align="center">
            <UserOutlined style={{ color: '#1677ff', fontSize: 16 }} />
            <div>
              <div style={{ fontWeight: 600, color: '#1f2937', fontSize: 13 }}>{displayName}</div>
              {username && (
                <div style={{ fontSize: 11.5, color: '#6b7280' }}>
                  @{username}
                </div>
              )}
            </div>
          </Space>
        )
      },
    },
    {
      title: 'Đối tượng quản trị',
      dataIndex: 'resourceType',
      key: 'resourceType',
      width: 190,
      render: (type) => (
        <span style={{ fontWeight: 500, fontSize: 13 }}>
          {getResourceIcon(type)}
          {formatResourceType(type)}
        </span>
      ),
    },
    {
      title: 'Hành động',
      dataIndex: 'actionType',
      key: 'actionType',
      width: 130,
      align: 'center',
      render: (type) => {
        const config = formatActionType(type)
        return (
          <Tag color={config.color} style={{ fontWeight: 600, padding: '2px 8px', borderRadius: 4 }}>
            {config.label}
          </Tag>
        )
      },
    },
    {
      title: 'Mã đối tượng',
      dataIndex: 'resourceId',
      key: 'resourceId',
      width: 130,
      render: (id) => {
        if (!id) return <Text type="secondary">—</Text>
        const idStr = String(id)
        const shortId = idStr.length > 8 ? idStr.slice(0, 8) : idStr
        return (
          <Tooltip title={`Mã UUID đầy đủ: ${idStr} (Nhấp biểu tượng để sao chép)`}>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
                padding: '2px 8px',
                borderRadius: 6,
                backgroundColor: '#f8fafc',
                border: '1px solid #e2e8f0',
              }}
            >
              <Text
                copyable={{ text: idStr, tooltips: ['Sao chép mã đầy đủ', 'Đã sao chép!'] }}
                style={{
                  margin: 0,
                  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
                  fontSize: 12,
                  color: '#334155',
                  fontWeight: 500,
                }}
              >
                {shortId}
              </Text>
            </span>
          </Tooltip>
        )
      },
    },
    {
      title: 'Chi tiết thay đổi',
      key: 'action',
      width: 130,
      align: 'center',
      render: (_, record) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => handleOpenDetail(record)}
          style={{ padding: 0, fontWeight: 500 }}
        >
          Xem thay đổi
        </Button>
      ),
    },
  ]

  // Parsed detail info for Drawer
  const detailData = selectedLog ? safeParseDetail(selectedLog.detail) : { before: {}, after: {}, isValid: false }
  const diffRows = selectedLog && detailData.isValid ? buildDiffRows(detailData.before, detailData.after) : []
  const changedCount = useMemo(() => diffRows.filter((r) => r.changed).length, [diffRows])
  const displayedDiffRows = useMemo(() => {
    if (!diffRows.length) return []
    return onlyShowChanged ? diffRows.filter((r) => r.changed) : diffRows
  }, [diffRows, onlyShowChanged])
  const isCreateAction = selectedLog?.actionType === 'CREATE'

  return (
    <div style={{ padding: 24, maxWidth: 1400, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: 20 }}>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space align="center">
              <HistoryOutlined style={{ fontSize: 26, color: '#1677ff' }} />
              <div>
                <Title level={3} style={{ margin: 0, color: '#0f172a' }}>
                  Nhật ký thao tác quản trị hệ thống
                </Title>
              </div>
            </Space>
          </Col>
          <Col>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => fetchLogs(page, pageSize)}
              loading={loading}
              style={{ height: 38, minWidth: 96, borderRadius: 6 }}
            >
              Làm mới
            </Button>
          </Col>
        </Row>
      </div>

      {/* Scope info notice */}
      <Alert
        type="info"
        showIcon
        closable
        style={{
          marginBottom: 16,
          borderRadius: 8,
          backgroundColor: '#f0f9ff',
          borderColor: '#bae6fd',
        }}
        message={
          <span style={{ fontWeight: 600, color: '#0369a1' }}>
            Phạm vi ghi nhận nhật ký quản trị:
          </span>
        }
        description={
          <span style={{ color: '#0c4a6e', fontSize: 13 }}>
            Chỉ lưu vết các thao tác cấu hình hệ thống bao gồm: <strong>Tài khoản nhân viên (USER)</strong>, <strong>Vai trò & Phân quyền (ROLE)</strong>, <strong>Danh mục thuốc (MEDICINE)</strong>, <strong>Danh mục dịch vụ (SERVICE_CATALOG)</strong> và <strong>Bảng giá (SERVICE_PRICE)</strong>. Các nghiệp vụ vận hành hàng ngày (nhập lô thuốc vào kho, kê đơn, khám bệnh, thu viện phí...) không thuộc phạm vi của nhật ký cấu hình này.
          </span>
        }
      />

      {/* Filter Card */}
      <Card
        bordered={false}
        style={{
          marginBottom: 20,
          borderRadius: 8,
          boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
          backgroundColor: '#ffffff',
        }}
      >
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={12} md={6}>
            <div style={{ marginBottom: 4, fontSize: 12, fontWeight: 600, color: '#4b5563' }}>
              Đối tượng quản trị
            </div>
            <Select
              allowClear
              placeholder="Tất cả đối tượng"
              value={selectedResourceType || 'ALL'}
              onChange={(val) => {
                const normalized = !val || val === 'ALL' ? undefined : val
                setSelectedResourceType(normalized)
                setPage(0)
              }}
              style={{ width: '100%' }}
              options={RESOURCE_TYPE_FILTER_OPTIONS}
            />
          </Col>

          <Col xs={24} sm={12} md={6}>
            <div style={{ marginBottom: 4, fontSize: 12, fontWeight: 600, color: '#4b5563' }}>
              Người thực hiện
            </div>
            <Select
              allowClear
              showSearch
              placeholder="Tất cả người thực hiện"
              value={selectedActorId || 'ALL'}
              loading={actorsLoading}
              onChange={(val) => {
                const normalized = !val || val === 'ALL' ? undefined : val
                setSelectedActorId(normalized)
                setPage(0)
              }}
              filterOption={(input, option) =>
                (option?.label || '').toLowerCase().includes(input.toLowerCase())
              }
              style={{ width: '100%' }}
              options={[
                { value: 'ALL', label: 'Tất cả người thực hiện' },
                ...actors.map((a) => {
                  let name = a.fullName || a.username || 'Người dùng'
                  if (name === 'Clinic Manager') name = 'Quản lý phòng khám'
                  if (name === 'System Administrator') name = 'Quản trị viên hệ thống'
                  return {
                    value: a.id,
                    label: `${name} (${a.username || a.email || 'N/A'})`,
                  }
                }),
              ]}
            />
          </Col>

          <Col xs={24} sm={12} md={7}>
            <div style={{ marginBottom: 4, fontSize: 12, fontWeight: 600, color: '#4b5563' }}>
              Khoảng thời gian thao tác
            </div>
            <DatePicker.RangePicker
              value={selectedDateRange}
              onChange={(dates) => {
                setSelectedDateRange(dates)
                setPage(0)
              }}
              format="DD/MM/YYYY"
              placeholder={['Từ ngày', 'Đến ngày']}
              style={{ width: '100%' }}
            />
          </Col>

          <Col xs={24} sm={12} md={5} style={{ textAlign: 'right', display: 'flex', alignItems: 'flex-end', gap: 8, marginTop: 22 }}>
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={handleSearch}
              loading={loading}
              style={{ height: 38, minWidth: 96, borderRadius: 6, flex: 1 }}
            >
              Tìm kiếm
            </Button>
            <Button
              onClick={handleResetFilters}
              style={{ height: 38, borderRadius: 6 }}
            >
              Xóa lọc
            </Button>
          </Col>
        </Row>
      </Card>

      {/* Main Table Card */}
      <Card
        bordered={false}
        style={{
          borderRadius: 8,
          boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
          backgroundColor: '#ffffff',
        }}
        bodyStyle={{ padding: 0 }}
      >
        <Table
          dataSource={logs}
          columns={columns}
          rowKey={(r) => r.id}
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize: pageSize,
            total: totalElements,
            showTotal: (total, range) => `${range[0]}-${range[1]} của ${total} nhật ký thao tác`,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            onChange: (newPage, newPageSize) => {
              setPage(newPage - 1)
              setPageSize(newPageSize)
            },
          }}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Chưa có nhật ký thao tác nào khớp với bộ lọc hiện tại."
              />
            ),
          }}
        />
      </Card>

      {/* Drawer: Chi tiết thay đổi trước và sau */}
      <Drawer
        title={
          <Space>
            <HistoryOutlined style={{ color: '#1677ff', fontSize: 18 }} />
            <span>Chi tiết thay đổi cấu hình</span>
          </Space>
        }
        placement="right"
        width={680}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        destroyOnClose
      >
        {selectedLog && (
          <div>
            {/* Meta info header */}
            <Descriptions
              bordered
              size="small"
              column={1}
              style={{ marginBottom: 20 }}
            >
              <Descriptions.Item label="Thời điểm ghi nhận">
                <Text strong>{formatVietnamDateTime(selectedLog.createdAt)}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Người thực hiện">
                <Space size={6}>
                  <UserOutlined style={{ color: '#1677ff' }} />
                  <Text strong>
                    {formatActorDisplay(selectedLog.actorName, selectedLog.actorId).displayName}
                  </Text>
                  {formatActorDisplay(selectedLog.actorName, selectedLog.actorId).username && (
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      ({formatActorDisplay(selectedLog.actorName, selectedLog.actorId).username})
                    </Text>
                  )}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Đối tượng quản trị">
                <Space size={4}>
                  {getResourceIcon(selectedLog.resourceType)}
                  <Text strong>{formatResourceType(selectedLog.resourceType)}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Hành động">
                <Tag
                  color={formatActionType(selectedLog.actionType).color}
                  style={{ fontWeight: 600 }}
                >
                  {formatActionType(selectedLog.actionType).label}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Mã đối tượng (ID)">
                {selectedLog.resourceId ? (
                  <Text
                    copyable={{
                      text: String(selectedLog.resourceId),
                      tooltips: ['Sao chép mã đầy đủ', 'Đã sao chép!'],
                    }}
                    style={{
                      margin: 0,
                      fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
                      fontSize: 12,
                    }}
                  >
                    {String(selectedLog.resourceId)}
                  </Text>
                ) : (
                  '—'
                )}
              </Descriptions.Item>
            </Descriptions>

            <Divider style={{ margin: '16px 0 20px 0' }} />

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <Text strong style={{ fontSize: 15, color: '#1e3a8a' }}>
                {isCreateAction
                  ? 'Giá trị được khởi tạo mới:'
                  : 'Bảng đối chiếu giá trị (Trước vs Sau khi thao tác):'}
              </Text>
              {!isCreateAction && changedCount > 0 && (
                <Checkbox
                  checked={onlyShowChanged}
                  onChange={(e) => setOnlyShowChanged(e.target.checked)}
                  style={{ fontSize: 13 }}
                >
                  Chỉ trường thay đổi ({changedCount})
                </Checkbox>
              )}
            </div>

            {/* Content rendering */}
            {!detailData.isValid ? (
              <Alert
                type="info"
                showIcon
                message="Thông tin chi tiết"
                description="Không thể hiển thị chi tiết thay đổi cho bản ghi này."
              />
            ) : displayedDiffRows.length === 0 ? (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Không có trường thông tin nào thay đổi được ghi nhận."
              />
            ) : (
              <div style={{ border: '1px solid #e5e7eb', borderRadius: 8, overflow: 'hidden' }}>
                <Table
                  dataSource={displayedDiffRows}
                  rowKey="field"
                  pagination={false}
                  size="small"
                  columns={[
                    {
                      title: 'Trường thông tin',
                      key: 'label',
                      width: 160,
                      render: (_, row) => (
                        <div>
                          <div style={{ fontWeight: 600, color: '#1f2937' }}>{row.label}</div>
                          <div style={{ fontSize: 11, color: '#9ca3af', fontFamily: 'monospace' }}>
                            {row.field}
                          </div>
                        </div>
                      ),
                    },
                    {
                      title: 'Trước khi sửa (before)',
                      key: 'oldValue',
                      width: 200,
                      render: (_, row) => {
                        const formatted = formatFieldValue(row.oldValue)
                        return (
                          <div
                            style={{
                              padding: '4px 8px',
                              borderRadius: 4,
                              backgroundColor: row.changed && row.oldValue !== undefined ? '#fef2f2' : 'transparent',
                              color: row.changed && row.oldValue !== undefined ? '#991b1b' : '#4b5563',
                              fontSize: 12.5,
                              lineHeight: 1.4,
                              wordBreak: 'break-word',
                            }}
                          >
                            {formatted}
                          </div>
                        )
                      },
                    },
                    {
                      title: 'Sau khi sửa (after)',
                      key: 'newValue',
                      width: 200,
                      render: (_, row) => {
                        const formatted = formatFieldValue(row.newValue)
                        return (
                          <div
                            style={{
                              padding: '4px 8px',
                              borderRadius: 4,
                              backgroundColor: row.changed ? '#f0fdf4' : 'transparent',
                              color: row.changed ? '#166534' : '#111827',
                              fontWeight: row.changed ? 600 : 400,
                              fontSize: 12.5,
                              lineHeight: 1.4,
                              wordBreak: 'break-word',
                            }}
                          >
                            {formatted}
                          </div>
                        )
                      },
                    },
                  ]}
                />
              </div>
            )}

            {/* Raw JSON detail inspection */}
            {selectedLog.detail && (
              <div style={{ marginTop: 24 }}>
                <details style={{ fontSize: 12, color: '#6b7280', cursor: 'pointer' }}>
                  <summary style={{ fontWeight: 500, marginBottom: 8 }}>
                    Xem chuỗi dữ liệu gốc (Raw JSON detail)
                  </summary>
                  <pre
                    style={{
                      backgroundColor: '#f8fafc',
                      padding: 12,
                      borderRadius: 6,
                      border: '1px solid #e2e8f0',
                      fontSize: 11.5,
                      fontFamily: 'monospace',
                      maxHeight: 200,
                      overflow: 'auto',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                    }}
                  >
                    {selectedLog.detail}
                  </pre>
                </details>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}
