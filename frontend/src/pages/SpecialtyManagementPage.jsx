import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Popconfirm,
  Radio,
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
  ApartmentOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  EyeOutlined,
  InfoCircleOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
  UserOutlined,
} from '@ant-design/icons'
import specialtyApi from '../api/specialtyApi'
import SpecialtyFormModal from '../components/specialty/SpecialtyFormModal'
import SpecialtyDeactivateConfirmModal from '../components/specialty/SpecialtyDeactivateConfirmModal'
import SpecialtyDetailDrawer from '../components/specialty/SpecialtyDetailDrawer'
import { useAuthContext } from '../context/AuthContext'
import { getApiErrorMessage } from '../utils/apiError'
import '../styles/specialtyManagement.css'

const { Title, Text, Paragraph } = Typography

export default function SpecialtyManagementPage() {
  const { user } = useAuthContext()

  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [user])

  const userRoles = useMemo(() => {
    return (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  }, [user])

  const isAdmin = userRoles.includes('admin')
  const canManage = userPermissions.includes('SPECIALTY_MANAGE') || isAdmin

  const [specialties, setSpecialties] = useState([])
  const [loading, setLoading] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  // Modals & Drawer states
  const [formModalOpen, setFormModalOpen] = useState(false)
  const [editingSpecialty, setEditingSpecialty] = useState(null)
  const [deactivateModalOpen, setDeactivateModalOpen] = useState(false)
  const [selectedSpecialty, setSelectedSpecialty] = useState(null)
  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false)
  const [detailSpecialtyId, setDetailSpecialtyId] = useState(null)
  const [actionLoadingId, setActionLoadingId] = useState(null)

  const fetchSpecialties = useCallback(async () => {
    setLoading(true)
    try {
      const params = {}
      if (keyword.trim()) {
        params.keyword = keyword.trim()
      }
      if (statusFilter === 'ACTIVE') {
        params.active = true
      } else if (statusFilter === 'INACTIVE') {
        params.active = false
      }

      const res = await specialtyApi.search(params)
      const list = Array.isArray(res.data) ? res.data : []
      setSpecialties(list)
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Không thể nạp danh sách chuyên khoa.')
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }, [keyword, statusFilter])

  useEffect(() => {
    fetchSpecialties()
  }, [fetchSpecialties])

  const stats = useMemo(() => {
    const total = specialties.length
    const activeCount = specialties.filter((s) => s.active).length
    const inactiveCount = total - activeCount
    return { total, activeCount, inactiveCount }
  }, [specialties])

  const handleOpenCreate = () => {
    setEditingSpecialty(null)
    setFormModalOpen(true)
  }

  const handleOpenEdit = (specialty) => {
    setEditingSpecialty(specialty)
    setFormModalOpen(true)
  }

  const handleOpenDetail = (specialtyId) => {
    setDetailSpecialtyId(specialtyId)
    setDetailDrawerOpen(true)
  }

  const handleOpenDeactivate = (specialty) => {
    if (specialty.code === 'GENERAL') {
      message.warning('Không thể ngừng dùng chuyên khoa mặc định hệ thống (GENERAL).')
      return
    }
    setSelectedSpecialty(specialty)
    setDeactivateModalOpen(true)
  }

  const handleActivate = async (specialty) => {
    setActionLoadingId(specialty.id)
    try {
      await specialtyApi.activate(specialty.id)
      message.success(`Kích hoạt lại chuyên khoa "${specialty.name}" thành công!`)
      fetchSpecialties()
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Không thể kích hoạt lại chuyên khoa.')
      message.error(msg)
    } finally {
      setActionLoadingId(null)
    }
  }

  const columns = [
    {
      title: 'Mã chuyên khoa',
      dataIndex: 'code',
      key: 'code',
      width: 170,
      render: (code) => {
        const isGeneral = code === 'GENERAL'
        return (
          <Space orientation="horizontal" size={4}>
            <Tag
              className={`specialty-code-tag ${isGeneral ? 'specialty-general-tag' : ''}`}
              color={isGeneral ? 'blue' : 'geekblue'}
            >
              {code}
            </Tag>
            {isGeneral && (
              <Tooltip title="Chuyên khoa mặc định hệ thống">
                <Tag color="purple" style={{ fontSize: 11, padding: '0 4px' }}>
                  Mặc định
                </Tag>
              </Tooltip>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Tên chuyên khoa',
      dataIndex: 'name',
      key: 'name',
      width: 220,
      render: (name, record) => (
        <div>
          <a
            onClick={() => handleOpenDetail(record.id)}
            style={{ fontWeight: 600, color: '#1e293b' }}
            className="hover:underline"
          >
            {name}
          </a>
        </div>
      ),
    },
    {
      title: 'Mô tả',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (description) =>
        description ? (
          <Tooltip title={description} placement="topLeft">
            <span>{description}</span>
          </Tooltip>
        ) : (
          <Text type="secondary" italic>
            Chưa có mô tả
          </Text>
        ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 150,
      render: (active) => (
        <span
          className={`specialty-pill-badge ${
            active ? 'specialty-pill-active' : 'specialty-pill-inactive'
          }`}
        >
          {active ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
          <span>{active ? 'Đang dùng' : 'Ngừng dùng'}</span>
        </span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 190,
      align: 'right',
      render: (_, record) => {
        const isGeneral = record.code === 'GENERAL'
        const isActionLoading = actionLoadingId === record.id

        return (
          <Space size="middle">
            <Tooltip title="Xem chi tiết chuyên khoa">
              <Button
                className="specialty-action-btn specialty-action-btn-view"
                icon={<EyeOutlined style={{ fontSize: 17 }} />}
                onClick={() => handleOpenDetail(record.id)}
              />
            </Tooltip>

            {canManage && (
              <>
                <Tooltip title="Chỉnh sửa thông tin">
                  <Button
                    className="specialty-action-btn specialty-action-btn-edit"
                    icon={<EditOutlined style={{ fontSize: 17 }} />}
                    onClick={() => handleOpenEdit(record)}
                  />
                </Tooltip>

                {record.active ? (
                  <Tooltip
                    title={
                      isGeneral
                        ? 'Chuyên khoa mặc định hệ thống không thể ngừng dùng'
                        : 'Ngừng dùng chuyên khoa'
                    }
                  >
                    <Button
                      className="specialty-action-btn specialty-action-btn-danger"
                      disabled={isGeneral}
                      icon={<StopOutlined style={{ fontSize: 17 }} />}
                      onClick={() => handleOpenDeactivate(record)}
                    />
                  </Tooltip>
                ) : (
                  <Popconfirm
                    title="Kích hoạt lại chuyên khoa"
                    description={`Bạn có chắc muốn kích hoạt lại chuyên khoa "${record.name}" không?`}
                    onConfirm={() => handleActivate(record)}
                    okText="Kích hoạt"
                    cancelText="Hủy"
                  >
                    <Tooltip title="Kích hoạt lại">
                      <Button
                        className="specialty-action-btn specialty-action-btn-activate"
                        loading={isActionLoading}
                        icon={<CheckCircleOutlined style={{ fontSize: 17 }} />}
                      />
                    </Tooltip>
                  </Popconfirm>
                )}
              </>
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <div className="specialty-page-container">
      {/* Header card */}
      <Card className="specialty-header-card" bordered={false}>
        <Row align="middle" justify="space-between" gutter={[16, 16]}>
          <Col xs={24} md={16}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div
                style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  backgroundColor: '#ecfdf5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#059669',
                  fontSize: 24,
                }}
              >
                <MedicineBoxOutlined />
              </div>
              <div>
                <Title level={3} style={{ margin: 0, color: '#064e3b' }}>
                  Quản lý danh mục chuyên khoa và phòng khám bệnh
                </Title>
              </div>
            </div>
          </Col>
          <Col xs={24} md={8} style={{ textAlign: 'right' }}>
            {canManage && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                size="large"
                style={{
                  background: '#059669',
                  borderColor: '#059669',
                  borderRadius: 8,
                  fontWeight: 600,
                  boxShadow: '0 4px 6px -1px rgba(5, 150, 105, 0.2)',
                }}
                onClick={handleOpenCreate}
              >
                Thêm chuyên khoa mới
              </Button>
            )}
          </Col>
        </Row>
      </Card>

      {/* KPI Stats row */}
      <Row gutter={[16, 16]} className="specialty-stats-row">
        <Col xs={24} sm={8}>
          <Card className="specialty-stat-card">
            <Text type="secondary" style={{ fontSize: 13, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Tổng số chuyên khoa
            </Text>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 }}>
              <span className="specialty-stat-value" style={{ color: '#0f172a' }}>
                {stats.total}
              </span>
              <div
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: 8,
                  background: '#eff6ff',
                  color: '#2563eb',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 18,
                }}
              >
                <MedicineBoxOutlined />
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card className="specialty-stat-card">
            <Text type="secondary" style={{ fontSize: 13, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Đang hoạt động
            </Text>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 }}>
              <span className="specialty-stat-value" style={{ color: '#16a34a' }}>
                {stats.activeCount}
              </span>
              <div
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: 8,
                  background: '#ecfdf5',
                  color: '#16a34a',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 18,
                }}
              >
                <CheckCircleOutlined />
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card className="specialty-stat-card">
            <Text type="secondary" style={{ fontSize: 13, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Tạm ngừng dùng
            </Text>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 }}>
              <span className="specialty-stat-value" style={{ color: '#64748b' }}>
                {stats.inactiveCount}
              </span>
              <div
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: 8,
                  background: '#f1f5f9',
                  color: '#64748b',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 18,
                }}
              >
                <StopOutlined />
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Filter Toolbar */}
      <Card className="specialty-filter-card" bodyStyle={{ padding: '16px 20px' }}>
        <Row gutter={[16, 16]} align="middle" justify="space-between">
          <Col xs={24} md={14}>
            <Space wrap size="middle">
              <Input
                placeholder="Tìm theo mã hoặc tên chuyên khoa..."
                prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
                allowClear
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onPressEnter={fetchSpecialties}
                style={{ width: 280, borderRadius: 8 }}
              />

              <Radio.Group
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                buttonStyle="solid"
              >
                <Radio.Button value="ALL">Tất cả ({stats.total})</Radio.Button>
                <Radio.Button value="ACTIVE">Đang dùng ({stats.activeCount})</Radio.Button>
                <Radio.Button value="INACTIVE">Ngừng dùng ({stats.inactiveCount})</Radio.Button>
              </Radio.Group>
            </Space>
          </Col>

          <Col xs={24} md={10} style={{ textAlign: 'right' }}>
            <Button
              icon={<ReloadOutlined />}
              onClick={fetchSpecialties}
              loading={loading}
              style={{ borderRadius: 8 }}
            >
              Làm mới
            </Button>
          </Col>
        </Row>
      </Card>

      {/* Table Card */}
      <Card className="specialty-table-card" bodyStyle={{ padding: 0 }}>
        <Table
          columns={columns}
          dataSource={specialties}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            showTotal: (total, range) => `${range[0]}-${range[1]} trong số ${total} chuyên khoa`,
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Không tìm thấy chuyên khoa nào phù hợp"
              />
            ),
          }}
        />
      </Card>

      {/* Form Modal (Create / Edit) */}
      <SpecialtyFormModal
        open={formModalOpen}
        onCancel={() => setFormModalOpen(false)}
        onSuccess={() => {
          setFormModalOpen(false)
          fetchSpecialties()
        }}
        editingSpecialty={editingSpecialty}
      />

      {/* Deactivate Confirm Modal */}
      <SpecialtyDeactivateConfirmModal
        open={deactivateModalOpen}
        specialty={selectedSpecialty}
        onCancel={() => setDeactivateModalOpen(false)}
        onSuccess={() => {
          setDeactivateModalOpen(false)
          fetchSpecialties()
        }}
      />

      {/* Detail Drawer */}
      <SpecialtyDetailDrawer
        open={detailDrawerOpen}
        specialtyId={detailSpecialtyId}
        onClose={() => setDetailDrawerOpen(false)}
      />
    </div>
  )
}
