import React, { useState, useEffect, useCallback } from 'react'
import { Alert, Table, Button, Tag, Typography } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import medicalRecordApi from '../api/medicalRecordApi'
import { formatDateTime, formatRecordStatus } from '../utils/helpers'
import { normalizeMedicalRecordDetail } from '../utils/workflowContract'

const { Title } = Typography

function MedicalRecordList({ patientId }) {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [records, setRecords] = useState([])

  const fetchRecords = useCallback(async () => {
    if (!patientId) {
      setRecords([])
      return
    }
    setLoading(true)
    try {
      const response = await medicalRecordApi.getByPatient(patientId)
      const list = Array.isArray(response?.data)
        ? response.data.map(normalizeMedicalRecordDetail).filter(Boolean)
        : []
      setRecords(list)
    } catch {
      setRecords([])
    } finally {
      setLoading(false)
    }
  }, [patientId])

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
          disabled={!record.visitId}
          onClick={() => navigate(`/medical-records/visits/${record.visitId}`)}
        >
          Xem
        </Button>
      ),
    },
  ]

  return (
    <div>
      <div className="page-header">
        <Title level={4} style={{ margin: 0 }}>Lịch sử bệnh án theo bệnh nhân</Title>
      </div>

      {!patientId && (
        <Alert
          type="info"
          showIcon
          message="Cần patientId để tải lịch sử bệnh án"
          description="Backend không cung cấp danh sách bệnh án toàn hệ thống; hãy mở lịch sử từ hồ sơ một bệnh nhân cụ thể."
          style={{ marginBottom: 16 }}
        />
      )}

      <Table
        columns={columns}
        dataSource={records}
        rowKey="id"
        loading={loading}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showTotal: (total) => `Tổng số: ${total} hồ sơ`,
        }}
      />
    </div>
  )
}

export default MedicalRecordList
