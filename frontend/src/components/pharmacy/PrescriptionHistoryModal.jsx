import React from 'react'
import { Modal, Timeline, Tag, Card, Typography, Button, Empty, Space } from 'antd'
import { HistoryOutlined, EditOutlined, PlusCircleOutlined, DeleteOutlined, InfoCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'

const { Text, Paragraph } = Typography

function PrescriptionHistoryModal({
  open,
  onClose,
  prescriptionCode,
  historyLogs = [],
  medicines = [],
}) {
  const getMedicineName = (id) => {
    const found = medicines.find((m) => String(m.id) === String(id))
    return found?.name || found?.medicineName || id
  }

  const parseItems = (items) => {
    if (!items) return []
    try {
      return typeof items === 'string' ? JSON.parse(items) : items
    } catch {
      return []
    }
  }

  const renderDetailedDiff = (beforeRaw, afterRaw) => {
    const beforeList = parseItems(beforeRaw)
    const afterList = parseItems(afterRaw)

    if (!beforeList.length && !afterList.length) {
      return <Text type="secondary">(Không có danh sách thuốc)</Text>
    }

    if (!beforeList.length) {
      return (
        <div style={{ marginTop: 8, padding: 10, backgroundColor: '#F0FDF4', borderRadius: 6, border: '1px solid #BBF7D0' }}>
          <Text type="success" strong style={{ fontSize: 12 }}>
            [DANH SÁCH THUỐC BAN ĐẦU KÊ]
          </Text>
          <ul style={{ paddingLeft: 20, margin: '6px 0 0 0' }}>
            {afterList.map((item, idx) => (
              <li key={idx}>
                <strong>{getMedicineName(item.medicineId)}</strong> — Số lượng: <strong>{item.quantity}</strong> | Liều: {item.dosage || 'Theo hướng dẫn bác sĩ'}
              </li>
            ))}
          </ul>
        </div>
      )
    }

    const afterMedIds = new Set(afterList.map((i) => String(i.medicineId)))
    const beforeMedIds = new Set(beforeList.map((i) => String(i.medicineId)))

    const removedItems = beforeList.filter((i) => !afterMedIds.has(String(i.medicineId)))
    const addedItems = afterList.filter((i) => !beforeMedIds.has(String(i.medicineId)))

    return (
      <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {removedItems.length > 0 && (
          <div style={{ padding: 8, backgroundColor: '#FEF2F2', borderRadius: 6, border: '1px solid #FCA5A5' }}>
            <Text danger strong style={{ fontSize: 12 }}>
              <DeleteOutlined /> THUỐC BÁC SĨ ĐÃ BỎ KHỎI ĐƠN ({removedItems.length}):
            </Text>
            <ul style={{ paddingLeft: 20, margin: '4px 0 0 0' }}>
              {removedItems.map((item, idx) => (
                <li key={idx} style={{ textDecoration: 'line-through', color: '#991B1B' }}>
                  <strong>{getMedicineName(item.medicineId)}</strong> (Cũ: SL {item.quantity}, {item.dosage})
                </li>
              ))}
            </ul>
          </div>
        )}

        {addedItems.length > 0 && (
          <div style={{ padding: 8, backgroundColor: '#F0FDF4', borderRadius: 6, border: '1px solid #86EFAC' }}>
            <Text type="success" strong style={{ fontSize: 12 }}>
              <PlusCircleOutlined /> THUỐC BÁC SĨ BỔ SUNG MỚI ({addedItems.length}):
            </Text>
            <ul style={{ paddingLeft: 20, margin: '4px 0 0 0' }}>
              {addedItems.map((item, idx) => (
                <li key={idx} style={{ color: '#166534' }}>
                  <strong>{getMedicineName(item.medicineId)}</strong> — SL: {item.quantity} | Liều: {item.dosage}
                </li>
              ))}
            </ul>
          </div>
        )}

        <div style={{ padding: 8, backgroundColor: '#EFF6FF', borderRadius: 6, border: '1px solid #93C5FD' }}>
          <Text style={{ fontSize: 12, color: '#1E40AF' }} strong>
            <InfoCircleOutlined /> DANH SÁCH THUỐC SAU KHI ĐIỀU CHỈNH ({afterList.length} loại):
          </Text>
          <ul style={{ paddingLeft: 20, margin: '4px 0 0 0' }}>
            {afterList.map((item, idx) => {
              const prevMatch = beforeList.find((b) => String(b.medicineId) === String(item.medicineId))
              const isChanged = prevMatch && (prevMatch.quantity !== item.quantity || prevMatch.dosage !== item.dosage)
              return (
                <li key={idx}>
                  <strong>{getMedicineName(item.medicineId)}</strong> — SL: <strong>{item.quantity}</strong> | Liều: {item.dosage}
                  {isChanged && (
                    <Tag color="blue" style={{ marginLeft: 8 }}>
                      Đã chỉnh liều/SL (Cũ: SL {prevMatch.quantity}, {prevMatch.dosage})
                    </Tag>
                  )}
                </li>
              )
            })}
          </ul>
        </div>
      </div>
    )
  }

  return (
    <Modal
      open={open}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <HistoryOutlined style={{ color: '#2563EB', fontSize: 20 }} />
          <span style={{ fontSize: 17, fontWeight: 600 }}>Lịch Sử Lưu Vết Điều Chỉnh Đơn Thuốc {prescriptionCode}</span>
        </div>
      }
      onCancel={onClose}
      footer={<Button onClick={onClose}>Đóng nhật ký</Button>}
      width={780}
    >
      {!historyLogs || historyLogs.length === 0 ? (
        <Empty description="Chưa có lịch sử thay đổi cho đơn thuốc này." />
      ) : (
        <Timeline
          mode="left"
          style={{ marginTop: 16 }}
          items={historyLogs.map((log, index) => ({
            color: log.action === 'CREATE' ? 'green' : 'blue',
            dot: log.action === 'CREATE' ? <PlusCircleOutlined /> : <EditOutlined />,
            children: (
              <Card size="small" style={{ marginBottom: 16, borderRadius: 8, borderColor: '#CBD5E1' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <Space style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <Tag color={log.action === 'CREATE' ? 'green' : 'blue'} style={{ fontWeight: 600 }}>
                      {log.action === 'CREATE' ? 'KHỞI TẠO ĐƠN THUỐC' : `ĐIỀU CHỈNH LẦN ${historyLogs.length - index}`}
                    </Tag>
                    <Text strong style={{ color: '#0F172A' }}>{log.changedBy}</Text>
                  </Space>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {dayjs(log.changedAt).format('HH:mm:ss DD/MM/YYYY')}
                  </Text>
                </div>

                {log.changeReason && (
                  <Paragraph style={{ margin: '6px 0', padding: '6px 10px', backgroundColor: '#F1F5F9', borderRadius: 4, color: '#1E40AF' }}>
                    <strong>Lý do điều chỉnh bác sĩ nhập:</strong> <em>"{log.changeReason}"</em>
                  </Paragraph>
                )}

                {renderDetailedDiff(log.before?.items, log.after?.items)}
              </Card>
            ),
          }))}
        />
      )}
    </Modal>
  )
}

export default PrescriptionHistoryModal
