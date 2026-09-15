import React, { useEffect, useState } from 'react'
import {
  Alert,
  AutoComplete,
  Button,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Tag,
  Typography,
  message,
} from 'antd'
import { EditOutlined, SaveOutlined } from '@ant-design/icons'
import clinicalServiceApi from '../../api/clinicalServiceApi'
import {
  CLINICAL_RESULT_DATA_TYPES,
  CLINICAL_SERVICE_TYPES,
  COMMON_UNITS,
  translateClinicalErrorMessage,
} from '../../utils/clinicalServiceValidation'

const { Text } = Typography
const { TextArea } = Input

export default function ClinicalServiceEditModal({ open, service, onCancel, onSuccess }) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)

  const watchedServiceType = Form.useWatch('serviceType', form)
  const watchedResultDataType = Form.useWatch('resultDataType', form)

  useEffect(() => {
    if (open && service) {
      setErrorMessage(null)
      form.setFieldsValue({
        serviceName: service.serviceName,
        serviceType: service.serviceType,
        resultDataType: service.resultDataType,
        unit: service.unit,
        referenceRange: service.referenceRange,
        description: service.description,
      })
    }
  }, [open, service, form])

  const doSubmit = async (payload) => {
    setSubmitting(true)
    try {
      const res = await clinicalServiceApi.update(service.id, payload)
      message.success('Cập nhật kỹ thuật cận lâm sàng thành công!')
      if (onSuccess) {
        onSuccess(res.data)
      }
    } catch (err) {
      console.error('Lỗi khi cập nhật dịch vụ cận lâm sàng:', err)
      const msg = translateClinicalErrorMessage(err, 'Không thể cập nhật dịch vụ cận lâm sàng.')
      setErrorMessage(msg)
    } finally {
      setSubmitting(false)
    }
  }

  const handleSubmit = async () => {
    if (!service?.id) return

    try {
      setErrorMessage(null)
      const values = await form.validateFields()

      const payload = {
        serviceName: (values.serviceName || '').trim(),
        serviceType: values.serviceType,
        resultDataType: values.resultDataType,
        unit: (values.unit || '').trim() || null,
        referenceRange: (values.referenceRange || '').trim() || null,
        description: (values.description || '').trim() || null,
      }

      if (service.resultDataType === 'NUMBER' && values.resultDataType !== 'NUMBER') {
        let hasRanges = false
        try {
          const checkRes = await clinicalServiceApi.getReferenceRanges(service.id)
          hasRanges = Array.isArray(checkRes.data) && checkRes.data.length > 0
        } catch (e) {
          console.warn('Không thể kiểm tra ngưỡng tham chiếu trước khi đổi kiểu dữ liệu:', e)
          hasRanges = Array.isArray(service.referenceRanges) && service.referenceRanges.length > 0
        }

        if (hasRanges) {
          Modal.confirm({
            title: 'Xác nhận thay đổi kiểu kết quả',
            content:
              'Kỹ thuật này đang có ngưỡng tham chiếu dạng số. Đổi kiểu kết quả sẽ khiến các ngưỡng này không còn áp dụng được. Bạn có chắc chắn muốn tiếp tục?',
            okText: 'Vẫn tiếp tục',
            okType: 'danger',
            cancelText: 'Hủy',
            onOk: () => doSubmit(payload),
            onCancel: () => {
              form.setFieldsValue({
                resultDataType: service.resultDataType,
                unit: service.unit,
              })
            },
          })
          return
        }
      }

      await doSubmit(payload)
    } catch (err) {
      if (err?.errorFields) {
        return
      }
      console.error('Lỗi khi cập nhật dịch vụ cận lâm sàng:', err)
      const msg = translateClinicalErrorMessage(err, 'Không thể cập nhật dịch vụ cận lâm sàng.')
      setErrorMessage(msg)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <EditOutlined style={{ color: '#2563eb', fontSize: 20 }} />
          <span>Chỉnh sửa Kỹ thuật Cận lâm sàng</span>
        </div>
      }
      open={open}
      onCancel={onCancel}
      width={680}
      footer={[
        <Button key="cancel" onClick={onCancel} disabled={submitting}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<SaveOutlined />}
          loading={submitting}
          onClick={handleSubmit}
        >
          Lưu thay đổi
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

        {service && (
          <div
            style={{
              padding: '10px 14px',
              background: '#f8fafc',
              borderRadius: 8,
              marginBottom: 16,
              display: 'flex',
              alignItems: 'center',
              gap: 12,
            }}
          >
            <span style={{ fontSize: 13, color: '#64748b' }}>Mã dịch vụ cố định:</span>
            <Tag color="blue" style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: 13 }}>
              {service.serviceCode}
            </Tag>
          </div>
        )}

        <Form
          form={form}
          layout="vertical"
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
              >
                <AutoComplete
                  placeholder="VD: mmol/L"
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
                tooltip="Mô tả tham chiếu text hiển thị tổng quan"
              >
                <Input placeholder="VD: 3.9 - 5.5" maxLength={255} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="description" label="Hướng dẫn / Ghi chú kỹ thuật">
            <TextArea rows={3} placeholder="Mô tả hướng dẫn chuẩn bị, lưu ý lấy mẫu hoặc chỉ định..." />
          </Form.Item>
        </Form>
      </div>
    </Modal>
  )
}
