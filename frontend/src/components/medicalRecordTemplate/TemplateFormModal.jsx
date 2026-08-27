import React, { useEffect, useState } from 'react'
import {
  Alert,
  Checkbox,
  Col,
  Divider,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Tooltip,
  message,
} from 'antd'
import {
  AppstoreAddOutlined,
  EditOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import SectionConfigEditor from './SectionConfigEditor'
import { DEFAULT_TEMPLATE_SECTIONS } from '../../constants/medicalRecordTemplateConstants'

function TemplateFormModal({
  open,
  onClose,
  onSubmit,
  initialData = null,
  specialties = [],
  loading = false,
}) {
  const [form] = Form.useForm()
  const [sections, setSections] = useState(DEFAULT_TEMPLATE_SECTIONS)
  const [serverError, setServerError] = useState('')

  const isEdit = Boolean(initialData?.id)

  useEffect(() => {
    if (open) {
      setServerError('')
      if (initialData) {
        form.setFieldsValue({
          specialtyId: initialData.specialty?.id || initialData.specialtyId,
          name: initialData.name || '',
          changeNote: '',
          makeDefault: Boolean(initialData.defaultTemplate || initialData.isDefault),
        })
        if (initialData.sections && initialData.sections.length > 0) {
          const sorted = [...initialData.sections].sort(
            (a, b) => a.displayOrder - b.displayOrder
          )
          setSections(sorted)
        } else {
          setSections(DEFAULT_TEMPLATE_SECTIONS)
        }
      } else {
        form.resetFields()
        const defaultSpecialty = specialties[0]?.id
        form.setFieldsValue({
          specialtyId: defaultSpecialty || undefined,
          makeDefault: false,
        })
        setSections(DEFAULT_TEMPLATE_SECTIONS)
      }
    }
  }, [open, initialData, specialties, form])

  const handleSubmit = async () => {
    setServerError('')
    try {
      const values = await form.validateFields()

      if (!sections || sections.length === 0) {
        message.error('Mẫu bệnh án phải có ít nhất 1 trường thông tin.')
        return
      }

      const emptyLabelIdx = sections.findIndex((s) => !s.label?.trim())
      if (emptyLabelIdx !== -1) {
        message.error(`Trường #${emptyLabelIdx + 1} chưa có tiêu đề hiển thị (nhãn). Vui lòng nhập tiêu đề.`)
        return
      }

      const codeSet = new Set()
      for (const s of sections) {
        if (codeSet.has(s.fieldCode)) {
          message.error(`Mã trường ${s.fieldCode} bị trùng lặp trong mẫu. Mỗi trường chỉ được thêm 1 lần.`)
          return
        }
        codeSet.add(s.fieldCode)
      }

      const payload = {
        name: values.name.trim(),
        sections: sections.map((s, idx) => ({
          fieldCode: s.fieldCode,
          label: s.label.trim(),
          required: Boolean(s.required),
          displayOrder: idx + 1,
        })),
      }

      if (isEdit) {
        payload.changeNote = values.changeNote?.trim() || ''
      } else {
        payload.specialtyId = values.specialtyId
        payload.makeDefault = Boolean(values.makeDefault)
      }

      await onSubmit(payload)
    } catch (err) {
      if (err.errorFields) {
        return
      }
      const msg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        'Có lỗi xảy ra khi lưu mẫu bệnh án.'
      setServerError(msg)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 15 }}>
          {isEdit ? (
            <>
              <EditOutlined style={{ color: '#2563eb' }} />
              <span>Chỉnh sửa Mẫu bệnh án (Phiên bản v{initialData?.currentVersionNo || 1})</span>
            </>
          ) : (
            <>
              <AppstoreAddOutlined style={{ color: '#059669' }} />
              <span>Thêm mới Mẫu bệnh án theo chuyên khoa</span>
            </>
          )}
        </div>
      }
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={loading}
      okText={isEdit ? 'Lưu & Tạo phiên bản mới' : 'Tạo mẫu bệnh án'}
      cancelText="Hủy bỏ"
      width={780}
      destroyOnClose
      maskClosable={false}
      bodyStyle={{ paddingTop: 12, paddingBottom: 8 }}
    >
      {serverError && (
        <Alert
          type="error"
          showIcon
          message="Không thể lưu mẫu bệnh án"
          description={serverError}
          style={{ marginBottom: 12 }}
          closable
          onClose={() => setServerError('')}
        />
      )}

      {isEdit && (
        <Alert
          type="info"
          showIcon
          icon={<InfoCircleOutlined />}
          message="Cơ chế bảo toàn phiên bản"
          description="Cập nhật sẽ tạo phiên bản mới (version). Dữ liệu bệnh án cũ đã khám trước đây vẫn giữ nguyên cấu trúc cũ."
          style={{ marginBottom: 12, padding: '6px 12px' }}
        />
      )}

      <Form form={form} layout="vertical" requiredMark="optional">
        <Row gutter={12}>
          <Col xs={24} sm={10}>
            <Form.Item
              name="specialtyId"
              label={<span style={{ fontWeight: 600, fontSize: 13 }}>Chuyên khoa áp dụng</span>}
              rules={[{ required: true, message: 'Vui lòng chọn chuyên khoa' }]}
              style={{ marginBottom: 10 }}
            >
              <Select
                placeholder="Chọn chuyên khoa..."
                disabled={isEdit}
                options={specialties.map((s) => ({
                  value: s.id,
                  label: `${s.name} (${s.code})`,
                }))}
              />
            </Form.Item>
          </Col>

          <Col xs={24} sm={14}>
            <Form.Item
              name="name"
              label={<span style={{ fontWeight: 600, fontSize: 13 }}>Tên mẫu bệnh án</span>}
              rules={[
                { required: true, message: 'Vui lòng nhập tên mẫu bệnh án' },
                { max: 150, message: 'Tên mẫu không vượt quá 150 ký tự' },
                {
                  validator: (_, val) => {
                    if (val && !val.trim()) {
                      return Promise.reject(new Error('Tên mẫu không được chỉ chứa khoảng trắng'))
                    }
                    return Promise.resolve()
                  },
                },
              ]}
              style={{ marginBottom: 10 }}
            >
              <Input placeholder="VD: Mẫu khám Nội tim mạch chuẩn, Mẫu Nhi khoa sơ bộ..." />
            </Form.Item>
          </Col>
        </Row>

        {!isEdit && (
          <Form.Item name="makeDefault" valuePropName="checked" style={{ marginBottom: 10 }}>
            <Checkbox>
              <span style={{ fontWeight: 500, fontSize: 13 }}>
                Đặt làm mẫu mặc định cho chuyên khoa này
              </span>
              <Tooltip title="Bác sĩ khi mở lượt khám thuộc chuyên khoa này sẽ tự động nạp mẫu này đầu tiên.">
                <InfoCircleOutlined style={{ color: '#64748b', marginLeft: 6 }} />
              </Tooltip>
            </Checkbox>
          </Form.Item>
        )}

        {isEdit && (
          <Form.Item
            name="changeNote"
            label={<span style={{ fontWeight: 600, fontSize: 13 }}>Ghi chú thay đổi (Lý do cập nhật)</span>}
            rules={[
              { required: true, message: 'Vui lòng nhập ghi chú thay đổi cho phiên bản mới' },
              { max: 500, message: 'Ghi chú không vượt quá 500 ký tự' },
            ]}
            style={{ marginBottom: 10 }}
          >
            <Input placeholder="VD: Bổ sung trường dặn dò bác sĩ, đổi tiêu đề phần triệu chứng lâm sàng..." />
          </Form.Item>
        )}

        <Divider style={{ margin: '8px 0 12px 0' }} />

        <SectionConfigEditor sections={sections} onChange={setSections} />
      </Form>
    </Modal>
  )
}

export default TemplateFormModal
