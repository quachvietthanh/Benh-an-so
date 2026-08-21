import React, { useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Descriptions,
  Divider,
  Empty,
  Modal,
  Space,
  Table,
  Tabs,
  Tag,
  Timeline,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  BarcodeOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CopyOutlined,
  EditOutlined,
  FileTextOutlined,
  HistoryOutlined,
  MedicineBoxOutlined,
  PlusCircleOutlined,
  PrinterOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import pharmacyApi from '../../api/pharmacyApi'
import { fixMojibake } from '../../utils/serviceCatalogValidation'
import {
  formatPrescriptionCode,
  isStandardRxCode,
} from '../../utils/electronicPrescriptionValidation'

const { Text, Paragraph, Title } = Typography

const ROUTE_LABELS = {
  ORAL: 'Uống',
  TOPICAL: 'Bôi ngoài da',
  INHALATION: 'Hít / Khí dung',
  OPHTHALMIC: 'Nhỏ / Tra mắt',
  NASAL: 'Xịt / Nhỏ mũi',
  OTIC: 'Nhỏ tai',
  SUBLINGUAL: 'Ngậm dưới lưỡi',
  RECTAL: 'Đặt hậu môn / Trực tràng',
  INTRAVENOUS: 'Tiêm tĩnh mạch',
  INTRAMUSCULAR: 'Tiêm bắp',
  SUBCUTANEOUS: 'Tiêm dưới da',
  TRANSDERMAL: 'Dán ngoài da',
  VAGINAL: 'Đặt âm đạo',
  OTHER: 'Cách dùng khác',
}

function PrescriptionDetailModal({
  open,
  onClose,
  prescription,
  medicines = [],
  onEditClick,
  onPrintClick,
  canEdit = false,
}) {
  if (!prescription) return null

  const getMedicineName = (id, fallback) => {
    const found = medicines.find((m) => String(m.id) === String(id))
    return fixMojibake(found?.medicineName || found?.name || fallback || id || '—')
  }

  const items = prescription.items || []
  const warnings = prescription.warnings || []
  const historyLogs = prescription.amendments || prescription.historyLogs || []

  const itemColumns = [
    {
      title: 'STT',
      key: 'index',
      width: 50,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Tên thuốc / Hàm lượng',
      key: 'medicineName',
      render: (_, item) => (
        <div>
          <Text strong style={{ color: '#1e40af' }}>
            {item.medicineName || getMedicineName(item.medicineId)}
          </Text>
          {item.strength && <Text type="secondary" style={{ marginLeft: 6 }}>({item.strength})</Text>}
          {item.activeIngredient && (
            <div style={{ fontSize: 12, color: '#6b7280' }}>
              Hoạt chất: {item.activeIngredient}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Cách dùng',
      dataIndex: 'route',
      key: 'route',
      width: 110,
      render: (route) => (
        <Tag color="cyan">{ROUTE_LABELS[route] || route || 'Mặc định'}</Tag>
      ),
    },
    {
      title: 'Liều & Tần suất',
      key: 'dosageFreq',
      render: (_, item) => (
        <div>
          <div><Text strong>{item.dosage || '—'}</Text></div>
          <div style={{ fontSize: 12, color: '#4b5563' }}>
            {item.frequency != null && item.frequency !== ''
              ? typeof item.frequency === 'number' || !isNaN(Number(item.frequency))
                ? `${item.frequency} lần/ngày`
                : item.frequency
              : '—'}
          </div>
        </div>
      ),
    },
    {
      title: 'Số ngày',
      dataIndex: 'durationDays',
      key: 'durationDays',
      width: 80,
      align: 'center',
      render: (days) => (days ? `${days} ngày` : '—'),
    },
    {
      title: 'Số lượng',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 90,
      align: 'right',
      render: (qty, item) => (
        <Text strong style={{ fontSize: 14 }}>
          {qty} {item.unit || 'đơn vị'}
        </Text>
      ),
    },
    {
      title: 'Hướng dẫn sử dụng',
      dataIndex: 'instructions',
      key: 'instructions',
      render: (text) => text ? <Text type="secondary">{text}</Text> : '—',
    },
  ]

  const statusTag = (
    <Tag
      color={
        prescription.status === 'DISPENSED'
          ? 'green'
          : prescription.status === 'CANCELLED'
            ? 'default'
            : 'orange'
      }
      style={{ fontSize: 13, padding: '2px 8px' }}
    >
      {prescription.status === 'PENDING_DISPENSE'
        ? 'Chờ cấp phát (PENDING_DISPENSE)'
        : prescription.status === 'DISPENSED'
          ? 'Đã cấp phát (DISPENSED)'
          : prescription.status === 'CANCELLED'
            ? 'Đã hủy (CANCELLED)'
            : prescription.status}
    </Tag>
  )

  const isPending = prescription.status === 'PENDING_DISPENSE'
  const isPrintable = Boolean(
    prescription?.id &&
    prescription?.prescriptionCode &&
    (prescription?.status === 'PENDING_DISPENSE' || prescription?.status === 'DISPENSED')
  )

  const [printing, setPrinting] = useState(false)

  const handlePrintPrescription = async () => {
    if (!isPrintable || printing) return
    setPrinting(true)
    try {
      const response = await pharmacyApi.printPrescription(prescription.id)
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)

      const disposition = response.headers?.['content-disposition']
      let filename = `prescription-${prescription.prescriptionCode || prescription.id}.pdf`
      if (disposition && disposition.includes('filename=')) {
        const match = disposition.match(/filename="?([^"]+)"?/)
        if (match && match[1]) filename = match[1]
      }

      const newWindow = window.open(url, '_blank')
      if (!newWindow || newWindow.closed || typeof newWindow.closed === 'undefined') {
        const link = document.createElement('a')
        link.href = url
        link.download = filename
        document.body.appendChild(link)
        link.click()
        setTimeout(() => {
          if (link.parentNode) link.parentNode.removeChild(link)
        }, 1000)
        message.info('Trình duyệt đang chặn cửa sổ xem bản in. Bản in đơn thuốc đã được tải về máy của bạn.')
      } else {
        message.success('Đã mở bản in đơn thuốc thành công.')
      }

      setTimeout(() => {
        window.URL.revokeObjectURL(url)
      }, 30000)
    } catch (error) {
      let backendMessage = ''
      if (error.response?.data instanceof Blob) {
        try {
          const text = await error.response.data.text()
          const json = JSON.parse(text)
          backendMessage = json.message || json.error
        } catch {
          // ignore
        }
      } else if (error.response?.data?.message) {
        backendMessage = error.response.data.message
      }

      const status = error.response?.status
      if (status === 403) {
        message.error(backendMessage || 'Bạn không có quyền in đơn thuốc.')
      } else if (status === 404) {
        message.error(backendMessage || 'Không tìm thấy đơn thuốc cần in.')
      } else if (status === 400 || status === 409) {
        message.error(backendMessage || 'Đơn thuốc chưa đủ điều kiện để in.')
      } else if (status === 500) {
        message.error(backendMessage || 'Không thể tạo bản in đơn thuốc từ hệ thống. Vui lòng thử lại.')
      } else {
        message.error(backendMessage || error.message || 'Lỗi khi tạo bản in đơn thuốc.')
      }
    } finally {
      setPrinting(false)
    }
  }

  return (
    <Modal
      open={open}
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, paddingRight: 24 }}>
          <Space>
            <MedicineBoxOutlined style={{ color: '#2563eb', fontSize: 20 }} />
            <span style={{ fontSize: 17, fontWeight: 600 }}>
              Đơn thuốc Điện tử: {formatPrescriptionCode(prescription.prescriptionCode || prescription.id)}
            </span>
          </Space>
          {statusTag}
        </div>
      }
      onCancel={onClose}
      footer={
        <Space>
          <Button
            type="primary"
            icon={<PrinterOutlined />}
            loading={printing}
            disabled={!isPrintable || printing}
            onClick={() => {
              if (onPrintClick) {
                onPrintClick(prescription)
              } else {
                handlePrintPrescription()
              }
            }}
          >
            In đơn thuốc
          </Button>
          {canEdit && isPending && (
            <Button
              type="default"
              icon={<EditOutlined />}
              onClick={() => {
                onClose()
                if (onEditClick) onEditClick(prescription)
              }}
            >
              Điều chỉnh đơn này
            </Button>
          )}
          <Button onClick={onClose}>Đóng</Button>
        </Space>
      }
      width={840}
      style={{ top: 20 }}
    >
      <Card size="small" style={{ marginBottom: 16, backgroundColor: '#f8fafc' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#eff6ff', padding: '10px 14px', borderRadius: 8, marginBottom: 12, border: '1px solid #bfdbfe', flexWrap: 'wrap', gap: 8 }}>
          <Space size={8} align="center">
            <BarcodeOutlined style={{ fontSize: 22, color: '#1d4ed8' }} />
            <div>
              <div style={{ fontSize: 11, color: '#1e40af', textTransform: 'uppercase', fontWeight: 600 }}>
                Mã Đơn Thuốc Điện Tử (Định danh duy nhất)
              </div>
              <Space size={6} align="center">
                <Text code strong style={{ color: '#1d4ed8', fontSize: 18 }}>
                  {formatPrescriptionCode(prescription.prescriptionCode || prescription.id)}
                </Text>
                <Tooltip title="Sao chép mã đơn điện tử">
                  <Button
                    type="text"
                    size="small"
                    icon={<CopyOutlined style={{ color: '#2563eb' }} />}
                    onClick={() => {
                      const code = formatPrescriptionCode(prescription.prescriptionCode || prescription.id)
                      if (code && code !== '—') {
                        navigator.clipboard.writeText(code)
                        message.success(`Đã sao chép mã đơn điện tử: ${code}`)
                      }
                    }}
                  />
                </Tooltip>
                <Tag color="cyan" style={{ fontSize: 11 }}>
                  <SafetyCertificateOutlined /> Liên thông & Bất biến
                </Tag>
              </Space>
            </div>
          </Space>
          <Text type="secondary" style={{ fontSize: 12, maxWidth: 300, textAlign: 'right' }}>
            Mã được gắn cố định với đơn và không thay đổi trong suốt vòng đời của đơn.
          </Text>
        </div>

        <Descriptions size="small" column={{ xs: 1, sm: 2, lg: 3 }} bordered={false}>
          <Descriptions.Item label={<Text strong><UserOutlined /> Bệnh nhân</Text>}>
            {prescription.patientName ? (
              <span>
                <Text strong>{fixMojibake(prescription.patientName)}</Text> ({prescription.patientCode || '—'})
              </span>
            ) : '—'}
          </Descriptions.Item>
          <Descriptions.Item label={<Text strong><ClockCircleOutlined /> Lượt khám</Text>}>
            <Text code>{prescription.visitCode || '—'}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Bác sĩ kê đơn">
            <Text strong>{fixMojibake(prescription.doctorName) || '—'}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Ngày kê">
            {prescription.prescribedAt ? dayjs(prescription.prescribedAt).format('HH:mm DD/MM/YYYY') : '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Cập nhật lần cuối">
            {prescription.updatedAt ? dayjs(prescription.updatedAt).format('HH:mm DD/MM/YYYY') : 'Chưa điều chỉnh'}
          </Descriptions.Item>
        </Descriptions>
        {prescription.note && (
          <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px dashed #e2e8f0' }}>
            <Text type="secondary"><strong>Ghi chú bác sĩ:</strong> {fixMojibake(prescription.note)}</Text>
          </div>
        )}
      </Card>

      <Tabs
        defaultActiveKey="medicines"
        items={[
          {
            key: 'medicines',
            label: (
              <span>
                <MedicineBoxOutlined /> Danh sách thuốc ({items.length})
              </span>
            ),
            children: (
              <>
                <Table
                  size="small"
                  rowKey={(r, index) => r.id || r.medicineId || index}
                  dataSource={items}
                  columns={itemColumns}
                  pagination={false}
                  bordered
                />

                {warnings && warnings.length > 0 && (
                  <div style={{ marginTop: 16 }}>
                    <Alert
                      type="warning"
                      showIcon
                      icon={<WarningOutlined />}
                      message={<Text strong>Cảnh báo tương tác thuốc đã ghi nhận ({warnings.length})</Text>}
                      description={
                        <ul style={{ paddingLeft: 20, margin: '6px 0 0 0' }}>
                          {warnings.map((w, idx) => (
                            <li key={idx} style={{ marginBottom: 4 }}>
                              <Text strong color="warning">[{w.severity || 'CẢNH BÁO'}]</Text>{' '}
                              {w.warningMessage || w.description}
                              {w.overrideReason && (
                                <div style={{ fontSize: 12, color: '#2563eb' }}>
                                  Lý do bác sĩ chấp thuận: <em>{w.overrideReason}</em>
                                </div>
                              )}
                            </li>
                          ))}
                        </ul>
                      }
                    />
                  </div>
                )}
              </>
            ),
          },
          {
            key: 'amendments',
            label: (
              <span>
                <HistoryOutlined /> Lịch sử lưu vết thay đổi {historyLogs.length > 0 ? `(${historyLogs.length})` : ''}
              </span>
            ),
            children: (
              <div style={{ maxHeight: 400, overflowY: 'auto', padding: '8px 4px' }}>
                <Alert
                  type="info"
                  showIcon
                  message={`Mã đơn thuốc điện tử: ${formatPrescriptionCode(prescription.prescriptionCode || prescription.id)}`}
                  description="Mã định danh duy nhất của đơn thuốc được duy trì cố định và bất biến qua mọi lần chỉnh sửa / điều chỉnh liều."
                  style={{ marginBottom: 12 }}
                />
                {historyLogs.length === 0 ? (
                  <div style={{ padding: '24px 0', textAlign: 'center' }}>
                    <Empty
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                      description="Đơn thuốc này chưa có lần điều chỉnh nào (đang ở trạng thái kê đơn ban đầu)."
                    />
                    {prescription.updatedAt && prescription.updatedAt !== prescription.prescribedAt && (
                      <Alert
                        type="info"
                        showIcon
                        message={`Lần cập nhật gần nhất lúc ${dayjs(prescription.updatedAt).format('HH:mm DD/MM/YYYY')}`}
                        style={{ marginTop: 12, display: 'inline-block' }}
                      />
                    )}
                  </div>
                ) : (
                  <Timeline
                    mode="left"
                    items={historyLogs.map((log, idx) => {
                      const isCreate = log.action === 'CREATE' || idx === historyLogs.length - 1
                      return {
                        color: isCreate ? 'green' : 'blue',
                        dot: isCreate ? <PlusCircleOutlined /> : <EditOutlined />,
                        children: (
                          <Card size="small" style={{ marginBottom: 12, borderRadius: 8, borderColor: '#e2e8f0' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                              <Space>
                                <Tag color={isCreate ? 'green' : 'blue'}>
                                  {log.action || (isCreate ? 'KHỞI TẠO ĐƠN' : 'ĐIỀU CHỈNH ĐƠN')}
                                </Tag>
                                <Text strong>{log.changedBy || log.amendedByName || prescription.doctorName || 'Bác sĩ'}</Text>
                              </Space>
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                {log.changedAt || log.amendedAt ? dayjs(log.changedAt || log.amendedAt).format('HH:mm:ss DD/MM/YYYY') : '—'}
                              </Text>
                            </div>

                            {(log.changeReason || log.reason) && (
                              <Paragraph style={{ margin: '6px 0', color: '#1e40af', backgroundColor: '#eff6ff', padding: '6px 10px', borderRadius: 4 }}>
                                <strong>Lý do điều chỉnh:</strong> {log.changeReason || log.reason}
                              </Paragraph>
                            )}

                            {log.before && (
                              <div style={{ marginTop: 8, padding: 8, backgroundColor: '#fef2f2', borderRadius: 6 }}>
                                <Text danger strong style={{ fontSize: 12 }}>
                                  [TRƯỚC KHI THAY ĐỔI]
                                </Text>
                                <ul style={{ paddingLeft: 20, margin: '4px 0' }}>
                                  {(Array.isArray(log.before.items)
                                    ? log.before.items
                                    : typeof log.before.items === 'string'
                                      ? JSON.parse(log.before.items)
                                      : []
                                  ).map((item, i) => {
                                    const freqText = item.frequency != null && item.frequency !== ''
                                      ? typeof item.frequency === 'number' || !isNaN(Number(item.frequency))
                                        ? `${item.frequency} lần/ngày`
                                        : item.frequency
                                      : ''
                                    return (
                                      <li key={i}>
                                        <strong>{item.medicineName || getMedicineName(item.medicineId)}</strong> — SL: {item.quantity} | Liều: {item.dosage || 'Theo chỉ định'} {freqText ? `(${freqText})` : ''}
                                      </li>
                                    )
                                  })}
                                </ul>
                              </div>
                            )}

                            <div style={{ marginTop: 8, padding: 8, backgroundColor: '#f0fdf4', borderRadius: 6 }}>
                              <Text type="success" strong style={{ fontSize: 12 }}>
                                {log.before ? '[SAU KHI THAY ĐỔI]' : '[DANH SÁCH THUỐC KÊ]'}
                              </Text>
                              <ul style={{ paddingLeft: 20, margin: '4px 0' }}>
                                {(Array.isArray(log.after?.items)
                                  ? log.after.items
                                  : typeof log.after?.items === 'string'
                                    ? JSON.parse(log.after.items)
                                    : items
                                ).map((item, i) => {
                                  const freqText = item.frequency != null && item.frequency !== ''
                                    ? typeof item.frequency === 'number' || !isNaN(Number(item.frequency))
                                      ? `${item.frequency} lần/ngày`
                                      : item.frequency
                                    : ''
                                  return (
                                    <li key={i}>
                                      <strong>{item.medicineName || getMedicineName(item.medicineId)}</strong> — SL: {item.quantity} | Liều: {item.dosage || 'Theo chỉ định'} {freqText ? `(${freqText})` : ''}
                                    </li>
                                  )
                                })}
                              </ul>
                            </div>
                          </Card>
                        ),
                      }
                    })}
                  />
                )}
              </div>
            ),
          },
        ]}
      />
    </Modal>
  )
}

export default PrescriptionDetailModal
