import React from 'react'
import { Alert, Button, Modal, Space, Table, Tag, Tooltip, message } from 'antd'
import {
  CalendarOutlined,
  CopyOutlined,
  ExclamationCircleOutlined,
  ExportOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import { formatAppointmentStatus } from '../../utils/doctorScheduleHelpers.js'

/**
 * Modal displaying active appointments that conflict with a registered doctor time-off (TC-03).
 * Enables receptionists and clinic managers to review, copy codes, or navigate to appointment queue for rescheduling.
 */
function AffectedAppointmentsModal({ open, onClose, appointments = [], doctorName, timeOffRange }) {
  const navigate = useNavigate()

  const handleCopyAllCodes = () => {
    const codes = (appointments || [])
      .map((a) => a.appointmentCode)
      .filter(Boolean)
      .join(', ')
    if (!codes) return
    navigator.clipboard?.writeText(codes)
    message.success('Đã sao chép danh sách mã lịch hẹn vào clipboard!')
  }

  const handleGoToAppointments = () => {
    onClose()
    navigate('/appointments')
  }

  const columns = [
    {
      title: 'Mã lịch hẹn',
      dataIndex: 'appointmentCode',
      key: 'appointmentCode',
      width: 150,
      render: (code) => (
        <Space orientation="horizontal" size="small">
          <strong style={{ color: '#2563eb' }}>{code || 'N/A'}</strong>
          {code && (
            <Tooltip title="Sao chép mã">
              <Button
                type="text"
                size="small"
                icon={<CopyOutlined />}
                onClick={() => {
                  navigator.clipboard?.writeText(code)
                  message.success(`Đã sao chép mã ${code}`)
                }}
              />
            </Tooltip>
          )}
        </Space>
      ),
    },
    {
      title: 'Khung giờ hẹn',
      key: 'time',
      width: 190,
      render: (_, record) => {
        const start = record.startTime ? dayjs(record.startTime).format('HH:mm - DD/MM/YYYY') : '---'
        const end = record.endTime ? dayjs(record.endTime).format('HH:mm') : '---'
        return (
          <div>
            <div style={{ fontWeight: 500 }}>{start}</div>
            <small style={{ color: '#64748b' }}>đến {end}</small>
          </div>
        )
      },
    },
    {
      title: 'Mã bệnh nhân',
      dataIndex: 'patientId',
      key: 'patientId',
      ellipsis: true,
      render: (pid) => <span style={{ fontFamily: 'monospace' }}>{pid ? String(pid).slice(0, 8) + '...' : '---'}</span>,
    },
    {
      title: 'Lý do khám',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
      render: (reason) => reason || 'Khám theo yêu cầu',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status) => {
        const { label, color } = formatAppointmentStatus(status)
        return <Tag color={color}>{label}</Tag>
      },
    },
  ]

  const isWeeklySchedule =
    timeOffRange === 'Cấu hình lịch làm việc tuần mới' ||
    (typeof timeOffRange === 'string' && timeOffRange.toLowerCase().includes('lịch làm việc'))

  return (
    <Modal
      title={(
        <Space align="center" style={{ color: '#d97706' }}>
          <ExclamationCircleOutlined style={{ fontSize: 20 }} />
          <span>
            {isWeeklySchedule
              ? 'Danh sách Lịch hẹn bị ảnh hưởng do Thay đổi Lịch làm việc tuần'
              : 'Danh sách Lịch hẹn bị ảnh hưởng do Bác sĩ nghỉ đột xuất'}
          </span>
        </Space>
      )}
      open={open}
      onCancel={onClose}
      width={780}
      footer={[
        <Button key="copy-all" icon={<CopyOutlined />} onClick={handleCopyAllCodes}>
          Sao chép tất cả mã
        </Button>,
        <Button key="goto-appts" type="primary" icon={<ExportOutlined />} onClick={handleGoToAppointments}>
          Đi đến Hàng đợi lịch hẹn xử lý
        </Button>,
        <Button key="close" onClick={onClose}>
          Đóng
        </Button>,
      ]}
    >
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
        message={
          <strong>
            {isWeeklySchedule
              ? `Phát hiện ${appointments.length} lịch hẹn xung đột với lịch làm việc mới của bác sĩ ${doctorName ? `“${doctorName}”` : ''}!`
              : `Phát hiện ${appointments.length} lịch hẹn trùng với khoảng nghỉ của bác sĩ ${doctorName ? `“${doctorName}”` : ''}!`}
          </strong>
        }
        description={
          <div>
            {isWeeklySchedule ? (
              <div>
                Thay đổi cấu hình lịch tuần khiến một số lịch hẹn đã đặt trước đó rơi vào ngày nghỉ hoặc ngoài giờ làm việc mới.
                Nhân viên tiếp nhận cần chủ động liên hệ bệnh nhân theo danh sách dưới đây để thông báo dời ngày hoặc sắp xếp lại lịch hẹn.
              </div>
            ) : (
              <>
                {timeOffRange && <div style={{ marginBottom: 4 }}>Khoảng nghỉ: <strong>{timeOffRange}</strong></div>}
                <div>
                  Các khung giờ này đã bị khóa trên hệ thống. Nhân viên lễ tân cần chủ động liên hệ bệnh nhân theo danh sách dưới đây để thông báo dời ngày hoặc hủy lịch hẹn.
                </div>
              </>
            )}
          </div>
        }
      />

      <Table
        dataSource={appointments}
        columns={columns}
        rowKey={(r) => r.id || r.appointmentCode}
        pagination={appointments.length > 5 ? { pageSize: 5 } : false}
        size="small"
        bordered
      />
    </Modal>
  )
}

export default AffectedAppointmentsModal
