import React from 'react'
import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  EditOutlined,
  InfoCircleOutlined,
  StopOutlined,
} from '@ant-design/icons'

const { Title, Text } = Typography

function ServiceEditModal({
  open,
  onCancel,
  onFinish,
  form,
  loading,
  formError,
  onClearError,
}) {
  return (
    <Modal
      className="service-form-modal"
      width={620}
      title={
        <div className="service-modal-title">
          <span className="service-name-icon">
            <EditOutlined />
          </span>
          <div>
            <Title level={4} style={{ margin: 0 }}>Cập nhật dịch vụ & Điều chỉnh giá</Title>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Điều chỉnh tên dịch vụ, đơn giá áp dụng hoặc ngày hiệu lực mới.
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
        onValuesChange={onClearError}
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
          <Form.Item name="serviceCode" label="Mã dịch vụ">
            <Input size="large" disabled style={{ background: '#f8fafc', color: '#0f172a', fontWeight: 600 }} />
          </Form.Item>

          <Form.Item
            name="name"
            label="Tên dịch vụ"
            rules={[
              { required: true, message: 'Vui lòng nhập tên dịch vụ' },
              { max: 255, message: 'Tên không quá 255 ký tự' },
            ]}
          >
            <Input size="large" placeholder="Nhập tên dịch vụ" />
          </Form.Item>

          <Form.Item
            name="price"
            label="Đơn giá niêm yết"
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
            label="Ngày hiệu lực giá"
            rules={[{ required: true, message: 'Vui lòng chọn ngày hiệu lực' }]}
          >
            <DatePicker
              size="large"
              format="DD/MM/YYYY"
              style={{ width: '100%' }}
              placeholder="Chọn ngày áp dụng"
            />
          </Form.Item>

          <Form.Item className="service-form-full" name="active" label="Trạng thái áp dụng">
            <Radio.Group size="large" buttonStyle="solid">
              <Radio.Button value={true}>
                <CheckCircleOutlined style={{ color: '#10b981', marginRight: 6 }} />
                Đang hiệu lực
              </Radio.Button>
              <Radio.Button value={false}>
                <StopOutlined style={{ color: '#ef4444', marginRight: 6 }} />
                Ngừng áp dụng
              </Radio.Button>
            </Radio.Group>
          </Form.Item>
        </div>

        <div className="service-modal-hint" style={{ marginTop: 8 }}>
          <InfoCircleOutlined />
          <span>
            Khi cập nhật đơn giá kèm ngày hiệu lực mới, hệ thống sẽ tự động lưu phiên bản giá mới vào lịch sử giá mà không ảnh hưởng đến các hóa đơn đã lập trước đó.
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
            Lưu thay đổi
          </Button>
        </div>
      </Form>
    </Modal>
  )
}

export default ServiceEditModal
