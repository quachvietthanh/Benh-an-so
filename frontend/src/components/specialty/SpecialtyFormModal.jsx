import React, { useEffect, useState } from 'react'
import { Alert, Form, Input, Modal, Select, message } from 'antd'
import { ApartmentOutlined, MedicineBoxOutlined, UserOutlined } from '@ant-design/icons'
import specialtyApi from '../../api/specialtyApi.js'
import userApi from '../../api/userApi.js'
import systemApi from '../../api/systemApi.js'
import { getApiErrorMessage } from '../../utils/apiError.js'

const { TextArea } = Input

export default function SpecialtyFormModal({ open, onCancel, onSuccess, editingSpecialty }) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [loadingResources, setLoadingResources] = useState(false)
  const [doctors, setDoctors] = useState([])
  const [rooms, setRooms] = useState([])
  const [apiError, setApiError] = useState(null)

  const isEdit = Boolean(editingSpecialty)

  useEffect(() => {
    if (!open) {
      form.resetFields()
      setApiError(null)
      return
    }

    const loadOptionsAndDetail = async () => {
      setLoadingResources(true)
      try {
        const [docsRes, roomsRes] = await Promise.all([
          userApi.getDoctors().catch((err) => {
            console.error('Lỗi nạp danh sách bác sĩ:', err)
            return { data: [] }
          }),
          systemApi.getRooms({ active: true, page: 0, size: 100 }).catch((err) => {
            console.error('Lỗi nạp danh sách phòng khám:', err)
            return { data: { content: [] } }
          }),
        ])

        const doctorList = Array.isArray(docsRes.data) ? docsRes.data : []
        const roomList = Array.isArray(roomsRes.data?.content)
          ? roomsRes.data.content
          : Array.isArray(roomsRes.data)
            ? roomsRes.data
            : []

        setDoctors(doctorList)
        setRooms(roomList)

        if (editingSpecialty) {
          // Fetch full detail if editing
          try {
            const detailRes = await specialtyApi.getById(editingSpecialty.id)
            const detail = detailRes.data || editingSpecialty
            form.setFieldsValue({
              code: detail.code,
              name: detail.name,
              description: detail.description || '',
              doctorIds: (detail.doctors || []).map((d) => d.id),
              roomIds: (detail.rooms || []).map((r) => r.id),
            })
          } catch {
            form.setFieldsValue({
              code: editingSpecialty.code,
              name: editingSpecialty.name,
              description: editingSpecialty.description || '',
              doctorIds: (editingSpecialty.doctors || []).map((d) => d.id),
              roomIds: (editingSpecialty.rooms || []).map((r) => r.id),
            })
          }
        } else {
          form.setFieldsValue({
            code: '',
            name: '',
            description: '',
            doctorIds: [],
            roomIds: [],
          })
        }
      } catch (err) {
        console.error('Lỗi nạp danh sách bác sĩ/phòng khám:', err)
      } finally {
        setLoadingResources(false)
      }
    }

    loadOptionsAndDetail()
  }, [open, editingSpecialty, form])

  const handleFinish = async (values) => {
    setSubmitting(true)
    setApiError(null)

    try {
      if (isEdit) {
        const payload = {
          name: values.name.trim(),
          description: values.description ? values.description.trim() : '',
          doctorIds: values.doctorIds || [],
          roomIds: values.roomIds || [],
        }
        await specialtyApi.update(editingSpecialty.id, payload)
        message.success('Cập nhật chuyên khoa thành công!')
      } else {
        const payload = {
          code: values.code.trim().toUpperCase(),
          name: values.name.trim(),
          description: values.description ? values.description.trim() : '',
          doctorIds: values.doctorIds || [],
          roomIds: values.roomIds || [],
        }
        await specialtyApi.create(payload)
        message.success('Tạo chuyên khoa mới thành công!')
      }

      onSuccess()
    } catch (err) {
      const code = err?.response?.data?.code || err?.apiError?.code
      const msg = getApiErrorMessage(err, isEdit ? 'Không thể cập nhật chuyên khoa' : 'Không thể tạo chuyên khoa')

      if (code === 'SPECIALTY_NAME_ALREADY_EXISTS') {
        form.setFields([
          {
            name: 'name',
            errors: ['Tên chuyên khoa đã tồn tại trong hệ thống. Vui lòng chọn tên khác.'],
          },
        ])
      } else if (code === 'SPECIALTY_CODE_ALREADY_EXISTS') {
        form.setFields([
          {
            name: 'code',
            errors: ['Mã chuyên khoa đã tồn tại trong hệ thống. Vui lòng chọn mã khác.'],
          },
        ])
      }

      setApiError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <MedicineBoxOutlined style={{ color: '#059669', fontSize: 22 }} />
          <span style={{ fontSize: 18, fontWeight: 600 }}>
            {isEdit ? 'Cập nhật thông tin chuyên khoa' : 'Khai báo chuyên khoa mới'}
          </span>
        </div>
      }
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText={isEdit ? 'Lưu thay đổi' : 'Tạo chuyên khoa'}
      cancelText="Hủy bỏ"
      okButtonProps={{
        size: 'large',
        style: {
          minWidth: 130,
          height: 42,
          borderRadius: 8,
          fontWeight: 600,
          fontSize: 15,
          background: '#059669',
          borderColor: '#059669',
        },
      }}
      cancelButtonProps={{
        size: 'large',
        style: { minWidth: 100, height: 42, borderRadius: 8, fontSize: 15 },
      }}
      width={640}
      destroyOnClose
    >
      {apiError && (
        <Alert
          type="error"
          showIcon
          message="Không thể lưu chuyên khoa"
          description={apiError}
          style={{ marginBottom: 16 }}
        />
      )}

      <Form
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        initialValues={{ doctorIds: [], roomIds: [] }}
      >
        <Form.Item
          name="code"
          label={<span style={{ fontWeight: 600 }}>Mã chuyên khoa</span>}
          tooltip={isEdit ? 'Mã chuyên khoa là định danh cố định không thể thay đổi.' : 'Mã viết hoa không dấu, ví dụ: KHOA_NHI, NOIKHOA, TIM_MACH'}
          rules={[
            { required: !isEdit, message: 'Vui lòng nhập mã chuyên khoa' },
            {
              pattern: /^[A-Za-z0-9_]+$/,
              message: 'Mã chuyên khoa chỉ được chứa chữ cái, số và dấu gạch dưới (_)',
            },
            { max: 30, message: 'Mã chuyên khoa không được vượt quá 30 ký tự' },
          ]}
        >
          <Input
            size="large"
            placeholder="Ví dụ: KHOA_NHI, NOIKHOA"
            disabled={isEdit}
            style={{ textTransform: 'uppercase', borderRadius: 8 }}
            onChange={(e) => {
              if (!isEdit) {
                form.setFieldValue('code', e.target.value.toUpperCase())
              }
            }}
          />
        </Form.Item>

        <Form.Item
          name="name"
          label={<span style={{ fontWeight: 600 }}>Tên chuyên khoa</span>}
          rules={[
            { required: true, message: 'Vui lòng nhập tên chuyên khoa' },
            { max: 100, message: 'Tên chuyên khoa không được vượt quá 100 ký tự' },
          ]}
        >
          <Input size="large" placeholder="Ví dụ: Khoa Nhi, Khoa Nội Tổng Hợp" style={{ borderRadius: 8 }} />
        </Form.Item>

        <Form.Item
          name="description"
          label={<span style={{ fontWeight: 600 }}>Mô tả chuyên khoa</span>}
          rules={[{ max: 500, message: 'Mô tả không được vượt quá 500 ký tự' }]}
        >
          <TextArea rows={3} placeholder="Mô tả chức năng, phạm vi chuyên môn..." showCount maxLength={500} style={{ borderRadius: 8 }} />
        </Form.Item>

        <Form.Item
          name="doctorIds"
          label={
            <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}>
              <UserOutlined />
              <span>Bác sĩ phụ trách chuyên khoa</span>
            </span>
          }
          tooltip="Gán các bác sĩ có chứng chỉ hoặc phạm vi hành nghề tương ứng với chuyên khoa"
        >
          <Select
            size="large"
            mode="multiple"
            placeholder="Chọn bác sĩ phụ trách..."
            allowClear
            loading={loadingResources}
            optionFilterProp="label"
            style={{ borderRadius: 8 }}
            options={doctors.map((doc) => ({
              value: doc.id,
              label: `${doc.fullName || doc.username} (${doc.username})`,
            }))}
          />
        </Form.Item>

        <Form.Item
          name="roomIds"
          label={
            <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}>
              <ApartmentOutlined />
              <span>Phòng khám bệnh trực thuộc</span>
            </span>
          }
          tooltip="Gán các phòng khám bệnh phục vụ khám và tiếp đón của chuyên khoa này"
        >
          <Select
            size="large"
            mode="multiple"
            placeholder="Chọn phòng khám bệnh..."
            allowClear
            loading={loadingResources}
            optionFilterProp="label"
            style={{ borderRadius: 8 }}
            options={rooms.map((room) => ({
              value: room.id,
              label: `${room.name} [${room.code}]`,
            }))}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
