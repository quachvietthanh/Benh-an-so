import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Row,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import {
  CloudDownloadOutlined,
  CloudUploadOutlined,
  DatabaseOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  HistoryOutlined,
  LockOutlined,
  PlusOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import backupApi from '../api/backupApi'
import { useAuthContext } from '../context/AuthContext'

const { Title, Text, Paragraph } = Typography

const formatDateTime = (val) => {
  if (!val) return '—'
  const date = new Date(val)
  if (isNaN(date.getTime())) return '—'
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()
  return `${hours}:${minutes}:${seconds} ${day}/${month}/${year}`
}

function BackupRestorePage() {
  const { user } = useAuthContext()

  // Phân quyền vai trò: Chỉ ADMIN mới được thực hiện Sao lưu & Phục hồi
  const userRoles = useMemo(() => {
    const raw = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
    return raw.map((r) => String(r || '').toLowerCase().replace(/^role_/, '')).filter(Boolean)
  }, [user])

  const isAdmin = userRoles.includes('admin')

  // State danh sách & trạng thái
  const [backups, setBackups] = useState([])
  const [loading, setLoading] = useState(false)
  const [creating, setCreating] = useState(false)
  const [restoring, setRestoring] = useState(false)
  const [apiError, setApiError] = useState('')
  const [isBackendAvailable, setIsBackendAvailable] = useState(true)

  // State Modals
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [backupNote, setBackupNote] = useState('')
  const [restoreTargetBackup, setRestoreTargetBackup] = useState(null)
  const [detailModalBackup, setDetailModalBackup] = useState(null)

  // 1. Tải danh sách bản sao lưu từ Backend REST API (GET /admin/backups)
  const loadBackups = useCallback(async () => {
    setLoading(true)
    setApiError('')
    try {
      const res = await backupApi.getAll({ page: 0, size: 50 })
      const data = res?.data
      const list = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
      setBackups(list)
      setIsBackendAvailable(true)
    } catch (err) {
      console.error('[BackupRestorePage] Lỗi loadBackups:', err?.response?.status, err?.message)
      const status = err?.response?.status
      const msg = err?.response?.data?.message

      if (status === 403) {
        setApiError('Bạn không có quyền thực hiện chức năng này (403 Forbidden).')
      } else if (status === 404) {
        setIsBackendAvailable(false)
        setApiError('Hệ thống Backend hiện tại chưa triển khai REST Endpoint Sao lưu/Phục hồi (/admin/backups - 404 Not Found).')
      } else {
        setApiError(msg || 'Không thể kết nối đến máy chủ Backend để tải danh sách bản sao lưu.')
      }
      setBackups([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadBackups()
  }, [loadBackups])

  // 2. Tạo bản sao lưu mới (POST /admin/backups)
  const handleConfirmCreateBackup = async () => {
    if (!isAdmin) {
      message.error('Bạn không có quyền thực hiện chức năng này.')
      return
    }

    setCreating(true)
    setApiError('')
    try {
      const payload = {
        note: backupNote.trim() || undefined,
      }
      const res = await backupApi.createBackup(payload)
      const newBackup = res?.data

      message.success('Đã tạo bản sao lưu dữ liệu thành công!')
      setCreateModalOpen(false)
      setBackupNote('')

      // Reload danh sách trực tiếp từ Backend
      await loadBackups()
    } catch (err) {
      console.error('[BackupRestorePage] Lỗi createBackup:', err?.response?.status, err?.message)
      const status = err?.response?.status
      const msg = err?.response?.data?.message

      if (status === 403) {
        setApiError('Bạn không có quyền tạo bản sao lưu dữ liệu (403 Forbidden).')
      } else if (status === 409) {
        setApiError('Hệ thống đang thực hiện một tiến trình sao lưu/phục hồi khác (409 Conflict).')
      } else {
        setApiError(msg || 'Không thể tạo bản sao lưu từ Backend. Vui lòng thử lại.')
      }
    } finally {
      setCreating(false)
    }
  }

  // 3. Phục hồi dữ liệu từ bản sao lưu chỉ định (POST /admin/backups/{backupId}/restore)
  const handleConfirmRestore = async () => {
    if (!restoreTargetBackup || !restoreTargetBackup.id) {
      message.error('Mã bản sao lưu không hợp lệ.')
      return
    }
    if (!isAdmin) {
      message.error('Bạn không có quyền phục hồi dữ liệu.')
      return
    }

    const backupId = restoreTargetBackup.id
    setRestoring(true)
    setApiError('')

    try {
      const res = await backupApi.restoreBackup(backupId)
      message.success(`Phục hồi dữ liệu thành công về thời điểm bản sao lưu!`)
      setRestoreTargetBackup(null)

      // Reload danh sách và dữ liệu Backend
      await loadBackups()
    } catch (err) {
      console.error('[BackupRestorePage] Lỗi restoreBackup:', err?.response?.status, err?.message)
      const status = err?.response?.status
      const msg = err?.response?.data?.message

      if (status === 403) {
        setApiError('Bạn không có quyền phục hồi dữ liệu hệ thống (403 Forbidden).')
      } else if (status === 404) {
        setApiError('Không tìm thấy bản sao lưu trên Backend (404 Not Found).')
      } else if (status === 409) {
        setApiError('Hệ thống đang bận hoặc bản sao lưu không ở trạng thái sẵn sàng để phục hồi (409 Conflict).')
      } else {
        setApiError(msg || 'Phục hồi dữ liệu thất bại từ Backend. Vui lòng thử lại.')
      }
    } finally {
      setRestoring(false)
    }
  }

  // Thống kê dữ liệu
  const latestBackup = useMemo(() => {
    if (!backups || backups.length === 0) return null
    return [...backups].sort((a, b) => new Date(b.createdAt || b.backupTime || 0) - new Date(a.createdAt || a.backupTime || 0))[0]
  }, [backups])

  // Cột bảng danh sách bản sao lưu chuẩn DTO Backend
  const columns = [
    {
      title: 'Thời gian sao lưu',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (val, record) => (
        <Space direction="vertical" size={0}>
          <Text strong>{formatDateTime(val || record.backupTime || record.createdDate)}</Text>
          <Text type="secondary" style={{ fontSize: 11 }}>
            ID: {record.id || record.backupId || '—'}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Người tạo',
      dataIndex: 'createdBy',
      key: 'createdBy',
      render: (val, record) => record.creatorName || val || record.creator || 'Hệ thống (ADMIN)',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      align: 'center',
      render: (status) => {
        const st = String(status || '').toUpperCase()
        if (st === 'COMPLETED' || st === 'SUCCESS') {
          return <Tag color="green" icon={<SafetyCertificateOutlined />}>Hoàn thành</Tag>
        }
        if (st === 'PROCESSING' || st === 'PENDING') {
          return <Tag color="processing" icon={<SyncOutlined spin />}>Đang xử lý</Tag>
        }
        if (st === 'FAILED' || st === 'ERROR') {
          return <Tag color="error" icon={<ExclamationCircleOutlined />}>Thất bại</Tag>
        }
        return <Tag color="default">{status || 'COMPLETED'}</Tag>
      },
    },
    {
      title: 'Dung lượng',
      dataIndex: 'fileSize',
      key: 'fileSize',
      align: 'right',
      render: (val, record) => record.sizeDisplay || val || (record.size ? `${(record.size / (1024 * 1024)).toFixed(2)} MB` : '—'),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      align: 'center',
      render: (_, record) => {
        const isCompleted = String(record.status || 'COMPLETED').toUpperCase() === 'COMPLETED' || String(record.status || '').toUpperCase() === 'SUCCESS'
        return (
          <Space size="small">
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => setDetailModalBackup(record)}
            >
              Xem chi tiết
            </Button>
            <Button
              size="small"
              type="primary"
              danger
              icon={<CloudDownloadOutlined />}
              disabled={!isAdmin || !isCompleted || restoring || loading}
              onClick={() => setRestoreTargetBackup(record)}
            >
              Phục hồi
            </Button>
          </Space>
        )
      },
    },
  ]

  return (
    <div style={{ padding: 24, maxWidth: 1200, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ margin: 0, color: '#0f172a' }}>
            <DatabaseOutlined style={{ color: '#2563eb', marginRight: 8 }} />
            SAO LƯU & PHỤC HỒI DỮ LIỆU
          </Title>
          <Text type="secondary">
            Quản lý các bản sao lưu dữ liệu hệ thống Bệnh Án Số và phục hồi khi có sự cố.
          </Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadBackups}>
            Làm mới
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            disabled={!isAdmin || loading || creating}
            onClick={() => setCreateModalOpen(true)}
          >
            + Tạo bản sao lưu
          </Button>
        </Space>
      </div>

      {/* Kiểm tra Role ADMIN */}
      {!isAdmin && (
        <Alert
          type="error"
          showIcon
          icon={<LockOutlined />}
          message="Bạn không có quyền thực hiện sao lưu hoặc phục hồi dữ liệu."
          description="Chức năng quản trị an toàn dữ liệu chỉ dành riêng cho tài khoản Quản trị viên (ADMIN)."
          style={{ marginBottom: 20 }}
        />
      )}

      {/* Cảnh báo Backend 404 / Lỗi API */}
      {apiError && (
        <Alert
          type={isBackendAvailable ? 'warning' : 'info'}
          showIcon
          message="Thông báo từ Backend REST API"
          description={apiError}
          action={<Button size="small" onClick={loadBackups}>Thử kết nối lại</Button>}
          style={{ marginBottom: 20 }}
        />
      )}

      {/* Cards Thống kê */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={8}>
          <Card size="small" style={{ background: '#f8fafc', borderColor: '#e2e8f0' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>Tổng số bản sao lưu</Text>
            <div style={{ fontSize: 24, fontWeight: 700, color: '#1e293b' }}>
              {backups.length} bản
            </div>
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small" style={{ background: '#f8fafc', borderColor: '#e2e8f0' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>Bản sao gần nhất</Text>
            <div style={{ fontSize: 14, fontWeight: 600, color: '#2563eb' }}>
              {latestBackup ? formatDateTime(latestBackup.createdAt || latestBackup.backupTime) : '— Chưa có'}
            </div>
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small" style={{ background: '#f8fafc', borderColor: '#e2e8f0' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>Trạng thái gần nhất</Text>
            <div>
              {latestBackup ? (
                <Tag color="green" icon={<SafetyCertificateOutlined />}>
                  {latestBackup.status || 'HOÀN THÀNH'}
                </Tag>
              ) : (
                <Text type="secondary">—</Text>
              )}
            </div>
          </Card>
        </Col>
      </Row>

      {/* Bảng Danh sách Bản sao lưu */}
      <Card title="Danh sách bản sao lưu hệ thống" styles={{ body: { padding: 0 } }}>
        <Table
          rowKey={(r) => r.id || r.backupId || Math.random()}
          columns={columns}
          dataSource={backups}
          loading={loading}
          pagination={{ pageSize: 10 }}
          locale={{
            emptyText: (
              <Empty
                description={
                  !isBackendAvailable
                    ? 'Backend chưa triển khai REST Controller /admin/backups'
                    : 'Chưa có bản sao lưu nào được tạo'
                }
              />
            ),
          }}
        />
      </Card>

      {/* Modal Xác nhận Tạo bản sao lưu */}
      <Modal
        title={
          <Space>
            <CloudUploadOutlined style={{ color: '#2563eb' }} />
            <span>Tạo bản sao lưu dữ liệu?</span>
          </Space>
        }
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        footer={[
          <Button key="cancel" onClick={() => setCreateModalOpen(false)}>
            Hủy
          </Button>,
          <Button
            key="submit"
            type="primary"
            loading={creating}
            disabled={!isAdmin || creating}
            onClick={handleConfirmCreateBackup}
          >
            Xác nhận sao lưu
          </Button>,
        ]}
      >
        <Paragraph>
          Hệ thống sẽ tiến hành đóng gói và tạo một bản sao lưu toàn bộ dữ liệu CSDL tại thời điểm hiện tại.
        </Paragraph>
        <Form layout="vertical">
          <Form.Item label="Ghi chú bản sao lưu (Tùy chọn)">
            <Input.TextArea
              rows={2}
              placeholder="Nhập ghi chú mục đích sao lưu (vd: Sao lưu định kỳ trước khi nâng cấp hệ thống...)"
              value={backupNote}
              onChange={(e) => setBackupNote(e.target.value)}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal Xác nhận Phục hồi Dữ liệu */}
      <Modal
        title={
          <Space>
            <ExclamationCircleOutlined style={{ color: '#dc2626' }} />
            <span style={{ color: '#dc2626', fontWeight: 700 }}>XÁC NHẬN PHỤC HỒI DỮ LIỆU?</span>
          </Space>
        }
        open={!!restoreTargetBackup}
        onCancel={() => setRestoreTargetBackup(null)}
        footer={[
          <Button key="cancel" onClick={() => setRestoreTargetBackup(null)}>
            Hủy
          </Button>,
          <Button
            key="submit"
            type="primary"
            danger
            loading={restoring}
            disabled={!isAdmin || restoring}
            onClick={handleConfirmRestore}
          >
            Phục hồi dữ liệu
          </Button>,
        ]}
      >
        {restoreTargetBackup && (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Alert
              type="warning"
              showIcon
              message="CẢNH BÁO PHỤC HỒI NGUY HIỂM"
              description="Dữ liệu vận hành hệ thống hiện tại sẽ được khôi phục hoàn toàn về trạng thái tại thời điểm của bản sao lưu này. Các dữ liệu phát sinh sau thời điểm sao lưu có thể bị ghi đè."
            />
            <Card size="small" style={{ background: '#f8fafc', marginTop: 12 }}>
              <Descriptions size="small" column={1} labelStyle={{ fontWeight: 600, width: 140 }}>
                <Descriptions.Item label="Mã bản sao">
                  <Text code>{restoreTargetBackup.id || restoreTargetBackup.backupId}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Thời gian tạo">
                  {formatDateTime(restoreTargetBackup.createdAt || restoreTargetBackup.backupTime)}
                </Descriptions.Item>
                <Descriptions.Item label="Người tạo">
                  {restoreTargetBackup.createdBy || 'ADMIN'}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          </Space>
        )}
      </Modal>

      {/* Modal Xem chi tiết bản sao lưu */}
      <Modal
        title="Chi tiết bản sao lưu"
        open={!!detailModalBackup}
        onCancel={() => setDetailModalBackup(null)}
        footer={[
          <Button key="close" onClick={() => setDetailModalBackup(null)}>
            Đóng
          </Button>,
        ]}
      >
        {detailModalBackup && (
          <Descriptions column={1} bordered size="small" labelStyle={{ fontWeight: 600, width: 160 }}>
            <Descriptions.Item label="Mã bản sao ID">
              <Text code>{detailModalBackup.id || detailModalBackup.backupId}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Thời gian sao lưu">
              {formatDateTime(detailModalBackup.createdAt || detailModalBackup.backupTime)}
            </Descriptions.Item>
            <Descriptions.Item label="Người khởi tạo">
              {detailModalBackup.createdBy || 'ADMIN'}
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color="green">{detailModalBackup.status || 'COMPLETED'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Dung lượng file">
              {detailModalBackup.fileSize || detailModalBackup.sizeDisplay || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Ghi chú">
              {detailModalBackup.note || detailModalBackup.description || 'Không có ghi chú'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default BackupRestorePage
