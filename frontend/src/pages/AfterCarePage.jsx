import React, { useCallback, useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import {
  BellOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  HistoryOutlined,
  PhoneOutlined,
  PlusOutlined,
  SearchOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import aftercareApi from '../api/aftercareApi'
import patientApi from '../api/patientApi'
import { useAuthContext } from '../context/AuthContext'
import { formatDate, formatDateTime } from '../utils/helpers'

const { Text, Title } = Typography

const statusMeta = {
  DUE: { label: 'Đến hạn tái khám', color: 'error', icon: <ExclamationCircleOutlined /> },
  PENDING: { label: 'Chờ đến hạn', color: 'processing', icon: <ClockCircleOutlined /> },
  COMPLETED: { label: 'Đã hoàn thành', color: 'success', icon: <CheckCircleOutlined /> },
  CANCELLED: { label: 'Đã hủy', color: 'default', icon: null },
}

function AfterCarePage() {
  const location = useLocation()
  const { user } = useAuthContext()
  const [activeTab, setActiveTab] = useState('reminders')
  const [reminders, setReminders] = useState([])
  const [notes, setNotes] = useState([])
  const [patients, setPatients] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')

  // Modal states
  const [createReminderOpen, setCreateReminderOpen] = useState(false)
  const [createNoteOpen, setCreateNoteOpen] = useState(false)
  const [selectedReminder, setSelectedReminder] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const [reminderForm] = Form.useForm()
  const [noteForm] = Form.useForm()

  useEffect(() => {
    if (location.state?.patientId) {
      reminderForm.setFieldsValue({
        patientId: location.state.patientId,
        recordCode: location.state.recordCode || '',
        note: location.state.doctorAdvice || 'Tái khám theo chỉ định của bác sĩ',
        remindDate: dayjs().add(14, 'day'),
      })
      setCreateReminderOpen(true)
    }
  }, [location.state, reminderForm])

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [remRes, noteRes, patRes] = await Promise.allSettled([
        aftercareApi.getReminders(),
        aftercareApi.getNotes(),
        patientApi.getAll({ page: 0, size: 200 }),
      ])

      if (remRes.status === 'fulfilled') setReminders(remRes.value.data || [])
      if (noteRes.status === 'fulfilled') setNotes(noteRes.value.data || [])
      if (patRes.status === 'fulfilled') setPatients(patRes.value.data?.content || [])
    } catch {
      // ignore load error
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // NCL-10-CN-001: Create follow-up reminder
  const handleCreateReminder = async (values) => {
    setSubmitting(true)
    try {
      const patient = patients.find((p) => p.id === values.patientId)
      const payload = {
        patientId: values.patientId,
        patientName: patient ? patient.fullName : 'Bệnh nhân',
        phone: patient ? patient.phone : '',
        recordCode: values.recordCode || 'BA-2026-N',
        doctorName: values.doctorName || 'BS. Chỉ định',
        remindDate: values.remindDate.format('YYYY-MM-DD'),
        note: values.note,
        status: dayjs(values.remindDate).isBefore(dayjs(), 'day') ? 'DUE' : 'PENDING',
      }

      await aftercareApi.createReminder(payload)
      message.success('Đã tạo lịch nhắc tái khám thành công!')
      setCreateReminderOpen(false)
      reminderForm.resetFields()
      loadData()
    } catch (err) {
      message.error('Lỗi khi tạo lịch nhắc tái khám: ' + (err.message || 'Không xác định'))
    } finally {
      setSubmitting(false)
    }
  }

  // NCL-10-CN-002: Create after-care note
  const handleCreateNote = async (values) => {
    setSubmitting(true)
    try {
      const payload = {
        reminderId: selectedReminder ? selectedReminder.id : null,
        patientId: selectedReminder ? selectedReminder.patientId : values.patientId,
        patientName: selectedReminder ? selectedReminder.patientName : (patients.find((p) => p.id === values.patientId)?.fullName || 'Bệnh nhân'),
        contactMethod: values.contactMethod,
        contactResult: values.contactResult,
        content: values.content,
        recordedBy: user?.fullName || 'Lễ tân',
      }

      await aftercareApi.createNote(payload)
      message.success('Đã ghi nhận nhật ký chăm sóc sau khám thành công!')
      setCreateNoteOpen(false)
      setSelectedReminder(null)
      noteForm.resetFields()
      loadData()
    } catch (err) {
      message.error('Lỗi khi lưu nhật ký chăm sóc: ' + (err.message || 'Không xác định'))
    } finally {
      setSubmitting(false)
    }
  }

  const filteredReminders = reminders.filter((item) => {
    const kw = searchText.trim().toLowerCase()
    if (!kw) return true
    return (
      (item.patientName || '').toLowerCase().includes(kw) ||
      (item.phone || '').includes(kw) ||
      (item.recordCode || '').toLowerCase().includes(kw)
    )
  })

  const filteredNotes = notes.filter((item) => {
    const kw = searchText.trim().toLowerCase()
    if (!kw) return true
    return (
      (item.patientName || '').toLowerCase().includes(kw) ||
      (item.content || '').toLowerCase().includes(kw)
    )
  })

  const dueCount = reminders.filter((r) => r.status === 'DUE').length
  const pendingCount = reminders.filter((r) => r.status === 'PENDING').length
  const completedCount = reminders.filter((r) => r.status === 'COMPLETED').length

  const reminderColumns = [
    {
      title: 'Mã hồ sơ',
      dataIndex: 'recordCode',
      key: 'recordCode',
      render: (val) => <Tag color="blue">{val || 'BA-000'}</Tag>,
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      render: (name, record) => (
        <div>
          <Text strong>{name}</Text>
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>
            <PhoneOutlined /> {record.phone || 'Chưa có SĐT'}
          </div>
        </div>
      ),
    },
    {
      title: 'Bác sĩ chỉ định',
      dataIndex: 'doctorName',
      key: 'doctorName',
      render: (val) => val || '---',
    },
    {
      title: 'Ngày đến hạn tái khám',
      dataIndex: 'remindDate',
      key: 'remindDate',
      render: (date) => {
        const isPast = dayjs(date).isBefore(dayjs(), 'day')
        return (
          <Space>
            <CalendarOutlined style={{ color: isPast ? '#ff4d4f' : '#1890ff' }} />
            <Text type={isPast ? 'danger' : 'default'} strong={isPast}>
              {formatDate(date)}
            </Text>
          </Space>
        )
      },
    },
    {
      title: 'Ghi chú chỉ định',
      dataIndex: 'note',
      key: 'note',
      render: (val) => val || '---',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const meta = statusMeta[status] || { label: status, color: 'default' }
        return (
          <Tag color={meta.color} icon={meta.icon}>
            {meta.label}
          </Tag>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_, record) => (
        <Space>
          {record.status !== 'COMPLETED' && (
            <Button
              type="primary"
              size="small"
              icon={<PhoneOutlined />}
              onClick={() => {
                setSelectedReminder(record)
                noteForm.setFieldsValue({
                  patientId: record.patientId,
                  contactMethod: 'PHONE_CALL',
                  contactResult: 'SUCCESS',
                  content: `Liên hệ nhắc tái khám ngày ${formatDate(record.remindDate)}. `,
                })
                setCreateNoteOpen(true)
              }}
            >
              Gọi chăm sóc
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const noteColumns = [
    {
      title: 'Thời gian ghi nhận',
      dataIndex: 'recordedAt',
      key: 'recordedAt',
      render: (val) => formatDateTime(val),
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      render: (name) => <Text strong>{name}</Text>,
    },
    {
      title: 'Hình thức',
      dataIndex: 'contactMethod',
      key: 'contactMethod',
      render: (method) => {
        if (method === 'PHONE_CALL') return <Tag color="green"><PhoneOutlined /> Gọi điện thoại</Tag>
        if (method === 'SMS') return <Tag color="blue">Tin nhắn SMS</Tag>
        return <Tag color="purple">Zalo / Khác</Tag>
      },
    },
    {
      title: 'Kết quả liên hệ',
      dataIndex: 'contactResult',
      key: 'contactResult',
      render: (res) => {
        if (res === 'SUCCESS') return <Tag color="success">Thành công (Đã dặn dò)</Tag>
        if (res === 'NO_ANSWER') return <Tag color="warning">Không bắt máy</Tag>
        return <Tag color="error">Máy bận / Lỗi</Tag>
      },
    },
    {
      title: 'Nội dung chăm sóc / Dặn dò',
      dataIndex: 'content',
      key: 'content',
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'recordedBy',
      key: 'recordedBy',
      render: (val) => <Tag icon={<UserOutlined />}>{val || 'Lễ tân'}</Tag>,
    },
  ]

  return (
    <div style={{ padding: '24px', maxWidth: 1200, margin: '0 auto' }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        {/* Banner Header */}
        <Card
          style={{
            background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
            borderRadius: 12,
            color: '#fff',
          }}
          bodyStyle={{ padding: '24px' }}
        >
          <Row align="middle" justify="space-between">
            <Col>
              <Title level={3} style={{ color: '#fff', margin: 0 }}>
                <BellOutlined /> Chăm sóc Sau khám & Nhắc Tái khám
              </Title>
              <Text style={{ color: 'rgba(255, 255, 255, 0.85)' }}>
                Hỗ trợ Lễ tân tạo lịch nhắc tái khám theo chỉ định bác sĩ & ghi nhận nhật ký chăm sóc theo dõi bệnh nhân
              </Text>
            </Col>
            <Col>
              <Button
                type="default"
                size="large"
                icon={<PlusOutlined />}
                style={{ borderRadius: 8, fontWeight: 600 }}
                onClick={() => {
                  reminderForm.resetFields()
                  setCreateReminderOpen(true)
                }}
              >
                + Tạo lịch nhắc tái khám
              </Button>
            </Col>
          </Row>
        </Card>

        {/* Statistics Cards */}
        <Row gutter={16}>
          <Col span={6}>
            <Card style={{ borderRadius: 8 }}>
              <Statistic
                title="Tất cả lịch nhắc"
                value={reminders.length}
                prefix={<CalendarOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card style={{ borderRadius: 8 }}>
              <Statistic
                title="Đến hạn tái khám"
                value={dueCount}
                prefix={<ExclamationCircleOutlined />}
                valueStyle={{ color: '#ff4d4f' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card style={{ borderRadius: 8 }}>
              <Statistic
                title="Chờ đến hạn"
                value={pendingCount}
                prefix={<ClockCircleOutlined />}
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card style={{ borderRadius: 8 }}>
              <Statistic
                title="Đã chăm sóc / Hoàn thành"
                value={completedCount}
                prefix={<CheckCircleOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
        </Row>

        {/* Main Content Tabs */}
        <Card style={{ borderRadius: 12 }}>
          <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
            <Col span={12}>
              <Input
                placeholder="Tìm kiếm theo tên bệnh nhân, số điện thoại, mã hồ sơ..."
                prefix={<SearchOutlined />}
                allowClear
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                style={{ width: '100%', borderRadius: 6 }}
              />
            </Col>
            <Col>
              <Button
                icon={<PlusOutlined />}
                onClick={() => {
                  noteForm.resetFields()
                  setSelectedReminder(null)
                  setCreateNoteOpen(true)
                }}
              >
                Ghi nhận chăm sóc tự do
              </Button>
            </Col>
          </Row>

          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={[
              {
                key: 'reminders',
                label: (
                  <span>
                    <CalendarOutlined /> Danh sách nhắc tái khám{' '}
                    {dueCount > 0 && <Badge count={dueCount} overflowCount={99} style={{ backgroundColor: '#ff4d4f' }} />}
                  </span>
                ),
                children: (
                  <Table
                    columns={reminderColumns}
                    dataSource={filteredReminders}
                    rowKey="id"
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                    locale={{ emptyText: 'Chưa có lịch nhắc tái khám nào' }}
                  />
                ),
              },
              {
                key: 'notes',
                label: (
                  <span>
                    <HistoryOutlined /> Nhật ký chăm sóc sau khám
                  </span>
                ),
                children: (
                  <Table
                    columns={noteColumns}
                    dataSource={filteredNotes}
                    rowKey="id"
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                    locale={{ emptyText: 'Chưa có ghi nhận nhật ký chăm sóc nào' }}
                  />
                ),
              },
            ]}
          />
        </Card>
      </Space>

      {/* Modal 1: Tạo lịch nhắc tái khám */}
      <Modal
        title="Tạo lịch nhắc tái khám mới"
        open={createReminderOpen}
        onCancel={() => setCreateReminderOpen(false)}
        onOk={() => reminderForm.submit()}
        confirmLoading={submitting}
        okText="Tạo lịch nhắc"
        cancelText="Hủy"
        destroyOnClose
      >
        <Alert
          message="Chỉ định tái khám của bác sĩ"
          description="Lễ tân tạo lịch nhắc tái khám dựa trên thời gian bác sĩ dặn dò khi kết thúc lượt khám."
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <Form form={reminderForm} layout="vertical" onFinish={handleCreateReminder}>
          <Form.Item
            name="patientId"
            label="Chọn Bệnh nhân"
            rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}
          >
            <Select
              showSearch
              placeholder="Gõ tên hoặc SĐT bệnh nhân..."
              optionFilterProp="children"
            >
              {patients.map((p) => (
                <Select.Option key={p.id} value={p.id}>
                  {p.fullName} - {p.phone || 'Chưa có SĐT'} ({p.patientCode || p.id})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="recordCode" label="Mã hồ sơ bệnh án">
            <Input placeholder="VD: BA-2026-001" />
          </Form.Item>

          <Form.Item name="doctorName" label="Bác sĩ chỉ định">
            <Input placeholder="VD: BS. Trần Văn Minh" />
          </Form.Item>

          <Form.Item
            name="remindDate"
            label="Ngày hẹn tái khám"
            rules={[{ required: true, message: 'Vui lòng chọn ngày tái khám' }]}
          >
            <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" placeholder="Chọn ngày" />
          </Form.Item>

          <Form.Item
            name="note"
            label="Ghi chú / Yêu cầu tái khám"
            rules={[{ required: true, message: 'Vui lòng nhập ghi chú dặn dò' }]}
          >
            <Input.TextArea rows={3} placeholder="VD: Đo lại huyết áp, làm lại công thức máu sau 2 tuần..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal 2: Ghi nhận chăm sóc sau khám (NCL-10-CN-002) */}
      <Modal
        title="Ghi nhận nội dung chăm sóc sau khám (NCL-10-CN-002)"
        open={createNoteOpen}
        onCancel={() => setCreateNoteOpen(false)}
        onOk={() => noteForm.submit()}
        confirmLoading={submitting}
        okText="Lưu ghi nhận"
        cancelText="Hủy"
        destroyOnClose
      >
        {selectedReminder && (
          <Alert
            message={`Đang chăm sóc cho: ${selectedReminder.patientName} (${selectedReminder.phone})`}
            description={`Lịch nhắc tái khám: ${formatDate(selectedReminder.remindDate)} - Ghi chú: ${selectedReminder.note}`}
            type="success"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        <Form form={noteForm} layout="vertical" onFinish={handleCreateNote}>
          {!selectedReminder && (
            <Form.Item
              name="patientId"
              label="Bệnh nhân"
              rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}
            >
              <Select showSearch placeholder="Chọn bệnh nhân...">
                {patients.map((p) => (
                  <Select.Option key={p.id} value={p.id}>
                    {p.fullName} - {p.phone || 'Chưa có SĐT'}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          )}

          <Form.Item
            name="contactMethod"
            label="Hình thức liên hệ"
            initialValue="PHONE_CALL"
            rules={[{ required: true }]}
          >
            <Select>
              <Select.Option value="PHONE_CALL">Gọi điện thoại</Select.Option>
              <Select.Option value="SMS">Gửi tin nhắn SMS</Select.Option>
              <Select.Option value="ZALO">Liên hệ Zalo</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="contactResult"
            label="Kết quả cuộc gọi / liên hệ"
            initialValue="SUCCESS"
            rules={[{ required: true }]}
          >
            <Select>
              <Select.Option value="SUCCESS">Thành công - Đã trao đổi / dặn dò xong</Select.Option>
              <Select.Option value="NO_ANSWER">Bệnh nhân không nghe máy</Select.Option>
              <Select.Option value="BUSY">Máy bận / Không liên lạc được</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="content"
            label="Nội dung chăm sóc / Phản hồi của bệnh nhân"
            rules={[{ required: true, message: 'Vui lòng nhập nội dung chăm sóc' }]}
          >
            <Input.TextArea
              rows={4}
              placeholder="VD: Bệnh nhân xác nhận uống thuốc đầy đủ, sức khỏe tiến triển tốt và hứa sẽ đi tái khám đúng hẹn..."
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AfterCarePage
