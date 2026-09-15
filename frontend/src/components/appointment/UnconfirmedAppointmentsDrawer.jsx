import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  DatePicker,
  Drawer,
  Empty,
  Modal,
  Row,
  Col,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  BellOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CopyOutlined,
  PhoneOutlined,
  ReloadOutlined,
  SendOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import appointmentApi from '../../api/appointmentApi'
import { handleQueueApiError } from '../../utils/queueHelpers'

const { Text, Title, Paragraph } = Typography

export default function UnconfirmedAppointmentsDrawer({
  open,
  onClose,
  onAppointmentConfirmed,
  user,
}) {
  const [selectedDate, setSelectedDate] = useState(dayjs())
  const [loading, setLoading] = useState(false)
  const [appointments, setAppointments] = useState([])
  const [actionLoadingId, setActionLoadingId] = useState(null)

  const fetchUnconfirmed = useCallback(async (dateObj) => {
    setLoading(true)
    try {
      const dateStr = (dateObj || dayjs()).format('YYYY-MM-DD')
      const res = await appointmentApi.getUnconfirmed({ date: dateStr, size: 50 })
      const data = res?.data || res
      const items = Array.isArray(data) ? data : data?.content || []
      setAppointments(items)
    } catch (err) {
      handleQueueApiError(err, 'Không thể tải danh sách lịch hẹn chưa xác nhận')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (open) {
      fetchUnconfirmed(selectedDate)
    }
  }, [open, selectedDate, fetchUnconfirmed])

  const handleDateChange = (date) => {
    if (date) {
      setSelectedDate(date)
    }
  }

  const handleSendReminder = async (record) => {
    setActionLoadingId(record.id)
    try {
      await appointmentApi.sendReminder(record.id)
      message.success(`Đã gửi thông báo nhắc lịch cho bệnh nhân ${record.patientName || record.appointmentCode}`)
    } catch (err) {
      handleQueueApiError(err, 'Không thể gửi nhắc nhở lịch hẹn')
    } finally {
      setActionLoadingId(null)
    }
  }

  const handleConfirmAppointment = (record) => {
    const timeVal = record.startTime || record.appointmentAt || record.date
    const appTime = dayjs(timeVal)
    const pName = record.patientName || 'Bệnh nhân'
    const docName = record.doctorName
      ? `BS. ${record.doctorName}`
      : record.doctorId
        ? `Bác sĩ #${record.doctorId}`
        : 'Chưa gán bác sĩ'

    Modal.confirm({
      title: 'Xác nhận lịch hẹn khám',
      icon: <CheckCircleOutlined style={{ color: '#16a34a' }} />,
      content: (
        <div>
          <Paragraph style={{ marginBottom: 6 }}>
            Bệnh nhân: <strong>{pName}</strong> ({record.appointmentCode || record.id})
          </Paragraph>
          <Paragraph style={{ marginBottom: 6 }}>
            Bác sĩ phụ trách: <strong>{docName}</strong>{' '}
            {record.department && <Text type="secondary">({record.department})</Text>}
          </Paragraph>
          <Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 0 }}>
            Khung giờ hẹn:{' '}
            <strong style={{ color: '#0f172a' }}>
              {appTime.isValid() ? appTime.format('HH:mm - DD/MM/YYYY') : 'Trong ngày'}
            </strong>
          </Paragraph>
        </div>
      ),
      okText: 'Xác nhận đến khám',
      okButtonProps: { style: { backgroundColor: '#16a34a', borderColor: '#16a34a' } },
      cancelText: 'Đóng',
      onOk: async () => {
        setActionLoadingId(record.id)
        try {
          await appointmentApi.confirm(record.id)
          message.success(`Đã xác nhận lịch hẹn của ${pName} thành công!`)
          // Remove from local list
          setAppointments((prev) => prev.filter((item) => item.id !== record.id))
          if (onAppointmentConfirmed) {
            onAppointmentConfirmed(record.id)
          }
        } catch (err) {
          handleQueueApiError(err, 'Không thể xác nhận lịch hẹn')
        } finally {
          setActionLoadingId(null)
        }
      },
    })
  }

  const handleCopyPhone = (phone) => {
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(phone)
      message.success(`Đã sao chép SĐT: ${phone}`)
    } else {
      message.info(`Số điện thoại: ${phone}`)
    }
  }

  const columns = [
    {
      title: 'Khung giờ',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 140,
      render: (startTime, record) => {
        const start = startTime ? dayjs(startTime) : null
        const end = record.endTime ? dayjs(record.endTime) : null
        const timeStr = start?.isValid()
          ? `${start.format('HH:mm')}${end?.isValid() ? ` - ${end.format('HH:mm')}` : ''}`
          : 'Trong ngày'

        const isSoon = start?.isValid() && dayjs().isAfter(start.subtract(2, 'hour')) && dayjs().isBefore(start)

        return (
          <Space direction="vertical" orientation="left" size={2}>
            <Text strong style={{ color: '#0f172a', whiteSpace: 'nowrap' }}>
              <ClockCircleOutlined style={{ marginRight: 4, color: '#2563eb' }} />
              {timeStr}
            </Text>
            {isSoon && (
              <Tag color="orange" style={{ fontSize: 11, margin: 0 }}>
                Sắp tới giờ (&lt;2h)
              </Tag>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Mã LH',
      dataIndex: 'appointmentCode',
      key: 'appointmentCode',
      width: 120,
      render: (code) => (
        <Text strong style={{ color: '#2563eb', whiteSpace: 'nowrap' }}>
          {code || 'Chưa có'}
        </Text>
      ),
    },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      width: 220,
      render: (_, record) => {
        const phone = record.phone || record.patientPhone
        return (
          <Space orientation="left" size="small">
            <Avatar style={{ backgroundColor: '#e0f2fe', color: '#0284c7' }}>
              {record.patientName ? record.patientName.charAt(0).toUpperCase() : <UserOutlined />}
            </Avatar>
            <div>
              <Text strong style={{ display: 'block', fontSize: 13, color: '#0f172a', lineHeight: 1.3 }}>
                {record.patientName || 'Bệnh nhân'}
              </Text>
              {phone ? (
                <Space size={4} style={{ marginTop: 2 }}>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    <PhoneOutlined style={{ color: '#16a34a' }} /> {phone}
                  </Text>
                  <Tooltip title="Sao chép số điện thoại">
                    <Button
                      type="text"
                      size="small"
                      icon={<CopyOutlined style={{ fontSize: 11, color: '#64748b' }} />}
                      onClick={() => handleCopyPhone(phone)}
                      style={{ padding: '0 2px', height: 'auto' }}
                    />
                  </Tooltip>
                </Space>
              ) : (
                <Text type="secondary" style={{ fontSize: 11 }}>Chưa có SĐT</Text>
              )}
            </div>
          </Space>
        )
      },
    },
    {
      title: 'Bác sĩ phụ trách',
      key: 'doctor',
      width: 180,
      render: (_, record) => (
        <div>
          <Text strong style={{ fontSize: 13, display: 'block' }}>
            {record.doctorName || 'Bác sĩ phụ trách'}
          </Text>
          {record.department && (
            <Tag color="cyan" style={{ fontSize: 11, marginTop: 2 }}>
              {record.department}
            </Tag>
          )}
        </div>
      ),
    },
    {
      title: 'Lý do khám',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
      render: (reason) => (
        <Text type="secondary" style={{ fontSize: 12 }}>
          {reason || 'Khám tổng quát'}
        </Text>
      ),
    },
    {
      title: 'Thao tác liên hệ & xác nhận',
      key: 'action',
      width: 200,
      align: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="Gửi tin nhắn nhắc hẹn">
            <Button
              size="small"
              icon={<SendOutlined />}
              loading={actionLoadingId === record.id}
              onClick={() => handleSendReminder(record)}
            >
              Nhắc lịch
            </Button>
          </Tooltip>
          <Button
            size="small"
            type="primary"
            icon={<CheckCircleOutlined />}
            loading={actionLoadingId === record.id}
            style={{ backgroundColor: '#16a34a', borderColor: '#16a34a' }}
            onClick={() => handleConfirmAppointment(record)}
          >
            Xác nhận
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <Drawer
      title={
        <Space align="center">
          <BellOutlined style={{ color: '#eab308', fontSize: 18 }} />
          <span>Danh Sách Lịch Hẹn Chưa Xác Nhận</span>
          <Badge count={appointments.length} overflowCount={99} style={{ backgroundColor: '#eab308' }} />
        </Space>
      }
      open={open}
      onClose={onClose}
      width={900}
      extra={
        <Space>
          <DatePicker
            value={selectedDate}
            onChange={handleDateChange}
            format="DD/MM/YYYY"
            allowClear={false}
          />
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchUnconfirmed(selectedDate)}
            loading={loading}
          >
            Làm mới
          </Button>
        </Space>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <Alert
          type="info"
          showIcon
          message="Hướng dẫn kiểm tra & nhắc lịch hẹn"
          description="Danh sách hiển thị các lịch hẹn đã đặt trong ngày chưa được xác nhận, sắp xếp theo khung giờ khám tăng dần. Lễ tân liên hệ bệnh nhân qua số điện thoại để gọi nhắc và bấm 'Xác nhận' khi bệnh nhân đồng ý đến khám."
          style={{ backgroundColor: '#f0fdf4', borderColor: '#bbf7d0', fontSize: 13 }}
        />

        <Table
          dataSource={appointments}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 8, showSizeChanger: true }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  <span>
                    Không có lịch hẹn nào chưa xác nhận trong ngày{' '}
                    <strong>{selectedDate.format('DD/MM/YYYY')}</strong>.
                  </span>
                }
              />
            ),
          }}
          scroll={{ x: 800 }}
        />
      </div>
    </Drawer>
  )
}
