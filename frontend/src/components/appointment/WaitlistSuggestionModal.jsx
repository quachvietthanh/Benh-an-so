import React from 'react'
import {
  Button,
  Card,
  Col,
  Descriptions,
  Modal,
  Row,
  Space,
  Tag,
  Typography,
} from 'antd'
import {
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  PhoneOutlined,
  SmileOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import { TIME_PREFERENCE_LABELS } from '../../utils/appointmentWaitlistHelpers.js'

dayjs.extend(relativeTime)

const { Title, Text, Paragraph } = Typography

export default function WaitlistSuggestionModal({
  open,
  suggestion,
  onClose,
  onBookForPatient,
}) {
  if (!suggestion) return null

  const {
    patientName,
    patientPhone,
    doctorName,
    desiredDate,
    timePreference,
    note,
    createdAt,
  } = suggestion

  const timeLabel = TIME_PREFERENCE_LABELS[timePreference] || timePreference || 'Bất kỳ lúc nào'
  const formattedDate = desiredDate ? dayjs(desiredDate).format('DD/MM/YYYY') : 'Hôm nay'
  const waitingSince = createdAt ? dayjs(createdAt).fromNow() : ''

  return (
    <Modal
      title={
        <Space>
          <SmileOutlined style={{ color: '#52c41a', fontSize: 20 }} />
          <span style={{ color: '#389e0d', fontWeight: 600 }}>
            🎉 Có khung giờ trống! Gợi ý liên hệ ngay:
          </span>
        </Space>
      }
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnClose
      width={560}
    >
      <div style={{ marginBottom: 16 }}>
        <Paragraph type="secondary" style={{ marginBottom: 12 }}>
          Lịch hẹn vừa bị hủy đã giải phóng khung giờ trống của bác sĩ. Dưới đây là bệnh nhân đang chờ đầu tiên theo nguyên tắc FIFO phù hợp với ngày và bác sĩ này:
        </Paragraph>

        <Card
          bordered={false}
          style={{
            background: 'linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%)',
            border: '1px solid #b7eb8f',
            borderRadius: 10,
          }}
        >
          <Descriptions column={1} size="small" labelStyle={{ fontWeight: 600, color: '#262626' }}>
            <Descriptions.Item label="Họ và tên bệnh nhân">
              <Space>
                <UserOutlined style={{ color: '#1677ff' }} />
                <Text strong style={{ fontSize: 16, color: '#1d39c4' }}>
                  {patientName || 'Chưa có tên'}
                </Text>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Số điện thoại liên hệ">
              {patientPhone ? (
                <Space>
                  <PhoneOutlined style={{ color: '#52c41a' }} />
                  <a
                    href={`tel:${patientPhone}`}
                    style={{
                      fontSize: 16,
                      fontWeight: 600,
                      color: '#389e0d',
                      textDecoration: 'underline',
                    }}
                  >
                    {patientPhone}
                  </a>
                  <Tag color="green">Nhấn để gọi</Tag>
                </Space>
              ) : (
                <Text type="secondary">Chưa cập nhật SĐT</Text>
              )}
            </Descriptions.Item>

            <Descriptions.Item label="Bác sĩ phụ trách">
              <Text strong>{doctorName || 'Bác sĩ chuyên khoa'}</Text>
            </Descriptions.Item>

            <Descriptions.Item label="Ngày mong muốn khám">
              <Space>
                <CalendarOutlined style={{ color: '#1677ff' }} />
                <Text strong>{formattedDate}</Text>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Khung giờ mong muốn">
              <Tag color="blue">{timeLabel}</Tag>
            </Descriptions.Item>

            {createdAt && (
              <Descriptions.Item label="Thời điểm đăng ký">
                <Space>
                  <ClockCircleOutlined />
                  <span>
                    {dayjs(createdAt).format('HH:mm DD/MM/YYYY')}
                    {waitingSince && ` (${waitingSince})`}
                  </span>
                </Space>
              </Descriptions.Item>
            )}

            {note && (
              <Descriptions.Item label="Ghi chú nhu cầu">
                <Text italic>{note}</Text>
              </Descriptions.Item>
            )}
          </Descriptions>
        </Card>
      </div>

      <div style={{ textAlign: 'right', marginTop: 16 }}>
        <Space size="middle">
          <Button onClick={onClose}>Bỏ qua</Button>
          <Button
            type="primary"
            icon={<CalendarOutlined />}
            style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
            onClick={() => onBookForPatient?.(suggestion)}
          >
            Đặt lịch cho bệnh nhân này
          </Button>
        </Space>
      </div>
    </Modal>
  )
}
