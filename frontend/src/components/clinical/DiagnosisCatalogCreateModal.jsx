import React, { useState } from 'react'
import { Form, Input, Modal, Select, message } from 'antd'
import { InfoCircleOutlined, PlusCircleOutlined } from '@ant-design/icons'
import diagnosisCatalogApi from '../../api/diagnosisCatalogApi'
import { getApiErrorMessage } from '../../utils/apiError'
import { icd10Categories } from '../../utils/icd10Data'

const { TextArea } = Input

const diseaseGroupOptions = icd10Categories
  .filter((c) => c.key !== 'ALL')
  .map((c) => ({
    value: c.label.replace(/\s*\([^)]*\)/g, '').trim(),
    label: c.label,
  }))

function DiagnosisCatalogCreateModal({ open, onCancel, onSuccess }) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)

  const handleFinish = async (values) => {
    setLoading(true)
    try {
      const payload = {
        code: String(values.code || '').trim().toUpperCase(),
        name: String(values.name || '').trim(),
        diseaseGroup: String(values.diseaseGroup || '').trim(),
        description: values.description ? String(values.description).trim() : null,
      }

      await diagnosisCatalogApi.create(payload)
      message.success(`Đã thêm mã bệnh [${payload.code}] ${payload.name} thành công!`)
      form.resetFields()
      if (typeof onSuccess === 'function') {
        onSuccess()
      }
    } catch (err) {
      const apiCode = err?.apiError?.code || err?.response?.data?.code
      if (apiCode === 'DIAGNOSIS_CATALOG_CODE_EXISTS' || apiCode === 'DUPLICATE_CODE') {
        message.error(`Mã bệnh [${form.getFieldValue('code')}] đã tồn tại trong danh mục hệ thống.`)
      } else {
        message.error(getApiErrorMessage(err, 'Không thể tạo mã bệnh mới. Vui lòng kiểm tra lại dữ liệu.'))
      }
    } finally {
      setLoading(false)
    }
  }

  const handleModalCancel = () => {
    if (!loading) {
      form.resetFields()
      if (typeof onCancel === 'function') {
        onCancel()
      }
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <PlusCircleOutlined style={{ color: '#2563eb' }} />
          <span>Thêm mã bệnh mới vào danh mục (ICD-10)</span>
        </div>
      }
      open={open}
      onCancel={handleModalCancel}
      onOk={() => form.submit()}
      okText="Lưu mã bệnh"
      cancelText="Hủy"
      confirmLoading={loading}
      destroyOnClose
      width={560}
      maskClosable={!loading}
    >
      <div className="diagnosis-form-hint info">
        <InfoCircleOutlined style={{ marginTop: 2 }} />
        <span>Mã bệnh mới sẽ được chuẩn hóa theo phân loại quốc tế và kích hoạt ngay để bác sĩ tra cứu khi khám bệnh.</span>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        initialValues={{
          diseaseGroup: 'Hô hấp',
        }}
      >
        <Form.Item
          name="code"
          label="Mã bệnh ICD (Mã số)"
          rules={[
            { required: true, message: 'Vui lòng nhập mã bệnh ICD (Ví dụ: J00, E11.9, I10).' },
            { max: 30, message: 'Mã bệnh không vượt quá 30 ký tự.' },
            {
              pattern: /^[A-Za-z0-9.]+$/,
              message: 'Mã bệnh chỉ bao gồm chữ cái, chữ số và dấu chấm (ví dụ: J06.9, K29.7).',
            },
          ]}
        >
          <Input
            placeholder="Ví dụ: J00, J06.9, E11.9, I10"
            maxLength={30}
            style={{ textTransform: 'uppercase', fontWeight: 600 }}
            onChange={(e) => {
              const val = e.target.value.toUpperCase()
              form.setFieldsValue({ code: val })
            }}
          />
        </Form.Item>

        <Form.Item
          name="name"
          label="Tên bệnh chuẩn hóa"
          rules={[
            { required: true, message: 'Vui lòng nhập tên bệnh chuẩn hóa.' },
            { max: 150, message: 'Tên bệnh không vượt quá 150 ký tự.' },
          ]}
        >
          <Input placeholder="Ví dụ: Cảm lạnh thông thường, Viêm mũi họng cấp tính" maxLength={150} />
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
            placeholder="Nhập mô tả triệu chứng đặc trưng, hướng dẫn hoặc ghi chú lâm sàng nếu có..."
            maxLength={500}
            showCount
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default DiagnosisCatalogCreateModal
