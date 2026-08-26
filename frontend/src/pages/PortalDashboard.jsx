import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button, Card, Descriptions, Tag } from 'antd'
import {
  MedicineBoxOutlined,
  LogoutOutlined,
  SearchOutlined,
  FileTextOutlined,
  CalendarOutlined,
  UserOutlined,
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
                <UserOutlined style={{ marginRight: 6 }} />
                {user?.username || 'Bệnh nhân'}
              </span>
              <span className="portal-dashboard-user-role">
                <Tag color="blue" style={{ margin: 0 }}>Tài khoản Bệnh nhân</Tag>
              </span>
            </div>
            <Button
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
          <h2>Xin chào, {user?.fullName || user?.username || 'Bệnh nhân'}!</h2>
          <p>
            Chào mừng bạn đến với Cổng thông tin y tế trực tuyến. Tại đây bạn có thể theo dõi kết quả khám bệnh, lịch sử dùng thuốc và thông tin điều trị cá nhân.
          </p>
        </div>

        <div className="portal-dashboard-grid">
          <div className="portal-dashboard-card">
            <div className="portal-dashboard-card-icon blue">
              <SearchOutlined />
            </div>
            <h3>Tra cứu kết quả khám</h3>
            <p>
              Tra cứu nhanh chóng kết quả cận lâm sàng, xét nghiệm và chẩn đoán theo mã lịch hẹn.
            </p>
            <Link to="/portal">
              <Button type="primary" block>
                Đến trang tra cứu
              </Button>
            </Link>
          </div>

          <div className="portal-dashboard-card">
            <div className="portal-dashboard-card-icon green">
              <FileTextOutlined />
            </div>
            <h3>Hồ sơ cá nhân</h3>
            <p>
              Mã hồ sơ bệnh nhân được liên kết an toàn và bảo mật với tài khoản của bạn.
            </p>
            <Button disabled block>
              Mã hồ sơ: {user?.patientId ? String(user.patientId).substring(0, 8) + '...' : 'Đã kết nối'}
            </Button>
          </div>

          <div className="portal-dashboard-card">
            <div className="portal-dashboard-card-icon purple">
              <CalendarOutlined />
            </div>
            <h3>Hỗ trợ y tế</h3>
            <p>
              Cần giải đáp thắc mắc hoặc cập nhật thông tin? Vui lòng liên hệ trực tiếp tại quầy tiếp đón.
            </p>
            <Button type="default" block href="tel:19001000">
              Hotline phòng khám
            </Button>
          </div>
        </div>

        <Card title="Thông tin tài khoản" style={{ borderRadius: 12 }}>
          <Descriptions bordered column={{ xxl: 2, xl: 2, lg: 2, md: 1, sm: 1, xs: 1 }}>
            <Descriptions.Item label="Tên đăng nhập">{user?.username || '—'}</Descriptions.Item>
            <Descriptions.Item label="Vai trò">
              <Tag color="green">Bệnh nhân (PATIENT)</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Mã bệnh nhân (Patient ID)">
              <code>{user?.patientId || 'Đã liên kết'}</code>
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái phiên">
              <Tag color="cyan">Đang hoạt động</Tag>
            </Descriptions.Item>
          </Descriptions>
        </Card>
      </main>
    </div>
  )
}

export default PortalDashboard
