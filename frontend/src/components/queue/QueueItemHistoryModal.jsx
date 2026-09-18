import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Descriptions,
  Empty,
  Modal,
  Space,
  Spin,
  Table,
  Tag,
  Timeline,
  Typography,
} from 'antd'
import {
  ClockCircleOutlined,
  HistoryOutlined,
  ReloadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import queueApi from '../../api/queueApi.js'
import {
  QUEUE_ACTION_META,
  formatQueueActionVi,
  formatQueueStatusVi,
} from '../../utils/queueDeferRecallHelpers.js'

const { Text, Title } = Typography

function QueueItemHistoryModal({ open, item, onClose }) {
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)

  const loadHistory = useCallback(async () => {
    if (!item?.id) return
    setLoading(true)
    setErrorMessage(null)
    try {
      const res = await queueApi.getHistory(item.id)
      const list = Array.isArray(res?.data) ? res.data : []
      setHistory(list)
    } catch (err) {
      setErrorMessage('Không thể tải nhật ký lịch sử hàng đợi.')
      setHistory([])
    } finally {
      setLoading(false)
    }
  }, [item?.id])

  useEffect(() => {
    if (open && item?.id) {
      loadHistory()
    } else {
      setHistory([])
      setErrorMessage(null)
    }
  }, [open, item?.id, loadHistory])

  if (!item) return null

  const columns = [
    {
      title: 'Thời điểm',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 160,
      render: (val) => (
        <Space direction="vertical" size={2}>
          <Text style={{ fontSize: 13, fontWeight: 500 }}>
            {val ? dayjs(val).format('HH:mm:ss') : '—'}
          </Text>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {val ? dayjs(val).format('DD/MM/YYYY') : ''}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Hành động',
      dataIndex: 'action',
      key: 'action',
      width: 170,
      render: (val) => {
        const meta = QUEUE_ACTION_META[String(val || '').toUpperCase()]
        return (
          <Tag color={meta?.color || 'default'} style={{ fontWeight: 600, padding: '2px 8px' }}>
            {formatQueueActionVi(val)}
          </Tag>
        )
      },
    },
    {
      title: 'Người thao tác',
      dataIndex: 'operatorName',
      key: 'operatorName',
      width: 150,
      render: (val) => (
        <Space size={4}>
          <UserOutlined style={{ fontSize: 12, color: '#64748b' }} />
          <Text style={{ fontSize: 13 }}>{val || 'Hệ thống'}</Text>
        </Space>
      ),
    },
    {
      title: 'Số lần gọi',
      dataIndex: 'callCount',
      key: 'callCount',
      width: 110,
      align: 'center',
      render: (val) => {
        const count = Number(val) || 0
        if (count <= 0) return <Text type="secondary">—</Text>
        return <Tag color="orange" style={{ fontWeight: 600 }}>{count} lần</Tag>
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: (val) => (
        <Text style={{ fontSize: 13 }}>
          {formatQueueStatusVi(val)}
        </Text>
      ),
    },
    {
      title: 'Lý do / Ghi chú',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
      render: (val) => val ? <Text style={{ fontSize: 13 }}>{val}</Text> : <Text type="secondary">—</Text>,
    },
  ]

  return (
    <Modal
      open={open}
      title={
        <Space align="center">
          <HistoryOutlined style={{ color: '#2563eb', fontSize: 20 }} />
          <span>Nhật ký & Lịch sử luân chuyển hàng đợi (TC-04)</span>
        </Space>
      }
      onCancel={onClose}
      footer={[
        <Button key="refresh" icon={<ReloadOutlined />} onClick={loadHistory} loading={loading}>
          Làm mới
        </Button>,
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
      width={880}
      destroyOnClose
    >
      <div style={{ marginTop: 12 }}>
        <div
          style={{
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: 8,
            padding: '12px 16px',
            marginBottom: 16,
          }}
        >
          <Descriptions size="small" column={{ xs: 1, sm: 2, md: 4 }}>
            <Descriptions.Item label="Bệnh nhân">
              <Text strong>{item.patientName || 'Bệnh nhân'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Số thứ tự">
              <Tag color="gold" style={{ fontWeight: 700 }}>
                STT #{String(item.queueNumber || 1).padStart(2, '0')}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Bác sĩ">
              <Text strong>{item.doctorName || '—'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Tổng số lần gọi">
              <Tag color={Number(item.callCount) > 0 ? 'orange' : 'default'} style={{ fontWeight: 600 }}>
                {Number(item.callCount) > 0 ? `${item.callCount} lần` : 'Chưa gọi'}
              </Tag>
            </Descriptions.Item>
          </Descriptions>
        </div>

        {errorMessage && (
          <Alert type="error" showIcon message={errorMessage} style={{ marginBottom: 16 }} />
        )}

        <Spin spinning={loading}>
          {history.length === 0 && !loading ? (
            <Empty
              description="Chưa có bản ghi lịch sử luân chuyển nào cho lượt khám này."
              style={{ padding: '32px 0' }}
            />
          ) : (
            <Table
              dataSource={history}
              columns={columns}
              rowKey={(r, idx) => r.id || `${r.timestamp}-${idx}`}
              pagination={false}
              size="middle"
              bordered
            />
          )}
        </Spin>
      </div>
    </Modal>
  )
}

export default QueueItemHistoryModal
