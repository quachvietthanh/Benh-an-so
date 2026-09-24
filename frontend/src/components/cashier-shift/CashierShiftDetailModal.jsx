import React, { useEffect, useState } from 'react'
import {
  Modal,
  Descriptions,
  Tag,
  Typography,
  Divider,
  Spin,
  Alert,
  Row,
  Col,
  Card,
  Statistic,
  Space,
  Button,
} from 'antd'
import {
  DollarOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ClockCircleOutlined,
  UserOutlined,
} from '@ant-design/icons'
import cashierShiftApi from '../../api/cashierShiftApi.js'
import {
  formatCurrency,
  formatDateTime,
  getShiftStatusMeta,
  mapCashierShiftError,
} from '../../utils/cashierShiftHelpers.js'

const { Text, Title, Paragraph } = Typography

export default function CashierShiftDetailModal({ open, shiftId, onClose }) {
  const [loading, setLoading] = useState(false)
  const [shift, setShift] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (open && shiftId) {
      fetchDetail(shiftId)
    } else {
      setShift(null)
      setError(null)
    }
  }, [open, shiftId])

  const fetchDetail = async (id) => {
    setLoading(true)
    setError(null)
    try {
      const res = await cashierShiftApi.getById(id)
      setShift(res.data)
    } catch (err) {
      setError(mapCashierShiftError(err))
    } finally {
      setLoading(false)
    }
  }

  const renderDifferenceBadge = (diff) => {
    const val = Number(diff) || 0
    if (val === 0) {
      return (
        <Tag color="green" icon={<CheckCircleOutlined />}>
          Khớp 100% (0 ₫)
        </Tag>
      )
    }
    if (val > 0) {
      return (
        <Tag color="orange" icon={<ExclamationCircleOutlined />}>
          Thừa: +{formatCurrency(val)}
        </Tag>
      )
    }
    return (
      <Tag color="red" icon={<ExclamationCircleOutlined />}>
        Thiếu: {formatCurrency(val)}
      </Tag>
    )
  }

  const statusMeta = shift ? getShiftStatusMeta(shift.status) : null

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
      title={
        <Space orientation="horizontal" size="middle">
          <Title level={4} style={{ margin: 0 }}>
            Chi tiết phiếu chốt ca {shift?.shiftCode ? `(${shift.shiftCode})` : ''}
          </Title>
          {statusMeta && <Tag color={statusMeta.tagColor}>{statusMeta.label}</Tag>}
        </Space>
      }
      width={780}
      destroyOnClose
    >
      {loading && (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin size="large" tip="Đang tải dữ liệu phiếu chốt ca..." />
        </div>
      )}

      {!loading && error && (
        <Alert
          type="error"
          message="Không thể tải chi tiết phiếu chốt ca"
          description={error}
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {!loading && shift && (
        <div>
          {/* Thông tin chung */}
          <Descriptions
            bordered
            size="small"
            column={{ xs: 1, sm: 2 }}
            style={{ marginBottom: 16 }}
          >
            <Descriptions.Item label="Mã phiếu">
              <Text strong copyable>{shift.shiftCode}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Thu ngân / Lễ tân">
              <Space>
                <UserOutlined />
                <Text strong>{shift.cashierName || '—'}</Text>
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Thời gian ca làm việc">
              <ClockCircleOutlined style={{ marginRight: 6 }} />
              {formatDateTime(shift.startTime)} → {formatDateTime(shift.endTime)}
            </Descriptions.Item>
            <Descriptions.Item label="Thời điểm lập phiếu">
              {formatDateTime(shift.createdAt)}
            </Descriptions.Item>
            <Descriptions.Item label="Tổng số giao dịch">
              <Text strong>{shift.totalTransactions}</Text> giao dịch
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color={statusMeta?.tagColor}>{statusMeta?.label}</Tag>
            </Descriptions.Item>
          </Descriptions>

          <Divider orientation="left" style={{ margin: '12px 0' }}>
            <DollarOutlined /> Đối chiếu doanh thu và tiền mặt
          </Divider>

          <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
            <Col xs={24} sm={8}>
              <Card size="small" bordered style={{ background: '#fafafa' }}>
                <Statistic
                  title="Tiền mặt hệ thống"
                  value={shift.systemCashAmount}
                  formatter={(val) => formatCurrency(val)}
                  valueStyle={{ fontSize: 18 }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card size="small" bordered style={{ background: '#f6ffed' }}>
                <Statistic
                  title="Tiền mặt thực tế"
                  value={shift.actualCashAmount}
                  formatter={(val) => formatCurrency(val)}
                  valueStyle={{ color: '#389e0d', fontSize: 18, fontWeight: 'bold' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card size="small" bordered>
                <div style={{ marginBottom: 4 }}>
                  <Text type="secondary" style={{ fontSize: 12 }}>Chênh lệch đối chiếu</Text>
                </div>
                <div>{renderDifferenceBadge(shift.differenceAmount)}</div>
              </Card>
            </Col>
          </Row>

          {/* Phân bổ theo phương thức */}
          <Descriptions
            bordered
            size="small"
            column={{ xs: 1, sm: 2 }}
            style={{ marginBottom: 16 }}
            title="Cơ cấu thanh toán hệ thống"
          >
            <Descriptions.Item label="Tiền mặt">
              <Text>{formatCurrency(shift.systemCashAmount)}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Chuyển khoản ngân hàng">
              <Text>{formatCurrency(shift.systemTransferAmount)}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Quẹt thẻ POS">
              <Text>{formatCurrency(shift.systemCardAmount)}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Phương thức khác">
              <Text>{formatCurrency(shift.systemOtherAmount)}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Tổng doanh thu hệ thống" span={2}>
              <Text strong style={{ fontSize: 16, color: '#1677ff' }}>
                {formatCurrency(shift.totalSystemAmount)}
              </Text>
            </Descriptions.Item>
          </Descriptions>

          {/* Khối Giải trình & Phê duyệt */}
          <Descriptions
            bordered
            size="small"
            column={1}
            title="Thông tin giải trình & phê duyệt"
          >
            <Descriptions.Item label="Ghi chú / Giải trình của thu ngân">
              {shift.notes ? (
                <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                  {shift.notes}
                </Paragraph>
              ) : (
                <Text type="secondary">Không có ghi chú</Text>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Quản lý xác nhận">
              {shift.confirmedByName ? (
                <Space>
                  <UserOutlined />
                  <Text strong>{shift.confirmedByName}</Text>
                  <Text type="secondary">vào lúc {formatDateTime(shift.confirmedAt)}</Text>
                </Space>
              ) : (
                <Text type="secondary">Chưa có người xác nhận</Text>
              )}
            </Descriptions.Item>
            {shift.confirmationNotes && (
              <Descriptions.Item label="Ý kiến của Quản lý">
                <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                  {shift.confirmationNotes}
                </Paragraph>
              </Descriptions.Item>
            )}
          </Descriptions>
        </div>
      )}
    </Modal>
  )
}
