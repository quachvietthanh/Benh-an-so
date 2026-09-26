import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Table, Tag, Button, Popconfirm, Space, Typography, message, Dropdown, Modal } from 'antd'
import { LockOutlined, UnlockOutlined, ReloadOutlined, KeyOutlined, MoreOutlined } from '@ant-design/icons'
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
      const content = response.data.content || []
      const storedOverrides = (() => {
        try {
          return JSON.parse(localStorage.getItem('admin_user_status_overrides') || '{}')
        } catch {
          return {}
        }
      })()
      const mapped = content.map((u) => {
        const isActive = u.active !== undefined
          ? u.active
          : (storedOverrides[u.id] !== undefined ? storedOverrides[u.id] : true)
        return {
          ...u,
          active: isActive,
          locked: !isActive,
        }
      })
      setUsers(mapped)
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
      if (locked) {
        await userApi.deactivateUser(user.id)
      } else {
        await userApi.activateUser(user.id)
      }
      try {
        const stored = JSON.parse(localStorage.getItem('admin_user_status_overrides') || '{}')
        stored[user.id] = !locked
        localStorage.setItem('admin_user_status_overrides', JSON.stringify(stored))
      } catch {
        // ignore localStorage error
      }
      message.success(`Đã ${action} tài khoản "${user.username}" thành công`)
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, active: !locked, locked } : u))
      )
    } catch (error) {
      console.error('Failed to update user status:', error)
      const errMsg =
        error.response?.data?.message || error.apiError?.message || 'Có lỗi xảy ra, vui lòng thử lại'
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
        if (record.locked || record.active === false) {
          return <Tag color="red">Đã khóa</Tag>
        }
        return <Tag color="green">Đang hoạt động</Tag>
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
        const isSelf = String(record.id) === String(currentUser?.id) || record.username === currentUser?.username
        const isLocked = Boolean(record.locked || record.active === false)

        const actionMenuItems = []

        if (canResetPassword && !isSelf) {
          actionMenuItems.push({
            key: 'reset-password',
            icon: <KeyOutlined />,
            label: 'Đặt lại mật khẩu',
            onClick: () => {
              setResetTargetUser(record)
              setResetModalOpen(true)
            },
          })
          actionMenuItems.push({
            type: 'divider',
          })
        }

        actionMenuItems.push({
          key: 'toggle-lock',
          danger: !isLocked,
          icon: isLocked ? <UnlockOutlined /> : <LockOutlined />,
          disabled: isSelf,
          onClick: () => {
            if (isSelf) return
            Modal.confirm({
              title: isLocked ? 'Mở khóa tài khoản' : 'Khóa tài khoản',
              content: `Bạn có chắc chắn muốn ${isLocked ? 'mở khóa' : 'khóa'} tài khoản "${record.username}" không?`,
              okText: isLocked ? 'Mở khóa' : 'Khóa',
              okType: isLocked ? 'primary' : 'danger',
              cancelText: 'Hủy',
              onOk: () => handleToggleLock(record, !isLocked),
            })
          },
          label: (
            <Popconfirm
              title={isLocked ? 'Mở khóa tài khoản' : 'Khóa tài khoản'}
              description={`Bạn có chắc chắn muốn ${isLocked ? 'mở khóa' : 'khóa'} tài khoản "${record.username}" không?`}
              onConfirm={(e) => {
                e?.stopPropagation?.()
                handleToggleLock(record, !isLocked)
              }}
              okText={isLocked ? 'Mở khóa' : 'Khóa'}
              cancelText="Hủy"
              okButtonProps={{ type: isLocked ? 'primary' : undefined, danger: !isLocked }}
              disabled={isSelf}
            >
              <span
                onClick={(e) => e.stopPropagation()}
                style={{ display: 'inline-block', width: '100%' }}
                title={isSelf ? 'Không thể tự khóa tài khoản của chính mình' : undefined}
              >
                {isLocked ? 'Mở khóa tài khoản' : 'Khóa tài khoản'}
              </span>
            </Popconfirm>
          ),
        })

        return (
          <Dropdown
            trigger={['click']}
            menu={{ items: actionMenuItems }}
            placement="bottomRight"
          >
            <Button
              icon={<MoreOutlined />}
              size="small"
              title="Thao tác"
            />
          </Dropdown>
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
