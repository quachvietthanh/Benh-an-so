import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  PhoneOutlined,
  PlusOutlined,
  ReloadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import appointmentWaitlistApi from '../../api/appointmentWaitlistApi.js'
import {
  TIME_PREFERENCE_LABELS,
  WAITLIST_STATUSES,
  getWaitlistStatusTag,
  mapWaitlistErrorMessage,
} from '../../utils/appointmentWaitlistHelpers.js'
import AddToWaitlistModal from './AddToWaitlistModal.jsx'

const { Text, Paragraph } = Typography

export default function AppointmentWaitlistPanel({
  doctorList = [],
  patients = [],
  onOpenDirectBooking,
  permissions = {},
}) {
  const [loading, setLoading] = useState(false)
  const [waitlistData, setWaitlistData] = useState([])

  // Bộ lọc
  const [filterDoctorId, setFilterDoctorId] = useState('ALL')
  const [filterDate, setFilterDate] = useState(null)
  const [filterStatus, setFilterStatus] = useState(WAITLIST_STATUSES.WAITING)

  // Modal thêm vào danh sách chờ
  const [addModalOpen, setAddModalOpen] = useState(false)

  // Modal hủy đăng ký chờ
  const [cancelModalOpen, setCancelModalOpen] = useState(false)
  const [cancelTargetItem, setCancelTargetItem] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [cancelling, setCancelling] = useState(false)

  const fetchWaitlist = useCallback(async () => {
    setLoading(true)
    try {
      const params = {}
      if (filterDoctorId && filterDoctorId !== 'ALL') {
        params.doctorId = filterDoctorId
      }
      if (filterDate) {
        params.date = filterDate.format('YYYY-MM-DD')
      }
      if (filterStatus && filterStatus !== 'ALL') {
        params.status = filterStatus
      }

      const res = await appointmentWaitlistApi.list(params)
      // TUYỆT ĐỐI KHÔNG tự sort lại ở Frontend — giữ nguyên mảng FIFO từ Backend
      const list = Array.isArray(res.data) ? res.data : []
      setWaitlistData(list)
    } catch (err) {
      message.error(mapWaitlistErrorMessage(err))
      setWaitlistData([])
    } finally {
      setLoading(false)
    }
  }, [filterDoctorId, filterDate, filterStatus])

  useEffect(() => {
    fetchWaitlist()
  }, [fetchWaitlist])

  // Xử lý xác nhận hủy đăng ký chờ
  const handleConfirmCancel = async () => {
    if (!cancelReason || cancelReason.trim() === '') {
      message.error('Vui lòng nhập lý do hủy đăng ký chờ!')
      return
    }

    if (cancelReason.length > 500) {
      message.error('Lý do hủy không được vượt quá 500 ký tự!')
      return
    }

    setCancelling(true)
    try {
      await appointmentWaitlistApi.cancel(cancelTargetItem.id, cancelReason.trim())
      message.success('Đã hủy đăng ký danh sách chờ thành công.')
      setCancelModalOpen(false)
      setCancelTargetItem(null)
      setCancelReason('')
      fetchWaitlist()
    } catch (err) {
      message.error(mapWaitlistErrorMessage(err))
    } finally {
      setCancelling(false)
    }
  }

  const columns = [
    {
      title: 'STT (Thứ tự ưu tiên)',
      key: 'priorityIndex',
      width: 130,
      align: 'center',
      render: (_, record, index) => {
        const order = index + 1
        if (record.status === WAITLIST_STATUSES.WAITING) {
          return (
            <Space orientation="vertical" size={2}>
              <Badge
                count={`#${order}`}
                style={{
                  backgroundColor: order === 1 ? '#faad14' : order === 2 ? '#1677ff' : '#52c41a',
                  fontWeight: 600,
                  fontSize: 13,
                }}
              />
              {order === 1 && (
                <Tag color="gold" style={{ fontSize: 11, margin: 0 }}>
                  Ưu tiên 1
                </Tag>
              )}
            </Space>
          )
        }
        return <Text type="secondary">#{order}</Text>
      },
    },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      render: (_, record) => (
        <div>
          <Space>
            <UserOutlined style={{ color: '#1677ff' }} />
            <Text strong>{record.patientName || 'Chưa có tên'}</Text>
          </Space>
          {record.patientPhone && (
            <div style={{ marginTop: 2 }}>
              <Space size={4}>
                <PhoneOutlined style={{ color: '#52c41a', fontSize: 12 }} />
                <a href={`tel:${record.patientPhone}`} style={{ color: '#389e0d' }}>
                  {record.patientPhone}
                </a>
              </Space>
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Bác sĩ phụ trách',
      dataIndex: 'doctorName',
      key: 'doctorName',
      render: (name) => <Text>{name || 'Bác sĩ chuyên khoa'}</Text>,
    },
    {
      title: 'Ngày mong muốn',
      dataIndex: 'desiredDate',
      key: 'desiredDate',
      width: 140,
      render: (date) => (
        <Space>
          <CalendarOutlined style={{ color: '#1677ff' }} />
          <Text strong>{date ? dayjs(date).format('DD/MM/YYYY') : '-'}</Text>
        </Space>
      ),
    },
    {
      title: 'Khung giờ',
      dataIndex: 'timePreference',
      key: 'timePreference',
      width: 130,
      render: (pref) => {
        const label = TIME_PREFERENCE_LABELS[pref] || pref || 'Bất kỳ lúc nào'
        return <Tag color="blue">{label}</Tag>
      },
    },
    {
      title: 'Ghi chú',
      dataIndex: 'note',
      key: 'note',
      ellipsis: true,
      render: (note) => note || <Text type="secondary">-</Text>,
    },
    {
      title: 'Thời điểm đăng ký',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (at) => (
        <Tooltip title={at ? dayjs(at).format('HH:mm:ss DD/MM/YYYY') : ''}>
          <Space>
            <ClockCircleOutlined style={{ color: '#8c8c8c' }} />
            <span>{at ? dayjs(at).format('HH:mm DD/MM/YYYY') : '-'}</span>
          </Space>
        </Tooltip>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => {
        const tag = getWaitlistStatusTag(status)
        return <Tag color={tag.color}>{tag.label}</Tag>
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 180,
      align: 'center',
      render: (_, record) => {
        if (record.status === WAITLIST_STATUSES.WAITING) {
          return (
            <Space>
              {onOpenDirectBooking && (
                <Button
                  size="small"
                  type="primary"
                  icon={<CalendarOutlined />}
                  onClick={() =>
                    onOpenDirectBooking({
                      patientId: record.patientId,
                      patientName: record.patientName,
                      doctorId: record.doctorId,
                      desiredDate: record.desiredDate,
                    })
                  }
                >
                  Đặt lịch
                </Button>
              )}
              <Button
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={() => {
                  setCancelTargetItem(record)
                  setCancelReason('')
                  setCancelModalOpen(true)
                }}
              >
                Hủy chờ
              </Button>
            </Space>
          )
        }

        if (record.status === WAITLIST_STATUSES.CANCELLED && record.cancelReason) {
          return (
            <Tooltip title={`Lý do hủy: ${record.cancelReason}`}>
              <Text type="secondary" italic style={{ fontSize: 12 }}>
                Đã hủy ({record.cancelReason})
              </Text>
            </Tooltip>
          )
        }

        if (record.status === WAITLIST_STATUSES.SCHEDULED) {
          return (
            <Tag color="green">Đã đặt lịch hẹn</Tag>
          )
        }

        return <Text type="secondary">-</Text>
      },
    },
  ]

  return (
    <Card style={{ borderRadius: 12 }}>
      {/* Alert quy tắc ưu tiên */}
      <Alert
        type="info"
        showIcon
        message="Thứ tự ưu tiên danh sách chờ"
        description="Bệnh nhân đăng ký trước sẽ được hệ thống tự động ưu tiên gợi ý khi có lịch hẹn cùng ngày của bác sĩ bị hủy."
        style={{ marginBottom: 16 }}
      />

      {/* Bộ lọc & Thanh công cụ */}
      <Row gutter={[12, 12]} style={{ marginBottom: 16 }} align="middle" justify="space-between">
        <Col xs={24} sm={18} md={18}>
          <Space wrap size="middle">
            <Select
              style={{ width: 200 }}
              placeholder="Chọn bác sĩ"
              value={filterDoctorId}
              onChange={setFilterDoctorId}
              options={[
                { value: 'ALL', label: 'Tất cả Bác sĩ' },
                ...doctorList.map((d) => ({
                  value: d.id,
                  label: d.fullName || d.username,
                })),
              ]}
            />

            <DatePicker
              placeholder="Lọc theo ngày khám"
              format="DD/MM/YYYY"
              value={filterDate}
              onChange={setFilterDate}
              allowClear
            />

            <Select
              style={{ width: 150 }}
              value={filterStatus}
              onChange={setFilterStatus}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: WAITLIST_STATUSES.WAITING, label: 'Đang chờ' },
                { value: WAITLIST_STATUSES.SCHEDULED, label: 'Đã đặt lịch' },
                { value: WAITLIST_STATUSES.CANCELLED, label: 'Đã hủy' },
                { value: WAITLIST_STATUSES.EXPIRED, label: 'Hết hạn' },
              ]}
            />

            <Button icon={<ReloadOutlined />} onClick={fetchWaitlist}>
              Làm mới
            </Button>
          </Space>
        </Col>

        <Col xs={24} sm={6} md={6} style={{ textAlign: 'right' }}>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setAddModalOpen(true)}
          >
            Thêm vào danh sách chờ
          </Button>
        </Col>
      </Row>

      {/* Bảng danh sách chờ */}
      <Table
        dataSource={waitlistData}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10, showSizeChanger: true }}
        scroll={{ x: 1000 }}
        locale={{
          emptyText:
            filterStatus === WAITLIST_STATUSES.WAITING
              ? 'Hiện không có bệnh nhân nào trong danh sách chờ.'
              : 'Không tìm thấy dữ liệu danh sách chờ.',
        }}
      />

      {/* Modal Thêm vào danh sách chờ */}
      <AddToWaitlistModal
        open={addModalOpen}
        onCancel={() => setAddModalOpen(false)}
        onSuccess={() => {
          fetchWaitlist()
        }}
        patients={patients}
        doctorList={doctorList}
        initialDoctorId={filterDoctorId !== 'ALL' ? filterDoctorId : undefined}
        initialDesiredDate={filterDate ? filterDate.format('YYYY-MM-DD') : undefined}
        onOpenDirectBooking={onOpenDirectBooking}
      />

      {/* Modal xác nhận Hủy đăng ký chờ */}
      <Modal
        title="Hủy Đăng Ký Danh Sách Chờ"
        open={cancelModalOpen}
        onCancel={() => {
          setCancelModalOpen(false)
          setCancelTargetItem(null)
          setCancelReason('')
        }}
        footer={null}
        destroyOnClose
      >
        <Paragraph>
          Bạn có chắc chắn muốn hủy đăng ký chờ của bệnh nhân{' '}
          <Text strong>{cancelTargetItem?.patientName}</Text> (Bác sĩ:{' '}
          <Text strong>{cancelTargetItem?.doctorName}</Text>, Ngày:{' '}
          <Text strong>
            {cancelTargetItem?.desiredDate
              ? dayjs(cancelTargetItem.desiredDate).format('DD/MM/YYYY')
              : ''}
          </Text>
          )?
        </Paragraph>

        <Form layout="vertical" onFinish={handleConfirmCancel}>
          <Form.Item
            label="Lý do hủy đăng ký chờ"
            required
            rules={[{ required: true, message: 'Vui lòng nhập lý do hủy!' }]}
          >
            <Input.TextArea
              rows={3}
              maxLength={500}
              showCount
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              placeholder="VD: Bệnh nhân đã tìm được cơ sở khám khác, bệnh nhân không còn nhu cầu..."
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button
                onClick={() => {
                  setCancelModalOpen(false)
                  setCancelTargetItem(null)
                  setCancelReason('')
                }}
              >
                Quay lại
              </Button>
              <Button type="primary" danger htmlType="submit" loading={cancelling}>
                Xác nhận Hủy chờ
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
