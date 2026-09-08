import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Table, Tag, Button, Popconfirm, Space, Typography, message } from 'antd'
import { LockOutlined, UnlockOutlined, ReloadOutlined, KeyOutlined } from '@ant-design/icons'
import userApi from '../api/userApi'
import { formatDateTime } from '../utils/helpers'
import { useAuthContext } from '../context/AuthContext'
import ResetPasswordModal from '../components/users/ResetPasswordModal'

const { Title } = Typography

function UserList() {
  const { user: currentUser } = useAuthContext()
  const [loading, setLoading] = useState(false)
  const [users, setUsers] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [resetModalOpen, setResetModalOpen] = useState(false)
  const [resetTargetUser, setResetTargetUser] = useState(null)

  const canResetPassword = useMemo(() => {
    if (!currentUser) return false
    const roles = (currentUser.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    const perms = (currentUser.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
    return roles.includes('admin') || perms.includes('USER_RESET_PASSWORD')
  }, [currentUser])

  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try {
      const response = await userApi.getAll({
        page,
        size: pageSize,
      })
      setUsers(response.data.content)
      setTotal(response.data.totalElements)
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

  const handleToggleLock = async (user, locked) => {
    try {
      const action = locked ? 'khóa' : 'mở khóa'
      await userApi.updateStatus(user.id, locked)
      message.success(`Đã ${action} tài khoản "${user.username}" thành công`)
      fetchUsers()
    } catch (error) {
      console.error('Failed to update user status:', error)
      const errMsg =
        error.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại'
      message.error(errMsg)
    }
  }

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
        const isSelf = String(record.id) === String(currentUser?.id) || record.username === currentUser?.username

        return (
          <Space size={6}>
            {canResetPassword && !isSelf && (
              <Button
                icon={<KeyOutlined />}
                size="small"
                onClick={() => {
                  setResetTargetUser(record)
                  setResetModalOpen(true)
                }}
              >
                Đặt lại MK
              </Button>
            )}
            {record.locked ? (
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
            ) : (
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
            )}
          </Space>
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

      <ResetPasswordModal
        open={resetModalOpen}
        targetUser={resetTargetUser}
        onClose={() => {
          setResetModalOpen(false)
          setResetTargetUser(null)
        }}
        onSuccess={fetchUsers}
      />
    </div>
  )
}

export default UserList
