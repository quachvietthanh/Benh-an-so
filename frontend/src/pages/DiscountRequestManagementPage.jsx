import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Table,
  Card,
  Row,
  Col,
  Space,
  Button,
  Tag,
  Select,
  DatePicker,
  Input,
  Modal,
  Drawer,
  Dropdown,
  Typography,
  Descriptions,
  Alert,
  Tooltip,
  Divider,
  message,
} from 'antd'
import {
  PercentageOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  EllipsisOutlined,
  ReloadOutlined,
  FilterOutlined,
  LockOutlined,
  ClockCircleOutlined,
  UserOutlined,
  FileTextOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import discountRequestApi from '../api/discountRequestApi.js'
import { useAuthContext } from '../context/AuthContext.jsx'
import {
  DISCOUNT_TYPES,
  DISCOUNT_TYPE_OPTIONS,
  getStatusTag,
  canApproveOrReject,
  mapDiscountErrorMessage,
} from '../utils/discountRequestHelpers.js'
import { formatCurrency } from '../utils/paymentMethodHelpers.js'
import { formatVisitCode } from '../utils/helpers.js'

const { Text, Title, Paragraph } = Typography
const { RangePicker } = DatePicker

export default function DiscountRequestManagementPage() {
  const { user } = useAuthContext()

  // State bộ lọc
  const [statusFilter, setStatusFilter] = useState('PENDING')
  const [typeFilter, setTypeFilter] = useState('')
  const [dateRange, setDateRange] = useState(null)

  // State dữ liệu & phân trang
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  // State xem chi tiết
  const [selectedRecord, setSelectedRecord] = useState(null)
  const [drawerOpen, setDrawerOpen] = useState(false)

  // State từ chối đề xuất
  const [rejectModalOpen, setRejectModalOpen] = useState(false)
  const [rejectTargetId, setRejectTargetId] = useState(null)
  const [rejectionReason, setRejectionReason] = useState('')
  const [actionLoading, setActionLoading] = useState(false)

  // Kiểm tra vai trò quản lý / admin
  const isManagerOrAdmin = useMemo(() => {
    if (!user) return false
    const roles = (user.roles || []).map((r) => String(r).toLowerCase().replace(/^role_/, ''))
    const permissions = (user.permissions || []).map((p) => String(p).toUpperCase().replace(/^PERMISSION_/, ''))
    return (
      roles.includes('admin') ||
      roles.includes('manager') ||
      roles.includes('clinic_manager') ||
      permissions.includes('INVOICE_UPDATE')
    )
  }, [user])

  // Tải danh sách đề xuất
  const fetchDiscountRequests = useCallback(async () => {
    setLoading(true)
    try {
      const params = {
        page,
        size: pageSize,
      }
      if (statusFilter) {
        params.status = statusFilter
      }
      if (typeFilter) {
        params.discountType = typeFilter
      }
      if (dateRange && dateRange[0] && dateRange[1]) {
        params.requestedFrom = dateRange[0].startOf('day').toISOString()
        params.requestedTo = dateRange[1].endOf('day').toISOString()
      }

      const res = await discountRequestApi.list(params)
      const resData = res.data

      if (Array.isArray(resData)) {
        setData(resData)
        setTotalElements(resData.length)
      } else if (resData?.content) {
        setData(resData.content)
        setTotalElements(resData.totalElements || 0)
      } else if (resData?.data?.content) {
        setData(resData.data.content)
        setTotalElements(resData.data.totalElements || 0)
      } else {
        setData([])
        setTotalElements(0)
      }
    } catch (err) {
      message.error(mapDiscountErrorMessage(err))
      setData([])
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, statusFilter, typeFilter, dateRange])

  useEffect(() => {
    fetchDiscountRequests()
  }, [fetchDiscountRequests])

  // Xử lý phê duyệt đề xuất
  const handleApprove = (record) => {
    Modal.confirm({
      title: 'Xác nhận phê duyệt đề xuất giảm giá',
      icon: <CheckCircleOutlined style={{ color: '#16a34a' }} />,
      content: (
        <div>
          <Paragraph>
            Bạn có chắc chắn muốn phê duyệt đề xuất giảm giá cho lượt khám{' '}
            <Tag color="geekblue" style={{ fontWeight: 600 }}>
              {formatVisitCode(record.visitCode, record.visitId)}
            </Tag>
            ?
          </Paragraph>
          <Descriptions size="small" column={1} bordered>
            <Descriptions.Item label="Viện phí gốc">
              {formatCurrency(record.originalAmount)}
            </Descriptions.Item>
            <Descriptions.Item label="Số tiền giảm">
              <strong style={{ color: '#16a34a' }}>-{formatCurrency(record.discountAmount)}</strong>
            </Descriptions.Item>
            <Descriptions.Item label="Số tiền thực thu">
              <strong style={{ color: '#0284c7' }}>{formatCurrency(record.finalAmount)}</strong>
            </Descriptions.Item>
            <Descriptions.Item label="Lý do">
              {record.reason}
            </Descriptions.Item>
          </Descriptions>
        </div>
      ),
      okText: 'Phê duyệt',
      cancelText: 'Hủy bỏ',
      okButtonProps: {
        style: { background: '#16a34a', borderColor: '#16a34a' },
        loading: actionLoading,
      },
      onOk: async () => {
        setActionLoading(true)
        try {
          await discountRequestApi.approve(record.id)
          message.success('Đã phê duyệt đề xuất giảm giá thành công.')
          fetchDiscountRequests()
        } catch (err) {
          message.error(mapDiscountErrorMessage(err))
        } finally {
          setActionLoading(false)
        }
      },
    })
  }

  // Mở modal từ chối
  const openRejectModal = (record) => {
    setRejectTargetId(record.id)
    setRejectionReason('')
    setRejectModalOpen(true)
  }

  // Gửi từ chối đề xuất
  const handleConfirmReject = async () => {
    if (!rejectionReason.trim()) {
      message.warning('Vui lòng nhập lý do từ chối.')
      return
    }

    setActionLoading(true)
    try {
      await discountRequestApi.reject(rejectTargetId, rejectionReason.trim())
      message.success('Đã từ chối đề xuất giảm giá.')
      setRejectModalOpen(false)
      fetchDiscountRequests()
    } catch (err) {
      message.error(mapDiscountErrorMessage(err))
    } finally {
      setActionLoading(false)
    }
  }

  // Cột bảng danh sách
  const columns = [
    {
      title: 'Thời gian gửi',
      dataIndex: 'requestedAt',
      key: 'requestedAt',
      width: 150,
      render: (val) => (val ? dayjs(val).format('HH:mm DD/MM/YYYY') : '—'),
    },
    {
      title: 'Mã lượt khám',
      dataIndex: 'visitId',
      key: 'visitId',
      width: 150,
      render: (v, record) => {
        const code = formatVisitCode(record?.visitCode, v)
        return (
          <Tooltip title={v ? `UUID: ${v} (Nhấn để sao chép)` : ''}>
            <Tag
              color="geekblue"
              style={{
                fontWeight: 600,
                fontSize: 12,
                cursor: 'pointer',
                borderRadius: 4,
                padding: '2px 8px',
                whiteSpace: 'nowrap',
              }}
              onClick={() => {
                if (v) {
                  navigator.clipboard?.writeText(String(v))
                  message.success(`Đã sao chép mã: ${code}`)
                }
              }}
            >
              {code}
            </Tag>
          </Tooltip>
        )
      },
    },
    {
      title: 'Hình thức giảm',
      dataIndex: 'discountType',
      key: 'discountType',
      width: 180,
      render: (type, r) => {
        if (type === DISCOUNT_TYPES.PERCENTAGE) {
          return (
            <Space size={6} align="center" style={{ whiteSpace: 'nowrap' }}>
              <Tag color="blue" style={{ margin: 0 }}>
                {DISCOUNT_TYPE_OPTIONS.PERCENTAGE}
              </Tag>
              <Tag color="cyan" style={{ margin: 0, fontWeight: 700 }}>
                {r.discountValue}%
              </Tag>
            </Space>
          )
        }
        if (type === DISCOUNT_TYPES.FIXED_AMOUNT) {
          return (
            <Tag color="green" style={{ margin: 0 }}>
              {DISCOUNT_TYPE_OPTIONS.FIXED_AMOUNT}
            </Tag>
          )
        }
        if (type === DISCOUNT_TYPES.FULL_FREE) {
          return (
            <Tag color="purple" style={{ margin: 0, fontWeight: 600 }}>
              {DISCOUNT_TYPE_OPTIONS.FULL_FREE}
            </Tag>
          )
        }
        return <Tag style={{ margin: 0 }}>{type}</Tag>
      },
    },
    {
      title: 'Viện phí gốc',
      dataIndex: 'originalAmount',
      key: 'originalAmount',
      width: 130,
      align: 'right',
      render: (v) => formatCurrency(v),
    },
    {
      title: 'Số tiền giảm',
      dataIndex: 'discountAmount',
      key: 'discountAmount',
      width: 130,
      align: 'right',
      render: (v) => <strong style={{ color: '#16a34a' }}>-{formatCurrency(v)}</strong>,
    },
    {
      title: 'Số tiền còn lại',
      dataIndex: 'finalAmount',
      key: 'finalAmount',
      width: 130,
      align: 'right',
      render: (v) => <strong style={{ color: '#0284c7' }}>{formatCurrency(v)}</strong>,
    },
    {
      title: 'Lý do đề xuất',
      dataIndex: 'reason',
      key: 'reason',
      width: 250,
      ellipsis: {
        showTitle: false,
      },
      render: (txt) => (
        <Tooltip placement="topLeft" title={txt}>
          <span>{txt || '—'}</span>
        </Tooltip>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 150,
      align: 'center',
      render: (status, record) => {
        const meta = getStatusTag(status)
        const isSelfRequester = status === 'PENDING' && !canApproveOrReject(record, user?.id)
        if (isSelfRequester) {
          return (
            <Tooltip title="Bạn là người tạo đề xuất này (QTN-37 quy định không được tự duyệt, đang chờ cấp quản lý khác duyệt)">
              <Tag color="orange" icon={<LockOutlined />} style={{ margin: 0, padding: '2px 8px' }}>
                Chờ người khác duyệt
              </Tag>
            </Tooltip>
          )
        }
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 80,
      align: 'center',
      fixed: 'right',
      render: (_, record) => {
        const isPending = record.status === 'PENDING'
        const canApprove = isPending && isManagerOrAdmin && canApproveOrReject(record, user?.id)
        const isSelfRequester = isPending && !canApproveOrReject(record, user?.id)

        const menuItems = [
          {
            key: 'detail',
            icon: <EyeOutlined />,
            label: 'Xem chi tiết đề xuất',
            onClick: () => {
              setSelectedRecord(record)
              setDrawerOpen(true)
            },
          },
        ]

        if (canApprove) {
          menuItems.unshift(
            {
              key: 'approve',
              icon: <CheckCircleOutlined style={{ color: '#16a34a' }} />,
              label: <span style={{ color: '#16a34a', fontWeight: 600 }}>Duyệt đề xuất</span>,
              onClick: () => handleApprove(record),
            },
            {
              key: 'reject',
              icon: <CloseCircleOutlined style={{ color: '#dc2626' }} />,
              label: <span style={{ color: '#dc2626' }}>Từ chối đề xuất</span>,
              onClick: () => openRejectModal(record),
            },
            { type: 'divider' },
          )
        }

        if (isSelfRequester) {
          menuItems.push(
            { type: 'divider' },
            {
              key: 'self-note',
              icon: <LockOutlined style={{ color: '#94a3b8' }} />,
              label: (
                <span style={{ fontSize: 12, color: '#64748b' }}>
                  Bạn là người tạo (Không thể tự duyệt)
                </span>
              ),
              disabled: true,
            },
          )
        }

        return (
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <Button
              size="small"
              icon={<EllipsisOutlined style={{ fontSize: 16 }} />}
              title="Thao tác"
              style={{
                borderRadius: 6,
                width: 32,
                height: 28,
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            />
          </Dropdown>
        )
      },
    },
  ]

  return (
    <div style={{ padding: '24px', maxWidth: 1400, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: 20 }}>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space align="center" size={12}>
              <div
                style={{
                  width: 44,
                  height: 44,
                  borderRadius: 10,
                  background: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#ffffff',
                  fontSize: 22,
                  boxShadow: '0 4px 12px rgba(2, 132, 199, 0.3)',
                }}
              >
                <PercentageOutlined />
              </div>
              <div>
                <Title level={4} style={{ margin: 0, color: '#0f172a' }}>
                  Quản lý phê duyệt Giảm giá &amp; Miễn phí
                </Title>
              </div>
            </Space>
          </Col>

          <Col>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                setPage(0)
                fetchDiscountRequests()
              }}
              loading={loading}
            >
              Làm mới
            </Button>
          </Col>
        </Row>
      </div>

      {/* Card Bộ lọc */}
      <Card
        size="small"
        style={{
          marginBottom: 16,
          borderRadius: 8,
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
        }}
      >
        <Row gutter={[16, 12]} align="middle">
          <Col xs={24} sm={8} md={6}>
            <Text orientation="left" type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
              Trạng thái đề xuất:
            </Text>
            <Select
              value={statusFilter}
              onChange={(val) => {
                setStatusFilter(val)
                setPage(0)
              }}
              style={{ width: '100%' }}
              options={[
                { value: 'PENDING', label: '⏳ Chờ phê duyệt (Mặc định)' },
                { value: 'APPROVED', label: '✓ Đã phê duyệt' },
                { value: 'REJECTED', label: '✗ Đã từ chối' },
                { value: '', label: 'Tất cả trạng thái' },
              ]}
            />
          </Col>

          <Col xs={24} sm={8} md={6}>
            <Text orientation="left" type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
              Loại hình giảm giá:
            </Text>
            <Select
              value={typeFilter}
              onChange={(val) => {
                setTypeFilter(val)
                setPage(0)
              }}
              style={{ width: '100%' }}
              options={[
                { value: '', label: 'Tất cả hình thức' },
                { value: DISCOUNT_TYPES.PERCENTAGE, label: DISCOUNT_TYPE_OPTIONS.PERCENTAGE },
                { value: DISCOUNT_TYPES.FIXED_AMOUNT, label: DISCOUNT_TYPE_OPTIONS.FIXED_AMOUNT },
                { value: DISCOUNT_TYPES.FULL_FREE, label: DISCOUNT_TYPE_OPTIONS.FULL_FREE },
              ]}
            />
          </Col>

          <Col xs={24} sm={8} md={8}>
            <Text orientation="left" type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
              Khoảng thời gian gửi:
            </Text>
            <RangePicker
              value={dateRange}
              onChange={(dates) => {
                setDateRange(dates)
                setPage(0)
              }}
              format="DD/MM/YYYY"
              placeholder={['Từ ngày', 'Đến ngày']}
              style={{ width: '100%' }}
            />
          </Col>

          <Col xs={24} sm={24} md={4} style={{ textAlign: 'right', alignSelf: 'flex-end' }}>
            <Button
              onClick={() => {
                setStatusFilter('PENDING')
                setTypeFilter('')
                setDateRange(null)
                setPage(0)
              }}
            >
              Đặt lại bộ lọc
            </Button>
          </Col>
        </Row>
      </Card>

      {/* Bảng dữ liệu */}
      <Card
        style={{
          borderRadius: 8,
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
        }}
      >
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize: pageSize,
            total: totalElements,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            onChange: (p, ps) => {
              setPage(p - 1)
              setPageSize(ps)
            },
            showTotal: (total) => `Tổng cộng ${total} đề xuất`,
          }}
          scroll={{ x: 1400 }}
        />
      </Card>

      {/* Drawer xem chi tiết */}
      <Drawer
        title="Chi tiết đề xuất giảm giá"
        placement="right"
        width={540}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
      >
        {selectedRecord && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              type={selectedRecord.status === 'APPROVED' ? 'success' : selectedRecord.status === 'REJECTED' ? 'error' : 'warning'}
              showIcon
              message={
                <strong>
                  Trạng thái: {getStatusTag(selectedRecord.status).label}
                </strong>
              }
              description={
                selectedRecord.status === 'PENDING'
                  ? 'Đề xuất đang chờ cấp quản lý phê duyệt trước khi lập hóa đơn.'
                  : selectedRecord.status === 'APPROVED'
                  ? 'Đề xuất đã được duyệt và áp dụng giảm trừ vào viện phí.'
                  : 'Đề xuất đã bị từ chối, viện phí thu theo giá gốc.'
              }
            />

            <Descriptions title="Thông tin tài chính" column={1} bordered size="small">
              <Descriptions.Item label="Mã đề xuất">
                <Text copyable>{selectedRecord.id}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Mã lượt khám">
                <Space>
                  <Tag color="geekblue" style={{ fontWeight: 600, fontSize: 13 }}>
                    {formatVisitCode(selectedRecord.visitCode, selectedRecord.visitId)}
                  </Tag>
                  <Text copyable={{ text: String(selectedRecord.visitId) }} type="secondary" style={{ fontSize: 12 }}>
                    ({selectedRecord.visitId})
                  </Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Hình thức giảm">
                {DISCOUNT_TYPE_OPTIONS[selectedRecord.discountType] || selectedRecord.discountType}
                {selectedRecord.discountType === DISCOUNT_TYPES.PERCENTAGE && ` (${selectedRecord.discountValue}%)`}
              </Descriptions.Item>
              <Descriptions.Item label="Viện phí gốc">
                {formatCurrency(selectedRecord.originalAmount)}
              </Descriptions.Item>
              <Descriptions.Item label="Khoản giảm trừ">
                <strong style={{ color: '#16a34a' }}>-{formatCurrency(selectedRecord.discountAmount)}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="Số tiền thực thu">
                <strong style={{ color: '#0284c7', fontSize: 16 }}>{formatCurrency(selectedRecord.finalAmount)}</strong>
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="Tiến trình phê duyệt" column={1} bordered size="small">
              <Descriptions.Item label="Người đề nghị">
                <Text code>{selectedRecord.requestedBy || '—'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Thời điểm đề nghị">
                {selectedRecord.requestedAt ? dayjs(selectedRecord.requestedAt).format('HH:mm:ss DD/MM/YYYY') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Lý do đề nghị">
                {selectedRecord.reason || '—'}
              </Descriptions.Item>

              {selectedRecord.approvedBy && (
                <>
                  <Descriptions.Item label="Người phê duyệt">
                    <Text code>{selectedRecord.approvedBy}</Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="Thời điểm duyệt">
                    {dayjs(selectedRecord.approvedAt).format('HH:mm:ss DD/MM/YYYY')}
                  </Descriptions.Item>
                </>
              )}

              {selectedRecord.rejectedBy && (
                <>
                  <Descriptions.Item label="Người từ chối">
                    <Text code>{selectedRecord.rejectedBy}</Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="Thời điểm từ chối">
                    {dayjs(selectedRecord.rejectedAt).format('HH:mm:ss DD/MM/YYYY')}
                  </Descriptions.Item>
                  <Descriptions.Item label="Lý do từ chối">
                    <strong style={{ color: '#dc2626' }}>{selectedRecord.rejectionReason || '—'}</strong>
                  </Descriptions.Item>
                </>
              )}

              {selectedRecord.invoiceId && (
                <Descriptions.Item label="Hóa đơn đã gắn">
                  <Text code copyable>{selectedRecord.invoiceId}</Text>
                </Descriptions.Item>
              )}
            </Descriptions>
          </Space>
        )}
      </Drawer>

      {/* Modal từ chối đề xuất */}
      <Modal
        title={
          <Space align="center">
            <CloseCircleOutlined style={{ color: '#dc2626' }} />
            <span>Từ chối đề xuất giảm giá</span>
          </Space>
        }
        open={rejectModalOpen}
        onCancel={() => !actionLoading && setRejectModalOpen(false)}
        onOk={handleConfirmReject}
        confirmLoading={actionLoading}
        okText="Xác nhận từ chối"
        cancelText="Hủy bỏ"
        okButtonProps={{ danger: true }}
      >
        <div style={{ marginTop: 12 }}>
          <Paragraph>
            Vui lòng nhập lý do từ chối đề xuất giảm giá này (Bắt buộc theo quy định QTN-37):
          </Paragraph>
          <Input.TextArea
            rows={3}
            placeholder="Ví dụ: Không đáp ứng tiêu chuẩn đối tượng miễn giảm theo quy chế số 12/QC-BV..."
            value={rejectionReason}
            onChange={(e) => setRejectionReason(e.target.value)}
            maxLength={500}
            showCount
          />
        </div>
      </Modal>
    </div>
  )
}
