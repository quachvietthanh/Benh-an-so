import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Dropdown,
  Empty,
  Input,
  Modal,
  Popconfirm,
  Radio,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AppstoreOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  ExperimentOutlined,
  FolderOpenOutlined,
  PlusOutlined,
  PoweroffOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
} from '@ant-design/icons'
import diagnosisCatalogApi from '../api/diagnosisCatalogApi'
import DiagnosisCatalogCreateModal from '../components/clinical/DiagnosisCatalogCreateModal'
import DiagnosisCatalogEditModal from '../components/clinical/DiagnosisCatalogEditModal'
import Loading from '../components/common/Loading'
import { useAuthContext } from '../context/AuthContext'
import { getApiErrorMessage } from '../utils/apiError'
import { icd10Categories } from '../utils/icd10Data'

const { Title, Text, Paragraph } = Typography

const groupColorMap = {
  'Hô hấp': 'cyan',
  'Tiêu hóa': 'orange',
  'Tim mạch': 'red',
  'Nội tiết': 'purple',
  'Cơ xương khớp': 'blue',
  'Nhiễm trùng': 'volcano',
  'Thần kinh': 'geekblue',
  'Tiết niệu': 'gold',
  'Khám tổng quát': 'green',
  'Da liễu': 'magenta',
}

const getGroupColor = (group = '') => {
  for (const [key, color] of Object.entries(groupColorMap)) {
    if (group.toLowerCase().includes(key.toLowerCase())) {
      return color
    }
  }
  return 'default'
}

function DiagnosisCatalogPage() {
  const { user } = useAuthContext()
  const userPermissions = (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  const userRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const isAdmin = userRoles.includes('admin')
  const canManage = userPermissions.includes('DIAGNOSIS_CATALOG_MANAGE') || isAdmin

  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [groupFilter, setGroupFilter] = useState('ALL')
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [selectedItem, setSelectedItem] = useState(null)
  const [actionLoadingId, setActionLoadingId] = useState(null)
  const [isReadOnly, setIsReadOnly] = useState(!canManage)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const params = {}
      if (keyword.trim()) params.keyword = keyword.trim()
      if (statusFilter === 'ACTIVE') params.active = true
      if (statusFilter === 'INACTIVE') params.active = false

      const response = await diagnosisCatalogApi.search(params)
      if (response?.isReadOnly) {
        setIsReadOnly(true)
      }
      const list = Array.isArray(response?.data)
        ? response.data
        : Array.isArray(response?.data?.content)
          ? response.data.content
          : []
      setData(list)
    } catch (err) {
      console.error('Failed to load diagnosis catalog:', err)
      setData([])
    } finally {
      setLoading(false)
    }
  }, [keyword, statusFilter])

  useEffect(() => {
    loadData()
  }, [loadData])

  const filteredData = useMemo(() => {
    if (groupFilter === 'ALL') return data
    return data.filter((item) => (item.diseaseGroup || '').trim() === groupFilter)
  }, [data, groupFilter])

  const distinctGroups = useMemo(() => {
    const groups = new Set()
    data.forEach((item) => {
      if (item.diseaseGroup) groups.add(item.diseaseGroup.trim())
    })
    return Array.from(groups).sort()
  }, [data])

  const stats = useMemo(() => {
    const total = data.length
    const active = data.filter((item) => item.active).length
    const inactive = total - active
    const groups = distinctGroups.length
    return { total, active, inactive, groups }
  }, [data, distinctGroups])

  const handleToggleStatus = async (record) => {
    if (!canManage) {
      message.warning('Chỉ Quản trị viên mới có quyền thay đổi trạng thái mã bệnh.')
      return
    }
    setActionLoadingId(record.id)
    const nextActive = !record.active
    try {
      await diagnosisCatalogApi.updateStatus(record.id, nextActive)
      message.success(
        nextActive
          ? `Đã kích hoạt lại mã bệnh [${record.code}] thành công!`
          : `Đã tạm ngừng sử dụng mã bệnh [${record.code}].`,
      )
      loadData()
    } catch (err) {
      message.error(getApiErrorMessage(err, 'Không thể thay đổi trạng thái mã bệnh.'))
    } finally {
      setActionLoadingId(null)
    }
  }

  const handleDelete = async (record) => {
    if (!canManage) {
      message.warning('Chỉ Quản trị viên mới có quyền xóa mã bệnh.')
      return
    }
    setActionLoadingId(record.id)
    try {
      await diagnosisCatalogApi.delete(record.id)
      message.success(`Đã xóa mã bệnh [${record.code}] khỏi danh mục.`)
      loadData()
    } catch (err) {
      const apiCode = err?.apiError?.code || err?.response?.data?.code
      if (apiCode === 'DIAGNOSIS_CATALOG_IN_USE') {
        Modal.warning({
          title: 'Không thể xóa mã bệnh',
          icon: <ExclamationCircleOutlined style={{ color: '#d97706' }} />,
          content: (
            <div>
              <p>
                Mã bệnh <strong>[{record.code}] {record.name}</strong> đã được sử dụng trong các hồ sơ bệnh án khám bệnh đã ghi nhận.
              </p>
              <p style={{ color: '#64748b' }}>
                Để bảo toàn dữ liệu lịch sử khám chữa bệnh, bạn không thể xóa hoàn toàn mã này. Bạn có thể chọn <strong>Ngừng sử dụng</strong> để ẩn mã khỏi danh sách gợi ý cho bác sĩ trong các ca khám mới.
              </p>
            </div>
          ),
          okText: 'Ngừng sử dụng mã bệnh',
          cancelText: 'Đóng',
          okButtonProps: { danger: true },
          onOk: () => handleToggleStatus({ ...record, active: true }),
        })
      } else {
        message.error(getApiErrorMessage(err, 'Không thể xóa mã bệnh. Vui lòng thử lại.'))
      }
    } finally {
      setActionLoadingId(null)
    }
  }

  const columns = useMemo(() => {
    const baseColumns = [
      {
        title: 'Mã ICD',
        dataIndex: 'code',
        key: 'code',
        width: 120,
        render: (code) => (
          <Tag color="blue" className="diagnosis-code-tag">
            {code}
          </Tag>
        ),
      },
      {
        title: 'Tên bệnh chuẩn hóa',
        dataIndex: 'name',
        key: 'name',
        minWidth: 240,
        render: (text, record) => (
          <div>
            <Text strong style={{ color: record.active !== false ? '#0f172a' : '#94a3b8', fontSize: 13.5 }}>
              {text}
            </Text>
            {record.description && (
              <div style={{ color: '#64748b', fontSize: 12, marginTop: 2 }}>
                {record.description}
              </div>
            )}
          </div>
        ),
      },
      {
        title: 'Nhóm bệnh / Chuyên khoa',
        dataIndex: 'diseaseGroup',
        key: 'diseaseGroup',
        width: 190,
        render: (group) => (
          <Tag color={getGroupColor(group)} className="diagnosis-group-badge">
            {group || 'Chung'}
          </Tag>
        ),
      },
      {
        title: 'Trạng thái',
        dataIndex: 'active',
        key: 'active',
        width: 150,
        render: (active, record) => {
          const isActive = active !== false
          if (!canManage || isReadOnly) {
            return (
              <Tag color={isActive ? 'success' : 'default'} style={{ margin: 0 }}>
                {isActive ? 'Đang dùng' : 'Ngừng dùng'}
              </Tag>
            )
          }
          return (
            <Space size={6}>
              <Switch
                checked={isActive}
                size="small"
                loading={actionLoadingId === record.id}
                onChange={() => handleToggleStatus(record)}
              />
              <Tag color={isActive ? 'success' : 'default'} style={{ margin: 0 }}>
                {isActive ? 'Đang dùng' : 'Ngừng dùng'}
              </Tag>
            </Space>
          )
        },
      },
    ]

    if (canManage && !isReadOnly) {
      baseColumns.push({
        title: 'Thao tác',
        key: 'actions',
        width: 130,
        align: 'center',
        render: (_, record) => (
          <Space size={8}>
            <Tooltip title="Chỉnh sửa thông tin mã bệnh">
              <Button
                type="text"
                size="small"
                icon={<EditOutlined style={{ color: '#2563eb' }} />}
                onClick={() => {
                  setSelectedItem(record)
                  setEditModalOpen(true)
                }}
              />
            </Tooltip>

            <Popconfirm
              title="Xác nhận xóa mã bệnh"
              description={`Bạn có chắc muốn xóa mã [${record.code}] khỏi danh mục?`}
              onConfirm={() => handleDelete(record)}
              okText="Xóa"
              cancelText="Hủy"
              okButtonProps={{ danger: true, loading: actionLoadingId === record.id }}
            >
              <Tooltip title="Xóa mã bệnh">
                <Button
                  type="text"
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  loading={actionLoadingId === record.id}
                />
              </Tooltip>
            </Popconfirm>
          </Space>
        ),
      })
    }

    return baseColumns
  }, [canManage, isReadOnly, actionLoadingId])

  return (
    <div className="diagnosis-catalog-page">
      <div className="diagnosis-page-header">
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <ExperimentOutlined style={{ color: '#2563eb', marginRight: 8 }} />
            Quản lý Danh mục Mã Bệnh (ICD-10)
          </Title>
          <Text type="secondary">
            Chuẩn hóa danh mục tên gọi và mã bệnh theo phân loại quốc tế, sẵn sàng cho bác sĩ tra cứu và chỉ định khi khám bệnh.
          </Text>
        </div>
        <div className="diagnosis-header-actions">
          <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
            Làm mới
          </Button>
          {canManage && !isReadOnly && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateModalOpen(true)}
              style={{ fontWeight: 600 }}
            >
              Thêm mã bệnh mới
            </Button>
          )}
        </div>
      </div>

      {(!canManage || isReadOnly) && (
        <Alert
          type="info"
          showIcon
          message="Chế độ tra cứu danh mục mã bệnh"
          description="Bạn đang xem danh mục mã bệnh ICD-10 ở chế độ tra cứu. Quyền thêm mới, chỉnh sửa và ngừng dùng mã bệnh dành riêng cho Quản trị viên (ADMIN)."
          style={{ marginBottom: 16, borderRadius: 10 }}
        />
      )}

      <div className="diagnosis-stats-grid">
        <div className="diagnosis-stat-card">
          <div className="diagnosis-stat-icon blue">
            <ExperimentOutlined />
          </div>
          <div className="diagnosis-stat-info">
            <span className="diagnosis-stat-label">Tổng số mã ICD</span>
            <strong className="diagnosis-stat-value">{stats.total}</strong>
          </div>
        </div>

        <div className="diagnosis-stat-card">
          <div className="diagnosis-stat-icon green">
            <CheckCircleOutlined />
          </div>
          <div className="diagnosis-stat-info">
            <span className="diagnosis-stat-label">Đang sử dụng</span>
            <strong className="diagnosis-stat-value">{stats.active}</strong>
          </div>
        </div>

        <div className="diagnosis-stat-card">
          <div className="diagnosis-stat-icon amber">
            <StopOutlined />
          </div>
          <div className="diagnosis-stat-info">
            <span className="diagnosis-stat-label">Tạm ngừng dùng</span>
            <strong className="diagnosis-stat-value">{stats.inactive}</strong>
          </div>
        </div>

        <div className="diagnosis-stat-card">
          <div className="diagnosis-stat-icon purple">
            <FolderOpenOutlined />
          </div>
          <div className="diagnosis-stat-info">
            <span className="diagnosis-stat-label">Nhóm bệnh</span>
            <strong className="diagnosis-stat-value">{stats.groups}</strong>
          </div>
        </div>
      </div>

      <div className="diagnosis-toolbar-card">
        <div className="diagnosis-toolbar-left">
          <Input
            className="diagnosis-search-input"
            prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
            placeholder="Tìm theo mã bệnh, tên bệnh, mô tả..."
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={loadData}
          />

          <Radio.Group
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            buttonStyle="solid"
          >
            <Radio.Button value="ALL">Tất cả ({stats.total})</Radio.Button>
            <Radio.Button value="ACTIVE">Đang dùng ({stats.active})</Radio.Button>
            <Radio.Button value="INACTIVE">Ngừng dùng ({stats.inactive})</Radio.Button>
          </Radio.Group>

          {distinctGroups.length > 0 && (
            <Select
              style={{ minWidth: 180 }}
              value={groupFilter}
              onChange={setGroupFilter}
              options={[
                { value: 'ALL', label: 'Tất cả nhóm bệnh' },
                ...distinctGroups.map((grp) => ({ value: grp, label: grp })),
              ]}
            />
          )}
        </div>

        <div className="diagnosis-toolbar-right">
          <Text type="secondary" style={{ fontSize: 13 }}>
            Hiển thị <strong>{filteredData.length}</strong> mã bệnh
          </Text>
        </div>
      </div>

      <Card bodyStyle={{ padding: 0 }} style={{ borderRadius: 12, overflow: 'hidden' }}>
        {loading && data.length === 0 ? (
          <div style={{ padding: 24 }}>
            <Loading type="table" rows={6} cols={5} tip="Đang tải danh mục mã bệnh ICD-10..." />
          </div>
        ) : (
          <Table
            columns={columns}
            dataSource={filteredData}
            rowKey="id"
            loading={loading && data.length > 0}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50', '100'],
              showTotal: (total, range) => `${range[0]}-${range[1]} của ${total} mã bệnh`,
            }}
            locale={{
              emptyText: (
                <Empty
                  description={
                    keyword || statusFilter !== 'ALL' || groupFilter !== 'ALL'
                      ? 'Không tìm thấy mã bệnh phù hợp với bộ lọc.'
                      : 'Chưa có mã bệnh nào trong danh mục.'
                  }
                />
              ),
            }}
          />
        )}
      </Card>

      <DiagnosisCatalogCreateModal
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onSuccess={() => {
          setCreateModalOpen(false)
          loadData()
        }}
      />

      <DiagnosisCatalogEditModal
        open={editModalOpen}
        item={selectedItem}
        onCancel={() => {
          setEditModalOpen(false)
          setSelectedItem(null)
        }}
        onSuccess={() => {
          setEditModalOpen(false)
          setSelectedItem(null)
          loadData()
        }}
      />
    </div>
  )
}

export default DiagnosisCatalogPage
