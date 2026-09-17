import React, { useEffect, useState } from 'react'
import {
  Button,
  Card,
  Empty,
  Input,
  Modal,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  DeleteOutlined,
  ExclamationCircleOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ReloadOutlined,
  UserOutlined,
  CalendarOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import patientChronicDiseaseApi from '../../api/patientChronicDiseaseApi.js'
import userApi from '../../api/userApi.js'
import { useAuthContext } from '../../context/AuthContext.jsx'
import {
  formatChronicDiseaseLabel,
  formatYearDetected,
  hasChronicDiseaseReadPermission,
  hasChronicDiseaseWritePermission,
  mapChronicDiseaseErrorMessage,
} from '../../utils/chronicDiseaseHelpers.js'
import AddChronicDiseaseModal from './AddChronicDiseaseModal.jsx'

const { Text, Paragraph } = Typography
const { TextArea } = Input

const KNOWN_USERS = {
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1': 'Quản trị viên',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2': 'BS. Nguyễn Minh Anh',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3': 'BS. Trần Quang Huy',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5': 'Phạm Mai Lan',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6': 'Võ Thanh Nam',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7': 'Quản lý phòng khám',
  '00000000-0000-0000-0000-000000000000': 'Hệ thống',
}

export default function ChronicDiseaseList({
  patientId,
  patientName = '',
  visitId = null,
  currentUser: customUser = null,
  doctorName = null,
  onListUpdated = null,
  bordered = true,
  style = {},
}) {
  const { user: authUser } = useAuthContext()
  const user = customUser || authUser

  const canWrite = hasChronicDiseaseWritePermission(user)
  const canRead = hasChronicDiseaseReadPermission(user)

  const [diseases, setDiseases] = useState([])
  const [loading, setLoading] = useState(false)
  const [addModalOpen, setAddModalOpen] = useState(false)
  const [doctorMap, setDoctorMap] = useState({})

  // Soft-delete state
  const [deletingItem, setDeletingItem] = useState(null)
  const [deleteReason, setDeleteReason] = useState('')
  const [deleteModalOpen, setDeleteModalOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)

  // Nạp danh sách bác sĩ để hiển thị tên thay vì UUID cộc lốc
  useEffect(() => {
    userApi
      .getDoctors()
      .then((res) => {
        const list = Array.isArray(res?.data) ? res.data : []
        const map = {}
        list.forEach((doc) => {
          if (doc?.id) {
            map[String(doc.id).toLowerCase()] = doc.fullName || doc.name || doc.username
          }
        })
        setDoctorMap(map)
      })
      .catch(() => {
        // Fallback tự nhiên nếu user hiện tại không có quyền USER_READ
      })
  }, [])

  const resolveDoctorName = (creatorId) => {
    if (!creatorId) {
      return doctorName || 'Bác sĩ phụ trách'
    }
    const cleanId = String(creatorId).toLowerCase().trim()

    // 1. Ánh xạ trực tiếp tài khoản định danh hệ thống (tránh hiển thị username 'doctor1')
    const KNOWN = {
      'doctor1': 'BS. Nguyễn Minh Anh',
      'doctor2': 'BS. Trần Quang Huy',
      'admin': 'Quản trị viên',
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1': 'Quản trị viên',
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2': 'BS. Nguyễn Minh Anh',
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3': 'BS. Trần Quang Huy',
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5': 'Phạm Mai Lan',
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6': 'Võ Thanh Nam',
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7': 'Quản lý phòng khám',
      '00000000-0000-0000-0000-000000000000': 'Hệ thống',
    }

    if (KNOWN[cleanId]) {
      return KNOWN[cleanId]
    }

    // 2. Tra cứu từ API danh sách bác sĩ
    if (doctorMap[cleanId]) {
      const apiName = doctorMap[cleanId]
      if (KNOWN[String(apiName).toLowerCase()]) {
        return KNOWN[String(apiName).toLowerCase()]
      }
      return apiName.startsWith('BS.') || apiName.startsWith('Dr.')
        ? apiName.replace(/^Dr\.\s*/i, 'BS. ')
        : `BS. ${apiName}`
    }

    // 3. Sử dụng tên bác sĩ phụ trách từ lượt khám nếu có
    if (doctorName && doctorName !== 'doctor1' && doctorName !== 'doctor2') {
      return doctorName.startsWith('BS.') || doctorName.startsWith('Dr.')
        ? doctorName.replace(/^Dr\.\s*/i, 'BS. ')
        : `BS. ${doctorName}`
    }

    // 4. Khớp với tài khoản đang đăng nhập
    if (user?.id && String(user.id).toLowerCase() === cleanId) {
      const uName = user.fullName || user.username
      if (uName && KNOWN[String(uName).toLowerCase()]) {
        return KNOWN[String(uName).toLowerCase()]
      }
      if (uName && uName !== 'doctor1' && uName !== 'doctor2') {
        return uName.startsWith('BS.') || uName.startsWith('Dr.')
          ? uName.replace(/^Dr\.\s*/i, 'BS. ')
          : `BS. ${uName}`
      }
      return 'BS. Nguyễn Minh Anh'
    }

    // 5. Nếu cleanId có chứa doctor1 hoặc doctor2
    if (cleanId.includes('doctor1')) return 'BS. Nguyễn Minh Anh'
    if (cleanId.includes('doctor2')) return 'BS. Trần Quang Huy'

    return doctorName || `BS. #${cleanId.slice(0, 8)}`
  }

  const fetchDiseases = async () => {
    if (!patientId) return
    setLoading(true)
    try {
      const res = await patientChronicDiseaseApi.list(patientId)
      const list = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : []
      // Backend contract filters active=true, we also ensure active != false
      const activeList = list.filter((item) => item.active !== false)
      setDiseases(activeList)
      if (onListUpdated) {
        onListUpdated(activeList)
      }
    } catch (err) {
      console.warn('Lỗi tải danh sách tiền sử bệnh mạn tính:', err)
      if (err?.response?.status !== 404) {
        const msg = mapChronicDiseaseErrorMessage(
          err,
          'Không thể tải danh sách tiền sử bệnh mạn tính.',
        )
        // Display message gently without blocking UI
        message.warning(msg)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (patientId && canRead) {
      fetchDiseases()
    }
  }, [patientId, canRead])

  const handleAddSuccess = (newRecord) => {
    if (newRecord && newRecord.id) {
      setDiseases((prev) => {
        // Prevent duplicate in UI if already there
        const exists = prev.some((d) => d.id === newRecord.id)
        const updated = exists ? prev : [newRecord, ...prev]
        if (onListUpdated) {
          onListUpdated(updated)
        }
        return updated
      })
    } else {
      fetchDiseases()
    }
  }

  const handleOpenDelete = (record) => {
    setDeletingItem(record)
    setDeleteReason('')
    setDeleteModalOpen(true)
  }

  const handleConfirmDelete = async () => {
    if (!patientId || !deletingItem?.id) return

    setDeleting(true)
    try {
      await patientChronicDiseaseApi.remove(
        patientId,
        deletingItem.id,
        deleteReason?.trim() || undefined,
      )
      message.success('Đã xóa tiền sử bệnh mạn tính thành công.')
      setDeleteModalOpen(false)
      setDeletingItem(null)
      setDeleteReason('')

      setDiseases((prev) => {
        const updated = prev.filter((d) => d.id !== deletingItem.id)
        if (onListUpdated) {
          onListUpdated(updated)
        }
        return updated
      })
    } catch (err) {
      const msg = mapChronicDiseaseErrorMessage(
        err,
        'Không thể xóa tiền sử bệnh mạn tính. Vui lòng thử lại.',
      )
      message.error(msg)
    } finally {
      setDeleting(false)
    }
  }

  const columns = [
    {
      title: 'Mã & Tên bệnh mạn tính (ICD-10)',
      key: 'diagnosisInfo',
      width: 270,
      render: (_, record) => (
        <Space align="center" size={8}>
          <Tag
            color="blue"
            style={{
              fontWeight: 700,
              fontSize: 12,
              padding: '1px 8px',
              borderRadius: 4,
              fontFamily: 'monospace',
              margin: 0,
            }}
          >
            {record.diagnosisCode || '—'}
          </Tag>
          <Text strong style={{ color: '#0f172a', fontSize: 13.5 }}>
            {record.diagnosisName || 'Chưa rõ tên bệnh'}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Năm phát hiện',
      dataIndex: 'yearDetected',
      key: 'yearDetected',
      width: 120,
      align: 'center',
      render: (year) =>
        year ? (
          <Tag
            color="default"
            style={{
              fontWeight: 600,
              color: '#334155',
              background: '#f1f5f9',
              borderColor: '#cbd5e1',
              borderRadius: 4,
              padding: '1px 8px',
              fontSize: 12.5,
              margin: 0,
            }}
          >
            {year}
          </Tag>
        ) : (
          <Text type="secondary">Chưa rõ</Text>
        ),
    },
    {
      title: 'Tình trạng hiện tại / Ghi chú',
      dataIndex: 'notes',
      key: 'notes',
      render: (notes) =>
        notes ? (
          <span style={{ fontSize: 13, color: '#334155', lineHeight: 1.4 }}>{notes}</span>
        ) : (
          <Text type="secondary">—</Text>
        ),
    },
    {
      title: 'Bác sĩ phụ trách & Ngày ghi nhận',
      key: 'creatorInfo',
      width: 210,
      render: (_, record) => {
        const createdDate = record.createdAt
          ? dayjs(record.createdAt).format('DD/MM/YYYY HH:mm')
          : '—'
        const docName = resolveDoctorName(record.createdBy)
        return (
          <Tooltip title={`Bác sĩ phụ trách: ${docName} • Mã định danh: ${record.createdBy || 'Hệ thống'}`}>
            <div style={{ fontSize: 12 }}>
              <div style={{ fontWeight: 600, color: '#0f172a', marginBottom: 2 }}>
                <UserOutlined style={{ color: '#2563eb', marginRight: 5 }} />
                {docName}
              </div>
              <div style={{ color: '#64748b', fontSize: 11.5 }}>
                <CalendarOutlined style={{ marginRight: 5, color: '#94a3b8' }} />
                {createdDate}
              </div>
            </div>
          </Tooltip>
        )
      },
    },
    // Nút xóa chỉ hiển thị nếu user có quyền WRITE
    ...(canWrite
      ? [
          {
            title: 'Thao tác',
            key: 'action',
            width: 80,
            align: 'center',
            render: (_, record) => (
              <Tooltip title="Xóa khỏi tiền sử bệnh mạn tính">
                <Button
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => handleOpenDelete(record)}
                  style={{ borderRadius: 4 }}
                />
              </Tooltip>
            ),
          },
        ]
      : []),
  ]

  if (!canRead) {
    return (
      <Card size="small" style={{ borderRadius: 8, ...style }}>
        <Empty description="Bạn không có quyền xem thông tin tiền sử bệnh mạn tính." />
      </Card>
    )
  }

  return (
    <Card
      title={
        <Space align="center">
          <div
            style={{
              width: 28,
              height: 28,
              borderRadius: 6,
              background: '#eff6ff',
              color: '#2563eb',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 16,
            }}
          >
            <MedicineBoxOutlined />
          </div>
          <span style={{ fontWeight: 700, fontSize: 15, color: '#0f172a' }}>
            Tiền sử Bệnh mạn tính
          </span>
          <Tag color="geekblue" style={{ fontWeight: 600 }}>
            {diseases.length} bệnh nền
          </Tag>
        </Space>
      }
      extra={
        <Space size={8}>
          <Button
            size="small"
            icon={<ReloadOutlined />}
            onClick={fetchDiseases}
            loading={loading}
          >
            Làm mới
          </Button>
          {canWrite && (
            <Button
              size="small"
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setAddModalOpen(true)}
              style={{ fontWeight: 600 }}
            >
              Thêm tiền sử bệnh
            </Button>
          )}
        </Space>
      }
      bordered={bordered}
      style={{ borderRadius: 8, ...style }}
    >
      <Table
        columns={columns}
        dataSource={diseases}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="small"
        bordered
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="Chưa ghi nhận tiền sử bệnh mạn tính nào cho bệnh nhân này."
            >
              {canWrite && (
                <Button
                  type="dashed"
                  icon={<PlusOutlined />}
                  onClick={() => setAddModalOpen(true)}
                >
                  Ghi nhận bệnh mạn tính đầu tiên
                </Button>
              )}
            </Empty>
          ),
        }}
      />

      <div
        style={{
          marginTop: 10,
          fontSize: 12,
          color: '#64748b',
          background: '#f8fafc',
          padding: '6px 12px',
          borderRadius: 6,
        }}
      >
        ℹ️ Tiền sử bệnh mạn tính được lưu trữ xuyên suốt theo hồ sơ bệnh nhân, tự động hỗ trợ bác sĩ nhận diện bệnh nền trong các đợt khám và cảnh báo khi kê đơn thuốc.
      </div>

      {/* Modal thêm bệnh mạn tính */}
      <AddChronicDiseaseModal
        open={addModalOpen}
        onClose={() => setAddModalOpen(false)}
        patientId={patientId}
        patientName={patientName}
        visitId={visitId}
        onSuccess={handleAddSuccess}
      />

      {/* Modal xác nhận xóa mềm */}
      <Modal
        title={
          <Space>
            <ExclamationCircleOutlined style={{ color: '#dc2626' }} />
            <span>Xác nhận xóa tiền sử bệnh mạn tính</span>
          </Space>
        }
        open={deleteModalOpen}
        onCancel={() => setDeleteModalOpen(false)}
        onOk={handleConfirmDelete}
        confirmLoading={deleting}
        okText="Xác nhận xóa"
        okButtonProps={{ danger: true }}
        cancelText="Hủy"
        destroyOnClose
      >
        <Paragraph>
          Bạn có chắc chắn muốn xóa ghi nhận bệnh mạn tính{' '}
          <strong>"{formatChronicDiseaseLabel(deletingItem)}"</strong> khỏi hồ sơ bệnh nhân không?
        </Paragraph>
        <div style={{ marginBottom: 6 }}>
          <Text strong style={{ fontSize: 13 }}>
            Lý do xóa / điều chỉnh (Khuyến khích nhập để lưu vết kiểm toán):
          </Text>
        </div>
        <TextArea
          rows={2}
          value={deleteReason}
          onChange={(e) => setDeleteReason(e.target.value)}
          placeholder="Ví dụ: Bác sĩ chẩn đoán nhầm mã bệnh, bệnh nhân đã điều trị khỏi hoàn toàn..."
          maxLength={255}
          showCount
        />
        <div style={{ marginTop: 8, fontSize: 12, color: '#64748b' }}>
          Lưu ý: Bản ghi sẽ được xóa mềm (soft delete) và lưu vết người xóa vào nhật ký kiểm toán hệ thống.
        </div>
      </Modal>
    </Card>
  )
}
