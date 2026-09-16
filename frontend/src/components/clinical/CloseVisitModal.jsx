import React, { useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Input,
  Modal,
  Radio,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  ExclamationCircleFilled,
  ExclamationCircleOutlined,
  IdcardOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import queueApi from '../../api/queueApi.js'
import {
  CLOSE_VISIT_OUTCOMES,
  PRESET_CLOSE_REASONS,
  mapCloseVisitErrorMessage,
  validateCloseVisitForm,
} from '../../utils/closeVisitHelpers.js'

const { Text, Paragraph } = Typography
const { TextArea } = Input

export default function CloseVisitModal({
  open,
  visit,
  queueItem,
  queueItemId: propQueueItemId,
  patient,
  patientName: propPatientName,
  patientCode: propPatientCode,
  medicalRecord,
  onClose,
  onSuccess,
  onInvalidStatus,
  loading: externalLoading = false,
}) {
  const [outcome, setOutcome] = useState(CLOSE_VISIT_OUTCOMES.EARLY_ENDED)
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const isMountedRef = useRef(true)

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
    }
  }, [])

  useEffect(() => {
    if (open) {
      setOutcome(CLOSE_VISIT_OUTCOMES.EARLY_ENDED)
      setReason('')
      setSubmitting(false)
    }
  }, [open])

  const patientName = useMemo(() => {
    return (
      propPatientName ||
      patient?.fullName ||
      patient?.name ||
      visit?.patient?.fullName ||
      visit?.patient?.name ||
      visit?.patientName ||
      queueItem?.patient?.fullName ||
      queueItem?.patient?.name ||
      queueItem?.patientName ||
      'Chưa xác định'
    )
  }, [propPatientName, patient, visit, queueItem])

  const patientCode = useMemo(() => {
    return (
      propPatientCode ||
      patient?.patientCode ||
      patient?.code ||
      visit?.patient?.patientCode ||
      visit?.patient?.code ||
      visit?.patientCode ||
      queueItem?.patient?.patientCode ||
      queueItem?.patient?.code ||
      queueItem?.patientCode ||
      ''
    )
  }, [propPatientCode, patient, visit, queueItem])

  const visitCode = useMemo(() => {
    const raw =
      visit?.visitCode ||
      queueItem?.visitCode ||
      visit?.code ||
      queueItem?.code ||
      visit?.id ||
      queueItem?.id ||
      ''
    if (!raw) return '—'
    return String(raw).length > 20 ? `VIS-${String(raw).slice(-6).toUpperCase()}` : raw
  }, [visit, queueItem])

  const startTime = useMemo(() => {
    return (
      visit?.startedAt ||
      visit?.visitAt ||
      queueItem?.calledAt ||
      queueItem?.checkedInAt ||
      visit?.appointmentTime ||
      queueItem?.appointmentTime ||
      null
    )
  }, [visit, queueItem])

  const currentStatus = useMemo(() => {
    return visit?.status || queueItem?.status || 'IN_PROGRESS'
  }, [visit, queueItem])

  const isRecordSigned = useMemo(() => {
    if (!medicalRecord) return false
    return Boolean(
      medicalRecord.isSigned ||
      medicalRecord.isContentLocked ||
      medicalRecord.status === 'SIGNED' ||
      medicalRecord.status === 'LOCKED'
    )
  }, [medicalRecord])

  const isStatusValid = currentStatus === 'IN_PROGRESS'
  const canProceed = isStatusValid && !isRecordSigned

  const currentLength = reason.trim().length
  const isReasonTooLong = currentLength > 500
  const isReasonBlank = currentLength === 0

  const validation = useMemo(() => {
    return validateCloseVisitForm({
      outcome,
      reason,
      currentStatus,
      isRecordSigned,
    })
  }, [outcome, reason, currentStatus, isRecordSigned])

  const handleSelectPreset = (preset) => {
    setReason((prev) => (prev === preset ? '' : preset))
  }

  const executeCloseVisit = async () => {
    const targetItemId =
      propQueueItemId ||
      queueItem?.id ||
      queueItem?.queueItemId ||
      visit?.queueItemId ||
      visit?.queueItem?.id ||
      (queueItem && typeof queueItem === 'string' ? queueItem : null)

    if (!targetItemId) {
      message.error('Không tìm thấy mã lượt khám trong hàng đợi (queueItemId) để thực hiện thao tác.')
      return
    }

    const val = validateCloseVisitForm({
      outcome,
      reason,
      currentStatus,
      isRecordSigned,
    })

    if (!val.valid) {
      message.warning(val.message)
      if (val.suggestion) {
        setOutcome(val.suggestion)
      }
      return
    }

    setSubmitting(true)
    try {
      const response = await queueApi.close(targetItemId, outcome, val.trimmedReason)
      const successMsg =
        outcome === CLOSE_VISIT_OUTCOMES.EARLY_ENDED
          ? 'Đã kết thúc sớm lượt khám thành công.'
          : 'Đã hủy lượt khám thành công.'
      message.success(successMsg, 3)

      if (onSuccess) {
        onSuccess(response?.data, outcome, val.trimmedReason)
      } else {
        onClose?.()
      }
    } catch (err) {
      const mapped = mapCloseVisitErrorMessage(err, outcome)
      message.error(mapped.message)

      if (mapped.shouldRefreshQueue) {
        onInvalidStatus?.()
      }
      if (mapped.suggestEarlyEnded && isMountedRef.current) {
        setOutcome(CLOSE_VISIT_OUTCOMES.EARLY_ENDED)
      }
    } finally {
      if (isMountedRef.current) {
        setSubmitting(false)
      }
    }
  }

  const handleModalClose = () => {
    onClose?.()
  }

  const handleConfirmSubmit = () => {
    if (!canProceed) {
      message.warning('Lượt khám này không thể đóng (không ở trạng thái Đang khám hoặc bệnh án đã khóa).')
      return
    }
    if (isReasonBlank) {
      message.warning('Vui lòng chọn gợi ý hoặc nhập lý do trước khi bấm xác nhận!')
      return
    }
    if (isReasonTooLong) {
      message.warning('Lý do đóng lượt khám không được vượt quá 500 ký tự!')
      return
    }
    if (!validation.valid) {
      message.warning(validation.message)
      if (validation.suggestion) setOutcome(validation.suggestion)
      return
    }

    const isEarly = outcome === CLOSE_VISIT_OUTCOMES.EARLY_ENDED
    const actionName = isEarly ? 'kết thúc sớm' : 'hủy'
    const titleText = isEarly ? 'Xác nhận kết thúc sớm lượt khám?' : 'Xác nhận hủy lượt khám?'

    Modal.confirm({
      title: titleText,
      icon: <ExclamationCircleOutlined style={{ color: isEarly ? '#ea580c' : '#dc2626' }} />,
      content: (
        <div style={{ marginTop: 8 }}>
          <Paragraph>
            Lượt khám của bệnh nhân <Text strong>{patientName}</Text> sẽ {actionName} với lý do:
          </Paragraph>
          <Card
            size="small"
            style={{
              backgroundColor: isEarly ? '#fff7ed' : '#fef2f2',
              borderColor: isEarly ? '#fdba74' : '#fca5a5',
              marginBottom: 12,
            }}
          >
            <Text italic>"{reason.trim()}"</Text>
          </Card>
          <Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 0 }}>
            ⚠️ <strong>Lưu ý:</strong> Lượt khám sẽ rời khỏi hàng đợi ngay lập tức và{' '}
            <strong>không thể hoàn tác</strong>. Bạn có chắc chắn muốn thực hiện?
          </Paragraph>
        </div>
      ),
      okText: isEarly ? 'Xác nhận kết thúc sớm' : 'Xác nhận hủy ca',
      okType: 'danger',
      cancelText: 'Quay lại xem xét',
      onOk: executeCloseVisit,
    })
  }

  const isLoading = submitting || externalLoading

  return (
    <Modal
      title={
        <Space align="center" size={10}>
          <ExclamationCircleFilled style={{ color: '#ea580c', fontSize: 20 }} />
          <span style={{ fontWeight: 700, fontSize: 16, color: '#0f172a' }}>
            Kết thúc sớm hoặc Hủy lượt khám kèm lý do
          </span>
        </Space>
      }
      open={open}
      onCancel={isLoading ? undefined : handleModalClose}
      width={720}
      centered={true}
      destroyOnClose
      footer={[
        <div
          key="footer-container"
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            gap: 12,
            paddingTop: 8,
          }}
        >
          <Button
            key="cancel"
            onClick={handleModalClose}
            disabled={isLoading}
            style={{ minWidth: 90, height: 36 }}
          >
            Quay lại
          </Button>
          <Button
            key="submit"
            type="primary"
            danger
            loading={isLoading}
            disabled={!canProceed}
            onClick={handleConfirmSubmit}
            style={{ minWidth: 170, height: 36, fontWeight: 600 }}
          >
            {outcome === CLOSE_VISIT_OUTCOMES.EARLY_ENDED
              ? 'Xác nhận kết thúc sớm'
              : 'Xác nhận hủy lượt khám'}
          </Button>
        </div>,
      ]}
    >
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: 18,
          paddingTop: 4,
          maxHeight: 'calc(80vh - 120px)',
          overflowY: 'auto',
          overflowX: 'hidden',
          paddingRight: 6,
        }}
      >
        {/* 1. KHỐI THÔNG TIN ĐỐI CHIẾU */}
        <div
          style={{
            backgroundColor: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: 8,
            padding: '14px 16px',
          }}
        >
          {/* Hàng 1: 3 cột đều nhau (Bệnh nhân / Mã lượt khám / Bắt đầu khám) */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(3, 1fr)',
              gap: 16,
              alignItems: 'flex-start',
            }}
          >
            {/* Cột 1: Bệnh nhân */}
            <div>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  color: '#64748b',
                  fontSize: 13,
                  marginBottom: 4,
                }}
              >
                <UserOutlined style={{ fontSize: 15, color: '#64748b' }} />
                <span>Bệnh nhân</span>
              </div>
              <div style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', lineHeight: 1.4 }}>
                {patientName}
              </div>
              {patientCode && (
                <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                  Mã BN: {patientCode}
                </div>
              )}
            </div>

            {/* Cột 2: Mã lượt khám */}
            <div>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  color: '#64748b',
                  fontSize: 13,
                  marginBottom: 4,
                }}
              >
                <IdcardOutlined style={{ fontSize: 15, color: '#64748b' }} />
                <span>Mã lượt khám</span>
              </div>
              <div>
                <Tag
                  color="blue"
                  style={{
                    fontFamily: 'monospace',
                    fontWeight: 600,
                    fontSize: 13,
                    padding: '2px 8px',
                    margin: 0,
                  }}
                >
                  {visitCode || '—'}
                </Tag>
              </div>
            </div>

            {/* Cột 3: Bắt đầu khám */}
            <div>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  color: '#64748b',
                  fontSize: 13,
                  marginBottom: 4,
                }}
              >
                <ClockCircleOutlined style={{ fontSize: 15, color: '#64748b' }} />
                <span>Bắt đầu khám</span>
              </div>
              <div style={{ fontSize: 13.5, color: '#1e293b', fontWeight: 500, lineHeight: 1.4 }}>
                {startTime ? dayjs(startTime).format('HH:mm - DD/MM/YYYY') : '—'}
              </div>
            </div>
          </div>

          {/* Hàng 2: Trạng thái hiện tại xuống dòng riêng biệt */}
          <div
            style={{
              marginTop: 12,
              paddingTop: 10,
              borderTop: '1px dashed #e2e8f0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <span style={{ fontSize: 13, color: '#64748b' }}>Trạng thái lượt khám hiện tại:</span>
            {currentStatus === 'IN_PROGRESS' ? (
              <Tag
                color="processing"
                icon={<ClockCircleOutlined />}
                style={{ fontSize: 12.5, padding: '2px 10px', borderRadius: 4, fontWeight: 600 }}
              >
                Đang khám (IN_PROGRESS)
              </Tag>
            ) : (
              <Tag color="default" style={{ fontSize: 12.5, padding: '2px 10px', borderRadius: 4 }}>
                {currentStatus}
              </Tag>
            )}
          </div>
        </div>

        {/* Cảnh báo nếu trạng thái không hợp lệ */}
        {!isStatusValid && (
          <Alert
            type="error"
            showIcon
            message="Không thể thực hiện đóng lượt khám"
            description={
              <span>
                Lượt khám này không ở trạng thái <strong>Đang khám (IN_PROGRESS)</strong>.
                Theo quy định nghiệp vụ, chỉ có thể đóng các ca khám đang diễn ra.
              </span>
            }
          />
        )}

        {/* Cảnh báo nếu bệnh án đã ký/khóa (QTN-18) */}
        {isRecordSigned && (
          <Alert
            type="error"
            showIcon
            message="Bệnh án đã được ký số / khóa nội dung (QTN-18)"
            description="Bệnh án đã được ký, không thể hủy lượt khám. Vui lòng lập bản đính chính theo quy định (QTN-18) thay vì đóng lượt khám."
          />
        )}

        {/* 2. HAI THẺ CHỌN LOẠI KẾT QUẢ (50% - 50% đồng nhất) */}
        <div>
          <Text strong style={{ display: 'block', marginBottom: 10, fontSize: 14, color: '#0f172a' }}>
            1. Chọn loại kết quả đóng ca khám <Text type="danger">*</Text>
          </Text>
          <Radio.Group
            value={outcome}
            onChange={(e) => setOutcome(e.target.value)}
            disabled={!canProceed || isLoading}
            style={{ width: '100%' }}
          >
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: 16,
                alignItems: 'stretch',
              }}
            >
              {/* Thẻ 1: EARLY_ENDED */}
              <div
                onClick={() => canProceed && !isLoading && setOutcome(CLOSE_VISIT_OUTCOMES.EARLY_ENDED)}
                style={{
                  cursor: canProceed && !isLoading ? 'pointer' : 'default',
                  border: outcome === CLOSE_VISIT_OUTCOMES.EARLY_ENDED ? '1.5px solid #f97316' : '1.5px solid #e2e8f0',
                  backgroundColor: outcome === CLOSE_VISIT_OUTCOMES.EARLY_ENDED ? '#fffaf5' : '#ffffff',
                  borderRadius: 8,
                  padding: '14px 16px',
                  boxSizing: 'border-box',
                  minHeight: 114,
                  display: 'flex',
                  flexDirection: 'column',
                  transition: 'all 0.2s ease',
                  boxShadow: outcome === CLOSE_VISIT_OUTCOMES.EARLY_ENDED ? '0 2px 8px rgba(249, 115, 22, 0.12)' : 'none',
                }}
              >
                {/* Dòng 1: [Radio] [Tiêu đề] [Badge mã] */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Radio value={CLOSE_VISIT_OUTCOMES.EARLY_ENDED} style={{ marginRight: 0 }} />
                  <span style={{ fontSize: 14.5, fontWeight: 600, color: '#0f172a', whiteSpace: 'nowrap' }}>
                    Kết thúc sớm
                  </span>
                  <Tag
                    color="orange"
                    style={{
                      margin: 0,
                      fontWeight: 600,
                      fontSize: 11,
                      padding: '1px 6px',
                      borderRadius: 4,
                      lineHeight: '18px',
                    }}
                  >
                    EARLY_ENDED
                  </Tag>
                </div>

                {/* Dòng 2: Mô tả căn lề theo tiêu đề */}
                <div style={{ paddingLeft: 24, marginTop: 6, fontSize: 12.5, color: '#64748b', lineHeight: 1.5 }}>
                  Bệnh nhân xin về sớm, bỏ về, chuyển viện khẩn cấp. Các đơn thuốc đã phát (nếu có) được giữ nguyên an toàn.
                </div>
              </div>

              {/* Thẻ 2: CANCELLED */}
              <div
                onClick={() => canProceed && !isLoading && setOutcome(CLOSE_VISIT_OUTCOMES.CANCELLED)}
                style={{
                  cursor: canProceed && !isLoading ? 'pointer' : 'default',
                  border: outcome === CLOSE_VISIT_OUTCOMES.CANCELLED ? '1.5px solid #ef4444' : '1.5px solid #e2e8f0',
                  backgroundColor: outcome === CLOSE_VISIT_OUTCOMES.CANCELLED ? '#fef2f2' : '#ffffff',
                  borderRadius: 8,
                  padding: '14px 16px',
                  boxSizing: 'border-box',
                  minHeight: 114,
                  display: 'flex',
                  flexDirection: 'column',
                  transition: 'all 0.2s ease',
                  boxShadow: outcome === CLOSE_VISIT_OUTCOMES.CANCELLED ? '0 2px 8px rgba(239, 68, 68, 0.12)' : 'none',
                }}
              >
                {/* Dòng 1: [Radio] [Tiêu đề] [Badge mã] */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Radio value={CLOSE_VISIT_OUTCOMES.CANCELLED} style={{ marginRight: 0 }} />
                  <span style={{ fontSize: 14.5, fontWeight: 600, color: '#0f172a', whiteSpace: 'nowrap' }}>
                    Hủy lượt khám
                  </span>
                  <Tag
                    color="red"
                    style={{
                      margin: 0,
                      fontWeight: 600,
                      fontSize: 11,
                      padding: '1px 6px',
                      borderRadius: 4,
                      lineHeight: '18px',
                    }}
                  >
                    CANCELLED
                  </Tag>
                </div>

                {/* Dòng 2: Mô tả căn lề theo tiêu đề */}
                <div style={{ paddingLeft: 24, marginTop: 6, fontSize: 12.5, color: '#64748b', lineHeight: 1.5 }}>
                  Tiếp nhận nhầm ca khám, trùng lịch hẹn phòng khám. Lượt khám sẽ bị hủy bỏ toàn bộ.
                </div>
              </div>
            </div>
          </Radio.Group>
        </div>

        {/* 3. Ô LƯU Ý ĐƠN THUỐC (Chỉ hiện khi chọn CANCELLED) */}
        {outcome === CLOSE_VISIT_OUTCOMES.CANCELLED && (
          <Alert
            type="warning"
            showIcon
            icon={<AlertOutlined />}
            message="Lưu ý về đơn thuốc đã phát"
            description="Nếu ca khám đã có đơn thuốc được phát tại quầy dược (DISPENSED), hệ thống máy chủ sẽ tự động từ chối hủy ca khám khi bấm xác nhận. Trong trường hợp đó, vui lòng chọn 'Kết thúc sớm' thay vì 'Hủy lượt khám'."
            style={{ margin: 0 }}
          />
        )}

        {/* 4. PHẦN LÝ DO ĐÓNG LƯỢT KHÁM */}
        <div>
          {/* Dòng tiêu đề chính */}
          <Text strong style={{ display: 'block', marginBottom: 4, fontSize: 14, color: '#0f172a' }}>
            2. Lý do đóng lượt khám <Text type="danger">*</Text>
          </Text>

          {/* Dòng nhãn gợi ý lý do nhanh đặt phía trên hàng nút */}
          <div style={{ fontSize: 12.5, color: '#64748b', marginBottom: 8 }}>
            Gợi ý lý do nhanh (nhấp để điền tự động):
          </div>

          {/* Lưới nút gợi ý lý do nhanh flex-wrap tự co giãn theo nội dung, chống tràn màn hình mobile */}
          <div
            className="close-visit-presets-container"
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              gap: 8,
              marginBottom: 10,
            }}
          >
            {(PRESET_CLOSE_REASONS[outcome] || []).map((preset) => {
              const isSelected = reason === preset
              return (
                <button
                  key={preset}
                  type="button"
                  className="close-visit-preset-btn"
                  disabled={!canProceed || isLoading}
                  onClick={() => canProceed && !isLoading && handleSelectPreset(preset)}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 6,
                    width: 'auto',
                    minHeight: 34,
                    padding: '6px 12px',
                    margin: 0,
                    borderRadius: 6,
                    fontSize: 13,
                    fontWeight: isSelected ? 600 : 400,
                    color: isSelected ? '#1d4ed8' : '#334155',
                    backgroundColor: isSelected ? '#eff6ff' : '#f8fafc',
                    border: isSelected ? '1.5px solid #3b82f6' : '1px solid #cbd5e1',
                    cursor: canProceed && !isLoading ? 'pointer' : 'not-allowed',
                    boxShadow: isSelected ? '0 1px 3px rgba(59, 130, 246, 0.15)' : 'none',
                    transition: 'all 0.15s ease',
                    boxSizing: 'border-box',
                    userSelect: 'none',
                    textAlign: 'center',
                    wordBreak: 'break-word',
                    lineHeight: 1.35,
                  }}
                  onMouseEnter={(e) => {
                    if (canProceed && !isLoading && !isSelected) {
                      e.currentTarget.style.backgroundColor = '#f1f5f9'
                      e.currentTarget.style.borderColor = '#94a3b8'
                      e.currentTarget.style.color = '#0f172a'
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (canProceed && !isLoading && !isSelected) {
                      e.currentTarget.style.backgroundColor = '#f8fafc'
                      e.currentTarget.style.borderColor = '#cbd5e1'
                      e.currentTarget.style.color = '#334155'
                    }
                  }}
                >
                  {isSelected && <CheckOutlined style={{ fontSize: 12, color: '#2563eb', flexShrink: 0 }} />}
                  <span>{preset}</span>
                </button>
              )
            })}
          </div>

          {/* Textarea nhập tự do */}
          <TextArea
            rows={3}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            disabled={!canProceed || isLoading}
            placeholder={
              outcome === CLOSE_VISIT_OUTCOMES.EARLY_ENDED
                ? 'Nhập lý do kết thúc sớm (ví dụ: Bệnh nhân bỏ về, xin xuất viện sớm, chuyển tuyến...)'
                : 'Nhập lý do hủy lượt khám (ví dụ: Tiếp nhận nhầm phòng khám, trùng lịch hẹn...)'
            }
            maxLength={600}
            status={isReasonTooLong ? 'error' : ''}
            style={{
              padding: '10px 12px',
              fontSize: 13.5,
              borderRadius: 6,
            }}
          />

          {/* Hàng hiển thị đếm ký tự góc dưới */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 }}>
            <Text type={isReasonTooLong ? 'danger' : 'secondary'} style={{ fontSize: 12 }}>
              {isReasonTooLong ? 'Lý do vượt quá giới hạn 500 ký tự cho phép của hệ thống!' : ''}
            </Text>
            <Text
              type={isReasonTooLong ? 'danger' : 'secondary'}
              style={{ fontSize: 12, fontWeight: 500 }}
            >
              {currentLength} / 500 ký tự
            </Text>
          </div>
        </div>
      </div>
    </Modal>
  )
}
