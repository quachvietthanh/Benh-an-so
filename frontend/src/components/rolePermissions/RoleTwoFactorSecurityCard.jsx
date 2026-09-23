import React, { useState } from 'react'
import { Card, Switch, Typography, Space, Modal, message, Tag, Row, Col } from 'antd'
import { SafetyCertificateOutlined, ExclamationCircleOutlined, LockOutlined } from '@ant-design/icons'
import twoFactorAuthApi from '../../api/twoFactorAuthApi.js'

const { Text, Title } = Typography

function RoleTwoFactorSecurityCard({ userPermissions = [], userRoles = [] }) {
  // Chỉ hiển thị khi người dùng có quyền TWO_FACTOR_AUTH_MANAGE hoặc là Quản trị viên
  const normalizedPerms = userPermissions.map((p) =>
    String(p || '').toUpperCase().replace(/^PERMISSION_/, '')
  )
  const isAdministrator = userRoles.some((r) =>
    String(r || '').toUpperCase().includes('ADMIN')
  )
  const canManage2FA = normalizedPerms.includes('TWO_FACTOR_AUTH_MANAGE') || isAdministrator

  if (!canManage2FA) {
    return null
  }

  // Khởi tạo trạng thái từ bộ nhớ đệm
  const [admin2FA, setAdmin2FA] = useState(() => {
    return localStorage.getItem('2fa_role_ADMIN') === 'true'
  })
  const [doctor2FA, setDoctor2FA] = useState(() => {
    return localStorage.getItem('2fa_role_DOCTOR') === 'true'
  })

  // Trạng thái chống double-submit
  const [submittingRole, setSubmittingRole] = useState(null)

  const handleToggle = (roleName, roleDisplayName, currentVal) => {
    const nextVal = !currentVal
    const actionText = nextVal ? 'bật' : 'tắt'

    Modal.confirm({
      title: `Xác nhận ${actionText} xác thực hai lớp`,
      icon: <ExclamationCircleOutlined style={{ color: nextVal ? '#0284c7' : '#f59e0b' }} />,
      content: nextVal
        ? `Bật xác thực hai lớp sẽ yêu cầu TẤT CẢ tài khoản ${roleDisplayName} phải nhập thêm mã xác thực khi đăng nhập từ lần sau. Xác nhận?`
        : `Tắt xác thực hai lớp sẽ cho phép các tài khoản ${roleDisplayName} đăng nhập chỉ bằng mật khẩu. Bạn có chắc chắn muốn tắt?`,
      okText: nextVal ? 'Xác nhận bật' : 'Xác nhận tắt',
      cancelText: 'Hủy bỏ',
      centered: true,
      okButtonProps: {
        danger: !nextVal,
        style: {
          height: 42,
          minWidth: 120,
          fontWeight: 600,
          fontSize: 14,
          borderRadius: 8,
        },
      },
      cancelButtonProps: {
        style: {
          height: 42,
          minWidth: 100,
          fontSize: 14,
          borderRadius: 8,
        },
      },
      onOk: async () => {
        setSubmittingRole(roleName)
        try {
          await twoFactorAuthApi.configureRole(roleName, nextVal)
          if (roleName === 'ADMIN') {
            setAdmin2FA(nextVal)
            localStorage.setItem('2fa_role_ADMIN', String(nextVal))
          } else if (roleName === 'DOCTOR') {
            setDoctor2FA(nextVal)
            localStorage.setItem('2fa_role_DOCTOR', String(nextVal))
          }
          message.success(`Đã ${actionText} xác thực hai lớp cho ${roleDisplayName} thành công!`)
        } catch (error) {
          const errMsg =
            error.response?.data?.message ||
            `Không thể ${actionText} xác thực hai lớp cho ${roleDisplayName}. Vui lòng thử lại.`
          message.error(errMsg)
        } finally {
          setSubmittingRole(null)
        }
      },
    })
  }

  return (
    <Card
      size="small"
      style={{
        marginBottom: 16,
        borderRadius: 12,
        border: '1px solid #bae6fd',
        background: '#f0f9ff',
        boxShadow: '0 2px 8px rgba(14, 165, 233, 0.08)',
      }}
      bodyStyle={{ padding: '16px 20px' }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
        <SafetyCertificateOutlined style={{ fontSize: 20, color: '#0284c7' }} />
        <Title level={5} style={{ margin: 0, color: '#0369a1', fontWeight: 600 }}>
          Cấu hình Xác thực hai lớp (2FA) cho tài khoản có quyền cao
        </Title>
      </div>

      <Row gutter={[16, 12]}>
        {/* Vai trò Quản trị viên (ADMIN) */}
        <Col xs={24} md={12}>
          <div
            style={{
              padding: '12px 16px',
              backgroundColor: '#ffffff',
              borderRadius: 10,
              border: `1.5px solid ${admin2FA ? '#38bdf8' : '#e2e8f0'}`,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              boxShadow: admin2FA ? '0 2px 8px rgba(56, 189, 248, 0.15)' : 'none',
              transition: 'all 0.2s ease',
            }}
          >
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <LockOutlined style={{ color: admin2FA ? '#0284c7' : '#94a3b8' }} />
                <Text strong style={{ fontSize: 14, color: '#1e293b' }}>
                  Quản trị viên (ADMIN)
                </Text>
                {admin2FA ? (
                  <Tag color="success" style={{ margin: 0, borderRadius: 10 }}>
                    Đã bật 2FA
                  </Tag>
                ) : (
                  <Tag style={{ margin: 0, borderRadius: 10 }}>Chưa kích hoạt</Tag>
                )}
              </div>
              <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>
                Bắt buộc nhập mã xác thực mô phỏng khi đăng nhập
              </div>
            </div>

            <Switch
              checked={admin2FA}
              loading={submittingRole === 'ADMIN'}
              onChange={() => handleToggle('ADMIN', 'Quản trị viên', admin2FA)}
              style={{
                backgroundColor: admin2FA ? '#0284c7' : undefined,
              }}
            />
          </div>
        </Col>

        {/* Vai trò Bác sĩ (DOCTOR) */}
        <Col xs={24} md={12}>
          <div
            style={{
              padding: '12px 16px',
              backgroundColor: '#ffffff',
              borderRadius: 10,
              border: `1.5px solid ${doctor2FA ? '#38bdf8' : '#e2e8f0'}`,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              boxShadow: doctor2FA ? '0 2px 8px rgba(56, 189, 248, 0.15)' : 'none',
              transition: 'all 0.2s ease',
            }}
          >
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <LockOutlined style={{ color: doctor2FA ? '#0284c7' : '#94a3b8' }} />
                <Text strong style={{ fontSize: 14, color: '#1e293b' }}>
                  Bác sĩ điều trị (DOCTOR)
                </Text>
                {doctor2FA ? (
                  <Tag color="success" style={{ margin: 0, borderRadius: 10 }}>
                    Đã bật 2FA
                  </Tag>
                ) : (
                  <Tag style={{ margin: 0, borderRadius: 10 }}>Chưa kích hoạt</Tag>
                )}
              </div>
              <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>
                Bảo vệ tài khoản có quyền truy cập dữ liệu lâm sàng và bệnh án
              </div>
            </div>

            <Switch
              checked={doctor2FA}
              loading={submittingRole === 'DOCTOR'}
              onChange={() => handleToggle('DOCTOR', 'Bác sĩ', doctor2FA)}
              style={{
                backgroundColor: doctor2FA ? '#0284c7' : undefined,
              }}
            />
          </div>
        </Col>
      </Row>
    </Card>
  )
}

export default RoleTwoFactorSecurityCard
