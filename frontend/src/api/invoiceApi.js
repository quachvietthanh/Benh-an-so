import axiosClient from './axiosClient.js'

const invoiceApi = {
  search: (params) => axiosClient.get('/invoices', { params }),

  getById: (invoiceId) => axiosClient.get(`/invoices/${invoiceId}`),

  getAdjustments: (invoiceId) => axiosClient.get(`/invoices/${invoiceId}/adjustments`),

  reprint: (invoiceId) => axiosClient.post(`/invoices/${invoiceId}/reprint`),

  getQuote: (payloadOrVisitId) => {
    const payload = typeof payloadOrVisitId === 'object' && payloadOrVisitId !== null
      ? payloadOrVisitId
      : { visitId: payloadOrVisitId, examFee: 0, medicineFee: 0 }
    return axiosClient.post('/invoices/payment-quotes', payload)
  },

  recordPayment: (payload) => {
    const calculatedAmountPaid = Array.isArray(payload?.paymentMethods)
      ? payload.paymentMethods.reduce((sum, item) => sum + (Number(item?.amount) || 0), 0)
      : (payload?.amountPaid || 0)

    const body = {
      visitId: payload?.visitId,
      examFee: payload?.examFee ?? 0,
      medicineFee: payload?.medicineFee ?? 0,
      amountPaid: payload?.amountPaid ?? calculatedAmountPaid,
      paymentMethods: payload?.paymentMethods,
    }
    return axiosClient.post('/invoices/payments', body)
  },

  createInvoice: (payload) => axiosClient.post('/invoices', payload),
}

export default invoiceApi
