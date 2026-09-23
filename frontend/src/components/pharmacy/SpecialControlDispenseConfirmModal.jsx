import React, { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  Divider,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  AuditOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  MedicineBoxOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import SpecialControlBadge from './SpecialControlBadge.jsx'
import {
  getSpecialControlMeta,
  validateDispenseConfirm,
} from '../../utils/specialControlHelpers.js'

const { Text, Paragraph } = Typography
const { TextArea } = Input

export default function SpecialControlDispenseConfirmModal({
  open,
  prescription,
  specialItems = [],
  batches = [],
  onConfirm,
  onCancel,
  loading = false,
}) {
  const [form] = Form.useForm()
  const [committed, setCommitted] = useState(false)
  const [selectedBatches, setSelectedBatches] = useState({})

  const patientName = prescription?.patientName || prescription?.patient?.fullName || 'Bệnh nhân'
  const patientCode = prescription?.patientCode || prescription?.patient?.patientCode || '—'
  const prescriptionCode = prescription?.prescriptionCode || prescription?.code || prescription?.id || '—'

  useEffect(() => {
    if (open && prescription) {
      form.resetFields()
      setCommitted(false)

      // Gợi ý lô đầu tiên khả dụng theo FEFO cho từng thuốc
      const initialBatchMap = {}
      specialItems.forEach((item) => {
        const itemBatches = batches.filter(
          (b) => String(b.medicineId) === String(item.medicineId) && (b.stockQuantity > 0 || b.eligibleStockQuantity > 0),
        )
        if (itemBatches.length > 0) {
          initialBatchMap[item.medicineId] = itemBatches[0].id || itemBatches[0].batchNumber
        }
      })
      setSelectedBatches(initialBatchMap)

      form.setFieldsValue({
        receiverName: patientName,
        receiverIdCard: prescription?.patient?.identityCard || prescription?.patientIdCard || '',
        confirmationNote: '',
      })
    }
  }, [open, prescription, specialItems, batches, form, patientName])

  const handleBatchChange = (medicineId, batchId) => {
    setSelectedBatches((prev) => ({
      ...prev,
      [medicineId]: batchId,
    }))
  }

  const handleFinish = async (values) => {
    if (!committed) {
      message.error('Vui lòng tích xác nhận cam kết đối chiếu trước khi thực hiện cấp phát.')
      return
    }

    // Kiểm tra thông tin người nhận
    const validation = validateDispenseConfirm({
      receiverName: values.receiverName,
      receiverIdCard: values.receiverIdCard,
      confirmed: committed,
    })

    if (!validation.isValid) {
      message.error(validation.error)
      return
    }

    // Kiểm tra xem tất cả thuốc kiểm soát đặc biệt đã chọn lô chưa
    for (const item of specialItems) {
      const selectedBatchId = selectedBatches[item.medicineId]
      if (!selectedBatchId && batches.length > 0) {
        message.error(`Chưa chọn số lô xuất kho cho thuốc "${item.medicineName}".`)
        return
      }

      // Kiểm tra tồn kho lô (QTN-06)
      const foundBatch = batches.find(
        (b) => String(b.id) === String(selectedBatchId) || String(b.batchNumber) === String(selectedBatchId),
      )
      if (foundBatch) {
        const avail = foundBatch.eligibleStockQuantity ?? foundBatch.stockQuantity ?? 0
        if (Number(item.quantity) > avail) {
          message.error(
            `Lô ${foundBatch.batchNumber || selectedBatchId} của thuốc "${item.medicineName}" chỉ còn tồn ${avail}, không đủ cấp ${item.quantity} theo đơn.`,
          )
          return
        }
      }
    }

    if (onConfirm) {
      await onConfirm({
        receiverName: values.receiverName.trim(),
        receiverIdCard: values.receiverIdCard.trim(),
        confirmationNote: (values.confirmationNote || '').trim(),
        selectedBatches,
      })
    }
  }

  const columns = [
    {
      title: 'STT',
      key: 'stt',
      width: 50,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Thuốc kiểm soát đặc biệt',
      key: 'medicineName',
      render: (_, record) => (
        <div>
          <Text strong style={{ color: '#1e293b' }}>
            {record.medicineName || 'Thuốc kiểm soát đặc biệt'}
          </Text>
          <div style={{ marginTop: 4 }}>
            <SpecialControlBadge
              isSpecialControl={true}
              group={record.specialControlGroup}
            />
          </div>
          {record.specialControlReason && (
            <div style={{ marginTop: 4, fontSize: 12, color: '#b45309' }}>
              <strong>Lý do chỉ định của bác sĩ:</strong> {record.specialControlReason}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Số lượng kê',
      key: 'quantity',
      width: 110,
      align: 'center',
      render: (_, record) => (
        <Text strong style={{ fontSize: 14, color: '#0369a1' }}>
          {record.quantity} {record.unit || 'viên'}
        </Text>
      ),
    },
    {
      title: 'Lô thuốc & Tồn kho khả dụng',
      key: 'batchSelection',
      width: 260,
      render: (_, record) => {
        const medBatches = batches.filter((b) => String(b.medicineId) === String(record.medicineId))
        const currentBatchId = selectedBatches[record.medicineId]
        const currentBatch = medBatches.find(
          (b) => String(b.id) === String(currentBatchId) || String(b.batchNumber) === String(currentBatchId),
        )
        const avail = currentBatch ? (currentBatch.eligibleStockQuantity ?? currentBatch.stockQuantity ?? 0) : 0
        const isShortage = currentBatch && Number(record.quantity) > avail

        if (!medBatches.length) {
          return (
            <Text type="secondary" style={{ fontSize: 12 }}>
              Lô kho mặc định (Hệ thống tự động phân bổ FEFO)
            </Text>
          )
        }

        return (
          <div>
            <Select
              style={{ width: '100%' }}
              value={currentBatchId}
              placeholder="Chọn số lô xuất kho..."
              onChange={(val) => handleBatchChange(record.medicineId, val)}
              options={medBatches.map((b) => {
                const bAvail = b.eligibleStockQuantity ?? b.stockQuantity ?? 0
                return {
                  value: b.id || b.batchNumber,
                  label: `${b.batchNumber} (Tồn: ${bAvail} — HSD: ${b.expiryDate ? new Date(b.expiryDate).toLocaleDateString('vi-VN') : '—'})`,
                }
              })}
            />
            {isShortage && (
              <div style={{ color: '#ef4444', fontSize: 11.5, marginTop: 4, fontWeight: 600 }}>
                ⚠️ Lô này không đủ số lượng để xuất kho! (Cần: {record.quantity}, Tồn: {avail})
              </div>
            )}
          </div>
        )
      },
    },
  ]

  return (
    <Modal
      open={open}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#b45309' }}>
          <SafetyCertificateOutlined style={{ fontSize: 22, color: '#d97706' }} />
          <span style={{ fontSize: 16, fontWeight: 700 }}>
            Xác nhận xuất kho thuốc kiểm soát đặc biệt
          </span>
        </div>
      }
      onCancel={onCancel}
      footer={null}
      width={780}
      maskClosable={false}
      destroyOnClose
    >
      <Alert
        type="warning"
        showIcon
        icon={<WarningOutlined style={{ fontSize: 18 }} />}
        message={
          <Text strong style={{ color: '#92400e' }}>
            Quy định xuất kho thuốc kiểm soát đặc biệt
          </Text>
        }
        description={
          <div style={{ fontSize: 12.5, color: '#78350f', marginTop: 2 }}>
            Đơn thuốc có chứa thuốc thuộc danh mục kiểm soát đặc biệt. Dược sĩ bắt buộc phải kiểm tra đối chiếu
            giấy tờ tùy thân người nhận thuốc, kiểm tra số lượng tồn kho lô và xác nhận cam kết trước khi tiến hành xuất kho.
          </div>
        }
        style={{
          marginBottom: 16,
          backgroundColor: '#fffbeb',
          borderColor: '#fde68a',
          borderRadius: 8,
        }}
      />

      {/* Thông tin đơn thuốc tóm tắt */}
      <Card
        size="small"
        style={{
          backgroundColor: '#f8fafc',
          borderColor: '#e2e8f0',
          borderRadius: 8,
          marginBottom: 16,
        }}
      >
        <Descriptions size="small" column={{ xs: 1, sm: 3 }}>
          <Descriptions.Item label="Mã đơn thuốc">
            <Text strong style={{ color: '#0369a1' }}>{prescriptionCode}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Họ tên bệnh nhân">
            <strong>{patientName}</strong>
          </Descriptions.Item>
          <Descriptions.Item label="Mã bệnh nhân">
            <Text type="secondary">{patientCode}</Text>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Bảng danh sách thuốc kiểm soát đặc biệt trong đơn */}
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 600, color: '#334155', marginBottom: 8, fontSize: 13.5 }}>
          Danh sách thuốc kiểm soát đặc biệt cần xuất kho:
        </div>
        <Table
          dataSource={specialItems}
          columns={columns}
          rowKey={(r) => r.medicineId || r.id}
          pagination={false}
          size="small"
          bordered
        />
      </div>

      {/* Form thông tin người nhận và đối chiếu */}
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <Form.Item
              name="receiverName"
              label={
                <span style={{ fontWeight: 600, color: '#334155' }}>
                  Họ tên người nhận thuốc <span style={{ color: '#ef4444' }}>*</span>
                </span>
              }
              rules={[{ required: true, message: 'Vui lòng nhập họ tên người nhận thuốc' }]}
            >
              <Input
                prefix={<UserOutlined style={{ color: '#94a3b8' }} />}
                placeholder="Nhập họ tên người trực tiếp nhận thuốc..."
                style={{ height: 40 }}
              />
            </Form.Item>
          </Col>

          <Col xs={24} sm={12}>
            <Form.Item
              name="receiverIdCard"
              label={
                <span style={{ fontWeight: 600, color: '#334155' }}>
                  Số CCCD / CMND người nhận <span style={{ color: '#ef4444' }}>*</span>
                </span>
              }
              rules={[
                { required: true, message: 'Vui lòng nhập số CCCD hoặc CMND' },
                {
                  pattern: /^(\d{9}|\d{12})$/,
                  message: 'Số CMND phải có 9 chữ số hoặc CCCD phải có 12 chữ số',
                },
              ]}
            >
              <Input
                prefix={<AuditOutlined style={{ color: '#94a3b8' }} />}
                placeholder="Nhập 9 hoặc 12 chữ số CMND/CCCD..."
                maxLength={12}
                style={{ height: 40 }}
              />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          name="confirmationNote"
          label={<span style={{ fontWeight: 600, color: '#334155' }}>Ghi chú xuất kho bổ sung (nếu có)</span>}
        >
          <TextArea
            rows={2}
            placeholder="Ghi chú thêm về việc đối chiếu giấy tờ, người nhận hộ..."
            maxLength={300}
            showCount
          />
        </Form.Item>

        <div
          style={{
            background: '#eff6ff',
            border: '1px solid #bfdbfe',
            padding: '12px 16px',
            borderRadius: 8,
            marginBottom: 20,
          }}
        >
          <Checkbox
            checked={committed}
            onChange={(e) => setCommitted(e.target.checked)}
          >
            <span style={{ fontWeight: 600, color: '#1e40af', fontSize: 13 }}>
              Tôi xác nhận đã kiểm tra đối chiếu thẻ CCCD/CMND của người nhận thuốc, kiểm tra đúng đơn thuốc và cam kết thực hiện đúng quy định xuất kho thuốc kiểm soát đặc biệt.
            </span>
          </Checkbox>
        </div>

        <Divider style={{ margin: '12px 0 16px' }} />

        {/* Nút bấm chuẩn hóa kích thước 42px */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
          <Button
            onClick={onCancel}
            disabled={loading}
            style={{
              height: 42,
              minWidth: 100,
              fontSize: 14,
              fontWeight: 500,
            }}
          >
            Hủy bỏ
          </Button>

          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            icon={<CheckCircleOutlined />}
            style={{
              height: 42,
              minWidth: 150,
              fontSize: 15,
              fontWeight: 600,
              backgroundColor: '#059669',
              borderColor: '#059669',
            }}
          >
            Xác nhận cấp phát
          </Button>
        </div>
      </Form>
    </Modal>
  )
}
