import React, { useState } from 'react'
import {
  Modal,
  Form,
  Input,
  Alert,
  Typography,
  Descriptions,
  Tag,
  Space,
  Button,
} from 'antd'
import { CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { useAuthContext } from '../../context/AuthContext.jsx'
import cashierShiftApi from '../../api/cashierShiftApi.js'
import {
  formatCurrency,
  formatDateTime,
  canConfirmShift,
  mapCashierShiftError,
} from '../../utils/cashierShiftHelpers.js'

const { Text } = Typography
const { TextArea } = Input

export default function ConfirmCashierShiftModal({ open, shift, onClose, onSuccess }) {
  const { user } = useAuthContext()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState(null)

  if (!shift) return null

  const isAllowedToConfirm = canConfirmShift(user, shift)
  const currentUserId = user?.id || user?.userId
  const isSelf =
    currentUserId &&
    shift.cashierId &&
    String(currentUserId).trim().toLowerCase() === String(shift.cashierId).trim().toLowerCase()

  const handleFinish = async (values) => {
    setLoading(true)
    setErrorMsg(null)
    try {
      await cashierShiftApi.confirmShift(shift.id, {
        confirmationNotes: values.confirmationNotes?.trim() || null,
      })
      form.resetFields()
      if (onSuccess) onSuccess()
      onClose()
    } catch (err) {
      setErrorMsg(mapCashierShiftError(err))
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    form.resetFields()
    setErrorMsg(null)
    onClose()
  }

  const diffVal = Number(shift.differenceAmount) || 0

  return (
    <Modal
      open={open}
      title={
        <Space>
          <CheckCircleOutlined style={{ color: '#52c41a' }} />
          <span>Xác nhận duyệt phiếu chốt ca ({shift.shiftCode})</span>
        </Space>
      }
      onCancel={handleCancel}
      footer={[
        <Button key="back" onClick={handleCancel} disabled={loading}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={loading}
          disabled={!isAllowedToConfirm}
          onClick={() => form.submit()}
        >
          Xác nhận duyệt phiếu
        </Button>,
      ]}
      destroyOnClose
    >
      {isSelf && (
        <Alert
          type="error"
          showIcon
          message="Không thể tự phê duyệt"
          description="Bạn là người tạo phiếu chốt ca này nên hệ thống không cho phép bạn tự duyệt phiếu của chính mình. Vui lòng để một Quản lý khác xác nhận."
          style={{ marginBottom: 16 }}
        />
      )}

      {errorMsg && (
        <Alert
          type="error"
          showIcon
          message="Duyệt phiếu không thành công"
          description={errorMsg}
          style={{ marginBottom: 16 }}
        />
      )}

      <Descriptions bordered size="small" column={1} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="Mã phiếu">
          <Text strong>{shift.shiftCode}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="Thu ngân">
          <Text strong>{shift.cashierName}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="Thời gian ca">
          {formatDateTime(shift.startTime)} → {formatDateTime(shift.endTime)}
        </Descriptions.Item>
        <Descriptions.Item label="Tổng doanh thu hệ thống">
          <Text strong>{formatCurrency(shift.totalSystemAmount)}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="Tiền mặt hệ thống">
          {formatCurrency(shift.systemCashAmount)}
        </Descriptions.Item>
        <Descriptions.Item label="Tiền mặt thực đếm">
          <Text strong style={{ color: '#389e0d' }}>
            {formatCurrency(shift.actualCashAmount)}
          </Text>
        </Descriptions.Item>
        <Descriptions.Item label="Chênh lệch đối chiếu">
          {diffVal === 0 ? (
            <Tag color="green">Khớp 100% (0 ₫)</Tag>
          ) : diffVal > 0 ? (
            <Tag color="orange" icon={<ExclamationCircleOutlined />}>
              Thừa: +{formatCurrency(diffVal)}
            </Tag>
          ) : (
            <Tag color="red" icon={<ExclamationCircleOutlined />}>
              Thiếu: {formatCurrency(diffVal)}
            </Tag>
          )}
        </Descriptions.Item>
        {shift.notes && (
          <Descriptions.Item label="Giải trình của thu ngân">
            <Text italic>{shift.notes}</Text>
          </Descriptions.Item>
        )}
      </Descriptions>

      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="confirmationNotes"
          label="Ý kiến xác nhận của Quản lý (tùy chọn)"
          rules={[{ max: 1000, message: 'Ý kiến xác nhận không được vượt quá 1000 ký tự.' }]}
        >
          <TextArea
            rows={3}
            placeholder="Nhập ghi chú hoặc ý kiến phê duyệt của bạn..."
            disabled={!isAllowedToConfirm || loading}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
