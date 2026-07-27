import axiosClient from './axiosClient'
export default {
  getAll: () => axiosClient.get('/invoices'),
  getPayable: () => axiosClient.get('/invoices/payable'),
  pay: (data) => axiosClient.post('/invoices/payments', data),
  adjust: (id, data) => axiosClient.post(`/invoices/${id}/adjustments`, data),
}
