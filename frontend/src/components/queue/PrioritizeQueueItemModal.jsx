import React, { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  ExclamationCircleOutlined,
  StarOutlined,
  UserOutlined,
} from '@ant-design/icons'
import queueApi from '../../api/queueApi.js'
import {
  PRESET_PRIORITY_REASONS,
  mapPrioritizeErrorMessage,
  validatePrioritizeForm,
} from '../../utils/queuePriorityHelpers.js'

const { Text, Paragraph } = Typography

export default function PrioritizeQueueItemModal({
  open,
  item,
  onClose,
  onSuccess,
}) {
  const [form] = Form.useForm()
  const [selectedPriority, setSelectedPriority] = useState('EMERGENCY')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (open && item) {
      const defaultPriority = 'EMERGENCY'
      setSelectedPriority(defaultPriority)
      form.resetFields()
      form.setFieldsValue({
        priority: defaultPriority,
        reason: PRESET_PRIORITY_REASONS[defaultPriority][0],
      })
    }
  }, [open, item, form])

  if (!item) return null

  const patientName = item.patientName || 'Bệnh nhân'
  const patientCode = item.patientCode || '—'
  const doctorName = item.doctorName || 'BS. Phụ trách'
  const queueNumber = String(item.queueNumber || 1).padStart(2, '0')
  const visitCode = item.visitCode || item.visitId || item.id || '—'

  const currentPresets = PRESET_PRIORITY_REASONS[selectedPriority] || []

  const handleSelectPriority = (level) => {
    setSelectedPriority(level)
    form.setFieldsValue({ priority: level })
    // Đổi gợi ý lý do mẫu đầu tiên tương ứng với mức vừa chọn
    const presets = PRESET_PRIORITY_REASONS[level] || []
    if (presets.length > 0) {
      form.setFieldsValue({ reason: presets[0] })
    }
  }

  const handleApplyPreset = (presetText) => {
    form.setFieldsValue({ reason: presetText })
  }

  const handleConfirmSubmit = (values) => {
    const { valid, error, trimmedReason } = validatePrioritizeForm(
      values.priority || selectedPriority,
      values.reason,
    )

    if (!valid) {
      message.error(error)
      return
    }

    const isEmergency = selectedPriority === 'EMERGENCY'

    Modal.confirm({
      title: isEmergency
        ? 'Xác nhận đưa bệnh nhân vào diện CẤP CỨU?'
        : 'Xác nhận chuyển sang ĐỐI TƯỢNG ƯU TIÊN?',
      icon: <ExclamationCircleOutlined style={{ color: isEmergency ? '#dc2626' : '#d97706' }} />,
      content: isEmergency
        ? `Bệnh nhân "${patientName}" sẽ được chuyển thẳng lên ĐẦU HÀNG ĐỢI của bác sĩ ngay lập tức. Bạn có chắc chắn muốn thực hiện?`
        : `Bệnh nhân "${patientName}" sẽ được xếp ưu tiên khám trước các lượt thông thường. Xác nhận thực hiện?`,
      okText: 'Xác nhận ưu tiên',
      cancelText: 'Xem lại',
      okButtonProps: {
        danger: isEmergency,
        style: isEmergency ? {} : { backgroundColor: '#d97706', borderColor: '#d97706' },
      },
      onOk: async () => {
        const targetItemId = item.id || item.queueItemId || item.itemId
        const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(String(targetItemId))
        if (!targetItemId || !isUuid) {
          console.error('[PrioritizeQueueItem] targetItemId không phải là UUID chuẩn của hàng đợi:', targetItemId, item)
          message.error('Không tìm thấy định danh hàng đợi hợp lệ của bệnh nhân.')
          return
        }
        setSubmitting(true)
        try {
          await queueApi.prioritize(targetItemId, selectedPriority, trimmedReason)
          message.success(`Đã đánh dấu ưu tiên cho bệnh nhân "${patientName}".`)
          if (onSuccess) {
            onSuccess()
          }
          if (onClose) {
            onClose()
          }
        } catch (err) {
          const errMsg = mapPrioritizeErrorMessage(err)
          message.error(errMsg)
        } finally {
          setSubmitting(false)
        }
      },
    })
  }

  return (
    <Modal
      open={open}
      title={
        <Space align="center">
          <AlertOutlined style={{ color: '#dc2626', fontSize: 20 }} />
          <span style={{ fontWeight: 700, fontSize: 16 }}>
            Đánh dấu ưu tiên khám cho bệnh nhân
          </span>
        </Space>
      }
      onCancel={submitting ? undefined : onClose}
      maskClosable={!submitting}
      destroyOnClose
      width={640}
      footer={[
        <Button
          key="cancel"
          onClick={onClose}
          disabled={submitting}
          style={{ height: 42, minWidth: 100, borderRadius: 6 }}
        >
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          danger={selectedPriority === 'EMERGENCY'}
          style={
            selectedPriority === 'EMERGENCY'
              ? { height: 42, minWidth: 140, borderRadius: 6, fontWeight: 600, fontSize: 15 }
              : {
                  height: 42,
                  minWidth: 140,
                  borderRadius: 6,
                  fontWeight: 600,
                  fontSize: 15,
                  backgroundColor: '#d97706',
                  borderColor: '#d97706',
                }
          }
          loading={submitting}
          onClick={() => form.submit()}
        >
          {selectedPriority === 'EMERGENCY' ? 'Ưu tiên Cấp cứu' : 'Xác nhận ưu tiên'}
        </Button>,
      ]}
    >
      <div style={{ marginTop: 12 }}>
        {/* Thông tin đối chiếu bệnh nhân */}
        <div
          style={{
            background: '#f8fafc',
            padding: '12px 16px',
            borderRadius: 8,
            marginBottom: 16,
            border: '1px solid #e2e8f0',
          }}
        >
          <Row justify="space-between" align="middle" style={{ marginBottom: 6 }}>
            <Space>
              <UserOutlined style={{ color: '#64748b' }} />
              <Text strong style={{ fontSize: 15, color: '#0f172a' }}>
                {patientName}
              </Text>
              {patientCode !== '—' && (
                <Text type="secondary">({patientCode})</Text>
              )}
            </Space>
            <Tag color="blue" style={{ fontWeight: 700, fontSize: 13, margin: 0 }}>
              STT hiện tại: #{queueNumber}
            </Tag>
          </Row>
          <Row justify="space-between" align="middle" style={{ fontSize: 13, color: '#64748b' }}>
            <span>
              Bác sĩ phụ trách: <Text strong style={{ color: '#334155' }}>{doctorName}</Text>
            </span>
            <span>
              Mã lượt khám: <Text code>{visitCode}</Text>
            </span>
          </Row>
        </div>

        <Alert
          type="info"
          showIcon
          message="Quy định thứ tự ưu tiên (QTN-40)"
          description="Hệ thống tự động đưa ca Cấp cứu lên đầu danh sách khám của bác sĩ; đối tượng Ưu tiên được xếp sau cấp cứu nhưng trước các lượt khám thông thường. Thứ tự các bệnh nhân còn lại được giữ nguyên tương đối."
          style={{ marginBottom: 16 }}
        />

        <Form
          form={form}
          layout="vertical"
          onFinish={handleConfirmSubmit}
          initialValues={{ priority: 'EMERGENCY' }}
        >
          {/* Chọn mức độ ưu tiên dạng 2 thẻ lớn (Radio Cards) */}
          <Form.Item
            label={<strong style={{ fontSize: 14 }}>Chọn mức độ ưu tiên *</strong>}
            name="priority"
            rules={[{ required: true, message: 'Vui lòng chọn mức độ ưu tiên' }]}
            style={{ marginBottom: 16 }}
          >
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <Card
                  hoverable
                  onClick={() => handleSelectPriority('EMERGENCY')}
                  style={{
                    borderRadius: 8,
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    border: selectedPriority === 'EMERGENCY' ? '2px solid #dc2626' : '1px solid #e2e8f0',
                    backgroundColor: selectedPriority === 'EMERGENCY' ? '#fef2f2' : '#ffffff',
                    boxShadow: selectedPriority === 'EMERGENCY' ? '0 2px 8px rgba(220, 38, 38, 0.15)' : 'none',
                  }}
                  bodyStyle={{ padding: 14 }}
                >
                  <Space align="start">
                    <AlertOutlined style={{ color: '#dc2626', fontSize: 22, marginTop: 2 }} />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: 15, color: '#dc2626' }}>
                        🚨 CẤP CỨU
                      </div>
                      <div style={{ fontSize: 12, color: '#991b1b', marginTop: 4, lineHeight: '1.4' }}>
                        Ưu tiên cao nhất — lên đầu hàng đợi ngay lập tức.
                      </div>
                    </div>
                  </Space>
                </Card>
              </Col>

              <Col span={12}>
                <Card
                  hoverable
                  onClick={() => handleSelectPriority('PRIORITY')}
                  style={{
                    borderRadius: 8,
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    border: selectedPriority === 'PRIORITY' ? '2px solid #d97706' : '1px solid #e2e8f0',
                    backgroundColor: selectedPriority === 'PRIORITY' ? '#fffbeb' : '#ffffff',
                    boxShadow: selectedPriority === 'PRIORITY' ? '0 2px 8px rgba(217, 119, 6, 0.15)' : 'none',
                  }}
                  bodyStyle={{ padding: 14 }}
                >
                  <Space align="start">
                    <StarOutlined style={{ color: '#d97706', fontSize: 22, marginTop: 2 }} />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: 15, color: '#d97706' }}>
                        ⭐ ĐỐI TƯỢNG ƯU TIÊN
                      </div>
                      <div style={{ fontSize: 12, color: '#92400e', marginTop: 4, lineHeight: '1.4' }}>
                        Người già, trẻ nhỏ, thai phụ... — ưu tiên sau nhóm cấp cứu.
                      </div>
                    </div>
                  </Space>
                </Card>
              </Col>
            </Row>
          </Form.Item>

          {/* Gợi ý lý do nhanh theo mức ưu tiên */}
          <div style={{ marginBottom: 12 }}>
            <Text type="secondary" style={{ fontSize: 13, marginRight: 8 }}>
              Gợi ý lý do nhanh:
            </Text>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 6 }}>
              {currentPresets.map((preset) => (
                <Tag.CheckableTag
                  key={preset}
                  checked={form.getFieldValue('reason') === preset}
                  onChange={() => handleApplyPreset(preset)}
                  style={{
                    padding: '3px 10px',
                    fontSize: 12.5,
                    borderRadius: 4,
                    border: '1px solid #cbd5e1',
                    background: '#f8fafc',
                  }}
                >
                  {preset}
                </Tag.CheckableTag>
              ))}
            </div>
          </div>

          {/* Ô nhập lý do chi tiết */}
          <Form.Item
            label={<strong style={{ fontSize: 14 }}>Lý do ưu tiên khám *</strong>}
            name="reason"
            rules={[
              { required: true, message: 'Vui lòng nhập lý do ưu tiên khám' },
              { max: 500, message: 'Lý do không được vượt quá 500 ký tự' },
            ]}
            tooltip="Ghi chú lâm sàng hoặc tình trạng của bệnh nhân cần được ưu tiên xử trí"
          >
            <Input.TextArea
              rows={3}
              maxLength={500}
              showCount
              placeholder={
                selectedPriority === 'EMERGENCY'
                  ? 'VD: Bệnh nhân sốt cao co giật, khó thở dữ dội...'
                  : 'VD: Bệnh nhân người cao tuổi (>75 tuổi), đi lại khó khăn...'
              }
            />
          </Form.Item>
        </Form>
      </div>
    </Modal>
  )
}
