import React, { useMemo, useState } from 'react'
import { Alert, Button, Card, Input, Space, Table, Tag } from 'antd'
import { DownloadOutlined, SafetyCertificateOutlined, SearchOutlined } from '@ant-design/icons'
import ExportAccessLogReportModal from './ExportAccessLogReportModal.jsx'

export default function AuditLogsReportView({ auditLogs = [], loading = false }) {
  const [searchTerm, setSearchTerm] = useState('')
  const [exportModalOpen, setExportModalOpen] = useState(false)

  const filteredLogs = useMemo(() => {
    if (!searchTerm.trim()) return auditLogs
    const term = searchTerm.toLowerCase()
    return auditLogs.filter((log) => (
      (log.userName && log.userName.toLowerCase().includes(term)) ||
      (log.patientName && log.patientName.toLowerCase().includes(term)) ||
      (log.recordCode && log.recordCode.toLowerCase().includes(term)) ||
      (log.action && log.action.toLowerCase().includes(term))
    ))
  }, [auditLogs, searchTerm])

  return (
    <>
      <Alert
        type="info"
        showIcon
        icon={<SafetyCertificateOutlined style={{ color: '#2563eb', fontSize: 18 }} />}
        style={{ marginBottom: 16, borderRadius: 10, background: '#eff6ff', borderColor: '#bfdbfe' }}
        message={<strong>Báo cáo Giám sát & Nhật ký Truy cập Hồ sơ Bệnh án theo Kỳ</strong>}
        description={(
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <span style={{ fontSize: 13, color: '#1e3a8a' }}>
              Hệ thống cung cấp báo cáo tổng hợp số lượt truy cập bệnh án theo từng tài khoản phục vụ công tác thanh tra, lưu trữ hồ sơ giám sát định kỳ.
            </span>
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={() => setExportModalOpen(true)}
              style={{ backgroundColor: '#1d4ed8' }}
            >
              Xuất tệp báo cáo tổng hợp
            </Button>
          </div>
        )}
      />

      <Card
        style={{ borderRadius: 14, border: '1px solid #f1f5f9' }}
        title="Nhật ký truy cập và giám sát hồ sơ y tế thực tế"
        extra={
          <Input
            placeholder="Tìm kiếm người dùng, bệnh nhân..."
            prefix={<SearchOutlined />}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ width: 300, borderRadius: 8 }}
            allowClear
          />
        }
      >
        <Table
          rowKey="id"
          dataSource={filteredLogs}
          loading={loading}
          locale={{ emptyText: 'Chưa có nhật ký truy cập' }}
          columns={[
            { title: 'Người dùng', dataIndex: 'userName', key: 'userName', render: (v) => <Tag color="purple">{v}</Tag> },
            { title: 'Bệnh nhân', dataIndex: 'patientName', key: 'patientName', render: (v) => <strong>{v}</strong> },
            { title: 'Mã bệnh án', dataIndex: 'recordCode', key: 'recordCode', render: (v) => <Tag color="blue">{v}</Tag> },
            { title: 'Hành động', dataIndex: 'action', key: 'action', render: (v) => <Tag color="cyan">{v}</Tag> },
            { title: 'Thời gian', dataIndex: 'accessedAt', key: 'accessedAt', render: (v) => new Date(v).toLocaleString('vi-VN') },
          ]}
        />
      </Card>

      <ExportAccessLogReportModal
        open={exportModalOpen}
        onClose={() => setExportModalOpen(false)}
      />
    </>
  )
}
