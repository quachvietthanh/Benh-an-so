import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  FileTextOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  StarFilled,
  StarOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import medicalRecordTemplateApi from '../api/medicalRecordTemplateApi'
import TemplateFormModal from '../components/medicalRecordTemplate/TemplateFormModal'
import StatusToggleModal from '../components/medicalRecordTemplate/StatusToggleModal'
import { useAuthContext } from '../context/AuthContext'
import './medicalRecordTemplateManagement.css'

const { Title, Text, Paragraph } = Typography

function MedicalRecordTemplateManagementPage() {
  const { user } = useAuthContext()
  const userPermissions = (user?.permissions || []).map((p) =>
    String(p || '').toUpperCase().replace(/^PERMISSION_/, '')
  )
  const userRoles = (user?.roles || []).map((r) =>
    String(r || '').toLowerCase().replace(/^role_/, '')
  )
  const isAdmin = userRoles.includes('admin')
  const canManage = userPermissions.includes('MEDICAL_RECORD_TEMPLATE_MANAGE') || isAdmin

  // State
  const [specialties, setSpecialties] = useState([])
  const [templates, setTemplates] = useState([])
  const [loading, setLoading] = useState(false)
  const [specialtyLoading, setSpecialtyLoading] = useState(false)

  // Filters
  const [selectedSpecialty, setSelectedSpecialty] = useState('ALL')
  const [selectedStatus, setSelectedStatus] = useState('ALL')
  const [searchQuery, setSearchQuery] = useState('')

  // Modals state
  const [formModalOpen, setFormModalOpen] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState(null)
  const [statusModalOpen, setStatusModalOpen] = useState(false)
  const [statusTargetTemplate, setStatusTargetTemplate] = useState(null)
  const [modalLoading, setModalLoading] = useState(false)

  // 1. Fetch Specialties
  const fetchSpecialties = useCallback(async () => {
    setSpecialtyLoading(true)
    try {
      const res = await medicalRecordTemplateApi.getSpecialties()
      const list = res.data || []
      setSpecialties(list)
    } catch (err) {
      message.error('Không thể tải danh mục chuyên khoa.')
    } finally {
      setSpecialtyLoading(false)
    }
  }, [])

  // 2. Fetch Templates
  const fetchTemplates = useCallback(async () => {
    setLoading(true)
    try {
      const params = {}
      if (selectedSpecialty !== 'ALL') {
        params.specialtyId = selectedSpecialty
      }
      if (selectedStatus === 'ACTIVE') {
        params.active = true
      } else if (selectedStatus === 'INACTIVE') {
        params.active = false
      }

      const res = await medicalRecordTemplateApi.searchTemplates(params)
      setTemplates(res.data || [])
    } catch (err) {
      message.error('Không thể tải danh sách mẫu bệnh án.')
    } finally {
      setLoading(false)
    }
  }, [selectedSpecialty, selectedStatus])

  useEffect(() => {
    fetchSpecialties()
  }, [fetchSpecialties])

  useEffect(() => {
    fetchTemplates()
  }, [fetchTemplates])

  // Filtered templates by search query
  const filteredTemplates = useMemo(() => {
    let result = templates || []
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase()
      result = result.filter(
        (t) =>
          t.name.toLowerCase().includes(q) ||
          t.specialty?.name?.toLowerCase().includes(q) ||
          t.specialty?.code?.toLowerCase().includes(q)
      )
    }
    return result
  }, [templates, searchQuery])

  // Handlers
  const handleOpenCreateModal = () => {
    setEditingTemplate(null)
    setFormModalOpen(true)
  }

  const handleOpenEditModal = async (record) => {
    setLoading(true)
    try {
      const res = await medicalRecordTemplateApi.getTemplateById(record.id)
      setEditingTemplate(res.data)
      setFormModalOpen(true)
    } catch (err) {
      message.error('Không thể tải chi tiết cấu hình mẫu bệnh án.')
    } finally {
      setLoading(false)
    }
  }

  const handleFormSubmit = async (payload) => {
    setModalLoading(true)
    try {
      if (editingTemplate) {
        await medicalRecordTemplateApi.updateTemplate(editingTemplate.id, payload)
        message.success(`Đã cập nhật mẫu "${payload.name}" và nâng lên phiên bản mới!`)
      } else {
        await medicalRecordTemplateApi.createTemplate(payload)
        message.success(`Đã tạo mẫu bệnh án "${payload.name}" thành công!`)
      }
      setFormModalOpen(false)
      setEditingTemplate(null)
      fetchTemplates()
    } finally {
      setModalLoading(false)
    }
  }

  const handleSetDefault = async (record) => {
    try {
      await medicalRecordTemplateApi.setDefaultTemplate(record.id)
      message.success(`Đã đặt "${record.name}" làm mẫu mặc định cho chuyên khoa ${record.specialty?.name || ''}!`)
      fetchTemplates()
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể thiết lập mẫu mặc định.'
      message.error(msg)
    }
  }

  const handleOpenStatusModal = (record) => {
    setStatusTargetTemplate(record)
    setStatusModalOpen(true)
  }

  const handleStatusSubmit = async (payload) => {
    if (!statusTargetTemplate) return
    setModalLoading(true)
    try {
      await medicalRecordTemplateApi.updateTemplateStatus(statusTargetTemplate.id, payload)
      message.success(
        payload.active
          ? `Đã kích hoạt mẫu "${statusTargetTemplate.name}"!`
          : `Đã ngừng áp dụng mẫu "${statusTargetTemplate.name}"!`
      )
      setStatusModalOpen(false)
      setStatusTargetTemplate(null)
      fetchTemplates()
    } finally {
      setModalLoading(false)
    }
  }

  // Table Columns
  const columns = [
    {
      title: 'Tên mẫu bệnh án',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <div className="template-name-cell">
          <div className="template-name-title">
            <span>{text}</span>
            {record.defaultTemplate && (
              <span className="template-badge-default">
                <StarFilled style={{ color: '#059669', marginRight: 4 }} />
                Mặc định
              </span>
            )}
          </div>
          <Text type="secondary" style={{ fontSize: 11 }}>
            ID: {record.id}
          </Text>
        </div>
      ),
    },
    {
      title: 'Chuyên khoa',
      dataIndex: 'specialty',
      key: 'specialty',
      width: 180,
      render: (specialty) => (
        <Tag color="blue" style={{ fontWeight: 500, fontSize: 12 }}>
          {specialty?.name || specialty?.code || '—'}
        </Tag>
      ),
    },
    {
      title: 'Phiên bản',
      dataIndex: 'currentVersionNo',
      key: 'currentVersionNo',
      width: 110,
      align: 'center',
      render: (versionNo) => (
        <Tag color="purple" style={{ fontWeight: 600 }}>
          v{versionNo || 1}
        </Tag>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 140,
      align: 'center',
      render: (active) => (
        <Tag color={active ? 'success' : 'default'} style={{ fontWeight: 500 }}>
          {active ? (
            <span><CheckCircleOutlined /> Đang áp dụng</span>
          ) : (
            <span><CloseCircleOutlined /> Ngừng áp dụng</span>
          )}
        </Tag>
      ),
    },
    {
      title: 'Cập nhật',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 160,
      render: (val, record) => {
        const time = val || record.createdAt
        return time ? dayjs(time).format('DD/MM/YYYY HH:mm') : '—'
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 220,
      align: 'right',
      render: (_, record) => (
        <Space size={6}>
          {/* Sửa */}
          <Tooltip title="Chỉnh sửa cấu hình trường và tạo phiên bản mới">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              disabled={!canManage}
              onClick={() => handleOpenEditModal(record)}
            >
              Sửa
            </Button>
          </Tooltip>

          {/* Đặt mặc định */}
          {!record.defaultTemplate && record.active && (
            <Popconfirm
              title="Đặt làm mẫu mặc định?"
              description={`Mẫu "${record.name}" sẽ trở thành mẫu mặc định khi bác sĩ khám chuyên khoa ${record.specialty?.name}.`}
              onConfirm={() => handleSetDefault(record)}
              okText="Đồng ý"
              cancelText="Hủy"
              disabled={!canManage}
            >
              <Tooltip title="Đặt làm mẫu mặc định cho chuyên khoa này">
                <Button
                  type="link"
                  size="small"
                  icon={<StarOutlined />}
                  disabled={!canManage}
                  style={{ color: '#d97706' }}
                >
                  Mặc định
                </Button>
              </Tooltip>
            </Popconfirm>
          )}

          {/* Bật / Tắt */}
          <Tooltip title={record.active ? 'Ngừng áp dụng mẫu này' : 'Kích hoạt lại mẫu này'}>
            <Button
              type="link"
              size="small"
              danger={record.active}
              disabled={!canManage}
              onClick={() => handleOpenStatusModal(record)}
            >
              {record.active ? 'Tắt' : 'Bật'}
            </Button>
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div className="template-management-page">
      {/* Page Header */}
      <div className="template-management-header">
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Title level={3} style={{ margin: 0 }}>
              <FileTextOutlined style={{ color: '#2563eb', marginRight: 8 }} />
              Quản lý Mẫu bệnh án theo chuyên khoa
            </Title>
            <Paragraph type="secondary" style={{ margin: '4px 0 0', fontSize: 13.5 }}>
              Định nghĩa danh mục các trường thông tin chuẩn, phân loại theo chuyên khoa và bảo toàn lịch sử phiên bản bệnh án.
            </Paragraph>
          </Col>
          <Col>
            <Space>
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  fetchSpecialties()
                  fetchTemplates()
                }}
              >
                Làm mới
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                disabled={!canManage}
                onClick={handleOpenCreateModal}
                style={{ background: '#2563eb', borderColor: '#2563eb' }}
              >
                Tạo mẫu bệnh án mới
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      {!canManage && (
        <Alert
          type="warning"
          showIcon
          icon={<ExclamationCircleOutlined />}
          message="Chế độ chỉ xem"
          description="Tài khoản hiện tại không có quyền MEDICAL_RECORD_TEMPLATE_MANAGE. Bạn chỉ có thể xem danh sách mẫu bệnh án mà không thể thay đổi."
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Filter Card */}
      <Card className="template-filter-card" bodyStyle={{ padding: '16px 20px' }}>
        <Row gutter={[16, 12]} align="middle">
          <Col xs={24} sm={8} md={6}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>
              Lọc theo chuyên khoa:
            </div>
            <Select
              style={{ width: '100%' }}
              value={selectedSpecialty}
              onChange={setSelectedSpecialty}
              loading={specialtyLoading}
              options={[
                { value: 'ALL', label: 'Tất cả chuyên khoa' },
                ...specialties.map((s) => ({
                  value: s.id,
                  label: `${s.name} (${s.code})`,
                })),
              ]}
            />
          </Col>

          <Col xs={24} sm={8} md={6}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>
              Trạng thái áp dụng:
            </div>
            <Select
              style={{ width: '100%' }}
              value={selectedStatus}
              onChange={setSelectedStatus}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'ACTIVE', label: 'Đang áp dụng (Active)' },
                { value: 'INACTIVE', label: 'Ngừng áp dụng (Inactive)' },
              ]}
            />
          </Col>

          <Col xs={24} sm={8} md={8}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>
              Tìm kiếm tên mẫu:
            </div>
            <Input
              placeholder="Nhập tên mẫu bệnh án..."
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              allowClear
            />
          </Col>

          <Col xs={24} md={4} style={{ textAlign: 'right', marginTop: { xs: 0, md: 18 } }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Tổng số: <b>{filteredTemplates.length}</b> mẫu
            </Text>
          </Col>
        </Row>
      </Card>

      {/* Templates Table Card */}
      <Card className="template-table-card" bodyStyle={{ padding: 0 }}>
        <Table
          columns={columns}
          dataSource={filteredTemplates}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            showTotal: (total) => `Tổng cộng ${total} mẫu bệnh án`,
          }}
          locale={{
            emptyText: (
              <Empty
                description="Không tìm thấy mẫu bệnh án nào phù hợp với bộ lọc."
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              >
                {canManage && (
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={handleOpenCreateModal}
                  >
                    Tạo mẫu bệnh án mới
                  </Button>
                )}
              </Empty>
            ),
          }}
        />
      </Card>

      {/* Modal Tạo mới / Chỉnh sửa */}
      <TemplateFormModal
        open={formModalOpen}
        onClose={() => {
          setFormModalOpen(false)
          setEditingTemplate(null)
        }}
        onSubmit={handleFormSubmit}
        initialData={editingTemplate}
        specialties={specialties}
        loading={modalLoading}
      />

      {/* Modal Bật / Tắt trạng thái */}
      <StatusToggleModal
        open={statusModalOpen}
        onClose={() => {
          setStatusModalOpen(false)
          setStatusTargetTemplate(null)
        }}
        template={statusTargetTemplate}
        otherTemplatesInSpecialty={templates.filter(
          (t) => t.specialty?.id === statusTargetTemplate?.specialty?.id
        )}
        onSubmit={handleStatusSubmit}
        loading={modalLoading}
      />
    </div>
  )
}

export default MedicalRecordTemplateManagementPage
