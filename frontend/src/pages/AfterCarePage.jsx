import React, { useCallback, useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Drawer,
  Form,
  Input,
  message,
  Modal,
  Pagination,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Timeline,
  Tooltip,
  Typography,
} from 'antd'
import {
  BellOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CustomerServiceOutlined,
  ExclamationCircleOutlined,
  FilterOutlined,
  HistoryOutlined,
  PhoneOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import careLogApi from '../api/careLogApi'
import followUpReminderApi from '../api/followUpReminderApi'
import patientApi from '../api/patientApi'
import { useAuthContext } from '../context/AuthContext'
import { formatDate, formatDateTime } from '../utils/helpers'

const { Text, Title } = Typography

// Mappings cho Enums Backend
const reminderTypeMap = {
  REVISIT: { label: 'Tái khám', color: 'blue' },
  MEDICATION_CHECK: { label: 'Kiểm tra dùng thuốc', color: 'purple' },
  GENERAL: { label: 'Nhắc nhở chung', color: 'cyan' },
}

const reminderStatusMap = {
  PENDING: { label: 'Chờ nhắc', color: 'warning', icon: <ClockCircleOutlined /> },
  SENT: { label: 'Đã gửi nhắc', color: 'processing', icon: <BellOutlined /> },
  COMPLETED: { label: 'Đã hoàn thành', color: 'success', icon: <CheckCircleOutlined /> },
  CANCELLED: { label: 'Đã hủy', color: 'default', icon: null },
}

const contactChannelMap = {
  PHONE: { label: 'Điện thoại', color: 'green', icon: <PhoneOutlined /> },
  SMS: { label: 'SMS', color: 'blue', icon: null },
  IN_PERSON: { label: 'Trực tiếp', color: 'orange', icon: null },
  ZALO: { label: 'Zalo', color: 'purple', icon: null },
}

const patientConditionMap = {
  STABLE: { label: 'Ổn định', color: 'success' },
  RECOVERING: { label: 'Đang hồi phục', color: 'processing' },
  COMPLICATIONS: { label: 'Có biến chứng', color: 'error' },
  NEEDS_REVISIT: { label: 'Cần tái khám ngay', color: 'warning' },
}

const contactOutcomeMap = {
  REACHED: { label: 'Liên lạc thành công', color: 'success' },
  UNREACHABLE: { label: 'Không liên lạc được', color: 'warning' },
  DECLINED: { label: 'Từ chối trao đổi', color: 'error' },
}

function AfterCarePage() {
  const location = useLocation()
  const { user } = useAuthContext()

  const [activeTab, setActiveTab] = useState('due')

  // Data states & Page info
  const [dueReminders, setDueReminders] = useState([])
  const [dueRemindersPage, setDueRemindersPage] = useState({ page: 0, total: 0, error: false })

  const [allReminders, setAllReminders] = useState([])
  const [allRemindersPage, setAllRemindersPage] = useState({ page: 0, total: 0, error: false })

  const [careLogs, setCareLogs] = useState([])
  const [careLogsPage, setCareLogsPage] = useState({ page: 0, total: 0, error: false })

  const [loading, setLoading] = useState(false)
  const [patients, setPatients] = useState([])

  // Modal / Drawer controls
  const [createReminderOpen, setCreateReminderOpen] = useState(false)
  const [createCareLogOpen, setCreateCareLogOpen] = useState(false)
  const [patientHistoryOpen, setPatientHistoryOpen] = useState(false)

  const [selectedReminder, setSelectedReminder] = useState(null)
  const [selectedPatientForHistory, setSelectedPatientForHistory] = useState(null)
  const [patientHistoryLogs, setPatientHistoryLogs] = useState([])
  const [loadingHistory, setLoadingHistory] = useState(false)

  // Submitting state
  const [submitting, setSubmitting] = useState(false)

  // Filters for All Reminders
  const [reminderFilterPatientId, setReminderFilterPatientId] = useState(null)
  const [reminderFilterStatus, setReminderFilterStatus] = useState(null)
  const [reminderFilterDateRange, setReminderFilterDateRange] = useState(null)
  const [reminderPageIndex, setReminderPageIndex] = useState(0)

  // Filters for Care Logs
  const [careLogFilterChannel, setCareLogFilterChannel] = useState(null)
  const [careLogFilterDateRange, setCareLogFilterDateRange] = useState(null)
  const [careLogPageIndex, setCareLogPageIndex] = useState(0)

  // Form instances
  const [reminderForm] = Form.useForm()
  const [careLogForm] = Form.useForm()

  // Selected patient's visits for modals
  const [selectedPatientVisits, setSelectedPatientVisits] = useState([])
  const [loadingVisits, setLoadingVisits] = useState(false)

  // Load patient list once for select dropdowns
  const loadPatients = useCallback(async () => {
    try {
      const res = await patientApi.getAll({ page: 0, size: 200 })
      const list = res.data?.content || res.data || []
      setPatients(Array.isArray(list) ? list : [])
    } catch {
      setPatients([])
    }
  }, [])

  // Load Due Reminders
  const loadDueReminders = useCallback(async () => {
    try {
      const res = await followUpReminderApi.getDue({ page: 0, size: 50 })
      const content = res.data?.content || res.data || []
      const total = res.data?.totalElements ?? content.length
      setDueReminders(Array.isArray(content) ? content : [])
      setDueRemindersPage({ page: 0, total, error: false, status: res.status || 200 })
    } catch (err) {
      setDueReminders([])
      setDueRemindersPage({
        page: 0,
        total: 0,
        error: true,
        status: err.response?.status || 500,
        message: err.response?.data?.message || err.message,
      })
    }
  }, [])

  // Load All Reminders with search query
  const loadAllReminders = useCallback(async () => {
    try {
      const params = {
        page: reminderPageIndex,
        size: 10,
      }
      if (reminderFilterPatientId) params.patientId = reminderFilterPatientId
      if (reminderFilterStatus) params.status = reminderFilterStatus
      if (reminderFilterDateRange && reminderFilterDateRange[0]) {
        params.from = reminderFilterDateRange[0].format('YYYY-MM-DD')
      }
      if (reminderFilterDateRange && reminderFilterDateRange[1]) {
        params.to = reminderFilterDateRange[1].format('YYYY-MM-DD')
      }

      const res = await followUpReminderApi.search(params)
      const content = res.data?.content || res.data || []
      const total = res.data?.totalElements ?? content.length
      setAllReminders(Array.isArray(content) ? content : [])
      setAllRemindersPage({ page: reminderPageIndex, total, error: false, status: res.status || 200 })
    } catch (err) {
      setAllReminders([])
      setAllRemindersPage({
        page: reminderPageIndex,
        total: 0,
        error: true,
        status: err.response?.status || 500,
        message: err.response?.data?.message || err.message,
      })
    }
  }, [reminderPageIndex, reminderFilterPatientId, reminderFilterStatus, reminderFilterDateRange])

  // Load Care Logs with search query
  const loadCareLogs = useCallback(async () => {
    try {
      const params = {
        page: careLogPageIndex,
        size: 10,
      }
      if (careLogFilterChannel) params.channel = careLogFilterChannel
      if (careLogFilterDateRange && careLogFilterDateRange[0]) {
        params.from = careLogFilterDateRange[0].startOf('day').toISOString()
      }
      if (careLogFilterDateRange && careLogFilterDateRange[1]) {
        params.to = careLogFilterDateRange[1].endOf('day').toISOString()
      }

      const res = await careLogApi.search(params)
      const content = res.data?.content || res.data || []
      const total = res.data?.totalElements ?? content.length
      setCareLogs(Array.isArray(content) ? content : [])
      setCareLogsPage({ page: careLogPageIndex, total, error: false, status: res.status || 200 })
    } catch (err) {
      setCareLogs([])
      setCareLogsPage({
        page: careLogPageIndex,
        total: 0,
        error: true,
        status: err.response?.status || 500,
        message: err.response?.data?.message || err.message,
      })
    }
  }, [careLogPageIndex, careLogFilterChannel, careLogFilterDateRange])

  // Reload everything independently
  const refreshAllData = useCallback(async () => {
    setLoading(true)
    await Promise.allSettled([
      loadPatients(),
      loadDueReminders(),
      loadAllReminders(),
      loadCareLogs(),
    ])
    setLoading(false)
  }, [loadPatients, loadDueReminders, loadAllReminders, loadCareLogs])

  useEffect(() => {
    refreshAllData()
  }, [refreshAllData])

  // Handle location state navigation (e.g. from Medical Encounter / Doctor advice)
  useEffect(() => {
    if (location.state?.patientId) {
      const pId = location.state.patientId
      reminderForm.setFieldsValue({
        patientId: pId,
        notes: location.state.doctorAdvice || 'Tái khám theo chỉ định của bác sĩ',
        followUpDate: dayjs().add(14, 'day'),
        remindAt: dayjs().add(13, 'day').hour(9).minute(0),
        reminderType: 'REVISIT',
      })
      handlePatientSelectForModal(pId)
      setCreateReminderOpen(true)
    }
  }, [location.state, reminderForm])

  // When patient is selected in modals -> fetch their visit history for visitId selection
  const handlePatientSelectForModal = async (patientId) => {
    if (!patientId) {
      setSelectedPatientVisits([])
      return
    }
    setLoadingVisits(true)
    try {
      const res = await patientApi.getHistory(patientId, { page: 0, size: 50 })
      const list = res.data?.content || res.data || []
      setSelectedPatientVisits(Array.isArray(list) ? list : [])
    } catch {
      setSelectedPatientVisits([])
    } finally {
      setLoadingVisits(false)
    }
  }

  // Handle Create Reminder Submit
  const handleCreateReminderSubmit = async (values) => {
    // Rule: remindAt must be in the future
    if (values.remindAt && dayjs(values.remindAt).isBefore(dayjs())) {
      message.warning('Thời điểm gửi nhắc phải nằm trong tương lai (sau thời điểm hiện tại).')
      return
    }

    setSubmitting(true)
    try {
      const payload = {
        patientId: values.patientId,
        visitId: values.visitId,
        appointmentId: values.appointmentId || null,
        followUpDate: values.followUpDate ? values.followUpDate.format('YYYY-MM-DD') : null,
        remindAt: values.remindAt ? values.remindAt.toISOString() : null,
        reminderType: values.reminderType || 'REVISIT',
        notes: values.notes || '',
      }

      await followUpReminderApi.create(payload)
      message.success('Đã tạo lịch nhắc tái khám thành công!')
      setCreateReminderOpen(false)
      reminderForm.resetFields()
      setSelectedPatientVisits([])
      refreshAllData()
    } catch (err) {
      const serverMsg = err.response?.data?.message || err.message || ''
      let translatedMsg = serverMsg
      if (serverMsg.includes('has no follow-up indication from doctor')) {
        translatedMsg = 'Không thể tạo lịch nhắc vì lượt khám này chưa có dặn dò / chỉ định tái khám từ bác sĩ.'
      } else if (serverMsg.includes('must not be before the visit date')) {
        translatedMsg = 'Ngày tái khám không được trước ngày thực hiện lượt khám.'
      }
      message.error(`Không thể tạo lịch nhắc (Lỗi ${err.response?.status || 400}): ${translatedMsg}`)
    } finally {
      setSubmitting(false)
    }
  }

  // Handle Update Reminder Status
  const handleUpdateStatus = async (reminderId, newStatus) => {
    try {
      await followUpReminderApi.updateStatus(reminderId, newStatus)
      message.success('Đã cập nhật trạng thái lịch nhắc!')
      refreshAllData()
    } catch (err) {
      const errMsg = err.response?.data?.message || err.message || 'Cập nhật trạng thái thất bại'
      message.error(`Lỗi (${err.response?.status || 400}): ${errMsg}`)
    }
  }

  // Handle Open Care Log Modal from Reminder row
  const handleOpenCareLogForReminder = (reminder) => {
    setSelectedReminder(reminder)
    careLogForm.setFieldsValue({
      patientId: reminder.patientId,
      visitId: reminder.visitId,
      reminderId: reminder.id,
      contactChannel: 'PHONE',
      contactedAt: dayjs(),
      contactOutcome: 'REACHED',
      patientCondition: 'STABLE',
      careNotes: `Liên hệ nhắc tái khám ngày ${formatDate(reminder.followUpDate)}. `,
    })
    handlePatientSelectForModal(reminder.patientId)
    setCreateCareLogOpen(true)
  }

  // Handle Create Care Log Submit
  const handleCreateCareLogSubmit = async (values) => {
    setSubmitting(true)
    try {
      const payload = {
        patientId: selectedReminder ? selectedReminder.patientId : values.patientId,
        visitId: selectedReminder ? selectedReminder.visitId : (values.visitId || null),
        reminderId: selectedReminder ? selectedReminder.id : (values.reminderId || null),
        contactChannel: values.contactChannel,
        contactedAt: values.contactedAt ? values.contactedAt.toISOString() : dayjs().toISOString(),
        patientCondition: values.patientCondition,
        careNotes: values.careNotes,
        contactOutcome: values.contactOutcome,
      }

      await careLogApi.create(payload)
      message.success('Ghi nhận chăm sóc thành công.')
      setCreateCareLogOpen(false)
      setSelectedReminder(null)
      careLogForm.resetFields()
      setSelectedPatientVisits([])
      refreshAllData()
    } catch (err) {
      const errMsg = err.response?.data?.message || err.message || 'Lưu nhật ký thất bại'
      message.error(`Lỗi (${err.response?.status || 400}): ${errMsg}`)
    } finally {
      setSubmitting(false)
    }
  }

  // Handle Open Patient Care Log History
  const handleOpenPatientHistory = async (patientId, patientName) => {
    setSelectedPatientForHistory({ id: patientId, name: patientName })
    setPatientHistoryOpen(true)
    setLoadingHistory(true)
    try {
      const res = await careLogApi.getForPatient(patientId)
      const list = res.data || []
      setPatientHistoryLogs(Array.isArray(list) ? list : [])
    } catch (err) {
      setPatientHistoryLogs([])
      message.error('Không thể lấy lịch sử chăm sóc của bệnh nhân: ' + (err.response?.data?.message || err.message))
    } finally {
      setLoadingHistory(false)
    }
  }

  // Helper to find patient name by ID
  const getPatientInfo = (patientId) => {
    const p = patients.find((item) => item.id === patientId)
    if (!p) return { name: patientId ? `${patientId.slice(0, 8)}...` : 'N/A', code: '', phone: '' }
    return { name: p.fullName || p.name, code: p.patientCode || '', phone: p.phone || p.phoneNumber || '' }
  }

  // Stat Cards values format (Error -> —, 200+[] -> 0)
  const dueStatValue = dueRemindersPage.error ? '—' : dueRemindersPage.total
  const careLogStatValue = careLogsPage.error ? '—' : careLogsPage.total
  const allRemindersStatValue = allRemindersPage.error ? '—' : allRemindersPage.total

  // Columns for Due Reminders Table
  const dueColumns = [
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientId',
      key: 'patientId',
      render: (pId) => {
        const info = getPatientInfo(pId)
        return (
          <div>
            <Text strong>{info.name}</Text>
            <div>
              {info.code && <Tag color="blue" style={{ fontSize: 11 }}>{info.code}</Tag>}
              <Text type="secondary" style={{ fontSize: 12 }}>{info.phone || 'Chưa có SĐT'}</Text>
            </div>
          </div>
        )
      },
    },
    {
      title: 'Ngày tái khám',
      dataIndex: 'followUpDate',
      key: 'followUpDate',
      render: (date) => (
        <Space>
          <CalendarOutlined style={{ color: '#1890ff' }} />
          <Text strong>{formatDate(date)}</Text>
        </Space>
      ),
    },
    {
      title: 'Thời điểm nhắc',
      dataIndex: 'remindAt',
      key: 'remindAt',
      render: (val) => formatDateTime(val),
    },
    {
      title: 'Loại nhắc',
      dataIndex: 'reminderType',
      key: 'reminderType',
      render: (type) => {
        const meta = reminderTypeMap[type] || { label: type, color: 'default' }
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st) => {
        const meta = reminderStatusMap[st] || { label: st, color: 'default' }
        return <Tag color={meta.color} icon={meta.icon}>{meta.label}</Tag>
      },
    },
    {
      title: 'Ghi chú chỉ định',
      dataIndex: 'notes',
      key: 'notes',
      ellipsis: true,
      render: (val) => val || '---',
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_, record) => (
        <Space wrap>
          <Button
            type="primary"
            size="small"
            icon={<PhoneOutlined />}
            onClick={() => handleOpenCareLogForReminder(record)}
          >
            Ghi nhận chăm sóc
          </Button>

          {record.status !== 'COMPLETED' && (
            <Button
              size="small"
              type="dashed"
              onClick={() => handleUpdateStatus(record.id, 'COMPLETED')}
            >
              Hoàn thành
            </Button>
          )}

          <Button
            size="small"
            type="link"
            icon={<HistoryOutlined />}
            onClick={() => handleOpenPatientHistory(record.patientId, getPatientInfo(record.patientId).name)}
          >
            Lịch sử
          </Button>
        </Space>
      ),
    },
  ]

  // Columns for All Reminders Table
  const allReminderColumns = [
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientId',
      key: 'patientId',
      render: (pId) => {
        const info = getPatientInfo(pId)
        return (
          <div>
            <Text strong>{info.name}</Text>
            <div>
              {info.code && <Tag color="blue" style={{ fontSize: 11 }}>{info.code}</Tag>}
              <Text type="secondary" style={{ fontSize: 12 }}>{info.phone}</Text>
            </div>
          </div>
        )
      },
    },
    {
      title: 'Ngày tái khám',
      dataIndex: 'followUpDate',
      key: 'followUpDate',
      render: (date) => formatDate(date),
    },
    {
      title: 'Thời điểm nhắc',
      dataIndex: 'remindAt',
      key: 'remindAt',
      render: (val) => formatDateTime(val),
    },
    {
      title: 'Loại nhắc',
      dataIndex: 'reminderType',
      key: 'reminderType',
      render: (type) => {
        const meta = reminderTypeMap[type] || { label: type, color: 'default' }
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st) => {
        const meta = reminderStatusMap[st] || { label: st, color: 'default' }
        return <Tag color={meta.color} icon={meta.icon}>{meta.label}</Tag>
      },
    },
    {
      title: 'Ghi chú',
      dataIndex: 'notes',
      key: 'notes',
      ellipsis: true,
      render: (val) => val || '---',
    },
    {
      title: 'Cập nhật trạng thái',
      key: 'statusAction',
      render: (_, record) => (
        <Select
          size="small"
          value={record.status}
          style={{ width: 140 }}
          onChange={(newSt) => handleUpdateStatus(record.id, newSt)}
        >
          <Select.Option value="PENDING">Chờ nhắc</Select.Option>
          <Select.Option value="SENT">Đã gửi nhắc</Select.Option>
          <Select.Option value="COMPLETED">Đã hoàn thành</Select.Option>
          <Select.Option value="CANCELLED">Đã hủy</Select.Option>
        </Select>
      ),
    },
  ]

  // Columns for Care Logs Table
  const careLogColumns = [
    {
      title: 'Thời gian liên hệ',
      dataIndex: 'contactedAt',
      key: 'contactedAt',
      render: (val) => formatDateTime(val),
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientId',
      key: 'patientId',
      render: (pId) => {
        const info = getPatientInfo(pId)
        return (
          <Space direction="vertical" size={0}>
            <Text strong>{info.name}</Text>
            {info.code && <Text type="secondary" style={{ fontSize: 12 }}>{info.code}</Text>}
          </Space>
        )
      },
    },
    {
      title: 'Kênh liên hệ',
      dataIndex: 'contactChannel',
      key: 'contactChannel',
      render: (ch) => {
        const meta = contactChannelMap[ch] || { label: ch, color: 'default', icon: null }
        return <Tag color={meta.color} icon={meta.icon}>{meta.label}</Tag>
      },
    },
    {
      title: 'Tình trạng BN',
      dataIndex: 'patientCondition',
      key: 'patientCondition',
      render: (cond) => {
        const meta = patientConditionMap[cond] || { label: cond, color: 'default' }
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Kết quả liên hệ',
      dataIndex: 'contactOutcome',
      key: 'contactOutcome',
      render: (outc) => {
        const meta = contactOutcomeMap[outc] || { label: outc, color: 'default' }
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Nội dung chăm sóc',
      dataIndex: 'careNotes',
      key: 'careNotes',
      ellipsis: true,
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'performedBy',
      key: 'performedBy',
      render: (val) => <Tag icon={<UserOutlined />}>{val ? String(val).slice(0, 8) : 'Lễ tân'}</Tag>,
    },
    {
      title: 'Xem',
      key: 'view',
      render: (_, record) => (
        <Button
          size="small"
          type="link"
          icon={<HistoryOutlined />}
          onClick={() => handleOpenPatientHistory(record.patientId, getPatientInfo(record.patientId).name)}
        >
          Lịch sử BN
        </Button>
      ),
    },
  ]

  // Watched condition value in care log form for showing prominent warning
  const watchedCondition = Form.useWatch('patientCondition', careLogForm)

  return (
    <div style={{ padding: '24px', maxWidth: 1300, margin: '0 auto' }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        
        {/* Header Card */}
        <Card
          style={{
            background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
            borderRadius: 12,
            color: '#fff',
          }}
          bodyStyle={{ padding: '24px' }}
        >
          <Row align="middle" justify="space-between" gutter={[16, 16]}>
            <Col xs={24} md={16}>
              <Title level={3} style={{ color: '#fff', margin: 0 }}>
                <CustomerServiceOutlined /> CHĂM SÓC SAU KHÁM
              </Title>
              <Text style={{ color: 'rgba(255, 255, 255, 0.9)', fontSize: 14 }}>
                Theo dõi lịch nhắc tái khám và ghi nhận quá trình chăm sóc bệnh nhân sau khám.
              </Text>
            </Col>
            <Col xs={24} md={8} style={{ textAlign: 'right' }}>
              <Space wrap style={{ justifyContent: 'flex-end' }}>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={refreshAllData}
                  loading={loading}
                  style={{ borderRadius: 6 }}
                >
                  Làm mới
                </Button>

                <Button
                  type="default"
                  icon={<PlusOutlined />}
                  style={{ borderRadius: 6, fontWeight: 600 }}
                  onClick={() => {
                    setSelectedReminder(null)
                    careLogForm.resetFields()
                    setSelectedPatientVisits([])
                    setCreateCareLogOpen(true)
                  }}
                >
                  Ghi nhận chăm sóc mới
                </Button>

                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  style={{ borderRadius: 6, fontWeight: 600, backgroundColor: '#52c41a', borderColor: '#52c41a' }}
                  onClick={() => {
                    reminderForm.resetFields()
                    setSelectedPatientVisits([])
                    setCreateReminderOpen(true)
                  }}
                >
                  Tạo lịch nhắc tái khám
                </Button>
              </Space>
            </Col>
          </Row>
        </Card>

        {/* 3 Stat Cards */}
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Card style={{ borderRadius: 8, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
              <Statistic
                title="Lịch nhắc đến hạn"
                value={dueStatValue}
                prefix={<ExclamationCircleOutlined />}
                valueStyle={{ color: '#ff4d4f', fontWeight: 'bold' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card style={{ borderRadius: 8, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
              <Statistic
                title="Nhật ký chăm sóc đã lưu"
                value={careLogStatValue}
                prefix={<HistoryOutlined />}
                valueStyle={{ color: '#52c41a', fontWeight: 'bold' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card style={{ borderRadius: 8, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
              <Statistic
                title="Tổng số lịch nhắc"
                value={allRemindersStatValue}
                prefix={<CalendarOutlined />}
                valueStyle={{ color: '#1890ff', fontWeight: 'bold' }}
              />
            </Card>
          </Col>
        </Row>

        {/* Main Content Tabs */}
        <Card style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={[
              {
                key: 'due',
                label: (
                  <span>
                    <ExclamationCircleOutlined /> Đến hạn chăm sóc{' '}
                    {!dueRemindersPage.error && dueRemindersPage.total > 0 && (
                      <Badge count={dueRemindersPage.total} overflowCount={99} style={{ backgroundColor: '#ff4d4f' }} />
                    )}
                  </span>
                ),
                children: (
                  <div>
                    {dueRemindersPage.error ? (
                      <Alert
                        type={dueRemindersPage.status === 403 ? "warning" : "error"}
                        showIcon
                        message={
                          dueRemindersPage.status === 403
                            ? "Tài khoản hiện tại chưa được phân quyền truy cập Lịch nhắc đến hạn (Lỗi 403 Forbidden)"
                            : `Không thể tải danh sách lịch nhắc đến hạn từ server (Lỗi ${dueRemindersPage.status}).`
                        }
                        description={
                          dueRemindersPage.status === 403
                            ? "Backend yêu cầu quyền FOLLOW_UP_REMINDER_READ. Vui lòng sử dụng tài khoản Quản trị viên (Admin) hoặc liên hệ Quản trị hệ thống để cấp quyền."
                            : (dueRemindersPage.message || "Vui lòng kiểm tra lại kết nối backend hoặc nhấn nút Làm mới.")
                        }
                        style={{ margin: '16px 0' }}
                      />
                    ) : (
                      <Table
                        columns={dueColumns}
                        dataSource={dueReminders}
                        rowKey="id"
                        loading={loading}
                        pagination={false}
                        locale={{ emptyText: 'Không có lịch nhắc nào đến hạn chăm sóc' }}
                      />
                    )}
                  </div>
                ),
              },
              {
                key: 'reminders',
                label: (
                  <span>
                    <CalendarOutlined /> Tất cả lịch nhắc
                  </span>
                ),
                children: (
                  <div>
                    {/* Filters bar */}
                    <Row gutter={[12, 12]} style={{ marginBottom: 16 }} align="middle">
                      <Col xs={24} sm={8} md={6}>
                        <Select
                          showSearch
                          allowClear
                          placeholder="Lọc theo Bệnh nhân..."
                          style={{ width: '100%' }}
                          value={reminderFilterPatientId}
                          onChange={(val) => {
                            setReminderFilterPatientId(val)
                            setReminderPageIndex(0)
                          }}
                          optionFilterProp="children"
                        >
                          {patients.map((p) => (
                            <Select.Option key={p.id} value={p.id}>
                              {p.fullName || p.name} ({p.patientCode || p.id.slice(0, 6)})
                            </Select.Option>
                          ))}
                        </Select>
                      </Col>

                      <Col xs={24} sm={8} md={5}>
                        <Select
                          allowClear
                          placeholder="Lọc theo Trạng thái..."
                          style={{ width: '100%' }}
                          value={reminderFilterStatus}
                          onChange={(val) => {
                            setReminderFilterStatus(val)
                            setReminderPageIndex(0)
                          }}
                        >
                          <Select.Option value="PENDING">Chờ nhắc</Select.Option>
                          <Select.Option value="SENT">Đã gửi nhắc</Select.Option>
                          <Select.Option value="COMPLETED">Đã hoàn thành</Select.Option>
                          <Select.Option value="CANCELLED">Đã hủy</Select.Option>
                        </Select>
                      </Col>

                      <Col xs={24} sm={8} md={7}>
                        <DatePicker.RangePicker
                          style={{ width: '100%' }}
                          format="DD/MM/YYYY"
                          placeholder={['Từ ngày', 'Đến ngày']}
                          value={reminderFilterDateRange}
                          onChange={(val) => {
                            setReminderFilterDateRange(val)
                            setReminderPageIndex(0)
                          }}
                        />
                      </Col>

                      <Col xs={24} sm={24} md={6} style={{ textAlign: 'right' }}>
                        <Button
                          icon={<FilterOutlined />}
                          onClick={() => {
                            setReminderFilterPatientId(null)
                            setReminderFilterStatus(null)
                            setReminderFilterDateRange(null)
                            setReminderPageIndex(0)
                          }}
                        >
                          Xóa bộ lọc
                        </Button>
                      </Col>
                    </Row>

                    {allRemindersPage.error ? (
                      <Alert
                        type="error"
                        showIcon
                        message="Lỗi lấy dữ liệu tất cả lịch nhắc."
                        style={{ margin: '16px 0' }}
                      />
                    ) : (
                      <>
                        <Table
                          columns={allReminderColumns}
                          dataSource={allReminders}
                          rowKey="id"
                          loading={loading}
                          pagination={false}
                          locale={{ emptyText: 'Chưa có lịch nhắc tái khám nào' }}
                        />
                        <div style={{ marginTop: 16, textAlign: 'right' }}>
                          <Pagination
                            current={reminderPageIndex + 1}
                            total={allRemindersPage.total}
                            pageSize={10}
                            onChange={(page) => setReminderPageIndex(page - 1)}
                            showSizeChanger={false}
                          />
                        </div>
                      </>
                    )}
                  </div>
                ),
              },
              {
                key: 'careLogs',
                label: (
                  <span>
                    <HistoryOutlined /> Nhật ký chăm sóc
                  </span>
                ),
                children: (
                  <div>
                    {/* Filters bar */}
                    <Row gutter={[12, 12]} style={{ marginBottom: 16 }} align="middle">
                      <Col xs={24} sm={8} md={6}>
                        <Select
                          allowClear
                          placeholder="Kênh liên hệ..."
                          style={{ width: '100%' }}
                          value={careLogFilterChannel}
                          onChange={(val) => {
                            setCareLogFilterChannel(val)
                            setCareLogPageIndex(0)
                          }}
                        >
                          <Select.Option value="PHONE">Điện thoại</Select.Option>
                          <Select.Option value="SMS">SMS</Select.Option>
                          <Select.Option value="IN_PERSON">Trực tiếp</Select.Option>
                          <Select.Option value="ZALO">Zalo</Select.Option>
                        </Select>
                      </Col>

                      <Col xs={24} sm={10} md={8}>
                        <DatePicker.RangePicker
                          style={{ width: '100%' }}
                          format="DD/MM/YYYY"
                          placeholder={['Từ ngày', 'Đến ngày']}
                          value={careLogFilterDateRange}
                          onChange={(val) => {
                            setCareLogFilterDateRange(val)
                            setCareLogPageIndex(0)
                          }}
                        />
                      </Col>

                      <Col xs={24} sm={6} md={10} style={{ textAlign: 'right' }}>
                        <Button
                          icon={<FilterOutlined />}
                          onClick={() => {
                            setCareLogFilterChannel(null)
                            setCareLogFilterDateRange(null)
                            setCareLogPageIndex(0)
                          }}
                        >
                          Xóa bộ lọc
                        </Button>
                      </Col>
                    </Row>

                    {careLogsPage.error ? (
                      <Alert
                        type="error"
                        showIcon
                        message="Lỗi kết nối nhật ký chăm sóc."
                        style={{ margin: '16px 0' }}
                      />
                    ) : (
                      <>
                        <Table
                          columns={careLogColumns}
                          dataSource={careLogs}
                          rowKey="id"
                          loading={loading}
                          pagination={false}
                          locale={{ emptyText: 'Chưa có ghi nhận nhật ký chăm sóc nào' }}
                        />
                        <div style={{ marginTop: 16, textAlign: 'right' }}>
                          <Pagination
                            current={careLogPageIndex + 1}
                            total={careLogsPage.total}
                            pageSize={10}
                            onChange={(page) => setCareLogPageIndex(page - 1)}
                            showSizeChanger={false}
                          />
                        </div>
                      </>
                    )}
                  </div>
                ),
              },
            ]}
          />
        </Card>
      </Space>

      {/* Modal 1: Tạo lịch nhắc tái khám */}
      <Modal
        title="TẠO LỊCH NHẮC TÁI KHÁM"
        open={createReminderOpen}
        onCancel={() => {
          setCreateReminderOpen(false)
          setSelectedPatientVisits([])
        }}
        onOk={() => reminderForm.submit()}
        confirmLoading={submitting}
        okText="Tạo lịch nhắc"
        cancelText="Hủy"
        destroyOnClose
        width={600}
      >
        <Form form={reminderForm} layout="vertical" onFinish={handleCreateReminderSubmit}>
          <Form.Item
            name="patientId"
            label="Bệnh nhân *"
            rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}
          >
            <Select
              showSearch
              placeholder="Gõ tên hoặc SĐT để tìm bệnh nhân..."
              optionFilterProp="children"
              onChange={handlePatientSelectForModal}
            >
              {patients.map((p) => (
                <Select.Option key={p.id} value={p.id}>
                  {p.fullName || p.name} ({p.patientCode || p.id.slice(0, 6)}) - {p.phone || p.phoneNumber || 'Chưa có SĐT'}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="visitId"
            label="Lượt khám *"
            rules={[{ required: true, message: 'Vui lòng chọn lượt khám liên quan' }]}
            help="Theo quy định hệ thống, lượt khám phải được Bác sĩ ghi nhận chỉ định / dặn dò tái khám trong Bệnh án mới có thể tạo Lịch nhắc."
          >
            <Select
              placeholder={loadingVisits ? 'Đang tải lịch sử khám...' : 'Chọn lượt khám của bệnh nhân...'}
              loading={loadingVisits}
              disabled={!selectedPatientVisits.length}
            >
              {selectedPatientVisits.map((v) => {
                const vId = v.visitId || v.id
                const vCode = v.visitCode || v.code || (vId ? String(vId).slice(0, 8) : 'LK')
                return (
                  <Select.Option key={vId} value={vId}>
                    Mã LK: {vCode} | Ngày: {formatDate(v.visitAt || v.createdDate || v.createdAt)} | BS: {v.doctorName || 'BS. Khám'}
                  </Select.Option>
                )
              })}
            </Select>
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="followUpDate"
                label="Ngày tái khám *"
                rules={[{ required: true, message: 'Vui lòng chọn ngày tái khám' }]}
              >
                <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" placeholder="Chọn ngày tái khám" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="remindAt"
                label="Thời điểm gửi nhắc *"
                rules={[{ required: true, message: 'Vui lòng chọn thời điểm nhắc' }]}
              >
                <DatePicker
                  showTime
                  style={{ width: '100%' }}
                  format="DD/MM/YYYY HH:mm"
                  placeholder="Chọn thời điểm nhắc"
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="reminderType"
            label="Loại nhắc *"
            initialValue="REVISIT"
            rules={[{ required: true, message: 'Vui lòng chọn loại nhắc' }]}
          >
            <Select>
              <Select.Option value="REVISIT">Tái khám</Select.Option>
              <Select.Option value="MEDICATION_CHECK">Kiểm tra dùng thuốc</Select.Option>
              <Select.Option value="GENERAL">Nhắc nhở chung</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="notes"
            label="Ghi chú / Nội dung dặn dò"
            rules={[{ max: 500, message: 'Không vượt quá 500 ký tự' }]}
          >
            <Input.TextArea rows={3} placeholder="VD: Bệnh nhân tái khám thử lại đường huyết sau 2 tuần..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal 2: Ghi nhận chăm sóc sau khám */}
      <Modal
        title="GHI NHẬN CHĂM SÓC SAU KHÁM"
        open={createCareLogOpen}
        onCancel={() => {
          setCreateCareLogOpen(false)
          setSelectedReminder(null)
          setSelectedPatientVisits([])
        }}
        onOk={() => careLogForm.submit()}
        confirmLoading={submitting}
        okText="Lưu ghi nhận"
        cancelText="Hủy"
        destroyOnClose
        width={650}
      >
        {selectedReminder && (
          <Alert
            message={`Đang chăm sóc theo Lịch nhắc: ${getPatientInfo(selectedReminder.patientId).name}`}
            description={`Ngày hẹn tái khám: ${formatDate(selectedReminder.followUpDate)} | Nội dung chỉ định: ${selectedReminder.notes || 'Không có'}`}
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        <Form form={careLogForm} layout="vertical" onFinish={handleCreateCareLogSubmit}>
          {!selectedReminder && (
            <>
              <Form.Item
                name="patientId"
                label="Bệnh nhân *"
                rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}
              >
                <Select
                  showSearch
                  placeholder="Gõ tên/SĐT chọn bệnh nhân..."
                  optionFilterProp="children"
                  onChange={handlePatientSelectForModal}
                >
                  {patients.map((p) => (
                    <Select.Option key={p.id} value={p.id}>
                      {p.fullName || p.name} ({p.patientCode || p.id.slice(0, 6)})
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item name="visitId" label="Lượt khám liên quan">
                <Select
                  placeholder={loadingVisits ? 'Đang tải danh sách lượt khám...' : 'Chọn lượt khám (nếu có)...'}
                  loading={loadingVisits}
                  disabled={!selectedPatientVisits.length}
                  allowClear
                >
                  {selectedPatientVisits.map((v) => {
                    const vId = v.visitId || v.id
                    const vCode = v.visitCode || v.code || (vId ? String(vId).slice(0, 8) : 'LK')
                    return (
                      <Select.Option key={vId} value={vId}>
                        Mã LK: {vCode} | Ngày: {formatDate(v.visitAt || v.createdDate || v.createdAt)} | BS: {v.doctorName || 'BS'}
                      </Select.Option>
                    )
                  })}
                </Select>
              </Form.Item>
            </>
          )}

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="contactChannel"
                label="Kênh liên hệ *"
                initialValue="PHONE"
                rules={[{ required: true }]}
              >
                <Select>
                  <Select.Option value="PHONE">Điện thoại</Select.Option>
                  <Select.Option value="SMS">SMS</Select.Option>
                  <Select.Option value="IN_PERSON">Trực tiếp</Select.Option>
                  <Select.Option value="ZALO">Zalo</Select.Option>
                </Select>
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                name="contactedAt"
                label="Thời gian liên hệ thực tế *"
                initialValue={dayjs()}
                rules={[{ required: true }]}
              >
                <DatePicker showTime style={{ width: '100%' }} format="DD/MM/YYYY HH:mm" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="contactOutcome"
                label="Kết quả liên hệ *"
                initialValue="REACHED"
                rules={[{ required: true }]}
              >
                <Select>
                  <Select.Option value="REACHED">Liên lạc thành công</Select.Option>
                  <Select.Option value="UNREACHABLE">Không liên lạc được</Select.Option>
                  <Select.Option value="DECLINED">Từ chối trao đổi</Select.Option>
                </Select>
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                name="patientCondition"
                label="Tình trạng sức khỏe bệnh nhân *"
                initialValue="STABLE"
                rules={[{ required: true }]}
              >
                <Select>
                  <Select.Option value="STABLE">Ổn định</Select.Option>
                  <Select.Option value="RECOVERING">Đang hồi phục</Select.Option>
                  <Select.Option value="COMPLICATIONS">Có biến chứng</Select.Option>
                  <Select.Option value="NEEDS_REVISIT">Cần tái khám ngay</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          {/* Warning Alert if COMPLICATIONS or NEEDS_REVISIT */}
          {(watchedCondition === 'COMPLICATIONS' || watchedCondition === 'NEEDS_REVISIT') && (
            <Alert
              message="CẢNH BÁO SỨC KHỎE BỆNH NHÂN"
              description="Bệnh nhân ghi nhận có biến chứng hoặc cần tái khám ngay. Vui lòng thông báo Bác sĩ hoặc tư vấn bệnh nhân đến phòng khám sớm nhất!"
              type="warning"
              showIcon
              icon={<WarningOutlined />}
              style={{ marginBottom: 16 }}
            />
          )}

          <Form.Item
            name="careNotes"
            label="Nội dung chăm sóc / ghi nhận *"
            rules={[
              { required: true, message: 'Vui lòng nhập nội dung chăm sóc' },
              { max: 2000, message: 'Không vượt quá 2000 ký tự' },
            ]}
          >
            <Input.TextArea
              rows={4}
              placeholder="Nhập chi tiết nội dung cuộc gọi/tin nhắn, tình trạng sức khỏe thực tế của bệnh nhân..."
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Drawer: Xem lịch sử chăm sóc theo bệnh nhân */}
      <Drawer
        title={`LỊCH SỬ CHĂM SÓC: ${selectedPatientForHistory?.name || ''}`}
        placement="right"
        width={520}
        open={patientHistoryOpen}
        onClose={() => setPatientHistoryOpen(false)}
      >
        {loadingHistory ? (
          <div style={{ textAlign: 'center', padding: 32 }}>Đang tải lịch sử...</div>
        ) : !patientHistoryLogs.length ? (
          <Alert message="Bệnh nhân chưa có ghi nhận nhật ký chăm sóc nào." type="info" showIcon />
        ) : (
          <Timeline
            mode="left"
            items={patientHistoryLogs.map((log) => {
              const condMeta = patientConditionMap[log.patientCondition] || { label: log.patientCondition, color: 'default' }
              const channelMeta = contactChannelMap[log.contactChannel] || { label: log.contactChannel, color: 'default' }
              const outcomeMeta = contactOutcomeMap[log.contactOutcome] || { label: log.contactOutcome, color: 'default' }

              return {
                color: log.patientCondition === 'COMPLICATIONS' ? 'red' : 'blue',
                children: (
                  <Card size="small" style={{ marginBottom: 12, borderRadius: 8 }}>
                    <Space direction="vertical" size={4} style={{ width: '100%' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {formatDateTime(log.contactedAt)}
                        </Text>
                        <Tag color={channelMeta.color}>{channelMeta.label}</Tag>
                      </div>

                      <Space wrap style={{ marginTop: 4 }}>
                        <Tag color={condMeta.color}>Sức khỏe: {condMeta.label}</Tag>
                        <Tag color={outcomeMeta.color}>Kết quả: {outcomeMeta.label}</Tag>
                      </Space>

                      <Text style={{ marginTop: 6, display: 'block' }}>
                        {log.careNotes}
                      </Text>

                      <div style={{ marginTop: 4, textAlign: 'right' }}>
                        <Text type="secondary" style={{ fontSize: 11 }}>
                          Người thực hiện: <UserOutlined /> {log.performedBy ? String(log.performedBy).slice(0, 8) : 'Lễ tân'}
                        </Text>
                      </div>
                    </Space>
                  </Card>
                ),
              }
            })}
          />
        )}
      </Drawer>
    </div>
  )
}

export default AfterCarePage
