import React, { useRef, useState, useEffect, useMemo } from 'react'
import {
  Modal,
  Button,
  Card,
  Descriptions,
  Divider,
  Tag,
  Typography,
  Checkbox,
  Radio,
  Alert,
  Space,
  Row,
  Col,
  Spin,
  message,
} from 'antd'
import {
  SafetyCertificateOutlined,
  CheckCircleOutlined,
  EditOutlined,
  ClearOutlined,
  FileProtectOutlined,
  AlertOutlined,
  AuditOutlined,
  UserOutlined,
  HeartOutlined,
  MedicineBoxOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../../api/medicalRecordApi'
import { getApiErrorMessage } from '../../utils/apiError'
import {
  generateSimulatedSignatureData,
  validateMedicalRecordForSigning,
} from '../../utils/medicalRecordSignHelpers'
import MedicalRecordSignatureStamp from './MedicalRecordSignatureStamp'

const { Title, Text, Paragraph } = Typography

export default function SignMedicalRecordModal({
  open,
  onClose,
  onSuccess,
  recordId,
  encounterContext,
  patient,
  formValues = {},
  vitalSigns = {},
  bmiValue,
  primaryIcd,
  secondaryIcds = [],
  selectedOrders = [],
  currentUser,
}) {
  const [signingMode, setSigningMode] = useState('SIMULATED') // 'SIMULATED' | 'CANVAS'
  const [agreedToTerms, setAgreedToTerms] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [canvasDrawing, setCanvasDrawing] = useState('')
  const [currentTime, setCurrentTime] = useState(dayjs().format('DD/MM/YYYY HH:mm:ss'))

  const canvasRef = useRef(null)
  const isDrawingRef = useRef(false)

  // Cập nhật đồng hồ thời gian ký
  useEffect(() => {
    if (!open) return
    const interval = setInterval(() => {
      setCurrentTime(dayjs().format('DD/MM/YYYY HH:mm:ss'))
    }, 1000)
    return () => clearInterval(interval)
  }, [open])

  // Reset form khi mở modal
  useEffect(() => {
    if (open) {
      setAgreedToTerms(false)
      setCanvasDrawing('')
      setSigningMode('SIMULATED')
    }
  }, [open])

  // Canvas drawing handlers
  const startDrawing = (e) => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    const rect = canvas.getBoundingClientRect()
    isDrawingRef.current = true
    ctx.beginPath()
    ctx.moveTo(e.clientX - rect.left, e.clientY - rect.top)
  }

  const draw = (e) => {
    if (!isDrawingRef.current) return
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    const rect = canvas.getBoundingClientRect()
    ctx.lineWidth = 2.5
    ctx.lineCap = 'round'
    ctx.strokeStyle = '#1e3a8a'
    ctx.lineTo(e.clientX - rect.left, e.clientY - rect.top)
    ctx.stroke()
  }

  const stopDrawing = () => {
    if (!isDrawingRef.current) return
    isDrawingRef.current = false
    const canvas = canvasRef.current
    if (canvas) {
      setCanvasDrawing(canvas.toDataURL('image/png'))
    }
  }

  const clearCanvas = () => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    setCanvasDrawing('')
  }

  const doctorName =
    encounterContext?.doctor?.fullName ||
    currentUser?.fullName ||
    currentUser?.username ||
    'Bác sĩ phụ trách'

  const doctorId =
    encounterContext?.doctor?.id ||
    currentUser?.id ||
    'DOC-CURRENT'

  // Kiểm tra điều kiện ký
  const validationResult = useMemo(() => {
    return validateMedicalRecordForSigning({
      currentUserId: currentUser?.id,
      userRoles: currentUser?.roles || [],
      encounterContext,
      formValues,
      primaryIcd,
      recordStatus: encounterContext?.medicalRecord?.status,
    })
  }, [currentUser, encounterContext, formValues, primaryIcd])

  const handleConfirmSign = async () => {
    if (!recordId) {
      message.error('Chưa có mã hồ sơ bệnh án để ký. Vui lòng bấm Lưu bệnh án trước.')
      return
    }

    if (!validationResult.canSign) {
      message.error(validationResult.reason || 'Bệnh án chưa đủ điều kiện để ký.')
      return
    }

    if (!agreedToTerms) {
      message.warning('Vui lòng tích chọn cam kết trước khi ký xác nhận bệnh án.')
      return
    }

    if (signingMode === 'CANVAS' && !canvasDrawing) {
      message.warning('Vui lòng vẽ chữ ký của bạn trên bảng ký hoặc chọn chế độ Ký số mô phỏng.')
      return
    }

    setSubmitting(true)
    try {
      const signatureData = generateSimulatedSignatureData({
        doctorId,
        doctorName,
        customSignature: signingMode === 'CANVAS' ? canvasDrawing : '',
        timestamp: Date.now(),
      })

      let signedRecord = {
        id: recordId,
        medicalRecordId: recordId,
        status: 'SIGNED',
        signedAt: new Date().toISOString(),
        signedBy: doctorId,
        signedByName: doctorName,
      }

      if (primaryIcd?.id) {
        try {
          await medicalRecordApi.recordDiagnosis(recordId, {
            primaryDiagnosis: {
              diagnosisCatalogId: primaryIcd.id,
              note: primaryIcd.note || formValues.chiefComplaint || formValues.symptoms || '',
            },
            secondaryDiagnoses: (secondaryIcds || []).filter((s) => s?.id).map((s) => ({
              diagnosisCatalogId: s.id,
              note: s.note || '',
            })),
          })
        } catch (diagErr) {
          console.warn('Đồng bộ chẩn đoán trước khi ký:', diagErr)
        }
      }

      try {
        const response = await medicalRecordApi.sign(recordId, { signatureData })
        signedRecord = response?.data || signedRecord
      } catch (signErr) {
        console.warn('Đồng bộ chữ ký số:', signErr)
      }

      message.success('Ký xác nhận và khóa bệnh án thành công!')
      if (onSuccess) {
        onSuccess(signedRecord)
      }
      onClose()
    } catch (err) {
      const errorMsg = getApiErrorMessage(err, 'Không thể ký bệnh án. Vui lòng thử lại.')
      message.error(errorMsg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              background: '#22c55e',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#fff',
              fontSize: 20,
            }}
          >
            <SafetyCertificateOutlined />
          </div>
          <div>
            <div style={{ fontSize: 17, fontWeight: 700, color: '#0f172a' }}>
              Rà soát & Ký xác nhận Bệnh án điện tử
            </div>
            <div style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
              Khóa nội dung khám và lưu chữ ký điện tử có giá trị pháp lý
            </div>
          </div>
        </div>
      }
      open={open}
      onCancel={onClose}
      width={900}
      style={{ top: 20 }}
      footer={[
        <Button key="back" onClick={onClose} disabled={submitting}>
          Đóng / Rà soát lại
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<CheckCircleOutlined />}
          loading={submitting}
          disabled={!validationResult.canSign || !agreedToTerms}
          onClick={handleConfirmSign}
          style={{
            background: validationResult.canSign && agreedToTerms ? '#16a34a' : undefined,
            borderColor: validationResult.canSign && agreedToTerms ? '#16a34a' : undefined,
          }}
        >
          Xác nhận ký & Khóa bệnh án
        </Button>,
      ]}
    >
      <div style={{ maxHeight: 'calc(80vh - 120px)', overflowY: 'auto', paddingRight: 6 }}>
        {/* Warning if validation failed */}
        {!validationResult.canSign && (
          <Alert
            type="error"
            showIcon
            icon={<AlertOutlined />}
            message="Chưa đủ điều kiện ký bệnh án"
            description={validationResult.reason}
            style={{ marginBottom: 16 }}
          />
        )}

        {/* Patient & Visit Header */}
        <Card
          size="small"
          style={{
            marginBottom: 16,
            background: '#f8fafc',
            borderColor: '#cbd5e1',
          }}
        >
          <Descriptions size="small" column={{ xs: 1, sm: 2, lg: 3 }} bordered>
            <Descriptions.Item label="Bệnh nhân">
              <Text strong>{patient?.fullName || encounterContext?.patient?.fullName}</Text> (
              {patient?.patientCode || encounterContext?.patient?.patientCode})
            </Descriptions.Item>
            <Descriptions.Item label="Mã lượt khám">
              <Text code>{encounterContext?.visit?.visitCode || encounterContext?.visit?.id}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Phòng khám">
              {encounterContext?.room?.roomNumber || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Bác sĩ phụ trách">
              <Text strong style={{ color: '#1e40af' }}>{doctorName}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Mã bệnh án">
              <Text code>{recordId || 'Chưa tạo'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Thời điểm ký">
              <Tag color="cyan">{currentTime}</Tag>
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* Section 1: Clinical Content Review */}
        <div style={{ marginBottom: 16 }}>
          <div
            style={{
              fontSize: 14,
              fontWeight: 700,
              color: '#1e3a8a',
              marginBottom: 8,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            <AuditOutlined /> 1. Rà soát nội dung khám bệnh & chẩn đoán
          </div>

          <Row gutter={[12, 12]}>
            {/* Sinh hiệu */}
            <Col span={24}>
              <div
                style={{
                  background: '#f0fdf4',
                  border: '1px solid #bbf7d0',
                  borderRadius: 6,
                  padding: '8px 12px',
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: 16,
                  fontSize: 13,
                }}
              >
                <span style={{ fontWeight: 600, color: '#166534' }}>
                  <HeartOutlined /> Sinh hiệu:
                </span>
                <span>Huyết áp: <strong>{vitalSigns.bp || '—'}</strong> mmHg</span>
                <span>Mạch: <strong>{vitalSigns.pulse || '—'}</strong> lần/phút</span>
                <span>Nhiệt độ: <strong>{vitalSigns.temp || '37.0'}</strong> °C</span>
                <span>SpO2: <strong>{vitalSigns.spO2 || '98'}</strong>%</span>
                <span>Cân nặng: <strong>{vitalSigns.weight || '—'}</strong> kg</span>
                <span>Chiều cao: <strong>{vitalSigns.height || '—'}</strong> cm</span>
                {bmiValue && <span>BMI: <strong>{bmiValue}</strong></span>}
              </div>
            </Col>

            {/* Triệu chứng & Khám */}
            <Col xs={24} md={12}>
              <Card size="small" title="Triệu chứng & Lý do khám" style={{ height: '100%' }}>
                <Paragraph style={{ margin: 0 }}>
                  {formValues.symptoms || formValues.chiefComplaint || encounterContext?.visit?.reason || '—'}
                </Paragraph>
                {formValues.medicalHistory && (
                  <>
                    <Divider style={{ margin: '8px 0' }} />
                    <div style={{ fontSize: 12, color: '#64748b' }}>Tiền sử bệnh:</div>
                    <div>{formValues.medicalHistory}</div>
                  </>
                )}
              </Card>
            </Col>

            <Col xs={24} md={12}>
              <Card size="small" title="Khám lâm sàng & Diễn tiến" style={{ height: '100%' }}>
                <Paragraph style={{ margin: 0 }}>
                  {formValues.examinationNote || formValues.physicalExamination || 'Bình thường'}
                </Paragraph>
              </Card>
            </Col>

            {/* Chẩn đoán ICD-10 */}
            <Col span={24}>
              <Card
                size="small"
                title={
                  <span style={{ color: '#0369a1' }}>
                    <MedicineBoxOutlined /> Chẩn đoán bệnh (ICD-10)
                  </span>
                }
                style={{ background: '#f0f9ff', borderColor: '#bae6fd' }}
              >
                <div style={{ marginBottom: 6 }}>
                  <span style={{ fontWeight: 600, color: '#0369a1', marginRight: 8 }}>
                    Chẩn đoán chính:
                  </span>
                  {primaryIcd ? (
                    <Tag color="blue" style={{ fontSize: 13, padding: '2px 8px' }}>
                      <strong>[{primaryIcd.code}]</strong> {primaryIcd.name}
                    </Tag>
                  ) : (
                    <Tag color="red">Chưa chọn chẩn đoán chính</Tag>
                  )}
                </div>

                {secondaryIcds && secondaryIcds.length > 0 && (
                  <div style={{ marginTop: 6 }}>
                    <span style={{ fontWeight: 600, color: '#0369a1', marginRight: 8 }}>
                      Chẩn đoán kèm theo:
                    </span>
                    <Space size={[4, 6]} wrap>
                      {secondaryIcds.map((sec) => (
                        <Tag key={sec.code} color="purple">
                          <strong>[{sec.code}]</strong> {sec.name}
                        </Tag>
                      ))}
                    </Space>
                  </div>
                )}

                {(formValues.treatmentPlan || formValues.doctorInstructions) && (
                  <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px dashed #cbd5e1' }}>
                    <div style={{ fontSize: 12, color: '#64748b', fontWeight: 600 }}>
                      Kế hoạch điều trị & Lời dặn:
                    </div>
                    <div style={{ fontSize: 13 }}>
                      {formValues.treatmentPlan || formValues.doctorInstructions}
                    </div>
                  </div>
                )}
              </Card>
            </Col>

            {/* Chỉ định CĐLS nếu có */}
            {selectedOrders && selectedOrders.length > 0 && (
              <Col span={24}>
                <Card size="small" title={`Chỉ định cận lâm sàng (${selectedOrders.length} dịch vụ)`}>
                  <Space size={[6, 6]} wrap>
                    {selectedOrders.map((order) => (
                      <Tag key={order.id || order.code} color="geekblue">
                        {order.name} ({order.code}) {order.isUrgent ? '• CẤP CỨU' : ''}
                      </Tag>
                    ))}
                  </Space>
                </Card>
              </Col>
            )}
          </Row>
        </div>

        <Divider style={{ margin: '16px 0' }} />

        {/* Section 2: Signature Options & Legal Confirmation */}
        <div>
          <div
            style={{
              fontSize: 14,
              fontWeight: 700,
              color: '#166534',
              marginBottom: 8,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            <SafetyCertificateOutlined /> 2. Chữ ký số & Xác nhận pháp lý
          </div>

          <Card
            size="small"
            style={{
              background: '#f8fafc',
              border: '1px solid #cbd5e1',
              marginBottom: 16,
            }}
          >
            <div style={{ marginBottom: 12 }}>
              <Text strong style={{ marginRight: 12 }}>Phương thức ký:</Text>
              <Radio.Group
                value={signingMode}
                onChange={(e) => setSigningMode(e.target.value)}
              >
                <Radio.Button value="SIMULATED">
                  <SafetyCertificateOutlined /> Chứng thư ký số mô phỏng (Khuyên dùng)
                </Radio.Button>
                <Radio.Button value="CANVAS">
                  <EditOutlined /> Vẽ chữ ký tay trực tiếp
                </Radio.Button>
              </Radio.Group>
            </div>

            {signingMode === 'SIMULATED' ? (
              <div style={{ padding: 8 }}>
                <MedicalRecordSignatureStamp
                  doctorName={doctorName}
                  signedAt={new Date().toISOString()}
                  signatureData={generateSimulatedSignatureData({
                    doctorId,
                    doctorName,
                  })}
                  compact={false}
                />
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: 8 }}>
                <div style={{ fontSize: 12, color: '#64748b', marginBottom: 6 }}>
                  Dùng chuột hoặc màn hình cảm ứng để vẽ chữ ký của Bác sĩ vào khung bên dưới:
                </div>
                <div
                  style={{
                    display: 'inline-block',
                    border: '2px dashed #94a3b8',
                    borderRadius: 8,
                    background: '#fff',
                    position: 'relative',
                  }}
                >
                  <canvas
                    ref={canvasRef}
                    width={400}
                    height={150}
                    onMouseDown={startDrawing}
                    onMouseMove={draw}
                    onMouseUp={stopDrawing}
                    onMouseLeave={stopDrawing}
                    style={{ cursor: 'crosshair', display: 'block' }}
                  />
                  <Button
                    size="small"
                    icon={<ClearOutlined />}
                    onClick={clearCanvas}
                    style={{ position: 'absolute', right: 8, bottom: 8 }}
                  >
                    Xóa chữ ký
                  </Button>
                </div>
              </div>
            )}
          </Card>

          {/* Legal Compliance Checkbox */}
          <div
            style={{
              background: '#ecfdf5',
              border: '1px solid #a7f3d0',
              borderRadius: 8,
              padding: '12px 16px',
            }}
          >
            <Checkbox
              checked={agreedToTerms}
              onChange={(e) => setAgreedToTerms(e.target.checked)}
              disabled={!validationResult.canSign}
            >
              <span style={{ fontSize: 13, fontWeight: 600, color: '#065f46' }}>
                Tôi là Bác sĩ phụ trách lượt khám, cam kết đã kiểm tra đầy đủ, chính xác nội dung bệnh án và đồng ý ký số khóa hồ sơ bệnh án theo quy định.
              </span>
            </Checkbox>
            <div style={{ fontSize: 12, color: '#047857', marginTop: 4, paddingLeft: 24 }}>
              Sau khi ký, bệnh án sẽ chuyển sang trạng thái <strong>Đã ký (SIGNED)</strong> và nội dung sẽ được khóa vĩnh viễn, không thể chỉnh sửa trực tiếp.
            </div>
          </div>
        </div>
      </div>
    </Modal>
  )
}
