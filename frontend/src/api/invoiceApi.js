import axiosClient from './axiosClient.js'

const invoiceApi = {
  /**
   * Tra cứu danh sách hóa đơn theo nhiều tiêu chí (phân trang, server-side sort/filter)
   * @param {Object} params - { invoiceCode, invoiceType, visitId, patientName, createdFrom, createdTo, page, size }
   */
  search: (params) => axiosClient.get('/invoices', { params }),

  /**
   * Lấy chi tiết một hóa đơn theo ID kèm danh sách khoản mục (lines)
   * @param {string} invoiceId
   */
  getById: (invoiceId) => axiosClient.get(`/invoices/${invoiceId}`),

  /**
   * Lấy danh sách hóa đơn điều chỉnh liên quan (originalAmount, finalAmount, adjustments[])
   * @param {string} invoiceId
   */
  getAdjustments: (invoiceId) => axiosClient.get(`/invoices/${invoiceId}/adjustments`),

  /**
   * Ghi nhận mỗi lần in lại hóa đơn (tăng reprintCount, ghi audit log)
   * @param {string} invoiceId
   */
  reprint: (invoiceId) => axiosClient.post(`/invoices/${invoiceId}/reprint`),
}

export default invoiceApi
