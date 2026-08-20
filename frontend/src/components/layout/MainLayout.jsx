import React, { useMemo, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { AutoComplete, Avatar, Badge, Drawer, Dropdown, Input, Layout, Menu, Tooltip } from 'antd'
import {
  BellOutlined,
  CalendarOutlined,
  CaretDownOutlined,
  DashboardOutlined,
  FileTextOutlined,
  FormOutlined,
  InboxOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
  MenuFoldOutlined,
  MenuOutlined,
  MenuUnfoldOutlined,
  SearchOutlined,
  SettingOutlined,
  ShopOutlined,
  SolutionOutlined,
  UserOutlined,
} from '@ant-design/icons'
import patientApi from '../../api/patientApi'
import { useAuthContext } from '../../context/AuthContext'

const { Header, Sider, Content } = Layout

const roleNames = {
  admin: 'Quản trị viên',
  manager: 'Quản lý',
  clinic_manager: 'Quản lý phòng khám',
  doctor: 'Bác sĩ',
  receptionist: 'Lễ tân',
  pharmacist: 'Dược sĩ',
}

const navigationSections = [
  { key: 'overview', paths: ['/'] },
  { key: 'reception', label: 'Tiếp nhận & Chăm sóc', paths: ['/patients', '/appointments'] },
  { key: 'examination', label: 'Khám bệnh', paths: ['/medical-records', '/prescriptions', '/clinical-results', '/results'] },
  { key: 'pharmacy', label: 'Nhà thuốc', paths: ['/pharmacy', '/medicines', '/pharmacy/receipts'] },
  { key: 'finance', label: 'Tài chính', paths: ['/billing'] },
  { key: 'reports', label: 'Báo cáo', paths: ['/reports'] },
  { key: 'system', label: 'Hệ thống', paths: ['/system-management'] },
  { key: 'lookup', label: 'Tra cứu', paths: ['/public-lookup'] },
]

const getNavigationItems = (roles = []) => {
  const normalizedRoles = (Array.isArray(roles) ? roles : [roles])
    .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)

  if (!normalizedRoles.length) return []

  const items = [
    { key: '/', label: 'Tổng quan', icon: DashboardOutlined, roles: ['admin', 'manager', 'doctor', 'nurse', 'receptionist', 'pharmacist'] },
    { key: '/patients', label: 'Quản lý hồ sơ bệnh nhân', icon: UserOutlined, roles: ['admin', 'doctor', 'receptionist'] },
    { key: '/appointments', label: 'Lịch hẹn và hàng đợi khám', icon: CalendarOutlined, roles: ['admin', 'doctor', 'nurse', 'receptionist'] },
    { key: '/medical-records', label: 'Khám bệnh & Bệnh án', icon: SolutionOutlined, roles: ['admin', 'doctor'] },
    { key: '/prescriptions', label: 'Kê đơn thuốc', icon: FormOutlined, roles: ['admin', 'doctor'] },
    { key: '/clinical-results', label: 'Nhập kết quả CĐLS', icon: FileTextOutlined, roles: ['admin', 'doctor'] },
    { key: '/pharmacy', label: 'Cấp phát thuốc', icon: MedicineBoxOutlined, roles: ['admin', 'pharmacist'] },
    { key: '/medicines', label: 'Danh mục & Ngưỡng tồn', icon: ShopOutlined, roles: ['admin', 'pharmacist'] },
    { key: '/pharmacy/receipts', label: 'Nhập kho theo lô', icon: InboxOutlined, roles: ['admin', 'pharmacist'] },
    { key: '/billing', label: 'Thu phí & hóa đơn', icon: FileTextOutlined, roles: ['admin', 'manager', 'receptionist'] },
    { key: '/reports', label: 'Báo cáo vận hành', icon: FileTextOutlined, roles: ['admin', 'manager'] },
    { key: '/system-management', label: 'Quản trị hệ thống', icon: SettingOutlined, roles: ['admin'] },
    { key: '/public-lookup', label: 'Cổng tra cứu công khai', icon: SearchOutlined, roles: ['admin', 'manager', 'doctor', 'nurse', 'receptionist', 'pharmacist'] },
  ]

  return items.filter((item) => item.roles.some((role) => normalizedRoles.includes(role)))
}

function MainLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false)
  const [searchValue, setSearchValue] = useState('')
  const [remotePatients, setRemotePatients] = useState([])
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuthContext()

  const syncPatients = React.useCallback(async () => {
    try {
      const res = await patientApi.getAll({ page: 0, size: 200 })
      const list = res.data?.content || res.data || []
      setRemotePatients(Array.isArray(list) ? list : [])
    } catch {
      setRemotePatients([])
    }
  }, [])

  React.useEffect(() => {
    syncPatients()
  }, [syncPatients, location.pathname])

  const searchOptions = useMemo(() => {
    const keyword = searchValue.trim().toLowerCase()
    if (!keyword) return []

    const matchedPatients = remotePatients.filter((p) =>
      [p.fullName, p.patientCode, p.phone, p.phoneNumber, p.identityNumber]
        .some((val) => String(val || '').toLowerCase().includes(keyword)),
    ).slice(0, 5)

    const options = []

    if (matchedPatients.length > 0) {
      options.push({
        label: <span style={{ fontWeight: 600, color: '#2563eb', fontSize: 12 }}>👤 BỆNH NHÂN ({matchedPatients.length})</span>,
        options: matchedPatients.map((p) => ({
          value: `patient:${p.id}`,
          label: (
            <div
              className="search-patient-item"
              onMouseDown={(e) => {
                e.preventDefault()
                e.stopPropagation()
                setSearchValue('')
                navigate(`/patients/${p.id}`, { state: { patient: p } })
              }}
              style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', cursor: 'pointer' }}
            >
              <span><strong>{p.fullName}</strong> <small style={{ color: '#64748b' }}>({p.patientCode})</small></span>
              <small style={{ color: '#2563eb' }}>{p.phone || p.phoneNumber || ''}</small>
            </div>
          ),
          type: 'patient',
          id: p.id,
          patient: p,
        })),
      })
    }

    return options
  }, [searchValue, remotePatients, navigate])

  const handleSelectSearch = (value, option) => {
    setSearchValue('')
    const valStr = String(value || '')

    if (valStr.startsWith('patient:')) {
      const patientId = valStr.replace('patient:', '')
      const foundPatient = option?.patient || remotePatients.find((p) => String(p.id) === String(patientId))
      navigate(`/patients/${patientId}`, { state: { patient: foundPatient } })
    }
  }

  const handleSearchSubmit = () => {
    if (!searchValue.trim()) return
    const keyword = searchValue.trim()
    setSearchValue('')

    const matched = remotePatients.find((p) =>
      [p.fullName, p.patientCode, p.phone, p.phoneNumber, p.identityNumber]
        .some((val) => String(val || '').toLowerCase() === keyword.toLowerCase())
    ) || remotePatients.find((p) =>
      [p.fullName, p.patientCode, p.phone, p.phoneNumber, p.identityNumber]
        .some((val) => String(val || '').toLowerCase().includes(keyword.toLowerCase()))
    )

    if (matched) {
      navigate(`/patients/${matched.id}`, { state: { patient: matched } })
    } else {
      navigate('/patients', { state: { keyword } })
    }
  }

  const navigationItems = useMemo(
    () => getNavigationItems(user?.roles || []).map((item) => ({
      key: item.key,
      icon: item.icon ? React.createElement(item.icon) : null,
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
    if (collapsed || !section.label) return items

    return [{
      type: 'group',
      key: `group-${section.key}`,
      label: section.label,
      children: items,
    }]
  }), [navigationItems, collapsed])

  const drawerItems = useMemo(() => navigationSections.flatMap((section) => {
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
  const displayName = user?.fullName || user?.username || 'Người dùng'

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleMenuClick = ({ key }) => {
    if (key && key.startsWith('group-')) {
      const sectionKey = key.replace('group-', '')
      const section = navigationSections.find((s) => s.key === sectionKey)
      const firstPath = section?.paths[0]
      if (firstPath) {
        navigate(firstPath)
        setMobileDrawerOpen(false)
      }
      return
    }
    if (key && key.startsWith('/')) {
      navigate(key)
      setMobileDrawerOpen(false)
    }
  }

  const userMenuItems = [
    { key: 'profile', icon: <UserOutlined />, label: 'Thông tin cá nhân' },
    { key: 'settings', icon: <SettingOutlined />, label: 'Cài đặt tài khoản' },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true, onClick: handleLogout },
  ]

  const handleToggleSidebar = () => {
    if (window.innerWidth <= 768 || document.documentElement.clientWidth <= 768) {
      setMobileDrawerOpen((prev) => !prev)
    } else {
      setCollapsed((prev) => !prev)
    }
  }

  return (
    <Layout className="clinic-shell">
      <Sider
        className="clinic-sider"
        trigger={null}
        collapsible
        collapsed={collapsed}
        collapsedWidth={0}
        breakpoint="md"
        onBreakpoint={(broken) => {
          if (broken) setCollapsed(true)
        }}
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
          onClick={handleMenuClick}
        />

        <Tooltip title={collapsed ? 'Mở rộng menu' : 'Thu gọn menu'} placement="right">
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
          <button
            type="button"
            className="sidebar-toggle-btn"
            onClick={handleToggleSidebar}
            aria-label="Thu gọn / Mở rộng menu"
            title="Thu gọn / Mở rộng menu"
          >
            {collapsed || mobileDrawerOpen ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </button>

          <AutoComplete
            className="clinic-search-autocomplete"
            style={{ width: 'min(460px, 46vw)' }}
            options={searchOptions}
            value={searchValue}
            onChange={setSearchValue}
            onSelect={handleSelectSearch}
            popupMatchSelectWidth={340}
          >
            <Input
              className="clinic-search"
              prefix={<SearchOutlined />}
              placeholder="Tìm kiếm bệnh nhân, lịch hẹn..."
              allowClear
              onFocus={syncPatients}
              onPressEnter={handleSearchSubmit}
            />
          </AutoComplete>

          <div className="clinic-header-actions">
            <Badge count={0} size="small" offset={[-2, 3]}>
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


      <Drawer
        title={(
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="clinic-brand-icon" style={{ background: '#123B6D', color: '#fff' }}><MedicineBoxOutlined /></span>
            <div>
              <strong style={{ display: 'block', fontSize: 15, color: '#123B6D' }}>BỆNH ÁN SỐ</strong>
              <small style={{ color: '#64748b' }}>Hệ thống quản lý phòng khám</small>
            </div>
          </div>
        )}
        placement="left"
        onClose={() => setMobileDrawerOpen(false)}
        open={mobileDrawerOpen}
        width={280}
        styles={{ body: { padding: '12px 0', background: '#123B6D' }, header: { borderBottom: '1px solid #e2e8f0', padding: '16px' } }}
      >
        <Menu
          className="clinic-menu"
          theme="dark"
          mode="inline"
          selectedKeys={[selectedPath]}
          items={drawerItems}

          inlineIndent={16}
          onClick={handleMenuClick}
        />
      </Drawer>
    </Layout>
  )
}

export default MainLayout
