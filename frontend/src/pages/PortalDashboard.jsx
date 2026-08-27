import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button, Card, Descriptions, Tooltip, message } from 'antd'
import {
  MedicineBoxOutlined,
  LogoutOutlined,
  SearchOutlined,
  FileTextOutlined,
  UserOutlined,
  IdcardOutlined,
  SafetyCertificateOutlined,
  CheckCircleOutlined,
  CopyOutlined,
  ArrowRightOutlined,
  HeartFilled,
  BarcodeOutlined,
} from '@ant-design/icons'
import { useAuthContext } from '../context/AuthContext'
import './portalDashboard.css'

function PortalDashboard() {
  const { user, logout } = useAuthContext()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/portal/login', { replace: true })
  }

  const handleCopy = (text, label) => {
    if (!text) return
    navigator.clipboard.writeText(text)
    message.success(`Đã sao chép ${label}!`)
  }

  const greetingName = user?.fullName && user.fullName !== user.username
    ? user.fullName
    : (user?.username || '')

  const patientIdStr = user?.patientId ? String(user.patientId) : ''
  const displayIdShort = patientIdStr ? `${patientIdStr.substring(0, 8)}...` : 'Đã kết nối'

  return (
    <div className="portal-dashboard-page">
      <header className="portal-dashboard-header">
        <div className="portal-dashboard-header-inner">
          <Link className="portal-dashboard-brand" to="/portal/dashboard">
            <span className="portal-dashboard-brand-icon">
              <MedicineBoxOutlined />
            </span>
            <span>
              <strong>BỆNH ÁN SỐ</strong>
              <small>Cổng thông tin bệnh nhân</small>
            </span>
          </Link>

          <div className="portal-dashboard-user-bar">
            <div className="portal-dashboard-user-info">
              <span className="portal-dashboard-user-name">
                <UserOutlined style={{ marginRight: 6, color: '#1677ff' }} />
                {user?.username || 'Bệnh nhân'}
              </span>
              <span className="portal-badge-patient">
                Tài khoản Bệnh nhân
              </span>
            </div>
            <Button
              className="portal-logout-btn"
              danger
              icon={<LogoutOutlined />}
              onClick={handleLogout}
            >
              Đăng xuất
            </Button>
          </div>
        </div>
      </header>

      <main className="portal-dashboard-main">
        <div className="portal-dashboard-welcome-card">
          <div className="portal-welcome-decoration-circle" aria-hidden="true" />
          <svg className="portal-welcome-pulse" viewBox="0 0 400 40" aria-hidden="true">
            <path
              d="M0 20 L80 20 L95 20 L105 5 L115 35 L125 20 L145 20 L400 20"
              fill="none"
              stroke="rgba(255,255,255,0.22)"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>

          <div className="portal-welcome-content">
            <div className="portal-welcome-pill">
              <HeartFilled style={{ color: '#ff7875', marginRight: 6 }} /> Cổng thông tin y tế trực tuyến
            </div>
            <h1 className="portal-welcome-title">Xin chào{greetingName ? `, ${greetingName}` : ''}! 👋</h1>
            <p className="portal-welcome-desc">
              Theo dõi kết quả khám bệnh, lịch sử dùng thuốc và thông tin điều trị cá nhân nhanh chóng và an toàn.
            </p>
          </div>
        </div>

        <div className="portal-dashboard-grid">
          <div className="portal-dashboard-card portal-card-lookup">
            <div className="portal-dashboard-card-top">
              <div className="portal-dashboard-card-icon blue">
                <SearchOutlined />
              </div>
              <div className="portal-card-header-text">
                <h3>Tra cứu kết quả khám</h3>
                <span className="portal-card-tag blue">Nhanh chóng</span>
              </div>
            </div>
            <p className="portal-card-desc">
              Tra cứu trực tuyến kết quả cận lâm sàng, xét nghiệm và chẩn đoán theo mã lịch hẹn.
            </p>
            <div className="portal-card-action">
              <Link to="/portal" style={{ width: '100%' }}>
                <Button type="primary" className="portal-btn-primary" block>
                  Đến trang tra cứu <ArrowRightOutlined />
                </Button>
              </Link>
            </div>
          </div>

          <div className="portal-dashboard-card portal-card-patient">
            <div className="portal-dashboard-card-top">
              <div className="portal-dashboard-card-icon green">
                <FileTextOutlined />
              </div>
              <div className="portal-card-header-text">
                <h3>Hồ sơ bệnh nhân</h3>
                <span className="portal-card-tag green">Đã liên kết</span>
              </div>
            </div>
            <p className="portal-card-desc">
              Hồ sơ y tế được mã hóa và đồng bộ trực tiếp với hệ thống phòng khám.
            </p>
            <div className="portal-card-action">
              <div className="portal-id-chip-box">
                <div className="portal-id-chip-content">
                  <span className="portal-id-chip-label">MÃ HỒ SƠ</span>
                  <strong className="portal-id-chip-value">
                    {user?.patientCode ? user.patientCode : displayIdShort}
                  </strong>
                </div>
                {patientIdStr && (
                  <Tooltip title="Sao chép mã hồ sơ đầy đủ">
                    <Button
                      type="text"
                      className="portal-copy-icon-btn"
                      icon={<CopyOutlined />}
                      onClick={() => handleCopy(patientIdStr, 'mã hồ sơ')}
                    />
                  </Tooltip>
                )}
              </div>
            </div>
          </div>
        </div>

        <Card
          className="portal-account-card"
          title={
            <div className="portal-account-card-title">
              <SafetyCertificateOutlined style={{ color: '#1677ff', fontSize: 18, marginRight: 8 }} />
              <span>Thông tin tài khoản</span>
            </div>
          }
        >
          <Descriptions
            bordered
            column={{ xxl: 2, xl: 2, lg: 2, md: 1, sm: 1, xs: 1 }}
            className="portal-descriptions"
          >
            <Descriptions.Item
              label={
                <span className="portal-desc-label">
                  <UserOutlined style={{ color: '#64748b', marginRight: 6 }} /> Tên đăng nhập
                </span>
              }
            >
              <strong style={{ color: '#0f172a', fontSize: 14 }}>{user?.username || '—'}</strong>
            </Descriptions.Item>

            <Descriptions.Item
              label={
                <span className="portal-desc-label">
                  <SafetyCertificateOutlined style={{ color: '#10b981', marginRight: 6 }} /> Vai trò
                </span>
              }
            >
              <span className="portal-status-pill green">
                <span className="portal-status-dot green" />
                Bệnh nhân (PATIENT)
              </span>
            </Descriptions.Item>

            {user?.patientCode && (
              <Descriptions.Item
                label={
                  <span className="portal-desc-label">
                    <BarcodeOutlined style={{ color: '#096dd9', marginRight: 6 }} /> Mã y tế (Mã BN)
                  </span>
                }
              >
                <span className="portal-status-pill blue">
                  {user.patientCode}
                </span>
              </Descriptions.Item>
            )}

            <Descriptions.Item
              label={
                <span className="portal-desc-label">
                  <IdcardOutlined style={{ color: '#6366f1', marginRight: 6 }} /> Mã bệnh nhân (Patient ID)
                </span>
              }
            >
              <div className="portal-copyable-code-row">
                <code className="portal-code-text">{user?.patientId || 'Đã liên kết'}</code>
                {patientIdStr && (
                  <Tooltip title="Sao chép">
                    <CopyOutlined
                      className="portal-inline-copy"
                      onClick={() => handleCopy(patientIdStr, 'mã bệnh nhân')}
                    />
                  </Tooltip>
                )}
              </div>
            </Descriptions.Item>

            <Descriptions.Item
              label={
                <span className="portal-desc-label">
                  <CheckCircleOutlined style={{ color: '#06b6d4', marginRight: 6 }} /> Trạng thái phiên
                </span>
              }
            >
              <span className="portal-status-pill cyan">
                <span className="portal-status-dot cyan pulse" />
                Đang hoạt động
              </span>
            </Descriptions.Item>
          </Descriptions>
        </Card>
      </main>
    </div>
  )
}

export default PortalDashboard
