import React, { useEffect, useRef, useState } from 'react'
import { Button, Modal, Space, Spin } from 'antd'
import { PrinterOutlined, CloseOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import systemApi from '../../api/systemApi'
import { fixMojibake } from '../../utils/serviceCatalogValidation'

const ROUTE_LABELS = {
  ORAL: 'Uống',
  TOPICAL: 'Bôi ngoài da',
  INHALATION: 'Hít / Khí dung',
  OPHTHALMIC: 'Nhỏ / Tra mắt',
  NASAL: 'Xịt / Nhỏ mũi',
  OTIC: 'Nhỏ tai',
  SUBLINGUAL: 'Ngậm dưới lưỡi',
  RECTAL: 'Đặt hậu môn / Trực tràng',
  INTRAVENOUS: 'Tiêm tĩnh mạch',
  INTRAMUSCULAR: 'Tiêm bắp',
  SUBCUTANEOUS: 'Tiêm dưới da',
  TRANSDERMAL: 'Dán ngoài da',
  OTHER: 'Khác',
}

// Generate simple visual barcode bars
function BarcodeView({ code }) {
  if (!code) return null
  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', height: 22, gap: 1.5 }}>
        {code.split('').map((char, idx) => {
          const val = char.charCodeAt(0)
          const widths = [1.2, 2.2, 1.5, 2.8, 1.2, 3]
          const heights = [18, 22, 20, 22, 19, 22]
          const w = widths[idx % widths.length]
          const h = heights[(val + idx) % heights.length]
          return (
            <React.Fragment key={idx}>
              <div style={{ width: w, height: h, backgroundColor: '#1e40af' }} />
              <div style={{ width: 1, height: h }} />
            </React.Fragment>
          )
        })}
      </div>
      <span style={{ fontSize: 17, fontWeight: 800, letterSpacing: 1, color: '#1e40af', fontFamily: 'monospace' }}>
        {code}
      </span>
    </div>
  )
}

function PrescriptionPrintTemplateModal({
  open,
  onClose,
  prescription,
  record,
  diagnoses = [],
  patient,
  encounter,
}) {
  const [clinic, setClinic] = useState(null)
  const [loadingClinic, setLoadingClinic] = useState(false)
  const printAreaRef = useRef(null)

  useEffect(() => {
    if (open) {
      setLoadingClinic(true)
      systemApi
        .clinic()
        .then((res) => {
          if (res.data) setClinic(res.data)
        })
        .catch(() => {
          // ignore or keep default
        })
        .finally(() => {
          setLoadingClinic(false)
        })
    }
  }, [open])

  if (!prescription) return null

  const items = prescription.items || []
  const patientData = encounter?.patient || patient || {}
  const doctorData = encounter?.doctor || {}

  const patientName = fixMojibake(patientData.fullName || patientData.name || prescription.patientName || record?.patientName || '—')
  const patientCode = patientData.patientCode || patientData.code || prescription.patientCode || record?.patientCode || 'BN000001'
  const visitCode = encounter?.visit?.visitCode || encounter?.visitCode || record?.visitCode || record?.visitId || 'VIS000001'
  const prescriptionCode = prescription.prescriptionCode || 'RX000001'

  // Format diagnoses without [undefined]
  const formattedDiagnoses = (diagnoses || [])
    .map((d) => {
      const code = d.diagnosisCode || d.code || d.icd10Code || ''
      const name = fixMojibake(d.diagnosisName || d.name || '')
      if (code && name) return `[${code}] ${name}`
      return name || code || ''
    })
    .filter(Boolean)
    .join('; ') || (record?.diagnosis ? fixMojibake(record.diagnosis) : 'Chưa ghi nhận')

  // Doctor Name
  const doctorName = prescription.doctorName || doctorData.fullName || record?.doctorName || 'Dr. Nguyen Minh Anh'

  // Prescribed Date
  const prescribedDate = prescription.prescribedAt ? dayjs(prescription.prescribedAt) : dayjs()

  // Clinic config
  const clinicName = clinic?.clinicName || 'PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ'
  const clinicAddress = clinic?.address || '123 Đường Y Học, Quận 1, TP. Hồ Chí Minh'
  const clinicPhone = clinic?.phone || clinic?.hotline || '1900 8888'
  const clinicWebsite = clinic?.website || 'https://benhanso.com'
  const clinicLicense = clinic?.licenseNumber || '01234/BYT-GPHĐ'

  const handlePrint = () => {
    const printContent = printAreaRef.current
    if (!printContent) {
      window.print()
      return
    }

    const printFrame = document.createElement('iframe')
    printFrame.style.position = 'fixed'
    printFrame.style.right = '0'
    printFrame.style.bottom = '0'
    printFrame.style.width = '0'
    printFrame.style.height = '0'
    printFrame.style.border = '0'
    document.body.appendChild(printFrame)

    const frameDoc = printFrame.contentWindow.document
    frameDoc.open()
    frameDoc.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="utf-8" />
          <title>Đơn thuốc điện tử - ${prescriptionCode}</title>
          <style>
            @page {
              size: A4 portrait;
              margin: 10mm 15mm 10mm 15mm;
            }
            * {
              box-sizing: border-box;
              -webkit-print-color-adjust: exact !important;
              print-color-adjust: exact !important;
            }
            body {
              margin: 0;
              padding: 0;
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
              font-size: 13.5px;
              color: #0f172a;
              background: #fff;
            }
          </style>
        </head>
        <body>
          ${printContent.outerHTML}
        </body>
      </html>
    `)
    frameDoc.close()

    setTimeout(() => {
      try {
        printFrame.contentWindow.focus()
        printFrame.contentWindow.print()
      } catch {
        window.print()
      } finally {
        setTimeout(() => {
          if (document.body.contains(printFrame)) {
            document.body.removeChild(printFrame)
          }
        }, 1500)
      }
    }, 250)
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={780}
      style={{ top: 20 }}
      footer={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: 12, color: '#6b7280' }}>
            Khổ in chuẩn: A4 / A5 (Đơn thuốc điện tử liên thông quốc gia)
          </span>
          <Space>
            <Button onClick={onClose} icon={<CloseOutlined />}>
              Đóng
            </Button>
            <Button type="primary" icon={<PrinterOutlined />} onClick={handlePrint} size="large">
              In đơn thuốc
            </Button>
          </Space>
        </div>
      }
    >
      <style>{`
        @media print {
          body * {
            visibility: hidden !important;
          }
          #printable-prescription-sheet,
          #printable-prescription-sheet * {
            visibility: visible !important;
          }
          #printable-prescription-sheet {
            position: fixed !important;
            left: 0 !important;
            top: 0 !important;
            width: 100% !important;
            margin: 0 !important;
            padding: 20px 30px !important;
            background: #fff !important;
            box-shadow: none !important;
            z-index: 999999 !important;
          }
          .ant-modal-mask,
          .ant-modal-footer,
          .ant-modal-close,
          .ant-modal-header {
            display: none !important;
          }
        }
      `}</style>

      <Spin spinning={loadingClinic}>
        <div
          id="printable-prescription-sheet"
          ref={printAreaRef}
          style={{
            backgroundColor: '#ffffff',
            padding: '24px 32px',
            color: '#0f172a',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
            fontSize: '13.5px',
            lineHeight: 1.5,
          }}
        >
          {/* HEADER PHÒNG KHÁM & MÃ ĐƠN THUỐC ĐIỆN TỬ */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', paddingBottom: 12 }}>
            <div style={{ maxWidth: '62%' }}>
              <div style={{ fontSize: 16, fontWeight: 800, color: '#1e3a8a', textTransform: 'uppercase', letterSpacing: '0.3px', marginBottom: 3 }}>
                {clinicName}
              </div>
              <div style={{ fontSize: 12, color: '#475569', lineHeight: 1.4 }}>
                Địa chỉ: {clinicAddress} · Hotline: {clinicPhone}
              </div>
              <div style={{ fontSize: 12, color: '#475569', lineHeight: 1.4 }}>
                Website: {clinicWebsite} · Giấy phép số: {clinicLicense}
              </div>
            </div>

            <div style={{ textAlign: 'right' }}>
              <div
                style={{
                  border: '1px solid #bfdbfe',
                  backgroundColor: '#f0f9ff',
                  borderRadius: 8,
                  padding: '6px 14px',
                  display: 'inline-block',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: 11, fontWeight: 700, color: '#2563eb', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 2 }}>
                  MÃ ĐƠN THUỐC ĐIỆN TỬ
                </div>
                <BarcodeView code={prescriptionCode} />
              </div>
              <div style={{ fontSize: 11.5, color: '#64748b', marginTop: 4 }}>
                Ngày kê: {prescribedDate.format('HH:mm DD/MM/YYYY')}
              </div>
            </div>
          </div>

          {/* DIVIDER */}
          <div style={{ borderBottom: '2px solid #2563eb', marginBottom: 14 }}></div>

          {/* TIÊU ĐỀ ĐƠN THUỐC */}
          <div style={{ textAlign: 'center', marginBottom: 14 }}>
            <div style={{ fontSize: 21, fontWeight: 800, letterSpacing: '0.5px', textTransform: 'uppercase', color: '#0f172a' }}>
              ĐƠN THUỐC ĐIỆN TỬ
            </div>
            <div style={{ fontSize: 12.5, color: '#475569', fontStyle: 'italic', marginTop: 2 }}>
              (Định danh duy nhất - Phục vụ in ấn, tra cứu và liên thông quốc gia)
            </div>
          </div>

          {/* THÔNG TIN BỆNH NHÂN & LƯỢT KHÁM */}
          <div
            style={{
              border: '1px solid #e2e8f0',
              borderRadius: 8,
              padding: '10px 16px',
              backgroundColor: '#ffffff',
              marginBottom: 16,
              fontSize: '13px',
              lineHeight: 1.6,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3 }}>
              <div>
                Họ và tên bệnh nhân: <strong style={{ color: '#0f172a', fontSize: '13.5px' }}>{patientName}</strong>
              </div>
              <div>
                Mã bệnh nhân: <strong style={{ color: '#0f172a' }}>{patientCode}</strong>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3 }}>
              <div>
                Lượt khám / Hồ sơ: <strong style={{ color: '#0f172a' }}>{visitCode}</strong>
              </div>
              <div>
                Bác sĩ kê đơn: <strong style={{ color: '#0f172a' }}>{doctorName}</strong>
              </div>
            </div>

            <div>
              Chẩn đoán bệnh (ICD-10): <strong style={{ color: '#0f172a' }}>{formattedDiagnoses}</strong>
            </div>
          </div>

          {/* BẢNG DANH SÁCH THUỐC */}
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              marginBottom: 16,
              fontSize: '13px',
            }}
          >
            <thead>
              <tr style={{ backgroundColor: '#f8fafc' }}>
                <th
                  style={{
                    border: '1px solid #cbd5e1',
                    padding: '8px 6px',
                    width: 48,
                    textAlign: 'center',
                    fontWeight: 700,
                    color: '#334155',
                  }}
                >
                  STT
                </th>
                <th
                  style={{
                    border: '1px solid #cbd5e1',
                    padding: '8px 12px',
                    textAlign: 'left',
                    fontWeight: 700,
                    color: '#334155',
                  }}
                >
                  Tên thuốc / Hoạt chất / Hàm lượng
                </th>
                <th
                  style={{
                    border: '1px solid #cbd5e1',
                    padding: '8px 10px',
                    width: 100,
                    textAlign: 'center',
                    fontWeight: 700,
                    color: '#334155',
                  }}
                >
                  Số lượng
                </th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? (
                <tr>
                  <td colSpan={3} style={{ border: '1px solid #cbd5e1', textAlign: 'center', padding: '12px', fontStyle: 'italic', color: '#64748b' }}>
                    Không có thuốc trong đơn.
                  </td>
                </tr>
              ) : (
                items.map((item, index) => {
                  const routeName = ROUTE_LABELS[item.route] || item.route || 'Uống'
                  const freqText = item.frequency ? `${item.frequency} lần/ngày` : ''
                  const daysText = item.durationDays ? `Dùng trong ${item.durationDays} ngày` : ''

                  return (
                    <tr key={index}>
                      <td
                        style={{
                          border: '1px solid #cbd5e1',
                          padding: '8px 6px',
                          textAlign: 'center',
                          verticalAlign: 'middle',
                          fontWeight: 600,
                        }}
                      >
                        {index + 1}
                      </td>
                      <td
                        style={{
                          border: '1px solid #cbd5e1',
                          padding: '8px 12px',
                          verticalAlign: 'top',
                        }}
                      >
                        <div style={{ fontWeight: 700, fontSize: '13.5px', color: '#0f172a' }}>
                          {item.medicineName} {item.strength ? `(${item.strength})` : ''}
                        </div>
                        <div style={{ fontSize: '12.5px', color: '#475569', marginTop: 2 }}>
                          <strong>Cách dùng:</strong> {routeName} · <strong>Liều:</strong> {item.dosage || '1 viên'} · {freqText} · {daysText}
                        </div>
                        {item.instructions && (
                          <div style={{ fontSize: '12px', color: '#16a34a', fontStyle: 'italic', marginTop: 2 }}>
                            HD: {item.instructions}
                          </div>
                        )}
                      </td>
                      <td
                        style={{
                          border: '1px solid #cbd5e1',
                          padding: '8px 10px',
                          textAlign: 'center',
                          verticalAlign: 'middle',
                          fontWeight: 700,
                          fontSize: '13.5px',
                          color: '#0f172a',
                        }}
                      >
                        {item.quantity} {item.unit || 'viên'}
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>

          {/* CHÂN TRANG: LỜI DẶN & CHỮ KÝ BÁC SĨ */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              marginTop: 10,
              paddingTop: 8,
            }}
          >
            {/* Cột trái: Lời dặn & Lưu ý */}
            <div style={{ maxWidth: '56%', fontSize: '12px', color: '#475569', lineHeight: 1.6 }}>
              <div>• Đơn thuốc có giá trị trong vòng 05 ngày kể từ ngày kê đơn.</div>
              <div>• Khám lại theo lịch hẹn hoặc khi có triệu chứng bất thường.</div>
              {record?.doctorInstructions && (
                <div style={{ color: '#0f172a', marginTop: 2 }}>
                  • <strong>Lời dặn:</strong> {record.doctorInstructions}
                </div>
              )}
              {prescription.note && (
                <div style={{ color: '#0f172a', marginTop: 2 }}>
                  • <strong>Ghi chú:</strong> {prescription.note}
                </div>
              )}
            </div>

            {/* Cột phải: Ngày tháng & Chữ ký số */}
            <div style={{ textAlign: 'center', minWidth: 210 }}>
              <div style={{ fontStyle: 'italic', fontSize: '12.5px', color: '#334155' }}>
                Ngày {prescribedDate.format('DD')} tháng {prescribedDate.format('MM')} năm {prescribedDate.format('YYYY')}
              </div>
              <div style={{ fontWeight: 800, fontSize: '13px', textTransform: 'uppercase', color: '#0f172a', marginTop: 3 }}>
                BÁC SĨ KÊ ĐƠN
              </div>
              <div
                style={{
                  display: 'inline-block',
                  padding: '3px 12px',
                  backgroundColor: '#eff6ff',
                  border: '1px solid #93c5fd',
                  borderRadius: 4,
                  color: '#2563eb',
                  fontSize: '11px',
                  fontWeight: 600,
                  marginTop: 6,
                  marginBottom: 10,
                }}
              >
                Đã ký số điện tử
              </div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: '#0f172a' }}>
                {doctorName}
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </Modal>
  )
}

export default PrescriptionPrintTemplateModal
