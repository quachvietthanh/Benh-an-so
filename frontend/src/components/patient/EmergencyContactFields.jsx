import React from 'react'
import { Form, Input, Select, Space, Button, Typography, Tooltip } from 'antd'
import {
  PhoneOutlined,
  UserOutlined,
  HeartOutlined,
  ClearOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import {
  EMERGENCY_RELATIONSHIPS,
  VIETNAMESE_PHONE_REGEX,
} from '../../utils/emergencyContactValidation'

const { Text } = Typography

/**
 * Cụm trường nhập thông tin Người liên hệ khẩn cấp (NCL-02-CN-007)
 * Dùng chung cho Form Đăng ký mới và Form Cập nhật bệnh nhân.
 * 
 * @param {Object} props
 * @param {FormInstance} props.form - Ant Design form instance để thực hiện reset/clear
 * @param {boolean} [props.layoutGrid=false] - True nếu dùng trong grid layout
 */
export default function EmergencyContactFields({ form, layoutGrid = false }) {
  const handleClearEmergency = () => {
    if (form) {
      form.setFieldsValue({
        emergencyContact: '',
        emergencyRelationship: null,
        emergencyPhone: '',
      })
      // Trigger validate lại cụm trường
      form.validateFields(['emergencyContact', 'emergencyRelationship', 'emergencyPhone']).catch(() => {})
    }
  }

  // Validator kiểm tra Cohesive Triplet thời gian thực
  const cohesiveTripletValidator = {
    validator(_, value) {
      if (!form) return Promise.resolve()
      const contact = form.getFieldValue('emergencyContact')?.trim() || ''
      const relationship = form.getFieldValue('emergencyRelationship')?.trim() || ''
      const phone = form.getFieldValue('emergencyPhone')?.trim() || ''

      const hasAny = Boolean(contact || relationship || phone)
      if (!hasAny) {
        return Promise.resolve()
      }

      // Đang nhập dở
      return Promise.resolve()
    },
  }

  const relationshipOptions = EMERGENCY_RELATIONSHIPS.map((rel) => ({
    value: rel,
    label: rel,
  }))

  const headerNotice = (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
      <Space size={4}>
        <HeartOutlined style={{ color: '#ef4444' }} />
        <Text strong style={{ fontSize: 13, color: '#0f172a' }}>
          Người liên hệ khẩn cấp
        </Text>
        <Tooltip title="Thông tin liên hệ khẩn cấp là tùy chọn. Nếu nhập thì bắt buộc phải nhập đủ Họ tên, Mối quan hệ và Số điện thoại (Quy tắc Cohesive Triplet).">
          <InfoCircleOutlined style={{ color: '#64748b', cursor: 'pointer', fontSize: 12 }} />
        </Tooltip>
      </Space>
      {form && (
        <Button
          type="link"
          size="small"
          danger
          icon={<ClearOutlined />}
          onClick={handleClearEmergency}
          style={{ padding: 0, fontSize: 12 }}
        >
          Xóa trắng liên hệ khẩn cấp
        </Button>
      )}
    </div>
  )

  if (layoutGrid) {
    return (
      <div style={{ width: '100%', marginTop: 4 }}>
        {headerNotice}
        <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr', gap: 12 }}>
          <Form.Item
            name="emergencyContact"
            label="Họ và tên người liên hệ"
            rules={[
              { max: 100, message: 'Họ tên không vượt quá 100 ký tự' },
              ({ getFieldValue }) => ({
                validator(_, val) {
                  const hasOther = Boolean(getFieldValue('emergencyRelationship') || getFieldValue('emergencyPhone'))
                  if (hasOther && (!val || !val.trim())) {
                    return Promise.reject(new Error('Vui lòng nhập họ tên người liên hệ'))
                  }
                  return Promise.resolve()
                },
              }),
            ]}
            style={{ marginBottom: 12 }}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#94a3b8' }} />}
              placeholder="Ví dụ: Nguyễn Thị Hoa"
              allowClear
            />
          </Form.Item>

          <Form.Item
            name="emergencyRelationship"
            label="Mối quan hệ"
            rules={[
              { max: 50, message: 'Mối quan hệ không vượt quá 50 ký tự' },
              ({ getFieldValue }) => ({
                validator(_, val) {
                  const hasOther = Boolean(getFieldValue('emergencyContact') || getFieldValue('emergencyPhone'))
                  if (hasOther && (!val || !val.trim())) {
                    return Promise.reject(new Error('Vui lòng chọn hoặc nhập quan hệ'))
                  }
                  return Promise.resolve()
                },
              }),
            ]}
            style={{ marginBottom: 12 }}
          >
            <Select
              showSearch
              allowClear
              placeholder="Chọn hoặc nhập quan hệ"
              options={relationshipOptions}
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
            />
          </Form.Item>

          <Form.Item
            name="emergencyPhone"
            label="Số điện thoại khẩn cấp"
            rules={[
              ({ getFieldValue }) => ({
                validator(_, val) {
                  const contact = getFieldValue('emergencyContact')?.trim()
                  const relationship = getFieldValue('emergencyRelationship')?.trim()
                  const phone = val?.trim()
                  const hasOther = Boolean(contact || relationship)

                  if (hasOther && !phone) {
                    return Promise.reject(new Error('Vui lòng nhập SĐT khẩn cấp'))
                  }
                  if (phone && !VIETNAMESE_PHONE_REGEX.test(phone)) {
                    return Promise.reject(new Error('SĐT phải gồm 10 số, bắt đầu bằng 03, 05, 07, 08, 09'))
                  }
                  return Promise.resolve()
                },
              }),
            ]}
            style={{ marginBottom: 12 }}
          >
            <Input
              prefix={<PhoneOutlined style={{ color: '#94a3b8' }} />}
              placeholder="09xxxxxxxx"
              allowClear
            />
          </Form.Item>
        </div>
      </div>
    )
  }

  return (
    <div style={{ width: '100%', marginBottom: 12, padding: '12px 14px', background: '#f8fafc', borderRadius: 8, border: '1px solid #e2e8f0' }}>
      {headerNotice}
      <Space.Compact block style={{ display: 'flex' }}>
        <Form.Item
          name="emergencyContact"
          label="Họ tên người liên hệ"
          rules={[
            { max: 100, message: 'Tối đa 100 ký tự' },
            ({ getFieldValue }) => ({
              validator(_, val) {
                const hasOther = Boolean(getFieldValue('emergencyRelationship') || getFieldValue('emergencyPhone'))
                if (hasOther && (!val || !val.trim())) {
                  return Promise.reject(new Error('Nhập họ tên'))
                }
                return Promise.resolve()
              },
            }),
          ]}
          style={{ flex: '1.2 1 0', marginRight: 8, marginBottom: 0 }}
        >
          <Input prefix={<UserOutlined style={{ color: '#94a3b8' }} />} placeholder="Họ tên người liên hệ" allowClear />
        </Form.Item>

        <Form.Item
          name="emergencyRelationship"
          label="Quan hệ nhân thân"
          rules={[
            { max: 50, message: 'Tối đa 50 ký tự' },
            ({ getFieldValue }) => ({
              validator(_, val) {
                const hasOther = Boolean(getFieldValue('emergencyContact') || getFieldValue('emergencyPhone'))
                if (hasOther && (!val || !val.trim())) {
                  return Promise.reject(new Error('Chọn quan hệ'))
                }
                return Promise.resolve()
              },
            }),
          ]}
          style={{ flex: '1 1 0', marginRight: 8, marginBottom: 0 }}
        >
          <Select
            showSearch
            allowClear
            placeholder="Quan hệ"
            options={relationshipOptions}
            filterOption={(input, option) =>
              (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
            }
          />
        </Form.Item>

        <Form.Item
          name="emergencyPhone"
          label="SĐT khẩn cấp"
          rules={[
            ({ getFieldValue }) => ({
              validator(_, val) {
                const hasOther = Boolean(getFieldValue('emergencyContact') || getFieldValue('emergencyRelationship'))
                const phone = val?.trim()
                if (hasOther && !phone) {
                  return Promise.reject(new Error('Nhập SĐT'))
                }
                if (phone && !VIETNAMESE_PHONE_REGEX.test(phone)) {
                  return Promise.reject(new Error('SĐT không hợp lệ'))
                }
                return Promise.resolve()
              },
            }),
          ]}
          style={{ flex: '1 1 0', marginBottom: 0 }}
        >
          <Input prefix={<PhoneOutlined style={{ color: '#94a3b8' }} />} placeholder="09xxxxxxxx" allowClear />
        </Form.Item>
      </Space.Compact>
    </div>
  )
}
