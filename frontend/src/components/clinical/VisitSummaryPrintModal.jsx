import React, { useEffect, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Divider,
  Empty,
  Modal,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  DownloadOutlined,
  HistoryOutlined,
  MedicineBoxOutlined,
  PrinterOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import visitSummaryApi from '../../api/visitSummaryApi.js'
import {
  calculateAgeFromDob,
  formatDateTimeVi,
  formatDateVi,
  formatGenderVi,
  isMedicalRecordSignedForSummary,
} from '../../utils/visitSummaryHelpers.js'
import { getApiErrorMessage } from '../../utils/apiError.js'
import '../../styles/visitSummaryPrint.css'

const { Text, Title, Paragraph } = Typography

export default function VisitSummaryPrintModal({ open, visitId, onClose, onPrinted }) {
  const [loading, setLoading] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [summaryData, setSummaryData] = useState(null)
  const [errorMsg, setErrorMsg] = useState(null)

  useEffect(() => {
    if (!open || !visitId) {
      setSummaryData(null)
      setErrorMsg(null)
      return
    }

    const fetchSummary = async () => {
      setLoading(true)
      setErrorMsg(null)
      try {
        const res = await visitSummaryApi.getSummary(visitId)
        setSummaryData(res.data)
      } catch (err) {
        const backendCode = err?.response?.data?.code
        if (backendCode === 'MEDICAL_RECORD_NOT_SIGNED' || err?.response?.status === 400) {
          setErrorMsg(
            'Bệnh án của lượt khám chưa được ký. Vui lòng ký bệnh án trước khi in phiếu tóm tắt.'
          )
        } else if (err?.response?.status === 403) {
          setErrorMsg(
            'Bạn không có quyền xem hoặc in phiếu tóm tắt của lượt khám này (Chỉ bác sĩ phụ trách hoặc nhân viên được phân quyền mới có thể truy cập).'
          )
        } else {
          setErrorMsg(getApiErrorMessage(err, 'Không thể tải dữ liệu phiếu tóm tắt lượt khám.'))
        }
      } finally {
        setLoading(false)
      }
    }

    fetchSummary()
  }, [open, visitId])

  const handlePrint = () => {
    if (!summaryData) return
    window.print()
    if (onPrinted) {
      onPrinted()
    }
  }

  const handleDownloadPdf = async () => {
    if (!summaryData) return
    setDownloading(true)
    try {
      await visitSummaryApi.downloadPdf(
        summaryData.visitId,
        `phieu-tom-tat-${summaryData.visitCode || 'kham-benh'}.pdf`
      )
      message.success('Đã tải xuống tệp PDF phiếu tóm tắt thành công!')
      if (onPrinted) {
        onPrinted()
      }
    } catch (err) {
      message.error(getApiErrorMessage(err, 'Không thể tải tệp PDF phiếu tóm tắt.'))
    } finally {
      setDownloading(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={880}
      className="visit-summary-modal"
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <MedicineBoxOutlined style={{ color: '#0284c7', fontSize: 20 }} />
          <span style={{ fontSize: 16, fontWeight: 700 }}>
            Phiếu tóm tắt lượt khám bệnh nhân
          </span>
        </div>
      }
      footer={[
        <Button
          key="close"
          onClick={onClose}
          style={{ height: 42, minWidth: 100, borderRadius: 8, fontSize: 15 }}
        >
          Đóng
        </Button>,
        <Button
          key="download"
          icon={<DownloadOutlined />}
          loading={downloading}
          disabled={!summaryData || Boolean(errorMsg)}
          onClick={handleDownloadPdf}
          style={{
            height: 42,
            minWidth: 140,
            borderRadius: 8,
            fontSize: 15,
            borderColor: '#0284c7',
            color: '#0284c7',
          }}
        >
          Tải file PDF
        </Button>,
        <Button
          key="print"
          type="primary"
          icon={<PrinterOutlined />}
          disabled={!summaryData || Boolean(errorMsg)}
          onClick={handlePrint}
          style={{
            height: 42,
            minWidth: 140,
            borderRadius: 8,
            fontSize: 15,
            fontWeight: 600,
            background: '#0284c7',
          }}
        >
          In phiếu
        </Button>,
      ]}
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin size="large" tip="Đang tải dữ liệu phiếu tóm tắt lượt khám..." />
        </div>
      ) : errorMsg ? (
        <Alert
          type="warning"
          showIcon
          icon={<WarningOutlined style={{ fontSize: 22 }} />}
          message={
            <span style={{ fontWeight: 700, fontSize: 15 }}>Chưa thể in phiếu tóm tắt</span>
          }
          description={<div style={{ marginTop: 6, fontSize: 14 }}>{errorMsg}</div>}
          style={{ margin: '24px 0', borderRadius: 8, padding: 16 }}
        />
      ) : summaryData ? (
        <div className="visit-summary-sheet">
          {/* Thông tin phòng khám & Mã chứng từ */}
          <div className="visit-summary-header">
            <div className="visit-summary-clinic-info">
              <h2>{summaryData.clinic?.name || 'PHÒNG KHÁM ĐA KHOA'}</h2>
              <p>Địa chỉ: {summaryData.clinic?.address || 'Hệ thống Y tế Chuyển đổi số'}</p>
              <p>Điện thoại: {summaryData.clinic?.phone || '028 3844 5566'}</p>
            </div>
            <div className="visit-summary-doc-meta">
              <div>
                Mã lượt khám:{' '}
                <span className="doc-code">{summaryData.visitCode || '---'}</span>
              </div>
              <div>Thời gian khám: {formatDateTimeVi(summaryData.visitAt)}</div>
              <div style={{ marginTop: 4 }}>
                <Tag color="blue" icon={<SafetyCertificateOutlined />}>
                  Chứng từ chuyên môn
                </Tag>
              </div>
            </div>
          </div>

          {/* Tiêu đề chính của phiếu */}
          <div className="visit-summary-title">
            <h1>PHIẾU TÓM TẮT LƯỢT KHÁM BỆNH</h1>
            <div className="subtitle">
              (Cung cấp cho người bệnh sau khi kết thúc lượt khám và ký bệnh án điện tử)
            </div>
          </div>

          {/* Phần I: Thông tin hành chính bệnh nhân */}
          <div className="visit-summary-section">
            <div className="visit-summary-section-title">
              I. Thông tin hành chính bệnh nhân
            </div>
            <div className="visit-summary-grid">
              <div className="visit-summary-row">
                <span className="label">Họ và tên:</span>
                <span className="value" style={{ fontWeight: 700, textTransform: 'uppercase' }}>
                  {summaryData.patient?.fullName || '---'}
                </span>
              </div>
              <div className="visit-summary-row">
                <span className="label">Mã bệnh nhân:</span>
                <span className="value" style={{ fontFamily: 'monospace', fontWeight: 600 }}>
                  {summaryData.patient?.patientCode || '---'}
                </span>
              </div>
              <div className="visit-summary-row">
                <span className="label">Ngày sinh:</span>
                <span className="value">
                  {formatDateVi(summaryData.patient?.dateOfBirth)} (
                  {calculateAgeFromDob(summaryData.patient?.dateOfBirth)})
                </span>
              </div>
              <div className="visit-summary-row">
                <span className="label">Giới tính:</span>
                <span className="value">{formatGenderVi(summaryData.patient?.gender)}</span>
              </div>
              <div className="visit-summary-row">
                <span className="label">Số điện thoại:</span>
                <span className="value">{summaryData.patient?.phone || '---'}</span>
              </div>
              <div className="visit-summary-row">
                <span className="label">Số CCCD / CMND:</span>
                <span className="value">{summaryData.patient?.identityNumber || '---'}</span>
              </div>
            </div>
          </div>

          {/* Phần II: Thông tin chuyên môn & Chẩn đoán */}
          <div className="visit-summary-section">
            <div className="visit-summary-section-title">
              II. Thông tin chuyên môn & Chẩn đoán
            </div>
            <div className="visit-summary-row">
              <span className="label">Bác sĩ phụ trách:</span>
              <span className="value" style={{ fontWeight: 600 }}>
                {summaryData.doctor?.fullName || '---'}
              </span>
            </div>
            <div className="visit-summary-row" style={{ marginTop: 6 }}>
              <span className="label">Chẩn đoán xác định:</span>
              <div className="value">
                {summaryData.diagnoses && summaryData.diagnoses.length > 0 ? (
                  <ul style={{ margin: 0, paddingLeft: 18 }}>
                    {summaryData.diagnoses.map((diag, idx) => (
                      <li key={idx} style={{ marginBottom: 4 }}>
                        <span style={{ fontWeight: diag.isPrimary ? 700 : 500 }}>
                          [{diag.code}] {diag.name}
                        </span>
                        {diag.isPrimary && (
                          <Tag color="geekblue" style={{ marginLeft: 6, fontSize: 11 }}>
                            Chẩn đoán chính
                          </Tag>
                        )}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <Text type="secondary">Chưa ghi nhận chẩn đoán</Text>
                )}
              </div>
            </div>
          </div>

          {/* Phần III: Cận lâm sàng đã thực hiện */}
          <div className="visit-summary-section">
            <div className="visit-summary-section-title">
              III. Cận lâm sàng & Thăm dò chức năng đã thực hiện
            </div>
            {summaryData.clinicalOrders && summaryData.clinicalOrders.length > 0 ? (
              <table className="visit-summary-table">
                <thead>
                  <tr>
                    <th style={{ width: '15%' }}>Mã chỉ định</th>
                    <th style={{ width: '35%' }}>Tên dịch vụ cận lâm sàng</th>
                    <th style={{ width: '35%' }}>Chỉ dẫn / Ghi chú</th>
                    <th style={{ width: '15%' }}>Trạng thái</th>
                  </tr>
                </thead>
                <tbody>
                  {summaryData.clinicalOrders.map((ord, idx) => (
                    <tr key={idx}>
                      <td style={{ fontFamily: 'monospace' }}>{ord.orderCode || '---'}</td>
                      <td style={{ fontWeight: 600 }}>{ord.serviceName || ord.serviceCode}</td>
                      <td>{ord.instruction || '---'}</td>
                      <td>
                        <Tag color={ord.status === 'COMPLETED' ? 'green' : 'blue'}>
                          {ord.status === 'COMPLETED' ? 'Đã có kết quả' : ord.status || 'Đã thực hiện'}
                        </Tag>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div style={{ color: '#64748b', fontStyle: 'italic', padding: '4px 0' }}>
                Không có chỉ định cận lâm sàng trong lượt khám này.
              </div>
            )}
          </div>

          {/* Phần IV: Điều trị, Lời dặn & Mốc tái khám */}
          <div className="visit-summary-section">
            <div className="visit-summary-section-title">
              IV. Kế hoạch điều trị & Lời dặn dò của Bác sĩ
            </div>
            <div className="visit-summary-row">
              <span className="label">Kế hoạch điều trị:</span>
              <span className="value">{summaryData.treatmentPlan || 'Theo dõi và điều trị ngoại trú theo đơn thuốc'}</span>
            </div>
            <div className="visit-summary-row" style={{ marginTop: 6 }}>
              <span className="label">Lời dặn của Bác sĩ:</span>
              <span className="value" style={{ fontStyle: 'italic' }}>
                {summaryData.doctorInstructions || 'Uống thuốc đúng liều lượng, tái khám theo lịch hẹn hoặc khi có triệu chứng bất thường.'}
              </span>
            </div>

            {summaryData.revisitDate && (
              <div className="visit-summary-revisit-box">
                <CalendarOutlined style={{ fontSize: 24, color: '#15803d' }} />
                <div>
                  <div style={{ fontSize: 13, color: '#166534', fontWeight: 600 }}>
                    HẸN NGÀY TÁI KHÁM:
                  </div>
                  <div className="date-highlight">
                    {formatDateVi(summaryData.revisitDate)}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Phần V: Chữ ký bác sĩ & Xác nhận */}
          <div className="visit-summary-signatures">
            <div className="visit-summary-sign-col">
              <div className="role-title">NGƯỜI BỆNH / ĐẠI DIỆN</div>
              <div className="sign-hint">(Ký và ghi rõ họ tên)</div>
            </div>

            <div className="visit-summary-sign-col">
              <div className="role-title">BÁC SĨ KHÁM BỆNH</div>
              <div className="sign-hint">
                {summaryData.medicalRecord?.signedByName ? (
                  <div>
                    <div className="digital-stamp">
                      <CheckCircleFilled style={{ marginRight: 4 }} />
                      ĐÃ KÝ SỐ BỆNH ÁN ĐIỆN TỬ
                    </div>
                    <div className="sign-date">
                      Ký lúc: {formatDateTimeVi(summaryData.medicalRecord?.signedAt)}
                    </div>
                  </div>
                ) : (
                  '(Ký và ghi rõ họ tên)'
                )}
              </div>
              <div className="signer-name">
                {summaryData.medicalRecord?.signedByName || summaryData.doctor?.fullName || '---'}
              </div>
            </div>
          </div>

          {/* Phần VI: Lịch sử in phiếu */}
          {summaryData.printHistory && summaryData.printHistory.length > 0 && (
            <div className="visit-summary-history-box no-print">
              <div style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                <HistoryOutlined />
                <span>Nhật ký các lần in phiếu trước đây ({summaryData.printHistory.length} lần):</span>
              </div>
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                {summaryData.printHistory.map((h, i) => (
                  <li key={i}>
                    Người in: <strong>{h.printedByName || 'Nhân viên y tế'}</strong> lúc {formatDateTimeVi(h.printedAt)} - {h.detail || 'In phiếu tóm tắt lượt khám'}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      ) : (
        <Empty description="Không có dữ liệu lượt khám" style={{ margin: '40px 0' }} />
      )}
    </Modal>
  )
}
