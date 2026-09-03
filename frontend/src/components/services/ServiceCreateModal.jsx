import React from 'react'
import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Space,
  Typography,
} from 'antd'
import {
  CalendarOutlined,
  InfoCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

const { Title, Text } = Typography

function ServiceCreateModal({
  open,
  onCancel,
  onFinish,
  form,
  loading,
  formError,
  onClearError,
}) {
  const handleValuesChange = (changedValues) => {
    if (onClearError) onClearError()
    // Clear field-level error for the field that was changed
    const changedField = Object.keys(changedValues)[0]
    if (changedField && form) {
      form.setFields([{ name: changedField, errors: [] }])
    }
  }

  return (
    <Modal
      className="service-form-modal"
      width={620}
      title={
        <div className="service-modal-title">
          <span className="service-name-icon">
            <PlusOutlined />
          </span>
          <div>
            <Title level={4} style={{ margin: 0 }}>Thêm dịch vụ khám mới</Title>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Nhập mã dịch vụ, tên dịch vụ và thiết lập mức giá niêm yết ban đầu.
            </Text>
          </div>
        </div>
      }
      open={open}
      footer={null}
      centered
      destroyOnClose
      maskClosable={!loading}
      closable={!loading}
      onCancel={onCancel}
    >
      <Form
        className="service-form"
        form={form}
        layout="vertical"
        onFinish={onFinish}
        onValuesChange={handleValuesChange}
      >
        {formError && (
          <Alert
            className="service-modal-alert"
            type="error"
            showIcon
            message={formError}
            closable
            onClose={onClearError}
            style={{ marginBottom: 16 }}
          />
        )}

        <div className="service-form-grid">
          <Form.Item
            name="serviceCode"
            label="Mã dịch vụ"
            normalize={(val) => (val ? String(val).toUpperCase() : val)}
            rules={[
              { required: true, message: 'Vui lòng nhập mã dịch vụ' },
              { max: 50, message: 'Mã không quá 50 ký tự' },
              {
                pattern: /^[A-Za-z0-9_.-]+$/,
                message: 'Mã dịch vụ chỉ gồm chữ cái, chữ số, gạch ngang (-) hoặc gạch dưới (_)',
              },
            ]}
          >
            <Input size="large" placeholder="VD: DV-KHAM-NOI, XQ-TIM-PHOI..." />
          </Form.Item>

          <Form.Item
            name="name"
            label="Tên dịch vụ khám / thủ thuật"
            rules={[
              { required: true, message: 'Vui lòng nhập tên dịch vụ' },
              { max: 255, message: 'Tên không quá 255 ký tự' },
            ]}
          >
            <Input size="large" placeholder="Nhập tên dịch vụ y tế" />
          </Form.Item>

          <Form.Item
            name="price"
            label="Đơn giá niêm yết ban đầu"
            rules={[{ required: true, message: 'Vui lòng nhập đơn giá' }]}
          >
            <InputNumber
              size="large"
              min={0}
              step={10000}
              controls={false}
              formatter={(val) => `${val || ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
              parser={(val) => val?.replace(/\./g, '') || ''}
              addonAfter="₫"
              placeholder="0"
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item
            name="effectiveFrom"
            label="Ngày bắt đầu hiệu lực"
            rules={[{ required: true, message: 'Vui lòng chọn ngày hiệu lực' }]}
          >
            <div>
              <DatePicker
                size="large"
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                placeholder="Chọn ngày áp dụng"
              />
              <div style={{ marginTop: 6, display: 'flex', gap: 6, alignItems: 'center' }}>
                <Text type="secondary" style={{ fontSize: 11 }}>Chọn nhanh:</Text>
                <Button
                  size="small"
                  type="dashed"
                  icon={<CalendarOutlined />}
                  onClick={() => {
                    form.setFieldsValue({ effectiveFrom: dayjs() })
                    if (onClearError) onClearError()
                  }}
                  style={{ fontSize: 11, height: 24, padding: '0 8px' }}
                >
                  Hôm nay
                </Button>
                <Button
                  size="small"
                  type="dashed"
                  onClick={() => {
                    form.setFieldsValue({ effectiveFrom: dayjs().add(1, 'day') })
                    if (onClearError) onClearError()
                  }}
                  style={{ fontSize: 11, height: 24, padding: '0 8px' }}
                >
                  Ngày mai
                </Button>
              </div>
            </div>
          </Form.Item>
        </div>

        <div className="service-modal-hint" style={{ marginTop: 8 }}>
          <InfoCircleOutlined />
          <span>
            Đơn giá này sẽ tự động có hiệu lực kể từ ngày được chỉ định và áp dụng trực tiếp khi lập hóa đơn thu phí cho bệnh nhân.
          </span>
        </div>

        <div className="service-modal-actions">
          <Button onClick={onCancel} disabled={loading}>
            Hủy bỏ
          </Button>
          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            style={{ background: '#2563eb' }}
          >
            Lưu dịch vụ
          </Button>
        </div>
      </Form>
    </Modal>
  )
}

export default ServiceCreateModal
