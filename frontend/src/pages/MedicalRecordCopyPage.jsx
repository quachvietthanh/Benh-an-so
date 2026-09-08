import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AuditOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  EyeOutlined,
  FileProtectOutlined,
  FileTextOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  LockOutlined,
  ReloadOutlined,
  SearchOutlined,
  SolutionOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../api/medicalRecordApi'
import patientApi from '../api/patientApi'
import userApi from '../api/userApi'
import MedicalRecordCopyPreviewModal from '../components/medicalRecord/MedicalRecordCopyPreviewModal'
import MedicalRecordVersionHistoryModal from '../components/medicalRecord/MedicalRecordVersionHistoryModal'
import { useAuthContext } from '../context/AuthContext'
import { getApiErrorMessage } from '../utils/apiError'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input

const RELATIONSHIP_OPTIONS = [
  { value: 'Bản thân bệnh nhân (Chính chủ)', label: 'Bản thân bệnh nhân (Chính chủ)' },
  { value: 'Người được ủy quyền hợp pháp (Có văn bản ủy quyền)', label: 'Người được ủy quyền hợp pháp (Có văn bản ủy quyền)' },
  { value: 'Người giám hộ hợp pháp (Bệnh nhân chưa thành niên / mất năng lực hành vi)', label: 'Người giám hộ hợp pháp' },
  { value: 'Bố / Mẹ đẻ (Bệnh nhân chưa thành niên)', label: 'Bố / Mẹ đẻ (Bệnh nhân chưa thành niên)' },
  { value: 'Vợ / Chồng', label: 'Vợ / Chồng' },
  { value: 'Con đẻ / Con nuôi hợp pháp', label: 'Con đẻ / Con nuôi hợp pháp' },
  { value: 'Cơ quan có thẩm quyền (Tư pháp / Điều tra / Giám định y khoa)', label: 'Cơ quan có thẩm quyền (Tư pháp / Bảo hiểm)' },
  { value: 'Đối tượng khác (Có giấy ủy quyền hợp pháp)', label: 'Đối tượng khác (Có giấy ủy quyền hợp pháp)' },
]

const PURPOSE_OPTIONS = [
  { value: 'Lưu trữ cá nhân và theo dõi sức khỏe', label: 'Lưu trữ cá nhân và theo dõi sức khỏe' },
  { value: 'Chuyển viện / Khám chữa bệnh tại tuyến trên', label: 'Chuyển viện / Khám chữa bệnh tại tuyến trên' },
  { value: 'Làm thủ tục thanh toán bảo hiểm y tế / bảo hiểm thương mại', label: 'Làm thủ tục thanh toán bảo hiểm y tế / thương mại' },
  { value: 'Phục vụ công tác giám định y khoa / pháp y', label: 'Phục vụ công tác giám định y khoa / pháp y' },
  { value: 'Cung cấp theo yêu cầu của cơ quan có thẩm quyền', label: 'Cung cấp theo yêu cầu cơ quan có thẩm quyền' },
  { value: 'Khác', label: 'Mục đích khác' },
]

function MedicalRecordCopyPage() {
  const { user } = useAuthContext()
  const [form] = Form.useForm()

  const userRoles = useMemo(
    () => (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, '')),
    [user],
  )
  const userPerms = useMemo(
    () => (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, '')),
    [user],
  )

  const isAdmin = userRoles.includes('admin')
  const isManager = userRoles.includes('manager') || userRoles.includes('clinic_manager')
  const isExplicitlyGrantedByAdmin = userPerms.includes('REPORT_EXPORT')

  const canUseFeature = isAdmin || isManager || isExplicitlyGrantedByAdmin

  const [patients, setPatients] = useState([])
  const [patientLoading, setPatientLoading] = useState(false)
  const [selectedPatientId, setSelectedPatientId] = useState(null)
  const [selectedPatient, setSelectedPatient] = useState(null)

  const [records, setRecords] = useState([])
  const [recordsLoading, setRecordsLoading] = useState(false)
  const [selectedRecord, setSelectedRecord] = useState(null)
  const [diagnoses, setDiagnoses] = useState([])

  const [previewModalOpen, setPreviewModalOpen] = useState(false)
  const [versionModalOpen, setVersionModalOpen] = useState(false)
  const [selectedVersionRecord, setSelectedVersionRecord] = useState(null)
  const [activeTab, setActiveTab] = useState('request')

  const [accessLogs, setAccessLogs] = useState([])
  const [logsLoading, setLogsLoading] = useState(false)
  const [users, setUsers] = useState([])

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

  const loadUsers = useCallback(async () => {
    try {
      const res = await (userApi.list ? userApi.list() : userApi.getAll())
      const list = Array.isArray(res.data?.content) ? res.data.content : (Array.isArray(res.data) ? res.data : [])
      setUsers(list)
    } catch {}
  }, [])

  useEffect(() => {
    loadPatients()
    loadUsers()
  }, [loadPatients, loadUsers])

  const userMap = useMemo(() => {
    const map = new Map()
    users.forEach((u) => {
      if (u?.id) map.set(String(u.id), u)
    })
    return map
  }, [users])

  const loadPatientRecords = useCallback(async (patientId) => {
    if (!patientId) return
    setRecordsLoading(true)
    setSelectedRecord(null)

    try {
      const res = await medicalRecordApi.getByPatient(patientId)
      const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
      setRecords(list)
      if (list.length > 0) {
        const lockedRecord = list.find((r) => r.status === 'LOCKED')
        setSelectedRecord(lockedRecord || list[0])
      }
    } catch {
      setRecords([])
      setSelectedRecord(null)
    } finally {
      setRecordsLoading(false)
    }
  }, [])

  const loadAccessLogs = useCallback(async (patientId) => {
    if (!patientId) return
    setLogsLoading(true)
    try {
      const res = await medicalRecordApi.getAccessLogsByPatient(patientId, { size: 50 })
      const rawLogs = Array.isArray(res.data?.content) ? res.data.content : (Array.isArray(res.data) ? res.data : [])
      setAccessLogs(rawLogs)
    } catch {
      setAccessLogs([])
    } finally {
      setLogsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (selectedPatientId) {
      const found = patients.find((p) => p.id === selectedPatientId)
      setSelectedPatient(found || null)
      loadPatientRecords(selectedPatientId, found?.patientCode)
      loadAccessLogs(selectedPatientId)

      if (found) {
        form.setFieldsValue({
          requesterName: found.fullName,
          relationship: 'Bản thân bệnh nhân (Chính chủ)',
          identityNumber: found.identityNumber || '',
          phone: found.phone || '',
          purpose: 'Lưu trữ cá nhân và theo dõi sức khỏe',
          copyCount: 1,
        })
      }
    }
  }, [selectedPatientId, patients, loadPatientRecords, loadAccessLogs, form])

  useEffect(() => {
    if (selectedRecord?.medicalRecordId || selectedRecord?.id) {
      const recordId = selectedRecord.medicalRecordId || selectedRecord.id
      medicalRecordApi
        .getDiagnosis(recordId)
        .then((res) => {
          const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
          if (list.length > 0) {
            setDiagnoses(list)
          } else if (selectedRecord.primaryIcdName) {
            setDiagnoses([
              {
                diagnosisCode: selectedRecord.primaryIcdCode || 'ICD',
                diagnosisName: selectedRecord.primaryIcdName,
                diagnosisType: 'PRIMARY',
              },
            ])
          } else {
            setDiagnoses([])
          }
        })
        .catch(() => {
          if (selectedRecord.primaryIcdName) {
            setDiagnoses([
              {
                diagnosisCode: selectedRecord.primaryIcdCode || 'ICD',
                diagnosisName: selectedRecord.primaryIcdName,
                diagnosisType: 'PRIMARY',
              },
            ])
          } else {
            setDiagnoses([])
          }
        })
    } else {
      setDiagnoses([])
    }
  }, [selectedRecord])

  const handleCopyIssued = useCallback((issueData) => {
    const newLog = {
      id: `log-copy-${Date.now()}`,
      accessedAt: new Date().toISOString(),
      accessedBy: user?.id || 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7',
      action: 'EXPORT',
      detail: `Đã cấp ${issueData?.copyCount || 1} bản sao trích lục HSBA [${selectedRecord?.visit?.visitCode || selectedRecord?.visitCode || 'VIS-001'}] cho: ${issueData?.requesterName} (Quan hệ: ${issueData?.relationship}). Mục đích: ${issueData?.purpose}`,
    }

    setAccessLogs((prev) => [newLog, ...(prev || [])])

    if (selectedPatientId) {
      const logsKey = `synced_access_logs_${selectedPatientId}`
      try {
        const existing = JSON.parse(localStorage.getItem(logsKey) || '[]')
        localStorage.setItem(logsKey, JSON.stringify([newLog, ...existing]))
      } catch {}
    }

    message.success({
      content: 'Đã tạo bản sao hồ sơ bệnh án thành công và tự động ghi nhận vào Nhật ký cấp phát!',
      duration: 4,
    })
  }, [user, selectedRecord, selectedPatientId])

  const handleOpenPreview = async () => {
    if (!canUseFeature) {
      message.error('Bạn không có quyền thực hiện cấp bản sao hồ sơ bệnh án.')
      return
    }

    if (!selectedRecord) {
      message.warning('Vui lòng chọn một hồ sơ bệnh án.')
      return
    }

    if (selectedRecord.status !== 'LOCKED') {
      message.error('Chỉ hồ sơ bệnh án ở trạng thái ĐÃ KHÓA (LOCKED) mới đủ điều kiện cấp bản sao pháp lý.')
      return
    }

    try {
      await form.validateFields()
      setPreviewModalOpen(true)
    } catch {
      message.warning('Vui lòng hoàn tất các thông tin yêu cầu cấp bản sao bắt buộc.')
    }
  }

  const recordColumns = [
    {
      title: 'Mã đợt khám',
      key: 'visitCode',
      width: 150,
      render: (_, r) => {
        const rawType = r.visit?.visitType || r.visitType || ''
        const typeLabel =
          rawType === 'APPOINTMENT'
            ? 'Hẹn trước'
            : rawType === 'WALK_IN'
            ? 'Vãng lai'
            : rawType || 'Ngoại trú'
        return (
          <div style={{ whiteSpace: 'nowrap' }}>
            <Text strong style={{ color: '#1e3a8a', display: 'block' }}>
              {r.visit?.visitCode || r.visitCode || 'VIS-001'}
            </Text>
            <Tag color="blue" style={{ fontSize: 10, margin: '2px 0 0 0', padding: '0 6px', borderRadius: 4 }}>
              {typeLabel}
            </Tag>
          </div>
        )
      },
    },
    {
      title: 'Thời gian khám',
      key: 'visitAt',
      width: 140,
      render: (_, r) => {
        const time = r.visit?.visitAt || r.createdAt
        return time ? (
          <div style={{ whiteSpace: 'nowrap' }}>
            <div>{dayjs(time).format('DD/MM/YYYY')}</div>
            <Text type="secondary" style={{ fontSize: 11 }}>
              {dayjs(time).format('HH:mm')}
            </Text>
          </div>
        ) : (
          '—'
        )
      },
    },
    {
      title: 'Bác sĩ phụ trách',
      key: 'doctor',
      width: 220,
      render: (_, r) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, maxWidth: '100%', overflow: 'hidden' }}>
          <Avatar size="small" icon={<UserOutlined />} style={{ backgroundColor: '#2563eb', flexShrink: 0 }} />
          <span style={{ fontWeight: 500, color: '#1e293b', whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden' }}>
            {r.visit?.doctorName || r.doctorName || 'Dr. Nguyen Minh Anh'}
          </span>
        </div>
      ),
    },
    {
      title: 'Lý do & Chẩn đoán',
      key: 'diagnosis',
      width: 230,
      render: (_, r) => (
        <div style={{ paddingLeft: 4 }}>
          <div style={{ fontWeight: 600, color: '#0f172a' }}>
            {r.chiefComplaint || r.visit?.reason || 'Khám bệnh'}
          </div>
          <div style={{ fontSize: 12, color: '#2563eb', marginTop: 2 }}>
            {r.primaryIcdName ? `[${r.primaryIcdCode || 'ICD'}] ${r.primaryIcdName}` : 'Chưa có CĐ chính'}
          </div>
        </div>
      ),
    },
    {
      title: 'Trạng thái hồ sơ',
      key: 'status',
      width: 160,
      align: 'center',
      render: (_, r) => {
        const isLocked = r.status === 'LOCKED'
        return (
          <Tooltip
            title={
              isLocked
                ? 'Hồ sơ đã khóa bảo mật, đủ điều kiện cấp trích sao pháp lý'
                : 'Hồ sơ chưa khóa, cần hoàn tất khám và khóa hồ sơ trước khi cấp bản sao'
            }
          >
            <Tag
              color={isLocked ? 'purple' : r.status === 'ARCHIVED' ? 'default' : 'orange'}
              icon={isLocked ? <LockOutlined /> : <ClockCircleOutlined />}
              style={{ padding: '3px 8px', fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap' }}
            >
              {isLocked ? 'ĐÃ KHÓA (LOCKED)' : r.status === 'ARCHIVED' ? 'LƯU TRỮ' : 'CHƯA KHÓA'}
            </Tag>
          </Tooltip>
        )
      },
    },
    {
      title: 'Hành động',
      key: 'action',
      width: 180,
      align: 'center',
      fixed: 'right',
      render: (_, r) => {
        const isSelected = (selectedRecord?.medicalRecordId || selectedRecord?.id) === (r.medicalRecordId || r.id)
        const isLocked = r.status === 'LOCKED'

        return (
          <Space size="small" style={{ whiteSpace: 'nowrap' }}>
            <Button
              type={isSelected ? 'primary' : 'default'}
              size="small"
              icon={isSelected ? <CheckCircleOutlined /> : <FileProtectOutlined />}
              disabled={!isLocked || !canUseFeature}
              onClick={() => setSelectedRecord(r)}
              style={{
                borderColor: isSelected ? undefined : '#2563eb',
                color: isSelected ? undefined : '#2563eb',
              }}
            >
              {isSelected ? 'Đang chọn' : 'Chọn cấp'}
            </Button>
            <Button
              type="link"
              size="small"
              icon={<HistoryOutlined />}
              style={{ color: '#4f46e5', padding: '0 4px' }}
              onClick={() => {
                setSelectedVersionRecord(r)
                setVersionModalOpen(true)
              }}
            >
              Phiên bản
            </Button>
          </Space>
        )
      },
    },
  ]

  const logColumns = [
    {
      title: 'Thời gian',
      dataIndex: 'accessedAt',
      key: 'accessedAt',
      width: 170,
      render: (val) => val ? dayjs(val).format('HH:mm:ss DD/MM/YYYY') : '—',
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'accessedBy',
      key: 'accessedBy',
      width: 200,
      render: (userId) => {
        const u = userMap.get(String(userId))
        return (
          <Space>
            <Avatar size="small" icon={<UserOutlined />} style={{ backgroundColor: '#1677ff' }} />
            <Space direction="vertical" size={0}>
              <strong>{u?.fullName || u?.username || 'Quản lý phòng khám'}</strong>
              <Text type="secondary" style={{ fontSize: 11 }}>{u?.roles?.[0] || 'Quản lý'}</Text>
            </Space>
          </Space>
        )
      },
    },
    {
      title: 'Hành động',
      dataIndex: 'action',
      key: 'action',
      width: 140,
      render: (action) => {
        const isExport = action === 'EXPORT'
        return (
          <Tag
            color={isExport ? 'magenta' : 'blue'}
            icon={isExport ? <AuditOutlined /> : <EyeOutlined />}
            style={{ fontWeight: 600 }}
          >
            {isExport ? 'XUẤT BẢN SAO' : (action === 'VIEW' ? 'XEM HỒ SƠ' : action)}
          </Tag>
        )
      },
    },
    {
      title: 'Chi tiết thao tác',
      dataIndex: 'detail',
      key: 'detail',
      render: (text) => text || 'Trích lục và cấp bản sao hồ sơ bệnh án điện tử',
    },
  ]

  return (
    <div style={{ paddingBottom: 32 }}>
      <div style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <Title level={3} style={{ margin: 0, color: '#1e3a8a' }}>
              <FileProtectOutlined style={{ marginRight: 8, color: '#2563eb' }} />
              Cấp Bản Sao Hồ Sơ Bệnh Án
            </Title>
            <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
              Tiếp nhận yêu cầu, kiểm tra điều kiện hồ sơ pháp lý, xem trước và xuất bản sao trích lục hồ sơ bệnh án theo chuẩn Bộ Y Tế.
            </Paragraph>
          </div>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                if (selectedPatientId) {
                  loadPatientRecords(selectedPatientId, selectedPatient?.patientCode)
                  loadAccessLogs(selectedPatientId)
                  message.success('Đã làm mới dữ liệu hồ sơ bệnh nhân')
                }
              }}
            >
              Làm mới
            </Button>
          </Space>
        </div>
      </div>

      {!canUseFeature && (
        <Alert
          type="warning"
          showIcon
          icon={<WarningOutlined />}
          message="Quyền hạn dành riêng cho Quản lý phòng khám (Manager)"
          description={
            <span>
              Chức năng <strong>Cấp bản sao hồ sơ bệnh án</strong> là thẩm quyền hành chính thuộc về <strong>Quản lý phòng khám (Manager)</strong> và <strong>Quản trị viên (Admin)</strong>.
              <br />
              Tài khoản với vai trò khác (Bác sĩ, Lễ tân) mặc định không được phép thực hiện chức năng này trừ khi được Quản trị viên cấp quyền <strong>REPORT_EXPORT</strong> trong Ma trận phân quyền hệ thống.
            </span>
          }
          style={{ marginBottom: 16, borderRadius: 8 }}
        />
      )}

      <Card size="small" style={{ marginBottom: 16, borderRadius: 10, background: '#f8fafc', borderColor: '#e2e8f0' }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} md={10}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>
              <SearchOutlined style={{ marginRight: 6, color: '#2563eb' }} />
              Tìm kiếm & Chọn Bệnh nhân cần cấp bản sao:
            </Text>
            <Select
              showSearch
              placeholder="Tìm theo tên bệnh nhân, mã BN, số điện thoại, CCCD..."
              value={selectedPatientId}
              onChange={(val) => setSelectedPatientId(val)}
              loading={patientLoading}
              style={{ width: '100%' }}
              optionFilterProp="label"
              options={patients.map((p) => ({
                value: p.id,
                label: `${p.patientCode || 'BN'} - ${p.fullName} (${p.phone || p.identityNumber || 'Chưa có SĐT'})`,
              }))}
            />
          </Col>

          {selectedPatient && (
            <Col xs={24} md={14}>
              <div style={{ backgroundColor: '#ffffff', padding: '8px 14px', borderRadius: 8, border: '1px solid #cbd5e1' }}>
                <Row gutter={[8, 4]}>
                  <Col span={12}>
                    <Text type="secondary">Bệnh nhân:</Text>{' '}
                    <strong style={{ color: '#1e3a8a' }}>{selectedPatient.fullName}</strong> ({selectedPatient.patientCode})
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">Giới tính / Ngày sinh:</Text>{' '}
                    <strong>{selectedPatient.gender === 'FEMALE' ? 'Nữ' : 'Nam'}</strong> - {selectedPatient.dateOfBirth ? dayjs(selectedPatient.dateOfBirth).format('DD/MM/YYYY') : '—'}
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">Số CCCD/ĐD:</Text>{' '}
                    <strong>{selectedPatient.identityNumber || '—'}</strong>
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">Thẻ BHYT:</Text>{' '}
                    <strong>{selectedPatient.insuranceNumber || '—'}</strong>
                  </Col>
                </Row>
              </div>
            </Col>
          )}
        </Row>
      </Card>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        items={[
          {
            key: 'request',
            label: (
              <span>
                <SolutionOutlined /> Quy trình Cấp bản sao
              </span>
            ),
            children: (
              <div>
                <Row gutter={[16, 16]}>
                  <Col xs={24} lg={14}>
                    <Card
                      title={
                        <Space>
                          <FileTextOutlined style={{ color: '#2563eb' }} />
                          <span>1. Danh sách đợt khám & Hồ sơ bệnh án</span>
                          <Badge count={records.length} style={{ backgroundColor: '#2563eb' }} />
                        </Space>
                      }
                      size="small"
                      style={{ borderRadius: 10, marginBottom: 16 }}
                    >
                      <Table
                        rowKey={(r) => r.medicalRecordId || r.id || r.visitCode}
                        columns={recordColumns}
                        dataSource={records}
                        loading={recordsLoading}
                        pagination={false}
                        size="small"
                        scroll={{ x: 1080 }}
                        locale={{
                          emptyText: (
                            <Empty
                              image={Empty.PRESENTED_IMAGE_SIMPLE}
                              description="Bệnh nhân chưa có hồ sơ đợt khám nào trong hệ thống."
                            />
                          ),
                        }}
                      />

                      <div style={{ marginTop: 12, padding: '8px 12px', backgroundColor: '#f0f9ff', borderRadius: 6, border: '1px solid #bae6fd' }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          <InfoCircleOutlined style={{ color: '#0284c7', marginRight: 4 }} />
                          <strong>Lưu ý nghiệp vụ:</strong> Theo quy định khám chữa bệnh, chỉ những hồ sơ bệnh án đã hoàn tất đợt khám và được Bác sĩ đóng/khóa bảo mật (<strong>LOCKED</strong>) mới đủ điều kiện trích sao phục vụ thủ tục hành chính, bảo hiểm hoặc chuyển tuyến.
                        </Text>
                      </div>
                    </Card>
                  </Col>

                  <Col xs={24} lg={10}>
                    <Card
                      title={
                        <Space>
                          <SolutionOutlined style={{ color: '#2563eb' }} />
                          <span>2. Thông tin Phiếu đề nghị cấp bản sao</span>
                        </Space>
                      }
                      size="small"
                      style={{ borderRadius: 10, marginBottom: 16 }}
                    >
                      <Form form={form} layout="vertical" size="middle">
                        <Form.Item
                          name="requesterName"
                          label="Họ và tên người yêu cầu cấp:"
                          rules={[{ required: true, message: 'Vui lòng nhập họ tên người yêu cầu' }]}
                        >
                          <Input placeholder="Nhập họ tên người xin trích sao..." disabled={!canUseFeature} />
                        </Form.Item>

                        <Row gutter={12}>
                          <Col span={12}>
                            <Form.Item
                              name="relationship"
                              label="Quan hệ với bệnh nhân:"
                              rules={[{ required: true, message: 'Vui lòng chọn mối quan hệ' }]}
                            >
                              <Select options={RELATIONSHIP_OPTIONS} disabled={!canUseFeature} />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item
                              name="identityNumber"
                              label="Số CCCD người yêu cầu:"
                              rules={[{ required: true, message: 'Vui lòng nhập số CCCD' }]}
                            >
                              <Input placeholder="CCCD / Hộ chiếu..." disabled={!canUseFeature} />
                            </Form.Item>
                          </Col>
                        </Row>

                        <Form.Item
                          noStyle
                          shouldUpdate={(prev, curr) => prev.relationship !== curr.relationship}
                        >
                          {({ getFieldValue }) => {
                            const rel = getFieldValue('relationship')
                            const isAuthorized = rel && !rel.includes('Chính chủ') && !rel.includes('Bản thân')
                            return isAuthorized ? (
                              <Form.Item
                                name="authorizationDoc"
                                label="Số văn bản / Giấy ủy quyền hợp pháp (Kèm theo hồ sơ):"
                                rules={[{ required: true, message: 'Vui lòng nhập số văn bản hoặc giấy ủy quyền hợp pháp' }]}
                              >
                                <Input
                                  placeholder="Ví dụ: Giấy ủy quyền số 12/2026/UQ-PL ngày 10/08/2026..."
                                  disabled={!canUseFeature}
                                />
                              </Form.Item>
                            ) : null
                          }}
                        </Form.Item>

                        <Form.Item
                          name="purpose"
                          label="Mục đích yêu cầu cấp bản sao:"
                          rules={[{ required: true, message: 'Vui lòng chọn mục đích' }]}
                        >
                          <Select options={PURPOSE_OPTIONS} disabled={!canUseFeature} />
                        </Form.Item>

                        <Row gutter={12}>
                          <Col span={12}>
                            <Form.Item
                              name="copyCount"
                              label="Số lượng bản sao:"
                              initialValue={1}
                              rules={[{ required: true }]}
                            >
                              <InputNumber min={1} max={10} style={{ width: '100%' }} disabled={!canUseFeature} />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item name="phone" label="Số điện thoại liên hệ:">
                              <Input placeholder="SĐT người yêu cầu..." disabled={!canUseFeature} />
                            </Form.Item>
                          </Col>
                        </Row>

                        <Form.Item name="note" label="Ghi chú bổ sung (nếu có):">
                          <TextArea rows={2} placeholder="Nội dung ghi chú thêm..." disabled={!canUseFeature} />
                        </Form.Item>

                        <div
                          style={{
                            padding: '10px 14px',
                            backgroundColor: selectedRecord ? '#f8fafc' : '#fff1f2',
                            border: `1px solid ${selectedRecord ? '#cbd5e1' : '#fecdd3'}`,
                            borderRadius: 8,
                            marginBottom: 16,
                          }}
                        >
                          {selectedRecord ? (
                            <div>
                              <div style={{ fontSize: 13, fontWeight: 700, color: '#1e3a8a', marginBottom: 4 }}>
                                Hồ sơ được chọn trích sao:
                              </div>
                              <div>
                                Mã đợt khám: <strong>{selectedRecord.visit?.visitCode || selectedRecord.visitCode}</strong>
                              </div>
                              <div>
                                Trạng thái:{' '}
                                <Tag color={selectedRecord.status === 'LOCKED' ? 'purple' : 'orange'}>
                                  {selectedRecord.status}
                                </Tag>
                              </div>
                              <div style={{ marginTop: 4, fontSize: 12, color: '#475569' }}>
                                Chẩn đoán: {selectedRecord.primaryIcdName || 'Đang cập nhật'}
                              </div>
                            </div>
                          ) : (
                            <div style={{ color: '#e11d48' }}>
                              ⚠️ Vui lòng chọn một hồ sơ bệnh án ở cột bên trái để cấp bản sao.
                            </div>
                          )}
                        </div>

                        <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                          <Button
                            type="primary"
                            icon={<EyeOutlined />}
                            disabled={!selectedRecord || selectedRecord.status !== 'LOCKED' || !canUseFeature}
                            onClick={handleOpenPreview}
                            style={{ backgroundColor: '#2563eb', fontWeight: 600 }}
                          >
                            Xem trước & Xuất bản sao
                          </Button>
                        </Space>
                      </Form>
                    </Card>
                  </Col>
                </Row>
              </div>
            ),
          },
          {
            key: 'history',
            label: (
              <span>
                <HistoryOutlined /> Lịch sử Cấp & Truy cập bản sao
              </span>
            ),
            children: (
              <Card
                title={
                  <Space>
                    <AuditOutlined style={{ color: '#2563eb' }} />
                    <span>Nhật ký trích lục & xuất bản sao bệnh án của bệnh nhân</span>
                  </Space>
                }
                size="small"
                style={{ borderRadius: 10 }}
              >
                <Table
                  rowKey="id"
                  columns={logColumns}
                  dataSource={accessLogs}
                  loading={logsLoading}
                  pagination={{ pageSize: 10 }}
                  locale={{
                    emptyText: (
                      <Empty
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                        description="Chưa có lịch sử xuất bản sao nào cho bệnh nhân này."
                      />
                    ),
                  }}
                />
              </Card>
            ),
          },
        ]}
      />

      {selectedRecord && (
        <MedicalRecordCopyPreviewModal
          open={previewModalOpen}
          onClose={() => {
            setPreviewModalOpen(false)
            if (selectedPatientId) {
              loadAccessLogs(selectedPatientId)
            }
          }}
          record={selectedRecord}
          patient={selectedPatient}
          visit={selectedRecord.visit}
          diagnoses={diagnoses}
          requestInfo={form.getFieldsValue()}
          onIssued={handleCopyIssued}
        />
      )}

      <MedicalRecordVersionHistoryModal
        open={versionModalOpen}
        onClose={() => {
          setVersionModalOpen(false)
          setSelectedVersionRecord(null)
        }}
        medicalRecordId={selectedVersionRecord?.id || selectedVersionRecord?.medicalRecordId}
        recordCode={selectedVersionRecord?.visit?.visitCode || selectedVersionRecord?.visitCode || selectedVersionRecord?.recordCode}
        patientName={selectedPatient?.fullName || selectedVersionRecord?.patientName}
        patientCode={selectedPatient?.patientCode || selectedVersionRecord?.patientCode}
      />
    </div>
  )
}

export default MedicalRecordCopyPage
