import React, { useEffect, useRef, useState } from 'react'
import { Modal, Button, Space } from 'antd'
import { PrinterOutlined, CloseOutlined } from '@ant-design/icons'
import systemApi from '../../api/systemApi'
import {
  formatCurrency,
  formatDateTime,
  formatDate,
  INVOICE_LINE_TYPE_META,
  numberToVietnameseWords,
} from '../../utils/invoiceLookupHelpers'

/**
 * Tạo nội dung HTML in hoàn chỉnh và kích hoạt in qua iframe cô lập
 */
export const executePrintInvoice = ({ invoice, encounter, clinic, adjustmentData }) => {
  if (!invoice) return

  const printFrame = document.createElement('iframe')
  printFrame.style.position = 'fixed'
  printFrame.style.right = '0'
  printFrame.style.bottom = '0'
  printFrame.style.width = '0'
  printFrame.style.height = '0'
  printFrame.style.border = '0'
  document.body.appendChild(printFrame)

  const patient = encounter?.patient || {}
  const doctor = encounter?.doctor || {}
  const visit = encounter?.visit || {}

  const clinicName = clinic?.clinicName || 'PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ'
  const clinicAddress = clinic?.address || '123 Đường Y Học, Quận 1, TP. Hồ Chí Minh'
  const clinicPhone = clinic?.phone || clinic?.hotline || '1900 8888'
  const clinicLicense = clinic?.licenseNumber || '01234/BYT-GPHĐ'

  const isAdjustment = invoice.type === 'ADJUSTMENT'
  const isReprint = Number(invoice.reprintCount || 0) > 0

  const lines = Array.isArray(invoice.lines) ? invoice.lines : []
  const totalAmountNumber = Number(invoice.totalAmount || 0)
  const amountInWords = numberToVietnameseWords(totalAmountNumber)

  const rowsHtml = lines
    .map((line, idx) => {
      const typeMeta = INVOICE_LINE_TYPE_META[line.lineType] || { label: line.lineType || 'Khoản thu' }
      return `
        <tr>
          <td style="text-align: center;">${idx + 1}</td>
          <td>
            <strong>${line.itemName || '—'}</strong>
          </td>
          <td style="text-align: center;">${typeMeta.label}</td>
          <td style="text-align: center;">${line.quantity || 1}</td>
          <td style="text-align: right;">${formatCurrency(line.unitPrice)}</td>
          <td style="text-align: right; font-weight: 600;">${formatCurrency(line.amount)}</td>
        </tr>
      `
    })
    .join('')

  const adjustmentBoxHtml = isAdjustment
    ? `
      <div style="margin: 12px 0; padding: 10px 14px; background: #faf5ff; border: 1px solid #d8b4fe; border-radius: 6px; font-size: 13px;">
        <div style="font-weight: 700; color: #6b21a8; margin-bottom: 4px;">THÔNG TIN ĐIỀU CHỈNH:</div>
        <div>• <strong>Lý do điều chỉnh:</strong> ${invoice.adjustmentReason || 'Không ghi nhận lý do'}</div>
        <div>• <strong>Mã hóa đơn gốc:</strong> <span style="font-family: monospace;">${invoice.originalInvoiceId || '—'}</span></div>
        ${
          adjustmentData
            ? `<div style="margin-top: 4px;">• <strong>Số tiền gốc:</strong> ${formatCurrency(adjustmentData.originalAmount)} | <strong>Số tiền sau điều chỉnh:</strong> <span style="color: #6b21a8; font-weight: 700;">${formatCurrency(adjustmentData.finalAmount)}</span></div>`
            : ''
        }
      </div>
    `
    : ''

  const watermarkHtml = isReprint
    ? `
      <div class="watermark-container">
        <div class="watermark-text">BẢN IN LẠI - LẦN ${invoice.reprintCount}</div>
      </div>
    `
    : ''

  const reprintBadgeHtml = isReprint
    ? `
      <div class="reprint-badge">
        <div class="reprint-badge-title">BẢN IN LẠI (LẦN ${invoice.reprintCount})</div>
        <div class="reprint-badge-sub">In lại lúc: ${formatDateTime(invoice.lastReprintedAt)}</div>
      </div>
    `
    : ''

  const frameDoc = printFrame.contentWindow.document
  frameDoc.open()
  frameDoc.write(`
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="utf-8" />
        <title>Hóa đơn ${invoice.invoiceCode || ''}</title>
        <style>
          @page {
            size: A4 portrait;
            margin: 12mm 15mm;
          }
          * {
            box-sizing: border-box;
            -webkit-print-color-adjust: exact !important;
            print-color-adjust: exact !important;
          }
          body {
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            font-size: 13.5px;
            color: #0f172a;
            background: #fff;
            line-height: 1.5;
            position: relative;
          }
          .header-row {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            border-bottom: 2px solid #0284c7;
            padding-bottom: 12px;
            margin-bottom: 16px;
          }
          .clinic-info {
            max-width: 65%;
          }
          .clinic-name {
            font-size: 16px;
            font-weight: 800;
            color: #0369a1;
            text-transform: uppercase;
            letter-spacing: 0.5px;
          }
          .clinic-detail {
            font-size: 12px;
            color: #475569;
            margin-top: 2px;
          }
          .invoice-meta {
            text-align: right;
            font-size: 12.5px;
          }
          .invoice-code {
            font-size: 15px;
            font-weight: 800;
            color: #0f172a;
            font-family: monospace;
          }
          .title-section {
            text-align: center;
            margin: 14px 0 16px;
            position: relative;
          }
          .main-title {
            font-size: 20px;
            font-weight: 800;
            color: ${isAdjustment ? '#6b21a8' : '#0369a1'};
            text-transform: uppercase;
            letter-spacing: 0.5px;
          }
          .sub-title {
            font-size: 12px;
            color: #64748b;
            margin-top: 2px;
          }
          .patient-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 6px 16px;
            padding: 10px 14px;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 6px;
            margin-bottom: 16px;
            font-size: 13px;
          }
          .lines-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 16px;
            font-size: 13px;
          }
          .lines-table th {
            background-color: #f1f5f9;
            color: #1e293b;
            font-weight: 700;
            border: 1px solid #cbd5e1;
            padding: 8px 10px;
          }
          .lines-table td {
            border: 1px solid #e2e8f0;
            padding: 7px 10px;
          }
          .summary-section {
            display: flex;
            justify-content: flex-end;
            margin-bottom: 16px;
          }
          .summary-table {
            width: 380px;
            border-collapse: collapse;
          }
          .summary-table td {
            padding: 6px 8px;
            font-size: 13.5px;
          }
          .summary-table .total-row td {
            border-top: 2px solid #0f172a;
            font-weight: 800;
            font-size: 15px;
            color: #0369a1;
          }
          .amount-words {
            font-style: italic;
            font-size: 13px;
            color: #334155;
            margin-bottom: 24px;
            padding: 8px 12px;
            background: #f8fafc;
            border-left: 3px solid #0369a1;
          }
          .signatures {
            display: grid;
            grid-template-columns: 1fr 1fr;
            margin-top: 30px;
            page-break-inside: avoid;
          }
          .sign-col {
            text-align: center;
          }
          .sign-title {
            font-weight: 700;
            font-size: 13.5px;
            text-transform: uppercase;
          }
          .sign-note {
            font-size: 11.5px;
            color: #64748b;
            font-style: italic;
          }
          .sign-space {
            height: 70px;
          }
          .reprint-badge {
            position: absolute;
            top: -6px;
            right: 0;
            border: 2px dashed #dc2626;
            background: rgba(254, 242, 242, 0.9);
            color: #dc2626;
            padding: 4px 10px;
            border-radius: 6px;
            text-align: right;
          }
          .reprint-badge-title {
            font-weight: 800;
            font-size: 12px;
            letter-spacing: 0.5px;
          }
          .reprint-badge-sub {
            font-size: 10.5px;
          }
          .watermark-container {
            position: fixed;
            top: 45%;
            left: 50%;
            transform: translate(-50%, -50%) rotate(-30deg);
            z-index: 9999;
            pointer-events: none;
            opacity: 0.13;
          }
          .watermark-text {
            font-size: 46px;
            font-weight: 900;
            color: #dc2626;
            text-transform: uppercase;
            white-space: nowrap;
            letter-spacing: 3px;
            border: 4px dashed #dc2626;
            padding: 10px 40px;
            border-radius: 12px;
          }
        </style>
      </head>
      <body>
        ${watermarkHtml}

        <div class="header-row">
          <div class="clinic-info">
            <div class="clinic-name">${clinicName}</div>
            <div class="clinic-detail">Địa chỉ: ${clinicAddress}</div>
            <div class="clinic-detail">Hotline: ${clinicPhone} | Giấy phép: ${clinicLicense}</div>
          </div>
          <div class="invoice-meta">
            <div>Mã HĐ: <span class="invoice-code">${invoice.invoiceCode || '—'}</span></div>
            <div>Ngày lập: ${formatDateTime(invoice.createdAt)}</div>
            <div>Lượt khám: ${visit.visitCode || '—'}</div>
          </div>
        </div>

        <div class="title-section">
          ${reprintBadgeHtml}
          <div class="main-title">${isAdjustment ? 'HÓA ĐƠN ĐIỀU CHỈNH THU PHÍ' : 'HÓA ĐƠN THU PHÍ KHÁM CHỮA BỆNH'}</div>
          <div class="sub-title">(Chứng từ xác nhận thanh toán viện phí)</div>
        </div>

        <div class="patient-grid">
          <div><strong>Bệnh nhân:</strong> ${patient.fullName || '—'}</div>
          <div><strong>Mã bệnh nhân:</strong> ${patient.patientCode || '—'}</div>
          <div><strong>Năm sinh / Giới tính:</strong> ${patient.dateOfBirth ? formatDate(patient.dateOfBirth) : '—'} (${patient.gender === 'MALE' ? 'Nam' : patient.gender === 'FEMALE' ? 'Nữ' : 'Khác'})</div>
          <div><strong>Điện thoại:</strong> ${patient.phone || '—'}</div>
          <div><strong>Bác sĩ khám:</strong> ${doctor.fullName || '—'}</div>
          <div><strong>Lý do khám:</strong> ${visit.reason || '—'}</div>
        </div>

        ${adjustmentBoxHtml}

        <table class="lines-table">
          <thead>
            <tr>
              <th style="width: 40px; text-align: center;">STT</th>
              <th>Khoản mục chi phí</th>
              <th style="width: 140px; text-align: center;">Loại khoản thu</th>
              <th style="width: 60px; text-align: center;">Số lượng</th>
              <th style="width: 110px; text-align: right;">Đơn giá</th>
              <th style="width: 120px; text-align: right;">Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            ${rowsHtml || '<tr><td colspan="6" style="text-align: center; color: #94a3b8;">Không có khoản mục nào</td></tr>'}
          </tbody>
        </table>

        <div class="summary-section">
          <table class="summary-table">
            <tr class="total-row">
              <td><strong>TỔNG TIỀN THANH TOÁN:</strong></td>
              <td style="text-align: right;">${formatCurrency(invoice.totalAmount)}</td>
            </tr>
          </table>
        </div>

        <div class="amount-words">
          <strong>Số tiền viết bằng chữ:</strong> ${amountInWords}
        </div>

        <div class="signatures">
          <div class="sign-col">
            <div class="sign-title">Người nộp tiền</div>
            <div class="sign-note">(Ký, ghi rõ họ tên)</div>
            <div class="sign-space"></div>
            <div>${patient.fullName || ''}</div>
          </div>
          <div class="sign-col">
            <div class="sign-title">Người lập hóa đơn / Thu ngân</div>
            <div class="sign-note">(Ký, đóng dấu xác nhận)</div>
            <div class="sign-space"></div>
            <div>Bệnh viện / Phòng khám</div>
          </div>
        </div>
      </body>
    </html>
  `)
  frameDoc.close()

  setTimeout(() => {
    try {
      printFrame.contentWindow.focus()
      printFrame.contentWindow.print()
    } catch (e) {
      console.error('Lỗi khi kích hoạt in:', e)
    } finally {
      setTimeout(() => {
        if (printFrame.parentNode) {
          printFrame.parentNode.removeChild(printFrame)
        }
      }, 2000)
    }
  }, 400)
}

/**
 * Component hiển thị Modal xem trước phiếu in hóa đơn
 */
export default function InvoicePrintTemplateModal({
  open,
  onClose,
  invoice,
  encounter,
  adjustmentData,
}) {
  const [clinic, setClinic] = useState(null)
  const [loadingClinic, setLoadingClinic] = useState(false)

  useEffect(() => {
    if (open) {
      setLoadingClinic(true)
      systemApi
        .clinic()
        .then((res) => {
          if (res?.data) setClinic(res.data)
        })
        .catch(() => {})
        .finally(() => setLoadingClinic(false))
    }
  }, [open])

  if (!invoice) return null

  const handlePrint = () => {
    executePrintInvoice({ invoice, encounter, clinic, adjustmentData })
  }

  const patient = encounter?.patient || {}
  const isAdjustment = invoice.type === 'ADJUSTMENT'
  const isReprint = Number(invoice.reprintCount || 0) > 0
  const lines = Array.isArray(invoice.lines) ? invoice.lines : []

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={780}
      title="Xem trước bản in hóa đơn"
      footer={[
        <Button key="close" icon={<CloseOutlined />} onClick={onClose}>
          Đóng
        </Button>,
        <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={handlePrint}>
          In hóa đơn
        </Button>,
      ]}
    >
      <div
        style={{
          border: '1px solid #e2e8f0',
          padding: 24,
          borderRadius: 8,
          background: '#fff',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {isReprint && (
          <div
            style={{
              position: 'absolute',
              top: 14,
              right: 14,
              border: '2px dashed #dc2626',
              background: '#fef2f2',
              color: '#dc2626',
              padding: '4px 10px',
              borderRadius: 6,
              textAlign: 'right',
            }}
          >
            <div style={{ fontWeight: 800, fontSize: 12 }}>BẢN IN LẠI (LẦN {invoice.reprintCount})</div>
            <div style={{ fontSize: 11 }}>In lại lúc: {formatDateTime(invoice.lastReprintedAt)}</div>
          </div>
        )}

        <div style={{ marginBottom: 16 }}>
          <div style={{ fontSize: 15, fontWeight: 800, color: '#0369a1' }}>
            {clinic?.clinicName || 'PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ'}
          </div>
          <div style={{ fontSize: 12, color: '#64748b' }}>
            {clinic?.address || '123 Đường Y Học, Quận 1, TP. Hồ Chí Minh'}
          </div>
        </div>

        <div style={{ textAlign: 'center', margin: '20px 0 16px' }}>
          <div
            style={{
              fontSize: 18,
              fontWeight: 800,
              color: isAdjustment ? '#6b21a8' : '#0369a1',
            }}
          >
            {isAdjustment ? 'HÓA ĐƠN ĐIỀU CHỈNH THU PHÍ' : 'HÓA ĐƠN THU PHÍ KHÁM CHỮA BỆNH'}
          </div>
          <div style={{ fontSize: 12, color: '#64748b' }}>Mã HĐ: {invoice.invoiceCode}</div>
        </div>

        <div
          style={{
            background: '#f8fafc',
            padding: 12,
            borderRadius: 6,
            marginBottom: 16,
            fontSize: 13,
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: 6,
          }}
        >
          <div>
            <strong>Bệnh nhân:</strong> {patient.fullName || '—'}
          </div>
          <div>
            <strong>Mã BN:</strong> {patient.patientCode || '—'}
          </div>
          <div>
            <strong>Ngày lập:</strong> {formatDateTime(invoice.createdAt)}
          </div>
          <div>
            <strong>Tổng tiền:</strong>{' '}
            <span style={{ color: '#0369a1', fontWeight: 700 }}>{formatCurrency(invoice.totalAmount)}</span>
          </div>
        </div>

        {isAdjustment && (
          <div
            style={{
              padding: 10,
              background: '#faf5ff',
              border: '1px solid #d8b4fe',
              borderRadius: 6,
              marginBottom: 16,
              fontSize: 13,
            }}
          >
            <div style={{ fontWeight: 700, color: '#6b21a8' }}>THÔNG TIN ĐIỀU CHỈNH:</div>
            <div>• Lý do điều chỉnh: {invoice.adjustmentReason || 'Không ghi nhận lý do'}</div>
            <div>• Hóa đơn gốc liên kết: {invoice.originalInvoiceId || '—'}</div>
          </div>
        )}

        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13, marginBottom: 16 }}>
          <thead>
            <tr style={{ background: '#f1f5f9' }}>
              <th style={{ border: '1px solid #cbd5e1', padding: '6px 8px', textAlign: 'center' }}>STT</th>
              <th style={{ border: '1px solid #cbd5e1', padding: '6px 8px' }}>Khoản mục</th>
              <th style={{ border: '1px solid #cbd5e1', padding: '6px 8px', textAlign: 'center' }}>Loại</th>
              <th style={{ border: '1px solid #cbd5e1', padding: '6px 8px', textAlign: 'center' }}>SL</th>
              <th style={{ border: '1px solid #cbd5e1', padding: '6px 8px', textAlign: 'right' }}>Đơn giá</th>
              <th style={{ border: '1px solid #cbd5e1', padding: '6px 8px', textAlign: 'right' }}>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            {lines.map((l, i) => {
              const meta = INVOICE_LINE_TYPE_META[l.lineType] || { label: l.lineType }
              return (
                <tr key={l.id || i}>
                  <td style={{ border: '1px solid #e2e8f0', padding: '6px 8px', textAlign: 'center' }}>{i + 1}</td>
                  <td style={{ border: '1px solid #e2e8f0', padding: '6px 8px' }}>{l.itemName}</td>
                  <td style={{ border: '1px solid #e2e8f0', padding: '6px 8px', textAlign: 'center' }}>{meta.label}</td>
                  <td style={{ border: '1px solid #e2e8f0', padding: '6px 8px', textAlign: 'center' }}>{l.quantity}</td>
                  <td style={{ border: '1px solid #e2e8f0', padding: '6px 8px', textAlign: 'right' }}>
                    {formatCurrency(l.unitPrice)}
                  </td>
                  <td style={{ border: '1px solid #e2e8f0', padding: '6px 8px', textAlign: 'right', fontWeight: 600 }}>
                    {formatCurrency(l.amount)}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>

        <div style={{ textAlign: 'right', fontSize: 15, fontWeight: 800, color: '#0369a1', marginBottom: 12 }}>
          TỔNG CỘNG: {formatCurrency(invoice.totalAmount)}
        </div>
        <div style={{ fontStyle: 'italic', fontSize: 12.5, color: '#475569' }}>
          Bằng chữ: {numberToVietnameseWords(invoice.totalAmount)}
        </div>
      </div>
    </Modal>
  )
}
