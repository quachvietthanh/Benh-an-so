import React, { useEffect, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Descriptions,
  Divider,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'

import medicationProcurementApi from '../../api/medicationProcurementApi.js'
import {
  canApproveOrRejectPlan,
  formatDate,
  formatDateTime,
  getProcurementStatusMeta,
} from '../../utils/medicationProcurementHelpers.js'
import RejectProcurementModal from './RejectProcurementModal.jsx'

const { Title, Text, Paragraph } = Typography

export default function ProcurementPlanDetailModal({
  open,
  onClose,
  planId,
  currentUserId,
  isManagerOrAdmin = false,
  onActionSuccess,
}) {
  const [plan, setPlan] = useState(null)
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [rejectModalOpen, setRejectModalOpen] = useState(false)

  const fetchDetail = async () => {
    if (!planId) return
    setLoading(true)
    try {
      const res = await medicationProcurementApi.getById(planId)
      setPlan(res.data)
    } catch (err) {
      message.error('Không thể tải chi tiết phiếu dự trù.')
      onClose()
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (open && planId) {
      fetchDetail()
    } else {
      setPlan(null)
    }
  }, [open, planId])

  const statusMeta = getProcurementStatusMeta(plan?.status)
  const sodCheck = canApproveOrRejectPlan(plan, currentUserId)

  // Xử lý Phê duyệt phiếu
  const handleApprove = async () => {
    if (!planId) return
    setActionLoading(true)
    try {
      await medicationProcurementApi.approve(planId, {
        note: 'Đã phê duyệt phiếu dự trù thuốc',
      })
      message.success('Đã phê duyệt phiếu dự trù mua thuốc thành công!')
      fetchDetail()
      if (onActionSuccess) onActionSuccess()
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể phê duyệt phiếu. Vui lòng thử lại.'
      message.error(msg)
    } finally {
      setActionLoading(false)
    }
  }

  // Xử lý Từ chối phiếu
  const handleConfirmReject = async (reason) => {
    if (!planId) return
    setActionLoading(true)
    try {
      await medicationProcurementApi.reject(planId, { reason })
      message.success('Đã từ chối phiếu dự trù mua thuốc!')
      setRejectModalOpen(false)
      fetchDetail()
      if (onActionSuccess) onActionSuccess()
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể từ chối phiếu. Vui lòng thử lại.'
      message.error(msg)
    } finally {
      setActionLoading(false)
    }
  }

  const columns = [
    {
      title: 'Mã thuốc',
      dataIndex: 'medicineCode',
      key: 'medicineCode',
      width: 110,
      render: (code) => (
        <span style={{ fontFamily: 'monospace', fontWeight: 600, color: '#2563eb' }}>
          {code || '—'}
        </span>
      ),
    },
    {
      title: 'Tên thuốc & Quy cách',
      dataIndex: 'medicineName',
      key: 'medicineName',
      render: (name) => <strong style={{ color: '#0f172a' }}>{name}</strong>,
    },
    {
      title: 'Đơn vị',
      dataIndex: 'unit',
      key: 'unit',
      width: 80,
      render: (unit) => <Tag color="blue">{unit || 'Viên'}</Tag>,
    },
    {
      title: 'Tồn hiện tại',
      dataIndex: 'currentStock',
      key: 'currentStock',
      width: 105,
      align: 'right',
      render: (stock, record) => {
        const isBelow = stock < record.minStockThreshold
        return (
          <span style={{ fontWeight: isBelow ? 700 : 500, color: isBelow ? '#dc2626' : '#1e293b' }}>
            {stock}
          </span>
        )
      },
    },
    {
      title: 'Tồn tối thiểu',
      dataIndex: 'minStockThreshold',
      key: 'minStockThreshold',
      width: 105,
      align: 'right',
      render: (min) => <span style={{ color: '#64748b' }}>{min}</span>,
    },
    {
      title: 'Tiêu thụ kỳ trước',
      dataIndex: 'previousPeriodConsumption',
      key: 'previousPeriodConsumption',
      width: 130,
      align: 'right',
      render: (consumption) => {
        if (consumption === null || consumption === undefined) {
          return <Tag color="orange">Chưa có lịch sử</Tag>
        }
        return <span style={{ color: '#0f172a', fontWeight: 500 }}>{consumption}</span>
      },
    },
    {
      title: 'Gợi ý mua',
      dataIndex: 'suggestedQuantity',
      key: 'suggestedQuantity',
      width: 105,
      align: 'right',
      render: (qty) => {
        if (qty === null || qty === undefined) {
          return <span style={{ color: '#94a3b8' }}>—</span>
        }
        return (
          <span style={{ fontWeight: 700, color: '#2563eb' }}>
            {qty}
          </span>
        )
      },
    },
    {
      title: 'Đề xuất đặt',
      dataIndex: 'proposedQuantity',
      key: 'proposedQuantity',
      width: 110,
      align: 'right',
      render: (proposed) => (
        <span style={{ fontSize: 14, fontWeight: 800, color: '#0f172a' }}>
          {proposed}
        </span>
      ),
    },
    ...(plan?.status === 'APPROVED'
      ? [
          {
            title: 'SL đã duyệt',
            dataIndex: 'approvedQuantity',
            key: 'approvedQuantity',
            width: 110,
            align: 'right',
            render: (approved) => (
              <span style={{ fontSize: 14, fontWeight: 800, color: '#16a34a' }}>
                {approved}
              </span>
            ),
          },
        ]
      : []),
    {
      title: 'Ghi chú',
      dataIndex: 'note',
      key: 'note',
      render: (note) => <span style={{ color: '#64748b', fontSize: 12.5 }}>{note || '—'}</span>,
    },
  ]

  return (
    <>
      <Modal
        open={open}
        onCancel={onClose}
        width={920}
        centered
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 8,
                background: '#eff6ff',
                color: '#2563eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 20,
              }}
            >
              <FileTextOutlined />
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span style={{ fontSize: 17, fontWeight: 800, color: '#0f172a' }}>
                  Chi tiết phiếu dự trù: {plan?.planCode || '...'}
                </span>
                <Tag color={statusMeta.color} style={{ fontWeight: 700, fontSize: 12 }}>
                  {statusMeta.label}
                </Tag>
              </div>
              <div style={{ fontSize: 12.5, color: '#64748b' }}>
                Phân hệ Quản lý kho thuốc & Cấp phát • Mã User Story NCL-06-CN-012
              </div>
            </div>
          </div>
        }
        footer={[
          <Button key="close" onClick={onClose} style={{ borderRadius: 8 }}>
            Đóng
          </Button>,
          ...(isManagerOrAdmin && plan?.status === 'PENDING_APPROVAL'
            ? [
                <Tooltip
                  key="reject-tip"
                  title={!sodCheck.allowed ? sodCheck.reason : ''}
                >
                  <Button
                    key="reject"
                    danger
                    icon={<CloseCircleOutlined />}
                    disabled={!sodCheck.allowed || actionLoading}
                    onClick={() => setRejectModalOpen(true)}
                    style={{ borderRadius: 8, fontWeight: 600 }}
                  >
                    Từ chối
                  </Button>
                </Tooltip>,
                <Tooltip
                  key="approve-tip"
                  title={!sodCheck.allowed ? sodCheck.reason : ''}
                >
                  <Popconfirm
                    key="approve-confirm"
                    title="Xác nhận phê duyệt phiếu dự trù"
                    description="Sau khi phê duyệt, phiếu sẽ chuyển sang trạng thái 'Đã duyệt' và Dược sĩ có thể tiến hành đặt mua hàng."
                    okText="Đồng ý duyệt"
                    cancelText="Hủy"
                    onConfirm={handleApprove}
                    disabled={!sodCheck.allowed || actionLoading}
                  >
                    <Button
                      key="approve"
                      type="primary"
                      icon={<CheckCircleOutlined />}
                      loading={actionLoading}
                      disabled={!sodCheck.allowed}
                      style={{
                        background: '#16a34a',
                        borderColor: '#16a34a',
                        borderRadius: 8,
                        fontWeight: 600,
                      }}
                    >
                      Phê duyệt phiếu
                    </Button>
                  </Popconfirm>
                </Tooltip>,
              ]
            : []),
        ]}
      >
        {loading ? (
          <div style={{ padding: '30px 0', textAlign: 'center' }}>Đang tải dữ liệu...</div>
        ) : plan ? (
          <div style={{ margin: '14px 0' }}>
            {/* Cảnh báo SoD nếu vi phạm */}
            {isManagerOrAdmin && plan.status === 'PENDING_APPROVAL' && sodCheck.isSoDViolation && (
              <Alert
                type="warning"
                showIcon
                icon={<WarningOutlined />}
                message="Quy tắc Phân tách nhiệm vụ (Separation of Duties - SoD)"
                description="Bạn là người lập phiếu dự trù này nên không được tự phê duyệt hoặc từ chối phiếu của chính mình. Vui lòng để một Quản lý phòng khám khác xem xét và duyệt phiếu."
                style={{ borderRadius: 10, marginBottom: 16 }}
              />
            )}

            {/* Thông báo nếu phiếu bị từ chối kèm lý do */}
            {plan.status === 'REJECTED' && (
              <Alert
                type="error"
                showIcon
                icon={<CloseCircleOutlined />}
                message="Phiếu dự trù đã bị Quản lý từ chối"
                description={
                  <div>
                    <div style={{ fontWeight: 600, marginBottom: 4 }}>
                      Lý do từ chối: <span style={{ color: '#991b1b', fontWeight: 500 }}>{plan.rejectionReason || 'Không có ghi chú cụ thể.'}</span>
                    </div>
                    <div style={{ fontSize: 12, color: '#7f1d1d' }}>
                      Dược sĩ vui lòng xem xét lý do trên và lập phiếu dự trù mới với số lượng điều chỉnh phù hợp.
                    </div>
                  </div>
                }
                style={{ borderRadius: 10, marginBottom: 16 }}
              />
            )}

            {/* Thông báo nếu phiếu đã duyệt */}
            {plan.status === 'APPROVED' && (
              <Alert
                type="success"
                showIcon
                icon={<CheckCircleOutlined />}
                message="Phiếu dự trù đã được phê duyệt chính thức"
                description={`Thời gian phê duyệt: ${formatDateTime(plan.approvedAt)}. Dược sĩ có thể sử dụng số liệu này để tiến hành liên hệ đặt hàng.`}
                style={{ borderRadius: 10, marginBottom: 16 }}
              />
            )}

            {/* Thẻ thông tin chung của phiếu */}
            <Card size="small" style={{ borderRadius: 10, marginBottom: 16, background: '#f8fafc' }}>
              <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }} bordered={false}>
                <Descriptions.Item label="Mã phiếu">
                  <strong style={{ color: '#2563eb', fontFamily: 'monospace' }}>{plan.planCode}</strong>
                </Descriptions.Item>
                <Descriptions.Item label="Trạng thái">
                  <Tag color={statusMeta.color} style={{ fontWeight: 600 }}>{statusMeta.label}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Ngày tạo">
                  {formatDateTime(plan.createdAt)}
                </Descriptions.Item>
                <Descriptions.Item label="Kỳ tham chiếu tiêu thụ">
                  {formatDate(plan.periodStartDate)} — {formatDate(plan.periodEndDate)}
                </Descriptions.Item>
                <Descriptions.Item label="Tổng số loại thuốc">
                  <strong>{plan.totalItems} loại</strong>
                </Descriptions.Item>
                <Descriptions.Item label="Tổng số lượng đề xuất">
                  <strong style={{ color: '#0f172a', fontSize: 14 }}>{plan.totalProposedQuantity}</strong>
                </Descriptions.Item>
                {plan.note && (
                  <Descriptions.Item label="Ghi chú phiếu" span={3}>
                    <span style={{ fontStyle: 'italic', color: '#475569' }}>"{plan.note}"</span>
                  </Descriptions.Item>
                )}
              </Descriptions>
            </Card>

            {/* Bảng danh sách các loại thuốc */}
            <div style={{ marginBottom: 8 }}>
              <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a', marginBottom: 8 }}>
                Danh sách thuốc dự trù ({plan.items?.length || 0} dòng):
              </div>
              <Table
                rowKey={(r) => r.id || r.medicineId}
                columns={columns}
                dataSource={plan.items || []}
                size="small"
                pagination={false}
                bordered
              />
            </div>
          </div>
        ) : null}
      </Modal>

      {/* Modal nhập lý do từ chối */}
      <RejectProcurementModal
        open={rejectModalOpen}
        onClose={() => setRejectModalOpen(false)}
        onConfirmReject={handleConfirmReject}
        loading={actionLoading}
        planCode={plan?.planCode}
      />
    </>
  )
}
