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
    <div style={{ display: 'inline-flex', flexDirection: 'column', alignItems: 'center' }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', height: 36, gap: 1.5, padding: '0 4px' }}>
        {code.split('').map((char, idx) => {
          const val = char.charCodeAt(0)
          const widths = [1, 2, 1.5, 2.5, 1, 3]
          const heights = [28, 36, 32, 34, 30, 36]
          const w = widths[idx % widths.length]
          const h = heights[(val + idx) % heights.length]
          return (
            <React.Fragment key={idx}>
              <div style={{ width: w, height: h, backgroundColor: '#000' }} />
              <div style={{ width: 1.5, height: h }} />
            </React.Fragment>
          )
        })}
      </div>
      <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 1, marginTop: 2, fontFamily: 'monospace' }}>
        {code}
      </div>
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

  const patientName = fixMojibake(patientData.fullName || patientData.name || prescription.patientName || '—')
  const patientCode = patientData.patientCode || patientData.code || prescription.patientCode || ''
  const patientPhone = patientData.phone || patientData.phoneNumber || ''
  const patientAddress = patientData.address || ''
  const genderText = patientData.gender === 'FEMALE' ? 'Nữ' : patientData.gender === 'MALE' ? 'Nam' : ''

  // Age calculation
  let ageText = ''
  if (patientData.dateOfBirth) {
    const birth = dayjs(patientData.dateOfBirth)
    if (birth.isValid()) {
      const now = dayjs()
      const years = now.diff(birth, 'year')
      if (years < 6) {
        const months = now.diff(birth, 'month') % 12
        ageText = months > 0 ? `${years} tuổi ${months} tháng` : `${years} tuổi`
      } else {
        ageText = `${years} tuổi`
      }
    }
  }

  // Primary Diagnosis
  const primaryDiag = diagnoses.find((d) => d.diagnosisType === 'PRIMARY') || diagnoses[0]
  const diagCode = primaryDiag?.diagnosisCode || primaryDiag?.code || ''
  const diagName = fixMojibake(primaryDiag?.diagnosisName || primaryDiag?.name || '')
  const fullDiagnosis = diagCode && diagName ? `${diagCode} - ${diagName}` : diagName || diagCode || 'Chưa ghi nhận'

  // Doctor Name
  const doctorName = prescription.doctorName || doctorData.fullName || 'Bác sĩ điều trị'

  // Prescribed Date
  const prescribedDate = prescription.prescribedAt ? dayjs(prescription.prescribedAt) : dayjs()

  // Clinic config
  const clinicName = clinic?.clinicName || 'PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ'
  const clinicAddress = clinic?.address || '120 Nguyễn Xiển, Long Bình, TP. Thủ Đức'
  const clinicPhone = clinic?.phone || clinic?.hotline || '0962.831.327'
  const clinicOpening = clinic?.openingTime ? `${clinic.openingTime.substring(0, 5)} - ${clinic.closingTime ? clinic.closingTime.substring(0, 5) : '20:00'}` : '08:00 - 20:00'

  const handlePrint = () => {
    window.print()
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={720}
      style={{ top: 20 }}
      footer={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: 12, color: '#6b7280' }}>
            Khổ in: Chuẩn đơn thuốc y tế (A5 / A4)
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
          #printable-prescription-sheet, #printable-prescription-sheet * {
            visibility: visible !important;
          }
          #printable-prescription-sheet {
            position: absolute !important;
            left: 0 !important;
            top: 0 !important;
            width: 100% !important;
            margin: 0 !important;
            padding: 20px 30px !important;
            background: #fff !important;
            box-shadow: none !important;
          }
          .ant-modal-wrap, .ant-modal-mask, .ant-modal-footer, .ant-modal-close {
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
            color: '#000000',
            fontFamily: '"Times New Roman", Times, serif',
            fontSize: '14px',
            lineHeight: 1.45,
          }}
        >
          {/* HEADER PHÒNG KHÁM & BARCODE */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid #e5e7eb', paddingBottom: 12 }}>
            <div style={{ display: 'flex', gap: 14, alignItems: 'center', maxWidth: '65%' }}>
              <div
                style={{
                  width: 64,
                  height: 64,
                  borderRadius: '50%',
                  backgroundColor: '#eff6ff',
                  border: '1.5px solid #2563eb',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 28,
                  flexShrink: 0,
                }}
              >
                🩺
              </div>
              <div>
                <div style={{ fontSize: 15, fontWeight: 'bold', textTransform: 'uppercase', color: '#111827' }}>
                  {clinicName}
                </div>
                <div style={{ fontSize: 12.5, color: '#374151', marginTop: 2 }}>
                  Địa chỉ: {clinicAddress}
                </div>
                <div style={{ fontSize: 12.5, color: '#374151' }}>
                  SĐT / Zalo đặt lịch: <strong>{clinicPhone}</strong>
                </div>
                <div style={{ fontSize: 12, color: '#4b5563' }}>
                  Giờ làm việc: {clinicOpening}
                </div>
              </div>
            </div>

            <div style={{ textAlign: 'right', flexShrink: 0 }}>
              <BarcodeView code={patientCode || prescription.prescriptionCode || 'DT000001'} />
            </div>
          </div>

          {/* TIÊU ĐỀ ĐƠN THUỐC */}
          <div style={{ textAlign: 'center', margin: '14px 0 12px' }}>
            <div style={{ fontSize: 22, fontWeight: 'bold', letterSpacing: '0.5px' }}>
              ĐƠN THUỐC
            </div>
            {prescription.prescriptionCode && (
              <div style={{ fontSize: 12, color: '#4b5563', marginTop: 1 }}>
                Mã đơn: <strong>{prescription.prescriptionCode}</strong>
              </div>
            )}
          </div>

          {/* THÔNG TIN BỆNH NHÂN */}
          <div style={{ marginBottom: 14, fontSize: 13.5 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <div>
                Họ tên: <strong style={{ fontSize: 15, textTransform: 'uppercase' }}>{patientName}</strong>
              </div>
              <div>
                Tuổi: <strong>{ageText || '—'}</strong> {genderText ? `(${genderText})` : ''}
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <div style={{ maxWidth: '65%' }}>
                Địa chỉ: <span>{patientAddress || '—'}</span>
              </div>
              <div>
                Điện thoại: <strong>{patientPhone || '—'}</strong>
              </div>
            </div>

            <div style={{ marginBottom: 4 }}>
              Chẩn đoán: <strong style={{ color: '#111827' }}>{fullDiagnosis}</strong>
            </div>

            <div style={{ fontWeight: 'bold', marginTop: 8, fontStyle: 'italic', textDecoration: 'underline' }}>
              Điều trị:
            </div>
          </div>

          {/* DANH SÁCH THUỐC */}
          <div style={{ minHeight: 140, marginBottom: 16 }}>
            {items.length === 0 ? (
              <div style={{ fontStyle: 'italic', color: '#6b7280', padding: '10px 0' }}>
                Không có thuốc trong đơn.
              </div>
            ) : (
              items.map((item, index) => {
                const routeName = ROUTE_LABELS[item.route] || item.route || 'Uống'
                const freqText = item.frequency ? `${item.frequency} lần/ngày` : ''
                const daysText = item.durationDays ? `(Dùng trong ${item.durationDays} ngày)` : ''
                const usageDetails = [item.dosage, routeName, freqText, daysText].filter(Boolean).join(' - ')

                return (
                  <div key={index} style={{ marginBottom: 10, paddingBottom: 6 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                      <div style={{ fontSize: 14, fontWeight: 'bold' }}>
                        {index + 1}/ {item.medicineName} {item.strength ? `${item.strength}` : ''}
                      </div>
                      <div style={{ fontSize: 14, fontWeight: 'bold', whiteSpace: 'nowrap', marginLeft: 16 }}>
                        {String(item.quantity).padStart(2, '0')} {item.unit || 'Đơn vị'}
                      </div>
                    </div>

                    <div style={{ paddingLeft: 18, fontSize: 13, color: '#1f2937', marginTop: 1 }}>
                      {usageDetails && <span>{usageDetails}</span>}
                      {item.instructions && (
                        <div style={{ fontStyle: 'italic', color: '#374151', marginTop: 1 }}>
                          * Lời dặn: {item.instructions}
                        </div>
                      )}
                    </div>
                  </div>
                )
              })
            )}
          </div>

          {/* CHÂN TRANG / LỜI DẶN / CHỮ KÝ */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginTop: 20, paddingTop: 10, borderTop: '1px dashed #d1d5db' }}>
            {/* Cột trái: Dặn dò & Giờ khám */}
            <div style={{ maxWidth: '50%', fontSize: 12.5 }}>
              {record?.doctorInstructions && (
                <div style={{ marginBottom: 4 }}>
                  <em>Lời dặn:</em> <strong>{record.doctorInstructions}</strong>
                </div>
              )}
              {prescription.note && (
                <div style={{ marginBottom: 4 }}>
                  <em>Ghi chú đơn:</em> {prescription.note}
                </div>
              )}
              <div style={{ marginTop: 6, color: '#4b5563' }}>
                <div><strong>Giờ khám bệnh:</strong></div>
                <div>Thứ 2 - Thứ 6: 08h00 - 20h00</div>
                <div>Thứ 7, Chủ nhật: 08h00 - 17h00</div>
              </div>
            </div>

            {/* Cột phải: Ngày tháng & Ký tên bác sĩ */}
            <div style={{ textAlign: 'center', minWidth: 200, fontSize: 13 }}>
              <div style={{ fontStyle: 'italic', marginBottom: 4 }}>
                Ngày {prescribedDate.format('DD')} Tháng {prescribedDate.format('MM')} Năm {prescribedDate.format('YYYY')}
              </div>
              <div style={{ fontWeight: 'bold', textTransform: 'uppercase', marginBottom: 45 }}>
                Bác sĩ khám bệnh
              </div>
              <div style={{ fontWeight: 'bold', fontSize: 14 }}>
                {doctorName}
              </div>
            </div>
          </div>

          {/* LƯU Ý DƯỚI CÙNG */}
          <div style={{ marginTop: 16, paddingTop: 6, borderTop: '1px solid #000', textAlign: 'center', fontSize: 11.5, fontStyle: 'italic' }}>
            * Lưu ý: Tái khám nhớ mang theo toa thuốc, phim X-quang, xét nghiệm và hồ sơ bệnh án cũ.
          </div>
        </div>
      </Spin>
    </Modal>
  )
}

export default PrescriptionPrintTemplateModal
