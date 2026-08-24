import React, { useEffect, useState, useCallback, useMemo } from 'react'
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Spin,
  Table,
  Tag,
  TimePicker,
  Typography,
  message,
} from 'antd'
import {
  SettingOutlined,
  SaveOutlined,
  UndoOutlined,
  LockOutlined,
  PlusOutlined,
  EditOutlined,
  CheckCircleOutlined,
  StopOutlined,
  AppstoreOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import systemApi from '../api/systemApi'
import { useAuthContext } from '../context/AuthContext'
import { useClinicConfig } from '../context/ClinicConfigurationContext'

dayjs.extend(customParseFormat)

const { Text } = Typography

function ClinicConfigurationPage() {
  const [form] = Form.useForm()
  const [roomForm] = Form.useForm()
  const { user } = useAuthContext()
  const { refreshClinicConfig } = useClinicConfig()

  const [loadingConfig, setLoadingConfig] = useState(false)
  const [savingConfig, setSavingConfig] = useState(false)
  const [lastFetchedConfig, setLastFetchedConfig] = useState(null)
  const [configErrorMsg, setConfigErrorMsg] = useState('')

  const [rooms, setRooms] = useState([])
  const [loadingRooms, setLoadingRooms] = useState(false)
  const [roomModalOpen, setRoomModalOpen] = useState(false)
  const [editingRoom, setEditingRoom] = useState(null)
  const [savingRoom, setSavingRoom] = useState(false)

  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [user])

  const canReadConfig = userPermissions.includes('CLINIC_CONFIGURATION_READ') || userPermissions.includes('ROOM_READ')
  const canUpdateConfig = userPermissions.includes('CLINIC_CONFIGURATION_UPDATE')
  const canCreateRoom = userPermissions.includes('ROOM_CREATE')
  const canUpdateRoom = userPermissions.includes('ROOM_UPDATE')

  const fetchConfig = useCallback(async () => {
    setLoadingConfig(true)
    setConfigErrorMsg('')
    try {
      const res = await systemApi.clinic()
      const data = res?.data || {}
      setLastFetchedConfig(data)
      form.setFieldsValue({
        clinicName: data.clinicName || '',
        address: data.address || '',
        phone: data.phone || '',
        openingTime: data.openingTime ? dayjs(data.openingTime, ['HH:mm:ss', 'HH:mm']) : null,
        closingTime: data.closingTime ? dayjs(data.closingTime, ['HH:mm:ss', 'HH:mm']) : null,
        retentionYears: data.retentionYears !== undefined && data.retentionYears !== null ? data.retentionYears : 10,
      })
    } catch (err) {
      console.error('[ClinicConfigurationPage] Lỗi tải cấu hình phòng khám:', err)
      const status = err?.response?.status
      if (status === 403) {
        // Silently ignore or don't set harsh red error alert for unprivileged users
        setConfigErrorMsg('')
      } else if (status === 401) {
        setConfigErrorMsg('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
      } else {
        setConfigErrorMsg(err?.response?.data?.message || 'Không thể tải thông tin cấu hình phòng khám từ hệ thống.')
      }
    } finally {
      setLoadingConfig(false)
    }
  }, [form])

  const fetchRooms = useCallback(async () => {
    setLoadingRooms(true)
    try {
      const res = await systemApi.getRooms({ page: 0, size: 100 })
      const list = res?.data?.content || res?.data || []
      setRooms(Array.isArray(list) ? list : [])
    } catch (err) {
      console.error('[ClinicConfigurationPage] Lỗi tải danh sách phòng khám:', err)
    } finally {
      setLoadingRooms(false)
    }
  }, [])

  useEffect(() => {
    if (canReadConfig) {
      fetchConfig()
      fetchRooms()
    }
  }, [canReadConfig, fetchConfig, fetchRooms])

  const handleResetConfig = () => {
    if (lastFetchedConfig) {
      form.setFieldsValue({
        clinicName: lastFetchedConfig.clinicName || '',
        address: lastFetchedConfig.address || '',
        phone: lastFetchedConfig.phone || '',
        openingTime: lastFetchedConfig.openingTime ? dayjs(lastFetchedConfig.openingTime, ['HH:mm:ss', 'HH:mm']) : null,
        closingTime: lastFetchedConfig.closingTime ? dayjs(lastFetchedConfig.closingTime, ['HH:mm:ss', 'HH:mm']) : null,
        retentionYears: lastFetchedConfig.retentionYears !== undefined && lastFetchedConfig.retentionYears !== null ? lastFetchedConfig.retentionYears : 10,
      })
      message.info('Đã hủy các thay đổi và khôi phục cấu hình gần nhất từ Backend.')
    } else {
      form.resetFields()
    }
  }

  const handleSubmitConfig = async (values) => {
    if (!canUpdateConfig) {
      message.error('Bạn không có quyền cập nhật cấu hình phòng khám (Yêu cầu quyền CLINIC_CONFIGURATION_UPDATE).')
      return
    }

    const nameTrimmed = (values.clinicName || '').trim()
    if (!nameTrimmed) {
      message.error('Tên phòng khám là bắt buộc.')
      return
    }
    if (nameTrimmed.length > 150) {
      message.error('Tên phòng khám không được vượt quá 150 ký tự.')
      return
    }

    if (values.address && values.address.trim().length > 500) {
      message.error('Địa chỉ không được vượt quá 500 ký tự.')
      return
    }

    if (values.phone && values.phone.trim().length > 30) {
      message.error('Số điện thoại không được vượt quá 30 ký tự.')
      return
    }

    if (!values.openingTime) {
      message.error('Giờ mở cửa là bắt buộc.')
      return
    }

    if (!values.closingTime) {
      message.error('Giờ đóng cửa là bắt buộc.')
      return
    }

    const openTimeDayjs = dayjs.isDayjs(values.openingTime) ? values.openingTime : dayjs(values.openingTime, ['HH:mm:ss', 'HH:mm'])
    const closeTimeDayjs = dayjs.isDayjs(values.closingTime) ? values.closingTime : dayjs(values.closingTime, ['HH:mm:ss', 'HH:mm'])

    const openSeconds = openTimeDayjs.hour() * 3600 + openTimeDayjs.minute() * 60 + openTimeDayjs.second()
    const closeSeconds = closeTimeDayjs.hour() * 3600 + closeTimeDayjs.minute() * 60 + closeTimeDayjs.second()

    if (closeSeconds <= openSeconds) {
      message.error('Giờ đóng cửa phải sau giờ mở cửa.')
      return
    }

    const retentionNum = Number(values.retentionYears)
    if (values.retentionYears === undefined || values.retentionYears === null || values.retentionYears === '' || isNaN(retentionNum)) {
      message.error('Số năm lưu trữ hồ sơ là bắt buộc.')
      return
    }
    if (!Number.isInteger(retentionNum)) {
      message.error('Số năm lưu trữ hồ sơ phải là số nguyên.')
      return
    }
    if (retentionNum < 10) {
      message.error('Số năm lưu trữ hồ sơ phải từ 10 năm trở lên.')
      return
    }

    const payload = {
      clinicName: nameTrimmed,
      address: values.address ? values.address.trim() : null,
      phone: values.phone ? values.phone.trim() : null,
      openingTime: openTimeDayjs.format('HH:mm:ss'),
      closingTime: closeTimeDayjs.format('HH:mm:ss'),
      retentionYears: retentionNum,
    }

    setSavingConfig(true)
    try {
      await systemApi.updateClinic(payload)
      message.success('Cập nhật cấu hình phòng khám thành công.')
      await refreshClinicConfig()
      await fetchConfig()
    } catch (err) {
      console.error('[ClinicConfigurationPage] Lỗi lưu cấu hình phòng khám:', err)
      const status = err?.response?.status
      const msg = err?.response?.data?.message
      if (status === 403) {
        message.error('Bạn không có quyền cập nhật cấu hình phòng khám.')
      } else if (status === 400) {
        message.error(msg || 'Dữ liệu cấu hình không hợp lệ.')
      } else {
        message.error(msg || 'Không thể lưu cấu hình phòng khám. Vui lòng thử lại.')
      }
    } finally {
      setSavingConfig(false)
    }
  }

  const openCreateRoomModal = () => {
    setEditingRoom(null)
    roomForm.resetFields()
    setRoomModalOpen(true)
  }

  const openEditRoomModal = (room) => {
    setEditingRoom(room)
    roomForm.setFieldsValue({
      code: room.code,
      name: room.name,
    })
    setRoomModalOpen(true)
  }

  const handleSaveRoom = async (values) => {
    setSavingRoom(true)
    try {
      if (editingRoom) {
        await systemApi.updateRoom(editingRoom.id, { name: values.name.trim() })
        message.success('Đã cập nhật tên phòng khám thành công.')
      } else {
        await systemApi.createRoom({
          code: values.code.trim(),
          name: values.name.trim(),
        })
        message.success('Đã thêm phòng khám mới thành công.')
      }
      setRoomModalOpen(false)
      fetchRooms()
    } catch (err) {
      console.error('[ClinicConfigurationPage] Lỗi lưu phòng khám:', err)
      message.error(err?.response?.data?.message || 'Không thể lưu phòng khám. Vui lòng thử lại.')
    } finally {
      setSavingRoom(false)
    }
  }

  const handleToggleRoomStatus = async (room) => {
    try {
      if (room.active) {
        await systemApi.deactivateRoom(room.id)
        message.success(`Đã ngưng hoạt động ${room.name}.`)
      } else {
        await systemApi.activateRoom(room.id)
        message.success(`Đã kích hoạt ${room.name}.`)
      }
      fetchRooms()
    } catch (err) {
      console.error('[ClinicConfigurationPage] Lỗi cập nhật trạng thái phòng:', err)
      message.error(err?.response?.data?.message || 'Không thể đổi trạng thái phòng khám.')
    }
  }

  if (!canReadConfig) {
    return (
      <Card style={{ marginTop: 16 }}>
        <Alert
          type="error"
          showIcon
          icon={<LockOutlined />}
          message="Bạn không có quyền truy cập chức năng này."
          description="Chức năng Cấu hình thông tin phòng khám yêu cầu quyền CLINIC_CONFIGURATION_READ hoặc ROOM_READ."
        />
      </Card>
    )
  }

  const roomColumns = [
    {
      title: 'Mã phòng',
      dataIndex: 'code',
      key: 'code',
      width: 140,
      render: (text) => <Text code style={{ color: '#0284c7', fontWeight: 600 }}>{text}</Text>,
    },
    {
      title: 'Tên phòng khám',
      dataIndex: 'name',
      key: 'name',
      render: (text) => <Text strong style={{ color: '#0f172a' }}>{text}</Text>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 160,
      align: 'center',
      render: (active) => (
        active ? (
          <Tag color="green" icon={<CheckCircleOutlined />}>Đang hoạt động</Tag>
        ) : (
          <Tag color="default" icon={<StopOutlined />}>Ngừng hoạt động</Tag>
        )
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 180,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          <Button
            size="small"
            type="text"
            icon={<EditOutlined style={{ color: '#0284c7' }} />}
            onClick={() => openEditRoomModal(record)}
          >
            Sửa
          </Button>
          <Popconfirm
            title={record.active ? 'Ngừng hoạt động phòng này?' : 'Kích hoạt lại phòng này?'}
            onConfirm={() => handleToggleRoomStatus(record)}
            okText="Đồng ý"
            cancelText="Hủy"
          >
            <Button
              size="small"
              type="text"
              danger={record.active}
              style={{ color: record.active ? '#dc2626' : '#16a34a' }}
            >
              {record.active ? 'Tắt' : 'Bật'}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', paddingTop: 8 }} className="clinic-config-container">
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Card
          title={
            <Space>
              <SettingOutlined style={{ color: '#0284c7' }} />
              <span style={{ fontWeight: 700, fontSize: 18, color: '#0f172a' }}>CẤU HÌNH THÔNG TIN PHÒNG KHÁM</span>
            </Space>
          }
          bordered={false}
          style={{ boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)', borderRadius: 8 }}
        >
          <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
            Thiết lập thông tin chung và giờ hoạt động của phòng khám.
          </Text>

          {configErrorMsg && (
            <Alert
              type="error"
              showIcon
              message={configErrorMsg}
              style={{ marginBottom: 20 }}
              action={
                <Button size="small" type="primary" danger onClick={fetchConfig}>
                  Tải lại
                </Button>
              }
            />
          )}

          <Spin spinning={loadingConfig}>
            <Form
              form={form}
              layout="vertical"
              onFinish={handleSubmitConfig}
              autoComplete="off"
            >
              <Form.Item
                label={<span>Tên phòng khám <span style={{ color: '#dc2626' }}>*</span></span>}
                name="clinicName"
                rules={[
                  {
                    validator: (_, value) => {
                      const val = (value || '').trim()
                      if (!val) {
                        return Promise.reject(new Error('Tên phòng khám là bắt buộc.'))
                      }
                      if (val.length > 150) {
                        return Promise.reject(new Error('Tên phòng khám không được vượt quá 150 ký tự.'))
                      }
                      return Promise.resolve()
                    },
                  },
                ]}
              >
                <Input placeholder="Nhập tên phòng khám (vd: Phòng khám Bệnh Án Số)" maxLength={150} showCount />
              </Form.Item>

              <Form.Item
                label="Địa chỉ"
                name="address"
                rules={[
                  {
                    validator: (_, value) => {
                      if (value && value.trim().length > 500) {
                        return Promise.reject(new Error('Địa chỉ không được vượt quá 500 ký tự.'))
                      }
                      return Promise.resolve()
                    },
                  },
                ]}
              >
                <Input.TextArea rows={3} placeholder="Nhập địa chỉ phòng khám" maxLength={500} showCount />
              </Form.Item>

              <Form.Item
                label="Số điện thoại"
                name="phone"
                rules={[
                  {
                    validator: (_, value) => {
                      if (value && value.trim().length > 30) {
                        return Promise.reject(new Error('Số điện thoại không được vượt quá 30 ký tự.'))
                      }
                      return Promise.resolve()
                    },
                  },
                ]}
              >
                <Input placeholder="Nhập số điện thoại liên hệ" maxLength={30} showCount />
              </Form.Item>

              <Space size="large" style={{ width: '100%', display: 'flex' }}>
                <Form.Item
                  label={<span>Giờ mở cửa <span style={{ color: '#dc2626' }}>*</span></span>}
                  name="openingTime"
                  style={{ flex: 1 }}
                  rules={[{ required: true, message: 'Giờ mở cửa là bắt buộc.' }]}
                >
                  <TimePicker format="HH:mm" style={{ width: '100%' }} placeholder="Chọn giờ mở cửa" />
                </Form.Item>

                <Form.Item
                  label={<span>Giờ đóng cửa <span style={{ color: '#dc2626' }}>*</span></span>}
                  name="closingTime"
                  style={{ flex: 1 }}
                  dependencies={['openingTime']}
                  rules={[
                    { required: true, message: 'Giờ đóng cửa là bắt buộc.' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        const openTime = getFieldValue('openingTime')
                        if (!value || !openTime) {
                          return Promise.resolve()
                        }
                        const openSec = openTime.hour() * 3600 + openTime.minute() * 60 + openTime.second()
                        const closeSec = value.hour() * 3600 + value.minute() * 60 + value.second()
                        if (closeSec <= openSec) {
                          return Promise.reject(new Error('Giờ đóng cửa phải sau giờ mở cửa.'))
                        }
                        return Promise.resolve()
                      },
                    }),
                  ]}
                >
                  <TimePicker format="HH:mm" style={{ width: '100%' }} placeholder="Chọn giờ đóng cửa" />
                </Form.Item>
              </Space>

              <Form.Item
                label={<span>Thời hạn lưu trữ hồ sơ bệnh án <span style={{ color: '#dc2626' }}>*</span></span>}
                name="retentionYears"
                extra="Quy định số năm lưu trữ hồ sơ bệnh án tối thiểu (tối thiểu 10 năm theo quy chuẩn y tế). Hệ thống sẽ ngăn chặn xóa các hồ sơ chưa hết hạn lưu trữ."
                rules={[
                  { required: true, message: 'Số năm lưu trữ hồ sơ là bắt buộc.' },
                  {
                    validator: (_, value) => {
                      if (value === undefined || value === null || value === '') {
                        return Promise.reject(new Error('Số năm lưu trữ hồ sơ là bắt buộc.'))
                      }
                      const num = Number(value)
                      if (!Number.isInteger(num)) {
                        return Promise.reject(new Error('Số năm lưu trữ phải là số nguyên.'))
                      }
                      if (num < 10) {
                        return Promise.reject(new Error('Số năm lưu trữ hồ sơ phải từ 10 năm trở lên.'))
                      }
                      return Promise.resolve()
                    },
                  },
                ]}
              >
                <InputNumber
                  min={10}
                  max={100}
                  step={1}
                  disabled={!canUpdateConfig}
                  placeholder="Nhập số năm (tối thiểu 10)"
                  style={{ width: '100%' }}
                  addonAfter="năm"
                />
              </Form.Item>

              <div style={{ marginTop: 24, textAlign: 'right', borderTop: '1px solid #f1f5f9', paddingTop: 16 }}>
                <Space>
                  <Button icon={<UndoOutlined />} onClick={handleResetConfig} disabled={loadingConfig || savingConfig}>
                    Hủy thay đổi
                  </Button>
                  <Button
                    type="primary"
                    htmlType="submit"
                    icon={<SaveOutlined />}
                    loading={savingConfig}
                    disabled={loadingConfig}
                    style={{ background: '#0284c7', borderColor: '#0284c7' }}
                  >
                    Lưu cấu hình
                  </Button>
                </Space>
              </div>
            </Form>
          </Spin>
        </Card>

        <Card
          title={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
              <Space>
                <AppstoreOutlined style={{ color: '#0284c7' }} />
                <span style={{ fontWeight: 700, fontSize: 18, color: '#0f172a' }}>CÁC PHÒNG KHÁM BỆNH</span>
              </Space>
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreateRoomModal} style={{ background: '#0284c7', borderColor: '#0284c7' }}>
                Thêm phòng khám
              </Button>
            </div>
          }
          bordered={false}
          style={{ boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)', borderRadius: 8 }}
        >
          <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            Khai báo và quản lý danh sách các phòng khám bệnh hiện có trong hệ thống.
          </Text>

          <Table
            dataSource={rooms}
            columns={roomColumns}
            rowKey="id"
            loading={loadingRooms}
            pagination={false}
            size="small"
            bordered
          />
        </Card>
      </Space>

      <Modal
        title={editingRoom ? 'CHỈNH SỬA PHÒNG KHÁM' : 'THÊM PHÒNG KHÁM MỚI'}
        open={roomModalOpen}
        onCancel={() => setRoomModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={roomForm} layout="vertical" onFinish={handleSaveRoom} style={{ marginTop: 16 }}>
          <Form.Item
            label={<span>Mã phòng <span style={{ color: '#dc2626' }}>*</span></span>}
            name="code"
            rules={[
              { required: true, message: 'Mã phòng là bắt buộc.' },
              { max: 30, message: 'Mã phòng tối đa 30 ký tự.' },
            ]}
          >
            <Input placeholder="Nhập mã phòng (vd: P101, PK-NOI...)" disabled={!!editingRoom} maxLength={30} />
          </Form.Item>

          <Form.Item
            label={<span>Tên phòng <span style={{ color: '#dc2626' }}>*</span></span>}
            name="name"
            rules={[
              { required: true, message: 'Tên phòng là bắt buộc.' },
              { max: 100, message: 'Tên phòng tối đa 100 ký tự.' },
            ]}
          >
            <Input placeholder="Nhập tên phòng khám (vd: Phòng khám Nội tổng quát)" maxLength={100} />
          </Form.Item>

          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setRoomModalOpen(false)}>Hủy</Button>
              <Button type="primary" htmlType="submit" loading={savingRoom} style={{ background: '#0284c7', borderColor: '#0284c7' }}>
                {editingRoom ? 'Cập nhật' : 'Tạo phòng mới'}
              </Button>
            </Space>
          </div>
        </Form>
      </Modal>
    </div>
  )
}

export default ClinicConfigurationPage
