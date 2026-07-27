import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  AppstoreOutlined,
  ClockCircleOutlined,
  EditOutlined,
  EnvironmentOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import {
  Button,
  Card,
  DatePicker,
  Empty,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Table,
  Tabs,
  Tag,
  TimePicker,
  Typography,
} from 'antd'
import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import systemApi from '../api/systemApi'

dayjs.extend(customParseFormat)

const { Paragraph, Text, Title } = Typography

const formatMoney = (value) => `${Number(value || 0).toLocaleString('vi-VN')} ₫`

const roleBasedStatus = {
  true: { color: 'success', label: 'Hiệu lực' },
  false: { color: 'default', label: 'Ngừng dùng' },
}

function ServicesPage() {
  const [services, setServices] = useState([])
  const [editing, setEditing] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [savingService, setSavingService] = useState(false)
  const [savingClinic, setSavingClinic] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [serviceForm] = Form.useForm()
  const [clinicForm] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [serviceResponse, clinicResponse] = await Promise.all([
        systemApi.services(),
        systemApi.clinic(),
      ])
      const clinic = clinicResponse.data || {}

      setServices(Array.isArray(serviceResponse.data) ? serviceResponse.data : [])
      clinicForm.setFieldsValue({
        ...clinic,
        openingTime: clinic.openingTime ? dayjs(clinic.openingTime, 'HH:mm:ss') : null,
        closingTime: clinic.closingTime ? dayjs(clinic.closingTime, 'HH:mm:ss') : null,
        examinationRooms: (clinic.examinationRooms || []).join('\n'),
      })
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể tải cấu hình')
    } finally {
      setLoading(false)
    }
  }, [clinicForm])

  useEffect(() => {
    load()
  }, [load])

  const filteredServices = useMemo(() => {
    const keyword = searchTerm.trim().toLocaleLowerCase('vi')
    if (!keyword) return services

    return services.filter((service) =>
      [service.serviceCode, service.name]
        .filter(Boolean)
        .some((value) => String(value).toLocaleLowerCase('vi').includes(keyword)),
    )
  }, [searchTerm, services])

  const closeServiceModal = () => {
    setModalOpen(false)
    setEditing(null)
    serviceForm.resetFields()
  }

  const openCreateModal = () => {
    setEditing(null)
    serviceForm.resetFields()
    serviceForm.setFieldsValue({ effectiveFrom: dayjs() })
    setModalOpen(true)
  }

  const openEditModal = (service) => {
    setEditing(service)
    serviceForm.setFieldsValue({
      ...service,
      effectiveFrom: service.effectiveFrom ? dayjs(service.effectiveFrom) : null,
    })
    setModalOpen(true)
  }

  const saveService = async (values) => {
    setSavingService(true)
    try {
      const payload = {
        ...values,
        serviceCode: values.serviceCode.trim(),
        name: values.name.trim(),
        effectiveFrom: values.effectiveFrom.format('YYYY-MM-DD'),
      }

      if (editing) {
        await systemApi.updateService(editing.id, payload)
      } else {
        await systemApi.createService({ ...payload, active: true })
      }

      message.success(editing ? 'Đã cập nhật dịch vụ' : 'Đã thêm dịch vụ mới')
      closeServiceModal()
      await load()
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể lưu dịch vụ')
    } finally {
      setSavingService(false)
    }
  }

  const saveClinic = async (values) => {
    setSavingClinic(true)
    try {
      await systemApi.updateClinic({
        ...values,
        clinicName: values.clinicName.trim(),
        address: values.address?.trim() || '',
        phone: values.phone?.trim() || '',
        openingTime: values.openingTime.format('HH:mm:ss'),
        closingTime: values.closingTime.format('HH:mm:ss'),
        examinationRooms: values.examinationRooms
          .split('\n')
          .map((room) => room.trim())
          .filter(Boolean),
      })
      message.success('Đã lưu cấu hình phòng khám')
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể lưu cấu hình')
    } finally {
      setSavingClinic(false)
    }
  }

  const columns = [
    {
      title: 'Mã dịch vụ',
      dataIndex: 'serviceCode',
      key: 'serviceCode',
      width: 150,
      render: (value) => <span className="service-code-badge">{value}</span>,
    },
    {
      title: 'Tên dịch vụ',
      dataIndex: 'name',
      key: 'name',
      render: (value) => (
        <div className="service-name-cell">
          <span className="service-name-icon">
            <AppstoreOutlined />
          </span>
          <Text strong>{value}</Text>
        </div>
      ),
    },
    {
      title: 'Đơn giá',
      dataIndex: 'price',
      key: 'price',
      width: 170,
      align: 'right',
      render: (value) => <span className="service-price">{formatMoney(value)}</span>,
    },
    {
      title: 'Hiệu lực từ',
      dataIndex: 'effectiveFrom',
      key: 'effectiveFrom',
      width: 160,
      render: (value) => (
        <span className="service-date-cell">
          {value && dayjs(value).isValid() ? dayjs(value).format('DD/MM/YYYY') : '—'}
        </span>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 140,
      align: 'center',
      render: (active) => {
        const status = roleBasedStatus[String(Boolean(active))]
        return (
          <Tag className={`service-status${active ? '' : ' is-inactive'}`} color={status.color}>
            {status.label}
          </Tag>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 120,
      align: 'center',
      render: (_, service) => (
        <Button
          className="service-edit-button"
          icon={<EditOutlined />}
          onClick={() => openEditModal(service)}
        >
          Sửa
        </Button>
      ),
    },
  ]

  const serviceCatalog = (
    <section className="service-catalog-panel">
      <div className="service-section-header">
        <div className="service-section-copy">
          <span className="service-section-eyebrow">Danh mục và bảng giá</span>
          <Title level={4}>Dịch vụ phòng khám</Title>
          <Paragraph type="secondary">
            Quản lý tên dịch vụ, đơn giá và thời điểm áp dụng trong toàn hệ thống.
          </Paragraph>
        </div>
        <div className="service-header-actions">
          <Button type="primary" size="large" icon={<PlusOutlined />} onClick={openCreateModal}>
            Thêm dịch vụ
          </Button>
        </div>
      </div>

      <Card className="services-data-card" bordered={false}>
        <div className="service-toolbar">
          <Input
            className="service-search"
            allowClear
            prefix={<SearchOutlined />}
            placeholder="Tìm theo mã hoặc tên dịch vụ..."
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
          />
          <Button icon={<ReloadOutlined />} loading={loading} onClick={load}>
            Làm mới
          </Button>
        </div>
        <Table
          className="services-table"
          rowKey="id"
          loading={loading}
          dataSource={filteredServices}
          columns={columns}
          scroll={{ x: 900 }}
          pagination={{
            pageSize: 8,
            showSizeChanger: false,
            showTotal: (total) => `Tổng ${total} dịch vụ`,
          }}
          locale={{
            emptyText: (
              <Empty
                className="service-empty-copy"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={searchTerm ? 'Không tìm thấy dịch vụ phù hợp' : 'Chưa có dịch vụ'}
              />
            ),
          }}
        />
      </Card>
    </section>
  )

  const clinicConfiguration = (
    <section className="clinic-config-shell">
      <Card className="clinic-config-card" bordered={false}>
        <div className="clinic-config-header">
          <span className="clinic-config-icon">
            <EnvironmentOutlined />
          </span>
          <div>
            <Title level={4}>Thông tin vận hành phòng khám</Title>
            <Paragraph type="secondary">
              Cập nhật thông tin liên hệ, khung giờ làm việc và danh sách phòng khám.
            </Paragraph>
          </div>
        </div>

        <Form className="clinic-form" form={clinicForm} layout="vertical" onFinish={saveClinic}>
          <div className="clinic-form-grid">
            <Form.Item
              name="clinicName"
              label="Tên phòng khám"
              rules={[{ required: true, message: 'Vui lòng nhập tên phòng khám' }]}
            >
              <Input size="large" placeholder="Nhập tên phòng khám" />
            </Form.Item>

            <Form.Item name="phone" label="Số điện thoại">
              <Input size="large" placeholder="Ví dụ: 1900 1234" />
            </Form.Item>

            <div className="clinic-hours-row">
              <Form.Item
                className="clinic-hours-field"
                name="openingTime"
                label={<span className="clinic-time-label"><ClockCircleOutlined /> Giờ mở cửa</span>}
                rules={[{ required: true, message: 'Chọn giờ mở cửa' }]}
              >
                <TimePicker size="large" format="HH:mm" minuteStep={5} />
              </Form.Item>
              <Form.Item
                className="clinic-hours-field"
                name="closingTime"
                label={<span className="clinic-time-label"><ClockCircleOutlined /> Giờ đóng cửa</span>}
                rules={[{ required: true, message: 'Chọn giờ đóng cửa' }]}
              >
                <TimePicker size="large" format="HH:mm" minuteStep={5} />
              </Form.Item>
            </div>

            <Form.Item className="clinic-form-full" name="address" label="Địa chỉ">
              <Input size="large" placeholder="Nhập địa chỉ phòng khám" />
            </Form.Item>

            <Form.Item
              className="clinic-form-full"
              name="examinationRooms"
              label="Danh sách phòng khám"
              extra="Mỗi dòng tương ứng với một phòng khám."
              rules={[{ required: true, message: 'Vui lòng nhập ít nhất một phòng khám' }]}
            >
              <Input.TextArea
                rows={5}
                placeholder={'Phòng khám 1\nPhòng khám 2\nPhòng khám 3'}
              />
            </Form.Item>
          </div>

          <div className="clinic-form-actions">
            <Button icon={<ReloadOutlined />} onClick={load} disabled={savingClinic}>
              Khôi phục dữ liệu
            </Button>
            <Button type="primary" htmlType="submit" loading={savingClinic}>
              Lưu cấu hình
            </Button>
          </div>
        </Form>
      </Card>
    </section>
  )

  return (
    <div className="services-admin-page">
      <Tabs
        className="service-subtabs"
        defaultActiveKey="catalog"
        items={[
          { key: 'catalog', label: 'Dịch vụ và bảng giá', children: serviceCatalog },
          {
            key: 'clinic',
            label: 'Cấu hình phòng khám',
            forceRender: true,
            children: clinicConfiguration,
          },
        ]}
      />

      <Modal
        className="service-form-modal"
        width={680}
        title={
          <div className="service-modal-title">
            <span className="service-name-icon">
              <AppstoreOutlined />
            </span>
            <div>
              <Title level={4}>{editing ? 'Cập nhật dịch vụ' : 'Thêm dịch vụ mới'}</Title>
              <Text type="secondary">
                {editing
                  ? 'Điều chỉnh thông tin và trạng thái áp dụng.'
                  : 'Nhập đầy đủ thông tin để thêm dịch vụ vào bảng giá.'}
              </Text>
            </div>
          </div>
        }
        open={modalOpen}
        footer={null}
        centered
        forceRender
        maskClosable={!savingService}
        closable={!savingService}
        onCancel={closeServiceModal}
      >
        <Form className="service-form" form={serviceForm} layout="vertical" onFinish={saveService}>
          <div className="service-form-grid">
            <Form.Item
              name="serviceCode"
              label="Mã dịch vụ"
              rules={[{ required: true, message: 'Vui lòng nhập mã dịch vụ' }]}
            >
              <Input size="large" placeholder="Ví dụ: DV001" />
            </Form.Item>

            <Form.Item
              name="name"
              label="Tên dịch vụ"
              rules={[{ required: true, message: 'Vui lòng nhập tên dịch vụ' }]}
            >
              <Input size="large" placeholder="Nhập tên dịch vụ" />
            </Form.Item>

            <Form.Item
              name="price"
              label="Đơn giá"
              rules={[{ required: true, message: 'Vui lòng nhập đơn giá' }]}
            >
              <InputNumber
                size="large"
                min={0}
                step={1000}
                controls={false}
                formatter={(value) => `${value || ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                parser={(value) => value?.replace(/\./g, '') || ''}
                addonAfter="₫"
                placeholder="0"
                style={{ width: '100%' }}
              />
            </Form.Item>

            <Form.Item
              name="effectiveFrom"
              label="Hiệu lực từ"
              rules={[{ required: true, message: 'Vui lòng chọn ngày áp dụng' }]}
            >
              <DatePicker size="large" format="DD/MM/YYYY" style={{ width: '100%' }} />
            </Form.Item>

            {editing && (
              <Form.Item className="service-form-full" name="active" label="Trạng thái">
                <Select
                  size="large"
                  options={[
                    { value: true, label: 'Đang hiệu lực' },
                    { value: false, label: 'Ngừng sử dụng' },
                  ]}
                />
              </Form.Item>
            )}
          </div>

          <div className="service-modal-hint">
            Bảng giá mới sẽ được áp dụng từ ngày hiệu lực đã chọn.
          </div>
          <div className="service-modal-actions">
            <Button size="large" onClick={closeServiceModal} disabled={savingService}>
              Hủy
            </Button>
            <Button size="large" type="primary" htmlType="submit" loading={savingService}>
              {editing ? 'Lưu thay đổi' : 'Thêm dịch vụ'}
            </Button>
          </div>
        </Form>
      </Modal>
    </div>
  )
}

export default ServicesPage
