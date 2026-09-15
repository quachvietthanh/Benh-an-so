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
import appointmentApi from '../../api/appointmentApi.js'
import patientApi from '../../api/patientApi.js'
import userApi from '../../api/userApi.js'
import { handleQueueApiError } from '../../utils/queueHelpers.js'

const { Text, Title, Paragraph } = Typography

const cleanUuid = (id) => String(id || '').toLowerCase().replace(/-/g, '')

const formatDoctorName = (name) => {
  if (!name || name === 'Chưa gán' || name === 'Chưa phân công' || name === '—') return 'Chưa phân công'
  const trimmed = String(name).trim()
  if (
    trimmed.startsWith('BS.') ||
    trimmed.startsWith('BS ') ||
    trimmed.startsWith('Dr.') ||
    trimmed.startsWith('Bác sĩ')
  ) {
    return trimmed
  }
  return `BS. ${trimmed}`
}

export default function UnconfirmedAppointmentsDrawer({
  open,
  onClose,
  onAppointmentConfirmed,
  user,
  patients: patientsProp = [],
  doctors: doctorsProp = [],
  getPatientInfo: getPatientInfoProp,
  getDoctorInfo: getDoctorInfoProp,
}) {
  const [selectedDate, setSelectedDate] = useState(dayjs())
  const [loading, setLoading] = useState(false)
  const [appointments, setAppointments] = useState([])
  const [actionLoadingId, setActionLoadingId] = useState(null)
  const [localDoctors, setLocalDoctors] = useState([])
  const [localPatients, setLocalPatients] = useState([])

  const resolveDoctor = useCallback(
    (doctorId, fallbackName, fallbackDept) => {
      if (fallbackName && fallbackName !== 'Bác sĩ phụ trách' && fallbackName !== '—') {
        return {
          name: fallbackName,
          department: fallbackDept || '',
        }
      }
      if (getDoctorInfoProp) {
        const res = getDoctorInfoProp(doctorId, fallbackName, fallbackDept)
        if (res && res.name && res.name !== 'Bác sĩ chưa xác định' && !res.name.includes('#')) {
          return res
        }
      }
      const allDocs = [...(doctorsProp || []), ...(localDoctors || [])]
      const targetClean = cleanUuid(doctorId)
      const doc = allDocs.find((d) => {
        const dClean = cleanUuid(d.id)
        return (targetClean && dClean === targetClean) || String(d.id) === String(doctorId)
      })
      if (doc) {
        return {
          name: doc.fullName || doc.name || doc.username || fallbackName || 'BS. Chưa phân công',
          department: doc.department || fallbackDept || '',
        }
      }
      return {
        name: fallbackName || (doctorId ? `Bác sĩ #${doctorId}` : 'Chưa phân công'),
        department: fallbackDept || '',
      }
    },
    [getDoctorInfoProp, doctorsProp, localDoctors]
  )

  const resolvePatient = useCallback(
    (patientId, fallbackName, fallbackCode, fallbackPhone) => {
      if (fallbackName && fallbackName !== 'Bệnh nhân' && fallbackName !== '—') {
        return {
          name: fallbackName,
          code: fallbackCode || '—',
          phone: fallbackPhone || '',
        }
      }
      if (getPatientInfoProp) {
        const res = getPatientInfoProp(patientId, fallbackName, fallbackCode, fallbackPhone)
        if (res && res.name && res.name !== 'Bệnh nhân') {
          return res
        }
      }
      const allPats = [...(patientsProp || []), ...(localPatients || [])]
      const targetClean = cleanUuid(patientId)
      const pat = allPats.find((p) => {
        const pClean = cleanUuid(p.id)
        return (targetClean && pClean === targetClean) || String(p.id) === String(patientId)
      })
      if (pat) {
        return {
          name: pat.fullName || pat.name || fallbackName || 'Bệnh nhân',
          code: pat.patientCode || pat.code || fallbackCode || '—',
          phone: pat.phoneNumber || pat.phone || fallbackPhone || '',
        }
      }
      return {
        name: fallbackName || 'Bệnh nhân',
        code: fallbackCode || '—',
        phone: fallbackPhone || '',
      }
    },
    [getPatientInfoProp, patientsProp, localPatients]
  )

  useEffect(() => {
    if (!open) return

    if ((!doctorsProp || doctorsProp.length === 0) && localDoctors.length === 0) {
      userApi
        .getDoctors()
        .then((res) => {
          const dData = res?.data || res
          const list = Array.isArray(dData) ? dData : dData?.content || []
          if (list.length > 0) {
            setLocalDoctors(list)
          }
        })
        .catch(() => {})
    }

    if ((!patientsProp || patientsProp.length === 0) && localPatients.length === 0) {
      patientApi
        .getAll({ page: 0, size: 500 })
        .then((res) => {
          const pData = res?.data || res
          const list = Array.isArray(pData) ? pData : pData?.content || []
          if (list.length > 0) {
            setLocalPatients(list)
          }
        })
        .catch(() => {})
    }
  }, [open, doctorsProp, patientsProp, localDoctors.length, localPatients.length])

  const fetchUnconfirmed = useCallback(
    async (dateObj) => {
      setLoading(true)
      try {
        const dateStr = (dateObj || dayjs()).format('YYYY-MM-DD')
        const res = await appointmentApi.getUnconfirmed({ date: dateStr, size: 50 })
        const data = res?.data || res
        const items = Array.isArray(data) ? data : data?.content || []
        setAppointments(items)

        const knownPatientIds = new Set(
          [...(patientsProp || []), ...(localPatients || [])].map((p) => cleanUuid(p.id))
        )
        const missingIds = [
          ...new Set(
            items
              .filter((it) => !it.patientName && it.patientId)
              .map((it) => it.patientId)
              .filter((pid) => pid && !knownPatientIds.has(cleanUuid(pid)))
          ),
        ]

        if (missingIds.length > 0) {
          Promise.allSettled(missingIds.map((id) => patientApi.getById(id))).then((results) => {
            const fetched = []
            results.forEach((r) => {
              if (r.status === 'fulfilled') {
                const pat = r.value?.data || r.value
                if (pat && pat.id) {
                  fetched.push(pat)
                }
              }
            })
            if (fetched.length > 0) {
              setLocalPatients((prev) => {
                const existing = new Set(prev.map((p) => cleanUuid(p.id)))
                const toAdd = fetched.filter((p) => !existing.has(cleanUuid(p.id)))
                return toAdd.length > 0 ? [...prev, ...toAdd] : prev
              })
            }
          })
        }
      } catch (err) {
        handleQueueApiError(err, 'Không thể tải danh sách lịch hẹn chưa xác nhận')
      } finally {
        setLoading(false)
      }
    },
    [patientsProp]
  )

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
    const pInfo = resolvePatient(
      record.patientId,
      record.patientName,
      record.patientCode,
      record.phone || record.patientPhone
    )
    try {
      await appointmentApi.sendReminder(record.id)
      message.success(`Đã gửi thông báo nhắc lịch cho bệnh nhân ${pInfo.name}`)
    } catch (err) {
      handleQueueApiError(err, 'Không thể gửi nhắc nhở lịch hẹn')
    } finally {
      setActionLoadingId(null)
    }
  }

  const handleConfirmAppointment = (record) => {
    const timeVal = record.startTime || record.appointmentAt || record.date
    const appTime = dayjs(timeVal)
    const pInfo = resolvePatient(
      record.patientId,
      record.patientName,
      record.patientCode,
      record.phone || record.patientPhone
    )
    const dInfo = resolveDoctor(record.doctorId, record.doctorName, record.department)
    const docDisplay = formatDoctorName(dInfo.name)

    Modal.confirm({
      title: 'Xác nhận lịch hẹn khám',
      icon: <CheckCircleOutlined style={{ color: '#16a34a' }} />,
      content: (
        <div>
          <Paragraph style={{ marginBottom: 6 }}>
            Bệnh nhân: <strong>{pInfo.name}</strong> ({record.appointmentCode || record.id})
          </Paragraph>
          <Paragraph style={{ marginBottom: 6 }}>
            Bác sĩ phụ trách: <strong>{docDisplay}</strong>{' '}
            {dInfo.department && <Text type="secondary">({dInfo.department})</Text>}
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
          message.success(`Đã xác nhận lịch hẹn của ${pInfo.name} thành công!`)
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
        const pInfo = resolvePatient(
          record.patientId,
          record.patientName,
          record.patientCode,
          record.phone || record.patientPhone
        )
        const phone = pInfo.phone
        return (
          <Space orientation="left" size="small">
            <Avatar style={{ backgroundColor: '#e0f2fe', color: '#0284c7' }}>
              {pInfo.name ? pInfo.name.charAt(0).toUpperCase() : <UserOutlined />}
            </Avatar>
            <div>
              <Text strong style={{ display: 'block', fontSize: 13, color: '#0f172a', lineHeight: 1.3 }}>
                {pInfo.name}
              </Text>
              {pInfo.code && pInfo.code !== '—' && (
                <Text type="secondary" style={{ fontSize: 11, display: 'block' }}>
                  {pInfo.code}
                </Text>
              )}
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
      render: (_, record) => {
        const dInfo = resolveDoctor(record.doctorId, record.doctorName, record.department)
        return (
          <div>
            <Text strong style={{ fontSize: 13, display: 'block', color: '#0f172a' }}>
              {formatDoctorName(dInfo.name)}
            </Text>
            {dInfo.department && dInfo.department !== '—' && (
              <Tag color="cyan" style={{ fontSize: 11, marginTop: 2 }}>
                {dInfo.department}
              </Tag>
            )}
          </div>
        )
      },
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
