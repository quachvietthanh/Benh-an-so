import React, { useMemo, useState } from 'react'
import { Card, Input, Table, Tag } from 'antd'
import { SearchOutlined } from '@ant-design/icons'

export default function AuditLogsReportView({ auditLogs = [], loading = false }) {
  const [searchTerm, setSearchTerm] = useState('')

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
  )
}
