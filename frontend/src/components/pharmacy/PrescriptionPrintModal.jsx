import React from 'react'
import { Modal, Button, Table, Typography, Tag, Space, Divider, message } from 'antd'
import {
  PrinterOutlined,
  DownloadOutlined,
  BarcodeOutlined,
  CheckCircleOutlined,
  SafetyCertificateOutlined,
  MedicineBoxOutlined,
  CopyOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import pharmacyApi from '../../api/pharmacyApi'
import { fixMojibake } from '../../utils/serviceCatalogValidation'

const { Title, Text, Paragraph } = Typography

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
  VAGINAL: 'Đặt âm đạo',
  OTHER: 'Cách dùng khác',
}

function PrescriptionPrintModal({
  open,
  onClose,
  prescription,
  medicines = [],
  patient,
  doctorName,
  diagnoses = [],
}) {
  const [downloadingServerPdf, setDownloadingServerPdf] = React.useState(false)

  if (!prescription) return null

  const items = prescription.items || []
  const prescCode = prescription.prescriptionCode || prescription.id || 'RX000001'
  const patientDisplayName = fixMojibake(prescription.patientName || patient?.fullName || patient?.name || 'Bệnh nhân')
  const patientCodeDisplay = prescription.patientCode || patient?.patientCode || 'BN000001'
  const doctorDisplayName = fixMojibake(prescription.doctorName || doctorName || 'Bác sĩ điều trị')
  const prescDate = prescription.prescribedAt ? dayjs(prescription.prescribedAt) : dayjs()

  const getMedicineInfo = (id, fallbackName) => {
    const found = medicines.find((m) => String(m.id) === String(id))
    return {
      name: fixMojibake(found?.medicineName || found?.name || fallbackName || 'Thuốc'),
      strength: found?.strength || '',
      activeIngredient: found?.activeIngredient || '',
      unit: found?.unit || 'viên',
    }
  }

  const handlePrint = () => {
    const printContent = document.getElementById('printable-prescription-sheet')
    if (!printContent) {
      window.print()
      return
    }

    const printWindow = window.open('', '_blank', 'width=900,height=1000')
    if (printWindow) {
      printWindow.document.write(`
        <!DOCTYPE html>
        <html>
          <head>
            <meta charset="utf-8" />
            <title>Đơn thuốc điện tử - ${prescCode}</title>
            <style>
              @page {
                size: A4 portrait;
                margin: 12mm 15mm;
              }
              * {
                box-sizing: border-box;
              }
              body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                color: #111827;
                margin: 0;
                padding: 16px;
                background: #ffffff;
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
              }
              table {
                width: 100%;
                border-collapse: collapse;
                margin: 16px 0;
              }
              th, td {
                border: 1px solid #cbd5e1;
                padding: 8px 10px;
                font-size: 13px;
                text-align: left;
                vertical-align: top;
              }
              th {
                background-color: #f1f5f9 !important;
                font-weight: 600;
                color: #334155;
              }
              .ant-typography {
                margin: 0;
              }
              .ant-tag {
                display: inline-block;
                padding: 2px 7px;
                font-size: 12px;
                border-radius: 4px;
                border: 1px solid #93c5fd;
                background-color: #eff6ff;
                color: #1e40af;
              }
            </style>
          </head>
          <body>
            ${printContent.innerHTML}
            <script>
              window.onload = function() {
                setTimeout(function() {
                  window.focus();
                  window.print();
                }, 200);
              };
            </script>
          </body>
        </html>
      `)
      printWindow.document.close()
    } else {
      window.print()
    }
  }

  const handleCopyCode = () => {
    if (prescCode) {
      navigator.clipboard?.writeText(prescCode)
      message.success(`Đã sao chép mã đơn thuốc điện tử: ${prescCode}`)
    }
  }

  const handleDownloadServerPdf = async () => {
    if (!prescription.id) return
    setDownloadingServerPdf(true)
    try {
      const response = await pharmacyApi.printPrescription(prescription.id)
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `prescription-${prescCode}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      message.success(`Đã tải file PDF đơn thuốc điện tử ${prescCode}.`)
    } catch (error) {
      let errMsg = 'Máy chủ chưa sẵn sàng xuất PDF lúc này. Bạn có thể sử dụng nút "In đơn thuốc" trên màn hình để in hoặc lưu PDF trực tiếp.'
      if (error?.response?.data instanceof Blob) {
        try {
          const text = await error.response.data.text()
          const parsed = JSON.parse(text)
          if (parsed.message) errMsg = parsed.message
        } catch {
        }
      }
      message.warning(errMsg)
    } finally {
      setDownloadingServerPdf(false)
    }
  }

  const columns = [
    {
      title: 'STT',
      key: 'stt',
      width: 50,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Tên thuốc / Hoạt chất / Hàm lượng',
      key: 'medicine',
      render: (_, item) => {
        const info = getMedicineInfo(item.medicineId, item.medicineName)
        return (
          <div>
            <Text strong style={{ color: '#1e40af', fontSize: 14 }}>
              {item.medicineName || info.name}
            </Text>
            {(item.strength || info.strength) && (
              <Text type="secondary" style={{ marginLeft: 6 }}>
                ({item.strength || info.strength})
              </Text>
            )}
            <div style={{ fontSize: 12, color: '#4b5563', marginTop: 2 }}>
              <strong>Cách dùng:</strong> {ROUTE_LABELS[item.route] || item.route || 'Uống'} · Liều:{' '}
              {item.dosage || '1 viên'} · {item.frequency ? `${item.frequency} lần/ngày` : ''} ·{' '}
              {item.durationDays ? `Dùng trong ${item.durationDays} ngày` : ''}
            </div>
            {item.instructions && (
              <div style={{ fontSize: 12, color: '#15803d', fontStyle: 'italic', marginTop: 2 }}>
                HD: {item.instructions}
              </div>
            )}
          </div>
        )
      },
    },
    {
      title: 'Số lượng',
      key: 'quantity',
      width: 100,
      align: 'right',
      render: (_, item) => {
        const info = getMedicineInfo(item.medicineId)
        return (
          <Text strong style={{ fontSize: 14 }}>
            {item.quantity} {item.unit || info.unit || 'viên'}
          </Text>
        )
      },
    },
  ]

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={820}
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingRight: 24 }}>
          <Space>
            <PrinterOutlined style={{ color: '#2563eb', fontSize: 18 }} />
            <span style={{ fontSize: 16, fontWeight: 600 }}>In Đơn Thuốc Điện Tử</span>
          </Space>
          <Tag color="blue" icon={<SafetyCertificateOutlined />}>
            Chuẩn liên thông Bộ Y Tế
          </Tag>
        </div>
      }
      footer={[
        <Button key="close" onClick={onClose}>
          Đóng
        </Button>,
        <Button
          key="download"
          icon={<DownloadOutlined />}
          loading={downloadingServerPdf}
          onClick={handleDownloadServerPdf}
        >
          Tải PDF từ máy chủ
        </Button>,
        <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={handlePrint}>
          In đơn thuốc
        </Button>,
      ]}
      style={{ top: 20 }}
    >
      <div
        id="printable-prescription-sheet"
        className="printable-prescription-container"
        style={{
          padding: '24px',
          background: '#ffffff',
          color: '#111827',
          borderRadius: 8,
          border: '1px solid #e2e8f0',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '2px solid #2563eb', paddingBottom: 12, marginBottom: 16 }}>
          <div>
            <Title level={4} style={{ margin: 0, color: '#1e3a8a', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ
            </Title>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Địa chỉ: 123 Đường Y Học, Quận 1, TP. Hồ Chí Minh · Hotline: 1900 8888
            </Text>
            <div style={{ fontSize: 12, color: '#6b7280' }}>
              Website: https://benhanso.com · Giấy phép số: 01234/BYT-GPHĐ
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ backgroundColor: '#eff6ff', border: '1px solid #bfdbfe', padding: '6px 12px', borderRadius: 6, display: 'inline-block' }}>
              <div style={{ fontSize: 11, color: '#1e40af', fontWeight: 600 }}>MÃ ĐƠN THUỐC ĐIỆN TỬ</div>
              <Text strong style={{ fontSize: 18, color: '#1e3a8a', letterSpacing: '1px' }}>
                <BarcodeOutlined style={{ marginRight: 6 }} />
                {prescCode}
              </Text>
            </div>
            <div style={{ fontSize: 12, color: '#6b7280', marginTop: 4 }}>
              Ngày kê: {prescDate.format('HH:mm DD/MM/YYYY')}
            </div>
          </div>
        </div>

        {/* Title */}
        <div style={{ textAlign: 'center', margin: '16px 0' }}>
          <Title level={3} style={{ margin: 0, color: '#1e293b', textTransform: 'uppercase', letterSpacing: '1px' }}>
            ĐƠN THUỐC ĐIỆN TỬ
          </Title>
          <Text type="secondary" style={{ fontSize: 13, fontStyle: 'italic' }}>
            (Định danh duy nhất - Phục vụ in ấn, tra cứu và liên thông quốc gia)
          </Text>
        </div>

        {/* Patient & Diagnosis Info */}
        <div style={{ background: '#f8fafc', padding: 14, borderRadius: 8, marginBottom: 16, border: '1px solid #e2e8f0' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '8px 16px', fontSize: 13 }}>
            <div>
              <Text type="secondary">Họ và tên bệnh nhân: </Text>
              <Text strong style={{ fontSize: 14, color: '#0f172a' }}>{patientDisplayName}</Text>
            </div>
            <div>
              <Text type="secondary">Mã bệnh nhân: </Text>
              <Text code strong>{patientCodeDisplay}</Text>
            </div>
            <div>
              <Text type="secondary">Lượt khám / Hồ sơ: </Text>
              <Text strong>{prescription.visitCode || '—'}</Text>
            </div>
            <div>
              <Text type="secondary">Bác sĩ kê đơn: </Text>
              <Text strong>{doctorDisplayName}</Text>
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <Text type="secondary">Chẩn đoán bệnh (ICD-10): </Text>
              <Text strong>
                {diagnoses.length > 0
                  ? diagnoses
                      .map((d) => {
                        const code = d.code || d.diagnosisCode || d.icdCode
                        const name = d.name || d.diagnosisName || ''
                        return code ? `[${code}] ${name}` : name
                      })
                      .filter(Boolean)
                      .join('; ')
                  : prescription.diagnosis || 'Theo hồ sơ bệnh án điện tử'}
              </Text>
            </div>
          </div>
        </div>

        {/* Medicines Table */}
        <Table
          size="small"
          rowKey={(r, idx) => r.id || r.medicineId || idx}
          columns={columns}
          dataSource={items}
          pagination={false}
          bordered
          style={{ marginBottom: 16 }}
        />

        {/* Doctor Note */}
        {prescription.note && (
          <div style={{ marginBottom: 16, padding: '10px 14px', backgroundColor: '#fffbeb', borderRadius: 6, border: '1px solid #fef3c7' }}>
            <Text strong style={{ color: '#b45309' }}>Lời dặn của bác sĩ: </Text>
            <Text style={{ color: '#92400e' }}>{fixMojibake(prescription.note)}</Text>
          </div>
        )}

        {/* Footer & Signatures */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginTop: 24, paddingRight: 24, paddingLeft: 12 }}>
          <div>
            <div style={{ fontSize: 12, color: '#6b7280', maxWidth: 300 }}>
              • Đơn thuốc có giá trị trong vòng 05 ngày kể từ ngày kê đơn.<br />
              • Khám lại theo lịch hẹn hoặc khi có triệu chứng bất thường.
            </div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <Text style={{ fontSize: 13, fontStyle: 'italic', display: 'block', marginBottom: 4 }}>
              Ngày {prescDate.format('DD')} tháng {prescDate.format('MM')} năm {prescDate.format('YYYY')}
            </Text>
            <Text strong style={{ fontSize: 14, textTransform: 'uppercase', color: '#1e3a8a' }}>
              BÁC SĨ KÊ ĐƠN
            </Text>
            <div style={{ height: 50, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Tag color="cyan" style={{ fontStyle: 'italic' }}>Đã ký số điện tử</Tag>
            </div>
            <Text strong style={{ fontSize: 14 }}>{doctorDisplayName}</Text>
          </div>
        </div>
      </div>
    </Modal>
  )
}

export default PrescriptionPrintModal
