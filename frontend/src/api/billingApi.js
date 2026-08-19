import axiosClient from './axiosClient'

export default {
  getAll: (params) => axiosClient.get('/invoices', { params }),
  getPayable: (params) => axiosClient.get('/invoices/payable', { params }),
  getByVisit: (visitId) => axiosClient.get('/invoices', { params: { visitId } }),
  getById: (invoiceId) => axiosClient.get(`/invoices/${invoiceId}`),
  pay: (data) => axiosClient.post('/invoices/payments', data),
  quotePayment: (data) => axiosClient.post('/invoices/payment-quotes', data),
  refundPayment: (paymentId, data) => axiosClient.post(`/invoices/payments/${paymentId}/refund`, data),
  createInvoice: (data) => axiosClient.post('/invoices', data),
  adjust: (invoiceId, data) => axiosClient.post(`/invoices/${invoiceId}/adjustments`, data),
}
