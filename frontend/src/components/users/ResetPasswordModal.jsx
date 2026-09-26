import React, { useState } from 'react'
import {
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  Button,
  Radio,
  Alert,
  Typography,
  Space,
  message,
  Tooltip,
} from 'antd'
import {
  KeyOutlined,
  CopyOutlined,
  CheckOutlined,
  ExclamationCircleOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import userApi from '../../api/userApi'
import { validatePasswordStrength } from '../../utils/passwordPolicy'

const { Title, Text, Paragraph } = Typography

function ResetPasswordModal({ open, targetUser, onClose, onSuccess }) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [resetMode, setResetMode] = useState('auto') // 'auto' | 'custom'

  // Expiration states
  const [expiryPreset, setExpiryPreset] = useState('24') // '1' | '24' | '72' | '168' | 'custom'
  const [customExpiryValue, setCustomExpiryValue] = useState(24)
  const [customExpiryUnit, setCustomExpiryUnit] = useState('hours') // 'hours' | 'days'

  // Error states
  const [tempPasswordError, setTempPasswordError] = useState(null)
  const [violations, setViolations] = useState([])
  const [generalError, setGeneralError] = useState(null)

  // Success result state (temporary password shown only once)
  const [resetResult, setResetResult] = useState(null)
  const [copied, setCopied] = useState(false)

  const getCalculatedExpiresInHours = () => {
    if (expiryPreset !== 'custom') {
      return Number(expiryPreset)
    }
    const val = Number(customExpiryValue)
    if (!val || isNaN(val) || val < 1) return null
    return customExpiryUnit === 'days' ? val * 24 : val
  }

  const calculatedExpiresInHours = getCalculatedExpiresInHours()
  const isCustomInvalid =
    expiryPreset === 'custom' &&
    (!calculatedExpiresInHours || calculatedExpiresInHours < 1 || calculatedExpiresInHours > 720)

  const formatExpiryDateTime = (dateStr) => {
    if (!dateStr) return null
    const d = dayjs(dateStr)
    return d.isValid() ? d.format('DD/MM/YYYY HH:mm') : String(dateStr)
  }

  const handleClose = () => {
    form.resetFields()
    setResetMode('auto')
    setExpiryPreset('24')
    setCustomExpiryValue(24)
    setCustomExpiryUnit('hours')
    setTempPasswordError(null)
    setViolations([])
    setGeneralError(null)
    setResetResult(null)
    setCopied(false)
    if (onClose) onClose()
  }

  const handleCopyPassword = async () => {
    if (!resetResult?.temporaryPassword) return
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(resetResult.temporaryPassword)
      } else {
        // Fallback for older browsers / non-secure contexts
        const textArea = document.createElement('textarea')
        textArea.value = resetResult.temporaryPassword
        textArea.style.position = 'fixed'
        textArea.style.opacity = '0'
        document.body.appendChild(textArea)
        textArea.focus()
        textArea.select()
        document.execCommand('copy')
        document.body.removeChild(textArea)
      }
      setCopied(true)
      message.success('Đã sao chép mật khẩu tạm vào bộ nhớ tạm!')
      setTimeout(() => setCopied(false), 3000)
    } catch {
      message.error('Không thể tự động sao chép. Vui lòng chọn và sao chép thủ công.')
    }
  }

  const handleSubmit = async (values) => {
    if (!targetUser?.id) return
    if (isCustomInvalid) return

    setTempPasswordError(null)
    setViolations([])
    setGeneralError(null)

    const payload = {
      expiresInHours: calculatedExpiresInHours || 24,
    }

    if (resetMode === 'custom') {
      const customPassword = values.temporaryPassword?.trim() || ''
      if (!customPassword) {
        setTempPasswordError('Vui lòng nhập mật khẩu tạm thời.')
        return
      }

      const clientViolations = validatePasswordStrength(customPassword)
      if (clientViolations.length > 0) {
        setViolations(clientViolations)
        setTempPasswordError('Mật khẩu tạm thời chưa đạt tiêu chuẩn độ mạnh.')
        return
      }

      payload.temporaryPassword = customPassword
    }

    setLoading(true)
    try {
      const response = await userApi.resetPassword(targetUser.id, payload)
      const data = response.data

      // Successfully reset: store response into state ONLY for display.
      // Do NOT log to console or localStorage.
      setResetResult({
        userId: data.userId,
        username: data.username || targetUser.username,
        temporaryPassword: data.temporaryPassword,
        resetAt: data.resetAt,
        tempPasswordExpiresAt: data.tempPasswordExpiresAt,
      })

      if (onSuccess) onSuccess()
    } catch (error) {
      const status = error.response?.status
      const data = error.response?.data || error.apiError || {}
      const code = data.code || error.apiError?.code

      if (code === 'USER_NOT_FOUND' || status === 404) {
        setGeneralError('Không tìm thấy tài khoản nhân viên (có thể tài khoản vừa bị xóa hoặc không tồn tại).')
      } else if (code === 'ACCOUNT_DISABLED' || (status === 403 && String(data.message || '').includes('disabled'))) {
        setGeneralError('Tài khoản nhân viên này đang bị vô hiệu hóa. Không thể đặt lại mật khẩu.')
      } else if (code === 'WEAK_PASSWORD' || (status === 400 && data.details?.violations?.length > 0)) {
        const vList = data.details?.violations || []
        setViolations(vList.length > 0 ? vList : ['Mật khẩu tạm chưa đạt yêu cầu độ mạnh tối thiểu.'])
        setTempPasswordError('Mật khẩu tạm thời chưa đạt tiêu chuẩn độ mạnh.')
      } else if (status === 403) {
        setGeneralError('Bạn không có quyền thực hiện đặt lại mật khẩu cho tài khoản này (yêu cầu quyền USER_RESET_PASSWORD).')
      } else {
        setGeneralError(data.message || 'Có lỗi xảy ra khi đặt lại mật khẩu. Vui lòng thử lại.')
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
      width={520}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <KeyOutlined style={{ color: '#D97706', fontSize: 20 }} />
          <span>Đặt lại mật khẩu nhân viên</span>
        </div>
      }
    >
      {resetResult ? (
        // RESULT SCREEN: displays one-time temporary password and expiration time
        <div style={{ paddingTop: 8 }}>
          <div style={{ textAlign: 'center', marginBottom: 20 }}>
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: '50%',
                backgroundColor: '#DCFCE7',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 10,
              }}
            >
              <CheckCircleOutlined style={{ fontSize: 28, color: '#16A34A' }} />
            </div>
            <Title level={4} style={{ margin: 0, color: '#15803D' }}>
              Đặt lại mật khẩu thành công!
            </Title>
            <Paragraph style={{ color: '#64748B', marginTop: 4, marginBottom: 0, fontSize: 13 }}>
              Tài khoản <strong>@{resetResult.username}</strong> ({targetUser?.fullName}) đã được cấp mật khẩu tạm mới.
            </Paragraph>
          </div>

          <div
            style={{
              backgroundColor: '#F8FAFC',
              border: '2px dashed #CBD5E1',
              borderRadius: 8,
              padding: '16px 20px',
              textAlign: 'center',
              marginBottom: 16,
            }}
          >
            <div style={{ fontSize: 12, color: '#64748B', marginBottom: 6, fontWeight: 500 }}>
              MẬT KHẨU TẠM THỜI MỚI:
            </div>
            <div
              style={{
                fontFamily: "Consolas, 'Courier New', monospace",
                fontSize: 22,
                fontWeight: 700,
                letterSpacing: 2,
                color: '#1E293B',
                marginBottom: 12,
                userSelect: 'all',
              }}
            >
              {resetResult.temporaryPassword}
            </div>

            <Button
              type="primary"
              icon={copied ? <CheckOutlined /> : <CopyOutlined />}
              onClick={handleCopyPassword}
              style={{
                backgroundColor: copied ? '#16A34A' : '#2563EB',
                fontWeight: 600,
              }}
            >
              {copied ? 'Đã sao chép' : 'Sao chép mật khẩu'}
            </Button>
          </div>

          {resetResult.tempPasswordExpiresAt && (
            <div
              style={{
                backgroundColor: '#FEF3C7',
                border: '1px solid #FCD34D',
                borderRadius: 8,
                padding: '12px 16px',
                marginBottom: 16,
                display: 'flex',
                alignItems: 'center',
                gap: 10,
              }}
            >
              <ClockCircleOutlined style={{ color: '#D97706', fontSize: 20, flexShrink: 0 }} />
              <div style={{ fontSize: 13, color: '#92400E', lineHeight: 1.5 }}>
                <span>Mật khẩu tạm thời có hiệu lực đến: </span>
                <strong style={{ color: '#78350F' }}>
                  {formatExpiryDateTime(resetResult.tempPasswordExpiresAt)}
                </strong>
                {calculatedExpiresInHours && (
                  <span style={{ color: '#B45309', marginLeft: 4 }}>
                    ({calculatedExpiresInHours} giờ kể từ lúc đặt lại)
                  </span>
                )}
              </div>
            </div>
          )}

          <Alert
            type="warning"
            showIcon
            icon={<WarningOutlined />}
            message="Lưu ý quan trọng"
            description="Mật khẩu tạm này chỉ hiển thị DUY NHẤT 1 LẦN. Vui lòng sao chép và gửi ngay cho nhân viên kèm thông tin thời hạn hiệu lực. Sau khi hết hạn, nhân viên sẽ không thể đăng nhập. Nhân viên sẽ bị bắt buộc đổi mật khẩu mới trong lần đăng nhập đầu tiên."
            style={{ marginBottom: 20 }}
          />

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button type="primary" onClick={handleClose}>
              Đã hiểu và đóng
            </Button>
          </div>
        </div>
      ) : (
        // FORM SCREEN: selects auto-generate or custom temporary password and expiration period
        <div style={{ paddingTop: 8 }}>
          <Paragraph style={{ color: '#475569', marginBottom: 16, fontSize: 13 }}>
            Đặt lại mật khẩu cho nhân viên <strong>{targetUser?.fullName}</strong> (@{targetUser?.username}).
            Sau khi đặt lại, toàn bộ phiên làm việc của nhân viên sẽ bị thu hồi và nhân viên phải đổi mật khẩu khi đăng nhập.
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
            initialValues={{ temporaryPassword: '' }}
          >
            <Form.Item label={<span style={{ fontWeight: 600 }}>Phương thức tạo mật khẩu</span>} style={{ marginBottom: 16 }}>
              <Radio.Group
                value={resetMode}
                onChange={(e) => {
                  setResetMode(e.target.value)
                  setTempPasswordError(null)
                  setViolations([])
                }}
                style={{ display: 'flex', flexDirection: 'column', gap: 10 }}
              >
                <Radio value="auto">
                  <span>
                    <strong>Tự động tạo mật khẩu ngẫu nhiên</strong> (Khuyên dùng)
                  </span>
                  <div style={{ fontSize: 12, color: '#64748B', paddingLeft: 24 }}>
                    Hệ thống sẽ tự sinh mật khẩu an toàn 10 ký tự gồm chữ hoa, chữ thường và chữ số.
                  </div>
                </Radio>
                <Radio value="custom">
                  <span>
                    <strong>Tự nhập mật khẩu tạm thời</strong>
                  </span>
                  <div style={{ fontSize: 12, color: '#64748B', paddingLeft: 24 }}>
                    Chỉ định thủ công mật khẩu tạm thời theo nhu cầu.
                  </div>
                </Radio>
              </Radio.Group>
            </Form.Item>

            {resetMode === 'custom' && (
              <Form.Item
                label={<span style={{ fontWeight: 600 }}>Mật khẩu tạm thời</span>}
                name="temporaryPassword"
                validateStatus={tempPasswordError ? 'error' : ''}
                help={
                  tempPasswordError && (
                    <div>
                      <div>{tempPasswordError}</div>
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
                  placeholder="Nhập mật khẩu tạm thời (tối thiểu 8 ký tự, hoa, thường, số)"
                  size="large"
                  onChange={() => {
                    setTempPasswordError(null)
                    setViolations([])
                  }}
                />
              </Form.Item>
            )}

            {/* SECTION: Thời gian hiệu lực (áp dụng cho cả 2 phương thức) */}
            <Form.Item
              label={<span style={{ fontWeight: 600 }}>Thời gian hiệu lực</span>}
              style={{ marginBottom: 14 }}
            >
              <Radio.Group
                value={expiryPreset}
                onChange={(e) => setExpiryPreset(e.target.value)}
                style={{ display: 'flex', flexDirection: 'column', gap: 8 }}
              >
                <Radio value="1">1 giờ</Radio>
                <Radio value="24">
                  <span>
                    <strong>24 giờ (1 ngày)</strong> (Mặc định)
                  </span>
                </Radio>
                <Radio value="72">3 ngày (72 giờ)</Radio>
                <Radio value="168">7 ngày (168 giờ)</Radio>
                <Radio value="custom">
                  <span>Tùy chỉnh</span>
                </Radio>
              </Radio.Group>

              {expiryPreset === 'custom' && (
                <div
                  style={{
                    marginTop: 10,
                    marginLeft: 24,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 6,
                  }}
                >
                  <Space.Compact style={{ width: '100%', maxWidth: 260 }}>
                    <InputNumber
                      min={1}
                      max={customExpiryUnit === 'days' ? 30 : 720}
                      value={customExpiryValue}
                      onChange={(val) => setCustomExpiryValue(val)}
                      placeholder={customExpiryUnit === 'days' ? 'Số ngày' : 'Số giờ'}
                      style={{ width: 160 }}
                    />
                    <Select
                      value={customExpiryUnit}
                      onChange={(u) => {
                        setCustomExpiryUnit(u)
                        if (u === 'days' && customExpiryValue > 30) {
                          setCustomExpiryValue(30)
                        }
                      }}
                      style={{ width: 100 }}
                      options={[
                        { value: 'hours', label: 'Giờ' },
                        { value: 'days', label: 'Ngày' },
                      ]}
                    />
                  </Space.Compact>

                  {customExpiryUnit === 'days' && customExpiryValue && (
                    <div style={{ fontSize: 12, color: '#64748B' }}>
                      Quy đổi: <strong>{customExpiryValue * 24} giờ</strong> (Tối đa 30 ngày / 720 giờ)
                    </div>
                  )}

                  {isCustomInvalid && (
                    <div style={{ color: '#DC2626', fontSize: 12 }}>
                      Thời gian hiệu lực tùy chỉnh phải từ 1 đến 720 giờ (1 giờ đến tối đa 30 ngày).
                    </div>
                  )}
                </div>
              )}
            </Form.Item>

            <Alert
              type="info"
              showIcon
              icon={<InfoCircleOutlined />}
              message="Sau khi hết hạn, nhân viên sẽ không thể đăng nhập bằng mật khẩu tạm thời này và cần được cấp lại."
              style={{
                marginBottom: 20,
                backgroundColor: '#EFF6FF',
                borderColor: '#BFDBFE',
              }}
            />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 24 }}>
              <Button onClick={handleClose} disabled={loading}>
                Hủy
              </Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                disabled={loading || isCustomInvalid}
                icon={<KeyOutlined />}
                style={{
                  fontWeight: 600,
                  backgroundColor: isCustomInvalid ? undefined : '#D97706',
                  borderColor: isCustomInvalid ? undefined : '#D97706',
                }}
              >
                Xác nhận đặt lại
              </Button>
            </div>
          </Form>
        </div>
      )}
    </Modal>
  )
}

export default ResetPasswordModal

