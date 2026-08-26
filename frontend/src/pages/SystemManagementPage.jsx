import React from 'react'
import { DatabaseOutlined, KeyOutlined, SafetyCertificateOutlined, SettingOutlined, TeamOutlined } from '@ant-design/icons'
import { Card, Empty, Tabs, Typography } from 'antd'
import BackupRestorePage from './BackupRestorePage'
import ClinicConfigurationPage from './ClinicConfigurationPage'
import DiagnosisCatalogPage from './DiagnosisCatalogPage'
import MedicalRecordAccessLogsPage from './MedicalRecordAccessLogsPage'
import RolePermissionsPage from './RolePermissionsPage'
import UsersPage from './UsersPage'
import { useAuthContext } from '../context/AuthContext'
import { ExperimentOutlined } from '@ant-design/icons'

const { Title, Text } = Typography

function SystemManagementPage() {
  const { user } = useAuthContext()
  const userPermissions = (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  const userRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const isAdmin = userRoles.includes('admin')
  const isManager = userRoles.includes('manager') || userRoles.includes('clinic_manager')

  const canViewConfig = userPermissions.includes('CLINIC_CONFIGURATION_READ') || userPermissions.includes('ROOM_READ') || isAdmin || isManager
  const canViewDiagnosisCatalog = userPermissions.includes('DIAGNOSIS_CATALOG_MANAGE') || userPermissions.includes('SERVICE_CATALOG_READ') || isAdmin || isManager
  const canViewUsers = userPermissions.includes('USER_READ') || isAdmin
  const canViewRolePermissions = userPermissions.includes('ROLE_READ') || userPermissions.includes('PERMISSION_READ') || isAdmin
  const canViewAccessLogs = userPermissions.includes('AUDIT_READ') || isAdmin
  const canViewBackup = userPermissions.includes('BACKUP_READ') || isAdmin

  const tabItems = [
    canViewConfig && {
      key: 'clinic-config',
      label: <span><SettingOutlined /> Cấu hình phòng khám</span>,
      children: <ClinicConfigurationPage />,
    },
    canViewDiagnosisCatalog && {
      key: 'diagnosis-catalog',
      label: <span><ExperimentOutlined /> Danh mục mã bệnh (ICD-10)</span>,
      children: <DiagnosisCatalogPage />,
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


  if (tabItems.length === 0) {
    return (
      <div className="system-management-page">
        <div className="page-heading-block">
          <Title level={3}>Quản trị hệ thống & Giám sát an toàn dữ liệu</Title>
          <Text type="secondary">Quản lý tài khoản, phân quyền, cấu hình thông tin phòng khám, sao lưu dữ liệu và kiểm toán nhật ký truy cập bệnh án.</Text>
        </div>
        <Card style={{ borderRadius: 12, textAlign: 'center', padding: '40px 20px', marginTop: 16 }}>
          <Empty description="Tài khoản của bạn hiện chưa có quyền truy cập vào các module quản trị hệ thống." />
        </Card>
      </div>
    )
  }

  return (
    <div className="system-management-page">
      <div className="page-heading-block">
        <Title level={3}>Quản trị hệ thống & Giám sát an toàn dữ liệu</Title>
        <Text type="secondary">Quản lý tài khoản, phân quyền, cấu hình thông tin phòng khám, sao lưu dữ liệu và kiểm toán nhật ký truy cập bệnh án.</Text>
      </div>
      <Tabs
        className="system-tabs"
        defaultActiveKey={tabItems[0]?.key || 'clinic-config'}
        items={tabItems}
      />
    </div>
  )
}

export default SystemManagementPage
