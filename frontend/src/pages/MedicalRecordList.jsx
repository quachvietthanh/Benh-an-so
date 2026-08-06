import React, { useState, useEffect, useCallback } from 'react'
import { Table, Button, Tag, Typography, Space } from 'antd'
import { PlusOutlined, EyeOutlined } from '@ant-design/icons'
import { formatDateTime, formatRecordStatus } from '../utils/helpers'
import { mergeMedicalRecords } from '../utils/storageHelpers'

const { Title } = Typography

function MedicalRecordList() {
  const [loading, setLoading] = useState(false)
  const [records, setRecords] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  const fetchRecords = useCallback(() => {
    setLoading(true)
    const merged = mergeMedicalRecords([])
    setRecords(merged)
    setTotal(merged.length)
    setLoading(false)
  }, [page, pageSize])

  useEffect(() => {
    fetchRecords()
  }, [fetchRecords])

  const columns = [
    {
      title: 'Mã hồ sơ',
      dataIndex: 'recordCode',
      key: 'recordCode',
      width: 120,
      render: (text) => <Tag color="green">{text}</Tag>,
    },
    {
      title: 'Mã bệnh nhân',
      dataIndex: 'patientCode',
      key: 'patientCode',
      width: 120,
    },
    {
      title: 'Tên bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      ellipsis: true,
    },
    {
      title: 'Bác sĩ',
      dataIndex: 'doctorName',
      key: 'doctorName',
      width: 150,
      render: (text) => text || '---',
    },
    {
      title: 'Chẩn đoán',
      dataIndex: 'diagnosis',
      key: 'diagnosis',
      ellipsis: true,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status) => {
        const formatted = formatRecordStatus(status)
        return <Tag color={formatted.color}>{formatted.label}</Tag>
      },
    },
    {
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date) => formatDateTime(date),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => console.log('View record:', record.id)}
        >
          Xem
        </Button>
      ),
    },
  ]

  return (
    <div>
      <div className="page-header">
        <Title level={4} style={{ margin: 0 }}>Quản lý hồ sơ bệnh án</Title>
        <Button type="primary" icon={<PlusOutlined />}>
          Tạo hồ sơ mới
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={records}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (total) => `Tổng số: ${total} hồ sơ`,
          onChange: (newPage, newSize) => {
            setPage(newPage - 1)
            setPageSize(newSize)
          },
        }}
      />
    </div>
  )
}

export default MedicalRecordList
