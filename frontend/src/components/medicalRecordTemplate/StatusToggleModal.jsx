import React, { useEffect, useState } from 'react'
import {
  Alert,
  Form,
  Modal,
  Select,
  Tag,
  Typography,
} from 'antd'
import {
  ExclamationCircleOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons'

const { Text, Paragraph } = Typography

function StatusToggleModal({
  open,
  onClose,
  template = null,
  otherTemplatesInSpecialty = [],
  onSubmit,
  loading = false,
}) {
  const [form] = Form.useForm()
  const [serverError, setServerError] = useState('')

  const isCurrentActive = Boolean(template?.active)
  const isDefault = Boolean(template?.defaultTemplate)
  const targetActive = !isCurrentActive

  const availableReplacements = otherTemplatesInSpecialty.filter(
    (t) => t.id !== template?.id && t.active
  )

  const requiresReplacement = isCurrentActive && isDefault && availableReplacements.length > 0
  const isLastActiveDefault = isCurrentActive && isDefault && availableReplacements.length === 0

  useEffect(() => {
    if (open) {
      setServerError('')
      form.resetFields()
      if (availableReplacements.length > 0) {
        form.setFieldsValue({
          replacementTemplateId: availableReplacements[0]?.id,
        })
      }
    }
  }, [open, template, form])

  const handleOk = async () => {
    setServerError('')
    try {
      let replacementId = null
      if (requiresReplacement) {
        const values = await form.validateFields()
        replacementId = values.replacementTemplateId
      }

      await onSubmit({
        active: targetActive,
        replacementTemplateId: replacementId || undefined,
      })
    } catch (err) {
      if (err.errorFields) return
      const msg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        'Có lỗi xảy ra khi cập nhật trạng thái mẫu bệnh án.'
      setServerError(msg)
    }
  }

  if (!template) return null

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {targetActive ? (
            <>
              <PlayCircleOutlined style={{ color: '#059669', fontSize: 18 }} />
              <span>Kích hoạt lại mẫu bệnh án</span>
            </>
          ) : (
            <>
              <PauseCircleOutlined style={{ color: '#dc2626', fontSize: 18 }} />
              <span>Ngừng áp dụng mẫu bệnh án</span>
            </>
          )}
        </div>
      }
      open={open}
      onCancel={onClose}
      onOk={handleOk}
      confirmLoading={loading}
      okText={targetActive ? 'Kích hoạt mẫu' : 'Xác nhận ngừng áp dụng'}
      okButtonProps={{ danger: !targetActive, disabled: isLastActiveDefault }}
      cancelText="Đóng"
      destroyOnClose
    >
      {serverError && (
        <Alert
          type="error"
          showIcon
          message="Lỗi cập nhật trạng thái"
          description={serverError}
          style={{ marginBottom: 16 }}
          closable
          onClose={() => setServerError('')}
        />
      )}

      <div style={{ marginBottom: 16 }}>
        <Text>
          Bạn có chắc chắn muốn {targetActive ? 'kích hoạt lại' : 'ngừng áp dụng'} mẫu bệnh án:
        </Text>
        <div
          style={{
            margin: '8px 0',
            padding: '10px 14px',
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: 6,
          }}
        >
          <Text strong style={{ fontSize: 14 }}>
            {template.name}
          </Text>
          <div style={{ marginTop: 4, display: 'flex', gap: 6, alignItems: 'center' }}>
            <Tag color="blue">{template.specialty?.name || 'Chuyên khoa'}</Tag>
            {isDefault && <Tag color="green">Mẫu mặc định</Tag>}
            <Tag color={isCurrentActive ? 'success' : 'default'}>
              {isCurrentActive ? 'Đang áp dụng' : 'Ngừng áp dụng'}
            </Tag>
          </div>
        </div>
      </div>

      {isLastActiveDefault && (
        <Alert
          type="error"
          showIcon
          icon={<ExclamationCircleOutlined />}
          message="Không thể ngừng áp dụng mẫu duy nhất"
          description="Đây là mẫu bệnh án duy nhất đang hoạt động của chuyên khoa này. Mỗi chuyên khoa phải có ít nhất 1 mẫu mặc định hoạt động. Vui lòng tạo thêm mẫu mới hoặc kích hoạt mẫu khác trước khi tắt mẫu này."
          style={{ marginBottom: 16 }}
        />
      )}

      {requiresReplacement && (
        <div>
          <Alert
            type="warning"
            showIcon
            icon={<ExclamationCircleOutlined />}
            message="Chỉ định mẫu mặc định thay thế"
            description="Mẫu này hiện đang là mẫu mặc định cho chuyên khoa. Vui lòng chọn một mẫu khác thay thế làm mẫu mặc định mới trước khi ngừng áp dụng."
            style={{ marginBottom: 16 }}
          />

          <Form form={form} layout="vertical">
            <Form.Item
              name="replacementTemplateId"
              label={<span style={{ fontWeight: 600 }}>Chọn mẫu thay thế làm mặc định mới:</span>}
              rules={[{ required: true, message: 'Vui lòng chọn mẫu thay thế' }]}
            >
              <Select
                placeholder="Chọn mẫu thay thế..."
                options={availableReplacements.map((t) => ({
                  value: t.id,
                  label: `${t.name} (v${t.currentVersionNo})`,
                }))}
              />
            </Form.Item>
          </Form>
        </div>
      )}

      {!targetActive && !isDefault && (
        <Paragraph type="secondary" style={{ fontSize: 13 }}>
          Khi ngừng áp dụng, bác sĩ sẽ không thấy mẫu này trong danh sách chọn khi khám bệnh mới. Các hồ sơ bệnh án cũ đã tạo theo mẫu này vẫn được lưu trữ bình thường.
        </Paragraph>
      )}
    </Modal>
  )
}

export default StatusToggleModal
