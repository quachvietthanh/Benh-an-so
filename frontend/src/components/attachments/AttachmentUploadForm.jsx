import React, { useMemo } from 'react'
import { Alert, Button, Col, Form, message, Row, Select, Space, Typography, Upload } from 'antd'
import { CloudUploadOutlined, InboxOutlined } from '@ant-design/icons'

const { Text } = Typography

const ALLOWED_CONTENT_TYPES = ['application/pdf', 'image/jpeg', 'image/png']
const MAX_FILE_SIZE = 10 * 1024 * 1024

const shortId = (value) => String(value || '').slice(0, 8).toUpperCase()

function AttachmentUploadForm({
  form,
  patients,
  clinicalResults,
  patientIdFilter,
  fileList,
  setFileList,
  uploading,
  onSubmit,
  onCancel,
}) {
  const selectedPatientId = Form.useWatch('patientId', form)
  const selectedResultId = Form.useWatch('resultId', form)

  const availableResults = useMemo(() => clinicalResults.filter((result) =>
    selectedPatientId && String(result.patientId) === String(selectedPatientId),
  ), [clinicalResults, selectedPatientId])

  const selectedResult = useMemo(() => availableResults.find((result) =>
    String(result.id) === String(selectedResultId),
  ), [availableResults, selectedResultId])

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '12px 0' }}>
      <Alert
        type="info"
        showIcon
        message="Tệp chỉ được gắn vào một kết quả cận lâm sàng đã tồn tại"
        description="Chọn đúng bệnh nhân và kết quả. Máy chủ hỗ trợ PDF, JPG/JPEG và PNG, tối đa 10 MB mỗi tệp."
        style={{ marginBottom: 20 }}
      />

      <Form
        form={form}
        layout="vertical"
        onFinish={onSubmit}
        initialValues={{ patientId: patientIdFilter || undefined }}
      >
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item
              name="patientId"
              label="Bệnh nhân"
              rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}
            >
              <Select
                showSearch
                placeholder="Chọn bệnh nhân"
                disabled={Boolean(patientIdFilter)}
                optionFilterProp="label"
                options={patients.map((patient) => ({
                  value: patient.id,
                  label: `${patient.fullName} (${patient.patientCode || patient.id})`,
                }))}
                onChange={() => form.setFieldValue('resultId', undefined)}
              />
            </Form.Item>
          </Col>

          <Col xs={24} md={12}>
            <Form.Item
              name="resultId"
              label="Kết quả cận lâm sàng"
              rules={[{ required: true, message: 'Vui lòng chọn kết quả nhận tệp' }]}
            >
              <Select
                showSearch
                optionFilterProp="label"
                disabled={!selectedPatientId}
                placeholder={selectedPatientId ? 'Chọn kết quả cận lâm sàng' : 'Chọn bệnh nhân trước'}
                notFoundContent="Bệnh nhân chưa có kết quả cận lâm sàng"
                options={availableResults.map((result) => ({
                  value: result.id,
                  label: `${result.visitCode || shortId(result.visitId)} · KQ ${shortId(result.id)} · ${result.resultType || 'Kết quả'} · ${result.status || '—'}`,
                }))}
              />
            </Form.Item>
          </Col>
        </Row>

        {selectedResult && (
          <Alert
            type="success"
            showIcon
            message={`Đang gắn tệp vào kết quả ${shortId(selectedResult.id)}`}
            description={(
              <Space direction="vertical" size={0}>
                <Text>Lượt khám: {selectedResult.visitCode || selectedResult.visitId || '—'}</Text>
                <Text>Kết luận: {selectedResult.conclusion || selectedResult.textValue || 'Chưa có kết luận'}</Text>
              </Space>
            )}
            style={{ marginBottom: 20 }}
          />
        )}

        <Form.Item label="Tệp đính kèm (PDF/JPG/PNG, tối đa 10 MB)">
          <Upload.Dragger
            multiple
            accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
            beforeUpload={(file) => {
              if (!ALLOWED_CONTENT_TYPES.includes(file.type)) {
                message.error(`Tệp ${file.name} không đúng định dạng PDF, JPG hoặc PNG.`)
                return Upload.LIST_IGNORE
              }
              if (file.size <= 0 || file.size > MAX_FILE_SIZE) {
                message.error(`Tệp ${file.name} phải có dung lượng từ 1 byte đến 10 MB.`)
                return Upload.LIST_IGNORE
              }
              setFileList((current) => current.some((item) => item.uid === file.uid)
                ? current
                : [...current, file])
              return false
            }}
            fileList={fileList}
            onRemove={(file) => setFileList((current) =>
              current.filter((item) => item.uid !== file.uid),
            )}
          >
            <p className="ant-upload-drag-icon"><InboxOutlined style={{ color: '#2563eb' }} /></p>
            <p className="ant-upload-text">Kéo thả tệp vào đây hoặc nhấn để chọn từ máy tính</p>
            <p className="ant-upload-hint">Mỗi tệp sẽ được upload vào đúng clinical result đã chọn.</p>
          </Upload.Dragger>
        </Form.Item>

        <Form.Item style={{ textAlign: 'right', marginTop: 24 }}>
          <Space>
            <Button onClick={onCancel}>Hủy</Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={uploading}
              disabled={!selectedResultId || fileList.length === 0}
              icon={<CloudUploadOutlined />}
            >
              Tải tệp lên kết quả
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </div>
  )
}

export default AttachmentUploadForm
