import React, { useEffect, useState, useMemo } from 'react'
import {
  Card,
  Row,
  Col,
  Statistic,
  Form,
  InputNumber,
  Input,
  Button,
  Popconfirm,
  Alert,
  Spin,
  Typography,
  Table,
  Space,
  Tag,
  Divider,
  Modal,
  Result,
} from 'antd'
import {
  DollarCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ClockCircleOutlined,
  FileDoneOutlined,
  HistoryOutlined,
  ReloadOutlined,
  CreditCardOutlined,
  BankOutlined,
  WalletOutlined,
  AppstoreOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import cashierShiftApi from '../api/cashierShiftApi.js'
import {
  formatCurrency,
  formatDateTime,
  calculatePreviewDifference,
  isNoteRequired,
  mapCashierShiftError,
  getShiftStatusMeta,
} from '../utils/cashierShiftHelpers.js'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input

export default function CashierShiftClosingPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm()

  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [summary, setSummary] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [submitError, setSubmitError] = useState(null)

  // Giá trị tiền mặt thực đếm người dùng đang nhập để tính preview chênh lệch
  const [actualCashInput, setActualCashInput] = useState(null)

  // Kết quả sau khi chốt thành công
  const [closedResult, setClosedResult] = useState(null)

  useEffect(() => {
    fetchCurrentSummary()
  }, [])

  const fetchCurrentSummary = async () => {
    setLoading(true)
    setLoadError(null)
    setSubmitError(null)
    try {
      const res = await cashierShiftApi.getCurrentSummary()
      setSummary(res.data)
      // Mặc định khởi tạo giá trị tiền mặt thực tế bằng tiền mặt hệ thống để tiện đối chiếu
      if (res.data?.systemCashAmount !== undefined) {
        const sysCash = Number(res.data.systemCashAmount) || 0
        setActualCashInput(sysCash)
        form.setFieldsValue({ actualCashAmount: sysCash, notes: '' })
      }
    } catch (err) {
      setLoadError(mapCashierShiftError(err))
    } finally {
      setLoading(false)
    }
  }

  // Tính chênh lệch preview ở Frontend (chỉ để hiển thị UI)
  const previewDifference = useMemo(() => {
    if (!summary || actualCashInput === null || actualCashInput === undefined) return 0
    return calculatePreviewDifference(actualCashInput, summary.systemCashAmount)
  }, [summary, actualCashInput])

  // Xác định xem ghi chú giải trình có bắt buộc hay không
  const noteRequired = useMemo(() => {
    return isNoteRequired(previewDifference)
  }, [previewDifference])

  const handleActualCashChange = (val) => {
    setActualCashInput(val)
    // Nếu chuyển sang trạng thái lệch tiền, re-validate ô ghi chú
    form.validateFields(['notes']).catch(() => {})
  }

  const handleCloseShift = async (values) => {
    setSubmitting(true)
    setSubmitError(null)

    const payload = {
      actualCashAmount: Number(values.actualCashAmount) || 0,
      notes: values.notes?.trim() || null,
    }

    try {
      const res = await cashierShiftApi.closeShift(payload)
      setClosedResult(res.data)
    } catch (err) {
      const errorMsg = mapCashierShiftError(err)
      setSubmitError(errorMsg)
      // Nếu lỗi do thiếu ghi chú, set error trực tiếp vào trường form
      if (err.response?.data?.code === 'CASHIER_SHIFT_NOTE_REQUIRED' || errorMsg.includes('ghi chú')) {
        form.setFields([
          {
            name: 'notes',
            errors: ['Bắt buộc phải nhập lý do/giải trình khi tiền thực tế có chênh lệch.'],
          },
        ])
      }
    } finally {
      setSubmitting(false)
    }
  }

  const hasNoTransactions = !summary || summary.totalTransactions === 0

  // Bảng phân bổ doanh thu theo phương thức
  const breakdownColumns = [
    {
      title: 'Phương thức thanh toán',
      dataIndex: 'method',
      key: 'method',
      render: (text, record) => (
        <Space>
          {record.icon}
          <Text strong>{text}</Text>
        </Space>
      ),
    },
    {
      title: 'Số tiền hệ thống ghi nhận',
      dataIndex: 'amount',
      key: 'amount',
      align: 'right',
      render: (val) => <Text strong>{formatCurrency(val)}</Text>,
    },
    {
      title: 'Tỷ trọng',
      dataIndex: 'percentage',
      key: 'percentage',
      align: 'right',
      render: (val) => <Tag color="blue">{val}%</Tag>,
    },
  ]

  const breakdownData = useMemo(() => {
    if (!summary) return []
    const total = Number(summary.totalSystemAmount) || 0
    const calcPct = (amount) => {
      const num = Number(amount) || 0
      if (total === 0) return '0.0'
      return ((num / total) * 100).toFixed(1)
    }

    return [
      {
        key: 'cash',
        method: 'Tiền mặt',
        amount: summary.systemCashAmount,
        percentage: calcPct(summary.systemCashAmount),
        icon: <WalletOutlined style={{ color: '#52c41a' }} />,
      },
      {
        key: 'transfer',
        method: 'Chuyển khoản ngân hàng',
        amount: summary.systemTransferAmount,
        percentage: calcPct(summary.systemTransferAmount),
        icon: <BankOutlined style={{ color: '#1677ff' }} />,
      },
      {
        key: 'card',
        method: 'Quẹt thẻ POS',
        amount: summary.systemCardAmount,
        percentage: calcPct(summary.systemCardAmount),
        icon: <CreditCardOutlined style={{ color: '#722ed1' }} />,
      },
      {
        key: 'other',
        method: 'Phương thức khác',
        amount: summary.systemOtherAmount,
        percentage: calcPct(summary.systemOtherAmount),
        icon: <AppstoreOutlined style={{ color: '#fa8c16' }} />,
      },
    ]
  }, [summary])

  return (
    <div style={{ padding: '4px 0 24px 0' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <FileDoneOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Chốt ca thu ngân cuối ngày
          </Title>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchCurrentSummary} loading={loading}>
            Làm mới số liệu
          </Button>
          <Button icon={<HistoryOutlined />} onClick={() => navigate('/cashier-shifts/history')}>
            Lịch sử chốt ca
          </Button>
        </Space>
      </div>

      {loading && (
        <Card style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin size="large" tip="Đang tải số liệu ca thu ngân hiện tại..." />
        </Card>
      )}

      {!loading && loadError && (
        <Alert
          type="error"
          message="Không thể tải số liệu ca hiện tại"
          description={loadError}
          showIcon
          action={
            <Button size="small" type="primary" onClick={fetchCurrentSummary}>
              Thử lại
            </Button>
          }
          style={{ marginBottom: 20 }}
        />
      )}

      {!loading && !loadError && summary && (
        <>
          {/* Cảnh báo nếu chưa có giao dịch */}
          {hasNoTransactions && (
            <Alert
              type="warning"
              showIcon
              message="Ca hiện tại chưa có khoản thu nào"
              description="Hệ thống chưa ghi nhận bất kỳ giao dịch thu viện phí nào chưa chốt của bạn. Nút chốt ca sẽ tạm thời bị vô hiệu hóa cho đến khi có ít nhất 1 khoản thu hợp lệ."
              style={{ marginBottom: 20 }}
            />
          )}

          {/* Khối Thông tin ca làm việc & KPI tổng hợp */}
          <Card
            title={
              <Space>
                <ClockCircleOutlined />
                <span>Thông tin ca làm việc hiện tại: <strong>{summary.cashierName || 'Thu ngân'}</strong></span>
              </Space>
            }
            extra={
              <Text type="secondary">
                {summary.startTime ? (
                  <>Bắt đầu từ: <Text strong>{formatDateTime(summary.startTime)}</Text> → Hiện tại: <Text strong>{formatDateTime(summary.endTime)}</Text></>
                ) : (
                  'Chưa phát sinh giao dịch trong ca'
                )}
              </Text>
            }
            style={{ marginBottom: 20 }}
          >
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12} md={8} lg={4}>
                <Card size="small" style={{ background: '#e6f4ff', borderColor: '#91caff' }}>
                  <Statistic
                    title="Tổng doanh thu"
                    value={summary.totalSystemAmount}
                    formatter={(val) => formatCurrency(val)}
                    valueStyle={{ color: '#0958d9', fontWeight: 'bold', fontSize: 18 }}
                    prefix={<DollarCircleOutlined />}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={8} lg={4}>
                <Card size="small" style={{ background: '#f6ffed', borderColor: '#b7eb8f' }}>
                  <Statistic
                    title="Tiền mặt hệ thống"
                    value={summary.systemCashAmount}
                    formatter={(val) => formatCurrency(val)}
                    valueStyle={{ color: '#389e0d', fontWeight: 'bold', fontSize: 18 }}
                    prefix={<WalletOutlined />}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={8} lg={4}>
                <Card size="small">
                  <Statistic
                    title="Chuyển khoản"
                    value={summary.systemTransferAmount}
                    formatter={(val) => formatCurrency(val)}
                    valueStyle={{ fontSize: 16 }}
                    prefix={<BankOutlined />}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={8} lg={4}>
                <Card size="small">
                  <Statistic
                    title="Quẹt thẻ POS"
                    value={summary.systemCardAmount}
                    formatter={(val) => formatCurrency(val)}
                    valueStyle={{ fontSize: 16 }}
                    prefix={<CreditCardOutlined />}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={8} lg={4}>
                <Card size="small">
                  <Statistic
                    title="Phương thức khác"
                    value={summary.systemOtherAmount}
                    formatter={(val) => formatCurrency(val)}
                    valueStyle={{ fontSize: 16 }}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={8} lg={4}>
                <Card size="small" style={{ background: '#fafafa' }}>
                  <Statistic
                    title="Số giao dịch"
                    value={summary.totalTransactions}
                    suffix="giao dịch"
                    valueStyle={{ fontWeight: 'bold', fontSize: 18 }}
                  />
                </Card>
              </Col>
            </Row>
          </Card>

          <Row gutter={[20, 20]}>
            {/* Cột trái: Bảng cơ cấu phương thức thanh toán */}
            <Col xs={24} lg={12}>
              <Card
                title="Cơ cấu thanh toán hệ thống ghi nhận"
                bordered
                style={{ height: '100%' }}
              >
                <Table
                  dataSource={breakdownData}
                  columns={breakdownColumns}
                  pagination={false}
                  size="middle"
                  summary={() => (
                    <Table.Summary.Row style={{ background: '#fafafa', fontWeight: 'bold' }}>
                      <Table.Summary.Cell index={0}>Tổng cộng</Table.Summary.Cell>
                      <Table.Summary.Cell index={1} align="right">
                        <Text strong style={{ color: '#1677ff', fontSize: 15 }}>
                          {formatCurrency(summary.totalSystemAmount)}
                        </Text>
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={2} align="right">
                        <Tag color="blue">100.0%</Tag>
                      </Table.Summary.Cell>
                    </Table.Summary.Row>
                  )}
                />
                {summary.unsettledPaymentIds && summary.unsettledPaymentIds.length > 0 && (
                  <div style={{ marginTop: 16 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      Mã định danh các khoản thu trong ca: {summary.unsettledPaymentIds.length} khoản thu đã sẵn sàng để khóa chốt.
                    </Text>
                  </div>
                )}
              </Card>
            </Col>

            {/* Cột phải: Form đối chiếu & Chốt ca */}
            <Col xs={24} lg={12}>
              <Card
                title={
                  <Space>
                    <WalletOutlined style={{ color: '#52c41a' }} />
                    <span>Đối chiếu tiền mặt thực tế & Tạo phiếu chốt ca</span>
                  </Space>
                }
                bordered
                style={{ height: '100%' }}
              >
                {submitError && (
                  <Alert
                    type="error"
                    showIcon
                    message="Chốt ca không thành công"
                    description={submitError}
                    style={{ marginBottom: 16 }}
                  />
                )}

                <Form
                  form={form}
                  layout="vertical"
                  onFinish={handleCloseShift}
                  disabled={hasNoTransactions || submitting}
                >
                  {/* Ô nhập tiền mặt thực đếm */}
                  <Form.Item
                    name="actualCashAmount"
                    label={
                      <Space>
                        <Text strong>Số tiền mặt thực đếm (VNĐ)</Text>
                        <Tag color="red">Bắt buộc</Tag>
                      </Space>
                    }
                    rules={[
                      { required: true, message: 'Vui lòng nhập số tiền mặt thực đếm.' },
                      {
                        validator: (_, value) => {
                          if (value !== undefined && value !== null && Number(value) < 0) {
                            return Promise.reject(new Error('Số tiền mặt thực tế không được âm.'))
                          }
                          return Promise.resolve()
                        },
                      },
                    ]}
                  >
                    <InputNumber
                      style={{ width: '100%' }}
                      size="large"
                      min={0}
                      step={10000}
                      formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                      parser={(value) => value.replace(/\$\s?|(,*)/g, '')}
                      placeholder="Nhập số tiền mặt thực tế đếm được..."
                      onChange={handleActualCashChange}
                    />
                  </Form.Item>

                  {/* Khối hiển thị Chênh lệch Preview */}
                  <Card
                    size="small"
                    style={{
                      background: previewDifference === 0 ? '#f6ffed' : previewDifference > 0 ? '#fffbe6' : '#fff2f0',
                      borderColor: previewDifference === 0 ? '#b7eb8f' : previewDifference > 0 ? '#ffe58f' : '#ffccc7',
                      marginBottom: 16,
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <Text strong style={{ display: 'block', fontSize: 13 }}>
                          Chênh lệch dự kiến (Tiền thực đếm - Tiền hệ thống):
                        </Text>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {previewDifference === 0
                            ? 'Khớp 100% với số liệu phần mềm, phiếu sẽ được xác nhận ngay.'
                            : previewDifference > 0
                            ? 'Thừa tiền so với hệ thống. Phiếu sẽ chuyển trạng thái chờ Quản lý duyệt.'
                            : 'Thiếu tiền so với hệ thống. Phiếu sẽ chuyển trạng thái chờ Quản lý duyệt.'}
                        </Text>
                      </div>
                      <div>
                        {previewDifference === 0 ? (
                          <Tag color="green" style={{ fontSize: 14, padding: '4px 10px' }} icon={<CheckCircleOutlined />}>
                            0 ₫ (Khớp)
                          </Tag>
                        ) : previewDifference > 0 ? (
                          <Tag color="orange" style={{ fontSize: 14, padding: '4px 10px' }} icon={<ExclamationCircleOutlined />}>
                            +{formatCurrency(previewDifference)} (Thừa)
                          </Tag>
                        ) : (
                          <Tag color="red" style={{ fontSize: 14, padding: '4px 10px' }} icon={<ExclamationCircleOutlined />}>
                            {formatCurrency(previewDifference)} (Thiếu)
                          </Tag>
                        )}
                      </div>
                    </div>
                  </Card>

                  {/* Ô nhập ghi chú / giải trình */}
                  <Form.Item
                    name="notes"
                    label={
                      <Space>
                        <span>Ghi chú / Giải trình của thu ngân</span>
                        {noteRequired ? (
                          <Tag color="red">Bắt buộc do có chênh lệch</Tag>
                        ) : (
                          <Tag color="default">Tùy chọn</Tag>
                        )}
                      </Space>
                    }
                    rules={[
                      {
                        validator: (_, value) => {
                          if (noteRequired && (!value || !value.trim())) {
                            return Promise.reject(
                              new Error('Số tiền mặt có chênh lệch, bạn bắt buộc phải nhập lý do/giải trình.')
                            )
                          }
                          return Promise.resolve()
                        },
                      },
                      { max: 1000, message: 'Ghi chú không được vượt quá 1000 ký tự.' },
                    ]}
                  >
                    <TextArea
                      rows={3}
                      placeholder={
                        noteRequired
                          ? 'Bắt buộc: Vui lòng giải trình nguyên nhân thừa/thiếu tiền mặt trước khi chốt ca...'
                          : 'Nhập ghi chú bàn giao ca nếu có...'
                      }
                    />
                  </Form.Item>

                  <Divider style={{ margin: '16px 0' }} />

                  {/* Nút bấm chốt ca kèm Popconfirm cảnh báo QTN-38 / QTN-09 */}
                  <Popconfirm
                    title="Xác nhận chốt ca làm việc?"
                    description={
                      <div style={{ maxWidth: 360 }}>
                        <Paragraph style={{ margin: 0, color: '#cf1322' }}>
                          <strong>Lưu ý quan trọng:</strong>
                        </Paragraph>
                        <Paragraph style={{ margin: 0, fontSize: 13 }}>
                          Sau khi chốt ca, toàn bộ các khoản thu trong ca sẽ bị <strong>khóa cứng vĩnh viễn</strong>. Hệ thống không cho phép sửa đổi hoặc hoàn tiền các khoản thu này nữa.
                        </Paragraph>
                        {previewDifference !== 0 && (
                          <Paragraph style={{ marginTop: 6, margin: 0, color: '#d46b08', fontSize: 13 }}>
                            ⚠️ Phiếu có chênh lệch {formatCurrency(previewDifference)} sẽ được gửi chờ Quản lý duyệt.
                          </Paragraph>
                        )}
                      </div>
                    }
                    onConfirm={() => form.submit()}
                    okText="Đồng ý chốt ca"
                    cancelText="Hủy bỏ"
                    okButtonProps={{ danger: previewDifference !== 0, loading: submitting }}
                    disabled={hasNoTransactions || submitting}
                  >
                    <Button
                      type="primary"
                      size="large"
                      block
                      danger={previewDifference !== 0}
                      disabled={hasNoTransactions}
                      loading={submitting}
                      icon={<FileDoneOutlined />}
                    >
                      {previewDifference === 0 ? 'Xác nhận Chốt ca làm việc' : 'Gửi phiếu chốt ca chờ Quản lý duyệt'}
                    </Button>
                  </Popconfirm>
                </Form>
              </Card>
            </Col>
          </Row>
        </>
      )}

      {/* Modal thông báo kết quả sau khi chốt thành công */}
      <Modal
        open={Boolean(closedResult)}
        footer={null}
        closable={false}
        width={560}
        destroyOnClose
      >
        {closedResult && (
          <Result
            status={closedResult.status === 'CONFIRMED' ? 'success' : 'warning'}
            title={
              closedResult.status === 'CONFIRMED'
                ? 'Chốt ca thu ngân thành công!'
                : 'Đã tạo phiếu chốt ca chờ Quản lý duyệt'
            }
            subTitle={
              <div>
                <Paragraph style={{ fontSize: 15 }}>
                  Mã phiếu chốt ca: <Text strong copyable>{closedResult.shiftCode}</Text>
                </Paragraph>
                {closedResult.status === 'CONFIRMED' ? (
                  <Text type="secondary">
                    Số tiền mặt hoàn toàn khớp với hệ thống. Ca làm việc đã được đóng và khóa sổ thành công.
                  </Text>
                ) : (
                  <Text type="secondary">
                    Do có chênh lệch tiền mặt ({formatCurrency(closedResult.differenceAmount)}), phiếu chốt ca đã được ghi nhận ở trạng thái <strong>Chờ Quản lý duyệt</strong>.
                  </Text>
                )}
              </div>
            }
            extra={[
              <Button
                type="primary"
                key="history"
                icon={<HistoryOutlined />}
                onClick={() => {
                  setClosedResult(null)
                  navigate('/cashier-shifts/history')
                }}
              >
                Xem lịch sử các ca đã chốt
              </Button>,
              <Button
                key="again"
                onClick={() => {
                  setClosedResult(null)
                  fetchCurrentSummary()
                }}
              >
                Tiếp tục ở màn hình này
              </Button>,
            ]}
          />
        )}
      </Modal>
    </div>
  )
}
