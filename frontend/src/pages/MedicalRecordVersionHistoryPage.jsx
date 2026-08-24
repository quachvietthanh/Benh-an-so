import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Timeline,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  HistoryOutlined,
  UserOutlined,
  ClockCircleOutlined,
  SolutionOutlined,
  CheckCircleOutlined,
  EditOutlined,
  SearchOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  LockOutlined,
  FileTextOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
import patientApi from '../api/patientApi'
import { normalizeMedicalRecordDetail } from '../utils/workflowContract'
import {
  canViewMedicalRecordVersionHistory,
  normalizeVersionHistoryData,
  validateVersionHistoryQuery,
} from '../utils/medicalRecordVersionHelpers'
import { useAuthContext } from '../context/AuthContext'
import { getApiErrorMessage } from '../utils/apiError'
import { formatDateTime, formatRecordStatus } from '../utils/helpers'

const { Title, Text, Paragraph } = Typography

function MedicalRecordVersionHistoryPage() {
  const { user } = useAuthContext()

  const userRoles = useMemo(
    () => (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, '')),
    [user],
  )
  const userPerms = useMemo(
    () => (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, '')),
    [user],
  )

  const canUseFeature = canViewMedicalRecordVersionHistory(userRoles, userPerms)

  const [patients, setPatients] = useState([])
  const [patientLoading, setPatientLoading] = useState(false)
  const [selectedPatientId, setSelectedPatientId] = useState(null)
  const [selectedPatient, setSelectedPatient] = useState(null)

  const [records, setRecords] = useState([])
  const [recordsLoading, setRecordsLoading] = useState(false)
  const [selectedRecord, setSelectedRecord] = useState(null)

  const [versionLoading, setVersionLoading] = useState(false)
  const [versionHistoryData, setVersionHistoryData] = useState(null)
  const [versionError, setVersionError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')

  // 1. Tải danh sách bệnh nhân
  const loadPatients = useCallback(async () => {
    setPatientLoading(true)
    try {
      const res = await patientApi.getAll({ page: 0, size: 100 })
      const list = Array.isArray(res.data?.content)
        ? res.data.content
        : Array.isArray(res.data)
        ? res.data
        : []
      setPatients(list)
      if (list.length > 0 && !selectedPatientId) {
        setSelectedPatientId(list[0].id)
        setSelectedPatient(list[0])
      }
    } catch (err) {
      message.error(getApiErrorMessage(err, 'Không thể tải danh sách bệnh nhân.'))
    } finally {
      setPatientLoading(false)
    }
  }, [selectedPatientId])

  useEffect(() => {
    loadPatients()
  }, [loadPatients])

  // 2. Tải danh sách bệnh án theo bệnh nhân
  const loadPatientRecords = useCallback(async (patientId) => {
    if (!patientId) {
      setRecords([])
      setSelectedRecord(null)
      setVersionHistoryData(null)
      return
    }

    setRecordsLoading(true)
    setSelectedRecord(null)
    setVersionHistoryData(null)
    setVersionError(null)

    try {
      const res = await medicalRecordApi.getByPatient(patientId)
      const list = Array.isArray(res?.data)
        ? res.data.map(normalizeMedicalRecordDetail).filter(Boolean)
        : []
      setRecords(list)

      if (list.length > 0) {
        setSelectedRecord(list[0])
      }
    } catch {
      setRecords([])
    } finally {
      setRecordsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (selectedPatientId) {
      loadPatientRecords(selectedPatientId)
    }
  }, [selectedPatientId, loadPatientRecords])

  // 3. Tải lịch sử phiên bản của bệnh án được chọn
  const loadVersionHistory = useCallback(async (record) => {
    const { valid, recordId, error } = validateVersionHistoryQuery(record)
    if (!valid || !recordId) {
      setVersionError(error || 'Thiếu thông tin mã bệnh án.')
      setVersionHistoryData(null)
      return
    }

    setVersionLoading(true)
    setVersionError(null)

    try {
      const res = await medicalRecordApi.getVersionHistory(recordId)
      const data = res?.data || res
      const normalized = normalizeVersionHistoryData(data)
      setVersionHistoryData(normalized)
    } catch (err) {
      const status = err?.response?.status
      if (status === 403) {
        setVersionError('Bạn không có quyền truy cập lịch sử phiên bản của bệnh án này (Chỉ dành cho Quản lý phòng khám và Quản trị viên).')
      } else if (status === 404) {
        setVersionError('Không tìm thấy dữ liệu phiên bản của hồ sơ bệnh án.')
      } else {
        setVersionError(getApiErrorMessage(err, 'Không thể tải lịch sử phiên bản của bệnh án.'))
      }
      setVersionHistoryData(null)
    } finally {
      setVersionLoading(false)
    }
  }, [])

  useEffect(() => {
    if (selectedRecord) {
      loadVersionHistory(selectedRecord)
    } else {
      setVersionHistoryData(null)
      setVersionError(null)
    }
  }, [selectedRecord, loadVersionHistory])

  // Lọc bệnh nhân
  const filteredPatients = useMemo(() => {
    if (!searchKeyword.trim()) return patients
    const q = searchKeyword.toLowerCase()
    return patients.filter(
      (p) =>
        (p.fullName && p.fullName.toLowerCase().includes(q)) ||
        (p.patientCode && p.patientCode.toLowerCase().includes(q)) ||
        (p.phone && p.phone.includes(q)),
    )
  }, [patients, searchKeyword])

  // Cột bảng bệnh án được thiết kế cân đối, không bị chật chội
  const recordColumns = [
    {
      title: 'Hồ sơ bệnh án',
      key: 'recordInfo',
      render: (_, r) => {
        const fullId = r.medicalRecordId || r.id || ''
        const displayCode = r.recordCode && r.recordCode.startsWith('BA-')
          ? r.recordCode
          : fullId
          ? `BA-${String(fullId).substring(0, 8).toUpperCase()}`
          : '---'
        const formattedStatus = formatRecordStatus(r.status)

        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4, padding: '4px 0' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 6 }}>
              <Tooltip title={`ID: ${fullId}`}>
                <Tag color="cyan" style={{ fontWeight: 600, fontFamily: 'monospace', margin: 0 }}>
                  {displayCode}
                </Tag>
              </Tooltip>
              <Tag color={formattedStatus.color} style={{ margin: 0, fontSize: 11 }}>
                {formattedStatus.label}
              </Tag>
            </div>
            <div style={{ fontWeight: 600, fontSize: 13, color: '#1e293b' }}>
              {r.doctorName || 'Bác sĩ phụ trách'}
            </div>
            {r.diagnosis && (
              <div style={{ fontSize: 12, color: '#475569', lineHeight: 1.4 }}>
                {r.diagnosis}
              </div>
            )}
            <div style={{ fontSize: 11, color: '#94a3b8', display: 'flex', alignItems: 'center', gap: 4 }}>
              <ClockCircleOutlined />
              <span>{formatDateTime(r.createdAt)}</span>
            </div>
          </div>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 85,
      align: 'center',
      render: (_, r) => {
        const isCurrentSelected =
          (selectedRecord?.id || selectedRecord?.medicalRecordId) === (r.id || r.medicalRecordId)
        return (
          <Button
            type={isCurrentSelected ? 'primary' : 'default'}
            size="small"
            icon={<HistoryOutlined />}
            onClick={() => setSelectedRecord(r)}
            style={{
              borderColor: isCurrentSelected ? undefined : '#6366f1',
              color: isCurrentSelected ? undefined : '#6366f1',
            }}
          >
            {isCurrentSelected ? 'Đang xem' : 'Xem'}
          </Button>
        )
      },
    },
  ]

  const renderClinicalSnapshot = (snapshot) => {
    if (!snapshot) return null
    const diagnosesList = Array.isArray(snapshot.diagnoses) ? snapshot.diagnoses : []

    return (
      <Card
        size="small"
        style={{
          marginTop: 12,
          backgroundColor: '#ffffff',
          border: '1px solid #e2e8f0',
          borderRadius: 8,
        }}
      >
        <div style={{ marginBottom: 10, fontWeight: 600, color: '#0f172a', display: 'flex', alignItems: 'center', gap: 6 }}>
          <SolutionOutlined style={{ color: '#0284c7' }} />
          <span>Dữ liệu lâm sàng ghi nhận tại bản gốc:</span>
        </div>

        <Descriptions size="small" column={{ xs: 1, sm: 2 }} bordered style={{ backgroundColor: '#ffffff' }}>
          {snapshot.chiefComplaint && (
            <Descriptions.Item label={<Text strong>Lý do khám</Text>} span={2}>
              {snapshot.chiefComplaint}
            </Descriptions.Item>
          )}
          {snapshot.symptoms && (
            <Descriptions.Item label={<Text strong>Triệu chứng lâm sàng</Text>} span={2}>
              {snapshot.symptoms}
            </Descriptions.Item>
          )}
          {snapshot.medicalHistory && (
            <Descriptions.Item label={<Text strong>Tiền sử bệnh</Text>}>
              {snapshot.medicalHistory}
            </Descriptions.Item>
          )}
          {snapshot.physicalExamination && (
            <Descriptions.Item label={<Text strong>Khám thực thể</Text>}>
              {snapshot.physicalExamination}
            </Descriptions.Item>
          )}
          {diagnosesList.length > 0 && (
            <Descriptions.Item label={<Text strong>Chẩn đoán ICD-10</Text>} span={2}>
              <Space direction="vertical" size={2}>
                {diagnosesList.map((diag, i) => (
                  <Tag key={i} color="blue" style={{ margin: '2px 0' }}>
                    {diag}
                  </Tag>
                ))}
              </Space>
            </Descriptions.Item>
          )}
          {snapshot.clinicalProgress && (
            <Descriptions.Item label={<Text strong>Diễn biến bệnh</Text>}>
              {snapshot.clinicalProgress}
            </Descriptions.Item>
          )}
          {snapshot.treatmentPlan && (
            <Descriptions.Item label={<Text strong>Hướng điều trị</Text>}>
              {snapshot.treatmentPlan}
            </Descriptions.Item>
          )}
          {snapshot.doctorInstructions && (
            <Descriptions.Item label={<Text strong>Lời dặn bác sĩ</Text>} span={2}>
              {snapshot.doctorInstructions}
            </Descriptions.Item>
          )}
          {snapshot.conclusion && (
            <Descriptions.Item label={<Text strong>Kết luận</Text>} span={2}>
              <Text strong style={{ color: '#059669' }}>{snapshot.conclusion}</Text>
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>
    )
  }

  const timelineItems = (versionHistoryData?.allVersions || []).map((ver) => {
    const isOriginal = ver.isOriginal
    return {
      dot: isOriginal ? (
        <CheckCircleOutlined style={{ fontSize: 18, color: '#10b981' }} />
      ) : (
        <EditOutlined style={{ fontSize: 18, color: '#6366f1' }} />
      ),
      children: (
        <div
          style={{
            marginBottom: 20,
            padding: 16,
            backgroundColor: isOriginal ? '#f0fdf4' : '#f5f3ff',
            border: isOriginal ? '1px solid #bbf7d0' : '1px solid #ddd6fe',
            borderRadius: 10,
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <Space>
              <Tag
                color={isOriginal ? 'success' : 'purple'}
                style={{ fontWeight: 700, fontSize: 13, padding: '3px 10px' }}
              >
                Phiên bản v{ver.versionNumber} {isOriginal ? '(Bản gốc khởi tạo)' : `(Bản đính chính #${ver.versionNumber - 1})`}
              </Tag>
            </Space>
            <Space size="middle">
              <span style={{ fontSize: 13, color: '#475569' }}>
                <UserOutlined style={{ marginRight: 4, color: '#64748b' }} />
                <strong>Người thực hiện:</strong> <Text type="secondary">{ver.modifiedBy}</Text>
              </span>
              <span style={{ fontSize: 13, color: '#475569' }}>
                <ClockCircleOutlined style={{ marginRight: 4, color: '#64748b' }} />
                <strong>Thời điểm:</strong> <Text type="secondary">{ver.formattedModifiedAt}</Text>
              </span>
            </Space>
          </div>

          <Divider style={{ margin: '12px 0' }} />

          {!isOriginal && (
            <div style={{ marginBottom: 12 }}>
              <div style={{ marginBottom: 4 }}>
                <Text strong style={{ color: '#4338ca' }}>Lý do đính chính / thay đổi:</Text>
              </div>
              <div
                style={{
                  padding: '8px 14px',
                  backgroundColor: '#ffffff',
                  border: '1px solid #e0e7ff',
                  borderRadius: 6,
                  color: '#3730a3',
                  fontWeight: 600,
                }}
              >
                {ver.reason}
              </div>
            </div>
          )}

          {!isOriginal && (
            <div>
              <div style={{ marginBottom: 4 }}>
                <Text strong style={{ color: '#1e293b' }}>Nội dung chi tiết đính chính / bổ sung:</Text>
              </div>
              <div
                style={{
                  padding: '10px 14px',
                  backgroundColor: '#ffffff',
                  border: '1px solid #e2e8f0',
                  borderRadius: 6,
                  color: '#334155',
                  whiteSpace: 'pre-wrap',
                  lineHeight: 1.6,
                }}
              >
                {ver.content}
              </div>
            </div>
          )}

          {isOriginal && renderClinicalSnapshot(ver.snapshot)}
        </div>
      ),
    }
  })

  return (
    <div style={{ padding: '4px 0' }}>
      {/* Header */}
      <div style={{ marginBottom: 20 }}>
        <Title level={3} style={{ margin: 0, color: '#1e293b', display: 'flex', alignItems: 'center', gap: 10 }}>
          <HistoryOutlined style={{ color: '#4f46e5' }} /> Lịch sử phiên bản bệnh án
        </Title>
        <Text type="secondary">
          Dành cho Quản lý phòng khám tra cứu, đối soát toàn bộ các phiên bản bệnh án và chứng minh tính minh bạch khi có khiếu nại.
        </Text>
      </div>

      <Row gutter={[16, 16]}>
        {/* Cột trái: Chọn bệnh nhân & Bệnh án */}
        <Col xs={24} lg={10} xl={9}>
          <Card
            title={
              <Space>
                <UserOutlined style={{ color: '#2563eb' }} />
                <span>Chọn bệnh nhân & Danh sách hồ sơ</span>
              </Space>
            }
            size="small"
            style={{ borderRadius: 10, boxShadow: '0 1px 3px rgba(0,0,0,0.05)', marginBottom: 16 }}
          >
            <div style={{ marginBottom: 12 }}>
              <Input
                placeholder="Tìm bệnh nhân theo tên, mã hoặc SĐT..."
                prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
                allowClear
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                style={{ marginBottom: 8 }}
              />
              <Select
                showSearch
                placeholder="Chọn bệnh nhân..."
                loading={patientLoading}
                value={selectedPatientId}
                onChange={(val) => {
                  setSelectedPatientId(val)
                  const p = patients.find((item) => item.id === val)
                  setSelectedPatient(p || null)
                }}
                style={{ width: '100%' }}
                filterOption={false}
              >
                {filteredPatients.map((p) => (
                  <Select.Option key={p.id} value={p.id}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span><strong>{p.fullName}</strong> ({p.patientCode || 'Chưa có mã'})</span>
                      <Text type="secondary" style={{ fontSize: 11 }}>{p.phone}</Text>
                    </div>
                  </Select.Option>
                ))}
              </Select>
            </div>

            {selectedPatient && (
              <div
                style={{
                  padding: 10,
                  backgroundColor: '#f1f5f9',
                  borderRadius: 6,
                  marginBottom: 12,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                }}
              >
                <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#2563eb' }} />
                <div>
                  <div style={{ fontWeight: 600, color: '#0f172a' }}>{selectedPatient.fullName}</div>
                  <div style={{ fontSize: 12, color: '#64748b' }}>
                    Mã BN: <Tag color="blue">{selectedPatient.patientCode || 'BN---'}</Tag>
                    {selectedPatient.gender && `• ${selectedPatient.gender === 'MALE' ? 'Nam' : 'Nữ'}`}
                    {selectedPatient.dob && ` • ${dayjs(selectedPatient.dob).format('YYYY')}`}
                  </div>
                </div>
              </div>
            )}

            <Divider style={{ margin: '12px 0' }} />

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <Text strong style={{ color: '#334155' }}>Hồ sơ bệnh án của bệnh nhân:</Text>
              <Button
                type="text"
                size="small"
                icon={<ReloadOutlined />}
                onClick={() => loadPatientRecords(selectedPatientId)}
                loading={recordsLoading}
              >
                Tải lại
              </Button>
            </div>

            <Table
              rowKey={(r) => r.id || r.medicalRecordId}
              columns={recordColumns}
              dataSource={records}
              loading={recordsLoading}
              pagination={{ pageSize: 5, size: 'small' }}
              size="small"
              locale={{
                emptyText: (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description="Bệnh nhân chưa có hồ sơ bệnh án nào."
                  />
                ),
              }}
            />
          </Card>
        </Col>

        {/* Cột phải: Dòng thời gian Lịch sử phiên bản */}
        <Col xs={24} lg={14} xl={15}>
          <Card
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                <Space>
                  <HistoryOutlined style={{ color: '#4f46e5' }} />
                  <span>Dòng thời gian các phiên bản & Đính chính</span>
                </Space>
                {selectedRecord && (
                  <Button
                    size="small"
                    icon={<ReloadOutlined />}
                    onClick={() => loadVersionHistory(selectedRecord)}
                    loading={versionLoading}
                  >
                    Làm mới phiên bản
                  </Button>
                )}
              </div>
            }
            size="small"
            style={{ borderRadius: 10, boxShadow: '0 1px 3px rgba(0,0,0,0.05)', minHeight: 480 }}
          >
            {!selectedRecord && (
              <div style={{ padding: '60px 0', textAlign: 'center' }}>
                <Empty
                  description="Vui lòng chọn một hồ sơ bệnh án từ danh sách bên trái để xem lịch sử các phiên bản."
                />
              </div>
            )}

            {selectedRecord && (
              <div>
                {/* Thẻ tóm tắt bệnh án */}
                <Card
                  size="small"
                  style={{
                    marginBottom: 16,
                    background: 'linear-gradient(135deg, #f8fafc 0%, #edf2f7 100%)',
                    border: '1px solid #cbd5e1',
                    borderRadius: 8,
                  }}
                >
                  <Descriptions size="small" column={{ xs: 1, sm: 3 }}>
                    <Descriptions.Item label={<Text strong>Mã bệnh án</Text>}>
                      <Tag color="cyan" style={{ fontFamily: 'monospace', fontWeight: 600 }}>
                        {selectedRecord.recordCode || `BA-${String(selectedRecord.id || selectedRecord.medicalRecordId).substring(0, 8).toUpperCase()}`}
                      </Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label={<Text strong>Bệnh nhân</Text>}>
                      <Text strong style={{ color: '#0f172a' }}>{selectedPatient?.fullName || selectedRecord.patientName}</Text>
                    </Descriptions.Item>
                    <Descriptions.Item label={<Text strong>Tổng số phiên bản</Text>}>
                      {versionLoading ? (
                        <Spin size="small" />
                      ) : (
                        <Badge
                          count={versionHistoryData?.totalVersions || 0}
                          showZero
                          style={{
                            backgroundColor: (versionHistoryData?.totalVersions || 0) > 1 ? '#7c3aed' : '#10b981',
                            fontWeight: 700,
                          }}
                        />
                      )}
                    </Descriptions.Item>
                  </Descriptions>
                </Card>

                {versionLoading && (
                  <div style={{ textAlign: 'center', padding: '60px 0' }}>
                    <Spin size="large" tip="Đang truy vấn dữ liệu lịch sử các phiên bản..." />
                  </div>
                )}

                {versionError && !versionLoading && (
                  <Alert
                    type="error"
                    showIcon
                    message="Không thể tải lịch sử phiên bản"
                    description={versionError}
                    style={{ marginBottom: 16 }}
                  />
                )}

                {!versionLoading && !versionError && versionHistoryData && (
                  <div>
                    {versionHistoryData.originalOnly ? (
                      <Alert
                        type="info"
                        showIcon
                        icon={<SafetyCertificateOutlined style={{ color: '#10b981' }} />}
                        message="Hồ sơ bệnh án ở phiên bản gốc ban đầu (Chưa có bản đính chính)"
                        description="Hồ sơ này chưa có bất kỳ bản đính chính nào kể từ khi bác sĩ tạo lập. Toàn bộ nội dung bệnh án được bảo toàn nguyên vẹn."
                        style={{ marginBottom: 16, backgroundColor: '#f0fdf4', border: '1px solid #bbf7d0' }}
                      />
                    ) : (
                      <Alert
                        type="warning"
                        showIcon
                        icon={<HistoryOutlined style={{ color: '#6366f1' }} />}
                        message={`Hồ sơ đã qua ${versionHistoryData.amendments.length} lần đính chính / bổ sung thông tin`}
                        description="Dưới đây là toàn bộ lịch sử chỉnh sửa bao gồm danh tính người thực hiện, mốc thời gian và lý do đính chính rõ ràng nhằm phục vụ minh bạch và đối soát khiếu nại."
                        style={{ marginBottom: 16, backgroundColor: '#faf5ff', border: '1px solid #e9d5ff' }}
                      />
                    )}

                    <div style={{ maxHeight: '65vh', overflowY: 'auto', paddingRight: 8 }}>
                      <Timeline items={timelineItems} />
                    </div>
                  </div>
                )}
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default MedicalRecordVersionHistoryPage
