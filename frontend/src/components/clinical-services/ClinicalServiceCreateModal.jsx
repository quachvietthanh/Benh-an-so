import React, { useEffect, useRef, useState } from 'react'
import {
  Alert,
  AutoComplete,
  Button,
  Col,
  Divider,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Typography,
  message,
} from 'antd'
import {
  AppstoreAddOutlined,
  ExperimentOutlined,
  InfoCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons'
import systemApi from '../../api/systemApi'
import clinicalServiceApi from '../../api/clinicalServiceApi'
import {
  CLINICAL_RESULT_DATA_TYPES,
  CLINICAL_SERVICE_TYPES,
  COMMON_UNITS,
  translateClinicalErrorMessage,
} from '../../utils/clinicalServiceValidation'

const { Text } = Typography
const { TextArea } = Input

export default function ClinicalServiceCreateModal({ open, onCancel, onSuccess }) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [parentServices, setParentServices] = useState([])
  const [loadingParents, setLoadingParents] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)

  const watchedServiceType = Form.useWatch('serviceType', form)
  const watchedResultDataType = Form.useWatch('resultDataType', form)

  const searchTimeoutRef = useRef(null)

  const loadParentServices = async (keyword = '') => {
    setLoadingParents(true)
    try {
      const params = { size: 50, active: true }
      if (keyword && keyword.trim()) {
        params.keyword = keyword.trim()
      }
      const res = await systemApi.services(params)
      const list = Array.isArray(res.data?.content)
        ? res.data.content
        : Array.isArray(res.data)
          ? res.data
          : []
      setParentServices(list)
    } catch (err) {
      console.error('Không thể nạp danh mục dịch vụ viện phí cha:', err)
      setParentServices([])
    } finally {
      setLoadingParents(false)
    }
  }

  const handleSearchParent = (value) => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current)
    }
    searchTimeoutRef.current = setTimeout(() => {
      loadParentServices(value)
    }, 300)
  }

  useEffect(() => {
    if (open) {
      form.resetFields()
      setErrorMessage(null)
      loadParentServices('')
    }
    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current)
      }
    }
  }, [open, form])

  const handleParentSelect = (selectedId) => {
    const parent = parentServices.find((p) => String(p.id) === String(selectedId))
    if (parent) {
      const currentCode = form.getFieldValue('serviceCode')
      const currentName = form.getFieldValue('serviceName')

      if (!currentCode) {
        form.setFieldsValue({ serviceCode: parent.serviceCode })
      }
      if (!currentName) {
        form.setFieldsValue({ serviceName: parent.serviceName })
      }

      const codeUpper = String(parent.serviceCode || '').toUpperCase()
      if (codeUpper.startsWith('LAB-')) {
        form.setFieldsValue({ serviceType: 'LAB_TEST', resultDataType: 'NUMBER' })
      } else if (codeUpper.startsWith('IMG-')) {
        form.setFieldsValue({ serviceType: 'IMAGING', resultDataType: 'FILE', unit: undefined })
      } else if (codeUpper.startsWith('OTH-')) {
        form.setFieldsValue({ serviceType: 'OTHER' })
      }
    }
  }

  const handleSubmit = async () => {
    try {
      setErrorMessage(null)
      const values = await form.validateFields()
      setSubmitting(true)

      const payload = {
        serviceCatalogId: values.serviceCatalogId,
        serviceCode: (values.serviceCode || '').trim().toUpperCase(),
        serviceName: (values.serviceName || '').trim(),
        serviceType: values.serviceType,
        resultDataType: values.resultDataType,
        unit: (values.unit || '').trim() || null,
        referenceRange: (values.referenceRange || '').trim() || null,
        description: (values.description || '').trim() || null,
      }

      const res = await clinicalServiceApi.create(payload)
      message.success('Thêm mới dịch vụ cận lâm sàng thành công!')
      form.resetFields()
      if (onSuccess) {
        onSuccess(res.data)
      }
    } catch (err) {
      if (err?.errorFields) {
        return
      }
      console.error('Lỗi khi tạo dịch vụ cận lâm sàng:', err)
      const msg = translateClinicalErrorMessage(err, 'Không thể tạo dịch vụ cận lâm sàng. Vui lòng thử lại.')
      setErrorMessage(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <ExperimentOutlined style={{ color: '#2563eb', fontSize: 20 }} />
          <span>Thêm mới Kỹ thuật Cận lâm sàng</span>
        </div>
      }
      open={open}
      onCancel={onCancel}
      width={720}
      footer={[
        <Button key="cancel" onClick={onCancel} disabled={submitting}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<PlusOutlined />}
          loading={submitting}
          onClick={handleSubmit}
        >
          Lưu kỹ thuật CLS
        </Button>,
      ]}
      destroyOnClose
    >
      <div style={{ marginTop: 12 }}>
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

        <Form
          form={form}
          layout="vertical"
          initialValues={{ serviceType: 'LAB_TEST', resultDataType: 'NUMBER' }}
          onValuesChange={(changedValues, allValues) => {
            if (changedValues.serviceType === 'IMAGING') {
              if (allValues.resultDataType === 'NUMBER') {
                form.setFieldsValue({ resultDataType: 'FILE', unit: undefined })
                message.info('Kỹ thuật Chẩn đoán hình ảnh không hỗ trợ kiểu kết quả Số trị, đã tự động chuyển sang kiểu Tệp tin!')
              }
            }
            if (changedValues.resultDataType && changedValues.resultDataType !== 'NUMBER') {
              form.setFieldValue('unit', undefined)
              form.clearValidate(['unit'])
            }
          }}
        >
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                name="serviceCatalogId"
                label={
                  <span>
                    Dịch vụ viện phí liên kết <Text type="danger">*</Text>
                  </span>
                }
                rules={[{ required: true, message: 'Vui lòng chọn dịch vụ viện phí cha!' }]}
                tooltip="Kỹ thuật cận lâm sàng bắt buộc phải gắn với một dịch vụ có trong bảng giá viện phí"
              >
                <Select
                  showSearch
                  placeholder="Tìm và chọn dịch vụ viện phí (VD: Blood glucose, Head CT scan...)"
                  loading={loadingParents}
                  onSearch={handleSearchParent}
                  onChange={handleParentSelect}
                  filterOption={false}
                  options={parentServices.map((p) => ({
                    value: p.id,
                    label: `[${p.serviceCode}] ${p.serviceName}`,
                  }))}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                name="serviceCode"
                label={
                  <span>
                    Mã kỹ thuật CLS <Text type="danger">*</Text>
                  </span>
                }
                rules={[
                  { required: true, message: 'Vui lòng nhập mã kỹ thuật!' },
                  { max: 30, message: 'Mã không quá 30 ký tự!' },
                ]}
              >
                <Input
                  placeholder="VD: LAB-GLU"
                  maxLength={30}
                  style={{ textTransform: 'uppercase' }}
                  onChange={(e) => form.setFieldsValue({ serviceCode: e.target.value.toUpperCase() })}
                />
              </Form.Item>
            </Col>

            <Col span={16}>
              <Form.Item
                name="serviceName"
                label={
                  <span>
                    Tên kỹ thuật CLS <Text type="danger">*</Text>
                  </span>
                }
                rules={[
                  { required: true, message: 'Vui lòng nhập tên kỹ thuật!' },
                  { max: 150, message: 'Tên không quá 150 ký tự!' },
                ]}
              >
                <Input placeholder="VD: Định lượng Glucose máu" maxLength={150} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="serviceType"
                label={
                  <span>
                    Loại kỹ thuật CLS <Text type="danger">*</Text>
                  </span>
                }
                rules={[{ required: true, message: 'Vui lòng chọn loại kỹ thuật!' }]}
              >
                <Select
                  options={CLINICAL_SERVICE_TYPES.map((t) => ({
                    value: t.value,
                    label: t.label,
                  }))}
                />
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                name="resultDataType"
                label={
                  <span>
                    Kiểu kết quả đo lường <Text type="danger">*</Text>
                  </span>
                }
                rules={[{ required: true, message: 'Vui lòng chọn kiểu dữ liệu!' }]}
              >
                <Select
                  options={CLINICAL_RESULT_DATA_TYPES.map((t) => ({
                    value: t.value,
                    label: t.label,
                    disabled: watchedServiceType === 'IMAGING' && t.value === 'NUMBER',
                  }))}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={10}>
              <Form.Item
                name="unit"
                label={
                  <span>
                    Đơn vị đo {watchedResultDataType === 'NUMBER' && <Text type="danger">*</Text>}
                  </span>
                }
                rules={[
                  {
                    required: watchedResultDataType === 'NUMBER',
                    message: 'Vui lòng nhập đơn vị đo cho kết quả định lượng (Số trị)!',
                  },
                ]}
                tooltip="Ví dụ: mmol/L, mg/dL, g/L, %, 10^9/L..."
              >
                <AutoComplete
                  placeholder="Chọn hoặc nhập đơn vị"
                  options={COMMON_UNITS.map((u) => ({ value: u }))}
                  filterOption={(inputValue, option) =>
                    option?.value?.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                  }
                  disabled={watchedResultDataType === 'FILE'}
                />
              </Form.Item>
            </Col>

            <Col span={14}>
              <Form.Item
                name="referenceRange"
                label="Khoảng tham chiếu mô tả chung"
                tooltip="Chuỗi hiển thị tham chiếu tổng quát (VD: 3.9 - 5.5). Bạn có thể cấu hình chi tiết theo tuổi/giới tính sau."
              >
                <Input placeholder="VD: 3.9 - 5.5 hoặc < 200" maxLength={255} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="description" label="Hướng dẫn / Ghi chú kỹ thuật">
            <TextArea
              rows={3}
              placeholder="VD: Hướng dẫn lấy mẫu máu lúc đói, nhịn ăn ít nhất 8 tiếng trước khi lấy máu..."
            />
          </Form.Item>

          <Alert
            type="info"
            showIcon
            icon={<InfoCircleOutlined />}
            message="Thiết lập ngưỡng tham chiếu chi tiết"
            description="Sau khi tạo dịch vụ, bạn có thể thiết lập các khoảng tham chiếu chuyên sâu (chia theo giới tính Nam/Nữ và từng khoảng độ tuổi) tại nút 'Ngưỡng tham chiếu' trên danh sách."
          />
        </Form>
      </div>
    </Modal>
  )
}
