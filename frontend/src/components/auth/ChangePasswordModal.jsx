import React, { useState } from 'react'
import { Modal, Form, Input, Button, Alert, Typography, message } from 'antd'
import {
  LockOutlined,
  KeyOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import authApi from '../../api/authApi'
import { useAuthContext } from '../../context/AuthContext'
import { validatePasswordStrength } from '../../utils/passwordPolicy'

const { Title, Paragraph } = Typography

function ChangePasswordModal({ open, onClose }) {
  const { logout } = useAuthContext()
  const navigate = useNavigate()

  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)

  // Separated error states
  const [oldPasswordError, setOldPasswordError] = useState(null)
  const [newPasswordError, setNewPasswordError] = useState(null)
  const [violations, setViolations] = useState([])
  const [generalError, setGeneralError] = useState(null)

  const handleValuesChange = (changedValues) => {
    if ('oldPassword' in changedValues) {
      setOldPasswordError(null)
    }
    if ('newPassword' in changedValues) {
      setNewPasswordError(null)
      setViolations([])
    }
    setGeneralError(null)
  }

  const handleClose = () => {
    form.resetFields()
    setOldPasswordError(null)
    setNewPasswordError(null)
    setViolations([])
    setGeneralError(null)
    if (onClose) onClose()
  }

  const handleSubmit = async (values) => {
    const { oldPassword, newPassword, confirmPassword } = values

    let hasClientError = false
    setOldPasswordError(null)
    setNewPasswordError(null)
    setViolations([])
    setGeneralError(null)

    if (!oldPassword || !oldPassword.trim()) {
      setOldPasswordError('Vui lòng nhập mật khẩu hiện tại.')
      hasClientError = true
    }

    if (!newPassword || !newPassword.trim()) {
      setNewPasswordError('Vui lòng nhập mật khẩu mới.')
      hasClientError = true
    } else {
      const strengthViolations = validatePasswordStrength(newPassword)
      if (strengthViolations.length > 0) {
        setViolations(strengthViolations)
        setNewPasswordError('Mật khẩu mới chưa đạt tiêu chuẩn độ mạnh.')
        hasClientError = true
      } else if (oldPassword && newPassword === oldPassword) {
        setNewPasswordError('Mật khẩu mới không được trùng với mật khẩu hiện tại.')
        hasClientError = true
      }
    }

    if (!confirmPassword || !confirmPassword.trim()) {
      form.setFields([{ name: 'confirmPassword', errors: ['Vui lòng xác nhận mật khẩu mới.'] }])
      hasClientError = true
    } else if (newPassword && confirmPassword !== newPassword) {
      form.setFields([{ name: 'confirmPassword', errors: ['Xác nhận mật khẩu mới không khớp.'] }])
      hasClientError = true
    }

    if (hasClientError) return

    setLoading(true)
    try {
      await authApi.changePassword({
        oldPassword,
        newPassword,
      })

      // Backend revokes all sessions including the current one.
      // Must logout and redirect to login page.
      message.success({
        content: 'Đổi mật khẩu thành công! Toàn bộ phiên làm việc cũ đã được thu hồi. Vui lòng đăng nhập lại.',
        duration: 4,
      })
      handleClose()
      logout()
      navigate('/login')
    } catch (error) {
      const status = error.response?.status
      const data = error.response?.data || error.apiError || {}
      const code = data.code || error.apiError?.code

      if (code === 'INVALID_OLD_PASSWORD' || (status === 400 && String(data.message || '').includes('Current password'))) {
        setOldPasswordError('Mật khẩu hiện tại không chính xác.')
      } else if (code === 'SAME_PASSWORD_NOT_ALLOWED' || (status === 400 && String(data.message || '').includes('different'))) {
        setNewPasswordError('Mật khẩu mới không được trùng với mật khẩu hiện tại.')
      } else if (code === 'WEAK_PASSWORD' || (status === 400 && data.details?.violations?.length > 0)) {
        const vList = data.details?.violations || []
        setViolations(vList.length > 0 ? vList : ['Mật khẩu chưa đáp ứng yêu cầu độ mạnh tối thiểu.'])
        setNewPasswordError('Mật khẩu mới chưa đạt tiêu chuẩn độ mạnh.')
      } else if (code === 'ACCOUNT_DISABLED' || status === 403) {
        setGeneralError('Tài khoản của bạn đã bị vô hiệu hóa hoặc khóa. Vui lòng liên hệ quản trị viên.')
      } else {
        setGeneralError(data.message || 'Có lỗi xảy ra khi đổi mật khẩu. Vui lòng thử lại.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={handleClose}
      footer={null}
      centered
      width={480}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <KeyOutlined style={{ color: '#2563EB', fontSize: 20 }} />
          <span>Đổi mật khẩu tài khoản</span>
        </div>
      }
    >
      <Paragraph style={{ color: '#64748B', marginTop: 4, marginBottom: 16, fontSize: 13 }}>
        Sau khi đổi mật khẩu thành công, toàn bộ phiên đăng nhập hiện tại sẽ bị thu hồi và bạn cần đăng nhập lại bằng mật khẩu mới.
      </Paragraph>

      {generalError && (
        <Alert
          type="error"
          showIcon
          icon={<ExclamationCircleOutlined />}
          message={generalError}
          style={{ marginBottom: 16 }}
        />
      )}

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        onValuesChange={handleValuesChange}
        requiredMark={false}
      >
        <Form.Item
          label={<span style={{ fontWeight: 600 }}>Mật khẩu hiện tại</span>}
          name="oldPassword"
          validateStatus={oldPasswordError ? 'error' : ''}
          help={oldPasswordError}
          style={{ marginBottom: 16 }}
        >
          <Input.Password
            prefix={<LockOutlined style={{ color: '#94A3B8' }} />}
            placeholder="Nhập mật khẩu hiện tại"
            size="large"
            autoComplete="current-password"
          />
        </Form.Item>

        <Form.Item
          label={<span style={{ fontWeight: 600 }}>Mật khẩu mới</span>}
          name="newPassword"
          validateStatus={newPasswordError ? 'error' : ''}
          help={
            newPasswordError && (
              <div>
                <div>{newPasswordError}</div>
                {violations.length > 0 && (
                  <ul style={{ margin: '4px 0 0 16px', padding: 0, color: '#DC2626', fontSize: 12 }}>
                    {violations.map((v, idx) => (
                      <li key={idx}>{v}</li>
                    ))}
                  </ul>
                )}
              </div>
            )
          }
          style={{ marginBottom: 16 }}
        >
          <Input.Password
            prefix={<KeyOutlined style={{ color: '#94A3B8' }} />}
            placeholder="Nhập mật khẩu mới"
            size="large"
            autoComplete="new-password"
          />
        </Form.Item>

        {violations.length === 0 && !newPasswordError && (
          <div
            style={{
              fontSize: 12,
              color: '#64748B',
              backgroundColor: '#F8FAFC',
              border: '1px solid #E2E8F0',
              borderRadius: 6,
              padding: '8px 12px',
              marginBottom: 16,
            }}
          >
            <strong>Tiêu chuẩn mật khẩu:</strong>
            <div style={{ marginTop: 4 }}>• Tối thiểu 8 ký tự (tối đa 50 ký tự)</div>
            <div>• Chứa ít nhất 1 chữ hoa (A-Z), 1 chữ thường (a-z) và 1 số (0-9)</div>
            <div>• Không trùng với mật khẩu hiện tại</div>
          </div>
        )}

        <Form.Item
          label={<span style={{ fontWeight: 600 }}>Xác nhận mật khẩu mới</span>}
          name="confirmPassword"
          dependencies={['newPassword']}
          style={{ marginBottom: 24 }}
        >
          <Input.Password
            prefix={<KeyOutlined style={{ color: '#94A3B8' }} />}
            placeholder="Nhập lại mật khẩu mới"
            size="large"
            autoComplete="new-password"
          />
        </Form.Item>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
          <Button onClick={handleClose} disabled={loading}>
            Hủy
          </Button>
          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            icon={<CheckCircleOutlined />}
            style={{ fontWeight: 600 }}
          >
            Cập nhật mật khẩu
          </Button>
        </div>
      </Form>
    </Modal>
  )
}

export default ChangePasswordModal
