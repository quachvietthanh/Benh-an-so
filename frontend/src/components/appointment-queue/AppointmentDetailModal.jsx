import React from 'react'
import { Button, Divider, Modal, Table, Tag, Typography } from 'antd'
import {
  CheckCircleOutlined,
  HistoryOutlined,
  SwapOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { APPOINTMENT_STATUS_META } from '../../utils/queueHelpers'
import {
  canConfirmAppointment,
  formatConfirmationInfo,
} from '../../utils/appointmentConfirmValidation'
import { canRescheduleAppointment } from '../../utils/appointmentRescheduleValidation'

const { Text, Title, Paragraph } = Typography

export default function AppointmentDetailModal({
  open,
  onClose,
  detailItem,
  permissions = {},
  onConfirm,
  onReschedule,
}) {
  return (
    <Modal
      title="Chi Tiết Lịch Hẹn & Hàng Đợi"
      open={open}
      width={detailItem?.rescheduleHistories?.length ? 720 : 540}
      onCancel={onClose}
      footer={[
        permissions?.canConfirmAppointment &&
          detailItem?.type === 'appointment' &&
          detailItem?.status === 'SCHEDULED' &&
          canConfirmAppointment(detailItem, dayjs()).allowed && (
            <Button
              key="confirm"
              type="primary"
              icon={<CheckCircleOutlined />}
              style={{ backgroundColor: '#16a34a', borderColor: '#16a34a' }}
              onClick={() => {
                const target = { ...detailItem }
                onClose()
                if (onConfirm) onConfirm(target)
              }}
            >
              Xác nhận lịch hẹn này
            </Button>
          ),
        permissions?.canRescheduleAppointment &&
          detailItem?.type === 'appointment' &&
          canRescheduleAppointment(detailItem, dayjs()).allowed && (
            <Button
              key="reschedule"
              type="primary"
              icon={<SwapOutlined />}
              onClick={() => {
                const target = { ...detailItem }
                onClose()
                if (onReschedule) onReschedule(target)
              }}
            >
              Đổi lịch hẹn này
            </Button>
          ),
        <Button key="close" onClick={onClose}>
          Đóng
        </Button>,
      ].filter(Boolean)}
    >
      {detailItem && (
        <div>
          <Paragraph>
            <Text type="secondary">Mã lịch hẹn / Lượt khám:</Text>{' '}
            <Text code>{detailItem.appointmentCode || detailItem.visitCode || detailItem.id}</Text>
          </Paragraph>
          <Paragraph>
            <Text type="secondary">Bệnh nhân:</Text> <Text strong>{detailItem.patientName}</Text>
          </Paragraph>
          <Paragraph>
            <Text type="secondary">Bác sĩ phụ trách:</Text>{' '}
            <Text strong>{detailItem.doctorName || 'Chưa gán'}</Text>
          </Paragraph>
          <Paragraph>
            <Text type="secondary">Chuyên khoa:</Text>{' '}
            <Tag color="cyan">{detailItem.department || '—'}</Tag>
          </Paragraph>
          <Paragraph>
            <Text type="secondary">Trạng thái:</Text>{' '}
            <Tag
              color={
                APPOINTMENT_STATUS_META[detailItem.status]?.tone ||
                (detailItem.status === 'CONFIRMED' ? 'green' : 'blue')
              }
            >
              {APPOINTMENT_STATUS_META[detailItem.status]?.label || detailItem.status}
            </Tag>
          </Paragraph>
          {detailItem.status === 'CONFIRMED' && (
            <Paragraph>
              <Text type="secondary">Xác nhận:</Text>{' '}
              <Tag color="green" icon={<CheckCircleOutlined />}>
                Đã xác nhận
              </Tag>
              {detailItem.confirmedAt && (
                <Text style={{ fontSize: 13, color: '#166534', marginLeft: 6 }}>
                  ({formatConfirmationInfo(detailItem.confirmedByName, detailItem.confirmedAt)})
                </Text>
              )}
            </Paragraph>
          )}
          {detailItem.reason && (
            <Paragraph>
              <Text type="secondary">Lý do khám:</Text> {detailItem.reason}
            </Paragraph>
          )}
          {detailItem.cancelReason && (
            <Paragraph>
              <Text type="secondary">Lý do hủy / kết thúc sớm:</Text>{' '}
              <Text type="danger">{detailItem.cancelReason}</Text>
            </Paragraph>
          )}
          {detailItem.cancelledAt && (
            <Paragraph>
              <Text type="secondary">Thời gian đóng / hủy:</Text>{' '}
              {dayjs(detailItem.cancelledAt).format('HH:mm - DD/MM/YYYY')}
            </Paragraph>
          )}
          {(detailItem.appointmentAt || detailItem.startTime) && (
            <Paragraph>
              <Text type="secondary">Thời gian hẹn:</Text>{' '}
              {dayjs(detailItem.appointmentAt || detailItem.startTime).format('HH:mm - DD/MM/YYYY')}
            </Paragraph>
          )}

          {/* TC-04: Lịch sử đổi lịch hẹn */}
          {detailItem.rescheduleHistories && detailItem.rescheduleHistories.length > 0 && (
            <div style={{ marginTop: 16 }}>
              <Divider style={{ margin: '12px 0' }} />
              <Title level={5} style={{ marginBottom: 8 }}>
                <HistoryOutlined style={{ marginRight: 6 }} />
                Nhật ký dời lịch hẹn ({detailItem.rescheduleHistories.length})
              </Title>
              <Table
                dataSource={detailItem.rescheduleHistories}
                rowKey={(h) => h.id || `${h.rescheduledAt}-${h.rescheduledBy}`}
                pagination={false}
                size="small"
                columns={[
                  {
                    title: 'Thời điểm',
                    dataIndex: 'rescheduledAt',
                    width: 140,
                    render: (t) => dayjs(t).format('HH:mm - DD/MM/YYYY'),
                  },
                  {
                    title: 'Người đổi',
                    dataIndex: 'rescheduledByName',
                    width: 110,
                    render: (name, h) => name || h.rescheduledBy || 'Lễ tân',
                  },
                  {
                    title: 'Thay đổi',
                    render: (_, h) => (
                      <div style={{ fontSize: 12 }}>
                        <div>
                          <Text type="secondary">Giờ: </Text>
                          <Text delete>{dayjs(h.oldStartTime).format('HH:mm DD/MM')}</Text>
                          {' → '}
                          <Text strong style={{ color: '#1677ff' }}>
                            {dayjs(h.newStartTime).format('HH:mm DD/MM')}
                          </Text>
                        </div>
                        {(h.oldDoctorName !== h.newDoctorName || h.oldDoctorId !== h.newDoctorId) && (
                          <div style={{ marginTop: 2 }}>
                            <Text type="secondary">BS: </Text>
                            <Text delete>{h.oldDoctorName || 'BS cũ'}</Text>
                            {' → '}
                            <Text strong style={{ color: '#52c41a' }}>
                              {h.newDoctorName || 'BS mới'}
                            </Text>
                          </div>
                        )}
                      </div>
                    ),
                  },
                  {
                    title: 'Lý do',
                    dataIndex: 'reason',
                    render: (r) => <Text style={{ fontSize: 12 }}>{r}</Text>,
                  },
                ]}
              />
            </div>
          )}
        </div>
      )}
    </Modal>
  )
}
