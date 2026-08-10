import React, { useMemo, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { AutoComplete, Avatar, Badge, Drawer, Dropdown, Input, Layout, Menu, Tooltip } from 'antd'
import {
  BellOutlined,
  CalendarOutlined,
  CaretDownOutlined,
  DashboardOutlined,
  FileTextOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
  MenuFoldOutlined,
  MenuOutlined,
  MenuUnfoldOutlined,
  SearchOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import patientApi from '../../api/patientApi'
import { useAuthContext } from '../../context/AuthContext'
import { getNavigationItems } from '../../services/mockDataService'
import { mergeAppointments, mergePatients } from '../../utils/storageHelpers'
import { APPOINTMENT_STATUS_META } from '../../utils/queueHelpers'

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
  { key: 'examination', label: 'Khám bệnh', paths: ['/medical-records', '/clinical-orders', '/clinical-results', '/results', '/prescriptions'] },
  { key: 'pharmacy', label: 'Nhà thuốc', paths: ['/pharmacy'] },
  { key: 'finance', label: 'Tài chính', paths: ['/billing'] },
  { key: 'reports', label: 'Báo cáo', paths: ['/reports'] },
  { key: 'system', label: 'Hệ thống', paths: ['/system-management'] },
  { key: 'lookup', label: 'Tra cứu', paths: ['/public-lookup'] },
]

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
      if (Array.isArray(list) && list.length) {
        setRemotePatients(list)
      }
    } catch {
      // fallback
    }
  }, [])

  React.useEffect(() => {
    syncPatients()
  }, [syncPatients, location.pathname])

  const searchOptions = useMemo(() => {
    const keyword = searchValue.trim().toLowerCase()
    if (!keyword) return []

    const currentPatients = mergePatients(remotePatients)
    const currentAppointments = mergeAppointments([])

    // Combine patients from patient list and appointment records so every patient is searchable
    const appointmentPatientsMap = new Map()
    currentAppointments.forEach((a) => {
      if (a.patientName) {
        const pId = a.patientId || `p_${a.id}`
        if (!currentPatients.some((p) => String(p.id) === String(a.patientId) || p.fullName === a.patientName) && !appointmentPatientsMap.has(pId)) {
          appointmentPatientsMap.set(pId, {
            id: pId,
            fullName: a.patientName,
            patientCode: a.patientCode || `BN-${pId}`,
            phone: a.phone || a.phoneNumber || '',
            active: true,
          })
        }
      }
    })

    const allCombinedPatients = [
      ...currentPatients,
      ...Array.from(appointmentPatientsMap.values()),
    ]

    const matchedPatients = allCombinedPatients.filter((p) =>
      [p.fullName, p.patientCode, p.phone, p.phoneNumber, p.identityNumber]
        .some((val) => String(val || '').toLowerCase().includes(keyword)),
    ).slice(0, 5)

    const matchedAppointments = currentAppointments.filter((a) =>
      [a.patientName, a.patientCode, a.id, a.doctorName]
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

    if (matchedAppointments.length > 0) {
      options.push({
        label: <span style={{ fontWeight: 600, color: '#16a34a', fontSize: 12 }}>📅 LỊCH HẸN ({matchedAppointments.length})</span>,
        options: matchedAppointments.map((a) => {
          const targetPatientId = a.patientId || allCombinedPatients.find((p) => p.fullName === a.patientName)?.id || a.id
          const targetPatientObj = allCombinedPatients.find((p) => String(p.id) === String(targetPatientId) || p.fullName === a.patientName) || { id: targetPatientId, fullName: a.patientName || 'Bệnh nhân' }

          return {
            value: `appointment:${a.id}`,
            label: (
              <div
                className="search-appointment-item"
                onMouseDown={(e) => {
                  e.preventDefault()
                  e.stopPropagation()
                  setSearchValue('')
                  navigate(`/patients/${targetPatientId}`, { state: { patient: targetPatientObj } })
                }}
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', cursor: 'pointer' }}
              >
                <span><strong>{a.patientName || 'Lịch hẹn'}</strong> <small style={{ color: '#64748b' }}>({a.slot || a.time || 'Hôm nay'})</small></span>
                <small style={{ color: '#16a34a' }}>{APPOINTMENT_STATUS_META[a.status]?.label || 'Không xác định'}</small>
              </div>
            ),
            type: 'appointment',
            id: a.id,
            patientId: targetPatientId,
            patient: targetPatientObj,
          }
        }),
      })
    }

    return options
  }, [searchValue, remotePatients, navigate])

  const handleSelectSearch = (value, option) => {
    setSearchValue('')
    const valStr = String(value || '')

    if (valStr.startsWith('patient:')) {
      const patientId = valStr.replace('patient:', '')
      const currentPatients = mergePatients(remotePatients)
      const foundPatient = option?.patient || currentPatients.find((p) => String(p.id) === String(patientId))
      navigate(`/patients/${patientId}`, { state: { patient: foundPatient } })
    } else if (valStr.startsWith('appointment:')) {
      const targetPatientId = option?.patientId || option?.patient?.id
      if (targetPatientId) {
        navigate(`/patients/${targetPatientId}`, { state: { patient: option?.patient } })
      } else {
        const appointmentId = valStr.replace('appointment:', '')
        navigate('/appointments', { state: { appointmentId } })
      }
    }
  }

  const handleSearchSubmit = () => {
    if (!searchValue.trim()) return
    const keyword = searchValue.trim()
    setSearchValue('')

    const currentPatients = mergePatients(remotePatients)
    const matched = currentPatients.find((p) =>
      [p.fullName, p.patientCode, p.phone, p.phoneNumber, p.identityNumber]
        .some((val) => String(val || '').toLowerCase() === keyword.toLowerCase())
    ) || currentPatients.find((p) =>
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
  const displayName = user?.fullName || user?.username || 'Nguyễn Văn A'

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
