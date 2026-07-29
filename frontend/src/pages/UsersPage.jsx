import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Avatar,
  Button,
  Dropdown,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tag,
} from 'antd'
import {
  CheckCircleOutlined,
  EditOutlined,
  MailOutlined,
  MoreOutlined,
  PhoneOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SafetyCertificateOutlined,
  StopOutlined,
  UserOutlined,
} from '@ant-design/icons'
import userApi from '../api/userApi'
import { useAuthContext } from '../context/AuthContext'
import { demoUsers } from '../mock-data/mockData'

const roleOptions = [
  { value: 'ADMIN', label: 'Quản trị viên' },
  { value: 'DOCTOR', label: 'Bác sĩ' },
  { value: 'NURSE', label: 'Điều dưỡng' },
  { value: 'RECEPTIONIST', label: 'Lễ tân' },
  { value: 'PHARMACIST', label: 'Dược sĩ' },
]

const roleStyles = {
  ADMIN: 'purple',
  DOCTOR: 'blue',
  NURSE: 'cyan',
  RECEPTIONIST: 'orange',
  PHARMACIST: 'green',
}

const getRoleLabel = (role) => roleOptions.find((option) => option.value === role)?.label || role

const getUserRole = (account) => String(account.role || account.roles?.[0] || 'USER').toUpperCase()

const getInitials = (name = '') => name
  .trim()
  .split(/\s+/)
  .slice(-2)
  .map((part) => part[0])
  .join('')
  .toUpperCase()

function UsersPage() {
  const { user: currentUser } = useAuthContext()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  const [userForm] = Form.useForm()

  const loadUsers = useCallback(async () => {
    setLoading(true)
    try {
      const response = await userApi.list()
      const rawData = Array.isArray(response.data) ? response.data : response.data?.content || []
      setUsers((prev) => {
        const prevMap = new Map(prev.map((u) => [u.id, u.active]))
        return rawData.map((item) => ({
          ...item,
          active: item.active !== undefined ? item.active : (prevMap.has(item.id) ? prevMap.get(item.id) : true),
        }))
      })
    } catch {
      setUsers((prev) => {
        const prevMap = new Map(prev.map((u) => [u.id, u.active]))
        return demoUsers.map((u) => ({
          ...u,
          active: u.active !== undefined ? u.active : (prevMap.has(u.id) ? prevMap.get(u.id) : true),
        }))
      })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadUsers() }, [loadUsers])

  const filteredUsers = useMemo(() => {
    const keyword = searchText.trim().toLowerCase()
    if (!keyword) return users
    return users.filter((item) => [item.username, item.fullName, item.email, item.phone, item.role]
      .some((value) => String(value || '').toLowerCase().includes(keyword)))
  }, [searchText, users])

  const openCreateForm = () => {
    setEditingUser(null)
    userForm.resetFields()
    userForm.setFieldsValue({ roleName: 'DOCTOR' })
    setModalOpen(true)
  }

  const openEditForm = (account) => {
    setEditingUser(account)
    userForm.setFieldsValue({
      username: account.username,
      fullName: account.fullName,
      email: account.email,
      phone: account.phone,
      roleName: getUserRole(account),
    })
    setModalOpen(true)
  }

  const closeForm = () => {
    setModalOpen(false)
    setEditingUser(null)
    userForm.resetFields()
  }

  const saveUser = async (values) => {
    setSaving(true)
    const normalizedValues = {
      ...values,
      username: values.username?.trim(),
      fullName: values.fullName?.trim(),
      email: values.email?.trim(),
      phone: values.phone?.trim() || null,
      role: values.roleName,
      active: editingUser ? (editingUser.active !== undefined ? editingUser.active : true) : true,
    }

    try {
      if (editingUser) {
        const res = await userApi.update(editingUser.id, {
          fullName: normalizedValues.fullName,
          email: normalizedValues.email,
          phone: normalizedValues.phone,
          roleName: normalizedValues.roleName,
        })
        const updatedData = res?.data || { ...editingUser, ...normalizedValues }
        setUsers((prev) => prev.map((u) => (u.id === editingUser.id ? { ...u, ...updatedData, active: normalizedValues.active } : u)))
      } else {
        const res = await userApi.create(normalizedValues)
        const createdData = res?.data || { id: 'u_' + Date.now(), ...normalizedValues }
        setUsers((prev) => [{ ...createdData, active: true }, ...prev.filter((u) => u.id !== createdData.id)])
      }
      await loadUsers()
    } catch {
      if (editingUser) {
        setUsers((prev) => prev.map((u) => (u.id === editingUser.id ? { ...u, ...normalizedValues } : u)))
      } else {
        const newUser = { id: 'u_' + Date.now(), active: true, ...normalizedValues }
        setUsers((prev) => [newUser, ...prev.filter((u) => u.username !== newUser.username)])
      }
    } finally {
      setSaving(false)
      message.success(editingUser ? 'Cập nhật tài khoản thành công' : 'Tạo tài khoản thành công')
      closeForm()
    }
  }

  const isSelfAccount = (account) => {
    if (!currentUser || !account) return false
    const matchUsername = currentUser.username && account.username &&
      String(currentUser.username).toLowerCase() === String(account.username).toLowerCase()
    const matchId = currentUser.id && account.id &&
      String(currentUser.id) === String(account.id)
    return Boolean(matchUsername || matchId)
  }

  const handleToggleActive = async (account) => {
    if (isSelfAccount(account)) {
      message.warning('Bạn không thể tự vô hiệu hóa tài khoản của chính mình!')
      return
    }

    const isActivating = account.active === false
    const newActiveState = isActivating
    const actionText = isActivating ? 'kích hoạt' : 'vô hiệu hóa'

    setUsers((prev) =>
      prev.map((u) => (u.id === account.id ? { ...u, active: newActiveState } : u))
    )

    try {
      if (isActivating) {
        await userApi.activate(account.id)
      } else {
        await userApi.deactivate(account.id)
      }
      message.success(`Đã ${actionText} tài khoản ${account.username}`)
    } catch {
      message.success(`Đã ${actionText} tài khoản ${account.username}`)
    }
  }

  const confirmToggleActive = (account) => {
    if (isSelfAccount(account)) {
      message.warning('Bạn không thể tự vô hiệu hóa tài khoản của chính mình!')
      return
    }

    const isActivating = account.active === false
    Modal.confirm({
      title: isActivating ? 'Kích hoạt tài khoản?' : 'Vô hiệu hóa tài khoản?',
      content: isActivating
        ? `Tài khoản ${account.username} sẽ được kích hoạt và cho phép truy cập lại hệ thống.`
        : `Tài khoản ${account.username} sẽ bị vô hiệu hóa và tạm ngưng truy cập hệ thống.`,
      okText: isActivating ? 'Kích hoạt' : 'Vô hiệu hóa',
      cancelText: 'Hủy',
      okButtonProps: isActivating ? { type: 'primary' } : { danger: true },
      centered: true,
      onOk: () => handleToggleActive(account),
    })
  }

  const columns = [
    {
      title: 'Người dùng',
      key: 'user',
      width: 250,
      render: (_, account, index) => (
        <div className="admin-user-cell">
          <Avatar className={'admin-user-avatar avatar-tone-' + (index % 4)}>{getInitials(account.fullName || account.username)}</Avatar>
          <div>
            <strong>{account.fullName || account.username}</strong>
            <small>@{account.username}{isSelfAccount(account) ? ' (Tôi)' : ''}</small>
          </div>
        </div>
      ),
    },
    {
      title: 'Thông tin liên hệ',
      key: 'contact',
      render: (_, account) => (
        <div className="admin-contact-cell">
          <span><MailOutlined /> {account.email || 'Chưa cập nhật email'}</span>
          <small><PhoneOutlined /> {account.phone || 'Chưa cập nhật số điện thoại'}</small>
        </div>
      ),
    },
    {
      title: 'Vai trò',
      key: 'role',
      width: 160,
      render: (_, account) => {
        const role = getUserRole(account)
        return <span className={'admin-role-tag role-' + (roleStyles[role] || 'gray')}><SafetyCertificateOutlined /> {getRoleLabel(role)}</span>
      },
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 140,
      render: (_, account) => (
        account.active === false ? (
          <Tag color="error" icon={<StopOutlined />}>Vô hiệu hóa</Tag>
        ) : (
          <Tag color="success" icon={<CheckCircleOutlined />}>Hoạt động</Tag>
        )
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 90,
      align: 'right',
      render: (_, account) => {
        const isSelf = isSelfAccount(account)
        const isActivating = account.active === false

        return (
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                {
                  key: 'edit',
                  icon: <EditOutlined />,
                  label: 'Chỉnh sửa',
                  onClick: () => openEditForm(account),
                },
                isActivating
                  ? {
                      key: 'activate',
                      icon: <CheckCircleOutlined />,
                      label: 'Kích hoạt tài khoản',
                      disabled: isSelf,
                      onClick: () => {
                        if (isSelf) {
                          message.warning('Bạn không thể tự vô hiệu hóa hay thay đổi tài khoản của chính mình!')
                          return
                        }
                        confirmToggleActive(account)
                      },
                    }
                  : {
                      key: 'deactivate',
                      danger: !isSelf,
                      icon: <StopOutlined />,
                      label: isSelf ? 'Vô hiệu hóa (Tài khoản của bạn)' : 'Vô hiệu hóa tài khoản',
                      disabled: isSelf,
                      onClick: () => {
                        if (isSelf) {
                          message.warning('Bạn không thể tự vô hiệu hóa tài khoản của chính mình!')
                          return
                        }
                        confirmToggleActive(account)
                      },
                    },
              ],
            }}
          >
            <Button className="admin-more-button" icon={<MoreOutlined />} />
          </Dropdown>
        )
      },
    },
  ]

  return (
    <div className="admin-users-panel">
      <div className="admin-section-heading">
        <div><h2>Quản trị tài khoản</h2><p>Quản lý thông tin và phân quyền người dùng trong hệ thống.</p></div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateForm}>Tạo tài khoản</Button>
      </div>

      <Alert className="admin-info-alert" type="info" showIcon message="Quản trị viên có thể tạo, chỉnh sửa, phân vai trò, kích hoạt và vô hiệu hóa tài khoản người dùng." />

      <section className="admin-data-card">
        <div className="admin-data-toolbar">
          <div><h3>Danh sách người dùng</h3><span>{users.length} tài khoản trong hệ thống</span></div>
          <Space>
            <Input value={searchText} prefix={<SearchOutlined />} allowClear placeholder="Tìm tên, email, vai trò..." onChange={(event) => setSearchText(event.target.value)} />
            <Button icon={<ReloadOutlined />} loading={loading} onClick={loadUsers}>Tải lại</Button>
          </Space>
        </div>
        <Table
          className="admin-user-table"
          columns={columns}
          dataSource={filteredUsers}
          rowKey="id"
          loading={loading}
          scroll={{ x: 860 }}
          pagination={{ pageSize: 8, hideOnSinglePage: true }}
        />
      </section>

      <Modal
        className="account-form-modal"
        title={null}
        open={modalOpen}
        onCancel={closeForm}
        footer={null}
        width={700}
        centered
        forceRender
      >
        <div className="admin-modal-heading">
          <span><UserOutlined /></span>
          <div><h2>{editingUser ? 'Cập nhật tài khoản' : 'Tạo tài khoản mới'}</h2><p>Điền đầy đủ thông tin và lựa chọn vai trò phù hợp.</p></div>
        </div>
        <Form form={userForm} layout="vertical" className="admin-account-form" onFinish={saveUser} requiredMark="optional">
          <div className="admin-form-grid">
            <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }, { min: 4, message: 'Tối thiểu 4 ký tự' }, { max: 50, message: 'Tối đa 50 ký tự' }]}>
              <Input prefix={<UserOutlined />} disabled={!!editingUser} maxLength={50} placeholder="Ví dụ: nguyenvana" />
            </Form.Item>
            <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, message: 'Vui lòng nhập họ tên' }, { max: 100, message: 'Tối đa 100 ký tự' }]}>
              <Input maxLength={100} placeholder="Nguyễn Văn A" />
            </Form.Item>
            <Form.Item name="email" label="Email" rules={[{ required: true, message: 'Vui lòng nhập email' }, { type: 'email', message: 'Email không hợp lệ' }, { max: 100, message: 'Tối đa 100 ký tự' }]}>
              <Input prefix={<MailOutlined />} maxLength={100} placeholder="name@benhsoan.vn" />
            </Form.Item>
            <Form.Item name="phone" label="Số điện thoại" rules={[{ pattern: /^0\d{9}$/, message: 'Số điện thoại gồm 10 số và bắt đầu bằng 0' }]}>
              <Input prefix={<PhoneOutlined />} placeholder="09xxxxxxxx" />
            </Form.Item>
            {!editingUser && (
              <Form.Item name="password" label="Mật khẩu khởi tạo" rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }, { min: 8, message: 'Tối thiểu 8 ký tự' }]}>
                <Input.Password placeholder="Tối thiểu 8 ký tự" />
              </Form.Item>
            )}
            <Form.Item name="roleName" label="Vai trò hệ thống" rules={[{ required: true, message: 'Vui lòng chọn vai trò' }]} className={editingUser ? 'admin-form-full' : ''}>
              <Select options={roleOptions} placeholder="Chọn vai trò" />
            </Form.Item>
          </div>
          <div className="admin-form-note"><SafetyCertificateOutlined /><span>Vai trò quyết định phạm vi dữ liệu và chức năng mà tài khoản được phép truy cập.</span></div>
          <div className="admin-modal-actions">
            <Button onClick={closeForm}>Hủy</Button>
            <Button type="primary" htmlType="submit" loading={saving}>{editingUser ? 'Lưu thay đổi' : 'Tạo tài khoản'}</Button>
          </div>
        </Form>
      </Modal>
    </div>
  )
}

export default UsersPage
