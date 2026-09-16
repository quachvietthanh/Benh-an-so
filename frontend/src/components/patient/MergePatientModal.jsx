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
  const [submitting, setSubmitting] = useState(false)

  // Sync state when props change
  useEffect(() => {
    if (open) {
      setTargetPatient(initialTargetPatient)
      setSourcePatient(initialSourcePatient)
      setReasonPreset(MERGE_REASON_PRESETS[0])
      setCustomReason('')
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

      {!validation.allowed && (
        <Alert
          type="error"
          showIcon
          message="Chưa thể thực hiện gộp hồ sơ"
          description={validation.message}
          style={{ marginBottom: 16 }}
        />
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 20 }}>
        <Button onClick={onClose} disabled={submitting}>
          Hủy bỏ
        </Button>

        <Popconfirm
          title="Xác nhận gộp hồ sơ bệnh nhân"
          description={
            <div style={{ maxWidth: 360 }}>
              Toàn bộ dữ liệu của hồ sơ <strong>{sourcePatient?.patientCode} ({sourcePatient?.fullName})</strong> sẽ được chuyển sang{' '}
              <strong>{targetPatient?.patientCode} ({targetPatient?.fullName})</strong>. Hồ sơ nguồn sẽ bị khóa chỉnh sửa. Bạn có chắc chắn?
            </div>
          }
          icon={<ExclamationCircleOutlined style={{ color: '#dc2626' }} />}
          okText="Đồng ý gộp"
          cancelText="Xem lại"
          okButtonProps={{ danger: true, loading: submitting }}
          onConfirm={handleExecuteMerge}
          disabled={!validation.allowed || submitting}
        >
          <Button
            type="primary"
            danger
            icon={<CheckCircleOutlined />}
            loading={submitting}
            disabled={!validation.allowed}
          >
            Xác nhận gộp hồ sơ
          </Button>
        </Popconfirm>
      </div>
    </Modal>
  )
}
