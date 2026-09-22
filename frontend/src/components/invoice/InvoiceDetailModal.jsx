import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  message,
  Modal,
  Popconfirm,
  Row,
  Skeleton,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import {
  ArrowRightOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  FileTextOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  PrinterOutlined,
  SwapOutlined,
} from '@ant-design/icons'
import invoiceApi from '../../api/invoiceApi'
import systemApi from '../../api/systemApi'
import { executePrintInvoice } from './InvoicePrintTemplate'
import {
  formatCurrency,
  formatDateTime,
  formatInvoiceCode,
  formatVisitCode,
  INVOICE_LINE_TYPE_META,
  INVOICE_TYPE_META,
} from '../../utils/invoiceLookupHelpers'

const { Text, Title } = Typography

export default function InvoiceDetailModal({
  open,
  onClose,
  invoiceId,
  encounter,
  onReprintSuccess,
}) {
  const [currentId, setCurrentId] = useState(invoiceId)
  const [invoice, setInvoice] = useState(null)
  const [loading, setLoading] = useState(false)
  const [adjustmentsData, setAdjustmentsData] = useState(null)
  const [loadingAdjustments, setLoadingAdjustments] = useState(false)
  const [reprinting, setReprinting] = useState(false)
  const [clinic, setClinic] = useState(null)

  useEffect(() => {
    setCurrentId(invoiceId)
  }, [invoiceId])

  // Lấy cấu hình phòng khám cho phiếu in
  useEffect(() => {
    if (open) {
      systemApi
        .clinic()
        .then((res) => {
          if (res?.data) setClinic(res.data)
        })
        .catch(() => {})
    }
  }, [open])

  // Lấy chi tiết hóa đơn
  const fetchInvoiceDetail = useCallback(async (id) => {
    if (!id) return
    setLoading(true)
    try {
      const res = await invoiceApi.getById(id)
      setInvoice(res.data || null)
    } catch (err) {
      message.error(err.response?.data?.message || 'Không thể tải chi tiết hóa đơn')
      setInvoice(null)
    } finally {
      setLoading(false)
    }
  }, [])

  // Lấy dữ liệu hóa đơn điều chỉnh liên quan
  const fetchAdjustments = useCallback(async (id) => {
    if (!id) return
    setLoadingAdjustments(true)
    try {
      const res = await invoiceApi.getAdjustments(id)
      setAdjustmentsData(res.data || null)
    } catch {
      setAdjustmentsData(null)
    } finally {
      setLoadingAdjustments(false)
    }
  }, [])

  useEffect(() => {
    if (open && currentId) {
      fetchInvoiceDetail(currentId)
      fetchAdjustments(currentId)
    } else {
      setInvoice(null)
      setAdjustmentsData(null)
    }
  }, [open, currentId, fetchInvoiceDetail, fetchAdjustments])

  // Xử lý In lại hóa đơn
  const handleReprint = async () => {
    if (!invoice?.id) return
    setReprinting(true)
    try {
      // 1. Gọi backend ghi nhận in lại trước
      const res = await invoiceApi.reprint(invoice.id)
      const updatedInvoice = res.data

      // 2. Cập nhật state modal và thông báo cho trang cha đồng bộ
      setInvoice(updatedInvoice)
      if (typeof onReprintSuccess === 'function') {
        onReprintSuccess(updatedInvoice)
      }

      message.success(`Đã ghi nhận in lại thành công (Lần ${updatedInvoice.reprintCount})`)

      // 3. Kích hoạt in chứng từ qua iframe cô lập
      executePrintInvoice({
        invoice: updatedInvoice,
        encounter,
        clinic,
        adjustmentData: adjustmentsData,
      })
    } catch (err) {
      const errorMsg =
        err.response?.data?.message ||
        err.message ||
        'Không thể ghi nhận in lại vào hệ thống. Hủy thao tác in.'
      message.error(errorMsg)
      // TUYỆT ĐỐI KHÔNG mở cửa sổ in khi backend thất bại
    } finally {
      setReprinting(false)
    }
  }

  const patient = encounter?.patient || {}
  const visit = encounter?.visit || {}
  const isAdjustment = invoice?.type === 'ADJUSTMENT'
  const reprintCount = Number(invoice?.reprintCount || 0)

  // Cột bảng khoản mục chi phí
  const lineColumns = [
    {
      title: 'STT',
      key: 'index',
      width: 60,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Tên khoản mục',
      dataIndex: 'itemName',
      key: 'itemName',
      render: (text) => <strong>{text || '—'}</strong>,
    },
    {
      title: 'Loại chi phí',
      dataIndex: 'lineType',
      key: 'lineType',
      width: 170,
      render: (type) => {
        const meta = INVOICE_LINE_TYPE_META[type] || { label: type, color: 'default' }
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Số lượng',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 90,
      align: 'center',
    },
    {
      title: 'Đơn giá',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      width: 130,
      align: 'right',
      render: (price) => formatCurrency(price),
    },
    {
      title: 'Thành tiền',
      dataIndex: 'amount',
      key: 'amount',
      width: 140,
      align: 'right',
      render: (amount) => (
        <span style={{ fontWeight: 700, color: '#0369a1' }}>{formatCurrency(amount)}</span>
      ),
    },
  ]

  // Cột bảng danh sách hóa đơn điều chỉnh con
  const adjustmentColumns = [
    {
      title: 'Mã HĐ điều chỉnh',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
      render: (code, record) => (
        <Button
          type="link"
          style={{ padding: 0, fontWeight: 600 }}
          onClick={() => setCurrentId(record.id)}
        >
          {code || record.id}
        </Button>
      ),
    },
    {
      title: 'Lý do điều chỉnh',
      dataIndex: 'adjustmentReason',
      key: 'adjustmentReason',
      render: (val) => val || '—',
    },
    {
      title: 'Số tiền',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      align: 'right',
      render: (amount) => (
        <span style={{ fontWeight: 700, color: '#6b21a8' }}>{formatCurrency(amount)}</span>
      ),
    },
    {
      title: 'Ngày lập',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date) => formatDateTime(date),
    },
    {
      title: 'In lại',
      dataIndex: 'reprintCount',
      key: 'reprintCount',
      align: 'center',
      render: (count) =>
        count > 0 ? (
          <Tag color="volcano">Đã in x{count}</Tag>
        ) : (
          <Tag color="default">Chưa in lại</Tag>
        ),
    },
  ]

  const tabItems = [
    {
      key: 'lines',
      label: (
        <span>
          <FileTextOutlined /> Khoản mục hóa đơn ({invoice?.lines?.length || 0})
        </span>
      ),
      children: (
        <Table
          rowKey="id"
          dataSource={invoice?.lines || []}
          columns={lineColumns}
          pagination={false}
          size="middle"
          bordered
          summary={() => (
            <Table.Summary.Row style={{ background: '#f8fafc' }}>
              <Table.Summary.Cell index={0} colSpan={5} style={{ textAlign: 'right', fontWeight: 700, padding: '10px 16px' }}>
                Tổng cộng:
              </Table.Summary.Cell>
              <Table.Summary.Cell
                index={5}
                style={{
                  textAlign: 'right',
                  fontWeight: 800,
                  color: Number(invoice?.totalAmount || 0) < 0 ? '#9333ea' : '#0369a1',
                  fontSize: 15,
                  padding: '10px 16px',
                }}
              >
                {formatCurrency(invoice?.totalAmount)}
              </Table.Summary.Cell>
            </Table.Summary.Row>
          )}
        />
      ),
    },
    {
      key: 'adjustments',
      label: (
        <span>
          <SwapOutlined /> Hóa đơn điều chỉnh liên quan (
          {adjustmentsData?.adjustments?.length || 0})
        </span>
      ),
      children: loadingAdjustments ? (
        <Skeleton active />
      ) : adjustmentsData ? (
        <div>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={8}>
              <Card size="small" style={{ background: '#f8fafc', borderLeft: '4px solid #0284c7' }}>
                <Statistic
                  title="Số tiền gốc ban đầu"
                  value={Number(adjustmentsData.originalAmount || 0)}
                  precision={0}
                  suffix="₫"
                  valueStyle={{ color: '#0369a1', fontWeight: 700 }}
                />
              </Card>
            </Col>
            <Col span={8} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <div style={{ textAlign: 'center', color: '#64748b' }}>
                <ArrowRightOutlined style={{ fontSize: 24 }} />
                <div style={{ fontSize: 11, marginTop: 4 }}>Cập nhật qua các đợt điều chỉnh</div>
              </div>
            </Col>
            <Col span={8}>
              <Card size="small" style={{ background: '#faf5ff', borderLeft: '4px solid #9333ea' }}>
                <Statistic
                  title="Số tiền thực thu cuối cùng"
                  value={Number(adjustmentsData.finalAmount || 0)}
                  precision={0}
                  suffix="₫"
                  valueStyle={{ color: '#7e22ce', fontWeight: 800 }}
                />
              </Card>
            </Col>
          </Row>

          <Table
            rowKey="id"
            dataSource={adjustmentsData.adjustments || []}
            columns={adjustmentColumns}
            pagination={false}
            size="small"
            locale={{ emptyText: 'Không có hóa đơn điều chỉnh nào phát sinh' }}
          />
        </div>
      ) : (
        <Empty description="Không có thông tin điều chỉnh" />
      ),
    },
  ]

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={900}
      title={
        <Space>
          <FileTextOutlined style={{ color: '#0284c7' }} />
          <span>Chi tiết hóa đơn {invoice?.invoiceCode || ''}</span>
          {invoice?.type && (
            <Tag color={INVOICE_TYPE_META[invoice.type]?.color}>
              {INVOICE_TYPE_META[invoice.type]?.label}
            </Tag>
          )}
          {reprintCount > 0 ? (
            <Tag color="volcano" icon={<PrinterOutlined />}>
              Bản in lại (x{reprintCount})
            </Tag>
          ) : (
            <Tag color="green" icon={<CheckCircleOutlined />}>
              Bản gốc
            </Tag>
          )}
        </Space>
      }
      footer={[
        <Button key="close" size="middle" onClick={onClose} style={{ minWidth: 90, borderRadius: 6 }}>
          Đóng
        </Button>,
        <Popconfirm
          key="reprint"
          title="Xác nhận in lại hóa đơn này?"
          description={
            <div style={{ maxWidth: 320 }}>
              Thao tác này sẽ ghi nhận lượt in lại vào nhật ký kiểm toán hệ thống và đánh dấu
              <strong> "BẢN IN LẠI"</strong> trên chứng từ.
            </div>
          }
          onConfirm={handleReprint}
          okText="Xác nhận & In"
          cancelText="Hủy"
          okButtonProps={{ loading: reprinting, icon: <PrinterOutlined /> }}
        >
          <Button
            type="primary"
            size="middle"
            icon={<PrinterOutlined />}
            loading={reprinting}
            disabled={loading || !invoice}
            style={{ minWidth: 140, borderRadius: 6 }}
          >
            In lại hóa đơn
          </Button>
        </Popconfirm>,
      ]}
    >
      {loading ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : invoice ? (
        <div>
          {/* Cảnh báo nếu là hóa đơn điều chỉnh */}
          {isAdjustment && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={
                <div>
                  <strong>Đây là Hóa đơn điều chỉnh:</strong> {invoice.adjustmentReason || 'Không ghi nhận lý do'}
                </div>
              }
              description={
                invoice.originalInvoiceId && (
                  <div style={{ marginTop: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span>Hóa đơn gốc tham chiếu:</span>
                    <Button
                      type="link"
                      style={{ padding: 0, fontWeight: 700, fontFamily: 'monospace' }}
                      onClick={() => setCurrentId(invoice.originalInvoiceId)}
                    >
                      {formatInvoiceCode(invoice.originalInvoiceId)}
                    </Button>
                  </div>
                )
              }
            />
          )}

          {/* Cảnh báo trạng thái in lại */}
          {reprintCount > 0 && (
            <Alert
              type="info"
              showIcon
              icon={<HistoryOutlined />}
              style={{ marginBottom: 16 }}
              message={
                <div>
                  Chứng từ này đã được in lại <strong>{reprintCount} lần</strong>. Lần in gần nhất vào:{' '}
                  <strong>{formatDateTime(invoice.lastReprintedAt)}</strong>.
                </div>
              }
            />
          )}

          <Descriptions
            size="small"
            bordered
            column={2}
            style={{ marginBottom: 16, borderRadius: 6, overflow: 'hidden' }}
            labelStyle={{ background: '#f8fafc', color: '#475569', fontWeight: 500, width: '20%' }}
            contentStyle={{ color: '#0f172a', fontWeight: 500, width: '30%' }}
          >
            <Descriptions.Item label="Mã hóa đơn">
              <strong style={{ fontFamily: 'monospace', fontSize: 14 }}>{invoice.invoiceCode}</strong>
            </Descriptions.Item>
            <Descriptions.Item label="Loại hóa đơn">
              <Tag color={INVOICE_TYPE_META[invoice.type]?.color}>
                {INVOICE_TYPE_META[invoice.type]?.label}
              </Tag>
            </Descriptions.Item>

            <Descriptions.Item label="Bệnh nhân">
              <strong>{patient.fullName || '—'}</strong> {patient.patientCode ? `(${patient.patientCode})` : ''}
            </Descriptions.Item>
            <Descriptions.Item label="Mã lượt khám">
              <span style={{ fontFamily: 'monospace' }}>
                {formatVisitCode(visit.visitCode || invoice.visitId)}
              </span>
            </Descriptions.Item>

            <Descriptions.Item label="Thời gian lập">{formatDateTime(invoice.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="Tổng tiền thanh toán">
              <span style={{ fontSize: 16, fontWeight: 800, color: '#0369a1' }}>
                {formatCurrency(invoice.totalAmount)}
              </span>
            </Descriptions.Item>

            <Descriptions.Item label="Số lần in lại">
              {reprintCount > 0 ? (
                <Tag color="volcano">Đã in lại x{reprintCount}</Tag>
              ) : (
                <Tag color="green">Chưa in lại</Tag>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Lần in lại gần nhất">
              {formatDateTime(invoice.lastReprintedAt)}
            </Descriptions.Item>
          </Descriptions>

          <Tabs defaultActiveKey="lines" items={tabItems} />
        </div>
      ) : (
        <Empty description="Không tìm thấy thông tin hóa đơn" />
      )}
    </Modal>
  )
}
