import React, { useEffect, useState } from 'react'
import {
  Modal,
  Button,
  Form,
  Select,
  Input,
  Alert,
  Card,
  Space,
  Tag,
  Typography,
  message,
  Spin,
} from 'antd'
import {
  SwapOutlined,
  UserOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import visitApi from '../../api/visitApi.js'
import {
  PRESET_HANDOVER_REASONS,
  canHandoverVisit,
  validateHandoverInput,
  saveStoredHandover,
} from '../../utils/handoverValidation.js'
import { getApiErrorMessage } from '../../utils/apiError.js'

const { TextArea } = Input
const { Text } = Typography

export default function HandoverPatientModal({
  open,
  onClose,
  visitId,
  visit,
  patient,
  medicalRecord,
  currentDoctorName,
  currentDoctorId,
  currentUser,
  onSuccess,
}) {
  const [form] = Form.useForm()
  const [doctors, setDoctors] = useState([])
  const [loadingDoctors, setLoadingDoctors] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  // Kiểm tra điều kiện bàn giao trước khi thao tác
  const checkCondition = canHandoverVisit(visit, medicalRecord, currentUser)

  useEffect(() => {
    if (open) {
      form.resetFields()
      fetchDoctors()
    }
  }, [open])

  const fetchDoctors = async () => {
    setLoadingDoctors(true)
    try {
      const res = await visitApi.getHandoverDoctors()
      const doctorList = Array.isArray(res.data) ? res.data : []
      // Lọc bỏ bác sĩ hiện tại đang phụ trách
      const filtered = doctorList.filter(
        (d) => String(d.id) !== String(currentDoctorId)
      )
      setDoctors(filtered)
    } catch (err) {
      message.error(getApiErrorMessage(err, 'Không thể tải danh sách bác sĩ tiếp nhận.'))
    } finally {
      setLoadingDoctors(false)
    }
  }

  // Chọn lý do mẫu nhanh
  const handleSelectPresetReason = (preset) => {
    const currentReason = form.getFieldValue('reason') || ''
    if (!currentReason) {
      form.setFieldsValue({ reason: preset })
    } else if (!currentReason.includes(preset)) {
      form.setFieldsValue({ reason: `${currentReason}. ${preset}` })
    }
  }

  // Thực hiện bàn giao
  const handleFinish = async (values) => {
    const { targetDoctorId, reason } = values

    // Validate đầu vào
    const validation = validateHandoverInput(targetDoctorId, reason, currentDoctorId)
    if (!validation.isValid) {
      message.warning(validation.error)
      return
    }

    setSubmitting(true)
    try {
      const targetDoctorObj = doctors.find((d) => String(d.id) === String(targetDoctorId))
      const targetDoctorName = targetDoctorObj?.fullName || 'bác sĩ tiếp nhận'

      await visitApi.handoverPatient(visitId, {
        targetDoctorId,
        reason: reason.trim(),
      })

      saveStoredHandover({
        visitId,
        patientId: patient?.id,
        patientName: patient?.fullName,
        patientCode: patient?.patientCode,
        fromDoctorId: currentDoctorId,
        fromDoctorName: currentDoctorName,
        toDoctorId: targetDoctorId,
        toDoctorName: targetDoctorName,
        reason: reason.trim(),
        handedOverAt: new Date().toISOString(),
      })

      if (onSuccess) {
        onSuccess(targetDoctorObj)
      } else {
        message.success(`Bàn giao ca khám sang ${targetDoctorName} thành công!`)
        onClose()
      }
    } catch (err) {
      message.error(getApiErrorMessage(err, 'Không thể thực hiện bàn giao bệnh nhân.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={680}
      style={{ top: 20 }}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SwapOutlined style={{ color: '#0284c7', fontSize: 20 }} />
          <span style={{ fontSize: 16, fontWeight: 700 }}>
            Bàn giao bệnh nhân sang bác sĩ khác
          </span>
        </div>
      }
      footer={
        <div
          style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            gap: 16,
            width: '100%',
            padding: '8px 0',
          }}
        >
          <Button
            key="cancel"
            onClick={onClose}
            disabled={submitting}
            style={{
              height: 46,
              minWidth: 120,
              borderRadius: 8,
              fontSize: 16,
              fontWeight: 500,
            }}
          >
            Hủy bỏ
          </Button>
          <Button
            key="submit"
            type="primary"
            icon={<SwapOutlined style={{ fontSize: 18 }} />}
            loading={submitting}
            disabled={!checkCondition.canHandover}
            onClick={() => form.submit()}
            style={{
              height: 46,
              minWidth: 180,
              borderRadius: 8,
              fontSize: 16,
              fontWeight: 600,
              background: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)',
              borderColor: '#0284c7',
              boxShadow: '0 2px 6px rgba(2, 132, 199, 0.3)',
            }}
          >
            Xác nhận bàn giao
          </Button>
        </div>
      }
    >
      {/* Cảnh báo nếu không đủ điều kiện (TC-02, TC-03) */}
      {!checkCondition.canHandover && (
        <Alert
          type="error"
          showIcon
          icon={<ExclamationCircleOutlined style={{ fontSize: 20 }} />}
          message="Không thể bàn giao ca khám"
          description={checkCondition.message}
          style={{ marginBottom: 16, borderRadius: 8 }}
        />
      )}

      {/* Thẻ tóm tắt thông tin ca khám & bệnh nhân */}
      <Card
        size="small"
        style={{
          marginBottom: 16,
          background: '#f8fafc',
          borderColor: '#e2e8f0',
          borderRadius: 8,
        }}
      >
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '8px 16px' }}>
          <div>
            <Text type="secondary">Bệnh nhân: </Text>
            <Text strong>{patient?.fullName || '---'}</Text>
            {patient?.patientCode && (
              <Tag color="geekblue" style={{ marginLeft: 6, fontFamily: 'monospace' }}>
                {patient.patientCode}
              </Tag>
            )}
          </div>
          <div>
            <Text type="secondary">Mã lượt khám: </Text>
            <Tag color="blue" style={{ fontFamily: 'monospace', fontWeight: 700 }}>
              {visit?.visitCode || '---'}
            </Tag>
          </div>
          <div>
            <Text type="secondary">Bác sĩ phụ trách hiện tại: </Text>
            <Text strong style={{ color: '#0f172a' }}>
              {currentDoctorName || visit?.doctor?.fullName || '---'}
            </Text>
          </div>
          <div>
            <Text type="secondary">Trạng thái bệnh án: </Text>
            <Tag color={medicalRecord?.status === 'SIGNED' ? 'success' : 'warning'}>
              {medicalRecord?.status === 'SIGNED' ? 'Đã ký số' : 'Chưa ký số (Đang khám)'}
            </Tag>
          </div>
        </div>
      </Card>

      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="targetDoctorId"
          label={<span style={{ fontWeight: 600 }}>Bác sĩ tiếp nhận ca khám</span>}
          rules={[{ required: true, message: 'Vui lòng chọn bác sĩ tiếp nhận ca khám' }]}
        >
          <Select
            placeholder="Chọn bác sĩ tiếp nhận..."
            loading={loadingDoctors}
            showSearch
            optionFilterProp="label"
            style={{ height: 42 }}
            options={doctors.map((d) => ({
              value: d.id,
              label: `${d.fullName} (${d.username || 'Bác sĩ'})`,
            }))}
            notFoundContent={
              loadingDoctors ? (
                <Spin size="small" />
              ) : (
                'Không tìm thấy bác sĩ nào khác đang hoạt động'
              )
            }
          />
        </Form.Item>

        {/* Lý do mẫu gợi ý nhanh */}
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary" style={{ fontSize: 13, display: 'block', marginBottom: 6 }}>
            Gợi ý lý do bàn giao nhanh (Bấm để chọn):
          </Text>
          <Space wrap size={6}>
            {PRESET_HANDOVER_REASONS.map((preset, idx) => (
              <Tag
                key={idx}
                style={{
                  cursor: 'pointer',
                  padding: '4px 10px',
                  borderRadius: 6,
                  background: '#f1f5f9',
                  borderColor: '#cbd5e1',
                  color: '#334155',
                  fontSize: 12.5,
                }}
                onClick={() => handleSelectPresetReason(preset)}
              >
                + {preset}
              </Tag>
            ))}
          </Space>
        </div>

        <Form.Item
          name="reason"
          label={<span style={{ fontWeight: 600 }}>Lý do bàn giao chi tiết</span>}
          rules={[
            { required: true, message: 'Vui lòng nhập lý do bàn giao bệnh nhân' },
            { max: 500, message: 'Lý do bàn giao không được vượt quá 500 ký tự' },
          ]}
        >
          <TextArea
            rows={4}
            maxLength={500}
            showCount
            placeholder="Ghi rõ lý do bàn giao ca khám (ví dụ: Có ca cấp cứu đột xuất, hết ca trực chuyển giao ca trực tiếp theo...)"
            style={{ borderRadius: 8, fontSize: 14 }}
          />
        </Form.Item>

        <Alert
          type="info"
          showIcon
          icon={<InfoCircleOutlined />}
          message="Lưu ý chuyển quyền ghi hồ sơ bệnh án"
          description="Sau khi xác nhận bàn giao thành công, quyền ghi tiếp hồ sơ bệnh án và ký hoàn tất ca khám sẽ được chuyển giao toàn bộ cho bác sĩ tiếp nhận. Hệ thống sẽ lưu giữ nhật ký bàn giao đầy đủ."
          style={{ borderRadius: 8 }}
        />
      </Form>
    </Modal>
  )
}
