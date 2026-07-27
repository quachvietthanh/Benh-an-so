import React from 'react'
import { AppstoreOutlined, TeamOutlined } from '@ant-design/icons'
import { Tabs, Typography } from 'antd'
import ServicesPage from './ServicesPage'
import UsersPage from './UsersPage'

const { Title, Text } = Typography

function SystemManagementPage() {
  return (
    <div className="system-management-page">
      <div className="page-heading-block">
        <Title level={3}>Quản trị hệ thống và danh mục dịch vụ</Title>
        <Text type="secondary">Quản lý tài khoản, phân quyền và cấu hình bảng giá dịch vụ.</Text>
      </div>
      <Tabs
        className="system-tabs"
        defaultActiveKey="users"
        items={[
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
        ]}
      />
    </div>
  )
}

export default SystemManagementPage
