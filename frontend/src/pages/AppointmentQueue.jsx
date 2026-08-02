import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  Modal,
  Popconfirm,
  Radio,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  BellOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  FilterOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ReloadOutlined,
  RightCircleOutlined,
  SearchOutlined,
  StepForwardOutlined,
  TeamOutlined,
  UserAddOutlined,
  UserOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import appointmentApi from '../api/appointmentApi'
import patientApi from '../api/patientApi'
import queueApi from '../api/queueApi'
import userApi from '../api/userApi'
import { useAuthContext } from '../context/AuthContext'
import {
  APPOINTMENT_STATUS_META,
  QUEUE_STATUS_META,
  checkQueuePermissions,
  handleQueueApiError,
} from '../utils/queueHelpers'
import {
  getAppointments,
  getPatients,
} from '../services/mockDataService'
import {
  mergeAppointments,
  mergePatients,
  saveStoredAppointment,
} from '../utils/storageHelpers'

const { Text, Title, Paragraph } = Typography

// Preset departments
const departmentOptions = [
  'Nội tổng quát',
  'Ngoại khoa',
  'Nhi khoa',
  'Sản phụ khoa',
  'Tim mạch',
  'Tai Mũi Họng',
  'Răng Hàm Mặt',
  'Mắt',
]

const avatarPalette = [
  ['#e7f0ff', '#1c68ce'],
  ['#fff0e5', '#bf6b32'],
  ['#e8f7ef', '#21835a'],
  ['#f1eaff', '#7541b7'],
]

const getInitials = (name = '') =>
  name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()

const getAvatarStyle = (seed = '') => {
  const paletteIndex =
    [...String(seed)].reduce((sum, character) => sum + character.charCodeAt(0), 0) % avatarPalette.length
  const [background, color] = avatarPalette[paletteIndex]
  return { background, color }
}

function AppointmentQueue() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  // Phân quyền theo vai trò (Bác sĩ, Lễ tân, Điều dưỡng, Admin)
  const permissions = useMemo(() => checkQueuePermissions(user?.roles || []), [user?.roles])

  // State quản lý danh sách & bộ lọc
  const [activeMainTab, setActiveMainTab] = useState('appointments') // 'appointments' | 'reception_queue' | 'doctor_queue'
  const [selectedDate, setSelectedDate] = useState(dayjs())
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  // Sub-data states
  const [appointments, setAppointments] = useState([])
  const [queues, setQueues] = useState([])
  const [myQueueData, setMyQueueData] = useState(null)
  const [patients, setPatients] = useState([])
  const [doctors, setDoctors] = useState([])

  // Filters for Appointments
  const [appStatusFilter, setAppStatusFilter] = useState('ALL')
  const [appDoctorFilter, setAppDoctorFilter] = useState('ALL')
  const [appKeyword, setAppKeyword] = useState('')

  // Filters for Queue Board (Lễ tân)
  const [queueDoctorFilter, setQueueDoctorFilter] = useState('ALL')
  const [queueRoomFilter, setQueueRoomFilter] = useState('ALL')
  const [queueStatusFilter, setQueueStatusFilter] = useState('ALL')
  const [queueSourceFilter, setQueueSourceFilter] = useState('ALL')
  const [queueKeyword, setQueueKeyword] = useState('')

  // Modals
  const [bookModalOpen, setBookModalOpen] = useState(false)
  const [walkInModalOpen, setWalkInModalOpen] = useState(false)
  const [skipModalItem, setSkipModalItem] = useState(null)
  const [detailItem, setDetailItem] = useState(null)

  // Forms
  const [bookForm] = Form.useForm()
  const [walkInForm] = Form.useForm()
  const [skipForm] = Form.useForm()

  // Tải danh sách Bệnh nhân & Bác sĩ
  const loadDirectories = useCallback(async () => {
    try {
      const [patientRes, doctorRes] = await Promise.allSettled([
        patientApi.getAll({ page: 0, size: 300 }),
        userApi.getDoctors(),
      ])

      if (patientRes.status === 'fulfilled' && Array.isArray(patientRes.value?.data?.content || patientRes.value?.data)) {
        const list = patientRes.value?.data?.content || patientRes.value?.data
        setPatients(mergePatients(list))
      } else {
        setPatients(mergePatients(getPatients()))
      }

      if (doctorRes.status === 'fulfilled' && Array.isArray(doctorRes.value?.data)) {
        setDoctors(doctorRes.value.data)
      } else {
        setDoctors([
          { id: 'u2', username: 'doctor1', fullName: 'BS. Nguyễn Minh Anh', department: 'Ngoại khoa' },
          { id: 'u3', username: 'doctor2', fullName: 'BS. Trần Quang Huy', department: 'Nội tổng quát' },
          { id: 'u4', username: 'nurse1', fullName: 'BS. Lê Thu Hà', department: 'Nhi khoa' },
        ])
      }
    } catch {
      setPatients(mergePatients(getPatients()))
    }
  }, [])

  // Tải dữ liệu Lịch hẹn (Appointments)
  const loadAppointments = useCallback(async () => {
    try {
      const res = await appointmentApi.getAll()
      const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
      setAppointments(mergeAppointments(list.length ? list : getAppointments()))
    } catch {
      setAppointments(mergeAppointments(getAppointments()))
    }
  }, [])

  // Tải dữ liệu Queue Board (GET /queues)
  const loadQueues = useCallback(async () => {
    try {
      const params = {
        date: selectedDate.format('YYYY-MM-DD'),
        doctorId: queueDoctorFilter !== 'ALL' ? queueDoctorFilter : undefined,
        roomId: queueRoomFilter !== 'ALL' ? queueRoomFilter : undefined,
        status: queueStatusFilter !== 'ALL' ? queueStatusFilter : undefined,
        sourceType: queueSourceFilter !== 'ALL' ? queueSourceFilter : undefined,
      }
      const res = await queueApi.getQueues(params)
      const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
      setQueues(list)
    } catch (err) {
      // Fallback local Queue items if BE is offline
      setQueues([])
    }
  }, [selectedDate, queueDoctorFilter, queueRoomFilter, queueStatusFilter, queueSourceFilter])

  // Tải dữ liệu Queue Bác sĩ (GET /queues/me)
  const loadMyQueue = useCallback(async () => {
    try {
      const res = await queueApi.getMyQueue({ date: selectedDate.format('YYYY-MM-DD') })
      setMyQueueData(res.data)
    } catch {
      setMyQueueData(null)
    }
  }, [selectedDate])

  // Reload tất cả dữ liệu
  const refreshAllData = useCallback(async () => {
    setLoading(true)
    await Promise.all([loadAppointments(), loadQueues(), loadMyQueue()])
    setLoading(false)
  }, [loadAppointments, loadQueues, loadMyQueue])

  useEffect(() => {
    loadDirectories()
    refreshAllData()
  }, [loadDirectories, refreshAllData])

  // Polling tự động mỗi 20 giây cho Queue Board
  useEffect(() => {
    const timer = setInterval(() => {
      loadQueues()
      if (permissions.canComplete) loadMyQueue()
    }, 20000)
    return () => clearInterval(timer)
  }, [loadQueues, loadMyQueue, permissions.canComplete])

  // Trích xuất danh sách Phòng khám từ các lượt khám trong Queue
  const extractedRooms = useMemo(() => {
    const map = new Map()
    queues.forEach((q) => {
      const rId = q.roomId || q.roomCode || q.id
      if (rId && !map.has(String(rId))) {
        map.set(String(rId), {
          id: rId,
          code: q.roomCode || `P${rId}`,
          name: q.roomName || `Phòng khám ${q.roomCode || rId}`,
        })
      }
    })
    // Seed phòng khám mặc định nếu chưa có
    if (!map.size) {
      map.set('90000000-0000-0000-0000-000000000001', { id: '90000000-0000-0000-0000-000000000001', code: 'P101', name: 'Phòng khám 101' })
      map.set('90000000-0000-0000-0000-000000000002', { id: '90000000-0000-0000-0000-000000000002', code: 'P102', name: 'Phòng khám 102' })
    }
    return Array.from(map.values())
  }, [queues])

  // Bệnh nhân được lọc cho Tab Lịch hẹn
  const filteredAppointments = useMemo(() => {
    const kw = appKeyword.trim().toLowerCase()
    return appointments.filter((app) => {
      const isDateMatch = !app.appointmentAt || dayjs(app.appointmentAt).isSame(selectedDate, 'day')
      const isDocMatch = appDoctorFilter === 'ALL' || String(app.doctorId) === String(appDoctorFilter)
      const isStatusMatch = appStatusFilter === 'ALL' || app.status === appStatusFilter
      const textMatch = !kw || [app.appointmentCode, app.patientName, app.doctorName, app.reason]
        .some((t) => String(t || '').toLowerCase().includes(kw))

      return isDateMatch && isDocMatch && isStatusMatch && textMatch
    })
  }, [appointments, selectedDate, appDoctorFilter, appStatusFilter, appKeyword])

  // Bệnh nhân được lọc cho Tab Queue Board Lễ tân
  const filteredQueues = useMemo(() => {
    const kw = queueKeyword.trim().toLowerCase()
    return queues.filter((q) => {
      const textMatch = !kw || [q.patientName, q.patientCode, q.visitCode, q.doctorName, q.roomName, q.id]
        .some((t) => String(t || '').toLowerCase().includes(kw))
      return textMatch
    })
  }, [queues, queueKeyword])

  // Nhóm bệnh nhân cho Queue Bác sĩ (/queues/me)
  const doctorQueueGroups = useMemo(() => {
    const items = myQueueData?.items || queues.filter((q) => !permissions.canComplete || String(q.doctorId) === String(user?.id))
    return {
      inProgress: items.filter((q) => q.status === 'IN_PROGRESS'),
      waiting: items.filter((q) => q.status === 'WAITING'),
      waitingForResult: items.filter((q) => q.status === 'WAITING_FOR_RESULT'),
      finished: items.filter((q) => ['COMPLETED', 'SKIPPED', 'CANCELLED'].includes(q.status)),
    }
  }, [myQueueData, queues, permissions.canComplete, user?.id])

  // Thao tác 1: Check-in Lịch hẹn (SCHEDULED -> CHECKED_IN)
  const handleCheckInAppointment = async (appId) => {
    setActionLoading(true)
    try {
      await queueApi.checkInAppointment(appId)
      handleQueueApiError(null)
      refreshAllData()
    } catch (err) {
      // Fallback local update if BE returns 404 mock
      setAppointments((prev) =>
        prev.map((item) => (String(item.id) === String(appId) ? { ...item, status: 'CHECKED_IN' } : item))
      )
      handleQueueApiError(err, 'Check-in lịch hẹn thành công (Đã cập nhật trạng thái)')
      refreshAllData()
    } finally {
      setActionLoading(false)
    }
  }

  // Thao tác 2: Check-in Bệnh nhân Walk-in
  const handleCheckInWalkInSubmit = async (values) => {
    setActionLoading(true)
    try {
      const payload = {
        patientId: values.patientId,
        doctorId: values.doctorId,
        reason: values.reason,
        notes: values.notes || '',
      }
      await queueApi.checkInWalkIn(payload)
      setWalkInModalOpen(false)
      walkInForm.resetFields()
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể tiếp nhận lượt khám Walk-in')
    } finally {
      setActionLoading(false)
    }
  }

  // Thao tác 3: Gọi người tiếp theo (Call Next)
  const handleCallNext = async (queueId) => {
    setActionLoading(true)
    try {
      const qId = queueId || myQueueData?.id || queues[0]?.medicalQueueId || queues[0]?.queueId
      if (!qId) {
        handleQueueApiError({ response: { status: 404, data: { message: 'Không tìm thấy hàng đợi khám phù hợp để gọi tiếp.' } } })
        return
      }
      await queueApi.callNext(qId)
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể gọi lượt khám tiếp theo')
    } finally {
      setActionLoading(false)
    }
  }

  // Thao tác 4: Bỏ qua / Vắng mặt khi gọi (Skip)
  const handleSkipSubmit = async (values) => {
    if (!skipModalItem) return
    setActionLoading(true)
    try {
      await queueApi.skip(skipModalItem.id, values.reason || 'Vắng mặt khi gọi')
      setSkipModalItem(null)
      skipForm.resetFields()
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể thực hiện bỏ qua lượt khám')
    } finally {
      setActionLoading(false)
    }
  }

  // Thao tác 5: Chuyển trạng thái lượt khám (VD: Chờ CĐLS hoặc Tiếp tục khám)
  const handleUpdateItemStatus = async (itemId, newStatus) => {
    setActionLoading(true)
    try {
      await queueApi.updateStatus(itemId, { status: newStatus })
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, `Không thể chuyển trạng thái lượt khám sang ${newStatus}`)
    } finally {
      setActionLoading(false)
    }
  }

  // Thao tác 6: Hoàn tất lượt khám (Complete)
  const handleCompleteItem = async (itemId) => {
    setActionLoading(true)
    try {
      await queueApi.complete(itemId)
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Bệnh án chưa được khóa hoặc sai chu trình khám.')
    } finally {
      setActionLoading(false)
    }
  }

  // Render Cột cho Bảng Lịch hẹn (Appointments Table)
  const appointmentColumns = [
    {
      title: 'Mã lịch hẹn',
      dataIndex: 'appointmentCode',
      key: 'appointmentCode',
      render: (code) => <Text strong style={{ color: '#2563eb' }}>{code || 'LH-N/A'}</Text>,
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      render: (name, record) => (
        <Space>
          <Avatar style={getAvatarStyle(name)}>{getInitials(name)}</Avatar>
          <div>
            <Text strong block>{name || 'Bệnh nhân'}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>{record.phone || record.patientCode || ''}</Text>
          </div>
        </Space>
      ),
    },
    {
      title: 'Bác sĩ & Phòng',
      dataIndex: 'doctorName',
      key: 'doctorName',
      render: (doc, record) => (
        <div>
          <Text block>{doc || 'BS. Chưa phân công'}</Text>
          <Tag color="cyan">{record.department || 'Nội tổng quát'}</Tag>
        </div>
      ),
    },
    {
      title: 'Ngày & Giờ hẹn',
      dataIndex: 'appointmentAt',
      key: 'appointmentAt',
      render: (at) => (at ? dayjs(at).format('HH:mm - DD/MM/YYYY') : 'Trong ngày'),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st) => {
        const meta = APPOINTMENT_STATUS_META[st] || { label: st, tone: 'gray' }
        return <Tag color={meta.tone}>{meta.label}</Tag>
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_, record) => (
        <Space>
          {/* Nút Check-in CHỈ xuất hiện khi trạng thái là SCHEDULED */}
          {record.status === 'SCHEDULED' && permissions.canCheckIn && (
            <Button
              type="primary"
              size="small"
              icon={<CheckCircleOutlined />}
              loading={actionLoading}
              onClick={() => handleCheckInAppointment(record.id)}
            >
              Check-in
            </Button>
          )}

          {record.status === 'CHECKED_IN' && (
            <Tag color="processing" icon={<ClockCircleOutlined />}>Đã vào hàng đợi</Tag>
          )}

          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => setDetailItem({ type: 'appointment', ...record })}
          >
            Chi tiết
          </Button>
        </Space>
      ),
    },
  ]

  // Render Cột cho Bảng Queue Board Lễ tân (Reception Queue Table)
  const queueBoardColumns = [
    {
      title: 'STT',
      dataIndex: 'queueNumber',
      key: 'queueNumber',
      width: 70,
      render: (num, _, idx) => <Badge count={num || idx + 1} style={{ backgroundColor: '#2563eb' }} />,
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      render: (name, record) => (
        <Space>
          <Avatar style={getAvatarStyle(name)}>{getInitials(name)}</Avatar>
          <div>
            <Text strong block>{name || 'Bệnh nhân'}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>Mã: {record.patientCode || record.patientId || 'N/A'}</Text>
          </div>
        </Space>
      ),
    },
    {
      title: 'Mã lượt khám',
      dataIndex: 'visitCode',
      key: 'visitCode',
      render: (val, record) => <Text code>{val || record.visitId || record.id || 'N/A'}</Text>,
    },
    {
      title: 'Nguồn',
      dataIndex: 'sourceType',
      key: 'sourceType',
      render: (src) => (
        src === 'WALK_IN'
          ? <Tag color="orange">Tự đến (Walk-in)</Tag>
          : <Tag color="blue">Hẹn trước</Tag>
      ),
    },
    {
      title: 'Bác sĩ',
      dataIndex: 'doctorName',
      key: 'doctorName',
      render: (doc) => doc || 'Chưa gán',
    },
    {
      title: 'Phòng',
      dataIndex: 'roomName',
      key: 'roomName',
      render: (room, record) => room || record.roomCode || 'Phòng khám',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st) => {
        const meta = QUEUE_STATUS_META[st] || { label: st, tone: 'gray' }
        return <Tag color={meta.tone}>{meta.label}</Tag>
      },
    },
    {
      title: 'Thời gian đến',
      dataIndex: 'checkedInAt',
      key: 'checkedInAt',
      render: (time) => (time ? dayjs(time).format('HH:mm DD/MM') : 'N/A'),
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_, record) => (
        <Space wrap>
          {/* Lễ tân / Admin Thao tác */}
          {!permissions.isNurseOnly && record.status === 'WAITING' && (
            <Button
              type="primary"
              size="small"
              icon={<StepForwardOutlined />}
              onClick={() => handleCallNext(record.medicalQueueId || record.queueId)}
            >
              Gọi khám
            </Button>
          )}

          {!permissions.isNurseOnly && record.status === 'IN_PROGRESS' && permissions.canSkip && (
            <Button
              danger
              size="small"
              icon={<CloseCircleOutlined />}
              onClick={() => {
                skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                setSkipModalItem(record)
              }}
            >
              Skip
            </Button>
          )}

          {/* Điều dưỡng (Nurse) Thao tác chuyển trạng thái CĐLS */}
          {permissions.canChangeResultStatus && record.status === 'IN_PROGRESS' && (
            <Button
              size="small"
              style={{ borderColor: '#9333ea', color: '#9333ea' }}
              onClick={() => handleUpdateItemStatus(record.id, 'WAITING_FOR_RESULT')}
            >
              Chờ CĐLS
            </Button>
          )}

          {permissions.canChangeResultStatus && record.status === 'WAITING_FOR_RESULT' && (
            <Button
              size="small"
              type="primary"
              style={{ backgroundColor: '#16a34a' }}
              onClick={() => handleUpdateItemStatus(record.id, 'IN_PROGRESS')}
            >
              Tiếp tục khám
            </Button>
          )}

          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => setDetailItem({ type: 'queue', ...record })}
          >
            Chi tiết
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24, maxWidth: 1400, margin: '0 auto' }}>
      {/* Header & Title */}
      <Card style={{ marginBottom: 24, borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space align="center" size="middle">
              <Avatar size={48} icon={<CalendarOutlined />} style={{ backgroundColor: '#2563eb' }} />
              <div>
                <Title level={4} style={{ margin: 0 }}>Quản Lý Lịch Hẹn & Hàng Đợi Khám Bệnh</Title>
                <Text type="secondary">
                  Theo dõi luồng vận hành khám bệnh chuẩn hóa từ Lịch hẹn, Check-in, Hàng đợi tới Phòng khám
                </Text>
              </div>
            </Space>
          </Col>
          <Col>
            <Space wrap>
              <DatePicker
                value={selectedDate}
                onChange={(date) => date && setSelectedDate(date)}
                format="DD/MM/YYYY"
                allowClear={false}
              />
              <Button icon={<ReloadOutlined />} onClick={refreshAllData} loading={loading}>
                Làm mới
              </Button>
              {permissions.canManageWalkIn && (
                <Button
                  type="primary"
                  icon={<UserAddOutlined />}
                  onClick={() => setWalkInModalOpen(true)}
                  style={{ backgroundColor: '#16a34a', borderColor: '#16a34a' }}
                >
                  + Check-in Walk-in
                </Button>
              )}
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Thông báo phân quyền nếu là Điều dưỡng */}
      {permissions.isNurseOnly && (
        <Alert
          type="info"
          showIcon
          message="Chế độ Điều dưỡng (Nurse View)"
          description="Bạn có quyền theo dõi Queue Board và chuyển trạng thái Chờ kết quả CĐLS (IN_PROGRESS <-> WAITING_FOR_RESULT). Các thao tác Check-in, Gọi khám, Skip và Hoàn tất do Lễ tân / Bác sĩ thực hiện."
          style={{ marginBottom: 20 }}
        />
      )}

      {/* Main Tabs Navigation */}
      <Tabs
        activeKey={activeMainTab}
        onChange={setActiveMainTab}
        type="card"
        items={[
          {
            key: 'appointments',
            label: (
              <span>
                <CalendarOutlined /> Danh sách Lịch Hẹn ({filteredAppointments.length})
              </span>
            ),
            children: (
              <Card style={{ borderRadius: 12 }}>
                {/* Bộ lọc Lịch hẹn */}
                <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                  <Col xs={24} sm={8} md={6}>
                    <Input
                      placeholder="Tìm mã LH, tên bệnh nhân..."
                      prefix={<SearchOutlined />}
                      value={appKeyword}
                      onChange={(e) => setAppKeyword(e.target.value)}
                      allowClear
                    />
                  </Col>
                  <Col xs={12} sm={8} md={6}>
                    <Select
                      style={{ width: '100%' }}
                      value={appDoctorFilter}
                      onChange={setAppDoctorFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Bác sĩ' },
                        ...doctors.map((d) => ({ value: d.id, label: d.fullName || d.username })),
                      ]}
                    />
                  </Col>
                  <Col xs={12} sm={8} md={6}>
                    <Select
                      style={{ width: '100%' }}
                      value={appStatusFilter}
                      onChange={setAppStatusFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả trạng thái' },
                        { value: 'SCHEDULED', label: 'Đã đặt hẹn (Chờ check-in)' },
                        { value: 'CHECKED_IN', label: 'Đã check-in' },
                        { value: 'COMPLETED', label: 'Đã hoàn thành' },
                        { value: 'CANCELLED', label: 'Đã hủy' },
                      ]}
                    />
                  </Col>
                </Row>

                <Table
                  dataSource={filteredAppointments}
                  columns={appointmentColumns}
                  rowKey="id"
                  loading={loading}
                  pagination={{ pageSize: 10, showSizeChanger: true }}
                />
              </Card>
            ),
          },
          {
            key: 'reception_queue',
            label: (
              <span>
                <TeamOutlined /> Queue Board Lễ Tân & Điều Dưỡng ({filteredQueues.length})
              </span>
            ),
            children: (
              <Card style={{ borderRadius: 12 }}>
                {/* Bộ lọc Queue Board */}
                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                  <Col xs={24} sm={6} md={5}>
                    <Input
                      placeholder="Tìm tên, mã BN, mã lượt..."
                      prefix={<SearchOutlined />}
                      value={queueKeyword}
                      onChange={(e) => setQueueKeyword(e.target.value)}
                      allowClear
                    />
                  </Col>
                  <Col xs={12} sm={6} md={4}>
                    <Select
                      style={{ width: '100%' }}
                      value={queueDoctorFilter}
                      onChange={setQueueDoctorFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Bác sĩ' },
                        ...doctors.map((d) => ({ value: d.id, label: d.fullName || d.username })),
                      ]}
                    />
                  </Col>
                  <Col xs={12} sm={6} md={4}>
                    <Select
                      style={{ width: '100%' }}
                      value={queueRoomFilter}
                      onChange={setQueueRoomFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Phòng khám' },
                        ...extractedRooms.map((r) => ({ value: r.id, label: `${r.code} - ${r.name}` })),
                      ]}
                    />
                  </Col>
                  <Col xs={12} sm={6} md={4}>
                    <Select
                      style={{ width: '100%' }}
                      value={queueStatusFilter}
                      onChange={setQueueStatusFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Trạng thái' },
                        { value: 'WAITING', label: 'WAITING (Đang chờ)' },
                        { value: 'IN_PROGRESS', label: 'IN_PROGRESS (Đang khám)' },
                        { value: 'WAITING_FOR_RESULT', label: 'WAITING_FOR_RESULT (Chờ CĐLS)' },
                        { value: 'COMPLETED', label: 'COMPLETED (Hoàn thành)' },
                        { value: 'SKIPPED', label: 'SKIPPED (Vắng mặt)' },
                        { value: 'CANCELLED', label: 'CANCELLED (Đã hủy)' },
                      ]}
                    />
                  </Col>
                  <Col xs={12} sm={6} md={4}>
                    <Select
                      style={{ width: '100%' }}
                      value={queueSourceFilter}
                      onChange={setQueueSourceFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Nguồn' },
                        { value: 'APPOINTMENT', label: 'Hẹn trước (Appointment)' },
                        { value: 'WALK_IN', label: 'Tự đến (Walk-in)' },
                      ]}
                    />
                  </Col>
                  <Col xs={24} sm={24} md={3} style={{ textAlign: 'right' }}>
                    {permissions.canCallNext && (
                      <Button
                        type="primary"
                        icon={<StepForwardOutlined />}
                        loading={actionLoading}
                        onClick={() => handleCallNext()}
                      >
                        Gọi tiếp
                      </Button>
                    )}
                  </Col>
                </Row>

                <Table
                  dataSource={filteredQueues}
                  columns={queueBoardColumns}
                  rowKey="id"
                  loading={loading}
                  pagination={{ pageSize: 10, showSizeChanger: true }}
                />
              </Card>
            ),
          },
          {
            key: 'doctor_queue',
            label: (
              <span>
                <UserSwitchOutlined /> Queue Khám Bệnh Của Bác Sĩ
              </span>
            ),
            children: (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {/* 1. Nhóm Đang Khám (IN_PROGRESS) */}
                <Card
                  title={<Text strong style={{ color: '#16a34a' }}>🔴 BỆNH NHÂN ĐANG KHÁM ({doctorQueueGroups.inProgress.length})</Text>}
                  style={{ borderRadius: 12, borderColor: '#bbf7d0' }}
                >
                  {doctorQueueGroups.inProgress.length === 0 ? (
                    <Text type="secondary">Chưa có bệnh nhân nào đang trong phòng khám.</Text>
                  ) : (
                    <List
                      dataSource={doctorQueueGroups.inProgress}
                      renderItem={(item) => (
                        <List.Item
                          actions={[
                            <Button
                              type="primary"
                              icon={<MedicineBoxOutlined />}
                              onClick={() => navigate('/medical-records', { state: { patientId: item.patientId, visitId: item.visitId, queueItemId: item.id } })}
                            >
                              Ghi bệnh án / Khám
                            </Button>,
                            <Button
                              style={{ borderColor: '#9333ea', color: '#9333ea' }}
                              onClick={() => handleUpdateItemStatus(item.id, 'WAITING_FOR_RESULT')}
                            >
                              Chờ CĐLS
                            </Button>,
                            <Popconfirm
                              title="Xác nhận hoàn tất lượt khám?"
                              description="Đảm bảo bệnh án đã được Bác sĩ ký/khóa trước khi hoàn tất."
                              onConfirm={() => handleCompleteItem(item.id)}
                            >
                              <Button type="primary" style={{ backgroundColor: '#16a34a' }}>
                                Hoàn tất
                              </Button>
                            </Popconfirm>,
                            <Button
                              danger
                              onClick={() => {
                                skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                                setSkipModalItem(item)
                              }}
                            >
                              Skip
                            </Button>,
                          ]}
                        >
                          <List.Item.Meta
                            avatar={<Avatar size="large" style={getAvatarStyle(item.patientName)}>{getInitials(item.patientName)}</Avatar>}
                            title={<Text strong>{item.patientName} - <Text type="secondary">STT: {item.queueNumber}</Text></Text>}
                            description={`Mã lượt: ${item.visitCode || item.visitId || 'N/A'} | Nguồn: ${item.sourceType === 'WALK_IN' ? 'Walk-in' : 'Hẹn trước'}`}
                          />
                        </List.Item>
                      )}
                    />
                  )}
                </Card>

                {/* 2. Nhóm Đang Chờ (WAITING) */}
                <Card
                  title={<Text strong style={{ color: '#2563eb' }}>🟡 BỆNH NHÂN ĐANG CHỜ ({doctorQueueGroups.waiting.length})</Text>}
                  style={{ borderRadius: 12 }}
                >
                  <List
                    dataSource={doctorQueueGroups.waiting}
                    pagination={{ pageSize: 5 }}
                    renderItem={(item) => (
                      <List.Item
                        actions={[
                          <Button
                            type="primary"
                            icon={<StepForwardOutlined />}
                            onClick={() => handleCallNext(item.medicalQueueId || item.queueId)}
                          >
                            Gọi vào khám
                          </Button>,
                          <Button
                            danger
                            onClick={() => {
                              skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                              setSkipModalItem(item)
                            }}
                          >
                            Skip
                          </Button>,
                        ]}
                      >
                        <List.Item.Meta
                          avatar={<Avatar style={getAvatarStyle(item.patientName)}>{getInitials(item.patientName)}</Avatar>}
                          title={<Text strong>STT {item.queueNumber}: {item.patientName}</Text>}
                          description={`Đến lúc: ${item.checkedInAt ? dayjs(item.checkedInAt).format('HH:mm') : 'N/A'}`}
                        />
                      </List.Item>
                    )}
                  />
                </Card>

                {/* 3. Nhóm Chờ Kết Quả (WAITING_FOR_RESULT) */}
                <Card
                  title={<Text strong style={{ color: '#9333ea' }}>🟣 BỆNH NHÂN CHỜ KẾT QUẢ CĐLS ({doctorQueueGroups.waitingForResult.length})</Text>}
                  style={{ borderRadius: 12, borderColor: '#e9d5ff' }}
                >
                  <List
                    dataSource={doctorQueueGroups.waitingForResult}
                    pagination={{ pageSize: 5 }}
                    renderItem={(item) => (
                      <List.Item
                        actions={[
                          <Button
                            type="primary"
                            style={{ backgroundColor: '#16a34a' }}
                            onClick={() => handleUpdateItemStatus(item.id, 'IN_PROGRESS')}
                          >
                            Tiếp tục khám
                          </Button>,
                          <Button onClick={() => navigate('/medical-records', { state: { patientId: item.patientId, visitId: item.visitId } })}>
                            Xem bệnh án
                          </Button>,
                        ]}
                      >
                        <List.Item.Meta
                          avatar={<Avatar style={{ backgroundColor: '#9333ea' }}>{getInitials(item.patientName)}</Avatar>}
                          title={<Text strong>{item.patientName}</Text>}
                          description="Đã gửi chỉ định CĐLS, đang chờ phòng xét nghiệm / CDHA trả kết quả."
                        />
                      </List.Item>
                    )}
                  />
                </Card>

                {/* 4. Nhóm Đã Hoàn Thành / Bỏ Qua */}
                <Card
                  title={<Text strong style={{ color: '#64748b' }}>⚪ ĐÃ HOÀN THÀNH / BỎ QUA ({doctorQueueGroups.finished.length})</Text>}
                  style={{ borderRadius: 12 }}
                >
                  <Table
                    dataSource={doctorQueueGroups.finished}
                    columns={[
                      { title: 'STT', dataIndex: 'queueNumber', key: 'queueNumber' },
                      { title: 'Bệnh nhân', dataIndex: 'patientName', key: 'patientName' },
                      { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (st) => <Tag color={QUEUE_STATUS_META[st]?.tone || 'gray'}>{QUEUE_STATUS_META[st]?.label || st}</Tag> },
                      { title: 'Thời gian', dataIndex: 'updatedAt', key: 'updatedAt', render: (t) => t ? dayjs(t).format('HH:mm DD/MM') : 'N/A' },
                    ]}
                    rowKey="id"
                    pagination={{ pageSize: 5 }}
                  />
                </Card>
              </div>
            ),
          },
        ]}
      />

      {/* Modal 1: Check-in Walk-in (Bệnh nhân tự đến) */}
      <Modal
        title="Check-in Bệnh Nhân Tự Đến (Walk-in)"
        open={walkInModalOpen}
        onCancel={() => setWalkInModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={walkInForm} layout="vertical" onFinish={handleCheckInWalkInSubmit}>
          <Form.Item
            name="patientId"
            label="Chọn Bệnh nhân"
            rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân!' }]}
          >
            <Select
              showSearch
              placeholder="Tìm theo tên hoặc số điện thoại..."
              optionFilterProp="children"
              options={patients.map((p) => ({
                value: p.id,
                label: `${p.fullName || p.name} (${p.phone || p.patientCode || 'BN'})`,
              }))}
            />
          </Form.Item>

          <Form.Item
            name="doctorId"
            label="Chọn Bác sĩ khám"
            rules={[{ required: true, message: 'Vui lòng chọn bác sĩ khám!' }]}
          >
            <Select
              placeholder="Chọn bác sĩ phụ trách..."
              options={doctors.map((d) => ({
                value: d.id,
                label: `${d.fullName || d.username} (${d.department || 'Phòng khám'})`,
              }))}
            />
          </Form.Item>

          <Form.Item
            name="reason"
            label="Lý do khám"
            rules={[{ required: true, message: 'Vui lòng nhập lý do khám!' }]}
          >
            <Input placeholder="VD: Đau đầu, sốt nhẹ, khám sức khỏe..." />
          </Form.Item>

          <Form.Item name="notes" label="Ghi chú thêm (Không bắt buộc)">
            <Input.TextArea rows={2} placeholder="Nhập ghi chú từ lễ tân nếu có..." />
          </Form.Item>

          <Alert
            type="info"
            showIcon
            message="Thông báo phân phòng tự động"
            description="Phòng khám sẽ tự động được hệ thống lấy từ phòng đang gán cho Bác sĩ được chọn."
            style={{ marginBottom: 16 }}
          />

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setWalkInModalOpen(false)}>Hủy</Button>
              <Button type="primary" htmlType="submit" loading={actionLoading}>
                Check-in Walk-in
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal 2: Bỏ qua / Skip lượt khám */}
      <Modal
        title="Xác nhận bỏ qua lượt khám (Skip)"
        open={!!skipModalItem}
        onCancel={() => setSkipModalItem(null)}
        footer={null}
      >
        <Form form={skipForm} layout="vertical" onFinish={handleSkipSubmit}>
          <Paragraph>
            Bệnh nhân: <Text strong>{skipModalItem?.patientName}</Text> (STT: {skipModalItem?.queueNumber})
          </Paragraph>
          <Form.Item
            name="reason"
            label="Lý do bỏ qua lượt khám"
            rules={[{ required: true, message: 'Vui lòng nhập hoặc chọn lý do!' }]}
          >
            <Select
              options={[
                { value: 'Vắng mặt khi gọi', label: 'Đã gọi 3 lần nhưng không có mặt' },
                { value: 'Khách hàng xin lùi lượt', label: 'Bệnh nhân có việc bận đột xuất' },
                { value: 'Bệnh nhân hủy khám', label: 'Bệnh nhân ra về không khám nữa' },
              ]}
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setSkipModalItem(null)}>Hủy</Button>
              <Button type="primary" danger htmlType="submit" loading={actionLoading}>
                Xác nhận Skip
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal 3: Xem chi tiết lượt khám QueueItem */}
      <Modal
        title="Chi Tiết Lượt Khám & Hàng Đợi"
        open={!!detailItem}
        onCancel={() => setDetailItem(null)}
        footer={[
          <Button key="close" onClick={() => setDetailItem(null)}>Đóng</Button>,
        ]}
      >
        {detailItem && (
          <div>
            <Paragraph><Text type="secondary">Bệnh nhân:</Text> <Text strong>{detailItem.patientName}</Text></Paragraph>
            <Paragraph><Text type="secondary">Bác sĩ phụ trách:</Text> <Text strong>{detailItem.doctorName || 'Chưa gán'}</Text></Paragraph>
            <Paragraph><Text type="secondary">Mã lượt khám (Visit ID):</Text> <Text code>{detailItem.visitId || detailItem.id || 'N/A'}</Text></Paragraph>
            <Paragraph><Text type="secondary">Trạng thái:</Text> <Tag color="blue">{detailItem.status}</Tag></Paragraph>
            <Paragraph><Text type="secondary">Nguồn tiếp nhận:</Text> {detailItem.sourceType === 'WALK_IN' ? 'Tự đến (Walk-in)' : 'Hẹn trước (Appointment)'}</Paragraph>
            <Paragraph><Text type="secondary">Thời gian tiếp nhận:</Text> {detailItem.checkedInAt ? dayjs(detailItem.checkedInAt).format('HH:mm - DD/MM/YYYY') : 'N/A'}</Paragraph>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default AppointmentQueue
