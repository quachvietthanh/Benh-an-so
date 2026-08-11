import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  InputNumber,
  message,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import {
  ArrowLeftOutlined,
  DeleteOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  ShopOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useLocation, useNavigate } from 'react-router-dom'
import pharmacyApi from '../api/pharmacyApi'

const { Text, Title } = Typography

const EMPTY_RECEIPT_ITEM = {
  medicineId: undefined,
  batchNumber: '',
  expiryDate: null,
  quantity: 1,
  importPrice: 0,
}

const toCollection = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

const normalizeBatch = (batch) => ({
  ...batch,
  batchId: batch?.batchId || batch?.id,
  id: batch?.batchId || batch?.id,
})

const getErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback

function InventoryReceiptPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [form] = Form.useForm()
  const [medicines, setMedicines] = useState([])
  const [batches, setBatches] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [loadError, setLoadError] = useState('')

  const loadData = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      const [medicineResponse, batchResponse] = await Promise.all([
        pharmacyApi.medicines({ active: true }),
        pharmacyApi.batches(),
      ])
      setMedicines(toCollection(medicineResponse.data).filter((item) => item?.active !== false))
      setBatches(toCollection(batchResponse.data).map(normalizeBatch))
    } catch (error) {
      setLoadError(getErrorMessage(error, 'Không thể tải dữ liệu nhập kho từ máy chủ.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Tự động điền dòng thuốc nếu được chuyển đến từ danh sách cảnh báo thiếu tồn kho
  useEffect(() => {
    if (location.state?.prefillItem && medicines.length > 0) {
      const { medicineId, quantity, medicineName } = location.state.prefillItem
      form.setFieldsValue({
        items: [
          {
            medicineId,
            batchNumber: `LOT-${Date.now().toString().slice(-6)}`,
            expiryDate: dayjs().add(1, 'year'),
            quantity: Math.max(Number(quantity) || 1, 1),
            importPrice: 0,
          },
        ],
        note: `Phiếu nhập bổ sung cho ${medicineName || 'thuốc dưới ngưỡng tồn'}`,
      })
    }
  }, [location.state, medicines, form])

  const medicineOptions = useMemo(
    () => medicines.map((medicine) => ({
      value: medicine.id,
      label: `${medicine.medicineCode || '—'} · ${medicine.medicineName}`,
    })),
    [medicines],
  )

  const handleSubmit = async (values) => {
    const rawItems = Array.isArray(values.items) ? values.items : []
    const duplicateKeys = new Set()
    for (const item of rawItems) {
      const key = `${item.medicineId}-${String(item.batchNumber || '').trim().toLowerCase()}`
      if (duplicateKeys.has(key)) {
        message.error('Không thể nhập trùng cùng một số lô cho cùng một thuốc trong một phiếu.')
        return
      }
      duplicateKeys.add(key)
    }

    const payload = {
      note: String(values.note || '').trim() || null,
      items: rawItems.map((item) => ({
        medicineId: item.medicineId,
        batchNumber: String(item.batchNumber).trim(),
        expiryDate: item.expiryDate.format('YYYY-MM-DD'),
        quantity: Number(item.quantity),
        importPrice: Number(item.importPrice),
      })),
    }

    setSubmitting(true)
    try {
      const response = await pharmacyApi.receiveBatch(payload)
      const receiptId = response.data?.id
      message.success(`Đã nhập kho thành công${receiptId ? `, mã phiếu ${receiptId}` : ''}.`)
      form.resetFields()
      form.setFieldsValue({ items: [{ ...EMPTY_RECEIPT_ITEM }] })
      await loadData()
    } catch (error) {
      message.error(getErrorMessage(error, 'Không thể tạo phiếu nhập kho.'))
    } finally {
      setSubmitting(false)
    }
  }

  const batchColumns = [
    {
      title: 'Thuốc',
      key: 'medicine',
      render: (_, batch) => (
        <Space direction="vertical" size={0}>
          <Text strong>{batch.medicineName || '—'}</Text>
          <Text type="secondary">{batch.medicineCode || '—'}</Text>
        </Space>
      ),
    },
    {
      title: 'Số lô',
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      render: (value) => <Tag color="blue">{value}</Tag>,
    },
    {
      title: 'Hạn sử dụng',
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      render: (value) => {
        const expired = value && dayjs(value).isBefore(dayjs(), 'day')
        const nearExpiry = value && !expired && dayjs(value).diff(dayjs(), 'day') <= 30
        return (
          <Tag color={expired ? 'red' : nearExpiry ? 'orange' : 'green'}>
            {value ? dayjs(value).format('DD/MM/YYYY') : '—'}
          </Tag>
        )
      },
    },
    {
      title: 'Tồn lô',
      dataIndex: 'quantity',
      key: 'quantity',
      align: 'right',
      render: (value) => Number(value || 0).toLocaleString('vi-VN'),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (value, batch) => (
        <Space wrap>
          <Tag>{value || '—'}</Tag>
          <Tag color={batch.eligibleForDispense ? 'green' : 'red'}>
            {batch.eligibleForDispense ? 'Được cấp phát' : 'Không đủ điều kiện'}
          </Tag>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ paddingBottom: 32 }}>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ margin: 0 }}>
            <ShopOutlined /> Nhập kho theo lô
          </Title>
          <Text type="secondary">Tạo phiếu nhập kho thật và theo dõi hạn sử dụng của từng lô thuốc.</Text>
        </div>
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/pharmacy')}>Về cấp phát</Button>
          <Button icon={<MedicineBoxOutlined />} onClick={() => navigate('/medicines')}>Danh mục thuốc</Button>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadData}>Làm mới</Button>
        </Space>
      </div>

      {loadError && (
        <Alert
          type="error"
          showIcon
          message="Không tải được dữ liệu kho"
          description={loadError}
          action={<Button size="small" onClick={loadData}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      <Row gutter={[16, 16]} align="top">
        <Col xs={24} xl={10}>
          <Card title="Phiếu nhập kho mới">
            <Form
              form={form}
              layout="vertical"
              initialValues={{ note: '', items: [{ ...EMPTY_RECEIPT_ITEM }] }}
              onFinish={handleSubmit}
            >
              <Form.Item name="note" label="Ghi chú phiếu nhập">
                <Input.TextArea rows={2} maxLength={1000} showCount placeholder="Nguồn nhập, số hóa đơn hoặc ghi chú kiểm nhận" />
              </Form.Item>

              <Form.List
                name="items"
                rules={[{
                  validator: async (_, items) => {
                    if (!items || items.length === 0) throw new Error('Phiếu nhập phải có ít nhất một dòng thuốc.')
                  },
                }]}
              >
                {(fields, { add, remove }, { errors }) => (
                  <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                    {fields.map((field, index) => (
                      <Card
                        key={field.key}
                        size="small"
                        title={`Dòng thuốc ${index + 1}`}
                        extra={fields.length > 1 && (
                          <Button danger type="text" icon={<DeleteOutlined />} onClick={() => remove(field.name)}>
                            Xóa
                          </Button>
                        )}
                      >
                        <Form.Item
                          {...field}
                          name={[field.name, 'medicineId']}
                          label="Thuốc"
                          rules={[{ required: true, message: 'Chọn thuốc cần nhập' }]}
                        >
                          <Select
                            showSearch
                            optionFilterProp="label"
                            options={medicineOptions}
                            placeholder="Chọn thuốc đang hoạt động"
                          />
                        </Form.Item>
                        <Form.Item
                          {...field}
                          name={[field.name, 'batchNumber']}
                          label="Số lô"
                          rules={[
                            { required: true, whitespace: true, message: 'Nhập số lô' },
                            { max: 100, message: 'Số lô không vượt quá 100 ký tự' },
                          ]}
                        >
                          <Input placeholder="Ví dụ: LOT-2026-001" />
                        </Form.Item>
                        <Row gutter={12}>
                          <Col xs={24} md={8}>
                            <Form.Item
                              {...field}
                              name={[field.name, 'expiryDate']}
                              label="Hạn sử dụng"
                              rules={[{ required: true, message: 'Chọn hạn sử dụng' }]}
                            >
                              <DatePicker
                                style={{ width: '100%' }}
                                format="DD/MM/YYYY"
                                disabledDate={(current) => current && !current.isAfter(dayjs(), 'day')}
                              />
                            </Form.Item>
                          </Col>
                          <Col xs={24} md={8}>
                            <Form.Item
                              {...field}
                              name={[field.name, 'quantity']}
                              label="Số lượng"
                              rules={[{ required: true, message: 'Nhập số lượng' }]}
                            >
                              <InputNumber min={1} precision={0} style={{ width: '100%' }} />
                            </Form.Item>
                          </Col>
                          <Col xs={24} md={8}>
                            <Form.Item
                              {...field}
                              name={[field.name, 'importPrice']}
                              label="Giá nhập"
                              rules={[{ required: true, message: 'Nhập giá nhập' }]}
                            >
                              <InputNumber min={0} precision={2} style={{ width: '100%' }} addonAfter="₫" />
                            </Form.Item>
                          </Col>
                        </Row>
                      </Card>
                    ))}
                    <Form.ErrorList errors={errors} />
                    <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add({ ...EMPTY_RECEIPT_ITEM })}>
                      Thêm dòng thuốc
                    </Button>
                  </Space>
                )}
              </Form.List>

              <Button
                type="primary"
                htmlType="submit"
                size="large"
                block
                icon={<SaveOutlined />}
                loading={submitting}
                disabled={loading || medicines.length === 0}
                style={{ marginTop: 20 }}
              >
                Xác nhận nhập kho
              </Button>
            </Form>
          </Card>
        </Col>

        <Col xs={24} xl={14}>
          <Card title={`Các lô hiện có (${batches.length})`}>
            <Table
              rowKey={(batch) => batch.batchId}
              columns={batchColumns}
              dataSource={batches}
              loading={loading}
              pagination={{ pageSize: 10, showSizeChanger: false }}
              scroll={{ x: 700 }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default InventoryReceiptPage
