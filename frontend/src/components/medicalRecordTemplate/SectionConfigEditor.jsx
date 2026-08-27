import React, { useState } from 'react'
import {
  Button,
  Checkbox,
  Empty,
  Input,
  Select,
  Tooltip,
  Typography,
} from 'antd'
import {
  DeleteOutlined,
  HolderOutlined,
  InfoCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons'
import {
  MEDICAL_RECORD_FIELD_CODES,
  getFieldMeta,
} from '../../constants/medicalRecordTemplateConstants'

const { Text } = Typography

function SectionConfigEditor({ sections = [], onChange }) {
  const [draggedIndex, setDraggedIndex] = useState(null)
  const [dragOverIndex, setDragOverIndex] = useState(null)

  const usedFieldCodes = new Set(sections.map((s) => s.fieldCode))
  const unusedFieldOptions = MEDICAL_RECORD_FIELD_CODES.filter(
    (item) => !usedFieldCodes.has(item.code)
  )

  const handleAddSection = (fieldCodeToAdd) => {
    const code = fieldCodeToAdd || unusedFieldOptions[0]?.code
    if (!code) return

    const meta = getFieldMeta(code)
    const newSection = {
      fieldCode: code,
      label: meta.defaultLabel,
      required: meta.defaultRequired,
      displayOrder: sections.length + 1,
    }

    const updated = [...sections, newSection].map((item, idx) => ({
      ...item,
      displayOrder: idx + 1,
    }))
    onChange(updated)
  }

  const handleRemoveSection = (index) => {
    const updated = sections
      .filter((_, idx) => idx !== index)
      .map((item, idx) => ({
        ...item,
        displayOrder: idx + 1,
      }))
    onChange(updated)
  }

  const handleFieldCodeChange = (index, newCode) => {
    const meta = getFieldMeta(newCode)
    const updated = [...sections]
    const prevMeta = getFieldMeta(updated[index].fieldCode)
    const shouldUpdateLabel = !updated[index].label || updated[index].label === prevMeta.defaultLabel

    updated[index] = {
      ...updated[index],
      fieldCode: newCode,
      label: shouldUpdateLabel ? meta.defaultLabel : updated[index].label,
      required: updated[index].required !== undefined ? updated[index].required : meta.defaultRequired,
    }
    onChange(updated)
  }

  const handleLabelChange = (index, newLabel) => {
    const updated = [...sections]
    updated[index] = {
      ...updated[index],
      label: newLabel,
    }
    onChange(updated)
  }

  const handleRequiredChange = (index, checked) => {
    const updated = [...sections]
    updated[index] = {
      ...updated[index],
      required: checked,
    }
    onChange(updated)
  }

  const handleDragStart = (e, index) => {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(index))
    setDraggedIndex(index)
  }

  const handleDragOver = (e, index) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    if (dragOverIndex !== index) {
      setDragOverIndex(index)
    }
  }

  const handleDrop = (e, targetIndex) => {
    e.preventDefault()
    setDragOverIndex(null)
    if (draggedIndex === null || draggedIndex === targetIndex) {
      setDraggedIndex(null)
      return
    }

    const updated = [...sections]
    const [movedItem] = updated.splice(draggedIndex, 1)
    updated.splice(targetIndex, 0, movedItem)

    const reordered = updated.map((item, idx) => ({
      ...item,
      displayOrder: idx + 1,
    }))

    setDraggedIndex(null)
    onChange(reordered)
  }

  const handleDragEnd = () => {
    setDraggedIndex(null)
    setDragOverIndex(null)
  }

  return (
    <div className="section-config-editor-compact">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <Text strong style={{ fontSize: 13, color: '#334155' }}>
          Cấu hình các trường thông tin trong mẫu ({sections.length}/8)
        </Text>
        {unusedFieldOptions.length > 0 && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            size="small"
            onClick={() => handleAddSection()}
          >
            Thêm trường
          </Button>
        )}
      </div>

      {sections.length === 0 ? (
        <div style={{ padding: '24px 0', textAlign: 'center', background: '#f8fafc', border: '1px dashed #cbd5e1', borderRadius: 6 }}>
          <Empty
            description="Chưa có trường nào trong mẫu. Vui lòng thêm ít nhất 1 trường."
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => handleAddSection()}
            >
              Thêm trường đầu tiên
            </Button>
          </Empty>
        </div>
      ) : (
        <div
          style={{
            border: '1px solid #e2e8f0',
            borderRadius: 6,
            overflowX: 'auto',
            maxHeight: 340,
            overflowY: 'auto',
            background: '#ffffff',
          }}
        >
          <table
            style={{
              width: '100%',
              minWidth: 620,
              borderCollapse: 'collapse',
              fontSize: 13,
            }}
          >
            <thead>
              <tr style={{ background: '#f1f5f9', borderBottom: '1px solid #cbd5e1', color: '#475569', textAlign: 'left' }}>
                <th style={{ width: 36, padding: '8px 4px', textAlign: 'center' }}>
                  <Tooltip title="Kéo để đổi thứ tự">
                    <HolderOutlined style={{ color: '#94a3b8' }} />
                  </Tooltip>
                </th>
                <th style={{ width: 34, padding: '8px 4px', textAlign: 'center' }}>#</th>
                <th style={{ width: 220, padding: '8px 8px' }}>Mã trường chuẩn</th>
                <th style={{ padding: '8px 8px' }}>Tiêu đề hiển thị (Nhãn)</th>
                <th style={{ width: 90, padding: '8px 4px', textAlign: 'center' }}>Bắt buộc</th>
                <th style={{ width: 44, padding: '8px 4px', textAlign: 'center' }}>Xóa</th>
              </tr>
            </thead>
            <tbody>
              {sections.map((section, index) => {
                const meta = getFieldMeta(section.fieldCode)
                const selectOptions = MEDICAL_RECORD_FIELD_CODES.filter(
                  (item) => item.code === section.fieldCode || !usedFieldCodes.has(item.code)
                ).map((item) => ({
                  value: item.code,
                  label: `${item.defaultLabel} (${item.code})`,
                }))

                const isDragOver = dragOverIndex === index
                const isDragging = draggedIndex === index

                return (
                  <tr
                    key={section.fieldCode || index}
                    draggable
                    onDragStart={(e) => handleDragStart(e, index)}
                    onDragOver={(e) => handleDragOver(e, index)}
                    onDrop={(e) => handleDrop(e, index)}
                    onDragEnd={handleDragEnd}
                    style={{
                      borderBottom: index === sections.length - 1 ? 'none' : '1px solid #f1f5f9',
                      background: isDragOver
                        ? '#e0f2fe'
                        : isDragging
                          ? '#f8fafc'
                          : index % 2 === 0
                            ? '#ffffff'
                            : '#fafafa',
                      opacity: isDragging ? 0.5 : 1,
                      transition: 'background 0.15s ease',
                    }}
                  >
                    <td
                      style={{
                        padding: '6px 4px',
                        textAlign: 'center',
                        cursor: 'grab',
                        color: '#94a3b8',
                      }}
                      title="Kéo thả để đổi vị trí"
                    >
                      <HolderOutlined style={{ fontSize: 14 }} />
                    </td>

                    <td style={{ padding: '6px 4px', textAlign: 'center', fontWeight: 600, color: '#64748b' }}>
                      {index + 1}
                    </td>

                    <td style={{ padding: '6px 8px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Select
                          size="small"
                          style={{ width: '100%' }}
                          value={section.fieldCode}
                          options={selectOptions}
                          onChange={(val) => handleFieldCodeChange(index, val)}
                        />
                        {meta.description && (
                          <Tooltip title={`${meta.defaultLabel}: ${meta.description}`}>
                            <InfoCircleOutlined style={{ color: '#94a3b8', cursor: 'pointer' }} />
                          </Tooltip>
                        )}
                      </div>
                    </td>

                    <td style={{ padding: '6px 8px' }}>
                      <Input
                        size="small"
                        value={section.label}
                        placeholder={meta.placeholder || 'Nhập nhãn...'}
                        onChange={(e) => handleLabelChange(index, e.target.value)}
                        status={!section.label?.trim() ? 'error' : ''}
                      />
                    </td>

                    <td style={{ padding: '6px 4px', textAlign: 'center' }}>
                      <Checkbox
                        checked={Boolean(section.required)}
                        onChange={(e) => handleRequiredChange(index, e.target.checked)}
                      />
                    </td>

                    <td style={{ padding: '6px 4px', textAlign: 'center' }}>
                      <Tooltip title="Xóa trường khỏi mẫu">
                        <Button
                          type="text"
                          danger
                          size="small"
                          icon={<DeleteOutlined />}
                          disabled={sections.length <= 1}
                          onClick={() => handleRemoveSection(index)}
                          style={{ padding: '0 4px' }}
                        />
                      </Tooltip>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default SectionConfigEditor
