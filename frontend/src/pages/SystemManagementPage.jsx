import React from 'react'
import { AppstoreOutlined, CloudServerOutlined, DatabaseOutlined, KeyOutlined, SafetyCertificateOutlined, SettingOutlined, TeamOutlined } from '@ant-design/icons'
import { Tabs, Typography } from 'antd'
import BackupRestorePage from './BackupRestorePage'
import ClinicConfigurationPage from './ClinicConfigurationPage'
import MedicalRecordAccessLogsPage from './MedicalRecordAccessLogsPage'
import PrescriptionInterconnectionPage from './PrescriptionInterconnectionPage'
import RolePermissionsPage from './RolePermissionsPage'
import ServicesPage from './ServicesPage'
import UsersPage from './UsersPage'
import { useAuthContext } from '../context/AuthContext'

const { Title, Text } = Typography

function SystemManagementPage() {
  const { user } = useAuthContext()
  const userPermissions = (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  const userRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const isAdmin = userRoles.includes('admin')

  const canViewConfig = userPermissions.includes('CLINIC_CONFIGURATION_READ') || userPermissions.includes('ROOM_READ') || isAdmin
  const canViewUsers = userPermissions.includes('USER_READ') || isAdmin
  const canViewRolePermissions = userPermissions.includes('ROLE_READ') || userPermissions.includes('PERMISSION_READ')
  const canViewServices = userPermissions.includes('SERVICE_CATALOG_READ') || isAdmin
  const canViewAccessLogs = userPermissions.includes('AUDIT_READ') || isAdmin
  const canViewBackup = userPermissions.includes('BACKUP_READ') || isAdmin
  const canViewInterconnections = userPermissions.includes('PRESCRIPTION_INTERCONNECTION_READ') || isAdmin || userRoles.includes('manager') || userRoles.includes('clinic_manager')

  const tabItems = [
    canViewInterconnections && {
      key: 'interconnections',
      label: <span><CloudServerOutlined /> Liên thông đơn thuốc</span>,
      children: <PrescriptionInterconnectionPage />,
    },
    canViewConfig && {
      key: 'clinic-config',
      label: <span><SettingOutlined /> Cấu hình phòng khám</span>,
      children: <ClinicConfigurationPage />,
    },
    canViewUsers && {
      key: 'users',
      label: <span><TeamOutlined /> Tài khoản người dùng</span>,
      children: <UsersPage />,
    },
    canViewRolePermissions && {
      key: 'role-permissions',
      label: <span><KeyOutlined /> Phân quyền vai trò</span>,
      children: <RolePermissionsPage />,
    },
    canViewServices && {
      key: 'services',
      label: <span><AppstoreOutlined /> Danh mục dịch vụ</span>,
      children: <ServicesPage />,
    },
    canViewAccessLogs && {
      key: 'access-logs',
      label: <span><SafetyCertificateOutlined /> Nhật ký truy cập bệnh án</span>,
      children: <MedicalRecordAccessLogsPage />,
    },
    canViewBackup && {
      key: 'backup-restore',
      label: <span><DatabaseOutlined /> Sao lưu & Phục hồi</span>,
      children: <BackupRestorePage />,
    },
  ].filter(Boolean)

  return (
    <div className="system-management-page">
      <div className="page-heading-block">
        <Title level={3}>Quản trị hệ thống & Giám sát an toàn dữ liệu</Title>
        <Text type="secondary">Quản lý tài khoản, phân quyền, cấu hình thông tin phòng khám, sao lưu dữ liệu và kiểm toán nhật ký truy cập bệnh án.</Text>
      </div>
      <Tabs
        className="system-tabs"
        defaultActiveKey="clinic-config"
        items={tabItems}
      />
    </div>
  )
}

export default SystemManagementPage
