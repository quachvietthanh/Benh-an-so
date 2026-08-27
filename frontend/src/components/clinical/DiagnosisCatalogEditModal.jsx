import React, { useEffect, useState } from 'react'
import { Alert, Form, Input, Modal, Select, Tag, message } from 'antd'
import { EditOutlined, LockOutlined } from '@ant-design/icons'
import diagnosisCatalogApi from '../../api/diagnosisCatalogApi'
import { getApiErrorMessage, normalizeApiError } from '../../utils/apiError'
import { icd10Categories } from '../../utils/icd10Data'

const { TextArea } = Input

const diseaseGroupOptions = icd10Categories
  .filter((c) => c.key !== 'ALL')
  .map((c) => ({
    value: c.label.replace(/\s*\([^)]*\)/g, '').trim(),
    label: c.label,
  }))

function DiagnosisCatalogEditModal({ open, item, onCancel, onSuccess }) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [formError, setFormError] = useState(null)

  useEffect(() => {
    if (open && item) {
      setFormError(null)
      form.setFieldsValue({
        code: item.code,
        name: item.name,
        diseaseGroup: item.diseaseGroup,
        description: item.description || '',
      })
    }
  }, [open, item, form])

  const handleFinish = async (values) => {
    if (!item?.id) return
    setLoading(true)
    setFormError(null)
    try {
      const payload = {
        name: String(values.name || '').trim(),
        diseaseGroup: String(values.diseaseGroup || '').trim(),
        description: values.description ? String(values.description).trim() : null,
      }

      await diagnosisCatalogApi.update(item.id, payload)
      message.success(`Đã cập nhật mã bệnh [${item.code}] thành công!`)
      setFormError(null)
      if (typeof onSuccess === 'function') {
        onSuccess()
      }
    } catch (err) {
      const normalized = normalizeApiError(err)
      let errorText = ''
      const fieldErrors = []

      if (normalized.fields && Object.keys(normalized.fields).length > 0) {
        Object.entries(normalized.fields).forEach(([fName, fMsg]) => {
          fieldErrors.push({ name: fName, errors: [fMsg] })
        })
        errorText = Object.values(normalized.fields)[0]
      } else {
        errorText = getApiErrorMessage(err, 'Không thể cập nhật thông tin mã bệnh. Vui lòng thử lại.')
      }

      setFormError(errorText)
      if (fieldErrors.length > 0) {
        form.setFields(fieldErrors)
      }
    } finally {
      setLoading(false)
    }
  }

  const handleModalCancel = () => {
    if (!loading) {
      form.resetFields()
      setFormError(null)
      if (typeof onCancel === 'function') {
        onCancel()
      }
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <EditOutlined style={{ color: '#2563eb' }} />
          <span>Chỉnh sửa thông tin mã bệnh</span>
        </div>
      }
      open={open}
      onCancel={handleModalCancel}
      onOk={() => form.submit()}
      okText="Lưu thay đổi"
      cancelText="Hủy"
      confirmLoading={loading}
      destroyOnClose
      width={560}
      maskClosable={!loading}
    >
      <div className="diagnosis-code-preview">
        <span style={{ fontSize: 13, color: '#64748b' }}>Mã định danh ICD:</span>
        <Tag color="blue" className="diagnosis-code-tag">
          {item?.code || '—'}
        </Tag>
        <span style={{ fontSize: 12, color: '#94a3b8', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
          <LockOutlined /> Mã bệnh cố định để bảo toàn dữ liệu bệnh án đã lưu
        </span>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        onValuesChange={() => {
          if (formError) setFormError(null)
        }}
      >
        {formError && (
          <Alert
            type="error"
            showIcon
            message={formError}
            closable
            onClose={() => setFormError(null)}
            style={{ marginBottom: 16 }}
          />
        )}

        <Form.Item
          name="name"
          label="Tên bệnh chuẩn hóa"
          rules={[
            { required: true, message: 'Vui lòng nhập tên bệnh chuẩn hóa.' },
            { max: 150, message: 'Tên bệnh không vượt quá 150 ký tự.' },
          ]}
        >
          <Input placeholder="Nhập tên bệnh chuẩn hóa" maxLength={150} />
        </Form.Item>

        <Form.Item
          name="diseaseGroup"
          label="Nhóm bệnh / Chuyên khoa"
          rules={[
            { required: true, message: 'Vui lòng chọn hoặc nhập nhóm bệnh.' },
            { max: 100, message: 'Nhóm bệnh không vượt quá 100 ký tự.' },
          ]}
        >
          <Select
            showSearch
            allowClear
            placeholder="Chọn hoặc tìm nhóm bệnh"
            options={diseaseGroupOptions}
            filterOption={(input, option) =>
              (option?.label ?? '').toLowerCase().includes(input.toLowerCase()) ||
              (option?.value ?? '').toLowerCase().includes(input.toLowerCase())
            }
          />
        </Form.Item>

        <Form.Item
          name="description"
          label="Mô tả / Hướng dẫn chẩn đoán (Tùy chọn)"
          rules={[{ max: 500, message: 'Mô tả không vượt quá 500 ký tự.' }]}
        >
          <TextArea
            rows={3}
            placeholder="Nhập mô tả triệu chứng đặc trưng, hướng dẫn hoặc ghi chú lâm sàng..."
            maxLength={500}
            showCount
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default DiagnosisCatalogEditModal
