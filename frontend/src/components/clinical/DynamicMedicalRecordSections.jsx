import React, { useMemo, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Row,
  Segmented,
  Space,
  Tabs,
  Tag,
} from 'antd'
import {
  MedicineBoxOutlined,
  AppstoreOutlined,
  FolderOutlined,
  BarsOutlined,
  DownOutlined,
  UpOutlined,
} from '@ant-design/icons'
import {
  DEFAULT_TEMPLATE_SECTIONS,
  FIELD_CODE_TO_FORM_NAME,
  formatSectionLabel,
  formatTemplateName,
  getFieldMeta,
} from '../../constants/medicalRecordTemplateConstants'

export { FIELD_CODE_TO_FORM_NAME }

function DynamicMedicalRecordSections({
  sections = [],
  template = null,
  disabled = false,
}) {
  const [viewMode, setViewMode] = useState(() => {
    try {
      return localStorage.getItem('medical_record_sections_view_mode') || 'grid'
    } catch {
      return 'grid'
    }
  })
  const [collapsed, setCollapsed] = useState(false)

  const handleViewModeChange = (mode) => {
    setViewMode(mode)
    try {
      localStorage.setItem('medical_record_sections_view_mode', mode)
    } catch {}
  }

  const effectiveSections = sections && sections.length > 0 ? sections : DEFAULT_TEMPLATE_SECTIONS
  const sortedSections = useMemo(() => {
    return [...effectiveSections].sort((a, b) => a.displayOrder - b.displayOrder)
  }, [effectiveSections])

  // Phân nhóm theo nghiệp vụ lâm sàng cho chế độ Tab
  const tabGroups = useMemo(() => {
    const group1Codes = new Set(['CHIEF_COMPLAINT', 'SYMPTOMS', 'MEDICAL_HISTORY', 'PHYSICAL_EXAMINATION'])
    const group2Codes = new Set(['CLINICAL_PROGRESS', 'TREATMENT_PLAN'])
    const group3Codes = new Set(['DOCTOR_INSTRUCTIONS', 'CONCLUSION'])

    const g1 = []
    const g2 = []
    const g3 = []

    sortedSections.forEach((s, idx) => {
      if (group1Codes.has(s.fieldCode)) {
        g1.push({ section: s, idx })
      } else if (group2Codes.has(s.fieldCode)) {
        g2.push({ section: s, idx })
      } else if (group3Codes.has(s.fieldCode)) {
        g3.push({ section: s, idx })
      } else {
        // Trường tùy biến khác trong template
        if (idx < 4) g1.push({ section: s, idx })
        else if (idx < 6) g2.push({ section: s, idx })
        else g3.push({ section: s, idx })
      }
    })

    return [
      { key: 'intake', label: `1. Khám & Tiền sử (${g1.length})`, items: g1 },
      { key: 'progress', label: `2. Diễn biến & Điều trị (${g2.length})`, items: g2 },
      { key: 'conclusion', label: `3. Dặn dò & Kết luận (${g3.length})`, items: g3 },
    ].filter((t) => t.items.length > 0)
  }, [sortedSections])

  const renderSectionField = (section, idx) => {
    const formFieldName = FIELD_CODE_TO_FORM_NAME[section.fieldCode] || section.fieldCode
    const meta = getFieldMeta(section.fieldCode)
    const isRequired = Boolean(section.required)
    const sectionLabel = formatSectionLabel(section.label, section.fieldCode)

    return (
      <Form.Item
        key={section.fieldCode || idx}
        name={formFieldName}
        required={false}
        label={
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                minWidth: 20,
                height: 20,
                borderRadius: 4,
                background: '#eff6ff',
                color: '#1d4ed8',
                fontSize: 11,
                fontWeight: 700,
                border: '1px solid #bfdbfe',
                padding: '0 4px',
              }}
            >
              {idx + 1}
            </span>
            <span style={{ fontWeight: 600, color: '#1e293b', fontSize: 13 }}>
              {sectionLabel}
            </span>
            {isRequired && <span style={{ color: '#ef4444', fontWeight: 'bold' }}>*</span>}
          </div>
        }
        rules={[
          {
            validator: (_, val) => {
              if (section.fieldCode === 'CHIEF_COMPLAINT' && isRequired && (!val || !val.trim())) {
                return Promise.reject(new Error(`Vui lòng không để trống trường ${sectionLabel}`))
              }
              return Promise.resolve()
            },
          },
        ]}
        style={{ marginBottom: 10 }}
      >
        <Input.TextArea
          autoSize={{ minRows: 2, maxRows: 6 }}
          placeholder={meta.placeholder}
          disabled={disabled}
          style={{
            borderRadius: 6,
            fontSize: 13,
            lineHeight: 1.5,
            borderColor: '#cbd5e1',
          }}
        />
      </Form.Item>
    )
  }

  return (
    <Card
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            <span style={{ color: '#1e3a8a', display: 'inline-flex', alignItems: 'center', gap: 6, fontWeight: 700 }}>
              <MedicineBoxOutlined style={{ color: '#2563eb', fontSize: 16 }} />
              <span>Khám lâm sàng & Diễn biến bệnh</span>
            </span>
            {template && (
              <Space size={4} wrap>
                <Tag color="blue" style={{ fontWeight: 600, fontSize: 11, margin: 0 }}>
                  Mẫu: {formatTemplateName(template.name)}
                </Tag>
                <Tag color="purple" style={{ fontSize: 11, margin: 0 }}>
                  v{template.versionNo || template.currentVersionNo || 1}
                </Tag>
                {template.defaultTemplate && <Tag color="green" style={{ fontSize: 11, margin: 0 }}>Mặc định</Tag>}
                {template.fallback && <Tag color="orange" style={{ fontSize: 11, margin: 0 }}>Đa khoa (Fallback)</Tag>}
              </Space>
            )}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Segmented
              size="small"
              value={viewMode}
              onChange={handleViewModeChange}
              options={[
                { value: 'grid', label: 'Lưới 2 cột', icon: <AppstoreOutlined /> },
                { value: 'tabs', label: 'Nhóm Tab', icon: <FolderOutlined /> },
                { value: 'single', label: '1 cột', icon: <BarsOutlined /> },
              ]}
            />
            <Button
              type="text"
              size="small"
              icon={collapsed ? <DownOutlined /> : <UpOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              title={collapsed ? 'Mở rộng bảng khám' : 'Thu gọn bảng khám'}
            />
          </div>
        </div>
      }
      bordered
      style={{ marginBottom: 16 }}
      bodyStyle={{
        padding: collapsed ? 0 : '12px 16px',
        display: collapsed ? 'none' : 'block',
      }}
    >
      {viewMode === 'tabs' ? (
        <Tabs
          size="small"
          destroyInactiveTabPane={false}
          items={tabGroups.map((group) => ({
            key: group.key,
            label: group.label,
            children: (
              <Row gutter={[12, 10]} style={{ marginTop: 8 }}>
                {group.items.map(({ section, idx }) => (
                  <Col xs={24} sm={12} key={section.fieldCode || idx}>
                    {renderSectionField(section, idx)}
                  </Col>
                ))}
              </Row>
            ),
          }))}
        />
      ) : (
        <Row gutter={[12, 10]}>
          {sortedSections.map((section, idx) => (
            <Col
              xs={24}
              md={viewMode === 'grid' ? 12 : 24}
              key={section.fieldCode || idx}
            >
              {renderSectionField(section, idx)}
            </Col>
          ))}
        </Row>
      )}
    </Card>
  )
}

export default DynamicMedicalRecordSections
