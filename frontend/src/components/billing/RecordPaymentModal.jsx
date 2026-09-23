import React, { useState, useEffect, useMemo } from 'react'
import {
  Modal,
  Button,
  Select,
  InputNumber,
  Input,
  Space,
  Row,
  Col,
  Card,
  Divider,
  Typography,
  Alert,
  Tag,
  Tooltip,
  Descriptions,
} from 'antd'
import {
  DollarCircleOutlined,
  PlusOutlined,
  DeleteOutlined,
  BankOutlined,
  CreditCardOutlined,
  QrcodeOutlined,
  WalletOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  CalculatorOutlined,
  SyncOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import invoiceApi from '../../api/invoiceApi.js'
import {
  PAYMENT_METHOD_OPTIONS,
  formatCurrency,
  validatePaymentMethods,
  calculateEqualSplit,
  mapPaymentErrorMessage,
} from '../../utils/paymentMethodHelpers.js'

const { Text } = Typography

export default function RecordPaymentModal({
  open,
  onClose,
  onSuccess,
  visitData,
  canCollectPayment = true,
}) {
  const [paymentRows, setPaymentRows] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [apiError, setApiError] = useState(null)

  const amountDue = useMemo(() => {
    if (!visitData) return 0
    const total = Number(visitData.totalAmount) || 0
    const discount = Number(visitData.discountAmount) || 0
    const due = total - discount
    return due > 0 ? due : 0
  }, [visitData])

  useEffect(() => {
    if (open && visitData) {
      setApiError(null)
      setSubmitting(false)
      setPaymentRows([
        {
          id: `row-${Date.now()}-0`,
          paymentMethod: 'CASH',
          amount: amountDue,
          referenceNumber: '',
        },
      ])
    } else {
      setPaymentRows([])
      setApiError(null)
    }
  }, [open, visitData, amountDue])

  const validation = useMemo(() => {
    return validatePaymentMethods(paymentRows, amountDue)
  }, [paymentRows, amountDue])

  const handleMethodChange = (index, newMethod) => {
    setApiError(null)
    setPaymentRows((prev) => {
      const next = [...prev]
      next[index] = {
        ...next[index],
        paymentMethod: newMethod,
        referenceNumber: newMethod === 'BANK_TRANSFER' ? next[index].referenceNumber : '',
      }
      return next
    })
  }

  const handleAmountChange = (index, val) => {
    setApiError(null)
    setPaymentRows((prev) => {
      const next = [...prev]
      next[index] = {
        ...next[index],
        amount: val === null || val === undefined ? '' : Number(val),
      }
      return next
    })
  }

  const handleReferenceChange = (index, text) => {
    setApiError(null)
    setPaymentRows((prev) => {
      const next = [...prev]
      next[index] = {
        ...next[index],
        referenceNumber: text,
      }
      return next
    })
  }

  const handleAddRow = () => {
    setApiError(null)
    const currentTotal = paymentRows.reduce((sum, r) => sum + (Number(r.amount) || 0), 0)
    const remaining = Math.max(0, amountDue - currentTotal)

    setPaymentRows((prev) => [
      ...prev,
      {
        id: `row-${Date.now()}-${prev.length}`,
        paymentMethod: 'BANK_TRANSFER',
        amount: remaining,
        referenceNumber: '',
      },
    ])
  }

  const handleDeleteRow = (index) => {
    if (paymentRows.length <= 1) return
    setApiError(null)
    setPaymentRows((prev) => prev.filter((_, idx) => idx !== index))
  }

  const handleAutoFillSecondRow = () => {
    if (paymentRows.length !== 2) return
    setApiError(null)
    const firstAmount = Number(paymentRows[0].amount) || 0
    const remaining = Math.max(0, amountDue - firstAmount)
    setPaymentRows((prev) => [
      prev[0],
      {
        ...prev[1],
        amount: remaining,
      },
    ])
  }

  const handleSplitEqually = () => {
    if (paymentRows.length <= 1) return
    setApiError(null)
    const splitAmounts = calculateEqualSplit(amountDue, paymentRows.length)
    setPaymentRows((prev) =>
      prev.map((row, idx) => ({
        ...row,
        amount: splitAmounts[idx] || 0,
      }))
    )
  }

  const handleSubmit = async () => {
    if (!visitData || !visitData.visitId) return
    if (!canCollectPayment) {
      setApiError('Bạn không có quyền thực hiện thu phí (yêu cầu quyền INVOICE_CREATE).')
      return
    }

    const finalValidation = validatePaymentMethods(paymentRows, amountDue)
    if (!finalValidation.isValid) {
      setApiError(finalValidation.errors[0] || 'Vui lòng kiểm tra lại thông tin thanh toán.')
      return
    }

    setSubmitting(true)
    setApiError(null)

    try {
      const cleanMethods = paymentRows.map((r) => ({
        paymentMethod: r.paymentMethod,
        amount: Number(r.amount),
        referenceNumber: r.paymentMethod === 'BANK_TRANSFER' ? r.referenceNumber?.trim() : null,
      }))

      const payload = {
        visitId: visitData.visitId,
        examFee: Number(visitData.examFee) || 0,
        medicineFee: Number(visitData.medicineFee) || 0,
        amountPaid: amountDue,
        paymentMethods: cleanMethods,
      }

      const res = await invoiceApi.recordPayment(payload)
      const paymentResult = res?.data

      if (!paymentResult || !paymentResult.id) {
        throw new Error('Backend không trả về thông tin thanh toán hợp lệ.')
      }

      onSuccess(paymentResult)
      onClose()
    } catch (err) {
      const errorMsg = mapPaymentErrorMessage(err)
      setApiError(errorMsg)
    } finally {
      setSubmitting(false)
    }
  }

  const getMethodIcon = (method) => {
    switch (method) {
      case 'CASH':
        return <DollarCircleOutlined style={{ color: '#16a34a' }} />
      case 'BANK_TRANSFER':
        return <BankOutlined style={{ color: '#7c3aed' }} />
      case 'CARD':
        return <CreditCardOutlined style={{ color: '#2563eb' }} />
      case 'QR_CODE':
        return <QrcodeOutlined style={{ color: '#0891b2' }} />
      case 'E_WALLET':
        return <WalletOutlined style={{ color: '#ea580c' }} />
      default:
        return <DollarCircleOutlined />
    }
  }

  return (
    <Modal
      open={open}
      onCancel={submitting ? undefined : onClose}
      width={780}
      title={
        <Space>
          <DollarCircleOutlined style={{ color: '#0284c7', fontSize: 20 }} />
          <span style={{ fontSize: 17, fontWeight: 700, color: '#0f172a' }}>
            Thu phí &amp; Ghi nhận phương thức thanh toán
          </span>
        </Space>
      }
      maskClosable={!submitting}
      destroyOnClose
      footer={[
        <Button key="cancel" onClick={onClose} disabled={submitting}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<DollarCircleOutlined />}
          loading={submitting}
          disabled={!validation.isValid || submitting || !canCollectPayment}
          onClick={handleSubmit}
          style={{
            minWidth: 160,
            background: validation.isValid ? '#16a34a' : undefined,
            borderColor: validation.isValid ? '#16a34a' : undefined,
            fontWeight: 600,
          }}
        >
          {submitting ? 'Đang xử lý...' : 'Xác nhận thu phí'}
        </Button>,
      ]}
    >
      <Space direction="vertical" size="middle" style={{ width: '100%', marginTop: 8 }}>
        {apiError && (
          <Alert
            type="error"
            showIcon
            message="Không thể ghi nhận thanh toán"
            description={apiError}
            closable
            onClose={() => setApiError(null)}
          />
        )}

        <Card
          size="small"
          style={{
            background: '#f8fafc',
            border: '1px solid #cbd5e1',
            borderRadius: 8,
          }}
        >
          <Row gutter={[16, 12]} align="middle">
            <Col xs={24} sm={14}>
              <Descriptions size="small" column={1} bordered={false}>
                <Descriptions.Item label="Lượt khám">
                  <Text strong code style={{ color: '#1e40af' }}>
                    {visitData?.visitCode || visitData?.visitId}
                  </Text>
                </Descriptions.Item>
                <Descriptions.Item label="Bệnh nhân">
                  <Text strong style={{ color: '#0f172a' }}>
                    {visitData?.patientName || '—'}
                  </Text>{' '}
                  <Text type="secondary">({visitData?.patientCode || '—'})</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Chi tiết khoản thu">
                  <Space split={<Divider type="vertical" />} wrap style={{ fontSize: 12 }}>
                    <span>Khám: <strong>{formatCurrency(visitData?.examFee || 0)}</strong></span>
                    <span>Thuốc: <strong>{formatCurrency(visitData?.medicineFee || 0)}</strong></span>
                    {Number(visitData?.serviceFee) > 0 && (
                      <span>DV CLS: <strong>{formatCurrency(visitData?.serviceFee)}</strong></span>
                    )}
                    {Number(visitData?.discountAmount) > 0 && (
                      <span style={{ color: '#16a34a' }}>
                        Miễn giảm: -{formatCurrency(visitData?.discountAmount)}
                      </span>
                    )}
                  </Space>
                </Descriptions.Item>
              </Descriptions>
            </Col>

            <Col xs={24} sm={10} style={{ textAlign: 'right', borderLeft: '1px dashed #cbd5e1', paddingLeft: 16 }}>
              <div style={{ color: '#64748b', fontSize: 13, fontWeight: 500, marginBottom: 2 }}>
                SỐ TIỀN CẦN THU (CỐ ĐỊNH)
              </div>
              <div style={{ fontSize: 24, fontWeight: 800, color: '#0284c7' }}>
                {formatCurrency(amountDue)}
              </div>
              <Text type="secondary" style={{ fontSize: 11, fontStyle: 'italic' }}>
                Tổng các phương thức thanh toán phải khớp chính xác 100% số tiền này
              </Text>
            </Col>
          </Row>
        </Card>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
          <Space>
            <Button
              type="dashed"
              icon={<PlusOutlined />}
              onClick={handleAddRow}
              disabled={submitting}
              style={{ borderColor: '#0284c7', color: '#0284c7' }}
            >
              Thêm phương thức khác
            </Button>

            {paymentRows.length === 2 && (
              <Tooltip title="Tự động tính số tiền dòng 2 bằng [Tổng cần thu - Số tiền dòng 1]">
                <Button
                  size="small"
                  icon={<SyncOutlined />}
                  onClick={handleAutoFillSecondRow}
                  disabled={submitting}
                >
                  Bù dòng 2
                </Button>
              </Tooltip>
            )}

            {paymentRows.length > 1 && (
              <Tooltip title="Chia đều số tiền cần thu cho tất cả các dòng hiện tại">
                <Button
                  size="small"
                  icon={<CalculatorOutlined />}
                  onClick={handleSplitEqually}
                  disabled={submitting}
                >
                  Chia đều
                </Button>
              </Tooltip>
            )}
          </Space>

          <Text type="secondary" style={{ fontSize: 12 }}>
            Số lượng phương thức: <strong>{paymentRows.length}</strong>
          </Text>
        </div>

        <div style={{ maxHeight: '340px', overflowY: 'auto', paddingRight: 4 }}>
          {paymentRows.map((row, index) => {
            const isBankTransfer = row.paymentMethod === 'BANK_TRANSFER'
            const hasAmountError = Number(row.amount) <= 0 || !row.amount
            const hasRefError = isBankTransfer && (!row.referenceNumber || !row.referenceNumber.trim() || row.referenceNumber.length > 100)

            return (
              <Card
                key={row.id}
                size="small"
                style={{
                  marginBottom: 10,
                  border: hasAmountError || hasRefError ? '1px solid #fca5a5' : '1px solid #e2e8f0',
                  borderRadius: 8,
                  background: index % 2 === 0 ? '#ffffff' : '#fcfcfd',
                }}
              >
                <Row gutter={[12, 8]} align="middle">
                  <Col xs={24} sm={7}>
                    <div style={{ fontSize: 12, color: '#64748b', marginBottom: 4 }}>
                      Phương thức {index + 1}
                    </div>
                    <Select
                      value={row.paymentMethod}
                      onChange={(val) => handleMethodChange(index, val)}
                      disabled={submitting}
                      style={{ width: '100%' }}
                      options={PAYMENT_METHOD_OPTIONS.map((opt) => ({
                        value: opt.value,
                        label: (
                          <Space>
                            {getMethodIcon(opt.value)}
                            <span>{opt.label}</span>
                          </Space>
                        ),
                      }))}
                    />
                  </Col>

                  <Col xs={24} sm={isBankTransfer ? 7 : 14}>
                    <div style={{ fontSize: 12, color: '#64748b', marginBottom: 4 }}>
                      Số tiền thu (VNĐ) <span style={{ color: '#ef4444' }}>*</span>
                    </div>
                    <InputNumber
                      value={row.amount}
                      onChange={(val) => handleAmountChange(index, val)}
                      disabled={submitting}
                      min={0}
                      step={10000}
                      formatter={(val) => `${val}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                      parser={(val) => val.replace(/\$\s?|(,*)/g, '')}
                      status={hasAmountError ? 'error' : ''}
                      placeholder="Nhập số tiền..."
                      style={{ width: '100%' }}
                    />
                  </Col>

                  {isBankTransfer && (
                    <Col xs={24} sm={7}>
                      <div style={{ fontSize: 12, color: '#64748b', marginBottom: 4 }}>
                        Số tham chiếu / Mã GD <span style={{ color: '#ef4444' }}>*</span>
                      </div>
                      <Input
                        value={row.referenceNumber}
                        onChange={(e) => handleReferenceChange(index, e.target.value)}
                        disabled={submitting}
                        maxLength={100}
                        status={hasRefError ? 'error' : ''}
                        placeholder="Mã GD ngân hàng (bắt buộc)"
                        style={{ width: '100%' }}
                      />
                    </Col>
                  )}

                  <Col xs={24} sm={isBankTransfer ? 3 : 3} style={{ textAlign: 'right' }}>
                    <div style={{ height: 18 }}></div>
                    {paymentRows.length > 1 ? (
                      <Tooltip title="Xóa phương thức này">
                        <Button
                          type="text"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => handleDeleteRow(index)}
                          disabled={submitting}
                        />
                      </Tooltip>
                    ) : (
                      <span style={{ fontSize: 12, color: '#94a3b8' }}>Mặc định</span>
                    )}
                  </Col>
                </Row>
              </Card>
            )
          })}
        </div>

        <Card
          size="small"
          style={{
            borderRadius: 8,
            background: validation.isExact
              ? '#f6ffed'
              : validation.difference !== 0
              ? '#fff2f0'
              : '#f8fafc',
            border: validation.isExact
              ? '1px solid #b7eb8f'
              : validation.difference !== 0
              ? '1px solid #ffccc7'
              : '1px solid #cbd5e1',
          }}
        >
          <Row gutter={[16, 8]} align="middle">
            <Col xs={12} sm={8}>
              <div style={{ fontSize: 12, color: '#64748b' }}>Tổng tiền đã nhập:</div>
              <div style={{ fontSize: 18, fontWeight: 700, color: '#0f172a' }}>
                {formatCurrency(validation.totalEntered)}
              </div>
            </Col>

            <Col xs={12} sm={8}>
              <div style={{ fontSize: 12, color: '#64748b' }}>Trạng thái đối soát:</div>
              <div>
                {validation.isExact ? (
                  <Tag color="green" icon={<CheckCircleOutlined />} style={{ fontSize: 13, padding: '2px 8px' }}>
                    Khớp chính xác (0 ₫)
                  </Tag>
                ) : validation.isUnder ? (
                  <Tag color="red" icon={<ExclamationCircleOutlined />} style={{ fontSize: 13, padding: '2px 8px' }}>
                    Còn thiếu: {formatCurrency(validation.difference)}
                  </Tag>
                ) : (
                  <Tag color="volcano" icon={<ExclamationCircleOutlined />} style={{ fontSize: 13, padding: '2px 8px' }}>
                    Vượt quá: {formatCurrency(Math.abs(validation.difference))}
                  </Tag>
                )}
              </div>
            </Col>

            <Col xs={24} sm={8} style={{ textAlign: 'right' }}>
              {validation.isExact ? (
                <Text type="success" style={{ fontSize: 12, fontWeight: 600 }}>
                  ✓ Đã sẵn sàng ghi nhận thu phí
                </Text>
              ) : (
                <Text type="danger" style={{ fontSize: 12 }}>
                  {validation.isUnder
                    ? `Cần nhập thêm ${formatCurrency(validation.difference)} để khớp hóa đơn`
                    : `Cần giảm bớt ${formatCurrency(Math.abs(validation.difference))} để khớp hóa đơn`}
                </Text>
              )}
            </Col>
          </Row>
        </Card>

        <Alert
          type="info"
          showIcon
          icon={<InfoCircleOutlined />}
          style={{ fontSize: 12 }}
          message={
            <span>
              Khi thu nhiều phương thức, hệ thống sẽ tự động gán phân loại là <code>MULTIPLE</code> và phân rã các khoản thu tiền mặt/chuyển khoản để đối soát quỹ cuối ngày khi Chốt ca.
            </span>
          }
        />
      </Space>
    </Modal>
  )
}
