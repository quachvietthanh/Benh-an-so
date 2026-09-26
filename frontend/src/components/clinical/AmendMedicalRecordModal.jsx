import React, { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Form,
  Input,
  Modal,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  FileProtectOutlined,
  HistoryOutlined,
  SafetyCertificateFilled,
  UserOutlined,
} from '@ant-design/icons'
import medicalRecordApi from '../../api/medicalRecordApi'
import { getApiErrorMessage } from '../../utils/apiError'
import { formatDateTime } from '../../utils/helpers'

const { TextArea } = Input
const { Text, Paragraph, Title } = Typography

const QUICK_REASONS = [
  'Bổ sung chẩn đoán phụ sau hội chẩn',
  'Đính chính liều dùng và hướng dẫn sử dụng thuốc',
  'Bổ sung diễn tiến lâm sàng & ghi nhận sau tái khám',
  'Đính chính sai sót chính tả / thuật ngữ chuyên môn',
  'Bổ sung kết quả cận lâm sàng gửi muộn',
  'Điều chỉnh lời dặn và hẹn tái khám',
]

export default function AmendMedicalRecordModal({
  open,
  onClose,
  onSuccess,
  recordId,
  encounterContext,
  medicalRecord,
  patient,
  currentUser,
  primaryIcd,
  secondaryIcds,
  formValues,
}) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)

  const effectiveRecordId =
    recordId ||
    medicalRecord?.medicalRecordId ||
    medicalRecord?.id ||
    encounterContext?.medicalRecord?.medicalRecordId ||
    encounterContext?.medicalRecord?.id

  const isVisitCompleted =
    encounterContext?.visit?.status === 'COMPLETED' ||
    encounterContext?.queueItem?.status === 'COMPLETED'

  const patientName = patient?.fullName || encounterContext?.patient?.fullName || '—'
  const patientCode = patient?.patientCode || encounterContext?.patient?.patientCode || '—'
  const visitCode = encounterContext?.visit?.visitCode || encounterContext?.visit?.id || '—'
  const doctorName = encounterContext?.doctor?.fullName || currentUser?.fullName || currentUser?.username || '—'

  const handleQuickReasonClick = (reason) => {
    form.setFieldsValue({ reason })
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (!effectiveRecordId) {
        message.error('Không tìm thấy mã hồ sơ bệnh án để lập bản đính chính.')
        return
      }

      setSubmitting(true)
      const payload = {
        reason: values.reason.trim(),
        content: values.content.trim(),
      }

      const response = await medicalRecordApi.amend(effectiveRecordId, payload)

      message.success('Đã lập bản đính chính bệnh án thành công!')
      form.resetFields()
      if (onSuccess) {
        onSuccess(response?.data)
      }
      onClose()
    } catch (err) {
      if (err?.errorFields) return
      const code = err?.response?.data?.code
      if (code === 'MEDICAL_RECORD_NOT_LOCKED' || code === 'MEDICAL_RECORD_NOT_SIGNED') {
        message.error('Bệnh án phải ở trạng thái Đã ký hoặc Đã khóa mới có thể lập đính chính.')
      } else if (code === 'ACCESS_DENIED' || code === 'MEDICAL_RECORD_ACCESS_DENIED') {
        message.error('Chỉ bác sĩ phụ trách lượt khám mới có quyền lập bản đính chính cho bệnh án này.')
      } else if (
        code === 'VISIT_NOT_COMPLETED' ||
        code === 'MEDICAL_RECORD_AMENDMENT_REQUIRES_COMPLETED_VISIT'
      ) {
        message.error('Lượt khám chưa ở trạng thái hoàn tất. Vui lòng hoàn tất ca khám trước khi lập bản đính chính.')
      } else {
        const errorMsg = getApiErrorMessage(err, 'Không thể lập bản đính chính bệnh án. Vui lòng thử lại.')
        message.error(errorMsg)
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 8,
              background: '#fef3c7',
              color: '#d97706',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 18,
            }}
          >
            <EditOutlined />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#1e293b' }}>
              Lập Bản Đính Chính Bệnh Án
            </div>
            <div style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
              Sửa đổi, bổ sung thông tin chuyên môn mà vẫn bảo toàn nguyên vẹn bản gốc
            </div>
          </div>
        </div>
      }
      open={open}
      onCancel={onClose}
      width={720}
      destroyOnClose
      footer={[
        <Button key="cancel" onClick={onClose} disabled={submitting}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<EditOutlined />}
          loading={submitting}
          onClick={handleSubmit}
          style={{
            background: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)',
            borderColor: '#d97706',
            fontWeight: 600,
          }}
        >
          Xác nhận lập đính chính
        </Button>,
      ]}
    >
      <div style={{ marginTop: 8 }}>
        <Card
          size="small"
          style={{
            background: '#f8fafc',
            borderColor: '#e2e8f0',
            borderRadius: 8,
            marginBottom: 16,
          }}
        >
          <Descriptions size="small" column={{ xs: 1, sm: 2 }}>
            <Descriptions.Item label="Bệnh nhân">
              <Text strong>{patientName}</Text> <Tag color="blue">{patientCode}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Mã lượt khám">
              <Text code>{visitCode}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Bác sĩ phụ trách">
              <Space size={4}>
                <UserOutlined />
                <Text strong>{doctorName}</Text>
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái hồ sơ">
              {medicalRecord?.status === 'LOCKED' ? (
                <Tag color="success" icon={<SafetyCertificateFilled />}>
                  ĐÃ KÝ & KHÓA NỘI DUNG GỐC
                </Tag>
              ) : medicalRecord?.status === 'SIGNED' ? (
                <Tag color="success" icon={<SafetyCertificateFilled />}>
                  ĐÃ KÝ SỐ NỘI DUNG GỐC
                </Tag>
              ) : (
                <Tag color="processing" icon={<CheckCircleOutlined />}>
                  TỰ ĐỘNG KÝ & KHÓA KHI LẬP ĐÍNH CHÍNH
                </Tag>
              )}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        <Alert
          type="warning"
          showIcon
          icon={<FileProtectOutlined style={{ fontSize: 18, color: '#d97706' }} />}
          message={<span style={{ fontWeight: 600 }}>Nguyên tắc toàn vẹn bệnh án số</span>}
          description="Nội dung bệnh án ban đầu đã được ký điện tử và lưu trữ bất biến. Bản đính chính này sẽ được tạo thành một phiên bản bổ sung (V2, V3...) kèm lý do và thời điểm cụ thể, cả hai bản đều được lưu vết phục vụ tra cứu y khoa và pháp lý."
          style={{
            marginBottom: 16,
            borderRadius: 8,
            borderColor: '#fde68a',
            background: '#fffbeb',
          }}
        />

        {!isVisitCompleted && (
          <Alert
            type="info"
            showIcon
            message="Lưu ý về quy trình hoàn tất ca khám"
            description="Theo quy định EMR, bản đính chính chỉ áp dụng cho hồ sơ sau khi ca khám đã hoàn tất. Hệ thống sẽ tự động hoàn tất lượt khám này khi bạn gửi bản đính chính."
            style={{
              marginBottom: 16,
              borderRadius: 8,
              background: '#f0fdf4',
              borderColor: '#bbf7d0',
            }}
          />
        )}

        <Form form={form} layout="vertical">
          <Form.Item
            name="reason"
            label={
              <span style={{ fontWeight: 600, fontSize: 13.5 }}>
                Lý do đính chính <span style={{ color: '#ef4444' }}>*</span>
              </span>
            }
            rules={[
              { required: true, message: 'Vui lòng nhập hoặc chọn lý do đính chính bệnh án.' },
              { min: 5, message: 'Lý do đính chính cần tối thiểu 5 ký tự.' },
            ]}
            extra={
              <div style={{ marginTop: 6 }}>
                <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                  Gợi ý lý do nhanh:
                </Text>
                <Space wrap size={[4, 6]}>
                  {QUICK_REASONS.map((reason) => (
                    <Tag
                      key={reason}
                      style={{ cursor: 'pointer', borderRadius: 4, padding: '2px 8px' }}
                      color="orange"
                      onClick={() => handleQuickReasonClick(reason)}
                    >
                      + {reason}
                    </Tag>
                  ))}
                </Space>
              </div>
            }
          >
            <Input
              placeholder="Ví dụ: Bổ sung chẩn đoán phụ sau khi hội chẩn chuyên khoa hoặc điều chỉnh phác đồ..."
              maxLength={255}
              showCount
              style={{ borderRadius: 6 }}
            />
          </Form.Item>

          <Form.Item
            name="content"
            label={
              <span style={{ fontWeight: 600, fontSize: 13.5 }}>
                Nội dung đính chính / Bổ sung chi tiết <span style={{ color: '#ef4444' }}>*</span>
              </span>
            }
            rules={[
              { required: true, message: 'Vui lòng nhập nội dung đính chính chi tiết.' },
              { min: 5, message: 'Nội dung đính chính cần tối thiểu 5 ký tự.' },
            ]}
          >
            <TextArea
              rows={5}
              placeholder="Nhập chi tiết các nội dung chuyên môn cần đính chính, bổ sung kết luận, thay đổi chỉ định hoặc diễn biến lâm sàng của bệnh nhân..."
              showCount
              maxLength={2000}
              style={{ borderRadius: 6 }}
            />
          </Form.Item>
        </Form>
      </div>
    </Modal>
  )
}
