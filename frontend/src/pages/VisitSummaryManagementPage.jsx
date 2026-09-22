import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Input,
  Radio,
  Row,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DownloadOutlined,
  EyeOutlined,
  FilePdfOutlined,
  MedicineBoxOutlined,
  PrinterOutlined,
  ReloadOutlined,
  SearchOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import visitSummaryApi from '../api/visitSummaryApi.js'
import patientApi from '../api/patientApi.js'
import VisitSummaryPrintModal from '../components/clinical/VisitSummaryPrintModal.jsx'
import { useAuthContext } from '../context/AuthContext.jsx'
import queueApi from '../api/queueApi.js'
import axiosClient from '../api/axiosClient.js'
import { isMedicalRecordSigned } from '../utils/medicalRecordSignHelpers.js'
import {
  calculateAgeFromDob,
  formatDateTimeVi,
  formatDateVi,
  formatGenderVi,
  getRecordStatusBadge,
} from '../utils/visitSummaryHelpers.js'
import { getApiErrorMessage } from '../utils/apiError.js'
import '../styles/visitSummaryPrint.css'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

export default function VisitSummaryManagementPage() {
  const { user } = useAuthContext()

  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) =>
      String(p || '').toUpperCase().replace(/^PERMISSION_/, '')
    )
  }, [user])

  const userRoles = useMemo(() => {
    return (user?.roles || []).map((r) =>
      String(r || '').toLowerCase().replace(/^role_/, '')
    )
  }, [user])

  const isAdmin = userRoles.includes('admin')
  const isDoctor = userRoles.includes('doctor')
  const isReceptionist = userRoles.includes('receptionist')
  const isManager = userRoles.includes('manager') || userRoles.includes('clinic_manager')
  const canAccess =
    isAdmin || isDoctor || isReceptionist || isManager || userPermissions.includes('VISIT_SUMMARY_PRINT')

  const [loading, setLoading] = useState(false)
  const [visits, setVisits] = useState([])
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [dateRange, setDateRange] = useState(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  // Modal in phiếu tóm tắt
  const [selectedVisitId, setSelectedVisitId] = useState(null)
  const [printModalOpen, setPrintModalOpen] = useState(false)
  const [downloadingId, setDownloadingId] = useState(null)

  // Nạp danh sách lượt khám trực tiếp từ Backend (không lặp N+1, không dùng slice giả lập)
  const fetchVisits = useCallback(async () => {
    setLoading(true)
    try {
      const targetDate = dateRange && dateRange[0]
        ? dateRange[0].format('YYYY-MM-DD')
        : dayjs().format('YYYY-MM-DD')

      const allVisitsMap = new Map()

      // 1. Tải lượt khám trong ngày từ hàng đợi tiếp nhận & khám bệnh (hỗ trợ cả lễ tân và bác sĩ)
      const [queueRes, payableRes] = await Promise.allSettled([
        queueApi.getQueues({ date: targetDate }),
        axiosClient.get('/invoices/payable', { params: { page: 0, size: 50 } }),
      ])

      if (queueRes.status === 'fulfilled' && Array.isArray(queueRes.value?.data)) {
        queueRes.value.data.forEach((item) => {
          if (item.visitId) {
            allVisitsMap.set(String(item.visitId), {
              visitId: item.visitId,
              visitCode: item.visitCode || `KB-${String(item.visitId).slice(0, 8).toUpperCase()}`,
              visitAt: item.completedAt || item.calledAt || item.checkedInAt || new Date().toISOString(),
              patient: {
                id: item.patientId,
                patientCode: item.patientCode || 'BN-0000',
                fullName: item.patientName || 'Bệnh nhân',
                dateOfBirth: item.patientDob,
                gender: item.patientGender,
                phone: item.patientPhone,
              },
              doctor: {
                id: item.doctorId,
                fullName: item.doctorName || 'Bác sĩ phụ trách',
              },
              medicalRecord: {
                status: item.status === 'COMPLETED' ? 'SIGNED' : (item.medicalRecordStatus || 'IN_PROGRESS'),
                signedAt: item.completedAt,
                signedByName: item.doctorName,
              },
              primaryDiagnosis: item.primaryDiagnosis || '',
              diagnosisCode: item.diagnosisCode || '',
            })
          }
        })
      }

      // 2. Tải lượt khám đã hoàn tất ca khám có thể thanh toán / in phiếu
      if (payableRes.status === 'fulfilled') {
        const payableList = Array.isArray(payableRes.value?.data?.content)
          ? payableRes.value.data.content
          : Array.isArray(payableRes.value?.data)
          ? payableRes.value.data
          : []
        payableList.forEach((item) => {
          if (item.visitId && !allVisitsMap.has(String(item.visitId))) {
            allVisitsMap.set(String(item.visitId), {
              visitId: item.visitId,
              visitCode: item.visitCode || `KB-${String(item.visitId).slice(0, 8).toUpperCase()}`,
              visitAt: item.completedAt || new Date().toISOString(),
              patient: {
                id: item.patientId,
                patientCode: item.patientCode || 'BN-0000',
                fullName: item.patientName || 'Bệnh nhân',
              },
              doctor: {
                fullName: item.doctorName || 'Bác sĩ phụ trách',
              },
              medicalRecord: {
                status: 'SIGNED',
                signedAt: item.completedAt,
              },
              primaryDiagnosis: item.reason || '',
              diagnosisCode: '',
            })
          }
        })
      }

      // 3. Nếu người dùng nhập từ khóa tìm kiếm bệnh nhân, tra cứu trực tiếp hồ sơ phù hợp
      if (keyword.trim()) {
        try {
          const patientSearchRes = await patientApi.getAll({ keyword: keyword.trim(), page: 0, size: 5 })
          const matchedPatients = Array.isArray(patientSearchRes.data?.content)
            ? patientSearchRes.data.content
            : Array.isArray(patientSearchRes.data)
            ? patientSearchRes.data
            : []

          if (matchedPatients.length > 0) {
            const histResponses = await Promise.allSettled(
              matchedPatients.map((p) => patientApi.getHistory(p.id, { page: 0, size: 10 }))
            )
            histResponses.forEach((hr, idx) => {
              if (hr.status === 'fulfilled') {
                const histItems = Array.isArray(hr.value?.data?.content)
                  ? hr.value.data.content
                  : Array.isArray(hr.value?.data)
                  ? hr.value.data
                  : []
                const p = matchedPatients[idx]
                histItems.forEach((h) => {
                  const vId = h.visitId || h.id
                  if (vId) {
                    allVisitsMap.set(String(vId), {
                      visitId: vId,
                      visitCode: h.visitCode || `KB-${String(vId).slice(0, 8).toUpperCase()}`,
                      visitAt: h.visitAt || h.createdAt || new Date().toISOString(),
                      patient: {
                        id: p.id,
                        patientCode: p.patientCode || 'BN-0000',
                        fullName: p.fullName || p.name,
                        dateOfBirth: p.dateOfBirth,
                        gender: p.gender,
                        phone: p.phone,
                      },
                      doctor: {
                        id: h.doctorId,
                        fullName: h.doctorName || h.doctorFullName || 'Bác sĩ phụ trách',
                      },
                      medicalRecord: {
                        id: h.medicalRecordId,
                        status: h.medicalRecordStatus || (h.signedAt ? 'SIGNED' : 'IN_PROGRESS'),
                        signedAt: h.signedAt,
                        signedByName: h.signedByName || h.doctorName,
                      },
                      primaryDiagnosis: h.primaryDiagnosis || h.diagnosis || '',
                      diagnosisCode: h.diagnosisCode || '',
                    })
                  }
                })
              }
            })
          }
        } catch {
          // Bỏ qua lỗi tra cứu phụ
        }
      }

      const visitsList = Array.from(allVisitsMap.values())
      visitsList.sort((a, b) => dayjs(b.visitAt).valueOf() - dayjs(a.visitAt).valueOf())
      setVisits(visitsList)
    } catch (err) {
      message.error(getApiErrorMessage(err, 'Không thể nạp danh sách lượt khám.'))
    } finally {
      setLoading(false)
    }
  }, [dateRange, keyword])

  useEffect(() => {
    fetchVisits()
  }, [fetchVisits])

  // Lọc dữ liệu theo từ khóa, trạng thái và khoảng thời gian
  const filteredVisits = useMemo(() => {
    return visits.filter((v) => {
      // Lọc từ khóa
      if (keyword.trim()) {
        const term = keyword.trim().toLowerCase()
        const matchCode = String(v.visitCode || '').toLowerCase().includes(term)
        const matchPatientCode = String(v.patient?.patientCode || '').toLowerCase().includes(term)
        const matchPatientName = String(v.patient?.fullName || '').toLowerCase().includes(term)
        const matchDoctor = String(v.doctor?.fullName || '').toLowerCase().includes(term)
        if (!matchCode && !matchPatientCode && !matchPatientName && !matchDoctor) {
          return false
        }
      }

      // Lọc trạng thái bệnh án
      const isSigned = isMedicalRecordSigned(v.medicalRecord)
      if (statusFilter === 'SIGNED' && !isSigned) return false
      if (statusFilter === 'UNSIGNED' && isSigned) return false

      // Lọc khoảng ngày khám
      if (dateRange && dateRange[0] && dateRange[1]) {
        const visitTime = dayjs(v.visitAt)
        const start = dateRange[0].startOf('day')
        const end = dateRange[1].endOf('day')
        if (visitTime.isBefore(start) || visitTime.isAfter(end)) {
          return false
        }
      }

      return true
    })
  }, [visits, keyword, statusFilter, dateRange])

  // Thống kê KPI toàn hệ thống
  const stats = useMemo(() => {
    const total = visits.length
    const signedCount = visits.filter((v) => isMedicalRecordSigned(v.medicalRecord)).length
    const unsignedCount = total - signedCount
    return { total, signedCount, unsignedCount }
  }, [visits])

  // Xử lý đổi từ khóa & reset về trang 1
  const handleKeywordChange = (e) => {
    setKeyword(e.target.value)
    setCurrentPage(1)
  }

  // Xử lý đổi bộ lọc trạng thái & reset về trang 1
  const handleStatusFilterChange = (e) => {
    setStatusFilter(e.target.value)
    setCurrentPage(1)
  }

  // Mở modal in phiếu tóm tắt
  const handleOpenPrintModal = (visit) => {
    const isSigned = isMedicalRecordSigned(visit.medicalRecord)
    if (!isSigned) {
      message.warning(
        'Bệnh án của lượt khám chưa được ký. Vui lòng ký bệnh án trước khi in phiếu tóm tắt.'
      )
      return
    }
    setSelectedVisitId(visit.visitId)
    setPrintModalOpen(true)
  }

  // Tải trực tiếp file PDF
  const handleDownloadDirectPdf = async (visit) => {
    const isSigned = isMedicalRecordSigned(visit.medicalRecord)
    if (!isSigned) {
      message.warning(
        'Bệnh án của lượt khám chưa được ký. Không thể xuất tệp PDF phiếu tóm tắt.'
      )
      return
    }

    setDownloadingId(visit.visitId)
    try {
      await visitSummaryApi.downloadPdf(visit.visitId, `phieu-tom-tat-${visit.visitCode}.pdf`)
      message.success(`Đã tải xuống phiếu tóm tắt lượt khám ${visit.visitCode}!`)
    } catch (err) {
      message.error(getApiErrorMessage(err, 'Không thể tải xuống tệp PDF.'))
    } finally {
      setDownloadingId(null)
    }
  }

  // Cấu hình các cột hiển thị của bảng
  const columns = [
    {
      title: 'Mã lượt khám',
      dataIndex: 'visitCode',
      key: 'visitCode',
      width: 150,
      render: (code) => (
        <span style={{ fontFamily: 'monospace', fontWeight: 700, color: '#0284c7' }}>
          {code}
        </span>
      ),
    },
    {
      title: 'Thời gian khám',
      dataIndex: 'visitAt',
      key: 'visitAt',
      width: 160,
      render: (date) => (
        <Space direction="vertical" size={2}>
          <span style={{ fontWeight: 600 }}>{formatDateTimeVi(date)}</span>
        </Space>
      ),
    },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      width: 220,
      render: (_, record) => {
        const p = record.patient
        return (
          <div>
            <div style={{ fontWeight: 700, color: '#0f172a' }}>{p?.fullName || '---'}</div>
            <div style={{ fontSize: 12, color: '#64748b' }}>
              Mã BN: <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{p?.patientCode}</span> |{' '}
              {formatGenderVi(p?.gender)} ({calculateAgeFromDob(p?.dateOfBirth)})
            </div>
          </div>
        )
      },
    },
    {
      title: 'Bác sĩ phụ trách',
      dataIndex: ['doctor', 'fullName'],
      key: 'doctor',
      width: 180,
      render: (name) => (
        <Space size={6}>
          <UserOutlined style={{ color: '#0284c7' }} />
          <span style={{ fontWeight: 500 }}>{name || '---'}</span>
        </Space>
      ),
    },
    {
      title: 'Trạng thái bệnh án',
      key: 'status',
      width: 170,
      render: (_, record) => {
        const badge = getRecordStatusBadge(record.medicalRecord?.status)
        return (
          <Tooltip
            title={
              badge.isSigned
                ? `Đã ký hợp lệ lúc ${formatDateTimeVi(record.medicalRecord?.signedAt)}`
                : 'Bệnh án chưa được ký. Cần ký để in phiếu tóm tắt.'
            }
          >
            <Tag
              color={badge.color}
              style={{
                borderRadius: 6,
                padding: '2px 10px',
                fontSize: 13,
                fontWeight: 600,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
              }}
            >
              {badge.isSigned ? <CheckCircleOutlined /> : <ClockCircleOutlined />}
              {badge.text}
            </Tag>
          </Tooltip>
        )
      },
    },
    {
      title: 'Chẩn đoán chính',
      key: 'diagnosis',
      ellipsis: true,
      render: (_, record) => {
        if (!record.primaryDiagnosis && !record.diagnosisCode) {
          return <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa ghi nhận</span>
        }
        return (
          <Tooltip title={record.primaryDiagnosis || record.diagnosisCode}>
            <span>
              {record.diagnosisCode && (
                <Tag color="geekblue" style={{ fontFamily: 'monospace', fontWeight: 600 }}>
                  {record.diagnosisCode}
                </Tag>
              )}
              <span style={{ fontWeight: 500 }}>{record.primaryDiagnosis || ''}</span>
            </span>
          </Tooltip>
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 130,
      align: 'center',
      fixed: 'right',
      render: (_, record) => {
        const isSigned = isMedicalRecordSigned(record.medicalRecord)
        return (
          <Space size={8}>
            <Tooltip title={isSigned ? 'Xem trước & In phiếu tóm tắt' : 'Nhắc nhở: Bệnh án chưa ký'}>
              <Button
                type="text"
                icon={<PrinterOutlined style={{ fontSize: 17, color: isSigned ? '#0284c7' : '#f59e0b' }} />}
                onClick={() => handleOpenPrintModal(record)}
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: 8,
                  background: isSigned ? '#f0f9ff' : '#fffbeb',
                  border: isSigned ? '1px solid #bae6fd' : '1px solid #fde68a',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              />
            </Tooltip>

            <Tooltip title={isSigned ? 'Tải tệp PDF' : 'Bệnh án chưa ký, không thể tải PDF'}>
              <Button
                type="text"
                icon={<DownloadOutlined style={{ fontSize: 17, color: isSigned ? '#16a34a' : '#94a3b8' }} />}
                disabled={!isSigned}
                loading={downloadingId === record.visitId}
                onClick={() => handleDownloadDirectPdf(record)}
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: 8,
                  background: isSigned ? '#f0fdf4' : '#f8fafc',
                  border: isSigned ? '1px solid #bbf7d0' : '1px solid #e2e8f0',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              />
            </Tooltip>
          </Space>
        )
      },
    },
  ]

  if (!canAccess) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          showIcon
          message="Từ chối truy cập"
          description="Bạn không có quyền xem hoặc in phiếu tóm tắt lượt khám. Vui lòng liên hệ Quản trị viên."
        />
      </div>
    )
  }

  return (
    <div className="specialty-management-container" style={{ padding: '24px 32px' }}>
      {/* Tiêu đề trang (Sạch sẽ, không mô tả phụ theo AGENTS.md) */}
      <div style={{ marginBottom: 20 }}>
        <Title
          level={2}
          style={{
            margin: 0,
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            fontWeight: 700,
            color: '#0f172a',
          }}
        >
          <PrinterOutlined style={{ color: '#0284c7' }} />
          <span>Quản lý & In phiếu tóm tắt lượt khám</span>
        </Title>
      </div>

      {/* Thẻ thống kê KPI */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={8}>
          <Card
            className="specialty-kpi-card"
            bodyStyle={{ padding: '16px 20px' }}
            style={{ borderRadius: 10, border: '1px solid #e2e8f0' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary" style={{ fontSize: 13, fontWeight: 600 }}>
                  TỔNG SỐ LƯỢT KHÁM
                </Text>
                <div style={{ fontSize: 28, fontWeight: 800, color: '#0f172a', marginTop: 4 }}>
                  {stats.total}
                </div>
              </div>
              <div
                style={{
                  width: 46,
                  height: 46,
                  borderRadius: 10,
                  background: '#f0f9ff',
                  color: '#0284c7',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 20,
                }}
              >
                <MedicineBoxOutlined />
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card
            className="specialty-kpi-card"
            bodyStyle={{ padding: '16px 20px' }}
            style={{ borderRadius: 10, border: '1px solid #e2e8f0' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary" style={{ fontSize: 13, fontWeight: 600 }}>
                  BỆNH ÁN ĐÃ KÝ (SẴN SÀNG IN)
                </Text>
                <div style={{ fontSize: 28, fontWeight: 800, color: '#16a34a', marginTop: 4 }}>
                  {stats.signedCount}
                </div>
              </div>
              <div
                style={{
                  width: 46,
                  height: 46,
                  borderRadius: 10,
                  background: '#f0fdf4',
                  color: '#16a34a',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 20,
                }}
              >
                <CheckCircleOutlined />
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card
            className="specialty-kpi-card"
            bodyStyle={{ padding: '16px 20px' }}
            style={{ borderRadius: 10, border: '1px solid #e2e8f0' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary" style={{ fontSize: 13, fontWeight: 600 }}>
                  BỆNH ÁN CHƯA KÝ (CẦN KÝ TRƯỚC)
                </Text>
                <div style={{ fontSize: 28, fontWeight: 800, color: '#f59e0b', marginTop: 4 }}>
                  {stats.unsignedCount}
                </div>
              </div>
              <div
                style={{
                  width: 46,
                  height: 46,
                  borderRadius: 10,
                  background: '#fffbeb',
                  color: '#f59e0b',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 20,
                }}
              >
                <ClockCircleOutlined />
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Thanh công cụ tìm kiếm & lọc */}
      <Card
        style={{ marginBottom: 20, borderRadius: 10, border: '1px solid #e2e8f0' }}
        bodyStyle={{ padding: '16px 20px' }}
      >
        <Row gutter={[16, 16]} align="middle" justify="space-between">
          <Col xs={24} xl={16}>
            <Space wrap size="middle">
              <Input
                placeholder="Tìm theo mã lượt khám, mã hoặc tên bệnh nhân..."
                prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
                allowClear
                value={keyword}
                onChange={handleKeywordChange}
                style={{ width: 320, borderRadius: 8, height: 40 }}
              />

              <Radio.Group
                value={statusFilter}
                onChange={handleStatusFilterChange}
                buttonStyle="solid"
              >
                <Radio.Button value="ALL">Tất cả ({stats.total})</Radio.Button>
                <Radio.Button value="SIGNED">Đã ký ({stats.signedCount})</Radio.Button>
                <Radio.Button value="UNSIGNED">Chưa ký ({stats.unsignedCount})</Radio.Button>
              </Radio.Group>

              <RangePicker
                placeholder={['Từ ngày', 'Đến ngày']}
                format="DD/MM/YYYY"
                value={dateRange}
                onChange={(dates) => {
                  setDateRange(dates)
                  setCurrentPage(1)
                }}
                style={{ borderRadius: 8, height: 40 }}
              />
            </Space>
          </Col>

          <Col xs={24} xl={8} style={{ textAlign: 'right' }}>
            <Button
              icon={<ReloadOutlined />}
              onClick={fetchVisits}
              loading={loading}
              style={{ borderRadius: 8, height: 40, minWidth: 110, fontSize: 14 }}
            >
              Làm mới
            </Button>
          </Col>
        </Row>
      </Card>

      {/* Bảng danh sách lượt khám */}
      <Card style={{ borderRadius: 10, border: '1px solid #e2e8f0' }} bodyStyle={{ padding: 0 }}>
        <Table
          columns={columns}
          dataSource={filteredVisits}
          rowKey="visitId"
          loading={loading}
          pagination={{
            current: currentPage,
            pageSize: pageSize,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            showTotal: (total, range) => `${range[0]}-${range[1]} trong số ${total} lượt khám`,
            onChange: (page, size) => {
              setCurrentPage(page)
              setPageSize(size)
            },
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Không tìm thấy lượt khám nào phù hợp"
              />
            ),
          }}
        />
      </Card>

      {/* Modal xem trước & In phiếu tóm tắt */}
      <VisitSummaryPrintModal
        open={printModalOpen}
        visitId={selectedVisitId}
        onClose={() => {
          setPrintModalOpen(false)
          setSelectedVisitId(null)
        }}
        onPrinted={() => {
          fetchVisits()
        }}
      />
    </div>
  )
}
