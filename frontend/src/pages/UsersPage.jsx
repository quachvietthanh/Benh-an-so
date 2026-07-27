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
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  MailOutlined,
  MoreOutlined,
  PhoneOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from '@ant-design/icons'
import userApi from '../api/userApi'

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
      setUsers(Array.isArray(response.data) ? response.data : response.data?.content || [])
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể tải danh sách người dùng')
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
    try {
      const normalizedValues = {
        ...values,
        username: values.username?.trim(),
        fullName: values.fullName?.trim(),
        email: values.email?.trim(),
        phone: values.phone?.trim() || null,
      }
      if (editingUser) {
        await userApi.update(editingUser.id, {
          fullName: normalizedValues.fullName,
          email: normalizedValues.email,
          phone: normalizedValues.phone,
          roleName: normalizedValues.roleName,
        })
        message.success('Cập nhật tài khoản thành công')
      } else {
        await userApi.create(normalizedValues)
        message.success('Tạo tài khoản thành công')
      }
      closeForm()
      await loadUsers()
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể lưu tài khoản')
    } finally {
      setSaving(false)
    }
  }

  const deleteUser = async (account) => {
    try {
      await userApi.remove(account.id)
      message.success(`Đã xóa tài khoản ${account.username}`)
      await loadUsers()
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể xóa tài khoản')
    }
  }

  const confirmDelete = (account) => {
    Modal.confirm({
      title: 'Xóa tài khoản?',
      content: `Tài khoản ${account.username} sẽ bị xóa khỏi hệ thống.`,
      okText: 'Xóa tài khoản',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      centered: true,
      onOk: () => deleteUser(account),
    })
  }

  const columns = [
    {
      title: 'Người dùng',
      key: 'user',
      width: 280,
      render: (_, account, index) => (
        <div className="admin-user-cell">
          <Avatar className={'admin-user-avatar avatar-tone-' + (index % 4)}>{getInitials(account.fullName || account.username)}</Avatar>
          <div><strong>{account.fullName || account.username}</strong><small>@{account.username}</small></div>
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
      width: 180,
      render: (_, account) => {
        const role = getUserRole(account)
        return <span className={'admin-role-tag role-' + (roleStyles[role] || 'gray')}><SafetyCertificateOutlined /> {getRoleLabel(role)}</span>
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 135,
      align: 'right',
      render: (_, account) => (
        <Space size={6}>
          <Button className="admin-table-action" icon={<EditOutlined />} onClick={() => openEditForm(account)}>Sửa</Button>
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                { key: 'edit', icon: <EditOutlined />, label: 'Chỉnh sửa', onClick: () => openEditForm(account) },
                {
                  key: 'delete',
                  danger: true,
                  icon: <DeleteOutlined />,
                  label: 'Xóa tài khoản',
                  onClick: () => confirmDelete(account),
                },
              ],
            }}
          >
            <Button className="admin-more-button" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
      ),
    },
  ]

  return (
    <div className="admin-users-panel">
      <div className="admin-section-heading">
        <div><h2>Quản trị tài khoản</h2><p>Quản lý thông tin và phân quyền người dùng trong hệ thống.</p></div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateForm}>Tạo tài khoản</Button>
      </div>

      <Alert className="admin-info-alert" type="warning" showIcon message="Quản trị viên có thể tạo, chỉnh sửa, phân vai trò và xóa tài khoản người dùng." />

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
