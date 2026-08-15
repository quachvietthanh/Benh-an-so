import React from 'react'
import { Button, Card, Space, Typography } from 'antd'
import { LogoutOutlined, TeamOutlined } from '@ant-design/icons'

const { Title, Paragraph } = Typography

export default function ManagerPermissionAlert({ user, roles = [], logout, navigate }) {
  return (
    <Card
      style={{
        borderRadius: 12,
        marginBottom: 24,
        border: '1px solid #fed7aa',
        background: '#fffbeb',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16 }}>
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: '50%',
            background: '#fef3c7',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#d97706',
            fontSize: 24,
            flexShrink: 0,
          }}
        >
          <TeamOutlined />
        </div>
        <div style={{ flex: 1 }}>
          <Title level={4} style={{ margin: '0 0 6px 0', color: '#92400e' }}>
            Phân quyền Báo cáo vận hành: Dành riêng cho Quản lý phòng khám (Manager)
          </Title>
          <Paragraph style={{ margin: '0 0 12px 0', color: '#78350f', fontSize: 14 }}>
            Theo chuẩn bảo mật và nghiệp vụ y tế, chức năng tổng hợp báo cáo lượt khám, phân tích tải khám và doanh thu được phân quyền riêng cho tài khoản <strong>Quản lý phòng khám (Clinic Manager)</strong>.
            <br />
            Tài khoản đang đăng nhập hiện tại là <strong>{user?.fullName || user?.username} ({roles.join(', ')})</strong>.
          </Paragraph>
          <Space wrap>
            <Button
              type="primary"
              icon={<LogoutOutlined />}
              style={{ background: '#d97706', borderColor: '#d97706' }}
              onClick={() => {
                if (logout) logout()
                if (navigate) navigate('/login')
              }}
            >
              Đăng xuất để đăng nhập tài khoản Quản lý (`manager1`)
            </Button>
          </Space>
        </div>
      </div>
    </Card>
  )
}
