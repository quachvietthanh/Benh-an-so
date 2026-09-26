import React from 'react'
import { Modal, Timeline, Tag, Card, Typography, List, Button, Empty, Space } from 'antd'
import { HistoryOutlined, FileTextOutlined, EditOutlined, PlusCircleOutlined } from '@ant-design/icons'
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

  const renderItems = (items = []) => {
    if (!items || !items.length) return <Text type="secondary">(Không có thuốc)</Text>
    return (
      <ul style={{ paddingLeft: 20, margin: '4px 0' }}>
        {items.map((item, idx) => (
          <li key={idx}>
            <strong>{getMedicineName(item.medicineId)}</strong> — SL: {item.quantity} | Liều: {item.dosage || 'Theo chỉ định'}
          </li>
        ))}
      </ul>
    )
  }

  return (
    <Modal
      open={open}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <HistoryOutlined style={{ color: '#2563EB' }} />
          <span>Lịch Sử Thay Đổi Đơn Thuốc Điện Tử: <Text code strong style={{ color: '#2563eb' }}>{prescriptionCode}</Text></span>
        </div>
      }
      onCancel={onClose}
      footer={<Button onClick={onClose}>Đóng</Button>}
      width={720}
    >
      <div style={{ marginBottom: 12, padding: '6px 12px', backgroundColor: '#eff6ff', borderRadius: 6, border: '1px solid #bfdbfe', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text strong style={{ color: '#1e40af', fontSize: 13 }}>
          Mã đơn thuốc điện tử: <Text code>{prescriptionCode}</Text>
        </Text>
        <Tag color="cyan" style={{ margin: 0 }}>Định danh cố định bất biến</Tag>
      </div>

      {!historyLogs || historyLogs.length === 0 ? (
        <Empty description="Chưa có lịch sử điều chỉnh cho đơn thuốc này" />
      ) : (
        <Timeline
          mode="left"
          items={historyLogs.map((log) => ({
            color: log.action === 'CREATE' ? 'green' : 'blue',
            dot: log.action === 'CREATE' ? <PlusCircleOutlined /> : <EditOutlined />,
            children: (
              <Card size="small" style={{ marginBottom: 12, borderRadius: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <Space style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <Tag color={log.action === 'CREATE' ? 'green' : 'blue'}>
                      {log.action === 'CREATE' ? 'KHỞI TẠO ĐƠN' : 'ĐIỀU CHỈNH ĐƠN'}
                    </Tag>
                    <Text strong>{log.changedBy}</Text>
                  </Space>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {dayjs(log.changedAt).format('HH:mm:ss DD/MM/YYYY')}
                  </Text>
                </div>

                {log.changeReason && (
                  <Paragraph style={{ margin: '4px 0', color: '#1E40AF', fontStyle: 'italic' }}>
                    <strong>Lý do điều chỉnh:</strong> {log.changeReason}
                  </Paragraph>
                )}

                {log.before && (
                  <div style={{ marginTop: 8, padding: 8, backgroundColor: '#FEF2F2', borderRadius: 6 }}>
                    <Text danger strong style={{ fontSize: 12 }}>
                      [TRƯỚC KHI THAY ĐỔI]
                    </Text>
                    {renderItems(typeof log.before.items === 'string' ? JSON.parse(log.before.items) : log.before.items)}
                  </div>
                )}

                <div style={{ marginTop: 8, padding: 8, backgroundColor: '#F0FDF4', borderRadius: 6 }}>
                  <Text type="success" strong style={{ fontSize: 12 }}>
                    {log.before ? '[SAU KHI THAY ĐỔI]' : '[DANH SÁCH THUỐC KÊ]'}
                  </Text>
                  {renderItems(typeof log.after?.items === 'string' ? JSON.parse(log.after.items) : log.after?.items)}
                </div>
              </Card>
            ),
          }))}
        />
      )}
    </Modal>
  )
}

export default PrescriptionHistoryModal
