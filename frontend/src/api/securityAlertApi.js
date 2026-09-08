import axiosClient from './axiosClient'

const securityAlertApi = {
  /**
   * Lấy danh sách cảnh báo truy cập bất thường có phân trang
   * @param {Object} params - { page: number, size: number, sort: string }
   * @returns {Promise} Page<SecurityAlertResponse>
   */
  getAlerts: (params = {}) => {
    return axiosClient.get('/security-alerts', {
      params: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        ...params,
      },
    })
  },

  /**
   * Cập nhật trạng thái của một cảnh báo bảo mật
   * @param {string} id - UUID của cảnh báo
   * @param {'UNREAD'|'READ'|'DISMISSED'} status - Trạng thái mới
   * @returns {Promise} SecurityAlertResponse
   */
  updateStatus: (id, status) => {
    return axiosClient.patch(`/security-alerts/${id}/status`, null, {
      params: { status },
    })
  },
}

export default securityAlertApi
