import React, { useMemo, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Avatar, Badge, Dropdown, Input, Layout, Menu, Tooltip } from 'antd'
import {
  BellOutlined,
  CaretDownOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SearchOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useAuthContext } from '../../context/AuthContext'
import { getNavigationItems } from '../../services/mockDataService'

const { Header, Sider, Content } = Layout

const roleNames = {
  admin: 'Quản trị viên',
  manager: 'Quản lý',
  doctor: 'Bác sĩ',
  receptionist: 'Lễ tân',
  pharmacist: 'Dược sĩ',
}

const navigationSections = [
  { key: 'overview', paths: ['/'] },
  { key: 'reception', label: 'Tiếp nhận & Chăm sóc', paths: ['/patients', '/appointments'] },
  { key: 'examination', label: 'Khám bệnh', paths: ['/medical-records', '/prescriptions'] },
  { key: 'pharmacy', label: 'Nhà thuốc', paths: ['/pharmacy'] },
  { key: 'finance', label: 'Tài chính', paths: ['/billing'] },
  { key: 'reports', label: 'Báo cáo', paths: ['/reports'] },
  { key: 'system', label: 'Hệ thống', paths: ['/system-management'] },
  { key: 'lookup', label: 'Tra cứu', paths: ['/public-lookup'] },
]

function MainLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuthContext()

  const navigationItems = useMemo(
    () => getNavigationItems(user?.roles || []).map((item) => ({
      key: item.key,
      icon: React.createElement(item.icon),
      label: item.label,
      title: item.label,
    })),
    [user?.roles],
  )



  const sidebarItems = useMemo(() => navigationSections.flatMap((section) => {
    const items = section.paths
      .map((path) => navigationItems.find((item) => item.key === path))
      .filter(Boolean)

    if (!items.length) return []
    if (!section.label) return items

    return [{
      type: 'group',
      key: `group-${section.key}`,
      label: section.label,
      children: items,
    }]
  }), [navigationItems])

  const selectedPath = useMemo(() => {
    const match = navigationItems
      .filter((item) => item.key === '/' ? location.pathname === '/' : location.pathname.startsWith(item.key))
      .sort((a, b) => b.key.length - a.key.length)[0]
    return match?.key || location.pathname
  }, [location.pathname, navigationItems])

  const primaryRole = user?.roles?.[0] || 'doctor'
  const displayName = user?.fullName || user?.username || 'Nguyễn Văn A'

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const userMenuItems = [
    { key: 'profile', icon: <UserOutlined />, label: 'Thông tin cá nhân' },
    { key: 'settings', icon: <SettingOutlined />, label: 'Cài đặt tài khoản' },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true, onClick: handleLogout },
  ]

  return (
    <Layout className="clinic-shell">
      <Sider
        className="clinic-sider"
        trigger={null}
        collapsible
        collapsed={collapsed}
        collapsedWidth={78}
        width={240}
        theme="dark"
      >
        <button type="button" className="clinic-brand" onClick={() => navigate('/')}>
          <span className="clinic-brand-icon"><MedicineBoxOutlined /></span>
          {!collapsed && (
            <span className="clinic-brand-copy">
              <strong>BỆNH ÁN SỐ</strong>
              <small>Hệ thống quản lý phòng khám</small>
            </span>
          )}
        </button>

        <Menu
          className="clinic-menu"
          theme="dark"
          mode="inline"
          selectedKeys={[selectedPath]}
          items={sidebarItems}
          inlineIndent={16}
          onClick={({ key }) => navigate(key)}
        />

        <Tooltip title={collapsed ? 'Mở rộng menu' : ''} placement="right">
          <button
            type="button"
            className="sidebar-collapse"
            onClick={() => setCollapsed((value) => !value)}
            aria-label={collapsed ? 'Mở rộng menu' : 'Thu gọn menu'}
          >
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            {!collapsed && <span>Thu gọn menu</span>}
          </button>
        </Tooltip>
      </Sider>

      <Layout className="clinic-main-layout">
        <Header className="clinic-header">
          <Input
            className="clinic-search"
            prefix={<SearchOutlined />}
            placeholder="Tìm kiếm bệnh nhân, lịch hẹn..."
            allowClear
          />

          <div className="clinic-header-actions">
            <Badge count={3} size="small" offset={[-2, 3]}>
              <button type="button" className="notification-button" aria-label="Thông báo">
                <BellOutlined />
              </button>
            </Badge>

            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
              <button type="button" className="header-user">
                <Avatar className="header-avatar" icon={<UserOutlined />} />
                <span className="header-user-copy">
                  <strong>{displayName}</strong>
                  <small>{roleNames[primaryRole] || primaryRole}</small>
                </span>
                <CaretDownOutlined />
              </button>
            </Dropdown>
          </div>
        </Header>

        <Content className="clinic-content">
          <div className="page-transition">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout
