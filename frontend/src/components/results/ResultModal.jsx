import React, { useEffect, useState } from 'react'
import {
  Modal,
  Form,
  Button,
  Tag,
  Typography,
  Space,
  Row,
  Col,
  Card,
  message,
  Divider,
  Alert,
} from 'antd'
import {
  FileDoneOutlined,
  CheckCircleOutlined,
  SaveOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import ResultForm from './ResultForm'
import ResultStatusBadge from './ResultStatusBadge'

const { Text } = Typography

export const ResultModal = ({
  visible,
  order,
  onClose,
  onSaveSuccess,
}) => {
  const [form] = Form.useForm()
  const [fileList, setFileList] = useState([])
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const isConfirmed = order?.status === 'CONFIRMED'
  const isCancelled = order?.status === 'CANCELLED'

  useEffect(() => {
    if (visible && order) {
      form.setFieldsValue({
        resultValues: order.resultValues || order.resultSummary || '',
        conclusion: order.conclusion || (order.resultSummary ? `Kết quả: ${order.resultSummary}` : ''),
        notes: order.notes || '',
      })

      setFileList(order.attachments || [])
      setErrors({})
    }
  }, [visible, order, form])

  const validate = () => {
    const values = form.getFieldsValue()
    const newErrors = {}

    if (!values.resultValues || !values.resultValues.trim()) {
      newErrors.resultValues = 'Vui lòng nhập kết quả chi tiết / các chỉ số đo đạc!'
    }

    if (!values.conclusion || !values.conclusion.trim()) {
      newErrors.conclusion = 'Vui lòng nhập kết luận chẩn đoán!'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSaveResult = async (targetStatus) => {
    if (targetStatus !== 'CANCELLED' && !validate()) {
      message.error('Vui lòng hoàn thiện các trường dữ liệu bắt buộc!')
      return
    }

    try {
      setSaving(true)
      const values = form.getFieldsValue()

      const updatedRecord = {
        ...order,
        resultValues: values.resultValues,
        conclusion: values.conclusion,
        notes: values.notes,
        resultSummary: values.conclusion,
        attachments: fileList,
        status: targetStatus,
        updatedAt: new Date().toISOString(),
        enteredBy: order?.enteredBy || 'KTV. Nguyễn Văn Hùng',
        confirmedBy: targetStatus === 'CONFIRMED' ? 'BS. Phạm Hồng Anh' : order?.confirmedBy,
      }

      await onSaveSuccess(updatedRecord)

      if (targetStatus === 'CONFIRMED') {
        message.success(`Bác sĩ đã xác nhận kết quả #${order.orderCode} thành công! Phiếu đã chuyển sang chế độ Khóa chỉnh sửa.`)
      } else {
        message.success(`Đã lưu kết quả cận lâm sàng #${order.orderCode} (Trạng thái: Đã có kết quả)!`)
      }

      onClose()
    } catch (err) {
      console.error(err)
      message.error('Có lỗi xảy ra khi lưu kết quả cận lâm sàng')
    } finally {
      setSaving(false)
    }
  }

  if (!order) return null

  return (
    <Modal
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: 24 }}>
          <Space>
            <FileDoneOutlined style={{ color: '#2563eb', fontSize: 20 }} />
            <span style={{ fontSize: 18, fontWeight: 700, color: '#0f172a' }}>
              Phiếu kết quả cận lâm sàng #{order.orderCode}
            </span>
          </Space>
          <Space>
            <ResultStatusBadge status={order.status} />
            {isConfirmed && (
              <Tag color="green" icon={<LockOutlined />} style={{ borderRadius: 6, fontWeight: 600 }}>
                Đã khóa
              </Tag>
            )}
          </Space>
        </div>
      }
      open={visible}
      onCancel={onClose}
      width={920}
      style={{ top: 20 }}
      destroyOnClose
      footer={
        isConfirmed || isCancelled ? [
          <Button key="close" type="primary" size="large" onClick={onClose} style={{ borderRadius: 8 }}>
            Đóng
          </Button>,
        ] : [
          <Button key="cancel" size="large" onClick={onClose} style={{ borderRadius: 8 }}>
            Hủy bỏ
          </Button>,
          <Button
            key="saveResult"
            size="large"
            icon={<SaveOutlined />}
            loading={saving}
            onClick={() => handleSaveResult('RESULTED')}
            style={{ borderRadius: 8, borderColor: '#2563eb', color: '#2563eb' }}
          >
            Lưu kết quả
          </Button>,
          <Button
            key="saveConfirm"
            type="primary"
            size="large"
            icon={<SafetyCertificateOutlined />}
            loading={saving}
            onClick={() => handleSaveResult('CONFIRMED')}
            style={{ borderRadius: 8, background: '#16a34a', borderColor: '#16a34a' }}
          >
            Bác sĩ xác nhận & Khóa
          </Button>,
        ]
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16, marginTop: 12 }}>
        {/* Patient Demographics & Order Metadata Header */}
        <Card
          size="small"
          style={{
            background: '#f8fafc',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
          }}
        >
          <Row gutter={[16, 12]}>
            <Col xs={24} sm={12} md={8}>
              <div style={{ fontSize: 12, color: '#64748b' }}>Họ và tên bệnh nhân:</div>
              <div style={{ fontWeight: 700, fontSize: 15, color: '#0f172a' }}>
                {order.patientName}
              </div>
              <div style={{ fontSize: 12, color: '#3b82f6', fontWeight: 600 }}>
                Mã BN: {order.patientCode}
              </div>
            </Col>

            <Col xs={12} sm={6} md={4}>
              <div style={{ fontSize: 12, color: '#64748b' }}>Giới tính / Tuổi:</div>
              <div style={{ fontWeight: 600, fontSize: 14, color: '#334155' }}>
                {order.gender || 'Nam'} ({order.age || 30}T)
              </div>
            </Col>

            <Col xs={12} sm={6} md={4}>
              <div style={{ fontSize: 12, color: '#64748b' }}>Ngày sinh:</div>
              <div style={{ fontWeight: 600, fontSize: 14, color: '#334155' }}>
                {order.dateOfBirth || '15/08/1990'}
              </div>
            </Col>

            <Col xs={24} sm={12} md={8}>
              <div style={{ fontSize: 12, color: '#64748b' }}>Bác sĩ chỉ định:</div>
              <div style={{ fontWeight: 600, fontSize: 14, color: '#334155' }}>
                {order.doctorName || 'BS. Phạm Hồng Anh'}
              </div>
              <div style={{ fontSize: 12, color: '#64748b' }}>
                {order.department || 'Khoa Nội'} • {order.createdAt ? new Date(order.createdAt).toLocaleDateString('vi-VN') : ''}
              </div>
            </Col>
          </Row>

          <Divider style={{ margin: '10px 0' }} />

          <Row gutter={[16, 8]}>
            <Col span={24}>
              <Text style={{ fontSize: 13, color: '#475569' }}>
                <b>Dịch vụ thực hiện:</b>{' '}
                {order.items?.map((it) => `${it.serviceName} [${it.serviceCode}]`).join(', ') || 'Chỉ định cận lâm sàng'}
              </Text>
            </Col>
            <Col span={24}>
              <Text style={{ fontSize: 13, color: '#d97706' }}>
                <b>Chẩn đoán lâm sàng:</b> {order.diagnosis || 'Chưa ghi nhận'}
              </Text>
            </Col>
          </Row>
        </Card>

        {isCancelled && (
          <Alert
            message="Chỉ định này đã bị hủy"
            description={`Lý do hủy: ${order.cancelReason || 'Không xác định'}`}
            type="error"
            showIcon
          />
        )}

        {/* Input Form */}
        <ResultForm
          form={form}
          fileList={fileList}
          onFileListChange={setFileList}
          errors={errors}
          disabled={isConfirmed || isCancelled}
        />
      </div>
    </Modal>
  )
}

export default ResultModal
