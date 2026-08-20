import React from 'react'
import { AppstoreOutlined, DatabaseOutlined, SafetyCertificateOutlined, SettingOutlined, TeamOutlined } from '@ant-design/icons'
import { Tabs, Typography } from 'antd'
import BackupRestorePage from './BackupRestorePage'
import ClinicConfigurationPage from './ClinicConfigurationPage'
import MedicalRecordAccessLogsPage from './MedicalRecordAccessLogsPage'
import ServicesPage from './ServicesPage'
import UsersPage from './UsersPage'

const { Title, Text } = Typography

function SystemManagementPage() {
  return (
    <div className="system-management-page">
      <div className="page-heading-block">
        <Title level={3}>Quản trị hệ thống & Giám sát an toàn dữ liệu</Title>
        <Text type="secondary">Quản lý tài khoản, phân quyền, cấu hình thông tin phòng khám, sao lưu dữ liệu và kiểm toán nhật ký truy cập bệnh án.</Text>
      </div>
      <Tabs
        className="system-tabs"
        defaultActiveKey="clinic-config"
        items={[
          {
            key: 'clinic-config',
            label: <span><SettingOutlined /> Cấu hình phòng khám</span>,
            children: <ClinicConfigurationPage />,
          },
          {
            key: 'users',
            label: <span><TeamOutlined /> Tài khoản người dùng</span>,
            children: <UsersPage />,
          },
          {
            key: 'services',
            label: <span><AppstoreOutlined /> Danh mục dịch vụ</span>,
            children: <ServicesPage />,
          },
          {
            key: 'access-logs',
            label: <span><SafetyCertificateOutlined /> Nhật ký truy cập bệnh án</span>,
            children: <MedicalRecordAccessLogsPage />,
          },
          {
            key: 'backup-restore',
            label: <span><DatabaseOutlined /> Sao lưu & Phục hồi</span>,
            children: <BackupRestorePage />,
          },
        ]}
      />
    </div>
  )
}

export default SystemManagementPage
