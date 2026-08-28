import React, { useState } from 'react'
import { Form, Checkbox, Typography, Button, Space, Tag } from 'antd'
import {
  SafetyCertificateOutlined,
  ReadOutlined,
  CheckCircleFilled,
  InfoCircleOutlined,
} from '@ant-design/icons'
import {
  CONSENT_SUMMARY_POINTS,
  DEFAULT_CONSENT_VERSION,
} from '../../constants/patientConsentConstants'
import PersonalDataConsentModal from './PersonalDataConsentModal'

const { Text, Paragraph } = Typography

export default function PersonalDataConsentField({
  fieldName = 'consentAgreed',
  patientName = '',
  required = true,
  defaultChecked = true,
}) {
  const [modalOpen, setModalOpen] = useState(false)

  return (
    <div
      style={{
        background: '#f0fdf4',
        border: '1px solid #86efac',
        borderRadius: 8,
        padding: '16px 20px',
        marginTop: 8,
        marginBottom: 16,
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 8,
          marginBottom: 12,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SafetyCertificateOutlined style={{ fontSize: 20, color: '#16a34a' }} />
          <Text strong style={{ fontSize: 14, color: '#166534' }}>
            Phiếu đồng ý xử lý dữ liệu cá nhân (Nghị định 13/2023/NĐ-CP)
          </Text>
          <Tag color="green" style={{ fontWeight: 600 }}>
            {DEFAULT_CONSENT_VERSION}
          </Tag>
        </div>

        <Button
          type="link"
          size="small"
          icon={<ReadOutlined />}
          onClick={() => setModalOpen(true)}
          style={{ padding: 0, fontWeight: 600, color: '#15803d' }}
        >
          Đọc toàn văn phiếu đồng ý
        </Button>
      </div>

      <div
        style={{
          background: '#ffffff',
          border: '1px solid #dcfce7',
          borderRadius: 6,
          padding: '10px 14px',
          marginBottom: 14,
        }}
      >
        {CONSENT_SUMMARY_POINTS.map((point) => (
          <div
            key={point.key}
            style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: 8,
              marginBottom: 6,
              fontSize: 12,
              lineHeight: 1.5,
            }}
          >
            <CheckCircleFilled style={{ color: '#22c55e', fontSize: 13, marginTop: 2 }} />
            <div>
              <Text strong style={{ color: '#1f2937' }}>
                {point.title}:{' '}
              </Text>
              <Text style={{ color: '#4b5563' }}>{point.description}</Text>
            </div>
          </div>
        ))}
      </div>

      <Form.Item
        name={fieldName}
        valuePropName="checked"
        initialValue={defaultChecked}
        rules={[
          {
            validator: (_, value) =>
              !required || value === true
                ? Promise.resolve()
                : Promise.reject(
                    new Error(
                      'Phải ghi nhận sự đồng ý của người bệnh trước khi lập hồ sơ mới (QTN-24).',
                    ),
                  ),
          },
        ]}
        style={{ marginBottom: 0 }}
      >
        <Checkbox style={{ fontSize: 13, fontWeight: 600, color: '#14532d', alignItems: 'flex-start' }}>
          <span>
            Tôi xác nhận đã đọc và giải thích đầy đủ nội dung Phiếu đồng ý xử lý dữ liệu cá nhân cho người bệnh (hoặc người giám hộ hợp pháp), và người bệnh đã đồng ý cho phép phòng khám xử lý dữ liệu cá nhân theo quy định. <span style={{ color: '#dc2626' }}>*</span>
          </span>
        </Checkbox>
      </Form.Item>

      <PersonalDataConsentModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        patientName={patientName}
        version={DEFAULT_CONSENT_VERSION}
      />
    </div>
  )
}
