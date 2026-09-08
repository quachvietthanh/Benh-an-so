import React, { useState } from 'react'
import { Layout, Typography, Space, Avatar, Dropdown } from 'antd'
import { UserOutlined, LogoutOutlined, SettingOutlined, KeyOutlined } from '@ant-design/icons'
import { useAuthContext } from '../../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import ChangePasswordModal from '../auth/ChangePasswordModal'

const { Header: AntHeader } = Layout
const { Text } = Typography

function Header() {
  const { user, logout } = useAuthContext()
  const navigate = useNavigate()
  const [changePasswordOpen, setChangePasswordOpen] = useState(false)

  const items = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Thông tin cá nhân',
    },
    {
      key: 'change-password',
      icon: <KeyOutlined />,
      label: 'Đổi mật khẩu',
      onClick: () => setChangePasswordOpen(true),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: 'Cài đặt',
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Đăng xuất',
      danger: true,
      onClick: () => {
        logout()
        navigate('/login')
      },
    },
  ]

  return (
    <AntHeader>
      <Space>
        <Text strong style={{ fontSize: 16 }}>Bệnh số án</Text>
      </Space>

      <Dropdown menu={{ items }} placement="bottomRight">
        <Space style={{ cursor: 'pointer' }}>
          <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#1890ff' }} />
          <Text>{user?.fullName || user?.username}</Text>
        </Space>
      </Dropdown>
      <ChangePasswordModal
        open={changePasswordOpen}
        onClose={() => setChangePasswordOpen(false)}
      />
    </AntHeader>
  )
}

export default Header
