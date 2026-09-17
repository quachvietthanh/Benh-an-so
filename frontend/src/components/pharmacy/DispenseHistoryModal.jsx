import React, { useEffect, useState } from 'react'
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
  Spin,
  Table,
  Tag,
  Timeline,
  Typography,
  message,
} from 'antd'
import {
  ClockCircleOutlined,
  HistoryOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import prescriptionDispenseApi from '../../api/prescriptionDispenseApi.js'
import { getApiErrorMessage } from '../../utils/apiError.js'

const { Text, Title } = Typography

/**
 * DispenseHistoryModal — Hiển thị lịch sử tất cả các lần cấp phát của đơn thuốc (NCL-06-CN-008)
 *
 * Hiển thị đầy đủ nhiều lần cấp phát (không chỉ lần gần nhất),
 * mỗi dòng thể hiện thời điểm, người cấp, tên thuốc, số lượng thực cấp, số lô xuất kho.
 */
function DispenseHistoryModal({ open, onClose, prescription }) {
  const [loading, setLoading] = useState(false)
  const [historyItems, setHistoryItems] = useState([])
  const [errorMessage, setErrorMessage] = useState('')

  const rxId = prescription?.id
  const rxCode = prescription?.prescriptionCode || prescription?.id || '—'

  const fetchHistory = () => {
    if (!rxId) return
    setLoading(true)
    setErrorMessage('')

    prescriptionDispenseApi
      .getHistory(rxId)
      .then((res) => {
        const data = res?.data
        const list = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : []
        // Sắp xếp giảm dần theo thời gian cấp phát (mới nhất lên đầu)
        list.sort((a, b) =>
          String(b.dispensedAt || '').localeCompare(String(a.dispensedAt || ''))
        )
        setHistoryItems(list)
      })
      .catch((err) => {
        console.warn('[DispenseHistoryModal] Lỗi lấy lịch sử cấp phát:', err)
        const msg = getApiErrorMessage(err, 'Không thể tải lịch sử cấp phát của đơn thuốc này.')
        setErrorMessage(msg)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (open && rxId) {
      fetchHistory()
    } else {
      setHistoryItems([])
      setErrorMessage('')
    }
  }, [open, rxId])

  // Gom nhóm các lần cấp phát theo thời điểm (round of dispensing)
  const groupedEvents = React.useMemo(() => {
    const groups = new Map()
    historyItems.forEach((item) => {
      // Key theo thời điểm cấp phát và người cấp
      const timeKey = item.dispensedAt ? dayjs(item.dispensedAt).format('YYYY-MM-DD HH:mm:ss') : 'Không rõ'
      const key = `${timeKey}_${item.dispenserName || item.dispensedBy || ''}`
      if (!groups.has(key)) {
        groups.set(key, {
          dispensedAt: item.dispensedAt,
          dispenserName: item.dispenserName || item.dispensedBy || 'Dược sĩ',
          items: [],
        })
      }
      groups.get(key).items.push(item)
    })
    return Array.from(groups.values())
  }, [historyItems])

  const columns = [
    {
      title: 'STT',
      key: 'stt',
      width: 50,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Thời điểm cấp',
      dataIndex: 'dispensedAt',
      key: 'dispensedAt',
      width: 155,
      render: (val) => (
        <Space direction="vertical" size={0}>
          <Text strong>{val && dayjs(val).isValid() ? dayjs(val).format('HH:mm DD/MM/YYYY') : '—'}</Text>
          <Text orientation="left" type="secondary" style={{ fontSize: 11 }}>
            {val && dayjs(val).isValid() ? dayjs(val).fromNow?.() : ''}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'dispenserName',
      key: 'dispenserName',
      width: 150,
      render: (val, item) => (
        <Space size={4}>
          <UserOutlined style={{ color: '#64748b' }} />
          <span>{val || item.dispensedBy || 'Dược sĩ phụ trách'}</span>
        </Space>
      ),
    },
    {
      title: 'Thuốc đã cấp',
      key: 'medicineName',
      render: (_, item) => (
        <div>
          <Text strong style={{ color: '#1e40af' }}>
            {item.medicineName || `Mã ${item.medicineId}`}
          </Text>
          {item.medicineCode && (
            <div style={{ fontSize: 12, color: '#64748b' }}>Mã thuốc: {item.medicineCode}</div>
          )}
        </div>
      ),
    },
    {
      title: 'Số lượng cấp',
      dataIndex: 'dispensedQuantity',
      key: 'dispensedQuantity',
      width: 110,
      align: 'center',
      render: (val) => (
        <Tag color="green" style={{ fontSize: 13, fontWeight: 700, padding: '2px 8px' }}>
          +{val}
        </Tag>
      ),
    },
    {
      title: 'Lô thuốc xuất',
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      width: 140,
      render: (batch, item) => (
        <Space direction="vertical" size={1}>
          <Tag color="blue" style={{ margin: 0 }}>
            {batch || item.medicineBatchId || 'Lô FEFO'}
          </Tag>
          {item.expiryDate && (
            <span style={{ fontSize: 11, color: '#64748b' }}>
              HSD: {dayjs(item.expiryDate).format('DD/MM/YYYY')}
            </span>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Modal
      title={
        <Space align="center">
          <HistoryOutlined style={{ color: '#1677ff', fontSize: 20 }} />
          <span style={{ fontSize: 17, fontWeight: 700 }}>Lịch sử cấp phát thuốc</span>
          <Tag color="blue" style={{ marginLeft: 8 }}>
            Đơn {rxCode}
          </Tag>
        </Space>
      }
      open={open}
      onCancel={onClose}
      width={880}
      footer={[
        <Button key="refresh" icon={<ReloadOutlined />} onClick={fetchHistory} loading={loading}>
          Làm mới
        </Button>,
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
      destroyOnClose
    >
      <div style={{ marginTop: 12 }}>
        {errorMessage && (
          <Alert
            type="error"
            showIcon
            message="Lỗi tải dữ liệu"
            description={errorMessage}
            style={{ marginBottom: 16 }}
          />
        )}

        {/* Tổng kết tiến trình cấp phát */}
        {groupedEvents.length > 0 && (
          <Card size="small" style={{ marginBottom: 16, backgroundColor: '#f8fafc', borderRadius: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
              <span>
                Tổng số đợt cấp phát: <strong>{groupedEvents.length} lần</strong>
              </span>
              <span>
                Tổng số lượt xuất kho: <strong>{historyItems.length} lượt thuốc/lô</strong>
              </span>
              <span>
                Trạng thái đơn: <Tag color={prescription?.status === 'DISPENSED' ? 'green' : 'gold'}>
                  {prescription?.status === 'DISPENSED' ? 'Đã cấp phát đầy đủ' : 'Đã cấp phát một phần'}
                </Tag>
              </span>
            </div>
          </Card>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Spin tip="Đang tải lịch sử cấp phát..." />
          </div>
        ) : historyItems.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Chưa có lượt cấp phát nào được ghi nhận cho đơn thuốc này."
          />
        ) : (
          <Table
            rowKey={(r, idx) => r.id || `${r.prescriptionItemId}-${r.dispensedAt}-${idx}`}
            columns={columns}
            dataSource={historyItems}
            pagination={{ pageSize: 8, hideOnSinglePage: true }}
            size="small"
          />
        )}
      </div>
    </Modal>
  )
}

export default DispenseHistoryModal
