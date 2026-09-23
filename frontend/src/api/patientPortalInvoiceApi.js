import axiosClient from './axiosClient.js'

/**
 * NCL-14-CN-007: API client cho Cổng thông tin bệnh nhân - Hóa đơn viện phí
 */
const patientPortalInvoiceApi = {
  /**
   * Lấy danh sách hóa đơn của bệnh nhân đang đăng nhập (TC-01, TC-04)
   * Backend tự động phân quyền theo JWT token của bệnh nhân (QTN-23)
   * @param {Object} [params] - { visitId, limit }
   */
  getInvoices: (params) => {
    return axiosClient.get('/patient-portal/invoices', { params })
  },

  /**
   * Lấy thông tin chi tiết một hóa đơn kèm các khoản mục (lines) (TC-01)
   * @param {string} invoiceId - UUID của hóa đơn
   */
  getInvoiceDetail: (invoiceId) => {
    return axiosClient.get(`/patient-portal/invoices/${invoiceId}`)
  },

  /**
   * Tải về file PDF hóa đơn điện tử đọc được (TC-02)
   * @param {string} invoiceId - UUID của hóa đơn
   */
  downloadInvoice: (invoiceId) => {
    return axiosClient.get(`/patient-portal/invoices/${invoiceId}/download`, {
      responseType: 'blob',
    })
  },
}

export default patientPortalInvoiceApi
