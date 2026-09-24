import React, { useState, useEffect, useMemo } from 'react'
import {
  Modal,
  Button,
  Input,
  InputNumber,
  Row,
  Col,
  Card,
  Space,
  Typography,
  Alert,
  Tag,
  Divider,
  Descriptions,
  message,
} from 'antd'
import {
  PercentageOutlined,
  DollarCircleOutlined,
  GiftOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  SendOutlined,
} from '@ant-design/icons'
import discountRequestApi from '../../api/discountRequestApi.js'
import {
  DISCOUNT_TYPES,
  DISCOUNT_TYPE_CARDS,
  validateDiscountForm,
  calculatePreview,
  mapDiscountErrorMessage,
} from '../../utils/discountRequestHelpers.js'
import { formatCurrency } from '../../utils/paymentMethodHelpers.js'
import { formatVisitCode } from '../../utils/helpers.js'

const { Text, Title, Paragraph } = Typography

const PRESET_REASONS = [
  'Bệnh nhân có hoàn cảnh khó khăn, hộ nghèo',
  'Chương trình ưu đãi khách hàng thân thiết / hội viên',
  'Người có công với cách mạng / gia đình chính sách',
  'Miễn giảm theo quyết định của Ban Giám đốc',
  'Sai sót kỹ thuật trong quá trình chỉ định cần bồi hoàn',
]

export default function CreateDiscountRequestModal({
  open,
  onClose,
  onSuccess,
  visitData,
}) {
  const [discountType, setDiscountType] = useState(DISCOUNT_TYPES.PERCENTAGE)
  const [discountValue, setDiscountValue] = useState(10)
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [apiError, setApiError] = useState(null)

  const originalAmount = useMemo(() => {
    if (!visitData) return 0
    return Number(visitData.totalAmount) || 0
  }, [visitData])

  useEffect(() => {
    if (open) {
      setDiscountType(DISCOUNT_TYPES.PERCENTAGE)
      setDiscountValue(10)
      setReason('')
      setSubmitting(false)
      setApiError(null)
    }
  }, [open, visitData])

  // Khi chuyển loại giảm giá, điều chỉnh giá trị mặc định hợp lý
  const handleSelectType = (type) => {
    setDiscountType(type)
    setApiError(null)
    if (type === DISCOUNT_TYPES.PERCENTAGE) {
      setDiscountValue(10)
    } else if (type === DISCOUNT_TYPES.FIXED_AMOUNT) {
      setDiscountValue(Math.min(50000, originalAmount))
    } else {
      setDiscountValue(0)
    }
  }

  // Tính preview tạm thời trên giao diện (chỉ dùng hiển thị tham khảo)
  const preview = useMemo(() => {
    return calculatePreview(discountType, discountValue, originalAmount)
  }, [discountType, discountValue, originalAmount])

  const validation = useMemo(() => {
    return validateDiscountForm(discountType, discountValue, reason, originalAmount)
  }, [discountType, discountValue, reason, originalAmount])

  const handleSubmit = () => {
    if (!validation.isValid) {
      message.warning(validation.message)
      return
    }

    const isFullFree = discountType === DISCOUNT_TYPES.FULL_FREE

    Modal.confirm({
      title: 'Xác nhận gửi đề nghị giảm giá',
      icon: <ExclamationCircleOutlined style={{ color: isFullFree ? '#9333ea' : '#0284c7' }} />,
      content: isFullFree ? (
        <div>
          <Paragraph strong style={{ color: '#9333ea', marginBottom: 6 }}>
            Đề nghị MIỄN PHÍ TOÀN BỘ (100%) VIỆN PHÍ:
          </Paragraph>
          <Paragraph>
            Toàn bộ <strong>{formatCurrency(originalAmount)}</strong> sẽ được miễn phí hoàn toàn.
            Yêu cầu này bắt buộc cần Quản lý phê duyệt trước khi có thể lập hóa đơn hoặc thu tiền.
          </Paragraph>
          <Text type="secondary">Xác nhận gửi đề nghị này?</Text>
        </div>
      ) : (
        <div>
          <Paragraph>
            Bạn có chắc chắn muốn gửi đề nghị giảm giá cho lượt khám{' '}
            <Tag color="geekblue" style={{ fontWeight: 600 }}>
              {formatVisitCode(visitData?.visitCode, visitData?.visitId)}
            </Tag>
            ?
          </Paragraph>
          <Descriptions size="small" column={1} bordered style={{ marginTop: 8 }}>
            <Descriptions.Item label="Khoản giảm dự kiến">
              <strong style={{ color: '#16a34a' }}>-{formatCurrency(preview.discountAmount)}</strong>
            </Descriptions.Item>
            <Descriptions.Item label="Số tiền còn thu">
              <strong style={{ color: '#0284c7' }}>{formatCurrency(preview.finalAmount)}</strong>
            </Descriptions.Item>
          </Descriptions>
        </div>
      ),
      okText: 'Xác nhận gửi',
      cancelText: 'Hủy bỏ',
      okButtonProps: {
        style: {
          background: isFullFree ? '#9333ea' : '#0284c7',
          borderColor: isFullFree ? '#9333ea' : '#0284c7',
        },
      },
      onOk: async () => {
        setSubmitting(true)
        setApiError(null)

        const payload = {
          visitId: visitData?.visitId,
          discountType,
          discountValue: isFullFree ? 0 : Number(discountValue),
          originalAmount: Number(originalAmount),
          reason: reason.trim(),
        }

        try {
          const res = await discountRequestApi.create(payload)
          message.success('Đã gửi đề nghị giảm giá, đang chờ Quản lý phê duyệt.')
          if (typeof onSuccess === 'function') {
            onSuccess(res.data)
          }
          if (typeof onClose === 'function') {
            onClose()
          }
        } catch (err) {
          const friendlyMessage = mapDiscountErrorMessage(err)
          setApiError(friendlyMessage)
          message.error(friendlyMessage)
        } finally {
          setSubmitting(false)
        }
      },
    })
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={
        <Space align="center" size={10}>
          <PercentageOutlined style={{ color: '#0284c7', fontSize: 20 }} />
          <div>
            <div style={{ fontSize: 17, fontWeight: 700, color: '#0f172a' }}>
              Đề nghị giảm giá &amp; miễn phí viện phí
            </div>
            <div style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
              Quy trình kiểm soát giảm trừ viện phí có phê duyệt (QTN-37 / NCL-07-CN-008)
            </div>
          </div>
        </Space>
      }
      footer={[
        <Button key="cancel" onClick={onClose} disabled={submitting}>
          Đóng
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<SendOutlined />}
          loading={submitting}
          disabled={submitting || !validation.isValid || originalAmount <= 0}
          onClick={handleSubmit}
          style={{
            background: '#0284c7',
            borderColor: '#0284c7',
            fontWeight: 600,
          }}
        >
          Gửi đề nghị
        </Button>,
      ]}
      width={720}
      destroyOnClose
      maskClosable={!submitting}
    >
      <div style={{ marginTop: 12 }}>
        {apiError && (
          <Alert
            type="error"
            showIcon
            message="Không thể gửi đề nghị"
            description={apiError}
            style={{ marginBottom: 16 }}
            closable
            onClose={() => setApiError(null)}
          />
        )}

        {/* Thông tin lượt khám & viện phí gốc */}
        <Card
          size="small"
          style={{
            background: '#f8fafc',
            borderColor: '#e2e8f0',
            marginBottom: 16,
            borderRadius: 8,
          }}
        >
          <Row gutter={[16, 8]} align="middle">
            <Col xs={24} sm={14}>
              <Space orientation="vertical" size={2}>
                <Text type="secondary" style={{ fontSize: 12 }}>LƯỢT KHÁM BỆNH NHÂN</Text>
                <div>
                  <Text strong style={{ fontSize: 15, color: '#1e293b' }}>
                    {visitData?.patientName || 'Chưa xác định'}
                  </Text>{' '}
                  <Text type="secondary">({visitData?.patientCode || '—'})</Text>
                </div>
                <div style={{ fontSize: 12, color: '#475569' }}>
                  Mã lượt:{' '}
                  <Tag color="geekblue" style={{ fontWeight: 600 }}>
                    {formatVisitCode(visitData?.visitCode, visitData?.visitId)}
                  </Tag>
                  {visitData?.doctorName && <span> • BS: {visitData.doctorName}</span>}
                </div>
              </Space>
            </Col>

            <Col xs={24} sm={10} style={{ textAlign: 'right' }}>
              <Text type="secondary" style={{ fontSize: 12 }}>TỔNG VIỆN PHÍ GỐC</Text>
              <div style={{ fontSize: 20, fontWeight: 700, color: '#0284c7' }}>
                {formatCurrency(originalAmount)}
              </div>
            </Col>
          </Row>
        </Card>

        {/* Lựa chọn 3 thẻ lớn (Radio Cards) */}
        <div style={{ marginBottom: 8 }}>
          <Text strong style={{ fontSize: 14, color: '#0f172a' }}>
            1. Chọn hình thức giảm giá / miễn phí:
          </Text>
        </div>

        <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
          {DISCOUNT_TYPE_CARDS.map((card) => {
            const isSelected = discountType === card.key
            return (
              <Col xs={24} sm={8} key={card.key}>
                <div
                  onClick={() => handleSelectType(card.key)}
                  style={{
                    border: isSelected ? `2px solid ${card.color}` : '1px solid #cbd5e1',
                    borderRadius: 8,
                    padding: '12px 14px',
                    cursor: 'pointer',
                    background: isSelected ? '#f0f9ff' : '#ffffff',
                    transition: 'all 0.2s ease',
                    boxShadow: isSelected ? '0 4px 12px rgba(2, 132, 199, 0.15)' : 'none',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                      <Text strong style={{ color: isSelected ? card.color : '#1e293b', fontSize: 14 }}>
                        {card.title}
                      </Text>
                      <Tag color={isSelected ? card.color : 'default'} style={{ margin: 0, fontWeight: 700 }}>
                        {card.badge}
                      </Tag>
                    </div>
                    <Text type="secondary" style={{ fontSize: 12, lineHeight: 1.4 }}>
                      {card.description}
                    </Text>
                  </div>
                  {isSelected && (
                    <div style={{ marginTop: 8, textAlign: 'right' }}>
                      <CheckCircleOutlined style={{ color: card.color, fontSize: 16 }} />
                    </div>
                  )}
                </div>
              </Col>
            )
          })}
        </Row>

        {/* Nhập giá trị theo loại đã chọn */}
        <div style={{ marginBottom: 16 }}>
          <Text strong style={{ fontSize: 14, color: '#0f172a' }}>
            2. Giá trị đề xuất &amp; Xem trước (Preview):
          </Text>

          <div style={{ marginTop: 8, background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: 8, padding: 16 }}>
            {discountType === DISCOUNT_TYPES.PERCENTAGE && (
              <Row gutter={[16, 16]} align="middle">
                <Col xs={24} sm={10}>
                  <Text style={{ display: 'block', marginBottom: 4 }}>Tỷ lệ giảm giá (%):</Text>
                  <InputNumber
                    min={1}
                    max={100}
                    value={discountValue}
                    onChange={(val) => setDiscountValue(val)}
                    addonAfter="%"
                    style={{ width: '100%' }}
                    placeholder="Ví dụ: 15"
                  />
                  <Text type="secondary" style={{ fontSize: 11 }}>
                    Nhập tỷ lệ từ 1% đến 100%
                  </Text>
                </Col>

                <Col xs={24} sm={14}>
                  <div style={{ background: '#f8fafc', padding: 12, borderRadius: 6, border: '1px solid #e2e8f0' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <Text type="secondary">Số tiền được giảm:</Text>
                      <strong style={{ color: '#16a34a' }}>-{formatCurrency(preview.discountAmount)}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Text type="secondary">Số tiền còn phải thu:</Text>
                      <strong style={{ color: '#0284c7', fontSize: 15 }}>{formatCurrency(preview.finalAmount)}</strong>
                    </div>
                  </div>
                </Col>
              </Row>
            )}

            {discountType === DISCOUNT_TYPES.FIXED_AMOUNT && (
              <Row gutter={[16, 16]} align="middle">
                <Col xs={24} sm={12}>
                  <Text style={{ display: 'block', marginBottom: 4 }}>Số tiền giảm giá (VNĐ):</Text>
                  <InputNumber
                    min={1000}
                    max={originalAmount}
                    step={10000}
                    value={discountValue}
                    onChange={(val) => setDiscountValue(val)}
                    formatter={(val) => `${val}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                    parser={(val) => val.replace(/\./g, '')}
                    addonAfter="₫"
                    style={{ width: '100%' }}
                    placeholder="Ví dụ: 50.000"
                  />
                  <Text type="secondary" style={{ fontSize: 11 }}>
                    Tối đa bằng tổng viện phí: {formatCurrency(originalAmount)}
                  </Text>
                </Col>

                <Col xs={24} sm={12}>
                  <div style={{ background: '#f8fafc', padding: 12, borderRadius: 6, border: '1px solid #e2e8f0' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <Text type="secondary">Số tiền được giảm:</Text>
                      <strong style={{ color: '#16a34a' }}>-{formatCurrency(preview.discountAmount)}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Text type="secondary">Số tiền còn phải thu:</Text>
                      <strong style={{ color: '#0284c7', fontSize: 15 }}>{formatCurrency(preview.finalAmount)}</strong>
                    </div>
                  </div>
                </Col>
              </Row>
            )}

            {discountType === DISCOUNT_TYPES.FULL_FREE && (
              <Alert
                type="info"
                icon={<GiftOutlined style={{ fontSize: 20, color: '#9333ea' }} />}
                showIcon
                message={
                  <strong style={{ color: '#9333ea' }}>
                    Miễn phí hoàn toàn 100% viện phí
                  </strong>
                }
                description={
                  <div style={{ marginTop: 4 }}>
                    Toàn bộ <strong style={{ color: '#0f172a' }}>{formatCurrency(originalAmount)}</strong> viện phí
                    của lượt khám sẽ được miễn phí 100%. Số tiền bệnh nhân cần nộp là{' '}
                    <strong style={{ color: '#16a34a' }}>0 ₫</strong>.
                  </div>
                }
              />
            )}
          </div>
        </div>

        {/* Lý do đề nghị */}
        <div style={{ marginBottom: 8 }}>
          <Space>
            <Text strong style={{ fontSize: 14, color: '#0f172a' }}>
              3. Lý do đề nghị giảm giá (Bắt buộc):
            </Text>
            <Text type="danger">*</Text>
          </Space>
        </div>

        <Input.TextArea
          rows={3}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Nhập lý do chi tiết để Quản lý có căn cứ phê duyệt..."
          maxLength={500}
          showCount
          style={{ borderRadius: 6, marginBottom: 8 }}
        />

        {/* Presets gợi ý nhanh */}
        <div style={{ marginTop: 4 }}>
          <Text orientation="left" type="secondary" style={{ fontSize: 12, marginRight: 6 }}>
            Gợi ý nhanh:
          </Text>
          <Space wrap size={[6, 6]}>
            {PRESET_REASONS.map((preset, idx) => (
              <Tag
                key={idx}
                style={{ cursor: 'pointer', borderRadius: 12, fontSize: 11 }}
                onClick={() => setReason(preset)}
              >
                + {preset}
              </Tag>
            ))}
          </Space>
        </div>

        <Divider style={{ margin: '14px 0 10px 0' }} />

        <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#64748b', fontSize: 12 }}>
          <InfoCircleOutlined />
          <span>
            Sau khi gửi đề nghị, lượt khám sẽ ở trạng thái chờ duyệt. Quản lý phòng khám sẽ xem xét và phê duyệt.
          </span>
        </div>
      </div>
    </Modal>
  )
}
