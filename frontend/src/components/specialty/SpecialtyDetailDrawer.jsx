import React, { useEffect, useState } from 'react'
import { Badge, Card, Descriptions, Drawer, Empty, List, Spin, Table, Tag, Typography } from 'antd'
import {
  ApartmentOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  FileTextOutlined,
  MedicineBoxOutlined,
  PhoneOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import specialtyApi from '../../api/specialtyApi'

const { Text, Title, Paragraph } = Typography

export default function SpecialtyDetailDrawer({ open, specialtyId, onClose }) {
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState(null)

  useEffect(() => {
    if (!open || !specialtyId) {
      setDetail(null)
      return
    }

    const fetchDetail = async () => {
      setLoading(true)
      try {
        const res = await specialtyApi.getById(specialtyId)
        setDetail(res.data)
      } catch (err) {
        console.error('Lỗi lấy chi tiết chuyên khoa:', err)
      } finally {
        setLoading(false)
      }
    }

    fetchDetail()
  }, [open, specialtyId])

  const isDefaultSpecialty = detail?.code === 'GENERAL'

  return (
    <Drawer
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <MedicineBoxOutlined style={{ color: '#059669', fontSize: 20 }} />
          <span>Chi tiết chuyên khoa</span>
        </div>
      }
      placement="right"
      width={600}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin size="large" tip="Đang tải dữ liệu chuyên khoa..." />
        </div>
      ) : detail ? (
        <div>
          <div style={{ marginBottom: 20 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
              <Title level={4} style={{ margin: 0, color: '#0f172a' }}>
                {detail.name}
              </Title>
              {detail.active ? (
                <Tag color="success" icon={<CheckCircleOutlined />}>
                  Đang hoạt động
                </Tag>
              ) : (
                <Tag color="default" icon={<CloseCircleOutlined />}>
                  Tạm ngừng dùng
                </Tag>
              )}
            </div>

            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <Tag color="blue" style={{ fontFamily: 'monospace', fontWeight: 600 }}>
                {detail.code}
              </Tag>
              {isDefaultSpecialty && (
                <Tag color="purple">Chuyên khoa mặc định</Tag>
              )}
            </div>
          </div>

          <Descriptions bordered column={1} size="small" style={{ marginBottom: 24 }}>
            <Descriptions.Item label="Mô tả">
              {detail.description ? (
                <Paragraph style={{ margin: 0 }}>{detail.description}</Paragraph>
              ) : (
                <Text type="secondary">Chưa có mô tả</Text>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Mẫu bệnh án áp dụng">
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <FileTextOutlined style={{ color: '#0284c7' }} />
                <Text strong>{detail.activeTemplateCount ?? 0}</Text> mẫu bệnh án đang hoạt động
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="Thời gian tạo">
              {detail.createdAt ? dayjs(detail.createdAt).format('HH:mm DD/MM/YYYY') : '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Cập nhật lần cuối">
              {detail.updatedAt ? dayjs(detail.updatedAt).format('HH:mm DD/MM/YYYY') : '—'}
            </Descriptions.Item>
          </Descriptions>

          <div style={{ marginBottom: 24 }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 12,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600, fontSize: 15 }}>
                <UserOutlined style={{ color: '#2563eb' }} />
                <span>Bác sĩ phụ trách ({detail.doctors?.length || 0})</span>
              </div>
            </div>

            {detail.doctors && detail.doctors.length > 0 ? (
              <List
                size="small"
                bordered
                dataSource={detail.doctors}
                renderItem={(doc) => (
                  <List.Item>
                    <div style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <Text strong style={{ color: '#1e293b' }}>
                          {doc.fullName || doc.username}
                        </Text>
                        <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                          (@{doc.username})
                        </Text>
                      </div>
                      <div style={{ display: 'flex', gap: 12, fontSize: 13, color: '#64748b' }}>
                        {doc.phone && (
                          <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                            <PhoneOutlined />
                            <span>{doc.phone}</span>
                          </span>
                        )}
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            ) : (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Chưa có bác sĩ nào được gán cho chuyên khoa này"
              />
            )}
          </div>

          <div>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 12,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600, fontSize: 15 }}>
                <ApartmentOutlined style={{ color: '#059669' }} />
                <span>Phòng khám bệnh trực thuộc ({detail.rooms?.length || 0})</span>
              </div>
            </div>

            {detail.rooms && detail.rooms.length > 0 ? (
              <List
                size="small"
                bordered
                dataSource={detail.rooms}
                renderItem={(room) => (
                  <List.Item>
                    <div style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <Text strong>{room.name}</Text>
                        <Tag style={{ marginLeft: 8 }} color="cyan">
                          {room.code}
                        </Tag>
                      </div>
                      <div>
                        {room.active ? (
                          <Badge status="success" text="Hoạt động" />
                        ) : (
                          <Badge status="default" text="Tạm đóng" />
                        )}
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            ) : (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Chưa có phòng khám bệnh nào được gán"
              />
            )}
          </div>
        </div>
      ) : (
        <Empty description="Không tìm thấy thông tin chuyên khoa" />
      )}
    </Drawer>
  )
}
