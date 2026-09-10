import React, { useEffect, useState, useCallback } from 'react'
import {
  Modal,
  Table,
  Tag,
  Typography,
  Space,
  Button,
  DatePicker,
  Empty,
  Switch,
  Alert,
} from 'antd'
import {
  HistoryOutlined,
  ReloadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import pharmacyApi from '../../api/pharmacyApi.js'
import {
  getStoredAllergyWarningLogs,
  saveStoredAllergyWarningLogs,
} from '../../utils/storageHelpers.js'

const { Text } = Typography
const { RangePicker } = DatePicker

function renderSeverityTag(severity) {
  if (!severity) return null
  const sev = String(severity).toUpperCase()
  switch (sev) {
    case 'ANAPHYLAXIS':
      return <Tag color="#b91c1c" style={{ fontWeight: 700 }}>Sốc phản vệ</Tag>
    case 'SEVERE':
      return <Tag color="red" style={{ fontWeight: 700 }}>Nghiêm trọng</Tag>
    case 'MODERATE':
      return <Tag color="orange" style={{ fontWeight: 600 }}>Trung bình</Tag>
    case 'MILD':
      return <Tag color="gold" style={{ fontWeight: 600 }}>Nhẹ</Tag>
    default:
      return <Tag color="default">{severity}</Tag>
  }
}

/**
 * Modal to view prescription allergy override warning logs (NCL-05-CN-004, TC-05)
 */
function PrescriptionAllergyWarningLogsModal({
  open,
  onClose,
  patientId = null,
  defaultPatientId = null,
  patientName = '',
}) {
  const activePatientId = patientId || defaultPatientId
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(false)
  const [filterOnlyCurrentPatient, setFilterOnlyCurrentPatient] = useState(true)
  const [dateRange, setDateRange] = useState(null)
  const [permissionNotice, setPermissionNotice] = useState(null)

  const fetchLogs = useCallback(async () => {
    setLoading(true)
    try {
      const localLogs = getStoredAllergyWarningLogs()
      let serverLogs = []

      try {
        const params = {
          page: 0,
          size: 100,
        }
        if (filterOnlyCurrentPatient && activePatientId) {
          params.patientId = activePatientId
        }
        if (dateRange && dateRange[0] && dateRange[1]) {
          params.from = dateRange[0].startOf('day').toISOString()
          params.to = dateRange[1].endOf('day').toISOString()
        }

        const res = await pharmacyApi.getAllergyWarningLogs(params)
        const data = res?.data || {}
        serverLogs = data.content || (Array.isArray(data) ? data : [])
        setPermissionNotice(null)
      } catch (err) {
        const status = err?.response?.status
        if (status === 403) {
          setPermissionNotice(
            'Tài khoản Bác sĩ đang hiển thị dữ liệu nhật ký lưu vết từ phiên làm việc và bộ nhớ cục bộ. (Quyền PRESCRIPTION_ALLERGY_WARNING_VIEW trên máy chủ dành cho Quản trị viên).'
          )
        }
      }

      // Merge server logs and local storage logs
      const combinedMap = new Map()
      serverLogs.forEach((item) => {
        const key = item.id || `${item.prescriptionId}_${item.medicineId}_${item.handledAt}`
        combinedMap.set(key, item)
      })
      localLogs.forEach((item) => {
        const key = item.id || `${item.prescriptionId}_${item.medicineId}_${item.handledAt}`
        if (!combinedMap.has(key)) {
          combinedMap.set(key, item)
        }
      })

      let allLogs = Array.from(combinedMap.values())

      // Filter by current patient if toggled
      if (filterOnlyCurrentPatient && (activePatientId || patientName)) {
        const pIdStr = activePatientId ? String(activePatientId).toLowerCase() : ''
        const pNameStr = patientName ? patientName.toLowerCase().trim() : ''

        allLogs = allLogs.filter((item) => {
          const itemPid = String(item.patientId || '').toLowerCase()
          const itemPName = String(item.patientName || '').toLowerCase()

          if (pIdStr && itemPid && (itemPid === pIdStr || itemPid.includes(pIdStr) || pIdStr.includes(itemPid))) {
            return true
          }
          if (pNameStr && itemPName && (itemPName.includes(pNameStr) || pNameStr.includes(itemPName))) {
            return true
          }
          return false
        })
      }

      // Filter by date range if specified
      if (dateRange && dateRange[0] && dateRange[1]) {
        const start = dateRange[0].startOf('day')
        const end = dateRange[1].endOf('day')
        allLogs = allLogs.filter((item) => {
          if (!item.handledAt) return true
          const itemDate = dayjs(item.handledAt)
          return itemDate.isAfter(start) && itemDate.isBefore(end)
        })
      }

      // Sort newest first
      allLogs.sort((a, b) => new Date(b.handledAt || 0) - new Date(a.handledAt || 0))
      setLogs(allLogs)
    } finally {
      setLoading(false)
    }
  }, [activePatientId, patientName, filterOnlyCurrentPatient, dateRange])

  useEffect(() => {
    if (open) {
      fetchLogs()
    }
  }, [open, fetchLogs])



  const columns = [
    {
      title: 'Thời gian',
      dataIndex: 'handledAt',
      key: 'handledAt',
      width: 130,
      render: (t) => (t ? dayjs(t).format('HH:mm DD/MM/YYYY') : '—'),
    },
    {
      title: 'Mã đơn',
      dataIndex: 'prescriptionCode',
      key: 'prescriptionCode',
      width: 125,
      render: (code) => <Text strong style={{ color: '#2563eb' }}>{code || '—'}</Text>,
    },
    {
      title: 'Bác sĩ xử lý',
      dataIndex: 'doctorName',
      key: 'doctorName',
      width: 140,
      render: (name) => (
        <Space size={4}>
          <UserOutlined style={{ color: '#64748b' }} />
          <span>{name || 'Bác sĩ'}</span>
        </Space>
      ),
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      width: 130,
      render: (name) => <Text strong>{name || '—'}</Text>,
    },
    {
      title: 'Thuốc kê trong đơn',
      key: 'medicine',
      width: 180,
      render: (_, r) => (
        <div>
          <Text strong>{r.medicineName || 'Thuốc'}</Text>
          {r.activeIngredient && (
            <div>
              <Text type="secondary" style={{ fontSize: 12 }}>
                Hoạt chất: {r.activeIngredient}
              </Text>
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Dị nguyên trùng',
      dataIndex: 'allergenName',
      key: 'allergenName',
      width: 140,
      render: (name) => <Text strong style={{ color: '#dc2626' }}>{name || '—'}</Text>,
    },
    {
      title: 'Mức độ',
      dataIndex: 'severity',
      key: 'severity',
      width: 110,
      render: (sev) => renderSeverityTag(sev),
    },
    {
      title: 'Lý do bỏ qua cảnh báo',
      dataIndex: 'overrideReason',
      key: 'overrideReason',
      render: (reason) => (
        <div
          style={{
            color: '#1e293b',
            fontSize: 13,
            lineHeight: 1.6,
            fontWeight: 400,
            wordBreak: 'break-word',
          }}
        >
          {reason || '—'}
        </div>
      ),
    },
  ]

  return (
    <Modal
      open={open}
      title={
        <Space align="center" style={{ color: '#dc2626' }}>
          <HistoryOutlined style={{ fontSize: 18 }} />
          <span style={{ fontWeight: 700, fontSize: 16 }}>
            Nhật ký lưu vết vượt qua cảnh báo dị ứng thuốc
          </span>
        </Space>
      }
      onCancel={onClose}
      width={1120}
      footer={[
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
      destroyOnClose
    >

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
          flexWrap: 'wrap',
          gap: 12,
          background: '#f8fafc',
          padding: '10px 14px',
          borderRadius: 8,
          border: '1px solid #e2e8f0',
        }}
      >
        <Space wrap size="middle">
          <RangePicker
            format="DD/MM/YYYY"
            placeholder={['Từ ngày', 'Đến ngày']}
            value={dateRange}
            onChange={(dates) => setDateRange(dates)}
          />

          <Space size={6} align="center">
            <Switch
              checked={filterOnlyCurrentPatient}
              onChange={(checked) => setFilterOnlyCurrentPatient(checked)}
              size="small"
            />
            <span style={{ fontSize: 13, fontWeight: 500, color: '#334155' }}>
              Chỉ lọc theo bệnh nhân này {patientName ? `(${patientName})` : ''}
            </span>
          </Space>

          <Button icon={<ReloadOutlined />} onClick={fetchLogs} loading={loading}>
            Lọc nhật ký
          </Button>
        </Space>
      </div>

      <Table
        dataSource={logs}
        columns={columns}
        rowKey={(r) => r.id || `${r.prescriptionId}_${r.medicineId}_${r.handledAt}`}
        loading={loading}
        pagination={{ pageSize: 8, showTotal: (total) => `Tổng cộng ${total} bản ghi lưu vết` }}
        size="small"
        bordered
        scroll={{ x: 1040 }}
        locale={{
          emptyText: (
            <Empty
              description={
                <div style={{ padding: 12 }}>
                  <div style={{ fontWeight: 600, color: '#475569', marginBottom: 4 }}>
                    Chưa có bản ghi nào về việc bỏ qua cảnh báo dị ứng thuốc
                  </div>
                  <div style={{ fontSize: 12, color: '#94a3b8' }}>
                    {filterOnlyCurrentPatient
                      ? 'Bệnh nhân này chưa có đơn thuốc nào cần vượt qua cảnh báo, hoặc bạn có thể tắt nút "Chỉ lọc theo bệnh nhân này" ở trên để xem lịch sử của toàn phòng khám.'
                      : 'Hệ thống chưa ghi nhận lượt vượt qua cảnh báo dị ứng nào.'}
                  </div>
                </div>
              }
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          ),
        }}
      />
    </Modal>
  )
}

export default PrescriptionAllergyWarningLogsModal
