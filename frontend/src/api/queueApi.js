import axiosClient from './axiosClient.js'
import dayjs from 'dayjs'
import { pickFields } from './apiContract.js'

const queueApi = {
  /**
   * Lấy danh sách Queue Board công khai / lễ tân
   * @param {Object} params { date, doctorId, roomId, status, sourceType }
   */
  getQueues: (params = {}) => {
    const formattedParams = pickFields({
      date: dayjs().format('YYYY-MM-DD'),
      ...params,
    }, ['date', 'doctorId', 'roomId'])
    // Clean up empty params
    Object.keys(formattedParams).forEach((key) => {
      if (formattedParams[key] === null || formattedParams[key] === undefined || formattedParams[key] === '') {
        delete formattedParams[key]
      }
    })
    return axiosClient.get('/queues', { params: formattedParams })
  },

  /**
   * Lấy Queue riêng của Bác sĩ đang đăng nhập
   * @param {Object} params { date }
   */
  getMyQueue: (params = {}) => {
    const formattedParams = pickFields({
      date: dayjs().format('YYYY-MM-DD'),
      ...params,
    }, ['date'])
    return axiosClient.get('/queues/me', { params: formattedParams })
  },

  /**
   * Lấy chi tiết lượt khám QueueItem theo ID
   */
  getById: (itemId) => axiosClient.get(`/queue-items/${itemId}`),

  /**
   * Check-in bệnh nhân có lịch hẹn trước (SCHEDULED -> CHECKED_IN)
   */
  checkInAppointment: (appointmentId) => axiosClient.post(`/appointments/${appointmentId}/check-in`),

  /**
   * Check-in bệnh nhân tự đến (Walk-in)
   * @param {Object} data { patientId, doctorId, reason, notes }
   */
  checkInWalkIn: (data) => axiosClient.post(
    '/queue-items/walk-in',
    pickFields(data, ['patientId', 'doctorId', 'reason', 'note']),
  ),

  /**
   * Gọi người tiếp theo trong hàng đợi
   */
  callNext: (queueId) => axiosClient.post(`/queues/${queueId}/call-next`),

  /**
   * Cập nhật trạng thái lượt khám (VD: sang WAITING_FOR_RESULT hoặc IN_PROGRESS)
   * @param {string} itemId
   * @param {Object} data { status }
   */
  updateStatus: (itemId, targetStatus, cancelReason) => {
    const status = typeof targetStatus === 'string'
      ? targetStatus
      : (targetStatus?.targetStatus || targetStatus?.status)
    const reason = cancelReason ?? targetStatus?.cancelReason
    return axiosClient.patch(`/queue-items/${itemId}/status`, {
      targetStatus: status,
      ...(reason ? { cancelReason: reason } : {}),
    })
  },

  /**
   * Đánh dấu bỏ qua/vắng mặt bệnh nhân khi gọi (IN_PROGRESS -> SKIPPED)
   * @param {string} itemId
   * @param {string} reason
   */
  skip: (itemId, reason = 'Vắng mặt khi gọi') => axiosClient.post(`/queue-items/${itemId}/skip`, { reason }),

  /**
   * Bác sĩ hoàn tất lượt khám sau khi bệnh án đã khóa (IN_PROGRESS -> COMPLETED)
   */
  complete: (itemId) => axiosClient.post(`/queue-items/${itemId}/complete`),
}

export default queueApi
