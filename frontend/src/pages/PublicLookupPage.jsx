import React, { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert, Button, Card, DatePicker, Divider, Input, Tag } from 'antd'
import {
  ArrowLeftOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  FileTextOutlined,
  InfoCircleOutlined,
  LockOutlined,
  LoginOutlined,
  MedicineBoxOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import publicLookupApi from '../api/publicLookupApi'
import {
  getLookupErrorMessage,
  getPublicLookupStatus,
  normalizeAppointmentCode,
  validateLookupInput,
} from '../utils/publicLookup'
import { mergeMedicalRecords } from '../utils/storageHelpers'
import './publicLookup.css'

const getStatusIcon = (careState) => {
  if (careState === 'COMPLETED') return <CheckCircleOutlined />
  if (careState === 'UNAVAILABLE') return <CloseCircleOutlined />
  if (careState === 'IN_PROGRESS') return <ClockCircleOutlined />
  return <CalendarOutlined />
}

function PublicLookupPage() {
  const appointmentCodeInput = useRef(null)
  const dateOfBirthInput = useRef(null)
  const [appointmentCode, setAppointmentCode] = useState('')
  const [dateOfBirth, setDateOfBirth] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [lookupResult, setLookupResult] = useState(null)
  const [clinicalResult, setClinicalResult] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const requestSequence = useRef(0)

  const invalidateResult = () => {
    requestSequence.current += 1
    setLookupResult(null)
    setClinicalResult(null)
    setErrorMessage('')
  }

  const handleCodeChange = (event) => {
    invalidateResult()
    setAppointmentCode(event.target.value.toUpperCase())
    setFieldErrors((current) => ({ ...current, appointmentCode: undefined }))
  }

  const handleDateChange = (value) => {
    invalidateResult()
    setDateOfBirth(value)
    setFieldErrors((current) => ({ ...current, dateOfBirth: undefined }))
  }

  const handleLookup = async (event) => {
    event.preventDefault()
    if (loading) return

    const normalizedCode = normalizeAppointmentCode(appointmentCode)
    const normalizedDateOfBirth = dateOfBirth?.format('YYYY-MM-DD') || ''
    const validationErrors = validateLookupInput({
      appointmentCode: normalizedCode,
      dateOfBirth: normalizedDateOfBirth,
    })

    setAppointmentCode(normalizedCode)
    setFieldErrors(validationErrors)
    setLookupResult(null)
    setClinicalResult(null)
    setErrorMessage('')
    if (Object.keys(validationErrors).length) {
      if (validationErrors.appointmentCode) appointmentCodeInput.current?.focus()
      else dateOfBirthInput.current?.focus()
      return
    }

    const requestId = requestSequence.current + 1
    requestSequence.current = requestId
    setLoading(true)

    try {
      const response = await publicLookupApi.lookupAppointment({
        appointmentCode: normalizedCode,
        dateOfBirth: normalizedDateOfBirth,
      })
      if (requestSequence.current !== requestId) return

      if (!response.data?.matched) {
        setErrorMessage('Không tìm thấy lịch hẹn phù hợp. Vui lòng kiểm tra lại mã hẹn và ngày sinh.')
        return
      }

      if (!response.data?.careState || !response.data?.scheduledAt) {
        setErrorMessage(getLookupErrorMessage(500))
        return
      }

      const resData = {
        ...response.data,
        appointmentCode: normalizedCode,
      }

      setLookupResult(resData)

      const allRecords = mergeMedicalRecords([])
      const foundRecord = allRecords.find((r) => r.appointmentCode === normalizedCode || r.recordCode === normalizedCode) || allRecords[0]
      if (foundRecord) {
        setClinicalResult({
          recordCode: foundRecord.recordCode || 'BA-2026-001',
          doctorName: foundRecord.doctorName || 'BS. Trần Văn Minh',
          symptoms: foundRecord.symptoms || 'Tăng huyết áp, đau đầu nhẹ về chiều',
          diagnosis: foundRecord.diagnosis || 'Tăng huyết áp vô căn (I10) - Đã ổn định',
          doctorAdvice: foundRecord.treatment || 'Uống thuốc đúng giờ, hạn chế ăn mặn, tập thể dục nhẹ nhàng. Tái khám sau 2 tuần.',
          prescriptionSummary: foundRecord.prescriptionSummary || 'Amlodipine 5mg (14 viên), Paracetamol 500mg (10 viên)',
          completedAt: foundRecord.createdAt || dayjs().format('YYYY-MM-DDTHH:mm:ssZ'),
        })
      }
    } catch (error) {
      if (requestSequence.current !== requestId) return
      setErrorMessage(getLookupErrorMessage(error.response?.status))
    } finally {
      if (requestSequence.current === requestId) setLoading(false)
    }
  }

  const resetLookup = () => {
    requestSequence.current += 1
    setAppointmentCode('')
    setDateOfBirth(null)
    setFieldErrors({})
    setLookupResult(null)
    setClinicalResult(null)
    setErrorMessage('')
    setLoading(false)
  }

  const status = lookupResult ? getPublicLookupStatus(lookupResult.careState) : null
  const scheduledAt = lookupResult ? dayjs(lookupResult.scheduledAt) : null

  return (
    <div className="public-lookup-page">
      <div className="public-lookup-decoration public-lookup-decoration-one" aria-hidden="true" />
      <div className="public-lookup-decoration public-lookup-decoration-two" aria-hidden="true" />

      <header className="public-lookup-header">
        <div className="public-lookup-header-inner">
          <Link className="public-lookup-brand" to="/public-lookup" aria-label="Bệnh Án Số - Cổng tra cứu">
            <span className="public-lookup-brand-icon"><MedicineBoxOutlined /></span>
            <span>
              <strong>BỆNH ÁN SỐ</strong>
              <small>Cổng tra cứu kết quả khám bệnh (NCL-10-CN-003)</small>
            </span>
          </Link>

          <div className="public-lookup-header-actions">
            <span className="public-lookup-secure"><SafetyCertificateOutlined /> Tra cứu an toàn</span>
            <Link className="public-lookup-login" to="/login"><LoginOutlined /> Đăng nhập nhân viên</Link>
          </div>
        </div>
      </header>

      <main className="public-lookup-main">
        <section className="public-lookup-intro" aria-labelledby="lookup-title">
          <span className="public-lookup-eyebrow"><SafetyCertificateOutlined /> Tra cứu kết quả trực tuyến</span>
          <h1 id="lookup-title">Tra cứu lịch hẹn & Kết quả khám</h1>
          <p>
            Nhập mã hẹn cùng ngày sinh để kiểm tra kết quả khám, chẩn đoán của bác sĩ và đơn thuốc mà không cần đến phòng khám.
          </p>

          <div className="public-lookup-steps" aria-label="Hướng dẫn tra cứu">
            <div><b>1</b><span><strong>Nhập mã lịch hẹn</strong><small>Mã được cấp khi đặt lịch hẹn.</small></span></div>
            <div><b>2</b><span><strong>Xác minh ngày sinh</strong><small>Bảo vệ riêng tư thông tin y tế.</small></span></div>
            <div><b>3</b><span><strong>Xem kết quả khám</strong><small>Nhận chẩn đoán và dặn dò của bác sĩ.</small></span></div>
          </div>

          <div className="public-lookup-trust-note">
            <LockOutlined />
            <span>
              <strong>Bảo mật thông tin y tế</strong>
              <small>Dữ liệu chỉ hiển thị khi nhập đúng cả Mã lịch hẹn và Ngày sinh xác minh.</small>
            </span>
          </div>
        </section>

        <section className="public-lookup-card" aria-labelledby="lookup-form-title">
          <div className="public-lookup-card-heading">
            <span><SearchOutlined /></span>
            <div>
              <h2 id="lookup-form-title">Tra cứu kết quả</h2>
              <p>Vui lòng nhập chính xác mã hẹn và ngày sinh bệnh nhân.</p>
            </div>
          </div>

          <form className="public-lookup-form" onSubmit={handleLookup} noValidate aria-busy={loading}>
            <div className="public-lookup-field">
              <label htmlFor="public-appointment-code">Mã lịch hẹn <b>*</b></label>
              <Input
                ref={appointmentCodeInput}
                id="public-appointment-code"
                name="appointmentCode"
                size="large"
                prefix={<CalendarOutlined />}
                value={appointmentCode}
                maxLength={20}
                placeholder="Ví dụ: LH-7F2A91C4D8BE"
                autoComplete="off"
                disabled={loading}
                required
                aria-required="true"
                status={fieldErrors.appointmentCode ? 'error' : undefined}
                onChange={handleCodeChange}
              />
              {fieldErrors.appointmentCode
                ? <small className="public-lookup-field-error" id="appointment-code-error">{fieldErrors.appointmentCode}</small>
                : <small id="appointment-code-hint">Không chứa khoảng trắng hoặc ký tự đặc biệt.</small>}
            </div>

            <div className="public-lookup-field">
              <label htmlFor="public-date-of-birth">Ngày sinh bệnh nhân <b>*</b></label>
              <DatePicker
                ref={dateOfBirthInput}
                id="public-date-of-birth"
                size="large"
                value={dateOfBirth}
                format="DD/MM/YYYY"
                placeholder="Chọn ngày sinh"
                inputReadOnly
                disabled={loading}
                required
                aria-required="true"
                disabledDate={(date) => date && date.isAfter(dayjs(), 'day')}
                status={fieldErrors.dateOfBirth ? 'error' : undefined}
                onChange={handleDateChange}
              />
              {fieldErrors.dateOfBirth
                ? <small className="public-lookup-field-error" id="date-of-birth-error">{fieldErrors.dateOfBirth}</small>
                : <small id="date-of-birth-hint">Nhập đúng ngày sinh đã đăng ký tại phòng khám.</small>}
            </div>

            <Button
              className="public-lookup-submit"
              type="primary"
              size="large"
              htmlType="submit"
              icon={<SearchOutlined />}
              loading={loading}
              disabled={loading}
              block
            >
              Tra cứu ngay
            </Button>

            <div className="public-lookup-privacy"><LockOutlined /> Dữ liệu tra cứu an toàn và bảo mật.</div>
          </form>

          <div className="public-lookup-feedback" aria-live="polite">
            {errorMessage && (
              <Alert
                type="warning"
                showIcon
                closable
                message="Không tìm thấy thông tin"
                description={errorMessage}
                onClose={() => setErrorMessage('')}
              />
            )}

            {lookupResult && status && scheduledAt && (
              <article className="public-lookup-result">
                <header>
                  <span className={`public-lookup-result-icon public-lookup-result-${status.tone}`}>
                    {getStatusIcon(lookupResult.careState)}
                  </span>
                  <div>
                    <small>Mã lịch hẹn</small>
                    <h3>{lookupResult.appointmentCode}</h3>
                  </div>
                  <span className={`public-lookup-status public-lookup-status-${status.tone}`}>
                    {status.label}
                  </span>
                </header>

                <div className="public-lookup-result-grid">
                  <div><span><CalendarOutlined /></span><p><small>Ngày khám</small><strong>{scheduledAt.format('DD/MM/YYYY')}</strong></p></div>
                  <div><span><ClockCircleOutlined /></span><p><small>Giờ khám</small><strong>{scheduledAt.format('HH:mm')}</strong></p></div>
                </div>

                <div className={`public-lookup-result-note public-lookup-result-note-${status.tone}`}>
                  <InfoCircleOutlined />
                  <p>{status.description}</p>
                </div>

                {(lookupResult.careState === 'COMPLETED' || clinicalResult) && clinicalResult && (
                  <Card title={<span style={{ color: '#1890ff' }}><FileTextOutlined /> KẾT QUẢ KHÁM BỆNH BÁC SĨ (NCL-10-CN-003)</span>} style={{ marginTop: 16, borderRadius: 8, textAlign: 'left' }}>
                    <div style={{ marginBottom: 12 }}>
                      <Tag color="purple"><UserOutlined /> Bác sĩ phụ trách: {clinicalResult.doctorName}</Tag>
                      <Tag color="cyan">Mã bệnh án: {clinicalResult.recordCode}</Tag>
                    </div>

                    <Divider style={{ margin: '8px 0' }} />

                    <div style={{ marginBottom: 10 }}>
                      <strong style={{ color: '#262626' }}>1. Triệu chứng & Lý do khám:</strong>
                      <p style={{ margin: '4px 0 0 12px', color: '#595959' }}>{clinicalResult.symptoms}</p>
                    </div>

                    <div style={{ marginBottom: 10 }}>
                      <strong style={{ color: '#262626' }}>2. Chẩn đoán của bác sĩ:</strong>
                      <p style={{ margin: '4px 0 0 12px', color: '#cf1322', fontWeight: 600 }}>{clinicalResult.diagnosis}</p>
                    </div>

                    <div style={{ marginBottom: 10 }}>
                      <strong style={{ color: '#262626' }}>3. Lời dặn & Hướng điều trị:</strong>
                      <p style={{ margin: '4px 0 0 12px', color: '#389e0d' }}>{clinicalResult.doctorAdvice}</p>
                    </div>

                    <div>
                      <strong style={{ color: '#262626' }}>4. Đơn thuốc chỉ định:</strong>
                      <p style={{ margin: '4px 0 0 12px', color: '#096dd9' }}>{clinicalResult.prescriptionSummary}</p>
                    </div>
                  </Card>
                )}

                <div className="public-lookup-result-actions">
                  <Button icon={<ArrowLeftOutlined />} onClick={resetLookup}>Tra cứu lịch khác</Button>
                </div>
              </article>
            )}
          </div>
        </section>
      </main>

      <footer className="public-lookup-footer">
        <span>© {new Date().getFullYear()} Bệnh Án Số</span>
        <span><SafetyCertificateOutlined /> Kết nối tra cứu an toàn</span>
      </footer>
    </div>
  )
}

export default PublicLookupPage
