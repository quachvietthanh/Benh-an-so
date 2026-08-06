import React, { useState, useEffect, useCallback } from 'react'
import { Table, Tag, Button, Popconfirm, Space, Typography, message } from 'antd'
import { LockOutlined, UnlockOutlined, ReloadOutlined } from '@ant-design/icons'
import userApi from '../api/userApi'
import { formatDateTime } from '../utils/helpers'

const { Title } = Typography

function UserList() {
  const [loading, setLoading] = useState(false)
  const [users, setUsers] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  // ---- Fetch danh sách người dùng ----
  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try {
      const response = await userApi.getAll({
        page,
        size: pageSize,
      })
      // Backend /users returns a plain array (not a Spring Page).
      const allUsers = Array.isArray(response.data)
        ? response.data
        : (response.data?.content || [])
      const start = page * pageSize
      setUsers(allUsers.slice(start, start + pageSize))
      setTotal(allUsers.length)
    } catch (error) {
      console.error('Failed to fetch users:', error)
      message.error('Không thể tải danh sách người dùng')
    } finally {
      setLoading(false)
    }
  }, [page, pageSize])

  useEffect(() => {
    fetchUsers()
  }, [fetchUsers])

  // ---- Khóa / Mở khóa tài khoản ----
  const handleToggleLock = async (user, locked) => {
    try {
      const action = locked ? 'khóa' : 'mở khóa'
      await userApi.updateStatus(user.id, locked)
      message.success(`Đã ${action} tài khoản "${user.username}" thành công`)
      fetchUsers() // Tải lại danh sách
    } catch (error) {
      console.error('Failed to update user status:', error)
      const errMsg =
        error.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại'
      message.error(errMsg)
    }
  }

  // ---- Cấu hình cột ----
  const columns = [
    {
      title: 'Tên đăng nhập',
      dataIndex: 'username',
      key: 'username',
      width: 160,
    },
    {
      title: 'Họ tên',
      dataIndex: 'fullName',
      key: 'fullName',
      ellipsis: true,
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      ellipsis: true,
    },
    {
      title: 'Số điện thoại',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 130,
      render: (_, record) => {
        if (record.locked) {
          return <Tag color="red">Bị khóa</Tag>
        }
        if (record.active) {
          return <Tag color="green">Hoạt động</Tag>
        }
        return <Tag color="orange">Vô hiệu hóa</Tag>
      },
    },
    {
      title: 'Lần đăng nhập cuối',
      dataIndex: 'lastLoginAt',
      key: 'lastLoginAt',
      width: 180,
      render: (date) => formatDateTime(date) || 'Chưa đăng nhập',
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 130,
      render: (_, record) => {
        if (record.locked) {
          return (
            <Popconfirm
              title="Mở khóa tài khoản"
              description={`Bạn có chắc chắn muốn mở khóa tài khoản "${record.username}" không?`}
              onConfirm={() => handleToggleLock(record, false)}
              okText="Mở khóa"
              cancelText="Hủy"
              okButtonProps={{ type: 'primary' }}
            >
              <Button type="primary" icon={<UnlockOutlined />} size="small">
                Mở khóa
              </Button>
            </Popconfirm>
          )
        }
        return (
          <Popconfirm
            title="Khóa tài khoản"
            description={`Bạn có chắc chắn muốn khóa tài khoản "${record.username}" không?`}
            onConfirm={() => handleToggleLock(record, true)}
            okText="Khóa"
            cancelText="Hủy"
            okButtonProps={{ danger: true }}
          >
            <Button danger icon={<LockOutlined />} size="small">
              Khóa
            </Button>
          </Popconfirm>
        )
      },
    },
  ]

  return (
    <div>
      <div className="page-header">
        <Title level={4} style={{ margin: 0 }}>
          Quản lý tài khoản người dùng
        </Title>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={fetchUsers}
            loading={loading}
          >
            Tải lại
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (total) => `Tổng số: ${total} người dùng`,
          onChange: (newPage, newSize) => {
            setPage(newPage - 1)
            setPageSize(newSize)
          },
        }}
      />
    </div>
  )
}

export default UserList
