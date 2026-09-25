import axiosClient from './axiosClient.js'

/**
 * NCL-06-CN-012: API client cho phân hệ Dự trù mua thuốc và phiếu đặt hàng
 * Base URL: /inventory/procurements
 */
const medicationProcurementApi = {
  /**
   * Lấy danh sách gợi ý số lượng thuốc cần mua dựa trên tồn hiện tại,
   * tồn tối thiểu và lượng tiêu thụ kỳ trước (TC-01)
   * @param {Object} params - { from, to, onlyBelowThreshold }
   */
  getSuggestions: (params = {}) => {
    return axiosClient.get('/inventory/procurements/suggestions', { params })
  },

  /**
   * Lập phiếu dự trù mua thuốc mới (TC-01)
   * @param {Object} data - { periodStartDate, periodEndDate, note, submitImmediately, items: [] }
   */
  create: (data) => {
    return axiosClient.post('/inventory/procurements', data)
  },

  /**
   * Tra cứu danh sách phiếu dự trù mua thuốc (TC-02)
   * @param {Object} params - { status, from, to, createdBy, page, size }
   */
  list: (params = {}) => {
    return axiosClient.get('/inventory/procurements', { params })
  },

  /**
   * Xem chi tiết một phiếu dự trù kèm danh sách thuốc chi tiết (TC-02)
   * @param {string} id - UUID của phiếu dự trù
   */
  getById: (id) => {
    return axiosClient.get(`/inventory/procurements/${id}`)
  },

  /**
   * Cập nhật phiếu dự trù khi ở trạng thái DRAFT hoặc PENDING_APPROVAL
   * @param {string} id - UUID của phiếu dự trù
   * @param {Object} data - { note, items: [] }
   */
  update: (id, data) => {
    return axiosClient.put(`/inventory/procurements/${id}`, data)
  },

  /**
   * Gửi duyệt phiếu dự trù đang nháp (chuyển sang PENDING_APPROVAL)
   * @param {string} id - UUID của phiếu dự trù
   */
  submit: (id) => {
    return axiosClient.post(`/inventory/procurements/${id}/submit`)
  },

  /**
   * Hủy phiếu dự trù (chuyển sang CANCELLED)
   * @param {string} id - UUID của phiếu dự trù
   */
  cancel: (id) => {
    return axiosClient.post(`/inventory/procurements/${id}/cancel`)
  },

  /**
   * Phê duyệt phiếu dự trù (dành cho Quản lý phòng khám, áp dụng SoD) (TC-02)
   * @param {string} id - UUID của phiếu dự trù
   * @param {Object} [data] - { note, itemAdjustments }
   */
  approve: (id, data = {}) => {
    return axiosClient.post(`/inventory/procurements/${id}/approve`, data)
  },

  /**
   * Từ chối phiếu dự trù kèm lý do bắt buộc (dành cho Quản lý phòng khám, áp dụng SoD) (TC-02)
   * @param {string} id - UUID của phiếu dự trù
   * @param {Object} data - { reason } (tối thiểu 5 ký tự)
   */
  reject: (id, data) => {
    return axiosClient.post(`/inventory/procurements/${id}/reject`, data)
  },
}

export default medicationProcurementApi
