import React, { useState } from 'react'
import { Modal, Form, Input, Button, Alert, Typography, Space, message } from 'antd'
import {
  LockOutlined,
  KeyOutlined,
  ExclamationCircleOutlined,
  LogoutOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import authApi from '../../api/authApi'
import { useAuthContext } from '../../context/AuthContext'
import { validatePasswordStrength } from '../../utils/passwordPolicy'

const { Text, Title, Paragraph } = Typography

function ForceChangePasswordModal() {
  const { user, logout } = useAuthContext()
  const navigate = useNavigate()

  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)

  // Specific separated error states
  const [oldPasswordError, setOldPasswordError] = useState(null)
  const [newPasswordError, setNewPasswordError] = useState(null)
  const [violations, setViolations] = useState([])
  const [generalError, setGeneralError] = useState(null)

  const isOpen = Boolean(user && user.mustChangePassword)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

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

  const handleSubmit = async (values) => {
    const { oldPassword, newPassword, confirmPassword } = values

    // Client-side pre-validation
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

      // Success: backend revokes all user sessions including current session.
      // Must logout and redirect to login page.
      message.success({
        content: 'Đổi mật khẩu thành công! Vui lòng đăng nhập lại bằng mật khẩu mới.',
        duration: 4,
      })
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

  if (!isOpen) return null

  return (
    <Modal
      open={isOpen}
      closable={false}
      maskClosable={false}
      keyboard={false}
      footer={null}
      centered
      width={480}
      className="force-change-password-modal"
    >
      <div style={{ textAlign: 'center', marginBottom: 20 }}>
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            backgroundColor: '#FEF3C7',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: 12,
          }}
        >
          <KeyOutlined style={{ fontSize: 28, color: '#D97706' }} />
        </div>
        <Title level={4} style={{ margin: 0, color: '#1E293B' }}>
          Yêu cầu đổi mật khẩu bắt buộc
        </Title>
        <Paragraph style={{ color: '#64748B', marginTop: 6, marginBottom: 0, fontSize: 13 }}>
          Tài khoản của bạn đang sử dụng mật khẩu tạm thời hoặc được yêu cầu đổi mật khẩu trước khi tiếp tục thao tác trên hệ thống.
        </Paragraph>
      </div>

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
            placeholder="Nhập mật khẩu hiện tại (mật khẩu tạm)"
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
            <div style={{ marginTop: 4 }}>• Tối thiểu 8 ký tự (tối đa 50)</div>
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

        <Space direction="vertical" style={{ width: '100%' }} size={10}>
          <Button
            type="primary"
            htmlType="submit"
            size="large"
            block
            loading={loading}
            icon={<CheckCircleOutlined />}
            style={{ fontWeight: 600, height: 44 }}
          >
            Đổi mật khẩu và tiếp tục
          </Button>

          <Button
            type="text"
            block
            icon={<LogoutOutlined />}
            onClick={handleLogout}
            style={{ color: '#64748B' }}
          >
            Đăng xuất khỏi tài khoản
          </Button>
        </Space>
      </Form>
    </Modal>
  )
}

export default ForceChangePasswordModal
