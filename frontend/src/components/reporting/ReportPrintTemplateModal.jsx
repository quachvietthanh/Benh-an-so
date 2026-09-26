import React, { useRef } from 'react'
import { Button, Card, Col, Divider, Modal, Row, Space, Table, Tag, Typography } from 'antd'
import { CheckCircleOutlined, PrinterOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useAuthContext } from '../../context/AuthContext'

const { Title, Text, Paragraph } = Typography

const formatMoney = (v) => `${Number(v || 0).toLocaleString('vi-VN')} đ`

export default function ReportPrintTemplateModal({
  open = false,
  onClose,
  activeTab = 'overview',
  range = [],
  summary = {},
  timeline = [],
  topMedicines = [],
  invoices = [],
}) {
  const { user } = useAuthContext()
  const printAreaRef = useRef(null)

  const fromFormatted = range?.[0] ? range[0].format('DD/MM/YYYY') : '—'
  const toFormatted = range?.[1] ? range[1].format('DD/MM/YYYY') : '—'
  const printedAt = dayjs().format('HH:mm:ss [ngày] DD/MM/YYYY')
  const reporterName = user?.fullName || user?.username || 'Quản trị viên'

  const getReportTitle = () => {
    switch (activeTab) {
      case 'visits':
        return 'BÁO CÁO THỐNG KÊ LƯỢT KHÁM BỆNH'
      case 'doctor-visits':
        return 'BÁO CÁO LƯỢT KHÁM THEO BÁC SĨ'
      case 'revenue':
        return 'BÁO CÁO DOANH THU & THU PHÍ DỊCH VỤ'
      case 'medicines':
        return 'BÁO CÁO DƯỢC PHẨM & CẤP PHÁT THUỐC'
      case 'audit':
        return 'BÁO CÁO NHẬT KÝ KIỂM TOÁN VẬN HÀNH'
      default:
        return 'BÁO CÁO TỔNG QUAN VẬN HÀNH PHÒNG KHÁM'
    }
  }

  const handlePrint = () => {
    window.print()
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={900}
      title={
        <Space>
          <PrinterOutlined style={{ color: '#2563eb' }} />
          <span>Xem trước & In Báo cáo Vận hành</span>
        </Space>
      }
      footer={[
        <Button key="close" onClick={onClose}>
          Đóng
        </Button>,
        <Button
          key="print"
          type="primary"
          icon={<PrinterOutlined />}
          onClick={handlePrint}
          style={{ backgroundColor: '#2563eb' }}
        >
          In báo cáo (Ctrl + P)
        </Button>,
      ]}
      style={{ top: 20 }}
    >
      <div
        ref={printAreaRef}
        className="report-print-container"
        style={{
          background: '#ffffff',
          padding: '24px 28px',
          color: '#0f172a',
          fontFamily: "'Times New Roman', Times, serif",
        }}
      >
        {/* Print Header */}
        <Row justify="space-between" align="top" style={{ marginBottom: 16 }}>
          <Col span={13}>
            <div style={{ fontWeight: 700, fontSize: 13, textTransform: 'uppercase', color: '#1e293b' }}>
              SỞ Y TẾ TP. HÀ NỘI
            </div>
            <div style={{ fontWeight: 800, fontSize: 15, textTransform: 'uppercase', color: '#1d4ed8' }}>
              PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ
            </div>
            <div style={{ fontSize: 12, color: '#475569' }}>
              Địa chỉ: 123 Đường Y Học, Quận Đống Đa, Hà Nội
            </div>
            <div style={{ fontSize: 12, color: '#475569' }}>
              Hotline: 1900 6868 · Website: https://benh-an-so.vn
            </div>
          </Col>
          <Col span={11} style={{ textAlign: 'right' }}>
            <div style={{ fontWeight: 700, fontSize: 12, textTransform: 'uppercase' }}>
              CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
            </div>
            <div style={{ fontSize: 12, fontStyle: 'italic', color: '#334155' }}>
              Độc lập - Tự do - Hạnh phúc
            </div>
            <div style={{ fontSize: 11, color: '#64748b', marginTop: 6 }}>
              Mã báo cáo: <Text code style={{ fontSize: 11 }}>RP-{dayjs().format('YYYYMMDD-HHmmss')}</Text>
            </div>
          </Col>
        </Row>

        <Divider style={{ margin: '8px 0 16px', borderColor: '#cbd5e1' }} />

        {/* Title */}
        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <Title level={3} style={{ margin: 0, textTransform: 'uppercase', color: '#0f172a', letterSpacing: 0.5 }}>
            {getReportTitle()}
          </Title>
          <div style={{ fontSize: 13, color: '#334155', fontStyle: 'italic', marginTop: 4 }}>
            Khoảng thời gian thống kê: Từ ngày <strong>{fromFormatted}</strong> đến ngày <strong>{toFormatted}</strong>
          </div>
          <div style={{ fontSize: 11.5, color: '#64748b', marginTop: 2 }}>
            Thời điểm trích xuất: {printedAt} · Người lập: <strong>{reporterName}</strong>
          </div>
        </div>

        {/* Key Indicators Summary Card */}
        <div
          style={{
            border: '1px solid #cbd5e1',
            borderRadius: 6,
            padding: '12px 16px',
            marginBottom: 20,
            backgroundColor: '#f8fafc',
          }}
        >
          <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 8, color: '#1e293b', textTransform: 'uppercase' }}>
            I. Tổng hợp chỉ số vận hành cốt lõi
          </div>
          <Row gutter={[12, 8]}>
            <Col span={6}>
              <div style={{ fontSize: 11.5, color: '#64748b' }}>Tổng lượt khám bệnh:</div>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#2563eb' }}>
                {summary.visitCount || 0} lượt
              </div>
            </Col>
            <Col span={6}>
              <div style={{ fontSize: 11.5, color: '#64748b' }}>Tổng doanh thu phòng khám:</div>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#16a34a' }}>
                {formatMoney(summary.revenue)}
              </div>
            </Col>
            <Col span={6}>
              <div style={{ fontSize: 11.5, color: '#64748b' }}>Tổng đơn thuốc đã cấp:</div>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#d97706' }}>
                {summary.dispensedCount || 0} đơn
              </div>
            </Col>
            <Col span={6}>
              <div style={{ fontSize: 11.5, color: '#64748b' }}>Lượt kiểm toán truy cập:</div>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#7c3aed' }}>
                {summary.auditCount || 0} lượt
              </div>
            </Col>
          </Row>
        </div>

        {/* Section Detail Data */}
        <div style={{ marginBottom: 24 }}>
          <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 8, color: '#1e293b', textTransform: 'uppercase' }}>
            II. Chi tiết dữ liệu thống kê
          </div>

          {(activeTab === 'visits' || activeTab === 'overview') && (
            <table
              style={{
                width: '100%',
                borderCollapse: 'collapse',
                fontSize: 12,
                marginBottom: 16,
              }}
            >
              <thead>
                <tr style={{ backgroundColor: '#f1f5f9' }}>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'center', width: 50 }}>STT</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'left' }}>Ngày khám</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'right', width: 140 }}>Số lượt khám</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'right', width: 180 }}>Doanh thu trong ngày</th>
                </tr>
              </thead>
              <tbody>
                {timeline && timeline.length > 0 ? (
                  timeline.slice(0, 30).map((item, idx) => (
                    <tr key={idx}>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'center' }}>{idx + 1}</td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px' }}>{item.reportDate || item.date}</td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'right', fontWeight: 600 }}>
                        {item.visitCount || 0}
                      </td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'right', fontWeight: 600, color: '#15803d' }}>
                        {formatMoney(item.revenue)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4} style={{ border: '1px solid #cbd5e1', padding: 12, textAlign: 'center', color: '#64748b' }}>
                      Chưa có dữ liệu lượt khám trong khoảng thời gian này
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}

          {activeTab === 'medicines' && (
            <table
              style={{
                width: '100%',
                borderCollapse: 'collapse',
                fontSize: 12,
                marginBottom: 16,
              }}
            >
              <thead>
                <tr style={{ backgroundColor: '#f1f5f9' }}>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'center', width: 50 }}>Top</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'left' }}>Tên thuốc / Dược phẩm</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'left', width: 160 }}>Nhóm</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'right', width: 160 }}>Số lượng cấp phát</th>
                </tr>
              </thead>
              <tbody>
                {topMedicines && topMedicines.length > 0 ? (
                  topMedicines.slice(0, 20).map((m, idx) => (
                    <tr key={idx}>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'center', fontWeight: 700 }}>{idx + 1}</td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', fontWeight: 600 }}>{m.name}</td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', color: '#475569' }}>{m.category || 'Dược phẩm'}</td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'right', fontWeight: 700, color: '#b91c1c' }}>
                        {m.dispensedQuantity || 0} đơn vị
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4} style={{ border: '1px solid #cbd5e1', padding: 12, textAlign: 'center', color: '#64748b' }}>
                      Chưa có dữ liệu cấp phát thuốc
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}

          {activeTab === 'revenue' && (
            <table
              style={{
                width: '100%',
                borderCollapse: 'collapse',
                fontSize: 12,
                marginBottom: 16,
              }}
            >
              <thead>
                <tr style={{ backgroundColor: '#f1f5f9' }}>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'center', width: 50 }}>STT</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'left' }}>Mã hóa đơn</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'left' }}>Bệnh nhân</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'center', width: 120 }}>Hình thức</th>
                  <th style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'right', width: 150 }}>Số tiền</th>
                </tr>
              </thead>
              <tbody>
                {invoices && invoices.length > 0 ? (
                  invoices.slice(0, 30).map((inv, idx) => (
                    <tr key={idx}>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'center' }}>{idx + 1}</td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', fontFamily: 'monospace', fontWeight: 600 }}>
                        {inv.invoiceCode || String(inv.id || '').slice(-8)}
                      </td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px' }}>{inv.patientName || 'Bệnh nhân'}</td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'center' }}>
                        {inv.paymentMethod === 'BANK_TRANSFER' ? 'Chuyển khoản' : 'Tiền mặt'}
                      </td>
                      <td style={{ border: '1px solid #cbd5e1', padding: '6px 10px', textAlign: 'right', fontWeight: 700, color: '#15803d' }}>
                        {formatMoney(inv.totalAmount)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} style={{ border: '1px solid #cbd5e1', padding: 12, textAlign: 'center', color: '#64748b' }}>
                      Chưa có hóa đơn thu phí
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>

        {/* Signatures */}
        <Row justify="space-between" align="top" style={{ marginTop: 32, pageBreakInside: 'avoid' }}>
          <Col span={10} style={{ textAlign: 'center' }}>
            <div style={{ fontWeight: 700, fontSize: 13, textTransform: 'uppercase' }}>
              NGƯỜI LẬP BÁO CÁO
            </div>
            <div style={{ fontSize: 11, fontStyle: 'italic', color: '#64748b', marginBottom: 50 }}>
              (Ký và ghi rõ họ tên)
            </div>
            <div style={{ fontWeight: 700, fontSize: 13 }}>{reporterName}</div>
          </Col>
          <Col span={10} style={{ textAlign: 'center' }}>
            <div style={{ fontWeight: 700, fontSize: 13, textTransform: 'uppercase' }}>
              GIÁM ĐỐC PHÒNG KHÁM
            </div>
            <div style={{ fontSize: 11, fontStyle: 'italic', color: '#64748b', marginBottom: 50 }}>
              (Ký, đóng dấu và ghi rõ họ tên)
            </div>
            <div style={{ fontWeight: 700, fontSize: 13 }}>BS. CKII. NGUYỄN VĂN AN</div>
          </Col>
        </Row>
      </div>

      <style>{`
        @media print {
          body * {
            visibility: hidden;
          }
          .report-print-container,
          .report-print-container * {
            visibility: visible;
          }
          .report-print-container {
            position: absolute;
            left: 0;
            top: 0;
            width: 100%;
            padding: 20px;
          }
          .ant-modal-footer,
          .ant-modal-header,
          .ant-modal-close {
            display: none !important;
          }
        }
      `}</style>
    </Modal>
  )
}
