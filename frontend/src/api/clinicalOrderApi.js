import axiosClient from './axiosClient.js'

const clinicalOrderApi = {
  /**
   * Lấy danh sách chỉ định cận lâm sàng đang chờ kết quả (TC-01, NCL-04-CN-008)
   */
  getPendingOrders: (params = {}) => {
    return axiosClient.get('/clinical-orders/pending', { params })
  },

  /**
   * Lấy danh sách chỉ định theo lượt khám
   */
  getByVisit: (visitId, params = {}) => {
    return axiosClient.get(`/clinical-orders/visits/${visitId}`, { params })
  },

  /**
   * Tạo chỉ định cận lâm sàng cho lượt khám
   */
  create: (visitId, data) => {
    return axiosClient.post(`/clinical-orders/visits/${visitId}`, data)
  },

  /**
   * Hủy toàn bộ phiếu chỉ định kèm lý do (TC-02)
   */
  cancelOrder: (orderId, cancelReason) => {
    return axiosClient.post(`/clinical-orders/${orderId}/cancel`, { cancelReason })
  },

  /**
   * Hủy từng dịch vụ chỉ định cận lâm sàng cụ thể kèm lý do (TC-02, TC-04)
   */
  cancelOrderItem: (orderItemId, cancelReason) => {
    return axiosClient.post(`/clinical-orders/items/${orderItemId}/cancel`, { cancelReason })
  },
}

export default clinicalOrderApi
