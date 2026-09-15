import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Drawer,
  Empty,
  Form,
  InputNumber,
  Popconfirm,
  Radio,
  Row,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import clinicalServiceApi from '../../api/clinicalServiceApi'
import {
  GENDER_OPTIONS,
  checkOverlapClientSide,
  formatAgeRangeDisplay,
  formatGenderDisplay,
  formatReferenceBoundsDisplay,
  translateClinicalErrorMessage,
  validateReferenceRangePayload,
} from '../../utils/clinicalServiceValidation'

const { Text, Title } = Typography

export default function ClinicalReferenceRangeDrawer({ open, service, onClose, onUpdated, canManage = false }) {
  const [form] = Form.useForm()
  const [ranges, setRanges] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [editingRange, setEditingRange] = useState(null)
  const [errorMessage, setErrorMessage] = useState(null)
  const [togglingId, setTogglingId] = useState(null)

  const loadRanges = useCallback(async () => {
    if (!service?.id) return
    setLoading(true)
    setErrorMessage(null)
    try {
      const res = await clinicalServiceApi.getReferenceRanges(service.id)
      const list = Array.isArray(res.data) ? res.data : []
      setRanges(list)
    } catch (err) {
      console.error('Không thể nạp danh sách ngưỡng tham chiếu:', err)
      message.error(translateClinicalErrorMessage(err, 'Lỗi khi tải danh sách ngưỡng tham chiếu.'))
      setRanges([])
    } finally {
      setLoading(false)
    }
  }, [service?.id])

  useEffect(() => {
    if (open && service?.id) {
      setEditingRange(null)
      form.resetFields()
      setErrorMessage(null)
      loadRanges()
    }
  }, [open, service?.id, form, loadRanges])

  const handleStartEdit = (record) => {
    if (!canManage) return
    setEditingRange(record)
    setErrorMessage(null)
    form.setFieldsValue({
      gender: record.gender || 'ALL',
      minAge: record.minAge,
      maxAge: record.maxAge,
      lowerBound: record.lowerBound,
      upperBound: record.upperBound,
    })
  }

  const handleCancelEdit = () => {
    setEditingRange(null)
    form.resetFields()
    setErrorMessage(null)
  }

  const handleSubmitForm = async () => {
    if (!canManage || !service?.id) return

    try {
      setErrorMessage(null)
      const values = await form.validateFields()

      const validation = validateReferenceRangePayload({
        minAge: values.minAge,
        maxAge: values.maxAge,
        lowerBound: values.lowerBound,
        upperBound: values.upperBound,
      })

      if (!validation.isValid) {
        setErrorMessage(validation.errors.join(' '))
        return
      }

      const overlapResult = checkOverlapClientSide(
        {
          gender: values.gender,
          minAge: values.minAge,
          maxAge: values.maxAge,
        },
        ranges,
        editingRange?.id
      )

      if (overlapResult.isOverlap) {
        setErrorMessage(overlapResult.message)
        form.setFields([
          {
            name: 'minAge',
            errors: [overlapResult.message],
          },
        ])
        return
      }

      setSubmitting(true)

      const payload = {
        gender: values.gender === 'ALL' ? null : values.gender,
        minAge: values.minAge != null ? Number(values.minAge) : null,
        maxAge: values.maxAge != null ? Number(values.maxAge) : null,
        lowerBound: values.lowerBound != null ? Number(values.lowerBound) : null,
        upperBound: values.upperBound != null ? Number(values.upperBound) : null,
      }

      if (editingRange) {
        await clinicalServiceApi.updateReferenceRange(service.id, editingRange.id, payload)
        message.success('Cập nhật ngưỡng tham chiếu thành công!')
      } else {
        await clinicalServiceApi.createReferenceRange(service.id, payload)
        message.success('Thêm mới ngưỡng tham chiếu thành công!')
      }

      form.resetFields()
      setEditingRange(null)
      await loadRanges()
      if (onUpdated) onUpdated()
    } catch (err) {
      if (err?.errorFields) return
      console.error('Lỗi khi lưu ngưỡng tham chiếu:', err)
      const msg = translateClinicalErrorMessage(err, 'Không thể lưu ngưỡng tham chiếu. Vui lòng kiểm tra lại.')
      setErrorMessage(msg)
    } finally {
      setSubmitting(false)
    }
  }

  const handleToggleStatus = async (record, nextActive) => {
    if (!canManage || !service?.id || togglingId) return

    setTogglingId(record.id)
    setErrorMessage(null)
    try {
      await clinicalServiceApi.updateReferenceRangeStatus(service.id, record.id, nextActive)
      message.success(
        nextActive
          ? 'Kích hoạt ngưỡng tham chiếu thành công!'
          : 'Đã tạm ngưng áp dụng ngưỡng tham chiếu!'
      )
      await loadRanges()
      if (onUpdated) onUpdated()
    } catch (err) {
      console.error('Lỗi khi thay đổi trạng thái ngưỡng:', err)
      const msg = translateClinicalErrorMessage(err, 'Không thể đổi trạng thái ngưỡng tham chiếu.')
      message.error(msg)
      setErrorMessage(msg)
    } finally {
      setTogglingId(null)
    }
  }

  const columns = [
    {
      title: 'Giới tính',
      dataIndex: 'gender',
      key: 'gender',
      width: 110,
      render: (gender) => {
        const text = formatGenderDisplay(gender)
        const color = gender === 'MALE' ? 'blue' : gender === 'FEMALE' ? 'pink' : 'purple'
        return <Tag color={color}>{text}</Tag>
      },
    },
    {
      title: 'Nhóm tuổi áp dụng',
      key: 'ageGroup',
      width: 140,
      render: (_, r) => {
        return <Tag color="cyan">{formatAgeRangeDisplay(r.minAge, r.maxAge)}</Tag>
      },
    },
    {
      title: `Khoảng chuẩn (${service?.unit || 'Chỉ số'})`,
      key: 'bounds',
      render: (_, r) => {
        return (
          <span className="reference-range-bounds-text">
            {formatReferenceBoundsDisplay(r.lowerBound, r.upperBound, service?.unit)}
          </span>
        )
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 120,
      render: (active, record) => (
        <Switch
          checked={active}
          checkedChildren="Áp dụng"
          unCheckedChildren="Tạm ngưng"
          disabled={!canManage}
          loading={togglingId === record.id}
          onChange={(val) => handleToggleStatus(record, val)}
        />
      ),
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 90,
      render: (_, record) => (
        <Button
          type="text"
          size="small"
          icon={<EditOutlined />}
          disabled={!canManage}
          onClick={() => handleStartEdit(record)}
        >
          Sửa
        </Button>
      ),
    },
  ]

  return (
    <Drawer
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <SettingOutlined style={{ color: '#2563eb', fontSize: 18 }} />
          <span>Quản lý Ngưỡng Tham chiếu Chi tiết</span>
        </div>
      }
      placement="right"
      width={760}
      open={open}
      onClose={onClose}
      destroyOnClose
      extra={
        <Button icon={<ReloadOutlined />} onClick={loadRanges} loading={loading}>
          Làm mới
        </Button>
      }
    >
      <div className="reference-range-drawer-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={5} style={{ margin: 0, color: '#0f172a' }}>
              {service?.serviceName}
            </Title>
            <Space style={{ marginTop: 4 }}>
              <span className="clinical-service-code-badge">{service?.serviceCode}</span>
              {service?.unit && <Tag color="blue">Đơn vị: {service.unit}</Tag>}
              <Tag color="geekblue">{service?.serviceType}</Tag>
            </Space>
          </div>
          <Tag color={service?.active ? 'success' : 'default'}>
            {service?.active ? 'Đang hoạt động' : 'Tạm ngưng'}
          </Tag>
        </div>
        {service?.description && (
          <p style={{ marginTop: 8, marginBottom: 0, color: '#64748b', fontSize: 13 }}>
            {service.description}
          </p>
        )}
      </div>

      {errorMessage && (
        <Alert
          type="error"
          message={errorMessage}
          showIcon
          closable
          onClose={() => setErrorMessage(null)}
          style={{ marginBottom: 16 }}
        />
      )}

      {canManage && (
        <div className="reference-range-add-box">
          <div className="reference-range-add-title">
            {editingRange ? (
              <>
                <EditOutlined style={{ color: '#2563eb' }} />
                <span>Chỉnh sửa khoảng ngưỡng tham chiếu #{editingRange.id?.slice(0, 8)}</span>
              </>
            ) : (
              <>
                <PlusOutlined style={{ color: '#16a34a' }} />
                <span>Khai báo ngưỡng tham chiếu mới</span>
              </>
            )}
          </div>

          <Form form={form} layout="vertical" initialValues={{ gender: 'ALL' }}>
            <Row gutter={12}>
              <Col span={8}>
                <Form.Item
                  name="gender"
                  label="Giới tính áp dụng"
                  rules={[{ required: true, message: 'Vui lòng chọn giới tính!' }]}
                >
                  <Select options={GENDER_OPTIONS} />
                </Form.Item>
              </Col>

              <Col span={8}>
                <Form.Item
                  name="minAge"
                  label="Độ tuổi từ (năm)"
                  tooltip="Bỏ trống nếu không giới hạn tuổi nhỏ nhất"
                >
                  <InputNumber min={0} max={150} placeholder="VD: 18" style={{ width: '100%' }} />
                </Form.Item>
              </Col>

              <Col span={8}>
                <Form.Item
                  name="maxAge"
                  label="Đến tuổi (năm)"
                  tooltip="Bỏ trống nếu không giới hạn tuổi lớn nhất"
                >
                  <InputNumber min={0} max={150} placeholder="VD: 60" style={{ width: '100%' }} />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={12}>
              <Col span={12}>
                <Form.Item
                  name="lowerBound"
                  label={
                    <span>
                      Ngưỡng dưới (Min bình thường){' '}
                      {service?.unit && <Text type="secondary">({service.unit})</Text>}
                    </span>
                  }
                  tooltip="Chỉ số bắt đầu bình thường (>=). Bỏ trống nếu chỉ có cận trên."
                >
                  <InputNumber
                    step="0.01"
                    placeholder="VD: 3.9"
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              </Col>

              <Col span={12}>
                <Form.Item
                  name="upperBound"
                  label={
                    <span>
                      Ngưỡng trên (Max bình thường){' '}
                      {service?.unit && <Text type="secondary">({service.unit})</Text>}
                    </span>
                  }
                  tooltip="Chỉ số kết thúc bình thường (<=). Bỏ trống nếu chỉ có cận dưới."
                >
                  <InputNumber
                    step="0.01"
                    placeholder="VD: 5.5"
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              </Col>
            </Row>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 4 }}>
              {editingRange && (
                <Button onClick={handleCancelEdit} disabled={submitting}>
                  Hủy sửa
                </Button>
              )}
              <Button
                type="primary"
                icon={editingRange ? <SaveOutlined /> : <PlusOutlined />}
                onClick={handleSubmitForm}
                loading={submitting}
              >
                {editingRange ? 'Cập nhật ngưỡng' : 'Thêm ngưỡng tham chiếu'}
              </Button>
            </div>
          </Form>
        </div>
      )}

      <Alert
        type="info"
        showIcon
        icon={<InfoCircleOutlined />}
        message="Quy tắc định chuẩn của hệ thống"
        description={
          <ul style={{ paddingLeft: 16, margin: '4px 0 0 0', fontSize: 12 }}>
            <li>
              Hệ thống tự động dùng các ngưỡng này để đối chiếu khi Bác sĩ nhập kết quả cận lâm sàng theo tuổi và giới tính của người bệnh.
            </li>
            <li>
              Các ngưỡng cùng giới tính không được giao thoa khoảng tuổi để tránh xung đột kết luận.
            </li>
            <li>
              Kết quả &lt; Ngưỡng dưới cảnh báo <strong>THẤP (LOW)</strong>; Kết quả &gt; Ngưỡng trên cảnh báo <strong>CAO (HIGH)</strong>.
            </li>
          </ul>
        }
        style={{ marginBottom: 16 }}
      />

      {ranges.length === 0 && !loading && (
        <Alert
          type="info"
          showIcon
          icon={<InfoCircleOutlined />}
          message="Chưa thiết lập ngưỡng chi tiết dạng số"
          description="Nếu chưa thiết lập ngưỡng chi tiết dạng số, hệ thống sẽ không thể tự động đánh cờ cảnh báo Cao/Thấp khi trả kết quả xét nghiệm."
          style={{ marginBottom: 16 }}
        />
      )}

      <Table
        columns={columns}
        dataSource={ranges}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="middle"
        locale={{
          emptyText: (
            <Empty
              description="Chưa có ngưỡng tham chiếu nào cho kỹ thuật này. Hãy khai báo ở khung phía trên!"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          ),
        }}
      />
    </Drawer>
  )
}
