import React from 'react'
import { Alert, Button, Col, Form, Input, message, Radio, Row, Select, Space, Upload } from 'antd'
import { CloudUploadOutlined, InboxOutlined } from '@ant-design/icons'
import { CATEGORY_OPTIONS } from './attachmentConstants.jsx'

function AttachmentUploadForm({
  form,
  patients,
  patientIdFilter,
  fileList,
  setFileList,
  uploading,
  categoryOptions = CATEGORY_OPTIONS,
  onSubmit,
  onCancel,
}) {
  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '12px 0' }}>
      <Alert
        type="info"
        showIcon
        message="Lập trình tải lên tệp đính kèm & Lưu kết quả cận lâm sàng"
        description="Hỗ trợ các định dạng hình ảnh (PNG, JPG, WEBP), tài liệu văn bản (PDF). Dung lượng tối đa 15MB cho mỗi tệp."
        style={{ marginBottom: 20 }}
      />

      <Form
        form={form}
        layout="vertical"
        onFinish={onSubmit}
        initialValues={{
          patientId: patientIdFilter || undefined,
          category: categoryOptions[0]?.value || 'Công thức máu',
          status: 'NORMAL',
        }}
      >
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <Form.Item
              name="patientId"
              label="Bệnh nhân"
              rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}
            >
              <Select
                showSearch
                placeholder="Chọn bệnh nhân..."
                disabled={!!patientIdFilter}
                optionFilterProp="label"
                options={patients.map((p) => ({
                  value: p.id,
                  label: `${p.fullName} (${p.patientCode || p.id})`,
                }))}
              />
            </Form.Item>
          </Col>

          <Col xs={24} sm={12}>
            <Form.Item
              name="category"
              label="Loại kết quả cận lâm sàng"
              rules={[{ required: true, message: 'Vui lòng chọn loại kết quả' }]}
            >
              <Select options={categoryOptions} />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <Form.Item name="status" label="Đánh giá kết quả (Trạng thái)">
              <Radio.Group buttonStyle="solid">
                <Radio.Button value="NORMAL">✅ Bình thường</Radio.Button>
                <Radio.Button value="ABNORMAL">⚠️ Cần chú ý / Bất thường</Radio.Button>
              </Radio.Group>
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          name="resultSummary"
          label="Tóm tắt chỉ số & Kết quả xét nghiệm / Chẩn đoán hình ảnh"
          rules={[{ required: true, message: 'Vui lòng nhập tóm tắt kết quả' }]}
        >
          <Input.TextArea
            rows={3}
            placeholder="Ví dụ: RBC: 4.8 T/L, WBC: 7.2 G/L, PLT: 250 G/L... Hình ảnh tim phổi bình thường."
          />
        </Form.Item>

        <Form.Item name="note" label="Ghi chú thêm của Bác sĩ / Kỹ thuật viên (Nếu có)">
          <Input.TextArea rows={2} placeholder="Lời khuyên hoặc yêu cầu làm lại xét nghiệm..." />
        </Form.Item>

        <Form.Item label="Chọn tệp đính kèm kết quả (PDF/JPG/PNG/WEBP, tối đa 15 MB)">
          <Upload.Dragger
            multiple
            beforeUpload={(file) => {
              const isLt15M = file.size / 1024 / 1024 < 15
              if (!isLt15M) {
                message.error('Kích thước tệp vượt quá 15MB!')
                return Upload.LIST_IGNORE
              }
              setFileList((curr) => [...curr, file])
              return false
            }}
            fileList={fileList}
            onRemove={(file) => setFileList((curr) => curr.filter((f) => f.uid !== file.uid))}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined style={{ color: '#2563eb' }} />
            </p>
            <p className="ant-upload-text">Kéo thả tệp vào đây hoặc nhấn để duyệt tệp từ máy tính</p>
            <p className="ant-upload-hint">Chấp nhận tệp ảnh X-quang, siêu âm, phiếu kết quả PDF...</p>
          </Upload.Dragger>
        </Form.Item>

        <Form.Item style={{ textAlign: 'right', marginTop: 24 }}>
          <Space>
            <Button onClick={onCancel}>Hủy bỏ</Button>
            <Button type="primary" htmlType="submit" loading={uploading} icon={<CloudUploadOutlined />}>
              Lưu và Tải lên tệp kết quả
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </div>
  )
}

export default AttachmentUploadForm
