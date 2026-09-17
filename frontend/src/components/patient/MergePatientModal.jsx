import React, { useState, useEffect, useMemo } from 'react'
import {
  Modal,
  Button,
  Select,
  Input,
  Alert,
  Descriptions,
  Tag,
  Space,
  Typography,
  Divider,
  Popconfirm,
  message,
  Card,
  Row,
  Col,
  Checkbox,
} from 'antd'
import {
  SwapOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  UserOutlined,
  AuditOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
import patientApi from '../../api/patientApi'
import {
  MERGE_REASON_PRESETS,
  validatePatientMerge,
  cleanMergeErrorMessage,
} from '../../utils/patientMergeValidation'
import { formatDate } from '../../utils/helpers'

const { Text } = Typography
const { Option } = Select
const { TextArea } = Input

export default function MergePatientModal({
  open,
  onClose,
  initialTargetPatient = null,
  initialSourcePatient = null,
  allPatients = [],
  onSuccess,
}) {
  const [targetPatient, setTargetPatient] = useState(initialTargetPatient)
  const [sourcePatient, setSourcePatient] = useState(initialSourcePatient)
  const [reasonPreset, setReasonPreset] = useState(MERGE_REASON_PRESETS[0])
  const [customReason, setCustomReason] = useState('')
  const [confirmedAgreement, setConfirmedAgreement] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  // Sync state when props change
  useEffect(() => {
    if (open) {
      setTargetPatient(initialTargetPatient)
      setSourcePatient(initialSourcePatient)
      setReasonPreset(MERGE_REASON_PRESETS[0])
      setCustomReason('')
      setConfirmedAgreement(false)
    }
  }, [open, initialTargetPatient, initialSourcePatient])

  const fullReason = useMemo(() => {
    if (reasonPreset === 'Khác') {
      return customReason.trim()
    }
    if (customReason.trim()) {
      return `${reasonPreset} - ${customReason.trim()}`
    }
    return reasonPreset
  }, [reasonPreset, customReason])

  const validation = useMemo(() => {
    return validatePatientMerge(sourcePatient, targetPatient, fullReason)
  }, [sourcePatient, targetPatient, fullReason])

  const handleSwap = () => {
    const temp = targetPatient
    setTargetPatient(sourcePatient)
    setSourcePatient(temp)
  }

  const handleExecuteMerge = async () => {
    if (!validation.allowed) {
      message.error(validation.message || 'Dữ liệu gộp hồ sơ không hợp lệ.')
      return
    }

    setSubmitting(true)
    try {
      const payload = {
        sourcePatientId: sourcePatient.id || sourcePatient.patientId,
        targetPatientId: targetPatient.id || targetPatient.patientId,
        reason: fullReason || 'Gộp hồ sơ trùng lặp tiếp đón',
      }

      const res = await patientApi.merge(payload)
      const data = res?.data || {}
      const transferred = data.transferredVisitsCount ?? data.transferredCount ?? 0

      message.success({
        content: `Gộp hồ sơ thành công! Đã chuyển ${transferred} lượt khám từ [${sourcePatient.patientCode || 'Hồ sơ phụ'}] sang [${targetPatient.patientCode || 'Hồ sơ chính'}].`,
        duration: 5,
      })

      if (onSuccess) {
        onSuccess(data)
      }
      onClose()
    } catch (err) {
      message.error(cleanMergeErrorMessage(err, 'Lỗi khi gộp hồ sơ bệnh nhân.'))
    } finally {
      setSubmitting(false)
    }
  }

  // Helper render patient card
  const renderPatientCard = (patient, isTarget) => {
    const isMerged = Boolean(patient?.isMerged || patient?.status === 'MERGED' || patient?.mergedIntoPatientId)

    return (
      <Card
        size="small"
        style={{
          border: isTarget ? '2px solid #22c55e' : '2px solid #ef4444',
          borderRadius: 8,
          background: isTarget ? '#f0fdf4' : '#fef2f2',
          height: '100%',
        }}
        title={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Space>
              <UserOutlined style={{ color: isTarget ? '#16a34a' : '#dc2626' }} />
              <Text strong style={{ color: isTarget ? '#15803d' : '#b91c1c' }}>
                {isTarget ? 'HỒ SƠ CHÍNH (GIỮ LẠI)' : 'HỒ SƠ PHỤ (SẼ BỊ GỘP)'}
              </Text>
            </Space>
            {isTarget ? (
              <Tag color="success">Đích đến (Target)</Tag>
            ) : (
              <Tag color="error">Nguồn chuyển (Source)</Tag>
            )}
          </div>
        }
      >
        {patient ? (
          <div>
            {isMerged && (
              <Alert
                type="error"
                showIcon
                message="Hồ sơ này đã ở trạng thái ĐÃ GỘP trước đó"
                style={{ marginBottom: 12, padding: '4px 8px' }}
              />
            )}
            <Descriptions size="small" column={1} bordered style={{ background: '#fff' }}>
              <Descriptions.Item label="Mã bệnh nhân">
                <Text strong copyable>{patient.patientCode || '---'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Họ và tên">
                <Text strong>{patient.fullName || '---'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Ngày sinh / Giới tính">
                {formatDate(patient.dateOfBirth)} ({patient.gender === 'MALE' ? 'Nam' : patient.gender === 'FEMALE' ? 'Nữ' : 'Khác'})
              </Descriptions.Item>
              <Descriptions.Item label="Số CCCD / CMND">
                {patient.identityNumber || 'Chưa cập nhật'}
              </Descriptions.Item>
              <Descriptions.Item label="Số điện thoại">
                {patient.phone || patient.phoneNumber || 'Chưa cập nhật'}
              </Descriptions.Item>
              <Descriptions.Item label="Số thẻ BHYT">
                {patient.insuranceNumber || 'Không có'}
              </Descriptions.Item>
              <Descriptions.Item label="Địa chỉ">
                {patient.address || 'Chưa cập nhật'}
              </Descriptions.Item>
              {patient.guardianName && (
                <Descriptions.Item label="Người giám hộ">
                  {patient.guardianName} ({patient.guardianRelationship || 'Giám hộ'}) - SĐT: {patient.guardianPhone || '---'}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="Đồng ý DLCN">
                {patient.consentAgreed ? (
                  <Tag color="green" style={{ fontSize: 12, padding: '2px 8px', borderRadius: 4, fontWeight: 500 }}>
                    Đã đồng ý (v{patient.consentVersion || '1.0'})
                  </Tag>
                ) : (
                  <Tag color="orange" style={{ fontSize: 12, padding: '2px 8px', borderRadius: 4, fontWeight: 500 }}>
                    Chưa có phiếu đồng ý
                  </Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                {isMerged ? (
                  <Tag color="magenta">Đã gộp (MERGED)</Tag>
                ) : patient.active === false ? (
                  <Tag color="default">Đã lưu trữ</Tag>
                ) : (
                  <Tag color="green">Đang hoạt động</Tag>
                )}
              </Descriptions.Item>
            </Descriptions>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '30px 0', color: '#9ca3af' }}>
            <UserOutlined style={{ fontSize: 36, marginBottom: 8 }} />
            <div>Chưa chọn hồ sơ</div>
          </div>
        )}
      </Card>
    )
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <AuditOutlined style={{ color: '#2563eb', fontSize: 20 }} />
          <span>Gộp hồ sơ bệnh nhân trùng lặp</span>
        </div>
      }
      open={open}
      onCancel={onClose}
      width={950}
      footer={null}
      destroyOnClose
    >
      <Alert
        type="warning"
        showIcon
        icon={<WarningOutlined />}
        message="Lưu ý quy tắc nghiệp vụ khi gộp hồ sơ:"
        description={
          <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13 }}>
            <li>
              <strong>Hồ sơ chính (Giữ lại):</strong> Được bảo lưu mã bệnh nhân và tiếp nhận toàn bộ lịch sử khám chữa bệnh, đơn thuốc, viện phí và lịch hẹn.
            </li>
            <li>
              <strong>Hồ sơ phụ (Sẽ gộp):</strong> Được chuyển sang trạng thái <code>MERGED</code> (Đã gộp), khóa chỉnh sửa và gắn liên kết chuyển hướng sang hồ sơ chính.
            </li>
            <li>
              Thao tác này được hệ thống lưu vết kiểm toán (Audit Log) đầy đủ để đối soát lịch sử.
            </li>
          </ul>
        }
        style={{ marginBottom: 16 }}
      />

      <div style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col xs={24} md={11}>
            {renderPatientCard(targetPatient, true)}
          </Col>

          <Col xs={24} md={2} style={{ textAlign: 'center', margin: '12px 0' }}>
            <Button
              type="primary"
              shape="circle"
              icon={<SwapOutlined />}
              onClick={handleSwap}
              disabled={!targetPatient || !sourcePatient || submitting}
              title="Đổi chiều gộp hồ sơ"
              style={{
                boxShadow: '0 2px 8px rgba(37, 99, 235, 0.3)',
                background: '#2563eb',
              }}
            />
            <div style={{ fontSize: 11, color: '#6b7280', marginTop: 4 }}>Đổi chiều</div>
          </Col>

          <Col xs={24} md={11}>
            {renderPatientCard(sourcePatient, false)}
          </Col>
        </Row>
      </div>

      {(!targetPatient || !sourcePatient) && allPatients.length > 0 && (
        <div style={{ marginBottom: 16, background: '#f8fafc', padding: 12, borderRadius: 8, border: '1px solid #e2e8f0' }}>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>
            Chọn hồ sơ từ danh sách bệnh nhân:
          </Text>
          <Space direction="horizontal" wrap style={{ width: '100%' }}>
            {!targetPatient && (
              <Select
                placeholder="Chọn hồ sơ chính (Giữ lại)..."
                style={{ width: 340 }}
                showSearch
                optionFilterProp="children"
                onChange={(val) => setTargetPatient(allPatients.find((p) => (p.id || p.patientId) === val))}
              >
                {allPatients
                  .filter((p) => (p.id || p.patientId) !== (sourcePatient?.id || sourcePatient?.patientId))
                  .map((p) => (
                    <Option key={p.id || p.patientId} value={p.id || p.patientId}>
                      {p.patientCode} - {p.fullName} ({formatDate(p.dateOfBirth)})
                    </Option>
                  ))}
              </Select>
            )}

            {!sourcePatient && (
              <Select
                placeholder="Chọn hồ sơ phụ (Sẽ gộp)..."
                style={{ width: 340 }}
                showSearch
                optionFilterProp="children"
                onChange={(val) => setSourcePatient(allPatients.find((p) => (p.id || p.patientId) === val))}
              >
                {allPatients
                  .filter((p) => (p.id || p.patientId) !== (targetPatient?.id || targetPatient?.patientId))
                  .map((p) => (
                    <Option key={p.id || p.patientId} value={p.id || p.patientId}>
                      {p.patientCode} - {p.fullName} ({formatDate(p.dateOfBirth)})
                    </Option>
                  ))}
              </Select>
            )}
          </Space>
        </div>
      )}

      <Divider style={{ margin: '16px 0' }} />

      <div style={{ marginBottom: 16 }}>
        <Text strong style={{ display: 'block', marginBottom: 6 }}>
          Lý do gộp hồ sơ <span style={{ color: '#dc2626' }}>*</span>
        </Text>
        <Select
          value={reasonPreset}
          onChange={setReasonPreset}
          style={{ width: '100%', marginBottom: 8 }}
        >
          {MERGE_REASON_PRESETS.map((preset) => (
            <Option key={preset} value={preset}>
              {preset}
            </Option>
          ))}
        </Select>

        {(reasonPreset === 'Khác' || reasonPreset) && (
          <TextArea
            rows={2}
            placeholder="Ghi chú chi tiết thêm về lý do gộp hồ sơ (tuỳ chọn hoặc bắt buộc nếu chọn Khác)..."
            value={customReason}
            onChange={(e) => setCustomReason(e.target.value)}
            maxLength={500}
            showCount
          />
        )}
      </div>

      {/* Khu vực cam kết và xác nhận đồng ý gộp hồ sơ to, nổi bật */}
      <Card
        size="small"
        style={{
          borderRadius: 10,
          border: confirmedAgreement ? '2px solid #16a34a' : '2px solid #f59e0b',
          background: confirmedAgreement ? '#f0fdf4' : '#fffbeb',
          marginBottom: 16,
          boxShadow: confirmedAgreement ? '0 2px 8px rgba(22, 163, 74, 0.12)' : '0 2px 8px rgba(245, 158, 11, 0.12)',
          transition: 'all 0.25s ease-in-out',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14, padding: '6px 4px' }}>
          <Checkbox
            id="checkbox-merge-agreement"
            checked={confirmedAgreement}
            onChange={(e) => setConfirmedAgreement(e.target.checked)}
            disabled={!validation.allowed}
            style={{ marginTop: 2, transform: 'scale(1.25)' }}
          />
          <div
            style={{ flex: 1, cursor: validation.allowed ? 'pointer' : 'default' }}
            onClick={() => validation.allowed && setConfirmedAgreement(!confirmedAgreement)}
          >
            <Text
              strong
              style={{
                fontSize: 15,
                color: confirmedAgreement ? '#15803d' : '#92400e',
                display: 'block',
                marginBottom: 4,
              }}
            >
              Xác nhận đồng ý gộp hồ sơ và chuyển giao toàn bộ dữ liệu y tế
            </Text>
            <div style={{ fontSize: 13.5, color: '#4b5563', lineHeight: 1.55 }}>
              Tôi đã đối soát kỹ lưỡng và xác nhận hai hồ sơ trên thuộc cùng một bệnh nhân. Tôi{' '}
              <strong style={{ color: '#111827' }}>đồng ý</strong> chuyển toàn bộ lịch sử khám, đơn thuốc,
              viện phí sang hồ sơ chính{' '}
              <Tag color="green" style={{ fontWeight: 600, fontSize: 12 }}>
                {targetPatient?.patientCode || 'Hồ sơ giữ lại'}
              </Tag>{' '}
              và khóa vĩnh viễn hồ sơ phụ{' '}
              <Tag color="red" style={{ fontWeight: 600, fontSize: 12 }}>
                {sourcePatient?.patientCode || 'Hồ sơ bị gộp'}
              </Tag>
              .
            </div>
          </div>
        </div>
      </Card>

      {!validation.allowed && (
        <Alert
          type="error"
          showIcon
          message="Chưa thể thực hiện gộp hồ sơ"
          description={validation.message}
          style={{ marginBottom: 16 }}
        />
      )}

      <style>{`
        .large-merge-popconfirm .ant-popconfirm-buttons {
          margin-top: 18px !important;
          display: flex !important;
          justify-content: flex-end !important;
          align-items: center !important;
          gap: 12px !important;
        }
        .large-merge-popconfirm .ant-popconfirm-buttons .ant-btn {
          height: 48px !important;
          font-size: 16px !important;
          font-weight: 600 !important;
          border-radius: 8px !important;
          padding: 0 24px !important;
          display: inline-flex !important;
          align-items: center !important;
          justify-content: center !important;
        }
        .large-merge-popconfirm .ant-popconfirm-buttons .ant-btn-dangerous,
        .large-merge-popconfirm .ant-popconfirm-buttons .ant-btn-primary {
          font-weight: 700 !important;
          font-size: 16px !important;
          min-width: 220px !important;
          height: 48px !important;
          box-shadow: 0 4px 14px rgba(220, 38, 38, 0.35) !important;
        }
      `}</style>

      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          alignItems: 'center',
          gap: 16,
          marginTop: 20,
          paddingTop: 16,
          borderTop: '1px solid #f1f5f9',
        }}
      >
        <Button
          size="large"
          onClick={onClose}
          disabled={submitting}
          style={{ minWidth: 130, height: 48, borderRadius: 8, fontSize: 16, fontWeight: 600 }}
        >
          Hủy bỏ
        </Button>

        <Popconfirm
          placement="topRight"
          overlayClassName="large-merge-popconfirm"
          overlayStyle={{ width: 560, maxWidth: '92vw' }}
          overlayInnerStyle={{
            padding: '24px 28px',
            borderRadius: 14,
            boxShadow: '0 16px 44px rgba(0, 0, 0, 0.22)',
            border: '1px solid #fed7aa',
            background: '#ffffff',
          }}
          title={
            <span style={{ fontSize: 18, fontWeight: 700, color: '#0f172a', letterSpacing: '-0.01em' }}>
              Xác nhận gộp hồ sơ bệnh nhân
            </span>
          }
          description={
            <div style={{ fontSize: 15, lineHeight: 1.7, color: '#334155', padding: '12px 0 16px 0' }}>
              <p style={{ margin: '0 0 12px 0', fontSize: 15 }}>
                Toàn bộ dữ liệu của hồ sơ{' '}
                <strong style={{ color: '#dc2626', fontSize: 15.5 }}>
                  {sourcePatient?.patientCode} ({sourcePatient?.fullName})
                </strong>{' '}
                sẽ được chuyển sang{' '}
                <strong style={{ color: '#16a34a', fontSize: 15.5 }}>
                  {targetPatient?.patientCode} ({targetPatient?.fullName})
                </strong>
                .
              </p>
              <div
                style={{
                  background: '#fef2f2',
                  border: '1px solid #fee2e2',
                  borderRadius: 8,
                  padding: '10px 14px',
                  color: '#b91c1c',
                  fontWeight: 600,
                  fontSize: 14,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <span>⚠️ Hồ sơ nguồn sẽ bị khóa và không thể hoàn tác. Bạn có chắc chắn?</span>
              </div>
            </div>
          }
          icon={<ExclamationCircleOutlined style={{ color: '#dc2626', fontSize: 28, marginTop: 2, marginRight: 8 }} />}
          okText="Tôi đồng ý gộp hồ sơ"
          cancelText="Xem lại"
          okButtonProps={{
            danger: true,
            size: 'large',
            loading: submitting,
            style: {
              fontWeight: 700,
              borderRadius: 8,
              minWidth: 220,
              height: 48,
              fontSize: 16,
              padding: '0 24px',
              boxShadow: '0 4px 14px rgba(220, 38, 38, 0.35)',
            },
          }}
          cancelButtonProps={{
            size: 'large',
            style: { borderRadius: 8, height: 48, minWidth: 130, fontSize: 16, fontWeight: 600, padding: '0 20px' },
          }}
          onConfirm={handleExecuteMerge}
          disabled={!validation.allowed || !confirmedAgreement || submitting}
        >
          <Button
            type="primary"
            danger
            size="large"
            icon={<CheckCircleOutlined style={{ fontSize: 20 }} />}
            loading={submitting}
            disabled={!validation.allowed || !confirmedAgreement}
            style={{
              minWidth: 270,
              height: 48,
              fontSize: 16,
              fontWeight: 700,
              borderRadius: 8,
              boxShadow:
                validation.allowed && confirmedAgreement
                  ? '0 4px 14px rgba(220, 38, 38, 0.35)'
                  : 'none',
            }}
          >
            Xác nhận đồng ý gộp hồ sơ
          </Button>
        </Popconfirm>
      </div>
    </Modal>
  )
}
