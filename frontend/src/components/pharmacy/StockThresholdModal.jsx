import React, { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Descriptions,
  Divider,
  Form,
  InputNumber,
  Modal,
  Radio,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  CheckCircleOutlined,
  DashboardOutlined,
  MedicineBoxOutlined,
  SaveOutlined,
} from '@ant-design/icons'
import medicineApi from '../../api/medicineApi'

const { Text, Title, Paragraph } = Typography

const PRESET_THRESHOLDS = [10, 20, 50, 100, 200, 500]

function StockThresholdModal({ open, onCancel, onSuccess, medicine }) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [currentThreshold, setCurrentThreshold] = useState(0)

  useEffect(() => {
    if (open && medicine) {
      const initialThreshold = Number(medicine.minStockThreshold ?? 0)
      setCurrentThreshold(initialThreshold)
      form.setFieldsValue({
        minStockThreshold: initialThreshold,
      })
    }
  }, [open, medicine, form])

  if (!medicine) return null

  const currentStock = Number(medicine.stockQuantity ?? 0)
  const isShortage = currentStock < currentThreshold && currentThreshold > 0
  const isZeroStock = currentStock === 0

  const handlePresetClick = (val) => {
    setCurrentThreshold(val)
    form.setFieldsValue({ minStockThreshold: val })
  }

  const handleSubmit = async (values) => {
    const nextThreshold = Number(values.minStockThreshold ?? 0)
    if (nextThreshold < 0) {
      message.error('Ngưỡng tồn kho tối thiểu không được âm.')
      return
    }

    setSubmitting(true)
    try {
      const payload = {
        medicineName: medicine.medicineName,
        activeIngredient: medicine.activeIngredient,
        strength: medicine.strength,
        dosageForm: medicine.dosageForm,
        unit: medicine.unit,
        defaultRoute: medicine.defaultRoute,
        minStockThreshold: nextThreshold,
      }

      await medicineApi.update(medicine.id, payload)
      message.success(
        `Đã cập nhật ngưỡng tồn tối thiểu cho ${medicine.medicineName}: ${nextThreshold} ${medicine.unit || ''}`
      )
      onSuccess?.(nextThreshold)
      onCancel?.()
    } catch (error) {
      const msg =
        error?.response?.data?.message ||
        error?.message ||
        'Không thể cập nhật ngưỡng tồn kho.'
      message.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={
        <Space align="center">
          <DashboardOutlined style={{ color: '#1677ff', fontSize: 20 }} />
          <span>Thiết lập ngưỡng tồn kho tối thiểu</span>
        </Space>
      }
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Lưu ngưỡng tồn"
      okButtonProps={{ icon: <SaveOutlined /> }}
      cancelText="Hủy"
      width={560}
      destroyOnClose
    >
      <div style={{ marginTop: 12 }}>
        <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="Mã thuốc" span={1}>
            <Text code>{medicine.medicineCode || '—'}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Đơn vị tính" span={1}>
            <Tag color="blue">{medicine.unit || '—'}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Tên thuốc" span={2}>
            <strong>{medicine.medicineName || '—'}</strong>
          </Descriptions.Item>
          <Descriptions.Item label="Hoạt chất & Hàm lượng" span={2}>
            {[medicine.activeIngredient, medicine.strength].filter(Boolean).join(' · ') || '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Tồn kho thực tế" span={2}>
            <Space>
              <span
                style={{
                  fontSize: 16,
                  fontWeight: 700,
                  color: isZeroStock ? '#dc2626' : currentStock < currentThreshold ? '#d97706' : '#16a34a',
                }}
              >
                {currentStock.toLocaleString('vi-VN')} {medicine.unit || ''}
              </span>
              {isZeroStock ? (
                <Tag color="red">Hết hàng</Tag>
              ) : currentStock < currentThreshold ? (
                <Tag color="orange">Dưới ngưỡng</Tag>
              ) : (
                <Tag color="green">An toàn</Tag>
              )}
            </Space>
          </Descriptions.Item>
        </Descriptions>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ minStockThreshold: Number(medicine.minStockThreshold ?? 0) }}
        >
          <Form.Item
            name="minStockThreshold"
            label={
              <Space>
                <strong>Ngưỡng tồn kho tối thiểu cảnh báo</strong>
                <Text type="secondary">(Đơn vị: {medicine.unit || 'đơn vị'})</Text>
              </Space>
            }
            rules={[
              { required: true, message: 'Vui lòng nhập ngưỡng tồn tối thiểu.' },
              {
                type: 'number',
                min: 0,
                message: 'Ngưỡng tồn phải lớn hơn hoặc bằng 0.',
              },
            ]}
            extra="Khi lượng tồn khả dụng trong kho thấp hơn mức này, hệ thống sẽ tự động bật cảnh báo thiếu hàng cho dược sĩ."
          >
            <InputNumber
              min={0}
              max={1000000}
              precision={0}
              style={{ width: '100%', fontSize: 16 }}
              placeholder="Nhập số lượng tồn tối thiểu (VD: 50)"
              onChange={(val) => setCurrentThreshold(Number(val ?? 0))}
              addonAfter={medicine.unit || ''}
            />
          </Form.Item>

          <div style={{ marginBottom: 16 }}>
            <Text type="secondary" style={{ display: 'block', marginBottom: 8, fontSize: 12 }}>
              Gợi ý nhanh mức tồn:
            </Text>
            <Space wrap>
              {PRESET_THRESHOLDS.map((preset) => (
                <Button
                  key={preset}
                  size="small"
                  type={currentThreshold === preset ? 'primary' : 'default'}
                  onClick={() => handlePresetClick(preset)}
                >
                  {preset} {medicine.unit || ''}
                </Button>
              ))}
            </Space>
          </div>

          <Divider style={{ margin: '12px 0' }} />

          {/* Dự báo trạng thái cảnh báo sau khi thiết lập */}
          <div style={{ marginTop: 8 }}>
            {currentThreshold <= 0 ? (
              <Alert
                type="info"
                showIcon
                message="Chưa đặt ngưỡng cảnh báo"
                description="Ngưỡng = 0 nghĩa là hệ thống sẽ không cảnh báo thiếu hàng cho loại thuốc này cho đến khi hết hàng hoàn toàn."
              />
            ) : isShortage ? (
              <Alert
                type="warning"
                showIcon
                icon={<AlertOutlined />}
                message={`Cảnh báo: Tồn kho hiện tại (${currentStock}) thấp hơn ngưỡng mới (${currentThreshold})`}
                description={
                  <span>
                    Hệ thống sẽ ghi nhận cảnh báo thiếu hụt <strong>{currentThreshold - currentStock} {medicine.unit}</strong> và hiển thị tại trung tâm cảnh báo để dược sĩ bổ sung.
                  </span>
                }
              />
            ) : (
              <Alert
                type="success"
                showIcon
                icon={<CheckCircleOutlined />}
                message={`Trạng thái an toàn (${currentStock} ≥ ${currentThreshold})`}
                description={`Lượng thuốc hiện có trong kho đủ đáp ứng mức tồn tối thiểu đã thiết lập.`}
              />
            )}
          </div>
        </Form>
      </div>
    </Modal>
  )
}

export default StockThresholdModal
