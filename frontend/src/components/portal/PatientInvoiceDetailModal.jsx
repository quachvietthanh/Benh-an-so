import React, { useEffect, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Descriptions,
  Divider,
  Modal,
  Skeleton,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  CopyOutlined,
  DownloadOutlined,
  FileDoneOutlined,
  FileTextOutlined,
  InfoCircleOutlined,
  MedicineBoxOutlined,
  PrinterOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
} from '@ant-design/icons'

import patientPortalInvoiceApi from '../../api/patientPortalInvoiceApi'
import {
  downloadPdfBlob,
  formatCurrency,
  formatDate,
  formatDateTime,
  getSecuritySafeErrorMessage,
  numberToVietnameseWords,
} from '../../utils/patientInvoiceHelpers'

const { Title, Text } = Typography

function PatientInvoiceDetailModal({
  open,
  onClose,
  invoiceId,
  initialSummary,
  onSelectAdjustment,
}) {
  const [loading, setLoading] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [invoiceDetail, setInvoiceDetail] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (!open || !invoiceId) {
      setInvoiceDetail(null)
      setErrorMessage('')
      return
    }

    let isMounted = true
    const fetchDetail = async () => {
      setLoading(true)
      setErrorMessage('')
      try {
        const res = await patientPortalInvoiceApi.getInvoiceDetail(invoiceId)
        if (isMounted) {
          setInvoiceDetail(res.data)
        }
      } catch (err) {
        if (isMounted) {
          // Security constraint: generic friendly message for 403/404, never reveal if invoice exists
          const safeMsg = getSecuritySafeErrorMessage(err)
          setErrorMessage(safeMsg)
          message.error(safeMsg)
        }
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    fetchDetail()

    return () => {
      isMounted = false
    }
  }, [open, invoiceId])

  const handleCopyCode = (code) => {
    if (!code) return
    navigator.clipboard.writeText(code)
    message.success(`Đã sao chép mã ${code}!`)
  }

  const handleDownload = async () => {
    const targetId = invoiceDetail?.invoiceId || invoiceId
    const targetCode = invoiceDetail?.invoiceCode || initialSummary?.invoiceCode || 'HD'
    if (!targetId || downloading) return

    setDownloading(true)
    try {
      const response = await patientPortalInvoiceApi.downloadInvoice(targetId)
      const disposition = response.headers?.['content-disposition']
      let filename = `hoa-don-${targetCode}.pdf`
      if (disposition && disposition.includes('filename=')) {
        const match = disposition.match(/filename="?([^"]+)"?/)
        if (match && match[1]) filename = match[1]
      }
      downloadPdfBlob(response.data, filename)
      message.success('Đã tải hóa đơn về máy thành công!')
    } catch (err) {
      const safeMsg = getSecuritySafeErrorMessage(err)
      message.error(safeMsg)
    } finally {
      setDownloading(false)
    }
  }

  const isAdjustment =
    String(invoiceDetail?.invoiceType || initialSummary?.invoiceType || '').toUpperCase() === 'ADJUSTMENT'

  const lineColumns = [
    {
      title: 'STT',
      key: 'index',
      width: 55,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Khoản mục / Dịch vụ y tế',
      dataIndex: 'itemName',
      key: 'itemName',
      render: (text, record) => (
        <div>
          <strong style={{ color: '#1e293b' }}>{text || 'Dịch vụ khám chữa bệnh'}</strong>
          {record.lineType && (
            <div style={{ fontSize: 11.5, color: '#64748b', marginTop: 2 }}>
              Loại:{' '}
              {record.lineType === 'EXAM_FEE'
                ? 'Công khám bệnh'
                : record.lineType === 'SERVICE_FEE'
                ? 'Dịch vụ cận lâm sàng'
                : record.lineType === 'MEDICINE_FEE'
                ? 'Thuốc / Dược phẩm'
                : record.lineType}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Số lượng',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 90,
      align: 'center',
      render: (val) => val || 1,
    },
    {
      title: 'Đơn giá',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      width: 120,
      align: 'right',
      render: (val) => formatCurrency(val),
    },
    {
      title: 'Thành tiền',
      dataIndex: 'amount',
      key: 'amount',
      width: 130,
      align: 'right',
      render: (val) => (
        <span style={{ fontWeight: 700, color: '#1e3a8a' }}>{formatCurrency(val)}</span>
      ),
    },
  ]

  const totalAmount = invoiceDetail?.totalAmount ?? initialSummary?.totalAmount ?? 0
  const items = invoiceDetail?.items || []

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={760}
      style={{ top: 24 }}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <FileDoneOutlined style={{ color: '#2563eb', fontSize: 20 }} />
          <span>Chi tiết hóa đơn viện phí</span>
        </div>
      }
      footer={[
        <Button key="close" onClick={onClose}>
          Đóng
        </Button>,
        <Button
          key="download"
          type="primary"
          icon={<DownloadOutlined />}
          loading={downloading}
          onClick={handleDownload}
          style={{ background: '#2563eb', borderColor: '#2563eb' }}
        >
          Tải bản in PDF
        </Button>,
      ]}
    >
      {loading ? (
        <div style={{ padding: '24px 0' }}>
          <Skeleton active paragraph={{ rows: 6 }} />
        </div>
      ) : errorMessage ? (
        <div style={{ padding: '24px 0' }}>
          <Alert
            type="error"
            message="Không thể hiển thị hóa đơn"
            description={errorMessage}
            showIcon
          />
        </div>
      ) : (
        <div className="invoice-detail-sheet">
          {/* Badge & Type Alert Banner */}
          {isAdjustment ? (
            <Alert
              type="info"
              icon={<InfoCircleOutlined style={{ color: '#2563eb' }} />}
              showIcon
              message={
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <Tag color="blue" style={{ fontWeight: 700 }}>
                    HÓA ĐƠN ĐIỀU CHỈNH
                  </Tag>
                  <span>
                    Đây là bản hóa đơn điều chỉnh có hiệu lực cao nhất cho lượt khám này.
                  </span>
                </div>
              }
              description={
                <div style={{ fontSize: 13, marginTop: 4 }}>
                  {invoiceDetail?.originalInvoiceCode && (
                    <div>
                      Hóa đơn gốc được điều chỉnh:{' '}
                      <strong style={{ color: '#1e3a8a' }}>
                        {invoiceDetail.originalInvoiceCode}
                      </strong>
                    </div>
                  )}
                  {invoiceDetail?.adjustmentReason && (
                    <div style={{ marginTop: 2 }}>
                      Lý do điều chỉnh:{' '}
                      <strong style={{ color: '#334155' }}>
                        {invoiceDetail.adjustmentReason}
                      </strong>
                    </div>
                  )}
                </div>
              }
              style={{ marginBottom: 16, borderRadius: 10 }}
            />
          ) : initialSummary?.hasBeenAdjusted ? (
            <Alert
              type="warning"
              icon={<WarningOutlined style={{ color: '#d97706' }} />}
              showIcon
              message={
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <Tag color="warning" style={{ fontWeight: 700 }}>
                    HÓA ĐƠN ĐÃ ĐƯỢC ĐIỀU CHỈNH
                  </Tag>
                  <span>
                    Hóa đơn gốc này đã có bản điều chỉnh thay thế kèm lý do từ phòng khám.
                  </span>
                </div>
              }
              description={
                <div style={{ fontSize: 13, marginTop: 4 }}>
                  <span>
                    Để làm chứng từ thanh toán bảo hiểm hoặc quyết toán hợp lệ, vui lòng sử dụng bản điều chỉnh mới nhất.
                  </span>
                  {initialSummary?.effectiveAdjustment && (
                    <div style={{ marginTop: 6 }}>
                      <Button
                        type="link"
                        size="small"
                        style={{ padding: 0, fontWeight: 600, color: '#2563eb' }}
                        onClick={() => {
                          if (onSelectAdjustment) {
                            onSelectAdjustment(initialSummary.effectiveAdjustment.invoiceId)
                          }
                        }}
                      >
                        👉 Xem ngay hóa đơn điều chỉnh mới nhất ({initialSummary.effectiveAdjustment.invoiceCode})
                      </Button>
                    </div>
                  )}
                </div>
              }
              style={{ marginBottom: 16, borderRadius: 10 }}
            />
          ) : (
            <Alert
              type="success"
              icon={<CheckCircleOutlined style={{ color: '#16a34a' }} />}
              showIcon
              message="Hóa đơn gốc hợp lệ"
              description="Hóa đơn đã được chốt hoàn tất và hợp lệ dùng làm chứng từ thanh toán bảo hiểm hoặc lưu trữ cá nhân."
              style={{ marginBottom: 16, borderRadius: 10 }}
            />
          )}

          {/* Electronic Invoice Paper Card */}
          <div
            style={{
              background: '#f8fafc',
              border: '1px solid #e2e8f0',
              borderRadius: 12,
              padding: '20px 24px',
              marginBottom: 16,
            }}
          >
            {/* Header info */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                flexWrap: 'wrap',
                gap: 16,
                borderBottom: '1px dashed #cbd5e1',
                paddingBottom: 16,
                marginBottom: 16,
              }}
            >
              <div>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    color: '#1e3a8a',
                    fontWeight: 800,
                    fontSize: 16,
                  }}
                >
                  <MedicineBoxOutlined style={{ fontSize: 20 }} />
                  <span>PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ</span>
                </div>
                <div style={{ fontSize: 12.5, color: '#64748b', marginTop: 4 }}>
                  Hệ thống quản trị và chăm sóc y tế kỹ thuật số
                </div>
              </div>

              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 12, color: '#64748b' }}>MÃ HÓA ĐƠN</div>
                <div
                  style={{
                    fontSize: 16,
                    fontWeight: 800,
                    color: '#2563eb',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 6,
                    justifyContent: 'flex-end',
                  }}
                >
                  <span>{invoiceDetail?.invoiceCode || initialSummary?.invoiceCode || '—'}</span>
                  <CopyOutlined
                    style={{ cursor: 'pointer', fontSize: 14, color: '#64748b' }}
                    onClick={() =>
                      handleCopyCode(invoiceDetail?.invoiceCode || initialSummary?.invoiceCode)
                    }
                  />
                </div>
                <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                  Ngày phát hành: {formatDateTime(invoiceDetail?.createdAt || initialSummary?.createdAt)}
                </div>
              </div>
            </div>

            {/* Visit and Doctor Meta */}
            <Descriptions
              size="small"
              bordered
              column={{ xs: 1, sm: 2 }}
              style={{ marginBottom: 16, background: '#ffffff', borderRadius: 8 }}
            >
              <Descriptions.Item label="Mã lượt khám">
                <strong>{invoiceDetail?.visitCode || initialSummary?.visitCode || '—'}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="Ngày khám">
                <span>{formatDate(invoiceDetail?.visitDate || initialSummary?.visitDate)}</span>
              </Descriptions.Item>
              <Descriptions.Item label="Bác sĩ khám">
                <span>
                  {invoiceDetail?.doctorName
                    ? `BS. ${invoiceDetail.doctorName}`
                    : initialSummary?.doctorName
                    ? `BS. ${initialSummary.doctorName}`
                    : '—'}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="Chuyên khoa">
                <span>{invoiceDetail?.specialtyName || initialSummary?.specialtyName || '—'}</span>
              </Descriptions.Item>
              {invoiceDetail?.creatorName && (
                <Descriptions.Item label="Thu ngân / Người lập" span={2}>
                  <span>{invoiceDetail.creatorName}</span>
                </Descriptions.Item>
              )}
            </Descriptions>

            {/* Line Items Table */}
            <div style={{ marginBottom: 16 }}>
              <div
                style={{
                  fontSize: 13,
                  fontWeight: 700,
                  color: '#334155',
                  marginBottom: 8,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                }}
              >
                <FileTextOutlined style={{ color: '#2563eb' }} />
                <span>DANH MỤC CÁC KHOẢN VIỆN PHÍ ĐÃ THANH TOÁN</span>
              </div>
              <Table
                dataSource={items.map((item, idx) => ({ ...item, key: item.lineId || idx }))}
                columns={lineColumns}
                pagination={false}
                size="small"
                bordered
                locale={{ emptyText: 'Chưa có chi tiết khoản mục' }}
              />
            </div>

            {/* Total Amount & Words */}
            <div
              style={{
                background: '#eff6ff',
                border: '1px solid #bfdbfe',
                borderRadius: 8,
                padding: '12px 16px',
                display: 'flex',
                flexDirection: 'column',
                gap: 6,
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                }}
              >
                <span style={{ fontSize: 14, fontWeight: 700, color: '#1e3a8a' }}>
                  TỔNG CỘNG TIỀN THANH TOÁN:
                </span>
                <span style={{ fontSize: 20, fontWeight: 800, color: '#1e3a8a' }}>
                  {formatCurrency(totalAmount)}
                </span>
              </div>
              <div style={{ fontSize: 13, color: '#334155', fontStyle: 'italic' }}>
                Số tiền viết bằng chữ: <strong>{numberToVietnameseWords(totalAmount)}</strong>
              </div>
            </div>
          </div>

          <div
            style={{
              fontSize: 12,
              color: '#64748b',
              textAlign: 'center',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 6,
            }}
          >
            <SafetyCertificateOutlined style={{ color: '#10b981' }} />
            <span>
              Chứng từ điện tử chính thức phát hành từ Cổng thông tin Bệnh Án Số, có giá trị thanh toán bảo hiểm và quyết toán thuế.
            </span>
          </div>
        </div>
      )}
    </Modal>
  )
}

export default PatientInvoiceDetailModal
