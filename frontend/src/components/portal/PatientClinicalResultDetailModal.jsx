import React, { useEffect, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  Modal,
  Row,
  Skeleton,
  Space,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  DownloadOutlined,
  ExperimentOutlined,
  FileDoneOutlined,
  FilePdfOutlined,
  InfoCircleOutlined,
  MedicineBoxOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'

import patientPortalClinicalResultApi from '../../api/patientPortalClinicalResultApi'
import {
  calculateValuePercentage,
  downloadPdfBlob,
  formatDate,
  formatDateTime,
  getAbnormalFlagInfo,
  getSecuritySafeErrorMessage,
  getServiceCategory,
  isPostVisitResult,
} from '../../utils/patientClinicalResultHelpers'

const { Title, Text, Paragraph } = Typography

function PatientClinicalResultDetailModal({
  open,
  onClose,
  resultId,
  initialSummary = null,
}) {
  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!open || !resultId) {
      setDetail(null)
      setError('')
      return
    }

    let isMounted = true
    const fetchDetail = async () => {
      setLoading(true)
      setError('')
      try {
        const res = await patientPortalClinicalResultApi.getClinicalResultDetail(resultId)
        if (isMounted) {
          // Bảo vệ nghiệp vụ: Chỉ chấp nhận hiển thị kết quả FINAL
          if (res.data?.status === 'FINAL') {
            setDetail(res.data)
          } else {
            setError('Kết quả này đang trong quá trình xét duyệt của bác sĩ và chưa được công bố.')
            setDetail(null)
          }
        }
      } catch (err) {
        if (isMounted) {
          const safeMsg = getSecuritySafeErrorMessage(err)
          setError(safeMsg)
          setDetail(null)
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
  }, [open, resultId])

  const currentData = detail || initialSummary
  const serviceName = currentData?.serviceName || 'Chi tiết kết quả cận lâm sàng'
  const serviceCode = currentData?.serviceCode || ''
  const category = getServiceCategory(serviceCode, serviceName)
  const abnormalInfo = getAbnormalFlagInfo(currentData?.abnormalFlag)
  const isPostVisit = isPostVisitResult(currentData?.enteredAt, currentData?.visitAt)

  const handleDownloadPdf = async () => {
    if (!resultId) return
    setDownloading(true)
    try {
      const res = await patientPortalClinicalResultApi.downloadResultPdf(resultId)
      const filename = `ket-qua-${serviceCode || 'CLS'}-${formatDate(currentData?.enteredAt)}.pdf`
      downloadPdfBlob(res.data, filename)
      message.success('Đã tải phiếu kết quả cận lâm sàng!')
    } catch (err) {
      const msg = getSecuritySafeErrorMessage(err)
      message.error(msg)
    } finally {
      setDownloading(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={720}
      centered
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, paddingRight: 24 }}>
          <div
            style={{
              width: 38,
              height: 38,
              borderRadius: 10,
              background: '#eff6ff',
              color: '#2563eb',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20,
              flexShrink: 0,
            }}
          >
            <ExperimentOutlined />
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
              <span style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
                {serviceName}
              </span>
              <Tag color={category.color} style={{ margin: 0, fontSize: 11, fontWeight: 600 }}>
                {category.label}
              </Tag>
              {serviceCode && (
                <Tag color="default" style={{ margin: 0, fontSize: 11 }}>
                  Mã: {serviceCode}
                </Tag>
              )}
            </div>
            <div style={{ fontSize: 12.5, color: '#64748b', marginTop: 2 }}>
              Phiếu kết quả cận lâm sàng điện tử đã được xác nhận chính thức
            </div>
          </div>
        </div>
      }
      footer={[
        <Button key="close" onClick={onClose} style={{ borderRadius: 8 }}>
          Đóng
        </Button>,
        <Button
          key="download"
          type="primary"
          icon={<DownloadOutlined />}
          loading={downloading}
          onClick={handleDownloadPdf}
          disabled={!currentData || !!error}
          style={{
            background: '#2563eb',
            borderColor: '#2563eb',
            borderRadius: 8,
            fontWeight: 600,
          }}
        >
          Tải bản đọc được (PDF)
        </Button>,
      ]}
      styles={{
        body: { maxHeight: '72vh', overflowY: 'auto', padding: '16px 20px 24px' },
      }}
    >
      {loading ? (
        <div style={{ padding: '20px 0' }}>
          <Skeleton active paragraph={{ rows: 6 }} />
        </div>
      ) : error ? (
        <Alert
          type="warning"
          showIcon
          message="Thông báo từ hệ thống"
          description={error}
          style={{ borderRadius: 10, marginTop: 12 }}
        />
      ) : currentData ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          {/* Banner trạng thái kết quả */}
          <div
            style={{
              background: abnormalInfo.bgLight,
              border: `1px solid ${abnormalInfo.borderLight}`,
              borderRadius: 12,
              padding: '14px 18px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: 12,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div
                style={{
                  width: 34,
                  height: 34,
                  borderRadius: '50%',
                  background: abnormalInfo.badgeColor,
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 16,
                }}
              >
                {abnormalInfo.isAbnormal ? <WarningOutlined /> : <CheckCircleOutlined />}
              </div>
              <div>
                <div style={{ fontSize: 14, fontWeight: 700, color: abnormalInfo.textColor }}>
                  Đánh giá chỉ số: {abnormalInfo.text}
                </div>
                <div style={{ fontSize: 12.5, color: '#475569' }}>
                  Xác nhận bởi Bác sĩ chuyên môn • Trạng thái hoàn tất (FINAL)
                </div>
              </div>
            </div>

            <Space size={6} wrap>
              {isPostVisit && (
                <Tag color="orange" style={{ fontWeight: 600, borderRadius: 6, margin: 0 }}>
                  <ClockCircleOutlined style={{ marginRight: 4 }} />
                  Kết quả trả sau
                </Tag>
              )}
              <Tag
                color="green"
                icon={<SafetyCertificateOutlined />}
                style={{ fontWeight: 600, borderRadius: 6, margin: 0 }}
              >
                Đã duyệt chính thức
              </Tag>
            </Space>
          </div>

          {/* Khung hiển thị chỉ số đo đạc */}
          {currentData.resultType === 'NUMBER' ? (
            <Card
              size="small"
              style={{
                borderRadius: 12,
                border: '1px solid #e2e8f0',
                boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
              }}
            >
              <div style={{ fontSize: 13, fontWeight: 600, color: '#64748b', marginBottom: 12 }}>
                KẾT QUẢ ĐO ĐẠC & NGƯỠNG THAM CHIẾU
              </div>

              <Row gutter={[16, 16]} align="middle">
                <Col xs={24} sm={10}>
                  <div
                    style={{
                      background: '#f8fafc',
                      border: '1px solid #e2e8f0',
                      borderRadius: 10,
                      padding: '16px',
                      textAlign: 'center',
                    }}
                  >
                    <div style={{ fontSize: 12, color: '#64748b', marginBottom: 4 }}>
                      Giá trị đo được
                    </div>
                    <div
                      style={{
                        fontSize: 28,
                        fontWeight: 800,
                        color: abnormalInfo.textColor,
                        lineHeight: 1.2,
                      }}
                    >
                      {currentData.numericValue != null ? currentData.numericValue : '—'}
                      <span style={{ fontSize: 14, fontWeight: 500, color: '#64748b', marginLeft: 6 }}>
                        {currentData.unit || ''}
                      </span>
                    </div>
                    <div style={{ marginTop: 6 }}>
                      <Tag color={abnormalInfo.color} style={{ fontWeight: 600, borderRadius: 6 }}>
                        {abnormalInfo.text}
                      </Tag>
                    </div>
                  </div>
                </Col>

                <Col xs={24} sm={14}>
                  <div style={{ padding: '0 8px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 6 }}>
                      <span style={{ color: '#64748b' }}>Khoảng tham chiếu chuẩn:</span>
                      <strong style={{ color: '#0f172a' }}>
                        {currentData.referenceRange ||
                          `${currentData.lowerBound ?? '—'} - ${currentData.upperBound ?? '—'}`}{' '}
                        {currentData.unit || ''}
                      </strong>
                    </div>

                    {/* Visual Gauge Bar */}
                    <div style={{ margin: '14px 0 10px' }}>
                      <div
                        style={{
                          height: 12,
                          background: '#e2e8f0',
                          borderRadius: 6,
                          position: 'relative',
                          overflow: 'visible',
                        }}
                      >
                        {/* Safe zone green area */}
                        <div
                          style={{
                            position: 'absolute',
                            left: '25%',
                            width: '50%',
                            height: '100%',
                            background: '#86efac',
                            borderRadius: 4,
                          }}
                        />
                        {/* Pointer to current value */}
                        <div
                          style={{
                            position: 'absolute',
                            left: `${calculateValuePercentage(
                              currentData.numericValue,
                              currentData.lowerBound,
                              currentData.upperBound
                            )}%`,
                            top: -4,
                            transform: 'translateX(-50%)',
                            width: 20,
                            height: 20,
                            borderRadius: '50%',
                            background: abnormalInfo.badgeColor,
                            border: '3px solid #ffffff',
                            boxShadow: '0 2px 5px rgba(0,0,0,0.25)',
                          }}
                        />
                      </div>
                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          fontSize: 11.5,
                          color: '#64748b',
                          marginTop: 6,
                        }}
                      >
                        <span>Ngưỡng dưới: {currentData.lowerBound ?? '—'}</span>
                        <span style={{ color: '#16a34a', fontWeight: 600 }}>Khoảng bình thường</span>
                        <span>Ngưỡng trên: {currentData.upperBound ?? '—'}</span>
                      </div>
                    </div>

                    <div
                      style={{
                        background: '#f1f5f9',
                        borderRadius: 8,
                        padding: '8px 12px',
                        fontSize: 12,
                        color: '#475569',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 6,
                      }}
                    >
                      <InfoCircleOutlined style={{ color: '#2563eb' }} />
                      <span>
                        Chỉ số mang tính tham khảo y khoa. Mọi đánh giá cần được bác sĩ điều trị giải thích phù hợp với bệnh cảnh.
                      </span>
                    </div>
                  </div>
                </Col>
              </Row>
            </Card>
          ) : (
            <Card
              size="small"
              style={{
                borderRadius: 12,
                border: '1px solid #e2e8f0',
                boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
              }}
            >
              <div style={{ fontSize: 13, fontWeight: 600, color: '#64748b', marginBottom: 10 }}>
                NỘI DUNG KẾT QUẢ ĐỊNH TÍNH / MÔ TẢ
              </div>
              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: 10,
                  padding: '14px 16px',
                  fontSize: 14,
                  lineHeight: 1.6,
                  color: '#1e293b',
                  whiteSpace: 'pre-wrap',
                }}
              >
                {currentData.textValue || 'Đã ghi nhận kết quả theo hồ sơ chuyên môn.'}
              </div>
            </Card>
          )}

          {/* Kết luận chuyên môn của bác sĩ */}
          <Card
            size="small"
            style={{
              borderRadius: 12,
              border: '1px solid #bae6fd',
              background: '#f0f9ff',
            }}
          >
            <div
              style={{
                fontSize: 13,
                fontWeight: 700,
                color: '#0369a1',
                marginBottom: 8,
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              <FileDoneOutlined />
              <span>KẾT LUẬN CỦA BÁC SĨ CHUYÊN MÔN</span>
            </div>
            <div
              style={{
                fontSize: 14.5,
                fontWeight: 600,
                color: '#0f172a',
                lineHeight: 1.5,
              }}
            >
              {currentData.conclusion || 'Không có ghi nhận kết luận bất thường.'}
            </div>
          </Card>

          {/* Bảng thông tin bác sĩ & lượt khám */}
          <Card
            size="small"
            style={{
              borderRadius: 12,
              border: '1px solid #e2e8f0',
            }}
          >
            <Descriptions
              size="small"
              column={{ xs: 1, sm: 2 }}
              bordered={false}
              labelStyle={{ color: '#64748b', fontSize: 13, width: 140 }}
              contentStyle={{ color: '#0f172a', fontSize: 13, fontWeight: 500 }}
            >
              <Descriptions.Item label="Bác sĩ chỉ định">
                {currentData.orderingDoctorName || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Bác sĩ / KTV thực hiện">
                {currentData.performingDoctorName || currentData.doctorName || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Chuyên khoa / Khoa">
                {currentData.specialtyName || 'Khoa Cận lâm sàng'}
              </Descriptions.Item>
              <Descriptions.Item label="Thời gian có kết quả">
                {formatDateTime(currentData.enteredAt)}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          {/* Danh sách tệp đính kèm (nếu có) */}
          {detail?.attachments && detail.attachments.length > 0 && (
            <Card
              size="small"
              style={{
                borderRadius: 12,
                border: '1px solid #e2e8f0',
              }}
            >
              <div style={{ fontSize: 13, fontWeight: 600, color: '#64748b', marginBottom: 10 }}>
                HÌNH ẢNH / TỆP ĐÍNH KÈM ({detail.attachments.length})
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {detail.attachments.map((att) => (
                  <div
                    key={att.attachmentId}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '10px 14px',
                      background: '#f8fafc',
                      borderRadius: 8,
                      border: '1px solid #e2e8f0',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <FilePdfOutlined style={{ fontSize: 20, color: '#ef4444' }} />
                      <div>
                        <div style={{ fontSize: 13.5, fontWeight: 600, color: '#1e293b' }}>
                          {att.fileName}
                        </div>
                        <div style={{ fontSize: 11.5, color: '#64748b' }}>
                          {att.contentType} • {Math.round((att.fileSize || 0) / 1024)} KB
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>
      ) : null}
    </Modal>
  )
}

export default PatientClinicalResultDetailModal
