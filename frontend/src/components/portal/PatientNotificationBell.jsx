import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Badge,
  Button,
  Empty,
  Modal,
  Popover,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  BellOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  RightOutlined,
  SwapOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import patientPortalNotificationApi from '../../api/patientPortalNotificationApi.js'
import { useAuthContext } from '../../context/AuthContext.jsx'
import {
  NOTIFICATION_TYPES,
  calculateUnreadCount,
  formatNotificationTime,
  getNotificationTypeMeta,
  resolveNotificationRoute,
} from '../../utils/patientNotificationHelpers.js'
import './patientNotificationBell.css'

const { Text, Paragraph, Title } = Typography

/**
 * Component Chuông thông báo dành riêng cho Cổng Bệnh Nhân (NCL-14-CN-008 / QTN-23).
 *
 * RÀNG BUỘC KIẾN TRÚC:
 * 1. TUYỆT ĐỐI KHÔNG nhúng vào MainLayout.jsx (tránh lỗi 403 cho nhân viên y tế).
 * 2. KHÔNG dựa vào FK ID (appointmentId, clinicalResultId) vì Backend Response không trả về.
 * 3. Tự tính unreadCount từ danh sách tải về (do Backend chưa có endpoint /unread-count).
 */
export default function PatientNotificationBell() {
  const { user, isAuthenticated } = useAuthContext()
  const navigate = useNavigate()

  // Kiểm tra vai trò bệnh nhân: Chỉ chạy khi user có role 'patient'
  const isPatient = useMemo(() => {
    if (!isAuthenticated || !user) return false
    const roles = (user?.roles || []).map((r) =>
      String(r || '').toLowerCase().replace(/^role_/, '')
    )
    return roles.includes('patient')
  }, [user, isAuthenticated])

  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(false)
  const [popoverOpen, setPopoverOpen] = useState(false)

  // State cho Modal xem chi tiết thông báo
  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [selectedNotification, setSelectedNotification] = useState(null)

  const isMountedRef = useRef(true)

  /**
   * Tải danh sách thông báo từ API Backend (/patient-portal/notifications?limit=50)
   */
  const fetchNotifications = useCallback(async (isSilent = false) => {
    if (!isPatient) return
    if (!isSilent) setLoading(true)

    try {
      const res = await patientPortalNotificationApi.getNotifications(50)
      const data = res?.data
      const list = Array.isArray(data)
        ? data
        : Array.isArray(data?.content)
        ? data.content
        : []

      if (isMountedRef.current) {
        setNotifications(list)
      }
    } catch (err) {
      // Bỏ qua lỗi silent khi polling nền để không gây popup phiền toái cho bệnh nhân
      if (!isSilent) {
        console.warn('[PatientNotificationBell] Lỗi tải thông báo:', err?.message)
      }
    } finally {
      if (isMountedRef.current && !isSilent) {
        setLoading(false)
      }
    }
  }, [isPatient])

  // Lần đầu mount
  useEffect(() => {
    isMountedRef.current = true
    if (isPatient) {
      fetchNotifications(false)
    }
    return () => {
      isMountedRef.current = false
    }
  }, [isPatient, fetchNotifications])

  // Polling định kỳ mỗi 30 giây (có clearInterval trong cleanup chống memory leak)
  useEffect(() => {
    if (!isPatient) return

    const timerId = setInterval(() => {
      fetchNotifications(true)
    }, 30000)

    return () => {
      clearInterval(timerId)
    }
  }, [isPatient, fetchNotifications])

  // Refetch khi tab trình duyệt lấy lại focus (window.addEventListener 'focus')
  useEffect(() => {
    if (!isPatient) return

    const handleWindowFocus = () => {
      fetchNotifications(true)
    }

    window.addEventListener('focus', handleWindowFocus)
    return () => {
      window.removeEventListener('focus', handleWindowFocus)
    }
  }, [isPatient, fetchNotifications])

  // Refetch khi người dùng bấm mở popover danh sách
  const handleOpenChange = (newOpen) => {
    setPopoverOpen(newOpen)
    if (newOpen) {
      fetchNotifications(true)
    }
  }

  /**
   * GIẢI PHÁP TẠM THỜI: Tính số lượng chưa đọc từ mảng danh sách
   * (do Backend hiện chưa có endpoint /unread-count riêng theo QTN-23)
   */
  const unreadCount = useMemo(() => {
    return calculateUnreadCount(notifications)
  }, [notifications])

  /**
   * Đánh dấu 1 thông báo là đã đọc (Optimistic Update)
   */
  const handleMarkAsRead = async (item) => {
    if (!item || item.read) return

    // Cập nhật giao diện ngay lập tức
    setNotifications((prev) =>
      prev.map((n) =>
        n.id === item.id ? { ...n, read: true, readAt: new Date().toISOString() } : n
      )
    )

    try {
      await patientPortalNotificationApi.markAsRead(item.id)
    } catch (err) {
      console.warn('[PatientNotificationBell] Lỗi markAsRead:', err?.message)
      // Không hoàn tác để tránh trải nghiệm giật lag cho người dùng (idempotent)
    }
  }

  /**
   * Đánh dấu toàn bộ thông báo hiển thị là đã đọc
   */
  const handleMarkAllAsRead = async (e) => {
    e.stopPropagation()
    const unreadItems = notifications.filter((n) => !n.read)
    if (unreadItems.length === 0) return

    // Optimistic update
    setNotifications((prev) =>
      prev.map((n) => ({ ...n, read: true, readAt: new Date().toISOString() }))
    )

    try {
      await Promise.allSettled(
        unreadItems.map((n) => patientPortalNotificationApi.markAsRead(n.id))
      )
      message.success('Đã đánh dấu tất cả là đã đọc.')
    } catch {
      // Silent error
    }
  }

  /**
   * Xử lý khi bệnh nhân bấm vào 1 dòng thông báo
   */
  const handleItemClick = (item) => {
    // 1. Đánh dấu đã đọc
    if (!item.read) {
      handleMarkAsRead(item)
    }

    // 2. Đóng popover danh sách
    setPopoverOpen(false)

    // 3. Mở modal xem chi tiết đầy đủ (Bảo đảm an toàn UX trước khi điều hướng)
    setSelectedNotification(item)
    setDetailModalOpen(true)
  }

  /**
   * Điều hướng thông minh từ Modal chi tiết theo 'type'
   */
  const handleNavigateFromDetail = () => {
    if (!selectedNotification) return
    const route = resolveNotificationRoute(selectedNotification.type)
    setDetailModalOpen(false)
    navigate(route)
  }

  // Nếu không phải vai trò bệnh nhân, tuyệt đối không render component này
  if (!isPatient) {
    return null
  }

  // Icon theo từng phân loại thông báo
  const renderTypeIcon = (type) => {
    switch (type) {
      case NOTIFICATION_TYPES.APPOINTMENT_REMINDER:
        return (
          <div className="patient-notification-icon-wrap" style={{ background: '#dbeafe', color: '#2563eb' }}>
            <ClockCircleOutlined />
          </div>
        )
      case NOTIFICATION_TYPES.APPOINTMENT_CHANGED:
        return (
          <div className="patient-notification-icon-wrap" style={{ background: '#ffedd5', color: '#ea580c' }}>
            <SwapOutlined />
          </div>
        )
      case NOTIFICATION_TYPES.LAB_RESULT_AVAILABLE:
        return (
          <div className="patient-notification-icon-wrap" style={{ background: '#dcfce7', color: '#16a34a' }}>
            <ExperimentOutlined />
          </div>
        )
      default:
        return (
          <div className="patient-notification-icon-wrap" style={{ background: '#f1f5f9', color: '#64748b' }}>
            <BellOutlined />
          </div>
        )
    }
  }

  // Nội dung Popover danh sách thông báo
  const popoverContent = (
    <div className="patient-notification-popover">
      <div className="patient-notification-header">
        <div className="patient-notification-title-wrap">
          <span className="patient-notification-title">Thông báo</span>
          {unreadCount > 0 && (
            <Tag color="red" style={{ borderRadius: 10, padding: '0 8px', fontWeight: 600, fontSize: 11 }}>
              {unreadCount} mới
            </Tag>
          )}
        </div>

        {unreadCount > 0 && (
          <button
            type="button"
            className="patient-notification-mark-all-btn"
            onClick={handleMarkAllAsRead}
          >
            Đánh dấu đã đọc tất cả
          </button>
        )}
      </div>

      <div className="patient-notification-list">
        {loading && notifications.length === 0 ? (
          <div style={{ padding: '36px 0', textAlign: 'center' }}>
            <Spin size="default" />
            <div style={{ marginTop: 8, fontSize: 12, color: '#64748b' }}>Đang tải thông báo...</div>
          </div>
        ) : notifications.length === 0 ? (
          <div style={{ padding: '32px 16px', textAlign: 'center' }}>
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <span style={{ color: '#64748b', fontSize: 13 }}>
                  Bạn chưa có thông báo mới nào
                </span>
              }
            />
          </div>
        ) : (
          notifications.map((item) => {
            const isUnread = !item.read
            const typeMeta = getNotificationTypeMeta(item.type)

            return (
              <div
                key={item.id}
                className={`patient-notification-item ${isUnread ? 'unread' : ''}`}
                onClick={() => handleItemClick(item)}
              >
                {renderTypeIcon(item.type)}

                <div className="patient-notification-body">
                  <div className="patient-notification-item-header">
                    <span className="patient-notification-item-title" title={item.title}>
                      {item.title}
                    </span>
                    {isUnread && <span className="patient-notification-unread-dot" title="Chưa đọc" />}
                  </div>

                  <div className="patient-notification-item-message" title={item.message}>
                    {item.message}
                  </div>

                  <div className="patient-notification-item-footer">
                    <Tag color={typeMeta.tagColor} style={{ fontSize: 10, margin: 0, padding: '0 4px', lineHeight: '18px' }}>
                      {typeMeta.label}
                    </Tag>
                    <span>{formatNotificationTime(item.createdAt)}</span>
                  </div>
                </div>
              </div>
            )
          })
        )}
      </div>

      {notifications.length > 0 && (
        <div className="patient-notification-footer">
          Chỉ hiển thị tối đa {notifications.length} thông báo mới nhất
        </div>
      )}
    </div>
  )

  const selectedMeta = getNotificationTypeMeta(selectedNotification?.type)

  return (
    <>
      <Popover
        content={popoverContent}
        trigger="click"
        placement="bottomRight"
        open={popoverOpen}
        onOpenChange={handleOpenChange}
        arrow={false}
        overlayInnerStyle={{ padding: 0, borderRadius: 12 }}
      >
        <Tooltip title="Thông báo và nhắc lịch">
          <Badge count={unreadCount} overflowCount={99} size="small" offset={[-2, 3]}>
            <button
              type="button"
              className={`patient-notification-bell-btn ${popoverOpen ? 'active' : ''}`}
              aria-label="Thông báo bệnh nhân"
            >
              <BellOutlined style={{ fontSize: 18 }} />
            </button>
          </Badge>
        </Tooltip>
      </Popover>

      {/* Modal xem chi tiết nội dung đầy đủ (Bảo đảm không phụ thuộc ID liên kết) */}
      <Modal
        open={detailModalOpen}
        onCancel={() => setDetailModalOpen(false)}
        title={
          <Space align="center" size={8}>
            {selectedNotification && renderTypeIcon(selectedNotification.type)}
            <span style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              {selectedNotification?.title || 'Chi tiết thông báo'}
            </span>
          </Space>
        }
        footer={[
          <Button key="close" onClick={() => setDetailModalOpen(false)}>
            Đóng
          </Button>,
          <Button
            key="action"
            type="primary"
            icon={
              selectedNotification?.type === NOTIFICATION_TYPES.LAB_RESULT_AVAILABLE ? (
                <FileTextOutlined />
              ) : (
                <CalendarOutlined />
              )
            }
            onClick={handleNavigateFromDetail}
            style={{
              backgroundColor: selectedMeta.badgeColor,
              borderColor: selectedMeta.badgeColor,
              fontWeight: 600,
            }}
          >
            {selectedMeta.actionText} &rarr;
          </Button>,
        ]}
      >
        {selectedNotification && (
          <div style={{ marginTop: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <Tag color={selectedMeta.tagColor} style={{ fontSize: 12, padding: '2px 8px', fontWeight: 600 }}>
                {selectedMeta.label}
              </Tag>
              <Text type="secondary" style={{ fontSize: 12 }}>
                Gửi lúc: {formatNotificationTime(selectedNotification.createdAt)}
              </Text>
            </div>

            <div
              style={{
                background: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: 8,
                padding: '16px',
                fontSize: 14,
                color: '#1e293b',
                lineHeight: 1.6,
                marginBottom: 12,
              }}
            >
              {selectedNotification.message}
            </div>

            <Paragraph type="secondary" style={{ fontSize: 12, fontStyle: 'italic', marginBottom: 0 }}>
              💡 Gợi ý: Nhấn nút <strong>"{selectedMeta.actionText}"</strong> bên dưới để chuyển nhanh đến màn hình quản lý tương ứng.
            </Paragraph>
          </div>
        )}
      </Modal>
    </>
  )
}
