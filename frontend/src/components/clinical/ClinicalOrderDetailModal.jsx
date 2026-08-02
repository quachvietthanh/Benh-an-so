import React from 'react'
import {
  Modal,
  Descriptions,
  Table,
  Tag,
  Typography,
  Divider,
  Button,
  Space,
  Row,
  Col,
  Card,
} from 'antd'
import {
  PrinterOutlined,
  CheckCircleOutlined,
  UserOutlined,
  MedicineBoxOutlined,
  FileTextOutlined,
} from '@ant-design/icons'
import ClinicalOrderStatusBadge, { ClinicalOrderPriorityBadge } from './ClinicalOrderStatusBadge'
import ClinicalOrderStatusTimeline from './ClinicalOrderStatusTimeline'

const { Title, Text, Paragraph } = Typography

export const ClinicalOrderDetailModal = ({ visible, order, onClose, onPrintOrder }) => {
  if (!order) return null

  const serviceColumns = [
    {
      title: 'STT',
      key: 'stt',
      width: 60,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Mã Dịch vụ',
      dataIndex: 'serviceCode',
      key: 'serviceCode',
      width: 110,
      render: (code) => <Tag color="blue" style={{ fontWeight: 600 }}>{code || 'CLS'}</Tag>,
    },
    {
      title: 'Tên Dịch vụ cận lâm sàng',
      dataIndex: 'serviceName',
      key: 'serviceName',
      render: (name, record) => (
        <div>
          <Text strong>{name}</Text>
          {record.note && (
            <div style={{ fontSize: 12, color: '#fa8c16', marginTop: 2 }}>
              Ghi chú: {record.note}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Nhóm dịch vụ',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 140,
      render: (cat) => <Tag color="purple">{cat || 'Xét nghiệm'}</Tag>,
    },
    {
      title: 'Đơn giá',
      dataIndex: 'price',
      key: 'price',
      width: 120,
      align: 'right',
      render: (price) => `${Number(price || 0).toLocaleString('vi-VN')} đ`,
    },
    {
      title: 'Trạng thái item',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      align: 'center',
      render: (st) => <ClinicalOrderStatusBadge status={st || order.status} />,
    },
  ]

  return (
    <Modal
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: 24 }}>
          <Space>
            <FileTextOutlined style={{ color: '#1890ff', fontSize: 20 }} />
            <span style={{ fontSize: 18, fontWeight: 600 }}>Phiếu Chỉ định Cận lâm sàng #{order.orderCode}</span>
          </Space>
          <ClinicalOrderPriorityBadge priority={order.priority} />
        </div>
      }
      open={visible}
      onCancel={onClose}
      footer={[
        <Button key="close" onClick={onClose}>
          Đóng
        </Button>,
        <Button
          key="print"
          type="primary"
          icon={<PrinterOutlined />}
          onClick={() => onPrintOrder(order)}
        >
          In phiếu chỉ định
        </Button>,
      ]}
      width={900}
      style={{ top: 20 }}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        {/* Timeline Status */}
        <ClinicalOrderStatusTimeline
          status={order.status}
          createdAt={order.createdAt}
          updatedAt={order.updatedAt}
          cancelledBy={order.cancelledBy}
          cancelReason={order.cancelReason}
        />

        {/* Patient Demographics & Order Metadata */}
        <Card size="small" style={{ background: '#fafafa', borderRadius: 8 }}>
          <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }} bordered>
            <Descriptions.Item label="Mã bệnh nhân">
              <Text strong style={{ color: '#1890ff' }}>{order.patientCode}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Họ và tên bệnh nhân">
              <Text strong>{order.patientName}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Giới tính / Tuổi">
              {order.gender} ({order.age} tuổi)
            </Descriptions.Item>

            <Descriptions.Item label="Số điện thoại">
              {order.phone || 'Chưa cập nhật'}
            </Descriptions.Item>
            <Descriptions.Item label="Bác sĩ chỉ định">
              <Text strong>{order.doctorName || 'BS. Trực'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Khoa chỉ định">
              {order.department || 'Khoa Nội'}
            </Descriptions.Item>

            <Descriptions.Item label="Chẩn đoán lâm sàng" span={3}>
              <Text strong style={{ color: '#d46b08' }}>{order.diagnosis || 'Chưa ghi nhận'}</Text>
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* Services Table */}
        <div>
          <Title level={5} style={{ marginBottom: 12 }}>
            Danh sách dịch vụ chỉ định ({Array.isArray(order.items) ? order.items.length : 0})
          </Title>
          <Table
            columns={serviceColumns}
            dataSource={Array.isArray(order.items) ? order.items : []}
            rowKey={(r, idx) => r?.serviceId || idx}
            pagination={false}
            size="small"
            bordered
          />
        </div>

        {/* Results Summary Section (if available) */}
        {order.resultSummary && (
          <Card size="small" style={{ background: '#f6ffed', borderColor: '#b7eb8f', borderRadius: 8 }}>
            <Title level={5} style={{ color: '#389e0d', marginTop: 0, marginBottom: 8 }}>
              <CheckCircleOutlined /> Tóm tắt kết quả cận lâm sàng
            </Title>
            <Paragraph style={{ margin: 0, fontSize: 14 }}>
              {order.resultSummary}
            </Paragraph>
          </Card>
        )}

        {/* Order Fee Summary */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', background: '#fff1f0', padding: '12px 16px', borderRadius: 6, border: '1px solid #ffa39e' }}>
          <Text style={{ fontSize: 15, marginRight: 16 }}>Tổng phí dịch vụ cận lâm sàng:</Text>
          <Title level={3} style={{ color: '#cf1322', margin: 0 }}>
            {Number(order.totalAmount || 0).toLocaleString('vi-VN')} đ
          </Title>
        </div>
      </Space>
    </Modal>
  )
}

export default ClinicalOrderDetailModal
