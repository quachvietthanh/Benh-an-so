import React, { useState, useEffect, useCallback } from 'react'
import {
  Modal,
  Table,
  Tag,
  Space,
  Button,
  Typography,
  Spin,
  Alert,
  Empty,
  Card,
  Descriptions,
  Timeline,
  Radio,
  Tooltip,
} from 'antd'
import {
  HistoryOutlined,
  FileTextOutlined,
  CalendarOutlined,
  UserOutlined,
  MedicineBoxOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FileSearchOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import patientApi from '../../api/patientApi'
import PatientAllergyBanner from './PatientAllergyBanner'
import { getApiErrorMessage } from '../../utils/apiError'

const { Text, Title, Paragraph } = Typography

/**
 * Modal hiển thị toàn bộ lịch sử các lần khám bệnh của bệnh nhân
 * Giúp bác sĩ tra cứu tiền sử khám, chẩn đoán cũ, các phác đồ và đơn thuốc trước đây
 */
export default function PatientMedicalHistoryModal({
  open,
  onClose,
  patientId,
  patientName,
  patientCode,
  onOpenEncounter,
}) {
  const [loading, setLoading] = useState(false)
  const [historyList, setHistoryList] = useState([])
  const [patientDetails, setPatientDetails] = useState(null)
  const [viewMode, setViewMode] = useState('table') // 'table' | 'timeline'
  const [errorMessage, setErrorMessage] = useState(null)

  const fetchHistory = useCallback(async () => {
    if (!open || !patientId) return
    setLoading(true)
    setErrorMessage(null)

    try {
      // 1. Tải thông tin bệnh nhân
      try {
        const pRes = await patientApi.getById(patientId)
        setPatientDetails(pRes.data || null)
      } catch {
        // Fallback giữ nguyên props truyền vào
      }

      // 2. Tải danh sách lịch sử khám
      const res = await patientApi.getHistory(patientId, {
        page: 0,
        size: 50,
        sort: 'visitAt,desc',
      })
      const items = res.data?.content || res.data?.items || (Array.isArray(res.data) ? res.data : [])
      setHistoryList(items)
    } catch (err) {
      console.error('Lỗi khi tải lịch sử khám bệnh nhân:', err)
      const msg = getApiErrorMessage(err, 'Không thể tải lịch sử khám bệnh của bệnh nhân.')
      setErrorMessage(msg)
      setHistoryList([])
    } finally {
      setLoading(false)
    }
  }, [open, patientId])

  useEffect(() => {
    if (open) {
      fetchHistory()
    } else {
      setHistoryList([])
      setPatientDetails(null)
      setErrorMessage(null)
    }
  }, [open, fetchHistory])

  const getRecordStatusTag = (status) => {
    switch (status) {
      case 'LOCKED':
        return <Tag color="error">Đã khóa</Tag>
      case 'SIGNED':
        return <Tag color="success">Đã ký số</Tag>
      case 'IN_PROGRESS':
        return <Tag color="processing">Đang khám</Tag>
      case 'DRAFT':
        return <Tag color="warning">Bản nháp</Tag>
      default:
        return status ? <Tag>{status}</Tag> : <Text type="secondary">—</Text>
    }
  }

  const getVisitStatusTag = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <Tag color="green">Đã hoàn thành</Tag>
      case 'IN_PROGRESS':
        return <Tag color="blue">Đang khám</Tag>
      case 'WAITING_FOR_RESULT':
        return <Tag color="purple">Chờ kết quả CĐLS</Tag>
      case 'WAITING':
        return <Tag color="gold">Chờ khám</Tag>
      case 'CANCELLED':
        return <Tag color="default">Đã hủy</Tag>
      case 'SKIPPED':
        return <Tag color="orange">Bỏ qua</Tag>
      default:
        return status ? <Tag>{status}</Tag> : <Text type="secondary">—</Text>
    }
  }

  const columns = [
    {
      title: 'Mã lượt / Ngày khám',
      key: 'visitInfo',
      width: 170,
      render: (_, record) => (
        <div>
          <div style={{ fontWeight: 700, color: '#1d4ed8', fontFamily: 'monospace' }}>
            {record.visitCode || '—'}
          </div>
          <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
            <CalendarOutlined style={{ marginRight: 4 }} />
            {record.visitAt ? dayjs(record.visitAt).format('DD/MM/YYYY HH:mm') : '—'}
          </div>
        </div>
      ),
    },
    {
      title: 'Bác sĩ phụ trách',
      dataIndex: 'doctorName',
      key: 'doctorName',
      width: 150,
      render: (doc) => (
        <span style={{ fontWeight: 600, color: '#334155' }}>
          {doc || '—'}
        </span>
      ),
    },
    {
      title: 'Lý do đến khám',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
      render: (reason) => reason || <Text type="secondary">—</Text>,
    },
    {
      title: 'Chẩn đoán / Triệu chứng',
      key: 'clinicalSummary',
      render: (_, record) => {
        const diag = record.conclusion || record.chiefComplaint
        if (!diag) return <Text type="secondary">—</Text>
        return (
          <div>
            {record.chiefComplaint && (
              <div style={{ fontSize: 12, color: '#475569' }}>
                <strong>Triệu chứng:</strong> {record.chiefComplaint}
              </div>
            )}
            {record.conclusion && (
              <div style={{ fontSize: 13, fontWeight: 600, color: '#0f172a', marginTop: 2 }}>
                <strong>Kết luận:</strong> {record.conclusion}
              </div>
            )}
          </div>
        )
      },
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 150,
      render: (_, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, alignItems: 'flex-start' }}>
          {getVisitStatusTag(record.visitStatus)}
          {record.medicalRecordStatus && getRecordStatusTag(record.medicalRecordStatus)}
        </div>
      ),
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 140,
      align: 'center',
      render: (_, record) => (
        <Button
          size="small"
          type="primary"
          ghost
          icon={<FileSearchOutlined />}
          onClick={() => {
            if (onOpenEncounter && record.visitId) {
              onClose()
              onOpenEncounter({
                visitId: record.visitId,
                patientId: patientId,
                patientName: patientName || patientDetails?.fullName,
                patientCode: patientCode || patientDetails?.patientCode,
              })
            }
          }}
        >
          Xem bệnh án
        </Button>
      ),
    },
  ]

  const displayPatient = patientDetails || {
    fullName: patientName,
    patientCode: patientCode,
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 38,
              height: 38,
              borderRadius: 8,
              backgroundColor: '#eff6ff',
              color: '#2563eb',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20,
            }}
          >
            <HistoryOutlined />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              Lịch Sử Khám Bệnh: {displayPatient.fullName || 'Bệnh nhân'}
            </div>
            <div style={{ fontSize: 12, color: '#64748b' }}>
              Mã hồ sơ BN: <strong>{displayPatient.patientCode || patientCode || '—'}</strong>
            </div>
          </div>
        </div>
      }
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="refresh" icon={<ReloadOutlined />} onClick={fetchHistory} loading={loading}>
          Làm mới
        </Button>,
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
      width={1020}
      style={{ top: 20 }}
      destroyOnClose
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {/* Banner tóm tắt bệnh nhân */}
        <Card size="small" style={{ borderRadius: 8, background: '#f8fafc', borderColor: '#e2e8f0' }}>
          <Descriptions size="small" column={{ xs: 1, sm: 2, md: 4 }}>
            <Descriptions.Item label="Họ tên">
              <Text strong>{displayPatient.fullName || '—'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Mã bệnh nhân">
              <Tag color="blue">{displayPatient.patientCode || '—'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Ngày sinh / Tuổi">
              {displayPatient.dateOfBirth
                ? `${dayjs(displayPatient.dateOfBirth).format('DD/MM/YYYY')} (${dayjs().diff(dayjs(displayPatient.dateOfBirth), 'year')} tuổi)`
                : '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Số điện thoại">
              {displayPatient.phone || displayPatient.phoneNumber || '—'}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* Cảnh báo tiền sử dị ứng thuốc nếu có */}
        {patientId && (
          <PatientAllergyBanner
            patientId={patientId}
            patientName={displayPatient.fullName}
            compact
            hideIfEmpty
          />
        )}

        {/* Thanh điều khiển hiển thị */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: '#334155' }}>
            Tổng số lần khám ghi nhận: <Tag color="geekblue">{historyList.length} lượt khám</Tag>
          </div>
          <Radio.Group
            value={viewMode}
            onChange={(e) => setViewMode(e.target.value)}
            size="small"
            buttonStyle="solid"
          >
            <Radio.Button value="table">Dạng bảng</Radio.Button>
            <Radio.Button value="timeline">Dạng tiến trình</Radio.Button>
          </Radio.Group>
        </div>

        {errorMessage && (
          <Alert type="error" message={errorMessage} showIcon style={{ borderRadius: 6 }} />
        )}

        {/* Nội dung danh sách */}
        {loading ? (
          <div style={{ padding: '40px 0', textAlign: 'center' }}>
            <Spin tip="Đang tải lịch sử khám bệnh..." />
          </div>
        ) : historyList.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Bệnh nhân chưa có lượt khám nào trước đây được ghi nhận trên hệ thống."
            style={{ margin: '30px 0' }}
          />
        ) : viewMode === 'table' ? (
          <Table
            dataSource={historyList}
            columns={columns}
            rowKey={(r) => r.visitId || r.id || r.visitCode}
            pagination={{ pageSize: 5, showSizeChanger: false }}
            size="small"
            scroll={{ x: 880 }}
            bordered
          />
        ) : (
          <div style={{ maxHeight: 380, overflowY: 'auto', padding: '16px 12px 0' }}>
            <Timeline
              items={historyList.map((item) => ({
                color: item.visitStatus === 'COMPLETED' ? 'green' : 'blue',
                dot: item.visitStatus === 'COMPLETED' ? <CheckCircleOutlined /> : <ClockCircleOutlined />,
                children: (
                  <div style={{ marginBottom: 16 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                      <span style={{ fontWeight: 700, color: '#1e293b' }}>
                        {item.visitAt ? dayjs(item.visitAt).format('DD/MM/YYYY HH:mm') : '—'}
                      </span>
                      <Tag color="blue">{item.visitCode}</Tag>
                      {getVisitStatusTag(item.visitStatus)}
                      {item.medicalRecordStatus && getRecordStatusTag(item.medicalRecordStatus)}
                    </div>
                    <div style={{ fontSize: 13, marginTop: 4, color: '#334155' }}>
                      <strong>Bác sĩ:</strong> {item.doctorName || '—'}
                    </div>
                    {item.reason && (
                      <div style={{ fontSize: 12.5, color: '#475569', marginTop: 2 }}>
                        <strong>Lý do khám:</strong> {item.reason}
                      </div>
                    )}
                    {(item.conclusion || item.chiefComplaint) && (
                      <div
                        style={{
                          background: '#f1f5f9',
                          padding: '6px 10px',
                          borderRadius: 6,
                          marginTop: 6,
                          fontSize: 12.5,
                        }}
                      >
                        {item.chiefComplaint && <div><strong>Triệu chứng:</strong> {item.chiefComplaint}</div>}
                        {item.conclusion && <div><strong>Kết luận:</strong> {item.conclusion}</div>}
                      </div>
                    )}
                    <div style={{ marginTop: 6 }}>
                      <Button
                        size="small"
                        type="link"
                        icon={<FileSearchOutlined />}
                        style={{ padding: 0 }}
                        onClick={() => {
                          if (onOpenEncounter && item.visitId) {
                            onClose()
                            onOpenEncounter({
                              visitId: item.visitId,
                              patientId: patientId,
                              patientName: patientName || patientDetails?.fullName,
                              patientCode: patientCode || patientDetails?.patientCode,
                            })
                          }
                        }}
                      >
                        Xem chi tiết bệnh án này →
                      </Button>
                    </div>
                  </div>
                ),
              }))}
            />
          </div>
        )}
      </div>
    </Modal>
  )
}
