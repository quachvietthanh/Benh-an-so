import React, { useEffect, useState } from 'react'
import {
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Modal,
  Row,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  CheckCircleOutlined,
  FieldTimeOutlined,
  HeartOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import vitalSignApi from '../../api/vitalSignApi'
import {
  ABNORMAL_FLAGS_META,
  formatBloodPressure,
  getBmiCategory,
} from '../../utils/vitalSignHelpers'

const { Text, Title } = Typography

export function VitalSignHistoryModal({
  open,
  onClose,
  patientId,
  patientName,
  patientCode,
}) {
  const [loading, setLoading] = useState(false)
  const [history, setHistory] = useState([])
  const [error, setError] = useState('')

  const fetchHistory = async () => {
    if (!patientId) return
    setLoading(true)
    setError('')
    try {
      const res = await vitalSignApi.getPatientHistory(patientId)
      const data = Array.isArray(res?.data) ? res.data : []
      // Sắp xếp giảm dần theo thời gian ghi nhận (mới nhất trước)
      data.sort((a, b) => new Date(b.recordedAt || 0) - new Date(a.recordedAt || 0))
      setHistory(data)
    } catch (err) {
      console.error('Error loading vital sign history:', err)
      setError('Không thể tải lịch sử chỉ số sinh tồn. Vui lòng thử lại.')
      setHistory([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (open && patientId) {
      fetchHistory()
    }
  }, [open, patientId])

  const latest = history[0] || null

  const columns = [
    {
      title: 'Thời gian đo',
      key: 'recordedAt',
      width: 140,
      render: (_, r) => (
        <div>
          <Text strong style={{ fontSize: 13, color: '#0f172a', whiteSpace: 'nowrap' }}>
            {r.recordedAt ? dayjs(r.recordedAt).format('DD/MM/YYYY') : '—'}
          </Text>
          <div style={{ fontSize: 11, color: '#64748b', whiteSpace: 'nowrap' }}>
            {r.recordedAt ? dayjs(r.recordedAt).format('HH:mm') : ''}
          </div>
        </div>
      ),
    },
    {
      title: 'Mạch',
      dataIndex: 'pulse',
      key: 'pulse',
      width: 95,
      align: 'center',
      render: (val, r) => {
        if (val == null) return '—'
        const isAbnormal = r.abnormalFlags?.includes('TACHYCARDIA') || r.abnormalFlags?.includes('BRADYCARDIA')
        return (
          <Text strong style={{ color: isAbnormal ? '#dc2626' : '#0f172a', whiteSpace: 'nowrap' }}>
            {val} <span style={{ fontSize: 11, fontWeight: 400, color: '#64748b' }}>l/p</span>
          </Text>
        )
      },
    },
    {
      title: 'Huyết áp',
      key: 'bp',
      width: 135,
      align: 'center',
      render: (_, r) => {
        const str = formatBloodPressure(r.bloodPressureSystolic, r.bloodPressureDiastolic)
        const isAbnormal = r.abnormalFlags?.includes('HYPERTENSION') || r.abnormalFlags?.includes('HYPOTENSION')
        return (
          <Text strong style={{ color: isAbnormal ? '#dc2626' : '#0f172a', whiteSpace: 'nowrap' }}>
            {str}
          </Text>
        )
      },
    },
    {
      title: 'Nhiệt độ',
      dataIndex: 'temperature',
      key: 'temperature',
      width: 100,
      align: 'center',
      render: (val, r) => {
        if (val == null) return '—'
        const num = parseFloat(val)
        const isAbnormal = r.abnormalFlags?.includes('FEVER') || r.abnormalFlags?.includes('HYPOTHERMIA')
        return (
          <Text strong style={{ color: isAbnormal ? '#dc2626' : '#0f172a', whiteSpace: 'nowrap' }}>
            {num.toFixed(1)} <span style={{ fontSize: 11, fontWeight: 400, color: '#64748b' }}>°C</span>
          </Text>
        )
      },
    },
    {
      title: 'Nhịp thở',
      dataIndex: 'respiratoryRate',
      key: 'respiratoryRate',
      width: 95,
      align: 'center',
      render: (val, r) => {
        if (val == null) return '—'
        const isAbnormal = r.abnormalFlags?.includes('TACHYPNEA') || r.abnormalFlags?.includes('BRADYPNEA')
        return (
          <Text strong style={{ color: isAbnormal ? '#dc2626' : '#0f172a', whiteSpace: 'nowrap' }}>
            {val} <span style={{ fontSize: 11, fontWeight: 400, color: '#64748b' }}>l/p</span>
          </Text>
        )
      },
    },
    {
      title: 'SpO2',
      dataIndex: 'spo2',
      key: 'spo2',
      width: 85,
      align: 'center',
      render: (val, r) => {
        if (val == null) return '—'
        const isAbnormal = r.abnormalFlags?.includes('HYPOXEMIA')
        return (
          <Text strong style={{ color: isAbnormal ? '#dc2626' : '#0f172a', whiteSpace: 'nowrap' }}>
            {val}%
          </Text>
        )
      },
    },
    {
      title: 'Cân / Cao (BMI)',
      key: 'body',
      width: 165,
      align: 'center',
      render: (_, r) => {
        const bmi = r.bmi != null ? parseFloat(r.bmi).toFixed(1) : null
        const bmiCat = bmi ? getBmiCategory(bmi) : null
        return (
          <div style={{ fontSize: 12 }}>
            <div style={{ whiteSpace: 'nowrap' }}>
              {r.weight ? `${r.weight} kg` : '—'} / {r.height ? `${r.height} cm` : '—'}
            </div>
            {bmi && (
              <Tag color={bmiCat?.tone || 'default'} style={{ margin: '3px 0 0', fontSize: 11, whiteSpace: 'nowrap' }}>
                BMI: {bmi} ({bmiCat?.label})
              </Tag>
            )}
          </div>
        )
      },
    },
    {
      title: 'Đánh giá & Cảnh báo',
      key: 'abnormal',
      width: 220,
      render: (_, r) => {
        const flags = r.abnormalFlags || []
        if (!r.abnormal && flags.length === 0) {
          return (
            <Tag color="success" icon={<CheckCircleOutlined />} style={{ whiteSpace: 'nowrap' }}>
              Bình thường
            </Tag>
          )
        }
        return (
          <Space wrap size={[4, 4]}>
            {flags.map((flag) => {
              const meta = ABNORMAL_FLAGS_META[flag] || { label: flag, color: 'red' }
              return (
                <Tooltip key={flag} title={meta.description}>
                  <Tag color={meta.color} icon={<WarningOutlined />} style={{ fontWeight: 600, whiteSpace: 'nowrap' }}>
                    {meta.label}
                  </Tag>
                </Tooltip>
              )
            })}
          </Space>
        )
      },
    },
    {
      title: 'Ghi chú',
      dataIndex: 'note',
      key: 'note',
      width: 180,
      ellipsis: { tooltip: true },
      render: (val) => val || <span style={{ color: '#94a3b8' }}>—</span>,
    },
  ]

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <HistoryOutlined style={{ color: '#0284c7', fontSize: 20 }} />
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              Diễn tiến chỉ số sinh tồn qua các lượt khám
            </div>
            <div style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
              Bệnh nhân: <strong style={{ color: '#1e293b' }}>{patientName || '—'}</strong>
              {patientCode && ` (${patientCode})`}
            </div>
          </div>
        </div>
      }
      width={1200}
      style={{ top: 24, maxWidth: '96vw' }}
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
      {loading && history.length === 0 ? (
        <div style={{ padding: '40px 0', textAlign: 'center' }}>
          <Spin size="large" tip="Đang tải lịch sử chỉ số sinh tồn..." />
        </div>
      ) : error ? (
        <div style={{ padding: '24px 0', textAlign: 'center', color: '#dc2626' }}>{error}</div>
      ) : history.length === 0 ? (
        <div style={{ padding: '40px 0', textAlign: 'center' }}>
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Bệnh nhân chưa có bản ghi chỉ số sinh tồn nào ở các lượt khám trước."
          />
        </div>
      ) : (
        <div>
          {latest && (
            <Card
              size="small"
              style={{
                marginBottom: 16,
                borderRadius: 8,
                borderColor: latest.abnormal ? '#fca5a5' : '#bbf7d0',
                backgroundColor: latest.abnormal ? '#fff5f5' : '#f0fdf4',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                <div>
                  <Text strong style={{ fontSize: 13, color: '#0f172a' }}>
                    Lần đo gần nhất: {latest.recordedAt ? dayjs(latest.recordedAt).format('HH:mm DD/MM/YYYY') : '—'}
                  </Text>
                  {latest.abnormal && (
                    <span style={{ marginLeft: 8 }}>
                      <Tag color="error" icon={<WarningOutlined />}>
                        Phát hiện {latest.abnormalFlags?.length || 1} chỉ số bất thường
                      </Tag>
                    </span>
                  )}
                </div>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  Tổng số lần đã ghi nhận: <strong>{history.length}</strong> lần khám
                </Text>
              </div>
            </Card>
          )}

          <Table
            dataSource={history}
            columns={columns}
            rowKey={(r, idx) => r.id || idx}
            pagination={{ pageSize: 6, showTotal: (total) => `Tổng cộng ${total} lần đo` }}
            size="middle"
            bordered
            scroll={{ x: 1150 }}
          />
        </div>
      )}
    </Modal>
  )
}

export default VitalSignHistoryModal
