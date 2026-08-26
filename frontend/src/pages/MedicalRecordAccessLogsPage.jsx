import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Divider,
  Empty,
  Input,
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
  AuditOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CopyOutlined,
  EditOutlined,
  EyeOutlined,
  FileDoneOutlined,
  FileProtectOutlined,
  HistoryOutlined,
  LockOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
import patientApi from '../api/patientApi'
import userApi from '../api/userApi'

const { Title, Text, Paragraph } = Typography
const { RangePicker } = DatePicker

const ACTION_CONFIG = {
  VIEW: {
    label: 'Xem bệnh án',
    color: 'blue',
    icon: <EyeOutlined />,
    category: 'READ',
  },
  VIEW_HISTORY: {
    label: 'Xem lịch sử',
    color: 'cyan',
    icon: <HistoryOutlined />,
    category: 'READ',
  },
  CREATE: {
    label: 'Tạo bệnh án',
    color: 'green',
    icon: <FileDoneOutlined />,
    category: 'WRITE',
  },
  UPDATE: {
    label: 'Cập nhật',
    color: 'orange',
    icon: <EditOutlined />,
    category: 'WRITE',
  },
  LOCK: {
    label: 'Khóa bệnh án',
    color: 'purple',
    icon: <LockOutlined />,
    category: 'SECURITY',
  },
  AMEND: {
    label: 'Tu chỉnh / Bổ sung',
    color: 'volcano',
    icon: <FileProtectOutlined />,
    category: 'WRITE',
  },
  EXPORT: {
    label: 'Xuất dữ liệu',
    color: 'magenta',
    icon: <AuditOutlined />,
    category: 'SECURITY',
  },
}

const getErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback

function MedicalRecordAccessLogsPage() {
  const [patients, setPatients] = useState([])
  const [users, setUsers] = useState([])
  const [selectedPatientId, setSelectedPatientId] = useState(null)
  const [accessLogs, setAccessLogs] = useState([])
  const [loading, setLoading] = useState(false)
  const [patientLoading, setPatientLoading] = useState(false)
  const [loadError, setLoadError] = useState('')

  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)

  const [actionFilter, setActionFilter] = useState('ALL')
  const [userFilter, setUserFilter] = useState('ALL')
  const [dateRange, setDateRange] = useState(null)

  const loadInitialData = useCallback(async () => {
    setPatientLoading(true)
    try {
      const [patientRes, userRes] = await Promise.allSettled([
        patientApi.search ? patientApi.search({ size: 100 }) : patientApi.getAll(),
        userApi.list ? userApi.list() : userApi.getAll(),
      ])

      let loadedPatients = []
      if (patientRes.status === 'fulfilled') {
        const payload = patientRes.value?.data
        loadedPatients = Array.isArray(payload?.content)
          ? payload.content
          : Array.isArray(payload)
          ? payload
          : []
        setPatients(loadedPatients)
        if (loadedPatients.length > 0 && !selectedPatientId) {
          setSelectedPatientId(loadedPatients[0].id)
        }
      }

      if (userRes.status === 'fulfilled') {
        const userPayload = userRes.value?.data
        const userList = Array.isArray(userPayload)
          ? userPayload
          : Array.isArray(userPayload?.content)
          ? userPayload.content
          : []
        setUsers(userList)
      }
    } catch (err) {
      console.error('Lỗi tải danh mục người dùng/bệnh nhân:', err)
    } finally {
      setPatientLoading(false)
    }
  }, [selectedPatientId])

  useEffect(() => {
    loadInitialData()
  }, [loadInitialData])

  const userMap = useMemo(() => {
    const map = new Map()
    users.forEach((u) => {
      if (u?.id) map.set(String(u.id), u)
    })
    return map
  }, [users])

  const patientMap = useMemo(() => {
    const map = new Map()
    patients.forEach((p) => {
      if (p?.id) map.set(String(p.id), p)
    })
    return map
  }, [patients])

  const loadAccessLogs = useCallback(async () => {
    if (!selectedPatientId) return

    setLoading(true)
    setLoadError('')
    try {
      const params = {
        page,
        size: pageSize,
      }

      if (dateRange && dateRange[0] && dateRange[1]) {
        params.from = dateRange[0].startOf('day').toISOString()
        params.to = dateRange[1].endOf('day').toISOString()
      }

      const response = await medicalRecordApi.getAccessLogsByPatient(selectedPatientId, params)
      const data = response.data

      let rawLogs = []
      if (Array.isArray(data?.content)) {
        rawLogs = data.content
        setTotalElements(data.totalElements || rawLogs.length)
      } else if (Array.isArray(data)) {
        rawLogs = data
        setTotalElements(rawLogs.length)
      } else {
        rawLogs = []
        setTotalElements(0)
      }

      setAccessLogs(rawLogs)
    } catch (error) {
      if (error?.response?.status !== 403) {
        setLoadError(getErrorMessage(error, 'Không thể tải nhật ký truy cập bệnh án.'))
      } else {
        setLoadError('')
      }
      setAccessLogs([])
    } finally {
      setLoading(false)
    }
  }, [selectedPatientId, page, pageSize, dateRange])

  useEffect(() => {
    loadAccessLogs()
  }, [loadAccessLogs])

  const filteredLogs = useMemo(() => {
    let list = accessLogs

    if (actionFilter !== 'ALL') {
      list = list.filter((item) => String(item.action).toUpperCase() === actionFilter)
    }

    if (userFilter !== 'ALL') {
      list = list.filter((item) => String(item.accessedBy) === String(userFilter))
    }

    return list
  }, [accessLogs, actionFilter, userFilter])

  const stats = useMemo(() => {
    const total = totalElements || accessLogs.length
    const reads = accessLogs.filter((l) => ['VIEW', 'VIEW_HISTORY'].includes(l.action)).length
    const writes = accessLogs.filter((l) => ['CREATE', 'UPDATE', 'AMEND'].includes(l.action)).length
    const security = accessLogs.filter((l) => ['LOCK', 'EXPORT'].includes(l.action)).length

    return {
      total,
      reads,
      writes,
      security,
    }
  }, [totalElements, accessLogs])

  const selectedPatientObj = patientMap.get(String(selectedPatientId))

  const columns = [
    {
      title: 'Thời gian truy cập',
      dataIndex: 'accessedAt',
      key: 'accessedAt',
      width: 190,
      render: (val) => {
        if (!val) return '—'
        const time = dayjs(val)
        return (
          <Space direction="vertical" size={1}>
            <span style={{ fontWeight: 600, color: '#1e293b' }}>{time.format('HH:mm:ss DD/MM/YYYY')}</span>
            <Text type="secondary" style={{ fontSize: 12 }}>
              <ClockCircleOutlined style={{ marginRight: 4 }} />
              {time.fromNow?.() || time.format('DD/MM/YYYY')}
            </Text>
          </Space>
        )
      },
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'accessedBy',
      key: 'accessedBy',
      width: 220,
      render: (userId) => {
        const u = userMap.get(String(userId))
        if (!u) {
          return (
            <Space>
              <Avatar size="small" icon={<UserOutlined />} />
              <Text code style={{ fontSize: 12 }}>{String(userId || 'Hệ thống').slice(-8)}</Text>
            </Space>
          )
        }

        const roleText = (u.roles || [u.role]).filter(Boolean).map((r) =>
          String(r).toUpperCase().replace(/^ROLE_/, '')
        ).join(', ')

        return (
          <Space align="start">
            <Avatar size="small" style={{ backgroundColor: '#1677ff', marginTop: 2 }}>
              {(u.fullName || u.username || 'U').charAt(0).toUpperCase()}
            </Avatar>
            <Space direction="vertical" size={0}>
              <strong>{u.fullName || u.username}</strong>
              <Text type="secondary" style={{ fontSize: 12 }}>
                @{u.username} · <Tag color="blue" style={{ fontSize: 11, margin: 0 }}>{roleText || 'Người dùng'}</Tag>
              </Text>
            </Space>
          </Space>
        )
      },
    },
    {
      title: 'Hành động',
      dataIndex: 'action',
      key: 'action',
      width: 170,
      render: (action) => {
        const config = ACTION_CONFIG[action] || {
          label: action || 'Thao tác',
          color: 'default',
          icon: <AuditOutlined />,
        }
        return (
          <Tag color={config.color} icon={config.icon} style={{ padding: '4px 10px', fontSize: 13 }}>
            {config.label}
          </Tag>
        )
      },
    },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      width: 200,
      render: (_, record) => {
        const p = patientMap.get(String(record.patientId)) || selectedPatientObj
        return (
          <Space direction="vertical" size={0}>
            <strong>{p?.fullName || 'Bệnh nhân'}</strong>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Mã BN: {p?.patientCode || String(record.patientId || '').slice(-8)}
            </Text>
          </Space>
        )
      },
    },
    {
      title: 'Lượt khám / Bệnh án',
      key: 'identifiers',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          {record.medicalRecordId && (
            <Space size={4}>
              <Text type="secondary" style={{ fontSize: 11 }}>Hồ sơ:</Text>
              <Text code style={{ fontSize: 12 }}>{String(record.medicalRecordId).slice(-8)}</Text>
              <Tooltip title="Sao chép mã bệnh án">
                <Button
                  type="text"
                  size="small"
                  icon={<CopyOutlined />}
                  onClick={() => {
                    navigator.clipboard?.writeText(record.medicalRecordId)
                    message.success('Đã sao chép mã bệnh án')
                  }}
                />
              </Tooltip>
            </Space>
          )}
          {record.visitId && (
            <Space size={4}>
              <Text type="secondary" style={{ fontSize: 11 }}>Lượt khám:</Text>
              <Text code style={{ fontSize: 12 }}>{String(record.visitId).slice(-8)}</Text>
            </Space>
          )}
        </Space>
      ),
    },
    {
      title: 'Chi tiết thao tác',
      dataIndex: 'detail',
      key: 'detail',
      render: (text) => (
        <Text style={{ color: '#334155', whiteSpace: 'pre-wrap' }}>
          {text || 'Xem chi tiết thông tin bệnh án y tế nhạy cảm'}
        </Text>
      ),
    },
  ]

  return (
    <div style={{ paddingBottom: 32 }}>
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
          <Title level={3} style={{ margin: 0 }}>
            <SafetyCertificateOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Nhật ký truy cập bệnh án & Dữ liệu y tế (Audit Logs)
          </Title>
          <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
            Giám sát minh bạch mọi lần xem, sửa, tạo, khóa hoặc bổ sung thông tin bệnh án theo người dùng và mốc thời gian.
          </Paragraph>
        </div>
        <Space wrap>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadAccessLogs}>
            Làm mới
          </Button>
        </Space>
      </div>

      {loadError && (
        <Alert
          type="error"
          showIcon
          message="Lỗi truy vấn nhật ký"
          description={loadError}
          action={<Button size="small" onClick={loadAccessLogs}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Tổng lượt truy cập ghi nhận"
              value={stats.total}
              prefix={<AuditOutlined style={{ color: '#1677ff' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Lượt xem dữ liệu bệnh án"
              value={stats.reads}
              valueStyle={{ color: '#0284c7' }}
              prefix={<EyeOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Lượt chỉnh sửa / Tạo mới"
              value={stats.writes}
              valueStyle={{ color: '#ea580c' }}
              prefix={<EditOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Lượt khóa & Bảo mật hồ sơ"
              value={stats.security}
              valueStyle={{ color: '#7c3aed' }}
              prefix={<LockOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card
        size="small"
        style={{ marginBottom: 16, borderRadius: 8, background: '#f8fafc', borderColor: '#e2e8f0' }}
      >
        <Row gutter={[12, 12]} align="middle">
          <Col xs={24} md={8}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>
              Chọn Bệnh nhân cần tra cứu nhật ký:
            </Text>
            <Select
              showSearch
              placeholder="Tìm theo tên bệnh nhân, mã BN..."
              value={selectedPatientId}
              onChange={(val) => {
                setSelectedPatientId(val)
                setPage(0)
              }}
              loading={patientLoading}
              style={{ width: '100%' }}
              optionFilterProp="label"
              options={patients.map((p) => ({
                value: p.id,
                label: `${p.patientCode || 'BN'} · ${p.fullName} (${p.phone || p.dateOfBirth || ''})`,
              }))}
            />
          </Col>

          <Col xs={24} sm={12} md={5}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>
              Người thực hiện:
            </Text>
            <Select
              defaultValue="ALL"
              value={userFilter}
              onChange={setUserFilter}
              style={{ width: '100%' }}
              options={[
                { value: 'ALL', label: 'Tất cả người dùng' },
                ...users.map((u) => ({
                  value: u.id,
                  label: `${u.fullName || u.username} (${u.roles?.[0] || u.role || 'User'})`,
                })),
              ]}
            />
          </Col>

          <Col xs={24} sm={12} md={5}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>
              Loại hành động:
            </Text>
            <Select
              defaultValue="ALL"
              value={actionFilter}
              onChange={setActionFilter}
              style={{ width: '100%' }}
              options={[
                { value: 'ALL', label: 'Tất cả hành động' },
                { value: 'VIEW', label: '👁️ VIEW - Xem bệnh án' },
                { value: 'VIEW_HISTORY', label: '📜 VIEW_HISTORY - Xem lịch sử' },
                { value: 'CREATE', label: '➕ CREATE - Tạo bệnh án' },
                { value: 'UPDATE', label: '✏️ UPDATE - Cập nhật' },
                { value: 'LOCK', label: '🔒 LOCK - Khóa bệnh án' },
                { value: 'AMEND', label: '📝 AMEND - Tu chỉnh bổ sung' },
                { value: 'EXPORT', label: '📤 EXPORT - Xuất dữ liệu' },
              ]}
            />
          </Col>

          <Col xs={24} md={6}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>
              Khoảng thời gian:
            </Text>
            <RangePicker
              style={{ width: '100%' }}
              format="DD/MM/YYYY"
              value={dateRange}
              onChange={(dates) => {
                setDateRange(dates)
                setPage(0)
              }}
              presets={[
                { label: 'Hôm nay', value: [dayjs().startOf('day'), dayjs().endOf('day')] },
                { label: '7 ngày qua', value: [dayjs().subtract(7, 'day'), dayjs()] },
                { label: '30 ngày qua', value: [dayjs().subtract(30, 'day'), dayjs()] },
              ]}
            />
          </Col>
        </Row>
      </Card>

      <Card style={{ borderRadius: 8 }}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={filteredLogs}
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize,
            total: totalElements,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            showTotal: (total) => `Tổng số ${total} bản ghi nhật ký truy cập`,
            onChange: (p, s) => {
              setPage(p - 1)
              setPageSize(s)
            },
          }}
          scroll={{ x: 1050 }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  selectedPatientId
                    ? 'Chưa ghi nhận nhật ký truy cập nào cho bệnh nhân này theo tiêu chí lọc'
                    : 'Vui lòng chọn một bệnh nhân để xem nhật ký truy cập'
                }
              />
            ),
          }}
        />
      </Card>
    </div>
  )
}

export default MedicalRecordAccessLogsPage
