import axiosClient from './axiosClient.js'

/**
 * API client cho tính năng Thông báo và nhắc lịch trên cổng bệnh nhân (NCL-14-CN-008 / QTN-23).
 * Backend: PatientPortalNotificationController (/patient-portal/notifications)
 * Yêu cầu quyền: ROLE_PATIENT
 */
const patientPortalNotificationApi = {
  /**
   * Lấy danh sách thông báo của bệnh nhân hiện tại (mới nhất xếp trước)
   * @param {number} limit Mặc định 50, tối đa 100
   */
  getNotifications: (limit = 50) =>
    axiosClient.get('/patient-portal/notifications', { params: { limit } }),

  /**
   * Xem chi tiết 1 thông báo (chỉ chính chủ xem được, tự ghi nhận AuditLog)
   * @param {string} id UUID của thông báo
   */
  getById: (id) =>
    axiosClient.get(`/patient-portal/notifications/${id}`),

  /**
   * Đánh dấu 1 thông báo đã đọc (idempotent)
   * @param {string} id UUID của thông báo
   */
  markAsRead: (id) =>
    axiosClient.patch(`/patient-portal/notifications/${id}/read`),
}

export default patientPortalNotificationApi
