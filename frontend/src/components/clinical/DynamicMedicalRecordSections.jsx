import React from 'react'
import {
  Card,
  Col,
  Form,
  Input,
  Row,
  Skeleton,
  Space,
  Tag,
} from 'antd'
import {
  MedicineBoxOutlined,
} from '@ant-design/icons'
import {
  DEFAULT_TEMPLATE_SECTIONS,
  FIELD_CODE_TO_FORM_NAME,
  getFieldMeta,
} from '../../constants/medicalRecordTemplateConstants'

export { FIELD_CODE_TO_FORM_NAME }

function DynamicMedicalRecordSections({
  sections = [],
  template = null,
  disabled = false,
  loading = false,
}) {
  if (loading) {
    return (
      <Card bordered style={{ marginBottom: 16 }}>
        <Skeleton active paragraph={{ rows: 6 }} />
      </Card>
    )
  }

  const effectiveSections = sections && sections.length > 0 ? sections : DEFAULT_TEMPLATE_SECTIONS
  const sortedSections = [...effectiveSections].sort((a, b) => a.displayOrder - b.displayOrder)

  return (
    <Card
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <span style={{ color: '#1e3a8a', display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            <MedicineBoxOutlined />
            <span>Khám lâm sàng & Diễn biến bệnh</span>
          </span>
          {template && (
            <Space size={6} wrap>
              <Tag color="blue" style={{ fontWeight: 600 }}>
                Mẫu: {template.name}
              </Tag>
              <Tag color="purple">
                v{template.versionNo || template.currentVersionNo || 1}
              </Tag>
              {template.defaultTemplate && <Tag color="green">Mặc định</Tag>}
              {template.fallback && <Tag color="orange">Đa khoa (Fallback)</Tag>}
            </Space>
          )}
        </div>
      }
      bordered
      style={{ marginBottom: 16 }}
    >
      <Row gutter={[16, 12]}>
        {sortedSections.map((section, idx) => {
          const formFieldName = FIELD_CODE_TO_FORM_NAME[section.fieldCode] || section.fieldCode
          const meta = getFieldMeta(section.fieldCode)
          const isRequired = Boolean(section.required)
          const rows = section.fieldCode === 'PHYSICAL_EXAMINATION' || section.fieldCode === 'MEDICAL_HISTORY' ? 3 : 2

          return (
            <Col xs={24} key={section.fieldCode || idx}>
              <Form.Item
                name={formFieldName}
                label={
                  <span style={{ fontWeight: 600, color: '#1e293b' }}>
                    {idx + 1}. {section.label || meta.defaultLabel}
                    {isRequired && <span style={{ color: '#ef4444', marginLeft: 4 }}>*</span>}
                  </span>
                }
                rules={[
                  {
                    required: isRequired,
                    message: `Vui lòng nhập ${section.label || meta.defaultLabel}`,
                  },
                  {
                    validator: (_, val) => {
                      if (isRequired && (!val || !val.trim())) {
                        return Promise.reject(new Error(`Vui lòng không để trống trường ${section.label || meta.defaultLabel}`))
                      }
                      return Promise.resolve()
                    },
                  },
                ]}
                style={{ marginBottom: 16 }}
              >
                <Input.TextArea
                  rows={rows}
                  placeholder={meta.placeholder}
                  disabled={disabled}
                  style={{ borderRadius: 6 }}
                />
              </Form.Item>
            </Col>
          )
        })}
      </Row>
    </Card>
  )
}

export default DynamicMedicalRecordSections
