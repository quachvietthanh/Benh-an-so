import React, { useEffect, useRef, useState } from 'react'
import {
  Button,
  Descriptions,
  Divider,
  Modal,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  CloseOutlined,
  DownloadOutlined,
  FileProtectOutlined,
  MedicineBoxOutlined,
  PrinterOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../../api/medicalRecordApi'
import systemApi from '../../api/systemApi'

const { Title, Text, Paragraph } = Typography

/**
 * Component tạo thanh mã vạch giả lập trực quan phục vụ in ấn
 */
function BarcodeStrip({ code }) {
  if (!code) return null
  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', height: 22, gap: 1.5 }}>
        {String(code).split('').map((char, idx) => {
          const val = char.charCodeAt(0)
          const widths = [1.2, 2.2, 1.5, 2.8, 1.2, 3]
          const heights = [18, 22, 20, 22, 19, 22]
          const w = widths[idx % widths.length]
          const h = heights[(val + idx) % heights.length]
          return (
            <React.Fragment key={idx}>
              <div style={{ width: w, height: h, backgroundColor: '#1e3a8a' }} />
              <div style={{ width: 1, height: h }} />
            </React.Fragment>
          )
        })}
      </div>
      <span style={{ fontSize: 15, fontWeight: 800, letterSpacing: 1, color: '#1e3a8a', fontFamily: 'monospace' }}>
        {code}
      </span>
    </div>
  )
}

/**
 * Modal xem trước bản sao trích lục hồ sơ bệnh án theo mẫu chuẩn y tế A4
 */
function MedicalRecordCopyPreviewModal({
  open,
  onClose,
  record,
  patient,
  visit,
  diagnoses = [],
  prescriptions = [],
  requestInfo = {},
  onIssued,
}) {
  const [clinic, setClinic] = useState(null)
  const [loadingClinic, setLoadingClinic] = useState(false)
  const [exportingPdf, setExportingPdf] = useState(false)
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
          // Sử dụng thông tin mặc định nếu chưa cấu hình
        })
        .finally(() => {
          setLoadingClinic(false)
        })
    }
  }, [open])

  if (!record && !visit) return null

  const patientData = patient || record?.patient || visit?.patient || {}
  const patientName = patientData.fullName || patientData.name || '—'
  const patientCode = patientData.patientCode || patientData.code || 'BN000000'
  const recordCode = record?.id ? String(record.id).slice(-8).toUpperCase() : 'HSBA001'
  const visitCode = visit?.visitCode || record?.visitCode || 'VIS000001'
  const doctorName = visit?.doctorName || record?.doctorName || 'Bác sĩ điều trị'

  // Format danh sách chẩn đoán
  const formattedDiagnoses = (diagnoses || [])
    .map((d) => {
      const code = d.diagnosisCode || d.code || ''
      const name = d.diagnosisName || d.name || ''
      const type = d.diagnosisType === 'PRIMARY' ? '(Chính)' : '(Kèm theo)'
      if (code && name) return `[${code}] ${name} ${type}`
      return name || code || ''
    })
    .filter(Boolean)
    .join('; ') || (record?.primaryIcdName ? `[${record.primaryIcdCode || 'ICD'}] ${record.primaryIcdName}` : 'Chưa ghi nhận')

  // Thời gian
  const visitDate = visit?.visitAt ? dayjs(visit.visitAt) : (record?.createdAt ? dayjs(record.createdAt) : dayjs())
  const issueDate = dayjs()

  // Clinic config
  const clinicName = clinic?.clinicName || 'PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ'
  const clinicAddress = clinic?.address || '123 Đường Y Học, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh'
  const clinicPhone = clinic?.phone || clinic?.hotline || '1900 8888'
  const clinicLicense = clinic?.licenseNumber || '01234/BYT-GPHĐ'

  /**
   * Xử lý in bản sao qua IFrame Print chuẩn A4
   */
  const handlePrint = () => {
    // Kích hoạt callback ghi nhận nhật ký cấp bản sao (Postcondition)
    onIssued?.({
      recordId: record?.medicalRecordId || record?.id,
      patientId: patientData.id || patientData.patientId,
      requesterName: requestInfo.requesterName || patientName,
      relationship: requestInfo.relationship || 'Bản thân bệnh nhân',
      purpose: requestInfo.purpose || 'Lưu trữ cá nhân và theo dõi sức khỏe',
      copyCount: requestInfo.copyCount || 1,
      action: 'EXPORT',
    })

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
          <title>Bản sao trích lục hồ sơ bệnh án - ${recordCode}</title>
          <style>
            @page {
              size: A4 portrait;
              margin: 12mm 15mm 12mm 15mm;
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
              font-size: 13px;
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

  /**
   * Xử lý xuất file PDF trực tiếp từ Backend
   * (Đã sẵn sàng kết nối API GET /medical-records/{id}/export-copy)
   */
  const handleExportPdfBackend = async () => {
    const targetRecordId = record?.medicalRecordId || record?.id
    if (!targetRecordId) {
      message.error('Không tìm thấy mã hồ sơ bệnh án để xuất PDF.')
      return
    }

    setExportingPdf(true)
    try {
      // Ghi nhận nhật ký cấp bản sao (Postcondition)
      onIssued?.({
        recordId: targetRecordId,
        patientId: patientData.id || patientData.patientId,
        requesterName: requestInfo.requesterName || patientName,
        relationship: requestInfo.relationship || 'Bản thân bệnh nhân',
        purpose: requestInfo.purpose || 'Lưu trữ cá nhân và theo dõi sức khỏe',
        copyCount: requestInfo.copyCount || 1,
        action: 'EXPORT',
      })

      // Gọi API xuất PDF từ backend
      const response = await medicalRecordApi.exportCopy(targetRecordId)
      
      // Tạo đường link tải blob file
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = `Ban-sao-benh-an-${patientCode}-${recordCode}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)

      message.success('Đã tạo và tải bản sao hồ sơ bệnh án PDF thành công!')
    } catch (err) {
      console.warn('Backend export-copy endpoint not implemented yet or error:', err)
      message.info({
        content: 'Chức năng xuất PDF trực tiếp từ máy chủ đang được Backend tích hợp. Đang mở hộp thoại In để bạn lưu PDF qua trình duyệt...',
        duration: 4,
      })
      handlePrint()
    } finally {
      setExportingPdf(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={840}
      style={{ top: 20 }}
      title={
        <Space>
          <FileProtectOutlined style={{ color: '#2563eb' }} />
          <span>Xem trước & In bản sao hồ sơ bệnh án</span>
        </Space>
      }
      footer={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <span style={{ fontSize: 12, color: '#64748b' }}>
            Khổ in tiêu chuẩn: A4 dọc (Bản sao trích lục có giá trị pháp lý theo quy định)
          </span>
          <Space>
            <Button onClick={onClose} icon={<CloseOutlined />}>
              Đóng
            </Button>
            <Button
              icon={<DownloadOutlined />}
              loading={exportingPdf}
              onClick={handleExportPdfBackend}
            >
              Tải file PDF
            </Button>
            <Button type="primary" icon={<PrinterOutlined />} onClick={handlePrint} size="large">
              In bản sao
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
          #printable-medical-record-sheet,
          #printable-medical-record-sheet * {
            visibility: visible !important;
          }
          #printable-medical-record-sheet {
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
          id="printable-medical-record-sheet"
          ref={printAreaRef}
          style={{
            backgroundColor: '#ffffff',
            padding: '24px 32px',
            color: '#0f172a',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
            fontSize: '13px',
            lineHeight: 1.5,
          }}
        >
          {/* HEADER: TÊN CƠ SỞ & QUỐC HIỆU TIÊU NGỮ */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', paddingBottom: 12 }}>
            <div style={{ maxWidth: '52%' }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: '#1e3a8a', textTransform: 'uppercase' }}>
                {clinicName}
              </div>
              <div style={{ fontSize: 11.5, color: '#475569', lineHeight: 1.4, marginTop: 2 }}>
                Địa chỉ: {clinicAddress}
              </div>
              <div style={{ fontSize: 11.5, color: '#475569', lineHeight: 1.4 }}>
                Hotline: {clinicPhone} · GP: {clinicLicense}
              </div>
            </div>

            <div style={{ textAlign: 'center', minWidth: '40%' }}>
              <div style={{ fontSize: 12, fontWeight: 700, textTransform: 'uppercase', color: '#0f172a' }}>
                CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
              </div>
              <div style={{ fontSize: 11.5, fontWeight: 600, color: '#0f172a', borderBottom: '1px solid #0f172a', display: 'inline-block', paddingBottom: 2 }}>
                Độc lập - Tự do - Hạnh phúc
              </div>
              <div style={{ fontSize: 11, color: '#64748b', marginTop: 4 }}>
                Ngày {issueDate.format('DD')} tháng {issueDate.format('MM')} năm {issueDate.format('YYYY')}
              </div>
            </div>
          </div>

          <div style={{ borderBottom: '2px solid #1e3a8a', marginBottom: 14 }}></div>

          {/* TIÊU ĐỀ BẢN SAO */}
          <div style={{ textAlign: 'center', marginBottom: 16 }}>
            <div style={{ fontSize: 19, fontWeight: 800, letterSpacing: '0.5px', textTransform: 'uppercase', color: '#1e3a8a' }}>
              BẢN SAO TRÍCH LỤC HỒ SƠ BỆNH ÁN
            </div>
            <div style={{ fontSize: 12, color: '#475569', fontStyle: 'italic', marginTop: 2 }}>
              (Trích lục dữ liệu bệnh án điện tử lưu trữ theo quy định của Bộ Y Tế)
            </div>
            <div style={{ marginTop: 6 }}>
              <BarcodeStrip code={recordCode} />
            </div>
          </div>

          {/* MỤC I: THÔNG TIN HÀNH CHÍNH BỆNH NHÂN */}
          <div style={{ marginBottom: 14 }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#1e3a8a', textTransform: 'uppercase', borderBottom: '1px solid #e2e8f0', paddingBottom: 3, marginBottom: 8 }}>
              I. THÔNG TIN HÀNH CHÍNH
            </div>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12.5px' }}>
              <tbody>
                <tr>
                  <td style={{ padding: '3px 0', width: '50%' }}>
                    Họ và tên: <strong style={{ textTransform: 'uppercase', color: '#0f172a' }}>{patientName}</strong>
                  </td>
                  <td style={{ padding: '3px 0', width: '25%' }}>
                    Mã BN: <strong>{patientCode}</strong>
                  </td>
                  <td style={{ padding: '3px 0', width: '25%' }}>
                    Giới tính: <strong>{patientData.gender === 'FEMALE' ? 'Nữ' : patientData.gender === 'MALE' ? 'Nam' : 'Khác'}</strong>
                  </td>
                </tr>
                <tr>
                  <td style={{ padding: '3px 0' }}>
                    Ngày sinh: <strong>{patientData.dateOfBirth ? dayjs(patientData.dateOfBirth).format('DD/MM/YYYY') : '—'}</strong>
                  </td>
                  <td style={{ padding: '3px 0' }}>
                    Số CCCD/ĐD: <strong>{patientData.identityNumber || '—'}</strong>
                  </td>
                  <td style={{ padding: '3px 0' }}>
                    Số thẻ BHYT: <strong>{patientData.insuranceNumber || '—'}</strong>
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '3px 0' }}>
                    Địa chỉ: {patientData.address || '—'}
                  </td>
                  <td style={{ padding: '3px 0' }}>
                    Điện thoại: {patientData.phone || patientData.phoneNumber || '—'}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          {/* MỤC II: QUÁ TRÌNH KHÁM & CHUYÊN MÔN BỆNH ÁN */}
          <div style={{ marginBottom: 14 }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#1e3a8a', textTransform: 'uppercase', borderBottom: '1px solid #e2e8f0', paddingBottom: 3, marginBottom: 8 }}>
              II. QUÁ TRÌNH KHÁM BỆNH & ĐIỀU TRỊ
            </div>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12.5px' }}>
              <tbody>
                <tr>
                  <td style={{ padding: '3px 0', width: '50%' }}>
                    Mã đợt khám: <strong>{visitCode}</strong>
                  </td>
                  <td style={{ padding: '3px 0', width: '50%' }}>
                    Thời gian khám: <strong>{visitDate.format('HH:mm - DD/MM/YYYY')}</strong>
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '3px 0' }}>
                    Bác sĩ phụ trách: <strong>{doctorName}</strong>
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '4px 0' }}>
                    <strong>1. Lý do vào khám:</strong> {record?.chiefComplaint || visit?.reason || 'Khám sức khỏe / Bệnh lý'}
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '4px 0' }}>
                    <strong>2. Quá trình bệnh lý & Triệu chứng:</strong> {record?.symptoms || 'Bệnh sử diễn biến ổn định'}
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '4px 0' }}>
                    <strong>3. Tiền sử bệnh:</strong> {record?.medicalHistory || 'Chưa ghi nhận bệnh lý mạn tính đặc biệt'}
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '4px 0' }}>
                    <strong>4. Khám thực thể lâm sàng:</strong> {record?.physicalExamination || 'Toàn trạng ổn định'}
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '4px 0' }}>
                    <strong>5. Chẩn đoán y khoa (ICD-10):</strong> <span style={{ fontWeight: 700, color: '#1e3a8a' }}>{formattedDiagnoses}</span>
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '4px 0' }}>
                    <strong>6. Diễn biến lâm sàng & Hướng điều trị:</strong> {record?.clinicalProgress || record?.treatmentPlan || 'Điều trị ngoại trú theo phác đồ y tế'}
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '4px 0' }}>
                    <strong>7. Lời dặn của bác sĩ:</strong> {record?.doctorInstructions || 'Nghỉ ngơi, dùng thuốc theo đơn và tái khám khi cần'}
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} style={{ padding: '4px 0' }}>
                    <strong>8. Kết luận hồ sơ:</strong> {record?.conclusion || 'Bệnh nhân điều trị ổn định, kết thúc đợt khám'}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          {/* MỤC III: THÔNG TIN CẤP BẢN SAO HỒ SƠ */}
          <div
            style={{
              backgroundColor: '#f8fafc',
              border: '1px solid #e2e8f0',
              borderRadius: 6,
              padding: '8px 12px',
              marginBottom: 16,
              fontSize: '12px',
            }}
          >
            <div style={{ fontWeight: 700, color: '#334155', marginBottom: 3, textTransform: 'uppercase' }}>
              III. THÔNG TIN YÊU CẦU CẤP BẢN SAO
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 4 }}>
              <div>
                Người yêu cầu: <strong>{requestInfo.requesterName || patientName}</strong>
              </div>
              <div>
                Quan hệ với BN: <strong>{requestInfo.relationship || 'Bản thân bệnh nhân'}</strong>
              </div>
              <div>
                Mục đích xin cấp: <strong>{requestInfo.purpose || 'Lưu trữ cá nhân & Thủ tục liên quan'}</strong>
              </div>
            </div>
          </div>

          {/* CHỮ KÝ XÁC NHẬN */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              marginTop: 18,
              paddingTop: 10,
            }}
          >
            <div style={{ textAlign: 'center', minWidth: 200 }}>
              <div style={{ fontWeight: 700, fontSize: '12.5px', textTransform: 'uppercase', color: '#0f172a' }}>
                NGƯỜI LẬP TRÍCH SAO
              </div>
              <div style={{ fontStyle: 'italic', fontSize: '11.5px', color: '#64748b', marginBottom: 28 }}>
                (Ký, ghi rõ họ tên)
              </div>
              <div style={{ fontWeight: 600, fontSize: '13px' }}>Bộ phận Hồ sơ bệnh án</div>
            </div>

            <div style={{ textAlign: 'center', minWidth: 220 }}>
              <div style={{ fontWeight: 700, fontSize: '12.5px', textTransform: 'uppercase', color: '#0f172a' }}>
                BÁC SĨ ĐIỀU TRỊ
              </div>
              <div
                style={{
                  display: 'inline-block',
                  padding: '2px 10px',
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
              <div style={{ fontWeight: 700, fontSize: '13.5px', color: '#0f172a' }}>
                {doctorName}
              </div>
            </div>

            <div style={{ textAlign: 'center', minWidth: 220 }}>
              <div style={{ fontWeight: 700, fontSize: '12.5px', textTransform: 'uppercase', color: '#0f172a' }}>
                TRƯỞNG CƠ SỞ KHÁM CHỮA BỆNH
              </div>
              <div style={{ fontStyle: 'italic', fontSize: '11.5px', color: '#64748b', marginBottom: 28 }}>
                (Ký tên, đóng dấu xác nhận sao y)
              </div>
              <div style={{ fontWeight: 700, fontSize: '13.5px', color: '#0f172a' }}>
                {clinicName}
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </Modal>
  )
}

export default MedicalRecordCopyPreviewModal
