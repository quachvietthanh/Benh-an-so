import React from 'react'
import { Form, Input, Typography, Card, Alert } from 'antd'
import { FileTextOutlined, CheckSquareOutlined, CommentOutlined, LockOutlined } from '@ant-design/icons'
import ResultUpload from './ResultUpload'

const { TextArea } = Input
const { Text } = Typography

export const ResultForm = ({
  form,
  fileList,
  onFileListChange,
  errors = {},
  disabled = false,
}) => {
  return (
    <Form form={form} layout="vertical" requiredMark="optional">
      {disabled && (
        <Alert
          message="Thông tin đã được Bác sĩ xác nhận & Khóa chỉnh sửa"
          description="Kết quả cận lâm sàng này đã hoàn thành luồng xác nhận chuyên môn và được khóa chỉnh sửa để đảm bảo tính an toàn dữ liệu."
          type="success"
          showIcon
          icon={<LockOutlined style={{ fontSize: 20 }} />}
          style={{ marginBottom: 16, borderRadius: 10, border: '1px solid #b7eb8f' }}
        />
      )}

      <Card
        size="small"
        style={{
          borderRadius: '12px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
          marginBottom: 16,
          background: disabled ? '#fafafa' : '#ffffff',
        }}
      >
        <Form.Item
          name="resultValues"
          label={
            <Text style={{ fontWeight: 600, fontSize: 14, color: '#1e293b' }}>
              <FileTextOutlined style={{ color: '#2563eb', marginRight: 6 }} />
              Kết quả chi tiết / Các chỉ số đo đạc {!disabled && <span style={{ color: '#ff4d4f' }}>*</span>}
            </Text>
          }
          validateStatus={errors.resultValues ? 'error' : ''}
          help={errors.resultValues}
          style={{ marginBottom: 16 }}
        >
          <TextArea
            rows={4}
            disabled={disabled}
            placeholder="Ví dụ:&#10;- RBC: 4.8 T/L, WBC: 7.2 G/L, Hb: 142 g/L, PLT: 250 G/L&#10;- Glucose lúc đói: 5.6 mmol/L, Ure: 4.8 mmol/L, Creatinine: 78 umol/L..."
            style={{ borderRadius: '8px', fontSize: 13.5, fontFamily: 'monospace, Inter, sans-serif' }}
          />
        </Form.Item>

        <Form.Item
          name="conclusion"
          label={
            <Text style={{ fontWeight: 600, fontSize: 14, color: '#1e293b' }}>
              <CheckSquareOutlined style={{ color: '#16a34a', marginRight: 6 }} />
              Kết luận chẩn đoán {!disabled && <span style={{ color: '#ff4d4f' }}>*</span>}
            </Text>
          }
          validateStatus={errors.conclusion ? 'error' : ''}
          help={errors.conclusion}
          style={{ marginBottom: 16 }}
        >
          <TextArea
            rows={3}
            disabled={disabled}
            placeholder="Ví dụ: Các chỉ số sinh hóa gan thận nằm trong giới hạn bình thường. Chưa phát hiện bất thường trên phim X-Quang ngực thẳng."
            style={{ borderRadius: '8px', fontSize: 13.5 }}
          />
        </Form.Item>

        <Form.Item
          name="notes"
          label={
            <Text style={{ fontWeight: 600, fontSize: 14, color: '#1e293b' }}>
              <CommentOutlined style={{ color: '#d97706', marginRight: 6 }} />
              Ghi chú & Đề xuất theo dõi (Nếu có)
            </Text>
          }
          style={{ marginBottom: 16 }}
        >
          <Input
            disabled={disabled}
            placeholder="Ví dụ: Đề nghị làm thêm xét nghiệm HbA1c sau 3 tháng hoặc tái khám khi có dấu hiệu bất thường..."
            style={{ borderRadius: '8px', fontSize: 13.5 }}
          />
        </Form.Item>
      </Card>

      <Card
        title={
          <Text style={{ fontWeight: 600, fontSize: 14, color: '#1e293b' }}>
            Đính kèm file hình ảnh / PDF phiếu kết quả
          </Text>
        }
        size="small"
        style={{
          borderRadius: '12px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
          background: disabled ? '#fafafa' : '#ffffff',
        }}
      >
        <ResultUpload
          fileList={fileList}
          onChange={onFileListChange}
          error={errors.files}
          disabled={disabled}
        />
      </Card>
    </Form>
  )
}

export default ResultForm
